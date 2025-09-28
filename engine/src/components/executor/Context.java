package components.executor;

import components.variable.Variable;

import java.util.Map;

public interface Context {
    long getVariableValue(Variable variable);
    void updateVariableValue(Variable variable, long value);
    Map<Variable, Long> getVariables();
    int getTotalCycles();
    void addCycles(int cyclesToAdd);
}
