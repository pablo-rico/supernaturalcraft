"""The Author's sound effects: the typewriter, his snap, the rewritten world, his voice and the hunters' arrival.

Each public function takes (sr, rng) and returns a float list; generate.py finishes, writes and encodes it.
Every sound is built in layers -- a transient, a body and a room -- so it reads as a physical thing.
"""

import math

from synth import (TAU, add, buf, env_apply, exp_decay, formants, glottal, midi_hz, noise, onepole_lp,
                   partials, reverb, reverse, saw, smooth_random, soft_clip, svf, tone)


# --- building blocks ----------------------------------------------------------------------------

def burst(sr, rng, seconds, fc, q, mode, tau, attack=0.0):
    """Filtered noise with an exponential tail: clicks, thumps, scrapes."""
    x = noise(int(sr * seconds), rng)
    x = svf(x, sr, fc, q, mode)
    return exp_decay(x, sr, tau, attack)


def thud(sr, f0, f1, tau, seconds):
    """A pitched drop: the body of a knock or an impact."""
    return tone(sr, f0, seconds, decay=tau, glide=f1, attack=0.001)


def typekey(sr, rng, pitch=1.0, weight=1.0, ring=1.0):
    """One typewriter key: finger thock, the typebar striking the platen, its ring, the key's release."""
    x = buf(sr, 0.34)
    # Finger on the keytop and the linkage moving.
    add(x, thud(sr, 210 * weight, 120 * weight, 0.018, 0.1), 0.0, 0.45, sr)
    add(x, burst(sr, rng, 0.08, 760 * weight, 2.2, "band", 0.016), 0.0, 0.55, sr)
    # The typebar hits the platen ~12 ms later: the sharp metallic clack.
    hit = 0.010 + rng.uniform(0, 0.004)
    add(x, burst(sr, rng, 0.03, 3400 * pitch, 0.8, "high", 0.0022), hit, 1.0, sr)
    add(x, burst(sr, rng, 0.06, 2300 * pitch, 5.0, "band", 0.010), hit, 0.75, sr)
    add(x, partials(sr, 0.25, [(1780 * pitch, 0.10 * ring, 0.05), (2960 * pitch, 0.16 * ring, 0.045),
                               (4430 * pitch, 0.10 * ring, 0.03), (6150 * pitch, 0.06 * ring, 0.02)], attack=0.0005), hit, 1.0, sr)
    # The platen's hollow knock under it.
    add(x, burst(sr, rng, 0.06, 420, 3.0, "band", 0.02), hit, 0.35, sr)
    # The key springs back.
    add(x, burst(sr, rng, 0.02, 2600 * pitch, 1.6, "band", 0.004), hit + 0.05 + rng.uniform(0, 0.02), 0.22, sr)
    return reverb(x, sr, size=0.25, damp=0.55, wet=0.07, tail=0.12)


def bell_voice(sr, f0, seconds, bright=1.0, beat=1.3):
    """A small bright bell: inharmonic partials, each split in two slightly detuned copies (the shimmer)."""
    spec = []
    for ratio, amp, decay in ((1.0, 1.0, 1.4), (2.32, 0.55 * bright, 0.75), (4.25, 0.30 * bright, 0.38),
                              (6.63, 0.16 * bright, 0.2), (9.38, 0.07 * bright, 0.1)):
        f = f0 * ratio
        spec.append((f, amp * 0.5, decay))
        spec.append((f + beat * ratio, amp * 0.5, decay * 0.92))
    return partials(sr, seconds, spec, attack=0.0008)


