"""v0.21 Abaddon, Knight of Hell: every sound in AbaddonAssets.SOUNDS, written by generate.py to sounds/abaddon/<id>.ogg;
AbaddonSoundData points supernaturalcraft:abaddon.<id> at them once the files exist (rerun runData).

Her palette: a woman's voice worn like a coat -- cool, amused, a little husky -- with something an octave under it that is not
hers (every line of hers is doubled by a rasped demon voice far below); black smoke (dark rushing noise that swirls and
hisses); hellfire (roaring gusts, crackle); the Brand (a searing sizzle and a deep seal-tone). Her retinue: armour (plate
clanking, a visored breath) and the herald's heavy iron bell.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from cain_sfx import breath, drone, shimmer
from coven_sfx import cackle, glide, rasp
from magic_sfx import OH, fire_roar, growl, iron, whisper
from michael_sfx import ring, whoosh
from raphael_sfx import crackle, rumble
from sfx import UH, burst, thud, voiced
from synth import TAU, add, buf, env_apply, formants, noise, partials, reverb, reverse, smooth_random, soft_clip, svf, tone

# A woman's vowels: formants a little higher than the shared (male-ish) ones.
AH_F = [(850, 4.0, 1.0), (1220, 5.0, 0.55), (2810, 7.0, 0.3)]
HUM_F = [(290, 2.2, 1.0), (1200, 4.0, 0.12), (2500, 6.0, 0.06)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def hall(x, sr, wet=0.18, tail=0.6):
    """Her hall: blackstone, a long dark reflection."""
    return reverb(x, sr, size=0.75, damp=0.5, wet=wet, tail=tail)


# --- building blocks -----------------------------------------------------------------------------------

def demon_double(sr, rng, T, f0, vowel, env, gain=0.45, rough=2.4):
    """The thing under her voice: the same line an octave down, darker, rasped."""
    under = voiced(sr, rng, T, (lambda t: f0(t) * 0.5) if callable(f0) else (lambda t: f0 * 0.5), vowel, 0.9, env)
    under = svf(under, sr, 1100, 0.7, "low")
    return [s * gain for s in rasp(under, rough)]


def her_line(sr, rng, T, f0, vowel, env, breath_=0.45, double=0.45):
    """A line of hers: the woman's voice and the demon under it."""
    x = voiced(sr, rng, T, f0, vowel, breath_, env)
    add(x, demon_double(sr, rng, T, f0, OH if vowel is AH_F else vowel, env, double), 0, 1.0, sr)
    return x


def smoke_rush(sr, rng, T, f0=180, f1=900, swirl=5.0, gain=1.0):
    """Black smoke moving: dark band-passed noise sweeping, swirling (a slow wobble of its centre) and hissing at its edge."""
    wob = smooth_random(sr, T, rng, swirl, 0.7, 1.3)
    x = svf(noise(int(sr * T), rng), sr, lambda t: (f0 * (f1 / f0) ** min(1.0, t / T)) * wob(t), 1.1, "band")
    hiss = svf(noise(int(sr * T), rng), sr, 4200, 0.8, "high")
    add(x, hiss, 0, 0.12, sr)
    return env_apply([s * gain for s in x], sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.2)


def plate(sr, rng, T=0.5, f0=420, bright=0.8):
    """A plate of armour knocked: a dull iron clank."""
    x = buf(sr, T)
    add(x, ring(sr, f0, T, bright), 0, 0.5, sr)
    add(x, ring(sr, f0 * 1.37, T * 0.6, bright), 0.004, 0.3, sr)
    add(x, burst(sr, rng, 0.05, 1600, 1.2, "band", 0.012), 0, 0.7, sr)
    return x


def toll(sr, f0, T, bright=1.0):
    """A heavy iron bell: a church bell's partials (hum, prime, tierce, quint, nominal), each a beating pair, long and dark."""
    spec = []
    for r, a, d in ((0.5, 0.55, T * 0.55), (1.0, 0.7, T * 0.4), (1.19, 0.45 * bright, T * 0.28), (1.5, 0.3 * bright, T * 0.22),
                    (2.0, 0.35 * bright, T * 0.18), (2.61, 0.18 * bright, T * 0.1), (3.02, 0.1 * bright, T * 0.07)):
        spec += [(f0 * r, a * 0.55, d), (f0 * r + 1.1 * r, a * 0.45, d * 0.93)]
    return partials(sr, T, spec, attack=0.0015)


