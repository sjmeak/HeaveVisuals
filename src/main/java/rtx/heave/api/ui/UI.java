package rtx.heave.api.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.ui.minced.GuiConfigsPanel;
import rtx.heave.api.ui.minced.GuiEventsPanel;
import rtx.heave.api.ui.minced.GuiHeader;
import rtx.heave.api.ui.minced.GuiModuleList;
import rtx.heave.api.ui.minced.GuiSidebar;
import rtx.heave.api.ui.minced.GuiThemePanel;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.animations.AnimatedFloat;
import rtx.heave.utils.input.HeaveKeyBindings;
import rtx.heave.utils.render.post.guilayerblur.GuiCapture;
import rtx.heave.utils.render.render2d.Render2D;

public class UI extends BaseScreen implements GuiCapture.Source {
    public static final UI INSTANCE = new UI();
    public static final float PANEL_W = 448.5f;
    public static final float PANEL_H = 272.5f;

    private final GuiHeader header = new GuiHeader();
    private final GuiSidebar sidebar = new GuiSidebar(Category.VISUALS);
    private final GuiModuleList moduleList = new GuiModuleList();
    private final GuiThemePanel themePanel = new GuiThemePanel();
    private final GuiConfigsPanel configsPanel = new GuiConfigsPanel();
    private final GuiEventsPanel eventsPanel = new GuiEventsPanel();

    private boolean isThemeView = false;
    private final AnimatedFloat openAnim = new AnimatedFloat(0.0f, 14.0f);
    private boolean closing = false;
    private float posX = 0.0f;
    private float posY = 0.0f;

    private static Screen pendingAfterClose;
    private static boolean cardStratumMarked;
    private static boolean panelSplitMarked;
    private static boolean popupStratumMarked;
    private static boolean vanillaBlurRequested;
    private static boolean popupLayerBlurWanted;
    private static boolean motionBlurPending;
    private static float motionBlurRadius;
    private static float guiCaptureScale = 1.0f;

    private UI() {
        super(Text.literal("UI"));
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        openAnim.setValue(0.0f);
        openAnim.setTarget(1.0f);
        updatePosition();
    }

