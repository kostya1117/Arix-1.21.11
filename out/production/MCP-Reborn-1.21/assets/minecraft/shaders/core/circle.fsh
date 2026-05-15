#version 150

in vec2 vUv;
in vec4 vColor;
flat in float vThickness;

uniform float uStartAngle = 0.0;
uniform float uEndAngle = 360.0;

out vec4 fragColor;

const float PI = 3.14159265359;

float atan2(float y, float x) {
    return atan(y, x);
}

void main() {
    // Нормализованные координаты от центра (-1 до 1)
    vec2 p = vUv * 2.0 - 1.0;
    float dist = length(p);
    
    // Внешний радиус = 1.0, внутренний = 1.0 - thickness
    float outerRadius = 1.0;
    float innerRadius = 1.0 - vThickness;
    
    // Проверяем находимся ли мы в кольце
    float alpha = 0.0;
    if (dist <= outerRadius && dist >= innerRadius) {
        // Вычисляем угол точки
        float angle = atan2(p.y, p.x) * 180.0 / PI;
        
        // Нормализуем угол в диапазон [0, 360)
        if (angle < 0.0) {
            angle += 360.0;
        }
        
        float startAngle = uStartAngle;
        float endAngle = uEndAngle;
        
        // Нормализуем стартовый угол
        if (startAngle < 0.0) {
            startAngle += 360.0;
        }
        if (endAngle < 0.0) {
            endAngle += 360.0;
        }
        
        bool inArc = false;
        if (startAngle <= endAngle) {
            inArc = angle >= startAngle && angle <= endAngle;
        } else {
            // Дуга пересекает 0 градусов
            inArc = angle >= startAngle || angle <= endAngle;
        }
        
        if (inArc) {
            // Сглаживание краев
            float aa = fwidth(dist);
            float outerAlpha = 1.0 - smoothstep(outerRadius - aa, outerRadius + aa, dist);
            float innerAlpha = smoothstep(innerRadius - aa, innerRadius + aa, dist);
            alpha = outerAlpha * innerAlpha;
        }
    }

    vec4 color = vColor;
    color.a *= alpha;

    if (color.a < 0.002) {
        discard;
    }

    fragColor = color;
}
