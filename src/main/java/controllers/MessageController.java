package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import services.ConversationService;
import services.MessageService;
import util.Session;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

public class MessageController {

    @FXML private ListView<Message> messagesListView;
    @FXML private TextArea messageContentTextArea;
    @FXML private Button sendButton;
    @FXML private Button deleteButton;
    @FXML private Button editButton;
    @FXML private Label conversationTitleLabel;

    private Conversation currentConversation;
    private final MessageService messageService = new MessageService();
    private final ConversationService conversationService = new ConversationService();
    private Consumer<Void> refreshCallback;
    
    public void initialize() {
        // Configure message list view with custom cell factory
        messagesListView.setCellFactory(listView -> new MessageListCell());
        
        // Disable edit and delete buttons by default
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        
        // Add selection listener to enable buttons when a message is selected
        messagesListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                boolean isMessageSelected = newValue != null;
                boolean isCurrentUserSender = isMessageSelected && 
                    newValue.getExpediteur_id() == Session.getUtilisateurConnecte().getId();
                
                // Only allow editing/deleting of own messages
                editButton.setDisable(!isMessageSelected || !isCurrentUserSender);
                deleteButton.setDisable(!isMessageSelected || !isCurrentUserSender);
            }
        );
    }
    
    public void setConversation(Conversation conversation) {
        this.currentConversation = conversation;
        
        // Check if current user has permission to view this conversation
        if (!hasPermissionToViewConversation()) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé", 
                    "Vous n'avez pas l'autorisation de voir cette conversation", 
                    "Vous ne pouvez consulter que les conversations dont vous êtes l'expéditeur ou le destinataire.");
            Stage stage = (Stage) messageContentTextArea.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
            return;
        }
        
        conversationTitleLabel.setText("Conversation: " + conversation.getSujet());
        loadMessages();
    }
    
    /**
     * Checks if the current user has permission to view this conversation.
     * Admin users can view all conversations.
     * Other users can only view conversations where they are sender or recipient.
     */
    private boolean hasPermissionToViewConversation() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null || currentConversation == null) {
            return false;
        }
        
        // Admin can view all conversations
        if ("ADMINISTRATEUR".equalsIgnoreCase(currentUser.getRole())) {
            return true;
        }
        
        // Other users can only view their own conversations
        String userEmail = currentUser.getEmail();
        return userEmail.equals(currentConversation.getExpediteur_email()) || 
               userEmail.equals(currentConversation.getDestinataire_email());
    }
    
    public void setRefreshCallback(Consumer<Void> refreshCallback) {
        this.refreshCallback = refreshCallback;
    }
    
    private void loadMessages() {
        // Update message list
        messagesListView.getItems().clear();
        messagesListView.getItems().addAll(currentConversation.getMessages());
        
        // Update conversation read status if necessary
        updateConversationReadStatus();
    }
    
    private void updateConversationReadStatus() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) return;
        
        // If the current user is the recipient and the status is "Non lu"
        if (currentUser.getEmail().equals(currentConversation.getDestinataire_email()) &&
                currentConversation.getStatut().equalsIgnoreCase("Non lu")) {
            
            // Mark as read
            currentConversation.setStatut("Lu");
            conversationService.updateStatut(currentConversation);
        }
        
        // Mark all messages as read for the current user if they are the recipient
        messageService.markAllAsReadByConversationAndRecipient(
                currentConversation.getId(), currentUser.getId());
    }
    
    @FXML
    private void handleSendMessage() {
        String content = messageContentTextArea.getText().trim();
        if (content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Message vide", "Le message est vide", 
                    "Veuillez saisir un message avant d'envoyer.");
            return;
        }
        
        if (content.length() <= 2) {
            showAlert(Alert.AlertType.WARNING, "Message trop court", "Le message est trop court", 
                    "Le message doit contenir au moins 3 caractères.");
            return;
        }
        
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Non connecté", 
                    "Vous devez être connecté pour envoyer un message.");
            return;
        }
        
        // Create and add the message
        Message message = new Message(
                content,
                Timestamp.valueOf(LocalDateTime.now()),
                currentConversation.getId(),
                currentUser.getId(),
                currentUser.getEmail()
        );
        
        messageService.add(message);
        
        // Update the conversation status to "Répondu" if it was "Lu" or "Non lu"
        if (!currentConversation.getStatut().equalsIgnoreCase("Répondu")) {
            currentConversation.setStatut("Répondu");
            conversationService.updateStatut(currentConversation);
        }
        
        // Clear the message input
        messageContentTextArea.clear();
        
        // Refresh the conversation to include the new message
        currentConversation.setMessages(
                messageService.getMessagesByConversationId(currentConversation.getId()));
        loadMessages();
        
        // Call the refresh callback if available
        if (refreshCallback != null) {
            refreshCallback.accept(null);
        }
    }
    
    @FXML
    private void handleEditMessage() {
        Message selectedMessage = messagesListView.getSelectionModel().getSelectedItem();
        if (selectedMessage == null) {
            return;
        }
        
        // Verify that the current user is the sender of the message
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null || selectedMessage.getExpediteur_id() != currentUser.getId()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Permission refusée", 
                    "Vous ne pouvez modifier que vos propres messages.");
            return;
        }
        
        // Set message content to the text area for editing
        messageContentTextArea.setText(selectedMessage.getContenu());
        
        // Create a confirmation alert
        Alert editConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        editConfirm.setTitle("Modifier le message");
        editConfirm.setHeaderText("Confirmez la modification");
        editConfirm.setContentText("Modifiez le message dans la zone de texte ci-dessous puis confirmez.");
        
        // Add the message area for editing
        TextArea editArea = new TextArea(selectedMessage.getContenu());
        editArea.setWrapText(true);
        editArea.setPrefRowCount(5);
        editArea.setPrefColumnCount(40);
        
        VBox content = new VBox(10);
        content.getChildren().add(editArea);
        editConfirm.getDialogPane().setContent(content);
        
        Optional<ButtonType> result = editConfirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String updatedContent = editArea.getText().trim();
            
            if (updatedContent.isEmpty() || updatedContent.length() <= 2) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Contenu invalide", 
                        "Le message doit contenir au moins 3 caractères.");
                return;
            }
            
            // Update the message
            selectedMessage.setContenu(updatedContent);
            messageService.update(selectedMessage);
            
            // Refresh the conversation messages
            currentConversation.setMessages(
                    messageService.getMessagesByConversationId(currentConversation.getId()));
            loadMessages();
            
            // Call the refresh callback if available
            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }
        }
    }
    
    @FXML
    private void handleDeleteMessage() {
        Message selectedMessage = messagesListView.getSelectionModel().getSelectedItem();
        if (selectedMessage == null) {
            return;
        }
        
        // Verify that the current user is the sender of the message
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null || selectedMessage.getExpediteur_id() != currentUser.getId()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Permission refusée", 
                    "Vous ne pouvez supprimer que vos propres messages.");
            return;
        }
        
        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le message");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer ce message ? Cette action est irréversible.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete the message
            messageService.delete(selectedMessage.getId());
            
            // Refresh the conversation messages
            currentConversation.setMessages(
                    messageService.getMessagesByConversationId(currentConversation.getId()));
            loadMessages();
            
            // Call the refresh callback if available
            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private class MessageListCell extends ListCell<Message> {
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            boolean isCurrentUserSender = (currentUser != null && message.getExpediteur_id() == currentUser.getId());
            
            // Check if this is a new sender compared to the previous message
            boolean isNewSender = true;
            int index = getIndex();
            if (index > 0) {
                Message prevMessage = getListView().getItems().get(index - 1);
                isNewSender = prevMessage.getExpediteur_id() != message.getExpediteur_id();
            }
            
            // Check if this is a message from a different day than the previous message
            boolean isNewDay = true;
            if (index > 0) {
                Message prevMessage = getListView().getItems().get(index - 1);
                isNewDay = !message.getCreatedAt().toLocalDateTime().toLocalDate()
                    .equals(prevMessage.getCreatedAt().toLocalDateTime().toLocalDate());
            }
            
            VBox mainContainer = new VBox(5);
            
            // Add date separator if this is a message from a new day
            if (isNewDay) {
                Label dateLabel = new Label(message.getCreatedAt().toLocalDateTime().toLocalDate().format(dateFormatter));
                dateLabel.setStyle("-fx-background-color: #e2e8f0; -fx-padding: 2 10; -fx-background-radius: 10;");
                dateLabel.setAlignment(Pos.CENTER);
                
                HBox dateLabelContainer = new HBox();
                dateLabelContainer.setAlignment(Pos.CENTER);
                dateLabelContainer.getChildren().add(dateLabel);
                
                mainContainer.getChildren().add(dateLabelContainer);
            }
            
            // Create message container
            HBox messageContainer = new HBox(10);
            messageContainer.setPadding(new Insets(5, 10, 5, 10));
            
            // Only show avatar for the first message from a sender in a sequence
            StackPane avatarPane = null;
            if (isNewSender) {
                // Avatar circle with first letter of sender's email
                String senderInitial = message.getExpediteur_email().substring(0, 1).toUpperCase();
                Circle avatar = new Circle(20);
                avatar.setFill(isCurrentUserSender ? Color.web("#ff6347") : Color.web("#3b82f6"));
                
                Label initialLabel = new Label(senderInitial);
                initialLabel.setTextFill(Color.WHITE);
                initialLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                
                avatarPane = new StackPane();
                avatarPane.getChildren().addAll(avatar, initialLabel);
            } else {
                // Create an empty pane with the same width for alignment
                avatarPane = new StackPane();
                avatarPane.setMinWidth(40);
            }
            
            // Message content container
            VBox contentContainer = new VBox(5);
            contentContainer.setMaxWidth(messagesListView.getWidth() * 0.7);
            
            // Only show the sender's name for the first message in a sequence
            if (isNewSender) {
                Label senderLabel = new Label(isCurrentUserSender ? "Vous" : message.getExpediteur_email());
                senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                senderLabel.setTextFill(isCurrentUserSender ? Color.web("#ff6347") : Color.web("#3b82f6"));
                contentContainer.getChildren().add(senderLabel);
            }
            
            // Message text
            Label contentLabel = new Label(message.getContenu());
            contentLabel.setWrapText(true);
            contentLabel.setPadding(new Insets(8, 12, 8, 12));
            
            // Different bubble shapes based on position in the conversation
            String bubbleStyle;
            if (isCurrentUserSender) {
                bubbleStyle = "-fx-background-color: #ffedea; -fx-background-radius: 15;";
            } else {
                bubbleStyle = "-fx-background-color: #ebf5ff; -fx-background-radius: 15;";
            }
            contentLabel.setStyle(bubbleStyle);
            
            // Timestamp
            Label timeLabel = new Label(message.getCreatedAt().toLocalDateTime().format(timeFormatter));
            timeLabel.setFont(Font.font("System", 10));
            timeLabel.setTextFill(Color.GRAY);
            
            // Add components to content container
            contentContainer.getChildren().addAll(contentLabel, timeLabel);
            
            // Add thumbs up/down (like/dislike) counter
            HBox reactionsBox = new HBox(10);
            reactionsBox.setAlignment(Pos.CENTER_LEFT);
            
            Label thumbsUpIcon = new Label("👍 0");
            thumbsUpIcon.setTextFill(Color.GRAY);
            thumbsUpIcon.setFont(Font.font("System", 12));
            
            Label thumbsDownIcon = new Label("👎 0");
            thumbsDownIcon.setTextFill(Color.GRAY);
            thumbsDownIcon.setFont(Font.font("System", 12));
            
            reactionsBox.getChildren().addAll(thumbsUpIcon, thumbsDownIcon);
            contentContainer.getChildren().add(reactionsBox);
            
            // Arrange components based on sender
            if (isCurrentUserSender) {
                messageContainer.setAlignment(Pos.CENTER_RIGHT);
                messageContainer.getChildren().addAll(contentContainer, avatarPane);
            } else {
                messageContainer.setAlignment(Pos.CENTER_LEFT);
                messageContainer.getChildren().addAll(avatarPane, contentContainer);
            }
            
            mainContainer.getChildren().add(messageContainer);
            setGraphic(mainContainer);
        }
    }
}