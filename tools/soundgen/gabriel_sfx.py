"""v0.14 Gabriel, the Trickster: TV Land's sound effects and the four channels' jingles. Same conventions as sfx.py /
michael_sfx.py: (sr, rng) -> float list, finished and encoded by generate.py into sounds/gabriel/<id>.ogg
(GabrielAssetData points supernaturalcraft:gabriel.<id> at them).

Effects: the canned laugh track (a crowd of detuned voiced laughs over a murmur), applause (dense random claps), the
game-show buzzer (square waves), the "ding ding" of a right answer, TV static, the heart monitor's beep, the
defibrillator (charge whine and thump), a falling piano (whistle, then a discordant crash), a pie splat, a finger snap,
"welcome" (a cheesy fanfare sting), his chuckle, hurt and death.

Jingles (LOOPS, rendered at the music rate and folded so they loop seamlessly, no fades): jingle_sitcom (a cheery 80s
sitcom theme), jingle_game_show (a frantic synth game-show theme), jingle_hospital (a dramatic medical-drama string
piece), jingle_commercial (a bright, smarmy ad jingle).
"""

import math

from michael_sfx import hall, trumpet
from sfx import burst, sparkle, thud, voiced, HUM, UH
from synth import (TAU, add, buf, env_apply, exp_decay, midi_hz, noise, normalize, onepole_lp, partials, pluck, reverb, saw,
                   soft_clip, svf, tone)

AH = [(780, 4.0, 1.0), (1240, 5.0, 0.5), (2600, 7.0, 0.2)]
EH = [(560, 4.0, 1.0), (1720, 5.0, 0.45), (2550, 7.0, 0.2)]


def _env(a, s, r):
    T = a + s + r

    def f(t):
        if t < a:
            return t / a
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def square(sr, freq, seconds, duty=0.5, detune=0.0):
    n = int(sr * seconds)
    f = freq * 2 ** (detune / 1200)
    p = f / sr
    return [1.0 if (i * p) % 1.0 < duty else -1.0 for i in range(n)]


def loopify(x, sr, length):
    """Folds everything after `length` seconds (the reverb tail, ringing notes) back onto the start, so the piece loops
    without a seam."""
    n = int(sr * length)
    out = list(x[:n]) + [0.0] * max(0, n - len(x))
    for i in range(n, len(x)):
        out[(i - n) % n] += x[i]
    return out


# =====================================================================================================
# Effects
# =====================================================================================================

def _laugher(sr, rng, T, f0, rate, vowel):
    """One voice in the audience: "ha-ha-ha" syllables at `rate` a second, falling in pitch and loudness."""
    jit = rng.uniform(0.8, 1.2)

    def env(t):
        if t > T:
            return 0.0
        ph = (t * rate) % 1.0
        pulse = math.sin(math.pi * min(1.0, ph / 0.55)) ** 1.5 if ph < 0.55 else 0.0
        return pulse * min(1.0, t / 0.05) * max(0.0, 1 - t / T) ** 0.7

    def f0c(t):
        ph = (t * rate) % 1.0
        return f0 * (1 - 0.12 * t / T) * (1 + 0.08 * (1 - ph)) * jit
    return voiced(sr, rng, T, f0c, vowel, 0.9, env)


def laugh_track(sr, rng):
    """The canned laugh: a burst of many laughing voices (men, women, a cackle or two), a murmur, a little room."""
    T = 3.2
    x = buf(sr, T)
    for i in range(16):
        f0 = rng.choice((105, 120, 135, 150, 210, 240, 270, 300, 330))
        dur = rng.uniform(1.4, 2.8)
        at = rng.uniform(0.0, 0.35) ** 1.5
        v = _laugher(sr, rng, dur, f0 * rng.uniform(0.95, 1.05), rng.uniform(4.6, 6.8), AH if i % 3 else EH)
        add(x, v, at, rng.uniform(0.5, 1.0), sr)
    bed = svf(noise(int(sr * T), rng), sr, 900, 0.6, "band")
    add(x, env_apply(bed, sr, lambda t: min(1.0, t / 0.15) * max(0.0, 1 - t / T) ** 1.3), 0, 0.5, sr)
    return reverb(x, sr, size=0.6, damp=0.5, wet=0.12, tail=0.5)


