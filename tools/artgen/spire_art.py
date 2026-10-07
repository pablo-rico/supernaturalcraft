"""The Hymnal Spire's own pieces: the Choir Altar and its seven bells (block models and
textures), the Shattered Hymn, and the rest of what the Broken Chorus leaves behind.

Block models are written as JSON next to the textures they use; datagen only points at them.
"""

import json
import math
import os

import palette as P
from common import ASSETS, art, save
from pixelkit import Ramp, Tex, fbm, hexc, mix

O = P.OUTLINE
QUARTZ = Ramp("#8e877c", "#b9b2a6", "#d8d2c6", "#ebe6dc", "#f6f3ec", "#ffffff")
GOLD = P.GOLD
# The bells' notes, low to high (chorus/Melody.java): red, orange, yellow, green, cyan, blue, purple.
NOTES = ["red", "orange", "yellow", "green", "cyan", "blue", "purple"]
NOTE_RGB = ["#e8423a", "#f08a2c", "#f2d64a", "#7cd045", "#46c8d8", "#4466e0", "#a050e0"]
MODELS = os.path.join(ASSETS, "models", "block")


def _quartz(seed, w=16, h=16):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            n = fbm(x, y, seed, w, h, 2, 5.0)
            t.set(x, y, QUARTZ[int(round(3.3 + (n - 0.5) * 1.6))])
    return t


def altar_side():
    t = _quartz(201)
    for x in range(16):
        t.set(x, 0, GOLD[4])
        t.set(x, 1, GOLD[2])
        t.set(x, 15, GOLD[1])
        t.set(x, 14, GOLD[3] if x % 2 else GOLD[2])
    # An open eye carved in the middle, rimmed in gold, pupil of light.
    eye = ["....oooooo....",
           "..oo......oo..",
           ".o...gGGg...o.",
           "o...gGwwGg...o",
           ".o...gGGg...o.",
           "..oo......oo..",
           "....oooooo...."]
    pal = {"o": GOLD[1], "g": GOLD[3], "G": GOLD[5], "w": hexc("#ffffff")}
    t.stamp(eye, pal, 1, 5)
    return t


def altar_top():
    t = _quartz(202)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 5.2 < d < 6.6:
                t.set(x, y, GOLD[4] if (x + y) % 3 else GOLD[2])
            elif d < 3.2:
                t.set(x, y, mix(GOLD[5], hexc("#ffffff"), 0.5))
            if x in (0, 15) or y in (0, 15):
                t.set(x, y, GOLD[2])
    return t


def altar_bottom():
    return _quartz(203)


def bell(color):
    """Burnished gold, a band of the bell's colour round its waist, a dark mouth."""
    band = hexc(color)
    t = Tex(16, 16, 210)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 211, 16, 16, 2, 4.0)
            v = 3.2 + (n - 0.5) * 1.4 - (0.8 if x > 11 else 0) + (0.8 if x < 4 else 0)
            t.set(x, y, GOLD[int(round(max(0, min(5, v))))])
    for x in range(16):
        for y in (7, 8, 9):
            t.set(x, y, mix(band, hexc("#ffffff"), 0.25) if y == 7 else band if y == 8 else mix(band, hexc("#000000"), 0.3))
    for x in range(16):
        t.set(x, 15, GOLD[0])
    return t


def bell_frame():
    t = _quartz(220)
    for y in range(16):
        t.set(0, y, QUARTZ[1])
        t.set(15, y, QUARTZ[1])
    for x in range(16):
        t.set(x, 0, GOLD[3])
    return t


def _face(uv, tex, cull=None):
    f = {"uv": uv, "texture": tex}
    if cull:
        f["cullface"] = cull
    return f


