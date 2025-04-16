package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.ParentUser;
import model.Utilisateur;
import service.UtilisateurService;
import services.ConversationService;
import util.Session;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import Main.DatabaseConnection;

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

    private final ConversationService conversationService = new ConversationService();
    private Consumer<Void> refreshCallback;
    private boolean isReplyMode = false;
    private Conversation originalConversation;

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
        ParentUser expediteur = new ParentUser("", "", conversation.getExpediteur_email(), "");
        expediteur.setId(conversation.getExpediteur_id());
        
        destinataireComboBox.setValue(expediteur);
        destinataireComboBox.setDisable(true); // Can't change recipient in reply mode
        
        // Update window title after scene is initialized
        if (destinataireComboBox.getScene() != null) {
            updateStageTitle();
        } else {
            // Add a listener to update the title once the scene is initialized
            destinataireComboBox.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene != null) {
                    updateStageTitle();
                }
            });
        }
    }
    
    private void updateStageTitle() {
        Stage stage = (Stage) destinataireComboBox.getScene().getWindow();
        if (stage != null) {
            stage.setTitle("Répondre: " + originalConversation.getSujet());
        }
    }

    /*private void loadDestinataires() {
        try {
            List<Utilisateur> utilisateurs = UtilisateurService.lister();
            
            // Remove the current user from the list
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            utilisateurs.removeIf(u -> u.getId() == currentUser.getId());
            
            destinataireComboBox.setItems(FXCollections.observableArrayList(utilisateurs));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", "Impossible de charger la liste des destinataires.");
        }
    }*/
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
            
            // Use the first few words of the message as the subject
            String messageText = contenuTextArea.getText();
            String subject;
            
            if (isReplyMode) {
                subject = "Re: " + originalConversation.getSujet();
            } else {
                subject = messageText.length() > 50 ? 
                         messageText.substring(0, 47) + "..." : 
                         messageText;
            }
            
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
            
            conversationService.add(conversation);
            
            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Message envoyé", "Votre message a été envoyé avec succès.");
            handleCancel();
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

    public void setRefreshCallback(Consumer<Void> refreshCallback) {
        this.refreshCallback = refreshCallback;
    }
} 