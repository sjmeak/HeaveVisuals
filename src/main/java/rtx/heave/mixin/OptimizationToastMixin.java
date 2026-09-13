package rtx.heave.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Utils.Optimization;

import java.util.Deque;
import java.util.List;

@Mixin(ToastManager.class)
public abstract class OptimizationToastMixin {
    @Shadow @Final private List<?> visibleEntries;
    @Shadow @Final private Deque<Toast> toastQueue;


    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave$skipEmptyDraw(DrawContext context, CallbackInfo ci) {
        if (this.visibleEntries.isEmpty() && this.toastQueue.isEmpty() && Optimization.shouldSkipEmptyToasts()) {
            ci.cancel();
        }
    }
}
