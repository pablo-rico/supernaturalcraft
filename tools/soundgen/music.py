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


# =====================================================================================================================
# v0.42 music.amara.phase1..6: Amara's six cues (sounds/music/amara/phase<n>.ogg). Each is a seamless loop: a steady bed
# (drones and air) rendered exactly periodic -- every frequency and every slow wobble a whole number of cycles in the
# loop, processed twice round so its filters and reverb are in their steady state -- plus the music itself, mixed into
# a buffer XF seconds longer than the loop, reverberated and folded (its tail crossfaded over its head). The music starts
# after the fold and settles before the end, so both ends sit at the bed's calm level.
#
#   1 primordial  -- before Creation: sub drones, slow dark clusters swelling and passing, distant metallic swells;
#   2 music box   -- the child: a detuned music box lullaby over a cold pad, children humming, small wrong notes, the
#                    box running down at the end;
#   3 noir        -- the woman: a dark tango -- plucked bass in 3-3-2, sustained minor strings, a slow sultry violin;
#   4 two themes  -- God and the Darkness: a warm major hymn and a dark chromatic minor motif, alone, then fighting
#                    in counterpoint, then braided into one line that ends on an open fifth (neither major nor minor);
#   5 cosmic      -- the face in the sky: enormous airy choir pads drifting through far-apart chords, shimmering
#                    partials, deep-space glides;
#   6 the end     -- a dying heartbeat slowing under a darkening drone, silence, and the dawn: a warm, bright major chord
#                    that swells and fades back to nothing (this one begins and ends at near-silence).
# =====================================================================================================================

import bisect  # noqa: E402

from sfx import bell_voice, voiced  # noqa: E402
from synth import env_apply, formants, partials, pluck, smooth_random, svf, tone  # noqa: E402

CH_OO = [(380, 3.0, 1.0), (1100, 4.0, 0.3), (2900, 6.0, 0.08)]
VOWELS = {
    "ah": [(730, 4.0, 1.0), (1090, 5.0, 0.55), (2440, 7.0, 0.3), (3400, 8.0, 0.12)],
    "oo": [(300, 3.0, 1.0), (870, 4.0, 0.35), (2240, 6.0, 0.1)],
    "oh": [(500, 3.5, 1.0), (880, 4.5, 0.5), (2600, 7.0, 0.15)],
}


# --- loop machinery ---------------------------------------------------------------------------------------------------

def _fold(x, sr, T, xf):
    """Equal-power fold: the last xf seconds (past T) crossfaded over the first xf; returns exactly T seconds."""
    n = int(sr * T)
    f = int(sr * xf)
    if len(x) < n + f:
        x = x + [0.0] * (n + f - len(x))
    out = x[:n]
    for i in range(f):
        k = i / f * math.pi / 2
        out[i] = x[i] * math.sin(k) + x[n + i] * math.cos(k)
    return out


def _loop_lfo(rng, T, points, lo, hi):
    """A smooth random curve that repeats every T seconds (cosine-interpolated through `points` values)."""
    pts = [rng.uniform(lo, hi) for _ in range(points)]

    def f(t):
        u = (t / T * points) % points
        i = int(u)
        fr = 0.5 - 0.5 * math.cos(math.pi * (u - i))
        a = pts[i % points]
        return a + (pts[(i + 1) % points] - a) * fr
    return f


def _bed(sr, rng, T, drones=(), airs=(), wet=0.25, size=0.95, damp=0.5):
    """The loop's steady floor, exactly periodic in T.
    drones = [(Hz, amp, wobble_depth)]: sines at the nearest whole-cycle frequency, each breathing slowly;
    airs = [(fc_lo, fc_hi, q, amp)]: noise (tiled, so periodic) through a band-pass wandering between fc_lo and fc_hi."""
    n = int(sr * T)
    m = 2 * n
    x = [0.0] * m
    for f, amp, depth in drones:
        w = TAU * round(f * T) / T / sr
        ph = rng.random() * TAU
        lfo = _loop_lfo(rng, T, max(2, int(T / 12)), 1 - depth, 1.0)
        s = [math.sin(w * i + ph) * amp for i in range(m)]
        add(x, env_apply(s, sr, lfo), 0)
    for lo, hi, q, amp in airs:
        src = noise(n, rng)
        src = src + src
        fc = _loop_lfo(rng, T, max(2, int(T / 8)), lo, hi)
        gain = _loop_lfo(rng, T, max(2, int(T / 6)), 0.4, 1.0)
        y = svf(src, sr, fc, q, "band")
        add(x, env_apply(y, sr, lambda t, g=gain, a=amp: g(t) * a), 0)
    y = reverb(x, sr, size=size, damp=damp, wet=wet, tail=0.0)
    return y[n:m]


def _finish_loop(events, bed, sr, T, xf, wet, size=0.95, damp=0.45, predelay=0.03, drive=1.3, bed_gain=1.0):
    """Reverb on the music, the fold, the bed under it, a gentle limiter."""
    y = reverb(events, sr, size=size, damp=damp, wet=wet, tail=0.0, predelay=predelay)
    y = _fold(y, sr, T, xf)
    if bed:
        for i in range(len(y)):
            y[i] += bed[i] * bed_gain
    return soft_clip(normalize(y, 0.0), drive)


# --- instruments ------------------------------------------------------------------------------------------------------

