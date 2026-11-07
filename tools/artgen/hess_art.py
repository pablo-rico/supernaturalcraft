"""Dr. Mildred Hess (v0.41, season 12): her rig, texture, clips (names and timings parsed from HessAssets), her trophy bust
(`block/hess_trophy`). Shared by the boss and her envoy at dawn. Her bar is in s1214_gui_art.

A woman in her sixties (31 px to the crown, slim, very straight): silver hair drawn back into a tight bun; a long composed face,
fine lines, thin lips, rimless glasses (`glasses`). A heather-brown tweed skirt suit -- a fitted jacket closed with two buttons
over a white blouse, the skirt to below the knee -- dark stockings, low black court shoes. The Order's aquarian star pinned to
her left lapel (`brooch`). A black leather folio held in her left hand (`folio`; its front cover, `folio_cover`, opens when she
reads), a slim wand of brass and bone in her right (`wand`).

Skeleton: apoc_vessel's (no coat) + bun (head), glasses (head), brooch (body), folio -- folio_cover (left_hand), wand
(right_hand). No clip keys folio, wand, glasses or brooch (the renderer's); `folio_read` opens folio_cover.
"""

import math

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, eye_shell, hair_m, head, legs, neck, skeleton, skin_m, tone,
                         torso)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, fbm, hexc, mix, shade

NAME = "hess"
A = "animation.hess."
C = K.Contract("entity/boss/hess/HessAssets")

B = Body(HIP=12.0, NECK=22.8, TW=3.35, TD=1.85, AW=2.8, SY=22.1, ELBOW=17.6, WRIST=13.4, PALM=11.6, LX=1.7, LW=3.2, KNEE=6.2,
         SHOE_H=1.2)

SKIN = Ramp("#5c463f", "#8a6e64", "#b0948a", "#c8ada2", "#dac3b9", "#eadbd3")
HAIR = Ramp("#5a5a5e", "#7c7c80", "#9c9ca0", "#b9b9bc", "#d2d2d4", "#ececee")
TWEED = Ramp("#2a1d18", "#3d2b23", "#523b30", "#68493c", "#7d5a4a", "#946e5c")
FLECK = (hexc("#8a7a5a"), hexc("#5e6a52"), hexc("#7a4a3a"), hexc("#a08f80"))
BLOUSE = Ramp("#8e9094", "#b4b6ba", "#cfd1d4", "#e1e3e5", "#eeeff0", "#fafafa")
NYLON = Ramp("#120f10", "#1d191a", "#2a2425", "#383031", "#473d3e", "#5a4e4f")
SHOE = Ramp("#020203", "#060608", "#0d0d10", "#16161a", "#24242a", "#55555f")
LEATHER = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3e3e46")
BRASS = Ramp("#3b2a10", "#5e4519", "#836226", "#a98236", "#cba354", "#e9c880")
BONE = Ramp("#5e5544", "#857a63", "#a89c82", "#c4b99e", "#d9cfb6", "#ece4d0")
LIPS = Ramp("#4a2c2c", "#6a3e3e", "#865050", "#9a6060", "#ad7070", "#bf8484")
SILVER = Ramp("#4a4f58", "#6e7580", "#959ca6", "#bcc2ca", "#dde1e6", "#ffffff")
EYE_WHITE, IRIS = hexc("#d6d0c8"), hexc("#4a5a6a")
GLASS, FRAME = hexc("#cfe0ea", 70), hexc("#b8b0a0")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssssssshhh",
    "hhsssjjsssjjsshh",
    "hssssssssssssssh",
    "hsbbbbssssbbbbsh",
    "sUUuuussssuuuUUs",
    "skuwIIwsswIIwuks",
    "sskUussssssuUkss",
    "skssssnnssssssks",
    "sjssssnnssssssjs",
    "sjsssNnnNssssjss",
    "ssjssssssssssjss",
    "ssjsslLMMLlsjsss",
    "sssjssssssssjsss",
    "ssssjjjjjjjjssss",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def tweed(seed, base=3.0):
    cl = fabric(TWEED, seed, base, 0.25)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        r = h01("tw", seed, f, x, y)
        if r < 0.16:
            c = mix(c, FLECK[int(r * 25) % 4], 0.45)
        if (x + 2 * y) % 4 == 0:
            c = shade(c, 1.07)                     # the herringbone's slant
        return c
    return m


