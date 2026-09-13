package rtx.heave.utils.sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SoundManager {
    public static final SoundEvent MOAN1 = SoundManager.sound("moan1");
    public static final SoundEvent MOAN2 = SoundManager.sound("moan2");
    public static final SoundEvent MOAN3 = SoundManager.sound("moan3");
    public static final SoundEvent MOAN4 = SoundManager.sound("moan4");
    public static final SoundEvent CRIME = SoundManager.sound("crime");
    public static final SoundEvent METALLIC = SoundManager.sound("metallic");
    public static final SoundEvent MODULE_ENABLE = SoundManager.sound("module_enable");
    public static final SoundEvent MODULE_DISABLE = SoundManager.sound("module_disable");
    public static final SoundEvent ON = SoundManager.sound("on");
    public static final SoundEvent OFF = SoundManager.sound("off");
    public static final SoundEvent MODULE_ENABLE_TYPE2 = SoundManager.sound("module_enable_type2");
    public static final SoundEvent MODULE_DISABLE_TYPE2 = SoundManager.sound("module_disable_type2");
    public static final SoundEvent SEARCH_TYPING = SoundManager.sound("searchtyping");
    public static final SoundEvent SLIDER = SoundManager.sound("slider");
    public static final SoundEvent SETTINGS_SLIDER = SoundManager.sound("settings_slider");
    public static final SoundEvent SWITCH_CATEGORY = SoundManager.sound("switchcategory");
    public static final SoundEvent NO_SETTINGS = SoundManager.sound("no-settings");
    public static final SoundEvent BUTTON_CLICK = SoundManager.sound("buttonclick");
    public static final SoundEvent UNKNOWN_COMMAND = SoundManager.sound("unknowncommand");
    public static final SoundEvent COMMAND_ERROR = SoundManager.sound("command_error");
    public static final SoundEvent OPEN_GUI = SoundManager.sound("open_gui");
    public static final SoundEvent CLOSE_GUI = SoundManager.sound("close_gui");
    public static final SoundEvent SELECT_CATEGORY = SoundManager.sound("select_category");
    public static final SoundEvent NOTIFICATION = SoundManager.sound("welcome");
    public static final SoundEvent NOTIFICATION_LOW = SoundManager.sound("notification_low");
    public static final SoundEvent PLAYER_PING = SoundManager.sound("player_ping");
    public static final SoundEvent MINE = SoundManager.sound("mine");
    public static final SoundEvent ACCOUNT_SWITCH = SoundManager.sound("accountswitch");
    public static final SoundEvent SWITCH_LEFT = SoundManager.sound("switch_left");
    public static final SoundEvent SWITCH_RIGHT = SoundManager.sound("switch_right");
    public static final SoundEvent SETTINGS_OPEN = SoundManager.sound("settings_open");
    public static final SoundEvent SETTINGS_CLOSE = SoundManager.sound("settings_close");
    public static final SoundEvent GUI_CATEGORY_SELECT = SoundManager.sound("gui_category_select");
    public static final SoundEvent RIBBIT_AMBIENT = SoundManager.sound("entity.ribbit.ambient");
    public static final SoundEvent RIBBIT_STEP = SoundManager.sound("entity.ribbit.step");
    public static final SoundEvent RIBBIT_HURT = SoundManager.sound("entity.ribbit.hurt");
    public static final SoundEvent RIBBIT_DEATH = SoundManager.sound("entity.ribbit.death");
    public static final SoundEvent NOTIFICATION_DIRECT = SoundManager.sound("notif");
    public static final SoundEvent LOW = SoundManager.sound("low");
    public static final SoundEvent PLAYERPING = SoundManager.sound("playerping");
    public static final SoundEvent ON_DIRECT = SoundManager.sound("on1");
    public static final SoundEvent OFF_DIRECT = SoundManager.sound("off1");
    public static final SoundEvent FRAG_EFFECT_ECHO_MAIN = SoundManager.sound("frag_effect_echo_main");
    public static final SoundEvent FRAG_EFFECT_KNOCK_MAIN = SoundManager.sound("frag_effect_knock_main");
    public static final SoundEvent FRAG_EFFECT_PULSE = SoundManager.sound("frag_effect_pulse");
    public static final SoundEvent FRAG_EFFECT_SPARKS_COLLISION = SoundManager.sound("frag_effect_sparks_collision");
    public static final SoundEvent SWITCH_CATEGORY_SELECT = SoundManager.sound("guicategory_select");
    public static final SoundEvent SWITCH_LEFT_DIRECT = SoundManager.sound("switch-left");
    public static final SoundEvent SWITCH_RIGHT_DIRECT = SoundManager.sound("switch-right");
    public static final SoundEvent SETTINGS_OPEN_DIRECT = SoundManager.sound("settings-open");
    public static final SoundEvent SETTINGS_CLOSE_DIRECT = SoundManager.sound("settings-close");
    public static final SoundEvent MODULE_SETTINGS_OPEN = SoundManager.sound("module_settings_open");
    public static final SoundEvent MODULE_SETTINGS_CLOSE = SoundManager.sound("module_settings_close");
    public static final SoundEvent TOGGLE1_ON = SoundManager.sound("toggle1_on");
    public static final SoundEvent TOGGLE1_OFF = SoundManager.sound("toggle1_off");
    public static final SoundEvent RIBBIT_AMBIENT1 = SoundManager.sound("ribbit_ambient1");
    public static final SoundEvent RIBBIT_AMBIENT2 = SoundManager.sound("ribbit_ambient2");
    public static final SoundEvent RIBBIT_AMBIENT3 = SoundManager.sound("ribbit_ambient3");
    public static final SoundEvent RIBBIT_AMBIENT4 = SoundManager.sound("ribbit_ambient4");
    public static final SoundEvent RIBBIT_AMBIENT5 = SoundManager.sound("ribbit_ambient5");
    public static final SoundEvent RIBBIT_STEP1 = SoundManager.sound("ribbit_step1");
    public static final SoundEvent RIBBIT_STEP2 = SoundManager.sound("ribbit_step2");
    public static final SoundEvent RIBBIT_HURT1 = SoundManager.sound("ribbit_hurt1");
    public static final SoundEvent RIBBIT_HURT2 = SoundManager.sound("ribbit_hurt2");
    public static final SoundEvent RIBBIT_HURT3 = SoundManager.sound("ribbit_hurt3");
    public static final SoundEvent RIBBIT_DEATH1 = SoundManager.sound("ribbit_death1");
    public static final SoundEvent RIBBIT_DEATH2 = SoundManager.sound("ribbit_death2");
    public static final SoundEvent HIT_BELL = SoundManager.sound("hit_bell");
    public static final SoundEvent HIT_BONK = SoundManager.sound("hit_bonk");
    public static final SoundEvent HIT_BUBBLE = SoundManager.sound("hit_bubble");
    public static final SoundEvent HIT_POP = SoundManager.sound("hit_pop");
    public static final SoundEvent HIT_UWU = SoundManager.sound("hit_uwu");
    public static final SoundEvent HIT_VK = SoundManager.sound("hit_vk");
    private static final SoundEvent[] ALL_SOUNDS = new SoundEvent[]{MOAN1, MOAN2, MOAN3, MOAN4, CRIME, METALLIC, HIT_BELL, HIT_BONK, HIT_BUBBLE, HIT_POP, HIT_UWU, HIT_VK, MODULE_ENABLE, MODULE_DISABLE, ON, OFF, MODULE_ENABLE_TYPE2, MODULE_DISABLE_TYPE2, SEARCH_TYPING, SLIDER, SETTINGS_SLIDER, SWITCH_CATEGORY, NO_SETTINGS, BUTTON_CLICK, UNKNOWN_COMMAND, COMMAND_ERROR, OPEN_GUI, CLOSE_GUI, SELECT_CATEGORY, NOTIFICATION, NOTIFICATION_LOW, PLAYER_PING, MINE, ACCOUNT_SWITCH, SWITCH_LEFT, SWITCH_RIGHT, SETTINGS_OPEN, SETTINGS_CLOSE, GUI_CATEGORY_SELECT, RIBBIT_AMBIENT, RIBBIT_STEP, RIBBIT_HURT, RIBBIT_DEATH, NOTIFICATION_DIRECT, LOW, PLAYERPING, ON_DIRECT, OFF_DIRECT, FRAG_EFFECT_ECHO_MAIN, FRAG_EFFECT_KNOCK_MAIN, FRAG_EFFECT_PULSE, FRAG_EFFECT_SPARKS_COLLISION, SWITCH_CATEGORY_SELECT, SWITCH_LEFT_DIRECT, SWITCH_RIGHT_DIRECT, SETTINGS_OPEN_DIRECT, SETTINGS_CLOSE_DIRECT, MODULE_SETTINGS_OPEN, MODULE_SETTINGS_CLOSE, TOGGLE1_ON, TOGGLE1_OFF, RIBBIT_AMBIENT1, RIBBIT_AMBIENT2, RIBBIT_AMBIENT3, RIBBIT_AMBIENT4, RIBBIT_AMBIENT5, RIBBIT_STEP1, RIBBIT_STEP2, RIBBIT_HURT1, RIBBIT_HURT2, RIBBIT_HURT3, RIBBIT_DEATH1, RIBBIT_DEATH2};
    private static boolean initialized;

    private SoundManager() {
    }

    private static void register(SoundEvent soundEvent) {
        Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)soundEvent.id(), (Object)soundEvent);
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        for (SoundEvent soundEvent : ALL_SOUNDS) {
            SoundManager.register(soundEvent);
        }
    }

    private static final java.util.Map<String, String> SOUND_PATHS = new java.util.HashMap<>();
    static {
        SOUND_PATHS.put("hit_bell", "assets/heave/sounds/hitsound/bell.ogg");
        SOUND_PATHS.put("hit_bonk", "assets/heave/sounds/hitsound/bonk.ogg");
        SOUND_PATHS.put("hit_bubble", "assets/heave/sounds/hitsound/bubble.ogg");
        SOUND_PATHS.put("hit_pop", "assets/heave/sounds/hitsound/pop.ogg");
        SOUND_PATHS.put("hit_uwu", "assets/heave/sounds/hitsound/uwu.ogg");
        SOUND_PATHS.put("hit_vk", "assets/heave/sounds/hitsound/vk.ogg");
        SOUND_PATHS.put("metallic", "assets/heave/sounds/hit_sounds/metallic.ogg");
        SOUND_PATHS.put("crime", "assets/heave/sounds/hit_sounds/crime.ogg");
        SOUND_PATHS.put("moan1", "assets/heave/sounds/hit_sounds/moan1.ogg");
        SOUND_PATHS.put("moan2", "assets/heave/sounds/hit_sounds/moan2.ogg");
        SOUND_PATHS.put("moan3", "assets/heave/sounds/hit_sounds/moan3.ogg");
        SOUND_PATHS.put("moan4", "assets/heave/sounds/hit_sounds/moan4.ogg");
        SOUND_PATHS.put("toggle1_on", "assets/heave/sounds/module_sounds/toggle1_on.ogg");
        SOUND_PATHS.put("toggle1_off", "assets/heave/sounds/module_sounds/toggle1_off.ogg");
        SOUND_PATHS.put("module_enable", "assets/heave/sounds/module_sounds/on.ogg");
        SOUND_PATHS.put("module_disable", "assets/heave/sounds/module_sounds/off.ogg");
        SOUND_PATHS.put("on", "assets/heave/sounds/module_sounds/on2.ogg");
        SOUND_PATHS.put("off", "assets/heave/sounds/module_sounds/off2.ogg");
        SOUND_PATHS.put("on1", "assets/heave/sounds/module_sounds/on1.ogg");
        SOUND_PATHS.put("off1", "assets/heave/sounds/module_sounds/off1.ogg");
        SOUND_PATHS.put("open_gui", "assets/heave/sounds/clickgui/open-gui.ogg");
        SOUND_PATHS.put("close_gui", "assets/heave/sounds/clickgui/close-gui.ogg");
        SOUND_PATHS.put("select_category", "assets/heave/sounds/clickgui/select-category.ogg");
        SOUND_PATHS.put("slider", "assets/heave/sounds/clickgui/slider.ogg");
        SOUND_PATHS.put("searchtyping", "assets/heave/sounds/clickgui/searchtyping.ogg");
        SOUND_PATHS.put("command_error", "assets/heave/sounds/chat/command-error.ogg");
        SOUND_PATHS.put("buttonclick", "assets/heave/sounds/clickgui/buttonclick.ogg");
        SOUND_PATHS.put("notif", "assets/heave/sounds/other/notification/notif.ogg");
        SOUND_PATHS.put("welcome", "assets/heave/sounds/other/notification/notif.ogg");
        SOUND_PATHS.put("low", "assets/heave/sounds/other/notification/low.ogg");
        SOUND_PATHS.put("playerping", "assets/heave/sounds/other/notification/playerping.ogg");
        SOUND_PATHS.put("player_ping", "assets/heave/sounds/other/notification/playerping.ogg");
    }

    public static void playSound(SoundEvent soundEvent, float f, float f2) {
        playSoundDirect(soundEvent, f, f2);
    }

    public static void playSoundDirect(SoundEvent soundEvent, float f, float f2) {
        if (soundEvent == null) {
            return;
        }
        String path = SOUND_PATHS.get(soundEvent.id().getPath());
        if (path != null) {
            DirectSoundPlayer.play(path, f);
        } else {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null && minecraftClient.getSoundManager() != null) {
                minecraftClient.getSoundManager().play((SoundInstance)PositionedSoundInstance.ui((SoundEvent)soundEvent, (float)f2, (float)f));
            }
        }
    }

    private static SoundEvent sound(String string) {
        return SoundEvent.of((Identifier)Identifier.of((String)"heave", (String)string));
    }
}