def _saw_cr(sr, seconds, f0, cents=0.0, phase=0.0, vib=(0.0, 0.0), curve=None, ctrl=32):
    """A naive sawtooth with its frequency updated every `ctrl` samples (cheap vibrato and glides).
    curve(t) -> Hz overrides f0; vib = (rate Hz, depth cents)."""
    n = int(sr * seconds)
    out = [0.0] * n
    p = phase % 1.0
    k = 2 ** (cents / 1200)
    rate, depth = vib
    for s in range(0, n, ctrl):
        t = s / sr
        f = (curve(t) if curve else f0) * k
        if depth:
            f *= 2 ** (depth * math.sin(TAU * rate * t) / 1200)
        inc = f / sr
        for i in range(s, min(n, s + ctrl)):
            out[i] = 2 * p - 1
            p += inc
            if p >= 1.0:
                p -= 1.0
    return out


def _choir(sr, rng, notes, seconds, vowel="ah", voices=3, detune=10.0, vib=(4.8, 7.0)):
    """A soft synthetic choir: detuned vibrato saws per note through a vowel."""
    x = [0.0] * int(sr * seconds)
    g = 1.0 / (voices * max(1, len(notes)))
    for m in notes:
        f = midi_hz(m)
        for v in range(voices):
            d = (v - (voices - 1) / 2) * detune * 2 / max(1, voices - 1)
            s = _saw_cr(sr, seconds, f, d, rng.random(), (vib[0] * rng.uniform(0.85, 1.15), vib[1]))
            for i, a in enumerate(s):
                x[i] += a * g
    return formants(x, sr, VOWELS[vowel])


def _swell(seconds, attack, release, power=2.0):
    def f(t):
        a = min(1.0, t / attack) if attack else 1.0
        r = min(1.0, max(0.0, seconds - t) / release) if release else 1.0
        return (a * r) ** power
    return f


def _metal_swell(sr, rng, f0, seconds, rise=0.7):
    """A distant metallic swell: struck-steel partials held and swelling up like a bell played backwards."""
    n = int(sr * seconds)
    out = [0.0] * n
    for r, a in ((1.0, 1.0), (1.51, 0.6), (2.27, 0.4), (3.11, 0.25), (4.42, 0.14)):
        for d in (0.0, rng.uniform(0.5, 1.4)):
            f = f0 * r + d
            if f > sr * 0.45:
                continue
            w = TAU * f / sr
            ph = rng.random() * TAU
            for i in range(n):
                out[i] += math.sin(w * i + ph) * a * 0.5
    peak = seconds * rise

    def env(t):
        if t < peak:
            return (t / peak) ** 3
        return max(0.0, 1 - (t - peak) / (seconds - peak)) ** 2
    return env_apply(onepole_lp(out, sr, 2400), sr, env)


def _tine(sr, midi, cents=0.0):
    """A music-box tine: a bright plucked comb tooth, its fundamental ringing, a high clangy mode dying fast, a tick."""
    f = midi_hz(midi) * 2 ** (cents / 1200)
    spec = [(f, 1.0, 1.3), (f * 1.0012, 0.35, 1.1), (f * 2.0, 0.1, 0.5), (f * 5.93, 0.22, 0.09), (f * 9.2, 0.06, 0.04)]
    x = partials(sr, 3.2, spec, attack=0.0008)
    m = int(sr * 0.003)
    for i in range(min(len(x), m)):
        x[i] += (1 - i / m) * 0.25 * (1 if i % 2 else -1)
    return x


class _Box:
    def __init__(self, sr):
        self.sr, self.cache = sr, {}

    def note(self, midi, cents):
        key = (midi, int(round(cents)))
        if key not in self.cache:
            self.cache[key] = _tine(self.sr, midi, key[1])
        return self.cache[key]


def _melody_curve(notes, beat, glide_s=0.07, vib=(5.2, 0.012), vib_delay=0.25):
    """f0(t) and env(t) for a legato line: notes = [(midi or None, beats)]; portamento between notes, vibrato that comes in
    late on long notes, a swell at each note's start and a breath before each rest."""
    starts = []
    t = 0.0
    for m, b in notes:
        starts.append((t, b * beat, m))
        t += b * beat
    total = t
    keys = [s for s, _, _ in starts]

    def find(u):
        return max(0, min(len(starts) - 1, bisect.bisect_right(keys, u) - 1))

    def f0(u):
        i = find(u)
        s, d, m = starts[i]
        if m is None:
            j = i - 1
            while j >= 0 and starts[j][2] is None:
                j -= 1
            return midi_hz(starts[j][2] if j >= 0 else 60)
        prev = starts[i - 1][2] if i > 0 else None
        mm = m
        if prev is not None and u - s < glide_s:
            k = 0.5 - 0.5 * math.cos(math.pi * (u - s) / glide_s)
            mm = prev + (m - prev) * k
        v = vib[1] * min(1.0, max(0.0, (u - s - vib_delay) / 0.4)) * math.sin(TAU * vib[0] * u)
        return midi_hz(mm) * (1 + v)

    def env(u):
        i = find(u)
        s, d, m = starts[i]
        if m is None or u > total:
            return 0.0
        into = u - s
        rest_next = i + 1 >= len(starts) or starts[i + 1][2] is None
        rel = 0.25 if rest_next else 0.06
        left = d - into
        r = min(1.0, left / rel) if left < rel else 1.0
        a = min(1.0, into / 0.12)
        start_level = 0.0 if (i == 0 or starts[i - 1][2] is None) else 0.75
        swell = 0.85 + 0.15 * min(1.0, into / max(0.2, d * 0.7))
        return max(0.0, (start_level + (1 - start_level) * a) * r * swell)
    return f0, env, total


