package rtx.heave.api.modules.impl.Visuals.particles;

import net.minecraft.util.Identifier;

public final class ParticleConstants {
    public static final ParticleTexture[] TEXTURES = new ParticleTexture[]{
        new ParticleTexture("Точка", Identifier.of("heave", "textures/features/particles/point.png")),
        new ParticleTexture("Звезда", Identifier.of("heave", "textures/features/particles/star.png")),
        new ParticleTexture("Молния", Identifier.of("heave", "textures/features/particles/lighting.png")),
        new ParticleTexture("Крест", Identifier.of("heave", "textures/features/particles/cross.png")),
        new ParticleTexture("Корона", Identifier.of("heave", "textures/features/particles/crown.png")),
        new ParticleTexture("Сердце", Identifier.of("heave", "textures/features/particles/heart.png")),
        new ParticleTexture("Линия", Identifier.of("heave", "textures/features/particles/line.png")),
        new ParticleTexture("Ромб", Identifier.of("heave", "textures/features/particles/rhombus.png")),
        new ParticleTexture("Доллар", Identifier.of("heave", "textures/features/particles/dollar.png")),
        new ParticleTexture("Снежинка", Identifier.of("heave", "textures/features/particles/snowflake.png")),
        new ParticleTexture("Треугольник", Identifier.of("heave", "textures/features/particles/triangle.png"))
    };

    private ParticleConstants() {}
}
