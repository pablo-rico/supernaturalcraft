"""The Four Horsemen's sounds (v0.11): steel, voices, coughs, flies, the clock, the bell of Limbo, the world turning
over, and the steeds. Same conventions as sfx.py: (sr, rng) -> float list, finished and encoded by generate.py.
Deep and ominous: voices are pitched low and given a dark room; the big set pieces carry sub-bass.
"""

import math

from sfx import UH, HUM, bell_voice, burst, thud, voiced
from synth import (TAU, add, buf, env_apply, exp_decay, formants, glottal, midi_hz, noise, onepole_lp, partials, reverb,
                   reverse, saw, smooth_random, soft_clip, svf, tone)

AH = [(700, 4.0, 1.0), (1150, 5.0, 0.5), (2500, 7.0, 0.2)]
OH = [(480, 3.5, 1.0), (850, 4.5, 0.45), (2500, 7.0, 0.12)]
EE = [(300, 3.0, 0.8), (2200, 6.0, 0.35), (2900, 8.0, 0.15)]


def _env(attack, hold, release):
    def f(t):
        if t < attack:
            return t / attack
        if t < attack + hold:
            return 1.0
        return max(0.0, 1 - (t - attack - hold) / release)
    return f


def growl(sr, rng, T, f0, f1, vowel=AH, rough=0.6, breath=0.4):
    """A rough, low voice: a glottal source wobbled by noise (vocal fry) through a vowel, with a sub layer."""
    wob = smooth_random(sr, T, rng, 30, -1, 1)
    v = voiced(sr, rng, T, lambda t: (f0 * (f1 / f0) ** (t / T)) * (1 + rough * 0.08 * wob(t)), vowel, breath,
               _env(0.04, T * 0.5, T * 0.5 - 0.04))
    sub = tone(sr, f0 / 2, T, attack=0.05)
    sub = env_apply(sub, sr, _env(0.05, T * 0.4, T * 0.6 - 0.05))
    x = buf(sr, T)
    add(x, v, 0, 1.0, sr)
    add(x, sub, 0, 0.25, sr)
    return soft_clip(x, 1.0 + rough)


# --- War ----------------------------------------------------------------------------------------------

def sword_clash(sr, rng, variant):
    """Steel on steel: a bright crack, two inharmonic rings that beat, a scrape, a hall."""
    x = buf(sr, 1.6)
    add(x, burst(sr, rng, 0.03, 3000, 0.7, "high", 0.002), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.08, 1800 + 300 * variant, 3.0, "band", 0.015), 0, 0.6, sr)
    base = (1480, 1310, 1620)[variant]
    spec = []
    for r, a, d in ((1.0, 0.5, 0.6), (1.47, 0.35, 0.45), (2.09, 0.25, 0.3), (2.76, 0.18, 0.22), (3.94, 0.1, 0.12)):
        spec += [(base * r, a, d), (base * r + 4.5, a * 0.6, d * 0.9)]
    add(x, partials(sr, 1.4, spec, attack=0.0005), 0.001, 0.5, sr)
    scr = svf(noise(int(sr * 0.25), rng), sr, lambda t: 4500 - 6000 * t, 2.0, "band")
    add(x, env_apply(scr, sr, lambda t: math.exp(-t / 0.08)), 0.02, 0.3, sr)
    add(x, thud(sr, 120, 70, 0.05, 0.2), 0, 0.3, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.18, tail=0.6)


def parry(sr, rng):
    """A blade turned aside: a sliding 'shing' that rises, and a clean ring."""
    x = buf(sr, 1.4)
    sl = svf(noise(int(sr * 0.35), rng), sr, lambda t: 2500 + 9000 * t, 3.0, "band")
    add(x, env_apply(sl, sr, lambda t: min(1, t / 0.05) * math.exp(-t / 0.15)), 0, 0.6, sr)
    add(x, burst(sr, rng, 0.02, 3500, 0.8, "high", 0.002), 0.18, 0.8, sr)
    add(x, partials(sr, 1.1, [(2120, 0.4, 0.7), (2124, 0.3, 0.65), (3350, 0.2, 0.4), (5060, 0.1, 0.25)], attack=0.001), 0.18, 0.6, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.15, tail=0.5)


