package util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

    public class DatabaseConnection {

        private static final String URL = "jdbc:mysql://localhost:3306/pijava";
        private static final String USER = "root";
        private static final String PASSWORD = "";
        private static DatabaseConnection instance;
        private Connection connection;

        public static Connection getConnection() {
            Connection connection = null;

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à la base réussie !");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("❌ Erreur de connexion : " + e.getMessage());
            }

            return connection;
        }
        // Méthode pour obtenir l'instance unique
        public static synchronized DatabaseConnection getInstance() {
            if (instance == null) {
                instance = new DatabaseConnection();
            } else if (instance.getConnection() == null) {
                instance = new DatabaseConnection();
            }
            return instance;
        }

        // Méthode pour obtenir la connexion

        // Méthode pour fermer la connexion
        public void closeConnection() {
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("Connexion fermée");
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
                }
            }
        }

    }

