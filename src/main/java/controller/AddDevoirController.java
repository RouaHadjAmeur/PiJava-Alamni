package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import dao.DevoirDAO;
import model.Devoir;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddDevoirController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AddDevoirController.class.getName());

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker datePicker;
    @FXML private TextField supportField;
    @FXML private Button browseBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private DevoirDAO devoirDAO;
    private int coursId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        devoirDAO = new DevoirDAO();

        // Configuration du DatePicker avec la date actuelle
        datePicker.setValue(LocalDate.now());

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

    public void setCoursId(int coursId) {
        this.coursId = coursId;
        LOGGER.info("CoursId défini sur: " + coursId);
    }

    private void saveDevoir() {
        // Validation des champs
        if (titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Titre manquant", "Veuillez saisir un titre pour le devoir.");
            return;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Date manquante", "Veuillez sélectionner une date pour le devoir.");
            return;
        }

        // Vérifier que coursId est valide
        if (coursId <= 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Cours invalide",
                    "Impossible d'associer le devoir à un cours. ID de cours invalide.");
            return;
        }

        try {
            // Création du nouveau devoir
            Devoir devoir = new Devoir();
            devoir.setTitre_d(titreField.getText().trim());

            // Gérer la description (potentiellement vide)
            String description = descriptionArea.getText();
            devoir.setDescr_d(description != null ? description.trim() : "");

            // Combiner la date et l'heure actuelle
            LocalDateTime dateTime = LocalDateTime.of(datePicker.getValue(), LocalTime.now());
            devoir.setDate_d(dateTime);

            // Gérer le chemin du support (potentiellement vide)
            String supportPath = supportField.getText();
            devoir.setSupport_d(supportPath != null ? supportPath.trim() : "");

            devoir.setId_cours(coursId);

            // Log pour débogage
            LOGGER.info("Tentative d'ajout d'un devoir: " + devoir);

            // Enregistrement dans la base de données
            boolean success = devoirDAO.ajouter(devoir);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Devoir ajouté",
                        "Le devoir a été ajouté avec succès.");

                // Fermer la fenêtre
                Stage stage = (Stage) saveBtn.getScene().getWindow();
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'ajout",
                        "L'ajout du devoir a échoué. Veuillez consulter les logs pour plus de détails.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception lors de l'ajout du devoir: " + e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Exception lors de l'ajout",
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