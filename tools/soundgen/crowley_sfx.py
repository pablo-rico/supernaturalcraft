"""v0.35 Crowley, King of the Crossroads: every sound in CrowleyAssets.SOUNDS, written by generate.py to sounds/crowley/<id>.ogg
(supernaturalcraft:crowley.<id>).

His palette: a man's low, unhurried voice -- amused, smug -- with a rasp an octave under it; the finger snap that does things;
red demon smoke (a dark rush with a crackle of sulphur); paper (contracts by the hundred, fluttering, tearing); hellfire;
wax seals cracking; hellhounds (unseen: growls, the bay, claws on stone, scorched ground). His retinue: a suited demon
(clipped grunts, the smoke torn out of him) and a possessed patient (wet ragged breath, a moan, a shriek).

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from abaddon_sfx import demon_double, hall, smoke_rush
from coven_sfx import rasp
from magic_sfx import AH, OH, fire_roar, growl, whisper
from raphael_sfx import crackle, rumble
from sfx import UH, burst, page_tear, thud, voiced
from synth import add, buf, env_apply, noise, partials, reverb, reverse, smooth_random, soft_clip, svf


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def his_line(sr, rng, T, f0, vowel, env, breath=0.5, double=0.35):
    """A line of his: a low man's voice, the demon under it."""
    x = voiced(sr, rng, T, f0, vowel, breath, env)
    add(x, demon_double(sr, rng, T, f0, vowel, env, double), 0, 1.0, sr)
    return x


def finger_snap(sr, rng, gain=1.0):
    x = buf(sr, 0.12)
    add(x, burst(sr, rng, 0.02, 1400, 0.8, "high", 0.0015), 0.0, gain, sr)
    add(x, burst(sr, rng, 0.05, 2200, 4.0, "band", 0.01), 0.0, 0.8 * gain, sr)
    add(x, burst(sr, rng, 0.06, 900, 2.0, "band", 0.015), 0.0006, 0.5 * gain, sr)
    return x


def flutter(sr, rng, T, rate=60, gain=1.0):
    """Many sheets of paper fluttering: dense clicks of flapping paper over an airy rush."""
    x = buf(sr, T)
    dens = smooth_random(sr, T, rng, 4, 0.4, 1.0)
    t = 0.0
    while t < T:
        f = rng.uniform(1500, 5200)
        add(x, burst(sr, rng, 0.02, f, 1.4, "band", rng.uniform(0.002, 0.006)), t, gain * rng.uniform(0.2, 0.7) * dens(t), sr)
        t += rng.expovariate(rate * dens(t))
    air = svf(noise(int(sr * T), rng), sr, 2400, 0.7, "band")
    add(x, env_apply(air, sr, lambda tt: 0.35 * dens(tt) * math.sin(math.pi * min(1.0, tt / T))), 0, gain, sr)
    return x


# --- his -----------------------------------------------------------------------------------------------------------

def snap(sr, rng):
    """His snap: a dry crack, then the air jolts -- a low thump of force and a short, too-big room."""
    x = buf(sr, 1.6)
    add(x, finger_snap(sr, rng), 0.0, 1.0, sr)
    add(x, thud(sr, 120, 50, 0.08, 0.5), 0.04, 0.8, sr)
    add(x, svf(env_apply(noise(int(sr * 0.4), rng), sr, lambda t: math.exp(-t / 0.08)), sr, 600, 0.9, "band"), 0.04, 0.5, sr)
    return reverb(x, sr, size=0.9, damp=0.35, wet=0.28, tail=1.0, predelay=0.02)


def smoke_out(sr, rng):
    """He goes: a sharp inhale reversed into a rush of red smoke, a crackle of sulphur, gone."""
    x = buf(sr, 1.4)
    add(x, reverse(smoke_rush(sr, rng, 0.5, 900, 200, 6.0, 0.8)), 0.0, 0.8, sr)
    add(x, smoke_rush(sr, rng, 0.9, 260, 1400, 7.0, 1.0), 0.42, 1.0, sr)
    add(x, crackle(sr, rng, 0.8, 0.08, 3800, 0.9), 0.45, 0.35, sr)
    add(x, thud(sr, 90, 40, 0.06, 0.3), 0.44, 0.5, sr)
    return hall(x, sr, 0.15, 0.5)


