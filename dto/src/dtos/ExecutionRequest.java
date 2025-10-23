package dtos;

public record ExecutionRequest(
        int degree,
        Long[] inputs,
        String architecture
) {}