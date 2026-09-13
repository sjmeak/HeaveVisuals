#version 150

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 TexCoord;
in vec4 VertexColor;

out vec4 OutColor;

const float C = 0.85645;
const float RC = 0.86840;
const float SCALE = 0.92;

void main() {
    vec2 p = abs(TexCoord - 0.5) * (2.0 / SCALE);

    // Distance to circular arc: positive inside the star, negative outside
    float dist = length(p - vec2(C)) - RC;

    // Clip outer tips cleanly
    if (p.x > 1.0) dist = min(dist, 1.0 - p.x);
    if (p.y > 1.0) dist = min(dist, 1.0 - p.y);

    // Smooth screen-space anti-aliasing via fwidth
    float delta = fwidth(dist);
    float alpha = clamp(dist / max(delta, 0.001) + 0.5, 0.0, 1.0);

    alpha *= VertexColor.a;
    if (alpha <= 0.001) {
        discard;
    }

    OutColor = vec4(VertexColor.rgb, alpha) * ColorModulator;
}
