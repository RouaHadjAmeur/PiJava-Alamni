package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import model.Reclamation;
import services.ReclamationServices;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class ReclamationDashboardController {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    private final ReclamationServices service = new ReclamationServices();

    @FXML
    public void initialize() {
        setupHeader();
        loadReclamations();
    }

    private void setupHeader() {
        // Create header
        GridPane header = new GridPane();
        header.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5px;");
        header.setHgap(10);

        Label objetHeader = new Label("Objet");
        Label statutHeader = new Label("Statut");
        Label dateHeader = new Label("Date");
        Label descriptionHeader = new Label("Description");
        Label ratingHeader = new Label("Rating");
        Label actionsHeader = new Label("Actions");

        header.add(objetHeader, 0, 0);
        header.add(statutHeader, 1, 0);
        header.add(dateHeader, 2, 0);
        header.add(descriptionHeader, 3, 0);
        header.add(ratingHeader, 4, 0);
        header.add(actionsHeader, 5, 0);

        // Set column constraints
        header.getColumnConstraints().addAll(
            new javafx.scene.layout.ColumnConstraints(150), // Objet
            new javafx.scene.layout.ColumnConstraints(100), // Statut
            new javafx.scene.layout.ColumnConstraints(100), // Date
            new javafx.scene.layout.ColumnConstraints(200), // Description
            new javafx.scene.layout.ColumnConstraints(100), // Rating
            new javafx.scene.layout.ColumnConstraints(150)  // Actions
        );

        reclamationsListView.setStyle("-fx-background-color: white;");
    }

    private void loadReclamations() {
        ObservableList<Reclamation> list = FXCollections.observableArrayList(service.afficher());
        reclamationsListView.setItems(list);

        reclamationsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setPadding(new Insets(5));

                    // Objet
                    Label objet = new Label(r.getObjet());
                    
                    // Statut
                    Label statut = new Label("Résolue");
                    statut.setStyle("-fx-text-fill: white; -fx-background-color: #4CAF50; -fx-padding: 2 5; -fx-background-radius: 3;");
                    
                    // Date
                    Label date = new Label(r.getDate_soumission().toString());
                    
                    // Description
                    Label desc = new Label(r.getDescription());
                    
                    // Rating
                    Label rating = new Label(getRatingStars(r.getRating()));
                    rating.setStyle("-fx-text-fill: #FFD700;"); // Gold color for stars
                    
                    // Actions
                    HBox actions = new HBox(5);
                    Button viewBtn = new Button();
                    viewBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
                    viewBtn.setText("👁");
                    
                    Button replyBtn = new Button();
                    replyBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
                    replyBtn.setText("↩");
                    
                    Button deleteBtn = new Button();
                    deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                    deleteBtn.setText("🗑");
                    
                    actions.getChildren().addAll(viewBtn, replyBtn, deleteBtn);

                    // Add all elements to grid
                    grid.add(objet, 0, 0);
                    grid.add(statut, 1, 0);
                    grid.add(date, 2, 0);
                    grid.add(desc, 3, 0);
                    grid.add(rating, 4, 0);
                    grid.add(actions, 5, 0);

                    // Set column constraints
                    grid.getColumnConstraints().addAll(
                        new javafx.scene.layout.ColumnConstraints(150), // Objet
                        new javafx.scene.layout.ColumnConstraints(100), // Statut
                        new javafx.scene.layout.ColumnConstraints(100), // Date
                        new javafx.scene.layout.ColumnConstraints(200), // Description
                        new javafx.scene.layout.ColumnConstraints(100), // Rating
                        new javafx.scene.layout.ColumnConstraints(150)  // Actions
                    );

                    setGraphic(grid);
                }
            }
        });
    }

    private String getRatingStars(int rating) {
        if (rating == 0) return "☆☆☆☆☆";
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("★");
        }
        for (int i = rating; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }
}
