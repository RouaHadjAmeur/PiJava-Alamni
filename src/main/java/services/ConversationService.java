package services;

import Main.DatabaseConnection;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import util.Session;

import java.sql.*;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class ConversationService implements Iservices<Conversation> {

    Connection cnx;
    private final MessageService messageService;

    public ConversationService() {
        cnx = DatabaseConnection.getInstance().getCnx();
        messageService = new MessageService();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement stmt = cnx.createStatement()) {
            // Check if the table exists but with wrong structure
            try {
                ResultSet rs = stmt.executeQuery("SELECT * FROM conversation LIMIT 1");
                ResultSetMetaData metaData = rs.getMetaData();
                boolean hasSujet = false;
                
                // Check if 'sujet' column exists
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    if (metaData.getColumnName(i).equalsIgnoreCase("sujet")) {
                        hasSujet = true;
                        break;
                    }
                }
                
                // If 'sujet' doesn't exist, drop and recreate the table
                if (!hasSujet) {
                    System.out.println("Table 'conversation' exists but has incorrect structure. Dropping and recreating...");
                    stmt.execute("DROP TABLE conversation");
                    createConversationTable(stmt);
                }
            } catch (SQLException e) {
                // Table doesn't exist, create it
                createConversationTable(stmt);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la vérification/création de la table conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createConversationTable(Statement stmt) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS `conversation` (" +
            "`id` int(11) NOT NULL AUTO_INCREMENT," +
            "`sujet` varchar(255) NOT NULL," +
            "`date_creation` date NOT NULL," +
            "`expediteur_email` varchar(255) NOT NULL," +
            "`destinataire_email` varchar(255) NOT NULL," +
            "`statut` varchar(50) NOT NULL DEFAULT 'Non lu'," +
            "`expediteur_id` int(11) NOT NULL," +
            "`destinataire_id` int(11) NOT NULL," +
            "PRIMARY KEY (`id`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            
        stmt.execute(createTableSQL);
        System.out.println("Table conversation créée avec succès");
    }

    @Override
    public void add(Conversation conversation) {
        String req = "INSERT INTO conversation (sujet, date_creation, expediteur_email, destinataire_email, statut, expediteur_id, destinataire_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement stm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            stm.setString(1, conversation.getSujet());
            stm.setDate(2, conversation.getDate_creation());
            stm.setString(3, conversation.getExpediteur_email());
            stm.setString(4, conversation.getDestinataire_email());
            stm.setString(5, conversation.getStatut());
            stm.setInt(6, conversation.getExpediteur_id());
            stm.setInt(7, conversation.getDestinataire_id());
            stm.executeUpdate();
            
            // Get the generated conversation ID
            ResultSet rs = stm.getGeneratedKeys();
            if (rs.next()) {
                int conversationId = rs.getInt(1);
                conversation.setId(conversationId);
                
                // Add initial message if exists
                if (!conversation.getMessages().isEmpty()) {
                    Message firstMessage = conversation.getMessages().get(0);
                    firstMessage.setConversation_id(conversationId);
                    messageService.add(firstMessage);
                }
            }
            
            System.out.println("Conversation ajoutée : " + conversation);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modify(Conversation conversation) {
        String req = "UPDATE conversation SET sujet=?, date_creation=?, expediteur_email=?, destinataire_email=?, statut=?, expediteur_id=?, destinataire_id=? WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, conversation.getSujet());
            stm.setDate(2, conversation.getDate_creation());
            stm.setString(3, conversation.getExpediteur_email());
            stm.setString(4, conversation.getDestinataire_email());
            stm.setString(5, conversation.getStatut());
            stm.setInt(6, conversation.getExpediteur_id());
            stm.setInt(7, conversation.getDestinataire_id());
            stm.setInt(8, conversation.getId());
            stm.executeUpdate();
            System.out.println("Conversation modifiée : " + conversation);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Conversation> afficher() {
        List<Conversation> conversations = new ArrayList<>();
        String req = "SELECT * FROM conversation";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                Conversation conversation = new Conversation();
                conversation.setId(rs.getInt("id"));
                conversation.setSujet(rs.getString("sujet"));
                conversation.setDate_creation(rs.getDate("date_creation"));
                conversation.setExpediteur_email(rs.getString("expediteur_email"));
                conversation.setDestinataire_email(rs.getString("destinataire_email"));
                conversation.setStatut(rs.getString("statut"));
                conversation.setExpediteur_id(rs.getInt("expediteur_id"));
                conversation.setDestinataire_id(rs.getInt("destinataire_id"));
                
                // Load messages for this conversation
                List<Message> messages = messageService.getMessagesByConversationId(conversation.getId());
                conversation.setMessages(messages);
                
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conversations;
    }

    @Override
    public void delete(int id) {
        try {
            // First delete all messages in this conversation
            messageService.deleteByConversationId(id);
            
            // Then delete the conversation
            String req = "DELETE FROM conversation WHERE id=?";
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            stm.executeUpdate();
            System.out.println("Conversation supprimée avec ID : " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Conversation getOne(int id) {
        String req = "SELECT * FROM conversation WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                Conversation conversation = new Conversation(
                    rs.getInt("id"),
                    rs.getString("sujet"),
                    rs.getDate("date_creation"),
                    rs.getString("expediteur_email"),
                    rs.getString("destinataire_email"),
                    rs.getString("statut"),
                    rs.getInt("expediteur_id"),
                    rs.getInt("destinataire_id")
                );
                
                // Load messages for this conversation
                List<Message> messages = messageService.getMessagesByConversationId(conversation.getId());
                conversation.setMessages(messages);
                
                return conversation;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Conversation> getMesConversationsEnvoyees() {
        List<Conversation> conversations = new ArrayList<>();
        try {
            Utilisateur user = Session.getUtilisateurConnecte();
            String req = "SELECT * FROM conversation WHERE expediteur_email = ?";
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, user.getEmail());
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Conversation conversation = new Conversation();
                conversation.setId(rs.getInt("id"));
                conversation.setSujet(rs.getString("sujet"));
                conversation.setDate_creation(rs.getDate("date_creation"));
                conversation.setExpediteur_email(rs.getString("expediteur_email"));
                conversation.setDestinataire_email(rs.getString("destinataire_email"));
                conversation.setStatut(rs.getString("statut"));
                conversation.setExpediteur_id(rs.getInt("expediteur_id"));
                conversation.setDestinataire_id(rs.getInt("destinataire_id"));
                
                // Load messages for this conversation
                List<Message> messages = messageService.getMessagesByConversationId(conversation.getId());
                conversation.setMessages(messages);
                
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getMesConversationsEnvoyees : " + e.getMessage());
        }
        return conversations;
    }

    public List<Conversation> getMesConversationsRecues() {
        List<Conversation> conversations = new ArrayList<>();
        try {
            Utilisateur user = Session.getUtilisateurConnecte();
            String req = "SELECT * FROM conversation WHERE destinataire_email = ?";
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, user.getEmail());
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Conversation conversation = new Conversation();
                conversation.setId(rs.getInt("id"));
                conversation.setSujet(rs.getString("sujet"));
                conversation.setDate_creation(rs.getDate("date_creation"));
                conversation.setExpediteur_email(rs.getString("expediteur_email"));
                conversation.setDestinataire_email(rs.getString("destinataire_email"));
                conversation.setStatut(rs.getString("statut"));
                conversation.setExpediteur_id(rs.getInt("expediteur_id"));
                conversation.setDestinataire_id(rs.getInt("destinataire_id"));
                
                // Load messages for this conversation
                List<Message> messages = messageService.getMessagesByConversationId(conversation.getId());
                conversation.setMessages(messages);
                
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getMesConversationsRecues : " + e.getMessage());
        }
        return conversations;
    }

    public void updateStatut(Conversation conversation) {
        String req = "UPDATE conversation SET statut=? WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, conversation.getStatut());
            stm.setInt(2, conversation.getId());
            stm.executeUpdate();
            System.out.println("Statut de la conversation mis à jour : " + conversation);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
} 