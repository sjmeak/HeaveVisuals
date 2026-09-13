package rtx.heave.api.modules.impl.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.RotationAxis;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.events.impl.network.PacketEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.holyworld.HolyWorldApi;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ButtonSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.world.WorldShapeRenderer;
import rtx.heave.utils.sounds.SoundManager;

public final class EventMarkers extends Module {
    private static EventMarkers instance;

    private static final Pattern COORD_XYZ_PATTERN = Pattern.compile(
        "(?:x|х)\\s*[:=]?\\s*(-?\\d+)\\D+(?:y|у)\\s*[:=]?\\s*(-?\\d+)\\D+(?:z|з)\\s*[:=]?\\s*(-?\\d+)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern COORD_XZ_PATTERN = Pattern.compile(
        "(?:x|х)\\s*[:=]?\\s*(-?\\d+)\\D+(?:z|з)\\s*[:=]?\\s*(-?\\d+)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern COORD_TRIPLE_PATTERN = Pattern.compile(
        "(-?\\d{2,6})\\s+(-?\\d{1,4})\\s+(-?\\d{2,6})"
    );
    private static final Pattern SECONDS_PATTERN = Pattern.compile(
        "(\\d+)\\s*(?:сек|sec|s)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MINUTES_PATTERN = Pattern.compile(
        "(\\d+)\\s*(?:мин|min|m)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final SeparatorSetting displaySeparator = this.register(new SeparatorSetting("Отображение"));
    private final BooleanSetting renderBaseBox = this.register(new BooleanSetting("Куб на земле", "Рисует куб на месте падения ивента.", true));
    private final BooleanSetting renderText = this.register(new BooleanSetting("Парящий текст", "Отображает название, дистанцию и таймер.", true));
    private final BooleanSetting renderBeaconLine = this.register(new BooleanSetting("Тонкий луч (линия)", "Аккуратная вертикальная линия на месте метки.", true));
    private final BooleanSetting soundNotification = this.register(new BooleanSetting("Звук при ивенте", "Воспроизводить звук при обнаружении ивента.", true));

    private final SeparatorSetting timerSeparator = this.register(new SeparatorSetting("Управление"));
    private final NumberSetting autoLifetimeMinutes = this.register(new NumberSetting("Время жизни (мин)", "Удалять метки по истечении минут.", 15.0, 1.0, 60.0, 1.0));
    private final ButtonSetting clearButton = this.register(new ButtonSetting("Очистить все метки", "Удалить все сохранённые метки.").label("Очистить").onClick(EventMarkers::clearAll));

    private final List<Marker> markers = new ArrayList<>();

    public EventMarkers() {
        super("Event Markers", "Автоматически ставит 3D-метки ивентов HolyWorld по сообщениям в чате.", Category.EVENTS);
        instance = this;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public static EventMarkers getInstance() {
        return instance;
    }

    public static void setGps(String name, Vec3d pos, Color color, long durationMs, boolean hasTimer) {
        if (instance == null) return;
        instance.markers.removeIf(m -> m.pos.squaredDistanceTo(pos) < 64.0);
        long expiresAt = System.currentTimeMillis() + (durationMs > 0 ? durationMs : (long)(instance.autoLifetimeMinutes.getValue() * 60_000.0));
        instance.markers.add(new Marker(name, pos, color != null ? color : new Color(0, 229, 255), expiresAt, hasTimer));
        if (!instance.isEnabled()) {
            instance.setEnabled(true);
        }
    }

    public static void clearAll() {
        if (instance != null) {
            instance.markers.clear();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§6[GPS] §fВсе метки сброшены."), false);
            }
        }
    }

    public static Marker getActiveMarker() {
        if (instance == null || instance.markers.isEmpty()) return null;
        return instance.markers.get(instance.markers.size() - 1);
    }

    public static List<Marker> getMarkers() {
        if (instance == null) return List.of();
        return new ArrayList<>(instance.markers);
    }

    public static double resolveY(double x, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            try {
                int topY = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                if (topY > mc.world.getBottomY() && topY < 320) {
                    return topY;
                }
            } catch (Throwable ignored) {}
            if (mc.player != null) {
                return mc.player.getY();
            }
        }
        return 70.0;
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (!event.isReceive() || this.mc.world == null || this.mc.player == null) {
            return;
        }
        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) {
            return;
        }

        String raw = packet.content().getString();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String stripped = Formatting.strip(raw).toLowerCase(Locale.ROOT);

        HolyEvent matchedEvent = matchEvent(stripped);
        Vec3d coords = findCoordinates(raw);
        if (coords == null) {
            return;
        }

        if (matchedEvent == null) {
            if (stripped.contains("ивент") || stripped.contains("груз") || stripped.contains("шахт") || stripped.contains("босс") || stripped.contains("коорд")) {
                matchedEvent = new HolyEvent("Ивент", new Color(0, 229, 255));
            } else {
                return;
            }
        }

        long lifetimeMs = (long)(this.autoLifetimeMinutes.getValue() * 60.0 * 1000.0);
        long timerMs = findTimerDurationMs(stripped);
        long expiresAt = System.currentTimeMillis() + (timerMs > 0 ? timerMs : lifetimeMs);