def choir(sr, rng, notes, seconds, vowel, voices=3, detune=9.0, vib=(5.2, 9.0)):
    """A soft synthetic choir: detuned saws per note through a vowel's formants."""
    x = [0.0] * int(sr * seconds)
    for m in notes:
        f = midi_hz(m)
        for v in range(voices):
            d = (v - (voices - 1) / 2) * detune * 2 / max(1, voices - 1)
            s = saw(sr, seconds, f, d, rng.random(), (vib[0] * (0.9 + 0.2 * rng.random()), vib[1]))
            add(x, s, 0, 1.0 / (voices * len(notes)))
    vowels = {
        "ah": [(730, 4.0, 1.0), (1090, 5.0, 0.55), (2440, 7.0, 0.3), (3400, 8.0, 0.12)],
        "oo": [(300, 3.0, 1.0), (870, 4.0, 0.35), (2240, 6.0, 0.1)],
        "oh": [(500, 3.5, 1.0), (880, 4.5, 0.5), (2600, 7.0, 0.15)],
    }
    return formants(x, sr, vowels[vowel])


def sparkle(sr, rng, count, start, end, seconds, lo=2600, hi=7000, rise=True):
    """Little golden pings scattered in time; with `rise` they climb as they go."""
    x = [0.0] * int(sr * seconds)
    for i in range(count):
        t = start + (end - start) * (i + rng.random() * 0.8) / count
        k = (i / max(1, count - 1)) if rise else rng.random()
        f = lo * (hi / lo) ** (0.25 + 0.75 * k * rng.uniform(0.7, 1.0))
        tau = rng.uniform(0.06, 0.2)
        p = partials(sr, tau * 6, [(f, 1.0, tau), (f * 2.76, 0.3, tau * 0.5)], attack=0.002)
        add(x, p, t, rng.uniform(0.06, 0.18), sr)
    return x


# --- the typewriter -----------------------------------------------------------------------------

def type_key(sr, rng, variant):
    pitch, weight, ring = [(1.0, 1.0, 1.0), (0.9, 1.08, 0.8), (1.12, 0.94, 1.15), (0.96, 0.9, 1.3)][variant]
    return typekey(sr, rng, pitch, weight, ring)


def ratchet_click(sr, rng, gain=1.0):
    x = burst(sr, rng, 0.012, 3200 + rng.uniform(-300, 300), 1.2, "band", 0.0018)
    add(x, partials(sr, 0.012, [(3900, 0.12, 0.006)]))
    return [v * gain for v in x]


def carriage(sr, rng, variant):
    """The return lever: the carriage zips back on its ratchet and stops with a clunk (and the line feed)."""
    T = 0.62 if variant == 0 else 0.5
    x = buf(sr, T + 0.45)
    # The line-feed lever first: a short ratcheting push.
    add(x, burst(sr, rng, 0.05, 1400, 2.0, "band", 0.012), 0.0, 0.5, sr)
    add(x, ratchet_click(sr, rng, 0.8), 0.012, 1.0, sr)
    # The zip: clicks accelerating then slowing a little as the carriage arrives.
    t = 0.05
    i = 0
    while t < T - 0.03:
        k = t / T
        gap = 0.019 - 0.011 * math.sin(math.pi * min(1.0, k * 1.1)) + rng.uniform(-0.002, 0.002)
        add(x, ratchet_click(sr, rng, 0.35 + 0.35 * rng.random()), t, 1.0, sr)
        t += gap
        i += 1
    # Friction of the sliding carriage.
    slide = noise(int(sr * T), rng)
    slide = svf(slide, sr, lambda tt: 900 + 1400 * tt / T, 1.4, "band")
    slide = env_apply(slide, sr, lambda tt: min(1.0, tt / 0.06) * max(0.0, 1 - tt / T) ** 0.4)
    add(x, slide, 0.04, 0.22, sr)
    # The stop: a heavy clunk with a metallic shiver.
    add(x, thud(sr, 150, 80, 0.05, 0.3), T, 0.9, sr)
    add(x, burst(sr, rng, 0.12, 600, 1.8, "band", 0.035), T, 0.8, sr)
    add(x, burst(sr, rng, 0.03, 3000, 0.8, "high", 0.003), T, 0.7, sr)
    add(x, partials(sr, 0.5, [(940, 0.12, 0.12), (1515, 0.08, 0.09), (2620, 0.05, 0.06)]), T, 1.0, sr)
    return reverb(x, sr, size=0.35, damp=0.5, wet=0.09, tail=0.25)


