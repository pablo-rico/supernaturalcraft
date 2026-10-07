"""The ghost: a pale woman of the 1800s in a long white dress, dark hair loose, hollow eyes. No feet:
the skirt's hem is torn to rags and fades to nothing (texture alpha; the renderer draws it translucent).

Bones: root > body > head (> jaw, hair_back), right_arm, left_arm, skirt > hem. The jaw drops for the
scream (its gap shows a dark mouth). Faces north, 32 px tall like the player.

Glowmask: a faint copy of the whole figure (so it reads at night, see lilith_art's inner_light), the
face and hands a little brighter, and a cold pinpoint in each empty eye socket. The glow layer adds
light, so the copy is scaled by the texture's alpha and the rags fade in the glow too.
"""

import math
import random

from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import SHADE, paint_cube, set_face_px, stamp_face
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

SKIN = Ramp("#46505c", "#6a7784", "#909dab", "#b3bfca", "#cfd9e2", "#e6eef4")
HAIR = Ramp("#06070a", "#0d0f14", "#151920", "#1f242c", "#2a303a", "#384050")
DRESS = Ramp("#57616d", "#7c8894", "#a2adb8", "#c3cdd6", "#dbe3ea", "#eff4f8")
LACE = hexc("#f7fbff")
SOCKET = hexc("#020305")
RIM = hexc("#2a323c")
GLINT = hexc("#5f86a8")
STAIN = hexc("#6b6258")


# --- geometry -------------------------------------------------------------------------------

def rig():
    r = Rig("ghost", 128, 128)
    r.bone("root", (0, 0, 0))
    body = r.bone("body", (0, 12, 0), "root")
    head = r.bone("head", (0, 24, 0), "body")
    jaw = r.bone("jaw", (0, 26, 2), "head")
    hair = r.bone("hair_back", (0, 27, 4), "head")
    ra = r.bone("right_arm", (-5.5, 22, 0), "body")
    la = r.bone("left_arm", (5.5, 22, 0), "body")
    skirt = r.bone("skirt", (0, 12, 0), "body")
    hem = r.bone("hem", (0, 6, 0), "skirt")
    p = {
        "torso": box(body, (-4, 12, -2), (8, 12, 4)),
        "head": box(head, (-4, 26, -4), (8, 6, 8)),
        "hair": box(head, (-4, 26, -4), (8, 6, 8), inflate=0.4),
        "strand_r": box(head, (-4.4, 17, -3.3), (2, 9, 1), inflate=0.1),
        "strand_l": box(head, (2.4, 18, -3.3), (2, 8, 1), inflate=0.1),
        "jaw": box(jaw, (-2.5, 24, -3.8), (5, 2, 6)),
        "hair_back": box(hair, (-4, 13, 3.4), (8, 14, 2), inflate=0.15),
        "arm_r": box(ra, (-7, 10, -1.5), (3, 12, 3)),
        "puff_r": box(ra, (-7.2, 18, -1.7), (3, 4, 3), inflate=0.35),
        "fingers_r": box(ra, (-6.8, 7.5, -1.2), (2, 3, 2)),
        "arm_l": box(la, (4, 10, -1.5), (3, 12, 3)),
        "puff_l": box(la, (4.2, 18, -1.7), (3, 4, 3), inflate=0.35),
        "fingers_l": box(la, (4.8, 7.5, -1.2), (2, 3, 2)),
        "skirt_top": box(skirt, (-4.5, 6, -2.5), (9, 6, 5), inflate=0.2),
        "skirt_low": box(hem, (-5, 1, -3), (10, 5, 6), inflate=0.1),
        "rags": box(hem, (-5.5, -3, -3.5), (11, 4, 7)),
    }
    r.pack()
    return r, p


# --- texture --------------------------------------------------------------------------------

def _ramp_px(ramp, base, n, amp):
    return ramp[max(0, min(5, int(round(base + (n - 0.5) * amp))))]


