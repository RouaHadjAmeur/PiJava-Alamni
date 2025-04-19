package util;

import model.Utilisateur;

public class Session {

    private static Utilisateur utilisateurConnecte;

    public static void setUtilisateurConnecte(Utilisateur user) {
        utilisateurConnecte = user;
    }

    public static Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public static void clear() {
        utilisateurConnecte = null;
    }
}