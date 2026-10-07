"""Effect textures: attack telegraphs, the arena dome, and the arena's own floor blocks.

Effect textures are white with alpha; the renderer tints them per attack and per phase.
"""

import math
import random

import palette as P
from blocks import stone_base
from common import save
from pixelkit import Tex, fbm, hexc, mix

W = 255


def _a(alpha):
    return (255, 255, 255, max(0, min(255, int(alpha))))


def telegraph_circle():
    S = 64
    t = Tex(S, S)
    c = S / 2
    for y in range(S):
        for x in range(S):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            if d > 1:
                continue
            a = 70 + 60 * d * d
            if d > 0.9:
                a = 245
            elif 0.72 < d < 0.76:
                a = 190
            ang = math.atan2(y + 0.5 - c, x + 0.5 - c)
            if 0.78 < d < 0.88 and int((ang + math.pi) / (2 * math.pi) * 24) % 2 == 0:
                a = 160
            t.set(x, y, _a(a))
    return t


def telegraph_ring():
    S = 64
    t = Tex(S, S)
    c = S / 2
    for y in range(S):
        for x in range(S):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            if 0.9 < d <= 1.0:
                t.set(x, y, _a(240))
            elif 0.8 < d <= 0.9:
                ang = math.atan2(y + 0.5 - c, x + 0.5 - c)
                t.set(x, y, _a(150 if int((ang + math.pi) / (2 * math.pi) * 32) % 2 else 60))
    return t


def telegraph_line():
    Wd, H = 32, 64
    t = Tex(Wd, H)
    for y in range(H):
        for x in range(Wd):
            edge = min(x, Wd - 1 - x)
            a = 80
            if edge < 2:
                a = 240
            # Chevrons pointing down the strip (the direction of travel is +v).
            if (y + abs(x - Wd / 2) * 0.8) % 16 < 3:
                a = max(a, 170)
            t.set(x, y, _a(a))
    return t


def telegraph_cone():
    Wd, H = 64, 32
    t = Tex(Wd, H)
    ax, ay = Wd / 2, 0
    for y in range(H):
        for x in range(Wd):
            dx, dy = x + 0.5 - ax, y + 0.5 - ay
            d = math.hypot(dx, dy) / H
            ang = math.degrees(math.atan2(dx, dy))
            if d > 1 or abs(ang) > 60:
                continue
            a = 70 + 70 * d
            if d > 0.92 or abs(ang) > 56:
                a = 240
            t.set(x, y, _a(a))
    return t


def arena_dome():
    S = 32
    t = Tex(S, S, 5)
    rng = random.Random(5)
    runes = {(rng.randrange(S), rng.randrange(8, S - 4)) for _ in range(28)}
    for y in range(S):
        for x in range(S):
            a = 40 + 120 * (y / S) ** 1.5
            if x % 8 == 0:
                a += 60
            if (x, y) in runes or (x - 1, y) in runes:
                a += 110
            n = fbm(x, y, 7, S, S, 2, 4.0)
            a *= 0.8 + 0.4 * n
            t.set(x, y, _a(a))
    return t


# --- arena floor --------------------------------------------------------------------------

def hellfire_crack():
    t = stone_base(P.ASH, 81, 1.2)
    rng = random.Random(82)
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(9):
            t.set(x % 16, y % 16, P.HELLFIRE[4] if rng.random() < 0.7 else P.HELLFIRE[5])
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if rng.random() < 0.3:
                    t.set(nx % 16, ny % 16, P.HELLFIRE[2])
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1, 1))
    return t


def cage_frost():
    t = stone_base(P.CAGE, 83, 0.9)
    rng = random.Random(84)
    for _ in range(18):
        t.set(rng.randrange(16), rng.randrange(16), P.FROST[5])
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        for k in range(3):
            t.set((x + k) % 16, (y + k) % 16, P.FROST[4])
    return t


def cage_ice():
    t = Tex(16, 16, 85)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 85, 16, 16, 2, 4.0)
            c = mix(P.FROST[3], P.FROST[4], n)
            t.set(x, y, (c[0], c[1], c[2], 190))
    rng = random.Random(86)
    for _ in range(5):
        x = rng.randrange(16)
        for y in range(rng.randrange(4), 16, 2):
            t.set((x + y // 5) % 16, y, (240, 250, 255, 230))
    for i in range(16):
        t.set(i, 0, (230, 245, 255, 230))
        t.set(0, i, (230, 245, 255, 230))
    return t


def seraphic_pillar():
    t = Tex(16, 16, 87)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 87, 16, 16, 3, 5.0)
            t.set(x, y, mix(hexc("#f4efe6"), hexc("#d9d0c2"), n))
    rng = random.Random(88)
    for _ in range(3):
        x, y = rng.randrange(16), 0
        while y < 16:
            t.set(x % 16, y, P.GOLD[4] if rng.random() < 0.6 else P.GOLD[5])
            x += rng.choice((-1, 0, 1))
            y += 1
    return t


def trophy_base():
    t = stone_base(P.DEEPSLATE, 91, 0.8)
    for x in range(16):
        t.set(x, 13, P.GOLD[3])
        t.set(x, 15, P.GOLD[1])
    return t


def trophy_figure():
    t = Tex(16, 16, 92)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 92, 16, 16, 2, 6.0)
            t.set(x, y, P.GOLD[int(round(2.5 + n * 2.5))])
    for x in range(12, 15):
        for y in range(0, 3):
            t.set(x, y, P.GRACE[5])
    t.set(1, 2, P.BLOOD[5])
    t.set(2, 2, P.BLOOD[5])
    return t


