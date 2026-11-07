"""The Apocalypse World's people (v0.41): Michael's soldiers (`au_angel_soldier`, variants `_0.._3`, the base texture = variant 0,
each with its glowmask) and the camps' prisoners (`au_prisoner`, variants `_0.._3`, the base = variant 0). Clips parsed from
AuSoldierAssets.

Soldier (32 px to the crown, a vessel in uniform): grey-black military fatigues (a jacket with breast pockets, the trousers
bloused into black boots), a gold armband on the left arm (`armband`), a dark helmet with a chin strap (`helmet`), an angel
blade in the right fist (`blade`). Their eyes glow white when they smite (`eyes`, lit). The four vessels differ in face, skin
and hair only.

Prisoner (the vanilla player's size): ragged survivors -- torn, dirty shirts and jackets, patched trousers, worn boots, grime
and bruises; four of them (a bearded man, a young woman, an older man, a woman in a knit cap). Clips: idle (huddled, loop),
walk (loop), cower (loop: crouched, arms over the head), cheer (once: freed, arms up).

No clip keys helmet, blade, armband or eyes.
"""

import math

import horsemen_art as H
import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import (Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, eye_shell, glove_m, hair_m, head, legs, neck, skeleton,
                         skin_m, tone, torso)
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import Clip, P_, fabric
from pixelkit import Ramp, hexc, mix, shade

SOLDIER = "au_angel_soldier"
PRISONER = "au_prisoner"
C = K.Contract("apocworld/army/AuSoldierAssets")

B = Body(HIP=12.4, NECK=24.0, TW=3.9, TD=2.05, AW=3.4, SY=23.3, ELBOW=18.5, WRIST=14.0, PALM=12.1, LX=1.95, LW=3.6, KNEE=6.5,
         SHOE_H=1.4)

FATIGUE = Ramp("#14161a", "#1e2126", "#292c32", "#34383f", "#41464e", "#51575f")
HELMET = Ramp("#0a0b0c", "#121315", "#1b1d20", "#25282c", "#31353a", "#454a51")
BOOT = Ramp("#030303", "#08080a", "#0f0f12", "#18181c", "#25252b", "#4a4a55")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
SILVER = Ramp("#5d636b", "#868d96", "#aab1ba", "#c8ced6", "#e0e5ea", "#f7f9fb")
GRIP = Ramp("#14161c", "#1d2029", "#272b37", "#333847", "#414759", "#535a6e")
WHITE_HOT, WHITE, WHITE_DIM = hexc("#ffffff"), hexc("#e8f0ff"), hexc("#8a9ab0")

SKINS = [Ramp("#55403a", "#86685e", "#ac8c80", "#c5a596", "#d6bbad", "#e8d2c6"),
         Ramp("#2a1a12", "#43291d", "#5c3b2a", "#744b37", "#8a5c45", "#a06e55"),
         Ramp("#4e3a28", "#7a5a40", "#9e7a58", "#b8926c", "#cca882", "#dec09c"),
         Ramp("#5a4238", "#8a685a", "#ae8a7a", "#c6a290", "#d8b8a6", "#e8cebe")]
HAIRS = [Ramp("#1a1410", "#2a2018", "#3a2c22", "#4a3a2c", "#5c4838", "#705a46"),
         Ramp("#050403", "#0c0907", "#15100c", "#1f1812", "#2a2119", "#372c22"),
         Ramp("#2a1a0a", "#3e2810", "#563818", "#6c4820", "#845a2a", "#9c6e36"),
         Ramp("#6a5a40", "#8a7650", "#aa9464", "#c4ae7a", "#d8c492", "#e8d8ac")]
LIPS = Ramp("#4a2e28", "#62403a", "#7a524a", "#8f6359", "#a27468", "#b48678")

SOLDIER_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhsssssssssssshh",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "ssUUssssssssUUss",
    "skwIIwsssswIIwks",
    "ssssssssssssssss",
    "ssssssnnssssssss",
    "ssssssnnssssssss",
    "sjsssNnnNssssjss",
    "sjssssssssssssjs",
    "ssjsslllllllsjss",
    "sssjssssssssjsss",
    "ssssjjjjjjjjssss",
]
assert all(len(r) == 16 for r in SOLDIER_FACE)


