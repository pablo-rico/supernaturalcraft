"""The Seals (v0.40): the three mobs met at a breaking seal, the blocks and loot of the events, and the seals' GUI sheet.
Contract: SealAssets (paths, clips and their lengths, the bones the code needs).

Mobs (one geo per rig, shared by its variants; the michael_art skeleton, painted the way angel_art paints its vessels):

  witness_0..3     the Witnesses: grey-blue, see-through ghosts of ordinary people of different decades, a dark wound where
                   they were killed, hollow white eyes (the glow). The renderer draws them translucent and flickers their
                   alpha; it moves no bones. Their feet fade away (texture alpha).
                     0  a 1950s man: grey suit, white shirt and tie, a fedora; shot in the chest
                     1  a woman of the 1970s: a long brown-and-orange patterned dress, long hair; her throat cut
                     2  a teenager: a hoodie, jeans and sneakers; a dark stain at the side
                     3  a farmhand: denim overalls over a flannel shirt, a cap, a beard; three claw marks across the chest
  seal_reaper      a reaper at a seal: a gaunt man in a plain black suit, a black tie and a long black coat, grey skin,
                   hollow cheeks, slick black hair, black eyes (a faint gleam in the glowmask)
  rebel_angel_0..2 angels who side with Lucifer: vessels in a black suit (0), an olive army field jacket (1), a black leather
                   jacket over a dark hoodie (2, a woman); an angel blade in the right hand (bone `blade`, under the hand);
                   shadow wings (allegiance_art "shadow", their rims of light in the glowmask) under `wings`, which the
                   renderer hides except while the angel dies: no clip scales `wings` (the death clip spreads them).

  Accessories that only some variants wear (the hat and its brim, the skirt, the hood) are cubes in every variant's geo,
  painted clear on the others (as angel_art does with its coats), so the variants share the one model.

Blocks and items:
  models/block/seal_hex_focus.json   a knot of bones bound with hair and a black candle on a scorched sigil (textures
                                     block/seal_hex_focus_{sigil,bone,hair,candle})
  textures/block/blood_pool.png      16x128: 8 frames of 16x16 dark glossy blood (with .mcmeta: frametime 4, interpolated)
  textures/block/wing_scorch.png     32x32: the burnt shape of wings seen from above, clear outside
  textures/item/{witness_shroud, scorched_feather, plague_ash, sacrificial_ash, seal_compass}.png   16x16

The GUI sheet (textures/gui/seals.png, 256x128):
  (16*i, 0)    16x16  seal glyph i (0-11), whole: a pale stone disc with its sigil carved in red
  (16*i, 16)   16x16  seal glyph i, broken: split in two, the halves apart, the sigil dark, chips
  (192, 0)     16x16  family icon: the dead and spirits (a ghost's face)
  (208, 0)     16x16  family icon: angels (a wing)
  (224, 0)     16x16  family icon: plagues (a locust)
  (240, 0)     16x16  family icon: demonic rites (a black candle in a pentagram)
  (192, 16)    8x8    pip: a seal standing   (200, 16) pip: broken   (208, 16) pip: the next to fall (lit)
  (0, 32)      182x12 the rite bar's frame (dark iron, sigils at both ends); its inside starts at (3, 3), 176x6
  (0, 44)      176x6  the rite's fill (the demons' rite: dark red, a hot edge), drawn from the left to its progress
  (0, 50)      176x6  the held fill (the rite stopped / the seal defended: pale gold)
"""

import json
import math
import os
import random
import sys

import allegiance_art as AL
import angel_art as AN
import blockmodel as BM
import horsemen_art as H
import palette as P
from animkit import AnimFile
from chuck_art import Model, h01, none_on
from common import ASSETS, art, save
from michael_art import ANIM, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, fabric, skeleton, tone
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

CLEAR = (0, 0, 0, 0)
O = P.OUTLINE
E_BACK = "easeOutBack"


def clear(f, x, y, w, h):
    return CLEAR


# --- palettes -----------------------------------------------------------------------------------------------------

GHOST = Ramp("#2a3442", "#46546a", "#66778e", "#8c9db2", "#b6c4d4", "#dfe8f2")
GHOST_EYE, GHOST_WHITE = hexc("#ffffff"), hexc("#e9f1ff")
REAPER_SKIN = Ramp("#2e3033", "#45484c", "#5d6065", "#73777c", "#888c91", "#9da1a6")
BLACK = Ramp("#030304", "#08080a", "#0e0e11", "#151519", "#1e1e23", "#2a2a31")
DARK_SHIRT = Ramp("#0d0e11", "#16171b", "#202227", "#2b2e34", "#373a41", "#45484f")
GREY_SUIT = Ramp("#2a2c30", "#3c3f44", "#50545a", "#666a71", "#7c8088", "#949aa2")
DRESS = Ramp("#3a1c0c", "#5a2c12", "#7c3e18", "#a0561e", "#c27428", "#dc9640")
DRESS_PRINT = hexc("#e8c070")
OVERALL = Ramp("#18243a", "#22324e", "#2e4264", "#3c5478", "#4c668c", "#62809f")
KHAKI = Ramp("#2a2618", "#3c3622", "#4f472e", "#62593a", "#766c48", "#8a8058")
FELT = Ramp("#14100c", "#221a12", "#30251a", "#3e3022", "#4c3c2c", "#5c4a38")
CAP = Ramp("#3a1210", "#521a16", "#6c241e", "#863028", "#a03e32", "#b85040")
WOUND, BLOOD = hexc("#120406"), hexc("#3e0a0c")
STEEL = Ramp("#3a3f48", "#5c636e", "#808894", "#a6aeb9", "#c9d0d9", "#eef2f6")
REBEL_EYE, REBEL_WHITE = hexc("#c4cfe0"), hexc("#dfe4ec")


# =====================================================================================================
# Heads
# =====================================================================================================

def head(m, v, eye, white, glow=None):
    """angel_art.head_parts with the eyes' colours chosen (`eye` the iris, `white` the rest of the eye) and the face rows
    optionally edited by v["face_fx"](rows)."""
    sk, hr = v["skin"], v["hair"]
    seed = v["seed"]
    skin = H.skin_mat(sk, seed + 1, 3.0, 0.4)
    hair = lambda f, x, y, w, h: tone(hr, 2.7 + 1.1 * (fbm(x * 2 + len(f) * 7, y, seed + 9, 64, 64, 2, 8.0) - 0.5) * 2
                                      + 0.45 * math.sin(x * 1.9 + seed) - 0.6 * (y / max(1, h)))
    beard_r = hr if v.get("beard") == "full" else AN.HR_GREY
    beard = lambda f, x, y, w, h: tone(beard_r, 2.2 + 1.2 * math.sin(x * 2.1 + y * 1.3))
    stub = lambda f, x, y, w, h: mix(skin(f, x, y, w, h), AN.HR_GREY[3] if (x * 3 + y) % 3 else AN.HR_GREY[1], 0.55)
    legend = {"h": hair, "s": skin, "b": hr[1], "w": white, "E": eye, "l": hr[0], "u": sk[2], "U": mix(sk[1], hexc("#2a2a34"), 0.45),
              "n": sk[4], "N": sk[1], "m": mix(sk[1], hexc("#5a3436"), 0.4), "L": mix(sk[2], hexc("#8a4a4e"), 0.45),
              "B": beard, "k": stub, "f": mix(sk[2], hexc("#8a4a2a"), 0.5), "d": mix(sk[2], hexc("#2e261c"), 0.5), "c": hexc("#5a1a1a"),
              "j": sk[2], "W": WOUND, "R": BLOOD}
    rows = AN.face_rows(v)
    if v.get("face_fx"):
        rows = v["face_fx"](rows)
    side_beard = beard if v.get("beard") == "full" else (stub if v.get("beard") == "grey" else None)
    H.head_cube(m, rows, legend, H.head_sides(skin, hair, top=4, back=12, temple=7, sideburn=8, beard=side_beard, beard_from=11),
                glow=glow)
    m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), lambda f, x, y, w, h: sk[4] if f == "north" else sk[2], density=2, tag="nose")
    style = v["style"]

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "up" or f == "south":
            return hair(f, x, y, w, h)
        if f == "north":
            if style == "long":
                return hair(f, x, y, w, h) if y < 2 + (8 - x) // 2 or y < 2 else None
            if style == "slick":
                return hair(f, x, y, w, h) if y < 2 else None
            if style in ("crop", "cropgrey"):
                return hair(f, x, y, w, h) if y < 1 else None
            if style == "tousled":
                return hair(f, x, y, w, h) if y < 2 + (x % 5 == 2) + (x % 7 == 4) * 2 else None
            return hair(f, x, y, w, h) if y < 2 + (x > 9) else None
        fx = x if f == "west" else w - 1 - x
        lim = {"long": 8, "crop": 3, "cropgrey": 3, "tousled": 5, "short": 4, "slick": 4}[style]
        if y < 2 or (fx > 6 and y < lim + 2) or (fx > 3 and y < lim):
            return hair(f, x, y, w, h)
        return None
    crop = style in ("crop", "cropgrey")
    m.cube("head", (-4, 28, -4), (8, 4, 8), shell, density=2, inflate=0.12 if crop else 0.3, tag="hair")
    long_ = style == "long"

    def mane(f, x, y, w, h):
        if not long_ or f in ("north", "up"):
            return None
        if f == "down":
            return hair(f, x, y, w, h)
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            if y > h - 3 and h01("tip", seed, f, x) < 0.5:
                return None
            return hair(f, x, y, w, h) if fx >= 1 or y < 5 else None
        if y > h - 3 and h01("tip", seed, f, x) < 0.45:
            return None
        return hair(f, x, y, w, h)
    m.cube("head", (-4, 21, -2), (8, 9, 6), mane, density=2, inflate=0.45, tag="mane")


