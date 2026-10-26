"""The Chain of Command's rank and file, Heaven's side (v0.26): five angels in vessels, built like the wild angel
(angel_art: michael_art.skeleton, 256x512 at density 2, shadow wings from allegiance_art.build_wings under `wings`, the
light locators eye_l / eye_r / mouth on the face). Weapons and shields are cubes of the model.

  angel_templar      a soldier of the Host: a long dark grey military greatcoat over a white shirt, a straight angelic
                     sword in the right hand, a tall pale-silver tower shield with the Host's sigil on the left forearm
                     (bone `shield`).
  angel_herald       a lean messenger: a pale suit, a long cream scarf, white gloves, golden light in the eyes (locator
                     `lance_hand` at the right hand, where the lance of light is thrown from).
  angel_choirmaster  an older vessel: a white choir robe, a deep blue stole, a grey beard, a small hymnal on a chain at the
                     rope belt.
  angel_dominion     a towering vessel (the whole rig scaled 1.2x): ornate white-and-gold ceremonial plate over a dark
                     suit, a great two-handed hammer of pale stone bound in gold.
  grigori            a Watcher gone rogue: a gaunt vessel in a long ragged black coat and hood, hollow cheeks, dim grey
                     eyes; its wings, burnt and broken, always show.

The contract (ids, clips, hit ticks, bones) is entity/chain/ChainMobAssets.java. Clips: idle, walk (loops), stab (the
melee blow; the dominion's is a hammer blow), teleport (a beat of the wings), smitten (held) + each one's role clips.
Each texture's `_glowmask`: the eyes (blue for the Host, gold for the herald, dim grey for the Grigori), the wings' rims
and a little of the holy metal.
"""

import math
import os
import sys

import allegiance_art as AL
import angel_art as AA
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import (ANIM, E_IN, E_IO, E_OUT, E_SNAP, GEO, GOLD, LIGHT, SILVER, WHITE_METAL, Clip, Gilt, P_, Plate, build_blade,
                         fabric, glyph_bit, skeleton, tone)
from pixelkit import Ramp, fbm, hexc, mix, shade

CLEAR = (0, 0, 0, 0)
E_BACK = "easeOutBack"

# --- palettes -------------------------------------------------------------------------------------------------
SK_TAN = Ramp("#4a3022", "#6e4a36", "#8e644a", "#a87b5d", "#bd9172", "#d0a888")
SK_RUDDY = Ramp("#6a4436", "#966253", "#b87e6a", "#cc957f", "#dcab95", "#e9c2ad")
SK_GAUNT = Ramp("#4a4446", "#686063", "#827a7c", "#968e8f", "#a8a1a1", "#bab4b3")
HR_BLOND = Ramp("#4a3818", "#6e5524", "#93732f", "#b3913f", "#ccab56", "#e0c470")
HR_SILVER = Ramp("#3a3836", "#5a5755", "#7d7a77", "#9e9b97", "#bcb9b5", "#d6d4d0")

GREATCOAT = Ramp("#15171a", "#1f2226", "#2a2e33", "#363b41", "#444a51", "#545b63")
BRASS = Ramp("#3a2a0e", "#5e4516", "#86661f", "#ab872e", "#cba845", "#e4c766")
BOOT = Ramp("#050404", "#0b0909", "#131011", "#1c1819", "#2a2526", "#3c3637")
PALE_SUIT = Ramp("#6f6c64", "#8d8a81", "#a9a69c", "#c2bfb5", "#d6d3ca", "#e8e6de")
SCARF = Ramp("#8a7a58", "#a8987a", "#c4b594", "#d8cbac", "#e7dcc2", "#f3ecd8")
GLOVE = Ramp("#8f8f8a", "#b0b0ab", "#cacac5", "#dddcd8", "#ebeae7", "#f8f8f6")
ROBE_W = Ramp("#7f8590", "#a2a8b2", "#c0c5cd", "#d7dbe1", "#e7eaee", "#f6f7f9")
STOLE = Ramp("#070d26", "#0c1640", "#13215c", "#1c2e78", "#283e93", "#3a52ad")
ROPE = Ramp("#6e6250", "#8a7d68", "#a49880", "#bcb198", "#d0c6ae", "#e2d9c3")
HYMNAL = Ramp("#1a0a0a", "#2e1212", "#461b1a", "#5e2523", "#77302c", "#8f3d37")
SUIT = AA.SUIT
STONE = Ramp("#6d6a63", "#8b8880", "#a6a39b", "#bfbcb4", "#d4d1c9", "#e6e3dc")
RAG = Ramp("#040404", "#090909", "#101011", "#18181a", "#222224", "#2e2e31")
SHIRT_DK = Ramp("#1a1a1c", "#252528", "#313135", "#3e3e43", "#4c4c52", "#5c5c63")

HOST_EYES = dict(eye=hexc("#9fd6ff"), white=hexc("#e4eff8"), eye_glow=hexc("#d6f0ff"), white_glow=hexc("#5f97d8"))
GOLD_EYES = dict(eye=hexc("#ffd970"), white=hexc("#fff3d2"), eye_glow=hexc("#fff0b0"), white_glow=hexc("#c99a2e"))
GRIGORI_EYES = dict(eye=hexc("#8e9298"), white=hexc("#aaa59c"), eye_glow=hexc("#55585e"), white_glow=None)


# --- the head ----------------------------------------------------------------------------------------------------

def face_rows(v):
    rows = [list(r) for r in AA.face_rows(v)]
    if v.get("hollow"):
        # Hollow cheeks: a shadow under each cheekbone, the jaw drawn.
        for y, xs in ((10, (1, 2, 13, 14)), (11, (2, 3, 12, 13)), (12, (2, 13))):
            for x in xs:
                rows[y][x] = "q"
    return ["".join(r) for r in rows]