def war_voice(sr, rng, kind, variant=0):
    if kind == "ambient":
        # A low, amused "hm-hm", rough in the throat.
        x = buf(sr, 1.2)
        for i, (t, f) in enumerate(((0.0, 92 + 6 * variant), (0.28, 86 + 6 * variant))):
            add(x, growl(sr, rng, 0.24, f, f * 0.85, HUM, 0.5, 0.5), t, 1.0 - i * 0.2, sr)
        return reverb(x, sr, size=0.5, damp=0.5, wet=0.15, tail=0.4)
    if kind == "hurt":
        x = growl(sr, rng, 0.42, 120 + 15 * variant, 80, UH, 0.9, 0.7)
        return reverb(x, sr, size=0.4, damp=0.5, wet=0.1, tail=0.25)
    if kind == "death":
        x = buf(sr, 2.6)
        add(x, growl(sr, rng, 2.2, 110, 52, AH, 1.1, 0.6), 0, 1.0, sr)
        add(x, thud(sr, 50, 28, 0.6, 2.0), 0.1, 0.5, sr)
        return reverb(x, sr, size=0.85, damp=0.4, wet=0.3, tail=1.4)
    # rage: a roar that builds, with a grinding layer and a sub drop.
    T = 1.9
    x = buf(sr, T)
    add(x, growl(sr, rng, T, 85, 130, AH, 1.5, 0.9), 0, 1.0, sr)
    add(x, growl(sr, rng, T, 128, 190, OH, 1.2, 0.6), 0.03, 0.45, sr)
    grind = svf(noise(int(sr * T), rng), sr, lambda t: 600 + 900 * t, 1.5, "band")
    add(x, env_apply(grind, sr, _env(0.5, 0.8, 0.6)), 0, 0.3, sr)
    add(x, thud(sr, 60, 35, 0.8, T), 0, 0.5, sr)
    return reverb(soft_clip(x, 1.6), sr, size=0.9, damp=0.35, wet=0.3, tail=1.4)


# --- Famine -------------------------------------------------------------------------------------------

def wheeze(sr, rng, T, lo=900, hi=2600, gain=1.0):
    """A thin whistling breath: band noise with a wandering whistle."""
    wh = smooth_random(sr, T, rng, 6, lo, hi)
    b = svf(noise(int(sr * T), rng), sr, lambda t: wh(t), 6.0, "band")
    return [v * gain for v in env_apply(b, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.5)]