def margin_bell(sr, rng):
    x = buf(sr, 1.9)
    add(x, burst(sr, rng, 0.02, 4000, 0.8, "high", 0.0015), 0.0, 0.5, sr)
    add(x, burst(sr, rng, 0.04, 1100, 2.0, "band", 0.008), 0.0, 0.3, sr)
    add(x, bell_voice(sr, 1480, 1.9), 0.001, 0.9, sr)
    return reverb(x, sr, size=0.4, damp=0.4, wet=0.12, tail=0.4)


def backspace(sr, rng):
    """A short reversed whoosh sucks back into a deep mechanical thunk."""
    lead = 0.2
    x = buf(sr, 0.9)
    w = noise(int(sr * lead), rng)
    w = svf(w, sr, lambda t: 350 * (2400 / 350) ** (t / lead), 1.3, "band")
    w = env_apply(w, sr, lambda t: (t / lead) ** 2.2)
    add(x, w, 0.0, 0.55, sr)
    add(x, reverse(burst(sr, rng, 0.12, 2200, 1.0, "high", 0.03)), lead - 0.12, 0.25, sr)
    add(x, thud(sr, 120, 62, 0.06, 0.35), lead, 1.0, sr)
    add(x, burst(sr, rng, 0.12, 480, 1.8, "band", 0.035), lead, 0.8, sr)
    add(x, burst(sr, rng, 0.02, 2800, 0.8, "high", 0.0025), lead, 0.6, sr)
    add(x, partials(sr, 0.4, [(860, 0.14, 0.09), (1290, 0.09, 0.07), (2140, 0.06, 0.05)]), lead, 1.0, sr)
    add(x, burst(sr, rng, 0.03, 1900, 1.4, "band", 0.005), lead + 0.055, 0.35, sr)
    return reverb(x, sr, size=0.5, damp=0.45, wet=0.12, tail=0.3)


def key_impact(sr, rng, variant):
    """A giant typewriter key slams the ground: falling air, a huge low thud, a metal clang, debris."""
    base = 180 if variant == 0 else 150
    fall = 0.22
    x = buf(sr, 2.6)
    air = noise(int(sr * fall), rng)
    air = svf(air, sr, lambda t: 2400 * (300 / 2400) ** (t / fall), 1.1, "band")
    air = env_apply(air, sr, lambda t: (t / fall) ** 2)
    add(x, air, 0.0, 0.35, sr)
    add(x, thud(sr, 72, 30, 0.38, 2.0), fall, 1.0, sr)
    add(x, thud(sr, 150, 60, 0.14, 0.8), fall, 0.45, sr)
    add(x, onepole_lp(burst(sr, rng, 0.6, 900, 0.7, "low", 0.12), sr, 1200), fall, 1.2, sr)
    add(x, burst(sr, rng, 0.05, 2200, 0.8, "high", 0.01), fall, 0.5, sr)
    ratios = (1.0, 1.59, 2.14, 2.92, 3.61, 4.72, 6.1)
    spec = [(base * r * rng.uniform(0.99, 1.01), 0.5 / (1 + i * 0.6), 1.5 / (1 + i * 0.45)) for i, r in enumerate(ratios)]
    add(x, partials(sr, 2.2, spec, attack=0.001), fall + 0.004, 0.42, sr)
    rumble = noise(int(sr * 1.9), rng)
    rumble = svf(rumble, sr, 170, 0.7, "low")
    rumble = env_apply(rumble, sr, lambda t: min(1.0, t / 0.04) * math.exp(-t / 0.6))
    add(x, rumble, fall, 1.4, sr)
    # Debris: grains of grit and splinters, thinning out.
    t = fall + 0.08
    while t < 2.2:
        g = burst(sr, rng, 0.02, rng.uniform(900, 4200), 1.5, "band", rng.uniform(0.002, 0.006))
        add(x, g, t, rng.uniform(0.05, 0.22) * math.exp(-(t - fall) / 0.7), sr)
        t += rng.expovariate(1.0 / (0.018 + 0.05 * (t - fall)))
    x = soft_clip(x, 1.6)
    return reverb(x, sr, size=0.9, damp=0.4, wet=0.22, tail=0.9, predelay=0.012)


