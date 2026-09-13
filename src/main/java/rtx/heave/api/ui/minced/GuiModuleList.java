package rtx.heave.api.ui.minced;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.ClickGui;
import rtx.heave.api.modules.settings.Setting;
import rtx.heave.api.modules.settings.impl.BindSetting;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ButtonSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.MultiSelectSetting;
import rtx.heave.api.modules.settings.impl.SelectSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;
import rtx.heave.api.modules.settings.impl.TextSetting;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.animations.AnimatedFloat;
import rtx.heave.utils.key.KeyBind;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;
import rtx.heave.utils.render.render2d.picker.BuiltPicker;
import rtx.heave.utils.sounds.Sounds;

public class GuiModuleList {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final float CARD_WIDTH = 159.0f;
    private static final float COL_GAP = 6.0f;
    private static final float ROW_GAP = 6.0f;

    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;
    private float totalContentHeight = 0.0f;

    private final Map<Module, AnimatedFloat> toggleAnims = new HashMap<>();
    private final Map<BooleanSetting, AnimatedFloat> boolAnims = new HashMap<>();
    private final Set<Module> expandedModules = new HashSet<>();
    private final Map<Module, AnimatedFloat> expandAnims = new HashMap<>();

    private SliderSetting activeDraggingSlider = null;
    private float draggingSliderTrackX = 0.0f;
    private float draggingSliderTrackW = 0.0f;

    private Module activeBindingModule = null;
    private BindSetting activeBindingSetting = null;
    private TextSetting activeFocusedText = null;

    private ColorSetting activeColorSetting = null;
    private float colorPickerX = 0.0f;
    private float colorPickerY = 0.0f;
    private int activeColorDrag = 0;

    public void render(DrawContext context, float x, float y, float alpha, Category category, String query) {
        float contentX = x + 107.0f;
        float contentY = y + 33.0f;
        float contentW = 341.0f;
        float contentH = 238.0f;

        // Smooth scroll update
        scroll += (scrollTarget - scroll) * 0.25f;
        if (Math.abs(scrollTarget - scroll) < 0.01f) {
            scroll = scrollTarget;
        }

        Render2D.pushScissor(context, contentX, contentY, contentW, contentH);

        List<Module> modules = getFilteredModules(category, query);

        float col1X = x + 114.0f;
        float col2X = col1X + CARD_WIDTH + COL_GAP;
        float col1Y = y + 40.0f + scroll;
        float col2Y = y + 40.0f + scroll;

        for (Module module : modules) {
            float cardH = calculateCardHeight(module);
            boolean useCol2 = (col2Y < col1Y);
            float cardX = useCol2 ? col2X : col1X;
            float cardY = useCol2 ? col2Y : col1Y;

            // Only render if visible within content window
            if (cardY + cardH >= contentY - 10.0f && cardY <= contentY + contentH + 10.0f) {
                renderModuleCard(context, module, cardX, cardY, cardH, alpha);
            }

            if (useCol2) {
                col2Y += cardH + ROW_GAP;
            } else {
                col1Y += cardH + ROW_GAP;
            }
        }

        totalContentHeight = Math.max(col1Y, col2Y) - (y + 40.0f + scroll);
        Render2D.popScissor(context);

        if (activeColorSetting != null) {
            renderColorPickerPopup(context, alpha);
        }
    }

