"""v0.16 the Archangel Raphael: the storm and the healer. Same conventions as sfx.py / michael_sfx.py: (sr, rng) -> float
list, finished and encoded by generate.py into sounds/raphael/<id>.ogg (RaphaelAssetData points supernaturalcraft:raphael.<id>
at them; the ids are RaphaelAssets.SOUNDS).

The storm: thunder (a tearing crack and a long rolling rumble), lightning (a dense electric crackle and buzz), gusts of
storm wind for his wings. The healer: a low, warm choir and soft bells. His voice is deep and controlled.
"""

import math

from michael_sfx import hall, whoosh
from sfx import UH, HUM, bell_voice, burst, choir, sparkle, thud, voiced
from synth import (add, buf, env_apply, midi_hz, noise, onepole_lp, reverb, smooth_random, soft_clip, svf, tone)


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def crackle(sr, rng, T, density=0.06, fc=3400, q=0.9, decay=None):
    """Electricity: sparse random impulses through a band-pass, in bursts that come and go."""
    n = int(sr * T)
    gate = smooth_random(sr, T, rng, 30, 0.0, 1.0)
    x = [0.0] * n
    for i in range(n):
        if rng.random() < density * gate(i / sr) ** 2:
            x[i] = rng.uniform(-1, 1)
    x = svf(x, sr, fc, q, "band")
    if decay:
        x = env_apply(x, sr, lambda t: math.exp(-t / decay))
    return x


def buzz(sr, rng, T, f=118):
    """The hum of an arc: a rough low tone with its odd harmonics."""
    x = buf(sr, T)
    for k, a in ((1, 1.0), (3, 0.45), (5, 0.25), (7, 0.12)):
        add(x, tone(sr, f * k * (1 + rng.uniform(-0.004, 0.004)), T), 0, a * 0.3, sr)
    return soft_clip(x, 2.0)


def rumble(sr, rng, T, f0=90, decay=2.0, rolls=6):
    """Thunder's roll: low-passed noise swelling and fading in uneven waves."""
    x = onepole_lp(onepole_lp(noise(int(sr * T), rng), sr, f0 * 2.2), sr, f0 * 1.6)
    waves = smooth_random(sr, T, rng, rolls / T, 0.25, 1.0)
    return env_apply([a * waves(i / sr) for i, a in enumerate(x)], sr, lambda t: min(1.0, t / 0.08) * math.exp(-t / decay))


def thunder_crack(sr, rng, T=4.0, gain=1.0):
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.25, 2600, 0.5, "high", 0.05), 0.0, 1.0 * gain, sr)
    add(x, burst(sr, rng, 0.6, 700, 0.7, "band", 0.18), 0.005, 0.9 * gain, sr)
    add(x, crackle(sr, rng, 0.35, 0.25, 4200, 0.8, 0.12), 0.0, 0.8 * gain, sr)
    add(x, rumble(sr, rng, T - 0.05, 85, T * 0.35, 7), 0.04, 3.2 * gain, sr)
    add(x, thud(sr, 70, 32, 0.4, 1.2), 0.0, 0.8 * gain, sr)
    return x


# --- the sounds ------------------------------------------------------------------------------------------

def thunder(sr, rng):
    """Thunder close by: the tearing crack right overhead and its roll across the sky."""
    return reverb(soft_clip(thunder_crack(sr, rng, 4.5), 1.3), sr, size=0.9, damp=0.4, wet=0.25, tail=1.5)


def arrive(sr, rng):
    """He comes down in a bolt: a rising whine of charge, the strike, thunder, and a deep chord of the Host under it."""
    T = 6.0
    x = buf(sr, T)
    rise = tone(sr, 180, 1.0, glide=1400, attack=0.4)
    add(x, env_apply(rise, sr, lambda t: (t / 1.0) ** 2), 0.0, 0.25, sr)
    add(x, crackle(sr, rng, 1.0, 0.03, 3000, 0.8), 0.0, 0.4, sr)
    add(x, thunder_crack(sr, rng, 5.0, 1.1), 1.0, 1.0, sr)
    c = choir(sr, rng, (38, 45, 50), 3.5, "ah", voices=3, detune=8)
    add(x, env_apply(c, sr, _env(0.6, 1.5, 1.4)), 1.3, 0.9, sr)
    return hall(soft_clip(x, 1.2), sr, 0.25, 1.8)


