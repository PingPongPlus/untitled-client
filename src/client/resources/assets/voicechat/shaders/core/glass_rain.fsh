#version 330
uniform sampler2D Sampler0;
in vec2 localUV;
in vec4 glassData;
out vec4 fragColor;

vec3 hash3(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(.1031, .11369, .13787));
    q += dot(q, q.yxz + 19.19);
    return fract(vec3((q.x + q.y) * q.z, (q.x + q.z) * q.y, (q.y + q.z) * q.x));
}

float merge(float a, float b) {
    float k = .12;
    float h = max(k - abs(a - b), 0.0) / k;
    return max(a, b) + h * h * k * .25 * min(1.0, (a + b) * 10.0);
}

// The path is anchored to the glass, so the head follows its own lingering wake.
float rivuletPath(float y, vec3 r, vec3 lane) {
    float bend = .015 + .042 * r.y;
    return (r.x - .5) * .26
        + bend * sin(y * (11.0 + lane.y * 17.0) + r.z * 31.0)
        + bend * .35 * sin(y * 67.0 + r.x * 19.0);
}

vec2 movingDrop(vec2 p, vec2 cell, vec2 uv, vec3 lane, float seed) {
    vec3 r = hash3(cell + seed);
    vec3 shape = hash3(cell + seed + 81.7);
    float present = step(.23, r.z);
    vec2 q = p - cell - vec2(.5, .33 + .34 * shape.z);
    q.x -= rivuletPath(uv.y, r, lane);
    // Small rounded drops, broad asymmetric beads, and stretched runners have distinct profiles.
    float runner = smoothstep(.38, .85, lane.x);
    float radius = mix(.036, .19, pow(r.y, 1.7)) * (1.0 + runner * .35);
    float aspect = mix(.24, .67, shape.x) + runner * .20;
    vec2 head = q / vec2(radius, radius * aspect);
    head.x += .13 * sin(head.y * 2.4 + shape.y * 6.28) * smoothstep(0.0, 1.0, abs(head.y));
    head.x *= 1.0 + max(-head.y, 0.0) * mix(.05, .70, shape.y);
    float oval = dot(head, head);
    float dome = sqrt(max(0.0, 1.0 - oval));
    // Irregular shoulders make large drops bulge and coalesce instead of repeating an oval stamp.
    vec2 shoulder = (q - vec2(radius * .35, -radius * aspect * .34))
            / vec2(radius * .63, radius * aspect * .72);
    float lobe = sqrt(max(0.0, 1.0 - dot(shoulder, shoulder))) * smoothstep(.35, .8, shape.y) * .8;
    dome = merge(dome, lobe);

    float length = mix(.16, .96, runner * .65 + shape.z * .35);
    float age = clamp(-q.y / length, 0.0, 1.0);
    float wake = smoothstep(-length, -length * .65, q.y) * (1.0 - smoothstep(-.005, .045, q.y));
    float neck = mix(.009, .032, shape.x) + runner * .012;
    float width = neck * mix(.32, 1.0, 1.0 - age);
    width *= .78 + .22 * sin(uv.y * 108.0 + r.y * 30.0);
    float crossSection = sqrt(max(0.0, 1.0 - pow(q.x / width, 2.0)));
    float trail = crossSection * wake * mix(.24, .47, runner);
    // Tiny pearls left along older sections break up the otherwise continuous wet filament.
    float pearlPhase = fract(q.y * (13.0 + shape.y * 10.0) + r.x * 5.0) - .5;
    vec2 pearl = vec2(q.x / (neck * 1.65), pearlPhase / .22);
    float pearls = sqrt(max(0.0, 1.0 - dot(pearl, pearl))) * wake * age * .20;
    float cleared = (1.0 - smoothstep(width, width * 3.5, abs(q.x))) * wake;
    return vec2(merge(dome, merge(trail, pearls)) * present, cleared * present);
}

vec2 flowingLayer(vec2 uv, float time, float scale, float seed) {
    vec2 grid = vec2(8.0, 2.4) * scale;
    vec2 p = uv * grid;
    vec3 lane = hash3(vec2(floor(p.x), seed));
    float speed = .08 + .70 * pow(lane.x, 3.0);
    p.y -= time * speed + .018 * sin(time * (1.0 + lane.y) + lane.z * 30.0);
    vec2 cell = floor(p);
    vec2 current = movingDrop(p, cell, uv, lane, seed);
    // Also evaluate the head below this cell: its long trail crosses the cell boundary.
    vec2 below = movingDrop(p, cell + vec2(0.0, 1.0), uv, lane, seed);
    return vec2(merge(current.x, below.x), max(current.y, below.y));
}

float condensation(vec2 uv, float time, float cleared) {
    vec2 grid = vec2(25.0, 18.0);
    vec2 p = uv * grid;
    vec3 r = hash3(floor(p) + 147.0);
    vec2 q = fract(p) - .5 - (r.xy - .5) * .65;
    float growth = smoothstep(.10, .8, .5 + .5 * sin(time * (.07 + .12 * r.y) + r.z * 30.0));
    float radius = mix(.025, .14, pow(r.z, 2.0)) * growth;
    vec2 shape = q / max(vec2(radius, radius * mix(.65, .95, r.x)), vec2(.001));
    return sqrt(max(0.0, 1.0 - dot(shape, shape))) * .32 * growth * (1.0 - cleared);
}

void main() {
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 screenUV = gl_FragCoord.xy / size;
    vec2 uv = vec2(gl_FragCoord.x / size.y, localUV.y);
    float time = localUV.x;
    vec2 nearDrops = flowingLayer(uv, time, 1.0, 7.0);
    vec2 fineDrops = flowingLayer(uv + vec2(2.37, .63), time * .73, 1.71, 31.0);
    float cleared = max(nearDrops.y, fineDrops.y);
    float height = merge(merge(nearDrops.x, fineDrops.x * .65), condensation(uv, time, cleared));
    vec2 slope = vec2(dFdx(height), dFdy(height)) * size.y * .012;
    vec3 normal = normalize(vec3(-slope, 1.0));
    vec2 refracted = clamp(screenUV + normal.xy * .020, .5 / size, 1.0 - .5 / size);
    vec3 scene = texture(Sampler0, refracted).rgb;
    float wet = smoothstep(.008, .12, height);
    float fresnel = pow(1.0 - normal.z, 3.0) * wet;
    float reflection = pow(max(dot(normal, normalize(vec3(-.45, .65, 1.2))), 0.0), 36.0) * wet;
    float lowerCaustic = pow(max(dot(normal, normalize(vec3(.25, -.75, .6))), 0.0), 14.0) * wet;
    scene *= 1.0 - fresnel * .42;
    scene += vec3(.70, .84, .89) * (reflection * .16 + lowerCaustic * .045 + fresnel * .09);
    fragColor = vec4(clamp(scene, 0.0, 1.0), 1.0);
}
