package fxml.statistics;

import dtos.RunHistoryDetails;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsController {

    @FXML private TableView<RunHistoryRow> statisticsTableView;
    @FXML private TableColumn<RunHistoryRow, Integer> runNumberColumn;
    @FXML private TableColumn<RunHistoryRow, Integer> degreeColumn;
    @FXML private TableColumn<RunHistoryRow, String> inputsColumn;
    @FXML private TableColumn<RunHistoryRow, Long> outputYColumn;
    @FXML private TableColumn<RunHistoryRow, Integer> cyclesColumn;

    @FXML
    public void initialize() {
        runNumberColumn.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        degreeColumn.setCellValueFactory(new PropertyValueFactory<>("degree"));
        inputsColumn.setCellValueFactory(new PropertyValueFactory<>("inputs"));
        outputYColumn.setCellValueFactory(new PropertyValueFactory<>("outputY"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));
    }

    public void loadStatistics(List<RunHistoryDetails> runHistory) {
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
        }
    }
}