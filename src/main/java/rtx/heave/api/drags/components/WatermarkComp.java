package rtx.heave.api.drags.components;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import rtx.heave.api.drags.Draggable;
import rtx.heave.api.drags.Position;
import rtx.heave.api.drags.hud.InfoHud;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.impl.Interface.WatermarkModule;
import rtx.heave.api.ui.settings.Setting;
import rtx.heave.api.ui.settings.SettingsFactory;
import rtx.heave.api.ui.theme.ClientAccent;
import rtx.heave.mixin.accessor.BossBarHudAccessor;
import rtx.heave.utils.animations.Easings;
import rtx.heave.utils.animations.SmoothAnimation;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.others.RectUtil;
import rtx.heave.utils.render.render2d.Render2D;

public final class WatermarkComp extends Draggable {
    private static final float PAD_X = 8.0f;
    private static final float PAD_Y = 6.0f;
    private static final float H = 20.0f;
    private static final String INFO_FONT = Fonts.SF.id();
    private static final String FALLBACK_NAME = "Player";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final SmoothAnimation visibility = new SmoothAnimation();

    private boolean lastTargetVisible;
    private float currentWidth = 80.0f;
    private float brandWidth = 55.0f;
    private final float starSize = 8.0f;
    private static final float STAR_TEXT_GAP = 5.0f;
    private String brandText = "heave.java";
    private float brandTextWidth;
    private String cachedName = "";
    private float nameWidth;
    private String timeText = "";
    private float timeWidth;

    public WatermarkComp() {
        super("watermark", 6.0f, 6.0f);
        this.visibility.set(0.0);
    }

    @Override
    public String displayName() {
        return "Watermark";
    }

    @Override
    public float width() {
        return this.currentWidth;
    }

    @Override
    public float height() {
        return H;
    }

    @Override
    public boolean isInteractive() {
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        return watermarkModule != null && watermarkModule.isEnabled();
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        if (watermarkModule != null) {
            for (rtx.heave.api.modules.settings.Setting s : watermarkModule.getSettings().all()) {
                Setting uiSetting = SettingsFactory.create(s);
                if (uiSetting != null) {
                    list.add(uiSetting);
                }
            }
        }
        return list;
    }

    private static int indexedPaletteColor(int[] nArray, float f) {
        int n = nArray.length;
        if (n <= 1) {
            return nArray[0];
        }
        float f2 = WatermarkComp.normalizedCycle(f) / 360.0f;
        float f3 = f2 < 0.5f ? f2 * 2.0f : (1.0f - f2) * 2.0f;
        float f4 = f3 * (float)(n - 1);
        int n2 = (int)f4;
        if (n2 > n - 2) {
            n2 = n - 2;
        }
        return ColorUtil.lerpColor(nArray[n2], nArray[n2 + 1], f4 - (float)n2);
    }

    private static int[] watermarkPalette(InterfaceModule interfaceModule) {
        int[] nArray = ClientAccent.currentPalette();
        if (nArray == null || nArray.length == 0) {
            int n = ColorUtil.lerpColor(-2234369, -1, 0.18f);
            return new int[]{n, ColorUtil.lerpColor(n, -1, 0.46f)};
        }
        int[] nArray2 = new int[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            nArray2[i] = ColorUtil.lerpColor(WatermarkComp.opaque(nArray[i]), -1, 0.18f);
        }
        return nArray2;
    }

    private static void drawIndexedGradientGlyph(String string, String string2, float f, float f2, float f3, int n, int[] nArray, float f4, float f5) {
        float f6 = Math.max(0.0f, Math.min(1.0f, f4));
        if (f6 <= 0.003921569f || string2 == null || string2.isEmpty()) {
            return;
        }
        int n2 = ColorUtil.multAlpha(WatermarkComp.indexedPaletteColor(nArray, f5 + (float)n * 15.0f), f6);
        Render2D.msdfText(string, string2, f, f2, f3, n2);
    }

    private static float indexedGradientPhase() {
        return (float)(System.currentTimeMillis() % 1200L) / 1200.0f * 360.0f;
    }

    private static float normalizedCycle(float f) {
        float f2 = f % 360.0f;
        return f2 < 0.0f ? f2 + 360.0f : f2;
    }

