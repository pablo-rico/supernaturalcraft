"""Graves of the restless dead: the bones in their shallow grave (as found, salted, and burnt to rest),
the weathered 1800s headstone, and the turned grave soil.

All 16x16 (they sit in the terrain). The three bone models share one set of elements and only swap
textures; the salted one adds a sprinkle of salt over the soil and two little heaps.
"""

import math
import random

import palette as P
from blockmodel import cube, model, write
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

SOIL = Ramp("#120c08", "#1f150e", "#2d1f15", "#3d2b1d", "#4f3926", "#634a32")
DIRT = Ramp("#3d2a1c", "#59402c", "#6e5038", "#866043", "#99704f", "#b0845e")  # vanilla-dirt-like
BONE = Ramp("#4e4636", "#776d56", "#9b9175", "#bdb393", "#d8d0b1", "#ece6cf")
CHAR = Ramp("#0b0908", "#171311", "#231d1a", "#332b26", "#4a403a", "#6a5f57")
ASHG = Ramp("#2c2a29", "#454240", "#5f5b58", "#7c7773", "#9a9590", "#bab5b0")
STONE = Ramp("#4a4741", "#625e57", "#7a756d", "#8f8a81", "#a39e94", "#b8b3a8")
MOSS = Ramp("#1e2a12", "#2c3d18", "#3d5220", "#506a29", "#678434", "#829c45")
LICHEN = [hexc("#b59f4a"), hexc("#c9b763"), hexc("#98a283"), hexc("#b3b99f")]
LEAF = [hexc("#3a2414"), hexc("#55341a"), hexc("#6b4422"), hexc("#7d5530")]


def _wrap_set(t, x, y, c):
    t.set(x % t.w, y % t.h, c)


# --- soil -----------------------------------------------------------------------------------

def soil_top(seed=1401, scorched=False):
    """Freshly turned earth, tileable: clods catching light on their upper-left, a pebble, dead leaves."""
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 3, 4.0)
            m = fbm(x, y, seed + 9, 16, 16, 2, 8.0)
            t.set(x, y, SOIL[max(0, min(5, int(round(2.3 + (n - 0.5) * 2.6 + (m - 0.5) * 1.2))))])
    rng = random.Random(seed + 1)
    for _ in range(9):  # clods
        cx, cy, r = rng.randrange(16), rng.randrange(16), rng.choice((1, 1, 2))
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r + 0.5:
                    c = SOIL[4] if dx + dy < 0 else SOIL[3] if dx + dy == 0 else SOIL[2]
                    _wrap_set(t, cx + dx, cy + dy, c)
        _wrap_set(t, cx + r + 1, cy + r, SOIL[0])
    for (x, y, c) in ((11, 4, P.STONE[4]), (12, 4, P.STONE[2]), (11, 5, P.STONE[1]), (3, 12, P.STONE[3])):
        _wrap_set(t, x, y, c)
    if not scorched:
        for (x, y, k) in ((4, 2, 0), (13, 11, 1), (7, 9, 2)):  # dead leaves
            for (dx, dy, s) in ((0, 0, 2), (1, 0, 3), (1, 1, 1), (2, 1, 0)):
                _wrap_set(t, x + dx, y + dy, LEAF[(s + k) % 4])
    else:
        for y in range(16):
            for x in range(16):
                c = t.get(x, y)
                a = fbm(x, y, seed + 21, 16, 16, 2, 4.0)
                t.set(x, y, mix(c, ASHG[2] if a > 0.55 else CHAR[0], 0.45 if a > 0.55 else 0.35))
    return t


def soil_side():
    """Dirt like vanilla's below a darker band of turned earth with a ragged lower edge."""
    t = Tex(16, 16, 1411)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1411, 16, 16, 3, 4.0)
            t.set(x, y, DIRT[max(0, min(5, int(round(2.6 + (n - 0.5) * 2.4))))])
    rng = random.Random(1412)
    for _ in range(14):
        _wrap_set(t, rng.randrange(16), rng.randrange(5, 16), DIRT[1] if rng.random() < 0.6 else DIRT[4])
    top = soil_top(1401)
    for x in range(16):
        depth = 4 + int(round((fbm(x, 0, 1413, 16, 16, 2, 8.0) - 0.5) * 4))
        for y in range(depth):
            t.set(x, y, top.get(x, y))
        t.set(x, depth, SOIL[1])
        if (x * 5) % 3 == 0:
            t.set(x, depth + 1, mix(DIRT[1], SOIL[1], 0.5))
    return t


