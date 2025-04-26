package services;

import Main.DatabaseConnection;
import model.Administrateur;
import model.Reclamation;
import model.ReponseReclamation;
import model.Utilisateur;
import service.UtilisateurService;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReponseReclamationService implements Iservices<ReponseReclamation> {

    Connection cnx;

    public ReponseReclamationService() {
        cnx = DatabaseConnection.getInstance().getCnx();
    }

    @Override
    public void add(ReponseReclamation r) {
        String req = "INSERT INTO pijava.reponsereclamation (reclamation_id_id, admin_id_id, contenue, date_reponse) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, r.getReclamation().getId());
            ps.setInt(2, r.getAdminId());
            ps.setString(3, r.getContenue());
            ps.setDate(4, r.getDateReponse());
            ps.executeUpdate();
            System.out.println("Réponse ajoutée avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la réponse : " + e.getMessage());
        }
    }

    @Override
    public void modify(ReponseReclamation r) {
        String req = "UPDATE reponsereclamation SET contenue=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, r.getContenue());
            ps.setInt(2, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        String req = "DELETE FROM reponsereclamation WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ReponseReclamation> afficher() {
        return List.of(); // inutilisé ici
    }

    @Override
    public ReponseReclamation getOne(int id) {
        return null;  // optionnel si jamais tu le développes après
    }

    public List<ReponseReclamation> getByReclamationId(int reclamationId) {
        List<ReponseReclamation> reponses = new ArrayList<>();

        String req = "SELECT rr.*, " +
                "r.objet, r.user_email, r.description, r.status, r.date_soumission, r.admin_mail, " +
                "u.nom, u.prenom, u.email " +
                "FROM reponsereclamation rr " +
                "JOIN reclamation r ON rr.reclamation_id_id = r.id " +
                "JOIN utilisateurs u ON rr.admin_id_id = u.id " +
                "WHERE rr.reclamation_id_id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ReponseReclamation rep = new ReponseReclamation();
                rep.setId(rs.getInt("id"));
                rep.setAdminId(rs.getInt("admin_id_id"));
                rep.setContenue(rs.getString("contenue"));
                rep.setDateReponse(rs.getDate("date_reponse"));
                rep.setUserReponse(rs.getString("user_reponse")); // 👈 ajout important

                Reclamation reclamation = new Reclamation();
                reclamation.setId(rs.getInt("reclamation_id_id"));
                reclamation.setObjet(rs.getString("objet"));
                reclamation.setUser_email(rs.getString("user_email"));
                reclamation.setDescription(rs.getString("description"));
                reclamation.setStatus(rs.getString("status"));
                reclamation.setDate_soumission(rs.getDate("date_soumission"));
                reclamation.setAdmin_mail(rs.getString("admin_mail"));

                rep.setReclamation(reclamation);

                Administrateur admin = new Administrateur();
                admin.setId(rs.getInt("admin_id_id"));
                admin.setNom(rs.getString("nom"));
                admin.setPrenom(rs.getString("prenom"));
                admin.setEmail(rs.getString("email"));

                rep.setAdmin(admin);
                reponses.add(rep);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des réponses : " + e.getMessage());
        }
        return reponses;
    }





    public List<ReponseReclamation> getReponsesByReclamationId(int reclamationId) {
        List<ReponseReclamation> reponses = new ArrayList<>();

        String req = "SELECT * FROM reponsereclamation WHERE reclamation_id_id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, reclamationId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ReponseReclamation rep = new ReponseReclamation();
                rep.setId(rs.getInt("id"));
                rep.setAdminId(rs.getInt("admin_id_id"));
                rep.setContenue(rs.getString("contenue"));
                rep.setDateReponse(rs.getDate("date_reponse"));
                rep.setUserReponse(rs.getString("user_reponse")); // 👈 ajout ici aussi

                UtilisateurService utilisateurService = new UtilisateurService();
                Utilisateur admin = utilisateurService.getById(rs.getInt("admin_id_id"));
                rep.setAdmin((Administrateur) admin);

                reponses.add(rep);
            }

        } catch (SQLException e) {
            System.out.println("Erreur getReponsesByReclamationId : " + e.getMessage());
        }

        return reponses;
    }

    public void updateUserReponse(int reponseId, String userReponse) {
        String req = "UPDATE reponsereclamation SET user_reponse = ? WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, userReponse);
            ps.setInt(2, reponseId);
            ps.executeUpdate();
            System.out.println("Réponse utilisateur enregistrée avec succès.");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la réponse utilisateur : " + e.getMessage());
        }
    }

    public int countUnreadResponsesByUserEmail(String userEmail) {
        int count = 0;
        String sql = "SELECT COUNT(r.id) " +
                "FROM reponsereclamation r " +
                "JOIN reclamation rec ON r.reclamation_id_id = rec.id " +
                "WHERE rec.user_email = ? AND r.is_read = 0";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, userEmail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public void markResponsesAsRead(String userEmail, int reclamationId) {
        String sql = "UPDATE reponsereclamation r " +
                "JOIN reclamation rec ON r.reclamation_id_id = rec.id " +
                "SET r.is_read = true " +
                "WHERE rec.user_email = ? AND rec.id = ? AND r.is_read = 0";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, userEmail);
            ps.setInt(2, reclamationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public  Map<Integer, Integer> getUnreadCountPerReclamation(String userEmail) {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT r.reclamation_id_id, COUNT(*) as total " +
                "FROM reponsereclamation r " +
                "JOIN reclamation rec ON r.reclamation_id_id = rec.id " +
                "WHERE rec.user_email = ? AND r.is_read = 0 " +
                "GROUP BY r.reclamation_id_id";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
           // System.out.println("Unread counts map: " + map);

            ps.setString(1, userEmail);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getInt("reclamation_id_id"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }

    public void markAllResponsesAsRead(String userEmail) {
        String sql = "UPDATE reponsereclamation r " +
                "JOIN reclamation rec ON r.reclamation_id_id = rec.id " +
                "SET r.is_read = true " +
                "WHERE rec.user_email = ? AND r.is_read = 0";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, userEmail);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
