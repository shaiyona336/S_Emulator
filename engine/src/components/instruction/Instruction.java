package components.instruction;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.label.FreeLabelGenerator;
import components.label.Label;
import components.program.Program;
import components.variable.FreeWorkVariableGenerator;
import components.variable.Variable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface Instruction extends Serializable {
    String getName();
    Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor);
    int getCyclesNumber();
    int getDegree();
    Label getLabel();
    List<Label> getAllInvolvedLabels();
    // List<Instruction> getInstructions();
    Variable getVariable();
    List<Variable> getAllInvolvedVariables();
    String getStringInstruction();
    List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions);
    void setInstructionNumber(int instructionNumber);
    boolean hasAncientInstruction();
    Instruction getAncientInstruction();
    void setAncientInstruction(Instruction ancientInstruction);
    public char getInstructionTypeChar();


    Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap);


    int getDegree(Map<String, Program> functions);





}
