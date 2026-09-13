package rtx.heave.mixin;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;
import rtx.heave.utils.optimization.OcclusionCuller;

@Mixin(net.minecraft.client.render.WorldRenderer.class)

public abstract class OptimizationLevelRendererMixin {
    @Inject(method="fillEntityRenderStates", at={@At(value="HEAD")}, require = 0)
    private void heave_beginCullFrame(CallbackInfo ci) {
        OcclusionCuller.beginFrame();
    }

    @ModifyArg(method="fillEntityRenderStates", at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;method_5840(D)V"), index=0, require = 0)
    private double heave_scaleEntityDistance(double original) {
        return Optimization.scaleEntityViewScale(original);
    }

    @Inject(method="renderClouds", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void heave_skipClouds(CallbackInfo ci) {
        if (Optimization.cullClouds()) {
            ci.cancel();
        }
    }
}

