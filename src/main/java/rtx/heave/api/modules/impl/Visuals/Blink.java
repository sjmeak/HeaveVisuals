package rtx.heave.api.modules.impl.Visuals;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;
import rtx.heave.api.ui.theme.ClientAccent;

public final class Blink extends Module {
    private final BooleanSetting useClientColor = this.register(
        new BooleanSetting("Цвет клиента", "Использовать основной цвет клиента.", true)
    );
    private final ColorSetting color = this.register(
        new ColorSetting("Цвет", "Цвет силуэтов Blink.", new Color(0x6B8AFD))
    );
    private final BooleanSetting fill = this.register(
        new BooleanSetting("Заливка", "Заливать грани полупрозрачным цветом.", true)
    );
    private final BooleanSetting dashedOutline = this.register(
        new BooleanSetting("Пунктир", "Отрисовывать анимированный пунктирный контур.", true)
    );
    private final NumberSetting lifetime = this.register(
        new NumberSetting("Время жизни", "Время отображения призрака в секундах.", 1.5, 0.4, 4.0, 0.1)
    );

    private final List<BlinkSnapshot> snapshots = new ArrayList<>();
    private Vec3d lastPlayerPos = null;
    private long lastSnapshotTime = 0;
    private float dashOffset = 0.0f;
    private long lastFrameNanos = 0;

    public Blink() {
        super("Blink", "3D полигональные силуэты игрока при движении с анимацией конечностей.", Category.VISUALS);
        this.color.visibleWhen(() -> !this.useClientColor.getValue());
    }

    @Override
    protected void onEnable() {
        this.snapshots.clear();
        this.lastPlayerPos = null;
    }

    @Override
    protected void onDisable() {
        this.snapshots.clear();
        this.lastPlayerPos = null;
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!event.isPre() || !this.isEnabled() || this.mc.player == null) {
            return;
        }

        ClientPlayerEntity player = this.mc.player;
        Vec3d currentPos = player.getEntityPos();
        long now = System.currentTimeMillis();

        if (this.lastPlayerPos == null) {
            this.lastPlayerPos = currentPos;
            return;
        }

        double movedDist = currentPos.distanceTo(this.lastPlayerPos);
        if (movedDist >= 0.35 && (now - this.lastSnapshotTime) >= 140) {
            this.snapshots.add(new BlinkSnapshot(
                now,
                this.lastPlayerPos,
                player.getPitch(),
                player.bodyYaw,
                player.headYaw,
                player.limbAnimator.getAnimationProgress(),
                player.limbAnimator.getSpeed()
            ));
            this.lastPlayerPos = currentPos;
            this.lastSnapshotTime = now;

            while (this.snapshots.size() > 8) {
                this.snapshots.remove(0);
            }
        }
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (!this.isEnabled() || this.snapshots.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lifeMs = (long) (this.lifetime.getFloat() * 1000.0f);

        // Prune expired
        this.snapshots.removeIf(s -> (now - s.createdAt) > lifeMs);
        if (this.snapshots.isEmpty()) return;

        // Dash animation
        long nanos = System.nanoTime();
        if (this.lastFrameNanos != 0) {
            float dt = (nanos - this.lastFrameNanos) / 1.0E9f;
            this.dashOffset = (this.dashOffset + dt * 1.5f) % 1.0f;
        }
        this.lastFrameNanos = nanos;

        Camera camera = event.getCamera();
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = event.getStack();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lightning());

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._disableCull();

        int baseRgb = this.useClientColor.getValue() ? ClientAccent.accent(255.0f) : this.color.getColor();
        int baseR = (baseRgb >> 16) & 0xFF;
        int baseG = (baseRgb >> 8) & 0xFF;
        int baseB = baseRgb & 0xFF;
        int baseAlpha = (baseRgb >> 24) & 0xFF;
        if (baseAlpha == 0) baseAlpha = 255;
        boolean doFill = this.fill.getValue();
        boolean doDashed = this.dashedOutline.getValue();

