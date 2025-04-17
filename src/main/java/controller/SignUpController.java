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
import service.UtilisateurService;
import util.PasswordUtils; // Nouvel import

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

    // Pattern pour valider les noms et prénoms (lettres, espaces, traits d'union, apostrophes)
    private static final Pattern TEXT_ONLY_PATTERN = Pattern.compile("^[\\p{L} \\-']+$");

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

    @FXML
    private void handleSignUp() {
        // Réinitialiser le message d'état
        statusLabel.setText("");
        statusLabel.setTextFill(Color.RED);

        // Vérifier que tous les champs obligatoires sont remplis
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || role == null) {
            statusLabel.setText("❗ Tous les champs obligatoires doivent être remplis.");
            return;
        }

        if (!TEXT_ONLY_PATTERN.matcher(nom).matches()) {
            statusLabel.setText("❗ Le nom doit contenir uniquement des lettres.");
            return;
        }

        if (!TEXT_ONLY_PATTERN.matcher(prenom).matches()) {
            statusLabel.setText("❗ Le prénom doit contenir uniquement des lettres.");
            return;
        }

        // Vérifier que le mot de passe a au moins 6 caractères
        if (password.length() < 6) {
            statusLabel.setText("❗ Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        // Vérifier que les mots de passe correspondent
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("❗ Les mots de passe ne correspondent pas.");
            return;
        }

        // Vérifier que le format d'email est valide
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            statusLabel.setText("❗ Format d'email invalide.");
            return;
        }

        // Vérifier que les CGU sont acceptées
        if (!cguCheckbox.isSelected()) {
            statusLabel.setText("❗ Vous devez accepter les conditions d'utilisation.");
            return;
        }

        // Pour les élèves, vérifier que le niveau est renseigné
        String niveau = null;
        String nomNiveau = null;

        if ("ÉLÈVE".equalsIgnoreCase(role)) {
            niveau = niveauComboBox.getValue();
            nomNiveau = nomNiveauField.getText().trim();

            if (niveau == null || nomNiveau.isEmpty()) {
                statusLabel.setText("❗ Veuillez compléter les informations académiques.");
                return;
            }
        }

        // Vérifier si l'email existe déjà
        if (UtilisateurService.emailExiste(email)) {
            statusLabel.setText("❗ Cet email est déjà utilisé.");
            return;
        }

        // Hacher le mot de passe avant de créer l'utilisateur (NOUVEAU CODE)
        String hashedPassword = PasswordUtils.hashPassword(password);

        // Créer l'utilisateur selon son rôle avec le mot de passe haché
        Utilisateur user;

        switch (role) {
            case "ÉLÈVE":
                Eleve eleve = new Eleve(nom, prenom, email, hashedPassword); // Mot de passe haché
                eleve.setNiveau(niveau);
                eleve.setNomNiveau(nomNiveau);
                user = eleve;
                break;
            case "ENSEIGNANT":
                user = new Enseignant(nom, prenom, email, hashedPassword); // Mot de passe haché
                break;
            case "PARENT":
                user = new ParentUser(nom, prenom, email, hashedPassword); // Mot de passe haché
                break;
            case "ADMINISTRATEUR":
                user = new Administrateur(nom, prenom, email, hashedPassword); // Mot de passe haché
                break;
            default:
                statusLabel.setText("❗ Rôle non reconnu.");
                return;
        }

        if (photoPath != null && !photoPath.isEmpty()) {
            user.setPhoto(photoPath);
        }

        // Marquer comme en attente (sauf pour les administrateurs qui sont déjà validés)
        if (!"ADMINISTRATEUR".equals(role)) {
            user.setPending(true);
        }

        // Enregistrer l'utilisateur
        try {
            UtilisateurService.inscrire(user);
            // Si on arrive ici sans exception, l'inscription a réussi
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("✅ Compte créé avec succès! Vous pouvez maintenant vous connecter.");

            // Redirection vers la page de connexion après un court délai
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::handleGoToLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            statusLabel.setText("❌ Erreur lors de la création du compte: " + e.getMessage());
        }
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