def applause(sr, rng):
    """Studio applause: a swell of dense random claps over a hiss, held, fading."""
    T = 3.2
    x = buf(sr, T)
    for c in range(28):
        rate = rng.uniform(3.6, 5.6)
        fc = rng.uniform(900, 2600)
        t = rng.uniform(0, 0.25)
        while t < T - 0.1:
            g = min(1.0, t / 0.3) * max(0.0, 1 - max(0.0, t - 2.0) / 1.1)
            cl = burst(sr, rng, 0.04, fc * rng.uniform(0.9, 1.1), 1.4, "band", rng.uniform(0.006, 0.012))
            add(x, cl, t, g * rng.uniform(0.25, 0.5), sr)
            t += 1 / rate * rng.uniform(0.8, 1.2)
    hiss = svf(noise(int(sr * T), rng), sr, 2400, 0.5, "band")
    add(x, env_apply(hiss, sr, lambda t: min(1.0, t / 0.3) * max(0.0, 1 - max(0.0, t - 2.0) / 1.1)), 0, 0.25, sr)
    return reverb(x, sr, size=0.6, damp=0.5, wet=0.1, tail=0.4)


def buzzer(sr, rng):
    """The wrong-answer buzzer: two harsh square waves a fifth-and-a-bit apart, held, buzzing."""
    T = 0.9
    x = buf(sr, T)
    for f, g in ((110, 0.5), (116.5, 0.4), (165, 0.25)):
        add(x, square(sr, f, T, 0.42), 0, g, sr)
    x = svf(x, sr, 2200, 0.8, "low")
    x = env_apply(x, sr, _env(0.01, T - 0.07, 0.06))
    return soft_clip(x, 1.6)


def ding(sr, rng):
    """Right answer: "ding ding!" -- two bright bells, the second higher."""
    x = buf(sr, 1.6)
    for at, m in ((0.0, 88), (0.16, 93)):
        f = midi_hz(m)
        add(x, partials(sr, 1.4, [(f, 0.6, 0.9), (f * 2.0, 0.25, 0.5), (f * 2.76, 0.18, 0.35), (f * 5.4, 0.06, 0.15)],
                        attack=0.001), at, 1.0, sr)
    return reverb(x, sr, size=0.5, damp=0.4, wet=0.12, tail=0.4)


def static(sr, rng):
    """TV static: hissing white noise, crackles, a flutter of the old set's hum."""
    T = 1.4
    n = noise(int(sr * T), rng)
    x = svf(n, sr, 5200, 0.5, "low")
    x = [v * (0.75 + 0.25 * math.sin(TAU * 60 * i / sr)) for i, v in enumerate(x)]
    for _ in range(14):
        add(x, burst(sr, rng, 0.02, 3000, 0.8, "high", 0.002), rng.uniform(0, T - 0.05), rng.uniform(0.4, 1.0), sr)
    add(x, tone(sr, 15734 / 2, T, attack=0.05), 0, 0.02, sr)
    return env_apply(x, sr, _env(0.02, T - 0.2, 0.18))


def monitor_beep(sr, rng):
    """The heart monitor: one clean 1 kHz beep."""
    x = buf(sr, 0.4)
    b = tone(sr, 1000, 0.16, attack=0.004)
    b = env_apply(b, sr, _env(0.004, 0.13, 0.025))
    add(x, b, 0, 0.9, sr)
    add(x, env_apply(tone(sr, 2000, 0.16), sr, _env(0.004, 0.13, 0.025)), 0, 0.08, sr)
    return reverb(x, sr, size=0.3, damp=0.6, wet=0.05, tail=0.15)


