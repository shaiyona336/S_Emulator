package components.label;

public class LabelFactory {


    public static Label createLabelFromString(String labelString) {
        if (labelString == null || labelString.trim().isEmpty()) {
            return FixedLabel.EMPTY;
        }

        labelString = labelString.trim();

        if (labelString.equalsIgnoreCase("EXIT")) {
            return FixedLabel.EXIT;
        }

        if (labelString.toUpperCase().startsWith("L")) {
            int serial = Integer.parseInt(labelString.substring(1));
            return new StandardLabel(serial);
        }

        throw new IllegalArgumentException("Unknown label format in XML: " + labelString);
    }
}