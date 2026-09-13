package rtx.heave.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.heave.api.modules.impl.Visuals.NameTags;

@Mixin(LabelCommandRenderer.class)
public class LabelCommandRendererMixin {
    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"
        ),
        require = 0
    )
    private void heave_wrapLabelDraw(TextRenderer textRenderer, Text text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType, int backgroundColor, int light, Operation<Void> original) {
        NameTags nt = NameTags.getInstance();
        if (nt != null && nt.isEnabled()) {
            boolean remBg = nt.removeBackground.getValue();
            boolean shadowOn = nt.shadow.getValue();
            int bg = remBg ? 0 : backgroundColor;
            int finalColor = color;
            if (remBg && layerType == TextRenderer.TextLayerType.SEE_THROUGH) {
                // If background is removed, ensure see-through text remains clearly legible through blocks
                finalColor = (color & 0x00FFFFFF) | 0xE0000000;
            }
            original.call(textRenderer, text, x, y, finalColor, shadowOn, matrix, vertexConsumers, layerType, bg, light);
            return;
        }
        original.call(textRenderer, text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light);
    }
}
