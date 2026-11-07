"""Asmodeus, Prince of Hell (v0.41, season 13): his rig, texture + glowmask, clips (names and timings parsed from
AsmodeusAssets) and his trophy bust (`block/asmodeus_trophy`). His bar is in s1214_gui_art.

A portly Southern gentleman (33 px to the crown, broad, a round belly): grey hair slicked back, a neat grey goatee, a round
smiling face. An immaculate white three-piece suit -- the jacket open on the waistcoat, its lower fronts on `jacket_r` /
`jacket_l` -- a white shirt, a black string tie; white shoes. A white wide-brimmed hat with a black band (`hat`). A black cane
with a silver head in his right fist (`cane`, planted on the ground when his hand turns it upright). His eyes burn yellow
(`eyes`, lit). A vial of archangel's grace on a gold chain at his chest (`vial`, its light lit), the shard of the Lance at his
left hip (`shard`, pale blue-white metal).

Extra bone the clips own: `vial_held` (left_hand: the vial uncorked in his hand, scale 0 except in grace_drink; every loop keys
it to 0). No clip keys hat, cane, vial, shard or eyes.
"""

import math

import horsemen_art as H
import s1214_kit as K
from angel_art import open_front
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, eye_shell, hair_m, head, legs, neck, skeleton, skin_m, tone,
                         torso)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

NAME = "asmodeus"
A = "animation.asmodeus."
C = K.Contract("entity/boss/asmodeus/AsmodeusAssets")

B = Body(HIP=12.6, NECK=24.6, TW=4.6, TD=2.6, AW=3.6, SY=23.9, ELBOW=19.0, WRIST=14.3, PALM=12.4, LX=2.2, LW=4.0, KNEE=6.6,
         SHOE_H=1.3)

SKIN = Ramp("#5e443a", "#8e6a5a", "#b48a76", "#cba18a", "#dcb69e", "#ebcbb6")
HAIR = Ramp("#4a4a4c", "#6a6a6c", "#8a8a8c", "#a8a8aa", "#c4c4c6", "#dededf")
WHITE = Ramp("#8e8a80", "#b0aca2", "#ccc8be", "#dfdcd3", "#ecead4", "#faf9f2")
TIE = Ramp("#040405", "#08080a", "#0e0e11", "#151519", "#1e1e23", "#2a2a31")
CANE = Ramp("#050505", "#0c0b0b", "#151313", "#1f1c1b", "#2b2725", "#3c3633")
SILVER = Ramp("#4a4f58", "#6e7580", "#959ca6", "#bcc2ca", "#dde1e6", "#ffffff")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
SHARD = Ramp("#4a5a6e", "#6e84a0", "#96acc6", "#bcd0e6", "#dceaf8", "#f6fbff")
LIPS = Ramp("#4a2a26", "#663a34", "#824c44", "#985c52", "#ac6c60", "#be7e70")
EYE_HOT, EYE, EYE_DIM = hexc("#fffbd0"), hexc("#ffe040"), hexc("#a08010")
GRACE_HOT, GRACE = hexc("#fffbe8"), hexc("#ffe7a0")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hsssssssssssssss",
    "ssssssssssssssss",
    "ssbbbbssssbbbbss",
    "sUUuuussssuuuUUs",
    "kswIIwsssswIIwsk",
    "ssUUssssssssUUss",
    "ksssssnnssssssks",
    "jsssnnnnnnsssssj",
    "jsssNnnnnNssssjj",
    "jjsgggggggggsjjs",
    "jjsglLMMMLlgsjjs",
    "jjsggggggggggjjs",
    "sjjsgggggggsjjss",
]
FACE = [r[:16] for r in FACE]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=49001):
    sk = skin_m(SKIN, seed, 3.2, 0.35)
    hr = hair_m(HAIR, seed + 1, base=3.0)
    slick = lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.15) if (f in ("up", "south") and x % 3 == 0) else hr(f, x, y, w, h)
    goatee = lambda f, x, y, w, h: tone(HAIR, 2.6 + 0.8 * h01("gt", seed, f, x, y))
    lg = K.legend(sk, slick, hexc("#dcd4c4"), hexc("#5a4a2a"), LIPS, SKIN, brow=HAIR[2], extra={"g": goatee})
    sides = H.head_sides(sk, slick, top=3, back=12, temple=9, sideburn=6)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" else slick(f, x, y, w, h), shell_inflate=0.2, shell_rows=1.5)
    K.brows(m, B, HAIR[2], tilt=-6, w=2.4, h=0.5)
    eye_shell(m, B, "dYHYd..dYHYd", {"d": EYE_DIM, "Y": EYE, "H": EYE_HOT})
    # The goatee's point under the chin, and a full jowl.
    m.cube("head", (-1.2, B.NECK - 1.0, -4.3), (2.4, 1.4, 1.2), none_on(("up",), goatee), density=2, tag="goatee")
    neck(m, B, sk, 4.2)
    # The hat: a white crown with a black band, a wide brim.
    ht = m.bone("hat", (0, B.crown, 0), "head")
    felt = lambda f, x, y, w, h: tone(WHITE, 3.8 + (0.8 if f == "up" else 0) - 0.4 * y / max(1, h) + 0.2 * math.sin(x * 0.7))
    m.cube(ht, (-4.6, B.crown - 0.6, -4.6), (9.2, 3.4, 9.2), lambda f, x, y, w, h: TIE[2] if f not in ("up", "down") and y >= h - 2
           else felt(f, x, y, w, h), density=2, tag="hat_crown")
    m.cube(ht, (-3.4, B.crown + 2.8, -3.6), (6.8, 0.6, 7.2), felt, density=2, tag="hat_top")
    m.cube(ht, (-7.0, B.crown - 0.9, -7.0), (14.0, 0.5, 14.0), lambda f, x, y, w, h: felt(f, x, y, w, h) if f != "down" else WHITE[1],
           density=1, tag="hat_brim")


