"""v0.18 Heaven: sounds/heaven/<id>.ogg (HeavenAssets.SOUNDS_HEAVEN), sounds/crossroads/<id>.ogg (SOUNDS_CROSSROADS) and the
music tracks in MUSIC (music.heaven, music.naomi, music.zachariah -> sounds/heaven/music_<name>.ogg).

Same conventions as sfx.py / raphael_sfx.py: (sr, rng) -> float list, finished and encoded by generate.py. Heaven is warm and
near: soft choir pads, small bells, warm air; never the vast brass of Michael's. The crossroads are dry earth and a low wind.

Music (rendered at the music rate and folded so they loop seamlessly, no fades; streamed):
  heaven     a calm hymn in D: choir pads, a harp's broken chords, slow bells and a warm low drone. 12 bars at 60 bpm (48 s).
  naomi      tense and clinical in A minor: a ticking pulse, a cold sine ostinato, glassy pads, a monitor's beep and string
             swells that never resolve. 16 bars at 120 bpm (32 s).
  zachariah  a bureaucratic march in C minor: snare rudiments, a tuba-like bass on the beat, low brass, a typewriter's ticks
             and a stamp on every downbeat. 16 bars at 112 bpm (34.3 s).
"""

import math

from gabriel_sfx import Kit, loopify
from michael_sfx import hall, trumpet, whoosh
from sfx import UH, bell_voice, burst, choir, sparkle, thud, voiced
from synth import add, buf, env_apply, midi_hz, noise, normalize, onepole_lp, partials, pluck, reverb, smooth_random, soft_clip, svf, tone


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def harp(sr, rng, m, d=1.6):
    return env_apply(pluck(sr, midi_hz(m), d, rng, 0.45, 0.997), sr, lambda t: 1.0 if t < d - 0.2 else max(0.0, (d - t) / 0.2))


def warm_air(sr, rng, T, fc=500):
    """A soft breath of warm air: low-passed noise rising and falling."""
    x = onepole_lp(onepole_lp(noise(int(sr * T), rng), sr, fc), sr, fc)
    return env_apply(x, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 2)


# =====================================================================================================
# Heaven
# =====================================================================================================

def gate_open(sr, rng):
    """A gate of light opens: a low swell of air, a choir rising out of it, bells spilling upwards."""
    T = 4.0
    x = buf(sr, T)
    add(x, thud(sr, 80, 50, 0.4, 1.0), 0.0, 0.5, sr)
    add(x, warm_air(sr, rng, 2.6, 700), 0.0, 1.6, sr)
    c = choir(sr, rng, (62, 69, 74, 78), 3.2, "ah", voices=3, detune=8)
    c = svf(c, sr, lambda t: 400 * (8 ** min(1.0, t / 1.6)), 0.8, "low")
    add(x, env_apply(c, sr, _env(1.2, 0.9, 1.1)), 0.2, 1.1, sr)
    add(x, sparkle(sr, rng, 22, 0.6, 2.6, T, 2000, 7000), 0, 0.7, sr)
    for i, m in enumerate((74, 78, 81, 86)):
        add(x, bell_voice(sr, midi_hz(m), 1.8, bright=0.5, beat=0.7), 0.8 + i * 0.14, 0.12, sr)
    return hall(x, sr, 0.25, 1.4)


def gate_hum(sr, rng):
    """The gate at rest: a hushed choir chord breathing in and out, a faint high shimmer."""
    T = 3.0
    x = buf(sr, T)
    c = choir(sr, rng, (62, 69, 74), T, "oo", voices=3, detune=6)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / T) ** 1.5), 0, 1.2, sr)
    add(x, sparkle(sr, rng, 6, 0.3, 2.4, T, 4000, 8000, rise=False), 0, 0.25, sr)
    return hall(x, sr, 0.2, 0.8)


