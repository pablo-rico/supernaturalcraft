"""music.chuck: "The Author" -- a slow, generative piece for the last fight (~2.5 minutes, D minor/aeolian, 64 bpm).

Instruments: an additive felt piano (inharmonic partials, two-stage decay, a soft hammer), a sine pad that drifts out of
tune in the middle, a low drone, and the typewriter itself as percussion (bursts of keys, a carriage return and the
margin bell as cadences). Form:

  bars  0-7   intro    -- the Author's theme high and sparse, like a music box; the pad fades in
  bars  8-17  build    -- the theme in the middle register, slow arpeggios, the first typing bursts
  bars 18-27  unsettle -- Neapolitan and tritone chords, a beating pad, wrong notes, frantic typing
  bars 28-35  return   -- the theme again, quieter; one last line typed
  bars 36-39  outro    -- the drone and a last bell; the piece fades (and loops back to its quiet start)

Everything comes from one seeded random.Random, so the file is identical on every run.
"""

import math
import random

import sfx
from synth import TAU, add, buf, midi_hz, noise, normalize, onepole_lp, reverb, soft_clip

TEMPO = 64
BEAT = 60.0 / TEMPO
BAR = 4 * BEAT
BARS = 40

# Two bars per chord: (bass, [pad tones], [arpeggio tones]).
DM = (38, [50, 53, 57], [50, 57, 62, 65, 62, 57, 53, 57])
BB = (34, [46, 50, 53], [46, 53, 58, 62, 65, 62, 58, 53])
GM = (43, [55, 58, 62], [43, 50, 55, 58, 62, 58, 55, 50])
A7 = (45, [57, 61, 64], [45, 52, 57, 61, 64, 61, 57, 52])
# The middle: a Neapolitan E-flat, D minor over a C# in the bass, a diminished chord on G# (the tritone).
EB = (39, [51, 55, 58], [39, 46, 51, 55, 58, 55, 51, 46])
DM_CS = (37, [50, 53, 57], [37, 50, 53, 57, 61, 57, 53, 50])
GSD = (44, [50, 53, 56], [44, 50, 53, 56, 62, 56, 53, 50])
PROG_A = [DM, BB, GM, A7]
PROG_C = [DM, EB, DM_CS, GSD, DM, EB, GSD, A7]

# The Author's theme: (midi, beats) over 8 bars of PROG_A.
THEME = [(69, 2), (65, 1), (64, 1), (62, 4),
         (65, 2), (67, 1), (69, 1), (70, 3), (69, 1),
         (67, 2), (65, 1), (64, 1), (62, 2), (64, 2),
         (61, 2), (64, 2), (57, 4)]


# --- instruments ----------------------------------------------------------------------------

class Piano:
    """Felt piano voice, rendered once per (note, velocity layer) and cached."""

    def __init__(self, sr, rng):
        self.sr, self.rng, self.cache = sr, rng, {}

    def note(self, midi, layer):
        key = (midi, layer)
        if key not in self.cache:
            self.cache[key] = self._render(midi, layer)
        return self.cache[key]

    def _render(self, midi, layer):
        sr = self.sr
        f0 = midi_hz(midi)
        slow = min(5.5, 3.0 * (261.6 / f0) ** 0.55)
        dur = min(6.5, slow * 2.6 + 0.3)
        n = int(sr * dur)
        out = [0.0] * n
        bright = 0.42 + 0.18 * layer
        for k in range(1, 10):
            fk = f0 * k * math.sqrt(1 + 0.00032 * k * k)
            if fk > sr * 0.42:
                break
            amp = bright ** (k - 1) / k ** 0.7
            ts = slow / (1 + 0.5 * (k - 1))
            tf = 0.22 / (1 + 0.35 * (k - 1))
            a = math.exp(-1.0 / (ts * sr))
            b = math.exp(-1.0 / (tf * sr))
            w = TAU * fk / sr
            c = 2 * math.cos(w)
            y1, y2 = math.sin(-w), math.sin(-2 * w)
            ea, eb = amp * 0.4, amp * 0.6
            limit = min(n, int(ts * sr * 7))
            for i in range(limit):
                y = c * y1 - y2
                y2, y1 = y1, y
                out[i] += y * (ea + eb)
                ea *= a
                eb *= b
        # The hammer's felt thump.
        hn = int(sr * 0.04)
        h = onepole_lp(noise(hn, self.rng), sr, 1800 + 900 * layer)
        for i in range(hn):
            out[i] += h[i] * 0.12 * math.exp(-i / (0.006 * sr))
        att = int(sr * 0.003)
        for i in range(att):
            out[i] *= i / att
        return onepole_lp(out, sr, 2600 + 1800 * layer)


def pad_segment(sr, rng, tones, seconds, detune_hz, extra=()):
    """Soft sine pad: two slightly detuned sines per tone, slow swell in and out, a gentle tremolo."""
    n = int(sr * seconds)
    out = [0.0] * n
    oscs = []
    for m in list(tones) + list(extra):
        f = midi_hz(m)
        for d in (-detune_hz / 2, detune_hz / 2):
            w = TAU * (f + d) / sr
            p = rng.random() * TAU
            oscs.append([2 * math.cos(w), math.sin(p - w), math.sin(p - 2 * w)])
    g = 1.0 / len(oscs)
    for o in oscs:
        c, y1, y2 = o
        for i in range(n):
            y = c * y1 - y2
            y2, y1 = y1, y
            out[i] += y * g
    att = int(sr * min(1.8, seconds / 3))
    rate = rng.uniform(0.15, 0.3)
    for i in range(n):
        e = min(1.0, i / att, (n - i) / att)
        out[i] *= e * e * (0.85 + 0.15 * math.sin(TAU * rate * i / sr))
    return out


