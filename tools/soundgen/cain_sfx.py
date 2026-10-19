"""v0.21 Cain, the Father of Murder: every sound in CainAssets.SOUNDS, written by generate.py to sounds/cain/<id>.ogg;
CainSoundData points supernaturalcraft:cain.<id> at them once the files exist (rerun runData).

His palette: the beekeeper (bees -- many detuned buzzing saws drifting in and out -- and a calm, tired low voice), the farm
(earth torn by a plough, farm tools whirling through the air with a violet telekinetic shimmer), and the Mark (a slow, heavy
heartbeat, a deep fire roar, Abel's blood crying out of the ground as a faint far-off formant swell). His voice is a low,
gruff baritone, used sparingly: grunts and breaths, never words.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from coven_sfx import glide, rasp
from magic_sfx import AH, OH, fire_roar, whisper
from michael_sfx import ring, whoosh
from raphael_sfx import crackle, rumble
from sfx import HUM, UH, burst, thud, voiced
from synth import TAU, add, buf, env_apply, formants, noise, reverb, saw, smooth_random, soft_clip, svf, tone

AW = [(640, 4.0, 1.0), (1000, 5.0, 0.6), (2600, 7.0, 0.2)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def yard(x, sr, wet=0.14, tail=0.5):
    """An open farmyard: a short, airy reflection."""
    return reverb(x, sr, size=0.55, damp=0.45, wet=wet, tail=tail)


# --- building blocks -----------------------------------------------------------------------------------

def bee_cloud(sr, rng, T, count=36, lo=190, hi=290, flutter=(4, 10), gain=0.06, swell=None, loop=True):
    """Bees: each a buzzing saw at its own pitch with a wing-beat vibrato, drifting in and out; with `swell(t)` the cloud
    rises (and gets angrier: pitches and brightness climb with it)."""
    x = buf(sr, T)
    for _ in range(count):
        f = rng.uniform(lo, hi)
        s = saw(sr, T, f, 0, rng.random(), (rng.uniform(*flutter), rng.uniform(25, 80)))
        amp = smooth_random(sr, T, rng, rng.uniform(1.5, 4.5), 0.0, 1.0)
        if loop:
            s = env_apply(s, sr, lambda t, a=amp: a(t) ** 2 * (0.6 - 0.4 * math.cos(TAU * t / T)))
        else:
            s = env_apply(s, sr, lambda t, a=amp: a(t) ** 2)
        add(x, s, 0, gain, sr)
    fc = (lambda t: 1300 + 1500 * swell(t)) if swell else 1500
    x = svf(x, sr, fc, 0.8, "low")
    x = formants(x, sr, [(300, 2.0, 1.0), (850, 2.5, 0.65), (2300, 3.0, 0.3)])
    if swell:
        x = env_apply(x, sr, swell)
    return x


def heartbeat(sr, rng, beats, bpm, gain=1.0, depth=1.0):
    """A heavy heart: lub-dub thumps (sub sines with a soft knock), one per beat."""
    gap = 60.0 / bpm
    T = beats * gap + 0.6
    x = buf(sr, T)
    for b in range(beats):
        t0 = b * gap
        for k, (f0, a) in enumerate(((62, 1.0), (54, 0.7))):
            at = t0 + k * 0.22
            add(x, thud(sr, f0 * 1.4, f0 * 0.8, 0.09 * depth, 0.4), at, a * gain, sr)
            add(x, burst(sr, rng, 0.08, 180, 0.8, "low", 0.03), at, 0.35 * a * gain, sr)
    return x


def grunt(sr, rng, T, f_hi, f_lo, vowel=UH, breath=0.7, rough=1.8):
    """His gruff voice: a short, falling, throaty grunt (rasped)."""
    v = voiced(sr, rng, T, glide(f_hi, f_lo, T, 0.7), vowel, breath,
               lambda t: min(1.0, t / 0.03) * math.exp(-max(0.0, t - 0.05) / (T * 0.35)))
    return rasp(v, rough)


def breath(sr, rng, T, fc=900, q=0.7, inhale=False):
    """A tired breath: band-passed noise swelling and sighing out."""
    x = svf(noise(int(sr * T), rng), sr, (lambda t: fc * (0.8 + 0.4 * t / T)) if inhale else (lambda t: fc * (1.2 - 0.5 * t / T)),
            q, "band")
    return env_apply(x, sr, lambda t: math.sin(math.pi * min(1.0, t / T)) ** 1.5)


def grains(sr, rng, T, rate, fc=1800, q=0.8, tau=0.012, gate=None):
    """Crumbling earth: many tiny clods, each a short filtered tick, scattered at `rate` per second (shaped by gate(t))."""
    x = buf(sr, T)
    n = int(T * rate)
    for _ in range(n):
        t = rng.uniform(0, T * 0.95)
        g = gate(t) if gate else 1.0
        if g <= 0.02 or rng.random() > g:
            continue
        add(x, burst(sr, rng, tau * 4, fc * rng.uniform(0.6, 1.6), q, "band", tau), t, rng.uniform(0.3, 1.0), sr)
    return x


def scrape(sr, rng, T, fc0=900, fc1=1600):
    """Steel dragged through earth: a rough band of noise with a rasping grain and a dull metallic whine."""
    x = svf(noise(int(sr * T), rng), sr, lambda t: fc0 + (fc1 - fc0) * t / T, 3.0, "band")
    rough = smooth_random(sr, T, rng, 60, 0.3, 1.0)
    x = [s * rough(i / sr) for i, s in enumerate(x)]
    add(x, env_apply(tone(sr, 1250, T, glide=980), sr, lambda t: 0.25 * math.sin(math.pi * t / T)), 0, 1.0, sr)
    return env_apply(x, sr, _env(0.05, T * 0.5, T * 0.45))


def whir(sr, rng, T, rate, f0=600, f1=1800):
    """A spinning tool: a whoosh pulsed at its spin rate, sweeping past."""
    x = whoosh(sr, rng, T, f0, f1, 1.6, 0.55)
    return [s * (0.35 + 0.65 * abs(math.sin(math.pi * rate * i / sr))) for i, s in enumerate(x)]


def shimmer(sr, rng, T, f=880, gain=1.0):
    """Telekinesis: a cluster of high detuned sines beating, swelling and fading (the violet hum)."""
    x = buf(sr, T)
    for k in range(5):
        fk = f * (1, 1.5, 2.01, 2.99, 4.02)[k] * (1 + rng.uniform(-0.006, 0.006))
        add(x, tone(sr, fk, T, phase=rng.random() * TAU), 0, 0.18 / (k + 1) * gain, sr)
    trem = smooth_random(sr, T, rng, 12, 0.4, 1.0)
    return env_apply([s * trem(i / sr) for i, s in enumerate(x)], sr, lambda t: math.sin(math.pi * min(1.0, t / T)))


def drone(sr, rng, T, f=55, bright=600):
    """A dark low drone: detuned saws, low-passed, slowly breathing."""
    x = buf(sr, T)
    for d in (-7, 0, 6, 1200):
        add(x, saw(sr, T, f, d, rng.random(), (0.3, 6)), 0, 0.22 if d != 1200 else 0.08, sr)
    x = svf(x, sr, bright, 0.9, "low")
    br = smooth_random(sr, T, rng, 0.8, 0.7, 1.0)
    return [s * br(i / sr) for i, s in enumerate(x)]


# --- the sounds ----------------------------------------------------------------------------------------

def ambient(sr, rng):
    """At ease among his bees: a low, closed-mouth murmur, a tired breath, a few bees drifting by."""
    T = 2.6
    x = buf(sr, T)
    add(x, bee_cloud(sr, rng, T, 9, 200, 260, gain=0.05), 0, 0.35, sr)
    hm = voiced(sr, rng, 1.1, glide(98, 86, 1.1), HUM, 0.4, _env(0.12, 0.5, 0.45))
    add(x, hm, 0.25, 0.8, sr)
    add(x, breath(sr, rng, 1.0, 700, 0.6), 1.45, 0.35, sr)
    return yard(x, sr, 0.12, 0.4)


def hurt(sr, rng):
    """A blow taken: a short gruff grunt through the teeth."""
    x = buf(sr, 0.6)
    add(x, burst(sr, rng, 0.08, 1400, 0.8, "band", 0.03), 0, 0.3, sr)
    add(x, grunt(sr, rng, 0.38, 132, 92, UH, 0.8, 2.0), 0.01, 1.0, sr)
    return reverb(x, sr, size=0.35, damp=0.5, wet=0.07, tail=0.15)


def death(sr, rng):
    """He kneels: a long falling groan, a last heavy breath, the Mark's heart slowing to nothing."""
    T = 4.8
    x = buf(sr, T)
    g = voiced(sr, rng, 1.9, glide(118, 70, 1.9, 0.8), AW, 0.9, _env(0.08, 0.8, 1.0))
    add(x, rasp(g, 1.6), 0.0, 0.9, sr)
    add(x, breath(sr, rng, 1.3, 650, 0.6), 2.1, 0.55, sr)
    hb = heartbeat(sr, rng, 4, 46, 1.0, 1.3)
    hb = env_apply(hb, sr, lambda t: max(0.0, 1 - t / 4.6) ** 1.3)
    add(x, hb, 0.6, 0.9, sr)
    add(x, env_apply(drone(sr, rng, T - 0.2, 49, 380), sr, _env(0.6, 1.6, 2.2)), 0.0, 0.35, sr)
    return reverb(x, sr, size=0.8, damp=0.4, wet=0.22, tail=0.8)


