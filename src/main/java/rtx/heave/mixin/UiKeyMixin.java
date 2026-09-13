package rtx.heave.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.ClickGui;
import rtx.heave.api.ui.UI;
import rtx.heave.utils.input.HeaveKeyBindings;
import rtx.heave.utils.sounds.Sounds;

@Mixin(net.minecraft.client.Keyboard.class)
public abstract class UiKeyMixin {
    @Inject(method = "onKey", at = @At("HEAD"), require = 0, cancellable = true)
    private void heave_uiKey(long window, int action, KeyInput keyEvent, CallbackInfo ci) {
        if (action != 1) {
            return;
        }
        boolean matches = HeaveKeyBindings.OPEN_MENU != null && HeaveKeyBindings.OPEN_MENU.matchesKey(keyEvent);
        if (!matches) {
            ClickGui clickGui = ModuleManager.get().get(ClickGui.class);
            if (clickGui != null && keyEvent.key() == clickGui.getBind().getCode()) {
                matches = true;
            }
        }
        if (!matches) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) {
            mc.setScreen((Screen)UI.INSTANCE);
            Sounds.play("gui_open");
            ci.cancel();
        } else if (mc.currentScreen == UI.INSTANCE) {
            UI.INSTANCE.close();
            ci.cancel();
        }
    }
}
