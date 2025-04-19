package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.DiscussionReclamation;
import model.Reclamation;
import model.ReponseReclamation;
import services.DiscussionReclamationService;
import util.Session;

import java.net.URL;
import java.sql.Date;
import java.util.List;
import java.util.ResourceBundle;

public class RepondreAAdminController implements Initializable {

    @FXML private VBox messagesContainer;
    @FXML private TextArea reponseArea;
    @FXML private ScrollPane scrollPane;

    private Reclamation reclamation;
    private ReponseReclamation selectedReponse;
    private ReponseReclamation reponse;
    private final DiscussionReclamationService service = new DiscussionReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        scrollPane.vvalueProperty().bind(messagesContainer.heightProperty());
    }

    public void setReclamationAndReponse(Reclamation reclamation, ReponseReclamation reponse) {
        this.reclamation = reclamation;
        this.selectedReponse = reponse;
        afficherMessages();
    }

    public void setReponse(ReponseReclamation rep) {
        this.reponse = rep;
        afficherMessages(); // ou actualiser si besoin
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        afficherMessages(); // recharge la discussion
    }

    private void afficherMessages() {
        messagesContainer.getChildren().clear();

        /* ── 1) Bloc de la réponse sélectionnée (admin) ───────────────────────── */
        if (selectedReponse != null) {
            Label adminRep = new Label("📩 Réponse de l'administrateur :\n" + selectedReponse.getContenue());
            adminRep.setWrapText(true);
            adminRep.setStyle(
                    "-fx-background-color: #fef3c7;" +   // JAUNE (admin)
                            "-fx-padding: 10;" +
                            "-fx-background-radius: 8;"
            );
            messagesContainer.getChildren().add(adminRep);
        }

        /* ── 2) Tous les messages de la discussion ────────────────────────────── */
       // List<DiscussionReclamation> messages = service.getByReclamationId(reclamation.getId());
        List<DiscussionReclamation> messages = service.getByReponseId(selectedReponse.getId());


        for (DiscussionReclamation msg : messages) {

            /* Choix de la couleur selon le rôle */
            String role = msg.getAuteurRole() == null ? "" : msg.getAuteurRole().toUpperCase();
            String bgColor = switch (role) {
                case "ADMIN", "ADMINISTRATEUR" -> "#fef3c7";   // JAUNE
                case "PARENT", "UTILISATEUR", "USER" -> "#dcfce7"; // VERT
                default -> "#e0f2fe";                           // BLEU‑GRIS (autres rôles)
            };

            Label contenu = new Label(role + " (" + msg.getAuteurEmail() + ") :\n" + msg.getContenu());
            contenu.setWrapText(true);
            contenu.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-padding: 10;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-size: 13;" +
                            "-fx-text-fill: #1f2937;"
            );

            messagesContainer.getChildren().add(contenu);
        }
    }


    @FXML
    private void handleEnvoyer() {
        String contenu = reponseArea.getText().trim();
        if (contenu.isEmpty()) {
            showAlert("Erreur", "Veuillez écrire une réponse.");
            return;
        }

        DiscussionReclamation dr = new DiscussionReclamation();
        dr.setReclamationId(reclamation.getId());
        dr.setAuteurEmail(Session.getUtilisateurConnecte().getEmail());
        dr.setAuteurRole(Session.getUtilisateurConnecte().getRole().toUpperCase());
        dr.setContenu(contenu);
        dr.setReponseId(selectedReponse.getId()); // 👈 TRÈS IMPORTANT !

        dr.setDateReponse(new Date(System.currentTimeMillis()));

        service.ajouter(dr);
        reponseArea.clear();
        afficherMessages();
    }

    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) reponseArea.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
