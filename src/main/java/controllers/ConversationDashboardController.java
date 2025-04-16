package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import model.Conversation;
import services.ConversationService;
import Main.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;

public class ConversationDashboardController {

    @FXML private ListView<Conversation> allConversationsListView;
    @FXML private Button btnAddConversation;
    @FXML private AnchorPane mainContentPane;

    private final ConversationService service = new ConversationService();
    private ObservableList<Conversation> allConversations = FXCollections.observableArrayList();
    private ObservableList<Conversation> favoriteConversations = FXCollections.observableArrayList();

    public ConversationDashboardController() {
        DatabaseConnection.getInstance();
    }

    @FXML
    public void initialize() {
        try {
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

    @FXML
    public void handleFavorisFilter() {
        try {
            allConversationsListView.getItems().clear();
            allConversationsListView.setItems(FXCollections.observableArrayList(service.getMesConversationsRecues()));
            setupListViewCellFactory(allConversationsListView);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage des favoris: " + e.getMessage());
        }
    }

    @FXML
    public void handleTousFilter() {
        try {
            loadAllConversations();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de toutes les conversations: " + e.getMessage());
        }
    }

    private void loadAllConversations() {
        try {
            allConversationsListView.getItems().clear();
            allConversations.addAll(service.afficher());
            allConversationsListView.setItems(allConversations);
            setupListViewCellFactory(allConversationsListView);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement de toutes les conversations: " + e.getMessage());
        }
    }

    private void setupListViewCellFactory(ListView<Conversation> listView) {
        listView.setCellFactory(param -> new ConversationListCell());
    }

    @FXML
    private void handleNewConversation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_conversation_view.fxml"));
            Parent root = loader.load();
            AddConversationController controller = loader.getController();
            controller.setRefreshCallback(v -> refreshData());
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Conversation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors de l'ouverture de la nouvelle conversation: " + e.getMessage());
        }
    }

    @FXML
    private void handleReturnToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/parent.fxml"));
            Parent root = loader.load();

            Scene scene = allConversationsListView.getScene();
            if (scene != null) {
                Stage stage = (Stage) scene.getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Alamni - Espace Parent");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }

    private void showErrorAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshData() {
        loadAllConversations();
    }
}