    private void renderModuleCard(DrawContext context, Module module, float cardX, float cardY, float cardH, float alpha) {
        boolean enabled = module.isEnabled();
        AnimatedFloat anim = toggleAnims.computeIfAbsent(module, m -> new AnimatedFloat(enabled ? 1.0f : 0.0f, 10.0f));
        anim.setTarget(enabled ? 1.0f : 0.0f);
        float p = anim.getValue();

        // 1. Card Base Background (Solid dark base)
        int baseBg = (Math.max(0, Math.min(255, (int)(245.0f * alpha))) << 24) | 0x111117;
        Render2D.rect(cardX, cardY, CARD_WIDTH, cardH, 5.0f, baseBg);

        // 2. Enabled State Highlights
        if (p > 0.01f) {
            // Subtle accent card tint
            int tintColor = ThemeManager.accent(28.0f * p * alpha);
            Render2D.rect(cardX, cardY, CARD_WIDTH, cardH, 5.0f, tintColor);

            // Accent outline
            int outlineColor = ThemeManager.accent(180.0f * p * alpha);
            Render2D.outline(cardX, cardY, CARD_WIDTH, cardH, 5.0f, 1.0f, outlineColor);
        } else {
            // Inactive subtle border
            int border = (Math.max(0, Math.min(255, (int)(32.0f * alpha))) << 24) | 0xFFFFFF;
            Render2D.outline(cardX, cardY, CARD_WIDTH, cardH, 5.0f, 0.5f, border);
        }

        // 3. Module Title
        int titleCol = lerpColor(
            (Math.max(0, Math.min(255, (int)(150.0f * alpha))) << 24) | 0xFFFFFF,
            (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF,
            p
        );
        Fonts.SF_BOLD.draw(module.getDisplayName(), cardX + 9.0f, cardY + 7.5f, 10.0f, titleCol);

        // 4. Keybind indicator next to name
        KeyBind bind = module.getBind();
        String bindText = (activeBindingModule == module) ? "[...]"
            : (bind.isBound() ? "[" + bind.getDisplayName() + "]" : "");
        if (!bindText.isEmpty()) {
            float nameW = Fonts.SF_BOLD.width(module.getDisplayName(), 10.0f);
            int bindColor = (activeBindingModule == module)
                ? ThemeManager.accent(255.0f * alpha)
                : ((Math.max(0, Math.min(255, (int)(120.0f * alpha))) << 24) | 0xFFFFFF);
            Fonts.SF.draw(bindText, cardX + 13.0f + nameW, cardY + 8.5f, 7.5f, bindColor);
        }

        // 5. Settings Gear Icon on Top-Right
        List<Setting> settings = getVisibleSettings(module);
        boolean hasSettings = !settings.isEmpty();
        boolean isExpanded = expandedModules.contains(module);

        if (hasSettings) {
            float gearX = cardX + CARD_WIDTH - 17.0f;
            float gearY = cardY + 8.0f;
            int gearColor = isExpanded
                ? ThemeManager.accent(255.0f * alpha)
                : ((Math.max(0, Math.min(255, (int)(150.0f * alpha))) << 24) | 0xFFFFFF);
            Fonts.KIMIKO.draw("f", gearX, gearY, 8.0f, gearColor);
        }

        // 6. Inline Settings (only if expanded and visible)
        AnimatedFloat expAnim = expandAnims.computeIfAbsent(module, m -> new AnimatedFloat(isExpanded ? 1.0f : 0.0f, 14.0f));
        expAnim.setTarget(isExpanded ? 1.0f : 0.0f);
        float exp = expAnim.getValue();

        if (hasSettings && exp > 0.01f && cardH > 26.0f) {
            Render2D.pushScissor(context, cardX, cardY + 23.0f, CARD_WIDTH, cardH - 23.0f);

            // Divider line
            int divCol = (Math.max(0, Math.min(255, (int)(20.0f * exp * alpha))) << 24) | 0xFFFFFF;
            Render2D.rect(cardX + 8.0f, cardY + 23.0f, CARD_WIDTH - 16.0f, 0.5f, 0.0f, divCol);

            float settingY = cardY + 28.0f;
            float sx = cardX + 8.0f;
            float sw = CARD_WIDTH - 16.0f;

            for (Setting setting : settings) {
                if (setting instanceof SeparatorSetting sep) {
                    renderSeparatorSetting(context, sep, sx, settingY, sw, alpha * exp);
                    settingY += 14.0f;
                } else if (setting instanceof BooleanSetting bool) {
                    renderBooleanSetting(context, bool, sx, settingY, sw, alpha * exp);
                    settingY += 12.0f;
                } else if (setting instanceof SliderSetting slider) {
                    renderSliderSetting(context, slider, sx, settingY, sw, alpha * exp);
                    settingY += 19.0f;
                } else if (setting instanceof SelectSetting select) {
                    float h = renderSelectSetting(context, select, sx, settingY, sw, alpha * exp);
                    settingY += h;
                } else if (setting instanceof MultiSelectSetting multi) {
                    float h = renderMultiSelectSetting(context, multi, sx, settingY, sw, alpha * exp);
                    settingY += h;
                } else if (setting instanceof ColorSetting color) {
                    renderColorSetting(context, color, sx, settingY, sw, alpha * exp);
                    settingY += 12.0f;
                } else if (setting instanceof BindSetting bindSetting) {
                    renderBindSetting(context, bindSetting, sx, settingY, sw, alpha * exp);
                    settingY += 12.0f;
                } else if (setting instanceof TextSetting textSetting) {
                    renderTextSetting(context, textSetting, sx, settingY, sw, alpha * exp);
                    settingY += 25.0f;
                } else if (setting instanceof ButtonSetting buttonSetting) {
                    renderButtonSetting(context, buttonSetting, sx, settingY, sw, alpha * exp);
                    settingY += 17.0f;
                }
            }

            Render2D.popScissor(context);
        }
    }

    private void renderSeparatorSetting(DrawContext context, SeparatorSetting sep, float x, float y, float w, float alpha) {
        int lineCol = (Math.max(0, Math.min(255, (int)(25.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(x + 2.0f, y + 6.0f, w - 4.0f, 0.5f, 0.0f, lineCol);

        String name = sep.getName();
        if (name != null && !name.isEmpty()) {
            float tw = Fonts.SF_BOLD.width(name, 7.0f);
            float tx = x + 6.0f;
            int bg = (Math.max(0, Math.min(255, (int)(245.0f * alpha))) << 24) | 0x111117;
            Render2D.rect(tx - 3.0f, y + 1.0f, tw + 6.0f, 10.0f, 2.0f, bg);
            int textCol = ThemeManager.accent(220.0f * alpha);
            Fonts.SF_BOLD.draw(name, tx, y + 2.0f, 7.0f, textCol);
        }
    }

    private void renderBooleanSetting(DrawContext context, BooleanSetting setting, float x, float y, float w, float alpha) {
        boolean val = setting.getValue();
        AnimatedFloat anim = boolAnims.computeIfAbsent(setting, s -> new AnimatedFloat(val ? 1.0f : 0.0f, 10.0f));
        anim.setTarget(val ? 1.0f : 0.0f);
        float p = anim.getValue();

        int textColor = (Math.max(0, Math.min(255, (int)((val ? 240.0f : 140.0f) * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 1.5f, 8.5f, textColor);

        float sw = 14.0f;
        float sh = 8.0f;
        float sx = x + w - sw;
        float sy = y + 1.0f;

        int trackOff = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0x22222C;
        int trackOn = ThemeManager.accent(240.0f * alpha);
        int bg = lerpColor(trackOff, trackOn, p);
        Render2D.rect(sx, sy, sw, sh, 4.0f, bg);

        float knobX = sx + 1.5f + p * (sw - 7.0f);
        int knob = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(knobX, sy + 1.5f, 5.0f, 5.0f, 2.5f, knob);
    }

    private void renderSliderSetting(DrawContext context, SliderSetting setting, float x, float y, float w, float alpha) {
        int labelColor = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 0.5f, 8.0f, labelColor);

        String valStr = String.format(java.util.Locale.US, "%.2f", setting.getFloat());
        if (valStr.endsWith(".00")) valStr = valStr.substring(0, valStr.length() - 3);
        float valW = Fonts.SF.width(valStr, 8.0f);
        Fonts.SF.draw(valStr, x + w - valW, y + 0.5f, 8.0f, labelColor);

        float trackY = y + 11.0f;
        float trackH = 3.0f;
        int trackBg = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0x22222C;
        Render2D.rect(x + 2.0f, trackY, w - 4.0f, trackH, 1.5f, trackBg);

        float range = setting.getMax() - setting.getMin();
        float norm = range <= 0.0001f ? 0.0f : MathHelper.clamp((setting.getFloat() - setting.getMin()) / range, 0.0f, 1.0f);
        float fillW = norm * (w - 4.0f);
        if (fillW > 0.0f) {
            Render2D.rect(x + 2.0f, trackY, fillW, trackH, 1.5f, ThemeManager.accent(240.0f * alpha));
        }

        // Knob
        int knob = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(x + 2.0f + fillW - 2.5f, trackY - 1.5f, 6.0f, 6.0f, 3.0f, knob);
    }

    private float renderSelectSetting(DrawContext context, SelectSetting setting, float x, float y, float w, float alpha) {
        int labelColor = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 0.5f, 8.0f, labelColor);

        float curX = x + 2.0f;
        float curY = y + 9.5f;
        float rowH = 13.0f;
        List<String> options = setting.getOptions();

        for (String opt : options) {
            boolean selected = opt.equalsIgnoreCase(setting.getSelected());
            float optW = Fonts.SF.width(opt, 7.5f) + 8.0f;

            if (curX + optW > x + w) {
                curX = x + 2.0f;
                curY += rowH + 2.0f;
            }

            int bg = selected
                ? ThemeManager.accent(200.0f * alpha)
                : ((Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0x1E1E28);
            Render2D.rect(curX, curY, optW, rowH, 3.0f, bg);

            int textCol = selected
                ? ((Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF)
                : ((Math.max(0, Math.min(255, (int)(150.0f * alpha))) << 24) | 0xFFFFFF);
            Fonts.SF.draw(opt, curX + 4.0f, curY + 2.5f, 7.5f, textCol);

            curX += optW + 3.0f;
        }

        return (curY + rowH + 2.0f) - y;
    }

    private float renderMultiSelectSetting(DrawContext context, MultiSelectSetting setting, float x, float y, float w, float alpha) {
        int labelColor = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 0.5f, 8.0f, labelColor);

        float curX = x + 2.0f;
        float curY = y + 9.5f;
        float rowH = 13.0f;
        List<String> options = setting.getOptions();

        for (String opt : options) {
            boolean selected = setting.isSelected(opt);
            float optW = Fonts.SF.width(opt, 7.5f) + 8.0f;

            if (curX + optW > x + w) {
                curX = x + 2.0f;
                curY += rowH + 2.0f;
            }

            int bg = selected
                ? ThemeManager.accent(200.0f * alpha)
                : ((Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0x1E1E28);
            Render2D.rect(curX, curY, optW, rowH, 3.0f, bg);

            int textCol = selected
                ? ((Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF)
                : ((Math.max(0, Math.min(255, (int)(150.0f * alpha))) << 24) | 0xFFFFFF);
            Fonts.SF.draw(opt, curX + 4.0f, curY + 2.5f, 7.5f, textCol);

            curX += optW + 3.0f;
        }

        return (curY + rowH + 2.0f) - y;
    }

    private void renderColorSetting(DrawContext context, ColorSetting setting, float x, float y, float w, float alpha) {
        int labelColor = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 1.5f, 8.5f, labelColor);

        float chipW = 16.0f;
        float chipH = 8.0f;
        float chipX = x + w - chipW;
        float chipY = y + 1.5f;

        int c = setting.getColor();
        int drawColor = (Math.max(0, Math.min(255, (int)(((c >>> 24) & 0xFF) * (alpha)))) << 24) | (c & 0xFFFFFF);
        Render2D.rect(chipX, chipY, chipW, chipH, 2.5f, drawColor);
        Render2D.outline(chipX, chipY, chipW, chipH, 2.5f, 0.5f, (Math.max(0, Math.min(255, (int)(50.0f * alpha))) << 24) | 0xFFFFFF);
    }

    private void renderColorPickerPopup(DrawContext context, float alpha) {
        if (activeColorSetting == null) return;
        float popW = 96.0f;
        float popH = 86.0f;
        float popX = colorPickerX;
        float popY = colorPickerY;

        // Shadow & Background
        int shadowColor = (Math.max(0, Math.min(255, (int)(120.0f * alpha))) << 24);
        Render2D.rect(popX - 1.0f, popY - 1.0f, popW + 2.0f, popH + 2.0f, 5.0f, shadowColor);
        int bg = (Math.max(0, Math.min(255, (int)(250.0f * alpha))) << 24) | 0x14141C;
        Render2D.rect(popX, popY, popW, popH, 4.0f, bg);
        int border = (Math.max(0, Math.min(255, (int)(60.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.outline(popX, popY, popW, popH, 4.0f, 0.5f, border);

        // 1. SV Box (84x46)
        float svX = popX + 6.0f;
        float svY = popY + 6.0f;
        float svW = 84.0f;
        float svH = 46.0f;

        int hColor = Color.HSBtoRGB(activeColorSetting.getHue(), 1.0f, 1.0f);
        int topR = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | (hColor & 0xFFFFFF);
        int topL = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(svX, svY, svW, svH, 2.0f, topL, topR, topR, topL);

        int botR = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24);
        Render2D.rect(svX, svY, svW, svH, 2.0f, 0, 0, botR, botR);

        // SV Dragger Knob
        float sat = MathHelper.clamp(activeColorSetting.getSaturation(), 0.0f, 1.0f);
        float val = MathHelper.clamp(activeColorSetting.getBrightness(), 0.0f, 1.0f);
        float knobX = svX + sat * svW;
        float knobY = svY + (1.0f - val) * svH;
        Render2D.circle(knobX, knobY, 3.0f, 0xFFFFFFFF);
        Render2D.circle(knobX, knobY, 2.0f, activeColorSetting.getColorOpaque());

        // 2. Hue Bar (84x6)
        float hueX = popX + 6.0f;
        float hueY = svY + svH + 5.0f;
        float hueW = 84.0f;
        float hueH = 6.0f;
        BuiltPicker.hue(hueX, hueY, hueW, hueH, alpha).render(context);

        float hKnobX = hueX + MathHelper.clamp(activeColorSetting.getHue(), 0.0f, 1.0f) * hueW;
        Render2D.rect(hKnobX - 1.5f, hueY - 1.0f, 3.0f, hueH + 2.0f, 1.0f, 0xFFFFFFFF);

        // 3. Alpha Bar (84x6)
        float alX = popX + 6.0f;
        float alY = hueY + hueH + 4.0f;
        float alW = 84.0f;
        float alH = 6.0f;
        BuiltPicker.alpha(alX, alY, alW, alH, activeColorSetting.getColorOpaque(), alpha).render(context);

        float aKnobX = alX + MathHelper.clamp(activeColorSetting.getAlpha(), 0.0f, 1.0f) * alW;
        Render2D.rect(aKnobX - 1.5f, alY - 1.0f, 3.0f, alH + 2.0f, 1.0f, 0xFFFFFFFF);

        // 4. Hex text readout
        float textY = alY + alH + 3.0f;
        String hex = String.format("#%08X", activeColorSetting.getColor());
        int hexCol = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(hex, popX + 6.0f, textY, 7.0f, hexCol);
    }

    private void updateColorSV(float mouseX, float mouseY) {
        if (activeColorSetting == null) return;
        float svX = colorPickerX + 6.0f;
        float svY = colorPickerY + 6.0f;
        float svW = 84.0f;
        float svH = 46.0f;
        float sat = MathHelper.clamp((mouseX - svX) / svW, 0.0f, 1.0f);
        float val = MathHelper.clamp(1.0f - (mouseY - svY) / svH, 0.0f, 1.0f);
        activeColorSetting.setHSB(activeColorSetting.getHue(), sat, val);
    }

    private void updateColorHue(float mouseX) {
        if (activeColorSetting == null) return;
        float hueX = colorPickerX + 6.0f;
        float hueW = 84.0f;
        float hue = MathHelper.clamp((mouseX - hueX) / hueW, 0.0f, 1.0f);
        activeColorSetting.setHSB(hue, activeColorSetting.getSaturation(), activeColorSetting.getBrightness());
    }

    private void updateColorAlpha(float mouseX) {
        if (activeColorSetting == null) return;
        float alX = colorPickerX + 6.0f;
        float alW = 84.0f;
        float a = MathHelper.clamp((mouseX - alX) / alW, 0.0f, 1.0f);
        activeColorSetting.setAlpha(a);
    }

    private void renderBindSetting(DrawContext context, BindSetting setting, float x, float y, float w, float alpha) {
        int labelColor = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 1.5f, 8.5f, labelColor);

        boolean active = (activeBindingSetting == setting);
        String text = active ? "[...]"
            : (setting.isBound() ? "[" + setting.getValue().getDisplayName() + "]" : "[None]");

        float tw = Fonts.SF.width(text, 8.0f);
        int valColor = active
            ? ThemeManager.accent(255.0f * alpha)
            : ((Math.max(0, Math.min(255, (int)(120.0f * alpha))) << 24) | 0xFFFFFF);
        Fonts.SF.draw(text, x + w - tw, y + 1.5f, 8.0f, valColor);
    }

    private void renderTextSetting(DrawContext context, TextSetting setting, float x, float y, float w, float alpha) {
        int labelColor = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(setting.getName(), x + 2.0f, y + 0.5f, 8.0f, labelColor);

        float boxY = y + 10.0f;
        float boxH = 13.0f;
        boolean focused = (activeFocusedText == setting);

        int bg = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0x1A1A22;
        Render2D.rect(x + 2.0f, boxY, w - 4.0f, boxH, 3.0f, bg);

        if (focused) {
            Render2D.outline(x + 2.0f, boxY, w - 4.0f, boxH, 3.0f, 0.5f, ThemeManager.accent(220.0f * alpha));
        }

        String val = setting.getText();
        int textColor = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(val, x + 5.0f, boxY + 2.5f, 8.0f, textColor);

        if (focused && (System.currentTimeMillis() / 450) % 2 == 0) {
            float tw = Fonts.SF.width(val, 8.0f);
            Render2D.rect(x + 5.0f + tw + 1.0f, boxY + 2.0f, 1.0f, 9.0f, 0.0f, textColor);
        }
    }

    private void renderButtonSetting(DrawContext context, ButtonSetting setting, float x, float y, float w, float alpha) {
        String label = setting.getLabel();
        if (label == null || label.isEmpty()) {
            label = setting.getName();
        }
        float h = 14.0f;
        int bg = (Math.max(0, Math.min(255, (int)(25.0f * alpha))) << 24) | 0xFFFFFF;
        int border = ThemeManager.accent(100.0f * alpha);
        Render2D.rect(x + 2.0f, y, w - 4.0f, h, 3.0f, bg);
        Render2D.outline(x + 2.0f, y, w - 4.0f, h, 3.0f, 0.5f, border);
        float tw = Fonts.SF.width(label, 7.5f);
        float tx = x + (w - tw) * 0.5f + setting.getLabelOffsetX();
        float ty = y + (h - 7.5f) * 0.5f + 1.0f;
        int textCol = ThemeManager.accent(255.0f * alpha);
        Fonts.SF.draw(label, tx, ty, 7.5f, textCol);
    }

    public boolean isBinding() {
        return activeBindingModule != null || activeBindingSetting != null;
    }

    public boolean handleMouseBind(int button) {
        if (activeBindingModule != null) {
            if (button != 0) {
                activeBindingModule.setBind(KeyBind.mouse(button));
            }
            activeBindingModule = null;
            return true;
        }
        if (activeBindingSetting != null) {
            if (button != 0) {
                int code = (button == 2) ? 1002 : button;
                activeBindingSetting.setKey(code);
            }
            activeBindingSetting = null;
            return true;
        }
        return false;
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button, float x, float y, Category category, String query) {
        if (isBinding()) {
            return handleMouseBind(button);
        }

        float contentX = x + 107.0f;
        float contentY = y + 33.0f;
        float contentW = 341.0f;
        float contentH = 238.0f;

        if (mouseX < contentX || mouseX > contentX + contentW || mouseY < contentY || mouseY > contentY + contentH) {
            if (activeColorSetting != null) {
                activeColorSetting = null;
            }
            return false;
        }

        if (activeColorSetting != null) {
            float popW = 96.0f;
            float popH = 86.0f;
            float popX = colorPickerX;
            float popY = colorPickerY;
            float svX = popX + 6.0f;
            float svY = popY + 6.0f;
            float svW = 84.0f;
            float svH = 46.0f;
            float hueX = popX + 6.0f;
            float hueY = svY + svH + 5.0f;
            float hueW = 84.0f;
            float hueH = 6.0f;
            float alX = popX + 6.0f;
            float alY = hueY + hueH + 4.0f;
            float alW = 84.0f;
            float alH = 6.0f;

            if (mouseX >= svX && mouseX <= svX + svW && mouseY >= svY && mouseY <= svY + svH) {
                activeColorDrag = 1;
                updateColorSV(mouseX, mouseY);
                return true;
            } else if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY - 2.0f && mouseY <= hueY + hueH + 2.0f) {
                activeColorDrag = 2;
                updateColorHue(mouseX);
                return true;
            } else if (mouseX >= alX && mouseX <= alX + alW && mouseY >= alY - 2.0f && mouseY <= alY + alH + 2.0f) {
                activeColorDrag = 3;
                updateColorAlpha(mouseX);
                return true;
            } else if (mouseX >= popX && mouseX <= popX + popW && mouseY >= popY && mouseY <= popY + popH) {
                return true;
            } else {
                activeColorSetting = null;
            }
        }

        List<Module> modules = getFilteredModules(category, query);
        float col1X = x + 114.0f;
        float col2X = col1X + CARD_WIDTH + COL_GAP;
        float col1Y = y + 40.0f + scroll;
        float col2Y = y + 40.0f + scroll;

        for (Module module : modules) {
            float cardH = calculateCardHeight(module);
            boolean useCol2 = (col2Y < col1Y);
            float cardX = useCol2 ? col2X : col1X;
            float cardY = useCol2 ? col2Y : col1Y;

            List<Setting> visibleSettings = getVisibleSettings(module);
            boolean hasSettings = !visibleSettings.isEmpty();
            boolean isExp = expandedModules.contains(module);

            // Module header click
            if (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + 23.0f) {
                // If module has settings: right click anywhere on header or left click on the gear area toggles expand
                if (hasSettings && (button == 1 || (button == 0 && mouseX >= cardX + CARD_WIDTH - 24.0f))) {
                    if (isExp) {
                        expandedModules.remove(module);
                        Sounds.play("settings_close");
                    } else {
                        expandedModules.add(module);
                        Sounds.play("settings_open");
                    }
                    return true;
                }
                if (button == 0) {
                    module.toggle();
                    return true;
                } else if (button == 2) {
                    activeBindingModule = module;
                    return true;
                }
            }

            // Only process settings clicks if module is expanded
            if (!isExp) {
                if (useCol2) {
                    col2Y += cardH + ROW_GAP;
                } else {
                    col1Y += cardH + ROW_GAP;
                }
                continue;
            }

            // Settings clicks
            float settingY = cardY + 28.0f;
            float sx = cardX + 8.0f;
            float sw = CARD_WIDTH - 16.0f;

            for (Setting setting : visibleSettings) {
                if (setting instanceof SeparatorSetting) {
                    settingY += 14.0f;
                } else if (setting instanceof BooleanSetting bool) {
                    if (mouseX >= sx && mouseX <= sx + sw && mouseY >= settingY && mouseY <= settingY + 12.0f) {
                        bool.setValue(!bool.getValue());
                        return true;
                    }
                    settingY += 12.0f;
                } else if (setting instanceof SliderSetting slider) {
                    if (mouseX >= sx && mouseX <= sx + sw && mouseY >= settingY && mouseY <= settingY + 19.0f) {
                        activeDraggingSlider = slider;
                        draggingSliderTrackX = sx;
                        draggingSliderTrackW = sw;
                        updateSlider(slider, mouseX, sx, sw);
                        return true;
                    }
                    settingY += 19.0f;
                } else if (setting instanceof SelectSetting select) {
                    float curX = sx + 2.0f;
                    float curY = settingY + 9.5f;
                    float rowH = 13.0f;
                    boolean handled = false;

                    for (String opt : select.getOptions()) {
                        float optW = Fonts.SF.width(opt, 7.5f) + 8.0f;
                        if (curX + optW > sx + sw) {
                            curX = sx + 2.0f;
                            curY += rowH + 2.0f;
                        }
                        if (mouseX >= curX && mouseX <= curX + optW && mouseY >= curY && mouseY <= curY + rowH) {
                            select.setSelected(opt);
                            handled = true;
                            break;
                        }
                        curX += optW + 3.0f;
                    }
                    settingY = curY + rowH + 2.0f;
                    if (handled) return true;
                } else if (setting instanceof MultiSelectSetting multi) {
                    float curX = sx + 2.0f;
                    float curY = settingY + 9.5f;
                    float rowH = 13.0f;
                    boolean handled = false;

                    for (String opt : multi.getOptions()) {
                        float optW = Fonts.SF.width(opt, 7.5f) + 8.0f;
                        if (curX + optW > sx + sw) {
                            curX = sx + 2.0f;
                            curY += rowH + 2.0f;
                        }
                        if (mouseX >= curX && mouseX <= curX + optW && mouseY >= curY && mouseY <= curY + rowH) {
                            multi.toggle(opt);
                            handled = true;
                            break;
                        }
                        curX += optW + 3.0f;
                    }
                    settingY = curY + rowH + 2.0f;
                    if (handled) return true;
                } else if (setting instanceof ColorSetting color) {
                    if (mouseX >= sx && mouseX <= sx + sw && mouseY >= settingY && mouseY <= settingY + 12.0f) {
                        if (activeColorSetting == color) {
                            activeColorSetting = null;
                        } else {
                            activeColorSetting = color;
                            float popW = 96.0f;
                            float popH = 86.0f;
                            colorPickerX = MathHelper.clamp(sx + sw - popW, x + 107.0f + 2.0f, x + 107.0f + 341.0f - popW - 4.0f);
                            colorPickerY = MathHelper.clamp(settingY + 14.0f, y + 33.0f + 2.0f, y + 33.0f + 238.0f - popH - 4.0f);
                        }
                        return true;
                    }
                    settingY += 12.0f;
                } else if (setting instanceof BindSetting bind) {
                    if (mouseX >= sx && mouseX <= sx + sw && mouseY >= settingY && mouseY <= settingY + 12.0f) {
                        activeBindingSetting = bind;
                        return true;
                    }
                    settingY += 12.0f;
                } else if (setting instanceof TextSetting textSetting) {
                    if (mouseX >= sx && mouseX <= sx + sw && mouseY >= settingY + 8.0f && mouseY <= settingY + 23.0f) {
                        activeFocusedText = textSetting;
                        return true;
                    }
                    settingY += 25.0f;
                } else if (setting instanceof ButtonSetting buttonSetting) {
                    if (button == 0 && mouseX >= sx && mouseX <= sx + sw && mouseY >= settingY && mouseY <= settingY + 14.0f) {
                        Sounds.play("select_category");
                        buttonSetting.click();
                        return true;
                    }
                    settingY += 17.0f;
                }
            }

            if (useCol2) {
                col2Y += cardH + ROW_GAP;
            } else {
                col1Y += cardH + ROW_GAP;
            }
        }

        activeFocusedText = null;
        return false;
    }

    public boolean mouseDragged(float mouseX, float mouseY, float x, float y) {
        if (activeColorSetting != null && activeColorDrag > 0) {
            if (activeColorDrag == 1) updateColorSV(mouseX, mouseY);
            else if (activeColorDrag == 2) updateColorHue(mouseX);
            else if (activeColorDrag == 3) updateColorAlpha(mouseX);
            return true;
        }
        if (activeDraggingSlider != null) {
            updateSlider(activeDraggingSlider, mouseX, draggingSliderTrackX, draggingSliderTrackW);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        activeDraggingSlider = null;
        activeColorDrag = 0;
    }

    public boolean mouseScrolled(double delta) {
        float maxScroll = Math.max(0.0f, totalContentHeight - 200.0f);
        scrollTarget += (float) delta * 20.0f;
        scrollTarget = MathHelper.clamp(scrollTarget, -maxScroll, 0.0f);
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (activeBindingModule != null) {
            if (keyCode == 256 || keyCode == 261) {
                activeBindingModule.setBind(KeyBind.NONE);
            } else {
                activeBindingModule.setBind(KeyBind.keyboard(keyCode));
            }
            activeBindingModule = null;
            return true;
        }
        if (activeBindingSetting != null) {
            if (keyCode == 256 || keyCode == 261) {
                activeBindingSetting.setKey(-1);
            } else {
                activeBindingSetting.setKey(keyCode);
            }
            activeBindingSetting = null;
            return true;
        }
        if (activeFocusedText != null) {
            if (keyCode == 256 || keyCode == 257) {
                activeFocusedText = null;
                return true;
            }
            if (keyCode == 259) {
                String val = activeFocusedText.getText();
                if (val.length() > 0) {
                    activeFocusedText.setText(val.substring(0, val.length() - 1));
                }
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (activeFocusedText != null && !Character.isISOControl(chr)) {
            activeFocusedText.setText(activeFocusedText.getText() + chr);
            return true;
        }
        return false;
    }

    public boolean isEditingText() {
        return activeFocusedText != null;
    }

    public void warmup() {
    }

    private void updateSlider(SliderSetting slider, float mouseX, float sx, float sw) {
        float trackLeft = sx + 2.0f;
        float trackW = sw - 4.0f;
        float norm = trackW <= 0.001f ? 0.0f : MathHelper.clamp((mouseX - trackLeft) / trackW, 0.0f, 1.0f);
        float value = slider.getMin() + norm * (slider.getMax() - slider.getMin());
        float step = slider.getIncrement();
        if (step > 0.0f) {
            value = Math.round(value / step) * step;
        }
        value = MathHelper.clamp(value, slider.getMin(), slider.getMax());
        slider.setValue(value);
    }

    private float calculateCardHeight(Module module) {
        List<Setting> settings = getVisibleSettings(module);
        if (settings.isEmpty()) {
            return 25.0f;
        }
        boolean isExp = expandedModules.contains(module);
        AnimatedFloat anim = expandAnims.computeIfAbsent(module, m -> new AnimatedFloat(isExp ? 1.0f : 0.0f, 14.0f));
        anim.setTarget(isExp ? 1.0f : 0.0f);
        float progress = anim.getValue();
        if (progress <= 0.001f) {
            return 25.0f;
        }
        float fullHeight = calculateFullSettingsHeight(settings);
        return 25.0f + (fullHeight + 5.0f) * progress;
    }

    private float calculateFullSettingsHeight(List<Setting> settings) {
        float h = 0.0f;
        float sw = CARD_WIDTH - 16.0f;
        for (Setting s : settings) {
            if (s instanceof SeparatorSetting) h += 14.0f;
            else if (s instanceof BooleanSetting) h += 12.0f;
            else if (s instanceof SliderSetting) h += 19.0f;
            else if (s instanceof SelectSetting select) {
                h += getSelectSettingHeight(select.getOptions(), sw);
            } else if (s instanceof MultiSelectSetting multi) {
                h += getSelectSettingHeight(multi.getOptions(), sw);
            } else if (s instanceof ColorSetting) h += 12.0f;
            else if (s instanceof BindSetting) h += 12.0f;
            else if (s instanceof TextSetting) h += 25.0f;
            else if (s instanceof ButtonSetting) h += 17.0f;
            else h += 10.0f;
        }
        return h + 4.0f;
    }

    private float getSelectSettingHeight(List<String> options, float maxW) {
        float curX = 2.0f;
        float curY = 9.5f;
        float rowH = 13.0f;
        for (String opt : options) {
            float optW = Fonts.SF.width(opt, 7.5f) + 8.0f;
            if (curX + optW > maxW) {
                curX = 2.0f;
                curY += rowH + 2.0f;
            }
            curX += optW + 3.0f;
        }
        return curY + rowH + 2.0f;
    }

    private List<Module> getFilteredModules(Category category, String query) {
        List<Module> all = ModuleManager.get().getAll();
        List<Module> filtered = new ArrayList<>();
        String q = query != null ? query.trim().toLowerCase() : "";

        for (Module m : all) {
            if (m instanceof ClickGui || "ClickGui".equalsIgnoreCase(m.getName())) {
                continue;
            }
            if (!q.isEmpty()) {
                if (m.getName().toLowerCase().contains(q) || m.getDisplayName().toLowerCase().contains(q)) {
                    filtered.add(m);
                }
            } else {
                if (m.getCategory() == category) {
                    filtered.add(m);
                }
            }
        }
        filtered.sort(Comparator.comparing(Module::getDisplayName));
        return filtered;
    }

    private List<Setting> getVisibleSettings(Module module) {
        List<Setting> visible = new ArrayList<>();
        for (Setting s : module.getSettings().all()) {
            if (s.isVisible()) {
                visible.add(s);
            }
        }
        return visible;
    }

    public void resetScroll() {
        scroll = 0.0f;
        scrollTarget = 0.0f;
        activeDraggingSlider = null;
        activeBindingModule = null;
        activeBindingSetting = null;
        activeFocusedText = null;
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
