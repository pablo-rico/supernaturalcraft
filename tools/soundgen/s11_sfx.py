"""v0.42 season 11 around Amara: every sound in S11Assets.SOUNDS, written by generate.py to sounds/<group>/<id>.ogg
(supernaturalcraft:<group>.<id>):
  casifer  -- Lucifer wearing Castiel: the finger snap that smites (a dry click, light tearing, Hell's crackle under it) and a
              smug, low laugh (breathy "heh"s, a second, deeper voice inside the first);
  darkness -- the black fog rolling (a seamless loop, see LOOPS), the Darkness let out (a vast black eruption), a villager
              taken by the fog (a choking gasp and a dark hiss), the fog letting go (a light, purifying chime) and something
              stirring (a subtle low swell);
  thule    -- the Thule Society's undead soldiers: rising from the earth with a groan, an old bolt-action rifle with its echo
              across the field, the officer's low necromantic chant, hurt and death;
  banshee  -- a keening wail and the deafening scream, hurt and death;
  chitter  -- the chitters: insect clicking from a human throat (vocal-fry clicks through a man's formants, mandibles ticking
              over them), a screech, hurt and death.

Same conventions as sfx.py: (sr, rng) -> float list, finished (normalised to its peak) and encoded by generate.py.
"""

import math

from amara_sfx import _env, echo, ghost_tone, grind, pre_swell, rubble, void_room
from coven_sfx import EH, glide, rasp, syllable
from godstiel_sfx import tearing_light
from magic_sfx import AH, EE, OH, growl, whisper
from michael_sfx import whoosh
from raphael_sfx import crackle, rumble
from sfx import HUM, UH, bell_voice, burst, choir, sparkle, thud, voiced
from synth import (TAU, add, buf, env_apply, midi_hz, noise, normalize, onepole_lp, reverb, reverse, smooth_random, soft_clip,
                   svf, tone)

LOOP_T = 7.0
XFADE = 1.5

# A woman's keening vowels (banshee) and a man's open throat (chitter clicks).
W_OO = [(330, 3.0, 1.0), (850, 4.0, 0.35), (2600, 6.0, 0.1)]
W_EE = [(330, 4.0, 1.0), (2700, 6.0, 0.5), (3200, 8.0, 0.25)]
W_AH = [(900, 4.0, 1.0), (1400, 5.0, 0.6), (2900, 7.0, 0.3)]
THROAT = [(600, 3.0, 1.0), (1700, 4.0, 0.6), (2600, 5.0, 0.35)]


def loop(x, sr, T=LOOP_T, xfade=XFADE):
    """Folds the tail over the head (equal-power crossfade): x must be at least T + xfade long; returns T seconds."""
    n = int(sr * T)
    f = int(sr * xfade)
    out = x[:n]
    for i in range(f):
        k = i / f
        out[i] = x[i] * math.sin(k * math.pi / 2) + x[n + i] * math.cos(k * math.pi / 2)
    return out


def field(x, sr, wet=0.14, tail=0.9):
    """Open ground at night: a short, wide space."""
    return reverb(x, sr, size=0.75, damp=0.5, wet=wet, tail=tail, predelay=0.02)


def clicks(sr, rng, T, rate_lo, rate_hi, formants_=THROAT, gain=1.0, hi=False):
    """Vocal-fry clicks: a throat clicking `rate` times per second (wandering between rate_lo and rate_hi), each click a short
    burst through the throat's formants; `hi` adds the hard, bright tick of a mandible on top."""
    x = buf(sr, T)
    rate = smooth_random(sr, T, rng, 4, rate_lo, rate_hi)
    t = 0.0
    while t < T - 0.03:
        for f, q, g in formants_:
            add(x, burst(sr, rng, 0.02, f * rng.uniform(0.92, 1.08), q * 1.5, "band", 0.0025), t, g * gain, sr)
        if hi and rng.random() < 0.6:
            add(x, burst(sr, rng, 0.008, rng.uniform(3500, 5500), 2.0, "band", 0.0012), t + rng.uniform(0, 0.006),
                0.5 * gain, sr)
        t += 1.0 / rate(t) * rng.uniform(0.8, 1.2)
    return x


# --- casifer ----------------------------------------------------------------------------------------------------------

