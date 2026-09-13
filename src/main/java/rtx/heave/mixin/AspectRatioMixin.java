package rtx.heave.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.heave.api.modules.impl.Visuals.AspectRatio;

@Mixin(value = GameRenderer.class, priority = 1100)
public abstract class AspectRatioMixin {

    @ModifyReturnValue(method = "getBasicProjectionMatrix", at = @At("RETURN"), require = 0)
    private Matrix4f heave_aspectRatioProjection(Matrix4f original) {
        if (original == null) return null;
        AspectRatio ar = AspectRatio.getInstance();
        if (ar != null && ar.isEnabled()) {
            Matrix4f copy = new Matrix4f(original);
            AspectRatio.apply(copy);
            return copy;
        }
        return original;
    }

    @WrapOperation(
        method = "renderWorld",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/ProjectionMatrix3;set(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
        require = 0
    )
    private GpuBufferSlice heave_handAspectRatio(ProjectionMatrix3 buffer, int width, int height, float fov, Operation<GpuBufferSlice> original) {
        AspectRatio ar = AspectRatio.getInstance();
        if (ar != null && ar.isEnabled()) {
            int adjustedWidth = Math.max(1, Math.round(height * ar.getRatio()));
            return (GpuBufferSlice) original.call(new Object[]{buffer, adjustedWidth, height, Float.valueOf(fov)});
        }
        return (GpuBufferSlice) original.call(new Object[]{buffer, width, height, Float.valueOf(fov)});
    }
}
