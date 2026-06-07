#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec3 vPos;
in vec3 vBaseColor;
in float vTime;
in float vAlpha;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;

    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.0 + vec2(13.7, 9.2);
        a *= 0.5;
    }

    return v;
}

void main() {
    vec2 st = vPos.xz * 1.15 + vPos.xy * 0.65 + vPos.yz * 0.55;
    float t = vTime * 0.45;

    vec2 q;
    q.x = fbm(st + vec2(t, 0.0));
    q.y = fbm(st + vec2(0.0, t));

    vec2 r;
    r.x = fbm(st + q + vec2(1.7, 9.2) + t * 0.18);
    r.y = fbm(st + q + vec2(8.3, 2.8) + t * 0.14);

    float f = fbm(st + r * 2.0);

    vec3 base = vBaseColor;

    vec3 c1 = base * 0.85;
    vec3 c2 = clamp(base * vec3(0.75, 1.05, 1.20), 0.0, 1.0);
    vec3 c3 = clamp(base * vec3(1.15, 0.95, 0.85), 0.0, 1.0);
    vec3 c4 = clamp(base * vec3(0.85, 1.10, 1.05), 0.0, 1.0);

    vec3 col = mix(c1, c2, clamp(f * 1.4, 0.0, 1.0));
    col = mix(col, c3, clamp(length(q) * 0.45, 0.0, 1.0));
    col = mix(col, c4, clamp(length(r) * 0.25, 0.0, 1.0));

    float glow = pow(f, 1.8) * 0.45;
    col += base * glow;

    float pulse = 0.92 + 0.08 * sin(vTime * 1.8);
    col *= pulse;

    col = clamp(col, 0.0, 1.0);

    float a = vAlpha * (0.72 + f * 0.22);
    a = clamp(a, 0.0, 1.0);

    if (a <= 0.001) discard;

    fragColor = vec4(col, a) * ColorModulator;
}