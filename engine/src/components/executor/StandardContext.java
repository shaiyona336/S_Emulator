package components.executor;

import components.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class StandardContext implements Context {

    private final Map<Variable, Long> variables = new HashMap<>();
    private int totalCycles = 0; // Field to hold the cycle count

    @Override
    public long getVariableValue(Variable variable) {
        // Return the value of the variable, or 0 if it doesn't exist yet.
        return variables.getOrDefault(variable, 0L);
    }

    @Override
    public void updateVariableValue(Variable variable, long value) {
        variables.put(variable, value);
    }

    @Override
    public Map<Variable, Long> getVariables() {
        return variables;
    }


    @Override
    public int getTotalCycles() {
        return totalCycles;
    }


    public void setTotalCycles(int cycles) {
        this.totalCycles = cycles;
    }

    @Override
    public void addCycles(int cyclesToAdd) {
        this.totalCycles += cyclesToAdd;
    }
}