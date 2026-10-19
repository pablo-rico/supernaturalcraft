"""v0.21 the witches and Rowena: every sound in AllSounds.COVEN_SOUNDS (WitchAssets.SOUNDS, RowenaAssets.SOUNDS), written by
generate.py to sounds/witch/<id>.ogg and sounds/rowena/<id>.ogg; CovenSoundData points witch.<id> / rowena.<id> at them once the
files exist (rerun runData).

Each speaker has her own timbre:
  Hedge Witch    -- an old country voice: dry, breathy, a cackle kept short and low in the mix, muttered words;
  Coven Witch    -- darker and colder: whispers over a low hum, a glass chime under her words;
  their craft    -- breath, glass and wind chimes (the Witchcraft school's palette), smoke puffs, a shield of glass;
  familiars      -- a black cat's meow and purr, a raven's harsh caw;
  hexed victims  -- a hollow groan, then a breath of relief and golden chimes when freed;
  Rowena         -- elegant and amused: a hum, a light laugh, a "mm?" for talk; her spells are bright bell chimes over the
                    element (fire's roar, ice's crackle, a choir for the summoning).

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from magic_sfx import AH, EE, OH, chimes, fire_roar, whisper
from michael_sfx import whoosh
from raphael_sfx import crackle
from sfx import UH, HUM, bell_voice, burst, choir, sparkle, thud, voiced
from synth import (TAU, add, buf, env_apply, formants, midi_hz, noise, onepole_lp, partials, reverb, reverse, smooth_random,
                   soft_clip, svf, tone)

EH = [(530, 4.0, 1.0), (1840, 6.0, 0.45), (2480, 7.0, 0.25)]
AW = [(640, 4.0, 1.0), (1000, 5.0, 0.6), (2600, 7.0, 0.2)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def room(x, sr, wet=0.12, tail=0.35):
    return reverb(x, sr, size=0.45, damp=0.5, wet=wet, tail=tail)


def glide(f0, f1, T, shape=1.0):
    """f0 -> f1 over T (exponential), held after."""
    return lambda t: f0 * (f1 / f0) ** (min(1.0, t / T) ** shape)


def rasp(x, amount=1.6):
    """A rough, worn voice: soft-clipped."""
    return soft_clip([s * amount for s in x], amount)


def syllable(sr, rng, d, f0, vowel, breath=0.6, attack=0.012, tau=None, jitter=0.0):
    """One short voiced syllable: a pitch curve (Hz or function), a vowel, its own envelope."""
    curve = f0 if callable(f0) else (lambda t, f=f0: f)
    if jitter:
        wob = smooth_random(sr, d, rng, 18, 1 - jitter, 1 + jitter)
        base = curve
        curve = lambda t: base(t) * wob(t)  # noqa: E731
    tau = tau or d * 0.5
    return voiced(sr, rng, d, curve, vowel, breath, lambda t: min(1.0, t / attack) * math.exp(-max(0.0, t - attack) / tau))


def cackle(sr, rng, f0, count, step=0.13, fall=0.94, vowel=EH, breath=0.9, jitter=0.02):
    """A witch's cackle: quick breathy "heh"s, each a little lower, an aspirated 'h' before each."""
    T = count * step + 0.35
    x = buf(sr, T)
    f = f0
    for i in range(count):
        t = i * step + rng.uniform(-0.01, 0.01)
        h = svf(noise(int(sr * 0.05), rng), sr, 1500, 0.7, "band")
        h = env_apply(h, sr, lambda u: min(1.0, u / 0.01) * math.exp(-u / 0.02))
        add(x, h, t, 0.25, sr)
        d = step * 0.9
        add(x, syllable(sr, rng, d, glide(f * 1.08, f * 0.9, d), vowel, breath, 0.008, d * 0.35, jitter), t + 0.02,
            1.0 - i * 0.08, sr)
        f *= fall
    return x


