"""Tiny DSP toolkit for the sound generator (pure stdlib, mono, float lists in -1..1).

Everything takes an explicit sample rate and, where randomness is involved, an explicit
random.Random, so every sound is byte-identical between runs.

    x = buf(sr, 1.0)                       # one second of silence
    add(x, tone(sr, 440, 0.5, decay=0.2))  # mix a decaying sine in at t = 0
    x = svf(x, sr, 1200, q=2, mode="band") # zero-delay state-variable filter (fc may vary with time)
"""

import math
import struct
import wave

TAU = 2 * math.pi


# --- buffers ----------------------------------------------------------------------------------

def buf(sr, seconds):
    return [0.0] * max(1, int(sr * seconds))


def add(dst, src, at=0.0, gain=1.0, sr=None):
    """Mixes src into dst starting at `at` seconds (or samples when sr is None)."""
    start = int(at * sr) if sr else int(at)
    end = min(len(dst), start + len(src))
    if start < 0:
        src = src[-start:]
        start = 0
    for i in range(start, end):
        dst[i] += src[i - start] * gain
    return dst


def scale(x, g):
    return [v * g for v in x]


def concat(*parts):
    out = []
    for p in parts:
        out.extend(p)
    return out


def reverse(x):
    return x[::-1]


def pad(x, sr, seconds):
    return x + [0.0] * int(sr * seconds)


# --- sources ----------------------------------------------------------------------------------

def noise(n, rng):
    u = rng.uniform
    return [u(-1.0, 1.0) for _ in range(n)]


def tone(sr, freq, seconds, decay=None, phase=0.0, attack=0.0, glide=None):
    """A sine. `decay` = time constant of an exponential fade (s); `glide` = end frequency (exponential sweep)."""
    n = int(sr * seconds)
    out = [0.0] * n
    ph = phase
    env = 1.0
    d = math.exp(-1.0 / (decay * sr)) if decay else 1.0
    a_n = int(attack * sr)
    if glide:
        ratio = (glide / freq) ** (1.0 / max(1, n))
    else:
        ratio = 1.0
    f = freq
    for i in range(n):
        g = env * (i / a_n if i < a_n else 1.0)
        out[i] = math.sin(ph) * g
        ph += TAU * f / sr
        f *= ratio
        env *= d
    return out


def partials(sr, seconds, spec, attack=0.002):
    """Additive bell/piano voice: spec = [(freq, amp, decay_s), ...]; each partial decays on its own."""
    n = int(sr * seconds)
    out = [0.0] * n
    a_n = max(1, int(attack * sr))
    sin = math.sin
    for freq, amp, decay in spec:
        if freq >= sr / 2 - 200:
            continue
        w = TAU * freq / sr
        d = math.exp(-1.0 / (decay * sr))
        env = amp
        limit = min(n, int(decay * sr * 9))
        for i in range(limit):
            out[i] += sin(w * i) * env
            env *= d
    for i in range(min(a_n, n)):
        out[i] *= i / a_n
    return out


def saw(sr, seconds, freq, detune_cents=0.0, phase=0.0, vibrato=(0.0, 0.0)):
    """Naive sawtooth (soften it with a lowpass); optional vibrato (rate Hz, depth cents)."""
    n = int(sr * seconds)
    out = [0.0] * n
    f0 = freq * 2 ** (detune_cents / 1200)
    p = phase % 1.0
    rate, depth = vibrato
    for i in range(n):
        f = f0 * (2 ** (depth * math.sin(TAU * rate * i / sr) / 1200)) if depth else f0
        out[i] = 2 * p - 1
        p += f / sr
        if p >= 1.0:
            p -= 1.0
    return out


def glottal(sr, f0_curve, seconds, jitter_rng=None):
    """A voiced source: a smooth glottal pulse train following f0_curve(t) (Hz)."""
    n = int(sr * seconds)
    out = [0.0] * n
    p = 0.0
    prev = 0.0
    for i in range(n):
        f = f0_curve(i / sr)
        if jitter_rng:
            f *= 1 + jitter_rng.uniform(-0.004, 0.004)
        # Rosenberg-like opening/closing shape, then differentiated (the "flow derivative").
        if p < 0.45:
            g = 0.5 * (1 - math.cos(math.pi * p / 0.45))
        elif p < 0.62:
            g = math.cos(math.pi * (p - 0.45) / 0.34)
        else:
            g = 0.0
        out[i] = g - prev
        prev = g
        p += f / sr
        if p >= 1.0:
            p -= 1.0
    return out


