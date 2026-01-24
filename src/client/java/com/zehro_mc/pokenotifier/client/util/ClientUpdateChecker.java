package com.zehro_mc.pokenotifier.client.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.client.PokeNotifierClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientUpdateChecker {
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String MODRINTH_PROJECT_ID = "yRK8rhwf";
    private static final String CURSEFORGE_PROJECT_ID = "1365296";

    public static CompletableFuture<Void> checkForUpdates() {
        String source = PokeNotifierClient.currentUpdateSource;
        if (source == null || source.equalsIgnoreCase("none")) {
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Please configure update source in GUI (Info & Help tab)").formatted(Formatting.YELLOW), false);
                }
            });
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                if (source.equalsIgnoreCase("modrinth")) {
                    checkModrinth();
                } else if (source.equalsIgnoreCase("curseforge")) {
                    checkCurseForge();
                }
            } catch (Exception e) {
                PokeNotifier.LOGGER.warn("[ClientUpdateChecker] Failed: " + e.getMessage());
            }
        });
    }

    private static void checkModrinth() throws Exception {
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String gameVersions = URLEncoder.encode("[\"" + MINECRAFT_VERSION + "\"]", StandardCharsets.UTF_8);
        String url = String.format("https://api.modrinth.com/v2/project/%s/version?loaders=%s&game_versions=%s",
                MODRINTH_PROJECT_ID, loaders, gameVersions);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonArray versions = new Gson().fromJson(response.body(), JsonArray.class);
            if (!versions.isEmpty()) {
                JsonObject latestVersion = versions.get(0).getAsJsonObject();
                String latestVersionNumber = latestVersion.get("version_number").getAsString();
                compareVersion(latestVersionNumber, "https://modrinth.com/mod/" + MODRINTH_PROJECT_ID);
            }
        }
    }

    private static void checkCurseForge() throws Exception {
        String url = String.format("https://api.cfwidget.com/%s", CURSEFORGE_PROJECT_ID);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject projectData = new Gson().fromJson(response.body(), JsonObject.class);
            JsonArray files = projectData.getAsJsonArray("files");
            for (var fileElement : files) {
                JsonObject file = fileElement.getAsJsonObject();
                JsonArray gameVersions = file.getAsJsonArray("versions");
                boolean isCompatible = false;
                for (var versionElement : gameVersions) {
                    if (versionElement.getAsString().contains(MINECRAFT_VERSION)) {
                        isCompatible = true;
                        break;
                    }
                }
                if (isCompatible) {
                    String fileName = file.get("name").getAsString();
                    Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
                    Matcher matcher = pattern.matcher(fileName);
                    if (matcher.find()) {
                        String latestVersionNumber = matcher.group(1);
                        compareVersion(latestVersionNumber, projectData.get("urls").getAsJsonObject().get("curseforge").getAsString());
                        return;
                    }
                }
            }
        }
    }

    private static void compareVersion(String latestVersionNumber, String downloadUrl) {
        String currentVersion = FabricLoader.getInstance()
                .getModContainer(PokeNotifier.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player != null) {
                if (isNewer(latestVersionNumber, currentVersion)) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("A new version of Poke Notifier is available: ").formatted(Formatting.GREEN)
                            .append(Text.literal(latestVersionNumber).formatted(Formatting.GOLD)), false);
                } else {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("Poke Notifier is up to date!").formatted(Formatting.GREEN), false);
                }
            }
        });
    }

    private static boolean isNewer(String newVersionStr, String currentVersionStr) {
        try {
            String cleanNew = newVersionStr.split("-")[0].trim();
            String cleanCurrent = currentVersionStr.split("-")[0].trim();
            String[] newParts = cleanNew.split("\\.");
            String[] currentParts = cleanCurrent.split("\\.");
            int length = Math.max(newParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
