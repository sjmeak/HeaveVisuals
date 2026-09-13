package rtx.heave.api.modules.impl.Interface;

import rtx.heave.api.modules.impl.Interface.InterfaceComponentModule;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;

public final class InventoryModule extends InterfaceComponentModule {
    private final BooleanSetting background = this.register(new BooleanSetting("Фон", "Отображать фон инвентаря.", true));
    private final SliderSetting customScale = this.register(new SliderSetting("Размер", "Масштаб инвентаря.").setValue(1.0f).range(0.5f, 2.0f).increment(0.05f));

    public InventoryModule() {
        super("Inventory", "Сетка предметов инвентаря с разделительными линиями.");
    }

    public boolean hasBackground() {
        return this.background.getValue();
    }

    public float getCustomScale() {
        return this.customScale.getFloat();
    }
}
