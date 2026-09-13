package rtx.heave.api.modules.impl.Visuals.killeffect;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import rtx.heave.api.modules.impl.Visuals.particles.ParticleRenderer;
import rtx.heave.utils.color.ColorUtil;

public final class KillEffectParticleSystem {
    private static final Identifier BLOOM_TEXTURE = Identifier.of("heave", "images/particles/bloom.png");
    private static final Random RANDOM = new Random();
    private final List<BodyParticle> particles = new CopyOnWriteArrayList<>();
    private final ParticleRenderer renderer = new ParticleRenderer();

    public void clear() {
        this.particles.clear();
        this.renderer.clear();
    }

    public void tick(ClientWorld world) {
        if (this.particles.isEmpty()) {
            return;
        }
        this.particles.removeIf(BodyParticle::isDead);
        for (BodyParticle p : this.particles) {
            p.step(world);
        }
    }

    public void spawnBodyShape(LivingEntity entity, int color, boolean gravityMode) {
        if (entity == null) return;
        Vec3d base = entity.getEntityPos();
        float height = entity.getHeight();
        float width = entity.getWidth();
        float yawRad = (float) Math.toRadians(-entity.getBodyYaw() + 90.0f);
        int baseCount = 250;

        // Head
        float headY = height - 0.2f;
        float headR = width * 0.4f;
        this.spawnSphere(base.add(0.0, headY, 0.0), headR, baseCount / 10, color, gravityMode);

        // Torso
        float torsoMaxY = height * 0.85f;
        float torsoMinY = height * 0.4f;
        float torsoW = width * 0.4f;
        float torsoD = width * 0.2f;
        this.spawnBox(base, torsoMinY, torsoMaxY, torsoW, torsoD, yawRad, baseCount / 4, color, gravityMode);

        // Arms (left & right)
        float armLen = height * 0.4f;
        float armW = width * 0.15f;
        for (int side = -1; side <= 1; side += 2) {
            Vec3d armOffset = new Vec3d(Math.sin(yawRad) * width * 0.5 * side, height * 0.75f, Math.cos(yawRad) * width * 0.5 * side);
            this.spawnColumn(base.add(armOffset), armLen, armW, yawRad, baseCount / 8, color, gravityMode);
        }

        // Legs (left & right)
        float legLen = height * 0.45f;
        float legW = width * 0.15f;
        for (int side = -1; side <= 1; side += 2) {
            Vec3d legOffset = new Vec3d(Math.sin(yawRad) * width * 0.15f * side, height * 0.4f, Math.cos(yawRad) * width * 0.15f * side);
            this.spawnColumn(base.add(legOffset), legLen, legW, yawRad, baseCount / 6, color, gravityMode);
        }
    }

    public void spawnSphere(Vec3d center, float radius, int count, int color, boolean gravity) {
        for (int i = 0; i < count; i++) {
            float phi = RANDOM.nextFloat() * (float) Math.PI * 2.0f;
            float theta = (float) Math.acos(2.0f * RANDOM.nextFloat() - 1.0f);
            float r = radius * (float) Math.cbrt(RANDOM.nextFloat());
            float ox = r * (float)(Math.sin(theta) * Math.cos(phi));
            float oz = r * (float)(Math.sin(theta) * Math.sin(phi));
            float oy = r * (float) Math.cos(theta);
            float velSpread = gravity ? 0.02f : 0.008f;
            float vx = (RANDOM.nextFloat() - 0.5f) * velSpread;
            float vy = gravity ? (0.03f + RANDOM.nextFloat() * 0.04f) : ((RANDOM.nextFloat() - 0.5f) * 0.008f);
            float vz = (RANDOM.nextFloat() - 0.5f) * velSpread;
            this.particles.add(new BodyParticle(center, ox, oy, oz, vx, vy, vz, color, gravity));
        }
    }

    private void spawnBox(Vec3d base, float minY, float maxY, float width, float depth, float yawRad, int count, int color, boolean gravity) {
        for (int i = 0; i < count; i++) {
            float rx = (RANDOM.nextFloat() - 0.5f) * width * 2.0f;
            float ry = minY + RANDOM.nextFloat() * (maxY - minY);
            float rz = (RANDOM.nextFloat() - 0.5f) * depth * 2.0f;
            float ox = (float)(rx * Math.cos(yawRad) - rz * Math.sin(yawRad));
            float oz = (float)(rx * Math.sin(yawRad) + rz * Math.cos(yawRad));
            float velSpread = gravity ? 0.025f : 0.01f;
            float vx = (RANDOM.nextFloat() - 0.5f) * velSpread;
            float vy = gravity ? (0.04f + RANDOM.nextFloat() * 0.05f) : ((RANDOM.nextFloat() - 0.5f) * 0.01f);
            float vz = (RANDOM.nextFloat() - 0.5f) * velSpread;
            this.particles.add(new BodyParticle(base, ox, ry, oz, vx, vy, vz, color, gravity));
        }
    }

