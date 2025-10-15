package components.architecture;

import components.instruction.InstructionSemantic;
import java.io.Serializable;
import java.util.Set;

public enum Architecture implements Serializable {
    GENERATION_I(5, Set.of(
            InstructionSemantic.NEUTRAL,
            InstructionSemantic.INCREASE,
            InstructionSemantic.DECREASE,
            InstructionSemantic.JUMP_NOT_ZERO
    )),
    GENERATION_II(100, Set.of(
            InstructionSemantic.NEUTRAL,
            InstructionSemantic.INCREASE,
            InstructionSemantic.DECREASE,
            InstructionSemantic.JUMP_NOT_ZERO,
            InstructionSemantic.ZERO_VARIABLE,
            InstructionSemantic.CONSTANT_ASSIGNMENT,
            InstructionSemantic.GOTO_LABEL
    )),
    GENERATION_III(500, Set.of(
            InstructionSemantic.NEUTRAL,
            InstructionSemantic.INCREASE,
            InstructionSemantic.DECREASE,
            InstructionSemantic.JUMP_NOT_ZERO,
            InstructionSemantic.ZERO_VARIABLE,
            InstructionSemantic.CONSTANT_ASSIGNMENT,
            InstructionSemantic.GOTO_LABEL,
            InstructionSemantic.ASSIGNMENT,
            InstructionSemantic.JUMP_ZERO,
            InstructionSemantic.JUMP_EQUAL_CONSTANT,
            InstructionSemantic.JUMP_EQUAL_VARIABLE
    )),
    GENERATION_IV(1000, Set.of(
            InstructionSemantic.NEUTRAL,
            InstructionSemantic.INCREASE,
            InstructionSemantic.DECREASE,
            InstructionSemantic.JUMP_NOT_ZERO,
            InstructionSemantic.ZERO_VARIABLE,
            InstructionSemantic.CONSTANT_ASSIGNMENT,
            InstructionSemantic.GOTO_LABEL,
            InstructionSemantic.ASSIGNMENT,
            InstructionSemantic.JUMP_ZERO,
            InstructionSemantic.JUMP_EQUAL_CONSTANT,
            InstructionSemantic.JUMP_EQUAL_VARIABLE,
            InstructionSemantic.QUOTE
    ));

    private final int cost;
    private final Set<InstructionSemantic> supportedInstructions;

    Architecture(int cost, Set<InstructionSemantic> supportedInstructions) {
        this.cost = cost;
        this.supportedInstructions = supportedInstructions;
    }

    public int getCost() {
        return cost;
    }

    public boolean supports(InstructionSemantic semantic) {
        return supportedInstructions.contains(semantic);
    }

    public Set<InstructionSemantic> getSupportedInstructions() {
        return supportedInstructions;
    }

    public String getDisplayName() {
        return switch (this) {
            case GENERATION_I -> "Generation I";
            case GENERATION_II -> "Generation II";
            case GENERATION_III -> "Generation III";
            case GENERATION_IV -> "Generation IV";
        };
    }

    public static Architecture getMinimumRequired(InstructionSemantic semantic) {
        for (Architecture arch : values()) {
            if (arch.supports(semantic)) {
                return arch;
            }
        }
        return GENERATION_IV;
    }
}