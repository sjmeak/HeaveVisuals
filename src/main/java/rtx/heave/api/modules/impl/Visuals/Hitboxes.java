package rtx.heave.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.player.AttackEntityEvent;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.MultiSelectSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.world.WorldShapeRenderer;

public final class Hitboxes extends Module {
    private static final String TARGET_PLAYERS = "Игроки";
    private static final String TARGET_MOBS = "Мобы";
    private static final String TARGET_SELF = "Себя";
    private static final String MODE_OUTLINE = "Контур";
    private static final String MODE_FILL = "Заливка";
    private static final String MODE_BOTH = "Оба";

    private final MultiSelectSetting targets = this.register(
        new MultiSelectSetting("Цели", "Чьи хитбоксы рисовать.")
            .value("Игроки", "Мобы", "Себя")
            .selected("Игроки", "Мобы")
    );
    private final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Контур, заливка или оба.", "Оба", "Контур", "Заливка", "Оба")
    );
    private final ColorSetting color = this.register(
        new ColorSetting("Цвет", "Цвет хитбокса по умолчанию.", new Color(70, 170, 255, 255))
    );
    private final BooleanSetting hoverHighlight = this.register(
        new BooleanSetting("При наводке", "Окрашивать хитбокс сущности при наведении прицела.", false)
    );
    private final ColorSetting hoverColor = this.register(
        new ColorSetting("Цвет при наводке", "Цвет хитбокса при наведении.", new Color(255, 200, 50, 255))
            .visible(() -> this.hoverHighlight.getValue())
    );
    private final BooleanSetting hitHighlight = this.register(
        new BooleanSetting("При ударе", "Окрашивать хитбокс сущности при получении урона.", false)
    );
    private final ColorSetting hitColor = this.register(
        new ColorSetting("Цвет при ударе", "Цвет хитбокса при ударе.", new Color(255, 60, 60, 255))
            .visible(() -> this.hitHighlight.getValue())
    );
    private final SliderSetting fillOpacity = this.register(
        new SliderSetting("Прозрачность заливки", "Сила заливки.")
            .range(0.0f, 1.0f).increment(0.05f).setValue(0.18f)
            .visible(() -> !this.mode.is(MODE_OUTLINE))
    );
    private final SliderSetting lineWidth = this.register(
        new SliderSetting("Толщина линий", "Толщина линий контура.")
            .range(1.0f, 4.0f).increment(0.5f).setValue(2.0f)
            .visible(() -> !this.mode.is(MODE_FILL))
    );

    private final List<Box> defaultBoxes = new ArrayList<>(64);
    private final List<Box> hoverBoxes = new ArrayList<>(16);
    private final List<Box> hitBoxes = new ArrayList<>(16);

    private Entity lastAttackedEntity;
    private long lastAttackTime;

    public Hitboxes() {
        super("Hitboxes", "Окрашивает хитбоксы сущностей.", Category.VISUALS);
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (event.getTarget() != null) {
            this.lastAttackedEntity = event.getTarget();
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }
        Vec3d cameraPos = worldRenderEvent.getCamera() != null ? worldRenderEvent.getCamera().getCameraPos() : this.mc.gameRenderer.getCamera().getCameraPos();
        double maxDistSq = 16384.0;

        this.defaultBoxes.clear();
        this.hoverBoxes.clear();
        this.hitBoxes.clear();

        for (Entity entity : this.mc.world.getEntities()) {
            if (!this.shouldRender(entity) || this.mc.player.squaredDistanceTo(entity) > maxDistSq) {
                continue;
            }
            Vec3d lerpedPos = entity.getLerpedPos(worldRenderEvent.getPartialTicks());
            Box box = entity.getBoundingBox().offset(lerpedPos.subtract(entity.getEntityPos())).expand(0.002);

            boolean isHit = this.hitHighlight.getValue() && (
                (entity instanceof LivingEntity living && living.hurtTime > 0) ||
                (entity == this.lastAttackedEntity && (System.currentTimeMillis() - this.lastAttackTime) < 450L)
            );
            boolean isHovered = !isHit && this.hoverHighlight.getValue() && (this.mc.targetedEntity == entity);

            if (isHit) {
                this.hitBoxes.add(box);
            } else if (isHovered) {
                this.hoverBoxes.add(box);
            } else {
                this.defaultBoxes.add(box);
            }
        }

        MatrixStack matrixStack = worldRenderEvent.getStack();
        if (!this.defaultBoxes.isEmpty()) {
            this.renderBoxBatch(matrixStack, cameraPos, this.defaultBoxes, this.color.getColor());
        }
        if (!this.hoverBoxes.isEmpty()) {
            this.renderBoxBatch(matrixStack, cameraPos, this.hoverBoxes, this.hoverColor.getColor());
        }
        if (!this.hitBoxes.isEmpty()) {
            this.renderBoxBatch(matrixStack, cameraPos, this.hitBoxes, this.hitColor.getColor());
        }
    }

    private void renderBoxBatch(MatrixStack matrixStack, Vec3d cameraPos, List<Box> batch, int col) {
        int outline = this.mode.is(MODE_FILL) ? 0 : col;
        int fill = this.mode.is(MODE_OUTLINE) ? 0 : ColorUtil.multAlpha(col, this.fillOpacity.getFloat());
        WorldShapeRenderer.boxes(
            (VertexConsumerProvider.Immediate) this.mc.getBufferBuilders().getEntityVertexConsumers(),
            matrixStack,
            cameraPos,
            batch,
            fill,
            outline,
            this.lineWidth.getFloat()
        );
    }

    private boolean hasArmor(LivingEntity living) {
        return living.hasStackEquipped(EquipmentSlot.HEAD)
            || living.hasStackEquipped(EquipmentSlot.CHEST)
            || living.hasStackEquipped(EquipmentSlot.LEGS)
            || living.hasStackEquipped(EquipmentSlot.FEET);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == null || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        if (entity == this.mc.player) {
            return this.targets.isSelected(TARGET_SELF) && !this.mc.options.getPerspective().isFirstPerson();
        }
        if (entity.isInvisible() && entity.isInvisibleTo((PlayerEntity)(Object)this.mc.player)) {
            if (!(entity instanceof LivingEntity living) || !this.hasArmor(living)) {
                return false;
            }
        }
        if (entity instanceof PlayerEntity) {
            return this.targets.isSelected(TARGET_PLAYERS);
        }
        return entity instanceof MobEntity && this.targets.isSelected(TARGET_MOBS);
    }
}

