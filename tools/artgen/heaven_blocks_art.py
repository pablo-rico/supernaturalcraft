"""v0.18 block textures (HeavenAssets.BLOCK_TEXTURES) and the two block models drawn here. 2D art work.

  cloud_stone            Heaven's stone: a soft, faintly luminous warm white with drifting billows and thin gold veins.
  cloud_bricks           the same stone dressed in bricks, pale mortar, a gold thread in some joints.
  heaven_gate            16 frames (.mcmeta, frametime 2, interpolated): white-gold light rising in soft rays, motes
                         drifting up; translucent (the gate is drawn like a nether portal plane).
  memory_veil            16 frames (frametime 3): a pearly veil, pale blue / lavender / gold sheen sliding sideways in slow
                         ripples; more transparent than the gate.
  celestial_seal         8 frames (frametime 5): a pane of gold light with Heaven's sigil (a ring, two interlaced triangles,
                         four Enochian ticks), a border so neighbouring seals read as panels; the glow breathes.
  hearth_front/_side/_top the hearth's stone surround (front: the full front view, the firebox opening dark), its sides
                         and its mantel top (gold inlay). The hearth is a block model (`models/block/hearth.json`, front to
                         the north): base stone, jambs, lintel and mantel, a soot back, two crossed logs and two crossed
                         flame planes drawn full bright. Extra textures: hearth_back (sooty firebrick), hearth_logs,
                         hearth_fire (8 frames, frametime 3, cutout).
  reprogramming_console  the console's screen: a cyan brain scan over a grid, readouts and an EEG trace. The console is a
                         block model (`models/block/reprogramming_console.json`, screen to the north): white lacquered
                         pedestal, a slanted control deck (reprogramming_console_panel) and the screen in a white bezel
                         (full bright). Extra textures: reprogramming_console_side, reprogramming_console_panel.
  filing_cabinet_front   a putty-grey steel cabinet front, two drawers with brass pulls and blank label cards;
                         filing_cabinet_front_1..4 the same with the numeral (I-IV) on the card and a colour tab
                         (I blue, II green, III amber, IV red). filing_cabinet_side, filing_cabinet_top (extra).
  crossroads_soil(_top)  trodden earth: the side packed darker under the surface; the top crossed by two pairs of wheel
                         ruts (north-south and east-west), pebbles, a faint ash-grey patch where things were buried.
"""

import json
import math
import os
import random

import blockmodel as BM
from common import TEXTURES, save
from pixelkit import Ramp, Tex, fbm, hexc, mix

CLEAR = (0, 0, 0, 0)
CLOUD = Ramp("#a8a29a", "#c8c2b8", "#dcd7ce", "#ebe7df", "#f5f2ec", "#fffdf8")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
LIGHT = Ramp("#b98a2c", "#d9ad4a", "#f0cf72", "#fae6a6", "#fff6d8", "#ffffff")
SOOT = Ramp("#120d0b", "#1d1512", "#2a1e19", "#3a2a22", "#4c372b", "#5e4535")
BARK = Ramp("#1c120b", "#2c1d12", "#3e2a1a", "#523823", "#68482e", "#7e5a3b")
FIRE = Ramp("#7a2a06", "#c4500c", "#ec7f1c", "#f8b13a", "#ffdc7a", "#fff8de")
LACQUER = Ramp("#7f8a90", "#a8b3b8", "#c8d1d5", "#dfe6e9", "#eef3f5", "#ffffff")
CYAN = Ramp("#062027", "#0b3540", "#11535f", "#1a8a96", "#4fd0dc", "#c8fbff")
PUTTY = Ramp("#4f4c46", "#6a665e", "#827d73", "#99948a", "#aeaa9f", "#c3bfb4")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
DIRT = Ramp("#2e2116", "#43301f", "#5a4029", "#6e5034", "#82603f", "#97724c")
TAB = {1: hexc("#3f6fc4"), 2: hexc("#3f9a52"), 3: hexc("#d49a2a"), 4: hexc("#c0392f")}


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


def tex16(fn, w=16, h=16, seed=0):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def _h(x, y, seed):
    return random.Random((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)).random()


