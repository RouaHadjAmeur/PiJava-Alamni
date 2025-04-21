package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    }

    @FXML
//    public void initialize() {
//        Utilisateur currentUser = Session.getUtilisateurConnecte();
//        if (currentUser != null) {
//            nomUtilisateur.setText(currentUser.getNom() + " " + currentUser.getPrenom());
//            roleUtilisateur.setText(currentUser.getRole());
//
//            if (currentUser.getPhoto() != null) {
//                File photoFile = new File(currentUser.getPhoto());
//                if (photoFile.exists()) {
//                    Image image = new Image(photoFile.toURI().toString());
//                    photoView.setImage(image);
//                }
//            }
//        }
//        statusPieChart.getStylesheets().add(
//                getClass().getResource("/css/chart.css").toExternalForm()
//        );
//
//
//        statusPieChart.setTitle("Répartition des Réclamations par Statut");
//        statusPieChart.setLegendVisible(true);
//        statusPieChart.setLabelsVisible(true);
//
//        loadStatsFromAPI();
//
//        statusFilterComboBox.setValue("Tous");
//        statusFilterComboBox.setItems(FXCollections.observableArrayList(
//                "Tous", "En attente", "En cours", "Résolue"
//        ));
//
//        emailSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
//                List<Reclamation> filtrées = reclamationService.rechercherParEmail(newVal);
//                reclamationsListView.setItems(FXCollections.observableArrayList(filtrées));
//        });
//
//        objetSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
//            List<Reclamation> filtrées = reclamationService.rechercherParObjet(newVal);
//            reclamationsListView.setItems(FXCollections.observableArrayList(filtrées));
//        });
//
//        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
//            String statut = newVal;
//            if ("Tous".equalsIgnoreCase(statut)) {
//                loadData(); // recharge tout
//            } else {
//                List<Reclamation> filtrées = reclamationService.rechercherParStatut(statut);
//                reclamationsListView.setItems(FXCollections.observableArrayList(filtrées));
//            }
//            // Reload statistics when filter changes
//            loadStatsFromAPI();
//        });
//
//        dateSortComboBox.setItems(FXCollections.observableArrayList(
//                "Plus récentes", "Plus anciennes"
//        ));
//        dateSortComboBox.setValue("Plus récentes");
//
//        dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
//            if (newVal != null) {
//                List<Reclamation> triées = reclamationService.trierParDate(newVal);
//                reclamationsListView.setItems(FXCollections.observableArrayList(triées));
//            }
//        });
//
//        loadData();
//    }
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