def famine_voice(sr, rng, kind, variant=0):
    if kind == "ambient":
        # A rattling inhale and a long, dry exhale with a hum of hunger in it.
        x = buf(sr, 2.4)
        add(x, wheeze(sr, rng, 0.9, 1200, 3000, 0.6), 0, 1.0, sr)
        rattle = [v * (0.5 + 0.5 * math.sin(TAU * 28 * i / sr)) for i, v in enumerate(svf(noise(int(sr * 0.9), rng), sr, 500, 2.0, "band"))]
        add(x, env_apply(rattle, sr, lambda t: math.sin(math.pi * t / 0.9)), 0, 0.4, sr)
        add(x, growl(sr, rng, 1.2, 105 - 8 * variant, 88, HUM, 0.8, 1.2), 1.0, 0.6, sr)
        return reverb(x, sr, size=0.5, damp=0.55, wet=0.12, tail=0.4)
    if kind == "hurt":
        x = growl(sr, rng, 0.45, 150 + 12 * variant, 110, UH, 1.2, 1.0)
        return reverb(x, sr, size=0.4, damp=0.55, wet=0.1, tail=0.2)
    if kind == "death":
        x = buf(sr, 3.2)
        add(x, growl(sr, rng, 1.4, 130, 70, AH, 1.4, 1.2), 0, 1.0, sr)
        add(x, wheeze(sr, rng, 1.6, 700, 1800, 0.8), 1.2, 1.0, sr)
        return reverb(x, sr, size=0.8, damp=0.45, wet=0.25, tail=1.2)
    if kind == "devour":
        # A great sucking inhale through the teeth; a ghostly choir of what he swallows, pulled down in pitch.
        T = 2.4
        x = buf(sr, T)
        suck = svf(noise(int(sr * T), rng), sr, lambda t: 800 + 3500 * (t / T), 1.2, "band")
        add(x, env_apply(suck, sr, lambda t: min(1.0, t / 0.4) * min(1.0, (T - t) / 0.3)), 0, 0.7, sr)
        for m, d in ((62, 0.0), (65, 7.0), (69, -6.0)):
            s = saw(sr, T, midi_hz(m), d, rng.random(), (5.0, 14.0))
            s = formants(s, sr, OH)
            gl = [v for v in env_apply(s, sr, lambda t: math.sin(math.pi * t / T) * 0.8)]
            # Pulled down: resample faster as it goes (pitch falls into him).
            out = []
            p = 0.0
            while p < len(gl) - 1:
                out.append(gl[int(p)])
                p += 1.0 - 0.45 * (len(out) / len(gl))
            add(x, out[:len(x)], 0, 0.25, sr)
        add(x, thud(sr, 70, 30, 0.5, 1.2), T - 0.9, 0.5, sr)
        return reverb(x, sr, size=0.85, damp=0.35, wet=0.3, tail=1.0)
    # hunger: a deep stomach growl, wet and long.
    T = 1.8
    x = buf(sr, T)
    pitch = smooth_random(sr, T, rng, 5, 50, 95)
    v = glottal(sr, lambda t: pitch(t), T, rng)
    v = svf(v, sr, 380, 2.0, "band")
    gurgle = [a * (0.6 + 0.4 * math.sin(TAU * 11 * i / sr + math.sin(i / sr * 7) * 3)) for i, a in enumerate(v)]
    add(x, env_apply(gurgle, sr, _env(0.2, 0.9, 0.7)), 0, 3.0, sr)
    add(x, env_apply(svf(noise(int(sr * T), rng), sr, 220, 1.5, "band"), sr, _env(0.3, 0.6, 0.9)), 0, 0.6, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.4, damp=0.6, wet=0.12, tail=0.4)


# --- Pestilence ---------------------------------------------------------------------------------------

def cough(sr, rng, variant, count=None):
    """A wet, hacking cough: each burst a glottal stop, a noisy blast through 'uh', a phlegmy rattle."""
    n = count or (2, 3, 1)[variant % 3]
    T = 0.35 * n + 0.6
    x = buf(sr, T)
    t = 0.0
    for i in range(n):
        g = 1.0 - i * 0.18
        d = 0.24 + rng.uniform(-0.03, 0.04)
        blast = formants(noise(int(sr * d), rng), sr, [(500, 2.0, 1.0), (1300, 3.0, 0.6), (2600, 4.0, 0.3)])
        blast = env_apply(blast, sr, lambda tt: min(1.0, tt / 0.008) * math.exp(-tt / 0.07))
        add(x, blast, t, 1.4 * g, sr)
        v = voiced(sr, rng, d, lambda tt, f=190 - 15 * i - 10 * variant: f * (1 - 0.3 * tt / d), UH, 1.5,
                   lambda tt: min(1.0, tt / 0.01) * math.exp(-tt / 0.06))
        add(x, v, t, 0.5 * g, sr)
        rat = [a * (0.5 + 0.5 * math.sin(TAU * 35 * k / sr)) for k, a in enumerate(svf(noise(int(sr * 0.12), rng), sr, 700, 2.0, "band"))]
        add(x, env_apply(rat, sr, lambda tt: math.exp(-tt / 0.05)), t + 0.05, 0.4 * g, sr)
        t += d + rng.uniform(0.05, 0.12)
    # The wheezing gasp after.
    add(x, wheeze(sr, rng, 0.45, 1500, 3200, 0.5), t, 1.0, sr)
    return reverb(x, sr, size=0.45, damp=0.5, wet=0.12, tail=0.35)


