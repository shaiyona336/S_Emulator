package fxml.statistics;

import dtos.ExecutionDetails;
import dtos.RunHistoryDetails;
import fxml.debugger.DebuggerPanelController;
import http.HttpClientUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsController {

    @FXML private TableView<RunHistoryRow> statisticsTableView;
    @FXML private TableColumn<RunHistoryRow, Integer> runNumberColumn;
    @FXML private TableColumn<RunHistoryRow, Integer> degreeColumn;
    @FXML private TableColumn<RunHistoryRow, String> inputsColumn;
    @FXML private TableColumn<RunHistoryRow, Long> outputYColumn;
    @FXML private TableColumn<RunHistoryRow, Integer> cyclesColumn;
    @FXML private Button showButton;
    @FXML private Button rerunButton;

    private List<RunHistoryDetails> currentRunHistory;
    private DebuggerPanelController debuggerController;

    @FXML
    public void initialize() {
        runNumberColumn.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        degreeColumn.setCellValueFactory(new PropertyValueFactory<>("degree"));
        inputsColumn.setCellValueFactory(new PropertyValueFactory<>("inputs"));
        outputYColumn.setCellValueFactory(new PropertyValueFactory<>("outputY"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        showButton.setDisable(true);
        rerunButton.setDisable(true);

        statisticsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean hasSelection = newSelection != null;
                    showButton.setDisable(!hasSelection);
                    rerunButton.setDisable(!hasSelection);
                }
        );

        showButton.setOnAction(e -> showFullResults());
        rerunButton.setOnAction(e -> rerunWithInputs());
    }

    public void setDebuggerController(DebuggerPanelController debuggerController) {
        this.debuggerController = debuggerController;
    }

    public void loadStatistics(List<RunHistoryDetails> runHistory) {
        this.currentRunHistory = runHistory;
        statisticsTableView.getItems().clear();
        if (runHistory == null || runHistory.isEmpty()) {
            return;
        }

        ObservableList<RunHistoryRow> historyRows = FXCollections.observableArrayList();
        for (RunHistoryDetails details : runHistory) {
            historyRows.add(new RunHistoryRow(
                    details.runNumber(),
                    details.expansionDegree(),
                    formatInputs(details.inputs()),
                    details.yValue(),
                    details.cyclesNumber()
            ));
        }
        statisticsTableView.setItems(historyRows);
    }

    private String formatInputs(List<Long> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return "None";
        }
        return inputs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    public void clearHistory() {
        if (statisticsTableView != null) {
            statisticsTableView.getItems().clear();
            currentRunHistory = null;
        }
        showButton.setDisable(true);
        rerunButton.setDisable(true);
    }

    private void showFullResults() {
        RunHistoryRow selectedRow = statisticsTableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null || currentRunHistory == null) {
            return;
        }

        RunHistoryDetails selectedRun = currentRunHistory.stream()
                .filter(r -> r.runNumber() == selectedRow.getRunNumber())
                .findFirst()
                .orElse(null);

        if (selectedRun == null) {
            return;
        }

        // Re-run via HTTP to get full results
        Task<ExecutionDetails> rerunTask = new Task<>() {
            @Override
            protected ExecutionDetails call() throws Exception {
                Long[] inputs = selectedRun.inputs().toArray(new Long[0]);
                return HttpClientUtil.runProgram(selectedRun.expansionDegree(), inputs);
            }
        };

        rerunTask.setOnSucceeded(e -> {
            ExecutionDetails results = rerunTask.getValue();
            Platform.runLater(() -> showResultsDialog(selectedRun, results));
        });

        rerunTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Could not retrieve full results", rerunTask.getException().getMessage());
        });

        new Thread(rerunTask).start();
    }

    private void showResultsDialog(RunHistoryDetails runDetails, ExecutionDetails results) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Run #" + runDetails.runNumber() + " - Full Results");
        dialog.setHeaderText("Execution Results - Degree: " + runDetails.expansionDegree());

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);
        content.setPrefHeight(400);

        Label infoLabel = new Label(String.format(
                "Inputs: %s\nTotal Cycles: %d\n",
                formatInputs(runDetails.inputs()),
                runDetails.cyclesNumber()
        ));
        content.getChildren().add(infoLabel);

        TableView<VariableRow> variableTable = new TableView<>();
        variableTable.setPrefHeight(300);

        TableColumn<VariableRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);

        TableColumn<VariableRow, String> nameCol = new TableColumn<>("Variable");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(100);

        TableColumn<VariableRow, Long> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(150);

        variableTable.getColumns().addAll(typeCol, nameCol, valueCol);

        ObservableList<VariableRow> variableRows = FXCollections.observableArrayList();
        variableRows.add(new VariableRow("Output", "y", runDetails.yValue()));

        if (results.programDetails() != null && results.programDetails().inputVariables() != null) {
            results.programDetails().inputVariables().forEach(var -> {
                long value = results.variables().getVariableValue(var);
                variableRows.add(new VariableRow("Input", var.getStringVariable(), value));
            });
        }

        if (results.programDetails() != null && results.programDetails().workVariables() != null) {
            results.programDetails().workVariables().forEach(var -> {
                long value = results.variables().getVariableValue(var);
                variableRows.add(new VariableRow("Work", var.getStringVariable(), value));
            });
        }

        variableTable.setItems(variableRows);
        content.getChildren().add(variableTable);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void rerunWithInputs() {
        if (debuggerController == null) {
            showAlert(Alert.AlertType.WARNING, "Warning",
                    "Cannot re-run", "Debugger controller not available");
            return;
        }

        RunHistoryRow selectedRow = statisticsTableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null || currentRunHistory == null) {
            return;
        }

        RunHistoryDetails selectedRun = currentRunHistory.stream()
                .filter(r -> r.runNumber() == selectedRow.getRunNumber())
                .findFirst()
                .orElse(null);

        if (selectedRun == null) {
            return;
        }

        debuggerController.populateInputs(selectedRun.inputs());

        showAlert(Alert.AlertType.INFORMATION, "Inputs Loaded",
                "Inputs from Run #" + selectedRun.runNumber() + " have been loaded",
                "Click 'Start Normal Run' or 'Start Debug Run' to execute with these inputs.");
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

    public static class VariableRow {
        private final String type;
        private final String name;
        private final Long value;

        public VariableRow(String type, String name, Long value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public Long getValue() { return value; }
    }
}