package rtx.heave.api.modules.impl.Movement;

import net.minecraft.entity.effect.StatusEffects;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;

public final class AutoSprint extends Module {
    public AutoSprint() {
        super("Auto Sprint", "Автоматический бег при движении вперед.", Category.MOVEMENT);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!event.isPre() || !this.isEnabled() || this.mc.player == null) {
            return;
        }
        if (this.shouldSprint()) {
            this.mc.player.setSprinting(true);
        }
    }

    private boolean shouldSprint() {
        if (this.mc.player == null) return false;
        boolean canSprintFood = this.mc.player.getHungerManager().getFoodLevel() > 6 || this.mc.player.getAbilities().allowFlying;
        boolean movingForward = this.mc.player.input != null && this.mc.player.input.hasForwardMovement();

        return movingForward
                && canSprintFood
                && !this.mc.player.isSneaking()
                && !this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !this.mc.player.horizontalCollision
                && !this.mc.player.isUsingItem();
    }
}
