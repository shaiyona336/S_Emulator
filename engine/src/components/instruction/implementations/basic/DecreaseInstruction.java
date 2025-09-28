package components.instruction.implementations.basic;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
import components.label.FixedLabel;
import components.label.FreeLabelGenerator;
import components.label.Label;
import components.program.Program;
import components.variable.FreeWorkVariableGenerator;
import components.variable.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecreaseInstruction extends AbstractInstruction {
    public DecreaseInstruction(Variable variable) {
        super(InstructionSemantic.DECREASE, variable);
    }

    public DecreaseInstruction(Variable variable, Label label) {
        super(InstructionSemantic.DECREASE, variable, label);
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        long value = context.getVariableValue(getVariable());
        if (value > 0) {
            value--;
        }
        context.updateVariableValue(getVariable(), value);
        return FixedLabel.EMPTY;
    }

    @Override
    public String getStringInstruction() {
        String variable = this.getVariable().getStringVariable();
        String command = String.format("%s <- %s - 1", variable, variable);

        return getInstructionDisplay(command);
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions) { // UPDATED SIGNATURE
        Instruction newInstruction = new DecreaseInstruction(getVariable(), getLabel());
        newInstruction.setAncientInstruction(getAncientInstruction());
        return List.of(newInstruction);
    }


    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newVar = varMap.getOrDefault(getVariable(), getVariable());
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        return new DecreaseInstruction(newVar, newLabel);
    }

    @Override
    public int getDegree(Map<String, Program> functions) {
        return 0;
    }
}
