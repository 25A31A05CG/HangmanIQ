
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class HangmanServer {

    static Map<String, Integer> sessions =
        new ConcurrentHashMap<>();

    static String createToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder()
            .withoutPadding().encodeToString(bytes);
    }

    public static void main(String[] args) throws Exception {

        Database.createTables();

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/", exchange -> {

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {

                // CORS / OPTIONS
                exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Origin", "*"
                );
                exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Methods",
                    "GET, POST, OPTIONS"
                );
                exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Headers",
                    "Content-Type, Authorization"
                );

                if (method.equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                // REGISTER
                if (path.equals("/api/register")) {

                    if (!method.equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405,
                            "{\"error\":\"POST required\"}");
                        return;
                    }

                    Map<String, String> data = readData(exchange);

                    String message = UserManager.register(
                        data.get("name"),
                        data.get("password")
                    );

                    if ("SUCCESS".equals(message)) {
                        sendJson(exchange, 200,
                            "{\"message\":\"Account created successfully!\"}");
                    } else if ("NAME_EXISTS".equals(message)) {
                        sendJson(exchange, 409,
                            "{\"error\":\"This name already exists. Please choose another name.\"}");
                    } else {
                        sendJson(exchange, 400,
                            "{\"error\":\"" + escape(message) + "\"}");
                    }

                    return;
                }

                // LOGIN
                if (path.equals("/api/login")) {

                    if (!method.equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405,
                            "{\"error\":\"POST required\"}");
                        return;
                    }

                    Map<String, String> data = readData(exchange);

                    String name = data.get("name");
                    String password = data.get("password");

                    if (name == null || password == null) {
                        sendJson(exchange, 400,
                            "{\"error\":\"Enter name and password\"}");
                        return;
                    }

                    int userId = UserManager.login(name, password);

                    if (userId == -1) {
                        sendJson(exchange, 401,
                            "{\"error\":\"Invalid name or password\"}");
                        return;
                    }

                    String token = createToken();
                    sessions.put(token, userId);

                    sendJson(exchange, 200,
                        "{\"token\":\"" + token +
                        "\",\"name\":\"" +
                        escape(UserManager.getPlayerName(userId)) +
                        "\",\"totalScore\":" +
                        UserManager.getTotalScore(userId) + "}");

                    return;
                }

                // CHECK LOGIN
                if (path.equals("/api/me")) {

                    int userId = getUserId(exchange);

                    if (userId == -1) {
                        sendJson(exchange, 401,
                            "{\"error\":\"Please login\"}");
                    } else {
                        sendJson(exchange, 200,
                            "{\"name\":\"" +
                            escape(UserManager.getPlayerName(userId)) +
                            "\",\"totalScore\":" +
                            UserManager.getTotalScore(userId) + "}");
                    }

                    return;
                }

                // SAVE SCORE
                if (path.equals("/api/save-score")) {

                    if (!method.equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405,
                            "{\"error\":\"POST required\"}");
                        return;
                    }

                    int userId = getUserId(exchange);

                    if (userId == -1) {
                        sendJson(exchange, 401,
                            "{\"error\":\"Please login again\"}");
                        return;
                    }

                    Map<String, String> data = readData(exchange);

                    int score;

                    try {
                        score = Integer.parseInt(data.get("score"));
                    } catch (Exception e) {
                        sendJson(exchange, 400,
                            "{\"error\":\"Invalid score\"}");
                        return;
                    }

                    boolean saved = GameManager.saveGame(
                        userId,
                        data.get("gameId"),
                        data.get("topic"),
                        data.get("word"),
                        score,
                        data.get("result")
                    );

                    if (saved) {
                        sendJson(exchange, 200,
                            "{\"message\":\"Score saved!\",\"totalScore\":" +
                            UserManager.getTotalScore(userId) + "}");
                    } else {
                        sendJson(exchange, 409,
                            "{\"error\":\"This game was already saved or could not be saved\"}");
                    }

                    return;
                }

                // RESET CURRENT SCORE
                if (path.equals("/api/reset-score")) {

                    if (!method.equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405,
                            "{\"error\":\"POST required\"}");
                        return;
                    }

                    int userId = getUserId(exchange);

                    if (userId == -1) {
                        sendJson(exchange, 401,
                            "{\"error\":\"Please login again\"}");
                        return;
                    }

                    boolean reset =
                        GameManager.resetCurrentScore(userId);

                    if (reset) {
                        sendJson(exchange, 200,
                            "{\"message\":\"Current score reset!\",\"totalScore\":0}");
                    } else {
                        sendJson(exchange, 500,
                            "{\"error\":\"Could not reset score\"}");
                    }

                    return;
                }

                // LEADERBOARD
                if (path.equals("/api/leaderboard")) {

                    if (!method.equalsIgnoreCase("GET")) {
                        sendJson(exchange, 405,
                            "{\"error\":\"GET required\"}");
                        return;
                    }

                    sendJson(exchange, 200,
                        GameManager.getLeaderboard());

                    return;
                }

                // LOGOUT
                if (path.equals("/api/logout")) {

                    if (!method.equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405,
                            "{\"error\":\"POST required\"}");
                        return;
                    }

                    sessions.remove(getToken(exchange));

                    sendJson(exchange, 200,
                        "{\"message\":\"Logged out\"}");

                    return;
                }

                // FRONTEND
                if (path.equals("/")) {
                    path = "/login.html";
                }

                Path folder = Paths.get("web")
                    .toAbsolutePath().normalize();

                Path file = folder.resolve(path.substring(1))
                    .normalize();

                if (!file.startsWith(folder) ||
                    !Files.exists(file) ||
                    Files.isDirectory(file)) {

                    send(exchange, 404, "Page not found");
                    return;
                }

                String type = "text/plain";

                if (path.endsWith(".html")) {
                    type = "text/html";
                } else if (path.endsWith(".css")) {
                    type = "text/css";
                } else if (path.endsWith(".js")) {
                    type = "application/javascript";
                }

                exchange.getResponseHeaders().set(
                    "Content-Type", type + "; charset=UTF-8"
                );

                byte[] bytes = Files.readAllBytes(file);

                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }

            } catch (Exception e) {

                e.printStackTrace();

                try {
                    sendJson(exchange, 500,
                        "{\"error\":\"Internal server error\"}");
                } catch (Exception ignored) {
                    exchange.close();
                }
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==============================");
        System.out.println("HANGMAN SERVER STARTED!");
        System.out.println("Open: http://localhost:8080");
        System.out.println("Reset API: POST /api/reset-score");
        System.out.println("==============================");
    }

    static String getToken(HttpExchange exchange) {

        String auth = exchange.getRequestHeaders()
            .getFirst("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        return "";
    }

    static int getUserId(HttpExchange exchange) {

        Integer id = sessions.get(getToken(exchange));

        return id == null ? -1 : id;
    }

    static Map<String, String> readData(
            HttpExchange exchange) throws Exception {

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String item : body.split("&")) {

            if (item.isEmpty()) continue;

            String[] pair = item.split("=", 2);

            String key = URLDecoder.decode(pair[0], "UTF-8");

            String value = pair.length > 1
                ? URLDecoder.decode(pair[1], "UTF-8")
                : "";

            data.put(key, value);
        }

        return data;
    }

    static String escape(String value) {

        if (value == null) return "";

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    static void sendJson(
            HttpExchange exchange,
            int status,
            String message) throws IOException {

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        send(exchange, status, message);
    }

    static void send(
            HttpExchange exchange,
            int status,
            String message) throws IOException {

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
