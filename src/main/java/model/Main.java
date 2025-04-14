package model;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.DatabaseConnection;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Utiliser getResource pour obtenir l'URL du fichier FXML
            URL fxmlUrl = getClass().getResource("/fxml/main-view.fxml");
            if (fxmlUrl == null) {
                System.err.println("Erreur: Impossible de trouver le fichier FXML");
                System.err.println("Chemin recherché: /fxml/main-view.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);

            // Chargement du fichier CSS
            URL cssUrl = getClass().getResource("/css/styles.css");
            if (cssUrl == null) {
                System.err.println("Erreur: Impossible de trouver le fichier CSS");
                System.err.println("Chemin recherché: /util/styles.css");
            } else {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(cssUrl.toExternalForm());

                // Configuration de la fenêtre principale
                primaryStage.setTitle("ALAMNI - Gestion des Cours");
                primaryStage.setScene(scene);
                primaryStage.setMaximized(true);
                primaryStage.show();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'interface: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Fermeture de la connexion à la base de données lors de la fermeture de l'application
        DatabaseConnection.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}