package rtx.heave.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(LightmapTextureManager.class)
public abstract class OptimizationLightmapMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave$throttleLightmap(CallbackInfo ci) {
        if (Optimization.shouldThrottleLightmap()) {
            ci.cancel();
        }
    }
}
