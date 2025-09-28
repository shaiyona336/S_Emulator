package components.instruction.implementations.synthetic;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
import components.instruction.implementations.basic.IncreaseInstruction;
import components.label.FixedLabel;
import components.label.FreeLabelGenerator;
import components.label.Label;
import components.program.Program; // Add import
import components.variable.FreeWorkVariableGenerator;
import components.variable.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Add import

public class ConstantAssignmentInstruction extends AbstractInstruction {
    private final int constantValue;

    public ConstantAssignmentInstruction(Variable variable, int constantValue) {
        this(variable, constantValue, FixedLabel.EMPTY);
    }

    public ConstantAssignmentInstruction(Variable variable, int constantValue, Label label) {
        super(InstructionSemantic.CONSTANT_ASSIGNMENT, variable, label);
        this.constantValue = constantValue;
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        context.updateVariableValue(getVariable(), constantValue);
        return FixedLabel.EMPTY;
    }

    @Override
    public String getStringInstruction() {
        String variable = this.getVariable().getStringVariable();
        String command = String.format("%s <- %d", variable, constantValue);
        return getInstructionDisplay(command);
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions) { // UPDATED SIGNATURE
        List<Instruction> instructions = new ArrayList<>();
        Variable v =  this.getVariable();
        Label l = this.getLabel();

        instructions.add(new ZeroVariableInstruction(v, l));
        for (int i = 0; i < constantValue; i++)
        {
            instructions.add(new IncreaseInstruction(v));
        }

        for (Instruction instruction : instructions) {
            instruction.setAncientInstruction(this);
        }

        return instructions;
    }

    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newVar = varMap.getOrDefault(getVariable(), getVariable());
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        return new ConstantAssignmentInstruction(newVar, this.constantValue, newLabel);
    }

    @Override
    public int getDegree(Map<String, Program> functions) {
        return 1;
    }
}