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
import javafx.scene.control.Button;
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
import java.util.ArrayList;
import java.util.List;

public class ConversationDashboardController {

    @FXML private ListView<Conversation> allConversationsListView;
    @FXML private Button btnAddConversation;
    @FXML private Button btnAllConversation; 
    @FXML private Button btnFavoriteConversation; 
    @FXML private Button btnBackToHome;
    @FXML private AnchorPane mainContentPane;
    @FXML private Label dashboardTitleLabel;
    @FXML private HBox filterButtonsContainer;

    // New UI elements for search and pagination
    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchTypeComboBox;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;
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
    private boolean showFavoritesOnly = false;

    public ConversationDashboardController() {
        DatabaseConnection.getInstance();
    }

    @FXML
    public void initialize() {
        // Check if the current user is an admin and redirect to admin dashboard if needed
        checkUserRoleAndRedirect();
        
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
        // Set up search components from FXML or create them if they don't exist
        if (searchTypeComboBox != null) {
            // Initialize the search type combo box with options
            searchTypeComboBox.getItems().clear();
            searchTypeComboBox.getItems().addAll(
                "Tout",
                "Sujet", 
                "Expéditeur", 
                "Destinataire", 
                "Contenu"
            );
            searchTypeComboBox.setValue("Tout");
            
            // Add listener for real-time search as user types
            if (searchField != null) {
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    // Only trigger search if text is 3 or more characters, or if it's empty (to reset)
                    if (newValue.length() >= 3 || newValue.isEmpty()) {
                        searchQuery = newValue;
                        applyFilters();
                    }
                });
            }
        } else {
            // Create search components if they don't exist in FXML
            HBox searchBox = new HBox(10);
            searchBox.setAlignment(Pos.CENTER_LEFT);
            searchBox.setPadding(new Insets(10, 0, 10, 0));

            searchField = new TextField();
            searchField.setPromptText("Rechercher...");
            searchField.setPrefWidth(250);

            searchTypeComboBox = new ComboBox<>();
            searchTypeComboBox.getItems().addAll(
                "Tout",
                "Sujet", 
                "Expéditeur", 
                "Destinataire", 
                "Contenu"
            );
            searchTypeComboBox.setValue("Tout");

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

            searchBox.getChildren().addAll(searchField, searchTypeComboBox, btnSearch, btnClearSearch);

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

        // Safely handle UI elements that might be null
        if (dashboardTitleLabel != null) {
            // Set dashboard title based on user role
            String role = currentUser.getRole();
            if ("ADMINISTRATEUR".equalsIgnoreCase(role)) {
                dashboardTitleLabel.setText("Toutes les conversations (Vue administrateur)");
            } else {
                dashboardTitleLabel.setText("Mes conversations");
            }
        }

        // Style the buttons if they exist
        if (btnAddConversation != null) {
            btnAddConversation.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        
        if (btnBackToHome != null) {
            btnBackToHome.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        }
        
        if (btnFavoriteConversation != null) {
            btnFavoriteConversation.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white;");
        }
        
        if (btnAllConversation != null) {
            btnAllConversation.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white;");
        }
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
    public void handleFavoriteFilter() {
        try {
            showFavoritesOnly = !showFavoritesOnly;
            currentPage = 1; // Reset to first page when changing filter
            applyFilters();
            
            // Update button style based on filter state
            if (btnFavoriteConversation != null) {
                if (showFavoritesOnly) {
                    btnFavoriteConversation.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold;");
                } else {
                    btnFavoriteConversation.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white;");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur lors du filtrage des conversations favorites: " + e.getMessage());
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

            // Search filter
            boolean matchesSearch = true;
            if (!searchQuery.isEmpty()) {
                String searchType = searchTypeComboBox.getValue();
                String lowerCaseQuery = searchQuery.toLowerCase();

                if ("Tout".equals(searchType)) {
                    // Search in all fields
                    matchesSearch = (conversation.getSujet() != null && conversation.getSujet().toLowerCase().contains(lowerCaseQuery)) ||
                                   (conversation.getExpediteur_email() != null && conversation.getExpediteur_email().toLowerCase().contains(lowerCaseQuery)) ||
                                   (conversation.getDestinataire_email() != null && conversation.getDestinataire_email().toLowerCase().contains(lowerCaseQuery));
                } else if ("Sujet".equals(searchType)) {
                    matchesSearch = conversation.getSujet() != null && conversation.getSujet().toLowerCase().contains(lowerCaseQuery);
                } else if ("Expéditeur".equals(searchType)) {
                    matchesSearch = conversation.getExpediteur_email() != null && conversation.getExpediteur_email().toLowerCase().contains(lowerCaseQuery);
                } else if ("Destinataire".equals(searchType)) {
                    matchesSearch = conversation.getDestinataire_email() != null && conversation.getDestinataire_email().toLowerCase().contains(lowerCaseQuery);
                } else if ("Contenu".equals(searchType)) {
                    // Search in message content if available
                    if (conversation.getMessages() != null) {
                        matchesSearch = conversation.getMessages().stream()
                                .anyMatch(m -> m.getContenu() != null &&
                                        m.getContenu().toLowerCase().contains(lowerCaseQuery));
                    } else {
                        matchesSearch = false;
                    }
                }
            }

            // Favorites filter
            boolean matchesFavorites = true;
            if (showFavoritesOnly) {
                matchesFavorites = conversation.isFavorite();
            }

            return matchesType && matchesSearch && matchesFavorites;
        });

        updatePagedItems();
    }

    private void updatePagedItems() {
        // Calculate total pages
        int totalItems = filteredConversations.size();
        totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 1;

        // Ensure current page is within valid range
        if (currentPage < 1) {
            currentPage = 1;
        } else if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        // Calculate start and end indices for the current page
        int fromIndex = (currentPage - 1) * pageSize;
        // Ensure fromIndex is valid
        fromIndex = Math.max(0, Math.min(fromIndex, totalItems > 0 ? totalItems - 1 : 0));
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        // Create a list for the current page
        List<Conversation> pagedItems;
        if (totalItems > 0 && fromIndex < toIndex) {
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
            // Get the current stage
            Stage currentStage = (Stage) btnBackToHome.getScene().getWindow();
            
            // Load the home/parent view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/parent.fxml"));
            Parent root = loader.load();
            
            // Create a new scene
            Scene scene = new Scene(root);
            
            // Set the scene on the stage
            currentStage.setScene(scene);
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de Navigation");
            alert.setHeaderText("Impossible de retourner à l'accueil");
            alert.setContentText("Une erreur est survenue: " + e.getMessage());
            alert.showAndWait();
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
    
    private void checkUserRoleAndRedirect() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser != null && "ADMINISTRATEUR".equalsIgnoreCase(currentUser.getRole())) {
            try {
                // Load the admin dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_dashboard.fxml"));
                Parent root = loader.load();
                
                // Get the current stage
                Stage currentStage = (Stage) dashboardTitleLabel.getScene().getWindow();
                if (currentStage != null) {
                    // Create a new scene with the admin dashboard
                    Scene scene = new Scene(root);
                    currentStage.setScene(scene);
                    currentStage.show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load admin dashboard: " + e.getMessage());
                // Continue with regular dashboard if admin dashboard fails to load
                setupDashboardForRegularUser();
            } catch (NullPointerException e) {
                // This might happen if the FXML elements aren't loaded yet
                // Just continue with regular initialization
                System.err.println("UI elements not ready yet, continuing with regular initialization");
                setupDashboardForRegularUser();
            }
        } else {
            // Set up the dashboard for regular users
            setupDashboardForRegularUser();
        }
    }
    
    private void setupDashboardForRegularUser() {
        // This method is called when the user is not an admin or if loading the admin dashboard fails
        // It ensures the regular conversation dashboard is properly set up
        // The rest of the initialization will happen in the other setup methods
    }
}
