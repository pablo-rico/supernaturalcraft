"""The Author's hand: one giant right hand of light (the left is the same model, mirrored by the renderer).

Not Metatron's marble-and-quill hand (scribe_hand_art): no sleeve and no portal. Ivory skin with a warm gold
light under it (brightest in the creases and the veins), a writer's ink on the fingertips and the side of the
palm, and a wrist that dissolves into ribbons of light inside a turning band of letters.

Built standing: fingers up (+Y), palm facing -Z (where the entity looks), thumb towards +X. The origin is the
entity's feet, under the wrist; the hand is about 3.4 blocks tall (draw at scale 1).
"""

import math
import random

from animkit import AnimFile
from chuck_art import ANIM, GEO, Model, h01, solid
from chuck_divine_art import BLUE, GOLD, glyph_bit
from common import save
from pixelkit import Ramp, fbm, hexc, mix, shade

IVORY = Ramp("#b89a74", "#d2b590", "#e6cfad", "#f2e2c7", "#faf0de", "#fffaf0")
INK = hexc("#1b1d33")
INK_SMUDGE = hexc("#3a3f5e")
WARM = hexc("#ffcf6a")

PALM_Y0, PALM_Y1 = 8, 30
FINGERS = [  # name, centre x, width, phalanges (proximal -> tip), rest curl
    ("index", 7.5, 4.5, (9, 6, 5), (8, 10, 6)),
    ("middle", 2.5, 4.5, (10, 7, 5), (6, 8, 6)),
    ("ring", -2.5, 4.5, (9, 6, 5), (10, 12, 8)),
    ("pinky", -7.5, 4.0, (7, 5, 4), (14, 14, 10)),
]
INKED = {"index", "middle", "thumb"}


def skin(seed, inked_tip=False, palm=False):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 9, y, seed, 96, 96, 3, 9.0)
        c = IVORY[3.2 + (n - 0.5) * 1.6]
        # Veins of warm light running under the skin.
        v = math.sin(x * 0.45 + y * 0.18 + fbm(x, y, seed + 1, 96, 96, 2, 12.0) * 7)
        if abs(v) < 0.07:
            c = mix(c, WARM, 0.5)
        if palm and face == "north" and (abs(y - h * 0.4 - x * 0.1) < 0.8 or abs(y - h * 0.62 + x * 0.15) < 0.8):
            c = mix(shade(c, 0.85), WARM, 0.35)  # palm lines, lit from within
        if inked_tip and y < h * 0.45 and face != "down" and h01("ink", seed, face, x, y) < 0.85 - y / h:
            c = INK if h01("ink2", seed, x, y) < 0.7 else INK_SMUDGE
        if palm and face == "west" and y > h * 0.3 and h01("smear", x, y) < 0.35:
            c = INK_SMUDGE  # the heel of a writing hand, dragged through wet ink
        edge = min(x, y, w - 1 - x, h - 1 - y)
        return shade(c, 0.88) if edge == 0 else c
    return m


def glow(face, x, y, w, h, c):
    if c in (INK, INK_SMUDGE):
        return None
    if c[2] < c[0] * 0.6:  # the warm veins and lines
        return shade(WARM, 0.9)
    return (int(c[0] * 0.42), int(c[1] * 0.34), int(c[2] * 0.2), 255)


def nail(face, x, y, w, h):
    return mix(hexc("#f5e9da"), hexc("#e3cdb7"), y / max(1, h - 1))


