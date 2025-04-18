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

import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;

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

            // AMÉLIORATION DE LA PHOTO DE PROFIL
            // 1. Définir les dimensions et comportement de base
            photoView.setFitHeight(80);
            photoView.setFitWidth(80);
            photoView.setPreserveRatio(true);

            // 2. Créer un effet de découpage circulaire
            Rectangle clip = new Rectangle(photoView.getFitWidth(), photoView.getFitHeight());
            clip.setArcWidth(photoView.getFitWidth());
            clip.setArcHeight(photoView.getFitHeight());
            photoView.setClip(clip);

            // 3. Ajouter des effets visuels (ombre et bordure)
            DropShadow shadow = new DropShadow();
            shadow.setRadius(10);
            shadow.setColor(Color.rgb(0, 0, 0, 0.2));
            photoView.setEffect(shadow);

            // 4. Ajouter un effet interactif au survol
            photoView.setOnMouseEntered(e -> {
                DropShadow hoverShadow = new DropShadow();
                hoverShadow.setRadius(15);
                hoverShadow.setColor(Color.rgb(57, 73, 171, 0.4));
                photoView.setEffect(hoverShadow);
                photoView.setCursor(Cursor.HAND);
            });

            photoView.setOnMouseExited(e -> {
                photoView.setEffect(shadow);
                photoView.setCursor(Cursor.DEFAULT);
            });

            // 5. Charger la photo avec la méthode robuste
            if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
                String photoPath = currentUser.getPhoto();
                boolean photoLoaded = false;

                // Méthode 1: Essayer via le classpath
                try {
                    Image image = new Image(getClass().getResourceAsStream("/" + photoPath));
                    if (image != null && !image.isError()) {
                        photoView.setImage(image);
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
                            Image image = new Image(imageFile.toURI().toString());
                            photoView.setImage(image);
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
                            Image image = new Image(imageFile.toURI().toString());
                            photoView.setImage(image);
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
                            photoView.setImage(new Image(f.toURI().toString()));
                        }
                    } catch (Exception e) {
                        System.err.println("Échec du chargement de la photo de profil: " + e.getMessage());
                    }
                }
            }

            // 6. Charger une image par défaut si aucune n'est disponible
            if (photoView.getImage() == null) {
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_avatar.png"));
                    photoView.setImage(defaultImage);
                } catch (Exception e) {
                    System.err.println("Impossible de charger l'avatar par défaut: " + e.getMessage());
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
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            mainContentPane.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadViewInMainContent(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            mainContentPane.getChildren().clear();
            mainContentPane.getChildren().add(view);

            // Étendre la vue pour qu'elle remplisse tout le conteneur
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
//            AnchorPane.setLeftAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

