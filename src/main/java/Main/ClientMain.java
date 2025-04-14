package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.DatabaseConnection;

import java.io.IOException;
import java.net.URL;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Simuler une connexion utilisateur (à remplacer par votre système d'authentification

            // Charger l'interface client des cours
            URL fxmlUrl = getClass().getResource("/fxml/client-cours-list.fxml");
            if (fxmlUrl == null) {
                System.err.println("Erreur: Impossible de trouver le fichier FXML");
                System.err.println("Chemin recherché: /fxml/client-cours-list.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);

            // Chargement du fichier CSS client
            URL cssUrl = getClass().getResource("/css/client-styles.css");
            if (cssUrl == null) {
                System.err.println("Erreur: Impossible de trouver le fichier CSS client");
                System.err.println("Chemin recherché: /css/client-styles.css");
            } else {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(cssUrl.toExternalForm());

                // Configuration de la fenêtre principale
                primaryStage.setTitle("ALAMNI - Mes Cours");
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