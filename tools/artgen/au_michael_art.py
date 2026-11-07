"""The Michael of the Apocalypse World (v0.41, season 13): rig, texture + glowmask, clips (names and timings parsed from
AuMichaelAssets) and his trophy bust (`block/au_michael_trophy`). His bar is in s1214_gui_art.

The general who won his war (34 px to the crown, broad): short grey hair, a hard square face, deep lines, a grey stubble.
A long dark military greatcoat, open, to mid-calf, double-breasted lapels, silver shoulder boards and collar insignia, over
black armour plates (a breastplate, vambraces) and black trousers tucked into black boots; black gloves. An archangel blade
in his right fist (`blade`, silver, a faint blue light along its edge). Blue-white eyes when he shows them (`eyes`, lit).

Two pairs of vast dark-gold wings of light at his back (`wings`, lit): the upper pair `wing_r` / `wing_l` (full feathering),
the lower pair `wing2_r` / `wing2_l` (lighter: flight feathers and greater coverts), built by s1214_wings. The renderer shows
`wings` in his later phases; the clips pose the pairs (folded in the loops that don't spread them).

The coat's back panel hangs from `coat_tail` through its child `coat_back`. No clip keys wings, blade, coat_tail or eyes.
"""

import math

import horsemen_art as H
import s1214_kit as K
import s1214_wings as W
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, coat, eye_shell, glove_m, hair_m, head, legs, neck, skeleton,
                         skin_m, tone, torso, wool_m)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

NAME = "au_michael"
A = "animation.au_michael."
C = K.Contract("entity/boss/aumichael/AuMichaelAssets")

B = Body(HIP=13.4, NECK=25.8, TW=4.5, TD=2.35, AW=3.8, SY=25.0, ELBOW=19.8, WRIST=15.0, PALM=13.0, LX=2.2, LW=4.0, KNEE=7.0,
         SHOE_H=1.5, HEM=3.6)

SKIN = Ramp("#4e3a32", "#7a5c50", "#9e7c6c", "#b69482", "#c8a896", "#dabdac")
HAIR = Ramp("#4a4a4c", "#626264", "#7c7c7e", "#969698", "#b0b0b2", "#cacacc")
COAT = Ramp("#0a0c0e", "#121518", "#1a1e22", "#23282d", "#2e343a", "#3c434b")
ARMOUR = Ramp("#050506", "#0b0b0d", "#131316", "#1c1c20", "#28282e", "#3a3a44")
SILVER = Ramp("#4a4f58", "#6e7580", "#959ca6", "#bcc2ca", "#dde1e6", "#ffffff")
GLOVE = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3c3c45")
BOOT = Ramp("#030303", "#08080a", "#0f0f12", "#18181c", "#25252b", "#4a4a55")
LIPS = Ramp("#3e2622", "#56342e", "#6c443c", "#80524a", "#926058", "#a47066")
GRIP = Ramp("#14161c", "#1d2029", "#272b37", "#333847", "#414759", "#535a6e")
EYE_HOT, EYE, EYE_DIM = hexc("#f4fbff"), hexc("#a8dcff"), hexc("#3a6a9a")
WING = dict(ramp=Ramp("#2a1c06", "#4a320c", "#7a5414", "#a8781e", "#d0a032", "#f0d070"), root=hexc("#3a2608"), rim=hexc("#fff2b0"),
            alpha=235, fade=40, glow=(0.18, 0.5), wisp=True)
