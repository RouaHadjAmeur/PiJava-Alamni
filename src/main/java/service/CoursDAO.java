package service;

import model.Cours;
import util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CoursDAO {
    private Connection connection;

    public CoursDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // Méthode publique pour vérifier et renouveler la connexion si nécessaire
    public boolean checkAndRenewConnection() {
        try {
            checkConnection();
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la connexion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Méthode privée pour vérifier et récupérer la connexion
    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Reconnexion à la base de données...");
            connection = DatabaseConnection.getInstance().getConnection();
        }
    }

    // Méthode pour ajouter un cours
    public boolean ajouter(Cours cours) {
        try {
            checkConnection();
            String query = "INSERT INTO cours (titre, descr_c, matiere_c, date_c, niveau, image, support_c) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, cours.getTitre());
                pstmt.setString(2, cours.getDescr_c());
                pstmt.setString(3, cours.getMatiere_c());

                if (cours.getDate_c() != null) {
                    pstmt.setTimestamp(4, Timestamp.valueOf(cours.getDate_c()));
                } else {
                    pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                }

                pstmt.setString(5, cours.getNiveau());
                pstmt.setString(6, cours.getImage());
                pstmt.setString(7, cours.getSupport_c());

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        cours.setId(generatedKeys.getInt(1));
                    }
                    return true;
                }

                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du cours: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Méthode pour modifier un cours
    public boolean modifier(Cours cours) {
        try {
            checkConnection();
            String query = "UPDATE cours SET titre = ?, descr_c = ?, matiere_c = ?, niveau = ?, image = ?, support_c = ? WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, cours.getTitre());
                pstmt.setString(2, cours.getDescr_c());
                pstmt.setString(3, cours.getMatiere_c());
                pstmt.setString(4, cours.getNiveau());
                pstmt.setString(5, cours.getImage());
                pstmt.setString(6, cours.getSupport_c());
                pstmt.setInt(7, cours.getId());

                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du cours: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Méthode pour vérifier l'existence du cours avant suppression
    public boolean coursExiste(int id) {
        try {
            checkConnection();
            String query = "SELECT COUNT(*) FROM cours WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du cours: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Méthode pour supprimer un cours - version corrigée
    public boolean supprimer(int id) {
        System.out.println("Début de la méthode supprimer pour le cours ID: " + id);

        if (id <= 0) {
            System.err.println("ID de cours invalide: " + id);
            return false;
        }

        // Vérifier si le cours existe
        if (!coursExiste(id)) {
            System.err.println("Le cours avec l'ID " + id + " n'existe pas");
            return false;
        }

        Connection localConnection = null;
        PreparedStatement pstmtDevoirs = null;
        PreparedStatement pstmtCours = null;
        boolean success = false;

        try {
            checkConnection();
            localConnection = DatabaseConnection.getInstance().getConnection();
            localConnection.setAutoCommit(false);
            System.out.println("Transaction démarrée, autoCommit désactivé");

            // 1. Vérifier le nom de la table des devoirs
            DatabaseMetaData metaData = localConnection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            boolean devoirTableExists = false;
            boolean devoirsTableExists = false;

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME").toLowerCase();
                if (tableName.equals("devoir")) {
                    devoirTableExists = true;
                } else if (tableName.equals("devoirs")) {
                    devoirsTableExists = true;
                }
            }

            System.out.println("Table 'devoir' existe: " + devoirTableExists);
            System.out.println("Table 'devoirs' existe: " + devoirsTableExists);

            // 2. D'abord supprimer les devoirs associés en fonction de la table qui existe
            String devoirQuery = null;
            if (devoirTableExists) {
                // Utiliser la table "devoir"
                devoirQuery = "DELETE FROM devoir WHERE id_cours = ?";
            } else if (devoirsTableExists) {
                // Utiliser la table "devoirs"
                devoirQuery = "DELETE FROM devoirs WHERE id_cours = ?";
            } else {
                // Si aucune table n'existe, essayer avec la table originale
                devoirQuery = "DELETE FROM devoir WHERE id_cours = ?";
            }

            try {
                pstmtDevoirs = localConnection.prepareStatement(devoirQuery);
                pstmtDevoirs.setInt(1, id);
                int devoirsDeleted = pstmtDevoirs.executeUpdate();
                System.out.println("Devoirs supprimés: " + devoirsDeleted);
            } catch (SQLException e) {
                // Si la suppression des devoirs échoue, on essaie de continuer
                System.err.println("Avertissement: Impossible de supprimer les devoirs associés: " + e.getMessage());
                System.err.println("Tentative de suppression du cours malgré tout...");
            }

            // 3. Ensuite supprimer le cours
            pstmtCours = localConnection.prepareStatement("DELETE FROM cours WHERE id = ?");
            pstmtCours.setInt(1, id);
            int coursDeleted = pstmtCours.executeUpdate();
            System.out.println("Cours supprimés: " + coursDeleted);

            success = coursDeleted > 0;

            if (success) {
                localConnection.commit();
                System.out.println("Transaction validée (commit)");
            } else {
                localConnection.rollback();
                System.out.println("Transaction annulée (rollback) - aucun cours supprimé");
            }

            return success;
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de la suppression: " + e.getMessage());
            e.printStackTrace();

            try {
                if (localConnection != null) {
                    localConnection.rollback();
                    System.out.println("Transaction annulée (rollback) suite à une erreur");
                }
            } catch (SQLException ex) {
                System.err.println("Erreur lors de l'annulation de la transaction: " + ex.getMessage());
                ex.printStackTrace();
            }

            return false;
        } finally {
            try {
                if (pstmtDevoirs != null) pstmtDevoirs.close();
                if (pstmtCours != null) pstmtCours.close();
                if (localConnection != null) {
                    localConnection.setAutoCommit(true);
                    if (localConnection != connection) {
                        localConnection.close();
                    }
                }
                System.out.println("Ressources de la transaction libérées");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    // Méthode pour récupérer toutes les matières distinctes
    public List<String> getAllMatieres() {
        List<String> matieres = new ArrayList<>();

        try {
            checkConnection();
            String query = "SELECT DISTINCT matiere_c FROM cours WHERE matiere_c IS NOT NULL ORDER BY matiere_c";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String matiere = rs.getString("matiere_c");
                    if (matiere != null && !matiere.isEmpty()) {
                        matieres.add(matiere);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des matières: " + e.getMessage());
            e.printStackTrace();
        }

        return matieres;
    }

    // Méthode pour récupérer tous les niveaux distincts
    public List<String> getAllNiveaux() {
        List<String> niveaux = new ArrayList<>();

        try {
            checkConnection();
            String query = "SELECT DISTINCT niveau FROM cours WHERE niveau IS NOT NULL ORDER BY niveau";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String niveau = rs.getString("niveau");
                    if (niveau != null && !niveau.isEmpty()) {
                        niveaux.add(niveau);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des niveaux: " + e.getMessage());
            e.printStackTrace();
        }

        return niveaux;
    }
    // Méthode pour récupérer un cours par son ID
    public Cours getById(int id) {
        try {
            checkConnection();
            String query = "SELECT * FROM cours WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return extractCoursFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du cours: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Méthode pour récupérer tous les cours
    public List<Cours> getAll() {
        List<Cours> coursList = new ArrayList<>();

        try {
            checkConnection();
            String query = "SELECT * FROM cours ORDER BY date_c DESC";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    Cours cours = extractCoursFromResultSet(rs);
                    coursList.add(cours);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des cours: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    // Méthode pour récupérer les cours par matière
    public List<Cours> getByMatiere(String matiere) {
        List<Cours> coursList = new ArrayList<>();

        try {
            checkConnection();
            String query = "SELECT * FROM cours WHERE matiere_c LIKE ? ORDER BY date_c DESC";

            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, "%" + matiere + "%");
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Cours cours = extractCoursFromResultSet(rs);
                    coursList.add(cours);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des cours par matière: " + e.getMessage());
            e.printStackTrace();
        }

        return coursList;
    }

    // Méthode utilitaire pour extraire un cours d'un ResultSet
    private Cours extractCoursFromResultSet(ResultSet rs) throws SQLException {
        Cours cours = new Cours();
        cours.setId(rs.getInt("id"));
        cours.setTitre(rs.getString("titre"));
        cours.setDescr_c(rs.getString("descr_c"));
        cours.setMatiere_c(rs.getString("matiere_c"));

        Timestamp timestamp = rs.getTimestamp("date_c");
        if (timestamp != null) {
            cours.setDate_c(timestamp.toLocalDateTime());
        }

        cours.setNiveau(rs.getString("niveau"));
        cours.setImage(rs.getString("image"));
        cours.setSupport_c(rs.getString("support_c"));

        return cours;
    }
}