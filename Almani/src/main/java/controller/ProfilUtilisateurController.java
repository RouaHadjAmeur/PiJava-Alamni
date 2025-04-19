package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.Eleve;
import model.Utilisateur;
import util.Session;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import java.io.File;
import service.UtilisateurService;
import java.io.IOException;
import javafx.scene.shape.Rectangle;

public class ProfilUtilisateurController {

    @FXML private ImageView photoView;
    @FXML private Label nomPrenomLabel, emailLabel, roleLabel;
    @FXML private Label niveauLabel, nomNiveauLabel;
    @FXML private Label roleTagLabel; // Nouveau élément pour le badge de rôle
    @FXML private VBox academicInfoBox; // Nouveau conteneur pour les infos académiques



    private Utilisateur utilisateur;

    @FXML
    public void initialize() {
        utilisateur = Session.getUtilisateurConnecte();

        if (utilisateur != null) {
            // Mise à jour du format d'affichage pour correspondre au nouveau design
            nomPrenomLabel.setText(utilisateur.getNom() + " " + utilisateur.getPrenom());
            emailLabel.setText(utilisateur.getEmail());

            // Pour le rôle, on met juste la valeur sans le préfixe "Rôle : "
            String role = utilisateur.getRole();
            roleLabel.setText(role);

            // Configuration du badge de rôle
            roleTagLabel.setText(role);

            // Configurer la couleur du badge selon le rôle
            switch(role.toUpperCase()) {
                case "ÉLÈVE":
                    roleTagLabel.setStyle("-fx-background-color: #bbdefb; -fx-text-fill: #1565c0; -fx-padding: 3 10; -fx-background-radius: 3;");
                    break;
                case "ENSEIGNANT":
                    roleTagLabel.setStyle("-fx-background-color: #c8e6c9; -fx-text-fill: #2e7d32; -fx-padding: 3 10; -fx-background-radius: 3;");
                    break;
                case "ADMINISTRATEUR":
                    roleTagLabel.setStyle("-fx-background-color: #ffccbc; -fx-text-fill: #d84315; -fx-padding: 3 10; -fx-background-radius: 3;");
                    break;
                case "PARENT":
                    roleTagLabel.setStyle("-fx-background-color: #d1c4e9; -fx-text-fill: #4527a0; -fx-padding: 3 10; -fx-background-radius: 3;");
                    break;
                default:
                    roleTagLabel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #616161; -fx-padding: 3 10; -fx-background-radius: 3;");
            }

            // Configuration du style de la photo de profil (ajustez les dimensions si nécessaire)
            photoView.setFitHeight(120);
            photoView.setFitWidth(120);
            photoView.setPreserveRatio(true);

// Effet arrondi pour la photo (optionnel si déjà défini dans le CSS)
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                    photoView.getFitWidth(), photoView.getFitHeight());
            clip.setArcWidth(photoView.getFitWidth());
            clip.setArcHeight(photoView.getFitHeight());
            photoView.setClip(clip);

// Chargement de la photo avec méthode robuste
            if (utilisateur.getPhoto() != null && !utilisateur.getPhoto().isEmpty()) {
                String photoPath = utilisateur.getPhoto();
                boolean photoLoaded = false;

                // Méthode 1: Essayer via le classpath
                try {
                    System.out.println("Tentative de chargement via classpath: " + photoPath);
                    Image image = new Image(getClass().getResourceAsStream("/" + photoPath));
                    if (image != null && !image.isError()) {
                        photoView.setImage(image);
                        photoLoaded = true;
                        System.out.println("Photo chargée via classpath");
                    }
                } catch (Exception e) {
                    System.out.println("Échec du chargement via classpath: " + e.getMessage());
                }

                // Méthode 2: Essayer via src/main/resources
                if (!photoLoaded) {
                    try {
                        File resourceDir = new File("src/main/resources");
                        File imageFile = new File(resourceDir, photoPath);
                        System.out.println("Tentative via ressources: " + imageFile.getAbsolutePath());
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            photoView.setImage(image);
                            photoLoaded = true;
                            System.out.println("Photo chargée via ressources");
                        }
                    } catch (Exception e) {
                        System.out.println("Échec du chargement via ressources: " + e.getMessage());
                    }
                }

                // Méthode 3: Essayer via target/classes
                if (!photoLoaded) {
                    try {
                        File targetDir = new File("target/classes");
                        File imageFile = new File(targetDir, photoPath);
                        System.out.println("Tentative via target: " + imageFile.getAbsolutePath());
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            photoView.setImage(image);
                            photoLoaded = true;
                            System.out.println("Photo chargée via target");
                        }
                    } catch (Exception e) {
                        System.out.println("Échec du chargement via target: " + e.getMessage());
                    }
                }

