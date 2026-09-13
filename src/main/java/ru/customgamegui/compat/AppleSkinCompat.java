package ru.customgamegui.compat;

import net.fabricmc.loader.api.FabricLoader;
import ru.customgamegui.config.CGGConfig;
import ru.customgamegui.config.CGGConfigManager;
import squeek.appleskin.api.event.HUDOverlayEvent;

public class AppleSkinCompat {
    private static boolean isPresent = false;

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("appleskin")) {
            return;
        }

        try {
            isPresent = true;
            registerEvents();
            CGGConfigManager.LOGGER.info("AppleSkin integration successfully initialized!");
        } catch (Throwable t) {
            CGGConfigManager.LOGGER.warn("Failed to initialize AppleSkin integration", t);
        }
    }

    private static void registerEvents() {
        HUDOverlayEvent.Saturation.EVENT.register(event -> {
            CGGConfig config = CGGConfigManager.getConfig();
            if (config.enabled && config.appleSkinIntegration && config.appleSkinHideDefaultSaturation) {
                event.isCanceled = true;
            }
        });
    }

    public static boolean isAppleSkinLoaded() {
        return isPresent;
    }
}
