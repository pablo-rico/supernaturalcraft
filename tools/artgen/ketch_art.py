"""Arthur Ketch (v0.41, season 12), the British Men of Letters' operative: his rig, texture, clips (names and timings parsed
from KetchAssets) and his trophy bust (`block/ketch_trophy`). His bar is in s1214_gui_art.

A lean man in his forties (32 px to the crown): short dark hair, side-parted; clean-shaven, a narrow hard face, a half smile.
A sharp dark-grey three-piece suit -- the jacket open over a buttoned waistcoat, a white shirt and a slate tie -- black gloves,
black oxfords. An earpiece in his right ear, its wire to the collar (`earpiece`). A suppressed pistol in his right fist
(`pistol`, the barrel down along the hanging hand: level when he aims); a combat knife sheathed at his left hip (`knife`);
three hex grenades clipped to a strap at his right side under the jacket (`grenades`).

Extra bones the clips own: `knife_held` (left_hand: the knife drawn, scale 0 except in knife_slash / knife_lunge) and
`grenade_held` (left_hand: a canister, shown in grenade_throw / smoke_vanish until it leaves his hand). Every loop keys both to
scale 0 (the action controller's keys win while a clip plays). No clip keys pistol, knife, grenades or earpiece.
"""

import math

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import (Body, E_IN, E_IO, E_OUT, E_SNAP, arms, glove_m, hair_m, head, legs, neck, skeleton, skin_m, tone, torso)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

NAME = "ketch"
A = "animation.ketch."
C = K.Contract("entity/boss/ketch/KetchAssets")

B = Body(HIP=12.6, NECK=24.3, TW=3.75, TD=1.95, AW=3.2, SY=23.6, ELBOW=18.8, WRIST=14.2, PALM=12.3, LX=1.9, LW=3.5, KNEE=6.6,
         SHOE_H=1.3)

SKIN = Ramp("#4e3a33", "#7c5f54", "#a1806f", "#bb9884", "#cfae99", "#e2c7b4")
HAIR = Ramp("#0b0908", "#15110f", "#201a17", "#2c2420", "#3a302a", "#4c4038")
SUIT = Ramp("#15161a", "#1f2025", "#2a2c32", "#363840", "#43464f", "#555963")
SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
TIE = Ramp("#14181e", "#1e252e", "#28313d", "#33404e", "#405063", "#506479")
GLOVE = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3c3c45")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
GUN = Ramp("#0c0d0f", "#16181b", "#212428", "#2d3136", "#3c4147", "#5a6068")
STEEL = Ramp("#26282d", "#474b52", "#6e737b", "#959ba3", "#bcc1c8", "#e8ebef")
OLIVE = Ramp("#14160e", "#1f2416", "#2b321f", "#384129", "#465134", "#566340")
BRASS = Ramp("#3b2a10", "#5e4519", "#836226", "#a98236", "#cba354", "#e9c880")
LIPS = Ramp("#4a2e28", "#62403a", "#7a524a", "#8f6359", "#a27468", "#b48678")
EYE_WHITE, IRIS = hexc("#d2ccc4"), hexc("#3e4a36")
SIGIL = hexc("#7fd0c8")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssssssshhh",
    "hssssssssssssssh",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "sUUUUssssssUUUUs",
    "skwIIwsssswIIwks",
    "sskUussssssuUkss",
    "ssssssnnssssssss",
    "skssssnnssssssks",
    "sjsssNnnNsssssjs",
    "sjssssssssssssjs",
    "ssjslllllllLsjss",
    "sssjssssssssjsss",
    "ssssjjjjjjjjssss",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=44001):
    sk = skin_m(SKIN, seed, 3.0, 0.3)
    hr = hair_m(HAIR, seed + 1, base=2.6)

    def hair_p(f, x, y, w, h):
        c = hr(f, x, y, w, h)
        if f == "up" and x == 5:
            return HAIR[0]                          # the side parting
        return c
    lg = K.legend(sk, hair_p, EYE_WHITE, IRIS, LIPS, SKIN, brow=HAIR[1])
    sides = H.head_sides(sk, hair_p, top=3, back=11, temple=8, sideburn=6)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 0 else hair_p(f, x, y, w, h), shell_inflate=0.18, shell_rows=1.5)
    K.brows(m, B, HAIR[1], tilt=-2, w=2.4, h=0.45)
    neck(m, B, sk, 3.2)
    # The earpiece in his right ear and its coiled wire down into the collar.
    ep = m.bone("earpiece", (-B.HEAD / 2 - 0.2, B.NECK + 3.6, 0.4), "head")
    m.cube(ep, (-B.HEAD / 2 - 0.45, B.NECK + 3.1, 0.0), (0.5, 0.9, 0.8), solid(hexc("#c8c2b8")), density=4, tag="earpiece")
    m.cube(ep, (-B.HEAD / 2 - 0.3, B.NECK - 0.6, 0.9), (0.2, 3.8, 0.2), lambda f, x, y, w, h: hexc("#d8d4cc") if y % 2 else hexc("#a8a49c"),
           density=4, tag="earpiece_wire")


