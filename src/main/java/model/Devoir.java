package model;

import java.time.LocalDateTime;

public class Devoir {
    private int id;
    private String titre_d;
    private String descr_d;
    private LocalDateTime date_d;
    private String support_d;
    private int id_cours;
    private String comment;

    public Devoir() {
    }

    public Devoir(int id, String titre_d, String descr_d, LocalDateTime date_d, String support_d, int id_cours, String comment) {
        this.id = id;
        setTitre_d(titre_d);
        setDescr_d(descr_d);
        this.date_d = date_d;
        this.support_d = support_d;
        this.id_cours = id_cours;
        setComment(comment);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre_d() {
        return titre_d;
    }

    public void setTitre_d(String titre_d) {
        if (titre_d == null || titre_d.trim().length() < 4) {
            throw new IllegalArgumentException("Le titre du devoir doit contenir au moins 4 caractères");
        }
        this.titre_d = titre_d;
    }

    public String getDescr_d() {
        return descr_d;
    }

    public void setDescr_d(String descr_d) {
        if (descr_d == null || descr_d.trim().length() < 8) {
            throw new IllegalArgumentException("La description du devoir doit contenir au moins 8 caractères");
        }
        this.descr_d = descr_d;
    }

    public LocalDateTime getDate_d() {
        return date_d;
    }

    public void setDate_d(LocalDateTime date_d) {
        this.date_d = date_d;
    }

    public String getSupport_d() {
        return support_d;
    }

    public void setSupport_d(String support_d) {
        this.support_d = support_d;
    }

    public int getId_cours() {
        return id_cours;
    }

    public void setId_cours(int id_cours) {
        this.id_cours = id_cours;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        if (comment != null && !comment.trim().isEmpty() && comment.trim().length() < 6) {
            throw new IllegalArgumentException("Le commentaire doit contenir au moins 6 caractères s'il est fourni");
        }
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "Devoir{" +
                "id=" + id +
                ", titre='" + titre_d + '\'' +
                ", date=" + date_d +
                ", id_cours=" + id_cours +
                '}';
    }
}