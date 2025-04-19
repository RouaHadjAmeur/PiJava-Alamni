package model;

public class Enseignant extends Utilisateur {

    public Enseignant(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password, "ENSEIGNANT");
    }

    @Override
    public String getDetailsRole() {
        return "Enseignant : accès aux cours et aux classes.";
    }
}