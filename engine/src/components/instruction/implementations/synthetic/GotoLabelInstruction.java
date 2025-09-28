package components.instruction.implementations.synthetic;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
import components.instruction.implementations.basic.IncreaseInstruction;
import components.instruction.implementations.basic.JumpNotZeroInstruction;
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

public class GotoLabelInstruction extends AbstractInstruction {
    private final Label gotoLabel;

    public GotoLabelInstruction(Label gotoLabel) {
        this(gotoLabel, FixedLabel.EMPTY);
    }

    public GotoLabelInstruction(Label gotoLabel, Label label) {
        super(InstructionSemantic.GOTO_LABEL, Variable.EMPTY,  label);
        this.gotoLabel = gotoLabel;
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        return gotoLabel;
    }

    @Override
    public String getStringInstruction() {
        String command = String.format("GOTO %s", gotoLabel.getStringLabel());
        return getInstructionDisplay(command);
    }

    @Override
    public List<Label> getAllInvolvedLabels() {
        return List.of(getLabel(), gotoLabel);
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions) { // UPDATED SIGNATURE
        List<Instruction> instructions = new ArrayList<>();
        Variable z1 = workVariableGenerator.getNextFreeWorkVariable();
        Label l = this.getLabel();

        instructions.add(new IncreaseInstruction(z1, l));
        instructions.add(new JumpNotZeroInstruction(z1, gotoLabel));

        for (Instruction instruction : instructions) {
            instruction.setAncientInstruction(this);
        }

        return instructions;
    }


    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        // This instruction doesn't use a variable, so we only rename labels
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        Label newGotoLabel = labelMap.getOrDefault(this.gotoLabel, this.gotoLabel);
        return new GotoLabelInstruction(newGotoLabel, newLabel);
    }

    @Override
    public int getDegree(Map<String, Program> functions) {
        return 1;
    }
}