def _el(name, frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), down_cull=False):
    out = {}
    for face in faces:
        if face in ("north", "south"):
            uv = [frm[0], 16 - to[1], to[0], 16 - frm[1]]
        elif face in ("east", "west"):
            uv = [frm[2], 16 - to[1], to[2], 16 - frm[1]]
        else:
            uv = [frm[0], frm[2], to[0], to[2]]
        out[face] = _face(uv, tex, "down" if face == "down" and down_cull else None)
    return {"name": name, "from": list(frm), "to": list(to), "faces": out}


def write_model(name, model):
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".json"), "w") as f:
        json.dump(model, f, indent=1)
        f.write("\n")


def altar_model():
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "supernaturalcraft:block/choir_altar_side", "side": "supernaturalcraft:block/choir_altar_side",
                     "top": "supernaturalcraft:block/choir_altar_top", "bottom": "supernaturalcraft:block/choir_altar_bottom"},
        "elements": [
            _el("base", (1, 0, 1), (15, 4, 15), "#bottom", down_cull=True),
            _el("column", (4, 4, 4), (12, 12, 12), "#side", ("north", "south", "east", "west")),
            _el("table", (2, 12, 2), (14, 15, 14), "#top"),
            _el("table_rim", (1.5, 14, 1.5), (14.5, 15.5, 14.5), "#side", ("north", "south", "east", "west")),
        ],
    }


def bell_model():
    """The shared shape; each bell's model only names its own texture."""
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#bell"},
        "elements": [
            _el("post_w", (1, 0, 7), (3, 15, 9), "#frame"),
            _el("post_e", (13, 0, 7), (15, 15, 9), "#frame"),
            _el("bar", (3, 13, 7), (13, 15, 9), "#frame"),
            _el("hanger", (7.5, 11, 7.5), (8.5, 13, 8.5), "#frame"),
            _el("crown", (5, 10, 5), (11, 11, 11), "#bell"),
            _el("body", (4, 4, 4), (12, 10, 12), "#bell"),
            _el("lip", (3, 2, 3), (13, 4, 13), "#bell"),
            _el("clapper", (7, 1, 7), (9, 3, 9), "#frame"),
        ],
    }


def shattered_hymn():
    """A torn page of golden score: one stave, three notes alight, the rest burnt away."""
    pal = {"o": O, "p": P.PARCHMENT[4], "P": P.PARCHMENT[5], "m": P.PARCHMENT[3], "d": P.PARCHMENT[1],
           "g": GOLD[3], "G": GOLD[5], "n": hexc("#fff4c0"), "e": hexc("#ff9a2a"), "E": hexc("#c43a0a")}
    return art([
        "................",
        "..oooooo........",
        ".oPPPPPpoo......",
        ".oPpppppppoo....",
        ".ogggggggggpo...",
        ".oppppnpppppo...",
        ".oggggGgggggo...",
        ".opnppppppnpeo..",
        ".oggggggGgggeEo.",
        ".opppppppppmeEo.",
        ".oggggggggmeEo..",
        ".opppppppmeEo...",
        ".ommmmmmmeEo....",
        "..oddddeEEo.....",
        "...oooooooo.....",
        "................",
    ], pal)


def choir_shard():
    """A shard of burnished rim, an eye still open in it."""
    pal = {"o": O, "g": GOLD[2], "G": GOLD[4], "h": GOLD[5], "d": GOLD[1], "w": hexc("#f4f6ff"), "b": hexc("#4f8ef0"), "k": hexc("#0a1024")}
    return art([
        "................",
        "..........oo....",
        ".........ohGo...",
        "........ohGGgo..",
        ".......ohGGggo..",
        "......ohGwwwgo..",
        ".....ohGwbkbwo..",
        "....ohGGwbbwgo..",
        "...ohGGggwwgdo..",
        "..ohGGgggggdo...",
        ".oGGgggggddo....",
        ".ogggdddddo.....",
        "..oddddooo......",
        "...ooooo........",
        "................",
        "................",
    ], pal)


