// Fichier: src/main/java/util/PasswordHasher.java
package util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    private static final int WORKLOAD = 12;

    public static String hashPassword(String plaintextPassword) {
        String salt = BCrypt.gensalt(WORKLOAD);
        return BCrypt.hashpw(plaintextPassword, salt);
    }

    public static boolean checkPassword(String plaintextPassword, String storedPassword) {
        // Si le mot de passe stocké est null ou vide
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }

        // Si le mot de passe n'est pas un hash BCrypt, comparer directement
        if (!storedPassword.startsWith("$2a$")) {
            System.out.println("Comparaison directe avec mot de passe non haché");
            return plaintextPassword.equals(storedPassword);
        }

        // Sinon, utiliser BCrypt pour comparer
        try {
            System.out.println("Vérification avec BCrypt");
            return BCrypt.checkpw(plaintextPassword, storedPassword);
        } catch (Exception e) {
            System.err.println("Erreur BCrypt: " + e.getMessage());
            // En cas d'erreur BCrypt, tenter une comparaison directe
            return plaintextPassword.equals(storedPassword);
        }
    }
}