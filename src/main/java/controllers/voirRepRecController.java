package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reclamation;
import model.ReponseReclamation;
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

    private Reclamation reclamation;

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;

        objetLabel.setText("Objet : " + reclamation.getObjet());
        descriptionLabel.setText("Description : " + reclamation.getDescription());
        statusLabel.setText("Status : " + reclamation.getStatus());

        List<ReponseReclamation> reponses = new ReponseReclamationService().getReponsesByReclamationId(reclamation.getId());

        for (ReponseReclamation rep : reponses) {
            VBox box = new VBox(5);
            box.setStyle("-fx-background-color: #ffffff; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5;");

            Label admin = new Label("Admin: " + rep.getAdmin().getEmail());
            Label contenu = new Label("Réponse :" + rep.getContenue());
            Label date = new Label("Posté le " + rep.getDateReponse().toString());

            box.getChildren().addAll(admin, contenu, date);
            reponsesContainer.getChildren().add(box);
        }
    }


    @FXML
    private void handleRetour() {
        ((Stage) reponsesContainer.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_reclamation_view.fxml"));
            Parent root = loader.load();

            controllers.AddReclamationController controller = loader.getController();

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