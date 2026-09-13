package rtx.heave.api.ui.minced;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import rtx.heave.api.ui.theme.ParticleStyle;
import rtx.heave.api.ui.theme.Theme;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public class GuiThemePanel {
    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;
    private float totalHeight = 0.0f;

    private boolean colorsOpen = true;
    private boolean particlesOpen = true;

    public void render(DrawContext context, float x, float y, float alpha, float mouseX, float mouseY) {
        float contentX = x + 104.0f;
        float contentY = y + 32.5f;
        float contentW = 344.5f;
        float contentH = 239.5f;

        scroll += (scrollTarget - scroll) * 0.25f;
        if (Math.abs(scrollTarget - scroll) < 0.01f) {
            scroll = scrollTarget;
        }

        Render2D.pushScissor(context, contentX, contentY, contentW, contentH);

        float curY = y + 40.0f + scroll;
        float startX = x + 114.0f;
        int accent = ThemeManager.accent(255.0f * alpha);
        int white = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        int muted = (Math.max(0, Math.min(255, (int)(160.0f * alpha))) << 24) | 0x888888;

        // 1. Colors Section Header
        Fonts.ICONSMINCED.draw(context, "x", startX, curY, 11.0f, accent);
        Fonts.SF_BOLD.draw(context, "Colors", startX + 13.0f, curY, 11.0f, white);
        Fonts.ICONSMINCED.draw(context, colorsOpen ? "z" : "x", startX + 320.0f, curY, 9.0f, muted);
        curY += 16.0f;

        if (colorsOpen) {
            Theme[] themes = Theme.values();
            float cardW = 105.0f;
            float cardH = 54.0f;
            float gapX = 5.0f;
            float gapY = 5.0f;

            for (int i = 0; i < themes.length; ++i) {
                int col = i % 3;
                int row = i / 3;
                float cx = startX + (float) col * (cardW + gapX);
                float cy = curY + (float) row * (cardH + gapY);

                Theme t = themes[i];
                boolean selected = (ThemeManager.current() == t);

                // Card background
                int cardAlpha = selected ? 35 : 18;
                int bg = (Math.max(0, Math.min(255, (int)(cardAlpha * alpha))) << 24) | (t.accentRgb() & 0xFFFFFF);
                Render2D.rect(cx, cy, cardW, cardH, 3.0f, bg);

                // Gradient preview
                int c1 = 0xFF000000 | (t.gradientA() & 0xFFFFFF);
                int c2 = 0xFF000000 | (t.gradientB() & 0xFFFFFF);
                Render2D.rect(cx + 8.0f, cy + 8.0f, cardW - 16.0f, 22.0f, 3.0f, c1, c2, c2, c1);

                // Theme name
                int nameColor = selected ? accent : white;
                Fonts.SF_BOLD.draw(context, t.getDisplayName(), cx + 8.0f, cy + 36.0f, 9.5f, nameColor);

                // Selection checkmark or radio dot
                if (selected) {
                    Render2D.rect(cx + cardW - 14.0f, cy + 37.0f, 6.0f, 6.0f, 3.0f, accent);
                }
            }

            int totalRows = (themes.length + 2) / 3;
            curY += (float) totalRows * (cardH + gapY) + 12.0f;
        }

        // 2. Particles Section Header
        Fonts.ICONSMINCED.draw(context, "b", startX, curY, 11.0f, accent);
        Fonts.SF_BOLD.draw(context, "Particles", startX + 13.0f, curY, 11.0f, white);
        Fonts.ICONSMINCED.draw(context, particlesOpen ? "z" : "x", startX + 320.0f, curY, 9.0f, muted);
        curY += 16.0f;

        if (particlesOpen) {
            ParticleStyle[] styles = ParticleStyle.values();
            float pCardW = 36.5f;
            float pCardH = 36.5f;
            float pGap = 5.0f;

            for (int i = 0; i < styles.length; ++i) {
                int col = i % 8;
                int row = i / 8;
                float px = startX + (float) col * (pCardW + pGap);
                float py = curY + (float) row * (pCardH + pGap + 12.0f);

                ParticleStyle style = styles[i];
                boolean selected = (ThemeManager.currentParticle() == style);

                int pBg = (Math.max(0, Math.min(255, (int)((selected ? 35 : 18) * alpha))) << 24) | 0xFFFFFF;
                Render2D.rect(px, py, pCardW, pCardH, 3.0f, pBg);

                if (selected) {
                    Render2D.outline(px, py, pCardW, pCardH, 3.0f, 1.0f, accent);
                }

                // Render particle icon
                int iconColor = selected ? accent : white;
                Render2D.image("heave:images/particles/" + style.getTextureName() + ".png", px + 8.25f, py + 8.25f, 20.0f, 20.0f, 0.0f, iconColor);

                // Label below
                float lw = Fonts.SF.width(style.getDisplayName(), 7.0f);
                Fonts.SF.draw(context, style.getDisplayName(), px + (pCardW - lw) * 0.5f, py + pCardH + 3.0f, 7.0f, iconColor);
            }

            int pRows = (styles.length + 7) / 8;
            curY += (float) pRows * (pCardH + pGap + 12.0f);
        }

        totalHeight = curY - (y + 40.0f + scroll);
        Render2D.popScissor(context);
    }

    public boolean mouseClicked(float mouseX, float mouseY, float x, float y) {
        float startX = x + 114.0f;
        float curY = y + 40.0f + scroll;

        // Toggle Colors header
        if (mouseX >= startX && mouseX <= startX + 330.0f && mouseY >= curY && mouseY <= curY + 16.0f) {
            colorsOpen = !colorsOpen;
            return true;
        }
        curY += 16.0f;

        if (colorsOpen) {
            Theme[] themes = Theme.values();
            float cardW = 105.0f;
            float cardH = 54.0f;
            float gapX = 5.0f;
            float gapY = 5.0f;

            for (int i = 0; i < themes.length; ++i) {
                int col = i % 3;
                int row = i / 3;
                float cx = startX + (float) col * (cardW + gapX);
                float cy = curY + (float) row * (cardH + gapY);

                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                    ThemeManager.set(themes[i]);
                    return true;
                }
            }

            int totalRows = (themes.length + 2) / 3;
            curY += (float) totalRows * (cardH + gapY) + 12.0f;
        }

        // Toggle Particles header
        if (mouseX >= startX && mouseX <= startX + 330.0f && mouseY >= curY && mouseY <= curY + 16.0f) {
            particlesOpen = !particlesOpen;
            return true;
        }
        curY += 16.0f;

        if (particlesOpen) {
            ParticleStyle[] styles = ParticleStyle.values();
            float pCardW = 36.5f;
            float pCardH = 36.5f;
            float pGap = 5.0f;

            for (int i = 0; i < styles.length; ++i) {
                int col = i % 8;
                int row = i / 8;
                float px = startX + (float) col * (pCardW + pGap);
                float py = curY + (float) row * (pCardH + pGap + 12.0f);

                if (mouseX >= px && mouseX <= px + pCardW && mouseY >= py && mouseY <= py + pCardH) {
                    ThemeManager.setParticle(styles[i]);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean mouseScrolled(double delta) {
        float maxScroll = Math.max(0.0f, totalHeight - 230.0f);
        scrollTarget += (float) delta * 20.0f;
        scrollTarget = MathHelper.clamp(scrollTarget, -maxScroll, 0.0f);
        return true;
    }

    public void resetScroll() {
        scroll = 0.0f;
        scrollTarget = 0.0f;
    }
}