def hat_cubes(m, kind, seed):
    """A fedora (felt crown with a band, a full brim) or a cap (a peak in front); clear otherwise. Always the same cubes."""
    felt = fabric(FELT, seed, 3.0, 0.3)
    cap = fabric(CAP, seed + 1, 3.0, 0.3)

    def crown(f, x, y, w, h):
        if kind is None or f == "down":
            return None
        if kind == "fedora":
            if f == "up":
                return shade(felt(f, x, y, w, h), 0.85 if abs(x - (w - 1) / 2) < 1 else 1.0)   # the pinch along the top
            return hexc("#0c0908") if y >= h - 2 else felt(f, x, y, w, h)                    # the band
        return cap(f, x, y, w, h)
    m.cube("head", (-4.2, 31.4, -4.2), (8.4, 3, 8.4), crown, density=2, inflate=0.05, tag="hat_crown")

    def brim(f, x, y, w, h):
        if kind is None:
            return None
        if kind == "cap":
            # Only the peak: the front third of the brim.
            if f in ("up", "down"):
                return shade(cap(f, x, y, w, h), 0.8 if f == "down" else 1.0) if y < 5 and 3 <= x <= w - 4 else None
            return cap(f, x, y, w, h) if f == "north" and 3 <= x <= w - 4 else None
        return shade(felt(f, x, y, w, h), 0.7 if f == "down" else 1.05)
    m.cube("head", (-5.5, 31.3, -5.5), (11, 0.5, 11), brim, density=2, tag="hat_brim")


# =====================================================================================================
# Cloth
# =====================================================================================================

def wounded(mat, spots):
    """`spots` [(face, cx, cy, r)] in face texels: a dark hole with blood run below it."""
    def m(f, x, y, w, h):
        for face, cx, cy, r in spots:
            if f != face:
                continue
            d = math.hypot(x - cx, (y - cy) * 1.1)
            if d <= r:
                return WOUND
            if d <= r + 1.2:
                return mix(BLOOD, WOUND, 0.4)
            if abs(x - cx) <= 1 and cy < y < cy + 6 + r and h01("run", cx, x) < 0.8:
                return BLOOD
        return mat(f, x, y, w, h)
    return m


def slashes(mat, face, x0, y0, n=3, length=9):
    """Claw marks: n diagonal dark lines."""
    def m(f, x, y, w, h):
        if f == face:
            for i in range(n):
                u = y - y0 - (x - x0 - 2 * i)
                if abs(u - 2 * i) < 0.8 and x0 + 2 * i <= x <= x0 + 2 * i + length * 0.6 and y0 <= y <= y0 + length:
                    return WOUND
                if abs(u - 2 * i - 1) < 0.6 and x0 + 2 * i <= x <= x0 + 2 * i + length * 0.6 and y0 <= y <= y0 + length:
                    return BLOOD
        return mat(f, x, y, w, h)
    return m


def throat_cut(mat):
    def m(f, x, y, w, h):
        if f == "north" and y in (0, 1) and 3 <= x <= 12:
            return WOUND if y == 0 else BLOOD
        if f == "north" and 2 <= y <= 6 and x in (6, 9) and h01("drip", x, y) < 0.7:
            return BLOOD
        return mat(f, x, y, w, h)
    return m


def print_dress(seed):
    """A 70s print: warm brown with rows of pale flowers."""
    base = fabric(DRESS, seed, 3.0, 0.3)

    def m(f, x, y, w, h):
        c = base(f, x, y, w, h)
        if f in ("north", "south", "east", "west") and (x + (y // 4) * 2) % 5 == 0 and y % 4 == 1:
            return mix(c, DRESS_PRINT, 0.7)
        if f in ("north", "south", "east", "west") and (x + (y // 4) * 2) % 5 in (1, 4) and y % 4 in (0, 2) and (x + (y // 4) * 2) % 5 == 1:
            return mix(c, DRESS_PRINT, 0.35)
        return c
    return m


def bib_overall(denim):
    """Overalls' bib and straps on the jacket layer (clear elsewhere above the waist)."""
    def m(f, x, y, w, h):
        if f in ("up", "down"):
            return None
        if f == "north":
            if y >= 14 or (4 <= x <= 11 and y >= 5):
                c = denim(f, x, y, w, h)
                if y == 5 and 4 <= x <= 11:
                    return shade(c, 1.2)
                if y == 8 and x in (5, 10):
                    return hexc("#b8b0a0")    # the bib's buttons
                return c
            if x in (4, 5, 10, 11) and y < 5:
                return denim(f, x, y, w, h)
            return None
        if f == "south":
            if y >= 14 or (abs(x - (w - 1) / 2 - (y - 7) * 0.3) < 1.1 and y < 14) or (abs(x - (w - 1) / 2 + (y - 7) * 0.3) < 1.1 and y < 14):
                return denim(f, x, y, w, h)
            return None
        return denim(f, x, y, w, h) if y >= 14 else None
    return m


# =====================================================================================================
# The common body
# =====================================================================================================

def seed_of(name):
    return 40100 + sum(ord(c) * (i + 1) for i, c in enumerate(name))


def body(m, v, o, arm_w=4.0):
    """The torso's layers, arms, legs and the optional skirt and hood, as angel_art.rig does."""
    s = v["seed"]
    skin = H.skin_mat(v["skin"], s + 3, 3.0, 0.4)
    m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), o.get("neck") or skin, density=2, tag="neck")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o["torso"], density=2, tag="torso")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o.get("jacket") or clear, density=2, inflate=0.3, tag="jacket")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o.get("coat") or clear, density=2, inflate=0.6, tag="coat")
    belt = o.get("belt")
    m.cube("body", (-4, 11.6, -2), (8, 1.2, 4), (lambda f, x, y, w, h: hexc("#9c968a") if f == "north" and 6 <= x <= 9
           else belt[2 + (x % 7 == 0)]) if belt else clear, inflate=0.38, density=2, tag="belt")
    hood = o.get("hood")
    m.cube("body", (-3.5, 20.5, 2.0), (7, 3, 2), none_on(("north",), hood) if hood else clear, density=2, inflate=0.2, tag="hood")
    hand = o.get("hand") or H.skin_mat(v["skin"], s + 4, 3.1, 0.35)
    shoe = o["shoe"]
    for side in ("right", "left"):
        H.arm_cubes(m, side, o["sleeve"], hand, w=arm_w, sleeve_inflate=0.25)
        H.leg_cubes(m, side, o["trousers"], lambda f, x, y, w, h, sh=shoe: sh[4] if f == "up" and y < 3 else
                    (sh[4] if sh is AN.SNEAKER and y == h - 1 else sh[2]), shoe_h=2.0)


def skirt(m, mat):
    """A skirt from the waist to the knee, in its own bone so the walk can sway it (clear on the trousered)."""
    m.bone("skirt", (0, 12, 0), "body")

    def sk(f, x, y, w, h):
        if mat is None or f in ("up", "down"):
            return None
        c = mat(f, x, y, w, h)
        return shade(c, 0.8) if y == h - 1 else c
    m.cube("skirt", (-4.4, 4.6, -2.4), (8.8, 7.6, 4.8), sk, density=2, inflate=0.35, tag="skirt")


def coat_tails(m, tails):
    for side, sg in (("r", -1), ("l", 1)):
        b = m.bone(f"coat_tail_{side}", (2 * sg, 12, 0), "body")

        def tail(f, x, y, w, h, sg=sg):
            if tails is None or f in ("up", "down"):
                return None
            if f == "north" and ((sg < 0 and x >= w - 2) or (sg > 0 and x <= 1)):
                return None
            return tails(f, x, y, w, h)
        m.cube(b, (2 * sg - 2.2, 1.5, -2.4), (4.4, 10.5, 4.8), tail, density=2, tag="coat_tail")


# =====================================================================================================
# The Witnesses
# =====================================================================================================

