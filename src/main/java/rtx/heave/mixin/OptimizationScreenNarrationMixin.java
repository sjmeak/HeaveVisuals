package rtx.heave.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(Screen.class)
public abstract class OptimizationScreenNarrationMixin {
    @Inject(method = "updateNarrator", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave$disableUpdateNarrator(CallbackInfo ci) {
        if (Optimization.shouldDisableNarration()) {
            ci.cancel();
        }
    }

    @Inject(method = "narrateScreenIfNarrationEnabled", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave$disableInitialNarration(boolean force, CallbackInfo ci) {
        if (Optimization.shouldDisableNarration()) {
            ci.cancel();
        }
    }
}
