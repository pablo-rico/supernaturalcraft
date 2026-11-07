"""v0.39 Godstiel and Castiel: every sound in GodstielAssets.SOUNDS, written by generate.py to sounds/godstiel/<id>.ogg
(supernaturalcraft:godstiel.<id>).

The palette runs from light to goo, as the fight does. Castiel's: great shadow wings (a deep push of air, a rustle of
feathers that are not quite there); the finger snap that smites (a dry click and a crack of light tearing the air); the
smite (a bright, tearing burst); judgment (a choir chord swelling and collapsing into a burst); radiance (a rising shimmer
breaking into a wash of light). Then the Leviathans: cracks (glass and bone splintering, a ring of light in them); voices
(layered whispers, wet and many, under a low gurgle); the bite (the chest maw: a gasp of air, teeth, a wet crunch); goo (thick
squelches, bubbles); the struggle (a man's strained breath through his teeth, the voices fighting him); hurt; death (the
Leviathans tearing out of him in a roar of teeth and goo, the light guttering out, a last breath).

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from coven_sfx import EH, glide, rasp
from leviathan_sfx import bubbles, gurgle, maw_roar, squelch, tear, teeth
from leviathan_sfx import snap as jaws
from magic_sfx import AH, OH, whisper
from michael_sfx import hall, ring, whoosh
from raphael_sfx import crackle, rumble
from sfx import UH, burst, choir, sparkle, thud, voiced
from synth import add, buf, env_apply, noise, onepole_lp, reverb, reverse, smooth_random, soft_clip, svf, tone


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def his_voice(sr, rng, T, f0, vowel, env, breath=0.6):
    """Castiel's voice: low, flat, gravelly (a little rasp on a man's baritone)."""
    return rasp(voiced(sr, rng, T, f0, vowel, breath, env), 1.3)


def tearing_light(sr, rng, T=0.9, f0=900, f1=5200, gain=1.0):
    """Light tearing the air: a bright band of noise ripping upward with a crackle in it, and a pure high ring."""
    x = buf(sr, T + 0.2)
    rip = svf(noise(int(sr * T), rng), sr, lambda t: f0 * (f1 / f0) ** min(1.0, t / (T * 0.4)), 2.5, "band")
    add(x, env_apply(rip, sr, lambda t: min(1.0, t / 0.01) * math.exp(-t / (T * 0.35))), 0, 0.9 * gain, sr)
    add(x, crackle(sr, rng, T * 0.7, 0.12, 4800, 0.9, T * 0.25), 0.0, 0.5 * gain, sr)
    add(x, tone(sr, 2093, T, decay=T * 0.4, attack=0.003), 0.0, 0.18 * gain, sr)
    add(x, tone(sr, 3136, T, decay=T * 0.3, attack=0.003), 0.0, 0.1 * gain, sr)
    return x


def many_whispers(sr, rng, T, n=5, gain=1.0):
    """The Leviathans' voices: several whispers at once, each a little lower and rougher, wandering in and out."""
    x = buf(sr, T)
    for i in range(n):
        w = whisper(sr, rng, T, int(T * 4) + 2, 1.0)
        w = svf(w, sr, 2400 - i * 300, 0.8, "band" if i % 2 else "low")
        gate = smooth_random(sr, T, rng, 2.0, 0.2, 1.0)
        add(x, env_apply(w, sr, lambda t, g=gate: g(t)), rng.uniform(0, 0.15), gain * (0.6 - i * 0.06), sr)
    return x


# --- Castiel, the new God --------------------------------------------------------------------------------------------

def wings(sr, rng):
    """His wings: a great soft downstroke of shadow, the rustle of smoke-feathers, a breath of light at its edge."""
    T = 1.3
    x = buf(sr, T + 0.4)
    add(x, whoosh(sr, rng, 0.85, 160, 420, 0.8, 0.42), 0, 1.0, sr)
    add(x, onepole_lp(whoosh(sr, rng, 0.8, 70, 140, 0.6, 0.4), sr, 240), 0.02, 1.4, sr)
    rus = svf(noise(int(sr * 0.7), rng), sr, 4600, 1.4, "band")
    rus = env_apply(rus, sr, lambda t: (0.5 + 0.5 * math.sin(t * 80)) * math.sin(math.pi * min(1.0, t / 0.7)))
    add(x, rus, 0.1, 0.16, sr)
    add(x, sparkle(sr, rng, 6, 0.25, 0.9, T, 3200, 6400, rise=False), 0, 0.08, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.18, tail=0.6)


