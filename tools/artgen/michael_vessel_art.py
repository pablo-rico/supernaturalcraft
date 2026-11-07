"""Michael in this world (v0.41, season 14), in a hunter's body: rig, texture + glowmask, clips (names and timings parsed
from MichaelVesselAssets) and his trophy bust (`block/michael_vessel_trophy`). Shared with his envoy NPC and his projection in
the mindscape (the renderer tints it white). His bar is in s1214_gui_art.

A man in his late thirties (33 px to the crown, lean, upright): short dark-blond hair, a clean strong jaw, a faint amused set
to the mouth. A dark charcoal suit, a white shirt open at the collar (no tie), a long black overcoat, open, to below the knee
(its back panel on `coat_tail` -> `coat_back`); black gloves; black shoes. Ice-blue eyes (`eyes`, lit). When his grace flares,
faint blue-white wing-shadows (`wings`: one translucent pair, flight feathers and greater coverts, s1214_wings; lit). An
archangel blade in his right hand (`blade`: the code shows it for `slash`).

No clip keys wings, coat_tail, eyes or blade.
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

NAME = "michael_vessel"
A = "animation.michael_vessel."
C = K.Contract("entity/boss/michaelvessel/MichaelVesselAssets")

B = Body(HIP=12.8, NECK=24.8, TW=3.95, TD=2.05, AW=3.3, SY=24.1, ELBOW=19.1, WRIST=14.5, PALM=12.6, LX=1.95, LW=3.6, KNEE=6.8,
         SHOE_H=1.3, HEM=4.6)

SKIN = Ramp("#5a4238", "#88685a", "#ac8c7c", "#c4a492", "#d6baa8", "#e6d0c0")
HAIR = Ramp("#4a3a1c", "#6a5428", "#8a6e36", "#a88a48", "#c2a45e", "#d8bc78")
SUIT = Ramp("#121316", "#1a1b1f", "#232428", "#2d2e33", "#38393f", "#46474e")
COAT = Ramp("#040405", "#08080a", "#0e0e11", "#151519", "#1e1e23", "#2c2c33")
SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
GLOVE = Ramp("#040405", "#09090b", "#111114", "#1a1a1e", "#26262c", "#3c3c45")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
SILVER = Ramp("#5d636b", "#868d96", "#aab1ba", "#c8ced6", "#e0e5ea", "#f7f9fb")
GRIP = Ramp("#14161c", "#1d2029", "#272b37", "#333847", "#414759", "#535a6e")
LIPS = Ramp("#4a2c26", "#623a32", "#7a4c42", "#8e5c50", "#a06c5e", "#b27c6e")
EYE_HOT, EYE, EYE_DIM = hexc("#f2fbff"), hexc("#8fd4ff"), hexc("#2a5a8a")
WING = dict(ramp=Ramp("#20304a", "#3a5478", "#5a7ca6", "#86a8cc", "#b4cce6", "#e2eefa"), root=hexc("#2a3a5a"), rim=hexc("#f2f8ff"),
            alpha=130, fade=70, glow=(0.25, 0.55), wisp=True)

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hsssssshhsssssss",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "ssUUssssssssUUss",
    "skwIIwsssswIIwks",
    "sskussssssssukss",
    "ssssssnnssssssss",
    "skssssnnssssssks",
    "sjsssNnnNsssssjs",
    "sjssssssssssssjs",
    "sjjsslllllLLsjjs",
    "ssjjssssssssjjss",
    "sssjjjjjjjjjjsss",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def build_head(m, seed=53001):
    sk = skin_m(SKIN, seed, 3.1, 0.3)
    hr = hair_m(HAIR, seed + 1, base=2.8)
    lg = K.legend(sk, hr, hexc("#d6d0c8"), hexc("#5a8aa8"), LIPS, SKIN, brow=HAIR[1])
    sides = H.head_sides(sk, hr, top=3, back=11, temple=8, sideburn=5)
    head(m, B, FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 0 else hr(f, x, y, w, h), shell_inflate=0.25, shell_rows=2.0)
    K.brows(m, B, HAIR[1], tilt=-3, w=2.4, h=0.45)
    eye_shell(m, B, "dEHEd..dEHEd", {"d": EYE_DIM, "E": EYE, "H": EYE_HOT})
    neck(m, B, sk, 3.4)


def build_body(m, seed=53200):
    shirt = fabric(SHIRT, seed + 1, 3.7, 0.2)
    suit = fabric(SUIT, seed + 2, 3.0, 0.3)
    skin = skin_m(SKIN, seed + 9, 3.1, 0.3)

    def shirt_front(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 3 and abs(x - mid) < 1.6 - y * 0.5:
            return skin(f, x, y, w, h)              # open at the collar
        return shirt(f, x, y, w, h) if f == "north" else SUIT[1]
    torso(m, B, shirt_front, tag="torso")
    front = H.jacket_front(suit, SUIT[4], shirt_front, tie=None, gap=(5, 10), v_depth=10, buttons=SUIT[0], button_rows=(12, 16))
    torso(m, B, front, inflate=0.25, bottom=B.HIP - 0.8, top=B.NECK - 0.2, tag="jacket", faces_off=("down",))
    for s in (-1, 1):
        m.cube("body", (s * 1.2 - 0.9, B.NECK - 1.4, -B.TD - 0.4), (1.8, 1.4, 0.4), lambda f, x, y, w, h: tone(SHIRT, 4.6 - y * 0.6),
               density=2, tag="shirt_collar", rotation=(16, 0, -34 * s), pivot=(s * 1.2, B.NECK - 0.1, -B.TD - 0.3))
    cl = wool_m(COAT, seed + 3, 2.8)
    coat(m, B, COAT, cl, seed + 3, lapel=True, collar_up=False, pockets=True)
    K.move_cubes(m, "coat_tail", "coat_back")
    cuff = lambda f, x, y, w, h: tone(COAT, 3.4) if f not in ("up", "down") else COAT[0]
    arms(m, B, wool_m(COAT, seed + 4, 2.7), None, cuff=cuff, cuff_depth=1.0, glove=glove_m(GLOVE, seed + 5))
    legs(m, B, fabric(SUIT, seed + 6, 2.9, 0.25, crease=True), SHOE)


def build_blade(m):
    cx = -B.AX
    y = B.PALM - 0.3
    b = m.bone("blade", (cx, y, 0), "right_hand", rotation=(32, 0, 0))
    m.cube(b, (cx - 0.5, y - 0.5, -0.6), (1.0, 1.0, 3.4), lambda f, xx, yy, w, h: GRIP[2 + ((xx + yy) % 3 == 0) * 2], density=2, tag="grip")
    m.cube(b, (cx - 0.75, y - 0.75, -1.1), (1.5, 1.5, 0.5), solid(SILVER[3]), density=2, tag="guard")

    def steel(f, xx, yy, w, h):
        if f in ("north", "south", "up", "down"):
            return SILVER[5]
        if yy in (0, h - 1):
            return EYE
        return SILVER[4] if f == "west" else SILVER[2]
    lit = lambda f, xx, yy, w, h, c: c if c == EYE else shade(c, 0.22)
    m.cube(b, (cx - 0.5, y - 0.5, -11.0), (1.0, 1.0, 9.9), steel, lit, density=2, tag="blade_steel", rotation=(0, 0, 45), pivot=(cx, y, 0))
    m.cube(b, (cx - 0.25, y - 0.25, -12.0), (0.5, 0.5, 1.0), solid(SILVER[5]), lit, density=2, tag="tip", rotation=(0, 0, 45), pivot=(cx, y, 0))


def rig():
    m = Model(NAME, 256, 512)
    skeleton(m, B, coat=True)
    build_head(m)
    build_body(m)
    build_blade(m)
    wb = m.bone("wings", (0, B.NECK - 4.0, B.TD + 0.6), "body")
    W.build(m, wb.name, "wing", WING, off=(0.0, B.NECK - 3.8, B.TD - 1.0), k=1.15, kinds=("primary", "secondary", "tertial", "greater"),
            seed=53500)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

POCKETS = P_(K.fist("right", 0.6), K.fist("left", 0.6), {"right_arm": [-6, 0, 8], "right_forearm": [-34, -14, 0], "right_hand": [0, 0, -10],
                                                        "left_arm": [-6, 0, -8], "left_forearm": [-34, 14, 0], "left_hand": [0, 0, 10],
                                                        "head": [-3, 0, 0], "body": [-2, 0, 0]})
LOOSE = P_(K.relax("right"), K.relax("left"), {"right_arm": [-4, 0, 4], "left_arm": [-4, 0, -4], "right_forearm": [-12, 0, 0],
                                               "left_forearm": [-12, 0, 0], "head": [-3, 0, 0]})


def anims():
    f = AnimFile()
    clips = []
    nm = {b.name for b in rig().rig.bones}

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    FOLDED = W.pose("wing", nm, **W.FOLD)
    OPEN = W.pose("wing", nm, **W.OPEN)
    WIDE = W.pose("wing", nm, **W.WIDE)
    UNFURL = W.pose("wing", nm, **W.UNFURL)
    h, L = C.hit, C.length
    cs = lambda u, a=1.0: {"coat_back": [3 * a * math.sin(u * 6.28), 0, 1.2 * a * math.sin(u * 3.14)],
                           "coat_r": [-2 * a * math.sin(u * 6.28 + 0.6), 0, 0], "coat_l": [-2 * a * math.sin(u * 6.28 + 1.2), 0, 0]}
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, P_(FOLDED, POCKETS), 4.0, head_turn=6, sway=lambda u: cs(u, 0.4))
    c = clip("walk", 1.3, loop=True)
    K.walk(c, P_(FOLDED, POCKETS), 1.3, stride=17, arm=0, bob=0.3, twist=4, knee=(6, 14), arms=False,
           extra=lambda s: {"coat_r": [-9 * s, 0, 0], "coat_l": [9 * s, 0, 0], "coat_back": [8, 0, 2 * s], "head": [-3, -3 * s, 0]})
    # Lock (mindscape): hammering on the door of the box he is shut in.
    c = clip("lock", 1.2, loop=True)
    pound = lambda k: P_(FOLDED, K.fist("right"), K.fist("left"), {"right_arm": [-150 + 50 * k, 0, -6], "left_arm": [-110 - 30 * k, 0, 6],
                                                                    "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0],
                                                                    "body": [8 + 4 * k, 0, 0], "head": [6, 0, 0], "@root": [0, 0, -0.4]})
    c.key(0, pound(0))
    c.key(0.3, pound(1), E_SNAP)
    c.key(0.6, pound(0), E_IO)
    c.key(0.9, pound(1), E_SNAP)
    c.key(1.2, pound(0), E_IO)

    # Snap (8): a snap of the fingers.
    c = clip("snap")
    raise_ = P_(FOLDED, LOOSE, {"right_arm": [-70, 0, 20], "right_forearm": [-60, -20, 0], "right_fingers": [60, 0, 0], "right_middle": [-10, 0, 0],
                                "right_index": [50, 0, 0], "right_thumb": [-10, 0, 40], "head": [-6, 0, 4]})
    snapped = P_(raise_, {"right_middle": [90, 0, 0], "right_thumb": [-30, 0, 10], "right_hand": [0, 0, -14], "right_arm": [-74, 0, 18]})
    c.key(0, P_(FOLDED, POCKETS))
    c.key(0.3, raise_, E_IO)
    c.key(h("snap"), snapped, E_SNAP)
    c.key(0.55, snapped, E_OUT)
    c.key(L("snap"), P_(FOLDED, POCKETS), E_IO)

    # Choke (14): a hand raised, closing -- the telekinetic choke, held.
    c = clip("choke")
    out = P_(FOLDED, LOOSE, K.spread("left"), {"left_arm": [-96, 0, -4], "left_forearm": [-10, 0, 0], "left_hand": [0, 90, 0], "head": [-8, 6, 0],
                                              "body": [0, -6, 0]})
    closed = P_(out, K.fist("left", 0.8), {"left_arm": [-112, 0, -4], "left_forearm": [-20, 0, 0], "head": [-10, 8, 6]})
    c.key(0, P_(FOLDED, POCKETS))
    c.key(0.4, out, E_IO)
    c.key(h("choke"), closed, E_IN)
    for i in range(1, 4):
        j = K.jit("mv-choke", i, 2)
        c.key(h("choke") + 0.15 * i, P_(closed, {"left_arm": [-112 - 3 * i + j[0], j[1], -4]}))
    c.key(1.3, P_(closed, {"left_arm": [-122, 0, -4]}), E_IO)
    c.key(L("choke"), P_(FOLDED, POCKETS), E_IO)

    # Smite (12): a palm of blue light.
    c = clip("smite")
    draw = P_(OPEN, LOOSE, K.spread("right"), {"right_arm": [-30, 0, 10], "right_forearm": [-100, 0, 0], "body": [0, 16, 0], "head": [-4, -8, 0]})
    push = P_(WIDE, LOOSE, K.spread("right"), {"right_arm": [-90, 0, 0], "right_forearm": [0, 0, 0], "right_hand": [-70, 0, 0], "body": [6, -12, 0],
                                              "head": [-8, 10, 0], "@root": [0, 0, -0.8]})
    c.key(0, P_(FOLDED, POCKETS))
    c.key(0.4, draw, E_IO)
    c.key(h("smite"), push, E_SNAP)
    c.key(0.85, P_(push, {"right_hand": [-74, 0, 0]}), E_OUT)
    c.key(L("smite"), P_(FOLDED, POCKETS), E_IO)

    # Wing flare (10): the wings flared wide, his arms opening.
    c = clip("wing_flare")
    flare = P_(UNFURL, K.spread("right"), K.spread("left"), {"right_arm": [-14, 0, 40], "left_arm": [-14, 0, -40], "head": [-10, 0, 0],
                                                             "body": [-6, 0, 0], "@root": [0, 0.4, 0]}, cs(0.25, 2))
    c.key(0, P_(FOLDED, POCKETS))
    c.key(0.3, P_(FOLDED, LOOSE, {"body": [10, 0, 0], "head": [10, 0, 0]}), E_IO)
    c.key(h("wing_flare"), flare, E_BACK)
    c.key(0.8, P_(flare, WIDE), E_IO)
    c.key(L("wing_flare"), P_(FOLDED, POCKETS), E_IO)

    # Teleport (6): a flicker away -- a sharp step and a turn of the shoulders.
    c = clip("teleport")
    c.key(0, P_(FOLDED, POCKETS))
    c.key(h("teleport"), P_(OPEN, POCKETS, {"body": [-6, 30, 0], "head": [-6, -20, 0], "@root": [0, 0.6, 0]}), E_SNAP)
    c.key(L("teleport"), P_(FOLDED, POCKETS), E_OUT)

    # Slash (7): an archangel blade drawn and cut.
    c = clip("slash")
    up = P_(K.fist("right"), {"right_arm": [-150, -20, 30], "right_forearm": [-40, 0, 0], "body": [-6, 20, 0], "head": [-6, -12, 0], "@root": [0, 0.2, 0.6]})
    down = P_(K.fist("right"), {"right_arm": [-40, 20, -40], "right_forearm": [-20, 0, 0], "body": [16, -20, 0], "head": [4, 12, 0],
                                "right_leg": [-20, 0, 0], "left_leg": [12, 0, 0], "@root": [0, -0.8, -1.4], "coat_back": [20, 0, 0]})
    c.key(0, P_(FOLDED, LOOSE))
    c.key(0.22, P_(OPEN, up), E_OUT)
    c.key(h("slash"), P_(WIDE, down), E_SNAP)
    c.key(0.5, P_(OPEN, down, {"right_arm": [-36, 22, -44]}), E_OUT)
    c.key(L("slash"), P_(FOLDED, LOOSE), E_IO)

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, P_(FOLDED, POCKETS), L("hurt"), h("hurt"), {"body": [-12, 10, 0], "head": [-18, 14, 0], "@root": [0, 0, 1.0],
                                                           "right_arm": [-20, 0, 24], "coat_back": [14, 0, 0]})

    # Expelled (40): light pours out of him upward -- head back, arms out, lifted on his toes; held, then he slumps.
    pour = P_(UNFURL, K.spread("right"), K.spread("left"), {"body": [-22, 0, 0], "head": [-50, 0, 0], "right_arm": [-20, 0, 70],
                                                            "left_arm": [-20, 0, -70], "@root": [0, 1.4, 0], "right_leg": [0, 0, 4],
                                                            "left_leg": [0, 0, -4]}, cs(0.25, 2))
    c = clip("expelled", hold=True)
    c.key(0, P_(FOLDED, LOOSE))
    c.key(0.8, P_(FOLDED, LOOSE, {"body": [20, 0, 0], "head": [20, 0, 0], "right_arm": [-40, 0, -20], "left_arm": [-40, 0, 20],
                                  "right_forearm": [-80, -30, 0], "left_forearm": [-80, 30, 0], "@root": [0, -0.6, 0]}), E_IO)
    c.key(h("expelled"), pour, E_BACK)
    for i in range(1, 6):
        j = K.jit("mv-expel", i, 3)
        c.key(h("expelled") + 0.15 * i, P_(pour, {"head": [-50 + j[0], j[1], j[2]], "body": [-22 + j[2], j[0], 0]}))
    knees = P_(FOLDED, LOOSE, {"@root": [0, -5.6, 0.4], "right_leg": [-90, 0, 4], "left_leg": [-90, 0, -4], "right_shin": [90, 0, 0],
                               "left_shin": [90, 0, 0], "body": [20, 0, 0], "head": [30, 0, 0], "right_arm": [-6, 0, 8], "left_arm": [-6, 0, -8]})
    c.key(3.0, knees, E_IN)
    c.key(L("expelled"), P_(knees, {"head": [36, 0, 0]}), E_IO)

    # Death (40): the same light, and he falls.
    c = clip("death", hold=True)
    c.key(0, P_(FOLDED, LOOSE))
    c.key(0.8, P_(FOLDED, LOOSE, {"body": [20, 0, 0], "head": [20, 0, 0], "@root": [0, -0.6, 0]}), E_IO)
    c.key(h("death"), pour, E_BACK)
    for i in range(1, 4):
        j = K.jit("mv-death", i, 3)
        c.key(h("death") + 0.15 * i, P_(pour, {"head": [-50 + j[0], j[1], j[2]]}))
    back = {"root": [86, 0, 0], "@root": [0, 2.4, 1.0], "head": [10, 16, 0], "right_arm": [-10, 0, 50], "left_arm": [-20, 0, -60],
            "right_leg": [-20, 0, 6], "right_shin": [30, 0, 0], "left_leg": [0, 0, -6], "coat_back": [-60, 0, 0]}
    c.key(3.0, P_(FOLDED, back), E_IN)
    c.key(L("death"), P_(FOLDED, back))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


def bust():
    import blockmodel as BM
    skin = lambda x, y: tone(SKIN, 3.3 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 2.8 + 0.6 * math.sin(x * 1.7 + y * 0.9))
    lg = {"s": skin, "h": hair, "b": HAIR[1], "U": lambda x, y: shade(skin(x, y), 0.8), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.82), "w": EYE, "I": EYE_HOT, "n": SKIN[4], "N": SKIN[1], "l": LIPS[1], "L": LIPS[3],
          "M": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.9)}

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 1.4 - 0.3 * y and y < 3:
            return skin(x, y)
        if d < 2.2 and y < 9:
            return tone(SHIRT, 4.2 - y / 14)
        if d < 3.0 and y < 11:
            return tone(SUIT, 2.6)
        if d < 4.0 and y < 12:
            return COAT[4]
        return tone(COAT, 2.8 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    coat_ = lambda x, y: tone(COAT, 2.8 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4))
    feather = lambda x, y: ((150 + 10 * ((x + y) % 3), 190, 230, 150) if abs(x - 7.5) < 7 - y * 0.35 else None)
    extra = [BM.cube("wing_r", (2.0, 4.6, 9.8), (6.0, 13.0, 10.2), {"*": "feather"}, rot=("z", 22.5, (6.0, 7.0, 10.0))),
             BM.cube("wing_l", (10.0, 4.6, 9.8), (14.0, 13.0, 10.2), {"*": "feather"}, rot=("z", -22.5, (10.0, 7.0, 10.0)))]
    K.bust("michael_vessel", K.face_fn(FACE, lg, skin), hair, skin, chest, coat_, extra=extra, extra_tex={"feather": feather},
           shoulders_w=7.6, head_w=4.2)


def build():
    m = rig()
    t, g = m.build(gutter=1, seed=53000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build()
    K.write_entity(m, f, NAME, t, g)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("wing_flare", 0.5), ("choke", 0.8), ("lock", 0.3), ("expelled", 2.2)), scale=4):
    m, t, g, f, clips = build()
    return K.render(m, t, clips, out_dir, NAME, poses, scale=scale, views=("front", "three_quarter", "side", "back"))


if __name__ == "__main__":
    generate()