def meow(sr, rng, f_lo=480, f_hi=820, T=0.62):
    """A cat: "mi-a-ow", the pitch rising then falling, the vowel opening from ee to ah and closing to oo."""
    def f0(t):
        u = t / T
        return f_lo + (f_hi - f_lo) * math.sin(math.pi * min(1.0, u * 1.1)) ** 0.8

    v = voiced(sr, rng, T, f0, EE, 0.35, _env(0.04, T * 0.5, T * 0.46))
    w = voiced(sr, rng, T, f0, AH, 0.35, _env(0.04, T * 0.5, T * 0.46))
    o = voiced(sr, rng, T, f0, OH, 0.35, _env(0.04, T * 0.5, T * 0.46))
    out = []
    for i in range(len(v)):
        u = i / len(v)
        a = max(0.0, 1 - u * 3)
        c = max(0.0, (u - 0.55) / 0.45)
        b = max(0.0, 1 - a - c)
        out.append(v[i] * a + w[i] * b + o[i] * c)
    return onepole_lp(out, sr, 5200)


def purr(sr, rng, T, rate=26.0):
    """A purr: low rumbling noise pulsed at ~26 Hz, in and out with the breath."""
    n = int(sr * T)
    x = onepole_lp(onepole_lp(noise(n, rng), sr, 420), sr, 300)
    breath = lambda t: 0.6 + 0.4 * math.sin(TAU * t / 1.1)  # noqa: E731
    return [s * (0.5 + 0.5 * math.sin(TAU * rate * i / sr)) ** 2 * breath(i / sr) * 3.0 for i, s in enumerate(x)]


def caw(sr, rng, T=0.32, f0=620):
    """A raven's caw: a harsh, buzzing "kraa" falling at its end, rough in the throat."""
    rough = smooth_random(sr, T, rng, 70, 0.85, 1.15)
    v = voiced(sr, rng, T, lambda t: f0 * rough(t) * (1 - 0.25 * max(0.0, t / T - 0.6)), AW, 1.4, _env(0.015, T * 0.55, T * 0.43))
    v = rasp(v, 3.0)
    v = formants(v, sr, [(1300, 3.0, 1.0), (2200, 4.0, 0.5)])
    return onepole_lp(v, sr, 4200)


def glass_shatter(sr, rng, T, count=16):
    """A pane of glass breaking: a dense cluster of high chimes, a crash of noise, fragments tinkling after."""
    x = buf(sr, T)
    add(x, chimes(sr, rng, T, count, 86, 110, 0.0, 0.12, 0.3), 0, 1.3, sr)
    add(x, burst(sr, rng, 0.4, 5200, 0.6, "high", 0.08), 0, 0.8, sr)
    add(x, crackle(sr, rng, T * 0.7, 0.12, 7000, 0.9, T * 0.25), 0.03, 0.5, sr)
    add(x, chimes(sr, rng, T, 6, 90, 112, 0.2, T * 0.75, 0.25), 0, 0.5, sr)
    return x


def smoke_puff(sr, rng, T=1.1, bright=0.0):
    """A blink in smoke: a reversed breath swelling into a soft "fwump", smoke rolling away."""
    x = buf(sr, T)
    swell = reverse(reverb(burst(sr, rng, 0.25, 900, 0.7, "band", 0.08), sr, size=0.8, damp=0.4, wet=0.85, tail=0.4))
    add(x, swell, 0, 0.6, sr)
    p = len(swell) / sr
    add(x, whoosh(sr, rng, 0.7, 1800, 260, 0.8, 0.1), p - 0.04, 0.9, sr)
    add(x, thud(sr, 95, 45, 0.14, 0.5), p, 0.7, sr)
    rolling = svf(noise(int(sr * 0.8), rng), sr, lambda t: 700 - 400 * t, 0.6, "low")
    add(x, env_apply(rolling, sr, lambda t: min(1.0, t / 0.05) * math.exp(-t / 0.25)), p, 0.5, sr)
    if bright:
        add(x, chimes(sr, rng, T, 4, 84, 104, p, p + 0.2, 0.35), 0, bright, sr)
    return room(x, sr, 0.2, 0.5)


# --- the witches' voices ------------------------------------------------------------------------------------------------

def hedge_ambient(sr, rng):
    T = 1.9
    x = buf(sr, T)
    # A muttered word or two, then a short, low cackle (kept tasteful: four breathy "heh"s).
    add(x, whisper(sr, rng, 0.8, 3, 0.8), 0, 0.8, sr)
    add(x, rasp(cackle(sr, rng, 300, 4, 0.12, 0.93), 1.3), 0.7, 0.75, sr)
    return room(x, sr)