def head_parts(m, v):
    """angel_art.head_parts with the eyes' colours, hollow cheeks and a hood (the hair hidden) as options."""
    sk, hr = v["skin"], v["hair"]
    seed = v["seed"]
    skin = H.skin_mat(sk, seed + 1, 3.0, 0.4)
    hair = lambda f, x, y, w, h: tone(hr, 2.7 + 1.1 * (fbm(x * 2 + len(f) * 7, y, seed + 9, 64, 64, 2, 8.0) - 0.5) * 2
                                      + 0.45 * math.sin(x * 1.9 + seed) - 0.6 * (y / max(1, h)))
    ey = v["eyes"]
    eye, white = ey["eye"], ey["white"]
    beard_r = hr if v.get("beard") == "full" else AA.HR_GREY
    beard = lambda f, x, y, w, h: tone(beard_r, 2.2 + 1.2 * math.sin(x * 2.1 + y * 1.3))
    stub = lambda f, x, y, w, h: mix(skin(f, x, y, w, h), AA.HR_GREY[3] if (x * 3 + y) % 3 else AA.HR_GREY[1], 0.55)
    legend = {"h": hair, "s": skin, "b": hr[1], "w": white, "E": eye, "l": hr[0], "u": sk[2], "U": mix(sk[1], hexc("#4a3a4a"), 0.3),
              "n": sk[4], "N": sk[1], "m": mix(sk[1], hexc("#7a3b38"), 0.4), "L": mix(sk[2], hexc("#a0444a"), 0.55),
              "B": beard, "k": stub, "f": mix(sk[2], hexc("#8a4a2a"), 0.5), "d": mix(sk[2], AA.GRIME, 0.5), "c": hexc("#6a1f1c"),
              "j": sk[2], "q": mix(sk[1], hexc("#2a2228"), 0.35)}

    def eye_glow(f, x, y, w, h, c):
        if f != "north":
            return None
        if c == eye:
            return ey["eye_glow"]
        if c == white:
            return ey["white_glow"]
        return None
    side_beard = beard if v.get("beard") == "full" else (stub if v.get("beard") == "grey" else None)
    H.head_cube(m, face_rows(v), legend, H.head_sides(skin, hair, top=4, back=12, temple=7, sideburn=8, beard=side_beard,
                                                      beard_from=11), glow=eye_glow)
    m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), lambda f, x, y, w, h: sk[4] if f == "north" else sk[2], density=2, tag="nose")
    if v.get("hood"):
        return
    style = v["style"]

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "up" or f == "south":
            return hair(f, x, y, w, h)
        if f == "north":
            if style in ("crop", "cropgrey"):
                return hair(f, x, y, w, h) if y < 1 else None
            if style == "receding":
                return None
            if style == "slick":
                return hair(f, x, y, w, h) if y < 2 + (x < 6) else None
            return hair(f, x, y, w, h) if y < 2 + (x > 9) else None
        fx = x if f == "west" else w - 1 - x
        lim = {"crop": 3, "cropgrey": 3, "short": 4, "slick": 4, "receding": 4}[style]
        if style == "receding" and fx < 5 and y < 4:
            return None
        if y < 2 or (fx > 6 and y < lim + 2) or (fx > 3 and y < lim):
            return hair(f, x, y, w, h)
        return None
    crop = style in ("crop", "cropgrey", "receding")
    m.cube("head", (-4, 28, -4), (8, 4, 8), shell, density=2, inflate=0.12 if crop else 0.3, tag="hair")


# --- cloth -------------------------------------------------------------------------------------------------------

def greatcoat_front(mat, shirt, seed):
    """A buttoned, double-breasted greatcoat: a narrow V at the throat showing the shirt's collar, two rows of brass
    buttons, a dark belt with a silver buckle."""
    def m(f, x, y, w, h):
        if f == "down":
            return None
        if f == "up":
            return mat(f, x, y, w, h)
        belt = h - 6 <= y <= h - 5
        if belt:
            if f == "north" and abs(x - (w - 1) / 2) < 1.6:
                return SILVER[4] if y == h - 6 else SILVER[2]
            return BOOT[3] if (x + y) % 5 else BOOT[4]
        if f != "north":
            return mat(f, x, y, w, h)
        mid = (w - 1) / 2
        d = abs(x - mid)
        if y < 5 and d <= 2.4 - y * 0.45:
            return shirt(f, x, y, w, h)
        if y < 6 and d <= 3.4 - y * 0.45:
            return shade(mat(f, x, y, w, h), 1.25)    # the lapels
        if abs(d - 3.0) < 0.6 and y in (7, 11, 15):
            return BRASS[4] if x < mid else BRASS[3]
        if abs(x - (mid + 1.5)) < 0.5 and y > 5:
            return shade(mat(f, x, y, w, h), 0.75)    # the overlap of the front
        return mat(f, x, y, w, h)
    return m


def robe_front(robe, stole):
    """A choir robe with a stole round the neck: two blue bands down the chest, gold-fringed at the ends."""
    def m(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            mid = (w - 1) / 2
            if y < 3 and abs(x - mid) < 2.5 - y * 0.6:
                return None                           # the neckline
            if 2.0 <= abs(x - mid) <= 4.0:
                return tone(STOLE, 3.0 + (0.6 if abs(x - mid) < 2.6 else -0.3) - 0.4 * y / h)
        if f in ("east", "west") and y < 3:
            return tone(STOLE, 2.8)                   # over the shoulders
        if f == "south" and y < 3 and abs(x - (w - 1) / 2) < 5:
            return tone(STOLE, 2.6)
        return robe(f, x, y, w, h)
    return m


def ragged(mat, seed, holes=0.14):
    return AA.worn(mat, seed, dirt=0.3, holes=holes, under=None, fray=True)


# --- shared body -------------------------------------------------------------------------------------------------

def vessel(m, v, o, tails=(3.0, 9.0), arm_w=4.0, wings="shadow"):
    """The angel's body (angel_art.rig): neck, torso, jacket, coat, belt, arms, legs, coat tails, locators, wings."""
    s = v["seed"]
    skin = H.skin_mat(v["skin"], s + 3, 3.0, 0.4)
    m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, density=2, tag="neck")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o["torso"], o.get("torso_glow"), density=2, tag="torso")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o.get("jacket") or AA.clear, density=2, inflate=0.3, tag="jacket")
    m.cube("body", (-4, 12, -2), (8, 12, 4), o.get("coat") or AA.clear, density=2, inflate=0.6, tag="coat")
    belt = o.get("belt")
    if belt:
        m.cube("body", (-4, 11.6, -2), (8, 1.2, 4), belt, inflate=0.38 + (0.3 if o.get("coat") else 0), density=2, tag="belt")
    hand = o.get("hand") or H.skin_mat(v["skin"], s + 4, 3.1, 0.35)
    shoe = o["shoe"]
    for side in ("right", "left"):
        H.arm_cubes(m, side, o["sleeve"], hand, w=arm_w, sleeve_inflate=o.get("sleeve_inflate", 0.25))
        H.leg_cubes(m, side, o["trousers"], lambda f, x, y, w, h, sh=shoe: sh[4] if f == "up" and y < 3 else sh[2],
                    shoe_h=o.get("shoe_h", 2.0))
    y0, ln = tails
    for side, sg in (("r", -1), ("l", 1)):
        b = m.bone(f"coat_tail_{side}", (2 * sg, 12, 0), "body")
        tl = o.get("tails")
        closed = o.get("closed_tails")

        def tail(f, x, y, w, h, sg=sg, tl=tl):
            if tl is None or f in ("up", "down"):
                return None
            if not closed and f == "north" and ((sg < 0 and x >= w - 2) or (sg > 0 and x <= 1)):
                return None
            return tl(f, x, y, w, h)
        m.cube(b, (2 * sg - 2.2, y0, -2.4), (4.4, ln, 4.8), tail, o.get("tails_glow"), density=2, tag="coat_tail")
    m.bone("eye_l", (AA.EYE_X, AA.EYE_Y, AA.FACE_Z), "head")
    m.bone("eye_r", (-AA.EYE_X, AA.EYE_Y, AA.FACE_Z), "head")
    m.bone("mouth", (0.0, AA.MOUTH_Y, AA.FACE_Z), "head")
    wb = m.bone("wings", (0, 20, 3.0), "body")
    AL.build_wings(m, wb.name, wings, off=(0.0, 19.8, 0.8), glow_rims=wings != "burnt")