def sizzle(sr, rng, T, fc=5200):
    """Searing: a dense, bright frying hiss."""
    x = buf(sr, T)
    add(x, crackle(sr, rng, T, 0.25, fc, 0.7), 0, 1.0, sr)
    add(x, svf(noise(int(sr * T), rng), sr, fc * 1.3, 0.6, "high"), 0, 0.25, sr)
    return env_apply(x, sr, lambda t: min(1.0, t / 0.02) * math.exp(-t / (T * 0.45)))


# --- her -----------------------------------------------------------------------------------------------

def ambient(sr, rng):
    """At her ease: a low, amused "mm-hm" with the thing under it, a breath of smoke round her."""
    T = 2.4
    x = buf(sr, T)
    add(x, smoke_rush(sr, rng, 2.2, 160, 420, 3.0), 0.0, 0.25, sr)
    add(x, her_line(sr, rng, 0.32, glide(232, 210, 0.3), HUM_F, _env(0.04, 0.16, 0.12), 0.35, 0.5), 0.35, 0.9, sr)
    add(x, her_line(sr, rng, 0.48, glide(205, 250, 0.45), HUM_F, _env(0.04, 0.26, 0.18), 0.35, 0.5), 0.72, 1.0, sr)
    add(x, breath(sr, rng, 0.8, 1100, 0.7), 1.4, 0.3, sr)
    return hall(x, sr, 0.16, 0.5)


def laugh(sr, rng):
    """Her laugh: low, unhurried, cruelly amused -- "ha, ha, ha" -- and the laugh under it, an octave down and rasped."""
    T = 1.8
    x = buf(sr, T)
    add(x, cackle(sr, rng, 300, 5, 0.17, 0.95, AH_F, 0.7, 0.012), 0.05, 1.0, sr)
    under = cackle(sr, rng, 150, 5, 0.17, 0.95, OH, 0.9, 0.02)
    add(x, rasp(svf(under, sr, 1000, 0.7, "low"), 2.6), 0.06, 0.5, sr)
    add(x, smoke_rush(sr, rng, 1.4, 200, 600, 4.0), 0.3, 0.2, sr)
    return hall(x, sr, 0.2, 0.7)


def hurt(sr, rng):
    """A blow taken: a hiss through her teeth snapping into a snarl, the demon snarling with her."""
    T = 0.7
    x = buf(sr, T)
    hs = svf(noise(int(sr * 0.14), rng), sr, 3800, 1.0, "band")
    add(x, env_apply(hs, sr, lambda t: math.sin(math.pi * min(1.0, t / 0.14))), 0, 0.35, sr)
    add(x, rasp(her_line(sr, rng, 0.42, glide(420, 300, 0.4), AH_F, _env(0.01, 0.12, 0.28), 0.8, 0.6), 1.6), 0.08, 1.0, sr)
    return reverb(x, sr, size=0.45, damp=0.5, wet=0.1, tail=0.25)


def death(sr, rng):
    """Her end: a scream climbing out of her as she burns, breaking into the demon's roar, the smoke dragged down into the
    earth with a falling rush and a deep collapse."""
    T = 5.2
    x = buf(sr, T)
    scream = voiced(sr, rng, 1.9, glide(440, 860, 1.6, 0.7), AH_F, 1.0, _env(0.08, 1.2, 0.6))
    add(x, rasp(scream, 1.5), 0.0, 0.8, sr)
    roar = growl(sr, rng, 2.0, 70, OH)
    add(x, env_apply(roar, sr, _env(0.3, 0.9, 0.8)), 1.2, 0.9, sr)
    add(x, fire_roar(sr, rng, 2.0, 150, 900, 0.3), 0.2, 0.7, sr)
    # Dragged down: the smoke rushing downward, the ground closing.
    add(x, smoke_rush(sr, rng, 1.8, 1400, 120, 7.0, 1.3), 2.4, 1.0, sr)
    add(x, rumble(sr, rng, 2.0, 60, 0.9, 5), 3.0, 2.0, sr)
    add(x, thud(sr, 55, 24, 0.5, 1.4), 4.0, 1.2, sr)
    add(x, env_apply(drone(sr, rng, T - 0.2, 41, 320), sr, _env(0.8, 2.2, 2.0)), 0.0, 0.35, sr)
    return hall(soft_clip(x, 1.3), sr, 0.24, 0.9)


