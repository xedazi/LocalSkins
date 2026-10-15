package com.xedazi.localskins.mixin;

import com.xedazi.localskins.skin.LocalSkinManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void localskins$overrideLocalSkin(CallbackInfoReturnable<Identifier> cir) {
        if ((Object) this == MinecraftClient.getInstance().player && LocalSkinManager.getSelectedTexture() != null) {
            cir.setReturnValue(LocalSkinManager.getSelectedTexture());
        }
    }

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void localskins$overrideLocalModel(CallbackInfoReturnable<String> cir) {
        if ((Object) this == MinecraftClient.getInstance().player && LocalSkinManager.getSelectedTexture() != null) {
            cir.setReturnValue(LocalSkinManager.getSelectedModel());
        }
    }
}