def smoke_in(sr, rng):
    """He arrives: smoke rushing together, a soft thump as he steps out of it, a pleased little "hm"."""
    x = buf(sr, 1.4)
    add(x, smoke_rush(sr, rng, 0.7, 1400, 220, 6.0, 1.0), 0.0, 0.9, sr)
    add(x, thud(sr, 100, 55, 0.05, 0.3), 0.66, 0.7, sr)
    add(x, his_line(sr, rng, 0.3, lambda t: 112 - 10 * t / 0.3, [(250, 2.2, 1.0), (1050, 4.0, 0.12), (2200, 6.0, 0.06)],
                    lambda t: min(1.0, t / 0.03) * math.exp(-t / 0.12), 0.7, 0.25), 0.8, 0.6, sr)
    return hall(x, sr, 0.12, 0.5)


def paper_storm(sr, rng):
    """His contracts: a gust that whips up hundreds of pages, fluttering and slapping, a tear or two, then settling."""
    T = 2.6
    x = buf(sr, T + 0.4)
    gust = smooth_random(sr, T, rng, 2.5, 0.5, 1.0)
    wind = svf(noise(int(sr * T), rng), sr, lambda t: 500 + 900 * gust(t), 0.8, "band")
    add(x, env_apply(wind, sr, lambda t: 0.6 * gust(t) * min(1.0, t / 0.2) * min(1.0, (T - t) / 0.8)), 0, 1.0, sr)
    add(x, flutter(sr, rng, T, 90, 1.0), 0.05, 0.9, sr)
    add(x, page_tear(sr, rng, 1), 0.7, 0.35, sr)
    add(x, page_tear(sr, rng, 0), 1.5, 0.25, sr)
    return reverb(x, sr, size=0.6, damp=0.5, wet=0.12, tail=0.5)


def hellfire(sr, rng):
    """Fire running along the floor in a line: a whoomph, the roar travelling, crackle in its wake."""
    x = buf(sr, 2.4)
    add(x, thud(sr, 80, 40, 0.1, 0.5), 0.0, 0.7, sr)
    add(x, fire_roar(sr, rng, 1.8, 160, 1100, 0.6), 0.02, 1.0, sr)
    add(x, crackle(sr, rng, 2.2, 0.1, 3000, 0.8), 0.2, 0.4, sr)
    return hall(x, sr, 0.12, 0.6)


def seal_break(sr, rng):
    """A wax seal cracks: a brittle snap, crumbs of wax pattering, and under it a deep tone like a held contract voided."""
    x = buf(sr, 2.6)
    for i in range(5):
        add(x, burst(sr, rng, 0.012, rng.uniform(2200, 3600), 1.2, "band", 0.0015), 0.004 * i + 0.003 * rng.random(), 0.9, sr)
    add(x, burst(sr, rng, 0.05, 700, 1.5, "band", 0.012), 0.0, 0.6, sr)
    for i in range(10):
        t = 0.06 + 0.4 * rng.random()
        add(x, burst(sr, rng, 0.01, rng.uniform(1500, 4000), 2.0, "band", 0.002), t, 0.25 * (1 - t), sr)
    add(x, partials(sr, 2.2, [(73, 0.6, 1.0), (110, 0.4, 0.8), (146, 0.25, 0.6), (219, 0.12, 0.4)], attack=0.02), 0.02, 0.7, sr)
    add(x, rumble(sr, rng, 1.6, 60, 1.2, 3), 0.05, 0.4, sr)
    return hall(x, sr, 0.2, 1.0)


