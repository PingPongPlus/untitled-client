#version 330
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
in vec2 texCoord;
in vec4 litColor;
in vec3 viewNormal;
in vec3 viewPosition;
in vec2 damage;
in float reflectivity;
in float sphericalDistance;
in float cylindricalDistance;
out vec4 fragColor;

void main() {
    vec4 skin = texture(Sampler0, texCoord);
    if (skin.a < 0.1) discard;
    vec4 normalColor = skin * litColor * ColorModulator;
    vec3 n = normalize(viewNormal);
    vec3 eye = normalize(-viewPosition);
    float fresnel = pow(1.0 - abs(dot(n, eye)), 3.0);
    float phase = damage.y;
    float wave = sin(viewPosition.y * 13.0 - phase * 24.0) * exp(-phase * 3.0);
    vec2 size = vec2(textureSize(Sampler1, 0));
    vec2 uv = gl_FragCoord.xy / size;
    vec2 bend = (n.xy * (3.0 + fresnel * 5.0) + vec2(wave * 1.8, wave)) * damage.x / size;
    vec2 sampleUV = clamp(uv + bend, vec2(0.001), vec2(0.999));
    vec3 scene = texture(Sampler1, sampleUV).rgb * 0.5;
    scene += texture(Sampler1, clamp(sampleUV + vec2(1.5, 0.0) / size, vec2(0.001), vec2(0.999))).rgb * 0.25;
    scene += texture(Sampler1, clamp(sampleUV - vec2(1.5, 0.0) / size, vec2(0.001), vec2(0.999))).rgb * 0.25;
    vec3 glass = mix(scene, vec3(0.68, 0.85, 0.94), 0.12);
    float highlight = pow(max(dot(reflect(-eye, n), normalize(vec3(-0.4, 0.7, 0.6))), 0.0), 30.0);
    glass += vec3(0.65, 0.88, 1.0) * (fresnel * 0.6 + highlight * 0.45 + max(wave, 0.0) * 0.06);
    // Use the captured scene as a screen-space environment. Reflection follows each face normal;
    // off-screen geometry cannot be reflected without a separate environment render.
    vec3 reflection = reflect(-eye, n);
    vec2 mirrorUV = vec2(0.5 + atan(reflection.x, reflection.z) / 6.2831853,
                         0.5 + asin(clamp(reflection.y, -1.0, 1.0)) / 3.14159265);
    mirrorUV += vec2(wave, -wave) * 0.0015 * (1.0 - reflectivity);
    vec3 mirror = texture(Sampler1, clamp(mirrorUV, vec2(0.001), vec2(0.999))).rgb;
    mirror += vec3(1.0) * (highlight * 0.3 + fresnel * 0.12);
    // At 100% all faces are reflective, not just grazing edges; no skin or transmission remains at peak hurt.
    vec3 material = mix(glass, mirror, reflectivity);
    float replacement = damage.x * mix(0.94, 1.0, reflectivity);
    vec4 color = vec4(mix(normalColor.rgb, material, replacement), normalColor.a);
    fragColor = apply_fog(color, sphericalDistance, cylindricalDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
