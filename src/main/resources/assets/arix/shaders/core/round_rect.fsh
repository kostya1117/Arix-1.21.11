#version 150

in vec2 vUv;
in vec4 vColor;
flat in float vRadius;

out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + vec2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    float du = abs(dFdx(vUv.x));
    float dv = abs(dFdy(vUv.y));

    if (du < 1.0e-7 || dv < 1.0e-7) {
        discard;
    }

    vec2 size = vec2(1.0 / du, 1.0 / dv);
    float maxRadius = min(size.x, size.y) * 0.5;
    float radius = clamp(vRadius, 0.0, maxRadius);

    vec2 p = vUv * size - size * 0.5;
    float dist = sdRoundRect(p, size * 0.5, radius);

    float aa = max(fwidth(dist), 0.75);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    vec4 color = vColor;
    color.a *= alpha;

    if (color.a < 0.002) {
        discard;
    }

    fragColor = color;
}