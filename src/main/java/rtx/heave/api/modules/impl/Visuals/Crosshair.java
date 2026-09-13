package rtx.heave.api.modules.impl.Visuals;

import java.awt.Color;
import net.minecraft.client.gui.screen.Screen;
import rtx.heave.api.crosshair.CrosshairConfig;
import rtx.heave.api.crosshair.CrosshairEditorScreen;
import rtx.heave.api.crosshair.CrosshairHud;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.render.HudRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ButtonSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.api.ui.UI;

public final class Crosshair extends Module {
    private final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Тип прицела.", "Свой", "Свой", "Крестик", "Точка")
    );
    private final ButtonSetting customEditor = this.register(
        new ButtonSetting("Редактор 15x15", "Нарисовать свой прицел в сетке 15х15.")
            .label("Открыть редактор (15x15)")
            .onClick(this::openEditor)
    );
    private final SliderSetting scale = this.register(
        new SliderSetting("Масштаб", "Размер прицела.")
            .range(0.5f, 2.5f).increment(0.25f).setValue(1.0f)
    );
    private final BooleanSetting applyBlend = this.register(
        new BooleanSetting("Инверсия цвета", "Ванильное смешивание цветов прицела с фоном.", true)
    );
    private final ColorSetting color = this.register(
        new ColorSetting("Цвет", "Основной цвет прицела.", new Color(255, 255, 255))
    );
    private final ColorSetting entityColor = this.register(
        new ColorSetting("Цвет на сущности", "Цвет прицела при наведении на сущность.", new Color(255, 68, 68))
    );
    private final ColorSetting containerColor = this.register(
        new ColorSetting("Цвет на контейнере", "Цвет прицела при наведении на сундук/блок.", new Color(68, 136, 255))
    );

    public Crosshair() {
        super("Crosshair", "Кастомный прицел с 15х15 редактором и центровкой из LabyMod 3.", Category.VISUALS);
    }

    private void openEditor() {
        this.mode.selected("Свой");
        Screen parent = this.mc.currentScreen;
        CrosshairEditorScreen screen = new CrosshairEditorScreen(parent);
        this.mc.setScreen(screen);
    }

    public void syncConfig() {
        CrosshairConfig cfg = CrosshairConfig.get();
        cfg.enabled = this.isEnabled();
        if (this.mode.is("Крестик")) {
            cfg.style = "CROSS";
        } else if (this.mode.is("Точка")) {
            cfg.style = "DOT";
        } else {
            cfg.style = "CUSTOM";
        }
        cfg.canvasScale = this.scale.getFloat();
        cfg.applyBlend = this.applyBlend.getValue();
        cfg.color = String.format("#%06X", (0xFFFFFF & this.color.getColor()));
        cfg.entityColor = String.format("#%06X", (0xFFFFFF & this.entityColor.getColor()));
        cfg.containerColor = String.format("#%06X", (0xFFFFFF & this.containerColor.getColor()));
    }

    @Override
    protected void onEnable() {
        this.syncConfig();
    }

    @Override
    protected void onDisable() {
        this.syncConfig();
    }
}
