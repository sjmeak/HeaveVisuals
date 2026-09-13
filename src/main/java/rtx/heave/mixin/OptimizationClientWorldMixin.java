package rtx.heave.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(ClientWorld.class)
public abstract class OptimizationClientWorldMixin {

    @ModifyVariable(method = "runQueuedChunkUpdates", at = @At("STORE"), ordinal = 1, require = 0)
    private int heave$clampChunkUpdatesPerFrame(int updates) {
        return Optimization.clampChunkUpdates(updates);
    }
}
