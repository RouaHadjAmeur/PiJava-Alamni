package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Eleve;
import model.Utilisateur;
import service.UtilisateurService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import java.io.File;

import javafx.stage.FileChooser;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.Desktop;
import java.io.File;

public class UtilisateurController {

    @FXML private TableView<Utilisateur> userTable;
    @FXML private TableColumn<Utilisateur, Void> photoCol;
    @FXML private TableColumn<Utilisateur, String> nomCol;
    @FXML private TableColumn<Utilisateur, String> prenomCol;
    @FXML private TableColumn<Utilisateur, String> emailCol;
    @FXML private TableColumn<Utilisateur, String> roleCol;
    @FXML private TableColumn<Utilisateur, String> niveauCol;
    @FXML private TableColumn<Utilisateur, String> nomNiveauCol;
    @FXML private TableColumn<Utilisateur, Void> actionsCol;
    @FXML private TextField searchField;
    @FXML private Label totalCountLabel;
    @FXML private Label lastUpdateLabel;

    private ObservableList<Utilisateur> utilisateurs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configurer les colonnes
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Configurer la colonne Niveau (pour les élèves)
        niveauCol.setCellValueFactory(cellData -> {
            Utilisateur user = cellData.getValue();
            if (user instanceof Eleve) {
                return new javafx.beans.property.SimpleStringProperty(((Eleve) user).getNiveau());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

// Configurer la colonne Nom niveau (pour les élèves)
        nomNiveauCol.setCellValueFactory(cellData -> {
            Utilisateur user = cellData.getValue();
            if (user instanceof Eleve) {
                return new javafx.beans.property.SimpleStringProperty(((Eleve) user).getNomNiveau());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Configurer la colonne Photo
        configurePhotoColumn();

        UtilisateurService.migrerCheminsPhotos();



        // Configurer la colonne Actions
        ajouterColonnesActions();

        // Configurer la recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrerUtilisateurs(newValue);
        });

        // Charger les utilisateurs
        chargerUtilisateurs();

        // Mettre à jour les statistiques
        updateStatusInfo();
    }

    private void configurePhotoColumn() {
        photoCol.setCellFactory(col -> new TableCell<Utilisateur, Void>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                try {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        Utilisateur utilisateur = getTableView().getItems().get(index);
                        String photoPath = utilisateur.getPhoto();

                        System.out.println("Tentative de chargement de photo pour: " +
                                utilisateur.getEmail() + ", chemin: " + photoPath);

                        if (photoPath != null && !photoPath.isEmpty()) {
                            boolean photoLoaded = false;

                            // MÉTHODE 1: Essayer via le classpath
                            try {
                                System.out.println("  Méthode 1 - classpath: /" + photoPath);
                                Image image = new Image(getClass().getResourceAsStream("/" + photoPath));
                                if (image != null && !image.isError()) {
                                    imageView.setImage(image);
                                    setGraphic(imageView);
                                    photoLoaded = true;
                                    System.out.println("  ✓ Image chargée via classpath");
                                } else {
                                    System.out.println("  ✗ Échec classpath: image error");
                                }
                            } catch (Exception e) {
                                System.out.println("  ✗ Échec classpath: " + e.getMessage());
                            }

                            // MÉTHODE 2: Essayer dans src/main/resources
                            if (!photoLoaded) {
                                try {
                                    File resourceDir = new File("src/main/resources");
                                    File imageFile = new File(resourceDir, photoPath);
                                    System.out.println("  Méthode 2 - resources: " + imageFile.getAbsolutePath());

                                    if (imageFile.exists()) {
                                        Image image = new Image(imageFile.toURI().toString());
                                        imageView.setImage(image);
                                        setGraphic(imageView);
                                        photoLoaded = true;
                                        System.out.println("  ✓ Image chargée via resources");
                                    } else {
                                        System.out.println("  ✗ Fichier non trouvé dans resources");
                                    }
                                } catch (Exception e) {
                                    System.out.println("  ✗ Échec resources: " + e.getMessage());
                                }
                            }

                            // MÉTHODE 3: Essayer dans target/classes
                            if (!photoLoaded) {
                                try {
                                    File targetDir = new File("target/classes");
                                    File imageFile = new File(targetDir, photoPath);
                                    System.out.println("  Méthode 3 - target: " + imageFile.getAbsolutePath());

                                    if (imageFile.exists()) {
                                        Image image = new Image(imageFile.toURI().toString());
                                        imageView.setImage(image);
                                        setGraphic(imageView);
                                        photoLoaded = true;
                                        System.out.println("  ✓ Image chargée via target");
                                    } else {
                                        System.out.println("  ✗ Fichier non trouvé dans target");
                                    }
                                } catch (Exception e) {
                                    System.out.println("  ✗ Échec target: " + e.getMessage());
                                }
                            }

                            // Si aucune méthode n'a réussi
                            if (!photoLoaded) {
                                setText("?");
                                setGraphic(null);
                                System.out.println("  ✗ Échec de toutes les méthodes de chargement");
                            }
                        } else {
                            setText("?");
                            setGraphic(null);
                            System.out.println("  ✗ Pas de chemin de photo défini");
                        }
                    }
                } catch (Exception e) {
                    setText("!");
                    setGraphic(null);
                    System.err.println("Erreur générale lors du chargement: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void chargerUtilisateurs() {
        utilisateurs.clear();
        // Utiliser findAll() au lieu de lister()
        utilisateurs.addAll(UtilisateurService.findAll().stream()
                .filter(u -> !u.isPending())  // Exclure les utilisateurs en attente
                .toList());
        userTable.setItems(utilisateurs);

        // Log pour débogage
        System.out.println("Utilisateurs chargés: " + utilisateurs.size());
    }
    private void filtrerUtilisateurs(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            userTable.setItems(utilisateurs);
        } else {
            ObservableList<Utilisateur> filteredList = FXCollections.observableArrayList();
            String lowerCaseFilter = searchText.toLowerCase();

            for (Utilisateur user : utilisateurs) {
                if (user.getNom().toLowerCase().contains(lowerCaseFilter) ||
                        user.getPrenom().toLowerCase().contains(lowerCaseFilter) ||
                        user.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    filteredList.add(user);
                }
            }
            userTable.setItems(filteredList);
        }

        updateStatusInfo();
    }

    private void updateStatusInfo() {
        totalCountLabel.setText(userTable.getItems().size() + " utilisateurs");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");
        lastUpdateLabel.setText(LocalDateTime.now().format(formatter));
    }

    @FXML
    private void handleExportPdf() {
        try {
            // Créer un sélecteur de fichier
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
            fileChooser.setInitialFileName("utilisateurs_" +
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");

            // Afficher le sélecteur de fichier
            java.io.File file = fileChooser.showSaveDialog(userTable.getScene().getWindow());

            if (file != null) {
                // Exporter les utilisateurs actuellement affichés dans le tableau
                util.PDFExporter.exportUtilisateurs(userTable.getItems(), file.getAbsolutePath());

                // Afficher un message de confirmation
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export PDF");
                alert.setHeaderText("Export réussi");
                alert.setContentText("Le fichier PDF a été créé avec succès à l'emplacement :\n" + file.getAbsolutePath());

                // Ajouter un bouton pour ouvrir le dossier contenant le fichier
                ButtonType openFolderButton = new ButtonType("Ouvrir le dossier");
                alert.getButtonTypes().add(openFolderButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == openFolderButton) {
                        try {
                            java.awt.Desktop.getDesktop().open(file.getParentFile());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();

            // Afficher une erreur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Échec de l'export PDF");
            alert.setContentText("Une erreur est survenue lors de la création du PDF : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAjouterUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ajouterUtilisateur.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un utilisateur");
            stage.showAndWait();

            // Recharger après ajout
            chargerUtilisateurs();
            updateStatusInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ajouterColonnesActions() {
        actionsCol.setCellFactory(col -> new TableCell<Utilisateur, Void>() {
            private final Button detailBtn = new Button("👀");
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox container = new HBox(5, detailBtn, editBtn, deleteBtn);

            {
                detailBtn.setStyle("-fx-background-color: #03a9f4; -fx-text-fill: white;");
                editBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                // Configuration des événements des boutons
                detailBtn.setOnAction(e -> {
                    Utilisateur utilisateur = getTableView().getItems().get(getIndex());
                    afficherDetailUtilisateur(utilisateur);
                });

                editBtn.setOnAction(e -> {
                    Utilisateur utilisateur = getTableView().getItems().get(getIndex());
                    modifierUtilisateur(utilisateur);
                });

                deleteBtn.setOnAction(e -> {
                    Utilisateur utilisateur = getTableView().getItems().get(getIndex());
                    supprimerUtilisateur(utilisateur);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void afficherDetailUtilisateur(Utilisateur utilisateur) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/detailUtilisateur.fxml"));
            Parent root = loader.load();

            DetailUtilisateurController controller = loader.getController();
            controller.setUtilisateur(utilisateur);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Détail - " + utilisateur.getPrenom());
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void modifierUtilisateur(Utilisateur utilisateur) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/modifierUtilisateur.fxml"));
            Parent root = loader.load();

            ModifierUtilisateurController controller = loader.getController();
            controller.setUtilisateur(utilisateur);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier - " + utilisateur.getPrenom());
            stage.showAndWait();

            // Recharger après modification
            chargerUtilisateurs();
            updateStatusInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void supprimerUtilisateur(Utilisateur utilisateur) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Voulez-vous vraiment supprimer cet utilisateur ?");
        alert.setContentText(utilisateur.getNom() + " " + utilisateur.getPrenom() + " (" + utilisateur.getEmail() + ")");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                UtilisateurService.supprimer(utilisateur.getId());
                chargerUtilisateurs();
                updateStatusInfo();
            }
        });
    }
}