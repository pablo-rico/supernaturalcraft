"""The two figures of Jack's birth scene (v0.41, staged, never fighting): Kelly Kline (`kelly_kline`) and Lucifer in President
Rourke's body (`rourke_lucifer`). Rigs, textures (+ Rourke's glowmask) and clips, names and timings parsed from BirthAssets.

Kelly (30 px to the crown, slim): long wavy brown hair (`hair` -- `hair_back` -- `hair_tip`, locks); a soft young face. A loose
grey cable sweater over a long dark skirt, grey socks. Heavily pregnant (`belly`: a rounded bump under the sweater, pivot at its
centre against her, so scaling it down flattens it into her; the code shrinks it after the birth). Her clips are staged on a
cot: the entity stands on the cot's mattress; `idle` sits her up on it (legs out along it), `labor` lays her back.

Rourke (33 px, tall, a little heavy): grey hair combed back, a lined, smiling politician's face; a dark navy suit, white shirt,
red tie, an American flag pin on his left lapel. His eyes go red (`eyes`, lit).

No clip keys `belly` or `hair` (the renderer's); the clips sway hair_back.
"""

import math

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, eye_shell, hair_m, head, legs, neck, skeleton, skin_m, tone,
                         torso, wool_m)
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

C = K.Contract("british/birth/BirthAssets")
KELLY = "kelly_kline"
ROURKE = "rourke_lucifer"

# =====================================================================================================
# Kelly
# =====================================================================================================

KB = Body(HIP=12.0, NECK=22.4, TW=3.3, TD=1.75, AW=2.8, SY=21.7, ELBOW=17.4, WRIST=13.3, PALM=11.6, LX=1.7, LW=3.2, KNEE=6.2,
          SHOE_H=1.0)
K_SKIN = Ramp("#5e4640", "#8e6e62", "#b49284", "#cdab9c", "#dfc2b3", "#eed8cc")
K_HAIR = Ramp("#1e120a", "#2e1c10", "#402818", "#523420", "#66422a", "#7c5436")
SWEATER = Ramp("#2e2e32", "#424248", "#56565e", "#6a6a72", "#7e7e86", "#94949c")
SKIRT = Ramp("#0c0d12", "#14161e", "#1e212c", "#282c3a", "#333848", "#42485a")
SOCK = Ramp("#3a3a3e", "#4e4e54", "#626268", "#76767c", "#8a8a90", "#a0a0a6")
K_LIPS = Ramp("#5a2c2c", "#7a3e3e", "#965050", "#aa6060", "#ba7070", "#ca8282")

K_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhssssshhhhhh",
    "hhhssssssssshhhh",
    "hhsssssssssssshh",
    "hhsssssssssssshh",
    "hhsbbbssssbbbshh",
    "hhuwIIwsswIIwuhh",
    "hhsllssssssllshh",
    "hhsssssssssssshh",
    "hhsssssnnssssshh",
    "hhsssssNNssssshh",
    "hhsssssssssssshh",
    "hhssssLMMLsssshh",
    "hhhssssssssssshh",
    "hhhhsssssssshhhh",
]
assert all(len(r) == 16 for r in K_FACE), [len(r) for r in K_FACE]


def cable(ramp, seed, base=3.0):
    """A cable-knit sweater: ribs, a plait down each panel, loose and soft."""
    def m(f, x, y, w, h):
        v = base + (0.5 if x % 2 == 0 else -0.1) - 0.5 * y / max(1, h) + (0.6 if f == "up" else 0)
        if f in ("north", "south") and (x % 6) in (2, 3) and (y + (x % 6 == 2)) % 3 == 0:
            v += 0.8
        if y >= h - 2 and f not in ("up", "down"):
            v -= 0.4 if (x % 2) else 0.0             # the ribbed hem
        return tone(ramp, v + (h01("kn", seed, f, x, y) - 0.5) * 0.5)
    return m


