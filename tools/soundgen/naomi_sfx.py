"""v0.18 Naomi: sounds/naomi/<id>.ogg (HeavenAssets.SOUNDS_NAOMI).

Same conventions as sfx.py / raphael_sfx.py: (sr, rng) -> float list, finished and encoded by generate.py (HeavenAssetData
points supernaturalcraft:naomi.<id> at them). Clinical where Heaven is warm: a dental drill's whine, leather straps and steel
buckles, a monitor's tones, white noise and tinnitus for the wipe. Her voice is a composed woman's, never raised.
"""

import math

from michael_sfx import hall, whoosh
from sfx import HUM, UH, bell_voice, burst, choir, sparkle, thud, voiced
from synth import add, buf, env_apply, midi_hz, noise, onepole_lp, partials, reverb, smooth_random, soft_clip, svf, tone

EE = [(400, 3.0, 1.0), (2300, 6.0, 0.3), (3000, 7.0, 0.12)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def room(x, sr, wet=0.1, tail=0.35):
    """A tiled clinic room: short, bright."""
    return reverb(x, sr, size=0.45, damp=0.25, wet=wet, tail=tail, predelay=0.008)


def buckle(sr, rng, pitch=1.0):
    """A steel buckle's clink: a click and a short inharmonic ring."""
    x = burst(sr, rng, 0.02, 4200 * pitch, 1.0, "high", 0.0015)
    add(x, partials(sr, 0.3, [(2350 * pitch, 0.25, 0.06), (3870 * pitch, 0.18, 0.04), (5610 * pitch, 0.1, 0.025)], 0.0005))
    return x


def creak(sr, rng, T, f0=180):
    """Leather pulled tight: a stick-slip buzz, its rate wandering."""
    rate = smooth_random(sr, T, rng, 8, 0.6, 1.4)
    x = [0.0] * int(sr * T)
    ph = 0.0
    for i in range(len(x)):
        ph += f0 * rate(i / sr) / sr
        if ph >= 1.0:
            ph -= 1.0
            x[i] = rng.uniform(0.5, 1.0)
    x = svf(x, sr, 900, 1.4, "band")
    return env_apply(x, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 0.7)


def whine(sr, T, f0, f1, rise=0.25):
    """A drill motor: a sine climbing from f0 to f1 and holding, with its harmonics and a fast wobble."""
    n = int(sr * T)
    out = [0.0] * n
    ph = 0.0
    for i in range(n):
        t = i / sr
        k = min(1.0, t / rise)
        f = f0 + (f1 - f0) * (1 - (1 - k) ** 2)
        f *= 1 + 0.004 * math.sin(2 * math.pi * 47 * t)
        ph += 2 * math.pi * f / sr
        out[i] = math.sin(ph) + 0.35 * math.sin(2 * ph + 0.3) + 0.12 * math.sin(3 * ph)
    return out


# --- her voice ------------------------------------------------------------------------------------------------------

def ambient(sr, rng):
    """She hums to herself as she works: three calm notes, a breath between."""
    x = buf(sr, 2.4)
    for t, m, d in ((0.0, 64, 0.45), (0.5, 62, 0.35), (0.9, 67, 0.7)):
        f = midi_hz(m)
        v = voiced(sr, rng, d, lambda tt, f=f: f * (1 + 0.006 * math.sin(tt * 30)), HUM, 0.3, _env(0.05, d - 0.2, 0.15))
        add(x, v, t, 0.8, sr)
    add(x, onepole_lp(env_apply(noise(int(sr * 0.4), rng), sr, _env(0.1, 0.1, 0.2)), sr, 1400), 1.75, 0.08, sr)
    return room(x, sr, 0.08, 0.3)


def hurt(sr, rng):
    """A sharp, controlled intake and a clipped "hn"."""
    x = buf(sr, 0.7)
    br = svf(noise(int(sr * 0.12), rng), sr, 2200, 0.9, "band")
    add(x, env_apply(br, sr, lambda t: min(1.0, t / 0.02) * math.exp(-t / 0.05)), 0.0, 0.4, sr)
    v = voiced(sr, rng, 0.35, lambda t: 270 * (0.8 ** (t / 0.35)), UH, 0.5, _env(0.015, 0.1, 0.22))
    add(x, v, 0.05, 1.0, sr)
    return room(x, sr, 0.08, 0.25)


def death(sr, rng):
    """She kneels; her voice climbs into a thin cry as the light pours from her eyes and mouth; a choir lifts it away."""
    T = 5.0
    x = buf(sr, T)
    v = voiced(sr, rng, 2.2, lambda t: 230 * (2.2 ** min(1.0, t / 1.8)), EE, 0.5, _env(0.15, 1.5, 0.55))
    add(x, v, 0.2, 0.9, sr)
    c = choir(sr, rng, (69, 76, 81), 3.2, "ah", voices=3, detune=10)
    c = svf(c, sr, lambda t: 500 * (12 ** min(1.0, t / 2.0)), 0.8, "low")
    add(x, env_apply(c, sr, _env(1.2, 1.0, 1.0)), 0.8, 1.0, sr)
    add(x, whoosh(sr, rng, 1.8, 300, 4200, 0.9, 0.7), 1.0, 0.6, sr)
    add(x, sparkle(sr, rng, 28, 1.6, 4.4, T, 2600, 9000), 0, 0.7, sr)
    add(x, thud(sr, 90, 40, 0.2, 0.6), 0.0, 0.5, sr)
    return hall(soft_clip(x, 1.2), sr, 0.25, 1.6)


# --- the chair and the drill ---------------------------------------------------------------------------------------

def drill(sr, rng):
    """The dental drill: the motor spinning up into a thin scream, the bit chattering, spinning down."""
    T = 1.9
    x = buf(sr, T)
    w = whine(sr, 1.6, 600, 3100, 0.3)
    w = env_apply(w, sr, lambda t: min(1.0, t / 0.05) * (1.0 if t < 1.25 else max(0.0, 1 - (t - 1.25) / 0.35)))
    add(x, w, 0.0, 0.28, sr)
    add(x, svf(noise(int(sr * 1.5), rng), sr, 6200, 2.0, "band"), 0.1, 0.12, sr)
    chatter = env_apply(svf(noise(int(sr * 0.6), rng), sr, 2400, 4.0, "band"), sr,
                        lambda t: (0.5 + 0.5 * math.sin(t * 2 * math.pi * 38)) * math.sin(math.pi * t / 0.6))
    add(x, chatter, 0.6, 0.5, sr)
    add(x, thud(sr, 160, 90, 0.04, 0.12), 0.0, 0.3, sr)
    return room(soft_clip(x, 1.2), sr, 0.08, 0.3)


def chair_strap(sr, rng):
    """Straps thrown over and pulled tight: a leather slap, the creak, two buckles snapping shut."""
    x = buf(sr, 1.1)
    add(x, burst(sr, rng, 0.08, 1300, 0.7, "band", 0.02), 0.0, 1.0, sr)
    add(x, thud(sr, 140, 80, 0.03, 0.1), 0.0, 0.5, sr)
    add(x, creak(sr, rng, 0.35, 160), 0.08, 0.5, sr)
    add(x, buckle(sr, rng, 1.0), 0.42, 0.8, sr)
    add(x, buckle(sr, rng, 0.92), 0.6, 0.7, sr)
    return room(soft_clip(x, 1.6), sr, 0.1, 0.3)


def chair_struggle(sr, rng):
    """Pulling against the straps: leather groaning, the buckles rattling in their frames."""
    x = buf(sr, 0.85)
    add(x, creak(sr, rng, 0.7, 130 + rng.uniform(-20, 20)), 0.0, 0.9, sr)
    for t in (0.12, 0.31, 0.5):
        add(x, buckle(sr, rng, rng.uniform(0.85, 1.1)), t + rng.uniform(0, 0.04), 0.35, sr)
    add(x, thud(sr, 110, 70, 0.05, 0.15), 0.05, 0.4, sr)
    return room(x, sr, 0.08, 0.25)


def chair_free(sr, rng):
    """A strap tears loose: the snap, buckles clattering to the floor, a breath of grace."""
    x = buf(sr, 1.5)
    add(x, burst(sr, rng, 0.05, 2400, 0.6, "high", 0.006), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.12, 900, 0.8, "band", 0.03), 0.0, 0.7, sr)
    for i, t in enumerate((0.18, 0.27, 0.33, 0.41)):
        add(x, buckle(sr, rng, 1.05 - i * 0.04), t, 0.5 - i * 0.08, sr)
    add(x, sparkle(sr, rng, 8, 0.05, 0.8, 1.5, 3000, 7000), 0, 0.5, sr)
    return room(soft_clip(x, 2.0), sr, 0.12, 0.35)


