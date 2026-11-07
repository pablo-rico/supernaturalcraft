"""Uriel, the traitor of the garrison (v0.40): his rig, texture + glowmask, every clip (names and timings in UrielAssets), his
trophy bust (`block/uriel_trophy`) and his bar's sheet (`textures/gui/uriel_bar.png`).

A tall, broad man (33 px to the crown): bald, a short dark beard, a stern face; dark skin. A long dark brown leather coat,
open, to below the knee, over a black turtleneck; dark trousers, boots. An angel blade in his right fist (`blade`). Huge wings
of grey-gold light at his back (`wings` -- `wing_r`, `wing_l` and their feathers, allegiance_art's "light" wings): the renderer
hides them until phase II, where `unfurl` opens them. His eyes (`eyes`, a shell) burn pale gold while his grace shows.

Skeleton: apoc_vessel's (coat, fingers) + blade (right_hand) + wings (body).

Bar sheet (256x128), read by client/apocalypse/seal/UrielHud (the constants there mirror these):
  band     (0, 0)     256x40   scorched gold leaf on dark bronze, grey feathers fanning out at both ends; a recessed trough
                                (x 34..221, y 22..31) and an empty band above it for his name (drawn at y 5..15)
  fill     (0, 40)    188x10   his grace: molten gold light, brighter along its top (drawn cropped from the left)
  drained  (0, 50)    188x10   the trough burnt out: charred gold, ash
  wings    (0, 64)    24x24 each: a grey wing whole (x 0), burning (x 24), burnt to a black stump (x 48) -- one per phase
  ember    (96, 64)   6 frames of 8x16 (x = 96 + 8 * frame): the bar's burning edge, sparks lifting (frame = time)
"""

import math
import os

import allegiance_art as AL
import blockmodel as BM
import horsemen_art as H
from angel_art import FOLDED, SPREAD, UNFURL, WIDE, HIGH, DOWN, wings
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, coat, eye_shell, glove_m, head, jitter, leather_m, legs,
                         neck, skeleton, skin_m, tone, torso)
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from michael_art import Clip, P_, fabric, write_compact
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "uriel"
A = "animation.uriel."

B = Body(HIP=13.0, NECK=25.2, TW=4.5, TD=2.4, AW=3.9, SY=24.4, ELBOW=19.2, WRIST=14.4, PALM=12.4, LX=2.2, LW=4.1,
         KNEE=6.9, SHOE_H=1.5, HEM=4.6)

SKIN = Ramp("#1e120d", "#2f1d15", "#43291d", "#583627", "#6d4432", "#83543f")
BEARD = Ramp("#070505", "#0e0a09", "#171211", "#211a18", "#2d2421", "#3d322e")
COAT = Ramp("#130b06", "#20130b", "#2e1c10", "#3e2616", "#4f311d", "#664028")
KNIT = Ramp("#050506", "#0a0a0c", "#111114", "#18181c", "#212126", "#2e2e35")
TROUSER = Ramp("#0b0b0d", "#131316", "#1c1c20", "#26262b", "#313137", "#404047")
BOOT = Ramp("#050403", "#0c0908", "#15110e", "#201a16", "#2c241e", "#4a3e34")
LIPS = Ramp("#2a1612", "#3b201a", "#4d2a22", "#5e352b", "#704034", "#834c3e")
SILVER = Ramp("#5d636b", "#868d96", "#aab1ba", "#c8ced6", "#e0e5ea", "#f7f9fb")
GRIP = Ramp("#14161c", "#1d2029", "#272b37", "#333847", "#414759", "#535a6e")
GOLD = Ramp("#6a4810", "#956a1a", "#bf8f2a", "#ddb245", "#f2d272", "#fff0b0")
EYE_WHITE, IRIS = hexc("#d6cdbf"), hexc("#1c120c")
LIGHT_HOT, LIGHT, LIGHT_DIM = hexc("#fff8e0"), hexc("#f2d88a"), hexc("#9a7a30")

