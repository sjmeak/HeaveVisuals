package rtx.heave.mixin;

import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(InactivityFpsLimiter.class)
public abstract class OptimizationInactivityFpsMixin {
    @Inject(method = "update", at = @At("RETURN"), cancellable = true, require = 0)
    private void heave$dynamicFps(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Optimization.getDynamicFps(cir.getReturnValueI()));
    }
}