# --- the score ------------------------------------------------------------------------------

def compose(sr, rng):
    total = BARS * BAR + 4.0
    mix = buf(sr, total)
    piano = Piano(sr, rng)
    keys = [sfx.type_key(sr, rng, i) for i in range(4)]
    carriage = sfx.carriage(sr, rng, 0)
    bell = sfx.margin_bell(sr, rng)

    def play(midi, t, vel):
        layer = 0 if vel < 0.45 else (1 if vel < 0.75 else 2)
        add(mix, piano.note(midi, layer), t, vel * 0.55, sr)

    def typing(t, count, gain=0.16, fast=False):
        for _ in range(count):
            add(mix, keys[rng.randrange(4)], t, gain * rng.uniform(0.6, 1.0), sr)
            t += rng.uniform(0.07, 0.13) if fast else rng.uniform(0.1, 0.24)
        return t

    def cadence(t, gain=0.2):
        """End of a typed line: the bell rings, then the carriage returns."""
        add(mix, bell, t, gain * 0.7, sr)
        add(mix, carriage, t + 0.35, gain, sr)

    def chord_at(bar):
        if 18 <= bar < 28:
            return PROG_C[((bar - 18) // 2) % len(PROG_C)]
        return PROG_A[(bar // 2) % 4]

    def theme(start_bar, octave, vel, wrong=0.0):
        t = start_bar * BAR
        for midi, beats in THEME:
            m = midi + 12 * octave
            if wrong and rng.random() < wrong:
                m += rng.choice((-1, 1, 6))
            play(m, t + rng.uniform(-0.01, 0.015), vel * rng.uniform(0.85, 1.05))
            t += beats * BEAT

    # Pad and drone: one segment per two bars, overlapping so they crossfade.
    for bar in range(0, BARS, 2):
        bass, tones, _ = chord_at(bar)
        unsettled = 18 <= bar < 28
        start = bar * BAR - 1.0
        seg = pad_segment(sr, rng, tones, 2 * BAR + 2.0, 3.2 if unsettled else 0.9,
                          extra=(tones[0] + 6,) if unsettled and bar % 4 == 2 else ())
        level = 0.10 if bar < 4 else 0.16 if bar < 18 else 0.2 if bar < 28 else 0.15 if bar < 36 else 0.11
        add(mix, seg, max(0.0, start), level, sr)
        drone = pad_segment(sr, rng, (26, 38), 2 * BAR + 2.0, 0.4 if not unsettled else 1.6)
        add(mix, drone, max(0.0, start), 0.05 if bar < 8 else 0.08, sr)

    # Intro: the theme high, like a music box, with a bass note every other bar.
    theme(0, 1, 0.42)
    for bar in range(0, 8, 2):
        play(chord_at(bar)[0] + 12, bar * BAR, 0.35)

    # Build: arpeggios, the theme in the middle, the first typing.
    for bar in range(8, 18):
        bass, _, arp = chord_at(bar)
        play(bass, bar * BAR, 0.55)
        if bar >= 10:
            for i, m in enumerate(arp):
                play(m + 12, bar * BAR + i * BEAT / 2, 0.22 + 0.06 * (i % 2 == 0))
    theme(8, 0, 0.62)
    for bar, beat, n in ((9, 2, 4), (12, 1, 6), (14, 3, 5), (16, 0, 7)):
        typing(bar * BAR + beat * BEAT, n)
    cadence(18 * BAR - 1.1)

    # Unsettle: wrong chords, a clustered bass, a fractured theme, frantic typing.
    for bar in range(18, 28):
        bass, _, arp = chord_at(bar)
        play(bass, bar * BAR, 0.7)
        if bar % 2 == 0:
            play(bass + 1, bar * BAR + 0.02, 0.45)
        pattern = list(arp)
        rng.shuffle(pattern)
        for i, m in enumerate(pattern[:6]):
            play(m + 12, bar * BAR + (i * 2 / 3) * BEAT + rng.uniform(0, 0.08), 0.25 + 0.2 * rng.random())
    theme(18, 0, 0.6, wrong=0.3)
    for bar in (24, 25, 26):
        for i in range(4):
            play(74 + rng.choice((0, 1, 6, 7)), bar * BAR + i * BEAT + BEAT / 2, 0.35)
    for bar, beat, n in ((19, 1, 6), (20, 3, 5), (21, 2, 9), (22, 0, 4), (23, 1, 10), (25, 0, 8), (26, 2, 12)):
        typing(bar * BAR + beat * BEAT, n, 0.18, fast=True)
    cadence(28 * BAR - 1.1, 0.26)

    # Return: the theme again, gentler, over slow arpeggios.
    for bar in range(28, 36):
        bass, _, arp = chord_at(bar)
        play(bass, bar * BAR, 0.45)
        for i, m in enumerate(arp[::2]):
            play(m + 12, bar * BAR + i * BEAT, 0.2)
    theme(28, 1, 0.45)
    typing(31 * BAR + 2 * BEAT, 5, 0.13)
    cadence(36 * BAR - 1.1, 0.18)

    # Outro: a last low D with the fifth above, a last bell, then the fade.
    play(38, 36 * BAR, 0.5)
    play(57, 36 * BAR + 0.05, 0.35)
    play(69, 37 * BAR, 0.3)
    play(62, 38 * BAR, 0.25)
    add(mix, bell, 38.5 * BAR, 0.08, sr)

    out = reverb(mix, sr, size=0.9, damp=0.45, wet=0.22, tail=0.5, predelay=0.02)
    # A gentle limiter: the loudest piano strikes are rounded off so the whole piece sits a little louder.
    return soft_clip(normalize(out, 0.0), 1.8)
