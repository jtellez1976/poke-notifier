/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    public record UpdateInfo(String version, String url) {}

    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String MODRINTH_PROJECT_ID = "yRK8rhwf"; // Your Modrinth Project ID/Slug
    private static final String CURSEFORGE_PROJECT_ID = "1365296"; // Your CurseForge Project ID

    public static CompletableFuture<Void> checkForUpdates(ServerPlayerEntity feedbackPlayer) {
        String source = ConfigManager.getServerConfig().update_checker_source;
        if (source == null || source.equalsIgnoreCase("unknown") || source.equalsIgnoreCase("none")) {
            if (feedbackPlayer != null) {
                feedbackPlayer.sendMessage(Text.literal("Update checker is disabled or not configured.").formatted(Formatting.YELLOW), false);
            }
            return CompletableFuture.completedFuture(null); // Return a completed future if no check is needed.
        }

        return CompletableFuture.runAsync(() -> {
            try {
                if (source.equalsIgnoreCase("modrinth")) {
                    checkModrinth(feedbackPlayer);
                } else if (source.equalsIgnoreCase("curseforge")) {
                    checkCurseForge(feedbackPlayer);
                }
            } catch (Exception e) {
                PokeNotifier.LOGGER.warn("[UpdateChecker] Failed to check for updates: " + e.getMessage());
                if (feedbackPlayer != null) {
                    feedbackPlayer.sendMessage(Text.literal("Failed to check for updates. See server console for details.").formatted(Formatting.RED), false);
                }
            }
        });
    }

    private static void checkModrinth(ServerPlayerEntity feedbackPlayer) throws Exception {
        // --- FIX: URL-encode parameters to handle special characters like [ and " ---
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
                compareAndSetVersion(latestVersionNumber, "https://modrinth.com/mod/" + MODRINTH_PROJECT_ID, feedbackPlayer);
            }
        }
    }

    private static void checkCurseForge(ServerPlayerEntity feedbackPlayer) throws Exception {
        // CurseForge API requires a key for direct access. We use a public proxy for simplicity.
        // This is less reliable but avoids needing to manage API keys.
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
                    // --- FIX: Use regex to reliably extract the version number from the file name ---
                    String fileName = file.get("name").getAsString();
                    // This regex looks for a version pattern like X.Y.Z that might be part of a larger string.
                    Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)"); 
                    Matcher matcher = pattern.matcher(fileName);

                    if (matcher.find()) {
                        String latestVersionNumber = matcher.group(1);
                        compareAndSetVersion(latestVersionNumber, projectData.get("urls").getAsJsonObject().get("curseforge").getAsString(), feedbackPlayer);
                        return; // Found the latest compatible version
                    }
                }
            }
        }
    }

    private static void compareAndSetVersion(String latestVersionNumber, String downloadUrl, ServerPlayerEntity feedbackPlayer) {
        String currentVersion = FabricLoader.getInstance()
                .getModContainer(PokeNotifier.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

        // Simple version comparison. Assumes semantic versioning without complex tags.
        if (isNewer(latestVersionNumber, currentVersion)) {
            PokeNotifier.LATEST_VERSION_INFO = new UpdateInfo(latestVersionNumber, downloadUrl);
            
            // --- MEJORA: Log a prominent message in the server console after a delay ---
            try {
                // Wait 5 seconds to let the server finish its initial loading spam.
                Thread.sleep(5000);
                PokeNotifier.LOGGER.info("*******************************************************");
                PokeNotifier.LOGGER.info("*   A new version of Poke Notifier is available!      *");
                PokeNotifier.LOGGER.info("*   Current: " + String.format("%-10s", currentVersion) + "  |   Latest: " + String.format("%-10s", latestVersionNumber) + "   *");
                PokeNotifier.LOGGER.info("*   Download at: " + downloadUrl + " *");
                PokeNotifier.LOGGER.info("*******************************************************");
            } catch (InterruptedException ignored) {
            }
            
            if (feedbackPlayer != null) {
                // --- FIX: Send the correct feedback message when an update is found ---
                Text updateMessage = Text.literal("A new version of Poke Notifier is available: ").formatted(Formatting.GREEN)
                        .append(Text.literal(latestVersionNumber).formatted(Formatting.GOLD));
                feedbackPlayer.sendMessage(updateMessage, false);
            }
        } else {
            PokeNotifier.LOGGER.info("[UpdateChecker] You are running the latest version.");
            if (feedbackPlayer != null) {
                feedbackPlayer.sendMessage(Text.literal("Poke Notifier is up to date!").formatted(Formatting.GREEN), false);
            }
        }
    }

    private static boolean isNewer(String newVersionStr, String currentVersionStr) {
        // --- FIX: More robust semantic version comparison ---
        try {
            // --- FIX: More robust version cleaning and comparison ---
            String cleanNew = newVersionStr.split("-")[0].trim();
            String cleanCurrent = currentVersionStr.split("-")[0].trim();

            String[] newParts = cleanNew.split("\\.");
            String[] currentParts = cleanCurrent.split("\\.");

            int length = Math.max(newParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                // Parse each part as an integer. If a part is missing, treat it as 0.
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            // Versions are identical
            return false;
        } catch (NumberFormatException e) {
            PokeNotifier.LOGGER.warn("[UpdateChecker] Could not parse version numbers for comparison: '{}' vs '{}'", newVersionStr, currentVersionStr);
            return false; // Fail safely
        }
    }
}
