"""The Archangel Michael's sounds (v0.12): trumpets of Heaven, the choir, great wings, the Lance, the smiting touch,
steel feathers, the bell of "I need your yes", the halo shattering, the vessel burning away, the Host, the General's
armour and the Grace's flight. Same conventions as sfx.py: (sr, rng) -> float list, finished and encoded by
generate.py into sounds/michael/. Bright and vast where the Horsemen were dark: brass, choir and bells in a big hall.
"""

import math

from sfx import UH, HUM, bell_voice, burst, choir, sparkle, thud, voiced
from synth import (TAU, add, buf, env_apply, exp_decay, formants, midi_hz, noise, onepole_lp, partials, reverb, reverse, saw,
                   smooth_random, soft_clip, svf, tone)

AH = [(700, 4.0, 1.0), (1150, 5.0, 0.5), (2500, 7.0, 0.2)]
BRASS = [(620, 2.5, 1.0), (1240, 3.0, 0.7), (2400, 4.0, 0.45), (3600, 5.0, 0.2)]


def _env(attack, hold, release):
    def f(t):
        if t < attack:
            return t / attack
        if t < attack + hold:
            return 1.0
        return max(0.0, 1 - (t - attack - hold) / release)
    return f


def hall(x, sr, wet=0.3, tail=1.5):
    return reverb(x, sr, size=0.95, damp=0.3, wet=wet, tail=tail, predelay=0.03)


def whoosh(sr, rng, T, f0, f1, q=1.2, peak=0.45):
    """Air moved fast: band-passed noise sweeping from f0 to f1, swelling to `peak` of the way through."""
    x = svf(noise(int(sr * T), rng), sr, lambda t: f0 * (f1 / f0) ** min(1.0, t / T), q, "band")
    return env_apply(x, sr, lambda t: (t / (T * peak)) ** 2 if t < T * peak else max(0.0, 1 - (t - T * peak) / (T * (1 - peak))) ** 1.5)


def ring(sr, f0, T, bright=1.0):
    """Struck steel: inharmonic partials with beating pairs."""
    spec = []
    for r, a, d in ((1.0, 0.5, 0.9), (1.51, 0.35, 0.6), (2.27, 0.25, 0.4), (3.11, 0.15 * bright, 0.25), (4.42, 0.08 * bright, 0.15)):
        spec += [(f0 * r, a, d), (f0 * r + 3.5, a * 0.6, d * 0.9)]
    return partials(sr, T, spec, attack=0.0006)


def trumpet(sr, rng, midi, T, vib=5.5):
    """One brass voice: a sawtooth that brightens as it is blown, through brass formants, with vibrato coming in late."""
    f = midi_hz(midi)
    s = saw(sr, T, f, rng.uniform(-4, 4), rng.random(), (vib, 0.0))
    late = saw(sr, T, f, rng.uniform(-4, 4), rng.random(), (vib, 14.0))
    x = [a * (1 - min(1.0, i / (sr * T * 0.6))) + b * min(1.0, i / (sr * T * 0.6)) for i, (a, b) in enumerate(zip(s, late))]
    x = svf(x, sr, lambda t: 900 + 3200 * min(1.0, t / 0.12), 0.9, "low")
    x = formants(x, sr, BRASS)
    return env_apply(x, sr, _env(0.04, T * 0.7, T * 0.3 - 0.04))


# --- Michael -----------------------------------------------------------------------------------------------

def ambient(sr, rng, variant):
    """He breathes light: a hushed "oo" with a high shimmer drifting over it."""
    T = 2.6
    x = buf(sr, T)
    c = choir(sr, rng, (62, 69) if variant == 0 else (64, 71), T, "oo", voices=3, detune=7)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / T) ** 2), 0, 1.4, sr)
    add(x, sparkle(sr, rng, 6, 0.4, 2.0, T, 3000, 6500, rise=False), 0, 0.5, sr)
    return hall(x, sr, 0.3, 1.2)


