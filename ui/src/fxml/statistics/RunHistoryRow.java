package fxml.statistics;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class RunHistoryRow {
    private final SimpleIntegerProperty runNumber;
    private final SimpleIntegerProperty degree;
    private final SimpleStringProperty inputs;
    private final SimpleLongProperty outputY;
    private final SimpleIntegerProperty cycles;

    public RunHistoryRow(int runNumber, int degree, String inputs, long outputY, int cycles) {
        this.runNumber = new SimpleIntegerProperty(runNumber);
        this.degree = new SimpleIntegerProperty(degree);
        this.inputs = new SimpleStringProperty(inputs);
        this.outputY = new SimpleLongProperty(outputY);
        this.cycles = new SimpleIntegerProperty(cycles);
    }

    public int getRunNumber() { return runNumber.get(); }
    public int getDegree() { return degree.get(); }
    public String getInputs() { return inputs.get(); }
    public long getOutputY() { return outputY.get(); }
    public int getCycles() { return cycles.get(); }
}