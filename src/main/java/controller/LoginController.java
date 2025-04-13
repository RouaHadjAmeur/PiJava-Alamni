package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import model.Utilisateur;
import service.UtilisateurService;
import util.Session;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // Connexion
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Veuillez remplir vos identifiants.");
            return;
        }

        Utilisateur user = UtilisateurService.login(email, password);
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Connexion échouée",
                    "Email ou mot de passe invalide, ou compte non validé.");
            return;
        }

        Session.setUtilisateurConnecte(user);

        try {
            Stage stage = (Stage) emailField.getScene().getWindow();

            String fxmlRoute = switch (user.getRole().toUpperCase()) {
                case "ADMINISTRATEUR" -> "/view/admin.fxml";
                case "ÉLÈVE" -> "/view/eleve.fxml";
                case "ENSEIGNANT" -> "/view/enseignant.fxml";
                case "PARENT" -> "/view/parent.fxml";
                default -> null;
            };

            if (fxmlRoute == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Rôle inconnu : " + user.getRole());
                return;
            }

            // Nouveau test pour afficher le chemin
            System.out.println("🧪 Chemin vers FXML : " + getClass().getResource(fxmlRoute));

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlRoute)); // ✅ Location bien définie
            if (loader.getLocation() == null) {
                throw new IOException("FXML non trouvé : " + fxmlRoute);
            }

            Parent root = loader.load(); // ✅ Fonctionne maintenant

            stage.setScene(new Scene(root));
            stage.setTitle("Bienvenue - " + user.getRole());
////mtaa dimension page //////
    //stage.setMaximized(true);//page dimension 100%
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'écran :\n" + e.getMessage());
        }
    }

    // Inscription
    @FXML
    private void handleSignupLink(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Créer un compte");
            stage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'écran d'inscription.");
        }
    }

    // Mot de passe oublié
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Mot de passe oublié", "Veuillez contacter l’administrateur.");
    }

    // Alerte réutilisable
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}