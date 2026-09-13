package rtx.heave.api.modules.impl.Utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.utils.optimization.OcclusionCuller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Модуль Optimization: куллинг сущностей, частиц, блок-сущностей, Fast Leaves,
 * кеш шрифтов, увеличенный атлас, оптимизация табличек, тостов и динамический FPS.
 */
public final class Optimization extends Module {
    private static final int[] RENDER_DISTANCE_CAP = new int[]{0, 12, 8};
    private static final double[] VIEW_SCALE = new double[]{1.0, 0.8, 0.6};
    private static Optimization instance;

    // Режим агрессивности
    public final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Агрессивность базовой оптимизации.", "Средний", "Низкий", "Средний", "Ультра")
    );

    // Основные переключатели
    public final BooleanSetting entityCulling = this.register(
        new BooleanSetting("Куллинг сущностей", "Не рендерить сущностей, полностью скрытых за блоками.", true)
    );
    public final BooleanSetting limitRenderDistance = this.register(
        new BooleanSetting("Ограничение прорисовки", "Средний — до 12 чанков, Ультра — до 8.", true)
            .visibleWhen(() -> this.tier() >= 1)
    );
    public final BooleanSetting fastLeaves = this.register(
        new BooleanSetting("Быстрая листва", "Отсекать скрытые внутренние грани листвы (Fast Leaves).", true)
    );

    public final BooleanSetting signTextCulling = this.register(
        new BooleanSetting("Отсечение текста табличек", "Скрывать текст на далеких табличках (буст на спавнах).", true)
    );
    public final SliderSetting signTextDistance = this.register(
        new SliderSetting("Дистанция табличек", "Максимальная дистанция прорисовки текста табличек.")
            .range(8.0f, 64.0f)
            .increment(1.0f)
            .setValue(24.0f)
            .visibleWhen(this.signTextCulling::getValue)
    );
    public final BooleanSetting blockEntityCulling = this.register(
        new BooleanSetting("Куллинг блок-сущностей", "Отсекать далекие сундуки, головы и баннеры.", true)
    );
    public final SliderSetting blockEntityDistance = this.register(
        new SliderSetting("Дистанция блок-сущностей", "Дистанция видимости сундуков и других тайлов.")
            .range(16.0f, 128.0f)
            .increment(4.0f)
            .setValue(48.0f)
            .visibleWhen(this.blockEntityCulling::getValue)
    );
    public final BooleanSetting particleCulling = this.register(
        new BooleanSetting("Куллинг частиц", "Не создавать частицы за пределами зоны видимости.", true)
    );
    public final SliderSetting particleDistance = this.register(
        new SliderSetting("Дистанция частиц", "Дистанция видимости частиц.")
            .range(8.0f, 64.0f)
            .increment(2.0f)
            .setValue(32.0f)
            .visibleWhen(this.particleCulling::getValue)
    );
    public final BooleanSetting fastToasts = this.register(
        new BooleanSetting("Быстрые тосты", "Пропускать рендер тостов, когда нет активных ачивок.", true)
    );
    public final BooleanSetting lightmapThrottle = this.register(
        new BooleanSetting("Оптимизация света", "Отключить лишние покадровые тики карты освещения.", true)
    );
    public final BooleanSetting dynamicFps = this.register(
        new BooleanSetting("FPS в фоне (Alt-Tab)", "Снижать FPS при неактивном или свернутом окне.", true)
    );
    public final SliderSetting backgroundFps = this.register(
        new SliderSetting("Лимит FPS в фоне", "Максимальный FPS при свернутом окне игры.")
            .range(10.0f, 60.0f)
            .increment(5.0f)
            .setValue(30.0f)
            .visibleWhen(this.dynamicFps::getValue)
    );
    public final BooleanSetting itemPhysicsThrottle = this.register(
        new BooleanSetting("Оптимизация предметов", "Пропускать физику неподвижно лежащих на земле предметов.", true)
    );
    public final BooleanSetting skyColorCache = this.register(
        new BooleanSetting("Кеш цвета неба", "Кешировать тригонометрические расчёты градиента неба.", true)
    );
    public final BooleanSetting clampChunkUpdates = this.register(
        new BooleanSetting("Плавные чанки", "Сглаживать прогрузку чанков за кадр (устраняет микрофризы).", true)
    );
    public final SliderSetting maxChunkUpdates = this.register(
        new SliderSetting("Лимит чанков за кадр", "Количество чанков, обновляемых за один кадр.")
            .range(1.0f, 8.0f)
            .increment(1.0f)
            .setValue(4.0f)
            .visibleWhen(this.clampChunkUpdates::getValue)
    );
    public final BooleanSetting disableNarration = this.register(
        new BooleanSetting("Отключить диктор", "Отключать фоновые проверки диктора Minecraft на экранах GUI.", true)
    );

    // Кеш неба
    private final Map<Long, Integer> skyColorMap = new ConcurrentHashMap<>();

    public Optimization() {
        super("Optimization", "Поднимает FPS: куллинг сущностей, частиц, листьев, кеш шрифтов и сглаживание чанков.", Category.UTILS);
        instance = this;
    }

    public static Optimization getInstance() {
        return ModuleManager.get().get(Optimization.class);
    }

    private static boolean active() {
        return instance != null && instance.isEnabled();
    }

    // --- Существующие методы ---
    public static int capEffectiveRenderDistance(int n) {
        if (!Optimization.active() || !Optimization.instance.limitRenderDistance.getValue()) {
            return n;
        }
        int n2 = RENDER_DISTANCE_CAP[instance.tier()];
        return n2 > 0 ? Math.min(n, n2) : n;
    }

    public static boolean allowBlockEntity(double distSq) {
        if (!Optimization.active() || !Optimization.instance.blockEntityCulling.getValue()) {
            return true;
        }
        double maxDist = Optimization.instance.blockEntityDistance.getValue();
        return distSq <= maxDist * maxDist;
    }

    public static boolean allowParticle(double x, double y, double z) {
        if (!Optimization.active() || !Optimization.instance.particleCulling.getValue()) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null) return true;
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) return true;
        Vec3d camPos = camera.getCameraPos();
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;
        double maxDist = Optimization.instance.particleDistance.getValue();
        return (dx * dx + dy * dy + dz * dz) <= maxDist * maxDist;
    }

    public static double scaleEntityViewScale(double d) {
        return Optimization.active() ? d * VIEW_SCALE[instance.tier()] : d;
    }

    public static boolean hideEntityShadows() {
        return Optimization.active() && instance.tier() >= 1;
    }

    public static boolean shouldRenderEntity(boolean original, Entity entity, double camX, double camY, double camZ) {
        if (!(original && Optimization.active() && Optimization.instance.entityCulling.getValue())) {
            return original;
        }
        return OcclusionCuller.isVisible(entity, camX, camY, camZ, instance.tier());
    }

    public static boolean shaderTransparency(boolean original) {
        return Optimization.active() && instance.tier() >= 1 ? false : original;
    }

    public static boolean cullClouds() {
        return Optimization.active() && instance.tier() == 2;
    }

    private int tier() {
        return this.mode.is("Ультра") ? 2 : (this.mode.is("Средний") ? 1 : 0);
    }

    // --- Новые методы оптимизации ---
    public static boolean shouldCullLeaves() {
        return Optimization.active() && Optimization.instance.fastLeaves.getValue();
    }



    public static boolean cullSignText(BlockPos pos) {
        if (!Optimization.active() || !Optimization.instance.signTextCulling.getValue() || pos == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        double maxDist = Optimization.instance.signTextDistance.getValue();
        return client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > maxDist * maxDist;
    }

    public static boolean shouldSkipEmptyToasts() {
        return Optimization.active() && Optimization.instance.fastToasts.getValue();
    }

    public static boolean shouldThrottleLightmap() {
        return Optimization.active() && Optimization.instance.lightmapThrottle.getValue();
    }

    public static int getDynamicFps(int currentFpsLimit) {
        if (!Optimization.active() || !Optimization.instance.dynamicFps.getValue()) {
            return currentFpsLimit;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isWindowFocused()) {
            int bgLimit = (int) Optimization.instance.backgroundFps.getValue();
            return Math.min(currentFpsLimit, Math.max(5, bgLimit));
        }
        return currentFpsLimit;
    }

    public static boolean shouldSkipItemTick(ItemEntity item) {
        if (!Optimization.active() || !Optimization.instance.itemPhysicsThrottle.getValue() || item == null) {
            return false;
        }
        return item.isOnGround() && item.getVelocity().lengthSquared() < 0.001;
    }

    public static Integer getCachedSkyColor(Vec3d pos) {
        if (!Optimization.active() || !Optimization.instance.skyColorCache.getValue() || pos == null) {
            return null;
        }
        long key = ((long)(pos.x / 16.0) << 32) ^ (long)(pos.z / 16.0);
        return Optimization.instance.skyColorMap.get(key);
    }

    public static void cacheSkyColor(Vec3d pos, Integer color) {
        if (!Optimization.active() || !Optimization.instance.skyColorCache.getValue() || pos == null || color == null) {
            return;
        }
        if (Optimization.instance.skyColorMap.size() > 256) {
            Optimization.instance.skyColorMap.clear();
        }
        long key = ((long)(pos.x / 16.0) << 32) ^ (long)(pos.z / 16.0);
        Optimization.instance.skyColorMap.put(key, color);
    }

    public static int clampChunkUpdates(int updates) {
        if (!Optimization.active() || !Optimization.instance.clampChunkUpdates.getValue()) {
            return updates;
        }
        int max = (int) Optimization.instance.maxChunkUpdates.getValue();
        return Math.min(updates, Math.max(1, max));
    }

    public static boolean shouldDisableNarration() {
        return Optimization.active() && Optimization.instance.disableNarration.getValue();
    }
}
