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

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ProgressBar;

import java.io.IOException;

import util.EmailService;


import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.util.Pair;
import java.util.Map;
import java.util.HashMap;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // Connexion
    // Modification dans LoginController.java - méthode handleLogin
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlRoute));
            if (loader.getLocation() == null) {
                throw new IOException("FXML non trouvé : " + fxmlRoute);
            }

            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setTitle("Bienvenue - " + user.getRole());
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
        // Créer une boîte de dialogue simplifiée
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Réinitialisation du mot de passe");
        dialog.setHeaderText("Veuillez entrer votre adresse email");

        // Définir les boutons
        ButtonType confirmButtonType = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Créer la zone de saisie d'email
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailInput = new TextField();
        emailInput.setPromptText("Email");
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailInput, 1, 0);

        // Activer/désactiver le bouton en fonction de la saisie de l'email
        Node confirmButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setDisable(true);

        // Validation pour activer le bouton uniquement si l'email est saisi
        emailInput.textProperty().addListener((observable, oldValue, newValue) -> {
            confirmButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Définir le résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return emailInput.getText().trim();
            }
            return null;
        });

        // Afficher la boîte de dialogue et traiter le résultat
        dialog.showAndWait().ifPresent(email -> {
            // Vérifier si l'email existe dans la base de données
            if (verifierEmail(email)) {
                // Envoyer un email avec le lien de réinitialisation
                boolean emailEnvoye = EmailService.envoyerEmailResetLink(email);

                if (emailEnvoye) {
                    showAlert(Alert.AlertType.INFORMATION, "Email envoyé",
                            "Un email contenant un lien de réinitialisation a été envoyé à " + email +
                                    ".\nVeuillez consulter votre boîte de réception pour réinitialiser votre mot de passe.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur d'envoi",
                            "Une erreur est survenue lors de l'envoi de l'email. Veuillez réessayer ultérieurement.");
                }
            } else {
                // Email non trouvé
                showAlert(Alert.AlertType.ERROR, "Email inconnu",
                        "Cet email n'est pas associé à un compte.");
            }
        });
    }

    // Vérifie si l'email existe dans la base de données
    private boolean verifierEmail(String email) {
        return UtilisateurService.emailExists(email);
    }

    // Génère un code temporaire aléatoire
    private String genererCodeTemporaire() {
        // Générer un code à 6 chiffres
        return String.format("%06d", new java.util.Random().nextInt(999999));
    }

    // Stocke le code temporaire pour l'email donné
    private void enregistrerCodeResetPourEmail(String email, String code) {
        // Dans un vrai système, on stockerait cela en base de données
        // Pour cette démo, on va utiliser une méthode statique dans UtilisateurService
        UtilisateurService.storeResetCode(email, code);
    }

    // Affiche une boîte de dialogue pour saisir le code et le nouveau mot de passe
    private void afficherCodeDeResetEtNouveauMdp(String email, String code) {
        // Créer une nouvelle boîte de dialogue
        Dialog<Pair<String, String>> resetDialog = new Dialog<>();
        resetDialog.setTitle("Réinitialisation du mot de passe");
        resetDialog.setHeaderText("Un code de vérification a été généré");

        // Styliser le DialogPane
        DialogPane dialogPane = resetDialog.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles/dialogs.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Feuille de style non trouvée: " + e.getMessage());
        }
        dialogPane.getStyleClass().add("reset-dialog");
        dialogPane.setPrefWidth(450);

        // Ajouter une icône
        try {
            dialogPane.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/key_icon.png"), 50, 50, true, true)));
        } catch (Exception e) {
            System.err.println("Image d'icône non trouvée: " + e.getMessage());
        }

        // Définir les boutons
        ButtonType confirmButtonType = new ButtonType("Réinitialiser", ButtonBar.ButtonData.OK_DONE);
        resetDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Créer les zones de saisie avec meilleur design
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20, 20, 10, 20));

        // Message d'information avec style
        Label infoLabel = new Label("Un code de réinitialisation a été envoyé à l'adresse " + email + ". " +
                "Veuillez vérifier votre boîte de réception (et éventuellement vos spams).");
        infoLabel.getStyleClass().add("info-text");
        infoLabel.setWrapText(true);

        // Affichage du code (pour démo)
        HBox codeBox = new HBox(10);
        Label codeLabel = new Label("Code de vérification:");
        codeLabel.getStyleClass().add("field-label");
        Label demoCodeLabel = new Label(code);
        demoCodeLabel.getStyleClass().add("demo-code");
        codeBox.getChildren().addAll(codeLabel, demoCodeLabel);

        // Champ de saisie du code
        VBox codeInputBox = new VBox(5);
        Label enterCodeLabel = new Label("Entrez le code reçu:");
        enterCodeLabel.getStyleClass().add("field-label");
        TextField codeInput = new TextField();
        codeInput.setPromptText("Code à 6 chiffres");
        codeInput.getStyleClass().add("modern-field");
        codeInputBox.getChildren().addAll(enterCodeLabel, codeInput);

        // Champ du nouveau mot de passe
        VBox passwordBox = new VBox(5);
        Label passwordLabel = new Label("Nouveau mot de passe:");
        passwordLabel.getStyleClass().add("field-label");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("8 caractères minimum");
        newPasswordField.getStyleClass().add("modern-field");
        passwordBox.getChildren().addAll(passwordLabel, newPasswordField);

        // Champ de confirmation du mot de passe
        VBox confirmPasswordBox = new VBox(5);
        Label confirmLabel = new Label("Confirmer le mot de passe:");
        confirmLabel.getStyleClass().add("field-label");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.getStyleClass().add("modern-field");
        confirmPasswordBox.getChildren().addAll(confirmLabel, confirmPasswordField);

        // Indicateur de force du mot de passe
        ProgressBar passwordStrength = new ProgressBar(0);
        passwordStrength.setPrefWidth(400);
        Label strengthLabel = new Label("Force du mot de passe: Faible");
        strengthLabel.getStyleClass().add("strength-label");

        // Mettre à jour l'indicateur de force
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            double strength = calculatePasswordStrength(newValue);
            passwordStrength.setProgress(strength);

            if (strength < 0.3) {
                strengthLabel.setText("Force du mot de passe: Faible");
                passwordStrength.getStyleClass().removeAll("medium-password", "strong-password");
                passwordStrength.getStyleClass().add("weak-password");
            } else if (strength < 0.7) {
                strengthLabel.setText("Force du mot de passe: Moyen");
                passwordStrength.getStyleClass().removeAll("weak-password", "strong-password");
                passwordStrength.getStyleClass().add("medium-password");
            } else {
                strengthLabel.setText("Force du mot de passe: Fort");
                passwordStrength.getStyleClass().removeAll("weak-password", "medium-password");
                passwordStrength.getStyleClass().add("strong-password");
            }
        });

        contentBox.getChildren().addAll(infoLabel, codeBox, codeInputBox,
                passwordBox, confirmPasswordBox,
                passwordStrength, strengthLabel);

        // Validation des entrées
        Node confirmButton = resetDialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setDisable(true);

        try {
            confirmButton.getStyleClass().add("confirm-button");
        } catch (Exception e) {
            System.err.println("Erreur d'application de style au bouton: " + e.getMessage());
        }

        // Activer le bouton uniquement si toutes les validations passent
        Runnable validateInputs = () -> {
            String inputCode = codeInput.getText().trim();
            String newPass = newPasswordField.getText().trim();
            String confirmPass = confirmPasswordField.getText().trim();

            boolean isValid = !inputCode.isEmpty() && !newPass.isEmpty() &&
                    newPass.equals(confirmPass) && newPass.length() >= 8;

            confirmButton.setDisable(!isValid);

            // Afficher message d'erreur pour les mots de passe qui ne correspondent pas
            if (!newPass.isEmpty() && !confirmPass.isEmpty() && !newPass.equals(confirmPass)) {
                confirmPasswordField.setStyle("-fx-border-color: red;");
            } else {
                confirmPasswordField.setStyle("");
            }
        };

        codeInput.textProperty().addListener((observable, oldValue, newValue) -> validateInputs.run());
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> validateInputs.run());
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> validateInputs.run());

        resetDialog.getDialogPane().setContent(contentBox);

        // Définir le résultat
        resetDialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return new Pair<>(codeInput.getText().trim(), newPasswordField.getText().trim());
            }
            return null;
        });

        // Traiter le résultat
        resetDialog.showAndWait().ifPresent(result -> {
            String inputCode = result.getKey();
            String newPassword = result.getValue();

            // Vérifier le code
            if (UtilisateurService.verifyResetCode(email, inputCode)) {
                // Mettre à jour le mot de passe
                boolean success = UtilisateurService.updatePassword(email, newPassword);

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Mot de passe réinitialisé",
                            "Votre mot de passe a été mis à jour avec succès.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Une erreur est survenue lors de la mise à jour du mot de passe.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Code incorrect",
                        "Le code de vérification saisi est incorrect.");
            }
        });
    }

    // Méthode pour calculer la force du mot de passe

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

    // Alerte réutilisable
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}