        for (BlinkSnapshot snap : this.snapshots) {
            float elapsed = (float) (now - snap.createdAt);
            float alphaFactor = MathHelper.clamp(1.0f - (elapsed / (float) lifeMs), 0.0f, 1.0f);
            if (alphaFactor <= 0.01f) continue;

            int r = baseR;
            int g = baseG;
            int b = baseB;
            int a = (int) (baseAlpha * alphaFactor);
            int fillA = (int) (a * 0.35f);

            float limbPos = snap.limbPos;
            float limbSpeed = snap.limbSpeed;
            float armSwing = MathHelper.sin(limbPos * 0.6662f + (float) Math.PI) * 2.0f * limbSpeed * 0.5f;
            float armSwing2 = MathHelper.sin(limbPos * 0.6662f) * 2.0f * limbSpeed * 0.5f;
            float legSwing = MathHelper.sin(limbPos * 0.6662f) * 1.4f * limbSpeed;
            float legSwing2 = MathHelper.sin(limbPos * 0.6662f + (float) Math.PI) * 1.4f * limbSpeed;
            float headDiff = -(snap.headYaw - snap.bodyYaw);

            matrices.push();
            matrices.translate(snap.pos.x - camPos.x, snap.pos.y - camPos.y, snap.pos.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - snap.bodyYaw));