def lunge(sr, rng):
    """He charges: a hard rush of air, boots skidding in the dirt, a grunt of effort."""
    T = 0.9
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.45, 350, 2400, 1.1, 0.7), 0, 0.9, sr)
    add(x, grunt(sr, rng, 0.22, 150, 115, UH, 0.9, 2.2), 0.05, 0.5, sr)
    skid = svf(noise(int(sr * 0.32), rng), sr, lambda t: 1600 - 1800 * t, 1.2, "band")
    add(x, env_apply(skid, sr, lambda t: min(1.0, t / 0.02) * math.exp(-t / 0.12)), 0.42, 0.6, sr)
    add(x, grains(sr, rng, 0.35, 160, 2000, 0.9, 0.008, lambda t: math.exp(-t / 0.12)), 0.43, 0.35, sr)
    add(x, thud(sr, 95, 50, 0.08, 0.3), 0.42, 0.5, sr)
    return yard(x, sr, 0.1, 0.3)


def swarm(sr, rng):
    """The bees rise: an angry drone swelling as the hive pours out, wing-beats fluttering faster."""
    T = 2.4
    sw = lambda t: min(1.0, (t / 1.6) ** 1.6) * (1.0 if t < 2.0 else max(0.0, 1 - (t - 2.0) / 0.4))  # noqa: E731
    x = bee_cloud(sr, rng, T, 44, 210, 360, (7, 14), 0.06, sw, loop=False)
    return yard(soft_clip(x, 1.4), sr, 0.12, 0.4)


