#version 150

layout(std140) uniform TargetLensData {
    float Count;
    float Aspect;
    float Strength;
    float _pad;
    vec4 Lens0;
    vec4 Lens1;
    vec4 Lens2;
    vec4 Lens3;
    vec4 Lens4;
    vec4 Lens5;
    vec4 Lens6;
    vec4 Lens7;
    vec4 Lens8;
    vec4 Lens9;
    vec4 Lens10;
    vec4 Lens11;
    vec4 Lens12;
    vec4 Lens13;
    vec4 Lens14;
    vec4 Lens15;
    vec4 Lens16;
    vec4 Lens17;
};

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

const int MAX_LENSES = 18;
const float PULL = 0.85;
const float CHROMA = 0.35;

void main() {
    vec4 ls[MAX_LENSES] = vec4[MAX_LENSES](
        Lens0, Lens1, Lens2, Lens3, Lens4, Lens5,
        Lens6, Lens7, Lens8, Lens9, Lens10, Lens11,
        Lens12, Lens13, Lens14, Lens15, Lens16, Lens17
    );

    vec2 uv = texCoord;
    float sceneDepth = texture(Sampler1, uv).r;

    int count = int(Count + 0.5);
    vec2 offset = vec2(0.0);

    for (int i = 0; i < MAX_LENSES; i++) {
        if (i >= count) break;
        vec4 c = ls[i];
        if (c.w <= 0.0) continue;

        vec2 d = (uv - c.xy) * vec2(Aspect, 1.0);
        float len = length(d);
        float t = len / c.w;
        if (t >= 1.0 || len < 1e-5) continue;

        if (sceneDepth < c.z - 0.0005) continue;

        float p = t * (1.0 - t) * (1.0 - t) * 6.75;

        vec2 dir = d / len;
        offset -= (dir / vec2(Aspect, 1.0)) * (p * c.w * PULL * Strength);
    }

    if (dot(offset, offset) < 1e-12) {
        fragColor = vec4(texture(Sampler0, uv).rgb, 1.0);
        return;
    }

    vec2 uvR = clamp(uv + offset * (1.0 - CHROMA), vec2(0.0), vec2(1.0));
    vec2 uvG = clamp(uv + offset, vec2(0.0), vec2(1.0));
    vec2 uvB = clamp(uv + offset * (1.0 + CHROMA), vec2(0.0), vec2(1.0));

    fragColor = vec4(texture(Sampler0, uvR).r, texture(Sampler0, uvG).g, texture(Sampler0, uvB).b, 1.0);
}
