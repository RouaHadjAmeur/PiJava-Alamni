package service;

import model.Devoir;
import util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DevoirDAO {
    private static final Logger LOGGER = Logger.getLogger(DevoirDAO.class.getName());
    private Connection connection;

    public DevoirDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // Méthode pour ajouter un devoir
    public boolean ajouter(Devoir devoir) {
        // Requête mise à jour pour inclure la colonne 'comment'
        String query = "INSERT INTO devoir (titre_d, descr_d, date_d, support_d, cours_id, comment) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            if (connection == null || connection.isClosed()) {
                connection = DatabaseConnection.getInstance().getConnection();
            }

            try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, devoir.getTitre_d());
                pstmt.setString(2, devoir.getDescr_d());

                if (devoir.getDate_d() != null) {
                    pstmt.setTimestamp(3, Timestamp.valueOf(devoir.getDate_d()));
                } else {
                    pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                }

                pstmt.setString(4, devoir.getSupport_d());
                pstmt.setInt(5, devoir.getId_cours());

                // Ajout de la valeur pour la colonne 'comment'
                // Si votre classe Devoir n'a pas cette propriété, utilisez une valeur par défaut
                String comment = devoir.getDescr_d(); // Utiliser la description comme commentaire par défaut
                pstmt.setString(6, comment != null ? comment : "");

                LOGGER.info("Exécution de la requête SQL: " + pstmt.toString());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        devoir.setId(generatedKeys.getInt(1));
                        LOGGER.info("Devoir ajouté avec succès, ID: " + devoir.getId());
                    }
                    return true;
                }

                LOGGER.warning("Aucune ligne affectée lors de l'ajout du devoir");
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors de l'ajout du devoir: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception lors de l'ajout du devoir: " + e.getMessage(), e);
            return false;
        }
    }

    // Méthode pour modifier un devoir
    public boolean modifier(Devoir devoir) {
        String query = "UPDATE devoir SET titre_d = ?, descr_d = ?, date_d = ?, support_d = ?, " +
                "cours_id = ?, comment = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, devoir.getTitre_d());
            pstmt.setString(2, devoir.getDescr_d());

            if (devoir.getDate_d() != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(devoir.getDate_d()));
            } else {
                pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            }

            pstmt.setString(4, devoir.getSupport_d());
            pstmt.setInt(5, devoir.getId_cours());

            // Utiliser la description comme commentaire par défaut
            pstmt.setString(6, devoir.getDescr_d() != null ? devoir.getDescr_d() : "");

            pstmt.setInt(7, devoir.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la modification du devoir: " + e.getMessage(), e);
            return false;
        }
    }

    // Méthode pour supprimer un devoir
    public boolean supprimer(int id) {
        String query = "DELETE FROM devoir WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du devoir: " + e.getMessage(), e);
            return false;
        }
    }

    // Méthode pour récupérer un devoir par son ID
    public Devoir getById(int id) {
        String query = "SELECT * FROM devoir WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractDevoirFromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération du devoir: " + e.getMessage(), e);
        }

        return null;
    }

    // Méthode pour récupérer tous les devoirs
    public List<Devoir> getAll() {
        List<Devoir> devoirList = new ArrayList<>();
        String query = "SELECT * FROM devoir ORDER BY date_d DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Devoir devoir = extractDevoirFromResultSet(rs);
                devoirList.add(devoir);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des devoirs: " + e.getMessage(), e);
        }

        return devoirList;
    }

    // Méthode pour récupérer les devoirs par cours
    public List<Devoir> getByCours(int idCours) {
        List<Devoir> devoirList = new ArrayList<>();
        // Utilisation de 'cours_id' au lieu de 'id_cours'
        String query = "SELECT * FROM devoir WHERE cours_id = ? ORDER BY date_d DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idCours);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Devoir devoir = extractDevoirFromResultSet(rs);
                devoirList.add(devoir);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des devoirs par cours: " + e.getMessage(), e);
        }

        return devoirList;
    }

    // Méthode utilitaire pour extraire un devoir d'un ResultSet
    private Devoir extractDevoirFromResultSet(ResultSet rs) throws SQLException {
        Devoir devoir = new Devoir();
        devoir.setId(rs.getInt("id"));
        devoir.setTitre_d(rs.getString("titre_d"));
        devoir.setDescr_d(rs.getString("descr_d"));

        Timestamp timestamp = rs.getTimestamp("date_d");
        if (timestamp != null) {
            devoir.setDate_d(timestamp.toLocalDateTime());
        }

        devoir.setSupport_d(rs.getString("support_d"));

        // Utiliser 'cours_id' au lieu de 'id_cours'
        devoir.setId_cours(rs.getInt("cours_id"));

        // Si vous décidez d'ajouter le champ 'comment' à votre classe Devoir,
        // vous pouvez le récupérer ici
        // devoir.setComment(rs.getString("comment"));

        return devoir;
    }
}