def choir_trophy():
    """A wheel within a wheel of gold over the trophies' pedestal, one blue eye at the heart."""
    gold_t = Tex(16, 16, 230)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 231, 16, 16, 2, 4.0)
            gold_t.set(x, y, GOLD[int(round(3.0 + (n - 0.5) * 2))])
    eye = Tex(16, 16, 232)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            eye.set(x, y, hexc("#0a1024") if d < 2 else hexc("#4f8ef0") if d < 4.5 else hexc("#f4f6ff"))
    save(gold_t, "block", "choir_trophy_gold")
    save(eye, "block", "choir_trophy_eye")

    def el(name, frm, to, tex, uv=None):
        faces = {f: {"uv": uv or [0, 0, 16, 16], "texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}
        return {"name": name, "from": frm, "to": to, "faces": faces}
    elements = [
        el("pedestal", [4, 0, 4], [12, 3, 12], "base", [4, 4, 12, 12]),
        el("stem", [7, 3, 7], [9, 5, 9], "base", [7, 7, 9, 9]),
        el("eye", [6.5, 7.5, 6.5], [9.5, 10.5, 9.5], "eye", [5, 5, 11, 11]),
        # The outer wheel stands across the pedestal, the inner one turned a quarter.
        el("outer_top", [3, 14, 7.5], [13, 15, 8.5], "gold", [0, 0, 10, 1]),
        el("outer_bottom", [3, 4, 7.5], [13, 5, 8.5], "gold", [0, 1, 10, 2]),
        el("outer_left", [3, 5, 7.5], [4, 14, 8.5], "gold", [0, 2, 1, 11]),
        el("outer_right", [12, 5, 7.5], [13, 14, 8.5], "gold", [1, 2, 2, 11]),
        el("inner_top", [7.5, 12, 5], [8.5, 13, 11], "gold", [2, 2, 8, 3]),
        el("inner_bottom", [7.5, 6, 5], [8.5, 7, 11], "gold", [2, 3, 8, 4]),
        el("inner_front", [7.5, 7, 5], [8.5, 12, 6], "gold", [2, 4, 3, 9]),
        el("inner_back", [7.5, 7, 10], [8.5, 12, 11], "gold", [3, 4, 4, 9]),
    ]
    write_model("choir_trophy", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                 "textures": {"particle": "supernaturalcraft:block/trophy_base", "base": "supernaturalcraft:block/trophy_base",
                                              "gold": "supernaturalcraft:block/choir_trophy_gold", "eye": "supernaturalcraft:block/choir_trophy_eye"},
                                 "elements": elements})


def map_icon():
    """8x8 map marker: a gold spire with a bell of light at its tip."""
    pal = {"o": O, "g": GOLD[3], "G": GOLD[5], "w": hexc("#ffffff"), "q": QUARTZ[4]}
    return art([
        "...ww...",
        "...GG...",
        "..oGGo..",
        "..oqqo..",
        ".oqqqqo.",
        ".oqggqo.",
        "oggggggo",
        "oooooooo",
    ], pal)


def generate():
    save(altar_side(), "block", "choir_altar_side")
    save(altar_top(), "block", "choir_altar_top")
    save(altar_bottom(), "block", "choir_altar_bottom")
    save(bell_frame(), "block", "choir_bell_frame")
    write_model("choir_altar", altar_model())
    write_model("choir_bell", bell_model())
    for name, rgb in zip(NOTES, NOTE_RGB):
        save(bell(rgb), "block", f"choir_bell_{name}")
        write_model(f"choir_bell_{name}", {"parent": "supernaturalcraft:block/choir_bell",
                                           "textures": {"bell": f"supernaturalcraft:block/choir_bell_{name}",
                                                        "frame": "supernaturalcraft:block/choir_bell_frame"}})
    save(shattered_hymn(), "item", "shattered_hymn")
    save(map_icon(), "map/decorations", "hymnal_spire")
    save(choir_shard(), "item", "choir_shard")
    choir_trophy()


if __name__ == "__main__":
    generate()
