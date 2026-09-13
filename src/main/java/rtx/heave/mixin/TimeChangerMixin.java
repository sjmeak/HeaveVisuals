package rtx.heave.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.heave.api.modules.impl.Visuals.TimeChanger;

@Mixin(ClientWorld.Properties.class)
public class TimeChangerMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true, require = 0)
    private void heave_modifyClientTime(CallbackInfoReturnable<Long> cir) {
        TimeChanger tc = TimeChanger.getInstance();
        if (tc != null && tc.isEnabled()) {
            cir.setReturnValue(tc.modifyTime(cir.getReturnValue()));
        }
    }
}
