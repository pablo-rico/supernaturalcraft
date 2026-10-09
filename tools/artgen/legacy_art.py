"""The Men of Letters (v0.17): Henry Winchester and the three monsters of their files -- GeckoLib rigs, textures, clips.

Names (creature ids, clips, toggled bones) are the contract in `legacy/LegacyAssets.java`; `LegacyAssetsTest` checks them.

  henry_winchester  Henry, as he walked out of 1958: a brown three-piece-cut suit, white shirt, a narrow burgundy tie
                    with a tie bar, a brown felt fedora with a black band (its own bone `hat`, so he can tip it), a
                    wristwatch, brown oxfords, and the Men of Letters' briefcase in his left hand (`briefcase`).
  vampire           a nest vampire: pale, long greasy black hair, dark-ringed eyes with a red glint, a black leather
                    jacket over a wine-dark shirt, black jeans, scuffed boots. `fangs` (toggled by `fangsOut()`): the
                    open mouth and the second row of teeth that slides down over the first (`fang_row`).
  werewolf          two forms, two top-level groups with their own limb chains (`human`: h_*, `wolf`: w_*):
                    `human`, a lean, unshaven man in a grey thermal henley, worn jeans and boots;
                    `wolf`, a hulking wolf-man -- a wolf's head with a jaw that opens, a heavy furred chest and arms,
                    long claws, digitigrade legs in what is left of the jeans, a tail; yellow eyes (glowmask).
  shapeshifter      a plain man nobody would look at twice (thinning hair, a tan zip jacket, a blue shirt, brown slacks).
                    `shed_skin` (shown once revealed): flaps of his borrowed skin peeling off his face, neck and hands,
                    the slime under them, and the silver flare in his eyes (glowmask).

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north), every man:
  root -- body -- head (-- hat)
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_thumb
               -- left_arm  -- left_forearm  -- left_hand  -- left_fingers, left_thumb
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from gabriel_art import open_shell
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, SHIRT, Clip, P_, fabric, tone, to_preview, world_point,
                         write_compact)
from pixelkit import Ramp, fbm, hexc, mix, shade
import horsemen_art as H

CLEAR = (0, 0, 0, 0)


# =====================================================================================================
# Proportions and the shared skeleton
# =====================================================================================================

class Build:
    """A man's proportions (Bedrock px)."""

    def __init__(self, hip=12.0, neck=24.0, tw=3.75, depth=4.0, aw=3.5, ax=5.6, sx=5.2, sy=23.0, elbow=18.0, wrist=13.6,
                 palm=11.6, lx=1.9, lw=3.6, knee=6.2, shoe_h=1.5):
        self.hip, self.neck, self.tw, self.depth = hip, neck, tw, depth
        self.aw, self.ax, self.sx, self.sy = aw, ax, sx, sy
        self.elbow, self.wrist, self.palm = elbow, wrist, palm
        self.lx, self.lw, self.knee, self.shoe_h = lx, lw, knee, shoe_h


SIDES = (("right", -1), ("left", 1))


def skeleton(m, b, p="", parent="root"):
    """The limb chain (bone names prefixed with `p`), parented to `parent`."""
    m.bone(p + "body", (0, b.hip, 0), parent)
    m.bone(p + "head", (0, b.neck, 0), p + "body")
    for side, s in SIDES:
        m.bone(f"{p}{side}_arm", (b.sx * s, b.sy, 0), p + "body")
        m.bone(f"{p}{side}_forearm", (b.ax * s, b.elbow, 0), f"{p}{side}_arm")
        m.bone(f"{p}{side}_hand", (b.ax * s, b.wrist, 0), f"{p}{side}_forearm")
        m.bone(f"{p}{side}_fingers", (b.ax * s, b.palm, 0), f"{p}{side}_hand")
        m.bone(f"{p}{side}_thumb", (b.ax * s - 1.0 * s, b.palm + 1.0, -1.3), f"{p}{side}_hand")
    m.bone(f"{p}right_index", (-b.ax, b.palm, -1.0), f"{p}right_hand")
    for side, s in SIDES:
        m.bone(f"{p}{side}_leg", (b.lx * s, b.hip, 0), parent)
        m.bone(f"{p}{side}_shin", (b.lx * s, b.knee, 0), f"{p}{side}_leg")


def head_cube(m, bone, y0, rows, legend, side_mat, glow=None, inflate=0.0):
    """An 8-px head at density 2: the north face drawn from 16 legend rows (x = 0 is his right)."""
    def mat(face, x, y, w, h):
        if face == "north":
            ch = rows[y][x] if y < len(rows) and x < len(rows[y]) else "s"
            v = legend.get(ch, legend["s"])
            return v(face, x, y, w, h) if callable(v) else v
        return side_mat(face, x, y, w, h)
    return m.cube(bone, (-4, y0, -4), (8, 8, 8), mat, glow, density=2, inflate=inflate, tag="head")


def fix(rows):
    return [(r + "s" * 16)[:16].replace(" ", "s") for r in rows]


def hands(m, b, skin, p="", glow=None, nails=None):
    """Palms, a block of fingers (the right index its own bone, to point), thumbs."""
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{p}{side}_hand", (cx - 1.3, b.palm, -1.45), (2.6, b.wrist - b.palm, 2.9), skin, glow, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(skin(f, x, y, w, h), 0.9 if (f in ("east", "west") and x % 2) else 1.0)
        if nails is not None:
            fing0 = fing
            fing = lambda f, x, y, w, h, fing0=fing0: nails if (f == "down" or (f != "up" and y == h - 1)) else fing0(f, x, y, w, h)
        if side == "right":
            m.cube(f"{p}right_index", (cx - 1.25, b.palm - 1.4, -1.4), (2.5, 1.4, 0.8), fing, glow, density=2, tag="index")
            m.cube(f"{p}right_fingers", (cx - 1.25, b.palm - 1.2, -0.6), (2.5, 1.2, 2.0), fing, glow, density=2, tag="fingers")
        else:
            m.cube(f"{p}left_fingers", (cx - 1.25, b.palm - 1.2, -1.4), (2.5, 1.2, 2.8), fing, glow, density=2, tag="fingers")
        m.cube(f"{p}{side}_thumb", (cx - 1.0 * s - 0.5, b.palm + 0.2, -2.05), (1.0, 1.5, 0.9), skin, glow, density=2, tag="thumb")


def ears_nose(m, bone, y0, skin_ramp, glow=None, nose_h=1.6, ears=True):
    m.cube(bone, (-0.6, y0 + 2.6, -4.55), (1.2, nose_h, 0.6), lambda f, x, y, w, h: skin_ramp[4] if f == "north" and y < 2 else
           (skin_ramp[1] if f == "down" else skin_ramp[2.6]), glow, density=2, tag="nose")
    for s in ((-1, 1) if ears else ()):
        m.cube(bone, (4 * s - 0.25 + (0 if s > 0 else -0.25), y0 + 2.6, -0.5), (0.5, 2.2, 1.5),
               lambda f, x, y, w, h: skin_ramp[2] if f not in ("east", "west") else skin_ramp[3], glow, density=2, tag="ear")


# --- materials ----------------------------------------------------------------------------------------

def worn(ramp, seed, base=3.0, amp=1.0, scuff=0.0, hem=True):
    """Hard-wearing cloth: soft top light, low-frequency wear, a darker hem, scuffed paler patches."""
    def m(face, x, y, w, h):
        if face == "up":
            return tone(ramp, base + 0.5)
        if face == "down":
            return tone(ramp, base - 1.2)
        n = fbm(x + len(face) * 7, y, seed, 64, 64, 3, 7.0)
        v = base + (n - 0.5) * amp * 1.6 - 0.35 * y / max(1, h)
        if scuff and fbm(x * 2 + 3, y * 2 + len(face), seed + 5, 64, 64, 2, 9.0) > 1 - scuff:
            v += 0.9
        if hem and y == h - 1:
            v -= 0.9
        return tone(ramp, v)
    return m


def denim_m(ramp, seed, fade=True, rips=0.0):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 3, y, seed, 64, 64, 3, 8.0)
        v = 2.6 + (n - 0.5) * 1.5 + (0.45 if (x - y) % 3 == 0 else 0)
        c = tone(ramp, v)
        if fade and face == "north" and 0.15 < y / max(1, h) < 0.55 and 2 <= x <= w - 3:
            c = mix(c, ramp[5], 0.22)
        if face in ("east", "west") and x == w // 2:
            c = shade(c, 1.15) if y % 2 else shade(c, 0.85)
        if rips and face == "north" and fbm(x * 3 + 1, y * 3, seed + 9, 64, 64, 2, 10.0) > 1 - rips:
            c = mix(hexc("#d9d2c4"), c, 0.35) if (x + y) % 2 else shade(c, 0.6)
        return c
    return m


def leather_m(ramp, seed, base=3.0, shine=True):
    def m(face, x, y, w, h):
        n = fbm(x * 3 + len(face), y * 3, seed, 64, 64, 2, 9.0)
        v = base + (n - 0.5) * 1.1 - 0.4 * y / max(1, h)
        if face == "up":
            v += 0.6
        if face == "down":
            v -= 1.0
        if shine and face == "north" and h01("lsh", seed, x, y) < 0.05:
            v += 1.2
        if face not in ("up", "down") and y == h - 1:
            v -= 0.7
        return tone(ramp, v)
    return m


def knit_m(ramp, seed, base=3.0, rib=False):
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 64, 64, 2, 12.0)
        v = base + (n - 0.5) * 0.8 + (0.3 if x % 2 == 0 else -0.1) - 0.3 * y / max(1, h)
        if rib or (face not in ("up", "down") and y >= h - 2):
            v -= 0.4 * (x % 2)
        return tone(ramp, v)
    return m


def hair_m(ramp, seed, base=2.6, comb=False):
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 11.0)
        strand = math.sin(x * 1.9 + n * 4) if f in ("up", "down") else math.sin(x * 1.6 + y * 0.35 + n * 5)
        v = base + (n - 0.5) * 0.7 + strand * 0.45
        if comb and f == "up" and 0.3 < x / max(1, w) < 0.55:
            v += 0.6
        return tone(ramp, v)
    return m


def shoe_m(ramp, seed, sole=None):
    """Leather shoes: a toe cap with a shine, laces on top, a dark welt."""
    sole = sole or ramp[0]

    def m(f, x, y, w, h):
        if f == "down":
            return sole
        if f != "up" and y >= h - 1:
            return sole
        if f == "up":
            fz = y / max(1, h - 1)
            if 0.45 < fz < 0.8 and 2 <= x <= w - 3 and y % 2 == 0:
                return ramp[1]
            if fz < 0.3 and abs(x - (w - 1) / 2) < 1.1:
                return ramp[5] if fz < 0.12 else ramp[4]
            return ramp[3]
        if f == "north":
            return ramp[4] if y == 0 and abs(x - (w - 1) / 2) < 1.5 else ramp[3]
        return ramp[3] if y else ramp[4]
    return m


def boot_m(ramp, seed):
    def m(f, x, y, w, h):
        if f == "down":
            return ramp[0]
        if f != "up" and y >= h - 1:
            return ramp[0]
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 8.0)
        v = 2.8 + (n - 0.5) * 1.0 + (0.6 if f == "up" else 0)
        if h01("scuff", seed, f, x, y) < 0.07:
            v += 1.1
        return tone(ramp, v)
    return m


def legs(m, b, p, trousers, shoe, shoe_m_=None, cuff=None, glow=None, shoe_len=4.8, shoe_h=None):
    shoe_h = b.shoe_h if shoe_h is None else shoe_h
    for side, s in SIDES:
        x0 = b.lx * s - b.lw / 2
        tr = trousers(side) if callable(trousers) and trousers.__code__.co_argcount == 1 else trousers
        m.cube(f"{p}{side}_leg", (x0, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), tr, glow, density=2, tag="thigh")
        m.cube(f"{p}{side}_shin", (x0, shoe_h, -b.lw / 2), (b.lw, b.knee - shoe_h, b.lw), tr, glow, density=2, tag="shin")
        if cuff is not None:
            m.cube(f"{p}{side}_shin", (x0, shoe_h, -b.lw / 2), (b.lw, 0.8, b.lw), none_on(("up", "down"), cuff), glow, inflate=0.18,
                   density=2, tag="cuff")
        m.cube(f"{p}{side}_shin", (x0 - 0.1, 0, -b.lw / 2 - (shoe_len - b.lw) * 0.75), (b.lw + 0.2, shoe_h, shoe_len), shoe, glow,
               density=2, tag="shoe")


# =====================================================================================================
# Pose helpers
# =====================================================================================================

_REACH = {}


def reach(r, pose, side, target, grip, p=""):
    """Arm and forearm angles (merged into `pose`) that bring `side`'s hand point `grip` (rest space) to world `target`:
    coordinate descent from the pose's own angles."""
    arm, fore, hand = f"{p}{side}_arm", f"{p}{side}_forearm", f"{p}{side}_hand"
    names = [(arm, 0), (arm, 1), (arm, 2), (fore, 0), (fore, 1)]
    key = (side, p, target, grip, repr(sorted((k, tuple(v)) for k, v in pose.items())))
    if key in _REACH:
        return P_(pose, _REACH[key])
    cur = {n: list(pose.get(n, [0, 0, 0])) for n, _ in names}

    def err():
        q = world_point(r, to_preview(P_(pose, cur)), hand, grip)
        return sum((q[i] - target[i]) ** 2 for i in range(3)) + 1e-5 * sum(abs(v) for vv in cur.values() for v in vv)
    best = err()
    for step in (16, 8, 4, 2, 1, 0.5):
        improved = True
        while improved:
            improved = False
            for n, i in names:
                for d in (step, -step):
                    cur[n][i] += d
                    e = err()
                    if e < best - 1e-9:
                        best, improved = e, True
                    else:
                        cur[n][i] -= d
    _REACH[key] = {n: [round(v, 1) for v in vv] for n, vv in cur.items()}
    return P_(pose, _REACH[key])


