package model;

public class ParentUser extends Utilisateur {

    public ParentUser(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password, "PARENT");
    }

    @Override
    public String getDetailsRole() {
        return "Parent : accès aux informations de l'élève.";
    }
}