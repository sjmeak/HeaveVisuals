package rtx.heave.api.modules.impl.Interface;

import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;

public final class WatermarkModule extends InterfaceComponentModule {
    public static final String ITEM_LOGO = "Лого";
    public static final String ITEM_NICK = "Ник";
    public static final String ITEM_TIME = "Время";
    public static final String ITEM_FPS = "ФПС";
    public static final String ITEM_PING = "Пинг";
    public static final String ITEM_NONE = "Пусто";

    public static final String ANCHOR_TOP_LEFT = "Слева сверху";
    public static final String ANCHOR_TOP_CENTER = "Посередине";
    public static final String ANCHOR_TOP_RIGHT = "Справа сверху";
    public static final String ANCHOR_FREE = "Свободная";

    public static final String EXT_JAVA = "java";
    public static final String EXT_CPP = "cpp";
    public static final String EXT_PRO = "pro";
    public static final String EXT_DLL = "dll";
    public static final String EXT_EXE = "exe";
    public static final String EXT_TXT = "txt";

    private static final String[] ITEMS_P1 = new String[]{ITEM_LOGO, ITEM_NICK, ITEM_TIME, ITEM_FPS, ITEM_PING, ITEM_NONE};
    private static final String[] ITEMS_P23 = new String[]{ITEM_NICK, ITEM_TIME, ITEM_FPS, ITEM_PING, ITEM_NONE};
    private static final String[] ANCHORS = new String[]{ANCHOR_TOP_LEFT, ANCHOR_TOP_CENTER, ANCHOR_TOP_RIGHT, ANCHOR_FREE};
    private static final String[] EXTENSIONS = new String[]{EXT_JAVA, EXT_CPP, EXT_PRO, EXT_DLL, EXT_EXE, EXT_TXT};

    private final SeparatorSetting anchorSep = this.register(new SeparatorSetting("Закрепление"));
    public final ModeSetting anchor = this.register(
        new ModeSetting("Позиция", "Закрепление вотермарка сверху экрана.", ANCHOR_TOP_LEFT, ANCHORS)
    );


    private final SeparatorSetting logoSep = this.register(new SeparatorSetting("Логотип"));
    public final ModeSetting extension = this.register(
        new ModeSetting("Расширение", "Расширение названия клиента в логотипе.", EXT_JAVA, EXTENSIONS)
    );

    private final SeparatorSetting slotsSep = this.register(new SeparatorSetting("Слоты"));
    public final ModeSetting pos1 = this.register(new ModeSetting("Позиция 1", "Элемент в левой позиции Watermark.", ITEM_LOGO, ITEMS_P1));
    public final ModeSetting pos2 = this.register(new ModeSetting("Позиция 2", "Элемент в средней позиции Watermark.", ITEM_NICK, ITEMS_P23));
    public final ModeSetting pos3 = this.register(new ModeSetting("Позиция 3", "Элемент в правой позиции Watermark.", ITEM_TIME, ITEMS_P23));


    private String lastPos1 = ITEM_LOGO;
    private String lastPos2 = ITEM_NICK;
    private String lastPos3 = ITEM_TIME;

    public WatermarkModule() {
        super("Watermark", "Вотермарк клиента с настраиваемыми слотами и закреплением сверху.");
    }

    public void updatePositions() {
        String p1 = pos1.getSelected();
        String p2 = pos2.getSelected();
        String p3 = pos3.getSelected();

        if (!p1.equals(lastPos1)) {
            if (!p1.equals(ITEM_NONE)) {
                if (p2.equals(p1)) {
                    pos2.setSelected(lastPos1.equals(p1) ? ITEM_NONE : lastPos1);
                } else if (p3.equals(p1)) {
                    pos3.setSelected(lastPos1.equals(p1) ? ITEM_NONE : lastPos1);
                }
            }
            lastPos1 = pos1.getSelected();
            lastPos2 = pos2.getSelected();
            lastPos3 = pos3.getSelected();
        } else if (!p2.equals(lastPos2)) {
            if (!p2.equals(ITEM_NONE)) {
                if (p1.equals(p2)) {
                    pos1.setSelected(lastPos2.equals(p2) ? ITEM_NONE : lastPos2);
                } else if (p3.equals(p2)) {
                    pos3.setSelected(lastPos2.equals(p2) ? ITEM_NONE : lastPos2);
                }
            }
            lastPos1 = pos1.getSelected();
            lastPos2 = pos2.getSelected();
            lastPos3 = pos3.getSelected();
        } else if (!p3.equals(lastPos3)) {
            if (!p3.equals(ITEM_NONE)) {
                if (p1.equals(p3)) {
                    pos1.setSelected(lastPos3.equals(p3) ? ITEM_NONE : lastPos3);
                } else if (p2.equals(p3)) {
                    pos2.setSelected(lastPos3.equals(p3) ? ITEM_NONE : lastPos3);
                }
            }
            lastPos1 = pos1.getSelected();
            lastPos2 = pos2.getSelected();
            lastPos3 = pos3.getSelected();
        }
    }
}