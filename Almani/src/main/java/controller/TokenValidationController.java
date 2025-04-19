package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.UtilisateurService;

public class TokenValidationController {

    @FXML
    private TextField tokenField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private void handleValidate() {
        String token = tokenField.getText().trim();
        String newPassword = newPasswordField.getText().trim();

        if (token.isEmpty() || newPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Veuillez remplir tous les champs.");
            return;
        }

        boolean success = UtilisateurService.updatePasswordWithToken(token, newPassword);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "🔓 Mot de passe mis à jour avec succès !");
            closeWindow();
        } else {
            showAlert(Alert.AlertType.ERROR, "❌ Code invalide ou expiré.");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tokenField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Réinitialisation");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}