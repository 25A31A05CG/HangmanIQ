
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class Database {

    private static final String URL = "jdbc:sqlite:hangman.db";

    public static Connection getConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(URL);
    }

    public static void createTables() {

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    total_score INTEGER DEFAULT 0,
                    best_score INTEGER DEFAULT 0
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS game_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    game_id TEXT NOT NULL,
                    topic TEXT,
                    word TEXT,
                    score INTEGER DEFAULT 0,
                    result TEXT,
                    FOREIGN KEY(user_id) REFERENCES players(id),
                    UNIQUE(user_id, game_id)
                )
            """);

            ensureColumn(con, "players", "total_score",
                    "INTEGER DEFAULT 0");

            ensureColumn(con, "players", "best_score",
                    "INTEGER DEFAULT 0");

            ensureColumn(con, "game_history", "game_id", "TEXT");
            ensureColumn(con, "game_history", "topic", "TEXT");
            ensureColumn(con, "game_history", "word", "TEXT");
            ensureColumn(con, "game_history", "score",
                    "INTEGER DEFAULT 0");
            ensureColumn(con, "game_history", "result", "TEXT");

            // Preserve previous scores when upgrading the database.
            st.execute("""
                UPDATE players
                SET best_score = MAX(
                    COALESCE(best_score, 0),
                    COALESCE(total_score, 0)
                )
            """);

            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS
                idx_game_history_user_game
                ON game_history(user_id, game_id)
            """);

            System.out.println("Database tables created/updated!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureColumn(
            Connection con,
            String table,
            String column,
            String definition) throws Exception {

        boolean exists = false;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "PRAGMA table_info(" + table + ")")) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            try (Statement st = con.createStatement()) {
                st.execute("ALTER TABLE " + table +
                        " ADD COLUMN " + column + " " + definition);
            }
        }
    }
}