# --- paper and ink ------------------------------------------------------------------------------

def page_tear(sr, rng, variant):
    """Paper tearing: a fibrous rasp with irregular crackle, in one pull (1) or two (2)."""
    T = 0.75 if variant == 0 else 0.55
    x = buf(sr, T + 0.2)
    pulls = [(0.0, T)] if variant == 0 else [(0.0, 0.22), (0.3, T)]
    dens = smooth_random(sr, T, rng, 14, 0.3, 1.0)
    for a, b in pulls:
        span = b - a
        rasp = noise(int(sr * span), rng)
        rasp = svf(rasp, sr, lambda t: 2600 + 1600 * dens(a + t), 0.9, "band")
        wob = smooth_random(sr, span, rng, 45, 0.15, 1.0)
        rasp = env_apply(rasp, sr, lambda t: min(1.0, t / 0.02) * min(1.0, (span - t) / 0.03) * wob(t))
        add(x, rasp, a, 0.6, sr)
        body = svf(noise(int(sr * span), rng), sr, 700, 1.0, "band")
        body = env_apply(body, sr, lambda t: min(1.0, t / 0.03) * min(1.0, (span - t) / 0.05))
        add(x, body, a, 0.25, sr)
        t = a
        while t < b:
            g = burst(sr, rng, 0.008, rng.uniform(1800, 6500), 1.2, "band", rng.uniform(0.0008, 0.003))
            add(x, g, t, rng.uniform(0.2, 0.9) * dens(t), sr)
            t += rng.expovariate(1.0 / (0.004 + 0.012 * (1.1 - dens(t))))
    return reverb(x, sr, size=0.3, damp=0.5, wet=0.08, tail=0.2)


def scribble(sr, rng, strokes, T, lo=3200, hi=5200, gain=1.0):
    """Nib strokes on paper: grainy band-passed scratches with tiny ticks at each start."""
    x = buf(sr, T + 0.1)
    t = 0.01
    for _ in range(strokes):
        d = rng.uniform(0.07, 0.17)
        if t + d > T:
            break
        s = noise(int(sr * d), rng)
        fc0, fc1 = rng.uniform(lo, hi), rng.uniform(lo, hi)
        s = svf(s, sr, lambda tt, d=d, a=fc0, b=fc1: a + (b - a) * tt / d, 1.6, "band")
        grain = smooth_random(sr, d, rng, 90, 0.2, 1.0)
        s = env_apply(s, sr, lambda tt, d=d, g=grain: min(1.0, tt / 0.01) * min(1.0, (d - tt) / 0.02) * g(tt))
        add(x, s, t, gain * rng.uniform(0.6, 1.0), sr)
        p = svf(noise(int(sr * d), rng), sr, 1100, 1.0, "band")
        p = env_apply(p, sr, lambda tt, d=d: min(1.0, tt / 0.01) * min(1.0, (d - tt) / 0.02))
        add(x, p, t, gain * 0.18, sr)
        add(x, burst(sr, rng, 0.01, 3500, 1.0, "band", 0.0015), t, gain * 0.3, sr)
        t += d + rng.uniform(0.02, 0.06)
    return x


def write(sr, rng, variant):
    x = scribble(sr, rng, 7 if variant == 0 else 9, 1.0 if variant == 0 else 1.1)
    return reverb(x, sr, size=0.3, damp=0.5, wet=0.07, tail=0.15)