def arrive(sr, rng):
    """Arriving: a rush of warm air washing everything white, a major chord blooming, birds of light (bells) settling."""
    T = 3.6
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 1.2, 300, 1800, 0.7, 0.5), 0.0, 0.7, sr)
    c = choir(sr, rng, (62, 66, 69, 74), 2.6, "ah", voices=3, detune=7)
    add(x, env_apply(c, sr, _env(0.4, 0.8, 1.4)), 0.5, 1.0, sr)
    for i, m in enumerate((78, 81, 85, 90, 86)):
        add(x, bell_voice(sr, midi_hz(m), 1.5, bright=0.4, beat=0.6), 0.7 + i * 0.11, 0.1, sr)
    add(x, warm_air(sr, rng, 2.4, 500), 0.6, 0.9, sr)
    return hall(x, sr, 0.25, 1.3)


def memory_enter(sr, rng):
    """A memory unfolds: a reversed bloom drawing in, then a soft, slightly detuned chord, as if heard through years."""
    T = 3.0
    x = buf(sr, T)
    bloom = buf(sr, 1.4)
    for m in (69, 74, 78):
        add(bloom, bell_voice(sr, midi_hz(m), 1.4, bright=0.6, beat=1.2), 0, 0.25, sr)
    bloom = reverb(bloom, sr, size=0.9, damp=0.3, wet=0.4, tail=0.6)
    add(x, bloom[::-1], 0.0, 0.8, sr)
    c = choir(sr, rng, (57, 64, 69, 73), 1.8, "oo", voices=3, detune=14)
    c = svf(c, sr, 1600, 0.7, "low")
    add(x, env_apply(c, sr, _env(0.2, 0.6, 1.0)), 1.9, 1.0, sr)
    return hall(x, sr, 0.25, 1.0)


def memory_collect(sr, rng):
    """A memory gathered: three bells climbing, a choir's soft "ah" taking them in."""
    x = buf(sr, 2.2)
    for i, m in enumerate((74, 78, 81)):
        add(x, bell_voice(sr, midi_hz(m), 1.6, bright=0.6, beat=0.8), i * 0.13, 0.3, sr)
    c = choir(sr, rng, (62, 69, 74), 1.4, "ah", voices=2, detune=6)
    add(x, env_apply(c, sr, _env(0.2, 0.4, 0.8)), 0.3, 0.8, sr)
    add(x, sparkle(sr, rng, 10, 0.3, 1.2, 2.2, 3500, 8000), 0, 0.4, sr)
    return hall(x, sr, 0.22, 0.9)


def memory_leave(sr, rng):
    """The memory fades: its chord sinking and closing, as if a door swung shut on it."""
    T = 2.4
    x = buf(sr, T)
    c = choir(sr, rng, (69, 74, 78), T, "ah", voices=3, detune=10)
    c = svf(c, sr, lambda t: 3000 * (0.08 ** min(1.0, t / T)), 0.8, "low")
    add(x, env_apply(c, sr, _env(0.05, 0.6, 1.6)), 0, 1.0, sr)
    add(x, sparkle(sr, rng, 8, 0.0, 1.2, T, 2500, 6000, rise=False), 0, 0.3, sr)
    add(x, whoosh(sr, rng, 1.0, 1400, 250, 0.7, 0.3), 0.1, 0.4, sr)
    return hall(x, sr, 0.2, 0.9)


def hearth_rest(sr, rng):
    """Resting by the hearth: the fire's crackle, a log settling, the warmth humming, one gentle bell."""
    T = 3.0
    x = buf(sr, T)
    pops = [0.0] * int(sr * T)
    for i in range(len(pops)):
        if rng.random() < 0.0012:
            pops[i] = rng.uniform(-1, 1)
    add(x, svf(pops, sr, 2600, 1.0, "band"), 0, 1.6, sr)
    add(x, onepole_lp(env_apply(noise(int(sr * T), rng), sr, lambda t: math.sin(math.pi * t / T)), sr, 400), 0, 0.5, sr)
    add(x, thud(sr, 110, 70, 0.06, 0.25), 1.1, 0.4, sr)
    add(x, burst(sr, rng, 0.2, 700, 0.8, "band", 0.05), 1.1, 0.3, sr)
    c = choir(sr, rng, (50, 57, 62), 2.4, "oo", voices=2, detune=5)
    add(x, env_apply(c, sr, lambda t: math.sin(math.pi * t / 2.4) ** 2), 0.3, 0.6, sr)
    add(x, bell_voice(sr, midi_hz(81), 1.6, bright=0.3, beat=0.5), 0.4, 0.12, sr)
    return reverb(x, sr, size=0.5, damp=0.5, wet=0.12, tail=0.5)


