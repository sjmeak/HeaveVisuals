package rtx.heave.utils.render.render2d.star;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class StarRenderState implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final float x, y, size;
    private final int cTL, cTR, cBR, cBL;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    StarRenderState(Matrix3x2f matrix3x2f, float x, float y, float size, int cTL, int cTR, int cBR, int cBL, ScreenRect screenRect) {
        this.pose = new Matrix3x2f((Matrix3x2fc) matrix3x2f);
        this.x = x;
        this.y = y;
        this.size = size;
        this.cTL = cTL;
        this.cTR = cTR;
        this.cBR = cBR;
        this.cBL = cBL;
        this.scissorArea = screenRect;
        int n = (int) Math.floor(x);
        int n2 = (int) Math.floor(y);
        int n3 = (int) Math.ceil(size);
        ScreenRect screenRect2 = new ScreenRect(n, n2, n3, n3).transformEachVertex((Matrix3x2fc) (Object) this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    @Override
    public ScreenRect bounds() {
        return this.bounds;
    }

    @Override
    public RenderPipeline pipeline() {
        return StarRenderer.STAR_PIPELINE;
    }

    @Override
    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    @Override
    public void setupVertices(VertexConsumer vertexConsumer) {
        float x0 = this.x;
        float y0 = this.y;
        float x1 = this.x + this.size;
        float y1 = this.y + this.size;

        vertexConsumer.vertex((Matrix3x2fc) (Object) this.pose, x0, y0).texture(0.0f, 0.0f).color(this.cTL);
        vertexConsumer.vertex((Matrix3x2fc) (Object) this.pose, x0, y1).texture(0.0f, 1.0f).color(this.cBL);
        vertexConsumer.vertex((Matrix3x2fc) (Object) this.pose, x1, y1).texture(1.0f, 1.0f).color(this.cBR);
        vertexConsumer.vertex((Matrix3x2fc) (Object) this.pose, x1, y0).texture(1.0f, 0.0f).color(this.cTR);
    }
}
