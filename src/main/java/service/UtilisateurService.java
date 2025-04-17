package service;

import model.*;
import util.DatabaseConnection;
import util.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {

    // Constants pour normaliser les noms dans tout le code
    private static final String TABLE_NAME = "utilisateur";
    private static final String COL_ID = "id";
    private static final String COL_NOM = "nom";
    private static final String COL_PRENOM = "prenom";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";
    private static final String COL_ROLE = "role";
    private static final String COL_PENDING = "pending";
    private static final String COL_PHOTO = "photo";
    private static final String COL_NIVEAU = "niveau";
    private static final String COL_NOM_NIVEAU = "nom_niveau";

    /**
     * Vérifie si un email existe déjà dans la base de données
     * @param email l'email à vérifier
     * @return true si l'email existe, false sinon
     */
    public static boolean emailExiste(String email) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + COL_EMAIL + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'email : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Inscrit un nouvel utilisateur dans la base de données
     * @param user l'utilisateur à inscrire
     */
    public static void inscrire(Utilisateur user) {
        String sql = "INSERT INTO " + TABLE_NAME + " (" +
                COL_NOM + ", " + COL_PRENOM + ", " + COL_EMAIL + ", " +
                COL_PASSWORD + ", " + COL_ROLE + ", " + COL_PENDING + ", " +
                COL_PHOTO + ", " + COL_NIVEAU + ", " + COL_NOM_NIVEAU +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Hacher le mot de passe avant l'insertion
            String hashedPassword = PasswordUtils.hashPassword(user.getPassword());

            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, hashedPassword);  // Utiliser le mot de passe haché
            stmt.setString(5, user.getRole());
            stmt.setBoolean(6, user.isPending());
            stmt.setString(7, user.getPhoto());

            // Valeurs pour niveau et nom_niveau
            if (user instanceof Eleve) {
                Eleve eleve = (Eleve) user;
                stmt.setString(8, eleve.getNiveau());
                stmt.setString(9, eleve.getNomNiveau());
            } else {
                stmt.setNull(8, Types.VARCHAR);
                stmt.setNull(9, Types.VARCHAR);
            }

            stmt.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    user.setId(id);
                }
            }

            System.out.println("✅ Utilisateur inscrit avec succès : " + user.getEmail());

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'inscription : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'inscription", e);
        }
    }

    /**
     * Authentifie un utilisateur
     * @param email l'email de l'utilisateur
     * @param password le mot de passe (non haché)
     * @return l'utilisateur si authentifié, null sinon
     */
    public static Utilisateur login(String email, String password) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_EMAIL + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Récupérer le mot de passe stocké
                    String storedPassword = rs.getString(COL_PASSWORD);

                    // Vérifier si le mot de passe correspond
                    if (PasswordUtils.checkPassword(password, storedPassword)) {
                        Utilisateur user = mapper(rs);

                        // Migration: si le mot de passe est en texte brut, le convertir en hash
                        if (!(storedPassword.startsWith("$2a$") ||
                                storedPassword.startsWith("$2b$") ||
                                storedPassword.startsWith("$2y$"))) {
                            migrerMotDePasse(user.getId(), password);
                        }

                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la connexion : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Crée un objet Utilisateur à partir d'un ResultSet
     * @param rs ResultSet contenant les données de l'utilisateur
     * @return Un objet Utilisateur correspondant aux données
     * @throws SQLException En cas d'erreur lors de l'accès aux données
     */
    private static Utilisateur mapper(ResultSet rs) throws SQLException {
        int id = rs.getInt(COL_ID);
        String nom = rs.getString(COL_NOM);
        String prenom = rs.getString(COL_PRENOM);
        String email = rs.getString(COL_EMAIL);
        String password = rs.getString(COL_PASSWORD);
        String role = rs.getString(COL_ROLE);
        boolean pending = rs.getBoolean(COL_PENDING);
        String photo = rs.getString(COL_PHOTO);

        Utilisateur user;

        switch (role) {
            case "ÉLÈVE":
                Eleve eleve = new Eleve(nom, prenom, email, password);
                eleve.setNiveau(rs.getString(COL_NIVEAU));
                eleve.setNomNiveau(rs.getString(COL_NOM_NIVEAU));
                user = eleve;
                break;

            case "ENSEIGNANT":
                user = new Enseignant(nom, prenom, email, password);
                break;

            case "PARENT":
                user = new ParentUser(nom, prenom, email, password);
                break;

            case "ADMINISTRATEUR":
                user = new Administrateur(nom, prenom, email, password);
                break;

            default:
                throw new SQLException("Rôle inconnu : " + role);
        }

        user.setId(id);
        user.setPending(pending);
        user.setPhoto(photo);

        return user;
    }

    /**
     * Migre un mot de passe en texte brut vers un format haché
     * @param userId ID de l'utilisateur
     * @param plainPassword Mot de passe en texte brut
     */
    public static void migrerMotDePasse(int userId, String plainPassword) {
        String sql = "UPDATE " + TABLE_NAME + " SET " + COL_PASSWORD + " = ? WHERE " + COL_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hacher le mot de passe
            String hashedPassword = PasswordUtils.hashPassword(plainPassword);

            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

            System.out.println("✅ Migration réussie: Mot de passe haché pour l'utilisateur ID: " + userId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la migration du mot de passe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Récupère tous les utilisateurs
     * @return la liste des utilisateurs
     */
    public static List<Utilisateur> lister() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_ID + " DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                try {
                    Utilisateur user = mapper(rs);
                    utilisateurs.add(user);
                } catch (SQLException e) {
                    System.err.println("Erreur lors du mapping d'un utilisateur: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des utilisateurs : " + e.getMessage());
            e.printStackTrace();
        }

        return utilisateurs;
    }

    /**
     * Récupère un utilisateur par son ID
     * @param id l'identifiant de l'utilisateur
     * @return l'utilisateur ou null s'il n'existe pas
     */
    public static Utilisateur getById(int id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapper(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Met à jour les informations d'un utilisateur
     * @param user l'utilisateur à mettre à jour
     * @return true si la mise à jour a réussi, false sinon
     */
    public static boolean modifier(Utilisateur user) {
        // Construction dynamique de la requête SQL selon le cas de mise à jour
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE ").append(TABLE_NAME).append(" SET ");
        sqlBuilder.append(COL_NOM).append(" = ?, ");
        sqlBuilder.append(COL_PRENOM).append(" = ?, ");
        sqlBuilder.append(COL_EMAIL).append(" = ?, ");

        // Si le mot de passe est fourni, l'inclure dans la mise à jour
        boolean updatePassword = user.getPassword() != null && !user.getPassword().isEmpty();
        if (updatePassword) {
            sqlBuilder.append(COL_PASSWORD).append(" = ?, ");
        }

        sqlBuilder.append(COL_PHOTO).append(" = ?, ");
        sqlBuilder.append(COL_PENDING).append(" = ?");

        // Ajouter la condition WHERE
        sqlBuilder.append(" WHERE ").append(COL_ID).append(" = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, user.getNom());
            stmt.setString(paramIndex++, user.getPrenom());
            stmt.setString(paramIndex++, user.getEmail());

            // Si update du mot de passe, hacher si nécessaire
            if (updatePassword) {
                String password = user.getPassword();
                // Vérifier si le mot de passe est déjà un hash BCrypt
                if (!(password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"))) {
                    // Le hacher s'il ne l'est pas
                    password = PasswordUtils.hashPassword(password);
                }
                stmt.setString(paramIndex++, password);
            }

            stmt.setString(paramIndex++, user.getPhoto());
            stmt.setBoolean(paramIndex++, user.isPending());
            stmt.setInt(paramIndex, user.getId());

            int rowsUpdated = stmt.executeUpdate();

            // Si c'est un élève, mettre à jour les informations spécifiques
            if (user instanceof Eleve) {
                Eleve eleve = (Eleve) user;

                String eleveUpdateSql = "UPDATE " + TABLE_NAME +
                        " SET " + COL_NIVEAU + " = ?, " +
                        COL_NOM_NIVEAU + " = ? WHERE " +
                        COL_ID + " = ?";

                try (PreparedStatement stmtEleve = conn.prepareStatement(eleveUpdateSql)) {
                    stmtEleve.setString(1, eleve.getNiveau());
                    stmtEleve.setString(2, eleve.getNomNiveau());
                    stmtEleve.setInt(3, user.getId());
                    stmtEleve.executeUpdate();
                }
            }

            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'utilisateur : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime un utilisateur de la base de données
     * @param id l'identifiant de l'utilisateur à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public static boolean supprimer(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE " + COL_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsDeleted = stmt.executeUpdate();
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'utilisateur : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Valide un compte en attente
     * @param id l'identifiant de l'utilisateur à valider
     * @return true si la validation a réussi, false sinon
     */
    public static boolean validerCompte(int id) {
        String sql = "UPDATE " + TABLE_NAME + " SET " + COL_PENDING + " = false WHERE " + COL_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la validation du compte : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère les comptes en attente de validation
     * @return la liste des utilisateurs en attente de validation
     */
    public static List<Utilisateur> getComptesEnAttente() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_PENDING + " = true";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Utilisateur user = mapper(rs);
                    utilisateurs.add(user);
                } catch (SQLException e) {
                    System.err.println("Erreur lors du mapping d'un utilisateur en attente: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des comptes en attente : " + e.getMessage());
            e.printStackTrace();
        }

        return utilisateurs;
    }

    /**
     * Change le mot de passe d'un utilisateur
     * @param userId l'identifiant de l'utilisateur
     * @param newPassword le nouveau mot de passe (non haché)
     * @return true si le changement a réussi, false sinon
     */
    public static boolean changerMotDePasse(int userId, String newPassword) {
        String sql = "UPDATE " + TABLE_NAME + " SET " + COL_PASSWORD + " = ? WHERE " + COL_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hacher le nouveau mot de passe
            String hashedPassword = PasswordUtils.hashPassword(newPassword);

            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors du changement de mot de passe : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Vérifie si un mot de passe est correct pour un utilisateur
     * @param userId l'identifiant de l'utilisateur
     * @param password le mot de passe à vérifier (non haché)
     * @return true si le mot de passe est correct, false sinon
     */
    public static boolean verifierMotDePasse(int userId, String password) {
        String sql = "SELECT " + COL_PASSWORD + " FROM " + TABLE_NAME + " WHERE " + COL_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString(COL_PASSWORD);
                    return PasswordUtils.checkPassword(password, storedPassword);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du mot de passe : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}