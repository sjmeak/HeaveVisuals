package rtx.heave.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.ui.UI;
import rtx.heave.mixin.accessor.GuiRendererDrawAccessor;
import rtx.heave.utils.render.post.guilayerblur.GuiCapture;
import rtx.heave.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.heave.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import rtx.heave.utils.render.render2d.ClientSplits;
import rtx.heave.utils.render.render2d.arc.ArcRenderer;
import rtx.heave.utils.render.render2d.blur.BlurFramebuffer;
import rtx.heave.utils.render.render2d.circle.CircleRenderer;
import rtx.heave.utils.render.render2d.glass.GlassRenderer;
import rtx.heave.utils.render.render2d.glow.GlowRenderer;
import rtx.heave.utils.render.render2d.image.ImageRenderer;
import rtx.heave.utils.render.render2d.line.LineRenderer;
import rtx.heave.utils.render.render2d.outline.outline360.Outline360Renderer;
import rtx.heave.utils.render.render2d.outline.outlinedefault.DefaultOutlineRenderer;
import rtx.heave.utils.render.render2d.outline.outlineglass.GlassOutlineRenderer;
import rtx.heave.utils.render.render2d.picker.PickerRenderer;
import rtx.heave.utils.render.render2d.radialglass.RadialGlassRenderer;
import rtx.heave.utils.render.render2d.rectangle.rectdefault.DefaultRectangleRenderer;
import rtx.heave.utils.render.render2d.rectangle.recthalficon.HalfIconRectangleRenderer;
import rtx.heave.utils.render.render2d.rectangle.recthalftone.HalftoneRectangleRenderer;
import rtx.heave.utils.render.render2d.ripple.RippleRenderer;
import rtx.heave.utils.render.render2d.sectormask.SectorMaskRenderer;
import rtx.heave.utils.render.render2d.shape.ShapeRenderer;
import rtx.heave.utils.render.render2d.shimmer.ShimmerRenderer;
import rtx.heave.utils.render.render2d.zippy.ZippyRenderer;
import rtx.heave.utils.render.renderitem.RenderItem;

