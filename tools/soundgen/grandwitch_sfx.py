"""v0.21 the Grand Witch: every grand_witch.<id> in AllSounds.COVEN_SOUNDS (GrandWitchAssets.SOUNDS), written by generate.py to
sounds/grand_witch/<id>.ogg; CovenSoundData points grand_witch.<id> at them once the files exist (rerun runData).

The Coven's matriarch: old, deep and regal -- lower, slower and more resonant than Rowena, every word with a long hall behind
it. Her coven hums under her when she is shielded; her staff's crystal rings in silver (high, pure, slow-beating bells). Her
spells: frost that crackles out across the floor, a ring of fire roaring up round her, a telekinetic press that groans and
crunches, the mass taglock as many whispering voices rising to a hiss, her smoke veil a thick rolling whoosh with a low chuckle.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from coven_sfx import glass_shatter, glide, rasp, smoke_puff, syllable, _env
from magic_sfx import AH, OH, chimes, fire_roar, whisper
from michael_sfx import whoosh
from raphael_sfx import crackle
from sfx import HUM, bell_voice, burst, choir, sparkle, thud, voiced
from synth import TAU, add, buf, env_apply, midi_hz, noise, reverb, smooth_random, svf, tone


def hall(x, sr, wet=0.3, tail=1.0):
    """Her voice and spells always carry a long, dark hall."""
    return reverb(x, sr, size=0.88, damp=0.45, wet=wet, tail=tail)


def silver(sr, rng, notes, start, step, gain, seconds=2.0):
    """The staff's crystal: pure silver bells, slow beating, one after another."""
    x = buf(sr, start + step * len(notes) + seconds)
    for i, m in enumerate(notes):
        add(x, bell_voice(sr, midi_hz(m), seconds, bright=0.45, beat=0.9), start + i * step, gain, sr)
    return x


def old_voice(sr, rng, d, f0, vowel, breath=0.7, env=None, tremble=0.025):
    """An old, deep voice: a slow tremble on the pitch, a little rasp."""
    wob = smooth_random(sr, d, rng, 6, 1 - tremble, 1 + tremble)
    curve = f0 if callable(f0) else (lambda t, f=f0: f)
    v = voiced(sr, rng, d, lambda t: curve(t) * wob(t), vowel, breath, env or _env(0.08, d * 0.5, d * 0.42))
    return rasp(v, 1.25)


def low_laugh(sr, rng, f0=165, count=5, step=0.2):
    """A slow, deep "ha... ha... ha": each beat a little lower, breath before each."""
    x = buf(sr, count * step + 0.6)
    f = f0
    for i in range(count):
        t = i * step + rng.uniform(-0.012, 0.012)
        h = svf(noise(int(sr * 0.07), rng), sr, 1100, 0.7, "band")
        add(x, env_apply(h, sr, lambda u: min(1.0, u / 0.015) * math.exp(-u / 0.03)), t, 0.22, sr)
        d = step * 0.95
        add(x, syllable(sr, rng, d, glide(f * 1.06, f * 0.88, d), AH, 0.75, 0.012, d * 0.4, 0.015), t + 0.03, 1.0 - i * 0.1, sr)
        f *= 0.95
    return rasp(x, 1.2)


# --- her voice --------------------------------------------------------------------------------------------------------------

def ambient(sr, rng):
    T = 2.2
    x = buf(sr, T)
    # A low, measured "hmm..." that falls, a muttered word after it, one silver note from the staff.
    add(x, old_voice(sr, rng, 1.1, glide(150, 118, 1.1), HUM, 0.4), 0, 0.9, sr)
    add(x, whisper(sr, rng, 0.8, 3, 0.7), 1.0, 0.55, sr)
    add(x, bell_voice(sr, midi_hz(86), 1.4, bright=0.4, beat=0.8), 0.6, 0.1, sr)
    return hall(x, sr, 0.25, 0.8)


def laugh(sr, rng):
    x = buf(sr, 2.0)
    add(x, low_laugh(sr, rng, 170, 5, 0.21), 0, 1.0, sr)
    hum = voiced(sr, rng, 1.6, lambda t: 82, HUM, 0.3, _env(0.3, 0.6, 0.7))
    add(x, hum, 0.05, 0.2, sr)
    return hall(x, sr, 0.28, 0.9)


def hurt(sr, rng):
    T = 0.6
    x = buf(sr, T)
    add(x, old_voice(sr, rng, 0.42, glide(240, 165, 0.42), AH, 0.8, _env(0.008, 0.12, 0.29), 0.03), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.12, 1200, 0.7, "band", 0.03), 0, 0.2, sr)
    add(x, bell_voice(sr, midi_hz(84), 0.6, bright=0.5, beat=1.2), 0.02, 0.12, sr)
    return hall(x, sr, 0.15, 0.35)


