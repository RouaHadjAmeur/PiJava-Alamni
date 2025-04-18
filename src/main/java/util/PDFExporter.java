package util;

import model.Eleve;
import model.Utilisateur;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFExporter {

    private static final PDFont TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont HEADER_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont REGULAR_FONT = PDType1Font.HELVETICA;
    private static final PDFont ITALIC_FONT = PDType1Font.HELVETICA_OBLIQUE;

    private static final float MARGIN = 50;
    private static final float ROW_HEIGHT = 25f;
    private static final int ROWS_PER_PAGE = 20;

    // Couleurs (conversion des valeurs RGB en fractions 0-1 comme requis par PDFBox)
    private static final float[] HEADER_BG_COLOR = new float[] { 0.23f, 0.35f, 0.6f }; // Bleu foncé
    private static final float[] HEADER_TEXT_COLOR = new float[] { 1f, 1f, 1f }; // Blanc
    private static final float[] ALT_ROW_COLOR = new float[] { 0.95f, 0.95f, 0.95f }; // Gris très clair
    private static final float[] BORDER_COLOR = new float[] { 0.7f, 0.7f, 0.7f }; // Gris moyen
    private static final float[] TITLE_COLOR = new float[] { 0.2f, 0.2f, 0.2f }; // Presque noir

    public static void exportUtilisateurs(List<Utilisateur> utilisateurs, String filePath) throws IOException {
        PDDocument document = new PDDocument();

        // Compteur de pages pour la pagination
        int totalPages = 1 + (utilisateurs.size() / ROWS_PER_PAGE);
        int currentPage = 1;

        // Génération de la première page
        generatePage(document, utilisateurs, 0, currentPage, totalPages);

        // Génération des pages suivantes si nécessaire
        for (int i = ROWS_PER_PAGE; i < utilisateurs.size(); i += ROWS_PER_PAGE) {
            currentPage++;
            generatePage(document, utilisateurs, i, currentPage, totalPages);
        }

        document.save(filePath);
        document.close();
    }

    private static void generatePage(PDDocument document, List<Utilisateur> utilisateurs,
                                     int startIndex, int pageNumber, int totalPages) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float tableWidth = pageWidth - 2 * MARGIN;

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // Fond blanc
        contentStream.setNonStrokingColor(1f, 1f, 1f);
        contentStream.addRect(0, 0, pageWidth, pageHeight);
        contentStream.fill();

        float yStart = pageHeight - MARGIN;
        float yPosition = yStart;

        // --- ENTÊTE DU DOCUMENT ---

        // Logo (si disponible)
        try {
            PDImageXObject logo = PDImageXObject.createFromFile("src/main/resources/images/logo_alamni.png", document);
            float logoWidth = 60;
            float logoHeight = 60;
            contentStream.drawImage(logo, MARGIN, yPosition - logoHeight, logoWidth, logoHeight);
        } catch (Exception e) {
            System.err.println("Logo non trouvé: " + e.getMessage());
        }

        // Titre avec couleur et style améliorés
        contentStream.setNonStrokingColor(TITLE_COLOR[0], TITLE_COLOR[1], TITLE_COLOR[2]);
        contentStream.beginText();
        contentStream.setFont(TITLE_FONT, 24);
        contentStream.newLineAtOffset(MARGIN + 70, yPosition - 30);
        contentStream.showText("LISTE DES UTILISATEURS");
        contentStream.endText();

        // Date d'export
        contentStream.setNonStrokingColor(0.4f, 0.4f, 0.4f);
        contentStream.beginText();
        contentStream.setFont(ITALIC_FONT, 10);
        contentStream.newLineAtOffset(MARGIN + 70, yPosition - 50);
        contentStream.showText("Export généré le: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        contentStream.endText();

        // Ligne de séparation élégante
        contentStream.setStrokingColor(0.8f, 0.8f, 0.8f);
        contentStream.setLineWidth(1f);
        contentStream.moveTo(MARGIN, yPosition - 70);
        contentStream.lineTo(pageWidth - MARGIN, yPosition - 70);
        contentStream.stroke();

        yPosition = yPosition - 100;

        // --- TABLEAU DES UTILISATEURS ---

        // Définition des colonnes et largeurs
        String[] headers = {"Nom", "Prénom", "Email", "Rôle", "Niveau"};
        float[] colWidths = {
                tableWidth * 0.18f,  // Nom
                tableWidth * 0.18f,  // Prénom
                tableWidth * 0.3f,   // Email
                tableWidth * 0.14f,  // Rôle
                tableWidth * 0.2f    // Niveau
        };

        // En-tête du tableau avec arrière-plan coloré
        float headerHeight = 30;
        contentStream.setNonStrokingColor(HEADER_BG_COLOR[0], HEADER_BG_COLOR[1], HEADER_BG_COLOR[2]);
        contentStream.addRect(MARGIN, yPosition - headerHeight, tableWidth, headerHeight);
        contentStream.fill();

        // Texte des en-têtes en blanc
        contentStream.setNonStrokingColor(HEADER_TEXT_COLOR[0], HEADER_TEXT_COLOR[1], HEADER_TEXT_COLOR[2]);
        float xPosition = MARGIN + 10;
        contentStream.beginText();
        contentStream.setFont(HEADER_FONT, 12);

        for (int i = 0; i < headers.length; i++) {
            contentStream.newLineAtOffset(xPosition, yPosition - 20);
            contentStream.showText(headers[i]);
            contentStream.newLineAtOffset(-xPosition, -yPosition + 20); // Reset position
            xPosition += colWidths[i];
        }
        contentStream.endText();

        yPosition -= headerHeight;

        // Contenu du tableau avec lignes alternées
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, utilisateurs.size());
        boolean alternate = false;

        contentStream.setFont(REGULAR_FONT, 10);

        for (int i = startIndex; i < endIndex; i++) {
            Utilisateur utilisateur = utilisateurs.get(i);

            // Alternance de couleurs pour les lignes
            if (alternate) {
                contentStream.setNonStrokingColor(ALT_ROW_COLOR[0], ALT_ROW_COLOR[1], ALT_ROW_COLOR[2]);
                contentStream.addRect(MARGIN, yPosition - ROW_HEIGHT, tableWidth, ROW_HEIGHT);
                contentStream.fill();
            }
            alternate = !alternate;

            // Ligne des données
            contentStream.setNonStrokingColor(0.1f, 0.1f, 0.1f); // Texte presque noir

            // Données de l'utilisateur
            String[] rowData = new String[5];
            rowData[0] = utilisateur.getNom();
            rowData[1] = utilisateur.getPrenom();
            rowData[2] = utilisateur.getEmail();
            rowData[3] = utilisateur.getRole();

            // Niveau (pour les élèves)
            if (utilisateur instanceof Eleve) {
                Eleve eleve = (Eleve) utilisateur;
                rowData[4] = eleve.getNiveau() + " - " + eleve.getNomNiveau();
            } else {
                rowData[4] = "";
            }

            // Affichage des données
            xPosition = MARGIN + 10;
            contentStream.beginText();

            for (int j = 0; j < rowData.length; j++) {
                contentStream.newLineAtOffset(xPosition, yPosition - 15);
                String text = rowData[j] != null ? rowData[j] : "";
                // Tronquer le texte s'il est trop long pour la colonne
                if (text.length() > 30 && j == 2) { // Pour l'email notamment
                    text = text.substring(0, 27) + "...";
                }
                contentStream.showText(text);
                contentStream.newLineAtOffset(-xPosition, -yPosition + 15); // Reset position
                xPosition += colWidths[j];
            }
            contentStream.endText();

            yPosition -= ROW_HEIGHT;
        }

        // Bordure du tableau
        contentStream.setStrokingColor(BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
        contentStream.setLineWidth(1f);
        contentStream.addRect(MARGIN, yPosition, tableWidth, (endIndex - startIndex) * ROW_HEIGHT + headerHeight);
        contentStream.stroke();

        // --- PIED DE PAGE ---

        // Pagination
        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
        contentStream.beginText();
        contentStream.setFont(REGULAR_FONT, 10);
        contentStream.newLineAtOffset(pageWidth / 2 - 30, 30);
        contentStream.showText("Page " + pageNumber + " sur " + totalPages);
        contentStream.endText();

        // Copyright
        contentStream.beginText();
        contentStream.setFont(REGULAR_FONT, 8);
        contentStream.newLineAtOffset(MARGIN, 15);
        contentStream.showText("Alamni - Système de Gestion Scolaire © " +
                LocalDateTime.now().getYear());
        contentStream.endText();

        contentStream.close();
    }
}