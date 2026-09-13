package rtx.heave.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Interface.ScoreboardModule;
import rtx.heave.api.modules.impl.Utils.StreamerMode;

@Mixin(InGameHud.class)
public abstract class ScoreboardMixin {
    @Unique
    private boolean heave_scoreboardScaled;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave_handleScoreboardHead(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        ScoreboardModule mod = ScoreboardModule.getInstance();
        this.heave_scoreboardScaled = false;
        if (mod != null && mod.isEnabled()) {
            if (mod.noScoreboard.getValue()) {
                ci.cancel();
                return;
            }
            float scale = mod.scale.getFloat();
            if (Math.abs(scale - 1.0f) > 0.01f) {
                float screenWidth = context.getScaledWindowWidth();
                float screenHeight = context.getScaledWindowHeight();
                float offsetX = screenWidth * (1.0f - scale);
                float offsetY = screenHeight * (1.0f - scale) * 0.5f;
                Matrix3x2fStack matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(offsetX, offsetY);
                matrices.scale(scale, scale);
                this.heave_scoreboardScaled = true;
            }
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"), require = 0)
    private void heave_handleScoreboardTail(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (this.heave_scoreboardScaled) {
            context.getMatrices().popMatrix();
            this.heave_scoreboardScaled = false;
        }
    }

    @WrapOperation(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"
        ),
        require = 0
    )
    private void heave_wrapScoreboardDrawText(DrawContext context, TextRenderer font, Text text, int x, int y, int color, boolean shadow, Operation<Void> original) {
        ScoreboardModule mod = ScoreboardModule.getInstance();
        if (mod != null && mod.isEnabled() && mod.noNumber.getValue()) {
            String raw = text.getString().replaceAll("(?i)\u00a7[0-9A-FK-OR]", "").trim();
            if (!raw.isEmpty() && raw.matches("-?\\d+")) {
                return;
            }
        }
        original.call(context, font, StreamerMode.applySelfRank(text), x, y, color, shadow);
    }
}
