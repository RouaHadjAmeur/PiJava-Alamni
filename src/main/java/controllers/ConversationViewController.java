package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ConversationViewController {

    @FXML private Label expediteurLabel;
    @FXML private Label destinataireLabel;
    @FXML private Label dateLabel;
    @FXML private Label statutLabel;
    @FXML private ListView<Message> messagesListView;
    @FXML private Button btnRepondre;
    @FXML private Button btnDelete;
    @FXML private Button btnUpdate;
    @FXML private Button btnClose;
    @FXML private Button btnManageMessages;
    @FXML private Button btnBackToHome;

    private Conversation conversation;
    private final ConversationService conversationService = new ConversationService();
    private final MessageService messageService = new MessageService();
    private Consumer<Void> refreshCallback;

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;

        // Check if current user has permission to view this conversation
        if (!hasPermissionToViewConversation()) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé",
                    "Vous n'avez pas l'autorisation de voir cette conversation",
                    "Vous ne pouvez consulter que les conversations dont vous êtes l'expéditeur ou le destinataire.");
            handleClose();
            return;
        }

        updateConversationUI();
        updateStatutIfNecessaire();
        checkPermissions();
    }

    private boolean hasPermissionToViewConversation() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null || conversation == null) {
            return false;
        }

        if ("ADMINISTRATEUR".equalsIgnoreCase(currentUser.getRole())) {
            return true;
        }

        String userEmail = currentUser.getEmail();
        return userEmail.equals(conversation.getExpediteur_email()) ||
                userEmail.equals(conversation.getDestinataire_email());
    }
    
    public void setRefreshCallback(Consumer<Void> refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    private void updateConversationUI() {
        if (conversation != null) {
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            String partnerEmail = currentUser.getEmail().equals(conversation.getExpediteur_email()) ?
                    conversation.getDestinataire_email() :
                    conversation.getExpediteur_email();

            expediteurLabel.setText(currentUser.getEmail());
            destinataireLabel.setText(partnerEmail);
            dateLabel.setText(conversation.getDate_creation().toString());
            updateStatus();

            // Get the Stage from btnRepondre (a Node in the FXML)
            if (btnRepondre.getScene() != null) {
                Stage stage = (Stage) btnRepondre.getScene().getWindow();
                stage.setTitle("Conversation avec " + partnerEmail);
            }

            loadMessages();
        }
    }

    private void updateStatus() {
        String statusText = conversation.getStatut();
        String styleClass;
        
        if (statusText.equalsIgnoreCase("Répondu")) {
            styleClass = "status-replied";
        } else if (statusText.equalsIgnoreCase("Lu")) {
            styleClass = "status-read";
        } else {
            styleClass = "status-unread";
        }
        
        statutLabel.setText(statusText);
        statutLabel.getStyleClass().clear();
        statutLabel.getStyleClass().add(styleClass);
    }

    private void loadMessages() {
        ObservableList<Message> messages = FXCollections.observableArrayList(conversation.getMessages());
        messagesListView.setItems(messages);
        messagesListView.setCellFactory(listView -> new MessageListCell());
    }

    private void updateStatutIfNecessaire() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        
        if (currentUser != null && 
            currentUser.getEmail().equals(conversation.getDestinataire_email()) && 
            conversation.getStatut().equalsIgnoreCase("Non lu")) {
            
            conversation.setStatut("Lu");
            conversationService.updateStatut(conversation);
            statutLabel.setText("Lu");
            statutLabel.getStyleClass().clear();
            statutLabel.getStyleClass().add("status-read");
        }
    }
    
    private void checkPermissions() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        
        boolean isExpediteur = currentUser != null && 
                              currentUser.getId() == conversation.getExpediteur_id();
        
        btnUpdate.setVisible(isExpediteur);
        btnDelete.setVisible(isExpediteur);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleRepondre() {
        try {
            // Create a dialog to enter reply message
            TextArea replyArea = new TextArea();
            replyArea.setPromptText("Entrez votre réponse...");
            replyArea.setPrefRowCount(5);
            replyArea.setWrapText(true);
            
            VBox content = new VBox(10);
            content.getChildren().addAll(
                new Label("Répondre à cette conversation:"),
                replyArea
            );
            content.setPadding(new Insets(20));
            
            Alert dialog = new Alert(Alert.AlertType.NONE);
            dialog.setTitle("Répondre");
            dialog.setHeaderText("Réponse à " + conversation.getSujet());
            dialog.getDialogPane().setContent(content);
            dialog.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String replyContent = replyArea.getText().trim();
                
                // Validate message content
                String validationError = validateMessageContent(replyContent);
                if (validationError != null) {
                    showAlert(Alert.AlertType.WARNING, "Message invalide", 
                            "Impossible d'envoyer ce message", 
                            validationError);
                    return;
                }
                
                // Get current user
                Utilisateur currentUser = Session.getUtilisateurConnecte();
                
                // Create a new message in the same conversation
                Message reply = new Message(
                    replyContent,
                    java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                    conversation.getId(),
                    currentUser.getId(),
                    currentUser.getEmail()
                );
                
                // Add the message to the conversation
                conversationService.addMessageToConversation(reply);
                
                // Update the conversation object
                conversation.setStatut("Répondu");
                conversationService.updateStatut(conversation);
                
                // Refresh the message list
                conversation.setMessages(messageService.getMessagesByConversationId(conversation.getId()));
                loadMessages();
                updateStatus();
                
                // Call refresh callback if available
                if (refreshCallback != null) {
                    refreshCallback.accept(null);
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                        "Message envoyé", 
                        "Votre réponse a été ajoutée à la conversation.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Impossible d'envoyer la réponse", 
                    e.getMessage());
        }
    }

    /**
     * Validates message content and returns an error message if invalid, or null if valid
     */
    private String validateMessageContent(String content) {
        if (content.isEmpty()) {
            return "Le message ne peut pas être vide.";
        } else if (content.length() <= 1) {
            return "Le message doit contenir plus d'un caractère.";
        }
        
        // Check for repetitive characters
        if (isRepetitiveText(content)) {
            return "Le message contient des caractères répétitifs. Veuillez entrer un message valide.";
        }
        
        // Check for inappropriate language
        if (containsInappropriateLanguage(content)) {
            return "Le message contient des termes inappropriés. Veuillez reformuler votre message.";
        }
        
        // Check if message has meaningful content
        if (!hasMinimumWordCount(content, 2)) {
            return "Le message doit contenir au moins 2 mots.";
        }
        
        return null; // Message is valid
    }
    
    /**
     * Checks if text consists of repetitive characters (e.g., "kkkkkk")
     */
    private boolean isRepetitiveText(String text) {
        if (text.length() < 4) {
            return false; // Too short to be considered repetitive
        }
        
        // Check for characters that repeat more than 3 times in sequence
        for (int i = 0; i < text.length() - 3; i++) {
            char c = text.charAt(i);
            if (c == text.charAt(i + 1) && c == text.charAt(i + 2) && c == text.charAt(i + 3)) {
                return true;
            }
        }
        
        // Check if the text is mostly a single character (more than 40%)
        if (text.length() >= 5) {
            char mostFrequentChar = findMostFrequentChar(text);
            int count = countOccurrences(text, mostFrequentChar);
            double percentage = (double) count / text.length();
            
            return percentage > 0.4; // If more than 40% is one character
        }
        
        return false;
    }
    
    /**
     * Find the most frequent character in a string
     */
    private char findMostFrequentChar(String text) {
        int[] charCounts = new int[256]; // ASCII character counts
        
        for (char c : text.toCharArray()) {
            charCounts[c]++;
        }
        
        char mostFrequent = ' ';
        int maxCount = 0;
        
        for (int i = 0; i < charCounts.length; i++) {
            if (charCounts[i] > maxCount) {
                maxCount = charCounts[i];
                mostFrequent = (char) i;
            }
        }
        
        return mostFrequent;
    }
    
    /**
     * Count occurrences of a character in a string
     */
    private int countOccurrences(String text, char c) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Check if the text contains inappropriate language
     */
    private boolean containsInappropriateLanguage(String text) {
        // List of inappropriate words to filter
        String[] inappropriateWords = {
            "insulte", "connard", "salaud", "merde", "putain", "con", "idiot", 
            "imbécile", "pute", "enculé", "bâtard", "salope", "bite", "niquer"
            // Add more inappropriate words as needed
        };
        
        String lowerText = text.toLowerCase();
        
        for (String word : inappropriateWords) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the message has at least the minimum number of words
     */
    private boolean hasMinimumWordCount(String text, int minWordCount) {
        // Split by whitespace and count non-empty words
        String[] words = text.split("\\s+");
        int realWordCount = 0;
        
        for (String word : words) {
            if (!word.trim().isEmpty()) {
                realWordCount++;
            }
        }
        
        return realWordCount >= minWordCount;
    }

    @FXML
    private void handleManageMessages() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/message_view.fxml"));
            Parent root = loader.load();
            MessageController controller = loader.getController();

            controller.setConversation(conversation);

            if (refreshCallback != null) {
                controller.setRefreshCallback(aVoid -> {
                    conversation.setMessages(messageService.getMessagesByConversationId(conversation.getId()));
                    loadMessages();
                    refreshCallback.accept(null);
                });
            }
            
            Stage stage = new Stage();
            stage.setTitle("Gérer les messages - " + conversation.getSujet());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture",
                    "Impossible d'ouvrir la gestion des messages: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUpdate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/update_conversation.fxml"));
            Parent root = loader.load();
            
            UpdateConversationController controller = loader.getController();
            controller.setConversation(conversation);
            
            if (refreshCallback != null) {
                controller.setRefreshCallback(refreshCallback);
            }
            
            Stage stage = new Stage();
            stage.setTitle("Modifier: " + conversation.getSujet());
            stage.setScene(new Scene(root));
            stage.show();
            
            handleClose();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'ouvrir la fenêtre de modification.");
        }
    }
    
    @FXML
    private void handleDelete() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer la conversation");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette conversation ? Cette action est irréversible.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
                conversationService.delete(conversation.getId());
                
                if (refreshCallback != null) {
                    refreshCallback.accept(null);
                }
                
            handleClose();
        }
    }

    @FXML
    private void handleBackToHome() {
        try {
            // Get the current stage
            Stage currentStage = (Stage) btnBackToHome.getScene().getWindow();
            
            // Get the current user and their role
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            String userRole = currentUser.getRole().toUpperCase();
            
            // Determine which dashboard to load based on the user's role
            String dashboardPath;
            String title;
            
            switch(userRole) {
                case "PARENT":
                    dashboardPath = "/view/parent.fxml";
                    title = "Espace Parent - ALAMNI";
                    break;
                case "ENSEIGNANT":
                    dashboardPath = "/view/enseignant.fxml";
                    title = "Espace Enseignant - ALAMNI";
                    break;
                case "ADMINISTRATEUR":
                    dashboardPath = "/view/admin_dashboard.fxml";
                    title = "Console d'Administration - ALAMNI";
                    break;
                default:
                    // Fallback to conversation dashboard if role is unknown
                    dashboardPath = "/view/conversation_dashboard.fxml";
                    title = "Tableau de Bord des Conversations";
            }
            
            // Load the appropriate dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource(dashboardPath));
            Parent root = loader.load();
            
            // Create a new scene with the dashboard view
            Scene scene = new Scene(root);
            
            // Set the new scene on the current stage
            currentStage.setScene(scene);
            currentStage.setTitle(title);
            currentStage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible de retourner au tableau de bord: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void showError(String title, String header, String content) {
        showAlert(Alert.AlertType.ERROR, title, header, content);
    }
    
    private class MessageListCell extends ListCell<Message> {
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Utilisateur currentUser = Session.getUtilisateurConnecte();
            boolean isCurrentUserSender = (currentUser != null && message.getExpediteur_id() == currentUser.getId());

            boolean isNewSender = true;
            int msgListIndex = getIndex();
            if (msgListIndex > 0) {
                Message prevMessage = getListView().getItems().get(msgListIndex - 1);
                isNewSender = prevMessage.getExpediteur_id() != message.getExpediteur_id();
            }

            boolean isNewDay = true;
            if (msgListIndex > 0) {
                Message prevMessage = getListView().getItems().get(msgListIndex - 1);
                isNewDay = !message.getCreatedAt().toLocalDateTime().toLocalDate()
                        .equals(prevMessage.getCreatedAt().toLocalDateTime().toLocalDate());
            }

            VBox mainContainer = new VBox(5);

            if (isNewDay) {
                Label dateLabel = new Label(message.getCreatedAt().toLocalDateTime().toLocalDate().format(dateFormatter));
                dateLabel.setStyle("-fx-background-color: #e2e8f0; -fx-padding: 2 10; -fx-background-radius: 10;");
                dateLabel.setAlignment(Pos.CENTER);

                HBox dateLabelContainer = new HBox();
                dateLabelContainer.setAlignment(Pos.CENTER);
                dateLabelContainer.getChildren().add(dateLabel);

                mainContainer.getChildren().add(dateLabelContainer);
            }

            HBox messageContainer = new HBox(10);
            messageContainer.setPadding(new Insets(5, 10, 5, 10));

            StackPane avatarPane = null;
            if (isNewSender) {
                String senderInitial = message.getExpediteur_email().substring(0, 1).toUpperCase();
                Circle avatar = new Circle(20);
                avatar.setFill(isCurrentUserSender ? Color.web("#ff6347") : Color.web("#3b82f6"));

                Label initialLabel = new Label(senderInitial);
                initialLabel.setTextFill(Color.WHITE);
                initialLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

                avatarPane = new StackPane();
                avatarPane.getChildren().addAll(avatar, initialLabel);
            } else {
                avatarPane = new StackPane();
                avatarPane.setMinWidth(40);
            }

            VBox contentContainer = new VBox(5);
            contentContainer.setMaxWidth(messagesListView.getWidth() * 0.7);

            if (isNewSender) {
                Label senderLabel = new Label(isCurrentUserSender ? "Vous" : message.getExpediteur_email());
                senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                senderLabel.setTextFill(isCurrentUserSender ? Color.web("#ff6347") : Color.web("#3b82f6"));
                contentContainer.getChildren().add(senderLabel);
            }

            Label contentLabel = new Label(message.getContenu());
            contentLabel.setWrapText(true);
            contentLabel.setPadding(new Insets(8, 12, 8, 12));

            String bubbleStyle;
            if (isCurrentUserSender) {
                bubbleStyle = "-fx-background-color: #ffedea; -fx-background-radius: 15;";
            } else {
                bubbleStyle = "-fx-background-color: #ebf5ff; -fx-background-radius: 15;";
            }
            contentLabel.setStyle(bubbleStyle);

            Label timeLabel = new Label(message.getCreatedAt().toLocalDateTime().format(timeFormatter));
            timeLabel.setFont(Font.font("System", 10));
            timeLabel.setTextFill(Color.GRAY);

            contentContainer.getChildren().addAll(contentLabel, timeLabel);

            // Update the reactions box with clickable buttons
            HBox reactionsBox = new HBox(10);
            reactionsBox.setAlignment(Pos.CENTER_LEFT);
            reactionsBox.setPadding(new Insets(3, 0, 3, 0));

            // Check if current user has already liked or disliked
            boolean hasLiked = currentUser != null && message.isLikedByUser(currentUser.getId());
            boolean hasDisliked = currentUser != null && message.isDislikedByUser(currentUser.getId());

            // Create like button
            HBox likeBox = new HBox(5);
            likeBox.setAlignment(Pos.CENTER);
            likeBox.getStyleClass().add("reaction-box");
            if (hasLiked) {
                likeBox.getStyleClass().add("liked");
            }
            
            Label thumbsUpIcon = new Label("👍");
            thumbsUpIcon.getStyleClass().add("reaction-icon");
            thumbsUpIcon.setTextFill(hasLiked ? Color.web("#3b82f6") : Color.GRAY);
            
            Label likeCountLabel = new Label(String.valueOf(message.getLikesCount()));
            likeCountLabel.getStyleClass().add("reaction-count");
            likeCountLabel.setTextFill(hasLiked ? Color.web("#3b82f6") : Color.GRAY);
            
            likeBox.getChildren().addAll(thumbsUpIcon, likeCountLabel);
            
            // Handle like clicks
            likeBox.setOnMouseClicked(e -> {
                if (currentUser != null) {
                    // Toggle like in database and get updated message
                    Message updatedMessage = messageService.toggleLike(message.getId(), currentUser.getId());
                    
                    // Update the message in the conversation's message list
                    int msgArrayPos = conversation.getMessages().indexOf(message);
                    if (msgArrayPos >= 0) {
                        conversation.getMessages().set(msgArrayPos, updatedMessage);
                    }
                    
                    // Refresh the list view
                    loadMessages();
                    
                    // Stop event propagation
                    e.consume();
                }
            });

            // Create dislike button  
            HBox dislikeBox = new HBox(5);
            dislikeBox.setAlignment(Pos.CENTER);
            dislikeBox.getStyleClass().add("reaction-box");
            if (hasDisliked) {
                dislikeBox.getStyleClass().add("disliked");
            }
            
            Label thumbsDownIcon = new Label("👎");
            thumbsDownIcon.getStyleClass().add("reaction-icon");
            thumbsDownIcon.setTextFill(hasDisliked ? Color.web("#ef4444") : Color.GRAY);
            
            Label dislikeCountLabel = new Label(String.valueOf(message.getDislikesCount()));
            dislikeCountLabel.getStyleClass().add("reaction-count");
            dislikeCountLabel.setTextFill(hasDisliked ? Color.web("#ef4444") : Color.GRAY);
            
            dislikeBox.getChildren().addAll(thumbsDownIcon, dislikeCountLabel);
            
            // Handle dislike clicks
            dislikeBox.setOnMouseClicked(e -> {
                if (currentUser != null) {
                    // Toggle dislike in database and get updated message
                    Message updatedMessage = messageService.toggleDislike(message.getId(), currentUser.getId());
                    
                    // Update the message in the conversation's message list
                    int msgArrayPos = conversation.getMessages().indexOf(message);
                    if (msgArrayPos >= 0) {
                        conversation.getMessages().set(msgArrayPos, updatedMessage);
                    }
                    
                    // Refresh the list view
                    loadMessages();
                    
                    // Stop event propagation
                    e.consume();
                }
            });
            
            // Add a small label to explain the feature on hover
            Label reactionTooltip = new Label("Cliquez pour réagir");
            reactionTooltip.getStyleClass().add("reaction-tooltip");
            reactionTooltip.setVisible(false);
            
            reactionsBox.setOnMouseEntered(e -> reactionTooltip.setVisible(true));
            reactionsBox.setOnMouseExited(e -> reactionTooltip.setVisible(false));

            reactionsBox.getChildren().addAll(likeBox, dislikeBox, reactionTooltip);
            contentContainer.getChildren().add(reactionsBox);

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
