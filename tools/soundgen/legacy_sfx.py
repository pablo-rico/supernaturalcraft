"""v0.17 the Men of Letters: the bunker, the research and the three monsters of their files. Same conventions as sfx.py:
(sr, rng) -> float list, finished and encoded by generate.py into sounds/legacy/<id>.ogg (LegacyAssetData points
supernaturalcraft:legacy.<id> at them; the ids are LegacyAssets.SOUNDS).

The bunker is steel and paper: the vault door's wheel ratcheting and its bolts thrown, the hum of the lit map table,
typewriters and rustling files, a warm bell when research is done and a brass chord for a new rank. The monsters are
voices: the vampire's breath and hiss, the werewolf's growl and howl, the shapeshifter's mutter and the wet tearing of
its skin.
"""

import math

from horsemen_sfx import AH, EE, OH, growl, wheeze
from michael_sfx import hall, ring, trumpet, whoosh
from sfx import HUM, UH, bell_voice, burst, page_tear, ratchet_click, thud, typekey, voiced
from synth import (add, buf, env_apply, midi_hz, noise, onepole_lp, partials, reverb, smooth_random, soft_clip, svf, tone)

OO = [(320, 3.0, 1.0), (800, 4.0, 0.3), (2400, 7.0, 0.08)]


def _env(a, s, r):
    def f(t):
        if t < a:
            return t / a if a else 1.0
        if t < a + s:
            return 1.0
        return max(0.0, 1 - (t - a - s) / r) if r else 0.0
    return f


def hiss_noise(sr, rng, T, fc=4200, q=0.8, env=None):
    x = svf(noise(int(sr * T), rng), sr, fc, q, "band")
    return env_apply(x, sr, env or _env(0.05, T * 0.6, T * 0.4 - 0.05))


def squelch(sr, rng, T, f0=300, wet=1.0):
    """Wet flesh: low gurgling bubbles over a slick band of noise."""
    x = buf(sr, T)
    t = 0.0
    while t < T - 0.05:
        f = f0 * rng.uniform(0.7, 1.5)
        add(x, tone(sr, f, 0.06, decay=0.02, glide=f * rng.uniform(1.3, 2.2), attack=0.002), t, 0.35 * wet, sr)
        t += rng.expovariate(1 / 0.03)
    slick = svf(noise(int(sr * T), rng), sr, smooth_random(sr, T, rng, 20, 600, 1800), 2.5, "band")
    add(x, env_apply(slick, sr, _env(0.02, T * 0.5, T * 0.5 - 0.02)), 0, 0.5 * wet, sr)
    return x


def bone_crack(sr, rng, gain=1.0):
    x = buf(sr, 0.12)
    add(x, burst(sr, rng, 0.02, 2600, 0.9, "high", 0.002), 0, gain, sr)
    add(x, burst(sr, rng, 0.06, 900, 2.0, "band", 0.012), 0.002, 0.7 * gain, sr)
    add(x, thud(sr, 180, 90, 0.02, 0.1), 0, 0.4 * gain, sr)
    return x


# --- the bunker ---------------------------------------------------------------------------------------------------

def bunker_door(sr, rng):
    """The vault door: the wheel spun on its ratchet, the locking bolts thrown back one after another, the heavy door
    swinging on its hinge with a long steel groan, and the boom of it settling."""
    T = 3.4
    x = buf(sr, T)
    t = 0.0
    while t < 1.0:
        add(x, ratchet_click(sr, rng, 0.5 + 0.4 * rng.random()), t, 0.7, sr)
        add(x, burst(sr, rng, 0.02, 900, 3.0, "band", 0.006), t, 0.25, sr)
        t += 0.055 + 0.02 * math.sin(t * 7)
    for i, at in enumerate((1.05, 1.22, 1.36, 1.48)):
        add(x, thud(sr, 120 - i * 6, 70, 0.06, 0.3), at, 0.9, sr)
        add(x, burst(sr, rng, 0.08, 1800, 1.5, "band", 0.02), at, 0.7, sr)
        add(x, partials(sr, 0.6, [(520 + 30 * i, 0.15, 0.25), (1310, 0.08, 0.15), (2470, 0.05, 0.1)]), at, 1.0, sr)
    groan = svf(noise(int(sr * 1.4), rng), sr, smooth_random(sr, 1.4, rng, 4, 180, 520), 9.0, "band")
    add(x, env_apply(groan, sr, _env(0.2, 0.8, 0.4)), 1.7, 1.6, sr)
    add(x, thud(sr, 70, 40, 0.3, 0.8), 3.0, 1.0, sr)
    add(x, burst(sr, rng, 0.3, 300, 1.0, "low", 0.12), 3.0, 0.6, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.85, damp=0.45, wet=0.25, tail=1.0)


