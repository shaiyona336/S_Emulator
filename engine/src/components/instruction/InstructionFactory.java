package components.instruction;

import components.executor.ArgumentParser;
import components.instruction.implementations.basic.*;
import components.instruction.implementations.synthetic.*;
import components.jaxb.generated.SInstructionArguments;
import components.label.Label;
import components.label.LabelFactory;
import components.variable.Variable;
import components.variable.VariableFactory;

import java.util.List;

public class InstructionFactory {

    public static Instruction createInstruction(String instructionName, Label label, Variable variable, SInstructionArguments sArgs) {

        switch (instructionName.toUpperCase()) {
            //basic instructions
            case "DECREASE":
                return new DecreaseInstruction(variable, label);
            case "INCREASE":
                return new IncreaseInstruction(variable, label);
            case "NEUTRAL":
                return new NeutralInstruction(variable, label);
            case "JUMP_NOT_ZERO":
                String jnzLabelStr = findArgumentValue(sArgs, "JNZLabel");
                Label jnzLabel = LabelFactory.createLabelFromString(jnzLabelStr);
                return new JumpNotZeroInstruction(variable, jnzLabel, label);

            //synthetic instructions
            case "ASSIGNMENT":
                String assignedVarStr = findArgumentValue(sArgs, "assignedVariable");
                Variable assignedVar = VariableFactory.createVariableFromString(assignedVarStr);
                return new AssignmentInstruction(variable, assignedVar, label);
            case "GOTO_LABEL":
                String gotoLabelStr = findArgumentValue(sArgs, "gotoLabel");
                Label gotoLabel = LabelFactory.createLabelFromString(gotoLabelStr);
                return new GotoLabelInstruction(gotoLabel, label);
            case "ZERO_VARIABLE":
                return new ZeroVariableInstruction(variable, label);
            case "CONSTANT_ASSIGNMENT":
                String constValStr = findArgumentValue(sArgs, "constantValue");
                int constValue = (int) Long.parseLong(constValStr);
                return new ConstantAssignmentInstruction(variable, constValue, label);
            case "JUMP_ZERO":
                String jzLabelStr = findArgumentValue(sArgs, "JZLabel"); // Ensure this name matches your XSD/XML
                Label jzLabel = LabelFactory.createLabelFromString(jzLabelStr);
                return new JumpZeroInstruction(variable, jzLabel, label);
            case "JUMP_EQUAL_CONSTANT":
                String jecLabelStr = findArgumentValue(sArgs, "JECLabel"); // Ensure this name matches
                String jecConstStr = findArgumentValue(sArgs, "constantValue");
                Label jecLabel = LabelFactory.createLabelFromString(jecLabelStr);
                int jecConst = (int) Long.parseLong(jecConstStr);
                return new JumpEqualConstantInstruction(variable, jecLabel, jecConst, label);
            case "JUMP_EQUAL_VARIABLE":
                String jevLabelStr = findArgumentValue(sArgs, "JEVLabel"); // Ensure this name matches
                String jevVarStr = findArgumentValue(sArgs, "variableName");
                Label jevLabel = LabelFactory.createLabelFromString(jevLabelStr);
                Variable jevVar = VariableFactory.createVariableFromString(jevVarStr);
                return new JumpEqualVariableInstruction(variable, jevLabel, jevVar, label);

            case "QUOTE":
                String functionName = findArgumentValue(sArgs, "functionName");
                String functionArgumentsStr = findArgumentValue(sArgs, "functionArguments");
                List<String> parsedArgs = ArgumentParser.parseArguments(functionArgumentsStr);
                return new QuoteInstruction(label, variable, functionName, parsedArgs);

            default:
                throw new IllegalArgumentException("Unknown or unhandled instruction name in factory: " + instructionName);
        }
    }

    private static String findArgumentValue(SInstructionArguments sArgs, String name) {
        if (sArgs == null || sArgs.getSInstructionArgument().isEmpty()) {
            throw new IllegalArgumentException("Instruction is missing required arguments.");
        }
        return sArgs.getSInstructionArgument().stream()
                .filter(arg -> arg.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing required argument named: '" + name + "'"))
                .getValue();
    }
}