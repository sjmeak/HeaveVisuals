package ru.customgamegui.config;

public class CGGConfig {
    // Master Switch
    public boolean enabled = true;

    // Experience & Hotbar
    public boolean hideExperienceBar = false;
    public boolean hideExperienceLevel = false;
    public boolean shiftDownWithoutExpBar = true;
    public boolean shiftExpLevelWhenBarHidden = true;
    public int statusBarOffset = 6; // px to shift down (vanilla gap is 6-7px)

    // Hotbar Transparency & Toggles
    public boolean hideHotbar = false;
    public boolean hideHeldItemName = false;
    public int hotbarBackgroundOpacity = 100; // 0 - 100%
    public int hotbarSelectionOpacity = 100;  // 0 - 100%

    // Status Bars
    public boolean hideHealthBar = false;
    public boolean hideHungerBar = false;
    public boolean hideArmorBar = false;
    public boolean hideAirBar = false;

    // Saturation Bar
    public boolean showSaturationBar = true;
    public boolean saturationPreviewFood = true;

    // AppleSkin Integration
    public boolean appleSkinIntegration = true;
    public boolean appleSkinHideDefaultSaturation = false;

    // Overlays & UI Elements
    public boolean hideCrosshair = false;
    public boolean hideScoreboard = false;
    public boolean hideBossBar = false;
    public boolean hideStatusEffects = false;
    public boolean hideSubtitles = false;
    public boolean hideVignette = false;
    public boolean hideChat = false;

    public void resetToDefaults() {
        this.enabled = true;
        this.hideExperienceBar = false;
        this.hideExperienceLevel = false;
        this.shiftDownWithoutExpBar = true;
        this.shiftExpLevelWhenBarHidden = true;
        this.statusBarOffset = 6;
        this.hideHotbar = false;
        this.hideHeldItemName = false;
        this.hotbarBackgroundOpacity = 100;
        this.hotbarSelectionOpacity = 100;
        this.hideHealthBar = false;
        this.hideHungerBar = false;
        this.hideArmorBar = false;
        this.hideAirBar = false;
        this.showSaturationBar = true;
        this.saturationPreviewFood = true;
        this.appleSkinIntegration = true;
        this.appleSkinHideDefaultSaturation = false;
        this.hideCrosshair = false;
        this.hideScoreboard = false;
        this.hideBossBar = false;
        this.hideStatusEffects = false;
        this.hideSubtitles = false;
        this.hideVignette = false;
        this.hideChat = false;
    }
}
