package fxml.debugger;

import components.engine.Engine;
import components.executor.Context;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.DebugStepDetails;
import components.variable.Variable;
import dtos.RunHistoryDetails;
import fxml.VariableOutputRow;
import fxml.app.mainController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class DebuggerPanelController {

    private Engine engine;
    private mainController mainController;
    private ProgramDetails loadedProgramDetails;
    private int currentProgramDegree;

    private boolean isInDebugMode = false;
    private Map<String, Long> previousVariableState = new HashMap<>();
    private final Map<String, TextField> variableInputFields = new LinkedHashMap<>();

    @FXML private Button startRunButton, startDebugButton, stopButton, resumeButton, stepOverButton, clearInputsButton;
    @FXML private VBox inputsContainer;
    @FXML private TableView<VariableOutputRow> variablesTableView;
    @FXML private TableColumn<VariableOutputRow, String> variableNameColumn;
    @FXML private TableColumn<VariableOutputRow, String> variableValueColumn;
    @FXML private Label cyclesLabel;

    @FXML
    public void initialize() {
        variableNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        variableValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        variablesTableView.setRowFactory(tv -> new TableRow<VariableOutputRow>() {
            @Override
            protected void updateItem(VariableOutputRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("changed-variable-row");
                if (!empty && item != null && item.isChanged()) {
                    getStyleClass().add("changed-variable-row");
                }
            }
        });
        //start with all controls disabled
        updateComponentStates();
    }

    public void setMainController(mainController mainController) { this.mainController = mainController; }
    public void setEngine(Engine engine) { this.engine = engine; }

    public void setupForNewProgram(ProgramDetails programDetails, int currentDegree) {
        this.loadedProgramDetails = programDetails;
        this.currentProgramDegree = currentDegree;
        if (isInDebugMode) { stopDebugging(); } // Stop any previous debug session
        resetInputsAndOutputs();

        if (programDetails != null && programDetails.inputVariables() != null) {
            programDetails.inputVariables().stream()
                    .sorted(Comparator.comparingInt(v -> Integer.parseInt(v.getStringVariable().substring(1))))
                    .forEach(this::createInputFieldForVariable);
        }
        //update the UI state now that a program is loaded
        updateComponentStates();
    }

    // --- UI Event Handlers ---
    @FXML private void handleStartNormalRun() {
        if (engine == null) return;
        try {
            ExecutionDetails executionDetails = engine.runProgram(currentProgramDegree, buildInputsArray());
            displayExecutionResults(executionDetails);
            if (mainController != null) mainController.onProgramRunFinished();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Execution Error", "Program encountered an error.", e.getMessage());
        }
    }

    @FXML private void handleStartDebugRun() {
        if (engine == null) return;
        try {
            Long[] inputs = buildInputsArray();
            DebugStepDetails initialStep = engine.startDebugging(currentProgramDegree, inputs);
            isInDebugMode = true;
            updateComponentStates();
            displayDebugStepResults(initialStep);
            if (mainController != null) mainController.highlightInstruction(1);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Debug Error", "Could not start debug session.", e.getMessage());
        }
    }

    @FXML
    private void handleStepOverClick() {
        if (!isInDebugMode || engine == null) return;
        try {
            DebugStepDetails nextStep = engine.stepOver();
            displayDebugStepResults(nextStep);

            if (mainController != null) {
                mainController.highlightInstruction(nextStep.nextInstructionNumber());
            }

            if (nextStep.isFinished()) {
                showAlert(Alert.AlertType.INFORMATION, "Debug Finished", "The program has finished execution.", null);

                //create the history record from the final state
                Context finalContext = nextStep.context();
                if (finalContext != null) {
                    Long yValue = finalContext.getVariableValue(Variable.OUTPUT);
                    Long[] inputs = buildInputsArray();

                    //use the engine's run number counter
                    int runNum = engine.getStatistics().size() + 1;

                    RunHistoryDetails finalRun = new RunHistoryDetails(runNum, currentProgramDegree, Arrays.asList(inputs), yValue, finalContext.getTotalCycles());

                    //add the completed run to the engine's history
                    engine.addRunToHistory(finalRun);
                }

                //now stop the debug session. The subsequent call to onProgramRunFinished()
                //will fetch the history list that now includes our new entry
                stopDebugging();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Step Over Error", "An error occurred during execution.", e.getMessage());
            stopDebugging();
        }
    }

    @FXML private void handleResumeClick() {
        if (!isInDebugMode || engine == null) return;
        try {
            ExecutionDetails finalDetails = engine.resume();
            displayExecutionResults(finalDetails);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Resume Error", "An error occurred during execution.", e.getMessage());
        } finally {
            stopDebugging();
        }
    }

    @FXML private void handleStopClick() { stopDebugging(); }

    @FXML private void handleClearInputs() {
        variableInputFields.values().forEach(TextField::clear);
        variablesTableView.getItems().clear();
        cyclesLabel.setText("Total Cycles: N/A");
    }

    public void populateInputs(List<Long> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }

        //clear existing inputs first
        variableInputFields.values().forEach(TextField::clear);

        //populate the input fields
        for (Map.Entry<String, TextField> entry : variableInputFields.entrySet()) {
            String varName = entry.getKey();
            TextField field = entry.getValue();

            //extract the variable number (for example "x1" -> 1)
            int varIndex = Integer.parseInt(varName.substring(1)) - 1;

            //set the value if it exists in the inputs list
            if (varIndex < inputs.size()) {
                Long value = inputs.get(varIndex);
                if (value != null && value != 0) {
                    field.setText(value.toString());
                }
            }
        }
    }

    //state and UI management logic

    private void updateComponentStates() {
        boolean isProgramLoaded = loadedProgramDetails != null;

        //normal run controls are enabled only if a program is loaded AND not in debug mode
        startRunButton.setDisable(!isProgramLoaded || isInDebugMode);
        startDebugButton.setDisable(!isProgramLoaded || isInDebugMode);
        clearInputsButton.setDisable(!isProgramLoaded || isInDebugMode);

        //input text fields are enabled only if a program is loaded AND not in debug mode
        variableInputFields.values().forEach(tf -> tf.setDisable(!isProgramLoaded || isInDebugMode));

        //debug controls are enabled ONLY when in debug mode
        stepOverButton.setDisable(!isInDebugMode);
        resumeButton.setDisable(!isInDebugMode);
        stopButton.setDisable(!isInDebugMode);

        //notify the main controller to enable/disable global controls
        if (mainController != null) {
            mainController.setExpansionControlsDisabled(isInDebugMode);
        }
    }

    private void stopDebugging() {
        if (engine != null) engine.stop();
        isInDebugMode = false;
        updateComponentStates(); // Reset UI to normal "program loaded" state
        if (mainController != null) {
            mainController.clearInstructionHighlight();
            mainController.onProgramRunFinished(); // Update stats after debug run
        }
    }

    private void createInputFieldForVariable(Variable var) {
        String varName = var.getStringVariable();
        HBox inputBox = new HBox(5);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        Label varLabel = new Label(varName + ":");
        varLabel.setPrefWidth(40);
        TextField inputField = new TextField();
        inputField.setPromptText("Enter value for " + varName);
        inputBox.getChildren().addAll(varLabel, inputField);
        inputsContainer.getChildren().add(inputBox);
        variableInputFields.put(varName, inputField);
    }

    private void resetInputsAndOutputs() {
        inputsContainer.getChildren().clear();
        variableInputFields.clear();
        variablesTableView.getItems().clear();
        cyclesLabel.setText("Total Cycles: N/A");
    }

    private Long[] buildInputsArray() {
        if (variableInputFields.isEmpty()) return new Long[0];
        int maxVarNum = variableInputFields.keySet().stream().mapToInt(name -> Integer.parseInt(name.substring(1))).max().orElse(0);
        Long[] inputs = new Long[maxVarNum];
        Arrays.fill(inputs, 0L);
        variableInputFields.forEach((name, textField) -> {
            int index = Integer.parseInt(name.substring(1)) - 1;
            String text = textField.getText().trim();
            if (!text.isEmpty()) {
                try {
                    inputs[index] = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    //ignore invalid input, keep as 0
                }
            }
        });
        return inputs;
    }

    private void displayExecutionResults(ExecutionDetails executionDetails) {
        if (executionDetails == null) return;
        previousVariableState.clear();
        displayContext(executionDetails.variables());
        cyclesLabel.setText("Total Cycles: " + executionDetails.cycles());
    }

    private void displayDebugStepResults(DebugStepDetails stepDetails) {
        if (stepDetails == null) return;
        displayContext(stepDetails.context());
        cyclesLabel.setText("Total Cycles: " + stepDetails.context().getTotalCycles());
    }

    private void displayContext(Context context) {
        if (context == null || context.getVariables() == null) return;
        Map<String, Long> currentVariableState = new HashMap<>();
        context.getVariables().forEach((var, val) -> currentVariableState.put(var.getStringVariable(), val));
        ObservableList<VariableOutputRow> variableRows = FXCollections.observableArrayList();
        currentVariableState.forEach((name, value) -> {
            VariableOutputRow row = new VariableOutputRow(name, String.valueOf(value));
            if (previousVariableState.containsKey(name) && !previousVariableState.get(name).equals(value) ||
                    (!previousVariableState.containsKey(name) && value != 0)) {
                row.setChanged(true);
            }
            variableRows.add(row);
        });
        variablesTableView.setItems(variableRows);
        previousVariableState = currentVariableState;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}