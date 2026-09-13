package ru.customgamegui.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.customgamegui.config.CGGConfig;
import ru.customgamegui.config.CGGConfigManager;

@Mixin(SubtitlesHud.class)
public abstract class SubtitlesHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cgg$hideSubtitles(DrawContext context, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideSubtitles) {
            ci.cancel();
        }
    }
}
