package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.HBox;
import model.Reclamation;
import services.ArchiveService;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArchiveController implements Initializable {

    @FXML private TableView<Reclamation> archivesTable;
    @FXML private TableColumn<Reclamation, Integer> idColumn;
    @FXML private TableColumn<Reclamation, String> emailColumn;
    @FXML private TableColumn<Reclamation, String> objetColumn;
    @FXML private TableColumn<Reclamation, String> descriptionColumn;
    @FXML private TableColumn<Reclamation, String> statusColumn;
    @FXML private TableColumn<Reclamation, String> dateColumn;
    @FXML private TableColumn<Reclamation, String> ratingColumn;
    @FXML private TableColumn<Reclamation, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private PieChart statusPieChart;

    private final ArchiveService archiveService = new ArchiveService();
    private ObservableList<Reclamation> archivesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        loadArchives();
        setupSearchListener();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("user_email"));
        objetColumn.setCellValueFactory(new PropertyValueFactory<>("objet"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date_soumission"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));

        // Configurer la colonne des actions
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Supprimer");
            private final Button restoreButton = new Button("Récupérer");
            private final HBox actionBox = new HBox(5, restoreButton, deleteButton);
            {
                deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");
                deleteButton.setOnAction(e -> {
                    Reclamation reclamation = getTableView().getItems().get(getIndex());
                    handleDelete(reclamation);
                });
                restoreButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white;");
                restoreButton.setOnAction(e -> {
                    Reclamation reclamation = getTableView().getItems().get(getIndex());
                    handleRestore(reclamation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });
    }

    private void setupFilters() {
        filterComboBox.setItems(FXCollections.observableArrayList(
            "Tous", "En attente", "En cours", "Résolue", "Archivée"
        ));
        filterComboBox.setValue("Tous");
        filterComboBox.setOnAction(e -> filterArchives());
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterArchives();
        });
    }

    private void loadArchives() {
        List<Reclamation> archives = archiveService.getArchivedReclamations();
        if (archives != null) {
            archivesList = FXCollections.observableArrayList(archives);
            archivesTable.setItems(archivesList);
        }
    }

    private void filterArchives() {
        String searchText = searchField.getText().toLowerCase();
        String selectedStatus = filterComboBox.getValue();

        List<Reclamation> filteredList = archivesList.stream()
            .filter(r -> {
                boolean matchesSearch = r.getObjet().toLowerCase().contains(searchText) ||
                                      r.getDescription().toLowerCase().contains(searchText) ||
                                      r.getUser_email().toLowerCase().contains(searchText);
                boolean matchesStatus = "Tous".equals(selectedStatus) ||
                                      r.getStatus().equals(selectedStatus);
                return matchesSearch && matchesStatus;
            })
            .collect(Collectors.toList());

        archivesTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    private void handleDelete(Reclamation reclamation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer cette archive ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                archiveService.deleteArchivedReclamation(reclamation.getId());
                archivesList.remove(reclamation);
            }
        });
    }

    private void handleRestore(Reclamation reclamation) {
        // Mettre à jour le statut dans la base locale
        services.ReclamationServices service = new services.ReclamationServices();
        reclamation.setStatus("En attente");
        service.update(reclamation);
        // Supprimer l'archive de MockAPI
        archiveService.deleteArchivedReclamation(reclamation.getId());
        archivesList.remove(reclamation);
        // Optionnel : afficher une alerte de succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Réclamation récupérée dans le dashboard.", ButtonType.OK);
        alert.showAndWait();
    }
} 