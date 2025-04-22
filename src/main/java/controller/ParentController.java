package controller;

import controllers.AddReclamationController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.stage.Window;
import model.Utilisateur;
import services.ReponseReclamationService;
import util.Session;



import java.io.File;
import java.io.IOException;
import javafx.scene.input.MouseEvent;

public class ParentController {

    @FXML private MenuButton userMenu;
    @FXML private ImageView profileImage;
    @FXML
    private Label alamniLogo;

    @FXML private StackPane notificationPane;
    @FXML private ImageView notificationIcon;
    @FXML private Label notificationBadge;



    @FXML
    public void initialize() {
        Utilisateur user = Session.getUtilisateurConnecte();

        if (user != null) {
            userMenu.setText(user.getPrenom() + " " + user.getNom());
            updateNotificationBadge(user.getEmail());

            if (user.getPhoto() != null) {
                File file = new File(user.getPhoto());
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 40, 40, true, true);
                    profileImage.setImage(image);
                }
            }
        }
    }

    private void updateNotificationBadge(String userEmail) {
        ReponseReclamationService reponseService = new ReponseReclamationService();
        int unread = reponseService.countUnreadResponsesByUserEmail(userEmail);

        if (unread > 0) {
            notificationBadge.setVisible(true);
            notificationBadge.setText(String.valueOf(unread));
        } else {
            notificationBadge.setVisible(false);
        }
    }

    @FXML
    private void handleNotificationsClick(MouseEvent event) {
        Utilisateur user = Session.getUtilisateurConnecte();
        if (user != null) {
            new ReponseReclamationService().markResponsesAsRead(user.getEmail());
            Window currentWindow = ((Node) event.getSource()).getScene().getWindow();
            switchScene("/view/MesReclamations.fxml", "Mes Réclamations", currentWindow);
        }
    }




    @FXML
    private void handleAlamniClick(MouseEvent event) {
        try {
            // Charger la nouvelle scène
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/parent.fxml"));
            Parent root = loader.load();

            // Créer une nouvelle fenêtre
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Espace Parent");
            stage.setResizable(false); // pour garder la même taille

            // Fermer l'ancienne fenêtre
            Stage currentStage = (Stage) alamniLogo.getScene().getWindow();
            currentStage.close();

            // Afficher la nouvelle fenêtre
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleMonProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/profilUtilisateur.fxml"));
            Parent root = loader.load();
            AddReclamationController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
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

//    @FXML
//    private void handleAjouterReclamation(ActionEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
//            Parent root = loader.load();
//
//            AddReclamationController controller = loader.getController();
//
//            Stage stage = new Stage();
//            stage.setTitle("Ajouter Réclamation");
//            stage.setScene(new Scene(root));
//            stage.show();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @FXML
//    private void handleMesReclamations(ActionEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MesReclamations.fxml"));
//            Parent root = loader.load();
//
//            // Récupérer le Stage courant proprement
//            //Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
//            Stage stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
//
//
//            stage.setScene(new Scene(root));
//            stage.setTitle("Mes Réclamations");
//            stage.show();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            showAlert(Alert.AlertType.ERROR, "Impossible de charger Mes Réclamations.");
//        }
//    }

    private void switchScene(String fxmlPath, String windowTitle, Window currentWindow) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene newScene = new Scene(root, 1022, 687); // taille uniforme
            Stage newStage = new Stage();
            newStage.setScene(newScene);
            newStage.setTitle(windowTitle);
            newStage.setResizable(false);

            // Positionner la nouvelle fenêtre à l’endroit exact de l’ancienne
            if (currentWindow instanceof Stage oldStage) {
                newStage.setX(oldStage.getX());
                newStage.setY(oldStage.getY());
                oldStage.close(); // fermeture rapide de l’ancienne
            }

            newStage.show(); // ouverture immédiate
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur lors du chargement de la fenêtre.");
        }
    }


    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        Window currentWindow;
        Utilisateur user = Session.getUtilisateurConnecte();
        if (user != null) {
            new ReponseReclamationService().markResponsesAsRead(user.getEmail());
        }
        if (event.getSource() instanceof MenuItem) {
            currentWindow = ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        } else {
            currentWindow = ((Node) event.getSource()).getScene().getWindow();
        }

        switchScene("/view/add_reclamation_view.fxml", "Ajouter Réclamation", currentWindow);
    }



    @FXML
    private void handleMesReclamations(ActionEvent event) {
        Window currentWindow;

        Utilisateur user = Session.getUtilisateurConnecte();
        if (user != null) {
            new ReponseReclamationService().markResponsesAsRead(user.getEmail());
        }

        if (event.getSource() instanceof MenuItem) {
            currentWindow = ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        } else {
            currentWindow = ((Node) event.getSource()).getScene().getWindow();
        }

        switchScene("/view/MesReclamations.fxml", "Mes Réclamations", currentWindow);
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
}