def mcmeta(name, frametime, interpolate=True):
    path = os.path.join(TEXTURES, "block", name + ".png.mcmeta")
    with open(path, "w") as f:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f, indent=1)
        f.write("\n")


def strip(frames):
    """Stacks equal 16x16 frames into one animation strip."""
    t = Tex(16, 16 * len(frames), 0)
    for i, fr in enumerate(frames):
        for y in range(16):
            for x in range(16):
                t.rows[y + 16 * i][x] = fr.rows[y][x]
    return t


# =====================================================================================================
# Cloud stone and bricks
# =====================================================================================================

def _veins(seed, count, length):
    """Thin wandering gold veins (wrapped, so the block still tiles)."""
    rng = random.Random(seed)
    out = {}
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        dx = rng.choice((-1, 1))
        for i in range(length):
            out[(x % 16, y % 16)] = 1.0 - i / (length * 1.6)
            if rng.random() < 0.55:
                x += dx
            else:
                y += 1
            if rng.random() < 0.12:
                dx = -dx
    return out


def cloud_stone():
    """Soft billows embossed from the top-left (a lit, cloud-like relief), thin gold veins, a few glints."""
    veins = _veins(18901, 2, 11)

    def n(x, y):
        return 0.6 * fbm(x % 16, y % 16, 18900, 16, 16, 3, 4.0) + 0.4 * fbm((x + 5) % 16, (y + 3) % 16, 18902, 16, 16, 2, 2.0)

    def f(x, y):
        h = n(x, y)
        relief = (h - n(x + 1, y + 1)) * 6.0
        v = 3.4 + (h - 0.5) * 1.2 + relief
        c = tone(CLOUD, v)
        if (x, y) in veins:
            c = mix(c, GOLD[4], 0.5 * veins[(x, y)])
        elif _h(x, y, 18903) < 0.025:
            c = CLOUD[5]                                  # a glint
        return c
    return tex16(f)


def _brick_face(seed, x, y, bx, by, w, h):
    """One brick's surface: soft billows, lit from the top-left."""
    n = fbm(x, y, seed, 16, 16, 2, 4.0)
    v = 3.4 + (n - 0.5) * 1.2
    if y == by:
        v += 0.9
    elif x == bx:
        v += 0.5
    elif y == by + h - 1 or x == bx + w - 1:
        v -= 0.5
    return tone(CLOUD, v)


