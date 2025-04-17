package Main;

import model.Message;
import model.Conversation;
import services.MessageService;
import services.ConversationService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Class to test the CRUD operations for Message
 */
public class MessageCrudTest {

    public static void main(String[] args) {
        // Initialize services
        MessageService messageService = new MessageService();
        ConversationService conversationService = new ConversationService();
        
        // Get a conversation for testing
        List<Conversation> conversations = conversationService.afficher();
        if (conversations.isEmpty()) {
            System.out.println("Aucune conversation disponible pour le test. Veuillez d'abord créer une conversation.");
            return;
        }
        
        Conversation testConversation = conversations.get(0);
        System.out.println("Test avec la conversation : " + testConversation);
        
        // 1. CREATE Test
        System.out.println("\n----- Test CREATE (Création d'un message) -----");
        Message newMessage = new Message(
            "Ceci est un message de test créé par MessageCrudTest",
            Timestamp.valueOf(LocalDateTime.now()),
            testConversation.getId(),
            testConversation.getExpediteur_id(),
            testConversation.getExpediteur_email()
        );
        
        messageService.add(newMessage);
        System.out.println("Message créé avec succès");
        
        // 2. READ Test
        System.out.println("\n----- Test READ (Lecture des messages) -----");
        List<Message> messages = messageService.getMessagesByConversationId(testConversation.getId());
        System.out.println("Nombre de messages trouvés : " + messages.size());
        
        // Find our test message
        Message testMessage = null;
        for (Message m : messages) {
            System.out.println("Message [ID: " + m.getId() + "]: " + m.getContenu() + 
                    " (Expediteur: " + m.getExpediteur_email() + ")");
            if (m.getContenu().equals(newMessage.getContenu())) {
                testMessage = m;
            }
        }
        
        if (testMessage == null) {
            System.out.println("Le message de test n'a pas été trouvé!");
            return;
        }
        
        // 3. UPDATE Test
        System.out.println("\n----- Test UPDATE (Mise à jour d'un message) -----");
        testMessage.setContenu("Ce message a été modifié par MessageCrudTest");
        testMessage.setIs_read(1);
        messageService.update(testMessage);
        
        // Verify update
        Message updatedMessage = messageService.getById(testMessage.getId());
        System.out.println("Message mis à jour: " + updatedMessage.getContenu());
        System.out.println("Statut de lecture: " + (updatedMessage.getIs_read() == 1 ? "Lu" : "Non lu"));
        
        // 4. DELETE Test
        System.out.println("\n----- Test DELETE (Suppression d'un message) -----");
        System.out.println("Suppression du message avec ID: " + testMessage.getId());
        messageService.delete(testMessage.getId());
        
        // Verify deletion
        messages = messageService.getMessagesByConversationId(testConversation.getId());
        boolean found = false;
        for (Message m : messages) {
            if (m.getId() == testMessage.getId()) {
                found = true;
                break;
            }
        }
        
        if (found) {
            System.out.println("ERREUR: Le message existe toujours après la suppression!");
        } else {
            System.out.println("Message supprimé avec succès!");
        }
        
        System.out.println("\nTests de CRUD pour Message terminés!");
    }
} 