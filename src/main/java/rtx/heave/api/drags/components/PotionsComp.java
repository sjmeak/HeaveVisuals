package rtx.heave.api.drags.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.drags.components.ListHudComp;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.PotionsModule;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.render2d.Render2D;

public final class PotionsComp extends ListHudComp {
    private static final RegistryEntry<StatusEffect>[] PREVIEW_EFFECTS = new RegistryEntry[]{
        StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.REGENERATION, StatusEffects.FIRE_RESISTANCE
    };
    private static final int NEGATIVE_COLOR = ColorUtil.lerpColor(-3355444, -53714, 0.32f);

    private float lastVanillaWidth = 100.0f;
    private float lastVanillaHeight = 46.0f;

    private long previewSwitchMs;
    private int previewIndex;

    public PotionsComp() {
        super("potions", "Potions", PotionsModule.class, 104.0f, 33.0f);
    }

    private PotionsModule getModule() {
        return ModuleManager.get().get(PotionsModule.class);
    }

    @Override
    public float width() {
        PotionsModule mod = getModule();
        if (mod != null && mod.isVanilla()) {
            return this.lastVanillaWidth;
        }
        return super.width();
    }

    @Override
    public float height() {
        PotionsModule mod = getModule();
        if (mod != null && mod.isVanilla()) {
            return this.lastVanillaHeight;
        }
        return super.height();
    }

    @Override
    public boolean isInteractive() {
        PotionsModule mod = getModule();
        return mod != null && mod.isEnabled();
    }

    @Override
    public boolean isVisible() {
        PotionsModule mod = getModule();
        if (mod == null || !mod.isEnabled()) {
            return DragSystem.get().isDragModeActive();
        }
        return super.isVisible();
    }

    @Override
    public void render(DrawContext drawContext) {
        PotionsModule mod = getModule();
        boolean dragMode = DragSystem.get().isDragModeActive();
        if (mod == null || (!mod.isEnabled() && !dragMode)) {
            return;
        }
        if (mod.isVanilla()) {
            renderVanilla(drawContext, mod);
            return;
        }
        super.render(drawContext);
    }

    private static record VanillaLine(RegistryEntry<StatusEffect> effect, String name, String amplifier, String duration) {}

    private void renderVanilla(DrawContext context, PotionsModule mod) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean dragMode = DragSystem.get().isDragModeActive();
        if ((mod == null || !mod.isEnabled()) && !dragMode) return;
        if ((mc.player == null || mc.world == null) && !dragMode) return;

        List<VanillaLine> lines = new ArrayList<>();
        if (mc.player != null && !mc.player.getStatusEffects().isEmpty()) {
            for (StatusEffectInstance inst : mc.player.getStatusEffects()) {
                if (inst == null) continue;
                RegistryEntry<StatusEffect> entry = inst.getEffectType();
                String name = Formatting.strip(entry.value().getName().getString());
                String amp = formatRoman(inst.getAmplifier());
                String dur = formatDuration(inst);
                lines.add(new VanillaLine(entry, name, amp, dur));
            }
        } else if (dragMode) {
            lines.add(new VanillaLine(StatusEffects.SPEED, "Speed", "II", "0:42"));
            lines.add(new VanillaLine(StatusEffects.STRENGTH, "Strength", "I", "1:15"));
            lines.add(new VanillaLine(StatusEffects.FIRE_RESISTANCE, "Fire Resistance", "", "**:**"));
        }

        if (lines.isEmpty()) return;

        lines.sort((a, b) -> {
            int lenA = a.name().length() + (a.amplifier().isEmpty() ? 0 : 1 + a.amplifier().length());
            int lenB = b.name().length() + (b.amplifier().isEmpty() ? 0 : 1 + b.amplifier().length());
            return Integer.compare(lenB, lenA);
        });

        float scale = mod.getCustomScale();
        int nameColor = mod.getNameColor();
        int ampColor = mod.getAmplifierColor();
        int durColor = mod.getDurationColor();
        boolean rightBound = mod.isRightBound();

        int maxTextWidth = 0;
        for (VanillaLine line : lines) {
            int lineW = mc.textRenderer.getWidth(line.name + (line.amplifier.isEmpty() ? "" : " " + line.amplifier));
            int durW = mc.textRenderer.getWidth(line.duration);
            maxTextWidth = Math.max(maxTextWidth, Math.max(lineW, durW));
        }

        int unscaledWidth = maxTextWidth + 28;
        int slotHeight = lines.size() > 5 ? Math.max(16, 132 / (lines.size() - 1)) : 23;
        int unscaledHeight = slotHeight * lines.size();

        float finalWidth = unscaledWidth * scale;
        float finalHeight = unscaledHeight * scale;
        this.lastVanillaWidth = Math.max(30.0f, finalWidth);
        this.lastVanillaHeight = Math.max(20.0f, finalHeight);

        float x = this.getX();
        float y = this.getY();

        if (mod.hasBackground()) {
            context.fill((int) (x - 2), (int) (y - 2), (int) (x + finalWidth + 2), (int) (y + finalHeight + 2), mod.getBackgroundColor());
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);

