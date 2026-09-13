package rtx.heave.api.mods.geckolib.renderer;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import rtx.heave.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.heave.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.heave.api.mods.geckolib.renderer.base.GeoRenderer;

public abstract class GeoReplacedEntityRenderer<T extends GeoAnimatable, E extends Entity, R extends EntityRenderState & GeoRenderState> implements GeoRenderer<T, E, R> {
    public T getAnimatable() {
        return null;
    }
}