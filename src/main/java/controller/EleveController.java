package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Utilisateur;
import util.Session;

import java.io.File;
import java.io.IOException;

public class EleveController {

    @FXML private MenuButton userMenu;
    @FXML private ImageView profileImage;

    @FXML
    public void initialize() {
        Utilisateur user = Session.getUtilisateurConnecte();

        if (user != null) {
            userMenu.setText(user.getPrenom() + " " + user.getNom());

            if (user.getPhoto() != null) {
                File photo = new File(user.getPhoto());
                if (photo.exists()) {
                    Image img = new Image(photo.toURI().toString(), 40, 40, true, true);
                    profileImage.setImage(img);
                }
            }
        }

        // ✅ Déplace le code après chargement de la scène
        javafx.application.Platform.runLater(() -> {
            if (userMenu.getScene() != null) {
                userMenu.getScene().getRoot().setUserData(this);
            }
        });
    }

    @FXML
    private void handleMonProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/profilUtilisateur.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.centerOnScreen(); // ✅ centrer
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMesNotes() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mes Résultats");
        alert.setHeaderText(null);
        alert.setContentText("Page des résultats de l'élève ici...");
        alert.showAndWait();
    }

    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
            Parent root = loader.load();


            Stage stage = new Stage();
            stage.setTitle("Ajouter Réclamation");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleMesReclamations() {

    }

    @FXML
    private void handleConversations() {
        try {
            // Load the conversation dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/conversation_dashboard.fxml"));
            
            // Set controller factory to handle package differences
            loader.setControllerFactory(c -> {
                try {
                    // Try to create controller from controllers package
                    return Class.forName(c.getName()).getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
            
            Parent root = loader.load();
            
            Stage currentStage = (Stage) userMenu.getScene().getWindow();
            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Conversations - Alamni");
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Impossible de charger les conversations: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) userMenu.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Alamni");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshUtilisateurInfo() {
        Utilisateur user = Session.getUtilisateurConnecte();

        if (user != null) {
            userMenu.setText(user.getPrenom() + " " + user.getNom());

            if (user.getPhoto() != null) {
                File photo = new File(user.getPhoto());
                if (photo.exists()) {
                    Image image = new Image(photo.toURI().toString(), 40, 40, true, true);
                    profileImage.setImage(image);
                }
            }
        }
    }
}