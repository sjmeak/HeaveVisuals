package rtx.heave.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix3;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.heave.api.modules.impl.Utils.CameraSettings;
import rtx.heave.api.modules.impl.Visuals.AspectRatio;
import rtx.heave.api.modules.impl.Visuals.KillEffect;
import rtx.heave.api.modules.impl.Visuals.NoRender;
import rtx.heave.api.ui.UI;
import rtx.heave.api.ui.theme.ClientAccent;
import rtx.heave.utils.FreezeProfiler;
import rtx.heave.utils.render.others.RenderCompatibility;
import rtx.heave.utils.render.post.guilayerblur.GuiCapture;
import rtx.heave.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.heave.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import rtx.heave.utils.render.post.itemoutline.ItemOutlineRenderer;
import rtx.heave.utils.render.post.saturation.Saturation2D;
import rtx.heave.utils.render.render2d.ClientPalette;

@Mixin(net.minecraft.client.render.GameRenderer.class)

public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract float getFarPlaneDistance();

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void heave_primeRenderCompatibility(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        FreezeProfiler.markFrame();
        ClientAccent.beginFrame();
        ClientPalette.update();
        RenderCompatibility.primeFromCurrentContext();
    }

    @Inject(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.BEFORE)}, require = 0)
    private void heave_preGuiRender(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return;
        }
        this.heave_applyWorldSaturation();
        if (UI.motionBlurCapturePending()) {
            GuiMotionBlurRenderer.captureBackground(UI.motionBlurCaptureRadius());
        }
    }

    @Inject(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.AFTER)}, require = 0)
    private void heave_postGuiRender(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        if (GuiLayerBlurRenderer.captureActiveThisFrame()) {
            UI.dropPendingBlurs();
            if (GuiCapture.active()) {
                GuiLayerBlurRenderer.composite(GuiCapture.scale(), GuiCapture.blurRadius());
            }
        } else {
            UI.flushMotionBlur();
        }
    }

    @Inject(method="close", at={@At(value="RETURN")}, require = 0)
    private void heave_closeMotionBlur(CallbackInfo ci) {
        GuiMotionBlurRenderer.shutdown();
        GuiLayerBlurRenderer.shutdown();
        ItemOutlineRenderer.clear();
    }

    @ModifyReturnValue(method="getFov", at={@At(value="RETURN")}, require = 0)
    private float heave_killZoomFov(float original) {
        float scale = KillEffect.getKillZoomFovScale();
        return scale == 1.0f ? original : original * scale;
    }

    @ModifyReturnValue(method="getFov", at={@At(value="RETURN")}, require = 0)
    private float heave_cameraZoomFov(float original) {
        float scale = CameraSettings.getFovScale();
        return scale == 1.0f ? original : original * scale;
    }

    @ModifyArg(
        method = "getBasicProjectionMatrix",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;", remap = false),
        index = 1,
        require = 0
    )
    private float heave_modifyAspectRatio(float original) {
        AspectRatio ar = AspectRatio.getInstance();
        if (ar != null && ar.isEnabled()) {
            return ar.getRatio();
        }
        return original;
    }

    @WrapOperation(method="renderWorld", at={@At(value="INVOKE", target="Lnet/minecraft/client/render/ProjectionMatrix3;set(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")}, require = 0)
    private GpuBufferSlice heave_handAspectRatio(ProjectionMatrix3 buffer, int width, int height, float fov, Operation<GpuBufferSlice> original) {
        AspectRatio ar = AspectRatio.getInstance();
        if (ar != null && ar.isEnabled()) {
            int adjustedWidth = Math.max(1, Math.round(height * ar.getRatio()));
            return (GpuBufferSlice)original.call(new Object[]{buffer, adjustedWidth, height, Float.valueOf(fov)});
        }
        return (GpuBufferSlice)original.call(new Object[]{buffer, width, height, Float.valueOf(fov)});
    }

    @ModifyReturnValue(method="getBasicProjectionMatrix", at={@At(value="RETURN")}, require = 0)
    private Matrix4f heave_killCameraShake(Matrix4f original) {
        if (original == null) {
            return original;
        }
        float shake = KillEffect.getKillShakeDegrees();
        if (shake == 0.0f) {
            return original;
        }
        return new Matrix4f((Matrix4fc)original).rotateZ((float)Math.toRadians(shake));
    }

    @Inject(method="tiltViewWhenHurt", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void heave_noRenderCameraShake(MatrixStack stack, float tickDelta, CallbackInfo ci) {
        if (NoRender.isActive("\u0422\u0440\u044f\u0441\u043a\u0430 \u043a\u0430\u043c\u0435\u0440\u044b")) {
            ci.cancel();
        }
    }

    private void heave_applyWorldSaturation() {
        float saturation = KillEffect.getWorldSaturationMultiplier();
        if (!Float.isFinite(saturation) || Math.abs(saturation - 1.0f) <= 5.0E-4f) {
            return;
        }
        Saturation2D.applyWithCopy((float)Math.clamp(saturation, 0.0f, 2.0f));
    }
}

