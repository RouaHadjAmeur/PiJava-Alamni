package model;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Message {
    private int id;
    private String contenu;
    private Timestamp dateCreation;
    private int conversation_id;
    private int expediteur_id;
    private String expediteur_email;
    private int is_read;
    private Set<Integer> likedByUsers;
    private Set<Integer> dislikedByUsers;
    private boolean isPinned;

    public Message() {
        this.is_read = 0;
        this.likedByUsers = new HashSet<>();
        this.dislikedByUsers = new HashSet<>();
        this.isPinned = false;
    }

    public Message(String contenu, Timestamp dateCreation, int conversation_id, int expediteur_id) {
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.conversation_id = conversation_id;
        this.expediteur_id = expediteur_id;
        this.is_read = 0;
        this.likedByUsers = new HashSet<>();
        this.dislikedByUsers = new HashSet<>();
        this.isPinned = false;
        
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
        this.likedByUsers = new HashSet<>();
        this.dislikedByUsers = new HashSet<>();
        this.isPinned = false;
    }

    public Message(int id, String contenu, Timestamp dateCreation, int conversation_id, int expediteur_id, String expediteur_email, int is_read) {
        this.id = id;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.conversation_id = conversation_id;
        this.expediteur_id = expediteur_id;
        this.expediteur_email = expediteur_email;
        this.is_read = is_read;
        this.likedByUsers = new HashSet<>();
        this.dislikedByUsers = new HashSet<>();
        this.isPinned = false;
    }
    
    public Message(int id, String contenu, Timestamp dateCreation, int conversation_id, int expediteur_id, String expediteur_email, int is_read, boolean isPinned) {
        this.id = id;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.conversation_id = conversation_id;
        this.expediteur_id = expediteur_id;
        this.expediteur_email = expediteur_email;
        this.is_read = is_read;
        this.likedByUsers = new HashSet<>();
        this.dislikedByUsers = new HashSet<>();
        this.isPinned = isPinned;
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

    public Set<Integer> getLikedByUsers() {
        return likedByUsers;
    }

    public void setLikedByUsers(Set<Integer> likedByUsers) {
        this.likedByUsers = likedByUsers;
    }

    public Set<Integer> getDislikedByUsers() {
        return dislikedByUsers;
    }

    public void setDislikedByUsers(Set<Integer> dislikedByUsers) {
        this.dislikedByUsers = dislikedByUsers;
    }

    public int getLikesCount() {
        return likedByUsers.size();
    }

    public int getDislikesCount() {
        return dislikedByUsers.size();
    }

    public boolean isLikedByUser(int userId) {
        return likedByUsers.contains(userId);
    }

    public boolean isDislikedByUser(int userId) {
        return dislikedByUsers.contains(userId);
    }

    public void addLike(int userId) {
        dislikedByUsers.remove(userId);
        likedByUsers.add(userId);
    }

    public void addDislike(int userId) {
        likedByUsers.remove(userId);
        dislikedByUsers.add(userId);
    }

    public void removeLike(int userId) {
        likedByUsers.remove(userId);
    }

    public void removeDislike(int userId) {
        dislikedByUsers.remove(userId);
    }

    public boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(boolean isPinned) {
        this.isPinned = isPinned;
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
                ", likes=" + getLikesCount() +
                ", dislikes=" + getDislikesCount() +
                ", isPinned=" + isPinned +
                '}';
    }
} 