package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import services.ConversationService;
import services.MessageService;
import util.Session;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;

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

    private Conversation conversation;
    private final ConversationService conversationService = new ConversationService();
    private final MessageService messageService = new MessageService();
    private Consumer<Void> refreshCallback;

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
        updateConversationUI();
        updateStatutIfNecessaire();
        checkPermissions();
    }
    
    public void setRefreshCallback(Consumer<Void> refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    private void updateConversationUI() {
        if (conversation != null) {
            expediteurLabel.setText(conversation.getExpediteur_email());
            destinataireLabel.setText(conversation.getDestinataire_email());
            dateLabel.setText(conversation.getDate_creation().toString());
            updateStatus();
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
        
        // If the current user is the recipient and the message is unread, mark it as read
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
        
        // Only show update and delete buttons if the current user is the sender
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_conversation_view.fxml"));
            Parent root = loader.load();
            AddConversationController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("Répondre");
            stage.setScene(new Scene(root));
            
            // Set reply mode after the scene is set
            controller.setReplyMode(conversation);
            
            // Add refresh callback
            controller.setRefreshCallback(refreshCallback);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la fenêtre de réponse", e.getMessage());
        }
    }
    
    @FXML
    private void handleUpdate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/update_conversation.fxml"));
            Parent root = loader.load();
            
            UpdateConversationController controller = loader.getController();
            controller.setConversation(conversation);
            
            // Pass the refresh callback to the update controller
            if (refreshCallback != null) {
                controller.setRefreshCallback(refreshCallback);
            }
            
            Stage stage = new Stage();
            stage.setTitle("Modifier: " + conversation.getSujet());
            stage.setScene(new Scene(root));
            stage.show();
            
            // Close the current window
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
            try {
                // Delete the conversation
                conversationService.delete(conversation.getId());
                
                // Notify the parent view to refresh
                if (refreshCallback != null) {
                    refreshCallback.accept(null);
                }
                
                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Conversation supprimée", 
                        "La conversation a été supprimée avec succès.");
                
                // Close the current window
                handleClose();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                        "Impossible de supprimer la conversation: " + e.getMessage());
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
    
    private void showError(String title, String header, String content) {
        showAlert(Alert.AlertType.ERROR, title, header, content);
    }
    
    private class MessageListCell extends ListCell<Message> {
        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox messageContainer = new VBox(5);
                Label messageText = new Label(message.getContenu());
                Label timestamp = new Label(message.getCreatedAt().toString());
                
                messageContainer.getChildren().addAll(messageText, timestamp);
                
                // Style based on whether the message is sent or received
                String bubbleStyle = message.getExpediteur_id() == conversation.getExpediteur_id() 
                    ? "message-bubble sent" 
                    : "message-bubble received";
                messageContainer.getStyleClass().add(bubbleStyle);
                
                setGraphic(messageContainer);
            }
        }
    }
} 