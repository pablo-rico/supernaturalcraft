"""Cain, the Father of Murder (v0.21): the boss's rig, texture + glowmask and clips (names in CainAssets), and the spoils
that are his: Cain's honey, the scrap with his address (a compass target, 32 needle frames like Rowena's calling card)
and the bust on the trophies' pedestal. The NPC on his farm (CODE's `cain_npc`) shares the rig and adds two loops, tend
and talk.

The beekeeper of seasons nine and ten: a heavy-set older man, weathered and sun-browned, short grey hair receding at the
temples, a short full grey beard, heavy grey brows over tired brown eyes. A rust-and-brown flannel shirt with the sleeves
rolled to the elbow (both forearms bare: the Mark is on the right one), an olive waxed-canvas work vest left open (a hive
tool in its breast pocket), a leather belt with a brass buckle, khaki work trousers worn pale at the knees, laced brown
work boots. On his left hip a bee smoker (tin can, spout, leather bellows); on his right a folded beekeeper's veil hat; a
red rag in his back pocket; an old watch on his left wrist.

Procedural bones (CainAssets.PROCEDURAL: no clip keys them):
  mark         a skin-tight shell over his right forearm: the Mark of Cain (a raised scar rune, raw skin round it, dark
               veins running from it to the elbow and the wrist). Glowmask red; the renderer shows it from phase II and
               pulses it.
  first_blade  the First Blade in his right fist, held forward (the grip through the fist along Z, the jawbone out of the
               thumb side, its teeth along the top edge). The renderer shows it in phase III.
Toggled bone (contract proposal CainAssets.EYES, the renderer's to show): `eyes`, a shell over his eyes that burns red
(glowmask) once the Mark has him (phase III); hidden it leaves his own brown eyes.
  mouth        his open mouth, tucked inside the head at rest; the roaring clips slide it forward.

Skeleton (Bedrock px; +X is his LEFT; the model faces north):
  root -- body -- head -- hair, brow_r, brow_l, eyes, mouth, beard
               -- collar, vest_r, vest_l, vest_back, hive_tool, smoker -- smoker_lid, bellows;  veil, rag
               -- right_arm -- right_forearm -- cuff_r, mark;  right_hand -- right_fingers, right_index, right_thumb, first_blade
               -- left_arm  -- left_forearm  -- cuff_l;  left_hand  -- left_fingers, left_index, left_thumb
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import blockmodel as BM
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import art, save
from legacy_art import (SIDES, Build, boot_m, done, ears_nose, fix, hair_m, head_cube, leather_m, reach as _reach, skeleton,
                        worn)
from michael_art import ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, Clip, to_preview, tone, world_point, write_compact
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade
from witchkit import CLAW_L, CLAW_R, CURL_L, CURL_R, FIST_L, FIST_R, OPEN_L, OPEN_R, POINT_R, glow_k, metal

A = "animation.cain."
NAME = "cain"

# A heavy-set older man: a deeper chest and a little belly, thick forearms.
CB = Build(hip=12.0, neck=24.0, tw=3.9, depth=4.3, aw=3.6, ax=5.75, sx=5.3, sy=23.0, elbow=18.0, wrist=13.6, palm=11.6,
           lx=1.95, lw=3.7, knee=6.2, shoe_h=1.5)

SKIN = Ramp("#5a3a2b", "#86593f", "#a87557", "#bf8b6b", "#d09f80", "#deb497")
GREY = Ramp("#34312f", "#4a4643", "#625d59", "#7b7570", "#948e88", "#aea8a1")
BEARD = Ramp("#3a3633", "#55504b", "#726c65", "#8f8880", "#aaa39a", "#c6c0b6")
IRIS = hexc("#5b3a22")
PUPIL = hexc("#1e130c")
EYE_WHITE = hexc("#ddd2c2")
LIPS = Ramp("#4f2a22", "#6c3a2f", "#86493a", "#9c5947", "#b06a56", "#c47e69")
FLANNEL = Ramp("#3a120c", "#561c12", "#73281a", "#8d3622", "#a5472d", "#bd5c3c")
FL_DARK = hexc("#22140e")
FL_LINE = hexc("#c9b48a")
CANVAS = Ramp("#26261a", "#363624", "#47472f", "#59583a", "#6c6a47", "#827f57")
KHAKI = Ramp("#3f3524", "#584a32", "#716041", "#8a7651", "#a28c62", "#b9a377")
BELT = Ramp("#1c1009", "#2c190e", "#3e2415", "#51311d", "#654027", "#7b5133")
BOOT = Ramp("#1f1209", "#311c0e", "#462815", "#5b351d", "#704428", "#875535")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
TIN = Ramp("#2e2f31", "#46484b", "#62656a", "#80848a", "#a2a6ac", "#c8ccd1")
RAG = Ramp("#3a0707", "#5a0c0c", "#7a1414", "#9a1e1c", "#b72d28", "#cf4a40")
VEIL = Ramp("#6e6a5e", "#8d887a", "#aca695", "#c6c0ae", "#dcd7c6", "#efebdd")
BONE = Ramp("#4f463a", "#7c705e", "#a69a83", "#c4b99f", "#dcd2ba", "#efe8d6")
GRIP = Ramp("#1a0f0a", "#2a1810", "#3b2217", "#4d2e1f", "#603b28", "#764a33")
BLOOD = Ramp("#1e0303", "#360607", "#520b0c", "#701312", "#931d1a", "#b82a24")
MARK = hexc("#ff3a1e")
MARK_HOT = hexc("#ffc27a")
HONEY = Ramp("#5a2c03", "#8a4806", "#b8680c", "#dc8d18", "#f2b232", "#ffd96a")

CAIN_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsshhhhhhsshhh",
    "hssssssssssssssh",
    "ssslllsssslllsss",
    "sbBBBbssssbBBBbs",
    "suWIEWssssWEIWus",
    "sccuuussssuuuccs",
    "sssssssnnsssssss",
    "gssssssnnssssssg",
    "ggssssNnnNssssgg",
    "gggGGGGGGGGGGggg",
    "ggggGmmmmmmGgggg",
    "ggggggMMMMgggggg",
    "gggggggggggggggg",
]


# =====================================================================================================
# Shared pieces (the Knight and the Herald use them too)
# =====================================================================================================

def man_hands(m, b, skin, glow=None, nails=None, knuckles=True, p=""):
    """Broad hands: the palm, the index, the other three together, the thumb (an index bone on both sides)."""
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{p}{side}_hand", (cx - 1.3, b.palm, -1.45), (2.6, b.wrist - b.palm, 2.9), skin, glow, density=2, tag="palm")

        def fing(f, x, y, w, h):
            c = skin(f, x, y, w, h)
            if nails is not None and (f == "down" or (f != "up" and y == h - 1)):
                return mix(c, nails, 0.6)
            if knuckles and f == "north" and y == 0:
                return shade(c, 1.08)
            return shade(c, 0.9 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{p}{side}_index", (cx - 1.25, b.palm - 1.5, -1.45), (2.5, 1.5, 0.8), fing, glow, density=2, tag="index")
        m.cube(f"{p}{side}_fingers", (cx - 1.25, b.palm - 1.35, -0.65), (2.5, 1.35, 2.05), fing, glow, density=2, tag="fingers")
        m.cube(f"{p}{side}_thumb", (cx - 1.0 * s - 0.5, b.palm + 0.2, -2.05), (1.0, 1.5, 0.9), skin, glow, density=2, tag="thumb")


def man_skeleton(m, b, p=""):
    m.rig.bone(p + "root", (0, 0, 0))
    skeleton(m, b, p, p + "root")
    m.bone(f"{p}left_index", (b.ax, b.palm, -1.0), f"{p}left_hand")


def plaid(seed, base=FLANNEL, dark=FL_DARK, line=FL_LINE, cell=8):
    """Flannel check: dark bands both ways (darker where they cross), a thin pale overcheck, a brushed nap."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f) * 5, y * 2, seed, 64, 64, 2, 9.0)
        c = tone(base, 2.8 + (n - 0.5) * 0.9 + (0.6 if f == "up" else -0.8 if f == "down" else 0))
        vx, vy = x % cell, y % cell
        bx, by = vx < 3, vy < 3
        if bx and by:
            c = mix(c, dark, 0.72)
        elif bx or by:
            c = mix(c, dark, 0.42)
        if vx == 5 or vy == 5:
            c = mix(c, line, 0.32 if (x + y) % 2 else 0.18)
        if f in ("east", "west"):
            c = shade(c, 0.92)
        return c
    return m


def skin_hairy(ramp, seed, base=3.1, hair=None, amount=0.12):
    """Weathered skin with a dusting of hairs (forearms)."""
    sk = H.skin_mat(ramp, seed, base, 0.45)

    def m(f, x, y, w, h):
        c = sk(f, x, y, w, h)
        if hair is not None and f not in ("down",) and h01("hair", seed, f, x, y) < amount:
            c = mix(c, hair, 0.45)
        return c
    return m


# =====================================================================================================
# The rig
# =====================================================================================================

GRIP_PT = (-CB.ax, CB.palm - 0.55, -0.35)      # where the First Blade's grip passes through his right fist (rest space)


