package rtx.heave;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.heave.api.mods.geckolib.GeckoLib;
import rtx.heave.manager.Manager;

public class Heave
implements ModInitializer,
ClientModInitializer {
    public static final String MOD_ID = "heave";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"heave");

    public void onInitializeClient() {
        sanitizeOptionsFile();
        rtx.heave.utils.input.HeaveKeyBindings.init();
        ru.customgamegui.config.CGGConfigManager.load();
        ru.customgamegui.compat.AppleSkinCompat.init();
        Manager.init();
    }

    private static void sanitizeOptionsFile() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            if (gameDir == null) return;
            File optionsFile = gameDir.resolve("options.txt").toFile();
            if (!optionsFile.exists()) return;

            String content = Files.readString(optionsFile.toPath(), StandardCharsets.UTF_8);
            boolean modified = false;

            if (content.contains("heave_custom_swords")) {
                content = content
                    .replace("\"file/heave_custom_swords\",", "")
                    .replace(",\"file/heave_custom_swords\"", "")
                    .replace("\"file/heave_custom_swords\"", "")
                    .replace("\"heave_custom_swords\",", "")
                    .replace(",\"heave_custom_swords\"", "")
                    .replace("\"heave_custom_swords\"", "");
                modified = true;
            }

            String cleaned = content
                .replaceAll(",\\s*\\]", "]")
                .replaceAll("\\[\\s*,", "[");
            if (!cleaned.equals(content)) {
                content = cleaned;
                modified = true;
            }

            if (content.contains("onboardAccessibility:true")) {
                content = content.replace("onboardAccessibility:true", "onboardAccessibility:false");
                modified = true;
            }

            if (modified) {
                Files.writeString(optionsFile.toPath(), content, StandardCharsets.UTF_8);
                LOGGER.info("[Heave] Repaired and sanitized options.txt");
            }
        } catch (Throwable t) {
            LOGGER.error("[Heave] Failed to sanitize options.txt", t);
        }
    }

    public void onInitialize() {
        new GeckoLib().onInitialize();
    }
}