    private void updateTextCache(WatermarkModule watermarkModule) {
        String ext = (watermarkModule != null && watermarkModule.extension != null) ? watermarkModule.extension.getSelected() : "java";
        String currentBrand = "heave." + (ext != null ? ext : "java");
        if (!currentBrand.equals(this.brandText) || this.brandTextWidth <= 0.0f) {
            this.brandText = currentBrand;
            this.brandTextWidth = Render2D.msdfWidth(INFO_FONT, this.brandText, 8.5f);
            // brandWidth = text only; the star icon is drawn globally (not per-slot)
            this.brandWidth = this.brandTextWidth;
        }
        String string2 = WatermarkComp.playerName();
        if (!string2.equals(this.cachedName)) {
            this.cachedName = string2;
            this.nameWidth = Render2D.msdfWidth(INFO_FONT, string2, 8.0f);
        }
        String string = LocalTime.now().format(TIME_FORMAT);
        if (!string.equals(this.timeText)) {
            this.timeText = string;
            this.timeWidth = Render2D.msdfWidth(INFO_FONT, string, 8.0f);
        }
    }


    private static int gradientDotColor(int[] nArray, float f, int n, float f2) {
        int n2 = WatermarkComp.indexedPaletteColor(nArray, f + (float)n * 15.0f);
        return ColorUtil.multAlpha(n2, Math.max(0.0f, Math.min(1.0f, f2)));
    }

    private static void drawGradientString(String string, String string2, float f, float f2, float f3, int n, int[] nArray, float f4, float f5) {
        int n2;
        float f6 = Math.max(0.0f, Math.min(1.0f, f4));
        if (f6 <= 0.003921569f || string2 == null || string2.isEmpty()) {
            return;
        }
        float f7 = f;
        int n3 = n;
        for (int i = 0; i < string2.length(); i += n2) {
            int n4 = string2.codePointAt(i);
            n2 = Character.charCount(n4);
            String string3 = string2.substring(i, i + n2);
            WatermarkComp.drawIndexedGradientGlyph(string, string3, f7, f2, f3, n3, nArray, f6, f5);
            f7 += Render2D.msdfWidth(string, string3, f3);
            ++n3;
        }
    }

    private record SlotItem(String type, String text, float width) {}

    @Override
    protected void render(DrawContext drawContext) {
        boolean bl = this.shouldShow();
        if (bl != this.lastTargetVisible) {
            this.visibility.run(bl ? 1.0 : 0.0, bl ? 0.18 : 0.12, Easings.CUBIC_OUT, false);
            this.lastTargetVisible = bl;
        }
        this.visibility.update();
        float f = this.visibility.get();
        if (bl && f <= 0.01f) {
            f = 0.01f;
        }
        if (f <= 0.01f && !bl) {
            return;
        }

        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        if (watermarkModule == null) {
            return;
        }
        watermarkModule.updatePositions();
        this.updateTextCache(watermarkModule);

        MinecraftClient mc = MinecraftClient.getInstance();
        String fpsText = mc.getCurrentFps() + " fps";
        float fpsWidth = Render2D.msdfWidth(INFO_FONT, fpsText, 8.0f);
        int pingVal = (mc.player != null) ? InfoHud.ping(mc, mc.player) : 0;
        String pingText = pingVal + " ms";
        float pingWidth = Render2D.msdfWidth(INFO_FONT, pingText, 8.0f);

        String[] slotChoices = new String[]{
            watermarkModule.pos1.getSelected(),
            watermarkModule.pos2.getSelected(),
            watermarkModule.pos3.getSelected()
        };

        List<SlotItem> activeSlots = new ArrayList<>();
        for (String choice : slotChoices) {
            if (choice == null || choice.equals(WatermarkModule.ITEM_NONE)) {
                continue;
            }
            switch (choice) {
                case WatermarkModule.ITEM_LOGO -> activeSlots.add(new SlotItem(WatermarkModule.ITEM_LOGO, this.brandText, this.brandWidth));
                case WatermarkModule.ITEM_NICK -> activeSlots.add(new SlotItem(WatermarkModule.ITEM_NICK, this.cachedName, this.nameWidth));
                case WatermarkModule.ITEM_TIME -> activeSlots.add(new SlotItem(WatermarkModule.ITEM_TIME, this.timeText, this.timeWidth));
                case WatermarkModule.ITEM_FPS -> activeSlots.add(new SlotItem(WatermarkModule.ITEM_FPS, fpsText, fpsWidth));
                case WatermarkModule.ITEM_PING -> activeSlots.add(new SlotItem(WatermarkModule.ITEM_PING, pingText, pingWidth));
            }
        }

        if (activeSlots.isEmpty()) {
            this.currentWidth = 40.0f;
            return;
        }

        // The star icon is ALWAYS shown before the first slot (regardless of what the slot is)
        float iconPlusGap = this.starSize + STAR_TEXT_GAP;

        float totalContentWidth = iconPlusGap; // always start with the star icon
        for (int i = 0; i < activeSlots.size(); ++i) {
            if (i > 0) {
                totalContentWidth += 11.0f; // symmetrical dot gap (5px + 1px dot + 5px)
            }
            totalContentWidth += activeSlots.get(i).width;
        }

        this.currentWidth = totalContentWidth + PAD_X * 2.0f;

        int bossBars = 0;
        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            try {
                bossBars = mc.inGameHud.getBossBarHud().bossBars.size();
            } catch (Throwable t) {
                try {
                    bossBars = ((BossBarHudAccessor) mc.inGameHud.getBossBarHud()).getBossBars().size();
                } catch (Throwable ignored) {}
            }
        }

