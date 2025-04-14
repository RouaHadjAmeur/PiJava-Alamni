package dao;

import model.Planning;
import model.Seance;
import utils.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PlanningDAO {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Planning> getAllPlannings() {
        List<Planning> list = new ArrayList<>();
        String sql = """
                SELECT p.*, s.id AS seance_id, s.name AS seance_name, s.description AS seance_desc
                FROM planning p
                LEFT JOIN seance s ON p.seance_id = s.id
                """;

        try (Connection conn = Database.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Planning p = new Planning();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setStartTime(LocalDateTime.parse(rs.getString("start_time"), formatter));
                p.setEndTime(LocalDateTime.parse(rs.getString("end_time"), formatter));
                p.setTeacher(rs.getString("teacher"));
                p.setStudentLevel(rs.getString("student_level"));

                Seance s = new Seance();
                s.setId(rs.getInt("seance_id"));
                s.setName(rs.getString("seance_name"));
                s.setDescription(rs.getString("seance_desc"));

                p.setSeance(s);

                list.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addPlanning(Planning p) {
        String sql = """
                INSERT INTO planning (name, start_time, end_time, seance_id, teacher, student_level)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getName());
            stmt.setString(2, p.getStartTime().format(formatter));
            stmt.setString(3, p.getEndTime().format(formatter));
            stmt.setInt(4, p.getSeance().getId());
            stmt.setString(5, p.getTeacher());
            stmt.setString(6, p.getStudentLevel());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePlanning(Planning p) {
        String sql = """
                UPDATE planning
                SET name = ?, start_time = ?, end_time = ?, seance_id = ?, teacher = ?, student_level = ?
                WHERE id = ?
                """;

        try (Connection conn = Database.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getName());
            stmt.setString(2, p.getStartTime().format(formatter));
            stmt.setString(3, p.getEndTime().format(formatter));
            stmt.setInt(4, p.getSeance().getId());
            stmt.setString(5, p.getTeacher());
            stmt.setString(6, p.getStudentLevel());
            stmt.setInt(7, p.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletePlanning(int id) {
        String sql = "DELETE FROM planning WHERE id = ?";
        try (Connection conn = Database.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}