def hair_silver(seed):
    base = hair_m(HAIR, seed, base=3.0)

    def m(f, x, y, w, h):
        c = base(f, x, y, w, h)
        if f in ("up", "south") and (x + y // 2) % 3 == 0:
            c = shade(c, 1.1)                      # combed back in fine lines
        return c
    return m


def build_head(m, seed=43001):
    sk = skin_m(SKIN, seed, 3.2, 0.3)
    hr = hair_silver(seed + 1)
    lg = K.legend(sk, hr, EYE_WHITE, IRIS, LIPS, SKIN, brow=HAIR[1])
    sides = H.head_sides(sk, hr, top=3, back=13, temple=7, sideburn=4)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 0 else hr(f, x, y, w, h), shell_inflate=0.12, shell_rows=1.5)
    K.brows(m, B, HAIR[1], tilt=-6, w=2.0, h=0.35)
    K.bun(m, B, hr, size=3.2)
    K.glasses(m, B, FRAME, GLASS, rimless=True)
    neck(m, B, sk, 2.8)


def build_suit(m, seed=43200):
    blouse = fabric(BLOUSE, seed + 1, 3.7, 0.2)
    torso(m, B, lambda f, x, y, w, h: blouse(f, x, y, w, h) if f == "north" else BLOUSE[2], tag="torso")
    tw = tweed(seed + 2)
    front = H.jacket_front(tw, TWEED[4], blouse, tie=None, gap=(6, 10), v_depth=8, buttons=hexc("#2a1a12"), button_rows=(10, 14))
    torso(m, B, front, inflate=0.28, bottom=B.HIP - 1.4, top=B.NECK - 0.2, tag="jacket", faces_off=("down",))
    # The blouse's soft collar over the jacket's notch, and the jacket's own lapels.
    for s in (-1, 1):
        m.cube("body", (s * 1.2 - 0.9, B.NECK - 1.5, -B.TD - 0.45), (1.8, 1.4, 0.4), lambda f, x, y, w, h: tone(BLOUSE, 4.6 - y * 0.7),
               density=2, tag="blouse_collar", rotation=(14, 0, -30 * s), pivot=(s * 1.2, B.NECK - 0.2, -B.TD - 0.3))
        m.cube("body", (s * 2.3 - 0.8, B.NECK - 4.2, -B.TD - 0.55), (1.6, 3.6, 0.3), lambda f, x, y, w, h: tone(TWEED, 4.2 - y * 0.25),
               density=2, tag="lapel", rotation=(0, 0, -14 * s), pivot=(s * 2.3, B.NECK - 0.6, -B.TD - 0.4))
    # The brooch: the aquarian star, silver, on her left lapel.
    br = m.bone("brooch", (2.4, B.NECK - 3.4, -B.TD - 0.7), "body")
    star = ["..s..", ".sSs.", "sSGSs", ".sSs.", "..s.."]

    def brooch(f, x, y, w, h):
        if f != "north":
            return SILVER[1]
        ch = star[y][x] if y < 5 and x < 5 else "."
        return {"s": SILVER[3], "S": SILVER[5], "G": hexc("#5a8ab8")}.get(ch)
    m.cube(br, (1.8, B.NECK - 4.0, -B.TD - 0.85), (1.25, 1.25, 0.25), brooch, density=4, tag="brooch")
    sl = tweed(seed + 3, 2.9)
    cuff = lambda f, x, y, w, h: tone(BLOUSE, 4.2 - y * 0.6) if f not in ("up", "down") else BLOUSE[1]
    arms(m, B, sl, skin_m(SKIN, seed + 4, 3.2, 0.3), cuff=cuff, cuff_depth=0.7, sleeve_inflate=0.28)
    nylon = lambda f, x, y, w, h: tone(NYLON, 2.6 + 0.6 * math.sin(x * 0.7) - 0.3 * y / max(1, h) + (0.5 if f == "north" and x in (2, 3) else 0))
    legs(m, B, nylon, SHOE, toe=0.6)
    K.skirt(m, B, tweed(seed + 5, 2.8), hem=4.8, flare=0.3)


