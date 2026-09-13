package rtx.heave.api.modules.impl.Interface;

import java.awt.Color;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;
import rtx.heave.api.modules.settings.impl.SelectSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.color.ColorUtil;

public final class InterfaceModule extends Module {
    private static InterfaceModule instance;

    private final SeparatorSetting themeSeparator = this.register(new SeparatorSetting("Тема и цвета"));
    public final SelectSetting themeMode = this.register(
        new SelectSetting("Тема", "Выбор темы оформления интерфейса: Дефолтная (Frost) или Кастомная.")
            .value("Дефолтная", "Кастомная")
            .selected("Дефолтная")
    );
    public final ColorSetting customMainColor = this.register(
        new ColorSetting("Основной цвет", "Цвет фона меню ClickGUI и карточек HUD.", new Color(15, 15, 22, 255))
            .visible(() -> this.themeMode.is("Кастомная"))
    );
    public final ColorSetting customAccentColor = this.register(
        new ColorSetting("Второстепенный цвет", "Цвет акцента, шрифтов HUD и выбора модулей.", new Color(124, 180, 255, 255))
            .visible(() -> this.themeMode.is("Кастомная"))
    );
    public final BooleanSetting useSecondColor = this.register(
        new BooleanSetting("Второй цвет", "Включить второй цвет для градиента.", false)
            .visible(() -> this.themeMode.is("Кастомная"))
    );
    public final ColorSetting customColor2 = this.register(
        new ColorSetting("Цвет 2", "Второй цвет градиента.", new Color(160, 230, 255, 255))
            .visible(() -> this.themeMode.is("Кастомная") && this.useSecondColor.getValue())
    );

    // Alias customColor1 to customAccentColor for backward compatibility
    public final ColorSetting customColor1 = this.customAccentColor;

    private final SeparatorSetting visualSeparator = this.register(new SeparatorSetting("Прозрачность и вид"));
    public final SliderSetting hudAlpha = this.register(
        new SliderSetting("Прозрачность худов", "Прозрачность всех элементов HUD.")
            .range(0.30f, 1.0f).increment(0.05f).setValue(0.90f)
    );
    public final SliderSetting hudCornerRadius = this.register(
        new SliderSetting("Скругление худов", "Радиус скругления углов карточек интерфейса.")
            .range(2.0f, 12.0f).increment(1.0f).setValue(6.0f)
    );
    public final BooleanSetting hudBorder = this.register(
        new BooleanSetting("Обводка худов", "Включить или выключить контур/обводку интерфейсных окон.", false)
    );
    public final SliderSetting hudBorderSize = this.register(
        new SliderSetting("Размер обводки", "Толщина линий обводки худов.")
            .range(0.5f, 3.0f).increment(0.5f).setValue(1.0f)
            .visibleWhen(() -> this.hudBorder.getValue())
    );
    public final ColorSetting hudBorderColor = this.register(
        new ColorSetting("Цвет обводки", "Цвет линий контура окон.", new Color(255, 255, 255, 60))
            .visibleWhen(() -> this.hudBorder.getValue())
    );

    private final SeparatorSetting dragSeparator = this.register(new SeparatorSetting("Перетаскивание"));
    public final ModeSetting dragStyle = this.register(
        new ModeSetting("Перетаскивание", "Стиль перемещения элементов HUD.", "Обычный", "Обычный", "Проекция")
    );
    public final BooleanSetting dragTilt = this.register(
        new BooleanSetting("Наклон при перетаскивании", "Плавно наклоняет элемент в сторону движения.", true)
            .visibleWhen(() -> this.dragStyle.is("Обычный"))
    );

    // Backward compatibility fields for legacy callers
    public final NumberSetting rectCornerRadius = new NumberSetting("Скругление", "", 6.0, 2.0, 12.0, 1.0);
    public final NumberSetting rectBackdropBlur = new NumberSetting("Размытие фона", "", 18.0, 0.0, 64.0, 1.0);
    public final NumberSetting rectRefractionStrength = new NumberSetting("Преломление", "", 0.0, 0.0, 0.8, 0.01);
    public final NumberSetting rectEdgeStrength = new NumberSetting("Сила края", "", 0.18, 0.0, 1.0, 0.01);
    public final NumberSetting rectEdgeSharpness = new NumberSetting("Резкость края", "", 55.0, 2.0, 100.0, 1.0);
    public final BooleanSetting rectGlow = new BooleanSetting("Свечение", "", false);
    public final NumberSetting rectGlowIntensity = new NumberSetting("Яркость свечения", "", 0.6, 0.0, 2.0, 0.05);
    public final NumberSetting rectGlowRadius = new NumberSetting("Радиус свечения", "", 15.0, 15.0, 70.0, 1.0);
    public final BooleanSetting rectUseSecondColor = this.useSecondColor;
    public final ColorSetting rectColor = this.customColor1;
    public final ColorSetting rectSecondColor = this.customColor2;
    public final BooleanSetting rectColorMovement = new BooleanSetting("Движение цвета", "", false);
    public final BooleanSetting dragJitter = new BooleanSetting("Тряска", "", false);
    public final BooleanSetting dragWaves = new BooleanSetting("Волны", "", false);
    public final BooleanSetting hudIcons = new BooleanSetting("Иконки", "", false);
    public final ModeSetting gradientStyle = new ModeSetting("Градиент", "", "Горизонтальный", "Горизонтальный");
    public final ModeSetting clientColorMode = new ModeSetting("Цвет клиента", "", "Темы", "Темы");
    public final SliderSetting rainbowSpeed = new SliderSetting("Скорость радуги", "").range(0.1f, 5.0f).setValue(1.0f);
    public final SliderSetting rainbowSpread = new SliderSetting("Разброс радуги", "").range(0.01f, 1.0f).setValue(0.18f);
    public final SliderSetting rainbowSaturation = new SliderSetting("Насыщенность радуги", "").range(0.0f, 1.0f).setValue(0.85f);

