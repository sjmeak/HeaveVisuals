package rtx.heave.api.modules.impl.Visuals;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.player.AttackEntityEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;

public final class HitWaves extends Module {
    private final NumberSetting radius = this.register(
        new NumberSetting("Радиус", "Максимальный радиус волны.", 6.0, 2.0, 16.0, 0.5)
    );
    private final NumberSetting speed = this.register(
        new NumberSetting("Скорость", "Скорость распространения волны.", 7.0, 2.0, 20.0, 0.5)
    );
    private final BooleanSetting useClientColor = this.register(
        new BooleanSetting("Цвет клиента", "Использовать основной цвет клиента.", true)
    );
    private final ColorSetting waveColor = this.register(
        new ColorSetting("Цвет", "Цвет волны.", new Color(0x6B8AFD))
    );

    private final List<Wave> waves = new ArrayList<>();

    public HitWaves() {
        super("Hit Waves", "3D волна от удара по блокам поверхности вокруг цели.", Category.VISUALS);
        this.waveColor.visibleWhen(() -> !this.useClientColor.getValue());
    }

    @Override
    protected void onDisable() {
        this.waves.clear();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!this.isEnabled() || this.mc.world == null || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        Vec3d center = target.getEntityPos();
        Color c = this.resolveColor();
        this.spawnWave(center, this.radius.getFloat(), this.speed.getFloat(), c);
    }

    private Color resolveColor() {
        if (this.useClientColor.getValue()) {
            InterfaceModule im = InterfaceModule.getInstance();
            if (im != null) {
                return new Color(im.clientPrimaryColorOpaque(), true);
            }
        }
        return new Color(this.waveColor.getColor(), true);
    }

    private void spawnWave(Vec3d center, float maxRadius, float spd, Color color) {
        List<BlockPos> surfaceBlocks = this.collectSurfaceBlocks(center, maxRadius);
        if (surfaceBlocks.isEmpty()) return;
        float duration = (maxRadius + 2.0f) / Math.max(0.1f, spd);
        this.waves.add(new Wave(center, maxRadius, spd, duration, color, System.currentTimeMillis(), surfaceBlocks));
    }

    private float computeAlphaForBlock(Wave wave, float dist, float waveFront) {
        float maxAlpha = Math.min((float) wave.color.getAlpha() / 255.0f, 0.8f);
        float diff = Math.abs(dist - waveFront);
        if (diff > 1.0f) {
            return 0.0f;
        }
        return maxAlpha * (1.0f - diff);
    }

    private List<BlockPos> collectSurfaceBlocks(Vec3d center, float maxRadius) {
        List<BlockPos> list = new ArrayList<>();
        if (this.mc.world == null) return list;
        int rad = (int) Math.ceil(maxRadius);
        BlockPos centerPos = BlockPos.ofFloored(center);
        float radSq = maxRadius * maxRadius;

        for (int x = -rad; x <= rad; x++) {
            for (int z = -rad; z <= rad; z++) {
                for (int y = -rad; y <= rad; y++) {
                    BlockPos pos = centerPos.add(x, y, z);
                    Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (blockCenter.squaredDistanceTo(center) > radSq) continue;

                    BlockState state = this.mc.world.getBlockState(pos);
                    BlockState above = this.mc.world.getBlockState(pos.up());
                    if (this.isSurfaceBlock(above, state, pos)) {
                        list.add(pos);
                    }
                }
            }
        }
        return list;
    }

