package services;
import Main.DatabaseConnection;
import model.Reclamation;
import model.Utilisateur;
import util.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ReclamationServices implements Iservices<Reclamation> {

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
            System.out.println("Ajoutée : " + reclamation);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Reclamation> afficher() {
        List<Reclamation> reclamations=new ArrayList<>();

        String req="SELECT * FROM reclamation";
        try {
            Statement stm=cnx.createStatement();
            ResultSet rs= stm.executeQuery(req);

            while (rs.next()){
                Reclamation rec=new Reclamation();
                rec.setId(rs.getInt(1));
                rec.setUser_email(rs.getString("user_email"));
                rec.setObjet(rs.getString("objet"));
                rec.setDescription(rs.getString("description"));
                rec.setStatus(rs.getString("status"));
                rec.setDate_soumission(rs.getDate("date_soumission"));
                rec.setAdmin_mail(rs.getString("admin_mail"));
                rec.setStatus(rs.getString("status"));
                rec.setRole(rs.getString("role"));
                rec.setUser_id(rs.getInt("user_id"));




                reclamations.add(rec);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println(reclamations);
        return reclamations;
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
                        rs.getInt("user_id")
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
                list.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }


}


