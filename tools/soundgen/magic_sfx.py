"""v0.21 the magic: every sound in MagicAssets.SOUNDS, written by generate.py to sounds/magic/<id>.ogg (MagicAssets.soundFile:
magic.enochian.cast -> magic/enochian_cast.ogg, bowl.boil -> magic/bowl_boil.ogg); MagicSoundData points the events at them.

Each school has its own timbre, so a spell is known by ear before it is seen:
  Enochian   -- a choir and bells shimmering (bell_voice, choir, sparkle);
  Infernal   -- roaring fire, crackle and a low growl under it;
  Witchcraft -- breath and whispers, glass and wind chimes;
  Hunter     -- the hiss of thrown salt and the ring of struck iron.
Per school: cast (the circle opening), impact (the effect landing), charge (a rite charging: a seamless loop, see LOOPS) and
complete (a rite taking: the big one). Then the bowl (boil, seal, send) and the rites' rift and implosion.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from michael_sfx import hall, ring, whoosh
from raphael_sfx import buzz, crackle, rumble
from sfx import bell_voice, burst, choir, scribble, sparkle, thud, voiced
from synth import (add, buf, env_apply, formants, midi_hz, noise, onepole_lp, partials, reverb, reverse, smooth_random,
                   soft_clip, svf, tone)

LOOP_T = 4.0
XFADE = 0.6

OH = [(500, 3.5, 1.0), (880, 4.5, 0.5), (2600, 7.0, 0.15)]
AH = [(730, 4.0, 1.0), (1090, 5.0, 0.55), (2440, 7.0, 0.3)]
EE = [(300, 4.0, 1.0), (2300, 6.0, 0.4), (3000, 8.0, 0.2)]
SS = [(4800, 2.0, 1.0), (7200, 3.0, 0.6)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def loop(x, sr, xfade=XFADE):
    """Folds the tail over the head (equal-power crossfade): x must be LOOP_T + xfade long; returns LOOP_T seconds."""
    n = int(sr * LOOP_T)
    f = int(sr * xfade)
    out = x[:n]
    for i in range(f):
        k = i / f
        out[i] = x[i] * math.sin(k * math.pi / 2) + x[n + i] * math.cos(k * math.pi / 2)
    return out


# --- building blocks -----------------------------------------------------------------------------------------------

def chimes(sr, rng, T, count, lo=76, hi=100, start=0.0, end=None, decay=0.6):
    """Glass and wind chimes: high pentatonic bells, struck at random, each ringing and shimmering out."""
    end = T * 0.8 if end is None else end
    penta = [0, 2, 4, 7, 9]
    x = buf(sr, T)
    for i in range(count):
        octave = rng.randint(0, 2)
        m = lo + penta[rng.randrange(5)] + 12 * octave
        m = min(m, hi)
        at = start + (end - start) * rng.random()
        add(x, bell_voice(sr, midi_hz(m), min(decay * 3, T - at), bright=0.7, beat=2.4), at, rng.uniform(0.08, 0.2), sr)
    return x


def whisper(sr, rng, T, syllables=5, gain=1.0):
    """Breathy words that never quite resolve: noise shaped into vowels and sibilants, syllable by syllable."""
    x = buf(sr, T)
    vowels = [OH, AH, EE]
    t = 0.05
    for i in range(syllables):
        d = rng.uniform(0.12, 0.26)
        if t + d > T:
            break
        n = noise(int(sr * d), rng)
        v = formants(n, sr, vowels[rng.randrange(3)])
        v = env_apply(v, sr, lambda u, d=d: math.sin(math.pi * min(1.0, u / d)) ** 1.5)
        add(x, v, t, 0.5 * gain, sr)
        if rng.random() < 0.5:
            s = svf(noise(int(sr * 0.08), rng), sr, 5600, 1.2, "band")
            s = env_apply(s, sr, lambda u: math.sin(math.pi * min(1.0, u / 0.08)))
            add(x, s, t + d * 0.8, 0.35 * gain, sr)
        t += d + rng.uniform(0.02, 0.1)
    return onepole_lp(x, sr, 6500)


def growl(sr, rng, T, f0=58, vowel=OH):
    """A beast's low growl: a rough voice with a wandering pitch, through an open vowel."""
    wander = smooth_random(sr, T, rng, 6, 0.85, 1.15)
    v = voiced(sr, rng, T, lambda t: f0 * wander(t), vowel, breath=0.8)
    return soft_clip([s * 2.0 for s in v], 1.6)