def bees(sr, rng):
    """The swarm stinging: a dense, close cloud of bees (2.6 s, loop-friendly)."""
    return bee_cloud(sr, rng, 2.6, 48, 200, 340, (6, 12), 0.055)


def furrow(sr, rng):
    """The plough's swing rips the earth: a blade scraping through, the ground tearing, clods raining down."""
    T = 2.0
    x = buf(sr, T)
    add(x, whoosh(sr, rng, 0.3, 300, 1400, 1.0, 0.7), 0, 0.5, sr)
    add(x, scrape(sr, rng, 0.55, 700, 1500), 0.18, 0.5, sr)
    tear = svf(noise(int(sr * 0.7), rng), sr, lambda t: 420 - 250 * t, 0.7, "band")
    add(x, env_apply(tear, sr, lambda t: min(1.0, t / 0.03) * math.exp(-t / 0.25)), 0.22, 1.1, sr)
    add(x, rumble(sr, rng, 1.6, 70, 0.6, 5), 0.2, 2.0, sr)
    add(x, thud(sr, 80, 38, 0.25, 0.8), 0.22, 0.8, sr)
    add(x, grains(sr, rng, 1.5, 420, 1500, 0.8, 0.01, lambda t: math.exp(-t / 0.55)), 0.3, 0.55, sr)
    return yard(soft_clip(x, 1.2), sr, 0.15, 0.6)


