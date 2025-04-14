package model;

import java.sql.Timestamp;
import java.util.Objects;

public class Message {
    private int id;
    private String contenu;
    private Timestamp dateCreation;
    private int conversation_id;
    private int expediteur_id;
    private String expediteur_email;
    private int is_read;

    public Message() {
        this.is_read = 0;
    }

    public Message(String contenu, Timestamp dateCreation, int conversation_id, int expediteur_id) {
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.conversation_id = conversation_id;
        this.expediteur_id = expediteur_id;
        this.is_read = 0;
        
        // We need to set expediteur_email separately after construction
        // since it's not available at this point
    }

    public Message(String contenu, Timestamp dateCreation, int conversation_id, int expediteur_id, String expediteur_email) {
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.conversation_id = conversation_id;
        this.expediteur_id = expediteur_id;
        this.expediteur_email = expediteur_email;
        this.is_read = 0;
    }

    public Message(int id, String contenu, Timestamp dateCreation, int conversation_id, int expediteur_id, String expediteur_email, int is_read) {
        this.id = id;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.conversation_id = conversation_id;
        this.expediteur_id = expediteur_id;
        this.expediteur_email = expediteur_email;
        this.is_read = is_read;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Timestamp getCreatedAt() {
        return dateCreation;
    }

    public void setCreatedAt(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(int conversation_id) {
        this.conversation_id = conversation_id;
    }

    public int getExpediteur_id() {
        return expediteur_id;
    }

    public void setExpediteur_id(int expediteur_id) {
        this.expediteur_id = expediteur_id;
    }

    public String getExpediteur_email() {
        return expediteur_email;
    }

    public void setExpediteur_email(String expediteur_email) {
        this.expediteur_email = expediteur_email;
    }

    public int getIs_read() {
        return is_read;
    }

    public void setIs_read(int is_read) {
        this.is_read = is_read;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id == message.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", dateCreation=" + dateCreation +
                ", conversation_id=" + conversation_id +
                ", expediteur_id=" + expediteur_id +
                ", expediteur_email='" + expediteur_email + '\'' +
                ", is_read=" + is_read +
                '}';
    }
} 