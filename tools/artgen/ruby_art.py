"""Ruby (v0.40): one rig for the story NPC (`ruby_npc`) and the fight (`ruby`), texture + glowmask, every clip (names and timings
in RubyAssets) and her flask (`rubys_flask`, the item).

A young woman (30 px to the crown, slim): long straight dark brown hair parted in the middle, down past her shoulders; pale.
A fitted black leather jacket, open, over a dark red top; dark jeans; black boots to mid-calf. Her knife -- the curved,
engraved demon-killing blade of the `rubys_knife` item -- in her right hand (`knife`, shown in the fight) or sheathed at her
right hip (`sheath` with its hilt `sheath_knife`, shown on the NPC). Her eyes go black over a faint red rim (`eyes`).

The flask she gives (`flask`, left hand) shows only in `give`: every loop keys its scale to 0 and `give` to 1 (the action
controller's keys win over the base loop's while it plays).

Skeleton: apoc_vessel's (no coat) + hair -- hair_back -- hair_tip, hair_r, hair_l (head); sheath -- sheath_knife (body);
knife (right_hand); flask (left_hand).
"""

import math
import os

import horsemen_art as H
from angel_art import open_front
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, eye_shell, hair_m, head, jitter, leather_m, legs, neck,
                         skeleton, skin_m, tone, torso)
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, art, save
from michael_art import Clip, P_, fabric, write_compact
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "ruby"
A = "animation.ruby."

B = Body(HIP=12.0, NECK=22.4, TW=3.3, TD=1.75, AW=2.8, SY=21.7, ELBOW=17.4, WRIST=13.3, PALM=11.6, LX=1.7, LW=3.2,
         KNEE=6.2, SHOE_H=1.4)

SKIN = Ramp("#5e4640", "#8c6c62", "#b28f83", "#cba89b", "#dcbdb1", "#ecd4c9")
HAIR = Ramp("#0e0806", "#1a0f0a", "#28170f", "#372016", "#472b1d", "#5a3726")
LEATHER = Ramp("#050506", "#0b0b0d", "#131316", "#1c1c20", "#29292f", "#45454f")
TOP = Ramp("#2a0306", "#43060b", "#5e0b12", "#7a121a", "#931c22", "#aa2a2c")
JEANS = Ramp("#0e1118", "#161b26", "#202736", "#2b3446", "#384357", "#4a566c")
BOOT = Ramp("#030303", "#08080a", "#0f0f12", "#18181c", "#25252b", "#4a4a55")
LIPS = Ramp("#4c2a2a", "#6a3a3a", "#874a48", "#9d5a56", "#b06a64", "#c27c74")
STEEL = Ramp("#26282d", "#474b52", "#6e737b", "#959ba3", "#bcc1c8", "#e8ebef")
BRASS = Ramp("#3a2a10", "#5c4418", "#806024", "#a47e34", "#c49c4c", "#dcbc70")
GRIP = Ramp("#120a06", "#1f130b", "#2e1c10", "#3f2716", "#52331d", "#664026")
EYE_WHITE, IRIS = hexc("#d9d1c7"), hexc("#3a2414")
BLACK, RIM, RIM_DIM = hexc("#030203"), hexc("#a0101a"), hexc("#4a060a")

