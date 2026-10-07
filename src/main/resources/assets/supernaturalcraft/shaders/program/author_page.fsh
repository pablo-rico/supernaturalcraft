#version 150

// The Author's page: the world drained to ink on paper. Lights become cream paper, darks become ink, a memory of
// colour stays; paper grain, faint ruled lines and a red margin, a few lines of text writing themselves along the
// rules, the edges of the sheet darkening. Whiteness blanks the page: everything goes to paper but the outlines,
// which stay as pencil strokes.

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Intensity;
uniform float Whiteness;
uniform float Time;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec3 src = texture(DiffuseSampler, texCoord).rgb;
    float l = luma(src);
    float lx = luma(texture(DiffuseSampler, texCoord + vec2(oneTexel.x, 0.0)).rgb)
             - luma(texture(DiffuseSampler, texCoord - vec2(oneTexel.x, 0.0)).rgb);
    float ly = luma(texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y)).rgb)
             - luma(texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y)).rgb);
    float edge = clamp(length(vec2(lx, ly)) * 3.5, 0.0, 1.0);

    vec3 paper = vec3(0.953, 0.918, 0.835);
    vec3 ink = vec3(0.10, 0.085, 0.10);
    vec3 page = mix(ink, paper, smoothstep(0.03, 0.85, l));
    page = mix(page, src * vec3(1.0, 0.95, 0.85), 0.18);
    page = mix(page, ink, edge * 0.25);

    vec2 px = texCoord * InSize;
    float grain = hash(floor(px / 2.0) + floor(Time * 12.0)) - 0.5;
    page += grain * 0.06;

    float rowPos = mod(px.y, 28.0);
    float rule = 1.0 - smoothstep(0.0, 1.2, abs(rowPos - 1.0));
    page = mix(page, vec3(0.55, 0.65, 0.82), rule * 0.10);
    float margin = 1.0 - smoothstep(0.0, 1.5, abs(px.x - InSize.x * 0.08));
    page = mix(page, vec3(0.80, 0.30, 0.28), margin * 0.14);

    // Text writing itself: short strokes in the band above each rule, revealed left to right, line by line.
    float line = floor(px.y / 28.0);
    float band = step(17.0, rowPos) * step(rowPos, 25.0);
    float word = step(0.28, hash(vec2(floor(px.x / 9.0), line)));
    float strokes = step(0.52, hash(floor(px / vec2(2.0, 3.0)) + vec2(line * 7.0, 0.0)));
    float width = InSize.x * 1.8;
    float written = step(px.x, mod(Time * 120.0 + hash(vec2(line, 3.0)) * width, width));
    page = mix(page, ink, band * word * strokes * written * 0.12);

    vec2 c = texCoord - 0.5;
    float vig = smoothstep(0.32, 0.85, length(c * vec2(1.0, 0.82)));
    page *= 1.0 - 0.35 * vig;

    vec3 col = mix(src, page, clamp(Intensity, 0.0, 1.0));
    vec3 blank = mix(vec3(0.985, 0.975, 0.955), ink * 1.6, edge * 0.8);
    col = mix(col, blank, clamp(Whiteness, 0.0, 1.0) * 0.96);
    fragColor = vec4(col, 1.0);
}