def casifer_snap(sr, rng):
    """Lucifer snaps Castiel's fingers: a dry click, light tearing through someone, Hell's own crackle and a low growl under it."""
    x = buf(sr, 1.6)
    add(x, burst(sr, rng, 0.02, 1400, 0.8, "high", 0.0015), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.05, 2200, 4.0, "band", 0.01), 0.0, 0.8, sr)
    add(x, burst(sr, rng, 0.06, 950, 2.0, "band", 0.016), 0.0006, 0.5, sr)
    add(x, tearing_light(sr, rng, 0.7, 900, 5200, 0.9), 0.06, 1.0, sr)
    add(x, crackle(sr, rng, 0.9, 0.08, 2400, 0.8, 0.3), 0.06, 0.7, sr)          # the red crackle of Hell under the light
    add(x, thud(sr, 100, 38, 0.12, 0.5), 0.06, 0.8, sr)
    g = growl(sr, rng, 0.6, 52)
    add(x, env_apply(g, sr, lambda t: min(1.0, t / 0.05) * math.exp(-t / 0.18)), 0.1, 0.25, sr)
    return void_room(soft_clip(x, 1.3), sr, wet=0.22, tail=1.0, size=0.9, damp=0.35)


def casifer_laugh(sr, rng):
    """A smug, low laugh: four breathy "heh"s falling, a closed-mouth "hm" to finish, and a deeper voice laughing inside his."""
    x = buf(sr, 1.6)
    f = 122
    t = 0.0
    for i in range(4):
        h = svf(noise(int(sr * 0.06), rng), sr, 1400, 0.7, "band")
        add(x, env_apply(h, sr, lambda u: min(1.0, u / 0.008) * math.exp(-u / 0.025)), t, 0.4, sr)
        d = 0.15
        s = syllable(sr, rng, d, glide(f * 1.06, f * 0.9, d), EH, 1.0, 0.008, d * 0.3, 0.02)
        add(x, s, t + 0.015, 1.0 - 0.1 * i, sr)
        inner = syllable(sr, rng, d, glide(f * 0.53, f * 0.47, d), UH, 1.2, 0.01, d * 0.3, 0.03)
        add(x, rasp(inner, 2.0), t + 0.03, 0.22, sr)
        f *= 0.95
        t += 0.17 + 0.015 * i
    hm = voiced(sr, rng, 0.45, glide(f * 1.02, f * 0.82, 0.45), HUM, 0.8, _env(0.03, 0.12, 0.28))
    add(x, hm, t + 0.08, 0.8, sr)
    return reverb(x, sr, size=0.45, damp=0.5, wet=0.1, tail=0.35)


# --- the Darkness -----------------------------------------------------------------------------------------------------

def darkness_fog(sr, rng):
    """The black fog rolling (a seamless 7 s loop): a low churning rumble breathing in and out, a moaning wind wandering
    through it, a sub drone, and very far off, whispers."""
    T = LOOP_T + XFADE
    n = int(sr * T)
    x = buf(sr, T)
    churn = onepole_lp(onepole_lp(noise(n, rng), sr, 140), sr, 120)
    breathe = smooth_random(sr, T, rng, 0.9, 0.35, 1.0)
    add(x, [v * breathe(i / sr) for i, v in enumerate(churn)], 0, 5.0, sr)
    fc = smooth_random(sr, T, rng, 0.6, 280, 720)
    wind = svf(noise(n, rng), sr, fc, 2.4, "band")
    gust = smooth_random(sr, T, rng, 0.8, 0.15, 0.7)
    add(x, [v * gust(i / sr) for i, v in enumerate(wind)], 0, 0.6, sr)
    for at in (0.8, 4.3):
        w = onepole_lp(whisper(sr, rng, 2.2, 6, 1.0), sr, 1100)
        add(x, env_apply(w, sr, lambda t: math.sin(math.pi * min(1.0, t / 2.2))), at, 0.08, sr)
    y = loop(reverb(x, sr, size=0.9, damp=0.55, wet=0.25, tail=0.0), sr)
    # The sub drone is added after the fold, at frequencies with a whole number of cycles in the loop: coherent sines would
    # swell or cancel under the crossfade.
    m = len(y)
    for f, g in ((41.0, 0.18), (61.7, 0.12), (62.2, 0.1)):
        w = TAU * round(f * LOOP_T) / LOOP_T / sr
        ph = rng.random() * TAU
        for i in range(m):
            y[i] += math.sin(w * i + ph) * g
    return y


