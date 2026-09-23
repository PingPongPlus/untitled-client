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
    // Bilinear Gaussian kernel; shared by all glass buttons, at half resolution.
    vec3 color = texture(Sampler0, texCoord).rgb * 0.227027;
    color += texture(Sampler0, texCoord + stepUV * 1.384615).rgb * 0.316216;
    color += texture(Sampler0, texCoord - stepUV * 1.384615).rgb * 0.316216;
    color += texture(Sampler0, texCoord + stepUV * 3.230769).rgb * 0.070270;
    color += texture(Sampler0, texCoord - stepUV * 3.230769).rgb * 0.070270;
    fragColor = vec4(color, 1.0);
}