def defib(sr, rng):
    """The defibrillator: the rising charge whine, a beat, then the thump and the zap."""
    T = 1.9
    x = buf(sr, T)
    whine = tone(sr, 420, 1.05, glide=3600, attack=0.05)
    whine = [v * (0.8 + 0.2 * math.sin(TAU * 24 * i / sr)) for i, v in enumerate(whine)]
    add(x, env_apply(whine, sr, lambda t: min(1.0, t / 0.4) * (1 if t < 1.0 else 0.0)), 0, 0.35, sr)
    add(x, tone(sr, 3600, 0.12, decay=0.05), 1.05, 0.15, sr)
    at = 1.18
    add(x, thud(sr, 95, 38, 0.12, 0.5), at, 1.0, sr)
    add(x, burst(sr, rng, 0.25, 4200, 0.7, "high", 0.05), at, 0.55, sr)
    for k in range(8):
        add(x, burst(sr, rng, 0.03, 2500 + 2000 * rng.random(), 2.0, "band", 0.004), at + k * 0.018 + 0.01 * rng.random(), 0.5, sr)
    return reverb(soft_clip(x, 1.4), sr, size=0.5, damp=0.5, wet=0.1, tail=0.4)


def piano(sr, rng):
    """A piano falls out of the sky: the cartoon whistle, then a discordant crash of strings and splintering wood."""
    import music
    T = 3.4
    x = buf(sr, T)
    whistle = tone(sr, 2200, 0.9, glide=480, attack=0.02)
    add(x, env_apply(whistle, sr, lambda t: min(1.0, t / 0.1)), 0, 0.3, sr)
    at = 0.95
    pn = music.Piano(sr, rng)
    for m in (28, 33, 41, 46, 52, 57, 61, 66, 70, 75, 80):
        add(x, pn.note(m, 2)[: int(sr * 2.4)], at + rng.uniform(0, 0.03), rng.uniform(0.25, 0.4), sr)
    add(x, thud(sr, 80, 30, 0.2, 0.7), at, 1.0, sr)
    add(x, burst(sr, rng, 0.6, 1400, 0.6, "band", 0.12), at, 0.8, sr)
    for _ in range(10):
        add(x, burst(sr, rng, 0.08, 800 + 2400 * rng.random(), 3.0, "band", 0.02), at + rng.uniform(0.02, 0.6), 0.35, sr)
    for k in range(3):
        add(x, pluck(sr, midi_hz(rng.choice((40, 47, 51, 58))), 1.6, rng, 0.8, 0.9965), at + 0.1 + 0.12 * k, 0.25, sr)
    return reverb(soft_clip(x, 1.5), sr, size=0.7, damp=0.45, wet=0.15, tail=0.6)


def pie(sr, rng):
    """A cream pie in the face: a wet low splat and a spatter of small blobs."""
    x = buf(sr, 0.8)
    sp = onepole_lp(noise(int(sr * 0.35), rng), sr, 900)
    add(x, exp_decay(sp, sr, 0.06, 0.004), 0, 1.4, sr)
    add(x, thud(sr, 180, 60, 0.05, 0.2), 0, 0.6, sr)
    for _ in range(9):
        f = rng.uniform(300, 900)
        add(x, tone(sr, f, 0.06, decay=0.015, glide=f * 0.5), rng.uniform(0.04, 0.35), rng.uniform(0.1, 0.25), sr)
    return reverb(x, sr, size=0.3, damp=0.6, wet=0.06, tail=0.2)


def snap(sr, rng):
    """His finger snap: a dry, bright crack and a short room (it changes the channel)."""
    x = buf(sr, 0.3)
    add(x, burst(sr, rng, 0.02, 1500, 0.8, "high", 0.0015), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, 2600, 4.0, "band", 0.01), 0.0, 0.8, sr)
    add(x, burst(sr, rng, 0.05, 1000, 2.0, "band", 0.014), 0.0006, 0.5, sr)
    add(x, thud(sr, 260, 170, 0.007, 0.04), 0.0, 0.2, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.14, tail=0.4, predelay=0.012)


