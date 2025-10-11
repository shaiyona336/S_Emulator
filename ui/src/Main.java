import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import fxml.Login.LoginController;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the login screen first
        FXMLLoader loader = new FXMLLoader();

        // Try to load the resource
        java.net.URL fxmlLocation = getClass().getResource("/fxml/Login/login.fxml");

        if (fxmlLocation == null) {
            System.err.println("ERROR: Could not find /fxml/Login/login.fxml");
            System.err.println("Trying alternative path...");
            fxmlLocation = getClass().getResource("fxml/Login/login.fxml");
        }

        if (fxmlLocation == null) {
            throw new RuntimeException("Could not find login.fxml in resources!");
        }

        loader.setLocation(fxmlLocation);
        Parent root = loader.load();

        // Get the controller and pass the stage
        LoginController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        // Set up the scene
        Scene scene = new Scene(root, 400, 350);
        primaryStage.setTitle("S-Emulator - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}