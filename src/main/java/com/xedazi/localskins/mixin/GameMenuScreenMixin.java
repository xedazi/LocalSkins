package com.xedazi.localskins.mixin;

import com.xedazi.localskins.screen.SkinSelectScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.Narratable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin() {
        super(new LiteralText("Game Menu"));
    }

    @Shadow
    protected abstract <T extends Element & Drawable & Narratable> T addButton(T button);

    @Inject(method = "init", at = @At("TAIL"))
    private void localskins$addSkinButton(CallbackInfo ci) {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int x = this.width / 2 - buttonWidth / 2;
        int y = this.height / 4 + 72;

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
