package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import java.net.URL;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            URL url = getClass().getResource("/view/Main.fxml");

            System.out.println("Main.fxml loaded from: " + url);
            Parent root = FXMLLoader.load(url);
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Planning App");
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        launch(args);
    }
}
