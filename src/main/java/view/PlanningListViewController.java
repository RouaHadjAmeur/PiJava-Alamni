package view;

import dao.PlanningDAO;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import model.Planning;

import java.util.List;

public class PlanningListViewController {

    @FXML
    private ListView<Planning> planningListView;

    @FXML
    public void initialize() {
        PlanningDAO dao = new PlanningDAO();
        List<Planning> all = dao.getAllPlannings();

        planningListView.getItems().addAll(all);
        planningListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Planning item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Text details = new Text("\uD83D\uDCDD Name: " + item.getName() + "\n" +
                            "\uD83D\uDD52 Start: " + item.getStartTime() + "\n" +
                            "\u23F0 End: " + item.getEndTime() + "\n" +
                            "\uD83D\uDC68\u200D\uD83C\uDF93 Teacher: " + item.getTeacher() + "\n" +
                            "\uD83D\uDCDA Level: " + item.getStudentLevel());

                    // Load icons from resources/icons/edit.png and delete.png
                    ImageView editIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/edit.png")));
                    editIcon.setFitWidth(20);
                    editIcon.setFitHeight(20);
                    editIcon.setCursor(Cursor.HAND);
                    editIcon.setOnMouseClicked(e -> MainController.getInstance().loadEditForm(item));

                    ImageView deleteIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/delete.png")));
                    deleteIcon.setFitWidth(20);
                    deleteIcon.setFitHeight(20);
                    deleteIcon.setCursor(Cursor.HAND);
                    deleteIcon.setOnMouseClicked(e -> {
                        dao.deletePlanning(item.getId());
                        planningListView.getItems().remove(item);
                    });

                    HBox row = new HBox(10, details, editIcon, deleteIcon);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                }
            }
        });
    }
}
