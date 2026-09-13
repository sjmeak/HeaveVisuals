package rtx.heave.utils.render.targetesp;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.others.WorldVertex;

public class JelloTargetEspRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.of("heave", "textures/targetesp/bloom.png");
    private static final Identifier GLOWING_TEXTURE = Identifier.of("heave", "textures/targetesp/glowing.png");

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        Quaternionf cameraRot,
        float entityWidth,
        float entityHeight,
        float rotAnim,
        float fade,
        int targetColor
    ) {
        if (immediate == null || matrixStack == null || fade <= 0.001f || cameraRot == null) {
            return;
        }

        float radius = entityWidth * 1.45f * Math.max(0.5f, 0.7f - 0.2f * fade + 0.2f - 0.2f * fade);
        MatrixStack.Entry entry = matrixStack.peek();

        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRot);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(cameraRot);

        float quadSize = 0.2f;
        float hs = quadSize * 0.5f;
        float rx = right.x * hs, ry = right.y * hs, rz = right.z * hs;
        float ux = up.x * hs, uy = up.y * hs, uz = up.z * hs;

        // Pass 1: 15 layers of bloom quads (waving ribbon cylinder)
        RenderLayer bloomLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer bloomConsumer = immediate.getBuffer(bloomLayer);

        for (int angle = 0; angle < 360; angle += 2) {
            double rad = (angle + rotAnim) * (Math.PI / 180.0);
            float dx = (float) (Math.sin(rad) * radius);
            float dz = (float) (Math.cos(rad) * radius);

            for (int layer = 0; layer < 15; layer++) {
                float dy = entityHeight / 1.75f + (entityHeight / 2.0f) * (float) Math.sin(Math.toRadians(rotAnim / 1.5f + layer * 2.0f));
                float alpha = fade * (layer / 15.0f) * 0.05f;
                int col = withAlpha(targetColor, alpha);
                if ((col >>> 24) == 0) continue;

                drawBillboard(bloomConsumer, entry, dx, dy, dz, rx, ry, rz, ux, uy, uz, col);
            }
        }
        immediate.draw(bloomLayer);

        // Pass 2: Leading ring of glowing quads
        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(GLOWING_TEXTURE);
        VertexConsumer glowConsumer = immediate.getBuffer(glowLayer);

        for (int angle = 0; angle < 360; angle += 2) {
            double rad = (angle + rotAnim) * (Math.PI / 180.0);
            float dx = (float) (Math.sin(rad) * radius);
            float dz = (float) (Math.cos(rad) * radius);
            float dy = entityHeight / 1.75f + (entityHeight / 2.0f) * (float) Math.sin(Math.toRadians(rotAnim / 1.5f + 30.0f));
            float alpha = fade * 0.2f;
            int col = withAlpha(targetColor, alpha);
            if ((col >>> 24) == 0) continue;

            drawBillboard(glowConsumer, entry, dx, dy, dz, rx, ry, rz, ux, uy, uz, col);
        }
        immediate.draw(glowLayer);
    }

    private static void drawBillboard(
        VertexConsumer vc,
        MatrixStack.Entry entry,
        float x, float y, float z,
        float rx, float ry, float rz,
        float ux, float uy, float uz,
        int color
    ) {
        WorldVertex.textured(vc, entry, x - rx - ux, y - ry - uy, z - rz - uz, 0.0f, 1.0f, color);
        WorldVertex.textured(vc, entry, x + rx - ux, y + ry - uy, z + rz - uz, 1.0f, 1.0f, color);
        WorldVertex.textured(vc, entry, x + rx + ux, y + ry + uy, z + rz + uz, 1.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, x - rx + ux, y - ry + uy, z - rz + uz, 0.0f, 0.0f, color);
    }

    private static int withAlpha(int baseRgb, float alphaMult) {
        int a = Math.max(0, Math.min(255, (int) (ColorUtil.alpha(baseRgb) * alphaMult)));
        return (baseRgb & 0x00FFFFFF) | (a << 24);
    }
}