def pen_write(sr, rng):
    """The Author's Pen: a smooth fountain-pen stroke and a little golden sparkle."""
    x = buf(sr, 1.5)
    add(x, scribble(sr, rng, 3, 0.6, 4600, 6400, 0.7), 0.0, 1.0, sr)
    add(x, sparkle(sr, rng, 8, 0.25, 1.0, 1.5, 2800, 6800), 0.0, 1.0, sr)
    add(x, bell_voice(sr, 2350, 1.0, bright=0.5), 0.55, 0.08, sr)
    return reverb(x, sr, size=0.7, damp=0.35, wet=0.2, tail=0.6)


def erase(sr, rng):
    """An eraser's rubbery scrub inside a band-passed whoosh."""
    T = 1.2
    x = buf(sr, T + 0.1)
    w = noise(int(sr * T), rng)
    w = svf(w, sr, lambda t: 500 * 6 ** math.sin(math.pi * t / T), 1.2, "band")
    w = env_apply(w, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.5)
    add(x, w, 0.0, 0.8, sr)
    jit = smooth_random(sr, T, rng, 6, -0.25, 0.25)
    scrub_env = lambda t: (0.5 + 0.5 * math.sin(TAU * 12.5 * t + 3 * jit(t))) ** 2 * math.sin(math.pi * min(1.0, t / T))
    sc = onepole_lp(noise(int(sr * T), rng), sr, 1300)
    sc = env_apply(sc, sr, scrub_env)
    add(x, sc, 0.0, 1.6, sr)
    sq = tone(sr, 1150, T)
    sq = env_apply(sq, sr, lambda t: scrub_env(t) * 0.5)
    add(x, sq, 0.0, 0.05, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.15, tail=0.4)


def echo(sr, rng):
    """Living ink: a dark, wet swell with gurgles rising through it."""
    T = 2.6
    x = buf(sr, T + 0.2)
    dark = noise(int(sr * T), rng)
    dark = svf(dark, sr, lambda t: 110 + 600 * math.sin(math.pi * min(1.0, t / T)) ** 2, 1.4, "low")
    dark = env_apply(dark, sr, lambda t: min(1.0, (t / 1.5) ** 2) * min(1.0, (T - t) / 0.9))
    add(x, dark, 0.0, 1.3, sr)
    for f in (55.0, 55.4, 82.4):
        d = saw(sr, T, f, 0, rng.random(), (0.3, 12))
        d = svf(d, sr, lambda t: 180 + 260 * min(1.0, t / 1.6), 1.0, "low")
        d = env_apply(d, sr, lambda t: min(1.0, t / 1.4) * min(1.0, (T - t) / 0.9))
        add(x, d, 0.0, 0.35, sr)
    for _ in range(46):
        t = T * (0.15 + 0.75 * rng.betavariate(2.2, 2.0))
        f0 = rng.uniform(170, 620)
        d = rng.uniform(0.03, 0.08)
        b = tone(sr, f0, d * 3, decay=d * 0.6, glide=f0 * rng.uniform(1.4, 1.9), attack=0.002)
        add(x, b, t, rng.uniform(0.06, 0.22), sr)
    return reverb(x, sr, size=0.95, damp=0.65, wet=0.3, tail=1.2, predelay=0.02)


# --- the Author ---------------------------------------------------------------------------------

def snap(sr, rng, variant):
    """A finger snap: a dry crack with a short body, then an uneasy, too-long room."""
    x = buf(sr, 0.3)
    hi = 2300 if variant == 0 else 1950
    add(x, burst(sr, rng, 0.02, 1300, 0.8, "high", 0.0016), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, hi, 4.0, "band", 0.011), 0.0, 0.8, sr)
    add(x, burst(sr, rng, 0.06, 950, 2.0, "band", 0.016), 0.0006, 0.55, sr)
    add(x, thud(sr, 240, 160, 0.008, 0.05), 0.0, 0.25, sr)
    x = x + [0.0] * int(sr * 1.4)
    add(x, partials(sr, 1.6, [(3720 if variant == 0 else 3310, 0.02, 0.9), (5530, 0.012, 0.6)], attack=0.03), 0.02, 1.0, sr)
    return reverb(x, sr, size=0.97, damp=0.3, wet=0.32, tail=0.8, predelay=0.025)


