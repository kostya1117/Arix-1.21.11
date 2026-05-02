#version 150

in vec2 vUv;
in vec4 vPacked;

layout(std140) uniform DynamicTransforms {
    mat4 _ModelViewMat;
    vec4 ShaderColor;
};

out vec4 fragColor;

float sdRoundRectVar(vec2 p, vec2 halfSize, vec4 radii) {
    float r = p.x > 0.0
        ? (p.y > 0.0 ? radii.z : radii.y)
        : (p.y > 0.0 ? radii.w : radii.x);

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

    vec4 radii = vec4(
        vPacked.r * maxRadius,
        vPacked.g * maxRadius,
        vPacked.b * maxRadius,
        vPacked.a * maxRadius
    );

    vec2 p = vUv * size - size * 0.5;
    float dist = sdRoundRectVar(p, size * 0.5, radii);

    float aa = max(fwidth(dist), 0.75);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    vec4 color = ShaderColor;
    color.a *= alpha;

    if (color.a < 0.002) {
        discard;
    }

    fragColor = color;
}