def wipe(sr, rng):
    """The wipe: white noise rushing up to a blank roar, a high tone ringing in the silence after it."""
    T = 2.6
    x = buf(sr, T)
    wn = svf(noise(int(sr * 1.4), rng), sr, lambda t: 800 * (8 ** min(1.0, t / 1.0)), 0.5, "low")
    add(x, env_apply(wn, sr, lambda t: (t / 1.0) ** 2 if t < 1.0 else max(0.0, 1 - (t - 1.0) / 0.4)), 0.0, 0.8, sr)
    ring = tone(sr, 6200, 1.6, attack=0.02)
    add(x, env_apply(ring, sr, lambda t: math.exp(-t / 0.7)), 1.0, 0.12, sr)
    add(x, bell_voice(sr, midi_hz(88), 1.4, bright=0.3, beat=0.5), 1.0, 0.15, sr)
    return room(soft_clip(x, 1.1), sr, 0.15, 0.4)


def guards(sr, rng):
    """An earpiece radio: a squelch, a clipped burst of chatter, a click; boots on tile."""
    x = buf(sr, 1.6)
    add(x, burst(sr, rng, 0.06, 2600, 3.0, "band", 0.03), 0.0, 0.6, sr)
    chat = svf(noise(int(sr * 0.45), rng), sr, 1600, 1.4, "band")
    chat = env_apply(chat, sr, lambda t: (0.5 + 0.5 * math.sin(t * 2 * math.pi * 7 + math.sin(t * 23))) * math.sin(math.pi * t / 0.45))
    add(x, soft_clip(chat, 3.0), 0.08, 0.35, sr)
    add(x, burst(sr, rng, 0.02, 3500, 1.0, "high", 0.002), 0.56, 0.5, sr)
    for t in (0.8, 1.15):
        add(x, burst(sr, rng, 0.06, 700, 1.2, "band", 0.015), t, 0.6, sr)
        add(x, thud(sr, 120, 80, 0.02, 0.08), t, 0.35, sr)
    return room(x, sr, 0.12, 0.35)