def fatigue(seed, base=3.0):
    cl = fabric(FATIGUE, seed, base, 0.35)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        if h01("ft", seed, f, x // 2, y // 2) < 0.12:
            c = shade(c, 1.1)
        return c
    return m


def soldier_rig(v=0):
    m = Model(SOLDIER, 256, 128)
    skeleton(m, B, coat=False)
    seed = 51000 + v * 31
    sk = skin_m(SKINS[v], seed, 3.0, 0.35)
    hr = hair_m(HAIRS[v], seed + 1, base=2.6)
    lg = K.legend(sk, hr, hexc("#d4cec6"), hexc(("#4a5a6a", "#1e140c", "#3a4a2a", "#5a6a7a")[v]), LIPS, SKINS[v], brow=HAIRS[v][1])
    sides = H.head_sides(sk, hr, top=3, back=10, temple=8, sideburn=4)
    head(m, B, SOLDIER_FACE, lg, sides, nose=lambda f, x, y, w, h: SKINS[v][4] if f == "north" else SKINS[v][2])
    K.brows(m, B, HAIRS[v][1], tilt=-4 + 2 * v, w=2.3, h=0.45)
    eye_shell(m, B, "dWHWd..dWHWd", {"d": WHITE_DIM, "W": WHITE, "H": WHITE_HOT})
    neck(m, B, sk, 3.2)
    # The helmet: a dark shell over the crown and sides, a short brim, a chin strap.
    hm = m.bone("helmet", (0, B.crown, 0), "head")
    shell = lambda f, x, y, w, h: tone(HELMET, 2.6 + (1.0 if f == "up" else 0) + 0.4 * math.sin(x * 0.6) - 0.4 * y / max(1, h))
    m.cube(hm, (-4.5, B.crown - 3.2, -4.5), (9.0, 3.6, 9.0), none_on(("down",), shell), density=2, tag="helmet_shell")
    m.cube(hm, (-4.7, B.crown - 3.6, -5.0), (9.4, 0.6, 9.8), lambda f, x, y, w, h: tone(HELMET, 2.2), density=2, tag="helmet_brim")
    for s in (-1, 1):
        m.cube(hm, (s * 4.2 - 0.15, B.NECK + 0.4, -0.6), (0.3, 3.4, 0.6), solid(HELMET[1]), density=4, tag="chin_strap")
    # Fatigues: a jacket with breast pockets, the collar open.
    ft = fatigue(seed + 2)

    def jacket(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 3 and abs(x - mid) < 1.6 - y * 0.5:
            return sk(f, x, y, w, h)
        if f == "north" and 4 <= y <= 7 and (1 <= x <= 5 or w - 6 <= x <= w - 2):
            return shade(ft(f, x, y, w, h), 0.82 if y == 4 else 1.05)   # the breast pockets
        if f == "north" and abs(x - mid) < 0.6:
            return FATIGUE[1]
        if f in ("north", "south", "east", "west") and y in (h - 3, h - 2):
            return FATIGUE[0]                                           # the belt
        return ft(f, x, y, w, h)
    torso(m, B, jacket, inflate=0.2, tag="jacket")
    for s in (-1, 1):
        m.cube("body", (s * 1.4 - 0.9, B.NECK - 1.2, -B.TD - 0.4), (1.8, 1.2, 0.4), lambda f, x, y, w, h: tone(FATIGUE, 3.6 - y),
               density=2, tag="collar", rotation=(14, 0, -26 * s), pivot=(s * 1.4, B.NECK - 0.1, -B.TD - 0.3))
    arms(m, B, fatigue(seed + 3, 2.9), skin_m(SKINS[v], seed + 4, 3.0, 0.35), sleeve_inflate=0.25,
         cuff=lambda f, x, y, w, h: tone(FATIGUE, 2.0) if f not in ("up", "down") else FATIGUE[0], cuff_depth=0.8)
    legs(m, B, fatigue(seed + 5, 2.8), BOOT, boot_h=4.0, toe=1.0)
    # The gold armband round the left upper arm.
    ab = m.bone("armband", (B.AX, B.SY - 3.0, 0), "left_arm")
    m.cube(ab, (B.AX - B.AW / 2, B.SY - 4.6, -B.AW / 2), (B.AW, 1.4, B.AW),
           lambda f, x, y, w, h: GOLD[4] if y == 0 else (GOLD[2] if y == h - 1 else GOLD[3]), inflate=0.45, density=2, tag="armband")
    # The angel blade in the right fist, point forward.
    cx, y = -B.AX, B.PALM - 0.3
    b = m.bone("blade", (cx, y, 0), "right_hand", rotation=(32, 0, 0))
    m.cube(b, (cx - 0.5, y - 0.5, -0.6), (1, 1, 3.4), lambda f, xx, yy, w, h: GRIP[2 + ((xx + yy) % 3 == 0) * 2], density=2, tag="grip")
    m.cube(b, (cx - 0.7, y - 0.7, -1.1), (1.4, 1.4, 0.5), solid(SILVER[3]), density=2, tag="guard")
    steel = lambda f, xx, yy, w, h: SILVER[5] if f in ("north", "south", "up", "down") else (SILVER[4] if f == "west" else SILVER[2])
    m.cube(b, (cx - 0.5, y - 0.5, -9.0), (1, 1, 7.9), steel, H.glow_faint(0.2), density=2, tag="blade_steel", rotation=(0, 0, 45), pivot=(cx, y, 0))
    m.cube(b, (cx - 0.25, y - 0.25, -10.0), (0.5, 0.5, 1.0), solid(SILVER[5]), H.glow_faint(0.3), density=2, tag="tip",
           rotation=(0, 0, 45), pivot=(cx, y, 0))
    return m


GRIP_R = K.fist("right")
S_IDLE = P_(GRIP_R, K.fist("left", 0.5), {"right_arm": [-8, 0, 4], "right_forearm": [-24, 0, 0], "left_arm": [-2, 0, -4],
                                          "left_forearm": [-8, 0, 0], "head": [-2, 0, 0]})


def soldier_anims():
    f = AnimFile()
    clips = []
    A = f"animation.{SOLDIER}."

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    c = clip("idle", 3.0, loop=True)
    K.breathe(c, S_IDLE, 3.0, head_turn=6)
    c = clip("walk", 1.0, loop=True)
    K.walk(c, S_IDLE, 1.0, stride=24, arm=16, bob=0.45, twist=2, knee=(4, 20), extra=lambda s: {"head": [-2, 0, 0]})
    c = clip("run", 0.6, loop=True)
    K.walk(c, P_(S_IDLE, {"body": [12, 0, 0], "head": [-10, 0, 0], "right_forearm": [-70, 0, 0], "left_forearm": [-70, 0, 0]}), 0.6,
           stride=40, arm=34, bob=0.8, twist=6, knee=(14, 70))
    # Slash (6).
    c = clip("slash")
    up = P_(GRIP_R, {"right_arm": [-150, -20, 30], "right_forearm": [-40, 0, 0], "body": [-6, 20, 0], "head": [-6, -12, 0], "@root": [0, 0.2, 0.6]})
    down = P_(GRIP_R, {"right_arm": [-40, 20, -40], "right_forearm": [-20, 0, 0], "body": [16, -20, 0], "head": [4, 12, 0],
                       "right_leg": [-20, 0, 0], "left_leg": [12, 0, 0], "@root": [0, -0.8, -1.4]})
    c.key(0, S_IDLE)
    c.key(0.2, up, E_OUT)
    c.key(h("slash"), down, E_SNAP)
    c.key(0.45, P_(down, {"right_arm": [-36, 22, -44]}), E_OUT)
    c.key(L("slash"), S_IDLE, E_IO)
    # Smite (10): a palm laid on a face -- the light.
    c = clip("smite")
    reach = P_(S_IDLE, K.spread("left"), {"left_arm": [-60, 0, 10], "left_forearm": [-60, 0, 0], "body": [0, -10, 0]})
    palm = P_(S_IDLE, K.spread("left"), {"left_arm": [-100, 0, -4], "left_forearm": [-10, 0, 0], "left_hand": [-70, 0, 0], "body": [6, 12, 0],
                                        "head": [-12, -8, 0], "@root": [0, 0, -0.8]})
    c.key(0, S_IDLE)
    c.key(0.3, reach, E_IO)
    c.key(h("smite"), palm, E_SNAP)
    for i in range(1, 3):
        j = K.jit("aus-smite", i, 2)
        c.key(h("smite") + 0.12 * i, P_(palm, {"left_arm": [-100 + j[0], j[1], -4], "head": [-12 + j[2], -8, 0]}))
    c.key(L("smite"), S_IDLE, E_IO)
    # Death (20): the light pours from his eyes -- head back, arms out -- he falls.
    c = clip("death", hold=True)
    flare = P_(K.spread("right"), K.spread("left"), {"body": [-16, 0, 0], "head": [-44, 0, 0], "right_arm": [-14, 0, 60], "left_arm": [-14, 0, -60],
                                                     "@root": [0, 0.3, 0.4]})
    c.key(0, S_IDLE)
    c.key(0.3, flare, E_SNAP)
    c.key(0.8, P_(flare, {"head": [-48, 4, 0]}))
    lying = {"root": [88, 0, 0], "@root": [0, 2.0, 0], "head": [10, 20, 0], "right_arm": [-10, 0, 60], "left_arm": [-10, 0, -50],
             "right_leg": [0, 0, 6], "left_leg": [-20, 0, -4], "left_shin": [30, 0, 0]}
    c.key(h("death"), P_(GRIP_R, lying), E_IN)
    c.key(L("death"), P_(GRIP_R, lying))
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Prisoners
# =====================================================================================================

PB = Body()
PRISONERS = [
    dict(skin=SKINS[0], hair=HAIRS[0], beard=True, woman=False, cap=False,
         shirt=Ramp("#3a3228", "#524638", "#6a5c48", "#7e6e58", "#928268", "#a8987c"),
         trousers=Ramp("#14161c", "#1e212a", "#292d38", "#353a46", "#424856", "#525868")),
    dict(skin=SKINS[2], hair=HAIRS[2], beard=False, woman=True, cap=False,
         shirt=Ramp("#3a1e1a", "#552a24", "#6e3830", "#84463c", "#985648", "#ac6858"),
         trousers=Ramp("#1a1a14", "#26261e", "#33332a", "#404036", "#4e4e42", "#5e5e50")),
    dict(skin=SKINS[3], hair=Ramp("#5a5a5c", "#6e6e70", "#868688", "#9e9ea0", "#b6b6b8", "#cecece"), beard=True, woman=False, cap=False,
         shirt=Ramp("#1e2a30", "#2a3a42", "#384c56", "#465e6a", "#56707c", "#68848e"),
         trousers=Ramp("#20180e", "#2e2214", "#3e2e1c", "#4e3a24", "#5e482e", "#705838")),
    dict(skin=SKINS[1], hair=HAIRS[1], beard=False, woman=True, cap=True,
         shirt=Ramp("#262a1c", "#353a26", "#454c32", "#555e3e", "#66704c", "#78845c"),
         trousers=Ramp("#121212", "#1c1c1c", "#262626", "#313131", "#3e3e3e", "#4c4c4c")),
]
P_FACE_M = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssshhsshhh",
    "hsssssssssssssss",
    "sssssgssssssssss",
    "sbbbbbssssbbbbbs",
    "sUUUUssssssUUUUs",
    "skwIIwsssswIIwks",
    "skUUssssssssUUks",
    "ssssssnnssssssss",
    "dsssssnnssssssgd",
    "ddsssNnnNssssddd",
    "ddddddddddddddd d".replace(" ", "")[:16],
    "dddddlllllldddd d".replace(" ", "")[:16],
    "dddddddddddddddd",
    "sdddddddddddddds",
]
P_FACE_W = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhsssssssshhhh",
    "hhssssssssssssh h".replace(" ", "")[:16],
    "hhssssssgsssssh h".replace(" ", "")[:16],
    "hhsbbbssssbbbshh",
    "hhUUussssssuUUhh",
    "hhuwIIwsswIIwuhh",
    "hhsUUssssssUUshh",
    "hhsssssnnssssshh",
    "hhsssssnnssssshh",
    "hhssssNnnNsssshh",
    "hhgsssssssssssh h".replace(" ", "")[:16],
    "hhsssLMMMLssssh h".replace(" ", "")[:16],
    "hhhssssssssssshh",
    "hhhhsssssssshhhh",
]
P_FACE_M = [r[:16].ljust(16, "d") for r in P_FACE_M]
P_FACE_W = [r[:16].ljust(16, "h") for r in P_FACE_W]


