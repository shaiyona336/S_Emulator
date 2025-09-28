package fxml.app;

import components.engine.Engine;
import components.engine.StandardEngine;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;
import fxml.debugger.DebuggerPanelController;
import fxml.instruction_history.instruction_historyController;
import fxml.instruction_table.instruction_tableController;
import fxml.statistics.StatisticsController;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class mainController {
    private Engine engine = new StandardEngine();

    @FXML private Button loadButton;
    @FXML private Label loadedFileLabel;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Button collapseButton;
    @FXML private Button expandButton;
    @FXML private Label degreeLabel;
    @FXML private ComboBox<String> highlightComboBox;

    @FXML private ComboBox<String> programSelectorComboBox;

    @FXML private instruction_tableController instructionsTableController;
    @FXML private DebuggerPanelController debuggerController;
    @FXML private StatisticsController statisticsController;

    @FXML private instruction_historyController instructionHistoryController;


    private int currentDegree = 0;
    private int maxDegree = 0;

    @FXML
    public void initialize() {
        if (debuggerController != null) {
            debuggerController.setEngine(engine);
            debuggerController.setMainController(this);

            if (instructionsTableController != null && instructionHistoryController != null) {
                instructionsTableController.setHistoryController(instructionHistoryController);
            }
        }

        highlightComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (instructionsTableController != null) {
                String termToHighlight = "";
                if (newValue != null && !newValue.equals("None")) {
                    termToHighlight = newValue.substring(newValue.indexOf(":") + 2);
                }
                instructionsTableController.highlightTerm(termToHighlight);
            }
        });

        programSelectorComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                // Set the engine's context to the newly selected program/function
                engine.setContextProgram(newValue);
                // Refresh the entire view to show the new context
                setupExpansionForNewProgram();
            }
        });

        updateButtonStates();
        updateDegreeLabel();
        highlightComboBox.setDisable(true);
        programSelectorComboBox.setDisable(true); // Disable until a file is loaded
    }

    @FXML
    void loadFileButtonListener(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open S-Emulator Program File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(loadButton.getScene().getWindow());
        if (selectedFile != null) {
            startFileLoadTask(selectedFile);
        }
    }

    private void startFileLoadTask(File file) {
        Task<ProgramDetails> loadTask = new Task<>() {
            @Override
            protected ProgramDetails call() throws Exception {
                updateProgress(30, 100);
                Thread.sleep(500);
                engine.loadProgramFromFile(file);
                updateProgress(100, 100);
                Thread.sleep(500);
                return engine.getProgramDetails(); // Gets details for the main program initially
            }
        };

        loadingProgressBar.progressProperty().bind(loadTask.progressProperty());
        loadingProgressBar.visibleProperty().bind(loadTask.runningProperty());
        loadButton.disableProperty().bind(loadTask.runningProperty());

        loadTask.setOnSucceeded(e -> {
            loadedFileLabel.setText("Loaded: " + file.getName());
            if (statisticsController != null) {
                statisticsController.clearHistory();
            }

            // --- MODIFIED: Populate and set up the program selector ---
            programSelectorComboBox.setItems(FXCollections.observableArrayList(engine.getDisplayableProgramNames()));
            programSelectorComboBox.getSelectionModel().selectFirst(); // Select the main program by default
            programSelectorComboBox.setDisable(false);
            // The listener will fire here and set the initial context, so setupExpansionForNewProgram is called correctly

            // This is now handled by the listener, but we call it once to ensure the initial view is correct.
            setupExpansionForNewProgram();
        });

        loadTask.setOnFailed(e -> {
            loadedFileLabel.setText("Failed to load file.");
            programSelectorComboBox.getItems().clear();
            programSelectorComboBox.setDisable(true);
            showAlert(Alert.AlertType.ERROR, "File Load Error", "Could not load file.", loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    @FXML
    void handleCollapseClick(ActionEvent event) {
        if (!engine.isProgramLoaded() || currentDegree <= 0) return;
        currentDegree--;
        updateProgramViewToCurrentDegree();
    }

    @FXML
    void handleExpandClick(ActionEvent event) {
        if (!engine.isProgramLoaded() || currentDegree >= maxDegree) return;
        currentDegree++;
        updateProgramViewToCurrentDegree();
    }

    public void onProgramRunFinished() {
        if (engine.isProgramLoaded() && statisticsController != null) {
            List<RunHistoryDetails> history = engine.getStatistics();
            statisticsController.loadStatistics(history);
        }
    }

    public void highlightInstruction(int instructionNumber) {
        if (instructionsTableController != null) {
            instructionsTableController.highlightInstruction(instructionNumber);
        }
    }

    public void clearInstructionHighlight() {
        if (instructionsTableController != null) {
            instructionsTableController.clearInstructionHighlight();
        }
    }

    public void setExpansionControlsDisabled(boolean disabled) {
        expandButton.setDisable(disabled);
        collapseButton.setDisable(disabled);
        if (!disabled) {
            updateButtonStates();
        }
    }

    private void setupExpansionForNewProgram() {
        if (!engine.isProgramLoaded()) return;
        currentDegree = 0;
        maxDegree = engine.getProgramMaxDegree(); // This will now get the max degree of the context program
        updateProgramViewToCurrentDegree();
    }

    private void updateProgramViewToCurrentDegree() {
        //this method now gets the details for the currently selected context program at the desired degree
        ProgramDetails programDetails = engine.expandProgram(currentDegree);

        if (instructionsTableController != null) {
            instructionsTableController.loadProgramData(programDetails);
        }
        if (debuggerController != null) {
            debuggerController.setupForNewProgram(programDetails, currentDegree);
        }

        populateHighlightComboBox(programDetails);
        updateDegreeLabel();
        updateButtonStates();
    }

    private void updateDegreeLabel() {
        if (engine.isProgramLoaded()) {
            degreeLabel.setText(String.format("Degree: %d / %d", currentDegree, maxDegree));
        } else {
            degreeLabel.setText("N/A");
        }
    }

    private void updateButtonStates() {
        boolean isLoaded = engine.isProgramLoaded();
        collapseButton.setDisable(!isLoaded || currentDegree <= 0);
        expandButton.setDisable(!isLoaded || currentDegree >= maxDegree);
    }

    private void populateHighlightComboBox(ProgramDetails programDetails) {
        highlightComboBox.getItems().clear();
        if (programDetails == null) {
            highlightComboBox.setDisable(true);
            return;
        }

        List<String> highlightOptions = new ArrayList<>();
        highlightOptions.add("None");

        programDetails.labels().stream()
                .map(label -> "Label: " + label.getStringLabel())
                .forEach(highlightOptions::add);
        programDetails.inputVariables().stream()
                .map(var -> "Var: " + var.getStringVariable())
                .forEach(highlightOptions::add);
        programDetails.workVariables().stream()
                .map(var -> "Var: " + var.getStringVariable())
                .forEach(highlightOptions::add);

        highlightComboBox.setItems(FXCollections.observableArrayList(highlightOptions));
        highlightComboBox.setDisable(false);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}