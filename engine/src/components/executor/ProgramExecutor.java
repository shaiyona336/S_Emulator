package components.executor;

import components.instruction.Instruction;
import components.label.FixedLabel;
import components.label.Label;
import components.program.Program;
import components.variable.Variable;
import components.variable.VariableFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramExecutor implements Executor {
    private final Program program;
    private final Map<String, Program> definedFunctions;
    private Context context;
    private int cyclesNumber;

    private int instructionPointer;
    private boolean isFinished;
    private Long[] initialInputs;
    private final Map<Label, Integer> labelToIndex = new HashMap<>();

    public ProgramExecutor(Program program, Map<String, Program> definedFunctions) {
        this.program = program;
        this.definedFunctions = definedFunctions;
        precomputeLabelLocations();
    }

    private void precomputeLabelLocations() {
        List<Instruction> instructions = program.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            Label label = instructions.get(i).getLabel();
            if (label != null && label != FixedLabel.EMPTY) {
                labelToIndex.put(label, i);
            }
        }
    }

    @Override
    public Long run(Long... input) {
        initializeDebugSession(input);
        return resume();
    }

    public void initializeDebugSession(Long[] inputs) {
        this.context = new StandardContext();
        this.initialInputs = inputs;
        this.cyclesNumber = 0;
        this.instructionPointer = 0;
        this.isFinished = false;
        initializeInputVariables(context, inputs);
    }

    public void stepOver() {
        if (isFinished) return;
        List<Instruction> instructions = program.getInstructions();
        if (instructionPointer >= instructions.size()) {
            isFinished = true;
            return;
        }
        Instruction currentInstruction = instructions.get(instructionPointer);
        Label nextInstructionLabel = currentInstruction.execute(context, this.definedFunctions, this);
        cyclesNumber += currentInstruction.getCyclesNumber();
        if (nextInstructionLabel == FixedLabel.EXIT) {
            isFinished = true;
        } else if (nextInstructionLabel == FixedLabel.EMPTY) {
            instructionPointer++;
        } else {
            instructionPointer = labelToIndex.getOrDefault(nextInstructionLabel, instructions.size());
        }
    }

    public Long resume() {
        while (!isFinished) {
            stepOver();
        }
        return context.getVariableValue(Variable.OUTPUT);
    }

    public Long evaluateArgument(String argString) {
        argString = argString.trim();
        if (!argString.startsWith("(")) {
            Variable argVar = VariableFactory.createVariableFromString(argString);
            return context.getVariableValue(argVar);
        }
        String innerContent = argString.substring(1, argString.length() - 1);
        int firstComma = innerContent.indexOf(',');
        String functionName;
        List<String> subArguments;
        if (firstComma == -1) {
            functionName = innerContent;
            subArguments = new ArrayList<>();
        } else {
            functionName = innerContent.substring(0, firstComma).trim();
            String subArgsString = innerContent.substring(firstComma + 1);
            subArguments = ArgumentParser.parseArguments(subArgsString);
        }
        Program functionToExecute = definedFunctions.get(functionName);
        if (functionToExecute == null) {
            throw new IllegalStateException("Function '" + functionName + "' is not defined.");
        }
        Long[] subProgramInputs = new Long[subArguments.size()];
        for (int i = 0; i < subArguments.size(); i++) {
            subProgramInputs[i] = this.evaluateArgument(subArguments.get(i));
        }
        ProgramExecutor subExecutor = new ProgramExecutor(functionToExecute, this.definedFunctions);
        Long result = subExecutor.run(subProgramInputs);
        this.addCycles(subExecutor.getCyclesNumber());
        return result;
    }

    public void addCycles(int cyclesToAdd) {
        this.cyclesNumber += cyclesToAdd;
    }

    public boolean isFinished() { return isFinished; }
    public int getNextInstructionNumber() { return instructionPointer + 1; }
    public Long[] getInitialInputs() { return initialInputs; }

    @Override
    public Context getVariablesContext() {
        if (context instanceof StandardContext) {
            ((StandardContext) context).setTotalCycles(this.cyclesNumber);
        }
        return context;
    }

    public int getCyclesNumber() { return cyclesNumber; }

    private void initializeInputVariables(Context context, Long... input) {
        // --- THIS IS THE FIX ---
        // The definedFunctions map is now passed to getInputVariables to satisfy the interface.
        List<Variable> inputVariables = program.getInputVariables(this.definedFunctions);
        int i = 0;

        while (i < inputVariables.size() && i < input.length) {
            context.updateVariableValue(inputVariables.get(i), input[i]);
            i++;
        }

        while (i < inputVariables.size()) {
            context.updateVariableValue(inputVariables.get(i), 0L);
            i++;
        }
    }
}