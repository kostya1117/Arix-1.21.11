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
flat out float vThickness;
flat out float vStartAngle;
flat out float vEndAngle;

void main() {
    // Position is already in clip space (we pass clip coords from Java)
    gl_Position = vec4(Position.xy, 0.0, 1.0);

    // UV0 = local quad coordinates (0..1 per vertex)
    vUv = UV0;

    // startDeg and endDeg packed into Color.r and Color.g (0..255 = 0..360 degrees)
    vStartAngle = Color.r * 360.0;
    vEndAngle   = Color.g * 360.0;

    // Real color from RenderSystem.setShaderColor via ColorModulator
    vColor = ColorModulator;

    // Position.z = thicknessNorm (thickness / radius)
    vThickness = Position.z;
}