def walk_cycle(n, i, stride=26, arm=14, knee=32, bob=0.4, sway=3, lean=1.5, p=""):
    ph = 2 * math.pi * i / n
    sn = math.sin(ph)
    return {f"{p}right_leg": [-stride * sn, 0, 0], f"{p}left_leg": [stride * sn, 0, 0],
            f"{p}right_shin": [knee * max(0, math.sin(ph + 1.2)), 0, 0], f"{p}left_shin": [knee * max(0, -math.sin(ph + 1.2)), 0, 0],
            f"{p}right_arm": [arm * sn, 0, 3], f"{p}left_arm": [-arm * sn, 0, -3], f"{p}right_forearm": [-8, 0, 0],
            f"{p}left_forearm": [-8, 0, 0], f"{p}body": [lean, sway * sn, 0], f"{p}head": [0, -sway * sn, 0],
            "@" + (p + "body" if p else "root"): [0, bob * abs(math.cos(ph)), 0]}


def pre(pose, p):
    """A pose's bone names with prefix `p` (the werewolf's two chains)."""
    out = {}
    for k, v in pose.items():
        if k[0] in "@%":
            out[k[0] + p + k[1:]] = v
        else:
            out[p + k] = v
    return out


FIST_R = {"right_fingers": [-80, 0, 0], "right_index": [-80, 0, 0], "right_thumb": [-20, 0, 20]}
FIST_L = {"left_fingers": [-80, 0, 0], "left_thumb": [-20, 0, -20]}
OPEN_R = {"right_fingers": [0, 0, 0], "right_index": [0, 0, 0], "right_thumb": [0, 0, -12]}
OPEN_L = {"left_fingers": [0, 0, 0], "left_thumb": [0, 0, 12]}
POINT_R = {"right_fingers": [-85, 0, 0], "right_index": [0, 0, 0], "right_thumb": [-30, 0, 20]}
CLAW_R = {"right_fingers": [-40, 0, 0], "right_index": [-40, 0, 0], "right_thumb": [0, 0, -20]}
CLAW_L = {"left_fingers": [-40, 0, 0], "left_thumb": [0, 0, 20]}
LOOSE = {"right_fingers": [-22, 0, 0], "right_index": [-18, 0, 0], "left_fingers": [-22, 0, 0], "right_arm": [0, 0, 3],
         "left_arm": [0, 0, -3]}


def done(clips):
    for cl in clips:
        cl.done(step=0.05)


# =====================================================================================================
# Henry Winchester
# =====================================================================================================

HB = Build()
H_SKIN = Ramp("#5e3d2e", "#8a6250", "#ad8068", "#c4977d", "#d6ad93", "#e6c4ab")
H_HAIR = Ramp("#0e0907", "#18110c", "#241a12", "#30231a", "#3e2e22", "#4e3b2c")
H_EYE = hexc("#4b6a86")
H_LIPS = Ramp("#5a2f27", "#7d453a", "#985a4c", "#ab6c5c", "#bf8070", "#d39684")
SUIT = Ramp("#17130f", "#241e18", "#342b23", "#453a2f", "#574a3c", "#6b5c4b")
TIE = Ramp("#1c080b", "#2f0d12", "#46141b", "#5d1c24", "#76262f", "#90333c")
HAT = Ramp("#120d09", "#1f1710", "#2e2218", "#3e2e20", "#4f3c2a", "#634c36")
BAND = Ramp("#050404", "#0b0909", "#131011", "#1c1819", "#262122", "#332d2e")
OXFORD = Ramp("#120a05", "#22130a", "#341e10", "#482a17", "#5e381f", "#7a4c2c")
CASE = Ramp("#1c0e06", "#33190b", "#4c2711", "#663618", "#7f4721", "#9a5a2d")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
STEEL = Ramp("#2a2f36", "#454c56", "#68717d", "#8f98a3", "#b8c0c9", "#e4e9ee")
EYE_WHITE = hexc("#e6ddd2")

HENRY_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhssssssssssshh",
    "ssssssssssssssss",
    "sbbbBsssssBbbbss",
    "sswEEsssssEEwsss",
    "ssuuusssssuuusss",
    "sssssssnnsssssss",
    "ssssssnNNnssssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "ssssssLLLLssssss",
    "jsssssssssssssj",
    "jjssssssssssssjj",
]


def henry_rig():
    m = Model("henry_winchester", 256, 256)
    m.rig.bone("root", (0, 0, 0))
    skeleton(m, HB)
    m.bone("hat", (0, 30.2, 0), "head")
    m.bone("briefcase", (HB.ax, HB.palm - 0.6, 0), "left_hand")
    m.bone("jacket_front_r", (-2.0, HB.hip, -2.0), "body")
    m.bone("jacket_front_l", (2.0, HB.hip, -2.0), "body")
    m.bone("jacket_back", (0, HB.hip, 2.1), "body")
    henry_head(m)
    henry_body(m)
    henry_hat(m)
    henry_case(m)
    return m


def henry_head(m):
    sk = H.skin_mat(H_SKIN, 17101, 3.15, 0.4)
    hr = hair_m(H_HAIR, 17102, 2.6, comb=True)
    legend = {"h": hr, "s": sk, "b": H_HAIR[1], "B": H_HAIR[2], "w": EYE_WHITE, "E": H_EYE, "u": H_SKIN[2], "n": H_SKIN[4],
              "N": H_SKIN[1], "m": mix(H_LIPS[1], H_SKIN[2], 0.35), "L": mix(H_LIPS[3], H_SKIN[3], 0.55), "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}
    sides = H.head_sides(sk, hr, top=5, back=13, temple=5, sideburn=8)
    head_cube(m, "head", HB.neck, fix(HENRY_FACE), legend, sides)
    ears_nose(m, "head", HB.neck, H_SKIN)
    m.cube("body", (-1.5, HB.neck - 0.6, -1.5), (3, 1, 3), sk, density=2, tag="neck")
    # Neatly combed, short at the back and sides, a side part on his left: a thin shell over the crown.
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 else None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if y < 1 + (fx > 5) * 2 else None
        if f == "up" and x == 10:
            return H_SKIN[2]                     # the part
        return hr(f, x, y, w, h)
    m.cube("head", (-4, HB.neck + 6.0, -4), (8, 2.0, 8), shell, inflate=0.25, density=2, tag="hair")


def henry_body(m):
    b = HB
    shirt = fabric(SHIRT, 17201, 3.6, 0.12)
    suit = fabric(SUIT, 17202, 3.0, 0.3)
    sk = H.skin_mat(H_SKIN, 17203, 3.1, 0.4)

    def tie(f, x, y, w, h):
        return TIE[4] if x == 7 or (y // 2 + x) % 5 == 0 else TIE[2]
    m.cube("body", (-b.tw, b.hip, -2), (2 * b.tw, b.neck - b.hip, 4), lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north" else SUIT[1],
           density=2, tag="torso")
    front = H.jacket_front(suit, SUIT[4], shirt, tie=tie, gap=(5, 10), v_depth=11, buttons=BRASS[1], button_rows=(13, 17, 21))
    m.cube("body", (-b.tw, b.hip - 0.2, -2), (2 * b.tw, b.neck - b.hip, 4), none_on(("down",), front), inflate=0.35, density=2,
           tag="jacket")
    m.cube("body", (-0.55, b.neck - 2.2, -2.62), (1.1, 1.0, 0.4), solid(TIE[3]), density=4, tag="tie_knot")
    m.cube("body", (-0.6, b.neck - 5.2, -2.66), (1.2, 0.25, 0.2), lambda f, x, y, w, h: BRASS[4] if x % 3 else BRASS[3], density=4,
           tag="tie_bar")
    for s in (-1, 1):
        m.cube("body", (0.9 * s - 0.75, b.neck - 1.3, -2.45), (1.5, 1.2, 0.4), lambda f, x, y, w, h: SHIRT[5] if y == 0 else SHIRT[4],
               density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.9 * s, b.neck - 0.2, -2.3))
        # Notch lapels laid back over the chest.
        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return SUIT[1]
            c = tone(SUIT, 4.0 - 1.1 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = SUIT[2]
            return c
        m.cube("body", (2.4 * s - 1.1, 17.6, -2.72), (2.2, 6.2, 0.4), lapel, density=2, tag="lapel", rotation=(3, 0, 12 * s),
               pivot=(1.6 * s, 17.6, -2.6))
        m.cube("body", (3.2 * s - 0.7, 22.6, -2.76), (1.4, 1.1, 0.4), solid(SUIT[4]), density=2, tag="lapel_notch",
               rotation=(0, 0, -35 * s), pivot=(3.2 * s, 22.6, -2.6))
        # Flap pockets low on the jacket.
        m.cube("body", (2.2 * s - 1.2, 13.0, -2.62), (2.4, 0.6, 0.3), lambda f, x, y, w, h: SUIT[4] if y == 0 else SUIT[2],
               density=4, tag="pocket_flap")
    # The breast pocket with a white square folded in points.
    m.cube("body", (1.3, 19.6, -2.6), (2.0, 0.35, 0.25), solid(SUIT[1]), density=4, tag="breast_pocket")
    m.cube("body", (1.5, 19.9, -2.66), (1.6, 0.6, 0.25), lambda f, x, y, w, h: SHIRT[5] if (x + y) % 3 else SHIRT[3], density=4,
           tag="pocket_square")
    # The jacket's skirt below the waist, two fronts and a back with a vent, swinging with the legs.
    L = 2.6
    for bone, s in (("jacket_front_r", -1), ("jacket_front_l", 1)):
        def fr(f, x, y, w, h, s=s):
            if f in ("up",):
                return None
            if f == ("west" if s < 0 else "east") or f == "south":
                return None
            if f == "north" and y >= h - 2 and ((s < 0 and x >= w - 2) or (s > 0 and x <= 1)):
                return None                       # the cutaway at the front
            return suit(f, x, y + 10, w, h + 10)
        m.cube(bone, ((0.0 if s > 0 else -b.tw), b.hip - L, -2), (b.tw, L, 4.0), fr, inflate=0.35, density=2, tag="jacket_skirt")
    def bk(f, x, y, w, h):
        if f not in ("north", "south"):
            return None if f in ("up", "east", "west") else SUIT[1]
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > 1:
            return SUIT[0]
        return suit(f, x, y + 10, w, h + 10)
    m.cube("jacket_back", (-b.tw, b.hip - L, 2.0), (2 * b.tw, L, 0.3), bk, inflate=0.35, density=2, tag="jacket_back")
    # Sleeves, the shirt cuffs, a wristwatch on the left wrist, the hands.
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow, b.aw), suit, inflate=0.2, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.65, b.wrist + 0.4, -1.65), (3.3, b.elbow - b.wrist - 0.4, 3.3), none_on(("up",), suit),
               inflate=0.2, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.45, b.wrist - 0.1, -1.45), (2.9, 0.6, 2.9), none_on(("up",), shirt), inflate=0.1, density=2,
               tag="shirt_cuff")
        # Shoulder pads: a little square to the jacket's shoulder line.
        m.cube("body", (s * (b.tw + 0.2) - 0.9, b.neck - 1.1, -2.2), (1.8, 0.8, 4.4), lambda f, x, y, w, h: tone(SUIT, 3.8 if f == "up" else 2.8),
               density=2, tag="shoulder")
    m.cube("left_hand", (b.ax - 1.4, b.wrist - 0.7, -1.5), (2.8, 0.6, 3.0), lambda f, x, y, w, h: BRASS[4] if f == "east" and 1 <= y <= 0 else
           (STEEL[4] if f == "east" else hexc("#2a1a10")), inflate=0.12, density=4, tag="watch_strap")
    m.cube("left_hand", (b.ax + 1.45, b.wrist - 0.85, -0.55), (0.25, 0.9, 1.1), lambda f, x, y, w, h: hexc("#f2ead2") if f == "east" else BRASS[3],
           density=4, tag="watch_face")
    hands(m, b, H.skin_mat(H_SKIN, 17204, 3.2, 0.4))
    # Trousers with a pressed crease and turn-ups, brown oxfords.
    trousers = fabric(SUIT, 17210, 2.9, 0.2, crease=True)
    legs(m, b, "", trousers, shoe_m(OXFORD, 17211), cuff=lambda f, x, y, w, h: tone(SUIT, 2.4 if y else 3.4))


