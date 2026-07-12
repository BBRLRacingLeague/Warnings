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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Warnings implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection databaseConnection;
    private final WebhookService webhookService;

    public Warnings(JavaPlugin plugin, Connection databaseConnection, WebhookService webhookService){
        this.plugin = plugin;
        this.databaseConnection = databaseConnection;
        this.webhookService = webhookService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        //Ensure only 1 argument is entered (a player name presumably)
        if(args.length != 1){
            commandSender.sendMessage("§4Invalid arguments, do '/warnings {user}");
            return true;
        }

        //Database logic
        try(PreparedStatement ps = databaseConnection.prepareStatement("SELECT id,reason FROM warnings WHERE culprit=?")){
            ps.setString(1, args[0]);
            try(ResultSet resultSet = ps.executeQuery()) {
                //Put ResultSet into a List, as it cannot by itself cannot determine size -- required for header
                List<String> warnings = new ArrayList<>();
                while (resultSet.next()) {
                    warnings.add(resultSet.getInt("id") + ": " + resultSet.getString("reason"));
                }
                //Send warnings to the player
                commandSender.sendMessage("§6~~" + args[0] + " " + warnings.size() + "~~");
                for (String reason : warnings) {
                    commandSender.sendMessage("§b" + reason);
                }
            }
        } catch (SQLException e) {
            webhookService.sendError("SQL Error:\\n" + e);
            plugin.getLogger().log(Level.SEVERE,"SQL Error", e);
            return true;
        }

        return true;
    }
}