# x = 0 is the viewer's left (his right); a bald crown, heavy brows, a short beard round the jaw and mouth.
FACE = [
    "SSSSSSSSSSSSSSSS",
    "SSSSSSSSSSSSSSSS",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "skkssssssssssskk",
    "ssssssssssssssss",
    "sbbbbbsssssbbbbs",
    "sUuwIIwsswIIwuUs",
    "ssssssssssssssss",
    "ssssssnnnnssssss",
    "dsssssNnnNssssdd",
    "dssmmmmmmmmmmssd",
    "ddsdlllllllldssd",
    "dddddLLLLLLddddd",
    "dddddddddddddddd",
    "pddddddddddddddp",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=42001):
    sk = skin_m(SKIN, seed, 3.0, 0.35)
    bd = lambda f, x, y, w, h: tone(BEARD, 2.0 + 0.8 * h01("bd", seed, f, x, y) + 0.4 * (y / max(1, h)))
    scalp = lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.12 if (f == "up" and 4 <= x <= 10 and 4 <= y <= 10) else 1.04)
    lg = {"s": sk, "S": scalp, "k": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.85), "b": BEARD[1],
          "U": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.75), "u": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.85),
          "w": EYE_WHITE, "I": IRIS, "n": SKIN[4], "N": SKIN[1], "m": bd, "d": bd, "l": LIPS[1], "L": LIPS[3],
          "p": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), BEARD[2], 0.5)}
    sides = H.head_sides(sk, scalp, top=0, back=0, temple=16, sideburn=0, beard=bd, beard_from=11)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[3] if f == "north" else SKIN[1])
    for s in (-1, 1):
        m.cube("brows", (s * 2.1 - 1.3, B.NECK + B.HEAD * 0.62, -4.15), (2.6, 0.55, 0.3), solid(BEARD[1]), density=4, tag="brow",
               rotation=(0, 0, 10 * s), pivot=(s * 1.0, B.NECK + 5.0, -4.1))
    m.cube("head", (-2.6, B.NECK - 0.6, -4.4), (5.2, 1.6, 1.2), none_on(("up",), bd), density=2, tag="chin")
    eye_shell(m, B, "uwWWwsswWWwu".replace("s", "."), {"u": LIGHT_DIM, "w": LIGHT, "W": LIGHT_HOT})
    neck(m, B, lambda f, x, y, w, h: tone(KNIT, 3.2 + (0.6 if y % 2 else 0)), 4.0)


def build_body(m, seed=42200):
    knit = lambda f, x, y, w, h: tone(KNIT, 2.8 + (0.7 if x % 2 == 0 else 0) - 0.5 * y / max(1, h))
    torso(m, B, knit, tag="torso")
    # The turtleneck's rolled collar.
    m.cube("body", (-2.3, B.NECK - 1.4, -2.3), (4.6, 1.6, 4.6), lambda f, x, y, w, h: tone(KNIT, 3.6 - y * 0.5), density=2,
           tag="roll_neck")
    lea = leather_m(COAT, seed + 1, 2.8, 0.8)
    coat(m, B, COAT, lea, seed + 1, collar_up=False, pockets=True)
    sleeve = leather_m(COAT, seed + 2, 2.7, 0.7)
    cuff = lambda f, x, y, w, h: tone(COAT, 3.6) if f not in ("up", "down") else COAT[0]
    arms(m, B, sleeve, skin_m(SKIN, seed + 3, 3.1, 0.3), cuff=cuff, cuff_depth=1.0)
    legs(m, B, fabric(TROUSER, seed + 4, 2.8, 0.2, crease=True), BOOT, boot_h=3.4, toe=1.0)


