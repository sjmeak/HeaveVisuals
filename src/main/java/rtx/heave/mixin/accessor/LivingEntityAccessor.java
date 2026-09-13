package rtx.heave.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("handSwingTicks")
    public void heave_setSwingTime(int var1);

    @Accessor("handSwingProgress")
    public void heave_setAttackAnim(float var1);

    @Accessor("handSwinging")
    public void heave_setSwinging(boolean var1);

    @Accessor("jumpingCooldown")
    public void heave_setNoJumpDelay(int var1);

    @Accessor("preferredHand")
    public void heave_setSwingingArm(Hand var1);
}
