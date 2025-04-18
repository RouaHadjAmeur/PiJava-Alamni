package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import service.UtilisateurService;

public class ResetPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private ProgressBar passwordStrength;
    @FXML private Label strengthLabel;

    private String token;

    public void setToken(String token) {
        this.token = token;
    }

    @FXML
    public void initialize() {
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            double strength = calculatePasswordStrength(newValue);
            passwordStrength.setProgress(strength);

            if (strength < 0.3) {
                strengthLabel.setText("Force du mot de passe: Faible");
                passwordStrength.setStyle("-fx-accent: #f44336;"); // Rouge
            } else if (strength < 0.7) {
                strengthLabel.setText("Force du mot de passe: Moyen");
                passwordStrength.setStyle("-fx-accent: #ff9800;"); // Orange
            } else {
                strengthLabel.setText("Force du mot de passe: Fort");
                passwordStrength.setStyle("-fx-accent: #4caf50;"); // Vert
            }

            validateInputs();
        });

        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateInputs();
        });
    }

    private void validateInputs() {
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        boolean isValid = !password.isEmpty() && password.equals(confirmPassword) && password.length() >= 8;

        resetButton.setDisable(!isValid);

        if (!password.isEmpty() && !confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            statusLabel.setText("Les mots de passe ne correspondent pas");
        } else if (!password.isEmpty() && password.length() < 8) {
            statusLabel.setText("Le mot de passe doit contenir au moins 8 caractères");
        } else {
            statusLabel.setText("");
        }
    }

    @FXML
    private void handleResetPassword() {
        if (token == null || token.isEmpty()) {
            statusLabel.setText("Token de réinitialisation invalide");
            return;
        }

        String newPassword = passwordField.getText().trim();

        boolean success = UtilisateurService.updatePasswordWithToken(token, newPassword);

        if (success) {
            messageLabel.setText("Votre mot de passe a été réinitialisé avec succès !");
            messageLabel.setStyle("-fx-text-fill: #4caf50;");

            // Désactiver les champs après réinitialisation
            passwordField.setDisable(true);
            confirmPasswordField.setDisable(true);
            resetButton.setDisable(true);

            // Fermer la fenêtre après quelques secondes
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> {
                    ((Stage) resetButton.getScene().getWindow()).close();
                });
            }).start();
        } else {
            statusLabel.setText("Erreur lors de la réinitialisation du mot de passe. Le lien a peut-être expiré.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private double calculatePasswordStrength(String password) {
        if (password.isEmpty()) return 0;

        double strength = 0;
        // Longueur
        strength += 0.3 * Math.min(1, password.length() / 12.0);

        // Complexité
        if (password.matches(".*[A-Z].*")) strength += 0.2;
        if (password.matches(".*[a-z].*")) strength += 0.2;
        if (password.matches(".*[0-9].*")) strength += 0.2;
        if (password.matches(".*[^A-Za-z0-9].*")) strength += 0.2;

        return Math.min(strength, 1.0);
    }
}