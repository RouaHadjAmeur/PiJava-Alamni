package Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    Connection cnx;
 

    public Connection getCnx() {
        return cnx;

    }

    public static DatabaseConnection instance;
    public DatabaseConnection(){

        String Url="jdbc:mysql://localhost/pijava";
        String Username="root";
        String Password="";
        try {
            cnx= DriverManager.getConnection(Url,Username,Password);
            System.out.println("connexion etablie");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static DatabaseConnection getInstance(){

        if (instance==null){
            return instance=new DatabaseConnection();
        }
        return instance;
    }
}
