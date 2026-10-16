package com.xedazi.localskins.gui;

import com.xedazi.localskins.skin.LocalSkinManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import java.nio.file.Path;

public final class RenameSkinScreen extends Screen {
    private final Screen parent;
    private final Path file;
    private TextFieldWidget nameField;
    private String status = "";
    public RenameSkinScreen(Screen parent, Path file) {
        super(new TranslatableText("screen.localskins.rename_title"));
        this.parent = parent; this.file = file;
    }
    @Override protected void init() {
        int w = 220, x = width / 2 - w / 2;
        String old = file.getFileName().toString().replaceAll("(?i)\\.png$", "");
        nameField = new TextFieldWidget(textRenderer, x, height / 2 - 12, w, 20, new TranslatableText("screen.localskins.new_name"));
        nameField.setMaxLength(80); nameField.setText(old); addChild(nameField); setInitialFocus(nameField); nameField.setCursorToEnd();
        addButton(new ButtonWidget(width / 2 - 104, height / 2 + 18, 100, 20, new TranslatableText("screen.localskins.save"), b -> save()));
        addButton(new ButtonWidget(width / 2 + 4, height / 2 + 18, 100, 20, new TranslatableText("gui.cancel"), b -> client.openScreen(parent)));
    }
    private void save() {
        try { LocalSkinManager.renameSkin(file, nameField.getText()); client.openScreen(parent); }
        catch (Exception e) { status = e.getMessage() == null ? "screen.localskins.rename_error" : e.getMessage(); }
    }
    @Override public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices); drawCenteredText(matrices, textRenderer, title, width / 2, height / 2 - 42, 0xFFFFFF);
        if (!status.isEmpty()) drawCenteredText(matrices, textRenderer, new LiteralText(status), width / 2, height / 2 + 46, 0xFF5555);
        super.render(matrices, mouseX, mouseY, delta);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { client.openScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