def pluck(sr, freq, seconds, rng, brightness=0.5, damping=0.996):
    """Karplus-Strong string: a noise burst through a tuned, damped delay loop."""
    n = int(sr * seconds)
    period = sr / freq
    L = max(2, int(period))
    frac = period - L
    line = [rng.uniform(-1, 1) for _ in range(L)]
    # Soften the excitation (darker pluck for low brightness).
    s = 0.0
    for i in range(L):
        s = s + (line[i] - s) * (0.2 + 0.8 * brightness)
        line[i] = s
    out = [0.0] * n
    idx = 0
    last = 0.0
    for i in range(n):
        a = line[idx]
        b = line[(idx + 1) % L]
        v = a + (b - a) * frac
        nv = damping * 0.5 * (v + last)
        last = v
        out[i] = a
        line[idx] = nv
        idx = (idx + 1) % L
    return out


# --- envelopes --------------------------------------------------------------------------------

def env_apply(x, sr, curve):
    """Multiplies x by curve(t) (t in seconds), sampled every 32 samples and interpolated."""
    n = len(x)
    out = [0.0] * n
    step = 32
    g0 = curve(0.0)
    for s in range(0, n, step):
        g1 = curve(min(n, s + step) / sr)
        e = min(n, s + step)
        for i in range(s, e):
            out[i] = x[i] * (g0 + (g1 - g0) * (i - s) / step)
        g0 = g1
    return out


def adsr(a, d, s, r, total):
    """Envelope function of t: attack, decay to sustain, release over the last r seconds of `total`."""
    def f(t):
        if t < a:
            v = t / a if a else 1.0
        elif t < a + d:
            v = 1 - (1 - s) * (t - a) / d
        else:
            v = s
        if t > total - r:
            v *= max(0.0, (total - t) / r) if r else 0.0
        return v
    return f


def exp_decay(x, sr, tau, attack=0.0):
    n = len(x)
    d = math.exp(-1.0 / (tau * sr))
    a_n = int(attack * sr)
    env = 1.0
    out = [0.0] * n
    for i in range(n):
        g = env * (i / a_n if i < a_n else 1.0)
        out[i] = x[i] * g
        if i >= a_n:
            env *= d
    return out


def smooth_random(sr, seconds, rng, rate, lo=0.0, hi=1.0):
    """A smoothly wandering control curve: random points `rate` per second, cosine-interpolated."""
    pts = [rng.uniform(lo, hi) for _ in range(int(seconds * rate) + 3)]

    def f(t):
        x = t * rate
        i = int(x)
        fr = x - i
        fr = 0.5 - 0.5 * math.cos(math.pi * fr)
        i = min(i, len(pts) - 2)
        return pts[i] + (pts[i + 1] - pts[i]) * fr
    return f


# --- filters ----------------------------------------------------------------------------------

def svf(x, sr, fc, q=0.707, mode="low"):
    """Zero-delay-feedback state-variable filter (stable at any cutoff, cheap to modulate).
    fc: Hz or a function of t (seconds); mode: low | band | high | notch. Band has unity peak gain."""
    n = len(x)
    out = [0.0] * n
    k = 1.0 / q
    ic1 = ic2 = 0.0
    varying = callable(fc)
    step = 16
    nyq = sr * 0.49
    m = {"low": 0, "band": 1, "high": 2, "notch": 3}[mode]
    for s in range(0, n, step):
        f = fc(s / sr) if varying else fc
        f = min(nyq, max(10.0, f))
        g = math.tan(math.pi * f / sr)
        a1 = 1.0 / (1.0 + g * (g + k))
        a2 = g * a1
        a3 = g * a2
        for i in range(s, min(n, s + step)):
            v0 = x[i]
            v3 = v0 - ic2
            v1 = a1 * ic1 + a2 * v3
            v2 = ic2 + a2 * ic1 + a3 * v3
            ic1 = 2 * v1 - ic1
            ic2 = 2 * v2 - ic2
            if m == 0:
                out[i] = v2
            elif m == 1:
                out[i] = v1 * k
            elif m == 2:
                out[i] = v0 - k * v1 - v2
            else:
                out[i] = v0 - k * v1
        if not varying:
            # Constant cutoff: finish in one pass with the same coefficients.
            for i in range(s + step, n):
                v0 = x[i]
                v3 = v0 - ic2
                v1 = a1 * ic1 + a2 * v3
                v2 = ic2 + a2 * ic1 + a3 * v3
                ic1 = 2 * v1 - ic1
                ic2 = 2 * v2 - ic2
                if m == 0:
                    out[i] = v2
                elif m == 1:
                    out[i] = v1 * k
                elif m == 2:
                    out[i] = v0 - k * v1 - v2
                else:
                    out[i] = v0 - k * v1
            break
    return out


def onepole_lp(x, sr, fc):
    a = 1 - math.exp(-TAU * fc / sr)
    s = 0.0
    out = [0.0] * len(x)
    for i, v in enumerate(x):
        s += (v - s) * a
        out[i] = s
    return out