def map_table(sr, rng):
    """The war room's table lighting up: a relay's clunk, the warm hum of old lamps under glass coming up to strength."""
    T = 2.2
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.03, 1800, 1.4, "band", 0.006), 0, 0.8, sr)
    add(x, thud(sr, 160, 110, 0.03, 0.15), 0, 0.5, sr)
    for k, a in ((60, 1.0), (120, 0.6), (180, 0.3), (240, 0.15)):
        h = tone(sr, k, T - 0.05, attack=0.4)
        add(x, env_apply(h, sr, _env(0.5, T - 1.0, 0.45)), 0.05, 0.12 * a, sr)
    add(x, wheeze(sr, rng, T - 0.2, 2400, 3600, 0.05), 0.1, 1.0, sr)
    return reverb(x, sr, size=0.6, damp=0.5, wet=0.15, tail=0.5)


def typewriter(sr, rng):
    """A few words typed in a hurry and the margin bell."""
    T = 1.6
    x = buf(sr, T)
    t = 0.0
    for i in range(9):
        add(x, typekey(sr, rng, rng.uniform(0.92, 1.1), rng.uniform(0.9, 1.08), rng.uniform(0.8, 1.2)), t, 0.8, sr)
        t += rng.uniform(0.07, 0.14) + (0.12 if i == 4 else 0)
    add(x, bell_voice(sr, 1480, 0.5), t + 0.05, 0.25, sr)
    return x


def paper(sr, rng):
    """Files leafed through: a page turned, a sheet slid off the stack."""
    x = buf(sr, 0.9)
    add(x, page_tear(sr, rng, 1), 0.0, 0.45, sr)
    sl = svf(noise(int(sr * 0.35), rng), sr, 2600, 0.7, "band")
    add(x, env_apply(sl, sr, _env(0.05, 0.1, 0.2)), 0.45, 0.35, sr)
    return x


def research_done(sr, rng):
    """A discovery: the typewriter's bell, a soft major chord on bells and a pen's last flourish."""
    T = 2.6
    x = buf(sr, T)
    add(x, bell_voice(sr, 1480, 1.5), 0.0, 0.5, sr)
    for i, m in enumerate((67, 71, 74, 79)):
        add(x, bell_voice(sr, midi_hz(m), 2.0, bright=0.6, beat=0.8), 0.12 + i * 0.09, 0.22, sr)
    add(x, page_tear(sr, rng, 1), 0.05, 0.12, sr)
    return hall(x, sr, 0.25, 0.9)


def rank_up(sr, rng):
    """The Legacy welcomes you: a warm brass chord swelling under a bell, held, a low bell to close."""
    T = 4.0
    x = buf(sr, T)
    for m, g in ((48, 0.5), (55, 0.45), (60, 0.45), (64, 0.4)):
        b = trumpet(sr, rng, m, 2.8, vib=4.5)
        add(x, env_apply(b, sr, _env(0.5, 1.6, 0.7)), 0.15, g * 0.6, sr)
    add(x, bell_voice(sr, midi_hz(72), 2.5, bright=0.7), 0.0, 0.35, sr)
    add(x, bell_voice(sr, midi_hz(48), 3.0, bright=0.5, beat=0.6), 2.4, 0.5, sr)
    return hall(soft_clip(x, 1.1), sr, 0.3, 1.5)


