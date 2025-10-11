package fxml.app;

import dtos.ProgramDetails;
import dtos.RunHistoryDetails;
import fxml.debugger.DebuggerPanelController;
import fxml.instruction_table.instruction_tableController;
import fxml.instruction_history.instruction_historyController;
import fxml.statistics.StatisticsController;
import http.HttpClientUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class mainController {

    @FXML private Button loadButton;
    @FXML private Label loadedFileLabel;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Button collapseButton;
    @FXML private Button expandButton;
    @FXML private Label degreeLabel;
    @FXML private ComboBox<String> highlightComboBox;
    @FXML private ComboBox<String> programSelectorComboBox;

    @FXML private instruction_tableController instructionsTableController;
    @FXML private instruction_historyController instructionHistoryController;
    @FXML private DebuggerPanelController debuggerController;
    @FXML private StatisticsController statisticsController;

    private int currentDegree = 0;
    private int maxDegree = 0;
    private boolean isProgramLoaded = false;

    @FXML
    private Label usernameLabel;  // Add this to your FXML if you want to display username

    private String currentUsername;

    // This method is called from LoginController after successful login
    public void setUsername(String username) {
        this.currentUsername = username;
        if (usernameLabel != null) {
            usernameLabel.setText("Logged in as: " + username);
        }
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    @FXML
    public void initialize() {
        // Set up debugger controller
        if (debuggerController != null) {
            debuggerController.setMainController(this);
        }

        // Connect statistics controller with debugger
        if (statisticsController != null) {
            if (debuggerController != null) {
                statisticsController.setDebuggerController(debuggerController);
            }
        }

        // Connect instruction table controller with history controller
        if (instructionsTableController != null && instructionHistoryController != null) {
            instructionsTableController.setHistoryController(instructionHistoryController);
        }

        // Set up highlight combo box listener
        highlightComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (instructionsTableController != null) {
                String termToHighlight = "";
                if (newValue != null && !newValue.equals("None")) {
                    termToHighlight = newValue.substring(newValue.indexOf(":") + 2);
                }
                instructionsTableController.highlightTerm(termToHighlight);
            }
        });

        // Set up program selector combo box listener
        programSelectorComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                handleContextChange(newValue);
            }
        });

        updateButtonStates();
        updateDegreeLabel();
        highlightComboBox.setDisable(true);
        programSelectorComboBox.setDisable(true);
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

                // Upload file via HTTP
                HttpClientUtil.uploadFile(file);

                updateProgress(70, 100);

                // Get program details
                ProgramDetails details = HttpClientUtil.getProgramDetails();

                updateProgress(100, 100);
                Thread.sleep(500);
                return details;
            }
        };

        loadingProgressBar.progressProperty().bind(loadTask.progressProperty());
        loadingProgressBar.visibleProperty().bind(loadTask.runningProperty());
        loadButton.disableProperty().bind(loadTask.runningProperty());

        loadTask.setOnSucceeded(e -> {
            loadedFileLabel.setText("Loaded: " + file.getName());
            isProgramLoaded = true;

            if (statisticsController != null) {
                statisticsController.clearHistory();
            }

            // Set up the program selector
            Task<List<String>> namesTask = new Task<>() {
                @Override
                protected List<String> call() throws Exception {
                    return HttpClientUtil.getProgramNames();
                }
            };

            namesTask.setOnSucceeded(ev -> {
                programSelectorComboBox.setItems(FXCollections.observableArrayList(namesTask.getValue()));
                programSelectorComboBox.getSelectionModel().selectFirst();
                programSelectorComboBox.setDisable(false);
                setupExpansionForNewProgram();
            });

            new Thread(namesTask).start();
        });

        loadTask.setOnFailed(e -> {
            loadedFileLabel.setText("Failed to load file.");
            isProgramLoaded = false;
            programSelectorComboBox.getItems().clear();
            programSelectorComboBox.setDisable(true);
            showAlert(Alert.AlertType.ERROR, "File Load Error",
                    "Could not load file.", loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void handleContextChange(String newContext) {
        Task<Void> contextTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpClientUtil.setContextProgram(newContext);
                return null;
            }
        };

        contextTask.setOnSucceeded(e -> setupExpansionForNewProgram());
        contextTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to change context", contextTask.getException().getMessage());
        });

        new Thread(contextTask).start();
    }

    @FXML
    void handleCollapseClick(ActionEvent event) {
        if (!isProgramLoaded || currentDegree <= 0) return;
        currentDegree--;
        updateProgramViewToCurrentDegree();
    }

    @FXML
    void handleExpandClick(ActionEvent event) {
        if (!isProgramLoaded || currentDegree >= maxDegree) return;
        currentDegree++;
        updateProgramViewToCurrentDegree();
    }

    public void onProgramRunFinished() {
        if (!isProgramLoaded || statisticsController == null) return;

        Task<List<RunHistoryDetails>> statsTask = new Task<>() {
            @Override
            protected List<RunHistoryDetails> call() throws Exception {
                return HttpClientUtil.getStatistics();
            }
        };

        statsTask.setOnSucceeded(e -> {
            statisticsController.loadStatistics(statsTask.getValue());
        });

        new Thread(statsTask).start();
    }

    public void highlightInstruction(int instructionNumber) {
        if (instructionsTableController != null) {
            Platform.runLater(() ->
                    instructionsTableController.highlightInstruction(instructionNumber)
            );
        }
    }

    public void clearInstructionHighlight() {
        if (instructionsTableController != null) {
            Platform.runLater(() ->
                    instructionsTableController.clearInstructionHighlight()
            );
        }
    }

    public void setExpansionControlsDisabled(boolean disabled) {
        expandButton.setDisable(disabled);
        collapseButton.setDisable(disabled);
        programSelectorComboBox.setDisable(disabled);
        highlightComboBox.setDisable(disabled);
        if (!disabled) {
            updateButtonStates();
        }
    }

    private void setupExpansionForNewProgram() {
        if (!isProgramLoaded) return;

        Task<Integer> maxDegreeTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return HttpClientUtil.getMaxDegree();
            }
        };

        maxDegreeTask.setOnSucceeded(e -> {
            currentDegree = 0;
            maxDegree = maxDegreeTask.getValue();
            updateProgramViewToCurrentDegree();
        });

        new Thread(maxDegreeTask).start();
    }

    private void updateProgramViewToCurrentDegree() {
        Task<ProgramDetails> expandTask = new Task<>() {
            @Override
            protected ProgramDetails call() throws Exception {
                return HttpClientUtil.expandProgram(currentDegree);
            }
        };

        expandTask.setOnSucceeded(e -> {
            ProgramDetails programDetails = expandTask.getValue();

            if (instructionsTableController != null) {
                instructionsTableController.loadProgramData(programDetails);
            }
            if (debuggerController != null) {
                debuggerController.setupForNewProgram(programDetails, currentDegree);
            }
            if (instructionHistoryController != null) {
                instructionHistoryController.clearHistory();
            }

            populateHighlightComboBox(programDetails);
            updateDegreeLabel();
            updateButtonStates();
        });

        expandTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to expand program", expandTask.getException().getMessage());
        });

        new Thread(expandTask).start();
    }

    private void updateDegreeLabel() {
        if (isProgramLoaded) {
            degreeLabel.setText(String.format("Degree: %d / %d", currentDegree, maxDegree));
        } else {
            degreeLabel.setText("N/A");
        }
    }

    private void updateButtonStates() {
        collapseButton.setDisable(!isProgramLoaded || currentDegree <= 0);
        expandButton.setDisable(!isProgramLoaded || currentDegree >= maxDegree);
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
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}