def build_suit(m, seed=49200):
    shirt = fabric(WHITE, seed + 1, 4.0, 0.2)
    suit = fabric(WHITE, seed + 2, 3.3, 0.35)

    def waistcoat(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 6 and abs(x - mid) < 2.6 - y * 0.35:
            if abs(x - mid) < 0.6 and y < 5:
                return TIE[2]                                         # the string tie
            return shirt(f, x, y, w, h)
        if f == "north" and abs(x - mid) < 0.6 and y % 3 == 1:
            return GOLD[3]                                            # gilt buttons
        if f == "north" and y == 12 and x > mid and x < mid + 5:
            return GOLD[4] if x % 2 else GOLD[2]                      # the watch chain
        return tone(WHITE, 3.0 + 0.3 * math.sin(x * 0.7 + y * 0.4) - 0.4 * y / max(1, h))
    torso(m, B, waistcoat, tag="torso")
    # The belly, round under the waistcoat.
    m.cube("body", (-3.6, B.HIP + 0.6, -B.TD - 1.2), (7.2, 5.4, 1.4), lambda f, x, y, w, h: waistcoat(f, x, y + 10, w, h + 10) if f != "up"
           else WHITE[3], inflate=0.1, density=2, tag="belly")
    torso(m, B, open_front(suit, half=2.6, collar=0, lapel=WHITE[5]), inflate=0.35, bottom=B.HIP + 1.2, top=B.NECK - 0.2, tag="jacket")
    for nm, s in (("jacket_r", -1), ("jacket_l", 1)):
        jb = m.bone(nm, (s * 2.8, B.HIP + 1.4, -B.TD - 1.2), "body")
        xa, xb = (-B.TW - 0.5, -1.6) if s < 0 else (1.6, B.TW + 0.5)
        m.cube(jb, (xa, B.HIP - 2.0, -B.TD - 1.6), (xb - xa, 3.6, 0.45), none_on(("up",), suit), density=2, tag="jacket_front",
               rotation=(-8, 0, 0), pivot=(0, B.HIP + 1.4, -B.TD - 1.2))
        m.cube(jb, (xa if s < 0 else xb - 0.5, B.HIP - 2.0, -B.TD - 1.2), (0.5, 3.6, 2 * B.TD + 1.6), none_on(("up",), suit), density=2,
               tag="jacket_hip")
    m.cube("body", (-B.TW - 0.5, B.HIP - 2.0, B.TD + 0.2), (2 * B.TW + 1.0, 3.6, 0.45), none_on(("up",), suit), density=2, tag="jacket_back")
    for s in (-1, 1):
        m.cube("body", (s * 2.9 - 0.9, B.NECK - 6.4, -B.TD - 0.7), (1.8, 6.0, 0.3), lambda f, x, y, w, h: tone(WHITE, 4.6 - y * 0.15),
               density=2, tag="lapel", rotation=(0, 0, -10 * s), pivot=(s * 2.9, B.NECK - 0.6, -B.TD - 0.5))
    cuff = lambda f, x, y, w, h: tone(WHITE, 4.6) if f not in ("up", "down") else WHITE[2]
    arms(m, B, fabric(WHITE, seed + 3, 3.2, 0.35), skin_m(SKIN, seed + 4, 3.2, 0.35), cuff=cuff, cuff_depth=0.6, sleeve_inflate=0.35)
    legs(m, B, fabric(WHITE, seed + 5, 3.1, 0.3, crease=True), Ramp("#6a665c", "#8e8a80", "#b0aca2", "#ccc8be", "#e2dfd6", "#faf9f2"))
    # A ring on his left hand.
    m.cube("left_hand", (B.AX - 1.3, B.PALM - 0.3, -1.5), (0.5, 0.5, 0.6), solid(GOLD[4]), density=4, tag="ring")


def vial_parts(m, bone, x, y, z, tag):
    """A small glass vial of grace: the glass, the gold light inside, a gold cap."""
    def glass(f, xx, yy, w, h):
        if yy < 1:
            return (220, 230, 240, 130)
        return GRACE_HOT if (xx + yy) % 3 == 0 else GRACE
    m.cube(bone, (x - 0.45, y, z - 0.45), (0.9, 1.8, 0.9), glass, glow=lambda f, xx, yy, w, h, c: c if c[3] == 255 else None, density=4,
           tag=tag, shading=False)
    m.cube(bone, (x - 0.35, y + 1.8, z - 0.35), (0.7, 0.4, 0.7), solid(GOLD[4]), density=4, tag=tag + "_cap")


def build_props(m):
    # The cane in his right fist: the silver knob in the fist, the black shaft straight down to a silver ferrule.
    cx, y = -B.AX, B.PALM
    cn = m.bone("cane", (cx, y, 0), "right_hand")
    m.cube(cn, (cx - 0.7, y - 0.4, -0.7), (1.4, 1.6, 1.4), lambda f, xx, yy, w, h: tone(SILVER, 4.4 - yy * 0.4 + (0.6 if f == "up" else 0)),
           density=4, tag="cane_knob")
    m.cube(cn, (cx - 0.35, y - 12.2, -0.35), (0.7, 11.8, 0.7), lambda f, xx, yy, w, h: tone(CANE, 2.6 + (1.2 if xx == 0 else 0)), density=2,
           tag="cane_shaft")
    m.cube(cn, (cx - 0.4, y - 12.6, -0.4), (0.8, 0.6, 0.8), solid(SILVER[3]), density=4, tag="cane_ferrule")
    # The vial on its gold chain, at his chest.
    vb = m.bone("vial", (0.9, B.NECK - 3.0, -B.TD - 1.0), "body")
    m.cube(vb, (-1.4, B.NECK - 2.6, -B.TD - 0.75), (2.8, 0.2, 0.2), lambda f, xx, yy, w, h: GOLD[4] if xx % 2 else GOLD[2], density=4,
           tag="vial_chain", rotation=(0, 0, -30), pivot=(0, B.NECK - 1.0, -B.TD - 0.7))
    vial_parts(m, vb, 1.4, B.NECK - 5.4, -B.TD - 1.1, "vial_glass")
    # The same vial, uncorked in his left hand (grace_drink only).
    vh = m.bone("vial_held", (B.AX, B.PALM, -0.6), "left_hand")
    vial_parts(m, vh, B.AX, B.PALM - 0.8, -1.2, "vial_in_hand")
    # The shard of the Lance at his left hip: a broken spearhead of pale blue-white metal, wrapped at its base.
    sh = m.bone("shard", (B.TW + 0.6, B.HIP + 0.4, 0), "body")

    def metal(f, xx, yy, w, h):
        return tone(SHARD, 4.2 - 0.4 * yy / max(1, h) + (0.8 if xx == 0 else 0))
    m.cube(sh, (B.TW + 0.45, B.HIP - 3.6, -0.6), (0.4, 3.6, 1.2), metal, glow=lambda f, xx, yy, w, h, c: shade(c, 0.35), density=4,
           tag="shard_blade", rotation=(0, 0, -8), pivot=(B.TW + 0.6, B.HIP, 0))
    m.cube(sh, (B.TW + 0.45, B.HIP - 4.4, -0.3), (0.4, 0.8, 0.6), metal, density=4, tag="shard_tip", rotation=(0, 0, -8),
           pivot=(B.TW + 0.6, B.HIP, 0))
    m.cube(sh, (B.TW + 0.35, B.HIP, -0.5), (0.6, 1.4, 1.0), lambda f, xx, yy, w, h: tone(Ramp("#2a1a10", "#3a2414", "#4c301a", "#5e3c22",
                                                                                                "#704a2a", "#845a34"), 3 - (yy % 2)),
           density=4, tag="shard_wrap", rotation=(0, 0, -8), pivot=(B.TW + 0.6, B.HIP, 0))


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=False)
    build_head(m)
    build_suit(m)
    build_props(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

GRIP = K.fist("right")
HIDE = {"%vial_held": [0, 0, 0]}
SHOW = {"%vial_held": [1, 1, 1]}
# Both hands on the cane, planted before him: the right fist turned so the cane stands upright, the left hand over it.
ON_CANE = P_(GRIP, K.fist("left", 0.6), HIDE, {"right_arm": [-22, 0, -20], "right_forearm": [-6, 0, 0], "right_hand": [26, 0, 20],
                                              "left_arm": [-28, 0, 36], "left_forearm": [-16, 0, 0], "left_hand": [10, 0, -14],
                                              "body": [-3, 0, 0], "head": [-4, 0, 0]})
STROLL = P_(GRIP, K.relax("left"), HIDE, {"right_arm": [-14, 0, 6], "right_forearm": [-20, 0, 0], "right_hand": [34, 0, 0],
                                          "left_arm": [0, 0, -6], "left_forearm": [-12, 0, 0], "body": [-3, 0, 0]})


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, ON_CANE, 4.0, head_turn=7, extra=lambda u, b: {"@body": [0, 0.2 * (1 - math.cos(u * 2 * math.pi)) * 0.5, 0]})
    c = clip("walk", 1.3, loop=True)
    K.walk(c, STROLL, 1.3, stride=16, arm=6, bob=0.35, twist=6, knee=(6, 14),
           extra=lambda s: {"right_arm": [-14 - 14 * s, 0, 6], "right_hand": [34 + 14 * s, 0, 0], "jacket_r": [-6 * s, 0, 0],
                            "jacket_l": [6 * s, 0, 0], "head": [-3, -3 * s, 2 * s]})

    # Cane strike (7): a swing of the cane, backhand to forehand.
    c = clip("cane_strike")
    wind = P_(GRIP, HIDE, {"right_arm": [-110, 30, 40], "right_forearm": [-30, 0, 0], "right_hand": [0, 0, 0], "body": [0, -24, 0],
                           "head": [-4, 14, 0], "left_arm": [-20, 0, -20]})
    swing = P_(GRIP, HIDE, {"right_arm": [-70, -30, -50], "right_forearm": [-10, 0, 0], "right_hand": [-20, 0, 0], "body": [8, 26, 0],
                            "head": [-6, -14, 0], "@root": [0, -0.4, -1.0], "right_leg": [-16, 0, 0], "left_leg": [10, 0, 0],
                            "jacket_r": [-14, 0, 0], "jacket_l": [-10, 0, 0]})
    c.key(0, ON_CANE)
    c.key(0.22, wind, E_IO)
    c.key(h("cane_strike"), swing, E_SNAP)
    c.key(0.5, P_(swing, {"right_arm": [-64, -34, -54]}), E_OUT)
    c.key(L("cane_strike"), ON_CANE, E_IO)

    # Cane slam (14): the cane raised two-handed overhead and driven down into the floor -- the shockwave.
    c = clip("cane_slam")
    lift = P_(GRIP, K.fist("left", 0.7), HIDE, {"right_arm": [-170, 10, 0], "left_arm": [-170, -10, 0], "right_forearm": [-20, 0, 0],
                                               "left_forearm": [-20, 0, 0], "right_hand": [70, 0, 0], "body": [-10, 0, 0], "head": [-14, 0, 0],
                                               "@root": [0, 0.4, 0]})
    slam = P_(GRIP, K.fist("left", 0.7), HIDE, {"right_arm": [-40, -10, 6], "left_arm": [-44, 16, -6], "right_forearm": [-10, 0, 0],
                                               "left_forearm": [-20, 20, 0], "right_hand": [40, 0, 0], "body": [24, 0, 0], "head": [-10, 0, 0],
                                               "@root": [0, -2.6, -0.6], "right_leg": [-40, 0, 4], "left_leg": [-20, 0, -4], "right_shin": [50, 0, 0],
                                               "left_shin": [40, 0, 0], "jacket_r": [-20, 0, 0], "jacket_l": [-20, 0, 0]})
    c.key(0, ON_CANE)
    c.key(0.5, lift, E_IO)
    c.key(0.6, P_(lift, {"right_arm": [-176, 10, 0], "left_arm": [-176, -10, 0]}))
    c.key(h("cane_slam"), slam, E_SNAP)
    c.key(0.95, P_(slam, {"@root": [0, -2.4, -0.6]}), E_OUT)
    c.key(L("cane_slam"), ON_CANE, E_IO)

    # Grace drink (20): the vial lifted from his chest, uncorked, drunk; gold light fills him.
    c = clip("grace_drink")
    hold = P_(GRIP, SHOW, {"right_arm": [-14, 0, 6], "right_forearm": [-20, 0, 0], "right_hand": [34, 0, 0], "left_arm": [-40, 0, 6],
                           "left_forearm": [-90, 30, 0], "left_hand": [0, 0, 0], "left_fingers": [60, 0, 0], "head": [10, -6, 0]})
    drink = P_(hold, {"left_arm": [-100, 20, 4], "left_forearm": [-90, 40, 0], "left_hand": [-40, 0, 0], "head": [-36, 0, 0], "body": [-8, 0, 0]})
    filled = P_(K.spread("left"), SHOW, GRIP, {"right_arm": [-14, 0, 20], "right_forearm": [-20, 0, 0], "right_hand": [34, 0, 0],
                                               "left_arm": [-20, 0, -50], "left_forearm": [-20, 0, 0], "head": [-24, 0, 0], "body": [-12, 0, 0],
                                               "@root": [0, 0.6, 0]})
    c.key(0, P_(ON_CANE, SHOW))
    c.key(0.5, hold, E_IO)
    c.key(0.75, drink, E_IO)
    c.key(h("grace_drink") - 0.1, P_(drink, {"head": [-44, 0, 0]}), E_IO)
    c.key(h("grace_drink"), filled, E_BACK)
    for i in range(1, 4):
        j = K.jit("asmo-grace", i, 2)
        c.key(h("grace_drink") + 0.25 * i, P_(filled, {"head": [-24 + j[0], j[1], j[2]]}))
    c.key(1.8, P_(STROLL, SHOW), E_IO)
    c.key(L("grace_drink"), P_(ON_CANE, HIDE), E_IO)

    # Grace blast (12): a palm of gold light from his left hand.
    c = clip("grace_blast")
    draw = P_(ON_CANE, K.spread("left"), {"left_arm": [-30, 0, -10], "left_forearm": [-100, 0, 0], "body": [0, -16, 0], "head": [-4, 8, 0]})
    push = P_(ON_CANE, K.spread("left"), {"left_arm": [-90, 0, 0], "left_forearm": [0, 0, 0], "left_hand": [-70, 0, 0], "body": [6, 14, 0],
                                         "head": [-8, -10, 0], "@root": [0, 0, -0.8]})
    c.key(0, ON_CANE)
    c.key(0.4, draw, E_IO)
    c.key(h("grace_blast"), push, E_SNAP)
    c.key(0.85, P_(push, {"left_hand": [-74, 0, 0]}), E_OUT)
    c.key(L("grace_blast"), ON_CANE, E_IO)

    # Summon (14): a snap of the fingers, held up beside his face -- the hounds come.
    c = clip("summon")
    up = P_(ON_CANE, {"left_arm": [-90, 0, -30], "left_forearm": [-80, 30, 0], "left_fingers": [60, 0, 0], "left_middle": [-10, 0, 0],
                      "left_index": [50, 0, 0], "left_thumb": [-10, 0, -40], "head": [-2, -10, 0]})
    snap = P_(up, {"left_middle": [90, 0, 0], "left_thumb": [-30, 0, -10], "left_hand": [0, 0, 14], "head": [-6, -6, 0]})
    c.key(0, ON_CANE)
    c.key(0.5, up, E_IO)
    c.key(h("summon"), snap, E_SNAP)
    c.key(0.95, snap, E_OUT)
    c.key(L("summon"), ON_CANE, E_IO)

    # Shapeshift (10): a shimmer -- he straightens, spreads his arms a little, and bows.
    c = clip("shapeshift")
    open_ = P_(STROLL, K.spread("left"), {"left_arm": [-20, 0, -40], "body": [-6, 0, 0], "head": [-10, 0, 0], "@root": [0, 0.3, 0]})
    bow = P_(STROLL, {"body": [30, 0, 0], "head": [6, 0, 0], "left_arm": [-40, 0, 20], "left_forearm": [-80, 50, 0], "@root": [0, -0.2, 0.4]})
    c.key(0, ON_CANE)
    c.key(0.3, open_, E_IO)
    for i in range(1, 3):
        j = K.jit("asmo-shift", i, 4)
        c.key(0.3 + 0.08 * i, P_(open_, {"body": [-6 + j[0], j[1], j[2]]}))
    c.key(h("shapeshift"), P_(open_, {"head": [-12, 0, 0]}))
    c.key(0.85, bow, E_IO)
    c.key(L("shapeshift"), ON_CANE, E_IO)

    # Taunt (10): he tips his hat -- the left hand up to its brim, a nod.
    c = clip("taunt")
    tip = P_(ON_CANE, {"left_arm": [-150, -20, -30], "left_forearm": [-40, 0, 0], "left_hand": [0, 0, 0], "left_fingers": [40, 0, 0],
                       "head": [14, -10, 0], "body": [6, 0, 0]})
    c.key(0, ON_CANE)
    c.key(0.35, P_(tip, {"head": [0, -10, 0]}), E_IO)
    c.key(h("taunt"), tip, E_OUT)
    c.key(0.8, P_(tip, {"head": [8, -6, 0]}), E_IO)
    c.key(L("taunt"), ON_CANE, E_IO)

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, ON_CANE, L("hurt"), h("hurt"), {"body": [-10, 8, 0], "head": [-16, 12, 0], "@root": [0, 0, 0.8], "left_arm": [-30, 0, -30],
                                                "jacket_r": [-10, 0, 0], "jacket_l": [-10, 0, 0]})

    # Death (50): he staggers, the grace burns out of his eyes and mouth -- head thrown back -- and he falls on his back.
    c = clip("death", hold=True)
    stag = P_(STROLL, {"body": [-10, 0, 0], "head": [-10, 0, 0], "left_arm": [-40, 0, -20], "left_forearm": [-60, 30, 0], "@root": [0, 0, 1.0],
                       "right_leg": [-10, 0, 0], "left_leg": [14, 0, 0]})
    burn = P_(K.spread("left"), K.spread("right"), HIDE, {"body": [-24, 0, 0], "head": [-50, 0, 0], "right_arm": [-14, 0, 60], "left_arm": [-14, 0, -60],
                                                          "@root": [0, 0.4, 1.2], "jacket_r": [-20, 0, -10], "jacket_l": [-20, 0, 10]})
    c.key(0, ON_CANE)
    c.key(0.3, stag, E_SNAP)
    c.key(1.0, P_(stag, {"@root": [0, 0, 2.0], "body": [6, 0, 0]}), E_IO)
    c.key(h("death"), burn, E_BACK)
    for i in range(1, 5):
        j = K.jit("asmo-death", i, 3)
        c.key(h("death") + 0.2 * i, P_(burn, {"head": [-50 + j[0], j[1], j[2]], "body": [-24 + j[2], j[0], 0]}))
    back = {"root": [86, 0, 0], "@root": [0, 2.8, 2.0], "head": [12, 14, 0], "right_arm": [-10, 0, 50], "left_arm": [-20, 0, -60],
            "right_leg": [-20, 0, 6], "right_shin": [30, 0, 0], "left_leg": [0, 0, -6]}
    c.key(3.6, P_(GRIP, HIDE, back), E_IN)
    c.key(L("death"), P_(GRIP, HIDE, back))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The bust
