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
import util.Session;

public class ModifierUtilisateurController {

    @FXML private TextField nomField, prenomField, emailField;
    @FXML private ComboBox<String> roleComboBox, niveauComboBox;
    @FXML private TextField nomNiveauField;
    @FXML private Button choosePhotoButton;
    @FXML private Label photoLabel, statusLabel;
    @FXML private ImageView photoImageView;


    private String photoPath = null;
    private Utilisateur utilisateur;
    private Runnable onCloseCallback;

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;

        if (utilisateur != null) {
            nomField.setText(utilisateur.getNom());
            prenomField.setText(utilisateur.getPrenom());
            emailField.setText(utilisateur.getEmail());
            roleComboBox.setValue(utilisateur.getRole());

            if (utilisateur instanceof Eleve eleve) {
                niveauComboBox.setVisible(true);
                nomNiveauField.setVisible(true);
                niveauComboBox.setValue(eleve.getNiveau());
                nomNiveauField.setText(eleve.getNomNiveau());
            } else {
                niveauComboBox.setVisible(false);
                nomNiveauField.setVisible(false);
            }

            photoPath = utilisateur.getPhoto();
            if (photoPath != null) {
                File photoFile = new File(photoPath);
                photoLabel.setText(photoFile.getName());

                // Afficher l'aperçu de l'image
                try {
                    Image image = new Image(photoFile.toURI().toString());
                    photoImageView.setImage(image);
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
                }
            }
        }
    }
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("ÉLÈVE", "ENSEIGNANT", "PARENT", "ADMINISTRATEUR");
        niveauComboBox.getItems().addAll("Collège", "Lycée", "Université", "Primaire");

        niveauComboBox.setVisible(false);
        nomNiveauField.setVisible(false);

        roleComboBox.setOnAction(e -> {
            boolean eleve = "ÉLÈVE".equalsIgnoreCase(roleComboBox.getValue());
            niveauComboBox.setVisible(eleve);
            nomNiveauField.setVisible(eleve);
        });
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
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
        if (utilisateur == null) return;

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String role = roleComboBox.getValue();
        String niveau = niveauComboBox.getValue();
        String nomNiveau = nomNiveauField.getText().trim();

        // 1️⃣ Nom / prénom → lettres uniquement
        if (!nom.matches("^[A-Za-zÀ-ÿ\\s-]{2,}$") || !prenom.matches("^[A-Za-zÀ-ÿ\\s-]{2,}$")) {
            statusLabel.setText("❗ Nom et prénom doivent contenir uniquement des lettres (au moins 2).");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 2️⃣ Email → format simple
        if (!email.matches("^[\\w.-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            statusLabel.setText("❗ Format de l'email invalide.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 3️⃣ Rôle non vide
        if (role == null || role.isEmpty()) {
            statusLabel.setText("❗ Veuillez sélectionner un rôle.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 4️⃣ Si élève → niveau requis
        if ("ÉLÈVE".equalsIgnoreCase(role)) {
            if (niveau == null || niveau.isEmpty() || nomNiveau.isEmpty()) {
                statusLabel.setText("❗ Veuillez renseigner le niveau et nom du niveau pour un élève.");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        }

        // ✅ Mise à jour des champs
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        utilisateur.setPhoto(photoPath);

        if (utilisateur instanceof Eleve eleve) {
            eleve.setNiveau(niveau);
            eleve.setNomNiveau(nomNiveau);
        }

        // 🔁 Mise à jour en base
        boolean success = UtilisateurService.updateUtilisateur(utilisateur);

        if (success) {
            Utilisateur utilisateurMisAJour = UtilisateurService.login(utilisateur.getEmail(), utilisateur.getPassword());
            Session.setUtilisateurConnecte(utilisateurMisAJour);

            statusLabel.setText("✅ Profil modifié !");
            statusLabel.setStyle("-fx-text-fill: green;");

            // ➤ fermer la fenêtre après une courte pause
            new Thread(() -> {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() ->
                        ((Stage) nomField.getScene().getWindow()).close()
                );
            }).start();
        } else {
            statusLabel.setText("❌ Erreur lors de la modification !");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
}