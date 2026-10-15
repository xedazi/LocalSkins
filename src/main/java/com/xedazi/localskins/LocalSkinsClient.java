package com.xedazi.localskins;

import com.xedazi.localskins.gui.SkinSelectScreen;
import com.xedazi.localskins.skin.LocalSkinManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class LocalSkinsClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        LocalSkinManager.initialize();
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.localskins.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.localskins"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.openScreen(new SkinSelectScreen(client.currentScreen));
                }
            }
        });


    }

    public static MinecraftClient client() { return MinecraftClient.getInstance(); }
}