# x = 0 is the viewer's left (her right); rows from the crown down. Long hair parted in the middle frames the face.
FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhPhhhhhhhh",
    "hhhhhhsPshhhhhhh",
    "hhhhhsssssshhhhh",
    "hhhssssssssssshh",
    "hhsssssssssssshh",
    "hhsbbbssssbbbshh",
    "hhuwIIwsswIIwuhh",
    "hhsllssssssllshh",
    "hhsssssssssssshh",
    "hhsssssnnssssshh",
    "hhsssssNNssssshh",
    "hhsssssssssssshh",
    "hhssssLMMLsssshh",
    "hhsssssssssssshh",
    "hhhsssssssssshhh",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=41001):
    sk = skin_m(SKIN, seed, 3.2, 0.3)
    hr = hair_m(HAIR, seed + 1, base=2.6)
    lg = {"s": sk, "h": hr, "P": HAIR[0], "b": HAIR[1], "u": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.86),
          "l": lambda f, x, y, w, h: HAIR[1], "w": EYE_WHITE, "I": IRIS, "n": SKIN[4], "N": SKIN[1],
          "M": LIPS[3], "L": LIPS[1], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}
    lg["l"] = HAIR[0]     # the lash line under the eyes' hood
    sides = H.head_sides(sk, hr, top=6, back=16, temple=5, sideburn=12, ear=False)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: (None if f == "north" and not (y < 1 or abs(x - 7.5) > 5.5 - y * 0.6) else hr(f, x, y, w, h)),
         shell_inflate=0.32, shell_rows=3.0)
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - 1.0, B.NECK + B.HEAD * 0.62, -4.1), (2.0, 0.35, 0.2), solid(HAIR[1]), density=4, tag="brow",
               rotation=(0, 0, 8 * s), pivot=(s * 1.0, B.NECK + 5.0, -4.1))
    eye_shell(m, B, "rKKKr..rKKKr", {"r": RIM_DIM, "K": BLACK})
    neck(m, B, sk, 2.8)
    # The long hair: down the back from under the crown, side locks over the shoulders in front.
    hb = m.bone("hair", (0, B.NECK + 6, 1.5), "head")
    back = m.bone("hair_back", (0, B.NECK + 0.5, 3.4), "hair")
    tip = m.bone("hair_tip", (0, B.NECK - 6.5, 3.6), "hair_back")
    strand = lambda f, x, y, w, h: tone(HAIR, 2.6 + 0.9 * math.sin(x * 1.7 + seed) - 0.6 * (y / max(1, h))
                                        + (0.6 if (x * 5 + y) % 9 == 0 else 0))
    m.cube(hb, (-4.2, B.NECK + 0.5, 1.6), (8.4, 6.0, 2.6), none_on(("up",), strand), density=2, tag="hair_volume")
    m.cube(back, (-4.0, B.NECK - 6.5, 2.4), (8.0, 7.0, 1.4), strand, density=2, tag="hair_fall")

    def ends(f, x, y, w, h):
        if f in ("north", "south", "east", "west") and y > h - 3 and h01("tip", seed, f, x) < 0.5 + (y - (h - 3)) * 0.2:
            return None
        return strand(f, x, y, w, h)
    m.cube(tip, (-3.6, B.NECK - 10.5, 2.5), (7.2, 4.0, 1.2), ends, density=2, tag="hair_ends")
    for name, s in (("hair_r", -1), ("hair_l", 1)):
        lock = m.bone(name, (s * 3.6, B.NECK + 4, -1.0), "head")
        m.cube(lock, (s * 3.9 - 0.9, B.NECK - 4.0, -2.6), (1.8, 8.5, 2.6), ends, density=2, tag="hair_lock",
               rotation=(4, 0, -4 * s), pivot=(s * 3.9, B.NECK + 4, -1.0))


