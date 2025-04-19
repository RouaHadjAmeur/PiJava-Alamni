package model;

public class Eleve extends Utilisateur {
    private String niveau;
    private String nomNiveau;

    public Eleve(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password, "ÉLÈVE");
    }

    @Override
    public String getDetailsRole() {
        return "Niveau : " + niveau + " - " + nomNiveau;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getNomNiveau() {
        return nomNiveau;
    }

    public void setNomNiveau(String nomNiveau) {
        this.nomNiveau = nomNiveau;
    }
}