            // Head
            matrices.push();
            matrices.translate(0.0f, 1.95f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(headDiff));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(snap.pitch));
            this.renderBox(matrices, buffer, 0.5f, 0.5f, 0.5f, r, g, b, a, fillA, doFill, doDashed);
            matrices.pop();

            // Torso
            this.renderPart(matrices, buffer, 0.0f, 1.45f, 0.0f, 0.5f, 0.7f, 0.25f, 0.0f, r, g, b, a, fillA, doFill, doDashed);

            // Left & Right Arms
            this.renderPart(matrices, buffer, -0.375f, 1.45f, 0.0f, 0.25f, 0.7f, 0.25f, armSwing, r, g, b, a, fillA, doFill, doDashed);
            this.renderPart(matrices, buffer, 0.375f, 1.45f, 0.0f, 0.25f, 0.7f, 0.25f, armSwing2, r, g, b, a, fillA, doFill, doDashed);

            // Left & Right Legs
            this.renderPart(matrices, buffer, -0.125f, 0.75f, 0.0f, 0.25f, 0.75f, 0.25f, legSwing, r, g, b, a, fillA, doFill, doDashed);
            this.renderPart(matrices, buffer, 0.125f, 0.75f, 0.0f, 0.25f, 0.75f, 0.25f, legSwing2, r, g, b, a, fillA, doFill, doDashed);

            matrices.pop();
        }

        immediate.draw();
        GlStateManager._enableCull();
        GlStateManager._disableBlend();
    }

    private void renderPart(MatrixStack matrices, VertexConsumer buffer, float x, float y, float z, float w, float h, float d, float rotX, int r, int g, int b, int a, int fillA, boolean doFill, boolean doDashed) {
        matrices.push();
        matrices.translate(x, y, z);
        if (Math.abs(rotX) > 0.001f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotX));
        }
        this.renderBox(matrices, buffer, w, h, d, r, g, b, a, fillA, doFill, doDashed);
        matrices.pop();
    }

    private void renderBox(MatrixStack matrices, VertexConsumer buffer, float w, float h, float d, int r, int g, int b, int a, int fillA, boolean doFill, boolean doDashed) {
        float x1 = -w * 0.5f;
        float y1 = -h;
        float z1 = -d * 0.5f;
        float x2 = w * 0.5f;
        float y2 = 0.0f;
        float z2 = d * 0.5f;

        MatrixStack.Entry entry = matrices.peek();
        if (doFill && fillA > 0) {
            this.emitSolidBox(entry, buffer, x1, y1, z1, x2, y2, z2, r, g, b, fillA);
        }
        if (a > 0) {
            this.emitOutline(entry, buffer, x1, y1, z1, x2, y2, z2, r, g, b, a, doDashed);
        }
    }

    private void emitSolidBox(MatrixStack.Entry entry, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        // Front
        emitQuad(entry, buffer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, a);
        // Back
        emitQuad(entry, buffer, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, r, g, b, a);
        // Top
        emitQuad(entry, buffer, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, a);
        // Bottom
        emitQuad(entry, buffer, x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, r, g, b, a);
        // Left
        emitQuad(entry, buffer, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, r, g, b, a);
        // Right
        emitQuad(entry, buffer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
    }

    private void emitOutline(MatrixStack.Entry entry, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a, boolean dashed) {
        emitLine(entry, buffer, x1, y1, z1, x2, y1, z1, r, g, b, a, dashed);
        emitLine(entry, buffer, x2, y1, z1, x2, y1, z2, r, g, b, a, dashed);
        emitLine(entry, buffer, x2, y1, z2, x1, y1, z2, r, g, b, a, dashed);
        emitLine(entry, buffer, x1, y1, z2, x1, y1, z1, r, g, b, a, dashed);

        emitLine(entry, buffer, x1, y2, z1, x2, y2, z1, r, g, b, a, dashed);
        emitLine(entry, buffer, x2, y2, z1, x2, y2, z2, r, g, b, a, dashed);
        emitLine(entry, buffer, x2, y2, z2, x1, y2, z2, r, g, b, a, dashed);
        emitLine(entry, buffer, x1, y2, z2, x1, y2, z1, r, g, b, a, dashed);

        emitLine(entry, buffer, x1, y1, z1, x1, y2, z1, r, g, b, a, dashed);
        emitLine(entry, buffer, x2, y1, z1, x2, y2, z1, r, g, b, a, dashed);
        emitLine(entry, buffer, x2, y1, z2, x2, y2, z2, r, g, b, a, dashed);
        emitLine(entry, buffer, x1, y1, z2, x1, y2, z2, r, g, b, a, dashed);
    }

    private void emitLine(MatrixStack.Entry entry, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a, boolean dashed) {
        if (!dashed) {
            emitThickSegment(entry, buffer, x1, y1, z1, x2, y2, z2, r, g, b, a);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.001f) return;

        float dashLen = 0.06f;
        float gapLen = 0.04f;
        float period = dashLen + gapLen;
        float phase = (this.dashOffset * period) % period;

        for (float t = -phase; t < len; t += period) {
            float s0 = Math.max(0.0f, t);
            float s1 = Math.min(len, t + dashLen);
            if (s1 > s0) {
                float segX1 = x1 + dx * (s0 / len);
                float segY1 = y1 + dy * (s0 / len);
                float segZ1 = z1 + dz * (s0 / len);
                float segX2 = x1 + dx * (s1 / len);
                float segY2 = y1 + dy * (s1 / len);
                float segZ2 = z1 + dz * (s1 / len);
                emitThickSegment(entry, buffer, segX1, segY1, segZ1, segX2, segY2, segZ2, r, g, b, a);
            }
        }
    }

    private static void emitThickSegment(MatrixStack.Entry entry, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        float thick = 0.006f;
        emitQuad(entry, buffer, x1 - thick, y1 - thick, z1 - thick, x2 + thick, y2 + thick, z2 + thick, x2 + thick, y2 + thick, z2 + thick, x1 - thick, y1 - thick, z1 - thick, r, g, b, a);
    }

    private static void emitQuad(MatrixStack.Entry entry, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b, int a) {
        buffer.vertex(entry, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(entry, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(entry, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(entry, x4, y4, z4).color(r, g, b, a);
    }

    private static final class BlinkSnapshot {
        final long createdAt;
        final Vec3d pos;
        final float pitch;
        final float bodyYaw;
        final float headYaw;
        final float limbPos;
        final float limbSpeed;

        BlinkSnapshot(long createdAt, Vec3d pos, float pitch, float bodyYaw, float headYaw, float limbPos, float limbSpeed) {
            this.createdAt = createdAt;
            this.pos = pos;
            this.pitch = pitch;
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
            this.limbPos = limbPos;
            this.limbSpeed = limbSpeed;
        }
    }
}