def fire_roar(sr, rng, T, lo=180, hi=900, peak=0.4):
    """A gout of fire: a swelling band of noise, gusting, with crackle in it."""
    x = whoosh(sr, rng, T, lo, hi, 0.9, peak)
    g = smooth_random(sr, T, rng, 9, 0.6, 1.0)
    x = [s * g(i / sr) for i, s in enumerate(x)]
    add(x, crackle(sr, rng, T, 0.08, 2600, 0.8), 0, 0.5, sr)
    return x


def salt_hiss(sr, rng, T, fc=6200):
    """Thrown salt: a bright scattering hiss of tiny grains."""
    n = int(sr * T)
    x = [0.0] * n
    for i in range(n):
        if rng.random() < 0.18:
            x[i] = rng.uniform(-1, 1)
    x = svf(x, sr, fc, 0.7, "high")
    return env_apply(x, sr, lambda t: min(1.0, t / 0.01) * math.exp(-t / (T * 0.35)))


def iron(sr, rng, f0, T, bright=1.0):
    """Struck iron: a ring with a dull knock in front of it."""
    x = buf(sr, T)
    add(x, ring(sr, f0, T, bright), 0, 0.9, sr)
    add(x, burst(sr, rng, 0.06, 900, 1.4, "band", 0.015), 0, 0.6, sr)
    add(x, thud(sr, f0 * 0.5, f0 * 0.3, 0.08, 0.3), 0, 0.4, sr)
    return x


def pops(sr, rng, T, count, lo=300, hi=900):
    """Bubbles bursting: short blips gliding up, at random."""
    x = buf(sr, T)
    for _ in range(count):
        f = rng.uniform(lo, hi)
        d = rng.uniform(0.02, 0.05)
        b = tone(sr, f, d, decay=d * 0.4, glide=f * rng.uniform(1.4, 2.2), attack=0.002)
        add(x, b, rng.uniform(0, T - 0.06), rng.uniform(0.2, 0.5), sr)
    return x


# --- Enochian: choir and bells ---------------------------------------------------------------------------------------

def enochian_cast(sr, rng):
    T = 1.6
    x = buf(sr, T)
    add(x, sparkle(sr, rng, 10, 0.0, 0.5, T, 2600, 7200), 0, 0.8, sr)
    for i, m in enumerate((84, 88, 91)):
        add(x, bell_voice(sr, midi_hz(m), 1.4, bright=0.8, beat=1.1), 0.04 + i * 0.05, 0.22, sr)
    c = choir(sr, rng, (72, 76, 79), 1.0, "ah", voices=2, detune=7)
    add(x, env_apply(c, sr, _env(0.08, 0.2, 0.7)), 0.02, 0.5, sr)
    return hall(x, sr, 0.28, 1.0)


def enochian_impact(sr, rng):
    T = 1.8
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.25, 3600, 0.7, "high", 0.05), 0, 0.7, sr)
    add(x, thud(sr, 160, 70, 0.12, 0.5), 0, 0.6, sr)
    add(x, bell_voice(sr, midi_hz(79), 1.6, bright=1.0, beat=1.6), 0, 0.35, sr)
    add(x, bell_voice(sr, midi_hz(86), 1.2, bright=0.8, beat=1.2), 0.02, 0.2, sr)
    c = choir(sr, rng, (67, 74, 79), 0.9, "ah", voices=2, detune=9)
    add(x, env_apply(c, sr, _env(0.01, 0.1, 0.8)), 0, 0.45, sr)
    add(x, sparkle(sr, rng, 8, 0.02, 0.6, T, 3000, 8000, rise=False), 0, 0.6, sr)
    return hall(x, sr, 0.3, 1.2)