def snap(sr, rng):
    """The snap that smites: a dry finger click, then a crack of light tearing the air, and the room ringing after."""
    x = buf(sr, 1.6)
    add(x, burst(sr, rng, 0.02, 1500, 0.8, "high", 0.0015), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, 2300, 4.0, "band", 0.01), 0.0, 0.8, sr)
    add(x, tearing_light(sr, rng, 0.7, 1200, 6000, 0.9), 0.05, 1.0, sr)
    add(x, thud(sr, 110, 45, 0.08, 0.4), 0.05, 0.7, sr)
    return hall(x, sr, 0.25, 1.0)


def smite(sr, rng):
    """The smiting: a searing tone rising, then a bright tearing burst as the light goes through someone."""
    T = 1.8
    x = buf(sr, T)
    sear = tone(sr, 480, 0.6, glide=1800, attack=0.02)
    add(x, env_apply(sear, sr, lambda t: min(1.0, t / 0.25) * max(0.0, 1 - t / 0.6)), 0, 0.25, sr)
    add(x, tearing_light(sr, rng, 1.0, 700, 5600, 1.0), 0.45, 1.0, sr)
    add(x, burst(sr, rng, 0.35, 1600, 0.7, "band", 0.1), 0.45, 0.7, sr)
    add(x, thud(sr, 90, 40, 0.18, 0.7), 0.45, 0.9, sr)
    add(x, choir(sr, rng, (76, 83), 0.8, "ah", voices=2, detune=16), 0.45, 0.25, sr)
    return hall(soft_clip(x, 1.4), sr, 0.25, 1.1)


def judgment(sr, rng):
    """Judgment: the choir swells on a great chord, holds -- then the chord collapses inward and bursts."""
    T = 3.6
    x = buf(sr, T)
    swell = 1.2
    c = choir(sr, rng, (50, 57, 62, 66, 69, 74, 81), swell + 0.3, "ah", voices=3, detune=10)
    add(x, env_apply(c, sr, lambda t: min(1.0, (t / swell) ** 2) * (1.0 if t < swell else max(0.0, 1 - (t - swell) / 0.3))),
        0, 2.4, sr)
    # The collapse: a reversed burst sucking in, the chord bending down into it.
    add(x, reverse(env_apply(svf(noise(int(sr * 0.35), rng), sr, 1800, 0.7, "band"), sr, lambda t: math.exp(-t / 0.1))),
        swell - 0.05, 0.6, sr)
    bend = choir(sr, rng, (62, 69), 0.3, "oh", voices=2, detune=30)
    add(x, env_apply(bend, sr, lambda t: math.exp(-t / 0.1)), swell, 0.7, sr)
    add(x, tearing_light(sr, rng, 1.2, 500, 5000, 1.2), swell + 0.28, 1.0, sr)
    add(x, thud(sr, 70, 30, 0.3, 1.2), swell + 0.28, 1.2, sr)
    add(x, rumble(sr, rng, 1.8, 70, 0.7, 4), swell + 0.3, 1.4, sr)
    return hall(soft_clip(x, 1.3), sr, 0.3, 1.6)


def radiance(sr, rng):
    """Radiance: a shimmer climbing fast, then breaking into a wide wash of light with a high choir in it."""
    T = 2.8
    x = buf(sr, T)
    rise = 0.9
    sh = svf(noise(int(sr * rise), rng), sr, lambda t: 600 * (8.0 ** (t / rise)), 4.0, "band")
    add(x, env_apply(sh, sr, lambda t: (t / rise) ** 2), 0, 0.6, sr)
    add(x, sparkle(sr, rng, 18, 0.1, rise, T, 2200, 7000, rise=True), 0, 0.3, sr)
    wash = svf(noise(int(sr * 1.8), rng), sr, 3000, 0.5, "band")
    add(x, env_apply(wash, sr, lambda t: min(1.0, t / 0.02) * math.exp(-t / 0.6)), rise, 0.8, sr)
    add(x, choir(sr, rng, (74, 81, 86), 1.6, "ah", voices=3, detune=12), rise, 0.6, sr)
    add(x, thud(sr, 80, 40, 0.15, 0.6), rise, 0.7, sr)
    return hall(x, sr, 0.35, 1.6)


