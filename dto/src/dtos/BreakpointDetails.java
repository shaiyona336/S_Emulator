package dtos;

public record BreakpointDetails(int instructionNumber) {
    public int getInstructionNumber() {
        return instructionNumber;
    }
}