def voiced(sr, rng, seconds, f0_curve, vowel, breath=0.25, env=None):
    """A voice: glottal source + aspiration noise through formants."""
    v = glottal(sr, f0_curve, seconds, rng)
    n = onepole_lp(noise(int(sr * seconds), rng), sr, 3000)
    mix = [a * 6.0 + b * breath for a, b in zip(v, n)]
    out = formants(mix, sr, vowel)
    out = onepole_lp(out, sr, 3800)
    return env_apply(out, sr, env) if env else out


HUM = [(250, 2.2, 1.0), (1050, 4.0, 0.12), (2200, 6.0, 0.06)]
UH = [(560, 3.5, 1.0), (1150, 5.0, 0.45), (2500, 7.0, 0.18)]


def hurt(sr, rng, variant):
    """A short pained exhale, "hmph": breath first, then a falling, nasal voice."""
    T = 0.55 if variant == 0 else 0.48
    f_hi, f_lo = (150, 98) if variant == 0 else (172, 112)
    x = buf(sr, T + 0.1)
    br = svf(noise(int(sr * T), rng), sr, 1300, 0.8, "band")
    br = env_apply(br, sr, lambda t: min(1.0, t / 0.015) * math.exp(-t / 0.12))
    add(x, br, 0.0, 0.35, sr)
    v = voiced(sr, rng, T, lambda t: f_hi * (f_lo / f_hi) ** min(1.0, t / T), UH if variant == 0 else HUM, 0.6,
               lambda t: min(1.0, max(0.0, t - 0.02) / 0.04) * math.exp(-max(0.0, t - 0.06) / 0.14))
    add(x, v, 0.0, 1.0, sr)
    return reverb(x, sr, size=0.35, damp=0.5, wet=0.06, tail=0.2)


def laugh(sr, rng):
    """A soft, closed-mouth chuckle: three breathy "hm"s, each a little lower."""
    x = buf(sr, 1.0)
    for i, (t, f) in enumerate(((0.0, 158), (0.2, 146), (0.39, 134))):
        d = 0.15
        br = onepole_lp(noise(int(sr * d), rng), sr, 1600)
        br = env_apply(br, sr, lambda tt: min(1.0, tt / 0.008) * math.exp(-tt / 0.03))
        add(x, br, t, 0.25 * (1 - i * 0.2), sr)
        v = voiced(sr, rng, d, lambda tt, f=f: f * (1 - 0.12 * tt / d), HUM, 0.8,
                   lambda tt: min(1.0, tt / 0.015) * math.exp(-tt / 0.055))
        add(x, v, t + 0.006, 1.0 - i * 0.18, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.08, tail=0.25)


def hum(sr, rng, variant):
    """The Author hums to himself: a few soft notes, the last one left hanging."""
    tunes = [
        [(50, 0.42), (53, 0.36), (52, 0.38), (50, 0.34), (49, 0.62)],          # D F E D C#... unresolved
        [(45, 0.34), (50, 0.4), (53, 0.3), (52, 0.3), (51, 0.7)],               # A D F E Eb: a little wrong
    ][variant]
    T = sum(d for _, d in tunes) + 0.1
    starts = []
    acc = 0.0
    for m, d in tunes:
        starts.append((acc, midi_hz(m)))
        acc += d

    def f0(t):
        f = starts[0][1]
        for i, (s, hz) in enumerate(starts):
            if t >= s:
                prev = starts[i - 1][1] if i else hz
                k = min(1.0, (t - s) / 0.07)
                k = 0.5 - 0.5 * math.cos(math.pi * k)
                f = prev + (hz - prev) * k
        vib = 1 + 0.011 * math.sin(TAU * 5.1 * t) * min(1.0, t / 0.6)
        return f * vib

    def env(t):
        e = min(1.0, t / 0.09) * min(1.0, max(0.0, T - 0.05 - t) / 0.3)
        for s, _ in starts[1:]:
            if s - 0.03 < t < s + 0.05:
                e *= 0.82
        return e

    x = voiced(sr, rng, T, f0, HUM, 0.35, env)
    return reverb(x, sr, size=0.4, damp=0.55, wet=0.1, tail=0.35)