WITNESSES = {
    0: dict(skin=AN.SK_FAIR, hair=AN.HR_BROWN, style="short", fallen=False, hat="fedora"),
    1: dict(skin=AN.SK_OLIVE, hair=AN.HR_BROWN, style="long", fallen=False, woman=True),
    2: dict(skin=AN.SK_FRECKLE, hair=AN.HR_DIRTY_BLONDE, style="tousled", fallen=False, freckles=True),
    3: dict(skin=AN.SK_LIGHT_TAN, hair=AN.HR_ASH, style="crop", fallen=False, beard="full", hat="cap"),
}


def witness_outfit(n, v):
    s = v["seed"]
    skin = H.skin_mat(v["skin"], s + 3, 3.0, 0.4)
    if n == 0:
        shirt = fabric(AN.SHIRT, s + 10, 3.8, 0.25)
        suit = fabric(GREY_SUIT, s + 11, 3.0, 0.3)
        jacket = AN.open_front(suit, v_depth=12, buttons=GREY_SUIT[0])
        return dict(torso=wounded(AN.shirt_front(shirt, AN.NAVY), [("north", 11, 9, 1.2)]),
                    jacket=wounded(jacket, [("north", 11, 9, 1.2)]), sleeve=suit,
                    trousers=fabric(GREY_SUIT, s + 12, 2.8, 0.15, crease=True), shoe=AN.BLACK_SHOE, belt=AN.BLACK_SHOE)
    if n == 1:
        dress = print_dress(s + 20)
        return dict(torso=throat_cut(dress), neck=lambda f, x, y, w, h: WOUND if f in ("north", "east", "west") and y == 1 else skin(f, x, y, w, h),
                    sleeve=dress, trousers=lambda f, x, y, w, h: shade(skin(f, x, y, w, h), 0.95), shoe=AN.BOOT, skirt=dress)
    if n == 2:
        hoodie = fabric(AN.HOODIE, s + 30, 3.2, 0.35)
        return dict(torso=wounded(AN.hoodie_front(hoodie), [("east", 4, 12, 1.6), ("north", 2, 13, 1.0)]), sleeve=AN.ribbed(hoodie),
                    trousers=AN.denim(s + 31), shoe=AN.SNEAKER, hood=hoodie)
    denim = AN.denim(s + 40, OVERALL)
    return dict(torso=slashes(AN.flannel(s + 41), "north", 3, 3), jacket=slashes(bib_overall(denim), "north", 3, 3),
                sleeve=AN.flannel(s + 42), trousers=denim, shoe=AN.BOOT)


def witness_rig(n):
    v = dict(WITNESSES[n])
    v["seed"] = seed_of(f"witness_{n}")
    m = Model("witness", 256, 256)
    skeleton(m)
    head(m, v, GHOST_EYE, GHOST_WHITE)
    hat_cubes(m, v.get("hat"), v["seed"] + 60)
    o = witness_outfit(n, v)
    body(m, v, o)
    skirt(m, o.get("skirt"))
    return m


