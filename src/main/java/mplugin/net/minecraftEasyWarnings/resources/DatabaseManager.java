package mplugin.net.minecraftEasyWarnings.resources;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {this.plugin = plugin;}

    public void initDatabase() {
        try {
            // Ensure plugin folder exists
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            File dbFile = new File(plugin.getDataFolder(), "warnings.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);

            plugin.getLogger().info("SQLite database initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database!", e);
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                                CREATE TABLE IF NOT EXISTS warnings (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    culprit TEXT,
                                    issuer TEXT,
                                    reason TEXT,
                                    origin TEXT
                                )
                            """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database connection!", e);
        }
    }

    public Connection getConnection() {return connection;}
}