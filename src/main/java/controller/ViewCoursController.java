package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import dao.DevoirDAO;
import model.Cours;
import model.Devoir;
import utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ViewCoursController implements Initializable {

    @FXML private Label titreCours;
    @FXML private ImageView imageView;
    @FXML private Label descriptionLabel;
    @FXML private Label matiereLabel;
    @FXML private Label dateLabel;
    @FXML private Label niveauLabel;
    @FXML private Button supportBtn;

    @FXML private TabPane tabPane;
    @FXML private Tab detailsTab;
    @FXML private Tab devoirsTab;

    @FXML private TableView<Devoir> devoirTableView;
    @FXML private TableColumn<Devoir, Integer> idColumn;
    @FXML private TableColumn<Devoir, String> titreColumn;
    @FXML private TableColumn<Devoir, String> descriptionColumn;
    @FXML private TableColumn<Devoir, String> dateColumn;
    @FXML private TableColumn<Devoir, Button> supportColumn;
    @FXML private TableColumn<Devoir, HBox> actionsColumn;

    @FXML private Button addDevoirBtn;
    @FXML private Button closeBtn;

    private Cours cours;
    private DevoirDAO devoirDAO;
    private ObservableList<Devoir> devoirsList;
    private Stage currentStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        devoirDAO = new DevoirDAO();
        devoirsList = FXCollections.observableArrayList();
        // Charger le CSS professionnel
        closeBtn.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                newValue.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            }
        });

        // Configuration des colonnes de la table des devoirs
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre_d"));

        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescr_d();
            // Limiter la longueur de la description pour l'affichage
            if (desc != null && desc.length() > 30) {
                desc = desc.substring(0, 30) + "...";
            }
            return new SimpleStringProperty(desc);
        });

        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_d();
            if (date != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
                return new SimpleStringProperty(date.format(formatter));
            }
            return new SimpleStringProperty("N/A");
        });

        supportColumn.setCellValueFactory(cellData -> {
            Button downloadBtn = new Button("Télécharger");
            downloadBtn.getStyleClass().addAll("action-button", "download-button");

            String supportPath = cellData.getValue().getSupport_d();
            if (supportPath == null || supportPath.isEmpty()) {
                downloadBtn.setDisable(true);
            }

            downloadBtn.setOnAction(event -> {
                // Logique pour télécharger le support
                if (supportPath != null && !supportPath.isEmpty()) {
                    System.out.println("Téléchargement du support: " + supportPath);
                    // Implémentez la logique de téléchargement ici
                }
            });

            return new SimpleObjectProperty<>(downloadBtn);
        });

        // Configuration de la colonne des actions
        setupActionsColumn();

        // Configuration du bouton d'ajout de devoir
        addDevoirBtn.setOnAction(event -> openAddDevoirDialog());

        // Configuration du bouton de fermeture
        closeBtn.setOnAction(event -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });
    }

    public void setCours(Cours cours) {
        this.cours = cours;
        loadCoursDetails();
        loadDevoirsData();
    }

    private void loadCoursDetails() {
        if (cours != null) {
            titreCours.setText(cours.getTitre());
            descriptionLabel.setText(cours.getDescr_c());
            matiereLabel.setText(cours.getMatiere_c());

            if (cours.getDate_c() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                dateLabel.setText(cours.getDate_c().format(formatter));
            } else {
                dateLabel.setText("N/A");
            }

            niveauLabel.setText(cours.getNiveau());

            // Affichage de l'image
            String imagePath = cours.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                } else {
                    // Image par défaut
                    String initial = cours.getTitre() != null && !cours.getTitre().isEmpty() ?
                            cours.getTitre().substring(0, 1).toUpperCase() : "C";
                    imageView.setImage(ImageUtils.createTextImage(120, 120, initial,
                            javafx.scene.paint.Color.LIGHTGRAY, javafx.scene.paint.Color.WHITE));
                }
            } else {
                // Image par défaut
                String initial = cours.getTitre() != null && !cours.getTitre().isEmpty() ?
                        cours.getTitre().substring(0, 1).toUpperCase() : "C";
                imageView.setImage(ImageUtils.createTextImage(120, 120, initial,
                        javafx.scene.paint.Color.LIGHTGRAY, javafx.scene.paint.Color.WHITE));
            }

            // Configuration du bouton de support
            String supportPath = cours.getSupport_c();
            if (supportPath != null && !supportPath.isEmpty()) {
                supportBtn.setDisable(false);
                supportBtn.setOnAction(event -> {
                    System.out.println("Téléchargement du support: " + supportPath);
                    // Logique de téléchargement
                });
            } else {
                supportBtn.setDisable(true);
            }
        }
    }

    private void loadDevoirsData() {
        if (cours != null) {
            List<Devoir> devoirs = devoirDAO.getByCours(cours.getId());
            devoirsList.clear();
            devoirsList.addAll(devoirs);
            devoirTableView.setItems(devoirsList);

            // Afficher un message si aucun devoir n'est disponible
            if (devoirs.isEmpty()) {
                devoirTableView.setPlaceholder(new Label("Aucun devoir n'est disponible pour ce cours"));
            }
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellValueFactory(cellData -> {
            Devoir devoir = cellData.getValue();



            // Bouton Modifier
            Button editBtn = new Button();
            ImageView editIcon = new ImageView(ImageUtils.createEditIcon(16));
            editIcon.setFitHeight(16);
            editIcon.setFitWidth(16);
            editBtn.setGraphic(editIcon);
            editBtn.getStyleClass().addAll("action-button", "edit-button");
            editBtn.setTooltip(new Tooltip("Modifier"));

            editBtn.setOnAction(event -> editDevoir(devoir));

            // Bouton Supprimer
            Button deleteBtn = new Button();
            ImageView deleteIcon = new ImageView(ImageUtils.createDeleteIcon(16));
            deleteIcon.setFitHeight(16);
            deleteIcon.setFitWidth(16);
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.getStyleClass().addAll("action-button", "delete-button");
            deleteBtn.setTooltip(new Tooltip("Supprimer"));

             deleteBtn.setOnAction(event -> deleteDevoir(devoir));

            // Conteneur pour les boutons
            HBox actionsBox = new HBox(5);
            actionsBox.setAlignment(Pos.CENTER);
            actionsBox.getChildren().addAll( editBtn, deleteBtn);

            return new SimpleObjectProperty<>(actionsBox);
        });
    }



    private void editDevoir(Devoir devoir) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit-devoir.fxml"));
            Parent root = loader.load();

            EditDevoirController controller = loader.getController();
            controller.setDevoir(devoir);
            controller.setCoursId(cours.getId());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier le devoir");
            stage.setScene(new Scene(root));

            // Actualiser la liste des devoirs après la fermeture
            stage.setOnHidden(event -> loadDevoirsData());
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de modification du devoir",
                    e.getMessage());
        }
    }

    private void deleteDevoir(Devoir devoir) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir supprimer ce devoir ?");
        confirmDialog.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = devoirDAO.supprimer(devoir.getId());
            if (success) {
                loadDevoirsData(); // Recharger la liste des devoirs
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Devoir supprimé",
                        "Le devoir a été supprimé avec succès.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression",
                        "La suppression du devoir a échoué.");
            }
        }
    }

    private void openAddDevoirDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-devoir.fxml"));
            Parent root = loader.load();

            AddDevoirController controller = loader.getController();
            controller.setCoursId(cours.getId());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter un devoir");
            stage.setScene(new Scene(root));

            // Actualiser la liste des devoirs après la fermeture
            stage.setOnHidden(event -> loadDevoirsData());
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre d'ajout de devoir",
                    e.getMessage());
        }
    }

    // Méthode utilitaire pour afficher des alertes
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}