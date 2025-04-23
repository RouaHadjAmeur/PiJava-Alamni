package controllers;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import services.ConversationService;
import Main.DatabaseConnection;
import model.Utilisateur;
import util.Session;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ConversationListCell extends ListCell<Conversation> {

    private final ConversationService service = new ConversationService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void updateItem(Conversation conversation, boolean empty) {
        super.updateItem(conversation, empty);

        if (empty || conversation == null) {
            setGraphic(null);
        } else {
            VBox container = new VBox(5);
            container.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);");

            // Get the current user to determine who the conversation is with
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            String partnerEmail = currentUser.getEmail().equals(conversation.getExpediteur_email()) ?
                                 conversation.getDestinataire_email() :
                                 conversation.getExpediteur_email();
            
            // Header with title, status and actions
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            
            // Conversation title
            Label conversationTitle = new Label("Conversation avec " + partnerEmail);
            conversationTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            HBox.setHgrow(conversationTitle, Priority.ALWAYS);
            
            // Status label with icon
            HBox statusBox = new HBox(5);
            statusBox.setAlignment(Pos.CENTER);
            
            String statusText = conversation.getStatut();
            String statusIcon;
            String statusColor;
            
            if (statusText.equalsIgnoreCase("Non lu")) {
                statusIcon = "📩";
                statusColor = "#f59e0b";
            } else if (statusText.equalsIgnoreCase("Lu")) {
                statusIcon = "👁️";
                statusColor = "#3b82f6";
            } else { // Répondu
                statusIcon = "↩️";
                statusColor = "#10b981";
            }
            
            Label statusIconLabel = new Label(statusIcon);
            Label statusLabel = new Label(statusText);
            statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");
            statusBox.getChildren().addAll(statusIconLabel, statusLabel);
            
            // Add a spacer region
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            // Action buttons container
            HBox actionButtons = new HBox(8);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            
            // Favorite button with star icon
            String favoriteStyle = conversation.isFavorite() 
                ? "-fx-background-color: #f59e0b; -fx-text-fill: white;" 
                : "-fx-background-color: #d1d5db; -fx-text-fill: white;";
            
            Button favoriteButton = new Button("⭐");
            Tooltip favoriteTooltip = new Tooltip(conversation.isFavorite() ? "Retirer des favoris" : "Ajouter aux favoris");
            Tooltip.install(favoriteButton, favoriteTooltip);
            favoriteButton.getStyleClass().add("icon-button");
            favoriteButton.setStyle(favoriteStyle + " -fx-cursor: hand; -fx-font-size: 14px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-max-width: 30px; -fx-max-height: 30px; -fx-background-radius: 15px;");
            favoriteButton.setOnAction(e -> toggleFavorite(conversation));
            
            // View button with icon
            Button viewButton = new Button("👁️");
            Tooltip viewTooltip = new Tooltip("Voir la conversation");
            Tooltip.install(viewButton, viewTooltip);
            viewButton.getStyleClass().add("icon-button");
            viewButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-max-width: 30px; -fx-max-height: 30px; -fx-background-radius: 15px;");
            viewButton.setOnAction(e -> openConversationView(conversation));
            
            // Delete button with icon
            Button deleteButton = new Button("🗑️");
            Tooltip deleteTooltip = new Tooltip("Supprimer la conversation");
            Tooltip.install(deleteButton, deleteTooltip);
            deleteButton.getStyleClass().add("icon-button");
            deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-max-width: 30px; -fx-max-height: 30px; -fx-background-radius: 15px;");
            deleteButton.setOnAction(e -> deleteConversation(conversation));
            
            actionButtons.getChildren().addAll(favoriteButton, viewButton, deleteButton);
            
            header.getChildren().addAll(conversationTitle, statusBox, spacer, actionButtons);

            // Original subject
            HBox subjectBox = new HBox(5);
            Label subjectLabel = new Label("📝");
            Label subjectText = new Label(conversation.getSujet());
            subjectText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4B5563;");
            subjectBox.getChildren().addAll(subjectLabel, subjectText);

            // Last message preview with icon
            HBox messageBox = new HBox(5);
            Label messageIcon = new Label("💬");
            
            String lastMessageContent = "";
            List<Message> messages = conversation.getMessages();
            if (messages != null && !messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                lastMessageContent = lastMessage.getContenu();
                if (lastMessageContent.length() > 50) {
                    lastMessageContent = lastMessageContent.substring(0, 47) + "...";
                }
            }
            
            Label messagePreview = new Label(lastMessageContent);
            messagePreview.setStyle("-fx-text-fill: #6B7280;");
            messageBox.getChildren().addAll(messageIcon, messagePreview);

            // Footer with message count and date info
            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_LEFT);
            
            // Message count with icon
            int messageCount = messages != null ? messages.size() : 0;
            Label countIcon = new Label("📊");
            Label messagesCountLabel = new Label(messageCount + " messages");
            messagesCountLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            
            // Separator
            Label separator = new Label("•");
            separator.setStyle("-fx-text-fill: #9CA3AF; -fx-padding: 0 5;");
            
            // Date with icon
            Label dateIcon = new Label("🕒");
            
            // Format the date nicer
            String formattedDate = "";
            if (conversation.getDate_creation() != null) {
                LocalDateTime dateTime = conversation.getDate_creation().toLocalDate().atStartOfDay();
                formattedDate = dateTime.format(DATE_FORMAT);
            }
            
            Label dateLabel = new Label(formattedDate);
            dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            
            footer.getChildren().addAll(countIcon, messagesCountLabel, separator, dateIcon, dateLabel);

            container.getChildren().addAll(header, subjectBox, messageBox, footer);
            setGraphic(container);
        }
    }

    private void openConversationView(Conversation conversation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/conversation_view.fxml"));
            Parent root = loader.load();
            
            // Get the controller from the loader
            Object controller = loader.getController();
            
            // Check if it's the correct type and set conversation
            if (controller instanceof ConversationViewController) {
                ConversationViewController viewController = (ConversationViewController) controller;
                viewController.setConversation(conversation);
                
                // Add a refresh callback to update the list after changes
                viewController.setRefreshCallback(unused -> {
                    getListView().refresh();
                    
                    // Try to get the parent controller (ConversationDashboardController) to refresh its data
                    if (getListView().getScene() != null && 
                        getListView().getScene().getWindow() != null) {
                        
                        Object userData = getListView().getScene().getWindow().getUserData();
                        if (userData instanceof Runnable) {
                            ((Runnable) userData).run();
                        }
                    }
                });
                
                Stage stage = new Stage();
                stage.setTitle("Conversation: " + conversation.getSujet());
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                System.err.println("Controller is not a ConversationViewController: " + controller.getClass().getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void deleteConversation(Conversation conversation) {
        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Supprimer la conversation");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir supprimer cette conversation ?");
        confirmDialog.setContentText("Cette action est irréversible et supprimera tous les messages associés.");
        
        // Customize the buttons
        confirmDialog.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                // Delete the conversation
                service.delete(conversation.getId());
                
                // Refresh the list view
                if (getListView().getScene() != null && 
                    getListView().getScene().getWindow() != null) {
                    
                    Object userData = getListView().getScene().getWindow().getUserData();
                    if (userData instanceof Runnable) {
                        ((Runnable) userData).run();
                    }
                }
                
                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Conversation supprimée");
                successAlert.setHeaderText(null);
                successAlert.setContentText("La conversation a été supprimée avec succès.");
                successAlert.showAndWait();
                
            } catch (Exception e) {
                e.printStackTrace();
                
                // Show error message
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Impossible de supprimer la conversation");
                errorAlert.setContentText("Une erreur est survenue: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }
    
    private void toggleFavorite(Conversation conversation) {
        // Toggle the favorite status
        conversation.setFavorite(!conversation.isFavorite());
        
        // Update in database
        service.modifier(conversation);
        
        // Refresh the cell
        updateItem(conversation, false);
        
        // Refresh the ListView to update filters if needed
        if (getListView().getScene() != null && 
            getListView().getScene().getWindow() != null) {
            
            Object userData = getListView().getScene().getWindow().getUserData();
            if (userData instanceof Runnable) {
                ((Runnable) userData).run();
            }
        }
    }
} 