package dtos;

import components.executor.Context;

public record DebugStepDetails(
        Context context,
        int nextInstructionNumber,
        boolean isFinished,
        int creditsRemaining,
        int cyclesConsumedThisStep
) {}