def _bowed(sr, rng, notes, beat, dark=False, gain=1.0):
    """A bowed string line (violin, or a cello when dark): a vibrato saw through a body's resonances, bow noise on it."""
    f0, env, total = _melody_curve(notes, beat, vib=(5.4, 0.011 if not dark else 0.008))
    T = total + 0.4
    s = _saw_cr(sr, T, 0, 0.0, rng.random(), curve=f0)
    s2 = _saw_cr(sr, T, 0, 7.0, rng.random(), curve=f0)
    x = [a + 0.6 * b for a, b in zip(s, s2)]
    x = svf(x, sr, 2600 if dark else 4200, 0.7, "low")
    body = [(280, 1.6, 0.9), (520, 2.0, 0.7), (1050, 2.4, 0.5), (2900, 2.5, 0.45)] if not dark else \
        [(180, 1.4, 1.0), (420, 1.8, 0.7), (900, 2.2, 0.4), (2000, 2.4, 0.2)]
    y = formants(x, sr, body)
    add(y, x, 0, 0.25)
    bow = svf(noise(len(y), rng), sr, 3200 if not dark else 1800, 1.0, "band")
    add(y, bow, 0, 0.05)
    return env_apply([v * gain for v in y], sr, env)


def _horn(sr, rng, notes, beat, gain=1.0):
    """A warm hymn voice: soft saws darkened, a little vibrato, round like a horn or a flute stop of an organ."""
    f0, env, total = _melody_curve(notes, beat, glide_s=0.05, vib=(4.6, 0.006), vib_delay=0.4)
    T = total + 0.4
    x = _saw_cr(sr, T, 0, 0.0, rng.random(), curve=f0)
    add(x, _saw_cr(sr, T, 0, -6.0, rng.random(), curve=lambda u: f0(u) * 0.5), 0, 0.4)
    x = svf(x, sr, 1500, 0.6, "low")
    x = formants(x, sr, [(500, 1.5, 1.0), (1000, 2.0, 0.5), (2200, 3.0, 0.15)])
    return env_apply([v * gain for v in x], sr, env)


def _strings(sr, rng, notes, seconds, cutoff=1800.0, attack=0.4, release=0.6):
    """A string section on one chord: three detuned vibrato saws per note, low-passed, slow bow."""
    x = [0.0] * int(sr * seconds)
    g = 1.0 / (3 * max(1, len(notes)))
    for m in notes:
        f = midi_hz(m)
        for c in (-8.0, 0.0, 8.0):
            s = _saw_cr(sr, seconds, f, c, rng.random(), (rng.uniform(4.6, 5.8), 6.0))
            for i, a in enumerate(s):
                x[i] += a * g
    x = svf(x, sr, cutoff, 0.7, "low")
    return env_apply(x, sr, _swell(seconds, attack, release, 1.0))


def _organ(sr, rng, notes, seconds, attack=1.0, release=1.5):
    """A soft additive organ/pad: each note with its octave and twelfth and a slightly detuned double (a slow chorus)."""
    n = int(sr * seconds)
    x = [0.0] * n
    for m in notes:
        f = midi_hz(m)
        for r, a, d in ((1.0, 1.0, 0.0), (2.0, 0.3, 0.0), (3.0, 0.12, 0.0), (1.0, 0.6, rng.uniform(0.3, 0.9))):
            ff = f * r + d
            if ff > sr * 0.45:
                continue
            w = TAU * ff / sr
            c = 2 * math.cos(w)
            ph = rng.random() * TAU
            y1, y2 = math.sin(ph - w) * a, math.sin(ph - 2 * w) * a
            for i in range(n):
                y = c * y1 - y2
                y2 = y1
                y1 = y
                x[i] += y
    g = 1.0 / max(1, len(notes))
    sw = _swell(seconds, attack, release, 1.5)
    return env_apply(x, sr, lambda t: sw(t) * g)


def _heartbeat(sr, rng, strength=1.0, pitch=1.0):
    x = [0.0] * int(sr * 0.8)
    for at, gg, f0 in ((0.0, 1.0, 60), (0.25, 0.62, 72)):
        add(x, tone(sr, f0 * pitch, 0.45, decay=0.09, glide=f0 * pitch * 0.62, attack=0.004), at, gg * strength, sr)
        knock = env_apply(svf(noise(int(sr * 0.08), rng), sr, 320 * pitch, 1.2, "band"), sr, lambda t: math.exp(-t / 0.02))
        add(x, knock, at, 0.45 * gg * strength, sr)
        th = onepole_lp(onepole_lp(noise(int(sr * 0.12), rng), sr, 150), sr, 150)
        th = env_apply(th, sr, lambda t: min(1.0, t / 0.006) * math.exp(-t / 0.03))
        add(x, th, at, 3.0 * gg * strength, sr)
    return x


def _timpani(sr, rng, f0=55, gain=1.0):
    x = tone(sr, f0, 1.6, decay=0.45, glide=f0 * 0.93, attack=0.003)
    add(x, tone(sr, f0 * 1.5, 1.0, decay=0.25, attack=0.003), 0, 0.35)
    th = onepole_lp(noise(int(sr * 0.08), rng), sr, 600)
    add(x, env_apply(th, sr, lambda t: math.exp(-t / 0.015)), 0, 0.6)
    return [v * gain for v in x]


# --- 1: primordial --------------------------------------------------------------------------------------------------

