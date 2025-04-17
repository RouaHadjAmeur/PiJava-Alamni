package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import service.UtilisateurService;
import services.ConversationService;
import util.Session;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import Main.DatabaseConnection;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public class AddConversationController {

    @FXML private ComboBox<Utilisateur> destinataireComboBox;
    @FXML private TextArea contenuTextArea;
    @FXML private Button btnCancel;
    @FXML private Button btnSend;
    @FXML private Button btnBackToHome;

    private final ConversationService conversationService = new ConversationService();
    private Consumer<Void> refreshCallback;
    private boolean isReplyMode = false;
    private Conversation originalConversation;
    private Stage stage;

    public AddConversationController() {
        DatabaseConnection.getInstance();
    }

    @FXML
    public void initialize() {
        loadDestinataires();
    }

    public void setReplyMode(Conversation conversation) {
        this.isReplyMode = true;
        this.originalConversation = conversation;

        // In reply mode, set the destinataire to the original sender
        Utilisateur expediteur = new Utilisateur() {
            @Override
            public String getDetailsRole() {
                return "";
            }
        };
        expediteur.setEmail(conversation.getExpediteur_email());
        expediteur.setId(conversation.getExpediteur_id());

        destinataireComboBox.setValue(expediteur);
        destinataireComboBox.setDisable(true); // Can't change recipient in reply mode

        // Set the window title early in the method
        updateStageTitle();
    }

    private void updateStageTitle() {
        if (stage != null) {
            stage.setTitle("Répondre: " + originalConversation.getSujet());
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void loadDestinataires() {
        try {
            List<Utilisateur> utilisateurs = UtilisateurService.lister();

            // Remove the current user from the list
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            utilisateurs.removeIf(u -> u.getId() == currentUser.getId());

            destinataireComboBox.setItems(FXCollections.observableArrayList(utilisateurs));

            // Set a custom cell factory to display the email of each utilisateur
            destinataireComboBox.setCellFactory(new Callback<ListView<Utilisateur>, ListCell<Utilisateur>>() {
                @Override
                public ListCell<Utilisateur> call(ListView<Utilisateur> param) {
                    return new ListCell<Utilisateur>() {
                        @Override
                        protected void updateItem(Utilisateur item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null) {
                                setText(item.getEmail());  // Show email in ComboBox
                            } else {
                                setText(null);
                            }
                        }
                    };
                }
            });

            destinataireComboBox.setButtonCell(destinataireComboBox.getCellFactory().call(null));  // Ensure the selected item is also displayed correctly

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", "Impossible de charger la liste des destinataires.");
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
            Utilisateur destinataire = destinataireComboBox.getValue();

            // Check if an existing conversation exists between these users
            Conversation existingConversation = conversationService.findExistingConversation(
                    expediteur.getEmail(), destinataire.getEmail());

            if (existingConversation != null && !isReplyMode) {
                // Use existing conversation
                Message message = new Message(
                        contenuTextArea.getText(),
                        Timestamp.valueOf(LocalDateTime.now()),
                        existingConversation.getId(),
                        expediteur.getId(),
                        expediteur.getEmail()
                );
                
                // Add message to the existing conversation
                conversationService.addMessageToConversation(message);
                
                // Refresh UI
                if (refreshCallback != null) {
                    refreshCallback.accept(null);
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Message envoyé", 
                        "Votre message a été ajouté à la conversation existante.");
                handleCancel();
                return;
            }

            // Use the first few words of the message as the subject if needed
            String messageText = contenuTextArea.getText();
            String subject;

            if (isReplyMode) {
                subject = originalConversation.getSujet();  // Keep original subject instead of adding "Re:"
            } else {
                subject = messageText.length() > 50 ?
                        messageText.substring(0, 47) + "..." :
                        messageText;
            }

            // Create the conversation
            Conversation conversation = new Conversation(
                    subject,
                    Date.valueOf(LocalDate.now()),
                    expediteur.getEmail(),
                    destinataire.getEmail(),
                    "Non lu",
                    expediteur.getId(),
                    destinataire.getId()
            );

            // Create the initial message
            Message message = new Message(
                    messageText,
                    Timestamp.valueOf(LocalDateTime.now()),
                    0, // This will be set after conversation is created
                    expediteur.getId(),
                    expediteur.getEmail()
            );

            conversation.addMessage(message);

            conversationService.ajouter(conversation);

            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Message envoyé", "Votre message a été envoyé avec succès.");
            handleCancel();
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

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (destinataireComboBox.getValue() == null) {
            errors.append("- Veuillez sélectionner un destinataire.\n");
        }

        String messageContent = contenuTextArea.getText().trim();
        if (messageContent.isEmpty()) {
            errors.append("- Le message ne peut pas être vide.\n");
        } else if (messageContent.length() <= 1) {
            errors.append("- Le message doit contenir plus d'un caractère.\n");
        } else {
            // Check for repetitive characters (e.g., "kkkkkkk")
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

    public void setRefreshCallback(Consumer<Void> refreshCallback) {
        this.refreshCallback = refreshCallback;
    }
}
