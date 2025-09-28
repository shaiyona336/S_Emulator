package components.instruction.implementations.synthetic;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
import components.instruction.implementations.basic.DecreaseInstruction;
import components.instruction.implementations.basic.IncreaseInstruction;
import components.instruction.implementations.basic.JumpNotZeroInstruction;
import components.instruction.implementations.basic.NeutralInstruction;
import components.label.FixedLabel;
import components.label.FreeLabelGenerator;
import components.label.Label;
import components.program.Program; // Add import
import components.variable.FreeWorkVariableGenerator;
import components.variable.StandardVariable;
import components.variable.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Add import

public class AssignmentInstruction extends AbstractInstruction {
    private final Variable assignedVariable;

    public AssignmentInstruction(Variable variable, Variable assignedVariable) {
        this(variable, assignedVariable, FixedLabel.EMPTY);
    }

    public AssignmentInstruction(Variable variable, Variable assignedVariable, Label label) {
        super(InstructionSemantic.ASSIGNMENT, variable, label);
        this.assignedVariable = assignedVariable;
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        long value = context.getVariableValue(assignedVariable);
        context.updateVariableValue(getVariable(), value);
        return FixedLabel.EMPTY;
    }

    @Override
    public String getStringInstruction() {
        String variable = this.getVariable().getStringVariable();
        String command = String.format("%s <- %s", variable, assignedVariable.getStringVariable());
        return getInstructionDisplay(command);
    }

    @Override
    public List<Variable> getAllInvolvedVariables() {
        return List.of(getVariable(), assignedVariable);
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions) { // UPDATED SIGNATURE
        List<Instruction> instructions = new ArrayList<>();
        Variable v = this.getVariable();
        Variable z1 = workVariableGenerator.getNextFreeWorkVariable();
        Label l = this.getLabel();
        Label l1 = labelGenerator.getNextFreeLabel();
        Label l2 = labelGenerator.getNextFreeLabel();
        Label l3 = labelGenerator.getNextFreeLabel();

        instructions.add(new ZeroVariableInstruction(v, l));
        instructions.add(new JumpNotZeroInstruction(assignedVariable, l1));
        instructions.add(new GotoLabelInstruction(l3, FixedLabel.EMPTY));
        instructions.add(new DecreaseInstruction(assignedVariable, l1));
        instructions.add(new IncreaseInstruction(z1));
        instructions.add(new JumpNotZeroInstruction(assignedVariable, l1));
        instructions.add(new DecreaseInstruction(z1, l2));
        instructions.add(new IncreaseInstruction(v));
        instructions.add(new IncreaseInstruction(assignedVariable));
        instructions.add(new JumpNotZeroInstruction(z1, l2));
        instructions.add(new NeutralInstruction(v, l3));

        for (Instruction instruction : instructions) {
            instruction.setAncientInstruction(this);
        }

        return instructions;
    }

    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newTargetVar = varMap.getOrDefault(getVariable(), getVariable());
        Variable newAssignedVar = varMap.getOrDefault(this.assignedVariable, this.assignedVariable);
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        return new AssignmentInstruction(newTargetVar, newAssignedVar, newLabel);
    }


    @Override
    public int getDegree(Map<String, Program> functions) {
        return 1;
    }
}