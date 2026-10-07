#version 150

// The Darkness bends the world near her: a slow sinuous warp, colour split along the radius,
// colour drained toward a cold violet-grey, and the edges of sight closing in.

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec2 c = texCoord - 0.5;
    float r = length(c);
    vec2 warp = vec2(sin(texCoord.y * 38.0 + Time * 2.6), cos(texCoord.x * 31.0 + Time * 2.1)) * 0.0045 * Intensity;
    vec2 uv = clamp(texCoord + warp, vec2(0.0), vec2(1.0));
    vec2 split = (r > 0.0001 ? c / r : vec2(0.0)) * r * 0.014 * Intensity;
    vec3 col = vec3(texture(DiffuseSampler, clamp(uv + split, vec2(0.0), vec2(1.0))).r,
                    texture(DiffuseSampler, uv).g,
                    texture(DiffuseSampler, clamp(uv - split, vec2(0.0), vec2(1.0))).b);
    float grey = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(grey) * vec3(0.86, 0.80, 1.06), 0.55 * Intensity);
    col *= 1.0 - 0.45 * Intensity * smoothstep(0.28, 0.78, r);
    fragColor = vec4(col, 1.0);
}
