package rtx.heave.mixin;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(net.minecraft.client.render.entity.EntityRenderer.class)

public abstract class OptimizationEntityShadowMixin {
    @Inject(method="updateShadow", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void heave_skipShadow(CallbackInfo ci) {
        if (Optimization.hideEntityShadows()) {
            ci.cancel();
        }
    }
}

