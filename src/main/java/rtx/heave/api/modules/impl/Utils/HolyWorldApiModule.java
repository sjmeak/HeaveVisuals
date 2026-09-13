package rtx.heave.api.modules.impl.Utils;

import rtx.heave.api.holyworld.HolyWorldApi;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;

public final class HolyWorldApiModule extends Module {
    private static HolyWorldApiModule instance;

    private final SeparatorSetting displaySep = this.register(new SeparatorSetting("Отображение данных"));
    public final BooleanSetting showCoins = this.register(
        new BooleanSetting("Курс монет", "Отслеживать курс покупки/продажи монет HolyWorld.", true)
    );
    public final BooleanSetting showEvents = this.register(
        new BooleanSetting("События сервера", "Отслеживать таймеры мистиков, боссов и захватов замков.", true)
    );
    public final BooleanSetting showOnline = this.register(
        new BooleanSetting("Общий онлайн", "Отслеживать суммарный онлайн серверов HolyWorld.", true)
    );


    public HolyWorldApiModule() {
        super("HolyWorld API", "Интеграция с официальным REST API HolyWorld (онлайн, биржа монет, события).", Category.EVENTS);
        instance = this;
        // Warm up API client
        HolyWorldApi.getInstance();
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public static HolyWorldApiModule getInstance() {
        return instance;
    }

    public HolyWorldApi getApi() {
        return HolyWorldApi.getInstance();
    }
}
