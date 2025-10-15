// engine/src/components/architecture/ArchitectureAnalyzer.java
package components.architecture;

import components.instruction.Instruction;
import dtos.ArchitectureStats;

import java.util.*;

public class ArchitectureAnalyzer {

    public static ArchitectureStats analyzeProgram(List<Instruction> instructions, Architecture targetArchitecture) {
        Map<String, Integer> countByArch = new HashMap<>();

        // Initialize counts
        for (Architecture arch : Architecture.values()) {
            countByArch.put(arch.name(), 0);
        }

        // Count instructions by required architecture
        Architecture minimumRequired = Architecture.GENERATION_I;
        for (Instruction inst : instructions) {
            Architecture required = inst.getRequiredArchitecture();
            countByArch.put(required.name(), countByArch.get(required.name()) + 1);

            if (required.ordinal() > minimumRequired.ordinal()) {
                minimumRequired = required;
            }
        }

        boolean canRun = minimumRequired.ordinal() <= targetArchitecture.ordinal();

        return new ArchitectureStats(
                countByArch,
                minimumRequired.name(),
                canRun
        );
    }

    public static int calculateAverageCost(List<Instruction> instructions, Architecture architecture) {
        int totalCycles = instructions.stream()
                .mapToInt(Instruction::getCyclesNumber)
                .sum();
        return architecture.getCost() + totalCycles;
    }
}