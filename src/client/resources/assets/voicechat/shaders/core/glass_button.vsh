#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 localUV;
out vec4 glassData;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    localUV = UV0;
    glassData = vec4(Color.rgb, Color.a * ColorModulator.a);
}