def build_folio(m):
    """The black leather folio in her left hand: held upright along the hand, spine down; the front cover opens outward."""
    cx = B.AX
    y0 = B.PALM - 2.2
    fo = m.bone("folio", (cx - 0.9, y0, 0), "left_hand")
    lea = lambda f, x, y, w, h: tone(LEATHER, 2.8 + 0.6 * math.sin(x * 0.8 + y * 0.5) + (1.0 if f == "up" else 0))
    pages = lambda f, x, y, w, h: tone(Ramp("#8a8070", "#b0a690", "#cbc2ae", "#ded6c4", "#ece6d8", "#f8f4ea"), 3.4 - (x % 2) * 0.5)
    m.cube(fo, (cx - 1.0, y0, -2.8), (0.25, 4.4, 5.6), lea, density=2, tag="folio_back")
    m.cube(fo, (cx - 0.78, y0 + 0.15, -2.65), (0.4, 4.1, 5.3), pages, density=2, tag="folio_pages")
    cv = m.bone("folio_cover", (cx - 0.3, y0, 2.8), "folio")

    def cover(f, x, y, w, h):
        c = lea(f, x, y, w, h)
        if f in ("east", "west") and (x in (1, w - 2) or y in (1, h - 2)):
            return BRASS[2]                        # a tooled gilt rule
        return c
    m.cube(cv, (cx - 0.38, y0, -2.8), (0.25, 4.4, 5.6), cover, density=2, tag="folio_cover")


