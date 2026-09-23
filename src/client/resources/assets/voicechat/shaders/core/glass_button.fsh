#version 330
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
in vec2 localUV;
in vec4 glassData;
out vec4 fragColor;

float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    // Recover size in physical pixels, so GUI scale and high-DPI displays stay crisp.
    vec2 size = 1.0 / max(vec2(length(vec2(dFdx(localUV.x), dFdy(localUV.x))),
                                    length(vec2(dFdx(localUV.y), dFdy(localUV.y)))), vec2(0.00001));
    vec2 halfSize = size * 0.5;
    vec2 p = (localUV - 0.5) * size;
    float pixelScale = max(size.y / max(glassData.b * 255.0, 1.0), 1.0);
    float radius = min(min(halfSize.x, halfSize.y) * 0.82, 14.0 * pixelScale);
    float distance = roundedBox(p, halfSize, radius);
    float aa = max(fwidth(distance), 0.75);
    // Evaluate derivatives before divergent discard/return branches at the silhouette.
    vec2 screenNormal = normalize(vec2(dFdx(distance), dFdy(distance)) + vec2(0.0001));
    float coverage = 1.0 - smoothstep(-aa, aa, distance);
    float hover = glassData.r;
    float enabled = glassData.g;
    float opacity = glassData.a;


    // Soft shadow extends beyond the rounded silhouette.
    float shadowDistance = roundedBox(p - vec2(0.0, pixelScale), halfSize, radius);
    float shadow = exp(-max(shadowDistance, 0.0) / (1.5  * pixelScale)) * 0.18;
    if (coverage < 0.001) {
        if (shadow < 0.005) discard;
        fragColor = vec4(0.025, 0.025, 0.025, shadow * opacity);
        return;
    }

    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 screenUV = gl_FragCoord.xy / texSize;
    // Rounded edge normal acts as a small lens, strongest near the rim.
    vec2 q = abs(p) - halfSize + radius;
    vec2 normal = normalize(max(q, vec2(0.0)) * sign(p) + vec2(0.0001));
    if (q.x < 0.0 && q.y < 0.0) {
        normal = q.x > q.y ? vec2(sign(p.x), 0.0) : vec2(0.0, sign(p.y));
    }
    float rim = 1.0 - smoothstep(0.0, max(radius, 1.0), max(-distance, 0.0));
    // UV derivatives carry the proper screen orientation for the active GPU backend.
    vec2 bentUV = screenUV - screenNormal * rim * rim * pixelScale * (2.0 + hover) / texSize;
    vec2 halfTexel = 0.5 / texSize;
    bentUV = clamp(bentUV, halfTexel, 1.0 - halfTexel);
    vec3 clear = texture(Sampler0, bentUV).rgb;
    vec3 frost = texture(Sampler1, bentUV).rgb;
    vec3 color = mix(clear, frost, 0.32 + (1.0 - enabled) * 0.15);
    // Neutral absorption preserves the backdrop without adding a blue color cast.
    color = mix(color, vec3(0.09), mix(0.18, 0.24, step(60.0, glassData.b * 255.0)) - hover * 0.04);
    color += vec3(0.045) * (1.0 - localUV.y) * (0.6 + hover * 0.4);

    float edge = 1.0 - smoothstep(0.0, 1.15 * pixelScale, abs(distance + 0.5 * pixelScale));
    float light = pow(max(dot(normal, normalize(vec2(-0.5, -0.85))), 0.0), 2.0);
    float lowerRim = pow(max(dot(normal, normalize(vec2(0.45, 0.9))), 0.0), 5.0);
    color = mix(color, vec3(0.96), edge * (0.16 + light * 0.52 + lowerRim * 0.20 + hover * 0.12));
    float sheen = exp(-pow((localUV.y - 0.08) / 0.20, 2.0)) * (0.035 + hover * 0.025);
    color += sheen;
    fragColor = vec4(clamp(color, 0.0, 1.0), coverage * opacity);
}
