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
in vec2 impactCenter;
in float impactRadius;
flat in int glassPreset;
flat in int rippleEnabled;
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
    vec2 size = vec2(textureSize(Sampler1, 0));
    vec2 uv = gl_FragCoord.xy / size;
    vec2 radial = (uv - impactCenter) * size / size.y;
    float distanceFromImpact = length(radial);
    float ring = (distanceFromImpact - phase * impactRadius * 2.4) / max(impactRadius * 0.16, 0.002);
    float wave = rippleEnabled == 1 ? sin(ring * 3.0) * exp(-ring * ring) * (1.0 - phase) : 0.0;
    if (glassPreset == 7) {
        // Independent ripple overlays the untouched vanilla model; no glass/reflection or skin replacement.
        float alpha = max(wave, 0.0) * damage.x * normalColor.a * 0.8;
        if (alpha < 0.002) discard;
        fragColor = apply_fog(vec4(0.65, 0.90, 1.0, alpha), sphericalDistance, cylindricalDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
        return;
    }
    vec2 direction = radial / max(distanceFromImpact, 0.0001);
    vec2 bend = (n.xy * (3.0 + fresnel * 5.0) + direction * wave * 7.0) * damage.x / size;
    vec2 sampleUV = clamp(uv + bend, vec2(0.001), vec2(0.999));
    float frost = glassPreset == 2 ? 7.0 : 1.5;
    vec3 scene = texture(Sampler1, sampleUV).rgb * 0.5;
    scene += texture(Sampler1, clamp(sampleUV + vec2(frost, 0.0) / size, vec2(0.001), vec2(0.999))).rgb * 0.25;
    scene += texture(Sampler1, clamp(sampleUV - vec2(frost, 0.0) / size, vec2(0.001), vec2(0.999))).rgb * 0.25;
    if (glassPreset == 2) {
        scene = scene * 0.5
            + texture(Sampler1, clamp(sampleUV + vec2(0.0, frost) / size, vec2(0.001), vec2(0.999))).rgb * 0.25
            + texture(Sampler1, clamp(sampleUV - vec2(0.0, frost) / size, vec2(0.001), vec2(0.999))).rgb * 0.25;
    }
    vec3 glass = mix(scene, vec3(0.68, 0.85, 0.94), 0.12);
    if (glassPreset == 1) glass = mix(scene * 0.45, vec3(0.12, 0.15, 0.20), 0.28);
    if (glassPreset == 2) glass = mix(scene, vec3(0.76, 0.90, 0.98), 0.52);
    if (glassPreset == 4) glass = mix(scene * vec3(0.72, 0.86, 1.0), vec3(0.48, 0.28, 0.78), 0.38);
    float highlight = pow(max(dot(reflect(-eye, n), normalize(vec3(-0.4, 0.7, 0.6))), 0.0), 30.0);
    glass += vec3(0.65, 0.88, 1.0) * (fresnel * 0.6 + highlight * 0.45 + max(wave, 0.0) * 0.06);
    // Use the captured scene as a screen-space environment. Reflection follows each face normal;
    // off-screen geometry cannot be reflected without a separate environment render.
    vec3 reflection = reflect(-eye, n);
    vec2 mirrorUV = vec2(0.5 + atan(reflection.x, reflection.z) / 6.2831853,
                         0.5 + asin(clamp(reflection.y, -1.0, 1.0)) / 3.14159265);
    mirrorUV += direction * wave * 0.008;
    vec3 mirror = texture(Sampler1, clamp(mirrorUV, vec2(0.001), vec2(0.999))).rgb;
    mirror += vec3(1.0) * (highlight * 0.3 + fresnel * 0.12);
    // At 100% all faces are reflective, not just grazing edges; no skin or transmission remains at peak hurt.
    vec3 material = mix(glass, mirror, reflectivity);
    material += vec3(0.65, 0.90, 1.0) * max(wave, 0.0) * 0.35;
    float replacement = damage.x * mix(0.94, 1.0, reflectivity);
    vec4 color = vec4(mix(normalColor.rgb, material, replacement), normalColor.a);
    if (glassPreset == 6) {
        // Death melt: reflectivity is forced to 1.0 via UV1, so the statue is pure mirror;
        // it condenses into a falling blob, splashes into a puddle, then the puddle dissolves.
        float melt = damage.y;
        float drip = 0.5 + 0.5 * sin(texCoord.y * 40.0 - melt * 60.0);
        color.rgb += vec3(0.75, 0.92, 1.0) * melt * drip * 0.14;
        color.a *= 1.0 - smoothstep(0.72, 1.0, melt);
    }
    fragColor = apply_fog(color, sphericalDistance, cylindricalDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
