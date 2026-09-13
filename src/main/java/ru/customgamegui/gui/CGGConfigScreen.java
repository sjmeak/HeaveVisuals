package ru.customgamegui.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.customgamegui.config.CGGConfig;
import ru.customgamegui.config.CGGConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CGGConfigScreen extends Screen {
    private final Screen parent;
    private ConfigListWidget list;
    private final CGGConfig config;

    public CGGConfigScreen(Screen parent) {
        super(Text.translatable("cgg.gui.title"));
        this.parent = parent;
        this.config = CGGConfigManager.getConfig();
    }

    @Override
    protected void init() {
        super.init();

        this.list = new ConfigListWidget(this.client, this.width, this.height - 64, 32, 25);
        this.addSelectableChild(this.list);

        // Master switch
        this.list.addCategory(Text.translatable("category.cgg.title").formatted(Formatting.GOLD, Formatting.BOLD));
        this.list.addBooleanEntry("cgg.gui.enabled", () -> config.enabled, val -> config.enabled = val);

        // Hotbar & XP
        this.list.addCategory(Text.translatable("cgg.gui.tab.hotbar").formatted(Formatting.YELLOW, Formatting.BOLD));
        this.list.addBooleanEntry("cgg.gui.hide_experience_bar", () -> config.hideExperienceBar, val -> config.hideExperienceBar = val);
        this.list.addBooleanEntry("cgg.gui.hide_experience_level", () -> config.hideExperienceLevel, val -> config.hideExperienceLevel = val);
        this.list.addBooleanEntry("cgg.gui.shift_down_without_exp_bar", () -> config.shiftDownWithoutExpBar, val -> config.shiftDownWithoutExpBar = val);
        this.list.addBooleanEntry("cgg.gui.shift_exp_level", () -> config.shiftExpLevelWhenBarHidden, val -> config.shiftExpLevelWhenBarHidden = val);
        this.list.addSliderEntry("cgg.gui.status_bar_offset", 0, 15, config.statusBarOffset, val -> config.statusBarOffset = val);
        this.list.addPercentSliderEntry("cgg.gui.hotbar_bg_opacity", 0, 100, config.hotbarBackgroundOpacity, val -> config.hotbarBackgroundOpacity = val);
        this.list.addPercentSliderEntry("cgg.gui.hotbar_selection_opacity", 0, 100, config.hotbarSelectionOpacity, val -> config.hotbarSelectionOpacity = val);
        this.list.addBooleanEntry("cgg.gui.hide_hotbar", () -> config.hideHotbar, val -> config.hideHotbar = val);
        this.list.addBooleanEntry("cgg.gui.hide_held_item_name", () -> config.hideHeldItemName, val -> config.hideHeldItemName = val);

        // Status Bars & Saturation
        this.list.addCategory(Text.translatable("cgg.gui.tab.status_bars").formatted(Formatting.RED, Formatting.BOLD));
        this.list.addBooleanEntry("cgg.gui.hide_health_bar", () -> config.hideHealthBar, val -> config.hideHealthBar = val);
        this.list.addBooleanEntry("cgg.gui.hide_hunger_bar", () -> config.hideHungerBar, val -> config.hideHungerBar = val);
        this.list.addBooleanEntry("cgg.gui.show_saturation_bar", () -> config.showSaturationBar, val -> config.showSaturationBar = val);
        this.list.addBooleanEntry("cgg.gui.saturation_preview_food", () -> config.saturationPreviewFood, val -> config.saturationPreviewFood = val);
        this.list.addBooleanEntry("cgg.gui.hide_armor_bar", () -> config.hideArmorBar, val -> config.hideArmorBar = val);
        this.list.addBooleanEntry("cgg.gui.hide_air_bar", () -> config.hideAirBar, val -> config.hideAirBar = val);

        // AppleSkin Integration
        boolean isAppleSkin = FabricLoader.getInstance().isModLoaded("appleskin");
        Text appleSkinTitle = Text.translatable("cgg.gui.tab.appleskin");
        if (isAppleSkin) {
            this.list.addCategory(Text.literal("").append(appleSkinTitle).append(" (Active)").formatted(Formatting.GREEN, Formatting.BOLD));
        } else {
            this.list.addCategory(appleSkinTitle.copy().formatted(Formatting.GRAY, Formatting.BOLD));
        }
        this.list.addBooleanEntry("cgg.gui.appleskin_integration", () -> config.appleSkinIntegration, val -> config.appleSkinIntegration = val);
        this.list.addBooleanEntry("cgg.gui.appleskin_hide_default_sat", () -> config.appleSkinHideDefaultSaturation, val -> config.appleSkinHideDefaultSaturation = val);

        // Overlays & UI
        this.list.addCategory(Text.translatable("cgg.gui.tab.overlays").formatted(Formatting.AQUA, Formatting.BOLD));
        this.list.addBooleanEntry("cgg.gui.hide_crosshair", () -> config.hideCrosshair, val -> config.hideCrosshair = val);
        this.list.addBooleanEntry("cgg.gui.hide_scoreboard", () -> config.hideScoreboard, val -> config.hideScoreboard = val);
        this.list.addBooleanEntry("cgg.gui.hide_boss_bar", () -> config.hideBossBar, val -> config.hideBossBar = val);
        this.list.addBooleanEntry("cgg.gui.hide_status_effects", () -> config.hideStatusEffects, val -> config.hideStatusEffects = val);
        this.list.addBooleanEntry("cgg.gui.hide_subtitles", () -> config.hideSubtitles, val -> config.hideSubtitles = val);
        this.list.addBooleanEntry("cgg.gui.hide_vignette", () -> config.hideVignette, val -> config.hideVignette = val);
        this.list.addBooleanEntry("cgg.gui.hide_chat", () -> config.hideChat, val -> config.hideChat = val);

        // Bottom buttons: Reset & Done
        int buttonY = this.height - 27;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("cgg.gui.reset"), button -> {
            this.config.resetToDefaults();
            CGGConfigManager.save();
            if (this.client != null) {
                this.client.setScreen(new CGGConfigScreen(this.parent));
            }
        }).dimensions(this.width / 2 - 155, buttonY, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
                .dimensions(this.width / 2 + 5, buttonY, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.list != null) {
            this.list.render(context, mouseX, mouseY, delta);
        }
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        CGGConfigManager.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public static class ConfigListWidget extends ElementListWidget<ConfigListWidget.BaseEntry> {
        public ConfigListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public void addCategory(Text title) {
            this.addEntry(new CategoryEntry(title));
        }

        public void addBooleanEntry(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.addEntry(new BooleanEntry(key, getter, setter));
        }

        public void addSliderEntry(String key, int min, int max, int current, Consumer<Integer> setter) {
            this.addEntry(new SliderEntry(key, min, max, current, setter));
        }

        public void addPercentSliderEntry(String key, int min, int max, int current, Consumer<Integer> setter) {
            this.addEntry(new PercentSliderEntry(key, min, max, current, setter));
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        public abstract static class BaseEntry extends ElementListWidget.Entry<BaseEntry> {}

        public static class CategoryEntry extends BaseEntry {
            private final Text title;

            public CategoryEntry(Text title) {
                this.title = title;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                MinecraftClient client = MinecraftClient.getInstance();
                context.drawCenteredTextWithShadow(client.textRenderer, this.title, this.getX() + this.getWidth() / 2, this.getY() + 6, 0xFFFFFFFF);
            }

            @Override
            public List<? extends Element> children() {
                return List.of();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of();
            }
        }

        public static class BooleanEntry extends BaseEntry {
            private final String key;
            private final Supplier<Boolean> getter;
            private final Consumer<Boolean> setter;
            private final ButtonWidget button;
            private final List<ClickableWidget> children = new ArrayList<>();

            public BooleanEntry(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
                this.key = key;
                this.getter = getter;
                this.setter = setter;

                this.button = ButtonWidget.builder(getButtonText(), b -> {
                    boolean next = !this.getter.get();
                    this.setter.accept(next);
                    b.setMessage(getButtonText());
                    CGGConfigManager.save();
                }).dimensions(0, 0, 75, 20).build();

                String descKey = key + ".desc";
                Text desc = Text.translatable(descKey);
                if (!desc.getString().equals(descKey)) {
                    this.button.setTooltip(Tooltip.of(desc));
                }

                this.children.add(this.button);
            }

            private Text getButtonText() {
                boolean val = getter.get();
                return val ? Text.literal("ON").formatted(Formatting.GREEN, Formatting.BOLD)
                           : Text.literal("OFF").formatted(Formatting.RED, Formatting.BOLD);
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                MinecraftClient client = MinecraftClient.getInstance();
                Text label = Text.translatable(key);
                context.drawTextWithShadow(client.textRenderer, label, this.getX() + 5, this.getY() + (this.getHeight() - 8) / 2, 0xFFE0E0E0);

                this.button.setX(this.getX() + this.getWidth() - 80);
                this.button.setY(this.getY());
                this.button.render(context, mouseX, mouseY, deltaTicks);
            }

            @Override
            public List<? extends Element> children() {
                return this.children;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return this.children;
            }
        }

        public static class SliderEntry extends BaseEntry {
            private final String key;
            private final int min;
            private final int max;
            private final Consumer<Integer> setter;
            private int currentValue;
            private final SliderWidget slider;
            private final List<ClickableWidget> children = new ArrayList<>();

            public SliderEntry(String key, int min, int max, int current, Consumer<Integer> setter) {
                this.key = key;
                this.min = min;
                this.max = max;
                this.currentValue = current;
                this.setter = setter;

                double initialProgress = (double) (current - min) / (double) (max - min);

                this.slider = new SliderWidget(0, 0, 75, 20, getMessageForValue(current), initialProgress) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(getMessageForValue(SliderEntry.this.currentValue));
                    }

                    @Override
                    protected void applyValue() {
                        SliderEntry.this.currentValue = (int) Math.round(min + this.value * (max - min));
                        SliderEntry.this.setter.accept(SliderEntry.this.currentValue);
                        CGGConfigManager.save();
                    }
                };

                String descKey = key + ".desc";
                Text desc = Text.translatable(descKey);
                if (!desc.getString().equals(descKey)) {
                    this.slider.setTooltip(Tooltip.of(desc));
                }

                this.children.add(this.slider);
            }

            private Text getMessageForValue(int value) {
                return Text.literal(value + "px");
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                MinecraftClient client = MinecraftClient.getInstance();
                Text label = Text.translatable(key);
                context.drawTextWithShadow(client.textRenderer, label, this.getX() + 5, this.getY() + (this.getHeight() - 8) / 2, 0xFFE0E0E0);

                this.slider.setX(this.getX() + this.getWidth() - 80);
                this.slider.setY(this.getY());
                this.slider.render(context, mouseX, mouseY, deltaTicks);
            }

            @Override
            public List<? extends Element> children() {
                return this.children;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return this.children;
            }
        }

        public static class PercentSliderEntry extends BaseEntry {
            private final String key;
            private final int min;
            private final int max;
            private final Consumer<Integer> setter;
            private int currentValue;
            private final SliderWidget slider;
            private final List<ClickableWidget> children = new ArrayList<>();

            public PercentSliderEntry(String key, int min, int max, int current, Consumer<Integer> setter) {
                this.key = key;
                this.min = min;
                this.max = max;
                this.currentValue = current;
                this.setter = setter;

                double initialProgress = (double) (current - min) / (double) (max - min);

                this.slider = new SliderWidget(0, 0, 75, 20, getMessageForValue(current), initialProgress) {
                    @Override
                    protected void updateMessage() {
                        this.setMessage(getMessageForValue(PercentSliderEntry.this.currentValue));
                    }

                    @Override
                    protected void applyValue() {
                        PercentSliderEntry.this.currentValue = (int) Math.round(min + this.value * (max - min));
                        PercentSliderEntry.this.setter.accept(PercentSliderEntry.this.currentValue);
                        CGGConfigManager.save();
                    }
                };

                String descKey = key + ".desc";
                Text desc = Text.translatable(descKey);
                if (!desc.getString().equals(descKey)) {
                    this.slider.setTooltip(Tooltip.of(desc));
                }

                this.children.add(this.slider);
            }

            private Text getMessageForValue(int value) {
                return Text.literal(value + "%");
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                MinecraftClient client = MinecraftClient.getInstance();
                Text label = Text.translatable(key);
                context.drawTextWithShadow(client.textRenderer, label, this.getX() + 5, this.getY() + (this.getHeight() - 8) / 2, 0xFFE0E0E0);

                this.slider.setX(this.getX() + this.getWidth() - 80);
                this.slider.setY(this.getY());
                this.slider.render(context, mouseX, mouseY, deltaTicks);
            }

            @Override
            public List<? extends Element> children() {
                return this.children;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return this.children;
            }
        }
    }
}
