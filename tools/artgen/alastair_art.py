"""Alastair, Hell's torturer (v0.40): his rig, texture + glowmask and clips (names and timings in AlastairAssets), the rack
(`alastair_rack`, its own rig and clips), the chain-hook (`alastair_hook`), the thrown scalpel's sprite, the scalpel item and
his trophy bust (`block/alastair_trophy`, on the trophies' shared base). His bar is in alastair_gui_art.

A lean, tall older man (33 px to the crown): close-cropped grey hair, receding; a narrow pale face, deep lines at the eyes
and mouth, a thin smile (the corner on his left raised). A dark grey overcoat, open, to below the knee, over a black suit;
a white shirt buttoned to the neck, no tie. Black gloves. A rolled leather case of instruments hangs at his left hip
(`tools`); a long scalpel in his right fist (`scalpel`, hidden while he is bound). His eyes go white (`eyes`: a shell lit in
the glowmask, the renderer shows it while they are lit).

Skeleton: apoc_vessel's (coat, fingers) + `tools` (body) and `scalpel` (right_hand).
"""

import math
import os

import blockmodel as BM
import horsemen_art as H
import palette as P
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, SIDES, arms, coat, eye_shell, glove_m, hair_m, head, jitter,
                         legs, neck, skeleton, skin_m, tone, torso, wool_m)
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, art, save
from michael_art import Clip, P_, fabric, write_compact
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "alastair"
A = "animation.alastair."

B = Body(HIP=13.0, NECK=25.4, TW=3.7, TD=1.95, AW=3.2, SY=24.6, ELBOW=19.6, WRIST=14.8, PALM=12.9, LX=1.85, LW=3.5,
         KNEE=6.9, SHOE_H=1.3, HEM=4.4)

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#5a4540", "#86695f", "#a98a7f", "#c2a498", "#d5bbb0", "#e6d2c8")
HAIR = Ramp("#2e2c2b", "#4a4745", "#686461", "#88837f", "#a7a29d", "#c6c1bb")
COAT = Ramp("#121214", "#1b1b1e", "#25252a", "#303036", "#3d3d44", "#515159")
SUIT = Ramp("#050506", "#0a0a0c", "#111114", "#19191d", "#232328", "#303037")
SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
GLOVE = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3c3c45")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
ROLL = Ramp("#1e110a", "#2e1a10", "#40261a", "#553423", "#6b442e", "#82573c")
STEEL = Ramp("#24272c", "#454a52", "#6b717a", "#9198a2", "#bac0c8", "#e6eaee")
BONE = Ramp("#5e5544", "#857a63", "#a89c82", "#c4b99e", "#d9cfb6", "#ece4d0")
LIPS = Ramp("#4a2c28", "#64403a", "#7c544c", "#93665d", "#a8786e", "#bb8a80")
IRON = Ramp("#0b0b0c", "#161618", "#222225", "#2f2f33", "#414146", "#5c5c63")
RUST = Ramp("#2a1208", "#45200e", "#643016", "#86421e", "#a65a2a", "#c47a40")
BLOOD = hexc("#5c0a0a")
EYE_WHITE, IRIS = hexc("#cfc8be"), hexc("#3a4250")
WHITE_HOT, WHITE, WHITE_DIM = hexc("#ffffff"), hexc("#e8eef4"), hexc("#9aa4b0")

# x = 0 is the viewer's left (his right); rows from the crown down.
FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhssssssssssshh",
    "hsssssssssssssss",
    "sssjjjjssjjjjsss",
    "ssbbbbssssbbbbss",
    "sUUUUUssssUUUUUs",
    "skuwIIwsswIIwuks",
    "sskuussssssuukss",
    "skssssnnssssssks",
    "ssssssnnssssssss",
    "sksssNnnNssssks.",
    "ssksssssssssLkss",
    "skssslllllllLsks",
    "sssjssssssssjsss",
    "ssssjjjjjjjjssss",
]
FACE = [r.replace(".", "s") for r in FACE]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


# =====================================================================================================
# The man
# =====================================================================================================

def build_head(m, seed=40001):
    sk = skin_m(SKIN, seed, 3.1, 0.3)
    hr = hair_m(HAIR, seed + 1, base=2.9)
    lg = {"s": sk, "h": hr, "b": HAIR[2], "U": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.78),
          "u": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.86), "k": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.8),
          "w": EYE_WHITE, "I": IRIS, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}
    sides = H.head_sides(sk, hr, top=3, back=11, temple=8, sideburn=5)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 0 else hr(f, x, y, w, h), shell_inflate=0.15,
         shell_rows=1.5)
    # Thin brows, level and pale.
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - 1.1, B.NECK + B.HEAD * 0.62, -4.1), (2.2, 0.4, 0.2), solid(HAIR[2]), density=4, tag="brow",
               rotation=(0, 0, -4 * s), pivot=(s * 1.0, B.NECK + 5.0, -4.1))
    eye_shell(m, B, "uwWWwsswWWwu".replace("s", "."), {"u": WHITE_DIM, "w": WHITE, "W": WHITE_HOT})
    neck(m, B, sk, 3.2)


