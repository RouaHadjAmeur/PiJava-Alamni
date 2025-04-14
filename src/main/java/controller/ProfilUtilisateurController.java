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

            // Chargement de la photo
            if (utilisateur.getPhoto() != null) {
                File photoFile = new File(utilisateur.getPhoto());
                if (photoFile.exists()) {
                    Image image = new Image(photoFile.toURI().toString());
                    photoView.setImage(image);
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
            Utilisateur utilisateurMisAJour = UtilisateurService.login(utilisateur.getEmail(), utilisateur.getPassword());
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