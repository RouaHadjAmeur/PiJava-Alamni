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

import java.sql.Timestamp;

public class RepondreConversationController {

    @FXML private TextArea originalMessageTextArea;
    @FXML private Label destinataireLabel;
    @FXML private Label sujetLabel;
    @FXML private TextArea reponseTextArea;
    @FXML private Button btnCancel;
    @FXML private Button btnSend;

    private Conversation originalConversation;
    private final ConversationService service = new ConversationService();

    // Set the original conversation
    public void setConversation(Conversation conversation) {
        this.originalConversation = conversation;
        updateUI();
    }

    // Update the UI with the original conversation details
    private void updateUI() {
        if (originalConversation != null) {
            originalMessageTextArea.setText(originalConversation.getLastMessageContent());

            // Set the recipient to the original sender's email
            destinataireLabel.setText(originalConversation.getExpediteur_email());

            // Add "Re:" to the subject if it doesn't already start with "Re:"
            String sujet = originalConversation.getSujet();
            if (!sujet.startsWith("Re:")) {
                sujet = "Re: " + sujet;
            }
            sujetLabel.setText(sujet);
        }
    }

    // Close the current window (cancel the reply)
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    // Send the reply to the conversation
    @FXML
    private void handleSend() {
        if (validateForm()) {
            Utilisateur expediteur = Session.getUtilisateurConnecte();

            // Create a new message based on the response text
            Message message = new Message(
                    reponseTextArea.getText(),
                    new Timestamp(System.currentTimeMillis()),
                    originalConversation.getId(), // Use the existing conversation ID
                    expediteur.getId(),
                    expediteur.getEmail()
            );

            // Add the message to the conversation using the service
            service.addMessageToConversation(message);

            // Update the conversation status to "Répondu"
            originalConversation.setStatut("Répondu");
            service.updateStatut(originalConversation);

            // Show success alert
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse envoyée", "Votre réponse a été ajoutée à la conversation.");
            handleCancel();
        }
    }

    // Validate the form input
    private boolean validateForm() {
        if (reponseTextArea.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire incomplet", "Le contenu de la réponse ne peut pas être vide.");
            return false;
        }
        return true;
    }

    // Show an alert to the user
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
