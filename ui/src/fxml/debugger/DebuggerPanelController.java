package fxml.debugger;

import components.executor.Context;
import components.variable.Variable;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.DebugStepDetails;
import dtos.RunHistoryDetails;
import fxml.VariableOutputRow;
import fxml.app.mainController;
import http.HttpClientUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class DebuggerPanelController {

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
        updateComponentStates();
    }

    public void setMainController(mainController mainController) {
        this.mainController = mainController;
    }

    public void setupForNewProgram(ProgramDetails programDetails, int currentDegree) {
        this.loadedProgramDetails = programDetails;
        this.currentProgramDegree = currentDegree;
        if (isInDebugMode) {
            stopDebugging();
        }
        resetInputsAndOutputs();

        if (programDetails != null && programDetails.inputVariables() != null) {
            programDetails.inputVariables().stream()
                    .sorted(Comparator.comparingInt(v -> Integer.parseInt(v.getStringVariable().substring(1))))
                    .forEach(this::createInputFieldForVariable);
        }
        updateComponentStates();
    }

    // --- UI Event Handlers ---

    @FXML
    private void handleStartNormalRun() {
        Task<ExecutionDetails> runTask = new Task<>() {
            @Override
            protected ExecutionDetails call() throws Exception {
                return HttpClientUtil.runProgram(currentProgramDegree, buildInputsArray());
            }
        };

        runTask.setOnSucceeded(e -> {
            ExecutionDetails executionDetails = runTask.getValue();
            displayExecutionResults(executionDetails);
            if (mainController != null) mainController.onProgramRunFinished();
        });

        runTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Execution Error",
                    "Program encountered an error.", runTask.getException().getMessage());
        });

        new Thread(runTask).start();
    }

    @FXML
    private void handleStartDebugRun() {
        Task<DebugStepDetails> debugTask = new Task<>() {
            @Override
            protected DebugStepDetails call() throws Exception {
                return HttpClientUtil.startDebugging(currentProgramDegree, buildInputsArray());
            }
        };

        debugTask.setOnSucceeded(e -> {
            DebugStepDetails initialStep = debugTask.getValue();
            isInDebugMode = true;
            updateComponentStates();
            displayDebugStepResults(initialStep);
            if (mainController != null) {
                mainController.highlightInstruction(1);
            }
        });

        debugTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Debug Error",
                    "Could not start debug session.", debugTask.getException().getMessage());
        });

        new Thread(debugTask).start();
    }

    @FXML
    private void handleStepOverClick() {
        if (!isInDebugMode) return;

        Task<DebugStepDetails> stepTask = new Task<>() {
            @Override
            protected DebugStepDetails call() throws Exception {
                return HttpClientUtil.stepOver();
            }
        };

        stepTask.setOnSucceeded(e -> {
            DebugStepDetails nextStep = stepTask.getValue();
            displayDebugStepResults(nextStep);

            if (mainController != null) {
                mainController.highlightInstruction(nextStep.nextInstructionNumber());
            }

            if (nextStep.isFinished()) {
                showAlert(Alert.AlertType.INFORMATION, "Debug Finished",
                        "The program has finished execution.", null);
                stopDebugging();
            }
        });

        stepTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Step Over Error",
                    "An error occurred during execution.", stepTask.getException().getMessage());
            stopDebugging();
        });

        new Thread(stepTask).start();
    }

    @FXML
    private void handleResumeClick() {
        if (!isInDebugMode) return;

        Task<ExecutionDetails> resumeTask = new Task<>() {
            @Override
            protected ExecutionDetails call() throws Exception {
                return HttpClientUtil.resume();
            }
        };

        resumeTask.setOnSucceeded(e -> {
            ExecutionDetails finalDetails = resumeTask.getValue();
            displayExecutionResults(finalDetails);
            stopDebugging();
        });

        resumeTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Resume Error",
                    "An error occurred during execution.", resumeTask.getException().getMessage());
            stopDebugging();
        });

        new Thread(resumeTask).start();
    }

    @FXML
    private void handleStopClick() {
        Task<Void> stopTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpClientUtil.stopDebugging();
                return null;
            }
        };

        stopTask.setOnSucceeded(e -> stopDebugging());
        new Thread(stopTask).start();
    }

    @FXML
    private void handleClearInputs() {
        variableInputFields.values().forEach(TextField::clear);
        variablesTableView.getItems().clear();
        cyclesLabel.setText("Total Cycles: N/A");
    }

    public void populateInputs(List<Long> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }

        variableInputFields.values().forEach(TextField::clear);

        for (Map.Entry<String, TextField> entry : variableInputFields.entrySet()) {
            String varName = entry.getKey();
            TextField field = entry.getValue();
            int varIndex = Integer.parseInt(varName.substring(1)) - 1;

            if (varIndex < inputs.size()) {
                Long value = inputs.get(varIndex);
                if (value != null && value != 0) {
                    field.setText(value.toString());
                }
            }
        }
    }

    // State and UI management logic

    private void updateComponentStates() {
        boolean isProgramLoaded = loadedProgramDetails != null;

        startRunButton.setDisable(!isProgramLoaded || isInDebugMode);
        startDebugButton.setDisable(!isProgramLoaded || isInDebugMode);
        clearInputsButton.setDisable(!isProgramLoaded || isInDebugMode);

        variableInputFields.values().forEach(tf -> tf.setDisable(!isProgramLoaded || isInDebugMode));

        stepOverButton.setDisable(!isInDebugMode);
        resumeButton.setDisable(!isInDebugMode);
        stopButton.setDisable(!isInDebugMode);

        if (mainController != null) {
            mainController.setExpansionControlsDisabled(isInDebugMode);
        }
    }

    private void stopDebugging() {
        isInDebugMode = false;
        updateComponentStates();
        if (mainController != null) {
            mainController.clearInstructionHighlight();
            mainController.onProgramRunFinished();
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
        int maxVarNum = variableInputFields.keySet().stream()
                .mapToInt(name -> Integer.parseInt(name.substring(1)))
                .max().orElse(0);
        Long[] inputs = new Long[maxVarNum];
        Arrays.fill(inputs, 0L);
        variableInputFields.forEach((name, textField) -> {
            int index = Integer.parseInt(name.substring(1)) - 1;
            String text = textField.getText().trim();
            if (!text.isEmpty()) {
                try {
                    inputs[index] = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    // Keep as 0
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
        context.getVariables().forEach((var, val) ->
                currentVariableState.put(var.getStringVariable(), val));

        ObservableList<VariableOutputRow> variableRows = FXCollections.observableArrayList();
        currentVariableState.forEach((name, value) -> {
            VariableOutputRow row = new VariableOutputRow(name, String.valueOf(value));
            if (previousVariableState.containsKey(name) &&
                    !previousVariableState.get(name).equals(value) ||
                    (!previousVariableState.containsKey(name) && value != 0)) {
                row.setChanged(true);
            }
            variableRows.add(row);
        });
        variablesTableView.setItems(variableRows);
        previousVariableState = currentVariableState;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}