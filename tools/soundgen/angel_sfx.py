"""v0.22 angels and the Fall: every sound in AngelAssets.SOUNDS, written by generate.py to sounds/angel/<id>.ogg; the angel sound
data points supernaturalcraft:angel.<id> at them once the files exist (rerun runData).

The palette: an angel is a quiet, airy presence (a soft high choir pad, a glassy shimmer); hurt, the pad cracks into a short
bright stab; the fallen are the same voice lower, rougher and out of tune. The smite is the show's: a piercing ring that climbs
over a rising hum, then a bright whoomp. The Fall is heard from far away: a thin whistle overhead and a distant boom.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from michael_sfx import hall, ring, whoosh
from raphael_sfx import rumble
from sfx import burst, choir, sparkle, thud
from synth import TAU, add, buf, env_apply, noise, onepole_lp, reverb, soft_clip, svf, tone


def _sines(sr, T, freqs, gain=1.0, vib=(5.0, 0.004), rng=None):
    """A cluster of slightly wavering sines (a glassy pad)."""
    x = buf(sr, T)
    for f in freqs:
        ph = rng.random() * TAU if rng else 0.0
        rate = vib[0] * (0.85 + 0.3 * (rng.random() if rng else 0.5))
        n = len(x)
        p = ph
        for i in range(n):
            t = i / sr
            p += TAU * f * (1 + vib[1] * math.sin(TAU * rate * t)) / sr
            x[i] += math.sin(p) * gain / len(freqs)
    return x


# --- an angel ----------------------------------------------------------------------------------------------------

def ambient(sr, rng):
    """A soft, high choir breath with a glassy shimmer, rising and settling."""
    T = 2.2
    x = buf(sr, T)
    c = choir(sr, rng, (76, 83, 88), T, "oo", voices=3, detune=7)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / T) ** 2), 0, 1.6, sr)
    add(x, env_apply(_sines(sr, T, (2637, 3136, 3951), 1.0, (6.0, 0.003), rng), sr,
                     lambda t: 0.25 * math.sin(math.pi * t / T) ** 3), 0, 1.0, sr)
    return hall(x, sr, 0.3, 1.2)


def hurt(sr, rng):
    """The pad struck: a short, bright, dissonant stab and a ring."""
    T = 0.7
    x = buf(sr, T)
    c = choir(sr, rng, (79, 80, 86), T, "ah", voices=2, detune=18)
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.01) * math.exp(-t / 0.18)), 0, 2.0, sr)
    add(x, ring(sr, 1760, T, 0.8), 0, 0.25, sr)
    add(x, burst(sr, rng, 0.12, 5200, 1.2, "band", 0.03), 0, 0.3, sr)
    return hall(x, sr, 0.22, 0.7)


def death(sr, rng):
    """A choir chord collapsing downward and dissolving into air."""
    T = 2.4
    x = buf(sr, T)
    c = choir(sr, rng, (74, 79, 83, 86), T, "ah", voices=3, detune=12)
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.05) * math.exp(-t / 0.9)), 0, 1.8, sr)
    add(x, tone(sr, 1320, T, decay=0.8, glide=440, attack=0.01), 0, 0.25, sr)
    add(x, whoosh(sr, rng, 1.6, 2400, 600, 0.9, 0.3), 0.1, 0.3, sr)
    return hall(x, sr, 0.35, 1.5)


# --- the fallen --------------------------------------------------------------------------------------------------

def fallen_ambient(sr, rng):
    """The same breath an octave down, out of tune and frayed: a rough, sour low choir with a dirty hiss."""
    T = 2.4
    x = buf(sr, T)
    c = choir(sr, rng, (52, 58, 63), T, "oh", voices=3, detune=28)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / T) ** 2), 0, 1.8, sr)
    hiss = svf(noise(int(sr * T), rng), sr, 2600, 0.8, "band")
    add(x, env_apply(hiss, sr, lambda t: 0.12 * math.sin(math.pi * t / T)), 0, 1.0, sr)
    return soft_clip(reverb(x, sr, size=0.6, damp=0.55, wet=0.2, tail=0.8), 1.6)


def fallen_hurt(sr, rng):
    """A torn, low stab: a sour cluster that cracks."""
    T = 0.6
    x = buf(sr, T)
    c = choir(sr, rng, (55, 56, 62), T, "ah", voices=2, detune=30)
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.008) * math.exp(-t / 0.15)), 0, 2.4, sr)
    add(x, burst(sr, rng, 0.15, 900, 0.9, "band", 0.04), 0, 0.5, sr)
    return soft_clip(reverb(x, sr, size=0.5, damp=0.5, wet=0.15, tail=0.4), 2.2)


# --- wings ---------------------------------------------------------------------------------------------------------

def teleport(sr, rng):
    """One heavy beat of great wings: the downstroke's push of air, the feathers' rustle, then nothing."""
    T = 0.9
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.55, 140, 520, 0.7, 0.35), 0, 1.0, sr)
    add(x, onepole_lp(whoosh(sr, rng, 0.5, 70, 140, 0.7, 0.3), sr, 260), 0.02, 1.5, sr)
    add(x, thud(sr, 85, 45, 0.09, 0.4), 0.14, 0.6, sr)
    rus = svf(noise(int(sr * 0.45), rng), sr, 4600, 1.4, "band")
    rus = env_apply(rus, sr, lambda t: (0.5 + 0.5 * math.sin(t * 110)) * math.sin(math.pi * min(1.0, t / 0.45)))
    add(x, rus, 0.08, 0.2, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.12, tail=0.4)


# --- the smite -----------------------------------------------------------------------------------------------------

def smite_ring(sr, rng):
    """The angel's light pouring out: a piercing, high choir-like ring climbing and beating over a rising hum (~1.6 s)."""
    T = 1.6
    x = buf(sr, T)
    rise = lambda t: min(1.0, (t / T) ** 1.4)
    # The hum: low sines and a soft saw-choir swelling up a fifth.
    hum = buf(sr, T)
    add(hum, tone(sr, 98, T, glide=147, attack=0.05), 0, 0.8, sr)
    add(hum, tone(sr, 196, T, glide=294, attack=0.05), 0, 0.4, sr)
    add(x, env_apply(hum, sr, lambda t: 0.2 + 0.8 * rise(t)), 0, 0.7, sr)
    c = choir(sr, rng, (79, 84, 88, 91), T, "ah", voices=3, detune=14, vib=(6.5, 14.0))
    add(x, env_apply(c, sr, lambda t: 0.1 + 0.9 * rise(t)), 0, 1.4, sr)
    # The scream: a cluster of high, beating sines gliding upward, brightening to the end.
    n = int(sr * T)
    scream = [0.0] * n
    for f0, a in ((2093, 1.0), (2100, 0.8), (3136, 0.55), (3148, 0.45), (4186, 0.3)):
        p = rng.random() * TAU
        for i in range(n):
            t = i / sr
            f = f0 * (1 + 0.12 * (t / T) ** 2) * (1 + 0.006 * math.sin(TAU * 7.5 * t))
            p += TAU * f / sr
            scream[i] += math.sin(p) * a * 0.3
    add(x, env_apply(scream, sr, lambda t: min(1.0, t / 0.04) * (0.35 + 0.65 * rise(t))), 0, 0.9, sr)
    add(x, env_apply(sparkle(sr, rng, 40, 0.0, T, T, 3000, 8000), sr, lambda t: 0.3 + 0.7 * rise(t)), 0, 0.18, sr)
    # Cut short at the end: the burst takes over.
    x = env_apply(x, sr, lambda t: 1.0 if t < T - 0.05 else max(0.0, (T - t) / 0.05))
    return hall(x, sr, 0.25, 0.6)