def henry_greet(sr, rng):
    """Henry clears his throat, politely: two short voiced "hm-hm"s and a breath."""
    x = buf(sr, 1.0)
    for i, (t, f) in enumerate(((0.0, 128), (0.22, 116))):
        d = 0.16
        br = svf(noise(int(sr * 0.05), rng), sr, 1400, 0.8, "band")
        add(x, env_apply(br, sr, lambda tt: math.exp(-tt / 0.015)), t, 0.4, sr)
        v = voiced(sr, rng, d, lambda tt, f=f: f * (1 - 0.1 * tt / d), HUM, 0.7, lambda tt: min(1.0, tt / 0.012) * math.exp(-tt / 0.06))
        add(x, v, t + 0.01, 1.0, sr)
    add(x, onepole_lp(env_apply(noise(int(sr * 0.3), rng), sr, _env(0.05, 0.05, 0.2)), sr, 900), 0.55, 0.15, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.08, tail=0.25)


# --- the vampire ----------------------------------------------------------------------------------------------------

def vampire_ambient(sr, rng):
    """A slow, rasping breath taken through the teeth -- scenting."""
    T = 2.0
    x = buf(sr, T)
    for at, d in ((0.0, 0.7), (0.9, 0.5)):
        add(x, hiss_noise(sr, rng, d, 3000, 1.6, _env(0.2, 0.2, d - 0.4)), at, 0.4, sr)
        add(x, growl(sr, rng, d, 70, 62, UH, 0.9, 0.9), at, 0.18, sr)
    return reverb(x, sr, size=0.5, damp=0.5, wet=0.12, tail=0.4)


def vampire_hiss(sr, rng):
    """The hiss with the fangs out: a hard rush of air through bared teeth over a low snarl."""
    T = 1.1
    x = buf(sr, T)
    add(x, hiss_noise(sr, rng, T, 5200, 0.9, _env(0.03, 0.5, 0.55)), 0, 1.0, sr)
    add(x, hiss_noise(sr, rng, T, 2600, 2.0, _env(0.05, 0.4, 0.6)), 0, 0.4, sr)
    add(x, growl(sr, rng, 0.9, 92, 78, AH, 1.0, 0.8), 0.04, 0.35, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.4, damp=0.5, wet=0.1, tail=0.3)


def vampire_bite(sr, rng):
    """Teeth go in: a snap of the jaw, a wet crunch, a swallow."""
    T = 0.9
    x = buf(sr, T)
    add(x, burst(sr, rng, 0.03, 2200, 0.9, "high", 0.003), 0, 0.9, sr)
    add(x, thud(sr, 220, 120, 0.03, 0.12), 0, 0.6, sr)
    add(x, squelch(sr, rng, 0.45, 260), 0.03, 0.9, sr)
    add(x, thud(sr, 110, 70, 0.08, 0.25), 0.55, 0.5, sr)
    return reverb(x, sr, size=0.3, damp=0.5, wet=0.08, tail=0.2)


def vampire_hurt(sr, rng):
    x = buf(sr, 0.6)
    add(x, growl(sr, rng, 0.45, 150, 110, AH, 1.1, 0.7), 0, 1.0, sr)
    add(x, hiss_noise(sr, rng, 0.35, 4200, 1.0, _env(0.01, 0.1, 0.24)), 0, 0.5, sr)
    return x


def vampire_death(sr, rng):
    """A shriek that chokes off into a gurgle."""
    T = 2.0
    x = buf(sr, T)
    add(x, growl(sr, rng, 0.9, 320, 180, EE, 1.2, 0.6), 0, 0.8, sr)
    add(x, hiss_noise(sr, rng, 0.9, 5000, 0.8, _env(0.02, 0.4, 0.5)), 0, 0.5, sr)
    add(x, squelch(sr, rng, 0.8, 180, 0.8), 0.8, 0.7, sr)
    add(x, growl(sr, rng, 0.8, 90, 50, UH, 1.4, 0.9), 0.85, 0.4, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.5, damp=0.5, wet=0.15, tail=0.5)


