package components.program;

import components.instruction.Instruction;
import components.label.Label;
import components.variable.Variable;
import java.util.List;
import java.util.Map;

public interface Program {
    String getName();
    List<Instruction> getInstructions();
    void addInstruction(Instruction instruction);

    List<Variable> getInputVariables(Map<String, Program> functions);
    List<Variable> getWorkVariables(Map<String, Program> functions);
    List<Label> getLabels(Map<String, Program> functions);
    int calculateMaxDegree(Map<String, Program> functions);
    Program expand(Map<String, Program> functions);
    int getNextFreeLabelNumber(Map<String, Program> functions);
    int getNextFreeWorkVariableNumber(Map<String, Program> functions);
}