def build_wand(m):
    """A slim wand in her right fist: a brass shaft, a bone grip, a little brass finial; it points along -Z."""
    cx = -B.AX
    y = B.PALM - 0.3
    w = m.bone("wand", (cx, y, 0), "right_hand", rotation=(14, 0, 0))
    m.cube(w, (cx - 0.3, y - 0.3, -1.0), (0.6, 0.6, 2.4), lambda f, x, yy, ww, hh: tone(BONE, 3.6 - (x + yy) % 2 * 0.6), density=4,
           tag="wand_grip")
    m.cube(w, (cx - 0.2, y - 0.2, -6.4), (0.4, 0.4, 5.4), lambda f, x, yy, ww, hh: tone(BRASS, 4.4 if f == "up" else 3.2), density=4,
           tag="wand_shaft")
    m.cube(w, (cx - 0.3, y - 0.3, -6.9), (0.6, 0.6, 0.5), solid(BRASS[5]), density=4, tag="wand_tip")
    m.cube(w, (cx - 0.35, y - 0.35, 1.4), (0.7, 0.7, 0.5), solid(BRASS[2]), density=4, tag="wand_pommel")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=False)
    build_head(m)
    build_suit(m)
    build_folio(m)
    build_wand(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

GRIP = K.fist("right")
HOLDF = {"left_fingers": [70, 0, 0], "left_index": [60, 0, 0], "left_middle": [65, 0, 0], "left_thumb": [-10, 0, -20]}
# Upright, the folio held across her waist in the left hand, the right hand (and the wand) resting over it.
IDLE = P_(GRIP, HOLDF, {"left_arm": [-14, 0, -4], "left_forearm": [-70, 52, 0], "left_hand": [0, 0, -8],
                        "right_arm": [-16, 0, 6], "right_forearm": [-62, -40, 0], "right_hand": [0, 0, 10],
                        "body": [-1, 0, 0], "head": [-2, 0, 0]})
# Walking: the folio carried at her side, the wand hand loose.
CARRY = P_(GRIP, HOLDF, {"left_arm": [-4, 0, -3], "left_forearm": [-24, 0, 0], "right_arm": [-6, 0, 4], "right_forearm": [-18, 0, 0]})
# Composed, the wand drawn: the stance her spells start from.
READY = P_(GRIP, HOLDF, {"right_arm": [-34, 6, 8], "right_forearm": [-30, 0, 0], "left_arm": [-10, 0, -6], "left_forearm": [-40, 20, 0],
                         "body": [-1, -6, 0], "head": [-3, 6, 0]})
OPEN = {"folio_cover": [0, 150, 0]}


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, IDLE, 4.0, head_turn=5, extra=lambda u, b: {"right_hand": [0, 0, 10 + 3 * math.sin(u * 4 * math.pi)]})

    c = clip("walk", 1.0, loop=True)
    K.walk(c, CARRY, 1.0, stride=17, arm=8, bob=0.35, twist=3, knee=(4, 14),
           extra=lambda s: {"head": [-2, -2 * s, 0], "left_arm": [-4 + 4 * s, 0, -3]})

    # --- once ----------------------------------------------------------------------------------------------------
    h, L = C.hit, C.length
    # Talk (4): a small open hand, the folio held.
    c = clip("talk")
    say = P_(IDLE, {"right_arm": [-30, 0, 14], "right_forearm": [-60, -10, 0], "right_hand": [0, -40, -20],
                    "right_fingers": [10, 0, 0], "right_index": [0, 0, 0], "right_middle": [6, 0, 0], "head": [0, -6, 4]})
    c.key(0, IDLE)
    c.key(h("talk"), say, E_OUT)
    c.key(0.9, P_(say, {"head": [-4, 6, -2], "right_hand": [0, -30, -10]}), E_IO)
    c.key(1.4, P_(say, {"head": [2, -4, 3]}), E_IO)
    c.key(L("talk"), IDLE, E_IO)

    # Wand cast (10): drawn back past her ear, then flicked forward -- the bolt.
    c = clip("wand_cast")
    back = P_(READY, {"right_arm": [-120, 20, 20], "right_forearm": [-60, 0, 0], "right_hand": [-20, 0, 0], "body": [-4, -14, 0],
                      "head": [-4, 10, 0]})
    flick = P_(READY, {"right_arm": [-88, -4, 0], "right_forearm": [-6, 0, 0], "right_hand": [24, 0, 0], "body": [4, 10, 0],
                       "head": [-6, -4, 0], "@root": [0, 0, -0.6], "right_leg": [-8, 0, 0], "left_leg": [6, 0, 0]})
    c.key(0, IDLE)
    c.key(0.32, back, E_IO)
    c.key(h("wand_cast"), flick, E_SNAP)
    c.key(0.65, P_(flick, {"right_hand": [16, 0, 0], "right_arm": [-84, -4, 0]}), E_OUT)
    c.key(L("wand_cast"), IDLE, E_IO)

    # Ward raise (14): the wand drawn round in a circle overhead -- the barrier.
    c = clip("ward_raise")
    up = P_(READY, {"right_arm": [-170, 0, -10], "right_forearm": [-10, 0, 0], "head": [-16, 0, 0], "body": [-4, 0, 0]})
    c.key(0, IDLE)
    c.key(0.25, P_(READY, {"right_arm": [-120, 0, 30], "right_forearm": [-20, 0, 0]}), E_IO)
    c.key(0.42, P_(up, {"right_arm": [-170, 30, 10]}), E_IO)
    c.key(0.56, P_(up, {"right_arm": [-170, -30, -20]}), E_IO)
    c.key(h("ward_raise"), P_(up, {"right_arm": [-176, 0, 0], "@root": [0, 0.3, 0]}), E_OUT)
    c.key(1.0, P_(up, {"right_arm": [-168, 0, 0]}))
    c.key(L("ward_raise"), IDLE, E_IO)

    # Summon agents (16): two fingers raised beside her face, then the hand chops forward -- a command.
    c = clip("summon_agents")
    two = {"right_fingers": [80, 0, 0], "right_index": [-10, 0, 0], "right_middle": [-10, 0, 0], "right_thumb": [-20, 0, 30]}
    raise_ = P_(IDLE, two, {"right_arm": [-110, 0, 30], "right_forearm": [-90, -30, 0], "right_hand": [0, 0, 0], "head": [-6, 10, 0]})
    order = P_(IDLE, two, {"right_arm": [-96, -10, 0], "right_forearm": [-10, 0, 0], "right_hand": [10, 0, 0], "head": [-4, -6, 0],
                           "body": [3, 8, 0]})
    c.key(0, IDLE)
    c.key(0.5, raise_, E_IO)
    c.key(0.62, P_(raise_, {"right_arm": [-118, 0, 30]}))
    c.key(h("summon_agents"), order, E_SNAP)
    c.key(1.05, order, E_OUT)
    c.key(L("summon_agents"), IDLE, E_IO)

    # Hex throw (12): an underhand toss of a hex bag (the wand hand swings low and up).
    c = clip("hex_throw")
    low = P_(READY, {"right_arm": [30, 0, 10], "right_forearm": [-20, 0, 0], "body": [10, -14, 0], "head": [-8, 10, 0],
                     "@root": [0, -0.6, 0.4], "right_leg": [-12, 0, 0], "right_shin": [14, 0, 0]})
    toss = P_(READY, {"right_arm": [-80, -10, 4], "right_forearm": [-8, 0, 0], "right_hand": [-30, 0, 0], "body": [-4, 10, 0],
                      "head": [-10, -6, 0], "@root": [0, 0, -0.6]}, K.spread("right"))
    c.key(0, IDLE)
    c.key(0.4, low, E_IO)
    c.key(h("hex_throw"), toss, E_SNAP)
    c.key(0.8, P_(toss, {"right_arm": [-86, -12, 4]}), E_OUT)
    c.key(L("hex_throw"), IDLE, E_IO)

    # Folio read (18): the folio raised and opened before her, she reads the spell aloud.
    c = clip("folio_read")
    hold_up = P_(GRIP, HOLDF, {"left_arm": [-50, 0, -4], "left_forearm": [-60, 40, 0], "left_hand": [-10, 80, 0],
                               "right_arm": [-30, 0, 10], "right_forearm": [-40, -10, 0], "head": [10, 0, 0], "body": [-2, 0, 0]})
    c.key(0, IDLE)
    c.key(0.4, hold_up, E_IO)
    c.key(0.65, P_(hold_up, OPEN), E_OUT)
    c.key(h("folio_read"), P_(hold_up, OPEN, {"right_arm": [-60, 0, 30], "right_forearm": [-40, -20, 0], "head": [4, 0, 0],
                                             "body": [-4, 0, 0], "@root": [0, 0.2, 0]}), E_OUT)
    c.key(1.3, P_(hold_up, OPEN, {"head": [8, 4, 0]}), E_IO)
    c.key(1.55, P_(hold_up, {"folio_cover": [0, 0, 0]}), E_IN)
    c.key(L("folio_read"), IDLE, E_IO)

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, IDLE, L("hurt"), h("hurt"), {"body": [-10, 8, 0], "head": [-16, 12, 0], "@root": [0, 0, 0.8],
                                             "right_arm": [-30, 0, 30], "left_arm": [-20, 0, -14]})

    # Appear (10): steps out, then straightens her jacket with both hands.
    c = clip("appear")
    step = P_(CARRY, {"right_leg": [-18, 0, 0], "left_leg": [10, 0, 0], "left_shin": [10, 0, 0], "@root": [0, 0, -1.0], "body": [4, 0, 0]})
    tug = P_(HOLDF, {"right_arm": [-26, 0, 10], "right_forearm": [-70, -50, 0], "left_arm": [-24, 0, -10], "left_forearm": [-72, 50, 0],
                     "right_fingers": [60, 0, 0], "left_fingers": [70, 0, 0], "body": [-4, 0, 0], "head": [6, 0, 0], "@body": [0, 0.3, 0]})
    c.key(0, step)
    c.key(0.3, P_(CARRY, {"@root": [0, 0, -0.4]}), E_IO)
    c.key(h("appear"), tug, E_OUT)
    c.key(0.7, P_(tug, {"@body": [0, -0.2, 0], "head": [-2, 0, 0]}), E_SNAP)
    c.key(L("appear"), IDLE, E_IO)

    # Leave (10): a nod, then she turns away.
    c = clip("leave")
    c.key(0, IDLE)
    c.key(0.3, P_(IDLE, {"head": [16, 0, 0]}), E_IO)
    c.key(h("leave"), P_(IDLE, {"head": [0, 20, 0], "body": [0, 30, 0], "@root": [0, 0, 0]}), E_IO)
    c.key(L("leave"), P_(CARRY, {"body": [0, 60, 0], "head": [0, 30, 0], "right_leg": [-12, 0, 0], "left_leg": [10, 0, 0]}), E_IO)

    # Death (40): she staggers, a hand to her chest, the glasses slip, and she sinks to the floor.
    c = clip("death", hold=True)
    clutch = P_(HOLDF, {"right_arm": [-40, 0, -10], "right_forearm": [-90, -50, 0], "right_fingers": [40, 0, 0], "body": [16, 0, 0],
                        "head": [14, 0, 6], "left_arm": [-10, 0, -20], "left_forearm": [-20, 0, 0], "@root": [0, -0.3, 0.6],
                        "right_leg": [-10, 0, 0], "left_leg": [8, 0, 0]})
    c.key(0, IDLE)
    c.key(0.25, clutch, E_SNAP)
    for i in range(1, 5):
        j = K.jit("hess-death", i, 4)
        c.key(0.25 + 0.3 * i, P_(clutch, {"body": [16 + 4 * i + j[0], j[1], j[2]], "head": [14 + j[2], j[0], 6], "@root": [0, -0.3 - 0.3 * i, 0.6]}))
    kneel = P_(clutch, {"@root": [0, -5.6, 0.4], "right_leg": [-80, 0, 4], "left_leg": [-60, 0, -4], "right_shin": [100, 0, 0],
                        "left_shin": [80, 0, 0], "body": [24, 0, 0], "head": [20, 0, 10], "right_arm": [-20, 0, -4], "left_arm": [-30, 0, -16]})
    c.key(h("death"), kneel, E_IN)
    fall = K.lie(side=1, lift=3.2)
    c.key(2.6, fall, E_IN)
    c.key(L("death"), fall)

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The bust
# =====================================================================================================

