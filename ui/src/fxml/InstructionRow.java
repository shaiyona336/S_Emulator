package fxml;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class InstructionRow {
    private final SimpleIntegerProperty number;
    private final SimpleStringProperty type;
    private final SimpleStringProperty label;
    private final SimpleStringProperty instructionText;
    private final SimpleIntegerProperty cycles;

    public InstructionRow(int number, String type, String label, String instructionText, int cycles) {
        this.number = new SimpleIntegerProperty(number);
        this.type = new SimpleStringProperty(type);
        this.label = new SimpleStringProperty(label);
        this.instructionText = new SimpleStringProperty(instructionText);
        this.cycles = new SimpleIntegerProperty(cycles);
    }

    public int getNumber() { return number.get(); }
    public String getType() { return type.get(); }
    public String getLabel() { return label.get(); }
    public String getInstructionText() { return instructionText.get(); }
    public int getCycles() { return cycles.get(); }
}