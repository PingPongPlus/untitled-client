#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;

void main() {
#ifdef HORIZONTAL
    vec2 stepUV = vec2(2.0 / float(textureSize(Sampler0, 0).x), 0.0);
#else
    vec2 stepUV = vec2(0.0, 1.0 / float(textureSize(Sampler0, 0).y));
#endif
    // Contiguous samples produce smooth frost, without repeated silhouettes from sparse taps.
    // Sigma is measured in half-resolution pixels (4.0 = roughly 8 framebuffer pixels).
    const float SIGMA = max(4.0 * BLUR_SCALE, 0.001);
    vec3 color = vec3(0.0);
    float totalWeight = 0.0;
    for (int i = -int(ceil(12.0 * BLUR_SCALE)); i <= int(ceil(12.0 * BLUR_SCALE)); i++) {
        float weight = exp(-float(i * i) / (2.0 * SIGMA * SIGMA));
        color += texture(Sampler0, texCoord + stepUV * float(i)).rgb * weight;
        totalWeight += weight;
    }
    fragColor = vec4(color / totalWeight, 1.0);
}
