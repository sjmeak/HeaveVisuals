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

public class CrystalsTargetEspRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.of("heave", "textures/targetesp/bloom.png");

    private static final Vector3f[] BASE_VERTICES = new Vector3f[]{
        new Vector3f(0.0f, 1.5f, 0.0f),
        new Vector3f(0.0f, -1.5f, 0.0f),
        new Vector3f(1.0f, 0.0f, 0.0f),
        new Vector3f(-1.0f, 0.0f, 0.0f),
        new Vector3f(0.0f, 0.0f, 1.0f),
        new Vector3f(0.0f, 0.0f, -1.0f)
    };

    private static final int[][] FACES = new int[][]{
        {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2},
        {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}
    };

    private static final float[] SHADING = new float[]{
        1.0f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.4f, 0.6f
    };

    public static void render(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrixStack,
        Quaternionf cameraRot,
        float entityWidth,
        float entityHeight,
        float rotDeg,
        float fade,
        int targetColor
    ) {
        if (immediate == null || matrixStack == null || fade <= 0.001f || cameraRot == null) {
            return;
        }

        float widthScale = entityWidth * 1.5f;

        // 1. Render 3D Octahedron Crystals
        VertexConsumer meshConsumer = immediate.getBuffer(RenderLayers.dragonRays());

        for (int angle = 0; angle < 360; angle += 20) {
            float fadeScale = 1.2f - 0.5f * fade;
            float rad = (float) Math.toRadians(angle + rotDeg * 0.3f);
            float dx = (float) (Math.sin(rad) * widthScale * fadeScale);
            float dz = (float) (Math.cos(rad) * widthScale * fadeScale);
            float dy = 0.1f + entityHeight * (float) Math.abs(Math.sin(Math.toRadians(angle)));

            // LookAt direction towards entity center (0, height * 0.5, 0)
            Vector3f dir = new Vector3f(-dx, entityHeight * 0.5f - 1.0f, -dz).normalize();
            Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
            Quaternionf quat = new Quaternionf().rotationTo(up, dir);

            matrixStack.push();
            matrixStack.translate(dx, dy, dz);
            matrixStack.multiply(quat);
            matrixStack.scale(0.1f, 0.1f, 0.1f);

            MatrixStack.Entry entry = matrixStack.peek();

            for (int i = 0; i < FACES.length; i++) {
                int[] face = FACES[i];
                float s = SHADING[i];
                int c = shadeColor(targetColor, s, fade);

                Vector3f v0 = BASE_VERTICES[face[0]];
                Vector3f v1 = BASE_VERTICES[face[1]];
                Vector3f v2 = BASE_VERTICES[face[2]];

                meshConsumer.vertex(entry, v0.x, v0.y, v0.z).color(c);
                meshConsumer.vertex(entry, v1.x, v1.y, v1.z).color(c);
                meshConsumer.vertex(entry, v2.x, v2.y, v2.z).color(c);
            }

            matrixStack.pop();
        }

        immediate.draw(RenderLayers.dragonRays());

        // 2. Render Camera-Facing Bloom Halos
        RenderLayer bloomLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer bloomConsumer = immediate.getBuffer(bloomLayer);
        MatrixStack.Entry entry = matrixStack.peek();

        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRot);
        Vector3f camUp = new Vector3f(0.0f, 1.0f, 0.0f).rotate(cameraRot);

        float haloSize = 1.0f;
        float hs = haloSize * 0.5f;
        float rx = right.x * hs, ry = right.y * hs, rz = right.z * hs;
        float ux = camUp.x * hs, uy = camUp.y * hs, uz = camUp.z * hs;

        int haloColor = withAlpha(targetColor, 0.2f * fade);

        for (int angle = 0; angle < 360; angle += 20) {
            float fadeScale = 1.2f - 0.5f * fade;
            float rad = (float) Math.toRadians(angle + rotDeg * 0.3f);
            float dx = (float) (Math.sin(rad) * widthScale * fadeScale);
            float dz = (float) (Math.cos(rad) * widthScale * fadeScale);
            float dy = 0.1f + entityHeight * (float) Math.abs(Math.sin(Math.toRadians(angle)));

            drawBillboard(bloomConsumer, entry, dx, dy, dz, rx, ry, rz, ux, uy, uz, haloColor);
        }

        immediate.draw(bloomLayer);
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

    private static int shadeColor(int baseRgb, float shading, float fade) {
        int a = Math.max(0, Math.min(255, (int) (ColorUtil.alpha(baseRgb) * fade)));
        int r = Math.max(0, Math.min(255, (int) (((baseRgb >> 16) & 0xFF) * shading)));
        int g = Math.max(0, Math.min(255, (int) (((baseRgb >> 8) & 0xFF) * shading)));
        int b = Math.max(0, Math.min(255, (int) ((baseRgb & 0xFF) * shading)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int baseRgb, float alphaMult) {
        int a = Math.max(0, Math.min(255, (int) (ColorUtil.alpha(baseRgb) * alphaMult)));
        return (baseRgb & 0x00FFFFFF) | (a << 24);
    }
}
