
import java.sql.*;

public class GameManager {

    // =====================================
    // SAVE GAME SCORE
    // =====================================

    public static boolean saveGame(
            int userId,
            String gameId,
            String topic,
            String word,
            int score,
            String result) {

        if (userId <= 0 ||
            gameId == null ||
            gameId.trim().isEmpty() ||
            score < 0) {

            return false;
        }

        try (Connection con = Database.getConnection()) {

            con.setAutoCommit(false);

            try {

                String insertSql = """
                    INSERT OR IGNORE INTO game_history
                    (user_id, game_id, topic, word, score, result)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

                int inserted;

                try (PreparedStatement ps =
                             con.prepareStatement(insertSql)) {

                    ps.setInt(1, userId);
                    ps.setString(2, gameId);
                    ps.setString(3, topic);
                    ps.setString(4, word);
                    ps.setInt(5, score);
                    ps.setString(6, result);

                    inserted = ps.executeUpdate();
                }

                if (inserted == 0) {
                    con.rollback();
                    return false;
                }

                // Update current score and best score.
                String updateSql = """
                    UPDATE players
                    SET total_score = total_score + ?,
                        best_score = MAX(
                            COALESCE(best_score, 0),
                            total_score + ?
                        )
                    WHERE id = ?
                """;

                int updated;

                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {

                    ps.setInt(1, score);
                    ps.setInt(2, score);
                    ps.setInt(3, userId);

                    updated = ps.executeUpdate();
                }

                if (updated != 1) {
                    con.rollback();
                    return false;
                }

                con.commit();
                return true;

            } catch (Exception e) {

                con.rollback();
                throw e;
            }

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }


    // =====================================
    // RESET CURRENT SCORE
    // =====================================

    public static boolean resetCurrentScore(int userId) {

        String sql = """
            UPDATE players
            SET total_score = 0
            WHERE id = ?
        """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            return ps.executeUpdate() == 1;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }


    // =====================================
    // GET LEADERBOARD
    // =====================================

    public static String getLeaderboard() {

        StringBuilder json = new StringBuilder("[");

        String sql = """
            SELECT player_name, best_score
            FROM players
            ORDER BY best_score DESC, player_name ASC
            LIMIT 10
        """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                first = false;

                String name = rs.getString("player_name");
                int score = rs.getInt("best_score");

                json.append("{")
                    .append("\"name\":\"")
                    .append(escape(name))
                    .append("\",")
                    .append("\"score\":")
                    .append(score)
                    .append("}");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        json.append("]");

        return json.toString();
    }


    // =====================================
    // JSON ESCAPE
    // =====================================

    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
