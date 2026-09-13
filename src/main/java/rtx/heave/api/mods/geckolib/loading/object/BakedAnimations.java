package rtx.heave.api.mods.geckolib.loading.object;
import java.util.Map;
import rtx.heave.api.mods.geckolib.cache.animation.Animation;

public record BakedAnimations(Map<String, Animation> animations) {
    public Animation getAnimation(String string) {
        return this.animations.get(string);
    }
}

