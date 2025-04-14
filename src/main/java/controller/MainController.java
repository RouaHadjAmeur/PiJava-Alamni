package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
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
import dao.CoursDAO;
import model.Cours;
import utils.ImageUtils;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TableView<Cours> coursTableView;
    @FXML private TableColumn<Cours, Integer> idColumn;
    @FXML private TableColumn<Cours, ImageView> imgColumn;
    @FXML private TableColumn<Cours, String> titreColumn;
    @FXML private TableColumn<Cours, String> descriptionColumn;
    @FXML private TableColumn<Cours, String> matiereColumn;
    @FXML private TableColumn<Cours, String> dateColumn;
    @FXML private TableColumn<Cours, String> niveauColumn;
    @FXML private TableColumn<Cours, Button> supportColumn;
    @FXML private TableColumn<Cours, HBox> actionsColumn;

    @FXML private TextField searchField;
    @FXML private Button exportPdfBtn;
    @FXML private Button addCoursBtn;

    private CoursDAO coursDAO;
    private ObservableList<Cours> coursList;
    private FilteredList<Cours> filteredCoursList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation du DAO
        coursDAO = new CoursDAO();

        // Configuration des colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        imgColumn.setCellValueFactory(cellData -> {
            ImageView imageView = new ImageView();
            imageView.setFitHeight(60);
            imageView.setFitWidth(60);
            imageView.setPreserveRatio(true);

            String imagePath = cellData.getValue().getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                } else {
                    // Image par défaut générée si le fichier n'existe pas
                    String initial = cellData.getValue().getTitre() != null && !cellData.getValue().getTitre().isEmpty() ?
                            cellData.getValue().getTitre().substring(0, 1).toUpperCase() : "C";
                    imageView.setImage(ImageUtils.createTextImage(60, 60, initial, Color.LIGHTGRAY, Color.WHITE));
                }
            } else {
                // Image par défaut générée si aucun chemin d'image
                String initial = cellData.getValue().getTitre() != null && !cellData.getValue().getTitre().isEmpty() ?
                        cellData.getValue().getTitre().substring(0, 1).toUpperCase() : "C";
                imageView.setImage(ImageUtils.createTextImage(60, 60, initial, Color.LIGHTGRAY, Color.WHITE));
            }

            return new SimpleObjectProperty<>(imageView);
        });

        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));

        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescr_c();
            // Limiter la longueur de la description pour l'affichage
            if (desc != null && desc.length() > 30) {
                desc = desc.substring(0, 30) + "...";
            }
            return new SimpleStringProperty(desc);
        });

        matiereColumn.setCellValueFactory(new PropertyValueFactory<>("matiere_c"));

        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_c();
            if (date != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
                return new SimpleStringProperty(date.format(formatter));
            }
            return new SimpleStringProperty("N/A");
        });

        niveauColumn.setCellValueFactory(new PropertyValueFactory<>("niveau"));

        supportColumn.setCellValueFactory(cellData -> {
            Button downloadBtn = new Button("Télécharger");
            downloadBtn.getStyleClass().addAll("action-button", "download-button");

            downloadBtn.setOnAction(event -> {
                // Logique pour télécharger le support
                String supportPath = cellData.getValue().getSupport_c();
                if (supportPath != null && !supportPath.isEmpty()) {
                    System.out.println("Téléchargement du support: " + supportPath);
                    // Implémentez la logique de téléchargement ici
                }
            });

            return new SimpleObjectProperty<>(downloadBtn);
        });

        actionsColumn.setCellValueFactory(cellData -> {
            Cours cours = cellData.getValue();

            // Bouton Voir
            Button viewBtn = new Button();
            ImageView viewIcon = new ImageView(ImageUtils.createViewIcon(16));
            viewIcon.setFitHeight(16);
            viewIcon.setFitWidth(16);
            viewBtn.setGraphic(viewIcon);
            viewBtn.getStyleClass().addAll("action-button", "view-button");
            viewBtn.setTooltip(new Tooltip("Voir"));

            viewBtn.setOnAction(event -> {
                System.out.println("Bouton Voir cliqué pour cours ID: " + cours.getId());
                viewCours(cours);
            });

            // Bouton Modifier
            Button editBtn = new Button();
            ImageView editIcon = new ImageView(ImageUtils.createEditIcon(16));
            editIcon.setFitHeight(16);
            editIcon.setFitWidth(16);
            editBtn.setGraphic(editIcon);
            editBtn.getStyleClass().addAll("action-button", "edit-button");
            editBtn.setTooltip(new Tooltip("Modifier"));

            editBtn.setOnAction(event -> {
                System.out.println("Bouton Modifier cliqué pour cours ID: " + cours.getId());
                editCours(cours);
            });

            // Bouton Supprimer
            Button deleteBtn = new Button();
            ImageView deleteIcon = new ImageView(ImageUtils.createDeleteIcon(16));
            deleteIcon.setFitHeight(16);
            deleteIcon.setFitWidth(16);
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.getStyleClass().addAll("action-button", "delete-button");
            deleteBtn.setTooltip(new Tooltip("Supprimer"));

            deleteBtn.setOnAction(event -> {
                System.out.println("Bouton Supprimer cliqué pour cours ID: " + cours.getId());
                try {
                    deleteCours(cours);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la suppression: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression",
                            "Une erreur est survenue: " + e.getMessage());
                }
            });

            // Conteneur pour les boutons
            HBox actionsBox = new HBox(5);
            actionsBox.setAlignment(Pos.CENTER);
            actionsBox.getChildren().addAll(viewBtn, editBtn, deleteBtn);

            return new SimpleObjectProperty<>(actionsBox);
        });

        // Chargement des données
        loadCoursData();

        // Configuration de la recherche
        setupSearch();

        // Configuration des boutons
        setupButtons();
    }

    private void loadCoursData() {
        try {
            List<Cours> cours = coursDAO.getAll();
            coursList = FXCollections.observableArrayList(cours);
            filteredCoursList = new FilteredList<>(coursList, p -> true);
            coursTableView.setItems(filteredCoursList);
            System.out.println("Données chargées: " + cours.size() + " cours");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement des données", e.getMessage());
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredCoursList.setPredicate(cours -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (cours.getMatiere_c() != null && cours.getMatiere_c().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (cours.getTitre() != null && cours.getTitre().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (cours.getNiveau() != null && cours.getNiveau().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
        });
    }

    private void setupButtons() {
        // Bouton Ajouter
        addCoursBtn.setOnAction(this::handleAddCours);

        // Bouton Exporter PDF
        exportPdfBtn.setOnAction(this::handleExportPDF);
    }

    @FXML
    private void handleAddCours(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-cours-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter un cours");
            stage.setScene(new Scene(root));

            // Afficher la fenêtre et attendre qu'elle soit fermée
            stage.showAndWait();

            // Recharger les données
            loadCoursData();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre d'ajout: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre d'ajout de cours", e.getMessage());
        }
    }

    private void handleExportPDF(ActionEvent event) {
        // Logique pour exporter en PDF
        showAlert(Alert.AlertType.INFORMATION, "Export PDF", "Fonctionnalité en cours de développement",
                "L'exportation en PDF sera disponible prochainement.");
    }

    private void viewCours(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/view-cours.fxml"));
            Parent root = loader.load();

            ViewCoursController controller = loader.getController();
            controller.setCours(cours);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails du cours");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre de détails: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de détails", e.getMessage());
        }
    }

    private void editCours(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit-cours-view.fxml"));
            Parent root = loader.load();

            EditCoursController controller = loader.getController();
            controller.setCours(cours);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier le cours");
            stage.setScene(new Scene(root));

            // Afficher la fenêtre et attendre qu'elle soit fermée
            stage.showAndWait();

            // Recharger les données
            loadCoursData();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre de modification: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de modification", e.getMessage());
        }
    }

    private void deleteCours(Cours cours) {
        System.out.println("Demande de suppression du cours ID: " + cours.getId());

        if (cours == null || cours.getId() <= 0) {
            System.err.println("Cours invalide ou ID négatif/nul");
            showAlert(Alert.AlertType.ERROR, "Erreur", "Cours invalide",
                    "Impossible de supprimer ce cours car son ID est invalide.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir supprimer ce cours ?");
        confirmDialog.setContentText("Cette action est irréversible et supprimera également tous les devoirs associés.");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("Confirmation reçue, lancement de la suppression");
                try {
                    // Vérifier la connexion à la base de données avant suppression
                    boolean connectionValid = coursDAO.checkAndRenewConnection();

                    if (!connectionValid) {
                        throw new Exception("Erreur de connexion à la base de données");
                    }

                    boolean success = coursDAO.supprimer(cours.getId());
                    System.out.println("Résultat de la suppression: " + success);

                    if (success) {
                        coursList.remove(cours);
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Cours supprimé",
                                "Le cours a été supprimé avec succès.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression",
                                "La suppression du cours a échoué. Aucune modification effectuée.");
                    }
                } catch (Exception e) {
                    System.err.println("Exception lors de la suppression: " + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Exception lors de la suppression",
                            "Détails: " + e.getMessage());
                }
            } else {
                System.out.println("Suppression annulée par l'utilisateur");
            }
        });
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