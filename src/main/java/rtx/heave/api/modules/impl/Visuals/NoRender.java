package rtx.heave.api.modules.impl.Visuals;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.MultiModeSetting;

public class NoRender
extends Module {
    public static final String FIRE = "Огонь";
    public static final String ENTITY_FIRE = "Огонь на сущностях";
    public static final String CAMERA_SHAKE = "Тряска камеры";
    public static final String FOV_DYNAMIC = "Динамика поля зрения";
    public static final String SCOREBOARD = "Таблица счёта";
    public static final String BOSS_BAR = "Полоса босса";
    private static NoRender instance;
    private final MultiModeSetting elements = this.register(new MultiModeSetting("Элементы", "Какие элементы рендера скрывать.", new String[]{FIRE, ENTITY_FIRE, CAMERA_SHAKE, FOV_DYNAMIC, SCOREBOARD, BOSS_BAR}, FIRE, ENTITY_FIRE, CAMERA_SHAKE));

    public NoRender() {
        super("No Render", "\u0421\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0435 \u0432\u0438\u0437\u0443\u0430\u043b\u044c\u043d\u044b\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u044b.", Category.VISUALS);
        instance = this;
    }

    public static NoRender getInstance() {
        return instance;
    }

    public static boolean isActive(String string) {
        if (instance == null || !instance.isEnabled()) {
            return false;
        }
        instance.ensureSelectedDefaults();
        return NoRender.instance.elements.isSelected(string);
    }

    private void ensureSelectedDefaults() {
        if (this.elements.getSelected().isEmpty()) {
            this.elements.selected(FIRE, ENTITY_FIRE, CAMERA_SHAKE);
        }
    }
}

