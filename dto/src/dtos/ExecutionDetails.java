package dtos;

import java.util.List;

public record ExecutionDetails(
        ProgramDetails programDetails,
        List<VariableDetails> variables,
        int totalCycles,
        long yValue
) {
}