def henry_hat(m):
    """A brown felt fedora: a pinched crown with a teardrop crease, a black grosgrain band with a bow, the brim snapped down
    at the front."""
    hat = "hat"
    y0 = 30.2

    def felt(base):
        def f_(f, x, y, w, h):
            n = fbm(x * 2 + len(f), y * 2, 17301, 64, 64, 2, 10.0)
            v = base + (n - 0.5) * 0.5 + (0.5 if f == "up" else 0) - (0.3 if f in ("east", "west") else 0)
            return tone(HAT, v)
        return f_

    def crown(f, x, y, w, h):
        if f == "down":
            return None
        c = felt(3.0)(f, x, y, w, h)
        if f == "up":
            mid = (w - 1) / 2
            if abs(x - mid) < 1.0 and 2 <= y <= h - 3:
                return tone(HAT, 1.6)                  # the teardrop crease
            if abs(x - mid) < 2.0 and 2 <= y <= h - 3:
                return tone(HAT, 2.3)
        if f == "north" and y < 3 and (abs(x - 4) < 1.2 or abs(x - (w - 5)) < 1.2):
            return shade(c, 0.86)                      # the front pinch
        return c
    m.cube(hat, (-4.3, y0, -4.3), (8.6, 3.4, 8.6), crown, density=2, tag="hat_crown")
    m.cube(hat, (-3.6, y0 + 3.4, -3.4), (7.2, 0.4, 6.8), none_on(("down",), felt(3.5)), density=2, tag="hat_top")
    # The band and its bow on his left.
    m.cube(hat, (-4.35, y0 + 0.1, -4.35), (8.7, 0.9, 8.7), none_on(("up", "down"), lambda f, x, y, w, h: BAND[3 if y == 0 else 2]),
           density=2, tag="hat_band")
    m.cube(hat, (4.3, y0 + 0.2, 0.6), (0.3, 0.8, 1.6), solid(BAND[3]), density=4, tag="hat_bow")
    # The brim: flat at the sides and back, curled up a little at the edges, snapped down over his brow.
    brim = lambda f, x, y, w, h: tone(HAT, 1.6) if f == "down" else (tone(HAT, 3.4 if f == "up" else 2.6))
    m.cube(hat, (-5.4, y0 - 0.25, -4.4), (10.8, 0.45, 9.6), brim, density=2, tag="hat_brim")
    m.cube(hat, (-4.6, y0 - 0.25, -6.3), (9.2, 0.45, 2.0), brim, density=2, tag="hat_brim_front", rotation=(14, 0, 0),
           pivot=(0, y0, -4.4))
    m.cube(hat, (-4.2, y0 - 0.25, 5.2), (8.4, 0.45, 1.2), brim, density=2, tag="hat_brim_back", rotation=(-12, 0, 0),
           pivot=(0, y0, 5.2))
    for s in (-1, 1):
        m.cube(hat, ((5.4 if s > 0 else -6.1), y0 - 0.2, -3.6), (0.7, 0.45, 7.4), brim, density=2, tag="hat_brim_side",
               rotation=(0, 0, 18 * s), pivot=(5.4 * s, y0, 0))


