package mplugin.net.minecraftEasyWarnings;

import mplugin.net.minecraftEasyWarnings.minecraftCommands.Warn;
import mplugin.net.minecraftEasyWarnings.minecraftCommands.Warnings;
import mplugin.net.minecraftEasyWarnings.resources.DatabaseManager;
import mplugin.net.minecraftEasyWarnings.resources.WebhookService;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.Objects;

public final class MinecraftEasyWarnings extends JavaPlugin {

    DatabaseManager databaseManager;
    Connection databaseConnection;

    @Override
    public void onEnable() {
        //Initialise the database
        databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();

        //Set up Webhook Service
        WebhookService webhookService = new WebhookService(this);

        //Load Minecraft commands
        Objects.requireNonNull(getCommand("warn")).setExecutor(new Warn(this, databaseConnection, webhookService));
        Objects.requireNonNull(getCommand("warnings")).setExecutor(new Warnings(this, databaseConnection, webhookService));
    }

    @Override
    public void onLoad(){
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        databaseManager.closeDatabase();
    }
}
