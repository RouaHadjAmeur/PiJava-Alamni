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
import javafx.application.Platform;

public class ReclamationDashboardController {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    private final ReclamationServices service = new ReclamationServices();

    @FXML
    public void initialize() {
        System.out.println("Initializing ReclamationDashboardController");
        setupHeader();
        loadReclamations();
    }

    private void setupHeader() {
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

        header.getColumnConstraints().addAll(
            new javafx.scene.layout.ColumnConstraints(150),
            new javafx.scene.layout.ColumnConstraints(100),
            new javafx.scene.layout.ColumnConstraints(100),
            new javafx.scene.layout.ColumnConstraints(200),
            new javafx.scene.layout.ColumnConstraints(100),
            new javafx.scene.layout.ColumnConstraints(150)
        );

        reclamationsListView.setStyle("-fx-background-color: white;");
    }

    private void loadReclamations() {
        System.out.println("Loading reclamations...");
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

                    Label objet = new Label(r.getObjet());
                    Label statut = new Label("Résolue");
                    statut.setStyle("-fx-text-fill: white; -fx-background-color: #4CAF50; -fx-padding: 2 5; -fx-background-radius: 3;");
                    Label date = new Label(r.getDate_soumission().toString());
                    Label desc = new Label(r.getDescription());
                    
                    Label rating = new Label();
                    int ratingValue = r.getRating();
                    System.out.println("Processing rating for ID " + r.getId() + ": " + ratingValue);
                    
                    updateRatingLabel(rating, ratingValue);
                    
                    HBox actions = new HBox(5);
                    Button viewBtn = new Button("👁");
                    viewBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
                    Button replyBtn = new Button("↩");
                    replyBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
                    Button deleteBtn = new Button("🗑");
                    deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                    
                    actions.getChildren().addAll(viewBtn, replyBtn, deleteBtn);

                    grid.add(objet, 0, 0);
                    grid.add(statut, 1, 0);
                    grid.add(date, 2, 0);
                    grid.add(desc, 3, 0);
                    grid.add(rating, 4, 0);
                    grid.add(actions, 5, 0);

                    grid.getColumnConstraints().addAll(
                        new javafx.scene.layout.ColumnConstraints(150),
                        new javafx.scene.layout.ColumnConstraints(100),
                        new javafx.scene.layout.ColumnConstraints(100),
                        new javafx.scene.layout.ColumnConstraints(200),
                        new javafx.scene.layout.ColumnConstraints(100),
                        new javafx.scene.layout.ColumnConstraints(150)
                    );

                    setGraphic(grid);
                }
            }
        });
    }

    private String getRatingDisplay(int rating) {
        if (rating == -1) {
            Label noReview = new Label("No review");
            noReview.setStyle("-fx-text-fill: gray;");
            return noReview.getText();
        } else if (rating == 0) {
            Label notRated = new Label("Not rated");
            notRated.setStyle("-fx-text-fill: gray;");
            return notRated.getText();
        } else {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < rating; i++) {
                stars.append("★"); // Filled star
            }
            for (int i = rating; i < 5; i++) {
                stars.append("☆"); // Empty star
            }
            Label ratingLabel = new Label(stars.toString());
            ratingLabel.setStyle("-fx-text-fill: gold;");
            return ratingLabel.getText();
        }
    }

    private void updateRatingLabel(Label ratingLabel, int rating) {
        ratingLabel.setText(getRatingDisplay(rating));
        if (rating <= 0) {
            ratingLabel.setStyle("-fx-text-fill: gray;");
        } else {
            ratingLabel.setStyle("-fx-text-fill: gold;");
        }
    }

    public void refreshList() {
        System.out.println("Refreshing reclamation list");
        Platform.runLater(() -> {
            loadReclamations();
            reclamationsListView.refresh();
        });
    }
}
