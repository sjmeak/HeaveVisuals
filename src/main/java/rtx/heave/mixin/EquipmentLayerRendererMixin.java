package rtx.heave.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.heave.api.modules.impl.Visuals.HitColor;
import rtx.heave.mixin.accessor.RenderLayerAccessor;
import rtx.heave.mixin.accessor.RenderSetupAccessor;

@Mixin(EquipmentRenderer.class)
public abstract class EquipmentLayerRendererMixin {
    @WrapOperation(
        method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        ),
        require = 1
    )
    private void heave_tintHitArmor(
        RenderCommandQueue collector,
        Model model,
        Object state,
        MatrixStack poseStack,
        RenderLayer renderType,
        int light,
        int overlay,
        int color,
        Sprite sprite,
        int outlineColor,
        ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
        Operation<Void> original
    ) {
        int finalOverlay = overlay;
        RenderLayer finalRenderLayer = renderType;

        if (state instanceof LivingEntityRenderState livingState) {
            if (HitColor.affectArmor() && livingState.hurt) {
                boolean isPlayer = state instanceof PlayerEntityRenderState;
                if (isPlayer || HitColor.affectsMobs()) {
                    if (!renderType.equals(RenderLayers.armorEntityGlint()) && !renderType.toString().contains("glint")) {
                        finalOverlay = net.minecraft.client.render.OverlayTexture.packUv(0, 3);
                        Identifier tex = heave_extractTexture(renderType);
                        if (tex != null) {
                            finalRenderLayer = RenderLayers.entityCutoutNoCullZOffset(tex, true);
                        }
                    }
                }
            }
        }
        original.call(collector, model, state, poseStack, finalRenderLayer, light, finalOverlay, color, sprite, outlineColor, crumblingOverlay);
    }

    private static Identifier heave_extractTexture(RenderLayer layer) {
        if (layer == null) return null;
        try {
            RenderSetup setup = ((RenderLayerAccessor) (Object) layer).heave$getRenderSetup();
            if (setup == null) return null;
            Map<String, ?> textures = ((RenderSetupAccessor) (Object) setup).heave$getTextures();
            if (textures == null) return null;
            Object spec = textures.get("Sampler0");
            if (spec instanceof Record rec) {
                for (java.lang.reflect.RecordComponent comp : rec.getClass().getRecordComponents()) {
                    if (Identifier.class.isAssignableFrom(comp.getType())) {
                        return (Identifier) comp.getAccessor().invoke(rec);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

