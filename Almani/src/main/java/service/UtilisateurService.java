package service;

import model.*;
import util.DatabaseConnection;
import util.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

public class UtilisateurService {

    // Méthode de vérification si un email existe déjà
    public static boolean emailExiste(String email) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(rs, stmt, conn);
        }
    }

    // Méthode d'authentification
    public static Utilisateur login(String email, String password) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM utilisateurs WHERE email = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Récupérer les données de l'utilisateur
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String storedPassword = rs.getString("password");
                String role = rs.getString("roles");
                boolean isPending = rs.getBoolean("isPending");
                String photo = rs.getString("photo");
                String niveau = rs.getString("niveau");
                String nomNiveau = rs.getString("nom_niveau");

                // Logs pour débogage
                System.out.println("Login - Utilisateur trouvé: ID=" + id + ", Nom=" + nom +
                        ", Email=" + email + ", Role=" + role + ", isPending=" + isPending);

                // Vérifier si le compte est en attente
                if (isPending) {
                    System.out.println("Login refusé: compte en attente de validation");
                    return null; // Compte non validé
                }

                // Vérifier le mot de passe
                if (PasswordHasher.checkPassword(password, storedPassword)) {
                    System.out.println("Mot de passe vérifié avec succès");

                    // Si le mot de passe était en clair, le mettre à jour avec un hash
                    if (storedPassword != null && !storedPassword.startsWith("$2a$")) {
                        updatePasswordHash(id, PasswordHasher.hashPassword(password), conn);
                    }

                    // Extraire le rôle du format JSON si nécessaire
                    String roleNormalise = extraireRoleDepuisJson(role);
                    System.out.println("Rôle normalisé: " + roleNormalise);

                    // Créer l'utilisateur selon son rôle
                    Utilisateur user;

                    switch (roleNormalise.toUpperCase()) {
                        case "ADMINISTRATEUR":
                            user = new Administrateur(nom, prenom, email, "");
                            break;
                        case "ÉLÈVE":
                        case "ELEVE":
                            Eleve eleve = new Eleve(nom, prenom, email, "");
                            eleve.setNiveau(niveau);
                            eleve.setNomNiveau(nomNiveau);
                            user = eleve;
                            break;
                        case "ENSEIGNANT":
                            user = new Enseignant(nom, prenom, email, "");
                            break;
                        case "PARENT":
                            user = new ParentUser(nom, prenom, email, "");
                            break;
                        default:
                            System.out.println("Rôle non reconnu: " + roleNormalise);
                            return null; // Rôle non reconnu
                    }

                    // Configurer l'utilisateur
                    user.setId(id);
                    user.setPending(isPending);
                    user.setPhoto(photo);

                    return user;
                } else {
                    System.out.println("Mot de passe incorrect");
                }
            } else {
                System.out.println("Aucun utilisateur trouvé avec l'email: " + email);
            }

            return null; // Utilisateur non trouvé ou mot de passe incorrect

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeResources(rs, stmt, conn);
        }
    }

    // Extraire le rôle du format JSON ou simple
    private static String extraireRoleDepuisJson(String rolesJson) {
        if (rolesJson == null) return "";
        if (rolesJson.startsWith("{\"role\":\"")) {
            return rolesJson
                    .replace("{\"role\":\"", "")
                    .replace("\"}", "")
                    .trim();
        }
        return rolesJson.trim(); // Format simple
    }

    // Mettre à jour le hash du mot de passe
    private static void updatePasswordHash(int userId, String hashedPassword, Connection conn) {
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE utilisateurs SET password = ? WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            int updated = stmt.executeUpdate();
            System.out.println("Mise à jour du mot de passe pour ID=" + userId + " : " + (updated > 0 ? "succès" : "échec"));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Inscription d'un nouvel utilisateur
    public static boolean inscrire(Utilisateur user) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // S'assurer que l'utilisateur est en attente de validation
            user.setPending(true);

            conn = DatabaseConnection.getConnection();

            System.out.println("Inscription: Nom=" + user.getNom() + ", Email=" + user.getEmail() +
                    ", Role=" + user.getRole() + ", isPending=" + user.isPending());

            if (user instanceof Eleve) {
                Eleve eleve = (Eleve) user;
                String query = "INSERT INTO utilisateurs (nom, prenom, email, password, roles, isPending, photo, niveau, nom_niveau) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, user.getNom());
                stmt.setString(2, user.getPrenom());
                stmt.setString(3, user.getEmail());
                stmt.setString(4, PasswordHasher.hashPassword(user.getPassword()));
                stmt.setString(5, user.getRole()); // Format simple
                stmt.setBoolean(6, user.isPending());
                stmt.setString(7, user.getPhoto());
                stmt.setString(8, eleve.getNiveau());
                stmt.setString(9, eleve.getNomNiveau());

                System.out.println("Inscription élève avec Niveau=" + eleve.getNiveau() + ", NomNiveau=" + eleve.getNomNiveau());
            } else {
                String query = "INSERT INTO utilisateurs (nom, prenom, email, password, roles, isPending, photo) VALUES (?, ?, ?, ?, ?, ?, ?)";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, user.getNom());
                stmt.setString(2, user.getPrenom());
                stmt.setString(3, user.getEmail());
                stmt.setString(4, PasswordHasher.hashPassword(user.getPassword()));
                stmt.setString(5, user.getRole()); // Format simple
                stmt.setBoolean(6, user.isPending());
                stmt.setString(7, user.getPhoto());
            }

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Inscription: " + (rowsAffected > 0 ? "succès" : "échec"));
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur d'inscription: " + e.getMessage());
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Récupérer tous les utilisateurs
    public static List<Utilisateur> findAll() {
        System.out.println("Récupération de tous les utilisateurs...");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Utilisateur> utilisateurs = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM utilisateurs";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String email = rs.getString("email");
                String role = rs.getString("roles");
                boolean isPending = rs.getBoolean("isPending");
                String photo = rs.getString("photo");
                String niveau = rs.getString("niveau");
                String nomNiveau = rs.getString("nom_niveau");

                System.out.println("FindAll - Utilisateur trouvé: ID=" + id + ", Nom=" + nom +
                        ", Email=" + email + ", Role brut=" + role + ", isPending=" + isPending);

                // Extraire le rôle si besoin
                String roleNormalise = extraireRoleDepuisJson(role);
                System.out.println("  Rôle normalisé: " + roleNormalise);

                // Créer le bon type d'utilisateur selon le rôle
                Utilisateur user;

                switch (roleNormalise.toUpperCase()) {
                    case "ADMINISTRATEUR":
                        user = new Administrateur(nom, prenom, email, "");
                        break;
                    case "ÉLÈVE":
                    case "ELEVE":
                        Eleve eleve = new Eleve(nom, prenom, email, "");
                        eleve.setNiveau(niveau);
                        eleve.setNomNiveau(nomNiveau);
                        user = eleve;
                        break;
                    case "ENSEIGNANT":
                        user = new Enseignant(nom, prenom, email, "");
                        break;
                    case "PARENT":
                        user = new ParentUser(nom, prenom, email, "");
                        break;
                    default:
                        System.out.println("  Rôle non reconnu, ignoré: " + roleNormalise);
                        continue; // Ignorer cet utilisateur
                }

                // Configurer l'utilisateur
                user.setId(id);
                user.setPhoto(photo);
                user.setPending(isPending);

                System.out.println("  Utilisateur ajouté à la liste: " + user.getNom() + ", En attente=" + user.isPending());
                utilisateurs.add(user);
            }

            System.out.println("Total utilisateurs récupérés: " + utilisateurs.size());
            System.out.println("Utilisateurs en attente: " + utilisateurs.stream().filter(Utilisateur::isPending).count());

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
        } finally {
            closeResources(rs, stmt, conn);
        }

        return utilisateurs;
    }

    // Mettre à jour un utilisateur
    public static boolean updateUtilisateur(Utilisateur utilisateur) {
        System.out.println("Mise à jour de l'utilisateur ID=" + utilisateur.getId() +
                ", isPending=" + utilisateur.isPending() +
                ", photo=" + utilisateur.getPhoto());

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Pour un élève, mettre à jour les champs spécifiques
            if (utilisateur instanceof Eleve) {
                Eleve eleve = (Eleve) utilisateur;
                String query = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?, isPending = ?, " +
                        "niveau = ?, nom_niveau = ?, roles = ?, photo = ? WHERE id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, utilisateur.getNom());
                stmt.setString(2, utilisateur.getPrenom());
                stmt.setString(3, utilisateur.getEmail());
                stmt.setBoolean(4, utilisateur.isPending());
                stmt.setString(5, eleve.getNiveau());
                stmt.setString(6, eleve.getNomNiveau());
                stmt.setString(7, utilisateur.getRole());
                stmt.setString(8, utilisateur.getPhoto()); // Ajout du champ photo
                stmt.setInt(9, utilisateur.getId());
            } else {
                String query = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?, isPending = ?, " +
                        "roles = ?, photo = ? WHERE id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, utilisateur.getNom());
                stmt.setString(2, utilisateur.getPrenom());
                stmt.setString(3, utilisateur.getEmail());
                stmt.setBoolean(4, utilisateur.isPending());
                stmt.setString(5, utilisateur.getRole());
                stmt.setString(6, utilisateur.getPhoto()); // Ajout du champ photo
                stmt.setInt(7, utilisateur.getId());
            }

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Mise à jour: " + (rowsAffected > 0 ? "succès" : "échec"));
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur de mise à jour: " + e.getMessage());
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Supprimer un utilisateur
    public static boolean supprimer(int id) {
        System.out.println("Suppression de l'utilisateur ID=" + id);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "DELETE FROM utilisateurs WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Suppression: " + (rowsAffected > 0 ? "succès" : "échec"));
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Migrer les mots de passe existants vers BCrypt
    public static void migrerMotsDePasse() {
        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Sélectionner tous les utilisateurs dont le mot de passe n'est pas au format BCrypt
            String selectQuery = "SELECT id, password FROM utilisateurs WHERE password NOT LIKE '$2a$%'";
            selectStmt = conn.prepareStatement(selectQuery);
            rs = selectStmt.executeQuery();

            // Préparer la requête de mise à jour
            String updateQuery = "UPDATE utilisateurs SET password = ? WHERE id = ?";
            updateStmt = conn.prepareStatement(updateQuery);

            int count = 0;

            // Pour chaque utilisateur, hacher le mot de passe
            while (rs.next()) {
                int id = rs.getInt("id");
                String plainTextPassword = rs.getString("password");

                if (plainTextPassword != null && !plainTextPassword.isEmpty()) {
                    String hashedPassword = PasswordHasher.hashPassword(plainTextPassword);

                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();

                    count++;
                    System.out.println("Mot de passe mis à jour pour l'utilisateur ID: " + id);
                }
            }

            System.out.println("Migration terminée. " + count + " mots de passe ont été mis à jour.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la migration: " + e.getMessage());
        } finally {
            closeResources(rs, selectStmt, conn);
            try {
                if (updateStmt != null) updateStmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Méthode de test pour créer un utilisateur en attente
    public static void creerCompteTest() {
        System.out.println("Création d'un compte test en attente...");

        // Créer un email unique avec timestamp
        String email = "test" + System.currentTimeMillis() + "@test.com";

        Eleve test = new Eleve("Test", "Utilisateur", email, "123456");
        test.setPending(true);
        test.setNiveau("Collège");
        test.setNomNiveau("6ème");

        if (inscrire(test)) {
            System.out.println("✅ Compte test créé avec succès: " + email);
            System.out.println("  isPending=" + test.isPending());
        } else {
            System.out.println("❌ Échec de création du compte test");
        }
    }

    // Ajoutez cette nouvelle méthode à votre classe UtilisateurService.java
    public static Utilisateur getUserById(int userId) {
        System.out.println("Récupération de l'utilisateur avec ID=" + userId);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM utilisateurs WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Récupérer les données de l'utilisateur
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String email = rs.getString("email");
                String password = rs.getString("password");
                String role = rs.getString("roles");
                boolean isPending = rs.getBoolean("isPending");
                String photo = rs.getString("photo");
                String niveau = rs.getString("niveau");
                String nomNiveau = rs.getString("nom_niveau");

                System.out.println("GetUserById - Utilisateur trouvé: ID=" + id + ", Nom=" + nom +
                        ", Email=" + email + ", Role=" + role);

                // Extraire le rôle du format JSON si nécessaire
                String roleNormalise = extraireRoleDepuisJson(role);

                // Créer le bon type d'utilisateur selon le rôle
                Utilisateur user;

                switch (roleNormalise.toUpperCase()) {
                    case "ADMINISTRATEUR":
                        user = new Administrateur(nom, prenom, email, "");
                        break;
                    case "ÉLÈVE":
                    case "ELEVE":
                        Eleve eleve = new Eleve(nom, prenom, email, "");
                        eleve.setNiveau(niveau);
                        eleve.setNomNiveau(nomNiveau);
                        user = eleve;
                        break;
                    case "ENSEIGNANT":
                        user = new Enseignant(nom, prenom, email, "");
                        break;
                    case "PARENT":
                        user = new ParentUser(nom, prenom, email, "");
                        break;
                    default:
                        System.out.println("Rôle non reconnu: " + roleNormalise);
                        return null; // Rôle non reconnu
                }

                // Configurer l'utilisateur
                user.setId(id);
                user.setPhoto(photo);
                user.setPending(isPending);
                // Important: préserver le mot de passe haché
                //user.passwordHash = password;
                user.setPassword(password);

                return user;
            } else {
                System.out.println("Aucun utilisateur trouvé avec l'ID: " + userId);
            }

            return null; // Utilisateur non trouvé

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeResources(rs, stmt, conn);
        }
    }
    // Utilitaire pour fermer les ressources
    private static void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Méthode pour mettre à jour uniquement le mot de passe
    public static boolean updatePassword(int userId, String nouveauMotDePasse) {
        System.out.println("Mise à jour du mot de passe pour l'utilisateur ID=" + userId);
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "UPDATE utilisateurs SET password = ? WHERE id = ?";
            stmt = conn.prepareStatement(query);

            // Hasher le nouveau mot de passe
            String hashedPassword = PasswordHasher.hashPassword(nouveauMotDePasse);

            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Mise à jour mot de passe: " + (rowsAffected > 0 ? "succès" : "échec"));
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Ajouter cette méthode à UtilisateurService.java
    public static void migrerCheminsPhotos() {
        System.out.println("Démarrage de la migration des chemins de photos...");

        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Sélectionner tous les utilisateurs avec des chemins absolus
            String selectQuery = "SELECT id, photo FROM utilisateurs WHERE photo LIKE 'C:%' OR photo LIKE '/Users/%' OR photo LIKE '%\\\\%'";
            selectStmt = conn.prepareStatement(selectQuery);
            rs = selectStmt.executeQuery();

            // Préparer la requête de mise à jour
            String updateQuery = "UPDATE utilisateurs SET photo = ? WHERE id = ?";
            updateStmt = conn.prepareStatement(updateQuery);

            int count = 0;

            // Pour chaque utilisateur, générer un nouveau chemin
            while (rs.next()) {
                int id = rs.getInt("id");
                String photoPath = rs.getString("photo");

                if (photoPath != null && !photoPath.isEmpty()) {
                    // Utiliser un chemin standard pour tous ces utilisateurs
                    String nouveauChemin = "images/users/default_avatar.png";

                    updateStmt.setString(1, nouveauChemin);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();

                    count++;
                    System.out.println("Photo migrée pour ID=" + id + ": " + photoPath + " -> " + nouveauChemin);
                }
            }

            System.out.println("Migration terminée: " + count + " chemins de photos ont été mis à jour.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la migration des chemins: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (selectStmt != null) selectStmt.close();
                if (updateStmt != null) updateStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    // Méthode pour la vérification d'email (adapte la méthode existante emailExiste)
    public static boolean emailExists(String email) {
        return emailExiste(email);
    }






    // Mettre à jour le mot de passe pour un email donné
    public static boolean updatePassword(String email, String newPassword) {
        System.out.println("Mise à jour du mot de passe pour: " + email);
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "UPDATE utilisateurs SET password = ? WHERE email = ?";
            stmt = conn.prepareStatement(query);

            // Hasher le nouveau mot de passe
            String hashedPassword = PasswordHasher.hashPassword(newPassword);

            stmt.setString(1, hashedPassword);
            stmt.setString(2, email);

            int rowsAffected = stmt.executeUpdate();



            System.out.println("Mise à jour mot de passe par email: " + (rowsAffected > 0 ? "succès" : "échec"));
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Map pour stocker les tokens de réinitialisation (en production, utilisez une base de données)
    private static final Map<String, ResetToken> resetTokens = new HashMap<>();

    // Classe interne pour stocker les tokens avec leur date d'expiration
    private static class ResetToken {
        private final String email;
        private final LocalDateTime expiration;

        public ResetToken(String email) {
            this.email = email;
            this.expiration = LocalDateTime.now().plusHours(24);
        }

        public String getEmail() {
            return email;
        }

        public boolean isValid() {
            return LocalDateTime.now().isBefore(expiration);
        }
    }






    // Supprimer un token après utilisation
    public static void removeResetToken(String token) {
        resetTokens.remove(token);
    }

    // Mettre à jour le mot de passe avec un token
    public static boolean updatePasswordWithToken(String token, String newPassword) {
        Utilisateur utilisateur = getUtilisateurParToken(token);  // ✅ fixé

        if (utilisateur != null) {
            boolean success = updatePassword(utilisateur.getEmail(), newPassword);
            if (success) {
                removeResetToken(token);
            }
            return success;
        }

        return false;
    }




    // Stockage d’un token pour un utilisateur
    public static void associerTokenAvecUtilisateur(String email, String token) {
        resetTokens.put(token, new ResetToken(email));
        System.out.println("✅ Token ajouté : " + token + " pour " + email);
    }

    // Vérifie un token et retourne l’utilisateur s’il est encore valide
    public static Utilisateur getUtilisateurParToken(String token) {
        ResetToken resetToken = resetTokens.get(token);

        if (resetToken != null && resetToken.isValid()) {
            return findByEmail(resetToken.getEmail());
        }

        return null;
    }

    public static Utilisateur findByEmail(String email) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM utilisateurs WHERE email = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String password = rs.getString("password");
                String role = rs.getString("roles");
                boolean isPending = rs.getBoolean("isPending");
                String photo = rs.getString("photo");
                String niveau = rs.getString("niveau");
                String nomNiveau = rs.getString("nom_niveau");

                String roleNormalise = extraireRoleDepuisJson(role);
                Utilisateur user;

                switch (roleNormalise.toUpperCase()) {
                    case "ADMINISTRATEUR":
                        user = new Administrateur(nom, prenom, email, "");
                        break;
                    case "ÉLÈVE":
                    case "ELEVE":
                        Eleve eleve = new Eleve(nom, prenom, email, "");
                        eleve.setNiveau(niveau);
                        eleve.setNomNiveau(nomNiveau);
                        user = eleve;
                        break;
                    case "ENSEIGNANT":
                        user = new Enseignant(nom, prenom, email, "");
                        break;
                    case "PARENT":
                        user = new ParentUser(nom, prenom, email, "");
                        break;
                    default:
                        return null;
                }

                user.setId(id);
                user.setPassword(password);
                user.setPending(isPending);
                user.setPhoto(photo);

                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }





}