def enochian_charge(sr, rng):
    T = LOOP_T + XFADE
    x = buf(sr, T)
    c = choir(sr, rng, (55, 62, 67, 71), T, "oo", voices=3, detune=8, vib=(4.6, 7.0))
    sway = smooth_random(sr, T, rng, 0.8, 0.75, 1.0)
    add(x, [s * sway(i / sr) for i, s in enumerate(c)], 0, 0.9, sr)
    add(x, sparkle(sr, rng, 14, 0.0, T - 0.2, T, 3200, 7600, rise=False), 0, 0.5, sr)
    return loop(reverb(x, sr, size=0.9, damp=0.3, wet=0.25, tail=0.0), sr)


def enochian_complete(sr, rng):
    T = 4.5
    x = buf(sr, T)
    c = choir(sr, rng, (48, 55, 60, 64, 67, 72), 3.4, "ah", voices=3, detune=9)
    add(x, env_apply(c, sr, _env(0.25, 1.6, 1.5)), 0.05, 1.0, sr)
    add(x, thud(sr, 90, 40, 0.5, 1.5), 0, 0.7, sr)
    add(x, burst(sr, rng, 0.4, 2400, 0.6, "high", 0.12), 0, 0.5, sr)
    for i, m in enumerate((72, 76, 79, 84, 88, 91, 96)):
        add(x, bell_voice(sr, midi_hz(m), 2.4, bright=0.8, beat=1.0), 0.1 + i * 0.11, 0.16, sr)
    add(x, sparkle(sr, rng, 24, 0.1, 2.6, T, 2800, 8000), 0, 0.7, sr)
    return hall(x, sr, 0.35, 2.0)


# --- Infernal: roar, crackle, growl ----------------------------------------------------------------------------------

def infernal_cast(sr, rng):
    T = 1.4
    x = buf(sr, T)
    add(x, fire_roar(sr, rng, 1.1, 160, 1400, 0.35), 0, 1.0, sr)
    g = growl(sr, rng, 0.9, 52)
    add(x, env_apply(g, sr, _env(0.12, 0.3, 0.45)), 0.05, 0.45, sr)
    add(x, crackle(sr, rng, 1.0, 0.12, 3200, 0.9, 0.4), 0.1, 0.6, sr)
    return reverb(soft_clip(x, 1.3), sr, size=0.6, damp=0.5, wet=0.18, tail=0.5)


def infernal_impact(sr, rng):
    T = 2.0
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.5, 500, 0.6, "low", 0.15), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.2, 2000, 0.7, "band", 0.05), 0, 0.6, sr)
    add(x, thud(sr, 90, 35, 0.25, 0.9), 0, 0.9, sr)
    add(x, rumble(sr, rng, 1.8, 80, 0.6, 5), 0.02, 1.6, sr)
    add(x, crackle(sr, rng, 1.6, 0.1, 2800, 0.8, 0.5), 0.05, 0.7, sr)
    add(x, fire_roar(sr, rng, 1.2, 300, 900, 0.15), 0.0, 0.6, sr)
    return reverb(soft_clip(x, 1.6), sr, size=0.7, damp=0.5, wet=0.2, tail=0.6)


def infernal_charge(sr, rng):
    T = LOOP_T + XFADE
    x = buf(sr, T)
    add(x, buzz(sr, rng, T, 55), 0, 0.5, sr)
    add(x, rumble(sr, rng, T, 70, 99.0, 8), 0, 1.4, sr)
    add(x, crackle(sr, rng, T, 0.03, 2400, 0.8), 0, 0.6, sr)
    g = growl(sr, rng, T, 41, OH)
    swell = smooth_random(sr, T, rng, 0.7, 0.2, 0.7)
    add(x, [s * swell(i / sr) for i, s in enumerate(g)], 0, 0.35, sr)
    return loop(onepole_lp(x, sr, 3200), sr)


