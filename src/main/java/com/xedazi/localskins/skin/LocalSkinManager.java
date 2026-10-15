package com.xedazi.localskins.skin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        catch (IOException e) { System.err.println("[LocalSkins] Could not create CustomSkins directory: " + e.getMessage()); }
    }

    public static Path getSkinDirectory() { return skinDirectory; }
    public static String getSelectedFileName() { return selectedFileName; }
    public static String getSelectedModel() { return selectedModel; }
    public static Identifier getSelectedTexture() { return selectedTexture; }

    public static List<Path> listSkins() {
        if (skinDirectory == null) initialize();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(skinDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .forEach(files::add);
        } catch (IOException e) {
            System.err.println("[LocalSkins] Could not list skins: " + e.getMessage());
        }
        Collections.sort(files, Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
        return files;
    }

    public static boolean select(Path file) {
        if (skinDirectory == null) initialize();
        if (file == null || !Files.isRegularFile(file)
                || !file.toAbsolutePath().normalize().getParent()
                    .equals(skinDirectory.toAbsolutePath().normalize())) {
            return false;
        }

        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (file.toAbsolutePath().normalize().equals(selectedPath)
                    && modified == selectedLastModified
                    && selectedTexture != null) {
                return true; // Already loaded; don't decode/register it again.
            }
        } catch (IOException ignored) {
            return false;
        }

        NativeImage image = null;
        try (InputStream input = Files.newInputStream(file)) {
            image = NativeImage.read(input);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width != 64 || (height != 64 && height != 32)) {
                image.close();
                return false;
            }
            if (height == 32) {
                NativeImage expanded = new NativeImage(64, 64, true);
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 64; x++) expanded.setPixelColor(x, y, image.getPixelColor(x, y));
                }
                image.close();
                image = expanded;
            }
            String base = file.getFileName().toString().replaceAll("(?i)\\.png$", "").replaceAll("[^a-zA-Z0-9_./-]", "_").toLowerCase(Locale.ROOT);
            Identifier id = new Identifier(NAMESPACE, "skins/" + base);
            MinecraftClient client = MinecraftClient.getInstance();
            Identifier previousTexture = selectedTexture;
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
            image = null; // TextureManager owns it now.

            // Only keep the currently selected local texture in memory.
            if (previousTexture != null && !previousTexture.equals(id)) {
                client.getTextureManager().destroyTexture(previousTexture);
            }

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
        } finally {
            if (image != null) image.close();
        }
    }

    public static void clearSelection() {
        if (selectedTexture != null && MinecraftClient.getInstance() != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(selectedTexture);
        }
        selectedTexture = null;
        selectedFileName = null;
        selectedPath = null;
        selectedLastModified = -1L;
        selectedModel = "default";
    }
}