WING2 = dict(WING, alpha=215, fade=70)

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhsssssssssssshh",
    "ssssjjjjjjjjssss",
    "ssssssssssssssss",
    "sbbbbbbssbbbbbbs",
    "sUUUUssssssUUUUs",
    "kkwIIwsssswIIwkk",
    "skkUussssssuUkks",
    "skssssnnssssssks",
    "jdssssnnssssssdj",
    "jdsssNnnNsssssdj",
    "jddssssssssssddj",
    "jddslllllllLsddj",
    "jdddssssssssdddj",
    "sjdddddddddddddj",
]
FACE = [r[:16] for r in FACE]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=50001):
    sk = skin_m(SKIN, seed, 3.0, 0.35)
    hr = hair_m(HAIR, seed + 1, base=2.8)
    crop = lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.12) if (x + y) % 2 == 0 else hr(f, x, y, w, h)
    stub = lambda f, x, y, w, h: mix(sk(f, x, y, w, h), HAIR[2], 0.4 if h01("st", seed, f, x, y) < 0.6 else 0.15)
    lg = K.legend(sk, crop, hexc("#d0c8c0"), hexc("#3a5068"), LIPS, SKIN, brow=HAIR[1], extra={"d": stub})
    sides = H.head_sides(sk, crop, top=2, back=10, temple=9, sideburn=4, beard=stub, beard_from=11)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" else crop(f, x, y, w, h), shell_inflate=0.12, shell_rows=1.0)
    K.brows(m, B, HAIR[1], tilt=-8, w=2.6, h=0.55)
    eye_shell(m, B, "dEHEd..dEHEd", {"d": EYE_DIM, "E": EYE, "H": EYE_HOT})
    neck(m, B, sk, 4.0)


def plate_m(seed):
    def m(f, x, y, w, h):
        v = 2.6 + 0.5 * math.sin(x * 0.5 + y * 0.2) - 0.6 * y / max(1, h) + (1.0 if f == "up" else 0)
        if f in ("north", "south") and (y % 7 == 0):
            v += 1.2                                    # the plates' upper edges
        if f == "north" and abs(x - (w - 1) / 2) < 0.6:
            v += 0.8                                    # a ridge down the breastplate
        c = tone(ARMOUR, v)
        if h01("scr", seed, f, x, y) < 0.03:
            c = mix(c, SILVER[2], 0.4)
        return c
    return m


def build_body(m, seed=50200):
    torso(m, B, plate_m(seed + 1), tag="breastplate")
    cl = wool_m(COAT, seed + 2, 2.9)
    coat(m, B, COAT, cl, seed + 2, lapel=True, collar_up=True, pockets=False)
    K.move_cubes(m, "coat_tail", "coat_back")
    # Silver shoulder boards and collar insignia; a row of silver buttons down each front.
    for s in (-1, 1):
        m.cube("coat", (s * (B.TW + 0.3) - 1.5, B.NECK - 0.45, -B.TD - 0.6), (3.0, 0.4, 2 * B.TD + 1.2),
               lambda f, x, y, w, h: SILVER[4] if (x + y) % 3 else SILVER[2], density=2, tag="shoulder_board")
        m.cube("collar", (s * 3.4 - 0.4, B.NECK + 0.6, -2.2), (0.8, 0.8, 0.2), lambda f, x, y, w, h: SILVER[5] if (x + y) % 2 else SILVER[3],
               density=4, tag="insignia")
    for bone, s in (("coat_r", -1), ("coat_l", 1)):
        for i in range(3):
            m.cube(bone, (s * 2.2 - 0.3, B.HIP - 1.0 - i * 2.4, -B.TD - 1.05), (0.6, 0.6, 0.3), solid(SILVER[4]), density=4, tag="button")
    sleeve = wool_m(COAT, seed + 3, 2.8)
    cuff = lambda f, x, y, w, h: SILVER[3] if (f not in ("up", "down") and y == 0) else tone(COAT, 3.4)
    arms(m, B, sleeve, None, cuff=cuff, cuff_depth=1.2, glove=glove_m(GLOVE, seed + 4))
    legs(m, B, fabric(ARMOUR, seed + 5, 2.8, 0.25), BOOT, boot_h=5.0, toe=1.0)


