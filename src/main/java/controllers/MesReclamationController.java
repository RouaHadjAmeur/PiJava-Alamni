package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reclamation;
import model.ReponseReclamation;
import services.ReclamationServices;
import services.ReponseReclamationService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MesReclamationController implements Initializable {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    private final ReclamationServices service = new ReclamationServices();
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        afficherMesReclamations();
    }

    private void afficherMesReclamations() {
        ObservableList<Reclamation> list = FXCollections.observableArrayList(service.getMesReclamations());
        reclamationsListView.setItems(list);

        reclamationsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation r, boolean empty) {
                super.updateItem(r, empty);

                if (empty || r == null) {
                    setGraphic(null);
                } else {
                    VBox card = new VBox(5);
                    card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 15;");

                    Label objet = new Label("Objet : " + r.getObjet());
                    objet.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

                    Label description = new Label("Description : " + r.getDescription());
                    description.setWrapText(true);

                    Label date = new Label("Date : " + r.getDate_soumission());
                    date.setStyle("-fx-text-fill: #6b7280;");

                    Label status = new Label("Statut : " + r.getStatus());
                    status.setStyle("-fx-background-color: " + getStatusColor(r.getStatus()) + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 5;");

                    Button btnReponse = new Button("Voir Réponse");
                    btnReponse.setStyle("-fx-background-color: #ff5722; -fx-text-fill: white; -fx-background-radius: 5;");
                    btnReponse.setOnAction(e -> openReponse(r));

                    card.getChildren().addAll(objet, description, date, status, btnReponse);
                    setGraphic(card);
                }
            }
        });
    }



//    private void openReponse(Reclamation r) {
//        List<ReponseReclamation> reponses = new ReponseReclamationService().getReponsesByReclamationId(r.getId());
//
//        if (reponses.isEmpty()) {
//            showAlert(Alert.AlertType.INFORMATION,"Aucune réponse trouvée pour cette réclamation.");
//        } else {
//            StringBuilder content = new StringBuilder();
//            for (ReponseReclamation rep : reponses) {
//                content.append("- ").append(rep.getContenue()).append("\n\n");
//            }
//
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Réponses");
//            alert.setHeaderText("Réponses à votre réclamation :");
//            alert.setContentText(content.toString());
//            alert.showAndWait();
//        }
//    }

    private void openReponse(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/voirRepRec.fxml"));
            Parent root = loader.load();

            voirRepRecController controller = loader.getController();
            controller.setReclamation(r);

            Stage stage = new Stage();
            stage.setTitle("Réponses de la réclamation");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
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

            // Récupérer le Stage courant proprement
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

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

    private String getStatusColor(String status) {
        return switch (status.toLowerCase()) {
            case "en attente" -> "#f59e0b";
            case "en cours" -> "#3b82f6";
            case "résolue" -> "#10b981";
            default -> "#6b7280";
        };
    }

    @FXML
    private void filterByStatus(ActionEvent event) {
        // Option 1 : Si tu n'as pas encore implémenté le filtre :
        System.out.println("Filtrer les réclamations selon le status...");

        // Option 2 : Si tu veux afficher un message temporaire :
        showAlert(Alert.AlertType.INFORMATION, "Filtre en cours de développement.");
    }



}
