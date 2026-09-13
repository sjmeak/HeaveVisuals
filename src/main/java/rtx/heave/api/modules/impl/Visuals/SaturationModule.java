package rtx.heave.api.modules.impl.Visuals;

import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;

public final class SaturationModule extends Module {
    private static SaturationModule instance;

    public final BooleanSetting previewFood = this.register(
        new BooleanSetting("Предпросмотр еды", "Показывает предварительное насыщение при удержании еды в руке.", true)
    );
    public final BooleanSetting airOnTop = this.register(
        new BooleanSetting("Пузыри поверх", "Отображать пузыри воздуха поверх полоски насыщения.", true)
    );

    public SaturationModule() {
        super("Saturation", "Отображает полоску насыщения еды над хотбаром.", Category.DISPLAY);
        instance = this;
    }

    public static SaturationModule getInstance() {
        return instance;
    }

    public boolean isPreviewFood() {
        return this.previewFood.getValue();
    }

    public boolean isAirOnTop() {
        return this.airOnTop.getValue();
    }
}
