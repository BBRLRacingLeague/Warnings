package mplugin.net.minecraftEasyWarnings;

import mplugin.net.minecraftEasyWarnings.resources.WebhookService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DiscordBot extends ListenerAdapter {
    Connection databaseConnection;
    JavaPlugin plugin;
    WebhookService webhookService;

    public DiscordBot(JavaPlugin plugin, Connection databaseConnection, WebhookService webhookService){
        this.databaseConnection = databaseConnection;
        this.plugin = plugin;
        this.webhookService = webhookService;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "warn": warn(event);
            case "warnings": warnings(event);
            case "deleteWarning": deleteWarning(event);
        }

    }

    public void warn(SlashCommandInteractionEvent event){
        OfflinePlayer culprit = Bukkit.getOfflinePlayer(String.valueOf(event.getOption("Minecraft Name")));
        String reason = String.valueOf(event.getOption("Reason"));

        if(culprit.getName() == null){
            event.reply("Could not resolve culprit's name!").setEphemeral(true).queue();
            return;
        }

        //Shouldn't be able to warn players who have never joined before
        if(!culprit.hasPlayedBefore() && !culprit.isOnline()){
            event.reply("The player \" + culprit.getName() + \" has never joined the server!").setEphemeral(true).queue();
            return;
        }

        //Create the logs and send a message to the player to confirm the warning has been issued
        log(culprit.getName(), event.getUser().getId(), reason);
        event.reply("Warned " + culprit + " for " + reason).setEphemeral(true).queue();
    }
    public void log(String culprit, String sender, String reason){
        sendLogInDiscord(culprit, sender, reason);
        createDatabaseRecord(culprit, sender, reason);
    }
    public void createDatabaseRecord(String culprit, String sender, String reason){
        String sql = "INSERT INTO warnings(culprit, issuer, reason, origin) VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = databaseConnection.prepareStatement(sql)){
            ps.setString(1, culprit);
            ps.setString(2, sender);
            ps.setString(3, reason);
            ps.setString(4, "minecraft");
            ps.executeUpdate();
        }catch(SQLException e){
            webhookService.sendError("An SQL Error has occurred when creating a warning:\n" + e);
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create warning", e);
        }
    }
    public void sendLogInDiscord(String culprit, String sender, String reason){
        String json = """
                {
                    "username": "Warnings",
                    "embeds": [
                        {
                            "title": "New Warning",
                            "description": "Culprit: %s \\r\\nIssuer: %s\\r\\nReason: \\"%s\\""
                        }
                    ]
                }
                """.formatted(culprit, sender, reason);
        webhookService.sendRequest(json, plugin.getConfig().getString("WARN_LOG_WEBHOOK_URL"));
    }

    public void warnings(SlashCommandInteractionEvent event){
        //Database logic
        try(PreparedStatement ps = databaseConnection.prepareStatement("SELECT id,reason FROM warnings WHERE culprit=?")){
            ps.setString(1, String.valueOf(event.getOption("Minecraft Name")));
            try(ResultSet resultSet = ps.executeQuery()) {
                //Put ResultSet into a List, as it cannot by itself cannot determine size -- required for header
                List<String> warnings = new ArrayList<>();
                while (resultSet.next()) {
                    warnings.add(resultSet.getInt("id") + ": " + resultSet.getString("reason"));
                }
                //Send warnings to the player
                StringBuilder message = new StringBuilder("~~" + event.getOption("Minecraft Name") + " " + warnings.size() + "~~");
                for (String reason : warnings) {
                    message.append("\n").append(reason);
                }
                event.reply(String.valueOf(message)).setEphemeral(true).queue();
            }
        } catch (SQLException e) {
            webhookService.sendError("SQL Error:\\n" + e);
            plugin.getLogger().log(Level.SEVERE,"SQL Error", e);
        }
    }

    public void deleteWarning(SlashCommandInteractionEvent event){
        try{
            Integer.parseInt(String.valueOf(event.getOption("Warning ID")));
        }catch(NumberFormatException e){
            event.reply("Warning ID must be an integer!").setEphemeral(true).queue();
            return;
        }

        String warn;

        try(PreparedStatement ps = databaseConnection.prepareStatement("SELECT culprit,reason FROM warnings WHERE id=?")){
            ps.setInt(1, Integer.parseInt(String.valueOf(event.getOption("Warning ID"))));
            try(ResultSet resultSet = ps.executeQuery()){
                if(resultSet.next()) {
                    warn = "Culprit: " + resultSet.getString("culprit") + "\nReason: " + resultSet.getString("reason");
                }else{
                    event.reply("No warnings with that ID exist!").setEphemeral(true).queue();
                    return;
                }
            }
        } catch (SQLException e) {
            webhookService.sendError("SQL Error:\\n" + e);
            plugin.getLogger().log(Level.SEVERE,"SQL Error", e);
            return;
        }

        try(PreparedStatement ps = databaseConnection.prepareStatement("DELETE FROM warnings WHERE id=?")){
            ps.setInt(1, Integer.parseInt(String.valueOf(event.getOption("Warning ID"))));
            ps.executeUpdate();

            event.reply("Warning with ID %s was successfully deleted!".formatted(event.getOption("Warning ID"))).setEphemeral(true).queue();

            webhookService.sendRequest("""
                        {
                            "username": "Warnings",
                            "embeds": [
                                {
                                    "title": "Warning Deleted",
                                    "description": "%s"
                                }
                            ]
                        }
                        """.formatted(warn), plugin.getConfig().getString("WARN_LOG_WEBHOOK_URL"));
        } catch (SQLException e) {
            webhookService.sendError("SQL Error:\\n" + e);
            plugin.getLogger().log(Level.SEVERE,"SQL Error", e);
        }
    }
}
