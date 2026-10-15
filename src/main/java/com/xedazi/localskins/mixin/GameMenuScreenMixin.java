package com.xedazi.localskins.mixin;

import com.xedazi.localskins.gui.SkinSelectScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin() {
        super(new LiteralText("Game Menu"));
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void localskins$addSkinButton(CallbackInfo ci) {
        int buttonWidth = 120;
        int buttonHeight = 20;
        // Keep the custom button in the otherwise empty bottom area.
        // The vanilla pause-menu rows are around height / 4, so placing it there
        // causes it to overlap Settings / Open to LAN on common GUI scales.
        int x = this.width / 2 - buttonWidth / 2;
        int y = this.height - buttonHeight - 8;

        this.addButton(new ButtonWidget(
                x,
                y,
                buttonWidth,
                buttonHeight,
                new LiteralText("Local Skins"),
                button -> MinecraftClient.getInstance().openScreen(
                        new SkinSelectScreen((Screen) (Object) this)
                )
        ));
    }
}