def death(sr, rng):
    T = 3.2
    x = buf(sr, T)
    # A long cry falling into a groan, the coven's hum dying under it, her crystal cracking, a heavy fall.
    cry = old_voice(sr, rng, 1.9, glide(300, 85, 1.9, 0.8), OH, 0.7, _env(0.06, 0.7, 1.14), 0.04)
    add(x, cry, 0, 0.9, sr)
    c = choir(sr, rng, (45, 52, 57), 2.4, "oo", voices=2, detune=12, vib=(4.5, 10.0))
    c = env_apply(c, sr, lambda t: min(1.0, t / 0.3) * max(0.0, 1 - t / 2.4) ** 2)
    add(x, c, 0.1, 0.35, sr)
    for i, m in enumerate((86, 81, 77, 72)):
        add(x, bell_voice(sr, midi_hz(m), 1.6, bright=0.4, beat=2.4), 0.6 + i * 0.25, 0.18, sr)
    add(x, glass_shatter(sr, rng, 1.2, 8), 1.4, 0.35, sr)
    add(x, thud(sr, 75, 38, 0.22, 0.7), 1.9, 0.6, sr)
    return hall(x, sr, 0.32, 1.3)


# --- her coven's shield --------------------------------------------------------------------------------------------------------

def shield(sr, rng):
    T = 2.4
    x = buf(sr, T)
    # The coven's voices humming up under her, a dome of glass rising, the crystal ringing a slow silver chord.
    c = choir(sr, rng, (48, 55, 60, 63), 2.2, "oo", voices=3, detune=10, vib=(4.8, 9.0))
    add(x, env_apply(c, sr, _env(0.7, 0.8, 0.7)), 0, 0.5, sr)
    add(x, tone(sr, 330, 1.4, decay=0.9, glide=495, attack=0.25), 0.1, 0.18, sr)
    add(x, tone(sr, 660, 1.4, decay=0.7, glide=990, attack=0.3), 0.1, 0.09, sr)
    add(x, silver(sr, rng, (79, 84, 88, 91), 0.35, 0.22, 0.2, 1.6), 0, 1.0, sr)
    add(x, sparkle(sr, rng, 10, 0.3, 1.6, T, 3000, 8000, rise=True), 0, 0.35, sr)
    add(x, whoosh(sr, rng, 1.2, 250, 2000, 1.0, 0.75), 0, 0.25, sr)
    return hall(x, sr, 0.3, 1.0)


def shield_break(sr, rng):
    T = 2.0
    x = buf(sr, T)
    # The dome shatters: a low thump, a great crash of glass and crystal, shards ringing down, the hum cut off.
    add(x, thud(sr, 80, 36, 0.25, 0.8), 0, 0.9, sr)
    add(x, glass_shatter(sr, rng, 1.6, 26), 0.01, 1.0, sr)
    add(x, chimes(sr, rng, T, 10, 84, 108, 0.25, 1.5, 0.35), 0, 0.45, sr)
    add(x, tone(sr, 990, 0.9, decay=0.35, glide=330, attack=0.002), 0, 0.14, sr)
    hum = choir(sr, rng, (48, 55), 0.35, "oo", voices=2, detune=10)
    add(x, env_apply(hum, sr, lambda t: max(0.0, 1 - t / 0.3)), 0, 0.3, sr)
    return hall(x, sr, 0.28, 0.9)


# --- her hexes -------------------------------------------------------------------------------------------------------------------

def frost_hex(sr, rng):
    T = 1.9
    x = buf(sr, T)
    # The staff swept low: a cold gust, ice crackling outward across the floor, glassy chimes as it sets.
    add(x, whoosh(sr, rng, 0.6, 2600, 600, 1.0, 0.3), 0, 0.5, sr)
    add(x, crackle(sr, rng, 1.3, 0.3, 6000, 1.1, 0.5), 0.12, 0.75, sr)
    add(x, crackle(sr, rng, 1.0, 0.15, 3200, 0.9, 0.35), 0.25, 0.4, sr)
    add(x, chimes(sr, rng, T, 9, 86, 110, 0.35, 1.2, 0.35), 0, 0.85, sr)
    add(x, burst(sr, rng, 0.3, 2000, 0.8, "band", 0.08), 0.1, 0.35, sr)
    add(x, thud(sr, 110, 60, 0.1, 0.3), 0.12, 0.35, sr)
    return hall(x, sr, 0.25, 0.8)


