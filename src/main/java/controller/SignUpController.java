package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.*;
import javafx.event.ActionEvent;
import service.UtilisateurService;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class SignUpController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> niveauComboBox;
    @FXML private TextField nomNiveauField;
    @FXML private CheckBox cguCheckbox;
    @FXML private Label statusLabel;
    @FXML private Button choosePhotoButton;
    @FXML private Label photoLabel;
    @FXML private ImageView photoPreview;
    @FXML private VBox academicSection;

    private String photoPath;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
    );

    @FXML
    public void initialize() {
        // Initialiser les ComboBox
        roleComboBox.getItems().addAll("ÉLÈVE", "ENSEIGNANT", "PARENT", "ADMINISTRATEUR");
        niveauComboBox.getItems().addAll("Collège", "Lycée", "Université", "Primaire");

        // Définir une image par défaut pour la prévisualisation
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_avatar.png"));
            if (defaultImage != null) {
                photoPreview.setImage(defaultImage);
            }
        } catch (Exception e) {
            System.err.println("Impossible de charger l'image par défaut: " + e.getMessage());
        }

        // Cacher la section académique initialement
        academicSection.setVisible(false);
        academicSection.setManaged(false);

        // Afficher les champs niveau et nomNiveau uniquement pour le rôle ÉLÈVE
        roleComboBox.setOnAction(event -> {
            boolean estEleve = "ÉLÈVE".equalsIgnoreCase(roleComboBox.getValue());
            academicSection.setVisible(estEleve);
            academicSection.setManaged(estEleve);
        });
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png")
        );
        File file = fileChooser.showOpenDialog(nomField.getScene().getWindow());

        if (file != null) {
            photoPath = file.getAbsolutePath();
            photoLabel.setText(file.getName());

            // Afficher l'aperçu de l'image
            try {
                Image image = new Image(file.toURI().toString());
                photoPreview.setImage(image);
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            }
        }
    }

    // Nouveau pattern pour valider les noms et prénoms (lettres, espaces, traits d'union, apostrophes)
    private static final Pattern TEXT_ONLY_PATTERN = Pattern.compile("^[\\p{L} \\-']+$");


    // Modification dans SignUpController.java - pour l'inscription avec mot de passe haché
    @FXML
    private void handleSignUp(ActionEvent event) {
        // Récupérer les données du formulaire
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        // Vérification des champs obligatoires
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.WARNING, "Champs incomplets", "Veuillez remplir tous les champs obligatoires.");
            return;
        }

        // Vérification que les mots de passe correspondent
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Mots de passe différents", "Les mots de passe ne correspondent pas.");
            return;
        }

        // Vérification de l'email
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showAlert(Alert.AlertType.ERROR, "Email invalide", "Veuillez entrer une adresse email valide.");
            return;
        }

        // Vérification de l'acceptation des CGU
        if (!cguCheckbox.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "CGU non acceptées", "Vous devez accepter les conditions générales d'utilisation.");
            return;
        }

        // Créer l'utilisateur selon le rôle
        Utilisateur user;

        switch (role.toUpperCase()) {
            case "ADMINISTRATEUR":
                user = new Administrateur(nom, prenom, email, password);
                break;
            case "ÉLÈVE":
                Eleve eleve = new Eleve(nom, prenom, email, password);
                // Définir le niveau et le nom du niveau pour l'élève
                if (niveauComboBox.getValue() != null) {
                    eleve.setNiveau(niveauComboBox.getValue());
                    eleve.setNomNiveau(nomNiveauField.getText());
                }
                user = eleve;
                break;
            case "ENSEIGNANT":
                user = new Enseignant(nom, prenom, email, password);
                break;
            case "PARENT":
                user = new ParentUser(nom, prenom, email, password);
                break;
            default:
                showAlert(Alert.AlertType.ERROR, "Erreur", "Rôle invalide.");
                return;
        }

        // Définir le chemin de la photo si elle a été choisie
        if (photoPath != null && !photoPath.isEmpty()) {
            user.setPhoto(photoPath);
        }

        // S'assurer que le compte est en attente de validation
        user.setPending(true);

        // Inscrire l'utilisateur
        if (UtilisateurService.inscrire(user)) {
            showAlert(Alert.AlertType.INFORMATION, "Inscription réussie",
                    "Votre compte a été créé. Veuillez attendre la validation par un administrateur.");

            // Retourner à l'écran de connexion
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
                Stage stage = (Stage) nomField.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur d'inscription",
                    "Un problème est survenu lors de l'inscription. L'email est peut-être déjà utilisé.");
        }
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleGoToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/Login.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Erreur lors du chargement de la page de connexion.");
        }
    }
}