package rtx.heave.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.drags.components.CooldownsComp;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.CooldownsModule;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

@Mixin(InGameHud.class)
public abstract class HotbarCooldownMixin {
    @Inject(method = "renderHotbarItem", at = @At("TAIL"), require = 0)
    private void heave_renderHotbarCooldownText(DrawContext context, int x, int y, RenderTickCounter deltaTracker, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (context == null || stack == null || stack.isEmpty()) return;
        CooldownsModule mod = ModuleManager.get().get(CooldownsModule.class);
        if (mod == null || !mod.isEnabled()) return;
        if (!mod.displayMode.is("Хотбар") && !mod.displayMode.is("Оба")) return;

        long remainingMs = CooldownsComp.getCooldownRemainingMs(stack);
        if (remainingMs > 0) {
            int sec = (int) Math.ceil(remainingMs / 1000.0f);
            String text = String.valueOf(sec);
            context.getMatrices().pushMatrix();
            Render2D.beginFrame(context);
            float tw = Fonts.SF.width(text, 6.5f);
            float tx = x + 16.0f - tw - 1.0f;
            float ty = y + 1.0f;
            int color = mod.cooldownColor.getColor();
            Fonts.SF.draw(text, tx, ty, 6.5f, color);
            Render2D.flush();
            context.getMatrices().popMatrix();
        }
    }
}
