package ru.customgamegui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerConstants;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Visuals.SaturationModule;
import ru.customgamegui.config.CGGConfig;
import ru.customgamegui.config.CGGConfigManager;

public final class SaturationBarRenderer {
    private static final Identifier FOOD_EMPTY = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");

    private static final int FULL_COLOR = 0xFFFFFFFF;    // Vanilla Sprite Color
    private static final int PREVIEW_COLOR = 0x80FFFFFF; // Semi-transparent Vanilla

    private SaturationBarRenderer() {}

    public static void render(DrawContext context, PlayerEntity player) {
        SaturationModule satMod = ModuleManager.get().get(SaturationModule.class);
        if (satMod == null || !satMod.isEnabled()) {
            return;
        }

        CGGConfig config = CGGConfigManager.getConfig();
        if (config.hideHungerBar) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (player == null || client.options.hudHidden || player.isCreative() || player.isSpectator()) {
            return;
        }

        HungerManager hungerManager = player.getHungerManager();
        if (hungerManager == null) {
            return;
        }

        int foodLevel = hungerManager.getFoodLevel();
        float saturationLevel = hungerManager.getSaturationLevel();

        int scaledWidth = context.getScaledWindowWidth();
        int scaledHeight = context.getScaledWindowHeight();
        int right = scaledWidth / 2 + 91;

        // Base Y coordinates (inside renderStatusBars matrix)
        int hungerY = scaledHeight - 39;
        int satY = scaledHeight - 49;

        // If airOnTop is false, shift saturation bar up when air bubbles are displayed
        boolean showingAir = player.isSubmergedIn(FluidTags.WATER) || player.getAir() < player.getMaxAir();
        if (showingAir && !satMod.isAirOnTop() && !config.hideAirBar) {
            satY -= 10;
        }

        int foodBonus = 0;
        float saturationBonus = 0.0f;

        if (satMod.isPreviewFood() && config.saturationPreviewFood) {
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            ItemStack foodStack = null;

            if (mainHand != null && mainHand.contains(DataComponentTypes.FOOD)) {
                foodStack = mainHand;
            } else if (offHand != null && offHand.contains(DataComponentTypes.FOOD)) {
                foodStack = offHand;
            }

            if (foodStack != null) {
                FoodComponent foodComponent = foodStack.get(DataComponentTypes.FOOD);
                if (foodComponent != null) {
                    foodBonus = foodComponent.nutrition();
                    saturationBonus = HungerConstants.calculateSaturation(foodComponent.nutrition(), foodComponent.saturation());
                }
            }
        }

        int targetFood = Math.min(20, foodLevel + foodBonus);
        float targetSaturation = Math.min((float) targetFood, saturationLevel + saturationBonus);

        // --- PASS 1: Render current saturation bar with outline ---
        for (int i = 0; i < 10; i++) {
            int iconX = right - i * 8 - 9;
            float satRemaining = saturationLevel - (float) (i * 2);

            if (satRemaining >= 2.0f) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, iconX, satY, 9, 9);
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_FULL, iconX, satY, 9, 9, FULL_COLOR);
            } else if (satRemaining > 0.0f) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, iconX, satY, 9, 9);
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_HALF, iconX, satY, 9, 9, FULL_COLOR);
            }
        }

        // --- PASS 2 & 3: Food preview (flashing hunger & preview saturation) ---
        if (satMod.isPreviewFood() && config.saturationPreviewFood && (foodBonus > 0 || saturationBonus > 0.0f)) {
            // Pass 2: Flashing food preview on hunger bar
            long time = Util.getMeasuringTimeMs();
            if (time % 1000L < 500L) {
                for (int i = 0; i < 10; i++) {
                    int iconX = right - i * 8 - 9;
                    int foodIndex = i * 2;

                    if (foodLevel < foodIndex + 2 && targetFood > foodIndex) {
                        if (targetFood - foodIndex >= 2) {
                            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_FULL, iconX, hungerY, 9, 9);
                        } else {
                            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_HALF, iconX, hungerY, 9, 9);
                        }
                    }
                }
            }

            // Pass 3: Preview saturation bonus on saturation bar
            for (int i = 0; i < 10; i++) {
                int iconX = right - i * 8 - 9;
                float satIndex = (float) (i * 2);

                if (saturationLevel < satIndex + 2.0f && targetSaturation > satIndex) {
                    float bonusRemaining = targetSaturation - satIndex;
                    if (bonusRemaining >= 2.0f) {
                        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, iconX, satY, 9, 9);
                        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_FULL, iconX, satY, 9, 9, PREVIEW_COLOR);
                    } else {
                        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, iconX, satY, 9, 9);
                        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_HALF, iconX, satY, 9, 9, PREVIEW_COLOR);
                    }
                }
            }
        }
    }
}
