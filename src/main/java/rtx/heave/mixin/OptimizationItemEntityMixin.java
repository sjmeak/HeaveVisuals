package rtx.heave.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(ItemEntity.class)
public abstract class OptimizationItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave$skipExcessItemTick(CallbackInfo ci) {
        ItemEntity item = (ItemEntity)(Object)this;
        if (item.getEntityWorld().isClient() && Optimization.shouldSkipItemTick(item)) {
            ci.cancel();
        }
    }
}