# =====================================================================================================

def bust():
    import blockmodel as BM
    skin = lambda x, y: tone(SKIN, 3.3 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 3.0 + 0.6 * math.sin(x * 1.7 + y * 0.9))
    lg = {"s": skin, "h": hair, "b": HAIR[2], "U": lambda x, y: shade(skin(x, y), 0.8), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.82), "w": EYE, "I": EYE_HOT, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "M": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.9), "g": lambda x, y: tone(HAIR, 2.6 + (x + y) % 2 * 0.6)}

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 0.6 and y < 6:
            return TIE[2]
        if d < 2.0 - 0.15 * y and y < 6:
            return tone(WHITE, 4.6)
        if d < 3.2 and y < 13:
            return GOLD[3] if (d < 0.6 and y % 3 == 1) else tone(WHITE, 3.0 - y / 16)
        return tone(WHITE, 3.6 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    coat = lambda x, y: tone(WHITE, 3.6 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4))
    hat = lambda x, y: TIE[2] if y >= 12 else tone(WHITE, 4.2 - y / 20 + 0.2 * math.sin(x))
    extra = [BM.cube("hat_crown", (5.6, 13.0, 5.6), (10.4, 15.2, 10.4), {"*": "hat"}),
             BM.cube("hat_brim", (3.6, 13.0, 3.6), (12.4, 13.4, 12.4), {"*": "hat"}),
             BM.cube("vial", (8.7, 4.6, 6.0), (9.5, 5.8, 6.2), {"*": "vial"})]
    K.bust("asmodeus", K.face_fn(FACE, lg, skin), hair, skin, chest, coat, hair_cap=False, extra=extra,
           extra_tex={"hat": hat, "vial": lambda x, y: GRACE_HOT if (x + y) % 3 == 0 else GRACE}, shoulders_w=8.4, head_w=4.6)


def build():
    m = rig()
    t, g = m.build(gutter=1, seed=49000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t, g)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("cane_slam", 0.7), ("grace_drink", 0.75), ("taunt", 0.5), ("death", 4.0)), scale=5):
    m, t, g, f, clips = build()
    return K.render(m, t, clips, out_dir, NAME, poses, scale=scale, views=("front", "three_quarter", "side"))


if __name__ == "__main__":
    generate()
