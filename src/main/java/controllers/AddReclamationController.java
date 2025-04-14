package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Reclamation;
import model.Utilisateur;
import services.ReclamationServices;
import util.Session;


import java.io.IOException;
import java.sql.Date;
import java.util.regex.Pattern;

public class AddReclamationController {

    @FXML private TextField emailField;
    @FXML private TextField objetField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField adminMailField;
    @FXML private TextField roleField;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;

    @FXML private Label emailErrorLabel;
    @FXML private Label objetErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label roleErrorLabel;
    @FXML private Label adminEmailErrorLabel;

    private final ReclamationServices service = new ReclamationServices();

    @FXML
    private void initialize() {
        Utilisateur user = Session.getUtilisateurConnecte();

        if (user != null) {
            emailLabel.setText(user.getEmail());
            roleLabel.setText(user.getRole());
        }
    }

    @FXML
    private void handleSave() {
        clearErrors();

        boolean valid = true;

        // Email admin
        if (adminMailField.getText().isEmpty()) {
            adminEmailErrorLabel.setText("Email admin requis.");
            valid = false;
        } else if (!isValidEmail(adminMailField.getText())) {
            adminEmailErrorLabel.setText("Format invalide. Ex: admin@gmail.com");
            valid = false;
        }

        // Objet
        if (objetField.getText().isEmpty()) {
            objetErrorLabel.setText("Objet requis.");
            valid = false;
        } else if (objetField.getText().length() < 4) {
            objetErrorLabel.setText("Au moins 4 lettres.");
            valid = false;
        }

        // Description
        if (descriptionArea.getText().isEmpty()) {
            descriptionErrorLabel.setText("Description requise.");
            valid = false;
        } else if (descriptionArea.getText().length() < 10) {
            descriptionErrorLabel.setText("Au moins 10 lettres.");
            valid = false;
        }



        if (!valid) return;

        try {
            Reclamation r = new Reclamation(
                    emailLabel.getText(),
                    objetField.getText(),
                    descriptionArea.getText(),
                    "En attente",
                    new Date(System.currentTimeMillis()),
                    adminMailField.getText(),
                    roleLabel.getText(),
                    0
            );

            service.add(r);

            showAlert(Alert.AlertType.INFORMATION, "Réclamation ajoutée avec succès !");
            ((Stage) emailLabel.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
        }
    }


    private boolean isValidEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.matches(regex, email);
    }

    private void clearErrors() {
       // emailErrorLabel.setText("");
        adminEmailErrorLabel.setText("");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            controllers.AddReclamationController controller = loader.getController();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleMesReclamations() {

    }

}
