package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Reclamation;
import services.ReclamationServices;

public class RatingController {
    @FXML private HBox starsContainer;
    @FXML private Button submitButton;
    @FXML private Label ratingLabel;

    private Reclamation reclamation;
    private final ReclamationServices reclamationService = new ReclamationServices();
    private int currentRating = 0;

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        initializeStars();
    }

    @FXML
    private void initialize() {
        submitButton.setOnAction(event -> handleSubmit());
    }

    private void initializeStars() {
        starsContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 30px; -fx-cursor: hand;");
            final int rating = i;
            
            star.setOnMouseEntered(e -> {
                updateStars(rating);
            });
            
            star.setOnMouseExited(e -> {
                updateStars(currentRating);
            });
            
            star.setOnMouseClicked(e -> {
                currentRating = rating;
                updateStars(currentRating);
            });
            
            starsContainer.getChildren().add(star);
        }
    }

    private void updateStars(int rating) {
        for (int i = 0; i < starsContainer.getChildren().size(); i++) {
            Label star = (Label) starsContainer.getChildren().get(i);
            star.setText(i < rating ? "★" : "☆");
            star.setStyle("-fx-font-size: 30px; -fx-cursor: hand; -fx-text-fill: " + 
                         (i < rating ? "#FFD700" : "#000000"));
        }
        ratingLabel.setText(rating > 0 ? "Note: " + rating + "/5" : "Sélectionnez une note");
    }

    private void handleSubmit() {
        if (currentRating > 0) {
            reclamation.setRating(currentRating);
            reclamationService.updateRating(reclamation);
            ((Stage) submitButton.getScene().getWindow()).close();
        }
    }
} 