package rtx.heave.api.ui.theme;
import java.awt.Color;

public enum Theme {
    NIGHT("Ночная", new int[]{991271, 2904932}),
    ETERNITY("Eternity", new int[]{12883696, 14522096}),
    GRASS("Grass", new int[]{10481814, 9892005}),
    REFLECTION("Reflection", new int[]{9934576, 12490480}),
    GRAY("Gray", new int[]{9477795, 8489615}),
    BLOOKER("Blooker", new int[]{15775382, 15780246}),
    PURPLE_LOVE("PurpleLove", new int[]{15767211, 15767190}),
    MARIMBA("Marimba", new int[]{16551309, 0xF77E7E}),
    SKY("Sky", new int[]{9892073, 9883120}),
    BLUE("Blue", new int[]{9873648, 10262256}),
    AURORA("Aurora", new int[]{6222530, 7252223}),
    SAKURA("Sakura", new int[]{16747206, 16765404}),
    SUNSET("Sunset", new int[]{16752494, 16765286}),
    OCEAN("Ocean", new int[]{5032432, 4756975}),
    JADE("Jade", new int[]{6477475, 11006928}),
    RUBY("Ruby", new int[]{16735354, 16747100}),
    FROST("Frost", new int[]{12443902, 10670847}),
    VOLT("Volt", new int[]{14286693, 7143352}),
    CYBERPUNK("Cyberpunk", new int[]{16722902, 58879}),
    ARCTIC("Arctic", new int[]{8250367, 15399935}),
    EMERALD_NIGHT("EmeraldNight", new int[]{3462041, 959908}),
    DEEP_OCEAN("DeepOcean", new int[]{49919, 3003583}),
    NEBULA("Nebula", new int[]{9133302, 15485081}),
    MATRIX("Matrix", new int[]{65382, 3800852}),
    GOLDEN_SAND("GoldenSand", new int[]{14066234, 15909198}),
    GARNET("Garnet", new int[]{12653087, 16741775}),
    NORTHERN_LIGHTS("NorthernLights", new int[]{3073702, 8141549}),
    VOLCANO("Volcano", new int[]{16727296, 16748800}),
    CRIMSON("Crimson", new int[]{14423100, 16739125}),
    AMETHYST("Amethyst", new int[]{0x9966FF, 14239471}),
    MINT("Mint", new int[]{6225840, 9895904}),
    COPPER("Copper", new int[]{12088115, 14711391}),
    LEMON("Lemon", new int[]{16773749, 12124006}),
    PLASMA("Plasma", new int[]{0xFF00CC, 0x3333FF});

    public static final Theme KIMIKO = FROST;

    private final String displayName;
    private final int accent;
    private final int accentBright;
    private final int accentSoft;
    private final int accentFill;
    private final int toggleOn;
    private final int gradientA;
    private final int gradientB;
    private final int[] palette;

    private Theme(String displayName, int[] palette) {
        this.displayName = displayName;
        int n2 = Math.max(1, palette.length);
        int[] nArray = new int[n2];
        for (int n = 0; n < n2; ++n) {
            nArray[n] = palette[n] & 0xFFFFFF;
        }
        this.palette = nArray;
        int n = nArray[0];
        this.accent = n;
        this.accentBright = Theme.lighten(n, 0.65f, 1.15f, 0.1f);
        this.accentSoft = Theme.lighten(n, 0.45f, 1.25f, 0.15f);
        this.accentFill = Theme.darken(n, 1.05f, 0.78f);
        this.toggleOn = Theme.darken(n, 1.1f, 0.55f);
        this.gradientA = n;
        this.gradientB = nArray[n2 - 1];
    }

    private Theme(String displayName, int accent, int accentBright, int accentSoft, int accentFill, int toggleOn) {
        this.displayName = displayName;
        this.accent = accent & 0xFFFFFF;
        this.accentBright = accentBright & 0xFFFFFF;
        this.accentSoft = accentSoft & 0xFFFFFF;
        this.accentFill = accentFill & 0xFFFFFF;
        this.toggleOn = toggleOn & 0xFFFFFF;
        this.gradientA = this.accent;
        this.gradientB = this.accent;
        this.palette = new int[]{this.accent};
    }

    public String displayName() {
        return this.displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int[] shades() {
        return new int[]{this.accent, this.accentBright, this.accentSoft, this.accentFill, this.toggleOn, this.gradientA, this.gradientB};
    }

    public int accentRgb() {
        return this.accent;
    }

    private static float[] toHsb(int n) {
        float[] fArray = new float[3];
        Color.RGBtoHSB(n >> 16 & 0xFF, n >> 8 & 0xFF, n & 0xFF, fArray);
        return fArray;
    }

    public int gradientA() {
        return this.gradientA;
    }

    public int gradientB() {
        return this.gradientB;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    private static int darken(int n, float f, float f2) {
        float[] fArray = Theme.toHsb(n);
        return Color.HSBtoRGB(fArray[0], Theme.clamp01(fArray[1] * f), fArray[2] * f2) & 0xFFFFFF;
    }

    public int[] palette() {
        return this.palette;
    }

    private static int lighten(int n, float f, float f2, float f3) {
        float[] fArray = Theme.toHsb(n);
        return Color.HSBtoRGB(fArray[0], Theme.clamp01(fArray[1] * f), Theme.clamp01(fArray[2] * f2 + f3)) & 0xFFFFFF;
    }

    public int accentSoftRgb() {
        return this.accentSoft;
    }

    public int accentFillRgb() {
        return this.accentFill;
    }

    public int accentBrightRgb() {
        return this.accentBright;
    }

    public int toggleOnRgb() {
        return this.toggleOn;
    }
}