def rig():
    m = Model("author_hand", 512, 512)
    m.bone("root", (0, 0, 0))
    hand = m.bone("hand", (0, 6, 0), "root")
    m.cube(hand, (-10, PALM_Y0, -3.5), (20, PALM_Y1 - PALM_Y0, 7), skin(3, palm=True), glow, density=2, tag="palm")
    m.cube(hand, (4, 9, -4.5), (7, 12, 1.5), skin(4), glow, density=2, tag="thenar")  # the thumb's mound
    m.cube(hand, (-10, 10, -4.25), (6, 12, 1), skin(5), glow, density=2, tag="hypothenar")
    for name, cx, w, _l, _c in FINGERS:
        m.cube(hand, (cx - w / 2 + 0.5, PALM_Y1 - 2, 2.75), (w - 1, 3, 1.5), skin(6), glow, density=2, tag="knuckle")
        m.cube(hand, (cx - 0.5, 14, 3.4), (1, 14, 0.5), skin(7), glow, density=2, tag="tendon")
    # The wrist dissolving into light.
    def wrist(face, x, y, w, h):
        if face == "down":
            return None
        if y > h * 0.45 and h01("wr", face, x, y) < (y / h - 0.45) * 1.8:
            return None
        return mix(skin(8)(face, x, y, w, h), hexc("#fff6dc"), max(0.0, y / h - 0.3))
    m.cube(hand, (-8, 0, -3), (16, 8, 6), wrist, lambda f, x, y, w, h, c: shade(c, 0.6), density=2, tag="wrist")
    rng = random.Random(9)
    for k in range(6):
        a = math.radians(k * 60 + 15)
        px, pz = 7 * math.cos(a), -3 * math.sin(a)
        b = m.bone(f"wisp_{k}", (px, 2, pz), "hand", rotation=(rng.uniform(-12, 12), 0, rng.uniform(-10, 10)))
        L = rng.choice((5, 6, 7))
        m.cube(b, (px - 0.75, 2 - L, pz - 0.75), (1.5, L, 1.5), lambda f, x, y, w, h: None if y > h - 2 and (x + y) % 2
               else mix(hexc("#fff4d6"), BLUE[3], y / h), lambda f, x, y, w, h, c: c, density=2, tag="wisp")
    # A band of his own letters turning round the wrist.
    script = m.bone("script", (0, 5, 0), "hand")
    word = "INTHEBEGINNING"
    for i, ch in enumerate(word):
        deg = i * 360 / len(word)
        m.cube(script, (-1.5, 3, -11), (3, 4, 0), lambda f, x, y, w, h, ch=ch: GOLD[5] if glyph_bit(ch, x, y, w, h) else None,
               lambda f, x, y, w, h, c: GOLD[5], faces=("north", "south"), density=4, tag="letter",
               rotation=(0, deg, 0), pivot=(0, 5, 0))

    for name, cx, w, lens, curls in FINGERS:
        parent, base = "hand", PALM_Y1
        for i, (ln, curl) in enumerate(zip(lens, curls)):
            wd = w - 0.5 * i
            b = m.bone(f"{name}{i + 1}", (cx, base, 0), parent, rotation=(curl, 0, (cx * 0.6) if i == 0 else 0))
            m.cube(b, (cx - wd / 2, base, -2.5 + 0.25 * i), (wd, ln, 5 - 0.5 * i), skin(20 + i, inked_tip=(i == 2 and name in INKED)),
                   glow, density=2, tag="finger")
            if i < 2:
                m.cube(b, (cx - wd / 2 - 0.25, base + ln - 1, -2.75 + 0.25 * i), (wd + 0.5, 2, 5.5 - 0.5 * i), skin(30 + i), glow,
                       density=2, tag="joint")
            else:
                m.cube(b, (cx - wd / 2 + 0.75, base + ln - 3.5, 2.25 - 0.25 * i), (wd - 1.5, 3.5, 0.5), nail, 0.4, density=2,
                       tag="nail")
            parent, base = b.name, base + ln
    # The thumb: from the heel of the palm, up and out to +X.
    t1 = m.bone("thumb1", (9, 12, -1.5), "hand", rotation=(12, 0, 38))
    m.cube(t1, (6.5, 12, -4), (5, 8, 5), skin(40), glow, density=2, tag="finger")
    t2 = m.bone("thumb2", (9, 20, -1.5), "thumb1", rotation=(8, 0, -12))
    m.cube(t2, (6.75, 20, -3.75), (4.5, 6, 4.5), skin(41), glow, density=2, tag="finger")
    m.cube(t2, (6.5, 19, -4), (5, 2, 5), skin(42), glow, density=2, tag="joint")
    t3 = m.bone("thumb3", (9, 26, -1.5), "thumb2", rotation=(6, 0, -10))
    m.cube(t3, (7, 26, -3.5), (4, 5, 4), skin(43, inked_tip=True), glow, density=2, tag="finger")
    m.cube(t3, (7.75, 27.5, -3.75), (2.5, 3.5, 0.5), nail, 0.4, density=2, tag="nail")
    return m


