package dtos;

import components.executor.Context;

public record ExecutionDetails(ProgramDetails programDetails, Context variables, int cycles) {}
