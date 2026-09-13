package rtx.heave.mixin.shulkerview;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.heave.api.mods.shulkerview.hook.ShulkerPreviewGuiGraphics;

@Mixin(net.minecraft.client.gui.DrawContext.class)

public abstract class GuiGraphicsExtractorMixin
implements ShulkerPreviewGuiGraphics {
    @Unique
    private int heave_shulkerPreviewMouseX = Integer.MIN_VALUE;
    @Unique
    private int heave_shulkerPreviewMouseY = Integer.MIN_VALUE;

    @Override
    public int heave_getMouseX() {
        return this.heave_shulkerPreviewMouseX;
    }

    @Override
    public int heave_getMouseY() {
        return this.heave_shulkerPreviewMouseY;
    }

    @Override
    public void heave_setMouse(int mouseX, int mouseY) {
        this.heave_shulkerPreviewMouseX = mouseX;
        this.heave_shulkerPreviewMouseY = mouseY;
    }
}

