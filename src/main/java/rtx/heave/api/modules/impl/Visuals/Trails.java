package rtx.heave.api.modules.impl.Visuals;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;

public final class Trails extends Module {
    private final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Стиль отрисовки шлейфа.", "Лента", "Лента", "Линия")
    );
    private final NumberSetting length = this.register(
        new NumberSetting("Длина", "Время жизни точек шлейфа в секундах.", 1.2, 0.2, 3.0, 0.1)
    );
    private final NumberSetting size = this.register(
        new NumberSetting("Размер", "Высота ленточного шлейфа.", 0.6, 0.1, 1.8, 0.05)
    );
    private final BooleanSetting useClientColor = this.register(
        new BooleanSetting("Цвет клиента", "Использовать основной цвет клиента.", true)
    );
    private final ColorSetting trailColor = this.register(
        new ColorSetting("Цвет", "Цвет шлейфа.", new Color(0x6B8AFD))
    );

    private final List<TrailPoint> points = new ArrayList<>();

    public Trails() {
        super("Trails", "Плавный полигональный шлейф за игроком при движении.", Category.VISUALS);
        this.trailColor.visibleWhen(() -> !this.useClientColor.getValue());
    }

    @Override
    protected void onDisable() {
        this.points.clear();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!event.isPre() || !this.isEnabled() || this.mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long maxLifetimeMs = (long) (this.length.getValue() * 1000.0);

        this.points.removeIf(p -> now - p.createdAt > maxLifetimeMs);

        Vec3d currentPos = this.mc.player.getEntityPos();
        if (this.points.isEmpty() || this.points.get(this.points.size() - 1).pos.squaredDistanceTo(currentPos) > 0.005) {
            this.points.add(new TrailPoint(currentPos, now));
        }
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (!this.isEnabled() || this.mc.player == null || this.points.size() < 2) {
            return;
        }
        Camera camera = event.getCamera();
        if (camera == null) return;
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = event.getStack();
        long now = System.currentTimeMillis();
        float lifetimeMs = (float) (this.length.getValue() * 1000.0);
        float ribbonHeight = this.size.getFloat();

        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lightning());

        Color baseColor = this.resolveColor();
        int r = baseColor.getRed();
        int g = baseColor.getGreen();
        int b = baseColor.getBlue();

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._disableCull();

        MatrixStack.Entry entry = matrices.peek();

        // Connect points with quads
        for (int i = 0; i < this.points.size() - 1; i++) {
            TrailPoint p1 = this.points.get(i);
            TrailPoint p2 = this.points.get(i + 1);

            float age1 = (float) (now - p1.createdAt);
            float age2 = (float) (now - p2.createdAt);
            float alpha1 = MathHelper.clamp(1.0f - (age1 / lifetimeMs), 0.0f, 1.0f);
            float alpha2 = MathHelper.clamp(1.0f - (age2 / lifetimeMs), 0.0f, 1.0f);

            int a1 = (int) (alpha1 * 180.0f);
            int a2 = (int) (alpha2 * 180.0f);
            if (a1 <= 0 && a2 <= 0) continue;

            float x1 = (float) (p1.pos.x - camPos.x);
            float y1_bottom = (float) (p1.pos.y + 0.1 - camPos.y);
            float y1_top = y1_bottom + ribbonHeight;
            float z1 = (float) (p1.pos.z - camPos.z);

            float x2 = (float) (p2.pos.x - camPos.x);
            float y2_bottom = (float) (p2.pos.y + 0.1 - camPos.y);
            float y2_top = y2_bottom + ribbonHeight;
            float z2 = (float) (p2.pos.z - camPos.z);

            if (this.mode.is("Лента")) {
                // Quad 1 (front)
                buffer.vertex(entry, x1, y1_bottom, z1).color(r, g, b, a1);
                buffer.vertex(entry, x2, y2_bottom, z2).color(r, g, b, a2);
                buffer.vertex(entry, x2, y2_top, z2).color(r, g, b, a2);
                buffer.vertex(entry, x1, y1_top, z1).color(r, g, b, a1);

                // Quad 2 (back)
                buffer.vertex(entry, x1, y1_top, z1).color(r, g, b, a1);
                buffer.vertex(entry, x2, y2_top, z2).color(r, g, b, a2);
                buffer.vertex(entry, x2, y2_bottom, z2).color(r, g, b, a2);
                buffer.vertex(entry, x1, y1_bottom, z1).color(r, g, b, a1);
            } else {
                // Line mode: slim ribbon
                float lineH = 0.06f;
                buffer.vertex(entry, x1, y1_bottom, z1).color(r, g, b, a1);
                buffer.vertex(entry, x2, y2_bottom, z2).color(r, g, b, a2);
                buffer.vertex(entry, x2, y2_bottom + lineH, z2).color(r, g, b, a2);
                buffer.vertex(entry, x1, y1_bottom + lineH, z1).color(r, g, b, a1);
            }
        }

        immediate.draw();
        GlStateManager._enableCull();
        GlStateManager._disableBlend();
    }

    private Color resolveColor() {
        if (this.useClientColor.getValue()) {
            InterfaceModule im = InterfaceModule.getInstance();
            if (im != null) {
                return new Color(im.clientPrimaryColorOpaque(), true);
            }
        }
        return new Color(this.trailColor.getColor(), true);
    }

    private static record TrailPoint(Vec3d pos, long createdAt) {}
}
