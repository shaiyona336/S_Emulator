package components.architecture;

import java.util.HashMap;
import java.util.Map;

public enum InstructionGeneration {
    GEN_I("I"),
    GEN_II("II"),
    GEN_III("III"),
    GEN_IV("IV");

    private static final Map<String, InstructionGeneration> INSTRUCTION_MAP = new HashMap<>();

    static {
        // Generation I
        INSTRUCTION_MAP.put("NEUTRAL", GEN_I);
        INSTRUCTION_MAP.put("INCREASE", GEN_I);
        INSTRUCTION_MAP.put("DECREASE", GEN_I);
        INSTRUCTION_MAP.put("JUMP_NOT_ZERO", GEN_I);

        // Generation II
        INSTRUCTION_MAP.put("ZERO_VARIABLE", GEN_II);
        INSTRUCTION_MAP.put("CONSTANT_ASSIGNMENT", GEN_II);
        INSTRUCTION_MAP.put("GOTO_LABEL", GEN_II);

        // Generation III
        INSTRUCTION_MAP.put("ASSIGNMENT", GEN_III);
        INSTRUCTION_MAP.put("JUMP_ZERO", GEN_III);
        INSTRUCTION_MAP.put("JUMP_EQUAL_CONSTANT", GEN_III);
        INSTRUCTION_MAP.put("JUMP_EQUAL_VARIABLE", GEN_III);

        // Generation IV
        INSTRUCTION_MAP.put("QUOTE", GEN_IV);
        INSTRUCTION_MAP.put("JUMP_EQUAL_FUNCTION", GEN_IV);
    }

    private final String generation;

    InstructionGeneration(String generation) {
        this.generation = generation;
    }

    public String getGeneration() {
        return generation;
    }

    public static InstructionGeneration getGenerationForInstruction(String instructionName) {
        return INSTRUCTION_MAP.get(instructionName.toUpperCase());
    }
}