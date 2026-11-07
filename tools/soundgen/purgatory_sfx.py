"""v0.39 Purgatory: the Alphas, Eve and Purgatory's creatures, written by generate.py to sounds/purgatory/<id>.ogg
(supernaturalcraft:purgatory.<id>); every id in purgatory/PurgatoryAssets.SOUNDS.

The palette: Eve -- a young woman's voice, warm and wrong (soft, motherly, a second voice of dry rustling under it), the wet
crackle of roots tearing up through earth, a skittering carpet of roaches, the earth splitting for her children. The Alphas:
the old vampire's command hiss and the rush of air of his lunge; the Alpha werewolf's deep howl and the slam of his arms;
the shapeshifter's skin tearing and peeling wet. The souls: hollow moans in a wide grey forest, a sigh dispersing. The rift:
a deep tearing whoosh with voices far inside it.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from coven_sfx import glide, rasp
from magic_sfx import AH, EE, OH, growl, whisper
from raphael_sfx import crackle, rumble
from sfx import HUM, UH, burst, thud, voiced
from synth import add, buf, env_apply, noise, onepole_lp, reverb, smooth_random, soft_clip, svf


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def woods(x, sr, wet=0.3, tail=1.2):
    """Purgatory's air: a wide, damp, grey forest, a long soft reflection."""
    return reverb(x, sr, size=0.85, damp=0.65, wet=wet, tail=tail)


# --- building blocks -----------------------------------------------------------------------------------

def rustle(sr, rng, T, density=900, gain=1.0):
    """Dry chitin and leaf litter: dense tiny clicks through a high band, coming and going."""
    x = buf(sr, T)
    gate = smooth_random(sr, T, rng, 6, 0.3, 1.0)
    t = 0.0
    while t < T:
        add(x, burst(sr, rng, 0.006, rng.uniform(2800, 7200), 2.0, "band", 0.0012), t, gain * rng.uniform(0.15, 0.6) * gate(t), sr)
        t += rng.expovariate(density * gate(t) + 1)
    return x


def roots_crackle(sr, rng, T, gain=1.0):
    """Roots tearing up through wet earth: woody snaps, a low tearing groan, clods falling."""
    x = buf(sr, T)
    add(x, crackle(sr, rng, T, 0.03, 1500, 1.4), 0, 1.4 * gain, sr)
    groan = svf(noise(int(sr * T), rng), sr, lambda t: 160 + 120 * math.sin(t * 7), 5.0, "band")
    add(x, env_apply(groan, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 0.8), 0, 0.9 * gain, sr)
    for _ in range(int(T * 9)):
        t = rng.uniform(0, T * 0.9)
        add(x, burst(sr, rng, 0.05, rng.uniform(600, 1800), 3.0, "band", 0.012), t, gain * rng.uniform(0.3, 0.8), sr)
    for _ in range(int(T * 4)):
        add(x, thud(sr, 120, 60, 0.05, 0.12), rng.uniform(0.1, T * 0.95), gain * rng.uniform(0.1, 0.3), sr)
    return x


def her_voice(sr, rng, T, f0, vowel, env, rustle_gain=0.25):
    """A line of Eve's: a warm young woman's voice, and under it something dry that rustles like a thousand legs."""
    x = voiced(sr, rng, T, f0, vowel, 0.35, env)
    under = rustle(sr, rng, T, 500, 1.0)
    add(x, env_apply(under, sr, env), 0, rustle_gain, sr)
    return x


def moan(sr, rng, T, f0=140, gain=1.0):
    """A hollow soul's moan: a breathy voice on 'oo', the pitch sagging, hollowed by a band-pass, far away."""
    wob = smooth_random(sr, T, rng, 3, 0.94, 1.06)
    v = voiced(sr, rng, T, lambda t: f0 * (1 - 0.18 * t / T) * wob(t), OH, 1.6,
               lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.3)
    v = svf(v, sr, 700, 1.4, "band")
    return [s * gain for s in v]


def whoosh_air(sr, rng, T, f0, f1, gain=1.0):
    """A rush of air: noise through a band sweeping f0 -> f1, swelling and gone."""
    x = svf(noise(int(sr * T), rng), sr, lambda t: f0 * (f1 / f0) ** min(1.0, t / T), 1.2, "band")
    return env_apply([s * gain for s in x], sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 2)


