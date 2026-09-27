#version 330
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler1;
in vec4 litColor;
in vec3 viewNormal;
in vec3 viewPosition;
in vec2 damage;
in float sphericalDistance;
in float cylindricalDistance;
out vec4 fragColor;
void main() {
    vec3 n = normalize(viewNormal);
    vec3 eye = normalize(-viewPosition);
    // Both the inner curl and outer face are visible; depth resolves the nearest surface.
    if (dot(n, eye) < 0.0) n = -n;
    float edge = pow(1.0 - max(dot(n, eye), 0.0), 3.0);
    vec3 r = reflect(-eye, n);
    vec2 mirrorUV = vec2(.5 + atan(r.x, r.z) / 6.2831853, .5 + asin(clamp(r.y, -1.0, 1.0)) / 3.14159265);
    vec3 reflected = texture(Sampler1, clamp(mirrorUV, vec2(.001), vec2(.999))).rgb;
    // Full mirror material: no transmission. Alpha is only the spawn/collapse envelope.
    vec3 glass = reflected;
    float shine = pow(max(dot(reflect(-eye, n), normalize(vec3(-.4, .7, .6))), 0.0), 70.0);
    glass += vec3(.85, .94, 1.0) * (edge * .18 + shine * .7);
    fragColor = apply_fog(vec4(glass, litColor.a), sphericalDistance, cylindricalDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