@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow
    @Final
    private List<?> draws;
    private RenderPass heave_currentRenderPass;
    private boolean heave_blurDrawActive;
    private boolean heave_glassDrawActive;
    private boolean heave_shapeDrawActive;
    private boolean heave_glowDrawActive;
    private boolean heave_glassOutlineDrawActive;
    private boolean heave_circleDrawActive;
    private boolean heave_arcDrawActive;
    private boolean heave_radialGlassDrawActive;
    private boolean heave_sectorMaskDrawActive;
    private boolean heave_pickerDrawActive;
    private boolean heave_rectangleDrawActive;
    private boolean heave_halfIconRectangleDrawActive;
    private boolean heave_halftoneRectangleDrawActive;
    private boolean heave_zippyDrawActive;
    private boolean heave_outlineDrawActive;
    private boolean heave_outline360DrawActive;
    private boolean heave_imageDrawActive;
    private boolean heave_lineDrawActive;
    private boolean heave_itemDrawActive;
    private boolean heave_rippleDrawActive;
    private boolean heave_shimmerDrawActive;

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), require = 0)
    private void heave_beginBlurFrame(CallbackInfo ci) {
        BlurFramebuffer.getInstance().beginGuiFrame();
        GlassRenderer.getInstance().beginGuiFrame();
        ShapeRenderer.getInstance().beginGuiFrame();
        GlowRenderer.getInstance().beginGuiFrame();
        GlassOutlineRenderer.getInstance().beginGuiFrame();
        CircleRenderer.getInstance().beginGuiFrame();
        ArcRenderer.getInstance().beginGuiFrame();
        RadialGlassRenderer.getInstance().beginGuiFrame();
        SectorMaskRenderer.getInstance().beginGuiFrame();
        PickerRenderer.getInstance().beginGuiFrame();
        DefaultRectangleRenderer.getInstance().beginGuiFrame();
        HalfIconRectangleRenderer.getInstance().beginGuiFrame();
        HalftoneRectangleRenderer.getInstance().beginGuiFrame();
        ZippyRenderer.getInstance().beginGuiFrame();
        DefaultOutlineRenderer.getInstance().beginGuiFrame();
        Outline360Renderer.getInstance().beginGuiFrame();
        ImageRenderer.getInstance().beginGuiFrame();
        LineRenderer.getInstance().beginGuiFrame();
        RippleRenderer.getInstance().beginGuiFrame();
        ShimmerRenderer.getInstance().beginGuiFrame();
        RenderItem.beginGuiFrame();
    }

    @Inject(method = "prepare()V", at = @At("HEAD"), require = 0)
    private void heave_preparePendingBlurResources(CallbackInfo ci) {
        ClientSplits.update();
        BlurFramebuffer.getInstance().preparePending();
        GlowRenderer.getInstance().preparePending();
    }

    @Inject(method = "prepare()V", at = @At("RETURN"), require = 0)
    private void heave_prepareRenderUniforms(CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareBuffers();
        GlassRenderer.getInstance().prepareBuffers();
        ShapeRenderer.getInstance().prepareBuffers();
        GlowRenderer.getInstance().prepareBuffers();
        GlassOutlineRenderer.getInstance().prepareBuffers();
        CircleRenderer.getInstance().prepareBuffers();
        ArcRenderer.getInstance().prepareBuffers();
        RadialGlassRenderer.getInstance().prepareBuffers();
        SectorMaskRenderer.getInstance().prepareBuffers();
        PickerRenderer.getInstance().prepareBuffers();
        DefaultRectangleRenderer.getInstance().prepareBuffers();
        HalfIconRectangleRenderer.getInstance().prepareBuffers();
        HalftoneRectangleRenderer.getInstance().prepareBuffers();
        ZippyRenderer.getInstance().prepareBuffers();
        DefaultOutlineRenderer.getInstance().prepareBuffers();
        Outline360Renderer.getInstance().prepareBuffers();
        ImageRenderer.getInstance().prepareBuffers();
        LineRenderer.getInstance().prepareBuffers();
        RippleRenderer.getInstance().prepareBuffers();
        ShimmerRenderer.getInstance().prepareBuffers();
        RenderItem.prepareBuffers();
    }

    @Inject(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), require = 0)
    private void heave_prepareBlurCapture(CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareGuiDraw();
        GuiLayerBlurRenderer.beginCapture(GuiCapture.active(), false);
    }

    @Inject(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("RETURN"), require = 0)
    private void heave_postPreparedDraws(CallbackInfo ci) {
        if (GuiLayerBlurRenderer.captureActiveThisFrame()) {
            UI.dropPendingBlurs();
            if (GuiCapture.active()) {
                GuiLayerBlurRenderer.composite(GuiCapture.scale(), GuiCapture.blurRadius());
            }
        } else {
            UI.flushMotionBlur();
        }
    }

    @WrapOperation(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"), require = 0)
    private void heave_routePanelRange(GuiRenderer instance, Supplier<String> label, Framebuffer target, GpuBufferSlice fog, GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to, Operation<Void> original) {
        this.heave_routeLocal(instance, label, target, fog, transforms, indices, indexType, from, to, original);
    }

    @Unique
    private void heave_routeLocal(GuiRenderer instance, Supplier<String> label, Framebuffer target, GpuBufferSlice fog, GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to, Operation<Void> original) {
        if (from >= to) {
            return;
        }
        Framebuffer captureTarget = GuiLayerBlurRenderer.captureTarget();
        if (captureTarget == null) {
            int popupBoundary = UI.popupLayerCapturePending() ? this.heave_findBoundary(from, to, true) : -1;
            if (popupBoundary < from) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, from, to});
                return;
            }
            if (from < popupBoundary) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, from, popupBoundary});
            }
            if (UI.consumePopupLayerCapture()) {
                GuiMotionBlurRenderer.captureBackground(0.0f);
            }
            if (popupBoundary + 1 < to) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, popupBoundary + 1, to});
            }
            return;
        }
        int boundary = this.heave_findBoundary(from, to, false);
        if (boundary < from) {
            if (GuiCapture.emitPanelBoundary()) {
                original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, from, to});
            } else {
                this.heave_drawCaptured(instance, label, captureTarget, fog, transforms, indices, indexType, from, to, original);
            }
            return;
        }
        if (from < boundary) {
            this.heave_drawCaptured(instance, label, captureTarget, fog, transforms, indices, indexType, from, boundary, original);
        }
        if (boundary + 1 < to) {
            original.call(new Object[]{instance, label, target, fog, transforms, indices, indexType, boundary + 1, to});
        }
    }

    @Unique
    private void heave_drawCaptured(GuiRenderer instance, Supplier<String> label, Framebuffer captureTarget, GpuBufferSlice fog, GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to, Operation<Void> original) {
        int cursor = from;
        int boundary;
        while (cursor < to && (boundary = this.heave_findBoundary(cursor, to, true)) >= cursor) {
            if (cursor < boundary) {
                original.call(new Object[]{instance, label, captureTarget, fog, transforms, indices, indexType, cursor, boundary});
            }
            BlurFramebuffer.getInstance().recaptureWorldBackdrop();
            cursor = boundary + 1;
        }
        if (cursor < to) {
            original.call(new Object[]{instance, label, captureTarget, fog, transforms, indices, indexType, cursor, to});
        }
    }

    @Unique
    private int heave_findBoundary(int from, int to, boolean popup) {
        int end = Math.min(to, this.draws.size());
        for (int i = Math.max(0, from); i < end; ++i) {
            Object draw = this.draws.get(i);
            if (!(draw instanceof GuiRendererDrawAccessor)) continue;
            GuiRendererDrawAccessor accessor = (GuiRendererDrawAccessor)draw;
            RenderPipeline pipeline = accessor.heave_getPipeline();
            if (!(popup ? GuiLayerBlurRenderer.isPopupBoundary(pipeline) : GuiLayerBlurRenderer.isPanelBoundary(pipeline))) continue;
            return i;
        }
        return -1;
    }

    @Redirect(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V"), require = 0)
    private void heave_cardLayerMidCapture(GameRenderer gameRenderer) {
        if (GuiCapture.active()) {
            GuiLayerBlurRenderer.markPanelRange();
            UI.consumePanelSplitMark();
            UI.consumeCardStratumMark();
            UI.consumePopupStratumMark();
            UI.consumeVanillaBlurRequest();
            return;
        }
        if (UI.consumePanelSplitMark()) {
            UI.applyMainCompositeAtSplit();
            if (UI.consumeVanillaBlurRequest() && !UI.isOpen()) {
                gameRenderer.renderBlur();
            }
            return;
        }
        if (UI.consumePopupStratumMark()) {
            BlurFramebuffer.getInstance().recaptureBackdrop();
            if (UI.consumePopupBlurCapture()) {
                GuiMotionBlurRenderer.captureBackground(0.0f);
            }
            return;
        }
        if (UI.consumeCardStratumMark()) {
            GuiMotionBlurRenderer.captureBackground(0.0f);
            return;
        }
        if (UI.isOpen()) {
            return;
        }
        gameRenderer.renderBlur();
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"), require = 0)
    private void heave_trackPipeline(RenderPass renderPass, RenderPipeline pipeline) {
        this.heave_currentRenderPass = renderPass;
        this.heave_blurDrawActive = BlurFramebuffer.getInstance().isBlurPipeline(pipeline);
        this.heave_glassDrawActive = GlassRenderer.getInstance().isGlassPipeline(pipeline);
        this.heave_shapeDrawActive = ShapeRenderer.getInstance().isShapePipeline(pipeline);
        this.heave_glowDrawActive = GlowRenderer.getInstance().isGlowPipeline(pipeline);
        this.heave_glassOutlineDrawActive = GlassOutlineRenderer.getInstance().isGlassOutlinePipeline(pipeline);
        this.heave_circleDrawActive = CircleRenderer.getInstance().isCirclePipeline(pipeline);
        this.heave_arcDrawActive = ArcRenderer.getInstance().isArcPipeline(pipeline);
        this.heave_radialGlassDrawActive = RadialGlassRenderer.getInstance().isRadialGlassPipeline(pipeline);
        this.heave_sectorMaskDrawActive = SectorMaskRenderer.getInstance().isSectorMaskPipeline(pipeline);
        this.heave_pickerDrawActive = PickerRenderer.getInstance().isPickerPipeline(pipeline);
        this.heave_rectangleDrawActive = DefaultRectangleRenderer.getInstance().isRectanglePipeline(pipeline);
        this.heave_halfIconRectangleDrawActive = HalfIconRectangleRenderer.getInstance().isHalfIconRectanglePipeline(pipeline);
        this.heave_halftoneRectangleDrawActive = HalftoneRectangleRenderer.getInstance().isHalftoneRectanglePipeline(pipeline);
        this.heave_zippyDrawActive = ZippyRenderer.getInstance().isZippyPipeline(pipeline);
        this.heave_outlineDrawActive = DefaultOutlineRenderer.getInstance().isOutlinePipeline(pipeline);
        this.heave_outline360DrawActive = Outline360Renderer.getInstance().isOutline360Pipeline(pipeline);
        this.heave_imageDrawActive = ImageRenderer.getInstance().isImagePipeline(pipeline);
        this.heave_lineDrawActive = LineRenderer.getInstance().isLinePipeline(pipeline);
        this.heave_rippleDrawActive = RippleRenderer.getInstance().isRipplePipeline(pipeline);
        this.heave_shimmerDrawActive = ShimmerRenderer.getInstance().isShimmerPipeline(pipeline);
        this.heave_itemDrawActive = RenderItem.isItemPipeline(pipeline);
        renderPass.setPipeline(pipeline);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", shift = At.Shift.BEFORE), require = 0)
    private void heave_bindBlurParams(CallbackInfo ci) {
        if (this.heave_blurDrawActive && this.heave_currentRenderPass != null) {
            BlurFramebuffer.getInstance().bindBlurParams(this.heave_currentRenderPass);
        }
        if (this.heave_glassDrawActive && this.heave_currentRenderPass != null) {
            GlassRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_shapeDrawActive && this.heave_currentRenderPass != null) {
            ShapeRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_glowDrawActive && this.heave_currentRenderPass != null) {
            GlowRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_glassOutlineDrawActive && this.heave_currentRenderPass != null) {
            GlassOutlineRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_circleDrawActive && this.heave_currentRenderPass != null) {
            CircleRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_arcDrawActive && this.heave_currentRenderPass != null) {
            ArcRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_radialGlassDrawActive && this.heave_currentRenderPass != null) {
            RadialGlassRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_sectorMaskDrawActive && this.heave_currentRenderPass != null) {
            SectorMaskRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_pickerDrawActive && this.heave_currentRenderPass != null) {
            PickerRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_rectangleDrawActive && this.heave_currentRenderPass != null) {
            DefaultRectangleRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_halfIconRectangleDrawActive && this.heave_currentRenderPass != null) {
            HalfIconRectangleRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_halftoneRectangleDrawActive && this.heave_currentRenderPass != null) {
            HalftoneRectangleRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_zippyDrawActive && this.heave_currentRenderPass != null) {
            ZippyRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_outlineDrawActive && this.heave_currentRenderPass != null) {
            DefaultOutlineRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_outline360DrawActive && this.heave_currentRenderPass != null) {
            Outline360Renderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_imageDrawActive && this.heave_currentRenderPass != null) {
            ImageRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_lineDrawActive && this.heave_currentRenderPass != null) {
            LineRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_rippleDrawActive && this.heave_currentRenderPass != null) {
            RippleRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_shimmerDrawActive && this.heave_currentRenderPass != null) {
            ShimmerRenderer.getInstance().bindParams(this.heave_currentRenderPass);
        }
        if (this.heave_itemDrawActive && this.heave_currentRenderPass != null) {
            RenderItem.bindParams(this.heave_currentRenderPass);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At("RETURN"), require = 0)
    private void heave_clearTrackedPipeline(CallbackInfo ci) {
        this.heave_currentRenderPass = null;
        this.heave_blurDrawActive = false;
        this.heave_glassDrawActive = false;
        this.heave_shapeDrawActive = false;
        this.heave_glowDrawActive = false;
        this.heave_glassOutlineDrawActive = false;
        this.heave_circleDrawActive = false;
        this.heave_arcDrawActive = false;
        this.heave_radialGlassDrawActive = false;
        this.heave_sectorMaskDrawActive = false;
        this.heave_pickerDrawActive = false;
        this.heave_rectangleDrawActive = false;
        this.heave_halfIconRectangleDrawActive = false;
        this.heave_halftoneRectangleDrawActive = false;
        this.heave_zippyDrawActive = false;
        this.heave_outlineDrawActive = false;
        this.heave_outline360DrawActive = false;
        this.heave_imageDrawActive = false;
        this.heave_lineDrawActive = false;
        this.heave_itemDrawActive = false;
        this.heave_rippleDrawActive = false;
        this.heave_shimmerDrawActive = false;
    }
}