def wet_peel(sr, rng, T, gain=1.0):
    """Skin coming away: a sticky tearing crackle in the mids, a wet smack at its end."""
    x = buf(sr, T)
    tear = svf(noise(int(sr * T), rng), sr, lambda t: 900 + 2200 * t / T, 3.5, "band")
    chop = smooth_random(sr, T, rng, 60, 0.0, 1.0)
    add(x, env_apply([s * chop(i / sr) ** 2 for i, s in enumerate(tear)], sr, _env(0.02, T * 0.6, T * 0.35)), 0, 1.2 * gain, sr)
    add(x, burst(sr, rng, 0.08, 500, 2.5, "band", 0.02), T * 0.85, 0.8 * gain, sr)
    return x


# --- Eve -----------------------------------------------------------------------------------------------

def eve_laugh(sr, rng):
    """A soft, fond laugh, three breaths falling -- the rustle under it swelling at the end."""
    T = 1.4
    x = buf(sr, T + 0.6)
    for i, (t, f) in enumerate(((0.0, 300), (0.22, 280), (0.42, 262), (0.64, 246))):
        d = 0.18
        add(x, her_voice(sr, rng, d, lambda tt, f=f: f * (1 - 0.15 * tt / d), AH,
                         lambda tt: min(1.0, tt / 0.012) * math.exp(-tt / 0.06), 0.2 + i * 0.15), t, 1.0 - i * 0.15, sr)
    add(x, rustle(sr, rng, 0.8, 700, 0.35), 0.7, 1.0, sr)
    return woods(x, sr, 0.22, 0.9)


def eve_call(sr, rng):
    """Mother's call: a long, rising, lilting cry, the woods answering it."""
    T = 2.6
    x = buf(sr, T + 1.2)
    f = lambda t: 260 * (1 + 0.35 * math.sin(math.pi * min(1.0, t / T))) * (1 + 0.02 * math.sin(t * 30))
    add(x, her_voice(sr, rng, T, f, OH, _env(0.25, T - 0.9, 0.65), 0.45), 0, 1.0, sr)
    # An octave-down shadow of the same call, and the forest's own voices answering faintly.
    add(x, voiced(sr, rng, T, lambda t: f(t) * 0.5, UH, 0.8, _env(0.4, T - 1.0, 0.6)), 0.05, 0.35, sr)
    for k in range(3):
        add(x, moan(sr, rng, 1.2, 120 + 30 * k, 0.4), 1.2 + 0.35 * k, 1.0, sr)
    return woods(x, sr, 0.35, 1.6)


def eve_birth(sr, rng):
    """The earth splits wet and something climbs out: a deep tear, sucking mud, a first cry."""
    T = 1.8
    x = buf(sr, T + 0.6)
    add(x, rumble(sr, rng, T, 70, 0.9, 5), 0, 2.4, sr)
    add(x, roots_crackle(sr, rng, 1.0, 0.8), 0.05, 1.0, sr)
    suck = svf(noise(int(sr * 0.7), rng), sr, lambda t: 300 + 900 * (1 - t / 0.7), 6.0, "band")
    add(x, env_apply(suck, sr, _env(0.05, 0.4, 0.25)), 0.5, 0.8, sr)
    add(x, rasp(growl(sr, rng, 0.6, 180, EE), 1.4), 1.1, 0.4, sr)
    return woods(x, sr, 0.25, 1.0)


def eve_roots(sr, rng):
    """Roots erupting: the wet woody crackle of them tearing up through the earth."""
    return woods(roots_crackle(sr, rng, 1.3, 1.0), sr, 0.2, 0.7)


def eve_roaches(sr, rng):
    """A swarm of roaches pouring out: a skittering carpet swelling and spreading, a hiss in it."""
    T = 2.4
    x = rustle(sr, rng, T, 2200, 1.0)
    hiss = svf(noise(int(sr * T), rng), sr, 5200, 0.9, "high")
    add(x, hiss, 0, 0.08, sr)
    x = env_apply(x, sr, _env(0.3, T - 0.9, 0.6))
    return woods(x, sr, 0.15, 0.5)


