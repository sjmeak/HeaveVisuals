package rtx.heave.api.modules.impl.Visuals;

import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;

public final class TimeChanger extends Module {
    private static TimeChanger instance;

    private final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Режим визуального времени.", "День", "День", "Ночь", "Закат", "Кастомное", "Быстрое")
    );
    private final NumberSetting customTime = this.register(
        new NumberSetting("Время", "Время суток (0 - 24000).", 6000.0, 0.0, 24000.0, 500.0)
    );
    private final NumberSetting cycleSpeed = this.register(
        new NumberSetting("Скорость", "Скорость анимации времени суток.", 20.0, 1.0, 100.0, 1.0)
    );

    public TimeChanger() {
        super("Time Changer", "Визуальная смена времени суток в мире.", Category.VISUALS);
        instance = this;
        this.customTime.visibleWhen(() -> this.mode.is("Кастомное"));
        this.cycleSpeed.visibleWhen(() -> this.mode.is("Быстрое"));
    }

    public static TimeChanger getInstance() {
        return instance;
    }

    public long modifyTime(long original) {
        if (!this.isEnabled()) return original;
        return switch (this.mode.getSelected()) {
            case "День" -> 6000L;
            case "Ночь" -> 18000L;
            case "Закат" -> 12000L;
            case "Кастомное" -> (long) this.customTime.getValue();
            case "Быстрое" -> (original + (long)(System.currentTimeMillis() * (this.cycleSpeed.getValue() / 10.0))) % 24000L;
            default -> 6000L;
        };
    }
}
