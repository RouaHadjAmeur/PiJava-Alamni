package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import util.EmailService;
import service.UtilisateurService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;

public class ResetPasswordController {

    @FXML
    private TextField emailField;

    // Action appelée par le bouton "Envoyer"
    @FXML
    private void handleSend() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Veuillez entrer une adresse e-mail.");
            return;
        }

        String token = java.util.UUID.randomUUID().toString();

        // 🔐 Associer le token à cet utilisateur (à stocker quelque part)
        if (!UtilisateurService.emailExiste(email)) {
            showAlert(Alert.AlertType.ERROR, "Aucun utilisateur ne correspond à cet e-mail.");
            return;
        }
        // Stocker le token temporairement (à améliorer avec base de données)
        UtilisateurService.associerTokenAvecUtilisateur(email, token);

        // 📧 Contenu HTML avec le code (non cliquable)
        String emailContent = """
                    Bonjour,

                    Voici votre code de réinitialisation de mot de passe :

                    🔑 Code : %s

                    Ouvrez l'application Alamni et entrez ce code pour créer un nouveau mot de passe.

                    Ce code expirera dans 24h.

                    -- Équipe Alamni
                """.formatted(token);

        boolean success = EmailService.envoyerMail(
                email,
                "🔐 Réinitialisation de mot de passe - Alamni",
                emailContent
        );

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Le code a été envoyé par e-mail.");
            closeWindow();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SaisirToken.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle("Saisir le code de réinitialisation");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur lors de l'ouverture de la fenêtre.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur lors de l'envoi de l'e-mail.");
        }
    }


    // Action appelée par le bouton "Annuler"
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    // Méthode utilitaire pour fermer la fenêtre
    private void closeWindow() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    // Affiche une alerte simple
    private void showAlert(Alert.AlertType type, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Réinitialisation");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}