def welcome(sr, rng):
    """"Welcome to TV Land!": a cheesy fanfare sting -- da-da-da-DAAA in brass, a glockenspiel run, a cymbal swell."""
    T = 2.8
    x = buf(sr, T)
    for at, notes, dur in ((0.0, (67, 72), 0.14), (0.16, (67, 72), 0.14), (0.32, (67, 72), 0.14), (0.5, (72, 76, 79, 84), 1.6)):
        for m in notes:
            add(x, trumpet(sr, rng, m, dur, vib=6.0), at, 0.3, sr)
    for i, m in enumerate((84, 88, 91, 96, 100)):
        f = midi_hz(m)
        add(x, partials(sr, 0.8, [(f, 0.4, 0.4), (f * 2.76, 0.15, 0.15)], attack=0.001), 0.5 + i * 0.05, 0.4, sr)
    cym = svf(noise(int(sr * 2.0), rng), sr, 6500, 0.5, "high")
    add(x, env_apply(cym, sr, lambda t: (t / 0.5) ** 2 if t < 0.5 else math.exp(-(t - 0.5) / 0.4)), 0.05, 0.25, sr)
    add(x, thud(sr, 90, 45, 0.15, 0.5), 0.5, 0.6, sr)
    add(x, sparkle(sr, rng, 14, 0.5, 2.0, T, 3500, 8000), 0, 0.4, sr)
    return hall(x, sr, 0.18, 0.8)


def ambient(sr, rng):
    """Gabriel chuckles to himself: "heh-heh-heh", sly and pleased."""
    x = buf(sr, 1.0)
    for i, (t, f) in enumerate(((0.0, 172), (0.17, 164), (0.33, 156), (0.5, 150))):
        d = 0.13
        br = onepole_lp(noise(int(sr * d), rng), sr, 2000)
        br = env_apply(br, sr, lambda tt: min(1.0, tt / 0.006) * math.exp(-tt / 0.025))
        add(x, br, t, 0.3 * (1 - i * 0.15), sr)
        v = voiced(sr, rng, d, lambda tt, f=f: f * (1 + 0.1 * (1 - tt / d)), EH, 0.7,
                   lambda tt: min(1.0, tt / 0.012) * math.exp(-tt / 0.05))
        add(x, v, t + 0.005, 1.0 - i * 0.15, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.08, tail=0.25)


def hurt(sr, rng):
    """A blow: a sharp "oof!"."""
    T = 0.42
    x = buf(sr, T + 0.1)
    br = svf(noise(int(sr * T), rng), sr, 1400, 0.8, "band")
    add(x, env_apply(br, sr, lambda t: min(1.0, t / 0.01) * math.exp(-t / 0.09)), 0, 0.35, sr)
    v = voiced(sr, rng, T, lambda t: 205 * (0.7 ** min(1.0, t / T)), UH, 0.6,
               lambda t: min(1.0, t / 0.02) * math.exp(-max(0.0, t - 0.05) / 0.1))
    add(x, v, 0, 1.0, sr)
    return reverb(x, sr, size=0.35, damp=0.5, wet=0.06, tail=0.2)


def death(sr, rng):
    """The Trickster falls: a cry rising into light, a golden chord that swells and shatters, wings of air, silence."""
    T = 4.2
    x = buf(sr, T)
    cry = voiced(sr, rng, 1.4, lambda t: 180 * (1.8 ** min(1.0, t / 1.0)), AH, 0.5,
                 lambda t: min(1.0, t / 0.08) * max(0.0, 1 - t / 1.4))
    add(x, cry, 0, 0.8, sr)
    chord = buf(sr, 3.0)
    for m in (62, 66, 69, 74, 78):
        for d in (-3, 3):
            s = saw(sr, 3.0, midi_hz(m), d, rng.random(), (5.0, 8.0))
            add(chord, s, 0, 0.08, sr)
    chord = svf(chord, sr, lambda t: 600 + 5000 * min(1.0, t / 1.2), 0.7, "low")
    add(x, env_apply(chord, sr, lambda t: min(1.0, t / 1.0) * (1 if t < 1.6 else max(0.0, 1 - (t - 1.6) / 1.2))), 0.6, 0.9, sr)
    add(x, burst(sr, rng, 1.2, 3200, 0.5, "high", 0.35), 2.2, 0.6, sr)
    add(x, thud(sr, 70, 30, 0.3, 1.0), 2.2, 0.8, sr)
    add(x, sparkle(sr, rng, 30, 2.2, 3.8, T, 3000, 9000, rise=False), 0, 0.5, sr)
    return hall(soft_clip(x, 1.3), sr, 0.3, 1.6)