def infernal_complete(sr, rng):
    T = 4.2
    x = buf(sr, T)
    add(x, fire_roar(sr, rng, 2.6, 120, 1100, 0.3), 0, 1.0, sr)
    g = growl(sr, rng, 2.4, 46, AH)
    add(x, env_apply(g, sr, _env(0.3, 1.2, 0.9)), 0.1, 0.7, sr)
    add(x, thud(sr, 70, 28, 0.6, 1.8), 0.25, 1.0, sr)
    add(x, rumble(sr, rng, 3.6, 70, 1.2, 6), 0.2, 2.0, sr)
    for at in (0.25, 0.6, 1.1):
        add(x, crackle(sr, rng, 0.8, 0.25, 3000, 0.9, 0.3), at, 0.7, sr)
    return reverb(soft_clip(x, 1.6), sr, size=0.8, damp=0.45, wet=0.22, tail=1.0)


# --- Witchcraft: breath, whispers, glass ------------------------------------------------------------------------------

def witchcraft_cast(sr, rng):
    T = 1.6
    x = buf(sr, T)
    add(x, whisper(sr, rng, 1.0, 4), 0, 0.9, sr)
    add(x, chimes(sr, rng, T, 5, 79, 103, 0.15, 0.6, 0.4), 0, 1.0, sr)
    add(x, whoosh(sr, rng, 0.8, 600, 3200, 1.0, 0.6), 0.1, 0.35, sr)
    return reverb(x, sr, size=0.75, damp=0.35, wet=0.3, tail=0.8)


def witchcraft_impact(sr, rng):
    T = 1.8
    x = buf(sr, T)
    # A pane of glass giving way: a bright cluster of chimes over a crackle of fragments.
    add(x, chimes(sr, rng, T, 9, 84, 108, 0.0, 0.15, 0.35), 0, 1.2, sr)
    add(x, crackle(sr, rng, 0.5, 0.2, 6000, 0.9, 0.15), 0, 0.5, sr)
    add(x, whoosh(sr, rng, 0.6, 2400, 500, 1.1, 0.2), 0, 0.5, sr)
    add(x, tone(sr, 110, 1.2, decay=0.4, glide=82, attack=0.005), 0, 0.35, sr)
    add(x, whisper(sr, rng, 0.8, 3, 0.6), 0.2, 0.6, sr)
    return reverb(x, sr, size=0.8, damp=0.3, wet=0.3, tail=1.0)


def witchcraft_charge(sr, rng):
    T = LOOP_T + XFADE
    x = buf(sr, T)
    wind = svf(noise(int(sr * T), rng), sr, lambda t: 500 + 400 * math.sin(t * 1.3) + 300 * math.sin(t * 0.37), 1.4, "band")
    gust = smooth_random(sr, T, rng, 1.2, 0.3, 1.0)
    add(x, [s * gust(i / sr) for i, s in enumerate(wind)], 0, 0.8, sr)
    add(x, whisper(sr, rng, T, 14, 0.5), 0, 1.0, sr)
    add(x, chimes(sr, rng, T, 7, 76, 100, 0.1, T - 0.6, 0.5), 0, 0.7, sr)
    return loop(reverb(x, sr, size=0.8, damp=0.35, wet=0.3, tail=0.0), sr)


def witchcraft_complete(sr, rng):
    T = 4.0
    x = buf(sr, T)
    add(x, chimes(sr, rng, T, 18, 76, 108, 0.0, 1.6, 0.6), 0, 1.2, sr)
    add(x, whoosh(sr, rng, 2.0, 300, 2600, 1.0, 0.4), 0, 0.6, sr)
    c = choir(sr, rng, (57, 60, 64, 69), 2.6, "oo", voices=2, detune=12, vib=(5.5, 14.0))
    add(x, env_apply(c, sr, _env(0.4, 1.0, 1.2)), 0.2, 0.7, sr)
    add(x, whisper(sr, rng, 2.0, 8, 0.7), 0.3, 0.7, sr)
    add(x, thud(sr, 80, 40, 0.4, 1.2), 0, 0.5, sr)
    return reverb(x, sr, size=0.9, damp=0.3, wet=0.32, tail=1.6)


# --- Hunter: salt and iron ---------------------------------------------------------------------------------------------