def rig():
    m = Model(NAME, 256, 256)
    b = CB
    man_skeleton(m, b)
    y0 = b.neck
    m.bone("hair", (0, y0 + 6, 0), "head")
    m.bone("brow_r", (-2.0, y0 + 4.8, -4.1), "head")
    m.bone("brow_l", (2.0, y0 + 4.8, -4.1), "head")
    m.bone("eyes", (0, y0 + 4.2, -4.0), "head")
    m.bone("mouth", (0, y0 + 1.6, -4.0), "head")
    m.bone("beard", (0, y0 + 1.0, -2.0), "head")
    m.bone("collar", (0, y0 - 0.3, 0), "body")
    m.bone("vest_r", (-2.4, y0 - 0.6, -2.3), "body")
    m.bone("vest_l", (2.4, y0 - 0.6, -2.3), "body")
    m.bone("vest_back", (0, b.hip - 0.2, 2.3), "body")
    m.bone("hive_tool", (2.3, 20.4, -2.6), "vest_l")
    m.bone("smoker", (b.tw + 0.5, b.hip + 0.2, 0.4), "body")
    m.bone("smoker_lid", (b.tw + 1.4, b.hip - 1.6, 0.4), "smoker")
    m.bone("bellows", (b.tw + 1.4, b.hip - 3.6, 1.6), "smoker")
    m.bone("veil", (-b.tw - 0.4, b.hip + 0.2, 1.4), "body")
    m.bone("veil_mesh", (-b.tw - 0.9, b.hip - 1.8, 1.4), "veil")
    m.bone("rag", (-1.8, b.hip - 0.4, 2.4), "body")
    for side, s in SIDES:
        m.bone(f"cuff_{side[0]}", (b.ax * s, b.elbow, 0), f"{side}_forearm")
    m.bone("mark", (-b.ax, b.elbow, 0), "right_forearm")
    m.bone("first_blade", GRIP_PT, "right_hand")
    build_head(m)
    build_body(m)
    build_arms(m)
    build_legs(m)
    build_props(m)
    build_blade(m, "first_blade", GRIP_PT, 0.8)
    return m


def build_head(m):
    b = CB
    y0 = b.neck
    sk = H.skin_mat(SKIN, 22101, 3.1, 0.5)
    hr = hair_m(GREY, 22102, 2.7, comb=True)

    def beard_m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, 22103, 64, 64, 2, 7.0)
        v = 2.9 + (n - 0.5) * 1.3 + 0.4 * math.sin(x * 2.3 + y * 0.7)
        c = tone(BEARD, v)
        if h01("bw", x, y, f) < 0.12:
            c = BEARD[5]                         # the white hairs coming through
        return c
    legend = {"h": hr, "s": sk, "l": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9), "b": GREY[2], "B": GREY[3],
              "u": mix(SKIN[2], hexc("#4a3540"), 0.3), "W": EYE_WHITE, "I": IRIS, "E": PUPIL,
              "c": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.86), "n": SKIN[4], "N": SKIN[1], "g": beard_m,
              "G": lambda f, x, y, w, h: shade(beard_m(f, x, y, w, h), 0.86), "m": LIPS[0],
              "M": lambda f, x, y, w, h: mix(LIPS[1], beard_m(f, x, y, w, h), 0.55)}
    sides = H.head_sides(sk, hr, top=3, back=13, temple=7, sideburn=10, beard=beard_m, beard_from=10)
    head_cube(m, "head", y0, fix(CAIN_FACE), legend, sides)
    ears_nose(m, "head", y0, SKIN, nose_h=1.8)
    m.cube("body", (-1.6, y0 - 0.8, -1.6), (3.2, 1.2, 3.2), sk, density=2, tag="neck")
    # Short grey hair, thinning at the temples, combed back: a thin shell over the crown and the back.
    def shell(f, x, y, w, h):
        if f in ("down", "north"):
            return None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if (fx > 6 or y < 1) else None
        return hr(f, x, y, w, h)
    m.cube("hair", (-4, y0 + 5.6, -3.6), (8, 2.4, 7.6), shell, inflate=0.28, density=2, tag="hair")
    m.cube("hair", (-4, y0 + 2.4, 1.0), (8, 3.2, 3.0), none_on(("north", "up"), hr), inflate=0.2, density=2, tag="hair_back")
    # Heavy grey brows.
    for bone, s in (("brow_r", -1), ("brow_l", 1)):
        m.cube(bone, (2.0 * s - 1.3, y0 + 4.6, -4.18), (2.6, 0.55, 0.3), lambda f, x, y, w, h: GREY[3] if (x + y) % 3 else GREY[2],
               density=4, tag="brow")
    # The beard under the jaw: a short full beard rounding the chin.
    def chin(f, x, y, w, h):
        if f == "up":
            return None
        return beard_m(f, x, y + 16, w, h)
    m.cube("beard", (-3.9, y0 - 1.1, -4.3), (7.8, 1.3, 4.6), chin, density=2, tag="beard_chin")
    m.cube("beard", (-2.6, y0 - 1.7, -4.1), (5.2, 0.7, 2.6), chin, density=2, tag="beard_tip")
    # The open mouth: tucked behind the lips at rest.
    def mouth(f, x, y, w, h):
        if y == 0:
            return hexc("#d9cfbe") if 1 <= x <= w - 2 else LIPS[1]
        if y == h - 1:
            return LIPS[2]
        return hexc("#2a0c0a") if 1 <= x <= w - 2 else LIPS[1]
    m.cube("mouth", (-1.3, y0 + 1.0, -3.7), (2.6, 1.2, 0), mouth, faces=("north",), density=4, tag="mouth")
    # The eyes the Mark gives him: a shell over each eye, blood-red, burning.
    def eyes(f, x, y, w, h):
        if f != "north":
            return None
        ex = x // 2       # 16 cells across (the face's texels), 2 sub-texels each
        if ex in (3, 4, 11, 12):
            return hexc("#ffd29a") if (ex in (4, 11) and y == 1) else hexc("#e8251a")
        if ex in (2, 5, 10, 13):
            return hexc("#6e0a08")
        return None

    def eyes_glow(f, x, y, w, h, c):
        if c == hexc("#6e0a08"):
            return hexc("#5a0606")
        return c
    m.cube("eyes", (-4, y0 + 4.0, -4.04), (8, 0.5, 0), eyes, eyes_glow, faces=("north",), density=4, tag="mark_eyes")


def build_body(m):
    b = CB
    fl = plaid(22201)
    vest = worn(CANVAS, 22202, 3.0, 1.1, scuff=0.08)
    # The shirt torso, a little belly at the belt.
    m.cube("body", (-b.tw, b.hip, -b.depth / 2), (2 * b.tw, b.neck - b.hip, b.depth), fl, density=2, tag="torso")
    m.cube("body", (-b.tw + 0.4, b.hip + 0.2, -b.depth / 2 - 0.15), (2 * b.tw - 0.8, 4.0, 0.6), none_on(("south",), fl), density=2,
           tag="belly")
    # Shirt buttons down the placket.
    m.cube("body", (-0.25, b.hip + 1.0, -b.depth / 2 - 0.55), (0.5, b.neck - b.hip - 2.0, 0.1),
           lambda f, x, y, w, h: hexc("#d6ccb4") if y % 6 == 2 else mix(FLANNEL[1], FL_DARK, 0.3), density=4, tag="placket")
    # The collar: points spread over the vest, the top button open.
    for s in (-1, 1):
        m.cube("collar", (1.2 * s - 0.9, b.neck - 1.4, -2.55), (1.8, 1.3, 0.4), lambda f, x, y, w, h: fl(f, x, y + 3, w, h),
               density=2, tag="shirt_collar", rotation=(16, 0, -34 * s), pivot=(1.2 * s, b.neck - 0.2, -2.3))
        m.cube("collar", (2.2 * s - 0.6 - (0.6 if s < 0 else 0), b.neck - 0.9, -1.8), (1.8, 0.9, 3.4), none_on(("down",), fl), density=2,
               tag="collar_band")
    # The vest's back and sides (its fronts are their own bones).
    def back(f, x, y, w, h):
        if f in ("north", "down"):
            return None
        if f == "up":
            fz = y / max(1, h - 1)
            return vest(f, x, y, w, h) if fz > 0.35 else None
        c = vest(f, x, y, w, h)
        if f == "south" and y in (3, h - 4) and x % 2 == 0:
            return CANVAS[5]                     # topstitching across the yoke and above the hem
        return c
    m.cube("body", (-b.tw, b.hip - 0.4, -b.depth / 2), (2 * b.tw, b.neck - b.hip - 0.6, b.depth), back, inflate=0.38, density=2, tag="vest")
    # Vest fronts: open over the shirt, a pocket with a flap each side, the breast pocket on his left.
    for bone, s in (("vest_r", -1), ("vest_l", 1)):
        def front(f, x, y, w, h, s=s):
            if f == "south":
                return CANVAS[1]
            c = vest(f, x, y, w, h)
            inner = (x == 0) if s > 0 else (x == w - 1)
            if f == "north":
                if inner:
                    return CANVAS[1]
                if (x == 1 if s > 0 else x == w - 2) and y % 2 == 0:
                    return CANVAS[5]                 # the edge stitching
                # Lower pocket: a flap at row 15, the pocket below.
                if 15 <= y <= 16 and 1 <= x <= w - 2:
                    return CANVAS[4] if y == 15 else CANVAS[1]
                if s > 0 and y in (6, 7) and 2 <= x <= w - 2:
                    return CANVAS[4] if y == 6 else CANVAS[1]   # the breast pocket's welt
            if f in ("east", "west") and y % 3 == 0:
                return shade(c, 0.9)
            return c
        x0 = 0.75 if s > 0 else -b.tw - 0.42
        m.cube(bone, (x0, b.hip - 1.6, -b.depth / 2 - 0.62), (b.tw - 0.33, b.neck - b.hip + 0.8, 0.45), front, density=2,
               tag="vest_front")
        # Metal snaps down the front edge.
        for k in range(3):
            m.cube(bone, ((0.95 if s > 0 else -1.35), b.hip + 1.0 + k * 3.2, -b.depth / 2 - 0.7), (0.4, 0.4, 0.1),
                   solid(TIN[4]), density=4, tag="snap")
    m.cube("vest_back", (-b.tw - 0.38, b.hip - 1.6, b.depth / 2 + 0.02), (2 * b.tw + 0.76, 1.6, 0.36),
           lambda f, x, y, w, h: CANVAS[0] if f == "north" else vest(f, x, y + 30, w, h + 30), density=2, tag="vest_hem")
    # The belt and buckle.
    def belt(f, x, y, w, h):
        if f in ("up", "down"):
            return None
        c = leather_m(BELT, 22203, 3.0)(f, x, y, w, h)
        if y in (0, h - 1):
            return shade(c, 0.8)
        if f == "south" and x % 6 == 0:
            return BELT[0]                          # belt loops
        return c
    m.cube("body", (-b.tw, b.hip - 0.3, -b.depth / 2), (2 * b.tw, 1.0, b.depth), belt, inflate=0.42, density=2, tag="belt")
    m.cube("body", (-0.8, b.hip - 0.45, -b.depth / 2 - 0.62), (1.6, 1.3, 0.25), lambda f, x, y, w, h: BRASS[4] if (x in (0, w - 1) or
           y in (0, h - 1)) else (BRASS[2] if f == "north" else BRASS[3]), glow_k(0.06), density=4, tag="buckle")


