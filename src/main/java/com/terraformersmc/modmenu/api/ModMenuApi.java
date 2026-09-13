package com.terraformersmc.modmenu.api;

import net.minecraft.text.Text;
import java.util.Map;

public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> null;
    }

    default Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return Map.of();
    }

    static Text createModsButtonText() {
        return Text.translatable("modmenu.title");
    }
}
