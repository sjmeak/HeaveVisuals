package rtx.heave.api.modules.impl.Interface;

import java.awt.Color;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;

public final class PotionsModule extends InterfaceComponentModule {

    private final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Стиль отображения эффектов.", "Vanilla", "Current", "Vanilla")
    );

    // Vanilla (LabyMod 3) settings
    private final ColorSetting nameColor = this.register(
        new ColorSetting("Цвет названия", "Цвет названия эффекта.", new Color(255, 255, 255, 255))
            .visibleWhen(() -> this.mode.is("Vanilla"))
    );
    private final ColorSetting amplifierColor = this.register(
        new ColorSetting("Цвет уровня", "Цвет уровня эффекта.", new Color(170, 170, 170, 255))
            .visibleWhen(() -> this.mode.is("Vanilla"))
    );
    private final ColorSetting durationColor = this.register(
        new ColorSetting("Цвет времени", "Цвет оставшегося времени эффекта.", new Color(127, 127, 127, 255))
            .visibleWhen(() -> this.mode.is("Vanilla"))
    );
    private final BooleanSetting background = this.register(
        new BooleanSetting("Задний фон", "Отображать фон позади эффектов.", false)
            .visibleWhen(() -> this.mode.is("Vanilla"))
    );
    private final ColorSetting backgroundColor = this.register(
        new ColorSetting("Цвет фона", "Цвет заднего фона.", new Color(0, 0, 0, 100))
            .visibleWhen(() -> this.mode.is("Vanilla") && this.background.getValue())
    );
    private final BooleanSetting rightBound = this.register(
        new BooleanSetting("Справа налево", "Выравнивание иконки справа и текста влево.", false)
            .visibleWhen(() -> this.mode.is("Vanilla"))
    );
    private final SliderSetting scale = this.register(
        new SliderSetting("Масштаб", "Масштаб отображения эффектов (%).")
            .setValue(100.0f).range(50, 150).increment(5)
            .visible(() -> this.mode.is("Vanilla"))
    );

    public PotionsModule() {
        super("Potions", "Перемещаемый список активных эффектов.");
    }

    public boolean isVanilla() {
        return this.mode.is("Vanilla");
    }

    public int getNameColor() {
        return this.nameColor.getColor();
    }

    public int getAmplifierColor() {
        return this.amplifierColor.getColor();
    }

    public int getDurationColor() {
        return this.durationColor.getColor();
    }

    public boolean hasBackground() {
        return this.background.getValue();
    }

    public int getBackgroundColor() {
        return this.backgroundColor.getColor();
    }

    public boolean isRightBound() {
        return this.rightBound.getValue();
    }

    public float getCustomScale() {
        return this.scale.getFloat() / 100.0f;
    }
}
