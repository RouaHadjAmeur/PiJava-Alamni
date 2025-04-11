package model;

import java.sql.Date;

public class ReponseReclamation {
    private int id;
    private Reclamation reclamation;
    private int adminId;
    private String contenue;
    private Date dateReponse;

    public ReponseReclamation() {}


    public ReponseReclamation(Reclamation reclamation, int adminId, String contenue, Date dateReponse) {
        this.reclamation = reclamation;
        this.adminId = adminId;
        this.contenue = contenue;
        this.dateReponse = dateReponse;
    }

    public ReponseReclamation(int id, Reclamation reclamation, int adminId, String contenue, Date dateReponse) {
        this.id = id;
        this.reclamation = reclamation;
        this.adminId = adminId;
        this.contenue = contenue;
        this.dateReponse = dateReponse;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getContenue() { return contenue; }
    public void setContenue(String contenue) { this.contenue = contenue; }

    public Date getDateReponse() { return dateReponse; }
    public void setDateReponse(Date dateReponse) { this.dateReponse = dateReponse; }

    public Reclamation getReclamation() { return reclamation; }
    public void setReclamation(Reclamation reclamation) { this.reclamation = reclamation; }
}
