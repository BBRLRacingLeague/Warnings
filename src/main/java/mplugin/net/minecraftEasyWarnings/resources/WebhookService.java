package mplugin.net.minecraftEasyWarnings.resources;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebhookService {
    public String TROUBLESHOOT_WEBHOOK_URL;

    public WebhookService(JavaPlugin plugin){
        TROUBLESHOOT_WEBHOOK_URL = plugin.getConfig().getString("TROUBLESHOOT_WEBHOOK_URL");
    }

    public void sendRequest(String json, String WEBHOOK_URL){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL + "?wait=true"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        }catch(Exception e){
            sendError("Error: \n" + e);
        }
    }

    public void sendError(String errorMessage){
        StringBuilder json = new StringBuilder("""
                {
                    "content": "%s",
                    "username": "Warnings Bot"
                }
                """.formatted(errorMessage));
        sendRequest(json.toString(), TROUBLESHOOT_WEBHOOK_URL);
    }
}