def hedge_hurt(sr, rng):
    T = 0.5
    x = buf(sr, T)
    add(x, rasp(syllable(sr, rng, 0.38, glide(330, 220, 0.38), AH, 0.9, 0.006, 0.12, 0.03), 1.5), 0, 1.0, sr)
    add(x, burst(sr, rng, 0.1, 1400, 0.7, "band", 0.03), 0, 0.2, sr)
    return room(x, sr, 0.08, 0.2)


def hedge_death(sr, rng):
    T = 1.9
    x = buf(sr, T)
    wail = voiced(sr, rng, 1.3, glide(340, 120, 1.3, 0.8), AH, 0.9, _env(0.05, 0.5, 0.75))
    add(x, rasp(wail, 1.4), 0, 0.9, sr)
    add(x, whisper(sr, rng, 0.8, 3, 0.6), 1.0, 0.5, sr)
    add(x, chimes(sr, rng, T, 3, 74, 90, 0.9, 1.3, 0.5), 0, 0.35, sr)
    return room(x, sr, 0.2, 0.6)


def coven_ambient(sr, rng):
    T = 2.4
    x = buf(sr, T)
    # A cold whispered incantation over a low hum, a single glass chime under it.
    add(x, whisper(sr, rng, 1.8, 6, 0.9), 0, 0.9, sr)
    hum = voiced(sr, rng, 2.0, lambda t: 175 * (1 + 0.01 * math.sin(TAU * 4.8 * t)), HUM, 0.3, _env(0.4, 0.9, 0.7))
    add(x, hum, 0.1, 0.45, sr)
    add(x, bell_voice(sr, midi_hz(88), 1.5, bright=0.6, beat=2.0), 0.5, 0.12, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.25, tail=0.8)


def coven_hurt(sr, rng):
    T = 0.5
    x = buf(sr, T)
    add(x, syllable(sr, rng, 0.36, glide(250, 175, 0.36), UH, 0.8, 0.008, 0.11, 0.02), 0, 1.0, sr)
    add(x, chimes(sr, rng, T, 2, 86, 98, 0.0, 0.05, 0.2), 0, 0.3, sr)
    return room(x, sr, 0.1, 0.25)


def coven_death(sr, rng):
    T = 2.4
    x = buf(sr, T)
    wail = voiced(sr, rng, 1.5, glide(290, 95, 1.5, 0.9), OH, 0.7, _env(0.06, 0.6, 0.84))
    add(x, wail, 0, 0.9, sr)
    add(x, glass_shatter(sr, rng, 1.6, 10), 0.6, 0.5, sr)
    add(x, whoosh(sr, rng, 1.2, 1600, 200, 0.9, 0.3), 0.7, 0.4, sr)
    return reverb(x, sr, size=0.8, damp=0.35, wet=0.3, tail=1.0)


# --- their craft ----------------------------------------------------------------------------------------------------------

def cast(sr, rng):
    T = 0.9
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.45, 500, 3600, 1.1, 0.7), 0, 0.7, sr)
    add(x, chimes(sr, rng, T, 4, 84, 104, 0.25, 0.4, 0.3), 0, 0.9, sr)
    add(x, burst(sr, rng, 0.06, 3000, 1.0, "band", 0.015), 0.3, 0.3, sr)
    return room(x, sr, 0.2, 0.4)


def hex_(sr, rng):
    T = 1.5
    x = buf(sr, T)
    # Whispered words, a sour drone swelling under them (a minor second), a plucked glass note as it takes.
    add(x, whisper(sr, rng, 1.0, 4, 1.0), 0, 0.9, sr)
    for f in (110, 116.5):
        d = tone(sr, f, 1.2, attack=0.5)
        add(x, env_apply(d, sr, _env(0.6, 0.3, 0.3)), 0.1, 0.25, sr)
    add(x, partials(sr, 0.8, [(midi_hz(75), 0.3, 0.5), (midi_hz(75) * 2.76, 0.12, 0.25)]), 0.9, 0.6, sr)
    return reverb(x, sr, size=0.75, damp=0.35, wet=0.28, tail=0.6)


def teleport(sr, rng):
    return smoke_puff(sr, rng, 1.1, 0.0)