def smite(sr, rng):
    """The smite: the charge in his raised palm (a rising electric whine), the fist closing, a burst of light."""
    T = 2.4
    x = buf(sr, T)
    whine = tone(sr, 300, 1.2, glide=2200, attack=0.05)
    add(x, env_apply(whine, sr, lambda t: min(1.0, t / 0.3) * (0.5 + 0.5 * t / 1.2)), 0.0, 0.18, sr)
    add(x, crackle(sr, rng, 1.2, 0.08, 3800, 1.0), 0.0, 0.45, sr)
    add(x, buzz(sr, rng, 1.2, 96), 0.0, 0.15, sr)
    # The burst at 1.2 s.
    add(x, burst(sr, rng, 0.4, 1800, 0.6, "band", 0.09), 1.2, 1.0, sr)
    add(x, thud(sr, 110, 34, 0.35, 1.0), 1.2, 1.1, sr)
    add(x, crackle(sr, rng, 0.8, 0.3, 4400, 0.8, 0.25), 1.2, 0.7, sr)
    add(x, choir(sr, rng, (62, 69), 0.9, "ah", voices=2, detune=14), 1.2, 0.35, sr)
    return hall(soft_clip(x, 1.4), sr, 0.25, 1.2)


def snap(sr, rng):
    """His finger snap, and the air bursting a beat after it."""
    T = 3.0
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.02, 1300, 0.8, "high", 0.0016), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, 2100, 4.0, "band", 0.011), 0.0, 0.8, sr)
    add(x, thud(sr, 240, 160, 0.008, 0.05), 0.0, 0.25, sr)
    boom = thunder_crack(sr, rng, 2.6, 0.8)
    add(x, boom, 0.18, 0.9, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.92, damp=0.35, wet=0.25, tail=1.2, predelay=0.02)


def heal(sr, rng):
    """Grace hums along a thread: a warm low choir and a soft bell, shimmering."""
    T = 3.2
    x = buf(sr, T)
    c = choir(sr, rng, (55, 62, 67), T, "oo", voices=3, detune=6)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / T) ** 1.5), 0, 1.2, sr)
    add(x, bell_voice(sr, midi_hz(79), 2.5, bright=0.6, beat=0.8), 0.1, 0.18, sr)
    add(x, sparkle(sr, rng, 10, 0.3, 2.6, T, 2400, 6000, rise=True), 0, 0.35, sr)
    return hall(x, sr, 0.3, 1.2)


def wings(sr, rng):
    """Great wings of storm cloud: a gust that pushes the air, thunder muttering in it, a flicker of static."""
    T = 1.6
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 1.0, 140, 380, 0.7, 0.4), 0, 1.1, sr)
    add(x, onepole_lp(whoosh(sr, rng, 1.0, 60, 120, 0.6, 0.4), sr, 220), 0.02, 1.4, sr)
    add(x, rumble(sr, rng, 1.4, 70, 0.6, 3), 0.1, 1.6, sr)
    add(x, crackle(sr, rng, 0.6, 0.04, 4000, 1.0, 0.3), 0.3, 0.35, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.18, tail=0.6)


def trapped(sr, rng):
    """Holy fire roars up round him: a whoomp of ignition, a roaring, crackling ring, and his snarl of pain."""
    T = 3.0
    x = buf(sr, T)
    add(x, onepole_lp(whoosh(sr, rng, 0.6, 120, 900, 0.7, 0.3), sr, 1500), 0, 1.3, sr)
    roar = svf(noise(int(sr * (T - 0.2)), rng), sr, 700, 0.6, "band")
    roar = env_apply(roar, sr, lambda t: min(1.0, t / 0.3) * (0.7 + 0.3 * math.sin(t * 9)) * max(0.0, 1 - (t - 2.2) / 0.6 if t > 2.2 else 1))
    add(x, roar, 0.15, 0.7, sr)
    pops = [0.0] * int(sr * (T - 0.2))
    for i in range(len(pops)):
        if rng.random() < 0.0009:
            pops[i] = rng.uniform(-1, 1)
    add(x, svf(pops, sr, 2500, 1.2, "band"), 0.15, 2.0, sr)
    v = voiced(sr, rng, 0.9, lambda t: 95 * (0.85 ** (t / 0.9)), UH, 0.7, _env(0.08, 0.4, 0.42))
    add(x, v, 0.35, 0.7, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.6, damp=0.5, wet=0.15, tail=0.6)


