package rtx.heave.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.player.AttackEntityEvent;
import rtx.heave.api.events.impl.render.HudRenderEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.api.ui.theme.ClientAccent;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.targetesp.BoxTargetEspRenderer;
import rtx.heave.utils.render.targetesp.CrystalsTargetEspRenderer;
import rtx.heave.utils.render.targetesp.GhostsTargetEspRenderer;
import rtx.heave.utils.render.targetesp.JelloTargetEspRenderer;
import rtx.heave.utils.render.targetesp.OrbitTargetEspRenderer;
import rtx.heave.utils.render.targetesp.PhantomSpiritsRenderer;
import rtx.heave.utils.render.targetesp.ProjectionUtil;
import rtx.heave.utils.render.targetesp.TargetCircleRenderer;
import rtx.heave.utils.render.targetesp.TargetEspColorProvider;
import rtx.heave.utils.render.targetesp.TargetEspMath;
import rtx.heave.utils.render.targetesp.TargetLensDistortionRenderer;

public class TargetESP extends Module {
    public static final String MODE_CRYSTALS = "Кристаллы";
    public static final String MODE_GHOSTS = "Призраки";
    public static final String MODE_CIRCLE = "Круг";
    public static final String MODE_RHOMBUS = "Квадрат";
    public static final String MODE_MARKER = "Маркер";
    public static final String MODE_JELLO = "Jello";
    public static final String MODE_ORBIT = "Орбита";

    public static final String SPIRIT_1 = "Призрак 1";
    public static final String SPIRIT_2 = "Призрак 2";
    public static final String SPIRIT_3 = "Призрак 3";
    public static final String SPIRIT_4 = "Призрак 4";
    public static final String SPIRIT_5 = "Призрак 5";

    public static final String SHAPE_ARROWS = "Стрелки";
    public static final String SHAPE_CRYSTALS = "Кристаллы";
    public static final String SHAPE_RHOMBUS = "Ромбы";
    public static final String SHAPE_CUBES = "Кубы";

    private static final Identifier RHOMBUS_TEXTURE = Identifier.of("heave", "textures/targetesp/target4.png");
    private static final Identifier MARKER_TEXTURE = Identifier.of("heave", "textures/targetesp/target5.png");

    private static TargetESP instance;

    // 1. Mode
    private final ModeSetting mode = this.register(
        new ModeSetting(
            "Режим", "Режим отображения Target ESP.",
            MODE_CRYSTALS,
            MODE_CRYSTALS,
            MODE_GHOSTS,
            MODE_CIRCLE,
            MODE_RHOMBUS,
            MODE_MARKER,
            MODE_JELLO,
            MODE_ORBIT
        )
    );

    private final ModeSetting spiritType = this.register(
        new ModeSetting(
            "Призрак", "Вариант призраков.",
            SPIRIT_1,
            SPIRIT_1,
            SPIRIT_2,
            SPIRIT_3,
            SPIRIT_4,
            SPIRIT_5
        ).visibleWhen(() -> this.mode.is(MODE_GHOSTS))
    );

    // 2. Settings
    private final SliderSetting rotationSpeed = this.register(
        new SliderSetting("Скорость вращения", "Скорость вращения маркера.")
            .range(0.1f, 5.0f).increment(0.1f).setValue(1.3f)
            .visible(() -> this.mode.is(MODE_RHOMBUS) || this.mode.is(MODE_MARKER))
    );
    private final SliderSetting markerSize = this.register(
        new SliderSetting("Размер", "Размер 2D маркера.")
            .range(0.5f, 2.5f).increment(0.1f).setValue(1.0f)
            .visible(() -> this.mode.is(MODE_RHOMBUS) || this.mode.is(MODE_MARKER))
    );
    private final SliderSetting transparency = this.register(
        new SliderSetting("Прозрачность", "Прозрачность 2D маркера.")
            .range(0.1f, 1.0f).increment(0.05f).setValue(1.0f)
            .visible(() -> this.mode.is(MODE_RHOMBUS) || this.mode.is(MODE_MARKER))
    );