def pestilence_voice(sr, rng, kind, variant=0):
    if kind == "ambient":
        # A thick sniff and a congested, muttering wheeze.
        x = buf(sr, 1.6)
        sn = svf(noise(int(sr * 0.35), rng), sr, lambda t: 1800 + 2500 * t, 3.0, "band")
        add(x, env_apply(sn, sr, lambda t: math.sin(math.pi * t / 0.35)), 0.0, 0.8, sr)
        add(x, sn[: int(sr * 0.2)], 0.4, 0.5, sr)
        add(x, growl(sr, rng, 0.6, 140 + 10 * variant, 120, HUM, 0.9, 1.4), 0.75, 0.6, sr)
        return reverb(x, sr, size=0.4, damp=0.55, wet=0.1, tail=0.3)
    if kind == "hurt":
        return cough(sr, rng, 2, 1)
    # death: a choking fit that drowns out.
    x = buf(sr, 3.0)
    add(x, cough(sr, rng, 1, 4), 0, 1.0, sr)
    add(x, growl(sr, rng, 1.2, 120, 60, AH, 1.6, 1.4), 1.6, 0.7, sr)
    return reverb(x, sr, size=0.8, damp=0.45, wet=0.22, tail=0.8)


def fly_buzz(sr, rng):
    """A swarm: forty flies, each a buzzing saw at its own pitch, drifting in and out (2.4 s, loop-friendly)."""
    T = 2.4
    x = buf(sr, T)
    for i in range(40):
        f = rng.uniform(150, 330)
        s = saw(sr, T, f, 0, rng.random(), (rng.uniform(3, 9), rng.uniform(20, 70)))
        amp = smooth_random(sr, T, rng, rng.uniform(1.5, 4), 0.0, 1.0)
        s = env_apply(s, sr, lambda t, a=amp: a(t) ** 2 * (0.5 - 0.5 * math.cos(TAU * t / T)))
        add(x, s, 0, 0.06, sr)
    x = svf(x, sr, 1600, 0.9, "low")
    x = formants(x, sr, [(320, 2.0, 1.0), (900, 2.5, 0.6), (2400, 3.0, 0.25)])
    return x


# --- Death --------------------------------------------------------------------------------------------

def clock_tick(sr, rng, variant):
    """A pocket watch, close to the ear: a tiny bright tick (variant 0) or tock (1), a ghost of an echo."""
    x = buf(sr, 0.5)
    f = 4200 if variant == 0 else 3500
    add(x, burst(sr, rng, 0.012, f, 6.0, "band", 0.0025), 0, 1.0, sr)
    add(x, partials(sr, 0.15, [(f * 0.73, 0.2, 0.02), (f * 1.31, 0.1, 0.015)], attack=0.0003), 0, 0.6, sr)
    add(x, burst(sr, rng, 0.02, 900, 2.0, "band", 0.004), 0.0015, 0.25, sr)
    return reverb(x, sr, size=0.9, damp=0.2, wet=0.35, tail=0.6, predelay=0.04)


def limbo_bell(sr, rng):
    """A huge, distant funeral bell: low strike, slow beating partials, a long grey tail."""
    T = 6.0
    x = buf(sr, T)
    f0 = midi_hz(36)
    spec = []
    for r, a, d in ((0.5, 0.7, 4.5), (1.0, 1.0, 3.8), (1.19, 0.6, 2.6), (1.5, 0.35, 2.0), (2.0, 0.4, 1.6), (2.51, 0.22, 1.1),
                    (3.01, 0.15, 0.8), (4.07, 0.08, 0.5)):
        spec += [(f0 * r, a * 0.5, d), (f0 * r + 0.9 * r, a * 0.5, d * 0.95)]
    add(x, partials(sr, T, spec, attack=0.003), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.06, 900, 1.0, "band", 0.02), 0, 0.5, sr)
    add(x, thud(sr, 55, 40, 1.2, 3.0), 0, 0.5, sr)
    x = onepole_lp(x, sr, 2600)
    return reverb(x, sr, size=1.0, damp=0.5, wet=0.4, tail=2.5, predelay=0.05)