def darkness_release(sr, rng):
    """The Darkness let out: the air sucked backwards into a point, then a vast black eruption -- a blast, a roaring gout of
    smoke, a choir of the void, the ground rolling for seconds after."""
    T = 5.2
    x = buf(sr, T)
    at = 0.9
    suck = reverse(env_apply(onepole_lp(noise(int(sr * at), rng), sr, 1200), sr, lambda t: math.exp(-t / 0.25)))
    add(x, suck, 0.0, 0.9, sr)
    add(x, reverse(tone(sr, 60, at, decay=0.3, glide=140)), 0.0, 0.5, sr)
    add(x, thud(sr, 58, 20, 0.6, 2.0), at, 1.8, sr)
    add(x, burst(sr, rng, 1.6, 380, 0.7, "low", 0.45), at, 1.4, sr)
    add(x, burst(sr, rng, 0.08, 2200, 0.7, "high", 0.02), at, 0.6, sr)
    add(x, whoosh(sr, rng, 1.8, 90, 900, 0.9, 0.2), at, 1.4, sr)
    ch = choir(sr, rng, (31, 38, 39, 45), 3.6, "oh", voices=4, detune=30, vib=(3.5, 20.0))
    add(x, env_apply(ch, sr, _env(0.15, 1.6, 1.8)), at + 0.05, 3.0, sr)
    add(x, rumble(sr, rng, T - at, 55, 1.8, 7), at, 2.2, sr)
    add(x, crackle(sr, rng, 2.0, 0.06, 1400, 0.8, 0.8), at, 0.4, sr)
    return void_room(soft_clip(x, 2.0), sr, wet=0.22, tail=1.4)


def darkness_fog_infect(sr, rng):
    """A villager taken by the fog: a sharp gasp, a choking voice strangled in its throat, and the dark hiss of the fog
    pouring into him."""
    T = 2.0
    x = buf(sr, T)
    gasp = svf(noise(int(sr * 0.22), rng), sr, lambda t: 900 + 2200 * t / 0.22, 1.3, "band")
    add(x, env_apply(gasp, sr, lambda t: (t / 0.22) ** 1.5), 0.0, 0.7, sr)
    choke = voiced(sr, rng, 0.9, lambda t: 128 * (1 + 0.08 * math.sin(TAU * 9 * t)) * (1 - 0.2 * t), UH, 1.4,
                   _env(0.03, 0.55, 0.3))
    stops = smooth_random(sr, 0.9, rng, 14, 0.0, 1.0)
    choke = [v * (stops(i / sr) ** 2) for i, v in enumerate(rasp(choke, 2.2))]
    add(x, choke, 0.24, 0.9, sr)
    hiss = svf(noise(int(sr * 1.5), rng), sr, lambda t: 4200 * (0.3 ** (t / 1.5)), 1.0, "band")
    add(x, env_apply(hiss, sr, lambda t: min(1.0, t / 0.3) * max(0.0, 1 - t / 1.5)), 0.3, 0.45, sr)
    add(x, env_apply(tone(sr, 48, 1.6, glide=38), sr, lambda t: math.sin(math.pi * t / 1.6)), 0.25, 0.12, sr)
    return void_room(x, sr, wet=0.16, tail=0.9, size=0.8)


def darkness_cure(sr, rng):
    """The fog lets go: a light, purifying chime climbing, an airy breath rushing outward, a soft warm chord under it."""
    T = 2.6
    x = buf(sr, T)
    for i, m in enumerate((84, 88, 91, 96)):
        add(x, bell_voice(sr, midi_hz(m), 2.0, 0.6, 1.8), 0.05 + 0.11 * i, 0.3 - 0.03 * i, sr)
    add(x, whoosh(sr, rng, 1.6, 1600, 5200, 1.0, 0.3), 0.0, 0.25, sr)
    for m in (72, 76, 79):
        add(x, env_apply(tone(sr, midi_hz(m), 2.2, attack=0.4), sr, lambda t: max(0.0, 1 - t / 2.2) ** 1.5), 0.1, 0.07, sr)
    add(x, sparkle(sr, rng, 14, 0.1, 1.4, 2.2, 3000, 7600, rise=True), 0.0, 0.25, sr)
    return void_room(x, sr, wet=0.22, tail=1.3, damp=0.3)


