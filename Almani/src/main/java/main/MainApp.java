package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import service.UtilisateurService;
import controller.ResetPasswordController;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
        primaryStage.setTitle("Connexion - Alamni");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Exécuter la migration des mots de passe
        System.out.println("Démarrage de la migration des mots de passe...");
        UtilisateurService.migrerMotsDePasse();
        System.out.println("Migration terminée.");

        // Lancer l'application normalement
        launch(args);
    }

    public static void openResetPasswordWindow(String token) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Réinitialisation de mot de passe");
        alert.setHeaderText("Lien de réinitialisation reçu");
        alert.setContentText("Fonctionnalité en cours d'implémentation.\n\nToken: " + token);
        alert.showAndWait();
    }
}