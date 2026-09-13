package rtx.heave.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.events.impl.player.AttackEntityEvent;
import rtx.heave.api.events.impl.player.TotemPopEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.impl.Visuals.killeffect.KillEffectDeathMemoryTracker;
import rtx.heave.api.modules.impl.Visuals.killeffect.KillEffectParticleSystem;
import rtx.heave.api.modules.impl.Visuals.killeffect.SoulRenderer;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.MultiSelectSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.color.RainbowLut;
import rtx.heave.utils.storage.friend.FriendUtils;

public final class KillEffect extends Module {
    private static final String TARGET_PLAYERS = "Игроки";
    private static final String TARGET_FRIENDS = "Друзья";
    private static final String TARGET_MOBS = "Мобы";
    private static final String TARGET_ANIMALS = "Животные";
    private static final String COLOR_RAINBOW = "Радуга";
    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();

    private final KillEffectParticleSystem particleSystem = new KillEffectParticleSystem();
    private final Map<Integer, Long> recentlyAttacked = new HashMap<>();
    private final KillEffectDeathMemoryTracker deathMemoryTracker = new KillEffectDeathMemoryTracker(2500L, this::handleRememberedDeath);

    // 1. Souls
    private final SeparatorSetting soulsSeparator = this.register(new SeparatorSetting("Души"));
    private final BooleanSetting soulsEffect = this.register(new BooleanSetting("Души", "Эффект взлетающей души игрока при смерти и тотеме.", true));
    private final BooleanSetting soulsOnDeath = this.register(new BooleanSetting("При смерти", "Спавнить душу игрока при убийстве.", true));
    private final BooleanSetting soulsOnTotem = this.register(new BooleanSetting("При тотеме", "Спавнить душу игрока при сбитии тотема.", true));
    private final BooleanSetting testEffect = this.register(new BooleanSetting("Тест эффекта", "Запустить тест эффекта взлета души прямо перед собой.", false));

    // 2. Particles
    private final SeparatorSetting particlesSeparator = this.register(new SeparatorSetting("Партиклы"));
    private final BooleanSetting particlesEffect = this.register(new BooleanSetting("Партиклы", "Эффект распада тела жертвы на частицы.", true));
    private final ModeSetting particlePhysics = this.register(new ModeSetting("Физика частиц", "Поведение частиц тела после убийства.", "Гравитация", "Гравитация", "Разлет"));

    private final SeparatorSetting colorsSeparator = this.register(new SeparatorSetting("Цвета"));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Режим цвета частиц.", "Клиент", "Клиент", "Свой"));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать второй свой цвет.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет частиц.", new Color(-50116, true)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет частиц.", new Color(ColorUtil.lerpColor(-50116, DARK_SECOND_COLOR, 0.7f), true)));

    // 4. Targets
    private final SeparatorSetting targetsSeparator = this.register(new SeparatorSetting("Цели"));
    private final MultiSelectSetting effectTargets = this.register(
        new MultiSelectSetting("Цели эффекта", "Выберите, какие сущности запускают эффект.")
            .value(TARGET_PLAYERS, TARGET_FRIENDS, TARGET_MOBS, TARGET_ANIMALS)
            .selected(TARGET_PLAYERS, TARGET_FRIENDS, TARGET_MOBS, TARGET_ANIMALS)
    );