    private boolean isSurfaceBlock(BlockState above, BlockState state, BlockPos pos) {
        if (this.mc.world == null) return false;
        if (state.isAir()) return false;
        if (!above.isAir()) return false;
        return !state.getOutlineShape(this.mc.world, pos).isEmpty();
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (!this.isEnabled() || this.waves.isEmpty() || this.mc.world == null) {
            return;
        }
        Camera camera = event.getCamera();
        if (camera == null) return;
        Vec3d camPos = camera.getCameraPos();
        long now = System.currentTimeMillis();

        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lightning());
        MatrixStack matrices = event.getStack();

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._disableCull();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        try {
            Iterator<Wave> it = this.waves.iterator();
            while (it.hasNext()) {
                Wave wave = it.next();
                float elapsed = (float) (now - wave.startTime) / 1000.0f;
                if (elapsed > wave.durationSeconds) {
                    it.remove();
                    continue;
                }
                float waveFront = elapsed * wave.speed;
                for (BlockPos pos : wave.blocks) {
                    Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    float dist = (float) Math.sqrt(blockCenter.squaredDistanceTo(wave.center));
                    if (dist > wave.radius) continue;

                    float alpha = this.computeAlphaForBlock(wave, dist, waveFront);
                    if (alpha <= 0.0f) continue;

                    int fillAlpha = MathHelper.clamp((int) (150.0f * alpha * 0.35f), 0, 255);
                    int wireAlpha = MathHelper.clamp((int) (255.0f * alpha), 0, 255);

                    Color c = wave.color;
                    int r = c.getRed();
                    int g = c.getGreen();
                    int b = c.getBlue();

                    Box solidBox = new Box(pos).expand(0.002);
                    Box wireBox = new Box(pos).expand(0.003);

                    this.drawSolidBox(matrices, buffer, solidBox, r, g, b, fillAlpha);
                    this.drawWireframeBox(matrices, buffer, wireBox, 0.015f, r, g, b, wireAlpha);
                }
            }
        } finally {
            matrices.pop();
            immediate.draw();
            GlStateManager._enableCull();
            GlStateManager._disableBlend();
        }
    }

    private void drawSolidBox(MatrixStack matrices, VertexConsumer buffer, Box box, int r, int g, int b, int a) {
        this.addBoxVertices(matrices, buffer,
            (float) box.minX, (float) box.minY, (float) box.minZ,
            (float) box.maxX, (float) box.maxY, (float) box.maxZ,
            r, g, b, a
        );
    }

    private void drawWireframeBox(MatrixStack matrices, VertexConsumer buffer, Box box, float f, int r, int g, int b, int a) {
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // 4 vertical edges
        this.addBoxVertices(matrices, buffer, x1 - f, y1, z1 - f, x1 + f, y2, z1 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x2 - f, y1, z1 - f, x2 + f, y2, z1 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x1 - f, y1, z2 - f, x1 + f, y2, z2 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x2 - f, y1, z2 - f, x2 + f, y2, z2 + f, r, g, b, a);

        // 4 bottom edges
        this.addBoxVertices(matrices, buffer, x1, y1 - f, z1 - f, x2, y1 + f, z1 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x1, y1 - f, z2 - f, x2, y1 + f, z2 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x1 - f, y1 - f, z1, x1 + f, y1 + f, z2, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x2 - f, y1 - f, z1, x2 + f, y1 + f, z2, r, g, b, a);

        // 4 top edges
        this.addBoxVertices(matrices, buffer, x1, y2 - f, z1 - f, x2, y2 + f, z1 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x1, y2 - f, z2 - f, x2, y2 + f, z2 + f, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x1 - f, y2 - f, z1, x1 + f, y2 + f, z2, r, g, b, a);
        this.addBoxVertices(matrices, buffer, x2 - f, y2 - f, z1, x2 + f, y2 + f, z2, r, g, b, a);
    }

    private void addBoxVertices(MatrixStack matrices, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int r, int g, int b, int a) {
        MatrixStack.Entry entry = matrices.peek();
        // Down
        buffer.vertex(entry, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, minX, minY, maxZ).color(r, g, b, a);
        // Up
        buffer.vertex(entry, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(entry, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, maxY, minZ).color(r, g, b, a);
        // North
        buffer.vertex(entry, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(entry, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, minY, minZ).color(r, g, b, a);
        // South
        buffer.vertex(entry, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, minX, minY, maxZ).color(r, g, b, a);
        // West
        buffer.vertex(entry, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(entry, minX, minY, minZ).color(r, g, b, a);
        // East
        buffer.vertex(entry, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(entry, maxX, minY, maxZ).color(r, g, b, a);
    }

    private static record Wave(Vec3d center, float radius, float speed, float durationSeconds, Color color, long startTime, List<BlockPos> blocks) {}
}
