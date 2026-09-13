package rtx.heave.api.modules.impl.Interface;

import net.minecraft.client.MinecraftClient;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import ru.customgamegui.config.CGGConfig;
import ru.customgamegui.config.CGGConfigManager;
import ru.customgamegui.gui.CGGConfigScreen;

public final class CustomGui extends Module {
    private static CustomGui instance;

    private final SeparatorSetting expSep = this.register(new SeparatorSetting("Опыт"));
    public final BooleanSetting hideExperienceBar = this.register(
        new BooleanSetting("Скрыть полоску опыта", "Скрывает полосу опыта над хотбаром.", false)
    );
    public final BooleanSetting hideExperienceLevel = this.register(
        new BooleanSetting("Скрыть уровень опыта", "Скрывает цифру уровня опыта.", false)
    );
    public final BooleanSetting shiftDownWithoutExpBar = this.register(
        new BooleanSetting("Сдвигать статус-бары", "Опускает сердечки и голод при скрытой полосе опыта.", true)
    );

    private final SeparatorSetting hotbarSep = this.register(new SeparatorSetting("Хотбар"));
    public final BooleanSetting hideHotbar = this.register(
        new BooleanSetting("Скрыть хотбар", "Полностью скрывает хотбар.", false)
    );
    public final BooleanSetting hideHeldItemName = this.register(
        new BooleanSetting("Скрыть имя предмета", "Скрывает всплывающее имя предмета в руках.", false)
    );
    public final SliderSetting hotbarBackgroundOpacity = this.register(
        new SliderSetting("Прозрачность фона хотбара", "Прозрачность фона слотов хотбара (%).")
            .range(0.0f, 100.0f).increment(5.0f).setValue(100.0f)
    );
    public final SliderSetting hotbarSelectionOpacity = this.register(
        new SliderSetting("Прозрачность выделения", "Прозрачность рамки выбранного слота (%).")
            .range(0.0f, 100.0f).increment(5.0f).setValue(100.0f)
    );

    private final SeparatorSetting statusSep = this.register(new SeparatorSetting("Статус-бары"));
    public final BooleanSetting hideHealthBar = this.register(
        new BooleanSetting("Скрыть здоровье", "Скрывает сердечки здоровья.", false)
    );
    public final BooleanSetting hideHungerBar = this.register(
        new BooleanSetting("Скрыть голод", "Скрывает иконки голода.", false)
    );
    public final BooleanSetting hideArmorBar = this.register(
        new BooleanSetting("Скрыть броню", "Скрывает иконки брони.", false)
    );
    public final BooleanSetting hideAirBar = this.register(
        new BooleanSetting("Скрыть воздух", "Скрывает пузыри под водой.", false)
    );

    public CustomGui() {
        super("Custom Gui", "Кастомизация и настройка игрового интерфейса (HUD, хотбар, полоски опыта и статусов).", Category.DISPLAY);
        instance = this;
        CGGConfigManager.load();
        syncToCgg();
    }

    public static CustomGui getInstance() {
        return instance;
    }

    public void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(new CGGConfigScreen(null));
    }

    public void syncToCgg() {
        CGGConfig config = CGGConfigManager.getConfig();
        config.enabled = this.isEnabled();
        config.hideExperienceBar = this.hideExperienceBar.getValue();
        config.hideExperienceLevel = this.hideExperienceLevel.getValue();
        config.shiftDownWithoutExpBar = this.shiftDownWithoutExpBar.getValue();
        config.hideHotbar = this.hideHotbar.getValue();
        config.hideHeldItemName = this.hideHeldItemName.getValue();
        config.hotbarBackgroundOpacity = (int) this.hotbarBackgroundOpacity.getValue();
        config.hotbarSelectionOpacity = (int) this.hotbarSelectionOpacity.getValue();
        config.hideHealthBar = this.hideHealthBar.getValue();
        config.hideHungerBar = this.hideHungerBar.getValue();
        config.hideArmorBar = this.hideArmorBar.getValue();
        config.hideAirBar = this.hideAirBar.getValue();
        config.hideCrosshair = false;
        config.hideBossBar = false;
        config.hideScoreboard = false;
        config.hideStatusEffects = false;
        config.hideChat = false;
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (event.isPre()) {
            syncToCgg();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        syncToCgg();
    }

    @Override
    public void onDisable() {
        CGGConfig config = CGGConfigManager.getConfig();
        config.enabled = false;
        super.onDisable();
    }
}
