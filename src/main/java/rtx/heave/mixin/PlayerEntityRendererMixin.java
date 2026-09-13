package rtx.heave.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.heave.api.modules.impl.Visuals.NameTags;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @WrapOperation(
        method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V"
        ),
        require = 0
    )
    private void heave_wrapSubmitLabel(OrderedRenderCommandQueue queue, MatrixStack matrixStack, Vec3d pos, int yOffset, Text text, boolean notSneaking, int light, double distance, CameraRenderState camera, Operation<Void> original, @Local(argsOnly = true) PlayerEntityRenderState state) {
        boolean finalNotSneaking = notSneaking;
        NameTags nt = NameTags.getInstance();
        if (nt != null && nt.isEnabled()) {
            boolean isInvis = state.invisible || state.invisibleToPlayer;
            if (isInvis && !nt.throughWalls.getValue()) {
                finalNotSneaking = false;
            }
        }
        original.call(queue, matrixStack, pos, yOffset, text, finalNotSneaking, light, distance, camera);
    }
}
