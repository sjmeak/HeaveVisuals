package ru.customgamegui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.customgamegui.config.CGGConfigManager;
import ru.customgamegui.gui.CGGConfigScreen;

public class CustomGameGui implements ClientModInitializer {
    public static final String MOD_ID = "cgg";
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        CGGConfigManager.load();
        ru.customgamegui.compat.AppleSkinCompat.init();

        // Register KeyBinding (default: 'O')
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cgg.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyBinding.Category.MISC
        ));

        // Listen for key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new CGGConfigScreen(null));
                }
            }
        });

        // Register client commands: /cgg and /customgamegui
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("cgg")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.send(() -> client.setScreen(new CGGConfigScreen(null)));
                        return 1;
                    })
                    .then(ClientCommandManager.literal("reload")
                            .executes(context -> {
                                CGGConfigManager.load();
                                context.getSource().sendFeedback(Text.translatable("cgg.command.reloaded"));
                                return 1;
                            }))
            );

            dispatcher.register(ClientCommandManager.literal("customgamegui")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.send(() -> client.setScreen(new CGGConfigScreen(null)));
                        return 1;
                    })
            );
        });
    }
}
