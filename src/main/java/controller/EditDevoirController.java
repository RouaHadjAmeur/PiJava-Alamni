package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.DevoirDAO;
import model.Devoir;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class EditDevoirController implements Initializable {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker datePicker;
    @FXML private TextField supportField;
    @FXML private Button browseBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private DevoirDAO devoirDAO;
    private Devoir devoir;
    private int coursId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        devoirDAO = new DevoirDAO();

        // Configuration du bouton de parcours de fichiers
        browseBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner un fichier de support");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                    new FileChooser.ExtensionFilter("Documents", "*.docx", "*.doc", "*.pptx", "*.ppt", "*.xlsx", "*.xls")
            );

            File selectedFile = fileChooser.showOpenDialog(browseBtn.getScene().getWindow());
            if (selectedFile != null) {
                supportField.setText(selectedFile.getAbsolutePath());
            }
        });

        // Configuration du bouton d'enregistrement
        saveBtn.setOnAction(event -> saveDevoir());

        // Configuration du bouton d'annulation
        cancelBtn.setOnAction(event -> {
            Stage stage = (Stage) cancelBtn.getScene().getWindow();
            stage.close();
        });
    }

    public void setDevoir(Devoir devoir) {
        this.devoir = devoir;

        // Remplir les champs avec les données du devoir
        titreField.setText(devoir.getTitre_d());
        descriptionArea.setText(devoir.getDescr_d());

        if (devoir.getDate_d() != null) {
            datePicker.setValue(devoir.getDate_d().toLocalDate());
        } else {
            datePicker.setValue(LocalDate.now());
        }

        supportField.setText(devoir.getSupport_d());
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    private void saveDevoir() {
        // Validation des champs
        if (titreField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Titre manquant", "Veuillez saisir un titre pour le devoir.");
            return;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Date manquante", "Veuillez sélectionner une date pour le devoir.");
            return;
        }

        try {
            // Mise à jour des données du devoir
            devoir.setTitre_d(titreField.getText());
            devoir.setDescr_d(descriptionArea.getText());

            // Combiner la date sélectionnée avec l'heure actuelle
            LocalDateTime dateTime = LocalDateTime.of(datePicker.getValue(),
                    devoir.getDate_d() != null ? devoir.getDate_d().toLocalTime() : LocalTime.now());
            devoir.setDate_d(dateTime);

            devoir.setSupport_d(supportField.getText());

            // Mise à jour dans la base de données
            boolean success = devoirDAO.modifier(devoir);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Devoir modifié",
                        "Le devoir a été modifié avec succès.");

                // Fermer la fenêtre
                Stage stage = (Stage) saveBtn.getScene().getWindow();
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la modification",
                        "La modification du devoir a échoué. Veuillez réessayer.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Exception lors de la modification",
                    "Détails: " + e.getMessage());
        }
    }

    // Méthode utilitaire pour afficher des alertes
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}