def henry_case(m):
    """The Men of Letters' briefcase hanging from his left hand: oxblood leather over a stiff frame, stitched edges, brass
    corners and clasps, a leather handle, and the order's emblem pressed in gold on its side."""
    b = HB
    cx = b.ax
    top = b.palm - 1.0
    leather = leather_m(CASE, 17401, 3.0)

    def body(f, x, y, w, h):
        c = leather(f, x, y, w, h)
        edge = x == 0 or x == w - 1 or y == 0 or y == h - 1
        if f in ("east", "west"):
            if (x in (1, w - 2) or y in (1, h - 2)) and (x + y) % 2 == 0:
                return CASE[5]                     # stitching
            # The emblem: a little gold eye in a star, middle of the side.
            ex, ey = (w - 1) / 2, (h - 1) / 2 + 0.5
            d = math.hypot(x - ex, (y - ey) * 1.2)
            if 1.6 < d < 2.4 and (int(math.degrees(math.atan2(y - ey, x - ex)) + 360) // 45) % 2 == 0:
                return BRASS[4]
            if d < 0.9:
                return BRASS[5]
        if edge:
            c = shade(c, 0.8)
        return c
    m.cube("briefcase", (cx - 0.9, top - 5.4, -3.6), (1.8, 5.0, 7.2), body, density=2, tag="case")
    # The frame's lip along the top, brass corners, two clasps and the handle.
    m.cube("briefcase", (cx - 1.0, top - 0.6, -3.7), (2.0, 0.4, 7.4), lambda f, x, y, w, h: CASE[1], density=2, tag="case_lip")
    for z in (-3.7, 3.2):
        for y in (top - 5.5, top - 1.0):
            m.cube("briefcase", (cx - 1.0, y, z), (2.0, 0.5, 0.5), lambda f, x, y_, w, h: BRASS[4 if f == "up" else 3], density=4,
                   tag="case_corner")
    for z in (-2.4, 1.6):
        m.cube("briefcase", (cx - 1.05, top - 1.5, z), (2.1, 0.9, 0.8), lambda f, x, y, w, h: BRASS[5] if y == 0 else BRASS[3],
               density=4, tag="case_clasp")
    m.cube("briefcase", (cx - 0.35, top - 0.2, -1.4), (0.7, 0.4, 2.8), lambda f, x, y, w, h: CASE[2], density=4, tag="handle")
    for z in (-1.6, 1.2):
        m.cube("briefcase", (cx - 0.35, top - 0.6, z), (0.7, 0.8, 0.4), solid(BRASS[3]), density=4, tag="handle_loop")


# --- Henry's clips ------------------------------------------------------------------------------------

def henry_anims():
    m = henry_rig()
    r = m.rig
    f = AnimFile()
    clips = []
    A = "animation.henry_winchester."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def K(c, t, pose, ease=None):
        c.key(t, jacket_follow(pose), ease)

    # The case hangs plumb from his hand whatever the arm does (a pendulum lag added in the walk).
    stand = P_(LOOSE, {"left_fingers": [-70, 0, 0], "left_thumb": [-20, 0, -20], "left_arm": [0, 0, -6], "left_forearm": [-4, 0, 0]})

    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        K(c, t, P_(stand, {"body": [0.6 * abs(k), 0, 0], "@body": [0, -0.12 * abs(k), 0], "head": [-1 * abs(k), 7 * k, 0],
                           "right_arm": [-2 * abs(k), 0, 3], "right_fingers": [-22 - 20 * max(0, k), 0, 0]}), E_IO if t else None)
    c.layer("briefcase", lambda t: [0, 0, 1.0 * math.sin(t * 2 * math.pi / 4.0)])

    L = 1.2
    c = clip("walk", L, loop=True)
    for i in range(9):
        t = L * i / 8
        w = walk_cycle(8, i, stride=24, arm=16, knee=30)
        w["left_arm"] = [w["left_arm"][0] * 0.4, 0, -6]
        K(c, t, P_(stand, w), E_IO if i else None)
    c.layer("briefcase", lambda t: [-10 * math.sin(2 * math.pi * t / L - 0.9), 0, 2 * math.sin(4 * math.pi * t / L)])

    # talk: an easy explaining hand -- the right forearm up, palm open, beats with the words; a nod, a tilt.
    c = clip("talk", 3.0, loop=True)
    g0 = {"right_arm": [-20, 0, 8], "right_forearm": [-62, 0, 0], "right_hand": [0, -30, 0]}
    g1 = {"right_arm": [-26, 10, 12], "right_forearm": [-78, 0, 0], "right_hand": [-8, -60, 0]}
    g2 = {"right_arm": [-14, -4, 6], "right_forearm": [-54, 0, 0], "right_hand": [10, -20, 0]}
    for t, g, hd in ((0, g0, [2, 6, 0]), (0.7, g1, [-3, -4, 2]), (1.3, g2, [3, 10, -2]), (2.1, g1, [-1, 2, 0]), (3.0, g0, [2, 6, 0])):
        K(c, t, P_(stand, OPEN_R, g, {"head": hd, "body": [1, 4, 0]}), E_IO if t else None)
    c.layer("head", lambda t: [2.5 * math.sin(t * 2 * math.pi * 3 / 3.0), 0, 0])

    # tip_hat: two fingers to the brim, the hat lifted and tilted forward an inch, a small bow; back on.
    c = clip("tip_hat", 1.4)
    brim = world_point(r, {}, "hat", (0, 30.0, -6.2))
    up = reach(r, P_(stand, {"right_hand": [0, 0, -20]}), "right", (brim[0] - 3.2, brim[1] + 0.9, brim[2] + 0.6), (-HB.ax, HB.palm - 0.6, -1.0))
    tipped = P_(up, POINT_R, {"hat": [-16, 0, 6], "@hat": [0.6, 1.4, -0.6], "head": [6, 0, 0], "body": [6, 0, 0]})
    lifted = reach(r, P_(tipped, {"head": [6, 0, 0], "body": [6, 0, 0]}), "right",
                   (brim[0] - 3.2, brim[1] + 2.3, brim[2] + 0.2), (-HB.ax, HB.palm - 0.6, -1.0))
    lifted = P_(lifted, POINT_R, {"hat": [-16, 0, 6], "@hat": [0.6, 1.4, -0.6]})
    K(c, 0, stand)
    K(c, 0.35, P_(up, POINT_R), E_OUT)
    K(c, 0.6, lifted, E_IO)
    K(c, 0.85, P_(lifted, {"head": [10, 0, 0], "body": [9, 0, 0]}), E_IO)
    K(c, 1.05, P_(up, POINT_R), E_IO)
    K(c, 1.4, stand, E_IO)

    # point_map: he leans over the map table and lays a finger on a spot, looks up at the hunter, taps it twice.
    c = clip("point_map", 2.4)
    lean = P_(stand, {"body": [24, 0, 0], "head": [10, 0, 0], "right_leg": [-6, 0, 0], "left_leg": [8, 0, 0], "@root": [0, -0.3, 0.5]})
    spot = (-2.0, 15.5, -9.0)
    point = P_(reach(r, lean, "right", spot, (-HB.ax, HB.palm - 1.4, -1.0)), POINT_R, {"right_hand": [-20, 0, 0]})
    K(c, 0, stand)
    K(c, 0.45, point, E_IO)
    K(c, 0.9, P_(point, {"head": [-14, 0, 0]}), E_IO)
    K(c, 1.3, point, E_IO)
    K(c, 1.45, P_(point, {"right_hand": [-34, 0, 0]}), E_SNAP)
    K(c, 1.6, point, E_OUT)
    K(c, 1.75, P_(point, {"right_hand": [-34, 0, 0]}), E_SNAP)
    K(c, 1.9, point, E_OUT)
    K(c, 2.4, stand, E_IO)

    done(clips)
    return f, clips


def jacket_follow(pose, k=0.5):
    """The jacket's skirt swings with the legs."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])[0]
    out["jacket_front_r"] = [min(0.0, rl) * k - body * 0.3, 0, 0]
    out["jacket_front_l"] = [min(0.0, ll) * k - body * 0.3, 0, 0]
    out["jacket_back"] = [-max(0.0, max(rl, ll)) * 0.3 - body * 0.3, 0, 0]
    return out


# =====================================================================================================
# The vampire
# =====================================================================================================

VB = Build(tw=3.5, aw=3.3, ax=5.4, sx=5.0)
V_SKIN = Ramp("#4e4a55", "#77727e", "#97939c", "#b0acb3", "#c6c2c7", "#d9d6d9")
V_HAIR = Ramp("#050405", "#0b090b", "#121012", "#1b181b", "#252125", "#332e33")
V_RED = hexc("#9c1a1e")
V_IRIS = hexc("#2a0c0e")
V_RING = Ramp("#3e2f3e", "#4f3d4f", "#5f4b5d", "#6e5a6b", "#7d6979", "#8c7987")
JACKET = Ramp("#050506", "#0b0b0d", "#131316", "#1c1c21", "#28282e", "#3a3a42")
WINE = Ramp("#14060a", "#220a10", "#320f17", "#43141f", "#551a28", "#6a2232")
BLACK_DENIM = Ramp("#0a0a0c", "#121215", "#1a1a1e", "#232328", "#2e2e34", "#3b3b43")
V_BOOT = Ramp("#060505", "#0e0c0b", "#171412", "#211d1a", "#2d2824", "#3c3530")
MOUTH = Ramp("#120203", "#2a0507", "#45090c", "#611013", "#7d1a1c", "#9a2a2a")
FANG = Ramp("#8a8476", "#a9a393", "#c4bfae", "#d9d4c4", "#ebe7da", "#fbf9f0")

VAMPIRE_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhshhhhhhhh",
    "hhhsssssssssshhh",
    "hhsssssssssssshh",
    "hhBBBssssssBBBhh",
    "hhwREssssssERwhh",
    "hhkkkssssssskkhh",
    "hsssssssnsssssshh",
    "hssssssnNNsssssh",
    "hsssssssssssssshh",
    "hssssmmmmmmmssshh",
    "hhssssslllssssshh",
    "hhssssssssssssh",
    "hhjsssssssssjhhh",
]


def vampire_rig():
    m = Model("vampire", 256, 256)
    m.rig.bone("root", (0, 0, 0))
    b = VB
    skeleton(m, b)
    m.bone("hair_back", (0, b.neck + 5.0, 3.0), "head")
    m.bone("fangs", (0, b.neck + 1.7, -4.0), "head")
    m.bone("fang_row", (0, b.neck + 2.15, -4.1), "fangs")
    m.bone("jacket_front_r", (-2.0, b.hip + 1.0, -2.0), "body")
    m.bone("jacket_front_l", (2.0, b.hip + 1.0, -2.0), "body")
    sk = H.skin_mat(V_SKIN, 17501, 3.1, 0.35)
    hr = hair_m(V_HAIR, 17502, 2.5)

    def eye_glow(f, x, y, w, h, c):
        if c == V_RED:
            return hexc("#7a1014")
        return None
    legend = {"h": hr, "s": sk, "B": V_HAIR[2], "w": hexc("#d7d0cc"), "R": V_RED, "E": V_IRIS, "k": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), V_RING[1], 0.55),
              "n": V_SKIN[4], "N": V_SKIN[1], "m": mix(WINE[2], V_SKIN[2], 0.3), "l": mix(WINE[3], V_SKIN[3], 0.5),
              "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.88)}
    sides = H.head_sides(sk, hr, top=6, back=16, temple=2, sideburn=12)
    head_cube(m, "head", b.neck, fix(VAMPIRE_FACE), legend, sides, glow=eye_glow)
    ears_nose(m, "head", b.neck, V_SKIN, nose_h=1.8, ears=False)
    m.cube("body", (-1.4, b.neck - 0.6, -1.4), (2.8, 1, 2.8), sk, density=2, tag="neck")
    # Long, lank hair: a shell over the crown, curtains either side of the face, a fall down the back to the shoulders.
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 else None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, b.neck + 6.0, -4), (8, 2.0, 8), shell, inflate=0.35, density=2, tag="hair")
    for s in (-1, 1):
        lock = lambda f, x, y, w, h: None if f == "up" else (CLEAR if f != "down" and y == h - 1 and (x * 3 + s) % 4 == 0 else hr(f, x, y, w, h))
        m.cube("head", (s * 4.1 - 0.6, b.neck - 1.2, -3.9), (1.2, 7.4, 3.4), lock, inflate=0.1, density=2, tag="hair_lock")
    def fall(f, x, y, w, h):
        if f in ("up", "north"):
            return None
        if f != "down" and y >= h - 2 and (x * 5) % 3 == 0:
            return CLEAR
        return hr(f, x, y, w, h)
    m.cube("hair_back", (-4.2, b.neck - 3.5, 2.6), (8.4, 11.0, 1.8), fall, inflate=0.1, density=2, tag="hair_fall")
    # The fangs: the open mouth and, above it, the second row that slides down (fang_row).
    def mouth(f, x, y, w, h):
        if y == 0:
            return MOUTH[3]
        if y == h - 1:
            return mix(WINE[3], V_SKIN[3], 0.4)
        return MOUTH[0] if 2 <= x <= w - 3 else MOUTH[1]
    m.cube("fangs", (-1.75, b.neck + 1.0, -4.08), (3.5, 1.25, 0), mouth, lambda f, x, y, w, h, c: shade(c, 0.6), faces=("north",),
           density=4, tag="mouth")
    for i, x in enumerate((-1.5, -1.0, -0.5, 0.25, 0.75, 1.25)):
        tall = 0.75 if i in (0, 5) else 0.5
        m.cube("fang_row", (x, b.neck + 2.0 - tall, -4.2), (0.25, tall, 0.25),
               lambda f, x_, y, w, h: FANG[5] if y < h - 1 else FANG[3], density=4, tag="fang")
    # Clothes: a wine-dark shirt hanging out under an open black leather jacket, black jeans, boots.
    shirt = worn(WINE, 17510, 2.8, 0.6)
    leather = leather_m(JACKET, 17511, 2.9)

    def shirt_front(f, x, y, w, h):
        c = shirt(f, x, y, w, h)
        mid = (w - 1) / 2
        if f == "north" and y < 4 and abs(x - mid) < 1.5 - y * 0.3:
            return sk(f, x, y, w, h)              # the open collar
        if f == "north" and abs(x - mid) < 0.6 and y % 4 == 2:
            return WINE[1]
        return c
    m.cube("body", (-b.tw, b.hip - 1.2, -2), (2 * b.tw, b.neck - b.hip + 1.2, 4), shirt_front, density=2, tag="torso")
    m.cube("body", (-b.tw, b.hip + 1.0, -2), (2 * b.tw, b.neck - b.hip - 1.0, 4), open_shell(leather, 2.2, 3.2, 8, lapel=JACKET[4]),
           inflate=0.45, density=2, tag="jacket")
    def collar(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f in ("north", "up") and abs(x - mid) <= 2.8 and (f == "north" or y < 3):
            return None
        return tone(JACKET, 3.4 - y * 0.4)
    m.cube("body", (-b.tw, b.neck - 1.0, -2), (2 * b.tw, 1.8, 4), collar, inflate=0.8, density=2, tag="collar")
    for side, s in (("r", -1), ("l", 1)):
        def panel(f, x, y, w, h, s=s):
            if f in ("up", "south") or f == ("west" if s < 0 else "east"):
                return None
            return leather(f, x, y + 18, w, h + 18)
        m.cube(f"jacket_front_{side}", ((0.0 if s > 0 else -b.tw), b.hip - 0.2, -2), (b.tw, 1.2, 4.0), panel, inflate=0.45, density=2,
               tag="jacket_hem")
        # Zip teeth down the open edges.
        m.cube("body", (s * 2.6 - 0.15, b.hip + 1.0, -2.5), (0.3, 9.0, 0.2), lambda f, x, y, w, h: STEEL[3] if y % 2 else STEEL[1],
               density=4, tag="zip")
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow, b.aw), leather, inflate=0.25, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.6, b.wrist + 0.2, -1.6), (3.2, b.elbow - b.wrist - 0.2, 3.2), none_on(("up",), leather), inflate=0.25,
               density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.6, b.wrist + 0.2, -1.6), (3.2, 0.7, 3.2), none_on(("up", "down"), lambda f, x, y, w, h: JACKET[3]),
               inflate=0.35, density=2, tag="cuff")
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.sy - 0.4, -b.aw / 2), (b.aw, 1.2, b.aw), none_on(("down",), lambda f, x, y, w, h: JACKET[4] if f == "up" else JACKET[3]),
               inflate=0.4, density=2, tag="shoulder")
    hands(m, b, H.skin_mat(V_SKIN, 17520, 3.0, 0.4), nails=hexc("#3b3036"))
    legs(m, b, "", denim_m(BLACK_DENIM, 17530, fade=False, rips=0.04), boot_m(V_BOOT, 17531), shoe_h=2.2, shoe_len=4.6)
    for side, s in SIDES:
        m.cube(f"{side}_shin", (b.lx * s - b.lw / 2 - 0.1, 2.2, -b.lw / 2 - 0.1), (b.lw + 0.2, 1.2, b.lw + 0.2),
               none_on(("down",), lambda f, x, y, w, h: tone(V_BOOT, 2.6 + (0.6 if f == "up" else 0))), density=2, tag="boot_shaft")
    m.cube("body", (-b.tw, b.hip - 0.4, -2), (2 * b.tw, 0.8, 4), none_on(("up", "down"), lambda f, x, y, w, h: STEEL[4] if f == "north" and abs(x - (w - 1) / 2) < 1 else JACKET[2]),
           inflate=0.3, density=2, tag="belt")
    return m


def vampire_anims():
    m = vampire_rig()
    r = m.rig
    f = AnimFile()
    clips = []
    A = "animation.vampire."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def K(c, t, pose, ease=None):
        c.key(t, coat(pose), ease)

    hunch = P_(CLAW_R, CLAW_L, {"body": [7, 0, 0], "head": [-6, 0, 0], "right_arm": [-4, 0, 6], "left_arm": [-4, 0, -6],
                                "right_forearm": [-12, 0, 0], "left_forearm": [-12, 0, 0]})

    # idle: stillness of a predator; the head lifts to scent the air, turns, the fingers twitch.
    c = clip("idle", 3.0, loop=True)
    K(c, 0, hunch)
    K(c, 0.9, P_(hunch, {"head": [-16, 10, -4], "body": [5, 4, 0]}), E_IO)
    K(c, 1.6, P_(hunch, {"head": [-12, -14, 3], "body": [5, -4, 0]}), E_IO)
    K(c, 2.3, P_(hunch, {"head": [-4, -6, 0], "right_fingers": [-60, 0, 0], "right_index": [-60, 0, 0]}), E_IO)
    K(c, 3.0, hunch, E_IO)
    c.layer("hair_back", lambda t: [3 + 1.5 * math.sin(t * 2 * math.pi / 3.0), 0, 0])

    L = 1.1
    c = clip("walk", L, loop=True)
    for i in range(9):
        w = walk_cycle(8, i, stride=24, arm=10, knee=30, lean=9, sway=5)
        K(c, L * i / 8, P_(hunch, w, {"head": [-8, w["head"][1], 0]}), E_IO if i else None)
    c.layer("hair_back", lambda t: [5 + 3 * math.sin(4 * math.pi * t / L), 0, 0])

    L = 0.55
    c = clip("run", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        K(c, L * i / 8, P_(CLAW_R, CLAW_L, {"right_leg": [-56 * sn, 0, 0], "left_leg": [56 * sn, 0, 0],
                                             "right_shin": [80 * max(0.0, math.sin(ph + 1.1)), 0, 0],
                                             "left_shin": [80 * max(0.0, -math.sin(ph + 1.1)), 0, 0],
                                             "right_arm": [40 * sn + 20, 0, 10], "left_arm": [-40 * sn + 20, 0, -10],
                                             "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0],
                                             "body": [24, 5 * sn, 0], "head": [-22, -5 * sn, 0], "@root": [0, 1.0 * abs(cs) - 0.4, 0]}),
          E_IO if i else None)
    c.layer("hair_back", lambda t: [30 + 6 * math.sin(4 * math.pi * t / L), 0, 0])

    # bite: both hands lunge to grab a shoulder in front of him, the head darts in (0.45), holds, pulls back.
    c = clip("bite", 1.1)
    grab = reach(r, hunch, "right", (-3.5, 22.0, -9.0), (-VB.ax, VB.palm - 0.6, -0.5))
    grab = reach(r, grab, "left", (3.5, 21.0, -9.0), (VB.ax, VB.palm - 0.6, -0.5))
    lunge = P_(grab, OPEN_R, OPEN_L, {"body": [22, 0, 0], "@root": [0, -0.6, -2.0], "right_leg": [-26, 0, 0], "right_shin": [24, 0, 0],
                                      "left_leg": [16, 0, 0], "head": [-8, 0, 0]})
    bite = P_(lunge, CLAW_R, CLAW_L, {"head": [26, -24, 18], "body": [30, -6, 0]})
    K(c, 0, hunch)
    K(c, 0.3, lunge, E_OUT)
    K(c, 0.45, bite, E_SNAP)
    K(c, 0.75, P_(bite, {"head": [30, -26, 20]}), E_IO)
    K(c, 1.1, hunch, E_IO)

    # hiss: recoils into a crouch, shoulders up, arms spread with the claws out, the head thrust forward.
    c = clip("hiss", 1.0)
    hiss = P_(CLAW_R, CLAW_L, {"body": [16, 0, 0], "head": [-28, 0, 0], "@root": [0, -1.2, 0.8], "right_arm": [-30, 30, 50],
                               "left_arm": [-30, -30, -50], "right_forearm": [-40, 0, 0], "left_forearm": [-40, 0, 0],
                               "right_leg": [-20, 0, 6], "left_leg": [10, 0, -6], "right_shin": [26, 0, 0], "left_shin": [18, 0, 0]})
    K(c, 0, hunch)
    K(c, 0.18, hiss, E_SNAP)
    K(c, 0.7, P_(hiss, {"head": [-30, 6, 4]}), E_IO)
    K(c, 1.0, hunch, E_IO)
    c.layer("head", lambda t: [1.5 * math.sin(t * 70) if 0.2 < t < 0.7 else 0, 0, 0])

    # fangs_out: the head jerks back, the second row of teeth drops into place.
    c = clip("fangs_out", 0.6)
    K(c, 0, P_(hunch, {"%fang_row": [1, 0.2, 1], "@fang_row": [0, 0.6, 0]}))
    K(c, 0.12, P_(hunch, {"head": [-24, 0, 0], "%fang_row": [1, 0.3, 1], "@fang_row": [0, 0.5, 0]}), E_OUT)
    K(c, 0.3, P_(hunch, {"head": [-18, 0, 0], "%fang_row": [1, 1, 1], "@fang_row": [0, 0, 0]}), E_SNAP)
    K(c, 0.6, hunch, E_IO)

    # stunned (loop): dead man's blood -- knees sagging, swaying, arms hanging, the head lolling.
    c = clip("stunned", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        k = (-1, 0, 1, 0, -1)[i]
        K(c, t, {"body": [14, 8 * k, 6 * k], "head": [24 + 6 * abs(k), -10 * k, 14 * k], "@root": [0, -1.6, 0],
                 "right_leg": [-14, 0, 4], "left_leg": [-14, 0, -4], "right_shin": [30, 0, 0], "left_shin": [30, 0, 0],
                 "right_arm": [6, 0, 4 + 4 * k], "left_arm": [6, 0, -4 + 4 * k], "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0]},
          E_IO if i else None)

    # rise: from flat on his back he swings up stiff from the heels, Nosferatu-style; the head snaps up last.
    c = clip("rise", 2.4)
    flat = P_(LOOSE, {"root": [-90, 0, 0], "@root": [0, 2.2, 0], "head": [10, 0, 0], "right_arm": [0, 0, 4], "left_arm": [0, 0, -4]})
    K(c, 0, flat)
    K(c, 0.5, P_(flat, {"right_fingers": [-70, 0, 0], "left_fingers": [-70, 0, 0]}), E_IO)
    K(c, 1.7, P_(LOOSE, {"root": [-6, 0, 0], "@root": [0, 0.2, 0], "head": [30, 0, 0]}), E_IN)
    K(c, 2.0, P_(hunch, {"head": [28, 0, 0]}), E_OUT)
    K(c, 2.2, P_(hunch, {"head": [-14, 0, 0]}), E_SNAP)
    K(c, 2.4, hunch, E_IO)

    # decapitated (held): the head is taken off -- it flies, drops and rolls away; the body sags to its knees and falls.
    c = clip("decapitated", 2.2, hold=True)
    knees = {"@root": [0, -5.6, 0], "right_leg": [-6, 0, 0], "left_leg": [-6, 0, 0], "right_shin": [92, 0, 0], "left_shin": [92, 0, 0],
             "body": [18, 0, 0], "right_arm": [8, 0, 10], "left_arm": [8, 0, -10]}
    down = {"@root": [0, -4.2, -4.0], "root": [80, 0, 0], "right_leg": [-70, 0, 0], "left_leg": [-70, 0, 0], "right_shin": [96, 0, 0],
            "left_shin": [96, 0, 0], "body": [6, 0, 0], "right_arm": [-160, 0, 14], "left_arm": [-170, 0, -10]}
    K(c, 0, hunch)
    K(c, 0.1, P_(hunch, {"@head": [3.0, 4.0, -1.0], "head": [-40, 30, 60], "body": [-8, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-30, 0, -30]}), E_OUT)
    K(c, 0.35, P_(hunch, {"@head": [7.0, -2.0, -4.0], "head": [-120, 60, 140], "body": [-4, 0, 0]}), E_IN)
    K(c, 0.9, P_(knees, {"@head": [10.0, -24.0, -6.0], "head": [-200, 90, 180]}), E_IN)
    K(c, 1.6, P_(down, {"@head": [10.0, -30.0, 6.0], "head": [-260, 120, 250]}), E_IN)
    K(c, 2.2, P_(down, {"@head": [10.5, -30.0, 6.5], "head": [-270, 120, 260]}), E_OUT)

    done(clips)
    return f, clips


def coat(pose, k=0.5):
    """A jacket's hem (and a hair fall) follows the body."""
    out = dict(pose)
    body = pose.get("body", [0, 0, 0])[0]
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    out.setdefault("jacket_front_r", [min(0.0, rl) * 0.3 - body * 0.3, 0, 0])
    out.setdefault("jacket_front_l", [min(0.0, ll) * 0.3 - body * 0.3, 0, 0])
    return out


# =====================================================================================================
# The werewolf: the man (`human`, h_*) and the wolf (`wolf`, w_*)
# =====================================================================================================

WB = Build(tw=3.4, aw=3.1, ax=5.2, sx=4.8)
W_SKIN = Ramp("#5a3a2a", "#86593f", "#a8775a", "#bf8e70", "#d1a487", "#e0bb9f")
W_HAIR = Ramp("#120c07", "#20160d", "#302114", "#40301d", "#523f28", "#665035")
THERMAL = Ramp("#2c2c2c", "#3e3e3d", "#525250", "#676764", "#7d7d79", "#94948f")
JEANS = Ramp("#141e30", "#1e2c45", "#2a3a5a", "#374a6e", "#485d82", "#5e7397")
W_BOOT = Ramp("#1e130b", "#2e1e12", "#40291a", "#533624", "#67452f", "#7c553b")
FUR = Ramp("#120d09", "#211812", "#33261b", "#473626", "#5d4833", "#755d43")
FUR_PALE = Ramp("#3d3226", "#544636", "#6d5c48", "#87745c", "#a08c72", "#b9a68b")
CLAW = Ramp("#0d0b09", "#1c1814", "#2e2922", "#4a4237", "#6c6253", "#958a77")
GUMS = Ramp("#1f0607", "#3a0b0d", "#561216", "#701b1f", "#8a272a", "#a33a3a")
W_EYE = hexc("#ffcf2e")

WOLF_MAN_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhssssssssshhhh",
    "hsssssssssssssss",
    "sbbbBsssssBbbbss",
    "sswEEsssssEEwsss",
    "ssuuusssssuuusss",
    "sssssssnnsssssss",
    "tssssnnNNnnssst",
    "ttssssssssssstt",
    "tttssmmmmmmsttt",
    "tttttttttttttttt",
    "jttttttttttttttj",
    "jjttttttttttttjj",
]


