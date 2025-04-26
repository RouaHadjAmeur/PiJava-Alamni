package services;
import Main.DatabaseConnection;
import model.Reclamation;
import model.Utilisateur;
import util.Session;
import services.ReponseReclamationService;
import model.ReponseReclamation;
import java.time.LocalDate;
import java.time.ZoneId;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class ReclamationServices implements Iservices<Reclamation> {

    private static final Logger LOGGER = Logger.getLogger(ReclamationServices.class.getName());

    // Remplace cette clé par ta vraie clé GeminiAI
    private static final String GEMINI_API_KEY = "AIzaSyDVwpbH46wq2B-15u_4JHvIQNlkzyMzEeo";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/embedding-001:embedContent?key=";

    Connection cnx;

    public ReclamationServices(){
        cnx = DatabaseConnection.getInstance().getCnx();
    }

    @Override
    public void add(Reclamation reclamation) {
        String req="INSERT INTO pijava.reclamation (user_email, objet, description, status, date_soumission, admin_mail, role, user_id, rating) VALUES (?, ?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement stm=cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            stm.setString(1, reclamation.getUser_email());
            stm.setString(2, reclamation.getObjet());
            stm.setString(3, reclamation.getDescription());
            stm.setString(4, reclamation.getStatus());
            stm.setDate(5, reclamation.getDate_soumission());
            stm.setString(6, reclamation.getAdmin_mail());
            stm.setString(7, reclamation.getRole());
            stm.setInt(8, reclamation.getUser_id());
            stm.setInt(9, reclamation.getRating());
            stm.executeUpdate();
            LOGGER.info("Reclamation added: " + reclamation);

            // Récupérer l'ID généré de la nouvelle réclamation
            int newReclamationId = -1;
            try (ResultSet generatedKeys = stm.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    newReclamationId = generatedKeys.getInt(1);
                }
            }

            // 1. Récupérer toutes les anciennes réclamations résolues
            List<Reclamation> anciennes = new ArrayList<>();
            String sql = "SELECT * FROM reclamation WHERE status = 'Résolue'";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Reclamation r = new Reclamation();
                    r.setId(rs.getInt("id"));
                    r.setObjet(rs.getString("objet"));
                    r.setDescription(rs.getString("description"));
                    anciennes.add(r);
                }
            }

            // 2. Comparer la nouvelle réclamation à chaque ancienne
            double maxScore = 0;
            Reclamation plusProche = null;
            for (Reclamation ancienne : anciennes) {
                double score = getSimilarityGemini(
                    reclamation.getObjet() + " " + reclamation.getDescription(),
                    ancienne.getObjet() + " " + ancienne.getDescription()
                );
                if (score > maxScore) {
                    maxScore = score;
                    plusProche = ancienne;
                }
            }

            // 3. Si similarité >= 0.85, créer une réponse automatique
            if (maxScore >= 0.85 && plusProche != null) {
                // Récupérer la réponse de l'ancienne réclamation
                ReponseReclamationService repService = new ReponseReclamationService();
                List<ReponseReclamation> reponses = repService.getReponsesByReclamationId(plusProche.getId());
                if (!reponses.isEmpty()) {
                    ReponseReclamation ancienneRep = reponses.get(0); // On prend la première réponse
                    // Créer une nouvelle réponse automatique
                    ReponseReclamation autoRep = new ReponseReclamation();
                    Reclamation rec = new Reclamation();
                    rec.setId(newReclamationId);
                    autoRep.setReclamation(rec);
                    autoRep.setAdminId(ancienneRep.getAdminId());
                    autoRep.setContenue("[Réponse automatique] " + ancienneRep.getContenue());
                    autoRep.setDateReponse(java.sql.Date.valueOf(LocalDate.now(ZoneId.systemDefault())));
                    repService.add(autoRep);
                    LOGGER.info("Réponse automatique ajoutée à la réclamation ID: " + newReclamationId);
                }
            }

            // Send email notification to admin
            try {
                String subject = "Nouvelle réclamation reçue";
                String content = String.format(
                    "<h2>Nouvelle réclamation</h2>" +
                    "<p><strong>De:</strong> %s</p>" +
                    "<p><strong>Objet:</strong> %s</p>" +
                    "<p><strong>Description:</strong> %s</p>" +
                    "<p><strong>Date:</strong> %s</p>",
                    reclamation.getUser_email(),
                    reclamation.getObjet(),
                    reclamation.getDescription(),
                    reclamation.getDate_soumission()
                );
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to send email notification for new reclamation", e);
                // Continue execution even if email fails
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding reclamation", e);
            throw new RuntimeException("Failed to add reclamation", e);
        }
    }

    // Appel à l'API GeminiAI pour la similarité (cosine similarity entre embeddings)
    private double getSimilarityGemini(String texte1, String texte2) {
        try {
            double[] emb1 = getGeminiEmbedding(texte1);
            double[] emb2 = getGeminiEmbedding(texte2);
            if (emb1 == null || emb2 == null) return 0.0;
            return cosineSimilarity(emb1, emb2);
        } catch (Exception e) {
            LOGGER.warning("Erreur GeminiAI: " + e.getMessage());
            return 0.0;
        }
    }

    // Appel Gemini pour obtenir l'embedding d'un texte
    private double[] getGeminiEmbedding(String texte) throws Exception {
        URL url = new URL(GEMINI_API_URL + GEMINI_API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String jsonInput = "{\"content\":{\"parts\":[{\"text\":\"" + texte.replace("\"", "\\\"") + "\"}]}}";
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInput.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        int code = conn.getResponseCode();
        if (code != 200) throw new RuntimeException("Gemini API HTTP error: " + code);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            JsonObject obj = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (!obj.has("embedding")) return null;
            var arr = obj.getAsJsonObject("embedding").getAsJsonArray("values");
            double[] vec = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = arr.get(i).getAsDouble();
            }
            return vec;
        }
    }

    // Calcul de la similarité cosinus entre deux vecteurs
    private double cosineSimilarity(double[] v1, double[] v2) {
        double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        return (norm1 == 0 || norm2 == 0) ? 0.0 : dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public void modify(Reclamation reclamation) {

        String req = "UPDATE reclamation SET user_email=?, objet=?, description=?, status=?, date_soumission=?, admin_mail=?, role=? WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, reclamation.getUser_email());
            stm.setString(2, reclamation.getObjet());
            stm.setString(3, reclamation.getDescription());
            stm.setString(4, reclamation.getStatus());
            stm.setDate(5, reclamation.getDate_soumission());
            stm.setString(6, reclamation.getAdmin_mail());
            stm.setString(7, reclamation.getRole());
            stm.setInt(8, reclamation.getId());

            stm.executeUpdate();
            LOGGER.info("Reclamation updated: " + reclamation);

            // Send email notification to admin if the update is from user
            if (!Session.getUtilisateurConnecte().getRole().equals("ADMINISTRATEUR")) {
                try {
                    String subject = "Mise à jour de réclamation";
                    String content = String.format(
                        "<h2>Mise à jour de réclamation</h2>" +
                        "<p><strong>De:</strong> %s</p>" +
                        "<p><strong>Objet:</strong> %s</p>" +
                        "<p><strong>Description:</strong> %s</p>" +
                        "<p><strong>Nouveau statut:</strong> %s</p>" +
                        "<p><strong>Date:</strong> %s</p>",
                        reclamation.getUser_email(),
                        reclamation.getObjet(),
                        reclamation.getDescription(),
                        reclamation.getStatus(),
                        reclamation.getDate_soumission()
                    );
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to send email notification for reclamation update", e);
                    // Continue execution even if email fails
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating reclamation", e);
            throw new RuntimeException("Failed to update reclamation", e);
        }
    }

    @Override
    public List<Reclamation> afficher() {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT * FROM reclamation";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setUser_email(rs.getString("user_email"));
                r.setObjet(rs.getString("objet"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                r.setDate_soumission(rs.getDate("date_soumission"));
                r.setAdmin_mail(rs.getString("admin_mail"));
                r.setRole(rs.getString("role"));
                r.setUser_id(rs.getInt("user_id"));
                r.setRating(rs.getInt("rating"));
                System.out.println("Loaded reclamation ID: " + r.getId() + " with rating: " + r.getRating());
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Error loading reclamations: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void delete(int id){
        String req = "DELETE FROM reclamation WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public Reclamation getOne(int id) {
        String req = "SELECT * FROM reclamation WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new Reclamation(
                        rs.getInt("id"),
                        rs.getString("user_email"),
                        rs.getString("objet"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getDate("date_soumission"),
                        rs.getString("admin_mail"),
                        rs.getString("role"),
                        rs.getInt("user_id"),
                        rs.getInt("rating")
                                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void update(Reclamation reclamation) {
        String req = "UPDATE reclamation SET status=? WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, reclamation.getStatus());
            stm.setInt(2, reclamation.getId());
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Reclamation> getMesReclamations() {
        List<Reclamation> list = new ArrayList<>();

        try {
            Utilisateur user = Session.getUtilisateurConnecte();

            String req = "SELECT * FROM reclamation WHERE user_email = ?";
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, user.getEmail());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setUser_email(rs.getString("user_email"));
                r.setObjet(rs.getString("objet"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                r.setDate_soumission(rs.getDate("date_soumission"));
                r.setAdmin_mail(rs.getString("admin_mail"));
                r.setRole(rs.getString("role"));
                r.setUser_id(rs.getInt("user_id"));
                r.setRating(rs.getInt("rating"));

                list.add(r);
            }

        } catch (SQLException e) {
            System.out.println("Erreur getMesReclamations : " + e.getMessage());
        }

        return list;
    }

    public List<Reclamation> rechercherParEmail(String email) {
        List<Reclamation> resultats = new ArrayList<>();
        String req = "SELECT * FROM reclamation WHERE user_email LIKE ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + email + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setUser_email(rs.getString("user_email"));
                r.setObjet(rs.getString("objet"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                r.setDate_soumission(rs.getDate("date_soumission"));
                r.setAdmin_mail(rs.getString("admin_mail"));
                r.setRole(rs.getString("role"));
                r.setUser_id(rs.getInt("user_id"));
                r.setRating(rs.getInt("rating"));
                resultats.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultats;
    }


    public List<Reclamation> rechercherParObjet(String objet) {
        List<Reclamation> resultats = new ArrayList<>();
        String req = "SELECT * FROM reclamation WHERE objet LIKE ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + objet + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setUser_email(rs.getString("user_email"));
                r.setObjet(rs.getString("objet"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                r.setDate_soumission(rs.getDate("date_soumission"));
                r.setAdmin_mail(rs.getString("admin_mail"));
                r.setRole(rs.getString("role"));
                r.setUser_id(rs.getInt("user_id"));
                r.setRating(rs.getInt("rating"));
                resultats.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultats;
    }

    public List<Reclamation> rechercherParStatut(String statut) {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT * FROM reclamation WHERE status LIKE ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + statut + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setUser_email(rs.getString("user_email"));
                r.setObjet(rs.getString("objet"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                r.setDate_soumission(rs.getDate("date_soumission"));
                r.setAdmin_mail(rs.getString("admin_mail"));
                r.setRole(rs.getString("role"));
                r.setUser_id(rs.getInt("user_id"));
                r.setRating(rs.getInt("rating"));
                list.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Reclamation> trierParDate(String ordre) {
        List<Reclamation> list = new ArrayList<>();
        String query = "SELECT * FROM reclamation ORDER BY date_soumission " + ("Plus anciennes".equalsIgnoreCase(ordre) ? "ASC" : "DESC");

        try (PreparedStatement ps = cnx.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Reclamation r = mapResultSetToReclamation(rs);
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private Reclamation mapResultSetToReclamation(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setUser_email(rs.getString("user_email"));
        r.setObjet(rs.getString("objet"));
        r.setDescription(rs.getString("description"));
        r.setStatus(rs.getString("status"));
        r.setDate_soumission(rs.getDate("date_soumission"));
        r.setAdmin_mail(rs.getString("admin_mail"));
        r.setRole(rs.getString("role"));
        r.setUser_id(rs.getInt("user_id"));
        r.setRating(rs.getInt("rating"));
        return r;
    }

    public void updateRating(Reclamation reclamation) {
        String req = "UPDATE reclamation SET rating = ? WHERE id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, reclamation.getRating());
            stm.setInt(2, reclamation.getId());
            int result = stm.executeUpdate();
            System.out.println("Rating updated for reclamation ID: " + reclamation.getId() + 
                             " New rating: " + reclamation.getRating() +
                             " Rows affected: " + result);
        } catch (SQLException e) {
            System.err.println("Error updating rating: " + e.getMessage());
            throw new RuntimeException("Failed to update rating", e);
        }
    }

}


