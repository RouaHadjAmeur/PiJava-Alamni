package form;

import dao.PlanningDAO;
import dao.SeanceDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import model.Planning;
import model.Seance;
import view.MainController;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class PlanningFormController implements Initializable {

    @FXML private TextField nameField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private ComboBox<Seance> seanceComboBox;
    @FXML private TextField teacherField;
    @FXML private TextField studentLevelField;
    @FXML private Button saveBtn;
    @FXML private Button backToListBtn;

    private Planning currentPlanning;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSeanceOptions();

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
                try {
                    LocalDate startDate = startDatePicker.getValue();
                    LocalTime startTime = LocalTime.parse(startTimeField.getText());
                    LocalDate endDate = endDatePicker.getValue();
                    LocalTime endTime = LocalTime.parse(endTimeField.getText());

                    Planning p = (currentPlanning != null) ? currentPlanning : new Planning();

                    p.setName(nameField.getText());
                    p.setStartTime(LocalDateTime.of(startDate, startTime));
                    p.setEndTime(LocalDateTime.of(endDate, endTime));
                    p.setSeance(seanceComboBox.getValue());
                    p.setTeacher(teacherField.getText());
                    p.setStudentLevel(studentLevelField.getText());

                    PlanningDAO dao = new PlanningDAO();
                    if (currentPlanning != null) {
                        dao.updatePlanning(p);
                    } else {
                        dao.addPlanning(p);
                    }

                    clearForm();
                    MainController.getInstance().loadView("/view/PlanningListView.fxml");

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void setPlanning(Planning p) {
        currentPlanning = p;
        nameField.setText(p.getName());
        startDatePicker.setValue(p.getStartTime().toLocalDate());
        startTimeField.setText(p.getStartTime().toLocalTime().toString());
        endDatePicker.setValue(p.getEndTime().toLocalDate());
        endTimeField.setText(p.getEndTime().toLocalTime().toString());
        seanceComboBox.setValue(p.getSeance());
        teacherField.setText(p.getTeacher());
        studentLevelField.setText(p.getStudentLevel());
        saveBtn.setText("Update Planning");
    }

    private void loadSeanceOptions() {
        List<Seance> seances = new SeanceDAO().getAllSeances();
        seanceComboBox.getItems().addAll(seances);

        seanceComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Seance item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        seanceComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Seance item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });
    }

    private boolean isValidInput() {
        if (nameField.getText().isEmpty() || teacherField.getText().isEmpty() || studentLevelField.getText().isEmpty()
                || startDatePicker.getValue() == null || startTimeField.getText().isEmpty()
                || endDatePicker.getValue() == null || endTimeField.getText().isEmpty()
                || seanceComboBox.getValue() == null) {
            showAlert("All fields must be filled.");
            return false;
        }

        if (!nameField.getText().matches("[a-zA-Z ]+")
                || !teacherField.getText().matches("[a-zA-Z ]+")
                || !studentLevelField.getText().matches("[a-zA-Z ]+")) {
            showAlert("Text fields cannot contain numbers or special characters.");
            return false;
        }

        try {
            LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), LocalTime.parse(startTimeField.getText()));
            LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), LocalTime.parse(endTimeField.getText()));
            if (!start.isBefore(end)) {
                showAlert("Start time must be before end time.");
                return false;
            }
        } catch (Exception e) {
            showAlert("Invalid time format. Use HH:mm.");
            return false;
        }

        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        nameField.clear();
        startDatePicker.setValue(null);
        startTimeField.clear();
        endDatePicker.setValue(null);
        endTimeField.clear();
        seanceComboBox.getSelectionModel().clearSelection();
        teacherField.clear();
        studentLevelField.clear();
        saveBtn.setText("➕ Add Planning");
        currentPlanning = null;
    }
}
