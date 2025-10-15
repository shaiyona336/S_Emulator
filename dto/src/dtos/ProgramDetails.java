package dtos;

import components.architecture.InstructionGeneration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public record ProgramDetails(
        String programName,
        List<InstructionDetails> instructions,
        List<LabelDetails> labels,
        List<VariableDetails> inputVariables,
        List<VariableDetails> workVariables,
        List<BreakpointDetails> breakpoints,
        Map<String, Integer> architectureInstructionCounts
) {

    public static ProgramDetails createWithArchitectureCounts(
            String programName,
            List<InstructionDetails> instructions,
            List<LabelDetails> labels,
            List<VariableDetails> inputVariables,
            List<VariableDetails> workVariables,
            List<BreakpointDetails> breakpoints) {

        Map<String, Integer> archCounts = calculateArchitectureCounts(instructions);

        return new ProgramDetails(
                programName,
                instructions,
                labels,
                inputVariables,
                workVariables,
                breakpoints,
                archCounts
        );
    }

    private static Map<String, Integer> calculateArchitectureCounts(List<InstructionDetails> instructions) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("GEN_I", 0);
        counts.put("GEN_II", 0);
        counts.put("GEN_III", 0);
        counts.put("GEN_IV", 0);

        for (InstructionDetails instruction : instructions) {
            InstructionGeneration gen = InstructionGeneration.getGenerationForInstruction(
                    instruction.name()
            );
            if (gen != null) {
                String key = "GEN_" + gen.getGeneration();
                counts.put(key, counts.get(key) + 1);
            }
        }

        return counts;
    }
}