def reap(sr, rng):
    """The scythe through the air: a long rising-then-falling whoosh with a thin metallic sing on its edge."""
    T = 0.9
    x = buf(sr, T)
    w = svf(noise(int(sr * T), rng), sr, lambda t: 400 + 2200 * math.sin(math.pi * t / T), 1.6, "band")
    add(x, env_apply(w, sr, lambda t: math.sin(math.pi * t / T) ** 2), 0, 1.0, sr)
    sing = tone(sr, 2900, T, glide=2300)
    add(x, env_apply(sing, sr, lambda t: math.sin(math.pi * t / T) ** 4), 0, 0.12, sr)
    add(x, thud(sr, 90, 45, 0.1, 0.4), T * 0.45, 0.35, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.2, tail=0.7)


def world_flip(sr, rng):
    """The world turns over: a reversed swell sucked in, a deep boom, the sky groaning back the other way."""
    T = 4.5
    x = buf(sr, T)
    swell = reverse(reverb(svf(noise(int(sr * 0.8), rng), sr, 700, 1.0, "band") + [0.0] * int(sr * 0.2), sr,
                           size=0.95, damp=0.3, wet=0.9, tail=1.2))
    add(x, [v * 0.8 for v in swell], 0, 1.0, sr)
    t0 = len(swell) / sr
    add(x, thud(sr, 48, 22, 1.2, 3.0), t0, 1.2, sr)
    add(x, burst(sr, rng, 0.2, 250, 1.0, "low", 0.15), t0, 0.8, sr)
    groan = saw(sr, T - t0, 55, 0, 0.0, (0.3, 60))
    groan = svf(groan, sr, lambda t: 200 + 300 * math.sin(math.pi * min(1.0, t / 2.5)), 2.0, "low")
    add(x, env_apply(groan, sr, lambda t: math.sin(math.pi * min(1.0, t / (T - t0)))), t0, 0.5, sr)
    return reverb(soft_clip(x, 1.4), sr, size=1.0, damp=0.35, wet=0.35, tail=1.8)


def death_voice(sr, rng, kind, variant=0):
    if kind == "ambient":
        # A slow, cold breath through the nose and a faint, very low hum: almost nothing, and that is the point.
        x = buf(sr, 2.2)
        br = svf(noise(int(sr * 1.6), rng), sr, 900, 1.2, "band")
        add(x, env_apply(br, sr, lambda t: math.sin(math.pi * t / 1.6) ** 2), 0, 0.4, sr)
        add(x, growl(sr, rng, 1.6, 72 + 4 * variant, 66, HUM, 0.3, 0.6), 0.4, 0.5, sr)
        return reverb(x, sr, size=0.95, damp=0.3, wet=0.35, tail=1.4)
    if kind == "hurt":
        # Not pain: displeasure. A dry sharp exhale and a cracking sound like old ice.
        x = buf(sr, 0.8)
        add(x, env_apply(svf(noise(int(sr * 0.3), rng), sr, 1400, 1.5, "band"), sr, lambda t: math.exp(-t / 0.08)), 0, 0.7, sr)
        for i in range(5):
            add(x, burst(sr, rng, 0.01, rng.uniform(2500, 5000), 4.0, "band", 0.003), 0.02 + i * 0.03, 0.4, sr)
        add(x, growl(sr, rng, 0.3, 80, 70, HUM, 0.4, 0.5), 0.05, 0.4, sr)
        return reverb(x, sr, size=0.8, damp=0.35, wet=0.25, tail=0.6)
    # death: a long exhale into an enormous dark room, and the bell, far away.
    x = buf(sr, 4.0)
    add(x, growl(sr, rng, 2.0, 70, 40, OH, 0.6, 1.0), 0, 0.9, sr)
    add(x, [v * 0.3 for v in limbo_bell(sr, rng)][: int(sr * 3.0)], 1.0, 1.0, sr)
    return reverb(x, sr, size=1.0, damp=0.3, wet=0.4, tail=1.5)


