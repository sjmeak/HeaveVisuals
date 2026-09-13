package rtx.heave.api.modules.impl.Utils;

import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.utils.net.ClientPresence;

public final class Globals
extends Module {
    private final BooleanSetting inTags = this.register(new BooleanSetting("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0432 \u0442\u0435\u0433\u0430\u0445", "\u041b\u043e\u0433\u043e\u0442\u0438\u043f \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0440\u044f\u0434\u043e\u043c \u0441 \u0438\u043c\u0435\u043d\u0435\u043c \u0438\u0433\u0440\u043e\u043a\u043e\u0432 Heave \u0432 \u0442\u0430\u0431\u043b\u0438\u0447\u043a\u0430\u0445 \u043d\u0430\u0434 \u0433\u043e\u043b\u043e\u0432\u043e\u0439.", true));
    private final BooleanSetting inTab = this.register(new BooleanSetting("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0432 \u0442\u0430\u0431\u0435", "\u041b\u043e\u0433\u043e\u0442\u0438\u043f \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0440\u044f\u0434\u043e\u043c \u0441 \u0438\u043c\u0435\u043d\u0435\u043c \u0438\u0433\u0440\u043e\u043a\u043e\u0432 Heave \u0432 \u0441\u043f\u0438\u0441\u043a\u0435 \u0438\u0433\u0440\u043e\u043a\u043e\u0432 (Tab).", true));

    public Globals() {
        super("Globals", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u043c\u0435\u0442\u043a\u0443 \u043a\u043b\u0438\u0435\u043d\u0442\u0430 \u0443 \u0434\u0440\u0443\u0433\u0438\u0445 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0435\u0439 Heave.", Category.UTILS);
    }

    public static Globals getInstance() {
        return ModuleManager.get().get(Globals.class);
    }

    @Override
    protected void onDisable() {
        ClientPresence.INSTANCE.stop();
    }

    @Override
    protected void onEnable() {
        ClientPresence.INSTANCE.start();
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public static boolean tagsBadge() {
        Globals globals = Globals.getInstance();
        return globals != null && globals.isEnabled() && globals.inTags.getValue();
    }

    public static boolean tabBadge() {
        Globals globals = Globals.getInstance();
        return globals != null && globals.isEnabled() && globals.inTab.getValue();
    }
}