def eve_hurt(sr, rng):
    """A sharp, offended gasp, a hiss of the thing under her."""
    T = 0.5
    x = buf(sr, T + 0.3)
    add(x, her_voice(sr, rng, T, glide(380, 250, T), AH, lambda t: min(1.0, t / 0.02) * math.exp(-t / 0.12), 0.5), 0, 1.0, sr)
    add(x, rustle(sr, rng, 0.4, 1500, 0.5), 0.05, 1.0, sr)
    return woods(x, sr, 0.12, 0.4)


def eve_death(sr, rng):
    """She withers: a long cry falling away, turning to dry crackling as the roots and roaches leave her."""
    T = 3.6
    x = buf(sr, T + 1.4)
    cry = her_voice(sr, rng, 2.2, glide(420, 150, 2.2, 0.8), AH, _env(0.08, 1.4, 0.7), 0.6)
    add(x, rasp(cry, 1.3), 0, 1.0, sr)
    add(x, crackle(sr, rng, 2.6, 0.05, 2600, 1.2), 1.0, 1.6, sr)
    dry = rustle(sr, rng, 2.4, 1200, 0.7)
    add(x, env_apply(dry, sr, lambda t: math.exp(-t / 1.0)), 1.4, 1.0, sr)
    return woods(x, sr, 0.3, 1.4)


def eve_whisper(sr, rng):
    """Her ambient murmur: words that never resolve, close, with roaches stirring."""
    T = 1.8
    x = whisper(sr, rng, T, 7, 1.0)
    add(x, rustle(sr, rng, T, 300, 0.3), 0, 1.0, sr)
    return woods(x, sr, 0.25, 0.8)


# --- the Alphas ----------------------------------------------------------------------------------------

def alpha_vampire_command(sr, rng):
    """The old vampire's command: a long, low, aristocratic hiss through bared teeth, a breathed word under it."""
    T = 1.2
    x = buf(sr, T + 0.4)
    hiss = svf(noise(int(sr * T), rng), sr, lambda t: 3400 + 1400 * t / T, 2.2, "band")
    add(x, env_apply(hiss, sr, _env(0.08, 0.7, 0.4)), 0, 1.0, sr)
    add(x, voiced(sr, rng, 0.8, glide(110, 82, 0.8), UH, 1.2, _env(0.06, 0.4, 0.3)), 0.1, 0.5, sr)
    return reverb(x, sr, size=0.6, damp=0.55, wet=0.18, tail=0.6)


def alpha_vampire_lunge(sr, rng):
    """His lunge: a rush of air past the ear, a coat snapping."""
    T = 0.45
    x = buf(sr, T + 0.2)
    add(x, whoosh_air(sr, rng, T, 400, 2600, 1.0), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, 1200, 1.5, "band", 0.01), T * 0.7, 0.6, sr)
    return x


def alpha_werewolf_howl(sr, rng):
    """The Alpha's howl: deep and enormous, rising and holding, a growl in its throat."""
    T = 3.0
    f = lambda t: 150 * (1 + 0.55 * math.sin(math.pi * min(1.0, t / (T * 0.8))) ** 0.6) * (1 + 0.015 * math.sin(t * 34))
    v = voiced(sr, rng, T, f, OH, 0.9, _env(0.3, T - 1.1, 0.8))
    x = buf(sr, T + 1.4)
    add(x, rasp(v, 1.5), 0, 1.0, sr)
    add(x, growl(sr, rng, 0.6, 60, OH), 0, 0.5, sr)
    return woods(x, sr, 0.35, 1.6)


def alpha_werewolf_slam(sr, rng):
    """Both arms down: a heavy thud through the ground, dirt flung, a grunt."""
    T = 1.0
    x = buf(sr, T + 0.4)
    add(x, thud(sr, 90, 38, 0.18, 0.7), 0, 1.6, sr)
    add(x, burst(sr, rng, 0.2, 900, 0.8, "band", 0.05), 0.01, 0.8, sr)
    add(x, rumble(sr, rng, 0.8, 60, 0.3, 3), 0.02, 1.2, sr)
    add(x, rasp(growl(sr, rng, 0.35, 70, UH), 1.6), 0.0, 0.45, sr)
    return reverb(x, sr, size=0.6, damp=0.6, wet=0.15, tail=0.5)


