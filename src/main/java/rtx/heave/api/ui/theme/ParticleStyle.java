package rtx.heave.api.ui.theme;

import net.minecraft.util.Identifier;

public enum ParticleStyle {
    BLOOM("Bloom", "bloom", Identifier.of("heave", "images/particles/bloom.png")),
    STAR("Star", "star", Identifier.of("heave", "images/particles/star.png")),
    DOLLAR("Dollar", "dollar", Identifier.of("heave", "images/particles/dollar.png")),
    HEART("Heart", "heart", Identifier.of("heave", "images/particles/heart.png")),
    LINE("Line", "line", Identifier.of("heave", "images/particles/line.png")),
    CROSS("Cross", "cross", Identifier.of("heave", "images/particles/cross.png")),
    CROWN("Crown", "crown", Identifier.of("heave", "images/particles/crown.png")),
    HACKER("Hacker", "hacker/0", Identifier.of("heave", "images/particles/hacker/0.png")),
    ROMB("Romb", "rombik", Identifier.of("heave", "images/particles/rombik.png")),
    PUMPKIN("Pumpkin", "pumpkin", Identifier.of("heave", "images/particles/pumpkin.png"));

    private final String displayName;
    private final String textureName;
    private final Identifier texture;

    ParticleStyle(String displayName, String textureName, Identifier texture) {
        this.displayName = displayName;
        this.textureName = textureName;
        this.texture = texture;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String displayName() {
        return this.displayName;
    }

    public String getTextureName() {
        return this.textureName;
    }

    public Identifier texture() {
        return this.texture;
    }
}
