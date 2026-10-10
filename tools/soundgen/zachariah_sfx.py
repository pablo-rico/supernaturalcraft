"""v0.18 Zachariah: sounds/zachariah/<id>.ogg (HeavenAssets.SOUNDS_ZACHARIAH).

Same conventions as sfx.py / raphael_sfx.py: (sr, rng) -> float list, finished and encoded by generate.py (HeavenAssetData
points supernaturalcraft:zachariah.<id> at them). The office: the rubber stamp's thud, paper in flurries, steel drawers, the
typewriter's bell, a buzzer and a ding for the verdicts; and under the suit, an angel: six great wings, a smite, a choir. His
voice is a tired middle manager's.
"""

import math

from michael_sfx import hall, trumpet, whoosh
from sfx import HUM, UH, bell_voice, burst, choir, margin_bell, sparkle, thud, typekey, voiced
from synth import add, buf, env_apply, midi_hz, noise, onepole_lp, partials, reverb, reverse, saw, soft_clip, svf, tone


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def office(x, sr, wet=0.1, tail=0.4):
    """A carpeted office: soft and short."""
    return reverb(x, sr, size=0.5, damp=0.6, wet=wet, tail=tail, predelay=0.01)


def rustle(sr, rng, T, density=60, lo=2500, hi=6000):
    """Paper: many tiny crisp crackles in a band, each one a short noise burst."""
    x = buf(sr, T)
    for _ in range(int(density * T)):
        t = rng.uniform(0, T - 0.05)
        add(x, burst(sr, rng, 0.03, rng.uniform(lo, hi), rng.uniform(1.0, 3.0), "band", rng.uniform(0.003, 0.012)), t,
            rng.uniform(0.2, 0.6), sr)
    return x


def slam(sr, rng, gain=1.0):
    """A rubber stamp brought down on a desk: the knock of wood, the slap of rubber, the desk's low boom."""
    x = buf(sr, 0.6)
    add(x, thud(sr, 120, 55, 0.06, 0.3), 0.0, 1.0 * gain, sr)
    add(x, burst(sr, rng, 0.05, 900, 0.9, "band", 0.012), 0.0, 0.9 * gain, sr)
    add(x, burst(sr, rng, 0.03, 2600, 0.7, "high", 0.004), 0.0, 0.5 * gain, sr)
    add(x, burst(sr, rng, 0.25, 220, 1.2, "band", 0.07), 0.003, 0.6 * gain, sr)
    return x


def wingbeat(sr, rng, T=1.0, depth=1.0):
    x = buf(sr, T)
    add(x, whoosh(sr, rng, T * 0.75, 150, 380, 0.8, 0.45), 0, 1.0 * depth, sr)
    add(x, onepole_lp(whoosh(sr, rng, T * 0.7, 70, 140, 0.7, 0.4), sr, 260), 0.02, 1.3 * depth, sr)
    rus = svf(noise(int(sr * T * 0.55), rng), sr, 4600, 1.4, "band")
    rus = env_apply(rus, sr, lambda t: (0.5 + 0.5 * math.sin(t * 80)) * math.sin(math.pi * min(1.0, t / (T * 0.55))))
    add(x, rus, 0.1, 0.16, sr)
    return x


# --- his voice ------------------------------------------------------------------------------------------------------

def ambient(sr, rng):
    """A long-suffering sigh through the nose, and the click of a ballpoint pen."""
    x = buf(sr, 2.4)
    br = onepole_lp(noise(int(sr * 1.2), rng), sr, 1100)
    add(x, env_apply(br, sr, _env(0.25, 0.3, 0.65)), 0.0, 0.35, sr)
    v = voiced(sr, rng, 0.9, lambda t: 108 * (0.82 ** (t / 0.9)), HUM, 0.8, _env(0.12, 0.35, 0.43))
    add(x, v, 0.25, 0.7, sr)
    for t in (1.6, 1.72):
        add(x, burst(sr, rng, 0.015, 3600, 2.0, "band", 0.002), t, 0.5, sr)
    return office(x, sr, 0.08, 0.3)


