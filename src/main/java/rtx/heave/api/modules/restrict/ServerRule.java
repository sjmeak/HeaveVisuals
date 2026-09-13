package rtx.heave.api.modules.restrict;
import rtx.heave.api.modules.restrict.Server;

public @interface ServerRule {
    public ServerRule.Mode mode();

    public Server[] servers();


    public static enum Mode {
        ONLY,
        BLOCK,
        HIDE;
    
    }
}