def build_suit(m, seed=40200):
    shirt = fabric(SHIRT, seed + 1, 3.6, 0.2)
    suit = fabric(SUIT, seed + 2, 3.0, 0.3)

    def shirt_front(f, x, y, w, h):
        c = shirt(f, x, y, w, h)
        if f == "north" and abs(x - (w - 1) / 2) < 0.6 and y > 1:
            return shade(c, 0.86) if y % 4 else hexc("#e0e0dc")       # the placket, buttoned to the throat
        return c
    torso(m, B, lambda f, x, y, w, h: shirt_front(f, x, y, w, h) if f == "north" else SUIT[1], tag="torso")
    front = H.jacket_front(suit, SUIT[4], shirt_front, tie=None, gap=(5, 10), v_depth=9, buttons=SUIT[0], button_rows=(11, 15))
    torso(m, B, front, inflate=0.25, bottom=B.HIP - 0.6, top=B.NECK - 0.2, tag="jacket", faces_off=("down",))
    # The shirt's collar, standing up round the neck, buttoned.
    for s in (-1, 1):
        m.cube("body", (0.9 * s - 0.8, B.NECK - 1.2, -B.TD - 0.4), (1.6, 1.3, 0.4),
               lambda f, x, y, w, h: tone(SHIRT, 4.6 if y == 0 else 3.8), density=2, tag="shirt_collar",
               rotation=(10, 0, -18 * s), pivot=(0.9 * s, B.NECK - 0.1, -B.TD - 0.3))


def build_tools(m):
    """The roll of instruments at his left hip: a strapped leather case, steel and bone handles standing out of it."""
    t = m.bone("tools", (B.TW + 1.2, B.HIP + 1.6, -0.4), "body")
    lea = lambda f, x, y, w, h: tone(ROLL, 2.8 + 0.6 * math.sin(x * 1.3 + y * 0.7) - 0.8 * y / max(1, h)
                                     + (0.8 if f == "up" else 0))
    x0 = B.TW + 0.65
    m.cube(t, (x0, B.HIP - 1.2, -1.9), (1.4, 4.2, 3.2), lea, density=2, tag="roll", rotation=(0, 0, -6), pivot=(x0, B.HIP + 3, 0))
    m.cube(t, (x0 - 0.05, B.HIP + 0.4, -1.95), (1.5, 0.6, 3.3), solid(ROLL[0]), density=2, tag="roll_strap",
           rotation=(0, 0, -6), pivot=(x0, B.HIP + 3, 0))
    for i, (dz, top, mat) in enumerate(((-1.4, 1.6, STEEL), (-0.6, 2.2, BONE), (0.2, 1.3, STEEL), (0.9, 1.9, BONE))):
        m.cube(t, (x0 + 0.45, B.HIP + 3.0, dz), (0.5, top, 0.45), lambda f, x, y, w, h, mat=mat: tone(mat, 3.6 - y * 0.5),
               density=4, tag="instrument", rotation=(0, 0, -6), pivot=(x0, B.HIP + 3, 0))
    # A loop of the belt it hangs from.
    m.cube(t, (x0 - 0.2, B.HIP + 2.6, -0.4), (0.4, 1.2, 0.8), solid(SUIT[3]), density=2, tag="loop")


