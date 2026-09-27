#version 330
#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform sampler2D Sampler2;
out vec2 texCoord;
out vec4 litColor;
out vec3 viewNormal;
out vec3 viewPosition;
out vec2 damage;
out float reflectivity;
out float sphericalDistance;
out float cylindricalDistance;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    viewPosition = view.xyz;
    viewNormal = normalize(mat3(ModelViewMat) * Normal);
    texCoord = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    litColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * sample_lightmap(Sampler2, UV2);
    damage = vec2(UV1.x & 255, UV1.y) / 255.0;
    reflectivity = clamp(float(UV1.x >> 8) / 100.0, 0.0, 1.0);
    sphericalDistance = fog_spherical_distance(Position);
    cylindricalDistance = fog_cylindrical_distance(Position);
}