def fur_m(seed, ramp=FUR, base=3.0, belly=None, belly_face="north"):
    """Shaggy fur laid in overlapping tufts (staggered cells two texels wide and three tall: lit at the top, a shadow under
    each), clumped and mottled by noise; a paler belly/throat on `belly_face`."""
    def m(f, x, y, w, h):
        n = fbm(x * 1.2 + len(f) * 5, y * 0.8, seed, 64, 64, 3, 8.0)
        col = x // 2
        off = int(h01("tuft", seed, f, col) * 3)
        k = (y + off + (col % 2)) % 3
        v = base + (n - 0.5) * 1.2 + (0.4, 0.05, -0.42)[k] - 0.4 * (y / max(1, h))
        if x % 2 == 1 and k == 2:
            v -= 0.25
        if f == "up":
            v += 0.4
        elif f == "down":
            v -= 0.9
        elif f == "south":
            v -= 0.25
        r = ramp
        if belly is not None and f == belly_face and abs(x - (w - 1) / 2) < w * belly:
            r = FUR_PALE
        if h01("tip", seed, f, x, y) < 0.05:
            v += 1.0
        return tone(r, v)
    return m


def fringe(mat, depth=2, seed=1):
    """`mat` with a ragged lower edge (strands of fur hanging unevenly)."""
    def m(f, x, y, w, h):
        if f not in ("up", "down") and y >= h - depth:
            if h01("fr", seed, f, x) * depth < (y - (h - depth)) + 0.6:
                return CLEAR
        return mat(f, x, y, w, h)
    return m


def werewolf_rig():
    m = Model("werewolf", 256, 256)
    m.rig.bone("root", (0, 0, 0))
    m.bone("human", (0, 0, 0), "root")
    m.bone("wolf", (0, 0, 0), "root")
    werewolf_human(m)
    werewolf_wolf(m)
    return m


def werewolf_human(m):
    b, p = WB, "h_"
    skeleton(m, b, p, "human")
    sk = H.skin_mat(W_SKIN, 17601, 3.1, 0.45)
    hr = hair_m(W_HAIR, 17602, 2.7)
    stub = H.stubble(sk, W_HAIR, 17603, 0.5)
    legend = {"h": hr, "s": sk, "t": stub, "b": W_HAIR[1], "B": W_HAIR[2], "w": EYE_WHITE, "E": hexc("#6b5a2a"), "u": W_SKIN[2],
              "n": W_SKIN[4], "N": W_SKIN[1], "m": mix(H_LIPS[1], W_SKIN[2], 0.4), "j": lambda f, x, y, w, h: shade(stub(f, x, y, w, h), 0.9)}
    sides = H.head_sides(sk, hr, top=4, back=13, temple=6, sideburn=10, beard=stub, beard_from=10)
    head_cube(m, p + "head", b.neck, fix(WOLF_MAN_FACE), legend, sides)
    ears_nose(m, p + "head", b.neck, W_SKIN)
    m.cube(p + "body", (-1.4, b.neck - 0.6, -1.4), (2.8, 1, 2.8), stub, density=2, tag="neck")
    # Unkempt hair: a messy shell, tufts sticking up and over the ears.
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 or (y < 3 and x % 3 == 0) else None
        if f != "up" and y == h - 1 and (x * 7) % 3 == 0:
            return None
        return hr(f, x, y, w, h)
    m.cube(p + "head", (-4, b.neck + 5.5, -4), (8, 2.5, 8), shell, inflate=0.35, density=2, tag="hair")
    for i, (x, z, ry) in enumerate(((-2.5, -2.5, 20), (0.5, -1.0, -15), (2.0, 1.5, 30), (-1.0, 2.0, 0))):
        m.cube(p + "head", (x - 1, b.neck + 8.1, z - 1), (2.0, 0.8, 2.0), none_on(("down",), hr), density=2, tag="tuft",
               rotation=(10 - 5 * i, ry, 8 * (i - 1.5)), pivot=(x, b.neck + 8.1, z))
    # A grey waffle-knit thermal, sleeves pushed up to the elbow; worn jeans; boots.
    therm = knit_m(THERMAL, 17610, 2.9)

    def henley(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 6 and abs(x - mid) < 0.6:
            return THERMAL[1] if y < 5 else THERMAL[2]
        if f == "north" and y < 6 and abs(x - mid) < 1.6 and y in (1, 4):
            return THERMAL[5]
        if f in ("north", "south", "east", "west") and (x + 2 * y) % 4 == 0:
            return shade(therm(f, x, y, w, h), 0.86)
        return therm(f, x, y, w, h)
    m.cube(p + "body", (-b.tw, b.hip - 0.8, -2), (2 * b.tw, b.neck - b.hip + 0.8, 4), henley, density=2, tag="torso")
    arm_skin = H.stubble(H.skin_mat(W_SKIN, 17611, 3.1, 0.45), W_HAIR, 17612, 0.25)
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{p}{side}_arm", (cx - b.aw / 2, b.elbow - 0.6, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow + 0.6, b.aw), henley, density=2,
               inflate=0.15, tag="sleeve")
        m.cube(f"{p}{side}_arm", (cx - b.aw / 2, b.elbow - 0.6, -b.aw / 2), (b.aw, 1.4, b.aw), none_on(("up", "down"), lambda f, x, y, w, h: THERMAL[3 if y == 0 else 2]),
               density=2, inflate=0.35, tag="pushed_sleeve")
        m.cube(f"{p}{side}_forearm", (cx - 1.4, b.wrist, -1.4), (2.8, b.elbow - b.wrist, 2.8), arm_skin, density=2, tag="forearm")
    hands(m, b, H.skin_mat(W_SKIN, 17613, 3.1, 0.45), p=p)
    legs(m, b, p, denim_m(JEANS, 17620, rips=0.03), boot_m(W_BOOT, 17621), shoe_h=2.0)
    m.cube(p + "body", (-b.tw, b.hip - 0.5, -2), (2 * b.tw, 0.7, 4), none_on(("up", "down"), lambda f, x, y, w, h: hexc("#2a1a10") if not (f == "north" and abs(x - (w - 1) / 2) < 1) else STEEL[3]),
           inflate=0.3, density=2, tag="belt")


# Wolf: hip height and the digitigrade leg (lengths along each segment, rest bends).
W_HIP = 14.4