def bust():
    skin = lambda x, y: tone(SKIN, 3.4 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 3.2 + 0.6 * math.sin(x * 1.7 + y * 0.9) + (0.5 if (x + y // 2) % 3 == 0 else 0))
    lg = {"s": skin, "h": hair, "b": HAIR[1], "U": lambda x, y: shade(skin(x, y), 0.78), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.8), "w": EYE_WHITE, "I": IRIS, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "M": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.9)}
    face = K.face_fn(FACE, lg, skin)

    def face_g(x, y):
        c = face(x, y)
        if y == 6 and 1 <= x <= 14 and x not in (7, 8):
            return mix(c, FRAME, 0.5)               # the glasses' top rim
        return c

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 2.6 - 0.25 * y and y < 9:
            return tone(BLOUSE, 4.2 - y / 12)
        if d < 3.6 - 0.25 * y and y < 10:
            return TWEED[4]
        if 10 <= x <= 11 and 4 <= y <= 5:
            return SILVER[4]                         # the brooch
        c = tone(TWEED, 3.0 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
        return mix(c, FLECK[(x * 3 + y) % 4], 0.4) if h01("tb", x, y) < 0.15 else c
    coat = lambda x, y: mix(tone(TWEED, 3.0 - y / 14), FLECK[(x + y) % 4], 0.35 if h01("tc", x, y) < 0.15 else 0)
    import blockmodel as BM
    extra = [BM.cube("bun", (7.0, 12.2, 10.0), (9.0, 14.0, 11.4), {"*": "hair"})]
    K.bust("hess", face_g, hair, skin, chest, coat, extra=extra, shoulders_w=7.0, head_w=4.0)


# =====================================================================================================
# Output
# =====================================================================================================

def build():
    m = rig()
    t, g = m.build(gutter=1, seed=43000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("wand_cast", 0.5), ("folio_read", 0.9), ("death", 3.0))):
    m, t, g, f, clips = build()
    return K.render(m, t, clips, out_dir, NAME, poses)


if __name__ == "__main__":
    generate()
