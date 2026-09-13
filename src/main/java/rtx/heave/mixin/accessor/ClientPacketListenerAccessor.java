package rtx.heave.mixin.accessor;

import java.util.Set;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPacketListenerAccessor {
    @Accessor("worldKeys")
    public void heave_setLevels(Set<RegistryKey<World>> var1);

    @Accessor("worldProperties")
    public ClientWorld.Properties heave_getLevelData();

    @Accessor("world")
    public void heave_setLevel(ClientWorld var1);

    @Accessor("worldProperties")
    public void heave_setLevelData(ClientWorld.Properties var1);

    @Accessor("chunkLoadDistance")
    public void heave_setServerChunkRadius(int var1);

    @Accessor("simulationDistance")
    public void heave_setServerSimulationDistance(int var1);

    @Accessor("simulationDistance")
    public int heave_getServerSimulationDistance();

    @Accessor("chunkLoadDistance")
    public int heave_getServerChunkRadius();
}
