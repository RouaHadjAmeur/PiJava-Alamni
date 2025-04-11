package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Reclamation;
import services.ReclamationServices;

import java.sql.Date;
import java.util.regex.Pattern;

public class AddReclamationController {

    @FXML private TextField emailField;
    @FXML private TextField objetField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField adminMailField;
    @FXML private TextField roleField;

    @FXML private Label emailErrorLabel;
    @FXML private Label adminEmailErrorLabel;

    private final ReclamationServices service = new ReclamationServices();

    @FXML
    private void handleSave() {
        clearErrors();

        boolean valid = true;

        if (emailField.getText().isEmpty()) {
            emailErrorLabel.setText("L'email est requis.");
            valid = false;
        } else if (!isValidEmail(emailField.getText())) {
            emailErrorLabel.setText("Format invalide. Ex: exemple@gmail.com");
            valid = false;
        }

        if (adminMailField.getText().isEmpty()) {
            adminEmailErrorLabel.setText("Email admin requis.");
            valid = false;
        } else if (!isValidEmail(adminMailField.getText())) {
            adminEmailErrorLabel.setText("Format invalide. Ex: admin@gmail.com");
            valid = false;
        }

        if (objetField.getText().isEmpty() ||
                descriptionArea.getText().isEmpty() ||
                roleField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs !");
            valid = false;
        }

        if (!valid) return;

        try {
            Reclamation r = new Reclamation(
                    emailField.getText(),
                    objetField.getText(),
                    descriptionArea.getText(),
                    "En attente",
                    new Date(System.currentTimeMillis()),
                    adminMailField.getText(),
                    roleField.getText(),
                    0 // userId auto ou non utilisé
            );

            service.add(r);

            showAlert(Alert.AlertType.INFORMATION, "Réclamation ajoutée avec succès !");
            ((Stage) emailField.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.matches(regex, email);
    }

    private void clearErrors() {
        emailErrorLabel.setText("");
        adminEmailErrorLabel.setText("");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
