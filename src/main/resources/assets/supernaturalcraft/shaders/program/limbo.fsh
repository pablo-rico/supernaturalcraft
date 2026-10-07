#version 150

// Death's limbo: the world drained of colour, a cold grey with a breath of blue, the edges closing in, a slow grain.
// Grey is how far gone (limbo is all the way; the world of the dead only partly).

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Grey;
uniform float Time;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec3 src = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(src, vec3(0.299, 0.587, 0.114));
    vec3 grey = vec3(l * 0.92, l * 0.95, l * 1.02);
    grey = mix(grey, vec3(0.62, 0.64, 0.68), 0.12);
    vec2 c = texCoord - 0.5;
    float vignette = smoothstep(0.85, 0.25, length(c * vec2(1.1, 1.0)));
    float grain = (hash(floor(texCoord * InSize * 0.5) + floor(Time * 12.0)) - 0.5) * 0.06;
    vec3 out_ = mix(src, grey * mix(0.55, 1.0, vignette) + grain, clamp(Grey, 0.0, 1.0));
    fragColor = vec4(out_, 1.0);
}