def sweep(sr, rng):
    """Hellfire swept out of her hand: a gust of fire roaring across, crackle in its wake."""
    T = 1.8
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.45, 260, 1800, 1.0, 0.6), 0, 0.6, sr)
    add(x, fire_roar(sr, rng, 1.4, 160, 1000, 0.25), 0.08, 1.6, sr)
    add(x, thud(sr, 80, 40, 0.2, 0.6), 0.1, 0.6, sr)
    add(x, crackle(sr, rng, 1.2, 0.07, 2800, 0.8, 0.5), 0.4, 0.7, sr)
    return hall(soft_clip(x, 1.2), sr, 0.14, 0.5)


def smoke(sr, rng):
    """She goes to smoke: a deep breath drawn in reverse, then the smoke rushing out of her, a demon's whisper inside it."""
    T = 2.2
    x = buf(sr, T)
    pre = reverse(reverb(burst(sr, rng, 0.3, 700, 0.7, "band", 0.1), sr, size=0.8, damp=0.4, wet=0.85, tail=0.4))
    add(x, pre, 0, 0.5, sr)
    p = len(pre) / sr
    add(x, smoke_rush(sr, rng, 1.6, 160, 1300, 6.0, 1.2), p - 0.1, 1.0, sr)
    w = svf(whisper(sr, rng, 1.2, 5, 1.0), sr, 700, 0.7, "high")
    add(x, w, p + 0.1, 0.3, sr)
    add(x, demon_double(sr, rng, 0.8, 190, OH, _env(0.2, 0.3, 0.3), 0.6), p + 0.2, 0.6, sr)
    return hall(x, sr, 0.2, 0.6)


def possess(sr, rng):
    """She takes a knight: the smoke diving in, the armour jarring round her, a growl from inside the helm, the visor kindling."""
    T = 2.0
    x = buf(sr, T)
    add(x, smoke_rush(sr, rng, 0.6, 300, 1800, 8.0, 1.2), 0, 0.9, sr)
    at = 0.52
    add(x, thud(sr, 90, 40, 0.18, 0.6), at, 1.0, sr)
    add(x, plate(sr, rng, 0.6, 360, 0.7), at, 0.6, sr)
    add(x, plate(sr, rng, 0.5, 520, 0.6), at + 0.05, 0.4, sr)
    g = growl(sr, rng, 0.9, 64, OH)
    g = formants(g, sr, [(400, 3.0, 1.0), (900, 4.0, 0.4)])
    add(x, env_apply(g, sr, _env(0.05, 0.4, 0.45)), at + 0.08, 0.8, sr)
    add(x, env_apply(tone(sr, 110, 1.2, glide=150), sr, _env(0.1, 0.4, 0.7)), at, 0.25, sr)
    return hall(soft_clip(x, 1.2), sr, 0.16, 0.5)


def pin(sr, rng):
    """Her fist closes: a low violet hum of the mind's grip tightening, chains of force snapping taut, a strained creak."""
    T = 1.6
    x = buf(sr, T)
    add(x, shimmer(sr, rng, 1.4, 440, 1.2), 0, 0.8, sr)
    hum = env_apply(tone(sr, 74, 1.4, glide=96), sr, _env(0.2, 0.6, 0.6))
    add(x, hum, 0, 0.5, sr)
    for k in range(3):
        at = 0.3 + k * 0.07
        add(x, ring(sr, 900 + 230 * k, 0.4, 0.6), at, 0.18, sr)
        add(x, burst(sr, rng, 0.04, 2200, 1.4, "band", 0.01), at, 0.4, sr)
    creak = svf(noise(int(sr * 0.6), rng), sr, lambda t: 500 + 200 * math.sin(TAU * 3 * t), 6.0, "band")
    add(x, env_apply(creak, sr, _env(0.1, 0.25, 0.25)), 0.5, 0.35, sr)
    return hall(x, sr, 0.14, 0.5)


def brand(sr, rng):
    """The Brand: her palm on the floor -- a searing sizzle, the seal's deep tone ringing out, hellfire bursting along the
    rings with a boom."""
    T = 3.2
    x = buf(sr, T)
    add(x, sizzle(sr, rng, 1.2), 0, 0.7, sr)
    seal = toll(sr, 98, 2.6, 0.7)
    add(x, seal, 0.05, 0.6, sr)
    add(x, env_apply(drone(sr, rng, 1.4, 49, 400), sr, lambda t: (t / 1.4) ** 1.4), 0.0, 0.5, sr)
    p = 1.2
    add(x, thud(sr, 62, 26, 0.5, 1.4), p, 1.3, sr)
    add(x, fire_roar(sr, rng, 1.8, 140, 1100, 0.12), p - 0.03, 2.0, sr)
    add(x, crackle(sr, rng, 1.6, 0.08, 3000, 0.8, 0.8), p + 0.1, 0.8, sr)
    return hall(soft_clip(x, 1.4), sr, 0.2, 0.7)


