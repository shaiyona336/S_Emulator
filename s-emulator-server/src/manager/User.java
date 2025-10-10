package manager;

public class User {
    private final String username;
    private int credits;

    public User(String username, int initialCredits) {
        this.username = username;
        this.credits = initialCredits;
    }

    // Add getters, setters, and methods to manage credits
    public String getUsername() { return username; }
    public int getCredits() { return credits; }
    public void deductCredits(int amount) { this.credits -= amount; }
}