def inferno(sr, rng):
    T = 2.1
    x = buf(sr, T)
    # The staff raised: a ring of fire roaring up all round her, a deep boom under it, embers spitting.
    add(x, bell_voice(sr, midi_hz(79), 1.2, bright=0.5, beat=1.0), 0, 0.25, sr)
    add(x, fire_roar(sr, rng, 1.7, 140, 900, 0.3), 0.06, 1.0, sr)
    add(x, fire_roar(sr, rng, 1.4, 300, 1600, 0.25), 0.15, 0.55, sr)
    add(x, thud(sr, 65, 32, 0.35, 1.0), 0.08, 0.8, sr)
    add(x, crackle(sr, rng, 1.6, 0.12, 2800, 0.9, 0.6), 0.2, 0.45, sr)
    return hall(x, sr, 0.22, 0.8)


def crush(sr, rng):
    T = 1.5
    x = buf(sr, T)
    # A clenching fist: air pressed down into a groaning low drone, then a crunch of stone and bone.
    drone = []
    for f in (55, 58.3, 82.4):
        d = tone(sr, f, 0.9, attack=0.35)
        drone = d if not drone else [a + b for a, b in zip(drone, d)]
    drone = env_apply(rasp(drone, 2.0), sr, _env(0.5, 0.25, 0.15))
    add(x, drone, 0, 0.6, sr)
    press = svf(noise(int(sr * 0.8), rng), sr, lambda t: 900 - 700 * min(1.0, t / 0.7), 0.9, "band")
    add(x, env_apply(press, sr, _env(0.6, 0.1, 0.1)), 0, 0.4, sr)
    add(x, thud(sr, 90, 35, 0.18, 0.6), 0.78, 1.0, sr)
    for i in range(7):
        add(x, burst(sr, rng, 0.05, rng.uniform(1500, 3800), 1.4, "band", 0.012), 0.78 + i * 0.022 + rng.uniform(0, 0.01), 0.6, sr)
    add(x, burst(sr, rng, 0.35, 400, 0.6, "low", 0.1), 0.8, 0.5, sr)
    return hall(x, sr, 0.18, 0.6)


def taglock(sr, rng):
    T = 2.4
    x = buf(sr, T)
    # Arms spread: many voices whispering at once, rising, swelling into a hiss as every lock is taken.
    for i in range(5):
        add(x, whisper(sr, rng, 1.7, 7, 0.7), rng.uniform(0, 0.25), 0.4 + 0.08 * i, sr)
    swell = svf(noise(int(sr * 2.0), rng), sr, lambda t: 1500 + 2500 * min(1.0, t / 1.8), 0.8, "band")
    add(x, env_apply(swell, sr, lambda t: (t / 1.9) ** 2 if t < 1.9 else max(0.0, 1 - (t - 1.9) / 0.1)), 0, 0.6, sr)
    hiss = svf(noise(int(sr * 0.5), rng), sr, 5200, 0.7, "high")
    add(x, env_apply(hiss, sr, lambda t: min(1.0, t / 0.02) * math.exp(-t / 0.15)), 1.9, 0.8, sr)
    for f in (98, 103.8):
        add(x, env_apply(tone(sr, f, 1.9, attack=0.8), sr, _env(1.2, 0.5, 0.2)), 0, 0.2, sr)
    return hall(x, sr, 0.3, 0.9)


def smoke_veil(sr, rng):
    T = 2.0
    x = buf(sr, T)
    # A thick roll of smoke (slower and heavier than Rowena's puff), a low chuckle in it.
    add(x, smoke_puff(sr, rng, 1.4, 0.0), 0, 1.0, sr)
    roll = svf(noise(int(sr * 1.3), rng), sr, lambda t: 500 - 250 * t, 0.6, "low")
    add(x, env_apply(roll, sr, lambda t: min(1.0, t / 0.1) * math.exp(-t / 0.45)), 0.35, 0.5, sr)
    add(x, low_laugh(sr, rng, 150, 3, 0.18), 0.7, 0.45, sr)
    return hall(x, sr, 0.25, 0.8)


# id (the file's base name) -> (builder, peak dBFS)
SOUNDS = {
    "ambient": (ambient, -3.5), "laugh": (laugh, -2.5), "hurt": (hurt, -2.0), "death": (death, -1.5),
    "shield": (shield, -2.0), "shield_break": (shield_break, -1.5), "frost_hex": (frost_hex, -1.5), "inferno": (inferno, -1.5),
    "crush": (crush, -1.5), "taglock": (taglock, -2.0), "smoke_veil": (smoke_veil, -2.0),
}
