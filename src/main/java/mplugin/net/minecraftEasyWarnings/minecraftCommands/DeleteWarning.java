package mplugin.net.minecraftEasyWarnings.minecraftCommands;

import mplugin.net.minecraftEasyWarnings.resources.WebhookService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class DeleteWarning implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection databaseConnection;
    private final WebhookService webhookService;

    public DeleteWarning(JavaPlugin plugin, Connection databaseConnection, WebhookService webhookService){
        this.plugin = plugin;
        this.databaseConnection = databaseConnection;
        this.webhookService = webhookService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if(args.length != 1){
            commandSender.sendMessage("§4Invalid arguments, do '/deleteWarning {id}'");
            return true;
        }

        try{
            Integer.parseInt(args[0]);
        }catch(NumberFormatException e){
            commandSender.sendMessage("§4Warning ID must be an integer!");
            return true;
        }

        String warn;

        try(PreparedStatement ps = databaseConnection.prepareStatement("SELECT culprit,reason FROM warnings WHERE id=?")){
            ps.setInt(1, Integer.parseInt(args[0]));
            try(ResultSet resultSet = ps.executeQuery()){
                if(resultSet.next()) {
                    warn = "Culprit: " + resultSet.getString("culprit") + "\nReason: " + resultSet.getString("reason");
                }else{
                    commandSender.sendMessage("§4No warning with that ID exists!");
                    return true;
                }
            }
        } catch (SQLException e) {
            webhookService.sendError("SQL Error:\\n" + e);
            plugin.getLogger().log(Level.SEVERE,"SQL Error", e);
            return true;
        }

        try(PreparedStatement ps = databaseConnection.prepareStatement("DELETE FROM warnings WHERE id=?")){
            ps.setInt(1, Integer.parseInt(args[0]));
            ps.executeUpdate();

            commandSender.sendMessage("§2Successfully deleted warning id " + args[0]);

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
            return true;
        }

        return true;
    }
}
