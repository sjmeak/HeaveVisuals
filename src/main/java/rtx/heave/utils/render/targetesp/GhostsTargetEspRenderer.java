package rtx.heave.utils.render.targetesp;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import rtx.heave.utils.render.others.WorldVertex;

public class GhostsTargetEspRenderer {
    private static final Identifier GLOW_TEXTURE = Identifier.of("heave", "textures/targetesp/bloom.png");
    private static final int TRAIL_STEPS = 6;

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        Quaternionf cameraRotation,
        float entityWidth,
        float entityHeight,
        float particleSize,
        int count,
        float rotationAngleDeg,
        float bobAngleRad,
        float alpha,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || alpha <= 0.001f || cameraRotation == null) {
            return;
        }

        int safeCount = Math.max(1, count);
        float radius = Math.max(0.4f, entityWidth * 0.5f + 0.65f);
        float centerY = entityHeight * 0.5f;

        double inclinationRad = Math.toRadians(50.0);
        Vec3d normal = new Vec3d(Math.sin(inclinationRad), Math.cos(inclinationRad), 0.0).normalize();
        Vec3d up = new Vec3d(0.0, 1.0, 0.0);
        Vec3d tangent = normal.crossProduct(up).normalize();
        Vec3d bitangent = normal.crossProduct(tangent).normalize();

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(GLOW_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int i = 0; i < safeCount; i++) {
            float baseAngleDeg = rotationAngleDeg + (360.0f * i / safeCount);
            float rad = (float) Math.toRadians(baseAngleDeg);
            float bob = (float) Math.sin(bobAngleRad + i * 0.8f) * 0.12f;

            double cosA = Math.cos(rad);
            double sinA = Math.sin(rad);

            double px = (tangent.x * cosA + bitangent.x * sinA) * radius;
            double py = centerY + bob + (tangent.y * cosA + bitangent.y * sinA) * radius;
            double pz = (tangent.z * cosA + bitangent.z * sinA) * radius;

            int colorOffset = (int)(i * (360 / safeCount));
            float size = particleSize * (1.0f + (float) i / 15.0f);
            int color = colorProvider != null ? colorProvider.getColor(colorOffset, alpha) : 0xFFFFFFFF;

            renderBillboardQuad(matrixStack, vc, (float)px, (float)py, (float)pz, cameraRotation, size, color);

            float trailSpan = 30.0f;
            for (int t = 1; t <= TRAIL_STEPS; t++) {
                float trailProgress = (float) t / TRAIL_STEPS;
                float trailRad = (float) Math.toRadians(baseAngleDeg - trailProgress * trailSpan);
                float trailBob = (float) Math.sin(bobAngleRad + i * 0.8f - trailProgress * 0.4f) * 0.12f;

                double tCos = Math.cos(trailRad);
                double tSin = Math.sin(trailRad);

                double tx = (tangent.x * tCos + bitangent.x * tSin) * radius;
                double ty = centerY + trailBob + (tangent.y * tCos + bitangent.y * tSin) * radius;
                double tz = (tangent.z * tCos + bitangent.z * tSin) * radius;

                float trailSize = size * (1.0f - trailProgress * 0.5f);
                float trailAlpha = alpha * (1.0f - trailProgress) * 0.6f;
                int trailColor = colorProvider != null ? colorProvider.getColor(colorOffset + t * 8, trailAlpha) : 0x80FFFFFF;

                renderBillboardQuad(matrixStack, vc, (float)tx, (float)ty, (float)tz, cameraRotation, trailSize, trailColor);
            }
        }

        immediate.draw(glowLayer);
    }

    private static void renderBillboardQuad(
        MatrixStack matrixStack,
        VertexConsumer vc,
        float x, float y, float z,
        Quaternionf cameraRot,
        float size,
        int color
    ) {
        if (size <= 0.001f || (color >>> 24) == 0) return;
        matrixStack.push();
        matrixStack.translate(x, y, z);
        matrixStack.multiply((Quaternionfc) cameraRot);
        MatrixStack.Entry entry = matrixStack.peek();
        float hs = size * 0.5f;
        WorldVertex.textured(vc, entry, -hs, -hs, 0.0f, 0.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, hs, -hs, 0.0f, 1.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, hs, hs, 0.0f, 1.0f, 1.0f, color);
        WorldVertex.textured(vc, entry, -hs, hs, 0.0f, 0.0f, 1.0f, color);
        matrixStack.pop();
    }
}