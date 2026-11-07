"""Dagon, Prince of Hell (v0.41, season 12), in her vessel: rig, texture + glowmask, clips (names and timings parsed from
DagonAssets) and her trophy bust (`block/dagon_trophy`). Her bar is in s1214_gui_art.

A tall woman (34 px to the crown, long-limbed, very still): long straight black hair parted in the middle, down her back
(`hair` -- `hair_back` -- `hair_tip`, and the locks `hair_r`, `hair_l` over her shoulders); a pale narrow face, dark brows,
dark red lips. A long black coat with a standing collar, open, to her ankles (`coat_tail` holds the back panel through its
child `coat_back`), over a dark red dress; black boots. Her eyes burn red-gold when her power shows (`eyes`, a lit shell);
a shimmer of bent air wraps her right forearm and hand (`aura`, lit: the renderer shows it while she casts).

Skeleton: apoc_vessel's (coat) + hair chain (head), coat_back (coat_tail), aura (right_forearm). No clip keys coat_tail, hair,
aura or eyes; the clips sway coat_back and hair_back.
"""

import math

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, coat, eye_shell, hair_m, head, leather_m, legs, neck,
                         skeleton, skin_m, tone, torso, wool_m)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

NAME = "dagon"
A = "animation.dagon."
C = K.Contract("entity/boss/dagon/DagonAssets")

B = Body(HIP=13.4, NECK=25.0, TW=3.35, TD=1.8, AW=2.8, SY=24.3, ELBOW=19.4, WRIST=14.8, PALM=13.0, LX=1.7, LW=3.2, KNEE=7.2,
         SHOE_H=1.4, HEM=2.2)

SKIN = Ramp("#6a5a58", "#968482", "#b8a8a6", "#cfc1bf", "#e0d5d3", "#efe7e6")
HAIR = Ramp("#030304", "#08080a", "#0f0f12", "#17171b", "#202026", "#2c2c34")
COAT = Ramp("#040405", "#09090b", "#101013", "#18181c", "#222228", "#303038")
DRESS = Ramp("#1c0306", "#30060b", "#460a10", "#5c1016", "#73161c", "#8c2026")
BOOT = Ramp("#020203", "#06060a", "#0d0d12", "#15151c", "#20202a", "#454555")
LIPS = Ramp("#2a0608", "#3e0a0e", "#541016", "#6a161c", "#801e22", "#962a2c")
EYE_WHITE, IRIS = hexc("#d8d0cc"), hexc("#2a1a14")
FIRE_HOT, FIRE, FIRE_DIM = hexc("#fff0b0"), hexc("#ffa030"), hexc("#b03010")

FACE = [
    "hhhhhhhPhhhhhhhh",
    "hhhhhhhPhhhhhhhh",
    "hhhhhhsPshhhhhhh",
    "hhhhhsssssshhhhh",
    "hhhssssssssssshh",
    "hhsssssssssssshh",
    "hhbbbbssssbbbbhh",
    "hhUwIIwsswIIwUhh",
    "hhsUUsssssUUsshh",
    "hhsssssnnssssshh",
    "hhkssssnnssssshh",
    "hhsssssNNssssshh",
    "hhjssssssssssjhh",
    "hhjssLMMMMLssjhh",
    "hhhjssllllssjhhh",
    "hhhhjjjjjjjjhhhh",
]
FACE = [r[:16] for r in FACE]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=46001):
    sk = skin_m(SKIN, seed, 3.3, 0.25)
    hr = hair_m(HAIR, seed + 1, base=2.4)
    sheen = lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.25) if (x * 3 + y) % 11 == 0 else hr(f, x, y, w, h)
    lg = K.legend(sk, sheen, EYE_WHITE, IRIS, LIPS, SKIN, brow=HAIR[0], extra={"P": HAIR[0]})
    sides = H.head_sides(sk, sheen, top=6, back=16, temple=4, sideburn=14, ear=False)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: (None if f == "north" and not (y < 1 or abs(x - 7.5) > 5.5 - y * 0.6) else sheen(f, x, y, w, h)),
         shell_inflate=0.3, shell_rows=3.0)
    K.brows(m, B, HAIR[0], tilt=-10, w=2.2, h=0.4)
    eye_shell(m, B, "rFHFr..rFHFr", {"r": FIRE_DIM, "F": FIRE, "H": FIRE_HOT})
    neck(m, B, sk, 2.6)
    strand = lambda f, x, y, w, h: tone(HAIR, 2.4 + 0.9 * math.sin(x * 1.7 + seed) - 0.5 * (y / max(1, h))
                                        + (0.9 if (x * 5 + y) % 9 == 0 else 0))
    K.long_hair(m, B, strand, seed, length=13.0, width=8.4, lock_len=10.0)


