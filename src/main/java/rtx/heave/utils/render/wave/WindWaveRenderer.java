package rtx.heave.utils.render.wave;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import rtx.heave.Heave;

public final class WindWaveRenderer {
    private static final int UNIFORM_SIZE = 16;
    private static final double TIME_WRAP = 3600.0;
    private static RenderPipeline solidPipeline;
    private static RenderPipeline cutoutPipeline;
    private static GpuBuffer uniformBuffer;
    private static boolean disabledAfterError;
    private static boolean active;
    private static long lastNanos;
    private static double phase;

    private WindWaveRenderer() {
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"heave", (String)string);
    }

    private static void init() {
        if (solidPipeline == null || cutoutPipeline == null) {
            RenderPipeline renderPipeline = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.TERRAIN_SNIPPET}).withLocation(WindWaveRenderer.id("pipeline/wave_solid_terrain")).withVertexShader(WindWaveRenderer.id("core/terrain_wave")).withUniform("WaveParams", UniformType.UNIFORM_BUFFER).build();
            RenderPipeline renderPipeline2 = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[]{RenderPipelines.TERRAIN_SNIPPET}).withLocation(WindWaveRenderer.id("pipeline/wave_cutout_terrain")).withVertexShader(WindWaveRenderer.id("core/terrain_wave")).withUniform("WaveParams", UniformType.UNIFORM_BUFFER).withShaderDefine("ALPHA_CUTOUT", 0.5f).build();
            if (!RenderSystem.getDevice().precompilePipeline(renderPipeline).isValid() || !RenderSystem.getDevice().precompilePipeline(renderPipeline2).isValid()) {
                disabledAfterError = true;
                Heave.LOGGER.warn("WindWave disabled: terrain_wave shader failed to compile");
                return;
            }
            solidPipeline = RenderPipelines.register((RenderPipeline)renderPipeline);
            cutoutPipeline = RenderPipelines.register((RenderPipeline)renderPipeline2);
        }
        if (uniformBuffer == null || uniformBuffer.isClosed()) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "heave_wave_params", 136, 16L);
        }
    }

    public static RenderPipeline substitute(RenderPipeline renderPipeline) {
        if (!active || uniformBuffer == null || uniformBuffer.isClosed()) {
            return renderPipeline;
        }
        if (renderPipeline == RenderPipelines.SOLID_TERRAIN && solidPipeline != null) {
            return solidPipeline;
        }
        if (renderPipeline == RenderPipelines.CUTOUT_TERRAIN && cutoutPipeline != null) {
            return cutoutPipeline;
        }
        return renderPipeline;
    }

    public static void beginFrame() {
        active = false;
        lastNanos = 0L;
    }

    public static void bindParams(RenderPass renderPass) {
        if (uniformBuffer != null && !uniformBuffer.isClosed()) {
            renderPass.setUniform("WaveParams", uniformBuffer);
        }
    }

    private static void closeUniform() {
        if (uniformBuffer != null) {
            try {
                if (!uniformBuffer.isClosed()) {
                    uniformBuffer.close();
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            uniformBuffer = null;
        }
    }
}

