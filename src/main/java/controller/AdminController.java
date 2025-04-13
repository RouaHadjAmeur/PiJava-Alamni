package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import model.Utilisateur;
import util.Session;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;

public class AdminController {

    @FXML private ImageView photoView;
    @FXML private Label nomUtilisateur;
    @FXML private Label roleUtilisateur;
    @FXML private AnchorPane mainContentPane;

    @FXML
    public void initialize() {
        // Initialiser les informations de l'utilisateur connecté
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser != null) {
            nomUtilisateur.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            roleUtilisateur.setText(currentUser.getRole());

            if (currentUser.getPhoto() != null) {
                File photoFile = new File(currentUser.getPhoto());
                if (photoFile.exists()) {
                    Image image = new Image(photoFile.toURI().toString());
                    photoView.setImage(image);
                }
            }
        }

        // Charger la vue utilisateur par défaut
        handleUser(null);
    }

    @FXML
    private void handleDashboard() {
        loadViewInMainContent("/view/dashboard.fxml");
    }

    @FXML
    private void handleManagement() {
        loadViewInMainContent("/view/management.fxml");
    }

    @FXML
    private void handleUser(ActionEvent event) { // Ajout du paramètre ActionEvent
        loadViewInMainContent("/view/utilisateur.fxml");
    }

    @FXML
    private void handleDemandes() {
        loadViewInMainContent("/view/demandes.fxml");
    }

    @FXML
    private void handleClasses() {
        loadViewInMainContent("/view/classes.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Pas besoin d'appeler Session.logout() si on change simplement de vue
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            mainContentPane.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Charge une vue dans le conteneur principal
     * @param fxmlPath chemin vers le fichier FXML à charger
     */
    private void loadViewInMainContent(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            mainContentPane.getChildren().clear();
            mainContentPane.getChildren().add(view);

            // Étendre la vue pour qu'elle remplisse tout le conteneur
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}