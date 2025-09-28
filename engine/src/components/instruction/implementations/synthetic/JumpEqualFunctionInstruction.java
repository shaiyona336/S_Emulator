package components.instruction.implementations.synthetic;

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
import components.variable.VariableFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JumpEqualFunctionInstruction extends AbstractInstruction {

    private final Label jumpLabel;
    private final String functionName;
    private final List<String> functionArguments;

    public JumpEqualFunctionInstruction(Variable variable, Label jumpLabel, String functionName, List<String> args, Label instructionLabel) {
        super(InstructionSemantic.JUMP_EQUAL_VARIABLE, variable, instructionLabel);
        this.jumpLabel = jumpLabel;
        this.functionName = functionName;
        this.functionArguments = args;
    }

    @Override
    public Label execute(Context context, Map<String, Program> functions, ProgramExecutor executor) {
        Program functionToRun = functions.get(functionName);
        Long[] evaluatedArgs = new Long[functionArguments.size()];
        for (int i = 0; i < functionArguments.size(); i++) {
            evaluatedArgs[i] = executor.evaluateArgument(functionArguments.get(i));
        }

        ProgramExecutor subExecutor = new ProgramExecutor(functionToRun, functions);
        long functionResult = subExecutor.run(evaluatedArgs);
        executor.addCycles(subExecutor.getCyclesNumber());

        long variableValue = context.getVariableValue(getVariable());

        if (variableValue == functionResult) {
            return jumpLabel;
        }
        return FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(FreeLabelGenerator labelGenerator, FreeWorkVariableGenerator workVarGenerator, Map<String, Program> functions) {
        List<Instruction> instructions = new ArrayList<>();

        Variable tempResultVar = workVarGenerator.getNextFreeWorkVariable();


        Instruction quote = new QuoteInstruction(getLabel(), tempResultVar, this.functionName, this.functionArguments);
        instructions.addAll(quote.expand(labelGenerator, workVarGenerator, functions));

        Instruction jump = new JumpEqualVariableInstruction(getVariable(), this.jumpLabel, tempResultVar);
        instructions.addAll(jump.expand(labelGenerator, workVarGenerator, functions));

        for (Instruction inst : instructions) {
            inst.setAncientInstruction(this);
        }

        return instructions;
    }

    @Override
    public String getStringInstruction() {
        String args = String.join(",", functionArguments);
        String command = String.format("IF %s = (%s,%s) GOTO %s", getVariable().getStringVariable(), functionName, args, jumpLabel.getStringLabel());
        return getInstructionDisplay(command);
    }

    @Override
    public Instruction rename(Map<Variable, Variable> varMap, Map<Label, Label> labelMap) {
        Variable newVar = varMap.getOrDefault(getVariable(), getVariable());
        Label newLabel = labelMap.getOrDefault(getLabel(), getLabel());
        Label newJumpLabel = labelMap.getOrDefault(this.jumpLabel, this.jumpLabel);
        List<String> newArgs = this.functionArguments.stream()
                .map(arg -> {
                    if (!arg.trim().startsWith("(")) {
                        Variable oldVar = VariableFactory.createVariableFromString(arg);
                        return varMap.getOrDefault(oldVar, oldVar).getStringVariable();
                    }
                    return arg;
                })
                .collect(Collectors.toList());

        return new JumpEqualFunctionInstruction(newVar, newJumpLabel, this.functionName, newArgs, newLabel);
    }

    @Override
    public int getDegree(Map<String, Program> functions) {
        Program p = functions.get(this.functionName);
        if (p != null) {
            return 1 + p.calculateMaxDegree(functions);
        }
        return 1;
    }
}