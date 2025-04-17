package controllers;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import services.ConversationService;
import Main.DatabaseConnection;
import model.Utilisateur;
import util.Session;

import java.io.IOException;
import java.util.List;

public class ConversationListCell extends ListCell<Conversation> {

    private final ConversationService service = new ConversationService();

    @Override
    protected void updateItem(Conversation conversation, boolean empty) {
        super.updateItem(conversation, empty);

        if (empty || conversation == null) {
            setGraphic(null);
        } else {
            VBox container = new VBox(5);
            container.setStyle("-fx-padding: 5;");

            // Get the current user to determine who the conversation is with
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            String partnerEmail = currentUser.getEmail().equals(conversation.getExpediteur_email()) ?
                                 conversation.getDestinataire_email() :
                                 conversation.getExpediteur_email();
            
            // Title and status row
            HBox header = new HBox(10);
            Label conversationTitle = new Label("Conversation avec " + partnerEmail);
            conversationTitle.setStyle("-fx-font-weight: bold;");
            
            Label statusLabel = new Label(conversation.getStatut());
            if (conversation.getStatut().equalsIgnoreCase("Non lu")) {
                statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            } else if (conversation.getStatut().equalsIgnoreCase("Lu")) {
                statusLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            } else if (conversation.getStatut().equalsIgnoreCase("Répondu")) {
                statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
            header.getChildren().addAll(conversationTitle, statusLabel);

            // Original subject
            Label originalSubject = new Label(conversation.getSujet());
            originalSubject.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

            // Last message preview
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

            // Message count and date info
            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_LEFT);
            
            int messageCount = messages != null ? messages.size() : 0;
            Label messagesCountLabel = new Label(messageCount + " messages");
            messagesCountLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            
            Label dateLabel = new Label(" · Dernière activité: " + conversation.getDate_creation().toString());
            dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            
            footer.getChildren().addAll(messagesCountLabel, dateLabel);

            // View button
            Button viewButton = new Button("Voir");
            viewButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-cursor: hand;");
            viewButton.setOnAction(e -> openConversationView(conversation));

            HBox buttonContainer = new HBox();
            buttonContainer.setAlignment(Pos.CENTER_RIGHT);
            buttonContainer.getChildren().add(viewButton);

            container.getChildren().addAll(header, originalSubject, messagePreview, footer, buttonContainer);
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
} 