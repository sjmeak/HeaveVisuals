package rtx.heave.utils.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HeaveKeyBindings {
    public static final KeyBinding.Category HEAVE_CATEGORY = KeyBinding.Category.create(Identifier.of("heave", "heavevisual"));
    public static KeyBinding OPEN_MENU;

    private HeaveKeyBindings() {
    }

    public static void init() {
        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.heave.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            HEAVE_CATEGORY
        ));
    }
}
