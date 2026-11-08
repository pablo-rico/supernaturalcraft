"""v0.42 Amara, the Darkness (season 11): every sound in AmaraAssets.SOUNDS, written by generate.py to sounds/amara/<id>.ogg
(supernaturalcraft:amara.<id>). The old entity.amara.* events stay on phase I; these are the rework's.

Her palette runs from the void to the dawn, as the fight does:
  her voice   -- a woman's breath and hum, close and intimate, and the little girl she was: a giggle, a scream (child formants,
                 reverse-reverb swells so the sound arrives before itself, long echoing rooms);
  her power   -- black smoke and stone: grinding lifts, roaring walls of smoke, the ground crumbling into nothing, islands
                 sinking, a vortex pulling toward the black sun, souls drawn out on a ghostly tone;
  God's light -- bright tones that crack and fizz, bells, falling stars;
  the end     -- the sun's deep falling bell, a dying heartbeat, the sun guttering out, and at last a warm chord: light meets
                 darkness, and the dawn.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py. The space
helpers (echo, pre_swell, void_room) are shared with s11_sfx.
"""

import math

from coven_sfx import glide, rasp, syllable
from magic_sfx import whisper
from michael_sfx import whoosh
from raphael_sfx import crackle, rumble
from sfx import bell_voice, burst, choir, sparkle, thud, voiced
from synth import (TAU, add, buf, env_apply, highpass, midi_hz, noise, onepole_lp, partials, reverb, reverse, smooth_random,
                   soft_clip, svf, tone)

