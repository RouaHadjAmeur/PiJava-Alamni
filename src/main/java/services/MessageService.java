package services;

import Main.DatabaseConnection;
import model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageService {
    private Connection cnx;

    public MessageService() {
        cnx = DatabaseConnection.getInstance().getCnx();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement stmt = cnx.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS `message` (" +
                "`id` int(11) NOT NULL AUTO_INCREMENT," +
                "`contenu` TEXT NOT NULL," +
                "`date_creation` TIMESTAMP NOT NULL," +
                "`conversation_id` int(11) NOT NULL," +
                "`expediteur_id` int(11) NOT NULL," +
                "`expediteur_email` varchar(255) NOT NULL," +
                "`is_read` int(1) NOT NULL DEFAULT 0," +
                "PRIMARY KEY (`id`)," +
                "FOREIGN KEY (`conversation_id`) REFERENCES `conversation`(`id`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            stmt.execute(createTableSQL);
            
            // Create message_reaction table for likes/dislikes
            String createReactionTableSQL = "CREATE TABLE IF NOT EXISTS `message_reaction` (" +
                "`message_id` int(11) NOT NULL," +
                "`user_id` int(11) NOT NULL," +
                "`reaction_type` ENUM('LIKE', 'DISLIKE') NOT NULL," +
                "PRIMARY KEY (`message_id`, `user_id`)," +
                "FOREIGN KEY (`message_id`) REFERENCES `message`(`id`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            stmt.execute(createReactionTableSQL);
            
            System.out.println("Tables message and message_reaction vérifiées/créées avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la création des tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void add(Message message) {
        String req = "INSERT INTO message (contenu, date_creation, conversation_id, expediteur_id, expediteur_email, is_read) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, message.getContenu());
            stm.setTimestamp(2, message.getCreatedAt());
            stm.setInt(3, message.getConversation_id());
            stm.setInt(4, message.getExpediteur_id());
            stm.setString(5, message.getExpediteur_email());
            stm.setInt(6, message.getIs_read());
            stm.executeUpdate();
            System.out.println("Message ajouté : " + message);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> getMessagesByConversationId(int conversationId) {
        List<Message> messages = new ArrayList<>();
        String req = "SELECT * FROM message WHERE conversation_id = ? ORDER BY date_creation ASC";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, conversationId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                    rs.getInt("id"),
                    rs.getString("contenu"),
                    rs.getTimestamp("date_creation"),
                    rs.getInt("conversation_id"),
                    rs.getInt("expediteur_id"),
                    rs.getString("expediteur_email"),
                    rs.getInt("is_read")
                );
                
                // Load reactions for this message
                loadReactions(message);
                
                messages.add(message);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des messages : " + e.getMessage());
        }
        return messages;
    }
    
    private void loadReactions(Message message) {
        String req = "SELECT user_id, reaction_type FROM message_reaction WHERE message_id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, message.getId());
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String reactionType = rs.getString("reaction_type");
                
                if ("LIKE".equals(reactionType)) {
                    message.getLikedByUsers().add(userId);
                } else if ("DISLIKE".equals(reactionType)) {
                    message.getDislikedByUsers().add(userId);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des réactions : " + e.getMessage());
        }
    }

    public Message getLastMessageByConversationId(int conversationId) {
        String req = "SELECT * FROM message WHERE conversation_id = ? ORDER BY date_creation DESC LIMIT 1";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, conversationId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                Message message = new Message(
                    rs.getInt("id"),
                    rs.getString("contenu"),
                    rs.getTimestamp("date_creation"),
                    rs.getInt("conversation_id"),
                    rs.getInt("expediteur_id"),
                    rs.getString("expediteur_email"),
                    rs.getInt("is_read")
                );
                
                // Load reactions
                loadReactions(message);
                
                return message;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération du dernier message : " + e.getMessage());
        }
        return null;
    }

    public void delete(int id) {
        String req = "DELETE FROM message WHERE id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            stm.executeUpdate();
            System.out.println("Message supprimé avec ID : " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByConversationId(int conversationId) {
        String req = "DELETE FROM message WHERE conversation_id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, conversationId);
            stm.executeUpdate();
            System.out.println("Messages supprimés pour la conversation : " + conversationId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void update(Message message) {
        String req = "UPDATE message SET contenu = ?, is_read = ? WHERE id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, message.getContenu());
            stm.setInt(2, message.getIs_read());
            stm.setInt(3, message.getId());
            stm.executeUpdate();
            System.out.println("Message mis à jour avec ID : " + message.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Message getById(int id) {
        String req = "SELECT * FROM message WHERE id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                Message message = new Message(
                    rs.getInt("id"),
                    rs.getString("contenu"),
                    rs.getTimestamp("date_creation"),
                    rs.getInt("conversation_id"),
                    rs.getInt("expediteur_id"),
                    rs.getString("expediteur_email"),
                    rs.getInt("is_read")
                );
                
                // Load reactions
                loadReactions(message);
                
                return message;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération du message : " + e.getMessage());
        }
        return null;
    }
    
    public void markAsRead(int messageId) {
        String req = "UPDATE message SET is_read = 1 WHERE id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, messageId);
            stm.executeUpdate();
            System.out.println("Message marqué comme lu : " + messageId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void markAllAsReadByConversationAndRecipient(int conversationId, int recipientId) {
        String req = "UPDATE message SET is_read = 1 WHERE conversation_id = ? AND expediteur_id != ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, conversationId);
            stm.setInt(2, recipientId);
            stm.executeUpdate();
            System.out.println("Tous les messages marqués comme lus pour la conversation : " + conversationId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Add a like reaction from a user to a message
     */
    public void addLike(int messageId, int userId) {
        addReaction(messageId, userId, "LIKE");
    }
    
    /**
     * Add a dislike reaction from a user to a message
     */
    public void addDislike(int messageId, int userId) {
        addReaction(messageId, userId, "DISLIKE");
    }
    
    /**
     * Remove all reactions from a user on a message
     */
    public void removeReaction(int messageId, int userId) {
        String req = "DELETE FROM message_reaction WHERE message_id = ? AND user_id = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, messageId);
            stm.setInt(2, userId);
            stm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression de la réaction : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Add or update a reaction
     */
    private void addReaction(int messageId, int userId, String reactionType) {
        // First remove any existing reaction
        removeReaction(messageId, userId);
        
        // Then add the new reaction
        String req = "INSERT INTO message_reaction (message_id, user_id, reaction_type) VALUES (?, ?, ?)";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, messageId);
            stm.setInt(2, userId);
            stm.setString(3, reactionType);
            stm.executeUpdate();
            System.out.println("Réaction " + reactionType + " ajoutée pour message " + messageId + " par utilisateur " + userId);
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de la réaction : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Toggle like status - add like if not liked, remove if already liked
     * @return The updated message with current reaction counts
     */
    public Message toggleLike(int messageId, int userId) {
        Message message = getById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        
        if (message.isLikedByUser(userId)) {
            // Already liked - remove like
            removeReaction(messageId, userId);
            message.removeLike(userId);
        } else {
            // Not liked - add like and remove any dislike
            addLike(messageId, userId);
            message.addLike(userId);
        }
        
        return message;
    }
    
    /**
     * Toggle dislike status - add dislike if not disliked, remove if already disliked
     * @return The updated message with current reaction counts
     */
    public Message toggleDislike(int messageId, int userId) {
        Message message = getById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        
        if (message.isDislikedByUser(userId)) {
            // Already disliked - remove dislike
            removeReaction(messageId, userId);
            message.removeDislike(userId);
        } else {
            // Not disliked - add dislike and remove any like
            addDislike(messageId, userId);
            message.addDislike(userId);
        }
        
        return message;
    }
} 