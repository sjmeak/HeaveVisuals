package rtx.heave.api.modules.impl.Utils;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvent;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.player.AttackEntityEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.NumberSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.utils.sounds.SoundManager;

import java.util.concurrent.ThreadLocalRandom;

public class HitSound extends Module {
    private final SeparatorSetting soundSeparator = this.register(new SeparatorSetting("Звук"));
    private final ModeSetting soundType = this.register(new ModeSetting("Звук", "Тип звука попадания.", "Колокольчик", "Колокольчик", "Бонк", "Пузырь", "Поп", "Uwu", "VK"));
    private final BooleanSetting onlyCrit = this.register(new BooleanSetting("Только криты", "Воспроизводить звук только при критическом ударе.", false));
    private final NumberSetting volume = this.register(new NumberSetting("Громкость", "Громкость звука.", 1.0, 0.1, 2.0, 0.1));

    public HitSound() {
        super("Hit Sound", "Проигрывает звук при попадании из Rain 1.21.8.", Category.UTILS);
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!this.isEnabled() || !(event.getTarget() instanceof LivingEntity)) {
            return;
        }
        if (this.onlyCrit.getValue() && !this.isCriticalHit()) {
            return;
        }
        this.playSelectedSound();
    }

    private boolean isCriticalHit() {
        if (this.mc.player == null) return false;
        return this.mc.player.fallDistance > 0.0f
                && !this.mc.player.isOnGround()
                && !this.mc.player.isClimbing()
                && !this.mc.player.isTouchingWater()
                && !this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !this.mc.player.hasVehicle()
                && !this.mc.player.isSprinting()
                && this.mc.player.getAttackCooldownProgress(0.5f) > 0.9f;
    }

    private void playSelectedSound() {
        float vol = this.volume.getFloat();
        float pitch = 0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f;
        SoundEvent sound = switch (this.soundType.getSelected()) {
            case "Бонк" -> SoundManager.HIT_BONK;
            case "Пузырь" -> SoundManager.HIT_BUBBLE;
            case "Поп" -> SoundManager.HIT_POP;
            case "Uwu" -> SoundManager.HIT_UWU;
            case "VK" -> SoundManager.HIT_VK;
            default -> SoundManager.HIT_BELL;
        };
        SoundManager.playSound(sound, vol, pitch);
    }
}