def build_arms(m):
    b = CB
    fl = plaid(22301)
    skin = skin_hairy(SKIN, 22302, 3.1, GREY[1], 0.1)
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        m.cube(f"{side}_arm", (cx - hw, b.elbow + 0.2, -hw), (b.aw, b.sy + 0.8 - b.elbow - 0.2, b.aw), fl, inflate=0.18, density=2,
               tag="sleeve")
        # Shoulder seams the vest's armhole shows.
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist, -1.5), (3.0, b.elbow - b.wrist + 0.4, 3.0), none_on(("up",), skin), density=2,
               tag="forearm")
        # The rolled sleeve: a fat band of the flannel turned inside out (paler) at the elbow.
        def roll(f, x, y, w, h):
            if f in ("up", "down"):
                return FLANNEL[1]
            c = plaid(22303, FLANNEL, mix(FL_DARK, FLANNEL[2], 0.4), FL_LINE)(f, x, y, w, h)
            c = mix(c, hexc("#c9a888"), 0.42)
            return shade(c, 1.1) if y == 1 else shade(c, 0.82) if y == h - 1 else c
        m.cube(f"cuff_{side[0]}", (cx - 1.75, b.elbow - 0.9, -1.75), (3.5, 1.5, 3.5), roll, inflate=0.32, density=2, tag="sleeve_roll")
    # The old watch on his left wrist.
    m.cube("left_hand", (b.ax - 1.45, b.wrist - 0.75, -1.6), (2.9, 0.65, 3.2), lambda f, x, y, w, h: BELT[3] if y else BELT[4],
           inflate=0.08, density=4, tag="watch_strap")
    m.cube("left_hand", (b.ax + 1.5, b.wrist - 0.9, -0.6), (0.25, 0.95, 1.2), lambda f, x, y, w, h: hexc("#e8dcc0") if
           (f == "east" and 0 < x < w - 1 and 0 < y < h - 1) else BRASS[3], density=4, tag="watch_face")
    man_hands(m, b, H.skin_mat(SKIN, 22304, 3.0, 0.5), nails=hexc("#b89a86"))
    build_mark(m)


# The Mark: a raised scar rune (X), raw skin round it (o); 7 x 11 texels at density 4 on the forearm's front.
MARK_RUNE = [
    "..XXX..",
    ".X...X.",
    ".....X.",
    "...XX..",
    "..X....",
    ".XXXXX.",
    "..X....",
    "..X....",
    "...X...",
    "....XX.",
    "......X",
]


