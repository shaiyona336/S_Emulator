package fxml.instruction_history;

import components.instruction.Instruction;
import fxml.InstructionRow;
import fxml.instruction_table.instruction_tableController.InstructionParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class instruction_historyController {

    @FXML private TableView<InstructionRow> historyTableView;
    @FXML private TableColumn<InstructionRow, Integer> numberColumn;
    @FXML private TableColumn<InstructionRow, String> typeColumn;
    @FXML private TableColumn<InstructionRow, String> labelColumn;
    @FXML private TableColumn<InstructionRow, String> instructionColumn;
    @FXML private TableColumn<InstructionRow, Integer> cyclesColumn;

    @FXML
    public void initialize() {
        // Set up the cell value factories for the columns
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        instructionColumn.setCellValueFactory(new PropertyValueFactory<>("instructionText"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        // Initially the table should be empty
        historyTableView.setPlaceholder(new Label("Select an instruction to view its expansion history"));
    }

    /**
     * Displays the historical chain of instructions for a given instruction.
     * The most recent instruction (the one passed in) is shown at the top,
     * and the most ancient (original) instruction is shown at the bottom.
     */
    public void showInstructionHistory(Instruction instruction) {
        if (instruction == null) {
            clearHistory();
            return;
        }

        // Build the chain of instructions from current to ancient
        List<Instruction> instructionChain = new ArrayList<>();
        Instruction currentInstruction = instruction;

        // Add the current instruction first
        instructionChain.add(currentInstruction);

        // Follow the chain of ancient instructions
        while (currentInstruction.hasAncientInstruction()) {
            currentInstruction = currentInstruction.getAncientInstruction();
            instructionChain.add(currentInstruction);
        }

        // Create the observable list for the table
        ObservableList<InstructionRow> historyRows = FXCollections.observableArrayList();

        // The most recent instruction should be at the top (index 0)
        // The most ancient should be at the bottom
        // Since we built the list from current to ancient, it's already in the right order
        int rowNumber = 1;
        for (Instruction inst : instructionChain) {
            String fullDisplayString = inst.getStringInstruction();
            String commandText = InstructionParser.getCommandFromDisplayString(fullDisplayString);

            InstructionRow row = new InstructionRow(
                    rowNumber++,
                    String.valueOf(inst.getInstructionTypeChar()),
                    inst.getLabel().getStringLabel(),
                    commandText,
                    inst.getCyclesNumber()
            );
            historyRows.add(row);
        }

        // Update the table with the history
        historyTableView.setItems(historyRows);

        // Optionally, you can highlight the first row (the current instruction)
        if (!historyRows.isEmpty()) {
            historyTableView.getSelectionModel().select(0);
        }
    }

    /**
     * Clears the instruction history table.
     */
    public void clearHistory() {
        historyTableView.getItems().clear();
        historyTableView.setPlaceholder(new Label("Select an instruction to view its expansion history"));
    }

    /**
     * Helper class for parsing instruction strings.
     * This is copied from instruction_tableController for consistency.
     */
    public static class InstructionParser {
        private static final java.util.regex.Pattern INSTRUCTION_PATTERN = java.util.regex.Pattern.compile(
                "^#\\d+\\s+\\(\\w\\)\\s+\\[.*?\\]\\s+(.+?)\\s+\\(\\d+\\)$"
        );

        public static String getCommandFromDisplayString(String fullInstructionString) {
            java.util.regex.Matcher matcher = INSTRUCTION_PATTERN.matcher(fullInstructionString);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return fullInstructionString;
        }
    }
}