def build_blade(m):
    """An angel blade in the right fist, point forward: a dark wrapped grip, a three-edged silver spike."""
    cx = -B.AX
    y = B.PALM - 0.3
    b = m.bone("blade", (cx, y, 0), "right_hand", rotation=(32, 0, 0))
    m.cube(b, (cx - 0.5, y - 0.5, -0.6), (1, 1, 3.4), lambda f, xx, yy, w, h: GRIP[2 + ((xx + yy) % 3 == 0) * 2], density=2,
           tag="grip")
    m.cube(b, (cx - 0.6, y - 0.6, 2.8), (1.2, 1.2, 0.5), solid(SILVER[2]), density=2, tag="pommel")
    m.cube(b, (cx - 0.75, y - 0.75, -1.1), (1.5, 1.5, 0.5), solid(SILVER[3]), density=2, tag="guard")

    def steel(f, xx, yy, w, h):
        if f in ("north", "south", "up", "down"):
            return SILVER[5]
        return SILVER[4] if f == "west" else SILVER[2]
    m.cube(b, (cx - 0.5, y - 0.5, -9.4), (1, 1, 8.3), steel, H.glow_faint(0.25), density=2, tag="blade_steel", rotation=(0, 0, 45),
           pivot=(cx, y, 0))
    m.cube(b, (cx - 0.25, y - 0.25, -10.4), (0.5, 0.5, 1.0), solid(SILVER[5]), H.glow_faint(0.35), density=2, tag="tip",
           rotation=(0, 0, 45), pivot=(cx, y, 0))


def rig():
    m = Model(NAME, 256, 512)
    skeleton(m, B, coat=True)
    build_head(m)
    build_body(m)
    build_blade(m)
    wb = m.bone("wings", (0, B.NECK - 4.0, B.TD + 0.6), "body")
    AL.build_wings(m, wb.name, "light", off=(0.0, B.NECK - 4.2, B.TD - 1.0), glow_rims=True)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

GRIP_R = {"right_fingers": [80, 0, 0], "right_index": [70, 0, 0], "right_middle": [75, 0, 0], "right_thumb": [-20, 0, 20]}
# A soldier at rest: square, the blade held low and forward, the left fist loose.
IDLE = P_(GRIP_R, {"right_arm": [-10, 0, 4], "right_forearm": [-22, 0, 0], "left_arm": [-4, 0, -4], "left_forearm": [-10, 0, 0],
                   "left_fingers": [40, 0, 0], "head": [-2, 0, 0], "body": [0, 0, 0]})
HANG = {"right_leg": [10, 0, 4], "left_leg": [4, 0, -4], "right_shin": [16, 0, 0], "left_shin": [22, 0, 0]}
HOVER_Y = 6.0


