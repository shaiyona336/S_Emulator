package components.variable;

public class VariableFactory {
    public static Variable createVariableFromString(String varString) {
        if (varString == null || varString.trim().isEmpty()) {
            return Variable.EMPTY;
        }

        varString = varString.trim();
        char firstChar = varString.charAt(0);

        return switch (firstChar) {
            case 'x' -> new StandardVariable(StandardVariable.VariableType.INPUT, Integer.parseInt(varString.substring(1)));
            case 'z' -> new StandardVariable(StandardVariable.VariableType.WORK, Integer.parseInt(varString.substring(1)));
            case 'y' -> Variable.OUTPUT;
            default -> throw new IllegalArgumentException("Unknown variable type in XML: " + varString);
        };
    }
}