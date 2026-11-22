
import java.sql.*;
import java.util.Scanner;

public class ViewDatabase {

    static final String URL = "jdbc:sqlite:hangman.db";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {
            Class.forName("org.sqlite.JDBC");

            try (Connection con = DriverManager.getConnection(URL)) {

                int choice;

                do {
                    System.out.println("\n====== HANGMAN DATABASE MENU ======");
                    System.out.println("1. Display Player Scores");
                    System.out.println("2. Delete User Record");
                    System.out.println("3. Exit");
                    System.out.print("Enter your choice: ");

                    choice = sc.nextInt();

                    switch (choice) {

                        case 1:
                            displayScores(con);
                            break;

                        case 2:
                            System.out.print("Enter Player ID to delete: ");
                            int userId = sc.nextInt();
                            deleteUser(con, userId);
                            break;

                        case 3:
                            System.out.println("Exiting Database Manager...");
                            break;

                        default:
                            System.out.println("Invalid choice! Try again.");
                    }

                } while (choice != 3);

            }

        } catch (Exception e) {
            System.out.println("Database Error: " + e.getMessage());
        }

        sc.close();
    }

    // DISPLAY PLAYER SCORES
    static void displayScores(Connection con) throws SQLException {

        String sql = "SELECT id, player_name, total_score, best_score " +
                     "FROM players ORDER BY best_score DESC";

        try (
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql)
        ) {

            System.out.println("\n========== PLAYER SCORES ==========");

            System.out.printf("%-8s %-20s %-15s %-15s%n",
                    "ID", "PLAYER NAME", "TOTAL SCORE", "BEST SCORE");

            System.out.println("--------------------------------------------------------------");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf("%-8d %-20s %-15d %-15d%n",
                        rs.getInt("id"),
                        rs.getString("player_name"),
                        rs.getInt("total_score"),
                        rs.getInt("best_score"));
            }

            if (!found) {
                System.out.println("No player records found.");
            }
        }
    }

    // DELETE USER RECORD
    static void deleteUser(Connection con, int userId) throws SQLException {

        String checkSQL = "SELECT player_name FROM players WHERE id = ?";

        try (PreparedStatement check = con.prepareStatement(checkSQL)) {

            check.setInt(1, userId);

            try (ResultSet rs = check.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("User ID not found!");
                    return;
                }

                String playerName = rs.getString("player_name");

                System.out.println("Player found: " + playerName);

                Scanner confirm = new Scanner(System.in);
                System.out.print("Are you sure you want to delete this user? (yes/no): ");

                String answer = confirm.nextLine();

                if (!answer.equalsIgnoreCase("yes")) {
                    System.out.println("Deletion cancelled.");
                    return;
                }

                con.setAutoCommit(false);

                try {
                    // Delete user's game history first
                    String historySQL =
                            "DELETE FROM game_history WHERE user_id = ?";

                    try (PreparedStatement ps = con.prepareStatement(historySQL)) {
                        ps.setInt(1, userId);
                        ps.executeUpdate();
                    }

                    // Delete player
                    String playerSQL =
                            "DELETE FROM players WHERE id = ?";

                    try (PreparedStatement ps = con.prepareStatement(playerSQL)) {
                        ps.setInt(1, userId);
                        ps.executeUpdate();
                    }

                    con.commit();

                    System.out.println("User and their game history deleted successfully!");

                } catch (SQLException e) {
                    con.rollback();
                    System.out.println("Deletion failed: " + e.getMessage());

                } finally {
                    con.setAutoCommit(true);
                }
            }
        }
    }
}
