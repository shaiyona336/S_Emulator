package fxml.debugger;

import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.VariableDetails;
import fxml.app.mainController;
import http.HttpClientUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.HashMap;
import java.util.Map;

public class DebuggerPanelController {

    @FXML private Button startRegularButton;
    @FXML private Button startDebugButton;
    @FXML private Button stepOverButton;
    @FXML private Button resumeButton;
    @FXML private Button stopButton;
    @FXML private GridPane inputVariablesGrid;
    @FXML private TableView<VariableRow> variablesTableView;
    @FXML private TableColumn<VariableRow, String> variableNameColumn;
    @FXML private TableColumn<VariableRow, String> variableValueColumn;
    @FXML private Label cyclesLabel;

    private mainController mainController;
    private boolean isDebugging = false;
    private Map<String, TextField> inputFields = new HashMap<>();
    private ProgramDetails currentProgramDetails;
    private int currentDegree;
    private boolean architectureValid = false;

    @FXML
    public void initialize() {
        variableNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        variableValueColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().value));

        updateButtonStates();
    }

    public void setMainController(mainController mainController) {
        this.mainController = mainController;
    }

    public void setArchitectureValid(boolean valid) {
        this.architectureValid = valid;
        updateButtonStates();
    }

    public void setupForNewProgram(ProgramDetails programDetails, int degree) {
        this.currentProgramDetails = programDetails;
        this.currentDegree = degree;
        this.isDebugging = false;

        setupInputFields(programDetails);
        clearVariablesTable();
        cyclesLabel.setText("Cycles: 0");
        updateButtonStates();
    }

    @FXML
    private void handleStartRegular() {
        if (!architectureValid) {
            showAlert(Alert.AlertType.ERROR, "Invalid Architecture",
                    "The selected architecture does not support all instructions in this program.");
            return;
        }

        Long[] inputs = collectInputs();
        if (inputs == null) return;

        String architecture = mainController.getSelectedArchitecture();

        Task<ExecutionDetails> runTask = new Task<>() {
            @Override
            protected ExecutionDetails call() throws Exception {
                return HttpClientUtil.runProgram(currentDegree, inputs, architecture);
            }
        };

        runTask.setOnSucceeded(e -> {
            ExecutionDetails result = runTask.getValue();
            displayExecutionResult(result);
            mainController.onProgramRunFinished();
        });

        runTask.setOnFailed(e -> {
            Throwable ex = runTask.getException();
            String message = ex.getMessage();

            if (message != null && message.contains("Insufficient credits")) {
                showAlert(Alert.AlertType.WARNING, "Insufficient Credits",
                        "You don't have enough credits to run this program.\n\n" + message);
            } else if (message != null && message.contains("Ran out of credits")) {
                showAlert(Alert.AlertType.ERROR, "Credits Depleted",
                        "You ran out of credits during execution.\n\n" + message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Execution Failed",
                        "Failed to run program: " + message);
            }
            mainController.onProgramRunFinished();
        });

        mainController.setExpansionControlsDisabled(true);
        new Thread(runTask).start();
    }

    @FXML
    private void handleStartDebug() {
        if (!architectureValid) {
            showAlert(Alert.AlertType.ERROR, "Invalid Architecture",
                    "The selected architecture does not support all instructions in this program.");
            return;
        }

        Long[] inputs = collectInputs();
        if (inputs == null) return;

        String architecture = mainController.getSelectedArchitecture();

        Task<DebugStepDetails> debugTask = new Task<>() {
            @Override
            protected DebugStepDetails call() throws Exception {
                return HttpClientUtil.startDebug(currentDegree, inputs, architecture);
            }
        };

        debugTask.setOnSucceeded(e -> {
            isDebugging = true;
            DebugStepDetails stepDetails = debugTask.getValue();
            updateDebugView(stepDetails);
            updateButtonStates();
            mainController.setExpansionControlsDisabled(true);
        });

        debugTask.setOnFailed(e -> {
            Throwable ex = debugTask.getException();
            String message = ex.getMessage();

            if (message != null && message.contains("Insufficient credits")) {
                showAlert(Alert.AlertType.WARNING, "Insufficient Credits",
                        "You don't have enough credits to debug this program.\n\n" + message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Debug Failed",
                        "Failed to start debugging: " + message);
            }
        });

        new Thread(debugTask).start();
    }

    @FXML
    private void handleStepOver() {
        if (!isDebugging) return;

        Task<DebugStepDetails> stepTask = new Task<>() {
            @Override
            protected DebugStepDetails call() throws Exception {
                return HttpClientUtil.stepOver();
            }
        };

        stepTask.setOnSucceeded(e -> {
            DebugStepDetails stepDetails = stepTask.getValue();
            updateDebugView(stepDetails);

            if (stepDetails.isFinished()) {
                isDebugging = false;
                mainController.clearInstructionHighlight();
                updateButtonStates();
                mainController.setExpansionControlsDisabled(false);
                mainController.onProgramRunFinished();
                showAlert(Alert.AlertType.INFORMATION, "Debug Complete",
                        "Program finished. Y = " + stepDetails.yValue());
            } else {
                mainController.onProgramRunFinished(); // Refresh credits
            }
        });

        stepTask.setOnFailed(e -> {
            Throwable ex = stepTask.getException();
            String message = ex.getMessage();

            if (message != null && message.contains("Insufficient credits")) {
                isDebugging = false;
                mainController.clearInstructionHighlight();
                updateButtonStates();
                mainController.setExpansionControlsDisabled(false);
                showAlert(Alert.AlertType.ERROR, "Credits Depleted",
                        "You ran out of credits during debugging.\n\n" + message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Step Failed",
                        "Failed to step over: " + message);
            }
        });

        new Thread(stepTask).start();
    }

    @FXML
    private void handleResume() {
        if (!isDebugging) return;

        Task<ExecutionDetails> resumeTask = new Task<>() {
            @Override
            protected ExecutionDetails call() throws Exception {
                return HttpClientUtil.resume();
            }
        };

        resumeTask.setOnSucceeded(e -> {
            isDebugging = false;
            ExecutionDetails result = resumeTask.getValue();
            displayExecutionResult(result);
            mainController.clearInstructionHighlight();
            updateButtonStates();
            mainController.setExpansionControlsDisabled(false);
            mainController.onProgramRunFinished();
        });

        resumeTask.setOnFailed(e -> {
            Throwable ex = resumeTask.getException();
            String message = ex.getMessage();

            if (message != null && message.contains("Ran out of credits")) {
                isDebugging = false;
                mainController.clearInstructionHighlight();
                updateButtonStates();
                mainController.setExpansionControlsDisabled(false);
                showAlert(Alert.AlertType.ERROR, "Credits Depleted",
                        "You ran out of credits during execution.\n\n" + message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Resume Failed",
                        "Failed to resume: " + message);
            }
            mainController.onProgramRunFinished();
        });

        new Thread(resumeTask).start();
    }

    @FXML
    private void handleStop() {
        if (!isDebugging) return;

        Task<Void> stopTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpClientUtil.stopDebugging();
                return null;
            }
        };

        stopTask.setOnSucceeded(e -> {
            isDebugging = false;
            mainController.clearInstructionHighlight();
            clearVariablesTable();
            cyclesLabel.setText("Cycles: 0");
            updateButtonStates();
            mainController.setExpansionControlsDisabled(false);
        });

        new Thread(stopTask).start();
    }

    private void setupInputFields(ProgramDetails programDetails) {
        inputVariablesGrid.getChildren().clear();
        inputFields.clear();

        int row = 0;
        for (VariableDetails var : programDetails.inputVariables()) {
            Label label = new Label(var.getStringVariable() + ":");
            TextField textField = new TextField("0");
            textField.setPrefWidth(100);

            inputVariablesGrid.add(label, 0, row);
            inputVariablesGrid.add(textField, 1, row);

            inputFields.put(var.getStringVariable(), textField);
            row++;
        }
    }

    private Long[] collectInputs() {
        if (currentProgramDetails == null) return null;

        try {
            Long[] inputs = new Long[currentProgramDetails.inputVariables().size()];
            int i = 0;
            for (VariableDetails var : currentProgramDetails.inputVariables()) {
                TextField field = inputFields.get(var.getStringVariable());
                inputs[i++] = Long.parseLong(field.getText());
            }
            return inputs;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input",
                    "Please enter valid numbers for all input variables.");
            return null;
        }
    }

    private void updateDebugView(DebugStepDetails stepDetails) {
        // Update variables table
        variablesTableView.getItems().clear();
        for (VariableDetails var : stepDetails.variables()) {
            variablesTableView.getItems().add(
                    new VariableRow(var.getStringVariable(), String.valueOf(var.getValue()))
            );
        }

        // Update cycles
        cyclesLabel.setText("Cycles: " + stepDetails.totalCycles());

        // Highlight current instruction
        mainController.highlightInstruction(stepDetails.currentInstructionNumber());
    }

    private void displayExecutionResult(ExecutionDetails result) {
        mainController.clearInstructionHighlight();
        cyclesLabel.setText("Cycles: " + result.totalCycles());

        showAlert(Alert.AlertType.INFORMATION, "Execution Complete",
                String.format("Program finished!\n\nY = %d\nTotal Cycles: %d",
                        result.yValue(), result.totalCycles()));
    }

    private void clearVariablesTable() {
        variablesTableView.getItems().clear();
    }

    private void updateButtonStates() {
        Platform.runLater(() -> {
            boolean hasProgram = currentProgramDetails != null;
            startRegularButton.setDisable(!hasProgram || isDebugging || !architectureValid);
            startDebugButton.setDisable(!hasProgram || isDebugging || !architectureValid);
            stepOverButton.setDisable(!isDebugging);
            resumeButton.setDisable(!isDebugging);
            stopButton.setDisable(!isDebugging);
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static class VariableRow {
        private final String name;
        private final String value;

        public VariableRow(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
    }


    public void populateInputsFromValues(Long[] values) {
        if (currentProgramDetails == null || values == null) return;

        int i = 0;
        for (VariableDetails var : currentProgramDetails.inputVariables()) {
            TextField field = inputFields.get(var.getStringVariable());
            if (field != null && i < values.length) {
                field.setText(String.valueOf(values[i++]));
            }
        }
    }
}