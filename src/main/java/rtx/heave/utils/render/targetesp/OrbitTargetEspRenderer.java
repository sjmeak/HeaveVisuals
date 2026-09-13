package rtx.heave.utils.render.targetesp;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import rtx.heave.utils.render.others.WorldVertex;

public class OrbitTargetEspRenderer {
    private static final Identifier GLOW_TEXTURE = Identifier.of("heave", "textures/targetesp/glow.png");
    private static final int TRAIL_STEPS = 8;

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        Quaternionf cameraRotation,
        String shape,
        float entityWidth,
        float entityHeight,
        float distance,
        int layers,
        float layerSpacing,
        int figuresPerCircle,
        float figureSize,
        boolean rotateFigures,
        float rotationAngleDeg,
        float bobAngleRad,
        boolean glow,
        float glowSize,
        float glowAlpha,
        float alpha,
        TargetEspColorProvider colorProvider
    ) {
        if (immediate == null || matrixStack == null || alpha <= 0.001f) {
            return;
        }

        int safeFigures = Math.max(1, figuresPerCircle);
        int safeLayers = Math.max(1, layers);
        float totalRadius = Math.max(0.3f, entityWidth * 0.5f + distance);
        float centerY = entityHeight * 0.5f;

        VertexConsumer consumer = immediate.getBuffer(RenderLayers.dragonRays());
        MatrixStack.Entry entry = matrixStack.peek();

        float centerLayer = (safeLayers - 1) * 0.5f;
        float totalHeightSpan = (safeLayers - 1) * layerSpacing;
        float startY = centerY - totalHeightSpan * 0.5f;

        for (int layer = 0; layer < safeLayers; layer++) {
            float baseY = (safeLayers <= 1) ? centerY : (startY + layer * layerSpacing);
            float layerDistFromCenter = Math.abs(layer - centerLayer) / Math.max(1.0f, centerLayer);
            float layerScale = 1.0f - layerDistFromCenter * 0.25f;

            float layerOffsetDeg = (layer % 2 == 1) ? (180.0f / safeFigures) : 0.0f;
            float layerSpin = (layer % 2 == 1) ? -rotationAngleDeg : rotationAngleDeg;

            for (int i = 0; i < safeFigures; i++) {
                float deg = layerSpin + layerOffsetDeg + (360.0f * i / safeFigures);
                float rad = (float) Math.toRadians(deg);
                float bob = (float) Math.sin(bobAngleRad + layer * 1.3f + i * 0.5f) * 0.06f;

                float gx = (float) (totalRadius * Math.cos(rad));
                float gz = (float) (totalRadius * Math.sin(rad));
                float gy = baseY + bob;

                float tx = (float) -Math.sin(rad);
                float ty = 0.0f;
                float tz = (float) Math.cos(rad);

                float rx = (float) Math.cos(rad);
                float ry = 0.0f;
                float rz = (float) Math.sin(rad);

                float bx = 0.0f;
                float by = 1.0f;
                float bz = 0.0f;

                int colorOffset = (int)(layer * 120 + i * (360 / safeFigures));
                int baseColor = colorProvider != null ? colorProvider.getColor(colorOffset, alpha * layerScale) : 0xFFFFFFFF;

                renderTrail(consumer, entry, totalRadius, baseY, bobAngleRad, layer, i, deg, figureSize * layerScale, bx, by, bz, alpha, colorProvider, colorOffset);
                renderShape(consumer, entry, shape, gx, gy, gz, rx, ry, rz, tx, ty, tz, bx, by, bz, figureSize * layerScale, baseColor, rotateFigures, (rotationAngleDeg * 2.0f + i * 45.0f));
            }
        }

        immediate.draw(RenderLayers.dragonRays());

        if (glow && cameraRotation != null && glowAlpha > 0.001f) {
            RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(GLOW_TEXTURE);
            VertexConsumer glowConsumer = immediate.getBuffer(glowLayer);

            for (int layer = 0; layer < safeLayers; layer++) {
                float baseY = (safeLayers <= 1) ? centerY : (startY + layer * layerSpacing);
                float layerDistFromCenter = Math.abs(layer - centerLayer) / Math.max(1.0f, centerLayer);
                float layerScale = 1.0f - layerDistFromCenter * 0.25f;

                float layerOffsetDeg = (layer % 2 == 1) ? (180.0f / safeFigures) : 0.0f;
                float layerSpin = (layer % 2 == 1) ? -rotationAngleDeg : rotationAngleDeg;

                for (int i = 0; i < safeFigures; i++) {
                    float deg = layerSpin + layerOffsetDeg + (360.0f * i / safeFigures);
                    float rad = (float) Math.toRadians(deg);
                    float bob = (float) Math.sin(bobAngleRad + layer * 1.3f + i * 0.5f) * 0.06f;

                    float gx = (float) (totalRadius * Math.cos(rad));
                    float gz = (float) (totalRadius * Math.sin(rad));
                    float gy = baseY + bob;

                    int colorOffset = (int)(layer * 120 + i * (360 / safeFigures));
                    int glowColor = colorProvider != null ? colorProvider.getColor(colorOffset, alpha * glowAlpha * layerScale) : 0x40FFFFFF;
                    float gSize = glowSize * (0.6f + layerScale * 0.4f);

                    renderBillboardQuad(matrixStack, glowConsumer, gx, gy, gz, cameraRotation, gSize, glowColor);
                }
            }
            immediate.draw(glowLayer);
        }
    }

    private static void renderShape(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        String shape,
        float gx, float gy, float gz,
        float rx, float ry, float rz,
        float tx, float ty, float tz,
        float bx, float by, float bz,
        float size,
        int color,
        boolean rotateFigures,
        float extraSpinDeg
    ) {
        if (shape == null) shape = "Стрелки";
        String s = shape.trim().toLowerCase();

        if (rotateFigures) {
            float spinRad = (float) Math.toRadians(extraSpinDeg);
            float cosS = (float) Math.cos(spinRad);
            float sinS = (float) Math.sin(spinRad);

            float newRx = rx * cosS - bx * sinS;
            float newRy = ry * cosS - by * sinS;
            float newRz = rz * cosS - bz * sinS;

            float newBx = rx * sinS + bx * cosS;
            float newBy = ry * sinS + by * cosS;
            float newBz = rz * sinS + bz * cosS;

            rx = newRx; ry = newRy; rz = newRz;
            bx = newBx; by = newBy; bz = newBz;
        }

        if (s.contains("стрелк") || s.contains("треуголь") || s.contains("arrow") || s.contains("triangle")) {
            renderTriangle(consumer, entry, gx, gy, gz, rx, ry, rz, tx, ty, tz, bx, by, bz, size, color);
        } else if (s.contains("куб") || s.contains("квадрат") || s.contains("cube") || s.contains("box")) {
            renderCube(consumer, entry, gx, gy, gz, rx, ry, rz, tx, ty, tz, bx, by, bz, size, color);
        } else {
            renderCrystal(consumer, entry, gx, gy, gz, rx, ry, rz, tx, ty, tz, bx, by, bz, size, color);
        }
    }

    private static void renderTriangle(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float gx, float gy, float gz,
        float rx, float ry, float rz,
        float tx, float ty, float tz,
        float bx, float by, float bz,
        float size,
        int color
    ) {
        float apexDist = size * 1.5f;
        float bSize = size * 0.5f;

        float apexX = gx + tx * apexDist;
        float apexY = gy + ty * apexDist;
        float apexZ = gz + tz * apexDist;

        float cUpX = gx + bx * bSize;
        float cUpY = gy + by * bSize;
        float cUpZ = gz + bz * bSize;

        float cDownX = gx - bx * bSize;
        float cDownY = gy - by * bSize;
        float cDownZ = gz - bz * bSize;

        float cRightX = gx + rx * bSize;
        float cRightY = gy + ry * bSize;
        float cRightZ = gz + rz * bSize;

        float cLeftX = gx - rx * bSize;
        float cLeftY = gy - ry * bSize;
        float cLeftZ = gz - rz * bSize;

        drawTriangle(consumer, entry, apexX, apexY, apexZ, cUpX, cUpY, cUpZ, cRightX, cRightY, cRightZ, color);
        drawTriangle(consumer, entry, apexX, apexY, apexZ, cRightX, cRightY, cRightZ, cDownX, cDownY, cDownZ, color);
        drawTriangle(consumer, entry, apexX, apexY, apexZ, cDownX, cDownY, cDownZ, cLeftX, cLeftY, cLeftZ, color);
        drawTriangle(consumer, entry, apexX, apexY, apexZ, cLeftX, cLeftY, cLeftZ, cUpX, cUpY, cUpZ, color);

        drawTriangle(consumer, entry, cUpX, cUpY, cUpZ, cDownX, cDownY, cDownZ, cRightX, cRightY, cRightZ, color);
        drawTriangle(consumer, entry, cUpX, cUpY, cUpZ, cLeftX, cLeftY, cLeftZ, cDownX, cDownY, cDownZ, color);
    }

    private static void renderCrystal(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float gx, float gy, float gz,
        float rx, float ry, float rz,
        float tx, float ty, float tz,
        float bx, float by, float bz,
        float size,
        int color
    ) {
        float apexDist = size * 1.5f;
        float eqDist = size * 0.9f;

        float topX = gx + bx * apexDist;
        float topY = gy + by * apexDist;
        float topZ = gz + bz * apexDist;

        float botX = gx - bx * apexDist;
        float botY = gy - by * apexDist;
        float botZ = gz - bz * apexDist;

        float c1x = gx + (tx + rx) * eqDist * 0.7071f;
        float c1y = gy + (ty + ry) * eqDist * 0.7071f;
        float c1z = gz + (tz + rz) * eqDist * 0.7071f;

        float c2x = gx + (tx - rx) * eqDist * 0.7071f;
        float c2y = gy + (ty - ry) * eqDist * 0.7071f;
        float c2z = gz + (tz - rz) * eqDist * 0.7071f;

        float c3x = gx - (tx + rx) * eqDist * 0.7071f;
        float c3y = gy - (ty + ry) * eqDist * 0.7071f;
        float c3z = gz - (tz + rz) * eqDist * 0.7071f;

        float c4x = gx - (tx - rx) * eqDist * 0.7071f;
        float c4y = gy - (ty - ry) * eqDist * 0.7071f;
        float c4z = gz - (tz - rz) * eqDist * 0.7071f;

        float[] shading = new float[]{1.0f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.4f, 0.6f};

        drawTriangle(consumer, entry, topX, topY, topZ, c1x, c1y, c1z, c2x, c2y, c2z, shadeColor(color, shading[0]));
        drawTriangle(consumer, entry, topX, topY, topZ, c2x, c2y, c2z, c3x, c3y, c3z, shadeColor(color, shading[1]));
        drawTriangle(consumer, entry, topX, topY, topZ, c3x, c3y, c3z, c4x, c4y, c4z, shadeColor(color, shading[2]));
        drawTriangle(consumer, entry, topX, topY, topZ, c4x, c4y, c4z, c1x, c1y, c1z, shadeColor(color, shading[3]));

        drawTriangle(consumer, entry, botX, botY, botZ, c2x, c2y, c2z, c1x, c1y, c1z, shadeColor(color, shading[4]));
        drawTriangle(consumer, entry, botX, botY, botZ, c3x, c3y, c3z, c2x, c2y, c2z, shadeColor(color, shading[5]));
        drawTriangle(consumer, entry, botX, botY, botZ, c4x, c4y, c4z, c3x, c3y, c3z, shadeColor(color, shading[6]));
        drawTriangle(consumer, entry, botX, botY, botZ, c1x, c1y, c1z, c4x, c4y, c4z, shadeColor(color, shading[7]));
    }

    private static int shadeColor(int baseRgb, float shading) {
        int a = (baseRgb >>> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, (int) (((baseRgb >> 16) & 0xFF) * shading)));
        int g = Math.max(0, Math.min(255, (int) (((baseRgb >> 8) & 0xFF) * shading)));
        int b = Math.max(0, Math.min(255, (int) ((baseRgb & 0xFF) * shading)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void renderCube(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float gx, float gy, float gz,
        float rx, float ry, float rz,
        float tx, float ty, float tz,
        float bx, float by, float bz,
        float size,
        int color
    ) {
        float hs = size * 0.45f;

        float p000x = gx - tx * hs - bx * hs - rx * hs;
        float p000y = gy - ty * hs - by * hs - ry * hs;
        float p000z = gz - tz * hs - bz * hs - rz * hs;

        float p001x = gx - tx * hs - bx * hs + rx * hs;
        float p001y = gy - ty * hs - by * hs + ry * hs;
        float p001z = gz - tz * hs - bz * hs + rz * hs;

        float p010x = gx - tx * hs + bx * hs - rx * hs;
        float p010y = gy - ty * hs + by * hs - ry * hs;
        float p010z = gz - tz * hs + bz * hs - rz * hs;

        float p011x = gx - tx * hs + bx * hs + rx * hs;
        float p011y = gy - ty * hs + by * hs + ry * hs;
        float p011z = gz - tz * hs + bz * hs + rz * hs;

        float p100x = gx + tx * hs - bx * hs - rx * hs;
        float p100y = gy + ty * hs - by * hs - ry * hs;
        float p100z = gz + tz * hs - bz * hs - rz * hs;

        float p101x = gx + tx * hs - bx * hs + rx * hs;
        float p101y = gy + ty * hs - by * hs + ry * hs;
        float p101z = gz + tz * hs - bz * hs + rz * hs;

        float p110x = gx + tx * hs + bx * hs - rx * hs;
        float p110y = gy + ty * hs - by * hs - ry * hs;
        float p110z = gz + tz * hs - bz * hs - rz * hs;

        float p111x = gx + tx * hs + bx * hs + rx * hs;
        float p111y = gy + ty * hs - by * hs + ry * hs;
        float p111z = gz + tz * hs - bz * hs + rz * hs;

        drawQuad(consumer, entry, p010x, p010y, p010z, p011x, p011y, p011z, p111x, p111y, p111z, p110x, p110y, p110z, color);
        drawQuad(consumer, entry, p000x, p000y, p000z, p100x, p100y, p100z, p101x, p101y, p101z, p001x, p001y, p001z, color);
        drawQuad(consumer, entry, p100x, p100y, p100z, p110x, p110y, p110z, p111x, p111y, p111z, p101x, p101y, p101z, color);
        drawQuad(consumer, entry, p000x, p000y, p000z, p001x, p001y, p001z, p011x, p011y, p011z, p010x, p010y, p010z, color);
        drawQuad(consumer, entry, p001x, p001y, p001z, p101x, p101y, p101z, p111x, p111y, p111z, p011x, p011y, p011z, color);
        drawQuad(consumer, entry, p000x, p000y, p000z, p010x, p010y, p010z, p110x, p110y, p110z, p100x, p100y, p100z, color);
    }

    private static void renderTrail(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float totalRadius,
        float baseY,
        float bobRad,
        int layer,
        int ghostIndex,
        float currentDeg,
        float size,
        float bx, float by, float bz,
        float alpha,
        TargetEspColorProvider colorProvider,
        int colorOffset
    ) {
        float trailSpanDeg = 36.0f;

        for (int t = 0; t < TRAIL_STEPS; t++) {
            float f0 = (float) t / TRAIL_STEPS;
            float f1 = (float) (t + 1) / TRAIL_STEPS;

            float deg0 = currentDeg - f0 * trailSpanDeg;
            float deg1 = currentDeg - f1 * trailSpanDeg;

            float rad0 = (float) Math.toRadians(deg0);
            float rad1 = (float) Math.toRadians(deg1);

            float x0 = (float) (totalRadius * Math.cos(rad0));
            float z0 = (float) (totalRadius * Math.sin(rad0));
            float y0 = baseY + (float) Math.sin(bobRad + layer * 1.3f + ghostIndex * 0.5f - f0 * 0.5f) * 0.06f;

            float x1 = (float) (totalRadius * Math.cos(rad1));
            float z1 = (float) (totalRadius * Math.sin(rad1));
            float y1 = baseY + (float) Math.sin(bobRad + layer * 1.3f + ghostIndex * 0.5f - f1 * 0.5f) * 0.06f;

            float w0 = (1.0f - f0) * size * 0.35f;
            float w1 = (1.0f - f1) * size * 0.35f;

            float a0 = (1.0f - f0) * alpha * 0.5f;
            float a1 = (1.0f - f1) * alpha * 0.5f;

            int c0 = colorProvider != null ? colorProvider.getColor(colorOffset + t * 8, a0) : 0xFFFFFFFF;
            int c1 = colorProvider != null ? colorProvider.getColor(colorOffset + (t + 1) * 8, a1) : 0xFFFFFFFF;

            drawQuadGradient(
                consumer, entry,
                x0 - bx * w0, y0 - by * w0, z0 - bz * w0, c0,
                x0 + bx * w0, y0 + by * w0, z0 + bz * w0, c0,
                x1 + bx * w1, y1 + by * w1, z1 + bz * w1, c1,
                x1 - bx * w1, y1 - by * w1, z1 - bz * w1, c1
            );
        }
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

    private static void drawTriangle(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int color
    ) {
        consumer.vertex(entry, x1, y1, z1).color(color);
        consumer.vertex(entry, x2, y2, z2).color(color);
        consumer.vertex(entry, x3, y3, z3).color(color);
    }

    private static void drawQuad(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float x4, float y4, float z4,
        int color
    ) {
        drawTriangle(consumer, entry, x1, y1, z1, x2, y2, z2, x3, y3, z3, color);
        drawTriangle(consumer, entry, x1, y1, z1, x3, y3, z3, x4, y4, z4, color);
    }

    private static void drawQuadGradient(
        VertexConsumer consumer,
        MatrixStack.Entry entry,
        float x1, float y1, float z1, int c1,
        float x2, float y2, float z2, int c2,
        float x3, float y3, float z3, int c3,
        float x4, float y4, float z4, int c4
    ) {
        consumer.vertex(entry, x1, y1, z1).color(c1);
        consumer.vertex(entry, x2, y2, z2).color(c2);
        consumer.vertex(entry, x3, y3, z3).color(c3);

        consumer.vertex(entry, x1, y1, z1).color(c1);
        consumer.vertex(entry, x3, y3, z3).color(c3);
        consumer.vertex(entry, x4, y4, z4).color(c4);
    }
}