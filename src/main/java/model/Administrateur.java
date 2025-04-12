package model;

public class Administrateur extends Utilisateur {

    public Administrateur(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password, "ADMINISTRATEUR");
    }

    @Override
    public String getDetailsRole() {
        return "Administrateur de la plateforme.";
    }
}