def amara_phase1(sr, rng):
    """Before Creation: a vast sub drone, dark clusters swelling up and passing, metallic swells far away."""
    T, XF = 120, 6.0
    bed = _bed(sr, rng, T, drones=((36.71, 0.16, 0.3), (73.42, 0.13, 0.35), (110.0, 0.07, 0.5), (110.4, 0.06, 0.5),
                                   (146.83, 0.035, 0.6)),
               airs=((60, 170, 0.9, 0.16), (260, 900, 2.0, 0.06)), wet=0.3)
    mix = buf(sr, T + XF)
    clusters = [[50, 51, 57], [48, 55, 56], [49, 50, 56], [46, 53, 54], [50, 52, 57], [45, 51, 52], [47, 48, 54],
                [50, 51, 57], [44, 50, 51], [49, 55, 56], [46, 47, 53], [50, 51, 57]]
    L = 16.0
    for k, notes in enumerate(clusters):
        start = XF + 8.0 * k
        x = [0.0] * int(sr * L)
        for m in notes:
            for c in (-9.0, 9.0):
                s = _saw_cr(sr, L, midi_hz(m), c + rng.uniform(-2, 2), rng.random(), (rng.uniform(0.15, 0.3), 5.0))
                add(x, s, 0, 0.12)
        sweep = smooth_random(sr, L, rng, 0.25, 250, 950)
        x = svf(x, sr, sweep, 0.9, "low")
        add(x, pad_segment(sr, rng, [m - 12 for m in notes], L, 0.6), 0, 0.25)
        add(mix, env_apply(x, sr, lambda t: math.sin(math.pi * min(1.0, t / L)) ** 2), start, 0.55, sr)
    t = XF + 4.0
    while t < T - 14:
        f0 = rng.choice((196.0, 233.1, 277.2, 311.1, 155.6, 415.3))
        Ls = rng.uniform(6.0, 9.0)
        add(mix, _metal_swell(sr, rng, f0, Ls, rng.uniform(0.6, 0.8)), t, rng.uniform(0.05, 0.09), sr)
        t += rng.uniform(9.0, 13.0)
    for at in (XF + 20, XF + 58, XF + 90):
        br = onepole_lp(onepole_lp(noise(int(sr * 7), rng), sr, 220), sr, 220)
        add(mix, env_apply(br, sr, lambda u: math.sin(math.pi * min(1.0, u / 7)) ** 2), at, 1.2, sr)
    return _finish_loop(mix, bed, sr, T, XF, wet=0.35, size=0.97, damp=0.5, predelay=0.05)


# --- 2: the music box ----------------------------------------------------------------------------------------------

_BOX_A = [(76, 2), (81, 1), (79, 1), (77, 1), (76, 1), (74, 2), (76, 1), (72, 3),
          (71, 2), (72, 1), (74, 1), (76, 1), (77, 1), (76, 2), (68, 1), (69, 3)]
_BOX_B = [(84, 2), (83, 1), (81, 1), (80, 1), (81, 1), (77, 2), (76, 1), (75, 3),
          (76, 1), (77, 1), (76, 1), (74, 1), (72, 1), (71, 1), (70, 2), (71, 1), (69, 3)]
# (root, pad tones) per bar.
_AM, _DM, _G, _C, _F = (45, [57, 60, 64]), (50, [57, 62, 65]), (43, [55, 59, 62]), (48, [55, 60, 64]), (41, [57, 60, 65])
_E, _E7, _BDIM, _BB = (40, [56, 59, 64]), (40, [56, 62, 64]), (47, [53, 59, 62]), (46, [58, 62, 65])
_BOX_CH_A = [_AM, _DM, _G, _C, _F, _E, _E7, _AM]
_BOX_CH_B = [_AM, _F, _DM, _BDIM, _AM, _DM, _BB, _AM]


