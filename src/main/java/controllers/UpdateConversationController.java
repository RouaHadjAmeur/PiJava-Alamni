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
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import java.util.function.Consumer;

public class UpdateConversationController {

    @FXML private Label destinataireLabel;
    @FXML private TextField sujetField;
    @FXML private ListView<Message> messagesListView;
    @FXML private TextArea messageTextArea;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    @FXML private Button btnBackToHome;
    
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
            // Show the conversation partner's email (who we're talking to)
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            String partnerEmail = currentUser.getEmail().equals(conversation.getExpediteur_email()) ? 
                                 conversation.getDestinataire_email() : 
                                 conversation.getExpediteur_email();
            
            destinataireLabel.setText(partnerEmail);
            sujetField.setText(conversation.getSujet());
            
            // Set up messages list view with grouping by sender
            messagesListView.setCellFactory(param -> new MessageListCell());
            messagesListView.getItems().clear();
            messagesListView.getItems().addAll(conversation.getMessages());
        }
    }
    
    @FXML
    private void initialize() {
        // Style and configure the Back to Home button if it exists in the FXML
        if (btnBackToHome != null) {
            btnBackToHome.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        }
    }
    
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
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
    
    @FXML
    private void handleSave() {
        if (validateForm()) {
            // Update the conversation subject
            conversation.setSujet(sujetField.getText());
            conversationService.modifier(conversation);
            
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
        if (!messageContent.isEmpty()) {
            if (messageContent.length() <= 2) {
                errors.append("- Le message doit contenir plus de 2 caractères.\n");
            }
            
            // Check for repetitive characters
            if (isRepetitiveText(messageContent)) {
                errors.append("- Le message contient des caractères répétitifs. Veuillez entrer un message valide.\n");
            }
            
            // Check for inappropriate language
            if (containsInappropriateLanguage(messageContent)) {
                errors.append("- Le message contient des termes inappropriés. Veuillez reformuler votre message.\n");
            }
            
            // Check if message has meaningful content
            if (!hasMinimumWordCount(messageContent, 2)) {
                errors.append("- Le message doit contenir au moins 2 mots.\n");
            }
        }
        
        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire incomplet", errors.toString());
            return false;
        }
        
        return true;
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
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private class MessageListCell extends javafx.scene.control.ListCell<Message> {
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
            
            // Check if this is a new sender compared to the previous message
            boolean isNewSender = true;
            int msgListIndex = getIndex();
            if (msgListIndex > 0) {
                Message prevMessage = getListView().getItems().get(msgListIndex - 1);
                isNewSender = prevMessage.getExpediteur_id() != message.getExpediteur_id();
            }
            
            // Check if this is a message from a different day than the previous message
            boolean isNewDay = true;
            if (msgListIndex > 0) {
                Message prevMessage = getListView().getItems().get(msgListIndex - 1);
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
                    
                    // Refresh the UI
                    updateUI();
                    
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
                    
                    // Refresh the UI
                    updateUI();
                    
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