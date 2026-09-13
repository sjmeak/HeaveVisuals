package rtx.heave.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftAccessor {
    @Invoker("setWorld")
    public void heave_updateLevelInEngines(ClientWorld var1);

    @Invoker("doAttack")
    public boolean heave_startAttack();

    @Invoker("doItemUse")
    public void heave_startUseItem();

    @Accessor("itemUseCooldown")
    public int heave_getRightClickDelay();

    @Accessor("itemUseCooldown")
    public void heave_setRightClickDelay(int var1);
}