def amara_phase2(sr, rng):
    """The child: a music box lullaby in A minor, its tines drifting out of tune, over a cold pad; children hum it; a few
    wrong notes creep in; at the end the box runs down."""
    T, XF = 96, 4.0
    beat = 60.0 / 76
    bar = 3 * beat
    bed = _bed(sr, rng, T, drones=((55.0, 0.10, 0.4), (82.41, 0.06, 0.5), (164.81, 0.025, 0.7)),
               airs=((2800, 5200, 3.0, 0.035), (300, 700, 1.5, 0.04)), wet=0.35)
    mix = buf(sr, T + XF)
    box = _Box(sr)
    t0 = XF + 0.5
    # Sections: (first bar, melody, chords, detune cents, hums?, running down?)
    sections = [(2, _BOX_A, _BOX_CH_A, 5, False, False), (10, _BOX_A, _BOX_CH_A, 9, True, False),
                (18, _BOX_B, _BOX_CH_B, 14, True, False), (26, _BOX_A, _BOX_CH_A, 24, True, True)]
    # The cold pad and the box's low note, bar by bar, with an intro and an outro.
    pad_bars = [(0, _AM), (1, _AM)]
    for first, _, chords, _, _, _ in sections:
        pad_bars += [(first + i, c) for i, c in enumerate(chords)]
    pad_bars += [(34, _AM), (35, _AM)]
    for b, (root, tones) in pad_bars:
        stretch = 1.0 + (0.22 * ((b - 26) / 8) ** 2 if b >= 26 else 0.0)
        at = t0 + (b if b < 26 else 26 + (b - 26) * stretch) * bar
        seg = pad_segment(sr, rng, [m + 12 for m in tones], bar * stretch + 1.6, 1.8)
        add(mix, seg, at - 0.8, 0.16, sr)
        if b < 34:
            add(mix, box.note(root + 24, rng.uniform(-4, 4)), at, 0.14, sr)
    for first, melody, chords, detune, hums, slowing in sections:
        t = t0 + first * bar
        total = sum(bt for _, bt in melody)
        acc = 0.0
        for m, bt in melody:
            k = 1.0 + (0.22 * (acc / total) ** 2 if slowing else 0.0)
            at = t + acc * beat * k
            vel = rng.uniform(0.75, 1.0) * (1.0 - (0.35 * acc / total if slowing else 0.0))
            add(mix, box.note(m + 12, rng.uniform(-2, 2)), at, 0.36 * vel, sr)
            add(mix, box.note(m + 12, detune + rng.uniform(-3, 3)), at + 0.004, 0.22 * vel, sr)
            if rng.random() < 0.14:      # a wrong note creeping in just before
                add(mix, box.note(m + 12 + rng.choice((-1, 1)), detune), at - 0.07, 0.16 * vel, sr)
            if rng.random() < 0.3:
                add(mix, box.note(m, detune * 0.5), at + beat * k, 0.1 * vel, sr)
            acc += bt
        # The comb's mechanism ticking very softly on every beat.
        for j in range(int(total)):
            k = 1.0 + (0.22 * (j / total) ** 2 if slowing else 0.0)
            tick = svf(noise(int(sr * 0.01), rng), sr, 4200, 1.5, "band")
            add(mix, env_apply(tick, sr, lambda u: math.exp(-u / 0.0015)), t + j * beat * k, 0.03, sr)
        if hums:
            notes = [(m - 12, bt) for m, bt in melody]
            f0, env, length = _melody_curve(notes, beat, glide_s=0.09, vib=(5.6, 0.012), vib_delay=0.2)
            for voice, (cents, delay, gain) in enumerate(((0.0, 0.0, 0.12), (14.0, 0.035, 0.08))):
                if slowing:
                    def warp(u, total_s=length):
                        # inverse of the box's slowing: u = acc*(1+0.22(acc/total)^2) solved by bisection
                        lo, hi = 0.0, total_s
                        for _ in range(24):
                            mid = (lo + hi) / 2
                            if mid * (1 + 0.22 * (mid / total_s) ** 2) < u:
                                lo = mid
                            else:
                                hi = mid
                        return lo
                    span = length * 1.22 + 0.3
                else:
                    warp = None
                    span = length + 0.3
                hum = voiced(sr, rng, span,
                             lambda u, f0=f0, c=cents, w=warp: f0(w(u) if w else u) * 2 ** (c / 1200), CH_OO, 0.5,
                             lambda u, env=env, w=warp: env(w(u) if w else u))
                add(mix, hum, t + delay, gain * (0.6 if voice and first == 10 else 1.0), sr)
    return _finish_loop(mix, bed, sr, T, XF, wet=0.32, size=0.93, damp=0.35, predelay=0.04, drive=1.2)


# --- 3: noir strings -------------------------------------------------------------------------------------------------

_N_DM = (38, 45, [50, 53, 57, 62])
_N_GM = (43, 50, [50, 55, 58, 62])
_N_A7 = (45, 40, [49, 52, 55, 57])
_N_BB = (46, 41, [50, 53, 58, 62])
_N_GM6 = (43, 50, [52, 55, 58, 62])
_N_EB = (39, 46, [51, 55, 58, 63])
_N_P1 = [(69, 3), (70, 1), (69, 2), (67, 1), (65, 1), (64, 4), (None, 2), (69, 1), (72, 1), (74, 3), (76, 1), (77, 2), (76, 1),
         (74, 1), (73, 3), (76, 1), (74, 4)]
_N_P2 = [(81, 3), (79, 1), (77, 2), (76, 1), (74, 1), (82, 3), (81, 1), (79, 4), (79, 1), (77, 1), (76, 1), (74, 1), (73, 2),
         (74, 1), (76, 1), (77, 2), (76, 2), (74, 4)]
_N_CH_P1 = [_N_DM, _N_GM, _N_A7, _N_DM, _N_BB, _N_GM6, _N_A7, _N_DM]
_N_CH_P2 = [_N_DM, _N_BB, _N_GM, _N_EB, _N_GM, _N_A7, _N_BB, _N_DM]
_N_CH_IN = [_N_DM, _N_GM, _N_A7, _N_DM, _N_BB, _N_GM6, _N_A7, _N_A7]


