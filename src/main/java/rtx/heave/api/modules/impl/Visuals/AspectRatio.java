package rtx.heave.api.modules.impl.Visuals;

import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.ModeSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;

public class AspectRatio extends Module {
    public static final String MODE_16_9 = "16:9";
    public static final String MODE_4_3 = "4:3";
    public static final String MODE_21_9 = "21:9";
    public static final String MODE_1_1 = "1:1";
    public static final String MODE_CUSTOM = "Custom";

    private static AspectRatio instance;

    private final ModeSetting mode = this.register(
        new ModeSetting("Режим", "Режим соотношения сторон экрана.", MODE_16_9, MODE_16_9, MODE_4_3, MODE_21_9, MODE_1_1, MODE_CUSTOM)
    );
    private final SliderSetting customRatio = this.register(
        new SliderSetting("Кастомное соотношение", "Своё значение соотношения сторон.")
            .range(0.5f, 3.0f)
            .increment(0.01f)
            .setValue(1.78f)
            .visible(() -> this.mode.is(MODE_CUSTOM))
    );

    public AspectRatio() {
        super("Aspect Ratio", "Изменяет соотношение сторон экрана и рук.", Category.VISUALS);
        instance = this;
    }

    public static AspectRatio getInstance() {
        return instance;
    }

    public float getRatio() {
        return switch (this.mode.getValue()) {
            case MODE_4_3 -> 4.0f / 3.0f;
            case MODE_21_9 -> 21.0f / 9.0f;
            case MODE_1_1 -> 1.0f;
            case MODE_CUSTOM -> this.customRatio.getFloat();
            default -> 16.0f / 9.0f;
        };
    }

    public static float resolveRatio(float width, float height) {
        AspectRatio ar = instance;
        if (ar == null || !ar.isEnabled() || height <= 0.0f) {
            return width;
        }
        return height * ar.getRatio();
    }

    public static void apply(Matrix4f matrix4f) {
        AspectRatio ar = instance;
        if (ar == null || !ar.isEnabled() || matrix4f == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        float w = mc.getWindow().getFramebufferWidth();
        float h = mc.getWindow().getFramebufferHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float normalRatio = w / h;
        float targetRatio = ar.getRatio();
        if (targetRatio <= 0.001f || !Float.isFinite(normalRatio) || !Float.isFinite(targetRatio)) {
            return;
        }
        matrix4f.m00(matrix4f.m00() * (normalRatio / targetRatio));
    }

    public static Matrix4f copyAdjusted(Matrix4fc matrix4fc) {
        Matrix4f matrix4f = new Matrix4f(matrix4fc);
        apply(matrix4f);
        return matrix4f;
    }
}

