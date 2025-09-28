package components.instruction.implementations.synthetic;

import components.executor.ArgumentParser;
import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.AbstractInstruction;
import components.instruction.Instruction;
import components.instruction.InstructionSemantic;
import components.instruction.implementations.basic.NeutralInstruction;
import components.label.FixedLabel;
import components.label.FreeLabelGenerator;
import components.label.Label;
import components.program.Program;
import components.variable.FreeWorkVariableGenerator;
import components.variable.Variable;
import components.variable.VariableFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuoteInstruction extends AbstractInstruction {

    private final String functionName;
    private final List<String> rawArgumentStrings;

    public QuoteInstruction(Label label, Variable targetVariable, String functionName, List<String> argStrings) {
        super(InstructionSemantic.QUOTE, targetVariable, label);
        this.functionName = functionName;
        this.rawArgumentStrings = argStrings;
    }

    public String getFunctionName() { return functionName; }
    public List<String> getRawArgumentStrings() { return rawArgumentStrings; }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        Long[] evaluatedArguments = new Long[rawArgumentStrings.size()];
        for (int i = 0; i < rawArgumentStrings.size(); i++) {
            evaluatedArguments[i] = executor.evaluateArgument(rawArgumentStrings.get(i));
        }
        Program functionToExecute = functions.get(functionName);
        if (functionToExecute == null) {
            throw new IllegalStateException("Function '" + functionName + "' is not defined.");
        }
        ProgramExecutor subExecutor = new ProgramExecutor(functionToExecute, functions);
        Long result = subExecutor.run(evaluatedArguments);
        executor.addCycles(subExecutor.getCyclesNumber());
        context.updateVariableValue(this.getVariable(), result);
        return FixedLabel.EMPTY;
    }


    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVarGenerator, Map<String, Program> functions) {
        List<Instruction> expandedInstructions = new ArrayList<>();
        List<Variable> finalArguments = new ArrayList<>();

        //recursively expand any arguments that are themselves function calls
        for (String rawArg : this.rawArgumentStrings) {
            if (rawArg.trim().startsWith("(")) {
                Variable tempResultVar = workVarGenerator.getNextFreeWorkVariable();
                Instruction tempQuote = createTemporaryQuote(rawArg, tempResultVar);
                expandedInstructions.addAll(tempQuote.expand(labelGenerator, workVarGenerator, functions));
                finalArguments.add(tempResultVar);
            } else {
                finalArguments.add(VariableFactory.createVariableFromString(rawArg));
            }
        }

        //expand the main function call using the simplified arguments
        Program functionToExpand = functions.get(this.functionName);
        Map<Variable, Variable> varMap = createVariableMapping(functionToExpand, workVarGenerator, functions);
        Map<Label, Label> labelMap = createLabelMapping(functionToExpand, labelGenerator, functions);

        expandedInstructions.addAll(createArgumentPassingInstructions(functionToExpand, finalArguments, varMap, functions));
        expandedInstructions.addAll(getRenamedFunctionBody(functionToExpand, varMap, labelMap));
        if (labelMap.containsKey(FixedLabel.EXIT)) {
            Label exitLabel = labelMap.get(FixedLabel.EXIT);
            expandedInstructions.add(new NeutralInstruction(Variable.EMPTY, exitLabel));
        }
        expandedInstructions.add(createReturnAssignment(this.getVariable(), varMap));

        for (Instruction inst : expandedInstructions) {
            inst.setAncientInstruction(this);
        }

        return expandedInstructions;
    }

    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newTargetVar = varMap.getOrDefault(getVariable(), getVariable());
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        List<String> newRawArgumentStrings = this.rawArgumentStrings.stream()
                .map(arg -> {
                    if (!arg.trim().startsWith("(")) {
                        Variable oldVar = VariableFactory.createVariableFromString(arg);
                        return varMap.getOrDefault(oldVar, oldVar).getStringVariable();
                    }
                    return arg;
                })
                .collect(Collectors.toList());
        return new QuoteInstruction(newLabel, newTargetVar, this.functionName, newRawArgumentStrings);
    }


    private Instruction createTemporaryQuote(String rawArg, Variable targetVar) {
        String inner = rawArg.trim().substring(1, rawArg.length() - 1);
        int firstComma = inner.indexOf(',');
        String funcName = (firstComma == -1) ? inner : inner.substring(0, firstComma).trim();
        String argsStr = (firstComma == -1) ? "" : inner.substring(firstComma + 1).trim();
        List<String> args = ArgumentParser.parseArguments(argsStr);
        return new QuoteInstruction(FixedLabel.EMPTY, targetVar, funcName, args);
    }

    private Map<Variable, Variable> createVariableMapping(Program func, FreeWorkVariableGenerator wg, Map<String, Program> funcs) {
        Map<Variable, Variable> map = new HashMap<>();

        //map input variables to new work variables
        func.getInputVariables(funcs).forEach(v -> map.put(v, wg.getNextFreeWorkVariable()));

        //map work variables to new work variables
        func.getWorkVariables(funcs).forEach(v -> map.put(v, wg.getNextFreeWorkVariable()));

        //map OUTPUT to a NEW work variable, NOT the target variable
        //the target variable will get the value via createReturnAssignment
        map.put(Variable.OUTPUT, wg.getNextFreeWorkVariable());

        return map;
    }

    private Map<Label, Label> createLabelMapping(Program func, FreeLabelGenerator lg, Map<String, Program> funcs) {
        Map<Label, Label> map = new HashMap<>();

        //map all the function's normal labels to new labels
        func.getLabels(funcs).forEach(l -> {
            if (l != FixedLabel.EXIT) {
                map.put(l, lg.getNextFreeLabel());
            }
        });
        //map EXIT to a new label that we will add at the end
        Label exitLabel = lg.getNextFreeLabel();
        map.put(FixedLabel.EXIT, exitLabel);

        return map;
    }

    private List<Instruction> createArgumentPassingInstructions(Program func, List<Variable> args, Map<Variable, Variable> varMap, Map<String, Program> funcs) {
        List<Instruction> instructions = new ArrayList<>();
        List<Variable> targetVars = func.getInputVariables(funcs);
        for (int i = 0; i < targetVars.size(); i++) {
            Variable newVar = varMap.get(targetVars.get(i));
            Variable sourceVar = args.get(i);
            instructions.add(new AssignmentInstruction(newVar, sourceVar));
        }
        return instructions;
    }

    private List<Instruction> getRenamedFunctionBody(Program func, Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        return func.getInstructions().stream()
                .map(inst -> inst.rename(varMap, labelMap))
                .collect(Collectors.toList());
    }

    private Instruction createReturnAssignment(Variable originalTarget, Map<Variable, Variable> varMap) {
        Variable renamedOutput = varMap.get(Variable.OUTPUT);
        return new AssignmentInstruction(originalTarget, renamedOutput);
    }

    @Override
    public String getStringInstruction() {
        String args = String.join(",", rawArgumentStrings);
        String command = String.format("%s <- (%s,%s)", getVariable().getStringVariable(), functionName, args);
        return getInstructionDisplay(command);
    }

    @Override
    public List<Variable> getAllInvolvedVariables() {
        List<Variable> vars = new ArrayList<>();
        vars.add(getVariable());
        return vars;
    }


    @Override
    public int getDegree(Map<String, Program> functions) {
        int selfDegree = 1;
        int maxArgDegree = 0;

        // Find the highest degree among all of its arguments
        for (String rawArg : this.getRawArgumentStrings()) {
            if (rawArg.trim().startsWith("(")) {
                // It's a nested function call, so find its degree recursively.
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
        return selfDegree + maxArgDegree;
    }
}