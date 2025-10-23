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

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Engine> userEngines = new ConcurrentHashMap<>();
    private final Map<String, ProgramInfo> globalPrograms = new ConcurrentHashMap<>();
    private final Map<String, FunctionInfo> globalFunctions = new ConcurrentHashMap<>();

    private final Map<String, List<RunHistoryDetails>> userHistories = new ConcurrentHashMap<>();

    private final Map<String, String> programXmlContent = new ConcurrentHashMap<>();


    public void updateLastRunHistory(String username, RunHistoryDetails updatedRun) {
        List<RunHistoryDetails> history = userHistories.get(username);
        if (history != null && !history.isEmpty()) {
            history.set(history.size() - 1, updatedRun);
        }
    }


    public List<RunHistoryDetails> getUserHistory(String username) {
        List<RunHistoryDetails> history = userHistories.getOrDefault(username, new ArrayList<>());

        //debug
        System.out.println("Getting history for user: " + username);
        System.out.println("  Total entries: " + history.size());
        if (!history.isEmpty()) {
            RunHistoryDetails first = history.get(0);
            System.out.println("  First entry - degree: " + first.expansionDegree() + ", cycles: " + first.cyclesNumber());
        }

        return history;
    }

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

            //check if program already exists
            if (globalPrograms.containsKey(programName)) {
                throw new Exception("A program with the name '" + programName + "' already exists.");
            }


            //convert and store
            Program program = JaxbConversion.SProgramToProgram(sProgram);

            //calculate instruction count at degree 0
            int instructionCount = program.getInstructions().size();
            int maxDegree = program.calculateMaxDegree(new HashMap<>());

            ProgramInfo programInfo = new ProgramInfo(programName, username, program,
                    instructionCount, maxDegree);
            globalPrograms.put(programName, programInfo);
            //store xml content
            programXmlContent.put(programName, xmlContent);


            //store functions (only if they dont already exist)
            if (sProgram.getSFunctions() != null) {
                for (SFunction sFunc : sProgram.getSFunctions().getSFunction()) {
                    String funcName = sFunc.getName();

                    //check if function already exists
                    if (globalFunctions.containsKey(funcName)) {
                        //function already exists - skip it (functions are shared)
                        System.out.println("Function '" + funcName + "' already exists. Skipping (shared function).");
                        continue;
                    }

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

            //update user stats
            User user = users.get(username);
            if (user != null) {
                user.incrementProgramsUploaded();
                //only count NEW functions added
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

            //load into user engine
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
        if (engine == null) {
            return;
        }

        // Check if program exists globally
        String xmlContent = programXmlContent.get(programName);

        // If not found as main program, check if it's a function
        if (xmlContent == null) {
            // Search through all programs to find which one contains this function
            for (Map.Entry<String, String> entry : programXmlContent.entrySet()) {
                String mainProgramName = entry.getKey();
                xmlContent = entry.getValue();

                try {
                    // Parse to check if this XML contains the function
                    JAXBContext jaxbContext = JAXBContext.newInstance(SProgram.class);
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    StringReader reader = new StringReader(xmlContent);
                    SProgram sProgram = (SProgram) unmarshaller.unmarshal(reader);

                    // Check if this program contains the function we're looking for
                    if (sProgram.getSFunctions() != null) {
                        for (SFunction sFunc : sProgram.getSFunctions().getSFunction()) {
                            if (sFunc.getUserString().equals(programName) || sFunc.getName().equals(programName)) {
                                // Found it! Use this XML
                                xmlContent = entry.getValue();
                                programName = sFunc.getUserString(); // Use the user string for display
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue searching
                }
            }
        }

        if (xmlContent != null) {
            // Check if program is already loaded in this user's engine
            try {
                List<String> availablePrograms = engine.getDisplayableProgramNames();

                // Parse the XML to get the main program name
                JAXBContext jaxbContext = JAXBContext.newInstance(SProgram.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                StringReader reader = new StringReader(xmlContent);
                SProgram sProgram = (SProgram) unmarshaller.unmarshal(reader);
                String mainProgramName = sProgram.getName();

                if (!availablePrograms.contains(mainProgramName)) {
                    // Load the entire program (including all functions) from stored XML
                    System.out.println("Loading program with functions into user engine: " + mainProgramName);
                    java.io.File tempFile = java.io.File.createTempFile("program_", ".xml");
                    java.io.FileWriter writer = new java.io.FileWriter(tempFile);
                    writer.write(xmlContent);
                    writer.close();
                    engine.loadProgramFromFile(tempFile);
                    tempFile.delete();
                    System.out.println("Program loaded successfully with all functions");
                } else {
                    System.out.println("Program already loaded in user engine: " + mainProgramName);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load program into user engine: " + e.getMessage());
            }
        }

        System.out.println("Setting context to: " + programName);
        engine.setContextProgram(programName);
    }

    public ExecutionDetails runProgram(String username, int degree, Long[] inputs) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            ExecutionDetails result = engine.runProgram(degree, inputs);

            ProgramDetails currentContext = engine.getProgramDetails();
            String contextProgramName = currentContext != null ? currentContext.name() : null;

            List<RunHistoryDetails> engineStats = engine.getStatistics();
            if (engineStats != null && !engineStats.isEmpty()) {
                RunHistoryDetails lastRun = engineStats.get(engineStats.size() - 1);

                List<RunHistoryDetails> history = userHistories.computeIfAbsent(username, k -> new ArrayList<>());
                history.add(lastRun);
            }

            return result;
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

        public String getName() { return name; }
        public String getUserString() { return userString; }
        public String getProgramName() { return programName; }
        public String getOwner() { return owner; }
        public Program getProgram() { return program; }
        public int getInstructionCount() { return instructionCount; }
        public int getMaxDegree() { return maxDegree; }
    }
}