def build_mark(m):
    b = CB
    cx = -b.ax
    rw, rh = len(MARK_RUNE[0]), len(MARK_RUNE)
    hot = {(x, y) for y, row in enumerate(MARK_RUNE) for x, ch in enumerate(row) if ch == "X"}

    def veins(f, x, y, w, h):
        # Dark veins from the rune up to the elbow and down to the wrist, forking.
        k = {"north": 0, "east": 1, "west": 2, "south": 3}.get(f, 4)
        for i in range(3):
            base = (w - 1) / 2 + (i - 1) * w * 0.3 + (k - 1.5) * 1.5
            xx = base + 1.6 * math.sin(y * 0.6 + i * 2.1 + k) + 0.6 * math.sin(y * 1.7 + i)
            if abs(x - xx) < 0.55 and h01("vein", f, i, y // 3) < 0.8:
                return True
        return False

    def kind(f, x, y, w, h):
        if f in ("up", "down"):
            return None
        if f == "north":
            ox, oy = (w - rw) // 2, (h - rh) // 2 + 1
            rx, ry = x - ox, y - oy
            if (rx, ry) in hot:
                return "rune"
            if any((rx + dx, ry + dy) in hot for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                return "raw"
            if 0 <= ry < rh and -1 <= rx <= rw:
                return None
        if veins(f, x, y, w, h):
            return "vein"
        return None

    def mat(f, x, y, w, h):
        k = kind(f, x, y, w, h)
        if k == "rune":
            return hexc("#5c0f0a") if (x + y) % 3 else hexc("#7a1a10")
        if k == "raw":
            return mix(SKIN[2], hexc("#a0281c"), 0.55)
        if k == "vein":
            return mix(SKIN[1], hexc("#4a0c10"), 0.6)
        return None

    def glow(f, x, y, w, h, c):
        k = kind(f, x, y, w, h)
        if k == "rune":
            return MARK_HOT if h01("hot", x, y) < 0.35 else MARK
        if k == "raw":
            return hexc("#8a1408")
        if k == "vein":
            return hexc("#4a0604")
        return None
    m.cube("mark", (cx - 1.5, b.wrist + 0.2, -1.5), (3.0, b.elbow - b.wrist - 0.6, 3.0), mat, glow, inflate=0.07, density=4, tag="mark")


def build_legs(m):
    b = CB
    tr = worn(KHAKI, 22401, 3.0, 1.0, scuff=0.06)

    def trousers(f, x, y, w, h):
        c = tr(f, x, y, w, h)
        if f == "north" and 0.1 < y / max(1, h) < 0.45 and 1 <= x <= w - 2:
            c = mix(c, KHAKI[5], 0.25)            # the knees worn pale
        if f in ("east", "west") and x == w // 2:
            c = shade(c, 0.85)                    # the outseam
        return c
    bt = boot_m(BOOT, 22402)
    for side, s in SIDES:
        x0 = b.lx * s - b.lw / 2
        m.cube(f"{side}_leg", (x0, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), trousers, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, 3.0, -b.lw / 2), (b.lw, b.knee - 3.0, b.lw), lambda f, x, y, w, h: trousers(f, x, y + 12, w, h + 12),
               density=2, tag="shin")
        # Laced work boots: the shaft to mid-shin, the toe, a heavy sole.
        def shaft(f, x, y, w, h):
            c = bt(f, x, y, w, h)
            if f == "north" and abs(x - (w - 1) / 2) < 1.6 and y % 2 == 0 and y < h - 1:
                return BOOT[0] if abs(x - (w - 1) / 2) > 0.6 else hexc("#c2ac80")   # laces over the tongue
            return c
        m.cube(f"{side}_shin", (x0 - 0.05, 1.0, -b.lw / 2 - 0.05), (b.lw + 0.1, 2.6, b.lw + 0.1), shaft, inflate=0.12, density=2,
               tag="boot_shaft")
        m.cube(f"{side}_shin", (x0 - 0.1, 0.5, -b.lw / 2 - 1.5), (b.lw + 0.2, 1.4, b.lw + 1.6), bt, density=2, tag="boot")
        m.cube(f"{side}_shin", (x0 - 0.2, 0.0, -b.lw / 2 - 1.6), (b.lw + 0.4, 0.55, b.lw + 1.85), lambda f, x, y, w, h: hexc("#1a120c")
               if f != "up" else BOOT[1], density=2, tag="sole")
        # Trouser hem bunched over the boot top.
        m.cube(f"{side}_shin", (x0, 3.0, -b.lw / 2), (b.lw, 0.7, b.lw), none_on(("up", "down"), lambda f, x, y, w, h: tone(KHAKI, 2.2 if y else 3.4)),
               inflate=0.3, density=2, tag="hem")


def build_props(m):
    b = CB
    # The hive tool in the breast pocket: a flat steel bar with a red-painted end.
    m.cube("hive_tool", (2.0, 20.2, -2.75), (0.6, 2.4, 0.18), lambda f, x, y, w, h: hexc("#a82a1e") if y < 3 else TIN[4 if f == "north" else 2],
           density=4, tag="hive_tool")
    # The bee smoker on his left hip: a tin can (an octagon of two boxes), the hinged spout lid, the bellows behind.
    sx, sy, sz = b.tw + 1.4, b.hip - 1.6, 0.4

    def can(f, x, y, w, h):
        if f == "up":
            return TIN[3]
        if f == "down":
            return TIN[0]
        n = fbm(x * 3, y * 3 + len(f), 22501, 64, 64, 2, 6.0)
        v = 3.2 - 1.6 * y / max(1, h) + (n - 0.5) * 1.2
        c = tone(TIN, v)
        if y >= h - 3 and n > 0.45:
            c = mix(c, hexc("#2a1a10"), 0.5)          # soot at the bottom
        if y in (1, h - 2):
            c = TIN[1]                             # rolled seams
        return c
    for rot in (0, 45):
        m.cube("smoker", (sx - 1.0, sy - 3.4, sz - 1.0), (2.0, 3.4, 2.0), can, density=4, tag="smoker_can", rotation=(0, rot, 0),
               pivot=(sx, sy, sz))
    # The heat shield's grille: a ring of holes.
    m.cube("smoker", (sx - 1.15, sy - 2.6, sz - 1.15), (2.3, 1.6, 2.3), lambda f, x, y, w, h: None if f in ("up", "down") else
           (TIN[1] if (x + y) % 3 == 0 and 0 < y < h - 1 else TIN[3]), density=4, tag="smoker_guard")
    m.cube("smoker", (sx - 0.2, sy, sz - 0.2), (0.4, 1.8, 0.4), solid(TIN[2]), density=4, tag="smoker_hook")
    m.cube("smoker", (sx - 0.7, sy + 1.4, sz - 0.2), (0.9, 0.3, 0.4), solid(TIN[3]), density=4, tag="smoker_hook_top")
    # The lid: a cone of three shrinking boxes and the nozzle.
    for i, (hw, hh) in enumerate(((1.05, 0.45), (0.8, 0.45), (0.5, 0.45))):
        m.cube("smoker_lid", (sx - hw, sy + i * 0.45 - 0.05, sz - hw), (2 * hw, hh, 2 * hw), lambda f, x, y, w, h, i=i: tone(TIN, 3.4 - i * 0.4 +
               (0.6 if f == "up" else 0)), density=4, tag="smoker_lid")
    m.cube("smoker_lid", (sx - 0.3, sy + 1.2, sz - 0.9), (0.6, 0.5, 0.9), lambda f, x, y, w, h: hexc("#1a120c") if f == "north" else TIN[3],
           density=4, tag="nozzle", rotation=(-25, 0, 0), pivot=(sx, sy + 1.3, sz - 0.3))
    # The bellows: two boards hinged at the bottom, pleated leather between them.
    bx, by, bz = sx, b.hip - 3.6, sz + 1.2
    m.cube("bellows", (bx - 0.7, by, bz), (1.4, 2.8, 0.25), lambda f, x, y, w, h: tone(Ramp("#3a2412", "#55351b", "#6f4524", "#8a582f",
           "#a46b3a", "#bd8048"), 3.0 - y / max(1, h)), density=4, tag="bellows_board")
    m.cube("bellows", (bx - 0.65, by + 0.1, bz + 0.25), (1.3, 2.4, 0.5), lambda f, x, y, w, h: BELT[3] if y % 2 else BELT[1], density=4,
           tag="bellows_leather")
    m.cube("bellows", (bx - 0.7, by, bz + 0.75), (1.4, 2.8, 0.25), lambda f, x, y, w, h: tone(Ramp("#3a2412", "#55351b", "#6f4524", "#8a582f",
           "#a46b3a", "#bd8048"), 3.2 - y / max(1, h)), density=4, tag="bellows_board")
    # The folded veil hat on his right hip: a khaki brim and crown on its side, the mesh hanging under it.
    vx, vy, vz = -b.tw - 0.7, b.hip - 1.0, 1.4
    hat = lambda f, x, y, w, h: tone(VEIL, 3.4 - (0.8 if f in ("down", "east") else 0) + 0.3 * math.sin(x + y))
    m.cube("veil", (vx - 0.3, vy - 1.6, vz - 1.6), (0.3, 3.2, 3.2), hat, density=4, tag="veil_brim")
    m.cube("veil", (vx - 0.95, vy - 0.95, vz - 0.95), (0.65, 1.9, 1.9), hat, density=4, tag="veil_crown")

    def mesh(f, x, y, w, h):
        if f in ("up",):
            return None
        if (x + y) % 2 == 0:
            return None if h01("mesh", f, x, y) < 0.5 else hexc("#3a3731")
        return hexc("#4a463f") if y < h - 1 else VEIL[3]
    m.cube("veil_mesh", (vx - 0.05, vy - 3.4, vz - 1.4), (0.25, 1.9, 2.8), mesh, density=4, tag="veil_mesh")
    # The red rag in his back pocket.
    def rag(f, x, y, w, h):
        if f == "up":
            return None
        c = tone(RAG, 3.0 + 0.6 * math.sin(x * 1.3 + y * 0.4) - 0.8 * y / max(1, h))
        if (x + 2 * y) % 7 == 0:
            return mix(c, hexc("#e8d8c4"), 0.5)    # the bandana's paisley dots
        return c
    m.cube("rag", (-2.9, b.hip - 3.8, 2.45), (2.2, 3.6, 0.2), rag, density=4, tag="rag")
    m.cube("rag", (-3.0, b.hip - 0.9, 2.3), (2.4, 0.7, 0.4), lambda f, x, y, w, h: tone(KHAKI, 2.2), density=4, tag="back_pocket")


def build_blade(m, bone, g, k=1.0):
    """The First Blade with its grip through `g`: the haft along Z (the condyle knob out the back of the fist), the jawbone
    forward out of the thumb side curving a touch down, the teeth along its top, a dried-blood wrap, a vein of the Mark's
    red down its spine (faint glow)."""
    gx, gy, gz = g

    def at(dx, dy, dz):
        return (gx + dx * k, gy + dy * k, gz + dz * k)

    def cube(o, s, mat, glow=None, rot=None, piv=None, tag="first_blade", density=4):
        m.cube(bone, at(*o), tuple(v * k for v in s), mat, glow, density=density, tag=tag, rotation=rot,
               pivot=at(*piv) if piv else None)

    def bone_m(seed, base=3.4):
        def mm(f, x, y, w, h):
            n = fbm(x * 2 + len(f) * 3, y * 2, seed, 64, 64, 3, 6.0)
            v = base + (n - 0.5) * 1.6 + (0.6 if f == "up" else -1.1 if f == "down" else 0)
            c = tone(BONE, v)
            if h01("pit", seed, f, x, y) < 0.06:
                c = shade(c, 0.75)                       # pits in the old bone
            if n < 0.32:
                c = mix(c, BLOOD[2], 0.35)               # stained where blood has soaked in
            return c
        return mm
    wrap = lambda f, x, y, w, h: tone(GRIP, 3.0 - (0.9 if (y + x) % 3 == 0 else 0) + (0.6 if f == "up" else 0))
    cube((-0.45, -0.45, -1.8), (0.9, 0.9, 3.2), wrap, tag="blade_grip")
    cube((-0.55, -0.55, -0.6), (1.1, 1.1, 0.5), lambda f, x, y, w, h: BLOOD[1 + (x + y) % 2], tag="blade_wrap")
    cube((-0.55, -0.55, 0.6), (1.1, 1.1, 0.4), lambda f, x, y, w, h: BLOOD[1 + (x + y) % 2], tag="blade_wrap")
    # The condyle knob (pommel) behind the fist.
    cube((-0.55, -0.4, 1.35), (1.1, 1.2, 1.0), bone_m(22601, 3.6), tag="blade_knob")
    # The ramus rising at the jaw's angle, just ahead of the fist.
    cube((-0.35, -0.1, -2.6), (0.7, 2.4, 0.9), bone_m(22602), rot=(-14, 0, 0), piv=(0, 0, -2.2), tag="blade_ramus")
    # The body of the jaw: segments tapering forward, each tipped a little further down.
    segs = [(-1.8, 2.4, 1.8, 0.7, 0), (-4.2, 2.4, 1.6, 0.64, 4), (-6.6, 2.2, 1.35, 0.56, 9), (-8.8, 1.8, 1.0, 0.46, 15),
            (-10.6, 1.4, 0.65, 0.36, 22)]
    y_off = 0.0
    for i, (z1, L, hgt, th, tilt) in enumerate(segs):
        z0 = z1 - L

        def jaw(f, x, y, w, h, i=i):
            c = bone_m(22610 + i)(f, x, y, w, h)
            if f in ("east", "west") and y == h - 1:
                return BONE[5]                            # the honed lower edge
            if f in ("east", "west") and y == 0:
                return BONE[2]
            return c

        def jaw_glow(f, x, y, w, h, c, i=i):
            if f in ("east", "west") and y == 1 and (x + i) % 3 != 0:
                return hexc("#6a0d08")                    # the Mark's vein down the spine
            return None
        cube((-th / 2, -0.4 - hgt + 0.6 + y_off, z0), (th, hgt, L + 0.1), jaw, jaw_glow, rot=(-tilt, 0, 0), piv=(0, y_off, z1),
             tag="blade_jaw")
        # The teeth along the top: worn molars, then sharper ones toward the tip.
        if i < 3:
            for j in range(2):
                tz = z1 - 0.3 - j * (L / 2)
                th_ = (0.75, 0.65, 0.5)[i]

                def tooth(f, x, y, w, h, i=i, j=j):
                    if f == "up":
                        return mix(BONE[4], hexc("#c9b27a"), 0.3) if (x + y) % 2 else BONE[3]
                    if y == h - 1:
                        return hexc("#5a4630")            # the dark root line at the gum
                    return mix(BONE[3], hexc("#b89a5a"), 0.25 + 0.1 * j)
                cube((-th / 2 - 0.05, 0.6 - 0.4 + y_off, tz - 0.85), (th + 0.1, th_, 0.85), tooth, rot=(-tilt, 0, 0), piv=(0, y_off, z1),
                     tag="blade_tooth")
        y_off -= math.sin(math.radians(tilt)) * L * 0.5
    cube((-0.15, -0.25 + y_off, -12.6), (0.3, 0.5, 0.8), solid(BONE[5]), rot=(-30, 0, 0), piv=(0, y_off, -12.0), tag="blade_tip")


# =====================================================================================================
# Clips
# =====================================================================================================

FORBID = ("mark", "first_blade")
MOUTH_OPEN = {"@mouth": [0, 0, -0.35], "@beard": [0, -0.25, 0]}
ANGRY = {"brow_r": [0, 0, -14], "brow_l": [0, 0, 14], "@brow_r": [0, -0.15, 0], "@brow_l": [0, -0.15, 0]}
REST = P_({"right_fingers": [-24, 0, 0], "right_index": [-18, 0, 0], "left_fingers": [-24, 0, 0], "left_index": [-18, 0, 0]},
          {"right_arm": [0, 0, 4], "left_arm": [0, 0, -4], "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0]})
# The fist that holds the First Blade (whether or not it is shown).
GRIP_R = {"right_fingers": [-88, 0, 0], "right_index": [-84, 0, 0], "right_thumb": [-30, 0, 24]}


def follow(pose):
    """The vest fronts swing open with the stride and the lean, the smoker and the veil hang plumb, the rag swings."""
    out = dict(pose)
    body = pose.get("body", [0, 0, 0])
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    if "vest_r" not in pose:
        out["vest_r"] = [max(-14.0, min(10.0, -body[0] * 0.35)), -max(0.0, body[0]) * 0.25, 0]
    if "vest_l" not in pose:
        out["vest_l"] = [max(-14.0, min(10.0, -body[0] * 0.35)), max(0.0, body[0]) * 0.25, 0]
    if "vest_back" not in pose:
        out["vest_back"] = [-max(0.0, max(rl, ll)) * 0.2 - body[0] * 0.3, 0, 0]
    if "smoker" not in pose:
        out["smoker"] = [-body[0] * 0.8 + min(rl, ll) * -0.1, 0, -body[2] * 0.8]
    if "veil" not in pose:
        out["veil"] = [-body[0] * 0.8, 0, -body[2] * 0.8]
    if "rag" not in pose:
        out["rag"] = [-body[0] * 0.7 - max(rl, ll) * 0.2, 0, 0]
    return out


class CClip(Clip):
    def key(self, t, pose, ease=None):
        return super().key(t, follow(pose), ease)


def man_walk(n, i, stride=22, arm=12, knee=30, bob=0.45, sway=3.5, lean=2.5):
    ph = 2 * math.pi * i / n
    sn = math.sin(ph)
    return {"right_leg": [-stride * sn, 0, 0], "left_leg": [stride * sn, 0, 0],
            "right_shin": [knee * max(0, math.sin(ph + 1.2)), 0, 0], "left_shin": [knee * max(0, -math.sin(ph + 1.2)), 0, 0],
            "right_arm": [arm * sn, 0, 5], "left_arm": [-arm * sn, 0, -5], "right_forearm": [-12, 0, 0],
            "left_forearm": [-12, 0, 0], "body": [lean, sway * sn, -1.2 * sn], "head": [-lean * 0.6, -sway * sn, 0],
            "@root": [0, bob * abs(math.cos(ph)), 0]}


def kneel1(head=12, body=8):
    """Down on his right knee, the left foot planted forward."""
    return {"@root": [0, -4.9, 0], "right_leg": [6, 0, 4], "right_shin": [86, 0, 0], "left_leg": [-82, 0, -4], "left_shin": [84, 0, 0],
            "body": [body, 0, 0], "head": [head, 0, 0]}


def kneel2(head=14, body=4):
    """Down on both knees, sitting back a little on his heels."""
    return {"@root": [0, -5.8, 0.6], "right_leg": [-4, 0, 4], "left_leg": [-4, 0, -4], "right_shin": [94, 0, 0], "left_shin": [94, 0, 0],
            "body": [body, 0, 0], "head": [head, 0, 0]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(CClip(f, A + name, length, loop, hold, forbid=FORBID))
        return clips[-1]

    def reach(pose, side, target, grip=None):
        s = -1 if side == "right" else 1
        return _reach(r, pose, side, target, grip or (CB.ax * s, CB.palm - 0.6, -0.3))

    # His stance: square, heavy, weight even; the right hand loosely closed (the grip), the left easy.
    stance = P_(REST, GRIP_R, {"body": [1, 0, 0], "head": [2, 0, 0], "right_leg": [0, 4, 2], "left_leg": [0, -4, -2],
                               "right_arm": [-4, 0, 6], "left_arm": [-2, 0, -6], "right_forearm": [-14, 0, 0], "right_hand": [0, -10, 0]})

    # --- idle: slow breaths, a look round; once a cycle the left hand rubs the right forearm (the Mark itches). ---
    L = 6.0
    c = clip("idle", L, loop=True)
    rub0 = reach(P_(stance, CURL_L, {"head": [10, -6, 0], "body": [3, -6, 0]}), "left", (-4.0, 16.6, -2.6))
    rub1 = reach(P_(stance, CURL_L, {"head": [12, -8, 0], "body": [3, -6, 0]}), "left", (-4.4, 14.8, -2.7))
    lift = {"right_arm": [-14, 24, 2], "right_forearm": [-58, 0, 0], "right_hand": [50, -10, 0]}
    c.key(0, stance)
    c.key(1.2, P_(stance, {"body": [2, 0, 0], "@root": [0, -0.1, 0], "head": [0, 14, 0]}), E_IO)
    c.key(2.4, P_(stance, {"head": [1, -8, 0]}), E_IO)
    c.key(3.1, P_(rub0, lift), E_IO)
    c.key(3.6, P_(rub1, lift), E_IO)
    c.key(4.1, P_(rub0, lift), E_IO)
    c.key(4.6, P_(rub1, lift), E_IO)
    c.key(5.3, P_(stance, {"head": [4, 0, 0]}), E_IO)
    c.key(L, stance, E_IO)
    c.layer("body", lambda t: [0.8 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
    c.layer("@root", lambda t: [0, -0.06 * math.sin(t * 2 * math.pi * 2 / L), 0])

    # --- walk: a heavy, unhurried stride, the head a little forward, the smoker swinging. ---
    L, n = 1.3, 8
    c = clip("walk", L, loop=True)
    for i in range(n + 1):
        w = man_walk(n, i % n)
        c.key(L * i / n, P_(REST, GRIP_R, w, {"head": [2, w["head"][1], 0]}), E_IO if i else None)
    c.layer("smoker", lambda t: [6 * math.sin(2 * math.pi * t / L - 0.9), 0, 3 * math.sin(4 * math.pi * t / L)])
    c.layer("veil", lambda t: [-5 * math.sin(2 * math.pi * t / L - 0.9), 0, -2 * math.sin(4 * math.pi * t / L)])
    c.layer("rag", lambda t: [8 * math.sin(2 * math.pi * t / L - 1.4), 0, 4 * math.sin(2 * math.pi * t / L)])

    # --- lunge: sinks and draws the blade hand back, then drives forward along the line, the stab out at tick 12. ---
    c = clip("lunge", 1.2)
    coil = P_(REST, GRIP_R, OPEN_L, ANGRY, {"@root": [0, -1.4, 1.2], "body": [16, 18, 0], "head": [-10, -16, 0],
                                            "right_leg": [24, 0, 6], "right_shin": [36, 0, 0], "left_leg": [-34, 0, -4],
                                            "left_shin": [40, 0, 0], "right_arm": [30, 28, 14], "right_forearm": [-78, 0, 0],
                                            "right_hand": [-20, 0, 0], "left_arm": [-50, -10, -16], "left_forearm": [-40, 0, 0]})
    stab = P_(REST, GRIP_R, OPEN_L, ANGRY, {"@root": [0, -1.8, -3.0], "body": [24, -14, 0], "head": [-18, 12, 0],
                                            "right_leg": [30, 0, 4], "right_shin": [18, 0, 0], "left_leg": [-56, 0, -4],
                                            "left_shin": [60, 0, 0], "right_arm": [-96, -10, 0], "right_forearm": [-6, 0, 0],
                                            "right_hand": [70, 0, 0], "left_arm": [26, 16, -22], "left_forearm": [-30, 0, 0],
                                            "vest_r": [-30, -10, 0], "vest_l": [-30, 10, 0], "rag": [-40, 0, 0]})
    c.key(0, stance)
    c.key(0.35, coil, E_OUT)
    c.key(0.48, P_(coil, {"body": [18, 22, 0], "right_arm": [34, 32, 16]}), E_IO)
    c.key(0.6, stab, E_SNAP)
    c.key(0.8, P_(stab, {"right_arm": [-90, -8, 0], "body": [22, -12, 0]}), E_OUT)
    c.key(1.2, stance, E_IO)

    # --- swarm: the right palm raised slowly over his head, fingers spread; the bees rise at tick 14. ---
    c = clip("swarm", 1.5)
    rise = P_(stance, OPEN_R, {"right_arm": [-120, -10, -8], "right_forearm": [-30, 0, 0], "right_hand": [0, 80, 0],
                               "head": [-16, -4, 0], "body": [-5, -4, 0], "right_fingers": [10, 0, 0], "bellows": [0, 0, 0]})
    up = P_(rise, {"right_arm": [-150, -12, -10], "right_forearm": [-8, 0, 0], "right_hand": [0, 80, -20], "head": [-24, 0, 0],
                   "body": [-8, 0, 0], "@root": [0, 0.3, 0], "right_thumb": [0, 0, -30], "bellows": [0, 0, 0]})
    c.key(0, stance)
    c.key(0.45, P_(rise, {"right_arm": [-70, 0, 0], "right_forearm": [-70, 0, 0]}), E_IO)
    c.key(0.7, up, E_OUT)
    c.key(1.05, P_(up, {"right_arm": [-146, -14, -8]}), E_IO)
    c.key(1.5, stance, E_IO)
    # The smoker puffs as they rise (the bellows squeeze twice).
    c.layer("bellows", lambda t: [-14 * max(0, math.sin((t - 0.6) * math.pi * 4)) if 0.6 < t < 1.1 else 0, 0, 0])

    # --- furrow: both fists raised over the right shoulder, a ploughing swing down and across that rips a cone (tick 18). ---
    c = clip("furrow", 1.6)
    raise_ = P_(REST, GRIP_R, FIST_L, ANGRY, {"body": [-14, 30, 0], "head": [-6, -22, 0], "@root": [0, 0.2, 0.6],
                                              "right_arm": [-170, 30, -10], "right_forearm": [-40, 0, 0], "left_arm": [-160, 40, 30],
                                              "left_forearm": [-60, 0, 0], "right_leg": [14, 0, 6], "left_leg": [-16, 0, -4],
                                              "left_shin": [14, 0, 0]})
    slam = P_(REST, GRIP_R, FIST_L, ANGRY, MOUTH_OPEN, {"body": [40, -20, 0], "head": [-24, 14, 0], "@root": [0, -2.4, -1.4],
                                                        "right_arm": [-60, -20, 10], "right_forearm": [-10, 0, 0], "right_hand": [40, 0, 0],
                                                        "left_arm": [-56, -36, -6], "left_forearm": [-40, 0, 0],
                                                        "right_leg": [24, 0, 6], "right_shin": [30, 0, 0], "left_leg": [-52, 0, -6],
                                                        "left_shin": [56, 0, 0], "vest_r": [-30, -10, 0], "vest_l": [-30, 10, 0]})
    # Both fists on one haft: the left hand rides just under the right.
    def both(pose):
        rh = world_point(r, to_preview(follow(pose)), "right_hand", (-CB.ax, CB.palm - 0.6, -0.3))
        d = world_point(r, to_preview(follow(pose)), "right_hand", (-CB.ax, CB.palm + 2.4, -0.3))
        tgt = tuple(rh[i] + (rh[i] - d[i]) * 0.9 for i in range(3))
        return reach(pose, "left", tgt)
    raise_ = both(raise_)
    slam = both(slam)
    raise2 = both(P_(raise_, {"body": [-18, 34, 0], "right_arm": [-176, 34, -12]}))
    c.key(0, stance)
    c.key(0.55, raise_, E_OUT)
    c.key(0.75, raise2, E_IO)
    c.key(0.9, slam, E_SNAP)
    c.key(1.15, P_(slam, {"body": [36, -18, 0]}), E_OUT)
    c.key(1.6, stance, E_IO)

    # --- abels_blood: down on one knee, the left palm laid on the earth; the spikes ring out at tick 22. ---
    c = clip("abels_blood", 2.0)
    kneel = P_(REST, GRIP_R, kneel1(head=22, body=22), {"right_arm": [-10, 0, 10], "right_forearm": [-40, 0, 0]})
    palm = reach(P_(kneel, OPEN_L, {"left_hand": [0, 0, 0]}), "left", (3.8, 0.9, -7.0))
    press = P_(palm, {"body": [28, 0, 0], "head": [30, 0, 0], "left_hand": [-70, 0, 0], "@root": [0, -5.2, 0], "left_fingers": [10, 0, 0]})
    c.key(0, stance)
    c.key(0.55, P_(kneel, {"@root": [0, -3.0, 0], "left_leg": [-60, 0, -4], "left_shin": [60, 0, 0]}), E_IN)
    c.key(0.8, palm, E_OUT)
    c.key(1.1, press, E_SNAP)
    c.key(1.5, P_(press, {"head": [24, 0, 0]}), E_IO)
    c.key(2.0, stance, E_IO)

    # --- throw_tools: the left hand sweeps up and out, fingers clawed, then flings forward (tick 14): the tools fly. ---
    c = clip("throw_tools", 1.4)
    gather = P_(stance, CLAW_L, ANGRY, {"body": [-4, 24, 0], "head": [-8, -14, 0], "left_arm": [-40, 40, -60],
                                        "left_forearm": [-40, 0, 0], "left_hand": [0, 0, 30], "right_leg": [8, 0, 4]})
    fling = P_(stance, OPEN_L, ANGRY, {"body": [10, -22, 0], "head": [2, 14, 0], "left_arm": [-92, -34, 0], "left_forearm": [-6, 0, 0],
                                       "left_hand": [-60, 0, 0], "left_fingers": [6, 0, 0], "left_leg": [-20, 0, -4], "right_leg": [10, 0, 4],
                                       "@root": [0, -0.4, -0.6]})
    c.key(0, stance)
    c.key(0.45, gather, E_OUT)
    c.key(0.58, P_(gather, {"left_arm": [-46, 46, -66]}), E_IO)
    c.key(0.7, fling, E_SNAP)
    c.key(0.95, P_(fling, {"left_arm": [-86, -30, 0]}), E_OUT)
    c.key(1.4, stance, E_IO)

    # --- execution: turns on his heel into nothing (the blink), then high over his victim and down -- the stab at tick 16. ---
    c = clip("execution", 1.8)
    turn = P_(stance, {"root": [0, -40, 0], "body": [10, 0, 0], "head": [14, 0, 0], "@root": [0, -0.6, 0]})
    high = P_(REST, GRIP_R, OPEN_L, ANGRY, {"right_arm": [-176, 10, -8], "right_forearm": [-30, 0, 0], "right_hand": [-60, 0, 0],
                                            "left_arm": [-70, -20, -20], "left_forearm": [-30, 0, 0], "body": [-10, 0, 0],
                                            "head": [-4, 0, 0], "@root": [0, 0.4, 0], "left_leg": [-14, 0, -4], "right_leg": [10, 0, 4]})
    down = P_(REST, GRIP_R, CLAW_L, ANGRY, MOUTH_OPEN, {"right_arm": [-40, 0, 6], "right_forearm": [-20, 0, 0], "right_hand": [-70, 0, 0],
                                                        "left_arm": [-60, 10, -24], "left_forearm": [-50, 0, 0], "body": [36, 0, 0],
                                                        "head": [10, 0, 0], "@root": [0, -2.4, -0.8], "left_leg": [-40, 0, -4],
                                                        "left_shin": [40, 0, 0], "right_leg": [24, 0, 4], "right_shin": [30, 0, 0],
                                                        "vest_r": [-26, -10, 0], "vest_l": [-26, 10, 0]})
    c.key(0, stance)
    c.key(0.3, turn, E_IN)
    c.key(0.4, P_(high, {"root": [0, 0, 0]}), E_SNAP)
    c.key(0.65, P_(high, {"right_arm": [-182, 12, -8]}), E_IO)
    c.key(0.8, down, E_SNAP)
    c.key(1.15, P_(down, {"right_hand": [-60, 0, 0], "body": [34, 0, 0]}), E_IO)
    c.key(1.35, P_(down, {"right_arm": [-60, 0, 6], "right_hand": [-40, 0, 0], "body": [20, 0, 0], "@root": [0, -1.4, -0.4]}), E_OUT)
    c.key(1.8, stance, E_IO)

    # --- mark_flare: the right arm thrust up, fist clenched, the left hand gripping the forearm; head back, a roar at
    # tick 26 as the Mark blazes; the arm shaking with it. ---
    c = clip("mark_flare", 2.5)
    hold = P_(REST, GRIP_R, CLAW_L, ANGRY, {"right_arm": [-168, -8, -6], "right_forearm": [-10, 0, 0], "right_hand": [0, 0, 0],
                                            "body": [-6, 0, 0], "head": [-8, 0, 0], "right_leg": [10, 0, 6], "left_leg": [-10, 0, -6]})
    grab = reach(hold, "left", tuple(world_point(r, {"right_arm": {"rotation": [-168, -8, -6]}, "right_forearm": {"rotation": [-10, 0, 0]},
                                                     "body": {"rotation": [-6, 0, 0]}}, "right_forearm", (-CB.ax + 1.6, 16.4, -0.6))))
    roar = P_(grab, MOUTH_OPEN, {"head": [-30, 0, 0], "body": [-14, 0, 0], "@root": [0, -0.6, 0], "right_arm": [-172, -6, -6]})
    c.key(0, stance)
    c.key(0.6, P_(hold, {"right_arm": [-120, -8, 0], "right_forearm": [-50, 0, 0]}), E_IO)
    c.key(1.0, grab, E_OUT)
    c.key(1.3, roar, E_BACK)
    c.key(1.9, P_(roar, {"head": [-26, 0, 0]}), E_IO)
    c.key(2.5, stance, E_IO)
    c.layer("right_arm", lambda t: [2.2 * math.sin(t * 70) if 1.0 < t < 2.0 else 0, 0, 1.5 * math.sin(t * 53) if 1.0 < t < 2.0 else 0])
    c.layer("body", lambda t: [0.8 * math.sin(t * 61) if 1.3 < t < 1.9 else 0, 0, 0])

    # --- death: the blade hand drops, he sways, goes down on both knees and sits back, hands open on his thighs, head
    # bowed -- at peace; he is down at tick 80 and holds. ---
    c = clip("death", 6.0, hold=True)
    hit = P_(REST, OPEN_R, OPEN_L, {"body": [-10, 6, 0], "head": [-16, 4, 0], "right_arm": [-20, 0, 20], "left_arm": [-20, 0, -20],
                                    "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "@root": [0, 0, 0.8],
                                    "left_leg": [10, 0, 0], "right_leg": [-6, 0, 0]})
    sway = P_(hit, {"body": [8, -6, 4], "head": [16, -4, 4], "right_leg": [-22, 0, 4], "right_shin": [36, 0, 0], "@root": [0, -1.4, 0]})
    down = P_(REST, OPEN_R, OPEN_L, kneel2(head=26, body=10), {"right_arm": [-26, 0, 6], "left_arm": [-26, 0, -6], "right_forearm": [-34, 0, 0],
                                                              "left_forearm": [-34, 0, 0], "right_hand": [-30, 0, 0], "left_hand": [-30, 0, 0]})
    c.key(0, stance)
    c.key(0.3, hit, E_SNAP)
    c.key(1.2, P_(hit, {"body": [-6, 4, -2]}), E_IO)
    c.key(2.4, sway, E_IO)
    c.key(3.4, P_(down, {"@root": [0, -3.2, 0.3], "right_shin": [56, 0, 0], "left_shin": [56, 0, 0], "body": [16, 0, 0]}), E_IN)
    c.key(4.0, down, E_OUT)
    c.key(4.8, P_(down, {"body": [6, 0, 0], "head": [-6, 0, 0]}), E_IO)
    c.key(6.0, P_(down, {"body": [8, 0, 0], "head": [30, 0, 0]}), E_IO)

    # ---------------------------------------------------------------------- NPC: the beekeeper at home ------------
    # His easy stance at home: the hands loose (no fist: there is no blade on the farm).
    calm = P_(stance, CURL_R, CURL_L, {"right_hand": [0, 0, 0], "right_forearm": [-10, 0, 0], "head": [4, 0, 0]})

    # --- tend: at a hive in front of him (its top at chest height a block ahead) he lifts a frame out by its top bar,
    # raises it to his eyes and turns it to the light, sets it back, then brushes the frames with his right hand while
    # the left steadies the smoker on his hip and it puffs. Calm and slow. ---
    # (Own length names: the boss clips' layers read `L` late, so it must keep the value walk left in it.)
    LT = 8.0
    c = clip("tend", LT, loop=True)
    bend = P_(calm, {"body": [14, 0, 0], "head": [22, 0, 0], "@root": [0, -0.2, 0], "right_leg": [-6, 4, 2], "left_leg": [4, -4, -2],
                     "left_shin": [6, 0, 0]})

    def frame(pose, y, z, tilt=0.0, half=3.2):
        """Both hands pinching the ends of a frame's top bar at height y, depth z; `tilt` raises the right end."""
        p = reach(P_(pose, CURL_R, {"right_hand": [-10, 0, 0], "right_thumb": [-30, 0, 10]}), "right", (-half, y + tilt, z))
        return reach(P_(p, CURL_L, {"left_hand": [-10, 0, 0], "left_thumb": [-30, 0, -10]}), "left", (half, y - tilt, z))
    hive = frame(bend, 15.2, -8.4)
    lift = frame(P_(calm, {"body": [6, 0, 0], "head": [14, 0, 0]}), 19.4, -9.8)
    look_r = frame(P_(calm, {"body": [5, 4, 0], "head": [12, 8, -6]}), 19.6, -9.6, tilt=1.4)
    look_l = frame(P_(calm, {"body": [5, -4, 0], "head": [14, -8, 6]}), 19.2, -9.6, tilt=-1.4)
    # The brush: the right palm sweeping over the top bars, the left hand steadying the smoker on his hip (tilted to the
    # hive) while its bellows puff.
    sm = {"smoker": [-20, 0, 0], "bellows": [0, 0, 0]}
    on_bellows = P_(bend, sm)
    lid = world_point(r, to_preview(follow(on_bellows)), "smoker_lid", (CB.tw + 1.4, CB.hip - 0.2, 0.4))
    on_bellows = reach(P_(on_bellows, CURL_L, {"left_hand": [0, 0, 0]}), "left", lid)
    sweep0 = reach(P_(on_bellows, OPEN_R, {"right_hand": [-40, 0, 0]}), "right", (-3.6, 15.8, -8.6))
    sweep1 = reach(P_(on_bellows, OPEN_R, {"right_hand": [-40, 0, 0], "head": [22, -6, 0]}), "right", (0.8, 15.8, -8.8))
    c.key(0, hive)
    c.key(0.7, P_(hive, {"head": [24, 0, 0]}), E_IO)
    c.key(1.6, lift, E_IO)
    c.key(2.5, look_r, E_IO)
    c.key(3.4, look_l, E_IO)
    c.key(4.0, lift, E_IO)
    c.key(4.8, hive, E_IO)
    c.key(5.3, sweep0, E_IO)
    c.key(5.9, sweep1, E_IO)
    c.key(6.5, sweep0, E_IO)
    c.key(7.1, sweep1, E_IO)
    c.key(LT, hive, E_IO)
    # The bellows squeezed twice while he brushes; a slow breath under it all.
    c.layer("bellows", lambda t: [-12 * max(0, math.sin((t - 5.3) * math.pi * 2 / 0.9)) if 5.3 < t < 7.1 else 0, 0, 0])
    c.layer("@root", lambda t: [0, -0.06 * math.sin(t * 2 * math.pi * 2 / LT), 0])

    # --- talk: weight easy, the right hand turning palm-up as he explains, the left now and then opening at his side, a
    # nod, the beard moving with the words. ---
    LK = 4.0
    c = clip("talk", LK, loop=True)
    t0 = P_(calm, CURL_R, {"right_arm": [-24, 0, 8], "right_forearm": [-62, 0, 0], "right_hand": [0, -30, 0], "head": [2, 4, 0]})
    t1 = P_(calm, OPEN_R, {"right_arm": [-34, 12, 14], "right_forearm": [-74, 0, 0], "right_hand": [-6, -84, 0], "head": [-4, 10, 4],
                           "body": [0, 4, 0], "@brow_r": [0, 0.2, 0], "@brow_l": [0, 0.2, 0]})
    t2 = P_(calm, CURL_R, OPEN_L, {"right_arm": [-18, -4, 8], "right_forearm": [-52, 0, 0], "right_hand": [10, -20, 0],
                                   "left_arm": [-20, -8, -14], "left_forearm": [-48, 0, 0], "left_hand": [0, 70, 0], "head": [6, -6, -2],
                                   "body": [0, -3, 0]})
    t3 = P_(t1, {"head": [8, 6, 0], "right_arm": [-30, 10, 12]})
    for tm, g in ((0, t0), (1.0, t1), (2.0, t2), (3.0, t3), (LK, t0)):
        c.key(tm, g, E_IO if tm else None)
    c.layer("@mouth", lambda t: [0, 0, -0.3 * max(0, math.sin(t * 2 * math.pi * 7 / LK))])
    c.layer("@beard", lambda t: [0, -0.2 * max(0, math.sin(t * 2 * math.pi * 7 / LK)), 0])
    c.layer("body", lambda t: [0.6 * math.sin(t * 2 * math.pi / LK), 0, 0])

    done(clips)
    return f, clips


# =====================================================================================================
# Sprites and the bust
# =====================================================================================================

def honey_icon():
    """Cain's Honey: a squat glass jar of dark amber honey, a cloth cap tied with twine, a red wax seal with his mark."""
    O = hexc("#1c1006")
    pal = {"o": O, "g": hexc("#cfe0d8"), "G": hexc("#f4fbf7"), "h": HONEY[2], "H": HONEY[3], "y": HONEY[4], "Y": HONEY[5],
           "d": HONEY[1], "D": HONEY[0], "c": KHAKI[4], "C": KHAKI[5], "k": KHAKI[2], "t": hexc("#c9b48a"),
           "r": BLOOD[3], "R": BLOOD[5], "w": hexc("#ff7a5a"), "q": BLOOD[1]}
    return art([
        "................",
        "....oooooooo....",
        "...occCCCCcco...",
        "...okcccccckot..",
        "...ottttttttot..",
        "....ogggggggo...",
        "...oGHYyyyHHdo..",
        "..oGHYyHHHHhdDo.",
        "..oGyHHrrrRhdDo.",
        "..ogyHrRwRrrhDo.",
        "..ogHHrqRRrqhDo.",
        "..ogHhhrrrqhhDo.",
        "..oghhhhhhhhdDo.",
        "...oghhdddddDo..",
        "....oooooooooo..",
        "................",
    ], pal)


ADDRESS_FRAMES = 32
_PAPER = {"o": hexc("#241a10"), "c": hexc("#efe4c2"), "C": hexc("#e2d4ab"), "q": hexc("#c4b285"), "Q": hexc("#a8956a"),
          "b": hexc("#cdbb8c"), "B": hexc("#b5a173"), "i": hexc("#3b3631"), "I": hexc("#6a625a"),
          "y": HONEY[4], "Y": HONEY[5], "h": HONEY[3], "d": HONEY[2], "D": HONEY[1]}
_ADDRESS = [
    "................",
    ".........DDD....",
    "..oooooooDYyDoo.",
    ".ocIcccIcdhyhdo.",
    ".ociiIiicchdcco.",
    ".occIcccIcydcco.",
    ".ociiiciicYhcco.",
    ".occccccccdDcco.",
    ".oCQQQQQQQQQQCo.",
    ".oCCCICCCCICCCo.",
    ".oCiiiiCIiiiCCo.",
    ".oBbCCCCCCCCCCo.",
    ".oBBbCCCiIiiCCo.",
    ".ooBBbqCqCCqCo..",
    "...ooooooooooo..",
    "................",
]


def address_icon():
    """Cain's address: a scrap of yellowed notepaper folded once (the crease across the middle, the bottom-left corner
    dog-eared), three lines of a hasty pencil scrawl, and a smear of his honey run down from the top-right corner."""
    t = art(_ADDRESS, _PAPER, seed=22950)
    return t


def address_frame(frame):
    """One of the scrap's 32 compass frames (the vanilla `angle` property, like letters_compass and Rowena's card): the
    same scrap, its scrawl gathered into one pencil stroke that turns like a needle, a bead of honey at its head and a
    faint graphite tail behind; the honey smear and the dog-ear stay put. Frame 16 points straight ahead (up), 24
    right, 0 down, as vanilla's compass frames do."""
    rows = [r.replace("i", "c" if k < 8 else "C").replace("I", "c" if k < 8 else "C") for k, r in enumerate(_ADDRESS)]
    t = art(rows, _PAPER, seed=22950 + frame)
    keep = {(x, y) for y, r in enumerate(_ADDRESS) for x, ch in enumerate(r) if ch in "oyYhdDbB"}
    cx, cy = 7.0, 8.0
    theta = (frame - 16) / ADDRESS_FRAMES * 2 * math.pi
    sx, sy = math.sin(theta), -math.cos(theta)
    lead, head, faded = _PAPER["i"], HONEY[4], hexc("#9a917f")

    def put(x, y, c):
        p = (math.floor(x + 0.5), math.floor(y + 0.5))
        if 2 <= p[0] <= 13 and 3 <= p[1] <= 13 and p not in keep:
            t.set(p[0], p[1], c)

    for k in range(1, 5):
        put(cx - sx * k * 0.5, cy - sy * k * 0.5, faded)
    for k in range(1, 8):
        put(cx + sx * k * 0.5, cy + sy * k * 0.5, head if k >= 6 else lead)
    put(cx, cy, lead)
    return t


def bust():
    """The trophy: Cain's bust on the pedestal -- grey hair and beard, the flannel and the open vest, his right forearm raised
    across the chest with the Mark."""
    def tex16(fn):
        t = Tex(16, 16, 0)
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                if c is not None:
                    t.set(x, y, c)
        return t

    def skin(x, y):
        return tone(SKIN, 3.0 + 0.4 * math.sin(x * 0.9 + y * 0.6) - 0.5 * y / 15)

    def beard(x, y):
        return tone(BEARD, 2.9 + 0.6 * math.sin(x * 2.1 + y * 0.8) + (0.8 if (x * 5 + y * 3) % 11 == 0 else 0))
    rows = [   # x = 0 is the viewer's left
        "gggggggggggggggg",
        "gggggggggggggggg",
        "ggggssssssssgggg",
        "gsssssssssssssgg",
        "sssllssssssllsss",
        "sbBBBbssssbBBBbs",
        "suWIEsssssEIWuss",
        "sccuussssssuuccs",
        "sssssssnnsssssss",
        "bssssssnnssssssb",
        "bbssssNnnNssssbb",
        "bbbGGGGGGGGGGbbb",
        "bbbbGmmmmmmGbbbb",
        "bbbbbbMMMMbbbbbb",
        "bbbbbbbbbbbbbbbb",
        "bbbbbbbbbbbbbbbb",
    ]
    hair = lambda x, y: tone(GREY, 3.0 + 0.5 * math.sin(x * 1.7 + y * 0.3) + (0.5 if y < 2 else 0))
    legend = {"g": hair, "s": skin, "l": lambda x, y: shade(skin(x, y), 0.9), "b": beard, "B": GREY[3], "u": SKIN[2], "W": EYE_WHITE,
              "I": IRIS, "E": PUPIL, "c": lambda x, y: shade(skin(x, y), 0.86), "n": SKIN[4], "N": SKIN[1],
              "G": lambda x, y: shade(beard(x, y), 0.85), "m": LIPS[0], "M": mix(LIPS[1], BEARD[2], 0.5)}
    legend["b"] = beard

    def face(x, y):
        ch = rows[y][x]
        if ch == "b" and y < 9:
            return GREY[2]
        v = legend.get(ch)
        return v(x, y) if callable(v) else v
    fl = plaid(22901)
    face_t = tex16(face)
    hair_t = tex16(lambda x, y: hair(x, y))
    beard_t = tex16(beard)
    skin_t = tex16(skin)

    def chest(x, y):
        # The open vest either side, the flannel down the middle, the collar points at the top.
        d = abs(x - 7.5)
        if d < 4.2:
            c = fl("north", x, y, 16, 16)
            if d < 0.6 and y % 4 == 2:
                return hexc("#d6ccb4")
            if y < 3 and d > 1.0:
                return mix(c, FL_LINE, 0.15)
            return c
        return tone(CANVAS, 3.2 - y / 12 + 0.3 * math.sin(x * 1.2) + (0.6 if d < 5.0 else 0))
    chest_t = tex16(chest)
    vest_t = tex16(lambda x, y: tone(CANVAS, 3.0 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.4)))

    def arm(x, y):
        # The forearm (skin) across the chest, the Mark burning on it, the rolled flannel cuff at one end.
        if x >= 13:
            return fl("north", x, y, 16, 16)
        c = skin(x, y)
        mark = {(5, 6), (6, 6), (7, 6), (8, 7), (7, 8), (6, 9), (5, 10), (6, 10), (7, 10), (8, 10), (6, 11), (7, 12), (8, 13)}
        if (x, y) in mark:
            return hexc("#d0301c")
        if any((x + dx, y + dy) in mark for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            return mix(c, hexc("#8a2014"), 0.4)
        return c
    arm_t = tex16(arm)
    def side(x, y):
        # A side of the head with the front at x = 0: hair over the top and the back, a grey sideburn into the beard.
        if y < 4 or (x >= 10 and y < 13) or (y < 6 and x >= 4):
            return hair(x, y)
        if y >= 10 and x < 10 or (6 <= x <= 8 and y >= 6):
            return beard(x, y)
        return skin(x, y)
    side_t = tex16(side)
    for name, t in (("face", face_t), ("hair", hair_t), ("beard", beard_t), ("skin", skin_t), ("chest", chest_t), ("vest", vest_t),
                    ("arm", arm_t), ("side", side_t)):
        save(t, "block", f"cain_trophy_{name}")
    full = [0, 0, 16, 16]
    F, HR, BD, S, CH, V, AR, SD = "face", "hair", "beard", "skin", "chest", "vest", "arm", "side"
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": V}),
        # Broad shoulders in the open vest over the flannel.
        BM.cube("chest", (4.3, 3.4, 6.0), (11.7, 7.8, 10.0), {"north": CH, "*": V}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (3.4, 6.0, 6.2), (12.6, 8.0, 9.8), {"*": V}),
        BM.cube("neck", (6.8, 7.8, 6.8), (9.2, 8.6, 9.2), {"*": S}),
        # The right forearm laid across the chest, the Mark on it.
        BM.cube("forearm", (4.6, 4.2, 5.1), (10.6, 5.8, 6.1), {"north": AR, "up": AR, "*": S}, uv={"north": [0, 4, 16, 9],
                                                                                                    "up": [0, 4, 16, 6]}),
        BM.cube("fist", (10.4, 4.0, 5.0), (11.9, 6.0, 6.2), {"*": S}),
        # The head: the face north, hair round the sides and top, the beard below the jaw.
        BM.cube("head", (5.5, 8.4, 5.5), (10.5, 13.4, 10.5), {"north": F, "up": HR, "south": HR, "east": SD, "west": SD},
                uv={"north": full, "west": full, "east": [16, 0, 0, 16]}),
        BM.cube("beard", (5.6, 7.8, 5.25), (10.4, 9.0, 8.4), {"up": None, "*": BD}),
        BM.cube("ear_r", (5.2, 10.3, 7.6), (5.5, 11.6, 8.6), {"*": S}),
        BM.cube("ear_l", (10.5, 10.3, 7.6), (10.8, 11.6, 8.6), {"*": S}),
        BM.cube("nose", (7.6, 10.0, 5.2), (8.4, 11.1, 5.5), {"*": S}),
    ]
    mdl = BM.model({"base": "trophy_base", F: "cain_trophy_face", HR: "cain_trophy_hair", BD: "cain_trophy_beard", S: "cain_trophy_skin",
                    CH: "cain_trophy_chest", V: "cain_trophy_vest", AR: "cain_trophy_arm", SD: "cain_trophy_side"}, e, "trophy_base")
    BM.write("cain_trophy", mdl)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=22000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    save(honey_icon(), "item", "cains_honey")
    save(address_icon(), "item", "cains_address")
    for k in range(ADDRESS_FRAMES):
        save(address_frame(k), "item", f"cains_address_{k:02d}")
    bust()


if __name__ == "__main__":
    generate()
