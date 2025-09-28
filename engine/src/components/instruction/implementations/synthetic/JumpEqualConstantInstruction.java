package components.instruction.implementations.synthetic;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
import components.instruction.implementations.basic.DecreaseInstruction;
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

public class JumpEqualConstantInstruction extends AbstractInstruction {
    private final Label JEConstantLabel;
    private final int constantValue;

    public JumpEqualConstantInstruction(Variable variable, Label JEConstantLabel, int constantValue) {
        this(variable, JEConstantLabel, constantValue, FixedLabel.EMPTY);
    }

    public JumpEqualConstantInstruction(Variable variable, Label JEConstantLabel, int constantValue, Label label) {
        super(InstructionSemantic.JUMP_EQUAL_CONSTANT, variable, label);
        this.JEConstantLabel = JEConstantLabel;
        this.constantValue = constantValue;
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        long value = context.getVariableValue(getVariable());
        if (value == constantValue) {
            return JEConstantLabel;
        }
        return FixedLabel.EMPTY;
    }

    @Override
    public String getStringInstruction() {
        String variable = this.getVariable().getStringVariable();
        String command = String.format("IF %s = %d GOTO %s", variable, constantValue, JEConstantLabel.getStringLabel());
        return getInstructionDisplay(command);
    }

    @Override
    public List<Label> getAllInvolvedLabels() {
        return List.of(getLabel(), JEConstantLabel);
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVariableGenerator, Map<String, Program> functions) { // UPDATED SIGNATURE
        List <Instruction> instructions = new ArrayList<>();
        Variable v =  this.getVariable();
        Variable z1 = workVariableGenerator.getNextFreeWorkVariable();
        Label l = this.getLabel();
        Label l1 = labelGenerator.getNextFreeLabel();

        instructions.add(new AssignmentInstruction(z1, v, l));
        for (int i = 0; i < constantValue; i++)
        {
            instructions.add(new JumpZeroInstruction(z1, l1));
            instructions.add(new DecreaseInstruction(z1));
        }
        instructions.add(new JumpNotZeroInstruction(z1, l1));
        instructions.add(new GotoLabelInstruction(JEConstantLabel));
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
        Label newJECLabel = labelMap.getOrDefault(this.JEConstantLabel, this.JEConstantLabel);
        return new JumpEqualConstantInstruction(newVar, newJECLabel, this.constantValue, newLabel);
    }

    @Override
    public int getDegree(Map<String, Program> functions) {
        return 1;
    }
}