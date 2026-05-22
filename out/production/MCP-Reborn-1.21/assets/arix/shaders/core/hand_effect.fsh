#version 330

in vec2 vUv;
in vec4 vColor;
flat in int vMode;
flat in float vTime;
flat in float vParam1;
flat in float vParam2;

out vec4 fragColor;

const float PI = 3.14159265359;

vec3 hsb2rgb(float h, float s, float b) {
    float hh = mod(h, 1.0) * 6.0;
    int   i  = int(hh);
    float f  = hh - float(i);
    float p  = b * (1.0 - s);
    float q  = b * (1.0 - s * f);
    float t  = b * (1.0 - s * (1.0 - f));
    if      (i == 0) return vec3(b, t, p);
    else if (i == 1) return vec3(q, b, p);
    else if (i == 2) return vec3(p, b, t);
    else if (i == 3) return vec3(p, q, b);
    else if (i == 4) return vec3(t, p, b);
    else             return vec3(b, p, q);
}

// Outline — просто заливка всего силуэта с полной альфой
// (контур будет определён самим stencil-силуэтом)
vec4 modeOutline() {
    return vec4(vColor.rgb, vColor.a);
}

// Rainbow — цвет по времени
vec4 modeRainbow() {
    float hue = mod(vTime * vParam1 * 0.1, 1.0);
    vec3  rgb = hsb2rgb(hue, 1.0, 1.0);
    return vec4(rgb, vColor.a);
}

// Glow — затухание от центра к краям quad (работает со stencil)
vec4 modeGlow() {
    // Расстояние от центра fullscreen quad (0,0 в NDC = центр экрана)
    // vUv идёт 0..1, центр = 0.5
    float dx = abs(vUv.x - 0.5) * 2.0; // 0 в центре, 1 на краях
    float dy = abs(vUv.y - 0.5) * 2.0;
    float d  = max(dx, dy);
    float glow = 1.0 - clamp(d, 0.0, 1.0);
    glow = pow(glow, 0.5); // мягкое затухание
    return vec4(vColor.rgb, vColor.a * glow);
}

// Chroma — переливание по X позиции на экране
vec4 modeChroma() {
    float offset = vUv.x * vParam2;
    float hue    = mod(vTime * vParam1 * 0.1 + offset, 1.0);
    vec3  rgb    = hsb2rgb(hue, 1.0, 1.0);
    return vec4(rgb, vColor.a);
}

// Pulse — пульсирует альфа
vec4 modePulse() {
    float pulse = sin(vTime * vParam1 * PI) * 0.5 + 0.5;
    return vec4(vColor.rgb, vColor.a * pulse);
}

// Ghost — полупрозрачный оверлей
vec4 modeGhost() {
    return vec4(vColor.rgb, vColor.a * vParam1);
}

void main() {
    vec4 result;

    if      (vMode == 0) result = modeOutline();
    else if (vMode == 1) result = modeRainbow();
    else if (vMode == 2) result = modeGlow();
    else if (vMode == 3) result = modeChroma();
    else if (vMode == 4) result = modePulse();
    else if (vMode == 5) result = modeGhost();
    else discard;

    if (result.a < 0.002) discard;

    fragColor = result;
}