package rtx.heave.api.mods.geckolib;
import net.fabricmc.api.ModInitializer;
import rtx.heave.api.mods.geckolib.GeckoLibConstants;
import rtx.heave.api.mods.geckolib.service.GeckoLibNetworking;

public final class GeckoLib
implements ModInitializer {
    public void onInitialize() {
        GeckoLibConstants.init();
        GeckoLibNetworking.init();
    }
}