def build_suit(m, seed=44200):
    shirt = fabric(SHIRT, seed + 1, 3.6, 0.2)
    suit = fabric(SUIT, seed + 2, 3.0, 0.3)
    tie = lambda f, x, y, w, h: tone(TIE, 3.2 - 0.6 * (y % 3 == 0) + (0.6 if x % 2 else 0))

    def waistcoat(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 6 and abs(x - mid) < 2.6 - y * 0.35:
            return tie(f, x, y, w, h) if abs(x - mid) < 0.9 else shirt(f, x, y, w, h)
        if f == "north" and abs(x - mid) < 0.6 and y % 3 == 1:
            return hexc("#0c0c0e")                  # the waistcoat's buttons
        return tone(SUIT, 2.4 + 0.3 * math.sin(x * 0.7 + y * 0.4) - 0.4 * y / max(1, h))
    torso(m, B, waistcoat, tag="torso")
    # The jacket, open: only its fronts and the back; its lapels.
    from angel_art import open_front
    torso(m, B, open_front(suit, half=1.9, collar=0, lapel=SUIT[4]), inflate=0.3, bottom=B.HIP - 1.6, top=B.NECK - 0.2, tag="jacket")
    for s in (-1, 1):
        lp = m.bone("lapel_r" if s < 0 else "lapel_l", (s * 2.5, B.NECK - 0.6, -B.TD - 0.4), "body")
        m.cube(lp, (s * 2.5 - 0.8, B.NECK - 5.4, -B.TD - 0.6), (1.6, 5.0, 0.3), lambda f, x, y, w, h: tone(SUIT, 4.4 - y * 0.25),
               density=2, tag="lapel", rotation=(0, 0, -10 * s), pivot=(s * 2.5, B.NECK - 0.6, -B.TD - 0.4))
        m.cube("body", (s * 1.0 - 0.8, B.NECK - 1.3, -B.TD - 0.45), (1.6, 1.3, 0.4), lambda f, x, y, w, h: tone(SHIRT, 4.6 - y * 0.6),
               density=2, tag="shirt_collar", rotation=(10, 0, -24 * s), pivot=(s * 1.0, B.NECK - 0.1, -B.TD - 0.3))
    sleeve = fabric(SUIT, seed + 3, 2.9, 0.3)
    cuff = lambda f, x, y, w, h: tone(SHIRT, 4.2 - y * 0.5) if f not in ("up", "down") else SHIRT[1]
    arms(m, B, sleeve, None, cuff=cuff, cuff_depth=0.6, glove=glove_m(GLOVE, seed + 4))
    legs(m, B, fabric(SUIT, seed + 5, 2.9, 0.25, crease=True), SHOE)


def knife_parts(m, bone, x, y, z, held=False):
    """A black combat knife. Sheathed: handle up out of a sheath. Held (`held`): reversed in the fist, the blade along -Y."""
    grip = lambda f, xx, yy, w, h: tone(GUN, 2.6 + (0.8 if (yy % 2) else 0))

    def blade(f, xx, yy, w, h):
        if f in ("east", "west"):
            return STEEL[5] if xx == 0 else tone(STEEL, 3.8 - 0.4 * yy / max(1, h))
        return STEEL[2]
    if held:
        m.cube(bone, (x - 0.3, y - 0.6, z - 0.6), (0.6, 2.2, 1.2), grip, density=4, tag="knife_grip")
        m.cube(bone, (x - 0.45, y - 0.9, z - 0.8), (0.9, 0.3, 1.6), solid(GUN[4]), density=4, tag="knife_guard")
        m.cube(bone, (x - 0.12, y - 4.6, z - 0.5), (0.25, 3.7, 1.1), blade, density=4, tag="knife_blade")
        m.cube(bone, (x - 0.12, y - 5.2, z - 0.5), (0.25, 0.6, 0.6), solid(STEEL[4]), density=4, tag="knife_tip")
    else:
        m.cube(bone, (x - 0.5, y - 4.6, z - 0.9), (1.0, 4.6, 1.8), lambda f, xx, yy, w, h: tone(OLIVE, 2.6 - yy * 0.1), density=2,
               tag="sheath")
        m.cube(bone, (x - 0.35, y, z - 0.55), (0.7, 2.2, 1.1), grip, density=4, tag="knife_hilt")
        m.cube(bone, (x - 0.55, y - 0.15, z - 0.85), (1.1, 0.3, 1.7), solid(GUN[4]), density=4, tag="knife_guard")


def grenade(m, bone, x, y, z, tag="grenade"):
    """A hex grenade: a black canister, a band of pale sigils round its middle, a brass cap and lever."""
    def can(f, xx, yy, w, h):
        if f in ("north", "south", "east", "west") and yy in (h // 2 - 1, h // 2):
            return SIGIL if (xx + yy) % 2 else hexc("#2a4a48")
        return tone(GUN, 2.4 + (1.0 if f == "up" else 0) + (0.6 if xx == 0 else 0))
    m.cube(bone, (x - 0.55, y, z - 0.55), (1.1, 2.0, 1.1), can, density=4, tag=tag)
    m.cube(bone, (x - 0.35, y + 2.0, z - 0.35), (0.7, 0.4, 0.7), solid(BRASS[3]), density=4, tag=tag + "_cap")
    m.cube(bone, (x + 0.35, y + 0.6, z - 0.2), (0.25, 1.8, 0.4), solid(BRASS[2]), density=4, tag=tag + "_lever")


def build_kit(m):
    # The pistol in his right fist: the barrel down along the hand (level when the arm comes up to aim), the grip in the fist.
    cx, y = -B.AX, B.PALM
    p = m.bone("pistol", (cx, y, 0), "right_hand")
    body = lambda f, xx, yy, w, h: tone(GUN, 2.8 + (1.0 if f in ("north",) else 0) - 0.3 * yy / max(1, h) + (0.5 if xx == 0 else 0))
    m.cube(p, (cx - 0.45, y - 3.6, -1.6), (0.9, 3.8, 1.3), body, density=4, tag="slide")
    m.cube(p, (cx - 0.4, y - 0.4, -0.5), (0.8, 1.0, 2.0), body, density=4, tag="pistol_grip")
    m.cube(p, (cx - 0.3, y - 2.0, -0.4), (0.6, 0.3, 0.9), solid(GUN[1]), density=4, tag="trigger_guard")
    m.cube(p, (cx - 0.35, y - 7.6, -1.45), (0.7, 4.0, 0.9), lambda f, xx, yy, w, h: tone(GUN, 2.2 + (0.8 if xx == 0 else 0)
                                                                                          + (0.5 if yy % 5 == 0 else 0)),
           density=4, tag="suppressor")
    m.cube(p, (cx - 0.1, y - 0.6, -1.75), (0.2, 0.4, 0.2), solid(STEEL[3]), density=4, tag="sight")
    # The knife sheathed at his left hip; the same knife drawn, reversed in the left fist.
    kn = m.bone("knife", (B.TW + 0.7, B.HIP + 0.6, 0.4), "body")
    knife_parts(m, kn, B.TW + 0.75, B.HIP + 0.6, 0.6)
    kh = m.bone("knife_held", (B.AX, B.PALM, -0.2), "left_hand")
    knife_parts(m, kh, B.AX, B.PALM + 0.4, -0.2, held=True)
    # Three grenades on a strap at his right side, under the jacket's edge.
    gr = m.bone("grenades", (-B.TW + 0.6, B.HIP + 1.5, -B.TD), "body")
    m.cube(gr, (-B.TW - 0.1, B.HIP + 2.6, -B.TD - 0.3), (3.0, 0.4, 0.3), solid(OLIVE[1]), density=4, tag="strap")
    for i in range(3):
        grenade(m, gr, -B.TW + 0.5 + i * 1.1, B.HIP + 0.4, -B.TD - 0.6, tag=f"grenade_{i}")
    gh = m.bone("grenade_held", (B.AX, B.PALM, -0.4), "left_hand")
    grenade(m, gh, B.AX, B.PALM - 1.2, -0.6, tag="grenade_in_hand")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=False)
    build_head(m)
    build_suit(m)
    build_kit(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

GRIP = K.fist("right")
HIDE = {"%knife_held": [0, 0, 0], "%grenade_held": [0, 0, 0]}
KNIFE = {"%knife_held": [1, 1, 1], "%grenade_held": [0, 0, 0]}
NADE = {"%knife_held": [0, 0, 0], "%grenade_held": [1, 1, 1]}
# Weight back, the pistol hanging by his right thigh, the left hand loose.
IDLE = P_(GRIP, K.relax("left"), {"right_arm": [-4, 0, 3], "right_forearm": [-10, 0, 0], "left_arm": [-2, 0, -3], "left_forearm": [-14, 0, 0],
                                  "body": [-1, 0, 0], "head": [-2, 0, 0], "right_leg": [0, 0, 2], "left_leg": [0, 0, -1]})
# Two hands on the pistol, arms out, level.
AIM = P_(GRIP, K.fist("left", 0.8), {"right_arm": [-88, 8, 0], "right_forearm": [-4, 0, 0], "left_arm": [-80, 36, 0],
                                     "left_forearm": [-18, -10, 0], "body": [2, -14, 0], "head": [-2, 12, 0],
                                     "right_leg": [-8, 0, 2], "left_leg": [10, 0, -2], "@root": [0, -0.2, 0]})


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, P_(IDLE, HIDE), 4.0, head_turn=8, extra=lambda u, b: {"right_hand": [0, 0, 4 * math.sin(u * 2 * math.pi)]})

    c = clip("walk", 1.2, loop=True)
    K.walk(c, P_(IDLE, HIDE), 1.2, stride=18, arm=9, bob=0.35, twist=4, knee=(6, 16), extra=lambda s: {"head": [-2, -3 * s, 0]})

    c = clip("run", 0.6, loop=True)
    run = P_(GRIP, HIDE, {"right_arm": [-20, 0, 6], "right_forearm": [-100, 0, 0], "right_hand": [40, 0, 0], "body": [14, 0, 0],
                          "head": [-12, 0, 0], "left_forearm": [-70, 0, 0]})
    K.walk(c, run, 0.6, stride=40, arm=34, bob=0.8, twist=8, knee=(14, 70), lean=0,
           extra=lambda s: {"right_arm": [-20 - 14 * s, 0, 6], "left_arm": [28 * s, 0, -4]})

    c = clip("aim", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        b = math.sin(t / 2.0 * 2 * math.pi)
        c.key(t, P_(AIM, HIDE, {"right_arm": [-88 + 1.2 * b, 8 + 1.5 * b, 0], "left_arm": [-80 + 1.2 * b, 36 + 1.5 * b, 0],
                                "body": [2, -14 + 1.5 * b, 0], "@body": [0, -0.08 * abs(b), 0]}), E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Shoot (3): one suppressed shot -- the kick throws the pistol up, he brings it back level.
    c = clip("shoot")
    kick = P_(AIM, {"right_arm": [-104, 8, 0], "left_arm": [-94, 36, 0], "right_hand": [-14, 0, 0], "head": [-4, 12, 0], "@root": [0, 0, 0.3]})
    c.key(0, AIM)
    c.key(0.1, P_(AIM, {"right_arm": [-86, 8, 0]}), E_IO)
    c.key(h("shoot"), kick, E_SNAP)
    c.key(L("shoot"), AIM, E_OUT)

    # Volley (4): three quick shots, sidestepping to his left between them.
    c = clip("volley")
    c.key(0, AIM)
    for i, t in enumerate((h("volley") / 20 * 20, 0.75, 1.15)):
        side = {"@root": [0.9 * i, 0, 0], "right_leg": [-8, 0, 6 if i % 2 else 0], "left_leg": [10, 0, -8 if i % 2 == 0 else 0]}
        c.key(max(0.01, t - 0.08), P_(AIM, side), E_IO)
        c.key(t, P_(kick, side), E_SNAP)
        c.key(t + 0.12, P_(AIM, side), E_OUT)
    c.key(L("volley"), P_(AIM, {"@root": [1.8, 0, 0]}), E_IO)

    # Knife slash (6): the knife drawn reversed in his left fist, a fast backhand cut across.
    c = clip("knife_slash")
    cock = P_(IDLE, KNIFE, K.fist("left"), {"left_arm": [-60, -40, -60], "left_forearm": [-80, -30, 0], "body": [0, 26, 0], "head": [-4, -16, 0],
                                             "right_arm": [-20, 0, 10]})
    cut = P_(IDLE, KNIFE, K.fist("left"), {"left_arm": [-80, 30, 50], "left_forearm": [-6, 0, 0], "left_hand": [0, 0, 20], "body": [4, -30, 0],
                                            "head": [-6, 20, 0], "@root": [0, 0, -0.8], "right_arm": [-10, 0, 20],
                                            "right_leg": [-14, 0, 0], "left_leg": [10, 0, 0]})
    c.key(0, P_(IDLE, KNIFE))
    c.key(0.2, cock, E_IO)
    c.key(h("knife_slash"), cut, E_SNAP)
    c.key(0.45, P_(cut, {"left_arm": [-74, 36, 56]}), E_OUT)
    c.key(L("knife_slash"), P_(IDLE, KNIFE), E_IO)

    # Knife lunge (8): a long step and a thrust low and forward.
    c = clip("knife_lunge")
    coil = P_(IDLE, KNIFE, K.fist("left"), {"left_arm": [20, 0, -10], "left_forearm": [-90, 0, 0], "body": [10, 20, 0], "@root": [0, -0.6, 1.0],
                                             "right_leg": [6, 0, 0], "left_leg": [-10, 0, 0]})
    lunge = P_(IDLE, KNIFE, K.fist("left"), {"left_arm": [-84, 0, -6], "left_forearm": [0, 0, 0], "left_hand": [80, 0, 0], "body": [24, -16, 0],
                                              "head": [-20, 10, 0], "left_leg": [-50, 0, 0], "left_shin": [50, 0, 0], "right_leg": [30, 0, 0],
                                              "right_shin": [10, 0, 0], "@root": [0, -2.0, -4.0], "right_arm": [20, 0, 20]})
    c.key(0, P_(IDLE, KNIFE))
    c.key(0.28, coil, E_IO)
    c.key(h("knife_lunge"), lunge, E_SNAP)
    c.key(0.6, P_(lunge, {"@root": [0, -2.0, -4.2]}), E_OUT)
    c.key(L("knife_lunge"), P_(IDLE, KNIFE), E_IO)

    # Grenade throw (12): an overarm throw with the left hand; the canister leaves it at the hit.
    c = clip("grenade_throw")
    wind = P_(IDLE, NADE, K.fist("left"), {"left_arm": [-170, 0, -30], "left_forearm": [-60, 0, 0], "body": [-10, -20, 0], "head": [-6, 14, 0],
                                            "right_arm": [-30, 0, 20], "@root": [0, 0, 0.8], "left_leg": [10, 0, 0], "right_leg": [-6, 0, 0]})
    rel = P_(IDLE, K.spread("left"), {"left_arm": [-80, 10, 0], "left_forearm": [-6, 0, 0], "body": [16, 20, 0], "head": [-6, -12, 0],
                                      "right_arm": [10, 0, 24], "@root": [0, -0.4, -1.2], "left_leg": [-20, 0, 0], "right_leg": [14, 0, 0]})
    c.key(0, P_(IDLE, NADE))
    c.key(0.45, wind, E_IO)
    c.key(h("grenade_throw") - 0.05, P_(wind, {"left_arm": [-130, 0, -20], "body": [0, 0, 0]}), E_IN)
    c.key(h("grenade_throw"), P_(rel, {"%grenade_held": [0, 0, 0], "%knife_held": [0, 0, 0]}), E_SNAP)
    c.key(0.8, P_(rel, HIDE, {"left_arm": [-70, 10, 0]}), E_OUT)
    c.key(L("grenade_throw"), P_(IDLE, HIDE), E_IO)

    # Smoke vanish (8): a quick crouch, a canister dropped at his feet.
    c = clip("smoke_vanish")
    crouch = P_(IDLE, NADE, K.spread("left"), {"@root": [0, -4.0, 0], "right_leg": [-70, 0, 4], "left_leg": [-30, 0, -4], "right_shin": [90, 0, 0],
                                                "left_shin": [70, 0, 0], "body": [30, 0, 0], "head": [-24, 0, 0], "left_arm": [-30, 0, -10],
                                                "left_forearm": [-10, 0, 0], "right_arm": [-40, 0, 20], "right_forearm": [-60, 0, 0]})
    c.key(0, P_(IDLE, NADE))
    c.key(0.3, crouch, E_IO)
    c.key(h("smoke_vanish"), P_(crouch, HIDE, {"left_arm": [-20, 0, -14]}), E_SNAP)
    c.key(0.7, P_(crouch, HIDE, {"@root": [0, -4.4, 0]}), E_OUT)
    c.key(L("smoke_vanish"), P_(crouch, HIDE, {"@root": [0, -4.4, 0]}))

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, P_(IDLE, HIDE), L("hurt"), h("hurt"), {"body": [-12, 10, 0], "head": [-18, 14, 0], "@root": [0, 0, 1.0],
                                                     "left_arm": [-40, 0, -30], "right_arm": [-20, 0, 24]})

    # Taunt (10): he adjusts his cuffs -- the left hand tugs the right sleeve, a small, polite smile.
    c = clip("taunt")
    cuffs = P_(IDLE, HIDE, K.fist("left", 0.6), {"right_arm": [-36, 0, -10], "right_forearm": [-70, -30, 0], "right_hand": [0, -40, 0],
                                                 "left_arm": [-40, 0, 10], "left_forearm": [-80, 60, 0], "head": [10, 8, 0], "body": [-2, 0, 0]})
    c.key(0, P_(IDLE, HIDE))
    c.key(0.35, cuffs, E_IO)
    c.key(h("taunt"), P_(cuffs, {"left_forearm": [-84, 66, 0], "@body": [0, 0.2, 0]}), E_SNAP)
    c.key(0.8, P_(cuffs, {"head": [-4, 0, 6]}), E_IO)
    c.key(L("taunt"), P_(IDLE, HIDE), E_IO)

    # Death (40): shot through, he drops to his knees, sways, and falls forward.
    c = clip("death", hold=True)
    hit_ = P_(IDLE, HIDE, {"body": [-16, 0, 0], "head": [-20, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-60, 0, -10],
                           "left_forearm": [-80, 40, 0], "@root": [0, 0, 1.0]})
    c.key(0, P_(IDLE, HIDE))
    c.key(0.2, hit_, E_SNAP)
    knees = P_(IDLE, HIDE, {"@root": [0, -5.8, 0.6], "right_leg": [-90, 0, 4], "left_leg": [-90, 0, -4], "right_shin": [90, 0, 0],
                            "left_shin": [90, 0, 0], "body": [12, 0, 0], "head": [18, 0, 0], "right_arm": [-10, 0, 10],
                            "left_arm": [-20, 0, -10], "left_forearm": [-40, 30, 0]})
    c.key(0.8, knees, E_IN)
    for i in range(1, 5):
        j = K.jit("ketch-death", i, 3)
        c.key(0.8 + 0.25 * i, P_(knees, {"body": [12 + 2 * i + j[0], j[1], j[2]], "head": [18 + j[1], j[0] * 2, 0]}))
    c.key(h("death"), P_(knees, {"body": [30, 0, 0], "head": [26, 0, 0]}), E_IO)
    c.key(2.6, K.lie(face_down=True, lift=2.0, extra={"right_leg": [0, 0, 4], "left_leg": [0, 0, -4], "%knife_held": [0, 0, 0],
                                                       "%grenade_held": [0, 0, 0]}), E_IN)
    c.key(L("death"), K.lie(face_down=True, lift=2.0, extra={"%knife_held": [0, 0, 0], "%grenade_held": [0, 0, 0]}))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The bust
# =====================================================================================================

def bust():
    skin = lambda x, y: tone(SKIN, 3.3 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 2.6 + 0.5 * math.sin(x * 1.7 + y * 0.9))
    lg = {"s": skin, "h": hair, "b": HAIR[1], "U": lambda x, y: shade(skin(x, y), 0.8), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.82), "w": EYE_WHITE, "I": IRIS, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "M": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.9)}

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 0.9 and y < 12:
            return tone(TIE, 3.2 + (0.5 if x % 2 else 0))
        if d < 2.0 - 0.1 * y and y < 7:
            return tone(SHIRT, 4.2)
        if d < 3.0 - 0.1 * y and y < 11:
            return tone(SUIT, 2.4)
        if d < 4.2 - 0.15 * y and y < 11:
            return SUIT[4]
        return tone(SUIT, 3.0 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    coat = lambda x, y: tone(SUIT, 3.0 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4))
    K.bust("ketch", K.face_fn(FACE, lg, skin), hair, skin, chest, coat, shoulders_w=7.6, head_w=4.2)


def build():
    m = rig()
    t, g = m.build(gutter=1, seed=44000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("aim", 0.0), ("knife_slash", 0.3), ("grenade_throw", 0.5), ("death", 3.0)), scale=5):
    m, t, g, f, clips = build()
    return K.render(m, t, clips, out_dir, NAME, poses, scale=scale)


if __name__ == "__main__":
    generate()
