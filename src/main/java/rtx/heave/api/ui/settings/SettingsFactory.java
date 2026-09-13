package rtx.heave.api.ui.settings;
import java.util.ArrayList;
import java.util.List;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ButtonSetting;
import rtx.heave.api.ui.settings.Setting;
import rtx.heave.api.ui.settings.impl.BindSetting;
import rtx.heave.api.ui.settings.impl.BoolSetting;
import rtx.heave.api.ui.settings.impl.ButtonRowSetting;
import rtx.heave.api.ui.settings.impl.ColorSetting;
import rtx.heave.api.ui.settings.impl.MultiSelectSetting;
import rtx.heave.api.ui.settings.impl.SelectSetting;
import rtx.heave.api.ui.settings.impl.SeparatorSetting;
import rtx.heave.api.ui.settings.impl.SliderSetting;
import rtx.heave.api.ui.settings.impl.TextSetting;

public final class SettingsFactory {
    private SettingsFactory() {
    }

    public static Setting create(rtx.heave.api.modules.settings.Setting setting) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            return new BoolSetting(booleanSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.SliderSetting) {
            rtx.heave.api.modules.settings.impl.SliderSetting sliderSetting = (rtx.heave.api.modules.settings.impl.SliderSetting)setting;
            return new SliderSetting(sliderSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.ColorSetting) {
            rtx.heave.api.modules.settings.impl.ColorSetting colorSetting = (rtx.heave.api.modules.settings.impl.ColorSetting)setting;
            return new ColorSetting(colorSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.SelectSetting) {
            rtx.heave.api.modules.settings.impl.SelectSetting selectSetting = (rtx.heave.api.modules.settings.impl.SelectSetting)setting;
            return new SelectSetting(selectSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.MultiSelectSetting) {
            rtx.heave.api.modules.settings.impl.MultiSelectSetting multiSelectSetting = (rtx.heave.api.modules.settings.impl.MultiSelectSetting)setting;
            return new MultiSelectSetting(multiSelectSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.BindSetting) {
            rtx.heave.api.modules.settings.impl.BindSetting bindSetting = (rtx.heave.api.modules.settings.impl.BindSetting)setting;
            return new BindSetting(bindSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.SeparatorSetting) {
            rtx.heave.api.modules.settings.impl.SeparatorSetting separatorSetting = (rtx.heave.api.modules.settings.impl.SeparatorSetting)setting;
            return new SeparatorSetting(separatorSetting);
        }
        if (setting instanceof rtx.heave.api.modules.settings.impl.TextSetting) {
            rtx.heave.api.modules.settings.impl.TextSetting textSetting = (rtx.heave.api.modules.settings.impl.TextSetting)setting;
            return new TextSetting(textSetting);
        }
        if (setting instanceof ButtonSetting) {
            ButtonSetting buttonSetting = (ButtonSetting)setting;
            return new ButtonRowSetting(buttonSetting);
        }
        return null;
    }

    public static List<Setting> build(Module module) {
        TextSetting.unfocusAll();
        ArrayList<Setting> arrayList = new ArrayList<Setting>();
        for (rtx.heave.api.modules.settings.Setting setting : module.getSettings().all()) {
            Setting setting2 = SettingsFactory.create(setting);
            if (setting2 == null) continue;
            arrayList.add(setting2);
        }
        return arrayList;
    }
}