def seal_open(sr, rng):
    """A seal of light unlocks: a crystalline click, the sigil ringing as it dissolves upwards, a breath of choir."""
    T = 2.8
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.02, 4200, 1.0, "high", 0.002), 0.0, 0.8, sr)
    add(x, partials(sr, 2.0, [(1046.5, 0.3, 0.9), (1568, 0.22, 0.7), (2637, 0.15, 0.4), (3951, 0.08, 0.25)], 0.001), 0.0, 0.6, sr)
    add(x, sparkle(sr, rng, 18, 0.1, 1.6, T, 2600, 9000), 0, 0.6, sr)
    c = choir(sr, rng, (67, 74, 79), 1.6, "ah", voices=2, detune=8)
    add(x, env_apply(c, sr, _env(0.3, 0.4, 0.9)), 0.2, 0.7, sr)
    add(x, thud(sr, 95, 60, 0.12, 0.4), 0.0, 0.4, sr)
    return hall(x, sr, 0.22, 1.0)


def ash_greet(sr, rng):
    """Ash at the bar: a glass set down with a clink and a laid-back, rising "heyyy"."""
    x = buf(sr, 1.4)
    add(x, partials(sr, 0.6, [(2860, 0.3, 0.25), (4170, 0.2, 0.18), (6020, 0.1, 0.1)], 0.0005), 0.0, 0.5, sr)
    add(x, burst(sr, rng, 0.03, 900, 1.0, "band", 0.01), 0.0, 0.4, sr)
    v = voiced(sr, rng, 0.75, lambda t: 118 * (1 + 0.25 * math.sin(math.pi * min(1.0, t / 0.75))), UH, 0.6, _env(0.05, 0.4, 0.3))
    add(x, v, 0.32, 0.9, sr)
    return reverb(x, sr, size=0.45, damp=0.5, wet=0.1, tail=0.3)


SOUNDS = {
    "gate_open": (gate_open, -1.5), "gate_hum": (gate_hum, -5.0), "arrive": (arrive, -1.5), "memory_enter": (memory_enter, -2.0),
    "memory_collect": (memory_collect, -2.0), "memory_leave": (memory_leave, -3.0), "hearth_rest": (hearth_rest, -3.0),
    "seal_open": (seal_open, -2.0), "ash_greet": (ash_greet, -3.0),
}


# =====================================================================================================
# The crossroads
# =====================================================================================================

def bury(sr, rng):
    """A box buried: a spade biting into packed earth twice, dirt shovelled back and patted down."""
    x = buf(sr, 1.9)
    for t in (0.0, 0.45):
        add(x, burst(sr, rng, 0.25, 500, 0.7, "band", 0.06), t, 1.0, sr)
        add(x, burst(sr, rng, 0.04, 2400, 0.8, "high", 0.006), t, 0.5, sr)
        add(x, thud(sr, 120, 70, 0.04, 0.12), t, 0.4, sr)
    grit = svf(noise(int(sr * 0.5), rng), sr, 1800, 0.6, "band")
    grit = env_apply(grit, sr, lambda t: (0.5 + 0.5 * math.sin(t * 90)) * math.sin(math.pi * t / 0.5))
    add(x, grit, 0.9, 0.4, sr)
    for t in (1.45, 1.62):
        add(x, burst(sr, rng, 0.08, 300, 0.8, "low", 0.03), t, 0.8, sr)
    return reverb(soft_clip(x, 1.6), sr, size=0.4, damp=0.6, wet=0.06, tail=0.25)