def shield_up(sr, rng):
    T = 1.3
    x = buf(sr, T)
    # Glass rising: two glassy tones gliding up a fifth, chimes climbing.
    add(x, tone(sr, 440, 0.9, decay=0.6, glide=660, attack=0.1), 0, 0.3, sr)
    add(x, tone(sr, 880, 0.9, decay=0.5, glide=1320, attack=0.12), 0, 0.15, sr)
    add(x, sparkle(sr, rng, 10, 0.05, 0.7, T, 2600, 7000, rise=True), 0, 0.5, sr)
    add(x, chimes(sr, rng, T, 5, 84, 108, 0.3, 0.7, 0.35), 0, 0.7, sr)
    add(x, whoosh(sr, rng, 0.8, 300, 2400, 1.0, 0.8), 0, 0.3, sr)
    return reverb(x, sr, size=0.75, damp=0.3, wet=0.3, tail=0.7)


def shield_break(sr, rng):
    T = 1.5
    x = buf(sr, T)
    add(x, glass_shatter(sr, rng, T, 18), 0, 1.0, sr)
    add(x, thud(sr, 120, 60, 0.12, 0.4), 0, 0.5, sr)
    add(x, tone(sr, 1320, 0.8, decay=0.3, glide=440, attack=0.002), 0, 0.15, sr)
    return reverb(x, sr, size=0.7, damp=0.3, wet=0.25, tail=0.7)


def channel(sr, rng):
    T = 2.2
    x = buf(sr, T)
    # A hum drawn up through her: a soft "oo" chord swelling, wind and chimes.
    c = choir(sr, rng, (55, 62, 67), 2.0, "oo", voices=2, detune=10, vib=(5.0, 10.0))
    add(x, env_apply(c, sr, _env(0.6, 0.7, 0.7)), 0, 0.6, sr)
    wind = svf(noise(int(sr * T), rng), sr, lambda t: 500 + 300 * math.sin(t * 2.1), 1.3, "band")
    add(x, env_apply(wind, sr, _env(0.5, 1.0, 0.7)), 0, 0.3, sr)
    add(x, chimes(sr, rng, T, 4, 79, 100, 0.4, 1.6, 0.4), 0, 0.5, sr)
    return reverb(x, sr, size=0.8, damp=0.35, wet=0.3, tail=0.8)


# --- familiars ------------------------------------------------------------------------------------------------------------

def cat_ambient(sr, rng):
    T = 1.6
    x = buf(sr, T)
    add(x, meow(sr, rng, 430, 760, 0.6), 0, 1.0, sr)
    add(x, purr(sr, rng, 0.9), 0.65, 0.35, sr)
    return room(x, sr, 0.08, 0.25)


def raven_caw(sr, rng):
    T = 1.0
    x = buf(sr, T)
    add(x, caw(sr, rng, 0.3, 640), 0, 1.0, sr)
    add(x, caw(sr, rng, 0.26, 600), 0.42, 0.8, sr)
    return reverb(x, sr, size=0.8, damp=0.4, wet=0.18, tail=0.6)


def familiar_steal(sr, rng):
    T = 0.7
    x = buf(sr, T)
    # A snip (two blades), a quick scurry of air, a tiny chime: something of yours is gone.
    add(x, burst(sr, rng, 0.02, 5600, 2.0, "band", 0.004), 0, 0.9, sr)
    add(x, burst(sr, rng, 0.02, 4800, 2.0, "band", 0.004), 0.035, 0.8, sr)
    add(x, whoosh(sr, rng, 0.3, 1800, 600, 1.2, 0.3), 0.06, 0.5, sr)
    add(x, chimes(sr, rng, T, 2, 88, 100, 0.08, 0.15, 0.25), 0, 0.5, sr)
    return room(x, sr, 0.12, 0.3)


# --- hexed victims ----------------------------------------------------------------------------------------------------------

def victim_ambient(sr, rng):
    T = 1.5
    x = buf(sr, T)
    g = voiced(sr, rng, 1.1, glide(118, 92, 1.1), UH, 0.9, _env(0.15, 0.4, 0.55))
    add(x, rasp(g, 1.8), 0, 0.9, sr)
    add(x, whisper(sr, rng, 0.9, 3, 0.4), 0.25, 0.25, sr)
    return reverb(x, sr, size=0.6, damp=0.45, wet=0.2, tail=0.6)


