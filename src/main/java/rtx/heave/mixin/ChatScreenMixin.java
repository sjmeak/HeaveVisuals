package rtx.heave.mixin;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.chat.commands.CommandManager;
import rtx.heave.api.chat.commands.suggestion.ClientCommandSuggestions;
import rtx.heave.utils.chat.ChatHistory;

@Mixin(net.minecraft.client.gui.screen.ChatScreen.class)

public abstract class ChatScreenMixin
extends Screen {
    @Shadow
    protected TextFieldWidget chatField;
    @Shadow
    ChatInputSuggestor chatInputSuggestor;
    @Unique
    private static boolean heave_historySeeded;

    @Shadow
    public abstract String method_44054(String var1);

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method="init", at={@At(value="TAIL")}, require = 0)
    private void heave_replaceSuggestions(CallbackInfo ci) {
        CommandManager.get().refreshRuntimeState();
        this.chatInputSuggestor = new ClientCommandSuggestions(this.client, this, this.chatField, this.client.textRenderer, false, false, 1, 10, true, -805306368);
        this.chatInputSuggestor.setCanLeave(false);
        this.chatInputSuggestor.refresh();
        this.heave_seedHistory();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Unique
    private void heave_seedHistory() {
        if (heave_historySeeded) {
            return;
        }
        heave_historySeeded = true;
        if (this.client == null || this.client.inGameHud == null) {
            return;
        }
        ChatHistory.seeding = true;
        try {
            ChatHud chat = this.client.inGameHud.getChatHud();
            for (String line : ChatHistory.entries()) {
                chat.addToMessageHistory(line);
            }
        }
        finally {
            ChatHistory.seeding = false;
        }
    }

    @Inject(method="sendMessage", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void heave_handleClientCommand(String message, boolean addToHistory, CallbackInfo ci) {
        CommandManager.get().refreshRuntimeState();
        String normalized = this.method_44054(message);
        if (!CommandManager.get().isClientCommand(normalized)) {
            return;
        }
        if (addToHistory) {
            this.client.inGameHud.getChatHud().addToMessageHistory(normalized);
        }
        String prefix = CommandManager.get().getPrefix();
        CommandManager.get().executeRaw(normalized.substring(prefix.length()));
        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave_onChatMouseClicked(net.minecraft.client.gui.Click click, boolean doubled, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (rtx.heave.api.drags.DragSystem.get().onChatClick(click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"), require = 0)
    private void heave_onChatRemoved(CallbackInfo ci) {
        rtx.heave.api.drags.DragSystem.get().onChatClosed();
    }
}