def kelly_rig():
    m = Model(KELLY, 256, 256)
    skeleton(m, KB, coat=False)
    seed = 47001
    sk = skin_m(K_SKIN, seed, 3.2, 0.3)

    def wavy(f, x, y, w, h):
        v = 2.7 + 0.9 * math.sin(x * 1.1 + y * 0.9 + math.sin(y * 0.6) * 2) - 0.4 * (y / max(1, h))
        return tone(K_HAIR, v + (0.7 if (x * 5 + y * 3) % 11 == 0 else 0))
    lg = K.legend(sk, wavy, hexc("#dad2c8"), hexc("#4a6a5a"), K_LIPS, K_SKIN, brow=K_HAIR[1], extra={"l": K_HAIR[0]})
    sides = H.head_sides(sk, wavy, top=6, back=16, temple=5, sideburn=12, ear=False)
    head(m, KB, K_FACE, lg, sides, nose=lambda f, x, y, w, h: K_SKIN[4] if f == "north" else K_SKIN[2],
         hair_shell=lambda f, x, y, w, h: (None if f == "north" and not (y < 1 or abs(x - 7.5) > 5.5 - y * 0.6) else wavy(f, x, y, w, h)),
         shell_inflate=0.35, shell_rows=3.0)
    K.brows(m, KB, K_HAIR[1], tilt=4, w=2.0, h=0.35)
    neck(m, KB, sk, 2.8)
    K.long_hair(m, KB, wavy, seed, length=11.0, width=8.8, lock_len=9.0)
    sw = cable(SWEATER, seed + 1)
    torso(m, KB, sw, inflate=0.35, tag="sweater")
    # The sweater's loose cowl neck.
    m.cube("body", (-2.2, KB.NECK - 1.2, -2.2), (4.4, 1.4, 4.4), lambda f, x, y, w, h: tone(SWEATER, 3.6 - y * 0.6 + (x % 2) * 0.4),
           density=2, tag="cowl")
    # The belly, under the sweater: a rounded bump, its pivot against her so a scale flattens it into her.
    bl = m.bone("belly", (0, KB.HIP + 3.5, -KB.TD), "body")
    m.cube(bl, (-2.8, KB.HIP + 0.6, -KB.TD - 2.6), (5.6, 5.8, 2.8), sw, inflate=0.3, density=2, tag="belly")
    m.cube(bl, (-2.0, KB.HIP + 1.4, -KB.TD - 3.3), (4.0, 4.2, 0.8), sw, inflate=0.3, density=2, tag="belly_front")
    arms(m, KB, cable(SWEATER, seed + 2, 2.9), skin_m(K_SKIN, seed + 3, 3.2, 0.3), sleeve_inflate=0.4,
         cuff=lambda f, x, y, w, h: tone(SWEATER, 2.8 + (x % 2) * 0.6) if f not in ("up", "down") else SWEATER[1], cuff_depth=1.4)
    legs(m, KB, fabric(SKIRT, seed + 4, 2.6, 0.3), SOCK, toe=0.8)
    K.skirt(m, KB, fabric(SKIRT, seed + 5, 2.8, 0.4), hem=1.6, flare=0.3)
    return m


# The entity stands on the cot's mattress: sitting, the hips drop to it and the legs lie straight out along it.
SIT = {"@root": [0, -10.2, 0], "right_leg": [-88, 0, 2], "left_leg": [-88, 0, -2], "right_shin": [0, 0, 0], "left_shin": [0, 0, 0]}
BELLY_HAND = P_(K.relax("right"), K.relax("left"), {"right_arm": [-30, 0, -6], "right_forearm": [-50, -30, 0], "right_hand": [0, 0, 10],
                                                    "left_arm": [10, 0, -26], "left_forearm": [-20, 0, 0], "left_hand": [-60, 0, 0]})