def rag(ramp, seed, base=3.0):
    """Worn cloth: faded, stained, torn holes showing darker cloth beneath, patches."""
    def m(f, x, y, w, h):
        v = base + 0.4 * math.sin(x * 0.8 + y * 0.3) - 0.5 * y / max(1, h) + (0.6 if f == "up" else 0)
        n = h01("rag", seed, f, x // 2, y // 2)
        c = tone(ramp, v)
        if n < 0.08:
            c = shade(c, 0.55)                             # a tear
        elif n < 0.18:
            c = mix(c, hexc("#3a3024"), 0.4)               # grime
        elif n > 0.95:
            c = mix(c, hexc("#6a5a40"), 0.5)               # a patch
        return c
    return m


def prisoner_rig(v=0):
    p = PRISONERS[v]
    m = Model(PRISONER, 128, 128)
    skeleton(m, PB, coat=False)
    seed = 52000 + v * 37
    sk0 = skin_m(p["skin"], seed, 3.0, 0.4)
    sk = lambda f, x, y, w, h: mix(sk0(f, x, y, w, h), hexc("#3a3024"), 0.3) if h01("dirt", seed, f, x, y) < 0.12 else sk0(f, x, y, w, h)
    hr = hair_m(p["hair"], seed + 1, base=2.6)
    beard = (lambda f, x, y, w, h: tone(p["hair"], 2.2 + h01("bd", seed, f, x, y))) if p["beard"] else sk
    lg = K.legend(sk, hr, hexc("#cec8be"), hexc("#3a3024"), LIPS, p["skin"], brow=p["hair"][1],
                  extra={"d": beard, "g": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), hexc("#5a3a5a"), 0.45)})
    hat = Ramp("#3a1e14", "#52281a", "#6a3422", "#80402a", "#984e34", "#b05e40")

    def hair_or_cap(f, x, y, w, h):
        if p["cap"]:
            return tone(hat, 3.0 + (0.6 if (x + y) % 2 else 0))
        return hr(f, x, y, w, h)
    sides = H.head_sides(sk, hair_or_cap, top=4 if p["cap"] else 3, back=14 if p["woman"] else 11, temple=8,
                         sideburn=10 if p["woman"] else 5, beard=beard if p["beard"] else None)
    head(m, PB, P_FACE_W if p["woman"] else P_FACE_M, lg, sides,
         nose=lambda f, x, y, w, h: p["skin"][4] if f == "north" else p["skin"][2])
    neck(m, PB, sk, 3.2)
    torso(m, PB, rag(p["shirt"], seed + 2), inflate=0.2, tag="shirt")
    arms(m, PB, rag(p["shirt"], seed + 3, 2.8), sk, sleeve_inflate=0.25)
    legs(m, PB, rag(p["trousers"], seed + 4), Ramp("#140e0a", "#22180e", "#302214", "#3e2c1a", "#4e3822", "#60462c"), boot_h=2.6)
    return m