def trophy_wing():
    t = Tex(16, 16, 93)
    for y in range(16):
        for x in range(16):
            col = x // 2
            length = 6 + (col % 3) if y < 8 else 12 + (col % 2)
            top = 0 if y < 8 else 8
            if y - top < length - top and (y < 7 or y >= 8):
                t.set(x, y, P.GRACE[5 if x % 2 else 3])
    return t


def beam():
    """A white beam texture: bright core, soft falloff, faint pulses along its length (tiles in v)."""
    Wd, H = 16, 32
    t = Tex(Wd, H)
    for y in range(H):
        for x in range(Wd):
            d = abs(x + 0.5 - Wd / 2) / (Wd / 2)
            a = (1 - d) ** 1.6 * 255
            if (y // 4) % 2 == 0:
                a *= 0.85
            t.set(x, y, _a(a))
    return t


def soul_crescent():
    """A crescent of soul-light, open toward the back (v=1)."""
    Wd, H = 64, 32
    t = Tex(Wd, H)
    for y in range(H):
        for x in range(Wd):
            dx, dy = (x + 0.5 - Wd / 2) / (Wd / 2), (y + 0.5) / H
            outer = dx * dx + (dy * 1.0) ** 2
            inner = dx * dx + ((dy - 0.35) * 1.3) ** 2
            if outer <= 1.0 and inner >= 0.55:
                a = 255 * (1 - abs(outer - 0.75))
                t.set(x, y, _a(max(80, a)))
    return t


def eclipse_corona():
    """The eclipsed sun's corona, drawn additively behind a black disc of radius 0.4 (in half-sizes).

    Pearly light thinning outwards in uneven streamers, faintly violet at the edge, with the
    'diamond ring': one blinding bead where the last of the sun slips past the disc.
    """
    N = 128
    t = Tex(N, N)
    rnd = random.Random(1999)
    streamers = [(rnd.uniform(0, math.tau), rnd.uniform(0.08, 0.22), rnd.uniform(0.25, 0.6)) for _ in range(14)]
    bead = (math.cos(math.radians(-40)) * 0.45, math.sin(math.radians(-40)) * 0.45)
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5) / N * 2 - 1, (y + 0.5) / N * 2 - 1
            r = math.hypot(dx, dy)
            if r < 0.36 or r > 1:
                continue
            ang = math.atan2(dy, dx)
            reach = 0.55
            for a0, width, length in streamers:
                d = abs((ang - a0 + math.pi) % math.tau - math.pi)
                if d < width:
                    reach = max(reach, 0.55 + length * (1 - d / width))
            fall = max(0.0, 1 - (r - 0.38) / (reach - 0.38)) if r < reach else 0.0
            a = 255 * fall ** 1.8
            violet = min(1.0, (r - 0.38) / 0.5)
            col = mix(hexc("#fff8ec"), hexc("#b9a2ff"), violet)
            bd = math.hypot(dx - bead[0], dy - bead[1])
            if bd < 0.12:
                b = (1 - bd / 0.12) ** 2
                col = mix(col, (255, 255, 255, 255), b)
                a = max(a, 255 * b)
            if a > 2:
                t.set(x, y, tuple(max(0, min(255, int(c))) for c in col[:3]) + (int(min(255, a)),))
    return t


def generate():
    save(eclipse_corona(), "environment", "eclipse_corona")
    save(beam(), "effect", "beam")
    save(soul_crescent(), "effect", "soul_crescent")
    save(trophy_base(), "block", "trophy_base")
    save(trophy_figure(), "block", "trophy_figure")
    save(trophy_wing(), "block", "trophy_wing")
    save(telegraph_circle(), "effect", "telegraph_circle")
    save(telegraph_ring(), "effect", "telegraph_ring")
    save(telegraph_line(), "effect", "telegraph_line")
    save(telegraph_cone(), "effect", "telegraph_cone")
    save(arena_dome(), "effect", "arena_dome")
    save(hellfire_crack(), "block", "hellfire_crack")
    save(cage_frost(), "block", "cage_frost")
    save(cage_ice(), "block", "cage_ice")
    save(seraphic_pillar(), "block", "seraphic_pillar")
