package rtx.heave.api.drags.components;

import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.drags.Draggable;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Utils.EventMarkers;
import rtx.heave.utils.animations.Easings;
import rtx.heave.utils.animations.SmoothAnimation;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.others.RectUtil;
import rtx.heave.utils.render.render2d.Render2D;

public final class GpsHudComp extends Draggable {
    private static final float HEIGHT = 20.0f;
    private static final float RADIUS = 10.0f;
    private final SmoothAnimation visibility = new SmoothAnimation();
    private float currentWidth = 140.0f;
    private boolean lastTargetVisible;

    public GpsHudComp() {
        super("gps_hud", 300.0f, 24.0f);
        this.visibility.set(0.0);
    }

    @Override
    public String displayName() {
        return "GPS Индикатор";
    }

    @Override
    public float width() {
        return this.currentWidth;
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    @Override
    public boolean isInteractive() {
        EventMarkers mod = ModuleManager.get().get(EventMarkers.class);
        return mod != null && mod.isEnabled();
    }

    @Override
    protected void render(DrawContext drawContext) {
        EventMarkers mod = ModuleManager.get().get(EventMarkers.class);
        boolean dragMode = DragSystem.get().isDragModeActive();
        if ((mod == null || !mod.isEnabled()) && !dragMode) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null && !dragMode) {
            return;
        }

        EventMarkers.Marker marker = EventMarkers.getActiveMarker();
        boolean hasMarker = marker != null;
        boolean show = hasMarker || dragMode;

        if (show != this.lastTargetVisible) {
            this.visibility.run(show ? 1.0 : 0.0, show ? 0.20 : 0.15, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = show;
        }
        this.visibility.update();
        float alpha = (float) this.visibility.get();
        if (alpha <= 0.01f) {
            return;
        }

        String name;
        String distStr;
        String arrow;
        int colorRgb;
        long leftMs = 0L;
        boolean hasTimer = false;

        if (hasMarker) {
            name = marker.name;
            colorRgb = marker.color.getRGB();
            leftMs = marker.expiresAt - System.currentTimeMillis();
            hasTimer = marker.hasTimer && leftMs > 0;

            Vec3d playerPos = mc.player != null ? mc.player.getEntityPos() : Vec3d.ZERO;
            double dist = playerPos.distanceTo(marker.pos);
            distStr = (dist >= 1000.0)
                ? String.format(Locale.ROOT, "%.1f км", dist / 1000.0)
                : String.format(Locale.ROOT, "%.0f м", dist);

            double dx = marker.pos.x - playerPos.x;
            double dz = marker.pos.z - playerPos.z;
            double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            double playerYaw = mc.player != null ? mc.player.getYaw() : 0.0;
            float diffYaw = MathHelper.wrapDegrees((float)(targetYaw - playerYaw));

            arrow = resolveArrow(diffYaw);
        } else {
            // Drag mode placeholder
            name = "Замок";
            distStr = "1.2 км";
            arrow = "▲";
            colorRgb = 0xFFFF4444;
        }

        String timerStr = "";
        if (hasTimer) {
            long totalSec = leftMs / 1000L;
            timerStr = String.format(Locale.ROOT, " (%02d:%02d)", totalSec / 60L, totalSec % 60L);
        }

        float x = this.getX();
        float y = this.getY();

        int nameW = mc.textRenderer.getWidth(name);
        int distW = mc.textRenderer.getWidth(distStr + timerStr);
        int arrowW = mc.textRenderer.getWidth(arrow);

        float targetWidth = 12.0f + 10.0f + 5.0f + nameW + 10.0f + distW + 10.0f + arrowW + 14.0f;
        if (targetWidth < 120.0f) targetWidth = 120.0f;

        this.currentWidth += (targetWidth - this.currentWidth) * 0.25f;
        if (Math.abs(this.currentWidth - targetWidth) < 0.5f) {
            this.currentWidth = targetWidth;
        }
        float w = this.currentWidth;

        // Render sleek glass capsule
        RectUtil.drawClientRect(x, y, w, HEIGHT, RADIUS, alpha);

        // Accent / Marker color outline
        int outlineCol = ColorUtil.multAlpha(colorRgb, 0.65f * alpha);
        Render2D.outline(drawContext, x, y, w, HEIGHT, RADIUS, 1.0f, outlineCol);

        // 1. Icon (Diamond) on left
        float iconX = x + 10.0f;
        float textY = y + 6.0f;
        drawContext.drawText(mc.textRenderer, "✦", (int) iconX, (int) textY, ColorUtil.multAlpha(colorRgb, alpha), false);

        // 2. Name
        float nameX = iconX + 13.0f;
        int nameColor = ColorUtil.multAlpha(0xFFFFFFFF, alpha);
        drawContext.drawText(mc.textRenderer, name, (int) nameX, (int) textY, nameColor, false);

        // 3. Separator dot
        float dotX = nameX + nameW + 4.0f;
        drawContext.drawText(mc.textRenderer, "•", (int) dotX, (int) textY, ColorUtil.multAlpha(0x88FFFFFF, alpha), false);

        // 4. Distance & Timer
        float distTextX = dotX + 8.0f;
        int distColor = ColorUtil.multAlpha(0xFF38BDF8, alpha);
        drawContext.drawText(mc.textRenderer, distStr + timerStr, (int) distTextX, (int) textY, distColor, false);

        // 5. Arrow pill badge on right
        float arrowBadgeW = (float) arrowW + 8.0f;
        float arrowBadgeH = 12.0f;
        float arrowBadgeX = x + w - arrowBadgeW - 8.0f;
        float arrowBadgeY = y + (HEIGHT - arrowBadgeH) / 2.0f;

        int arrowBadgeBg = ColorUtil.multAlpha(colorRgb, 0.22f * alpha);
        Render2D.rect(drawContext, arrowBadgeX, arrowBadgeY, arrowBadgeW, arrowBadgeH, 6.0f, arrowBadgeBg);

        int arrowCol = ColorUtil.multAlpha(colorRgb, alpha);
        drawContext.drawText(mc.textRenderer, arrow, (int)(arrowBadgeX + 4.0f), (int)(arrowBadgeY + 2.0f), arrowCol, false);
    }

    private static String resolveArrow(float diffYaw) {
        if (Math.abs(diffYaw) < 22.5f) return "▲";
        if (diffYaw >= 22.5f && diffYaw < 67.5f) return "↗";
        if (diffYaw >= 67.5f && diffYaw < 112.5f) return "→";
        if (diffYaw >= 112.5f && diffYaw < 157.5f) return "↘";
        if (diffYaw <= -22.5f && diffYaw > -67.5f) return "↖";
        if (diffYaw <= -67.5f && diffYaw > -112.5f) return "←";
        if (diffYaw <= -112.5f && diffYaw > -157.5f) return "↙";
        return "▼";
    }
}