def rewrite(sr, rng, keys):
    """The rules change: a glassy shimmer climbs a fifth while keys flutter underneath."""
    T = 2.6
    x = buf(sr, T + 0.2)
    for i, m in enumerate((74, 81, 86, 90, 93, 98)):
        f = midi_hz(m)
        start = i * 0.12
        d = T - start
        s = tone(sr, f, d, glide=f * 2 ** (7 / 12), phase=rng.random() * TAU)
        rate = rng.uniform(6, 11)
        s = env_apply(s, sr, lambda t, d=d, r=rate: min(1.0, t / 0.6) * min(1.0, (d - t) / 0.7) * (0.75 + 0.25 * math.sin(TAU * r * t)))
        add(x, s, start, 0.12 / (1 + i * 0.25), sr)
    add(x, sparkle(sr, rng, 14, 0.2, 2.2, T + 0.2, 3000, 8000), 0.0, 0.8, sr)
    t = 0.08
    while t < 2.1:
        k = t / 2.1
        g = math.sin(math.pi * k) ** 0.7
        add(x, keys[rng.randrange(len(keys))], t, 0.35 * g * rng.uniform(0.6, 1.0), sr)
        t += rng.uniform(0.04, 0.075)
    sub = tone(sr, midi_hz(38), T)
    sub = env_apply(sub, sr, lambda t: min(1.0, t / 1.2) * min(1.0, (T - t) / 0.6))
    add(x, sub, 0.0, 0.25, sr)
    return reverb(x, sr, size=0.9, damp=0.3, wet=0.3, tail=1.2)


def reveal(sr, rng):
    """Light pours out: a choir swells, opens to "ah" and blooms, with shimmer rising through it."""
    T = 4.2
    x = buf(sr, T + 0.2)
    c = choir(sr, rng, (50, 57, 62, 66, 69, 74), T, "ah", voices=3, detune=10)
    c = svf(c, sr, lambda t: 500 * (9 ** min(1.0, t / 2.8)), 0.8, "low")
    c = env_apply(c, sr, lambda t: min(1.0, (t / 2.8) ** 2) * min(1.0, (T - t) / 0.9))
    add(x, c, 0.0, 2.2, sr)
    add(x, sparkle(sr, rng, 26, 1.2, 3.8, T + 0.2, 2400, 7600), 0.0, 1.0, sr)
    sub = tone(sr, midi_hz(38), T)
    sub = env_apply(sub, sr, lambda t: min(1.0, t / 2.5) * min(1.0, (T - t) / 0.9))
    add(x, sub, 0.0, 0.35, sr)
    air = svf(noise(int(sr * T), rng), sr, lambda t: 1500 + 5000 * min(1.0, t / 3.0), 0.7, "high")
    air = env_apply(air, sr, lambda t: min(1.0, (t / 3.0) ** 3) * min(1.0, (T - t) / 0.8))
    add(x, air, 0.0, 0.07, sr)
    return reverb(x, sr, size=1.0, damp=0.3, wet=0.3, tail=2.0, predelay=0.03)