                // Si aucune méthode n'a fonctionné, essayer la méthode originale
                if (!photoLoaded) {
                    try {
                        File f = new File(photoPath);
                        System.out.println("Tentative via chemin absolu: " + f.getAbsolutePath());
                        if (f.exists()) {
                            Image image = new Image(f.toURI().toString());
                            photoView.setImage(image);
                            photoLoaded = true;
                            System.out.println("Photo chargée via chemin absolu");
                        }
                    } catch (Exception e) {
                        System.err.println("Échec du chargement de la photo: " + e.getMessage());
                    }
                }
            }

// Ajouter une image par défaut si nécessaire
            if (photoView.getImage() == null) {
                try {
                    System.out.println("Tentative de chargement de l'image par défaut");
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_avatar.png"));
                    photoView.setImage(defaultImage);
                    System.out.println("Image par défaut chargée");
                } catch (Exception e) {
                    System.err.println("Impossible de charger l'avatar par défaut: " + e.getMessage());
                }
            }

            // Gestion des informations d'élève
            if (utilisateur instanceof Eleve eleve) {
                // Mise à jour du format d'affichage pour correspondre au nouveau design
                niveauLabel.setText(eleve.getNiveau());
                nomNiveauLabel.setText(eleve.getNomNiveau());

                // Rendre la section académique visible
                academicInfoBox.setVisible(true);
                academicInfoBox.setManaged(true);
            } else {
                // Cacher la section académique pour les non-élèves
                academicInfoBox.setVisible(false);
                academicInfoBox.setManaged(false);
            }
        }
    }

    @FXML
    private void handleModifierProfil() {
        // Le reste du code reste inchangé
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/modifierUtilisateur.fxml"));
            Parent root = loader.load();

            ModifierUtilisateurController controller = loader.getController();
            controller.setUtilisateur(utilisateur);

            Stage stage = new Stage();
            stage.setTitle("Modifier Mon Profil");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.centerOnScreen();
            stage.showAndWait();

            // 🔁 Recharge infos après modif
            Utilisateur utilisateurMisAJour = UtilisateurService.getUserById(utilisateur.getId());

            if (utilisateurMisAJour != null) {
                // Mettre à jour l'utilisateur dans la session
                Session.setUtilisateurConnecte(utilisateurMisAJour);

                // Rafraîchir l'interface
                initialize();
                System.out.println("Profil utilisateur rechargé avec succès");
            } else {
                System.err.println("Impossible de recharger les informations de l'utilisateur");
            }
            Session.setUtilisateurConnecte(utilisateurMisAJour);
            Stage mainStage = (Stage) nomPrenomLabel.getScene().getWindow();
            Scene mainScene = mainStage.getScene();
            Parent mainRoot = mainScene.getRoot();

            // Sécurité : si EleveController est le controller actif
            Object controllerObj = mainRoot.getUserData();
            if (controllerObj instanceof EleveController eleveController) {
                eleveController.refreshUtilisateurInfo(); // 💥 met à jour l'affichage du menu
            }
            initialize();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFermer() {
        Stage stage = (Stage) nomPrenomLabel.getScene().getWindow();
        stage.close();
    }
}