def hurt(sr, rng, variant):
    """A blow on holy armour: a hard metallic ring and a short choral gasp."""
    x = buf(sr, 1.2)
    add(x, burst(sr, rng, 0.03, 2600, 0.8, "high", 0.003), 0, 0.9, sr)
    add(x, ring(sr, 820 + 120 * variant, 1.0), 0.002, 0.5, sr)
    add(x, thud(sr, 140, 70, 0.06, 0.25), 0, 0.5, sr)
    g = voiced(sr, rng, 0.35, lambda t: (230 + 20 * variant) * (0.8 ** (t / 0.35)), UH, 0.5, _env(0.02, 0.1, 0.23))
    add(x, g, 0.02, 0.5, sr)
    return hall(x, sr, 0.2, 0.8)


def death(sr, rng):
    """The Sword of Heaven falls: the choir sinks, bells toll, then the light rises in a long shimmering swell."""
    T = 7.0
    x = buf(sr, T)
    c = choir(sr, rng, (62, 69, 74), 3.0, "ah", voices=3, detune=10)
    c = svf(c, sr, lambda t: 3000 * (0.25 ** min(1.0, t / 3.0)), 0.8, "low")
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.3) * max(0.0, 1 - t / 3.0)), 0, 1.6, sr)
    for i, m in enumerate((62, 57, 50)):
        add(x, bell_voice(sr, midi_hz(m), 3.0, bright=0.8, beat=1.0), 0.4 + i * 0.9, 0.25, sr)
    rise = choir(sr, rng, (74, 81, 86), 3.6, "oo", voices=3, detune=12)
    rise = svf(rise, sr, lambda t: 600 * (8 ** min(1.0, t / 3.0)), 0.7, "low")
    add(x, env_apply(rise, sr, lambda t: min(1.0, (t / 2.5) ** 2) * min(1.0, (3.6 - t) / 1.0)), 3.2, 1.2, sr)
    add(x, sparkle(sr, rng, 30, 3.4, 6.6, T, 2600, 9000), 0, 0.8, sr)
    add(x, thud(sr, 60, 30, 0.6, 2.0), 0.0, 0.9, sr)
    return hall(x, sr, 0.35, 2.0)


def wings(sr, rng, variant):
    """A great wingbeat: the downstroke's deep push of air and the rustle of metal feathers."""
    T = 1.1
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.75, 180, 420 + 60 * variant, 0.8, 0.45), 0, 1.0, sr)
    add(x, onepole_lp(whoosh(sr, rng, 0.7, 90, 160, 0.7, 0.4), sr, 300), 0.02, 1.2, sr)
    rus = svf(noise(int(sr * 0.6), rng), sr, 5200, 1.5, "band")
    rus = env_apply(rus, sr, lambda t: (0.5 + 0.5 * math.sin(t * 90)) * math.sin(math.pi * min(1.0, t / 0.6)))
    add(x, rus, 0.12, 0.18, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.15, tail=0.5)


def smite(sr, rng):
    """The touch that burns from inside: a rising searing tone, crackling fire, a thump of light."""
    T = 1.8
    x = buf(sr, T)
    sear = tone(sr, 520, 1.2, glide=1900, attack=0.02)
    add(x, env_apply(sear, sr, lambda t: min(1.0, t / 0.3) * max(0.0, 1 - t / 1.2)), 0, 0.3, sr)
    cr = svf(noise(int(sr * 1.4), rng), sr, 3000, 0.7, "high")
    cr = env_apply(cr, sr, lambda t: (rng.random() < 0.08 or 0.25) * math.exp(-t / 0.6))
    add(x, cr, 0.05, 0.6, sr)
    add(x, thud(sr, 90, 40, 0.2, 0.8), 0.0, 1.0, sr)
    add(x, choir(sr, rng, (74, 81), 1.0, "ah", voices=2, detune=14), 0.0, 0.5, sr)
    return hall(soft_clip(x, 1.4), sr, 0.25, 1.0)


def ask_yes(sr, rng):
    """'I need your yes': a pure bell over a held, quiet chord."""
    T = 3.6
    x = buf(sr, T)
    for m, at in ((76, 0.0), (83, 0.25)):
        add(x, bell_voice(sr, midi_hz(m), 3.2, bright=0.5, beat=0.7), at, 0.35, sr)
    c = choir(sr, rng, (64, 71, 76), T, "oo", voices=3, detune=6)
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.8) * min(1.0, (T - t) / 1.2)), 0, 0.9, sr)
    return hall(x, sr, 0.35, 1.6)


