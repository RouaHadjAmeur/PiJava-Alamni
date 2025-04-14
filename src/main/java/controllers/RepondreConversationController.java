package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import services.ConversationService;
import util.Session;

import java.sql.Date;
import java.time.LocalDate;

public class RepondreConversationController {

    @FXML private TextArea originalMessageTextArea;
    @FXML private Label destinataireLabel;
    @FXML private Label sujetLabel;
    @FXML private TextArea reponseTextArea;
    @FXML private Button btnCancel;
    @FXML private Button btnSend;

    private Conversation originalConversation;
    private final ConversationService service = new ConversationService();

    public void setConversation(Conversation conversation) {
        this.originalConversation = conversation;
        updateUI();
    }

    private void updateUI() {
        if (originalConversation != null) {
            originalMessageTextArea.setText(originalConversation.getLastMessageContent());
            
            // Inverser l'expéditeur et le destinataire pour la réponse
            destinataireLabel.setText(originalConversation.getExpediteur_email());
            
            // Ajouter 'Re:' au sujet s'il ne commence pas déjà par 'Re:'
            String sujet = originalConversation.getSujet();
            if (!sujet.startsWith("Re:")) {
                sujet = "Re: " + sujet;
            }
            sujetLabel.setText(sujet);
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSend() {
        if (validateForm()) {
            Utilisateur expediteur = Session.getUtilisateurConnecte();
            
            // Créer une nouvelle conversation en tant que réponse
            Conversation reponse = new Conversation(
                    sujetLabel.getText(),
                    Date.valueOf(LocalDate.now()),
                    expediteur.getEmail(),
                    originalConversation.getExpediteur_email(),
                    "Non lu",
                    expediteur.getId(),
                    originalConversation.getExpediteur_id()
            );
            
            // Create and add the message
            Message message = new Message(
                reponseTextArea.getText(),
                new java.sql.Timestamp(System.currentTimeMillis()),
                0, // This will be set after conversation is created
                expediteur.getId(),
                expediteur.getEmail()
            );
            
            reponse.addMessage(message);
            
            service.add(reponse);
            
            // Mettre à jour le statut de la conversation d'origine en "Répondu"
            if (originalConversation.getStatut().equalsIgnoreCase("Non lu") || 
                originalConversation.getStatut().equalsIgnoreCase("Lu")) {
                originalConversation.setStatut("Répondu");
                service.updateStatut(originalConversation);
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse envoyée", "Votre réponse a été envoyée avec succès.");
            handleCancel();
        }
    }

    private boolean validateForm() {
        if (reponseTextArea.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire incomplet", "Le contenu de la réponse ne peut pas être vide.");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 