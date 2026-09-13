package rtx.heave.mixin;

import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class OptimizationSignTextMixin {
    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave$skipFarSignText(SignBlockEntityRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, boolean front, CallbackInfo ci) {
        if (renderState != null && Optimization.cullSignText(renderState.pos)) {
            ci.cancel();
        }
    }
}
