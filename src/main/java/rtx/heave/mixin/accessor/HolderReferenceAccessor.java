package rtx.heave.mixin.accessor;

import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryEntry.Reference.class)
public interface HolderReferenceAccessor<T> {
    @Invoker("setValue")
    public void heave_bindValue(T var1);
}