def hounds(sr, rng):
    """Hellhounds he has called through: deep overlapping growls, a bay, claws scrabbling, the ground sizzling where they land."""
    T = 2.8
    x = buf(sr, T + 0.4)
    for i, (t0, f0) in enumerate(((0.0, 52), (0.25, 61), (0.5, 47))):
        g = growl(sr, rng, 1.8, f0, OH)
        add(x, env_apply(g, sr, _env(0.15, 1.2, 0.45)), t0, 0.55, sr)
    bay = voiced(sr, rng, 1.0, lambda t: 190 + 110 * math.sin(math.pi * min(1.0, t / 1.0)), AH, 0.6, _env(0.08, 0.6, 0.3))
    add(x, rasp(bay, 2.0), 0.9, 0.5, sr)
    for i in range(14):
        add(x, burst(sr, rng, 0.012, rng.uniform(2500, 5500), 2.0, "band", 0.002), 0.1 + rng.random() * 2.2, 0.35, sr)
    sizzle = svf(noise(int(sr * 2.0), rng), sr, 5200, 0.7, "high")
    add(x, env_apply(sizzle, sr, _env(0.1, 1.2, 0.7)), 0.3, 0.18, sr)
    return hall(x, sr, 0.15, 0.7)


def laugh(sr, rng):
    """His laugh: a low, smug "heh-heh-heh" through a closed smile, the demon chuckling under it."""
    x = buf(sr, 1.4)
    for i, (t, f) in enumerate(((0.0, 120), (0.17, 114), (0.33, 108), (0.52, 98))):
        d = 0.14 if i < 3 else 0.26
        add(x, his_line(sr, rng, d, lambda tt, f=f: f * (1 - 0.1 * tt / d), UH, lambda tt: min(1.0, tt / 0.012) * math.exp(-tt / 0.06),
                        0.9, 0.3), t, 1.0 - i * 0.12, sr)
    return hall(x, sr, 0.1, 0.4)


def hurt(sr, rng):
    """A blow lands: a clipped grunt through his teeth, irritated more than hurt, the rasp snarling under it."""
    x = buf(sr, 0.7)
    br = svf(noise(int(sr * 0.2), rng), sr, 1400, 0.8, "band")
    add(x, env_apply(br, sr, lambda t: math.exp(-t / 0.05)), 0, 0.3, sr)
    add(x, his_line(sr, rng, 0.42, lambda t: 140 * (0.72 ** min(1.0, t / 0.42)), UH,
                    lambda t: min(1.0, t / 0.02) * math.exp(-max(0.0, t - 0.05) / 0.12), 0.6, 0.5), 0.01, 1.0, sr)
    return hall(x, sr, 0.08, 0.3)


def death(sr, rng):
    """The blade goes in: a choked gasp, then the demon's light -- a crackling roar climbing out of him -- and the hush after."""
    T = 3.6
    x = buf(sr, T + 0.6)
    add(x, his_line(sr, rng, 0.5, lambda t: 150 + 40 * t, AH, _env(0.02, 0.2, 0.25), 0.8, 0.3), 0.0, 0.8, sr)
    scream = voiced(sr, rng, 1.8, lambda t: 140 + 260 * min(1.0, t / 1.4), AH, 0.7, _env(0.3, 1.1, 0.4))
    add(x, rasp(scream, 2.6), 0.6, 0.7, sr)
    add(x, growl(sr, rng, 1.8, 44, OH), 0.7, 0.5, sr)
    # The flare: an electric crackle swelling, then a burst.
    zap = crackle(sr, rng, 1.6, 0.22, 4200, 0.7)
    add(x, env_apply(zap, sr, lambda t: min(1.0, t / 1.2)), 0.9, 0.7, sr)
    add(x, thud(sr, 70, 30, 0.25, 1.0), 2.4, 1.0, sr)
    add(x, burst(sr, rng, 0.4, 1800, 0.6, "band", 0.12), 2.4, 0.6, sr)
    return hall(soft_clip(x, 1.2), sr, 0.22, 1.2)


# --- the suited demon ----------------------------------------------------------------------------------------------

