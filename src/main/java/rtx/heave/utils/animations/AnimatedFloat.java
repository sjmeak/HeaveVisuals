package rtx.heave.utils.animations;

import net.minecraft.util.math.MathHelper;

public class AnimatedFloat {
    private boolean animating;
    private long animationStartNanos;
    private double animationDurationNanos;
    private Easing easing;
    private float currentValue;
    private float targetValue;
    private float startValue;
    private float speed;

    public AnimatedFloat(float initialValue, float speed, Easing easing) {
        this.currentValue = initialValue;
        this.targetValue = initialValue;
        this.startValue = initialValue;
        this.speed = Math.max(0.1f, speed);
        this.easing = easing != null ? easing : Easings.EXPO_OUT;
        this.animating = false;
    }

    public AnimatedFloat(float initialValue, float speed) {
        this(initialValue, speed, Easings.EXPO_OUT);
    }

    public void setTarget(float target) {
        if (Math.abs(this.targetValue - target) < 0.0001f) {
            return;
        }
        this.targetValue = target;
        this.startValue = this.currentValue;
        this.animationStartNanos = System.nanoTime();
        float distance = Math.abs(this.targetValue - this.startValue);
        this.animationDurationNanos = (distance / this.speed) * 1_000_000_000.0;
        if (this.animationDurationNanos < 1_000_000.0) {
            this.animationDurationNanos = 1_000_000.0;
        }
        this.animating = true;
    }

    public void animate(float target) {
        setTarget(target);
    }

    public void setValue(float value) {
        this.currentValue = value;
        this.targetValue = value;
        this.startValue = value;
        this.animating = false;
    }

    public float getValue() {
        if (!this.animating) {
            return this.currentValue;
        }
        long now = System.nanoTime();
        double elapsed = (double) (now - this.animationStartNanos);
        double progress = MathHelper.clamp(elapsed / this.animationDurationNanos, 0.0, 1.0);
        float eased = (float) this.easing.ease(progress);
        this.currentValue = this.startValue + (this.targetValue - this.startValue) * eased;
        if (progress >= 1.0) {
            this.currentValue = this.targetValue;
            this.animating = false;
        }
        return this.currentValue;
    }

    public float getTarget() {
        return this.targetValue;
    }

    public boolean isFinished() {
        return !this.animating || Math.abs(this.currentValue - this.targetValue) < 0.001f;
    }

    public void setEasing(Easing easing) {
        this.easing = easing != null ? easing : Easings.EXPO_OUT;
    }
}
