package rtx.heave.api.ui.minced;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import rtx.heave.api.config.ConfigManager;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.animations.AnimatedFloat;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public class GuiConfigsPanel {
    public enum SortMode {
        NEWEST("Сначала новые"),
        OLDEST("Сначала старые"),
        BY_NAME("По имени");

        private final String title;

        SortMode(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private static class ConfigCardState {
        final AnimatedFloat hover = new AnimatedFloat(0.0f, 12.0f);
        final AnimatedFloat saveHover = new AnimatedFloat(0.0f, 12.0f);
        final AnimatedFloat loadHover = new AnimatedFloat(0.0f, 12.0f);
        final AnimatedFloat deleteHover = new AnimatedFloat(0.0f, 12.0f);
    }

    private static class ConfigItem {
        final String name;
        final long lastModified;

        ConfigItem(String name, long lastModified) {
            this.name = name;
            this.lastModified = lastModified;
        }
    }

    private final StringBuilder createBuffer = new StringBuilder();
    private boolean modalOpen = false;
    private boolean modalFocused = true;
    private final AnimatedFloat modalAnim = new AnimatedFloat(0.0f, 12.0f);
    private final AnimatedFloat createBtnHover = new AnimatedFloat(0.0f, 12.0f);

    private final AnimatedFloat createTopHover = new AnimatedFloat(0.0f, 12.0f);
    private final AnimatedFloat importTopHover = new AnimatedFloat(0.0f, 12.0f);
    private final AnimatedFloat sortDropdownAnim = new AnimatedFloat(0.0f, 12.0f);
    private boolean sortOpen = false;
    private SortMode sortMode = SortMode.NEWEST;
    private final AnimatedFloat[] sortOptionHover = new AnimatedFloat[SortMode.values().length];

    private final Map<String, ConfigCardState> cardStates = new HashMap<>();
    private float scrollTarget = 0.0f;
    private float scrollCurrent = 0.0f;
    private float contentHeight = 0.0f;
    private String activeLoadedConfig = "";

    public GuiConfigsPanel() {
        for (int i = 0; i < sortOptionHover.length; i++) {
            sortOptionHover[i] = new AnimatedFloat(0.0f, 12.0f);
        }
    }

    public boolean isModalOpen() {
        return modalOpen;
    }

    public void openCreateModal() {
        this.modalOpen = true;
        this.modalFocused = true;
        this.createBuffer.setLength(0);
        this.sortOpen = false;
    }

    public void closeModal() {
        this.modalOpen = false;
        this.modalFocused = false;
    }

    public void reset() {
        this.modalOpen = false;
        this.modalFocused = false;
        this.sortOpen = false;
        this.scrollTarget = 0.0f;
        this.scrollCurrent = 0.0f;
    }

    public void render(DrawContext context, float x, float y, float alpha, float mouseX, float mouseY) {
        float startX = x + 114.0f;
        float startY = y + 40.0f;
        float width = 326.5f;
        float height = 224.5f;

        renderTopBar(context, startX, startY, width, alpha, mouseX, mouseY);

        float listY = startY + 22.0f;
        float listH = height - 22.0f;

        Render2D.pushScissor(context, startX, listY, width, listH);

        // Smooth scroll
        float maxScroll = Math.max(0.0f, contentHeight - listH + 8.0f);
        scrollTarget = MathHelper.clamp(scrollTarget, -maxScroll, 0.0f);
        scrollCurrent = MathHelper.lerp(0.18f, scrollCurrent, scrollTarget);

        List<ConfigItem> items = loadConfigs();
        if (items.isEmpty()) {
            String empty = "У вас пока нет сохранённых конфигов";
            float tw = Fonts.SF.width(empty, 9.5f);
            int textColor = (Math.max(0, Math.min(255, (int)(110.0f * alpha))) << 24) | 0xFFFFFF;
            Fonts.SF.draw(context, empty, startX + (width - tw) * 0.5f, listY + listH * 0.4f, 9.5f, textColor);
            contentHeight = 0.0f;
        } else {
            float cardW = 158.0f;
            float cardH = 58.0f;
            float gapX = 8.0f;
            float gapY = 6.0f;

            int rows = 0;
            for (int i = 0; i < items.size(); i++) {
                ConfigItem item = items.get(i);
                int col = i % 2;
                int row = i / 2;
                rows = row + 1;

                float cx = startX + (float) col * (cardW + gapX);
                float cy = listY + scrollCurrent + (float) row * (cardH + gapY);

                if (cy + cardH >= listY - 10.0f && cy <= listY + listH + 10.0f) {
                    renderConfigCard(context, item, cx, cy, cardW, cardH, alpha, mouseX, mouseY);
                }
            }
            contentHeight = (float) rows * (cardH + gapY);
        }

        Render2D.popScissor(context);

        // Render sort dropdown overlay if open
        renderSortDropdown(context, startX, startY, width, alpha, mouseX, mouseY);
    }

    private void renderTopBar(DrawContext context, float x, float y, float width, float alpha, float mouseX, float mouseY) {
        int accent = ThemeManager.accent(255.0f * alpha);
        int white = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;

        float createW = Fonts.SF_BOLD.width("Создать", 9.5f) + 24.0f;
        float btnH = 16.0f;
        boolean overCreate = mouseX >= x && mouseX <= x + createW && mouseY >= y && mouseY <= y + btnH;
        createTopHover.setTarget(overCreate ? 1.0f : 0.0f);
        int createBg = (Math.max(0, Math.min(255, (int)((20.0f + 30.0f * createTopHover.getValue()) * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(x, y, createW, btnH, 4.0f, createBg);
        Fonts.MINCED_ICONS.draw(context, "I", x + 5.0f, y + 4.5f, 9.0f, accent);
        Fonts.SF_BOLD.draw(context, "Создать", x + 16.0f, y + 4.5f, 9.5f, white);

        float folderX = x + createW + 6.0f;
        float folderW = Fonts.SF_BOLD.width("Папка", 9.5f) + 22.0f;
        boolean overFolder = mouseX >= folderX && mouseX <= folderX + folderW && mouseY >= y && mouseY <= y + btnH;
        importTopHover.setTarget(overFolder ? 1.0f : 0.0f);
        int folderBg = (Math.max(0, Math.min(255, (int)((20.0f + 30.0f * importTopHover.getValue()) * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(folderX, y, folderW, btnH, 4.0f, folderBg);
        Fonts.MINCED_ICONS.draw(context, "t", folderX + 5.0f, y + 4.5f, 9.0f, (Math.max(0, Math.min(255, (int)(180.0f * alpha))) << 24) | 0xFFFFFF);
        Fonts.SF_BOLD.draw(context, "Папка", folderX + 16.0f, y + 4.5f, 9.5f, white);

        // Sort selector on right
        float sortW = Fonts.SF.width(sortMode.getTitle(), 9.0f) + 24.0f;
        float sortX = x + width - sortW;
        boolean overSort = mouseX >= sortX && mouseX <= sortX + sortW && mouseY >= y && mouseY <= y + btnH;
        int sortBg = (Math.max(0, Math.min(255, (int)((overSort ? 35.0f : 20.0f) * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(sortX, y, sortW, btnH, 4.0f, sortBg);
        Fonts.SF.draw(context, sortMode.getTitle(), sortX + 7.0f, y + 4.5f, 9.0f, white);
        Fonts.MINCED_ICONS.draw(context, sortOpen ? "u" : "z", sortX + sortW - 12.0f, y + 5.0f, 7.5f, (Math.max(0, Math.min(255, (int)(140.0f * alpha))) << 24) | 0xFFFFFF);
    }

    private void renderSortDropdown(DrawContext context, float x, float y, float width, float alpha, float mouseX, float mouseY) {
        sortDropdownAnim.setTarget(sortOpen ? 1.0f : 0.0f);
        float progress = sortDropdownAnim.getValue();
        if (progress < 0.02f) return;

        float sortW = Fonts.SF.width(sortMode.getTitle(), 9.0f) + 24.0f;
        float sortX = x + width - sortW;
        float dropY = y + 18.0f;
        float itemH = 16.0f;
        float dropH = (float) SortMode.values().length * itemH + 4.0f;

        int bgAlpha = (int)(240.0f * progress * alpha);
        Render2D.rect(sortX, dropY, sortW, dropH, 4.0f, (bgAlpha << 24) | 0x0D0D12);
        Render2D.outline(sortX, dropY, sortW, dropH, 4.0f, 0.5f, (Math.max(0, Math.min(255, (int)(40.0f * progress * alpha))) << 24) | 0xFFFFFF);

        SortMode[] modes = SortMode.values();
        for (int i = 0; i < modes.length; i++) {
            SortMode mode = modes[i];
            float iy = dropY + 2.0f + (float) i * itemH;
            boolean hovered = mouseX >= sortX && mouseX <= sortX + sortW && mouseY >= iy && mouseY <= iy + itemH;
            sortOptionHover[i].setTarget(hovered ? 1.0f : 0.0f);

            boolean active = mode == sortMode;
            if (active || sortOptionHover[i].getValue() > 0.01f) {
                int pillAlpha = (int)((active ? 40.0f : 20.0f * sortOptionHover[i].getValue()) * progress * alpha);
                Render2D.rect(sortX + 2.0f, iy, sortW - 4.0f, itemH - 1.0f, 3.0f, (pillAlpha << 24) | 0xFFFFFF);
            }

            int textColor = active ? ThemeManager.accent(255.0f * progress * alpha) : ((Math.max(0, Math.min(255, (int)(180.0f * progress * alpha))) << 24) | 0xFFFFFF);
            Fonts.SF.draw(context, mode.getTitle(), sortX + 6.0f, iy + 4.0f, 8.5f, textColor);
        }
    }

    private void renderConfigCard(DrawContext context, ConfigItem item, float x, float y, float w, float h, float alpha, float mouseX, float mouseY) {
        ConfigCardState state = cardStates.computeIfAbsent(item.name, k -> new ConfigCardState());
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        state.hover.setTarget(hovered ? 1.0f : 0.0f);

        boolean isActive = item.name.equalsIgnoreCase(activeLoadedConfig);

        // Card background
        int cardBg = (Math.max(0, Math.min(255, (int)((14.0f + 10.0f * state.hover.getValue()) * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(x, y, w, h, 6.0f, cardBg);

        // Border or active accent
        if (isActive) {
            int accent = ThemeManager.accent(180.0f * alpha);
            Render2D.outline(x, y, w, h, 6.0f, 1.0f, accent);
            Render2D.rect(x + 2.0f, y + 10.0f, 2.0f, h - 20.0f, 1.0f, accent);
        } else {
            int border = (Math.max(0, Math.min(255, (int)((20.0f + 25.0f * state.hover.getValue()) * alpha))) << 24) | 0xFFFFFF;
            Render2D.outline(x, y, w, h, 6.0f, 0.5f, border);
        }

        // Title
        int titleColor = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF_BOLD.draw(context, item.name, x + 8.0f, y + 8.0f, 10.5f, titleColor);

        // Subtitle (date)
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(item.lastModified));
        int dateColor = (Math.max(0, Math.min(255, (int)(110.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(context, dateStr, x + 8.0f, y + 20.0f, 8.0f, dateColor);

        // Divider
        int divColor = (Math.max(0, Math.min(255, (int)(20.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(x + 8.0f, y + 31.0f, w - 16.0f, 0.5f, 0.0f, divColor);

        float btnY = y + 36.0f;
        float btnH = 16.0f;
        float btnW = 44.0f;

        // 1. Save
        float saveX = x + 8.0f;
        boolean overSave = mouseX >= saveX && mouseX <= saveX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        state.saveHover.setTarget(overSave ? 1.0f : 0.0f);
        int saveBg = ThemeManager.accent((18.0f + 28.0f * state.saveHover.getValue()) * alpha);
        Render2D.rect(saveX, btnY, btnW, btnH, 3.0f, saveBg);
        int saveText = ThemeManager.accent(255.0f * alpha);
        float saveTw = Fonts.SF_BOLD.width("Сохранить", 7.5f);
        Fonts.SF_BOLD.draw(context, "Сохранить", saveX + (btnW - saveTw) * 0.5f, btnY + 4.5f, 7.5f, saveText);

        // 2. Load
        float loadX = saveX + btnW + 5.0f;
        boolean overLoad = mouseX >= loadX && mouseX <= loadX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        state.loadHover.setTarget(overLoad ? 1.0f : 0.0f);
        int loadBg = (Math.max(0, Math.min(255, (int)((18.0f + 25.0f * state.loadHover.getValue()) * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(loadX, btnY, btnW, btnH, 3.0f, loadBg);
        int loadText = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0xFFFFFF;
        float loadTw = Fonts.SF_BOLD.width("Загрузить", 7.5f);
        Fonts.SF_BOLD.draw(context, "Загрузить", loadX + (btnW - loadTw) * 0.5f, btnY + 4.5f, 7.5f, loadText);

        // 3. Delete
        float delX = loadX + btnW + 5.0f;
        boolean overDel = mouseX >= delX && mouseX <= delX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        state.deleteHover.setTarget(overDel ? 1.0f : 0.0f);
        int delBg = (Math.max(0, Math.min(255, (int)((18.0f + 25.0f * state.deleteHover.getValue()) * alpha))) << 24) | 0xFF4444;
        Render2D.rect(delX, btnY, btnW, btnH, 3.0f, delBg);
        int delText = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0xFF5555;
        float delTw = Fonts.SF_BOLD.width("Удалить", 7.5f);
        Fonts.SF_BOLD.draw(context, "Удалить", delX + (btnW - delTw) * 0.5f, btnY + 4.5f, 7.5f, delText);
    }

    public void renderModal(DrawContext context, float x, float y, float alpha, float mouseX, float mouseY) {
        modalAnim.setTarget(modalOpen ? 1.0f : 0.0f);
        float progress = modalAnim.getValue();
        if (progress < 0.01f) return;

        float winW = 448.5f;
        float winH = 272.5f;

        // Dark dim overlay
        int dimColor = (Math.max(0, Math.min(255, (int)(150.0f * progress * alpha))) << 24);
        Render2D.rect(x, y, winW, winH, 8.0f, dimColor);

        // Centered modal box
        float mw = 175.0f;
        float mh = 105.0f;
        float mx = x + (winW - mw) * 0.5f;
        float my = y + (winH - mh) * 0.5f;

        int boxBg = (Math.max(0, Math.min(255, (int)(250.0f * progress * alpha))) << 24) | 0x0E0E14;
        Render2D.rect(mx, my, mw, mh, 8.0f, boxBg);
        Render2D.outline(mx, my, mw, mh, 8.0f, 0.8f, (Math.max(0, Math.min(255, (int)(45.0f * progress * alpha))) << 24) | 0xFFFFFF);

        // Header
        int accent = ThemeManager.accent(255.0f * progress * alpha);
        Fonts.MINCED_ICONS.draw(context, "f", mx + 10.0f, my + 10.0f, 10.0f, accent);
        Fonts.SF_BOLD.draw(context, "Создание конфигурации", mx + 24.0f, my + 9.5f, 9.5f, (Math.max(0, Math.min(255, (int)(255.0f * progress * alpha))) << 24) | 0xFFFFFF);
        Fonts.SF.draw(context, "Введите имя для нового конфига:", mx + 10.0f, my + 23.0f, 8.0f, (Math.max(0, Math.min(255, (int)(130.0f * progress * alpha))) << 24) | 0xFFFFFF);

        // Text input field
        float tfX = mx + 10.0f;
        float tfY = my + 38.0f;
        float tfW = mw - 20.0f;
        float tfH = 20.0f;

        int tfBg = (Math.max(0, Math.min(255, (int)(30.0f * progress * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(tfX, tfY, tfW, tfH, 4.0f, tfBg);
        Render2D.outline(tfX, tfY, tfW, tfH, 4.0f, 0.5f, modalFocused ? accent : ((Math.max(0, Math.min(255, (int)(40.0f * progress * alpha))) << 24) | 0xFFFFFF));

        Fonts.MINCED_ICONS.draw(context, "H", tfX + 6.0f, tfY + 6.0f, 8.5f, (Math.max(0, Math.min(255, (int)(120.0f * progress * alpha))) << 24) | 0xFFFFFF);

        String text = createBuffer.toString();
        if (text.isEmpty()) {
            Fonts.SF.draw(context, "Название...", tfX + 18.0f, tfY + 6.0f, 8.5f, (Math.max(0, Math.min(255, (int)(80.0f * progress * alpha))) << 24) | 0xFFFFFF);
        } else {
            Fonts.SF.draw(context, text, tfX + 18.0f, tfY + 6.0f, 8.5f, (Math.max(0, Math.min(255, (int)(255.0f * progress * alpha))) << 24) | 0xFFFFFF);
        }

        // Blinking cursor
        if (modalFocused && (System.currentTimeMillis() / 450) % 2 == 0) {
            float curX = tfX + 18.0f + (text.isEmpty() ? 0.0f : Fonts.SF.width(text, 8.5f)) + 1.0f;
            Render2D.rect(curX, tfY + 4.0f, 1.0f, 12.0f, 0.0f, (Math.max(0, Math.min(255, (int)(200.0f * progress * alpha))) << 24) | 0xFFFFFF);
        }

        float btnW = mw - 20.0f;
        float btnH = 20.0f;
        float btnX = mx + 10.0f;
        float btnY = my + mh - 28.0f;

        boolean canCreate = !createBuffer.toString().trim().isEmpty();
        boolean overBtn = canCreate && mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        createBtnHover.setTarget(overBtn ? 1.0f : 0.0f);

        int submitBg = canCreate ? ThemeManager.accent((160.0f + 95.0f * createBtnHover.getValue()) * progress * alpha) : ((Math.max(0, Math.min(255, (int)(30.0f * progress * alpha))) << 24) | 0xFFFFFF);
        Render2D.rect(btnX, btnY, btnW, btnH, 4.0f, submitBg);

        String submitLabel = "Создать конфигурацию";
        float slW = Fonts.SF_BOLD.width(submitLabel, 9.0f);
        int submitText = canCreate ? ((Math.max(0, Math.min(255, (int)(255.0f * progress * alpha))) << 24) | 0xFFFFFF) : ((Math.max(0, Math.min(255, (int)(90.0f * progress * alpha))) << 24) | 0xFFFFFF);
        Fonts.SF_BOLD.draw(context, submitLabel, btnX + (btnW - slW) * 0.5f, btnY + 6.0f, 9.0f, submitText);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button, float x, float y) {
        if (button != 0) return false;

        float startX = x + 114.0f;
        float startY = y + 40.0f;
        float width = 326.5f;
        float height = 224.5f;

        // If modal open, handle modal clicks
        if (modalOpen) {
            float winW = 448.5f;
            float winH = 272.5f;
            float mw = 175.0f;
            float mh = 105.0f;
            float mx = x + (winW - mw) * 0.5f;
            float my = y + (winH - mh) * 0.5f;

            // Submit button
            float btnW = mw - 20.0f;
            float btnH = 20.0f;
            float btnX = mx + 10.0f;
            float btnY = my + mh - 28.0f;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                submitCreate();
                return true;
            }

            // Text input field
            float tfX = mx + 10.0f;
            float tfY = my + 38.0f;
            float tfW = mw - 20.0f;
            float tfH = 20.0f;
            if (mouseX >= tfX && mouseX <= tfX + tfW && mouseY >= tfY && mouseY <= tfY + tfH) {
                modalFocused = true;
                return true;
            }

            // Outside modal box -> close
            if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
                closeModal();
                return true;
            }
            return true;
        }

        // Top bar buttons
        float createW = Fonts.SF_BOLD.width("Создать", 9.5f) + 24.0f;
        float btnH = 16.0f;
        if (mouseX >= startX && mouseX <= startX + createW && mouseY >= startY && mouseY <= startY + btnH) {
            openCreateModal();
            return true;
        }

        float folderX = startX + createW + 6.0f;
        float folderW = Fonts.SF_BOLD.width("Папка", 9.5f) + 22.0f;
        if (mouseX >= folderX && mouseX <= folderX + folderW && mouseY >= startY && mouseY <= startY + btnH) {
            openFolder();
            return true;
        }

        // Sort dropdown toggle
        float sortW = Fonts.SF.width(sortMode.getTitle(), 9.0f) + 24.0f;
        float sortX = startX + width - sortW;
        if (mouseX >= sortX && mouseX <= sortX + sortW && mouseY >= startY && mouseY <= startY + btnH) {
            sortOpen = !sortOpen;
            return true;
        }

        // Sort options
        if (sortOpen) {
            float dropY = startY + 18.0f;
            float itemH = 16.0f;
            SortMode[] modes = SortMode.values();
            for (int i = 0; i < modes.length; i++) {
                float iy = dropY + 2.0f + (float) i * itemH;
                if (mouseX >= sortX && mouseX <= sortX + sortW && mouseY >= iy && mouseY <= iy + itemH) {
                    sortMode = modes[i];
                    sortOpen = false;
                    return true;
                }
            }
            sortOpen = false;
        }

        // Config cards actions
        float listY = startY + 22.0f;
        float listH = height - 22.0f;
        if (mouseX < startX || mouseX > startX + width || mouseY < listY || mouseY > listY + listH) {
            return false;
        }

        List<ConfigItem> items = loadConfigs();
        float cardW = 158.0f;
        float cardH = 58.0f;
        float gapX = 8.0f;
        float gapY = 6.0f;

        for (int i = 0; i < items.size(); i++) {
            ConfigItem item = items.get(i);
            int col = i % 2;
            int row = i / 2;

            float cx = startX + (float) col * (cardW + gapX);
            float cy = listY + scrollCurrent + (float) row * (cardH + gapY);

            if (cy + cardH < listY || cy > listY + listH) continue;

            float btnCardY = cy + 36.0f;
            float bH = 16.0f;
            float bW = 44.0f;

            // Save button
            float saveX = cx + 8.0f;
            if (mouseX >= saveX && mouseX <= saveX + bW && mouseY >= btnCardY && mouseY <= btnCardY + bH) {
                ConfigManager.saveProfile(item.name);
                activeLoadedConfig = item.name;
                return true;
            }

            // Load button
            float loadX = saveX + bW + 5.0f;
            if (mouseX >= loadX && mouseX <= loadX + bW && mouseY >= btnCardY && mouseY <= btnCardY + bH) {
                ConfigManager.loadProfile(item.name);
                activeLoadedConfig = item.name;
                return true;
            }

            // Delete button
            float delX = loadX + bW + 5.0f;
            if (mouseX >= delX && mouseX <= delX + bW && mouseY >= btnCardY && mouseY <= btnCardY + bH) {
                ConfigManager.deleteProfile(item.name);
                if (activeLoadedConfig.equalsIgnoreCase(item.name)) {
                    activeLoadedConfig = "";
                }
                return true;
            }
        }

        return false;
    }

    public boolean mouseScrolled(float mouseX, float mouseY, double amount, float x, float y) {
        if (modalOpen) return true;
        float startX = x + 114.0f;
        float startY = y + 40.0f;
        float width = 326.5f;
        float height = 224.5f;

        if (mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height) {
            scrollTarget += (float) (amount * 24.0);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!modalOpen) return false;

        if (keyCode == 256) { // ESC
            closeModal();
            return true;
        }
        if (keyCode == 259 && modalFocused && createBuffer.length() > 0) { // Backspace
            createBuffer.deleteCharAt(createBuffer.length() - 1);
            return true;
        }
        if (keyCode == 257 && modalFocused) { // Enter
            submitCreate();
            return true;
        }
        // Ctrl+V paste
        if (keyCode == 86 && (modifiers & 2) != 0 && modalFocused) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null) {
                for (char c : clip.toCharArray()) {
                    if (createBuffer.length() >= 24) break;
                    if (isValidChar(c)) {
                        createBuffer.append(c);
                    }
                }
            }
            return true;
        }
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!modalOpen || !modalFocused) return false;
        if (createBuffer.length() < 24 && isValidChar(chr)) {
            createBuffer.append(chr);
            return true;
        }
        return true;
    }

    private boolean isValidChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == ' '
            || (c >= 'а' && c <= 'я') || (c >= 'А' && c <= 'Я') || c == 'ё' || c == 'Ё';
    }

    private void submitCreate() {
        String name = createBuffer.toString().trim();
        if (!name.isEmpty()) {
            ConfigManager.saveProfile(name);
            activeLoadedConfig = name;
            closeModal();
        }
    }

    private void openFolder() {
        try {
            Path dir = ConfigManager.profilesDirectory();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir.toFile());
            } else {
                Runtime.getRuntime().exec(new String[]{"explorer", dir.toAbsolutePath().toString()});
            }
        } catch (Exception ignored) {
        }
    }

    private List<ConfigItem> loadConfigs() {
        List<String> names = ConfigManager.listProfiles();
        List<ConfigItem> items = new ArrayList<>();
        Path profilesDir = ConfigManager.profilesDirectory();

        for (String name : names) {
            long time = System.currentTimeMillis();
            try {
                Path path = profilesDir.resolve(name + ".heave");
                if (!Files.exists(path)) {
                    path = profilesDir.resolve(name + ".tria");
                }
                if (Files.exists(path)) {
                    time = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime().toMillis();
                }
            } catch (Exception ignored) {}
            items.add(new ConfigItem(name, time));
        }

        switch (sortMode) {
            case NEWEST -> items.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
            case OLDEST -> items.sort(Comparator.comparingLong(a -> a.lastModified));
            case BY_NAME -> items.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        }

        return items;
    }
}
