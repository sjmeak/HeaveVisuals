package rtx.heave.manager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import rtx.heave.api.chat.commands.CommandManager;
import rtx.heave.api.config.ConfigManager;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.holyworld.HolyWorldApi;
import rtx.heave.api.mods.chatanim.ChatAnimationMod;
import rtx.heave.api.mods.chathads.ChatHeads;
import rtx.heave.api.mods.geckolib.GeckoLibClient;
import rtx.heave.api.mods.shulkerview.ShulkerViewMod;
import rtx.heave.api.mods.waveycapes.WaveyCapesMod;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.party.PartyClient;
import rtx.heave.utils.animations.AnimationUtil;
import rtx.heave.utils.render.render2d.Render2D;
import rtx.heave.utils.render.render2d.gif.GifRenderer;
import rtx.heave.utils.render.warmup.Load;
import rtx.heave.utils.render.warmup.Render2DWarmup;
import rtx.heave.utils.sounds.SoundManager;
import rtx.heave.utils.storage.macro.MacroHandler;

public final class Manager {
    private Manager() {
    }

    public static void shutdown() {
        ConfigManager.saveAll();
        HolyWorldApi.getInstance().shutdown();
        PartyClient.INSTANCE.stop();
        Render2D.close();
    }

    public static void init() {
        Render2D.init();
        Render2DWarmup.init();
        AnimationUtil.init();
        ModuleManager.get().init();
        ConfigManager.init();
        CommandManager.get().init();
        MacroHandler.init();
        DragSystem.get().init();
        HolyWorldApi.getInstance();
        new GeckoLibClient().onInitializeClient();
        SoundManager.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraftClient -> Manager.shutdown());
        WaveyCapesMod.INSTANCE.init();
        ShulkerViewMod.init();
        ChatHeads.init();
        ChatAnimationMod.init();
        MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
        Runnable runnable = () -> {
            GifRenderer.preload("heave:gif/kity.gif");
            Load.runStartupWarmup();
        };
        if (minecraftClient2 != null) {
            minecraftClient2.execute(runnable);
        } else {
            runnable.run();
        }
        Identifier identifier = Identifier.of((String)"heave", (String)"ui_font_rewarmup");
        ResourceLoader.get((ResourceType)ResourceType.CLIENT_RESOURCES).registerReloader(identifier, (ResourceReloader)((SynchronousResourceReloader)resourceManager -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient == null) {
                return;
            }
            minecraftClient.execute(() -> {
                boolean bl = !Load.initialReloadSeen;
                Load.initialReloadSeen = true;
                if (bl) {
                    Load.runStartupWarmup();
                    return;
                }
                Load.warmupFonts();
                try {
                    Render2DWarmup.reset();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
        }));
    }
}