        // Position calculation
        String anchorMode = watermarkModule.anchor.getSelected();
        if (this.getDrag().isDragging()) {
            if (!WatermarkModule.ANCHOR_FREE.equals(anchorMode)) {
                watermarkModule.anchor.setSelected(WatermarkModule.ANCHOR_FREE);
            }
        } else if (!WatermarkModule.ANCHOR_FREE.equals(anchorMode)) {
            float screenW = Position.screenWidth();
            float targetX;
            if (WatermarkModule.ANCHOR_TOP_CENTER.equals(anchorMode)) {
                targetX = (screenW - this.currentWidth) / 2.0f;
            } else if (WatermarkModule.ANCHOR_TOP_RIGHT.equals(anchorMode)) {
                targetX = screenW - this.currentWidth - 6.0f;
            } else {
                targetX = 6.0f;
            }

            float targetY = (bossBars > 0) ? (14.0f + bossBars * 19.0f) : 6.0f;

            this.getDrag().setTargetX(targetX);
            this.getDrag().setTargetY(targetY);
        }

        float f5 = this.getX();
        float f6 = this.getY();
        if (WatermarkModule.ANCHOR_FREE.equals(anchorMode) && bossBars > 0 && !this.getDrag().isDragging()) {
            float bossThreshold = 14.0f + bossBars * 19.0f;
            if (f6 < bossThreshold) {
                f6 = bossThreshold;
            }
        }

        float f2 = 0.92f + f * 0.08f;
        float f3 = f5 + this.currentWidth * 0.5f;
        float f4 = f6 + 10.0f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f3, f4);
        drawContext.getMatrices().scale(f2);
        drawContext.getMatrices().translate(-f3, -f4);
        Render2D.beginFrame(drawContext);

        RectUtil.drawClientRect(f5, f6, this.currentWidth, 20.0f, 10.0f, f);

        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        int[] nArray = WatermarkComp.watermarkPalette(interfaceModule);
        float phase = WatermarkComp.indexedGradientPhase();

        float curX = (float) Math.round(f5 + PAD_X);
        float textY = (float) Math.round(f6 + 5.0f);
        float dotY = (float) Math.round(f6 + 10.0f);

        float iconY = (float) Math.round(f6 + (20.0f - this.starSize) / 2.0f);
        int starColor1 = WatermarkComp.gradientDotColor(nArray, phase, 0, f);
        int starColor2 = WatermarkComp.gradientDotColor(nArray, phase, 1, f);
        Render2D.star(curX, iconY, this.starSize, starColor1, starColor2, starColor2, starColor1);
        curX += iconPlusGap;

        int dotIndex = 0;
        for (int i = 0; i < activeSlots.size(); ++i) {
            SlotItem item = activeSlots.get(i);
            if (i > 0) {
                // centered dot separator with perfectly equal 5px spacing on both sides
                Render2D.circle(curX + 5.0f, dotY, 1.0f, WatermarkComp.gradientDotColor(nArray, phase, dotIndex++, f));
                curX += 11.0f;
            }
            if (item.type.equals(WatermarkModule.ITEM_LOGO)) {
                // LOGO: draw only text (icon is always drawn above already)
                WatermarkComp.drawGradientString(INFO_FONT, this.brandText, curX, textY, 8.5f, 1, nArray, f, phase);
            } else {
                WatermarkComp.drawGradientString(INFO_FONT, item.text, curX, textY, 8.0f, 1, nArray, f, phase);
            }
            curX += item.width;
        }

        Render2D.flush();
        drawContext.getMatrices().popMatrix();

    }


    private static String playerName() {
        String string;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.getSession() != null && minecraftClient.getSession().getUsername() != null && !minecraftClient.getSession().getUsername().isBlank()) {
            return minecraftClient.getSession().getUsername();
        }
        if (minecraftClient.player != null && (string = minecraftClient.player.getGameProfile().name()) != null && !string.isBlank()) {
            return string;
        }
        return FALLBACK_NAME;
    }

    private boolean shouldShow() {
        WatermarkModule watermarkModule = ModuleManager.get().get(WatermarkModule.class);
        return watermarkModule != null && watermarkModule.isEnabled();
    }

    private static int opaque(int n) {
        return n & 0xFFFFFF | 0xFF000000;
    }
}