package services;

import Main.DatabaseConnection;
import model.Reclamation;
import model.ReponseReclamation;

import Main.DatabaseConnection;
import model.Reclamation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseReclamationService implements Iservices<ReponseReclamation>{
    Connection cnx;

    public ReponseReclamationService(){
        cnx = DatabaseConnection.getInstance().getCnx();
    }

    @Override
    public void add(ReponseReclamation r) {
        String req = "INSERT INTO pijava.reponsereclamation (reclamation_id_id, admin_id_id, contenue, date_reponse) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps=cnx.prepareStatement(req);
            ps.setInt(1, r.getReclamationId());
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
    public void modify(ReponseReclamation reponseReclamation) {
        String req = "UPDATE reponsereclamation SET contenue=? WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, reponseReclamation.getContenue());
            stm.setInt(2, reponseReclamation.getId());



            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ReponseReclamation> afficher() {
        return List.of();
    }

    @Override
    public void delete(int id){
        String req = "DELETE FROM reponsereclamation WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public ReponseReclamation getOne(int id) {
        return null;
    }

    public List<ReponseReclamation> getByReclamationId(int reclamationId) {
        List<ReponseReclamation> reponses = new ArrayList<>();
        //String req = "SELECT * FROM pijava.reponsereclamation WHERE reclamation_id_id = ? ORDER BY date_reponse ASC";
        String req = "SELECT rr.*, r.objet, r.user_email, r.description, r.status " +
                "FROM reponsereclamation rr " +
                "JOIN reclamation r ON rr.reclamation_id_id = r.id " +
                "WHERE rr.reclamation_id_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ReponseReclamation rep = new ReponseReclamation();
                rep.setId(rs.getInt("id"));
                rep.setReclamationId(rs.getInt("reclamation_id_id"));
                rep.setAdminId(rs.getInt("admin_id_id"));
                rep.setContenue(rs.getString("contenue"));
                rep.setDateReponse(rs.getDate("date_reponse"));

                reponses.add(rep);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des réponses : " + e.getMessage());
        }

        return reponses;
    }

}