def abels_blood(sr, rng):
    """Abel's blood cries out: a wet, deep thud, the earth splitting, stone spikes bursting up, and far off a voice."""
    T = 3.0
    x = buf(sr, T)
    add(x, env_apply(drone(sr, rng, 2.6, 46, 300), sr, _env(0.5, 1.0, 1.1)), 0, 0.45, sr)
    # The palm on the ground: a wet slap and a deep thump.
    wet = svf(noise(int(sr * 0.25), rng), sr, lambda t: 900 - 2000 * t, 1.5, "band")
    add(x, env_apply(wet, sr, lambda t: math.exp(-t / 0.06)), 0.0, 0.6, sr)
    add(x, thud(sr, 70, 32, 0.35, 1.0), 0.0, 1.0, sr)
    # The ground splits, then the spikes: sharp stone cracks in a quick ripple.
    add(x, rumble(sr, rng, 1.0, 80, 0.35, 4), 0.25, 1.4, sr)
    for k in range(9):
        at = 0.38 + k * 0.035 + rng.uniform(-0.01, 0.01)
        add(x, burst(sr, rng, 0.12, rng.uniform(2400, 4200), 1.2, "band", 0.025), at, rng.uniform(0.5, 0.9), sr)
        add(x, burst(sr, rng, 0.2, rng.uniform(500, 900), 1.0, "band", 0.05), at, 0.4, sr)
    add(x, grains(sr, rng, 1.0, 260, 2600, 1.0, 0.006, lambda t: math.exp(-t / 0.3)), 0.45, 0.4, sr)
    # Far off, faint: a cry rising out of the ground (an "ah" swelling, high and thin, deep in reverb).
    cry = voiced(sr, rng, 1.6, glide(330, 470, 1.6, 0.6), AH, 1.2, lambda t: math.sin(math.pi * min(1.0, t / 1.6)) ** 2)
    cry = svf(cry, sr, 1400, 0.8, "low")
    add(x, reverb(cry, sr, size=0.95, damp=0.6, wet=0.9, tail=0.6), 0.7, 0.18, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.7, damp=0.4, wet=0.18, tail=0.7)


def throw(sr, rng):
    """His tools fly: a violet shimmer of the mind's grip, a rattle of iron, and three spinning whirs past."""
    T = 1.4
    x = buf(sr, T)
    add(x, shimmer(sr, rng, 1.0, 760), 0, 0.8, sr)
    for k in range(5):
        add(x, ring(sr, rng.uniform(1400, 2600), 0.25, 0.7), 0.02 + k * 0.04 + rng.uniform(0, 0.02), 0.12, sr)
    for k, (rate, f0, f1) in enumerate(((11, 500, 1700), (14, 650, 2100), (9, 420, 1500))):
        add(x, whir(sr, rng, 0.55, rate, f0, f1), 0.15 + k * 0.12, 0.7, sr)
    return yard(x, sr, 0.14, 0.4)


