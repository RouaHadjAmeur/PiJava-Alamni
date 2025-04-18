package util;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import service.UtilisateurService;

import javax.mail.Session;

public class EmailService {

    private static final String FROM_EMAIL = "malekbensaid50@gmail.com";
    private static final String PASSWORD = "jgplkxxnrdzoshqj";

    // URL de base de votre application
    private static final String BASE_URL = "http://localhost:8080";

    public static String genererTokenReinitialisation(String email) {
        // Générer un token unique
        String token = UUID.randomUUID().toString();

        // Stocker le token en association avec l'email
        UtilisateurService.storeResetToken(email, token);

        return token;
    }

    public static boolean envoyerEmailResetLink(String destinataire) {
        // Générer un token unique pour cette demande
        String token = genererTokenReinitialisation(destinataire);

        // Construire l'URL de réinitialisation
        String resetUrl = BASE_URL + "/reset-password?token=" + token;

        // Configuration des propriétés pour le serveur SMTP de Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Établir une session avec authentification
        Session session = javax.mail.Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("Réinitialisation de votre mot de passe - Alamni");

            // Corps du message en HTML avec un bouton de réinitialisation
            String htmlContent =
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                            "<h2 style='color: #3949ab;'>Réinitialisation de votre mot de passe</h2>" +
                            "<p>Vous avez demandé la réinitialisation de votre mot de passe pour votre compte Alamni.</p>" +
                            "<p>Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='" + resetUrl + "' style='background-color: #3949ab; color: white; padding: 12px 24px; " +
                            "text-decoration: none; border-radius: 4px; font-weight: bold;'>Réinitialiser mon mot de passe</a>" +
                            "</div>" +
                            "<p>Si le bouton ne fonctionne pas, vous pouvez également copier et coller le lien suivant dans votre navigateur :</p>" +
                            "<p style='word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 4px;'>" + resetUrl + "</p>" +
                            "<p>Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.</p>" +
                            "<p>Ce lien expirera dans 24 heures pour des raisons de sécurité.</p>" +
                            "<p style='margin-top: 30px; font-size: 12px; color: #757575;'>Ce message a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
                            "</div>";

            // Définir le contenu HTML
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Envoyer le message
            Transport.send(message);

            System.out.println("Email avec lien de réinitialisation envoyé à " + destinataire);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            return false;
        }
    }
}