def build_body(m, seed=46200):
    dress = fabric(DRESS, seed + 1, 3.0, 0.4)

    def bodice(f, x, y, w, h):
        if f == "north" and y < 3 and abs(x - (w - 1) / 2) < 2.0 - y * 0.5:
            return shade(skin_m(SKIN, seed + 9, 3.3, 0.25)(f, x, y, w, h), 0.95)
        return dress(f, x, y, w, h)
    torso(m, B, bodice, tag="torso")
    K.skirt(m, B, fabric(DRESS, seed + 2, 2.8, 0.45), hem=2.6, flare=0.4)
    cl = wool_m(COAT, seed + 3, 2.8)
    coat(m, B, COAT, cl, seed + 3, lapel=False, collar_up=True, pockets=False)
    K.move_cubes(m, "coat_tail", "coat_back")
    sleeve = wool_m(COAT, seed + 4, 2.7)
    cuff = lambda f, x, y, w, h: tone(COAT, 3.6 - y * 0.5) if f not in ("up", "down") else COAT[0]
    arms(m, B, sleeve, skin_m(SKIN, seed + 5, 3.4, 0.25), cuff=cuff, cuff_depth=1.0, sleeve_inflate=0.3)
    legs(m, B, fabric(DRESS, seed + 6, 2.4, 0.2), BOOT, boot_h=5.4, toe=0.9)
    # The aura: bent air wrapped round her right forearm and hand, lit at its rim.
    au = m.bone("aura", (-B.AX, B.ELBOW, 0), "right_forearm")

    def air(f, x, y, w, h):
        # Heat-haze: broken ripples of light, thicker near the palm, never a closed outline.
        n = h01("au", f, x, y)
        ripple = math.sin((y + 0.5 * x) * 1.3 + len(f)) > 0.55
        if not ripple or n < 0.35 + 0.4 * (1 - y / max(1, h)):
            return None
        c = FIRE_HOT if n > 0.9 else (FIRE if n > 0.7 else FIRE_DIM)
        return (c[0], c[1], c[2], 120)
    m.cube(au, (-B.AX - 1.9, B.PALM - 1.6, -1.9), (3.8, B.ELBOW - B.PALM, 3.8), air, glow=lambda f, x, y, w, h, c: c,
           density=2, tag="aura", shading=False)


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=True)
    build_head(m)
    build_body(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

STILL = P_(K.relax("right"), K.relax("left"), {"right_arm": [-2, 0, 3], "left_arm": [-2, 0, -3], "right_forearm": [-6, 0, 0],
                                               "left_forearm": [-6, 0, 0], "head": [-4, 0, 0], "body": [-2, 0, 0]})
CROSSED = {"right_arm": [-26, 10, -8], "left_arm": [-30, -10, 8], "right_forearm": [-90, -50, 0], "left_forearm": [-84, 54, 0],
           "right_hand": [0, 0, 10], "left_hand": [0, 0, -10], "right_fingers": [40, 0, 0], "left_fingers": [40, 0, 0],
           "body": [-4, 0, 0], "head": [-8, 0, 0]}
POINT = {"right_fingers": [80, 0, 0], "right_middle": [75, 0, 0], "right_index": [-6, 0, 0], "right_thumb": [-20, 0, 20]}
HOVER_Y = 7.0


def drift(t, amp=1.0):
    """The coat's back and her hair stir (the named bones are the renderer's: the clips move their children)."""
    ph = t * 2 * math.pi
    return {"coat_back": [3 * amp * math.sin(ph), 0, 1.2 * amp * math.sin(ph * 0.5)], "coat_r": [-2 * amp * math.sin(ph + 0.6), 0, 0],
            "coat_l": [-2 * amp * math.sin(ph + 1.2), 0, 0], "hair_back": [2 * amp * math.sin(ph + 0.3), 0, amp * math.sin(ph * 0.5)],
            "hair_tip": [3 * amp * math.sin(ph + 0.9), 0, 0]}


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, STILL, 4.0, head_turn=3, sway=lambda u: drift(u, 0.4))

    c = clip("walk", 1.6, loop=True)
    K.walk(c, STILL, 1.6, stride=12, arm=3, bob=0.12, twist=2, knee=(4, 10),
           extra=lambda s: {"coat_r": [-8 * s, 0, 0], "coat_l": [8 * s, 0, 0], "coat_back": [8, 0, 2 * s], "hair_back": [4, 0, 0]})

    c = clip("hover_idle", 3.0, loop=True)
    hang = {"right_leg": [8, 0, 3], "left_leg": [2, 0, -3], "right_shin": [10, 0, 0], "left_shin": [16, 0, 0]}
    for i, t in enumerate((0, 0.75, 1.5, 2.25, 3.0)):
        u = t / 3.0
        b = math.sin(u * 2 * math.pi)
        c.key(t, P_(STILL, hang, drift(u * 2, 1.4), {"@root": [0, HOVER_Y + 0.6 * b, 0], "right_arm": [-6, 0, 16 + 2 * b],
                                                     "left_arm": [-6, 0, -16 - 2 * b], "head": [-6, 0, 0]}), E_IO if i else None)

    c = clip("guard", 4.0, loop=True)
    K.breathe(c, P_(STILL, CROSSED), 4.0, head_turn=4, sway=lambda u: drift(u, 0.3))

    # --- once ----------------------------------------------------------------------------------------------------
    # Fling (10): the right hand lifted to the side, then flicked out -- a telekinetic throw.
    c = clip("fling")
    lift = P_(STILL, K.spread("right"), {"right_arm": [-60, 0, -40], "right_forearm": [-60, -30, 0], "right_hand": [0, 0, -20],
                                        "body": [0, 18, 0], "head": [-4, 10, 0]})
    flick = P_(STILL, K.spread("right"), {"right_arm": [-80, 0, 60], "right_forearm": [-6, 0, 0], "right_hand": [0, 0, 40],
                                         "body": [0, -18, 0], "head": [-4, -14, 0]}, drift(0.25, 2))
    c.key(0, STILL)
    c.key(0.35, lift, E_IO)
    c.key(h("fling"), flick, E_SNAP)
    c.key(0.75, P_(flick, {"right_arm": [-74, 0, 56]}), E_OUT)
    c.key(L("fling"), STILL, E_IO)

    # Crush (18): a hand held out, the fingers closing slowly into a fist.
    c = clip("crush")
    out = P_(STILL, K.spread("right"), {"right_arm": [-86, 0, 6], "right_forearm": [-10, 0, 0], "right_hand": [0, -90, 0],
                                       "head": [-6, 0, 0], "body": [0, 6, 0]})
    c.key(0, STILL)
    c.key(0.35, out, E_IO)
    c.key(0.65, P_(out, K.fist("right", 0.35)), E_IO)
    c.key(h("crush"), P_(out, K.fist("right", 1.1), {"right_forearm": [-16, 0, 0], "head": [-10, 0, 0]}), E_IN)
    c.key(1.15, P_(out, K.fist("right", 1.1), {"right_forearm": [-18, 0, 0]}))
    c.key(L("crush"), STILL, E_IO)

    # Fire palm (12): a palm thrust -- the jet of hellfire.
    c = clip("fire_palm")
    draw = P_(STILL, K.spread("right"), {"right_arm": [-30, 0, 10], "right_forearm": [-100, 0, 0], "body": [0, 16, 0], "head": [-4, -8, 0]})
    push = P_(STILL, K.spread("right"), {"right_arm": [-90, 0, 0], "right_forearm": [0, 0, 0], "right_hand": [-70, 0, 0], "body": [6, -12, 0],
                                        "head": [-8, 10, 0], "@root": [0, 0, -0.8]}, drift(0.25, 2))
    c.key(0, STILL)
    c.key(0.4, draw, E_IO)
    c.key(h("fire_palm"), push, E_SNAP)
    c.key(0.85, P_(push, {"right_hand": [-74, 0, 0]}), E_OUT)
    c.key(L("fire_palm"), STILL, E_IO)

    # Fire ring (20): both arms raised wide, then swept down -- the ring of fire.
    c = clip("fire_ring")
    up = P_(K.spread("right"), K.spread("left"), {"right_arm": [-20, 0, 150], "left_arm": [-20, 0, -150], "head": [-18, 0, 0],
                                                  "body": [-6, 0, 0], "@root": [0, 0.4, 0]})
    down = P_(K.spread("right"), K.spread("left"), {"right_arm": [-10, 0, 40], "left_arm": [-10, 0, -40], "right_hand": [0, 0, -20],
                                                    "left_hand": [0, 0, 20], "head": [10, 0, 0], "body": [8, 0, 0], "@root": [0, -0.6, 0]},
              drift(0.25, 2.5))
    c.key(0, STILL)
    c.key(0.65, up, E_IO)
    c.key(0.8, P_(up, {"right_arm": [-20, 0, 156], "left_arm": [-20, 0, -156]}))
    c.key(h("fire_ring"), down, E_SNAP)
    c.key(1.3, P_(down, {"head": [6, 0, 0]}), E_OUT)
    c.key(L("fire_ring"), STILL, E_IO)

    # Pin (8): one finger points, holding a target in the air.
    c = clip("pin")
    point = P_(STILL, POINT, {"right_arm": [-94, 0, 2], "right_forearm": [-4, 0, 0], "head": [-6, 0, 0], "body": [0, 4, 0]})
    c.key(0, STILL)
    c.key(0.25, P_(point, {"right_arm": [-70, 0, 6]}), E_IO)
    c.key(h("pin"), point, E_SNAP)
    c.key(0.65, point)
    c.key(L("pin"), STILL, E_IO)

    # Scream (14): head thrown back, arms flung down and back -- the shriek.
    c = clip("scream")
    gather = P_(STILL, {"body": [14, 0, 0], "head": [20, 0, 0], "right_arm": [-20, 0, 10], "left_arm": [-20, 0, -10]})
    shriek = P_(K.spread("right"), K.spread("left"), {"body": [-16, 0, 0], "head": [-40, 0, 0], "right_arm": [30, 0, 30],
                                                      "left_arm": [30, 0, -30], "@root": [0, 0.2, 0.6]}, drift(0.25, 3))
    c.key(0, STILL)
    c.key(0.45, gather, E_IO)
    c.key(h("scream"), shriek, E_SNAP)
    for i, t in enumerate((0.9, 1.1, 1.2)):
        j = K.jit("dagon-scream", i, 2)
        c.key(t, P_(shriek, {"head": [-40 + j[0], j[1], j[2]]}))
    c.key(L("scream"), STILL, E_IO)

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, STILL, L("hurt"), h("hurt"), P_(drift(0.25, 2), {"body": [-8, 8, 0], "head": [-14, 12, 0], "@root": [0, 0, 0.6],
                                                                 "right_arm": [-20, 0, 24], "left_arm": [-10, 0, -20]}))

    # Death (50): she doubles over; her eyes flare, she arches back burning from inside; she sinks and falls.
    c = clip("death", hold=True)
    bend = P_(STILL, {"body": [24, 0, 0], "head": [20, 0, 0], "right_arm": [-40, 0, -14], "left_arm": [-40, 0, 14],
                      "right_forearm": [-60, -30, 0], "left_forearm": [-60, 30, 0], "@root": [0, -0.4, 0.4]})
    arch = P_(K.spread("right"), K.spread("left"), drift(0.25, 3),
              {"body": [-26, 0, 0], "head": [-50, 0, 0], "right_arm": [-20, 0, 70], "left_arm": [-20, 0, -70], "@root": [0, 0.6, 0.6]})
    c.key(0, STILL)
    c.key(0.4, bend, E_SNAP)
    for i in range(1, 6):
        j = K.jit("dagon-death", i, 2 + i)
        c.key(0.4 + 0.3 * i, P_(bend, {"body": [24 - 8 * i + j[0], j[1], j[2]], "head": [20 - 10 * i, j[0], j[1]]}))
    c.key(h("death"), arch, E_BACK)
    for i in range(1, 4):
        j = K.jit("dagon-burn", i, 4)
        c.key(h("death") + 0.25 * i, P_(arch, {"body": [-26 + j[0], j[1], j[2]], "head": [-50 + j[2], j[0], 0]}))
    knees = P_(arch, {"@root": [0, -6.2, 0.6], "right_leg": [-90, 0, 4], "left_leg": [-90, 0, -4], "right_shin": [90, 0, 0],
                      "left_shin": [90, 0, 0], "body": [10, 0, 0], "head": [10, 0, 0], "right_arm": [-10, 0, 10], "left_arm": [-10, 0, -10]})
    c.key(3.5, knees, E_IN)
    c.key(3.8, K.lie(side=-1, lift=3.0, extra={"coat_back": [10, 0, 0]}), E_IN)
    c.key(L("death"), K.lie(side=-1, lift=3.0, extra={"coat_back": [10, 0, 0]}))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# The bust
