#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 vUv;
out vec4 vColor;
flat out int vMode;
flat out float vTime;
flat out float vParam1; // thickness / speed / depth
flat out float vParam2; // speed для chroma offset

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);

    vUv    = UV0;
    vColor = ColorModulator;

    // mode packed в Color.r (0..8 -> 0..255)
    vMode  = int(round(Color.r * 8.0));

    // param1 packed в Color.g
    vParam1 = Color.g;

    // param2 packed в Color.b
    vParam2 = Color.b;

    // time packed через Position.z
    vTime = Position.z;
}