package rtx.heave.mixin;

import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.render.TextFactoryEvent;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    @ModifyVariable(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String heave_adjustText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        EventBus bus = EventBus.get();
        if (!bus.hasListeners(TextFactoryEvent.class)) {
            return text;
        }
        TextFactoryEvent event = bus.post(new TextFactoryEvent(text));
        return event.getText();
    }
}