def alpha_shapeshifter_steal(sr, rng):
    """Taking a face: a hand on skin, then the long sticky tear and a soft wet ripple."""
    T = 1.3
    x = buf(sr, T + 0.3)
    add(x, wet_peel(sr, rng, 0.9, 1.0), 0.1, 1.0, sr)
    rip = svf(noise(int(sr * 0.5), rng), sr, lambda t: 300 + 400 * math.sin(t * 40), 5.0, "band")
    add(x, env_apply(rip, sr, _env(0.05, 0.25, 0.2)), 0.8, 0.6, sr)
    return reverb(x, sr, size=0.4, damp=0.6, wet=0.1, tail=0.3)


def alpha_shapeshifter_slough(sr, rng):
    """He shrugs off a sheet of skin: a wet peel and a heavy slap on the ground."""
    T = 0.9
    x = buf(sr, T + 0.3)
    add(x, wet_peel(sr, rng, 0.55, 0.9), 0, 1.0, sr)
    add(x, thud(sr, 160, 70, 0.05, 0.2), 0.62, 0.9, sr)
    add(x, burst(sr, rng, 0.07, 700, 2.0, "band", 0.02), 0.62, 0.7, sr)
    return x


# --- the souls -----------------------------------------------------------------------------------------

def soul_ambient(sr, rng):
    T = 2.0
    x = buf(sr, T + 1.2)
    add(x, moan(sr, rng, T, rng.uniform(120, 150), 1.0), 0, 1.0, sr)
    return woods(x, sr, 0.4, 1.4)


def soul_hurt(sr, rng):
    T = 0.45
    x = buf(sr, T + 0.6)
    add(x, moan(sr, rng, T, 210, 1.0), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.15, 2600, 1.0, "band", 0.04), 0, 0.25, sr)
    return woods(x, sr, 0.3, 0.6)


def soul_death(sr, rng):
    """A long sigh dispersing into the woods."""
    T = 2.2
    x = buf(sr, T + 1.2)
    add(x, moan(sr, rng, 1.0, 170, 0.8), 0, 1.0, sr)
    sigh = svf(noise(int(sr * T), rng), sr, lambda t: 1800 - 1200 * t / T, 1.0, "band")
    add(x, env_apply(sigh, sr, lambda t: min(1.0, t / 0.2) * math.exp(-t / 0.7)), 0.2, 0.6, sr)
    return woods(x, sr, 0.45, 1.6)


# --- the rift ------------------------------------------------------------------------------------------

def rift_open(sr, rng):
    """A deep tearing whoosh, the air pulled in, voices far inside it."""
    T = 2.6
    x = buf(sr, T + 1.4)
    add(x, whoosh_air(sr, rng, T, 90, 900, 1.4), 0, 1.0, sr)
    add(x, rumble(sr, rng, T, 55, 1.4, 4), 0, 2.0, sr)
    add(x, crackle(sr, rng, 0.6, 0.08, 3000, 1.0, 0.25), 0.1, 1.0, sr)
    for k in range(4):
        add(x, moan(sr, rng, 1.3, 110 + 25 * k, 0.35), 0.6 + 0.3 * k, 1.0, sr)
    return woods(soft_clip(x, 1.2), sr, 0.35, 1.6)


# name -> (fn(sr, rng) -> float list, peak)
SOUNDS = {
    "eve_laugh": (eve_laugh, -2.0),
    "eve_call": (eve_call, -1.5),
    "eve_birth": (eve_birth, -1.5),
    "eve_roots": (eve_roots, -1.5),
    "eve_roaches": (eve_roaches, -3.0),
    "eve_hurt": (eve_hurt, -2.0),
    "eve_death": (eve_death, -1.5),
    "eve_whisper": (eve_whisper, -4.0),
    "alpha_vampire_command": (alpha_vampire_command, -2.0),
    "alpha_vampire_lunge": (alpha_vampire_lunge, -2.0),
    "alpha_werewolf_howl": (alpha_werewolf_howl, -1.5),
    "alpha_werewolf_slam": (alpha_werewolf_slam, -1.0),
    "alpha_shapeshifter_steal": (alpha_shapeshifter_steal, -2.5),
    "alpha_shapeshifter_slough": (alpha_shapeshifter_slough, -2.5),
    "soul_ambient": (soul_ambient, -5.0),
    "soul_hurt": (soul_hurt, -3.0),
    "soul_death": (soul_death, -3.0),
    "rift_open": (rift_open, -1.5),
}
