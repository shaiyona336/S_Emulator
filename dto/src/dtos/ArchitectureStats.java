// dto/src/dtos/ArchitectureStats.java
package dtos;

import java.util.Map;

public record ArchitectureStats(
        Map<String, Integer> instructionCountByArchitecture,  // "GENERATION_I" -> count
        String minimumRequiredArchitecture,
        boolean canRunOnArchitecture
) {}