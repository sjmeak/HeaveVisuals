package rtx.heave.api.modules.impl.Interface;

import java.awt.Color;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;

public final class InfoModule extends InterfaceComponentModule {
    private final SeparatorSetting displaySep = this.register(new SeparatorSetting("Отображение"));
    public final BooleanSetting showBps = this.register(new BooleanSetting("BPS", "Отображать скорость перемещения.", true));
    public final BooleanSetting showXyz = this.register(new BooleanSetting("XYZ", "Отображать координаты игрока.", true));
    public final BooleanSetting showNether = this.register(new BooleanSetting("Nether XYZ", "Отображать координаты в другом измерении.", true).visible(this.showXyz::getValue));
    public final BooleanSetting showFps = this.register(new BooleanSetting("FPS", "Отображать текущий FPS.", true));
    public final BooleanSetting showTps = this.register(new BooleanSetting("TPS", "Отображать тикрейт сервера.", true));
    public final BooleanSetting showPing = this.register(new BooleanSetting("Ping", "Отображать пинг игрока.", true));

    private final SeparatorSetting colorSep = this.register(new SeparatorSetting("Цвета"));
    public final ColorSetting labelColor = this.register(new ColorSetting("Цвет названий", "Цвет префиксов параметров.", new Color(200, 200, 208, 255)));
    public final ColorSetting valueColor = this.register(new ColorSetting("Цвет значений", "Цвет значений параметров.", new Color(78, 196, 255, 255)));

    public InfoModule() {
        super("Info", "Статичная сводка на экране (bps, координаты, пинг, tps).");
    }
}