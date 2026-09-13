package rtx.heave.utils.render.post.wetworld;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import rtx.heave.utils.render.others.RenderSampler;

public final class WetWorldRenderer {
    private static final int UNIFORM_SIZE = 256;
    private static final Identifier PIPELINE_ID = Identifier.of("heave", "pipeline/post/wetworld");
    private static final Identifier VERTEX_SHADER = Identifier.of("heave", "post/wetworld/wetworld");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("heave", "post/wetworld/wetworld");

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyTextureView;
    private static int sceneWidth = -1;
    private static int sceneHeight = -1;
    private static boolean disabledAfterError = false;

    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static final Matrix4f INV_VIEW_PROJ = new Matrix4f();

    private WetWorldRenderer() {}

    private static void initPipeline() {
        if (disabledAfterError) return;
        try {
            if (pipeline == null) {
                RenderPipeline.Builder builder = RenderPipeline.builder()
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("WetWorldParams", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withSampler("Sampler1")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false);
                pipeline = RenderPipelines.register(builder.build());
            }
            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < UNIFORM_SIZE) {
                if (uniformBuffer != null && !uniformBuffer.isClosed()) {
                    uniformBuffer.close();
                }
                uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "heave:wet_world_uniforms", 136, (long) UNIFORM_SIZE);
            }
        } catch (Throwable t) {
            disabledAfterError = true;
            pipeline = null;
        }
    }

    private static boolean ensureTargets(int width, int height) {
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) return false;
        if (sceneCopyTexture != null && sceneWidth == width && sceneHeight == height) {
            return true;
        }
        closeTargets();
        try {
            sceneCopyTexture = device.createTexture(() -> "heave:wet_world_scene_copy", 5, TextureFormat.RGBA8, width, height, 1, 1);
            sceneCopyTextureView = device.createTextureView(sceneCopyTexture);
            sceneWidth = width;
            sceneHeight = height;
            return true;
        } catch (Throwable t) {
            closeTargets();
            return false;
        }
    }

    public static void closeTargets() {
        if (sceneCopyTextureView != null) {
            sceneCopyTextureView.close();
            sceneCopyTextureView = null;
        }
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        sceneWidth = -1;
        sceneHeight = -1;
    }

    public static void render(Framebuffer framebuffer, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, float reflectionVal, float amountVal, String quality) {
        if (disabledAfterError || framebuffer == null || camera == null || !camera.isReady()) {
            return;
        }
        if (framebuffer.getColorAttachment() == null || framebuffer.getDepthAttachment() == null) {
            return;
        }
        int w = framebuffer.textureWidth;
        int h = framebuffer.textureHeight;
        if (w <= 0 || h <= 0) return;

        initPipeline();
        if (pipeline == null || uniformBuffer == null || uniformBuffer.isClosed()) return;
        if (!ensureTargets(w, h)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        VIEW_PROJ.set((Matrix4fc) projectionMatrix).mul((Matrix4fc) positionMatrix);
        INV_VIEW_PROJ.set((Matrix4fc) VIEW_PROJ).invert();

        Vec3d camPos = camera.getCameraPos();
        float f = 0.0f;
        int skyColor = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SKY_COLOR_VISUAL, f);
        float skyR = (float)(skyColor >> 16 & 0xFF) / 255.0f;
        float skyG = (float)(skyColor >> 8 & 0xFF) / 255.0f;
        float skyB = (float)(skyColor & 0xFF) / 255.0f;

        float sunAngle = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SUN_ANGLE_VISUAL, f) * ((float)Math.PI / 180.0f);
        float sunDirX = -MathHelper.sin(sunAngle);
        float sunDirY = MathHelper.cos(sunAngle);
        float sunDirZ = 0.0f;

        float steps = "Низкое".equals(quality) ? 14.0f : ("Высокое".equals(quality) ? 40.0f : 24.0f);
        float time = (float)(System.currentTimeMillis() % 3600000L) / 1000.0f;

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buf = stack.calloc(UNIFORM_SIZE);

            // 0: ViewProj (64)
            VIEW_PROJ.get(0, buf);
            // 64: InvViewProj (64)
            INV_VIEW_PROJ.get(64, buf);

            // 128: CamPos (12) + pad0 (4)
            buf.putFloat(128, (float) camPos.x);
            buf.putFloat(132, (float) camPos.y);
            buf.putFloat(136, (float) camPos.z);
            buf.putFloat(140, 0.0f);

            // 144: SkyTint (12) + pad1 (4)
            buf.putFloat(144, skyR);
            buf.putFloat(148, skyG);
            buf.putFloat(152, skyB);
            buf.putFloat(156, 0.0f);

            // 160: SunDir (12) + pad2 (4)
            buf.putFloat(160, sunDirX);
            buf.putFloat(164, sunDirY);
            buf.putFloat(168, sunDirZ);
            buf.putFloat(172, 0.0f);

            // 176: Reflectivity, Wetness, Ripple, Gloss
            buf.putFloat(176, reflectionVal);
            buf.putFloat(180, amountVal);
            buf.putFloat(184, 0.35f);
            buf.putFloat(188, 0.45f);

            // 192: Steps, HitThickness, MaxDistance, UpOnly
            buf.putFloat(192, steps);
            buf.putFloat(196, 0.6f);
            buf.putFloat(200, 24.0f);
            buf.putFloat(204, 1.0f);

            // 208: Time, pads
            buf.putFloat(208, time);
            buf.putFloat(212, 0.0f);
            buf.putFloat(216, 0.0f);
            buf.putFloat(220, 0.0f);

            buf.position(0);
            encoder.writeToBuffer(uniformBuffer.slice(0L, UNIFORM_SIZE), buf);
        }

        // Copy scene color to sceneCopyTexture
        encoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, w, h);

        // Render pass: composite wet world directly to framebuffer color attachment
        try (RenderPass pass = encoder.createRenderPass(() -> "heave:wet_world", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.bindTexture("Sampler0", sceneCopyTextureView, RenderSampler.linear());
            pass.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSampler.nearest());
            pass.setUniform("WetWorldParams", uniformBuffer);
            pass.draw(0, 6);
        }
    }

    public static void clear() {
        closeTargets();
    }
}