def crack(sr, rng):
    """The script cracks: a bright porcelain crack over a deep sub hit, then tinkling."""
    x = buf(sr, 1.6)
    t = 0.0
    for i in range(7):
        add(x, burst(sr, rng, 0.015, 2600, 0.8, "high", 0.0015), t, rng.uniform(0.5, 1.0), sr)
        add(x, burst(sr, rng, 0.02, rng.uniform(4000, 8000), 6, "band", 0.004), t, 0.4, sr)
        t += 0.004 + 0.012 * rng.random() * (1 + i * 0.3)
    add(x, partials(sr, 1.2, [(2730, 0.3, 0.45), (3910, 0.22, 0.3), (5470, 0.15, 0.22), (7220, 0.08, 0.14)]), 0.002, 0.6, sr)
    add(x, thud(sr, 58, 28, 0.28, 1.2), 0.0, 1.1, sr)
    add(x, thud(sr, 116, 60, 0.07, 0.4), 0.0, 0.4, sr)
    for i in range(12):
        tt = 0.15 + 0.85 * (i + rng.random()) / 12
        f = rng.uniform(3000, 9000)
        tau = rng.uniform(0.03, 0.08)
        add(x, partials(sr, tau * 6, [(f, 1.0, tau), (f * 1.73, 0.4, tau * 0.6)]), tt, 0.12 * (1 - tt / 1.2), sr)
    x = soft_clip(x, 1.3)
    return reverb(x, sr, size=0.7, damp=0.35, wet=0.22, tail=0.8)


def approve(sr, rng):
    """He approves: a warm D major chime, arpeggiated, over a soft chord."""
    T = 2.8
    x = buf(sr, T)
    for i, (m, at) in enumerate(((74, 0.0), (78, 0.11), (81, 0.22), (86, 0.36))):
        f = midi_hz(m)
        spec = [(f * r, a, d) for r, a, d in ((1.0, 1.0, 1.6), (2.0, 0.35, 0.9), (3.0, 0.12, 0.5), (4.16, 0.08, 0.3), (5.43, 0.04, 0.2))]
        add(x, partials(sr, T - at, spec, attack=0.004), at, 0.32 * (1 - i * 0.1), sr)
    for m in (62, 66, 69):
        p = tone(sr, midi_hz(m), T, phase=rng.random() * TAU)
        p = env_apply(p, sr, lambda t: min(1.0, t / 0.4) * max(0.0, 1 - t / T) ** 1.5)
        add(x, p, 0.0, 0.07, sr)
    return reverb(x, sr, size=0.85, damp=0.35, wet=0.28, tail=1.4)


def ally_arrive(sr, rng):
    """The hunters step out of the light: a hushed "oo" opens into a bright major chord with a bell."""
    T = 4.6
    x = buf(sr, T + 0.2)
    a = choir(sr, rng, (50, 57), 2.9, "oo", voices=3, detune=8)
    a = env_apply(a, sr, lambda t: min(1.0, (t / 2.0) ** 2) * min(1.0, (2.9 - t) / 0.5))
    add(x, a, 0.0, 1.6, sr)
    b = choir(sr, rng, (50, 57, 62, 66, 69, 74), T - 2.3, "ah", voices=3, detune=10)
    b = env_apply(b, sr, lambda t: min(1.0, t / 0.45) * min(1.0, (T - 2.3 - t) / 1.2))
    add(x, b, 2.3, 2.0, sr)
    for m, at in ((86, 2.35), (90, 2.45), (93, 2.55)):
        add(x, bell_voice(sr, midi_hz(m), 2.0, bright=0.6, beat=0.8), at, 0.08, sr)
    add(x, sparkle(sr, rng, 18, 2.3, 4.2, T + 0.2, 2600, 7000, rise=False), 0.0, 0.7, sr)
    sub = tone(sr, midi_hz(38), T)
    sub = env_apply(sub, sr, lambda t: min(1.0, t / 2.3) * min(1.0, (T - t) / 1.0))
    add(x, sub, 0.0, 0.3, sr)
    return reverb(x, sr, size=1.0, damp=0.3, wet=0.3, tail=1.8, predelay=0.03)
