package rtx.heave.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.player.PlayerVelocityStrafeEvent;

@Mixin(net.minecraft.entity.Entity.class)

public abstract class EntityMixin {

    @Redirect(method="updateVelocity", at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;method_18795(Lnet/minecraft/class_243;FF)Lnet/minecraft/class_243;"), require = 0)
    private Vec3d heave_fixMoveRelative(Vec3d movementInput, float speed, float yaw) {
        if (this.heave_isLocalPlayer()) {
            PlayerVelocityStrafeEvent event = EventBus.get().post(new PlayerVelocityStrafeEvent(EntityMixin.heave_computeInputVector(movementInput, speed, yaw), movementInput, speed));
            return event.getVelocity();
        }
        return EntityMixin.heave_computeInputVector(movementInput, speed, yaw);
    }

    @Unique
    private boolean heave_isLocalPlayer() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && (Object)this == player;
    }

    @Unique
    private static Vec3d heave_computeInputVector(Vec3d input, float speed, float yaw) {
        double len = input.lengthSquared();
        if (len < 1.0E-7) {
            return Vec3d.ZERO;
        }
        Vec3d scaled = (len > 1.0 ? input.normalize() : input).multiply((double)speed);
        float sin = MathHelper.sin((double)(yaw * ((float)Math.PI / 180)));
        float cos = MathHelper.cos((double)(yaw * ((float)Math.PI / 180)));
        return new Vec3d(scaled.x * (double)cos - scaled.z * (double)sin, scaled.y, scaled.z * (double)cos + scaled.x * (double)sin);
    }
}

