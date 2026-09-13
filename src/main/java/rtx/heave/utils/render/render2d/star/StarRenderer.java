package rtx.heave.utils.render.render2d.star;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.heave.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.heave.utils.render.render2d.Render2DCoordinateSpace;
import rtx.heave.utils.render.scissor.ScissorUtil;

public final class StarRenderer {
    private static volatile StarRenderer instance;
    public static final RenderPipeline STAR_PIPELINE;
    private DrawContext activeGraphics;

    private StarRenderer() {
    }

    static {
        STAR_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.of("heave", "pipeline/star"))
            .withVertexShader(Identifier.of("heave", "core/star"))
            .withFragmentShader(Identifier.of("heave", "core/star"))
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .build();
    }

    public static StarRenderer getInstance() {
        StarRenderer r = instance;
        if (r != null) return r;
        synchronized (StarRenderer.class) {
            r = instance;
            if (r != null) return r;
            instance = r = new StarRenderer();
            return r;
        }
    }

    public static void closeInstance() {
        instance = null;
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void flush() {
        this.activeGraphics = null;
    }

    public void draw(DrawContext drawContext, float x, float y, float size, int cTL, int cTR, int cBR, int cBL) {
        if (drawContext == null || size <= 0.0f) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor) drawContext).heave_getGuiRenderState().addSimpleElement(
                (SimpleGuiElementRenderState) new StarRenderState(matrix3x2f, x, y, size, cTL, cTR, cBR, cBL, ScissorUtil.current())
            );
        } catch (RuntimeException ignored) {
        }
    }

    public void draw(float x, float y, float size, int cTL, int cTR, int cBR, int cBL) {
        this.draw(this.activeGraphics, x, y, size, cTL, cTR, cBR, cBL);
    }

    public void draw(float x, float y, float size, int color) {
        this.draw(this.activeGraphics, x, y, size, color, color, color, color);
    }
}
