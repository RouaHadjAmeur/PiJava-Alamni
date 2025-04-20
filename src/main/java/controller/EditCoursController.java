package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.CoursDAO;
import model.Cours;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class EditCoursController implements Initializable {

    @FXML private TextField idField;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField matiereField;
    @FXML private ComboBox<String> niveauComboBox;
    @FXML private TextField imagePathField;
    @FXML private Button browseImageBtn;
    @FXML private TextField supportPathField;
    @FXML private Button browseSupportBtn;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private CoursDAO coursDAO;
    private Cours cours;
    private File selectedImageFile;
    private File selectedSupportFile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation du DAO
        coursDAO = new CoursDAO();

        // Configuration du ComboBox pour les niveaux
        niveauComboBox.setItems(FXCollections.observableArrayList(
                 "Collège", "Lycée"
        ));

        // Configuration des boutons de parcours de fichiers
        browseImageBtn.setOnAction(event -> browseImage());
        browseSupportBtn.setOnAction(event -> browseSupport());

        // Configuration des boutons d'action
        cancelBtn.setOnAction(event -> ((Stage) cancelBtn.getScene().getWindow()).close());
        saveBtn.setOnAction(event -> updateCours());
    }

    public void setCours(Cours cours) {
        this.cours = cours;

        // Remplir les champs avec les données du cours
        idField.setText(String.valueOf(cours.getId()));
        titreField.setText(cours.getTitre());
        descriptionField.setText(cours.getDescr_c());
        matiereField.setText(cours.getMatiere_c());
        niveauComboBox.setValue(cours.getNiveau());

        if (cours.getImage() != null) {
            imagePathField.setText(cours.getImage());
        }

        if (cours.getSupport_c() != null) {
            supportPathField.setText(cours.getSupport_c());
        }
    }

    private void browseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        selectedImageFile = fileChooser.showOpenDialog(browseImageBtn.getScene().getWindow());
        if (selectedImageFile != null) {
            imagePathField.setText(selectedImageFile.getAbsolutePath());
        }
    }

    private void browseSupport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un support");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.ppt", "*.pptx"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        selectedSupportFile = fileChooser.showOpenDialog(browseSupportBtn.getScene().getWindow());
        if (selectedSupportFile != null) {
            supportPathField.setText(selectedSupportFile.getAbsolutePath());
        }
    }

    private void updateCours() {
        // Validation des champs obligatoires
        if (titreField.getText().isEmpty() || matiereField.getText().isEmpty() || niveauComboBox.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Champs obligatoires",
                    "Veuillez remplir tous les champs obligatoires (Titre, Matière, Niveau).");
            return;
        }

        try {
            // Mise à jour des champs de base
            cours.setTitre(titreField.getText());
            cours.setDescr_c(descriptionField.getText());
            cours.setMatiere_c(matiereField.getText());
            cours.setNiveau(niveauComboBox.getValue());

            // Création du dossier pour les fichiers s'il n'existe pas
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Copie de l'image si une nouvelle est sélectionnée
            if (selectedImageFile != null) {
                Path targetImage = uploadDir.resolve("img_" + System.currentTimeMillis() + "_" + selectedImageFile.getName());
                Files.copy(selectedImageFile.toPath(), targetImage, StandardCopyOption.REPLACE_EXISTING);
                cours.setImage(targetImage.toString());
            }

            // Copie du support si un nouveau est sélectionné
            if (selectedSupportFile != null) {
                Path targetSupport = uploadDir.resolve("support_" + System.currentTimeMillis() + "_" + selectedSupportFile.getName());
                Files.copy(selectedSupportFile.toPath(), targetSupport, StandardCopyOption.REPLACE_EXISTING);
                cours.setSupport_c(targetSupport.toString());
            }

            // Mise à jour dans la base de données
            boolean success = coursDAO.modifier(cours);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Cours modifié",
                        "Le cours a été modifié avec succès.");
                ((Stage) saveBtn.getScene().getWindow()).close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la modification",
                        "Une erreur est survenue lors de la modification du cours.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Exception",
                    "Une erreur est survenue: " + e.getMessage());
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