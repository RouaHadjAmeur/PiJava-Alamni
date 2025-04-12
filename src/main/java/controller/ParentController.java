package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import model.Utilisateur;
import util.Session;

import java.io.File;
import java.io.IOException;

public class ParentController {

    @FXML private MenuButton userMenu;
    @FXML private ImageView profileImage;

    @FXML
    public void initialize() {
        Utilisateur user = Session.getUtilisateurConnecte();

        if (user != null) {
            userMenu.setText(user.getPrenom() + " " + user.getNom());

            if (user.getPhoto() != null) {
                File file = new File(user.getPhoto());
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 40, 40, true, true);
                    profileImage.setImage(image);
                }
            }
        }
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
    private void handleMesBulletins() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Bulletin");
        info.setHeaderText(null);
        info.setContentText("Ici s'afficheront les notes de l'élève suivi.");
        info.showAndWait();
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
}