package rtx.heave.utils.render.targetesp;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import rtx.heave.Heave;
import rtx.heave.utils.render.others.RenderSampler;

public final class TargetLensDistortionRenderer {
    private static final Identifier PIPELINE_ID = Identifier.of("heave", "pipeline/post/targetlens/distortion");
    private static final Identifier VERTEX_SHADER = Identifier.of("heave", "post/targetlens/targetlens");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("heave", "post/targetlens/targetlens");
    private static final int UNIFORM_SIZE = 304;

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyView;
    private static int texWidth = -1;
    private static int texHeight = -1;
    private static boolean disabledAfterError;

    public record Lens(float u, float v, float depth, float radius) {}

    private TargetLensDistortionRenderer() {}

    private static boolean ensurePipeline() {
        if (pipeline != null) {
            return true;
        }
        if (disabledAfterError) {
            return false;
        }
        try {
            pipeline = RenderPipelines.register(
                RenderPipeline.builder()
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("TargetLensData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withSampler("Sampler1")
                    .withoutBlend()
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
            );
            return true;
        } catch (Throwable t) {
            disabledAfterError = true;
            pipeline = null;
            Heave.LOGGER.error("[TargetLens] Failed to register lens distortion pipeline", t);
            return false;
        }
    }

    private static void ensureResources(int w, int h) {
        if (sceneCopyTexture != null && w == texWidth && h == texHeight) {
            return;
        }
        closeResources();
        sceneCopyTexture = RenderSystem.getDevice().createTexture(() -> "heave:target_lens_copy", 5, TextureFormat.RGBA8, w, h, 1, 1);
        sceneCopyView = RenderSystem.getDevice().createTextureView(sceneCopyTexture);
        uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "heave:target_lens_uniforms", 136, UNIFORM_SIZE);
        texWidth = w;
        texHeight = h;
    }

    public static void closeResources() {
        if (sceneCopyView != null) {
            sceneCopyView.close();
            sceneCopyView = null;
        }
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        texWidth = -1;
        texHeight = -1;
    }

    public static void projectLens(
        List<Lens> list,
        Matrix4f modelViewProj,
        Matrix4f proj,
        Vec3d targetPos,
        Vec3d camPos,
        float dx, float dy, float dz,
        float radiusScale
    ) {
        Vector4f clip = modelViewProj.transform(new Vector4f(
            (float) (targetPos.x + dx - camPos.x),
            (float) (targetPos.y + dy - camPos.y),
            (float) (targetPos.z + dz - camPos.z),
            1.0f
        ));
        if (clip.w <= 0.05f) {
            return;
        }
        float u = clip.x / clip.w * 0.5f + 0.5f;
        float v = clip.y / clip.w * 0.5f + 0.5f;
        float depth = clip.z / clip.w * 0.5f + 0.5f;
        float radius = Math.min(radiusScale * proj.m11() / clip.w * 0.5f, 0.35f);
        if (u < -radius * 2.0f || u > 1.0f + radius * 2.0f || v < -radius * 2.0f || v > 1.0f + radius * 2.0f) {
            return;
        }
        list.add(new Lens(u, v, depth, radius));
    }

    public static void apply(Framebuffer framebuffer, float aspect, float strength, List<Lens> lenses) {
        if (disabledAfterError || lenses == null || lenses.isEmpty() || strength <= 0.001f) {
            return;
        }
        if (framebuffer == null || framebuffer.getColorAttachment() == null || framebuffer.getDepthAttachment() == null) {
            return;
        }
        int w = framebuffer.textureWidth;
        int h = framebuffer.textureHeight;
        if (w <= 0 || h <= 0 || !ensurePipeline()) {
            return;
        }
        try {
            ensureResources(w, h);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, w, h);

            int count = Math.min(lenses.size(), 18);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer bb = stack.calloc(UNIFORM_SIZE);
                bb.putFloat(0, (float) count);
                bb.putFloat(4, aspect);
                bb.putFloat(8, strength);
                bb.putFloat(12, 0.0f);
                for (int i = 0; i < count; i++) {
                    Lens lens = lenses.get(i);
                    int offset = 16 + i * 16;
                    bb.putFloat(offset, lens.u());
                    bb.putFloat(offset + 4, lens.v());
                    bb.putFloat(offset + 8, lens.depth());
                    bb.putFloat(offset + 12, lens.radius());
                }
                encoder.writeToBuffer(uniformBuffer.slice(0L, UNIFORM_SIZE), bb);
            }

            try (RenderPass pass = encoder.createRenderPass(() -> "heave:target_lens_distortion", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                pass.setPipeline(pipeline);
                pass.setUniform("TargetLensData", uniformBuffer.slice());
                pass.bindTexture("Sampler0", sceneCopyView, RenderSampler.linear());
                pass.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSampler.nearest());
                pass.draw(0, 6);
            }
        } catch (Throwable t) {
            disabledAfterError = true;
            closeResources();
            Heave.LOGGER.error("[TargetLens] Distortion render pass failed, disabling", t);
        }
    }
}
