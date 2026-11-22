
import java.sql.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserManager {

    private static String hashPassword(String password, byte[] salt)
            throws Exception {

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(), salt, 120000, 256
        );

        SecretKeyFactory factory =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();

        spec.clearPassword();

        return Base64.getEncoder().encodeToString(hash);
    }

    public static String register(String name, String password) {

        if (name == null || name.trim().isEmpty() ||
            password == null || password.length() < 4) {

            return "Enter a player name and password (minimum 4 characters).";
        }

        name = name.trim();

        if (name.length() > 30) {
            return "Player name must be 30 characters or less.";
        }

        try (Connection con = Database.getConnection()) {

            PreparedStatement check = con.prepareStatement(
                "SELECT id FROM players WHERE player_name = ?"
            );

            check.setString(1, name);

            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                return "NAME_EXISTS";
            }

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            String saltText =
                Base64.getEncoder().encodeToString(salt);

            String passwordHash = hashPassword(password, salt);

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO players(player_name,password_hash,salt) VALUES(?,?,?)"
            );

            ps.setString(1, name);
            ps.setString(2, passwordHash);
            ps.setString(3, saltText);

            ps.executeUpdate();

            return "SUCCESS";

        } catch (Exception e) {
            e.printStackTrace();
            return "Registration failed.";
        }
    }

    public static int login(String name, String password) {

        try (Connection con = Database.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "SELECT id,password_hash,salt FROM players WHERE player_name = ?"
            );

            ps.setString(1, name.trim());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return -1;

            int id = rs.getInt("id");

            byte[] salt = Base64.getDecoder().decode(
                rs.getString("salt")
            );

            String storedHash = rs.getString("password_hash");
            String enteredHash = hashPassword(password, salt);

            if (MessageDigest.isEqual(
                storedHash.getBytes("UTF-8"),
                enteredHash.getBytes("UTF-8"))) {

                return id;
            }

            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String getPlayerName(int id) {

        try (Connection con = Database.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "SELECT player_name FROM players WHERE id = ?"
            );

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("player_name");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static int getTotalScore(int id) {

        try (Connection con = Database.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "SELECT total_score FROM players WHERE id = ?"
            );

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_score");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