def coat_sway(t, amp=1.0):
    ph = t * 2 * math.pi
    return {"coat_tail": [3 * amp * math.sin(ph), 0, 1.5 * amp * math.sin(ph * 0.5)],
            "coat_r": [-2 * amp * math.sin(ph + 0.6), 0, 0], "coat_l": [-2 * amp * math.sin(ph + 1.2), 0, 0]}


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("eyes", "wings")))
        return clips[-1]

    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1.0, 2.0, 3.0, 4.0)):
        b = math.sin(t / 4.0 * 2 * math.pi)
        c.key(t, P_(FOLDED, IDLE, coat_sway(t / 4.0, 0.4), {"@body": [0, -0.15 * abs(b), 0], "head": [-2 + b, 5 * b, 0]}),
              E_IO if i else None)

    c = clip("walk", 1.1, loop=True)
    for i, (t, s) in enumerate(((0, 1), (0.55, -1), (1.1, 1))):
        c.key(t, P_(FOLDED, GRIP_R, {"right_leg": [22 * s, 0, 0], "left_leg": [-22 * s, 0, 0], "right_shin": [6 if s < 0 else 18, 0, 0],
                                     "left_shin": [18 if s < 0 else 6, 0, 0], "right_arm": [-10 - 10 * s, 0, 4],
                                     "left_arm": [14 * s, 0, -4], "right_forearm": [-22, 0, 0], "left_forearm": [-12, 0, 0],
                                     "body": [-2, 6 * s, 0], "head": [-2, -5 * s, 0], "coat_r": [-10 * s, 0, 0], "coat_l": [10 * s, 0, 0],
                                     "coat_tail": [8, 0, 2 * s]}), E_IO if i else None)
    c.key(0.275, P_(c.frames[0][1], {"@body": [0, 0.5, 0]}), E_OUT)
    c.key(0.825, P_(c.frames[1][1], {"@body": [0, 0.5, 0]}), E_OUT)

    # Hover (phase II): off the ground, the legs hanging, the wings beating slowly.
    c = clip("hover", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        up = i % 2 == 0
        c.key(t, P_(HIGH if up else DOWN, IDLE, HANG, coat_sway(t / 2.0, 1.2),
                    {"@root": [0, HOVER_Y + (0.0 if up else 0.8), 0], "body": [4, 0, 0], "head": [-6, 0, 0]}), E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Slash (9): a heavy blade cut, high right to low left.
    c = clip("slash", 0.9)
    up = P_(GRIP_R, {"right_arm": [-160, -20, 30], "right_forearm": [-40, 0, 0], "body": [-6, 24, 0], "head": [-6, -14, 0],
                     "left_arm": [-30, 0, -20], "left_forearm": [-40, 0, 0], "@root": [0, 0.2, 0.6]})
    down = P_(GRIP_R, {"right_arm": [-40, 20, -40], "right_forearm": [-20, 0, 0], "body": [18, -24, 0], "head": [4, 14, 0],
                       "left_arm": [-20, 0, -30], "left_forearm": [-30, 0, 0], "right_leg": [-24, 0, 0], "left_leg": [14, 0, 0],
                       "right_shin": [20, 0, 0], "@root": [0, -1.0, -1.8], "coat_tail": [20, 0, 0]})
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.35, P_(UNFURL, up), E_OUT)
    c.key(0.45, P_(WIDE, down), E_SNAP)
    c.key(0.6, P_(SPREAD, down, {"right_arm": [-36, 22, -44]}), E_OUT)
    c.key(0.9, P_(SPREAD, IDLE), E_IO)

    # Thrust (8): a stab straight ahead.
    c = clip("thrust", 0.8)
    coil = P_(GRIP_R, {"right_arm": [10, 0, 10], "right_forearm": [-100, 0, 0], "body": [6, -18, 0], "@root": [0, -0.4, 1.0]})
    stab = P_(GRIP_R, {"right_arm": [-88, 0, 2], "right_forearm": [0, 0, 0], "body": [14, 18, 0], "head": [-12, -12, 0],
                       "right_leg": [-30, 0, 0], "right_shin": [20, 0, 0], "left_leg": [20, 0, 0], "@root": [0, -0.8, -3.0]})
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.3, P_(SPREAD, coil), E_IO)
    c.key(0.4, P_(WIDE, stab), E_SNAP)
    c.key(0.55, P_(WIDE, stab, {"@root": [0, -0.8, -3.3]}), E_OUT)
    c.key(0.8, P_(SPREAD, IDLE), E_IO)

    # Palm (16): the left palm raised high and slammed into the ground -- the crack.
    c = clip("palm", 1.4)
    raise_ = P_(GRIP_R, {"left_arm": [-170, 0, -14], "left_forearm": [-20, 0, 0], "left_fingers": [-10, 0, 0], "body": [-8, 0, 0],
                         "head": [-12, 0, 0], "@root": [0, 0.4, 0]})
    slam = P_(GRIP_R, {"left_arm": [-60, 0, -10], "left_forearm": [-10, 0, 0], "left_hand": [-60, 0, 0], "left_fingers": [-20, 0, 0],
                       "body": [46, 0, 0], "head": [-30, 0, 0], "right_leg": [-60, 0, 0], "right_shin": [80, 0, 0], "left_leg": [16, 0, 0],
                       "left_shin": [70, 0, 0], "@root": [0, -5.0, 0], "right_arm": [10, 0, 20], "coat_tail": [40, 0, 0]})
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.55, P_(HIGH, raise_), E_IO)
    c.key(0.65, P_(HIGH, raise_, {"left_arm": [-176, 0, -10]}))
    c.key(0.8, P_(DOWN, slam), E_SNAP)
    c.key(1.05, P_(DOWN, slam), E_OUT)
    c.key(1.4, P_(SPREAD, IDLE), E_IO)

    # Smite (18): the left hand thrust forward, fingers spread -- the cone.
    c = clip("smite", 1.5)
    draw_ = P_(GRIP_R, {"left_arm": [-40, 0, 10], "left_forearm": [-110, 0, 0], "left_hand": [0, 0, 0], "body": [-4, 20, 0],
                        "head": [-6, -10, 0]})
    push = P_(GRIP_R, {"left_arm": [-92, 0, -2], "left_forearm": [0, 0, 0], "left_hand": [-70, 0, 0], "left_fingers": [-30, 0, 0],
                       "left_index": [-30, 0, 0], "left_thumb": [0, 0, -40], "body": [6, -16, 0], "head": [-8, 12, 0],
                       "@root": [0, 0, -1.0], "right_leg": [-10, 0, 0], "left_leg": [10, 0, 0]})
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.6, P_(UNFURL, draw_), E_IO)
    c.key(0.9, P_(WIDE, push), E_SNAP)
    c.key(1.2, P_(WIDE, push, {"left_hand": [-74, 0, 0]}), E_OUT)
    c.key(1.5, P_(SPREAD, IDLE), E_IO)

    # Flash (14): arms flung wide -- blinding grace.
    c = clip("flash", 1.3)
    gather = P_(GRIP_R, {"right_arm": [-30, 0, -20], "left_arm": [-30, 0, 20], "right_forearm": [-90, 0, 0], "left_forearm": [-90, 0, 0],
                         "body": [14, 0, 0], "head": [14, 0, 0], "@root": [0, -0.8, 0]})
    burst = P_(GRIP_R, {"right_arm": [-10, 0, 100], "left_arm": [-10, 0, -100], "right_forearm": [0, 0, 0], "left_forearm": [0, 0, 0],
                        "left_fingers": [-20, 0, 0], "body": [-14, 0, 0], "head": [-30, 0, 0], "@root": [0, 0.6, 0]})
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.5, P_(FOLDED, gather), E_IO)
    c.key(0.7, P_(WIDE, burst), E_SNAP)
    c.key(1.0, P_(WIDE, burst, {"head": [-34, 0, 0]}))
    c.key(1.3, P_(SPREAD, IDLE), E_IO)

    # Unfurl (20): the wings open (phase II) -- a gathering, then the spread.
    c = clip("unfurl", 1.8, hold=True)
    c.key(0, P_(FOLDED, IDLE))
    c.key(0.6, P_(FOLDED, IDLE, {"body": [16, 0, 0], "head": [20, 0, 0], "right_arm": [10, 0, 20], "left_arm": [10, 0, -20],
                                  "@root": [0, -0.6, 0]}), E_IO)
    c.key(1.0, P_(UNFURL, IDLE, {"body": [-12, 0, 0], "head": [-20, 0, 0], "right_arm": [-10, 0, 40], "left_arm": [-10, 0, -40],
                                  "@root": [0, 0.8, 0]}), E_SNAP)
    c.key(1.4, P_(WIDE, IDLE, {"body": [-6, 0, 0], "head": [-10, 0, 0], "@root": [0, 0.4, 0]}), E_OUT)
    c.key(1.8, P_(SPREAD, IDLE), E_IO)

    # Command (10): the blade arm points -- his rebels attack.
    c = clip("command", 1.0)
    aim = P_(GRIP_R, {"right_arm": [-86, 6, 4], "right_forearm": [-6, 0, 0], "body": [0, 12, 0], "head": [-4, -8, 0],
                      "left_arm": [-6, 0, -8], "left_forearm": [-20, 0, 0]})
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.35, P_(SPREAD, aim, {"right_arm": [-60, 6, 4]}), E_OUT)
    c.key(0.5, P_(WIDE, aim), E_SNAP)
    c.key(0.75, P_(SPREAD, aim))
    c.key(1.0, P_(SPREAD, IDLE), E_IO)

    # Kneel (16): beaten, down on his right knee, head bowed, the wings drooping.
    c = clip("kneel", 1.2, hold=True)
    kneel = {"@root": [0, -5.8, 0], "body": [16, 0, 0], "head": [24, 0, 0], "right_leg": [6, 0, 2], "right_shin": [86, 0, 0],
             "left_leg": [-84, -6, -2], "left_shin": [82, 0, 0], "right_arm": [-4, 0, 10], "left_arm": [-44, 0, -10],
             "left_forearm": [-34, 0, 0], "coat_r": [2, 0, 2], "coat_l": [-70, -6, 0], "@coat_l": [0, 1.2, 0], "coat_tail": [30, 0, 0]}
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.5, P_(SPREAD, IDLE, {"body": [26, 0, 4], "head": [20, 0, 0], "@root": [0, -0.8, 0]}), E_OUT)
    c.key(0.8, P_(FOLDED, GRIP_R, kneel, {"body": [24, 0, 0], "@root": [0, -6.2, 0]}), E_IN)
    c.key(1.2, P_(FOLDED, GRIP_R, kneel), E_IO)

    # Death (60): the angel's death -- rigid, head back, arms out, light out of eyes and mouth; he falls with the wings spread.
    c = clip("death", 4.5, hold=True)
    rigid = {"head": [-38, 0, 0], "body": [-10, 0, 0], "right_arm": [-12, 0, 82], "left_arm": [-12, 0, -82], "right_forearm": [-6, 0, 0],
             "left_forearm": [-6, 0, 0], "right_leg": [4, 0, 5], "left_leg": [4, 0, -5], "@root": [0, 0.6, 0.6]}
    c.key(0, P_(SPREAD, IDLE))
    c.key(0.3, P_(WIDE, GRIP_R, {"body": [20, 0, 0], "head": [14, 0, 0], "@root": [0, -0.6, 0.6]}), E_SNAP)
    c.key(1.2, P_(WIDE, rigid), E_OUT)
    for i, t in enumerate((1.6, 2.0, 2.4, 2.8)):
        j = jitter("uriel-death", i, 2.5)
        c.key(t, P_(WIDE, rigid, {"head": [-40 + j[0], j[1], j[2]], "body": [-11 + j[2] * 0.5, 0, 0]}))
    c.key(3.0, P_(WIDE, rigid, {"head": [-44, 0, 0]}))
    fall = {"@root": [0, -1.0, 6.0], "body": [-86, 0, 0], "head": [-6, 0, 0], "right_arm": [-170, 0, 50], "left_arm": [-170, 0, -50],
            "right_leg": [6, 0, 6], "left_leg": [6, 0, -6], "coat_tail": [86, 0, 0], "coat_r": [-30, 0, -10], "coat_l": [-30, 0, 10]}
    c.key(3.8, P_(WIDE, fall), E_IN)
    c.key(4.5, P_(WIDE, fall))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The bust and the bar