def build_blade(m):
    """The archangel blade in his right fist, point forward: a black grip bound in silver, a long three-edged silver blade with
    a cold blue light down its edges (lit)."""
    cx = -B.AX
    y = B.PALM - 0.3
    b = m.bone("blade", (cx, y, 0), "right_hand", rotation=(32, 0, 0))
    m.cube(b, (cx - 0.55, y - 0.55, -0.6), (1.1, 1.1, 3.6), lambda f, xx, yy, w, h: GRIP[2 + ((xx + yy) % 3 == 0) * 2], density=2, tag="grip")
    m.cube(b, (cx - 0.65, y - 0.65, 3.0), (1.3, 1.3, 0.6), solid(SILVER[3]), density=2, tag="pommel")
    m.cube(b, (cx - 0.9, y - 0.9, -1.2), (1.8, 1.8, 0.6), solid(SILVER[4]), density=2, tag="guard")

    def steel(f, xx, yy, w, h):
        if f in ("north", "south", "up", "down"):
            return SILVER[5]
        if yy in (0, h - 1):
            return EYE
        return SILVER[4] if f == "west" else SILVER[2]
    lit = lambda f, xx, yy, w, h, c: c if c == EYE else shade(c, 0.22)
    m.cube(b, (cx - 0.55, y - 0.55, -12.4), (1.1, 1.1, 11.2), steel, lit, density=2, tag="blade_steel", rotation=(0, 0, 45), pivot=(cx, y, 0))
    m.cube(b, (cx - 0.3, y - 0.3, -13.6), (0.6, 0.6, 1.2), solid(SILVER[5]), lit, density=2, tag="tip", rotation=(0, 0, 45), pivot=(cx, y, 0))


def rig():
    m = Model(NAME, 512, 512)
    skeleton(m, B, coat=True)
    build_head(m)
    build_body(m)
    build_blade(m)
    wb = m.bone("wings", (0, B.NECK - 4.0, B.TD + 0.6), "body")
    W.build(m, wb.name, "wing", WING, off=(0.0, B.NECK - 3.6, B.TD - 1.0), k=1.25, seed=50500)
    W.build(m, wb.name, "wing2", WING2, off=(0.0, B.NECK - 9.0, B.TD - 1.2), k=0.95, kinds=("primary", "secondary", "greater"), seed=50700)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

GRIP_R = K.fist("right")
BEHIND = {"right_arm": [26, 0, 10], "left_arm": [26, 0, -10], "right_forearm": [-30, -40, 0], "left_forearm": [-30, 40, 0],
          "right_hand": [0, 0, -20], "left_hand": [0, 0, 20], "right_fingers": [60, 0, 0], "left_fingers": [40, 0, 0],
          "body": [-3, 0, 0], "head": [-4, 0, 0]}
READY = P_(GRIP_R, K.relax("left"), {"right_arm": [-12, 0, 6], "right_forearm": [-20, 0, 0], "left_arm": [-4, 0, -6], "left_forearm": [-12, 0, 0],
                                     "head": [-3, 0, 0]})
HOVER_Y = 7.0