def darkness_visit(sr, rng):
    """Something stirs: a subtle low swell rising out of nothing and sinking back, a breath somewhere in it."""
    T = 3.8
    x = buf(sr, T)
    env = lambda t: math.sin(math.pi * min(1.0, t / T)) ** 2  # noqa: E731
    for f, g in ((44.0, 0.35), (66.0, 0.22), (88.3, 0.12)):
        add(x, env_apply(tone(sr, f, T, phase=rng.random() * TAU), sr, env), 0, g, sr)
    low = svf(noise(int(sr * T), rng), sr, 210, 1.0, "band")
    add(x, env_apply(low, sr, env), 0, 0.35, sr)
    mid = svf(noise(int(sr * T), rng), sr, 420, 1.6, "band")
    add(x, env_apply(mid, sr, env), 0, 0.3, sr)
    br = reverse(env_apply(onepole_lp(whisper(sr, rng, 0.9, 3, 1.0), sr, 1200), sr, lambda t: math.exp(-t / 0.3)))
    add(x, br, 1.3, 0.12, sr)
    return void_room(x, sr, wet=0.25, tail=1.0)


# --- the Thule soldiers -----------------------------------------------------------------------------------------------

def soldier(sr, rng, T, f0, vowel=UH, env=None, rough=2.2):
    """An undead soldier's voice: a dead man's low, rasped baritone, the pitch staggering."""
    wob = smooth_random(sr, T, rng, 9, 0.9, 1.1)
    curve = f0 if callable(f0) else (lambda t, f=f0: f)
    v = voiced(sr, rng, T, lambda t: curve(t) * wob(t), vowel, 1.2, env)
    return rasp(v, rough)


def thule_rise(sr, rng):
    """A dead soldier claws up out of the earth: soil shifting and pouring off him, roots tearing, a long groan."""
    T = 2.6
    x = buf(sr, T)
    add(x, env_apply(grind(sr, rng, 1.4, 140, 1.0), sr, lambda t: min(1.0, t / 0.3) * max(0.0, 1 - t / 1.4)), 0.0, 0.8, sr)
    add(x, rubble(sr, rng, 2.0, 45, 250, 1600, 0.8, 0.45), 0.1, 1.0, sr)
    soil = onepole_lp(noise(int(sr * 1.8), rng), sr, 900)
    pour = smooth_random(sr, 1.8, rng, 10, 0.2, 1.0)
    add(x, env_apply(soil, sr, lambda t: pour(t) * math.sin(math.pi * t / 1.8)), 0.3, 0.35, sr)
    add(x, crackle(sr, rng, 1.0, 0.05, 1300, 1.0, 0.4), 0.5, 0.35, sr)
    groan = soldier(sr, rng, 1.5, glide(92, 70, 1.5, 0.8), UH, _env(0.25, 0.7, 0.55))
    add(x, groan, 0.9, 0.75, sr)
    return field(x, sr, 0.14, 0.9)


def thule_shoot(sr, rng):
    """An old bolt-action rifle: a sharp crack, the boom of the charge, its slap echoing back across the field -- and the bolt
    worked after it."""
    x = buf(sr, 1.6)
    add(x, burst(sr, rng, 0.03, 2400, 0.7, "high", 0.004), 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.12, 900, 0.8, "band", 0.03), 0.0, 1.2, sr)
    add(x, thud(sr, 150, 45, 0.12, 0.6), 0.0, 1.4, sr)
    add(x, burst(sr, rng, 0.5, 260, 0.7, "low", 0.16), 0.002, 1.0, sr)
    x = soft_clip(x, 2.0)
    # The bolt: up, back, forward, down.
    for i, (dt, f, g) in enumerate(((0.95, 2600, 0.18), (1.02, 3400, 0.22), (1.14, 3000, 0.24), (1.2, 2200, 0.2))):
        add(x, burst(sr, rng, 0.02, f, 2.5, "band", 0.003), dt, g, sr)
        add(x, burst(sr, rng, 0.02, 900, 2.0, "band", 0.004), dt + 0.002, g * 0.6, sr)
    y = echo(x, sr, 0.42, fb=0.32, wet=0.38, lp=1300, tail=1.2)
    return field(y, sr, 0.16, 1.0)


