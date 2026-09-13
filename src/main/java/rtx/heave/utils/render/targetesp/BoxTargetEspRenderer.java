package rtx.heave.utils.render.targetesp;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import rtx.heave.utils.render.others.WorldVertex;

public class BoxTargetEspRenderer {
    private static final Identifier TARGET_TEXTURE = Identifier.of("heave", "textures/targetesp/target.png");

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        Quaternionf cameraRotation,
        float entityHeight,
        float size,
        float rotationAngleDeg,
        float bobAngleRad,
        float alpha,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || alpha <= 0.001f || cameraRotation == null) {
            return;
        }

        float bobOffset = (float)(0.06 * Math.sin(bobAngleRad));
        float y = entityHeight * 0.5f + bobOffset;
        float pulsate = size * (1.0f + 0.05f * (float)Math.sin(bobAngleRad));

        RenderLayer layer = RenderLayers.entityTranslucentEmissive(TARGET_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(layer);

        int color = colorProvider != null ? colorProvider.getColor(0, alpha) : 0xFFFFFFFF;

        matrixStack.push();
        matrixStack.translate(0.0f, y, 0.0f);
        matrixStack.multiply((Quaternionfc) cameraRotation);
        matrixStack.multiply((Quaternionfc) RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngleDeg));

        MatrixStack.Entry entry = matrixStack.peek();
        float hs = pulsate * 0.5f;
        WorldVertex.textured(vc, entry, -hs, -hs, 0.0f, 0.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, hs, -hs, 0.0f, 1.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, hs, hs, 0.0f, 1.0f, 1.0f, color);
        WorldVertex.textured(vc, entry, -hs, hs, 0.0f, 0.0f, 1.0f, color);
        matrixStack.pop();

        immediate.draw(layer);
    }
}