package mplugin.net.minecraftEasyWarnings;

import mplugin.net.minecraftEasyWarnings.minecraftCommands.DeleteWarning;
import mplugin.net.minecraftEasyWarnings.minecraftCommands.Warn;
import mplugin.net.minecraftEasyWarnings.minecraftCommands.Warnings;
import mplugin.net.minecraftEasyWarnings.resources.DatabaseManager;
import mplugin.net.minecraftEasyWarnings.resources.WebhookService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.Objects;
import java.util.logging.Level;

public final class MinecraftEasyWarnings extends JavaPlugin {

    DatabaseManager databaseManager;
    Connection databaseConnection;

    @Override
    public void onEnable() {
        //Initialise the database
        databaseManager = new DatabaseManager(this);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();

        if(databaseConnection == null){
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //Set up Webhook Service
        WebhookService webhookService = new WebhookService(this);

        //Load Minecraft commands
        Warn warnCommand = new Warn(this, databaseConnection, webhookService);
        Objects.requireNonNull(getCommand("warn")).setExecutor(warnCommand);
        Objects.requireNonNull(getCommand("warn")).setTabCompleter(warnCommand);
        Objects.requireNonNull(getCommand("warnings")).setExecutor(new Warnings(this, databaseConnection, webhookService));
        Objects.requireNonNull(getCommand("deleteWarning")).setExecutor(new DeleteWarning(this, databaseConnection, webhookService));

        //Load Discord Bot
        try {
            JDA jda = JDABuilder.createDefault(getConfig().getString("DISCORD_BOT_TOKEN")).addEventListeners(new DiscordBot(this, databaseConnection, webhookService)).build();
            jda.awaitReady();
            jda.updateCommands().addCommands(
                    Commands.slash("warn", "Warn a user -- NOTE: Use their minecraft username!")
                            .addOption(OptionType.STRING, "minecraft_name", "Minecraft Name", true)
                            .addOption(OptionType.STRING, "reason", "Reason", true),
                    Commands.slash("warnings", "Check the warnings a user has -- NOTE: Use their minecraft username!")
                            .addOption(OptionType.STRING, "minecraft_name", "Minecraft Name", true),
                    Commands.slash("delete_warning", "Delete a warning by its ID!")
                            .addOption(OptionType.STRING, "warning_id", "Warning ID", true)
            ).queue();
        } catch (InterruptedException e) {
            webhookService.sendError("Discord Bot Error:\n" + e);
            getLogger().log(Level.SEVERE,"Discord Bot Error:\n" + e);
        }

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
