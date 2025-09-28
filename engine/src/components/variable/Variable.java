package components.variable;

import java.io.Serializable;

public interface Variable extends Serializable {
    String getStringVariable();
    StandardVariable.VariableType getVariableType();
    int getSerialNumber();

    Variable OUTPUT = new StandardVariable(StandardVariable.VariableType.OUTPUT, 0);
    Variable EMPTY = new StandardVariable(StandardVariable.VariableType.EMPTY, 0);
}
