package components.variable;

import components.label.Label;
import components.label.StandardLabel;

public class FreeWorkVariableGenerator {
    int nextSerialNumber;

    public FreeWorkVariableGenerator(int nextSerialNumber) {
        this.nextSerialNumber = nextSerialNumber;
    }

    public Variable getNextFreeWorkVariable() {
        Variable variable = new StandardVariable(StandardVariable.VariableType.WORK, nextSerialNumber);
        nextSerialNumber++;
        return variable;
    }
}
