#version 150

uniform sampler2D Sampler0; // Текстура экрана (TextureSetup)

in vec2 vUv;
in vec4 vColor;
flat in float vRadius;

out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + vec2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    // 1. Считаем размеры объекта в пикселях через производные
    vec2 size = vec2(1.0 / abs(dFdx(vUv.x)), 1.0 / abs(dFdy(vUv.y)));

    // 2. SDF маска закругления
    vec2 p = vUv * size - size * 0.5;
    float dist = sdRoundRect(p, size * 0.5, vRadius);
    float alphaMask = 1.0 - smoothstep(-0.75, 0.75, dist);

    // Если пиксель вне закругления — отсекаем
    if (alphaMask <= 0.0) discard;

    // 3. Блюр (9-tap Gaussian approximation)
    // Сила блюра берется из альфы (vColor.a * 10.0)
    float strength = vColor.a * 10.0;
    vec2 screenTexSize = textureSize(Sampler0, 0);

    // gl_FragCoord.xy дает текущий пиксель на мониторе
    vec2 screenUv = gl_FragCoord.xy / screenTexSize;
    float offset = strength / screenTexSize.x;

    // 9 выборок для мягкого размытия
    vec4 acc = texture(Sampler0, screenUv) * 0.227027;
    acc += texture(Sampler0, screenUv + vec2(offset * 1.38, offset * 1.38)) * 0.316216;
    acc += texture(Sampler0, screenUv - vec2(offset * 1.38, offset * 1.38)) * 0.316216;
    acc += texture(Sampler0, screenUv + vec2(offset * 3.23, offset * 3.23)) * 0.070270;
    acc += texture(Sampler0, screenUv - vec2(offset * 3.23, offset * 3.23)) * 0.070270;

    // Результат: Блюр-текстура * Цвет-фильтр, Маска * Альфа
    // Примечание: vColor.a здесь используется дважды (как сила и как прозрачность),
    // что идеально подходит для плавного появления.
    fragColor = vec4(acc.rgb * vColor.rgb, alphaMask * vColor.a);
}