package utils;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;


import javafx.scene.paint.LinearGradient;

public class ImageUtils {

    /**
     * Crée une image de couleur unie
     */
    public static Image createColorImage(int width, int height, Color color) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelWriter.setColor(x, y, color);
            }
        }

        return image;
    }

    /**
     * Crée une image avec un texte centré
     */
    public static Image createTextImage(int width, int height, String text, Color bgColor, Color textColor) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Dessiner le fond
        gc.setFill(bgColor);
        gc.fillRect(0, 0, width, height);

        // Dessiner le texte
        gc.setFill(textColor);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);        gc.setFont(javafx.scene.text.Font.font(Math.min(width, height) / 4));
        gc.fillText(text, width / 2, height / 2);

        // Convertir le canvas en image
        WritableImage writableImage = new WritableImage(width, height);
        canvas.snapshot(null, writableImage);

        return writableImage;
    }

    /**
     * Crée une icône pour le bouton "Voir"
     */
    public static Image createViewIcon(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond bleu
        gc.setFill(Color.web("#2196F3"));
        gc.fillOval(0, 0, size, size);

        // Dessiner un œil
        gc.setFill(Color.WHITE);
        gc.fillOval(size * 0.25, size * 0.35, size * 0.5, size * 0.3);
        gc.setFill(Color.BLACK);
        gc.fillOval(size * 0.4, size * 0.4, size * 0.2, size * 0.2);

        WritableImage writableImage = new WritableImage(size, size);
        canvas.snapshot(null, writableImage);

        return writableImage;
    }

    /**
     * Crée une icône pour le bouton "Modifier"
     */
    public static Image createEditIcon(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond orange
        gc.setFill(Color.web("#FFC107"));
        gc.fillOval(0, 0, size, size);

        // Dessiner un crayon
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.1);

        // Tige du crayon
        double[] xPoints = {size * 0.3, size * 0.7, size * 0.7, size * 0.3};
        double[] yPoints = {size * 0.7, size * 0.3, size * 0.4, size * 0.8};
        gc.strokePolygon(xPoints, yPoints, 4);

        // Pointe du crayon
        gc.setFill(Color.WHITE);
        gc.fillPolygon(new double[]{size * 0.25, size * 0.35, size * 0.3},
                new double[]{size * 0.75, size * 0.65, size * 0.8}, 3);

        WritableImage writableImage = new WritableImage(size, size);
        canvas.snapshot(null, writableImage);

        return writableImage;
    }

    /**
     * Crée une icône pour le bouton "Supprimer"
     */
    public static Image createDeleteIcon(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond rouge
        gc.setFill(Color.web("#FF5252"));
        gc.fillOval(0, 0, size, size);

        // Dessiner une croix
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(size * 0.15);
        gc.strokeLine(size * 0.3, size * 0.3, size * 0.7, size * 0.7);
        gc.strokeLine(size * 0.3, size * 0.7, size * 0.7, size * 0.3);

        WritableImage writableImage = new WritableImage(size, size);
        canvas.snapshot(null, writableImage);

        return writableImage;
    }

    /**
     * Crée une image par défaut pour les cours
     */
    public static Image createDefaultCourseImage(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond gris clair
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, size, size);

        // Dessiner un livre
        gc.setFill(Color.WHITE);
        gc.fillRect(size * 0.2, size * 0.2, size * 0.6, size * 0.6);

        // Dessiner des lignes pour représenter des pages
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);
        for (int i = 1; i < 5; i++) {
            double y = size * (0.2 + i * 0.1);
            gc.strokeLine(size * 0.25, y, size * 0.75, y);
        }

        WritableImage writableImage = new WritableImage(size, size);
        canvas.snapshot(null, writableImage);

        return writableImage;
    }
    public static Image createGradientImage(int width, int height, String text, Color startColor, Color endColor) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Créer un dégradé linéaire
        LinearGradient gradient = new LinearGradient(
                0, 0, width, height, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, startColor),
                new javafx.scene.paint.Stop(1, endColor)
        );

        // Remplir le fond avec le dégradé
        gc.setFill(gradient);
        gc.fillRect(0, 0, width, height);

        // Ajouter un motif subtil
        gc.setFill(new Color(1, 1, 1, 0.05));
        for (int i = 0; i < width; i += 20) {
            for (int j = 0; j < height; j += 20) {
                gc.fillOval(i, j, 10, 10);
            }
        }

        // Ajouter le texte si fourni
        if (text != null && !text.isEmpty()) {
            // Configurer la police
            int fontSize = Math.min(width, height) / 10;
            gc.setFont(new javafx.scene.text.Font("Arial Bold", fontSize));
            gc.setFill(new Color(1, 1, 1, 0.3));

            // Centrer le texte
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);

            // Dessiner le texte
            gc.fillText(text, width / 2, height / 2);
        }

        // Convertir le canvas en image
        WritableImage image = new WritableImage(width, height);
        canvas.snapshot(null, image);
        return image;
    }
}