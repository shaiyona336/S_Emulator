package fxml;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class VariableOutputRow {
    private final SimpleStringProperty name;
    private final SimpleStringProperty value;
    private final SimpleBooleanProperty changed;

    public VariableOutputRow(String name, String value) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.changed = new SimpleBooleanProperty(false);
    }

    public String getName() {
        return name.get();
    }

    public String getValue() {
        return value.get();
    }


    public boolean isChanged() { return changed.get(); }
    public void setChanged(boolean changed) { this.changed.set(changed); }

}