# --- the werewolf ----------------------------------------------------------------------------------------------------

def werewolf_ambient(sr, rng):
    """Heavy panting with a growl under it."""
    T = 1.8
    x = buf(sr, T)
    t = 0.0
    for i in range(5):
        d = 0.16
        p = svf(noise(int(sr * d), rng), sr, 1100 if i % 2 else 1500, 1.4, "band")
        add(x, env_apply(p, sr, lambda tt: math.sin(math.pi * tt / d)), t, 0.5, sr)
        t += 0.3
    add(x, growl(sr, rng, T, 62, 58, OH, 1.0, 0.5), 0, 0.25, sr)
    return x


def werewolf_howl(sr, rng):
    """The howl: rising from a growl to a long, wavering, falling cry."""
    T = 3.4

    def f0(t):
        if t < 0.5:
            return 180 + 260 * (t / 0.5)
        if t < 2.4:
            return 440 + 18 * math.sin(t * 9) - 30 * (t - 0.5) / 1.9
        return 410 * (0.55 ** ((t - 2.4) / 1.0))
    x = buf(sr, T)
    v = voiced(sr, rng, T, f0, OO, 0.5, _env(0.3, 2.2, 0.9))
    add(x, v, 0, 1.0, sr)
    add(x, voiced(sr, rng, T, lambda t: f0(t) * 1.006, OH, 0.5, _env(0.3, 2.2, 0.9)), 0.01, 0.5, sr)
    add(x, growl(sr, rng, 0.6, 90, 120, OH, 1.0, 0.5), 0, 0.35, sr)
    return reverb(x, sr, size=0.95, damp=0.3, wet=0.35, tail=1.8)


def werewolf_growl(sr, rng):
    T = 1.4
    x = growl(sr, rng, T, 70, 64, OH, 1.4, 0.5)
    trem = smooth_random(sr, T, rng, 9, 0.55, 1.0)
    x = [v * trem(i / sr) for i, v in enumerate(x)]
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.1, tail=0.3)


def werewolf_hurt(sr, rng):
    """A yelp."""
    x = buf(sr, 0.5)
    add(x, voiced(sr, rng, 0.3, lambda t: 620 * (0.5 ** (t / 0.3)), EE, 0.4, _env(0.01, 0.08, 0.2)), 0, 1.0, sr)
    add(x, growl(sr, rng, 0.3, 120, 90, AH, 1.0, 0.6), 0.05, 0.4, sr)
    return x


def werewolf_death(sr, rng):
    """A long dying whine sinking into a last growl."""
    T = 2.4
    x = buf(sr, T)
    add(x, voiced(sr, rng, 1.6, lambda t: 520 * (0.45 ** (t / 1.6)) * (1 + 0.03 * math.sin(t * 30)), OO, 0.6, _env(0.05, 0.8, 0.75)), 0, 0.9,
        sr)
    add(x, growl(sr, rng, 1.0, 70, 40, OH, 1.4, 0.9), 1.3, 0.5, sr)
    return reverb(x, sr, size=0.6, damp=0.4, wet=0.2, tail=0.6)


def werewolf_turn(sr, rng):
    """The change: bones cracking one after another, a groan climbing into a snarl."""
    T = 2.8
    x = buf(sr, T)
    t = 0.1
    while t < 2.2:
        add(x, bone_crack(sr, rng, rng.uniform(0.6, 1.0)), t, 0.8, sr)
        t += rng.uniform(0.08, 0.25)
    add(x, growl(sr, rng, 1.2, 110, 150, UH, 0.8, 0.6), 0.2, 0.5, sr)
    add(x, growl(sr, rng, 1.2, 90, 75, AH, 1.6, 0.6), 1.4, 0.8, sr)
    add(x, squelch(sr, rng, 1.6, 200, 0.5), 0.4, 0.4, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.6, damp=0.45, wet=0.15, tail=0.5)


# --- the shapeshifter --------------------------------------------------------------------------------------------------

