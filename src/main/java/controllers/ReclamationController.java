package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import model.Reclamation;
import services.ReclamationServices;

public class ReclamationController {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    private final ReclamationServices service = new ReclamationServices();

    public ReclamationController() {
        Main.DatabaseConnection.getInstance();
    }

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
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
                    Label email = new Label(r.getUser_email());
                    email.setPrefWidth(160);

                    Label objet = new Label(r.getObjet());
                    objet.setPrefWidth(160);

                    Label description = new Label(r.getDescription());
                    description.setPrefWidth(160);

                    Label date = new Label(r.getDate_soumission().toString());
                    date.setPrefWidth(100);

                    Label status = new Label(r.getStatus());
                    status.setStyle("-fx-background-color: " + getStatusColor(r.getStatus()) +
                            "; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 10;");
                    status.setPrefWidth(100);
                    status.setAlignment(Pos.CENTER);

                    // Icons
                    Button btnView = createIconButton("/img/voir.png", "#3B82F6");
                    btnView.setOnAction(e -> openViewReclamation(r));

                    Button btnRepondre = createIconButton("/img/repondre.png", "#10B981");
                    btnRepondre.setOnAction(e -> openRepondreReclamation(r));

                    Button btnDelete = createIconButton("/img/supprimer.png", "#EF4444");
                    btnDelete.setOnAction(e -> {
                        service.delete(r.getId());
                        loadData();
                    });

                    HBox actions = new HBox(10, btnView, btnRepondre, btnDelete);
                    actions.setPrefWidth(160);
                    actions.setAlignment(Pos.CENTER_LEFT);

                    HBox row = new HBox(20, email, objet, description, status, date, actions);
                    row.setStyle("-fx-padding: 10; -fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
                    row.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(row);
                }
            }
        });
    }

    private Button createIconButton(String imgPath, String bgColor) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imgPath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);

        Button btn = new Button("", icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 5;");
        return btn;
    }

    private void openViewReclamation(Reclamation r) {
        try {
            if ("En attente".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("En cours");
                service.update(r);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reclamation_view.fxml"));
            Parent root = loader.load();

            ReclamationDetailsController controller = loader.getController();
            controller.setReclamation(r);

            Stage stage = new Stage();
            stage.setTitle("Détails de la Réclamation");
            stage.setScene(new Scene(root));
            stage.show();

            stage.setOnHiding(event -> loadData());

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur lors de l'ouverture de la réclamation.");
        }
    }

    private void openRepondreReclamation(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/repondre_reclamation.fxml"));
            Parent root = loader.load();

            RepondreReclamationController controller = loader.getController();
            controller.setReclamation(r);

            Stage stage = new Stage();
            stage.setTitle("Répondre à la Réclamation");
            stage.setScene(new Scene(root));
            stage.show();

            stage.setOnHiding(event -> loadData());

            if (r.getStatus().equalsIgnoreCase("En attente") || r.getStatus().equalsIgnoreCase("En cours")) {
                r.setStatus("En cours");
                service.update(r);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur lors de l'ouverture de la fenêtre de réponse.");
        }
    }

    private String getStatusColor(String status) {
        return switch (status.toLowerCase()) {
            case "en attente" -> "#f59e0b";
            case "en cours" -> "#3b82f6";
            case "résolue" -> "#10b981";
            default -> "#6b7280";
        };
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
