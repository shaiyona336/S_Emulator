package manager;

import components.program.JaxbConversion; // Your conversion class
import components.program.Program;      // Your Program interface/class
import components.jaxb.generated.SProgram; // Your JAXB generated class

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EngineManager {
    private static final EngineManager instance = new EngineManager();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    // A map to hold loaded programs, keyed by program name
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
        return true;
    }

    /**
     * Parses an XML string and loads the program into memory.
     * @param xmlContent The XML content of the program file.
     * @return The name of the loaded program.
     * @throws Exception if parsing fails or the program already exists.
     */
    public synchronized String uploadProgram(String xmlContent) throws Exception {
        try {
            // 1. Create a JAXBContext and Unmarshaller
            JAXBContext jaxbContext = JAXBContext.newInstance(SProgram.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // 2. Unmarshal the XML string into an SProgram object
            StringReader reader = new StringReader(xmlContent);
            SProgram sProgram = (SProgram) unmarshaller.unmarshal(reader);

            // 3. Check if a program with this name already exists
            if (loadedPrograms.containsKey(sProgram.getName())) {
                throw new Exception("A program with the name '" + sProgram.getName() + "' already exists.");
            }

            // 4. Use your conversion logic to create the final Program object
            Program program = JaxbConversion.SProgramToProgram(sProgram);

            // 5. Store the program and return its name
            loadedPrograms.put(program.getName(), program);
            return program.getName();

        } catch (JAXBException e) {
            // This catches errors from the XML parsing itself
            throw new Exception("Invalid XML format: " + e.getMessage(), e);
        }
    }
}