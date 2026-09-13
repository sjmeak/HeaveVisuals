package rtx.heave.api.drags.hud;
import rtx.heave.api.events.EventHandler;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import rtx.heave.api.drags.Position;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.render.HudRenderEvent;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.InfoModule;
import rtx.heave.api.modules.impl.Utils.StreamerMode;
import rtx.heave.utils.network.Network;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public final class InfoHud {
    private static final String FONT = Fonts.SF.id();
    private static final float SIZE = 7.0f;
    private static final float MARGIN = 5.0f;
    private static final float LINE_GAP = 3.0f;
    private static final int LABEL_COLOR = -3618608;
    private static final int LEFT_VALUE_COLOR = -11665561;
    private static final int RIGHT_VALUE_COLOR = -45747;
    private static final int OUTLINE_COLOR = -16777216;
    private static final float OUTLINE_OFFSET = 0.2f;
    private static final float[][] OUTLINE_DIRS = new float[][]{{-1.0f, -1.0f}, {0.0f, -1.0f}, {1.0f, -1.0f}, {-1.0f, 0.0f}, {1.0f, 0.0f}, {-1.0f, 1.0f}, {0.0f, 1.0f}, {1.0f, 1.0f}};

    public InfoHud() {
        EventBus.get().subscribe(this);
    }

    private static float width(InfoHud.Seg[] segArray) {
        float f = 0.0f;
        for (InfoHud.Seg seg : segArray) {
            f += Render2D.msdfWidth(FONT, seg.text, 7.0f);
        }
        return f;
    }

    public static int ping(MinecraftClient minecraftClient, PlayerEntity playerEntity) {
        if (minecraftClient.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry playerListEntry = minecraftClient.getNetworkHandler().getPlayerListEntry(playerEntity.getUuid());
        return playerListEntry == null ? 0 : Math.max(0, playerListEntry.getLatency());
    }

    @EventHandler
    private void onHud(HudRenderEvent hudRenderEvent) {
        InfoModule infoModule = ModuleManager.get().get(InfoModule.class);
        if (infoModule == null || !infoModule.isEnabled()) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
        if (clientPlayerEntity == null || minecraftClient.world == null) {
            return;
        }
        float f = Position.screenWidth();
        float f2 = Position.screenHeight();
        float lineH = 10.0f;
        float baseY = f2 - 5.0f - 7.0f;
        int labelColor = infoModule.labelColor.getColor();
        int valueColor = infoModule.valueColor.getColor();

        DrawContext drawContext = hudRenderEvent.getGraphics();
        Render2D.beginFrame(drawContext);

        // Left stack (bottom up)
        float curLeftY = baseY;
        if (infoModule.showXyz.getValue()) {
            InfoHud.Seg[] xyzSegs;
            if (infoModule.showNether.getValue()) {
                xyzSegs = new InfoHud.Seg[]{
                    new InfoHud.Seg("XYZ ", labelColor),
                    new InfoHud.Seg(InfoHud.coords(clientPlayerEntity.getX(), clientPlayerEntity.getY(), clientPlayerEntity.getZ()), valueColor),
                    new InfoHud.Seg("  " + InfoHud.otherDim(minecraftClient, (PlayerEntity)clientPlayerEntity), labelColor)
                };
            } else {
                xyzSegs = new InfoHud.Seg[]{
                    new InfoHud.Seg("XYZ ", labelColor),
                    new InfoHud.Seg(InfoHud.coords(clientPlayerEntity.getX(), clientPlayerEntity.getY(), clientPlayerEntity.getZ()), valueColor)
                };
            }
            InfoHud.drawSegs(xyzSegs, 5.0f, curLeftY);
            curLeftY -= lineH;
        }
        if (infoModule.showBps.getValue()) {
            InfoHud.Seg[] bpsSegs = new InfoHud.Seg[]{
                new InfoHud.Seg("BPS ", labelColor),
                new InfoHud.Seg(InfoHud.bps((PlayerEntity)clientPlayerEntity), valueColor)
            };
            InfoHud.drawSegs(bpsSegs, 5.0f, curLeftY);
            curLeftY -= lineH;
        }

        // Right stack (bottom up)
        float curRightY = baseY;
        if (infoModule.showPing.getValue()) {
            InfoHud.Seg[] pingSegs = new InfoHud.Seg[]{
                new InfoHud.Seg("Ping ", labelColor),
                new InfoHud.Seg(InfoHud.ping(minecraftClient, (PlayerEntity)clientPlayerEntity) + "ms", valueColor)
            };
            InfoHud.drawSegs(pingSegs, f - 5.0f - InfoHud.width(pingSegs), curRightY);
            curRightY -= lineH;
        }
        if (infoModule.showTps.getValue()) {
            InfoHud.Seg[] tpsSegs = new InfoHud.Seg[]{
                new InfoHud.Seg("TPS ", labelColor),
                new InfoHud.Seg(String.format(Locale.ROOT, "%.1f", Float.valueOf(Network.getTPS())), valueColor)
            };
            InfoHud.drawSegs(tpsSegs, f - 5.0f - InfoHud.width(tpsSegs), curRightY);
            curRightY -= lineH;
        }
        if (infoModule.showFps.getValue()) {
            InfoHud.Seg[] fpsSegs = new InfoHud.Seg[]{
                new InfoHud.Seg("FPS ", labelColor),
                new InfoHud.Seg(Integer.toString(minecraftClient.getCurrentFps()), valueColor)
            };
            InfoHud.drawSegs(fpsSegs, f - 5.0f - InfoHud.width(fpsSegs), curRightY);
            curRightY -= lineH;
        }

        Render2D.flush();
    }

    private static String otherDim(MinecraftClient minecraftClient, PlayerEntity playerEntity) {
        if (StreamerMode.hideCoords()) {
            return "[#, #, #]";
        }
        boolean bl = minecraftClient.world.getDimension().coordinateScale() > 1.0;
        double d = bl ? 8.0 : 0.125;
        int n = (int)Math.floor(playerEntity.getX() * d);
        int n2 = (int)Math.floor(playerEntity.getZ() * d);
        return "[" + n + ", " + (int)Math.floor(playerEntity.getY()) + ", " + n2 + "]";
    }

    private static void drawSegs(InfoHud.Seg[] segArray, float f, float f2) {
        for (float[] fArray : OUTLINE_DIRS) {
            float f3 = f;
            for (InfoHud.Seg seg : segArray) {
                Render2D.msdfText(FONT, seg.text, f3 + fArray[0] * 0.2f, f2 + fArray[1] * 0.2f, 7.0f, -16777216);
                f3 += Render2D.msdfWidth(FONT, seg.text, 7.0f);
            }
        }
        float f4 = f;
        for (InfoHud.Seg seg : segArray) {
            Render2D.msdfText(FONT, seg.text, f4, f2, 7.0f, seg.color);
            f4 += Render2D.msdfWidth(FONT, seg.text, 7.0f);
        }
    }

    private static String bps(PlayerEntity playerEntity) {
        double d = playerEntity.getX() - playerEntity.lastRenderX;
        double d2 = playerEntity.getZ() - playerEntity.lastRenderZ;
        double d3 = Math.sqrt(d * d + d2 * d2) * 20.0;
        return String.format(Locale.ROOT, "%.2f", d3);
    }

    private static String coords(double d, double d2, double d3) {
        if (StreamerMode.hideCoords()) {
            return "#, #, #";
        }
        return (int)Math.floor(d) + ", " + (int)Math.floor(d2) + ", " + (int)Math.floor(d3);
    }

    public static float reservedRightHeight() {
        return 36.0f;
    }


    public record Seg(String text, int color) {
}
}

