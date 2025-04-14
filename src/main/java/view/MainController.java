package view;

import form.PlanningFormController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import model.Planning;

import java.io.IOException;

public class MainController {

    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }
    public void setMainContent(Node node) {
        mainContentPane.getChildren().setAll(node);
    }

    @FXML private AnchorPane mainContentPane;
    @FXML private Button btnAddPlanning;
    @FXML private Button btnAddSeance;

    @FXML
    public void initialize() {
        loadView("/view/PlanningListView.fxml");

        btnAddPlanning.setOnAction(e -> loadView("/view/PlanningForm.fxml"));
        btnAddSeance.setOnAction(e -> loadView("/view/SeanceForm.fxml"));
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            mainContentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadEditForm(Planning planningToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PlanningForm.fxml"));
            Node content = loader.load();
            PlanningFormController controller = loader.getController();
            controller.setPlanning(planningToEdit);
            mainContentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
