"""Death's reapers (v0.11): tall hooded figures in ragged black cloaks, no face under the hood but two cold
pinpoints, long pale bony hands; they float a hand's breadth off the ground, the cloak's hem trailing in tatters.

Rig: root -- float -- body -- head (hood), right_arm -- right_forearm -- right_hand, left_*, skirt -- skirt_back.
Clips (animation.reaper.*): idle (loop: bob and sway), walk (loop: a leaning glide, cloak streaming), attack (once:
the right claw rises and rakes down).
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from horsemen_art import Clip, E_IN, E_IO, E_OUT, P, glow_faint
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

CLOAK = Ramp("#020203", "#060608", "#0b0b0e", "#121216", "#1a1a20", "#24242b")
BONE = Ramp("#55535a", "#7a7880", "#9c9aa2", "#b8b6bd", "#cfcdd3", "#e4e3e8")
EYE = hexc("#cfe6ff")


def cloak_mat(seed, ragged=False):
    def m(face, x, y, w, h):
        if face == "down" and ragged:
            return None
        n = fbm(x + len(face) * 9, y * 2, seed, 64, 64, 2, 8.0)
        v = 2.4 + (n - 0.5) * 1.2 + (0.6 if face == "up" else 0)
        if face in ("north", "south", "east", "west") and (x % 3 == 0):
            v -= 0.5  # folds
        if ragged and face in ("north", "south", "east", "west"):
            cut = int(3 + 4 * h01("rag", seed, face, x // 2))
            if y >= h - cut and h01("hole", seed, face, x, y) < 0.35 + (y - (h - cut)) * 0.12:
                return None
        return CLOAK[v]
    return m


def build():
    m = Model("reaper", 256, 128)
    m.bone("root", (0, 0, 0))
    m.bone("float", (0, 0, 0), "root")
    m.bone("body", (0, 16, 0), "float")
    m.bone("head", (0, 31, 0), "body", rotation=(8, 0, 0))
    m.bone("skirt", (0, 18, 0), "body")
    m.bone("skirt_back", (0, 10, 2.5), "skirt", rotation=(10, 0, 0))
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (5.5 * s, 29, 0), "body", rotation=(-10, 0, -6 * s))
        m.bone(f"{side}_forearm", (6 * s, 23, 0), f"{side}_arm", rotation=(-20, 0, 0))
        m.bone(f"{side}_hand", (6 * s, 17.5, 0), f"{side}_forearm")
    g = glow_faint(0.12)
    cl = cloak_mat(3501)
    # Torso and shoulders under the cloak, a deep hood with a void inside, two eyes.
    m.cube("body", (-4.5, 18, -2.5), (9, 13, 5), cl, glow=g, density=2, tag="torso")
    m.cube("body", (-5, 27, -3), (10, 4, 6), none_on(("down",), cl), glow=g, inflate=0.3, density=2, tag="mantle")

    def hood(face, x, y, w, h):
        if face == "north":
            cx = (w - 1) / 2
            if abs(x - cx) < 5.5 and y > 3:
                return hexc("#010102")  # the void
            return CLOAK[3.4 - y * 0.05]
        if face == "down":
            return hexc("#010102")
        return cl(face, x, y, w, h)

    def eyes(face, x, y, w, h, col):
        return EYE if face == "north" and y == 9 and x in (5, 10) else shade(col, 0.1)
    m.cube("head", (-4.5, 30, -4.5), (9, 9, 9), lambda f, x, y, w, h: EYE if f == "north" and y == 9 and x in (5, 10) else hood(f, x, y, w, h),
           glow=eyes, density=2, tag="hood")
    m.cube("head", (-1.5, 38.5, -1), (3, 2, 4), cl, glow=g, density=2, tag="hood_peak", rotation=(-25, 0, 0), pivot=(0, 38.5, 1))
    # Long robe skirt (no legs), ragged at the hem; a trailing back panel.
    m.cube("skirt", (-5, 4, -3), (10, 14, 6), cloak_mat(3502, True), glow=g, inflate=0.2, density=2, tag="robe")
    m.cube("skirt_back", (-4.5, 0, 2), (9, 10, 2), cloak_mat(3503, True), glow=g, density=2, tag="robe_tail")
    bone = lambda f, x, y, w, h: BONE[3 + (fbm(x, y, 3504, 32, 32, 2, 6.0) - 0.5) * 1.6 - (1.2 if y == h - 1 else 0)]
    for side, s in (("right", -1), ("left", 1)):
        m.cube(f"{side}_arm", (6 * s - 2, 23, -2), (4, 6.5, 4), cl, glow=g, density=2, inflate=0.2, tag="sleeve")
        m.cube(f"{side}_forearm", (6 * s - 2.25, 17.5, -2.25), (4.5, 5.5, 4.5), cloak_mat(3505 + (s > 0), True), glow=g, density=2,
               tag="sleeve_cuff")
        m.cube(f"{side}_hand", (6 * s - 1, 14.5, -1), (2, 3.5, 2), bone, glow=glow_faint(0.3), density=2, tag="palm")
        for k, z in enumerate((-1.0, 0.0, 1.0)):
            m.cube(f"{side}_hand", (6 * s - 0.4 - 0.3 * (k - 1) * s, 10.5, z - 0.3), (0.6, 4.5, 0.6), bone, glow=glow_faint(0.3), density=4,
                   rotation=(-8 + 8 * k, 0, 6 * s), pivot=(6 * s, 14.5, z), tag="finger")
    t, gl = m.build(gutter=1, seed=3510)
    return m, t, gl


def anims():
    f = AnimFile()
    A = "animation.reaper."
    c = Clip(f, A + "idle", 3.0, loop=True)
    for i in range(7):
        t = 3.0 * i / 6
        k = math.sin(2 * math.pi * t / 3.0)
        c.key(t, {"@float": [0, 2.0 + 0.8 * k, 0], "body": [2 * k, 0, 1.5 * math.cos(2 * math.pi * t / 3.0)], "head": [-2 * k, 6 * k, 0],
                  "skirt": [-3 * k, 0, 2 * math.cos(2 * math.pi * t / 3.0)], "skirt_back": [4 * k, 0, 0],
                  "right_arm": [3 * k, 0, 2 * k], "left_arm": [-3 * k, 0, -2 * k]})
    c.done()
    c = Clip(f, A + "walk", 1.6, loop=True)
    for i in range(9):
        t = 1.6 * i / 8
        k = math.sin(2 * math.pi * t / 1.6)
        c.key(t, {"@float": [0, 2.0 + 0.6 * math.sin(4 * math.pi * t / 1.6), 0], "body": [14, 3 * k, 0], "head": [-10, 0, 0],
                  "skirt": [-20 + 4 * k, 0, 3 * k], "skirt_back": [26 + 6 * math.sin(4 * math.pi * t / 1.6), 0, 0],
                  "right_arm": [20 + 4 * k, 0, 4], "left_arm": [20 - 4 * k, 0, -4], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0]})
    c.done()
    c = Clip(f, A + "attack", 1.2)
    rest = {"@float": [0, 2, 0]}
    up = P(rest, {"right_arm": [-160, 0, -10], "right_forearm": [-30, 0, 0], "right_hand": [-30, 0, 0], "body": [-10, 15, 0],
                  "head": [-10, 0, 0], "left_arm": [-30, 0, -20], "skirt": [10, 0, 0]})
    rake = P(rest, {"right_arm": [-40, -20, 10], "right_forearm": [-10, 0, 0], "right_hand": [40, 0, 0], "body": [24, -20, 0],
                    "head": [10, 0, 0], "left_arm": [10, 0, -20], "skirt": [-16, 0, 0], "skirt_back": [20, 0, 0], "@float": [0, 1.2, -3]})
    c.key(0, rest)
    c.key(0.45, up, E_OUT)
    c.key(0.62, rake, E_IN)
    c.key(0.8, P(rake, {"body": [20, -24, 0]}), E_OUT)
    c.key(1.2, rest, E_IO)
    c.done()
    return f


def generate():
    m, t, g = build()
    m.rig.write(GEO + "reaper.geo.json")
    save(t, "entity", "reaper")
    save(g, "entity", "reaper_glowmask")
    anims().write(ANIM + "reaper.animation.json")
