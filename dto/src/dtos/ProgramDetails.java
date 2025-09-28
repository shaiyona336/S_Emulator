package dtos;

import components.instruction.Instruction;
import components.label.Label;
import components.variable.Variable;

import java.util.List;

public record ProgramDetails(String name, List<Variable> inputVariables, List<Variable> workVariables, List<Label> labels, List<Instruction> instructions) {}