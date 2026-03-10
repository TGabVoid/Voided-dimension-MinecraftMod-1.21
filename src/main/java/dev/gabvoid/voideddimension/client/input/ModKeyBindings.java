package dev.gabvoid.voideddimension.client.input;

import dev.gabvoid.voideddimension.client.screen.ArcaneAdvancementsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {
    private static KeyBinding openArcaneScreen9;
    private static KeyBinding openArcaneScreen0;

    private ModKeyBindings() {
    }

    public static void register() {
        openArcaneScreen9 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.voideddimension.arcane_screen_9",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_9,
            "category.voideddimension"
        ));

        openArcaneScreen0 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.voideddimension.arcane_screen_0",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            "category.voideddimension"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ModKeyBindings::handleKeyPress);
    }

    private static void handleKeyPress(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        if (openArcaneScreen9.wasPressed() || openArcaneScreen0.wasPressed()) {
            toggleArcaneScreen(client);
        }
    }

    private static void toggleArcaneScreen(MinecraftClient client) {
        if (client.currentScreen instanceof ArcaneAdvancementsScreen) {
            client.setScreen(null);
            return;
        }

        client.setScreen(new ArcaneAdvancementsScreen());
    }
}

