"""v0.36 "The Word and the Ward": the wall sigils, the crypt and the Word of God tablets, written by generate.py to
sounds/word/<id>.ogg (supernaturalcraft:word.<id>).

  banish_blast    the banishing sigil slapped: a blinding angelic shriek climbing over a whoosh that sucks everything away
  ward_hum        an Enochian ward holding: a low holy hum, a fifth and an octave, slowly beating (seamless 2 s loop)
  alarm_chime     the alarm sigil waking: an eerie, slightly detuned bell with a dissonant overtone and a breath of air
  tablet_whisper  reading the tablet: layered whispering voices over a faint, distant choir drone
  glyph_wrong     a wrong glyph: a sharp, piercing ringing tone, like a headache behind the eyes, with a dull knock
  oil_ignite      holy oil catching: a soft whoomph and the fire running, crackling

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from magic_sfx import whisper
from michael_sfx import hall, ring, whoosh
from raphael_sfx import crackle
from sfx import bell_voice, burst, choir, sparkle, thud
from synth import add, buf, env_apply, noise, smooth_random, svf, tone

TAU = 2 * math.pi
LOOP_T = 2.0
XFADE = 0.4


def _loop(x, sr):
    """Folds the tail over the head (equal-power crossfade): x is LOOP_T + XFADE long; returns LOOP_T seconds."""
    n, f = int(sr * LOOP_T), int(sr * XFADE)
    out = x[:n]
    for i in range(f):
        k = i / f
        out[i] = x[i] * math.sin(k * math.pi / 2) + x[n + i] * math.cos(k * math.pi / 2)
    return out


def _sines(sr, T, parts, rng, vib=(5.5, 0.004)):
    n = int(sr * T)
    x = [0.0] * n
    for f0, a in parts:
        p = rng.random() * TAU
        for i in range(n):
            t = i / sr
            p += TAU * f0 * (1 + vib[1] * math.sin(TAU * vib[0] * t + f0)) / sr
            x[i] += math.sin(p) * a
    return x


def banish_blast(sr, rng):
    """~1.5 s: a sharp intake, the shriek of high beating sines climbing, a white flash and a whoosh pulling away."""
    T = 1.5
    x = buf(sr, T + 0.3)
    shriek = _sines(sr, 1.0, ((1760, 0.8), (1771, 0.7), (2640, 0.5), (2655, 0.4), (3520, 0.25)), rng, (9.0, 0.01))
    shriek = [s * (1 + 0.25 * i / len(shriek)) for i, s in enumerate(shriek)]
    add(x, env_apply(shriek, sr, lambda t: min(1.0, t / 0.03) * max(0.0, 1 - t / 1.0) ** 0.6), 0.0, 0.35, sr)
    c = choir(sr, rng, (81, 86, 88, 93), 0.9, "ah", voices=3, detune=18, vib=(7.0, 18.0))
    add(x, env_apply(c, sr, lambda t: min(1.0, t / 0.05) * max(0.0, 1 - t / 0.9)), 0.0, 0.6, sr)
    add(x, thud(sr, 140, 40, 0.18, 0.8), 0.0, 0.8, sr)
    add(x, burst(sr, rng, 0.6, 3000, 0.6, "high", 0.12, attack=0.003), 0.0, 0.6, sr)
    add(x, whoosh(sr, rng, 1.2, 6000, 400, 0.9, 0.5), 0.15, 0.9, sr)
    add(x, sparkle(sr, rng, 24, 0.0, 0.7, T, 3500, 9000, rise=False), 0, 0.15, sr)
    return hall(x, sr, 0.3, 0.8)


def ward_hum(sr, rng):
    """A seamless 2 s loop: low sines (a root, its fifth and octave) beating slowly, a soft breath of choir 'oo'."""
    T = LOOP_T + XFADE
    x = _sines(sr, T, ((73.4, 0.5), (74.1, 0.4), (110.0, 0.3), (146.8, 0.22), (220.0, 0.08)), rng, (0.5, 0.002))
    c = choir(sr, rng, (50, 57, 62), T, "oo", voices=2, detune=6, vib=(4.0, 5.0))
    add(x, c, 0, 0.25, sr)
    sway = smooth_random(sr, T, rng, 1.0, 0.85, 1.0)
    x = [s * sway(i / sr) for i, s in enumerate(x)]
    return _loop(x, sr)


def alarm_chime(sr, rng):
    """~1 s: a struck bell, a tritone overtone ringing against it, beating, then a hiss of air like a held breath."""
    T = 1.2
    x = buf(sr, T)
    add(x, bell_voice(sr, 880.0, 1.1, bright=0.9, beat=2.8), 0, 0.6, sr)
    add(x, bell_voice(sr, 1244.5, 0.9, bright=0.6, beat=3.6), 0.005, 0.32, sr)
    add(x, ring(sr, 2489, 0.8, 0.5), 0.0, 0.08, sr)
    air = svf(noise(int(sr * 0.8), rng), sr, 4200, 0.8, "band")
    add(x, env_apply(air, sr, lambda t: 0.12 * math.sin(math.pi * min(1.0, t / 0.8))), 0.15, 1.0, sr)
    return hall(x, sr, 0.3, 0.6)


def tablet_whisper(sr, rng):
    """~3 s: three or four whispering voices overlapping, panned by time, over a far choir drone that swells and goes."""
    T = 3.0
    x = buf(sr, T + 0.4)
    for k in range(4):
        start = 0.05 + k * 0.45 + rng.uniform(0, 0.2)
        w = whisper(sr, rng, T - start, syllables=9, gain=1.0)
        add(x, w, start, 0.8 - k * 0.12, sr)
    c = choir(sr, rng, (52, 59, 64, 67), T, "oo", voices=2, detune=12, vib=(3.5, 8.0))
    add(x, env_apply(c, sr, lambda t: 0.18 * math.sin(math.pi * min(1.0, t / T))), 0, 1.0, sr)
    low = tone(sr, 55, T, attack=0.6)
    add(x, env_apply(low, sr, lambda t: 0.1 * math.sin(math.pi * min(1.0, t / T))), 0, 1.0, sr)
    return hall(x, sr, 0.35, 0.8)


def glyph_wrong(sr, rng):
    """~0.7 s: a dull knock, then a piercing tone (two close high sines beating fast) that bores in and dies away."""
    T = 0.75
    x = buf(sr, T)
    add(x, thud(sr, 90, 50, 0.06, 0.25), 0, 0.6, sr)
    add(x, burst(sr, rng, 0.08, 700, 1.2, "band", 0.02), 0, 0.4, sr)
    pierce = _sines(sr, 0.7, ((3150, 0.6), (3187, 0.6), (6300, 0.12)), rng, (0.0, 0.0))
    add(x, env_apply(pierce, sr, lambda t: min(1.0, t / 0.01) * math.exp(-t / 0.28)), 0.01, 0.45, sr)
    return x


def oil_ignite(sr, rng):
    """~1 s: a soft low whoomph as the vapour catches, the roar of flame running along the line, crackle after it."""
    T = 1.1
    x = buf(sr, T + 0.2)
    add(x, thud(sr, 85, 35, 0.12, 0.5), 0, 0.8, sr)
    add(x, whoosh(sr, rng, 0.9, 250, 1400, 0.9, 0.6), 0.0, 0.9, sr)
    roar = svf(noise(int(sr * T), rng), sr, 700, 0.7, "band")
    add(x, env_apply(roar, sr, lambda t: min(1.0, t / 0.08) * math.exp(-t / 0.5) * 0.5), 0.02, 1.0, sr)
    add(x, crackle(sr, rng, 1.0, 0.09, 3200, 0.9), 0.1, 0.35, sr)
    return hall(x, sr, 0.12, 0.4)


# id -> (builder, peak dBFS)
SOUNDS = {
    "banish_blast": (banish_blast, -1.0), "ward_hum": (ward_hum, -6.0), "alarm_chime": (alarm_chime, -2.0),
    "tablet_whisper": (tablet_whisper, -3.0), "glyph_wrong": (glyph_wrong, -2.5), "oil_ignite": (oil_ignite, -1.5),
}
LOOPS = {"ward_hum"}