def shapeshifter_ambient(sr, rng):
    """It mutters in a voice that is not quite its own: a few low syllables, slightly off pitch."""
    T = 1.6
    x = buf(sr, T)
    t = 0.05
    for i in range(4):
        d = rng.uniform(0.12, 0.22)
        f = rng.uniform(100, 135)
        v = voiced(sr, rng, d, lambda tt, f=f, d=d: f * (1 + 0.15 * math.sin(tt / d * 3.0)), (UH, HUM, AH)[i % 3], 0.5,
                   lambda tt, d=d: math.sin(math.pi * min(1.0, tt / d)))
        add(x, v, t, 0.8, sr)
        t += d + rng.uniform(0.05, 0.15)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.1, tail=0.3)


def shapeshifter_shed(sr, rng):
    """Skin sloughing off: a long wet peel, tearing, slime dropping to the floor."""
    T = 2.2
    x = buf(sr, T)
    add(x, squelch(sr, rng, T - 0.4, 240), 0, 1.0, sr)
    tear = svf(noise(int(sr * 1.2), rng), sr, smooth_random(sr, 1.2, rng, 25, 900, 2400), 1.5, "band")
    add(x, env_apply(tear, sr, _env(0.1, 0.6, 0.5)), 0.3, 0.6, sr)
    for at in (1.5, 1.75, 1.9):
        add(x, thud(sr, 140, 80, 0.04, 0.15), at, 0.5, sr)
        add(x, squelch(sr, rng, 0.15, 320, 0.7), at, 0.5, sr)
    return reverb(x, sr, size=0.4, damp=0.5, wet=0.1, tail=0.3)


def shapeshifter_hurt(sr, rng):
    x = buf(sr, 0.55)
    add(x, growl(sr, rng, 0.4, 150, 105, UH, 0.8, 0.6), 0, 1.0, sr)
    add(x, squelch(sr, rng, 0.2, 260, 0.5), 0.05, 0.4, sr)
    return x


def shapeshifter_death(sr, rng):
    """A scream that keeps changing voice, ending in a wet collapse."""
    T = 2.0
    x = buf(sr, T)
    for i, (at, f0, f1, vw) in enumerate(((0.0, 260, 330, AH), (0.35, 420, 300, EE), (0.7, 160, 90, UH))):
        add(x, growl(sr, rng, 0.5, f0, f1, vw, 1.0, 0.5), at, 0.7, sr)
    add(x, squelch(sr, rng, 0.7, 200), 1.15, 0.8, sr)
    add(x, thud(sr, 90, 50, 0.15, 0.5), 1.4, 0.7, sr)
    return reverb(soft_clip(x, 1.2), sr, size=0.5, damp=0.5, wet=0.15, tail=0.5)


SOUNDS = {
    "bunker_door": (bunker_door, -1.0), "map_table": (map_table, -4.0), "typewriter": (typewriter, -3.0), "paper": (paper, -4.0),
    "research_done": (research_done, -2.0), "rank_up": (rank_up, -1.5), "henry_greet": (henry_greet, -3.0),
    "vampire_ambient": (vampire_ambient, -5.0), "vampire_hiss": (vampire_hiss, -1.5), "vampire_bite": (vampire_bite, -2.0),
    "vampire_hurt": (vampire_hurt, -2.0), "vampire_death": (vampire_death, -1.5),
    "werewolf_ambient": (werewolf_ambient, -4.0), "werewolf_howl": (werewolf_howl, -1.0), "werewolf_growl": (werewolf_growl, -2.0),
    "werewolf_hurt": (werewolf_hurt, -2.0), "werewolf_death": (werewolf_death, -1.5), "werewolf_turn": (werewolf_turn, -1.5),
    "shapeshifter_ambient": (shapeshifter_ambient, -5.0), "shapeshifter_shed": (shapeshifter_shed, -2.0),
    "shapeshifter_hurt": (shapeshifter_hurt, -2.0), "shapeshifter_death": (shapeshifter_death, -1.5),
}