def thule_chant(sr, rng):
    """The Thule officer's chant: a low monotone litany, the words hard-edged, a second voice an octave under it like the
    grave answering."""
    words = [(0.3, AH, 0), (0.24, OH, 0), (0.42, EH, 1), (0.26, UH, 0), (0.3, AH, 0), (0.24, OH, 2), (0.5, AH, 0), (0.34, UH, -1)]
    T = sum(d for d, _, _ in words) + 0.5
    x = buf(sr, T)
    t = 0.05
    base = 98
    for d, vowel, step in words:
        f = base * 2 ** (step / 12)
        if rng.random() < 0.7:
            add(x, burst(sr, rng, 0.03, rng.choice((2800, 3600, 1800)), 1.4, "band", 0.008), t, 0.35, sr)
        syl = soldier(sr, rng, d, lambda u, f=f, d=d: f * (1 - 0.03 * u / d), vowel, _env(0.04, d * 0.6, d * 0.3), 1.6)
        add(x, syl, t + 0.025, 1.0, sr)
        under = voiced(sr, rng, d, lambda u, f=f: f * 0.5, vowel, 1.0, _env(0.06, d * 0.6, d * 0.3))
        add(x, rasp(under, 2.4), t + 0.04, 0.28, sr)
        if rng.random() < 0.35:
            s = svf(noise(int(sr * 0.08), rng), sr, 5200, 1.2, "band")
            add(x, env_apply(s, sr, lambda u: math.sin(math.pi * min(1.0, u / 0.08))), t + d, 0.25, sr)
        t += d + 0.05
    return reverb(x, sr, size=0.85, damp=0.5, wet=0.22, tail=1.2, predelay=0.03)


def thule_hurt(sr, rng):
    """A blow on a dead man: a short rasped grunt, dry bone and old cloth."""
    x = buf(sr, 0.6)
    g = soldier(sr, rng, 0.35, glide(120, 85, 0.35), UH, lambda t: min(1.0, t / 0.015) * math.exp(-max(0.0, t - 0.04) / 0.1))
    add(x, g, 0.0, 1.0, sr)
    add(x, burst(sr, rng, 0.04, 1100, 2.0, "band", 0.01), 0.0, 0.5, sr)
    return field(x, sr, 0.08, 0.3)


def thule_death(sr, rng):
    """He falls apart: a last groan sinking, bones clattering down, dust settling."""
    x = buf(sr, 1.8)
    g = soldier(sr, rng, 1.0, glide(105, 55, 1.0, 0.8), UH, _env(0.05, 0.4, 0.5))
    add(x, g, 0.0, 0.9, sr)
    for _ in range(12):
        at = 0.5 + rng.random() * 0.7
        add(x, burst(sr, rng, 0.03, rng.uniform(900, 2400), 3.0, "band", 0.006), at, rng.uniform(0.3, 0.7), sr)
    add(x, thud(sr, 110, 55, 0.08, 0.3), 0.55, 0.6, sr)
    dust = onepole_lp(noise(int(sr * 0.9), rng), sr, 1600)
    add(x, env_apply(dust, sr, lambda t: math.exp(-t / 0.35)), 0.6, 0.18, sr)
    return field(x, sr, 0.12, 0.6)


# --- the banshee ------------------------------------------------------------------------------------------------------

def keen(sr, rng, T, curve, vowels, breath=1.0, env=None):
    """A woman's keening voice: `curve` the pitch, cross-fading through `vowels`, with a ghostly whistle an octave over it."""
    x = buf(sr, T)
    k = len(vowels)
    for i, v in enumerate(vowels):
        seg = voiced(sr, rng, T, curve, v, breath, None)
        c = (i + 0.5) / k
        add(x, env_apply(seg, sr, lambda t, c=c: max(0.0, 1 - abs(t / T - c) * k * 0.9)), 0, 1.0, sr)
    n = len(x)
    p = 0.0
    wh = [0.0] * n
    for i in range(n):
        p += TAU * curve(i / sr) * 2 / sr
        wh[i] = math.sin(p)
    add(x, wh, 0, 0.05, sr)
    return env_apply(x, sr, env) if env else x


