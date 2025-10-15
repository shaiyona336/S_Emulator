package dtos;

import java.util.List;

public record DebugStepDetails(
        List<VariableDetails> variables,
        int currentInstructionNumber,
        boolean isFinished,
        int cyclesConsumed,
        long yValue
) {
    public int totalCycles() {
        return cyclesConsumed;
    }
}