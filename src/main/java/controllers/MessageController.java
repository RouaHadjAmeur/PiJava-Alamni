package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.application.Platform;
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
    @FXML private TextField messageContentTextField;
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
        if (messagesListView != null) {
            messagesListView.setCellFactory(listView -> new MessageListCell());
        }
        
        // Safely handle buttons that might be null
        if (editButton != null) {
            editButton.setDisable(true);
        }
        
        if (deleteButton != null) {
            deleteButton.setDisable(true);
        }
        
        // Add selection listener to enable buttons when a message is selected
        if (messagesListView != null) {
            messagesListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean isMessageSelected = newValue != null;
                    boolean isCurrentUserSender = isMessageSelected && 
                        newValue.getExpediteur_id() == Session.getUtilisateurConnecte().getId();
                    
                    // Only allow editing/deleting of own messages
                    if (editButton != null) {
                        editButton.setDisable(!isMessageSelected || !isCurrentUserSender);
                    }
                    
                    if (deleteButton != null) {
                        deleteButton.setDisable(!isMessageSelected || !isCurrentUserSender);
                    }
                }
            );
        }
    }
    
    public void setConversation(Conversation conversation) {
        this.currentConversation = conversation;
        
        // Check if current user has permission to view this conversation
        if (!hasPermissionToViewConversation()) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé", 
                    "Vous n'avez pas l'autorisation de voir cette conversation", 
                    "Vous ne pouvez consulter que les conversations dont vous êtes l'expéditeur ou le destinataire.");
            Stage stage = (Stage) messageContentTextField.getScene().getWindow();
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
        String content = messageContentTextField.getText().trim();
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
        messageContentTextField.clear();
        
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
        messageContentTextField.setText(selectedMessage.getContenu());
        
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
    
    @FXML
    private void handleBackToHome() {
        try {
            // Get the current stage
            Stage currentStage = (Stage) messagesListView.getScene().getWindow();
            
            // Load the conversation dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/conversation_dashboard.fxml"));
            Parent root = loader.load();
            
            // Create a new scene
            Scene scene = new Scene(root);
            
            // Set the scene on the stage
            currentStage.setScene(scene);
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible de revenir à la liste des conversations: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteConversation() {
        if (currentConversation == null) return;
        
        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer la conversation");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette conversation et tous ses messages ? Cette action est irréversible.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete the conversation
            conversationService.delete(currentConversation.getId());
            
            // Call the refresh callback if available
            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }
            
            // Close the window
            Stage stage = (Stage) messagesListView.getScene().getWindow();
            stage.close();
        }
    }
    
    @FXML
    private void handleReply() {
        try {
            // Load the add conversation form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_conversation_view.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set up the reply mode
            AddConversationController controller = loader.getController();
            controller.setReplyMode(currentConversation);
            controller.setRefreshCallback(param -> {
                // Refresh the conversation list
                if (refreshCallback != null) {
                    refreshCallback.accept(null);
                }
                
                // Close this window
                Stage stage = (Stage) messagesListView.getScene().getWindow();
                stage.close();
            });
            
            // Create a new stage for the form
            Stage newStage = new Stage();
            newStage.setTitle("Répondre: " + currentConversation.getSujet());
            newStage.setScene(new Scene(root));
            
            // Set the stage on the controller so it can be accessed
            controller.setStage(newStage);
            
            // Show the form
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de répondre", 
                    "Erreur lors de l'ouverture du formulaire de réponse: " + e.getMessage());
        }
    }
    
    private class MessageListCell extends ListCell<Message> {
        private Message lastMessage;
        
        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            
            // Main container for the entire message row
            VBox mainContainer = new VBox(5);
            mainContainer.setPadding(new Insets(5, 10, 5, 10));
            
            // Check if this message is from the same sender as the previous one
            boolean isNewSender = true;
            int currentIndex = getIndex();
            if (currentIndex > 0) {
                Message previousMessage = getListView().getItems().get(currentIndex - 1);
                isNewSender = previousMessage.getExpediteur_id() != message.getExpediteur_id();
            }
            
            // Check if the current user is the sender
            boolean isCurrentUserSender = message.getExpediteur_id() == Session.getUtilisateurConnecte().getId();
            
            // Message container with avatar and content
            HBox messageContainer = new HBox(10);
            
            // Show pinned indicator if message is pinned
            if (message.getIsPinned()) {
                Label pinnedLabel = new Label("📌");
                pinnedLabel.setStyle("-fx-font-size: 14px;");
                VBox pinnedContainer = new VBox(pinnedLabel);
                pinnedContainer.setAlignment(Pos.TOP_CENTER);
                messageContainer.getChildren().add(pinnedContainer);
            }
            
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
            Label timeLabel = new Label(message.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.setFont(Font.font("System", 10));
            timeLabel.setTextFill(Color.GRAY);
            
            // Add components to content container
            contentContainer.getChildren().addAll(contentLabel, timeLabel);
            
            // Add thumbs up/down (like/dislike) counter
            HBox reactionsBox = new HBox(10);
            reactionsBox.setAlignment(Pos.CENTER_LEFT);
            
            Label thumbsUpIcon = new Label("👍 " + message.getLikesCount());
            thumbsUpIcon.setTextFill(Color.GRAY);
            thumbsUpIcon.setFont(Font.font("System", 12));
            
            Label thumbsDownIcon = new Label("👎 " + message.getDislikesCount());
            thumbsDownIcon.setTextFill(Color.GRAY);
            thumbsDownIcon.setFont(Font.font("System", 12));
            
            reactionsBox.getChildren().addAll(thumbsUpIcon, thumbsDownIcon);
            
            // Add action buttons for the message (only visible for user's own messages)
            if (isCurrentUserSender) {
                HBox actionButtonsBox = new HBox(5);
                actionButtonsBox.setAlignment(Pos.CENTER_RIGHT);
                
                // Edit button
                Button editBtn = new Button("✏️");
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                editBtn.setTooltip(new Tooltip("Modifier ce message"));
                editBtn.setOnAction(e -> handleEditSingleMessage(message));
                
                // Delete button
                Button deleteBtn = new Button("🗑️");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                deleteBtn.setTooltip(new Tooltip("Supprimer ce message"));
                deleteBtn.setOnAction(e -> handleDeleteSingleMessage(message));
                
                // Pin/Unpin button
                Button pinBtn = new Button(message.getIsPinned() ? "📌" : "📍");
                pinBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                pinBtn.setTooltip(new Tooltip(message.getIsPinned() ? "Désépingler ce message" : "Épingler ce message"));
                pinBtn.setOnAction(e -> handleTogglePin(message));
                
                actionButtonsBox.getChildren().addAll(editBtn, deleteBtn, pinBtn);
                
                // Add action buttons to reactions box
                HBox combinedBox = new HBox(10);
                combinedBox.getChildren().addAll(reactionsBox, actionButtonsBox);
                combinedBox.setAlignment(Pos.CENTER);
                HBox.setHgrow(reactionsBox, Priority.ALWAYS);
                contentContainer.getChildren().add(combinedBox);
            } else {
                contentContainer.getChildren().add(reactionsBox);
            }
            
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
    
    private void handleEditSingleMessage(Message message) {
        // Store the selected message ID
        int messageId = message.getId();
        
        // Create a dialog for editing the message
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText("Modifier le contenu du message");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the content area
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextArea contentArea = new TextArea(message.getContenu());
        contentArea.setWrapText(true);
        contentArea.setPrefWidth(400);
        contentArea.setPrefHeight(200);
        
        grid.add(new Label("Contenu:"), 0, 0);
        grid.add(contentArea, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the content area by default
        javafx.application.Platform.runLater(contentArea::requestFocus);
        
        // Convert the result to a string when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return contentArea.getText();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(content -> {
            if (content.trim().length() < 3) {
                showAlert(Alert.AlertType.WARNING, "Message trop court", "Le message est trop court", 
                        "Le message doit contenir au moins 3 caractères.");
                return;
            }
            
            // Update the message content
            message.setContenu(content);
            
            // Save to database
            messageService.update(message);
            
            // Refresh the messages list
            loadMessages();
        });
    }
    
    private void handleDeleteSingleMessage(Message message) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le message");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce message ?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete the message
            messageService.delete(message.getId());
            
            // Refresh the messages list
            loadMessages();
        }
    }
    
    private void handleTogglePin(Message message) {
        if (message.getIsPinned()) {
            // Unpin the message
            message.setIsPinned(false);
            messageService.unpinMessage(message.getId());
        } else {
            // Pin the message
            message.setIsPinned(true);
            messageService.pinMessage(message.getId());
        }
        
        // Refresh the messages list
        loadMessages();
    }
}