def build_scalpel(m):
    """The long scalpel in his right fist: a bone handle along the fist, the blade out in front, a dried red line on its edge."""
    cx = -B.AX
    sc = m.bone("scalpel", (cx, B.PALM - 0.2, -0.2), "right_hand")
    m.cube(sc, (cx - 0.3, B.PALM - 0.65, -1.4), (0.6, 0.7, 3.2), lambda f, x, y, w, h: tone(BONE, 3.4 - 0.4 * (x % 3 == 0)),
           density=4, tag="handle")
    m.cube(sc, (cx - 0.35, B.PALM - 0.7, -1.9), (0.7, 0.8, 0.5), solid(STEEL[2]), density=4, tag="ferrule")

    def blade(f, x, y, w, h):
        if f in ("east", "west"):
            if y == h - 1:
                return BLOOD if (x % 3) else hexc("#7a1212")          # the edge (lower side), dried blood along it
            return tone(STEEL, 4.6 - 0.9 * y)
        return STEEL[5] if f == "up" else STEEL[2]
    m.cube(sc, (cx - 0.12, B.PALM - 0.75, -5.4), (0.25, 0.75, 3.5), blade, density=4, tag="blade")
    m.cube(sc, (cx - 0.12, B.PALM - 0.45, -6.0), (0.25, 0.3, 0.6), solid(STEEL[4]), density=4, tag="tip")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=True)
    build_head(m)
    build_suit(m)
    cl = wool_m(COAT, 40300, 3.0)
    coat(m, B, COAT, cl, 40300, collar_up=False)
    sleeve = wool_m(COAT, 40400, 2.9)
    cuff = lambda f, x, y, w, h: tone(COAT, 3.8 - 1.0 * y / max(1, h)) if f not in ("up", "down") else COAT[0]
    arms(m, B, sleeve, None, cuff=cuff, glove=glove_m(GLOVE, 40401))
    legs(m, B, fabric(SUIT, 40500, 2.9, 0.25, crease=True), SHOE)
    build_tools(m)
    build_scalpel(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

def fist(side):
    return {f"{side}_fingers": [80, 0, 0], f"{side}_index": [70, 0, 0], f"{side}_middle": [75, 0, 0], f"{side}_thumb": [-20, 0, 20]}


GRIP = fist("right")
# Standing at ease, very straight; the scalpel held low by his thigh, point forward; the left hand loose.
IDLE = P_(GRIP, {"right_arm": [-8, 0, 3], "right_forearm": [-24, 0, 0], "left_arm": [-2, 0, -2], "left_forearm": [-12, 0, 0],
                 "left_fingers": [16, 0, 0], "head": [-3, 0, 4], "body": [-1, 0, 0]})
# Kneeling in the trap, arms dragged up and out by the chains, head tilted to one side.
BOUND = {"@root": [0, -6.6, 0], "right_leg": [0, 0, 3], "left_leg": [0, 0, -3], "right_shin": [90, 0, 0], "left_shin": [90, 0, 0],
         "body": [6, 0, 0], "head": [6, -10, 16], "right_arm": [-14, 0, 132], "left_arm": [-14, 0, -132],
         "right_forearm": [-16, 0, 0], "left_forearm": [-16, 0, 0], "right_hand": [0, 0, 10], "left_hand": [0, 0, -10],
         "right_fingers": [30, 0, 0], "left_fingers": [30, 0, 0], "coat_r": [-70, 0, 4], "@coat_r": [0, 1.0, 0.4],
         "coat_l": [-70, 0, -4], "@coat_l": [0, 1.0, 0.4], "coat_tail": [36, 0, 0]}
# Hands folded behind the back.
BEHIND = {"right_arm": [26, 0, 12], "left_arm": [26, 0, -12], "right_forearm": [-30, 40, 0], "left_forearm": [-30, -40, 0],
          "right_hand": [0, 0, -20], "left_hand": [0, 0, 20], "right_fingers": [30, 0, 0], "left_fingers": [30, 0, 0],
          "body": [-3, 0, 0], "head": [-4, 0, 0]}


def coat_sway(t, amp=1.0):
    ph = t * 2 * math.pi
    return {"coat_tail": [3 * amp * math.sin(ph), 0, 1.5 * amp * math.sin(ph * 0.5)],
            "coat_r": [-2 * amp * math.sin(ph + 0.6), 0, 0], "coat_l": [-2 * amp * math.sin(ph + 1.2), 0, 0]}


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("scalpel", "eyes")))
        return clips[-1]

    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = math.sin(t / 4.0 * 2 * math.pi)
        c.key(t, P_(IDLE, coat_sway(t / 4.0, 0.5), {"@body": [0, -0.15 * (1 - math.cos(t / 4.0 * 2 * math.pi)), 0],
                                                     "head": [-3 + 1.5 * b, 6 * b, 4 + 3 * b]}), E_IO if i else None)

    c = clip("walk", 1.2, loop=True)
    for i, (t, s) in enumerate(((0, 1), (0.6, -1), (1.2, 1))):
        c.key(t, P_(GRIP, {"right_leg": [20 * s, 0, 0], "left_leg": [-20 * s, 0, 0], "right_shin": [6 if s < 0 else 16, 0, 0],
                           "left_shin": [16 if s < 0 else 6, 0, 0], "right_arm": [-10 - 8 * s, 0, 3], "left_arm": [10 * s, 0, -3],
                           "right_forearm": [-24, 0, 0], "left_forearm": [-12, 0, 0], "body": [-1, 5 * s, 0], "head": [-3, -4 * s, 3],
                           "coat_r": [-9 * s, 0, 0], "coat_l": [9 * s, 0, 0], "coat_tail": [8, 0, 2 * s]}), E_IO if i else None)
    c.key(0.3, P_(c.frames[0][1], {"@body": [0, 0.45, 0]}), E_OUT)
    c.key(0.9, P_(c.frames[1][1], {"@body": [0, 0.45, 0]}), E_OUT)

    c = clip("bound_idle", 3.0, loop=True)
    for i, t in enumerate((0, 0.75, 1.5, 2.25, 3.0)):
        b = math.sin(t / 3.0 * 2 * math.pi)
        talk = math.sin(t / 3.0 * 4 * math.pi)
        c.key(t, P_(BOUND, {"body": [6 + 2 * b, 2 * b, 0], "head": [6 + 4 * talk, -10 + 8 * b, 16 - 6 * b],
                            "right_arm": [-14, 0, 132 + 3 * b], "left_arm": [-14, 0, -132 + 3 * b],
                            "@body": [0, 0.15 * b, 0]}), E_IO if i else None)

    c = clip("hum_idle", 4.0, loop=True)
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = math.sin(t / 4.0 * 2 * math.pi)
        c.key(t, P_(BEHIND, coat_sway(t / 4.0, 0.8), {"body": [-3, 4 * b, 3 * b], "head": [-4 - 2 * abs(b), -6 * b, 8 * b],
                                                       "@root": [0.25 * b, 0, 0], "right_leg": [0, 0, -2 * b], "left_leg": [0, 0, -2 * b]}),
              E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Break free (hit 20 = 1.0 s): straining against the chains, then the arms snap down, the chains burst; he stands.
    c = clip("break_free", 1.8)
    strain = P_(BOUND, {"body": [18, 0, 0], "head": [24, 0, 0], "right_arm": [-8, 0, 112], "left_arm": [-8, 0, -112],
                        "right_forearm": [-40, 0, 0], "left_forearm": [-40, 0, 0], "right_fingers": [80, 0, 0], "left_fingers": [80, 0, 0]})
    snap = P_(BOUND, {"@root": [0, -6.0, 0], "body": [-14, 0, 0], "head": [-30, 0, 0], "right_arm": [-20, 0, 34],
                      "left_arm": [-20, 0, -34], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
                      "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0]})
    c.key(0, BOUND)
    c.key(0.5, strain, E_IO)
    c.key(0.85, P_(strain, {"body": [22, 0, 0], "right_arm": [-6, 0, 104], "left_arm": [-6, 0, -104]}), E_IN)
    c.key(1.0, snap, E_SNAP)
    c.key(1.2, P_(snap, {"head": [-22, 0, 0]}), E_OUT)
    c.key(1.5, {"@root": [0, -2.4, 0], "right_leg": [-40, 0, 2], "left_leg": [10, 0, -2], "right_shin": [50, 0, 0], "left_shin": [60, 0, 0],
                "body": [18, 0, 0], "head": [-8, 0, 0], "right_arm": [-10, 0, 14], "left_arm": [-10, 0, -14]}, E_IO)
    c.key(1.8, IDLE, E_OUT)

    # Scalpel slash (hit 7 = 0.35 s): a quick backhand cut across, right to left... his right hand from his left hip outward.
    c = clip("scalpel_slash", 0.7)
    wind = P_(GRIP, {"right_arm": [-50, 30, -40], "right_forearm": [-70, 20, 0], "right_hand": [0, 0, 20], "body": [0, -22, 0],
                     "head": [-4, 14, 0], "left_arm": [-8, 0, -10], "left_forearm": [-20, 0, 0]})
    cut = P_(GRIP, {"right_arm": [-78, -20, 50], "right_forearm": [-8, 0, 0], "right_hand": [0, 0, -10], "body": [0, 26, 0],
                    "head": [-6, -16, 0], "left_arm": [-6, 0, -16], "left_forearm": [-18, 0, 0], "@root": [0, 0, -0.8],
                    "coat_r": [-8, 0, 0]})
    c.key(0, IDLE)
    c.key(0.22, wind, E_OUT)
    c.key(0.35, cut, E_SNAP)
    c.key(0.45, P_(cut, {"right_arm": [-72, -24, 56]}), E_OUT)
    c.key(0.7, IDLE, E_IO)

    # Scalpel throw (hit 12 = 0.6 s): three blades fanned from the left hand, a flick out from the chest.
    c = clip("scalpel_throw", 1.1)
    draw = {"left_arm": [-40, -30, 30], "left_forearm": [-100, -20, 0], "left_hand": [0, 0, 0], "left_fingers": [40, 0, 0],
            "body": [0, 18, 0], "head": [-4, -10, 0]}
    fling = {"left_arm": [-92, 22, -18], "left_forearm": [-4, 0, 0], "left_hand": [0, 0, -30], "left_fingers": [-20, 0, 0],
             "left_index": [-20, 0, 0], "body": [0, -22, 0], "head": [-6, 14, 0], "@root": [0, 0, -0.6]}
    c.key(0, IDLE)
    c.key(0.35, P_(IDLE, draw), E_IO)
    c.key(0.5, P_(IDLE, draw, {"left_forearm": [-112, -24, 0], "body": [0, 22, 0]}))
    c.key(0.6, P_(IDLE, fling), E_SNAP)
    c.key(0.75, P_(IDLE, fling, {"left_arm": [-86, 26, -22]}), E_OUT)
    c.key(1.1, IDLE, E_IO)

    # Hook throw (hit 14 = 0.7 s): an overhand cast of the chain-hook with the left arm, the body following through.
    c = clip("hook_throw", 1.3)
    back = {"left_arm": [-160, 0, -20], "left_forearm": [-50, 0, 0], "left_fingers": [70, 0, 0], "body": [-10, 20, 0],
            "head": [-8, -16, 0], "right_arm": [-16, 0, 14], "right_leg": [-10, 0, 0], "left_leg": [14, 0, 0], "@root": [0, 0, 0.6]}
    cast = {"left_arm": [-70, 10, 6], "left_forearm": [-6, 0, 0], "left_fingers": [-10, 0, 0], "body": [16, -18, 0],
            "head": [-4, 12, 0], "right_arm": [10, 0, 20], "right_leg": [14, 0, 0], "left_leg": [-24, 0, 0], "left_shin": [14, 0, 0],
            "@root": [0, -0.5, -1.4], "coat_tail": [20, 0, 0], "coat_l": [-10, 0, 0]}
    c.key(0, IDLE)
    c.key(0.45, P_(GRIP, back), E_IO)
    c.key(0.58, P_(GRIP, back, {"left_arm": [-170, 0, -16]}))
    c.key(0.7, P_(GRIP, cast), E_SNAP)
    c.key(0.9, P_(GRIP, cast, {"left_arm": [-60, 10, 6]}), E_OUT)
    c.key(1.3, IDLE, E_IO)

    # Chain lash (hit 16 = 0.8 s): a chain whipped along the ground, left to right, low and wide.
    c = clip("chain_lash", 1.5)
    low = {"left_arm": [-30, 40, -60], "left_forearm": [-30, 0, 0], "left_fingers": [80, 0, 0], "body": [16, 30, 0],
           "head": [-14, -20, 0], "@root": [0, -1.2, 0], "right_leg": [-20, 0, 6], "left_leg": [10, 0, -6], "right_shin": [26, 0, 0]}
    sweep = {"left_arm": [-60, -40, 50], "left_forearm": [-6, 0, 0], "left_fingers": [70, 0, 0], "body": [16, -34, 0],
             "head": [-10, 22, 0], "@root": [0, -1.4, 0], "right_leg": [-20, 0, 6], "left_leg": [10, 0, -6], "right_shin": [26, 0, 0],
             "coat_r": [-14, 0, -8], "coat_l": [-14, 0, 8], "coat_tail": [24, 0, 6]}
    c.key(0, IDLE)
    c.key(0.6, P_(GRIP, low), E_IO)
    c.key(0.8, P_(GRIP, sweep), E_SNAP)
    c.key(1.0, P_(GRIP, sweep, {"left_arm": [-52, -50, 56]}), E_OUT)
    c.key(1.5, IDLE, E_IO)

    # Rack call (hit 22 = 1.1 s): both hands raised, palms up, as the rack rises from the floor.
    c = clip("rack_call", 1.7)
    lift = lambda k: {"right_arm": [-30 * k, 0, 30 * k], "left_arm": [-30 * k, 0, -30 * k], "right_forearm": [-80 * k, -30 * k, 0],
                      "left_forearm": [-80 * k, 30 * k, 0], "right_hand": [0, 70 * k, 0], "left_hand": [0, -70 * k, 0],
                      "right_fingers": [-10 * k, 0, 0], "left_fingers": [-10 * k, 0, 0], "head": [-10 * k, 0, 0], "body": [-4 * k, 0, 0]}
    c.key(0, IDLE)
    c.key(0.5, P_(GRIP, lift(0.6)), E_IO)
    c.key(1.1, P_(GRIP, lift(1.2), {"head": [-18, 0, 0]}), E_OUT)
    c.key(1.35, P_(GRIP, lift(1.15)))
    c.key(1.7, IDLE, E_IO)

    # Smoke (hit 10 = 0.5 s): a backward lean, head back, mouth open as the white smoke pours from him; held, then back.
    c = clip("smoke", 2.0)
    lean = {"body": [-14, 0, 0], "head": [-36, 0, 0], "right_arm": [-10, 0, 40], "left_arm": [-10, 0, -40], "right_forearm": [-20, 0, 0],
            "left_forearm": [-20, 0, 0], "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0], "@root": [0, 0, 0.6],
            "coat_tail": [10, 0, 0]}
    c.key(0, IDLE)
    c.key(0.5, lean, E_OUT)
    c.key(1.5, P_(lean, {"head": [-40, 4, 0], "body": [-16, 0, 0]}), E_IO)
    c.key(2.0, IDLE, E_IO)

    # Summon (hit 14 = 0.7 s): one hand down, fingers spread over the floor.
    c = clip("summon", 1.3)
    down = {"left_arm": [-34, 0, -24], "left_forearm": [-20, 0, 0], "left_hand": [-50, 0, 0], "left_fingers": [-30, 0, 0],
            "left_index": [-30, 0, 0], "left_thumb": [0, 0, -30], "body": [10, 0, 0], "head": [10, 0, 0], "@root": [0, -0.6, 0]}
    c.key(0, IDLE)
    c.key(0.45, P_(IDLE, down, {"left_arm": [-54, 0, -30]}), E_IO)
    c.key(0.7, P_(IDLE, down), E_SNAP)
    c.key(0.95, P_(IDLE, down), E_OUT)
    c.key(1.3, IDLE, E_IO)

    # Taunt (hit 10 = 0.5 s): a tilted head and a small, mocking bow.
    c = clip("taunt", 1.2)
    bow = {"body": [18, 0, 0], "head": [6, 0, 24], "right_arm": [-30, 0, 6], "right_forearm": [-60, -30, 0], "left_arm": [20, 0, -8],
           "left_forearm": [-30, 0, 0]}
    c.key(0, IDLE)
    c.key(0.35, P_(IDLE, {"head": [-6, 0, 22]}), E_IO)
    c.key(0.5, P_(IDLE, bow), E_OUT)
    c.key(0.8, P_(IDLE, bow, {"head": [2, 0, 26]}))
    c.key(1.2, IDLE, E_IO)

    # Hurt by holy water or salt (hit 2): a recoil, the left hand up to the face.
    c = clip("hurt_holy", 0.7)
    recoil = {"body": [-12, 10, 0], "head": [-20, 18, 0], "left_arm": [-110, 0, 24], "left_forearm": [-80, 30, 0],
              "left_hand": [0, 0, 20], "left_fingers": [10, 0, 0], "right_arm": [-20, 0, 30], "@root": [0, 0, 1.0]}
    c.key(0, IDLE)
    c.key(0.1, P_(IDLE, recoil), E_SNAP)
    c.key(0.3, P_(IDLE, recoil, {"head": [-12, 22, 4]}))
    c.key(0.7, IDLE, E_IO)

    # Death (hit 50 = 2.5 s): the blow doubles him over, he shudders; the demon-light flare (head back, arms out); he falls.
    c = clip("death", 4.0, hold=True)
    stab = {"body": [26, 0, 0], "head": [18, 0, 0], "right_arm": [-40, 0, -30], "left_arm": [-40, 0, 30],
            "right_forearm": [-60, -30, 0], "left_forearm": [-60, 30, 0], "@root": [0, -0.6, 0.8], "right_leg": [-8, 0, 0]}
    c.key(0, IDLE)
    c.key(0.3, stab, E_SNAP)
    for i in range(1, 9):
        t = round(0.3 + 0.2 * i, 2)
        k = (t - 0.3) / 1.8
        j = jitter("alastair-death", i, 3 + 6 * k)
        c.key(t, P_(stab, {"body": [26 - 42 * k + j[0], j[1], j[2]], "head": [18 - 52 * k + j[2], j[0], j[1]],
                           "right_arm": [-40 + 30 * k, 0, -30 + 105 * k], "left_arm": [-40 + 30 * k, 0, 30 - 105 * k],
                           "right_forearm": [-60 + 50 * k, 0, 0], "left_forearm": [-60 + 50 * k, 0, 0],
                           "@root": [0, -0.6 + 0.8 * k, 0.8]}))
    flare = {"body": [-18, 0, 0], "head": [-48, 0, 0], "right_arm": [-14, 0, 82], "left_arm": [-14, 0, -82],
             "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_hand": [0, 0, 20], "left_hand": [0, 0, -20],
             "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0], "@root": [0, 0.4, 0], "coat_r": [-14, 0, -10],
             "coat_l": [-14, 0, 10], "coat_tail": [16, 0, 0]}
    c.key(2.5, flare, E_BACK)
    c.key(2.9, P_(flare, {"head": [-52, 4, 0]}))
    fall = {"@root": [0, -1.0, 6.0], "body": [-84, 0, 0], "head": [-10, 20, 0], "right_arm": [-170, 0, 30], "left_arm": [-160, 0, -40],
            "right_forearm": [-10, 0, 0], "left_forearm": [-20, 0, 0], "right_leg": [6, 0, 6], "left_leg": [12, 0, -4],
            "coat_tail": [80, 0, 0], "coat_r": [-30, 0, -10], "coat_l": [-30, 0, 10]}
    c.key(3.5, fall, E_IN)
    c.key(4.0, fall)

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The rack, the hook, the thrown scalpel
# =====================================================================================================

def iron_m(seed, rust=0.25):
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f) * 7, y * 2, seed, 64, 64, 3, 6.0)
        c = tone(IRON, 2.6 + (n - 0.5) * 1.4 + (0.9 if f == "up" else 0))
        r = fbm(x * 3 + 11, y * 3 + len(f), seed + 3, 64, 64, 2, 5.0)
        if r > 1 - rust:
            c = mix(c, tone(RUST, 2 + 3 * (r - (1 - rust)) / rust), 0.75)
        return c
    return m


def chain_m(seed):
    def m(f, x, y, w, h):
        link = y % 4
        if f in ("north", "south"):
            if link == 0 or (link in (1, 3) and x in (0, w - 1)):
                return tone(IRON, 4.0 + (0.6 if h01("ch", seed, y) < 0.3 else 0))
            if link == 2:
                return None
            return None
        if link == 2:
            return tone(IRON, 3.2)
        return None if link == 0 else tone(IRON, 2.4)
    return m


RACK_W, RACK_H = 32.0, 40.0


def rack_rig():
    """A black iron frame, 2 blocks wide, 1 deep, 2.5 high: four posts, crossbars, two chains with cuffs from the top bar,
    two cuffs at the foot. Bones: root -- frame -- chains -- cuffs."""
    m = Model("alastair_rack", 128, 128)
    m.bone("root", (0, 0, 0))
    fr = m.bone("frame", (0, 0, 0), "root")
    ch = m.bone("chains", (0, RACK_H - 4, 0), "frame")
    cu = m.bone("cuffs", (0, 20, -1), "chains")
    im = iron_m(40701)
    hw = RACK_W / 2
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cube(fr, (sx * (hw - 1.5) - 1.5, 0, sz * 6.5 - 1.5), (3, RACK_H, 3), im, density=1, tag="post")
            m.cube(fr, (sx * (hw - 1.5) - 1.0, RACK_H, sz * 6.5 - 1.0), (2, 1.5, 2), im, density=2, tag="post_cap")
        m.cube(fr, (sx * (hw - 1.5) - 1.0, 2, -5), (2, 2, 10), im, density=1, tag="side_bar")
        m.cube(fr, (sx * (hw - 1.5) - 1.0, RACK_H - 4, -5), (2, 2, 10), im, density=1, tag="side_bar")
    for sz in (-1, 1):
        m.cube(fr, (-hw + 3, RACK_H - 4, sz * 6.5 - 1.0), (RACK_W - 6, 2.5, 2), im, density=1, tag="top_bar")
        m.cube(fr, (-hw + 3, 2, sz * 6.5 - 1.0), (RACK_W - 6, 2.5, 2), im, density=1, tag="foot_bar")
    # Spikes along the top bar (front).
    for i in range(7):
        x = -hw + 5 + i * 3.7
        m.cube(fr, (x - 0.5, RACK_H - 1.5, -7.0), (1, 2, 1), solid(IRON[4]), density=2, tag="spike")
    # A middle crossbar, rusted with old blood.
    bl = lambda f, x, y, w, h: mix(im(f, x, y, w, h), BLOOD, 0.45) if h01("bl", x // 2, f) < 0.4 else im(f, x, y, w, h)
    m.cube(fr, (-hw + 3, 18, 5.5), (RACK_W - 6, 2, 2), bl, density=1, tag="mid_bar")
    # The chains, hanging from the top bar to the cuffs.
    cm = chain_m(40702)
    for sx in (-1, 1):
        m.cube(ch, (sx * 7 - 0.75, 20, -1.75), (1.5, RACK_H - 24, 1.5), cm, density=2, tag="chain")
        m.cube(ch, (sx * 7 - 0.75, 4, 6.0), (1.5, 14, 1.5), cm, density=2, tag="foot_chain")
    # The cuffs: rings of iron, open (the clamp closes them).
    for sx in (-1, 1):
        m.cube(cu, (sx * 7 - 1.5, 18, -2.5), (3, 2, 3), lambda f, x, y, w, h: None if f in ("up", "down") else tone(IRON, 3.6),
               density=2, tag="cuff")
        m.cube(cu, (sx * 7 - 1.5, 4, 5.2), (3, 2, 3), lambda f, x, y, w, h: None if f in ("up", "down") else tone(IRON, 3.6),
               density=2, tag="cuff")
    return m


def rack_anims():
    f = AnimFile()
    a = "animation.alastair_rack."
    r = f.new(a + "rise", 1.0, hold=True)
    r.pos("frame", (0, [0, -44, 0]), (0.75, [0, 1.5, 0], "easeOutQuad"), (1.0, [0, 0, 0], "easeInOutSine"))
    r.rot("chains", (0, [0, 0, 0]), (0.75, [8, 0, 0]), (1.0, [0, 0, 0], "easeOutBack"))
    i = f.new(a + "idle", 3.0, loop=True)
    i.rot("chains", (0, [0, 0, 0]), (1.5, [3, 0, 1.5], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    i.rot("cuffs", (0, [0, 0, 0]), (1.5, [-4, 0, 0], "easeInOutSine"), (3.0, [0, 0, 0], "easeInOutSine"))
    c = f.new(a + "clamp", 0.5, hold=True)
    c.pos("cuffs", (0, [0, 0, 0]), (0.25, [0, -1.0, 0], "easeInQuart"), (0.5, [0, -0.6, 0]))
    c.scale("cuffs", (0, [1, 1, 1]), (0.25, [0.7, 1.15, 0.7], "easeInQuart"), (0.5, [0.75, 1.1, 0.75]))
    c.rot("chains", (0, [0, 0, 0]), (0.2, [-10, 0, 0]), (0.5, [-4, 0, 0], "easeOutQuad"))
    s = f.new(a + "sink", 1.0, hold=True)
    s.pos("frame", (0, [0, 0, 0]), (0.2, [0, 1.0, 0], "easeOutQuad"), (1.0, [0, -44, 0], "easeInQuad"))
    return f


def hook_rig():
    """The chain-hook: a rusty iron hook, its eye on a short length of chain, flying point first along -Z."""
    m = Model("alastair_hook", 64, 64)
    m.bone("root", (0, 0, 0))
    hk = m.bone("hook", (0, 4, 0), "root")
    im = iron_m(40801, rust=0.4)
    # The shank along Z, the bend at the front, the point coming back.
    m.cube(hk, (-0.75, 3.25, -3), (1.5, 1.5, 8), im, density=2, tag="shank")
    m.cube(hk, (-0.75, 0.0, -4.5), (1.5, 4.75, 1.5), im, density=2, tag="bend")
    m.cube(hk, (-0.6, 0.0, -4.5), (1.2, 1.2, 4.0), im, density=2, tag="return", rotation=(-20, 0, 0), pivot=(0, 0.6, -4.5))
    m.cube(hk, (-0.4, 0.5, -1.2), (0.8, 2.0, 0.8), lambda f, x, y, w, h: tone(IRON, 4.4) if y == 0 else tone(RUST, 3), density=4,
           tag="point", rotation=(-35, 0, 0), pivot=(0, 0.6, -1.0))
    m.cube(hk, (-1.2, 2.8, 5), (2.4, 2.4, 1.0), lambda f, x, y, w, h: None if f in ("north", "south") and 1 <= x <= 3 and 1 <= y <= 3
           else tone(IRON, 3.4), density=2, tag="eye")
    cm = chain_m(40802)
    m.cube(hk, (-0.6, 3.4, 6), (1.2, 1.2, 10), lambda f, x, y, w, h: tone(IRON, 3.0 + (0.8 if (y if f in ("up", "down") else x) % 4 == 0 else 0)),
           density=2, tag="chain")
    return m


def hook_anims():
    f = AnimFile()
    i = f.new("animation.alastair_hook.idle", 1.0, loop=True)
    i.rot("hook", (0, [0, 0, 0]), (0.5, [0, 0, 180]), (1.0, [0, 0, 360]))
    return f


def thrown_scalpel():
    """Vanilla arrow layout (32x32): the side view in the top 16x5 strip (point to the left), the end cross at (0, 5) 5x5."""
    t = Tex(32, 32, 40901)
    # The side: x 0..15, y 0..4 (vanilla draws u 0..16, v 0..5). Point at x = 0.
    for x in range(16):
        if x < 7:      # the blade, narrowing to the point
            top = 2 if x < 2 else 1
            for y in range(top, 4):
                c = STEEL[5] if y == top else (BLOOD if y == 3 and x % 2 else STEEL[3])
                t.set(x, y, c)
        elif x < 8:
            for y in range(1, 4):
                t.set(x, y, STEEL[2])
        else:          # the bone handle
            for y in range(1, 4):
                t.set(x, y, BONE[4] if y == 1 else (BONE[2] if y == 3 else BONE[3]))
    # The end: a small bone cross section.
    for y in range(5, 10):
        for x in range(0, 5):
            if 1 <= x <= 3 and 6 <= y <= 8:
                t.set(x, y, BONE[3] if (x + y) % 2 else BONE[2])
    return t


# =====================================================================================================
# The scalpel item and the bust
# =====================================================================================================

def scalpel_item():
    pal = {"o": P.OUTLINE if hasattr(P, "OUTLINE") else hexc("#0a0a0c"), "s": STEEL[5], "S": STEEL[4], "m": STEEL[3], "d": STEEL[2],
           "r": BLOOD, "R": hexc("#8a1a16"), "b": BONE[4], "B": BONE[3], "k": BONE[1], "f": STEEL[1]}
    t = art([
        "................",
        "...............s",
        "..............sS",
        ".............sSr",
        "............sSr.",
        "...........sSR..",
        "..........sSr...",
        ".........mSr....",
        "........dmd.....",
        ".......bBf......",
        "......bBk.......",
        ".....bBk........",
        "....bBk.........",
        "...bBk..........",
        "..bkk...........",
        "................",
    ], pal)
    item_outline(t, hexc("#0c0b0d"))
    return t


def bust():
    """His bust on the shared pedestal: close-cropped grey hair, the narrow pale face, white collar buttoned high, grey coat."""
    def tex16(fn):
        t = Tex(16, 16, 0)
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                if c is not None:
                    t.set(x, y, c)
        return t

    skin = lambda x, y: tone(SKIN, 3.4 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 2.8 + 0.5 * math.sin(x * 1.7 + y * 0.9))
    legend = {"s": skin, "h": hair, "b": HAIR[2], "U": lambda x, y: shade(skin(x, y), 0.78), "u": lambda x, y: shade(skin(x, y), 0.86),
              "k": lambda x, y: shade(skin(x, y), 0.8), "w": WHITE, "I": WHITE_HOT, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1],
              "L": LIPS[3], "j": lambda x, y: shade(skin(x, y), 0.9)}

    def face(x, y):
        ch = FACE[y][x]
        if ch == "I":
            ch = "w"
        v = legend.get(ch, skin)
        return v(x, y) if callable(v) else v
    face_t = tex16(face)
    hair_t = tex16(hair)
    skin_t = tex16(skin)

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 1.6 and y < 12:
            return tone(SHIRT, 4.2 - y / 14) if not (d < 0.6 and y % 4 == 1) else hexc("#d8d8d4")
        if d < 2.8 - 0.1 * y and y < 10:
            return tone(SUIT, 2.6)
        if d < 4.0 - 0.15 * y and y < 11:
            return COAT[4]
        return tone(COAT, 3.0 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    chest_t = tex16(chest)
    coat_t = tex16(lambda x, y: tone(COAT, 3.1 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4)))
    for n, t in (("face", face_t), ("hair", hair_t), ("skin", skin_t), ("chest", chest_t), ("coat", coat_t)):
        save(t, "block", f"alastair_trophy_{n}")
    full = [0, 0, 16, 16]
    F, HR, S, CH, C = "face", "hair", "skin", "chest", "coat"
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": C}),
        BM.cube("chest", (4.9, 3.4, 6.2), (11.1, 7.6, 9.8), {"north": CH, "*": C}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (4.3, 6.0, 6.4), (11.7, 7.8, 9.6), {"*": C}),
        BM.cube("collar", (5.4, 7.2, 8.9), (10.6, 9.0, 9.7), {"*": C}, rot=("x", -22.5, (8, 7.2, 9.3))),
        BM.cube("neck", (7.2, 7.4, 7.2), (8.8, 8.8, 8.8), {"*": S}),
        BM.cube("head", (5.9, 8.6, 5.8), (10.1, 13.6, 10.2), {"north": F, "up": HR, "south": HR, "east": S, "west": S},
                uv={"north": full}),
        BM.cube("hair_cap", (5.75, 12.6, 5.9), (10.25, 13.8, 10.35), {"north": None, "down": None, "*": HR}),
        BM.cube("nose", (7.7, 10.4, 5.55), (8.3, 11.3, 5.8), {"*": S}),
    ]
    mdl = BM.model({"base": "trophy_base", F: "alastair_trophy_face", HR: "alastair_trophy_hair", S: "alastair_trophy_skin",
                    CH: "alastair_trophy_chest", C: "alastair_trophy_coat"}, e, "trophy_base")
    BM.write("alastair_trophy", mdl)


# =====================================================================================================
# Output
# =====================================================================================================

def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=40000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    rk = rack_rig()
    t, _ = rk.build(gutter=1, seed=40700)
    rk.rig.write(GEO + "alastair_rack.geo.json")
    save(t, "entity", "alastair_rack")
    rack_anims().write(ANIM + "alastair_rack.animation.json")
    hk = hook_rig()
    t, _ = hk.build(gutter=1, seed=40800)
    hk.rig.write(GEO + "alastair_hook.geo.json")
    save(t, "entity", "alastair_hook")
    hook_anims().write(ANIM + "alastair_hook.animation.json")
    save(thrown_scalpel(), "entity", "alastair_thrown_scalpel")
    save(scalpel_item(), "item", "alastair_scalpel")
    bust()


def preview(out_dir, poses=(("idle", 0.0),), hidden=()):
    import preview3d
    os.makedirs(out_dir, exist_ok=True)
    m = rig()
    t, g = m.build(gutter=1, seed=40000)
    _, clips = anims()
    by = {c.anim.name.split(".")[-1]: c for c in clips}
    for name, tm in poses:
        preview3d.render(m.rig, t, os.path.join(out_dir, f"alastair_{name}_{tm}.png"), pose=by[name].pose_at(tm), hidden=hidden, scale=4)


if __name__ == "__main__":
    generate()