def trumpet_fanfare(sr, rng):
    """The trumpets of Heaven: a rising call (G C E G) answered by a held C major chord."""
    T = 4.2
    x = buf(sr, T)
    for i, (m, at, d) in enumerate(((67, 0.0, 0.3), (72, 0.3, 0.3), (76, 0.6, 0.3), (79, 0.9, 0.7))):
        add(x, trumpet(sr, rng, m, d + 0.05), at, 0.6, sr)
    for m in (60, 64, 67, 72):
        add(x, trumpet(sr, rng, m, 2.4, vib=5.0), 1.65, 0.32, sr)
    add(x, thud(sr, 65, 50, 0.6, 2.0), 1.65, 0.35, sr)
    return hall(soft_clip(x, 1.2), sr, 0.35, 1.8)


def transform(sr, rng):
    """The vessel burns away: a swell of choir and sub rising to a blinding peak at 4.5 s, then the ring of it."""
    T = 6.5
    x = buf(sr, T)
    c = choir(sr, rng, (50, 57, 62, 69, 74, 81), 5.0, "ah", voices=3, detune=12)
    c = svf(c, sr, lambda t: 300 * (20 ** min(1.0, t / 4.5)), 0.8, "low")
    add(x, env_apply(c, sr, lambda t: min(1.0, (t / 4.5) ** 2.5) * (1.0 if t < 4.6 else max(0.0, 1 - (t - 4.6) / 0.4))), 0, 2.2, sr)
    sub = tone(sr, 38, 5.0, glide=55)
    add(x, env_apply(sub, sr, lambda t: min(1.0, t / 4.5) ** 2), 0, 0.5, sr)
    hiss = svf(noise(int(sr * 5.0), rng), sr, lambda t: 800 + 7000 * min(1.0, t / 4.5), 0.7, "high")
    add(x, env_apply(hiss, sr, lambda t: min(1.0, t / 4.5) ** 3), 0, 0.18, sr)
    add(x, burst(sr, rng, 0.4, 2500, 0.6, "high", 0.12), 4.5, 0.8, sr)
    add(x, thud(sr, 70, 28, 0.5, 1.8), 4.5, 1.2, sr)
    for m in (86, 91, 98):
        add(x, bell_voice(sr, midi_hz(m), 2.0, bright=0.7, beat=1.0), 4.5, 0.12, sr)
    return hall(soft_clip(x, 1.2), sr, 0.3, 1.8)


def halo_break(sr, rng):
    """The halo shatters: a cracked bell chord, a burst of breaking glass, falling tinkles."""
    x = buf(sr, 2.4)
    for m in (79, 86, 91):
        add(x, bell_voice(sr, midi_hz(m), 1.4, bright=1.0, beat=9.0), 0, 0.22, sr)
    t = 0.0
    for i in range(10):
        add(x, burst(sr, rng, 0.02, rng.uniform(3000, 8000), 1.0, "high", 0.002), t, rng.uniform(0.4, 1.0), sr)
        t += 0.003 + 0.02 * rng.random()
    for i in range(22):
        tt = 0.1 + 1.7 * (i + rng.random()) / 22
        f = rng.uniform(3500, 9500)
        add(x, partials(sr, 0.3, [(f, 1.0, 0.05), (f * 1.62, 0.4, 0.03)]), tt, 0.15 * (1 - tt / 2.0), sr)
    add(x, thud(sr, 80, 40, 0.15, 0.6), 0, 0.8, sr)
    return hall(x, sr, 0.25, 1.2)


def lance_throw(sr, rng):
    """The Lance hurled: a hard whoosh rising past the ear, a bright metallic 'shing'."""
    x = buf(sr, 1.2)
    add(x, whoosh(sr, rng, 0.7, 300, 2600, 1.5, 0.55), 0, 1.0, sr)
    sl = svf(noise(int(sr * 0.3), rng), sr, lambda t: 3000 + 7000 * t, 3.0, "band")
    add(x, env_apply(sl, sr, lambda t: min(1.0, t / 0.04) * math.exp(-t / 0.1)), 0.32, 0.5, sr)
    add(x, ring(sr, 1750, 0.8), 0.34, 0.25, sr)
    return reverb(x, sr, size=0.7, damp=0.35, wet=0.18, tail=0.6)


