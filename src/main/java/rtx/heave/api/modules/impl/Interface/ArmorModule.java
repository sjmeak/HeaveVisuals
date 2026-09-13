package rtx.heave.api.modules.impl.Interface;

import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;

public final class ArmorModule extends InterfaceComponentModule {
    public static final String ORIENT_AUTO = "Авто";
    public static final String ORIENT_HORIZ = "Горизонтально";
    public static final String ORIENT_VERT = "Вертикально";

    public static final String DUR_HIDDEN = "Скрыто";
    public static final String DUR_PERCENT = "Проценты";
    public static final String DUR_VALUE = "Прочность";

    public static final String SIDE_AUTO = "Авто";
    public static final String SIDE_LEFT = "Слева";
    public static final String SIDE_RIGHT = "Справа";

    public static final String STYLE_NORMAL = "Обычный";
    public static final String STYLE_UKU = "uku";

    public final ModeSetting style = this.register(
        new ModeSetting("Стиль", "Стиль отображения брони: Обычный или uku (как в 1.21.8).", STYLE_NORMAL, STYLE_NORMAL, STYLE_UKU)
    );

    public final BooleanSetting ukuShowDamage = this.register(
        new BooleanSetting("Отображать прочность", "Показывать числовую прочность над предметами.", true)
            .visibleWhen(() -> this.style.is(STYLE_UKU))
    );
    public final ModeSetting ukuDamageFormat = this.register(
        new ModeSetting("Формат прочности", "Вид отображения прочности.", "Число", "Число", "Проценты")
            .visibleWhen(() -> this.style.is(STYLE_UKU) && this.ukuShowDamage.getValue())
    );
    public final ModeSetting ukuDamageColor = this.register(
        new ModeSetting("Цвет прочности", "Цветовая схема чисел прочности.", "3 Цвета", "3 Цвета", "Цвет бара", "Белый")
            .visibleWhen(() -> this.style.is(STYLE_UKU) && this.ukuShowDamage.getValue())
    );
    public final BooleanSetting ukuBackground = this.register(
        new BooleanSetting("Фон хотбара", "Отображать классические слоты хотбара под броней.", true)
            .visibleWhen(() -> this.style.is(STYLE_UKU))
    );
    public static final String UKU_SIDE_RIGHT = "Справа";
    public static final String UKU_SIDE_LEFT = "Слева";
    public final ModeSetting ukuSide = this.register(
        new ModeSetting("Сторона (uku)", "Расположение uku-панели относительно хотбара.", UKU_SIDE_RIGHT, UKU_SIDE_RIGHT, UKU_SIDE_LEFT)
            .visibleWhen(() -> this.style.is(STYLE_UKU))
    );

    // Backward-compat aliases
    public final BooleanSetting ukuShowHands = new BooleanSetting("Предметы в руках", "", false);
    public final BooleanSetting ukuDurabilityBar = new BooleanSetting("Полоска прочности", "", false);
    public final ModeSetting ukuTextMode = new ModeSetting("Индикатор прочности", "", DUR_VALUE, DUR_VALUE);

    public final ModeSetting orientation = this.register(
        new ModeSetting("Ориентация", "Расположение элементов брони.", ORIENT_AUTO, ORIENT_AUTO, ORIENT_HORIZ, ORIENT_VERT)
            .visibleWhen(() -> this.style.is(STYLE_NORMAL))
    );
    public final ModeSetting side = this.register(
        new ModeSetting("Позиция", "Сторона экрана для вертикального режима.", SIDE_AUTO, SIDE_AUTO, SIDE_LEFT, SIDE_RIGHT)
            .visibleWhen(() -> this.style.is(STYLE_NORMAL))
    );
    public final ModeSetting durabilityMode = this.register(
        new ModeSetting("Прочность", "Режим отображения прочности брони.", DUR_PERCENT, DUR_HIDDEN, DUR_PERCENT, DUR_VALUE)
            .visibleWhen(() -> this.style.is(STYLE_NORMAL))
    );
    public final BooleanSetting background = this.register(
        new BooleanSetting("Фон", "Отображать ли тёмный фон за слотами брони.", true)
            .visibleWhen(() -> this.style.is(STYLE_NORMAL))
    );
    public final BooleanSetting vanillaFont = this.register(
        new BooleanSetting("Обычный шрифт", "Использовать стандартный шрифт Minecraft для прочности.", false)
            .visibleWhen(() -> this.style.is(STYLE_NORMAL))
    );

    public ArmorModule() {
        super("Armor", "Отображение надетой брони с настройками ориентации, прочности и фона.");
    }
}


