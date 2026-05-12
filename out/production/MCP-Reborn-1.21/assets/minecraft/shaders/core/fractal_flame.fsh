#version 150

in vec2 texCoord;
in float vTime;
in float vAspect;
in float vAlpha;

out vec4 fragColor;

const int ITER = 24;
const float TAU = 6.2831853;
const float STEP = TAU / 64.0;
const float GLOW = 0.005;
const vec3 PAL_SHIFT = vec3(0.0, 2.0943951, 4.1887902);

void rot(inout float s, inout float c, float sd, float cd) {
    float ns = s * cd + c * sd;
    float nc = c * cd - s * sd;
    s = ns;
    c = nc;
}

void rot3(inout vec3 s, inout vec3 c, float sd, float cd) {
    vec3 ns = s * cd + c * sd;
    vec3 nc = c * cd - s * sd;
    s = ns;
    c = nc;
}

void main() {
    vec2 st = texCoord * 2.0 - 1.0;
    st.x *= vAspect;

    vec3 col = vec3(0.0);

    float r = length(st);
    float a = atan(st.y, st.x);
    float baseT = vTime * 0.3;

    float swirl = r * 5.0 - vTime * 0.5;
    float swS = sin(swirl);
    float swC = cos(swirl);

    float s1 = sin(a * 3.0 + baseT);
    float c1 = cos(a * 3.0 + baseT);

    float s2 = sin(a * 2.0 + baseT * 1.3);
    float c2 = cos(a * 2.0 + baseT * 1.3);

    float s3 = sin(baseT * 0.7);
    float c3 = cos(baseT * 0.7);

    float s4 = sin(baseT * 0.5);
    float c4 = cos(baseT * 0.5);

    vec3 palS = sin(PAL_SHIFT);
    vec3 palC = cos(PAL_SHIFT);

    float sd1 = sin(STEP);
    float cd1 = cos(STEP);

    float sd2 = sin(STEP * 1.3);
    float cd2 = cos(STEP * 1.3);

    float sd3 = sin(STEP * 0.7);
    float cd3 = cos(STEP * 0.7);

    float sd4 = sin(STEP * 0.5);
    float cd4 = cos(STEP * 0.5);

    for (int i = 0; i < ITER; i++) {
        vec2 p = vec2(s1, c2) * r;

        p = vec2(
            p.x * swC - p.y * swS,
            p.x * swS + p.y * swC
        );

        float invR2 = 0.5 / (dot(p, p) + 0.001);
        p *= invR2;

        vec2 center = vec2(s3, c4) * 0.3;
        vec2 d = p - center;

        float glowVal = GLOW / (dot(d, d) + GLOW);

        col += glowVal * (0.5 + 0.5 * palS);

        rot(s1, c1, sd1, cd1);
        rot(s2, c2, sd2, cd2);
        rot(s3, c3, sd3, cd3);
        rot(s4, c4, sd4, cd4);
        rot3(palS, palC, sd1, cd1);
    }

    col *= 1.0 / 64.0;

    col *= 3.0;
    col = max(col, vec3(0.0));
    col *= sqrt(col); // вместо pow(col, 1.5)

    col = col / (col + vec3(1.0));
    col = pow(col, vec3(0.4545));

    fragColor = vec4(col, vAlpha);
}