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

            if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                String photoPath = user.getPhoto();
                boolean photoLoaded = false;

                // Méthode 1: Essayer via le classpath
                try {
                    Image image = new Image(getClass().getResourceAsStream("/" + photoPath), 40, 40, true, true);
                    if (image != null && !image.isError()) {
                        profileImage.setImage(image);
                        photoLoaded = true;
                    }
                } catch (Exception e) {
                    // Passer à la méthode suivante
                }

                // Méthode 2: Essayer via src/main/resources
                if (!photoLoaded) {
                    try {
                        File resourceDir = new File("src/main/resources");
                        File imageFile = new File(resourceDir, photoPath);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString(), 40, 40, true, true);
                            profileImage.setImage(image);
                            photoLoaded = true;
                        }
                    } catch (Exception e) {
                        // Passer à la méthode suivante
                    }
                }

                // Méthode 3: Essayer via target/classes
                if (!photoLoaded) {
                    try {
                        File targetDir = new File("target/classes");
                        File imageFile = new File(targetDir, photoPath);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString(), 40, 40, true, true);
                            profileImage.setImage(image);
                            photoLoaded = true;
                        }
                    } catch (Exception e) {
                        // Passer à la méthode suivante
                    }
                }

                // Si aucune méthode n'a fonctionné, essayer la méthode originale
                if (!photoLoaded) {
                    try {
                        File f = new File(photoPath);
                        if (f.exists()) {
                            Image image = new Image(f.toURI().toString(), 40, 40, true, true);
                            profileImage.setImage(image);
                        }
                    } catch (Exception e) {
                        System.err.println("Échec du chargement de la photo: " + e.getMessage());
                    }
                }
            }

// Ajouter une image par défaut si nécessaire
            if (profileImage.getImage() == null) {
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_avatar.png"), 40, 40, true, true);
                    profileImage.setImage(defaultImage);
                } catch (Exception e) {
                    System.err.println("Impossible de charger l'avatar par défaut: " + e.getMessage());
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