def host_sigil(f, x, y, w, h):
    """The Host's sigil: a ring, a cross, four points between its arms, a star at the heart."""
    cx, cy = (w - 1) / 2, (h - 1) / 2
    r = math.hypot(x - cx, (y - cy) * 1.0)
    R = min(w, h) / 2 - 1.0
    if abs(r - R) < 0.6:
        return GOLD[4]
    if (abs(x - cx) < 0.6 or abs(y - cy) < 0.6) and r < R - 1:
        return LIGHT[4]
    if abs(abs(x - cx) - R * 0.5) < 0.6 and abs(abs(y - cy) - R * 0.5) < 0.6:
        return GOLD[5]
    return None


# =================================================================================================================
# The five
# =================================================================================================================

def seed_of(name):
    return 9400 + sum(ord(c) for c in name) * 11


def templar():
    v = dict(skin=SK_TAN, hair=AA.HR_BLACK, style="crop", fallen=False, eyes=HOST_EYES, seed=seed_of("angel_templar"))
    m = Model("angel_templar", 256, 512)
    skeleton(m)
    head_parts(m, v)
    s = v["seed"]
    coat = fabric(GREATCOAT, s + 10, 3.0, 0.3)
    shirt = fabric(AA.SHIRT, s + 11, 3.6, 0.15)
    o = dict(torso=AA.shirt_front(shirt), coat=greatcoat_front(coat, shirt, s), tails=coat, closed_tails=True, sleeve=coat,
             trousers=fabric(GREATCOAT, s + 12, 2.4, 0.15, crease=True), shoe=BOOT, shoe_h=3.0)
    vessel(m, v, o, tails=(2.0, 10.0))
    # Epaulettes on the greatcoat's shoulders.
    for side, sg in (("right", -1), ("left", 1)):
        m.cube(f"{side}_arm", (6 * sg - 2.4, 23.4, -2.4), (4.8, 0.8, 4.8), lambda f, x, y, w, h: GREATCOAT[4] if f == "up" else
               (BRASS[3] if y == 0 else GREATCOAT[2]), density=2, tag="epaulette")
    sword(m)
    shield(m)
    return m


def sword(m, x=-6.0, y=12.25):
    """A straight angelic sword: a dark wrapped grip, a gold cross-guard, a long silver blade with a fuller of script."""
    b = m.bone("sword", (x, y, 0), "right_hand", rotation=(62, 0, 0))
    m.cube(b, (x - 0.5, y - 0.5, -0.5), (1, 1, 3.5), lambda f, xx, yy, w, h: AA.LEATHER[1 + ((xx + yy) % 3 == 0) * 2], density=2,
           tag="grip")
    m.cube(b, (x - 0.75, y - 0.75, 3.0), (1.5, 1.5, 1.0), Gilt(9501, 2).mat, Gilt(9501, 2).glow, density=2, tag="pommel")
    m.cube(b, (x - 2.5, y - 0.5, -1.5), (5, 1, 1), Gilt(9502, 2).mat, Gilt(9502, 2).glow, density=2, tag="guard")

    def steel(f, xx, yy, w, h):
        if f in ("east", "west"):
            mid = (h - 1) / 2
            if abs(yy - mid) < 0.6 and 2 < xx < w - 5 and xx % 3 != 0:
                return LIGHT[3]                         # the script in the fuller
            return SILVER[5] if yy < mid else SILVER[3]
        return SILVER[4] if f in ("up", "north") else SILVER[2]

    def steel_glow(f, xx, yy, w, h, c):
        return shade(c, 0.9) if c == LIGHT[3] else shade(c, 0.12)
    m.cube(b, (x - 0.4, y - 0.9, -14.0), (0.8, 1.8, 12.5), steel, steel_glow, density=2, tag="blade")
    m.cube(b, (x - 0.3, y - 0.5, -15.0), (0.6, 1.0, 1.0), solid(SILVER[5]), H.glow_faint(0.2), density=2, tag="tip")


