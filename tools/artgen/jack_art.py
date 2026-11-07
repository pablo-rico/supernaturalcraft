"""Jack Kline, the nephilim (v0.41, seasons 13-14): one rig for the companion (`jack`) and the boss (`soulless_jack`), two
textures and glowmasks, the clips (names and timings parsed from JackAssets) and soulless Jack's trophy bust
(`block/soulless_jack_trophy`). His bar (`soulless_jack_bar`) is in s1214_gui_art.

A young man of about eighteen (32 px to the crown): a boyish face, dark brown slightly shaggy hair (a loose fringe,
`hair_fringe`); a tan canvas jacket, open, to the hip (its fronts `jacket_r`, `jacket_l` swing), over a grey hoodie (its hood
lies at the back of his neck, `hood`), a plain dark t-shirt at the throat, jeans, brown boots. His eyes glow gold when his
power shows (`eyes`, a lit shell); a shell of gold light can wrap him (`aura`, lit; the renderer shows it).

Soulless (`soulless_jack.png`): the same rig, a colder palette (the jacket a grey khaki, the hoodie a cold slate, the jeans
near black), the face set (no smile, the brows drawn), the eyes painted gold into the face itself and lit in its glowmask
along with the aura (always on).

No clip keys eyes, aura or hood.
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

NAME = "jack"
SOULLESS = "soulless_jack"
A = "animation.jack."
C = K.Contract("nephilim/jack/JackAssets")

B = Body(HIP=12.4, NECK=24.0, TW=3.75, TD=1.95, AW=3.2, SY=23.3, ELBOW=18.5, WRIST=14.0, PALM=12.1, LX=1.9, LW=3.5, KNEE=6.5,
         SHOE_H=1.4)

SKIN = Ramp("#5e4640", "#8c6c60", "#b28f80", "#caa898", "#dcbfb0", "#ecd6c8")
HAIR = Ramp("#120a06", "#1e130b", "#2c1c11", "#3a2618", "#4a3220", "#5c402a")
GOLD_HOT, GOLD, GOLD_DIM = hexc("#fff6c8"), hexc("#ffd24a"), hexc("#a8781a")

LOOKS = {
    NAME: dict(jacket=Ramp("#3a2a14", "#56401e", "#735828", "#8c6e36", "#a48446", "#bb9a5a"),
               hoodie=Ramp("#2a2a2e", "#3c3c42", "#505058", "#64646c", "#787880", "#8e8e96"),
               tee=Ramp("#0e0e12", "#16161c", "#202028", "#2a2a34", "#363642", "#444452"),
               jeans=Ramp("#0f1622", "#1b2638", "#283850", "#374b68", "#4a6283", "#62799b"),
               boot=Ramp("#1a0f08", "#2a180c", "#3c2412", "#4e3018", "#623e22", "#7a5030"),
               lips=Ramp("#4c2a2a", "#6a3a3a", "#875048", "#9d6058", "#b07068", "#c27c74"), soulless=False),
    SOULLESS: dict(jacket=Ramp("#1e1e18", "#2c2c24", "#3c3b30", "#4c4a3c", "#5c5a4a", "#6e6c5a"),
                   hoodie=Ramp("#14181e", "#1e232b", "#29303a", "#343c48", "#414a58", "#505a6a"),
                   tee=Ramp("#050507", "#0a0a0d", "#101014", "#17171c", "#202026", "#2a2a32"),
                   jeans=Ramp("#07090e", "#0d1118", "#141a24", "#1c2430", "#252f3e", "#303c4e"),
                   boot=Ramp("#0a0806", "#120e0b", "#1c1612", "#261e18", "#32281f", "#40342a"),
                   lips=Ramp("#3e2a2c", "#523638", "#664446", "#785254", "#886062", "#986e70"), soulless=True),
}

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hshhhsshhhhshhsh",
    "ssssssssssssssss",
    "ssbbbbssssbbbbss",
    "ssuuussssssuuuss",
    "suwIIwsssswIIwus",
    "sssUssssssssUsss",
    "ssssssnnssssssss",
    "ssssssnnssssssss",
    "sssssNnnNsssssss",
    "ssssssssssssssss",
    "sssslLMMMLlsssss",
    "ssssssssssssssss",
    "sssssjjjjjjsssss",
]
FACE_SOULLESS = [r for r in FACE]
FACE_SOULLESS[5] = "ssbbbbbssbbbbbss"           # the brows drawn down
FACE_SOULLESS[7] = "suwGGwsssswGGwus"           # gold eyes, painted in
FACE_SOULLESS[13] = "sssssllllllsssss"           # a flat line of a mouth
assert all(len(r) == 16 for r in FACE + FACE_SOULLESS)


def rig(look=NAME):
    v = LOOKS[look]
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=False)
    seed = 48001
    sk = skin_m(SKIN, seed, 3.2, 0.3)
    hr = hair_m(HAIR, seed + 1, base=2.7)
    shag = lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.18) if (x * 7 + y * 3) % 10 == 0 else hr(f, x, y, w, h)
    gold_eye = lambda f, x, y, w, h: GOLD
    lg = K.legend(sk, shag, hexc("#dcd4ca"), hexc("#4a6888"), v["lips"], SKIN, brow=HAIR[1], extra={"G": gold_eye})
    sides = H.head_sides(sk, shag, top=5, back=14, temple=6, sideburn=8)
    head(m, B, FACE_SOULLESS if v["soulless"] else FACE, lg, sides, nose=lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2],
         hair_shell=lambda f, x, y, w, h: None if f == "north" and y > 1 else shag(f, x, y, w, h), shell_inflate=0.4, shell_rows=3.0,
         glow=(lambda f, x, y, w, h, c: c if (v["soulless"] and c == GOLD) else None))
    K.brows(m, B, HAIR[1], tilt=(-8 if v["soulless"] else 4), w=2.2, h=0.4)
    eye_shell(m, B, "dGHGd..dGHGd", {"d": GOLD_DIM, "G": GOLD, "H": GOLD_HOT})
    neck(m, B, sk, 3.0)
    # The shaggy fringe falling over his forehead.
    fr = m.bone("hair_fringe", (0, B.crown - 0.5, -B.HEAD / 2), "head")

    def fringe(f, x, y, w, h):
        if f in ("north", "east", "west") and y == h - 1 and h01("fr", seed, x) < 0.5:
            return None
        return shag(f, x, y, w, h)
    m.cube(fr, (-4.2, B.crown - 2.6, -4.6), (8.4, 2.4, 1.0), fringe, density=2, tag="fringe", rotation=(-8, 0, 0),
           pivot=(0, B.crown - 0.5, -4.2))
    # The t-shirt at the throat, the hoodie, the hood at the back of his neck.
    tee = fabric(v["tee"], seed + 2, 3.0, 0.2)
    hood_c = fabric(v["hoodie"], seed + 3, 3.0, 0.3)

    def hoodie(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and y < 4 and abs(x - mid) < 2.4 - y * 0.4:
            return tee(f, x, y, w, h)
        if f == "north" and y < 9 and abs(x - mid) in (1.5, 2.5) and y > 2:
            return tone(v["hoodie"], 4.6) if y % 2 else tone(v["hoodie"], 1.0)     # the drawstrings
        if f == "north" and 14 <= y <= 18 and abs(x - mid) < 3.5:
            return shade(hood_c(f, x, y, w, h), 0.85 if y == 14 else 1.0)          # the kangaroo pocket
        return hood_c(f, x, y, w, h)
    torso(m, B, hoodie, tag="hoodie")
    hd = m.bone("hood", (0, B.NECK - 0.5, B.TD), "body")
    m.cube(hd, (-3.2, B.NECK - 2.4, B.TD - 0.2), (6.4, 3.4, 1.6), lambda f, x, y, w, h: tone(v["hoodie"], 3.2 - y * 0.3 + (0.6 if f == "up" else 0)),
           inflate=0.3, density=2, tag="hood")
    # The canvas jacket, open: the shell over shoulders and back, its lower fronts on their own bones.
    canvas = fabric(v["jacket"], seed + 4, 3.0, 0.35)

    def jkt(f, x, y, w, h):
        c = canvas(f, x, y, w, h)
        if h01("seam", seed, f, y) < 0.06:
            c = shade(c, 0.88)
        return c
    torso(m, B, open_front(jkt, half=2.0, collar=3, lapel=v["jacket"][4]), inflate=0.4, bottom=B.HIP + 2.2, top=B.NECK - 0.2, tag="jacket")
    for nm, s in (("jacket_r", -1), ("jacket_l", 1)):
        jb = m.bone(nm, (s * 2.4, B.HIP + 2.4, -B.TD), "body")
        xa, xb = (-B.TW - 0.45, -1.3) if s < 0 else (1.3, B.TW + 0.45)

        def front(f, x, y, w, h, s=s):
            if f == "north" and 1 <= y <= 3 and 1 <= x <= w - 2:
                return shade(jkt(f, x, y, w, h), 0.8) if y == 1 else jkt(f, x, y, w, h)   # a flap pocket
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return v["jacket"][4]
            return jkt(f, x, y, w, h)
        m.cube(jb, (xa, B.HIP - 1.2, -B.TD - 0.45), (xb - xa, 3.6, 0.45), none_on(("up",), front), density=2, tag="jacket_front")
        m.cube(jb, (xa if s < 0 else xb - 0.45, B.HIP - 1.2, -B.TD - 0.2), (0.45, 3.6, 2 * B.TD + 0.6), none_on(("up",), jkt),
               density=2, tag="jacket_hip")
    # Collar of the jacket, turned down.
    m.cube("body", (-3.6, B.NECK - 1.6, 0.6), (7.2, 1.8, 1.8), lambda f, x, y, w, h: tone(v["jacket"], 3.8 - y * 0.5), inflate=0.3,
           density=2, tag="jacket_collar")
    cuff = lambda f, x, y, w, h: tone(v["hoodie"], 3.0 + (x % 2) * 0.5) if f not in ("up", "down") else v["hoodie"][1]
    arms(m, B, fabric(v["jacket"], seed + 5, 2.9, 0.35), skin_m(SKIN, seed + 6, 3.2, 0.3), cuff=cuff, cuff_depth=0.9, sleeve_inflate=0.4)

    def jeans(f, x, y, w, h):
        c = fabric(v["jeans"], seed + 7, 3.0, 0.3)(f, x, y, w, h)
        if f == "north" and (x + y) % 5 == 0:
            c = shade(c, 1.08)                      # the twill
        return c
    legs(m, B, jeans, v["boot"], boot_h=3.0, toe=1.0)
    # The aura: a soft halo of gold light round him -- two crossed planes, an ellipse of glow fading out from his outline.
    au = m.bone("aura", (0, 16, 0), "root")

    def halo(f, x, y, w, h):
        dx = (x + 0.5 - w / 2) / (w / 2)
        dy = (y + 0.5 - h / 2) / (h / 2)
        r = math.sqrt(dx * dx + dy * dy)
        if r > 1.0:
            return None
        a = (1 - r) ** 0.7 * (0.55 + 0.45 * r)              # brightest in a ring, soft at the centre and the rim
        n = h01("jh", f, x // 2, y // 3)
        c = mix(GOLD_DIM, GOLD_HOT, min(1.0, 0.3 + r * 0.7))
        a *= 0.75 + 0.5 * n
        return (c[0], c[1], c[2], max(0, min(160, int(150 * a)))) if a > 0.08 else None
    m.cube(au, (-9.0, -1.0, 0), (18.0, 36.0, 0), halo, glow=lambda f, x, y, w, h, c: c, faces=("north", "south"), density=1,
           tag="aura_ns", shading=False)
    m.cube(au, (0, -1.0, -9.0), (0, 36.0, 18.0), halo, glow=lambda f, x, y, w, h, c: c, faces=("east", "west"), density=1,
           tag="aura_ew", shading=False)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

# Hands in his jacket pockets, looking round.
POCKETS = P_(K.fist("right", 0.6), K.fist("left", 0.6), {"right_arm": [-8, 0, 8], "right_forearm": [-38, -16, 0], "right_hand": [0, 0, -10],
                                                        "left_arm": [-8, 0, -8], "left_forearm": [-38, 16, 0], "left_hand": [0, 0, 10],
                                                        "head": [-2, 0, 0]})
LOOSE = P_(K.relax("right"), K.relax("left"), {"right_arm": [-4, 0, 4], "left_arm": [-4, 0, -4], "right_forearm": [-10, 0, 0],
                                               "left_forearm": [-10, 0, 0]})
FLOAT_Y = 6.0


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    sway = lambda s: {"jacket_r": [-8 * s, 0, 0], "jacket_l": [8 * s, 0, 0], "hair_fringe": [3, 0, 0]}
    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 5.0, loop=True)
    for i, t in enumerate((0, 1.25, 2.5, 3.75, 5.0)):
        u = t / 5.0
        look = (0, 20, -6, -24, 0)[i]
        c.key(t, P_(POCKETS, {"head": [-2 + 3 * math.sin(u * 6.28), look, 3 * math.sin(u * 12.5)],
                              "@body": [0, 0.1 * math.sin(u * 12.5), 0], "body": [0, look * 0.15, 0]}), E_IO if i else None)
    c = clip("walk", 1.1, loop=True)
    K.walk(c, LOOSE, 1.1, stride=20, arm=14, bob=0.45, twist=5, knee=(6, 18), extra=lambda s: P_(sway(s), {"head": [-1, -3 * s, 2 * s]}))
    c = clip("run", 0.6, loop=True)
    run = P_(K.fist("right", 0.7), K.fist("left", 0.7), {"right_forearm": [-80, 0, 0], "left_forearm": [-80, 0, 0], "body": [12, 0, 0],
                                                         "head": [-10, 0, 0]})
    K.walk(c, run, 0.6, stride=40, arm=36, bob=0.8, twist=7, knee=(14, 70), extra=lambda s: {"jacket_r": [-20 * s - 10, 0, 0],
                                                                                            "jacket_l": [20 * s - 10, 0, 0]})
    c = clip("float_idle", 3.0, loop=True)
    hang = {"right_leg": [6, 0, 3], "left_leg": [0, 0, -3], "right_shin": [8, 0, 0], "left_shin": [14, 0, 0]}
    for i, t in enumerate((0, 0.75, 1.5, 2.25, 3.0)):
        b = math.sin(t / 3.0 * 2 * math.pi)
        c.key(t, P_(K.spread("right"), K.spread("left"), hang, {"@root": [0, FLOAT_Y + 0.5 * b, 0], "right_arm": [-8, 0, 22 + 2 * b],
                                                                "left_arm": [-8, 0, -22 - 2 * b], "right_forearm": [-10, 0, 0],
                                                                "left_forearm": [-10, 0, 0], "head": [-4, 0, 0],
                                                                "jacket_r": [-4 - 3 * b, 0, -4], "jacket_l": [-4 - 3 * b, 0, 4]}),
              E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Smite (10): a palm thrust -- the gold blast.
    c = clip("smite")
    draw = P_(LOOSE, K.spread("right"), {"right_arm": [-30, 0, 10], "right_forearm": [-100, 0, 0], "body": [0, 18, 0], "head": [-4, -10, 0]})
    push = P_(LOOSE, K.spread("right"), {"right_arm": [-90, 0, 0], "right_forearm": [0, 0, 0], "right_hand": [-70, 0, 0], "body": [6, -14, 0],
                                        "head": [-8, 10, 0], "@root": [0, 0, -0.8], "right_leg": [-10, 0, 0], "left_leg": [10, 0, 0]})
    c.key(0, LOOSE)
    c.key(0.32, draw, E_IO)
    c.key(h("smite"), push, E_SNAP)
    c.key(0.7, P_(push, {"right_hand": [-74, 0, 0]}), E_OUT)
    c.key(L("smite"), LOOSE, E_IO)

    # Wave aside (8): a backhand wave -- the telekinetic sweep.
    c = clip("wave_aside")
    cross = P_(LOOSE, K.spread("right"), {"right_arm": [-60, 0, -40], "right_forearm": [-50, -20, 0], "body": [0, 22, 0], "head": [-2, 10, 0]})
    wave = P_(LOOSE, K.spread("right"), {"right_arm": [-70, 0, 60], "right_forearm": [-6, 0, 0], "right_hand": [0, 0, 30], "body": [0, -20, 0],
                                        "head": [-4, -14, 0]})
    c.key(0, LOOSE)
    c.key(0.25, cross, E_IO)
    c.key(h("wave_aside"), wave, E_SNAP)
    c.key(0.55, P_(wave, {"right_arm": [-64, 0, 66]}), E_OUT)
    c.key(L("wave_aside"), LOOSE, E_IO)

    # Shield (6): both hands up, palms out -- the gold shell; held, then let down.
    c = clip("shield")
    up = P_(K.spread("right"), K.spread("left"), {"right_arm": [-100, 0, -14], "left_arm": [-100, 0, 14], "right_forearm": [-30, 0, 0],
                                                  "left_forearm": [-30, 0, 0], "right_hand": [-60, 0, 0], "left_hand": [-60, 0, 0],
                                                  "body": [4, 0, 0], "head": [-6, 0, 0], "@root": [0, -0.4, 0.4], "right_leg": [-10, 0, 0],
                                                  "left_leg": [12, 0, 0]})
    c.key(0, LOOSE)
    c.key(h("shield"), up, E_SNAP)
    c.key(0.5, P_(up, {"@root": [0, -0.6, 0.6]}), E_OUT)
    c.key(1.1, P_(up, {"@root": [0, -0.5, 0.5], "head": [-4, 0, 0]}), E_IO)
    c.key(L("shield"), LOOSE, E_IO)

    # Heal (14): a hand laid on someone's shoulder, head bowed.
    c = clip("heal")
    lay = P_(LOOSE, K.relax("right"), {"right_arm": [-80, 0, 10], "right_forearm": [-20, 0, 0], "right_hand": [40, 0, 0], "body": [10, 6, 0],
                                      "head": [20, 0, 0], "@root": [0, 0, -0.6]})
    c.key(0, LOOSE)
    c.key(0.5, P_(lay, {"right_arm": [-70, 0, 10], "head": [6, 0, 0]}), E_IO)
    c.key(h("heal"), lay, E_OUT)
    c.key(1.2, P_(lay, {"head": [24, 0, 0]}), E_IO)
    c.key(L("heal"), LOOSE, E_IO)

    # Burn grace (30): arms flung wide, head back, gold pouring out of him; held while it burns.
    c = clip("burn_grace")
    curl = P_(LOOSE, {"body": [24, 0, 0], "head": [20, 0, 0], "right_arm": [-40, 0, -20], "left_arm": [-40, 0, 20], "right_forearm": [-90, -20, 0],
                      "left_forearm": [-90, 20, 0], "@root": [0, -1.2, 0]}, K.fist("right"), K.fist("left"))
    flung = P_(K.spread("right"), K.spread("left"), {"body": [-18, 0, 0], "head": [-44, 0, 0], "right_arm": [-14, 0, 86], "left_arm": [-14, 0, -86],
                                                     "right_forearm": [0, 0, 0], "left_forearm": [0, 0, 0], "@root": [0, 1.0, 0],
                                                     "jacket_r": [-20, 0, -14], "jacket_l": [-20, 0, 14], "hair_fringe": [-10, 0, 0]})
    c.key(0, LOOSE)
    c.key(1.0, curl, E_IO)
    c.key(h("burn_grace"), flung, E_BACK)
    for i in range(1, 6):
        j = K.jit("jack-burn", i, 3)
        c.key(h("burn_grace") + 0.2 * i, P_(flung, {"head": [-44 + j[0], j[1], j[2]], "body": [-18 + j[2], 0, 0]}))
    c.key(2.7, P_(flung, {"head": [-40, 0, 0], "body": [-14, 0, 0]}), E_IO)
    c.key(L("burn_grace"), P_(LOOSE, {"body": [14, 0, 0], "head": [20, 0, 0], "@root": [0, -0.8, 0]}), E_IO)

    # Snap (10): a flick of the fingers -- something unmade.
    c = clip("snap")
    raise_ = P_(LOOSE, {"right_arm": [-70, 0, 20], "right_forearm": [-60, -20, 0], "right_fingers": [60, 0, 0], "right_middle": [-10, 0, 0],
                        "right_index": [50, 0, 0], "right_thumb": [-10, 0, 40], "head": [-4, 0, 0]})
    snapped = P_(raise_, {"right_middle": [90, 0, 0], "right_thumb": [-30, 0, 10], "right_hand": [0, 0, -14], "right_arm": [-74, 0, 18]})
    c.key(0, LOOSE)
    c.key(0.35, raise_, E_IO)
    c.key(h("snap"), snapped, E_SNAP)
    c.key(0.7, snapped, E_OUT)
    c.key(L("snap"), LOOSE, E_IO)

    # Nova (30): curled up tight in the air, then flung open -- the burst.
    c = clip("nova")
    tuck = P_(K.fist("right"), K.fist("left"), {"body": [34, 0, 0], "head": [30, 0, 0], "right_arm": [-60, 0, -24], "left_arm": [-60, 0, 24],
                                                "right_forearm": [-110, -20, 0], "left_forearm": [-110, 20, 0], "right_leg": [-60, 0, 0],
                                                "left_leg": [-60, 0, 0], "right_shin": [100, 0, 0], "left_shin": [100, 0, 0], "@root": [0, 3.0, 0]})
    burst = P_(K.spread("right"), K.spread("left"), {"body": [-20, 0, 0], "head": [-36, 0, 0], "right_arm": [-30, 0, 110], "left_arm": [-30, 0, -110],
                                                     "right_leg": [6, 0, 18], "left_leg": [6, 0, -18], "@root": [0, 4.0, 0],
                                                     "jacket_r": [-30, 0, -20], "jacket_l": [-30, 0, 20]})
    c.key(0, LOOSE)
    c.key(1.1, tuck, E_IO)
    c.key(1.35, P_(tuck, {"body": [38, 0, 0], "@root": [0, 3.4, 0]}), E_IN)
    c.key(h("nova"), burst, E_SNAP)
    c.key(1.9, P_(burst, {"head": [-40, 0, 0]}), E_OUT)
    c.key(L("nova"), LOOSE, E_IO)

    # Talk (4): a small shrug and a look.
    c = clip("talk")
    shrug = P_(POCKETS, {"right_arm": [-10, 0, 12], "left_arm": [-10, 0, -12], "@body": [0, 0.4, 0], "head": [0, 0, 8]})
    c.key(0, POCKETS)
    c.key(h("talk"), shrug, E_OUT)
    c.key(0.5, P_(shrug, {"@body": [0, 0, 0]}), E_IO)
    c.key(1.0, P_(POCKETS, {"head": [-4, 14, -4]}), E_IO)
    c.key(L("talk"), POCKETS, E_IO)

    # Hurt (2).
    c = clip("hurt")
    K.recoil(c, LOOSE, L("hurt"), h("hurt"), {"body": [-12, 10, 0], "head": [-18, 14, 0], "@root": [0, 0, 1.0], "right_arm": [-30, 0, 30],
                                              "left_arm": [-20, 0, -24], "jacket_r": [-10, 0, 0], "jacket_l": [-10, 0, 0]})

    # Appear (10): a gold flash -- he straightens out of a crouch.
    c = clip("appear")
    crouch = P_(LOOSE, {"@root": [0, -3.6, 0], "right_leg": [-60, 0, 4], "left_leg": [-40, 0, -4], "right_shin": [80, 0, 0],
                        "left_shin": [70, 0, 0], "body": [26, 0, 0], "head": [-20, 0, 0], "right_arm": [-20, 0, 20], "left_arm": [-20, 0, -20]})
    c.key(0, crouch)
    c.key(h("appear"), P_(LOOSE, {"@root": [0, 0.3, 0], "body": [-4, 0, 0], "head": [-6, 0, 0]}), E_OUT)
    c.key(L("appear"), LOOSE, E_IO)

    # Leave (10): he turns away.
    c = clip("leave")
    c.key(0, LOOSE)
    c.key(h("leave"), P_(LOOSE, {"head": [4, 40, 0], "body": [0, 40, 0]}), E_IO)
    c.key(L("leave"), P_(LOOSE, {"head": [4, 60, 0], "body": [0, 80, 0], "right_leg": [-12, 0, 0], "left_leg": [10, 0, 0]}), E_IO)

    # Death (50): the gold goes out of his eyes; he sways, drops, and lies still (the tar takes him: the code's FX).
    c = clip("death", hold=True)
    struck = P_(LOOSE, {"body": [-10, 0, 0], "head": [-24, 0, 0], "right_arm": [-20, 0, 30], "left_arm": [-20, 0, -30], "@root": [0, 0, 0.6]})
    c.key(0, LOOSE)
    c.key(0.3, struck, E_SNAP)
    for i in range(1, 6):
        j = K.jit("jack-death", i, 3)
        c.key(0.3 + 0.35 * i, P_(struck, {"head": [-24 + 5 * i + j[0], j[1], j[2]], "body": [-10 + 4 * i + j[2], j[0], 0]}))
    knees = P_(LOOSE, {"@root": [0, -5.8, 0.4], "right_leg": [-90, 0, 4], "left_leg": [-90, 0, -4], "right_shin": [90, 0, 0],
                       "left_shin": [90, 0, 0], "body": [16, 0, 0], "head": [30, 0, 0], "right_arm": [-6, 0, 6], "left_arm": [-6, 0, -6]})
    c.key(h("death") - 0.2, knees, E_IN)
    c.key(h("death") + 0.4, P_(knees, {"body": [24, 0, 0], "head": [36, 0, 0]}), E_IO)
    c.key(3.4, K.lie(side=1, lift=3.0), E_IN)
    c.key(L("death"), K.lie(side=1, lift=3.0))

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


# =====================================================================================================
# Soulless Jack's bust
# =====================================================================================================

def bust():
    v = LOOKS[SOULLESS]
    skin = lambda x, y: tone(SKIN, 3.3 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.4 * y / 15)
    hair = lambda x, y: tone(HAIR, 2.7 + 0.6 * math.sin(x * 1.7 + y * 0.9) + (0.8 if (x * 7 + y * 3) % 10 == 0 else 0))
    lg = {"s": skin, "h": hair, "b": HAIR[1], "U": lambda x, y: shade(skin(x, y), 0.8), "u": lambda x, y: shade(skin(x, y), 0.86),
          "k": lambda x, y: shade(skin(x, y), 0.82), "w": hexc("#dcd4ca"), "I": hexc("#4a6888"), "G": GOLD, "n": SKIN[4], "N": SKIN[1],
          "l": v["lips"][1], "L": v["lips"][3], "M": v["lips"][4], "j": lambda x, y: shade(skin(x, y), 0.9)}

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 1.6 and y < 3:
            return tone(v["tee"], 3.0)
        if d < 3.2 and y < 13:
            return tone(v["hoodie"], 3.0 - y / 14) if not (d in (1.5, 2.5) and 2 < y < 8) else v["hoodie"][5]
        return tone(v["jacket"], 3.0 - y / 14 + 0.3 * math.sin(x * 1.1 + y * 0.3))
    coat = lambda x, y: tone(v["jacket"], 3.0 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4))
    import blockmodel as BM
    extra = [BM.cube("fringe", (5.7, 12.4, 5.45), (10.3, 13.4, 6.0), {"*": "hair"}),
             BM.cube("hood", (6.0, 7.2, 9.6), (10.0, 9.0, 10.6), {"*": "hood"})]
    K.bust("soulless_jack", K.face_fn(FACE_SOULLESS, lg, skin), hair, skin, chest, coat, extra=extra,
           extra_tex={"hood": lambda x, y: tone(v["hoodie"], 3.0 - y / 16)}, shoulders_w=7.6, head_w=4.2)


def build():
    out = {}
    sig = None
    for look in (NAME, SOULLESS):
        m = rig(look)
        t, g = m.build(gutter=1, seed=48000)
        s = [(c.box, c.size) for _, c in m.rig.cubes()]
        assert sig is None or s == sig, "jack and soulless_jack must share one atlas layout"
        sig = s
        out[look] = (m, t, g)
    f, clips = anims()
    K.verify_contract(out[NAME][0], f, C)
    return out, f, clips


def generate():
    from common import save
    out, f, _ = build()
    m, t, g = out[NAME]
    K.write_entity(m, f, NAME, t, g)
    _, t2, g2 = out[SOULLESS]
    save(t2, "entity", SOULLESS)
    save(g2, "entity", SOULLESS + "_glowmask")
    bust()


def preview(out_dir, scale=5):
    out, f, clips = build()
    p = K.render(out[NAME][0], out[NAME][1], clips, out_dir, NAME, [("idle", 0.0), ("smite", 0.5), ("burn_grace", 1.6)], scale=scale,
                 hidden=("aura",), views=("front", "three_quarter", "side"))
    p += K.render(out[SOULLESS][0], out[SOULLESS][1], clips, out_dir, SOULLESS, [("float_idle", 0.0), ("nova", 1.5), ("death", 4.0)],
                  scale=scale, views=("front", "three_quarter", "side"))
    return p


if __name__ == "__main__":
    generate()
