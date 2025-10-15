package manager;

import components.engine.Engine;
import components.engine.StandardEngine;
import components.jaxb.generated.SFunction;
import components.jaxb.generated.SProgram;
import components.program.JaxbConversion;
import components.program.Program;
import dtos.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EngineManager {
    private static final EngineManager instance = new EngineManager();

    // Global storage
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Engine> userEngines = new ConcurrentHashMap<>();
    private final Map<String, ProgramInfo> globalPrograms = new ConcurrentHashMap<>(); // programName -> ProgramInfo
    private final Map<String, FunctionInfo> globalFunctions = new ConcurrentHashMap<>(); // functionName -> FunctionInfo

    private EngineManager() {}

    public static EngineManager getInstance() {
        return instance;
    }

    public synchronized boolean addUser(String username) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, new User(username, 1000));
        userEngines.put(username, new StandardEngine());
        return true;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public Engine getUserEngine(String username) {
        return userEngines.get(username);
    }

    public synchronized String uploadProgram(String username, String xmlContent) throws Exception {
        Engine engine = getUserEngine(username);
        if (engine == null) {
            throw new Exception("User not logged in");
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SProgram.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmlContent);
            SProgram sProgram = (SProgram) unmarshaller.unmarshal(reader);

            String programName = sProgram.getName();

            // Check if program already exists
            if (globalPrograms.containsKey(programName)) {
                throw new Exception("A program with the name '" + programName + "' already exists.");
            }

            // REMOVED: Validation that prevented duplicate functions
            // Functions can be shared across programs!

            // Convert and store
            Program program = JaxbConversion.SProgramToProgram(sProgram);

            // Calculate instruction count at degree 0
            int instructionCount = program.getInstructions().size();
            int maxDegree = program.calculateMaxDegree(new HashMap<>());

            ProgramInfo programInfo = new ProgramInfo(programName, username, program,
                    instructionCount, maxDegree);
            globalPrograms.put(programName, programInfo);

            // Store functions (only if they don't already exist)
            if (sProgram.getSFunctions() != null) {
                for (SFunction sFunc : sProgram.getSFunctions().getSFunction()) {
                    String funcName = sFunc.getName();

                    // Check if function already exists
                    if (globalFunctions.containsKey(funcName)) {
                        // Function already exists - skip it (functions are shared)
                        System.out.println("Function '" + funcName + "' already exists. Skipping (shared function).");
                        continue;
                    }

                    // Add new function
                    Program funcProgram = JaxbConversion.SFunctionToProgram(sFunc);
                    int funcInstructionCount = funcProgram.getInstructions().size();
                    int funcMaxDegree = funcProgram.calculateMaxDegree(new HashMap<>());

                    FunctionInfo funcInfo = new FunctionInfo(
                            sFunc.getName(),
                            sFunc.getUserString(),
                            programName,
                            username,
                            funcProgram,
                            funcInstructionCount,
                            funcMaxDegree
                    );
                    globalFunctions.put(funcName, funcInfo);
                }
            }

            // Update user stats
            User user = users.get(username);
            if (user != null) {
                user.incrementProgramsUploaded();
                // Only count NEW functions added
                int newFunctionsCount = 0;
                if (sProgram.getSFunctions() != null) {
                    for (SFunction sFunc : sProgram.getSFunctions().getSFunction()) {
                        if (!globalFunctions.containsKey(sFunc.getName())) {
                            newFunctionsCount++;
                        }
                    }
                }
                user.incrementFunctionsUploaded(newFunctionsCount);
            }

            // Load into user's engine
            java.io.File tempFile = java.io.File.createTempFile("program_", ".xml");
            java.io.FileWriter writer = new java.io.FileWriter(tempFile);
            writer.write(xmlContent);
            writer.close();
            engine.loadProgramFromFile(tempFile);
            tempFile.delete();

            return programName;

        } catch (JAXBException e) {
            throw new Exception("Invalid XML format: " + e.getMessage(), e);
        }
    }

    public Collection<ProgramInfo> getAllPrograms() {
        return globalPrograms.values();
    }

    public Collection<FunctionInfo> getAllFunctions() {
        return globalFunctions.values();
    }

    public ProgramInfo getProgramInfo(String programName) {
        return globalPrograms.get(programName);
    }

    // Existing methods...
    public ProgramDetails getProgramDetails(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.getProgramDetails();
        }
        return null;
    }

    public int getProgramMaxDegree(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.getProgramMaxDegree();
        }
        return 0;
    }

    public ProgramDetails expandProgram(String username, int degree) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.expandProgram(degree);
        }
        return null;
    }

    public List<String> getDisplayableProgramNames(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            return engine.getDisplayableProgramNames();
        }
        return null;
    }

    public void setContextProgram(String username, String programName) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            engine.setContextProgram(programName);
        }
    }

    public ExecutionDetails runProgram(String username, int degree, Long[] inputs) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.runProgram(degree, inputs);
        }
        return null;
    }

    public DebugStepDetails startDebugging(String username, int degree, Long[] inputs, String architecture, int initialCredits) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.startDebugging(degree, inputs, architecture, initialCredits);
        }
        return null;
    }

    public DebugStepDetails stepOver(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            return engine.stepOver();
        }
        return null;
    }

    public ExecutionDetails resume(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            return engine.resume();
        }
        return null;
    }

    public void stopDebugging(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            engine.stop();
        }
    }

    public List<RunHistoryDetails> getStatistics(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            return engine.getStatistics();
        }
        return null;
    }

    public static class ProgramInfo {
        private final String name;
        private final String owner;
        private final Program program;
        private final int instructionCount;
        private final int maxDegree;
        private int runCount = 0;
        private int totalCost = 0;

        public ProgramInfo(String name, String owner, Program program,
                           int instructionCount, int maxDegree) {
            this.name = name;
            this.owner = owner;
            this.program = program;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
        }

        public synchronized void recordRun(int cost) {  // Make it synchronized
            runCount++;
            totalCost += cost;
        }

        public int getAvgCost() {
            return runCount > 0 ? totalCost / runCount : 0;
        }

        // Getters
        public String getName() { return name; }
        public String getOwner() { return owner; }
        public Program getProgram() { return program; }
        public int getInstructionCount() { return instructionCount; }
        public int getMaxDegree() { return maxDegree; }
        public int getRunCount() { return runCount; }
    }

    public static class FunctionInfo {
        private final String name;
        private final String userString;
        private final String programName;
        private final String owner;
        private final Program program;
        private final int instructionCount;
        private final int maxDegree;

        public FunctionInfo(String name, String userString, String programName,
                            String owner, Program program,
                            int instructionCount, int maxDegree) {
            this.name = name;
            this.userString = userString;
            this.programName = programName;
            this.owner = owner;
            this.program = program;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
        }

        // Getters
        public String getName() { return name; }
        public String getUserString() { return userString; }
        public String getProgramName() { return programName; }
        public String getOwner() { return owner; }
        public Program getProgram() { return program; }
        public int getInstructionCount() { return instructionCount; }
        public int getMaxDegree() { return maxDegree; }
    }
}