        this.markers.removeIf(m -> m.pos.squaredDistanceTo(coords) < 256.0);

        Marker newMarker = new Marker(matchedEvent.name, coords, matchedEvent.color, expiresAt, timerMs > 0);
        this.markers.add(newMarker);

        HolyWorldApi.getInstance().notifyLiveChatEvent(matchedEvent.name, coords, timerMs);

        if (this.soundNotification.getValue() && this.mc.getSoundManager() != null) {
            SoundManager.playSound(SoundManager.NOTIFICATION, 1.0f, 1.0f);
        }

        String distanceStr = String.format(Locale.ROOT, "%.0fм", this.mc.player.getEntityPos().distanceTo(coords));
        MutableText msg = Text.literal("§6[GPS] §fМетка ивента §e" + matchedEvent.name + "§f на §b" + (int)coords.x + " " + (int)coords.z + " §7(" + distanceStr + ") ")
            .append(Text.literal("§8[§c✕ Сбросить§8]")
                .styled(style -> style.withClickEvent(new ClickEvent.RunCommand(".gps off"))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Нажмите, чтобы отключить метку (.gps off)")))));
        this.mc.player.sendMessage(msg, false);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!event.isPre()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.markers.removeIf(m -> now >= m.expiresAt);
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (this.markers.isEmpty() || this.mc.player == null || this.mc.world == null) {
            return;
        }

        Camera camera = event.getCamera() != null ? event.getCamera() : this.mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        MatrixStack matrixStack = event.getStack();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        TextRenderer textRenderer = this.mc.textRenderer;
        long now = System.currentTimeMillis();

        for (Marker marker : this.markers) {
            int argb = marker.color.getRGB();
            int fill = ColorUtil.multAlpha(argb, 0.25f);
            int outline = ColorUtil.multAlpha(argb, 0.90f);
            double dist = this.mc.player.getEntityPos().distanceTo(marker.pos);

            // 1. Compact 3D Ground Box & Sleek Beacon Line (when within 512 blocks)
            if (dist < 512.0) {
                if (this.renderBaseBox.getValue()) {
                    Box baseBox = new Box(
                        marker.pos.x - 0.6, marker.pos.y, marker.pos.z - 0.6,
                        marker.pos.x + 0.6, marker.pos.y + 1.0, marker.pos.z + 0.6
                    );
                    WorldShapeRenderer.boxes(immediate, matrixStack, cameraPos, List.of(baseBox), fill, outline, 1.8f);
                }

                // Sleek thin vertical laser/line from ground up
                if (this.renderBeaconLine.getValue()) {
                    VertexConsumer lineConsumer = immediate.getBuffer(RenderLayers.lines());
                    MatrixStack.Entry entry = matrixStack.peek();
                    float lx = (float)(marker.pos.x - cameraPos.x);
                    float lz = (float)(marker.pos.z - cameraPos.z);
                    float y1 = (float)(marker.pos.y - cameraPos.y);
                    float y2 = (float)(Math.min(320.0, marker.pos.y + 50.0) - cameraPos.y);
                    lineConsumer.vertex(entry, lx, y1, lz).color(outline).normal(entry, 0.0f, 1.0f, 0.0f).lineWidth(2.0f);
                    lineConsumer.vertex(entry, lx, y2, lz).color(outline).normal(entry, 0.0f, 1.0f, 0.0f).lineWidth(2.0f);
                    immediate.draw(RenderLayers.lines());
                }
            }

            // 2. Floating 3D Billboard Text (SEE_THROUGH layer for full visibility)
            if (this.renderText.getValue() && textRenderer != null) {
                Vec3d diff = marker.pos.subtract(cameraPos);
                Vec3d dir = diff.lengthSquared() > 1e-4 ? diff.normalize() : new Vec3d(0, 1, 0);

                // Virtual projection: if target is farther than 35m, place billboard 35m away along ray
                // so it is NEVER culled by fog, far plane, or render distance!
                Vec3d renderPos;
                float scale;
                if (dist > 35.0) {
                    renderPos = cameraPos.add(dir.multiply(35.0));
                    scale = 0.040f;
                } else {
                    renderPos = new Vec3d(marker.pos.x, marker.pos.y + 1.6, marker.pos.z);
                    scale = (float) Math.max(0.022, dist * 0.0022);
                }

                matrixStack.push();
                matrixStack.translate(
                    (float)(renderPos.x - cameraPos.x),
                    (float)(renderPos.y - cameraPos.y),
                    (float)(renderPos.z - cameraPos.z)
                );
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrixStack.scale(-scale, -scale, scale);

                Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

                String line1 = "§l✦ " + marker.name;
                String distStr = (dist >= 1000.0)
                    ? String.format(Locale.ROOT, "%.1fкм", dist / 1000.0)
                    : String.format(Locale.ROOT, "%.0fм", dist);

                String line2;
                long leftMs = marker.expiresAt - now;
                if (marker.hasTimer && leftMs > 0) {
                    long totalSec = leftMs / 1000L;
                    long min = totalSec / 60L;
                    long sec = totalSec % 60L;
                    line2 = String.format(Locale.ROOT, "§b%s §7| §e%02d:%02d", distStr, min, sec);
                } else {
                    line2 = "§b" + distStr;
                }

                int w1 = textRenderer.getWidth(line1);
                int w2 = textRenderer.getWidth(line2);

                textRenderer.draw(line1, -w1 / 2.0f, -10.0f, argb, true, matrix4f, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0x90000000, 0xF000F0);
                textRenderer.draw(line2, -w2 / 2.0f, 2.0f, 0xFFFFFFFF, true, matrix4f, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0x90000000, 0xF000F0);

                matrixStack.pop();
            }
        }
        // FLUSH IMMEDIATE BUFFER! Essential for text and world elements to render!
        immediate.draw();
    }

    private static HolyEvent matchEvent(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        if (s.contains("захват замка") || s.contains("замок") || s.contains("castle")) {
            return new HolyEvent("Замок", new Color(255, 60, 60));
        }
        if (s.contains("ценный груз") || s.contains("груз") || s.contains("посылк") || s.contains("cargo")) {
            return new HolyEvent("Ценный груз", new Color(0, 229, 255));
        }
        if (s.contains("золотая лихорадка") || s.contains("лихорадк") || s.contains("золот") || s.contains("крепост")) {
            return new HolyEvent("Золотая лихорадка", new Color(255, 215, 0));
        }
        if (s.contains("цветочная поляна") || s.contains("полян")) {
            return new HolyEvent("Цветочная поляна", new Color(74, 222, 128));
        }
        if (s.contains("снежная шахта") || s.contains("снежн")) {
            return new HolyEvent("Снежная шахта", new Color(56, 189, 248));
        }
        if (s.contains("смертельная шахта") || s.contains("адская шахта") || s.contains("шахта") || s.contains("шахт")) {
            return new HolyEvent("Шахта", new Color(239, 68, 68));
        }
        if (s.contains("игральный куб") || s.contains("кубик") || s.contains("куб")) {
            return new HolyEvent("Кубик", new Color(168, 85, 247));
        }
        if (s.contains("мистический босс") || s.contains("босс") || s.contains("boss") || s.contains("warp pvp") || s.contains("варп пвп")) {
            return new HolyEvent("Босс", new Color(147, 51, 234));
        }
        if (s.contains("опытное поле") || s.contains("опытное") || s.contains("поле")) {
            return new HolyEvent("Опытное поле", new Color(255, 140, 0));
        }
        if (s.contains("таинственный корабль") || s.contains("корабль")) {
            return new HolyEvent("Таинственный корабль", new Color(60, 180, 220));
        }
        return null;
    }

    private static Vec3d findCoordinates(String text) {
        // 1. Try XYZ pattern first: x: 1234, y: 65, z: -567
        Matcher xyz = COORD_XYZ_PATTERN.matcher(text);
        if (xyz.find()) {
            try {
                double x = Double.parseDouble(xyz.group(1));
                double y = Double.parseDouble(xyz.group(2));
                double z = Double.parseDouble(xyz.group(3));
                return new Vec3d(x, y, z);
            } catch (NumberFormatException ignored) {}
        }
        // 2. Try XZ pattern: x: 1234, z: -567 (without Y)
        Matcher xz = COORD_XZ_PATTERN.matcher(text);
        if (xz.find()) {
            try {
                double x = Double.parseDouble(xz.group(1));
                double z = Double.parseDouble(xz.group(2));
                double y = resolveY(x, z);
                return new Vec3d(x, y, z);
            } catch (NumberFormatException ignored) {}
        }
        // 3. Try triple integers: 1234 65 -567
        Matcher triple = COORD_TRIPLE_PATTERN.matcher(text);
        if (triple.find()) {
            try {
                double x = Double.parseDouble(triple.group(1));
                double y = Double.parseDouble(triple.group(2));
                double z = Double.parseDouble(triple.group(3));
                return new Vec3d(x, y, z);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static long findTimerDurationMs(String text) {
        Matcher mins = MINUTES_PATTERN.matcher(text);
        if (mins.find()) {
            try {
                return Long.parseLong(mins.group(1)) * 60_000L;
            } catch (NumberFormatException ignored) {}
        }
        Matcher secs = SECONDS_PATTERN.matcher(text);
        if (secs.find()) {
            try {
                return Long.parseLong(secs.group(1)) * 1_000L;
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    private record HolyEvent(String name, Color color) {}

    public static final class Marker {
        public final String name;
        public final Vec3d pos;
        public final Color color;
        public final long expiresAt;
        public final boolean hasTimer;

        public Marker(String name, Vec3d pos, Color color, long expiresAt, boolean hasTimer) {
            this.name = name;
            this.pos = pos;
            this.color = color;
            this.expiresAt = expiresAt;
            this.hasTimer = hasTimer;
        }

        public String name() { return name; }
        public Vec3d pos() { return pos; }
        public Color color() { return color; }
        public long expiresAt() { return expiresAt; }
        public boolean hasTimer() { return hasTimer; }
    }
}
