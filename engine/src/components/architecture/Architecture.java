package components.architecture;

public enum Architecture {
    GENERATION_I(5, "Generation I"),
    GENERATION_II(100, "Generation II"),
    GENERATION_III(500, "Generation III"),
    GENERATION_IV(1000, "Generation IV");

    private final int cost;
    private final String displayName;

    Architecture(int cost, String displayName) {
        this.cost = cost;
        this.displayName = displayName;
    }

    public int getCost() {
        return cost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean supportsInstruction(String instructionName) {
        InstructionGeneration gen = InstructionGeneration.getGenerationForInstruction(instructionName);
        if (gen == null) return false;

        return switch (this) {
            case GENERATION_I -> gen == InstructionGeneration.GEN_I;
            case GENERATION_II -> gen == InstructionGeneration.GEN_I || gen == InstructionGeneration.GEN_II;
            case GENERATION_III -> gen != InstructionGeneration.GEN_IV;
            case GENERATION_IV -> true;
        };
    }

    public static Architecture fromString(String str) {
        return switch (str) {
            case "Generation I", "GENERATION_I", "I" -> GENERATION_I;
            case "Generation II", "GENERATION_II", "II" -> GENERATION_II;
            case "Generation III", "GENERATION_III", "III" -> GENERATION_III;
            case "Generation IV", "GENERATION_IV", "IV" -> GENERATION_IV;
            default -> throw new IllegalArgumentException("Unknown architecture: " + str);
        };
    }
}