def lance_impact(sr, rng):
    """It strikes home: a deep slam into the ground, the shaft ringing, stone pattering, a column of light."""
    x = buf(sr, 2.2)
    add(x, thud(sr, 75, 30, 0.35, 1.2), 0, 1.3, sr)
    add(x, burst(sr, rng, 0.25, 900, 0.7, "low", 0.08), 0, 0.9, sr)
    add(x, ring(sr, 640, 1.8), 0.005, 0.35, sr)
    for i in range(14):
        tt = 0.05 + 0.6 * rng.random()
        add(x, burst(sr, rng, 0.03, rng.uniform(1500, 4000), 3.0, "band", 0.008), tt, 0.2, sr)
    c = choir(sr, rng, (69, 76), 1.6, "ah", voices=2, detune=10)
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.05) * math.exp(-t / 0.6)), 0.02, 0.6, sr)
    return hall(soft_clip(x, 1.3), sr, 0.25, 1.2)


def lance_recall(sr, rng):
    """It flies back: a reversed swoosh rushing in, caught with a chime."""
    x = buf(sr, 1.6)
    w = reverse(whoosh(sr, rng, 0.9, 400, 3000, 1.2, 0.3))
    add(x, w, 0, 0.9, sr)
    add(x, bell_voice(sr, midi_hz(88), 1.2, bright=0.6, beat=1.2), 0.9, 0.3, sr)
    add(x, thud(sr, 200, 120, 0.04, 0.2), 0.9, 0.6, sr)
    return reverb(x, sr, size=0.7, damp=0.35, wet=0.2, tail=0.6)


def feather_storm(sr, rng):
    """A storm of steel feathers: many thin whistles darting past, tinks of metal, a wing's gust under it."""
    T = 2.4
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 1.4, 150, 500, 0.8, 0.3), 0, 0.9, sr)
    for i in range(24):
        at = 0.1 + 1.9 * (i + rng.random()) / 24
        f0 = rng.uniform(2500, 5000)
        d = rng.uniform(0.15, 0.35)
        w = svf(noise(int(sr * d), rng), sr, lambda t, f0=f0, d=d: f0 * (1.6 ** (t / d)), 9.0, "band")
        add(x, env_apply(w, sr, lambda t, d=d: math.sin(math.pi * t / d) ** 2), at, 0.35, sr)
        if rng.random() < 0.5:
            add(x, ring(sr, rng.uniform(2500, 4200), 0.2, 0.6), at + d, 0.05, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.15, tail=0.6)


def choir_chord(sr, rng):
    """The choir swells on a D major chord and fades."""
    T = 3.4
    x = buf(sr, T)
    c = choir(sr, rng, (50, 57, 62, 66, 69, 74), T, "ah", voices=3, detune=10)
    add(x, env_apply(c, sr, lambda t: min(1.0, (t / 1.4) ** 2) * min(1.0, (T - t) / 1.2)), 0, 2.2, sr)
    return hall(x, sr, 0.35, 1.8)


def dive(sr, rng):
    """He stoops: wind roaring and climbing in pitch as he falls."""
    T = 1.4
    x = buf(sr, T)
    add(x, whoosh(sr, rng, T, 200, 1800, 0.9, 0.85), 0, 1.0, sr)
    roar = onepole_lp(noise(int(sr * T), rng), sr, 400)
    add(x, env_apply(roar, sr, lambda t: (t / T) ** 2), 0, 0.8, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.12, tail=0.4)


# --- the Host, the armour, the Grace --------------------------------------------------------------------------

def host_ambient(sr, rng, variant):
    """A soldier of the Host murmurs: a low hum of many voices, faintly ringing."""
    T = 1.8
    x = buf(sr, T)
    for i in range(3):
        f = 110 + 7 * i + 9 * variant
        v = voiced(sr, rng, T, lambda t, f=f: f * (1 + 0.02 * math.sin(t * 3 + i)), HUM, 0.5, _env(0.2, T - 0.7, 0.5))
        add(x, v, 0, 0.5, sr)
    return hall(x, sr, 0.25, 0.8)


