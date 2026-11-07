"""Season 6's spoils and props (v0.39): the Blade of the New God, Castiel's trench coat (sprite and armour layer), the grace
shard, the Judgment page, Crowley's King of Hell skin and the FX particles. Contract: GodstielAssets.

  The Blade of the New God (`new_god_blade`): a member of the angelic blade family (weapon_models.angelic_blade, contract
  AngelicBladeAssets): bones `blade`, `grip`, `light`, built upright with the point up and centred on the origin; a long
  three-edged spike of pale silver engraved in soul-blue light, a gold collar and crossguard, the grip wound in pale gold
  with soul-blue wire that glows, a soul-blue gem for a pommel. `light` (the sheath of light) is at scale 0 in `idle`, rises
  and holds in `charge`, blinks in `flash`. Texture, glowmask and the 16-px inventory icon.

  Crowley, King of Hell (`textures/entity/crowley_king.png`): crowley_art's own rig and painter (so the atlas is the same),
  then a crimson pocket square on his left breast and a gold signet ring on his right hand painted over its texels.
  crowley_art's own output is untouched.

  Particles (16x16, white with alpha, the code tints them): purgatory_soul, grace_mote, goo_drop, shadow_feather.
"""

import math
import random

import crowley_art
import palette as P
import preview3d
from common import art, save
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix
from weapon_models import ANIM, GEO, BladeLook, PALE_SILVER, _SLIM, angelic_blade, blade_anims, noise

O = P.OUTLINE
SOUL = Ramp("#1f4f9c", "#3a7cd0", "#6fb2f2", "#a8d6ff", "#d6efff", "#ffffff")
PALE_GOLD = Ramp("#6a5224", "#9a7c3e", "#c4a560", "#dfc688", "#f0e0b0", "#fff8e2")
TRENCH = Ramp("#5e4f37", "#7f6c4c", "#9c8762", "#b49e77", "#c8b38c", "#dac7a2")
SUIT = Ramp("#080b15", "#0e1424", "#151e35", "#1d2945", "#283757", "#36486e")
SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
TIE = Ramp("#0d1b3a", "#142a57", "#1d3a75", "#284c93", "#3a63ad", "#557fc4")
CRIMSON = Ramp("#3a0408", "#5e0810", "#8a1018", "#b01c22", "#d43a36", "#f06a5a")
PAPER = Ramp("#5e4a2c", "#86704a", "#a8926a", "#c6b08a", "#ddcaa4", "#efe2c4")


# =====================================================================================================
# The Blade of the New God
# =====================================================================================================

BLADE_LOOK = BladeLook("new_god_blade", PALE_SILVER, hexc("#9cd2ff"), (196, 228, 255), 391, step=5, top=0.7,
                       border=hexc("#7fb8f0"))
GRIP_H = 5
WRAP_PERIOD = 4


def _wrap_turn(face, wi, x, y):
    turn = {"east": 0, "north": 1, "west": 2, "south": 3}
    return y + (turn[face] * wi + x) if face in turn else None