    private void updatePosition() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        posX = (float)(sw - PANEL_W) / 2.0f;
        posY = (float)(sh - PANEL_H) / 2.0f;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnim.setTarget(0.0f);
            header.clearSearch();
            configsPanel.closeModal();
            eventsPanel.reset();
        }
    }

    @Override
    protected void renderScreen(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updatePosition();

        if (closing && openAnim.getValue() <= 0.02f) {
            closing = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(pendingAfterClose);
            pendingAfterClose = null;
            return;
        }

        float alpha = MathHelper.clamp(openAnim.getValue(), 0.0f, 1.0f);
        float scale = 0.92f + 0.08f * MathHelper.clamp(openAnim.getValue(), 0.0f, 1.0f);

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // 1. Fullscreen background Kawase blur behind GUI
        if (alpha > 0.01f) {
            float blurRadius = 18.0f;
            try {
                InterfaceModule iface = InterfaceModule.getInstance();
                if (iface != null && iface.rectBackdropBlur != null) {
                    blurRadius = iface.rectBackdropBlur.getFloat();
                }
            } catch (Exception ignored) {}
            Render2D.blur(0.0f, 0.0f, (float)sw, (float)sh, 0.0f, blurRadius * alpha);
        }

        // 2. Dark backdrop overlay
        int backdropAlpha = (Math.max(0, Math.min(255, (int)(105.0f * alpha))) << 24);
        Render2D.rect(0, 0, sw, sh, 0.0f, backdropAlpha | 0x08080C);

        // 3. Centered window rendering with scale transform
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(posX + PANEL_W * 0.5f, posY + PANEL_H * 0.5f);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-(posX + PANEL_W * 0.5f), -(posY + PANEL_H * 0.5f));
        Render2D.beginFrame(context);


        // Window base
        InterfaceModule ifaceMod = InterfaceModule.getInstance();
        int windowBaseRgb = (ifaceMod != null && ifaceMod.isCustomTheme()) ? (ifaceMod.getCustomMainColor() & 0xFFFFFF) : 0x09090D;
        int bg = (Math.max(0, Math.min(255, (int)(248.0f * alpha))) << 24) | windowBaseRgb;
        Render2D.rect(posX, posY, PANEL_W, PANEL_H, 8.0f, bg);

        // Subtle theme accent border
        int border = ThemeManager.accent(35.0f * alpha);
        Render2D.outline(posX, posY, PANEL_W, PANEL_H, 8.0f, 1.0f, border);

        // Header (with logo and top search bar)
        header.render(context, posX, posY, alpha, alpha, isThemeView, sidebar.getCurrentCategory());

        // Sidebar (Visuals, Display, Utils on top; Events, Configs at bottom)
        sidebar.render(context, posX, posY, alpha, sidebar.getCurrentCategory());

        // Content Area
        Category currentCat = sidebar.getCurrentCategory();
        String search = header.getSearchQuery();

        if (isThemeView) {
            themePanel.render(context, posX, posY, alpha, (float) mouseX, (float) mouseY);
        } else if (currentCat == Category.CONFIGS && search.isEmpty()) {
            configsPanel.render(context, posX, posY, alpha, (float) mouseX, (float) mouseY);
        } else if (currentCat == Category.EVENTS && search.isEmpty()) {
            eventsPanel.render(context, posX, posY, alpha, (float) mouseX, (float) mouseY);
        } else {
            moduleList.render(context, posX, posY, alpha, currentCat, search);
        }

        // Modal overlay for Config creation
        if (currentCat == Category.CONFIGS && !isThemeView && configsPanel.isModalOpen()) {
            configsPanel.renderModal(context, posX, posY, alpha, (float) mouseX, (float) mouseY);
        }

        Render2D.flush();
        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mx = (float) click.x();
        float my = (float) click.y();
        int btn = click.button();

        Category currentCat = sidebar.getCurrentCategory();
        String search = header.getSearchQuery();

        // 0. Active bind intercept (mouse buttons including Mouse 4, Mouse 5, etc.)
        if (moduleList.isBinding()) {
            return moduleList.handleMouseBind(btn);
        }

        // 1. Modal clicks
        if (currentCat == Category.CONFIGS && !isThemeView && configsPanel.isModalOpen()) {
            return configsPanel.mouseClicked(mx, my, btn, posX, posY);
        }

        // 2. Header search bar
        if (header.mouseClicked(mx, my, btn, posX, posY)) {
            return true;
        }

        // 3. Sidebar category clicks
        if (btn == 0 && sidebar.mouseClicked(mx, my, btn, posX, posY)) {
            isThemeView = false;
            moduleList.resetScroll();
            eventsPanel.reset();
            return true;
        }

        // 4. Content panel clicks
        if (isThemeView) {
            return themePanel.mouseClicked(mx, my, posX, posY);
        } else if (currentCat == Category.CONFIGS && search.isEmpty()) {
            return configsPanel.mouseClicked(mx, my, btn, posX, posY);
        } else if (currentCat == Category.EVENTS && search.isEmpty()) {
            return eventsPanel.mouseClicked(mx, my, btn, posX, posY);
        } else {
            return moduleList.mouseClicked(mx, my, btn, posX, posY, currentCat, search);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Category currentCat = sidebar.getCurrentCategory();
        String search = header.getSearchQuery();
        if (currentCat == Category.EVENTS && search.isEmpty()) {
            return eventsPanel.mouseDragged((float) click.x(), (float) click.y(), posX, posY);
        }
        if (!isThemeView && (currentCat != Category.CONFIGS || !search.isEmpty())) {
            return moduleList.mouseDragged((float) click.x(), (float) click.y(), posX, posY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        eventsPanel.mouseReleased();
        moduleList.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Category currentCat = sidebar.getCurrentCategory();
        String search = header.getSearchQuery();

        if (currentCat == Category.CONFIGS && !isThemeView && configsPanel.isModalOpen()) {
            return true;
        }

        if (isThemeView) {
            return themePanel.mouseScrolled(verticalAmount);
        } else if (currentCat == Category.CONFIGS && search.isEmpty()) {
            return configsPanel.mouseScrolled((float) mouseX, (float) mouseY, verticalAmount, posX, posY);
        } else if (currentCat == Category.EVENTS && search.isEmpty()) {
            return eventsPanel.mouseScrolled(verticalAmount);
        } else {
            return moduleList.mouseScrolled(verticalAmount);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        Category currentCat = sidebar.getCurrentCategory();
        String search = header.getSearchQuery();

        if (currentCat == Category.CONFIGS && !isThemeView && configsPanel.isModalOpen()) {
            return configsPanel.keyPressed(keyCode, input.scancode(), input.modifiers());
        }

        if (header.isSearchFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                header.setSearchFocused(false);
                return true;
            }
            return header.keyPressed(keyCode);
        }

        if (!isThemeView && (currentCat != Category.CONFIGS || !search.isEmpty())) {
            if (moduleList.keyPressed(keyCode)) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || (HeaveKeyBindings.OPEN_MENU != null && HeaveKeyBindings.OPEN_MENU.matchesKey(input))) {
            if (isThemeView) {
                isThemeView = false;
                return true;
            }
            close();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        char c = (char) input.codepoint();

        Category currentCat = sidebar.getCurrentCategory();
        String search = header.getSearchQuery();

        if (currentCat == Category.CONFIGS && !isThemeView && configsPanel.isModalOpen()) {
            return configsPanel.charTyped(c, input.modifiers());
        }

        if (header.isSearchFocused()) {
            return header.charTyped(c);
        }

        if (!isThemeView && (currentCat != Category.CONFIGS || !search.isEmpty())) {
            return moduleList.charTyped(c);
        }

        return false;
    }

    // =========================================================================
    // Mixin & Compatibility bridges
    // =========================================================================

    public static boolean isOpen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.currentScreen == INSTANCE;
    }

    public static boolean isSearchTyping() {
        return isOpen() && (INSTANCE.header.isSearchFocused() || INSTANCE.configsPanel.isModalOpen() || INSTANCE.moduleList.isEditingText());
    }

    public static void closeInto(Screen screen) {
        pendingAfterClose = screen;
        INSTANCE.close();
        if (MinecraftClient.getInstance().currentScreen == INSTANCE) {
            MinecraftClient.getInstance().setScreen(screen);
        }
    }

    public void warmupRender() {
        moduleList.warmup();
    }

    public static boolean isSettingsPopupVisible() {
        return false;
    }

    public static boolean isSettingsOpenFor(String name) {
        return isOpen();
    }

    public static boolean guiCaptureActive() {
        return isOpen() && INSTANCE.openAnim.getValue() > 0.01f;
    }

    public static float guiCaptureScale() {
        return guiCaptureScale;
    }

    public static boolean motionBlurCapturePending() {
        return motionBlurPending;
    }

    public static float motionBlurCaptureRadius() {
        return motionBlurRadius;
    }

    public static void flushMotionBlur() {
        motionBlurPending = false;
    }

    public static void dropPendingBlurs() {
        cardStratumMarked = false;
        panelSplitMarked = false;
        vanillaBlurRequested = false;
        popupStratumMarked = false;
        popupLayerBlurWanted = false;
        motionBlurPending = false;
    }

    public static boolean consumeCardStratumMark() {
        boolean m = cardStratumMarked;
        cardStratumMarked = false;
        return m;
    }

    public static boolean consumePanelSplitMark() {
        boolean m = panelSplitMarked;
        panelSplitMarked = false;
        return m;
    }

    public static boolean consumePopupStratumMark() {
        boolean m = popupStratumMarked;
        popupStratumMarked = false;
        return m;
    }

    public static boolean consumePopupBlurCapture() {
        return false;
    }

    public static boolean consumePopupLayerCapture() {
        boolean m = popupLayerBlurWanted;
        popupLayerBlurWanted = false;
        return m;
    }

    public static boolean consumeVanillaBlurRequest() {
        boolean m = vanillaBlurRequested;
        vanillaBlurRequested = false;
        return m;
    }

    public static void requestVanillaBlurAtSplit() {
        vanillaBlurRequested = true;
    }

    public static void applyMainCompositeAtSplit() {
    }

    public static void markCardStratum() {
        cardStratumMarked = true;
    }

    public static void markPopupStratum(boolean b1, boolean b2) {
        popupStratumMarked = true;
    }

    public static void markPopupLayerCapture() {
        popupLayerBlurWanted = true;
    }

    public static float guiShatterProgress() {
        return 0.0f;
    }

    @Override
    public float shatterProgress() {
        return 0.0f;
    }

    @Override
    public float captureScale() {
        return guiCaptureScale;
    }

    @Override
    public boolean captureActive() {
        return guiCaptureActive();
    }

    @Override
    public float captureBlurRadius() {
        return 14.0f;
    }

    public static boolean popupLayerCapturePending() {
        return popupLayerBlurWanted;
    }

    public static void renderClosingPanelOverHud(DrawContext drawContext) {
        if (INSTANCE.closing && INSTANCE.openAnim.getValue() > 0.01f) {
            Render2D.beginFrame(drawContext);
            INSTANCE.renderScreen(drawContext, 0, 0, 0.0f);
            Render2D.flush();
        }
    }
}