def host_hurt(sr, rng, variant):
    x = buf(sr, 0.8)
    add(x, burst(sr, rng, 0.03, 2200, 0.8, "high", 0.003), 0, 0.7, sr)
    add(x, ring(sr, 1150 + 140 * variant, 0.5, 0.7), 0.002, 0.25, sr)
    add(x, voiced(sr, rng, 0.3, lambda t: (160 + 15 * variant) * (0.8 ** (t / 0.3)), UH, 0.6, _env(0.015, 0.08, 0.2)), 0.01, 0.8, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.1, tail=0.3)


def host_death(sr, rng):
    """A soldier of the Host burns out: a falling sigh of light and a fizzing flare."""
    x = buf(sr, 2.0)
    c = choir(sr, rng, (69, 76), 1.4, "oo", voices=2, detune=10)
    c = svf(c, sr, lambda t: 2400 * (0.3 ** min(1.0, t / 1.4)), 0.8, "low")
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.05) * max(0.0, 1 - t / 1.4)), 0, 1.2, sr)
    fz = svf(noise(int(sr * 1.0), rng), sr, 5000, 0.7, "high")
    add(x, env_apply(fz, sr, lambda t: math.exp(-t / 0.35)), 0.05, 0.3, sr)
    add(x, burst(sr, rng, 0.2, 1800, 0.7, "band", 0.05), 0, 0.5, sr)
    return hall(x, sr, 0.25, 1.0)


def host_march(sr, rng, variant):
    """The Host marches: a boot and the clink of armour, in step."""
    x = buf(sr, 0.5)
    add(x, thud(sr, 95 + 10 * variant, 55, 0.04, 0.2), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.06, 600, 1.2, "band", 0.02), 0, 0.6, sr)
    add(x, ring(sr, 2600 + 300 * variant, 0.3, 0.8), 0.03, 0.12, sr)
    return reverb(x, sr, size=0.5, damp=0.5, wet=0.1, tail=0.3)


def host_shield(sr, rng):
    """Shields lock: a heavy wooden-metal thud, a ring, the scrape of rims meeting."""
    x = buf(sr, 1.0)
    add(x, thud(sr, 120, 60, 0.08, 0.4), 0, 1.0, sr)
    add(x, ring(sr, 980, 0.8), 0.003, 0.3, sr)
    sc = svf(noise(int(sr * 0.2), rng), sr, 3200, 2.0, "band")
    add(x, env_apply(sc, sr, lambda t: math.exp(-t / 0.06)), 0.05, 0.25, sr)
    return reverb(x, sr, size=0.5, damp=0.45, wet=0.12, tail=0.4)


def armor_ward(sr, rng):
    """A wing of light takes the blow: a crystal chime chord and a soft beat of wings."""
    x = buf(sr, 2.0)
    for m, at in ((84, 0.0), (88, 0.04), (91, 0.08)):
        add(x, bell_voice(sr, midi_hz(m), 1.6, bright=0.6, beat=1.4), at, 0.25, sr)
    add(x, whoosh(sr, rng, 0.5, 300, 700, 0.8, 0.4), 0, 0.5, sr)
    return hall(x, sr, 0.25, 1.0)


def grace_flight(sr, rng):
    """Wings carry you: wind rushing past and the slow flutter of great wings."""
    T = 2.4
    x = buf(sr, T)
    wind = svf(noise(int(sr * T), rng), sr, lambda t: 600 + 300 * math.sin(t * 2.5), 0.6, "band")
    add(x, env_apply(wind, sr, lambda t: min(1.0, t / 0.4) * min(1.0, (T - t) / 0.5)), 0, 0.7, sr)
    for at in (0.2, 1.1):
        add(x, whoosh(sr, rng, 0.6, 140, 320, 0.8, 0.45), at, 0.6, sr)
    return reverb(x, sr, size=0.5, damp=0.4, wet=0.1, tail=0.4)
