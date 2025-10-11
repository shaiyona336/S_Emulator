package manager;

public class User {
    private final String username;
    private int credits;

    public User(String username, int initialCredits) {
        this.username = username;
        this.credits = initialCredits;
    }

    public String getUsername() {
        return username;
    }

    public int getCredits() {
        return credits;
    }

    public void addCredits(int amount) {
        this.credits += amount;
    }

    public boolean deductCredits(int amount) {
        if (credits >= amount) {
            credits -= amount;
            return true;
        }
        return false;
    }
}