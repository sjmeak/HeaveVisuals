package ru.customgamegui.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.customgamegui.config.CGGConfig;
import ru.customgamegui.config.CGGConfigManager;
import ru.customgamegui.hud.SaturationBarRenderer;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow
    protected abstract PlayerEntity getCameraPlayer();

    // --- Hotbar & Experience ---

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void cgg$hideHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideHotbar) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void cgg$customHotbarTextureAlpha(DrawContext context, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height, Operation<Void> original) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (!config.enabled) {
            original.call(context, pipeline, texture, x, y, width, height);
            return;
        }

        float alpha = 1.0f;
        String path = texture.getPath();

        if (path.contains("hotbar_selection")) {
            if (config.hotbarSelectionOpacity <= 0) {
                return;
            }
            alpha = config.hotbarSelectionOpacity / 100.0f;
        } else if (path.contains("hotbar")) {
            if (config.hotbarBackgroundOpacity <= 0) {
                return;
            }
            alpha = config.hotbarBackgroundOpacity / 100.0f;
        }

        if (alpha < 1.0f) {
            context.drawGuiTexture(pipeline, texture, x, y, width, height, alpha);
        } else {
            original.call(context, pipeline, texture, x, y, width, height);
        }
    }

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/bar/Bar;renderBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void cgg$hideExperienceBar(Bar instance, DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideExperienceBar && instance instanceof ExperienceBar) {
            return;
        }
        original.call(instance, context, tickCounter);
    }

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"))
    private void cgg$handleExperienceLevel(DrawContext context, TextRenderer textRenderer, int level, Operation<Void> original) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideExperienceLevel) {
            return;
        }

        if (config.enabled && config.hideExperienceBar && config.shiftExpLevelWhenBarHidden) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, (float) config.statusBarOffset);
            original.call(context, textRenderer, level);
            context.getMatrices().popMatrix();
        } else {
            original.call(context, textRenderer, level);
        }
    }

    // --- Status Bars (Hearts, Hunger, Armor, Air, Saturation) & Clear GUI Shift ---

    @WrapMethod(method = "renderStatusBars")
    private void cgg$shiftStatusBars(DrawContext context, Operation<Void> original) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideExperienceBar && config.shiftDownWithoutExpBar) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, (float) config.statusBarOffset);
            original.call(context);
            context.getMatrices().popMatrix();
        } else {
            original.call(context);
        }
    }

    @Inject(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderFood(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;II)V", shift = At.Shift.AFTER))
    private void cgg$renderSaturationBar(DrawContext context, CallbackInfo ci) {
        SaturationBarRenderer.render(context, this.getCameraPlayer());
    }

    @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
    private void cgg$hideHealthBar(DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideHealthBar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void cgg$hideFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideHungerBar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void cgg$hideArmor(DrawContext context, PlayerEntity player, int i, int j, int k, int x, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideArmorBar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderAirBubbles", at = @At("HEAD"), cancellable = true)
    private void cgg$hideAir(DrawContext context, PlayerEntity player, int heartCount, int top, int left, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideAirBar) {
            ci.cancel();
        }
    }

    // --- Overlays & Tooltips ---

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void cgg$hideCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideCrosshair) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
    private void cgg$hideScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideScoreboard) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void cgg$hideStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideStatusEffects) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void cgg$hideHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideHeldItemName) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void cgg$hideVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideVignette) {
            ci.cancel();
        }
    }

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true)
    private void cgg$hideChat(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CGGConfig config = CGGConfigManager.getConfig();
        if (config.enabled && config.hideChat) {
            ci.cancel();
        }
    }
}
