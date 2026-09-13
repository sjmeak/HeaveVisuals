package rtx.heave.api.modules.impl.Visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;

public final class ViewModel extends Module {
    private static ViewModel instance;

    private final SeparatorSetting mainHandSep = this.register(new SeparatorSetting("Правая рука"));
    public final SliderSetting mainHandScale = this.register(
        new SliderSetting("Масштаб правой", "Масштаб правой руки.").range(0.2f, 2.0f).increment(0.05f).setValue(1.0f)
    );
    public final SliderSetting mainHandX = this.register(
        new SliderSetting("X правой", "Смещение правой руки по горизонтали.").range(-2.0f, 2.0f).increment(0.05f).setValue(0.0f)
    );
    public final SliderSetting mainHandY = this.register(
        new SliderSetting("Y правой", "Смещение правой руки по вертикали.").range(-2.0f, 2.0f).increment(0.05f).setValue(0.0f)
    );
    public final SliderSetting mainHandZ = this.register(
        new SliderSetting("Z правой", "Смещение правой руки по глубине.").range(-2.0f, 2.0f).increment(0.05f).setValue(0.0f)
    );

    private final SeparatorSetting offHandSep = this.register(new SeparatorSetting("Левая рука"));
    public final SliderSetting offHandScale = this.register(
        new SliderSetting("Масштаб левой", "Масштаб левой руки.").range(0.2f, 2.0f).increment(0.05f).setValue(1.0f)
    );
    public final SliderSetting offHandX = this.register(
        new SliderSetting("X левой", "Смещение левой руки по горизонтали.").range(-2.0f, 2.0f).increment(0.05f).setValue(0.0f)
    );
    public final SliderSetting offHandY = this.register(
        new SliderSetting("Y левой", "Смещение левой руки по вертикали.").range(-2.0f, 2.0f).increment(0.05f).setValue(0.0f)
    );
    public final SliderSetting offHandZ = this.register(
        new SliderSetting("Z левой", "Смещение левой руки по глубине.").range(-2.0f, 2.0f).increment(0.05f).setValue(0.0f)
    );

    public ViewModel() {
        super("ViewModel", "Кастомизация расположения и масштаба рук от первого лица.", Category.VISUALS);
        instance = this;
    }

    public static ViewModel getInstance() {
        ViewModel mod = ModuleManager.get() != null ? ModuleManager.get().get(ViewModel.class) : null;
        return mod != null ? mod : instance;
    }

    public static boolean applyEquip(MatrixStack matrices, Arm arm, float equipProgress) {
        ViewModel mod = getInstance();
        if (mod == null || !mod.isEnabled() || matrices == null) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isMain = (mc.player != null) ? (arm == mc.player.getMainArm()) : (arm == Arm.RIGHT);
        int side = arm == Arm.RIGHT ? 1 : -1;

        float scale = isMain ? mod.mainHandScale.getFloat() : mod.offHandScale.getFloat();
        float offX = isMain ? mod.mainHandX.getFloat() : mod.offHandX.getFloat();
        float offY = isMain ? mod.mainHandY.getFloat() : mod.offHandY.getFloat();
        float offZ = isMain ? mod.mainHandZ.getFloat() : mod.offHandZ.getFloat();

        matrices.translate((float)side * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
        matrices.scale(scale, scale, scale);
        matrices.translate(offX, offY, offZ);
        return true;
    }

    public void resetLayout() {
        mainHandScale.setValue(1.0f);
        mainHandX.setValue(0.0f);
        mainHandY.setValue(0.0f);
        mainHandZ.setValue(0.0f);
        offHandScale.setValue(1.0f);
        offHandX.setValue(0.0f);
        offHandY.setValue(0.0f);
        offHandZ.setValue(0.0f);
    }

    // Compat helpers
    public static float outlineAlpha(Hand hand) {
        return 0.0f;
    }

    public static void beginHandFrame() {
    }

    public static boolean wantsHandMask() {
        return false;
    }

    public static boolean suppressEatAnimation() {
        return false;
    }
}
