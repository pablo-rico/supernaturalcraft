"""One giant key of an old typewriter, raining down in the Author's attack (TypewriterKeyEntity).

A round keycap (blank cream face: the renderer draws the letter on top) in a chrome rim, on a stem, with the
key lever running back (+Z) from its foot. About 1.2 blocks across; the keycap face is the top (+Y) plane at
y = 12 px, centred on x = z = 0; the origin is the entity's feet.
"""

import math

from chuck_art import GEO, Model, solid
from common import save
from pixelkit import Ramp, hexc, mix

CREAM = Ramp("#b9ab88", "#d2c5a2", "#e3d8b8", "#efe6cc", "#f7f0de", "#fffaf0")
CHROME = Ramp("#4b5258", "#6e777e", "#9aa3aa", "#c4ccd2", "#e2e8ec", "#ffffff")
BLACK = Ramp("#0c0c0e", "#16161a", "#222228", "#2e2e35", "#3b3b43", "#4a4a53")

CAP_R = 9.0
TOP = 12.0


def chrome(face, x, y, w, h):
    return CHROME[5] if y == 0 else CHROME[3.5 - (y % 3) * 0.6]


def rig():
    m = Model("typewriter_key", 256, 256)
    key = m.bone("key", (0, 0, 0))
    # The cap's body: overlapping slabs make it round; its face is one plane just above them.
    for k in range(6):
        m.cube(key, (-CAP_R + 0.75, 9, -3), (2 * CAP_R - 1.5, 2.75, 6), lambda f, x, y, w, h: CREAM[3] if f == "up" else BLACK[2],
               density=2, tag="cap_body", rotation=(0, k * 30, 0), pivot=(0, 0, 0))

    def face(f, x, y, w, h):
        r = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) / (w / 2)
        if r > 1:
            return None
        if r > 0.86:
            return CHROME[4]
        # A faint dish towards the middle, as on a worn glass-topped key.
        return mix(CREAM[5], CREAM[2], r ** 2)
    m.cube(key, (-CAP_R, TOP, -CAP_R), (2 * CAP_R, 0, 2 * CAP_R), face, faces=("up",), density=4, tag="cap_face")
    # The chrome rim round the cap.
    n = 24
    for k in range(n):
        m.cube(key, (CAP_R - 0.5, 8.5, -1.5), (1, 3.75, 3), chrome, density=2, tag="rim",
               rotation=(0, k * 360 / n, 0), pivot=(0, 0, 0))
    # Stem, a collar, and the lever running back to where the typebar would be.
    m.cube(key, (-1.5, 1.5, -1.5), (3, 7, 3), lambda f, x, y, w, h: CHROME[2 + (x % 2)], density=2, tag="stem")
    m.cube(key, (-2.5, 7.5, -2.5), (5, 1, 5), chrome, density=2, tag="collar")
    m.cube(key, (-1.5, 0, -1.5), (3, 2, 19), lambda f, x, y, w, h: BLACK[3] if y else BLACK[4], density=2, tag="lever")
    m.cube(key, (-1, 0, 16), (2, 6, 2), solid(BLACK[2]), density=2, tag="lever_end")
    return m


def generate():
    m = rig()
    t, _ = m.build(seed=1301)
    m.rig.write(GEO + "typewriter_key.geo.json")
    save(t, "entity", "typewriter_key")


if __name__ == "__main__":
    generate()
