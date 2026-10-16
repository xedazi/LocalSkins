package com.xedazi.localskins.gui;

import com.xedazi.localskins.skin.LocalSkinManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkinSelectScreen extends Screen {
    private final Screen parent;
    private final List<Path> skins = new ArrayList<>();
    private final List<Path> visibleSkins = new ArrayList<>();
    private int scroll = 0;
    private String status = "";
    private Path selectedFile;
    private TextFieldWidget searchField;
    private TextFieldWidget renameField;
    private TextFieldWidget urlField;
    private static final int ROW_HEIGHT = 22;
    private static final int LEFT_WIDTH = 310;

    public SkinSelectScreen(Screen parent) { super(new TranslatableText("screen.localskins.title")); this.parent = parent; }

    @Override protected void init() {
        int left = this.width / 2 - LEFT_WIDTH / 2;
        searchField = new TextFieldWidget(this.textRenderer, left, 48, LEFT_WIDTH, 18, new LiteralText("Search"));
        searchField.setMaxLength(100);
        searchField.setSuggestion("Search by file name...");
        searchField.setChangedListener(value -> { scroll = 0; rebuildVisible(); });
        this.addChild(searchField);
        renameField = new TextFieldWidget(this.textRenderer, left, 70, LEFT_WIDTH, 18, new LiteralText("Rename"));
        renameField.setMaxLength(80);
        renameField.setSuggestion("New file name (without .png)");
        this.addChild(renameField);
        urlField = new TextFieldWidget(this.textRenderer, left, 92, LEFT_WIDTH, 18, new LiteralText("PNG URL"));
        urlField.setMaxLength(500);
        urlField.setSuggestion("Direct link to a PNG image");
        this.addChild(urlField);

        int bw = 74, gap = 4, row1 = this.height - 52, row2 = this.height - 28;
        int x = this.width / 2 - (bw * 4 + gap * 3) / 2;
        addButton(new ButtonWidget(x, row2, bw, 20, new LiteralText("Folder"), b -> LocalSkinManager.openSkinDirectory()));
        addButton(new ButtonWidget(x + bw + gap, row2, bw, 20, new TranslatableText("screen.localskins.refresh"), b -> refreshSkins()));
        addButton(new ButtonWidget(x + (bw + gap) * 2, row2, bw, 20, new TranslatableText("screen.localskins.clear"), b -> { LocalSkinManager.clearSelection(); status = "Skin cleared"; }));
        addButton(new ButtonWidget(x + (bw + gap) * 3, row2, bw, 20, new TranslatableText("gui.done"), b -> this.client.openScreen(parent)));
        int wide = 100, total = wide * 3 + 5 * 2, x2 = this.width / 2 - total / 2;
        addButton(new ButtonWidget(x2, row1, wide, 20, new LiteralText("Rename"), b -> renameSelected()));
        addButton(new ButtonWidget(x2 + wide + 5, row1, wide, 20, new LiteralText("Download PNG"), b -> downloadFromUrl()));
        addButton(new ButtonWidget(x2 + (wide + 5) * 2, row1, wide, 20, new LiteralText("Refresh list"), b -> refreshSkins()));
        refreshSkins();
    }

    private int listTop() { return 116; }
    private int visibleRows() { return Math.max(1, (this.height - 190) / ROW_HEIGHT); }

    private void rebuildVisible() {
        visibleSkins.clear();
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        for (Path p : skins) if (p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).contains(q)) visibleSkins.add(p);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, visibleSkins.size() - visibleRows())));
    }

    private void refreshSkins() {
        skins.clear(); skins.addAll(LocalSkinManager.listSkins());
        if (selectedFile != null && !java.nio.file.Files.exists(selectedFile)) selectedFile = null;
        rebuildVisible();
    }

    private void renameSelected() {
        if (selectedFile == null) { status = "Select a skin in the list first"; return; }
        try {
            selectedFile = LocalSkinManager.renameSkin(selectedFile, renameField.getText());
            renameField.setText(""); status = "Renamed: " + selectedFile.getFileName(); refreshSkins();
        } catch (Exception e) { status = e.getMessage() == null ? "Rename failed" : e.getMessage(); }
    }

    private void downloadFromUrl() {
        final String address = urlField.getText().trim();
        if (address.isEmpty()) { status = "Paste a direct PNG URL first"; return; }
        status = "Downloading PNG...";
        Thread worker = new Thread(() -> {
            try {
                Path downloaded = LocalSkinManager.downloadSkin(address);
                MinecraftClient.getInstance().execute(() -> {
                    urlField.setText(""); status = "Downloaded: " + downloaded.getFileName(); refreshSkins();
                });
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> status = "Download failed: " + (e.getMessage() == null ? "invalid URL" : e.getMessage()));
            }
        }, "LocalSkins-download");
        worker.setDaemon(true); worker.start();
    }

    @Override public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Folder: " + LocalSkinManager.getSkinDirectory()), this.width / 2, 25, 0xAAAAAA);
        int left = this.width / 2 - LEFT_WIDTH / 2, top = listTop();
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= visibleSkins.size()) break;
            Path skin = visibleSkins.get(index); int y = top + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX <= left + LEFT_WIDTH && mouseY >= y && mouseY < y + 19;
            fill(matrices, left, y, left + LEFT_WIDTH, y + 19, skin.equals(selectedFile) ? 0xFF454545 : (hovered ? 0xFF555555 : 0xFF333333));
            String name = skin.getFileName().toString();
            if (name.length() > 38) name = name.substring(0, 35) + "...";
            drawTextWithShadow(matrices, this.textRenderer, new LiteralText(name), left + 7, y + 5, 0xFFFFFF);
            if (skin.getFileName().toString().equals(LocalSkinManager.getSelectedFileName())) drawTextWithShadow(matrices, this.textRenderer, new LiteralText("[active]"), left + 245, y + 5, 0x55FF55);
        }
        if (visibleSkins.isEmpty()) drawCenteredText(matrices, this.textRenderer, new LiteralText("No matching PNG skins"), this.width / 2, top + 12, 0xFFAAAA);
        drawCenteredText(matrices, this.textRenderer, new LiteralText(status), this.width / 2, this.height - 66, 0xFFFF55);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField.mouseClicked(mouseX, mouseY, button) || renameField.mouseClicked(mouseX, mouseY, button) || urlField.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            int left = this.width / 2 - LEFT_WIDTH / 2, top = listTop();
            if (mouseX >= left && mouseX <= left + LEFT_WIDTH && mouseY >= top) {
                int row = ((int) mouseY - top) / ROW_HEIGHT, index = scroll + row;
                if (row >= 0 && row < visibleRows() && index >= 0 && index < visibleSkins.size()) {
                    selectedFile = visibleSkins.get(index);
                    if (LocalSkinManager.select(selectedFile)) status = "Applied: " + selectedFile.getFileName();
                    else status = "Invalid PNG: expected 64x64 or 64x32";
                    renameField.setText(selectedFile.getFileName().toString().replaceAll("(?i)\\.png$", ""));
                    renameField.setFocused(true);
                    renameField.setCursorToEnd(false);
                    status = "Selected: edit the name above, then press Rename";
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll = Math.max(0, Math.min(Math.max(0, visibleSkins.size() - visibleRows()), scroll - (int)Math.signum(amount) * 2));
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { this.client.openScreen(parent); return true; }
        if (searchField.isFocused() || renameField.isFocused() || urlField.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == 264) { scroll = Math.min(Math.max(0, visibleSkins.size() - visibleRows()), scroll + 1); return true; }
        if (keyCode == 265) { scroll = Math.max(0, scroll - 1); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
