package rtx.heave.api.modules.impl.Visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.utils.animations.DecelerateValue;

public final class BetterHud extends Module {
    private static BetterHud INSTANCE;

    public final BooleanSetting animateHotbar = this.register(
        new BooleanSetting("Анимация хотбара", "Плавное перемещение рамки выбранного слота.", true)
    );
    public final BooleanSetting hotbarChatLift = this.register(
        new BooleanSetting("Хотбар при чате", "Плавный подъем хотбара при открытии чата.", true)
    );
    public final BooleanSetting animateChat = this.register(
        new BooleanSetting("Анимации чата", "Плавное появление сообщений и открытие поля ввода чата.", true)
    );
    public final BooleanSetting smoothTab = this.register(
        new BooleanSetting("Плавный таб", "Плавное появление и закрытие списка игроков.", true)
    );

    private static final DecelerateValue chatLift = new DecelerateValue(260);
    private static final DecelerateValue hotbarSelection = new DecelerateValue(180);
    private static long hotbarSelectionFrameNanos;
    private static float tabProgress = 0.0f;

    public BetterHud() {
        super("Better HUD", "Плавные визуальные анимации хотбара, чата и таба.", Category.DISPLAY);
        INSTANCE = this;
    }

    public static BetterHud getInstance() {
        return INSTANCE;
    }

    public static boolean animateHotbarEnabled() {
        BetterHud mod = ModuleManager.get().get(BetterHud.class);
        return mod != null && mod.isEnabled() && mod.animateHotbar.getValue();
    }

    public static boolean hotbarChatLiftEnabled() {
        BetterHud mod = ModuleManager.get().get(BetterHud.class);
        return mod != null && mod.isEnabled() && mod.hotbarChatLift.getValue();
    }

    public static boolean animateChatEnabled() {
        BetterHud mod = ModuleManager.get().get(BetterHud.class);
        return mod != null && mod.isEnabled() && mod.animateChat.getValue();
    }

    public static boolean smoothTabEnabled() {
        BetterHud mod = ModuleManager.get().get(BetterHud.class);
        return mod != null && mod.isEnabled() && mod.smoothTab.getValue();
    }

    public static int animateHotbarSelectionX(int targetX) {
        long now = System.nanoTime();
        long diff = now - hotbarSelectionFrameNanos;
        hotbarSelectionFrameNanos = now;
        if (!animateHotbarEnabled() || diff <= 0L || diff > 250000000L) {
            hotbarSelection.snap(targetX);
            return targetX;
        }
        return Math.round(hotbarSelection.update(targetX));
    }

    public static float chatHotbarLiftOffset() {
        boolean inChat = hotbarChatLiftEnabled() && MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
        return chatLift.update(inChat ? 14.0f : 0.0f);
    }

    // Compatibility delegates
    public static boolean chatAnimationsEnabled() {
        return animateChatEnabled();
    }

    public static boolean tabAnimationEnabled() {
        return smoothTabEnabled();
    }

    public static boolean inventoryAnimationEnabled() {
        return false;
    }

    public static boolean itemMoveAnimationEnabled() {
        return false;
    }

    public static float inventorySlideOffset() {
        return 0.0f;
    }

    public static void markInventoryOpen() {}

    public static boolean capeWavesEnabled() {
        return true;
    }

    public static float getTabProgress() {
        return tabProgress;
    }

    public static void setTabProgress(float val) {
        tabProgress = Math.max(0.0f, Math.min(1.0f, val));
    }
}
