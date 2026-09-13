layout(std140) uniform ThemeWaveParams {
    vec4 kThemeData[65];
};

vec2 heaveScreenSize() {
    return max(kThemeData[2].xy, vec2(1.0));
}

float heaveGuiScale() {
    return max(kThemeData[2].w, 0.0001);
}

int heaveClientStopCount() {
    return clamp(int(kThemeData[0].x + 0.5), 1, 6);
}

float heaveClientPhase() {
    return kThemeData[0].y;
}

float heaveClientStyleId() {
    return kThemeData[0].z;
}

float heaveClientSweep() {
    return kThemeData[0].w;
}

float heaveClientPrevStyleId() {
    return kThemeData[1].x;
}

float heaveClientClosed() {
    return clamp(kThemeData[1].y, 0.0, 1.0);
}

float heaveClientScrollPhase() {
    return kThemeData[1].z;
}

bool heaveWaveActive() {
    return kThemeData[1].w > 0.5;
}

vec2 heaveFragXYFromUV(vec2 uv) {
    return uv * heaveScreenSize();
}

vec2 heaveFragXYMapped(vec2 local, vec4 map) {
    return map.xy + local * map.z;
}

vec2 heaveFragXYMappedFlipY(vec2 local, vec4 map) {
    return vec2(map.x + local.x * map.z, map.y - local.y * map.z);
}

float heaveThemeLayerCoverage(int layer, vec2 fragXY) {
    vec4 wave = kThemeData[3 + layer];
    if (wave.w < 0.5) {
        return 0.0;
    }
    if (wave.w > 1.5) {
        return clamp(wave.z, 0.0, 1.0);
    }
    vec2 res = max(kThemeData[2].xy, vec2(1.0));
    float aspect = res.x / res.y;
    vec2 d = (fragXY / res - wave.xy) * vec2(aspect, 1.0);
    float wf = max(kThemeData[2].z, 0.0005);
    return 1.0 - smoothstep(wave.z - wf, wave.z + wf, length(d));
}

vec2 kThemeCovXY = vec2(-1.0e18);
float kThemeCov0 = 0.0;
float kThemeCov1 = 0.0;
float kThemeCov2 = 0.0;
float kThemeCov3 = 0.0;
float kThemeCov4 = 0.0;
float kThemeCov5 = 0.0;

void heaveThemeCoverageCache(vec2 fragXY) {
    if (kThemeCovXY == fragXY) {
        return;
    }
    kThemeCovXY = fragXY;
    kThemeCov0 = heaveThemeLayerCoverage(0, fragXY);
    kThemeCov1 = heaveThemeLayerCoverage(1, fragXY);
    kThemeCov2 = heaveThemeLayerCoverage(2, fragXY);
    kThemeCov3 = heaveThemeLayerCoverage(3, fragXY);
    kThemeCov4 = heaveThemeLayerCoverage(4, fragXY);
    kThemeCov5 = heaveThemeLayerCoverage(5, fragXY);
}

vec3 heaveThemeSlot(int slot, vec2 fragXY) {
    heaveThemeCoverageCache(fragXY);
    vec3 c = kThemeData[9 + slot].rgb;
    c = mix(c, kThemeData[17 + slot].rgb, kThemeCov0);
    c = mix(c, kThemeData[25 + slot].rgb, kThemeCov1);
    c = mix(c, kThemeData[33 + slot].rgb, kThemeCov2);
    c = mix(c, kThemeData[41 + slot].rgb, kThemeCov3);
    c = mix(c, kThemeData[49 + slot].rgb, kThemeCov4);
    c = mix(c, kThemeData[57 + slot].rgb, kThemeCov5);
    return c;
}

vec3 heaveClientPrimary(vec2 fragXY) {
    return heaveThemeSlot(0, fragXY);
}

vec3 heaveClientSecondary(vec2 fragXY) {
    return heaveThemeSlot(1, fragXY);
}

vec3 heaveClientStop(int index, vec2 fragXY) {
    return heaveThemeSlot(2 + clamp(index, 0, 5), fragXY);
}

vec3 heaveClientPaletteColor(float t, vec2 fragXY) {
    int count = heaveClientStopCount();
    if (count <= 1) {
        return heaveClientStop(0, fragXY);
    }
    float f = clamp(t, 0.0, 1.0) * float(count - 1);
    int i = clamp(int(floor(f)), 0, count - 1);
    int j = min(i + 1, count - 1);
    float frac = clamp(f - float(i), 0.0, 1.0);
    frac = frac * frac * (3.0 - 2.0 * frac);
    return mix(heaveClientStop(i, fragXY), heaveClientStop(j, fragXY), frac);
}

vec3 heaveClientPaletteLoop(float t, vec2 fragXY) {
    int count = heaveClientStopCount();
    if (count <= 1) {
        return heaveClientStop(0, fragXY);
    }
    float f = fract(t) * float(count);
    int i1 = clamp(int(floor(f)), 0, count - 1);
    float u = clamp(f - float(i1), 0.0, 1.0);

    int i0 = i1 - 1;
    if (i0 < 0) i0 += count;
    int i2 = i1 + 1;
    if (i2 >= count) i2 -= count;
    int i3 = i1 + 2;
    if (i3 >= count) i3 -= count;

    vec3 c0 = heaveClientStop(i0, fragXY);
    vec3 c1 = heaveClientStop(i1, fragXY);
    vec3 c2 = heaveClientStop(i2, fragXY);
    vec3 c3 = heaveClientStop(i3, fragXY);

    float u2 = u * u;
    float u3 = u2 * u;
    vec3 cyclicCol = 0.5 * ((2.0 * c1)
                      + (-c0 + c2) * u
                      + (2.0 * c0 - 5.0 * c1 + 4.0 * c2 - c3) * u2
                      + (-c0 + 3.0 * c1 - 3.0 * c2 + c3) * u3);

    float tri = 0.5 - 0.5 * cos(6.2831853 * fract(t));
    vec3 mirrorCol = heaveClientPaletteColor(tri, fragXY);

    return clamp(mix(mirrorCol, cyclicCol, heaveClientClosed()), 0.0, 1.0);
}