# =====================================================================================================
# The jingles (music rate, loopable)
# =====================================================================================================

class Kit:
    """Cached instrument notes for the jingles."""

    def __init__(self, sr, rng):
        self.sr, self.rng, self.c = sr, rng, {}

    def _get(self, key, fn):
        if key not in self.c:
            self.c[key] = fn()
        return self.c[key]

    def epiano(self, m, d):
        f = midi_hz(m)
        return self._get(("ep", m, d), lambda: partials(self.sr, d + 0.4, [(f, 0.55, d * 0.7 + 0.2), (f * 2, 0.2, 0.25),
                                                                          (f * 3.0, 0.1, 0.12), (f * 7.1, 0.05, 0.04)], 0.002))

    def bass(self, m, d):
        f = midi_hz(m)
        sr = self.sr
        return self._get(("bs", m, d), lambda: env_apply([a + 0.3 * b for a, b in zip(tone(sr, f, d), tone(sr, 2 * f, d))], sr,
                                                         _env(0.008, d * 0.6, d * 0.4 - 0.008)))

    def kick(self):
        return self._get("kick", lambda: thud(self.sr, 130, 42, 0.09, 0.3))

    def snare(self):
        sr, rng = self.sr, self.rng
        return self._get("snare", lambda: [a + b for a, b in zip(burst(sr, rng, 0.2, 1900, 0.6, "band", 0.045),
                                                                 tone(sr, 190, 0.2, decay=0.04))])

    def hat(self, open_=False):
        return self._get(("hat", open_), lambda: burst(self.sr, self.rng, 0.15 if open_ else 0.05, 7000, 0.7, "high",
                                                       0.05 if open_ else 0.012))

    def clap(self):
        sr, rng = self.sr, self.rng

        def mk():
            x = buf(sr, 0.2)
            for k in range(3):
                add(x, burst(sr, rng, 0.06, 1500, 1.2, "band", 0.008), k * 0.011, 0.7, sr)
            return x
        return self._get("clap", mk)

    def sq(self, m, d, duty=0.25):
        f = midi_hz(m)
        sr = self.sr
        return self._get(("sq", m, d, duty), lambda: env_apply(svf(square(sr, f, d, duty), sr, 3800, 0.8, "low"), sr,
                                                               _env(0.004, d * 0.5, d * 0.5 - 0.004)))

    def lead(self, m, d):
        """A soft synth-sax lead: two detuned saws, filtered, a little vibrato."""
        f = midi_hz(m)
        sr = self.sr

        def mk():
            a = saw(sr, d, f, -6, 0.1, (5.5, 10.0))
            b = saw(sr, d, f, 6, 0.6, (5.0, 10.0))
            return env_apply(svf([u + v for u, v in zip(a, b)], sr, 2000, 1.2, "low"), sr, _env(0.03, d * 0.7, d * 0.3 - 0.03))
        return self._get(("ld", m, d), mk)

    def strings(self, m, d):
        f = midi_hz(m)
        sr = self.sr

        def mk():
            a = saw(sr, d, f, -9, 0.0, (4.8, 7.0))
            b = saw(sr, d, f, 8, 0.5, (5.3, 7.0))
            x = svf([u + v for u, v in zip(a, b)], sr, 1700, 0.8, "low")
            return env_apply(x, sr, _env(min(0.5, d * 0.3), d * 0.5, d * 0.2 + 0.0001))
        return self._get(("st", m, d), mk)

    def pizz(self, m, d=0.4):
        f = midi_hz(m)
        return self._get(("pz", m), lambda: env_apply(svf(saw(self.sr, d, f, 0, 0.0), self.sr, 900, 1.0, "low"), self.sr,
                                                      lambda t: math.exp(-t / 0.09)))

    def uke(self, m):
        return self._get(("uk", m), lambda: pluck(self.sr, midi_hz(m), 0.9, self.rng, 0.7, 0.994))

    def glock(self, m):
        f = midi_hz(m)
        return self._get(("gl", m), lambda: partials(self.sr, 0.9, [(f, 0.5, 0.5), (f * 2.76, 0.2, 0.18), (f * 5.4, 0.07, 0.08)], 0.001))

    def whistle(self, m, d):
        f = midi_hz(m)
        sr = self.sr

        def mk():
            ph, out = 0.0, []
            for i in range(int(sr * d)):
                t = i / sr
                ph += TAU * f * (1 + 0.006 * math.sin(TAU * 6.0 * t)) / sr
                out.append(math.sin(ph))
            br = svf(noise(int(sr * d), self.rng), sr, f, 6.0, "band")
            return env_apply([a + 0.15 * b for a, b in zip(out, br)], sr, _env(0.03, d * 0.75, d * 0.25 - 0.03))
        return self._get(("wh", m, d), mk)

    def timpani(self, m):
        f = midi_hz(m)
        return self._get(("tp", m), lambda: [a + b for a, b in zip(partials(self.sr, 1.6, [(f, 0.8, 0.6), (f * 1.5, 0.3, 0.3),
                                                                                         (f * 1.98, 0.2, 0.2)], 0.003),
                                                                  burst(self.sr, self.rng, 1.6, 300, 0.7, "low", 0.05))])


