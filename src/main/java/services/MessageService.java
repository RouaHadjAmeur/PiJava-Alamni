package services;

import Main.DatabaseConnection;
import model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
            System.out.println("Table message vérifiée/créée avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la création de la table message: " + e.getMessage());
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
                messages.add(message);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des messages : " + e.getMessage());
        }
        return messages;
    }

    public Message getLastMessageByConversationId(int conversationId) {
        String req = "SELECT * FROM message WHERE conversation_id = ? ORDER BY date_creation DESC LIMIT 1";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, conversationId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new Message(
                    rs.getInt("id"),
                    rs.getString("contenu"),
                    rs.getTimestamp("date_creation"),
                    rs.getInt("conversation_id"),
                    rs.getInt("expediteur_id"),
                    rs.getString("expediteur_email"),
                    rs.getInt("is_read")
                );
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
} 