package model;

import java.sql.Date;

public class DiscussionReclamation {

    private int id;
    private int reclamationId;
    private int reponseId; // 🔥 Ajouté
    private String auteurEmail;
    private String auteurRole;
    private String contenu;
    private Date dateReponse;

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(int reclamationId) {
        this.reclamationId = reclamationId;
    }

    public int getReponseId() {
        return reponseId;
    }

    public void setReponseId(int reponseId) {
        this.reponseId = reponseId;
    }

    public String getAuteurEmail() {
        return auteurEmail;
    }

    public void setAuteurEmail(String auteurEmail) {
        this.auteurEmail = auteurEmail;
    }

    public String getAuteurRole() {
        return auteurRole;
    }

    public void setAuteurRole(String auteurRole) {
        this.auteurRole = auteurRole;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Date getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(Date dateReponse) {
        this.dateReponse = dateReponse;
    }

    // 🔧 Constructeurs
    public DiscussionReclamation() {}

    public DiscussionReclamation(int reclamationId, int reponseId, String auteurEmail, String auteurRole, String contenu, Date dateReponse) {
        this.reclamationId = reclamationId;
        this.reponseId = reponseId;
        this.auteurEmail = auteurEmail;
        this.auteurRole = auteurRole;
        this.contenu = contenu;
        this.dateReponse = dateReponse;
    }

    public DiscussionReclamation(int id, int reclamationId, int reponseId, String auteurEmail, String auteurRole, String contenu, Date dateReponse) {
        this.id = id;
        this.reclamationId = reclamationId;
        this.reponseId = reponseId;
        this.auteurEmail = auteurEmail;
        this.auteurRole = auteurRole;
        this.contenu = contenu;
        this.dateReponse = dateReponse;
    }
}