def banshee_wail(sr, rng):
    """The banshee's wail: a long keening note rising, trembling at the top and sinking away, echoing in the night."""
    T = 3.0

    def f0(t):
        if t < 0.8:
            f = 430 * (800 / 430) ** (t / 0.8)
        else:
            f = 800 * (560 / 800) ** min(1.0, (t - 0.8) / 2.2)
        return f * (1 + 0.035 * math.sin(TAU * 6.0 * t) * min(1.0, t / 0.6))

    x = keen(sr, rng, T, f0, (W_OO, W_EE, W_OO), 1.4, _env(0.25, 1.8, 0.95))
    y = echo(x, sr, 0.33, fb=0.4, wet=0.3, lp=2400, tail=1.0)
    return void_room(pre_swell(y, sr, 0.5, 0.3), sr, wet=0.22, tail=1.4)


def banshee_scream(sr, rng):
    """The scream that deafens: several shrieking throats at once, high and rasping, a hard noise of breath torn through it."""
    T = 1.9
    x = buf(sr, T + 0.1)
    env = _env(0.05, 1.3, 0.55)
    for k, g, d in ((1.0, 1.0, 0.0), (1.08, 0.7, 0.012), (0.93, 0.7, 0.02), (1.5, 0.3, 0.008)):
        jit = smooth_random(sr, T, rng, 20, 0.97, 1.03)
        v = voiced(sr, rng, T, lambda t, k=k, j=jit: 1000 * k * (1 + 0.05 * math.sin(TAU * 8 * t)) * j(t) *
                   (1 - 0.15 * max(0.0, t - 1.2)), W_AH, 1.6, env)
        add(x, rasp(v, 2.6), d, g, sr)
    tear = svf(noise(int(sr * T), rng), sr, 2600, 0.9, "band")
    add(x, env_apply(tear, sr, env), 0.0, 0.35, sr)
    add(x, env_apply(tone(sr, 2400, T, attack=0.08), sr, env), 0.0, 0.05, sr)
    return void_room(soft_clip(x, 1.8), sr, wet=0.2, tail=1.2)


def banshee_hurt(sr, rng):
    """A short shriek, falling."""
    T = 0.5
    x = keen(sr, rng, T, lambda t: 920 * (600 / 920) ** (t / T), (W_AH, W_EE), 1.4,
             lambda t: min(1.0, t / 0.02) * math.exp(-max(0.0, t - 0.05) / 0.14))
    return void_room(rasp(x, 1.6), sr, wet=0.14, tail=0.6, size=0.8)


def banshee_death(sr, rng):
    """She dies keening: the wail falling down and down, the tremble widening, until only breath and wind are left."""
    T = 2.4

    def f0(t):
        return 820 * (230 / 820) ** (t / T) * (1 + (0.02 + 0.06 * t / T) * math.sin(TAU * 5.5 * t))

    x = keen(sr, rng, T, f0, (W_EE, W_AH, W_OO), 1.6, _env(0.05, 1.2, 1.15))
    wind = svf(noise(int(sr * 1.8), rng), sr, lambda t: 1800 * (0.5 ** (t / 1.8)), 1.6, "band")
    add(x, env_apply(wind, sr, lambda t: math.sin(math.pi * t / 1.8)), 1.2, 0.3, sr)
    add(x, ghost_tone(sr, rng, 1.6, 600, 300, 4.0, 0.03), 1.0, 0.05, sr)
    return void_room(x, sr, wet=0.25, tail=1.6)


# --- the chitters -----------------------------------------------------------------------------------------------------

def chitter_chitter(sr, rng):
    """A chitter chitters: a man's throat clicking like an insect, in rapid bursts, mandibles ticking, a wet hiss between."""
    x = buf(sr, 1.4)
    t = 0.0
    for d, lo, hi in ((0.32, 28, 42), (0.22, 36, 55), (0.42, 24, 46)):
        c = clicks(sr, rng, d, lo, hi, THROAT, 1.0, True)
        # Under the clicks, the throat itself: a creaking vocal fry at the same slow rate.
        fry = voiced(sr, rng, d, lambda u, lo=lo, hi=hi, d=d: lo + (hi - lo) * u / d, THROAT, 0.4)
        add(c, fry, 0, 0.12, sr)
        add(x, env_apply(c, sr, lambda u, d=d: math.sin(math.pi * min(1.0, u / d)) ** 0.5), t, 1.0, sr)
        t += d + rng.uniform(0.06, 0.12)
    hiss = svf(noise(int(sr * 1.2), rng), sr, 3800, 1.4, "band")
    gate = smooth_random(sr, 1.2, rng, 8, 0.0, 1.0)
    add(x, env_apply(hiss, sr, lambda u: gate(u) ** 3), 0.05, 0.08, sr)
    return field(soft_clip(normalize(x, 0.0), 2.5), sr, 0.1, 0.4)