    private final SeparatorSetting crystalsSep = this.register(new SeparatorSetting("Кристаллы").visible(() -> this.mode.is(MODE_CRYSTALS)));
    private final BooleanSetting distortion = this.register(
        new BooleanSetting("Искажение", "Искажение экрана вокруг кристаллов.", true).visible(() -> this.mode.is(MODE_CRYSTALS))
    );
    private final SliderSetting distortionStrength = this.register(
        new SliderSetting("Сила искажения", "Интенсивность искажения пространства.").range(0.01f, 0.20f).increment(0.01f).setValue(0.05f).visible(() -> this.mode.is(MODE_CRYSTALS) && this.distortion.getValue())
    );
    private final SliderSetting crystalsSpeed = this.register(
        new SliderSetting("Скорость анимации", "Скорость вращения кристаллов.").range(0.5f, 5.0f).increment(0.1f).setValue(1.5f).visible(() -> this.mode.is(MODE_CRYSTALS))
    );

    // 4. Group "Jello"
    private final SeparatorSetting jelloSep = this.register(new SeparatorSetting("Jello").visible(() -> this.mode.is(MODE_JELLO)));
    private final SliderSetting jelloSpeed = this.register(
        new SliderSetting("Скорость анимации", "Скорость анимации Jello.").range(0.5f, 5.0f).increment(0.1f).setValue(1.5f).visible(() -> this.mode.is(MODE_JELLO))
    );

