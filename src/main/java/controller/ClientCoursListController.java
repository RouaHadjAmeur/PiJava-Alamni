package controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.CoursDAO;
import model.Cours;
import utils.ImageUtils;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientCoursListController implements Initializable {

    @FXML private Label userInitialsLabel;
    @FXML private Label usernameLabel;
    @FXML private ComboBox<String> matiereComboBox;
    @FXML private ComboBox<String> niveauComboBox;
    @FXML private TextField searchField;
    @FXML private GridPane coursesGrid;
    @FXML private StackPane emptyCoursesPane;

    private CoursDAO coursDAO;
    private ObservableList<Cours> allCoursList;
    private ObservableList<Cours> filteredCoursList;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le DAO
        coursDAO = new CoursDAO();
        allCoursList = FXCollections.observableArrayList();
        filteredCoursList = FXCollections.observableArrayList();

        // Charger le CSS client
        Platform.runLater(() -> {
            Scene scene = userInitialsLabel.getScene();
            if (scene != null) {
                scene.getStylesheets().add(getClass().getResource("/css/client-styles.css").toExternalForm());
            }
        });



        // Configurer les filtres
        setupFilters();

        // Configurer la recherche
        setupSearch();

        // Charger les cours
        loadCourses();
    }

    private void setupFilters() {
        // Charger les matières disponibles
        List<String> matieres = coursDAO.getAllMatieres();
        matiereComboBox.getItems().add("Toutes les matières");
        matiereComboBox.getItems().addAll(matieres);
        matiereComboBox.setValue("Toutes les matières");

        // Charger les niveaux disponibles
        List<String> niveaux = coursDAO.getAllNiveaux();
        niveauComboBox.getItems().add("Tous les niveaux");
        niveauComboBox.getItems().addAll(niveaux);
        niveauComboBox.setValue("Tous les niveaux");

        // Ajouter les écouteurs pour filtrer les cours
        matiereComboBox.setOnAction(e -> filterCourses());
        niveauComboBox.setOnAction(e -> filterCourses());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCourses();
        });
    }

    private void loadCourses() {
        // Charger tous les cours disponibles
        List<Cours> courses = coursDAO.getAll();
        allCoursList.clear();
        allCoursList.addAll(courses);

        // Appliquer les filtres initiaux
        filterCourses();
    }

    private void filterCourses() {
        String searchText = searchField.getText().toLowerCase();
        String matiere = matiereComboBox.getValue();
        String niveau = niveauComboBox.getValue();

        // Filtrer les cours selon les critères
        List<Cours> filtered = allCoursList.stream()
                .filter(cours -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            cours.getTitre().toLowerCase().contains(searchText) ||
                            (cours.getDescr_c() != null && cours.getDescr_c().toLowerCase().contains(searchText));

                    boolean matchesMatiere = "Toutes les matières".equals(matiere) ||
                            (cours.getMatiere_c() != null && cours.getMatiere_c().equals(matiere));

                    boolean matchesNiveau = "Tous les niveaux".equals(niveau) ||
                            (cours.getNiveau() != null && cours.getNiveau().equals(niveau));

                    return matchesSearch && matchesMatiere && matchesNiveau;
                })
                .collect(Collectors.toList());

        // Mettre à jour la liste filtrée
        filteredCoursList.clear();
        filteredCoursList.addAll(filtered);

        // Afficher les cours filtrés
        displayCourses();
    }

    private void displayCourses() {
        // Effacer la grille existante
        coursesGrid.getChildren().clear();

        // Vérifier s'il y a des cours à afficher
        if (filteredCoursList.isEmpty()) {
            emptyCoursesPane.setVisible(true);
            coursesGrid.setVisible(false);
            return;
        }

        // Afficher les cours dans la grille
        emptyCoursesPane.setVisible(false);
        coursesGrid.setVisible(true);

        int column = 0;
        int row = 0;

        for (Cours cours : filteredCoursList) {
            // Créer une carte de cours
            VBox courseCard = createCourseCard(cours);

            // Ajouter la carte à la grille
            coursesGrid.add(courseCard, column, row);

            // Mettre à jour les indices de colonne et de ligne
            column++;
            if (column > 2) {
                column = 0;
                row++;
            }

            // Ajouter une animation de fondu
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), courseCard);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    private VBox createCourseCard(Cours cours) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");

        // Image du cours
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("course-image-container");

        ImageView courseImage = new ImageView();
        courseImage.setFitWidth(300);
        courseImage.setFitHeight(140);
        courseImage.setPreserveRatio(false);
        courseImage.getStyleClass().add("course-image");

        // Charger l'image du cours ou une image par défaut
        String imagePath = cours.getImage();
        if (imagePath != null && !imagePath.isEmpty()) {
            File file = new File(imagePath);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                courseImage.setImage(image);
            } else {
                // Image par défaut avec la première lettre du titre
                String initial = cours.getTitre() != null && !cours.getTitre().isEmpty() ?
                        cours.getTitre().substring(0, 1).toUpperCase() : "C";
                courseImage.setImage(ImageUtils.createTextImage(300, 140, initial,
                        Color.web("#2a7aad"), Color.WHITE));
            }
        } else {
            // Image par défaut avec la première lettre du titre
            String initial = cours.getTitre() != null && !cours.getTitre().isEmpty() ?
                    cours.getTitre().substring(0, 1).toUpperCase() : "C";
            courseImage.setImage(ImageUtils.createTextImage(300, 140, initial,
                    Color.web("#2a7aad"), Color.WHITE));
        }

        imageContainer.getChildren().add(courseImage);

        // Contenu du cours
        VBox content = new VBox();
        content.getStyleClass().add("course-content");

        Label titleLabel = new Label(cours.getTitre());
        titleLabel.getStyleClass().add("course-title");

        Label descriptionLabel = new Label(cours.getDescr_c() != null ? cours.getDescr_c() : "Aucune description disponible");
        descriptionLabel.getStyleClass().add("course-description");

        // Métadonnées du cours
        HBox metaData = new HBox();
        metaData.getStyleClass().add("course-meta");

        // Matière
        HBox matiereBox = new HBox();
        matiereBox.getStyleClass().add("meta-item");

        SVGPath bookIcon = new SVGPath();
        bookIcon.setContent("M21,5C19.89,4.65 18.67,4.5 17.5,4.5C15.55,4.5 13.45,4.9 12,6C10.55,4.9 8.45,4.5 6.5,4.5C4.55,4.5 2.45,4.9 1,6V20.65C1,20.9 1.25,21.15 1.5,21.15C1.6,21.15 1.65,21.1 1.75,21.1C3.1,20.45 5.05,20 6.5,20C8.45,20 10.55,20.4 12,21.5C13.35,20.65 15.8,20 17.5,20C19.15,20 20.85,20.3 22.25,21.05C22.35,21.1 22.4,21.1 22.5,21.1C22.75,21.1 23,20.85 23,20.6V6C22.4,5.55 21.75,5.25 21,5M21,18.5C19.9,18.15 18.7,18 17.5,18C15.8,18 13.35,18.65 12,19.5V8C13.35,7.15 15.8,6.5 17.5,6.5C18.7,6.5 19.9,6.65 21,7V18.5Z");
        bookIcon.getStyleClass().add("meta-icon");

        Label matiereLabel = new Label(cours.getMatiere_c() != null ? cours.getMatiere_c() : "N/A");
        matiereLabel.getStyleClass().add("meta-text");

        matiereBox.getChildren().addAll(bookIcon, matiereLabel);

        // Niveau
        HBox niveauBox = new HBox();
        niveauBox.getStyleClass().add("meta-item");

        SVGPath levelIcon = new SVGPath();
        levelIcon.setContent("M12,3L1,9L12,15L21,10.09V17H23V9M5,13.18V17.18L12,21L19,17.18V13.18L12,17L5,13.18Z");
        levelIcon.getStyleClass().add("meta-icon");

        Label niveauLabel = new Label(cours.getNiveau() != null ? cours.getNiveau() : "N/A");
        niveauLabel.getStyleClass().add("meta-text");

        niveauBox.getChildren().addAll(levelIcon, niveauLabel);

        // Date
        HBox dateBox = new HBox();
        dateBox.getStyleClass().add("meta-item");

        SVGPath dateIcon = new SVGPath();
        dateIcon.setContent("M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1");
        dateIcon.getStyleClass().add("meta-icon");

        String dateText = "N/A";
        if (cours.getDate_c() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            dateText = cours.getDate_c().format(formatter);
        }
        Label dateLabel = new Label(dateText);
        dateLabel.getStyleClass().add("meta-text");

        dateBox.getChildren().addAll(dateIcon, dateLabel);

        metaData.getChildren().addAll(matiereBox, niveauBox, dateBox);

        // Actions
        HBox actions = new HBox();
        actions.getStyleClass().add("course-actions");

        Button viewButton = new Button("Voir");
        viewButton.getStyleClass().add("view-button");
        viewButton.setOnAction(e -> openCourseDetails(cours));

        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("favorite-button");

        SVGPath heartIcon = new SVGPath();
        heartIcon.setContent("M12.1,18.55L12,18.65L11.89,18.55C7.14,14.24 4,11.39 4,8.5C4,6.5 5.5,5 7.5,5C9.04,5 10.54,6 11.07,7.36H12.93C13.46,6 14.96,5 16.5,5C18.5,5 20,6.5 20,8.5C20,11.39 16.86,14.24 12.1,18.55M16.5,3C14.76,3 13.09,3.81 12,5.08C10.91,3.81 9.24,3 7.5,3C4.42,3 2,5.41 2,8.5C2,12.27 5.4,15.36 10.55,20.03L12,21.35L13.45,20.03C18.6,15.36 22,12.27 22,8.5C22,5.41 19.58,3 16.5,3Z");
        heartIcon.getStyleClass().add("favorite-icon");

        favoriteButton.setGraphic(heartIcon);
        favoriteButton.setOnAction(e -> toggleFavorite(favoriteButton, cours));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(viewButton, spacer, favoriteButton);

        // Assembler la carte
        content.getChildren().addAll(titleLabel, descriptionLabel, metaData);
        card.getChildren().addAll(imageContainer, content, actions);

        return card;
    }

    private void openCourseDetails(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client-cours-view.fxml"));
            Parent root = loader.load();

            ClientCoursViewController controller = loader.getController();
            controller.setCours(cours);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails du cours");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/client-styles.css").toExternalForm());
            stage.setScene(scene);

            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails du cours", e.getMessage());
        }
    }

    private void toggleFavorite(Button favoriteButton, Cours cours) {
        if (favoriteButton.getStyleClass().contains("favorite-active")) {
            favoriteButton.getStyleClass().remove("favorite-active");
            // Logique pour retirer des favoris
            System.out.println("Cours retiré des favoris: " + cours.getTitre());
        } else {
            favoriteButton.getStyleClass().add("favorite-active");
            // Logique pour ajouter aux favoris
            System.out.println("Cours ajouté aux favoris: " + cours.getTitre());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Appliquer le style client à la boîte de dialogue
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/client-styles.css").toExternalForm());

        alert.showAndWait();
    }
}