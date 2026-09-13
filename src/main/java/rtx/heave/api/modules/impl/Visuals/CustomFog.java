package rtx.heave.api.modules.impl.Visuals;

import java.awt.Color;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;

public final class CustomFog extends Module {
    private static CustomFog instance;

    private final NumberSetting fogDistance = this.register(
        new NumberSetting("Дистанция", "Начало тумана.", 10.0, -20.0, 256.0, 1.0)
    );
    private final NumberSetting fogDensity = this.register(
        new NumberSetting("Плотность", "Глубина тумана от начала.", 16.0, 1.0, 128.0, 1.0)
    );
    private final BooleanSetting useColor = this.register(
        new BooleanSetting("Свой цвет", "Использовать кастомный цвет тумана.", true)
    );
    private final ColorSetting fogColor = this.register(
        new ColorSetting("Цвет", "Цвет тумана.", new Color(0x6B8AFD))
    );

    public CustomFog() {
        super("Custom Fog", "Кастомизация дистанции, плотности и цвета тумана.", Category.VISUALS);
        instance = this;
        this.fogColor.visibleWhen(this.useColor::getValue);
    }

    public static CustomFog getInstance() {
        return instance;
    }

    public float getStartDistance() {
        return this.fogDistance.getFloat();
    }

    public float getEndDistance() {
        return this.fogDistance.getFloat() + this.fogDensity.getFloat();
    }

    public boolean useCustomColor() {
        return this.useColor.getValue();
    }

    public Color getFogColor() {
        return new Color(this.fogColor.getColor(), true);
    }
}
