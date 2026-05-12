#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out float vTime;
out float vAspect;
out float vAlpha;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);

    texCoord = UV0;
    vTime = Position.z;
    vAspect = (Color.r * 255.0) / 100.0;
    vAlpha = Color.a;
}