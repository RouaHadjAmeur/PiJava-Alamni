package services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;
import model.Reclamation;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class ArchiveService {
    private static final Logger LOGGER = Logger.getLogger(ArchiveService.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final String API_URL = "https://680e329ec47cb8074d926092.mockapi.io/archives";
    private final ReclamationServices reclamationService = new ReclamationServices();
    private final Gson gson;

    public ArchiveService() {
        // Configurer Gson pour gérer les dates
        gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new TypeAdapter<Date>() {
                @Override
                public void write(JsonWriter out, Date value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.getTime() / 1000); // Convertir en secondes
                    }
                }

                @Override
                public Date read(JsonReader in) throws IOException {
                    if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    long timestamp = in.nextLong();
                    return new Date(timestamp * 1000); // Convertir en millisecondes
                }
            })
            .create();
    }

    public void startArchiving() {
        LOGGER.info("Démarrage du service d'archivage...");
        // Exécuter tous les 3 jours
        scheduler.scheduleAtFixedRate(this::archiveResolvedReclamations, 0, 3, TimeUnit.DAYS);
    }

    private void archiveResolvedReclamations() {
        try {
            LOGGER.info("Recherche des réclamations résolues à archiver...");
            List<Reclamation> resolvedReclamations = reclamationService.getResolvedReclamations();
            
            if (resolvedReclamations.isEmpty()) {
                LOGGER.info("Aucune réclamation résolue à archiver.");
                return;
            }

            LOGGER.info("Nombre de réclamations à archiver : " + resolvedReclamations.size());

            for (Reclamation r : resolvedReclamations) {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonInput = gson.toJson(r);
                LOGGER.info("Données à envoyer (id " + r.getId() + ") : " + jsonInput);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                LOGGER.info("Code de réponse de l'API (archive id " + r.getId() + ") : " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    LOGGER.info("Réclamation archivée avec succès (id " + r.getId() + ")");
                } else {
                    LOGGER.warning("Erreur lors de l'archivage (id " + r.getId() + "). Code: " + responseCode);
                }
            }

            // Marquer toutes les réclamations comme archivées dans la base de données
            reclamationService.markAsArchived(resolvedReclamations);
            LOGGER.info("Archivage terminé pour toutes les réclamations.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'archivage", e);
        }
    }

    public List<Reclamation> getArchivedReclamations() {
        try {
            LOGGER.info("Récupération des archives...");
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            LOGGER.info("Code de réponse de l'API : " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    LOGGER.info("Réponse de l'API : " + response.toString());
                    
                    TypeToken<List<Reclamation>> token = new TypeToken<List<Reclamation>>() {};
                    List<Reclamation> archives = gson.fromJson(response.toString(), token.getType());
                    LOGGER.info("Nombre d'archives récupérées : " + (archives != null ? archives.size() : 0));
                    return archives;
                }
            } else {
                LOGGER.warning("Erreur lors de la récupération des archives. Code: " + responseCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des archives", e);
        }
        return null;
    }

    public void deleteArchivedReclamation(int id) {
        try {
            LOGGER.info("Suppression de l'archive ID : " + id);
            URL url = new URL(API_URL + "/" + id);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");

            int responseCode = conn.getResponseCode();
            LOGGER.info("Code de réponse de l'API : " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                LOGGER.info("Archive supprimée avec succès");
            } else {
                LOGGER.warning("Erreur lors de la suppression. Code: " + responseCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression de l'archive", e);
        }
    }
} 