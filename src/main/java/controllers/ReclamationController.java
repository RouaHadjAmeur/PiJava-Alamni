package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reclamation;
import model.Utilisateur;
import services.ArchiveService;
import services.ReclamationServices;
import util.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReclamationController {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    @FXML private ImageView photoView;
    @FXML private Label nomUtilisateur;
    @FXML private Label roleUtilisateur;
    @FXML private BorderPane rootPane;
    @FXML private AnchorPane mainContentPane;
    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private TextField emailSearchField;
    @FXML
    private TextField objetSearchField;
    @FXML
    private ComboBox<String> dateSortComboBox;

    @FXML
    private PieChart statusPieChart;


    private final ReclamationServices reclamationService = new ReclamationServices();


    private final ReclamationServices service = new ReclamationServices();

    public ReclamationController() {
        Main.DatabaseConnection.getInstance();
        // Démarrer le service d'archivage
        new ArchiveService().startArchiving();
    }

    @FXML
    public void initialize() {
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser != null) {
            nomUtilisateur.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            roleUtilisateur.setText(currentUser.getRole());

            if (currentUser.getPhoto() != null) {
                File photoFile = new File(currentUser.getPhoto());
                if (photoFile.exists()) {
                    Image image = new Image(photoFile.toURI().toString());
                    photoView.setImage(image);
                }
            }
        }

        // Style PieChart
        statusPieChart.getStylesheets().add(
                getClass().getResource("/css/chart.css").toExternalForm()
        );
        statusPieChart.setTitle("Répartition des Réclamations par Statut");
        statusPieChart.setLegendVisible(true);
        statusPieChart.setLabelsVisible(true);
        loadStatsFromAPI();

        // ComboBox Statuts
        statusFilterComboBox.setValue("Tous");
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Tous", "En attente", "En cours", "Résolue"
        ));

        // ComboBox Tri par date
        dateSortComboBox.setItems(FXCollections.observableArrayList(
                "Plus récentes", "Plus anciennes"
        ));
        dateSortComboBox.setValue("Plus récentes");

        // Listeners de filtres
        emailSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadData(); // affichage par défaut
            } else {
                loadData(reclamationService.rechercherParEmail(newVal));
            }
        });

        objetSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadData();
            } else {
                loadData(reclamationService.rechercherParObjet(newVal));
            }
        });

        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Tous".equalsIgnoreCase(newVal)) {
                loadData();
            } else {
                loadData(reclamationService.rechercherParStatut(newVal));
            }
            loadStatsFromAPI(); // mettre à jour le camembert
        });

        dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadData(reclamationService.trierParDate(newVal));
            }
        });

        // Affichage initial
        loadData();
    }

    private void loadData() {
        loadData(service.afficher());
    }


    private void loadData(List<Reclamation> reclamations) {
        Map<String, List<Reclamation>> reclamationsByUser = reclamations.stream()
                .collect(Collectors.groupingBy(Reclamation::getUser_email));

        // Compter les archivées par user
        List<Reclamation> all = service.afficherToutes();
        Map<String, Long> archivedByUser = all.stream()
            .filter(r -> "Archivée".equalsIgnoreCase(r.getStatus()))
            .collect(Collectors.groupingBy(Reclamation::getUser_email, Collectors.counting()));

        ObservableList<Reclamation> displayList = FXCollections.observableArrayList();
        reclamationsByUser.forEach((email, userReclamations) -> {
            if (!userReclamations.isEmpty()) {
                displayList.add(userReclamations.get(0));
            }
        });

        reclamationsListView.setCellFactory(param -> new ListCell<>() {
            private VBox dropdownContent;
            private boolean isExpanded = false;

            @Override
            protected void updateItem(Reclamation item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String userEmail = item.getUser_email();
                List<Reclamation> userReclamations = reclamationsByUser.get(userEmail);
                long archivedCount = archivedByUser.getOrDefault(userEmail, 0L);

                // Create main row
                HBox mainRow = new HBox(15);
                mainRow.setStyle("-fx-padding: 10; -fx-background-color: #fff; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
                mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label emailLabel = new Label(userEmail);
                emailLabel.setPrefWidth(200);
                emailLabel.setStyle("-fx-font-weight: bold;");

                Button toggleButton = new Button(userReclamations.size() + " (+" + archivedCount + " archivées)");
                toggleButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 5;");
                if (archivedCount > 0) {
                    toggleButton.setStyle(toggleButton.getStyle() + " -fx-text-fill: #3B82F6; -fx-background-color: #e5e7eb; -fx-font-style: italic;");
                }

                mainRow.getChildren().addAll(emailLabel, toggleButton);

                dropdownContent = new VBox(5);
                dropdownContent.setStyle("-fx-padding: 10; -fx-background-color: #f3f4f6; -fx-border-color: #ddd; -fx-border-radius: 6;");
                dropdownContent.setVisible(false);
                dropdownContent.setManaged(false);

                HBox header = new HBox(10);
                header.setStyle("-fx-padding: 5; -fx-background-color: #e5e7eb; -fx-background-radius: 5;");
                header.getChildren().addAll(
                    createHeaderLabel("Objet", 150),
                    createHeaderLabel("Statut", 100),
                    createHeaderLabel("Date", 100),
                    createHeaderLabel("Description", 200),
                    createHeaderLabel("Rating", 100),
                    createHeaderLabel("Actions", 150)
                );
                dropdownContent.getChildren().add(header);

                for (Reclamation r : userReclamations) {
                    HBox reclamationRow = new HBox(10);
                    reclamationRow.setStyle("-fx-padding: 5; -fx-background-color: white;");

                    Label objetLabel = new Label(r.getObjet());
                    objetLabel.setPrefWidth(150);

                    Label statusLabel = new Label(r.getStatus());
                    statusLabel.setStyle("-fx-background-color: " + getStatusColor(r.getStatus()) + 
                                      "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
                    statusLabel.setPrefWidth(100);

                    Label dateLabel = new Label(r.getDate_soumission().toString());
                    dateLabel.setPrefWidth(100);

                    Label descLabel = new Label(r.getDescription());
                    descLabel.setPrefWidth(200);
                    descLabel.setWrapText(true);

                    // Affichage correct du rating
                    HBox starsContainer = new HBox(2);
                    starsContainer.setPrefWidth(100);
                    starsContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label ratingLabel = new Label(getRatingStars(r.getRating()));
                    ratingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFD700;");
                    starsContainer.getChildren().add(ratingLabel);

                    HBox actionsBox = createActionButtons(r);
                    actionsBox.setPrefWidth(150);

                    reclamationRow.getChildren().addAll(
                        objetLabel, statusLabel, dateLabel, descLabel, starsContainer, actionsBox
                    );
                    dropdownContent.getChildren().add(reclamationRow);
                }

                toggleButton.setOnAction(e -> {
                    isExpanded = !isExpanded;
                    dropdownContent.setVisible(isExpanded);
                    dropdownContent.setManaged(isExpanded);
                });

                VBox container = new VBox(5);
                container.getChildren().addAll(mainRow, dropdownContent);
                setGraphic(container);
            }
        });

        reclamationsListView.setItems(displayList);
    }

    private Label createHeaderLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private HBox createActionButtons(Reclamation r) {
        HBox actionsBox = new HBox(5);
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ImageView viewIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/voir.png")));
        viewIcon.setFitHeight(16);
        viewIcon.setFitWidth(16);
        Button btnView = new Button();
        btnView.setGraphic(viewIcon);
        btnView.setStyle("-fx-background-color: #3B82F6; -fx-background-radius: 5;");
        btnView.setOnAction(e -> openViewReclamation(r));

        ImageView replyIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/repondre.png")));
        replyIcon.setFitHeight(16);
        replyIcon.setFitWidth(16);
        Button btnRepondre = new Button();
        btnRepondre.setGraphic(replyIcon);
        btnRepondre.setStyle("-fx-background-color: #10B981; -fx-background-radius: 5;");
        btnRepondre.setOnAction(e -> openRepondreReclamation(r));

        ImageView deleteIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/supprimer.png")));
        deleteIcon.setFitHeight(16);
        deleteIcon.setFitWidth(16);
        Button btnDelete = new Button();
        btnDelete.setGraphic(deleteIcon);
        btnDelete.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 5;");
        btnDelete.setOnAction(e -> handleDeleteReclamation(r));

        actionsBox.getChildren().addAll(btnView, btnRepondre, btnDelete);
        return actionsBox;
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        return btn;
    }

    private String getStatusColor(String status) {
        return switch (status.toLowerCase()) {
            case "en attente" -> "#f59e0b";
            case "en cours" -> "#3b82f6";
            case "résolue" -> "#10b981";
            default -> "#6b7280";
        };
    }

    private void openViewReclamation(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reclamation_view.fxml"));
            Parent root = loader.load();
            ReclamationDetailsController controller = loader.getController();
            controller.setReclamation(r);
            Stage stage = new Stage();
            stage.setTitle("Détails Réclamation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openRepondreReclamation(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/repondre_reclamation.fxml"));
            Parent root = loader.load();
            RepondreReclamationController controller = loader.getController();
            controller.setReclamation(r);
            Stage stage = new Stage();
            stage.setTitle("Répondre Réclamation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleDeleteReclamation(Reclamation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Êtes-vous sûr(e) de vouloir supprimer cette réclamation ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            service.delete(r.getId());
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Suppression réussie");
            info.setHeaderText(null);
            info.setContentText("La réclamation a été supprimée avec succès !");
            info.show();
            loadStatsFromAPI();
            loadData(service.afficher());
        }
    }

    // Navigation
    @FXML private void handleDashboard() { loadView("/view/dashboard.fxml"); }
    @FXML private void handleManagement() { loadView("/view/management.fxml"); }
    @FXML private void handleUser() { loadView("/view/utilisateur.fxml"); }
    @FXML private void handleDemandes() { loadView("/view/demandes.fxml"); }
    @FXML private void handleClasses() { loadView("/view/classes.fxml"); }
    @FXML private void handleReclamations() { loadView("/view/reclamation_dashboard.fxml"); }
    @FXML private void handleArchives() {
        try {
            ArchiveService archiveService = new ArchiveService();
            List<Reclamation> archivedReclamations = archiveService.getArchivedReclamations();
            
            if (archivedReclamations != null) {
                // Afficher les archives dans une nouvelle fenêtre
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/archives.fxml"));
                Parent root = loader.load();
                
                Stage stage = new Stage();
                stage.setTitle("Archives des Réclamations");
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                showAlert(Alert.AlertType.ERROR, "Impossible de récupérer les archives.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur lors du chargement des archives.");
        }
    }


    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadView(String path) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(path));
            mainContentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            // Récupère la fenêtre à partir d'un composant quelconque
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.centerOnScreen(); // centrer proprement
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStatsFromAPI() {
        try {
            List<Reclamation> allReclamations = service.afficher();

            Map<String, Integer> stats = new HashMap<>();
            stats.put("En attente", 0);
            stats.put("En cours", 0);
            stats.put("Résolue", 0);

            for (Reclamation r : allReclamations) {
                String status = r.getStatus().toLowerCase();
                if (status.contains("en cours")) {
                    stats.put("En cours", stats.get("En cours") + 1);
                } else if (status.contains("en attente")) {
                    stats.put("En attente", stats.get("En attente") + 1);
                } else if (status.contains("résolue")) {
                    stats.put("Résolue", stats.get("Résolue") + 1);
                }
            }

            // Calculate total
            int total = stats.values().stream().mapToInt(Integer::intValue).sum();

            // Update the pie chart with the data
            Platform.runLater(() -> {
                statusPieChart.getData().clear();
                for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                    if (entry.getValue() > 0) { // Only add non-zero values
                        double percentage = total > 0 ? (entry.getValue() * 100.0) / total : 0;
                        String label = String.format("%s (%d - %.1f%%)",
                            entry.getKey(),
                            entry.getValue(),
                            percentage);
                        statusPieChart.getData().add(new PieChart.Data(
                            label,
                            entry.getValue()
                        ));
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRatingStars(int rating) {
        if (rating == -1) {
            return "No review";
        }
        else if (rating == -2) {
            return "_";
        }

        StringBuilder stars = new StringBuilder();
        // Add filled stars
        for (int i = 0; i < rating; i++) {
            stars.append("★");
        }
        // Add empty stars
        for (int i = rating; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

}