# --- the Leviathans in him ---------------------------------------------------------------------------------------------

def crack(sr, rng):
    """He cracks: glass splintering and bone snapping, light ringing out of the fissures, a groan through his teeth."""
    T = 2.0
    x = buf(sr, T)
    for i in range(9):
        at = 0.02 * i + rng.uniform(0, 0.03)
        add(x, burst(sr, rng, 0.015, rng.uniform(2800, 6000), 1.4, "band", 0.002), at, rng.uniform(0.5, 1.0), sr)
    add(x, ring(sr, 2600, 1.2, 0.8), 0.0, 0.12, sr)
    add(x, ring(sr, 3900, 0.9, 0.6), 0.05, 0.08, sr)
    for i in range(4):
        add(x, burst(sr, rng, 0.05, rng.uniform(500, 900), 2.0, "band", 0.012), 0.08 + 0.07 * i, 0.7, sr)    # bone
        add(x, thud(sr, 220, 120, 0.02, 0.08), 0.08 + 0.07 * i, 0.4, sr)
    add(x, crackle(sr, rng, 1.0, 0.1, 4400, 0.9, 0.4), 0.05, 0.4, sr)
    groan = his_voice(sr, rng, 1.0, glide(120, 92, 1.0), UH, _env(0.1, 0.5, 0.4), 0.9)
    add(x, groan, 0.3, 0.4, sr)
    return hall(x, sr, 0.2, 0.8)


def voices(sr, rng):
    """The Leviathans speaking in him: many wet whispers at once, a gurgle under them, a single voice surfacing and gone."""
    T = 3.2
    x = buf(sr, T)
    add(x, many_whispers(sr, rng, T, 6, 1.0), 0, 1.0, sr)
    add(x, gurgle(sr, rng, T, 62, 520, 0.5), 0, 0.5, sr)
    surf = voiced(sr, rng, 0.8, glide(96, 70, 0.8), OH, 1.1, _env(0.2, 0.3, 0.3))
    add(x, rasp(surf, 2.2), 1.3, 0.35, sr)
    return reverb(x, sr, size=0.5, damp=0.55, wet=0.22, tail=0.6)


def bite(sr, rng):
    """The chest maw: a rush of air into it, the teeth slamming shut, a wet crunch and a low snarl."""
    x = buf(sr, 1.4)
    add(x, jaws(sr, rng, 1.2), 0.0, 1.0, sr)
    add(x, teeth(sr, rng, 8, 0.012, 3600, 0.6), 0.2, 1.0, sr)
    add(x, maw_roar(sr, rng, 0.7, 96, 64, 0.6), 0.22, 0.6, sr)
    return reverb(x, sr, size=0.5, damp=0.5, wet=0.12, tail=0.4)


def goo(sr, rng):
    """Goo: thick, slow squelches, bubbles welling up and popping, a slap as it lands."""
    T = 1.6
    x = buf(sr, T)
    add(x, squelch(sr, rng, 0.5, 220, 1100, 4.0), 0.0, 0.9, sr)
    add(x, squelch(sr, rng, 0.4, 900, 260, 3.0), 0.45, 0.7, sr)
    add(x, bubbles(sr, rng, T, 14, 140, 420, 0.12), 0.0, 0.6, sr)
    add(x, burst(sr, rng, 0.08, 500, 1.2, "band", 0.03), 0.9, 0.6, sr)
    add(x, thud(sr, 140, 60, 0.05, 0.2), 0.9, 0.5, sr)
    return reverb(x, sr, size=0.5, damp=0.6, wet=0.14, tail=0.4)