HUDDLE = P_(K.fist("right", 0.4), K.fist("left", 0.4), {"right_arm": [-30, 0, -14], "left_arm": [-30, 0, 14], "right_forearm": [-70, -40, 0],
                                                        "left_forearm": [-70, 40, 0], "body": [14, 0, 0], "head": [10, 0, 0], "@root": [0, -0.6, 0]})


def prisoner_anims():
    f = AnimFile()
    clips = []
    A = f"animation.{PRISONER}."

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    c = clip("idle", 4.0, loop=True)
    K.breathe(c, HUDDLE, 4.0, head_turn=10)
    c = clip("walk", 1.3, loop=True)
    K.walk(c, P_(HUDDLE, {"right_arm": [-10, 0, -4], "left_arm": [-10, 0, 4], "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0]}),
           1.3, stride=16, arm=6, bob=0.3, twist=3, knee=(8, 16), extra=lambda s: {"head": [8, -4 * s, 0]})
    c = clip("cower", 1.0, loop=True)
    crouch = P_(K.fist("right", 0.5), K.fist("left", 0.5), {"@root": [0, -4.6, 0], "right_leg": [-80, 0, 6], "left_leg": [-70, 0, -6],
                                                            "right_shin": [110, 0, 0], "left_shin": [100, 0, 0], "body": [36, 0, 0],
                                                            "head": [20, 0, 0], "right_arm": [-150, 0, -30], "left_arm": [-150, 0, 30],
                                                            "right_forearm": [-80, 0, 0], "left_forearm": [-80, 0, 0]})
    for i, t in enumerate((0, 0.25, 0.5, 0.75, 1.0)):
        j = K.jit("cower", i % 4, 2)
        c.key(t, P_(crouch, {"body": [36 + j[0], j[1], 0], "head": [20 + j[2], 0, 0]}), E_IO if i else None)
    c = clip("cheer", 2.0)
    up = P_(K.spread("right"), K.spread("left"), {"right_arm": [-170, 0, -20], "left_arm": [-170, 0, 20], "head": [-24, 0, 0], "body": [-6, 0, 0]})
    c.key(0, HUDDLE)
    c.key(0.35, P_(up, {"@root": [0, 1.2, 0]}), E_OUT)
    c.key(0.6, P_(up, {"@root": [0, 0, 0], "right_arm": [-160, 0, -30], "left_arm": [-160, 0, 30]}), E_IN)
    c.key(0.9, P_(up, {"@root": [0, 1.2, 0]}), E_OUT)
    c.key(1.2, P_(up, {"@root": [0, 0, 0]}), E_IN)
    c.key(2.0, P_(K.relax("right"), K.relax("left"), {"head": [-6, 0, 0]}), E_IO)
    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Output
# =====================================================================================================

def variants(fn, n, seed):
    out = []
    for v in range(n):
        m = fn(v)
        t, g = m.build(gutter=1, seed=seed)
        out.append((m, t, g))
    sig = [[(c.box, c.size) for _, c in m.rig.cubes()] for m, _, _ in out]
    assert all(s == sig[0] for s in sig), "variants must share one atlas layout"
    return out


def build():
    sol = variants(soldier_rig, 4, 51000)
    sf, sc = soldier_anims()
    K.verify_contract(sol[0][0], sf, C)
    pri = variants(prisoner_rig, 4, 52000)
    pf, pc = prisoner_anims()
    K.verify(pri[0][0], pf, f"animation.{PRISONER}.", C.lst("PRISONER_CLIPS"), ("idle", "walk", "cower"), None, (), C.bones, 0)
    return (sol, sf, sc), (pri, pf, pc)


def generate():
    (sol, sf, _), (pri, pf, _) = build()
    K.write_entity(sol[0][0], sf, SOLDIER, sol[0][1], sol[0][2])
    for i, (_, t, g) in enumerate(sol):
        save(t, "entity", f"{SOLDIER}_{i}")
        save(g, "entity", f"{SOLDIER}_{i}_glowmask")
    K.write_entity(pri[0][0], pf, PRISONER, pri[0][1])
    for i, (_, t, _) in enumerate(pri):
        save(t, "entity", f"{PRISONER}_{i}")


def preview(out_dir, scale=4):
    (sol, sf, sc), (pri, pf, pc) = build()
    p = []
    for i, (m, t, _) in enumerate(sol):
        p += K.render(m, t, sc, out_dir, f"{SOLDIER}_{i}", [("idle", 0.0)] if i else [("idle", 0.0), ("slash", 0.3), ("death", 1.5)], scale=scale,
                      views=("front", "three_quarter", "side"))
    for i, (m, t, _) in enumerate(pri):
        p += K.render(m, t, pc, out_dir, f"{PRISONER}_{i}", [("idle", 0.0)] if i else [("idle", 0.0), ("cower", 0.0), ("cheer", 0.35)], scale=scale,
                      views=("front", "three_quarter", "side"))
    return p


if __name__ == "__main__":
    generate()
