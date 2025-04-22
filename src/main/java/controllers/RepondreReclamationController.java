package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.Administrateur;
import model.Reclamation;
import model.ReponseReclamation;
import services.ReclamationServices;
import services.ReponseReclamationService;
import util.Session;

import java.sql.Date;

public class RepondreReclamationController {

    @FXML
    private Label emailLabel;

    @FXML
    private Label reponseErrorLabel;

    @FXML
    private Label objetLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private TextArea reponseArea;

    @FXML
    private Button envoyerBtn;

    @FXML
    private Button btnRetour;

    private Reclamation currentReclamation;

    private final ReclamationServices reclamationService = new ReclamationServices();
    private final ReponseReclamationService reponseService = new ReponseReclamationService();

    public void setReclamation(Reclamation reclamation) {
        this.currentReclamation = reclamation;

        emailLabel.setText(reclamation.getUser_email());
        objetLabel.setText(reclamation.getObjet());
        descriptionLabel.setText(reclamation.getDescription());

        btnRetour.setOnAction(e -> ((Stage) btnRetour.getScene().getWindow()).close());
    }

    @FXML
    private void initialize() {
        envoyerBtn.setOnAction(event -> handleEnvoyer());
    }



    private void handleEnvoyer() {
        String reponse = reponseArea.getText();
        reponseErrorLabel.setText(""); // clear erreur

        if (reponse == null || reponse.trim().isEmpty()) {
            reponseErrorLabel.setText("Veuillez saisir une réponse.");
            return;
        } else if (reponse.trim().length() < 10) {
            reponseErrorLabel.setText("Votre réponse doit contenir au moins 10 caractères.");
            return;
        }

        try {
            ReponseReclamation rep = new ReponseReclamation();
            rep.setReclamation(currentReclamation);
            Administrateur admin = (Administrateur) Session.getUtilisateurConnecte();

            rep.setAdmin(admin);            rep.setContenue(reponse);
            rep.setDateReponse(new Date(System.currentTimeMillis()));

            reponseService.add(rep);

            //currentReclamation.setStatus("Résolue");
            reclamationService.update(currentReclamation);

            showAlert(Alert.AlertType.INFORMATION, "Réponse envoyée avec succès !");
            ((Stage) envoyerBtn.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur lors de l'envoi de la réponse : " + e.getMessage());
        }
    }


    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
