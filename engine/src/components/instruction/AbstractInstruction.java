package components.instruction;

import components.architecture.Architecture;  // ADD THIS IMPORT
import components.label.FixedLabel;
import components.label.Label;
import components.variable.Variable;

import java.util.List;
import java.util.Map;

public abstract class AbstractInstruction implements Instruction {
    private final InstructionSemantic instructionSemantic;
    private final Variable variable;
    private final Label label;
    private int instructionNumber;
    private Instruction ancientInstruction;

    public AbstractInstruction(InstructionSemantic instructionSemantic, Variable variable) {
        this(instructionSemantic, variable, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionSemantic instructionSemantic, Variable variable, Label label) {
        this.instructionSemantic = instructionSemantic;
        this.variable = variable;
        this.label = label;
    }

    public char getInstructionTypeChar() {
        return instructionSemantic.getInstructionTypeChar();
    }

    @Override
    public String getName() {
        return instructionSemantic.getName();
    }

    @Override
    public int getCyclesNumber() {
        return instructionSemantic.getCyclesNumber();
    }

    @Override
    public int getDegree() {
        return instructionSemantic.getDegree();
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }

    public String getInstructionDisplay(String command) {
        return String.format("#%d (%c) [ %-3s ] %s (%d)", instructionNumber,
                instructionSemantic.getInstructionTypeChar(), label.getStringLabel(), command, instructionSemantic.getCyclesNumber());
    }

    @Override
    public List<Label> getAllInvolvedLabels() {
        return List.of(getLabel());
    }

    @Override
    public List<Variable> getAllInvolvedVariables() {
        return List.of(getVariable());
    }

    @Override
    public void setInstructionNumber(int instructionNumber) {
        this.instructionNumber = instructionNumber;
    }

    @Override
    public boolean hasAncientInstruction() {
        return ancientInstruction != null;
    }

    @Override
    public Instruction getAncientInstruction() {
        return ancientInstruction;
    }

    @Override
    public void setAncientInstruction(Instruction ancientInstruction) {
        this.ancientInstruction = ancientInstruction;
    }

    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newVar = varMap.getOrDefault(this.getVariable(), this.getVariable());
        Label newLabel = labelMap.getOrDefault(this.getLabel(), this.getLabel());
        throw new UnsupportedOperationException("Rename method not implemented for " + this.getClass().getSimpleName());
    }

    @Override
    public Architecture getRequiredArchitecture() {
        return Architecture.getMinimumRequired(instructionSemantic);
    }
}