LIE = P_(SIT, {"body": [-80, 0, 0], "head": [40, 0, 0], "right_arm": [30, 0, 20], "left_arm": [30, 0, -20],
               "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0], "right_shin": [30, 0, 0], "right_leg": [-110, 0, 6]})
GRIPPING = P_(K.fist("right"), K.fist("left"), {"right_arm": [20, 0, 30], "left_arm": [20, 0, -30], "right_hand": [0, 0, 30],
                                                "left_hand": [0, 0, -30]})


def kelly_anims():
    f = AnimFile()
    clips = []
    A = f"animation.{KELLY}."

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    hair = lambda u, a=1.0: {"hair_back": [2 * a * math.sin(u * 2 * math.pi), 0, 0], "hair_tip": [3 * a * math.sin(u * 2 * math.pi + 0.8), 0, 0]}
    # Idle: sitting up on the cot, a hand on her belly, breathing.
    c = clip("idle", 4.0, loop=True)
    K.breathe(c, P_(SIT, BELLY_HAND, {"body": [-6, 0, 0], "head": [6, 0, 0]}), 4.0, head_turn=8,
              extra=lambda u, b: P_(hair(u, 0.5), {"right_hand": [0, 0, 10 + 6 * math.sin(u * 4 * math.pi)]}))
    # Labor: lying back, gripping the cot, her breath coming hard.
    c = clip("labor", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        b = math.sin(t / 2.0 * 2 * math.pi)
        c.key(t, P_(LIE, GRIPPING, {"body": [-80 + 6 * max(0, b), 0, 0], "head": [40 + 10 * max(0, b), 4 * b, 0],
                                    "right_arm": [20 - 6 * b, 0, 30], "left_arm": [20 - 6 * b, 0, -30], "@body": [0, 0.2 * b, 0]}),
              E_IO if i else None)
    # Glow (20): the light pours from her -- she arches, head back, arms flung out.
    c = clip("glow", hold=True)
    arch = P_(LIE, K.spread("right"), K.spread("left"), {"body": [-66, 0, 0], "head": [-20, 0, 0], "right_arm": [-10, 0, 80],
                                                        "left_arm": [-10, 0, -80], "right_forearm": [0, 0, 0], "left_forearm": [0, 0, 0],
                                                        "@body": [0, 1.0, 0]})
    c.key(0, P_(LIE, GRIPPING))
    c.key(0.6, P_(LIE, GRIPPING, {"body": [-72, 0, 0], "head": [50, 0, 0]}), E_IO)
    c.key(h("glow"), arch, E_BACK)
    for i in range(1, 5):
        j = K.jit("kelly-glow", i, 2)
        c.key(h("glow") + 0.35 * i, P_(arch, {"head": [-20 + j[0], j[1], j[2]], "body": [-66 + j[2], 0, 0]}))
    c.key(L("glow"), P_(arch, {"head": [-24, 0, 0]}), E_IO)
    # Fade (40): she sinks back and lies still.
    c = clip("fade", hold=True)
    still = P_(LIE, K.relax("right"), K.relax("left"), {"body": [-88, 0, 0], "head": [10, 18, 0], "right_arm": [10, 0, 26],
                                                      "left_arm": [10, 0, -20], "right_forearm": [-10, 0, 0], "left_forearm": [-30, 0, 0],
                                                      "right_leg": [-90, 0, 4], "right_shin": [6, 0, 0], "@body": [0, 0, 0]})
    c.key(0, arch)
    c.key(1.0, P_(arch, {"body": [-76, 0, 0], "head": [0, 0, 0], "right_arm": [-10, 0, 50], "left_arm": [-10, 0, -50]}), E_IO)
    c.key(h("fade"), still, E_IO)
    c.key(L("fade"), still)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Rourke
# =====================================================================================================

RB = Body(HIP=13.0, NECK=25.2, TW=4.1, TD=2.25, AW=3.5, SY=24.4, ELBOW=19.4, WRIST=14.7, PALM=12.8, LX=2.0, LW=3.8, KNEE=6.9,
          SHOE_H=1.3)
R_SKIN = Ramp("#5a403a", "#88665a", "#ad8a7a", "#c4a090", "#d6b6a6", "#e6ccbe")
R_HAIR = Ramp("#5a5a5c", "#7a7a7c", "#9a9a9c", "#b6b6b8", "#cecece", "#e6e6e6")
NAVY = Ramp("#06080f", "#0b1018", "#121a26", "#1a2434", "#243044", "#324058")
SHIRT = Ramp("#8f9298", "#b3b6bc", "#cdd0d5", "#dfe2e6", "#eceef1", "#f9fafb")
RED = Ramp("#3a0408", "#5a080e", "#7a0e16", "#98161e", "#b02028", "#c43034")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
R_LIPS = Ramp("#4a2a28", "#62383a", "#7a4a48", "#8e5a58", "#a26a66", "#b47c76")
EYE_HOT, EYE, EYE_DIM = hexc("#ffd0a0"), hexc("#ff3020"), hexc("#801010")

R_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhsssssssshhhh",
    "hssssjjjjjjssssh",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "sUUuussssssuuUUs",
    "kkwIIwsssswIIwkk",
    "sskUussssssuUkss",
    "skssssnnssssssks",
    "sjsssnnnnssssjss",
    "sjsssNnnNssssjss",
    "jssjssssssssjssj",
    "jsjslLMMMMLlsjsj",
    "sjssjssssssjssjs",
    "ssjjjjjjjjjjjjss",
]
R_FACE = [r[:16] for r in R_FACE]
assert all(len(r) == 16 for r in R_FACE), [len(r) for r in R_FACE]


def rourke_rig():
    m = Model(ROURKE, 256, 256)
    skeleton(m, RB, coat=False)
    seed = 47501
    sk = skin_m(R_SKIN, seed, 3.1, 0.35)
    hr = hair_m(R_HAIR, seed + 1, base=3.0)
    combed = lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.1) if (f == "up" and x % 3 == 0) else hr(f, x, y, w, h)
    lg = K.legend(sk, combed, hexc("#d8d0c6"), hexc("#3a4a5a"), R_LIPS, R_SKIN, brow=R_HAIR[1])
    sides = H.head_sides(sk, combed, top=3, back=12, temple=8, sideburn=5)
    head(m, RB, R_FACE, lg, sides, nose=lambda f, x, y, w, h: R_SKIN[4] if f == "north" else R_SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 0 else combed(f, x, y, w, h), shell_inflate=0.25, shell_rows=2.0)
    K.brows(m, RB, R_HAIR[1], tilt=-4, w=2.4, h=0.5)
    eye_shell(m, RB, "dRHRd..dRHRd", {"d": EYE_DIM, "R": EYE, "H": EYE_HOT})
    neck(m, RB, sk, 3.6)
    shirt = fabric(SHIRT, seed + 2, 3.6, 0.2)
    tie = lambda f, x, y, w, h: tone(RED, 3.2 + (0.6 if (x + y) % 4 == 0 else 0) - 0.3 * y / max(1, h))
    navy = fabric(NAVY, seed + 3, 3.0, 0.3)
    torso(m, RB, lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north" else NAVY[1], tag="torso")
    front = H.jacket_front(navy, NAVY[4], shirt, tie=tie, gap=(5, 11), v_depth=11, buttons=NAVY[0], button_rows=(13, 17))
    torso(m, RB, front, inflate=0.3, bottom=RB.HIP - 1.0, top=RB.NECK - 0.2, tag="jacket", faces_off=("down",))
    for s in (-1, 1):
        m.cube("body", (s * 1.0 - 0.8, RB.NECK - 1.3, -RB.TD - 0.45), (1.6, 1.3, 0.4), lambda f, x, y, w, h: tone(SHIRT, 4.6 - y * 0.6),
               density=2, tag="shirt_collar", rotation=(10, 0, -24 * s), pivot=(s * 1.0, RB.NECK - 0.1, -RB.TD - 0.3))
    # The flag pin on his left lapel.
    flag = ["rwrw", "bbrw", "bbrw"]
    m.cube("body", (2.4, RB.NECK - 4.6, -RB.TD - 0.65), (1.0, 0.75, 0.2),
           lambda f, x, y, w, h: {"r": hexc("#c02028"), "w": hexc("#f0f0f0"), "b": hexc("#203070")}[flag[min(2, y)][min(3, x)]] if f == "north"
           else hexc("#a08040"), density=4, tag="flag_pin")
    cuff = lambda f, x, y, w, h: tone(SHIRT, 4.2 - y * 0.5) if f not in ("up", "down") else SHIRT[1]
    arms(m, RB, fabric(NAVY, seed + 4, 2.9, 0.3), skin_m(R_SKIN, seed + 5, 3.1, 0.35), cuff=cuff, cuff_depth=0.6)
    legs(m, RB, fabric(NAVY, seed + 6, 2.9, 0.25, crease=True), SHOE)
    return m


R_IDLE = P_(K.relax("right"), K.relax("left"), {"right_arm": [-4, 0, 4], "left_arm": [-4, 0, -4], "right_forearm": [-12, 0, 0],
                                                "left_forearm": [-12, 0, 0], "head": [-2, 0, 0]})


def rourke_anims():
    f = AnimFile()
    clips = []
    A = f"animation.{ROURKE}."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("eyes",)))
        return clips[-1]

    c = clip("idle", 4.0, loop=True)
    K.breathe(c, R_IDLE, 4.0, head_turn=6)
    c = clip("walk", 1.2, loop=True)
    K.walk(c, R_IDLE, 1.2, stride=18, arm=10, bob=0.4, twist=4, knee=(6, 16))
    # Reach: a hand held out to the newborn, palm up, leaning in; he holds it there.
    c = clip("reach", 1.5, hold=True)
    reach = P_(K.spread("right"), K.relax("left"), {"right_arm": [-70, 0, 4], "right_forearm": [-20, 0, 0], "right_hand": [0, 90, 0],
                                                   "body": [16, 0, 0], "head": [14, 0, 0], "@root": [0, -0.4, -0.6],
                                                   "right_leg": [-14, 0, 0], "left_leg": [10, 0, 0]})
    c.key(0, R_IDLE)
    c.key(0.6, P_(reach, {"right_arm": [-60, 0, 4], "body": [8, 0, 0]}), E_IO)
    c.key(1.2, reach, E_IO)
    c.key(1.5, reach)
    # Thrown: hurled back by the light -- arms flung up before his face, he flies back and lands on his back.
    c = clip("thrown", 1.5, hold=True)
    blast = P_(K.spread("right"), K.spread("left"), {"right_arm": [-120, 0, 30], "left_arm": [-120, 0, -30], "right_forearm": [-60, 0, 0],
                                                     "left_forearm": [-60, 0, 0], "body": [-20, 0, 0], "head": [-24, 0, 0], "@root": [0, 1.0, 4.0]})
    c.key(0, reach)
    c.key(0.15, blast, E_SNAP)
    c.key(0.5, P_(blast, {"root": [50, 0, 0], "@root": [0, 3.0, 14.0]}), E_OUT)
    down = {"root": [88, 0, 0], "@root": [0, 2.2, 22.0], "head": [20, 10, 0], "right_arm": [-20, 0, 50], "left_arm": [-30, 0, -40],
            "right_leg": [-30, 0, 4], "right_shin": [40, 0, 0], "left_leg": [-6, 0, -4]}
    c.key(0.9, down, E_IN)
    c.key(1.05, P_(down, {"@root": [0, 2.8, 22.6]}), E_OUT)
    c.key(1.5, down, E_IO)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Output
