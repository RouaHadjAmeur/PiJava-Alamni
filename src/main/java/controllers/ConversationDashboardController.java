package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import controllers.AddConversationController;
import services.ConversationService;
import Main.DatabaseConnection;
import util.Session;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ConversationDashboardController {

    @FXML private ListView<Conversation> allConversationsListView;
    @FXML private Button btnAddConversation;
    @FXML private ToggleButton btnAllConversations;
    @FXML private ToggleButton btnSentConversations;
    @FXML private ToggleButton btnReceivedConversations;
    @FXML private Button btnBackToHome;
    @FXML private AnchorPane mainContentPane;
    @FXML private Label dashboardTitleLabel;
    @FXML private HBox filterButtonsContainer;

    // New UI elements for search and pagination
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchTypeComboBox;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button btnPrev;
    @FXML private Label pageLabel;
    @FXML private Button btnNext;
    @FXML private ComboBox<Integer> pageSizeComboBox;

    private final ConversationService service = new ConversationService();
    private ObservableList<Conversation> allConversations = FXCollections.observableArrayList();
    private FilteredList<Conversation> filteredConversations;

    // Pagination variables
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;

    // Search state
    private String currentFilterType = "TOUS";
    private String searchQuery = "";
    private LocalDate startDate = null;
    private LocalDate endDate = null;

    public ConversationDashboardController() {
        DatabaseConnection.getInstance();
    }

    @FXML
    public void initialize() {
        try {
            setupDashboardBasedOnRole();

            // Initialize search and pagination components
            initializeSearchComponents();
            initializePaginationControls();

            loadAllConversations();
            applyFilters();

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

    // Method to refresh the conversation data
    public void refreshData() {
        loadAllConversations();
        applyFilters();
    }

    private void initializeSearchComponents() {
        if (searchField == null || searchTypeComboBox == null) {
            // Create search components if they don't exist in FXML
            HBox searchBox = new HBox(10);
            searchBox.setAlignment(Pos.CENTER_LEFT);
            searchBox.setPadding(new Insets(10, 0, 10, 0));

            searchField = new TextField();
            searchField.setPromptText("Rechercher...");
            searchField.setPrefWidth(250);

            searchTypeComboBox = new ComboBox<>();
            searchTypeComboBox.getItems().addAll("Sujet", "Expéditeur", "Destinataire", "Contenu");
            searchTypeComboBox.setValue("Sujet");

            btnSearch = new Button("🔍");
            Tooltip searchTooltip = new Tooltip("Rechercher");
            Tooltip.install(btnSearch, searchTooltip);
            btnSearch.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-min-width: 36px; -fx-min-height: 36px; -fx-background-radius: 4px;");
            btnSearch.setOnAction(e -> handleSearch());

            btnClearSearch = new Button("❌");
            Tooltip clearTooltip = new Tooltip("Effacer la recherche");
            Tooltip.install(btnClearSearch, clearTooltip);
            btnClearSearch.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-min-width: 36px; -fx-min-height: 36px; -fx-background-radius: 4px;");
            btnClearSearch.setOnAction(e -> clearSearch());

            Label dateRangeLabel = new Label("Période:");
            dateRangeLabel.setStyle("-fx-font-weight: bold;");

            startDatePicker = new DatePicker();
            startDatePicker.setPromptText("Date début");
            startDatePicker.setPrefWidth(130);
            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                startDate = newVal;
                if (searchField.getText().isEmpty() && startDate != null) {
                    handleSearch();
                }
            });

            endDatePicker = new DatePicker();
            endDatePicker.setPromptText("Date fin");
            endDatePicker.setPrefWidth(130);
            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                endDate = newVal;
                if (searchField.getText().isEmpty() && endDate != null) {
                    handleSearch();
                }
            });

            searchBox.getChildren().addAll(searchField, searchTypeComboBox, btnSearch, btnClearSearch,
                    new Separator(javafx.geometry.Orientation.VERTICAL), dateRangeLabel, startDatePicker, endDatePicker);

            // Insert search box below filter buttons
            VBox contentPane = (VBox) filterButtonsContainer.getParent();
            contentPane.getChildren().add(contentPane.getChildren().indexOf(allConversationsListView), searchBox);
        }
    }

    private void initializePaginationControls() {
        if (btnPrev == null || pageLabel == null || btnNext == null) {
            // Create pagination controls
            HBox paginationBox = new HBox(10);
            paginationBox.setAlignment(Pos.CENTER);
            paginationBox.setPadding(new Insets(10, 0, 10, 0));

            btnPrev = new Button("◀");
            btnPrev.setStyle("-fx-background-color: #4B5563; -fx-text-fill: white; -fx-min-width: 36px; -fx-min-height: 36px; -fx-background-radius: 4px;");
            btnPrev.setOnAction(e -> previousPage());

            pageLabel = new Label("Page 1 sur 1");

            btnNext = new Button("▶");
            btnNext.setStyle("-fx-background-color: #4B5563; -fx-text-fill: white; -fx-min-width: 36px; -fx-min-height: 36px; -fx-background-radius: 4px;");
            btnNext.setOnAction(e -> nextPage());

            Label itemsPerPageLabel = new Label("Éléments par page:");

            pageSizeComboBox = new ComboBox<>();
            pageSizeComboBox.getItems().addAll(5, 10, 20, 50);
            pageSizeComboBox.setValue(10);
            pageSizeComboBox.setOnAction(e -> {
                pageSize = pageSizeComboBox.getValue();
                currentPage = 1; // Reset to first page when changing page size
                applyFilters();
            });

            paginationBox.getChildren().addAll(btnPrev, pageLabel, btnNext, new Separator(javafx.geometry.Orientation.VERTICAL),
                    itemsPerPageLabel, pageSizeComboBox);

            // Add pagination controls below the list view
            VBox contentPane = (VBox) filterButtonsContainer.getParent();
            contentPane.getChildren().add(paginationBox);
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

            // Update with icon buttons for admin
            btnAllConversations.setText("👥 Toutes les conversations");
            btnSentConversations.setText("📤 Envoyées");
            btnReceivedConversations.setText("📥 Reçues");
        } else {
            dashboardTitleLabel.setText("Mes conversations");

            // Update with icon buttons for non-admin users
            btnAllConversations.setText("👥 Mes conversations");
            btnSentConversations.setText("📤 Envoyées");
            btnReceivedConversations.setText("📥 Reçues");
        }

        // Update the "Add Conversation" button to use an icon
        btnAddConversation.setText("✉️ Nouvelle Conversation");
        btnAddConversation.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");

        // Update the "Back to Home" button to use an icon
        btnBackToHome.setText("🏠 Accueil");
        btnBackToHome.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
    }

    @FXML
    public void handleReceivedFilter() {
        try {
            currentFilterType = "RECUES";
            currentPage = 1; // Reset to first page when changing filter
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage des conversations reçues: " + e.getMessage());
        }
    }

    @FXML
    public void handleSentFilter() {
        try {
            currentFilterType = "ENVOYEES";
            currentPage = 1; // Reset to first page when changing filter
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage des conversations envoyées: " + e.getMessage());
        }
    }

    @FXML
    public void handleAllFilter() {
        try {
            currentFilterType = "TOUS";
            currentPage = 1; // Reset to first page when changing filter
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage de toutes les conversations: " + e.getMessage());
        }
    }

    @FXML
    public void handleSearch() {
        searchQuery = searchField.getText().trim();
        currentPage = 1; // Reset to first page when searching
        applyFilters();
    }

    @FXML
    public void clearSearch() {
        searchField.clear();
        searchQuery = "";
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        startDate = null;
        endDate = null;
        currentPage = 1; // Reset to first page when clearing search
        applyFilters();
    }

    @FXML
    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagedItems();
        }
    }

    @FXML
    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagedItems();
        }
    }

    private void loadAllConversations() {
        try {
            // Get the current user
            Utilisateur currentUser = Session.getUtilisateurConnecte();
            if (currentUser == null) {
                showErrorAlert("Aucun utilisateur connecté. Veuillez vous connecter.");
                return;
            }

            // Clear existing conversations
            allConversations.clear();

            // Get conversations based on role
            List<Conversation> conversations;
            if ("ADMINISTRATEUR".equalsIgnoreCase(currentUser.getRole())) {
                conversations = service.afficher(); // All conversations for admin
            } else {
                conversations = service.getMesConversations(); // Only user's conversations
            }

            // Add to observable list
            allConversations.addAll(conversations);

            // Initialize filtered list
            filteredConversations = new FilteredList<>(allConversations, p -> true);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du chargement des conversations: " + e.getMessage());
        }
    }

    private void applyFilters() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) return;

        String userEmail = currentUser.getEmail();

        filteredConversations.setPredicate(conversation -> {
            // Type filter (SENT/RECEIVED/ALL)
            boolean matchesType;
            switch (currentFilterType) {
                case "ENVOYEES":
                    matchesType = userEmail.equals(conversation.getExpediteur_email());
                    break;
                case "RECUES":
                    matchesType = userEmail.equals(conversation.getDestinataire_email());
                    break;
                default: // "TOUS"
                    matchesType = true;
                    break;
            }

            // Search query filter
            boolean matchesSearch = true;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String query = searchQuery.toLowerCase();
                String searchType = searchTypeComboBox.getValue();

                if (searchType == null) {
                    searchType = "Sujet";
                }

                switch (searchType) {
                    case "Sujet":
                        matchesSearch = conversation.getSujet() != null &&
                                conversation.getSujet().toLowerCase().contains(query);
                        break;
                    case "Expéditeur":
                        matchesSearch = conversation.getExpediteur_email() != null &&
                                conversation.getExpediteur_email().toLowerCase().contains(query);
                        break;
                    case "Destinataire":
                        matchesSearch = conversation.getDestinataire_email() != null &&
                                conversation.getDestinataire_email().toLowerCase().contains(query);
                        break;
                    case "Contenu":
                        if (conversation.getMessages() != null) {
                            matchesSearch = conversation.getMessages().stream()
                                    .anyMatch(m -> m.getContenu() != null &&
                                            m.getContenu().toLowerCase().contains(query));
                        }
                        break;
                    default:
                        matchesSearch = true;
                }
            }

            // Date range filter
            boolean matchesDateRange = true;
            if (conversation.getDate_creation() != null && (startDate != null || endDate != null)) {
                java.time.LocalDate conversationDate = conversation.getDate_creation().toLocalDate();

                if (startDate != null && endDate != null) {
                    matchesDateRange = !conversationDate.isBefore(startDate) && !conversationDate.isAfter(endDate);
                } else if (startDate != null) {
                    matchesDateRange = !conversationDate.isBefore(startDate);
                } else if (endDate != null) {
                    matchesDateRange = !conversationDate.isAfter(endDate);
                }
            }

            return matchesType && matchesSearch && matchesDateRange;
        });

        updatePagedItems();
    }

    private void updatePagedItems() {
        // Calculate total pages
        int totalItems = filteredConversations.size();
        totalPages = (int) Math.ceil((double) totalItems / pageSize);

        if (totalPages == 0) totalPages = 1; // At least one page even if empty

        // Adjust current page if out of bounds
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        // Calculate start and end indices for the current page
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        // Create a list for the current page
        List<Conversation> pagedItems;
        if (fromIndex < toIndex) {
            pagedItems = new ArrayList<>(filteredConversations.subList(fromIndex, toIndex));
        } else {
            pagedItems = new ArrayList<>();
        }

        // Update the list view with only the current page items
        allConversationsListView.setItems(FXCollections.observableArrayList(pagedItems));
        setupListViewCellFactory(allConversationsListView);

        // Update the page label
        pageLabel.setText(String.format("Page %d sur %d", currentPage, totalPages));

        // Enable/disable navigation buttons
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
    }

    private void setupListViewCellFactory(ListView<Conversation> listView) {
        listView.setCellFactory(param -> new ConversationListCell());
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
    
    @FXML
    public void handleBackToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/parent.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) btnBackToHome.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du retour à l'accueil: " + e.getMessage());
        }
    }
    
    @FXML
    public void handleAddConversation() {
        try {
            // Load the new conversation form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_conversation_view.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set up the refresh callback
            AddConversationController controller = loader.getController();
            controller.setRefreshCallback(param -> refreshData());
            
            // Create a new stage for the form
            Stage newStage = new Stage();
            newStage.setTitle("Nouvelle Conversation");
            newStage.setScene(new Scene(root));
            
            // Set modality to block input to other windows
            newStage.initModality(Modality.WINDOW_MODAL);
            newStage.initOwner(btnAddConversation.getScene().getWindow());
            
            // Set the stage on the controller so it can be accessed
            controller.setStage(newStage);
            
            // Show the form
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors de l'ouverture du formulaire de nouvelle conversation: " + e.getMessage());
        }
    }
}
