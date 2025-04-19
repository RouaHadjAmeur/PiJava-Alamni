package model;

import java.sql.Date;

public class ReponseReclamation {
    private int id;
    private Reclamation reclamation;
    private Administrateur admin;
    private String contenue;
    private Date dateReponse;
    private String userReponse;


    public ReponseReclamation() {}

    public ReponseReclamation(Reclamation reclamation, Administrateur admin, String contenue, Date dateReponse, String userReponse) {
        this.reclamation = reclamation;
        this.admin = admin;
        this.contenue = contenue;
        this.dateReponse = dateReponse;
        this.userReponse = userReponse;
    }

    public ReponseReclamation(int id, Reclamation reclamation, Administrateur admin, String contenue, Date dateReponse, String userReponse) {
        this.id = id;
        this.reclamation = reclamation;
        this.admin = admin;
        this.contenue = contenue;
        this.dateReponse = dateReponse;
        this.userReponse = userReponse;

    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenue() { return contenue; }
    public void setContenue(String contenue) { this.contenue = contenue; }

    public Date getDateReponse() { return dateReponse; }
    public void setDateReponse(Date dateReponse) { this.dateReponse = dateReponse; }

    public Reclamation getReclamation() { return reclamation; }
    public void setReclamation(Reclamation reclamation) { this.reclamation = reclamation; }

    public Administrateur getAdmin() { return admin; }
    public void setAdmin(Administrateur admin) { this.admin = admin; }

    // Getter
    public int getAdminId() {
        return admin != null ? admin.getId() : 0;
    }

    // Setter
    public void setAdminId(int adminId) {
        this.admin = new Administrateur();
        this.admin.setId(adminId);
    }

    public String getUserReponse() {
        return userReponse;
    }

    public void setUserReponse(String userReponse) {
        this.userReponse = userReponse;
    }
}
