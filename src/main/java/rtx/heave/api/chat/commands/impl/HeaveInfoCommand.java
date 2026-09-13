package rtx.heave.api.chat.commands.impl;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.heave.api.chat.commands.Command;
import rtx.heave.utils.chat.ChatMessage;

public final class HeaveInfoCommand extends Command {
    public HeaveInfoCommand() {
        super("heave", "Инструкция и информация о HeaveVisuals", "info", "guide", "about");
    }

    @Override
    public void execute(String label, String[] args) {
        ChatMessage.brandmessage("§6=== HeaveVisuals v1.0.0 ===");
        this.logDirect("§f• §eМеню клиента: §bRight Shift §7(Правый Shift)", Formatting.WHITE);
        this.logDirect("§f• §eНастройка HUD: §fОткройте чат (§bT§f) и перетаскивайте любые элементы интерфейса мышью.", Formatting.WHITE);
        this.logDirect("§f• §eКастомные мечи: §fПапка §a.minecraft/CustomHeaveSword §7(поддерживаются все 6 мечей)", Formatting.WHITE);
        this.logDirect("§f• §eАвтор: §bheavefist", Formatting.WHITE);

        MutableText githubLink = Text.literal("§f• §eGitHub: §9§nhttps://github.com/sjmeak§r §7(нажмите, чтобы открыть)")
            .styled(style -> style
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/sjmeak")))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("§aПерейти на GitHub автора heavefist"))));
        
        if (this.mc.player != null) {
            this.mc.player.sendMessage(githubLink, false);
        }
        ChatMessage.brandmessage("§6=======================");
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Показывает руководство по использованию HeaveVisuals.", "Использование:", "> heave", "> info");
    }
}
