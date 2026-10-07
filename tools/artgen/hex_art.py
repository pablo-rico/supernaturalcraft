"""The hex bag of cursing as a block (also its item model): a tiny pouch of dark, stained burlap
tied with red thread, a finger bone and a crow feather poking out of its neck, a sigil daubed on it
in old blood. (The bag of protection is a flat item sprite in items.py.)"""

import math
import random

import palette as P
from blockmodel import cube, model, write
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

BURLAP = Ramp("#1c1511", "#2c2219", "#3e3024", "#524031", "#66513e", "#7c654e")
THREAD = Ramp("#2a0606", "#4a0c0c", "#6b1212", "#8a1c19", "#a52a24", "#c23d33")
BONE = Ramp("#5e5440", "#857a60", "#a89d80", "#c8bea0", "#e0d8bd", "#f1ecd8")
FEATHER = Ramp("#050507", "#0c0c12", "#15151f", "#20202e", "#2e3346", "#465072")


def cloth_tex():
    """Coarse burlap: a woven grid, loose fibres, and (rows 10-11, cols 1-3) a mark daubed in dried blood."""
    t = Tex(16, 16, 1301)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1301, 16, 16, 2, 4.0)
            v = 2.7 + (n - 0.5) * 1.6 + (0.35 if (x % 2 == 0) != (y % 2 == 0) else -0.2)
            t.set(x, y, BURLAP[max(0, min(5, int(round(v))))])
    rng = random.Random(1302)
    for _ in range(10):  # loose fibres
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, BURLAP[5])
    dried = {"r": mix(THREAD[1], BURLAP[2], 0.35), "R": mix(THREAD[2], BURLAP[2], 0.3)}
    t.stamp(["R.r", ".r."], dried, 1, 10)
    return t


def gather_tex():
    """The gathered neck and frill seen from above: folds radiating from a dark mouth."""
    t = Tex(16, 16, 1311)
    for y in range(16):
        for x in range(16):
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            d = math.hypot(y + 0.5 - 8, x + 0.5 - 8)
            fold = math.cos(a * 7) * 0.9
            v = 2.4 + fold + (fbm(x, y, 1312, 16, 16, 2, 4.0) - 0.5) * 1.2 - (1.6 if d < 2.2 else 0)
            t.set(x, y, BURLAP[max(0, min(5, int(round(v))))])
    return t


def thread_tex():
    t = Tex(16, 16, 1321)
    for y in range(16):
        for x in range(16):
            t.set(x, y, THREAD[3] if (x + y) % 3 else THREAD[2])
        t.set(y, 0, THREAD[4])
    return t


def bone_tex():
    t = Tex(16, 16, 1331)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1331, 16, 16, 2, 4.0)
            t.set(x, y, BONE[max(1, min(5, int(round(3.4 + (n - 0.5) * 1.6))))])
    for y in range(16):
        t.set(0, y, BONE[2])
    return t


def feather_tex():
    """A crow feather on a transparent plane: dark vane with a blue sheen round a pale quill."""
    t = Tex(16, 16, 1341)
    for y in range(16):
        w = 2.8 * math.sin(min(1.0, (y + 1) / 16) * math.pi * 0.95) + 0.3
        for x in range(16):
            d = abs(x + 0.5 - 8)
            if y < 15 and d <= w:
                v = 2.0 + (1.6 if (x + y) % 4 == 0 else 0) - d * 0.3
                if (y * 7 + x * 3) % 11 == 0 and d > 1:
                    continue  # split barbs
                t.set(x, y, FEATHER[max(0, min(5, int(round(v))))])
        if y >= 2:
            t.set(8, y, mix(BONE[1], FEATHER[3], 0.4))
    return t


def bag_model():
    els = [
        cube("base", (6, 0, 6), (10, 0.5, 10), {"*": "cloth"}),
        cube("body", (5.5, 0.5, 5.5), (10.5, 2.8, 10.5), {"*": "cloth", "down": None},
             uv={"north": [0, 10, 5, 12.3]}),
        cube("shoulder", (6, 2.8, 6), (10, 3.4, 10), {"*": "cloth", "down": None, "up": "gather"}),
        cube("neck", (6.8, 3.4, 6.8), (9.2, 4.1, 9.2), {"*": "cloth", "down": None, "up": None}),
        cube("thread", (6.6, 3.5, 6.6), (9.4, 3.85, 9.4), {"*": "thread", "down": None, "up": None}),
        cube("thread_tail", (9.4, 2.4, 7.6), (9.75, 3.85, 7.95), {"*": "thread", "up": None}),
        cube("frill", (6.2, 4.1, 6.2), (9.8, 4.8, 9.8), {"*": "cloth", "down": "gather", "up": "gather"}),
        # A finger bone leaning out of the mouth, its knuckle on top.
        cube("bone", (8.2, 4.0, 7.3), (8.8, 6.6, 7.9), {"*": "bone", "down": None}, rot=("z", -22.5, (8.5, 4.2, 7.6))),
        cube("knuckle", (8.0, 6.4, 7.1), (9.0, 7.1, 8.1), {"*": "bone"}, rot=("z", -22.5, (8.5, 4.2, 7.6))),
        # The feather: a crossed pair of planes.
        cube("feather_a", (6.2, 4.2, 8.7), (8.2, 8.2, 8.7), {"north": "feather", "south": "feather"},
             uv={"north": [6, 0, 10, 16], "south": [6, 0, 10, 16]}, rot=("z", 22.5, (7.2, 4.2, 8.7))),
        cube("feather_b", (7.2, 4.2, 7.7), (7.2, 8.2, 9.7), {"east": "feather", "west": "feather"},
             uv={"east": [6, 0, 10, 16], "west": [6, 0, 10, 16]}, rot=("z", 22.5, (7.2, 4.2, 8.7))),
    ]
    return model({"cloth": "curse_bag_cloth", "gather": "curse_bag_gather", "thread": "curse_bag_thread",
                  "bone": "curse_bag_bone", "feather": "curse_bag_feather"}, els, "curse_bag_cloth")


def generate():
    save(cloth_tex(), "block", "curse_bag_cloth")
    save(gather_tex(), "block", "curse_bag_gather")
    save(thread_tex(), "block", "curse_bag_thread")
    save(bone_tex(), "block", "curse_bag_bone")
    save(feather_tex(), "block", "curse_bag_feather")
    write("curse_bag", bag_model())


if __name__ == "__main__":
    generate()
