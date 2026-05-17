#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 vUv;
out vec4 vColor;
flat out float vRadius;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vUv = UV0;
    vColor = Color;
    vRadius = Position.z;
}