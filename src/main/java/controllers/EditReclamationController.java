package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Reclamation;
import services.ReclamationServices;

import javax.swing.*;
import java.sql.Date;

public class EditReclamationController {

    @FXML private TextField emailField;
    @FXML private TextField objetField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> statusBox;
    @FXML private TextField adminMailField;
    @FXML private TextField roleField;
    @FXML private TextField userIdField;

    private final ReclamationServices service = new ReclamationServices();
    private Reclamation reclamation;

    public void setReclamation(Reclamation r) {
        this.reclamation = r;
        // Remplir les champs
        emailField.setText(r.getUser_email());
        objetField.setText(r.getObjet());
        descriptionArea.setText(r.getDescription());
        statusBox.setValue(r.getStatus());
        adminMailField.setText(r.getAdmin_mail());
        roleField.setText(r.getRole());
        userIdField.setText(String.valueOf(r.getUser_id()));
    }

    @FXML
    public void initialize() {
        statusBox.getItems().addAll("En attente", "En cours", "Résolue");
    }

    @FXML
    private void handleUpdate() {
        try {
            reclamation.setUser_email(emailField.getText());
            reclamation.setObjet(objetField.getText());
            reclamation.setDescription(descriptionArea.getText());
            reclamation.setStatus(statusBox.getValue());
            reclamation.setAdmin_mail(adminMailField.getText());
            reclamation.setRole(roleField.getText());
            reclamation.setUser_id(Integer.parseInt(userIdField.getText()));
            reclamation.setDate_soumission(new Date(System.currentTimeMillis()));

            service.modify(reclamation);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Réclamation mise à jour !");
            alert.showAndWait();
            loadData();
            ((Stage) emailField.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette réclamation ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.delete(reclamation.getId());

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Réclamation supprimée !");
                alert.showAndWait();

                ((Stage) emailField.getScene().getWindow()).close(); // Ferme la fenêtre après suppression
            }
        });
    }


}