// ui/src/fxml/instruction_table/instruction_tableController.java
package fxml.instruction_table;

import components.instruction.Instruction;
import dtos.ProgramDetails;
import fxml.instruction_history.instruction_historyController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import fxml.InstructionRow;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class instruction_tableController {

    @FXML private TableView<InstructionRow> instructionsTableView;
    @FXML private TableColumn<InstructionRow, Integer> numberColumn;
    @FXML private TableColumn<InstructionRow, String> typeColumn;
    @FXML private TableColumn<InstructionRow, String> labelColumn;
    @FXML private TableColumn<InstructionRow, String> instructionColumn;
    @FXML private TableColumn<InstructionRow, Integer> cyclesColumn;
    @FXML private TableColumn<InstructionRow, String> architectureColumn;  // NEW
    @FXML private Label summaryLabel;

    private String highlightedTerm = "";
    private ProgramDetails currentProgramDetails;
    private instruction_historyController historyController;

    @FXML
    public void initialize() {
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        instructionColumn.setCellValueFactory(new PropertyValueFactory<>("instructionText"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));
        architectureColumn.setCellValueFactory(new PropertyValueFactory<>("architecture"));  // NEW

        instructionsTableView.setRowFactory(tv -> new TableRow<InstructionRow>() {
            @Override
            protected void updateItem(InstructionRow item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().remove("highlighted-row");

                if (empty || item == null) {
                    return;
                }

                if (highlightedTerm != null && !highlightedTerm.isEmpty()) {
                    String label = item.getLabel() != null ? item.getLabel() : "";
                    String instruction = item.getInstructionText() != null ? item.getInstructionText() : "";

                    if (label.equals(highlightedTerm) || instruction.contains(highlightedTerm)) {
                        getStyleClass().add("highlighted-row");
                    }
                }
            }
        });

        instructionsTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        instructionsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null && historyController != null) {
                        int instructionIndex = newSelection.getNumber() - 1;
                        if (currentProgramDetails != null &&
                                instructionIndex >= 0 &&
                                instructionIndex < currentProgramDetails.instructions().size()) {

                            Instruction selectedInstruction = currentProgramDetails.instructions().get(instructionIndex);
                            historyController.showInstructionHistory(selectedInstruction);
                        }
                    }
                }
        );
    }

    public void setHistoryController(instruction_historyController historyController) {
        this.historyController = historyController;
    }

    public void highlightInstruction(int instructionNumber) {
        if (instructionNumber > 0 && instructionNumber <= instructionsTableView.getItems().size()) {
            instructionsTableView.getSelectionModel().select(instructionNumber - 1);
            instructionsTableView.scrollTo(instructionNumber - 1);
        }
    }

    public void clearInstructionHighlight() {
        instructionsTableView.getSelectionModel().clearSelection();
    }

    public void highlightTerm(String term) {
        this.highlightedTerm = term;
        if (instructionsTableView != null) {
            instructionsTableView.refresh();
        }
    }

    public static class InstructionParser {
        private static final Pattern INSTRUCTION_PATTERN = Pattern.compile(
                "^#\\d+\\s+\\(\\w\\)\\s+\\[.*?\\]\\s+(.+?)\\s+\\(\\d+\\)$"
        );

        public static String getCommandFromDisplayString(String fullInstructionString) {
            Matcher matcher = INSTRUCTION_PATTERN.matcher(fullInstructionString);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return fullInstructionString;
        }
    }

    public void loadProgramData(ProgramDetails programDetails) {
        this.currentProgramDetails = programDetails;
        instructionsTableView.getItems().clear();

        if (historyController != null) {
            historyController.clearHistory();
        }

        ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList();
        Map<String, Integer> archCounts = new HashMap<>();

        // Initialize architecture counts
        archCounts.put("GENERATION_I", 0);
        archCounts.put("GENERATION_II", 0);
        archCounts.put("GENERATION_III", 0);
        archCounts.put("GENERATION_IV", 0);

        int instructionCounter = 1;
        for (Instruction instruction : programDetails.instructions()) {
            String fullDisplayString = instruction.getStringInstruction();
            String commandText = InstructionParser.getCommandFromDisplayString(fullDisplayString);

            // Get required architecture
            String requiredArch = instruction.getRequiredArchitecture().name();
            archCounts.put(requiredArch, archCounts.get(requiredArch) + 1);

            InstructionRow row = new InstructionRow(
                    instructionCounter++,
                    String.valueOf(instruction.getInstructionTypeChar()),
                    instruction.getLabel().getStringLabel(),
                    commandText,
                    instruction.getCyclesNumber(),
                    requiredArch
            );
            instructionRows.add(row);
        }

        instructionsTableView.setItems(instructionRows);

        updateSummaryWithArchitecture(programDetails, archCounts);
    }

    private void updateSummaryWithArchitecture(ProgramDetails programDetails,
                                               Map<String, Integer> archCounts) {
        int basicCount = 0;
        int syntheticCount = 0;

        for (Instruction inst : programDetails.instructions()) {
            if (inst.getInstructionTypeChar() == 'B') {
                basicCount++;
            } else {
                syntheticCount++;
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Total: %d instructions (Basic: %d, Synthetic: %d)\n",
                programDetails.instructions().size(), basicCount, syntheticCount));

        summary.append("By Architecture: ");
        summary.append("Gen I: ").append(archCounts.get("GENERATION_I")).append("  ");
        summary.append("Gen II: ").append(archCounts.get("GENERATION_II")).append("  ");
        summary.append("Gen III: ").append(archCounts.get("GENERATION_III")).append("  ");
        summary.append("Gen IV: ").append(archCounts.get("GENERATION_IV"));

        summaryLabel.setText(summary.toString());
    }
}