def reaper_whisper(sr, rng, variant):
    """Many whispering voices, unvoiced and unintelligible, swirling."""
    T = 2.2
    x = buf(sr, T)
    vowels = [AH, OH, EE, UH, HUM]
    for k in range(6):
        seg = noise(int(sr * T), rng)
        v = vowels[(k + variant) % len(vowels)]
        f = formants(seg, sr, [(fr * rng.uniform(0.9, 1.2), q * 2, g) for fr, q, g in v])
        amp = smooth_random(sr, T, rng, 9, 0, 1)
        f = env_apply(f, sr, lambda t, a=amp: max(0.0, a(t) - 0.35) ** 1.5 * math.sin(math.pi * t / T))
        add(x, f, 0, 0.5, sr)
    x = svf(x, sr, 1200, 0.7, "high")
    return reverb(x, sr, size=0.9, damp=0.3, wet=0.4, tail=1.0)


def reaper_attack(sr, rng):
    """A rush of cold air and a thin dead shriek."""
    T = 0.9
    x = buf(sr, T)
    w = svf(noise(int(sr * T), rng), sr, lambda t: 600 + 4000 * (t / T), 1.4, "band")
    add(x, env_apply(w, sr, lambda t: min(1, t / 0.1) * math.exp(-t / 0.35)), 0, 0.8, sr)
    shriek = voiced(sr, rng, 0.6, lambda t: 620 * (1 - 0.35 * t / 0.6), EE, 2.0, lambda t: min(1, t / 0.05) * math.exp(-t / 0.25))
    add(x, shriek, 0.08, 0.5, sr)
    return reverb(x, sr, size=0.85, damp=0.3, wet=0.35, tail=0.8)


# --- the steeds ---------------------------------------------------------------------------------------

def neigh(sr, rng, variant):
    """A demonic whinny: a high squealing whinny (falling, trilled) over a low growl and a snort."""
    T = 1.6
    x = buf(sr, T)
    hi = (620, 560)[variant]

    def f0(t):
        return hi * (0.55 + 0.45 * math.exp(-t * 1.8)) * (1 + 0.08 * math.sin(TAU * 14 * t))
    w = voiced(sr, rng, 1.2, f0, EE, 1.2, lambda t: min(1.0, t / 0.05) * min(1.0, (1.2 - t) / 0.4))
    add(x, w, 0, 0.7, sr)
    add(x, growl(sr, rng, 1.3, 95, 70, AH, 1.4, 0.8), 0.05, 0.6, sr)
    snort = svf(noise(int(sr * 0.25), rng), sr, 700, 1.0, "band")
    add(x, env_apply(snort, sr, lambda t: min(1, t / 0.01) * math.exp(-t / 0.08)), 1.25, 0.8, sr)
    return reverb(soft_clip(x, 1.3), sr, size=0.6, damp=0.45, wet=0.18, tail=0.6)


def gallop(sr, rng, variant):
    """One gallop stride: four heavy hoofbeats (da-da-da-dum) on hard ground, with a ring of iron shoes."""
    x = buf(sr, 0.7)
    for i, t in enumerate((0.0, 0.07, 0.19, 0.27)):
        g = (0.7, 0.8, 0.85, 1.0)[i] * rng.uniform(0.9, 1.05)
        add(x, thud(sr, 95 - 10 * variant, 55, 0.045, 0.2), t, g, sr)
        add(x, burst(sr, rng, 0.05, 1100, 1.5, "band", 0.01), t, 0.5 * g, sr)
        add(x, partials(sr, 0.12, [(2350 + 120 * i, 0.06, 0.03)]), t + 0.002, g, sr)
    return reverb(x, sr, size=0.35, damp=0.5, wet=0.1, tail=0.2)
