package rtx.heave.api.drags.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.drags.Draggable;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Visuals.TotemCounter;

public final class TotemCounterComp extends Draggable {
    private static final ItemStack TOTEM_STACK = new ItemStack(Items.TOTEM_OF_UNDYING);

    public TotemCounterComp() {
        super("totem_counter", 250.0f, 150.0f);
    }

    @Override
    public String displayName() {
        return "Тотем коунтер";
    }

    @Override
    public float width() {
        return 16.0f;
    }

    @Override
    public float height() {
        return 16.0f;
    }

    @Override
    public boolean isInteractive() {
        TotemCounter module = ModuleManager.get().get(TotemCounter.class);
        return module != null && module.isEnabled();
    }

    @Override
    protected void render(DrawContext drawContext) {
        TotemCounter module = ModuleManager.get().get(TotemCounter.class);
        boolean dragMode = DragSystem.get().isDragModeActive();
        if ((module == null || !module.isEnabled()) && !dragMode) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null && !dragMode) {
            return;
        }

        int count = getTotemCount(mc);
        if (count == 0 && dragMode) {
            count = 1;
        }

        float x = this.getX();
        float y = this.getY();

        drawContext.drawItem(TOTEM_STACK, (int) x, (int) y);

        String countStr = String.valueOf(count);
        float scale = 0.85f;
        float textW = (float) mc.textRenderer.getWidth(countStr) * scale;
        float textX = x + (16.0f - textW) / 2.0f;
        float textY = y + 16.0f - (9.0f * scale) + 0.5f;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(textX, textY);
        drawContext.getMatrices().scale(scale, scale);
        drawContext.drawText(mc.textRenderer, Text.literal(countStr), 0, 0, 0xFFFFFFFF, true);
        drawContext.getMatrices().popMatrix();
    }

    public static int getTotemCount(MinecraftClient mc) {
        if (mc == null || mc.player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        return count;
    }
}
