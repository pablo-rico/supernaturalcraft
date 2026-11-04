"""Crowley's retinue (v0.35): the suited demon and the possessed patient (names in CrowleyAssets). Both on Crowley's rig
family (crowley_art: the same skeleton and builders, no overcoat).

crowley_suit: a crossroads functionary in a black suit, white shirt and dark tie, a manila folder clamped under his left
arm (`folder`, a child of the left arm), black eyes. Clean-shaven, short dark hair. Clips idle, walk, strike (a backhand
on tick 8 of 16), smoke (thrown back as the smoke pours out, tick 10 of 20; held).

possessed_patient: a stained, pale hospital gown, a canvas straitjacket half on (the right sleeve still on, the left arm torn
free), its loose straps hanging (`straps`, swinging), bare feet, sickly grey skin, matted hair, black eyes. Jerky clips:
idle, walk (a dragging shamble), lunge (tick 9 of 18), twitch (24 ticks).

  suit:     root -- body -- head -- eyes, brows, beard (empty);  tie;  folder (under left_arm);  arms;  legs
  patient:  root -- body -- head -- ...;  gown_f, gown_b;  straps -- strap_l, strap_r;  arms;  legs
"""

import math

import crowley_art as C
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from michael_art import fabric, write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO, ANIM = C.GEO, C.ANIM
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = C.E_IO, C.E_OUT, C.E_IN, C.E_BACK, C.E_SNAP
Clip, merge, tone, jitter = C.Clip, C.merge, C.tone, C.jitter
HIP, NECK, TW, TD, SX, SY, AX = C.HIP, C.NECK, C.TW, C.TD, C.SX, C.SY, C.AX

SUIT = Ramp("#060607", "#0c0c0f", "#141418", "#1d1d22", "#28282e", "#36363e")
WHITE = Ramp("#8f949b", "#aeb3ba", "#c6cad0", "#d9dce0", "#e8eaed", "#f6f7f8")
DARK_TIE = Ramp("#0a0b10", "#12141c", "#1b1e2a", "#262a3a", "#33384c", "#474d66")
MANILA = Ramp("#5e4521", "#7d5e30", "#9c7a42", "#b99657", "#d1b073", "#e6cb95")
SKIN2 = Ramp("#4a3127", "#6c4a3a", "#8d6650", "#a87e66", "#bd957c", "#d1ad95")
SICK = Ramp("#3e4038", "#585a4e", "#737465", "#8d8d7c", "#a5a492", "#bdbba8")
GOWN = Ramp("#4f5a58", "#6b7774", "#8a9692", "#a8b2ad", "#c1c9c3", "#d6dcd6")
CANVAS = Ramp("#5a5446", "#776f5d", "#958b75", "#ada38b", "#c2b9a0", "#d6cfb8")
LEATHER = Ramp("#1e130b", "#2e1d11", "#412a18", "#563921", "#6c4a2c", "#86603b")
BRASS = Ramp("#3d2e10", "#5e4718", "#836424", "#a98432", "#cba64a", "#e8cc78")
STAIN = hexc("#6a5a2a")
BLOOD = hexc("#4a1410")
BLACK_EYE, EYE_GLINT = hexc("#040304"), hexc("#3a3a44")

SUIT_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhsssssssssssshh",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "ssuuuuussuuuuuss",
    "ssuKKKkssKKKkuss",
    "sssbbbsssssbbbss",
    "ssssssssnsssssss",
    "sssssnnnnnnsssss",
    "ssssssNNNNssssss",
    "ssssssssssssssss",
    "sssssqqqqqqsssss",
    "ssssssssssssssss",
    "jsssssssssssssss",
]
PATIENT_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhshhhhhhshhhh",
    "hhsssssssshssshh",
    "ssssssssssssssss",
    "ssUUUUUssUUUUUss",
    "ssUKKKkssKKKkUss",
    "ssvvvvvssvvvvvss",
    "sssvssssnssssvss",
    "sssssnnnnnnsssss",
    "ssssssNNNNssssss",
    "sssssqqqqqqsssss",
    "ssssqrrrrrrqssss",
    "sssssssssssssss.",
    "ssssssssssssssss",
]
PATIENT_FACE[14] = PATIENT_FACE[14].replace(".", "s")
assert all(len(r) == 16 for r in SUIT_FACE + PATIENT_FACE)


