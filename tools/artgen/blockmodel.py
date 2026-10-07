"""Small helpers for vanilla JSON block models (elements with per-face textures and uvs).

    cube("skull", (6.5, 2, 1), (9.5, 4, 3.5), {"up": "skull_top", "*": "bone"}, rot=("y", 22.5, (8, 2, 2)))

`tex` maps a face name (or "*" for every other face) to a texture variable; a face mapped to None is
left out. `uv` optionally maps a face to an explicit uv; otherwise the face takes vanilla's default
(its own footprint on the texture), written out explicitly so models read the same in every tool.
"""

import json
import os

from common import ASSETS

MODELS = os.path.join(ASSETS, "models", "block")
FACES = ("north", "south", "east", "west", "up", "down")
NS = "supernaturalcraft:block/"


def default_uv(face, f, t):
    if face == "down":
        return [f[0], 16 - t[2], t[0], 16 - f[2]]
    if face == "up":
        return [f[0], f[2], t[0], t[2]]
    if face == "north":
        return [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]]
    if face == "south":
        return [f[0], 16 - t[1], t[0], 16 - f[1]]
    if face == "west":
        return [f[2], 16 - t[1], t[2], 16 - f[1]]
    return [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]]


def _r(v):
    return [round(float(x), 4) for x in v]


def cube(name, frm, to, tex, uv=None, rot=None, shade=True):
    faces = {}
    for face in FACES:
        var = tex.get(face, tex.get("*")) if isinstance(tex, dict) else tex
        if var is None:
            continue
        u = (uv or {}).get(face) or default_uv(face, frm, to)
        # Clamp default uvs of boxes that stick out of the block (vanilla wants 0..16).
        u = [min(16.0, max(0.0, x)) for x in u]
        faces[face] = {"uv": _r(u), "texture": "#" + var}
    e = {"name": name, "from": _r(frm), "to": _r(to), "faces": faces}
    if rot:
        axis, angle, origin = rot
        e["rotation"] = {"origin": _r(origin), "axis": axis, "angle": angle}
    if not shade:
        e["shade"] = False
    return e


def model(textures, elements, particle, cutout=True):
    m = {"parent": "minecraft:block/block"}
    if cutout:
        m["render_type"] = "minecraft:cutout"
    tx = {"particle": particle if ":" in particle else NS + particle}
    for k, v in textures.items():
        tx[k] = v if ":" in v else NS + v
    m["textures"] = tx
    m["elements"] = elements
    return m


def write(name, m):
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".json"), "w") as f:
        json.dump(m, f, indent=1)
        f.write("\n")
