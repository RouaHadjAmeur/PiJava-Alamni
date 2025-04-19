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
import javafx.scene.layout.Region;
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
        private LocalDateTime lastDate = null;
        private String lastSender = null;

        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                lastSender = null;
                return;
            }

            // Get current user
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            boolean isSentByCurrentUser = message.getExpediteur_id() == currentUser.getId();
            String senderEmail = message.getExpediteur_email();

            // Check if we need a date separator
            LocalDateTime messageDate = message.getCreatedAt().toLocalDateTime();
            boolean isNewDay = lastDate == null || !messageDate.toLocalDate().equals(lastDate.toLocalDate());

            // Main container for the cell
            VBox container = new VBox(8);
            container.setPadding(new Insets(4, 8, 4, 8));

            // Add day separator if needed
            if (isNewDay) {
                Label dateSeparator = new Label(dateFormatter.format(messageDate));
                dateSeparator.getStyleClass().add("day-separator");

                StackPane separatorPane = new StackPane(dateSeparator);
                separatorPane.setAlignment(Pos.CENTER);
                separatorPane.setPadding(new Insets(8, 0, 8, 0));

                container.getChildren().add(separatorPane);
                lastDate = messageDate;
            }

            // Check if this is a new sender (for grouping messages)
            boolean isNewSender = lastSender == null || !lastSender.equals(senderEmail);
            lastSender = senderEmail;

            // Create message box
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(isSentByCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            // If it's a new sender or a new day, show the avatar and name
            if (isNewSender || isNewDay) {
                if (!isSentByCurrentUser) {
                    // Avatar for other user - left side
                    Circle avatar = new Circle(16);
                    avatar.getStyleClass().addAll("avatar-circle");
                    // Use first letter of email as avatar text
                    Label avatarText = new Label(senderEmail.substring(0, 1).toUpperCase());
                    avatarText.setTextFill(Color.WHITE);
                    avatarText.setFont(Font.font("System", FontWeight.BOLD, 14));

                    StackPane avatarPane = new StackPane(avatar, avatarText);
                    messageBox.getChildren().add(avatarPane);
                }
            } else {
                // Add spacing for alignment with avatar when grouped
                if (!isSentByCurrentUser) {
                    Region spacer = new Region();
                    spacer.setMinWidth(42); // Avatar width + spacing
                    messageBox.getChildren().add(spacer);
                }
            }

            // Message content
            VBox messageContent = new VBox(2);

            // Add sender name if new sender or new day
            if (isNewSender || isNewDay) {
                Label senderName = new Label(senderEmail);
                senderName.getStyleClass().add("sender-name");
                senderName.getStyleClass().add(isSentByCurrentUser ? "sender-name-self" : "sender-name-other");
                messageContent.getChildren().add(senderName);
            }

            // Message bubble
            Label textLabel = new Label(message.getContenu());
            textLabel.setWrapText(true);
            textLabel.getStyleClass().add("message-content");

            VBox bubble = new VBox(textLabel);
            bubble.getStyleClass().addAll("message-bubble", isSentByCurrentUser ? "sender-bubble" : "receiver-bubble");

            // Timestamp
            Label timestamp = new Label(timeFormatter.format(messageDate));
            timestamp.getStyleClass().add("timestamp");
            timestamp.setAlignment(isSentByCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            messageContent.getChildren().addAll(bubble, timestamp);

            // Create action buttons if it's the current user's message
            if (isSentByCurrentUser) {
                HBox messageWithActions = new HBox(5);
                messageWithActions.setAlignment(Pos.CENTER_RIGHT);

                // Message actions (reply, edit, delete)
                HBox actionsBox = new HBox(4);
                actionsBox.getStyleClass().add("message-actions");
                actionsBox.setAlignment(Pos.CENTER);
                actionsBox.setVisible(false);

                // Only show actions on hover
                messageContent.setOnMouseEntered(e -> actionsBox.setVisible(true));
                messageContent.setOnMouseExited(e -> actionsBox.setVisible(false));

                // Add actions box and message content
                messageWithActions.getChildren().addAll(actionsBox, messageContent);
                messageBox.getChildren().add(messageWithActions);
            } else {
                // For received messages, just add content
                messageBox.getChildren().add(messageContent);
            }

            // Add avatar for current user on right side if it's a new sender or day
            if (isSentByCurrentUser && (isNewSender || isNewDay)) {
                Circle avatar = new Circle(16);
                avatar.getStyleClass().addAll("avatar-circle", "sender-avatar");

                // Use first letter of email as avatar text
                Label avatarText = new Label(senderEmail.substring(0, 1).toUpperCase());
                avatarText.setTextFill(Color.WHITE);
                avatarText.setFont(Font.font("System", FontWeight.BOLD, 14));

                StackPane avatarPane = new StackPane(avatar, avatarText);
                messageBox.getChildren().add(avatarPane);
            }

            container.getChildren().add(messageBox);
            setGraphic(container);
        }
    }
}
