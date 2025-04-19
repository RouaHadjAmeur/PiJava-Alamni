import model.Eleve;
import model.Utilisateur;
import service.UtilisateurService;

public class Main {
    public static void main(String[] args) {

        // 👤 Création d’un élève
        Eleve eleve = new Eleve("Ali", "Ben Salah", "ali@example.com", "123456");
        eleve.setNiveau("Lycée");
        eleve.setNomNiveau("Terminale");
        eleve.setPending(false); // validé à la main ici pour test

        if (!UtilisateurService.emailExiste(eleve.getEmail())) {
            UtilisateurService.inscrire(eleve);
        } else {
            System.out.println("⚠️ Utilisateur déjà inscrit.");
        }

        // 🚪 Essai de Connexion
        Utilisateur user = UtilisateurService.login("ali@example.com", "123456");
        if (user != null) {
            System.out.println("✅ Connecté : " + user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ")");
        } else {
            System.out.println("❌ Échec de connexion ou compte en attente.");
        }
    }
}