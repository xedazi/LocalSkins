package com.xedazi.localskins.gui;

import com.xedazi.localskins.skin.LocalSkinManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkinSelectScreen extends Screen {
    private final Screen parent;
    private final List<Path> skins = new ArrayList<>();
    private int scroll = 0;
    private String status = "";
    private static final int ROW_HEIGHT = 24;

    public SkinSelectScreen(Screen parent) {
        super(new TranslatableText("screen.localskins.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshSkins();

        int left = this.width / 2 - 155;
        int bottom = this.height - 34;

        addButton(new ButtonWidget(
                left, bottom, 100, 20,
                new TranslatableText("screen.localskins.refresh"),
                b -> refreshSkins()
        ));

        addButton(new ButtonWidget(
                left + 105, bottom, 100, 20,
                new TranslatableText("screen.localskins.clear"),
                b -> {
                    LocalSkinManager.clearSelection();
                    status = "Selection cleared";
                }
        ));

        addButton(new ButtonWidget(
                left + 210, bottom, 100, 20,
                new TranslatableText("gui.done"),
                b -> this.client.openScreen(parent)
        ));
    }

    private void refreshSkins() {
        skins.clear();
        skins.addAll(LocalSkinManager.listSkins());
        scroll = Math.max(0, Math.min(
                scroll,
                Math.max(0, skins.size() - visibleRows())
        ));
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 105) / ROW_HEIGHT);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        drawCenteredText(
                matrices, this.textRenderer, this.title,
                this.width / 2, 16, 0xFFFFFF
        );

        drawCenteredText(
                matrices, this.textRenderer,
                new LiteralText("Folder: " + LocalSkinManager.getSkinDirectory()),
                this.width / 2, 32, 0xAAAAAA
        );

        int left = this.width / 2 - 155;
        int top = 52;
        int rows = visibleRows();

        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            if (index >= skins.size()) {
                break;
            }

            Path skin = skins.get(index);
            int y = top + row * ROW_HEIGHT;

            boolean hovered = mouseX >= left
                    && mouseX <= left + 310
                    && mouseY >= y
                    && mouseY < y + 20;

            fill(
                    matrices, left, y, left + 310, y + 20,
                    hovered ? 0xFF555555 : 0xFF333333
            );

            String name = skin.getFileName().toString();
            if (name.length() > 38) {
                name = name.substring(0, 35) + "...";
            }

            drawTextWithShadow(
                    matrices, this.textRenderer, new LiteralText(name),
                    left + 7, y + 6, 0xFFFFFF
            );

            if (skin.getFileName().toString()
                    .equals(LocalSkinManager.getSelectedFileName())) {
                drawTextWithShadow(
                        matrices, this.textRenderer,
                        new LiteralText("[selected]"),
                        left + 240, y + 6, 0x55FF55
                );
            }
        }

        if (skins.isEmpty()) {
            drawCenteredText(
                    matrices, this.textRenderer,
                    new LiteralText(
                            "No PNG skins found. Put PNG files in CustomSkins."
                    ),
                    this.width / 2, top + 20, 0xFFAAAA
            );
        }

        drawCenteredText(
                matrices, this.textRenderer, new LiteralText(status),
                this.width / 2, this.height - 48, 0xFFFF55
        );

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = this.width / 2 - 155;
            int top = 52;
            int row = ((int) mouseY - top) / ROW_HEIGHT;

            if (mouseX >= left && mouseX <= left + 310
                    && mouseY >= top
                    && row >= 0 && row < visibleRows()) {

                int index = scroll + row;

                if (index < skins.size()) {
                    Path path = skins.get(index);

                    if (LocalSkinManager.select(path)) {
                        status = "Applied: " + path.getFileName();
                    } else {
                        status = "Invalid PNG: expected 64x64 or 64x32";
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int max = Math.max(0, skins.size() - visibleRows());

        scroll = Math.max(
                0,
                Math.min(max, scroll - (int) Math.signum(amount) * 2)
        );

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.client.openScreen(parent);
            return true;
        }

        if (keyCode == 264) {
            scroll = Math.min(
                    Math.max(0, skins.size() - visibleRows()),
                    scroll + 1
            );
            return true;
        }

        if (keyCode == 265) {
            scroll = Math.max(0, scroll - 1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
