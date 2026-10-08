"""v0.13 Allegiance sounds: ascensions (Heaven's choir, Hell's furnace, the hunter's grim resolve), an angel's blink, a demon's
smoke, the smiting touch, angel radio, the throne, true form, an exorcised player expelled, holy oil catching, a healing
touch, a lance of light and telekinesis. Same conventions as sfx.py / michael_sfx.py: (sr, rng) -> float list, finished
and encoded by generate.py into sounds/allegiance/<id>.ogg (AllegianceAssetData points the events at them).
"""

import math

from michael_sfx import hall, ring, whoosh
from sfx import burst, choir, sparkle, thud, voiced, UH, HUM
from synth import (add, buf, env_apply, midi_hz, noise, onepole_lp, saw, smooth_random, soft_clip, svf, tone, reverb, partials)


def _swell(T, a=0.3, r=0.6):
    return lambda t: min(1.0, t / a) * max(0.0, min(1.0, (T - t) / r))


def ascend_angel(sr, rng):
    """Raised to a new rank of Heaven: a choir rising to a bright major chord, bells and a shower of sparkle."""
    T = 4.2
    x = buf(sr, T)
    low = choir(sr, rng, (62, 69), 1.6, "oo", voices=3, detune=8)
    add(x, env_apply(low, sr, _swell(1.6, 0.4, 0.5)), 0, 1.0, sr)
    hi = choir(sr, rng, (74, 78, 81, 86), 2.8, "ah", voices=3, detune=10)
    hi = svf(hi, sr, lambda t: 900 * (6 ** min(1.0, t / 1.2)), 0.7, "low")
    add(x, env_apply(hi, sr, _swell(2.8, 0.6, 1.2)), 1.2, 1.3, sr)
    for i, m in enumerate((86, 90, 93)):
        add(x, partials(sr, 2.0, [(midi_hz(m), 0.4, 1.6), (midi_hz(m) * 2.76, 0.15, 0.6)]), 1.25 + 0.12 * i, 0.35, sr)
    add(x, sparkle(sr, rng, 24, 1.2, 3.8, T, 3000, 9000), 0, 0.7, sr)
    return hall(x, sr, 0.35, 1.6)


def ascend_demon(sr, rng):
    """Raised in Hell: a furnace roar, a low growled chord of voices, chains and a bell struck backwards."""
    T = 4.0
    x = buf(sr, T)
    roar = svf(noise(int(sr * 3.2), rng), sr, lambda t: 120 + 500 * min(1.0, t / 1.5), 0.9, "low")
    add(x, env_apply(roar, sr, _swell(3.2, 0.8, 1.0)), 0, 1.4, sr)
    growl = choir(sr, rng, (31, 38, 43), 3.0, "oo", voices=3, detune=22)
    add(x, env_apply(soft_clip(growl, 2.2), sr, _swell(3.0, 0.5, 1.0)), 0.5, 0.9, sr)
    for i in range(7):
        add(x, burst(sr, rng, 0.05, 3200 + 400 * rng.random(), 3.0, "band", 0.03), 1.0 + i * 0.14 + 0.05 * rng.random(), 0.35, sr)
    add(x, thud(sr, 70, 28, 0.5, 1.5), 1.1, 1.1, sr)
    return hall(soft_clip(x, 1.3), sr, 0.25, 1.4)


def ascend_hunter(sr, rng):
    """A hunter's rank earned: a shotgun racked, a match struck, a low resolute hum and a single toll."""
    T = 3.2
    x = buf(sr, T)
    for at in (0.0, 0.16):
        add(x, burst(sr, rng, 0.04, 2400, 1.5, "band", 0.012), at, 0.9, sr)
        add(x, thud(sr, 180, 90, 0.03, 0.12), at, 0.5, sr)
    strike = svf(noise(int(sr * 0.5), rng), sr, 3500, 0.8, "high")
    add(x, env_apply(strike, sr, lambda t: math.exp(-t / 0.12)), 0.6, 0.5, sr)
    hum = voiced(sr, rng, 2.0, lambda t: 98, HUM, 0.15, _swell(2.0, 0.5, 0.8))
    add(x, hum, 0.8, 0.6, sr)
    add(x, partials(sr, 2.2, [(midi_hz(45), 0.6, 1.8), (midi_hz(45) * 2.4, 0.25, 0.9), (midi_hz(45) * 3.9, 0.1, 0.5)]), 1.0, 0.6, sr)
    return hall(x, sr, 0.25, 1.2)


def teleport(sr, rng):
    """An angel's blink: a flutter of unseen wings and a soft air pop."""
    T = 0.7
    x = buf(sr, T)
    for i in range(4):
        add(x, whoosh(sr, rng, 0.14, 300, 1400, 1.0, 0.4), i * 0.06, 0.5 - 0.08 * i, sr)
    add(x, burst(sr, rng, 0.04, 900, 0.8, "band", 0.015), 0.3, 0.6, sr)
    return reverb(x, sr, size=0.5, damp=0.4, wet=0.15, tail=0.3)


