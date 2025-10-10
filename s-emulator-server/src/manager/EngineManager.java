package manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EngineManager {
    private static final EngineManager instance = new EngineManager();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    private EngineManager() {}

    public static EngineManager getInstance() {
        return instance;
    }

    public synchronized boolean addUser(String username) {
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        users.put(username, new User(username, 1000)); // Start with 1000 credits
        return true;
    }

    //will add your other engine logic here later
}