//    private void loadData() {
//        List<Reclamation> allReclamations = service.afficher();
//
//        // Group reclamations by user email
//        Map<String, List<Reclamation>> reclamationsByUser = allReclamations.stream()
//                .collect(Collectors.groupingBy(Reclamation::getUser_email));
//
//        ObservableList<Reclamation> displayList = FXCollections.observableArrayList();
//
//        // Create table headers
//        HBox headerRow = new HBox(15);
//        headerRow.setStyle("-fx-padding: 10; -fx-background-color: #f3f4f6; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
//
//        Label emailHeader = new Label("Email");
//        emailHeader.setPrefWidth(150);
//        Label dateHeader = new Label("Date");
//        dateHeader.setPrefWidth(100);
//        Label countHeader = new Label("Réclamations");
//        countHeader.setPrefWidth(120);
//
//        headerRow.getChildren().addAll(emailHeader, dateHeader, countHeader);
//        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
//
//        // Add header to the list view
//        reclamationsListView.setPlaceholder(headerRow);
//
//        reclamationsListView.setCellFactory(param -> new ListCell<>() {
//            @Override
//            protected void updateItem(Reclamation r, boolean empty) {
//                super.updateItem(r, empty);
//
//                if (empty || r == null) {
//                    setGraphic(null);
//                } else {
//                    String userEmail = r.getUser_email();
//                    List<Reclamation> userReclamations = reclamationsByUser.get(userEmail);
//                    int reclamationCount = userReclamations.size();
//
//                    // Main row content
//                    Label email = new Label(userEmail);
//                    email.setPrefWidth(150);
//
//                    Label date = new Label(r.getDate_soumission().toString());
//                    date.setPrefWidth(100);
//
//                    // Reclamation count with dropdown button
//                    Button countButton = new Button(reclamationCount + " réclamation(s)");
//                    countButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 5;");
//                    countButton.setPrefWidth(120);
//
//                    // Main row container
//                    HBox mainRow = new HBox(15, email, date, countButton);
//                    mainRow.setStyle("-fx-padding: 10; -fx-background-color: #fff; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
//                    mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
//
//                    // Create dropdown content
//                    VBox dropdownContent = new VBox(5);
//                    dropdownContent.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 10; -fx-background-radius: 5;");
//                    dropdownContent.setVisible(false);
//                    dropdownContent.setManaged(false);
//
//                    // Create headers for reclamation rows
//                    HBox reclamationHeader = new HBox(10);
//                    reclamationHeader.setStyle("-fx-background-color: #e5e7eb; -fx-padding: 5; -fx-background-radius: 5;");
//
//                    Label recObjetHeader = new Label("Objet");
//                    recObjetHeader.setPrefWidth(150);
//                    Label recStatusHeader = new Label("Statut");
//                    recStatusHeader.setPrefWidth(100);
//                    Label recDateHeader = new Label("Date");
//                    recDateHeader.setPrefWidth(100);
//                    Label recDescHeader = new Label("Description");
//                    recDescHeader.setPrefWidth(200);
//                    Label recActionsHeader = new Label("Actions");
//                    recActionsHeader.setPrefWidth(120);
//
//                    reclamationHeader.getChildren().addAll(recObjetHeader, recStatusHeader, recDateHeader, recDescHeader, recActionsHeader);
//                    dropdownContent.getChildren().add(reclamationHeader);
//
//                    for (Reclamation userReclamation : userReclamations) {
//                        HBox reclamationRow = new HBox(10);
//                        reclamationRow.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-background-radius: 5;");
//
//                        Label recObjet = new Label(userReclamation.getObjet());
//                        recObjet.setPrefWidth(150);
//
//                        Label recStatus = new Label(userReclamation.getStatus());
//                        recStatus.setStyle("-fx-background-color: " + getStatusColor(userReclamation.getStatus())
//                                + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
//                        recStatus.setPrefWidth(100);
//
//                        Label recDate = new Label(userReclamation.getDate_soumission().toString());
//                        recDate.setPrefWidth(100);
//
//                        Label recDescription = new Label(userReclamation.getDescription());
//                        recDescription.setPrefWidth(200);
//                        recDescription.setWrapText(true);
//
//                        // Action buttons for each reclamation
//                        ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/img/voir.png")));
//                        iconView.setFitWidth(16);
//                        iconView.setFitHeight(16);
//                        Button btnView = new Button("", iconView);
//                        btnView.setStyle("-fx-background-color: #3B82F6; -fx-background-radius: 5;");
//                        btnView.setOnAction(e -> openViewReclamation(userReclamation));
//
//                        ImageView iconRepondre = new ImageView(new Image(getClass().getResourceAsStream("/img/repondre.png")));
//                        iconRepondre.setFitWidth(16);
//                        iconRepondre.setFitHeight(16);
//                        Button btnRepondre = new Button("", iconRepondre);
//                        btnRepondre.setStyle("-fx-background-color: #10B981; -fx-background-radius: 5;");
//                        btnRepondre.setOnAction(e -> openRepondreReclamation(userReclamation));
//
//                        ImageView iconDelete = new ImageView(new Image(getClass().getResourceAsStream("/img/supprimer.png")));
//                        iconDelete.setFitWidth(16);
//                        iconDelete.setFitHeight(16);
//                        Button btnDelete = new Button("", iconDelete);
//                        btnDelete.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 5;");
//                        btnDelete.setOnAction(e -> {
//                            // Boîte de confirmation avant suppression
//                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
//                            confirm.setTitle("Confirmation de suppression");
//                            confirm.setHeaderText("Êtes-vous sûr(e) de vouloir supprimer cette réclamation ?");
//                            confirm.setContentText("Cette action est irréversible.");
//
//                            Optional<ButtonType> result = confirm.showAndWait();
//                            if (result.isPresent() && result.get() == ButtonType.OK) {
//                                // Suppression
//                                service.delete(userReclamation.getId());
//
//                                // Message d'information
//                                Alert info = new Alert(Alert.AlertType.INFORMATION);
//                                info.setTitle("Suppression réussie");
//                                info.setHeaderText(null);
//                                info.setContentText("La réclamation a été supprimée avec succès !");
//                                info.show();
//
//                                // Mise à jour du PieChart et des données
//                                loadStatsFromAPI();
//                                loadData(); // ou refreshTable(), selon le nom de ta méthode
//                            }
//                        });
//
//                        HBox actions = new HBox(10, btnView, btnRepondre, btnDelete);
//                        actions.setPrefWidth(120);
//
//                        reclamationRow.getChildren().addAll(recObjet, recStatus, recDate, recDescription, actions);
//                        dropdownContent.getChildren().add(reclamationRow);
//                    }
//
//                    // Toggle dropdown visibility
//                    countButton.setOnAction(e -> {
//                        dropdownContent.setVisible(!dropdownContent.isVisible());
//                        dropdownContent.setManaged(dropdownContent.isVisible());
//                    });
//
//                    VBox mainContainer = new VBox(0);
//                    mainContainer.getChildren().addAll(mainRow, dropdownContent);
//                    setGraphic(mainContainer);
//                }
//            }
//        });
//
//        // Add only one reclamation per user to the list
//        reclamationsListView.setItems(FXCollections.observableArrayList(
//                reclamationsByUser.values().stream()
//                        .map(list -> list.get(0))
//                        .collect(Collectors.toList())
//        ));
//    }

    private void loadData(List<Reclamation> reclamations) {
        Map<String, List<Reclamation>> reclamationsByUser = reclamations.stream()
                .collect(Collectors.groupingBy(Reclamation::getUser_email));

        ObservableList<Reclamation> displayList = FXCollections.observableArrayList();

        reclamationsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation r, boolean empty) {
                super.updateItem(r, empty);

                if (empty || r == null) {
                    setGraphic(null);
                } else {
                    String userEmail = r.getUser_email();
                    List<Reclamation> userReclamations = reclamationsByUser.get(userEmail);

                    if (userReclamations == null || userReclamations.isEmpty()) {
                        setGraphic(null);
                        return;
                    }

                    int reclamationCount = userReclamations.size();

                    Label email = new Label(userEmail);
                    email.setPrefWidth(150);

                    Label date = new Label(r.getDate_soumission().toString());
                    date.setPrefWidth(100);

                    Button countButton = new Button(reclamationCount + " réclamation(s)");
                    countButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 5;");
                    countButton.setPrefWidth(120);

                    HBox mainRow = new HBox(15, email, date, countButton);
                    mainRow.setStyle("-fx-padding: 10; -fx-background-color: #fff; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
                    mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    VBox dropdownContent = new VBox(5);
                    dropdownContent.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 10; -fx-background-radius: 5;");
                    dropdownContent.setVisible(false);
                    dropdownContent.setManaged(false);

                    HBox reclamationHeader = new HBox(10);
                    reclamationHeader.setStyle("-fx-background-color: #e5e7eb; -fx-padding: 5; -fx-background-radius: 5;");
                    Label recObjetHeader = new Label("Objet");
                    recObjetHeader.setPrefWidth(150);
                    Label recStatusHeader = new Label("Statut");
                    recStatusHeader.setPrefWidth(100);
                    Label recDateHeader = new Label("Date");
                    recDateHeader.setPrefWidth(100);
                    Label recDescHeader = new Label("Description");
                    recDescHeader.setPrefWidth(200);
                    Label recActionsHeader = new Label("Actions");
                    recActionsHeader.setPrefWidth(120);
                    reclamationHeader.getChildren().addAll(recObjetHeader, recStatusHeader, recDateHeader, recDescHeader, recActionsHeader);
                    dropdownContent.getChildren().add(reclamationHeader);

                    for (Reclamation userReclamation : userReclamations) {
                        HBox reclamationRow = new HBox(10);
                        reclamationRow.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-background-radius: 5;");

                        Label recObjet = new Label(userReclamation.getObjet());
                        recObjet.setPrefWidth(150);

                        Label recStatus = new Label(userReclamation.getStatus());
                        recStatus.setStyle("-fx-background-color: " + getStatusColor(userReclamation.getStatus())
                                + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
                        recStatus.setPrefWidth(100);

                        Label recDate = new Label(userReclamation.getDate_soumission().toString());
                        recDate.setPrefWidth(100);

                        Label recDescription = new Label(userReclamation.getDescription());
                        recDescription.setPrefWidth(200);
                        recDescription.setWrapText(true);

                        ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/img/voir.png")));
                        iconView.setFitWidth(16);
                        iconView.setFitHeight(16);
                        Button btnView = new Button("", iconView);
                        btnView.setStyle("-fx-background-color: #3B82F6; -fx-background-radius: 5;");
                        btnView.setOnAction(e -> openViewReclamation(userReclamation));

                        ImageView iconRepondre = new ImageView(new Image(getClass().getResourceAsStream("/img/repondre.png")));
                        iconRepondre.setFitWidth(16);
                        iconRepondre.setFitHeight(16);
                        Button btnRepondre = new Button("", iconRepondre);
                        btnRepondre.setStyle("-fx-background-color: #10B981; -fx-background-radius: 5;");
                        btnRepondre.setOnAction(e -> openRepondreReclamation(userReclamation));

                        ImageView iconDelete = new ImageView(new Image(getClass().getResourceAsStream("/img/supprimer.png")));
                        iconDelete.setFitWidth(16);
                        iconDelete.setFitHeight(16);
                        Button btnDelete = new Button("", iconDelete);
                        btnDelete.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 5;");
                        btnDelete.setOnAction(e -> {

                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle("Confirmation de suppression");
                            confirm.setHeaderText("Êtes-vous sûr(e) de vouloir supprimer cette réclamation ?");
                            confirm.setContentText("Cette action est irréversible.");

                            Optional<ButtonType> result = confirm.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                service.delete(userReclamation.getId());
                                Alert info = new Alert(Alert.AlertType.INFORMATION);
                                info.setTitle("Suppression réussie");
                                info.setHeaderText(null);
                                info.setContentText("La réclamation a été supprimée avec succès !");
                                info.show();
                                loadStatsFromAPI();
                                loadData(service.afficher()); // Recharger les données depuis la base
                            }
                        });

                        HBox actions = new HBox(10, btnView, btnRepondre, btnDelete);
                        actions.setPrefWidth(120);

                        reclamationRow.getChildren().addAll(recObjet, recStatus, recDate, recDescription, actions);
                        dropdownContent.getChildren().add(reclamationRow);
                    }

                    countButton.setOnAction(e -> {
                        dropdownContent.setVisible(!dropdownContent.isVisible());
                        dropdownContent.setManaged(dropdownContent.isVisible());
                    });

                    VBox mainContainer = new VBox(0);
                    mainContainer.getChildren().addAll(mainRow, dropdownContent);
                    setGraphic(mainContainer);
                }
            }
        });

        // Affiche une seule réclamation par utilisateur
        reclamationsListView.setItems(FXCollections.observableArrayList(
                reclamationsByUser.values().stream()
                        .map(list -> list.get(0))
                        .collect(Collectors.toList())
        ));
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




    // Navigation
    @FXML private void handleDashboard() { loadView("/view/dashboard.fxml"); }
    @FXML private void handleManagement() { loadView("/view/management.fxml"); }
    @FXML private void handleUser() { loadView("/view/utilisateur.fxml"); }
    @FXML private void handleDemandes() { loadView("/view/demandes.fxml"); }
    @FXML private void handleClasses() { loadView("/view/classes.fxml"); }
    @FXML private void handleReclamations() { loadView("/view/reclamation_dashboard.fxml"); }

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


}
