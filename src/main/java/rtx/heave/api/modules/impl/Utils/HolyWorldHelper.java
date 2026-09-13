package rtx.heave.api.modules.impl.Utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.pipeline.ClientPipelines;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HolyWorldHelper extends Module {
    private static HolyWorldHelper instance;

    private static final int MAX_ACTIVE_ZONES = 16;
    private static final int[][] BOX_EDGES = new int[][]{
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private final SeparatorSetting displaySeparator = this.register(new SeparatorSetting("Отображение"));
    public final BooleanSetting heldPreview = this.register(
        new BooleanSetting("Предпросмотр", "Показывает будущую зону, пока предмет находится в руке.", true)
    );
    public final NumberSetting previewOpacity = this.register(
        new NumberSetting("Прозрачность предпросмотра", "Яркость зоны, пока предмет находится в руке.", 0.5, 0.05, 1.0, 0.05)
            .visibleWhen(this.heldPreview::getValue)
    );
    public final BooleanSetting snowballTrajectory = this.register(
        new BooleanSetting("Траектория снежка", "Рассчитывать траекторию и место падения снежка.", true)
    );

    private final SeparatorSetting timerSeparator = this.register(new SeparatorSetting("Таймеры"));
    public final BooleanSetting actionbarOverlay = this.register(
        new BooleanSetting("Вывод в Actionbar", "Показывать оставшиеся секунды по центру экрана над хотбаром.", true)
    );

    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвета"));
    public final ColorSetting lineColor = this.register(
        new ColorSetting("Основной цвет", "Цвет линий зоны.", new Color(255, 181, 72, 235))
    );
    public final ColorSetting playerInZoneColor = this.register(
        new ColorSetting("Цвет при игроке", "Цвет линий зоны, когда внутри находится другой игрок.", new Color(235, 65, 65, 255))
    );

    private final List<ActiveZone> activeZones = new ArrayList<>();
    private final List<Vec3d> simulationPoints = new ArrayList<>(161);
    private Map<PyrotechnicItemType, Integer> previousCounts = new EnumMap<>(PyrotechnicItemType.class);
    private PendingActivation pendingActivation = null;
    private ClientWorld trackedLevel = null;
    private Vec3d snowLanding = null;
    private int tickCounter = 0;
    private boolean wasDropKeyDown = false;
    private int dropGraceTicks = 0;
    private long lastFrameNanos = 0L;
    private float frameDt = 0.016f;
    private float previewOccupiedBlend = 0.0f;

    public HolyWorldHelper() {
        super("HolyWorld Helper", "Показывает зоны и таймеры HolyWorld-предметов (стан, трапка, взрывная трапка).", Category.UTILS);
        instance = this;
    }

    public static HolyWorldHelper getInstance() {
        return instance;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    protected void onEnable() {
        if (this.mc.world != null) {
            this.trackedLevel = this.mc.world;
            if (this.mc.player != null) {
                this.previousCounts = countItemsInInventory(this.mc.player);
            }
        }
    }

    @Override
    protected void onDisable() {
        this.clearState();
        this.trackedLevel = null;
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!event.isPre()) {
            return;
        }

        MinecraftClient client = this.mc;
        if (client.player == null || client.world == null) {
            this.clearState();
            this.trackedLevel = null;
            return;
        }

        if (this.trackedLevel != client.world) {
            this.clearState();
            this.trackedLevel = client.world;
            this.previousCounts = countItemsInInventory(client.player);
            return;
        }

        this.tickCounter++;

        boolean dropDown = client.options.dropKey.isPressed();
        if (dropDown && !this.wasDropKeyDown) {
            this.dropGraceTicks = 8;
        }
        this.wasDropKeyDown = dropDown;
        if (this.dropGraceTicks > 0) {
            this.dropGraceTicks--;
        }

        this.processPendingActivation(client);

        if (this.tickCounter % 2 == 0) {
            this.updateInventoryTracking(client);
        }

        if (this.tickCounter % 4 == 0) {
            this.cleanExpiredZones();
        }

        if (this.actionbarOverlay.getValue() && client.inGameHud != null) {
            this.updateActionbar(client);
        }
    }

    private void processPendingActivation(MinecraftClient client) {
        if (this.pendingActivation != null) {
            if (--this.pendingActivation.delayTicks <= 0) {
                if (client.currentScreen == null) {
                    if (this.activeZones.size() >= MAX_ACTIVE_ZONES) {
                        this.activeZones.remove(0);
                    }
                    this.activeZones.add(new ActiveZone(
                        this.pendingActivation.type,
                        this.pendingActivation.position,
                        this.pendingActivation.detectedTime
                    ));
                }
                this.pendingActivation = null;
            }
        }
    }

    private void updateInventoryTracking(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        Map<PyrotechnicItemType, Integer> currentCounts = countItemsInInventory(player);

        for (Map.Entry<PyrotechnicItemType, Integer> entry : this.previousCounts.entrySet()) {
            PyrotechnicItemType type = entry.getKey();
            if (type == PyrotechnicItemType.SNOWBALL) {
                continue;
            }
            int previous = entry.getValue();
            int current = currentCounts.getOrDefault(type, 0);
            if (current < previous) {
                int consumed = previous - current;
                if (consumed == 1 && this.pendingActivation == null && client.currentScreen == null && this.dropGraceTicks == 0) {
                    this.pendingActivation = new PendingActivation(type, player.getEntityPos());
                }
            }
        }

        this.previousCounts = currentCounts;
    }

    private static Map<PyrotechnicItemType, Integer> countItemsInInventory(PlayerEntity player) {
        Map<PyrotechnicItemType, Integer> counts = new EnumMap<>(PyrotechnicItemType.class);
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            PyrotechnicItemType type = PyrotechnicItemType.fromStack(stack);
            if (type != null) {
                counts.merge(type, stack.getCount(), Integer::sum);
            }
        }

        ItemStack offhand = player.getOffHandStack();
        PyrotechnicItemType offhandType = PyrotechnicItemType.fromStack(offhand);
        if (offhandType != null) {
            counts.merge(offhandType, offhand.getCount(), Integer::sum);
        }

        return counts;
    }

    private void cleanExpiredZones() {
        Iterator<ActiveZone> it = this.activeZones.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired()) {
                it.remove();
            }
        }
    }

    private void updateActionbar(MinecraftClient client) {
        StringBuilder sb = new StringBuilder();

        for (ActiveZone zone : this.activeZones) {
            double remaining = zone.getRemainingSeconds();
            if (remaining > 0.0) {
                if (!sb.isEmpty()) {
                    sb.append(" §7| ");
                }
                sb.append("§e").append(zone.type.displayName).append(": §f")
                  .append(String.format(Locale.ROOT, "%.1fs", remaining));
            }
        }

        PyrotechnicItemType held = this.getHeldType(client);
        if (held != null && held != PyrotechnicItemType.SNOWBALL && sb.isEmpty()) {
            sb.append("§b").append(held.displayName).append(" §7(§aГотов§7, ").append(String.format(Locale.ROOT, "%.0fs", held.durationMs / 1000.0)).append(")");
        }

        if (!sb.isEmpty()) {
            client.inGameHud.setOverlayMessage(Text.literal(sb.toString()), false);
        }
    }

    public boolean hasAnyPlayerInZone(Box zoneBox) {
        MinecraftClient client = this.mc;
        if (client.player == null || client.world == null) {
            return false;
        }

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator()) {
                continue;
            }
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !hasVisibleArmor(player)) {
                continue;
            }
            if (zoneBox.intersects(player.getBoundingBox())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasVisibleArmor(PlayerEntity player) {
        return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    public PyrotechnicItemType getHeldType(MinecraftClient client) {
        if (client.player == null) {
            return null;
        }
        PyrotechnicItemType main = PyrotechnicItemType.fromStack(client.player.getMainHandStack());
        if (main != null) {
            return main;
        }
        return PyrotechnicItemType.fromStack(client.player.getOffHandStack());
    }

    public PyrotechnicItemType getHeldType() {
        return this.getHeldType(this.mc);
    }

    public List<ActiveZone> getActiveZones() {
        return this.activeZones;
    }

    public void clearState() {
        this.activeZones.clear();
        this.simulationPoints.clear();
        this.previousCounts.clear();
        this.pendingActivation = null;
        this.snowLanding = null;
        this.dropGraceTicks = 0;
        this.wasDropKeyDown = false;
        this.lastFrameNanos = 0L;
        this.previewOccupiedBlend = 0.0f;
    }

    private float approach(float current, float target) {
        return current + (target - current) * Math.min(1.0f, this.frameDt * 6.0f);
    }

    private int blendOccupied(int normalColor, float blend) {
        if (blend <= 0.001f) {
            return normalColor;
        }
        if (blend >= 0.999f) {
            return this.playerInZoneColor.getColor();
        }
        return ColorUtil.lerpColor(normalColor, this.playerInZoneColor.getColor(), blend);
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        MinecraftClient client = this.mc;
        if (client.player == null || client.world == null) {
            return;
        }

        Camera camera = event.getCamera() != null ? event.getCamera() : client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = event.getStack();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = immediate.getBuffer(net.minecraft.client.render.RenderLayers.lines());
        MatrixStack.Entry entry = matrices.peek();

        long nowNanos = System.nanoTime();
        this.frameDt = this.lastFrameNanos == 0L ? 0.016f : Math.min((float)(nowNanos - this.lastFrameNanos) * 1.0E-9f, 0.1f);
        this.lastFrameNanos = nowNanos;

        int defaultColor = this.lineColor.getValue();

        if (this.heldPreview.getValue()) {
            PyrotechnicItemType held = this.getHeldType(client);
            if (held != null) {
                if (held == PyrotechnicItemType.SNOWBALL && this.snowballTrajectory.getValue()) {
                    this.renderSnowPrediction(consumer, entry, camPos, defaultColor, event.getPartialTicks());
                } else if (held != PyrotechnicItemType.SNOWBALL) {
                    Vec3d playerPos = client.player.getEntityPos();
                    double originX = held.blockAligned ? Math.floor(playerPos.x) : playerPos.x;
                    double originY = held.blockAligned ? Math.floor(playerPos.y) : playerPos.y;
                    double originZ = held.blockAligned ? Math.floor(playerPos.z) : playerPos.z;

                    Box renderBox = held.getRenderBoundingBox(originX, originY, originZ);
                    Box hitBox = held.getHitboxBoundingBox(originX, originY, originZ);

                    boolean hasPlayer = this.hasAnyPlayerInZone(hitBox);
                    this.previewOccupiedBlend = this.approach(this.previewOccupiedBlend, hasPlayer ? 1.0f : 0.0f);
                    int blended = this.blendOccupied(defaultColor, this.previewOccupiedBlend);
                    int previewColor = ColorUtil.multAlpha(blended, this.previewOpacity.getFloat());

                    this.drawBoxOutline(consumer, entry, camPos, renderBox, previewColor);
                }
            } else {
                this.previewOccupiedBlend = 0.0f;
            }
        }

        for (ActiveZone zone : this.activeZones) {
            double originX = zone.type.blockAligned ? Math.floor(zone.position.x) : zone.position.x;
            double originY = zone.type.blockAligned ? Math.floor(zone.position.y) : zone.position.y;
            double originZ = zone.type.blockAligned ? Math.floor(zone.position.z) : zone.position.z;

            Box renderBox = zone.type.getRenderBoundingBox(originX, originY, originZ);
            Box hitBox = zone.type.getHitboxBoundingBox(originX, originY, originZ);

            boolean hasPlayer = this.hasAnyPlayerInZone(hitBox);
            zone.occupiedBlend = this.approach(zone.occupiedBlend, hasPlayer ? 1.0f : 0.0f);
            int blended = this.blendOccupied(defaultColor, zone.occupiedBlend);

            this.drawBoxOutline(consumer, entry, camPos, renderBox, blended);
        }

        immediate.draw(net.minecraft.client.render.RenderLayers.lines());

        if (this.heldPreview.getValue() && this.snowLanding != null && this.getHeldType(client) == PyrotechnicItemType.SNOWBALL) {
            VertexConsumer particleBuffer = immediate.getBuffer(ClientPipelines.WORLD_PARTICLES_COLOR);
            this.emitLandingMarker(particleBuffer, entry, this.snowLanding, camPos, defaultColor);
            immediate.draw(ClientPipelines.WORLD_PARTICLES_COLOR);
        }
    }

    private void drawBoxOutline(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, Box box, int color) {
        Vec3d[] corners = new Vec3d[]{
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.maxZ)
        };

        for (int[] edge : BOX_EDGES) {
            this.line(consumer, entry, camPos, corners[edge[0]], corners[edge[1]], color);
        }
    }

    private void line(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, Vec3d p1, Vec3d p2, int color) {
        float x1 = (float)(p1.x - camPos.x);
        float y1 = (float)(p1.y - camPos.y);
        float z1 = (float)(p1.z - camPos.z);
        float x2 = (float)(p2.x - camPos.x);
        float y2 = (float)(p2.y - camPos.y);
        float z2 = (float)(p2.z - camPos.z);
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6f) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;
        consumer.vertex(entry, x1, y1, z1).color(color).normal(entry, nx, ny, nz).lineWidth(2.0f);
        consumer.vertex(entry, x2, y2, z2).color(color).normal(entry, nx, ny, nz).lineWidth(2.0f);
    }

    private void renderSnowPrediction(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, int color, float tickDelta) {
        this.updateSnowPrediction(tickDelta);
        if (this.simulationPoints.size() < 2) {
            return;
        }

        int total = this.simulationPoints.size() - 1;
        for (int i = 1; i < this.simulationPoints.size(); i++) {
            float alphaFactor = 1.0f - (float)i / total * 0.35f;
            int pointColor = ColorUtil.multAlpha(color, alphaFactor);
            if (ColorUtil.alpha(pointColor) > 0) {
                this.line(consumer, entry, camPos, this.simulationPoints.get(i - 1), this.simulationPoints.get(i), pointColor);
            }
        }
    }

    private void updateSnowPrediction(float tickDelta) {
        MinecraftClient client = this.mc;
        if (client.player == null || client.world == null) {
            this.snowLanding = null;
            this.simulationPoints.clear();
            return;
        }

        Vec3d rot = client.player.getRotationVec(tickDelta);
        if (rot.lengthSquared() < 1.0E-6) {
            this.snowLanding = null;
            this.simulationPoints.clear();
            return;
        }

        Vec3d start = client.player.getCameraPosVec(tickDelta).add(rot.multiply(0.05));
        this.snowLanding = this.simulateSnowLanding(start, rot.multiply(1.5));
    }

    private Vec3d simulateSnowLanding(Vec3d start, Vec3d velocity) {
        this.simulationPoints.clear();
        this.simulationPoints.add(start);
        Vec3d current = start;
        Vec3d vel = velocity;

        for (int i = 0; i < 160; i++) {
            Vec3d next = current.add(vel);
            BlockHitResult hit = this.mc.world.raycast(new RaycastContext(
                current, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)this.mc.player
            ));

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                Vec3d hitPos = hit.getPos();
                this.simulationPoints.add(hitPos);
                return hitPos;
            }

            this.simulationPoints.add(next);
            current = next;
            vel = vel.multiply(0.99).add(0.0, -0.03, 0.0);
            if (current.y < -64.0) {
                break;
            }
        }

        return null;
    }

    private void emitLandingMarker(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d pos, Vec3d camPos, int color) {
        double r = 0.15;
        double y = pos.y + 0.005;
        consumer.vertex(entry, (float)(pos.x - r - camPos.x), (float)(y - camPos.y), (float)(pos.z - r - camPos.z)).color(color);
        consumer.vertex(entry, (float)(pos.x + r - camPos.x), (float)(y - camPos.y), (float)(pos.z - r - camPos.z)).color(color);
        consumer.vertex(entry, (float)(pos.x + r - camPos.x), (float)(y - camPos.y), (float)(pos.z + r - camPos.z)).color(color);
        consumer.vertex(entry, (float)(pos.x - r - camPos.x), (float)(y - camPos.y), (float)(pos.z + r - camPos.z)).color(color);
    }

    // ==========================================
    // ==========================================

    public static class ActiveZone {
        public final PyrotechnicItemType type;
        public Vec3d position;
        public final long startTime;
        public final long durationMs;
        public float occupiedBlend = 0.0f;

        public ActiveZone(PyrotechnicItemType type, Vec3d position, long startTime) {
            this.type = type;
            this.position = position;
            this.startTime = startTime;
            this.durationMs = type.durationMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - this.startTime >= this.durationMs;
        }

        public double getRemainingSeconds() {
            long elapsed = System.currentTimeMillis() - this.startTime;
            long remaining = Math.max(0L, this.durationMs - elapsed);
            return remaining / 1000.0;
        }
    }

    private static class PendingActivation {
        final PyrotechnicItemType type;
        final Vec3d position;
        final long detectedTime;
        int delayTicks;

        PendingActivation(PyrotechnicItemType type, Vec3d position) {
            this.type = type;
            this.position = position;
            this.detectedTime = System.currentTimeMillis();
            this.delayTicks = 4;
        }
    }

    public enum PyrotechnicItemType {
        STUN_STAR("STUN_STAR", "Стан", 30.0f, 30.0f, 30.0f, -15.0f, -15.0f, -15.0f, -15.0f, -15.0f, -15.0f, 15000L, false, Items.NETHER_STAR),
        ALTERNATIVE_TRAP("ALTERNATIVE_TRAP", "Трапка", 5.0f, 4.0f, 5.0f, -2.0f, 0.0f, -2.0f, 0.0f, 0.0f, 0.0f, 11000L, true, Items.POPPED_CHORUS_FRUIT),
        EXPLOSIVE_TRAP("EXPLOSIVE_TRAP", "Взрывная трапка", 7.0f, 2.0f, 7.0f, -3.0f, -2.0f, -3.0f, 0.0f, 1.0f, 0.0f, 11000L, true, Items.PRISMARINE_SHARD),
        SNOWBALL("SNOWBALL", "Снежок", 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0L, false, Items.SNOWBALL);

        public final String nbtName;
        public final String displayName;
        public final float renderSizeX;
        public final float renderSizeY;
        public final float renderSizeZ;
        public final float renderOffsetX;
        public final float renderOffsetY;
        public final float renderOffsetZ;
        public final float hitboxOffsetX;
        public final float hitboxOffsetY;
        public final float hitboxOffsetZ;
        public final long durationMs;
        public final boolean blockAligned;
        public final Item fallbackItem;

        PyrotechnicItemType(
            String nbtName, String displayName,
            float sizeX, float sizeY, float sizeZ,
            float offsetX, float offsetY, float offsetZ,
            float hitboxOffX, float hitboxOffY, float hitboxOffZ,
            long durationMs, boolean blockAligned, Item fallbackItem
        ) {
            this.nbtName = nbtName;
            this.displayName = displayName;
            this.renderSizeX = sizeX;
            this.renderSizeY = sizeY;
            this.renderSizeZ = sizeZ;
            this.renderOffsetX = offsetX;
            this.renderOffsetY = offsetY;
            this.renderOffsetZ = offsetZ;
            this.hitboxOffsetX = hitboxOffX;
            this.hitboxOffsetY = hitboxOffY;
            this.hitboxOffsetZ = hitboxOffZ;
            this.durationMs = durationMs;
            this.blockAligned = blockAligned;
            this.fallbackItem = fallbackItem;
        }

        public float getHitboxSizeY() {
            return this == EXPLOSIVE_TRAP ? 3.0f : this.renderSizeY;
        }

        public Box getRenderBoundingBox(double originX, double originY, double originZ) {
            double minX = originX + this.renderOffsetX;
            double minY = originY + this.renderOffsetY;
            double minZ = originZ + this.renderOffsetZ;
            double maxX = minX + this.renderSizeX;
            double maxY = minY + this.renderSizeY;
            double maxZ = minZ + this.renderSizeZ;
            return new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public Box getHitboxBoundingBox(double originX, double originY, double originZ) {
            double minX = originX + this.hitboxOffsetX;
            double minY = originY + this.hitboxOffsetY;
            double minZ = originZ + this.hitboxOffsetZ;
            double maxX = minX + this.renderSizeX;
            double maxY = minY + this.getHitboxSizeY();
            double maxZ = minZ + this.renderSizeZ;
            return new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public static PyrotechnicItemType fromStack(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return null;
            }

            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData != null) {
                NbtCompound nbt = customData.copyNbt();
                if (nbt != null) {
                    java.util.Optional<NbtCompound> pyrotechnicItemOpt = nbt.getCompound("pyrotechnic-item");
                    if (pyrotechnicItemOpt.isPresent()) {
                        String name = pyrotechnicItemOpt.get().getString("name").orElse("");
                        for (PyrotechnicItemType type : values()) {
                            if (type.nbtName.equalsIgnoreCase(name)) {
                                return type;
                            }
                        }
                    }
                }
            }

            if (stack.isOf(Items.NETHER_STAR)) return STUN_STAR;
            if (stack.isOf(Items.POPPED_CHORUS_FRUIT)) return ALTERNATIVE_TRAP;
            if (stack.isOf(Items.PRISMARINE_SHARD)) return EXPLOSIVE_TRAP;
            if (stack.isOf(Items.SNOWBALL)) return SNOWBALL;

            return null;
        }
    }
}
