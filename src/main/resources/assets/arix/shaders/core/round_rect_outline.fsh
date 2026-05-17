#version 150

in vec2 vUv;
flat in float vRadius;
flat in float vThicknessNorm;

layout(std140) uniform DynamicTransforms {
    mat4 _ModelViewMat;
    vec4 ShaderColor;
};

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
    float minSide = min(size.x, size.y);

    float radius = clamp(vRadius, 0.0, minSide * 0.5);
    float thickness = clamp(vThicknessNorm * minSide, 0.0, minSide * 0.5);

    vec2 p = vUv * size - size * 0.5;

    float outerDist = sdRoundRect(p, size * 0.5, radius);

    vec2 innerHalf = max(size * 0.5 - vec2(thickness), vec2(0.0));
    float innerRadius = max(radius - thickness, 0.0);
    float innerDist = sdRoundRect(p, innerHalf, innerRadius);

    float aa = max(fwidth(outerDist), 0.75);

    float outerAlpha = 1.0 - smoothstep(-aa, aa, outerDist);
    float innerAlpha = 1.0 - smoothstep(-aa, aa, innerDist);

    float border = clamp(outerAlpha - innerAlpha, 0.0, 1.0);

    vec4 color = ShaderColor;
    color.a *= border;

    if (color.a < 0.002) {
        discard;
    }

    fragColor = color;
}