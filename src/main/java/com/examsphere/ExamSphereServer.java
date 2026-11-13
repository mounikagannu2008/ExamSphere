package com.examsphere;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight HTTP server using Java's built-in HttpServer.
 * It serves HTML/CSS/JS and exposes small form-encoded JSON endpoints.
 */
public class ExamSphereServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("EXAMSPHERE_PORT", "8080"));
    private static final Map<String, User> SESSIONS = new ConcurrentHashMap<>();
    private final ExamService service = new ExamService();
    private final Path webRoot = Paths.get("web").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        new ExamSphereServer().start();
    }

    private void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::serveStatic);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("ExamSphere is running at http://localhost:" + PORT);
    }

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Map<String, String> form = HttpUtil.readForm(ex);

            if (path.equals("/api/register") && method.equals("POST")) {
                String name = HttpUtil.required(form, "name");
                String email = HttpUtil.required(form, "email").toLowerCase();
                String password = HttpUtil.required(form, "password");
                if (password.length() < 6) throw new IllegalArgumentException("Password must contain at least 6 characters.");
                service.register(name, email, password);
                HttpUtil.send(ex, 201, "application/json", HttpUtil.jsonMessage("Account created. Please sign in."));
                return;
            }
            if (path.equals("/api/login") && method.equals("POST")) {
                String email = HttpUtil.required(form, "email").toLowerCase();
                String password = HttpUtil.required(form, "password");
                String role = form.getOrDefault("role", "STUDENT").toUpperCase();
                if (!role.equals("STUDENT") && !role.equals("ADMIN")) throw new IllegalArgumentException("Invalid role.");
                User user = service.authenticate(email, password, role);
                if (user == null) { HttpUtil.send(ex, 401, "application/json", HttpUtil.jsonMessage("Email, password or role is incorrect.")); return; }
                String token = UUID.randomUUID().toString();
                SESSIONS.put(token, user);
                ex.getResponseHeaders().add("Set-Cookie", "EXAMSPHERE_SESSION=" + token + "; Path=/; HttpOnly; SameSite=Strict");
                HttpUtil.send(ex, 200, "application/json", "{\"id\":" + user.getId() + ",\"name\":\"" +
                        HttpUtil.escapeJson(user.getName()) + "\",\"role\":\"" + user.getRole() + "\"}");
                return;
            }
            if (path.equals("/api/logout") && method.equals("POST")) {
                String token = cookie(ex, "EXAMSPHERE_SESSION");
                if (token != null) SESSIONS.remove(token);
                ex.getResponseHeaders().add("Set-Cookie", "EXAMSPHERE_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
                HttpUtil.send(ex, 200, "application/json", HttpUtil.jsonMessage("Signed out."));
                return;
            }

            User user = currentUser(ex);
            if (path.equals("/api/me") && method.equals("GET")) {
                if (user == null) { HttpUtil.send(ex, 401, "application/json", HttpUtil.jsonMessage("Not signed in.")); return; }
                HttpUtil.send(ex, 200, "application/json", "{\"id\":" + user.getId() + ",\"name\":\"" +
                        HttpUtil.escapeJson(user.getName()) + "\",\"role\":\"" + user.getRole() + "\"}");
                return;
            }
            if (user == null) { HttpUtil.send(ex, 401, "application/json", HttpUtil.jsonMessage("Please sign in first.")); return; }

            if (path.equals("/api/exams") && method.equals("GET")) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (Exam exam : service.getExams()) {
                    if (!first) json.append(",");
                    first = false;
                    json.append("{\"id\":").append(exam.getId()).append(",\"title\":\"")
                        .append(HttpUtil.escapeJson(exam.getTitle())).append("\",\"description\":\"")
                        .append(HttpUtil.escapeJson(exam.getDescription())).append("\",\"duration\":")
                        .append(exam.getDurationMinutes()).append("}");
                }
                HttpUtil.send(ex, 200, "application/json", json.append("]").toString()); return;
            }
            if (path.equals("/api/questions") && method.equals("GET")) {
                int examId = Integer.parseInt(ex.getRequestURI().getQuery().replace("id=", ""));
                List<Question> questions = service.getQuestions(examId);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    if (i > 0) json.append(",");
                    json.append("{\"id\":").append(q.getId()).append(",\"text\":\"").append(HttpUtil.escapeJson(q.getText()))
                        .append("\",\"options\":[\"").append(HttpUtil.escapeJson(q.getOptionA())).append("\",\"")
                        .append(HttpUtil.escapeJson(q.getOptionB())).append("\",\"").append(HttpUtil.escapeJson(q.getOptionC()))
                        .append("\",\"").append(HttpUtil.escapeJson(q.getOptionD())).append("\"]}");
                }
                HttpUtil.send(ex, 200, "application/json", json.append("]").toString()); return;
            }
            if (path.equals("/api/submit") && method.equals("POST")) {
                if (!user.getRole().equals("STUDENT")) throw new IllegalArgumentException("Only students can submit an exam.");
                int examId = HttpUtil.intValue(form, "examId");
                Map<Integer, String> answers = new HashMap<>();
                for (Map.Entry<String, String> entry : form.entrySet()) {
                    if (entry.getKey().startsWith("answer_")) {
                        answers.put(Integer.parseInt(entry.getKey().substring(7)), entry.getValue());
                    }
                }
                int score = service.submitAttempt(user.getId(), examId, answers);
                HttpUtil.send(ex, 200, "application/json", "{\"score\":" + score + ",\"message\":\"Exam submitted successfully.\"}"); return;
            }
            if (path.equals("/api/results") && method.equals("GET")) {
                if (!user.getRole().equals("STUDENT")) throw new IllegalArgumentException("Student access only.");
                HttpUtil.send(ex, 200, "application/json", service.getResultsJson(user.getId())); return;
            }
            if (path.equals("/api/admin/users") && method.equals("GET")) {
                requireAdmin(user);
                HttpUtil.send(ex, 200, "application/json", service.getAdminUsersJson()); return;
            }
            if (path.equals("/api/admin/results") && method.equals("GET")) {
                requireAdmin(user);
                HttpUtil.send(ex, 200, "application/json", service.getAdminResultsJson()); return;
            }
            if (path.equals("/api/admin/stats") && method.equals("GET")) {
                requireAdmin(user);
                HttpUtil.send(ex, 200, "application/json", service.getAdminStatsJson()); return;
            }
            if (path.equals("/api/admin/exams") && method.equals("POST")) {
                requireAdmin(user);
                int id = service.createExam(HttpUtil.required(form, "title"),
                        form.getOrDefault("description", ""), Integer.parseInt(form.getOrDefault("duration", "30")));
                HttpUtil.send(ex, 201, "application/json", "{\"id\":" + id + ",\"message\":\"Examination created.\"}"); return;
            }
            if (path.equals("/api/admin/questions") && method.equals("POST")) {
                requireAdmin(user);
                service.addQuestion(HttpUtil.intValue(form, "examId"), HttpUtil.required(form, "text"),
                        HttpUtil.required(form, "a"), HttpUtil.required(form, "b"),
                        HttpUtil.required(form, "c"), HttpUtil.required(form, "d"),
                        HttpUtil.required(form, "correct"), Integer.parseInt(form.getOrDefault("marks", "1")));
                HttpUtil.send(ex, 201, "application/json", HttpUtil.jsonMessage("Question added.")); return;
            }
            HttpUtil.send(ex, 404, "application/json", HttpUtil.jsonMessage("API endpoint not found."));
        } catch (IllegalArgumentException ex2) {
            HttpUtil.send(ex, 400, "application/json", HttpUtil.jsonMessage(ex2.getMessage()));
        } catch (SQLException ex2) {
            String message = ex2.getMessage() != null && ex2.getMessage().contains("Duplicate")
                    ? "That email address is already registered." : "Database operation failed: " + ex2.getMessage();
            HttpUtil.send(ex, 500, "application/json", HttpUtil.jsonMessage(message));
        } catch (Exception ex2) {
            HttpUtil.send(ex, 500, "application/json", HttpUtil.jsonMessage("Server error: " + ex2.getMessage()));
        } finally {
            ex.close();
        }
    }

    private void serveStatic(HttpExchange ex) throws IOException {
        try {
            String requested = ex.getRequestURI().getPath();
            if (requested.equals("/")) requested = "/index.html";
            Path file = webRoot.resolve(requested.substring(1)).normalize();
            if (!file.startsWith(webRoot) || !Files.exists(file) || Files.isDirectory(file)) {
                HttpUtil.send(ex, 404, "text/plain", "Page not found"); return;
            }
            String name = file.getFileName().toString();
            String type = name.endsWith(".css") ? "text/css" :
                    name.endsWith(".js") ? "application/javascript" : "text/html";
            HttpUtil.send(ex, 200, type, Files.readString(file));
        } finally { ex.close(); }
    }

    private static void requireAdmin(User user) {
        if (!user.getRole().equals("ADMIN")) throw new IllegalArgumentException("Administrator access required.");
    }

    private static User currentUser(HttpExchange ex) {
        String token = cookie(ex, "EXAMSPHERE_SESSION");
        return token == null ? null : SESSIONS.get(token);
    }

    private static String cookie(HttpExchange ex, String name) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String header : cookies) for (String part : header.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals(name)) return pair[1];
        }
        return null;
    }
}
