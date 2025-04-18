package util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    private static final int ROUNDS = 12;

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(ROUNDS));
    }

    public static boolean checkPassword(String plainPassword, String storedPassword) {
        // Si un des paramètres est null, retourner false
        if (plainPassword == null || storedPassword == null) {
            return false;
        }

        // Vérifier si le mot de passe stocké est un hash BCrypt
        // Les hash BCrypt commencent toujours par $2a$, $2b$ ou $2y$
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            try {
                // C'est un hash BCrypt, on utilise BCrypt.checkpw
                return BCrypt.checkpw(plainPassword, storedPassword);
            } catch (IllegalArgumentException e) {
                // Si le hash est mal formé, on compare en texte brut
                return plainPassword.equals(storedPassword);
            }
        } else {
            // Ce n'est pas un hash BCrypt, on compare en texte brut (mode transition)
            return plainPassword.equals(storedPassword);
        }
    }
}