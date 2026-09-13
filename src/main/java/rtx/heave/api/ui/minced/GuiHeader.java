package rtx.heave.api.ui.minced;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import rtx.heave.api.modules.Category;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public class GuiHeader {
    private static final Identifier STAR_ICON = Identifier.of("heave", "textures/logo.png");
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchFocused = false;

    public void render(DrawContext context, float x, float y, float alpha, float contentAlpha, boolean isThemeView, Category category) {
        int accent = ThemeManager.accent(255.0f * alpha);
        int white = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        int muted = (Math.max(0, Math.min(255, (int)(140.0f * alpha))) << 24) | 0x7A808A;

        // 1. Star particle logo & "Heave Visuals" - perfectly centered vertically at y + 16.0f
        float starSize = 13.0f;
        float starX = (float) Math.round(x + 7.0f);
        float starY = (float) Math.round(y + (32.0f - starSize) / 2.0f);

        // Star particle logo in theme accent color (rendered via procedural analytic GLSL shader)
        Render2D.star(starX, starY, starSize, accent);

        // "Heave Visuals" text vertically centered at y + 16.0f
        float textX = starX + starSize + 5.0f;
        float textY = y + 9.5f;
        Fonts.SF_BOLD.draw("Heave Visuals", textX, textY, 10.5f, white);

        // 2. Search Bar in Header (centered vertically at y + 16.0f)
        float searchX = x + 114.0f;
        float searchY = y + 7.0f;
        float searchW = 324.0f;
        float searchH = 18.0f;
        float radius = 4.0f;

        // Search Bar Background
        int sbBg = (Math.max(0, Math.min(255, (int)(245.0f * alpha))) << 24) | 0x111117;
        Render2D.rect(searchX, searchY, searchW, searchH, radius, sbBg);

        // Search Bar Outline
        if (searchFocused) {
            Render2D.outline(searchX, searchY, searchW, searchH, radius, 1.0f, ThemeManager.accent(220.0f * alpha));
        } else {
            int border = (Math.max(0, Math.min(255, (int)(32.0f * alpha))) << 24) | 0xFFFFFF;
            Render2D.outline(searchX, searchY, searchW, searchH, radius, 0.5f, border);
        }

        // Search icon (magnifying glass)
        int iconCol = searchFocused ? accent : muted;
        Fonts.KIMIKO.draw("q", searchX + 6.0f, searchY + 4.5f, 8.5f, iconCol);

        // Scissored text area
        float textInputX = searchX + 19.0f;
        float textMaxW = searchW - 36.0f;
        Render2D.pushScissor(context, textInputX, searchY, textMaxW, searchH);

        String query = searchBuffer.toString();
        if (query.isEmpty() && !searchFocused) {
            Fonts.SF.draw("Поиск по модулям...", textInputX, searchY + 4.5f, 8.5f, muted);
        } else {
            Fonts.SF.draw(query, textInputX, searchY + 4.5f, 8.5f, white);
            if (searchFocused && (System.currentTimeMillis() / 450) % 2 == 0) {
                float tw = Fonts.SF.width(query, 8.5f);
                int cursorCol = (Math.max(0, Math.min(255, (int)(230.0f * alpha))) << 24) | 0xFFFFFF;
                Render2D.rect(textInputX + tw + 1.0f, searchY + 4.0f, 1.0f, 10.0f, 0.0f, cursorCol);
            }
        }
        Render2D.popScissor(context);

        // Clear button (x)
        if (!query.isEmpty()) {
            float clearX = searchX + searchW - 14.0f;
            int clearCol = (Math.max(0, Math.min(255, (int)(170.0f * alpha))) << 24) | 0x9999AA;
            Fonts.SF_BOLD.draw("x", clearX, searchY + 4.0f, 8.5f, clearCol);
        }

        // 3. Subtle divider lines
        int lineCol = (Math.max(0, Math.min(255, (int)(30.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(x, y + 32.0f, 448.5f, 0.5f, 0.0f, lineCol);
        Render2D.rect(x + 106.0f, y, 0.5f, 272.5f, 0.0f, lineCol);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button, float x, float y) {
        float searchX = x + 114.0f;
        float searchY = y + 7.0f;
        float searchW = 324.0f;
        float searchH = 18.0f;

        // Clear button click
        if (searchBuffer.length() > 0) {
            float clearX = searchX + searchW - 16.0f;
            if (mouseX >= clearX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                clearSearch();
                return true;
            }
        }

        // Search bar click
        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
            if (button == 0) {
                searchFocused = true;
                return true;
            } else if (button == 1) {
                clearSearch();
                return true;
            }
        } else {
            searchFocused = false;
        }

        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!searchFocused) return false;

        if (keyCode == 256) { // Escape
            searchFocused = false;
            return true;
        }
        if (keyCode == 257) { // Enter
            searchFocused = false;
            return true;
        }
        if (keyCode == 259) { // Backspace
            if (searchBuffer.length() > 0) {
                searchBuffer.deleteCharAt(searchBuffer.length() - 1);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (searchFocused && !Character.isISOControl(chr)) {
            searchBuffer.append(chr);
            return true;
        }
        return false;
    }

    public String getSearchQuery() {
        return searchBuffer.toString().trim();
    }

    public void clearSearch() {
        searchBuffer.setLength(0);
        searchFocused = false;
    }

    public boolean isSearchFocused() {
        return searchFocused;
    }

    public void setSearchFocused(boolean focused) {
        this.searchFocused = focused;
    }

    public boolean isCustomizeHovered(float mouseX, float mouseY, float x, float y) {
        return false;
    }
}
