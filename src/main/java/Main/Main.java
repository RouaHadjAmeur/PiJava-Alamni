package Main;

import model.Reclamation;
import services.ReclamationServices;

import java.sql.Date;


public class Main {

     public static void main(String[] args) {

        DatabaseConnection.getInstance();
        ReclamationServices rec=new ReclamationServices();

//         Reclamation rec1 = new Reclamation(
//                 "test2@gmail.com",
//                 "Objet test",
//                 "Description test",
//                 "En attente",
//                 Date.valueOf("2025-02-22"),
//                 "admin@gmail.com",
//                 "etudiant",
//                 7
//         );
//         rec.add(rec1);

       // rec.afficher();

         Reclamation existing = rec.getOne(7);
         if (existing != null) {
             System.out.println("Avant modification : " + existing);
             existing.setStatus("Résolue");
             rec.modify(existing);
             System.out.println("Après modification : " + rec.getOne(7));
         } else {
             System.out.println("Réclamation non trouvée.");
         }
    rec.delete(37);
    }

}

