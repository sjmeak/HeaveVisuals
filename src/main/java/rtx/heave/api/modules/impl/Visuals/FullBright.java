package rtx.heave.api.modules.impl.Visuals;

import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.NumberSetting;

public final class FullBright extends Module {
    private static FullBright instance;

    private final NumberSetting gamma = this.register(
        new NumberSetting("Яркость", "Уровень гаммы освещения.", 15.0, 1.0, 25.0, 1.0)
    );

    public FullBright() {
        super("Full Bright", "Максимальная яркость ночного видения без эффектов.", Category.VISUALS);
        instance = this;
    }

    public static FullBright getInstance() {
        return instance;
    }

    public double getGamma() {
        return this.gamma.getValue();
    }
}
