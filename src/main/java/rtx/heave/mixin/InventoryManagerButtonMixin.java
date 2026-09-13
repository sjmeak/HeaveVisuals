package rtx.heave.mixin;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.invmanager.InventoryArranger;
import rtx.heave.api.invmanager.InventoryManagerScreen;
import rtx.heave.utils.inventory.InventoryTemplates;

@Mixin(InventoryScreen.class)
public abstract class InventoryManagerButtonMixin extends HandledScreen<PlayerScreenHandler> {
    @Unique
    private ButtonWidget heave_setsButton;
    @Unique
    private ButtonWidget heave_arrangeButton;

    protected InventoryManagerButtonMixin(PlayerScreenHandler menu, PlayerInventory inventory, Text title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = {@At("TAIL")}, require = 0)
    private void heave_addManagerButtons(CallbackInfo ci) {
    }

    @Inject(method = "render", at = {@At("TAIL")}, require = 0)
    private void heave_tickArranger(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (InventoryArranger.isActive()) {
            InventoryArranger.tick((HandledScreen<?>)(Object)this, InventoryTemplates.active());
        }
    }

    private static boolean canArrange() {
        InventoryTemplates.Template template = InventoryTemplates.active();
        if (template == null) {
            return false;
        }
        if (InventoryArranger.isActive()) {
            return true;
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof HandledScreen<?> acs) {
            return InventoryArranger.needsArrange(acs.getScreenHandler(), template);
        }
        return false;
    }

    private static void triggerArrange() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof HandledScreen<?> acs) {
            InventoryArranger.start(acs);
        }
    }
}
