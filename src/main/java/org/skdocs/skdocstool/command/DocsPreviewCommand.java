package org.skdocs.skdocstool.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DocsPreviewCommand {

    private final JavaPlugin plugin;
    private static final String API_URL = "https://minestom.skdocs.org/api/preview";
    private static final Pattern SKDOCS_PATTERN = Pattern.compile("^skdocs-.*\\.json$");

    public DocsPreviewCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> getAvailableAddons(String partial) {
        List<String> addons = new ArrayList<>();
        Path docsPath = plugin.getDataFolder().toPath().resolve("documentation");

        if (!Files.exists(docsPath)) {
            return addons;
        }

        try (var stream = Files.list(docsPath)) {
            stream.filter(p -> SKDOCS_PATTERN.matcher(p.getFileName().toString()).matches())
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        String addonName = fileName
                                .replaceFirst("^skdocs-", "")
                                .replaceFirst("\\.json$", "");
                        if (addonName.toLowerCase().startsWith(partial.toLowerCase())) {
                            addons.add(addonName);
                        }
                    });
        } catch (IOException ignored) {
        }

        return addons;
    }

    public void generatePreview(CommandSender sender, String addonName) {
        Path docsPath = plugin.getDataFolder().toPath().resolve("documentation");

        if (!Files.exists(docsPath)) {
            sender.sendMessage(Component.text("No documentation found. Run ", NamedTextColor.RED)
                    .append(Component.text("/gendocs", NamedTextColor.YELLOW))
                    .append(Component.text(" first.", NamedTextColor.RED)));
            return;
        }

        File foundFile = findAddonFile(docsPath, addonName);

        if (foundFile == null) {
            sender.sendMessage(Component.text("Addon documentation not found: ", NamedTextColor.RED)
                    .append(Component.text(addonName != null ? addonName : "unknown", NamedTextColor.YELLOW))
                    .append(Component.text(". Run ", NamedTextColor.RED))
                    .append(Component.text("/gendocs", NamedTextColor.YELLOW))
                    .append(Component.text(" first.", NamedTextColor.RED)));
            return;
        }

        sender.sendMessage(Component.text("Uploading ", NamedTextColor.GRAY)
                .append(Component.text(addonName, NamedTextColor.AQUA))
                .append(Component.text(" to skdocs preview API...", NamedTextColor.GRAY)));

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                uploadAndPreview(sender, foundFile);
            } catch (Exception e) {
                sender.sendMessage(Component.text("Failed to create preview: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Preview upload failed: " + e.getMessage());
            }
        });
    }

    private File findAddonFile(Path docsPath, String addonName) {
        File[] files = docsPath.toFile().listFiles((d, name) -> SKDOCS_PATTERN.matcher(name).matches());
        if (files == null) {
            return null;
        }

        for (File file : files) {
            String fileName = file.getName();
            String fileAddonName = fileName
                    .replaceFirst("^skdocs-", "")
                    .replaceFirst("\\.json$", "");
            if (fileAddonName.equalsIgnoreCase(addonName)) {
                return file;
            }
        }

        return null;
    }

    private void uploadAndPreview(CommandSender sender, File addonFile) throws IOException {
        String jsonContent = Files.readString(addonFile.toPath(), StandardCharsets.UTF_8);

        JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();
        String addonNameFromJson = json.has("addon") ? json.get("addon").getAsString() : "Unknown";

        String boundary = "----" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(jsonContent, boundary);

        URL url;
        try {
            url = new java.net.URI(API_URL).toURL();
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Invalid API URL: " + e.getMessage(), e);
        }

        var connection = url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Content-Length", String.valueOf(body.length));

        try (var os = connection.getOutputStream()) {
            os.write(body);
            os.flush();
        }

        java.net.HttpURLConnection httpConnection = (java.net.HttpURLConnection) connection;
        int responseCode = httpConnection.getResponseCode();

        String responseContent = readResponseContent(httpConnection);

        if (responseCode == 429) {
            handleRateLimit(sender, httpConnection);
            return;
        }

        if (responseCode != 200) {
            throw new IOException("API returned status " + responseCode + ": " + responseContent);
        }

        JsonObject response = JsonParser.parseString(responseContent).getAsJsonObject();
        String previewUrl = response.get("previewUrl").getAsString();
        int docCount = response.get("docCount").getAsInt();
        long expiresAt = response.get("expiresAt").getAsLong();

        long expiresIn = (expiresAt - System.currentTimeMillis()) / 1000;

        net.minestom.server.MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            sender.sendMessage("");
            sender.sendMessage(Component.text("Preview created successfully!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Addon: ", NamedTextColor.GRAY)
                    .append(Component.text(addonNameFromJson, NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("Entries parsed: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(docCount), NamedTextColor.YELLOW)));
            sender.sendMessage(Component.text("Expires in: ", NamedTextColor.GRAY)
                    .append(Component.text(formatSeconds(expiresIn), NamedTextColor.YELLOW)));
            sender.sendMessage("");

            String fullUrl = "https://minestom.skdocs.org" + previewUrl;
            sender.sendMessage(Component.text("Preview URL:", NamedTextColor.GRAY));
            sender.sendMessage(Component.text(fullUrl, NamedTextColor.BLUE)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(fullUrl)));
            sender.sendMessage("");
        });
    }

    private byte[] buildMultipartBody(String jsonContent, String boundary) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"addon.json\"\r\n"
                + "Content-Type: application/json\r\n"
                + "\r\n";

        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[headerBytes.length + contentBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(contentBytes, 0, result, headerBytes.length, contentBytes.length);
        System.arraycopy(footerBytes, 0, result, headerBytes.length + contentBytes.length, footerBytes.length);

        return result;
    }

    private String readResponseContent(java.net.HttpURLConnection httpConnection) throws IOException {
        try (InputStream is = httpConnection.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            try (InputStream es = httpConnection.getErrorStream()) {
                if (es != null) {
                    return new String(es.readAllBytes(), StandardCharsets.UTF_8);
                } else {
                    return e.getMessage();
                }
            }
        }
    }

    private void handleRateLimit(CommandSender sender, java.net.HttpURLConnection httpConnection) throws IOException {
        String retryAfter = httpConnection.getHeaderField("Retry-After");
        if (retryAfter == null) {
            throw new IOException("Rate limit exceeded but no Retry-After header provided");
        }

        int waitSeconds;
        try {
            waitSeconds = Integer.parseInt(retryAfter);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid Retry-After header value: " + retryAfter);
        }

        final int finalWaitSeconds = waitSeconds;
        net.minestom.server.MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            sender.sendMessage(Component.text("Rate limit exceeded!", NamedTextColor.RED));
            sender.sendMessage(Component.text("Please wait ", NamedTextColor.GRAY)
                    .append(Component.text(formatSeconds(finalWaitSeconds), NamedTextColor.YELLOW))
                    .append(Component.text(" before uploading another preview.", NamedTextColor.GRAY)));
        });
    }

    private String formatSeconds(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}

