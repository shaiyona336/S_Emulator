package fxml.instruction_history;

import components.instruction.Instruction;
import fxml.InstructionRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class instruction_historyController {

    @FXML private TableView<InstructionRow> historyTableView;
    @FXML private TableColumn<InstructionRow, Integer> numberColumn;
    @FXML private TableColumn<InstructionRow, String> typeColumn;
    @FXML private TableColumn<InstructionRow, String> labelColumn;
    @FXML private TableColumn<InstructionRow, String> instructionColumn;
    @FXML private TableColumn<InstructionRow, Integer> cyclesColumn;

    @FXML
    public void initialize() {
        //set up the cell value factories for the columns
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        instructionColumn.setCellValueFactory(new PropertyValueFactory<>("instructionText"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        //initially the table should be empty
        historyTableView.setPlaceholder(new Label("Select an instruction to view its expansion history"));
    }

    public void showInstructionHistory(Instruction instruction) {
        if (instruction == null) {
            clearHistory();
            return;
        }

        //build the chain of instructions from current to ancient
        List<Instruction> instructionChain = new ArrayList<>();
        Instruction currentInstruction = instruction;

        //add the current instruction first
        instructionChain.add(currentInstruction);

        //follow the chain of ancient instructions
        while (currentInstruction.hasAncientInstruction()) {
            currentInstruction = currentInstruction.getAncientInstruction();
            instructionChain.add(currentInstruction);
        }

        //create the observable list for the table
        ObservableList<InstructionRow> historyRows = FXCollections.observableArrayList();

        //the most recent instruction should be at the top (index 0)
        //the most ancient should be at the bottom
        //since we built the list from current to ancient, it's already in the right order
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

        //update the table with the history
        historyTableView.setItems(historyRows);

        //optionally, you can highlight the first row (the current instruction)
        if (!historyRows.isEmpty()) {
            historyTableView.getSelectionModel().select(0);
        }
    }

    public void clearHistory() {
        historyTableView.getItems().clear();
        historyTableView.setPlaceholder(new Label("Select an instruction to view its expansion history"));
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
}