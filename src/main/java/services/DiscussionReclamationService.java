package services;

import Main.DatabaseConnection;
import model.DiscussionReclamation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiscussionReclamationService {

    private final Connection cnx;

    public DiscussionReclamationService() {
        cnx = DatabaseConnection.getInstance().getCnx();
    }

    // ✅ Ajouter un message (admin ou user)
    public void ajouter(DiscussionReclamation dr) {
        String req = "INSERT INTO discussion_reclamation (reclamation_id , reponse_id, auteur_email, auteur_role, contenu, date_reponse) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, dr.getReclamationId());
            ps.setInt(2, dr.getReponseId());
            ps.setString(3, dr.getAuteurEmail());
            ps.setString(4, dr.getAuteurRole());
            ps.setString(5, dr.getContenu());
            ps.setDate(6, dr.getDateReponse());


            ps.executeUpdate();
            System.out.println("Message de discussion ajouté avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    // ✅ Récupérer tous les messages d'une réclamation
    public List<DiscussionReclamation> getByReclamationId(int reclamationId) {
        List<DiscussionReclamation> list = new ArrayList<>();
        String req = "SELECT * FROM discussion_reclamation WHERE reclamation_id = ? ORDER BY date_reponse ASC, id ASC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiscussionReclamation dr = new DiscussionReclamation();
                dr.setId(rs.getInt("id"));
                dr.setReclamationId(rs.getInt("reclamation_id"));
                dr.setAuteurEmail(rs.getString("auteur_email"));
                dr.setAuteurRole(rs.getString("auteur_role"));
                dr.setContenu(rs.getString("contenu"));
                dr.setDateReponse(rs.getDate("date_reponse"));

                list.add(dr);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture : " + e.getMessage());
        }

        return list;
    }

    public List<DiscussionReclamation> getByReponseId(int reponseId) {
        List<DiscussionReclamation> discussions = new ArrayList<>();
        String sql = "SELECT * FROM discussion_reclamation WHERE reponse_id = ? ORDER BY date_reponse ASC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, reponseId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiscussionReclamation dr = new DiscussionReclamation();
                dr.setId(rs.getInt("id"));
                dr.setReclamationId(rs.getInt("reclamation_id"));
                dr.setReponseId(rs.getInt("reponse_id"));
                dr.setAuteurEmail(rs.getString("auteur_email"));
                dr.setAuteurRole(rs.getString("auteur_role"));
                dr.setContenu(rs.getString("contenu"));
                dr.setDateReponse(rs.getDate("date_reponse"));
                discussions.add(dr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des discussions : " + e.getMessage());
        }

        return discussions;
    }

}
