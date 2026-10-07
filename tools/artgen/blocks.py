"""Block textures: ores, floor drawings and the devil's trap."""

import math
import random

import palette as P
from common import save
from pixelkit import Tex, fbm


def stone_base(ramp, seed, contrast=1.0):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=3, scale=4.0)
            v = 2.6 + (n - 0.5) * 3.2 * contrast
            t.set(x, y, ramp[int(round(v))])
    # A few darker cracks and light flecks so it isn't plain noise.
    rng = random.Random(seed)
    for _ in range(5):
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, ramp[1])
        t.set((x + 1) % 16, y, ramp[1])
    for _ in range(6):
        t.set(rng.randrange(16), rng.randrange(16), ramp[4])
    return t


def ore(base_ramp, gem, seed, clusters):
    t = stone_base(base_ramp, seed)
    for cx, cy, shape in clusters:
        for dy, row in enumerate(shape):
            for dx, ch in enumerate(row):
                if ch == ".":
                    continue
                c = {"o": gem[0], "d": gem[1], "m": gem[3], "l": gem[4], "W": gem[5]}[ch]
                t.set(cx + dx, cy + dy, c)
    return t


CRYSTAL_A = [".o..", "oWlo", "olmd", ".od."]
CRYSTAL_B = ["oWo", "lmd", ".o."]
CRYSTAL_C = [".oo.", "oWlo", "olmd", "odd.", ".o.."]


def rock_salt_ore():
    return ore(P.STONE, P.ROSE_SALT, 11, [(2, 2, CRYSTAL_A), (9, 4, CRYSTAL_C), (4, 10, CRYSTAL_B), (11, 11, CRYSTAL_B)])


def deepslate_rock_salt_ore():
    return ore(P.DEEPSLATE, P.ROSE_SALT, 12, [(3, 1, CRYSTAL_C), (10, 3, CRYSTAL_B), (2, 10, CRYSTAL_A), (10, 10, CRYSTAL_A)])


def nether_sulfur_ore():
    return ore(P.NETHERRACK, P.SULFUR, 13, [(1, 3, CRYSTAL_A), (8, 1, CRYSTAL_B), (9, 8, CRYSTAL_C), (3, 11, CRYSTAL_B)])


# --- floor lines ------------------------------------------------------------------------

def _rough(t, ramp, cells, seed):
    rng = random.Random(seed)
    for x, y in cells:
        n = fbm(x, y, seed, 16, 16, octaves=2, scale=8.0)
        t.set(x, y, ramp[2 + int(round(n * 3))] if rng.random() > 0.08 else ramp[1])


def line_dot(ramp, seed, grain=False):
    t = Tex(16, 16, seed)
    cells = []
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            wobble = (fbm(x, y, seed, 16, 16, 2, 6.0) - 0.5) * 1.4
            if d < 2.9 + wobble:
                cells.append((x, y))
    _rough(t, ramp, cells, seed)
    if grain:
        _grain(t, ramp, seed)
    return t


def line_side(ramp, seed, grain=False):
    t = Tex(16, 16, seed)
    cells = []
    for y in range(0, 9):
        for x in range(16):
            wobble = (fbm(x, y, seed, 16, 16, 2, 6.0) - 0.5) * 1.2
            if abs(x + 0.5 - 8) < 1.9 + wobble:
                cells.append((x, y))
    _rough(t, ramp, cells, seed)
    if grain:
        _grain(t, ramp, seed)
    return t


def _grain(t, ramp, seed):
    """Salt is poured, not drawn: scatter loose grains around the stroke."""
    rng = random.Random(seed * 7)
    rows = t.copy_rows()
    for y in range(16):
        for x in range(16):
            if rows[y][x][3]:
                for _ in range(1):
                    nx, ny = x + rng.randint(-2, 2), y + rng.randint(-2, 2)
                    if 0 <= nx < 16 and 0 <= ny < 16 and not rows[ny][nx][3] and rng.random() < 0.18:
                        t.set(nx, ny, ramp[rng.randint(3, 5)])


LINES = {
    "salt_line": (P.SALT, True),
    "chalk_line": (P.CHALK, False),
    "blood_chalk_line": (P.BLOOD, False),
}


# --- devil's trap -------------------------------------------------------------------------

