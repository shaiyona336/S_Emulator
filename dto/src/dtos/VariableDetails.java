package dtos;

public record VariableDetails(String variable, long value) {
    public String getStringVariable() {
        return variable;
    }

    public long getValue() {
        return value;
    }
}