package fxml.dashboard;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import http.HttpClientUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardController {

    @FXML private Label usernameLabel;
    @FXML private Label creditsLabel;
    @FXML private Button chargeCreditsButton;
    @FXML private Button loadFileButton;

    // Users table
    @FXML private TableView<UserRow> usersTableView;
    @FXML private TableColumn<UserRow, String> userNameColumn;
    @FXML private TableColumn<UserRow, Integer> userProgramsColumn;
    @FXML private TableColumn<UserRow, Integer> userFunctionsColumn;
    @FXML private TableColumn<UserRow, Integer> userCreditsColumn;
    @FXML private TableColumn<UserRow, Integer> userUsedCreditsColumn;
    @FXML private TableColumn<UserRow, Integer> userRunsColumn;
    @FXML private Button unselectUserButton;

    // Programs table
    @FXML private TableView<ProgramRow> programsTableView;
    @FXML private TableColumn<ProgramRow, String> programNameColumn;
    @FXML private TableColumn<ProgramRow, String> programOwnerColumn;
    @FXML private TableColumn<ProgramRow, Integer> programInstructionsColumn;
    @FXML private TableColumn<ProgramRow, Integer> programMaxDegreeColumn;
    @FXML private TableColumn<ProgramRow, Integer> programRunsColumn;
    @FXML private TableColumn<ProgramRow, Integer> programAvgCostColumn;
    @FXML private Button executeProgramButton;

    // Functions table
    @FXML private TableView<FunctionRow> functionsTableView;
    @FXML private TableColumn<FunctionRow, String> functionNameColumn;
    @FXML private TableColumn<FunctionRow, String> functionProgramColumn;
    @FXML private TableColumn<FunctionRow, String> functionOwnerColumn;
    @FXML private TableColumn<FunctionRow, Integer> functionInstructionsColumn;
    @FXML private TableColumn<FunctionRow, Integer> functionMaxDegreeColumn;
    @FXML private Button executeFunctionButton;

    // History table
    @FXML private Label historyTitleLabel;
    @FXML private TableView<HistoryRow> historyTableView;
    @FXML private TableColumn<HistoryRow, Integer> historyRunNumColumn;
    @FXML private TableColumn<HistoryRow, String> historyTypeColumn;
    @FXML private TableColumn<HistoryRow, String> historyNameColumn;
    @FXML private TableColumn<HistoryRow, String> historyArchColumn;
    @FXML private TableColumn<HistoryRow, Integer> historyDegreeColumn;
    @FXML private TableColumn<HistoryRow, Long> historyYValueColumn;
    @FXML private TableColumn<HistoryRow, Integer> historyCyclesColumn;
    @FXML private Button showStatusButton;
    @FXML private Button rerunButton;

    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private String currentUsername;
    private Timer refreshTimer;

    // Fields to preserve selections during refresh
    private String selectedProgramName = null;
    private String selectedFunctionName = null;
    private String selectedUserName = null;

    // Flags to prevent listener interference during refresh
    private boolean isRefreshingPrograms = false;
    private boolean isRefreshingFunctions = false;
    private boolean isRefreshingUsers = false;

    @FXML
    public void initialize() {
        setupUserTable();
        setupProgramsTable();
        setupFunctionsTable();
        setupHistoryTable();

        executeProgramButton.setDisable(true);
        executeFunctionButton.setDisable(true);
        showStatusButton.setDisable(true);
        rerunButton.setDisable(true);
        unselectUserButton.setDisable(true);

        Platform.runLater(() -> loadMyHistory());


        // Selection listeners - SAVE selections (but only when NOT refreshing)
        programsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    executeProgramButton.setDisable(newVal == null);
                    if (!isRefreshingPrograms) {  // Only update if not refreshing
                        if (newVal != null) {
                            selectedProgramName = newVal.getName();
                        } else {
                            selectedProgramName = null;
                        }
                    }
                }
        );

        functionsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    executeFunctionButton.setDisable(newVal == null);
                    if (!isRefreshingFunctions) {  // Only update if not refreshing
                        if (newVal != null) {
                            selectedFunctionName = newVal.getName();
                        } else {
                            selectedFunctionName = null;
                        }
                    }
                }
        );

        historyTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    boolean hasSelection = newVal != null;
                    showStatusButton.setDisable(!hasSelection);
                    rerunButton.setDisable(!hasSelection);
                }
        );

        usersTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    unselectUserButton.setDisable(newVal == null);
                    if (!isRefreshingUsers) {  // Only update if not refreshing
                        if (newVal != null) {
                            selectedUserName = newVal.getUsername();
                            loadUserHistory(newVal.getUsername());
                        } else {
                            selectedUserName = null;
                            loadMyHistory();
                        }
                    }
                }
        );
    }



    public void setUsername(String username) {
        this.currentUsername = username;
        usernameLabel.setText("User: " + username);
        startAutoRefresh();
        refreshData();
    }

    private void setupUserTable() {
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        userProgramsColumn.setCellValueFactory(new PropertyValueFactory<>("programCount"));
        userFunctionsColumn.setCellValueFactory(new PropertyValueFactory<>("functionCount"));
        userCreditsColumn.setCellValueFactory(new PropertyValueFactory<>("credits"));
        userUsedCreditsColumn.setCellValueFactory(new PropertyValueFactory<>("usedCredits"));
        userRunsColumn.setCellValueFactory(new PropertyValueFactory<>("runCount"));
    }

    private void setupProgramsTable() {
        programNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        programOwnerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));
        programInstructionsColumn.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        programMaxDegreeColumn.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
        programRunsColumn.setCellValueFactory(new PropertyValueFactory<>("runCount"));
        programAvgCostColumn.setCellValueFactory(new PropertyValueFactory<>("avgCost"));
    }

    private void setupFunctionsTable() {
        functionNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        functionProgramColumn.setCellValueFactory(new PropertyValueFactory<>("programName"));
        functionOwnerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));
        functionInstructionsColumn.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        functionMaxDegreeColumn.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
    }

    private void setupHistoryTable() {
        historyRunNumColumn.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        historyTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        historyNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        historyArchColumn.setCellValueFactory(new PropertyValueFactory<>("architecture"));
        historyDegreeColumn.setCellValueFactory(new PropertyValueFactory<>("degree"));
        historyYValueColumn.setCellValueFactory(new PropertyValueFactory<>("yValue"));
        historyCyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));
    }

    @FXML
    private void handleChargeCredits() {
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Charge Credits");
        dialog.setHeaderText("Add credits to your account");
        dialog.setContentText("Amount:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                int credits = Integer.parseInt(amount);
                if (credits > 0) {
                    loadingIndicator.setVisible(true);
                    new Thread(() -> {
                        try {
                            HttpClientUtil.addCredits(credits);
                            Platform.runLater(() -> {
                                showStatus("Added " + credits + " credits");
                                refreshData();
                                loadingIndicator.setVisible(false);
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                showError("Failed to add credits: " + e.getMessage());
                                loadingIndicator.setVisible(false);
                            });
                        }
                    }).start();
                } else {
                    showError("Amount must be positive");
                }
            } catch (NumberFormatException e) {
                showError("Invalid amount");
            }
        });
    }

    @FXML
    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Program File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );

        File file = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (file != null) {
            loadingIndicator.setVisible(true);
            new Thread(() -> {
                try {
                    HttpClientUtil.uploadFile(file);
                    Platform.runLater(() -> {
                        showStatus("File loaded successfully: " + file.getName());
                        refreshData();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to load file: " + e.getMessage()));
                } finally {
                    Platform.runLater(() -> loadingIndicator.setVisible(false));
                }
            }).start();
        }
    }

    @FXML
    private void handleExecuteProgram() {
        ProgramRow selected = programsTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            navigateToExecutionScreen(selected.getName(), "PROGRAM");
        }
    }

    @FXML
    private void handleExecuteFunction() {
        FunctionRow selected = functionsTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            navigateToExecutionScreen(selected.getName(), "FUNCTION");
        }
    }

    @FXML
    private void handleUnselectUser() {
        usersTableView.getSelectionModel().clearSelection();
        historyTitleLabel.setText("My History / Statistics");
        loadMyHistory();
    }

    @FXML
    private void handleShowStatus() {
        // TODO: Show full variable status for selected run
        showStatus("Show status not yet implemented");
    }

    @FXML
    private void handleRerun() {
        // TODO: Re-run selected execution
        showStatus("Re-run not yet implemented");
    }

    private void navigateToExecutionScreen(String programName, String type) {
        try {
            // Stop auto-refresh before navigating
            if (refreshTimer != null) {
                refreshTimer.cancel();
            }

            HttpClientUtil.setContextProgram(programName);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/app/abc3.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof fxml.app.mainController) {
                fxml.app.mainController mainCtrl = (fxml.app.mainController) controller;
                mainCtrl.setUsername(currentUsername);
                mainCtrl.initializeWithProgram();
            }

            Stage stage = (Stage) loadFileButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("S-Emulator - Execute: " + programName);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to navigate: " + e.getMessage());
        }
    }

    private void refreshData() {
        loadUsers();
        loadPrograms();
        loadFunctions();
        loadMyCredits();

        // Refresh history based on current selection
        if (selectedUserName != null) {
            loadUserHistory(selectedUserName);
        } else {
            loadMyHistory();
        }
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                List<HttpClientUtil.UserInfo> users = HttpClientUtil.getAllUsers();
                ObservableList<UserRow> userRows = FXCollections.observableArrayList();
                for (HttpClientUtil.UserInfo user : users) {
                    userRows.add(new UserRow(
                            user.username,
                            user.programsUploaded,
                            user.functionsUploaded,
                            user.credits,
                            user.usedCredits,
                            user.totalRuns
                    ));
                }
                Platform.runLater(() -> {
                    isRefreshingUsers = true;  // Set flag before updating
                    usersTableView.setItems(userRows);
                    // Restore selection
                    if (selectedUserName != null) {
                        for (UserRow row : userRows) {
                            if (row.getUsername().equals(selectedUserName)) {
                                usersTableView.getSelectionModel().select(row);
                                break;
                            }
                        }
                    }
                    isRefreshingUsers = false;  // Clear flag after updating
                });
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Failed to load users: " + e.getMessage()));
            }
        }).start();
    }

    private void loadPrograms() {
        new Thread(() -> {
            try {
                List<HttpClientUtil.ProgramInfoDTO> programs = HttpClientUtil.getAllPrograms();
                ObservableList<ProgramRow> programRows = FXCollections.observableArrayList();
                for (HttpClientUtil.ProgramInfoDTO prog : programs) {
                    programRows.add(new ProgramRow(
                            prog.name,
                            prog.owner,
                            prog.instructionCount,
                            prog.maxDegree,
                            prog.runCount,      // This will now update
                            prog.avgCost        // This will now update
                    ));
                }
                Platform.runLater(() -> {
                    isRefreshingPrograms = true;
                    programsTableView.setItems(programRows);
                    // Restore selection
                    if (selectedProgramName != null) {
                        for (ProgramRow row : programRows) {
                            if (row.getName().equals(selectedProgramName)) {
                                programsTableView.getSelectionModel().select(row);
                                break;
                            }
                        }
                    }
                    isRefreshingPrograms = false;
                });
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Failed to load programs: " + e.getMessage()));
            }
        }).start();
    }

    private void loadFunctions() {
        new Thread(() -> {
            try {
                List<HttpClientUtil.FunctionInfoDTO> functions = HttpClientUtil.getAllFunctions();
                ObservableList<FunctionRow> functionRows = FXCollections.observableArrayList();
                for (HttpClientUtil.FunctionInfoDTO func : functions) {
                    functionRows.add(new FunctionRow(
                            func.name,
                            func.programName,
                            func.owner,
                            func.instructionCount,
                            func.maxDegree
                    ));
                }
                Platform.runLater(() -> {
                    isRefreshingFunctions = true;  // Set flag before updating
                    functionsTableView.setItems(functionRows);
                    // Restore selection
                    if (selectedFunctionName != null) {
                        for (FunctionRow row : functionRows) {
                            if (row.getName().equals(selectedFunctionName)) {
                                functionsTableView.getSelectionModel().select(row);
                                break;
                            }
                        }
                    }
                    isRefreshingFunctions = false;  // Clear flag after updating
                });
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Failed to load functions: " + e.getMessage()));
            }
        }).start();
    }

    private void loadMyCredits() {
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

    private void loadMyHistory() {
        historyTitleLabel.setText("My History / Statistics");
        new Thread(() -> {
            try {
                List<HttpClientUtil.UserHistoryEntry> history = HttpClientUtil.getUserHistory(null);
                ObservableList<HistoryRow> historyRows = FXCollections.observableArrayList();

                for (HttpClientUtil.UserHistoryEntry entry : history) {
                    historyRows.add(new HistoryRow(
                            entry.runNumber,
                            entry.type,
                            entry.name,
                            entry.architecture,
                            entry.degree,
                            entry.yValue,
                            entry.cycles
                    ));
                }

                Platform.runLater(() -> historyTableView.setItems(historyRows));
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Failed to load history: " + e.getMessage()));
            }
        }).start();
    }

    private void loadUserHistory(String username) {
        historyTitleLabel.setText(username + "'s History / Statistics");
        new Thread(() -> {
            try {
                List<HttpClientUtil.UserHistoryEntry> history = HttpClientUtil.getUserHistory(username);
                ObservableList<HistoryRow> historyRows = FXCollections.observableArrayList();

                for (HttpClientUtil.UserHistoryEntry entry : history) {
                    historyRows.add(new HistoryRow(
                            entry.runNumber,
                            entry.type,
                            entry.name,
                            entry.architecture,
                            entry.degree,
                            entry.yValue,
                            entry.cycles
                    ));
                }

                Platform.runLater(() -> historyTableView.setItems(historyRows));
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Failed to load history: " + e.getMessage()));
            }
        }).start();
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshData();  // This calls loadPrograms() which updates the table
            }
        }, 2000, 2000); // Refresh every 2 seconds
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }

    private void showStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Row classes for tables
    public static class UserRow {
        private final SimpleStringProperty username;
        private final SimpleIntegerProperty programCount;
        private final SimpleIntegerProperty functionCount;
        private final SimpleIntegerProperty credits;
        private final SimpleIntegerProperty usedCredits;
        private final SimpleIntegerProperty runCount;

        public UserRow(String username, int programCount, int functionCount,
                       int credits, int usedCredits, int runCount) {
            this.username = new SimpleStringProperty(username);
            this.programCount = new SimpleIntegerProperty(programCount);
            this.functionCount = new SimpleIntegerProperty(functionCount);
            this.credits = new SimpleIntegerProperty(credits);
            this.usedCredits = new SimpleIntegerProperty(usedCredits);
            this.runCount = new SimpleIntegerProperty(runCount);
        }

        public String getUsername() { return username.get(); }
        public int getProgramCount() { return programCount.get(); }
        public int getFunctionCount() { return functionCount.get(); }
        public int getCredits() { return credits.get(); }
        public int getUsedCredits() { return usedCredits.get(); }
        public int getRunCount() { return runCount.get(); }
    }

    public static class ProgramRow {
        private final SimpleStringProperty name;
        private final SimpleStringProperty owner;
        private final SimpleIntegerProperty instructionCount;
        private final SimpleIntegerProperty maxDegree;
        private final SimpleIntegerProperty runCount;
        private final SimpleIntegerProperty avgCost;

        public ProgramRow(String name, String owner, int instructionCount,
                          int maxDegree, int runCount, int avgCost) {
            this.name = new SimpleStringProperty(name);
            this.owner = new SimpleStringProperty(owner);
            this.instructionCount = new SimpleIntegerProperty(instructionCount);
            this.maxDegree = new SimpleIntegerProperty(maxDegree);
            this.runCount = new SimpleIntegerProperty(runCount);
            this.avgCost = new SimpleIntegerProperty(avgCost);
        }

        public String getName() { return name.get(); }
        public String getOwner() { return owner.get(); }
        public int getInstructionCount() { return instructionCount.get(); }
        public int getMaxDegree() { return maxDegree.get(); }
        public int getRunCount() { return runCount.get(); }
        public int getAvgCost() { return avgCost.get(); }
    }

    public static class FunctionRow {
        private final SimpleStringProperty name;
        private final SimpleStringProperty programName;
        private final SimpleStringProperty owner;
        private final SimpleIntegerProperty instructionCount;
        private final SimpleIntegerProperty maxDegree;

        public FunctionRow(String name, String programName, String owner,
                           int instructionCount, int maxDegree) {
            this.name = new SimpleStringProperty(name);
            this.programName = new SimpleStringProperty(programName);
            this.owner = new SimpleStringProperty(owner);
            this.instructionCount = new SimpleIntegerProperty(instructionCount);
            this.maxDegree = new SimpleIntegerProperty(maxDegree);
        }

        public String getName() { return name.get(); }
        public String getProgramName() { return programName.get(); }
        public String getOwner() { return owner.get(); }
        public int getInstructionCount() { return instructionCount.get(); }
        public int getMaxDegree() { return maxDegree.get(); }
    }

    public static class HistoryRow {
        private final SimpleIntegerProperty runNumber;
        private final SimpleStringProperty type;
        private final SimpleStringProperty name;
        private final SimpleStringProperty architecture;
        private final SimpleIntegerProperty degree;
        private final SimpleIntegerProperty yValue;
        private final SimpleIntegerProperty cycles;

        public HistoryRow(int runNumber, String type, String name, String architecture,
                          int degree, long yValue, int cycles) {
            this.runNumber = new SimpleIntegerProperty(runNumber);
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.architecture = new SimpleStringProperty(architecture);
            this.degree = new SimpleIntegerProperty(degree);
            this.yValue = new SimpleIntegerProperty((int)yValue);
            this.cycles = new SimpleIntegerProperty(cycles);
        }

        public int getRunNumber() { return runNumber.get(); }
        public String getType() { return type.get(); }
        public String getName() { return name.get(); }
        public String getArchitecture() { return architecture.get(); }
        public int getDegree() { return degree.get(); }
        public long getYValue() { return yValue.get(); }
        public int getCycles() { return cycles.get(); }
    }
}