# =====================================================================================================

def bust():
    def tex16(fn):
        t = Tex(16, 16, 0)
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                if c is not None:
                    t.set(x, y, c)
        return t
    skin = lambda x, y: tone(SKIN, 3.2 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.3 * y / 15)
    beard = lambda x, y: tone(BEARD, 2.0 + 0.8 * h01("bb", x, y))
    legend = {"s": skin, "S": lambda x, y: shade(skin(x, y), 1.1), "k": lambda x, y: shade(skin(x, y), 0.85), "b": BEARD[1],
              "U": lambda x, y: shade(skin(x, y), 0.75), "u": lambda x, y: shade(skin(x, y), 0.85), "w": LIGHT, "I": LIGHT_HOT,
              "n": SKIN[4], "N": SKIN[1], "m": beard, "d": beard, "l": LIPS[1], "L": LIPS[3],
              "p": lambda x, y: mix(skin(x, y), BEARD[2], 0.5)}

    def face(x, y):
        v = legend.get(FACE[y][x], skin)
        return v(x, y) if callable(v) else v
    for n, t in (("face", tex16(face)), ("skin", tex16(lambda x, y: shade(skin(x, y), 1.05))),
                 ("coat", tex16(lambda x, y: tone(COAT, 2.9 - y / 14 + 0.4 * math.sin(x * 0.9 + y * 0.4)))),
                 ("chest", tex16(lambda x, y: tone(KNIT, 2.8 + (0.7 if x % 2 == 0 else 0)) if abs(x - 7.5) < 3.2 - 0.05 * y
                                 else tone(COAT, 3.0 - y / 14 + 0.3 * math.sin(x * 1.3)))),
                 ("feather", tex16(lambda x, y: mix(AL.PEARL[2 + (x + y) % 3], AL.GOLD[3], 0.25 if y < 6 else 0.0)
                                   if abs(x - 7.5) < 7 - y * 0.35 else None))):
        save(t, "block", f"uriel_trophy_{n}")
    full = [0, 0, 16, 16]
    F, S, C, CH, W = "face", "skin", "coat", "chest", "feather"
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": C}),
        BM.cube("chest", (4.5, 3.4, 6.0), (11.5, 7.6, 10.0), {"north": CH, "*": C}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (3.9, 6.0, 6.2), (12.1, 7.8, 9.8), {"*": C}),
        BM.cube("neck", (6.9, 7.4, 6.9), (9.1, 8.8, 9.1), {"*": CH}),
        BM.cube("head", (5.8, 8.6, 5.8), (10.2, 13.2, 10.2), {"north": F, "*": S}, uv={"north": full}),
        BM.cube("nose", (7.7, 10.1, 5.55), (8.3, 11.0, 5.8), {"*": S}),
        BM.cube("wing_r", (2.6, 5.0, 9.6), (6.0, 12.5, 10.2), {"*": W}, rot=("z", 22.5, (6.0, 7.0, 9.9))),
        BM.cube("wing_l", (10.0, 5.0, 9.6), (13.4, 12.5, 10.2), {"*": W}, rot=("z", -22.5, (10.0, 7.0, 9.9))),
    ]
    mdl = BM.model({"base": "trophy_base", F: "uriel_trophy_face", S: "uriel_trophy_skin", C: "uriel_trophy_coat",
                    CH: "uriel_trophy_chest", W: "uriel_trophy_feather"}, e, "trophy_base")
    BM.write("uriel_trophy", mdl)