def _melody(mix, sr, kit, inst, notes, t0, beat, gain):
    t = t0
    for m, beats in notes:
        if m is not None:
            add(mix, inst(m, beats * beat * 0.92), t, gain, sr)
        t += beats * beat


def jingle_sitcom(sr, rng):
    """A cheery 80s sitcom theme in C: electric piano chords, a walking bass, a light backbeat, a synth-sax melody.
    16 bars at 120 bpm (32 s), looping."""
    bpm, bars = 120, 16
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 2.5)
    kit = Kit(sr, rng)
    C, Am, F, G, Em, Dm = (48, [60, 64, 67]), (45, [60, 64, 69]), (41, [60, 65, 69]), (43, [59, 62, 67]), (40, [59, 64, 67]), (38, [60, 62, 65])
    prog = [C, Am, F, G, C, Am, F, G, F, G, Em, Am, Dm, G, C, G]
    for b, (root, ch) in enumerate(prog):
        t = b * 4 * beat
        for k, off in enumerate((0, 1.5, 2, 3.5)):
            for m in ch:
                add(mix, kit.epiano(m, 0.45), t + off * beat, 0.11 if k == 0 else 0.07, sr)
        for k, step in enumerate((0, 7, 12, 7)):
            add(mix, kit.bass(root + step, beat * 0.9), t + k * beat, 0.35, sr)
        for k in range(4):
            add(mix, kit.kick() if k in (0, 2) else kit.snare(), t + k * beat, 0.5 if k in (0, 2) else 0.32, sr)
            add(mix, kit.hat(), t + k * beat + beat / 2, 0.12, sr)
            add(mix, kit.hat(), t + k * beat, 0.08, sr)
    A = [(72, 1), (74, 0.5), (76, 1.5), (79, 1), (76, 1), (74, 1), (72, 1), (69, 2), (72, 1), (None, 1),
         (77, 1), (76, 0.5), (74, 1.5), (72, 1), (74, 1), (76, 2), (None, 2)]
    B = [(72, 1), (74, 0.5), (76, 1.5), (79, 1), (81, 1.5), (79, 0.5), (76, 1), (79, 2), (76, 1), (None, 1),
         (77, 1), (79, 1), (81, 1), (83, 1), (84, 3), (None, 1)]
    _melody(mix, sr, kit, kit.lead, A, 0, beat, 0.11)
    _melody(mix, sr, kit, kit.lead, B, 8 * 4 * beat, beat, 0.11)
    out = reverb(mix, sr, size=0.6, damp=0.45, wet=0.12, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.4)


