package rtx.heave.utils.render.targetesp;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rtx.heave.utils.render.others.WorldVertex;

public class PhantomSpiritsRenderer {
    public static final Identifier BLOOM_TEXTURE = Identifier.of("heave", "textures/targetesp/bloom.png");

    private static final float[] SCALE_CACHE = new float[101];

    static {
        for (int idx = 0; idx <= 100; idx++) {
            SCALE_CACHE[idx] = Math.max(0.28f * ((float) idx / 100.0f), 0.15f);
        }
    }

    public static int getRainbowColor(long now, float offset) {
        float hue = (((float) (now % 3600L) / 3600.0f) + offset) % 1.0f;
        return Color.HSBtoRGB(hue, 1.0f, 1.0f);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    public static int multAlpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * factor);
        return (MathHelper.clamp(a, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    public static int multDark(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >>> 16) & 0xFF) * factor);
        int g = (int) (((argb >>> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawBillboardQuad(
        MatrixStack matrices,
        VertexConsumer vc,
        float x, float y, float z,
        Quaternionf cameraRot,
        float size,
        int color
    ) {
        if (size <= 0.001f || (color >>> 24) == 0) return;
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(cameraRot);
        MatrixStack.Entry entry = matrices.peek();
        float hs = size * 0.5f;
        WorldVertex.textured(vc, entry, -hs, -hs, 0.0f, 0.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, hs, -hs, 0.0f, 1.0f, 0.0f, color);
        WorldVertex.textured(vc, entry, hs, hs, 0.0f, 1.0f, 1.0f, color);
        WorldVertex.textured(vc, entry, -hs, hs, 0.0f, 0.0f, 1.0f, color);
        matrices.pop();
    }

    public static void drawSpiritsQuad(
        MatrixStack matrices,
        VertexConsumer vc,
        float x, float y, float z,
        Quaternionf cameraRot,
        float size,
        int c1, int c2, int c3, int c4
    ) {
        if (size <= 0.001f) return;
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(cameraRot);
        matrices.translate(size * 0.5f, size * 0.5f, 0.0f);
        MatrixStack.Entry entry = matrices.peek();
        WorldVertex.textured(vc, entry, 0.0f, -size, 0.0f, 0.0f, 0.0f, c1);
        WorldVertex.textured(vc, entry, -size, -size, 0.0f, 0.0f, 1.0f, c2);
        WorldVertex.textured(vc, entry, -size, 0.0f, 0.0f, 1.0f, 1.0f, c3);
        WorldVertex.textured(vc, entry, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, c4);
        matrices.pop();
    }

    public static void renderNewSpirits(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Camera camera,
        Quaternionf cameraRot,
        float fadeAlpha,
        long now,
        float spiritTime,
        int baseRgb,
        boolean isRainbow
    ) {
        int renderAlpha = (int) (235.0f * fadeAlpha);
        if (renderAlpha <= 0) return;

        float pulse = 0.85f + 0.15f * (float) Math.sin((double) now / 400.0);
        float sizeScale = fadeAlpha * pulse;

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int i = 0; i < 9; i += 3) {
            int i2 = i * i;
            for (int i3 = 0; i3 < 12; i3++) {
                float f = spiritTime + (float) i3 * 0.1f;
                float px = (float) (0.75 * Math.sin((double) (f + (float) i2)));
                float py = (float) (0.5 + 0.3 * Math.sin((double) (spiritTime + (float) i3 * 0.2f)) + (double) (0.2f * (float) i));
                float pz = (float) (0.75 * Math.cos((double) (f - (float) i2)));

                float scale = (0.005f + (float) i3 / 2000.0f) * sizeScale;
                float quadSize = 50.0f * scale;

                int color;
                if (!isRainbow) {
                    color = withAlpha(baseRgb, renderAlpha);
                } else {
                    int rgb = getRainbowColor(now, (float) (i + i3) * 0.1f);
                    color = withAlpha(rgb, renderAlpha);
                }

                drawBillboardQuad(matrices, vc, px, py, pz, cameraRot, quadSize, color);
            }
        }

        immediate.draw(glowLayer);
    }

    public static void renderFriendsSpirits(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float fadeAlpha,
        long now,
        int baseRgb,
        boolean isRainbow
    ) {
        renderFriendsSpirits(immediate, matrices, cameraRot, fadeAlpha, now, baseRgb, baseRgb, isRainbow);
    }

    public static void renderFriendsSpirits(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float fadeAlpha,
        long now,
        int baseRgb,
        int secondRgb,
        boolean isRainbow
    ) {
        int renderAlpha = (int) (235.0f * fadeAlpha);
        if (renderAlpha <= 0) return;

        float baseSize = 0.594f * fadeAlpha;
        double d4 = (double) now / 384.61539872299335;
        double d5 = (double) now / 666.6666666666666;

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        float radius = 0.65f;
        for (int layer = 0; layer < 4; layer++) {
            int layerColor = (layer % 2 == 0) ? baseRgb : secondRgb;
            for (int i = 0; i < 20; i++) {
                double d6 = d4 - (double) i * 0.05;
                double d7 = d5 - (double) i * 0.05;
                double cyc = (Math.sin(d7) + 1.0) * 0.5;
                double baseAngle = Math.toRadians((double) layer * 90.0 + d6 * 50.0 % 360.0);
                double d8 = Math.cos(baseAngle) * (double) radius;
                double d9 = Math.sin(baseAngle) * (double) radius;
                double d10 = (layer % 2 != 0) ? (1.8 - 1.7 * cyc) : (0.1 + 1.7 * cyc);

                float factor = (float) i / 20.0f;
                float sizeFactor = 1.0f - factor * 0.6f;
                float dynSize = baseSize * sizeFactor;

                int targetColor = isRainbow ? getRainbowColor(now, (float) (layer * 20 + i) * 0.05f) : layerColor;
                targetColor = withAlpha(targetColor, renderAlpha);

                int color = multAlpha(targetColor, sizeFactor);
                drawBillboardQuad(matrices, vc, (float) d8, (float) d10, (float) d9, cameraRot, dynSize, color);
            }
            radius = -radius;
        }

        immediate.draw(glowLayer);
    }

    public static void renderGhosts(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float entityWidth,
        float entityHeight,
        float fadeAlpha,
        long now,
        long lastGhostTime,
        int baseRgb
    ) {
        renderGhosts(immediate, matrices, cameraRot, entityWidth, entityHeight, fadeAlpha, now, lastGhostTime, baseRgb, baseRgb);
    }

    public static void renderGhosts(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float entityWidth,
        float entityHeight,
        float fadeAlpha,
        long now,
        long lastGhostTime,
        int baseRgb,
        int secondRgb
    ) {
        int renderAlpha = (int) (235.0f * fadeAlpha);
        if (renderAlpha <= 0) return;

        double radius = 0.3 + (double) entityWidth / 2.0;
        float size = 0.4f;
        double distance = 6.0;
        int length = 40;
        float centerY = (float) (0.32 + (double) entityHeight / 2.0);

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int pass = 0; pass < 3; pass++) {
            int passColor = (pass % 2 == 0) ? baseRgb : secondRgb;
            for (int i = 0; i < length; i++) {
                double angle = 0.05 * ((double) (now - lastGhostTime) - (double) i * distance) / 30.0;
                double d = Math.sin(angle * Math.PI) * radius;
                double d2 = Math.cos(angle * Math.PI) * radius;
                if (pass == 1) {
                    d = -d;
                }
                float f2 = (float) i / 40.0f;
                float scale = 1.0f - f2 * 0.3f;
                float curSize = size * scale;
                double zPos = (pass != 2) ? -d2 : d2;

                int targetColor = withAlpha(passColor, (int) ((float) renderAlpha * scale));
                int color = multAlpha(targetColor, scale);

                drawBillboardQuad(matrices, vc, (float) d, centerY + (float) d2, (float) zPos, cameraRot, curSize, color);
            }
        }

        immediate.draw(glowLayer);
    }

    public static void renderJavelinSpirits(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float entityHeight,
        float fadeAlpha,
        long now,
        long modeStartTime,
        float accumulatedHurt,
        int baseRgb
    ) {
        float centerY = entityHeight * 0.5f;
        float animValue = -0.15f * fadeAlpha + 0.65f;
        long time = (long) ((float) (now - modeStartTime) / 2.0f);

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 14; i++) {
                float progress = (float) i / 13.0f;
                float size = (0.55f * (1.0f - progress) + 0.2f * progress) * fadeAlpha * 1.5f;
                double angle = (double) (0.2f * ((float) time + accumulatedHurt - (float) i * 7.0f) / 15.0f);
                float wave = (progress < 0.5f) ? progress * 2.0f : (1.0f - progress) * 2.0f;
                double amplitude = Math.sin((double) wave * Math.PI) * 2.0;

                Random random = new Random((long) i * 12345L);
                double offsetX = (random.nextDouble() - 0.5) * amplitude;
                double offsetY = (random.nextDouble() - 0.5) * amplitude;
                double offsetZ = (random.nextDouble() - 0.5) * amplitude;

                double animOffsetX = offsetX * (double) fadeAlpha - offsetX;
                double animOffsetY = offsetY * (double) fadeAlpha - offsetY;
                double animOffsetZ = offsetZ * (double) fadeAlpha - offsetZ;

                double d4 = -Math.sin(angle) * (double) animValue;
                double d5 = -Math.cos(angle) * (double) animValue;

                double px, py, pz;
                switch (layer) {
                    case 0:
                        animOffsetY += (double) i * 0.02;
                        px = d4 + animOffsetX;
                        py = centerY + d5 + animOffsetY;
                        pz = -d5 + animOffsetZ;
                        break;
                    case 1:
                        animOffsetY -= (double) i * 0.02;
                        px = -d4 + animOffsetX;
                        py = centerY + d4 + animOffsetY;
                        pz = -d5 + animOffsetZ;
                        break;
                    default:
                        px = -d4 + animOffsetX;
                        py = centerY - d4 + animOffsetY;
                        pz = d5 + animOffsetZ;
                        break;
                }

                int alpha = (int) (240.0f * fadeAlpha);
                int color = withAlpha(baseRgb, alpha);

                drawBillboardQuad(matrices, vc, (float) px, (float) py, (float) pz, cameraRot, size, color);
            }
        }

