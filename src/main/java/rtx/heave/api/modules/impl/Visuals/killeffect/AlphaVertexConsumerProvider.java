package rtx.heave.api.modules.impl.Visuals.killeffect;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

public final class AlphaVertexConsumerProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider delegate;
    private final float alpha;

    public AlphaVertexConsumerProvider(VertexConsumerProvider delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        VertexConsumer consumer = this.delegate.getBuffer(layer);
        return new AlphaVertexConsumer(consumer, this.alpha);
    }

    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;
        private final float alpha;

        AlphaVertexConsumer(VertexConsumer parent, float alpha) {
            this.parent = parent;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.parent.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            int newA = Math.round((float) a * this.alpha);
            this.parent.color(r, g, b, newA);
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int newA = Math.round((float) a * this.alpha);
            this.parent.color((newA << 24) | (r << 16) | (g << 8) | b);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.parent.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            this.parent.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            this.parent.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.parent.normal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            this.parent.lineWidth(width);
            return this;
        }
    }
}