def amara_phase3(sr, rng):
    """The woman: a dark tango in D minor -- the plucked bass in 3-3-2, sustained minor strings, pizzicato and a brush on
    the off-beats, and a slow, sultry violin; a cello takes the tune in the middle."""
    T, XF = 108, 5.0
    beat = 60.0 / 96
    bar = 4 * beat
    bed = _bed(sr, rng, T, drones=((73.42, 0.06, 0.5), (55.0, 0.05, 0.5)), airs=((400, 1200, 1.5, 0.025),), wet=0.3)
    mix = buf(sr, T + XF)
    t0 = XF + 0.5
    plucks = {}
    pizzes = {}

    def bass(m, at, vel):
        key = (m, rng.randrange(2))
        if key not in plucks:
            p = pluck(sr, midi_hz(m), 1.4, rng, brightness=0.35, damping=0.995)
            plucks[key] = env_apply(onepole_lp(p, sr, 1400), sr, lambda u: math.exp(-u / 0.5))
        add(mix, plucks[key], at, 0.55 * vel, sr)

    def pizz(notes, at, vel):
        for m in notes[1:]:
            key = (m, rng.randrange(2))
            if key not in pizzes:
                p = pluck(sr, midi_hz(m + 12), 0.4, rng, brightness=0.55, damping=0.99)
                pizzes[key] = env_apply(p, sr, lambda u: math.exp(-u / 0.09))
            add(mix, pizzes[key], at + rng.uniform(0, 0.012), 0.08 * vel, sr)

    def brush(at, vel):
        b = svf(noise(int(sr * 0.18), rng), sr, 6200, 0.8, "band")
        add(mix, env_apply(b, sr, lambda u: min(1.0, u / 0.01) * math.exp(-u / 0.05)), at, 0.06 * vel, sr)

    # Sections: (first bar, chords, string octave, level)
    plan = [(0, [_N_DM, _N_DM, _N_A7, _N_A7], 0, 0.8), (4, _N_CH_P1, 0, 1.0), (12, _N_CH_P2, 12, 1.0), (20, _N_CH_IN, 0, 0.85),
            (28, _N_CH_P1, 0, 1.0), (36, [_N_DM, _N_GM, _N_A7, _N_DM], 0, 0.7)]
    for first, chords, octave, level in plan:
        for i, (root, fifth, tones) in enumerate(chords):
            b = first + i
            t = t0 + b * bar
            last = b == 39
            fade = 1.0 - (0.15 * (b - 36) if b >= 36 else 0.0)
            bass(root, t, 1.0 * level * fade)
            bass(root, t + 1.5 * beat, 0.8 * level * fade)
            if not last:
                bass(fifth, t + 3 * beat, 0.85 * level * fade)
                nxt = chords[i + 1][0] if i + 1 < len(chords) else 38
                if b % 2 == 1:
                    bass(nxt + rng.choice((-1, 1)), t + 3.5 * beat, 0.55 * level * fade)
            seg = _strings(sr, rng, [m + octave for m in tones], bar + 0.9, 1500 + 600 * level, 0.45, 0.7)
            add(mix, seg, t - 0.1, 0.5 * level * fade, sr)
            if 4 <= b < 36:
                pizz(tones, t + beat, 0.9)
                pizz(tones, t + 3 * beat, 0.7)
                brush(t + beat, 1.0)
                brush(t + 3 * beat, 0.8)
                brush(t + 2.5 * beat, 0.4)
    cello_p1 = [(m - 12 if m else None, b) for m, b in _N_P1]
    add(mix, _bowed(sr, rng, _N_P1, beat, False), t0 + 4 * bar, 0.26, sr)
    add(mix, _bowed(sr, rng, _N_P2, beat, False), t0 + 12 * bar, 0.28, sr)
    add(mix, _bowed(sr, rng, cello_p1, beat, True), t0 + 20 * bar, 0.3, sr)
    add(mix, _bowed(sr, rng, _N_P1, beat, False), t0 + 28 * bar, 0.24, sr)
    add(mix, _bowed(sr, rng, cello_p1, beat, True), t0 + 28 * bar + 0.02, 0.1, sr)
    return _finish_loop(mix, bed, sr, T, XF, wet=0.22, size=0.85, damp=0.45, predelay=0.025, drive=1.4)


# --- 4: two themes interlocking ---------------------------------------------------------------------------------------

_HYMN = [(66, 1), (67, 1), (69, 2), (71, 1), (69, 1), (67, 1), (66, 1), (64, 1), (66, 1), (67, 2), (66, 4),
         (69, 1), (71, 1), (73, 1), (74, 1), (76, 1), (74, 1), (73, 1), (71, 1), (69, 1), (67, 1), (66, 1), (64, 1), (62, 4)]
_DARK = [(50, 2), (51, 1), (50, 1), (49, 3), (45, 1), (46, 2), (45, 1), (44, 1), (45, 4),
         (50, 1), (53, 1), (52, 1), (51, 1), (50, 1), (49, 1), (48, 1), (47, 1), (46, 2), (45, 2), (38, 4)]
_BRAID = [(66, 1), (67, 1), (69, 2), (70, 1), (69, 1), (67, 1), (65, 1), (64, 1), (66, 1), (67, 2), (65, 4),
          (69, 1), (70, 1), (72, 1), (74, 1), (76, 1), (74, 1), (73, 1), (71, 1), (69, 1), (67, 1), (66, 1), (64, 1), (62, 4)]
_H_D, _H_G, _H_A, _H_A7 = (38, [50, 57, 62, 66]), (43, [55, 59, 62, 67]), (45, [57, 61, 64, 69]), (45, [55, 61, 64, 69])
_K_DM, _K_A, _K_BB, _K_CL = (38, [50, 53, 57]), (45, [49, 52, 57]), (46, [46, 50, 53, 57]), (37, [49, 50, 53])
_K_GM = (43, [50, 55, 58])
_HYMN_CH = [_H_D, _H_G, _H_A, _H_D, _H_D, _H_A, _H_A7, _H_D]
_DARK_CH = [_K_DM, _K_A, _K_BB, _K_A, _K_DM, _K_CL, _K_BB, _K_DM]
_BRAID_CH = [_H_D, _K_GM, _H_A, _K_DM, _K_BB, _H_A, _H_A7, _H_D]


