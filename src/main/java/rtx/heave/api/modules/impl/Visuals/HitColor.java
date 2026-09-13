package rtx.heave.api.modules.impl.Visuals;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ColorSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.mixin.accessor.OverlayTextureAccessor;

public final class HitColor extends Module {
    private static HitColor instance;
    private static final int VANILLA_RED = -1291911168; // 0xB3FF0000

    private final BooleanSetting affectArmor = this.register(
        new BooleanSetting("На броню", "Окрашивать броню при получении урона.", true)
    );
    private final BooleanSetting mobs = this.register(
        new BooleanSetting("На мобов", "Окрашивать мобов при получении урона.", true)
    );
    private final ColorSetting color = this.register(
        new ColorSetting("Цвет", "Цвет при получении урона.", new Color(255, 110, 110, 200))
    );

    private int lastAppliedColor = 0;
    private long lastUpdateTime = 0L;

    public HitColor() {
        super("Hit Color", "Меняет цвет при получении урона.", Category.VISUALS);
        instance = this;
        this.color.setChangeListener(this::updateOverlay);
    }

    public static HitColor getInstance() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    public static int color() {
        if (instance == null) return -1;
        return instance.color.getColor();
    }

    public static boolean affectArmor() {
        return instance != null && instance.isEnabled() && instance.affectArmor.getValue();
    }

    public static boolean affectsMobs() {
        return instance != null && instance.isEnabled() && instance.mobs.getValue();
    }

    public static boolean shouldTint(LivingEntity livingEntity) {
        if (!HitColor.isActive() || livingEntity == null) {
            return false;
        }
        if (livingEntity.hurtTime <= 0 && livingEntity.deathTime <= 0) {
            return false;
        }
        if (livingEntity instanceof PlayerEntity) {
            return true;
        }
        return instance.mobs.getValue();
    }

    @Override
    protected void onEnable() {
        this.lastAppliedColor = 0;
        this.updateOverlay();
    }

    @Override
    protected void onDisable() {
        this.restoreVanillaOverlay();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!event.isPre() || !this.isEnabled()) return;
        if (this.lastAppliedColor != color()) {
            this.updateOverlay();
        }
    }

    public void updateOverlay() {
        if (!this.isEnabled()) {
            this.restoreVanillaOverlay();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null) return;

        OverlayTexture overlayTexture = client.gameRenderer.getOverlayTexture();
        if (overlayTexture == null) return;

        int currentColor = color();
        long now = System.currentTimeMillis();

        if (currentColor == this.lastAppliedColor && now - this.lastUpdateTime < 1000L) {
            return;
        }
        this.lastAppliedColor = currentColor;
        this.lastUpdateTime = now;

        try {
            NativeImageBackedTexture texture = ((OverlayTextureAccessor) overlayTexture).heave$getTexture();
            if (texture != null) {
                NativeImage image = texture.getImage();
                if (image != null) {
                    int userA = (currentColor >> 24) & 0xFF;
                    if (userA == 0) userA = 255;
                    float intensity = userA / 255.0f;
                    int shaderA = Math.max(0, Math.min(255, (int) ((1.0f - intensity) * 255.0f)));
                    int finalPixel = (shaderA << 24) | (currentColor & 0x00FFFFFF);

                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 16; x++) {
                            image.setColorArgb(x, y, finalPixel);
                        }
                    }
                    texture.upload();
                }
            }
        } catch (Throwable ignored) {}
    }

    public void restoreVanillaOverlay() {
        this.lastAppliedColor = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null) return;

        OverlayTexture overlayTexture = client.gameRenderer.getOverlayTexture();
        if (overlayTexture == null) return;

        try {
            NativeImageBackedTexture texture = ((OverlayTextureAccessor) overlayTexture).heave$getTexture();
            if (texture != null) {
                NativeImage image = texture.getImage();
                if (image != null) {
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 16; x++) {
                            image.setColorArgb(x, y, VANILLA_RED);
                        }
                    }
                    texture.upload();
                }
            }
        } catch (Throwable ignored) {}
    }
}