def ghostify(m, t):
    """Turns a vessel's texture into a Witness: tinted grey-blue, see-through, the feet fading out; and its glowmask, a
    faint copy of the figure (scaled by its alpha) with the hollow eyes bright."""
    fade = {}
    for b, c in m.rig.cubes():
        if not b.name.endswith("_shin"):
            continue
        for face, (u, v0, w, h) in c.rects.items():
            for y in range(h):
                for x in range(w):
                    if face == "down":
                        k = 0.08
                    elif c.tag == "shoe":
                        k = 0.12
                    elif face == "up":
                        k = 0.6
                    else:
                        k = max(0.12, 0.7 - 0.55 * y / max(1, h - 1))
                    fade[(u + x, v0 + y)] = k
    g = Tex(t.w, t.h, 0)
    for y in range(t.h):
        for x in range(t.w):
            c = t.rows[y][x]
            if not c[3]:
                continue
            if c[:3] in (GHOST_EYE[:3], GHOST_WHITE[:3]):
                t.rows[y][x] = (c[0], c[1], c[2], 235)
                g.rows[y][x] = (230, 240, 255, 255) if c[:3] == GHOST_EYE[:3] else (150, 175, 210, 255)
                continue
            lum = (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255
            wound = lum < 0.09
            tint = GHOST.t(min(1.0, lum ** 0.75 * 1.15))
            out = mix(c, tint, 0.45 if wound else 0.82)
            n = fbm(x, y, 40999, t.w, t.h, 2, 32.0)
            a = 190 * fade.get((x, y), 1.0) * (0.86 + 0.28 * (n - 0.5))
            if wound:
                a = max(a, 170 * fade.get((x, y), 1.0))
            t.rows[y][x] = (out[0], out[1], out[2], int(max(18, min(205, a))))
            k = 0.16 * a / 255
            g.rows[y][x] = (int(out[0] * k), int(out[1] * k), int(min(255, out[2] * k * 1.1)), 255) if not wound else CLEAR
    return t, g


def witness_anims():
    f = AnimFile()
    clips = []
    A = "animation.witness."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    # idle: standing too still, the head cocked, a slow sway; the arms hanging, a little forward.
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        k = math.sin(math.pi * t / 2)
        c.key(t, {"head": [6 + 2 * k, [0, 10, 0, -6, 0][i], [12, 14, 12, 9, 12][i]], "body": [3, 0, 2 * k],
                  "right_arm": [-8, 0, 2], "left_arm": [-6, 0, -2], "right_forearm": [-8 - 2 * abs(k), 0, 0],
                  "left_forearm": [-6 - 2 * abs(k), 0, 0], "skirt": [0, 0, -1.5 * k]}, E_IO if i else None)

    # walk: a slow, stiff stalk; arms half raised towards you.
    c = clip("walk", 1.2, loop=True)
    for i in range(5):
        t = 1.2 * i / 4
        sn = math.sin(2 * math.pi * i / 4)
        c.key(t, {"right_leg": [-20 * sn, 0, 0], "left_leg": [20 * sn, 0, 0], "right_shin": [18 * max(0, math.sin(2 * math.pi * i / 4 + 1.2)), 0, 0],
                  "left_shin": [18 * max(0, -math.sin(2 * math.pi * i / 4 + 1.2)), 0, 0], "right_arm": [-34 + 4 * sn, 0, 4],
                  "left_arm": [-30 - 4 * sn, 0, -4], "right_forearm": [-12, 0, 0], "left_forearm": [-12, 0, 0],
                  "head": [8, -3 * sn, 10], "body": [6, 3 * sn, 0], "skirt": [6 * sn, 0, 0],
                  "@root": [0, 0.3 * abs(math.cos(2 * math.pi * i / 4)), 0]}, E_IO if i else None)

    # flicker (12 ticks): the image jumps -- head, body and arms snap to wrong places and back.
    c = clip("flicker", 0.6)
    rng = random.Random(40310)
    for i in range(13):
        t = round(0.05 * i, 2)
        if i in (0, 12):
            c.key(t, {"head": [0, 0, 0], "body": [0, 0, 0], "@body": [0, 0, 0], "right_arm": [0, 0, 0], "left_arm": [0, 0, 0]})
            continue
        j = lambda a: [round(rng.uniform(-a, a), 1) for _ in range(3)]
        c.key(t, {"head": j(28), "body": j(8), "@body": [round(rng.uniform(-1.4, 1.4), 1), 0, round(rng.uniform(-1, 1), 1)],
                  "right_arm": j(22), "left_arm": j(22)})

    # grasp (hit at 8 ticks): both hands lunge for your throat and close.
    c = clip("grasp", 0.8)
    rest = {"right_arm": [-8, 0, 2], "left_arm": [-6, 0, -2], "head": [6, 0, 12]}
    c.key(0, rest)
    c.key(0.25, {"right_arm": [-70, 0, 14], "left_arm": [-70, 0, -14], "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0],
                 "body": [-6, 0, 0], "head": [-6, 0, 0], "@root": [0, 0, 0.8]}, E_OUT)
    c.key(0.4, {"right_arm": [-92, 12, -8], "left_arm": [-92, -12, 8], "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0],
                "right_hand": [0, 0, -20], "left_hand": [0, 0, 20], "body": [14, 0, 0], "head": [-10, 0, 0], "@root": [0, 0, -2.4]}, E_SNAP)
    c.key(0.6, {"right_arm": [-88, 10, -6], "left_arm": [-88, -10, 6], "right_forearm": [-14, 0, 0], "left_forearm": [-14, 0, 0],
                "right_hand": [0, 0, -30], "left_hand": [0, 0, 30], "body": [12, 0, 0], "head": [-6, 0, 0], "@root": [0, 0, -2.0]})
    c.key(0.8, rest, E_IO)

    # wail (hit at 10 ticks): head thrown back, arms flung down and out, the whole figure shaking.
    c = clip("wail", 1.5)
    c.key(0, rest)
    c.key(0.3, {"head": [12, 0, 0], "body": [10, 0, 0], "right_arm": [10, 0, 10], "left_arm": [10, 0, -10]}, E_OUT)
    scream = {"head": [-42, 0, 0], "body": [-12, 0, 0], "right_arm": [-20, 0, 48], "left_arm": [-20, 0, -48],
              "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "right_hand": [0, 0, 14], "left_hand": [0, 0, -14],
              "@root": [0, 0.6, 0.4]}
    c.key(0.5, scream, E_BACK)
    c.key(1.2, P_(scream, {"head": [-46, 0, 0]}))
    c.key(1.5, rest, E_IO)
    trem = lambda amp, fx: (lambda t: [0.0, 0.0, 0.0] if t < 0.5 or t > 1.25 else
                            [amp * math.sin(t * fx), amp * 0.7 * math.sin(t * fx * 1.3), amp * 0.5 * math.sin(t * fx * 0.7)])
    c.layer("head", trem(3.0, 90))
    c.layer("body", trem(1.2, 77))

    # fade (30 ticks, held): the arms drop, it sags and sinks into the ground.
    c = clip("fade", 1.5, hold=True)
    c.key(0, rest)
    c.key(0.4, {"head": [24, 0, 18], "body": [10, 0, 0], "right_arm": [4, 0, 4], "left_arm": [4, 0, -4], "@root": [0, 0.4, 0]}, E_OUT)
    c.key(1.5, {"head": [36, 0, 22], "body": [22, 0, 0], "right_arm": [6, 0, 6], "left_arm": [6, 0, -6], "right_leg": [-30, 0, 0],
                "left_leg": [-30, 0, 0], "right_shin": [60, 0, 0], "left_shin": [60, 0, 0], "@root": [0, -14, 0]}, E_IN)

    for cl in clips:
        cl.done(step=0.05 if cl.anim.name.endswith(("wail",)) else 0.2)
    return f, clips


# =====================================================================================================
# The reaper
# =====================================================================================================

REAPER = dict(skin=REAPER_SKIN, hair=AN.HR_RAVEN, style="slick", fallen=False)


def gaunt(rows):
    """Hollow cheeks and sunken eyes."""
    out = [list(r) for r in rows]
    for y, xs in ((9, (1, 2, 13, 14)), (10, (2, 3, 12, 13)), (11, (2, 13))):
        for x in xs:
            if out[y][x] == "s":
                out[y][x] = "U"
    return ["".join(r) for r in out]


def reaper_rig():
    v = dict(REAPER, seed=seed_of("seal_reaper"), face_fx=gaunt)
    s = v["seed"]
    m = Model("seal_reaper", 256, 256)
    skeleton(m)
    eye, white = hexc("#050507"), hexc("#0b0b0e")

    def glow(f, x, y, w, h, c):
        if f == "north" and c[:3] == eye[:3]:
            return hexc("#2c3036")
        return None
    head(m, v, eye, white, glow)
    shirt = fabric(DARK_SHIRT, s + 1, 3.4, 0.25)
    suit = fabric(BLACK, s + 2, 3.2, 0.3)
    coat = fabric(BLACK, s + 3, 3.6, 0.45)
    o = dict(torso=AN.shirt_front(shirt, BLACK), jacket=AN.open_front(suit, v_depth=13, buttons=BLACK[5]),
             coat=AN.open_front(coat, half=3.0, collar=5, lapel=BLACK[5]), sleeve=coat,
             trousers=fabric(BLACK, s + 4, 2.8, 0.15, crease=True), shoe=AN.BLACK_SHOE,
             hand=H.skin_mat(REAPER_SKIN, s + 5, 2.8, 0.35))
    body(m, v, o, arm_w=3.6)
    coat_tails(m, coat)
    return m


def reaper_anims():
    f = AnimFile()
    clips = []
    A = "animation.seal_reaper."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    stand = {"right_arm": [-14, 0, -10], "left_arm": [-14, 0, 10], "right_forearm": [-36, -20, 0], "left_forearm": [-36, 20, 0]}
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        c.key(t, P_(stand, {"head": [2, [0, 14, 4, -12, 0][i], 0], "@body": [0, -0.1 * abs(math.sin(math.pi * t / 2)), 0]}),
              E_IO if i else None)

    c = clip("walk", 1.2, loop=True)
    for i in range(5):
        t = 1.2 * i / 4
        sn = math.sin(2 * math.pi * i / 4)
        c.key(t, {"right_leg": [-22 * sn, 0, 0], "left_leg": [22 * sn, 0, 0], "right_shin": [22 * max(0, math.sin(2 * math.pi * i / 4 + 1.2)), 0, 0],
                  "left_shin": [22 * max(0, -math.sin(2 * math.pi * i / 4 + 1.2)), 0, 0], "right_arm": [10 * sn, 0, 2],
                  "left_arm": [-10 * sn, 0, -2], "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0], "body": [1, 3 * sn, 0],
                  "coat_tail_r": [8 * max(0, -sn) + 3, 0, 1], "coat_tail_l": [8 * max(0, sn) + 3, 0, -1],
                  "@root": [0, 0.3 * abs(math.cos(2 * math.pi * i / 4)), 0]}, E_IO if i else None)

    # touch (hit at 10 ticks): two fingers raised to the forehead before it -- the reaper's touch.
    c = clip("touch", 1.0)
    reach = {"right_arm": [-112, -14, -6], "right_forearm": [-34, 0, 0], "right_hand": [-10, 0, 0], "head": [10, 0, 0], "body": [6, -8, 0],
             "left_arm": [-14, 0, 10], "left_forearm": [-36, 20, 0], "@root": [0, 0, -0.6]}
    c.key(0, stand)
    c.key(0.3, P_(reach, {"right_arm": [-80, -10, -4], "right_forearm": [-60, 0, 0]}), E_OUT)
    c.key(0.5, reach, E_IO)
    c.key(0.75, reach)
    c.key(1.0, stand, E_IO)

    # fade (30 ticks, held): a slow nod, then gone -- he narrows to a sliver.
    c = clip("fade", 1.5, hold=True)
    c.key(0, stand)
    c.key(0.6, P_(stand, {"head": [18, 0, 0]}), E_IO)
    c.key(1.1, P_(stand, {"head": [4, 0, 0], "%root": [1, 1, 1]}), E_IO)
    c.key(1.5, P_(stand, {"head": [4, 0, 0], "%root": [0.02, 1.08, 0.02]}), E_IN)

    for cl in clips:
        cl.done()
    return f, clips


# =====================================================================================================
# The rebel angels
# =====================================================================================================

REBELS = {
    0: dict(skin=AN.SK_FAIR, hair=AN.HR_BLACK, style="short", fallen=False),
    1: dict(skin=AN.SK_DARK, hair=AN.HR_BLACK, style="crop", fallen=False),
    2: dict(skin=AN.SK_PALE, hair=AN.HR_RAVEN, style="long", fallen=False, woman=True),
}


def rebel_outfit(n, v):
    s = v["seed"]
    if n == 0:
        shirt = fabric(DARK_SHIRT, s + 10, 3.4, 0.25)
        suit = fabric(BLACK, s + 11, 3.2, 0.3)
        return dict(torso=AN.shirt_front(shirt, BLACK), jacket=AN.open_front(suit, v_depth=13, buttons=BLACK[5]), sleeve=suit,
                    trousers=fabric(BLACK, s + 12, 2.8, 0.15, crease=True), shoe=AN.BLACK_SHOE, belt=AN.BLACK_SHOE)
    if n == 1:
        field = fabric(AN.OLIVE, s + 20, 3.0, 0.45)

        def jacket(f, x, y, w, h):
            c = AN.open_front(field, half=2.2, collar=4, zipper=hexc("#3a3a32"))(f, x, y, w, h)
            # Two chest pockets with flaps.
            if c is not None and f == "north" and 5 <= y <= 9 and (1 <= x <= 4 or 11 <= x <= 14):
                return shade(c, 0.8 if y == 5 else 1.0)
            return c
        return dict(torso=AN.tee_front(fabric(AN.TEE, s + 21, 2.4, 0.3), s), jacket=jacket, sleeve=field,
                    trousers=fabric(KHAKI, s + 22, 2.8, 0.3), shoe=AN.BOOT, belt=AN.BOOT)
    lea = H.skin_mat(BLACK, s + 30, 3.4, 1.0)
    hoodie = fabric(AN.CHARCOAL, s + 31, 2.8, 0.35)
    return dict(torso=AN.hoodie_front(hoodie), jacket=AN.open_front(lea, half=2.6, collar=4, zipper=hexc("#6c6c66")), sleeve=lea,
                trousers=AN.denim(s + 32, AN.BLACK_DENIM), shoe=AN.BOOT, hood=hoodie)


def blade_mat(f, x, y, w, h):
    """The angel blade: a long silver spike, a darker edge line, a cold glint."""
    if f in ("up", "down"):
        return STEEL[2]
    if y < 3:
        return STEEL[1]                         # the grip end in the fist
    c = STEEL[3 + (x == 0) - (x == w - 1)]
    if (y + x) % 9 == 0:
        c = STEEL[5]
    if y >= h - 2:
        c = STEEL[4]                            # the point
    return c


def rebel_rig(n):
    v = dict(REBELS[n])
    v["seed"] = seed_of(f"rebel_angel_{n}")
    m = Model("rebel_angel", 256, 512)
    skeleton(m)

    def eye_glow(f, x, y, w, h, c):
        if f != "north":
            return None
        if c[:3] == REBEL_EYE[:3]:
            return hexc("#b4c6e6")
        if c[:3] == REBEL_WHITE[:3]:
            return hexc("#4a5a78")
        return None
    head(m, v, REBEL_EYE, REBEL_WHITE, eye_glow)
    body(m, v, rebel_outfit(n, v))
    # The blade, point down out of the right fist.
    bl = m.bone("blade", (-6.0, 12.0, 0), "right_hand")
    m.cube(bl, (-6.4, 2.4, -0.4), (0.8, 9.6, 0.8), blade_mat, lambda f, x, y, w, h, c: shade(c, 0.12), density=4, tag="blade")
    m.bone("eye_l", (AN.EYE_X, AN.EYE_Y, AN.FACE_Z), "head")
    m.bone("eye_r", (-AN.EYE_X, AN.EYE_Y, AN.FACE_Z), "head")
    wb = m.bone("wings", (0, 20, 3.0), "body")
    AL.build_wings(m, wb.name, "shadow", off=(0.0, 19.8, 0.8), glow_rims=True)
    return m


def rebel_anims():
    f = AnimFile()
    clips = []
    A = "animation.rebel_angel."
    FOLDED, SPREAD, WIDE, UNFURL = AN.FOLDED, AN.SPREAD, AN.WIDE, AN.UNFURL

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    ready = {"right_arm": [-14, 0, 4], "right_forearm": [-30, 0, 0], "right_hand": [-30, 0, 0], "left_arm": [-4, 0, -4],
             "left_forearm": [-10, 0, 0]}
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        k = math.sin(math.pi * t / 2)
        c.key(t, P_(FOLDED, ready, {"@body": [0, -0.15 * abs(k), 0], "head": [-2 * abs(k), [0, 8, 0, -10, 0][i], 0],
                                    "right_forearm": [-30 - 3 * abs(k), 0, 0]}), E_IO if i else None)

    c = clip("walk", 1.0, loop=True)
    for i in range(5):
        t = i / 4
        sn = math.sin(2 * math.pi * t)
        c.key(t, P_(FOLDED, {"right_leg": [-26 * sn, 0, 0], "left_leg": [26 * sn, 0, 0],
                             "right_shin": [30 * max(0, math.sin(2 * math.pi * t + 1.2)), 0, 0],
                             "left_shin": [30 * max(0, -math.sin(2 * math.pi * t + 1.2)), 0, 0],
                             "right_arm": [12 * sn - 10, 0, 4], "right_forearm": [-28, 0, 0], "right_hand": [-30, 0, 0],
                             "left_arm": [-16 * sn, 0, -3], "left_forearm": [-12, 0, 0], "body": [3, 4 * sn, 0], "head": [-2, -4 * sn, 0],
                             "@root": [0, 0.5 * abs(math.cos(2 * math.pi * t)), 0]}), E_IO if i else None)

    # slash (hit at 8 ticks): the blade drawn back high over the right shoulder and cut down across the body.
    c = clip("slash", 0.8)
    wind = {"right_arm": [-150, 20, 30], "right_forearm": [-30, 0, 0], "right_hand": [-60, 0, 0], "body": [-4, 24, 0], "head": [0, -16, 0],
            "left_arm": [-34, 0, -20], "left_forearm": [-30, 0, 0], "right_leg": [8, 0, 0], "left_leg": [-10, 0, 0]}
    cut = {"right_arm": [-40, -40, -40], "right_forearm": [-10, 0, 0], "right_hand": [-80, 0, 0], "body": [14, -26, 0], "head": [6, 18, 0],
           "left_arm": [-10, 0, -30], "left_forearm": [-20, 0, 0], "right_leg": [16, 0, 0], "left_leg": [-24, 0, 0], "left_shin": [18, 0, 0],
           "@root": [0, -0.6, -1.6]}
    c.key(0, P_(FOLDED, ready))
    c.key(0.3, P_(FOLDED, wind), E_OUT)
    c.key(0.4, P_(FOLDED, cut), E_SNAP)
    c.key(0.55, P_(FOLDED, cut, {"body": [12, -24, 0], "@root": [0, -0.5, -1.4]}), E_OUT)
    c.key(0.8, P_(FOLDED, ready), E_IO)

    # smite (hit at 14 ticks): the left palm raised and thrust at you, fingers spread, the light let go.
    c = clip("smite", 1.3)
    draw = {"left_arm": [-60, 20, -30], "left_forearm": [-70, 0, 0], "left_hand": [0, 0, 0], "body": [0, -20, 0], "head": [-4, 14, 0],
            "right_arm": [-10, 0, 12], "right_forearm": [-30, 0, 0], "right_hand": [-30, 0, 0], "@root": [0, -0.4, 0.6]}
    thrust = {"left_arm": [-92, -6, -4], "left_forearm": [-2, 0, 0], "left_hand": [60, 0, 0], "body": [6, 10, 0], "head": [-8, -6, 0],
              "right_arm": [-6, 0, 18], "right_forearm": [-24, 0, 0], "right_hand": [-30, 0, 0], "right_leg": [14, 0, 0],
              "left_leg": [-22, 0, 0], "@root": [0, -0.5, -1.4]}
    c.key(0, P_(FOLDED, ready))
    c.key(0.5, P_(FOLDED, draw), E_OUT)
    c.key(0.7, P_(FOLDED, thrust), E_SNAP)
    c.key(1.0, P_(FOLDED, thrust, {"left_hand": [50, 0, 0]}))
    c.key(1.3, P_(FOLDED, ready), E_IO)
    trem = lambda t: [0.0, 0.0, 0.0] if not 0.7 <= t <= 1.0 else [2.4 * math.sin(t * 110), 0, 1.6 * math.sin(t * 87)]
    c.layer("left_arm", trem)

    # death (hit at 40 ticks = the light's peak; 60, held): run through -- rigid, head back, arms out, the wings burnt open,
    # the light pouring out; then the body drops to its knees and pitches forward.
    c = clip("death", 3.0, hold=True)
    rigid = {"head": [-38, 0, 0], "body": [-10, 0, 0], "right_arm": [-12, 0, 82], "left_arm": [-12, 0, -82],
             "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_hand": [0, 0, 10], "left_hand": [0, 0, -10],
             "right_leg": [4, 0, 5], "left_leg": [4, 0, -5], "@root": [0, 0.6, 0.6]}
    c.key(0, P_(FOLDED, ready))
    c.key(0.1, P_(UNFURL, {"head": [-10, 0, 0], "body": [-3, 0, 0], "right_arm": [-6, 0, 30], "left_arm": [-6, 0, -30]}), E_OUT)
    c.key(0.4, P_(WIDE, rigid), E_OUT)
    c.key(2.0, P_(WIDE, rigid, {"head": [-46, 0, 0], "body": [-14, 0, 0], "right_arm": [-16, 0, 90], "left_arm": [-16, 0, -90],
                                "@root": [0, 1.0, 0.8]}), E_IO)
    knees = {"@root": [0, -5.6, 0], "right_leg": [-4, 0, 4], "left_leg": [-4, 0, -4], "right_shin": [88, 0, 0], "left_shin": [88, 0, 0],
             "body": [30, 0, 4], "head": [40, 0, -10], "right_arm": [-6, 0, 10], "left_arm": [-4, 0, -12],
             "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0]}
    c.key(2.5, P_(SPREAD, knees), E_IN)
    c.key(3.0, P_(AN.wings(dict(AL.OPEN, arm=(0, 16, 6))), knees, {"body": [58, 0, 4], "head": [30, 0, -10], "@root": [0, -6.4, -2.4]}),
          E_IN)
    trem2 = lambda amp, fx, fy: (lambda t: [0.0, 0.0, 0.0] if t < 0.4 or t > 2.1 else
                                 [amp * math.sin(t * fx), amp * 0.6 * math.sin(t * fy), amp * 0.5 * math.sin(t * (fx + fy) * 0.5)])
    c.layer("head", trem2(1.6, 97, 71))
    c.layer("body", trem2(0.7, 83, 59))
    c.layer("right_arm", trem2(1.2, 91, 67))
    c.layer("left_arm", trem2(1.2, 79, 101))

    for cl in clips:
        cl.done(step=0.05 if cl.anim.name.endswith(("death", "smite")) else 0.2)
    return f, clips


# =====================================================================================================
# Blocks
# =====================================================================================================

def tex16(fn, w=16, h=16, seed=0):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


BONE = Ramp("#4a4336", "#6e6552", "#948a72", "#b6ab90", "#d2c8ae", "#ebe3cc")
HAIRK = Ramp("#0a0807", "#141010", "#1e1816", "#2a2220", "#38302c", "#463c36")
WAX = Ramp("#040405", "#09090b", "#101013", "#18181c", "#222228", "#2e2e36")
CHARR = Ramp("#0a0807", "#15110e", "#221b16", "#30271f", "#40352a", "#524436")
HEX_RED = Ramp("#2a0204", "#4a0608", "#700c0e", "#9a1814", "#c8301c", "#f0602a")


def hex_focus():
    """The hex: a scorched sigil burnt into the ground (a ring and a pentagram in dried blood), a knot of three bones bound
    with black hair, a black candle burning beside it."""
    def sigil(x, y):
        dx, dy = x - 7.5, y - 7.5
        r = math.hypot(dx, dy)
        ang = math.atan2(dy, dx)
        n = fbm(x * 3, y * 3, 40501, 48, 48, 2, 6.0)
        # The pentagram's five lines.
        star = False
        for i in range(5):
            a0 = -math.pi / 2 + i * 4 * math.pi / 5
            a1 = a0 + 4 * math.pi / 5
            ax, ay, bx, by = 6.2 * math.cos(a0), 6.2 * math.sin(a0), 6.2 * math.cos(a1), 6.2 * math.sin(a1)
            L2 = (bx - ax) ** 2 + (by - ay) ** 2
            t = max(0, min(1, ((dx - ax) * (bx - ax) + (dy - ay) * (by - ay)) / L2))
            if math.hypot(dx - ax - t * (bx - ax), dy - ay - t * (by - ay)) < 0.55:
                star = True
        if 6.6 < r < 7.6 or star:
            return mix(HEX_RED[2], HEX_RED[1], n) if h01("sg", x, y) > 0.1 else None
        if r < 7.0 and n > 0.45:
            return (CHARR[1][0], CHARR[1][1], CHARR[1][2], int(80 + 120 * (n - 0.45)))   # scorch
        _ = ang
        return None
    bone = tex16(lambda x, y: tone(BONE, 3.0 + 0.8 * math.sin(x * 0.9 + y * 0.4) + (0.8 if y % 5 == 0 else 0) - (0.7 if x in (0, 15) else 0)))
    hairt = tex16(lambda x, y: tone(HAIRK, 2.4 + 1.4 * math.sin(x * 2.1 + y * 0.3) + (h01("hk", x, y) - 0.5)))

    def candle(x, y):
        # The upper 6 rows: a flame and wick (shade false in the model); below, black wax with drips.
        if y < 5:
            d = abs(x - 7.5) / (1.0 + y * 0.45)
            if d < 1:
                return [hexc("#fff2c0"), hexc("#ffd060"), hexc("#ff9a28"), hexc("#e05a14"), hexc("#7a1c08")][min(4, int(d * 2.2 + (4 - y) * 0.6))]
            return None
        if y == 5:
            return hexc("#1a1210") if 7 <= x <= 8 else None
        c = tone(WAX, 2.6 + 0.8 * math.sin(x * 1.1) - 0.6 * (y - 6) / 10)
        if y == 6 or (h01("drip", x) < 0.3 and y < 6 + 3 + int(h01("dl", x) * 5)):
            c = WAX[5]
        return c
    save(tex16(sigil, seed=40500), "block", "seal_hex_focus_sigil")
    save(bone, "block", "seal_hex_focus_bone")
    save(hairt, "block", "seal_hex_focus_hair")
    save(tex16(candle, seed=40502), "block", "seal_hex_focus_candle")
    e = [
        BM.cube("sigil", (0, 0.05, 0), (16, 0.15, 16), {"up": "sigil", "down": "sigil"}, uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}),
        BM.cube("bone_a", (4.0, 0.2, 7.0), (12.0, 1.6, 8.6), {"*": "bone"}, rot=("y", 22.5, (8, 1, 8))),
        BM.cube("bone_b", (5.0, 0.2, 7.2), (11.5, 1.4, 8.6), {"*": "bone"}, rot=("y", -45, (8, 1, 8))),
        BM.cube("bone_c", (7.2, 0.4, 4.5), (8.8, 7.0, 6.0), {"*": "bone"}, rot=("x", -22.5, (8, 0.5, 5.2))),
        BM.cube("knot", (6.6, 0.8, 6.6), (9.4, 3.0, 9.4), {"*": "hair"}),
        BM.cube("knot_top", (7.0, 2.8, 5.4), (9.0, 4.0, 7.0), {"*": "hair"}, rot=("x", -22.5, (8, 3, 6))),
        BM.cube("candle", (11.0, 0.2, 10.0), (13.0, 6.2, 12.0), {"*": "candle", "up": "candle"},
                uv={"north": [6, 6, 10, 16], "south": [6, 6, 10, 16], "east": [6, 6, 10, 16], "west": [6, 6, 10, 16], "up": [6, 6, 8, 8]}),
        BM.cube("flame_a", (12.0, 6.2, 10.0), (12.0, 9.2, 12.0), {"east": "candle", "west": "candle"},
                uv={"east": [6, 0, 10, 6], "west": [6, 0, 10, 6]}, rot=("y", 45, (12, 7, 11)), shade=False),
        BM.cube("flame_b", (11.0, 6.2, 11.0), (13.0, 9.2, 11.0), {"north": "candle", "south": "candle"},
                uv={"north": [6, 0, 10, 6], "south": [6, 0, 10, 6]}, rot=("y", 45, (12, 7, 11)), shade=False),
    ]
    mdl = BM.model({"sigil": "seal_hex_focus_sigil", "bone": "seal_hex_focus_bone", "hair": "seal_hex_focus_hair",
                    "candle": "seal_hex_focus_candle"}, e, "seal_hex_focus_bone")
    BM.write("seal_hex_focus", mdl)


def blood_pool():
    """8 frames of 16x16: dark glossy blood, a ragged edge, a slow sheen sliding across."""
    t = Tex(16, 128, 40600)
    BLOODR = Ramp("#1a0103", "#2c0306", "#40060a", "#560a0e", "#701216", "#8e2420")
    for fr in range(8):
        ph = fr / 8 * 2 * math.pi
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy * 1.1)
                edge = 7.2 + 1.2 * (fbm(math.atan2(dy, dx) * 8 + 32, 3, 40601, 64, 64, 2, 6.0) - 0.5) * 2
                if r > edge:
                    continue
                n = fbm(x * 2 + fr * 0.6, y * 2, 40602, 32, 32, 2, 6.0)
                v = 2.2 + (n - 0.5) * 1.2 - 0.8 * max(0, r - edge + 1.6)
                sheen = math.exp(-((dx * 0.7 + dy * 0.7) - 3.5 * math.sin(ph)) ** 2 / 3.0) * (1 - r / edge)
                c = mix(tone(BLOODR, v), hexc("#b04a40"), min(0.55, sheen * 0.7))
                t.set(x, fr * 16 + y, (c[0], c[1], c[2], 235 if r < edge - 1 else 200))
    path = save(t, "block", "blood_pool")
    with open(path + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": 4, "interpolate": True}}, fh, indent=1)
        fh.write("\n")


def wing_scorch_block():
    """A 32x32 single-block decal: the burnt shape of spread wings seen from above (the body's mark at the bottom middle),
    charcoal feathers with an ash seam between them, ragged ash at the edges, a few embers; clear outside."""
    W = 32
    t = Tex(W, W, 40700)
    cx, sy = 16.0, 24.0
    caps = []
    for s_ in (-1, 1):
        sh, wrist, tip = (cx + s_ * 1.5, sy - 3.5), (cx + s_ * 8.5, sy - 11.0), (cx + s_ * 15.0, sy - 15.5)
        caps.append((sh, wrist, 1.4))
        caps.append((wrist, tip, 1.0))
        for i in range(9):
            u = i / 8
            if u < 0.6:
                k = u / 0.6
                ax, ay = sh[0] + (wrist[0] - sh[0]) * k, sh[1] + (wrist[1] - sh[1]) * k
            else:
                k = (u - 0.6) / 0.4
                ax, ay = wrist[0] + (tip[0] - wrist[0]) * k, wrist[1] + (tip[1] - wrist[1]) * k
            ang = math.radians(90 - s_ * (6 + 50 * u ** 1.3))
            L = 7.0 + 5.0 * u - 3.0 * u * u
            caps.append(((ax, ay), (ax + math.cos(ang) * L, ay + math.sin(ang) * L), 1.25 - 0.35 * u))
    for y in range(W):
        for x in range(W):
            px, py = x + 0.5, y + 0.5
            d1 = d2 = 99.0
            for (ax, ay), (bx, by), wd in caps:
                d = _seg(px, py, ax, ay, bx, by) - wd
                if d < d1:
                    d1, d2 = d, d1
                elif d < d2:
                    d2 = d
            bd = math.hypot((px - cx) / 2.6, (py - sy) / 4.2) * 2.6 - 2.6
            if bd < d1:
                d1, d2 = bd, 99.0
            n = fbm(x, y, 40701, W, W, 3, 8.0)
            edge = d1 + (n - 0.5) * 1.2
            if edge > 0.9:
                if edge < 2.2 and h01("ash", x, y) < 0.12:
                    t.set(x, y, (112, 106, 98, 110))
                continue
            core = max(0.0, min(1.0, (0.4 - edge) / 1.6))
            col = mix(hexc("#4a423b"), hexc("#0d0a09"), core)
            a = 255 if edge < 0 else int(255 * (0.9 - edge))
            if d2 < 0.2 and abs(d1 - d2) < 0.45:
                col = mix(col, hexc("#3a322c"), 0.55)
            if -0.6 < edge < 0.8 and h01("ember", x, y) < 0.025:
                col, a = AN.EMBER[4], 255
            t.set(x, y, (col[0], col[1], col[2], max(60, a)))
    save(t, "block", "wing_scorch")


# =====================================================================================================
# Items
# =====================================================================================================

def witness_shroud():
    """A grey-blue burial shroud, folded over on itself, faintly see-through, an old dark stain."""
    G = GHOST
    pal = {"o": hexc("#1c2430"), "L": G[5], "l": G[4], "m": G[3], "d": G[2], "D": G[1], "w": WOUND, "r": BLOOD}
    t = art([
        "................",
        "...oooooooo.....",
        "..oLLLLLLLLoo...",
        "..oLlllllllLLo..",
        "..olmmlmmlmllo..",
        "..olmmlmmlmmlo..",
        "..oLLLLLLLLLLo..",
        "..odddddddddLo..",
        ".oLlllllllllLo..",
        ".olmmlmmrwmmlo..",
        ".olmmlmmrrmmdo..",
        ".olmmlmmmlmmdo..",
        ".oLLLLLLLLLLdo..",
        ".oddddDddDdddo..",
        "..ooooooooooo...",
        "................",
    ], pal)
    for y in range(16):
        for x in range(16):
            c = t.rows[y][x]
            if c[3] and c[:3] != pal["o"][:3]:
                t.rows[y][x] = (c[0], c[1], c[2], 225)
    return t


def scorched_feather():
    """A long charred feather, its vane eaten ragged, the tip still glowing."""
    t = Tex(16, 16, 40801)
    for i in range(14):
        x, y = 2 + i * 0.82, 14 - i * 0.9
        for k in range(-2, 3):
            px, py = int(round(x + k * 0.6)), int(round(y + k * 0.6))
            if abs(k) == 2 and h01("rag", i, k) < 0.45:
                continue
            c = AL.AL_CHAR[1.5 + 1.6 * (i / 14) - abs(k) * 0.3]
            if i > 11 and abs(k) < 2:
                c = mix(c, AL.AL_EMBER[4], 0.6)
            t.set(px, py, c)
        t.set(int(round(x)), int(round(y)), hexc("#5a5048"))
    item_outline(t, O)
    t.set(14, 2, AL.AL_EMBER[5])
    return t


def ash_pile(seed, ramp, motes):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, (y - 11.0) * 1.9
            r = math.hypot(dx, dy)
            lim = 6.2 + (fbm(x * 3, y, seed, 48, 48, 2, 6.0) - 0.5) * 2.0
            if y < 6 or r > lim:
                continue
            n = fbm(x * 2, y * 2, seed + 1, 32, 32, 2, 5.0)
            t.set(x, y, tone(ramp, 3.0 + (n - 0.5) * 2 - 1.2 * (y - 6) / 10 + (0.6 if dy < -3 else 0)))
    for x, y, c in motes:
        t.set(x, y, c)
    item_outline(t, O)
    return t


def compass():
    """A tarnished brass compass: its glass cracked across a red seal glyph, the needle pointing off true."""
    BR = Ramp("#2a1c0a", "#4a3414", "#6e5222", "#94722e", "#b8963e", "#d6b860")
    t = Tex(16, 16, 40901)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 8.0)
            if r < 6.4:
                c = tone(Ramp("#20262a", "#2e363a", "#3c464a", "#4c585c", "#5e6a6e", "#748084"), 2.6 - r * 0.2)
                if 3.6 < r < 4.6:
                    c = HEX_RED[3] if (int(math.degrees(math.atan2(y - 8, x - 7.5))) // 30) % 2 == 0 else HEX_RED[2]
                t.set(x, y, c)
            elif r < 7.6:
                n = fbm(x * 3, y * 3, 40902, 48, 48, 2, 6.0)
                c = tone(BR, 3.0 + (n - 0.5) * 2 - (0.8 if y > 9 else 0))
                if n > 0.66:
                    c = mix(c, hexc("#3e6a52"), 0.5)    # verdigris
                t.set(x, y, c)
    t.set(7, 0, BR[3]); t.set(8, 0, BR[3]); t.set(7, 1, BR[4]); t.set(8, 1, BR[2])
    # The needle (red tip up-right), the crack across the glass.
    for i in range(5):
        t.set(8 + i * 0.7, 7 - i * 0.7, HEX_RED[4] if i > 1 else HEX_RED[3])
        t.set(7 - i * 0.7, 9 + i * 0.7, hexc("#c8ccd0"))
    for x, y in ((3, 5), (4, 6), (5, 6), (6, 7), (8, 9), (9, 10), (10, 10), (11, 11)):
        t.set(x, y, hexc("#d6e2e6"))
    item_outline(t, O)
    return t


# =====================================================================================================
# The GUI sheet
# =====================================================================================================

STONE = Ramp("#3a342e", "#5a5248", "#7a7064", "#9a9082", "#b8ae9e", "#d2c8b8")


def _seg(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    L2 = dx * dx + dy * dy
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L2)) if L2 else 0.0
    return math.hypot(px - ax - t * dx, py - ay - t * dy)


