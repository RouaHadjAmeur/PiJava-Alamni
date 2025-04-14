package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import services.ConversationService;
import services.MessageService;
import util.Session;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class UpdateConversationController {

    @FXML private Label destinataireLabel;
    @FXML private TextField sujetField;
    @FXML private ListView<Message> messagesListView;
    @FXML private TextArea messageTextArea;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    
    private Conversation conversation;
    private final ConversationService conversationService = new ConversationService();
    private final MessageService messageService = new MessageService();
    private Consumer<Void> refreshCallback;
    
    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
        updateUI();
    }
    
    public void setRefreshCallback(Consumer<Void> refreshCallback) {
        this.refreshCallback = refreshCallback;
    }
    
    private void updateUI() {
        if (conversation != null) {
            destinataireLabel.setText(conversation.getDestinataire_email());
            sujetField.setText(conversation.getSujet());
            
            // Set up messages list view
            messagesListView.setCellFactory(param -> new MessageListCell());
            messagesListView.getItems().clear();
            messagesListView.getItems().addAll(conversation.getMessages());
        }
    }
    
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void handleSave() {
        if (validateForm()) {
            // Update the conversation subject
            conversation.setSujet(sujetField.getText());
            conversationService.modify(conversation);
            
            // Add new message if not empty
            if (!messageTextArea.getText().trim().isEmpty()) {
                Utilisateur currentUser = Session.getUtilisateurConnecte();
                
                // Create a new message with the expediteur_email already set
                Message message = new Message(
                    messageTextArea.getText(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    conversation.getId(),
                    currentUser.getId(),
                    currentUser.getEmail()
                );
                
                // Add the message
                messageService.add(message);
                
                // Update status to "Répondu" if it was "Lu" or "Non lu"
                if (!conversation.getStatut().equalsIgnoreCase("Répondu")) {
                    conversation.setStatut("Répondu");
                    conversationService.updateStatut(conversation);
                }
            }
            
            // Call the refresh callback if available
            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }
            
            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Conversation modifiée", 
                    "La conversation a été modifiée avec succès.");
            
            // Close the window
            handleCancel();
        }
    }
    
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (sujetField.getText().trim().isEmpty()) {
            errors.append("- Le sujet ne peut pas être vide.\n");
        }
        
        // Check message length if a new message is being added
        String messageContent = messageTextArea.getText().trim();
        if (!messageContent.isEmpty() && messageContent.length() <= 2) {
            errors.append("- Le message doit contenir plus de 2 caractères.\n");
        }
        
        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire incomplet", errors.toString());
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
    
    private class MessageListCell extends javafx.scene.control.ListCell<Message> {
        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setGraphic(null);
                setText(null);
            } else {
                String content = message.getContenu();
                String timestamp = message.getCreatedAt().toString();
                String sender = message.getExpediteur_id() == Session.getUtilisateurConnecte().getId() ? 
                        "Vous" : conversation.getExpediteur_email();
                
                setText(sender + " (" + timestamp + "):\n" + content);
                setWrapText(true);
            }
        }
    }
} 