def werewolf_wolf(m):
    fur = fur_m(17701)
    fur_b = fur_m(17702, belly=0.28)
    fur_d = fur_m(17703, base=2.4)
    g = lambda f, x, y, w, h, c: None
    m.bone("w_body", (0, W_HIP, 0), "wolf")
    m.bone("w_chest", (0, 19.0, 0), "w_body")
    m.bone("w_head", (0, 26.5, -2.5), "w_chest")
    m.bone("w_jaw", (0, 27.6, -6.0), "w_head")
    for side, s in SIDES:
        m.bone(f"w_{side}_ear", (2.3 * s, 32.8, -3.0), "w_head")
        m.bone(f"w_{side}_arm", (6.6 * s, 24.6, 0), "w_chest")
        m.bone(f"w_{side}_forearm", (6.9 * s, 17.6, 0), f"w_{side}_arm")
        m.bone(f"w_{side}_hand", (6.9 * s, 11.6, 0), f"w_{side}_forearm")
        m.bone(f"w_{side}_claws", (6.9 * s, 9.2, -0.8), f"w_{side}_hand")
        m.bone(f"w_{side}_leg", (2.7 * s, W_HIP, 0.3), "wolf", rotation=(-25, 0, 0))
        m.bone(f"w_{side}_shin", (2.7 * s, W_HIP - 6.0, 0.3), f"w_{side}_leg", rotation=(60, 0, 0))
        m.bone(f"w_{side}_foot", (2.7 * s, W_HIP - 11.0, 0.3), f"w_{side}_shin", rotation=(-50, 0, 0))
        m.bone(f"w_{side}_paw", (2.7 * s, W_HIP - 15.0, 0.3), f"w_{side}_foot", rotation=(15, 0, 0))
    m.bone("w_tail", (0, W_HIP - 0.5, 2.6), "w_body", rotation=(38, 0, 0))
    m.bone("w_tail_tip", (0, W_HIP - 6.0, 2.6), "w_tail", rotation=(-14, 0, 0))

    # Body: a lean belly under a deep barrel chest, a hump of muscle over the shoulders, a ruff round the neck, a mane
    # down the spine.
    m.cube("w_body", (-3.8, W_HIP - 0.8, -2.6), (7.6, 5.4, 5.2), fur_b, density=2, tag="belly")
    m.cube("w_chest", (-5.4, 18.4, -3.6), (10.8, 7.6, 7.0), fur_m(17704, belly=0.25), density=2, tag="chest")
    m.cube("w_chest", (-4.6, 24.4, -2.0), (9.2, 2.4, 6.0), none_on(("down",), fur_d), density=2, tag="hump")
    m.cube("w_chest", (-4.4, 22.0, -4.4), (8.8, 4.6, 7.4), fringe(fur_m(17705, base=3.3), 3, 17705), inflate=0.2, density=2, tag="ruff")
    m.cube("w_chest", (-1.2, 18.0, 3.2), (2.4, 9.0, 1.4), fringe(fur_d, 2, 17706), density=2, tag="mane",
           rotation=(-8, 0, 0), pivot=(0, 27, 3.2))
    # Pecs and abdominal ridges in the fur's shading (a darker line down the middle).
    m.cube("w_chest", (-0.3, 18.6, -3.75), (0.6, 5.0, 0.2), solid(FUR_PALE[1]), density=4, tag="sternum")
    # What is left of his jeans: ragged shorts round the hips and thighs.
    jeans = denim_m(JEANS, 17707, rips=0.12)
    m.cube("w_body", (-4.0, W_HIP - 1.6, -2.8), (8.0, 2.6, 5.6), fringe(jeans, 1, 17707), inflate=0.15, density=2, tag="jeans_seat")
    m.cube("w_body", (-4.0, W_HIP + 0.6, -2.8), (8.0, 0.6, 5.6), none_on(("up", "down"), lambda f, x, y, w, h: hexc("#2a1a10")),
           inflate=0.3, density=2, tag="belt")

    # Head: a wolf's skull, long muzzle, black lips, teeth; ears up; heavy brows; cheek ruffs; yellow eyes.
    hy = 26.5

    def skull(f, x, y, w, h):
        c = fur_m(17710, base=3.1)(f, x, y, w, h)
        if f == "north":
            # Eyes (row 3-4 of 12, slanted), brow shadow above, the pale mask round the muzzle's root.
            ex = (2, 3, w - 4, w - 3)
            if y in (4, 5) and x in ex:
                return W_EYE if (y == 4 and x in (3, w - 4)) or (y == 5 and x in (2, w - 3)) else hexc("#1a0f05")
            if y == 3 and 1 <= x <= w - 2:
                return FUR[1]
            if y >= 7:
                return tone(FUR_PALE, 2.6 + 0.3 * math.sin(x))
        return c

    def eye_glow(f, x, y, w, h, c):
        return hexc("#ffd84a") if c == W_EYE else None
    m.cube("w_head", (-3.5, hy, -6.0), (7.0, 6.0, 6.5), skull, eye_glow, density=2, tag="skull")
    m.cube("w_head", (-3.0, hy + 5.6, -5.0), (6.0, 1.2, 5.0), none_on(("down",), fur_d), density=2, tag="crown")
    snout = fur_m(17711, ramp=FUR, base=3.0)

    def muzzle(f, x, y, w, h):
        if f == "north":
            return tone(FUR_PALE, 2.7 - 0.5 * y / max(1, h))
        if f == "up":
            return snout(f, x, y, w, h)
        if f == "down":
            return GUMS[2]
        if f in ("east", "west") and y == h - 1:
            return hexc("#120b09")                     # the black lip line
        return tone(FUR_PALE, 2.4 - 0.4 * y / max(1, h)) if f != "south" else snout(f, x, y, w, h)
    m.cube("w_head", (-1.9, hy + 1.2, -10.6), (3.8, 2.8, 4.8), muzzle, density=2, tag="muzzle")
    m.cube("w_head", (-1.0, hy + 3.4, -11.0), (2.0, 0.8, 0.6), solid(hexc("#0b0908")), density=4, tag="nose")
    # Upper teeth along the muzzle's lower edge, the long canines.
    for i, z in enumerate((-10.4, -9.6, -8.8, -8.0, -7.2)):
        for s in (-1, 1):
            tall = 0.9 if i == 1 else 0.5
            m.cube("w_head", (s * 1.6 - 0.2, hy + 1.2 - tall, z), (0.4, tall, 0.4), lambda f, x, y, w, h: FANG[5] if y < h - 1 else FANG[3],
                   density=4, tag="tooth")
    def jaw(f, x, y, w, h):
        if f == "up":
            return GUMS[3] if 1 <= x <= w - 2 else GUMS[1]
        if f == "down":
            return tone(FUR_PALE, 2.0)
        return tone(FUR_PALE, 2.6 - 0.4 * y / max(1, h)) if f != "north" else tone(FUR_PALE, 2.2)
    m.cube("w_jaw", (-1.6, hy + 0.2, -10.2), (3.2, 1.0, 4.4), jaw, density=2, tag="jaw")
    m.cube("w_jaw", (-1.1, hy + 1.05, -9.8), (2.2, 0.1, 3.6), solid(GUMS[4]), density=4, tag="tongue")
    for z in (-9.9, -8.6, -7.4):
        for s in (-1, 1):
            m.cube("w_jaw", (s * 1.3 - 0.2, hy + 1.2, z), (0.4, 0.5 if z != -9.9 else 0.8, 0.4), solid(FANG[4]), density=4, tag="tooth")
    for s in (-1, 1):
        m.cube("w_head", (s * 3.6 - 0.8, hy + 4.4, -6.15), (1.6, 0.6, 1.0), solid(FUR[1]), density=2, tag="brow",
               rotation=(0, 0, 14 * s), pivot=(s * 3.0, hy + 4.7, -6.0))
        # Cheek ruffs sweeping back from the jaw.
        m.cube("w_head", ((3.4 if s > 0 else -5.0), hy - 0.5, -4.8), (1.6, 4.0, 4.4), fringe(fur_m(17712, base=3.4), 2, 17712 + s),
               density=2, tag="cheek_ruff", rotation=(0, -20 * s, 10 * s), pivot=(3.4 * s, hy + 2, -3.0))

        def ear(f, x, y, w, h, s=s):
            if f == "north":
                d = abs(x - (w - 1) / 2)
                if d > (h - y) * 0.45 + 0.2:
                    return CLEAR
                return mix(GUMS[1], FUR[2], 0.5) if d < (h - y) * 0.25 and y > 1 else tone(FUR, 3.0)
            if f in ("east", "west", "south"):
                d = abs(x - (w - 1) / 2) if f == "south" else 0
                if f == "south" and d > (h - y) * 0.45 + 0.2:
                    return CLEAR
                return tone(FUR, 2.6 + 0.3 * y / max(1, h))
            return tone(FUR, 2.0)
        m.cube(f"w_{'right' if s < 0 else 'left'}_ear", (2.3 * s - 1.0, hy + 5.8, -3.6), (2.0, 3.4, 1.2), ear, density=2, tag="ear",
               rotation=(-10, 0, -12 * s), pivot=(2.3 * s, hy + 5.8, -3.0))

    # Arms: knotted with muscle under the fur, tufts at the elbows, long black claws.
    for side, s in SIDES:
        cx = 6.9 * s
        m.cube(f"w_{side}_arm", (cx - 2.0, 17.2, -2.1), (4.0, 7.8, 4.2), fur_m(17720 + (s > 0), base=3.1), density=2, tag="upper_arm")
        m.cube(f"w_{side}_arm", (cx - 2.3, 22.0, -2.4), (4.6, 3.4, 4.8), fringe(fur_d, 2, 17722 + (s > 0)), density=2, tag="shoulder")
        m.cube(f"w_{side}_forearm", (cx - 1.8, 11.4, -1.9), (3.6, 6.6, 3.8), fur_m(17724 + (s > 0), base=3.2), density=2, tag="forearm")
        m.cube(f"w_{side}_forearm", (cx - 1.6, 15.6, 1.6), (3.2, 2.6, 1.2), fringe(fur_m(17726, base=3.6), 2, 17726 + (s > 0)), density=2,
               tag="elbow_tuft", rotation=(24, 0, 0), pivot=(cx, 18, 1.6))
        m.cube(f"w_{side}_hand", (cx - 1.7, 9.2, -1.8), (3.4, 2.6, 3.6), fur_m(17728, ramp=FUR, base=2.6), density=2, tag="hand")
        for i, x in enumerate((-1.2, -0.4, 0.4, 1.2)):
            m.cube(f"w_{side}_claws", (cx + x - 0.2, 6.6, -2.0 + 0.2 * abs(i - 1.5)), (0.4, 2.6, 0.45),
                   lambda f, x_, y, w, h: CLAW[4 - y * 3 // max(1, h)] if f != "down" else CLAW[0], density=4, tag="claw",
                   rotation=(-12, 0, 0), pivot=(cx + x, 9.2, -1.8))
        m.cube(f"w_{side}_claws", (cx - 1.5, 8.2, -2.0), (3.0, 1.0, 1.4), fur_m(17729, base=2.4), density=2, tag="knuckles")

    # Legs: thick thighs in the torn jeans, sinewy shins, long feet and paws with claws.
    for side, s in SIDES:
        x = 2.7 * s
        thigh = lambda f, x_, y, w, h, s=s: jeans(f, x_, y, w, h) if y < h * 0.55 and not (y > h * 0.45 and h01("rag", s, f, x_) < 0.5) else fur(f, x_, y, w, h)
        m.cube(f"w_{side}_leg", (x - 2.2, W_HIP - 6.0, -2.4), (4.4, 6.4, 4.8), thigh, density=2, tag="thigh")
        m.cube(f"w_{side}_shin", (x - 1.6, W_HIP - 11.2, -1.6), (3.2, 5.6, 3.2), fur_m(17730 + (s > 0), base=2.9), density=2, tag="shin")
        m.cube(f"w_{side}_shin", (x - 1.8, W_HIP - 7.6, 1.2), (3.6, 2.4, 1.4), fringe(fur_m(17732, base=3.4), 2, 17732 + (s > 0)), density=2,
               tag="calf_tuft", rotation=(-20, 0, 0), pivot=(x, W_HIP - 6, 1.2))
        m.cube(f"w_{side}_foot", (x - 1.3, W_HIP - 15.2, -1.3), (2.6, 4.4, 2.6), fur_m(17734, base=2.6), density=2, tag="foot")
        m.cube(f"w_{side}_paw", (x - 1.7, W_HIP - 16.0, -3.4), (3.4, 1.0, 4.4), fur_m(17736, base=2.4), density=2, tag="paw")
        for i, dx in enumerate((-1.2, -0.4, 0.4, 1.2)):
            m.cube(f"w_{side}_paw", (x + dx - 0.2, W_HIP - 16.0, -4.4), (0.4, 0.6, 1.2), solid(CLAW[3]), density=4, tag="toe_claw")
    m.cube("w_tail", (-1.3, W_HIP - 6.0, 1.4), (2.6, 6.0, 2.6), fur_m(17740, base=2.9), density=2, tag="tail")
    m.cube("w_tail_tip", (-1.5, W_HIP - 12.0, 1.2), (3.0, 6.2, 3.0), fringe(fur_m(17741, base=3.2), 2, 17741), density=2, tag="tail_tip")


def werewolf_anims():
    m = werewolf_rig()
    r = m.rig
    f = AnimFile()
    clips = []
    A = "animation.werewolf."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    # --- the man -------------------------------------------------------------------------------------------------
    hstand = pre(P_(LOOSE, {"body": [3, 0, 0], "head": [4, 0, 0]}), "h_")
    c = clip("human_idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        c.key(t, P_(hstand, pre({"body": [3 + 1.2 * abs(k), 0, 0], "@body": [0, -0.15 * abs(k), 0], "head": [4 - 2 * abs(k), 9 * k, 0],
                                  "right_leg": [0, 0, 3], "left_leg": [-3, 0, -2], "left_shin": [6, 0, 0]}, "h_"),
                    {"@human": [0.3, 0, 0]}), E_IO if t else None)
    L = 1.1
    c = clip("human_walk", L, loop=True)
    for i in range(9):
        w = walk_cycle(8, i, stride=26, arm=16, knee=32, lean=4, p="h_")
        w = {k.replace("@root", "@human"): v for k, v in w.items()}
        c.key(L * i / 8, P_(hstand, w), E_IO if i else None)

    # --- the wolf ------------------------------------------------------------------------------------------------
    crouch = {"w_body": [14, 0, 0], "w_chest": [14, 0, 0], "w_head": [-24, 0, 0], "w_right_arm": [-10, 0, 10], "w_left_arm": [-10, 0, -10],
              "w_right_forearm": [-24, 0, 0], "w_left_forearm": [-24, 0, 0], "w_right_leg": [-10, 0, 3], "w_left_leg": [-10, 0, -3],
              "w_right_shin": [12, 0, 0], "w_left_shin": [12, 0, 0], "w_right_foot": [-4, 0, 0], "w_left_foot": [-4, 0, 0],
              "@wolf": [0, -0.9, 0], "w_tail": [-6, 0, 0]}

    c = clip("idle", 3.0, loop=True)
    for t, k in ((0, 0), (0.75, 1), (1.5, 0), (2.25, -1), (3.0, 0)):
        c.key(t, P_(crouch, {"w_chest": [14 - 2.5 * abs(k), 0, 0], "w_head": [-24 + 3 * k, 8 * k, 0], "w_jaw": [6 + 4 * abs(k), 0, 0],
                             "w_tail": [-6, 10 * k, 0], "w_right_ear": [0, 0, 6 * max(0, k)], "w_left_ear": [0, 0, -6 * max(0, -k)]}),
              E_IO if t else None)
    c.layer("w_chest", lambda t: [1.5 * math.sin(t * 2 * math.pi * 3 / 3.0), 0, 0])

    L = 1.0
    c = clip("walk", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(L * i / 8, P_(crouch, {"w_right_leg": [-10 - 26 * sn, 0, 3], "w_left_leg": [-10 + 26 * sn, 0, -3],
                                     "w_right_shin": [12 + 22 * max(0, math.sin(ph + 1.2)), 0, 0],
                                     "w_left_shin": [12 + 22 * max(0, -math.sin(ph + 1.2)), 0, 0],
                                     "w_right_arm": [-10 + 22 * sn, 0, 10], "w_left_arm": [-10 - 22 * sn, 0, -10],
                                     "w_chest": [16, 6 * sn, 0], "w_head": [-26, -6 * sn, 0], "@wolf": [0, -0.9 + 0.6 * abs(cs), 0],
                                     "w_tail": [-4, 14 * sn, 0]}), E_IO if i else None)

    # run: down on all fours, a gallop -- hind legs drive together, then the forelegs reach.
    L = 0.5
    c = clip("run", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(L * i / 8, {"w_body": [52 + 6 * cs, 0, 0], "w_chest": [24 - 6 * cs, 0, 0], "w_head": [-62 + 6 * cs, 0, 0], "w_jaw": [14, 0, 0],
                          "w_right_arm": [-48 + 44 * sn, 0, 6], "w_left_arm": [-48 + 44 * math.sin(ph - 0.5), 0, -6],
                          "w_right_forearm": [-20 + 20 * cs, 0, 0], "w_left_forearm": [-20 + 20 * math.cos(ph - 0.5), 0, 0],
                          "w_right_leg": [-14 - 40 * sn, 0, 3], "w_left_leg": [-14 - 40 * math.sin(ph + 0.4), 0, -3],
                          "w_right_shin": [20 + 20 * max(0, cs), 0, 0], "w_left_shin": [20 + 20 * max(0, math.cos(ph + 0.4)), 0, 0],
                          "@wolf": [0, -3.0 + 1.4 * abs(sn), 0], "w_tail": [-40, 8 * sn, 0], "w_right_ear": [-30, 0, 0], "w_left_ear": [-30, 0, 0]},
              E_IO if i else None)

    # turn (human -> wolf): the man doubles over, shudders and shrinks away while the wolf swells up out of him and rears.
    c = clip("turn", 3.0)
    bent = pre({"body": [40, 0, 0], "head": [20, 0, 0], "right_arm": [-40, 0, 20], "left_arm": [-40, 0, -20], "right_forearm": [-60, 0, 0],
                "left_forearm": [-60, 0, 0], "right_leg": [-20, 0, 0], "left_leg": [-20, 0, 0], "right_shin": [30, 0, 0], "left_shin": [30, 0, 0]}, "h_")
    bent.update({"@human": [0, -1.5, 0]})
    rear = P_(crouch, {"w_body": [-6, 0, 0], "w_chest": [-10, 0, 0], "w_head": [-30, 0, 0], "w_jaw": [36, 0, 0], "w_right_arm": [-30, 0, 40],
                       "w_left_arm": [-30, 0, -40], "w_right_forearm": [-30, 0, 0], "w_left_forearm": [-30, 0, 0], "@wolf": [0, 0, 0]})
    c.key(0, P_(hstand, {"%human": [1, 1, 1], "%wolf": [0.01, 0.01, 0.01]}))
    c.key(0.5, P_(bent, {"%human": [1, 1, 1], "%wolf": [0.01, 0.01, 0.01]}), E_OUT)
    c.key(1.3, P_(bent, {"%human": [1.08, 1.12, 1.08], "%wolf": [0.3, 0.3, 0.3]}, crouch), E_IO)
    c.key(1.5, P_(bent, {"%human": [0.01, 0.01, 0.01], "%wolf": [0.75, 0.75, 0.75]}, crouch), E_IN)
    c.key(2.1, P_({"%human": [0.01, 0.01, 0.01], "%wolf": [1.05, 1.05, 1.05]}, rear), E_OUT)
    c.key(3.0, P_({"%human": [0.01, 0.01, 0.01], "%wolf": [1, 1, 1]}, crouch), E_IO)
    c.layer("h_body", lambda t: [3 * math.sin(t * 60) if 0.5 < t < 1.5 else 0, 2 * math.sin(t * 47) if 0.5 < t < 1.5 else 0, 0])
    c.layer("w_chest", lambda t: [2 * math.sin(t * 55) if 1.3 < t < 2.0 else 0, 0, 0])

    # claw: rears up and rakes the right claws down across (the hit at 0.42).
    c = clip("claw", 0.85)
    up = P_(crouch, {"w_chest": [-6, 20, 0], "w_body": [6, 10, 0], "w_head": [-20, -16, 0], "w_jaw": [24, 0, 0],
                     "w_right_arm": [-160, 20, 30], "w_right_forearm": [-30, 0, 0], "w_left_arm": [-40, 0, -24]})
    rake = P_(crouch, {"w_chest": [26, -24, 0], "w_body": [16, -10, 0], "w_head": [-30, 14, 0], "w_jaw": [30, 0, 0],
                       "w_right_arm": [-40, -40, -10], "w_right_forearm": [-6, 0, 0], "w_left_arm": [-20, 0, -30], "@wolf": [0, -1.4, -1.6]})
    c.key(0, crouch)
    c.key(0.28, up, E_OUT)
    c.key(0.42, rake, E_SNAP)
    c.key(0.6, P_(rake, {"w_right_arm": [-20, -46, -14]}), E_OUT)
    c.key(0.85, crouch, E_IO)

    # pounce: sinks low, springs (0.45) and lands claws first (the hit at 0.7) a body length ahead.
    c = clip("pounce", 1.3)
    low = P_(crouch, {"w_body": [30, 0, 0], "w_chest": [20, 0, 0], "w_head": [-40, 0, 0], "w_right_leg": [-40, 0, 3], "w_left_leg": [-40, 0, -3],
                      "w_right_shin": [40, 0, 0], "w_left_shin": [40, 0, 0], "@wolf": [0, -3.0, 1.0], "w_right_arm": [10, 0, 14], "w_left_arm": [10, 0, -14]})
    air = P_(crouch, {"w_body": [10, 0, 0], "w_chest": [-4, 0, 0], "w_head": [-30, 0, 0], "w_jaw": [40, 0, 0], "w_right_arm": [-150, 0, 20],
                      "w_left_arm": [-150, 0, -20], "w_right_leg": [30, 0, 3], "w_left_leg": [30, 0, -3], "w_right_shin": [0, 0, 0],
                      "w_left_shin": [0, 0, 0], "@wolf": [0, 6.0, -10.0], "w_tail": [10, 0, 0]})
    land = P_(crouch, {"w_body": [40, 0, 0], "w_chest": [30, 0, 0], "w_head": [-50, 0, 0], "w_jaw": [30, 0, 0], "w_right_arm": [-80, 0, 16],
                       "w_left_arm": [-80, 0, -16], "w_right_forearm": [-10, 0, 0], "w_left_forearm": [-10, 0, 0], "@wolf": [0, -3.4, -16.0]})
    c.key(0, crouch)
    c.key(0.35, low, E_OUT)
    c.key(0.5, air, E_OUT)
    c.key(0.7, land, E_IN)
    c.key(1.0, P_(land, {"@wolf": [0, -2.0, -16.0]}), E_OUT)
    c.key(1.3, P_(crouch, {"@wolf": [0, -0.9, 0]}), E_IO)

    # howl: straightens, throws the head back and howls (held while it lasts), the arms hanging loose.
    c = clip("howl", 2.6)
    howl = P_(crouch, {"w_body": [-4, 0, 0], "w_chest": [-12, 0, 0], "w_head": [-58, 0, 0], "w_jaw": [30, 0, 0], "w_right_arm": [6, 0, 16],
                       "w_left_arm": [6, 0, -16], "w_right_forearm": [-8, 0, 0], "w_left_forearm": [-8, 0, 0], "@wolf": [0, 0.4, 0],
                       "w_right_ear": [-30, 0, 0], "w_left_ear": [-30, 0, 0], "w_tail": [-10, 0, 0]})
    c.key(0, crouch)
    c.key(0.5, howl, E_IO)
    c.key(2.1, P_(howl, {"w_head": [-62, 0, 0], "w_jaw": [34, 0, 0]}), E_IO)
    c.key(2.6, crouch, E_IO)
    c.layer("w_jaw", lambda t: [3 * math.sin(t * 30) if 0.6 < t < 2.0 else 0, 0, 0])

    # flee: struck by silver -- it yelps and cringes, curls away, ears flat, tail down.
    c = clip("flee", 1.2)
    cringe = P_(crouch, {"w_body": [30, 30, 0], "w_chest": [24, 20, 0], "w_head": [-10, 40, 0], "w_jaw": [16, 0, 0], "w_right_arm": [-60, 30, 10],
                         "w_left_arm": [-20, 0, -30], "w_right_ear": [40, 0, 30], "w_left_ear": [40, 0, -30], "w_tail": [30, 0, 0], "@wolf": [0, -2.2, 1.0]})
    c.key(0, crouch)
    c.key(0.15, P_(crouch, {"w_chest": [-10, 0, 0], "w_head": [-40, 0, 0], "w_jaw": [36, 0, 0]}), E_SNAP)
    c.key(0.5, cringe, E_OUT)
    c.key(1.2, P_(crouch, {"w_right_ear": [30, 0, 20], "w_left_ear": [30, 0, -20], "w_tail": [20, 0, 0]}), E_IO)

    done(clips)
    return f, clips


# =====================================================================================================
# The shapeshifter
# =====================================================================================================

SB = Build(tw=3.7, aw=3.4, ax=5.5, sx=5.1)
S_SKIN = Ramp("#634333", "#8f6a55", "#b08870", "#c49f86", "#d5b39b", "#e3c7b1")
S_HAIR = Ramp("#2a2016", "#3d3022", "#524230", "#685540", "#7f6a52", "#978266")
TAN = Ramp("#3b3221", "#564a33", "#726347", "#8c7c5c", "#a59572", "#bdaf8c")
OXBLUE = Ramp("#4a5f7e", "#647b9c", "#8099b8", "#9db3cf", "#b7cbe1", "#d1e0ef")
SLACKS = Ramp("#24180f", "#352418", "#483222", "#5c412d", "#715239", "#876447")
RAW = Ramp("#3a0a0c", "#5e1418", "#82222a", "#a3343b", "#bf4d52", "#d8706f")
SLIME = Ramp("#4a4a2c", "#6b6a3b", "#8d8c4c", "#adab61", "#cbc87c", "#e6e29c")
PEEL = Ramp("#6e5a4c", "#94806e", "#b4a08b", "#cbb8a1", "#ddcdb8", "#ece1d0")
FLARE = hexc("#e9f2ff")

SHIFTER_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhssshhhhhhssshh",
    "hsssssssssssssssh",
    "ssssssssssssssss",
    "sbbbbsssssbbbbss",
    "sswEEsssssEEwsss",
    "ssuuusssssuuusss",
    "sssssssnnsssssss",
    "ssssssnNNnssssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "ssssssssssssssss",
    "jsssssssssssssj",
    "jjssssssssssssjj",
]


def slime_m(seed):
    """Raw flesh glistening under the shed skin: wet red, threads of yellowish slime, bright wet highlights."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 9.0)
        c = tone(RAW, 2.0 + n * 2.0)
        if fbm(x * 3 + 7, y * 1.2, seed + 3, 64, 64, 2, 7.0) > 0.62:
            c = mix(c, SLIME[3], 0.6)
        if h01("wet", seed, f, x, y) < 0.06:
            c = mix(c, hexc("#fff2e6"), 0.6)
        return c
    return m


def peel_m(seed, ragged=True):
    """A flap of sloughed skin: pale and papery, darker and curled at the torn edge, translucent in places."""
    def m(f, x, y, w, h):
        if ragged and f not in ("up", "down") and y >= h - 2 and h01("rag", seed, f, x) < 0.5:
            return CLEAR
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 10.0)
        c = tone(PEEL, 2.4 + n * 2.0 - 0.8 * y / max(1, h))
        if f == "south":
            c = mix(c, SLIME[2], 0.5)
        a = 235 if h01("thin", seed, f, x, y) < 0.8 else 170
        return (c[0], c[1], c[2], a)
    return m


def shapeshifter_rig():
    m = Model("shapeshifter", 256, 256)
    m.rig.bone("root", (0, 0, 0))
    b = SB
    skeleton(m, b)
    m.bone("jacket_front_r", (-2.0, b.hip + 0.5, -2.0), "body")
    m.bone("jacket_front_l", (2.0, b.hip + 0.5, -2.0), "body")
    sk = H.skin_mat(S_SKIN, 17801, 3.1, 0.4)
    hr = hair_m(S_HAIR, 17802, 2.8)
    legend = {"h": hr, "s": sk, "b": S_HAIR[2], "w": EYE_WHITE, "E": hexc("#5a4a38"), "u": S_SKIN[2], "n": S_SKIN[4], "N": S_SKIN[1],
              "m": mix(H_LIPS[2], S_SKIN[2], 0.4), "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}
    sides = H.head_sides(sk, hr, top=3, back=11, temple=8, sideburn=6)
    head_cube(m, "head", b.neck, fix(SHIFTER_FACE), legend, sides)
    ears_nose(m, "head", b.neck, S_SKIN, nose_h=1.8)
    m.cube("body", (-1.5, b.neck - 0.6, -1.5), (3, 1, 3), sk, density=2, tag="neck")
    # Thinning hair, combed flat: a thin shell from the temples back, a bald spot on the crown.
    def shell(f, x, y, w, h):
        if f == "down" or f == "north":
            return None
        if f == "up" and math.hypot(x - (w - 1) / 2, y - (h - 1) * 0.55) < 2.2:
            return None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if fx > 4 else None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, b.neck + 6.4, -4), (8, 1.6, 8), shell, inflate=0.2, density=2, tag="hair")
    # A tan zip-up windbreaker over a pale blue oxford shirt, brown slacks, plain brown shoes, a cheap watch.
    oxf = fabric(OXBLUE, 17810, 3.2, 0.12)
    jacket = worn(TAN, 17811, 3.0, 0.6, scuff=0.04)

    def zipped(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north":
            if y < 6 and abs(x - mid) < 2.4 - y * 0.25:
                return oxf(f, x, y, w, h)
            if abs(x - mid) < 0.6:
                return STEEL[3] if y % 2 else TAN[1]
            if y in (h - 2, h - 1):
                return TAN[2]                     # the elastic waistband
        return jacket(f, x, y, w, h)
    m.cube("body", (-b.tw, b.hip, -2), (2 * b.tw, b.neck - b.hip, 4), oxf, density=2, tag="torso")
    m.cube("body", (-b.tw, b.hip - 0.6, -2), (2 * b.tw, b.neck - b.hip + 0.4, 4), none_on(("up", "down"), zipped), inflate=0.35, density=2,
           tag="jacket")
    m.cube("body", (-b.tw, b.neck - 1.2, -2), (2 * b.tw, 1.4, 4), none_on(("down",), lambda f, x, y, w, h: None if f in ("north", "up") and abs(x - (w - 1) / 2) < 2.4 else TAN[3]),
           inflate=0.6, density=2, tag="jacket_collar")
    for s in (-1, 1):
        m.cube("body", (0.9 * s - 0.75, b.neck - 1.3, -2.45), (1.5, 1.2, 0.4), lambda f, x, y, w, h: OXBLUE[4] if y == 0 else OXBLUE[3],
               density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.9 * s, b.neck - 0.2, -2.3))
        m.cube("body", (2.2 * s - 0.1, 14.0, -2.5), (0.2, 3.0, 0.2), solid(TAN[1]), density=4, tag="pocket_seam")
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow, b.aw), jacket, inflate=0.25, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.65, b.wrist + 0.2, -1.65), (3.3, b.elbow - b.wrist - 0.2, 3.3), none_on(("up",), jacket),
               inflate=0.25, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.65, b.wrist + 0.2, -1.65), (3.3, 0.6, 3.3), none_on(("up", "down"), lambda f, x, y, w, h: TAN[2]),
               inflate=0.4, density=2, tag="cuff")
    hands(m, b, H.skin_mat(S_SKIN, 17812, 3.1, 0.4))
    m.cube("left_hand", (b.ax - 1.4, b.wrist - 0.6, -1.5), (2.8, 0.5, 3.0), lambda f, x, y, w, h: STEEL[4] if f == "east" else STEEL[2],
           inflate=0.12, density=4, tag="watch")
    legs(m, b, "", fabric(SLACKS, 17820, 3.0, 0.2, crease=True), shoe_m(OXFORD, 17821))
    for side, s in (("r", -1), ("l", 1)):
        def hem(f, x, y, w, h, s=s):
            if f in ("up", "south") or f == ("west" if s < 0 else "east"):
                return None
            return jacket(f, x, y + 16, w, h + 16)
        m.cube(f"jacket_front_{side}", ((0.0 if s > 0 else -b.tw), b.hip - 1.4, -2), (b.tw, 1.0, 4.0), hem, inflate=0.35, density=2,
               tag="jacket_hem")

    # --- shed_skin: what shows once he is found out ---------------------------------------------------------------
    m.bone("shed_skin", (0, b.neck + 4, -4.2), "head")
    m.bone("shed_skin_face", (0, b.neck + 5.5, -4.3), "shed_skin")
    m.bone("shed_skin_body", (0, b.neck - 1, -2.3), "body")
    for side, s in SIDES:
        m.bone(f"shed_skin_{side}_arm", (b.ax * s, b.elbow + 2, 0), f"{side}_forearm")
        m.bone(f"shed_skin_{side}_hand", (b.ax * s, b.wrist, 0), f"{side}_hand")
    raw = slime_m(17830)

    def face_wound(f, x, y, w, h):
        # Raw patches where the face has come away: the left cheek and brow, a strip along the jaw.
        cx, cy = w * 0.7, h * 0.45
        d = math.hypot((x - cx) / (w * 0.32), (y - cy) / (h * 0.5))
        jaw = y > h * 0.78 and 0.15 < x / w < 0.6
        if d < 1.0 or jaw:
            if h01("hole", x, y) < 0.08:
                return None
            return raw(f, x, y, w, h)
        return None
    # A decal just in front of the face, with the eyes punched through (they stay his own, but flare).
    m.cube("shed_skin", (-4.0, b.neck, -4.06), (8.0, 8.0, 0), face_wound, faces=("north",), density=2, tag="face_raw")
    for x0 in (-3.0, 1.5):
        m.cube("shed_skin", (x0, b.neck + 4.0, -4.12), (1.5, 0.5, 0), solid(FLARE), lambda f, x, y, w, h, c: hexc("#f4f8ff"), faces=("north",),
               density=2, tag="eye_flare")
    # The flap of his face peeling down from the brow over the left cheek; a loose strip from the neck; flaps on the hands.
    m.cube("shed_skin_face", (0.4, b.neck + 1.4, -4.5), (3.2, 4.1, 0.25), peel_m(17831), density=4, tag="face_flap",
           rotation=(-14, 0, 8), pivot=(2.0, b.neck + 5.5, -4.3))
    m.cube("shed_skin_body", (-2.0, b.neck - 4.2, -2.75), (2.4, 3.2, 0.25), peel_m(17832), density=4, tag="neck_flap",
           rotation=(-6, 0, -10), pivot=(-0.8, b.neck - 1, -2.6))
    m.cube("shed_skin_body", (-b.tw, b.neck - 1.4, -2.1), (2 * b.tw, 1.4, 4.2), none_on(("up", "down"), lambda f, x, y, w, h: raw(f, x, y, w, h) if h01("n", f, x) < 0.6 else None),
           inflate=0.62, density=2, tag="collar_raw")
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"shed_skin_{side}_hand", (cx - 1.3, b.palm, -1.45), (2.6, b.wrist - b.palm, 2.9),
               lambda f, x, y, w, h: raw(f, x, y, w, h) if h01("hn", f, x, y) < 0.7 else None, inflate=0.06, density=2, tag="hand_raw")
        m.cube(f"shed_skin_{side}_hand", (cx + s * 1.25 - 0.15, b.palm - 2.0, -1.2), (0.3, 3.2, 2.4), peel_m(17833 + (s > 0)), density=4,
               tag="hand_flap", rotation=(0, 0, 10 * s), pivot=(cx + s * 1.25, b.wrist - 0.4, 0))
        drip = lambda f, x, y, w, h, s=s: CLEAR if (f not in ("up", "down") and y > 2 and h01("drip", s, x) * h < y) else \
            (mix(SLIME[2 + (y % 3 == 0)], RAW[2], 0.35) if h01("dr", s, f, x, y) < 0.8 else hexc("#f6f2d0"))
        m.cube(f"shed_skin_{side}_arm", (cx - 1.0, b.wrist + 0.6, -1.95), (2.0, 3.0, 0.2), drip, density=4,
               tag="sleeve_slime", rotation=(-6, 0, 0), pivot=(cx, b.elbow + 1, -1.9))
    return m


def shapeshifter_anims():
    m = shapeshifter_rig()
    r = m.rig
    f = AnimFile()
    clips = []
    A = "animation.shapeshifter."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def K(c, t, pose, ease=None):
        c.key(t, coat(pose), ease)

    stand = P_(LOOSE, {"body": [2, 0, 0]})
    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        K(c, t, P_(stand, {"body": [2 + 0.8 * abs(k), 0, 0], "@body": [0, -0.12 * abs(k), 0], "head": [2, 12 * k, 0],
                           "right_leg": [0, 0, 2], "left_leg": [-2, 0, -2], "left_shin": [4, 0, 0]}), E_IO if t else None)
    # Now and then the face twitches -- a shiver through the borrowed skin.
    c.layer("head", lambda t: [0, 0, 2.5 * math.sin(t * 90) if 2.6 < t < 2.9 else 0])
    c.layer("shed_skin_face", lambda t: [6 * math.sin(t * 9), 0, 0])

    L = 1.15
    c = clip("walk", L, loop=True)
    for i in range(9):
        K(c, L * i / 8, P_(stand, walk_cycle(8, i, stride=24, arm=14, knee=30, lean=2)), E_IO if i else None)
    c.layer("shed_skin_face", lambda t: [8 * math.sin(4 * math.pi * t / L), 0, 0])

    # attack: a sudden lunge, a backhand rake across (the hit at 0.35), the face pushed forward.
    c = clip("attack", 0.8)
    wind = P_(stand, CLAW_R, {"right_arm": [-60, 60, 70], "right_forearm": [-50, 0, 0], "body": [0, 30, 0], "head": [0, -20, 0],
                              "left_arm": [-20, 0, -20], "right_leg": [6, 0, 0], "left_leg": [-10, 0, 0]})
    rake = P_(stand, CLAW_R, {"right_arm": [-80, -40, -10], "right_forearm": [-10, 0, 0], "body": [14, -30, 0], "head": [-6, 22, 0],
                              "left_arm": [-30, 0, -30], "@root": [0, -0.6, -1.6], "right_leg": [-20, 0, 0], "right_shin": [16, 0, 0],
                              "left_leg": [14, 0, 0]})
    K(c, 0, stand)
    K(c, 0.22, wind, E_OUT)
    K(c, 0.35, rake, E_SNAP)
    K(c, 0.5, P_(rake, {"right_arm": [-60, -50, -14]}), E_OUT)
    K(c, 0.8, stand, E_IO)

    # revealed: as if scalded -- recoils, a hand thrown up to the face, then the glare (the eyes flare) and a snarl.
    c = clip("revealed", 1.6)
    flinch = P_(stand, OPEN_L, {"body": [-12, 14, 0], "head": [-20, 30, 0], "@root": [0, 0, 1.6], "right_arm": [-20, 0, 30],
                                "right_forearm": [-30, 0, 0], "left_leg": [10, 0, 0], "right_leg": [-6, 0, 0]})
    flinch = reach(r, flinch, "left", (1.5, 27.0, -6.0), (SB.ax, SB.palm - 0.6, -0.6))
    glare = P_(stand, CLAW_R, CLAW_L, {"body": [16, 0, 0], "head": [-22, 0, 0], "@root": [0, -0.8, 0], "right_arm": [-26, 20, 30],
                                       "left_arm": [-26, -20, -30], "right_forearm": [-40, 0, 0], "left_forearm": [-40, 0, 0],
                                       "right_leg": [-12, 0, 4], "left_leg": [8, 0, -4], "right_shin": [16, 0, 0], "left_shin": [10, 0, 0]})
    K(c, 0, stand)
    K(c, 0.15, flinch, E_SNAP)
    K(c, 0.7, P_(flinch, {"head": [-10, 20, 0]}), E_IO)
    K(c, 1.0, glare, E_OUT)
    K(c, 1.6, P_(glare, {"head": [-18, 0, 0]}), E_IO)
    c.layer("head", lambda t: [2 * math.sin(t * 80) if 0.95 < t < 1.3 else 0, 0, 0])

    # shed: he claws at his own face and drags it down; the skin comes away in strips and drops; a shudder.
    c = clip("shed", 2.6, hold=True)
    face = (0.0, 28.0, -5.2)
    cl1 = reach(r, P_(stand, CLAW_R, CLAW_L, {"head": [-10, 0, 0]}), "right", (-1.6, 29.5, -5.0), (-SB.ax, SB.palm - 0.6, -0.6))
    cl1 = reach(r, cl1, "left", (1.6, 29.5, -5.0), (SB.ax, SB.palm - 0.6, -0.6))
    drag = reach(r, P_(stand, FIST_R, FIST_L, {"head": [16, 0, 0], "body": [18, 0, 0]}), "right", (-1.8, 21.5, -6.5), (-SB.ax, SB.palm - 0.6, -0.6))
    drag = reach(r, drag, "left", (1.8, 21.5, -6.5), (SB.ax, SB.palm - 0.6, -0.6))
    K(c, 0, stand)
    K(c, 0.45, cl1, E_OUT)
    K(c, 0.75, P_(cl1, {"head": [-16, 0, 0]}), E_IO)
    K(c, 1.35, P_(drag, {"shed_skin_face": [60, 0, 0], "@shed_skin_face": [0, -3, -1.5], "shed_skin_body": [30, 0, 0]}), E_IN)
    K(c, 1.8, P_(stand, {"body": [20, 0, 0], "head": [20, 0, 0], "shed_skin_face": [100, 0, 30], "@shed_skin_face": [0.5, -22, -2],
                         "%shed_skin_face": [1, 1, 1], "shed_skin_body": [80, 0, 0], "@shed_skin_body": [0, -14, -2]}), E_IN)
    K(c, 2.6, P_(stand, {"body": [8, 0, 0], "head": [-8, 0, 0], "shed_skin_face": [90, 0, 30], "@shed_skin_face": [0.5, -22.5, -2],
                         "shed_skin_body": [88, 0, 0], "@shed_skin_body": [0, -14.6, -2]}), E_OUT)
    c.layer("body", lambda t: [2.5 * math.sin(t * 70) if 1.8 < t < 2.3 else 0, 0, 0])

    done(clips)
    return f, clips


# =====================================================================================================
# Output
# =====================================================================================================

def write_creature(name, m, glow=True):
    t, g = m.build(gutter=1, seed=abs(hash(name)) % 10000 if False else sum(map(ord, name)))
    m.rig.write(GEO + name + ".geo.json")
    save(t, "entity", name)
    if glow:
        save(g, "entity", name + "_glowmask")
    return t, g


def generate():
    for name in CREATURES:
        write_creature(name, RIGS[name](), glow=name != "henry_winchester")
        f, _ = ANIMS[name]()
        write_compact(f, ANIM + name + ".animation.json")



CREATURES = ("henry_winchester", "vampire", "werewolf", "shapeshifter")
RIGS = {"henry_winchester": lambda: henry_rig(), "vampire": lambda: vampire_rig(), "werewolf": lambda: werewolf_rig(), "shapeshifter": lambda: shapeshifter_rig()}
ANIMS = {"henry_winchester": lambda: henry_anims(), "vampire": lambda: vampire_anims(), "werewolf": lambda: werewolf_anims(), "shapeshifter": lambda: shapeshifter_anims()}


if __name__ == "__main__":
    generate()