def glyph_strokes(i):
    """Seal i's sigil: a few strokes inside the disc, picked from a stable hash (no two alike)."""
    rng = random.Random(41000 + i * 97)
    strokes = []
    n = 3 + i % 3
    for k in range(n):
        a = rng.uniform(0, math.pi * 2)
        if k == 0:
            strokes.append((-3.6 * math.cos(a), -3.6 * math.sin(a), 3.6 * math.cos(a), 3.6 * math.sin(a)))
            continue
        ax, ay = rng.uniform(-3, 3), rng.uniform(-3, 3)
        L = rng.uniform(2.2, 4.2)
        strokes.append((ax, ay, ax + L * math.cos(a), ay + L * math.sin(a)))
    dots = [(rng.uniform(-2.5, 2.5), rng.uniform(-2.5, 2.5)) for _ in range(i % 2 + 1)]
    return strokes, dots


def seal_glyph(t, ox, oy, i, broken):
    strokes, dots = glyph_strokes(i)
    cut = random.Random(42000 + i).uniform(-0.6, 0.6)   # the crack's slope
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - 8, y + 0.5 - 8
            half = 0
            if broken:
                side = py - cut * px
                if abs(side) < 1.1:
                    continue                          # the crack
                half = 1 if side > 0 else -1
                px -= half * 0.9 * -cut / math.hypot(1, cut)
                py -= half * 0.9 / math.hypot(1, cut)
                px, py = px + half * 0.9, py + half * 1.6
            r = math.hypot(px, py)
            if r > 7.2:
                continue
            n = fbm(x * 3 + i * 16, y * 3, 41500, 256, 48, 2, 24.0)
            c = tone(STONE, 3.2 - r * 0.18 + (n - 0.5) * 1.4 - (0.9 if r > 6.3 else 0) + (0.5 if py < -3 else 0))
            on = any(_seg(px, py, *s) < 0.62 for s in strokes) or any(math.hypot(px - dx, py - dy) < 0.9 for dx, dy in dots) \
                or 4.9 < r < 5.6
            if on:
                c = (HEX_RED[3] if not broken else mix(HEX_RED[1], STONE[1], 0.5))
            if broken and h01("chip", i, x, y) < 0.05 and r > 5.5:
                continue
            t.set(ox + x, oy + y, c)


