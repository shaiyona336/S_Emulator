// dto/src/dtos/ExecutionRequest.java
package dtos;

public record ExecutionRequest(
        int degree,
        Long[] inputs,
        String architecture  // "GENERATION_I", "GENERATION_II", etc.
) {}