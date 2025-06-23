package ua.tox8729.vclans.database;

import org.bukkit.Color;
import ua.tox8729.vclans.managers.ClanManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClanDatabase {
    private final DatabaseManager databaseManager;

    public ClanDatabase(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        createTables();
    }

    public Connection getConnection() throws SQLException {
        return databaseManager.getConnection();
    }

    private void createTables() {
        String createClansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                "name VARCHAR(50) PRIMARY KEY, " +
                "leader_uuid VARCHAR(36) NOT NULL, " +
                "balance DOUBLE NOT NULL DEFAULT 0.0, " +
                "pvp_enabled BOOLEAN NOT NULL DEFAULT FALSE, " +
                "kills INT NOT NULL DEFAULT 0, " +
                "glow_enabled BOOLEAN NOT NULL DEFAULT FALSE, " +
                "glow_color_r INT NOT NULL DEFAULT 0, " +
                "glow_color_g INT NOT NULL DEFAULT 255, " +
                "glow_color_b INT NOT NULL DEFAULT 0)";
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "clan_name VARCHAR(50), " +
                "FOREIGN KEY (clan_name) REFERENCES clans(name))";
        String createInvitationsTable = "CREATE TABLE IF NOT EXISTS invitations (" +
                "player_uuid VARCHAR(36), " +
                "clan_name VARCHAR(50), " +
                "PRIMARY KEY (player_uuid, clan_name), " +
                "FOREIGN KEY (clan_name) REFERENCES clans(name))";
        String createPointsTable = "CREATE TABLE IF NOT EXISTS player_points (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "points INT NOT NULL DEFAULT 0)";

        try (Connection conn = getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(createClansTable);
             PreparedStatement stmt2 = conn.prepareStatement(createPlayersTable);
             PreparedStatement stmt3 = conn.prepareStatement(createInvitationsTable);
             PreparedStatement stmt4 = conn.prepareStatement(createPointsTable)) {
            conn.setAutoCommit(false);
            stmt1.executeUpdate();
            stmt2.executeUpdate();
            stmt3.executeUpdate();
            stmt4.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveClan(ClanManager.Clan clan) {
        String sql = "INSERT OR REPLACE INTO clans (name, leader_uuid, balance, pvp_enabled, kills, glow_enabled, glow_color_r, glow_color_g, glow_color_b) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            stmt.setString(1, clan.getName());
            stmt.setString(2, clan.getLeader().toString());
            stmt.setDouble(3, clan.getBalance());
            stmt.setBoolean(4, clan.isPvpEnabled());
            stmt.setInt(5, clan.getKills());
            stmt.setBoolean(6, clan.isGlowEnabled());
            Color glowColor = clan.getGlowColor();
            stmt.setInt(7, glowColor.getRed());
            stmt.setInt(8, glowColor.getGreen());
            stmt.setInt(9, glowColor.getBlue());
            stmt.executeUpdate();

            String deleteMembers = "DELETE FROM players WHERE clan_name = ?";
            String insertMember = "INSERT OR REPLACE INTO players (uuid, clan_name) VALUES (?, ?)";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteMembers);
                 PreparedStatement insertStmt = conn.prepareStatement(insertMember)) {
                deleteStmt.setString(1, clan.getName());
                deleteStmt.executeUpdate();

                for (UUID member : clan.getMembers()) {
                    insertStmt.setString(1, member.toString());
                    insertStmt.setString(2, clan.getName());
                    insertStmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try (Connection conn = getConnection()) {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        }
    }

    public void deleteClan(String clanName) {
        String deleteClan = "DELETE FROM clans WHERE name = ?";
        String deleteMembers = "DELETE FROM players WHERE clan_name = ?";
        String deleteInvitations = "DELETE FROM invitations WHERE clan_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(deleteClan);
             PreparedStatement stmt2 = conn.prepareStatement(deleteMembers);
             PreparedStatement stmt3 = conn.prepareStatement(deleteInvitations)) {
            conn.setAutoCommit(false);
            stmt1.setString(1, clanName);
            stmt2.setString(1, clanName);
            stmt3.setString(1, clanName);
            stmt2.executeUpdate();
            stmt3.executeUpdate();
            stmt1.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try (Connection conn = getConnection()) {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        }
    }

    public ClanManager.Clan loadClan(String clanName) {
        String sql = "SELECT * FROM clans WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, clanName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    UUID leader = UUID.fromString(rs.getString("leader_uuid"));
                    double balance = rs.getDouble("balance");
                    boolean pvpEnabled = rs.getBoolean("pvp_enabled");
                    int kills = rs.getInt("kills");
                    boolean glowEnabled = rs.getBoolean("glow_enabled");
                    int red = rs.getInt("glow_color_r");
                    int green = rs.getInt("glow_color_g");
                    int blue = rs.getInt("glow_color_b");

                    List<UUID> members = new ArrayList<>();
                    String membersSql = "SELECT uuid FROM players WHERE clan_name = ?";
                    try (PreparedStatement membersStmt = conn.prepareStatement(membersSql)) {
                        membersStmt.setString(1, name);
                        try (ResultSet membersRs = membersStmt.executeQuery()) {
                            while (membersRs.next()) {
                                members.add(UUID.fromString(membersRs.getString("uuid")));
                            }
                        }
                    }

                    ClanManager.Clan clan = new ClanManager.Clan(name, leader, members);
                    clan.setBalance(balance);
                    clan.setPvpEnabled(pvpEnabled);
                    clan.setKills(kills);
                    clan.setGlowEnabled(glowEnabled);
                    clan.setGlowColor(Color.fromRGB(red, green, blue));
                    return clan;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ClanManager.Clan> loadAllClans() {
        List<ClanManager.Clan> clans = new ArrayList<>();
        String sql = "SELECT name FROM clans";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String clanName = rs.getString("name");
                ClanManager.Clan clan = loadClan(clanName);
                if (clan != null) {
                    clans.add(clan);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clans;
    }

    public void saveInvitation(UUID player, String clanName) {
        String checkClanSql = "SELECT name FROM clans WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkClanSql)) {
            checkStmt.setString(1, clanName);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Ошибка: клан " + clanName + " не существует. Невозможно сохранить приглашение для игрока " + player);
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        String sql = "INSERT OR IGNORE INTO invitations (player_uuid, clan_name) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeInvitation(UUID player, String clanName) {
        String sql = "DELETE FROM invitations WHERE player_uuid = ? AND clan_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.setString(2, clanName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getInvitations(UUID player) {
        List<String> invitations = new ArrayList<>();
        String sql = "SELECT clan_name FROM invitations WHERE player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invitations.add(rs.getString("clan_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invitations;
    }

    public void removePlayer(UUID player) {
        String sql = "DELETE FROM players WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePoints(UUID player, int points) {
        String sql = "INSERT OR REPLACE INTO player_points (uuid, points) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            stmt.setInt(2, points);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Integer> loadAllPoints() {
        Map<UUID, Integer> points = new HashMap<>();
        String sql = "SELECT uuid, points FROM player_points";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                points.put(UUID.fromString(rs.getString("uuid")), rs.getInt("points"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

    public int getPoints(UUID player) {
        String sql = "SELECT points FROM player_points WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("points") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}