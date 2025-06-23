package ua.tox8729.vclans.database;

import ua.tox8729.vclans.VClans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final VClans plugin;
    private final String dbPath;
    private final ClanDatabase clanDatabase;

    public DatabaseManager(VClans plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + "/clans.db";
        this.clanDatabase = new ClanDatabase(this);
        initialize();
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return connection;
    }

    public void close() {
    }

    public ClanDatabase getClanDatabase() {
        return clanDatabase;
    }
}