def jingle_game_show(sr, rng):
    """A frantic synth game-show theme: square-wave arpeggios racing, a pumping octave bass, stabs and fast hats,
    a key change up a tone halfway. 16 bars at 160 bpm (24 s), looping."""
    bpm, bars = 160, 16
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 2.0)
    kit = Kit(sr, rng)
    prog = [(57, (0, 4, 7)), (53, (0, 4, 7)), (55, (0, 4, 7)), (52, (0, 3, 7)),
            (57, (0, 4, 7)), (53, (0, 4, 7)), (50, (0, 3, 7)), (52, (0, 4, 7))]
    for b in range(bars):
        root, iv = prog[b % 8]
        up = 2 if b >= 8 else 0
        root += up
        t = b * 4 * beat
        arp = [root + 12 + i for i in iv] + [root + 24]
        for k in range(16):
            m = arp[(k * (1 if b % 2 == 0 else 3)) % 4]
            add(mix, kit.sq(m, beat / 4 * 0.9, 0.25), t + k * beat / 4, 0.07, sr)
        for k in range(8):
            add(mix, kit.sq(root - 12 + (12 if k % 2 else 0), beat / 2 * 0.8, 0.5), t + k * beat / 2, 0.12, sr)
        for k in range(4):
            add(mix, kit.kick(), t + k * beat, 0.45, sr)
            if k in (1, 3):
                add(mix, kit.clap(), t + k * beat, 0.35, sr)
        for k in range(8):
            add(mix, kit.hat(k % 4 == 3), t + k * beat / 2 + beat / 4, 0.1, sr)
        if b % 2 == 1:
            for i in iv:
                add(mix, kit.sq(root + 24 + i, beat * 0.3, 0.5), t + 3.5 * beat, 0.08, sr)
    tune = [(81, 0.5), (84, 0.5), (88, 1), (86, 0.5), (84, 0.5), (81, 1), (79, 0.5), (81, 0.5), (84, 2),
            (77, 0.5), (81, 0.5), (84, 1), (86, 1), (88, 1), (84, 4)]
    _melody(mix, sr, kit, lambda m, d: kit.sq(m, d, 0.5), tune, 4 * 4 * beat, beat, 0.09)
    _melody(mix, sr, kit, lambda m, d: kit.sq(m, d, 0.5), [(m + 2 if m else None, d) for m, d in tune], 12 * 4 * beat, beat, 0.09)
    out = reverb(mix, sr, size=0.4, damp=0.5, wet=0.08, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.6)


def jingle_hospital(sr, rng):
    """A dramatic medical-drama theme in D minor: swelling strings, a driving low pulse, timpani, a lonely high piano
    line that sounds a little like a monitor. 8 bars at 72 bpm (26.7 s), looping."""
    bpm, bars = 72, 8
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 3.0)
    kit = Kit(sr, rng)
    prog = [(38, [62, 65, 69]), (34, [62, 65, 70]), (36, [60, 64, 67]), (33, [61, 64, 69]),
            (38, [62, 65, 69]), (41, [60, 65, 69]), (43, [62, 67, 70]), (33, [61, 64, 69])]
    for b, (root, ch) in enumerate(prog):
        t = b * 4 * beat
        for m in ch:
            add(mix, kit.strings(m, 4 * beat + 0.3), t, 0.06, sr)
        add(mix, kit.strings(root + 12, 4 * beat + 0.3), t, 0.07, sr)
        for k in range(8):
            add(mix, kit.pizz(root + (12 if k % 4 == 2 else 0)), t + k * beat / 2, 0.22 if k % 2 == 0 else 0.15, sr)
        if b % 2 == 0:
            add(mix, kit.timpani(root), t, 0.45, sr)
        if b in (3, 7):
            for k in range(4):
                add(mix, kit.timpani(root), t + 3 * beat + k * beat / 4, 0.25 + 0.06 * k, sr)
    line = [(74, 3), (77, 1), (76, 2), (74, 2), (72, 3), (70, 1), (69, 4),
            (74, 3), (77, 1), (81, 2), (79, 2), (77, 2), (76, 2), (73, 4)]
    _melody(mix, sr, kit, kit.epiano, line, 0, beat, 0.12)
    out = reverb(mix, sr, size=0.9, damp=0.4, wet=0.2, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.3)


