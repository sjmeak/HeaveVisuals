package rtx.heave.api.drags;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import rtx.heave.utils.animations.Easings;
import rtx.heave.utils.animations.SmoothAnimation;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public final class HudAlignment {
    public static final HudAlignment INSTANCE = new HudAlignment();
    private static final float SNAP_THRESHOLD = 5.0f;

    private Float guideX = null;
    private Float guideY = null;
    private final SmoothAnimation guideAlpha = new SmoothAnimation();
    private Draggable activeDragging = null;

    private HudAlignment() {
        this.guideAlpha.set(0.0);
    }

    public static boolean isAltDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    public SnapResult snap(Draggable current, float targetX, float targetY) {
        this.activeDragging = current;
        if (isAltDown()) {
            this.clearGuides();
            return new SnapResult(targetX, targetY);
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return new SnapResult(targetX, targetY);
        }

        float sw = Position.screenWidth();
        float sh = Position.screenHeight();
        List<Draggable> all = DragSystem.get().getAll();

        AxisResult resX = snapAxis(targetX, current.width(), sw, current, all, true);
        AxisResult resY = snapAxis(targetY, current.height(), sh, current, all, false);

        this.guideX = resX.guide;
        this.guideY = resY.guide;

        return new SnapResult(resX.pos, resY.pos);
    }

    private AxisResult snapAxis(float pos, float size, float screenSize, Draggable current, List<Draggable> all, boolean isX) {
        float bestDist = SNAP_THRESHOLD;
        float bestDelta = 0.0f;
        Float bestGuide = null;

        // 1. Screen center
        float screenCenter = screenSize / 2.0f;
        float[] tests = testAnchor(pos, size, screenCenter);
        if (tests[0] < bestDist) {
            bestDist = tests[0];
            bestDelta = tests[1];
            bestGuide = screenCenter;
        }

        // 2. Other draggables
        for (Draggable other : all) {
            if (other == current || !other.isInteractive() || other.width() <= 0.0f || other.height() <= 0.0f) {
                continue;
            }
            float otherPos = isX ? other.getX() : other.getY();
            float otherSize = isX ? other.width() : other.height();

            // Check min, center, max anchors of the other draggable
            float[] anchors = new float[] { otherPos, otherPos + otherSize / 2.0f, otherPos + otherSize };
            for (float anchor : anchors) {
                float[] res = testAnchor(pos, size, anchor);
                if (res[0] < bestDist) {
                    bestDist = res[0];
                    bestDelta = res[1];
                    bestGuide = anchor;
                }
            }
        }

        return new AxisResult(pos + bestDelta, bestGuide);
    }

    private static float[] testAnchor(float pos, float size, float anchor) {
        float bestDist = Float.MAX_VALUE;
        float bestDelta = 0.0f;

        // Test current.min, current.center, current.max against anchor
        float[] currentPoints = new float[] { pos, pos + size / 2.0f, pos + size };
        for (float pt : currentPoints) {
            float dist = Math.abs(anchor - pt);
            if (dist < bestDist) {
                bestDist = dist;
                bestDelta = anchor - pt;
            }
        }
        return new float[] { bestDist, bestDelta };
    }

    public void clearGuides() {
        this.guideX = null;
        this.guideY = null;
    }

    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        boolean active = (this.activeDragging != null && this.activeDragging.getDrag().isDragging());
        if (!active) {
            this.clearGuides();
            this.activeDragging = null;
        }

        this.guideAlpha.run(active ? 1.0 : 0.0, active ? 0.12 : 0.18, Easings.CUBIC_OUT, false);
        this.guideAlpha.update();
        float alpha = this.guideAlpha.get();

        if (alpha <= 0.005f) {
            return;
        }

        float sw = Position.screenWidth();
        float sh = Position.screenHeight();
        boolean alt = isAltDown();

        Render2D.beginFrame(context);

        // Guide lines (when not holding Alt)
        if (!alt) {
            if (this.guideX != null) {
                float gx = this.guideX;
                int col = ColorUtil.multAlpha(-1, 0.55f * alpha);
                Render2D.rect(gx - 0.5f, 0.0f, 1.0f, sh, 0.0f, col);
            }
            if (this.guideY != null) {
                float gy = this.guideY;
                int col = ColorUtil.multAlpha(-1, 0.55f * alpha);
                Render2D.rect(0.0f, gy - 0.5f, sw, 1.0f, 0.0f, col);
            }
        }

        // Hint bar at the bottom
        String hintText = alt ? "Свободное перемещение" : "Alt: Свободное перемещение";
        float fontSize = 7.5f;
        float textW = Fonts.SF_MEDIUM.width(hintText, fontSize);
        float padX = 8.0f;
        float padY = 4.5f;
        float pillW = textW + padX * 2.0f;
        float pillH = fontSize + padY * 2.0f;
        float pillX = (sw - pillW) / 2.0f;
        float pillY = sh - 70.0f;

        int pillBg = ColorUtil.multAlpha(0x101018, 0.85f * alpha);
        int borderCol = ColorUtil.multAlpha(-1, 0.12f * alpha);
        int textCol = ColorUtil.multAlpha(-1, 0.90f * alpha);

        Render2D.rect(pillX, pillY, pillW, pillH, pillH / 2.0f, pillBg);
        Render2D.outline(pillX, pillY, pillW, pillH, pillH / 2.0f, 0.5f, borderCol);
        Fonts.SF_MEDIUM.draw(hintText, pillX + padX, pillY + padY, fontSize, textCol);

        Render2D.flush();
    }

    public static record SnapResult(float x, float y) {
        public float getX() { return x; }
        public float getY() { return y; }
    }

    private static record AxisResult(float pos, Float guide) {}
}