def smite_burst(sr, rng):
    """The flash: a bright, airy whoomp (low thump, a breath of white noise and a short high shimmer)."""
    T = 1.8
    x = buf(sr, T)
    add(x, thud(sr, 120, 38, 0.25, 1.0), 0, 1.0, sr)
    add(x, burst(sr, rng, 1.2, 1800, 0.6, "low", 0.22, attack=0.004), 0, 0.9, sr)
    add(x, whoosh(sr, rng, 0.9, 5000, 900, 0.8, 0.08), 0, 0.45, sr)
    add(x, ring(sr, 2640, 1.4, 0.6), 0.01, 0.12, sr)
    add(x, sparkle(sr, rng, 26, 0.02, 0.8, T, 3500, 9000, rise=False), 0, 0.12, sr)
    return hall(x, sr, 0.35, 1.4)


# --- the Fall ------------------------------------------------------------------------------------------------------

def fall_streak(sr, rng):
    """A far, thin whistle overhead falling in pitch, with a soft rush of air (the Doppler of something burning down)."""
    T = 2.6
    x = buf(sr, T)
    add(x, env_apply(tone(sr, 2400, T, glide=900), sr, lambda t: math.sin(math.pi * t / T) ** 2), 0, 0.35, sr)
    add(x, env_apply(tone(sr, 1800, T, glide=650), sr, lambda t: math.sin(math.pi * t / T) ** 3), 0, 0.15, sr)
    add(x, whoosh(sr, rng, T, 3000, 700, 1.0, 0.6), 0, 0.6, sr)
    x = onepole_lp(x, sr, 4500)
    return reverb(x, sr, size=0.95, damp=0.6, wet=0.35, tail=1.2)


def fall_impact(sr, rng):
    """Far away, something lands: a muffled boom and a long rolling rumble."""
    T = 3.5
    x = buf(sr, T)
    add(x, thud(sr, 70, 30, 0.45, 1.6), 0, 1.0, sr)
    add(x, rumble(sr, rng, T, 70, 1.2, 5), 0, 1.2, sr)
    add(x, onepole_lp(burst(sr, rng, 0.5, 600, 0.7, "low", 0.12), sr, 400), 0, 0.6, sr)
    x = onepole_lp(x, sr, 900)
    return reverb(x, sr, size=0.95, damp=0.65, wet=0.3, tail=1.5, predelay=0.04)


SOUNDS = {
    "ambient": (ambient, -5.0), "hurt": (hurt, -2.0), "death": (death, -1.5), "fallen_ambient": (fallen_ambient, -5.0),
    "fallen_hurt": (fallen_hurt, -2.0), "teleport": (teleport, -1.5), "smite_ring": (smite_ring, -1.0),
    "smite_burst": (smite_burst, -1.0), "fall_streak": (fall_streak, -4.0), "fall_impact": (fall_impact, -1.5),
}