def overlay(kind, seed):
    """A transparent 32x32 decal (fine grains) of salt or ash, thickest down the middle where the body lies."""
    t = Tex(32, 32, seed)
    rng = random.Random(seed)
    for y in range(32):
        for x in range(32):
            mid = max(0.0, 1 - abs(x + 0.5 - 16) / 13) * max(0.0, 1 - abs(y + 0.5 - 15) / 17)
            if kind == "salt":
                if rng.random() < 0.05 + 0.32 * mid:
                    t.set(x, y, P.SALT[rng.choice((3, 4, 5, 5))])
            else:
                a = fbm(x, y, seed, 32, 32, 3, 4.0) + mid * 0.25
                if a > 0.62:
                    t.set(x, y, ASHG[min(5, 2 + int((a - 0.62) * 12) + rng.choice((0, 0, 1)))])
                elif rng.random() < 0.04:
                    t.set(x, y, CHAR[0])
    return t


def salt_heap():
    t = Tex(16, 16, 1475)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1475, 16, 16, 2, 8.0)
            t.set(x, y, P.SALT[max(2, min(5, int(round(4.0 + (n - 0.5) * 2.4))))])
    return t


# --- bones ----------------------------------------------------------------------------------

def skeleton_tex(state):
    """32x32 bone surface (the bones are small and seen close). Face regions, in uv units:
    skull top [0,0,5,4], jaw top [6,0,9.4,1.2], pelvis top [0,5,4,6.4], rib tops [0,15.5,6,16]."""
    seed = 1421
    t = Tex(32, 32, seed)
    ramp = CHAR if state == "charred" else BONE
    for y in range(32):
        for x in range(32):
            n = fbm(x, y, seed, 32, 32, 3, 4.0)
            if state == "charred":
                c = CHAR[max(0, min(5, int(round(2.4 + (n - 0.5) * 2.6))))]
            else:
                c = BONE[max(1, min(5, int(round(3.3 + (n - 0.5) * 1.8))))]
                if fbm(x, y, seed + 5, 32, 32, 2, 8.0) > 0.64:
                    c = mix(c, SOIL[3], 0.45)  # grave dirt ground into the bone
            t.set(x, y, c)
    hole = SOIL[0] if state != "charred" else hexc("#020101")
    pal = {"b": ramp[3], "B": ramp[4], "W": ramp[5], "d": ramp[2], "k": hole, "K": mix(hole, ramp[1], 0.4), "t": ramp[5]}
    skull = ["..bBWWBb..",
             ".bBWWWWBb.",
             "bBWBBBBWBb",
             "bkkKbbKkkb",
             "bkkkbbkkkb",
             "dbKbkkbKbd",
             ".dbbkkbbd.",
             ".tktktktk."]
    for y in range(8):
        for x in range(10):
            if skull[y][x] == ".":
                t.set(x, y, (0, 0, 0, 0))  # rounded corners: the box shows as a round cranium
    t.stamp(skull, pal, 0, 0)
    t.stamp(["bbbbbbb", "tktktkt", "dbbbbbd"], pal, 12, 0)
    t.stamp(["bBWbbWBb", "Bkkbbkkb", ".bd..db."], pal, 0, 10)
    for x in range(12):  # ribs: darker where they curve down into the earth
        t.set(x, 31, ramp[2] if x in (0, 11) else ramp[3] if x in (1, 10) else ramp[4])
    if state == "salted":
        rng = random.Random(1431)
        for _ in range(150):
            x, y = rng.randrange(32), rng.randrange(32)
            if t.get(x, y)[3]:
                t.set(x, y, P.SALT[rng.choice((4, 5, 5))])
    if state == "charred":
        rng = random.Random(1432)
        for _ in range(60):
            x, y = rng.randrange(32), rng.randrange(32)
            if t.get(x, y)[3]:
                t.set(x, y, ASHG[rng.choice((3, 4, 5))])
    return t