def wrap_mat(face, x, y, wi, he):
    """Pale gold strips wound diagonally, soul-blue wire between the turns."""
    k = _wrap_turn(face, wi, x, y)
    if k is None:
        return PALE_GOLD[1]
    if k % WRAP_PERIOD == 0:
        return SOUL[2]
    n = fbm(x * 2, y * 2, 392, 32, 32, 2, 5.0)
    return PALE_GOLD[int(round(2.6 + (k // WRAP_PERIOD % 2) * 0.6 + (n - 0.5) * 1.0 + (0.7 if k % WRAP_PERIOD == 1 else 0)))]


def wrap_glow(face, x, y, wi, he):
    k = _wrap_turn(face, wi, x, y)
    return SOUL[3] if k is not None and k % WRAP_PERIOD == 0 else None


def blade_guard(w, root, y):
    """A gold collar (an octagon: a square and its 45-degree twin) and a short swept crossguard with soul-blue tips."""
    gold = noise(PALE_GOLD, 393, 3.2, 0.8)
    w.cube("collar", root, (-1.5, y, -1.5), (3, 1, 3), gold, inflate=-0.25, density=2)
    b = w.bone("collar45", (0, y, 0), "root", rotation=(0, 45, 0))
    w.cube("collar45", b, (-1.5, y, -1.5), (3, 1, 3), noise(PALE_GOLD, 394, 2.8, 0.8), inflate=-0.37, density=2)
    w.cube("guard", root, (-2.5, y - 0.6, -0.5), (5, 1, 1), gold, inflate=-0.15, density=2)
    tip = lambda f, x, yy, wi, he: SOUL[4]
    for side, x in (("l", 2.1), ("r", -3.1)):
        w.cube("quillon_" + side, root, (x, y - 0.2, -0.5), (1, 1, 1), tip, tip, inflate=-0.25, density=2)
    return y + 1


def blade_pommel(w, grip):
    """A gold cap and a faceted soul-blue gem below it."""
    w.cube("pommel", grip, (-1, 1, -1), (2, 1, 2), noise(PALE_GOLD, 395, 3.0, 0.7), inflate=0.15, density=2)
    gem = w.bone("finial", (0, 0.9, 0), "grip", rotation=(45, 45, 0))
    g = lambda f, x, y, wi, he: SOUL[4] if (x + y) % 2 else SOUL[2]
    w.cube("finial", gem, (-0.5, 0.4, -0.5), (1, 1, 1), g, g, inflate=0.1, density=2)
    return 2


def new_god_blade():
    w = angelic_blade(BLADE_LOOK, GRIP_H, 14, 2.1, wrap_mat, blade_guard, blade_pommel)
    # The wire's glow: angelic_blade takes no glow for the wrap, so add it on the same cube.
    for i, (c, mat, glow) in enumerate(w.paints):
        if c.tag == "wrap":
            w.paints[i] = (c, mat, wrap_glow)
    wrap = w.parts["wrap"]
    t, g = w.build()
    lo, hi = w.rig.height_span()
    grip_mid = wrap.origin[1] + wrap.size[1] / 2
    return w, t, g, blade_anims(w.name, BLADE_LOOK), hi - lo, grip_mid


# =====================================================================================================
# 16x16 sprites
# =====================================================================================================

def trenchcoat_sprite():
    pal = {"o": O, "L": TRENCH[5], "l": TRENCH[4], "t": TRENCH[3], "m": TRENCH[2], "d": TRENCH[1], "D": TRENCH[0],
           "s": SHIRT[4], "S": SHIRT[2], "b": TIE[3], "B": TIE[1], "n": SUIT[3], "N": SUIT[1], "k": hexc("#6a4c2e")}
    return art([
        "....oooooooo....",
        "...oLlo..olLo...",
        "..oLltlossoltlo.",
        ".oLlttlbsSmttmdo",
        "oLlttttbBnmtttmo",
        "oltttlnbBNttttdo",
        "oltmtlnbBNmtkmdo",
        "otmtolnBNNmomtdo",
        "ottmolNNNNmomdo.",
        ".odDolkddkmo.oo.",
        "..oolttnNtmo....",
        "...oltmnNmmo....",
        "...oltmoNmdo....",
        "...olmmoomdo....",
        "...odddoodDo....",
        "....ooo..ooo....",
    ], pal)


def grace_shard():
    """A sliver of grace: a thin crystal of blue-white light, a bright core, motes round it."""
    t = Tex(16, 16, 3961)
    # A long diamond leaning to the right, its axis from (4, 14) to (12, 1).
    ax0, ay0, ax1, ay1 = 4.0, 14.5, 11.5, 1.0
    L = math.hypot(ax1 - ax0, ay1 - ay0)
    ux, uy = (ax1 - ax0) / L, (ay1 - ay0) / L
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - ax0, y + 0.5 - ay0
            along = px * ux + py * uy
            across = -px * uy + py * ux
            u = along / L
            if not 0 <= u <= 1:
                continue
            half = 2.3 * (1 - abs(u - 0.42) / 0.58) ** 0.9
            if abs(across) > half:
                continue
            q = abs(across) / max(0.3, half)
            if q < 0.3:
                c = SOUL[5]
            elif across < 0:
                c = SOUL[4] if q < 0.7 else SOUL[2]       # the lit facet
            else:
                c = SOUL[3] if q < 0.7 else SOUL[1]
            t.set(x, y, c)
    item_outline(t, hexc("#0d2448"))
    for x, y in ((2, 4), (13, 9), (3, 9), (12, 13)):
        t.set(x, y, SOUL[4])
    t.set(12, 0, SOUL[5])
    return t


def judgment_page():
    """An old torn page, a line of faded script, and a halo glyph drawn on it in gold over a single eye of light."""
    pal = {"o": O, "P": PAPER[5], "p": PAPER[4], "q": PAPER[3], "r": PAPER[2], "d": PAPER[1], "i": hexc("#4a3a26"),
           "g": P.GOLD[3], "G": P.GOLD[5], "h": P.GOLD[2], "e": SOUL[3], "E": SOUL[5]}
    return art([
        "..oooooooooooo..",
        ".oPpppPpppppqro.",
        ".oPiipiiipipqqo.",
        ".opppppppppppqo.",
        ".oppphgGGghpqro.",
        ".opphGppppGhqqo.",
        ".opgpppeEpppgqo.",
        ".opgpppEEpppgro.",
        ".opphGppppGhqqo.",
        ".opppghGGhgpqro.",
        ".opppppppppppqo.",
        ".opiipiiiipiqro.",
        ".opppppppppqqro.",
        ".oqpqppqpqqqrdo.",
        "..odoo.odroodo..",
        "...o....oo..o...",
    ], pal)


# =====================================================================================================
# The coat worn (vanilla humanoid armour layer, 64x32)
# =====================================================================================================

def box_faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def fill(t, rect, fn):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                t.set(x0 + x, y0 + y, c)


def gab(x, y, base=3.0):
    """Gabardine: a fine twill and soft vertical folds."""
    v = base + 0.5 * math.sin(x * 1.3 + 0.7) - (0.4 if (x + y) % 4 == 0 else 0)
    return TRENCH[max(0, min(5, int(round(v))))]


def coat_layer():
    t = Tex(64, 32, 396)
    body = box_faces(16, 16, 8, 12, 4)

    def front(x, y, w, h):
        # Open: the suit, the shirt and the loose tie in the middle (x 2..5), the lapels beside them, the belt at row 8.
        if 2 <= x <= 5:
            if y <= 4 and x in (3, 4):
                return TIE[3] if x == 3 else TIE[2]
            if y <= 2:
                return SHIRT[4] if x in (2, 5) else SHIRT[3]
            if x in (3, 4) and y <= 7:
                return TIE[2] if (x + y) % 3 else TIE[3]
            if y >= 9:
                return SUIT[2] if (x + y) % 3 else SUIT[3]
            return SUIT[3] if x in (2, 5) else SUIT[2]
        if y <= 4 and x in (1, 6):
            return TRENCH[5]                             # the lapels catch the light
        if y == 8:
            return TRENCH[1] if x in (0, 7) else TRENCH[2]
        if x in (1, 6) and y in (6, 10):
            return hexc("#6a4c2e")                       # the buttons
        if y == 11:
            return TRENCH[1]
        return gab(x, y, 3.0 if x not in (0, 7) else 2.6)

    fill(t, body["front"], front)
    fill(t, body["back"], lambda x, y, w, h: TRENCH[2] if y in (3, 8) else TRENCH[1] if (x == 4 and y > 8) else gab(x, y, 2.8))
    for side in ("right", "left"):
        fill(t, body[side], lambda x, y, w, h: TRENCH[2] if y == 8 else gab(x, y, 2.6))
    fill(t, body["top"], lambda x, y, w, h: TRENCH[4] if (x in (0, 7) or y in (0, 3)) else (SHIRT[3] if 2 <= x <= 5 else TRENCH[3]))
    fill(t, body["bottom"], lambda x, y, w, h: TRENCH[1])

    arm = box_faces(40, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        def sleeve(x, y, w, h, face=face):
            if y == 11:
                return SHIRT[3] if face in ("front", "back") and 1 <= x <= 2 else TRENCH[1]
            if y in (8, 9):
                if face == "right" and y == 8:
                    return hexc("#6a4c2e") if x == 1 else TRENCH[1]     # the cuff strap's buckle
                return TRENCH[2] if y == 8 else TRENCH[3]
            if y == 0:
                return TRENCH[4]
            return gab(x, y, 2.9 if face != "left" else 2.5)
        fill(t, arm[face], sleeve)
    fill(t, arm["top"], lambda x, y, w, h: TRENCH[4] if (x + y) % 3 else TRENCH[3])
    fill(t, arm["bottom"], lambda x, y, w, h: TRENCH[1])
    return t


# =====================================================================================================
# Crowley, King of Hell
# =====================================================================================================

def crowley_king():
    """Crowley's own rig and painter, then a crimson pocket square and a gold signet ring painted over his texels."""
    m = crowley_art.rig()
    t, g = m.build(gutter=1, seed=35000)

    def rect(bone, tag, face, nth=0):
        hits = [c for c in m.rig.get(bone).cubes if c.tag == tag]
        return hits[nth].rects[face]

    # The pocket square: a peaked fold of crimson silk in the breast pocket on his left, beside the lapel.
    u, v, w, h = rect("body", "jacket", "north")
    square = [".x.x", "xXxX", "XxXx", "mmmm"]
    for j, row in enumerate(square):
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            c = {"x": CRIMSON[4], "X": CRIMSON[3], "m": CRIMSON[1]}[ch]
            t.set(u + 11 + i, v + 7 + j, c)
    # The signet ring: a gold band round his right hand's last fingers, a dark red seal on its face.
    u, v, w, h = rect("right_fingers", "fingers", "west")
    for x in range(w):
        t.set(u + x, v, P.GOLD[4] if x != w // 2 else CRIMSON[2])
    u, v, w, h = rect("right_fingers", "fingers", "north")
    t.set(u, v, P.GOLD[3])
    u, v, w, h = rect("right_fingers", "fingers", "south")
    t.set(u + w - 1, v, P.GOLD[2])
    return m, t, g


# =====================================================================================================
# Particles (white with alpha: the code tints them)
# =====================================================================================================

def _white(a):
    return (255, 255, 255, max(0, min(255, int(a))))


def soul_wisp():
    """A soul: a soft round head trailing a curling tail of mist downwards."""
    t = Tex(16, 16, 3971)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 5.5)
            a = 0.0
            if d < 4.2:
                a = 255 * (1 - (d / 4.2) ** 2) + 30
            # The tail: a curl below the head.
            if 8 <= y <= 15:
                k = (y - 8) / 7
                cx = 7.5 + 2.4 * math.sin(k * 3.4)
                wd = 2.6 * (1 - k) + 0.4
                dd = abs(x - cx)
                if dd < wd:
                    a = max(a, 170 * (1 - dd / wd) * (1 - 0.8 * k))
            if a > 8:
                t.set(x, y, _white(a))
    return t


def grace_mote():
    """A mote of grace: a bright point with a four-pointed glint and a soft halo."""
    t = Tex(16, 16, 3972)
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            d = math.hypot(dx, dy)
            a = 0.0
            if d < 2.0:
                a = 255
            elif d < 5.0:
                a = 110 * (1 - (d - 2.0) / 3.0)
            if (dx < 0.6 and dy < 7.5) or (dy < 0.6 and dx < 7.5):
                a = max(a, 230 * (1 - max(dx, dy) / 7.5))
            if abs(dx - dy) < 0.6 and d < 4.5:
                a = max(a, 120 * (1 - d / 4.5))
            if a > 6:
                t.set(x, y, _white(a))
    return t


def goo_drop():
    """A drop of goo: a teardrop, point up, a wet highlight (lighter alpha) on its upper left."""
    t = Tex(16, 16, 3973)
    for y in range(1, 16):
        for x in range(16):
            ty = (y - 1) / 14
            half = 4.8 * (ty / 0.6) ** 1.4 if ty < 0.6 else 4.8 * math.sqrt(max(0.0, 1 - ((ty - 0.6) / 0.4) ** 2))
            dx = abs(x - 7.5)
            if dx > half:
                continue
            edge = half - dx
            a = 255 if edge > 0.8 else 170
            c = (225, 225, 225, a)
            if x < 7.5 and 0.45 < ty < 0.75 and edge > 1.0 and dx > half * 0.35:
                c = (255, 255, 255, a)
            t.set(x, y, c)
    return t


def shadow_feather():
    """A feather of shadow: a curved vane fraying into wisps at its tip, the shaft a little denser."""
    t = Tex(16, 16, 3974)
    rng = random.Random(3975)
    for i in range(160):
        s = i / 159
        # The shaft: a gentle curve from the quill (bottom left) to the tip (top right).
        x = 2.5 + 11 * s
        y = 14.0 - 12 * s + 2.4 * math.sin(s * math.pi)
        t.set(int(x), int(y), _white(235))
    for y in range(16):
        for x in range(16):
            # Distance to the shaft curve, sampled.
            best, bs = 99.0, 0.0
            for i in range(41):
                s = i / 40
                cx = 2.5 + 11 * s
                cy = 14.0 - 12 * s + 2.4 * math.sin(s * math.pi)
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d < best:
                    best, bs = d, s
            width = 3.4 * math.sin(math.pi * min(1.0, bs * 1.1)) + 0.3
            if best < width and bs > 0.08:
                a = 200 * (1 - best / width) ** 0.6
                if bs > 0.7 and rng.random() < (bs - 0.7) * 1.8:
                    continue                                 # wisps at the tip
                if (x * 3 + y) % 4 == 0:
                    a *= 0.7                                 # the barbs
                if t.rows[y][x][3] < a:
                    t.set(x, y, _white(a))
    return t


PARTICLES = {"purgatory_soul": soul_wisp, "grace_mote": grace_mote, "goo_drop": goo_drop, "shadow_feather": shadow_feather}


def generate():
    w, t, g, anims, height, grip = new_god_blade()
    w.rig.write(GEO + w.name + ".geo.json")
    anims.write(ANIM + w.name + ".animation.json")
    save(t, "item", w.name)
    save(g, "item", w.name + "_glowmask")
    save(preview3d.icon(w.rig, t, angle_deg=-45, hidden=("light",), pose=_SLIM), "item", w.name + "_icon")
    print(f"{w.name}: height {height:.2f}, grip middle {grip:.2f}")
    save(trenchcoat_sprite(), "item", "castiels_trenchcoat")
    save(grace_shard(), "item", "grace_shard")
    save(judgment_page(), "item", "judgment_page")
    save(coat_layer(), "models/armor", "castiels_trenchcoat_layer_1")
    _, kt, kg = crowley_king()
    save(kt, "entity", "crowley_king")
    save(kg, "entity", "crowley_king_glowmask")
    for name, fn in PARTICLES.items():
        save(fn(), "particle", name)


if __name__ == "__main__":
    generate()