def anims():
    f = AnimFile()
    clips = []
    names = None

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    nm = {b.name for b in rig().rig.bones}

    def wp(upper, lower=None):
        """Both pairs: `upper` for wing_*, `lower` (default a lower, tucked variant) for wing2_*."""
        lower = lower or dict(upper, arm=tuple(a + b for a, b in zip(upper.get("arm", (0, 0, 0)), (0, 4, -28))))
        return P_(W.pose("wing", nm, **upper), W.pose("wing2", nm, **lower))

    FOLDED = wp(W.FOLD, dict(W.FOLD, arm=(16, 6, 60)))
    OPEN = wp(W.OPEN)
    WIDE = wp(W.WIDE)
    HIGH = wp(W.HIGH)
    DOWN = wp(W.FLAP_DOWN)
    WRAPPED = wp(W.WRAP, dict(W.WRAP, arm=(0, -40, 0)))
    BEAT = wp(W.BEAT)
    h, L = C.hit, C.length
    cs = lambda u, a=1.0: {"coat_back": [3 * a * math.sin(u * 6.28), 0, 1.2 * a * math.sin(u * 3.14)],
                           "coat_r": [-2 * a * math.sin(u * 6.28 + 0.6), 0, 0], "coat_l": [-2 * a * math.sin(u * 6.28 + 1.2), 0, 0]}
    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, P_(FOLDED, BEHIND), 4.0, head_turn=4, sway=lambda u: cs(u, 0.4))
    c = clip("walk", 1.3, loop=True)
    K.walk(c, P_(FOLDED, BEHIND), 1.3, stride=18, arm=0, bob=0.3, twist=3, knee=(6, 14), arms=False,
           extra=lambda s: {"coat_r": [-9 * s, 0, 0], "coat_l": [9 * s, 0, 0], "coat_back": [8, 0, 2 * s]})
    c = clip("hover_idle", 2.0, loop=True)
    hang = {"right_leg": [10, 0, 4], "left_leg": [4, 0, -4], "right_shin": [16, 0, 0], "left_shin": [22, 0, 0]}
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        up = i % 2 == 0
        c.key(t, P_(HIGH if up else DOWN, READY, hang, cs(t / 2.0, 1.4), {"@root": [0, HOVER_Y + (0.0 if up else 0.9), 0], "body": [4, 0, 0],
                                                                          "head": [-6, 0, 0]}), E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    up_cut = P_(GRIP_R, {"right_arm": [-160, -20, 30], "right_forearm": [-40, 0, 0], "body": [-6, 24, 0], "head": [-6, -14, 0],
                         "left_arm": [-30, 0, -20], "left_forearm": [-40, 0, 0], "@root": [0, 0.2, 0.6]})
    down_cut = P_(GRIP_R, {"right_arm": [-40, 20, -40], "right_forearm": [-20, 0, 0], "body": [18, -24, 0], "head": [4, 14, 0],
                           "left_arm": [-20, 0, -30], "left_forearm": [-30, 0, 0], "right_leg": [-24, 0, 0], "left_leg": [14, 0, 0],
                           "right_shin": [20, 0, 0], "@root": [0, -1.0, -1.8], "coat_back": [20, 0, 0]})
    # Blade slash (7): one heavy cut, high right to low left.
    c = clip("blade_slash")
    c.key(0, P_(FOLDED, READY))
    c.key(0.25, P_(OPEN, up_cut), E_OUT)
    c.key(h("blade_slash"), P_(WIDE, down_cut), E_SNAP)
    c.key(0.5, P_(OPEN, down_cut, {"right_arm": [-36, 22, -44]}), E_OUT)
    c.key(L("blade_slash"), P_(FOLDED, READY), E_IO)

    # Blade combo (18): three cuts -- across, back, and down (the third is the moment).
    c = clip("blade_combo")
    across = P_(GRIP_R, {"right_arm": [-80, -30, -50], "right_forearm": [-10, 0, 0], "body": [6, 26, 0], "head": [-4, -14, 0], "@root": [0, -0.3, -0.8]})
    back = P_(GRIP_R, {"right_arm": [-90, 30, 50], "right_forearm": [-10, 0, 0], "body": [6, -26, 0], "head": [-4, 14, 0], "@root": [0, -0.3, -1.4]})
    c.key(0, P_(FOLDED, READY))
    c.key(0.2, P_(OPEN, back), E_IO)
    c.key(0.35, P_(OPEN, across), E_SNAP)
    c.key(0.55, P_(OPEN, across, {"right_arm": [-100, -20, -30]}), E_OUT)
    c.key(0.65, P_(OPEN, back), E_SNAP)
    c.key(0.75, P_(OPEN, up_cut), E_IO)
    c.key(h("blade_combo"), P_(WIDE, down_cut), E_SNAP)
    c.key(1.2, P_(OPEN, down_cut, {"right_arm": [-34, 22, -46]}), E_OUT)
    c.key(L("blade_combo"), P_(FOLDED, READY), E_IO)

    # Smite (12): a raised palm -- holy fire.
    c = clip("smite")
    draw = P_(READY, K.spread("left"), {"left_arm": [-40, 0, 10], "left_forearm": [-110, 0, 0], "body": [-4, -20, 0], "head": [-6, 10, 0]})
    push = P_(READY, K.spread("left"), {"left_arm": [-110, 0, -4], "left_forearm": [-10, 0, 0], "left_hand": [-70, 0, 0], "body": [6, 16, 0],
                                       "head": [-10, -12, 0], "@root": [0, 0, -1.0]})
    c.key(0, P_(FOLDED, READY))
    c.key(0.4, P_(OPEN, draw), E_IO)
    c.key(h("smite"), P_(WIDE, push), E_SNAP)
    c.key(0.9, P_(WIDE, push, {"left_hand": [-74, 0, 0]}), E_OUT)
    c.key(L("smite"), P_(FOLDED, READY), E_IO)

    # Wing beat (10): the wings swept high, then beaten forward -- the gale.
    c = clip("wing_beat")
    c.key(0, P_(OPEN, READY))
    c.key(0.35, P_(wp(W.FLAP_UP), READY, {"body": [-8, 0, 0], "head": [-8, 0, 0], "@root": [0, 0.6, 0.6]}), E_IO)
    c.key(h("wing_beat"), P_(BEAT, READY, {"body": [14, 0, 0], "head": [-6, 0, 0], "@root": [0, -0.6, -0.8], "coat_back": [30, 0, 0]}), E_SNAP)
    c.key(0.7, P_(BEAT, READY, {"body": [10, 0, 0]}), E_OUT)
    c.key(L("wing_beat"), P_(OPEN, READY), E_IO)

    # Command (14): a pointing order -- his soldiers come.
    c = clip("command")
    point = P_(READY, {"left_arm": [-96, 0, 0], "left_forearm": [-4, 0, 0], "left_fingers": [80, 0, 0], "left_middle": [75, 0, 0],
                       "left_index": [-6, 0, 0], "left_thumb": [-20, 0, -20], "head": [-6, 0, 0], "body": [2, -8, 0]})
    c.key(0, P_(FOLDED, READY))
    c.key(0.45, P_(FOLDED, point, {"left_arm": [-120, 0, -10], "left_forearm": [-40, 0, 0]}), E_IO)
    c.key(h("command"), P_(OPEN, point), E_SNAP)
    c.key(1.0, P_(OPEN, point), E_OUT)
    c.key(L("command"), P_(FOLDED, READY), E_IO)

    # Lightning (22): both arms raised -- the burned sky answers.
    c = clip("lightning")
    gather = P_(GRIP_R, K.fist("left"), {"right_arm": [-30, 0, -20], "left_arm": [-30, 0, 20], "right_forearm": [-90, 0, 0],
                                         "left_forearm": [-90, 0, 0], "body": [14, 0, 0], "head": [14, 0, 0], "@root": [0, -0.8, 0]})
    raised = P_(GRIP_R, K.spread("left"), {"right_arm": [-170, 0, -20], "left_arm": [-170, 0, 20], "right_forearm": [-6, 0, 0],
                                           "left_forearm": [-6, 0, 0], "head": [-30, 0, 0], "body": [-8, 0, 0], "@root": [0, 0.6, 0]})
    c.key(0, P_(FOLDED, READY))
    c.key(0.6, P_(FOLDED, gather), E_IO)
    c.key(h("lightning"), P_(HIGH, raised), E_SNAP)
    for i in range(1, 4):
        j = K.jit("aum-light", i, 2)
        c.key(h("lightning") + 0.18 * i, P_(HIGH, raised, {"head": [-30 + j[0], j[1], 0], "body": [-8 + j[2], 0, 0]}))
    c.key(1.7, P_(OPEN, raised), E_IO)
    c.key(L("lightning"), P_(FOLDED, READY), E_IO)

    # Rift flee (40): he turns away, the wings wrap round him, and he steps into the rift.
    c = clip("rift_flee", hold=True)
    c.key(0, P_(OPEN, READY))
    c.key(0.8, P_(OPEN, READY, {"body": [0, 50, 0], "head": [0, 40, 0]}), E_IO)
    c.key(h("rift_flee"), P_(WRAPPED, READY, {"root": [0, 160, 0], "body": [6, 0, 0], "head": [-6, 0, 0], "right_arm": [-30, 0, -20],
                                              "left_arm": [-30, 0, 20]}), E_IO)
    c.key(3.0, P_(WRAPPED, READY, {"root": [0, 180, 0], "@root": [0, 0, 4.0], "right_leg": [-16, 0, 0], "left_leg": [12, 0, 0]}), E_IO)
    c.key(L("rift_flee"), P_(WRAPPED, READY, {"root": [0, 180, 0], "@root": [0, 0, 10.0]}), E_IN)

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, P_(FOLDED, READY), L("hurt"), h("hurt"), {"body": [-12, 10, 0], "head": [-18, 14, 0], "@root": [0, 0, 1.0], "left_arm": [-30, 0, -30],
                                                         "coat_back": [14, 0, 0]})

    # Death (40): he never dies here -- down on one knee, head bowed, the blade's point to the ground.
    c = clip("death", hold=True)
    kneel = P_(FOLDED, GRIP_R, {"@root": [0, -4.0, 0], "right_leg": [-90, 0, 4], "right_shin": [90, 0, 0], "left_leg": [-10, 0, -4],
                                "left_shin": [80, 0, 0], "body": [16, 0, 0], "head": [26, 0, 0], "right_arm": [-40, 0, 10],
                                "right_forearm": [-10, 0, 0], "right_hand": [40, 0, 0], "left_arm": [-30, 0, -10], "left_forearm": [-70, 0, 0],
                                "coat_back": [30, 0, 0]})
    c.key(0, P_(FOLDED, READY))
    c.key(0.3, P_(FOLDED, READY, {"body": [-14, 0, 0], "head": [-20, 0, 0], "@root": [0, 0, 1.2]}), E_SNAP)
    c.key(1.2, P_(kneel, {"head": [10, 0, 0], "@root": [0, -3.4, 0]}), E_IN)
    c.key(h("death"), kneel, E_OUT)
    c.key(L("death"), P_(kneel, {"head": [30, 0, 0]}), E_IO)

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The bust
# =====================================================================================================

