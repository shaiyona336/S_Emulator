import components.engine.Engine;
import components.engine.StandardEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


import static javafx.application.Application.launch;

public class Main  extends Application {
    public static void main(String[] args) {
//        LogicManager logicManager = new LogicManager();
//        logicManager.run();
        launch(args);


    }


    @Override
    public void start(Stage primaryStage) throws IOException {
        Engine engine = new StandardEngine();
        FXMLLoader fxmlLoader = new FXMLLoader();

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/app/abc3.fxml"));
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        primaryStage.setTitle("Hello World!");
        primaryStage.setScene(scene);
        primaryStage.show();



    }
}
