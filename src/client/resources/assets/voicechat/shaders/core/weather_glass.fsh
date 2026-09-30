#version 330
uniform sampler2D Sampler0;
in vec2 localUV;
in vec4 weatherData;
flat in float weatherTime;
flat in float logicalHeight;
flat in float shapeKind;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + 1.0), f.x), f.y);
}
float box(vec2 p, vec2 h, float r) {
    vec2 q = abs(p) - h + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
float segment(vec2 p, vec2 a, vec2 b) {
    vec2 v = b - a;
    return length(p - a - v * clamp(dot(p - a, v) / max(dot(v, v), .001), 0.0, 1.0));
}
float smoothUnion(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * .25;
}

// A bead and smaller neighbour coalesce into a convex lens rather than overlapping rings.
float beads(vec2 p, float wet, float t, bool flowing) {
    float cellSize = flowing ? 42.0 : 17.0;
    vec2 grid = floor(p / cellSize);
    float lens = 0.0;
    for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
            vec2 cell = grid + vec2(dx, dy);
            float seed = hash(cell + (flowing ? 84.3 : 7.1));
            float appear = smoothstep(seed * .85, seed * .85 + .12, wet);
            vec2 center = (cell + vec2(.2 + hash(cell + 4.1) * .6, .15 + hash(cell + 8.2) * .7)) * cellSize;
            float radius = flowing ? 2.2 + seed * 2.3 : .75 + pow(seed, 2.0) * 2.6;
            float phase = fract(t / 16.0 + hash(cell + 23.0));
            if (flowing) center.y = (cell.y + phase) * cellSize;
            vec2 d = p - center;
            d.x += sin(d.y * .17 + seed * 9.0) * .16;
            d.y /= flowing ? 1.22 : .85 + seed * .35;
            float sdf = length(d) - radius;
            if (!flowing && seed > .58) {
                float approach = .5 + .5 * cos(t * .392699 + seed * 6.28);
                vec2 second = d - vec2(radius * (1.2 + approach), radius * .25);
                sdf = smoothUnion(sdf, length(second) - radius * .65, .65);
            }
            float cap = pow(max(0.0, 1.0 - pow(clamp((sdf + radius) / radius, 0.0, 1.0), 2.0)), .65);
            if (flowing) {
                float trailX = d.x - sin(d.y * .13 + seed * 6.0) * .35;
                float trail = exp(-pow(trailX / .55, 2.0)) * smoothstep(-22.0, -2.0, d.y) * (1.0 - smoothstep(-2.0, .0, d.y));
                cap = max(cap, trail * .20);
            }
            lens = max(lens, cap * appear);
        }
    }
    return lens;
}

// Dendrites grow from irregular seeds on the closest edge; their centre stays clear.
vec2 crystals(vec2 p, vec2 size, float frozen) {
    vec4 edgeDistances = vec4(p.x, size.x - p.x, p.y, size.y - p.y);
    float depth = min(min(edgeDistances.x, edgeDistances.y), min(edgeDistances.z, edgeDistances.w));
    bool vertical = min(edgeDistances.x, edgeDistances.y) < min(edgeDistances.z, edgeDistances.w);
    float tangent = vertical ? p.y : p.x;
    float edgeSeed = vertical ? (p.x < size.x * .5 ? 31.0 : 63.0) : (p.y < size.y * .5 ? 97.0 : 121.0);
    float irregular = noise(vec2(tangent * .06, edgeSeed)) * .65 + noise(vec2(tangent * .17, edgeSeed)) * .35;
    float reach = (4.0 + irregular * 23.0) * frozen;
    float bloom = (1.0 - smoothstep(reach * .25, reach + 2.0, depth)) * frozen;
    float nearest = 1000.0;
    if (depth < 36.0) {
        float rootCell = floor(tangent / 23.0);
        for (int i = -1; i <= 1; i++) {
            float root = rootCell + float(i);
            float seed = hash(vec2(root, edgeSeed));
            vec2 origin = vec2((root + .15 + seed * .7) * 23.0, 0.0);
            float length = (8.0 + seed * 23.0) * frozen;
            float tilt = (hash(vec2(root, edgeSeed + 1.0)) - .5) * .8;
            vec2 tip = origin + vec2(tilt * length, length);
            vec2 pos = vec2(tangent, depth);
            // Fine, broken growth follows a slightly crooked spine, never a repeated snowflake icon.
            vec2 bend = mix(origin, tip, .52) + vec2((seed - .5) * 2.5, 0);
            nearest = min(nearest, min(segment(pos, origin, bend), segment(pos, bend, tip)));
            for (int j = 1; j <= 4; j++) {
                float irregularBranch = hash(vec2(root + float(j) * 3.7, edgeSeed + 4.0));
                float along = float(j) * .18 + (irregularBranch - .5) * .10;
                vec2 joint = mix(origin, tip, along) + vec2(sin(along * 6.0 + seed) * .8, 0);
                float branch = length * (1.0 - along) * (.2 + irregularBranch * .32);
                for (int side = -1; side <= 1; side += 2) {
                    float direction = float(side);
                    vec2 end = joint + vec2(direction * branch, branch * (.4 + seed * .65));
                    nearest = min(nearest, segment(pos, joint, end));
                    for (int hair = 1; hair <= 2; hair++) {
                        float position = float(hair) * .29 + irregularBranch * .08;
                        vec2 fork = mix(joint, end, position);
                        vec2 needle = fork + vec2(direction * branch * .25, branch * (.32 + position * .15));
                        nearest = min(nearest, segment(pos, fork, needle));
                    }
                }
            }
        }
    }
    float microstructure = .4 + .6 * noise(p * 1.35);
    float veins = (1.0 - smoothstep(.02, .40, nearest)) * frozen * microstructure;
    float grain = noise(p * 1.2) * .5 + noise(p * .3) * .5;
    return vec2(bloom * (.35 + grain * .65), veins);
}

