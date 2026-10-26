"""v0.25 the Leviathans and Dick Roman: every sound in LeviathanAssets.SOUNDS and DickAssets.SOUNDS, written by generate.py to
sounds/leviathan/<id>.ogg and sounds/dick_roman/<id>.ogg; the sound data points supernaturalcraft:leviathan.<id> and
dick_roman.<id> at them once the files exist (rerun runData; until then the datagen falls back to vanilla sounds).

The palette: the goo (wet, resonant squelches; slow bubbles bloop-ing up through something thick; a gurgle in a throat that
is not a throat), the maw (the head tearing open, rows of teeth clicking and snapping, a low throaty roar), borax (a bright
hiss of tiny crackles and high bubbles, like salt on a slug), and Dick himself: a smooth, warm baritone businessman, used for
his arrival and his laugh -- polished shoes on a hard floor, a pleased "mm", a short easy chuckle -- with the goo under it.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from coven_sfx import EH, glide, rasp
from magic_sfx import AH, OH
from sfx import HUM, UH, burst, thud, voiced
from synth import TAU, add, buf, env_apply, highpass, noise, onepole_lp, reverb, saw, smooth_random, soft_clip, svf, tone


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def office(x, sr, wet=0.16, tail=0.5):
    """A boardroom: hard glass and stone, a short bright reflection."""
    return reverb(x, sr, size=0.5, damp=0.25, wet=wet, tail=tail)


def cellar(x, sr, wet=0.2, tail=0.7):
    """Somewhere damp and close."""
    return reverb(x, sr, size=0.7, damp=0.6, wet=wet, tail=tail)


# --- building blocks -----------------------------------------------------------------------------------

def squelch(sr, rng, T, fc0=300, fc1=1400, q=4.0, gain=1.0):
    """A wet squelch: noise through a sharp resonant band sweeping fc0 -> fc1, chopped by a fast wobble."""
    x = svf(noise(int(sr * T), rng), sr, lambda t: fc0 * (fc1 / fc0) ** min(1.0, t / T), q, "band")
    wob = smooth_random(sr, T, rng, 40, 0.2, 1.0)
    return env_apply([s * wob(i / sr) * gain for i, s in enumerate(x)], sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 0.7)


def bloop(sr, f, T=0.09):
    """One bubble: a short sine gliding up (the cavity shrinking as it rises), decaying fast."""
    return tone(sr, f, T, decay=T * 0.35, glide=f * 1.7, attack=0.002)


def bubbles(sr, rng, T, rate, lo=180, hi=520, size=0.09, gate=None):
    """Bubbles welling up through goo at `rate` per second (shaped by gate(t))."""
    x = buf(sr, T)
    for _ in range(int(T * rate)):
        t = rng.uniform(0, T * 0.95)
        if gate and rng.random() > gate(t):
            continue
        f = rng.uniform(lo, hi)
        add(x, bloop(sr, f, size * rng.uniform(0.7, 1.4)), t, rng.uniform(0.3, 1.0), sr)
    return x


def gurgle(sr, rng, T, f=70, bright=700, gain=1.0):
    """A throat full of goo: a low buzz through a wobbling formant, bubbles rising through it."""
    x = buf(sr, T)
    for d in (-12, 0, 9):
        add(x, saw(sr, T, f, d, rng.random(), (5.5, 30)), 0, 0.25, sr)
    wob = smooth_random(sr, T, rng, 7, 0.0, 1.0)
    x = svf(x, sr, lambda t: bright * (0.5 + 0.8 * wob(t)), 3.0, "band")
    add(x, bubbles(sr, rng, T, 14, 90, 260, 0.12), 0, 0.5, sr)
    return [s * gain for s in x]


def teeth(sr, rng, n, spacing=0.035, fc=3800, gain=1.0):
    """Rows of teeth clicking together: `n` hard, bright ticks with a bony knock under each."""
    T = n * spacing + 0.12
    x = buf(sr, T)
    for i in range(n):
        at = i * spacing * rng.uniform(0.8, 1.2)
        add(x, burst(sr, rng, 0.03, fc * rng.uniform(0.8, 1.3), 2.5, "band", 0.004), at, gain * rng.uniform(0.6, 1.0), sr)
        add(x, thud(sr, 900, 500, 0.01, 0.04), at, 0.3 * gain, sr)
    return x


def snap(sr, rng, depth=1.0):
    """Jaws snapping shut: a rush of air drawn in, then a hard crack of teeth and a wet crunch."""
    T = 0.7
    x = buf(sr, T)
    add(x, env_apply(svf(noise(int(sr * 0.18), rng), sr, lambda t: 600 + 4000 * t / 0.18, 1.2, "band"), sr,
                     lambda t: (t / 0.18) ** 2), 0, 0.35, sr)
    add(x, burst(sr, rng, 0.06, 3200, 1.5, "band", 0.006), 0.18, 1.0, sr)
    add(x, teeth(sr, rng, 5, 0.006, 4200, 0.7), 0.18, 1.0, sr)
    add(x, thud(sr, 180 * depth, 80 * depth, 0.06, 0.25), 0.18, 0.9, sr)
    add(x, squelch(sr, rng, 0.3, 1200, 300, 3.0), 0.2, 0.7, sr)
    return x


def maw_roar(sr, rng, T, f_hi=110, f_lo=70, gain=1.0):
    """The maw's roar: a low growl that is half voice, half gurgle, rasped, with teeth chattering in it."""
    v = voiced(sr, rng, T, glide(f_hi, f_lo, T, 0.8), AH, 1.1, _env(0.05, T * 0.5, T * 0.45))
    x = rasp(v, 2.4)
    add(x, gurgle(sr, rng, T, f_lo * 0.8, 600, 0.6), 0, 1.0, sr)
    add(x, teeth(sr, rng, int(T * 14), 0.07, 3400, 0.25), 0.05, 1.0, sr)
    return [s * gain for s in x]


