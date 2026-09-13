package rtx.heave.api.modules.impl.Visuals;

import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;

public final class NameTags extends Module {
    private static NameTags instance;

    public final BooleanSetting removeBackground = this.register(
        new BooleanSetting("Без фона", "Убрать темный прямоугольный фон под никнеймом.", true)
    );
    public final BooleanSetting shadow = this.register(
        new BooleanSetting("Тень", "Отображать тень у текста никнейма (выключите, чтобы убрать).", false)
    );
    public final BooleanSetting addShadow = this.shadow;
    public final BooleanSetting invisibles = this.register(
        new BooleanSetting("Невидимые игроки", "Отображать никнеймы невидимых игроков.", true)
    );
    public final BooleanSetting throughWalls = this.register(
        new BooleanSetting("Сквозь стены", "Всегда отображать никнеймы сквозь стены (в т.ч. на шифте).", false)
    );

    public NameTags() {
        super("NameTags", "Настройки внешнего вида никнеймов игроков.", Category.VISUALS);
        instance = this;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public static NameTags getInstance() {
        return instance;
    }
}
