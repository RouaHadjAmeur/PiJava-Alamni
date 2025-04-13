package service;

import model.*;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {

    // ✅ Connexion avec filtre isPending
    public static Utilisateur login(String email, String password) {
        String sql = "SELECT * FROM pijava.utilisateurs WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String rolesJson = rs.getString("roles");
                String role = extraireRoleDepuisJson(rolesJson);
                boolean pending = rs.getBoolean("isPending");

                // Ne permettre la connexion que si validé OU admin
                if (!"ADMINISTRATEUR".equalsIgnoreCase(role) && pending) {
                    return null; // Refusé
                }

                return mapper(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ✅ Vérifie si l'email existe
    public static boolean emailExiste(String email) {
        String sql = "SELECT id FROM pijava.utilisateurs WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ✅ Inscription dans la base
    public static void inscrire(Utilisateur user) {
        String sql = "INSERT INTO pijava.utilisateurs (nom, prenom, email, password, photo, niveau, nom_niveau, roles, isPending) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getPhoto());

            if (user instanceof Eleve eleve) {
                stmt.setString(6, eleve.getNiveau());
                stmt.setString(7, eleve.getNomNiveau());
            } else {
                stmt.setNull(6, Types.VARCHAR);
                stmt.setNull(7, Types.VARCHAR);
            }

            String roleJson = "{\"role\":\"" + user.getRole().toUpperCase() + "\"}";
            stmt.setString(8, roleJson);
            stmt.setBoolean(9, user.isPending());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Dans UtilisateurService.java


    public static List<Utilisateur> lister() {
        // Simplement appeler la méthode existante
        return findAll();
    }

    // ✅ Lire tous les utilisateurs
    public static List<Utilisateur> findAll() {
        List<Utilisateur> utilisateurs = new ArrayList<>();

        String sql = "SELECT * FROM utilisateurs ORDER BY nom ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Utilisateur u = mapper(rs);
                if (u != null) {
                    utilisateurs.add(u);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return utilisateurs;
    }

    // ✅ Supprimer un utilisateur par ID
    public static void supprimer(int id) {
        String sql = "DELETE FROM utilisateurs WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ Modifier les coordonnées de l’utilisateur
    public static boolean updateUtilisateur(Utilisateur utilisateur) {
        String sql = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?, photo = ?, niveau = ?, nom_niveau = ?, roles = ?, isPending = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, utilisateur.getNom());
            stmt.setString(2, utilisateur.getPrenom());
            stmt.setString(3, utilisateur.getEmail());
            stmt.setString(4, utilisateur.getPhoto());

            if (utilisateur instanceof Eleve eleve) {
                stmt.setString(5, eleve.getNiveau());
                stmt.setString(6, eleve.getNomNiveau());
            } else {
                stmt.setNull(5, Types.VARCHAR);
                stmt.setNull(6, Types.VARCHAR);
            }

            stmt.setString(7, "{\"role\":\"" + utilisateur.getRole() + "\"}");
            stmt.setBoolean(8, utilisateur.isPending()); // 🔁 ici !
            stmt.setInt(9, utilisateur.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Mapper un utilisateur depuis la base
    private static Utilisateur mapper(ResultSet rs) {
        try {
            String roleJson = rs.getString("roles");
            String role = extraireRoleDepuisJson(roleJson);
            Utilisateur utilisateur;

            switch (role.toUpperCase()) {
                case "ÉLÈVE":
                    Eleve eleve = new Eleve(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("password")
                    );
                    eleve.setNiveau(rs.getString("niveau"));
                    eleve.setNomNiveau(rs.getString("nom_niveau"));
                    utilisateur = eleve;
                    break;

                case "ENSEIGNANT":
                    utilisateur = new Enseignant(rs.getString("nom"), rs.getString("prenom"), rs.getString("email"), rs.getString("password"));
                    break;

                case "PARENT":
                    utilisateur = new ParentUser(rs.getString("nom"), rs.getString("prenom"), rs.getString("email"), rs.getString("password"));
                    break;

                case "ADMINISTRATEUR":
                    utilisateur = new Administrateur(rs.getString("nom"), rs.getString("prenom"), rs.getString("email"), rs.getString("password"));
                    break;

                default:
                    return null; // rôle inconnu
            }

            utilisateur.setId(rs.getInt("id"));
            utilisateur.setPhoto(rs.getString("photo"));
            utilisateur.setPending(rs.getBoolean("isPending"));
            utilisateur.setRole(role); // important pour la navigation

            return utilisateur;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Méthode utilitaire : extraire "role" depuis {"role":"ÉLÈVE"}
    private static String extraireRoleDepuisJson(String rolesJson) {
        if (rolesJson != null && rolesJson.contains(":")) {
            return rolesJson
                    .replace("{\"role\":\"", "")
                    .replace("\"}", "")
                    .trim();
        }
        return "";
    }

    public Utilisateur getById(int id) {
        Utilisateur user = null;
        String req = "SELECT * FROM utilisateurs WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getConnection();
            //PreparedStatement stmt = conn.prepareStatement(sql)
            PreparedStatement ps = conn.prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                user = new Administrateur(); // ou new Eleve() si c'est autre role
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getById : " + e.getMessage());
        }
        return user;
    }

}