def family_icons(t):
    # The dead: a pale ghost's face with hollow eyes.
    ghost = [
        "................",
        ".....oooooo.....",
        "....oLLLLLLo....",
        "...oLLlllLLLo...",
        "...oLokllkoLo...",
        "...oLkkllkkLo...",
        "...oLLlllLLLo...",
        "...oLlLookLLo...",
        "...oLllooklLo...",
        "...oLLlllLLLo...",
        "...oLlLLllLLo...",
        "...oLLlLLlLLo...",
        "...oLoLLoLoLo...",
        "...oo.oo.o.oo...",
        "................",
        "................",
    ]
    t.paste(art(ghost, {"o": hexc("#1c2430"), "L": GHOST[5], "l": GHOST[4], "k": hexc("#0a0c12")}), 192, 0)
    # Angels: a grey wing of light.
    tt = Tex(16, 16, 0)
    for k in range(6):
        for j in range(9 - k):
            x, y = 3 + k * 2 + j * 0.25, 3 + j + k * 0.6
            tt.set(int(x), int(y), mix(hexc("#e6ecf4"), hexc("#7a8698"), j / 9))
            tt.set(int(x) + 1, int(y), mix(hexc("#c4ccd8"), hexc("#5c667a"), j / 9))
    for i in range(10):
        tt.set(2 + i, 3 + i // 4, hexc("#ffffff"))
    item_outline(tt, hexc("#1a1e28"))
    t.paste(tt, 208, 0)
    # Plagues: a locust.
    loc = [
        "................",
        "................",
        "..o.........o...",
        "...o.......o....",
        "....oooooooo....",
        "...ogGGGGGGgo...",
        "..oggwwwwwwGgo..",
        ".ogGwwwWWwwwGgo.",
        ".oGgwwwWWwwwgGo.",
        "..ogGGGGGGGGgo..",
        "...oggogogoggo..",
        "...o.o.o.o.o.o..",
        "..o..o...o...o..",
        "................",
        "................",
        "................",
    ]
    t.paste(art(loc, {"o": hexc("#141008"), "g": hexc("#4a4a1c"), "G": hexc("#6e6a28"), "w": hexc("#9a9468"),
                      "W": hexc("#c4bc8a")}), 224, 0)
    # Rites: a black candle in a red pentagram.
    tt = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 9.5
            r = math.hypot(dx, dy)
            if 5.6 < r < 6.6:
                tt.set(x, y, HEX_RED[3])
            for i in range(5):
                a0 = -math.pi / 2 + i * 4 * math.pi / 5
                a1 = a0 + 4 * math.pi / 5
                if _seg(dx, dy, 5.6 * math.cos(a0), 5.6 * math.sin(a0), 5.6 * math.cos(a1), 5.6 * math.sin(a1)) < 0.5:
                    tt.set(x, y, HEX_RED[2])
    for y in range(5, 13):
        for x in (7, 8):
            tt.set(x, y, WAX[2 + (x == 7)])
    tt.set(7, 4, hexc("#ffd060")); tt.set(8, 4, hexc("#ff9a28")); tt.set(7, 3, hexc("#fff2c0")); tt.set(8, 2, hexc("#ff9a28"))
    t.paste(tt, 240, 0)


def pips(t):
    for k, (ox, col) in enumerate(((192, STONE[3]), (200, STONE[1]), (208, HEX_RED[4]))):
        for y in range(8):
            for x in range(8):
                r = math.hypot(x - 3.5, y - 3.5)
                if r > 3.6:
                    continue
                c = col if r < 2.6 else shade(col, 0.6)
                if k == 1 and abs((y - 3.5) - 0.4 * (x - 3.5)) < 0.6:
                    continue                                   # cracked through
                if k == 2 and r < 1.2:
                    c = hexc("#ffb070")
                t.set(ox + x, 16 + y, c)


def rite_bar(t):
    IRON = Ramp("#0c0c0e", "#18181c", "#24242a", "#323238", "#44444c", "#5a5a64")
    W, Hh = 182, 12
    for y in range(Hh):
        for x in range(W):
            inside = 3 <= x < 179 and 3 <= y < 9
            if inside:
                t.set(x, 32 + y, (10, 4, 6, 230))
                continue
            n = fbm(x * 2, y * 2, 41700, 364, 24, 2, 32.0)
            c = tone(IRON, 2.6 + (n - 0.5) * 1.6 + (0.9 if y == 0 else 0) - (0.9 if y == Hh - 1 else 0))
            if y in (1, 10) and x % 6 == 3:
                c = IRON[5]                                     # rivets
            t.set(x, 32 + y, c)
    # A small sigil at either end.
    for ox in (0, 170):
        for y in range(12):
            for x in range(12):
                r = math.hypot(x - 5.5, y - 5.5)
                if r < 5.6:
                    c = IRON[2] if r < 4.6 else IRON[4]
                    if abs(x - 5.5) < 0.6 or abs(y - 5.5) < 0.6 or 2.6 < r < 3.4:
                        c = HEX_RED[3]
                    t.set(ox + x, 32 + y, c)
    # The fills.
    for x in range(176):
        for y in range(6):
            n = fbm(x, y * 4, 41800, 176, 24, 2, 22.0)
            c = tone(HEX_RED, 2.2 + (n - 0.5) * 1.6 + (1.2 if y == 1 else 0) - (0.8 if y == 5 else 0))
            if x >= 172:
                c = mix(c, hexc("#ffb070"), (x - 171) / 4)      # the hot leading edge
            t.set(x, 44 + y, c)
            GOLDR = Ramp("#5a4420", "#80662e", "#a88a44", "#cbb062", "#e6d290", "#fff2c8")
            g = tone(GOLDR, 2.6 + (n - 0.5) * 1.4 + (1.2 if y == 1 else 0) - (0.8 if y == 5 else 0))
            t.set(x, 50 + y, g)


def gui_sheet():
    t = Tex(256, 128, 41900)
    for i in range(12):
        seal_glyph(t, 16 * i, 0, i, False)
        seal_glyph(t, 16 * i, 16, i, True)
    family_icons(t)
    pips(t)
    rite_bar(t)
    save(t, "gui", "seals")


# =====================================================================================================
# Output
# =====================================================================================================

def generate():
    for n in range(4):
        m = witness_rig(n)
        t, _ = m.build(gutter=1, seed=40000)
        t, g = ghostify(m, t)
        save(t, "entity", f"witness_{n}")
        save(g, "entity", f"witness_{n}_glowmask")
    m.rig.write(GEO + "witness.geo.json")
    witness_anims()[0].write(ANIM + "witness.animation.json")
    m = reaper_rig()
    t, g = m.build(gutter=1, seed=40020)
    m.rig.write(GEO + "seal_reaper.geo.json")
    save(t, "entity", "seal_reaper")
    save(g, "entity", "seal_reaper_glowmask")
    reaper_anims()[0].write(ANIM + "seal_reaper.animation.json")
    for n in range(3):
        m = rebel_rig(n)
        t, g = m.build(gutter=1, seed=40040)
        save(t, "entity", f"rebel_angel_{n}")
        save(g, "entity", f"rebel_angel_{n}_glowmask")
    m.rig.write(GEO + "rebel_angel.geo.json")
    rebel_anims()[0].write(ANIM + "rebel_angel.animation.json")
    hex_focus()
    blood_pool()
    wing_scorch_block()
    save(witness_shroud(), "item", "witness_shroud")
    save(scorched_feather(), "item", "scorched_feather")
    save(ash_pile(40810, Ramp("#2a2a1c", "#3e3e28", "#56543a", "#706c4e", "#8a8462", "#a49c7a"),
                  [(4, 4, hexc("#5a5a20")), (11, 3, hexc("#4a4a18")), (12, 5, hexc("#6e6a28"))]), "item", "plague_ash")
    save(ash_pile(40820, Ramp("#141010", "#241c1a", "#382c28", "#4c3c36", "#625048", "#7a665c"),
                  [(5, 7, HEX_RED[4]), (10, 8, HEX_RED[3]), (8, 6, HEX_RED[5])]), "item", "sacrificial_ash")
    save(compass(), "item", "seal_compass")
    gui_sheet()


def preview(out_dir):
    import preview3d
    os.makedirs(out_dir, exist_ok=True)
    for rig_fn, anims_fn, names, hidden_fn in (
            (witness_rig, witness_anims, [0, 1, 2, 3], lambda c: ()),
            (lambda n: reaper_rig(), reaper_anims, [0], lambda c: ()),
            (rebel_rig, rebel_anims, [0, 1, 2], lambda c: () if c == "death" else ("wings",))):
        _, clips = anims_fn()
        for n in names:
            m = rig_fn(n)
            t, g = m.build(gutter=1, seed=40000)
            if rig_fn is witness_rig:
                t, g = ghostify(m, t)
            preview3d.render(m.rig, t, os.path.join(out_dir, f"{m.rig.identifier}_{n}_rest.png"), hidden=hidden_fn("idle"), scale=4)
            for cl in clips:
                nm = cl.anim.name.split(".")[-1]
                tm = {"grasp": 0.4, "wail": 0.5, "touch": 0.5, "slash": 0.3, "smite": 0.7, "death": 2.0, "fade": 1.0,
                      "flicker": 0.25}.get(nm, 0.25)
                if n == names[0]:
                    preview3d.render(m.rig, t, os.path.join(out_dir, f"{m.rig.identifier}_{nm}.png"), pose=cl.pose_at(tm),
                                     hidden=hidden_fn(nm), scale=3, views=("front", "three_quarter"))


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "preview":
        preview(sys.argv[2])
    else:
        generate()
