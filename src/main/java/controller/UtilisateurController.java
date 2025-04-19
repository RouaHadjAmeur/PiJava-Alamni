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
                // Configurer l'ImageView pour qu'elle s'adapte bien à la colonne
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    Utilisateur utilisateur = getTableView().getItems().get(getIndex());
                    if (utilisateur != null && utilisateur.getPhoto() != null) {
                        try {
                            File photoFile = new File(utilisateur.getPhoto());
                            if (photoFile.exists()) {
                                Image image = new Image(photoFile.toURI().toString());
                                imageView.setImage(image);
                                setGraphic(imageView);
                            } else {
                                setDefaultImage();
                            }
                        } catch (Exception e) {
                            setDefaultImage();
                            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
                        }
                    } else {
                        setDefaultImage();
                    }
                }
            }

            private void setDefaultImage() {
                try {
                    // Option 1: Utiliser une image par défaut depuis les ressources
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_avatar.png"));
                    imageView.setImage(defaultImage);
                    setGraphic(imageView);
                } catch (Exception e) {
                    // Option 2: Créer un cercle avec les initiales ou un placeholder
                    StackPane placeholder = new StackPane();
                    placeholder.setPrefSize(40, 40);
                    placeholder.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 20;");
                    Label label = new Label("?");
                    label.setStyle("-fx-text-fill: #757575; -fx-font-weight: bold;");
                    placeholder.getChildren().add(label);
                    setGraphic(placeholder);
                }
            }
        });
    }

    private void chargerUtilisateurs() {
        utilisateurs.clear();
// Remplace getAllUtilisateurs() par la méthode qui existe réellement
        utilisateurs.addAll(UtilisateurService.lister()); // ou la méthode correcte
        userTable.setItems(utilisateurs);
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
        // Implémentation de l'export PDF
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export PDF");
        alert.setHeaderText("Fonction Export PDF");
        alert.setContentText("Le PDF a été exporté !");
        alert.showAndWait();
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