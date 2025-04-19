package services;

import Main.DatabaseConnection;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import util.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class ConversationService implements Iservices<Conversation> {

    private Connection cnx;
    private final MessageService messageService;

    public ConversationService() {
        cnx = DatabaseConnection.getInstance().getCnx();
        messageService = new MessageService();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement stmt = cnx.createStatement()) {
            // Check if the table exists but with the wrong structure
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
    public void ajouter(Conversation conversation) {
        // Check if a conversation between these users already exists
        Conversation existingConversation = findExistingConversation(conversation.getExpediteur_email(), conversation.getDestinataire_email());

        if (existingConversation != null) {
            // Use the existing conversation instead of creating a new one
            if (!conversation.getMessages().isEmpty()) {
                Message firstMessage = conversation.getMessages().get(0);
                firstMessage.setConversation_id(existingConversation.getId());
                messageService.add(firstMessage);

                // Update conversation status
                existingConversation.setStatut("Non lu");
                updateStatut(existingConversation);

                System.out.println("Message added to existing conversation: " + existingConversation.getId());
            }
        } else {
            // Create a new conversation
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

                System.out.println("New conversation created: " + conversation);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updateStatut(Conversation conversation) {
        String req = "UPDATE conversation SET statut=? WHERE id=?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, conversation.getStatut());
            stm.setInt(2, conversation.getId());
            stm.executeUpdate();
            System.out.println("Statut de la conversation modifié: " + conversation.getId() + " -> " + conversation.getStatut());
        } catch (SQLException e) {
            System.out.println("Erreur lors de la mise à jour du statut: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void addMessageToConversation(Message message) {
        // Add the message to the database
        messageService.add(message);
        
        // Update the conversation's status to "Répondu" if needed
        Conversation conversation = getOne(message.getConversation_id());
        if (conversation != null && !conversation.getStatut().equalsIgnoreCase("Répondu")) {
            conversation.setStatut("Répondu");
            updateStatut(conversation);
        }
        
        System.out.println("Message ajouté à la conversation: " + message.getConversation_id());
    }

    public Conversation findExistingConversation(String user1Email, String user2Email) {
        String req = "SELECT * FROM conversation WHERE " +
                "((expediteur_email = ? AND destinataire_email = ?) OR " +
                "(expediteur_email = ? AND destinataire_email = ?)) " +
                "ORDER BY id DESC LIMIT 1";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, user1Email);
            stm.setString(2, user2Email);
            stm.setString(3, user2Email);
            stm.setString(4, user1Email);

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

                System.out.println("Found existing conversation between " + user1Email + " and " + user2Email + ": ID=" + conversation.getId());
                return conversation;
            }
        } catch (SQLException e) {
            System.out.println("Error finding existing conversation: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void modifier(Conversation conversation) {
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
    public int add(Conversation conversation) {
        // Implementation of the add method from the Iservices interface
        ajouter(conversation);
        return conversation.getId();
    }

    @Override
    public void modify(Conversation conversation) {
        // Implementation of the modify method from the Iservices interface
        modifier(conversation);
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
    public List<Conversation> afficher() {
        List<Conversation> conversations = new ArrayList<>();
        String req = "SELECT * FROM conversation ORDER BY date_creation DESC";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Conversation conversation = mapResultSetToConversation(rs);
                // Load messages for the conversation
                conversation.setMessages(messageService.getMessagesByConversationId(conversation.getId()));
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des conversations: " + e.getMessage());
        }
        return conversations;
    }

    private List<Conversation> getAllConversations() {
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
            System.out.println("Erreur getAllConversations : " + e.getMessage());
        }
        return conversations;
    }

    private List<Conversation> getUserConversations(Utilisateur user) {
        List<Conversation> conversations = new ArrayList<>();
        String req = "SELECT * FROM conversation WHERE expediteur_email = ? OR destinataire_email = ?";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setString(1, user.getEmail());
            stm.setString(2, user.getEmail());
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
            System.out.println("Erreur getUserConversations : " + e.getMessage());
        }
        return conversations;
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

    @Override
    public List<Conversation> readAll() {
        return getAllConversations();
    }
    
    public List<Conversation> getMesConversationsRecues() {
        List<Conversation> conversations = new ArrayList<>();
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) {
            return conversations;
        }
        
        String req = "SELECT * FROM conversation WHERE destinataire_id = ? ORDER BY date_creation DESC";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, currentUser.getId());
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Conversation conversation = mapResultSetToConversation(rs);
                // Load messages for the conversation
                conversation.setMessages(messageService.getMessagesByConversationId(conversation.getId()));
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des conversations reçues: " + e.getMessage());
        }
        return conversations;
    }
    
    public List<Conversation> getMesConversationsEnvoyees() {
        List<Conversation> conversations = new ArrayList<>();
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) {
            return conversations;
        }
        
        String req = "SELECT * FROM conversation WHERE expediteur_id = ? ORDER BY date_creation DESC";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, currentUser.getId());
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Conversation conversation = mapResultSetToConversation(rs);
                // Load messages for the conversation
                conversation.setMessages(messageService.getMessagesByConversationId(conversation.getId()));
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des conversations envoyées: " + e.getMessage());
        }
        return conversations;
    }

    /**
     * Unified method to handle adding a message to any conversation.
     * This ensures that messages are always added to existing conversations when possible.
     * @param senderEmail The email of the message sender
     * @param recipientEmail The email of the message recipient
     * @param content The message content
     * @param subject Optional subject (only used if creating a new conversation)
     * @return true if successful, false otherwise
     */
    public boolean sendMessage(String senderEmail, String recipientEmail, String content, String subject) {
        try {
            // Get user IDs
            int senderId = getUserIdByEmail(senderEmail);
            int recipientId = getUserIdByEmail(recipientEmail);
            
            if (senderId == -1 || recipientId == -1) {
                System.out.println("Error: Invalid sender or recipient");
                return false;
            }
            
            // Check for existing conversation
            Conversation existingConversation = findExistingConversation(senderEmail, recipientEmail);
            
            // Create timestamp for message
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            
            if (existingConversation != null) {
                // Add to existing conversation
                Message message = new Message(
                    content,
                    timestamp,
                    existingConversation.getId(),
                    senderId,
                    senderEmail
                );
                
                addMessageToConversation(message);
                return true;
            } else {
                // Create new conversation with message
                Conversation newConversation = new Conversation(
                    subject != null ? subject : content.substring(0, Math.min(content.length(), 50)),
                    new Date(System.currentTimeMillis()),
                    senderEmail,
                    recipientEmail,
                    "Non lu",
                    senderId,
                    recipientId
                );
                
                // Create message
                Message message = new Message(
                    content,
                    timestamp,
                    0, // Will be set after conversation creation
                    senderId,
                    senderEmail
                );
                
                newConversation.addMessage(message);
                ajouter(newConversation);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Utility method to get user ID by email
     */
    private int getUserIdByEmail(String email) {
        try {
            String query = "SELECT id FROM utilisateur WHERE email = ?";
            PreparedStatement stm = cnx.prepareStatement(query);
            stm.setString(1, email);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("Error getting user ID by email: " + e.getMessage());
        }
        return -1;
    }

    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
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
        return conversation;
    }

    public List<Conversation> getMesConversations() {
        List<Conversation> conversations = new ArrayList<>();
        Utilisateur currentUser = Session.getUtilisateurConnecte();
        if (currentUser == null) {
            return conversations;
        }
        
        String req = "SELECT * FROM conversation WHERE expediteur_id = ? OR destinataire_id = ? ORDER BY date_creation DESC";
        try {
            PreparedStatement stm = cnx.prepareStatement(req);
            stm.setInt(1, currentUser.getId());
            stm.setInt(2, currentUser.getId());
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Conversation conversation = mapResultSetToConversation(rs);
                // Load messages for the conversation
                conversation.setMessages(messageService.getMessagesByConversationId(conversation.getId()));
                conversations.add(conversation);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des conversations: " + e.getMessage());
        }
        return conversations;
    }
}
