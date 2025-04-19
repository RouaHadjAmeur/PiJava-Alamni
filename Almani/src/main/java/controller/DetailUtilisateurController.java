package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.Eleve;
import model.Utilisateur;
import javafx.scene.layout.VBox;

import java.io.File;

public class DetailUtilisateurController {

    @FXML private ImageView photoView;
    @FXML private Label nomPrenomLabel, emailLabel, roleLabel;
    @FXML private Label niveauLabel, nomNiveauLabel, statutLabel;
    @FXML private VBox sectionNiveau;


    private Utilisateur utilisateur;

    public void setUtilisateur(Utilisateur user) {
        this.utilisateur = user;

        // Mettre à jour les textes (sans "Nom :", "Email :", etc. car ce sont maintenant des labels séparés)
        nomPrenomLabel.setText(user.getNom() + " " + user.getPrenom());
        emailLabel.setText(user.getEmail());
        roleLabel.setText(user.getRole());

        // Configurer le style du statut
        if (user.isPending()) {
            statutLabel.setText("⏳ En attente de validation");
            statutLabel.setStyle("-fx-background-color: #ffd54f; -fx-text-fill: #5d4037; -fx-padding: 5 15; -fx-background-radius: 20;");
        } else {
            statutLabel.setText("🟢 Compte validé");
            statutLabel.setStyle("-fx-background-color: #a5d6a7; -fx-text-fill: #1b5e20; -fx-padding: 5 15; -fx-background-radius: 20;");
        }

        // Photo - Code amélioré pour charger les images depuis différentes sources
        if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
            String photoPath = user.getPhoto();
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

            // Méthode 2: Essayer via le chemin absolu dans src/main/resources
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

            // Méthode 3: Essayer via le chemin absolu dans target/classes
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
                    System.err.println("Impossible de charger la photo: " + photoPath);
                }
            }

            // Si aucune méthode n'a fonctionné, essayer la méthode originale (pour la compatibilité)
            if (!photoLoaded) {
                try {
                    File f = new File(photoPath);
                    if (f.exists()) {
                        photoView.setImage(new Image(f.toURI().toString()));
                    }
                } catch (Exception e) {
                    System.err.println("Échec du chargement de l'image: " + e.getMessage());
                }
            }
        }

        // Si c'est un élève
        if (user instanceof Eleve eleve) {
            niveauLabel.setText(eleve.getNiveau());
            nomNiveauLabel.setText(eleve.getNomNiveau());
            sectionNiveau.setVisible(true);
        } else {
            sectionNiveau.setVisible(false);
        }
    }

    @FXML
    private void handleFermer() {
        Stage stage = (Stage) nomPrenomLabel.getScene().getWindow();
        stage.close();
    }
}