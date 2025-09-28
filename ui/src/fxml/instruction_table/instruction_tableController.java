package fxml.instruction_table;

import components.instruction.Instruction;
import dtos.ProgramDetails;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import fxml.InstructionRow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class instruction_tableController {

    @FXML private TableView<InstructionRow> instructionsTableView;
    @FXML private TableColumn<InstructionRow, Integer> numberColumn;
    @FXML private TableColumn<InstructionRow, String> typeColumn;
    @FXML private TableColumn<InstructionRow, String> labelColumn;
    @FXML private TableColumn<InstructionRow, String> instructionColumn;
    @FXML private TableColumn<InstructionRow, Integer> cyclesColumn;
    @FXML private Label summaryLabel;

    private String highlightedTerm = "";

    @FXML
    public void initialize() {

        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        instructionColumn.setCellValueFactory(new PropertyValueFactory<>("instructionText"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        instructionsTableView.setRowFactory(tv -> new TableRow<InstructionRow>() {
            @Override
            protected void updateItem(InstructionRow item, boolean empty) {
                super.updateItem(item, empty);

                //first, remove any existing highlight
                getStyleClass().remove("highlighted-row");

                if (empty || item == null) {
                    //dont style empty rows
                    return;
                }

                //if a term is selected and this row data contains it, apply the style
                if (highlightedTerm != null && !highlightedTerm.isEmpty()) {
                    String label = item.getLabel() != null ? item.getLabel() : "";
                    String instruction = item.getInstructionText() != null ? item.getInstructionText() : "";
                    //check if the label matches or if the instruction text contains the variable
                    if (label.equals(highlightedTerm) || instruction.contains(highlightedTerm)) {
                        getStyleClass().add("highlighted-row");
                    }
                }
            }
        });

        instructionsTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    }

    public void highlightInstruction(int instructionNumber) {
        if (instructionNumber > 0 && instructionNumber <= instructionsTableView.getItems().size()) {
            //select the row, The css will style it
            instructionsTableView.getSelectionModel().select(instructionNumber - 1);
            //scroll to the selected row to make sure its visible
            instructionsTableView.scrollTo(instructionNumber - 1);
        }
    }


    public void clearInstructionHighlight() {
        instructionsTableView.getSelectionModel().clearSelection();
    }


    public void highlightTerm(String term) {
        this.highlightedTerm = term;
        //refresh the table to force the row factory to be called again on all rows
        if (instructionsTableView != null) {
            instructionsTableView.refresh();
        }
    }


    //InstructionParser+getCommandFromDisplayString used to break down from instruction.getStringInstruction() only the command part
    public class InstructionParser { //or add this method to your controller

        //this pattern matches your formatted string and captures the command part
        private static final Pattern INSTRUCTION_PATTERN = Pattern.compile(
                // Matches: #1 (B) [ L1  ] DECREASE x1 (1)
                "^#\\d+\\s+\\(\\w\\)\\s+\\[.*?\\]\\s+(.+?)\\s+\\(\\d+\\)$"
        );

        public static String getCommandFromDisplayString(String fullInstructionString) {
            Matcher matcher = INSTRUCTION_PATTERN.matcher(fullInstructionString);
            if (matcher.find()) {
                // Group 1 is the part of the pattern inside the first parentheses: (.+?)
                return matcher.group(1).trim();
            }
            // If the pattern doesn't match for some reason, return the original string
            return fullInstructionString;
        }
    }

    public void loadProgramData(ProgramDetails programDetails) {
        // Clear any old data from the table
        instructionsTableView.getItems().clear();


        // Create a list that the TableView can observe for changes
        ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList();


        int instructionCounter = 1;
        for (Instruction instruction : programDetails.instructions()) {
            String fullDisplayString = instruction.getStringInstruction();
            String commandText = InstructionParser.getCommandFromDisplayString(fullDisplayString);
            InstructionRow row = new InstructionRow(
                    instructionCounter++,
                    String.valueOf(instruction.getInstructionTypeChar()),
                    instruction.getLabel().getStringLabel(),
                    commandText,
                    instruction.getCyclesNumber()
            );
            instructionRows.add(row);
        }

        // Populate the table with the new data
        instructionsTableView.setItems(instructionRows);

        // Update the summary line as well
        // summaryLabel.setText("Total instructions: " + programDetails.instructions().size());
    }

}
