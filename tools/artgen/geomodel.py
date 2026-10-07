"""Declarative GeckoLib (Bedrock 1.12.0) geometry with per-face UVs.

Conventions (worked out from GeckoLib's baking code, see the README):
  * Units are pixels, Y up, origin at the entity's feet. The model faces NORTH (-Z).
  * GeckoLib mirrors X when baking, so Bedrock +X (east) is the entity's LEFT and -X its
    RIGHT -- the vanilla Bedrock player puts rightArm at x = -5.
  * Every cube gets a classic box-unwrap region on the atlas:
        [ .  ][ up  ][down][ .  ]
        [east][north][west][south]
    so a material painted across the bottom row wraps continuously around the cube.
    Seen from outside, each face is unmirrored, v grows downward.
  * Rotations are Bedrock degrees (GeckoLib negates X and Y when baking):
      -X swings a hanging limb forward and tilts a head back (looks up);
      +Y turns toward the entity's right;
      +Z swings a hanging limb toward the entity's right (right arm outward, left arm inward).
"""

import json
import os


class Cube:
    def __init__(self, origin, size, inflate=0.0, paint=None, faces=None, tag=None, rotation=None, pivot=None, density=1):
        # Texels per unit on every face: more than 1 for fine painted detail (an engraving).
        self.density = int(density)
        self.origin = tuple(float(v) for v in origin)
        # Optional per-cube rotation (Bedrock degrees, same signs as bone rotations) about pivot.
        self.rotation = tuple(float(v) for v in rotation) if rotation else None
        self.pivot = tuple(float(v) for v in pivot) if pivot else None
        self.size = tuple(int(round(v)) for v in size)
        self.inflate = inflate
        self.paint = paint          # painter(tex, cube) called after packing
        self.faces = faces          # None = all six; otherwise a subset (flat planes)
        self.tag = tag
        self.box = None             # (u, v) of the unwrap region, set by pack()
        self.rects = {}             # face -> (u, v, w, h)

    def _face_dims(self):
        w, h, d = (v * self.density for v in self.size)
        return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}

    @property
    def unwrap_size(self):
        if self.faces and len(self.faces) == 1:
            # A lone face (a decal plane) needs only its own rectangle.
            return self._face_dims()[self.faces[0]]
        w, h, d = (v * self.density for v in self.size)
        return 2 * d + 2 * w, d + h

    def face_list(self):
        w, h, d = self.size
        faces = self.faces or ("north", "south", "east", "west", "up", "down")
        dims = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        return [f for f in faces if dims[f][0] > 0 and dims[f][1] > 0]

    def layout(self):
        u, v = self.box
        if self.faces and len(self.faces) == 1:
            fw, fh = self._face_dims()[self.faces[0]]
            self.rects = {self.faces[0]: (u, v, fw, fh)} if fw > 0 and fh > 0 else {}
            return
        w, h, d = (s * self.density for s in self.size)
        self.rects = {
            "up": (u + d, v, w, d),
            "down": (u + d + w, v, w, d),
            "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h),
            "west": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h),
        }
        self.rects = {f: r for f, r in self.rects.items() if f in self.face_list()}


class Bone:
    def __init__(self, name, pivot, parent=None, rotation=None, cubes=None, mirror=False):
        self.name = name
        self.pivot = tuple(float(v) for v in pivot)
        self.parent = parent
        self.rotation = rotation
        self.cubes = cubes or []


class Rig:
    def __init__(self, identifier, tex_w, tex_h):
        self.identifier = identifier
        self.tex_w, self.tex_h = tex_w, tex_h
        self.bones = []

    def bone(self, name, pivot, parent=None, rotation=None):
        b = Bone(name, pivot, parent, rotation)
        self.bones.append(b)
        return b

    def get(self, name):
        for b in self.bones:
            if b.name == name:
                return b
        raise KeyError(name)

    def translate(self, dx, dy, dz):
        """Moves every pivot and cube; used to centre item models in the item cube."""
        for b in self.bones:
            b.pivot = (b.pivot[0] + dx, b.pivot[1] + dy, b.pivot[2] + dz)
            for c in b.cubes:
                c.origin = (c.origin[0] + dx, c.origin[1] + dy, c.origin[2] + dz)

    def height_span(self):
        ys = [c.origin[1] for _, c in self.cubes()] + [c.origin[1] + c.size[1] for _, c in self.cubes()]
        return min(ys), max(ys)

    def cubes(self):
        for b in self.bones:
            for c in b.cubes:
                yield b, c

    # -- packing ---------------------------------------------------------------------------

    def pack(self, gutter=0):
        """Shelf-packs every cube's unwrap region, tallest first. Deterministic."""
        items = sorted(((c.unwrap_size, i, c) for i, (_, c) in enumerate(self.cubes())),
                       key=lambda t: (-t[0][1], -t[0][0], t[1]))
        x = y = shelf_h = 0
        for (w, h), _, c in items:
            if x + w > self.tex_w:
                x, y = 0, y + shelf_h + gutter
                shelf_h = 0
            if y + h > self.tex_h or w > self.tex_w:
                raise ValueError(f"{self.identifier}: atlas {self.tex_w}x{self.tex_h} too small")
            c.box = (x, y)
            c.layout()
            x += w + gutter
            shelf_h = max(shelf_h, h)

    # -- export ----------------------------------------------------------------------------

    def to_json(self):
        bones = []
        for b in self.bones:
            jb = {"name": b.name, "pivot": list(b.pivot)}
            if b.parent:
                jb["parent"] = b.parent
            if b.rotation:
                jb["rotation"] = list(b.rotation)
            if b.cubes:
                jb["cubes"] = []
                for c in b.cubes:
                    jc = {"origin": list(c.origin), "size": list(c.size),
                          "uv": {f: {"uv": [r[0], r[1]], "uv_size": [r[2], r[3]]} for f, r in c.rects.items()}}
                    if c.inflate:
                        jc["inflate"] = c.inflate
                    if c.rotation:
                        jc["rotation"] = list(c.rotation)
                        jc["pivot"] = list(c.pivot or (0, 0, 0))
                    jb["cubes"].append(jc)
            bones.append(jb)
        xs = [abs(c.origin[0]) + c.size[0] for _, c in self.cubes()] or [16]
        ys = [c.origin[1] + c.size[1] for _, c in self.cubes()] or [16]
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": "geometry." + self.identifier,
                    "texture_width": self.tex_w,
                    "texture_height": self.tex_h,
                    "visible_bounds_width": round(max(xs) * 2 / 16 + 1, 1),
                    "visible_bounds_height": round(max(ys) / 16 + 1, 1),
                    "visible_bounds_offset": [0, round(max(ys) / 32, 2), 0],
                },
                "bones": bones,
            }],
        }

    def write(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            json.dump(self.to_json(), f, indent=1)
            f.write("\n")

    def uvmap(self):
        return {f"{b.name}/{i}": c.rects for b in self.bones for i, c in enumerate(b.cubes)}


def box(rig_bone, origin, size, inflate=0.0, paint=None, faces=None, tag=None, rotation=None, pivot=None, density=1):
    c = Cube(origin, size, inflate, paint, faces, tag, rotation, pivot, density)
    rig_bone.cubes.append(c)
    return c