def bust():
    import blockmodel as BM
    skin = lambda x, y: tone(SKIN, 3.2 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 2.8 + 0.6 * ((x + y) % 2))
    stub = lambda x, y: mix(skin(x, y), HAIR[2], 0.4)
    lg = {"s": skin, "h": hair, "b": HAIR[1], "U": lambda x, y: shade(skin(x, y), 0.8), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.82), "w": EYE, "I": EYE_HOT, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "M": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.88), "d": stub}

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 2.4 and y < 13:
            return tone(ARMOUR, 2.8 - y / 14 + (1.2 if y % 5 == 0 else 0))
        if d < 3.2 and y < 13:
            return COAT[4]
        if d < 4.4 and y % 3 == 1 and y < 12:
            return SILVER[4]
        return tone(COAT, 3.0 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    coat_ = lambda x, y: SILVER[4] if y < 2 else tone(COAT, 3.0 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4))
    feather = lambda x, y: (mix(WING["ramp"][2 + (x + y) % 3], WING["rim"], 0.25 if y < 5 else 0.0) if abs(x - 7.5) < 7 - y * 0.35 else None)
    extra = [BM.cube("collar", (5.0, 7.4, 6.4), (11.0, 9.4, 9.6), {"north": None, "up": None, "down": None, "*": "coat"}),
             BM.cube("wing_r", (2.0, 4.6, 9.6), (6.0, 13.5, 10.2), {"*": "feather"}, rot=("z", 22.5, (6.0, 7.0, 9.9))),
             BM.cube("wing_l", (10.0, 4.6, 9.6), (14.0, 13.5, 10.2), {"*": "feather"}, rot=("z", -22.5, (10.0, 7.0, 9.9))),
             BM.cube("wing2_r", (3.0, 3.4, 10.2), (6.4, 9.0, 10.6), {"*": "feather"}, rot=("z", 45, (6.0, 6.0, 10.4))),
             BM.cube("wing2_l", (9.6, 3.4, 10.2), (13.0, 9.0, 10.6), {"*": "feather"}, rot=("z", -45, (10.0, 6.0, 10.4)))]
    K.bust("au_michael", K.face_fn(FACE, lg, skin), hair, skin, chest, coat_, extra=extra, extra_tex={"feather": feather},
           shoulders_w=8.4, head_w=4.4)


def build():
    m = rig()
    t, g = m.build(gutter=1, seed=50000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t, g)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("hover_idle", 0.0), ("blade_slash", 0.35), ("rift_flee", 2.0), ("death", 2.5)), scale=4):
    m, t, g, f, clips = build()
    return K.render(m, t, clips, out_dir, NAME, poses, scale=scale, views=("front", "three_quarter", "side", "back"))


if __name__ == "__main__":
    generate()