void main() {
    vec2 physical = 1.0 / max(vec2(length(vec2(dFdx(localUV.x), dFdy(localUV.x))),
        length(vec2(dFdx(localUV.y), dFdy(localUV.y)))), vec2(.00001));
    float scale = physical.y / max(logicalHeight, 1.0);
    vec2 size = physical / scale;
    vec2 p = localUV * size;
    float corners = floor(weatherData.b * 255.0 + .5) / 64.0;
    float radius = min(physical.x, physical.y) * .5 * corners;
    if (shapeKind < .5 && logicalHeight > 40.0) {
        // Match the base material's byte-sized height metadata, including tall category panels.
        float baseScale = max(physical.y / max(min(logicalHeight, 255.0), 1.0), 1.0);
        radius = min(radius, 14.0 * baseScale * corners);
    }
    if (shapeKind > .5 && shapeKind < 1.5) radius *= .96;
    if (shapeKind > 1.5) radius = min(min(physical.x, physical.y) * .5, 4.0);
    vec2 silhouette = (localUV - .5) * physical;
    if (shapeKind > 2.5) { silhouette.x -= .22 * silhouette.y; radius = min(min(physical.x, physical.y) * .5, 5.0); }
    float distance = box(silhouette, physical * .5, radius);
    float coverage = 1.0 - smoothstep(-max(fwidth(distance), .75), max(fwidth(distance), .75), distance);
    float wet = weatherData.r, frozen = weatherData.g;
    float lens = 0.0;
    if (wet > .003) lens = max(beads(p, wet, weatherTime, false), beads(p, wet * .85, weatherTime, true));
    vec2 normal = vec2(dFdx(lens), dFdy(lens)) * 2.0;
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy / texSize;
    vec2 bent = clamp(uv + normal * scale * 2.4 / texSize, .5 / texSize, 1.0 - .5 / texSize);
    vec3 transmitted = texture(Sampler0, bent).rgb;
    float highlight = pow(max(dot(normalize(vec3(-normal, .45)), normalize(vec3(-.5, .8, .5))), 0.0), 20.0);
    float beadEdge = smoothstep(.025, .12, lens) * (1.0 - smoothstep(.18, .5, lens));
    vec3 rainColor = transmitted * (.95 - beadEdge * .08) + vec3(.83, .94, 1.0) * (highlight * .32 + beadEdge * .10);
    float rainAlpha = smoothstep(.015, .10, lens) * .42;
    vec2 ice = frozen > .003 ? crystals(p, size, frozen) : vec2(0);
    float condensation = noise(p * .055) * noise(p * .19) * frozen * .025;
    float frostAlpha = clamp(ice.x * .21 + ice.y * .29 + condensation, 0.0, .38);
    vec3 iceColor = mix(transmitted * .85, vec3(.78, .89, .97), .6 + ice.y * .3);
    float alpha = rainAlpha + frostAlpha * (1.0 - rainAlpha);
    vec3 color = (rainColor * rainAlpha * (1.0 - frostAlpha) + iceColor * frostAlpha) / max(alpha, .001);
    if (coverage * alpha < .001) discard;
    fragColor = vec4(clamp(color, 0.0, 1.0), alpha * coverage * weatherData.a);
}
