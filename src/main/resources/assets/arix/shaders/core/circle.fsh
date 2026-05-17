#version 330

in vec2 vUv;
in vec4 vColor;
flat in float vThickness;
flat in float vStartAngle;
flat in float vEndAngle;

out vec4 fragColor;

const float PI = 3.14159265359;

void main() {
    // vUv is local quad coords (0..1), convert to circle space [-1, 1]
    vec2 p = vUv * 2.0 - 1.0;
    float dist = length(p);

    float outerRadius = 1.0;
    float innerRadius = 1.0 - vThickness;

    // Early discard outside the ring
    float aa = fwidth(dist);
    if (dist > outerRadius + aa || dist < innerRadius - aa) {
        discard;
    }

    // Compute angle in [0, 360)
    float angle = atan(p.y, p.x) * (180.0 / PI);
    if (angle < 0.0) angle += 360.0;

    // Arc check: handle wrap-around (e.g. startAngle=270, endAngle=90)
    bool inArc;
    if (vStartAngle <= vEndAngle) {
        inArc = angle >= vStartAngle && angle <= vEndAngle;
    } else {
        inArc = angle >= vStartAngle || angle <= vEndAngle;
    }

    if (!inArc) {
        discard;
    }

    // Anti-aliasing on outer and inner edges
    float outerAlpha = 1.0 - smoothstep(outerRadius - aa, outerRadius + aa, dist);
    float innerAlpha = smoothstep(innerRadius - aa, innerRadius + aa, dist);
    float alpha = outerAlpha * innerAlpha;

    vec4 color = vColor;
    color.a *= alpha;

    if (color.a < 0.002) {
        discard;
    }

    fragColor = color;
}