    private void spawnColumn(Vec3d base, float length, float radius, float yawRad, int count, int color, boolean gravity) {
        for (int i = 0; i < count; i++) {
            float ox = (RANDOM.nextFloat() - 0.5f) * radius * 2.0f;
            float oy = -RANDOM.nextFloat() * length;
            float oz = (RANDOM.nextFloat() - 0.5f) * radius * 2.0f;
            float velSpread = gravity ? 0.018f : 0.006f;
            float vx = (RANDOM.nextFloat() - 0.5f) * velSpread;
            float vy = gravity ? (0.025f + RANDOM.nextFloat() * 0.035f) : ((RANDOM.nextFloat() - 0.5f) * 0.006f);
            float vz = (RANDOM.nextFloat() - 0.5f) * velSpread;
            this.particles.add(new BodyParticle(base, ox, oy, oz, vx, vy, vz, color, gravity));
        }
    }

    public void render(MatrixStack matrices, Camera camera, VertexConsumerProvider.Immediate immediate) {
        if (this.particles.isEmpty() || camera == null || immediate == null) {
            return;
        }
        Vec3d camPos = camera.getCameraPos();
        Quaternionf camRot = camera.getRotation();

        for (BodyParticle p : this.particles) {
            float alpha = p.alpha();
            if (alpha <= 0.001f) continue;
            int mainColor = ColorUtil.withAlpha(p.color, Math.round(alpha * 230.0f));
            Vec3d pos = new Vec3d(p.x, p.y, p.z);
            this.renderer.drawTexture(matrices, immediate, BLOOM_TEXTURE, pos, camPos, camRot, 0.25f, 0.0f, mainColor, true);
            int glowColor = ColorUtil.withAlpha(p.color, Math.round(alpha * 55.0f));
            this.renderer.drawTexture(matrices, immediate, BLOOM_TEXTURE, pos, camPos, camRot, 0.60f, 0.0f, glowColor, true);
        }
        this.renderer.flush(immediate);
    }

    public static class BodyParticle {
        double x, y, z;
        float vx, vy, vz;
        long startTime, lastTime;
        long lifetime;
        float gravityAcc;
        float drag;
        int color;
        boolean hasGravity;

        public BodyParticle(Vec3d basePos, float ox, float oy, float oz, float vx, float vy, float vz, int color, boolean gravityMode) {
            this.x = basePos.x + ox;
            this.y = basePos.y + oy;
            this.z = basePos.z + oz;
            this.color = color;
            this.startTime = System.currentTimeMillis();
            this.lastTime = this.startTime;
            this.hasGravity = gravityMode;
            this.gravityAcc = 0.005f + RANDOM.nextFloat() * 0.005f;
            this.lifetime = 1500L + RANDOM.nextInt(1500);
            this.drag = gravityMode ? 0.999f : 0.995f;
            float spread = gravityMode ? 0.04f : 0.03f;
            this.vx = (RANDOM.nextFloat() - 0.5f) * spread + vx;
            this.vy = gravityMode ? (0.025f + RANDOM.nextFloat() * 0.035f + vy) : ((RANDOM.nextFloat() - 0.5f) * 0.03f + vy);
            this.vz = (RANDOM.nextFloat() - 0.5f) * spread + vz;
        }

        public void step(ClientWorld world) {
            long now = System.currentTimeMillis();
            float dt = Math.min((float)(now - this.lastTime) / 16.67f, 5.0f);
            this.lastTime = now;

            if (this.hasGravity) {
                this.vy -= this.gravityAcc * dt;
            }
            float curDrag = (float) Math.pow(this.drag, dt);
            this.vx *= curDrag;
            this.vy *= curDrag;
            this.vz *= curDrag;

            double nx = this.x + this.vx * dt;
            double ny = this.y + this.vy * dt;
            double nz = this.z + this.vz * dt;

            if (this.hasGravity && world != null) {
                BlockPos floorPos = BlockPos.ofFloored(this.x, ny - 0.05, this.z);
                if (!world.getBlockState(floorPos).isAir()) {
                    this.vy = -this.vy * 0.5f;
                    ny = this.y;
                }
                BlockPos wallX = BlockPos.ofFloored(nx, this.y, this.z);
                if (!world.getBlockState(wallX).isAir()) {
                    this.vx = -this.vx * 0.5f;
                    nx = this.x;
                }
                BlockPos wallZ = BlockPos.ofFloored(this.x, this.y, nz);
                if (!world.getBlockState(wallZ).isAir()) {
                    this.vz = -this.vz * 0.5f;
                    nz = this.z;
                }
            }

            if (Math.abs(this.vy) <= 1.0E-4f) {
                this.vx = 0.0f;
                this.vz = 0.0f;
            }

            this.x = nx;
            this.y = ny;
            this.z = nz;
        }

        public boolean isDead() {
            return System.currentTimeMillis() - this.startTime > this.lifetime;
        }

        public float alpha() {
            float progress = MathHelper.clamp((float)(System.currentTimeMillis() - this.startTime) / (float)this.lifetime, 0.0f, 1.0f);
            return 1.0f - progress;
        }
    }
}
