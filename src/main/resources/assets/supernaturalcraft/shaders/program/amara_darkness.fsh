#version 150

// The Darkness over sight (v0.42). Six strengths, each 0..1 unless noted:
//   Warp   - the old sinuous bend and colour split, near her;
//   Desat  - colour drained toward a cold violet-grey, except where something burns (bright pixels keep their colour);
//   Chroma - chromatic aberration and a shove when one of her blows lands;
//   Ink    - ink creeping in from the edges in living tendrils, a violet sheen at its front;
//   Wave   - the front of a distortion ring racing out from the centre (-1 = none), WaveStrength its force;
//   Dark   - the dying sun's gutter dimming everything.

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Warp;
uniform float Desat;
uniform float Chroma;
uniform float Ink;
uniform float Dark;
uniform float Wave;
uniform float WaveStrength;
uniform float Time;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i), b = hash(i + vec2(1.0, 0.0)), c = hash(i + vec2(0.0, 1.0)), d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

vec3 sampleAt(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
}

void main() {
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 c = texCoord - 0.5;
    vec2 ca = vec2(c.x * aspect, c.y);
    float r = length(ca);
    vec2 radial = r > 0.0001 ? c / max(length(c), 0.0001) : vec2(0.0);

    vec2 uv = texCoord;
    // The old warp.
    uv += vec2(sin(texCoord.y * 38.0 + Time * 2.6), cos(texCoord.x * 31.0 + Time * 2.1)) * 0.0045 * Warp;
    // The wave: a ring of displacement at its front.
    if (Wave >= 0.0) {
        float d = r - Wave;
        float ring = exp(-d * d * 260.0);
        uv -= radial * ring * 0.03 * WaveStrength;
    }
    // A hit shoves the image a little toward the centre.
    uv -= c * 0.02 * Chroma;

    float split = r * 0.014 * Warp + 0.012 * Chroma + (Wave >= 0.0 ? 0.004 * WaveStrength : 0.0);
    vec2 s = radial * split;
    vec3 col = vec3(sampleAt(uv + s).r, sampleAt(uv).g, sampleAt(uv - s).b);
    if (Chroma > 0.01) {
        // A ghost of the image, offset sideways, for the jolt.
        vec3 ghost = vec3(sampleAt(uv + vec2(0.006, 0.0) * Chroma).r, 0.0, sampleAt(uv - vec2(0.006, 0.0) * Chroma).b);
        col = mix(col, max(col, ghost), 0.35 * Chroma);
    }

    // Drained of colour, but what burns keeps its own: the brighter and more saturated a pixel, the less it loses.
    float grey = dot(col, vec3(0.299, 0.587, 0.114));
    float bright = max(col.r, max(col.g, col.b));
    float keep = smoothstep(0.62, 0.92, bright);
    float drain = clamp(Desat + 0.55 * Warp, 0.0, 1.0) * (1.0 - keep);
    vec3 cold = vec3(grey) * vec3(0.86, 0.82, 1.04);
    col = mix(col, cold, drain);
    col *= 1.0 - 0.18 * drain * (1.0 - grey);
    col *= 1.0 - 0.45 * Warp * smoothstep(0.28, 0.78, r);

    // Ink from the edges: how far in it has come at this point, with living tendrils.
    if (Ink > 0.005) {
        vec2 e = min(texCoord, 1.0 - texCoord);
        float edge = min(e.x * aspect, e.y);
        float n = fbm(texCoord * vec2(aspect, 1.0) * 6.0 + vec2(Time * 0.05, -Time * 0.07));
        float tendril = fbm(texCoord * vec2(aspect, 1.0) * 14.0 - vec2(0.0, Time * 0.12));
        float reach = Ink * 0.32 + (n - 0.5) * 0.12 * Ink + pow(tendril, 3.0) * 0.18 * Ink;
        float m = smoothstep(reach, reach - 0.035, edge);
        float front = smoothstep(reach + 0.02, reach, edge) - m;
        vec3 inkCol = vec3(0.012, 0.006, 0.024) + vec3(0.05, 0.02, 0.10) * n;
        col = mix(col, inkCol, m);
        col += vec3(0.32, 0.16, 0.62) * front * 0.6 * Ink;
    }

    col *= 1.0 - Dark;
    fragColor = vec4(col, 1.0);
}