def console(sr, rng):
    """A key press and the console's two-tone acknowledgement."""
    x = buf(sr, 0.7)
    add(x, burst(sr, rng, 0.02, 3000, 1.0, "band", 0.003), 0.0, 0.5, sr)
    for t, f in ((0.04, 1318.5), (0.16, 1760.0)):
        b = tone(sr, f, 0.12)
        add(x, env_apply(b, sr, _env(0.004, 0.08, 0.03)), t, 0.4, sr)
    return room(x, sr, 0.08, 0.2)


def test_bell(sr, rng):
    """The test begins: a clean two-note chime, like a hospital's paging bell."""
    x = buf(sr, 2.0)
    add(x, bell_voice(sr, midi_hz(76), 1.6, bright=0.4, beat=0.6), 0.0, 0.5, sr)
    add(x, bell_voice(sr, midi_hz(72), 1.6, bright=0.4, beat=0.6), 0.35, 0.5, sr)
    return room(x, sr, 0.15, 0.4)


SOUNDS = {
    "ambient": (ambient, -4.0), "hurt": (hurt, -2.0), "death": (death, -1.0), "drill": (drill, -4.0),
    "chair_strap": (chair_strap, -1.5), "chair_struggle": (chair_struggle, -3.0), "chair_free": (chair_free, -1.5),
    "wipe": (wipe, -1.5), "guards": (guards, -2.0), "console": (console, -6.0), "test_bell": (test_bell, -3.0),
}