def smoke(sr, rng):
    """Black smoke pouring out: a rushing hiss with a hollow howl inside it."""
    T = 1.5
    x = buf(sr, T)
    rush = svf(noise(int(sr * T), rng), sr, lambda t: 600 + 1800 * math.sin(math.pi * t / T), 1.2, "band")
    add(x, env_apply(rush, sr, _swell(T, 0.15, 0.7)), 0, 0.9, sr)
    howl = tone(sr, 160, 1.1, glide=95)
    add(x, env_apply(howl, sr, _swell(1.1, 0.2, 0.6)), 0.1, 0.25, sr)
    return reverb(x, sr, size=0.7, damp=0.5, wet=0.2, tail=0.5)


def smite(sr, rng):
    """A palm to the face: a searing rising tone, light flaring out through the eyes, a thump."""
    T = 1.5
    x = buf(sr, T)
    sear = tone(sr, 600, 1.0, glide=2200, attack=0.01)
    add(x, env_apply(sear, sr, _swell(1.0, 0.15, 0.6)), 0, 0.3, sr)
    add(x, choir(sr, rng, (79, 86), 0.9, "ah", voices=2, detune=16), 0.05, 0.45, sr)
    cr = svf(noise(int(sr * 1.2), rng), sr, 3200, 0.7, "high")
    add(x, env_apply(cr, sr, lambda t: math.exp(-t / 0.5) * (0.4 + 0.6 * (rng.random() < 0.1))), 0.05, 0.5, sr)
    add(x, thud(sr, 95, 40, 0.18, 0.7), 0, 1.0, sr)
    return hall(soft_clip(x, 1.4), sr, 0.22, 0.9)


def radio(sr, rng):
    """Angel radio: a tuning dial sweeping through static, a burst of overlapping whispers, a high tone."""
    T = 2.0
    x = buf(sr, T)
    st = svf(noise(int(sr * T), rng), sr, lambda t: 1200 + 2500 * (0.5 + 0.5 * math.sin(t * 7)), 2.0, "band")
    add(x, env_apply(st, sr, _swell(T, 0.1, 0.5)), 0, 0.5, sr)
    for i in range(3):
        w = voiced(sr, rng, 0.6, lambda t, i=i: 180 + 30 * i + 20 * math.sin(t * 9), UH, 0.9, _swell(0.6, 0.1, 0.3))
        add(x, svf(w, sr, 1800, 1.0, "band"), 0.4 + i * 0.25, 0.35, sr)
    add(x, env_apply(tone(sr, 3520, 1.4), sr, _swell(1.4, 0.3, 0.6)), 0.4, 0.08, sr)
    return reverb(x, sr, size=0.6, damp=0.3, wet=0.25, tail=0.6)


def throne(sr, rng):
    """The King's throne: a deep chord of the damned and a wave of force that drives all to their knees."""
    T = 3.0
    x = buf(sr, T)
    add(x, thud(sr, 55, 25, 0.8, 2.2), 0, 1.4, sr)
    c = choir(sr, rng, (26, 33, 38), 2.6, "oo", voices=3, detune=25)
    add(x, env_apply(soft_clip(c, 2.5), sr, _swell(2.6, 0.1, 1.2)), 0, 0.9, sr)
    wave = svf(noise(int(sr * 1.6), rng), sr, lambda t: 300 * (0.3 ** (t / 1.6)), 0.8, "low")
    add(x, env_apply(wave, sr, lambda t: math.exp(-t / 0.6)), 0.05, 1.2, sr)
    return hall(soft_clip(x, 1.2), sr, 0.3, 1.4)


def true_form(sr, rng):
    """True form: a blinding chord of light swelling to a roar, with a high ringing that hurts to hear."""
    T = 3.6
    x = buf(sr, T)
    c = choir(sr, rng, (62, 69, 74, 81, 86), 3.2, "ah", voices=3, detune=14)
    c = svf(c, sr, lambda t: 700 * (8 ** min(1.0, t / 1.0)), 0.7, "low")
    add(x, env_apply(c, sr, _swell(3.2, 0.5, 1.2)), 0, 1.3, sr)
    add(x, env_apply(tone(sr, 4186, 3.0), sr, _swell(3.0, 0.8, 1.0)), 0.3, 0.12, sr)
    roar = svf(noise(int(sr * 2.6), rng), sr, 1800, 0.6, "band")
    add(x, env_apply(roar, sr, _swell(2.6, 0.6, 1.0)), 0.3, 0.6, sr)
    add(x, sparkle(sr, rng, 30, 0.3, 3.2, T, 3500, 10000), 0, 0.6, sr)
    return hall(soft_clip(x, 1.3), sr, 0.35, 1.5)


