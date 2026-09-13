package rtx.heave.api.ui.settings.impl;

import rtx.heave.api.modules.settings.impl.ButtonSetting;
import rtx.heave.api.ui.settings.Setting;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public class ButtonRowSetting implements Setting {
    public static final float HEIGHT = 20.0f;
    private final ButtonSetting backend;

    public ButtonRowSetting(ButtonSetting backend) {
        this.backend = backend;
    }

    @Override
    public String name() {
        return this.backend.getName();
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    @Override
    public void render(float x, float y, float width, float alpha) {
        float btnX = x + 4.0f;
        float btnY = y + 2.0f;
        float btnW = width - 8.0f;
        float btnH = 16.0f;

        String label = this.backend.getLabel();
        if (label == null || label.isEmpty()) {
            label = this.backend.getName();
        }

        int bg = ColorUtil.multAlpha(0x181824, 0.95f * alpha);
        int border = ThemeManager.accent(160.0f * alpha);
        Render2D.rect(btnX, btnY, btnW, btnH, 4.0f, bg);
        Render2D.outline(btnX, btnY, btnW, btnH, 4.0f, 0.6f, border);

        float textW = Fonts.SF_MEDIUM.width(label, 7.0f);
        int textCol = ThemeManager.accent(255.0f * alpha);
        Fonts.SF_MEDIUM.draw(label, btnX + (btnW - textW) / 2.0f, btnY + (btnH - 7.0f) / 2.0f + 0.5f, 7.0f, textCol);
    }

    @Override
    public boolean isVisible() {
        return this.backend.isVisible();
    }

    @Override
    public boolean click(float x, float y, float width, float mouseX, float mouseY) {
        float btnX = x + 4.0f;
        float btnY = y + 2.0f;
        float btnW = width - 8.0f;
        float btnH = 16.0f;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            this.backend.click();
            return true;
        }
        return false;
    }

    @Override
    public float preferredWidth() {
        String label = this.backend.getLabel();
        if (label == null || label.isEmpty()) label = this.backend.getName();
        return Fonts.SF_MEDIUM.width(label, 7.0f) + 24.0f;
    }
}