def bricks(seed, mortar=CLOUD[1], gold_joints=True, warm=0.0):
    rng = random.Random(seed)
    thread = {r: rng.random() < 0.5 for r in range(4)}

    def f(x, y):
        row = y // 4
        off = 0 if row % 2 == 0 else 4
        bx = ((x + off) // 8) * 8 - off
        by = row * 4
        if y % 4 == 3 or (x + off) % 8 == 7:
            if gold_joints and thread[row] and y % 4 == 3 and (x + row * 5) % 16 < 6:
                return GOLD[3]
            return mortar
        c = _brick_face(seed, x, y, bx, by, 7, 3)
        return mix(c, hexc("#f2d9b0"), warm) if warm else c
    return tex16(f)


# =====================================================================================================
# The gate and the veil
# =====================================================================================================

def gate_frame(f, frames=16):
    ph = f / frames * math.tau
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            yy = (y + f) % 16                              # everything drifts up one texel a frame
            n = fbm(x, yy, 19001, 16, 16, 3, 4.0)
            ray = 0.5 + 0.5 * math.sin(x * math.tau / 8 + math.sin(ph + x * 0.7) * 0.8)
            v = 0.55 * n + 0.45 * ray
            c = tone(LIGHT, 2.0 + v * 3.6)
            a = int(175 + 70 * v)
            # Motes: tiny bright sparks riding the light up.
            if _h(x, (y + 2 * f) % 16, 19002) < 0.035:
                c, a = LIGHT[5], 245
            t.set(x, y, (c[0], c[1], c[2], a))
    return t


def veil_frame(f, frames=16):
    ph = f / frames * math.tau
    pearl = (hexc("#cfe3ff"), hexc("#e2d4ff"), hexc("#fff0c8"))
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            xx = (x + f) % 16
            n = fbm(xx, y, 19101, 16, 16, 3, 4.0)
            rip = 0.5 + 0.5 * math.sin(y * math.tau / 8 + math.sin(ph + y * 0.5) * 1.2 + xx * 0.25)
            v = 0.5 * n + 0.5 * rip
            hue = (v * 2.2 + x / 16 + f / frames) % 3
            i = int(hue)
            c = mix(pearl[i], pearl[(i + 1) % 3], hue - i)
            c = mix(c, hexc("#ffffff"), max(0.0, v - 0.6) * 1.5)
            a = int(95 + 90 * v)
            if _h((x + f) % 16, y, 19102) < 0.025:
                c, a = hexc("#ffffff"), 220
            t.set(x, y, (c[0], c[1], c[2], a))
    return t


# =====================================================================================================
# The celestial seal
# =====================================================================================================

def _seg(px, py, a, b):
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    L = dx * dx + dy * dy
    t = 0 if L == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def seal_field():
    """{(x, y): 'ring' | 'star' | 'tick' | 'dot'} for Heaven's sigil on a 16-px face."""
    c = 7.5
    out = {}
    tri_a = [(c + 4.6 * math.cos(math.radians(-90 + 120 * i)), c + 4.6 * math.sin(math.radians(-90 + 120 * i))) for i in range(3)]
    tri_b = [(c + 4.6 * math.cos(math.radians(90 + 120 * i)), c + 4.6 * math.sin(math.radians(90 + 120 * i))) for i in range(3)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - c, y - c)
            if 5.7 <= d <= 6.6:
                out[(x, y)] = "ring"
            elif any(_seg(x, y, tri[i], tri[(i + 1) % 3]) < 0.5 for tri in (tri_a, tri_b) for i in range(3)):
                out[(x, y)] = "star"
            elif d < 0.9:
                out[(x, y)] = "dot"
    for (x, y) in ((7, 0), (8, 0), (0, 7), (0, 8), (15, 7), (15, 8), (7, 15), (8, 15)):
        out[(x, y)] = "tick"
    return out


def seal_frame(f, frames=8):
    pulse = 0.5 + 0.5 * math.cos(f / frames * math.tau)
    fld = seal_field()
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            k = fld.get((x, y))
            glint = abs(((x + y) - f * 4) % 32 - 16) < 1.5
            if k in ("ring", "star", "dot"):
                c = LIGHT[5] if (k == "dot" or glint) else tone(LIGHT, 3.6 + pulse)
                t.set(x, y, (c[0], c[1], c[2], 235))
            elif k == "tick" or x in (0, 15) or y in (0, 15):
                c = GOLD[3] if k != "tick" else GOLD[4]
                t.set(x, y, (c[0], c[1], c[2], 200 if k == "tick" else 170))
            else:
                n = fbm(x, y, 19201, 16, 16, 2, 4.0)
                c = tone(LIGHT, 1.8 + n * 1.6 + pulse * 0.6)
                if glint:
                    c = mix(c, LIGHT[5], 0.5)
                t.set(x, y, (c[0], c[1], c[2], int(70 + 40 * n + 30 * pulse)))
    return t


# =====================================================================================================
# The hearth
# =====================================================================================================

def hearth_front():
    """The full front view: mantel (v 0), lintel (v 1-2), jambs (u 0-2 / 13-15), base stone (v 14-15); the opening dark."""
    base = bricks(19301, warm=0.08, gold_joints=False)

    def f(x, y):
        if y == 0:
            return GOLD[4] if x % 4 else GOLD[5]           # the mantel's gilded edge
        if y in (1, 2):
            if 6 <= x <= 9:                                # the keystone
                return GOLD[4] if y == 1 else GOLD[3]
            return tone(CLOUD, 4.3 - y * 0.7 + (0.4 if x % 5 == 0 else 0))
        if y >= 14:
            return tone(CLOUD, 3.6 - (y - 14) * 1.2 + 0.4 * fbm(x, y, 19302, 16, 16, 2, 4.0))
        if 3 <= x <= 12:
            return SOOT[1]
        c = base.get(x, y)
        if x in (2, 13):
            c = mix(c, CLOUD[1], 0.5)                     # the inner edge of the jambs
        return c
    return tex16(f)


def hearth_side():
    base = bricks(19311, warm=0.08, gold_joints=False)

    def f(x, y):
        if y == 0:
            return GOLD[4] if x % 4 else GOLD[5]
        if y == 1:
            return CLOUD[1]
        if y >= 14:
            return tone(CLOUD, 3.6 - (y - 14) * 1.2)
        return base.get(x, y)
    return tex16(f)


def hearth_top():
    """The mantel top: polished cloud stone inlaid with a gold border and a sunburst in its heart."""
    def f(x, y):
        if x in (0, 15) or y in (0, 15):
            return CLOUD[2]
        if x in (1, 14) or y in (1, 14):
            return GOLD[4] if (x + y) % 2 else GOLD[3]
        d = math.hypot(x - 7.5, y - 7.5)
        a = math.degrees(math.atan2(y - 7.5, x - 7.5)) % 45
        if d < 1.4:
            return GOLD[5]
        if d < 4.2 and a < 9:
            return mix(CLOUD[4], GOLD[4], 0.6)
        n = fbm(x, y, 19321, 16, 16, 2, 4.0)
        return tone(CLOUD, 3.8 + (n - 0.5) * 1.0)
    return tex16(f)


def hearth_back():
    """Sooty firebrick at the back of the firebox, a warm glow along its foot."""
    def f(x, y):
        row = y // 4
        off = 0 if row % 2 == 0 else 3
        if y % 4 == 3 or (x + off) % 6 == 5:
            c = SOOT[0]
        else:
            c = tone(SOOT, 2.6 + (fbm(x, y, 19331, 16, 16, 2, 4.0) - 0.5) * 1.6 - (0.6 if y % 4 == 0 else 0))
        glow = max(0.0, (y - 9) / 7)
        return mix(c, FIRE[2], 0.45 * glow * glow)
    return tex16(f)


def hearth_logs():
    """Birch-pale logs gone dark with fire: bark in long strips, ember cracks glowing orange."""
    def f(x, y):
        v = 2.6 + 0.9 * math.sin(y * 1.9 + math.sin(x * 0.4) * 1.2) + (fbm(x, y, 19341, 16, 16, 2, 4.0) - 0.5)
        c = tone(BARK, v)
        if _h(x // 3, y, 19342) < 0.05:
            c = FIRE[2] if _h(x, y, 19343) < 0.5 else FIRE[3]
        return c
    return tex16(f)


def fire_frame(f, frames=8):
    """A pixel flame: tongues of gold-white licking up, the column heights wandering frame to frame."""
    t = Tex(16, 16, 0)
    for x in range(16):
        edge = 1 - abs(x - 7.5) / 8.5
        wob = fbm(x * 2, f * 2, 19351, 32, 16, 2, 4.0)
        height = 4 + 11 * edge * (0.55 + 0.6 * wob)
        for y in range(16):
            h = 15 - y
            if h > height:
                continue
            k = h / max(1.0, height)                       # 0 at the foot, 1 at the tip
            core = edge * (1 - k)
            c = tone(FIRE, 1.0 + core * 5.0 + (0.6 if _h(x, (y + f * 3) % 16, 19352) < 0.2 else 0))
            if k > 0.85 and _h(x, y + f, 19353) < 0.5:
                continue                                    # ragged tips
            t.set(x, y, c)
    return t


def hearth_model():
    F, S, T, B, L, FI = "front", "side", "top", "back", "logs", "fire"
    els = [
        BM.cube("hearthstone", (0, 0, 0), (16, 2, 16), {"north": F, "up": B, "*": S}),
        BM.cube("jamb_l", (0, 2, 1), (3, 13, 16), {"north": F, "east": B, "*": S}),
        BM.cube("jamb_r", (13, 2, 1), (16, 13, 16), {"north": F, "west": B, "*": S}),
        BM.cube("lintel", (0, 13, 1), (16, 15, 16), {"north": F, "down": B, "*": S}),
        BM.cube("mantel", (0, 15, 0), (16, 16, 16), {"north": F, "up": T, "*": S}),
        BM.cube("back", (3, 2, 12), (13, 13, 16), {"north": B, "south": S, "up": None, "down": None, "east": None, "west": None}),
        BM.cube("log_a", (4, 2, 6.5), (12, 3.6, 8.1), {"*": L}, rot=("y", 22.5, (8, 2, 7.3))),
        BM.cube("log_b", (4, 2, 8.2), (12, 3.6, 9.8), {"*": L}, rot=("y", -22.5, (8, 2, 9))),
        BM.cube("embers", (4.5, 2.05, 6), (11.5, 2.3, 10.5), {"up": L, "*": None}),
    ]
    for name, ang in (("flame_a", 45), ("flame_b", -45)):
        e = BM.cube(name, (3, 2.4, 8), (13, 12.4, 8), {"north": FI, "south": FI}, rot=("y", ang, (8, 2.4, 8)), shade=False)
        e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        els.append(e)
    tex = {F: "hearth_front", S: "hearth_side", T: "hearth_top", B: "hearth_back", L: "hearth_logs", FI: "hearth_fire"}
    BM.write("hearth", BM.model(tex, els, "hearth_front"))


# =====================================================================================================
# Naomi's console
# =====================================================================================================

BRAIN = [
    "................",
    "................",
    "................",
    ".....cccccc.....",
    "...cc.c..c.cc...",
    "..c..c.cc.c..c..",
    "..c.c.c..c.c.c..",
    "..cc.cc..cc.cc..",
    "..c..c....c..c..",
    "...cc.c..c.cc...",
    "....cccccccc....",
    "......c.cc......",
    "................",
    "................",
    "................",
    "................",
]


def console_screen():
    def f(x, y):
        if x in (0, 15) or y in (0, 15):
            return CYAN[0]
        c = CYAN[1] if (x % 4 == 0 or y % 4 == 0) else mix(CYAN[0], CYAN[1], 0.5)
        if BRAIN[y][x] == "c":
            c = CYAN[4] if (x + y) % 3 else CYAN[5]
        # Readouts: a column of bars at the right, a status lamp at the top left.
        if x == 13 and 2 <= y <= 9:
            c = CYAN[3] if y >= 2 + (x * 3 + 5) % 5 else CYAN[1]
        if (x, y) in ((2, 2), (3, 2)):
            c = hexc("#ff5b5b")
        # The EEG trace along the foot.
        if 12 <= y <= 14:
            wave = 13 + round(math.sin(x * 1.3) * 0.9 + (1.0 if x == 7 else 0) * -1.8)
            if y == max(12, min(14, wave)):
                c = hexc("#e8ffff")
        return c
    return tex16(f)


def console_side():
    """Glossy white lacquer, a cyan pinstripe, vent slots low down, a chrome kick plate."""
    def f(x, y):
        n = fbm(x, y, 19401, 16, 16, 2, 4.0)
        c = tone(LACQUER, 3.6 + (n - 0.5) * 0.8 + (0.8 if x == 1 else 0) - (0.5 if x == 14 else 0))
        if y == 4:
            c = CYAN[4]
        if y in (10, 12) and 3 <= x <= 12:
            c = LACQUER[1]
        if y == 15:
            c = hexc("#8c959c")
        return c
    return tex16(f)


def console_panel():
    """The control deck: rows of keys (white, cyan, a red stop), a slider in its track and a round dial."""
    def f(x, y):
        c = tone(LACQUER, 3.4 + (0.6 if y < 2 else 0))
        if x in (0, 15) or y in (0, 15):
            return LACQUER[1]
        if y in (3, 4, 6, 7) and 2 <= x <= 9 and x % 2 == 0:
            c = CYAN[4] if (x // 2 + y) % 3 == 0 else LACQUER[5]
            if y in (4, 7):
                c = mix(c, LACQUER[1], 0.45)               # the keys' fronts
        if (x, y) in ((8, 3), (8, 4)):
            c = hexc("#e04040") if y == 3 else hexc("#902020")
        if x == 12 and 2 <= y <= 8:
            c = LACQUER[1]
        if (x, y) in ((12, 5), (11, 5), (13, 5)):
            c = hexc("#2b3238")
        d = math.hypot(x - 5.5, y - 11.5)
        if d < 2.6:
            c = hexc("#2b3238") if d > 1.7 else CYAN[3]
        if (x, y) == (6, 10):
            c = CYAN[5]
        if 10 <= x <= 13 and 10 <= y <= 13:
            c = CYAN[1] if (x + y) % 2 else CYAN[2]        # a little graph window
        return c
    return tex16(f)


def console_model():
    S, P, SC = "side", "panel", "screen"
    screen = BM.cube("screen", (3, 12.2, 11.45), (13, 17.8, 11.45), {"north": SC}, uv={"north": [0, 1, 16, 15]}, shade=False)
    screen["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    els = [
        BM.cube("plinth", (1.5, 0, 2.5), (14.5, 1, 13.5), {"*": S}),
        BM.cube("pedestal", (2.5, 1, 3), (13.5, 7.5, 12.5), {"*": S}),
        BM.cube("deck", (1.5, 7.5, 1.5), (14.5, 9, 11), {"up": P, "*": S}, rot=("x", -22.5, (8, 7.5, 1.5))),
        BM.cube("bezel", (2.4, 11.4, 11.5), (13.6, 18.6, 13), {"*": S}),
        BM.cube("neck", (6.5, 7.5, 11.6), (9.5, 11.4, 12.8), {"*": S}),
        screen,
    ]
    BM.write("reprogramming_console", BM.model({S: "reprogramming_console_side", P: "reprogramming_console_panel",
                                                SC: "reprogramming_console"}, els, "reprogramming_console_side"))


# =====================================================================================================
# Zachariah's filing cabinets
# =====================================================================================================

def putty(x, y, seed, vertical=True):
    n = fbm(x, y, seed, 16, 16, 2, 4.0)
    streak = math.sin((x if vertical else y) * 2.1 + seed) * 0.25
    return tone(PUTTY, 3.2 + (n - 0.5) * 0.8 + streak)


def cabinet_front():
    """Two drawers (v 1-7 and 9-15) in a putty-grey case, each with a brass label holder whose foot is the pull."""
    def f(x, y):
        if x in (0, 15) or y == 0:
            return PUTTY[1] if x == 15 else PUTTY[4]
        if y == 8:
            return PUTTY[0]                               # the gap between the drawers
        top = 1 if y < 8 else 9
        dy = y - top
        c = putty(x, y, 19501)
        if x in (1, 14) or dy == 6:
            c = PUTTY[1] if (x == 14 or dy == 6) else PUTTY[5]
        elif dy == 0:
            c = PUTTY[5]
        # A combined label holder and pull: a brass frame at x 4..11 (rows 1..4 of the drawer, the card in rows 2..4),
        # its bottom edge the pull bar (row 5, x 3..12).
        if 4 <= x <= 11 and 1 <= dy <= 4:
            if x in (4, 11) or dy == 1:
                c = BRASS[4] if dy == 1 else BRASS[2]
            else:
                c = hexc("#f1ecdf") if (x + dy) % 7 else hexc("#e2dccc")
        if 3 <= x <= 12 and dy == 5:
            c = BRASS[5] if x in (3, 12) else (BRASS[4] if x % 3 else BRASS[3])
        if 3 <= x <= 12 and dy == 6:
            c = PUTTY[0] if 4 <= x <= 11 else c            # the pull's shadow
        return c
    return tex16(f)


def cabinet_numeral(number):
    """The numeral (I, II, III, IV, 3 px tall) on both cards (rows 2..4 of each drawer) and a colour tab on the top lip."""
    t = cabinet_front()
    ink = hexc("#1d1a16")
    glyph = {1: ["#", "#", "#"], 2: ["#.#", "#.#", "#.#"], 3: ["#.#.#", "#.#.#", "#.#.#"], 4: ["#.#.#", "#.#.#", "#..#."]}[number]
    w = len(glyph[0])
    ox = 8 - (w + 1) // 2
    for top in (1, 9):
        if top == 1:
            for x in range(2, 14):
                t.set(x, top, TAB[number])
        for gy, line in enumerate(glyph):
            for gx, ch in enumerate(line):
                if ch == "#":
                    t.set(ox + gx, top + 2 + gy, ink)
    return t


def cabinet_side():
    def f(x, y):
        c = putty(x, y, 19511, vertical=False)
        if x == 0 or y == 0:
            c = PUTTY[4]
        if x == 15 or y == 15:
            c = PUTTY[1]
        if y in (5, 10) and 2 <= x <= 13:
            c = PUTTY[2]                                   # pressed stiffening ribs
        return c
    return tex16(f)


def cabinet_top():
    def f(x, y):
        c = putty(x, y, 19521, vertical=False)
        if x in (0, 15) or y in (0, 15):
            c = PUTTY[1] if (x == 15 or y == 15) else PUTTY[4]
        return c
    return tex16(f)


# =====================================================================================================
# The crossroads soil
# =====================================================================================================

def crossroads_top():
    """Packed earth at the crossing: two pairs of ruts (u 3-4 / 11-12 and v 3-4 / 11-12), pebbles, a pale ash patch."""
    def f(x, y):
        n = fbm(x, y, 19601, 16, 16, 3, 4.0)
        v = 3.0 + (n - 0.5) * 1.6
        wx = x + (1 if math.sin(y * 0.9) > 0.75 else 0)
        wy = y + (1 if math.sin(x * 0.8 + 1) > 0.75 else 0)
        if wx in (3, 4, 11, 12) or wy in (3, 4, 11, 12):
            v -= 0.9 if (wx in (3, 12) or wy in (3, 12)) else 0.6
        c = tone(DIRT, v)
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 2.6 + fbm(x, y, 19602, 16, 16, 2, 4.0) * 1.2:
            c = mix(c, hexc("#8a8178"), 0.35)               # ash where boxes were buried before
        if _h(x, y, 19603) < 0.05:
            c = hexc("#9a9184") if _h(x, y, 19604) < 0.6 else hexc("#6f685f")   # pebbles
        return c
    return tex16(f)


def crossroads_side():
    def f(x, y):
        n = fbm(x, y, 19611, 16, 16, 3, 4.0)
        v = 3.0 + (n - 0.5) * 1.6
        if y < 3:
            v = 2.0 + (n - 0.5) * 1.0 - (0.6 if y == 2 else 0)   # the packed crust
        c = tone(DIRT, v)
        if y >= 3 and _h(x, y, 19612) < 0.05:
            c = hexc("#8f8679")
        if y >= 6 and _h(x, y, 19613) < 0.025:
            c = hexc("#3a2c20")
        return c
    return tex16(f)


# =====================================================================================================

def generate():
    save(cloud_stone(), "block", "cloud_stone")
    save(bricks(18910), "block", "cloud_bricks")
    save(strip([gate_frame(f) for f in range(16)]), "block", "heaven_gate")
    mcmeta("heaven_gate", 2)
    save(strip([veil_frame(f) for f in range(16)]), "block", "memory_veil")
    mcmeta("memory_veil", 3)
    save(strip([seal_frame(f) for f in range(8)]), "block", "celestial_seal")
    mcmeta("celestial_seal", 5)
    save(hearth_front(), "block", "hearth_front")
    save(hearth_side(), "block", "hearth_side")
    save(hearth_top(), "block", "hearth_top")
    save(hearth_back(), "block", "hearth_back")
    save(hearth_logs(), "block", "hearth_logs")
    save(strip([fire_frame(f) for f in range(8)]), "block", "hearth_fire")
    mcmeta("hearth_fire", 3)
    hearth_model()
    save(console_screen(), "block", "reprogramming_console")
    save(console_side(), "block", "reprogramming_console_side")
    save(console_panel(), "block", "reprogramming_console_panel")
    console_model()
    save(cabinet_front(), "block", "filing_cabinet_front")
    for n in range(1, 5):
        save(cabinet_numeral(n), "block", f"filing_cabinet_front_{n}")
    save(cabinet_side(), "block", "filing_cabinet_side")
    save(cabinet_top(), "block", "filing_cabinet_top")
    save(crossroads_side(), "block", "crossroads_soil")
    save(crossroads_top(), "block", "crossroads_soil_top")


if __name__ == "__main__":
    generate()