# A little girl's vowels (formants a good deal higher than a woman's) and a woman's hum.
CH_EE = [(420, 4.0, 1.0), (2900, 7.0, 0.45), (3600, 8.0, 0.25)]
CH_EH = [(700, 4.0, 1.0), (2300, 6.0, 0.5), (3300, 8.0, 0.25)]
CH_AH = [(1000, 4.0, 1.0), (1500, 5.0, 0.6), (3200, 7.0, 0.25)]
CH_OO = [(380, 3.0, 1.0), (1100, 4.0, 0.3), (2900, 6.0, 0.08)]
F_HUM = [(290, 2.2, 1.0), (1250, 4.0, 0.12), (2650, 6.0, 0.05)]
F_AH = [(850, 4.0, 1.0), (1220, 5.0, 0.55), (2810, 7.0, 0.25)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def _sum(*parts):
    n = max(len(p) for p in parts)
    out = [0.0] * n
    for p in parts:
        for i, v in enumerate(p):
            out[i] += v
    return out


# --- space ------------------------------------------------------------------------------------------------------------

def echo(x, sr, delay, fb=0.4, wet=0.35, lp=3000.0, tail=None):
    """A feedback delay whose repeats darken (a one-pole low-pass in the loop). Appends `tail` seconds (default: until the
    repeats are ~40 dB down)."""
    d = max(1, int(delay * sr))
    if tail is None:
        tail = delay * max(1.0, math.log(0.01) / math.log(max(0.05, fb)))
    y = x + [0.0] * int(tail * sr)
    line = [0.0] * d
    a = 1 - math.exp(-TAU * lp / sr)
    s = 0.0
    idx = 0
    out = [0.0] * len(y)
    for i, v in enumerate(y):
        o = line[idx]
        s += (o - s) * a
        line[idx] = v + s * fb
        out[i] = v + s * wet
        idx += 1
        if idx == d:
            idx = 0
    return out


def void_room(x, sr, wet=0.3, tail=1.8, size=0.95, damp=0.45, predelay=0.04):
    """Her space: a vast, dark room (long tail, high end damped)."""
    return reverb(x, sr, size=size, damp=damp, wet=wet, tail=tail, predelay=predelay)


def pre_swell(x, sr, lead=0.8, wet=0.5, size=0.9, damp=0.4):
    """Reverse reverb: the room's tail played backwards, so the sound swells up out of nothing before it arrives. Returns the
    swell with the dry sound placed after it (the dry part starts at `lead` seconds)."""
    rev = reverse(x)
    r = reverb(rev, sr, size=size, damp=damp, wet=wet, tail=lead)
    for i, v in enumerate(rev):
        r[i] -= v
    out = reverse(r)
    add(out, x, lead, 1.0, sr)
    return out


# --- building blocks --------------------------------------------------------------------------------------------------

def grind(sr, rng, T, fc=180, gain=1.0):
    """Stone grinding on stone: a low rough band of noise, juddering, with grit crackling in it."""
    x = svf(noise(int(sr * T), rng), sr, fc, 1.4, "band")
    jud = smooth_random(sr, T, rng, 16, 0.25, 1.0)
    x = [v * jud(i / sr) for i, v in enumerate(x)]
    add(x, crackle(sr, rng, T, 0.05, fc * 5, 0.9), 0, 0.5, sr)
    add(x, onepole_lp(noise(int(sr * T), rng), sr, fc * 0.5), 0, 0.6, sr)
    return [v * gain for v in x]


def rubble(sr, rng, T, count, lo=350, hi=2600, fall=0.6, gain=1.0):
    """Rubble falling: `count` stones knocking, thick at first and thinning out (exponential spacing)."""
    x = buf(sr, T)
    for _ in range(count):
        at = min(T - 0.06, -math.log(1 - rng.random() * 0.97) * T * fall * 0.35)
        fc = rng.uniform(lo, hi)
        add(x, burst(sr, rng, 0.05, fc, rng.uniform(1.5, 3.5), "band", rng.uniform(0.004, 0.018)), at,
            rng.uniform(0.2, 1.0) * gain, sr)
        if rng.random() < 0.3:
            add(x, thud(sr, rng.uniform(140, 260), rng.uniform(70, 120), 0.03, 0.1), at, 0.3 * gain, sr)
    return x


def inhale(sr, rng, T, lo=700, hi=2600, gain=1.0):
    """A long breath drawn in: noise through a band rising as the lungs fill, swelling, cut at the top."""
    x = svf(noise(int(sr * T), rng), sr, lambda t: lo * (hi / lo) ** (t / T), 1.1, "band")
    rough = smooth_random(sr, T, rng, 22, 0.6, 1.0)
    return env_apply([v * rough(i / sr) * gain for i, v in enumerate(x)], sr,
                     lambda t: (min(1.0, t / (T * 0.85)) ** 1.6) * min(1.0, max(0.0, T - t) / 0.06))


def ghost_tone(sr, rng, T, f0, f1, vib=5.0, depth=0.02):
    """A wavering, hollow sine with a soft fifth: the sound of a soul."""
    n = int(sr * T)
    out = [0.0] * n
    p = 0.0
    q = 0.0
    wob = smooth_random(sr, T, rng, 3, 0.6, 1.0)
    for i in range(n):
        t = i / sr
        f = f0 * (f1 / f0) ** (t / T) * (1 + depth * math.sin(TAU * vib * t))
        p += TAU * f / sr
        q += TAU * f * 1.5 / sr
        out[i] = (math.sin(p) + 0.25 * math.sin(q) + 0.12 * math.sin(2 * p)) * wob(t)
    return out


def bell_glide(sr, f0, T, fall=0.88, spec=None):
    """A great bell whose whole voice sinks as it rings (hum, prime, tierce, quint, nominal, upper partials)."""
    spec = spec or ((0.5, 0.9, 3.4), (1.0, 1.0, 2.6), (1.19, 0.55, 2.0), (1.5, 0.35, 1.6), (2.0, 0.6, 1.3), (2.66, 0.25, 0.8),
                    (3.01, 0.2, 0.6), (4.16, 0.1, 0.4))
    n = int(sr * T)
    out = [0.0] * n
    sink = fall ** (1.0 / n)
    for r, a, d in spec:
        k = math.exp(-1.0 / (d * sr))
        for beat, share in ((0.0, 1.0), (0.9, 0.6)):
            p = 0.0
            env = a * share
            w = TAU * (f0 * r + beat) / sr
            limit = min(n, int(d * sr * 8))
            for i in range(limit):
                p += w
                out[i] += math.sin(p) * env
                env *= k
                w *= sink
    att = int(0.004 * sr)
    for i in range(att):
        out[i] *= i / att
    return out


def saw_glide(sr, T, f0, f1, cents=0.0, phase=0.0):
    """A naive sawtooth sliding exponentially from f0 to f1 (soften it with a low-pass)."""
    n = int(sr * T)
    out = [0.0] * n
    p = phase % 1.0
    k = 2 ** (cents / 1200)
    ratio = (f1 / f0) ** (1.0 / max(1, n))
    f = f0 * k
    for i in range(n):
        out[i] = 2 * p - 1
        p += f / sr
        if p >= 1.0:
            p -= 1.0
        f *= ratio
    return out


def heart(sr, rng, strength=1.0, pitch=1.0):
    """One heartbeat, lub-dub: two muffled low knocks, the second smaller."""
    x = buf(sr, 0.7)
    for at, g, f0 in ((0.0, 1.0, 62), (0.24, 0.6, 74)):
        add(x, tone(sr, f0 * pitch, 0.4, decay=0.08, glide=f0 * pitch * 0.62, attack=0.004), at, g * strength, sr)
        add(x, burst(sr, rng, 0.08, 330 * pitch, 1.2, "band", 0.02), at, 0.45 * g * strength, sr)   # the knock you hear
        th = onepole_lp(onepole_lp(noise(int(sr * 0.12), rng), sr, 160), sr, 160)
        add(x, env_apply(th, sr, lambda t: min(1.0, t / 0.006) * math.exp(-t / 0.03)), at, 2.5 * g * strength, sr)
    return x


def her_breath(sr, rng, T, gain=1.0):
    """A woman's breath, very close: soft noise through an open vowel."""
    br = svf(noise(int(sr * T), rng), sr, 1500, 0.7, "band")
    return env_apply(br, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.5 * gain)


# --- her voice --------------------------------------------------------------------------------------------------------

def whisper_(sr, rng):
    """Amara whispers: a woman's breathy words close to the ear, a darker second whisper under them, a breath before, and the
    room swelling up into them backwards."""
    T = 2.4
    x = buf(sr, T)
    add(x, her_breath(sr, rng, 0.35, 0.5), 0.0, 1.0, sr)
    w1 = whisper(sr, rng, T - 0.4, 7, 1.0)
    add(x, w1, 0.3, 1.0, sr)
    w2 = onepole_lp(whisper(sr, rng, T - 0.5, 5, 1.0), sr, 1800)
    add(x, w2, 0.38, 0.55, sr)
    # The faintest voice under the breath: she is almost speaking aloud.
    v = voiced(sr, rng, T - 0.6, lambda t: 185 * (1 - 0.06 * t), F_HUM, 2.5, _env(0.3, T - 1.4, 0.5))
    add(x, v, 0.35, 0.12, sr)
    return void_room(pre_swell(x, sr, 0.6, 0.35), sr, wet=0.22, tail=1.4)


def giggle(sr, rng):
    """A little girl's giggle: quick breathy "hee-hee"s tumbling down, a last one rising -- eerie: it swells up out of the
    dark backwards and echoes away in a room much too large."""
    x = buf(sr, 1.4)
    f = 520
    t = 0.0
    steps = (0.105, 0.1, 0.098, 0.104, 0.115, 0.13)
    for i, st in enumerate(steps):
        h = svf(noise(int(sr * 0.04), rng), sr, 2400, 0.8, "band")
        add(x, env_apply(h, sr, lambda u: min(1.0, u / 0.006) * math.exp(-u / 0.014)), t, 0.35, sr)
        d = st * 0.9
        add(x, syllable(sr, rng, d, glide(f * 1.12, f * 0.9, d), CH_EE if i % 2 else CH_EH, 0.8, 0.006, d * 0.38, 0.03),
            t + 0.012, 1.0 - 0.07 * i, sr)
        f *= 0.95
        t += st
    # The last one rises, longer: "hee?"
    d = 0.24
    add(x, syllable(sr, rng, d, glide(f * 0.95, f * 1.35, d, 0.7), CH_EE, 0.9, 0.01, d * 0.5, 0.02), t + 0.04, 0.8, sr)
    y = echo(x, sr, 0.27, fb=0.42, wet=0.32, lp=2200)
    return void_room(pre_swell(y, sr, 0.7, 0.4), sr, wet=0.3, tail=1.8)


def child_scream(sr, rng):
    """A child's scream: a gasp, then a high voice shooting up and holding, shaking, two throats at once, in a cold room."""
    T = 1.7
    x = buf(sr, T + 0.2)
    gasp = svf(noise(int(sr * 0.16), rng), sr, lambda t: 1400 + 3000 * t / 0.16, 1.2, "band")
    add(x, env_apply(gasp, sr, lambda t: (t / 0.16) ** 2), 0.0, 0.4, sr)

    def f0(t, k=1.0):
        rise = 520 * (980 / 520) ** min(1.0, t / 0.22)
        fall = 1.0 - 0.28 * max(0.0, (t - 1.1) / 0.5)
        return rise * fall * k * (1 + 0.03 * math.sin(TAU * 7.2 * t))

    env = _env(0.04, 1.05, 0.5)
    for k, g, at in ((1.0, 1.0, 0.16), (1.035, 0.55, 0.175)):
        v = voiced(sr, rng, T - 0.15, lambda t, k=k: f0(t, k), CH_AH, 1.3, env)
        add(x, rasp(v, 1.8), at, g, sr)
    shriek = svf(noise(int(sr * (T - 0.2)), rng), sr, 3200, 1.6, "band")
    add(x, env_apply(shriek, sr, env), 0.18, 0.18, sr)
    return void_room(x, sr, wet=0.24, tail=1.4)


def hum(sr, rng):
    """She hums a lullaby to herself: a few soft notes in a minor key, the last left hanging, the void humming an octave below."""
    tune = [(62, 0.5), (65, 0.42), (64, 0.46), (62, 0.42), (60, 0.5), (61, 0.95)]
    T = sum(d for _, d in tune) + 0.15
    starts = []
    acc = 0.0
    for m, d in tune:
        starts.append((acc, midi_hz(m)))
        acc += d

    def f0(t):
        f = starts[0][1]
        for i, (s, hz) in enumerate(starts):
            if t >= s:
                prev = starts[i - 1][1] if i else hz
                k = 0.5 - 0.5 * math.cos(math.pi * min(1.0, (t - s) / 0.08))
                f = prev + (hz - prev) * k
        return f * (1 + 0.012 * math.sin(TAU * 4.8 * t) * min(1.0, t / 0.8))

    def env(t):
        e = min(1.0, t / 0.12) * min(1.0, max(0.0, T - 0.05 - t) / 0.45)
        for s, _ in starts[1:]:
            if s - 0.03 < t < s + 0.05:
                e *= 0.8
        return e

    x = voiced(sr, rng, T, f0, F_HUM, 0.5, env)
    low = voiced(sr, rng, T, lambda t: f0(t) * 0.5, F_HUM, 0.8, env)
    add(x, onepole_lp(low, sr, 700), 0.0, 0.3, sr)
    return void_room(x, sr, wet=0.2, tail=1.4)


# --- her power --------------------------------------------------------------------------------------------------------

def tk(sr, rng):
    """Telekinesis: stone grinding loose, a rising whoosh as it lifts, the low hum of the power holding it."""
    T = 2.3
    x = buf(sr, T + 0.2)
    g = grind(sr, rng, 1.6, 170, 1.0)
    add(x, env_apply(g, sr, lambda t: min(1.0, t / 0.15) * max(0.0, 1 - t / 1.6) ** 0.7), 0.0, 0.9, sr)
    add(x, whoosh(sr, rng, 2.0, 140, 1500, 1.3, 0.8), 0.2, 1.1, sr)
    sub = tone(sr, 42, T, glide=86, attack=0.3)
    add(x, env_apply(sub, sr, lambda t: min(1.0, t / 0.4) * max(0.0, 1 - t / T)), 0.0, 0.3, sr)
    power = onepole_lp(saw_glide(sr, T, 55, 82), sr, 320)
    add(x, env_apply(power, sr, lambda t: min(1.0, t / 0.6) * max(0.0, 1 - t / T) * (0.75 + 0.25 * math.sin(TAU * 6 * t))),
        0.1, 0.25, sr)
    add(x, rubble(sr, rng, 0.8, 10, 500, 2400, 0.5, 0.35), 0.05, 1.0, sr)
    return void_room(x, sr, wet=0.16, tail=0.9, size=0.8)


def drain(sr, rng):
    """A soul drawn out: a long breathy inhale, and a ghostly tone rising out of the victim, swelling up backwards and gone."""
    T = 2.0
    x = buf(sr, T + 0.3)
    add(x, inhale(sr, rng, T, 600, 2800, 1.0), 0.0, 0.8, sr)
    gt = ghost_tone(sr, rng, T * 0.95, 300, 690, 5.5, 0.025)
    add(x, env_apply(gt, sr, lambda t: (min(1.0, t / 1.4) ** 2) * min(1.0, max(0.0, T * 0.95 - t) / 0.12)), 0.05, 0.35, sr)
    add(x, onepole_lp(whisper(sr, rng, T - 0.3, 6, 1.0), sr, 2400), 0.2, 0.35, sr)
    # The soul leaves: a soft hollow pop and a little shimmer.
    add(x, thud(sr, 240, 110, 0.04, 0.2), T - 0.02, 0.35, sr)
    add(x, sparkle(sr, rng, 5, 0.0, 0.25, 0.6, 2800, 5200, rise=True), T - 0.05, 0.12, sr)
    return void_room(pre_swell(x, sr, 0.5, 0.3), sr, wet=0.22, tail=1.3)


def strike(sr, rng):
    """Her blow: a dark rush of air, a heavy impact with a hard crack on its edge, the black hiss of the void after it."""
    x = buf(sr, 1.4)
    add(x, whoosh(sr, rng, 0.3, 220, 1100, 1.2, 0.9), 0.0, 0.6, sr)
    at = 0.27
    add(x, thud(sr, 95, 34, 0.22, 0.9), at, 1.3, sr)
    add(x, burst(sr, rng, 0.3, 320, 0.9, "band", 0.07), at, 0.9, sr)
    add(x, burst(sr, rng, 0.04, 2600, 0.8, "high", 0.008), at, 0.6, sr)
    hiss = svf(noise(int(sr * 0.9), rng), sr, lambda t: 1500 * (0.4 ** min(1.0, t / 0.9)), 1.4, "band")
    flick = smooth_random(sr, 0.9, rng, 30, 0.3, 1.0)
    add(x, env_apply(hiss, sr, lambda t: flick(t) * math.exp(-t / 0.3)), at + 0.02, 0.35, sr)
    return void_room(soft_clip(x, 1.6), sr, wet=0.16, tail=0.9, size=0.85)


def wound(sr, rng):
    """God's light tears her: a bright, pure tone rings out -- then cracks, flickering, and fizzes away into sparks."""
    T = 2.0
    x = buf(sr, T)
    f = 1318.5
    bright = partials(sr, 1.6, [(f, 1.0, 1.4), (f * 2, 0.45, 0.9), (f * 3, 0.2, 0.6), (f * 1.5, 0.3, 1.2), (f + 2.2, 0.5, 1.3)],
                      attack=0.004)
    crack_at = 0.42
    gate = smooth_random(sr, 1.6, rng, 45, 0.0, 1.0)
    add(x, env_apply(bright, sr, lambda t: 1.0 if t < crack_at else (gate(t) ** 2) * math.exp(-(t - crack_at) / 0.35)), 0.0,
        0.7, sr)
    add(x, tone(sr, 659.25, 0.8, decay=0.4, attack=0.01), 0.0, 0.2, sr)
    # The crack: glass splintering, then the fizz.
    for i in range(10):
        add(x, burst(sr, rng, 0.015, rng.uniform(2600, 6500), 1.3, "band", 0.002), crack_at + 0.012 * i + rng.uniform(0, 0.02),
            rng.uniform(0.4, 0.9), sr)
    fz = highpass(noise(int(sr * 1.4), rng), sr, 5000)
    pops = [v if rng.random() < 0.08 else v * 0.15 for v in fz]
    add(x, env_apply(pops, sr, lambda t: math.exp(-t / 0.4)), crack_at, 0.5, sr)
    add(x, crackle(sr, rng, 1.0, 0.12, 4800, 0.9, 0.35), crack_at, 0.5, sr)
    add(x, sparkle(sr, rng, 10, 0.0, 0.8, 1.3, 2600, 7000, rise=False), crack_at, 0.25, sr)
    return void_room(x, sr, wet=0.2, tail=1.2, size=0.85, damp=0.3)


def wall(sr, rng):
    """A wall of smoke rising: a deep roar of air swelling up out of the ground, gusting, a rumble under it."""
    T = 3.0
    x = buf(sr, T)
    add(x, onepole_lp(whoosh(sr, rng, 2.8, 55, 280, 0.8, 0.45), sr, 500), 0.0, 2.2, sr)
    add(x, whoosh(sr, rng, 2.6, 260, 1500, 1.0, 0.5), 0.1, 0.5, sr)
    gust = smooth_random(sr, T, rng, 5, 0.6, 1.0)
    add(x, [v * gust(i / sr) for i, v in enumerate(rumble(sr, rng, T, 55, 1.6, 5))], 0.0, 2.2, sr)
    add(x, crackle(sr, rng, 2.2, 0.03, 900, 0.8, 1.0), 0.2, 0.3, sr)
    x = env_apply(x, sr, lambda t: min(1.0, t / 0.5))
    return void_room(soft_clip(x, 1.4), sr, wet=0.2, tail=1.2)


def crumble(sr, rng):
    """The ground unmade: stones knocking loose and falling away into nothing, a low rumble rolling under them."""
    T = 2.6
    x = buf(sr, T)
    add(x, thud(sr, 80, 38, 0.2, 0.7), 0.0, 0.9, sr)
    add(x, rubble(sr, rng, T, 70, 300, 2600, 0.7, 0.7), 0.02, 1.0, sr)
    add(x, rubble(sr, rng, T, 40, 2400, 5200, 0.5, 0.3), 0.04, 1.0, sr)   # grit
    add(x, rumble(sr, rng, T, 60, 1.3, 5), 0.0, 1.4, sr)
    return void_room(x, sr, wet=0.16, tail=1.0, size=0.85)


def island(sr, rng):
    """An island sinks a step: rock grinding against rock, a heavy thud as it drops, rubble scattering off its edge."""
    T = 2.6
    x = buf(sr, T)
    g = grind(sr, rng, 0.55, 120, 1.0)
    add(x, env_apply(g, sr, lambda t: min(1.0, t / 0.1)), 0.0, 1.0, sr)
    at = 0.5
    add(x, thud(sr, 62, 24, 0.45, 1.6), at, 1.3, sr)
    add(x, burst(sr, rng, 0.4, 160, 0.8, "band", 0.14), at, 1.1, sr)
    add(x, burst(sr, rng, 0.06, 1200, 0.9, "band", 0.015), at, 0.5, sr)
    add(x, rubble(sr, rng, 1.8, 40, 300, 2400, 0.6, 0.85), at + 0.05, 1.0, sr)
    add(x, rumble(sr, rng, 2.0, 50, 0.9, 4), at, 2.2, sr)
    return void_room(soft_clip(x, 1.5), sr, wet=0.18, tail=1.1)


def pull(sr, rng):
    """The black sun pulls: a low vortex winding up -- a tone rising, swirling ever faster, air sucked around it."""
    T = 3.0
    x = buf(sr, T + 0.1)
    n = int(sr * T)
    v = [0.0] * n
    p = 0.0
    sw = 0.0
    for i in range(n):
        t = i / sr
        f = 36 * (100 / 36) ** ((t / T) ** 1.3)
        rate = 1.5 + 9 * (t / T) ** 1.5
        sw += TAU * rate / sr
        p += TAU * f / sr
        v[i] = (math.sin(p) + 0.4 * math.sin(2 * p + 0.3) + 0.2 * math.sin(3 * p)) * (0.65 + 0.35 * math.sin(sw))
    env = lambda t: (min(1.0, t / (T * 0.9)) ** 1.5) * min(1.0, max(0.0, T - t) / 0.18)  # noqa: E731
    add(x, env_apply(v, sr, env), 0.0, 0.7, sr)

    def fc(t):
        rate = 1.5 + 9 * (t / T) ** 1.5
        return (130 + 700 * (t / T) ** 1.4) * (1 + 0.45 * math.sin(TAU * rate * t))

    air = svf(noise(n, rng), sr, fc, 2.2, "band")
    add(x, env_apply(air, sr, env), 0.0, 0.7, sr)
    add(x, reverse(onepole_lp(whisper(sr, rng, 1.6, 6, 1.0), sr, 2000)), T - 1.7, 0.25, sr)
    return void_room(soft_clip(x, 1.3), sr, wet=0.2, tail=1.0)


def starfall(sr, rng):
    """A falling star: a thin whistle sliding down out of the sky, a soft impact, and a chime ringing where it lands."""
    T = 1.3
    x = buf(sr, 3.6)
    wh = tone(sr, 3400, T, glide=620, attack=0.05)
    add(x, env_apply(wh, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 0.8 * (1 + 0.1 * math.sin(TAU * 9 * t))), 0.0,
        0.35, sr)
    air = svf(noise(int(sr * T), rng), sr, lambda t: 3400 * (620 / 3400) ** (t / T), 6.0, "band")
    add(x, env_apply(air, sr, lambda t: (t / T) ** 1.5), 0.0, 0.35, sr)
    add(x, thud(sr, 130, 50, 0.08, 0.4), T, 0.8, sr)
    add(x, burst(sr, rng, 0.12, 700, 0.9, "band", 0.035), T, 0.45, sr)
    add(x, bell_voice(sr, midi_hz(91), 2.2, bright=0.7, beat=1.6), T + 0.01, 0.35, sr)
    add(x, bell_voice(sr, midi_hz(98), 1.8, bright=0.5, beat=2.1), T + 0.03, 0.18, sr)
    add(x, sparkle(sr, rng, 8, 0.0, 0.6, 1.4, 3000, 7000, rise=False), T, 0.18, sr)
    return void_room(x, sr, wet=0.25, tail=1.3, damp=0.3)


def countdown(sr, rng):
    """The sun dims: a deep bell struck once, its whole voice sinking as it rings out."""
    x = buf(sr, 4.0)
    add(x, bell_glide(sr, 98.0, 4.0, 0.86), 0.0, 0.6, sr)
    strike = partials(sr, 1.2, [(98.0 * r, a, d) for r, a, d in ((5.42, 0.3, 0.5), (6.8, 0.22, 0.35), (8.93, 0.15, 0.25))])
    add(x, strike, 0.0, 0.5, sr)
    add(x, thud(sr, 70, 40, 0.25, 0.8), 0.0, 0.6, sr)
    add(x, burst(sr, rng, 0.05, 1800, 0.8, "band", 0.01), 0.0, 0.25, sr)
    return void_room(x, sr, wet=0.25, tail=1.6, damp=0.5)


def heartbeat(sr, rng):
    """A slow, dying heartbeat: two beats, the second weaker, later and lower."""
    x = buf(sr, 2.0)
    add(x, heart(sr, rng, 1.0, 1.0), 0.0, 1.0, sr)
    add(x, heart(sr, rng, 0.6, 0.93), 1.2, 1.0, sr)
    return reverb(x, sr, size=0.5, damp=0.6, wet=0.08, tail=0.3)


def enrage(sr, rng):
    """A roar of the void: the air sucked in, then a distorted low choir and a growl tearing out together."""
    T = 3.0
    x = buf(sr, T + 0.4)
    suck = reverse(env_apply(onepole_lp(noise(int(sr * 0.45), rng), sr, 900), sr, lambda t: math.exp(-t / 0.12)))
    add(x, suck, 0.0, 0.8, sr)
    at = 0.42
    ch = choir(sr, rng, (36, 43, 48, 49, 55), T - 0.4, "ah", voices=4, detune=26, vib=(4.0, 18.0))
    add(x, env_apply(ch, sr, _env(0.12, T - 1.4, 0.9)), at, 3.0, sr)
    g = voiced(sr, rng, T - 0.4, glide(78, 55, T - 0.4, 0.7), F_AH, 1.4, _env(0.06, T - 1.5, 0.9))
    add(x, rasp(g, 2.6), at, 0.8, sr)
    add(x, env_apply(svf(noise(int(sr * (T - 0.4)), rng), sr, 420, 0.8, "band"), sr, _env(0.08, T - 1.6, 1.0)), at, 0.5, sr)
    add(x, thud(sr, 70, 30, 0.3, 1.0), at, 0.8, sr)
    return void_room(soft_clip(x, 2.2), sr, wet=0.2, tail=1.2)


def sun_die(sr, rng):
    """The sun gutters: a huge chord sinking an octave and more, darkening, flickering faster and deeper until it goes out."""
    T = 4.2
    x = buf(sr, T + 0.2)
    drone = buf(sr, T)
    for m, g in ((48, 1.0), (55, 0.8), (60, 0.7), (63, 0.5), (36, 0.9)):
        f = midi_hz(m)
        for c in (-9, 9):
            add(drone, saw_glide(sr, T, f, f * 0.42, c, rng.random()), 0, g * 0.18, sr)
    drone = svf(drone, sr, lambda t: 2600 * (0.15 ** (t / T)), 0.9, "low")
    flick = smooth_random(sr, T, rng, 14, 0.0, 1.0)

    def gate(t):
        depth = min(1.0, (t / T) ** 1.2 * 1.1)
        g = flick(t)
        g = 1.0 if g > 0.35 + 0.4 * depth else 0.25 + 0.75 * (g / (0.35 + 0.4 * depth)) ** 2
        return (1 - depth) + depth * g

    add(x, env_apply(drone, sr, lambda t: gate(t) * min(1.0, t / 0.15) * min(1.0, max(0.0, T - t) / 0.4)), 0.0, 1.0, sr)
    sub = tone(sr, 55, T, glide=24, attack=0.1)
    add(x, env_apply(sub, sr, lambda t: min(1.0, max(0.0, T - t) / 0.6)), 0.0, 0.35, sr)
    add(x, crackle(sr, rng, T, 0.04, 1600, 0.8), 0.0, 0.3, sr)
    return void_room(soft_clip(x, 1.4), sr, wet=0.24, tail=1.4)


# --- the end ------------------------------------------------------------------------------------------------------------

def reconcile(sr, rng):
    """Light meets darkness: a low minor chord whose third slowly rises into the major, a warm choir swelling over it, and a
    soft shimmer of bells as they meet."""
    T = 5.6
    x = buf(sr, T)
    # The darkness: D minor, low, the F as its own voice so it can rise to F#.
    for m in (38, 45, 50):
        add(x, env_apply(onepole_lp(saw_glide(sr, T, midi_hz(m), midi_hz(m), rng.uniform(-6, 6), rng.random()), sr, 700), sr,
                         _env(1.0, T - 2.6, 1.6)), 0.0, 0.22, sr)
    third = saw_glide(sr, T, midi_hz(53), midi_hz(53), 4, rng.random())
    n = len(third)
    p = 0.0
    for i in range(n):           # F -> F# between 1.8 s and 2.8 s
        t = i / sr
        k = min(1.0, max(0.0, (t - 1.8) / 1.0))
        k = 0.5 - 0.5 * math.cos(math.pi * k)
        p += midi_hz(53 + k) / sr
        p -= int(p)
        third[i] = 2 * p - 1
    add(x, env_apply(onepole_lp(third, sr, 900), sr, _env(1.0, T - 2.6, 1.6)), 0.0, 0.22, sr)
    # The light: a warm choir on D major, swelling in as the third turns.
    ch = choir(sr, rng, (62, 66, 69, 74), T - 1.2, "oh", voices=3, detune=10)
    add(x, env_apply(ch, sr, lambda t: (min(1.0, t / 2.0) ** 2) * min(1.0, max(0.0, T - 1.2 - t) / 1.6)), 1.2, 1.6, sr)
    add(x, sparkle(sr, rng, 10, 0.0, 1.6, 2.8, 2400, 6000, rise=True), 2.4, 0.18, sr)
    add(x, bell_voice(sr, midi_hz(86), 2.5, 0.5, 1.2), 2.8, 0.1, sr)
    return void_room(x, sr, wet=0.25, tail=1.2, damp=0.35)


def dawn(sr, rng):
    """Dawn breaks: bells ringing up a bright major chord, a high choir swelling behind them, light glittering, a warm low
    note under it all."""
    T = 5.6
    x = buf(sr, T)
    for i, m in enumerate((79, 83, 86, 91)):
        add(x, bell_voice(sr, midi_hz(m), 3.4, 0.7, 1.4), 0.15 + 0.32 * i, 0.32 - 0.04 * i, sr)
    ch = choir(sr, rng, (55, 62, 67, 71, 74, 79), T - 0.4, "ah", voices=3, detune=11)
    add(x, env_apply(ch, sr, lambda t: (min(1.0, t / 2.4) ** 2) * min(1.0, max(0.0, T - 0.4 - t) / 1.8)), 0.3, 1.8, sr)
    low = tone(sr, midi_hz(43), T - 0.5, attack=1.2)
    add(x, env_apply(low, sr, lambda t: min(1.0, max(0.0, T - 0.5 - t) / 1.8)), 0.3, 0.2, sr)
    add(x, sparkle(sr, rng, 22, 0.8, 4.2, T, 2600, 7600, rise=True), 0.0, 0.2, sr)
    return void_room(x, sr, wet=0.25, tail=1.2, damp=0.3)


def visit(sr, rng):
    """Amara is near: a soft dark shimmer -- a low beating swell, high glassy partials trembling over it, a breath of a
    whisper."""
    T = 3.2
    x = buf(sr, T)
    env = lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.6  # noqa: E731
    for f, g in ((73.4, 0.3), (74.1, 0.25), (110.0, 0.2)):
        add(x, env_apply(tone(sr, f, T), sr, env), 0.0, g, sr)
    for f in (2349.0, 2489.0, 3520.0, 4186.0):
        trem = smooth_random(sr, T, rng, 3, 0.0, 1.0)
        add(x, env_apply(tone(sr, f + rng.uniform(-3, 3), T), sr, lambda t, tr=trem: env(t) * tr(t)), 0.0, 0.11, sr)
    glass = svf(noise(int(sr * T), rng), sr, 900, 6.0, "band")
    flick = smooth_random(sr, T, rng, 4, 0.2, 1.0)
    add(x, env_apply(glass, sr, lambda t: env(t) * flick(t)), 0.0, 0.35, sr)
    add(x, onepole_lp(whisper(sr, rng, 1.6, 4, 1.0), sr, 1600), 0.9, 0.12, sr)
    return void_room(x, sr, wet=0.3, tail=1.4)


def feed(sr, rng):
    """Amara feeds: a long inhale and a crowd of souls whispering as they are pulled into her, dimming as they go; she sighs."""
    T = 2.6
    x = buf(sr, T + 0.8)
    add(x, inhale(sr, rng, T, 500, 2400, 1.0), 0.0, 0.7, sr)
    for i in range(5):
        w = whisper(sr, rng, T - 0.2, 7, 1.0)
        w = svf(w, sr, lambda t, i=i: (3200 - 300 * i) * (0.35 ** (t / T)), 0.8, "low")
        add(x, env_apply(w, sr, lambda t: min(1.0, t / 0.3) * min(1.0, max(0.0, T - 0.25 - t) / 0.3)),
            rng.uniform(0, 0.2), 0.5 - 0.05 * i, sr)
    gt = ghost_tone(sr, rng, T, 420, 260, 4.0, 0.03)
    add(x, env_apply(gt, sr, lambda t: math.sin(math.pi * min(1.0, t / T))), 0.0, 0.15, sr)
    sigh = voiced(sr, rng, 0.7, glide(210, 175, 0.7), F_HUM, 1.6, _env(0.08, 0.2, 0.4))
    add(x, sigh, T + 0.02, 0.35, sr)
    return void_room(x, sr, wet=0.22, tail=1.3)


SOUNDS = {
    "whisper": (whisper_, -3.0), "giggle": (giggle, -2.5), "child_scream": (child_scream, -1.5), "hum": (hum, -3.0),
    "tk": (tk, -1.5), "drain": (drain, -2.0), "strike": (strike, -1.0), "wound": (wound, -1.5), "wall": (wall, -1.5),
    "crumble": (crumble, -1.5), "island": (island, -1.0), "pull": (pull, -1.5), "starfall": (starfall, -2.0),
    "countdown": (countdown, -1.5), "heartbeat": (heartbeat, -1.5), "enrage": (enrage, -1.0), "sun_die": (sun_die, -1.0),
    "reconcile": (reconcile, -2.0), "dawn": (dawn, -2.0), "visit": (visit, -4.0), "feed": (feed, -2.5),
}