    private final SeparatorSetting orbitSep = this.register(new SeparatorSetting("Орбита").visible(() -> this.mode.is(MODE_ORBIT)));
    private final ModeSetting orbitShape = this.register(
        new ModeSetting("Форма", "Форма фигур на орбите.", SHAPE_CRYSTALS, SHAPE_ARROWS, SHAPE_CRYSTALS, SHAPE_CUBES).visibleWhen(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitSpeed = this.register(
        new SliderSetting("Скорость анимации", "Скорость вращения фигур по орбите.").range(0.5f, 5.0f).increment(0.1f).setValue(1.5f).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitFiguresPerCircle = this.register(
        new SliderSetting("Фигур по кругу", "Количество фигур на каждом кольце.").range(2.0f, 8.0f).increment(1.0f).setValue(3.0f).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitLayers = this.register(
        new SliderSetting("Слоёв по высоте", "Количество слоёв фигур по высоте цели.").range(2.0f, 5.0f).increment(1.0f).setValue(3.0f).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitLayerSpacing = this.register(
        new SliderSetting("Отступ между слоями", "Расстояние между слоями по высоте.").range(0.3f, 2.0f).increment(0.05f).setValue(1.0f).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitDistance = this.register(
        new SliderSetting("Дистанция", "Радиус удаления фигур от цели.").range(0.5f, 2.0f).increment(0.05f).setValue(0.9f).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitFigureSize = this.register(
        new SliderSetting("Размер фигур", "Размер 3D фигур на орбите.").range(0.08f, 0.4f).increment(0.01f).setValue(0.2f).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final BooleanSetting orbitRotation = this.register(
        new BooleanSetting("Вращение", "Вращение фигур вокруг своей оси.", true).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final BooleanSetting orbitGlow = this.register(
        new BooleanSetting("Свечение", "Отображать свечение вокруг фигур.", true).visible(() -> this.mode.is(MODE_ORBIT))
    );
    private final SliderSetting orbitGlowSize = this.register(
        new SliderSetting("Размер свечения", "Размер ореола свечения.").range(0.1f, 1.0f).increment(0.05f).setValue(0.85f).visible(() -> this.mode.is(MODE_ORBIT) && this.orbitGlow.getValue())
    );
    private final SliderSetting orbitGlowAlpha = this.register(
        new SliderSetting("Прозрачность свечения", "Прозрачность ореола свечения.").range(0.1f, 1.0f).increment(0.05f).setValue(0.25f).visible(() -> this.mode.is(MODE_ORBIT) && this.orbitGlow.getValue())
    );

    private final SeparatorSetting colorSep = this.register(new SeparatorSetting("Цвет"));
    private final BooleanSetting clientColor = this.register(new BooleanSetting("Цвет клиента", "Использовать акцентный цвет клиента.", true));
    private final ColorSetting customColor = this.register(new ColorSetting("Кастомный цвет", "Пользовательский цвет подсветки.", new Color(255, 255, 255, 255)).visible(() -> !this.clientColor.getValue()));
    private final ColorSetting customColor2 = this.register(new ColorSetting("Второй цвет", "Второй цвет для двухцветных спиралей и призраков.", new Color(120, 160, 255, 255)).visible(() -> !this.clientColor.getValue()));

    private final SeparatorSetting hitSep = this.register(new SeparatorSetting("При ударе"));
    private final BooleanSetting hitEnable = this.register(new BooleanSetting("Включить", "Включить реакцию эффектов при ударе цели.", true));
    private final BooleanSetting hitChangeColor = this.register(new BooleanSetting("Изменять цвет", "Окрашивать в цвет урона при ударе.", true).visible(this.hitEnable::getValue));
    private final ColorSetting hitDamageColor = this.register(new ColorSetting("Цвет урона", "Цвет вспышки при получении урона.", new Color(255, 50, 50, 255)).visible(() -> this.hitEnable.getValue() && this.hitChangeColor.getValue()));
    private final BooleanSetting hitSpeedUp = this.register(new BooleanSetting("Ускорять анимацию", "Временно ускорять анимацию при нанесении урона.", true).visible(this.hitEnable::getValue));
    private final SliderSetting hitSpeedMultiplier = this.register(new SliderSetting("Множитель ускорения", "Во сколько раз ускоряется анимация.").range(1.2f, 5.0f).increment(0.1f).setValue(3.0f).visible(() -> this.hitEnable.getValue() && this.hitSpeedUp.getValue()));
    private final SliderSetting hitEffectDuration = this.register(new SliderSetting("Длительность эффекта", "Длительность эффекта ускорения в секундах.").range(0.3f, 2.0f).increment(0.1f).setValue(0.8f).visible(this.hitEnable::getValue));

    // 9. Targets
    private final SeparatorSetting targetsSep = this.register(new SeparatorSetting("Цели"));
    private final BooleanSetting targetPlayers = this.register(new BooleanSetting("Players", "Отображать Target ESP на игроках.", true));
    private final BooleanSetting targetMobs = this.register(new BooleanSetting("Mobs", "Отображать Target ESP на мобах.", true));

    // Target fade and tracking
    private final Map<LivingEntity, Float> fadeMap = new HashMap<>();
    private final Map<LivingEntity, Double> rotMap = new HashMap<>();
    private final Map<LivingEntity, Double> bobMap = new HashMap<>();
    private final Map<LivingEntity, Integer> prevHurtTimeMap = new HashMap<>();
    private final Map<LivingEntity, Long> hitTimeMap = new HashMap<>();
    private long lastFrameTime = System.currentTimeMillis();

    // Phantom TargetESP physics & animation states
    private final Vec3d[] orbitPositions = new Vec3d[3];
    private final Vec3d[] orbitMotions = new Vec3d[3];
    @SuppressWarnings("unchecked")
    private final List<Vec3d>[] orbitTrails = new List[3];
    private final float[] orbitShrinkHolder = new float[]{0.0f};

    private long lastSpiritFrameTime = 0L;
    private float spiritTime = 0.0f;
    private long lastGhostTime = 0L;
    private long modeStartTime = System.currentTimeMillis();
    private long lastNanoTime = System.nanoTime();
    private float accumulatedHurt = 0.0f;
    private float crystalMoving = 0.0f;
    private float damageAnimation = 0.0f;

    // Combat target tracking
    private LivingEntity combatTarget = null;
    private long combatTargetTime = 0L;
    private static final long COMBAT_TARGET_TIMEOUT_MS = 15000L;

    public TargetESP() {
        super("Target ESP", "Отображает ESP вокруг цели.", Category.VISUALS);
        instance = this;
        for (int i = 0; i < 3; i++) {
            orbitTrails[i] = new ArrayList<>();
            orbitMotions[i] = Vec3d.ZERO;
        }
    }

    public static TargetESP getInstance() {
        return instance;
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity living && matchesTargetType(living)) {
            this.combatTarget = living;
            this.combatTargetTime = System.currentTimeMillis();
            this.damageAnimation = 1.0f;
            if (this.hitEnable.getValue()) {
                this.hitTimeMap.put(living, System.currentTimeMillis());
            }
        }
    }

    @Override
    public void onDisable() {
        fadeMap.clear();
        rotMap.clear();
        bobMap.clear();
        prevHurtTimeMap.clear();
        hitTimeMap.clear();
        combatTarget = null;
        combatTargetTime = 0L;
        damageAnimation = 0.0f;
        lastSpiritFrameTime = 0L;
        spiritTime = 0.0f;
        lastGhostTime = 0L;
        accumulatedHurt = 0.0f;
        crystalMoving = 0.0f;
        orbitShrinkHolder[0] = 0.0f;
        for (int i = 0; i < 3; i++) {
            orbitPositions[i] = null;
            orbitMotions[i] = Vec3d.ZERO;
            if (orbitTrails[i] != null) {
                orbitTrails[i].clear();
            }
        }
        TargetLensDistortionRenderer.closeResources();
        super.onDisable();
    }

    public boolean matchesTargetType(LivingEntity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return this.targetPlayers.getValue();
        }
        return this.targetMobs.getValue();
    }

    private LivingEntity acquireTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        double maxDistSq = 64.0 * 64.0;

        if (combatTarget != null) {
            if (combatTarget.isAlive() 
                && matchesTargetType(combatTarget) 
                && System.currentTimeMillis() - combatTargetTime < COMBAT_TARGET_TIMEOUT_MS
                && mc.player.squaredDistanceTo(combatTarget) <= maxDistSq) {
                return combatTarget;
            } else {
                combatTarget = null;
            }
        }

        if (mc.targetedEntity instanceof LivingEntity living && matchesTargetType(living) && mc.player.squaredDistanceTo(living) <= maxDistSq) {
            return living;
        }
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && matchesTargetType(living) && mc.player.squaredDistanceTo(living) <= maxDistSq) {
            return living;
        }

        LivingEntity closest = null;
        double bestDist = maxDistSq;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && matchesTargetType(living)) {
                double distSq = mc.player.squaredDistanceTo(living);
                if (distSq < bestDist) {
                    bestDist = distSq;
                    closest = living;
                }
            }
        }
        return closest;
    }

    private float getBaseSpeed() {
        return switch (this.mode.getValue()) {
            case MODE_CRYSTALS -> this.crystalsSpeed.getFloat();
            case MODE_JELLO -> this.jelloSpeed.getFloat();
            default -> this.orbitSpeed.getFloat();
        };
    }

    private float getEffectiveSpeed(LivingEntity entity) {
        float speed = getBaseSpeed();
        if (!this.hitEnable.getValue() || !this.hitSpeedUp.getValue() || entity == null) {
            return speed;
        }

        int curHurt = entity.hurtTime;
        int prevHurt = this.prevHurtTimeMap.getOrDefault(entity, 0);
        this.prevHurtTimeMap.put(entity, curHurt);

        if (curHurt > 0 && prevHurt <= 0) {
            this.hitTimeMap.put(entity, System.currentTimeMillis());
            this.damageAnimation = 1.0f;
        }

        Long hitTime = this.hitTimeMap.get(entity);
        if (hitTime == null) {
            return speed;
        }

        float duration = this.hitEffectDuration.getFloat();
        float elapsed = (System.currentTimeMillis() - hitTime) / 1000.0f;
        if (elapsed > duration) {
            return speed;
        }

        float progress = elapsed / duration;
        float factor;
        if (progress < 0.15f) {
            factor = progress / 0.15f;
        } else {
            float dec = (progress - 0.15f) / 0.85f;
            factor = 1.0f - dec * dec * (3.0f - 2.0f * dec);
        }
        return speed * (1.0f + (this.hitSpeedMultiplier.getFloat() - 1.0f) * factor);
    }

    private int getTargetColorRGB(LivingEntity entity) {
        int baseRGB = this.clientColor.getValue() ? ClientAccent.accentOpaque() : this.customColor.getColor();
        if (this.hitEnable.getValue() && this.hitChangeColor.getValue() && entity != null && entity.hurtTime > 0) {
            float t = MathHelper.clamp(entity.hurtTime / 10.0f, 0.0f, 1.0f);
            baseRGB = ColorUtil.lerpColor(baseRGB, this.hitDamageColor.getColor(), t);
        }
        return baseRGB;
    }

    private int getTargetSecondColorRGB(LivingEntity entity) {
        int baseRGB = this.clientColor.getValue() ? ClientAccent.gradientB(255.0f) : this.customColor2.getColor();
        if (this.hitEnable.getValue() && this.hitChangeColor.getValue() && entity != null && entity.hurtTime > 0) {
            float t = MathHelper.clamp(entity.hurtTime / 10.0f, 0.0f, 1.0f);
            baseRGB = ColorUtil.lerpColor(baseRGB, this.hitDamageColor.getColor(), t);
        }
        return baseRGB;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        float dt = MathHelper.clamp((now - lastFrameTime) / 1000.0f, 0.001f, 0.1f);
        lastFrameTime = now;

        if (this.damageAnimation > 0.0f) {
            this.damageAnimation = Math.max(0.0f, this.damageAnimation - 0.083333336f);
        }

        LivingEntity activeTarget = acquireTarget();

        if (activeTarget != null) {
            fadeMap.putIfAbsent(activeTarget, 0.0f);
        }

        Iterator<Map.Entry<LivingEntity, Float>> it = fadeMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Float> entry = it.next();
            LivingEntity entity = entry.getKey();
            float fade = entry.getValue();

            boolean isActive = (entity == activeTarget) && entity.isAlive();
            if (isActive) {
                fade = Math.min(1.0f, fade + dt / 0.18f);
            } else {
                fade = Math.max(0.0f, fade - dt / 0.22f);
            }
            entry.setValue(fade);

            if (fade <= 0.001f && !isActive) {
                it.remove();
                rotMap.remove(entity);
                bobMap.remove(entity);
                prevHurtTimeMap.remove(entity);
                hitTimeMap.remove(entity);
                continue;
            }

            float effSpeed = getEffectiveSpeed(entity);
            double rot = rotMap.getOrDefault(entity, 0.0) + (double)(effSpeed * dt * 180.0f);
            double bob = bobMap.getOrDefault(entity, 0.0) + (double)(effSpeed * dt * 3.0f);
            rotMap.put(entity, rot % 360000.0);
            bobMap.put(entity, bob % (Math.PI * 2000.0));
        }

        if (fadeMap.isEmpty()) {
            return;
        }

        // Animation state updates matching Phantom
        this.crystalMoving += 1.0f;
        if (this.lastSpiritFrameTime == 0L) {
            this.lastSpiritFrameTime = now;
        }
        this.spiritTime += (float) Math.max(0L, now - this.lastSpiritFrameTime) * 5.0f * 1.3f / 900.0f;
        this.lastSpiritFrameTime = now;

        long nanoTime = System.nanoTime();
        float deltaTime = (float) (nanoTime - this.lastNanoTime) / 2000000.0f;
        this.lastNanoTime = nanoTime;
        if (activeTarget != null && activeTarget.hurtTime > 0) {
            this.accumulatedHurt += ((float) activeTarget.hurtTime / 10.0f) * deltaTime;
        }

        MatrixStack matrixStack = event.getStack();
        if (matrixStack == null) {
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        float tickDelta = event.getPartialTicks();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        Quaternionf cameraRot = mc.gameRenderer.getCamera().getRotation();

        for (Map.Entry<LivingEntity, Float> entry : fadeMap.entrySet()) {
            LivingEntity entity = entry.getKey();
            float alpha = entry.getValue();
            if (alpha <= 0.001f) continue;

            double ex = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double ey = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double ez = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            float rotDeg = (float) rotMap.getOrDefault(entity, 0.0).doubleValue();
            float bobRad = (float) bobMap.getOrDefault(entity, 0.0).doubleValue();
            int targetRGB = getTargetColorRGB(entity);
            int secondRGB = getTargetSecondColorRGB(entity);

            TargetEspColorProvider colorProvider = (colorOffset, a) -> {
                int finalAlpha = MathHelper.clamp((int)(ColorUtil.alpha(targetRGB) * a), 0, 255);
                return (targetRGB & 0x00FFFFFF) | (finalAlpha << 24);
            };

            matrixStack.push();
            matrixStack.translate(ex - cameraPos.x, ey - cameraPos.y, ez - cameraPos.z);

            float entityWidth = Math.max(0.2f, entity.getWidth());
            float entityHeight = Math.max(0.4f, entity.getHeight());

            switch (this.mode.getValue()) {
                case MODE_CRYSTALS -> {
                    PhantomSpiritsRenderer.renderCrystals(
                        immediate,
                        matrixStack,
                        cameraRot,
                        entityWidth,
                        entityHeight,
                        alpha,
                        this.crystalMoving,
                        targetRGB
                    );
                }
                case MODE_GHOSTS -> {
                    switch (this.spiritType.getValue()) {
                        case SPIRIT_1 -> PhantomSpiritsRenderer.renderNewSpirits(
                            immediate,
                            matrixStack,
                            mc.gameRenderer.getCamera(),
                            cameraRot,
                            alpha,
                            now,
                            this.spiritTime,
                            targetRGB,
                            false
                        );
                        case SPIRIT_2 -> PhantomSpiritsRenderer.renderFriendsSpirits(
                            immediate,
                            matrixStack,
                            cameraRot,
                            alpha,
                            now,
                            targetRGB,
                            secondRGB,
                            false
                        );
                        case SPIRIT_3 -> PhantomSpiritsRenderer.renderGhosts(
                            immediate,
                            matrixStack,
                            cameraRot,
                            entityWidth,
                            entityHeight,
                            alpha,
                            now,
                            this.modeStartTime,
                            targetRGB,
                            secondRGB
                        );
                        case SPIRIT_4 -> PhantomSpiritsRenderer.renderJavelinSpirits(
                            immediate,
                            matrixStack,
                            cameraRot,
                            entityHeight,
                            alpha,
                            now,
                            this.modeStartTime,
                            this.accumulatedHurt,
                            targetRGB
                        );
                        case SPIRIT_5 -> PhantomSpiritsRenderer.renderNursultan(
                            immediate,
                            matrixStack,
                            cameraRot,
                            entityHeight,
                            alpha,
                            now,
                            this.modeStartTime,
                            targetRGB
                        );
                    }
                }
                case MODE_CIRCLE -> {
                    PhantomSpiritsRenderer.renderCircle(
                        immediate,
                        matrixStack,
                        cameraRot,
                        entityHeight,
                        alpha,
                        now,
                        this.modeStartTime,
                        this.accumulatedHurt,
                        targetRGB
                    );
                }
                case MODE_JELLO -> {
                    JelloTargetEspRenderer.render(
                        immediate,
                        matrixStack,
                        cameraRot,
                        entityWidth,
                        entityHeight,
                        rotDeg,
                        alpha,
                        targetRGB
                    );
                }
                case MODE_ORBIT -> {
                    OrbitTargetEspRenderer.render(
                        immediate,
                        matrixStack,
                        cameraRot,
                        this.orbitShape.getValue(),
                        entityWidth,
                        entityHeight,
                        this.orbitDistance.getFloat(),
                        this.orbitLayers.getInt(),
                        this.orbitLayerSpacing.getFloat(),
                        this.orbitFiguresPerCircle.getInt(),
                        this.orbitFigureSize.getFloat(),
                        this.orbitRotation.getValue(),
                        rotDeg,
                        bobRad,
                        this.orbitGlow.getValue(),
                        this.orbitGlowSize.getFloat(),
                        this.orbitGlowAlpha.getFloat(),
                        alpha,
                        colorProvider
                    );
                }
            }

            matrixStack.pop();

            if (this.mode.is(MODE_CRYSTALS) && this.distortion.getValue()) {
                float strength = this.distortionStrength.getFloat() * alpha;
                if (strength > 0.001f) {
                    Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
                    Vec3d targetPos = new Vec3d(ex, ey, ez);
                    float width = entityWidth * 1.5f;
                    float height = entityHeight;
                    Matrix4f proj = event.getProjectionMatrix();
                    Matrix4f modelViewProj = new Matrix4f(proj).mul(event.getPositionMatrix());
                    float aspect = proj.m11() / proj.m00();
                    List<TargetLensDistortionRenderer.Lens> lenses = new ArrayList<>();

                    for (int angle = 0; angle < 360; angle += 20) {
                        float scale = 1.2f - 0.5f * alpha;
                        float rad = (float) Math.toRadians(angle + rotDeg * 0.3f);
                        float dx = (float) (Math.sin(rad) * width * scale);
                        float dz = (float) (Math.cos(rad) * width * scale);
                        float dy = 0.1f + height * (float) Math.abs(Math.sin(Math.toRadians(angle)));
                        TargetLensDistortionRenderer.projectLens(lenses, modelViewProj, proj, targetPos, camPos, dx, dy, dz, 0.45f);
                    }

                    if (!lenses.isEmpty()) {
                        TargetLensDistortionRenderer.apply(mc.getFramebuffer(), aspect, strength, lenses);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHudRender(HudRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!this.mode.is(MODE_RHOMBUS) && !this.mode.is(MODE_MARKER)) {
            return;
        }

        DrawContext context = event.getGraphics();
        if (context == null) return;

        long now = System.currentTimeMillis();
        float tickDelta = event.getPartialTick();
        boolean isMarker = this.mode.is(MODE_MARKER);
        Identifier texture = isMarker ? MARKER_TEXTURE : RHOMBUS_TEXTURE;

        for (Map.Entry<LivingEntity, Float> entry : fadeMap.entrySet()) {
            LivingEntity entity = entry.getKey();
            float fadeAlpha = entry.getValue();
            if (fadeAlpha <= 0.001f || !entity.isAlive()) continue;

            double ex = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double ey = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + entity.getBoundingBox().getLengthY() * 0.5;
            double ez = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
            Vec3d worldPos = new Vec3d(ex, ey, ez);

            Vector2f screenPos = ProjectionUtil.worldToScreen(worldPos);
            if (screenPos == null) continue;

            float scaledSize = ProjectionUtil.getScaledSize(worldPos) * this.markerSize.getFloat();
            if (scaledSize <= 0.0f) continue;

            float dmgEased = (float) TargetEspMath.easeInOutQuad(this.damageAnimation);
            if (dmgEased > 0.0f) {
                scaledSize = Math.max(1.0f, scaledSize - 2.0f * dmgEased);
            }

            double rotFactor = Math.sin((double) now / 1000.0 * (double) this.rotationSpeed.getFloat());
            float maxRot = isMarker ? 300.0f : 360.0f;
            float rotation = (float) (rotFactor * (double) maxRot);

            float glowPadding = MathHelper.clamp(scaledSize * 0.18f, 1.0f, 3.0f);
            float x = screenPos.x - scaledSize / 2.0f;
            float y = screenPos.y - scaledSize / 2.0f;

            float alphaMult = this.transparency.getFloat();
            int alphaValue = (int) (235.0f * fadeAlpha * alphaMult);
            if (alphaValue <= 0) continue;

            int targetRGB = getTargetColorRGB(entity);
            int finalColor;
            if (!this.hitEnable.getValue() || dmgEased <= 0.0f) {
                finalColor = (targetRGB & 0x00FFFFFF) | (alphaValue << 24);
            } else {
                int g = (int) (255.0f * (1.0f - dmgEased * 0.7f));
                int b = (int) (255.0f * (1.0f - dmgEased * 0.7f));
                finalColor = (alphaValue << 24) | (255 << 16) | (g << 8) | b;
            }

            int glowColor = (finalColor & 0x00FFFFFF) | ((alphaValue / 4) << 24);

            org.joml.Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(screenPos.x, screenPos.y);
            matrices.rotate((float) Math.toRadians(rotation));
            matrices.translate(-screenPos.x, -screenPos.y);

            // Glow pass
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                (int) (x - glowPadding),
                (int) (y - glowPadding),
                0.0f, 0.0f,
                (int) (scaledSize + glowPadding * 2.0f),
                (int) (scaledSize + glowPadding * 2.0f),
                (int) (scaledSize + glowPadding * 2.0f),
                (int) (scaledSize + glowPadding * 2.0f),
                glowColor
            );

            // Main pass
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                (int) x,
                (int) y,
                0.0f, 0.0f,
                (int) scaledSize,
                (int) scaledSize,
                (int) scaledSize,
                (int) scaledSize,
                finalColor
            );

            matrices.popMatrix();
        }
    }
}