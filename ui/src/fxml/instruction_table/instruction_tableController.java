package fxml.instruction_table;

import dtos.InstructionDetails;
import dtos.ProgramDetails;
import fxml.instruction_history.instruction_historyController;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class instruction_tableController {

    @FXML private TableView<InstructionRow> instructionTableView;
    @FXML private TableColumn<InstructionRow, Integer> indexColumn;
    @FXML private TableColumn<InstructionRow, Integer> instructionNumberColumn;
    @FXML private TableColumn<InstructionRow, Integer> cyclesColumn;
    @FXML private TableColumn<InstructionRow, String> nameColumn;
    @FXML private TableColumn<InstructionRow, String> operand1Column;
    @FXML private TableColumn<InstructionRow, String> operand2Column;
    @FXML private TableColumn<InstructionRow, String> architectureColumn;  // NEW

    private instruction_historyController historyController;
    private String currentHighlightTerm = "";

    @FXML
    public void initialize() {
        indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
        instructionNumberColumn.setCellValueFactory(new PropertyValueFactory<>("instructionNumber"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        operand1Column.setCellValueFactory(new PropertyValueFactory<>("operand1"));
        operand2Column.setCellValueFactory(new PropertyValueFactory<>("operand2"));
        architectureColumn.setCellValueFactory(new PropertyValueFactory<>("architecture"));  // NEW

        // Row factory for highlighting
        instructionTableView.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && shouldHighlight(newItem)) {
                    row.setStyle("-fx-background-color: yellow;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
    }

    public void setHistoryController(instruction_historyController historyController) {
        this.historyController = historyController;
    }

    public void loadProgramData(ProgramDetails programDetails) {
        List<InstructionRow> rows = new ArrayList<>();
        for (InstructionDetails instruction : programDetails.instructions()) {
            rows.add(new InstructionRow(
                    instruction.index(),
                    instruction.instructionNumber(),
                    0,
                    instruction.name(),
                    instruction.operand1(),
                    instruction.operand2(),
                    instruction.architecture()  // NEW
            ));
        }
        instructionTableView.setItems(FXCollections.observableArrayList(rows));
    }

    public void highlightTerm(String term) {
        currentHighlightTerm = term;
        instructionTableView.refresh();
    }

    public void highlightInstruction(int instructionNumber) {
        for (InstructionRow row : instructionTableView.getItems()) {
            if (row.getInstructionNumber() == instructionNumber) {
                instructionTableView.getSelectionModel().select(row);
                instructionTableView.scrollTo(row);
                break;
            }
        }
    }

    public void clearInstructionHighlight() {
        instructionTableView.getSelectionModel().clearSelection();
    }

    private boolean shouldHighlight(InstructionRow row) {
        if (currentHighlightTerm == null || currentHighlightTerm.isEmpty()) {
            return false;
        }
        return row.getName().contains(currentHighlightTerm) ||
                row.getOperand1().contains(currentHighlightTerm) ||
                row.getOperand2().contains(currentHighlightTerm);
    }

    public static class InstructionRow {
        private final SimpleIntegerProperty index;
        private final SimpleIntegerProperty instructionNumber;
        private final SimpleIntegerProperty cycles;
        private final SimpleStringProperty name;
        private final SimpleStringProperty operand1;
        private final SimpleStringProperty operand2;
        private final SimpleStringProperty architecture;

        public InstructionRow(int index, int instructionNumber, int cycles, String name,
                              String operand1, String operand2, String architecture) {
            this.index = new SimpleIntegerProperty(index);
            this.instructionNumber = new SimpleIntegerProperty(instructionNumber);
            this.cycles = new SimpleIntegerProperty(cycles);
            this.name = new SimpleStringProperty(name);
            this.operand1 = new SimpleStringProperty(operand1);
            this.operand2 = new SimpleStringProperty(operand2);
            this.architecture = new SimpleStringProperty(architecture);
        }

        // Getters
        public int getIndex() { return index.get(); }
        public int getInstructionNumber() { return instructionNumber.get(); }
        public int getCycles() { return cycles.get(); }
        public String getName() { return name.get(); }
        public String getOperand1() { return operand1.get(); }
        public String getOperand2() { return operand2.get(); }
        public String getArchitecture() { return architecture.get(); }
    }
}