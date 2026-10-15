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
    protected GameMenuScreenMixin() { super(new LiteralText("Game Menu")); }

    @Inject(method = "init", at = @At("TAIL"))
    private void localskins$addSkinButton(CallbackInfo ci) {
        int w = 120;
        int x = this.width / 2 - w / 2;
        // Place in the gap between the settings/LAN row and Save & Quit.
        int y = this.height / 4 + 120;
        this.addButton(new ButtonWidget(x, y, w, 20, new LiteralText("Local Skins"),
                button -> MinecraftClient.getInstance().openScreen(new SkinSelectScreen((Screen) (Object) this))));
    }
}
