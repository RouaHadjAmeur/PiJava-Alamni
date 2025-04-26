package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reclamation;
import services.ReclamationServices;
import com.ratingwidget.RatingWidget;

public class RatingController {
    @FXML private RatingWidget ratingWidget;
    @FXML private Button submitButton;
    @FXML private Button noThanksButton;
    @FXML private Label ratingLabel;
    @FXML private VBox mainContainer;

    private Reclamation reclamation;
    private final ReclamationServices reclamationService = new ReclamationServices();
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Toujours afficher les étoiles cliquables pour permettre la notation
        ratingWidget.setRating(0);
        // Listener pour les changements de note
        ratingWidget.ratingProperty().addListener((observable, oldValue, newValue) -> {
            submitButton.setDisable(newValue.intValue() < 1);
            updateRatingLabel(newValue.intValue());
        });
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        if (reclamation.getRating() > 0) {
            ratingWidget.setRating(reclamation.getRating());
        }
    }

    private void updateRatingLabel(int rating) {
        if (rating <= 0) {
            ratingLabel.setText("Sélectionnez une note");
            ratingLabel.setStyle("-fx-text-fill: #4B5563;");
        } else {
            ratingLabel.setText("Note: " + rating + "/5");
            ratingLabel.setStyle("-fx-text-fill: #4B5563;");
        }
    }

    @FXML
    private void handleSubmit() {
        int rating = ratingWidget.getRating();
        if (rating > 0) {
            reclamation.setRating(rating);
            reclamationService.updateRating(reclamation);
            System.out.println("Rating updated to " + rating + " for reclamation ID: " + reclamation.getId());
            closeWindow();
        }
    }

    @FXML
    private void handleNoThanks() {
        reclamation.setRating(-1); // -1 signifie "No Thanks"
        reclamationService.updateRating(reclamation);
        System.out.println("Rating updated to -1 for reclamation ID: " + reclamation.getId());
        closeWindow();
    }

    private void closeWindow() {
        dialogStage.close();
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }
}