def hunter_cast(sr, rng):
    T = 1.4
    x = buf(sr, T)
    add(x, salt_hiss(sr, rng, 0.7), 0, 0.9, sr)
    add(x, iron(sr, rng, 520, 1.2, 1.0), 0.04, 0.6, sr)
    add(x, iron(sr, rng, 780, 0.8, 0.8), 0.09, 0.3, sr)
    return reverb(x, sr, size=0.55, damp=0.45, wet=0.18, tail=0.5)


def hunter_impact(sr, rng):
    T = 1.6
    x = buf(sr, T)
    add(x, iron(sr, rng, 330, 1.5, 1.2), 0, 1.0, sr)
    add(x, thud(sr, 120, 50, 0.12, 0.5), 0, 0.7, sr)
    add(x, salt_hiss(sr, rng, 0.9, 5200), 0, 0.8, sr)
    add(x, burst(sr, rng, 0.1, 2800, 0.8, "high", 0.02), 0, 0.5, sr)
    return reverb(x, sr, size=0.6, damp=0.45, wet=0.2, tail=0.6)


def hunter_charge(sr, rng):
    T = LOOP_T + XFADE
    x = buf(sr, T)
    # Iron humming under a hand: sustained partials of a big ring, beating slowly.
    for r, a in ((1.0, 0.5), (1.51, 0.3), (2.27, 0.2), (3.11, 0.1)):
        f = 110 * r
        add(x, tone(sr, f, T), 0, a * 0.5, sr)
        add(x, tone(sr, f + 0.6 * r, T, phase=1.0), 0, a * 0.4, sr)
    hiss = svf(noise(int(sr * T), rng), sr, 5200, 0.7, "high")
    swell = smooth_random(sr, T, rng, 1.0, 0.1, 0.5)
    add(x, [s * swell(i / sr) for i, s in enumerate(hiss)], 0, 0.25, sr)
    return loop(reverb(x, sr, size=0.7, damp=0.4, wet=0.2, tail=0.0), sr)


def hunter_complete(sr, rng):
    T = 4.0
    x = buf(sr, T)
    add(x, iron(sr, rng, 98, 3.8, 1.3), 0, 1.0, sr)
    add(x, iron(sr, rng, 196, 2.6, 1.0), 0.02, 0.5, sr)
    add(x, thud(sr, 70, 30, 0.5, 1.4), 0, 0.9, sr)
    s = salt_hiss(sr, rng, 2.0, 5600)
    add(x, reverse(s), 0, 0.5, sr)
    add(x, salt_hiss(sr, rng, 1.4, 6000), 2.0, 0.7, sr)
    return reverb(x, sr, size=0.85, damp=0.4, wet=0.25, tail=1.4)


# --- the bowl and the rites -----------------------------------------------------------------------------------------------

def bowl_boil(sr, rng):
    T = 2.4
    x = buf(sr, T)
    add(x, pops(sr, rng, T, 46, 260, 900), 0, 1.0, sr)
    low = onepole_lp(noise(int(sr * T), rng), sr, 240)
    churn = smooth_random(sr, T, rng, 7, 0.3, 1.0)
    add(x, [s * churn(i / sr) for i, s in enumerate(low)], 0, 1.4, sr)
    return env_apply(x, sr, _env(0.15, T - 0.6, 0.45))


def bowl_seal(sr, rng):
    T = 2.6
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 1.2, 300, 1800, 1.0, 0.5), 0, 0.5, sr)
    add(x, scribble(sr, rng, 7, 1.4, 2600, 4800, 0.9), 0.25, 0.7, sr)
    add(x, whisper(sr, rng, 1.4, 5, 0.6), 0.2, 0.7, sr)
    add(x, bell_voice(sr, midi_hz(84), 1.2, bright=0.7, beat=1.5), 1.55, 0.28, sr)
    add(x, bell_voice(sr, midi_hz(91), 1.0, bright=0.6, beat=1.2), 1.6, 0.18, sr)
    return reverb(x, sr, size=0.75, damp=0.35, wet=0.28, tail=0.9)


def bowl_send(sr, rng):
    T = 2.0
    x = buf(sr, T)
    # Away it goes: a whoosh falling in pitch as it leaves, glyph-sparkle trailing behind.
    add(x, whoosh(sr, rng, 1.4, 2600, 380, 1.3, 0.25), 0, 0.8, sr)
    add(x, sparkle(sr, rng, 10, 0.05, 0.9, T, 2400, 6400, rise=False), 0, 0.6, sr)
    add(x, tone(sr, 880, 1.0, decay=0.35, glide=440, attack=0.01), 0, 0.15, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.25, tail=0.8)


