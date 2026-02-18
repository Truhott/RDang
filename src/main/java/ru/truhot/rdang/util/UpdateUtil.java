package ru.truhot.rdang.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.bukkit.Bukkit;
import ru.truhot.rdang.RDang;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateUtil {
    private final RDang plugin;
    private final String url = "https://api.github.com/repos/Truhott/RDang/releases/latest";
    @Getter
    private boolean updateAvailable = false;
    @Getter
    private String latestVersion = null;

    public UpdateUtil(RDang plugin) {
        this.plugin = plugin;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java 11 HttpClient (RDang)")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(response.body()).getAsJsonObject();
                    latestVersion = jsonObject.has("tag_name") ? jsonObject.get("tag_name").getAsString().replace("v", "") : "Unknown";
                    String currentVersion = plugin.getDescription().getVersion().replace("v", "");
                    updateAvailable = needsUpdate(currentVersion, latestVersion);
                    if (updateAvailable) {
                        plugin.getLogger().warning("------------------------------------------");
                        plugin.getLogger().warning("   [ ОБНОВЛЕНИЕ ДОСТУПНО ]");
                        plugin.getLogger().warning("");
                        plugin.getLogger().warning(" » Текущая версия: " + currentVersion);
                        plugin.getLogger().warning(" » Новая версия:    " + latestVersion);
                        plugin.getLogger().warning("");
                        plugin.getLogger().warning(" Ссылка: github.com/Truhott/RDang/releases");
                        plugin.getLogger().warning("------------------------------------------");
                    } else {
                        plugin.getLogger().info("Актуальная версия: " + currentVersion);
                    }
                } else {
                    plugin.getLogger().warning("Ошибка HTTP: " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка обновления: " + e.getMessage());
            }
        });
    }

    private boolean needsUpdate(String current, String latest) {
        try {
            String[] cP = current.split("\\.");
            String[] lP = latest.split("\\.");
            int length = Math.max(cP.length, lP.length);
            for (int i = 0; i < length; i++) {
                int cV = i < cP.length ? Integer.parseInt(cP[i]) : 0;
                int lV = i < lP.length ? Integer.parseInt(lP[i]) : 0;
                if (cV < lV) return true;
                if (cV > lV) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }
}