def knife_blade(m, bone, x, y0, z0, length=4.6, along="z"):
    """The demon-killing knife: a curved, engraved blade, brass guard, a dark wrapped grip. Along -Z from (x, y0, z0)."""
    def blade(f, xx, yy, w, h):
        if f in ("east", "west"):
            k = xx / max(1, w - 1)
            if yy == 0:
                return STEEL[5]
            if (xx + yy * 2) % 5 == 0 and 0 < yy < h - 1:
                return STEEL[2]                 # the engraving
            return tone(STEEL, 4.2 - 0.8 * yy - 0.6 * k)
        return STEEL[4]
    m.cube(bone, (x - 0.15, y0 - 0.5, z0 - 2.2), (0.3, 1.0, 2.6), none_on(("down",), solid(GRIP[3])) if False else
           (lambda f, xx, yy, w, h: tone(GRIP, 3 + (xx + yy) % 2)), density=4, tag="grip")
    m.cube(bone, (x - 0.5, y0 - 0.6, z0 - 2.6), (1.0, 1.2, 0.4), lambda f, xx, yy, w, h: tone(BRASS, 3.6 - yy * 0.5), density=4,
           tag="guard")
    m.cube(bone, (x - 0.1, y0 - 0.5, z0 - 2.6 - length), (0.2, 1.0, length), blade, density=4, tag="blade",
           rotation=(-6, 0, 0), pivot=(x, y0, z0 - 2.6))
    m.cube(bone, (x - 0.1, y0 - 0.15, z0 - 3.5 - length), (0.2, 0.6, 1.0), solid(STEEL[4]), density=4, tag="blade_tip",
           rotation=(-16, 0, 0), pivot=(x, y0, z0 - 2.6 - length))


