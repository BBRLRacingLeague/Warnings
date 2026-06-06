package mplugin.net.minecraftEasyWarnings;

import mplugin.net.minecraftEasyWarnings.resources.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;

public final class MinecraftEasyWarnings extends JavaPlugin {

    DatabaseManager databaseManager;
    Connection databaseConnection;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();
    }

    @Override
    public void onDisable() {
        databaseManager.closeDatabase();
    }
}
