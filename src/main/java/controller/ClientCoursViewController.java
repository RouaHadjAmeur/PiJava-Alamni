package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import dao.DevoirDAO;
import model.Cours;
import model.Devoir;
import utils.ImageUtils;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ClientCoursViewController implements Initializable {

    @FXML private ImageView courseImageView;
    @FXML private Label courseTitleLabel;
    @FXML private Label matiereLabel;
    @FXML private Label niveauLabel;
    @FXML private Label dateLabel;
    @FXML private Label descriptionLabel;
    @FXML private Button downloadBtn;
    @FXML private Button closeBtn;
    @FXML private Button favoriteBtn;

    @FXML private TableView<Devoir> devoirsTableView;
    @FXML private TableColumn<Devoir, String> titreColumn;
    @FXML private TableColumn<Devoir, String> descriptionColumn;
    @FXML private TableColumn<Devoir, String> dateColumn;
    @FXML private TableColumn<Devoir, Button> supportColumn;

    private Cours cours;
    private DevoirDAO devoirDAO;
    private ObservableList<Devoir> devoirsList;
    private boolean isFavorite = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        devoirDAO = new DevoirDAO();
        devoirsList = FXCollections.observableArrayList();

        // Configuration du bouton de fermeture
        closeBtn.setOnAction(event -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });

        // Configuration du bouton favori
        setupFavoriteButton();

        // Configuration des colonnes de la table des devoirs
        setupDevoirsTable();
    }

    private void setupFavoriteButton() {
        favoriteBtn.setOnAction(event -> {
            toggleFavorite();
        });
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;

        if (isFavorite) {
            // Mettre à jour l'icône et le texte
            SVGPath heartIcon = new SVGPath();
            heartIcon.setContent("M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z");
            heartIcon.getStyleClass().add("action-icon");
            heartIcon.getStyleClass().add("favorite-active");

            Label favoriteText = new Label("Retirer des favoris");
            favoriteText.getStyleClass().add("action-text");

            favoriteBtn.getGraphic().lookup(".action-icon").getStyleClass().add("favorite-active");
            ((Label)((javafx.scene.layout.HBox)favoriteBtn.getGraphic()).getChildren().get(1)).setText("Retirer des favoris");

            // Logique pour ajouter aux favoris
            System.out.println("Cours ajouté aux favoris: " + cours.getTitre());
        } else {
            // Mettre à jour l'icône et le texte
            favoriteBtn.getGraphic().lookup(".action-icon").getStyleClass().remove("favorite-active");
            ((Label)((javafx.scene.layout.HBox)favoriteBtn.getGraphic()).getChildren().get(1)).setText("Ajouter aux favoris");

            // Logique pour retirer des favoris
            System.out.println("Cours retiré des favoris: " + cours.getTitre());
        }
    }

    private void setupDevoirsTable() {
        titreColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitre_d()));

        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescr_d();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            return new SimpleStringProperty(desc);
        });

        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_d();
            if (date != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new SimpleStringProperty(date.format(formatter));
            }
            return new SimpleStringProperty("N/A");
        });

        supportColumn.setCellValueFactory(cellData -> {
            Button downloadBtn = new Button("Télécharger");
            downloadBtn.getStyleClass().add("download-button-small");

            String supportPath = cellData.getValue().getSupport_d();
            if (supportPath == null || supportPath.isEmpty()) {
                downloadBtn.setDisable(true);
            }

            downloadBtn.setOnAction(event -> {
                if (supportPath != null && !supportPath.isEmpty()) {
                    System.out.println("Téléchargement du support: " + supportPath);
                    // Logique de téléchargement
                }
            });

            return new SimpleObjectProperty<>(downloadBtn);
        });
    }

    public void setCours(Cours cours) {
        this.cours = cours;
        loadCoursDetails();
        loadDevoirsData();
    }

    private void loadCoursDetails() {
        if (cours != null) {
            courseTitleLabel.setText(cours.getTitre());
            descriptionLabel.setText(cours.getDescr_c() != null ? cours.getDescr_c() : "Aucune description disponible");
            matiereLabel.setText(cours.getMatiere_c() != null ? cours.getMatiere_c() : "N/A");
            niveauLabel.setText(cours.getNiveau() != null ? cours.getNiveau() : "N/A");

            if (cours.getDate_c() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                dateLabel.setText(cours.getDate_c().format(formatter));
            } else {
                dateLabel.setText("N/A");
            }

            // Charger l'image du cours
            String imagePath = cours.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    courseImageView.setImage(image);
                } else {
                    // Image par défaut
                    String initial = cours.getTitre() != null && !cours.getTitre().isEmpty() ?
                            cours.getTitre().substring(0, 1).toUpperCase() : "C";
                    courseImageView.setImage(ImageUtils.createTextImage(800, 200, initial,
                            Color.web("#1e6091"), Color.WHITE));
                }
            } else {
                // Image par défaut
                String initial = cours.getTitre() != null && !cours.getTitre().isEmpty() ?
                        cours.getTitre().substring(0, 1).toUpperCase() : "C";
                courseImageView.setImage(ImageUtils.createTextImage(800, 200, initial,
                        Color.web("#1e6091"), Color.WHITE));
            }

            // Configuration du bouton de téléchargement
            String supportPath = cours.getSupport_c();
            if (supportPath != null && !supportPath.isEmpty()) {
                downloadBtn.setDisable(false);
                downloadBtn.setOnAction(event -> {
                    System.out.println("Téléchargement du support: " + supportPath);
                    // Logique de téléchargement
                });
            } else {
                downloadBtn.setDisable(true);
            }
        }
    }

    private void loadDevoirsData() {
        if (cours != null) {
            List<Devoir> devoirs = devoirDAO.getByCours(cours.getId());
            devoirsList.clear();
            devoirsList.addAll(devoirs);
            devoirsTableView.setItems(devoirsList);
        }
    }
}