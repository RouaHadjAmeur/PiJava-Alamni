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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.Reclamation;
import model.Utilisateur;
import services.ReclamationServices;
import services.ReponseReclamationService;
import util.Session;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.scene.input.MouseEvent;

public class MesReclamationController implements Initializable {
    @FXML private Label alamniLogo;
    @FXML private ListView<Reclamation> reclamationsListView;
    @FXML private MenuButton userMenu;
    @FXML private ImageView profileImage;
    @FXML private Label statusLabel;

    private final ReclamationServices service = new ReclamationServices();
    private final ReponseReclamationService reponseService = new ReponseReclamationService();
    private Utilisateur user;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        user = Session.getUtilisateurConnecte();

        if (user != null) {
            userMenu.setText(user.getPrenom() + " " + user.getNom());

            if (user.getPhoto() != null) {
                File file = new File(user.getPhoto());
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 40, 40, true, true);
                    profileImage.setImage(image);
                }
            }

            afficherMesReclamations(user.getEmail());
        }
    }

    @FXML
    private void handleAlamniClick(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/parent.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Espace Parent");
            stage.setResizable(false);
            Stage currentStage = (Stage) alamniLogo.getScene().getWindow();
            currentStage.close();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherMesReclamations(String userEmail) {
        ObservableList<Reclamation> list = FXCollections.observableArrayList(service.getMesReclamations());
        Map<Integer, Integer> unreadResponseCounts = reponseService.getUnreadCountPerReclamation(userEmail);

        reclamationsListView.setItems(list);
        reclamationsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation r, boolean empty) {
                super.updateItem(r, empty);

                if (empty || r == null) {
                    setGraphic(null);
                } else {
                    VBox card = new VBox(10);
                    card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 15;");

                    Label objet = new Label("Objet : " + r.getObjet());
                    objet.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

                    Label description = new Label("Description : " + r.getDescription());
                    description.setWrapText(true);

                    Label date = new Label("Date : " + r.getDate_soumission());
                    date.setStyle("-fx-text-fill: #6b7280;");

                    Label status = new Label("Statut : " + r.getStatus());
                    status.setStyle("-fx-background-color: " + getStatusColor(r.getStatus()) + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 5;");

                    HBox header = new HBox(10, objet);

                    // ✅ Ajout du badge SI nécessaire
                    Integer count = unreadResponseCounts.getOrDefault(r.getId(), 0);
                    if (count > 0) {
                        Label badge = new Label(String.valueOf(count));
                        badge.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-padding: 2 6; -fx-font-weight: bold; -fx-background-radius: 20;");
                        header.getChildren().add(badge);
                    }

                    Button btnVoirReponses = new Button("Voir Réponses");
                    btnVoirReponses.setStyle("-fx-background-color: #ff5722; -fx-text-fill: white; -fx-background-radius: 5;");
                    btnVoirReponses.setOnAction(e -> openReponse(r));

                    HBox actions = new HBox(10, btnVoirReponses);
                    card.getChildren().addAll(header, description, date, status, actions);
                    setGraphic(card);
                }
            }
        });
    }


    private void openReponse(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/voirRepRec.fxml"));
            Parent root = loader.load();
            voirRepRecController controller = loader.getController();
            controller.setReclamation(reclamation);
            Stage currentStage = (Stage) reclamationsListView.getScene().getWindow();
            Scene newScene = new Scene(root, currentStage.getWidth(), currentStage.getHeight());
            currentStage.setScene(newScene);
            currentStage.setTitle("Réponses de la Réclamation");
            currentStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchScene(String fxmlPath, String windowTitle, Window currentWindow) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene newScene = new Scene(root, 900, 600);
            Stage newStage = new Stage();
            newStage.setScene(newScene);
            newStage.setTitle(windowTitle);
            newStage.setResizable(false);
            if (currentWindow instanceof Stage oldStage) {
                newStage.setX(oldStage.getX());
                newStage.setY(oldStage.getY());
                oldStage.close();
            }
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur lors du chargement de la fenêtre.");
        }
    }

    @FXML
    private void handleAjouterReclamation(ActionEvent event) {
        Window currentWindow;
        if (event.getSource() instanceof MenuItem) {
            currentWindow = ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        } else {
            currentWindow = ((Node) event.getSource()).getScene().getWindow();
        }
        switchScene("/view/add_reclamation_view.fxml", "Ajouter Réclamation", currentWindow);
    }

    @FXML
    private void handleMesReclamations(ActionEvent event) {
        Window currentWindow;
        if (event.getSource() instanceof MenuItem) {
            currentWindow = ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        } else {
            currentWindow = ((Node) event.getSource()).getScene().getWindow();
        }
        switchScene("/view/MesReclamations.fxml", "Mes Réclamations", currentWindow);
    }

    @FXML
    private void handleMonProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/profilUtilisateur.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.centerOnScreen();
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMesBulletins() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Bulletin");
        info.setHeaderText(null);
        info.setContentText("Ici s'afficheront les notes de l'élève suivi.");
        info.showAndWait();
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) userMenu.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Alamni");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void filterByStatus(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Filtre en cours de développement.");
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
}
