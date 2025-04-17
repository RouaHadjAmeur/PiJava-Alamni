package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Eleve;
import model.Utilisateur;
import service.UtilisateurService;

import java.util.List;

public class DemandeInscriptionController {

    @FXML private TableView<Utilisateur> tableDemandes;
    @FXML private TableColumn<Utilisateur, String> nomCol;
    @FXML private TableColumn<Utilisateur, String> prenomCol;
    @FXML private TableColumn<Utilisateur, String> emailCol;
    @FXML private TableColumn<Utilisateur, String> roleCol;
    @FXML private TableColumn<Utilisateur, String> niveauCol;
    @FXML private TableColumn<Utilisateur, Void> actionsCol;

    private ObservableList<Utilisateur> demandes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        nomCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getNom()));
        prenomCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getPrenom()));
        emailCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getEmail()));
        roleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getRole()));

        niveauCol.setCellValueFactory(cell -> {
            if (cell.getValue() instanceof Eleve eleve) {
                return new javafx.beans.property.SimpleStringProperty(eleve.getNiveau());
            } else {
                return new javafx.beans.property.SimpleStringProperty("");
            }
        });

        // Charger les demandes et configurer les boutons d'action
        loadDemandes();
        setupActionButtons();
    }

    /**
     * Charge les demandes d'inscription en attente de validation
     */
    private void loadDemandes() {
        // Utiliser la méthode getComptesEnAttente() plutôt que de filtrer tous les utilisateurs
        List<Utilisateur> utilisateursEnAttente = UtilisateurService.getComptesEnAttente();
        demandes.setAll(utilisateursEnAttente);
        tableDemandes.setItems(demandes);

        // Afficher un message si aucune demande n'est en attente
        if (demandes.isEmpty()) {
            System.out.println("Aucune demande d'inscription en attente");
        } else {
            System.out.println(demandes.size() + " demande(s) d'inscription en attente");
        }
    }

    /**
     * Configure les boutons d'action pour chaque ligne du tableau
     */
    private void setupActionButtons() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button validerBtn = new Button("✅");
            private final Button refuserBtn = new Button("❌");

            {
                // Style des boutons
                validerBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
                refuserBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");

                // Action du bouton Valider
                validerBtn.setOnAction(e -> {
                    Utilisateur utilisateur = getTableView().getItems().get(getIndex());

                    // Utiliser la méthode dédiée validerCompte
                    if (UtilisateurService.validerCompte(utilisateur.getId())) {
                        // Afficher un message de confirmation
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Validation");
                        alert.setHeaderText(null);
                        alert.setContentText("Le compte de " + utilisateur.getPrenom() + " " + utilisateur.getNom() + " a été validé avec succès.");
                        alert.showAndWait();

                        // Recharger les demandes
                        loadDemandes();
                    } else {
                        // Afficher un message d'erreur
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText(null);
                        alert.setContentText("Une erreur est survenue lors de la validation du compte.");
                        alert.showAndWait();
                    }
                });

                // Action du bouton Refuser
                refuserBtn.setOnAction(e -> {
                    Utilisateur utilisateur = getTableView().getItems().get(getIndex());

                    // Demander confirmation avant suppression
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirmation");
                    confirmAlert.setHeaderText("Refuser la demande d'inscription");
                    confirmAlert.setContentText("Êtes-vous sûr de vouloir refuser et supprimer la demande de " +
                            utilisateur.getPrenom() + " " + utilisateur.getNom() + " ?");

                    confirmAlert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            if (UtilisateurService.supprimer(utilisateur.getId())) {
                                // Recharger les demandes
                                loadDemandes();
                            } else {
                                // Afficher un message d'erreur
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Erreur");
                                errorAlert.setHeaderText(null);
                                errorAlert.setContentText("Une erreur est survenue lors de la suppression du compte.");
                                errorAlert.showAndWait();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10, validerBtn, refuserBtn);
                    setGraphic(box);
                }
            }
        });
    }

    /**
     * Rafraîchit la liste des demandes d'inscription
     * Cette méthode peut être appelée depuis l'extérieur
     */
    public void refreshDemandes() {
        loadDemandes();
    }
}