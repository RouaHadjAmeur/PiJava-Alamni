package model;

import java.sql.Date;

public class ReponseReclamation {
    private int id;
    private int reclamationId;
    private int adminId;
    private String contenue;
    private Date dateReponse;

    public ReponseReclamation() {}

    public ReponseReclamation(int reclamationId, int adminId, String contenue, Date dateReponse) {
        this.reclamationId = reclamationId;
        this.adminId = adminId;
        this.contenue = contenue;
        this.dateReponse = dateReponse;
    }

    public ReponseReclamation(int id, int reclamationId, int adminId, String contenue, Date dateReponse) {
        this.id = id;
        this.reclamationId = reclamationId;
        this.adminId = adminId;
        this.contenue = contenue;
        this.dateReponse = dateReponse;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReclamationId() { return reclamationId; }
    public void setReclamationId(int reclamationId) { this.reclamationId = reclamationId; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getContenue() { return contenue; }
    public void setContenue(String contenue) { this.contenue = contenue; }

    public Date getDateReponse() { return dateReponse; }
    public void setDateReponse(Date dateReponse) { this.dateReponse = dateReponse; }
}