def bones_model(bone, soil, extra=None):
    S = {"*": "soil"}
    B = {"*": "bone", "down": None}
    rib_uv = lambda z: {"up": [0, 15.5, 6, 16]}
    els = [
        cube("bed", (0, 0, 0), (16, 2, 16), {"*": "side", "up": "soil"},
             uv={f: [0, 0, 16, 2] for f in ("north", "south", "east", "west")}),
        cube("skull", (5.5, 1.6, 0.6), (10.5, 4.0, 4.6), B, uv={"up": [0, 0, 5, 4]}),
        cube("jaw", (6.3, 1.6, 4.4), (9.7, 2.5, 5.6), B, uv={"up": [6, 0, 9.4, 1.2]}),
        cube("neck", (7.5, 1.6, 5.6), (8.5, 2.4, 6.3), B),
        cube("collar", (5.0, 1.8, 6.2), (11.0, 2.5, 6.8), B),
        cube("spine", (7.6, 1.5, 6.8), (8.4, 2.6, 11.8), B),
    ]
    for i, (z, half) in enumerate(((7.0, 2.8), (7.9, 2.7), (8.8, 2.5), (9.7, 2.1))):
        els.append(cube(f"rib_{i}", (8 - half, 1.7, z), (8 + half, 2.9, z + 0.5), B, uv=rib_uv(z)))
    els += [
        cube("sternum", (7.6, 2.9, 7.0), (8.4, 3.0, 9.6), B),
        cube("pelvis", (6.0, 1.6, 11.6), (10.0, 2.8, 13.0), B, uv={"up": [0, 5, 4, 6.4]}),
        cube("humerus_r", (4.2, 1.6, 6.6), (5.0, 2.4, 10.2), B),
        cube("humerus_l", (11.0, 1.6, 6.6), (11.8, 2.4, 10.2), B),
        cube("forearm_r", (4.4, 1.6, 10.0), (5.2, 2.3, 13.0), B, rot=("y", 22.5, (4.8, 2, 10))),
        cube("forearm_l", (10.8, 1.6, 10.0), (11.6, 2.3, 13.0), B, rot=("y", -22.5, (11.2, 2, 10))),
        cube("hand_r", (5.5, 1.6, 12.6), (6.7, 2.2, 13.8), B),
        cube("hand_l", (9.3, 1.6, 12.6), (10.5, 2.2, 13.8), B),
        cube("femur_r", (6.3, 1.6, 13.0), (7.2, 2.5, 15.2), B),
        cube("femur_l", (8.8, 1.6, 13.0), (9.7, 2.5, 15.0), B),
        # The legs vanish under a heap of earth at the foot of the grave, and a few clods lie about.
        cube("heap", (3.0, 1.5, 14.4), (13.0, 3.2, 16.0), S, uv={"north": [3, 0, 13, 1.7]}),
        cube("heap_top", (4.5, 3.2, 14.9), (11.5, 3.8, 16.0), S),
        cube("clod_0", (1.0, 1.8, 3.0), (3.0, 2.9, 5.4), S),
        cube("clod_1", (12.5, 1.8, 1.0), (15.0, 2.7, 3.0), S),
        cube("clod_2", (11.8, 1.8, 10.2), (13.6, 3.0, 12.4), S),
    ]
    tex = {"bone": bone, "soil": soil, "side": "grave_soil_side"}
    if extra:
        kind, var = extra
        tex[kind] = var
        els.append(cube(kind, (0.2, 2.03, 0.2), (15.8, 2.03, 15.8), {"up": kind}, uv={"up": [0.2, 0.2, 15.8, 15.8]}))
        if kind == "salt":
            els.append(cube("salt_heap_0", (7.0, 2.9, 8.0), (8.6, 3.4, 9.3), {"*": "heap", "down": None}))
            els.append(cube("salt_heap_1", (11.4, 1.9, 4.2), (12.8, 2.5, 5.6), {"*": "heap", "down": None}))
            tex["heap"] = "grave_salt_heap"
    return model(tex, els, soil)


# --- headstone ------------------------------------------------------------------------------

def stone_px(x, y, seed):
    n = fbm(x, y, seed, 16, 16, 3, 4.0)
    return STONE[max(0, min(5, int(round(3.0 + (n - 0.5) * 2.0))))]


def weather(t, seed, moss_from_row=12, lichen=6):
    """Moss creeping up from the ground, lichen rosettes, rain streaks."""
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            m = fbm(x, y, seed + 3, 16, 16, 2, 8.0)
            if y >= moss_from_row and m + (y - moss_from_row) * 0.08 > 0.55:
                t.set(x, y, MOSS[max(1, min(5, int(round(2.5 + (m - 0.5) * 5))))])
    for _ in range(lichen):
        x, y = rng.randrange(16), rng.randrange(16)
        c = rng.choice(LICHEN)
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (-1, 0)):
            if rng.random() < 0.75:
                t.set((x + dx) % 16, (y + dy) % 16, mix(t.get((x + dx) % 16, (y + dy) % 16), c, 0.75))
    for _ in range(4):
        x = rng.randrange(16)
        for y in range(rng.randrange(0, 6), rng.randrange(8, 14)):
            t.set(x, y, mix(t.get(x, y), STONE[0], 0.3))


