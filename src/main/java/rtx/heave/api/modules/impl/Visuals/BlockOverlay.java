package rtx.heave.api.modules.impl.Visuals;

import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.world.WorldShapeRenderer;
import java.awt.Color;

public final class BlockOverlay extends Module {

    private final SeparatorSetting outlineGroup = this.register(new SeparatorSetting("Обводка"));
    public final BooleanSetting outlineEnabled = this.register(new BooleanSetting("Обводка", "Включить обводку наведенного блока.", true));
    public final SliderSetting lineWidth = this.register(
        new SliderSetting("Толщина линий", "Толщина линий обводки блока.").range(1.0F, 5.0F).increment(0.5F).setValue(2.0F)
            .visibleWhen(() -> this.outlineEnabled.getValue())
    );

    private final SeparatorSetting fillGroup = this.register(new SeparatorSetting("Заливка"));
    public final BooleanSetting fillEnabled = this.register(new BooleanSetting("Заливка", "Включить заливку наведенного блока.", false));
    public final SliderSetting fillAlpha = this.register(
        new SliderSetting("Прозрачность заливки", "Прозрачность заливки.").range(0.05F, 1.0F).increment(0.05F).setValue(0.3F)
            .visibleWhen(() -> this.fillEnabled.getValue())
    );

    private final SeparatorSetting colorGroup = this.register(new SeparatorSetting("Цвет"));
    public final BooleanSetting useClientColor = this.register(new BooleanSetting("Цвет клиента", "Использовать цвет клиента вместо кастомного.", true));
    public final ColorSetting customColor = this.register(
        new ColorSetting("Кастомный цвет", "Пользовательский цвет выделения блока.", Color.WHITE)
            .visibleWhen(() -> !this.useClientColor.getValue())
    );

    public BlockOverlay() {
        super("Block Overlay", "Красиво выделяет блок, на который наведен игрок", Category.VISUALS);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (this.mc == null || this.mc.world == null || this.mc.player == null) {
            return;
        }
        if (this.mc.options.hudHidden) {
            return;
        }
        if (!(this.mc.crosshairTarget instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = this.mc.world.getBlockState(blockPos);
        if (blockState.isAir()) {
            return;
        }

        VoxelShape shape = blockState.getOutlineShape(this.mc.world, blockPos);
        if (shape.isEmpty()) {
            return;
        }

        // Box in world coordinates (WorldShapeRenderer will subtract camera pos)
        Box worldBox = shape.getBoundingBox().offset(blockPos);

        Camera camera = event.getCamera() != null ? event.getCamera() : this.mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        MatrixStack matrixStack = event.getStack();

        int baseColor = this.overlayColor();

        int outlineColor = this.outlineEnabled.getValue() ? baseColor : 0;
        int fillColor = this.fillEnabled.getValue() ? ColorUtil.multAlpha(baseColor, this.fillAlpha.getFloat()) : 0;

        List<Box> boxes = Collections.singletonList(worldBox);

        WorldShapeRenderer.boxes(
            (VertexConsumerProvider.Immediate) this.mc.getBufferBuilders().getEntityVertexConsumers(),
            matrixStack,
            cameraPos,
            boxes,
            fillColor,
            outlineColor,
            this.lineWidth.getFloat()
        );
    }

    private int overlayColor() {
        if (this.useClientColor.getValue()) {
            InterfaceModule ui = InterfaceModule.getInstance();
            if (ui != null) {
                return ui.clientPrimaryColorOpaque();
            }
            return -1; // white
        }
        return this.customColor.getColor();
    }
}
