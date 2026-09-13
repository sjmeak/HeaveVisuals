package rtx.heave.utils.render.world;

import java.util.List;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class WorldShapeRenderer {
    private WorldShapeRenderer() {}

    public static void boxes(VertexConsumerProvider.Immediate consumers, MatrixStack matrixStack, Vec3d cameraPos, List<Box> boxes, int fillColor, int outlineColor, float lineWidth) {
        if (boxes == null || boxes.isEmpty() || consumers == null || matrixStack == null) {
            return;
        }
        MatrixStack.Entry entry = matrixStack.peek();

        if ((fillColor >>> 24) > 0) {
            VertexConsumer fillConsumer = consumers.getBuffer(RenderLayers.lightning());
            for (Box box : boxes) {
                float x1 = (float) (box.minX - cameraPos.x);
                float y1 = (float) (box.minY - cameraPos.y);
                float z1 = (float) (box.minZ - cameraPos.z);
                float x2 = (float) (box.maxX - cameraPos.x);
                float y2 = (float) (box.maxY - cameraPos.y);
                float z2 = (float) (box.maxZ - cameraPos.z);

                // Bottom
                fillConsumer.vertex(entry, x1, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y1, z2).color(fillColor);

                // Top
                fillConsumer.vertex(entry, x1, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z1).color(fillColor);

                // North
                fillConsumer.vertex(entry, x1, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z1).color(fillColor);

                // South
                fillConsumer.vertex(entry, x1, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z2).color(fillColor);

                // West
                fillConsumer.vertex(entry, x1, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x1, y1, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x1, y2, z1).color(fillColor);

                // East
                fillConsumer.vertex(entry, x2, y1, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z1).color(fillColor);
                fillConsumer.vertex(entry, x2, y2, z2).color(fillColor);
                fillConsumer.vertex(entry, x2, y1, z2).color(fillColor);
            }
            consumers.draw(RenderLayers.lightning());
        }

        if ((outlineColor >>> 24) > 0) {
            VertexConsumer lineConsumer = consumers.getBuffer(RenderLayers.lines());
            for (Box box : boxes) {
                float x1 = (float) (box.minX - cameraPos.x);
                float y1 = (float) (box.minY - cameraPos.y);
                float z1 = (float) (box.minZ - cameraPos.z);
                float x2 = (float) (box.maxX - cameraPos.x);
                float y2 = (float) (box.maxY - cameraPos.y);
                float z2 = (float) (box.maxZ - cameraPos.z);

                // Bottom 4 edges
                drawLine(lineConsumer, entry, x1, y1, z1, x2, y1, z1, 1.0f, 0.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x2, y1, z1, x2, y1, z2, 0.0f, 0.0f, 1.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x2, y1, z2, x1, y1, z2, -1.0f, 0.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x1, y1, z2, x1, y1, z1, 0.0f, 0.0f, -1.0f, outlineColor, lineWidth);

                // Top 4 edges
                drawLine(lineConsumer, entry, x1, y2, z1, x2, y2, z1, 1.0f, 0.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x2, y2, z1, x2, y2, z2, 0.0f, 0.0f, 1.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x2, y2, z2, x1, y2, z2, -1.0f, 0.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x1, y2, z2, x1, y2, z1, 0.0f, 0.0f, -1.0f, outlineColor, lineWidth);

                // Vertical 4 edges
                drawLine(lineConsumer, entry, x1, y1, z1, x1, y2, z1, 0.0f, 1.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x2, y1, z1, x2, y2, z1, 0.0f, 1.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x2, y1, z2, x2, y2, z2, 0.0f, 1.0f, 0.0f, outlineColor, lineWidth);
                drawLine(lineConsumer, entry, x1, y1, z2, x1, y2, z2, 0.0f, 1.0f, 0.0f, outlineColor, lineWidth);
            }
            consumers.draw(RenderLayers.lines());
        }
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, float nx, float ny, float nz, int color, float lineWidth) {
        consumer.vertex(entry, x1, y1, z1).color(color).normal(entry, nx, ny, nz).lineWidth(lineWidth);
        consumer.vertex(entry, x2, y2, z2).color(color).normal(entry, nx, ny, nz).lineWidth(lineWidth);
    }
}
