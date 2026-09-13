package rtx.heave.mixin;

import java.awt.Color;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rtx.heave.api.modules.impl.Visuals.CustomFog;
import rtx.heave.api.modules.impl.Visuals.FogBlur;
import rtx.heave.api.modules.impl.Visuals.KillEffect;

@Mixin(FogRenderer.class)
public abstract class FogRendererAmbienceMixin {
    @ModifyArg(
        method = "applyFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;method_71110(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"),
        index = 2,
        require = 0
    )
    private Vector4f heave_modifyFogColor(Vector4f color) {
        return FogRendererAmbienceMixin.resolveFogColor(color);
    }

    @ModifyArgs(
        method = "applyFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;method_71110(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"),
        require = 0
    )
    private void heave_scaleFogDistances(Args args) {
        CustomFog customFog = CustomFog.getInstance();
        if (customFog != null && customFog.isEnabled()) {
            float start = customFog.getStartDistance();
            float end = customFog.getEndDistance();
            args.set(3, (Object) Float.valueOf(start));
            args.set(4, (Object) Float.valueOf(end));
            args.set(5, (Object) Float.valueOf(start));
            args.set(6, (Object) Float.valueOf(end));
        }

        FogBlur fogBlur = FogBlur.getInstance();
        if (fogBlur != null && fogBlur.hasCustomFogDistance()) {
            float factor = fogBlur.getFogDistanceFactor();
            for (int i = 3; i <= 8; ++i) {
                float value = ((Float) args.get(i)).floatValue();
                args.set(i, (Object) Float.valueOf(value * factor));
            }
        }
    }

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true, require = 0)
    private void heave_setupFogReturn(Camera camera, int renderDistance, RenderTickCounter deltaTracker, float tickProgress, ClientWorld level, CallbackInfoReturnable<Vector4f> cir) {
        Vector4f modified = FogRendererAmbienceMixin.resolveFogColor((Vector4f) cir.getReturnValue());
        if (modified != null && modified != cir.getReturnValue()) {
            cir.setReturnValue(modified);
        }
    }

    private static Vector4f resolveFogColor(Vector4f fogColor) {
        if (fogColor == null) return null;
        CustomFog customFog = CustomFog.getInstance();
        FogBlur fogBlur = FogBlur.getInstance();
        boolean customFogActive = customFog != null && customFog.isEnabled() && customFog.useCustomColor();
        boolean fogBlurColor = fogBlur != null && fogBlur.hasCustomFogColor();
        if (!customFogActive && !fogBlurColor) {
            return fogColor;
        }

        float r = fogColor.x;
        float g = fogColor.y;
        float b = fogColor.z;
        boolean changed = false;

        if (customFogActive) {
            Color c = customFog.getFogColor();
            r = (float) c.getRed() / 255.0f;
            g = (float) c.getGreen() / 255.0f;
            b = (float) c.getBlue() / 255.0f;
            changed = true;
        } else if (fogBlurColor) {
            int c = fogBlur.getCustomFogColor();
            r = (float)(c >> 16 & 0xFF) / 255.0f;
            g = (float)(c >> 8 & 0xFF) / 255.0f;
            b = (float)(c & 0xFF) / 255.0f;
            changed = true;
        }

        float saturation = KillEffect.getWorldSaturationMultiplier();
        if (Float.isFinite(saturation) && Math.abs(saturation - 1.0f) > 5.0E-4f) {
            float lum = r * 0.2126f + g * 0.7152f + b * 0.0722f;
            r = MathHelper.clamp(lum + (r - lum) * saturation, 0.0f, 1.0f);
            g = MathHelper.clamp(lum + (g - lum) * saturation, 0.0f, 1.0f);
            b = MathHelper.clamp(lum + (b - lum) * saturation, 0.0f, 1.0f);
            changed = true;
        }

        return changed ? new Vector4f(r, g, b, fogColor.w) : fogColor;
    }
}
