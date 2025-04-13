package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import model.Reclamation;
import services.ReclamationServices;
import javafx.geometry.Pos;

public class ReclamationDashboardController {

    @FXML
    private ListView<Reclamation> reclamationsListView;

    private final ReclamationServices service = new ReclamationServices();

    @FXML
    public void initialize() {
        loadReclamations();
    }

    private void loadReclamations() {
        ObservableList<Reclamation> list = FXCollections.observableArrayList(service.afficher());
        reclamationsListView.setItems(list);

        reclamationsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label objet = new Label("Objet: " + r.getObjet());
                    Label desc = new Label("Description: " + r.getDescription());
                    Button view = new Button("Voir");

                    box.getChildren().addAll(objet, desc, view);
                    setGraphic(box);
                }
            }
        });
    }
}