def build_body(m, seed=41200):
    top = fabric(TOP, seed + 1, 3.0, 0.3)

    def top_front(f, x, y, w, h):
        if f == "north" and y < 3 and abs(x - (w - 1) / 2) < 2.5 - y * 0.6:
            return shade(skin_m(SKIN, seed + 9, 3.2, 0.3)(f, x, y, w, h), 0.95)   # the scoop neck
        return top(f, x, y, w, h)
    torso(m, B, top_front, tag="torso")
    lea = leather_m(LEATHER, seed + 2, 2.8, 1.0)
    torso(m, B, open_front(lea, half=2.2, collar=4, zipper=hexc("#5a5d64"), lapel=LEATHER[4]), inflate=0.3, bottom=B.HIP + 0.4,
          tag="jacket")
    # The jacket's collar, turned down; a belt.
    for s in (-1, 1):
        m.cube("body", (s * 2.0 - 1.0, B.NECK - 1.3, -B.TD - 0.45), (2.0, 1.5, 0.4), lambda f, x, y, w, h: tone(LEATHER, 4.2 - y),
               density=2, tag="jacket_collar", rotation=(16, 0, -26 * s), pivot=(s * 2.0, B.NECK - 0.2, -B.TD - 0.3))
    m.cube("body", (-B.TW, B.HIP - 0.4, -B.TD), (2 * B.TW, 1.0, 2 * B.TD), lambda f, x, y, w, h: hexc("#8a8c90") if f == "north"
           and 5 <= x <= 8 else LEATHER[2], inflate=0.32, density=2, tag="belt")
    skin = skin_m(SKIN, seed + 3, 3.1, 0.3)
    lea_sleeve = leather_m(LEATHER, seed + 4, 2.7, 0.9)
    cuff = lambda f, x, y, w, h: LEATHER[4] if f not in ("up", "down") else LEATHER[0]
    arms(m, B, lea_sleeve, skin, cuff=cuff, cuff_depth=0.8, sleeve_inflate=0.25)
    legs(m, B, fabric(JEANS, seed + 5, 2.8, 0.3), BOOT, boot_h=4.2, toe=0.9)
    # The sheath at her right hip, its hilt standing out (the NPC's knife).
    sh = m.bone("sheath", (-B.TW - 0.6, B.HIP, -0.2), "body")
    m.cube(sh, (-B.TW - 1.1, B.HIP - 4.6, -0.9), (0.9, 5.2, 1.6), lambda f, x, y, w, h: tone(LEATHER, 3.2 - y * 0.2), density=2,
           tag="sheath", rotation=(0, 0, 8), pivot=(-B.TW - 0.6, B.HIP, 0))
    hilt = m.bone("sheath_knife", (-B.TW - 0.6, B.HIP + 0.6, -0.2), "sheath")
    m.cube(hilt, (-B.TW - 1.0, B.HIP + 0.6, -0.8), (0.7, 2.2, 1.0), lambda f, x, y, w, h: tone(GRIP, 3 + (x + y) % 2), density=4,
           tag="hilt", rotation=(0, 0, 8), pivot=(-B.TW - 0.6, B.HIP, 0))
    m.cube(hilt, (-B.TW - 1.2, B.HIP + 0.4, -1.0), (1.1, 0.4, 1.4), solid(BRASS[3]), density=4, tag="hilt_guard",
           rotation=(0, 0, 8), pivot=(-B.TW - 0.6, B.HIP, 0))
    # Her knife in hand (the fight).
    kn = m.bone("knife", (-B.AX, B.PALM - 0.2, -0.2), "right_hand")
    knife_blade(m, kn, -B.AX, B.PALM - 0.2, 1.0)
    # The flask in her left hand (only `give` shows it).
    fl = m.bone("flask", (B.AX, B.PALM - 0.6, -0.4), "left_hand")

    def flask(f, x, y, w, h):
        c = tone(STEEL, 3.8 - 0.5 * y / max(1, h) + (0.8 if x == 1 else 0))
        if h01("dent", x, y, f) < 0.08:
            c = shade(c, 0.75)
        return c
    m.cube(fl, (B.AX - 0.6, B.PALM - 3.4, -1.6), (1.2, 3.4, 2.4), flask, density=4, tag="flask")
    m.cube(fl, (B.AX - 0.3, B.PALM, -1.0), (0.6, 0.6, 0.6), lambda f, x, y, w, h: hexc("#3a0a0a") if y == h - 1 else STEEL[2],
           density=4, tag="flask_neck")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=False)
    build_head(m)
    build_body(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

HOLD = {"right_fingers": [80, 0, 0], "right_index": [70, 0, 0], "right_middle": [75, 0, 0], "right_thumb": [-20, 0, 20]}
NO_FLASK = {"%flask": [0, 0, 0]}
# Weight on her left leg, right hip out, arms loose.
IDLE = P_(HOLD, {"body": [0, 0, -2], "right_leg": [0, 0, 4], "left_leg": [0, 0, -1], "right_shin": [6, 0, 0],
                 "right_arm": [-6, 0, 6], "right_forearm": [-20, 0, 0], "left_arm": [0, 0, -4], "left_forearm": [-12, 0, 0],
                 "left_fingers": [20, 0, 0], "head": [0, 0, 4]})
CROSSED = {"right_arm": [-26, -20, -12], "left_arm": [-30, 20, 12], "right_forearm": [-96, 40, 0], "left_forearm": [-90, -44, 0],
           "right_hand": [0, 0, 10], "left_hand": [0, 0, -10], "right_fingers": [40, 0, 0], "left_fingers": [40, 0, 0],
           "body": [-2, 0, 3], "right_leg": [0, 0, 5], "right_shin": [8, 0, 0], "head": [-2, -8, 6]}
GUARD = P_(HOLD, {"right_arm": [-40, 10, 8], "right_forearm": [-40, 0, 0], "left_arm": [-30, -10, -10], "left_forearm": [-60, 0, 0],
                  "body": [8, -10, 0], "head": [-8, 10, 0], "right_leg": [-14, 0, 4], "left_leg": [10, 0, -4], "right_shin": [16, 0, 0],
                  "@root": [0, -0.4, 0]})


def hair_sway(t, amp=1.0):
    ph = t * 2 * math.pi
    return {"hair_back": [3 * amp * math.sin(ph), 0, 2 * amp * math.sin(ph * 0.5)], "hair_tip": [4 * amp * math.sin(ph - 0.7), 0, 0],
            "hair_r": [2 * amp * math.sin(ph + 0.4), 0, 0], "hair_l": [2 * amp * math.sin(ph + 1.1), 0, 0]}


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("knife", "sheath", "sheath_knife", "eyes")))
        return clips[-1]

    # --- loops (the flask hidden in every one) ---------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = math.sin(t / 4.0 * 2 * math.pi)
        c.key(t, P_(IDLE, NO_FLASK, hair_sway(t / 4.0, 0.5), {"head": [-1 + b, 8 * b, 4], "@body": [0, -0.12 * abs(b), 0]}),
              E_IO if i else None)

    c = clip("walk", 1.0, loop=True)
    for i, (t, s) in enumerate(((0, 1), (0.5, -1), (1.0, 1))):
        c.key(t, P_(HOLD, NO_FLASK, hair_sway(t, 1.0), {"right_leg": [24 * s, 0, 0], "left_leg": [-24 * s, 0, 0],
                                                        "right_shin": [6 if s < 0 else 20, 0, 0], "left_shin": [20 if s < 0 else 6, 0, 0],
                                                        "right_arm": [-16 * s, 0, 4], "left_arm": [16 * s, 0, -4],
                                                        "right_forearm": [-18, 0, 0], "left_forearm": [-14, 0, 0],
                                                        "body": [0, 8 * s, 0], "head": [-2, -6 * s, 0]}), E_IO if i else None)
    c.key(0.25, P_(c.frames[0][1], {"@body": [0, 0.5, 0]}), E_OUT)
    c.key(0.75, P_(c.frames[1][1], {"@body": [0, 0.5, 0]}), E_OUT)

    c = clip("run", 0.6, loop=True)
    for i, (t, s) in enumerate(((0, 1), (0.3, -1), (0.6, 1))):
        c.key(t, P_(HOLD, NO_FLASK, hair_sway(t / 0.6, 2.0), {"right_leg": [44 * s, 0, 0], "left_leg": [-44 * s, 0, 0],
                                                              "right_shin": [10 if s < 0 else 60, 0, 0], "left_shin": [60 if s < 0 else 10, 0, 0],
                                                              "right_arm": [-40 * s, 0, 6], "left_arm": [40 * s, 0, -6],
                                                              "right_forearm": [-60, 0, 0], "left_forearm": [-60, 0, 0],
                                                              "body": [14, 10 * s, 0], "head": [-12, -8 * s, 0],
                                                              "hair_back": [20, 0, 0], "hair_tip": [20, 0, 0]}), E_IO if i else None)
    c.key(0.15, P_(c.frames[0][1], {"@body": [0, 1.0, 0]}), E_OUT)
    c.key(0.45, P_(c.frames[1][1], {"@body": [0, 1.0, 0]}), E_OUT)

    c = clip("arms_crossed", 4.0, loop=True)
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = math.sin(t / 4.0 * 2 * math.pi)
        c.key(t, P_(CROSSED, NO_FLASK, hair_sway(t / 4.0, 0.4), {"head": [-2 + 2 * b, -8 + 10 * b, 6], "@body": [0, -0.1 * abs(b), 0]}),
              E_IO if i else None)

    # --- the NPC -----------------------------------------------------------------------------------------------------
    # Appear (8): out of the smoke, a flick of the hair as she straightens.
    c = clip("appear", 0.9)
    c.key(0, P_(IDLE, {"@root": [0, -0.6, 0], "body": [16, 0, 0], "head": [20, 0, 0], "right_arm": [10, 0, 20], "left_arm": [10, 0, -20]}))
    c.key(0.3, P_(IDLE, {"body": [-4, 0, 0], "head": [-14, -20, 0], "hair_back": [-30, 0, 10], "hair_tip": [-20, 0, 0],
                         "hair_r": [-20, 0, 20], "hair_l": [-20, 0, -10]}), E_OUT)
    c.key(0.4, P_(IDLE, {"head": [-6, 16, 4], "hair_back": [20, 0, -12], "hair_tip": [16, 0, 0]}), E_SNAP)
    c.key(0.9, IDLE, E_IO)

    # Vanish (10): a half turn away as the smoke takes her.
    c = clip("vanish", 1.0, hold=True)
    c.key(0, IDLE)
    c.key(0.5, P_(IDLE, {"@root": [0, 0, 0], "body": [0, 60, 0], "head": [0, 40, 0], "hair_back": [10, 0, 14]}), E_IO)
    c.key(1.0, P_(IDLE, {"body": [0, 150, 0], "head": [0, 20, 0], "hair_back": [20, 0, 20], "@root": [0, -0.4, 0]}), E_IN)

    # Talk (8): a hand gesture, palm open, a nod.
    c = clip("talk", 1.5)
    gest = lambda k, hz: {"left_arm": [-24 * k, 0, -12 * k], "left_forearm": [-66 * k, 10, 0], "left_hand": [0, 60 * k, hz],
                          "left_fingers": [10, 0, 0], "head": [-4 + 4 * k, 10 * k, -4 * k]}
    c.key(0, IDLE)
    c.key(0.25, P_(IDLE, gest(0.9, 10)), E_IO)
    c.key(0.4, P_(IDLE, gest(1.0, -6), {"head": [6, 8, -2]}), E_OUT)
    c.key(0.8, P_(IDLE, gest(0.8, 8)), E_IO)
    c.key(1.1, P_(IDLE, gest(1.0, -4), {"head": [2, -6, 4]}), E_IO)
    c.key(1.5, IDLE, E_IO)

    # Give (12): the flask held out in the left hand -- the only clip that shows it.
    c = clip("give", 1.3)
    out = P_(IDLE, {"left_arm": [-72, -10, 4], "left_forearm": [-14, 0, 0], "left_hand": [0, -70, 0], "left_fingers": [70, 0, 0],
                    "left_index": [60, 0, 0], "left_middle": [65, 0, 0], "left_thumb": [-20, 0, -20], "body": [4, -8, 0],
                    "head": [6, 6, 0], "%flask": [1, 1, 1]})
    c.key(0, P_(IDLE, {"%flask": [0, 0, 0]}))
    c.key(0.15, P_(IDLE, {"left_arm": [10, 0, -4], "%flask": [1, 1, 1]}), E_OUT)
    c.key(0.6, out, E_IO)
    c.key(0.9, out)
    c.key(1.15, P_(IDLE, {"left_arm": [-20, 0, -4], "%flask": [1, 1, 1]}), E_IO)
    c.key(1.3, P_(IDLE, {"%flask": [0, 0, 0]}), E_SNAP)

    # Laugh (10): the reveal -- head back, arms open.
    c = clip("laugh", 2.0)
    open_ = {"head": [-30, 0, 0], "body": [-10, 0, 0], "right_arm": [-20, 0, 50], "left_arm": [-20, 0, -50], "right_forearm": [-20, 0, 0],
             "left_forearm": [-20, 0, 0], "right_hand": [0, 40, 0], "left_hand": [0, -40, 0], "hair_back": [-16, 0, 0]}
    c.key(0, IDLE)
    c.key(0.5, P_(HOLD, open_), E_OUT)
    for i, t in enumerate((0.7, 0.9, 1.1, 1.3)):
        c.key(t, P_(HOLD, open_, {"head": [-30 + (6 if i % 2 else -2), 0, 0], "@body": [0, 0.3 if i % 2 else 0, 0]}))
    c.key(2.0, IDLE, E_IO)

    # --- the fight ---------------------------------------------------------------------------------------------------
    # Slash (6): a fast forehand cut, right arm across from her right to her left.
    c = clip("slash", 0.6)
    wind = P_(GUARD, {"right_arm": [-60, 40, 60], "right_forearm": [-40, 0, 0], "body": [8, 26, 0], "head": [-8, -14, 0]})
    cut = P_(GUARD, {"right_arm": [-80, -30, -40], "right_forearm": [-10, 0, 0], "body": [10, -30, 0], "head": [-6, 18, 0],
                     "@root": [0, -0.4, -1.0], "hair_back": [10, 0, 16]})
    c.key(0, IDLE)
    c.key(0.2, wind, E_OUT)
    c.key(0.3, cut, E_SNAP)
    c.key(0.4, P_(cut, {"right_arm": [-74, -36, -44]}), E_OUT)
    c.key(0.6, IDLE, E_IO)

    # Lunge (10): a step and a stab.
    c = clip("lunge", 0.9)
    coil = P_(GUARD, {"right_arm": [20, 0, 10], "right_forearm": [-90, 0, 0], "body": [16, -16, 0], "@root": [0, -0.8, 1.0]})
    stab = P_(HOLD, {"right_arm": [-92, 0, 4], "right_forearm": [0, 0, 0], "left_arm": [20, 0, -20], "body": [22, 16, 0],
                     "head": [-16, -10, 0], "right_leg": [-50, 0, 0], "right_shin": [40, 0, 0], "left_leg": [30, 0, 0],
                     "@root": [0, -1.6, -4.0], "hair_back": [30, 0, 0], "hair_tip": [16, 0, 0]})
    c.key(0, IDLE)
    c.key(0.35, coil, E_IO)
    c.key(0.5, stab, E_SNAP)
    c.key(0.65, P_(stab, {"@root": [0, -1.5, -4.4]}), E_OUT)
    c.key(0.9, IDLE, E_IO)

    # Kick (9): a spinning kick -- a full turn, the right leg swung out at the hit.
    c = clip("kick", 0.8)
    c.key(0, IDLE)
    c.key(0.2, P_(GUARD, {"root": [0, -120, 0], "body": [6, 0, 0]}), E_IN)
    c.key(0.45, P_(HOLD, {"root": [0, -300, 0], "right_leg": [-80, 0, 30], "right_shin": [10, 0, 0], "left_leg": [0, 0, -6],
                          "body": [-6, 0, -24], "right_arm": [-20, 0, 50], "left_arm": [-20, 0, -60], "hair_back": [-20, 0, -30],
                          "hair_tip": [-10, 0, -20], "@root": [0, 0.6, 0]}), E_OUT)
    c.key(0.6, P_(GUARD, {"root": [0, -360, 0]}), E_IO)
    c.key(0.8, IDLE, E_IO)

    # Shove (8): a flat palm thrust -- telekinesis.
    c = clip("shove", 0.8)
    c.key(0, IDLE)
    c.key(0.25, P_(IDLE, {"left_arm": [-40, 0, -10], "left_forearm": [-90, 0, 0], "body": [-4, 16, 0], "head": [-4, -10, 0]}), E_IO)
    c.key(0.4, P_(IDLE, {"left_arm": [-90, 0, 0], "left_forearm": [0, 0, 0], "left_hand": [-70, 0, 0], "left_fingers": [-20, 0, 0],
                         "body": [6, -18, 0], "head": [-8, 12, 0], "@root": [0, 0, -0.8], "hair_back": [16, 0, 0]}), E_SNAP)
    c.key(0.55, P_(IDLE, {"left_arm": [-86, 0, 0], "left_hand": [-70, 0, 0], "body": [6, -16, 0]}), E_OUT)
    c.key(0.8, IDLE, E_IO)

    # Smoke dash (6): a crouch and a blur forward.
    c = clip("smoke_dash", 0.7)
    crouch = P_(GUARD, {"@root": [0, -2.0, 0.8], "body": [24, 0, 0], "head": [-20, 0, 0], "right_leg": [-40, 0, 0], "left_leg": [-20, 0, 0],
                        "right_shin": [60, 0, 0], "left_shin": [50, 0, 0]})
    c.key(0, IDLE)
    c.key(0.2, crouch, E_OUT)
    c.key(0.3, P_(crouch, {"@root": [0, -0.8, -6.0], "body": [30, 0, 0], "hair_back": [50, 0, 0], "hair_tip": [30, 0, 0],
                           "right_arm": [20, 0, 10], "left_arm": [20, 0, -10]}), E_SNAP)
    c.key(0.45, P_(GUARD, {"@root": [0, -0.6, -2.0]}), E_OUT)
    c.key(0.7, IDLE, E_IO)

    # Hurt (2): a flinch.
    c = clip("hurt", 0.5)
    c.key(0, IDLE)
    c.key(0.1, P_(IDLE, {"body": [-10, 8, 0], "head": [-16, 10, 0], "right_arm": [-10, 0, 24], "left_arm": [-30, 0, -20],
                         "@root": [0, 0, 0.8], "hair_back": [-14, 0, 0]}), E_SNAP)
    c.key(0.5, IDLE, E_IO)

    # Death (50): the knife goes in; she shudders; the demon-light flare and the fall.
    c = clip("death", 4.0, hold=True)
    hit = {"body": [24, 0, 0], "head": [18, 0, 0], "right_arm": [-40, 0, -30], "left_arm": [-40, 0, 30],
           "right_forearm": [-60, -30, 0], "left_forearm": [-60, 30, 0], "@root": [0, -0.6, 0.8]}
    c.key(0, IDLE)
    c.key(0.3, hit, E_SNAP)
    for i in range(1, 9):
        t = round(0.3 + 0.2 * i, 2)
        k = (t - 0.3) / 1.8
        j = jitter("ruby-death", i, 3 + 6 * k)
        c.key(t, P_(hit, {"body": [24 - 40 * k + j[0], j[1], j[2]], "head": [18 - 52 * k + j[2], j[0], j[1]],
                          "right_arm": [-40 + 30 * k, 0, -30 + 105 * k], "left_arm": [-40 + 30 * k, 0, 30 - 105 * k],
                          "right_forearm": [-60 + 50 * k, 0, 0], "left_forearm": [-60 + 50 * k, 0, 0], "@root": [0, -0.6 + 0.8 * k, 0.8]}))
    flare = {"body": [-16, 0, 0], "head": [-48, 0, 0], "right_arm": [-14, 0, 82], "left_arm": [-14, 0, -82],
             "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "@root": [0, 0.4, 0], "hair_back": [-24, 0, 0], "hair_tip": [-14, 0, 0]}
    c.key(2.5, flare, E_BACK)
    c.key(2.9, P_(flare, {"head": [-52, 4, 0]}))
    fall = {"@root": [0, -0.8, 5.0], "body": [-82, 0, 0], "head": [-10, 24, 0], "right_arm": [-170, 0, 34], "left_arm": [-150, 0, -50],
            "right_leg": [8, 0, 6], "left_leg": [14, 0, -6], "hair_back": [-60, 0, 0], "hair_tip": [-20, 0, 0]}
    c.key(3.5, fall, E_IN)
    c.key(4.0, fall)

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The flask (item)
# =====================================================================================================

def flask_item():
    pal = {"d": STEEL[1], "m": STEEL[2], "s": STEEL[3], "S": STEEL[4], "W": STEEL[5], "c": STEEL[0], "r": hexc("#3e0a0a"),
           "R": hexc("#6a1010"), "k": hexc("#1a1a1e")}
    t = art([
        "................",
        "......kkk.......",
        "......mSm.......",
        ".....rmmmr......",
        "....dsSWSsmd....",
        "...dsSWWSssmd...",
        "...dsSWSssmmd...",
        "...dsSSssmmmd...",
        "...dssscsmmmd...",
        "...dsssssmmmd...",
        "...dsScsssmmd...",
        "...dsSssssmmd...",
        "...dsSSsssmmd...",
        "....dsssssmd....",
        ".....ddddddd....",
        "................",
    ], pal)
    item_outline(t, hexc("#0c0b0d"))
    return t


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=41000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    save(flask_item(), "item", "rubys_flask")


if __name__ == "__main__":
    generate()
