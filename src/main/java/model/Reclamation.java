package model;

import java.sql.Date;

public class Reclamation {

    private int id;
    private String user_email;
    private String objet;
    private String description;
    private String status;
    private Date date_soumission;
    private String admin_mail;
    private String role;
    private int user_id;

    public Reclamation(String mail, String aaa, String aaaa, String enAttente, String s, String mail1, String etudiant, int userId) {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDate_soumission() {
        return date_soumission;
    }

    public void setDate_soumission(Date date_soumission) {
        this.date_soumission = date_soumission;
    }

    public String getAdmin_mail() {
        return admin_mail;
    }

    public void setAdmin_mail(String admin_mail) {
        this.admin_mail = admin_mail;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", user_email='" + user_email + '\'' +
                ", objet='" + objet + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", date_soumission=" + date_soumission +
                ", admin_mail='" + admin_mail + '\'' +
                ", role='" + role + '\'' +
                ", user_id='" + user_id + '\'' +
                '}';
    }

    public Reclamation(int id, String user_email, String objet, String description, String status, Date date_soumission, String admin_mail, String role, int user_id) {
        this.id = id;
        this.user_email = user_email;
        this.objet = objet;
        this.description = description;
        this.status = status;
        this.date_soumission = date_soumission;
        this.admin_mail = admin_mail;
        this.role = role;
        this.user_id = user_id;
    }

    public Reclamation(String user_email, String objet, String description, String status, Date date_soumission, String admin_mail, String role, int user_id) {
        this.user_email = user_email;
        this.objet = objet;
        this.description = description;
        this.status = status;
        this.date_soumission = date_soumission;
        this.admin_mail = admin_mail;
        this.role = role;
        this.user_id = user_id;

    }

    public Reclamation() {
    }
}
