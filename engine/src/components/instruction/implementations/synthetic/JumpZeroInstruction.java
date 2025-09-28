package components.instruction.implementations.synthetic;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
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

public class JumpZeroInstruction extends AbstractInstruction {
    private final Label JZLabel;

    public JumpZeroInstruction(Variable variable, Label JZLabel) {
        this(variable, JZLabel, FixedLabel.EMPTY);
    }

    public JumpZeroInstruction(Variable variable, Label JZLabel, Label label) {
        super(InstructionSemantic.JUMP_ZERO, variable, label);
        this.JZLabel = JZLabel;
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        long value = context.getVariableValue(getVariable());
        if (value == 0) {
            return JZLabel;
        }
        return FixedLabel.EMPTY;
    }

    @Override
    public String getStringInstruction() {
        String variable = this.getVariable().getStringVariable();
        String command = String.format("IF %s = 0 GOTO %s", variable, JZLabel.getStringLabel());
        return getInstructionDisplay(command);
    }

    @Override
    public List<Label> getAllInvolvedLabels() {
        return List.of(getLabel(), JZLabel);
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions) { // UPDATED SIGNATURE
        List<Instruction> instructions = new ArrayList<>();
        Variable v = this.getVariable();
        Label l =  this.getLabel();
        Label l1 = labelGenerator.getNextFreeLabel();

        instructions.add(new JumpNotZeroInstruction(v, l1, l));
        instructions.add(new GotoLabelInstruction(JZLabel));
        instructions.add(new NeutralInstruction(Variable.OUTPUT, l1));

        for (Instruction instruction : instructions) {
            instruction.setAncientInstruction(this);
        }

        return instructions;
    }


    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newVar = varMap.getOrDefault(getVariable(), getVariable());
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        Label newJZLabel = labelMap.getOrDefault(this.JZLabel, this.JZLabel);
        return new JumpZeroInstruction(newVar, newJZLabel, newLabel);
    }

    @Override
    public int getDegree(Map<String, Program> functions) {
        return 1;
    }
}