package manager;

public class User {
    private final String username;
    private int credits;
    private int usedCredits = 0;
    private int programsUploaded = 0;
    private int functionsUploaded = 0;
    private int totalRuns = 0;

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
            usedCredits += amount;
            return true;
        }
        return false;
    }

    public void incrementRun() {
        totalRuns++;
    }

    public void incrementProgramsUploaded() {
        programsUploaded++;
    }

    public void incrementFunctionsUploaded(int count) {
        functionsUploaded += count;
    }

    public int getUsedCredits() { return usedCredits; }
    public int getProgramsUploaded() { return programsUploaded; }
    public int getFunctionsUploaded() { return functionsUploaded; }
    public int getTotalRuns() { return totalRuns; }
}