def headstone_front():
    """The north face. Rows are heights (v = 16 - y): a carved cross in the round top, three faint lines
    of epitaph, moss at the foot. Columns are mirrored (default north uv), but it is symmetric."""
    t = Tex(16, 16, 1441)
    for y in range(16):
        for x in range(16):
            t.set(x, y, stone_px(x, y, 1441))
    weather(t, 1442, 13, 7)

    def carve(cells, depth=0.75):
        cs = set(cells)
        for (x, y) in cells:
            t.set(x, y, mix(t.get(x, y), STONE[0], depth))
        for (x, y) in cells:  # light catches the carving's lower and right edges
            for (dx, dy) in ((1, 0), (0, 1)):
                if (x + dx, y + dy) not in cs:
                    t.set(x + dx, y + dy, mix(t.get(x + dx, y + dy), STONE[5], 0.45))
    carve([(7, y) for y in range(2, 7)] + [(8, y) for y in range(2, 7)] + [(x, 3) for x in (5, 6, 9, 10)])
    carve([(x, 8) for x in range(4, 12)], 0.55)
    carve([(x, 10) for x in range(5, 11) if x != 8], 0.5)
    carve([(x, 12) for x in (4, 5, 6, 8, 9, 10, 11)], 0.4)
    return t


def headstone_side():
    t = Tex(16, 16, 1451)
    for y in range(16):
        for x in range(16):
            t.set(x, y, stone_px(x, y, 1451))
    weather(t, 1452, 12, 5)
    return t


def headstone_top():
    t = Tex(16, 16, 1461)
    for y in range(16):
        for x in range(16):
            t.set(x, y, stone_px(x, y, 1461))
    weather(t, 1462, 0, 8)  # the top collects moss and lichen everywhere
    for y in range(16):
        for x in range(16):
            if fbm(x, y, 1463, 16, 16, 2, 4.0) < 0.45:
                t.set(x, y, stone_px(x, y, 1461))
    return t


def headstone_model():
    """~12 wide, 15 tall, 3 deep at the back of the block, rounded top; FRONT = NORTH."""
    F = {"*": "side", "north": "front", "up": "top"}
    tiers = [((2.0, 1.5, 12.0), (14.0, 12.0, 15.0)),
             ((2.5, 12.0, 12.0), (13.5, 13.0, 15.0)),
             ((3.5, 13.0, 12.0), (12.5, 14.0, 15.0)),
             ((5.0, 14.0, 12.0), (11.0, 14.7, 15.0)),
             ((6.5, 14.7, 12.0), (9.5, 15.2, 15.0))]
    els = [cube("plinth", (1.5, 0, 11.4), (14.5, 1.5, 15.6), {"*": "side", "north": "front", "up": "top"},
                uv={"north": [1.5, 14.5, 14.5, 16]})]
    for i, (a, b) in enumerate(tiers):
        els.append(cube(f"stone_{i}", a, b, dict(F, down=None) if i else F))
    return model({"front": "grave_headstone_front", "side": "grave_headstone_side", "top": "grave_headstone_top"},
                 els, "grave_headstone_side")


def generate():
    save(soil_top(), "block", "grave_soil")
    save(soil_top(1401, scorched=True), "block", "grave_soil_scorched")
    save(soil_side(), "block", "grave_soil_side")
    save(overlay("salt", 1471), "block", "grave_salt")
    save(overlay("ash", 1472), "block", "grave_ash")
    save(salt_heap(), "block", "grave_salt_heap")
    for state, name in (("plain", "grave_skeleton"), ("salted", "grave_skeleton_salted"), ("charred", "grave_skeleton_charred")):
        save(skeleton_tex(state), "block", name)
    write("grave_bones", bones_model("grave_skeleton", "grave_soil"))
    write("grave_bones_salted", bones_model("grave_skeleton_salted", "grave_soil", ("salt", "grave_salt")))
    write("grave_bones_rested", bones_model("grave_skeleton_charred", "grave_soil_scorched", ("ash", "grave_ash")))
    save(headstone_front(), "block", "grave_headstone_front")
    save(headstone_side(), "block", "grave_headstone_side")
    save(headstone_top(), "block", "grave_headstone_top")
    write("grave_headstone", headstone_model())


if __name__ == "__main__":
    generate()
