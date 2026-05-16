#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 vUv;
out vec4 vColor;
flat out float vRadius;

void main() {
    // Клип-спейс позиция (уже посчитана в Java)
    gl_Position = vec4(Position.xy, 0.0, 1.0);

    vUv = UV0;         // Локальные UV 0.0 - 1.0 для SDF
    vColor = Color;     // Цвет и сила блюра (в альфе)
    vRadius = Position.z; // Радиус закругления
}