def wild_arrive(sr, rng):
    """He comes to a real crossroads: the wind dropping low and dry, a long hiss through the grass, a step in the dust."""
    T = 4.0
    x = buf(sr, T)
    wind = svf(noise(int(sr * T), rng), sr, lambda t: 300 + 500 * math.sin(math.pi * min(1.0, t / 2.5)), 0.9, "band")
    gust = smooth_random(sr, T, rng, 1.5, 0.3, 1.0)
    add(x, env_apply(wind, sr, lambda t: gust(t) * math.sin(math.pi * t / T)), 0, 1.0, sr)
    add(x, onepole_lp(env_apply(noise(int(sr * T), rng), sr, lambda t: math.sin(math.pi * t / T) ** 2), sr, 120), 0, 2.0, sr)
    add(x, tone(sr, 49, 2.5, attack=0.8), 0.6, 0.18, sr)
    add(x, burst(sr, rng, 0.1, 600, 1.0, "band", 0.03), 2.9, 0.6, sr)
    add(x, thud(sr, 85, 50, 0.06, 0.2), 2.9, 0.5, sr)
    return reverb(x, sr, size=0.7, damp=0.5, wet=0.12, tail=0.6)


CROSSROADS = {"bury": (bury, -2.0), "wild_arrive": (wild_arrive, -2.0)}


# =====================================================================================================
# Music (loops)
# =====================================================================================================

def music_heaven(sr, rng):
    bpm, bars = 60, 12
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 4.0)
    kit = Kit(sr, rng)
    D, G, Bm, A, Em, Fsm = (38, (62, 66, 69, 73)), (43, (62, 67, 71, 74)), (35, (62, 66, 71, 74)), (45, (61, 64, 69, 73)), \
        (40, (59, 64, 67, 71)), (42, (61, 66, 69, 73))
    prog = [D, G, Bm, A, D, G, Em, A, Bm, G, Fsm, A]
    pads = {}
    for b, (root, ch) in enumerate(prog):
        t = b * 4 * beat
        key = (root, ch)
        if key not in pads:
            c = choir(sr, rng, ch, 4 * beat + 1.0, "oo", voices=2, detune=6)
            pads[key] = env_apply(c, sr, _env(1.2, 4 * beat - 1.0, 0.8))
        add(mix, pads[key], t, 0.55, sr)
        add(mix, env_apply(tone(sr, midi_hz(root), 4 * beat + 0.6), sr, _env(0.8, 4 * beat - 1.0, 0.8)), t, 0.16, sr)
        # The harp: a broken chord rising across each bar.
        notes = [ch[0] - 12, ch[1] - 12, ch[2] - 12, ch[0], ch[1], ch[2], ch[3], ch[2]]
        for k, m in enumerate(notes):
            add(mix, kit._get(("hp", m), lambda m=m: harp(sr, rng, m)), t + k * beat / 2, 0.16 if k else 0.2, sr)
        if b % 2 == 1:
            add(mix, kit._get(("bl", ch[3] + 12), lambda m=ch[3] + 12: bell_voice(sr, midi_hz(m), 2.5, bright=0.35, beat=0.5)),
                t + 3 * beat, 0.07, sr)
    # A slow melody on the bells over the second half.
    line = [(78, 2), (76, 1), (74, 1), (73, 2), (69, 2), (71, 2), (74, 2), (76, 4),
            (78, 2), (81, 2), (79, 2), (78, 2), (76, 3), (73, 1), (74, 4)]
    t = 4 * 4 * beat
    for m, d in line:
        add(mix, kit._get(("bm", m), lambda m=m: bell_voice(sr, midi_hz(m), 2.2, bright=0.4, beat=0.6)), t, 0.12, sr)
        t += d * beat
    out = reverb(mix, sr, size=0.95, damp=0.35, wet=0.22, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.2)