    public InterfaceModule() {
        super("Interface", "Настройка темы, цветов и прозрачности меню и худов.", Category.DISPLAY);
        instance = this;
    }

    public static InterfaceModule getInstance() {
        return instance;
    }

    public boolean isCustomTheme() {
        return this.themeMode.is("Кастомная");
    }

    public int getCustomMainColor() {
        return this.customMainColor.getColor();
    }

    public int getCustomAccentColor() {
        return this.customAccentColor.getColor();
    }

    public int getCustomColor1() {
        return this.customAccentColor.getColor();
    }

    public boolean hasSecondColor() {
        return this.useSecondColor.getValue();
    }

    public int getCustomColor2() {
        return this.customColor2.getColor();
    }

    public float getHudAlpha() {
        return this.hudAlpha.getFloat();
    }

    public float getCornerRadius() {
        return this.hudCornerRadius.getFloat();
    }

    public int getCustomAccent(float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        if (!this.useSecondColor.getValue()) {
            return (a << 24) | (this.customColor1.getColor() & 0xFFFFFF);
        }
        float progress = (float)(System.currentTimeMillis() % 4000L) / 4000.0f;
        float factor = 0.5f - 0.5f * (float)Math.cos(progress * 2.0 * Math.PI);
        int blended = ColorUtil.lerpColor(this.customColor1.getColor(), this.customColor2.getColor(), factor);
        return (a << 24) | (blended & 0xFFFFFF);
    }

    public int getCustomAccentBright(float alpha) {
        int col = this.getCustomAccent(255.0f);
        int bright = ColorUtil.lerpColor(col, 0xFFFFFF, 0.25f);
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        return (a << 24) | (bright & 0xFFFFFF);
    }

    public int getCustomAccentSoft(float alpha) {
        int col = this.getCustomAccent(255.0f);
        int soft = ColorUtil.lerpColor(col, 0x000000, 0.35f);
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        return (a << 24) | (soft & 0xFFFFFF);
    }

    public int getCustomAccentFill(float alpha) {
        int col = this.getCustomAccent(255.0f);
        int fill = ColorUtil.lerpColor(col, 0x000000, 0.55f);
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        return (a << 24) | (fill & 0xFFFFFF);
    }

    public int getCustomGradientA(float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        return (a << 24) | (this.customColor1.getColor() & 0xFFFFFF);
    }

    public int getCustomGradientB(float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        int col = this.useSecondColor.getValue() ? this.customColor2.getColor() : this.customColor1.getColor();
        return (a << 24) | (col & 0xFFFFFF);
    }

    public int clientPrimaryColor() {
        if (this.isCustomTheme()) {
            return this.getCustomAccent(204.0f);
        }
        return ThemeManager.accent(204.0f);
    }

    public int clientPrimaryColorOpaque() {
        return 0xFF000000 | (this.clientPrimaryColor() & 0xFFFFFF);
    }

    public int clientSecondaryColor() {
        if (this.isCustomTheme()) {
            return this.getCustomGradientB(204.0f);
        }
        return ThemeManager.gradientB(204.0f);
    }

    public int clientSecondaryColorOpaque() {
        return 0xFF000000 | (this.clientSecondaryColor() & 0xFFFFFF);
    }

    public boolean usesSecondClientColor() {
        return this.isCustomTheme() && this.useSecondColor.getValue();
    }

    public boolean clientColorMovement() {
        return this.usesSecondClientColor();
    }

    public boolean isRainbowClientColor() {
        return false;
    }

    public boolean isThemeClientColor() {
        return !this.isCustomTheme();
    }

    public boolean isCustomClientColor() {
        return this.isCustomTheme();
    }

    public int gradientStyleId() {
        return 0;
    }

    public int[] clientPalette() {
        if (this.isCustomTheme()) {
            if (this.useSecondColor.getValue()) {
                return new int[]{this.customColor1.getColor() & 0xFFFFFF, this.customColor2.getColor() & 0xFFFFFF};
            }
            return new int[]{this.customColor1.getColor() & 0xFFFFFF};
        }
        return ThemeManager.blendedPalette();
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }
}

