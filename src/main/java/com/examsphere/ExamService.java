package com.examsphere;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer: contains the main examination rules and JDBC operations.
 * Keeping this separate from HTTP handling makes the code easier to explain.
 */
public class ExamService {
    public User authenticate(String email, String password, String role) throws SQLException {
        String sql = "SELECT id, full_name, email, role FROM users WHERE email=? AND password_hash=? AND role=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new User(rs.getInt("id"), rs.getString("full_name"),
                        rs.getString("email"), rs.getString("role"));
            }
        }
        return null;
    }

    public boolean register(String name, String email, String password) throws SQLException {
        String sql = "INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'STUDENT')";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, PasswordUtil.hash(password));
            ps.executeUpdate();
            return true;
        }
    }

    public List<Exam> getExams() throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT id,title,description,duration_minutes FROM exams WHERE active=1 ORDER BY id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) exams.add(new Exam(rs.getInt("id"), rs.getString("title"),
                    rs.getString("description"), rs.getInt("duration_minutes")));
        }
        return exams;
    }

    public List<Question> getQuestions(int examId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT id,question_text,option_a,option_b,option_c,option_d FROM questions WHERE exam_id=? ORDER BY id";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) questions.add(new Question(rs.getInt("id"), rs.getString("question_text"),
                        rs.getString("option_a"), rs.getString("option_b"),
                        rs.getString("option_c"), rs.getString("option_d")));
            }
        }
        return questions;
    }

    public int createExam(String title, String description, int duration) throws SQLException {
        String sql = "INSERT INTO exams(title,description,duration_minutes,active) VALUES(?,?,?,1)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title); ps.setString(2, description); ps.setInt(3, duration);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create examination.");
    }

    public void addQuestion(int examId, String text, String a, String b, String c, String d,
                            String correct, int marks) throws SQLException {
        String sql = "INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId); ps.setString(2, text); ps.setString(3, a);
            ps.setString(4, b); ps.setString(5, c); ps.setString(6, d);
            ps.setString(7, correct.toUpperCase()); ps.setInt(8, marks);
            ps.executeUpdate();
        }
    }

    /** Saves answers and calculates the score on the server, never trusting a browser score. */
    public int submitAttempt(int userId, int examId, java.util.Map<Integer, String> answers) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int score = 0;
                int total = 0;
                List<Integer> questionIds = new ArrayList<>();
                String read = "SELECT id,correct_option,marks FROM questions WHERE exam_id=?";
                try (PreparedStatement ps = con.prepareStatement(read)) {
                    ps.setInt(1, examId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int qid = rs.getInt("id");
                            questionIds.add(qid);
                            int marks = rs.getInt("marks");
                            total += marks;
                            String chosen = answers.getOrDefault(qid, "");
                            if (chosen.equalsIgnoreCase(rs.getString("correct_option"))) score += marks;
                        }
                    }
                }
                if (questionIds.isEmpty()) throw new SQLException("This examination has no questions yet.");
                String insertAttempt = "INSERT INTO attempts(user_id,exam_id,score,total_marks) VALUES(?,?,?,?)";
                int attemptId;
                try (PreparedStatement ps = con.prepareStatement(insertAttempt, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId); ps.setInt(2, examId); ps.setInt(3, score); ps.setInt(4, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not save attempt.");
                        attemptId = keys.getInt(1);
                    }
                }
                String saveAnswer = "INSERT INTO attempt_answers(attempt_id,question_id,selected_option) VALUES(?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(saveAnswer)) {
                    for (int qid : questionIds) {
                        ps.setInt(1, attemptId); ps.setInt(2, qid);
                        ps.setString(3, answers.getOrDefault(qid, ""));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return score;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }


    /** Admin-only report: registered accounts. Password hashes are deliberately excluded. */
    public String getAdminUsersJson() throws SQLException {
        StringBuilder out = new StringBuilder("[");
        String sql = "SELECT id,full_name,email,role,created_at FROM users ORDER BY created_at DESC,id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) out.append(",");
                first = false;
                out.append("{\"id\":").append(rs.getInt("id"))
                   .append(",\"name\":\"").append(HttpUtil.escapeJson(rs.getString("full_name"))).append("\"")
                   .append(",\"email\":\"").append(HttpUtil.escapeJson(rs.getString("email"))).append("\"")
                   .append(",\"role\":\"").append(HttpUtil.escapeJson(rs.getString("role"))).append("\"")
                   .append(",\"createdAt\":\"").append(HttpUtil.escapeJson(String.valueOf(rs.getTimestamp("created_at")))).append("\"}");
            }
        }
        return out.append("]").toString();
    }

    /** Admin-only report: every submitted attempt, joined to its student and exam. */
    public String getAdminResultsJson() throws SQLException {
        StringBuilder out = new StringBuilder("[");
        String sql = "SELECT a.id,u.full_name,u.email,e.title,a.score,a.total_marks,a.submitted_at " +
                     "FROM attempts a JOIN users u ON u.id=a.user_id JOIN exams e ON e.id=a.exam_id " +
                     "ORDER BY a.submitted_at DESC,a.id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) out.append(",");
                first = false;
                out.append("{\"id\":").append(rs.getInt("id"))
                   .append(",\"studentName\":\"").append(HttpUtil.escapeJson(rs.getString("full_name"))).append("\"")
                   .append(",\"email\":\"").append(HttpUtil.escapeJson(rs.getString("email"))).append("\"")
                   .append(",\"examTitle\":\"").append(HttpUtil.escapeJson(rs.getString("title"))).append("\"")
                   .append(",\"score\":").append(rs.getInt("score"))
                   .append(",\"total\":").append(rs.getInt("total_marks"))
                   .append(",\"submittedAt\":\"").append(HttpUtil.escapeJson(String.valueOf(rs.getTimestamp("submitted_at")))).append("\"}");
            }
        }
        return out.append("]").toString();
    }

    public String getAdminStatsJson() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM users WHERE role='STUDENT') AS students, " +
                     "(SELECT COUNT(*) FROM exams WHERE active=1) AS exams, " +
                     "(SELECT COUNT(*) FROM attempts) AS attempts";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return "{\"students\":" + rs.getInt("students") +
                    ",\"exams\":" + rs.getInt("exams") +
                    ",\"attempts\":" + rs.getInt("attempts") + "}";
        }
        return "{\"students\":0,\"exams\":0,\"attempts\":0}";
    }

    public String getResultsJson(int userId) throws SQLException {
        StringBuilder out = new StringBuilder("[");
        String sql = "SELECT a.id,e.title,a.score,a.total_marks,a.submitted_at FROM attempts a JOIN exams e ON e.id=a.exam_id WHERE a.user_id=? ORDER BY a.id DESC";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) out.append(",");
                    first = false;
                    out.append("{\"id\":").append(rs.getInt("id"))
                       .append(",\"title\":\"").append(HttpUtil.escapeJson(rs.getString("title"))).append("\"")
                       .append(",\"score\":").append(rs.getInt("score"))
                       .append(",\"total\":").append(rs.getInt("total_marks"))
                       .append(",\"submittedAt\":\"").append(rs.getTimestamp("submitted_at")).append("\"}");
                }
            }
        }
        return out.append("]").toString();
    }
}