        int slotY = 0;
        for (VanillaLine line : lines) {
            if (rightBound) {
                int rightX = unscaledWidth;
                int iconX = rightX - 18;
                drawVanillaStatusIcon(context, mc, line.effect, iconX, slotY);

                int textRight = iconX - 6;
                int spaceW = line.amplifier.isEmpty() ? 0 : mc.textRenderer.getWidth(" ");
                int ampW = line.amplifier.isEmpty() ? 0 : mc.textRenderer.getWidth(line.amplifier);
                int nameW = mc.textRenderer.getWidth(line.name);
                int totalTitleW = nameW + (line.amplifier.isEmpty() ? 0 : spaceW + ampW);
                int titleStartX = textRight - totalTitleW;

                context.drawText(mc.textRenderer, Text.literal(line.name), titleStartX, slotY - 1, nameColor, true);
                if (!line.amplifier.isEmpty()) {
                    context.drawText(mc.textRenderer, Text.literal(line.amplifier), titleStartX + nameW + spaceW, slotY - 1, ampColor, true);
                }
                int durW = mc.textRenderer.getWidth(line.duration);
                context.drawText(mc.textRenderer, Text.literal(line.duration), textRight - durW, slotY + 9, durColor, true);
            } else {
                drawVanillaStatusIcon(context, mc, line.effect, 0, slotY);
                context.drawText(mc.textRenderer, Text.literal(line.name), 28, slotY - 1, nameColor, true);
                if (!line.amplifier.isEmpty()) {
                    int nameW = mc.textRenderer.getWidth(line.name + " ");
                    context.drawText(mc.textRenderer, Text.literal(line.amplifier), 28 + nameW, slotY - 1, ampColor, true);
                }
                context.drawText(mc.textRenderer, Text.literal(line.duration), 28, slotY + 9, durColor, true);
            }
            slotY += slotHeight;
        }

        context.getMatrices().popMatrix();
    }

    private static String formatRoman(int amplifier) {
        if (amplifier <= 0) return "";
        int level = amplifier + 1;
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }

    private static boolean isNegative(RegistryEntry<StatusEffect> registryEntry) {
        if (registryEntry.value().getCategory() == StatusEffectCategory.HARMFUL) {
            return true;
        }
        return registryEntry.value() == StatusEffects.SLOW_FALLING.value();
    }

    private static float smooth(float f) {
        f = Math.max(0.0f, Math.min(1.0f, f));
        return f * f * (3.0f - 2.0f * f);
    }

    @Override
    protected String headerIconGlyph() {
        return "s";
    }

    @Override
    protected List<ListHudComp.Row> collectRows() {
        ArrayList<ListHudComp.Row> arrayList = new ArrayList<>();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player == null) {
            if (DragSystem.get().isDragModeActive()) {
                arrayList.add(this.previewRow());
            }
            return arrayList;
        }
        for (StatusEffectInstance statusEffectInstance : minecraftClient.player.getStatusEffects()) {
            if (statusEffectInstance == null) continue;
            RegistryEntry<StatusEffect> registryEntry = statusEffectInstance.getEffectType();
            String object = Formatting.strip(registryEntry.value().getName().getString());
            int n = statusEffectInstance.getAmplifier();
            if (n > 0) {
                object = object + " " + (n + 1);
            }
            int n2 = isNegative(registryEntry) ? NEGATIVE_COLOR : 0;
            arrayList.add(new ListHudComp.Row(
                registryEntry, object, formatDuration(statusEffectInstance),
                (drawContext, f, f2, f3, f4) -> drawEffectIcon(registryEntry, f, f2, f3, f4),
                expiryPulse(statusEffectInstance), n2
            ));
        }
        if (arrayList.isEmpty() && DragSystem.get().isDragModeActive()) {
            arrayList.add(this.previewRow());
        }
        return arrayList;
    }

    private static String formatDuration(StatusEffectInstance statusEffectInstance) {
        if (statusEffectInstance.isInfinite()) {
            return "**:**";
        }
        int n = Math.max(0, statusEffectInstance.getDuration()) / 20;
        int n2 = n / 60;
        return n2 + ":" + String.format(Locale.ROOT, "%02d", n % 60);
    }

    private static void drawVanillaStatusIcon(DrawContext context, MinecraftClient client, RegistryEntry<StatusEffect> effect, int x, int y) {
        if (client != null && effect != null && effect.hasKeyAndValue()) {
            try {
                Identifier id = Registries.STATUS_EFFECT.getId(effect.value());
                if (id != null) {
                    Identifier texture = Identifier.of(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png");
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, 18, 18, 18, 18, -1);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        context.drawItem(new ItemStack(Items.POTION), x + 1, y + 1);
    }

    private static void drawEffectIcon(RegistryEntry<StatusEffect> registryEntry, float f, float f2, float f3, float f4) {
        float f5 = (f3 - 8.0f) * 0.5f;
        Render2D.effectIcon(registryEntry, f + f5, f2 + f5, 8.0f, ColorUtil.multAlpha(-1, f4));
    }

    private static ListHudComp.AlphaPulse expiryPulse(StatusEffectInstance statusEffectInstance) {
        if (statusEffectInstance.isInfinite() || statusEffectInstance.getDuration() > 200) {
            return null;
        }
        return () -> {
            int n = Math.max(0, statusEffectInstance.getDuration());
            if (n > 200) {
                return 1.0f;
            }
            float f = 1.0f - (float)n / 200.0f;
            float f2 = smooth(f) * 0.8f;
            float f3 = (float)(System.currentTimeMillis() % 900L) / 900.0f;
            float f4 = 0.5f - 0.5f * (float)Math.cos((double)f3 * 2.0 * Math.PI);
            return 1.0f - f2 * f4;
        };
    }

    private ListHudComp.Row previewRow() {
        long l = System.currentTimeMillis();
        if (l - this.previewSwitchMs >= 1000L) {
            this.previewIndex = (this.previewIndex + 1) % PREVIEW_EFFECTS.length;
            this.previewSwitchMs = l;
        }
        RegistryEntry<StatusEffect> registryEntry = PREVIEW_EFFECTS[this.previewIndex];
        return new ListHudComp.Row("preview", "Example effect", "**:**", (drawContext, f, f2, f3, f4) -> drawEffectIcon(registryEntry, f, f2, f3, f4));
    }
}
