package rtx.heave.api.mods.waveycapes.renderlayers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import rtx.heave.api.mods.waveycapes.WaveyCapesBase;
import rtx.heave.api.mods.waveycapes.WaveyCapesMod;
import rtx.heave.api.mods.waveycapes.compat.PlayerWrapper;

public class CustomCapeRenderLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CustomCapeRenderLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> featureRendererContext) {
        super(featureRendererContext);
    }

    @Override
    public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int n, PlayerEntityRenderState playerEntityRenderState, float f, float f2) {
        PlayerWrapper playerWrapper = new PlayerWrapper(playerEntityRenderState);
        float f3 = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        if (playerWrapper.isPlayerInvisible()) {
            return;
        }
        if (playerWrapper.hasElytraEquipped()) {
            return;
        }
        if (!playerWrapper.isCapeVisible()) {
            return;
        }
        matrixStack.push();
        this.getContextModel().getRootPart().applyTransform(matrixStack);
        this.getContextModel().body.applyTransform(matrixStack);
        if (playerWrapper.hasChestplateEquipped()) {
            matrixStack.translate(0.0f, -0.053125f, 0.06875f);
        }
        if (WaveyCapesBase.INSTANCE == null) {
            WaveyCapesMod.INSTANCE.init();
        }
        if (WaveyCapesBase.INSTANCE != null) {
            WaveyCapesBase.INSTANCE.getRenderer().render(playerWrapper, matrixStack, orderedRenderCommandQueue, n, f3);
        }
        matrixStack.pop();
    }
}
