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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import util.Session;

public class ModifierUtilisateurController {

    @FXML private TextField nomField, prenomField, emailField;
    @FXML private ComboBox<String> roleComboBox, niveauComboBox;
    @FXML private TextField nomNiveauField;
    @FXML private Button choosePhotoButton;
    @FXML private Label photoLabel, statusLabel;
    @FXML private ImageView photoImageView;


    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;


    private String photoPath = null;
    private Utilisateur utilisateur;
    private Runnable onCloseCallback;
    private boolean photoChanged = false;

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

            // Dans setUtilisateur()
            photoPath = utilisateur.getPhoto();
            if (photoPath != null) {
                photoLabel.setText(new File(photoPath).getName());

                try {
                    // Méthode 1 : Essayer de charger via le classpath des ressources
                    Image image = new Image(getClass().getResourceAsStream("/" + photoPath));
                    photoImageView.setImage(image);
                    System.out.println("Image chargée via classpath: " + photoPath);
                } catch (Exception e) {
                    try {
                        // Méthode 2 : Essayer de charger via le chemin absolu
                        File resourceDir = new File("src/main/resources");
                        File imageFile = new File(resourceDir, photoPath);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            photoImageView.setImage(image);
                            System.out.println("Image chargée via chemin absolu: " + imageFile.getAbsolutePath());
                        } else {
                            System.err.println("Fichier d'image introuvable: " + imageFile.getAbsolutePath());
                        }
                    } catch (Exception ex) {
                        System.err.println("Erreur lors du chargement de l'image: " + ex.getMessage());
                    }
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
        fileChooser.setTitle("Sélectionner une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(photoImageView.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Créer le répertoire de destination s'il n'existe pas
                File destinationDir = new File("src/main/resources/images/users/");
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs();
                }

                // Générer un nom unique pour la photo
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                File targetFile = new File(destinationDir, fileName);

                // Copier le fichier
                Files.copy(selectedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // IMPORTANT: Stocker un chemin relatif (et non absolu)
                photoPath = "images/users/" + fileName;

                // Charger l'image pour l'aperçu
                Image image = new Image(targetFile.toURI().toString());
                photoImageView.setImage(image);

                // Signaler que la photo a été modifiée
                photoChanged = true;

                photoLabel.setText(fileName);
                System.out.println("Photo mise à jour avec succès. Nouveau chemin : " + photoPath);

            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("❌ Erreur lors du téléchargement de la photo !");
                statusLabel.setStyle("-fx-text-fill: red;");
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

        // Récupération des valeurs des champs
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String role = roleComboBox.getValue();
        String niveau = niveauComboBox.getValue();
        String nomNiveau = nomNiveauField.getText().trim();

        // Récupération des valeurs des champs de mot de passe
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

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

        // 5️⃣ Validation du mot de passe si l'utilisateur souhaite le modifier
        boolean changePassword = !newPassword.isEmpty() || !confirmPassword.isEmpty();

        if (changePassword) {
            // Vérifier que le nouveau mot de passe est assez fort
            if (newPassword.length() < 8) {
                statusLabel.setText("❗ Le nouveau mot de passe doit contenir au moins 8 caractères.");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Vérifier que les deux mots de passe correspondent
            if (!newPassword.equals(confirmPassword)) {
                statusLabel.setText("❗ Les nouveaux mots de passe ne correspondent pas.");
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

        // Mise à jour du mot de passe si nécessaire
        if (changePassword) {
            boolean passwordUpdated = UtilisateurService.updatePassword(utilisateur.getId(), newPassword);
            if (!passwordUpdated) {
                statusLabel.setText("❌ Erreur lors de la modification du mot de passe !");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        }

        // 🔁 Mise à jour en base
        boolean success = UtilisateurService.updateUtilisateur(utilisateur);

        if (success) {
            // Au lieu de faire un login qui peut échouer
            if (changePassword) {
                // Si le mot de passe a changé, récupérer l'utilisateur mis à jour par login
                Utilisateur utilisateurMisAJour = UtilisateurService.login(utilisateur.getEmail(), newPassword);
                if (utilisateurMisAJour != null) {
                    Session.setUtilisateurConnecte(utilisateurMisAJour);
                } else {
                    // Fallback: utiliser getUserById pour éviter de perdre la session
                    Utilisateur utilisateurFallback = UtilisateurService.getUserById(utilisateur.getId());
                    Session.setUtilisateurConnecte(utilisateurFallback);
                }
            } else {
                // Si le mot de passe n'a pas changé, récupérer directement par ID
                Utilisateur utilisateurMisAJour = UtilisateurService.getUserById(utilisateur.getId());
                Session.setUtilisateurConnecte(utilisateurMisAJour);
            }

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