package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Reclamation;
import model.ReponseReclamation;
import services.ReclamationServices;
import services.ReponseReclamationService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class voirRepRecController implements Initializable {

    @FXML private Label objetLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusLabel;
    @FXML private VBox reponsesContainer;
    @FXML private CheckBox resolueCheckBox;


    private Reclamation reclamation;
    private ReponseReclamationService reponseService = new ReponseReclamationService();

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        
        // Mark responses as read for this specific reclamation
        reponseService.markResponsesAsRead(reclamation.getUser_email(), reclamation.getId());
        
        objetLabel.setText("Objet : " + reclamation.getObjet());
        descriptionLabel.setText("Description : " + reclamation.getDescription());
        statusLabel.setText("Statut : " + reclamation.getStatus());

        // Add rating display
        if (reclamation.getRating() > 0) {
            Label ratingLabel = new Label("Note : " + getRatingStars(reclamation.getRating()));
            ratingLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px;");
            reponsesContainer.getChildren().add(0, ratingLabel);
        }

        resolueCheckBox.setSelected("Résolue".equalsIgnoreCase(reclamation.getStatus()));

// Empêcher l'exécution du listener lors de l'initialisation
        resolueCheckBox.setOnAction(event -> {
            if (resolueCheckBox.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Vous êtes sur le point de marquer cette réclamation comme résolue.");
                alert.setContentText("Êtes-vous sûr de vouloir continuer ?");

                ButtonType oui = new ButtonType("Oui");
                ButtonType non = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(oui, non);

                alert.showAndWait().ifPresent(response -> {
                    if (response == oui) {
                        // ✅ Mettre à jour le statut
                        reclamation.setStatus("Résolue");
                        reclamation.setDate_resolution(new java.sql.Date(System.currentTimeMillis()));
                        statusLabel.setText("Statut : Résolue");

                        // Appel du service pour enregistrer le changement
                        new services.ReclamationServices().update(reclamation);

                        // Show rating window
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/rating.fxml"));
                            Parent root = loader.load();
                            RatingController controller = loader.getController();
                            controller.setReclamation(reclamation);
                            
                            Stage stage = new Stage();
                            stage.setTitle("Évaluation");
                            stage.setScene(new Scene(root));

                            controller.setDialogStage(stage);

                            stage.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        resolueCheckBox.setSelected(false);
                    }
                });
            }
        });


        afficherReponses();
    }

    private String getRatingStars(int rating) {
        if (rating == 0) return "Non évalué";
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("★");
        }
        for (int i = rating; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString() + " (" + rating + "/5)";
    }

    private void afficherReponses() {
        reponsesContainer.getChildren().clear();

        List<ReponseReclamation> reponses = new ReponseReclamationService().getReponsesByReclamationId(reclamation.getId());

        for (ReponseReclamation rep : reponses) {
            VBox box = new VBox(5);
            box.setStyle("-fx-background-color: #ffffff; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5;");

            Label admin = new Label("Admin: " + rep.getAdmin().getEmail());
            Label contenu = new Label("Réponse : " + rep.getContenue());
            Label date = new Label("Posté le " + rep.getDateReponse().toString());

            Button btnRepondre = new Button("Répondre");
            btnRepondre.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5;");
            btnRepondre.setOnAction(e -> openDiscussion(rep));

            HBox footer = new HBox(btnRepondre);
            footer.setSpacing(10);

            box.getChildren().addAll(admin, contenu, date, footer);
            reponsesContainer.getChildren().add(box);
        }
    }

    private void openDiscussion(ReponseReclamation rep) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/repondre_a_admin.fxml"));
            Parent root = loader.load();

            controllers.RepondreAAdminController controller = loader.getController();
            controller.setReclamationAndReponse(reclamation, rep);

            Stage stage = new Stage();
            stage.setTitle("Discussion sur la réponse");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MesReclamations.fxml"));
            Parent root = loader.load();

            // Remplacer le contenu de la scène actuelle
            Stage currentStage = (Stage) reponsesContainer.getScene().getWindow();
            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Mes Réclamations");
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Rien à initialiser
    }

    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Réclamation");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMesReclamations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MesReclamations.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Mes Réclamations");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Impossible de charger Mes Réclamations.");
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