def _seg_dist(px, py, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    wx, wy = px - ax, py - ay
    t = max(0.0, min(1.0, (wx * vx + wy * vy) / (vx * vx + vy * vy)))
    return math.hypot(px - (ax + vx * t), py - (ay + vy * t))


def devils_trap_full():
    """The full 48x48 sigil: two rings, a pentagram, glyphs between the rings."""
    S = 48
    c = S / 2
    t = Tex(S, S, 66)
    paint = P.BLOOD
    r_out, r_mid, r_star = 22.6, 18.4, 17.6
    star = [(c + r_star * math.cos(math.radians(-90 + k * 72)), c + r_star * math.sin(math.radians(-90 + k * 72)))
            for k in range(5)]
    rng = random.Random(9)
    glyph_marks = []
    # Glyph strokes sit in the band between the rings, five groups between the star points.
    for k in range(5):
        a0 = math.radians(-90 + 36 + k * 72)
        for j in range(4):
            a = a0 + math.radians((j - 1.5) * 9)
            r = (r_out + r_mid) / 2
            gx, gy = c + r * math.cos(a), c + r * math.sin(a)
            ang = rng.uniform(0, math.pi)
            ln = rng.uniform(1.0, 1.8)
            glyph_marks.append((gx - ln * math.cos(ang), gy - ln * math.sin(ang), gx + ln * math.cos(ang), gy + ln * math.sin(ang)))
    for y in range(S):
        for x in range(S):
            px, py = x + 0.5, y + 0.5
            d = math.hypot(px - c, py - c)
            on = abs(d - r_out) < 0.95 or abs(d - r_mid) < 0.8 or d < 1.6
            if not on and d < r_mid:
                for k in range(5):
                    a, b = star[k], star[(k + 2) % 5]
                    if _seg_dist(px, py, a[0], a[1], b[0], b[1]) < 0.85:
                        on = True
                        break
            if not on and r_mid < d < r_out:
                for m in glyph_marks:
                    if _seg_dist(px, py, *m) < 0.7:
                        on = True
                        break
            if on:
                n = fbm(x, y, 5, S, S, octaves=2, scale=6.0)
                shade = 2 + int(round(n * 2.6))
                if rng.random() < 0.06:
                    shade = 1
                t.set(x, y, paint[shade])
    return t


def devils_trap_parts():
    full = devils_trap_full()
    parts = []
    for part in range(9):
        ox, oy = (part % 3) * 16, (part // 3) * 16
        t = Tex(16, 16)
        for y in range(16):
            for x in range(16):
                t.set(x, y, full.rows[oy + y][ox + x])
        parts.append(t)
    return full, parts


# --- ritual altar -------------------------------------------------------------------------

ALTAR_STONE = P.DEEPSLATE


def _bricks(seed, rows=4):
    """Dark dressed-stone bricks, offset every other course."""
    t = Tex(16, 16, seed)
    for y in range(16):
        course = y // rows
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 2, 6.0)
            v = 2.8 + (n - 0.5) * 1.6
            off = (course % 2) * 4
            if y % rows == 0 or (x + off) % 8 == 0:
                v = 1.0
            elif y % rows == 1 or (x + off) % 8 == 1:
                v += 0.8
            t.set(x, y, ALTAR_STONE[int(round(v))])
    return t


def altar_side():
    t = _bricks(71)
    # A gold band and a column of red runes down the middle.
    for x in range(16):
        t.set(x, 2, P.GOLD[2] if x % 3 else P.GOLD[3])
        t.set(x, 13, P.GOLD[1])
    for (x, y) in ((7, 5), (8, 5), (7, 6), (8, 8), (7, 9), (8, 9), (7, 11), (8, 10)):
        t.set(x, y, P.BLOOD[4] if (x + y) % 2 else P.BLOOD[3])
    return t


def altar_rim():
    t = _bricks(72, 8)
    for x in range(16):
        t.set(x, 0, P.GOLD[3])
        t.set(x, 1, P.GOLD[2])
    return t


def altar_top():
    t = _bricks(73, 8)
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                t.set(x, y, P.GOLD[2])
    return t


def altar_bottom():
    return stone_base(ALTAR_STONE, 74, 0.7)


def altar_bowl():
    """The basin: old blood dried into the stone, a faint pentagram scratched underneath."""
    t = stone_base(ALTAR_STONE, 75, 0.6)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 5.5:
                n = fbm(x, y, 76, 16, 16, 2, 5.0)
                t.set(x, y, P.BLOOD[1 + int(round(n * 2.2))])
            elif d < 6.3:
                t.set(x, y, ALTAR_STONE[0])
    for k in range(5):
        a0 = math.radians(-90 + k * 72)
        a1 = math.radians(-90 + ((k + 2) % 5) * 72)
        for i in range(12):
            s = i / 11
            x = 8 + 4.6 * ((1 - s) * math.cos(a0) + s * math.cos(a1))
            y = 8 + 4.6 * ((1 - s) * math.sin(a0) + s * math.sin(a1))
            t.set(int(x), int(y), P.BLOOD[4])
    return t


def generate():
    save(altar_side(), "block", "ritual_altar_side")
    save(altar_rim(), "block", "ritual_altar_rim")
    save(altar_top(), "block", "ritual_altar_top")
    save(altar_bottom(), "block", "ritual_altar_bottom")
    save(altar_bowl(), "block", "ritual_altar_bowl")
    save(rock_salt_ore(), "block", "rock_salt_ore")
    save(deepslate_rock_salt_ore(), "block", "deepslate_rock_salt_ore")
    save(nether_sulfur_ore(), "block", "nether_sulfur_ore")
    for i, (name, (ramp, grain)) in enumerate(LINES.items()):
        save(line_dot(ramp, 30 + i, grain), "block", name + "_dot")
        save(line_side(ramp, 40 + i, grain), "block", name + "_side")
    _, parts = devils_trap_parts()
    for i, t in enumerate(parts):
        save(t, "block", f"devils_trap_{i}")
