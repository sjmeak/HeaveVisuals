package rtx.heave.api.drags.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.drags.Draggable;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.impl.Utils.HolyWorldHelper;
import rtx.heave.api.modules.impl.Utils.HolyWorldHelper.ActiveZone;
import rtx.heave.api.modules.impl.Utils.HolyWorldHelper.PyrotechnicItemType;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.render2d.Render2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HolyWorldComp extends Draggable {
    private float animatedWidth = 110.0f;
    private float animatedHeight = 26.0f;

    public HolyWorldComp() {
        super("holyworld_hud", 180.0f, 100.0f);
    }

    @Override
    public String displayName() {
        return "HolyWorld HUD";
    }

    @Override
    public float width() {
        return this.animatedWidth;
    }

    @Override
    public float height() {
        return this.animatedHeight;
    }

    @Override
    public boolean isInteractive() {
        HolyWorldHelper module = ModuleManager.get().get(HolyWorldHelper.class);
        return module != null && module.isEnabled();
    }

    @Override
    protected void render(DrawContext drawContext) {
        HolyWorldHelper module = ModuleManager.get().get(HolyWorldHelper.class);
        boolean dragMode = DragSystem.get().isDragModeActive();
        if ((module == null || !module.isEnabled()) && !dragMode) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null && !dragMode) {
            return;
        }

        List<HudEntry> entries = new ArrayList<>();

        if (module != null && module.isEnabled()) {
            for (ActiveZone zone : module.getActiveZones()) {
                double remaining = zone.getRemainingSeconds();
                if (remaining > 0.0) {
                    entries.add(new HudEntry(
                        new ItemStack(zone.type.fallbackItem),
                        zone.type.displayName,
                        String.format(Locale.ROOT, "%.1fs", remaining),
                        remaining <= 2.0
                    ));
                }
            }

            PyrotechnicItemType held = module.getHeldType(mc);
            if (held != null && held != PyrotechnicItemType.SNOWBALL) {
                boolean alreadyInZones = false;
                for (ActiveZone z : module.getActiveZones()) {
                    if (z.type == held && z.getRemainingSeconds() > 0.0) {
                        alreadyInZones = true;
                        break;
                    }
                }
                if (!alreadyInZones) {
                    entries.add(new HudEntry(
                        new ItemStack(held.fallbackItem),
                        held.displayName,
                        String.format(Locale.ROOT, "%.0fs", held.durationMs / 1000.0),
                        false
                    ));
                }
            }
        }

        if (entries.isEmpty()) {
            if (dragMode) {
                entries.add(new HudEntry(
                    new ItemStack(Items.NETHER_STAR),
                    "Стан",
                    "15.0s",
                    false
                ));
            } else {
                return;
            }
        }

        float x = this.getX();
        float y = this.getY();

        int rowHeight = 22;
        float targetHeight = (float) entries.size() * rowHeight + 4.0f;
        float maxTargetWidth = 90.0f;

        for (HudEntry entry : entries) {
            int nameW = mc.textRenderer.getWidth(entry.name);
            int valueW = mc.textRenderer.getWidth(entry.value);
            float w = 24.0f + nameW + 12.0f + valueW + 8.0f;
            if (w > maxTargetWidth) {
                maxTargetWidth = w;
            }
        }

        this.animatedWidth += (maxTargetWidth - this.animatedWidth) * 0.2f;
        if (Math.abs(this.animatedWidth - maxTargetWidth) < 0.5f) {
            this.animatedWidth = maxTargetWidth;
        }

        this.animatedHeight += (targetHeight - this.animatedHeight) * 0.2f;
        if (Math.abs(this.animatedHeight - targetHeight) < 0.5f) {
            this.animatedHeight = targetHeight;
        }

        float curW = this.animatedWidth;
        float curH = this.animatedHeight;

        InterfaceModule iface = InterfaceModule.getInstance();
        float radius = iface != null ? iface.rectCornerRadius.getFloat() : 4.0f;
        int bgColor = dragMode ? 0xCC18181B : 0xAA101012;
        Render2D.rect(drawContext, x, y, curW, curH, radius, bgColor);

        for (int i = 0; i < entries.size(); i++) {
            HudEntry entry = entries.get(i);
            float rowY = y + 2.0f + (float) i * rowHeight;

            drawContext.drawItem(entry.icon, (int)(x + 3.0f), (int)(rowY + 2.0f));

            float textX = x + 23.0f;
            float textY = rowY + 6.0f;
            drawContext.drawText(mc.textRenderer, Text.literal(entry.name), (int) textX, (int) textY, 0xFFFFFFFF, true);

            if (entry.critical) {
                boolean flash = (System.currentTimeMillis() / 250L) % 2L == 0L;
                if (flash) {
                    int nameWidth = mc.textRenderer.getWidth(entry.name);
                    drawContext.drawText(mc.textRenderer, Text.literal("!"), (int)(textX + nameWidth + 2.0f), (int) textY, 0xFFFF3333, true);
                }
            }

            int valueWidth = mc.textRenderer.getWidth(entry.value);
            float valueX = x + curW - valueWidth - 6.0f;
            int valueColor = entry.critical ? 0xFFFF4444 : 0xFFFFAA00;
            drawContext.drawText(mc.textRenderer, Text.literal(entry.value), (int) valueX, (int) textY, valueColor, true);
        }
    }

    private record HudEntry(ItemStack icon, String name, String value, boolean critical) {}
}