def victim_freed(sr, rng):
    T = 2.0
    x = buf(sr, T)
    # A breath of relief, then golden chimes climbing and a shimmer.
    br = onepole_lp(noise(int(sr * 0.6), rng), sr, 1800)
    add(x, env_apply(br, sr, _env(0.05, 0.1, 0.45)), 0, 0.35, sr)
    add(x, voiced(sr, rng, 0.5, glide(150, 120, 0.5), HUM, 0.8, _env(0.04, 0.15, 0.3)), 0.05, 0.4, sr)
    for i, m in enumerate((72, 76, 79, 84, 88)):
        add(x, bell_voice(sr, midi_hz(m), 1.4, bright=0.7, beat=1.8), 0.4 + i * 0.09, 0.22, sr)
    add(x, sparkle(sr, rng, 12, 0.4, 1.4, T, 3000, 8000, rise=True), 0, 0.5, sr)
    return reverb(x, sr, size=0.8, damp=0.3, wet=0.3, tail=0.9)


# --- Rowena -----------------------------------------------------------------------------------------------------------------

def rowena_ambient(sr, rng):
    T = 2.0
    notes = [(0.0, 64, 0.35), (0.35, 67, 0.3), (0.65, 71, 0.6)]

    def f0(t):
        f = midi_hz(notes[0][1] - 12)
        for s, m, _ in notes:
            if t >= s:
                f = midi_hz(m - 12)
        return f * (1 + 0.012 * math.sin(TAU * 5.3 * t) * min(1.0, t / 0.5))

    x = buf(sr, T)
    add(x, voiced(sr, rng, 1.3, f0, HUM, 0.3, _env(0.08, 0.8, 0.42)), 0, 0.8, sr)
    add(x, bell_voice(sr, midi_hz(83), 1.4, bright=0.6, beat=1.5), 0.7, 0.15, sr)
    return reverb(x, sr, size=0.6, damp=0.45, wet=0.18, tail=0.6)


def rowena_laugh(sr, rng):
    x = buf(sr, 1.2)
    # Light and amused: "ha-ha-ha", bright vowel, not a cackle.
    add(x, cackle(sr, rng, 380, 4, 0.14, 0.95, AH, 0.6, 0.01), 0, 1.0, sr)
    return room(x, sr, 0.12, 0.35)


def rowena_talk(sr, rng):
    T = 0.8
    x = buf(sr, T)
    # "Mm-hm?": two hummed syllables, the second rising.
    add(x, syllable(sr, rng, 0.22, glide(260, 240, 0.22), HUM, 0.4, 0.02, 0.12), 0, 0.8, sr)
    add(x, syllable(sr, rng, 0.32, glide(240, 330, 0.3), HUM, 0.4, 0.02, 0.2), 0.24, 1.0, sr)
    return room(x, sr, 0.1, 0.3)


def rowena_hurt(sr, rng):
    T = 0.45
    x = buf(sr, T)
    add(x, syllable(sr, rng, 0.32, glide(420, 310, 0.32), AH, 0.6, 0.005, 0.1), 0, 1.0, sr)
    return room(x, sr, 0.08, 0.2)


def rowena_death(sr, rng):
    T = 2.8
    x = buf(sr, T)
    cry = voiced(sr, rng, 1.6, glide(440, 160, 1.6, 0.8), AH, 0.5, _env(0.05, 0.6, 0.95))
    add(x, cry, 0, 0.9, sr)
    # Her bells darkening, a fall to the floor.
    for i, m in enumerate((84, 79, 75, 70)):
        add(x, bell_voice(sr, midi_hz(m), 1.6, bright=0.5, beat=2.2), 0.5 + i * 0.22, 0.2, sr)
    add(x, thud(sr, 90, 50, 0.15, 0.5), 1.5, 0.5, sr)
    return reverb(x, sr, size=0.85, damp=0.35, wet=0.3, tail=1.2)


def rowena_fling(sr, rng):
    T = 0.8
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.35, 800, 4200, 1.3, 0.75), 0, 0.9, sr)
    add(x, bell_voice(sr, midi_hz(91), 0.6, bright=0.9, beat=2.5), 0.25, 0.4, sr)
    add(x, thud(sr, 140, 70, 0.06, 0.2), 0.27, 0.4, sr)
    return room(x, sr, 0.15, 0.35)


