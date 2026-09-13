package rtx.heave.api.modules.impl.Interface;

import java.awt.Color;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;

public final class CooldownsModule extends InterfaceComponentModule {
    public final ModeSetting displayMode = this.register(
        new ModeSetting("Отображение", "Где отображать кулдауны предметов.", "HUD", "HUD", "Хотбар", "Оба")
    );
    public final BooleanSetting showInInventory = this.register(
        new BooleanSetting("В инвентаре", "Отображать секунды до окончания перезарядки в слотах инвентаря.", true)
    );
    public final ColorSetting cooldownColor = this.register(
        new ColorSetting("Цвет таймера", "Цвет цифр кулдауна в хотбаре и инвентаре.", new Color(255, 255, 255, 255))
    );

    public CooldownsModule() {
        super("Cooldowns", "Перемещаемый список перезарядки предметов и таймеры в хотбаре и инвентаре.");
    }
}
