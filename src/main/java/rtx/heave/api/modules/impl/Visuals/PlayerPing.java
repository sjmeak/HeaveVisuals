package rtx.heave.api.modules.impl.Visuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;

public final class PlayerPing extends Module {
    public PlayerPing() {
        super("Player Ping", "Отображает пинг игроков в 3D над их никнеймами.", Category.VISUALS);
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (!this.isEnabled() || this.mc.world == null || this.mc.player == null) {
            return;
        }
        Camera camera = event.getCamera();
        if (camera == null) return;
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = event.getStack();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();

        for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (player == this.mc.player && this.mc.options.getPerspective().isFirstPerson()) continue;
            if (!player.isAlive() || player.isInvisible()) continue;

            double distSq = player.squaredDistanceTo(camPos);
            if (distSq > 4096.0) continue; // 64 blocks

            int ping = this.getPing(player);
            if (ping < 0) continue;

            String pingText = ping + " ms";
            int color = this.getPingColor(ping);

            double x = player.getX() - camPos.x;
            double y = player.getY() + player.getHeight() + 0.55 - camPos.y;
            double z = player.getZ() - camPos.z;

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            float scale = 0.025f;
            matrices.scale(-scale, -scale, scale);

            TextRenderer tr = this.mc.textRenderer;
            float textWidth = tr.getWidth(pingText);
            tr.draw(
                Text.literal(pingText),
                -textWidth / 2.0f,
                0.0f,
                color,
                true,
                matrices.peek().getPositionMatrix(),
                immediate,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0x80000000,
                0xF000F0
            );
            matrices.pop();
        }
        immediate.draw();
    }

    private int getPing(PlayerEntity player) {
        if (this.mc.getNetworkHandler() == null) return -1;
        PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null ? entry.getLatency() : -1;
    }

    private int getPingColor(int ping) {
        if (ping < 60) return 0xFF55FF55; // Green
        if (ping < 130) return 0xFFFFFF55; // Yellow
        if (ping < 220) return 0xFFFFAA00; // Orange
        return 0xFFFF5555; // Red
    }
}