def hurt(sr, rng):
    """An affronted grunt."""
    x = buf(sr, 0.7)
    add(x, burst(sr, rng, 0.03, 1800, 0.8, "band", 0.01), 0.0, 0.4, sr)
    v = voiced(sr, rng, 0.38, lambda t: 128 * (0.78 ** (t / 0.38)), UH, 0.55, _env(0.012, 0.12, 0.24))
    add(x, v, 0.01, 1.0, sr)
    return office(x, sr, 0.08, 0.25)


def death(sr, rng):
    """The angel under the suit burns out: a cry going up into light, six wings beating once, a choir, and a hush."""
    T = 6.0
    x = buf(sr, T)
    v = voiced(sr, rng, 2.2, lambda t: 115 * (2.5 ** min(1.0, t / 1.9)), UH, 0.7, _env(0.2, 1.4, 0.6))
    add(x, v, 0.3, 0.9, sr)
    for i in range(3):
        add(x, wingbeat(sr, rng, 1.0, 0.8), 1.4 + i * 0.12, 0.7, sr)
    c = choir(sr, rng, (57, 64, 69, 76), 3.5, "ah", voices=3, detune=12)
    c = svf(c, sr, lambda t: 450 * (10 ** min(1.0, t / 2.0)), 0.8, "low")
    add(x, env_apply(c, sr, _env(1.2, 1.2, 1.1)), 1.6, 1.0, sr)
    add(x, sparkle(sr, rng, 30, 2.4, 5.4, T, 2400, 8500), 0, 0.6, sr)
    add(x, thud(sr, 70, 30, 0.35, 1.0), 1.4, 0.7, sr)
    return hall(soft_clip(x, 1.3), sr, 0.28, 1.8)


# --- the office -----------------------------------------------------------------------------------------------------

def stamp(sr, rng):
    """The Rubber Stamp: a wind-up whoosh and the slam, heavier than any stamp should be."""
    x = buf(sr, 0.9)
    add(x, whoosh(sr, rng, 0.22, 400, 1200, 1.0, 0.8), 0.0, 0.35, sr)
    add(x, slam(sr, rng, 1.0), 0.2, 1.0, sr)
    add(x, thud(sr, 60, 32, 0.2, 0.5), 0.2, 0.9, sr)
    return office(soft_clip(x, 1.3), sr, 0.12, 0.35)


def paper_storm(sr, rng):
    """A storm of memos: a gust picking up, hundreds of sheets fluttering through it."""
    T = 2.4
    x = buf(sr, T)
    add(x, whoosh(sr, rng, T * 0.9, 250, 900, 0.6, 0.35), 0.0, 0.6, sr)
    r = rustle(sr, rng, T, 110)
    add(x, env_apply(r, sr, lambda t: min(1.0, t / 0.4) * max(0.0, 1 - max(0.0, t - 1.5) / 0.9)), 0, 0.6, sr)
    flap = svf(noise(int(sr * T), rng), sr, 1500, 1.0, "band")
    flap = env_apply(flap, sr, lambda t: (0.5 + 0.5 * math.sin(t * 2 * math.pi * 14)) ** 3 * math.sin(math.pi * t / T))
    add(x, flap, 0.0, 0.25, sr)
    return office(x, sr, 0.12, 0.4)


def file(sr, rng):
    """A form filed: the steel drawer rolling out on its runners, the paper dropped in, the drawer slammed home."""
    x = buf(sr, 1.3)
    roll = svf(noise(int(sr * 0.35), rng), sr, 700, 1.0, "band")
    roll = env_apply(roll, sr, lambda t: (0.6 + 0.4 * math.sin(t * 2 * math.pi * 60)) * math.sin(math.pi * t / 0.35))
    add(x, roll, 0.0, 0.5, sr)
    add(x, rustle(sr, rng, 0.25, 70), 0.4, 0.5, sr)
    add(x, roll[::-1], 0.62, 0.55, sr)
    add(x, thud(sr, 150, 80, 0.05, 0.2), 0.97, 0.8, sr)
    add(x, partials(sr, 0.5, [(420, 0.3, 0.2), (1130, 0.2, 0.12), (2210, 0.12, 0.07)], 0.001), 0.97, 0.6, sr)
    add(x, burst(sr, rng, 0.03, 3000, 0.8, "high", 0.004), 0.97, 0.5, sr)
    return office(soft_clip(x, 1.6), sr, 0.1, 0.3)


