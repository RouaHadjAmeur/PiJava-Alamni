package form;

import dao.SeanceDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import model.Seance;
import view.MainController;

import java.io.IOException;

public class SeanceFormController {

    @FXML private TextField nameField;
    @FXML private TextArea descArea;
    @FXML private Button saveBtn;
    @FXML private Button backToListBtn;

    @FXML
    private void initialize() {
        backToListBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PlanningListView.fxml"));
                Node listView = loader.load();
                MainController.getInstance().setMainContent(listView);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        saveBtn.setOnAction(e -> {
            if (isValidInput()) {
                Seance s = new Seance();
                s.setName(nameField.getText());
                s.setDescription(descArea.getText());

                new SeanceDAO().addSeance(s);

                nameField.clear();
                descArea.clear();

                MainController.getInstance().loadView("/view/PlanningListView.fxml");
            }
        });
    }

    private boolean isValidInput() {
        if (nameField.getText().isEmpty() || descArea.getText().isEmpty()) {
            showAlert("Fields cannot be empty.");
            return false;
        }

        if (!nameField.getText().matches("[a-zA-Z ]+")) {
            showAlert("Name must contain only letters.");
            return false;
        }

        return true;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
