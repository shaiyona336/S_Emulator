package fxml.Login;

import fxml.app.mainController;
import http.HttpClientUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameTextField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void initialize() {
        // Allow Enter key to trigger login
        usernameTextField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameTextField.getText().trim();

        // Validate username
        if (username.isEmpty()) {
            showError("Please enter a username");
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Create login task
        Task<Void> loginTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Call the login endpoint
                HttpClientUtil.login(username);
                return null;
            }
        };

        loginTask.setOnSucceeded(event -> {
            setLoadingState(false);
            // Login successful - navigate to main screen
            navigateToMainScreen(username);
        });

        loginTask.setOnFailed(event -> {
            setLoadingState(false);
            Throwable exception = loginTask.getException();
            String errorMessage = exception.getMessage();

            // Check if username already exists
            if (errorMessage != null && errorMessage.contains("already exists")) {
                showError("Username already taken. Please choose another.");
            } else {
                showError("Login failed: " + errorMessage);
            }
        });

        // Start the task in background thread
        new Thread(loginTask).start();
    }

    private void navigateToMainScreen(String username) {
        try {
            // Load the dashboard screen instead
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard/dashboard.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the username
            Object controller = loader.getController();
            if (controller instanceof fxml.dashboard.DashboardController) {
                ((fxml.dashboard.DashboardController) controller).setUsername(username);
            }

            // Create new scene and show it
            Scene scene = new Scene(root, 1200, 800); // Larger size for dashboard
            primaryStage.setScene(scene);
            primaryStage.setTitle("S-Emulator Dashboard - " + username);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load dashboard: " + e.getMessage());
        }
    }

    private void setLoadingState(boolean loading) {
        Platform.runLater(() -> {
            loginButton.setDisable(loading);
            usernameTextField.setDisable(loading);
            loadingIndicator.setVisible(loading);
            errorLabel.setVisible(false);
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

}