def eye_legend(skin_ramp):
    return {"K": BLACK_EYE, "k": EYE_GLINT, "n": skin_ramp[4], "N": skin_ramp[1], "q": hexc("#3a1e18"), "r": hexc("#5e2a24"),
            "v": lambda f, x, y, w, h: mix(skin_ramp[1], hexc("#3a2a40"), 0.5)}


# =====================================================================================================
# The suited demon
# =====================================================================================================

def suit_rig():
    m = Model("crowley_suit", 256, 256)
    C.skeleton(m, coat=False)
    m.bone("folder", (AX + 0.0, SY - 4.0, 0), "left_arm")
    sk = H.skin_mat(SKIN2, 35601, 3.1, 0.5)
    hr = lambda f, x, y, w, h: tone(C.HAIR, 1.4 + (0.8 if (y * 3 + x // 3) % 5 == 0 else 0) + 0.5 * fbm(x, y, 35602, 32, 32, 2, 5))
    C.build_head(m, SUIT_FACE, sk=sk, hr=hr, bd=sk, seed=35600, eyes=None, beard=False, brow=C.HAIR[0], skin=SKIN2,
                 legend_extra=eye_legend(SKIN2))
    C.build_suit(m, SUIT, WHITE, DARK_TIE, seed=35610)
    # The jacket's skirt, to the top of the thigh.
    suit = fabric(SUIT, 35620, 3.0, 0.3)
    m.cube("body", (-TW, HIP - 2.8, -TD), (2 * TW, 2.4, 2 * TD), none_on(("up",), lambda f, x, y, w, h: SUIT[0]
           if f == "north" and abs(x - (w - 1) / 2) < 0.6 else suit(f, x, y + 18, w, h + 18)), inflate=0.3, density=2, tag="skirt")
    shirt_cuff = lambda f, x, y, w, h: WHITE[4] if f not in ("up", "down") else WHITE[2]
    C.build_arms(m, sleeve=fabric(SUIT, 35630, 3.0, 0.3), cuff=shirt_cuff, seed=35630, hand=H.skin_mat(SKIN2, 35631, 3.1, 0.4),
                 cuff_depth=0.6)
    C.build_legs(m, SUIT, seed=35640)

    # The manila folder pressed under the left arm, papers peeking out.
    def folder(f, x, y, w, h):
        if f in ("up", "down"):
            return WHITE[4] if x % 3 else WHITE[2]
        if f in ("north", "south"):
            return MANILA[2]
        c = tone(MANILA, 3.2 - 0.6 * y / max(1, h) + 0.4 * h01("folder", x, y))
        if y == 2 and 2 <= x <= 5:
            return hexc("#f0ece2")                     # the label
        return c
    m.cube("folder", (AX - 2.25, SY - 9.6, -2.8), (0.5, 6.4, 5.6), folder, density=2, tag="folder")
    m.cube("folder", (AX - 2.2, SY - 4.2, -2.4), (0.4, 0.5, 4.8), solid(WHITE[5]), density=4, tag="folder_papers")
    return m


SUIT_IDLE = {"right_arm": [-4, 0, -4], "left_arm": [-14, 0, -6], "left_forearm": [-30, 10, 0], "head": [-2, 0, 0],
             "right_forearm": [-10, 0, 0], "right_fingers": [30, 0, 0]}


def suit_anims():
    f = AnimFile()
    a = "animation.crowley_suit."
    idle = Clip(f, a + "idle", 3.0, loop=True)
    for t in (0, 1.5, 3.0):
        b = math.sin(t / 3.0 * 2 * math.pi)
        idle.key(t, merge(SUIT_IDLE, {"head": [-2 + 2 * b, 5 * b, 0], "@body": [0, -0.15 * (1 - math.cos(t / 3 * 2 * math.pi)), 0]}),
                 E_IO if t else None)
    idle.done()
    walk = Clip(f, a + "walk", 1.0, loop=True)
    for t, s in ((0, 1), (0.5, -1), (1.0, 1)):
        walk.key(t, merge(SUIT_IDLE, {"right_leg": [24 * s, 0, 0], "left_leg": [-24 * s, 0, 0], "right_shin": [6 if s < 0 else 18, 0, 0],
                                      "left_shin": [18 if s < 0 else 6, 0, 0], "right_arm": [-18 * s, 0, 2],
                                      "body": [0, 5 * s, 0], "tie": [-4, 0, 3 * s]}), E_IO if t else None)
    walk.anim.pos("body", (0, [0, 0, 0]), (0.25, [0, 0.5, 0], E_OUT), (0.5, [0, 0, 0], E_IN), (0.75, [0, 0.5, 0], E_OUT),
                  (1.0, [0, 0, 0], E_IN))
    walk.done()
    # Strike: a backhand across, landing on tick 8.
    st = Clip(f, a + "strike", 0.8)
    st.key(0, SUIT_IDLE)
    st.key(0.28, merge(SUIT_IDLE, {"right_arm": [-60, 0, -40], "right_forearm": [-60, 30, 0], "body": [0, -26, 0], "head": [0, 10, 0],
                                   "right_fingers": [70, 0, 0], "left_leg": [-8, 0, 0]}), E_OUT)
    st.key(0.4, merge(SUIT_IDLE, {"right_arm": [-80, 0, 50], "right_forearm": [-10, 0, 0], "body": [0, 26, 0], "head": [0, -6, 0],
                                  "right_fingers": [70, 0, 0], "right_leg": [-12, 0, 0], "tie": [0, 0, 14]}), E_SNAP)
    st.key(0.55, merge(SUIT_IDLE, {"right_arm": [-66, 0, 46], "right_forearm": [-14, 0, 0], "body": [0, 18, 0], "head": [0, -4, 0]}))
    st.key(0.8, SUIT_IDLE, E_IO)
    st.done()
    # Smoke: thrown back, arms stiff, the smoke pours out of his mouth from tick 10; held.
    sm = Clip(f, a + "smoke", 1.0, hold=True)
    sm.key(0, SUIT_IDLE)
    arch = lambda k, j: {"head": [-50 * k + j[0], j[1], j[2]], "body": [-16 * k, 0, 0], "right_arm": [-10, 0, 30 * k + j[1]],
                         "left_arm": [-14, 0, -24 * k], "left_forearm": [-30, 10, 0], "right_fingers": [-20 * k, 0, 0],
                         "@root": [0, 0.5 * k, 0], "right_leg": [4 * k, 0, 0], "left_leg": [4 * k, 0, 0]}
    sm.key(0.3, arch(0.6, [0, 0, 0]), E_OUT)
    for i, t in enumerate((0.4, 0.5, 0.6, 0.7, 0.8)):
        sm.key(t, arch(1.0, jitter("suit-smoke", i, 5)))
    sm.key(1.0, arch(1.05, [0, 0, 0]), E_IO)
    sm.done()
    return f


# =====================================================================================================
# The possessed patient
# =====================================================================================================

def stained(ramp, seed, base=3.0):
    cl = fabric(ramp, seed, base, 0.3, hem=True)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        n = fbm(x * 1.5, y * 1.5 + len(f), seed + 9, 48, 48, 2, 4.0)
        if n > 0.7:
            c = mix(c, STAIN, min(0.6, (n - 0.7) * 3))
        if fbm(x * 2 + 7, y * 2, seed + 13, 48, 48, 2, 5.0) > 0.8:
            c = mix(c, BLOOD, 0.45)
        return c
    return m


def patient_rig():
    m = Model("possessed_patient", 256, 256)
    C.skeleton(m, coat=False)
    m.bone("gown_f", (0, HIP, -TD), "body")
    m.bone("gown_b", (0, HIP, TD), "body")
    m.bone("straps", (0, NECK - 6, 0), "body")
    m.bone("strap_r", (-TW - 0.5, HIP + 4.0, -0.5), "straps")
    m.bone("strap_l", (TW + 0.5, HIP + 4.0, -0.5), "straps")
    sk = H.skin_mat(SICK, 35701, 3.0, 0.7, blotch=hexc("#5a4a5a"), blotch_p=0.05)

    def hr(f, x, y, w, h):
        n = fbm(x * 1.2, y * 1.6, 35702, 32, 32, 2, 4.0)
        return tone(C.HAIR, 1.0 + 2.2 * n + (0.6 if h01("hair", f, x, y) < 0.12 else 0))
    C.build_head(m, PATIENT_FACE, sk=sk, hr=hr, bd=sk, seed=35700, eyes=None, beard=False, brow=C.HAIR[2], skin=SICK,
                 legend_extra=eye_legend(SICK) | {"U": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.7)})
    # Matted locks hanging over the brow.
    for i, x in enumerate((-3.2, -1.0, 1.6, 3.0)):
        m.cube("head", (x - 0.5, NECK + 5.2 - 0.4 * (i % 2), -4.25), (1.0, 2.8, 0.4), none_on(("up",), hr), density=4, tag="lock")
    gown = stained(GOWN, 35710, 3.2)
    canvas = stained(CANVAS, 35711, 3.0)
    # The gown's torso under the straitjacket's canvas (the jacket's buckled front, its collar).
    def jacket(f, x, y, w, h):
        if f == "north":
            if y in (6, 7, 13, 14) and 2 <= x <= w - 3:
                return LEATHER[2] if y in (6, 13) else LEATHER[1]          # straps across the chest
            if y in (6, 13) and x == w // 2:
                return BRASS[4]                                             # the buckle
            if y < 4 and abs(x - (w - 1) / 2) < 3:
                return gown(f, x, y, w, h)                                  # the gown at the throat
        return canvas(f, x, y, w, h)
    m.cube("body", (-TW, HIP - 0.4, -TD), (2 * TW, NECK - HIP + 0.4, 2 * TD), jacket, inflate=0.25, density=2, tag="straitjacket")
    for bone, z, s in (("gown_f", -TD - 0.45, -1), ("gown_b", TD + 0.05, 1)):
        m.cube(bone, (-TW - 0.3, HIP - 6.2, z), (2 * TW + 0.6, 6.4, 0.4), lambda f, x, y, w, h: None if (f != "up" and y >= h - 2 and
               h01("rag", bone, x) < 0.35) else gown(f, x, y + 20, w, h), density=2, tag="gown", rotation=(4 * s, 0, 0),
               pivot=(0, HIP, z))
    # Loose straps: a long one off each side, its buckle at the end.
    for bone, s in (("strap_r", -1), ("strap_l", 1)):
        px = (TW + 0.5) * s
        m.cube(bone, (px - 0.35, HIP - 3.0, -0.9), (0.7, 7.0, 0.3), lambda f, x, y, w, h: LEATHER[3] if x == 0 else LEATHER[2],
               density=4, tag="strap")
        m.cube(bone, (px - 0.5, HIP - 3.6, -1.0), (1.0, 0.7, 0.5), solid(BRASS[3]), density=4, tag="strap_buckle")
    m.cube("straps", (-0.4, NECK - 13.5, TD + 0.2), (0.8, 7.0, 0.3), solid(LEATHER[2]), density=4, tag="back_strap")
    # Arms: the right still in the canvas sleeve (a strap trailing off its end), the left torn free, bare and bruised.
    bare = H.skin_mat(SICK, 35720, 3.0, 0.7, blotch=hexc("#4a3a50"), blotch_p=0.08)
    for side, s in C.SIDES:
        cx = AX * s
        mat = canvas if s < 0 else bare
        m.cube(f"{side}_arm", (cx - C.AW / 2, C.ELBOW, -C.AW / 2), (C.AW, SY + 0.8 - C.ELBOW, C.AW), mat, inflate=0.2 if s < 0 else 0,
               density=2, tag="arm")
        m.cube(f"{side}_forearm", (cx - 1.7, C.WRIST, -1.7), (3.4, C.ELBOW - C.WRIST, 3.4), none_on(("up",), mat),
               inflate=0.2 if s < 0 else 0, density=2, tag="forearm")
        m.cube(f"{side}_hand", (cx - 1.3, C.PALM, -1.5), (2.6, C.WRIST - C.PALM + 0.2, 3.0), bare, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(bare(f, x, y, w, h), 0.85 if x % 2 else 1.0)
        m.cube(f"{side}_fingers", (cx - 1.25, C.PALM - 1.6, -0.1), (2.5, 1.6, 1.5), fing, density=2, tag="fingers")
        m.cube(f"{side}_middle", (cx - 1.25, C.PALM - 1.7, -0.8), (2.5, 1.7, 0.7), fing, density=4, tag="middle")
        m.cube(f"{side}_index", (cx - 1.25, C.PALM - 1.6, -1.5), (2.5, 1.6, 0.7), fing, density=4, tag="index")
        m.cube(f"{side}_thumb", (cx - 0.9 * s - 0.5, C.PALM + 0.2, -2.05), (1.0, 1.6, 0.9), bare, density=2, tag="thumb")
    m.cube("right_forearm", (-AX - 0.35, C.WRIST - 4.2, 0.6), (0.7, 4.4, 0.3), solid(LEATHER[2]), density=4, tag="sleeve_strap")
    # Legs: the gown's hem over bare, mottled shins; bare feet.
    feet = H.skin_mat(SICK, 35730, 2.6, 0.8, blotch=hexc("#3a3030"), blotch_p=0.12)
    C.build_legs(m, GOWN, seed=35740, bare=feet)
    return m


P_IDLE = {"body": [16, 0, 4], "head": [-10, 10, 14], "right_arm": [-6, 0, 6], "left_arm": [-10, 0, -10],
          "right_forearm": [-14, 0, 0], "left_forearm": [-24, 0, 0], "right_fingers": [40, 0, 0], "left_fingers": [50, 0, 0],
          "right_leg": [-6, 0, 2], "left_leg": [4, 0, -2], "right_shin": [8, 0, 0], "gown_f": [-4, 0, 0], "@root": [0, -0.4, 0]}


def strap_sway(t, amp=1.0):
    return {"strap_r": [10 * amp * math.sin(t * 6.1), 0, 8 * amp * math.sin(t * 3.7)],
            "strap_l": [10 * amp * math.sin(t * 5.3 + 1), 0, -8 * amp * math.sin(t * 4.1 + 2)]}


def patient_anims():
    f = AnimFile()
    a = "animation.possessed_patient."
    idle = Clip(f, a + "idle", 3.0, loop=True)
    for i, t in enumerate((0, 0.6, 1.2, 1.25, 1.3, 1.9, 2.5, 3.0)):
        b = math.sin(t / 3.0 * 2 * math.pi)
        j = [0, 0, 0] if t in (0, 3.0) else jitter("pidle", i, 14 if 1.2 <= t <= 1.3 else 3)
        idle.key(t, merge(P_IDLE, strap_sway(t, 0.5), {"head": [-10 + j[0], 10 + 8 * b + j[1], 14 + j[2]],
                                                       "body": [16 + 3 * b, j[1] * 0.3, 4]}), E_IO if t not in (1.25, 1.3) and t else None)
    idle.done()
    walk = Clip(f, a + "walk", 1.2, loop=True)
    # A shamble: the right leg steps, the left drags; the body lurches.
    for t, s in ((0, 1), (0.3, 0), (0.6, -1), (0.9, 0), (1.2, 1)):
        walk.key(t, merge(P_IDLE, strap_sway(t * 4, 1.2), {
            "right_leg": [-26 * s if s > 0 else 14, 0, 2], "right_shin": [24 if s > 0 else 4, 0, 0],
            "left_leg": [10 * s if s > 0 else -6, 0, -4], "left_shin": [6, 0, 0], "body": [20, 8 * s, 6 * s],
            "head": [-14, -8 * s, 14 + 6 * s], "right_arm": [10 * s, 0, 6], "left_arm": [-10 * s - 10, 0, -10],
            "gown_f": [-10 * max(0, s), 0, 0], "gown_b": [8, 0, 0]}), E_IO if t else None)
    walk.anim.pos("body", (0, [0, 0, 0]), (0.3, [0, 0.7, 0], E_OUT), (0.6, [0, -0.4, 0], E_IN), (0.9, [0, 0.3, 0]), (1.2, [0, 0, 0], E_IN))
    walk.done()
    # Lunge: a crouch, then the spring with both hands out clawing (tick 9).
    lu = Clip(f, a + "lunge", 0.9)
    lu.key(0, P_IDLE)
    crouch = {"body": [34, 0, 0], "head": [-30, 0, 0], "right_arm": [10, 0, 20], "left_arm": [10, 0, -20], "right_forearm": [-30, 0, 0],
              "left_forearm": [-30, 0, 0], "right_leg": [-40, 0, 6], "left_leg": [-20, 0, -6], "right_shin": [60, 0, 0],
              "left_shin": [50, 0, 0], "@root": [0, -2.4, 1.0], "right_fingers": [60, 0, 0], "left_fingers": [60, 0, 0]}
    lu.key(0.3, merge(crouch, strap_sway(0.3, 1)), E_OUT)
    spring = {"body": [30, 0, 0], "head": [-36, 0, 0], "right_arm": [-110, 0, 14], "left_arm": [-110, 0, -14], "right_forearm": [-6, 0, 0],
              "left_forearm": [-6, 0, 0], "right_fingers": [-30, 0, 0], "left_fingers": [-30, 0, 0], "right_index": [-30, 0, 0],
              "left_index": [-30, 0, 0], "right_leg": [-30, 0, 0], "left_leg": [30, 0, 0], "left_shin": [30, 0, 0],
              "@root": [0, 0.8, -4.0], "gown_f": [-20, 0, 0], "gown_b": [24, 0, 0], **strap_sway(0.45, 2)}
    lu.key(0.45, spring, E_SNAP)
    lu.key(0.6, merge(spring, {"@root": [0, 0, -3.0]}))
    lu.key(0.9, P_IDLE, E_IO)
    lu.done()
    # Twitch: the thing inside jerks him about -- snaps of the head and shoulders, no easing.
    tw = Clip(f, a + "twitch", 1.2)
    tw.key(0, P_IDLE)
    for i in range(1, 12):
        t = round(i * 0.1, 2)
        j = jitter("twitch", i, 26 if i % 3 else 10)
        tw.key(t, merge(P_IDLE, strap_sway(t * 8, 1.5), {"head": [-10 + j[0], 10 + j[1] * 2, 14 + j[2] * 1.5],
                                                       "body": [16 + j[1] * 0.5, j[2] * 0.4, 4 + j[0] * 0.4],
                                                       "right_arm": [-6 + j[2], 0, 6 + abs(j[0])], "left_arm": [-10 - j[1], 0, -10 - abs(j[2])],
                                                       "right_fingers": [40 - j[0], 0, 0]}))
    tw.key(1.2, P_IDLE)
    tw.done()
    return f


def generate():
    for rig_fn, anim_fn, name, seed in ((suit_rig, suit_anims, "crowley_suit", 35600), (patient_rig, patient_anims, "possessed_patient", 35700)):
        m = rig_fn()
        t, g = m.build(gutter=1, seed=seed)
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        write_compact(anim_fn(), ANIM + name + ".animation.json")


if __name__ == "__main__":
    generate()
