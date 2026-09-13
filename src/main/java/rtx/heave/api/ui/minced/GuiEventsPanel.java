package rtx.heave.api.ui.minced;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.holyworld.HolyWorldApi;
import rtx.heave.api.holyworld.HolyWorldEvent;
import rtx.heave.api.modules.impl.Utils.EventMarkers;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.animations.AnimatedFloat;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public class GuiEventsPanel {
    public enum EventFilter {
        ALL("Все", null),
        CARGO("Груз", "CARGO"),
        GOLD_RUSH("Лихорадка", "GOLD_RUSH"),
        QUARRY("Шахты", "QUARRY"),
        CUBE("Кубик", "CUBE"),
        OTHER("Прочие", "OTHER");

        private final String title;
        private final String typeKey;

        EventFilter(String title, String typeKey) {
            this.title = title;
            this.typeKey = typeKey;
        }

        public String getTitle() {
            return title;
        }

        public String getTypeKey() {
            return typeKey;
        }
    }

    private static class CardAnimState {
        final AnimatedFloat hover = new AnimatedFloat(0.0f, 12.0f);
    }

    private EventFilter activeFilter = EventFilter.ALL;
    private final Map<EventFilter, AnimatedFloat> filterHovers = new EnumMap<>(EventFilter.class);
    private final Map<String, CardAnimState> cardStates = new HashMap<>();

    private float scrollTarget = 0.0f;
    private float scrollCurrent = 0.0f;
    private float contentHeight = 0.0f;

    public GuiEventsPanel() {
        for (EventFilter f : EventFilter.values()) {
            filterHovers.put(f, new AnimatedFloat(0.0f, 12.0f));
        }
    }

    public void reset() {
        this.scrollTarget = 0.0f;
        this.scrollCurrent = 0.0f;
    }

    public void render(DrawContext context, float x, float y, float alpha, float mouseX, float mouseY) {
        float startX = x + 114.0f;
        float startY = y + 40.0f;
        float width = 326.5f;
        float height = 224.5f;

        // 1. Top bar: status pill + horizontal filter buttons
        renderTopBar(context, startX, startY, width, alpha, mouseX, mouseY);

        // 2. Filter tabs row
        float tabsY = startY + 18.0f;
        renderFilterTabs(context, startX, tabsY, width, alpha, mouseX, mouseY);

        float listY = tabsY + 18.0f;
        float listH = height - (listY - startY);

        Render2D.pushScissor(context, startX, listY, width, listH);

        // Smooth scroll clamp & lerp
        float maxScroll = Math.max(0.0f, contentHeight - listH + 8.0f);
        scrollTarget = MathHelper.clamp(scrollTarget, -maxScroll, 0.0f);
        scrollCurrent = MathHelper.lerp(0.18f, scrollCurrent, scrollTarget);

        HolyWorldApi api = HolyWorldApi.getInstance();
        List<HolyWorldEvent> events = api.getTrackedEvents(activeFilter.getTypeKey(), null);

        if (events.isEmpty()) {
            String empty = "Нет событий по выбранному фильтру";
            float tw = Fonts.SF.width(empty, 9.5f);
            int textColor = (Math.max(0, Math.min(255, (int)(110.0f * alpha))) << 24) | 0xFFFFFF;
            Fonts.SF.draw(context, empty, startX + (width - tw) * 0.5f, listY + listH * 0.4f, 9.5f, textColor);
            contentHeight = 0.0f;
        } else {
            float cardW = 158.0f;
            float cardH = 50.0f;
            float gapX = 8.0f;
            float gapY = 6.0f;

            int rows = 0;
            for (int i = 0; i < events.size(); i++) {
                HolyWorldEvent event = events.get(i);
                int col = i % 2;
                int row = i / 2;
                rows = row + 1;

                float cx = startX + (float) col * (cardW + gapX);
                float cy = listY + scrollCurrent + (float) row * (cardH + gapY);

                if (cy + cardH >= listY - 10.0f && cy <= listY + listH + 10.0f) {
                    renderEventCard(context, event, cx, cy, cardW, cardH, alpha, mouseX, mouseY);
                }
            }
            contentHeight = (float) rows * (cardH + gapY);
        }

        Render2D.popScissor(context);
    }

    private void renderTopBar(DrawContext context, float x, float y, float width, float alpha, float mouseX, float mouseY) {
        HolyWorldApi api = HolyWorldApi.getInstance();

        // 1. Title
        int white = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF_BOLD.draw(context, "События HolyWorld", x, y + 2.0f, 10.5f, white);

        // 2. Server count status pill (green dot)
        float dotX = x + Fonts.SF_BOLD.width("События HolyWorld", 10.5f) + 8.0f;
        int dotColor = (Math.max(0, Math.min(255, (int)(220.0f * alpha))) << 24) | 0x22C55E;
        Render2D.rect(dotX, y + 4.5f, 5.0f, 5.0f, 2.5f, dotColor);

        int serversCount = api.getTotalServersCount();
        String countStr = serversCount > 0 ? (serversCount + " серверов") : "Онлайн";
        int subText = (Math.max(0, Math.min(255, (int)(130.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(context, countStr, dotX + 8.0f, y + 2.5f, 8.5f, subText);

        // 3. Right: Coins trade exchange rate badge
        HolyWorldApi.CoinsTradeInfo coins = api.getCoinsTrade();
        if (coins.buyPrice() > 0.0) {
            String coinStr = String.format(Locale.ROOT, "1₽ = %.0f", coins.buyPrice());
            float coinW = Fonts.SF_BOLD.width(coinStr, 8.5f) + 16.0f;
            float coinX = x + width - coinW;
            int badgeBg = (Math.max(0, Math.min(255, (int)(20.0f * alpha))) << 24) | 0xF59E0B;
            Render2D.rect(coinX, y, coinW, 13.0f, 3.0f, badgeBg);
            int goldColor = (Math.max(0, Math.min(255, (int)(230.0f * alpha))) << 24) | 0xFBBF24;
            Fonts.SF_BOLD.draw(context, "🪙 " + coinStr, coinX + 4.0f, y + 2.5f, 8.5f, goldColor);
        }
    }

    private void renderFilterTabs(DrawContext context, float x, float y, float width, float alpha, float mouseX, float mouseY) {
        float curX = x;
        float tabH = 14.0f;

        for (EventFilter f : EventFilter.values()) {
            boolean active = (f == activeFilter);
            AnimatedFloat hover = filterHovers.computeIfAbsent(f, k -> new AnimatedFloat(0.0f, 12.0f));

            float tabW = Fonts.SF_BOLD.width(f.getTitle(), 8.0f) + 12.0f;
            boolean over = mouseX >= curX && mouseX <= curX + tabW && mouseY >= y && mouseY <= y + tabH;
            hover.setTarget(over ? 1.0f : 0.0f);

            int bg;
            int textCol;

            if (active) {
                bg = ThemeManager.accent((160.0f + 50.0f * hover.getValue()) * alpha);
                textCol = (Math.max(0, Math.min(255, (int)(255.0f * alpha))) << 24) | 0xFFFFFF;
            } else {
                int bgAlpha = (int)((15.0f + 25.0f * hover.getValue()) * alpha);
                bg = (Math.max(0, Math.min(255, bgAlpha)) << 24) | 0xFFFFFF;
                int textAlpha = (int)((130.0f + 100.0f * hover.getValue()) * alpha);
                textCol = (Math.max(0, Math.min(255, textAlpha)) << 24) | 0xFFFFFF;
            }

            Render2D.rect(curX, y, tabW, tabH, 3.0f, bg);
            Fonts.SF_BOLD.draw(context, f.getTitle(), curX + 6.0f, y + 3.0f, 8.0f, textCol);

            curX += tabW + 4.0f;
        }
    }

    private void renderEventCard(DrawContext context, HolyWorldEvent event, float x, float y, float w, float h,
                                 float alpha, float mouseX, float mouseY) {
        String cardKey = event.getServerId() + "_" + event.getType().name();
        CardAnimState state = cardStates.computeIfAbsent(cardKey, k -> new CardAnimState());

        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        state.hover.setTarget(hovered ? 1.0f : 0.0f);
        float hProgress = state.hover.getValue();

        // 1. Card Base
        int baseAlpha = (int)((20.0f + 16.0f * hProgress) * alpha);
        int cardBg = (Math.max(0, Math.min(255, baseAlpha)) << 24) | 0xFFFFFF;
        Render2D.rect(x, y, w, h, 5.0f, cardBg);

        // 2. Outline (accented when hovered, subtle otherwise)
        int outlineCol;
        if (event.isLive()) {
            outlineCol = ColorUtil.multAlpha(event.getColor(), (0.45f + 0.35f * hProgress) * alpha);
        } else {
            outlineCol = ThemeManager.accent((15.0f + 40.0f * hProgress) * alpha);
        }
        Render2D.outline(x, y, w, h, 5.0f, 1.0f, outlineCol);

        // 3. Left Icon Badge (with colored theme background)
        float iconBoxSize = 34.0f;
        float iconBoxX = x + 6.0f;
        float iconBoxY = y + (h - iconBoxSize) * 0.5f;

        int iconBoxBg = ColorUtil.multAlpha(event.getColor(), 0.18f * alpha);
        int iconBoxBorder = ColorUtil.multAlpha(event.getColor(), 0.50f * alpha);
        Render2D.rect(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0f, iconBoxBg);
        Render2D.outline(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0f, 0.75f, iconBoxBorder);

        // Draw MSDF Glyph from Fonts.EVENT_ICONS
        int iconColor = ColorUtil.multAlpha(event.getColor(), 0.95f * alpha);
        float[] bounds = Fonts.EVENT_ICONS.msdfBounds(event.getIconChar(), 14.0f);
        float glyphW = bounds != null ? (bounds[2] - bounds[0]) : 12.0f;
        float glyphH = bounds != null ? (bounds[3] - bounds[1]) : 12.0f;
        float glyphX = iconBoxX + (iconBoxSize - glyphW) * 0.5f;
        float glyphY = iconBoxY + (iconBoxSize - glyphH) * 0.5f;
        Fonts.EVENT_ICONS.msdf(event.getIconChar(), glyphX, glyphY, 14.0f, iconColor);

        // 4. Event Title
        float textX = iconBoxX + iconBoxSize + 7.0f;
        int titleCol = (Math.max(0, Math.min(255, (int)(250.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF_BOLD.draw(context, event.getName(), textX, y + 8.0f, 9.5f, titleCol);

        // 5. Server pill / Anarchy number
        float pillY = y + 22.0f;
        String sName = event.getServerName();
        float pillW = Fonts.SF.width(sName, 7.5f) + 8.0f;
        int pillBg = (Math.max(0, Math.min(255, (int)(22.0f * alpha))) << 24) | 0xFFFFFF;
        Render2D.rect(textX, pillY, pillW, 11.0f, 2.5f, pillBg);
        int sCol = (Math.max(0, Math.min(255, (int)(180.0f * alpha))) << 24) | 0xFFFFFF;
        Fonts.SF.draw(context, sName, textX + 4.0f, pillY + 1.5f, 7.5f, sCol);

        // Quick join command hint
        String cmdHint = "/an " + event.getAnarchyNumber();
        int cmdCol = ThemeManager.accent(170.0f * alpha);
        Fonts.SF.draw(context, cmdHint, textX + pillW + 5.0f, pillY + 1.5f, 7.5f, cmdCol);

        // Coordinates tag if present
        if (event.getCoords() != null) {
            Vec3d c = event.getCoords();
            String cStr = String.format(Locale.ROOT, "XYZ: %d %d", (int) c.x, (int) c.z);
            float cW = Fonts.SF.width(cStr, 6.5f) + 6.0f;
            float cX = textX + pillW + Fonts.SF.width(cmdHint, 7.5f) + 9.0f;
            int cBg = (Math.max(0, Math.min(255, (int)(20.0f * alpha))) << 24) | 0x06B6D4;
            Render2D.rect(cX, pillY, cW, 11.0f, 2.5f, cBg);
            int cText = (Math.max(0, Math.min(255, (int)(220.0f * alpha))) << 24) | 0x22D3EE;
            Fonts.SF.draw(context, cStr, cX + 3.0f, pillY + 2.0f, 6.5f, cText);
        }

        // 6. Right Side: Status / Live Countdown Timer
        if (event.isLive()) {
            // Pulsing live indicator
            long pulseTime = System.currentTimeMillis();
            float pulse = 0.65f + 0.35f * (float) Math.sin((double) pulseTime / 200.0);
            int liveCol = ColorUtil.multAlpha(0xFF22C55E, pulse * alpha);

            String liveText = "ИДЁТ СЕЙЧАС!";
            float liveW = Fonts.SF_BOLD.width(liveText, 8.0f);
            float liveX = x + w - liveW - 8.0f;
            Fonts.SF_BOLD.draw(context, liveText, liveX, y + 8.0f, 8.0f, liveCol);

            // Remaining active duration
            String rem = event.formatRemaining();
            float remW = Fonts.SF.width(rem, 7.5f);
            float remX = x + w - remW - 8.0f;
            int remCol = (Math.max(0, Math.min(255, (int)(140.0f * alpha))) << 24) | 0xFFFFFF;
            Fonts.SF.draw(context, rem, remX, y + 20.0f, 7.5f, remCol);
        } else {
            // Upcoming countdown
            String timer = event.formatRemaining();
            float timerW = Fonts.SF_BOLD.width(timer, 10.0f);
            float timerX = x + w - timerW - 8.0f;
            int timerCol = (Math.max(0, Math.min(255, (int)(240.0f * alpha))) << 24) | 0xFFFFFF;
            Fonts.SF_BOLD.draw(context, timer, timerX, y + 7.5f, 10.0f, timerCol);

            String sub = "до начала";
            float subW = Fonts.SF.width(sub, 7.0f);
            float subX = x + w - subW - 8.0f;
            int subCol = (Math.max(0, Math.min(255, (int)(110.0f * alpha))) << 24) | 0xFFFFFF;
            Fonts.SF.draw(context, sub, subX, y + 21.0f, 7.0f, subCol);
        }

        // Hover click hint
        if (hProgress > 0.05f) {
            String clickHint = event.getCoords() != null
                ? "ЛКМ: /an " + event.getAnarchyNumber() + " | ПКМ: метка GPS"
                : "Клик: перейти на /an " + event.getAnarchyNumber();
            int hintCol = ThemeManager.accent(210.0f * hProgress * alpha);
            Fonts.SF.draw(context, clickHint, textX, y + 36.0f, 7.0f, hintCol);
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int btn, float posX, float posY) {
        float startX = posX + 114.0f;
        float startY = posY + 40.0f;
        float width = 326.5f;
        float height = 224.5f;

        // 1. Filter tabs clicks
        float tabsY = startY + 18.0f;
        float tabH = 14.0f;
        float curX = startX;
        for (EventFilter f : EventFilter.values()) {
            float tabW = Fonts.SF_BOLD.width(f.getTitle(), 8.0f) + 12.0f;
            if (mouseX >= curX && mouseX <= curX + tabW && mouseY >= tabsY && mouseY <= tabsY + tabH) {
                if (btn == 0) {
                    this.activeFilter = f;
                    this.scrollTarget = 0.0f;
                    this.scrollCurrent = 0.0f;
                    return true;
                }
            }
            curX += tabW + 4.0f;
        }

        // 2. Event card clicks
        float listY = tabsY + 18.0f;
        float listH = height - (listY - startY);

        if (mouseX >= startX && mouseX <= startX + width && mouseY >= listY && mouseY <= listY + listH) {
            HolyWorldApi api = HolyWorldApi.getInstance();
            List<HolyWorldEvent> events = api.getTrackedEvents(activeFilter.getTypeKey(), null);

            float cardW = 158.0f;
            float cardH = 50.0f;
            float gapX = 8.0f;
            float gapY = 6.0f;

            for (int i = 0; i < events.size(); i++) {
                HolyWorldEvent event = events.get(i);
                int col = i % 2;
                int row = i / 2;
                float cx = startX + (float) col * (cardW + gapX);
                float cy = listY + scrollCurrent + (float) row * (cardH + gapY);

                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                    if (btn == 0) {
                        HolyWorldApi.joinAnarchy(event.getAnarchyNumber());
                        return true;
                    }
                    if (btn == 1 && event.getCoords() != null) {
                        EventMarkers.setGps(event.getName(), event.getCoords(), new java.awt.Color(event.getColor()), 15 * 60 * 1000L, true);
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null) {
                            mc.player.sendMessage(net.minecraft.text.Text.literal("§6[GPS] §fМетка установлена на §e" + event.getName() + " §b" + (int)event.getCoords().x + " " + (int)event.getCoords().z), false);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean mouseScrolled(double verticalAmount) {
        scrollTarget += (float) verticalAmount * 28.0f;
        return true;
    }

    public boolean mouseDragged(float mouseX, float mouseY, float posX, float posY) {
        return false;
    }

    public void mouseReleased() {
    }
}
