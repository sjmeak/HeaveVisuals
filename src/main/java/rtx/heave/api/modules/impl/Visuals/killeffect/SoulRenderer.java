package rtx.heave.api.modules.impl.Visuals.killeffect;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.events.impl.render.WorldRenderEvent;

public final class SoulRenderer {
    private static final long DURATION_MS = 1600L;
    private static final float RISE_HEIGHT = 2.0f;
    private static final float START_ALPHA = 0.7f;
    private static final List<Soul> souls = new ArrayList<>();

    private static PlayerEntityModel defaultModel;
    private static PlayerEntityModel slimModel;

    public static final class Soul {
        private final Vec3d startPos;
        private final long startAt;
        private final Identifier skinTexture;
        private final boolean isSlim;
        private final float baseYaw;

        public Soul(Vec3d startPos, long startAt, Identifier skinTexture, boolean isSlim, float baseYaw) {
            this.startPos = startPos;
            this.startAt = startAt;
            this.skinTexture = skinTexture;
            this.isSlim = isSlim;
            this.baseYaw = baseYaw;
        }

        public Vec3d getStartPos() { return this.startPos; }
        public long getStartAt() { return this.startAt; }
        public Identifier getSkinTexture() { return this.skinTexture; }
        public boolean isSlim() { return this.isSlim; }
        public float getBaseYaw() { return this.baseYaw; }
    }

    private static void ensureModels() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (defaultModel == null && mc.getLoadedEntityModels() != null) {
            try {
                defaultModel = new PlayerEntityModel(mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
                slimModel = new PlayerEntityModel(mc.getLoadedEntityModels().getModelPart(new net.minecraft.client.render.entity.model.EntityModelLayer(net.minecraft.util.Identifier.of("minecraft", "player_slim"), "main")), true);
            } catch (Throwable ignored) {}
        }
    }

    public static void spawnSoul(PlayerEntity target) {
        if (target == null) return;
        long now = System.currentTimeMillis();
        Identifier skinTexture = null;
        boolean isSlim = false;

        if (target instanceof AbstractClientPlayerEntity clientPlayer) {
            SkinTextures skin = clientPlayer.getSkin();
            if (skin != null) {
                if (skin.body() != null) {
                    skinTexture = skin.body().texturePath();
                }
                isSlim = skin.model() == PlayerSkinType.SLIM;
            }
        }

        if (skinTexture == null) {
            skinTexture = DefaultSkinHelper.getTexture();
        }

        souls.add(new Soul(new Vec3d(target.getX(), target.getY() + 0.1, target.getZ()), now, skinTexture, isSlim, target.getYaw()));
    }

    public static void spawnSoulAt(Vec3d pos, float yaw, Identifier skinTexture, boolean isSlim) {
        if (skinTexture == null) {
            skinTexture = DefaultSkinHelper.getTexture();
        }
        souls.add(new Soul(pos, System.currentTimeMillis(), skinTexture, isSlim, yaw));
    }

    public static void render(WorldRenderEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || souls.isEmpty() || event.getCamera() == null) {
            return;
        }
        ensureModels();
        if (defaultModel == null) {
            return;
        }

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cameraPos = event.getCamera().getCameraPos();
        long now = System.currentTimeMillis();

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._disableCull();

        Iterator<Soul> it = souls.iterator();
        while (it.hasNext()) {
            Soul soul = it.next();
            float progress = Math.min(1.0f, (float)(now - soul.getStartAt()) / (float)DURATION_MS);
            if (progress >= 1.0f) {
                it.remove();
                continue;
            }

            float ease = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
            float alpha = Math.max(0.0f, Math.min(1.0f, START_ALPHA * (1.0f - progress)));

            double x = soul.getStartPos().x;
            double y = soul.getStartPos().y + (double)(ease * RISE_HEIGHT);
            double z = soul.getStartPos().z;
            float yaw = soul.getBaseYaw() + 360.0f * ease;

            PlayerEntityModel model = soul.isSlim() && slimModel != null ? slimModel : defaultModel;
            Identifier texture = soul.getSkinTexture() != null ? soul.getSkinTexture() : DefaultSkinHelper.getTexture();
            RenderLayer layer = RenderLayers.entityTranslucent(texture);
            VertexConsumer consumer = immediate.getBuffer(layer);

            int alphaInt = Math.round(alpha * 255.0f);
            int color = (alphaInt << 24) | 0x00FFFFFF;

            // Calm floating pose
            try {
                model.head.setAngles(0, 0, 0);
                model.rightArm.setAngles(-0.2f, 0, 0.1f);
                model.leftArm.setAngles(-0.2f, 0, -0.1f);
                model.rightLeg.setAngles(0, 0, 0.05f);
                model.leftLeg.setAngles(0, 0, -0.05f);
            } catch (Throwable ignored) {}

            event.getStack().push();
            event.getStack().translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
            event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
            event.getStack().scale(-1.0f, -1.0f, 1.0f);
            event.getStack().translate(0.0f, -1.5f, 0.0f);

            model.render(event.getStack(), consumer, 0xF000F0, OverlayTexture.DEFAULT_UV, color);
            event.getStack().pop();
        }
        immediate.draw();
        GlStateManager._enableCull();
        GlStateManager._disableBlend();
    }

    public static void clear() {
        souls.clear();
    }
}
