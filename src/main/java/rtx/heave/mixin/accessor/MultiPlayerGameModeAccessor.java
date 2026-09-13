package rtx.heave.mixin.accessor;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface MultiPlayerGameModeAccessor {
    @Invoker("syncSelectedSlot")
    public void heave_ensureHasSentCarriedItem();

    @Accessor("breakingBlock")
    public boolean heave_isDestroying();

    @Accessor("currentBreakingPos")
    public BlockPos heave_getDestroyBlockPos();

    @Accessor("currentBreakingProgress")
    public float heave_getDestroyProgress();

    @Accessor("breakingBlock")
    public void heave_setDestroying(boolean var1);

    @Accessor("blockBreakingCooldown")
    public void heave_setDestroyDelay(int var1);

    @Accessor("currentBreakingProgress")
    public void heave_setDestroyProgress(float var1);
}
