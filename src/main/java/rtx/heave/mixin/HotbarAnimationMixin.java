package rtx.heave.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.heave.api.modules.impl.Visuals.BetterHud;

@Mixin(net.minecraft.client.gui.hud.InGameHud.class)

public abstract class HotbarAnimationMixin {
    @ModifyArg(method="renderHotbar", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;method_52706(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/class_2960;IIII)V", ordinal=1), index=2, require = 0)
    private int heave_animateHotbarSelection(int targetX) {
        return BetterHud.animateHotbarSelectionX(targetX);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @WrapMethod(method="renderMainHud")
    private void heave_liftHotbarWithChat(DrawContext guiGraphics, RenderTickCounter deltaTracker, Operation<Void> original) {
        float offset = BetterHud.chatHotbarLiftOffset();
        if (offset <= 0.01f) {
            original.call(new Object[]{guiGraphics, deltaTracker});
            return;
        }
        guiGraphics.getMatrices().pushMatrix();
        guiGraphics.getMatrices().translate(0.0f, -offset);
        try {
            original.call(new Object[]{guiGraphics, deltaTracker});
        }
        finally {
            guiGraphics.getMatrices().popMatrix();
        }
    }
}

