package com.xedazi.localskins.skin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class LocalSkinManager {
    private static final String NAMESPACE = "localskins";
    private static Path skinDirectory;
    private static Identifier selectedTexture;
    private static String selectedModel = "default";
    private static String selectedFileName;
    private static Path selectedPath;
    private static long selectedLastModified = -1L;

    private LocalSkinManager() { }

    public static void initialize() {
        skinDirectory = MinecraftClient.getInstance().runDirectory.toPath().resolve("CustomSkins");
        try { Files.createDirectories(skinDirectory); }
        catch (Exception e) { System.err.println("[LocalSkins] Cannot create skin folder: " + e.getMessage()); }
    }

    public static Path getSkinDirectory() { if (skinDirectory == null) initialize(); return skinDirectory; }
    public static String getSelectedFileName() { return selectedFileName; }
    public static String getSelectedModel() { return selectedModel; }
    public static Identifier getSelectedTexture() { return selectedTexture; }

    public static void openSkinDirectory() {
        try { Util.getOperatingSystem().open(getSkinDirectory().toFile()); }
        catch (Exception e) { System.err.println("[LocalSkins] Cannot open folder: " + e.getMessage()); }
    }

    public static List<Path> listSkins() {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getSkinDirectory())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .forEach(files::add);
        } catch (Exception e) { System.err.println("[LocalSkins] Cannot list skins: " + e.getMessage()); }
        Collections.sort(files, Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
        return files;
    }

    public static Path renameSkin(Path file, String newName) throws Exception {
        if (file == null || !Files.isRegularFile(file)) throw new IllegalArgumentException("Select a skin first");
        String safe = newName == null ? "" : newName.trim().replaceAll("(?i)\\.png$", "").replaceAll("[^a-zA-Z0-9 _.-]", "_");
        if (safe.isEmpty()) throw new IllegalArgumentException("Enter a new file name");
        if (safe.length() > 80) safe = safe.substring(0, 80);
        Path target = getSkinDirectory().resolve(safe + ".png");
        if (!target.toAbsolutePath().normalize().getParent().equals(getSkinDirectory().toAbsolutePath().normalize())) throw new IllegalArgumentException("Invalid name");
        if (Files.exists(target) && !target.equals(file)) throw new IllegalArgumentException("That name already exists");
        Path renamed = Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        if (renamed.toAbsolutePath().normalize().equals(selectedPath)) {
            selectedPath = renamed.toAbsolutePath().normalize();
            selectedFileName = renamed.getFileName().toString();
            selectedLastModified = Files.getLastModifiedTime(renamed).toMillis();
        }
        return renamed;
    }

    /** Downloads a direct PNG URL; website pages are not skin image files. */
    public static Path downloadSkin(String address) throws Exception {
        String value = address == null ? "" : address.trim();
        if (!(value.startsWith("https://") || value.startsWith("http://"))) throw new IllegalArgumentException("Enter a direct http(s) PNG link");
        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(7000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "LocalSkins/1.0 Minecraft");
        try {
            connection.connect();
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) throw new IllegalArgumentException("Download failed: HTTP " + connection.getResponseCode());
            String pathPart = new URL(connection.getURL().toString()).getPath();
            String name = pathPart.substring(pathPart.lastIndexOf('/') + 1).replaceAll("(?i)\\.png$", "");
            name = name.replaceAll("[^a-zA-Z0-9 _.-]", "_").trim();
            if (name.isEmpty()) name = "downloaded_skin";
            if (name.length() > 70) name = name.substring(0, 70);
            Path target = getSkinDirectory().resolve(name + ".png");
            if (Files.exists(target)) target = getSkinDirectory().resolve(name + "_" + System.currentTimeMillis() + ".png");
            try (InputStream in = connection.getInputStream()) {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int count;
                while ((count = in.read(chunk)) != -1) {
                    if (buffer.size() + count > 2 * 1024 * 1024) throw new IllegalArgumentException("PNG is too large (max 2 MB)");
                    buffer.write(chunk, 0, count);
                }
                byte[] data = buffer.toByteArray();
                if (data.length < 8 || data[0] != (byte)137 || data[1] != 80 || data[2] != 78 || data[3] != 71) throw new IllegalArgumentException("Link did not return a PNG image; use a direct PNG URL");
                try (NativeImage check = NativeImage.read(new java.io.ByteArrayInputStream(data))) {
                    if (check.getWidth() != 64 || (check.getHeight() != 64 && check.getHeight() != 32)) {
                        throw new IllegalArgumentException("Skin PNG must be 64x64 or 64x32");
                    }
                }
                Files.write(target, data);
            }
            return target;
        } finally { connection.disconnect(); }
    }

    public static boolean select(Path file) {
        Path directory = getSkinDirectory().toAbsolutePath().normalize();
        if (file == null || !Files.isRegularFile(file) || !file.toAbsolutePath().normalize().getParent().equals(directory)) return false;
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (file.toAbsolutePath().normalize().equals(selectedPath) && modified == selectedLastModified && selectedTexture != null) return true;
        } catch (Exception e) { return false; }

        NativeImage image = null;
        try (InputStream input = Files.newInputStream(file)) {
            image = NativeImage.read(input);
            int width = image.getWidth(), height = image.getHeight();
            if (width != 64 || (height != 64 && height != 32)) return false;
            if (height == 32) {
                NativeImage legacy = new NativeImage(64, 64, true);
                for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) legacy.setPixelColor(x, y, image.getPixelColor(x, y));
                // Legacy 64x32 skins have no separate left limbs. Mirror the right limbs into modern left-limb UV areas.
                legacy.copyRect(0, 16, 0, 48, 16, 16, true, false);
                legacy.copyRect(40, 16, 32, 48, 16, 16, true, false);
                image.close(); image = legacy;
            }
            String base = file.getFileName().toString().replaceAll("(?i)\\.png$", "").replaceAll("[^a-zA-Z0-9_./-]", "_").toLowerCase(Locale.ROOT);
            Identifier id = new Identifier(NAMESPACE, "skins/" + base);
            MinecraftClient client = MinecraftClient.getInstance();
            Identifier previous = selectedTexture;
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
            image = null;
            if (previous != null && !previous.equals(id)) client.getTextureManager().destroyTexture(previous);
            selectedTexture = id;
            selectedPath = file.toAbsolutePath().normalize();
            selectedLastModified = Files.getLastModifiedTime(file).toMillis();
            selectedFileName = file.getFileName().toString();
            String lower = selectedFileName.toLowerCase(Locale.ROOT);
            selectedModel = (lower.contains("slim") || lower.contains("alex")) ? "slim" : "default";
            return true;
        } catch (Exception e) {
            System.err.println("[LocalSkins] Failed to load skin " + file + ": " + e.getMessage());
            return false;
        } finally { if (image != null) image.close(); }
    }

    public static void clearSelection() {
        if (selectedTexture != null && MinecraftClient.getInstance() != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(selectedTexture);
        selectedTexture = null; selectedFileName = null; selectedPath = null; selectedLastModified = -1L; selectedModel = "default";
    }
}