def ambient(sr, rng):
    """He breathes and the storm with him: a slow low breath, a distant roll of thunder, rain-hiss."""
    T = 3.4
    x = buf(sr, T)
    br = svf(noise(int(sr * 1.6), rng), sr, 600, 0.8, "band")
    add(x, env_apply(br, sr, lambda t: math.sin(math.pi * t / 1.6) ** 2), 0.1, 0.4, sr)
    add(x, rumble(sr, rng, T - 0.6, 60, 1.4, 3), 0.5, 1.5, sr)
    hiss = onepole_lp(noise(int(sr * T), rng), sr, 5000)
    add(x, env_apply(svf(hiss, sr, 3000, 0.5, "high"), sr, lambda t: 0.08 * math.sin(math.pi * t / T)), 0, 1.0, sr)
    return reverb(x, sr, size=0.8, damp=0.5, wet=0.2, tail=0.8)


def hurt(sr, rng):
    """A blow lands: a deep grunt, the grace crackling at the wound."""
    T = 0.9
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.03, 2000, 0.8, "high", 0.004), 0, 0.6, sr)
    v = voiced(sr, rng, 0.45, lambda t: 105 * (0.8 ** (t / 0.45)), UH, 0.6, _env(0.02, 0.12, 0.3))
    add(x, v, 0.01, 1.0, sr)
    add(x, crackle(sr, rng, 0.5, 0.12, 3600, 1.0, 0.15), 0.0, 0.6, sr)
    return reverb(x, sr, size=0.5, damp=0.5, wet=0.12, tail=0.4)


def death(sr, rng):
    """The archangel of the storm falls: his voice tears into a scream of light, thunder breaks, the choir rises and fades."""
    T = 7.5
    x = buf(sr, T)
    v = voiced(sr, rng, 2.4, lambda t: 110 * (2.6 ** min(1.0, t / 2.0)), UH, 0.8, _env(0.2, 1.6, 0.6))
    add(x, v, 0.4, 0.9, sr)
    sc = choir(sr, rng, (60, 67, 72), 2.4, "ah", voices=3, detune=18)
    sc = svf(sc, sr, lambda t: 500 * (10 ** min(1.0, t / 2.0)), 0.8, "low")
    add(x, env_apply(sc, sr, _env(1.2, 0.8, 0.4)), 0.8, 1.2, sr)
    add(x, thunder_crack(sr, rng, 4.5, 1.3), 3.0, 1.0, sr)
    add(x, sparkle(sr, rng, 40, 3.1, 6.8, T, 2600, 9000), 0, 0.8, sr)
    rise = choir(sr, rng, (67, 74, 79), 3.6, "oo", voices=3, detune=10)
    add(x, env_apply(rise, sr, lambda t: min(1.0, t / 1.5) * max(0.0, 1 - t / 3.6)), 3.4, 0.8, sr)
    return hall(soft_clip(x, 1.3), sr, 0.3, 2.0)


def stormcaller_zap(sr, rng):
    """The Stormcaller's bolt leaping from foe to foe: three snapping cracks over a buzzing arc."""
    T = 1.0
    x = buf(sr, T)
    add(x, buzz(sr, rng, 0.55, 140), 0.0, 0.3, sr)
    for i, at in enumerate((0.0, 0.14, 0.27)):
        add(x, burst(sr, rng, 0.08, 3000 - i * 400, 0.7, "high", 0.02), at, 0.9 - 0.15 * i, sr)
        add(x, crackle(sr, rng, 0.25, 0.25, 4400, 0.9, 0.08), at, 0.6, sr)
        add(x, thud(sr, 150, 60, 0.05, 0.2), at, 0.4, sr)
    return reverb(soft_clip(x, 1.3), sr, size=0.6, damp=0.4, wet=0.15, tail=0.4)


def stormcaller_heal(sr, rng):
    """A healing grace settles: a soft chord of bells and a breath of choir."""
    T = 2.2
    x = buf(sr, T)
    for i, m in enumerate((72, 76, 79)):
        add(x, bell_voice(sr, midi_hz(m), 1.8, bright=0.5, beat=0.7), i * 0.06, 0.18, sr)
    c = choir(sr, rng, (60, 67), 1.8, "oo", voices=2, detune=6)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / 1.8)), 0.05, 0.8, sr)
    add(x, sparkle(sr, rng, 8, 0.1, 1.6, T, 2600, 6500), 0, 0.3, sr)
    return hall(x, sr, 0.25, 0.9)


SOUNDS = {
    "arrive": (arrive, -1.0), "thunder": (thunder, -1.0), "smite": (smite, -1.0), "snap": (snap, -1.0), "heal": (heal, -3.0),
    "wings": (wings, -2.0), "trapped": (trapped, -1.5), "ambient": (ambient, -4.0), "hurt": (hurt, -2.0), "death": (death, -1.0),
    "stormcaller_zap": (stormcaller_zap, -1.5), "stormcaller_heal": (stormcaller_heal, -3.0),
}