class Painter:
    def __init__(self, rig_, parts):
        self.r, self.p = rig_, parts
        self.t = Tex(rig_.tex_w, rig_.tex_h, 1501)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 1502)
        self.bright = set()  # (cube, face, x, y) that glow a little more (face, hands)

    def mat(self, ramp, seed, base, amp=1.4, scale=6.0):
        def m(face, x, y, w, h):
            return _ramp_px(ramp, base, fbm(x * 2 + len(face), y * 2, seed, 64, 64, 2, scale), amp)
        return m

    def paint(self):
        p = self.p
        skin, hair, dress = self.mat(SKIN, 11, 3.3, 0.8), self.mat(HAIR, 12, 2.4, 1.6, 8.0), self.mat(DRESS, 13, 3.2, 1.0)

        def head(face, x, y, w, h):
            if face == "north" and 1 <= x <= 6:
                return skin(face, x, y, w, h)
            if face == "down":
                return hexc("#0b0c10")  # the roof of the mouth, seen when the jaw drops
            return hair(face, x, y, w, h)
        paint_cube(self.t, p["head"], head)
        self.face()

        def hair_shell(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north":
                # A centre parting, swept to the sides: hair only frames the face.
                if y == 0 and x not in (3, 4):
                    return hair(face, x, y, w, h)
                if x in (0, 7) and y < h:
                    return hair(face, x, y, w, h)
                return None
            if face == "up" and x in (3, 4):
                return HAIR[4]  # the parting
            return hair(face, x, y, w, h)
        paint_cube(self.t, p["hair"], hair_shell)
        for s in ("strand_r", "strand_l"):
            paint_cube(self.t, p[s], lambda f, x, y, w, h: None if f == "up" or (f != "down" and y == h - 1 and x % 2)
                       else hair(f, x, y, w, h))

        def hair_back(face, x, y, w, h):
            if face == "up":
                return None
            if face != "down" and y >= h - 3 and (x * 7 + y) % 3 == 0:
                return None  # ragged ends
            return hair(face, x, y, w, h)
        paint_cube(self.t, p["hair_back"], hair_back)

        def jaw(face, x, y, w, h):
            if face == "up":
                return hexc("#0b0c10") if 0 < y else (SKIN[5] if x % 2 else SKIN[3])  # teeth along the front
            if face == "north":
                if y == 0:
                    return hexc("#2c2f37") if 1 <= x <= 3 else SKIN[2]  # thin, colourless lips
                return SKIN[2] if x in (0, 4) else skin(face, x, y, w, h)
            return skin(face, x, y, w, h) if face != "down" else SKIN[1]
        paint_cube(self.t, p["jaw"], jaw)
        for (x, y) in ((0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (0, 1), (1, 1), (2, 1), (3, 1), (4, 1)):
            self.bright.add((p["jaw"], "north", x, y))

        # Bodice: high lace collar, a row of buttons, a seam at the waist, an old stain over the heart.
        def bodice(face, x, y, w, h):
            c = dress(face, x, y, w, h)
            if face in ("north", "south", "east", "west"):
                if y <= 1:
                    return LACE if (x + y) % 2 == 0 else DRESS[4]
                if y == h - 1:
                    return DRESS[1]
                if face == "north" and x in (3, 4):
                    return DRESS[2] if y % 2 else (DRESS[0] if x == 3 else DRESS[5])
                if face in ("north", "south") and x in (0, w - 1):
                    return mix(c, DRESS[1], 0.5)  # darts at the sides
            return c
        paint_cube(self.t, p["torso"], bodice)
        for (x, y, k) in ((5, 4, 0.55), (6, 4, 0.35), (5, 5, 0.45), (6, 5, 0.25), (5, 6, 0.2)):
            u, v, _, _ = p["torso"].rects["north"]
            self.t.set(u + x, v + y, mix(self.t.get(u + x, v + y), STAIN, k))

        sleeve = self.mat(DRESS, 14, 3.0, 1.0)
        for side in ("r", "l"):
            def arm(face, x, y, w, h):
                if face == "down" or (face != "up" and y >= h - 3):
                    return skin(face, x, y, w, h)
                if face != "up" and y == h - 4:
                    return LACE if x % 2 == 0 else DRESS[4]  # lace cuff
                return sleeve(face, x, y, w, h)
            paint_cube(self.t, p["arm_" + side], arm)
            paint_cube(self.t, p["puff_" + side], lambda f, x, y, w, h: None if f == "down"
                       else (DRESS[4] if (x + y) % 3 == 0 else sleeve(f, x, y, w, h)))

            def fingers(face, x, y, w, h):
                if face == "up":
                    return None
                if face != "down" and x % 2 == 1 and y >= 1:
                    return None  # gaps between long fingers
                if face == "down" or y == h - 1:
                    return SKIN[1]  # grey nails
                return SKIN[4] if y == 0 else SKIN[3]
            paint_cube(self.t, p["fingers_" + side], fingers)
            for name in ("arm_" + side, "fingers_" + side):
                cube = p[name]
                for face, (u, v, w, h) in cube.rects.items():
                    for y in range(h):
                        for x in range(w):
                            if name.startswith("fingers") or face == "down" or y >= h - 3:
                                self.bright.add((cube, face, x, y))

        self.skirt()
        self.inner_light()
        return self.t, self.g

    def face(self):
        p = self.p
        pal = {"d": SKIN[2], "k": SOCKET, "g": RIM, "n": SKIN[2], "c": SKIN[1], "m": hexc("#3c3f47"), "S": SKIN[4]}
        stamp_face(self.t, p["head"], "north", [
            "..SSSS..",
            ".gddddg.",
            ".kk..kk.",
            ".kk..kk.",
            ".cgnngc.",
            ".cc..cc.",
        ], pal)
        for y in range(6):
            for x in range(1, 7):
                self.bright.add((p["head"], "north", x, y))
        for x in (2, 5):  # a faint cold pinpoint deep in each empty socket (glow only)
            set_face_px(self.g, p["head"], "north", x, 3, GLINT)

    def skirt(self):
        """Long folds; the hem frays into rags that fade out between y = 4 and y = -3."""
        p = self.p
        rng = random.Random(1510)
        tears = {}
        for name, top in (("skirt_top", 12.2), ("skirt_low", 6.1), ("rags", 1.0)):
            cube = p[name]
            for face, (u, v, w, h) in cube.rects.items():
                for y in range(h):
                    for x in range(w):
                        if face == "up":
                            c = DRESS[2]
                            a = 255 if name == "skirt_top" else 0
                        elif face == "down":
                            c, a = DRESS[1], 0
                        else:
                            fold = math.sin((x + (u % 7)) * 1.9) * 0.9
                            n = fbm(x + u, y + v, 1511, 128, 128, 2, 16.0)
                            c = DRESS[max(0, min(5, int(round(3.0 + fold + (n - 0.5) * 1.2))))]
                            wy = top - y - 0.5
                            key = (name, face, x)
                            if key not in tears:
                                tears[key] = rng.uniform(-1.6, 1.4) + (rng.random() < 0.25) * rng.uniform(1.0, 3.0)
                            k = (wy + 3.0 - tears[key]) / 7.0  # 1 at y = 4, 0 at y = -3 (per-column ragged)
                            k = max(0.0, min(1.0, k + (n - 0.5) * 0.3))
                            a = int(255 * k ** 1.4)
                            if name == "skirt_top" and y == 0:
                                c = DRESS[1]  # the waist seam
                        if a < 12:
                            continue
                        c = shade(c, SHADE[face])
                        self.t.set(u + x, v + y, (c[0], c[1], c[2], a))

    def inner_light(self, k=0.22, k_bright=0.5):
        """The glow layer adds light: a faint copy of everything (scaled by alpha), face and hands brighter."""
        bright = set()
        for (cube, face, x, y) in self.bright:
            if face in cube.rects:
                u, v, w, h = cube.rects[face]
                if 0 <= x < w and 0 <= y < h:
                    bright.add((u + x, v + y))
        for y in range(self.t.h):
            for x in range(self.t.w):
                c = self.t.rows[y][x]
                if not c[3] or self.g.rows[y][x][3]:
                    continue
                f = (k_bright if (x, y) in bright else k) * c[3] / 255
                self.g.rows[y][x] = (int(c[0] * f), int(c[1] * f), int(min(255, c[2] * f * 1.08)), 255)


def paint():
    r, p = rig()
    return Painter(r, p).paint()


# --- animations -----------------------------------------------------------------------------

E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"
A = "animation.ghost."


def anims():
    f = AnimFile()

    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("root", (0, [0, 1.5, 0]), (2.0, [0, 2.6, 0], E_IO), (4.0, [0, 1.5, 0], E_IO))
    idle.rot("body", (0, [2, 0, 1]), (2.0, [3, 3, -1], E_IO), (4.0, [2, 0, 1], E_IO))
    idle.rot("head", (0, [8, -6, -6]), (1.6, [10, 4, -9], E_IO), (3.0, [6, -2, -4], E_IO), (4.0, [8, -6, -6], E_IO))
    idle.rot("right_arm", (0, [-2, 0, 3]), (2.0, [2, 0, 5], E_IO), (4.0, [-2, 0, 3], E_IO))
    idle.rot("left_arm", (0, [-2, 0, -3]), (2.0, [2, 0, -5], E_IO), (4.0, [-2, 0, -3], E_IO))
    idle.rot("skirt", (0, [0, 0, 0]), (2.0, [-3, 0, 1], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("hem", (0, [3, 0, -2]), (1.3, [-4, 0, 2], E_IO), (2.7, [2, 0, -3], E_IO), (4.0, [3, 0, -2], E_IO))
    idle.rot("hair_back", (0, [4, 0, 0]), (2.0, [8, 0, 2], E_IO), (4.0, [4, 0, 0], E_IO))

    drift = f.new(A + "float", 2.4, loop=True)
    drift.pos("root", (0, [0, 2.0, 0]), (1.2, [0, 3.0, 0], E_IO), (2.4, [0, 2.0, 0], E_IO))
    drift.rot("body", (0, [16, 0, 0]), (1.2, [19, 0, 0], E_IO), (2.4, [16, 0, 0], E_IO))
    drift.rot("head", (0, [-14, 0, 0]), (1.2, [-10, 0, 3], E_IO), (2.4, [-14, 0, 0], E_IO))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        drift.rot(side, (0, [24, 0, 6 * s]), (1.2, [30, 0, 9 * s], E_IO), (2.4, [24, 0, 6 * s], E_IO))
    drift.rot("skirt", (0, [14, 0, 0]), (1.2, [18, 0, 0], E_IO), (2.4, [14, 0, 0], E_IO))
    drift.rot("hem", (0, [22, 0, -3]), (0.6, [30, 0, 0], E_IO), (1.2, [24, 0, 3], E_IO), (1.8, [32, 0, 0], E_IO),
              (2.4, [22, 0, -3], E_IO))
    drift.rot("hair_back", (0, [26, 0, 0]), (1.2, [34, 0, 3], E_IO), (2.4, [26, 0, 0], E_IO))

    scream = f.new(A + "scream", 1.0)
    scream.pos("root", (0, [0, 1.5, 0]), (0.25, [0, 3.0, 0], E_OUT), (0.8, [0, 3.0, 0]), (1.0, [0, 1.5, 0], E_IO))
    scream.rot("body", (0, [0, 0, 0]), (0.2, [10, 0, 0], E_OUT), (0.35, [-12, 0, 0], E_BACK), (0.8, [-10, 0, 0]),
               (1.0, [0, 0, 0], E_IO))
    scream.rot("head", (0, [0, 0, 0]), (0.2, [18, 0, 0], E_OUT), (0.35, [-8, 0, 0], E_BACK), (0.45, [-6, 0, 4]),
               (0.55, [-8, 0, -4]), (0.8, [-6, 0, 0]), (1.0, [0, 0, 0], E_IO))
    scream.pos("head", (0, [0, 0, 0]), (0.35, [0, 0, -2.2], E_BACK), (0.8, [0, 0, -1.8]), (1.0, [0, 0, 0], E_IO))
    scream.rot("jaw", (0, [0, 0, 0]), (0.3, [42, 0, 0], E_BACK), (0.8, [38, 0, 0]), (1.0, [0, 0, 0], E_IO))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        scream.rot(side, (0, [0, 0, 0]), (0.2, [10, 0, 10 * s], E_OUT), (0.35, [-35, 0, 85 * s], E_BACK),
                   (0.8, [-30, 0, 80 * s]), (1.0, [0, 0, 3 * s], E_IO))
    scream.rot("hem", (0, [0, 0, 0]), (0.35, [-24, 0, 0], E_BACK), (0.8, [-18, 0, 0]), (1.0, [0, 0, 0], E_IO))
    scream.rot("hair_back", (0, [0, 0, 0]), (0.35, [45, 0, 0], E_BACK), (0.8, [38, 0, 0]), (1.0, [4, 0, 0], E_IO))

    fade = f.new(A + "fade", 1.0, hold=True)
    fade.pos("root", (0, [0, 1.5, 0]), (0.3, [0, 3.5, 0], E_OUT), (1.0, [0, -10, 0], E_IN))
    fade.scale("root", (0, [1, 1, 1]), (0.3, [1.05, 1.08, 1.05], E_OUT), (1.0, [0.25, 0.55, 0.25], E_IN))
    fade.rot("head", (0, [0, 0, 0]), (0.3, [-30, 0, 0], E_OUT), (1.0, [-40, 0, 0]))
    fade.rot("jaw", (0, [0, 0, 0]), (0.3, [25, 0, 0], E_OUT), (1.0, [30, 0, 0]))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        fade.rot(side, (0, [0, 0, 0]), (0.3, [-60, 0, 30 * s], E_OUT), (1.0, [-150, 0, 10 * s], E_IN))
    fade.rot("hem", (0, [0, 0, 0]), (1.0, [-30, 0, 0], E_IN))

    flicker = f.new(A + "flicker", 0.5)
    rng = random.Random(1520)
    keys = [i * 0.05 for i in range(11)]
    jit = lambda a: [round(rng.uniform(-a, a), 1) for _ in range(3)]
    flicker.rot("head", *[(t, ([0, 0, 0] if t in (0, 0.5) else jit(22))) for t in keys])
    flicker.pos("body", *[(t, ([0, 0, 0] if t in (0, 0.5) else [round(rng.uniform(-1.2, 1.2), 1), 0, round(rng.uniform(-0.8, 0.8), 1)]))
                          for t in keys])
    flicker.rot("body", *[(t, ([0, 0, 0] if t in (0, 0.5) else jit(6))) for t in keys])
    for side in ("right_arm", "left_arm"):
        flicker.rot(side, *[(t, ([0, 0, 0] if t in (0, 0.5) else jit(18))) for t in keys])
    flicker.rot("jaw", (0, [0, 0, 0]), (0.1, [20, 0, 0]), (0.15, [0, 0, 0]), (0.3, [25, 0, 0]), (0.35, [0, 0, 0]), (0.5, [0, 0, 0]))
    return f


def generate():
    r, _ = rig()
    r.write(GEO + "ghost.geo.json")
    t, g = paint()
    save(t, "entity", "ghost")
    save(g, "entity", "ghost_glowmask")
    anims().write(ANIM + "ghost.animation.json")


if __name__ == "__main__":
    generate()
