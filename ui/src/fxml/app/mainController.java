package fxml.app;

import dtos.ProgramDetails;
import fxml.debugger.DebuggerPanelController;
import fxml.instruction_table.instruction_tableController;
import fxml.instruction_history.instruction_historyController;
import http.HttpClientUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class mainController {

    // Remove loadButton and loadingProgressBar
    // Remove loadedFileLabel

    // Add new top bar elements
    @FXML private Label usernameLabel;
    @FXML private Label creditsLabel;

    @FXML private Button collapseButton;
    @FXML private Button expandButton;
    @FXML private Label degreeLabel;
    @FXML private ComboBox<String> highlightComboBox;
    @FXML private ComboBox<String> programSelectorComboBox;
    @FXML private Button backToDashboardButton;

    @FXML private instruction_tableController instructionsTableController;
    @FXML private instruction_historyController instructionHistoryController;
    @FXML private DebuggerPanelController debuggerController;

    private int currentDegree = 0;
    private int maxDegree = 0;
    private boolean isProgramLoaded = false;
    private String currentUsername;
    private Timer creditRefreshTimer;

    @FXML
    public void initialize() {
        if (debuggerController != null) {
            debuggerController.setMainController(this);
        }

        if (instructionsTableController != null && instructionHistoryController != null) {
            instructionsTableController.setHistoryController(instructionHistoryController);
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
                handleContextChange(newValue);
            }
        });

        updateButtonStates();
        updateDegreeLabel();
        highlightComboBox.setDisable(true);
        programSelectorComboBox.setDisable(true);
    }

    public void setUsername(String username) {
        this.currentUsername = username;
        if (usernameLabel != null) {
            usernameLabel.setText("User: " + username);
        }
        startCreditRefresh();
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    // Call this method after navigation from dashboard
    public void initializeWithProgram() {
        if (!isProgramLoaded) {
            setupExpansionForNewProgram();
            isProgramLoaded = true;
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            if (creditRefreshTimer != null) {
                creditRefreshTimer.cancel();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard/dashboard.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof fxml.dashboard.DashboardController) {
                ((fxml.dashboard.DashboardController) controller).setUsername(currentUsername);
            }

            Stage stage = (Stage) backToDashboardButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("S-Emulator Dashboard - " + currentUsername);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to return to dashboard", e.getMessage());
        }
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
        refreshCredits();
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

        maxDegreeTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to get max degree", maxDegreeTask.getException().getMessage());
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

    private void startCreditRefresh() {
        creditRefreshTimer = new Timer(true);
        creditRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshCredits();
            }
        }, 0, 2000);
    }

    private void refreshCredits() {
        new Thread(() -> {
            try {
                List<HttpClientUtil.UserInfo> users = HttpClientUtil.getAllUsers();
                for (HttpClientUtil.UserInfo user : users) {
                    if (user.username.equals(currentUsername)) {
                        int credits = user.credits;
                        Platform.runLater(() -> creditsLabel.setText(String.valueOf(credits)));
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }).start();
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