#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 localUV;
out vec4 weatherData;
flat out float weatherTime;
flat out float logicalHeight;
flat out float shapeKind;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    float timeBits = floor(UV0.x / 2.0);
    float geometry = floor(UV0.y / 2.0);
    localUV = vec2(UV0.x - timeBits * 2.0, UV0.y - geometry * 2.0);
    weatherTime = timeBits / 32.0;
    logicalHeight = floor(geometry / 4.0);
    shapeKind = mod(geometry, 4.0);
    weatherData = vec4(Color.rgb, Color.a * ColorModulator.a);
}