    public KillEffect() {
        super("Kill Effect", "Эффекты при убийстве: души и партиклы распада.", Category.VISUALS);
        this.soulsOnDeath.visibleWhen(this.soulsEffect::getValue);
        this.soulsOnTotem.visibleWhen(this.soulsEffect::getValue);
        this.particlePhysics.visibleWhen(this.particlesEffect::getValue);
        this.useSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    public static KillEffect getInstance() {
        return ModuleManager.get().get(KillEffect.class);
    }

    public static KillEffect getInstanceIfReady() {
        try {
            return KillEffect.getInstance();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static float getWorldSaturationMultiplier() {
        return 1.0f;
    }

    public static float getKillZoomFovScale() {
        return 1.0f;
    }

    public static float getKillShakeDegrees() {
        return 0.0f;
    }

    private void resetState() {
        this.particleSystem.clear();
        this.recentlyAttacked.clear();
        this.deathMemoryTracker.clear();
        SoulRenderer.clear();
    }

    @Override
    protected void onDisable() {
        this.resetState();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPost()) return;
        if (this.mc.player == null || this.mc.world == null) {
            this.resetState();
            return;
        }
        if (this.testEffect.getValue()) {
            this.testEffect.setValue(false);
            this.triggerTestEffect();
        }
        this.deathMemoryTracker.tick();
        this.particleSystem.tick(this.mc.world);
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (this.mc.player == null || attackEntityEvent.isSynthetic()) return;
        Entity entity = attackEntityEvent.getTarget();
        if (entity instanceof LivingEntity livingEntity && livingEntity != this.mc.player) {
            this.recentlyAttacked.put(livingEntity.getId(), System.currentTimeMillis());
            this.deathMemoryTracker.remember(livingEntity, true);
            this.pruneAttackMemory();
        }
    }

    @EventHandler
    public void onTotemPop(TotemPopEvent totemPopEvent) {
        if (!this.isEnabled() || !this.soulsEffect.getValue() || !this.soulsOnTotem.getValue()) return;
        if (totemPopEvent.getEntity() instanceof PlayerEntity player && player != this.mc.player) {
            SoulRenderer.spawnSoul(player);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent worldRenderEvent) {
        if (!this.isEnabled()) return;
        if (this.soulsEffect.getValue()) {
            SoulRenderer.render(worldRenderEvent);
        }
        if (this.particlesEffect.getValue()) {
            this.particleSystem.render(worldRenderEvent.getStack(), worldRenderEvent.getCamera(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers());
        }
    }

    public static void notifyEntityDied(LivingEntity livingEntity, DamageSource damageSource) {
        KillEffect killEffect = KillEffect.getInstanceIfReady();
        if (killEffect == null || !killEffect.isEnabled()) return;
        killEffect.handleDeath(livingEntity, damageSource);
    }

    private void handleDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (this.mc.player == null || this.mc.world == null || livingEntity == null || livingEntity == this.mc.player) return;
        if (!this.localPlayerGotKill(livingEntity, damageSource)) return;
        if (!this.matchesEffectTarget(livingEntity)) return;

        this.recentlyAttacked.remove(livingEntity.getId());
        this.deathMemoryTracker.forget(livingEntity.getId());
        this.onTrackedKill(livingEntity);
    }

    private void handleRememberedDeath(LivingEntity livingEntity) {
        if (!this.matchesEffectTarget(livingEntity)) return;
        this.recentlyAttacked.remove(livingEntity.getId());
        this.onTrackedKill(livingEntity);
    }

    private void onTrackedKill(LivingEntity livingEntity) {
        if (!this.matchesEffectTarget(livingEntity)) return;
        if (this.mc.player == null || this.mc.player.isDead()) return;

        // 1. Rockstar particles
        if (this.particlesEffect.getValue()) {
            this.particleSystem.spawnBodyShape(livingEntity, this.accent(), this.particlePhysics.is("Гравитация"));
        }

        // 2. Souls
        if (this.soulsEffect.getValue() && this.soulsOnDeath.getValue() && livingEntity instanceof PlayerEntity player) {
            SoulRenderer.spawnSoul(player);
        }
    }

    private boolean localPlayerGotKill(LivingEntity livingEntity, DamageSource damageSource) {
        if (this.mc.player == null) return false;
        Long l = this.recentlyAttacked.get(livingEntity.getId());
        if (l != null && System.currentTimeMillis() - l <= 2500L) return true;
        if (livingEntity.getPrimeAdversary() == this.mc.player) return true;
        return damageSource != null && damageSource.getAttacker() == this.mc.player;
    }

    private boolean matchesEffectTarget(LivingEntity livingEntity) {
        if (livingEntity == null || livingEntity == this.mc.player) return false;
        if (livingEntity instanceof PlayerEntity playerEntity) {
            if (FriendUtils.isFriend(playerEntity.getName().getString())) {
                return this.effectTargets.isSelected(TARGET_FRIENDS);
            }
            return this.effectTargets.isSelected(TARGET_PLAYERS);
        }
        if (livingEntity instanceof AnimalEntity) {
            return this.effectTargets.isSelected(TARGET_ANIMALS);
        }
        if (livingEntity instanceof MobEntity) {
            return this.effectTargets.isSelected(TARGET_MOBS);
        }
        return false;
    }

    private void pruneAttackMemory() {
        long cutoff = System.currentTimeMillis() - 2500L;
        this.recentlyAttacked.values().removeIf(l -> l < cutoff);
    }

    private static int fade(int c1, int c2) {
        int n3 = (int)(System.currentTimeMillis() / 8L % 360L);
        n3 = n3 >= 180 ? 360 - n3 : n3;
        return ColorUtil.lerpColor(c1, c2, (float)n3 / 180.0f);
    }

    private int accent() {
        if (this.colorMode.is(COLOR_CLIENT)) {
            InterfaceModule im = InterfaceModule.getInstance();
            if (im != null) {
                int c1 = im.clientPrimaryColorOpaque();
                int c2 = im.usesSecondClientColor() ? im.clientSecondaryColorOpaque() : c1;
                return c1 == c2 ? (0xFF000000 | c1) : (0xFF000000 | fade(c1, c2));
            }
            return 0xFF5078DC;
        }
        int c1 = this.customColor.getColorOpaque();
        int c2 = this.useSecondColor.getValue() ? this.customSecondColor.getColorOpaque() : c1;
        if (c1 == c2) {
            return 0xFF000000 | c1;
        }
        return 0xFF000000 | fade(c1, c2);
    }
    private void triggerTestEffect() {
        if (this.mc.player == null) return;
        net.minecraft.util.math.Vec3d forward = this.mc.player.getRotationVec(1.0f).multiply(2.5);
        net.minecraft.util.math.Vec3d pos = this.mc.player.getCameraPosVec(1.0f).add(forward).subtract(0, 0.8, 0);

        net.minecraft.util.Identifier skin = null;
        boolean slim = false;
        if (this.mc.player instanceof net.minecraft.client.network.AbstractClientPlayerEntity cp) {
            net.minecraft.entity.player.SkinTextures st = cp.getSkin();
            if (st != null) {
                if (st.body() != null) skin = st.body().texturePath();
                slim = st.model() == net.minecraft.entity.player.PlayerSkinType.SLIM;
            }
        }
        SoulRenderer.spawnSoulAt(pos, this.mc.player.getYaw() + 180.0f, skin, slim);
        if (this.particlesEffect.getValue()) {
            this.particleSystem.spawnSphere(pos.add(0, 0.8, 0), 0.6f, 80, this.accent(), this.particlePhysics.is("Гравитация"));
        }
    }
}
