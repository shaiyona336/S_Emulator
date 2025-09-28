package components.instruction;

public enum InstructionSemantic {
    INCREASE("INCREASE", 1, InstructionType.BASIC, 0),
    DECREASE("DECREASE", 1,  InstructionType.BASIC, 0),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, InstructionType.BASIC, 0),
    NEUTRAL("NEUTRAL", 0, InstructionType.BASIC, 0),
    ZERO_VARIABLE("ZERO_VARIABLE", 1, InstructionType.SYNTHETIC, 1),
    GOTO_LABEL("GOTO_LABEL", 1, InstructionType.SYNTHETIC, 1),
    ASSIGNMENT("ASSIGNMENT", 4, InstructionType.SYNTHETIC, 2),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2, InstructionType.SYNTHETIC, 2),
    JUMP_ZERO("JUMP_ZERO", 2, InstructionType.SYNTHETIC, 2),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2, InstructionType.SYNTHETIC, 3),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2, InstructionType.SYNTHETIC, 3),
    QUOTE("QUOTE", 1, InstructionType.SYNTHETIC, 3),
    ;

    public enum InstructionType {
        BASIC,
        SYNTHETIC,
    }
    private final String name;
    private final int cyclesNumber;
    private final InstructionType instructionType;
    private final int degree;

    InstructionSemantic(String name, int cyclesNumber, InstructionType instructionType,  int degree) {
        this.name = name;
        this.cyclesNumber = cyclesNumber;
        this.instructionType = instructionType;
        this.degree = degree;
    }

    public String getName() {
        return name;
    }

    public int getCyclesNumber() {
        return cyclesNumber;
    }

    public char getInstructionTypeChar() {
        return switch (instructionType)
        {
            case BASIC -> 'B';
            case SYNTHETIC -> 'S';
        };
    }

    public int getDegree() {
        return degree;
    }
}