def music_naomi(sr, rng):
    bpm, bars = 120, 16
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 3.0)
    kit = Kit(sr, rng)
    prog = [(45, (57, 60, 64)), (41, (57, 60, 65)), (45, (57, 60, 64)), (44, (56, 59, 64)),
            (45, (57, 60, 64)), (38, (57, 62, 65)), (40, (56, 59, 64)), (44, (56, 59, 62))]
    blip = tone(sr, 1760, 0.09)
    blip = env_apply(blip, sr, _env(0.003, 0.06, 0.02))
    for b in range(bars):
        root, ch = prog[b % 8]
        t = b * 4 * beat
        # The cold ostinato: sine eighths, root and fifth, the octave on the off-beats.
        for k in range(8):
            m = root + 12 + (0, 7, 12, 7, 0, 7, 15, 7)[k]
            add(mix, kit._get(("os", m), lambda m=m: env_apply(tone(sr, midi_hz(m), beat * 0.45), sr, _env(0.004, beat * 0.2, beat * 0.2))),
                t + k * beat / 2, 0.11, sr)
        add(mix, kit._get(("sb", root), lambda root=root: env_apply(tone(sr, midi_hz(root - 12), 4 * beat), sr, _env(0.02, 3.3 * beat, 0.7 * beat))),
            t, 0.3, sr)
        # Glassy pad (string swell) every other bar, never resolving.
        if b % 2 == 0:
            for m in ch:
                add(mix, kit.strings(m + 12, 8 * beat), t, 0.035, sr)
        # Ticks: a clock-like closed hat on every eighth, accent on the beat.
        for k in range(8):
            add(mix, kit.hat(False), t + k * beat / 2, 0.07 if k % 2 else 0.11, sr)
        # The monitor: one beep a bar (two in the second half, as the pulse quickens).
        add(mix, blip, t, 0.12, sr)
        if b >= 8:
            add(mix, blip, t + 2 * beat, 0.1, sr)
        if b % 4 == 3:
            add(mix, kit.timpani(root - 12), t + 3 * beat, 0.25, sr)
    out = reverb(mix, sr, size=0.55, damp=0.3, wet=0.12, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.3)


def music_zachariah(sr, rng):
    bpm, bars = 112, 16
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 3.0)
    kit = Kit(sr, rng)
    prog = [(36, (60, 63, 67)), (36, (60, 63, 67)), (41, (60, 65, 68)), (43, (59, 62, 67)),
            (36, (60, 63, 67)), (44, (60, 63, 68)), (41, (60, 65, 68)), (43, (59, 62, 65))]
    for b in range(bars):
        root, ch = prog[b % 8]
        t = b * 4 * beat
        # Tuba-like bass on the beat: root, fifth.
        for k, step in enumerate((0, 7, 0, 7)):
            add(mix, kit.bass(root + step, beat * 0.6), t + k * beat, 0.4, sr)
        # Snare rudiments: a march figure, a roll into every fourth bar.
        for off in (0.0, 1.0, 1.5, 2.0, 3.0, 3.25, 3.5):
            add(mix, kit.snare(), t + off * beat, 0.22 if off in (0.0, 2.0) else 0.14, sr)
        if b % 4 == 3:
            for k in range(8):
                add(mix, kit.snare(), t + 3 * beat + k * beat / 8, 0.08 + 0.02 * k, sr)
        # The stamp on every downbeat; a typewriter tick on the off-beats.
        add(mix, kit.kick(), t, 0.5, sr)
        add(mix, kit.clap(), t, 0.2, sr)
        for k in range(4):
            add(mix, kit.hat(False), t + k * beat + beat / 2, 0.09, sr)
        # Low brass chords, held two beats at bars' halves.
        if b % 2 == 0:
            for m in ch:
                add(mix, kit._get(("br", m), lambda m=m: trumpet(sr, rng, m - 12, 2 * beat * 0.95, 4.0)), t, 0.05, sr)
    # The march tune on the brass in the second half.
    tune = [(67, 1), (67, 0.5), (68, 0.5), (70, 1), (67, 1), (65, 1), (63, 1), (62, 2),
            (63, 1), (63, 0.5), (65, 0.5), (67, 1), (63, 1), (62, 1), (60, 1), (59, 2),
            (67, 1), (67, 0.5), (68, 0.5), (70, 1), (72, 1), (70, 1), (68, 1), (67, 2),
            (65, 1), (63, 1), (62, 1), (59, 1), (60, 4)]
    t = 8 * 4 * beat
    for m, d in tune:
        add(mix, kit._get(("tn", m, d), lambda m=m, d=d: trumpet(sr, rng, m, d * beat * 0.92, 5.0)), t, 0.09, sr)
        t += d * beat
    out = reverb(mix, sr, size=0.6, damp=0.45, wet=0.1, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.3)


MUSIC = {"heaven": (music_heaven, -5.0), "naomi": (music_naomi, -6.0), "zachariah": (music_zachariah, -3.0)}
