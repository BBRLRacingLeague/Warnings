package mplugin.net.minecraftEasyWarnings.minecraftCommands;

import mplugin.net.minecraftEasyWarnings.resources.WebhookService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class Warn implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Connection databaseConnection;
    private final WebhookService webhookService;

    private final String[] defaultReasons;

    public Warn(JavaPlugin plugin, Connection databaseConnection, WebhookService webhookService){
        this.plugin = plugin;
        this.databaseConnection = databaseConnection;
        this.webhookService = webhookService;

        defaultReasons = plugin.getConfig().getStringList("WARN_REASONS").toArray(new String[0]);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args){
        //Stop console issuing commands
        if(!(commandSender instanceof Player sender)){
            commandSender.sendMessage("§4You cannot issue this command from the console!");
            return true;
        }

        //Checks if a reason/culprit was given
        if(args.length <= 1){
            return false;
        }

        OfflinePlayer culprit = Bukkit.getOfflinePlayer(args[0]);
        String reason = createReason(args);

        //Unlikely but in theory possible
        if(culprit.getName() == null){
            sender.sendMessage("§4Could not resolve culprit's name!");
            webhookService.sendError("Unable to resolve culprit's name!\n" + args[0]);
            return true;
        }

        //Catch self-warns
        if(culprit.getUniqueId().equals(sender.getUniqueId())){
            sender.sendMessage("§4You cannot warn yourself!");
            return true;
        }

        //If warn reason contains a ';' then we can assume an SQL Injection is taking place -- no logical reason to use one in a warning message
        if(reason.contains(";")){
            sender.sendMessage("§4SQL Injection Detector Activated, command not running!");
            webhookService.sendError("SQL Injection Attempt Recognised!\nSender: " + sender.getName() + "\nReason: \"" + reason + "\"");
            return true;
        }

        //Shouldn't be able to warn players who have never joined before
        if(!culprit.hasPlayedBefore() && !culprit.isOnline()){
            sender.sendMessage("§4The player " + args[0] + " has never joined the server!");
            return true;
        }

        //Create the logs and send a message to the player to confirm the warning has been issued
        log(culprit.getName(), sender.getName(), reason);
        sender.sendMessage("§2Warned " + culprit.getName() + " for \"" + reason + "\"!");

        return true;
    }

    public String createReason(String[] args){
        return String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace("\"", "'");
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 1) { //Shows online Players
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase()
                            .startsWith(args[0].toLowerCase()))
                    .toList();
        } else if(args.length == 2){ //Shows default warns reasons -- not mandated to be one of these
            return Arrays.stream(defaultReasons).filter(reason -> reason.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }

        return Collections.emptyList();
    }
}
