package components.variable;

import java.util.Objects;

public class StandardVariable implements Variable {
    public enum VariableType {INPUT, WORK, OUTPUT, EMPTY}

    private final VariableType variableType;
    private final int serialNumber;

    public StandardVariable(VariableType variableType, int serialNumber) {
        this.variableType = variableType;
        this.serialNumber = serialNumber;
    }

    @Override
    public String getStringVariable() {
        return switch (variableType)
        {
            case INPUT -> "x" + serialNumber;
            case WORK -> "z" + serialNumber;
            case OUTPUT -> "y";
            case EMPTY -> "";
        };
    }

    @Override
    public VariableType getVariableType() {
        return variableType;
    }

    @Override
    public int getSerialNumber() {
        return serialNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StandardVariable that = (StandardVariable) o;
        return serialNumber == that.serialNumber && variableType == that.variableType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableType, serialNumber);
    }
}