def struggle(sr, rng):
    """The inner fight: a man's strained breath hissed through clenched teeth, a groan breaking out, the voices fighting him."""
    T = 3.0
    x = buf(sr, T)
    for t0, d, inhale in ((0.0, 0.5, True), (0.6, 0.7, False), (1.4, 0.45, True), (1.95, 0.8, False)):
        br = svf(noise(int(sr * d), rng), sr, 3200 if inhale else 2200, 2.0, "band")
        wob = smooth_random(sr, d, rng, 25, 0.4, 1.0)
        add(x, env_apply(br, sr, lambda t, d=d, w=wob: math.sin(math.pi * t / d) * w(t)), t0, 0.45, sr)
    g = his_voice(sr, rng, 0.9, lambda t: 110 + 30 * math.sin(t * 6), EH, _env(0.08, 0.5, 0.3), 1.0)
    add(x, soft_clip([s * 1.6 for s in g], 1.6), 0.7, 0.55, sr)
    add(x, many_whispers(sr, rng, T, 4, 0.8), 0.1, 0.6, sr)
    add(x, gurgle(sr, rng, 1.2, 58, 450, 0.4), 1.6, 0.4, sr)
    return reverb(x, sr, size=0.5, damp=0.5, wet=0.15, tail=0.5)


def hurt(sr, rng):
    """A blow lands: a short grunt through his teeth, a crackle of light, something wet stirring."""
    x = buf(sr, 0.8)
    add(x, his_voice(sr, rng, 0.35, lambda t: 132 * (0.75 ** min(1.0, t / 0.35)), UH,
                     lambda t: min(1.0, t / 0.015) * math.exp(-max(0.0, t - 0.04) / 0.1), 0.7), 0.0, 1.0, sr)
    add(x, crackle(sr, rng, 0.3, 0.12, 4600, 0.9, 0.1), 0.0, 0.35, sr)
    add(x, squelch(sr, rng, 0.25, 600, 250, 3.0), 0.08, 0.3, sr)
    return reverb(x, sr, size=0.45, damp=0.5, wet=0.1, tail=0.3)


def death(sr, rng):
    """The Leviathans tear out of him: the voices rising to a roar of teeth and goo, flesh tearing, the light guttering out
    in a falling ring, and a last breath."""
    T = 5.6
    x = buf(sr, T)
    swell = many_whispers(sr, rng, 2.4, 6, 1.0)
    add(x, env_apply(swell, sr, lambda t: min(1.0, t / 2.2)), 0.0, 0.9, sr)
    add(x, gurgle(sr, rng, 2.6, 60, 600, 0.6), 0.2, 0.6, sr)
    scream = voiced(sr, rng, 1.6, glide(130, 330, 1.4), AH, 0.8, _env(0.3, 1.0, 0.3))
    add(x, rasp(scream, 2.4), 0.9, 0.5, sr)
    add(x, maw_roar(sr, rng, 1.8, 110, 60, 1.0), 2.3, 1.0, sr)
    add(x, tear(sr, rng, 0.8), 2.3, 0.8, sr)
    add(x, teeth(sr, rng, 20, 0.04, 3600, 0.5), 2.4, 1.0, sr)
    add(x, thud(sr, 70, 30, 0.3, 1.0), 2.3, 1.0, sr)
    # The light going out: a falling ring and a shimmer dying.
    fall = tone(sr, 1760, 1.8, attack=0.01, glide=220)
    add(x, env_apply(fall, sr, lambda t: math.exp(-t / 0.7)), 2.4, 0.25, sr)
    add(x, squelch(sr, rng, 0.6, 400, 120, 3.0), 3.6, 0.6, sr)
    sigh = voiced(sr, rng, 0.9, glide(105, 80, 0.9), UH, 1.3, _env(0.1, 0.2, 0.6))
    add(x, sigh, 4.4, 0.35, sr)
    return hall(soft_clip(x, 1.3), sr, 0.22, 1.2)


SOUNDS = {
    "wings": (wings, -2.0), "snap": (snap, -1.5), "smite": (smite, -1.0), "judgment": (judgment, -1.0), "radiance": (radiance, -1.5),
    "crack": (crack, -1.5), "voices": (voices, -3.0), "bite": (bite, -1.5), "goo": (goo, -2.0), "struggle": (struggle, -2.5),
    "hurt": (hurt, -2.0), "death": (death, -1.0),
}
