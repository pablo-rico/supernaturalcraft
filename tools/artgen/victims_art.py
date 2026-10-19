"""The witch's victims (v0.21): `hexed_villager`, a villager under a witch's curse -- GeckoLib rig, texture, glowmask and
clips (names in entity/witch/WitchAssets).

A vanilla villager's build (the tall head and the big nose, the robe to the shins) gone wrong: grey, sickly skin threaded
with black veins that run out from the hex; hollow, sunken eyes with a magenta pinprick in each; a witch's mark burned on
the brow and on the backs of the hands (magenta, glowmask); the robe filthy and torn at the hem. It stands hunched, the
head pushed forward, the arms hanging loose (it is not a vanilla villager: the arms are free, to claw).

Skeleton (Bedrock px; +X is its LEFT; it faces north):
  root -- body -- head -- jaw;  robe;  right_arm -- right_forearm,  left_arm -- left_forearm
       -- right_leg, left_leg
Clips: idle, walk (loops), attack (both arms rake down, tick 8), freed (the hex breaks: it slumps, shudders, then
straightens and blinks -- ends standing at rest).
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, tone, write_compact
from pixelkit import Ramp, hexc, mix, shade

NAME = "hexed_villager"
SKIN = Ramp("#33352f", "#4a4d45", "#62655b", "#787b70", "#8d9084", "#a3a598")
VEIN = hexc("#120c12")
MAGENTA = Ramp("#3d0630", "#6e0c55", "#a3187e", "#d22aa6", "#f05bc8", "#ffb0ea")
ROBE = Ramp("#1f170f", "#2f2318", "#413022", "#523e2d", "#644d39", "#775e47")
SASH = Ramp("#1c1416", "#2a1e21", "#3a2a2e", "#4a363b", "#5b4349", "#6d5158")
SHOE = Ramp("#120d09", "#1d1610", "#2a2018", "#372a20", "#45352a", "#544135")


def veins(seed, density=0.0):
    """Grey skin with black veins wandering out in short random walks."""
    cache = {}

    def m(f, x, y, w, h):
        key = (f, w, h)
        if key not in cache:
            pts = set()
            n = max(1, int(w * h * density / 12))
            for i in range(n):
                px, py = int(h01("vx", seed, f, i) * w), int(h01("vy", seed, f, i) * h)
                for k in range(int(4 + h01("vl", seed, f, i) * 6)):
                    pts.add((px, py))
                    r = h01("vs", seed, f, i, k)
                    px += -1 if r < 0.33 else (1 if r < 0.66 else 0)
                    py += 1 if h01("vd", seed, f, i, k) < 0.6 else -1
            cache[key] = pts
        n = h01("sk", seed, f, x, y)
        c = tone(SKIN, 3.0 + (n - 0.5) * 0.9 - 0.3 * y / max(1, h))
        if (x, y) in cache[key]:
            return mix(c, VEIN, 0.75)
        return c
    return m


def mark_glow(f, x, y, w, h, c):
    return MAGENTA[5] if c in (MAGENTA[3], MAGENTA[4]) else None


FACE = [
    "ssssssssssssssss",
    "sssssMssssMsssss",
    "ssssssMMMMssssss",
    "sssssMsMMsMsssss",
    "ssssssMMMMssssss",
    "ssssssssssssssss",
    "sssvsssssssssvss",
    "ssbbbbssssbbbbss",
    "sbkkkdssssdkkkbs",
    "sbkpkdssssdkpkbs",
    "sbbkkbssssbkkbbs",
    "ssbbbssssssbbbss",
    "ssssvssssssvssss",
    "sssssssssssssssss",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "ssssmkkkkkkmssss",
    "ssssssmmmmssssss",
    "ssssssssssssssss",
]


def rig():
    m = Model(NAME, 256, 128)
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 12, 0), "root")
    m.bone("head", (0, 24, 0), "body")
    m.bone("jaw", (0, 26.0, -3.0), "head")
    m.bone("robe", (0, 12, 0), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (s * 5.5, 22.5, 0), "body")
        m.bone(f"{side}_forearm", (s * 5.5, 17.5, 0), f"{side}_arm")
        m.bone(f"{side}_leg", (s * 2.0, 12, 0), "root")
    sk = veins(61001, 0.12)
    legend = {"s": sk, "M": MAGENTA[3], "v": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), VEIN, 0.7), "b": SKIN[1], "k": hexc("#0b080b"),
              "d": SKIN[2], "p": MAGENTA[4], "m": SKIN[1]}

    def head_m(f, x, y, w, h):
        if f == "north":
            rows = FACE
            ch = rows[y][x] if y < len(rows) and x < len(rows[y]) else "s"
            v = legend.get(ch, sk)
            return v(f, x, y, w, h) if callable(v) else v
        if f == "up":
            return tone(SKIN, 2.2 + h01("sc", f, x, y))                       # bald, blotched scalp
        return sk(f, x, y, w, h)
    # The villager's long head (8 x 10 x 8) and the big nose.
    m.cube("head", (-4, 24, -4), (8, 10, 8), head_m, mark_glow, density=2, tag="head")
    m.cube("head", (-1, 23, -6), (2, 4, 2), lambda f, x, y, w, h: tone(SKIN, 2.6 + 0.6 * (f == "north") - 0.3 * y / max(1, h)), density=2, tag="nose")
    # A slack jaw (it hangs open when it attacks).
    m.cube("jaw", (-2.5, 24.0, -4.2), (5.0, 1.6, 0.4), lambda f, x, y, w, h: SKIN[1] if f == "north" and y == 0 else sk(f, x, y, w, h), density=2,
           tag="jaw")
    # Robe: the shirt and the long over-robe, torn at the hem, a frayed sash.
    worn = lambda f, x, y, w, h: tone(ROBE, 2.8 + (h01("rb", f, x, y) - 0.5) * 1.2 - 0.5 * y / max(1, h) - (0.8 if h01("dirt", f, x // 2, y // 2) < 0.15 else 0))
    m.cube("body", (-4, 12, -3), (8, 12, 6), worn, density=2, tag="torso")

    def robe(f, x, y, w, h):
        if f == "up":
            return None
        if f != "down" and y >= h - 3 and h01("tear", f, x) < 0.35 + 0.2 * (y - (h - 3)):
            return None
        if f == "down":
            return None
        return worn(f, x, y + 4, w, h)
    m.cube("robe", (-4, 6, -3), (8, 18, 6), robe, inflate=0.5, density=2, tag="robe")
    m.cube("robe", (-4, 13, -3), (8, 1.2, 6), none_on(("up", "down"), lambda f, x, y, w, h: tone(SASH, 2.6 + (x % 3 == 0))), inflate=0.6,
           density=2, tag="sash")
    # Arms: sleeves to the elbow, grey forearms veined black, a hex mark on the back of each hand.
    for side, s in (("right", -1), ("left", 1)):
        cx = s * 6.0
        m.cube(f"{side}_arm", (cx - 2, 17.5, -2), (4, 7, 4), worn, density=2, inflate=0.2, tag="sleeve")
        def fore(f, x, y, w, h, s=s):
            c = sk(f, x, y, w, h)
            if f == ("west" if s < 0 else "east") and y >= h - 4 and abs(x - (w - 1) / 2) < 1.5 and (x + y) % 2 == 0:
                return MAGENTA[3]
            if f in ("north", "down") and y >= h - 2 and x % 2 == 0:
                return SKIN[1]                                                 # long, dirty nails
            return c
        m.cube(f"{side}_forearm", (cx - 1.8, 10.5, -1.8), (3.6, 7.0, 3.6), fore, mark_glow, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 2, 16.0, -2), (4, 1.5, 4), none_on(("up",), lambda f, x, y, w, h: tone(ROBE, 1.8 + (x % 2))), inflate=0.3,
               density=2, tag="ragged_cuff")
        m.cube(f"{side}_leg", (s * 2.0 - 2, 0, -2), (4, 12, 4), lambda f, x, y, w, h: tone(SHOE, 2.6 - (y == h - 1)) if y >= h - 3 else
               tone(SASH, 2.2 + (h01("tr", f, x, y) - 0.5)), density=2,
               tag="leg")
    return m


HUNCH = {"body": [18, 0, 0], "head": [-16, 0, 0], "right_arm": [-10, 0, 6], "left_arm": [-10, 0, -6], "right_forearm": [-14, 0, 0],
         "left_forearm": [-14, 0, 0], "@root": [0, -0.6, 0], "right_leg": [-8, 0, 0], "left_leg": [-8, 0, 0], "robe": [-14, 0, 0]}


def anims():
    m = rig()
    A = f"animation.{NAME}."
    f = AnimFile()
    clips = []

    def clip(n, length, loop=False, hold=False):
        clips.append(Clip(f, A + n, length, loop, hold))
        return clips[-1]

    # --- idle: hunched and swaying, twitching now and then as the hex bites. ---
    L = 3.0
    c = clip("idle", L, loop=True)
    for t, k in ((0, 0), (0.75, 1), (1.5, 0), (2.25, -1), (3.0, 0)):
        c.key(t, P_(HUNCH, {"body": [18 + 2 * abs(k), 4 * k, 3 * k], "head": [-16, -6 * k, -6 * k], "right_arm": [-10 + 4 * k, 0, 6],
                            "left_arm": [-10 - 4 * k, 0, -6]}), E_IO if t else None)
    c.layer("head", lambda t: [0, 0, 14 * math.sin((t - 1.9) * math.pi / 0.2)] if 1.9 < t < 2.1 else [0, 0, 0])
    c.layer("right_forearm", lambda t: [-20 * math.sin((t - 1.9) * math.pi / 0.25), 0, 0] if 1.9 < t < 2.15 else [0, 0, 0])
    c.layer("jaw", lambda t: [8 * max(0, math.sin(t * 2 * math.pi / L)), 0, 0])

    # --- walk: a shuffle, the right foot dragging, arms swinging dead. ---
    L = 1.4
    c = clip("walk", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn = math.sin(ph)
        c.key(L * i / 8, P_(HUNCH, {"right_leg": [-8 - 14 * sn, 0, 0], "left_leg": [-8 + 20 * sn, 0, 0], "right_arm": [-10 + 10 * sn, 0, 6],
                                    "left_arm": [-10 - 10 * sn, 0, -6], "body": [20, 6 * sn, 4 * sn], "head": [-18, -4 * sn, -6 * sn],
                                    "@root": [0, -0.6 + 0.3 * abs(math.cos(ph)), 0], "robe": [-14 + 4 * sn, 0, 0]}), E_IO if i else None)
    c.layer("right_forearm", lambda t: [6 * math.sin(t * 2 * math.pi / L + 1), 0, 0])
    c.layer("left_forearm", lambda t: [6 * math.sin(t * 2 * math.pi / L + 2), 0, 0])

    # --- attack: both arms raised and raked down together (tick 8). ---
    c = clip("attack", 0.8)
    c.key(0, HUNCH)
    up = P_(HUNCH, {"body": [-4, 0, 0], "head": [-24, 0, 0], "right_arm": [-150, 0, -12], "left_arm": [-150, 0, 12], "right_forearm": [-30, 0, 0],
                    "left_forearm": [-30, 0, 0], "jaw": [20, 0, 0], "@root": [0, 0.2, 0]})
    down = P_(HUNCH, {"body": [34, 0, 0], "head": [-24, 0, 0], "right_arm": [-40, 0, 10], "left_arm": [-40, 0, -10], "right_forearm": [-10, 0, 0],
                      "left_forearm": [-10, 0, 0], "jaw": [24, 0, 0], "@root": [0, -0.8, -0.6]})
    c.key(0.28, up, E_OUT)
    c.key(0.4, down, E_SNAP)
    c.key(0.8, HUNCH, E_IO)

    # --- freed: the hex breaks -- it slumps forward, shudders, then straightens up, blinking, a person again. ---
    c = clip("freed", 2.4)
    c.key(0, HUNCH)
    slump = P_(HUNCH, {"body": [40, 0, 0], "head": [20, 0, 0], "right_arm": [10, 0, 4], "left_arm": [10, 0, -4], "right_forearm": [0, 0, 0],
                       "left_forearm": [0, 0, 0], "@root": [0, -2.0, -0.6], "right_leg": [-24, 0, 0], "left_leg": [-20, 0, 0], "robe": [-30, 0, 0]})
    c.key(0.35, slump, E_OUT)
    c.key(1.2, P_(slump, {"body": [36, 0, 0]}), E_IO)
    c.key(1.9, {"body": [-4, 0, 0], "head": [-6, 0, 0], "@root": [0, 0.2, 0]}, E_BACK)
    c.key(2.4, {}, E_IO)
    c.layer("body", lambda t: [0, 0, 5 * math.sin(t * 50)] if 0.4 < t < 1.2 else [0, 0, 0])
    c.layer("head", lambda t: [0, 12 * math.sin((t - 2.0) * 2 * math.pi / 0.4), 0] if 2.0 < t < 2.4 else [0, 0, 0])
    for cl in clips:
        cl.done(step=0.05)
    return f, clips


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=61000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")


if __name__ == "__main__":
    generate()