def execution(sr, rng):
    """He is behind you: a sudden zip of displaced air, a beat of silence, the blade driven down -- slice and thud."""
    T = 1.4
    x = buf(sr, T)
    zip_ = svf(noise(int(sr * 0.12), rng), sr, lambda t: 5000 - 30000 * t, 2.0, "band")
    add(x, env_apply(zip_, sr, lambda t: math.sin(math.pi * min(1.0, t / 0.12))), 0, 0.7, sr)
    add(x, tone(sr, 1800, 0.1, decay=0.03, glide=300), 0, 0.2, sr)
    at = 0.32
    add(x, whoosh(sr, rng, 0.16, 1200, 4800, 2.0, 0.8), at - 0.13, 0.6, sr)
    add(x, ring(sr, 2300, 0.6, 1.2), at, 0.25, sr)
    add(x, burst(sr, rng, 0.18, 3800, 1.4, "band", 0.04), at, 0.7, sr)
    add(x, thud(sr, 110, 45, 0.12, 0.5), at + 0.01, 1.0, sr)
    add(x, burst(sr, rng, 0.2, 260, 0.8, "low", 0.06), at + 0.01, 0.8, sr)
    add(x, grunt(sr, rng, 0.25, 125, 95, UH, 0.8, 2.2), at - 0.05, 0.35, sr)
    return reverb(soft_clip(x, 1.3), sr, size=0.55, damp=0.45, wet=0.13, tail=0.4)


def mark_flare(sr, rng):
    """The Mark blazes: a heart racing under a rising drone, a fire's roar bursting out with a sub boom, embers crackling."""
    T = 3.6
    x = buf(sr, T)
    # The charge: heartbeats quickening into the drone's rise.
    t = 0.0
    gap = 0.62
    while t < 1.5:
        add(x, heartbeat(sr, rng, 1, 60 / gap, 1.0, 0.9), t, 0.7, sr)
        t += gap
        gap *= 0.8
    rise = drone(sr, rng, 1.7, 41, 500)
    rise = env_apply(rise, sr, lambda t: (t / 1.7) ** 1.5)
    add(x, rise, 0.0, 0.7, sr)
    add(x, env_apply(tone(sr, 70, 1.7, glide=160), sr, lambda t: (t / 1.7) ** 2), 0.0, 0.3, sr)
    # The burst.
    p = 1.7
    add(x, thud(sr, 65, 28, 0.6, 1.6), p, 1.3, sr)
    add(x, fire_roar(sr, rng, 1.7, 160, 1100, 0.15), p - 0.05, 2.0, sr)
    add(x, burst(sr, rng, 0.4, 900, 0.6, "band", 0.12), p, 0.9, sr)
    # A roar in it (his voice under the fire, deep and wide).
    rv = voiced(sr, rng, 1.0, glide(96, 72, 1.0), AW, 1.0, _env(0.06, 0.4, 0.5))
    add(x, rasp(rv, 2.2), p, 0.45, sr)
    add(x, crackle(sr, rng, 1.8, 0.06, 2600, 0.8, 0.9), p + 0.1, 0.9, sr)
    return reverb(soft_clip(x, 1.4), sr, size=0.85, damp=0.4, wet=0.2, tail=0.6)


def offer_mark(sr, rng):
    """The Mark offered: a slow heavy heart under a warm, dark drone, a breath of a whisper over it."""
    T = 3.6
    x = buf(sr, T)
    add(x, env_apply(drone(sr, rng, T - 0.3, 55, 520), sr, _env(0.8, 1.4, 1.1)), 0, 0.5, sr)
    oo = voiced(sr, rng, 2.6, glide(110, 104, 2.6), OH, 0.6, _env(0.8, 0.8, 1.0))
    add(x, oo, 0.3, 0.25, sr)
    add(x, heartbeat(sr, rng, 3, 52, 1.0, 1.2), 0.15, 0.9, sr)
    w = svf(whisper(sr, rng, 1.6, 4, 1.0), sr, 900, 0.7, "high")
    add(x, w, 1.2, 0.3, sr)
    return reverb(x, sr, size=0.85, damp=0.45, wet=0.25, tail=0.8)


# id (the file's base name) -> (builder, peak dBFS)
SOUNDS = {
    "ambient": (ambient, -4.0), "hurt": (hurt, -2.0), "death": (death, -1.5), "lunge": (lunge, -1.5), "swarm": (swarm, -2.0),
    "bees": (bees, -3.0), "furrow": (furrow, -1.0), "abels_blood": (abels_blood, -1.0), "throw": (throw, -2.0),
    "execution": (execution, -1.0), "mark_flare": (mark_flare, -1.0), "offer_mark": (offer_mark, -2.0),
}