BRONZE = [hexc(c) for c in ("#1a1206", "#2a1d0b", "#3d2a12", "#55391a", "#6e4b24", "#8a5f30")]
LEAF = [hexc(c) for c in ("#5a3c0c", "#86601a", "#b08a2c", "#d4ae48", "#ecd27a", "#fff2b8")]
ASH = [hexc(c) for c in ("#0c0a08", "#1a1612", "#2a241e", "#3c342c", "#524840", "#6e645a")]
FEATHER = [hexc(c) for c in ("#3a3836", "#5a5752", "#7a7670", "#9a958e", "#bab4ac", "#dcd6cc")]
TROUGH = (34, 22, 188, 10)


def _put(t, x, y, c, a=1.0):
    x, y = int(x), int(y)
    if not (0 <= x < t.w and 0 <= y < t.h) or a <= 0:
        return
    d = t.rows[y][x]
    a = min(1.0, a)
    if d[3] == 0:
        t.rows[y][x] = (c[0], c[1], c[2], int(255 * a))
        return
    da = d[3] / 255
    oa = a + da * (1 - a)
    t.rows[y][x] = tuple(int((c[i] * a + d[i] * da * (1 - a)) / oa) for i in range(3)) + (int(255 * oa),)


def _r(r, v):
    return r[max(0, min(len(r) - 1, int(round(v))))]