def tear(sr, rng, T=0.55):
    """Flesh splitting: a wet rip sweeping up, crackling."""
    x = squelch(sr, rng, T, 250, 2200, 2.5)
    for _ in range(int(T * 60)):
        add(x, burst(sr, rng, 0.02, rng.uniform(1500, 4000), 1.2, "band", 0.003), rng.uniform(0, T * 0.9), rng.uniform(0.2, 0.6), sr)
    return x


def fizz(sr, rng, T, density=900, gain=1.0):
    """Borax on goo: a bright hiss of tiny crackles and high, fast bubbles, the flesh sizzling."""
    n = int(sr * T)
    x = highpass(noise(n, rng), sr, 4500)
    x = [s * 0.25 for s in x]
    for _ in range(int(T * density)):
        add(x, burst(sr, rng, 0.006, rng.uniform(3000, 8000), 1.5, "band", 0.0012), rng.uniform(0, T * 0.97), rng.uniform(0.1, 0.5), sr)
    add(x, bubbles(sr, rng, T, 50, 900, 2400, 0.03), 0, 0.5, sr)
    return [s * gain for s in x]


def footstep(sr, rng, hard=1.0):
    """A polished shoe on stone: a sharp heel click and a soft sole."""
    T = 0.25
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.05, 2600, 2.0, "band", 0.008), 0, 0.8 * hard, sr)
    add(x, thud(sr, 240, 140, 0.02, 0.08), 0, 0.5 * hard, sr)
    add(x, burst(sr, rng, 0.08, 700, 0.8, "band", 0.02), 0.05, 0.25 * hard, sr)
    return x


def chuckle(sr, rng, n, f0=118, gap=0.17, vowel=EH, breath=0.55, gain=1.0):
    """A smooth, easy laugh: `n` short voiced "heh"s, each falling a little, the last longer."""
    T = n * gap + 0.4
    x = buf(sr, T)
    for i in range(n):
        last = i == n - 1
        d = 0.24 if last else 0.11
        f = f0 * (1.0 - 0.03 * i)
        h = voiced(sr, rng, d, glide(f * 1.08, f * 0.9, d), vowel, breath,
                   lambda t, d=d: min(1.0, t / 0.012) * math.exp(-max(0.0, t - 0.02) / (d * 0.45)))
        # The breathy "h" before each.
        add(x, svf(noise(int(sr * 0.04), rng), sr, 1600, 0.8, "band"), i * gap, 0.08, sr)
        add(x, h, i * gap + 0.02, gain * (1.0 if i == 0 else 0.85), sr)
    return x


# --- the Leviathans ------------------------------------------------------------------------------------

def lev_ambient(sr, rng):
    """A Leviathan at rest in its borrowed skin: a slow wet breath, something shifting under it, a bubble or two."""
    T = 2.4
    x = buf(sr, T)
    add(x, env_apply(gurgle(sr, rng, T, 62, 520), sr, _env(0.5, 1.0, 0.9)), 0, 0.45, sr)
    add(x, squelch(sr, rng, 0.5, 260, 500, 3.0), 0.7, 0.35, sr)
    add(x, bubbles(sr, rng, T, 4, 140, 320, 0.12), 0, 0.6, sr)
    return cellar(x, sr, 0.15, 0.4)


def lev_reveal(sr, rng):
    """The face splits: a wet rip, the jaws unfolding with a rattle of teeth, a short hungry roar."""
    T = 1.5
    x = buf(sr, T)
    add(x, tear(sr, rng, 0.5), 0, 0.9, sr)
    add(x, teeth(sr, rng, 10, 0.03, 3600, 0.6), 0.35, 1.0, sr)
    add(x, maw_roar(sr, rng, 0.9, 130, 85), 0.45, 0.9, sr)
    return cellar(soft_clip(x, 1.2), sr, 0.16, 0.5)


