package model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Conversation {
    private int id;
    private String sujet;
    private Date date_creation;
    private String expediteur_email;
    private String destinataire_email;
    private String statut;
    private int expediteur_id;
    private int destinataire_id;
    private List<Message> messages;

    public Conversation() {
        this.messages = new ArrayList<>();
    }

    public Conversation(String sujet, Date date_creation, String expediteur_email, String destinataire_email, String statut, int expediteur_id, int destinataire_id) {
        this.sujet = sujet;
        this.date_creation = date_creation;
        this.expediteur_email = expediteur_email;
        this.destinataire_email = destinataire_email;
        this.statut = statut;
        this.expediteur_id = expediteur_id;
        this.destinataire_id = destinataire_id;
        this.messages = new ArrayList<>();
    }

    public Conversation(int id, String sujet, Date date_creation, String expediteur_email, String destinataire_email, String statut, int expediteur_id, int destinataire_id) {
        this.id = id;
        this.sujet = sujet;
        this.date_creation = date_creation;
        this.expediteur_email = expediteur_email;
        this.destinataire_email = destinataire_email;
        this.statut = statut;
        this.expediteur_id = expediteur_id;
        this.destinataire_id = destinataire_id;
        this.messages = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public Date getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(Date date_creation) {
        this.date_creation = date_creation;
    }

    public String getExpediteur_email() {
        return expediteur_email;
    }

    public void setExpediteur_email(String expediteur_email) {
        this.expediteur_email = expediteur_email;
    }

    public String getDestinataire_email() {
        return destinataire_email;
    }

    public void setDestinataire_email(String destinataire_email) {
        this.destinataire_email = destinataire_email;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getExpediteur_id() {
        return expediteur_id;
    }

    public void setExpediteur_id(int expediteur_id) {
        this.expediteur_id = expediteur_id;
    }

    public int getDestinataire_id() {
        return destinataire_id;
    }

    public void setDestinataire_id(int destinataire_id) {
        this.destinataire_id = destinataire_id;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public String getLastMessageContent() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1).getContenu();
        }
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sujet, date_creation, expediteur_email, destinataire_email, statut, expediteur_id, destinataire_id);
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", sujet='" + sujet + '\'' +
                ", date_creation=" + date_creation +
                ", expediteur_email='" + expediteur_email + '\'' +
                ", destinataire_email='" + destinataire_email + '\'' +
                ", statut='" + statut + '\'' +
                ", expediteur_id=" + expediteur_id +
                ", destinataire_id=" + destinataire_id +
                ", messages=" + messages +
                '}';
    }
} 