def trapped(sr, rng):
    """Caught in a devil's trap: the trap's ring humming like struck iron, a sub thump, her furious snarl straining against it."""
    T = 2.4
    x = buf(sr, T)
    add(x, thud(sr, 70, 34, 0.3, 0.8), 0, 1.0, sr)
    add(x, toll(sr, 147, 2.2, 0.5), 0, 0.35, sr)
    add(x, env_apply(drone(sr, rng, 2.2, 55, 600), sr, _env(0.1, 1.2, 0.9)), 0, 0.4, sr)
    sn = her_line(sr, rng, 0.9, glide(320, 260, 0.9), AH_F, _env(0.05, 0.5, 0.35), 0.9, 0.7)
    add(x, rasp(sn, 2.0), 0.25, 0.7, sr)
    add(x, smoke_rush(sr, rng, 1.2, 600, 150, 4.0), 0.4, 0.35, sr)
    return hall(soft_clip(x, 1.2), sr, 0.18, 0.6)


# --- her retinue ---------------------------------------------------------------------------------------

def knight_ambient(sr, rng):
    """A knight of hers: a slow, heavy breath through a visor, plate shifting."""
    T = 2.0
    x = buf(sr, T)
    br = breath(sr, rng, 1.1, 520, 1.4)
    br = formants(br, sr, [(380, 4.0, 1.0), (1500, 6.0, 0.3)])
    add(x, rasp(br, 2.2), 0.05, 0.7, sr)
    add(x, plate(sr, rng, 0.45, 300, 0.5), 1.2, 0.35, sr)
    add(x, plate(sr, rng, 0.35, 440, 0.4), 1.32, 0.2, sr)
    return reverb(x, sr, size=0.6, damp=0.55, wet=0.12, tail=0.4)


def knight_hurt(sr, rng):
    """A blow on plate: a clang and a grunt inside the helm."""
    T = 0.7
    x = buf(sr, T)
    add(x, iron(sr, rng, 520, 0.6, 0.8), 0, 0.8, sr)
    g = voiced(sr, rng, 0.3, glide(120, 88, 0.3), UH, 0.9, _env(0.01, 0.08, 0.2))
    add(x, rasp(formants(g, sr, [(420, 3.0, 1.0)]), 2.0), 0.02, 0.7, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.08, tail=0.2)


def knight_death(sr, rng):
    """A knight falls: a dying growl in the helm, the armour collapsing in a clatter, smoke hissing out of it."""
    T = 2.0
    x = buf(sr, T)
    g = growl(sr, rng, 0.9, 58, OH)
    add(x, env_apply(g, sr, _env(0.03, 0.4, 0.45)), 0, 0.7, sr)
    for k in range(6):
        at = 0.45 + k * 0.08 + rng.uniform(-0.02, 0.02)
        add(x, plate(sr, rng, 0.5, rng.uniform(280, 620), 0.6), at, rng.uniform(0.35, 0.7), sr)
    add(x, thud(sr, 80, 40, 0.2, 0.6), 0.5, 0.8, sr)
    add(x, smoke_rush(sr, rng, 1.0, 900, 300, 5.0), 0.8, 0.4, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.6, damp=0.5, wet=0.14, tail=0.5)


def herald_mark(sr, rng):
    """The herald's bell: one heavy iron toll, long and dark, a whisper of the mark in its ring."""
    T = 3.6
    x = buf(sr, T)
    add(x, toll(sr, 196, 3.4, 0.9), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, 1200, 1.2, "band", 0.012), 0, 0.3, sr)
    w = svf(whisper(sr, rng, 1.0, 3, 1.0), sr, 900, 0.7, "high")
    add(x, w, 0.3, 0.12, sr)
    return hall(x, sr, 0.22, 0.9)


# id (the file's base name) -> (builder, peak dBFS)
SOUNDS = {
    "ambient": (ambient, -4.0), "laugh": (laugh, -2.0), "hurt": (hurt, -2.0), "death": (death, -1.0), "sweep": (sweep, -1.5),
    "smoke": (smoke, -2.0), "possess": (possess, -1.5), "pin": (pin, -2.0), "brand": (brand, -1.0), "trapped": (trapped, -1.5),
    "knight_ambient": (knight_ambient, -4.0), "knight_hurt": (knight_hurt, -2.0), "knight_death": (knight_death, -1.5),
    "herald_mark": (herald_mark, -1.5),
}