def lev_bite(sr, rng):
    """A Leviathan's bite."""
    return cellar(snap(sr, rng, 1.2), sr, 0.12, 0.3)


def lev_hurt(sr, rng):
    """A blow into goo: a wet slap and a gargled snarl."""
    T = 0.6
    x = buf(sr, T)
    add(x, squelch(sr, rng, 0.2, 900, 300, 3.5), 0, 0.8, sr)
    add(x, maw_roar(sr, rng, 0.35, 150, 105, 0.8), 0.03, 1.0, sr)
    return cellar(x, sr, 0.1, 0.2)


def lev_death(sr, rng):
    """It comes apart: a long gargling roar sinking into bubbling, the body slumping wetly."""
    T = 2.8
    x = buf(sr, T)
    add(x, maw_roar(sr, rng, 1.4, 120, 55), 0, 0.9, sr)
    g = bubbles(sr, rng, 1.8, 30, 120, 380, 0.12, lambda t: max(0.0, 1 - t / 1.8))
    add(x, g, 0.9, 0.8, sr)
    add(x, squelch(sr, rng, 0.6, 600, 160, 2.5), 1.3, 0.8, sr)
    add(x, thud(sr, 90, 45, 0.12, 0.5), 1.4, 0.6, sr)
    return cellar(x, sr, 0.2, 0.6)


def head_crawl(sr, rng):
    """The severed head dragging itself home: small wet slaps, teeth clicking on the floor."""
    T = 1.2
    x = buf(sr, T)
    t = 0.0
    while t < T - 0.2:
        add(x, squelch(sr, rng, 0.09, rng.uniform(500, 900), rng.uniform(250, 400), 4.0), t, rng.uniform(0.5, 0.9), sr)
        if rng.random() < 0.5:
            add(x, teeth(sr, rng, 2, 0.02, 3800, 0.4), t + 0.04, 1.0, sr)
        t += rng.uniform(0.12, 0.2)
    return cellar(x, sr, 0.1, 0.25)


def borax_fizz(sr, rng):
    """Borax on a Leviathan: a burst of hiss and crackle, sizzling down."""
    T = 1.6
    x = fizz(sr, rng, T)
    x = env_apply(x, sr, lambda t: min(1.0, t / 0.01) * (0.35 + 0.65 * math.exp(-t / 0.5)) * max(0.0, 1 - t / T) ** 0.5)
    add(x, squelch(sr, rng, 0.25, 1800, 700, 2.0), 0, 0.35, sr)
    return x


# --- Dick Roman --------------------------------------------------------------------------------------------

def dick_arrive(sr, rng):
    """He steps in: three unhurried steps on a hard floor, a jacket button, a pleased, closed-mouth "mm" -- the goo just under it."""
    T = 2.6
    x = buf(sr, T)
    for i, at in enumerate((0.0, 0.48, 0.96)):
        add(x, footstep(sr, rng, 0.8 + 0.1 * i), at, 1.0, sr)
    # The button: a tiny, dry click.
    add(x, burst(sr, rng, 0.02, 3000, 3.0, "band", 0.003), 1.25, 0.5, sr)
    mm = voiced(sr, rng, 0.75, glide(104, 118, 0.3, 1.0), HUM, 0.35, _env(0.06, 0.35, 0.34))
    add(x, mm, 1.45, 0.9, sr)
    # And under the smile, briefly, something wet moving.
    add(x, squelch(sr, rng, 0.4, 200, 380, 3.5), 1.6, 0.18, sr)
    return office(x, sr, 0.16, 0.6)


def dick_laugh(sr, rng):
    """A smooth, amused chuckle -- the boardroom's -- with the faintest gargle at its end."""
    x = chuckle(sr, rng, 4, 116, 0.16, EH, 0.5)
    tail = env_apply(gurgle(sr, rng, 0.4, 60, 450), sr, lambda t: math.sin(math.pi * t / 0.4))
    add(x, tail, 0.62, 0.15, sr)
    return office(x, sr, 0.17, 0.5)


def dick_maw(sr, rng):
    """The smile splits wide open: a rip, the jaws unfolding, a deep wet roar with the voice still in it."""
    T = 2.2
    x = buf(sr, T)
    add(x, tear(sr, rng, 0.7), 0, 1.0, sr)
    add(x, teeth(sr, rng, 16, 0.025, 3400, 0.6), 0.45, 1.0, sr)
    add(x, maw_roar(sr, rng, 1.6, 98, 52, 1.1), 0.5, 1.0, sr)
    add(x, thud(sr, 70, 35, 0.3, 1.2), 0.5, 0.5, sr)
    return office(soft_clip(x, 1.3), sr, 0.2, 0.8)