# =====================================================================================================

def bust():
    import blockmodel as BM
    skin = lambda x, y: tone(SKIN, 3.5 + 0.2 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 2.4 + 0.8 * math.sin(x * 1.7 + y * 0.3) + (0.8 if (x * 5 + y) % 9 == 0 else 0))
    lg = {"s": skin, "h": hair, "P": HAIR[0], "b": HAIR[0], "U": lambda x, y: shade(skin(x, y), 0.8), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.82), "w": FIRE, "I": FIRE_HOT, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "M": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.9)}

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 2.0 - 0.3 * y and y < 4:
            return skin(x, y)
        if d < 3.2 and y < 13:
            return tone(DRESS, 3.0 - y / 14 + 0.3 * math.sin(x + y))
        if d < 3.8 and y < 13:
            return COAT[4]
        return tone(COAT, 2.8 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    coat_ = lambda x, y: tone(COAT, 2.8 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4))
    extra = [BM.cube("hair_back", (5.6, 6.0, 9.6), (10.4, 13.6, 10.5), {"*": "hair"}),
             BM.cube("collar", (5.2, 7.4, 6.2), (10.8, 9.2, 9.8), {"north": None, "up": None, "down": None, "*": "coat"})]
    K.bust("dagon", K.face_fn(FACE, lg, skin), hair, skin, chest, coat_, extra=extra, shoulders_w=7.0, head_w=4.0)


def build():
    m = rig()
    t, g = m.build(gutter=1, seed=46000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t, g)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("fire_ring", 0.75), ("crush", 0.9), ("guard", 0.0), ("death", 2.5)), scale=5):
    m, t, g, f, clips = build()
    return K.render(m, t, clips, out_dir, NAME, poses, scale=scale, views=("front", "three_quarter", "side", "back"))


if __name__ == "__main__":
    generate()
