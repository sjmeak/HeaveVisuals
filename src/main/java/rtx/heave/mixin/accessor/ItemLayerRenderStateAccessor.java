package rtx.heave.mixin.accessor;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    @Accessor("glint")
    public ItemRenderState.Glint heave_getFoilType();

    @Accessor("useLight")
    public boolean heave_getUsesBlockLight();

    @Accessor("specialModelType")
    public SpecialModelRenderer<Object> heave_getSpecialRenderer();

    @Accessor("transform")
    public Transformation heave_getItemTransform();

    @Accessor("tints")
    public int[] heave_getTintLayers();
}
