package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Reclamation;
import model.ReponseReclamation;
import services.ReclamationServices;
import services.ReponseReclamationService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import java.util.List;

public class ReclamationDetailsController {

    @FXML private Label idLabel;
    @FXML private Label emailLabel;
    @FXML private Label objetLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusLabel;
    @FXML private Label dateLabel;
    @FXML private Label ReponseDateLabel;
    @FXML private Label adminMailLabel;
    @FXML private Button btnRetour;
    @FXML private Button btnSupprimer;
    @FXML private VBox reponsesContainer;

    private final ReponseReclamationService reponseReclamationService = new ReponseReclamationService();
    private final ReclamationServices service = new ReclamationServices();

    private Reclamation reclamation;

    public void setReclamation(Reclamation r) {
        this.reclamation = r;

        // Mise à jour status
        if (r.getStatus().equalsIgnoreCase("en attente")) {
            r.setStatus("en cours");
            service.update(r);
        }

        remplirDetailsReclamation();
        afficherReponses();
    }

    private void remplirDetailsReclamation() {
        idLabel.setText(String.valueOf(reclamation.getId()));
        emailLabel.setText(reclamation.getUser_email());
        objetLabel.setText(reclamation.getObjet());
        descriptionLabel.setText(reclamation.getDescription());
        statusLabel.setText(reclamation.getStatus());
        statusLabel.setStyle("-fx-padding: 6 16; -fx-text-fill: white; -fx-background-radius: 12; -fx-alignment: center;"
                + " -fx-background-color: " + getStatusColor(reclamation.getStatus()) + ";");
        dateLabel.setText(reclamation.getDate_soumission().toString());
        adminMailLabel.setText(reclamation.getAdmin_mail());

        btnRetour.setOnAction(e -> ((Stage) btnRetour.getScene().getWindow()).close());

        btnSupprimer.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cette réclamation ?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    service.delete(reclamation.getId());
                    ((Stage) btnSupprimer.getScene().getWindow()).close();
                }
            });
        });
    }

    private void afficherReponses() {
        List<ReponseReclamation> reponses = reponseReclamationService.getByReclamationId(reclamation.getId());
        reponsesContainer.getChildren().clear();

        if (reponses.isEmpty()) {
            Label aucunRep = new Label("Aucune réponse disponible pour le moment.");
            aucunRep.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
            reponsesContainer.getChildren().add(aucunRep);
            return;
        }

        for (ReponseReclamation rep : reponses) {
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: #fff5f2; -fx-padding: 10; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #dcdde1;");

            Label admin = new Label("Admin : ghaith aissa");
            admin.setStyle("-fx-font-weight: bold;");

            Label contenu = new Label(rep.getContenue());
            contenu.setWrapText(true);

            String dateText;
            if (rep.getDateReponse() != null) {
                dateText = "Posté le " + formatDate(rep.getDateReponse());
            } else {
                dateText = "Date inconnue";
            }

            Label date = new Label(dateText);
            date.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8fa6;");


            Button btnModifier = new Button("Modifier");
            btnModifier.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 3 10;");

            btnModifier.setOnAction(e -> {
                Dialog<String> dialog = new Dialog<>();
                dialog.setTitle("Modifier la réponse");
                dialog.setHeaderText("Saisir la nouvelle réponse :");
                dialog.getDialogPane().setStyle("-fx-background-color: #ffffff;"); 


                ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

                VBox content = new VBox(10);
                content.setPadding(new Insets(10));

                TextArea textArea = new TextArea(rep.getContenue());
                textArea.setWrapText(true);
                textArea.setPrefRowCount(4);

                Label errorLabel = new Label();
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");

                content.getChildren().addAll(textArea, errorLabel);
                dialog.getDialogPane().setContent(content);

                Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
                saveButton.setDisable(true);

                textArea.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.trim().isEmpty()) {
                        errorLabel.setText("La réponse ne doit pas être vide.");
                        saveButton.setDisable(true);
                    } else if (newVal.trim().length() < 10) {
                        errorLabel.setText("Minimum 10 caractères requis.");
                        saveButton.setDisable(true);
                    } else {
                        errorLabel.setText("");
                        saveButton.setDisable(false);
                    }
                });

                // Custom Style for Buttons
                dialog.setOnShown(event -> {
                    Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
                    btnSave.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 20;");

                    Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                    btnCancel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 20;");
                });

                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == saveButtonType) {
                        return textArea.getText().trim();
                    }
                    return null;
                });

                dialog.showAndWait().ifPresent(newText -> {
                    rep.setContenue(newText);
                    reponseReclamationService.modify(rep);
                    afficherReponses();
                });
            });





            // Bouton Supprimer
            Button btnSupprimer = new Button("Supprimer");
            btnSupprimer.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 3 10;");
            btnSupprimer.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette réponse ?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        reponseReclamationService.delete(rep.getId());
                        afficherReponses();
                    }
                });
            });

            HBox actions = new HBox(10, btnModifier, btnSupprimer);
            actions.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(admin, contenu, date, actions);
            reponsesContainer.getChildren().add(card);
        }
    }

    private String formatDate(java.sql.Date date) {
        return date.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }



    private String getStatusColor(String status) {
        return switch (status.toLowerCase()) {
            case "en attente" -> "#f59e0b";
            case "en cours" -> "#3b82f6";
            case "résolue" -> "#10b981";
            default -> "#6b7280";
        };
    }
}
