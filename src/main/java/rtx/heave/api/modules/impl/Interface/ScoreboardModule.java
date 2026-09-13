package rtx.heave.api.modules.impl.Interface;

import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;

public final class ScoreboardModule extends Module {
    private static ScoreboardModule instance;

    public final BooleanSetting noScoreboard = this.register(
        new BooleanSetting("Скрыть скорборд", "Полностью отключить отображение скорборда.", false)
    );
    public final BooleanSetting noNumber = this.register(
        new BooleanSetting("Скрыть цифры", "Убрать красные цифры счетов справа.", true)
    );
    public final NumberSetting scale = this.register(
        new NumberSetting("Масштаб", "Масштаб панели скорборда.", 1.0, 0.5, 1.5, 0.05)
    );

    public ScoreboardModule() {
        super("Scoreboard", "Настройки отображения скорборда (масштаб, скрытие цифр).", Category.DISPLAY);
        instance = this;
    }

    public static ScoreboardModule getInstance() {
        return instance;
    }
}
