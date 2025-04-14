package model;

import java.time.LocalDateTime;

public class Cours {
    private int id;
    private String titre;
    private String descr_c;
    private String matiere_c;
    private LocalDateTime date_c;
    private String niveau;
    private String image;
    private String support_c;

    // Constructeur par défaut
    public Cours() {
    }

    // Constructeur avec paramètres
    public Cours(int id, String titre, String descr_c, String matiere_c, LocalDateTime date_c, String niveau, String image, String support_c) {
        this.id = id;
        setTitre(titre);
        setDescr_c(descr_c);
        setMatiere_c(matiere_c);
        this.date_c = date_c;
        setNiveau(niveau);
        this.image = image;
        this.support_c = support_c;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        if (titre == null || titre.trim().length() < 5) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 5 caractères");
        }
        this.titre = titre;
    }

    public String getDescr_c() {
        return descr_c;
    }

    public void setDescr_c(String descr_c) {
        if (descr_c == null || descr_c.trim().length() < 5) {
            throw new IllegalArgumentException("La description doit contenir au moins 5 caractères");
        }
        this.descr_c = descr_c;
    }

    public String getMatiere_c() {
        return matiere_c;
    }

    public void setMatiere_c(String matiere_c) {

        this.matiere_c = matiere_c;
    }

    public LocalDateTime getDate_c() {
        return date_c;
    }

    public void setDate_c(LocalDateTime date_c) {
        this.date_c = date_c;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {

        this.niveau = niveau;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSupport_c() {
        return support_c;
    }

    public void setSupport_c(String support_c) {
        this.support_c = support_c;
    }

    @Override
    public String toString() {
        return "Cours{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", matiere='" + matiere_c + '\'' +
                ", niveau='" + niveau + '\'' +
                '}';
    }
}