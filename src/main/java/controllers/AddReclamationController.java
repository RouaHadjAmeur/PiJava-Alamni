package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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


import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.regex.Pattern;

public class AddReclamationController {

    @FXML private MenuButton userMenu;
    @FXML private ImageView profileImage;


    @FXML private TextField emailField;
    @FXML private TextField objetField;
    @FXML private TextArea descriptionArea;
//    @FXML private TextField adminMailField;
    @FXML private ComboBox<String> adminComboBox;

    @FXML private TextField roleField;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;

    @FXML private Label emailErrorLabel;
    @FXML private Label objetErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label roleErrorLabel;
    @FXML private Label adminEmailErrorLabel;

    private final ReclamationServices service = new ReclamationServices();

    @FXML
    private void initialize() {
        Utilisateur user = Session.getUtilisateurConnecte();
        loadAdmins();


        if (user != null) {
            emailLabel.setText(user.getEmail());
            roleLabel.setText(user.getRole());
            userMenu.setText(user.getPrenom() + " " + user.getNom());
        }
        if (user.getPhoto() != null) {
            File file = new File(user.getPhoto());
            if (file.exists()) {
                Image image = new Image(file.toURI().toString(), 40, 40, true, true);
                profileImage.setImage(image);
            }
        }
    }

    private void loadAdmins() {
        try {
            // Appel à un service fictif (on l'ajoutera à l'étape suivante)
            List<Utilisateur> admins = UtilisateurService.getAdmins();

            for (Utilisateur admin : admins) {
                adminComboBox.getItems().add(admin.getEmail());
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur lors du chargement des administrateurs : " + e.getMessage());
        }
    }


    @FXML
    private void handleSave() {
        clearErrors();

        boolean valid = true;

//        // Email admin
//        if (adminMailField.getText().isEmpty()) {
//            adminEmailErrorLabel.setText("Email admin requis.");
//            valid = false;
//        } else if (!isValidEmail(adminMailField.getText())) {
//            adminEmailErrorLabel.setText("Format invalide. Ex: admin@gmail.com");
//            valid = false;
//        }
        String selectedAdminEmail = adminComboBox.getValue();

        if (selectedAdminEmail == null || selectedAdminEmail.isEmpty()) {
            adminEmailErrorLabel.setText("Veuillez sélectionner un administrateur.");
            valid = false;
        } else if (!isValidEmail(selectedAdminEmail)) {
            adminEmailErrorLabel.setText("Format invalide. Ex: admin@gmail.com");
            valid = false;
        }


        // Objet
        if (objetField.getText().isEmpty()) {
            objetErrorLabel.setText("Objet requis.");
            valid = false;
        } else if (objetField.getText().length() < 4) {
            objetErrorLabel.setText("Au moins 4 lettres.");
            valid = false;
        }

        // Description
        if (descriptionArea.getText().isEmpty()) {
            descriptionErrorLabel.setText("Description requise.");
            valid = false;
        } else if (descriptionArea.getText().length() < 10) {
            descriptionErrorLabel.setText("Au moins 10 lettres.");
            valid = false;
        }



        if (!valid) return;

        try {
            Reclamation r = new Reclamation(
                    emailLabel.getText(),
                    objetField.getText(),
                    descriptionArea.getText(),
                    "En attente",
                    new Date(System.currentTimeMillis()),
                   // adminMailField.getText(),
                    selectedAdminEmail,
                    roleLabel.getText(),
                    0
            );

            service.add(r);

            showAlert(Alert.AlertType.INFORMATION, "Réclamation ajoutée avec succès !");
            ((Stage) emailLabel.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
        }
    }


    private boolean isValidEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.matches(regex, email);
    }

    private void clearErrors() {
       // emailErrorLabel.setText("");
        adminEmailErrorLabel.setText("");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            controllers.AddReclamationController controller = loader.getController();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleMesReclamations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MesReclamations.fxml"));
            Parent root = loader.load();

            // Récupérer le Stage courant proprement
            //Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Stage stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();


            stage.setScene(new Scene(root));
            stage.setTitle("Mes Réclamations");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Impossible de charger Mes Réclamations.");
        }
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
