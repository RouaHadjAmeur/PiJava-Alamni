package services;
import Main.DatabaseConnection;
import model.Reclamation;
import model.Utilisateur;
import util.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ReclamationServices implements Iservices<Reclamation> {

    private static final Logger LOGGER = Logger.getLogger(ReclamationServices.class.getName());

    Connection cnx;

    public ReclamationServices(){
        cnx = DatabaseConnection.getInstance().getCnx();
    }

    @Override
    public void add(Reclamation reclamation) {

        String req="INSERT INTO pijava.reclamation (user_email, objet, description, status, date_soumission, admin_mail, role, user_id) VALUES (?, ?,?,?,?,?,?,?)";
        try {
            PreparedStatement stm=cnx.prepareStatement(req);
            stm.setString(1, reclamation.getUser_email());
            stm.setString(2, reclamation.getObjet());
            stm.setString(3, reclamation.getDescription());
            stm.setString(4, reclamation.getStatus());
            stm.setDate(5, reclamation.getDate_soumission());
            stm.setString(6, reclamation.getAdmin_mail());
            stm.setString(7, reclamation.getRole());
            stm.setInt(8, reclamation.getUser_id());
            
            stm.executeUpdate();
            LOGGER.info("Reclamation added: " + reclamation);

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