def expel(sr, rng):
    """Exorcised: a ripping scream of smoke torn out of the body, a fading howl, then silence."""
    T = 2.0
    x = buf(sr, T)
    rip = svf(noise(int(sr * 1.2), rng), sr, lambda t: 400 + 3000 * min(1.0, t / 0.4), 1.0, "band")
    add(x, env_apply(rip, sr, _swell(1.2, 0.05, 0.8)), 0, 1.0, sr)
    scream = voiced(sr, rng, 1.2, lambda t: 420 * (0.5 ** (t / 1.2)) + 30 * math.sin(t * 40), UH, 0.6, _swell(1.2, 0.05, 0.7))
    add(x, soft_clip(scream, 2.0), 0.05, 0.6, sr)
    add(x, thud(sr, 80, 35, 0.25, 0.8), 0, 0.8, sr)
    return reverb(x, sr, size=0.8, damp=0.4, wet=0.3, tail=0.8)


def holy_oil(sr, rng):
    """Holy oil catching: a soft whump, then a ring of fire crackling bright with a faint chime."""
    T = 2.2
    x = buf(sr, T)
    add(x, onepole_lp(burst(sr, rng, 0.5, 300, 0.7, "low", 0.2), sr, 600), 0, 1.2, sr)
    fire = svf(noise(int(sr * 1.9), rng), sr, 2400, 0.6, "high")
    add(x, env_apply(fire, sr, lambda t: min(1.0, t / 0.15) * math.exp(-t / 1.4) * (0.5 + 0.5 * (rng.random() < 0.12))), 0.1, 0.6, sr)
    add(x, partials(sr, 1.5, [(midi_hz(88), 0.3, 1.2), (midi_hz(95), 0.15, 0.8)]), 0.15, 0.25, sr)
    return reverb(x, sr, size=0.5, damp=0.4, wet=0.18, tail=0.6)


def heal(sr, rng):
    """Two fingers to the brow: a warm rising shimmer and a gentle chord."""
    T = 1.8
    x = buf(sr, T)
    c = choir(sr, rng, (67, 71, 74), 1.5, "oo", voices=2, detune=6)
    add(x, env_apply(c, sr, _swell(1.5, 0.3, 0.8)), 0, 0.9, sr)
    add(x, sparkle(sr, rng, 14, 0.1, 1.4, T, 2500, 7000), 0, 0.6, sr)
    return hall(x, sr, 0.3, 0.9)


def lance(sr, rng):
    """A lance of light thrown: a bright charge, a ringing release and the hiss of its flight."""
    T = 1.4
    x = buf(sr, T)
    add(x, env_apply(tone(sr, 880, 0.3, glide=1760), sr, _swell(0.3, 0.2, 0.08)), 0, 0.3, sr)
    add(x, ring(sr, 1320, 1.0), 0.28, 0.4, sr)
    add(x, whoosh(sr, rng, 0.8, 2400, 600, 1.0, 0.2), 0.3, 0.8, sr)
    return hall(x, sr, 0.2, 0.7)


def telekinesis(sr, rng):
    """Telekinesis: a low warping drone that bends in pitch, a grinding strain, a release."""
    T = 1.6
    x = buf(sr, T)
    wob = smooth_random(sr, T, rng, 6.0, -1.0, 1.0)
    d = saw(sr, T, 70, 8, 0.0, (3.0, 40.0))
    d = [v * (0.6 + 0.4 * wob(i / sr)) for i, v in enumerate(d)]
    d = svf(d, sr, lambda t: 300 + 500 * math.sin(math.pi * t / T), 1.5, "low")
    add(x, env_apply(d, sr, _swell(T, 0.2, 0.5)), 0, 0.8, sr)
    grind = svf(noise(int(sr * 1.0), rng), sr, 900, 3.0, "band")
    add(x, env_apply(grind, sr, _swell(1.0, 0.3, 0.3)), 0.3, 0.3, sr)
    return reverb(x, sr, size=0.6, damp=0.5, wet=0.2, tail=0.5)


# id -> (builder, peak dBFS)
SOUNDS = {
    "ascend_angel": (ascend_angel, -1.0), "ascend_demon": (ascend_demon, -1.0), "ascend_hunter": (ascend_hunter, -1.5),
    "teleport": (teleport, -2.0), "smoke": (smoke, -2.0), "smite": (smite, -1.0), "radio": (radio, -3.0),
    "throne": (throne, -1.0), "true_form": (true_form, -1.0), "expel": (expel, -1.0), "holy_oil": (holy_oil, -2.0),
    "heal": (heal, -2.5), "lance": (lance, -1.5), "telekinesis": (telekinesis, -2.0),
}