def suit_hurt(sr, rng):
    """A suit takes a blow: a short "hgh" and a hiss."""
    x = buf(sr, 0.5)
    add(x, his_line(sr, rng, 0.3, lambda t: 128 - 30 * t, UH, lambda t: min(1.0, t / 0.015) * math.exp(-t / 0.08), 0.7, 0.4), 0, 1.0, sr)
    add(x, svf(env_apply(noise(int(sr * 0.25), rng), sr, lambda t: math.exp(-t / 0.08)), sr, 4500, 0.8, "high"), 0.12, 0.2, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.06, tail=0.2)


def suit_death(sr, rng):
    """The smoke torn out of a suit: a strangled cry, the black smoke roaring out of his mouth, the body dropping."""
    x = buf(sr, 2.0)
    cry = voiced(sr, rng, 0.6, lambda t: 150 + 100 * t, AH, 0.7, _env(0.03, 0.3, 0.25))
    add(x, rasp(cry, 2.2), 0.0, 0.7, sr)
    add(x, smoke_rush(sr, rng, 1.0, 300, 1600, 8.0, 1.0), 0.25, 1.0, sr)
    add(x, thud(sr, 90, 45, 0.09, 0.4), 1.3, 0.8, sr)
    return hall(x, sr, 0.12, 0.5)


# --- the possessed patient -----------------------------------------------------------------------------------------

def patient_ambient(sr, rng):
    """A patient: ragged, wet breathing in and out, a low moan, a whisper under it that is not his."""
    T = 3.0
    x = buf(sr, T)
    for t0, d, inhale in ((0.0, 0.7, True), (0.8, 0.9, False), (1.9, 0.6, True)):
        br = svf(noise(int(sr * d), rng), sr, 900 if inhale else 650, 1.2, "band")
        wob = smooth_random(sr, d, rng, 30, 0.4, 1.0)
        add(x, env_apply(br, sr, lambda t, d=d, w=wob: math.sin(math.pi * t / d) * w(t)), t0, 0.5, sr)
    moan = voiced(sr, rng, 1.1, lambda t: 100 - 12 * t, OH, 0.9, _env(0.2, 0.5, 0.4))
    add(x, moan, 0.85, 0.45, sr)
    add(x, whisper(sr, rng, 1.2, 4, 0.5), 1.6, 0.4, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.2, tail=0.6)


def patient_hurt(sr, rng):
    """A patient hit: a cracked shriek, cut off."""
    x = buf(sr, 0.6)
    s = voiced(sr, rng, 0.35, lambda t: 380 - 150 * t, AH, 0.8, lambda t: min(1.0, t / 0.01) * math.exp(-t / 0.12))
    add(x, rasp(s, 2.4), 0, 1.0, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.14, tail=0.3)


def patient_death(sr, rng):
    """A patient freed: a long shriek, the smoke tearing out of him, then a man's exhausted sigh."""
    x = buf(sr, 2.4)
    s = voiced(sr, rng, 0.9, lambda t: 320 + 120 * math.sin(t * 5), AH, 0.7, _env(0.04, 0.5, 0.3))
    add(x, rasp(s, 2.6), 0, 0.8, sr)
    add(x, smoke_rush(sr, rng, 0.9, 300, 1500, 8.0, 0.9), 0.4, 0.9, sr)
    sigh = voiced(sr, rng, 0.6, lambda t: 110 - 20 * t, UH, 1.2, _env(0.1, 0.1, 0.4))
    add(x, sigh, 1.5, 0.35, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.18, tail=0.6)


SOUNDS = {
    "snap": (snap, -1.5), "smoke_out": (smoke_out, -2.0), "smoke_in": (smoke_in, -2.0), "paper_storm": (paper_storm, -2.0),
    "hellfire": (hellfire, -1.5), "seal_break": (seal_break, -1.0), "hounds": (hounds, -1.5), "laugh": (laugh, -2.0),
    "hurt": (hurt, -2.0), "death": (death, -1.0), "suit_hurt": (suit_hurt, -2.0), "suit_death": (suit_death, -1.5),
    "patient_ambient": (patient_ambient, -4.0), "patient_hurt": (patient_hurt, -2.0), "patient_death": (patient_death, -1.5),
}
