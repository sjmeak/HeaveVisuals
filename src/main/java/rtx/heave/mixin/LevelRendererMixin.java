package rtx.heave.mixin;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.render.WorldRenderEvent;
import rtx.heave.api.modules.impl.Visuals.ChinaHat;
import rtx.heave.api.modules.impl.Visuals.FogBlur;
import rtx.heave.api.modules.impl.Visuals.JumpCircle;
import rtx.heave.api.modules.impl.Visuals.KillEffect;
import rtx.heave.api.ui.window.WorldGuiCloseAnimation;
import rtx.heave.utils.render.post.customsky.CustomSkyRenderer;
import rtx.heave.utils.render.post.fogblur.FogBlurRenderer;
import rtx.heave.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.heave.utils.render.post.wetworld.WetWorldRenderer;
import rtx.heave.utils.render.render2d.blur.BlurFramebuffer;
import rtx.heave.utils.render.wave.WindWaveRenderer;

@Mixin(net.minecraft.client.render.WorldRenderer.class)

public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Unique
    private static final Matrix4f heave_skyViewProj = new Matrix4f();

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void heave_captureFogBlurFallback(ObjectAllocator allocator, RenderTickCounter deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return;
        }
        if (fogColor != null) {
            FogBlurRenderer.setFallbackColor(fogColor.x, fogColor.y, fogColor.z);
            BlurFramebuffer.setSkyFallbackColor(fogColor.x, fogColor.y, fogColor.z);
        }
        FogBlurRenderer.beginFrame();
        WindWaveRenderer.beginFrame();
    }

    @Inject(method="render", at={@At(value="RETURN")}, require = 0)
    private void heave_worldRenderEvent(ObjectAllocator allocator, RenderTickCounter deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return;
        }
        MatrixStack stack = new MatrixStack();
        stack.multiplyPositionMatrix((Matrix4fc)new Matrix4f((Matrix4fc)positionMatrix));
        WorldGuiCloseAnimation.captureWorldMatrices(projectionMatrix, positionMatrix, camera.getCameraPos());
        GuiLayerBlurRenderer.snapshotWorldDepth();
        this.applyFogBlur();
        WorldRenderEvent worldRenderEvent = new WorldRenderEvent(stack, deltaTracker.getTickProgress(true), camera, new Matrix4f((Matrix4fc)positionMatrix), new Matrix4f((Matrix4fc)projectionMatrix));
        EventBus.get().post(worldRenderEvent);
        this.applyJumpCircleDistortion(camera, positionMatrix, projectionMatrix, frustumMatrix);
        DragSystem.get().applyDragDistortion(this.client.getFramebuffer());
        this.renderChinaHat(worldRenderEvent);
    }






    private void applyFogBlur() {
        FogBlur fogBlur = FogBlur.getInstance();
        if (fogBlur != null && fogBlur.isEnabled()) {
            fogBlur.onAfterTranslucent(this.client.getFramebuffer());
        }
    }
    private void applyJumpCircleDistortion(Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix) {
        JumpCircle jumpCircle = JumpCircle.getInstance();
        if (jumpCircle != null && jumpCircle.isEnabled()) {
            jumpCircle.onAfterWorld(this.client.getFramebuffer(), positionMatrix, projectionMatrix, frustumMatrix, camera);
        }
    }

    private void renderChinaHat(WorldRenderEvent event) {
        ChinaHat chinaHat = ChinaHat.getInstance();
        if (chinaHat != null && chinaHat.isEnabled()) {
            chinaHat.renderAfterPostEffects(event);
        }
    }


}

