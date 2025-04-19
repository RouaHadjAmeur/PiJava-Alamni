package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ReclamationController {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    @FXML private ImageView photoView;
    @FXML private Label nomUtilisateur;
    @FXML private Label roleUtilisateur;
    @FXML private BorderPane rootPane;
    @FXML private AnchorPane mainContentPane;

    @FXML
    private TextField emailSearchField;
    @FXML
    private TextField objetSearchField;

    private final ReclamationServices reclamationService = new ReclamationServices();


    private final ReclamationServices service = new ReclamationServices();

    public ReclamationController() {
        Main.DatabaseConnection.getInstance();
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


            emailSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                List<Reclamation> filtrées = reclamationService.rechercherParEmail(newVal);
                reclamationsListView.setItems(FXCollections.observableArrayList(filtrées));
        });

            objetSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                List<Reclamation> filtrées = reclamationService.rechercherParObjet(newVal);
                reclamationsListView.setItems(FXCollections.observableArrayList(filtrées));
            });

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
                    setGraphic(null);
                } else {
                    Label email = new Label(r.getUser_email());
                    email.setPrefWidth(150);

                    Label objet = new Label(r.getObjet());
                    objet.setPrefWidth(150);

                    Label description = new Label(r.getDescription());
                    description.setPrefWidth(200);

                    Label status = new Label(r.getStatus());
                    status.setPrefWidth(100);
                    status.setStyle("-fx-background-color: " + getStatusColor(r.getStatus())
                            + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
                    status.setAlignment(javafx.geometry.Pos.CENTER);

                    Label date = new Label(r.getDate_soumission().toString());
                    date.setPrefWidth(100);

                    ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/img/voir.png")));
                    iconView.setFitWidth(16);
                    iconView.setFitHeight(16);
                    Button btnView = new Button("", iconView);
                    btnView.setStyle("-fx-background-color: #3B82F6; -fx-background-radius: 5;");

                    //Button btnView = createActionButton("Voir", "#3B82F6");
                    btnView.setOnAction(e -> openViewReclamation(r));


                    ImageView iconRepondre = new ImageView(new Image(getClass().getResourceAsStream("/img/repondre.png")));
                    iconRepondre.setFitWidth(16);
                    iconRepondre.setFitHeight(16);
                    Button btnRepondre = new Button("", iconRepondre);
                    btnRepondre.setStyle("-fx-background-color: #10B981; -fx-background-radius: 5;");
                    //Button btnRepondre = createActionButton("Répondre", "#10B981");
                    btnRepondre.setOnAction(e -> openRepondreReclamation(r));

                    ImageView iconDelete = new ImageView(new Image(getClass().getResourceAsStream("/img/supprimer.png")));
                    iconDelete.setFitWidth(16);
                    iconDelete.setFitHeight(16);
                    Button btnDelete = new Button("", iconDelete);
                    btnDelete.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 5;");

                    //Button btnDelete = createActionButton("Supprimer", "#EF4444");
                    btnDelete.setOnAction(e -> {
                        service.delete(r.getId());
                        loadData();
                    });

                    HBox actions = new HBox(10, btnView, btnRepondre, btnDelete);

                    HBox row = new HBox(15, email, objet, description, status, date, actions);
                    row.setStyle("-fx-padding: 10; -fx-background-color: #fff; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    setGraphic(row);
                }
            }
        });
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

            // Récupère la fenêtre à partir d’un composant quelconque
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.centerOnScreen(); // centrer proprement
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