def amara_phase4(sr, rng):
    """God and the Darkness: his warm hymn in D major, her dark chromatic motif in D minor; then both at once, fighting
    (major and minor chords trading blows, timpani under them); then braided into one line in octaves that ends on an open
    fifth."""
    T, XF = 116, 5.0
    beat = 60.0 / 76
    bar = 4 * beat
    bed = _bed(sr, rng, T, drones=((73.42, 0.10, 0.4), (110.0, 0.05, 0.5), (36.71, 0.12, 0.3)),
               airs=((300, 900, 1.6, 0.03),), wet=0.3)
    mix = buf(sr, T + XF)
    t0 = XF + 0.5

    def hymn_chord(ch, at, level):
        root, tones = ch
        add(mix, _choir(sr, rng, tones, bar + 1.4, "oh", 3, 9.0), at - 0.3, 0.55 * level, sr)
        add(mix, _organ(sr, rng, [root], bar + 1.4, 0.4, 0.8), at - 0.2, 0.25 * level, sr)

    def dark_chord(ch, at, level):
        root, tones = ch
        add(mix, _choir(sr, rng, tones, bar + 1.4, "oo", 3, 22.0, (3.6, 12.0)), at - 0.3, 0.6 * level, sr)
        low = onepole_lp(onepole_lp(_saw_cr(sr, bar + 1.2, midi_hz(root - 12), 0, rng.random()), sr, 220), sr, 220)
        add(mix, env_apply(low, sr, _swell(bar + 1.2, 0.4, 0.8, 1.0)), at - 0.1, 0.25 * level, sr)

    # I: the hymn.
    for i, ch in enumerate(_HYMN_CH):
        hymn_chord(ch, t0 + i * bar, 0.8)
    add(mix, _horn(sr, rng, _HYMN, beat), t0, 0.32, sr)
    # II: the darkness.
    s2 = t0 + 8 * bar
    for i, ch in enumerate(_DARK_CH):
        dark_chord(ch, s2 + i * bar, 0.8)
    add(mix, _bowed(sr, rng, _DARK, beat, True), s2, 0.34, sr)
    # III: both, fighting.
    s3 = t0 + 16 * bar
    for i in range(8):
        if i % 2 == 0:
            hymn_chord(_HYMN_CH[i], s3 + i * bar, 0.65)
        else:
            dark_chord(_DARK_CH[i], s3 + i * bar, 0.7)
        add(mix, _timpani(sr, rng, 55 if i % 2 == 0 else 51.9, 0.35 + 0.05 * i), s3 + i * bar, 1.0, sr)
        if i >= 4:
            add(mix, _timpani(sr, rng, 55, 0.25), s3 + i * bar + 2 * beat, 1.0, sr)
    add(mix, _horn(sr, rng, [(m + 12, b) for m, b in _HYMN], beat), s3, 0.24, sr)
    add(mix, _bowed(sr, rng, _DARK, beat, True), s3, 0.36, sr)
    # IV: braided -- one line, both voices in octaves, the chords mixing major and minor.
    s4 = t0 + 24 * bar
    for i, ch in enumerate(_BRAID_CH):
        (hymn_chord if ch in (_H_D, _H_A, _H_A7) else dark_chord)(ch, s4 + i * bar, 0.75)
    add(mix, _horn(sr, rng, _BRAID, beat), s4, 0.28, sr)
    add(mix, _bowed(sr, rng, [(m - 12, b) for m, b in _BRAID], beat, True), s4, 0.28, sr)
    # Coda: the open fifth, neither major nor minor, fading into the bed.
    s5 = t0 + 32 * bar
    add(mix, env_apply(_choir(sr, rng, [50, 57, 62, 69], 2 * bar + 2.0, "oo", 3, 8.0), sr,
                       _swell(2 * bar + 2.0, 0.8, 4.0, 1.5)), s5 - 0.3, 0.5, sr)
    add(mix, _organ(sr, rng, [38, 45], 2 * bar + 2.0, 0.5, 4.0), s5 - 0.2, 0.3, sr)
    return _finish_loop(mix, bed, sr, T, XF, wet=0.3, size=0.93, damp=0.4, predelay=0.04, drive=1.3)


# --- 5: cosmic choir ---------------------------------------------------------------------------------------------------

_COSMIC = [([52, 59, 64, 66, 71], "ah"), ([49, 56, 64, 68, 71], "oo"), ([45, 57, 61, 63, 68], "ah"),
           ([52, 58, 61, 66, 70], "oh"), ([56, 59, 63, 66, 71], "ah"), ([48, 55, 59, 64, 67], "oo"),
           ([47, 54, 59, 64, 66], "ah"), ([52, 59, 64, 66, 71], "oh"), ([50, 55, 59, 62, 66], "oo"),
           ([45, 57, 59, 64, 69], "ah"), ([52, 59, 64, 66, 71], "oo")]