def denied(sr, rng):
    """DENIED: a flat double buzz and the stamp's slam."""
    x = buf(sr, 0.9)
    for t in (0.0, 0.16):
        b = [a + 0.5 * c for a, c in zip(saw(sr, 0.12, 110, 0, 0.0), saw(sr, 0.12, 116.5, 0, 0.3))]
        add(x, env_apply(svf(b, sr, 900, 0.9, "low"), sr, _env(0.005, 0.1, 0.015)), t, 0.4, sr)
    add(x, slam(sr, rng, 0.9), 0.34, 1.0, sr)
    return office(soft_clip(x, 1.2), sr, 0.1, 0.3)


def approved(sr, rng):
    """APPROVED: a light stamp and a bright counter-bell."""
    x = buf(sr, 1.4)
    add(x, slam(sr, rng, 0.6), 0.0, 1.0, sr)
    add(x, bell_voice(sr, midi_hz(84), 1.2, bright=0.6, beat=0.8), 0.08, 0.35, sr)
    add(x, sparkle(sr, rng, 5, 0.1, 0.5, 1.4, 3500, 7000), 0, 0.3, sr)
    return office(x, sr, 0.12, 0.35)


def termination(sr, rng):
    """A termination notice: a low, final brass chord under a struck bell, the room going quiet around it."""
    T = 2.8
    x = buf(sr, T)
    for m in (36, 43, 48, 51):
        add(x, trumpet(sr, rng, m, 1.8, 4.5), 0.05, 0.22, sr)
    add(x, bell_voice(sr, midi_hz(60), 2.4, bright=0.5, beat=0.6), 0.0, 0.5, sr)
    add(x, thud(sr, 55, 30, 0.4, 1.0), 0.0, 0.8, sr)
    return hall(soft_clip(x, 1.2), sr, 0.22, 1.2)


def wings(sr, rng):
    """Six burnt-gold wings unfolding: three heavy beats overlapping, the air of the office thrown about."""
    x = buf(sr, 1.9)
    for i, t in enumerate((0.0, 0.14, 0.3)):
        add(x, wingbeat(sr, rng, 1.2, 1.0 - 0.15 * i), t, 0.8, sr)
    add(x, rustle(sr, rng, 0.8, 50, 2000, 4500), 0.5, 0.25, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.15, tail=0.5)


def wrap(sr, rng):
    """The office folds back on itself: air sucked backwards and a tape-stop drop in pitch."""
    T = 1.3
    x = buf(sr, T)
    add(x, reverse(whoosh(sr, rng, 0.7, 300, 2400, 0.9, 0.3)), 0.0, 0.8, sr)
    drop = tone(sr, 880, 0.5, glide=110, attack=0.01)
    drop = [v + 0.3 * math.sin(i * 2 * math.pi * 3 / sr) * v for i, v in enumerate(drop)]
    add(x, env_apply(drop, sr, _env(0.02, 0.3, 0.18)), 0.6, 0.25, sr)
    add(x, thud(sr, 90, 45, 0.08, 0.3), 0.7, 0.6, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.2, tail=0.4)


def docket(sr, rng):
    """It was already written: two keys struck and the typewriter's margin bell."""
    x = buf(sr, 1.6)
    add(x, typekey(sr, rng, 1.0, 1.0, 1.0), 0.0, 0.6, sr)
    add(x, typekey(sr, rng, 1.08, 0.95, 1.1), 0.09, 0.55, sr)
    add(x, margin_bell(sr, rng), 0.2, 0.7, sr)
    return x


SOUNDS = {
    "ambient": (ambient, -4.0), "hurt": (hurt, -2.0), "death": (death, -1.0), "stamp": (stamp, -1.0),
    "paper_storm": (paper_storm, -2.0), "file": (file, -2.0), "denied": (denied, -2.0), "approved": (approved, -2.0),
    "termination": (termination, -1.5), "wings": (wings, -1.5), "wrap": (wrap, -2.0), "docket": (docket, -3.0),
}