def highpass(x, sr, fc):
    lp = onepole_lp(x, sr, fc)
    return [a - b for a, b in zip(x, lp)]


def formants(x, sr, spec):
    """Parallel band-pass bank: spec = [(freq, q, gain), ...] -- vowels and hums."""
    out = [0.0] * len(x)
    for f, q, g in spec:
        y = svf(x, sr, f, q, "band")
        for i, v in enumerate(y):
            out[i] += v * g
    return out


# --- space ------------------------------------------------------------------------------------

def reverb(x, sr, size=0.8, damp=0.35, wet=0.3, tail=1.0, predelay=0.0):
    """Freeverb-style room: four damped combs into two allpasses. Appends `tail` seconds."""
    x = x + [0.0] * int(tail * sr)
    n = len(x)
    k = sr / 44100.0
    combs = [int(l * k * (0.6 + 0.6 * size)) for l in (1116, 1188, 1277, 1356)]
    aps = [int(l * k) for l in (556, 441)]
    fb = 0.7 + 0.28 * size
    pd = int(predelay * sr)
    cb = [[0.0] * L for L in combs]
    ci = [0, 0, 0, 0]
    cs = [0.0, 0.0, 0.0, 0.0]
    ab = [[0.0] * L for L in aps]
    ai = [0, 0]
    out = [0.0] * n
    d1 = 1 - damp
    b0, b1, b2, b3 = cb
    L0, L1, L2, L3 = combs
    i0 = i1 = i2 = i3 = 0
    s0 = s1 = s2 = s3 = 0.0
    a0b, a1b = ab
    A0, A1 = aps
    j0 = j1 = 0
    for i in range(n):
        inp = (x[i - pd] if i >= pd else 0.0) * 0.015
        o0 = b0[i0]; s0 = o0 * d1 + s0 * damp; b0[i0] = inp + s0 * fb; i0 += 1
        if i0 == L0: i0 = 0
        o1 = b1[i1]; s1 = o1 * d1 + s1 * damp; b1[i1] = inp + s1 * fb; i1 += 1
        if i1 == L1: i1 = 0
        o2 = b2[i2]; s2 = o2 * d1 + s2 * damp; b2[i2] = inp + s2 * fb; i2 += 1
        if i2 == L2: i2 = 0
        o3 = b3[i3]; s3 = o3 * d1 + s3 * damp; b3[i3] = inp + s3 * fb; i3 += 1
        if i3 == L3: i3 = 0
        y = o0 + o1 + o2 + o3
        bo = a0b[j0]; a0b[j0] = y + bo * 0.5; y = bo - y; j0 += 1
        if j0 == A0: j0 = 0
        bo = a1b[j1]; a1b[j1] = y + bo * 0.5; y = bo - y; j1 += 1
        if j1 == A1: j1 = 0
        out[i] = x[i] + y * wet * 4.0
    return out


# --- finishing --------------------------------------------------------------------------------

def dc_block(x, sr, fc=18.0):
    return highpass(x, sr, fc)


def fade(x, sr, fade_in=0.002, fade_out=0.02):
    n = len(x)
    a = max(1, int(fade_in * sr))
    b = max(1, int(fade_out * sr))
    y = list(x)
    for i in range(min(a, n)):
        y[i] *= i / a
    for i in range(min(b, n)):
        y[n - 1 - i] *= i / b
    return y


def trim_tail(x, sr, floor_db=-60.0, keep=0.05):
    """Cuts trailing near-silence (relative to the peak) but keeps a short margin."""
    peak = max(1e-9, max(abs(v) for v in x))
    lim = peak * 10 ** (floor_db / 20)
    end = len(x)
    while end > 1 and abs(x[end - 1]) < lim:
        end -= 1
    return x[:min(len(x), end + int(keep * sr))]


def soft_clip(x, drive=1.0):
    return [math.tanh(v * drive) / math.tanh(drive) for v in x]


def normalize(x, peak_db=-1.5):
    peak = max(1e-9, max(abs(v) for v in x))
    g = 10 ** (peak_db / 20) / peak
    return [v * g for v in x]


def finish(x, sr, peak_db=-1.5, fade_in=0.001, fade_out=0.03, trim=True):
    """DC block, trim, fades, normalise: every sound goes through this before writing."""
    y = dc_block(x, sr)
    if trim:
        y = trim_tail(y, sr)
    y = fade(y, sr, fade_in, fade_out)
    return normalize(y, peak_db)


def stats(x, sr):
    peak = max(abs(v) for v in x)
    rms = math.sqrt(sum(v * v for v in x) / len(x))
    dc = sum(x) / len(x)
    return len(x) / sr, peak, rms, dc


def write_wav(path, x, sr):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(struct.pack("<%dh" % len(x), *(max(-32767, min(32767, int(round(v * 32767)))) for v in x)))


def midi_hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)