def shield(m):
    sh = m.bone("shield", (8.3, 15, 0), "left_forearm", rotation=(0, 35, 0))
    ramp = SILVER

    def face(f, x, y, w, h):
        if f != "east":
            return SILVER[2] if f in ("up", "down") else tone(ramp, 2.2)
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return GOLD[5] if y < h / 2 else GOLD[2]
        if e == 1:
            return GOLD[3]
        # Script along the top and bottom bands.
        for r0 in (3, h - 8):
            if 0 <= y - r0 < 5 and 3 <= x < w - 3 and (x - 3) % 4 < 3 and glyph_bit(9600 + r0, (x - 3) // 4, 0, (x - 3) % 4, y - r0):
                return GOLD[2]
        # The Host's sigil in the middle.
        cy = h * 0.46
        sx, sy = x - 2, y - (cy - (w - 4) / 2)
        if 0 <= sx < w - 4 and 0 <= sy < w - 4:
            c = host_sigil(f, sx, sy, w - 4, w - 4)
            if c is not None:
                return c
        # A pale cross the length of the shield behind the sigil.
        if abs(x - (w - 1) / 2) < 0.6 and 9 < y < h - 9:
            return SILVER[5]
        return tone(ramp, 4.4 - 1.6 * y / max(1, h) + 0.3 * math.sin(x * 0.7))

    def glow(f, x, y, w, h, c):
        if c in (LIGHT[4], GOLD[5]):
            return shade(c, 0.85)
        return shade(c, 0.35) if c in (GOLD[4], SILVER[5]) else shade(c, 0.08)
    m.cube(sh, (8.2, 3.5, -5), (1.2, 18.5, 10), face, glow, density=2, tag="shield")
    boss = Gilt(9610, 2)
    m.cube(sh, (7.6, 13.5, -1), (0.6, 2, 2), boss.mat, boss.glow, density=2, tag="shield_grip")


def herald():
    v = dict(skin=AA.SK_FAIR, hair=HR_BLOND, style="slick", fallen=False, eyes=GOLD_EYES, seed=seed_of("angel_herald"))
    m = Model("angel_herald", 256, 512)
    skeleton(m)
    head_parts(m, v)
    s = v["seed"]
    suit = fabric(PALE_SUIT, s + 10, 3.2, 0.25)
    shirt = fabric(AA.SHIRT, s + 11, 3.8, 0.15)
    glove = H.skin_mat(GLOVE, s + 12, 3.2, 0.3)
    o = dict(torso=AA.shirt_front(shirt, buttons=True), jacket=AA.open_front(suit, v_depth=12, buttons=PALE_SUIT[1]), sleeve=suit,
             trousers=fabric(PALE_SUIT, s + 13, 2.9, 0.15, crease=True), shoe=Ramp("#2a1c12", "#3b281a", "#4f3624", "#64452f",
                                                                               "#79553a", "#8f6747"),
             hand=glove, belt=lambda f, x, y, w, h: hexc("#c9b98a") if f == "north" and 6 <= x <= 9 else hexc("#3b281a"))
    vessel(m, v, o, arm_w=3.6)
    scarf = lambda f, x, y, w, h: tone(SCARF, 3.2 + 0.5 * math.sin(x * 1.3 + y * 0.4) - (0.6 if f in ("south", "down") else 0)
                                       - (0.8 if (y % 6 == 5 and f in ("north", "south")) else 0))
    # Wound twice round the neck...
    m.cube("body", (-2.6, 22.6, -2.6), (5.2, 1.8, 5.2), none_on(("down",), scarf), H.glow_faint(0.08), density=2, inflate=0.2,
           tag="scarf")
    # ...one end down the front (left of the chest), the long end down the back, each on its own bone to flutter.
    fb = m.bone("scarf_front", (1.6, 23, -2.9), "body")
    m.cube(fb, (0.6, 12.5, -3.4), (2.2, 10.5, 0.6), lambda f, x, y, w, h: (SCARF[2] if y >= h - 2 and x % 2 else scarf(f, x, y, w, h)),
           H.glow_faint(0.08), density=2, tag="scarf_end")
    bb = m.bone("scarf_back", (-1.2, 23, 2.6), "body")
    m.cube(bb, (-2.4, 9.0, 2.4), (2.4, 14, 0.6), lambda f, x, y, w, h: (SCARF[2] if y >= h - 2 and x % 2 else scarf(f, x, y, w, h)),
           H.glow_faint(0.08), density=2, tag="scarf_end", rotation=(3, 0, 0), pivot=(-1.2, 23, 2.6))
    build_blade(m, "right_hand")
    m.bone("lance_hand", AA.ITEM_PIVOT, "right_hand")
    return m


def choirmaster():
    v = dict(skin=SK_RUDDY, hair=AA.HR_GREY, style="receding", fallen=False, beard="full", eyes=HOST_EYES,
             seed=seed_of("angel_choirmaster"))
    m = Model("angel_choirmaster", 256, 512)
    skeleton(m)
    head_parts(m, v)
    s = v["seed"]
    robe = fabric(ROBE_W, s + 10, 3.4, 0.4)

    def skirt(f, x, y, w, h):
        if f == "north" and (x <= 1 or x >= w - 2):
            return tone(STOLE, 3.0) if y < h - 2 else GOLD[3 + (x % 2)]
        return robe(f, x, y, w, h)
    rope = lambda f, x, y, w, h: ROPE[2 + ((x + y) % 3 == 0)]
    o = dict(torso=fabric(ROBE_W, s + 11, 3.2, 0.3), coat=robe_front(robe, STOLE), tails=skirt, closed_tails=True,
             sleeve=fabric(ROBE_W, s + 12, 3.3, 0.45), sleeve_inflate=0.45, trousers=fabric(AA.SLACKS, s + 13, 2.4, 0.15),
             shoe=BOOT, belt=rope)
    vessel(m, v, o, tails=(0.6, 11.4))
    # The stole's ends on the robe's skirt (the tails' inner front edges, painted above), the hymnal on its chain.
    hb = m.bone("hymnal", (3.2, 11.5, -2.4), "body")
    m.cube(hb, (3.0, 10.4, -3.0), (0.4, 1.4, 0.4), lambda f, x, y, w, h: GOLD[3 + (y % 2)], density=2, tag="hymnal_chain")
    m.cube(hb, (2.4, 7.6, -3.6), (2.0, 2.8, 1.0), lambda f, x, y, w, h: (GOLD[4] if f == "north" and (x, y) in ((1, 2), (2, 2), (1, 3), (2, 3))
                                                                           else (ROBE_W[5] if f == "east" else HYMNAL[2 + (f == "north")])),
           density=2, tag="hymnal")
    build_blade(m, "right_hand")
    return m


DOMINION_SCALE = 1.2


def dominion():
    v = dict(skin=AA.SK_DARK, hair=AA.HR_BLACK, style="crop", fallen=False, beard="full", eyes=HOST_EYES, seed=seed_of("angel_dominion"))
    m = Model("angel_dominion", 256, 512)
    skeleton(m)
    head_parts(m, v)
    s = v["seed"]
    suit = fabric(SUIT, s + 10, 3.0, 0.3)
    o = dict(torso=AA.shirt_front(fabric(AA.SHIRT, s + 11, 3.5, 0.15), AA.NAVY), jacket=AA.open_front(suit, v_depth=13, buttons=SUIT[4]),
             sleeve=suit, trousers=fabric(SUIT, s + 12, 2.7, 0.15, crease=True), shoe=BOOT)
    vessel(m, v, o)
    armour(m)
    hammer(m)
    return m


def armour(m):
    plate = lambda seed, motif="plain", eng=False, sc=False: Plate(seed, 2, ramp=WHITE_METAL, trim=GOLD, engrave=eng, rivets=False,
                                                                   base=3.0, motif=motif, scallop=sc, glow=0.1)
    bp = plate(9701, "frame")
    m.cube("body", (-4.6, 14.6, -3.0), (9.2, 8.6, 1.0), bp.mat, bp.glow, density=2, tag="breastplate")
    m.cube("body", (-1.5, 16.6, -3.25), (3, 3, 0.25), host_sigil, lambda f, x, y, w, h, c: c, density=4, faces=("north",), tag="sigil")
    back = plate(9702)
    m.cube("body", (-4.6, 14.6, 2.0), (9.2, 8.6, 0.8), back.mat, back.glow, density=2, tag="backplate")
    g = Gilt(9703, 2)
    m.cube("body", (-2.6, 22.6, -2.6), (5.2, 1.6, 5.2), g.mat, g.glow, density=2, tag="gorget")
    for i in range(2):
        fl = plate(9704 + i)
        m.cube("body", (-4.6, 12.0 + i * 1.4 - 1.0, -2.6), (9.2, 1.6, 5.2), fl.mat, fl.glow, density=2, tag="fauld")
    for side, sg in (("right", -1), ("left", 1)):
        pd = plate(9710 + (sg > 0), "rosette", sc=True)
        m.cube(f"{side}_arm", (6 * sg - 2.8, 20.6, -2.9), (5.6, 3.6, 5.8), pd.mat, pd.glow, density=2, tag="pauldron",
               rotation=(0, 0, -12 * sg), pivot=(6 * sg, 22, 0))
        vb = plate(9712 + (sg > 0))
        m.cube(f"{side}_forearm", (6 * sg - 2.3, 13.8, -2.3), (4.6, 3.8, 4.6), vb.mat, vb.glow, density=2, tag="vambrace")
        gr = plate(9714 + (sg > 0))
        m.cube(f"{side}_shin", (2 * sg - 2.35, 2.2, -2.5), (4.7, 3.8, 1.0), gr.mat, gr.glow, density=2, tag="greave")
        kc = plate(9716 + (sg > 0), "rosette")
        m.cube(f"{side}_shin", (2 * sg - 1.6, 5.2, -2.6), (3.2, 2.0, 0.8), kc.mat, kc.glow, density=2, tag="knee")
        tb = m.rig.get(f"coat_tail_{'r' if sg < 0 else 'l'}")
        ts = plate(9718 + (sg > 0))
        m.cube(tb, (2 * sg - 2.3, 7.0, -2.8), (4.6, 4.2, 0.8), ts.mat, ts.glow, density=2, tag="tasset")


def hammer(m, x=-6.0, y=12.25):
    """A great hammer of pale stone bound in gold, held by the middle of its haft, the head hanging by the right foot."""
    b = m.bone("hammer", (x, y, 0), "right_hand")
    wood = lambda f, xx, yy, w, h: tone(AA.LEATHER if 7 < yy < 13 else Ramp("#2b1c10", "#3f2a18", "#563b22", "#6c4c2e", "#82603b",
                                                                              "#987449"), 2.6 + 0.6 * math.sin(yy * 0.5))
    m.cube(b, (x - 0.6, 4.6, -0.6), (1.2, 15.0, 1.2), wood, density=2, tag="haft")
    g = Gilt(9720, 2)
    m.cube(b, (x - 0.9, 19.4, -0.9), (1.8, 1.2, 1.8), g.mat, g.glow, density=2, tag="pommel")
    m.cube(b, (x - 0.85, 4.4, -0.85), (1.7, 0.8, 1.7), g.mat, g.glow, density=2, tag="collar")

    def stone(f, xx, yy, w, h):
        n = fbm(xx * 2 + len(f) * 5, yy * 2, 9721, 64, 64, 3, 6.0)
        c = tone(STONE, 3.1 + (n - 0.5) * 1.6 - 0.5 * yy / max(1, h))
        if f in ("north", "south") and abs(xx - (w - 1) / 2) < 0.6:
            return LIGHT[3] if (yy % 3) else GOLD[4]   # a line of script down each face
        if fbm(xx * 4, yy * 4 + len(f), 9722, 64, 64, 2, 9.0) > 0.78:
            c = shade(c, 0.8)
        return c

    def stone_glow(f, xx, yy, w, h, c):
        return shade(c, 0.8) if c == LIGHT[3] else None
    m.cube(b, (x - 2.1, 0.3, -4.2), (4.2, 4.4, 8.4), stone, stone_glow, density=2, tag="hammer_head")
    for z in (-3.6, 2.6):
        m.cube(b, (x - 2.3, 0.1, z), (4.6, 4.8, 1.0), g.mat, g.glow, density=2, tag="hammer_band")


def grigori():
    v = dict(skin=SK_GAUNT, hair=AA.HR_RAVEN, style="long", fallen=True, hollow=True, hood=True, eyes=GRIGORI_EYES,
             seed=seed_of("grigori"))
    m = Model("grigori", 256, 512)
    skeleton(m)
    head_parts(m, v)
    s = v["seed"]
    skin = H.skin_mat(SK_GAUNT, s + 3, 3.0, 0.4)
    coat = fabric(RAG, s + 10, 2.9, 0.4)
    o = dict(torso=AA.worn(fabric(SHIRT_DK, s + 11, 2.8, 0.3), s + 12, 0.4, 0.12, skin),
             coat=ragged(AA.open_front(coat, half=2.4, collar=5), s + 13, 0.16),
             tails=ragged(coat, s + 14, 0.22), sleeve=ragged(coat, s + 15, 0.12),
             trousers=AA.worn(fabric(RAG, s + 16, 2.6, 0.2), s + 17, 0.3, 0.1, skin), shoe=AA.BOOT,
             hand=H.skin_mat(SK_GAUNT, s + 4, 2.8, 0.35))
    vessel(m, v, o, tails=(0.8, 11.2), arm_w=3.4, wings="burnt")
    hood = AA.worn(fabric(RAG, s + 20, 3.0, 0.45), s + 21, 0.3, 0.0, None, fray=False)

    def hood_m(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            if 2 <= x <= w - 3 and y >= 3:
                return None                            # the face opening
            if y >= 3 or y == 2 and 2 <= x <= w - 3:
                return shade(hood(f, x, y, w, h), 0.7)  # its shadowed rim
        if f in ("east", "west", "south") and y >= h - 2 and h01("hood", s, f, x) < 0.5:
            return None                                 # a ragged hem on the shoulders
        return hood(f, x, y, w, h)
    m.cube("head", (-4.5, 23.4, -4.6), (9, 9.4, 9), hood_m, density=2, inflate=0.2, tag="hood")
    build_blade(m, "right_hand")
    return m


def scaled(m, k):
    """Scales a built rig in place (pivots, cubes, inflation): its texels grow with it."""
    for b in m.rig.bones:
        b.pivot = tuple(round(v * k, 4) for v in b.pivot)
        for c in b.cubes:
            c.origin = tuple(round(v * k, 4) for v in c.origin)
            c.size = tuple(round(v * k, 4) for v in c.size)
            c.inflate = round(c.inflate * k, 4)
            if c.pivot:
                c.pivot = tuple(round(v * k, 4) for v in c.pivot)
            c.density = c.density / k


MOBS = {"angel_templar": templar, "angel_herald": herald, "angel_choirmaster": choirmaster, "angel_dominion": dominion,
        "grigori": grigori}


def build(name):
    m = MOBS[name]()
    t, g = m.build(gutter=1, seed=seed_of(name))
    if name == "angel_dominion":
        scaled(m, DOMINION_SCALE)
    return m, t, g


# =================================================================================================================
# animations
# =================================================================================================================

wings = AA.wings
FOLDED, DROOP, SPREAD, WIDE, HIGH, DOWN, UNFURL = AA.FOLDED, AA.DROOP, AA.SPREAD, AA.WIDE, AA.HIGH, AA.DOWN, AA.UNFURL


class SClip(Clip):
    """A clip whose positions (the "@" channels) are scaled with the rig."""

    def __init__(self, f, name, length, loop=False, hold=False, k=1.0):
        super().__init__(f, name, length, loop, hold)
        self.k = k

    def key(self, t, pose, ease=None):
        if self.k != 1.0:
            pose = {n: ([c * self.k for c in v] if n.startswith("@") else v) for n, v in pose.items()}
        return super().key(t, pose, ease)


# Per mob: the rest of the wings, the arms' carriage at rest, the stab's hit (s) and the kind of blow.
STYLE = {
    "angel_templar": dict(rest=FOLDED, hit=0.3, carry={"right_arm": [-6, 0, 4], "right_forearm": [-20, 0, 0], "left_arm": [-10, 0, -4],
                                                       "left_forearm": [-30, 0, 0]}),
    "angel_herald": dict(rest=FOLDED, hit=0.3, carry={"right_arm": [-2, 0, 3], "right_forearm": [-10, 0, 0], "left_arm": [0, 0, -3],
                                                      "left_forearm": [-8, 0, 0]}),
    "angel_choirmaster": dict(rest=FOLDED, hit=0.3, carry={"right_arm": [-8, 0, 3], "right_forearm": [-30, 0, 0], "left_arm": [-8, 0, -3],
                                                           "left_forearm": [-30, 0, 0]}),
    "angel_dominion": dict(rest=FOLDED, hit=0.35, carry={"right_arm": [-4, 0, 6], "right_forearm": [-6, 0, 0], "left_arm": [0, 0, -4],
                                                         "left_forearm": [-8, 0, 0]}),
    "grigori": dict(rest=DROOP, hit=0.25, carry={"right_arm": [-4, 0, 3], "right_forearm": [-24, 0, 0], "left_arm": [-6, 0, -3],
                                                 "left_forearm": [-30, 0, 0], "head": [10, 0, 0], "body": [6, 0, 0]}),
}


def anims(name):
    st = STYLE[name]
    k = DOMINION_SCALE if name == "angel_dominion" else 1.0
    A = f"animation.{name}."
    f = AnimFile()
    clips = []
    rest, carry = st["rest"], st["carry"]

    def clip(cn, length, loop=False, hold=False):
        clips.append(SClip(f, A + cn, length, loop, hold, k))
        return clips[-1]

    def add(base, extra):
        out = dict(base)
        for n, v in extra.items():
            out[n] = [out[n][i] + v[i] for i in range(3)] if n in out else list(v)
        return out

    # idle: a slow breath, the head tilting to study you.
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        kk = math.sin(math.pi * t / 2)
        c.key(t, P_(rest, add(carry, {"@body": [0, -0.15 * abs(kk), 0], "head": [-2 * abs(kk), [0, 6, 0, -8, 0][i], [0, 7, 0, -3, 0][i]],
                                      "right_forearm": [-3 * abs(kk), 0, 0], "left_forearm": [-3 * abs(kk), 0, 0],
                                      "coat_tail_r": [1.5 * abs(kk), 0, 1], "coat_tail_l": [1.5 * abs(kk), 0, -1]})), E_IO if i else None)
    if name == "angel_herald":
        c.layer("scarf_back", lambda t: [3 + 3 * math.sin(t * math.pi), 0, 2 * math.sin(t * math.pi * 0.5)])
    # walk: an unhurried, purposeful stride (the grigori's a stalking lope, head low).
    wl = 1.1 if name == "angel_dominion" else 1.0
    c = clip("walk", wl, loop=True)
    for i in range(5):
        t = i / 4 * wl
        ph = i / 4
        sn = math.sin(2 * math.pi * ph)
        arms = 0.4 if name in ("angel_templar", "angel_dominion") else 1.0
        c.key(t, P_(rest, add(carry, {"right_leg": [-28 * sn, 0, 0], "left_leg": [28 * sn, 0, 0],
                                      "right_shin": [34 * max(0, math.sin(2 * math.pi * ph + 1.2)), 0, 0],
                                      "left_shin": [34 * max(0, -math.sin(2 * math.pi * ph + 1.2)), 0, 0],
                                      "right_arm": [18 * sn * arms, 0, 0], "left_arm": [-18 * sn * arms if name != "angel_templar" else 0, 0, 0],
                                      "body": [2, 4 * sn, 0], "head": [-1, -4 * sn, 0],
                                      "@root": [0, 0.5 * abs(math.cos(2 * math.pi * ph)), 0], "coat_tail_r": [12 * max(0, -sn) + 3, 0, 2],
                                      "coat_tail_l": [12 * max(0, sn) + 3, 0, -2]})), E_IO if i else None)
    if name == "angel_herald":
        c.layer("scarf_back", lambda t: [16 + 4 * math.sin(t * math.pi * 4), 0, 3 * math.sin(t * math.pi * 2)])
        c.layer("scarf_front", lambda t: [-6 - 3 * math.sin(t * math.pi * 4), 0, 0])

    # stab: the wings' shadow thrown open, the blow driven down and in.
    hit = st["hit"]
    c = clip("stab", round(hit * 2, 2))
    if name == "angel_dominion":
        up = {"right_arm": [-170, 0, 6], "right_forearm": [-10, 0, 0], "left_arm": [-165, 0, -18], "left_forearm": [-30, 0, 0],
              "body": [-10, 6, 0], "head": [-6, 0, 0], "@root": [0, 0.3, 0.6], "right_leg": [6, 0, 0], "left_leg": [-10, 0, 0]}
        blow = {"right_arm": [-62, 0, 0], "right_forearm": [-4, 0, 0], "left_arm": [-58, -20, -4], "left_forearm": [-20, 0, 0],
                "body": [22, -4, 0], "head": [-10, 4, 0], "@root": [0, -1.0, -2.0], "right_leg": [10, 0, 0], "left_leg": [-28, 0, 0],
                "left_shin": [22, 0, 0]}
    elif name == "angel_templar":
        up = {"right_arm": [-30, 30, 30], "right_forearm": [-70, 0, 0], "body": [-4, 22, 0], "head": [-4, -16, 0],
              "left_arm": [-40, 0, -6], "left_forearm": [-60, 0, 0], "@root": [0, 0.2, 0.6], "right_leg": [10, 0, 0], "left_leg": [-8, 0, 0]}
        blow = {"right_arm": [-82, -6, 0], "right_forearm": [-4, 0, 0], "body": [12, -14, 0], "head": [4, 10, 0], "left_arm": [-30, 0, -10],
                "left_forearm": [-55, 0, 0], "@root": [0, -0.8, -2.2], "right_leg": [14, 0, 0], "left_leg": [-26, 0, 0], "left_shin": [20, 0, 0]}
        blow["sword"] = [28, 0, 0]
    else:
        up = {"right_arm": [-165, -10, 18], "right_forearm": [-38, 0, 0], "body": [-6, 16, 0], "head": [-6, -10, 0],
              "left_arm": [-30, 0, -14], "left_forearm": [-30, 0, 0], "@root": [0, 0.3, 0.5], "right_leg": [10, 0, 0], "left_leg": [-8, 0, 0]}
        blow = {"right_arm": [-62, 8, 0], "right_forearm": [-34, 0, 0], "body": [16, -10, 0], "head": [6, 8, 0], "left_arm": [-40, 0, -8],
                "left_forearm": [-50, 0, 0], "@root": [0, -0.8, -2.0], "right_leg": [14, 0, 0], "left_leg": [-26, 0, 0], "left_shin": [20, 0, 0]}
    if name == "grigori":
        blow.update({"left_arm": [-70, -10, -6], "left_forearm": [-10, 0, 0], "left_hand": [-30, 0, 0]})   # the other hand clawing
    c.key(0, rest)
    c.key(hit * 0.6, P_(UNFURL if name != "grigori" else rest, up), E_OUT)
    c.key(hit, P_(WIDE if name != "grigori" else rest, blow), E_SNAP)
    c.key(hit + hit * 0.4, P_(SPREAD if name != "grigori" else rest, blow, {"body": [blow["body"][0] - 2, blow["body"][1], 0]}), E_OUT)
    c.key(round(hit * 2, 2), rest, E_IO)

    # teleport (the beat lands at 0.4 s = 8 ticks): a crouch, the wings raised high, one beat down -- gone.
    c = clip("teleport", 0.5)
    crouch = {"@root": [0, -1.6, 0], "body": [14, 0, 0], "head": [-10, 0, 0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0],
              "right_shin": [46, 0, 0], "left_shin": [46, 0, 0], "right_arm": [10, 0, 20], "left_arm": [10, 0, -20]}
    c.key(0, rest)
    c.key(0.15, P_(UNFURL, crouch), E_OUT)
    c.key(0.28, P_(HIGH, crouch, {"@root": [0, -2.0, 0], "body": [18, 0, 0]}), E_IO)
    c.key(0.4, P_(DOWN, {"@root": [0, 0.6, 0], "body": [-4, 0, 0], "head": [-6, 0, 0], "right_arm": [0, 0, 26], "left_arm": [0, 0, -26]}),
          E_SNAP)
    c.key(0.5, P_(DOWN, {"@root": [0, 1.2, 0], "body": [-6, 0, 0], "head": [-8, 0, 0], "right_arm": [0, 0, 30], "left_arm": [0, 0, -30]}),
          E_OUT)

    # smitten (held): snapped rigid, head thrown back, arms flung out, the back arched; it holds, trembling.
    c = clip("smitten", 1.5, hold=True)
    rigid = {"head": [-38, 0, 0], "body": [-10, 0, 0], "right_arm": [-12, 0, 82], "left_arm": [-12, 0, -82],
             "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_hand": [0, 0, 10], "left_hand": [0, 0, -10],
             "right_leg": [4, 0, 5], "left_leg": [4, 0, -5], "@root": [0, 0.6, 0.6], "coat_tail_r": [8, 0, 4], "coat_tail_l": [8, 0, -4]}
    sw = WIDE if name != "grigori" else wings(dict(AA.BROKEN_R, arm=(0, 14, 50), hand=(0, 0, -30)), dict(AL.OPEN, arm=(0, 16, 50)))
    c.key(0, rest)
    c.key(0.06, P_(UNFURL if name != "grigori" else rest, {"head": [-10, 0, 0], "body": [-3, 0, 0], "right_arm": [-6, 0, 30],
                                                         "left_arm": [-6, 0, -30]}), E_OUT)
    c.key(0.2, P_(sw, rigid), E_OUT)
    c.key(0.85, P_(sw, rigid, {"head": [-42, 0, 0], "body": [-12, 0, 0], "right_arm": [-14, 0, 86], "left_arm": [-14, 0, -86],
                               "@root": [0, 0.9, 0.7]}), E_IO)
    c.key(1.5, P_(sw, rigid, {"head": [-44, 0, 0], "body": [-13, 0, 0], "right_arm": [-15, 0, 88], "left_arm": [-15, 0, -88],
                              "@root": [0, 1.0, 0.8]}), E_IO)
    trem = lambda amp, fx, fy: (lambda t: [0.0, 0.0, 0.0] if t < 0.2 else
                                [amp * math.sin(t * fx), amp * 0.6 * math.sin(t * fy), amp * 0.5 * math.sin(t * (fx + fy) * 0.5)])
    c.layer("head", trem(1.6, 97, 71))
    c.layer("body", trem(0.7, 83, 59))
    c.layer("right_arm", trem(1.2, 91, 67))
    c.layer("left_arm", trem(1.2, 79, 101))

    ROLES[name](clip, rest, carry, trem)
    for cl in clips:
        cl.done(step=0.05 if cl.anim.name.endswith(("smitten", "drain", "hymn")) else 0.2)
    return f, clips


def roles_templar(clip, rest, carry, trem):
    # block (played and held): the shield brought round in front of the face, the sword drawn back low.
    c = clip("block", 0.3, hold=True)
    guard = {"left_arm": [-90, 68, 0], "left_forearm": [-95, 0, 0], "shield": [0, -125, 0],
             "body": [4, 10, 0], "head": [6, -6, 0], "right_arm": [10, 0, 10], "right_forearm": [-30, 0, 0], "@root": [0, -0.6, 0],
             "right_leg": [12, 0, 0], "left_leg": [-14, 0, 0], "left_shin": [10, 0, 0]}
    c.key(0, P_(rest, carry))
    c.key(0.18, P_(rest, guard), E_OUT)
    c.key(0.3, P_(rest, guard, {"@root": [0, -0.8, 0]}), E_IO)
    # slash (hit 0.45 s = 9 ticks): a wide horizontal cut from the right to the left.
    c = clip("slash", 0.8)
    wind = {"right_arm": [-80, 75, 10], "right_forearm": [-20, 0, 0], "sword": [28, 0, 0], "body": [0, 34, 0], "head": [0, -26, 0],
            "left_arm": [-40, 0, -10], "left_forearm": [-55, 0, 0], "right_leg": [8, 0, 0], "left_leg": [-10, 0, 0]}
    cut = {"right_arm": [-86, -60, -6], "right_forearm": [-6, 0, 0], "sword": [28, 0, 0], "body": [6, -32, 0], "head": [2, 24, 0],
           "left_arm": [-20, 0, -14], "left_forearm": [-50, 0, 0], "@root": [0, -0.6, -1.2], "right_leg": [-18, 0, 0],
           "left_leg": [12, 0, 0]}
    c.key(0, P_(rest, carry))
    c.key(0.3, P_(UNFURL, wind), E_OUT)
    c.key(0.45, P_(WIDE, cut), E_SNAP)
    c.key(0.58, P_(SPREAD, cut, {"body": [6, -36, 0]}), E_OUT)
    c.key(0.8, P_(rest, carry), E_IO)


def roles_herald(clip, rest, carry, trem):
    # throw (released at 0.45 s = 9 ticks): wound up like a javelin thrower, the arm whipped over and forward.
    c = clip("throw", 0.8)
    wind = {"right_arm": [-150, 10, 30], "right_forearm": [50, 0, 0], "body": [-8, 30, 0], "head": [0, -26, 0],
            "left_arm": [-90, -30, -10], "left_forearm": [-10, 0, 0], "@root": [0, -0.4, 1.4], "right_leg": [22, 0, 0], "left_leg": [-20, 0, 0],
            "scarf_back": [30, 0, 0], "scarf_front": [-10, 0, 0]}
    rel = {"right_arm": [-110, -20, 0], "right_forearm": [-10, 0, 0], "body": [14, -24, 0], "head": [4, 20, 0],
           "left_arm": [10, 0, -24], "left_forearm": [-30, 0, 0], "@root": [0, -0.6, -1.6], "right_leg": [-10, 0, 0], "left_leg": [10, 0, 0],
           "scarf_back": [50, 0, -10], "scarf_front": [-24, 0, 0]}
    c.key(0, P_(rest, carry))
    c.key(0.32, P_(UNFURL, wind), E_OUT)
    c.key(0.45, P_(WIDE, rel), E_SNAP)
    c.key(0.58, P_(SPREAD, rel, {"right_arm": [-60, -26, 0]}), E_OUT)
    c.key(0.8, P_(rest, carry), E_IO)


def roles_choirmaster(clip, rest, carry, trem):
    # hymn (the note rises at 0.6 s = 12 ticks, about 2 s): arms raised, head back, mouth open in song.
    c = clip("hymn", 2.0)
    sing = {"right_arm": [-150, 0, 28], "left_arm": [-150, 0, -28], "right_forearm": [-12, 0, 0], "left_forearm": [-12, 0, 0],
            "head": [-30, 0, 0], "body": [-8, 0, 0], "@root": [0, 0.3, 0.4]}
    c.key(0, P_(rest, carry))
    c.key(0.45, P_(UNFURL, sing, {"head": [-20, 0, 0]}), E_OUT)
    c.key(0.6, P_(SPREAD, sing), E_BACK)
    c.key(1.7, P_(SPREAD, sing, {"head": [-34, 0, 0], "right_arm": [-156, 0, 30], "left_arm": [-156, 0, -30]}), E_IO)
    c.key(2.0, P_(rest, carry), E_IO)
    c.layer("head", lambda t: [0.0, 0.0, 0.0] if t < 0.6 or t > 1.7 else [1.5 * math.sin(t * 9), 0, 2.5 * math.sin(t * 5)])
    c.layer("body", lambda t: [0.0, 0.0, 0.0] if t < 0.6 or t > 1.7 else [0, 0, 1.2 * math.sin(t * 5)])


def roles_dominion(clip, rest, carry, trem):
    # judgement (lands at 0.8 s = 16 ticks): the hammer lifted high in both hands, then brought down to the ground.
    c = clip("judgement", 1.4)
    lift = {"right_arm": [-178, -8, 4], "left_arm": [-176, 22, -10], "right_forearm": [-4, 0, 0], "left_forearm": [-20, 0, 0],
            "body": [-14, 0, 0], "head": [-16, 0, 0], "@root": [0, 0.8, 0.8], "right_leg": [4, 0, 0], "left_leg": [-4, 0, 0]}
    down = {"right_arm": [-48, -6, 2], "left_arm": [-48, 24, -6], "right_forearm": [-4, 0, 0], "left_forearm": [-24, 0, 0],
            "body": [34, 0, 0], "head": [-24, 0, 0], "@root": [0, -2.4, -2.0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0],
            "right_shin": [44, 0, 0], "left_shin": [44, 0, 0]}
    c.key(0, P_(rest, carry))
    c.key(0.55, P_(UNFURL, lift), E_OUT)
    c.key(0.66, P_(HIGH, lift, {"right_arm": [-182, -8, 4], "left_arm": [-180, 22, -10], "body": [-16, 0, 0]}), E_IO)
    c.key(0.8, P_(WIDE, down), E_SNAP)
    c.key(1.05, P_(SPREAD, down, {"body": [32, 0, 0]}), E_OUT)
    c.key(1.4, P_(rest, carry), E_IO)


def roles_grigori(clip, rest, carry, trem):
    # fade (gone after 0.4 s = 8 ticks): a step back as it dissolves.
    c = clip("fade", 0.6)
    back = {"@root": [0, 0, 2.6], "body": [-8, 0, 0], "head": [14, 0, 0], "right_arm": [-20, 0, 18], "left_arm": [-20, 0, -18],
            "right_forearm": [-50, 0, 0], "left_forearm": [-50, 0, 0], "right_leg": [24, 0, 0], "left_leg": [-6, 0, 0]}
    c.key(0, P_(rest, carry))
    c.key(0.4, P_(rest, back), E_OUT)
    c.key(0.48, P_(rest, back, {"@root": [0, 0, 3.0]}), E_OUT)
    c.key(0.6, P_(rest, carry), E_IO)
    # drain (it feeds from 0.3 s = 6 ticks, held about 1 s): one hand clamped forward, the other clawing.
    c = clip("drain", 1.4)
    feed = {"right_arm": [-88, -6, 0], "right_forearm": [-4, 0, 0], "right_hand": [-24, 0, 0], "left_arm": [-60, 30, -30],
            "left_forearm": [-60, 0, 0], "left_hand": [-40, 0, 0], "body": [16, 0, 0], "head": [-10, 0, 0], "@root": [0, -0.6, -1.2],
            "right_leg": [-12, 0, 0], "left_leg": [14, 0, 0]}
    flare = wings(dict(AA.BROKEN_R, arm=(0, 18, 46)), dict(AL.OPEN, arm=(0, 16, 40)))
    c.key(0, P_(rest, carry))
    c.key(0.3, P_(flare, feed), E_SNAP)
    c.key(1.2, P_(flare, feed, {"body": [20, 0, 0], "head": [-14, 0, 0]}), E_IO)
    c.key(1.4, P_(rest, carry), E_IO)
    c.layer("right_arm", trem(1.4, 91, 67))
    c.layer("left_hand", trem(4.0, 60, 43))
    c.layer("head", trem(1.0, 77, 53))


ROLES = {"angel_templar": roles_templar, "angel_herald": roles_herald, "angel_choirmaster": roles_choirmaster,
         "angel_dominion": roles_dominion, "grigori": roles_grigori}


# =================================================================================================================

def generate():
    for name in MOBS:
        m, t, g = build(name)
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        f, _ = anims(name)
        f.write(ANIM + name + ".animation.json")


def preview(out_dir, names=None, clip_times=(("idle", 0.0),), hide_wings=True):
    import preview3d
    os.makedirs(out_dir, exist_ok=True)
    for name in names or MOBS:
        m, t, g = build(name)
        _, clips = anims(name)
        by = {c.anim.name.split(".")[-1]: c for c in clips}
        for clip, tm in clip_times:
            pose = by[clip].pose_at(tm)
            hidden = ("wings",) if (hide_wings and name != "grigori" and clip in ("idle", "walk", "block", "throw")) else ()
            preview3d.render(m.rig, t, os.path.join(out_dir, f"{name}_{clip}.png"), pose=pose, hidden=hidden, scale=4)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "preview":
        names = sys.argv[3].split(",") if len(sys.argv) > 3 else None
        clips = [(c.split(":")[0], float(c.split(":")[1])) for c in sys.argv[4].split(",")] if len(sys.argv) > 4 else [("idle", 0.0)]
        preview(sys.argv[2], names, clips)
    else:
        generate()
