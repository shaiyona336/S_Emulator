package dtos;

import components.architecture.InstructionGeneration;

public record InstructionDetails(
        int index,
        int instructionNumber,
        String name,
        String operand1,
        String operand2,
        String architecture
) {
    public static InstructionDetails fromInstruction(
            int index,
            int instructionNumber,
            String name,
            String operand1,
            String operand2) {

        InstructionGeneration gen = InstructionGeneration.getGenerationForInstruction(name);
        String arch = gen != null ? gen.getGeneration() : "?";

        return new InstructionDetails(
                index,
                instructionNumber,
                name,
                operand1,
                operand2,
                arch
        );
    }

    // Accessor methods
    public String getName() { return name; }
    public int getInstructionNumber() { return instructionNumber; }
    public int cycles() { return 0; } // Placeholder - cycles are tracked during execution
}