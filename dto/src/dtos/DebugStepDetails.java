package dtos;

import components.executor.Context;

public record DebugStepDetails(
        Context context,             // The state of all variables after the step
        int nextInstructionNumber,   // The 1-based number of the *next* instruction to be executed
        boolean isFinished           // A flag indicating if the program has completed
) {}