# --- animations -----------------------------------------------------------------------------------------

E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"
H = "animation.author_hand."
DIGITS = [f"{n}{i}" for n, *_ in FINGERS for i in (1, 2, 3)] + ["thumb1", "thumb2", "thumb3"]


def curl_all(a, t_keys, amount):
    """Keys every finger joint to curl by `amount` (0 open .. 1 fist) at the given times."""
    for name, *_ in FINGERS:
        for i in (1, 2, 3):
            a.rot(f"{name}{i}", *[(t, [80 * k * (1 if i > 1 else 0.9), 0, 0], *e) for t, k, *e in t_keys])
    a.rot("thumb2", *[(t, [30 * k, 0, -30 * k], *e) for t, k, *e in t_keys])
    a.rot("thumb3", *[(t, [40 * k, 0, -20 * k], *e) for t, k, *e in t_keys])


def anims():
    f = AnimFile()
    idle = f.new(H + "idle", 6.0, loop=True)
    idle.pos("hand", (0, [0, 0, 0]), (3.0, [0, 1.5, 0], E_IO), (6.0, [0, 0, 0], E_IO))
    idle.rot("hand", (0, [0, 0, 0]), (3.0, [-5, 4, 3], E_IO), (6.0, [0, 0, 0], E_IO))
    idle.rot("script", (0, [0, 0, 0]), (6.0, [0, 360, 0]))
    for i, b in enumerate(DIGITS):
        idle.rot(b, (0, [0, 0, 0]), (2.0 + (i % 3) * 0.3, [6, 0, 0], E_IO), (4.0, [0, 0, 0], E_IO), (6.0, [0, 0, 0]))
    for k in range(6):
        idle.rot(f"wisp_{k}", (0, [0, 0, 0]), (1.5 + k * 0.4, [12, 0, 8], E_IO), (6.0, [0, 0, 0], E_IO))

    appear = f.new(H + "appear", 1.8)
    appear.scale("hand", (0, [0.05, 0.05, 0.05]), (0.9, [1.12, 1.12, 1.12], E_BACK), (1.3, [1, 1, 1], E_IO), (1.8, [1, 1, 1]))
    curl_all(appear, [(0, 1.0), (0.6, 1.0), (1.4, 0.0, E_OUT), (1.8, 0.0)], 1)
    appear.rot("script", (0, [0, 0, 0]), (1.8, [0, 540, 0], E_OUT))

    slam = f.new(H + "slam", 1.6)
    # Palm turned to the ground and brought down flat.
    slam.rot("hand", (0, [0, 0, 0]), (0.4, [-70, 0, 0], E_OUT), (0.55, [-92, 0, 0], E_SNAP), (1.1, [-92, 0, 0]), (1.6, [0, 0, 0], E_IO))
    slam.pos("hand", (0, [0, 0, 0]), (0.4, [0, 6, 0], E_OUT), (0.55, [0, -4, 0], E_SNAP), (1.1, [0, -4, 0]), (1.6, [0, 0, 0], E_IO))
    curl_all(slam, [(0, 0.0), (0.4, -0.1), (1.1, -0.1), (1.6, 0.0, E_IO)], 1)

    grab = f.new(H + "grab", 1.6)
    grab.rot("hand", (0, [0, 0, 0]), (0.4, [-20, 0, 0], E_OUT), (0.6, [10, 0, 0], E_SNAP), (1.2, [10, 0, 0]), (1.6, [0, 0, 0], E_IO))
    curl_all(grab, [(0, 0.0), (0.4, -0.15, E_OUT), (0.6, 1.0, E_SNAP), (1.2, 1.0), (1.6, 0.0, E_IO)], 1)

    snap = f.new(H + "snap", 1.6)
    # Middle finger against the thumb, a beat of pressure, then it cracks down into the palm.
    snap.rot("hand", (0, [0, 0, 0]), (0.5, [-10, 20, -8], E_OUT), (0.9, [-10, 20, -8]), (0.95, [-4, 24, -12], E_SNAP),
             (1.6, [0, 0, 0], E_IO))
    snap.rot("middle1", (0, [0, 0, 0]), (0.5, [38, 0, 0], E_OUT), (0.9, [40, 0, 0]), (0.95, [95, 0, 0], E_SNAP), (1.3, [95, 0, 0]),
             (1.6, [0, 0, 0], E_IO))
    snap.rot("middle2", (0, [0, 0, 0]), (0.5, [30, 0, 0], E_OUT), (0.9, [30, 0, 0]), (0.95, [70, 0, 0], E_SNAP), (1.3, [70, 0, 0]),
             (1.6, [0, 0, 0], E_IO))
    snap.rot("thumb1", (0, [0, 0, 0]), (0.5, [30, -20, -26], E_OUT), (0.9, [32, -20, -28]), (0.95, [10, 0, -6], E_SNAP),
             (1.6, [0, 0, 0], E_IO))
    snap.rot("thumb2", (0, [0, 0, 0]), (0.5, [20, 0, -10], E_OUT), (0.95, [0, 0, 6], E_SNAP), (1.6, [0, 0, 0], E_IO))
    for fn in ("ring", "pinky"):
        for i in (1, 2, 3):
            snap.rot(f"{fn}{i}", (0, [0, 0, 0]), (0.5, [75, 0, 0], E_OUT), (1.3, [75, 0, 0]), (1.6, [0, 0, 0], E_IO))
    snap.rot("index1", (0, [0, 0, 0]), (0.5, [10, 0, 0], E_OUT), (1.6, [0, 0, 0], E_IO))

    sweep = f.new(H + "sweep", 1.4)
    sweep.rot("hand", (0, [0, 0, 0]), (0.35, [-20, -60, 70], E_OUT), (0.7, [-20, 60, 70], E_SNAP), (1.0, [-20, 60, 70]),
              (1.4, [0, 0, 0], E_IO))
    curl_all(sweep, [(0, 0.0), (0.35, 0.1), (1.0, 0.1), (1.4, 0.0, E_IO)], 1)

    write = f.new(H + "write", 1.2)
    # Pinched as if holding a pen, tracing a line of script.
    write.rot("hand", (0, [0, 0, 0]), (0.3, [-40, 0, 10], E_OUT), (0.55, [-38, -8, 6], E_IO), (0.8, [-42, 8, 12], E_IO),
              (1.2, [0, 0, 0], E_IO))
    for name, curl in (("index", 0.45), ("middle", 0.5), ("ring", 0.85), ("pinky", 0.9)):
        for i in (1, 2, 3):
            write.rot(f"{name}{i}", (0, [0, 0, 0]), (0.3, [70 * curl, 0, 0], E_OUT), (0.9, [70 * curl, 0, 0]), (1.2, [0, 0, 0], E_IO))
    write.rot("thumb1", (0, [0, 0, 0]), (0.3, [24, -10, -20], E_OUT), (0.9, [24, -10, -20]), (1.2, [0, 0, 0], E_IO))

    vanish = f.new(H + "vanish", 1.4, hold=True)
    curl_all(vanish, [(0, 0.0), (0.5, -0.2, E_OUT), (1.4, -0.2)], 1)
    vanish.scale("hand", (0, [1, 1, 1]), (0.5, [1.1, 1.1, 1.1], E_OUT), (1.4, [0.02, 0.02, 0.02], E_IN))
    vanish.rot("script", (0, [0, 0, 0]), (1.4, [0, 720, 0], E_IN))
    return f


def generate():
    m = rig()
    t, g = m.build(seed=1201)
    m.rig.write(GEO + "author_hand.geo.json")
    save(t, "entity", "author_hand")
    save(g, "entity", "author_hand_glowmask")
    anims().write(ANIM + "author_hand.animation.json")


def preview(path, anim=None, at=0.0, scale=6, views=("front", "three_quarter", "side", "back")):
    import json
    import preview3d
    m = rig()
    t, _ = m.build(seed=1201)
    pose = None
    if anim:
        data = json.loads(json.dumps({"animations": {x.name: x.to_json() for x in anims().anims}}))
        pose = preview3d.pose_from(data, H + anim, at)
    preview3d.render(m.rig, t, path, pose=pose, scale=scale, views=views)


if __name__ == "__main__":
    generate()
