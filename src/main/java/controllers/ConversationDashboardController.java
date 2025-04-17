package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Conversation;
import model.Utilisateur;
import services.ConversationService;
import Main.DatabaseConnection;
import util.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;

public class ConversationDashboardController {

    @FXML private ListView<Conversation> allConversationsListView;
    @FXML private Button btnAddConversation;
    @FXML private Button btnAllConversations;
    @FXML private Button btnSentConversations;
    @FXML private Button btnReceivedConversations;
    @FXML private Button btnBackToHome;
    @FXML private AnchorPane mainContentPane;
    @FXML private Label dashboardTitleLabel;
    @FXML private HBox filterButtonsContainer;

    private final ConversationService service = new ConversationService();
    private ObservableList<Conversation> allConversations = FXCollections.observableArrayList();

    public ConversationDashboardController() {
        DatabaseConnection.getInstance();
    }

    @FXML
    public void initialize() {
        try {
            setupDashboardBasedOnRole();
            loadAllConversations();

            // Set the window's userData to a refresh callback
            if (allConversationsListView.getScene() != null &&
                    allConversationsListView.getScene().getWindow() != null) {
                allConversationsListView.getScene().getWindow().setUserData((Runnable) this::refreshData);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement des conversations: " + e.getMessage());
        }
    }

    private void setupDashboardBasedOnRole() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) {
            showErrorAlert("Aucun utilisateur connecté. Veuillez vous connecter.");
            return;
        }

        // Set dashboard title based on user role
        String role = currentUser.getRole();
        if ("ADMINISTRATEUR".equalsIgnoreCase(role)) {
            dashboardTitleLabel.setText("Toutes les conversations (Vue administrateur)");

            // Rename the buttons to be clearer for admin
            btnAllConversations.setText("👥 Toutes les conversations");
            btnSentConversations.setText("📤 Conversations envoyées");
            btnReceivedConversations.setText("📥 Conversations reçues");
        } else {
            dashboardTitleLabel.setText("Mes conversations");

            // Standard button labels for non-admin users
            btnAllConversations.setText("👥 Mes conversations");
            btnSentConversations.setText("📤 Envoyées");
            btnReceivedConversations.setText("📥 Reçues");
        }
    }

    @FXML
    public void handleReceivedFilter() {
        try {
            allConversationsListView.getItems().clear();
            allConversationsListView.setItems(FXCollections.observableArrayList(service.getMesConversationsRecues()));
            setupListViewCellFactory(allConversationsListView);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage des conversations reçues: " + e.getMessage());
        }
    }

    @FXML
    public void handleSentFilter() {
        try {
            allConversationsListView.getItems().clear();
            allConversationsListView.setItems(FXCollections.observableArrayList(service.getMesConversationsEnvoyees()));
            setupListViewCellFactory(allConversationsListView);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage des conversations envoyées: " + e.getMessage());
        }
    }

    @FXML
    public void handleAllFilter() {
        try {
            loadAllConversations();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de toutes les conversations: " + e.getMessage());
        }
    }

    private void loadAllConversations() {
        try {
            allConversations.clear();
            allConversationsListView.getItems().clear();
            allConversations.addAll(service.afficher());
            allConversationsListView.setItems(allConversations);
            setupListViewCellFactory(allConversationsListView);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement des conversations: " + e.getMessage());
        }
    }

    private void setupListViewCellFactory(ListView<Conversation> listView) {
        listView.setCellFactory(param -> new ConversationListCell());
    }

    @FXML
    private void handleNewConversation() {
        try {
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            if (currentUser == null) {
                showErrorAlert("Vous devez être connecté pour créer une nouvelle conversation.");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_conversation_view.fxml"));
            Parent root = loader.load();
            AddConversationController controller = loader.getController();

            controller.setRefreshCallback(this::accept);

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Conversation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors de l'ouverture de la fenêtre de nouvelle conversation: " + e.getMessage());
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
                    // Just refresh the current page if role is unknown
                    refreshData();
                    return;
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
            showErrorAlert("Erreur lors de la navigation: " + e.getMessage());
        }
    }

    public void refreshData() {
        loadAllConversations();
    }

    private void showErrorAlert(String message) {
        System.err.println("ERREUR: " + message);
        
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            // If the alert display fails, at least we've logged to the console
            System.err.println("Impossible d'afficher l'alerte: " + e.getMessage());
        }
    }

    private Object accept(Void aVoid) {
        refreshData();
        return null;
    }
}