def dick_bite(sr, rng):
    """His bite: a deeper, bigger snap."""
    x = snap(sr, rng, 0.75)
    add(x, maw_roar(sr, rng, 0.3, 90, 70, 0.4), 0.1, 1.0, sr)
    return office(x, sr, 0.15, 0.4)


def dick_goo(sr, rng):
    """A heave of goo: a gulping retch and a heavy wet splat."""
    T = 1.4
    x = buf(sr, T)
    add(x, gurgle(sr, rng, 0.5, 58, 500, 0.8), 0, 1.0, sr)
    add(x, squelch(sr, rng, 0.5, 180, 900, 2.0, 1.2), 0.35, 1.0, sr)
    add(x, thud(sr, 120, 50, 0.08, 0.3), 0.75, 0.8, sr)
    add(x, squelch(sr, rng, 0.35, 900, 220, 3.0), 0.75, 0.9, sr)
    add(x, bubbles(sr, rng, 0.6, 20, 120, 360, 0.1), 0.8, 0.6, sr)
    return office(x, sr, 0.14, 0.5)


def dick_copy(sr, rng):
    """He becomes someone else: skin and bone slithering into a new shape, a glassy shimmer settling over it."""
    T = 1.8
    x = buf(sr, T)
    for i in range(6):
        add(x, squelch(sr, rng, 0.22, rng.uniform(300, 600), rng.uniform(800, 1600), 4.5), 0.05 + i * 0.18, 0.5, sr)
    sh = buf(sr, T)
    for k in range(5):
        fk = 660 * (1, 1.5, 2.01, 2.99, 4.02)[k]
        add(sh, tone(sr, fk, T, phase=rng.random() * TAU, glide=fk * 1.12), 0, 0.15 / (k + 1), sr)
    add(x, env_apply(sh, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 2), 0, 0.6, sr)
    return office(x, sr, 0.2, 0.6)


def dick_hurt(sr, rng):
    """A blow taken: a clipped, annoyed grunt through a smile, a wet slap under it."""
    T = 0.55
    x = buf(sr, T)
    v = voiced(sr, rng, 0.28, glide(140, 104, 0.28, 0.7), UH, 0.6,
               lambda t: min(1.0, t / 0.02) * math.exp(-max(0.0, t - 0.04) / 0.1))
    add(x, v, 0.01, 1.0, sr)
    add(x, squelch(sr, rng, 0.18, 800, 300, 3.5), 0, 0.5, sr)
    return office(x, sr, 0.1, 0.2)


def dick_death(sr, rng):
    """He bursts from inside: a choked voice, a long roar breaking into gargling, goo spattering, a last fizzing hiss."""
    T = 4.6
    x = buf(sr, T)
    ch = voiced(sr, rng, 0.6, glide(130, 160, 0.6), OH, 0.8, _env(0.04, 0.3, 0.26))
    add(x, rasp(ch, 1.6), 0, 0.7, sr)
    add(x, maw_roar(sr, rng, 1.8, 110, 45, 1.1), 0.55, 1.0, sr)
    add(x, tear(sr, rng, 0.6), 1.8, 0.9, sr)
    for i in range(7):
        add(x, squelch(sr, rng, 0.25, rng.uniform(600, 1100), rng.uniform(160, 300), 3.0), 2.0 + i * 0.16 * rng.uniform(0.8, 1.3), 0.6, sr)
    add(x, thud(sr, 60, 30, 0.4, 1.4), 1.9, 0.8, sr)
    f = env_apply(fizz(sr, rng, 2.0, 700, 0.6), sr, lambda t: math.sin(math.pi * min(1.0, t / 2.0)) * max(0.0, 1 - t / 2.0))
    add(x, f, 2.5, 0.7, sr)
    add(x, bubbles(sr, rng, 2.2, 18, 100, 300, 0.14, lambda t: max(0.0, 1 - t / 2.2)), 2.3, 0.6, sr)
    return office(soft_clip(x, 1.3), sr, 0.22, 0.9)


# id (the file's base name) -> (builder, peak dBFS)
LEVIATHAN = {
    "ambient": (lev_ambient, -5.0), "reveal": (lev_reveal, -1.5), "bite": (lev_bite, -1.5), "hurt": (lev_hurt, -2.0),
    "death": (lev_death, -1.5), "head_crawl": (head_crawl, -4.0), "borax_fizz": (borax_fizz, -2.0),
}

DICK = {
    "arrive": (dick_arrive, -2.0), "laugh": (dick_laugh, -2.0), "maw": (dick_maw, -1.0), "bite": (dick_bite, -1.0),
    "goo": (dick_goo, -1.5), "copy": (dick_copy, -2.5), "hurt": (dick_hurt, -2.0), "death": (dick_death, -1.0),
}
