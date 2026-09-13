package rtx.heave.api.modules.impl.Visuals;

import net.minecraft.item.Items;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;

public final class TotemCounter extends Module {
    public TotemCounter() {
        super("TotemCounter", "Отображает количество тотемов.", Category.VISUALS);
    }

    public static TotemCounter getInstance() {
        return ModuleManager.get().get(TotemCounter.class);
    }

    private int getTotemCount() {
        if (this.mc.player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < this.mc.player.getInventory().size(); ++i) {
            if (this.mc.player.getInventory().getStack(i).getItem() != Items.TOTEM_OF_UNDYING) continue;
            count += this.mc.player.getInventory().getStack(i).getCount();
        }
        return count;
    }
}
