package model;

public abstract class Utilisateur {
    public abstract String getDetailsRole();
    protected int id;
    protected String nom;
    protected String prenom;
    protected String email;
    protected String password;
    protected String photo;
    protected boolean isPending;
    protected String role;

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String email, String password, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isPending = true;
    }

    // Getters & Setters communs ⬇
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public boolean isPending() { return isPending; }
    public void setPending(boolean pending) { isPending = pending; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}