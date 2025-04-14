package dao;

import model.Seance;
import utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeanceDAO {

    public List<Seance> getAllSeances() {
        List<Seance> seanceList = new ArrayList<>();
        String sql = "SELECT * FROM seance";
        try (Connection conn = Database.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Seance s = new Seance();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setDescription(rs.getString("description"));
                seanceList.add(s);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return seanceList;
    }

    public void addSeance(Seance s) {
        String sql = "INSERT INTO seance (name, description) VALUES (?, ?)";
        try (Connection conn = Database.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, s.getName());
            stmt.setString(2, s.getDescription());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
