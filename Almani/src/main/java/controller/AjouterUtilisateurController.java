package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.*;
import service.UtilisateurService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


import java.io.File;

public class AjouterUtilisateurController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> niveauComboBox;
    @FXML private TextField nomNiveauField;
    @FXML private Label photoLabel;
    @FXML private Label statusLabel;
    @FXML private Button choosePhotoButton;

    private String photoPath;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("ÉLÈVE", "ENSEIGNANT", "PARENT", "ADMINISTRATEUR");
        niveauComboBox.getItems().addAll("Collège", "Lycée", "Université", "Primaire");

        niveauComboBox.setVisible(false);
        nomNiveauField.setVisible(false);

        roleComboBox.setOnAction(event -> {
            boolean estEleve = "ÉLÈVE".equalsIgnoreCase(roleComboBox.getValue());
            niveauComboBox.setVisible(estEleve);
            nomNiveauField.setVisible(estEleve);
        });
    }
    @FXML private ImageView photoImageView;
    @FXML
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(nomField.getScene().getWindow());

        if (file != null) {
            photoPath = file.getAbsolutePath();
            photoLabel.setText(file.getName());

            // Afficher l'aperçu de l'image
            try {
                Image image = new Image(file.toURI().toString());
                photoImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            }
        }
    }
    @FXML
    private void handleCancel() {
        ((Stage) nomField.getScene().getWindow()).close();
    }
    @FXML
    private void handleSave() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        String niveau = niveauComboBox.isVisible() ? niveauComboBox.getValue() : "";
        String nomNiveau = nomNiveauField.isVisible() ? nomNiveauField.getText() : "";

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            statusLabel.setText("Tous les champs obligatoires doivent être remplis.");
            return;
        }

        if (UtilisateurService.emailExiste(email)) {
            statusLabel.setText("Cet email est déjà utilisé.");
            return;
        }

        Utilisateur user = switch (role) {
            case "ÉLÈVE" -> {
                Eleve e = new Eleve(nom, prenom, email, password);
                e.setNiveau(niveau);
                e.setNomNiveau(nomNiveau);
                yield e;
            }
            case "ENSEIGNANT" -> new Enseignant(nom, prenom, email, password);
            case "PARENT" -> new ParentUser(nom, prenom, email, password);
            case "ADMINISTRATEUR" -> new Administrateur(nom, prenom, email, password);
            default -> throw new IllegalArgumentException("Rôle invalide");
        };

        user.setPhoto(photoPath);
        user.setPending(false); // validé directement car admin

        UtilisateurService.inscrire(user);

        ((Stage) nomField.getScene().getWindow()).close();
    }
}