"""The Empty's blocks (v0.41):

  empty_floor    pitch black, the faintest violet speckle (cube_all)
  empty_tar      a thin wet layer of black tar, a violet sheen in slow swirls (carpet-style: the lead's datagen uses it as "wool")
  empty_sleeper  a sleeper wrapped in black tar, lying along -Z (north) at rotation 0, in two block models like a vanilla bed:
                 `block/empty_sleeper_head` (the head and shoulders, the head toward north) and `block/empty_sleeper_foot` (hips
                 and legs, the feet toward south) -- 7 px high at most; textures `empty_sleeper_wrap` (the body: tar wound in
                 strands) and `empty_sleeper_face` (the head: a face's shape pressed up under the tar).
"""

import math

import blockmodel as BM
from chuck_art import h01
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

TAR = Ramp("#020203", "#060509", "#0b0910", "#120f19", "#1b1624", "#282034")
SHEEN = Ramp("#2a1f3e", "#3e2c5c", "#56407a", "#705898", "#8c74b4", "#b4a0d8")


def tex(fn, seed=0, w=16, h=16):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def floor():
    def f(x, y):
        n = fbm(x, y, 55001, 16, 16, 2, 6.0)
        c = TAR[0.6 + n * 1.2]
        r = h01("sp", x, y)
        if r < 0.025:
            c = SHEEN[1]
        elif r < 0.06:
            c = mix(c, SHEEN[0], 0.5)
        return c
    return tex(f, 55001)


def tar_layer():
    def f(x, y):
        n = fbm(x, y, 55002, 16, 16, 3, 5.0)
        c = TAR[1.4 + n * 2.0]
        s = math.sin(x * 0.7 + 3 * math.sin(y * 0.4) + n * 3)
        if s > 0.82:
            c = mix(c, SHEEN[2], (s - 0.82) * 3.5)
        if h01("bl", x, y) < 0.03:
            c = SHEEN[3]                                    # a wet glint
        return c
    return tex(f, 55002)


def wrap():
    """Tar wound round a body in strands, diagonals over and under, glossy ridges."""
    def f(x, y):
        band = (x + y * 0.6) % 5
        n = fbm(x, y, 55003, 16, 16, 2, 6.0)
        v = 1.6 + n * 1.4 + (1.2 if band < 1.0 else 0) - (0.6 if band > 3.6 else 0)
        c = TAR[v]
        if band < 0.6 and h01("gl", x, y) < 0.4:
            c = mix(c, SHEEN[2], 0.5)
        return c
    return tex(f, 55003)


def face():
    """The head end: the shape of a face pressed up under the tar -- brow, the hollows of the eyes, nose, a closed mouth."""
    rows = [
        "................",
        "................",
        "...hhhhhhhhhh...",
        "..hhhhhhhhhhhh..",
        "..hbbbbhhbbbbh..",
        "..hssshhhhsssh..",
        "..hhhhhnnhhhhh..",
        "..hhhhhnnhhhhh..",
        "..hhhhhhhhhhhh..",
        "...hhhmmmmhhh...",
        "...hhhhhhhhhh...",
        "....hhhhhhhh....",
        "................",
        "................",
        "................",
        "................",
    ]

    def f(x, y):
        n = fbm(x, y, 55004, 16, 16, 2, 6.0)
        base = TAR[1.4 + n]
        ch = rows[y][x]
        if ch == "b" or ch == "n":
            return mix(base, SHEEN[2], 0.45)                # the light catches the brow and nose
        if ch == "s":
            return TAR[0]                                   # the hollows of the eyes
        if ch == "m":
            return TAR[0]
        if ch == "h":
            return mix(base, SHEEN[1], 0.2)
        return base
    return tex(f, 55004)


def sleeper_models():
    W, F = "wrap", "face"
    head = [
        BM.cube("pool", (1, 0, 0), (15, 0.5, 16), {"up": W, "down": W, "*": W}),
        BM.cube("head", (4.5, 0.5, 1.5), (11.5, 6.5, 8.0), {"up": F, "*": W}, uv={"up": [2, 1, 14, 12]}),
        BM.cube("crown", (5.2, 1.0, 1.0), (10.8, 6.0, 1.5), {"*": W}),
        BM.cube("neck", (6.0, 0.5, 8.0), (10.0, 4.5, 9.0), {"*": W}),
        BM.cube("shoulders", (2.5, 0.5, 9.0), (13.5, 6.0, 16.0), {"*": W}),
        BM.cube("chest", (3.5, 6.0, 10.0), (12.5, 7.0, 16.0), {"*": W}),
    ]
    foot = [
        BM.cube("pool", (1, 0, 0), (15, 0.5, 16), {"up": W, "down": W, "*": W}),
        BM.cube("hips", (3.0, 0.5, 0.0), (13.0, 6.0, 5.0), {"*": W}),
        BM.cube("legs", (3.8, 0.5, 5.0), (12.2, 5.0, 12.5), {"*": W}),
        BM.cube("shins", (4.6, 0.5, 9.0), (11.4, 4.4, 13.0), {"*": W}),
        BM.cube("feet", (4.8, 0.5, 13.0), (11.2, 6.2, 14.6), {"*": W}),
    ]
    tx = {W: "empty_sleeper_wrap", F: "empty_sleeper_face"}
    BM.write("empty_sleeper_head", BM.model(tx, head, "empty_sleeper_wrap"))
    BM.write("empty_sleeper_foot", BM.model(tx, foot, "empty_sleeper_wrap"))


def generate():
    save(floor(), "block", "empty_floor")
    save(tar_layer(), "block", "empty_tar")
    save(wrap(), "block", "empty_sleeper_wrap")
    save(face(), "block", "empty_sleeper_face")
    sleeper_models()


if __name__ == "__main__":
    generate()