def rowena_ignis(sr, rng):
    T = 1.6
    x = buf(sr, T)
    add(x, bell_voice(sr, midi_hz(86), 1.0, bright=0.8, beat=2.0), 0, 0.35, sr)
    add(x, fire_roar(sr, rng, 1.2, 200, 1200, 0.25), 0.08, 1.0, sr)
    add(x, thud(sr, 85, 45, 0.25, 0.7), 0.1, 0.5, sr)
    return reverb(x, sr, size=0.7, damp=0.4, wet=0.2, tail=0.6)


def rowena_glacies(sr, rng):
    T = 1.6
    x = buf(sr, T)
    add(x, chimes(sr, rng, T, 10, 88, 112, 0.0, 0.5, 0.3), 0, 1.0, sr)
    add(x, crackle(sr, rng, 1.0, 0.25, 6500, 1.1, 0.4), 0.05, 0.6, sr)
    add(x, whoosh(sr, rng, 0.9, 3000, 700, 1.0, 0.2), 0, 0.5, sr)
    add(x, burst(sr, rng, 0.2, 2400, 0.8, "band", 0.05), 0.02, 0.4, sr)
    return reverb(x, sr, size=0.75, damp=0.25, wet=0.28, tail=0.8)


def rowena_summon(sr, rng):
    T = 2.6
    x = buf(sr, T)
    swell = reverse(reverb(burst(sr, rng, 0.4, 600, 0.6, "band", 0.15), sr, size=0.9, damp=0.3, wet=0.85, tail=0.8))
    add(x, swell, 0, 0.6, sr)
    p = len(swell) / sr
    c = choir(sr, rng, (50, 57, 62, 65), 1.8, "ah", voices=2, detune=14, vib=(5.0, 14.0))
    add(x, env_apply(c, sr, _env(0.3, 0.8, 0.7)), p - 0.2, 0.6, sr)
    add(x, thud(sr, 70, 35, 0.4, 1.2), p, 0.7, sr)
    add(x, chimes(sr, rng, T, 6, 79, 100, p, p + 0.8, 0.5), 0, 0.6, sr)
    return reverb(x, sr, size=0.9, damp=0.3, wet=0.3, tail=1.2)


def rowena_recite(sr, rng):
    T = 2.4
    x = buf(sr, T)
    # Words spoken low and sure (whispered syllables), a pad of voices and her bells marking the phrase.
    add(x, whisper(sr, rng, 2.0, 8, 1.0), 0, 0.9, sr)
    c = choir(sr, rng, (57, 64, 69), 2.2, "oo", voices=2, detune=9, vib=(5.2, 8.0))
    add(x, env_apply(c, sr, _env(0.5, 1.0, 0.7)), 0.1, 0.4, sr)
    for i, m in enumerate((81, 88, 84)):
        add(x, bell_voice(sr, midi_hz(m), 1.2, bright=0.7, beat=1.6), 0.3 + i * 0.6, 0.18, sr)
    return reverb(x, sr, size=0.8, damp=0.35, wet=0.3, tail=0.9)


def rowena_teleport(sr, rng):
    return smoke_puff(sr, rng, 1.1, 0.8)


# id (the file's base name) -> (builder, peak dBFS)
WITCH = {
    "hedge_ambient": (hedge_ambient, -3.0), "hedge_hurt": (hedge_hurt, -2.0), "hedge_death": (hedge_death, -1.5),
    "coven_ambient": (coven_ambient, -3.0), "coven_hurt": (coven_hurt, -2.0), "coven_death": (coven_death, -1.5),
    "cast": (cast, -2.5), "hex": (hex_, -2.5), "teleport": (teleport, -2.0), "shield_up": (shield_up, -2.5),
    "shield_break": (shield_break, -1.5), "channel": (channel, -4.0), "cat_ambient": (cat_ambient, -3.0),
    "raven_caw": (raven_caw, -2.5), "familiar_steal": (familiar_steal, -2.5), "victim_ambient": (victim_ambient, -3.0),
    "victim_freed": (victim_freed, -2.0),
}

ROWENA = {
    "ambient": (rowena_ambient, -3.5), "laugh": (rowena_laugh, -2.5), "talk": (rowena_talk, -3.0), "hurt": (rowena_hurt, -2.0),
    "death": (rowena_death, -1.5), "fling": (rowena_fling, -2.0), "ignis": (rowena_ignis, -1.5), "glacies": (rowena_glacies, -1.5),
    "summon": (rowena_summon, -1.5), "recite": (rowena_recite, -3.0), "teleport": (rowena_teleport, -2.0),
}
