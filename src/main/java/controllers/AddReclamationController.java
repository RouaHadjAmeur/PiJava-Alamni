package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.Reclamation;
import model.Utilisateur;
import services.ReclamationServices;
import service.UtilisateurService;
import util.Session;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.URL;
import java.util.ResourceBundle;

public class AddReclamationController implements Initializable {

    @FXML private MenuButton userMenu;
    @FXML private ImageView profileImage;
    @FXML private TextField objetField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> adminComboBox;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;
    @FXML
    private Label alamniLogo;
    @FXML private Label objetErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label adminEmailErrorLabel;

    private final ReclamationServices service = new ReclamationServices();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set user info
        Utilisateur user = Session.getUtilisateurConnecte();
        if (user != null) {
            userEmailLabel.setText(user.getEmail());
            userRoleLabel.setText(user.getRole());
            userMenu.setText(user.getPrenom() + " " + user.getNom());
            if (user.getPhoto() != null) {
                File file = new File(user.getPhoto());
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 40, 40, true, true);
                    profileImage.setImage(image);
                }
            }
        }
        
        // Load admin emails
        loadAdminEmails();
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


    private void loadAdminEmails() {
        List<Utilisateur> admins = UtilisateurService.getAdmins();
        ObservableList<String> adminEmails = FXCollections.observableArrayList();
        for (Utilisateur admin : admins) {
            adminEmails.add(admin.getEmail());
        }
        adminComboBox.setItems(adminEmails);
    }


    @FXML
    private void handleSave() {
        clearErrors();
        boolean valid = true;

        // Admin email validation
        String selectedAdminEmail = adminComboBox.getValue();
        if (selectedAdminEmail == null || selectedAdminEmail.isEmpty()) {
            adminEmailErrorLabel.setText("Veuillez sélectionner un administrateur");
            valid = false;
        }

        // Objet validation
        if (objetField.getText().isEmpty()) {
            objetErrorLabel.setText("L'objet est requis");
            valid = false;
        } else if (objetField.getText().length() < 5) {
            objetErrorLabel.setText("L'objet doit contenir au moins 5 caractères");
            valid = false;
        }

        // Description validation
        if (descriptionArea.getText().isEmpty()) {
            descriptionErrorLabel.setText("La description est requise");
            valid = false;
        } else if (descriptionArea.getText().length() < 10) {
            descriptionErrorLabel.setText("La description doit contenir au moins 10 caractères");
            valid = false;
        }

        if (!valid) return;

        try {
            Utilisateur user = Session.getUtilisateurConnecte();
            Reclamation r = new Reclamation(
                    user.getEmail(),
                    objetField.getText(),
                    descriptionArea.getText(),
                    "En attente",
                    new Date(System.currentTimeMillis()),
                    selectedAdminEmail,
                    user.getRole(),
                    user.getId(),
                    -2
            );
            //r.setRating(-2);
            service.add(r);

            showAlert(Alert.AlertType.INFORMATION, "Réclamation ajoutée avec succès !");
            
            // Clear the fields instead of closing the window
            objetField.clear();
            descriptionArea.clear();
            adminComboBox.setValue(null);
            clearErrors();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
        }
    }


    private boolean isValidEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.matches(regex, email);
    }

    private void clearErrors() {
        adminEmailErrorLabel.setText("");
        objetErrorLabel.setText("");
        descriptionErrorLabel.setText("");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

//    @FXML
//    private void handleAjouterReclamation(ActionEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
//            Parent root = loader.load();
//            Stage stage = new Stage();
//            stage.setScene(new Scene(root));
//            stage.setTitle("Ajouter Réclamation");
//            stage.setWidth(800);
//            stage.setHeight(600);
//            stage.centerOnScreen();
//            stage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    @FXML
//    private void handleMesReclamations(ActionEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MesReclamations.fxml"));
//            Parent root = loader.load();
//            Stage stage = new Stage();
//            stage.setScene(new Scene(root));
//            stage.setTitle("Mes Réclamations");
//            stage.setWidth(800);
//            stage.setHeight(600);
//            stage.centerOnScreen();
//            stage.show();
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

            // Positionner la nouvelle fenêtre à l'endroit exact de l'ancienne
            if (currentWindow instanceof Stage oldStage) {
                newStage.setX(oldStage.getX());
                newStage.setY(oldStage.getY());
                oldStage.close(); // fermeture rapide de l'ancienne
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

        if (event.getSource() instanceof MenuItem) {
            currentWindow = ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        } else {
            currentWindow = ((Node) event.getSource()).getScene().getWindow();
        }

        switchScene("/view/MesReclamations.fxml", "Mes Réclamations", currentWindow);
    }





    //----------------------------------profile-----------------------------------------------
@FXML
private void handleMonProfil() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/profilUtilisateur.fxml"));
        Parent root = loader.load();
        controllers.AddReclamationController controller = loader.getController();

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
