package rtx.heave.api.ui.minced;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import rtx.heave.api.modules.Category;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.animations.AnimatedFloat;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public class GuiSidebar {
    private static final Category[] TOP_CATEGORIES = new Category[]{
        Category.VISUALS,
        Category.DISPLAY,
        Category.UTILS
    };

    private static final Category[] BOTTOM_CATEGORIES = new Category[]{
        Category.EVENTS,
        Category.CONFIGS
    };

    private final Map<Category, AnimatedFloat> categoryAnims = new EnumMap<>(Category.class);
    private Category currentCategory = Category.VISUALS;

    public GuiSidebar(Category initial) {
        if (initial != null) {
            this.currentCategory = initial;
        }
        for (Category cat : TOP_CATEGORIES) {
            categoryAnims.put(cat, new AnimatedFloat(cat == currentCategory ? 1.0f : 0.0f, 12.0f));
        }
        for (Category cat : BOTTOM_CATEGORIES) {
            categoryAnims.put(cat, new AnimatedFloat(cat == currentCategory ? 1.0f : 0.0f, 12.0f));
        }
    }

    public void render(DrawContext context, float x, float y, float alpha, Category category) {
        if (category != null) {
            this.currentCategory = category;
        }
        renderCategories(context, x, y, alpha);
    }

    private void renderCategories(DrawContext context, float x, float y, float alpha) {
        float btnX = x + 8.0f;
        float btnW = 90.0f;
        float btnH = 20.0f;
        float radius = 5.0f;

        // 1. Top Section: Visuals, Display, Utils
        float startY = y + 42.0f;
        for (int i = 0; i < TOP_CATEGORIES.length; ++i) {
            Category cat = TOP_CATEGORIES[i];
            float rowY = startY + (float) i * 24.0f;
            renderCategoryButton(context, cat, btnX, rowY, btnW, btnH, radius, alpha);
        }

        // 2. Subtle divider above bottom section
        int lineCol = (Math.max(0, Math.min(255, (int)(25.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(btnX + 4.0f, y + 214.0f, btnW - 8.0f, 0.5f, 0.0f, lineCol);

        // 3. Bottom Section: Events, Configs
        float eventsY = y + 220.5f;
        renderCategoryButton(context, Category.EVENTS, btnX, eventsY, btnW, btnH, radius, alpha);

        float configsY = y + 244.5f;
        renderCategoryButton(context, Category.CONFIGS, btnX, configsY, btnW, btnH, radius, alpha);
    }

    private void renderCategoryButton(DrawContext context, Category cat, float btnX, float rowY, float btnW, float btnH, float radius, float alpha) {
        boolean active = (cat == currentCategory);
        AnimatedFloat anim = categoryAnims.computeIfAbsent(cat, c -> new AnimatedFloat(active ? 1.0f : 0.0f, 12.0f));
        anim.setTarget(active ? 1.0f : 0.0f);
        float progress = anim.getValue();

        // Active pill background
        if (progress > 0.01f) {
            int pillColor = ThemeManager.accent(30.0f * progress * alpha);
            Render2D.rect(btnX, rowY, btnW, btnH, radius, pillColor);

            // Left vertical accent bar
            int barColor = ThemeManager.accent(255.0f * progress * alpha);
            Render2D.rect(btnX + 1.0f, rowY + 4.0f, 2.0f, btnH - 8.0f, 1.0f, barColor);
        }

        int accent = ThemeManager.accent(255.0f * alpha);
        int inactiveIcon = (Math.max(0, Math.min(255, (int)(110.0f * alpha))) << 24) | 0xFFFFFF;
        int inactiveText = (Math.max(0, Math.min(255, (int)(140.0f * alpha))) << 24) | 0xFFFFFF;
        int white = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;

        int iconCol = lerpColor(inactiveIcon, accent, progress);
        int textCol = lerpColor(inactiveText, white, progress);

        // Icon from Fonts.KIMIKO
        String icon = getCategoryIcon(cat);
        Fonts.KIMIKO.draw(icon, btnX + 9.0f, rowY + 5.5f, 9.5f, iconCol);

        // Category Name
        Fonts.SF_BOLD.draw(cat.getDisplayName(), btnX + 25.0f, rowY + 5.5f, 9.5f, textCol);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button, float x, float y) {
        if (button != 0) return false;

        float btnX = x + 8.0f;
        float btnW = 90.0f;
        float btnH = 20.0f;

        // Top categories
        float startY = y + 42.0f;
        for (int i = 0; i < TOP_CATEGORIES.length; ++i) {
            float rowY = startY + (float) i * 24.0f;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= rowY && mouseY <= rowY + btnH) {
                currentCategory = TOP_CATEGORIES[i];
                return true;
            }
        }

        // Bottom categories
        float eventsY = y + 220.5f;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= eventsY && mouseY <= eventsY + btnH) {
            currentCategory = Category.EVENTS;
            return true;
        }

        float configsY = y + 244.5f;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= configsY && mouseY <= configsY + btnH) {
            currentCategory = Category.CONFIGS;
            return true;
        }

        return false;
    }

    public Category getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(Category category) {
        if (category != null) {
            this.currentCategory = category;
        }
    }

    private static String getCategoryIcon(Category category) {
        if (category == null) return "p";
        return switch (category) {
            case VISUALS -> "p";
            case DISPLAY -> "j";
            case UTILS -> "r";
            case EVENTS -> "i";
            case CONFIGS -> "h";
            default -> "p";
        };
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