# =====================================================================================================

def build():
    km = kelly_rig()
    kt, _ = km.build(gutter=1, seed=47000)
    kf, kclips = kelly_anims()
    K.verify_contract(km, kf, C, prefix=f"animation.{KELLY}.")
    rm = rourke_rig()
    rt, rg = rm.build(gutter=1, seed=47500)
    rf, rclips = rourke_anims()
    rc = C.lst("ROURKE_CLIPS")
    K.verify(rm, rf, f"animation.{ROURKE}.", rc, ("idle", "walk"), None, ("eyes",), C.bones, 20)
    return (km, kt, kf, kclips), (rm, rt, rg, rf, rclips)


def generate():
    (km, kt, kf, _), (rm, rt, rg, rf, _) = build()
    K.write_entity(km, kf, KELLY, kt)
    K.write_entity(rm, rf, ROURKE, rt, rg)


def preview(out_dir, scale=5):
    (km, kt, kf, kc), (rm, rt, rg, rf, rc) = build()
    p = K.render(km, kt, kc, out_dir, KELLY, [("idle", 0.0), ("labor", 0.5), ("glow", 1.2), ("fade", 4.0)], scale=scale,
                 views=("front", "three_quarter", "side"))
    p += K.render(rm, rt, rc, out_dir, ROURKE, [("idle", 0.0), ("reach", 1.4), ("thrown", 0.4)], scale=scale,
                  views=("front", "three_quarter", "side"))
    return p


if __name__ == "__main__":
    generate()