def _feather(t, ox, oy, ang, L, wd, col_fn):
    """A feather from (ox, oy) along angle `ang` (degrees, 0 = right), length L, half width wd."""
    ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
    for i in range(int(L * 2)):
        u = i / (L * 2)
        half = wd * (math.sin(math.pi * min(1.0, u * 1.3)) if u < 0.75 else (1 - u) * 4 * wd / wd * 0.8)
        for k in range(-int(half * 2) - 1, int(half * 2) + 2):
            v = k / 2
            if abs(v) > half:
                continue
            x = ox + ca * u * L - sa * v
            y = oy + sa * u * L + ca * v
            _put(t, x, y, col_fn(u, v / max(0.5, half)))


def bar_sheet():
    t = Tex(256, 128, 42900)
    # The band: dark bronze with scorched gold leaf, ragged burnt edges.
    for y in range(40):
        for x in range(256):
            e = min(x, 255 - x, y * 2.5, (39 - y) * 2.5)
            wob = 3.0 * fbm(x * 0.6, y * 0.6, 42901, 64, 64, 2, 3.0)
            if e < wob:
                continue
            n = fbm(x, y, 42902, 256, 64, 3, 16.0)
            c = _r(BRONZE, 2.6 + (n - 0.5) * 1.6)
            leaf = fbm(x * 1.4 + 9, y * 1.4, 42903, 256, 64, 2, 10.0)
            if leaf > 0.6 and 6 < x < 250:
                c = mix(c, _r(LEAF, 2.0 + (leaf - 0.6) * 8), 0.6)
            elif 3 <= y <= 4 or 35 <= y <= 36:
                c = mix(c, LEAF[2], 0.5)                      # a gilt rule along the top and bottom
            edge = max(0.0, 1 - (e - wob) / 6.0)
            if edge > 0:
                c = mix(c, ASH[1], edge * 0.85)
            _put(t, x, y, c)
    # Feathers fanning out at both ends (grey, burnt at their tips).
    for side in (-1, 1):
        cx = 18 if side < 0 else 237
        for i in range(5):
            ang = (180 if side < 0 else 0) + side * (-50 + i * 25) * (1 if side > 0 else -1)
            col = lambda u, v, i=i: mix(_r(FEATHER, 3.6 - abs(v) * 1.5 + 0.6 * (i % 2)), ASH[1], max(0.0, (u - 0.7) * 2.5))
            _feather(t, cx, 20, ang, 14, 3.2, col)
    # The trough.
    tx, ty, tw, th = TROUGH
    for x in range(tx - 1, tx + tw + 1):
        _put(t, x, ty - 1, BRONZE[0])
        _put(t, x, ty + th, LEAF[4])
    for y in range(ty - 1, ty + th + 1):
        _put(t, tx - 1, y, BRONZE[0])
        _put(t, tx + tw, y, LEAF[4])
    for y in range(ty, ty + th):
        for x in range(tx, tx + tw):
            _put(t, x, y, ASH[0])
    # Fill: molten gold light.
    for y in range(10):
        for x in range(188):
            n = fbm(x, y, 42910, 188, 10, 2, 6.0)
            v = 3.6 - abs(y - 3.0) * 0.45 + (n - 0.5) * 1.2
            c = _r(LEAF, v)
            if y <= 1:
                c = mix(c, LEAF[5], 0.6)
            _put(t, x, 40 + y, c)
    # Drained: charred gold, ash.
    for y in range(10):
        for x in range(188):
            n = fbm(x * 1.6, y * 2, 42911, 188, 10, 2, 5.0)
            c = _r(ASH, 1.6 + (n - 0.5) * 2.0)
            if h01("gl", x, y) < 0.04:
                c = mix(c, LEAF[1], 0.6)
            _put(t, x, 50 + y, c)
    # Wings, one per phase: whole, burning, a burnt stump.
    for s in range(3):
        ox, oy = s * 24, 64
        for i in range(5):
            ang = -150 + i * 22
            if s == 2 and i > 1:
                continue
            L = 14 if s < 2 else 6

            def col(u, v, s=s, i=i):
                base = _r(FEATHER, 3.6 - abs(v) * 1.4 + 0.5 * (i % 2))
                if s == 1 and u > 0.55:
                    return mix(LEAF[4], hexc("#ff7a20"), (u - 0.55) * 2)
                if s == 2:
                    return _r(ASH, 2.4 - abs(v))
                return base
            _feather(t, ox + 20, oy + 18, ang, L, 2.6, col)
    # Ember frames: the burning edge, sparks lifting.
    for fr in range(6):
        ox, oy = 96 + fr * 8, 64
        for y in range(16):
            for x in range(8):
                d = abs(x - 3.5)
                k = 1 - y / 16.0
                if y >= 6 and d < 1.6:
                    _put(t, ox + x, oy + y, LEAF[5] if d < 0.8 else LEAF[4])
                elif y < 6 and h01("sp", fr, x, y) < 0.06 + 0.04 * k:
                    _put(t, ox + x, oy + y, hexc("#ffb040"), 0.9)
        for k in range(3):
            sy = 5 - ((fr * 2 + k * 3) % 6)
            _put(t, ox + 2 + k * 2, oy + sy, LEAF[5])
    return t


# =====================================================================================================
# Output
# =====================================================================================================

def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=42000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    bust()
    save(bar_sheet(), "gui", "uriel_bar")


if __name__ == "__main__":
    generate()
