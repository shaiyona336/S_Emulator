package manager;

import components.engine.Engine;
import components.engine.StandardEngine;
import components.jaxb.generated.SProgram;
import components.program.JaxbConversion;
import components.program.Program;
import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EngineManager {
    private static final EngineManager instance = new EngineManager();

    // Map of username -> User
    private final Map<String, User> users = new ConcurrentHashMap<>();

    // Map of username -> Engine (each user has their own engine instance)
    private final Map<String, Engine> userEngines = new ConcurrentHashMap<>();

    // Map to store loaded programs globally
    private final Map<String, Program> loadedPrograms = new ConcurrentHashMap<>();

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

    public Engine getUserEngine(String username) {
        return userEngines.get(username);
    }

    /**
     * Uploads and parses a program file for a specific user
     */
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

            // Check if program already exists
            if (loadedPrograms.containsKey(sProgram.getName())) {
                throw new Exception("A program with the name '" + sProgram.getName() + "' already exists.");
            }

            Program program = JaxbConversion.SProgramToProgram(sProgram);
            loadedPrograms.put(program.getName(), program);

            // Load the program into the user's engine
            // We'll create a temp file for this
            java.io.File tempFile = java.io.File.createTempFile("program_", ".xml");
            java.io.FileWriter writer = new java.io.FileWriter(tempFile);
            writer.write(xmlContent);
            writer.close();

            engine.loadProgramFromFile(tempFile);
            tempFile.delete();

            return program.getName();

        } catch (JAXBException e) {
            throw new Exception("Invalid XML format: " + e.getMessage(), e);
        }
    }

    // Program operations
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

    // Execution operations
    public ExecutionDetails runProgram(String username, int degree, Long[] inputs) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.runProgram(degree, inputs);
        }
        return null;
    }

    // Debug operations
    public DebugStepDetails startDebugging(String username, int degree, Long[] inputs) {
        Engine engine = getUserEngine(username);
        if (engine != null && engine.isProgramLoaded()) {
            return engine.startDebugging(degree, inputs);
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

    // Statistics
    public List<RunHistoryDetails> getStatistics(String username) {
        Engine engine = getUserEngine(username);
        if (engine != null) {
            return engine.getStatistics();
        }
        return null;
    }
}