def amara_phase5(sr, rng):
    """The face in the sky: enormous airy choir pads drifting through far-apart chords (E lydian, chromatic mediants),
    shimmering high partials, slow deep-space glides, in a vast reverb."""
    T, XF = 120, 6.0
    bed = _bed(sr, rng, T, drones=((41.2, 0.06, 0.3), (82.41, 0.05, 0.4), (123.47, 0.03, 0.6), (82.9, 0.025, 0.5)),
               airs=((2500, 6500, 2.0, 0.03), (500, 1400, 1.2, 0.03)), wet=0.4, size=0.98)
    mix = buf(sr, T + XF)
    L = 14.0
    for k, (notes, vowel) in enumerate(_COSMIC):
        start = XF + 9.0 * k
        ch = _choir(sr, rng, notes, L, vowel, 3, 14.0, (4.2, 6.0))
        sweep = smooth_random(sr, L, rng, 0.2, 1400, 3400)
        ch = svf(ch, sr, sweep, 0.7, "low")
        add(mix, env_apply(ch, sr, lambda t: math.sin(math.pi * min(1.0, t / L)) ** 2), start, 0.6, sr)
        # Shimmer: a few of the chord's tones two and three octaves up, trembling.
        for m in rng.sample(notes, 3):
            f = midi_hz(m + rng.choice((24, 36)))
            if f > sr * 0.4:
                f /= 2
            trem = smooth_random(sr, L, rng, rng.uniform(0.3, 0.7), 0.0, 1.0)
            s = tone(sr, f + rng.uniform(-1.5, 1.5), L, phase=rng.random() * TAU)
            add(mix, env_apply(s, sr, lambda t, tr=trem: math.sin(math.pi * min(1.0, t / L)) ** 2 * tr(t) ** 2), start, 0.025,
                sr)
    for at in (XF + 14, XF + 47, XF + 78, XF + 100):
        f0 = rng.choice((220.0, 196.0, 246.9))
        g = tone(sr, f0, 7.0, glide=f0 * 0.75, attack=2.0)
        add(g, tone(sr, f0 * 1.5, 7.0, glide=f0 * 1.5 * 0.75, attack=2.5), 0, 0.3)
        add(mix, env_apply(g, sr, lambda t: math.sin(math.pi * min(1.0, t / 7.0)) ** 2), at, 0.05, sr)
    return _finish_loop(mix, bed, sr, T, XF, wet=0.45, size=0.98, damp=0.4, predelay=0.06, drive=1.2)


# --- 6: the dying heart and the dawn ------------------------------------------------------------------------------

def amara_phase6(sr, rng):
    """The end: a heartbeat slowing and weakening under a drone that darkens (a minor second, then a tritone creep in,
    the light filtered away); it stops; a breath of silence; then the dawn -- a warm, bright D major chord with bells,
    swelling and fading back to nothing."""
    T, XF = 100, 3.0
    mix = buf(sr, T + XF)
    # The heartbeat: 64 bpm slowing to ~28, weakening.
    t = XF + 0.5
    stop = 71.0
    while t < stop:
        k = min(1.0, (t - XF) / (stop - XF))
        add(mix, _heartbeat(sr, rng, 1.0 - 0.45 * k ** 2, 1.0 - 0.08 * k), t, 0.9, sr)
        t += 0.94 + (2.15 - 0.94) * k ** 1.3
    # The drone: D and A at first, then F, then Eb (the minor second), then Ab (the tritone); darker and louder.
    D = 76.0
    start = XF - 1.0
    drone = buf(sr, D)
    for m, enter, gain in ((38, 0.0, 1.0), (45, 0.0, 0.7), (41, 12.0, 0.6), (39, 30.0, 0.55), (44, 46.0, 0.5),
                           (50, 22.0, 0.35)):
        for c in (-7.0, 7.0):
            s = _saw_cr(sr, D, midi_hz(m), c, rng.random(), (0.2, 4.0))
            add(drone, env_apply(s, sr, lambda u, e=enter: min(1.0, max(0.0, (u - e) / 8.0)) ** 2), 0, 0.1 * gain)
    drone = svf(drone, sr, lambda u: 1100 * (0.22 ** min(1.0, u / 66.0)), 0.8, "low")
    end = stop + 3.0 - start
    add(mix, env_apply(drone, sr, lambda u: (0.25 + 0.75 * min(1.0, u / 60.0)) * min(1.0, u / 6.0) *
                       min(1.0, max(0.0, end - u) / 3.5)), start, 0.9, sr)
    for at, f0 in ((XF + 18, 233.1), (XF + 38, 207.7), (XF + 55, 185.0)):
        add(mix, _metal_swell(sr, rng, f0, 8.0, 0.75), at, 0.07, sr)
    # The dawn.
    dawn = stop + 4.5
    L = T - dawn - 0.5
    swell = _swell(L, 5.0, 13.0, 1.8)
    add(mix, env_apply(_choir(sr, rng, [50, 57, 62, 66, 69, 74, 76], L, "ah", 3, 9.0, (4.6, 6.0)), sr, swell), dawn, 0.75, sr)
    add(mix, env_apply(_organ(sr, rng, [38, 45, 54], L, 0.1, 0.1), sr, swell), dawn, 0.35, sr)
    for i, m in enumerate((74, 78, 81, 86, 90)):
        add(mix, bell_voice(sr, midi_hz(m), 4.0, 0.6, 1.3), dawn + 3.0 + 0.45 * i, 0.12, sr)
    for _ in range(14):
        at = dawn + 3.5 + rng.random() * 9.0
        f = midi_hz(rng.choice((86, 90, 93, 98)))
        add(mix, partials(sr, 1.5, [(f, 1.0, 0.25), (f * 2.76, 0.3, 0.12)], attack=0.002), at, rng.uniform(0.02, 0.05), sr)
    return _finish_loop(mix, None, sr, T, XF, wet=0.32, size=0.95, damp=0.4, predelay=0.04, drive=1.2)


AMARA = {
    "phase1": (amara_phase1, -3.0), "phase2": (amara_phase2, -3.0), "phase3": (amara_phase3, -3.0),
    "phase4": (amara_phase4, -3.0), "phase5": (amara_phase5, -3.0), "phase6": (amara_phase6, -3.0),
}
