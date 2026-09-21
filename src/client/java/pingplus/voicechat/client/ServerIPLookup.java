package pingplus.voicechat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

public class ServerIPLookup {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerIPLookup.class);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record ServerInfo(String address, String country, String city) {
        public String displayText() {
            return address + "\nApproximate location: " + city + ", " + country;
        }
    }

    // DNS and HTTP are blocking: call this from a background task.
    public ArrayList<ServerInfo> returnIp(ServerData server) {
        ArrayList<ServerInfo> results = new ArrayList<>();
        ServerAddress address = ServerAddress.parseString(server.ip);

        ServerNameResolver.DEFAULT.resolveAddress(address).ifPresent(resolved -> {
            String ip = resolved.getHostIp();
            int port = resolved.getPort();
            String endpoint = ip.contains(":")
                    ? "[" + ip + "]:" + port
                    : ip + ":" + port;
            results.add(lookupLocation(ip, endpoint));
        });
        return results;
    }

    private ServerInfo lookupLocation(String ip, String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipwho.is/" + ip + "?fields=success,country,city"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    return new ServerInfo(endpoint, readText(json, "country"), readText(json, "city"));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("Could not look up server location", e);
        }
        return new ServerInfo(endpoint, "Unknown", "Unknown");
    }

    private static String readText(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return "Unknown";
        }
        String value = json.get(key).getAsString();
        return value.isBlank() ? "Unknown" : value;
    }
}