def chitter_screech(sr, rng):
    """The screech: a high, raw shriek from the throat, chopped by the clicking under it."""
    T = 1.2
    x = buf(sr, T + 0.1)
    env = _env(0.04, 0.75, 0.4)
    v = voiced(sr, rng, T, lambda t: 760 * (1 + 0.25 * min(1.0, t / 0.3)) * (1 - 0.2 * max(0.0, t - 0.8)), EE, 1.5, env)
    n = len(v)
    chop = [0.55 + 0.45 * math.copysign(1.0, math.sin(TAU * 42 * i / sr)) for i in range(n)]
    v = onepole_lp([a * b for a, b in zip(v, chop)], sr, 6000)
    add(x, rasp(v, 2.8), 0.0, 1.0, sr)
    add(x, env_apply(clicks(sr, rng, T, 40, 60, THROAT, 0.6, True), sr, env), 0.0, 0.6, sr)
    add(x, env_apply(svf(noise(int(sr * T), rng), sr, 3400, 1.0, "band"), sr, env), 0.0, 0.25, sr)
    return field(soft_clip(x, 1.5), sr, 0.12, 0.6)


def chitter_hurt(sr, rng):
    """A hurt chitter: a hiss through the teeth and a snarl of clicks."""
    x = buf(sr, 0.6)
    add(x, env_apply(clicks(sr, rng, 0.35, 50, 70, THROAT, 1.0, True), sr, lambda t: math.exp(-t / 0.15)), 0.0, 1.0, sr)
    v = voiced(sr, rng, 0.3, glide(560, 380, 0.3), EH, 1.6, lambda t: min(1.0, t / 0.01) * math.exp(-t / 0.1))
    add(x, rasp(v, 2.2), 0.0, 0.6, sr)
    return field(x, sr, 0.08, 0.3)


def chitter_death(sr, rng):
    """It dies: the screech choking off, the clicking slowing and stopping, a last wet breath."""
    T = 1.6
    x = buf(sr, T)
    v = voiced(sr, rng, 0.6, glide(820, 300, 0.6), EE, 1.6, _env(0.02, 0.2, 0.38))
    add(x, rasp(v, 2.4), 0.0, 0.8, sr)
    c = clicks(sr, rng, 1.2, 8, 30, THROAT, 1.0, True)
    add(x, env_apply(c, sr, lambda t: max(0.0, 1 - t / 1.2) ** 0.7), 0.3, 0.8, sr)
    br = svf(noise(int(sr * 0.5), rng), sr, 1200, 0.8, "band")
    add(x, env_apply(br, sr, lambda t: math.sin(math.pi * t / 0.5)), 1.05, 0.25, sr)
    return field(x, sr, 0.12, 0.5)


SOUNDS = {
    "casifer": {"snap": (casifer_snap, -1.0), "laugh": (casifer_laugh, -2.5)},
    "darkness": {"fog": (darkness_fog, -4.0), "release": (darkness_release, -1.0), "fog_infect": (darkness_fog_infect, -2.0),
                 "cure": (darkness_cure, -2.0), "visit": (darkness_visit, -3.0)},
    "thule": {"rise": (thule_rise, -2.0), "shoot": (thule_shoot, -1.0), "chant": (thule_chant, -2.5),
              "hurt": (thule_hurt, -2.0), "death": (thule_death, -2.0)},
    "banshee": {"wail": (banshee_wail, -2.0), "scream": (banshee_scream, -1.0), "hurt": (banshee_hurt, -2.0),
                "death": (banshee_death, -2.0)},
    "chitter": {"chitter": (chitter_chitter, -2.0), "screech": (chitter_screech, -1.5), "hurt": (chitter_hurt, -2.0),
                "death": (chitter_death, -2.0)},
}
# Seamless loops (folded by their builder): generate.py skips their fades and trimming.
LOOPS = {"darkness": ("fog",)}
