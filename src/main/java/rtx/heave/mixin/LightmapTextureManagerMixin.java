package rtx.heave.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.modules.impl.Visuals.FullBright;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    private boolean dirty;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void heave_markDirtyForFullbright(CallbackInfo ci) {
        FullBright fb = FullBright.getInstance();
        if (fb != null && fb.isEnabled()) {
            this.dirty = true;
        }
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"
        ),
        require = 0
    )
    private Object heave_fullbrightGamma(SimpleOption<?> instance) {
        Object val = instance.getValue();
        FullBright fb = FullBright.getInstance();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (fb != null && fb.isEnabled() && mc != null && mc.options != null && instance == mc.options.getGamma()) {
            return Double.valueOf(fb.getGamma());
        }
        return val;
    }
}
