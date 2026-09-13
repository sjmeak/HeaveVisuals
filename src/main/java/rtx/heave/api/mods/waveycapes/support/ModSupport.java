package rtx.heave.api.mods.waveycapes.support;

import rtx.heave.api.mods.waveycapes.CapeRenderer;
import rtx.heave.api.mods.waveycapes.compat.PlayerWrapper;

public interface ModSupport {
    boolean shouldBeUsed(PlayerWrapper playerWrapper);
    CapeRenderer getRenderer();
    default boolean blockFeatureRenderer(Object featureRenderer) {
        return false;
    }
}