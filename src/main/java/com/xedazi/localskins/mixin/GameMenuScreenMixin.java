package com.xedazi.localskins.mixin;

import com.xedazi.localskins.gui.SkinSelectScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Shadow protected abstract <T extends Element & Drawable & Selectable> T addButton(T button);
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void localskins$addButton(CallbackInfo ci) {
        GameMenuScreen screen = (GameMenuScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        this.addButton(new ButtonWidget(this.width / 2 - 100, this.height / 4 + 120,
                200, 20, new TranslatableText("screen.localskins.open"),
                button -> client.openScreen(new SkinSelectScreen(screen))));
    }
}