def jingle_commercial(sr, rng):
    """A bright, smarmy ad jingle in G: ukulele strums, glockenspiel, a whistled hook, hand claps, a bouncy bass.
    12 bars at 112 bpm (25.7 s), looping."""
    bpm, bars = 112, 12
    beat = 60.0 / bpm
    L = bars * 4 * beat
    mix = buf(sr, L + 2.0)
    kit = Kit(sr, rng)
    G, Em, C, D = (43, [55, 59, 62, 67]), (40, [55, 59, 64, 67]), (36, [55, 60, 64, 67]), (38, [54, 57, 62, 66])
    prog = [G, Em, C, D, G, Em, C, D, C, D, G, G]
    for b, (root, ch) in enumerate(prog):
        t = b * 4 * beat
        for k, off in enumerate((0, 1, 1.5, 2.5, 3)):
            for i, m in enumerate(ch):
                add(mix, kit.uke(m), t + off * beat + i * 0.012, 0.09 if k == 0 else 0.06, sr)
        for k, step in enumerate((0, 12, 7, 12)):
            add(mix, kit.bass(root + step, beat * 0.5), t + k * beat, 0.32, sr)
        for k in range(4):
            if k in (1, 3):
                add(mix, kit.clap(), t + k * beat, 0.3, sr)
            add(mix, kit.kick() if k in (0, 2) else kit.hat(), t + k * beat, 0.35 if k in (0, 2) else 0.08, sr)
        add(mix, kit.glock(ch[-1] + 12), t + 3.5 * beat, 0.2, sr)
    hook = [(74, 1), (79, 0.5), (79, 0.5), (78, 1), (76, 1), (74, 2), (71, 1), (74, 1),
            (76, 1), (76, 0.5), (74, 0.5), (72, 1), (71, 1), (69, 4),
            (74, 1), (79, 0.5), (79, 0.5), (81, 1), (83, 1), (84, 2), (83, 1), (81, 1),
            (79, 1), (76, 1), (78, 1), (81, 1), (79, 4)]
    _melody(mix, sr, kit, kit.whistle, hook, 0, beat, 0.14)
    for i, m in enumerate((79, 83, 86, 91)):
        add(mix, kit.glock(m), 11 * 4 * beat + i * beat / 2, 0.22, sr)
    out = reverb(mix, sr, size=0.5, damp=0.4, wet=0.12, tail=0.0)
    return soft_clip(normalize(loopify(out, sr, L), 0.0), 1.4)


SOUNDS = {
    "laugh_track": (laugh_track, -1.5), "applause": (applause, -2.0), "buzzer": (buzzer, -2.0), "ding": (ding, -2.0),
    "static": (static, -4.0), "monitor_beep": (monitor_beep, -3.0), "defib": (defib, -1.0), "piano": (piano, -1.0),
    "pie": (pie, -1.5), "snap": (snap, -1.0), "welcome": (welcome, -1.0), "ambient": (ambient, -3.0), "hurt": (hurt, -2.0),
    "death": (death, -1.0),
}
JINGLES = {
    "jingle_sitcom": (jingle_sitcom, -3.0), "jingle_game_show": (jingle_game_show, -3.0),
    "jingle_hospital": (jingle_hospital, -3.0), "jingle_commercial": (jingle_commercial, -3.0),
}