def ritual_rift(sr, rng):
    T = 4.0
    x = buf(sr, T)
    # The air tearing: a dense crackle and a band of noise ripping upward, a deep boom, a sub rumble, and a reversed swell into it.
    swell = reverse(reverb(burst(sr, rng, 0.6, 1200, 0.6, "band", 0.25), sr, size=0.9, damp=0.3, wet=0.8, tail=0.8))
    add(x, swell, 0, 0.8, sr)
    t0 = len(swell) / sr
    add(x, crackle(sr, rng, 1.6, 0.3, 3600, 0.8, 0.6), t0 - 0.05, 0.9, sr)
    add(x, whoosh(sr, rng, 1.0, 400, 4200, 1.6, 0.2), t0 - 0.1, 0.7, sr)
    add(x, thud(sr, 60, 24, 0.7, 2.0), t0, 1.2, sr)
    add(x, rumble(sr, rng, 2.6, 60, 1.0, 5), t0, 2.2, sr)
    add(x, buzz(sr, rng, 1.8, 47), t0 + 0.1, 0.25, sr)
    return reverb(soft_clip(x, 1.5), sr, size=0.9, damp=0.4, wet=0.25, tail=1.2)


def ritual_implode(sr, rng):
    T = 3.0
    x = buf(sr, T)
    # Sucked in (a reversed tail), held breath, then the pop and its dark thump.
    tail = reverb(burst(sr, rng, 0.5, 900, 0.7, "band", 0.2), sr, size=0.95, damp=0.3, wet=0.9, tail=1.0)
    suck = reverse(tail)
    add(x, suck, 0, 0.9, sr)
    pop = len(suck) / sr
    add(x, whoosh(sr, rng, pop, 1800, 200, 1.0, 0.95), 0, 0.5, sr)
    add(x, burst(sr, rng, 0.15, 300, 0.8, "low", 0.04), pop, 1.2, sr)
    add(x, thud(sr, 75, 30, 0.35, 1.2), pop, 1.1, sr)
    add(x, crackle(sr, rng, 0.8, 0.2, 2200, 0.8, 0.25), pop + 0.02, 0.5, sr)
    add(x, rumble(sr, rng, 1.2, 60, 0.4, 4), pop, 1.2, sr)
    return reverb(soft_clip(x, 1.4), sr, size=0.7, damp=0.45, wet=0.18, tail=0.6)


# id (MagicAssets.soundFile's base name) -> (builder, peak dBFS)
SOUNDS = {
    "enochian_cast": (enochian_cast, -2.5), "enochian_impact": (enochian_impact, -1.5),
    "enochian_charge": (enochian_charge, -6.0), "enochian_complete": (enochian_complete, -1.0),
    "infernal_cast": (infernal_cast, -2.5), "infernal_impact": (infernal_impact, -1.5),
    "infernal_charge": (infernal_charge, -6.0), "infernal_complete": (infernal_complete, -1.0),
    "witchcraft_cast": (witchcraft_cast, -2.5), "witchcraft_impact": (witchcraft_impact, -1.5),
    "witchcraft_charge": (witchcraft_charge, -6.0), "witchcraft_complete": (witchcraft_complete, -1.0),
    "hunter_cast": (hunter_cast, -2.5), "hunter_impact": (hunter_impact, -1.5),
    "hunter_charge": (hunter_charge, -6.0), "hunter_complete": (hunter_complete, -1.0),
    "bowl_boil": (bowl_boil, -5.0), "bowl_seal": (bowl_seal, -2.5), "bowl_send": (bowl_send, -2.5),
    "ritual_rift": (ritual_rift, -1.0), "ritual_implode": (ritual_implode, -1.5),
}

# Seamless loops (generate.py skips fades and trimming for these).
LOOPS = {"enochian_charge", "infernal_charge", "witchcraft_charge", "hunter_charge"}
