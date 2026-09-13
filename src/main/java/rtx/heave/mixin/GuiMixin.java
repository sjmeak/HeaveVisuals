package rtx.heave.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.render.DrawEvent;
import rtx.heave.api.events.impl.render.HudRenderEvent;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.PotionsModule;
import rtx.heave.api.modules.impl.Visuals.BetterHud;
import rtx.heave.api.ui.BaseScreen;
import rtx.heave.api.ui.UI;
import rtx.heave.utils.render.others.LoadingVisualGuard;
import rtx.heave.utils.render.warmup.Render2DWarmup;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Unique
    private boolean heave_debugEarly;

    @Unique
    private boolean heave_hudRendered;

    @Shadow
    public abstract void renderDebugHud(DrawContext context);

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), require = 0)
    private void heave_renderHead(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        this.heave_hudRendered = false;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.BEFORE), require = 0)
    private void heave_renderHudUnderVanilla(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (LoadingVisualGuard.shouldSuppressHud(this.client)) {
            return;
        }
        this.heave_hudRendered = true;
        EventBus.get().post(new HudRenderEvent(graphics, deltaTracker));
        graphics.createNewRootLayer();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("RETURN"), require = 0)
    private void heave_renderTopLayer(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (LoadingVisualGuard.shouldSuppressHud(this.client)) {
            Render2DWarmup.runWarmupFrame(graphics);
            return;
        }
        if (!this.heave_hudRendered) {
            this.heave_hudRendered = true;
            EventBus.get().post(new HudRenderEvent(graphics, deltaTracker));
            graphics.createNewRootLayer();
        }
        EventBus.get().post(new DrawEvent(graphics, deltaTracker));
        UI.renderClosingPanelOverHud(graphics);
        BaseScreen.renderClosingOverlay(graphics);
        if (UI.isOpen() && this.client.getDebugHud() != null && this.client.getDebugHud().shouldShowDebugHud()) {
            this.heave_debugEarly = true;
            this.renderDebugHud(graphics);
            this.heave_debugEarly = false;
        }
    }

    @Inject(method = "renderDebugHud(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave_moveDebugUnderClickGui(DrawContext graphics, CallbackInfo ci) {
        if (!this.heave_debugEarly && UI.isOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave_hideVanillaEffects(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        PotionsModule potions = ModuleManager.get().get(PotionsModule.class);
        if (potions != null && potions.isEnabled()) {
            ci.cancel();
        }
    }

}
