package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

        loadDemandes();
        setupActionButtons();
    }

    private void loadDemandes() {
        List<Utilisateur> utilisateurs = UtilisateurService.findAll();
        demandes.setAll(utilisateurs.stream().filter(Utilisateur::isPending).toList());
        tableDemandes.setItems(demandes);
    }

    private void setupActionButtons() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button validerBtn = new Button("✅");
            private final Button refuserBtn = new Button("❌");

            {
                validerBtn.setStyle("-fx-background-color: green; -fx-text-fill: white;");
                refuserBtn.setStyle("-fx-background-color: red; -fx-text-fill: white;");

                validerBtn.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    u.setPending(false);

                    if (UtilisateurService.updateUtilisateur(u)) {
                        loadDemandes(); // 🔄 recharge et supprime ceux validés
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Erreur lors de la validation.").showAndWait();
                    }
                });
                refuserBtn.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    UtilisateurService.supprimer(u.getId());
                    loadDemandes();
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
}
