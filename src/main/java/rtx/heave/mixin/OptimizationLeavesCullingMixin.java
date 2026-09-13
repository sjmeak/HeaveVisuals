package rtx.heave.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.heave.api.modules.impl.Utils.Optimization;

@Mixin(Block.class)
public abstract class OptimizationLeavesCullingMixin {
    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"), require = 0)
    private static boolean heave_cullLeaves(boolean original, BlockState state, BlockState otherState, Direction side) {
        if (state.getBlock() instanceof LeavesBlock && otherState.getBlock() instanceof LeavesBlock && Optimization.shouldCullLeaves()) {
            return false;
        }
        return original;
    }
}