        immediate.draw(glowLayer);
    }

    public static void renderNursultan(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float entityHeight,
        float fadeAlpha,
        long now,
        long modeStartTime,
        int baseRgb
    ) {
        float centerY = entityHeight * 0.5f;
        float time = (float) (now - modeStartTime) / 1100.0f;
        float rotation = time * 360.0f;
        float radius = 0.5f;

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int layer = 0; layer < 4; layer++) {
            float layerOffset = (float) (layer - 1) * 0.4f;
            float prevSize = -1.0f;

            for (float f = 0.0f; f < 130.0f; f += 2.0f) {
                float angle = rotation + f + layerOffset * 360.0f;
                double radians = Math.toRadians((double) -angle);
                double yOffset = Math.sin(radians + 2.0) * (double) layerOffset;
                float size = radius * (f / 140.0f);
                float finalSize = (prevSize < 0.0f ? size : (prevSize + size) / 2.0f) * fadeAlpha;
                prevSize = size;

                float alphaPrc = MathHelper.clamp(finalSize, 0.0f, 1.0f);
                int colorAlpha = (int) (240.0f * fadeAlpha * alphaPrc);
                int color = withAlpha(baseRgb, colorAlpha);

                float px = (float) (Math.cos(radians) * 0.5);
                float py = centerY + (float) yOffset;
                float pz = (float) (Math.sin(radians) * 0.5);

                drawBillboardQuad(matrices, vc, px, py, pz, cameraRot, finalSize, color);
            }
        }

        immediate.draw(glowLayer);
    }

    public static void renderCircle(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float entityHeight,
        float fadeAlpha,
        long now,
        long modeStartTime,
        float accumulatedHurt,
        int baseRgb
    ) {
        long time = now % 1500L;
        boolean ascending = (time > 750L);
        float progress = (float) time / 750.0f;
        if (!ascending) {
            progress = 1.0f - progress;
        } else {
            progress = progress - 1.0f;
        }

        float easedProg = ((double) progress >= 0.5)
            ? (float) (1.0 - Math.pow((double) (-2.0f * progress + 2.0f), 2.0) / 2.0)
            : 2.0f * progress * progress;
        progress = easedProg;

        float halfH = entityHeight / 2.0f;
        float hFactor = ((double) progress <= 0.5) ? progress : (1.0f - progress);
        int dir = (!ascending) ? 1 : -1;
        double yOffset = (double) (halfH * hFactor * (float) dir);
        double ringY = (double) (entityHeight * progress) + yOffset;

        long timeMs = (long) ((float) (now - modeStartTime) / 2.5f);

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int layer = 0; layer < 4; layer++) {
            for (int i = 0; i < 15; i++) {
                float particleProgress = (float) i / 14.0f;
                float size = (0.5f * (1.0f - particleProgress) + 0.5f * particleProgress) * fadeAlpha;
                float angle = 0.2f * ((float) timeMs + accumulatedHurt - (float) i * 3.5f) / 15.0f;
                float wave = (particleProgress < 0.5f) ? particleProgress * 2.0f : (1.0f - particleProgress) * 2.0f;
                double amplitude = Math.sin((double) wave * Math.PI) * 2.0;

                Random random = new Random((long) i * 12345L);
                double offsetX = (random.nextDouble() - 0.5) * amplitude;
                double offsetY = (random.nextDouble() - 0.5) * amplitude;
                double offsetZ = (random.nextDouble() - 0.5) * amplitude;

                double animOffsetX = offsetX * (double) fadeAlpha - offsetX;
                double animOffsetY = offsetY * (double) fadeAlpha - offsetY;
                double animOffsetZ = offsetZ * (double) fadeAlpha - offsetZ;

                double radius = 0.7;
                double px, py, pz;
                py = ringY + animOffsetY;

                switch (layer) {
                    case 0:
                        px = Math.cos((double) angle) * radius + animOffsetX;
                        pz = Math.sin((double) angle) * radius + animOffsetZ;
                        break;
                    case 1:
                        px = -Math.sin((double) angle) * radius + animOffsetX;
                        pz = Math.cos((double) angle) * radius + animOffsetZ;
                        break;
                    case 2:
                        px = -Math.cos((double) angle) * radius + animOffsetX;
                        pz = -Math.sin((double) angle) * radius + animOffsetZ;
                        break;
                    default:
                        px = Math.sin((double) angle) * radius + animOffsetX;
                        pz = -Math.cos((double) angle) * radius + animOffsetZ;
                        break;
                }

                int alpha = (int) (240.0f * fadeAlpha);
                int color = withAlpha(baseRgb, alpha);

                drawBillboardQuad(matrices, vc, (float) px, (float) py, (float) pz, cameraRot, size, color);
            }
        }

        immediate.draw(glowLayer);
    }

    public static void renderGhostOrbits(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Camera camera,
        Quaternionf cameraRot,
        Vec3d targetWorldPos,
        Vec3d cameraPos,
        float entityHeight,
        float fadeAlpha,
        long now,
        int hurtTime,
        float[] orbitShrinkHolder,
        Vec3d[] orbitPositions,
        Vec3d[] orbitMotions,
        List<Vec3d>[] orbitTrails,
        int baseRgb
    ) {
        if (fadeAlpha <= 0.01f) return;

        Vec3d targetCenter = new Vec3d(targetWorldPos.x, targetWorldPos.y + (double) entityHeight / 2.0, targetWorldPos.z);
        float timeAngle = (float) (now % 10000L) / 10000.0f * 360.0f * 0.27272728f;
        float shrinkTarget = (hurtTime > 7) ? 1.0f : 0.0f;
        orbitShrinkHolder[0] += (shrinkTarget - orbitShrinkHolder[0]) * 0.1f;
        orbitShrinkHolder[0] = MathHelper.clamp(orbitShrinkHolder[0], 0.0f, 1.0f);

        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer vc = immediate.getBuffer(glowLayer);

        for (int i = 0; i < 3; i++) {
            float angleOffset = (float) i * 360.0f / 3.0f;
            float currentAngle = timeAngle + angleOffset;
            double radian = Math.toRadians((double) currentAngle);
            float orbitRadius = 0.4f - orbitShrinkHolder[0] * 0.4f;
            float fx = (float) Math.sin(radian) * orbitRadius;
            float fz = (float) Math.cos(radian) * orbitRadius;
            double fy = 0.3 * Math.sin(Math.toRadians((double) (timeAngle / ((float) i + 1.0f))));
            Vec3d targetGhostPos = targetCenter.add((double) fx, fy, (double) fz);

            if (orbitPositions[i] == null || orbitPositions[i].distanceTo(targetGhostPos) > 10.0) {
                orbitPositions[i] = targetGhostPos;
                orbitMotions[i] = Vec3d.ZERO;
            }

            Vec3d diff = targetGhostPos.subtract(orbitPositions[i]);
            orbitMotions[i] = new Vec3d(diff.x * 0.5, diff.y * 0.5, diff.z * 0.5);
            orbitPositions[i] = orbitPositions[i].add(orbitMotions[i]);

            if (orbitTrails[i].isEmpty() || orbitTrails[i].get(0).distanceTo(orbitPositions[i]) > 0.01) {
                orbitTrails[i].add(0, orbitPositions[i]);
                while (orbitTrails[i].size() > 40) {
                    orbitTrails[i].remove(orbitTrails[i].size() - 1);
                }
            }

            // Render Trails
            for (int trailIdx = 0; trailIdx < orbitTrails[i].size(); trailIdx++) {
                Vec3d trailPos = orbitTrails[i].get(trailIdx);
                float offset = 1.0f - (float) trailIdx / 40.0f;
                float trailOpacity = (float) Math.pow((double) offset, 1.8) * fadeAlpha * 0.7f;
                int trailAlpha = (int) (trailOpacity * 255.0f);
                int color = withAlpha(baseRgb, trailAlpha);
                float scale = SCALE_CACHE[Math.min((int) (offset * 100.0f), 100)] * 0.8f;

                float rx = (float) (trailPos.x - cameraPos.x);
                float ry = (float) (trailPos.y - cameraPos.y);
                float rz = (float) (trailPos.z - cameraPos.z);

                drawBillboardQuad(matrices, vc, rx, ry, rz, cameraRot, scale * 2.0f, color);
            }

            // Render Head Orb
            if (!orbitTrails[i].isEmpty()) {
                Vec3d headPos = orbitTrails[i].get(0);
                float headSize = 0.35f * fadeAlpha;
                int headAlpha = (int) (120.0f * fadeAlpha);
                int headColor = withAlpha(baseRgb, headAlpha);

                float rx = (float) (headPos.x - cameraPos.x);
                float ry = (float) (headPos.y - cameraPos.y);
                float rz = (float) (headPos.z - cameraPos.z);

                drawBillboardQuad(matrices, vc, rx, ry, rz, cameraRot, headSize * 2.0f, headColor);
            }
        }

        immediate.draw(glowLayer);
    }

    public static void renderCrystals(
        VertexConsumerProvider.Immediate immediate,
        MatrixStack matrices,
        Quaternionf cameraRot,
        float entityWidth,
        float entityHeight,
        float fadeAlpha,
        float crystalMoving,
        int baseRgb
    ) {
        float width = entityWidth * 1.5f;

        int r = (baseRgb >> 16) & 0xFF;
        int g = (baseRgb >> 8) & 0xFF;
        int b = baseRgb & 0xFF;

        int crystalAlpha = Math.min(255, (int) (fadeAlpha * 255.0f));
        int cTop = crystalAlpha << 24 | Math.min(255, r + 60) << 16 | Math.min(255, g + 60) << 8 | Math.min(255, b + 60);
        int cSide1 = crystalAlpha << 24 | Math.min(255, r + 30) << 16 | Math.min(255, g + 30) << 8 | Math.min(255, b + 30);
        int cSide2 = crystalAlpha << 24 | r << 16 | g << 8 | b;
        int cBot = crystalAlpha << 24 | Math.max(0, r - 30) << 16 | Math.max(0, g - 30) << 8 | Math.max(0, b - 30);

        float f = 0.075f;
        float f2 = 0.2f;

        // Draw 3D crystals geometry using dragonRays
        VertexConsumer crystalConsumer = immediate.getBuffer(RenderLayers.dragonRays());

        float[] temp0 = new float[]{f, 0.0f, -0.075f, 0.0f};
        float[] temp1 = new float[]{0.0f, f, 0.0f, -0.075f};

        for (int crystalStep = 0; crystalStep < 360; crystalStep += 19) {
            float val = 1.2f - 0.5f * fadeAlpha;
            float angleDeg = (float) crystalStep + crystalMoving * 0.3f;
            float angleRad = (float) Math.toRadians((double) angleDeg);
            float sin = (float) (Math.sin((double) angleRad) * (double) width * (double) val);
            float cos = (float) (Math.cos((double) angleRad) * (double) width * (double) val);
            float heightPrc = (float) crystalStep / 20.0f * 0.6180339f % 1.0f;
            float crystalY = entityHeight * heightPrc;

            Vector3f dir = new Vector3f(-sin, 0.0f, -cos).normalize();
            Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0.0f, 1.0f, 0.0f), dir);

            matrices.push();
            matrices.translate(sin, crystalY, cos);
            matrices.multiply(rotation);

            MatrixStack.Entry entry = matrices.peek();

            // Top pyramid
            for (int i4 = 0; i4 < 4; i4++) {
                int next = (i4 + 1) % 4;
                int c = (i4 % 2 != 0) ? cSide1 : cTop;
                crystalConsumer.vertex(entry, 0.0f, f2, 0.0f).color(c);
                crystalConsumer.vertex(entry, temp0[i4], 0.0f, temp1[i4]).color(c);
                crystalConsumer.vertex(entry, temp0[next], 0.0f, temp1[next]).color(c);
            }

            // Bottom pyramid
            for (int i4 = 0; i4 < 4; i4++) {
                int next = (i4 + 1) % 4;
                int c = (i4 % 2 != 0) ? cSide2 : cBot;
                crystalConsumer.vertex(entry, 0.0f, -0.2f, 0.0f).color(c);
                crystalConsumer.vertex(entry, temp0[next], 0.0f, temp1[next]).color(c);
                crystalConsumer.vertex(entry, temp0[i4], 0.0f, temp1[i4]).color(c);
            }

            matrices.pop();
        }

        immediate.draw(RenderLayers.dragonRays());

        // Draw Bloom Halos around crystals
        RenderLayer glowLayer = RenderLayers.entityTranslucentEmissive(BLOOM_TEXTURE);
        VertexConsumer glowConsumer = immediate.getBuffer(glowLayer);

        for (int glowStep = 0; glowStep < 360; glowStep += 19) {
            float radDist = 1.2f - 0.5f * fadeAlpha;
            float angleRad = (float) glowStep + crystalMoving * 0.3f;
            float sinRad = (float) Math.toRadians((double) angleRad);
            float posX = (float) (Math.sin((double) sinRad) * (double) width * (double) radDist);
            float posZ = (float) (Math.cos((double) sinRad) * (double) width * (double) radDist);
            float heightPrc = (float) glowStep / 20.0f * 0.6180339f % 1.0f;
            float posY = entityHeight * heightPrc;

            float glowSize = 0.3f * fadeAlpha;
            int gAlpha = (int) (fadeAlpha * 100.0f);
            int glowColor = withAlpha(baseRgb, gAlpha);

            drawBillboardQuad(matrices, glowConsumer, posX, posY, posZ, cameraRot, glowSize, glowColor);
        }

        immediate.draw(glowLayer);
    }
}
