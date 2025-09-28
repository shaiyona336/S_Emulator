package components.program;

import components.executor.ArgumentParser;
import components.instruction.Instruction;
import components.instruction.implementations.synthetic.QuoteInstruction;
import components.label.FixedLabel;
import components.label.FreeLabelGenerator;
import components.label.Label;
import components.variable.FreeWorkVariableGenerator;
import components.variable.StandardVariable;
import components.variable.Variable;
import components.variable.VariableFactory;

import java.util.*;
import java.util.stream.Collectors;

public class StandardProgram implements Program {
    private final String name;
    private final List<Instruction> instructions;
    private int nextInstructionNumber;

    public StandardProgram(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.nextInstructionNumber = 0;
    }

    public StandardProgram(String name, List<Instruction> instructions) {
        this.name = name;
        this.instructions = instructions;
        for(int i = 0; i < instructions.size(); i++) {
            instructions.get(i).setInstructionNumber(i + 1);
        }
        this.nextInstructionNumber = instructions.size();
    }

    @Override
    public String getName() { return name; }

    @Override
    public List<Instruction> getInstructions() { return instructions; }

    @Override
    public void addInstruction(Instruction instruction) {
        instruction.setInstructionNumber(++nextInstructionNumber);
        instructions.add(instruction);
    }

    @Override
    public List<Variable> getInputVariables(Map<String, Program> functions) {
        Set<Variable> variables = new HashSet<>();
        for (Instruction instruction : instructions) {
            if (instruction instanceof QuoteInstruction quote) {
                for (String rawArg : quote.getRawArgumentStrings()) {
                    findInputsInArgument(rawArg, functions, variables);
                }
            } else {
                for (Variable variable : instruction.getAllInvolvedVariables()) {
                    if (variable.getVariableType() == StandardVariable.VariableType.INPUT) {
                        variables.add(variable);
                    }
                }
            }
        }
        return variables.stream()
                .sorted(Comparator.comparingInt(Variable::getSerialNumber))
                .collect(Collectors.toList());
    }

    private void findInputsInArgument(String arg, Map<String, Program> functions, Set<Variable> inputs) {
        arg = arg.trim();
        if (!arg.startsWith("(")) {
            Variable v = VariableFactory.createVariableFromString(arg);
            if (v.getVariableType() == StandardVariable.VariableType.INPUT) {
                inputs.add(v);
            }
            return;
        }

        String innerContent = arg.substring(1, arg.length() - 1);
        int firstComma = innerContent.indexOf(',');

        if (firstComma == -1) {
            Program function = functions.get(innerContent);
            if (function != null) {
                inputs.addAll(function.getInputVariables(functions));
            }
        } else {
            String subArgsString = innerContent.substring(firstComma + 1);
            List<String> subArguments = ArgumentParser.parseArguments(subArgsString);
            for (String subArg : subArguments) {
                findInputsInArgument(subArg, functions, inputs);
            }
        }
    }

    @Override
    public int calculateMaxDegree(Map<String, Program> functions) {
        int maxDegree = 0;
        for (Instruction instruction : instructions) {
            int currentInstructionDegree;
            if (instruction instanceof QuoteInstruction quote) {
                // The degree of a quote is 1 (for itself) PLUS the HIGHEST degree found among its arguments.
                int maxArgDegree = 0;
                for (String rawArg : quote.getRawArgumentStrings()) {
                    if (rawArg.trim().startsWith("(")) {
                        String inner = rawArg.trim().substring(1, rawArg.length() - 1);
                        int firstComma = inner.indexOf(',');
                        String funcName = (firstComma == -1) ? inner : inner.substring(0, firstComma).trim();
                        Program p = functions.get(funcName);
                        if (p != null) {
                            int argDegree = p.calculateMaxDegree(functions);
                            if (argDegree > maxArgDegree) {
                                maxArgDegree = argDegree;
                            }
                        }
                    }
                }
                currentInstructionDegree = 1 + maxArgDegree; // Use 1 for the QUOTE's own degree
            } else {
                currentInstructionDegree = instruction.getDegree();
            }

            if (currentInstructionDegree > maxDegree) {
                maxDegree = currentInstructionDegree;
            }
        }
        return maxDegree;
    }

    @Override
    public List<Variable> getWorkVariables(Map<String, Program> functions) {
        Set<Variable> variables = new HashSet<>();
        for (Instruction instruction : instructions) {
            if (instruction instanceof QuoteInstruction quote) {
                Program function = functions.get(quote.getFunctionName());
                if (function != null) {
                    variables.addAll(function.getWorkVariables(functions));
                }
            } else {
                for (Variable variable : instruction.getAllInvolvedVariables()) {
                    if (variable.getVariableType() == StandardVariable.VariableType.WORK) {
                        variables.add(variable);
                    }
                }
            }
        }
        return variables.stream()
                .sorted(Comparator.comparingInt(Variable::getSerialNumber))
                .collect(Collectors.toList());
    }

    @Override
    public List<Label> getLabels(Map<String, Program> functions) {
        Set<Label> labels = new HashSet<>();
        for (Instruction instruction : instructions) {
            if (instruction instanceof QuoteInstruction quote) {
                Program function = functions.get(quote.getFunctionName());
                if (function != null) {
                    labels.addAll(function.getLabels(functions));
                }
            } else {
                for (Label label : instruction.getAllInvolvedLabels()) {
                    if (label != FixedLabel.EMPTY) {
                        labels.add(label);
                    }
                }
            }
        }
        return labels.stream()
                .sorted(Comparator.comparingInt(Label::getSerialNumber))
                .collect(Collectors.toList());
    }

    @Override
    public Program expand(Map<String, Program> functions) {
        Program expandedProgram = new StandardProgram(name);
        FreeLabelGenerator nextFreeLabel = new FreeLabelGenerator(this.getNextFreeLabelNumber(functions));
        FreeWorkVariableGenerator nextFreeWorkVariable = new FreeWorkVariableGenerator(this.getNextFreeWorkVariableNumber(functions));

        for (Instruction instruction : instructions) {
            List<Instruction> currentExpand = instruction.expand(nextFreeLabel, nextFreeWorkVariable, functions);
            for (Instruction baseInstruction : currentExpand) {
                expandedProgram.addInstruction(baseInstruction);
            }
        }
        return expandedProgram;
    }

    @Override
    public int getNextFreeLabelNumber(Map<String, Program> functions) {
        int maxLabelNumber = 0;
        for (Label label : getLabels(functions)) {
            if (label.getSerialNumber() > maxLabelNumber && label != FixedLabel.EXIT) {
                maxLabelNumber = label.getSerialNumber();
            }
        }
        return maxLabelNumber + 1;
    }

    @Override
    public int getNextFreeWorkVariableNumber(Map<String, Program> functions) {
        int maxWorkVariableNumber = 0;
        Set<Variable> allVars = new HashSet<>();
        allVars.addAll(getWorkVariables(functions));
        allVars.addAll(getInputVariables(functions));

        for (Variable variable : allVars) {
            if (variable.getSerialNumber() > maxWorkVariableNumber) {
                maxWorkVariableNumber = variable.getSerialNumber();
            }
        }
        return maxWorkVariableNumber + 1;
    }
}