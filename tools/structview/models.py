"""Block models for structview: blockstate -> variant/multipart -> model JSON (parent chain) -> quads.

A Quad is in block-local space (0..1 on each axis), vertices in vanilla order (TL, BL, BR, TR seen from outside), with its
UVs (0..1 over the texture), texture, tint, cull face, normal and render kind. Element rotations (origin/axis/angle/rescale),
face UV rotations and the variant's x/y rotations are applied as vanilla's FaceBakery and BlockModelRotation do.
"""

import math

import blockcolors

DIRS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
VEC2DIR = {v: k for k, v in DIRS.items()}


class Quad:
    __slots__ = ("verts", "uvs", "tex", "color", "cull", "normal", "kind", "shade", "plane", "tint")

    def __init__(self, verts, uvs, tex, color, cull, normal, kind, shade=True, tint=None):
        self.tint = tint        # RGB multiplier for textured mode (None: none)
        self.verts = verts
        self.uvs = uvs
        self.tex = tex
        self.color = color      # base RGB (texture average x tint) used in flat mode, and the tint in textured mode
        self.cull = cull        # direction name or None
        self.normal = normal
        self.kind = kind        # opaque | cutout | translucent
        self.shade = shade
        self.plane = None       # (axis, side) when the quad lies on a face of the block cell: for AO

    def moved(self, dx, dy, dz):
        q = Quad([(x + dx, y + dy, z + dz) for x, y, z in self.verts], self.uvs, self.tex, self.color, self.cull, self.normal,
                 self.kind, self.shade, self.tint)
        q.plane = self.plane
        return q


def parse_state(s):
    """'minecraft:oak_stairs[facing=north,half=top]' -> ('minecraft:oak_stairs', {'facing': 'north', 'half': 'top'})."""
    s = s.strip()
    props = {}
    b = s.find("[")
    if b >= 0:
        body = s[b + 1:s.rindex("]")]
        s = s[:b]
        for kv in body.split(","):
            if "=" in kv:
                k, v = kv.split("=", 1)
                props[k.strip()] = v.strip()
    if ":" not in s:
        s = "minecraft:" + s
    return s, props


# --- geometry helpers ----------------------------------------------------------------------------------------------------

def _rot_axis(p, axis, ang, origin, rescale):
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    c, s = math.cos(ang), math.sin(ang)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        # Right-handed rotation about +Y: z -> x.
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    if rescale:
        f = 1.0 / math.cos(ang) if abs(math.cos(ang)) > 1e-6 else 1.0
        if axis == "x":
            y *= f
            z *= f
        elif axis == "y":
            x *= f
            z *= f
        else:
            x *= f
            y *= f
    return (x + origin[0], y + origin[1], z + origin[2])


def _rot_vec(v, axis, ang):
    return _rot_axis(v, axis, ang, (0.0, 0.0, 0.0), False)


def model_rotate(p, rx, ry, centre=(0.5, 0.5, 0.5)):
    """Vanilla BlockModelRotation: rotate by -x about X, then by -y about Y (y is clockwise seen from above)."""
    if rx:
        p = _rot_axis(p, "x", math.radians(-rx), centre, False)
    if ry:
        p = _rot_axis(p, "y", math.radians(-ry), centre, False)
    return p


def model_rotate_vec(v, rx, ry):
    return model_rotate(v, rx, ry, (0.0, 0.0, 0.0))


def _snap(v):
    return tuple(round(c) if abs(c - round(c)) < 1e-4 else c for c in v)


def face_corners(face, x0, y0, z0, x1, y1, z1):
    """Corners TL, BL, BR, TR of a box face (vanilla FaceInfo order)."""
    if face == "north":
        return [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)]
    if face == "south":
        return [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)]
    if face == "west":
        return [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)]
    if face == "east":
        return [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)]
    if face == "up":
        return [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)]
    return [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)]


def default_uv(face, f, t):
    """Vanilla's default UVs (pixels) for a face of an element from f to t."""
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


def corner_uvs(uv, rotation):
    u0, v0, u1, v1 = [c / 16.0 for c in uv]
    base = [(u0, v0), (u0, v1), (u1, v1), (u1, v0)]
    shift = (rotation // 90) % 4
    return [base[(i + shift) % 4] for i in range(4)]


def plane_of(verts, normal):
    """(axis index, coordinate) when an axis-aligned quad lies on the cell's boundary."""
    for axis in range(3):
        if abs(abs(normal[axis]) - 1) < 1e-6:
            c = verts[0][axis]
            if all(abs(v[axis] - c) < 1e-6 for v in verts) and (abs(c) < 1e-6 or abs(c - 1) < 1e-6):
                return (axis, 1 if normal[axis] > 0 else -1)
    return None


# --- the model store -------------------------------------------------------------------------------------------------------

class Models:
    def __init__(self, assets):
        self.assets = assets
        self.report = blockcolors.report(assets.jar_path) or {}
        self._model = {}
        self._baked = {}

    # blockstates

    def defaults(self, bid):
        r = self.report.get(bid)
        return dict(r["default"]) if r else {}

    def full_props(self, bid, props):
        d = self.defaults(bid)
        d.update(props)
        return d

    def blockstate(self, bid):
        ns, path = blockcolors.split(bid)
        return self.assets.json("assets/%s/blockstates/%s.json" % (ns, path))

    def models_for(self, bid, props, seed=0):
        """[(model ref, x, y, uvlock)] for a block state, or None when it has no blockstate file."""
        bs = self.blockstate(bid)
        if bs is None:
            return None
        props = self.full_props(bid, props)
        out = []
        if "variants" in bs:
            chosen = None
            for key, val in bs["variants"].items():
                if _variant_matches(key, props):
                    chosen = val
                    break
            if chosen is None and bs["variants"]:
                chosen = next(iter(bs["variants"].values()))
            if chosen is not None:
                out.append(_pick(chosen, seed))
        for case in bs.get("multipart", []):
            when = case.get("when")
            if when is None or _when(when, props):
                out.append(_pick(case["apply"], seed))
        return [(m.get("model"), m.get("x", 0), m.get("y", 0), m.get("uvlock", False)) for m in out if m.get("model")]

    # models

    def model(self, ref):
        """(elements or None, textures dict, ambientocclusion) for a model ref, resolving the parent chain."""
        if ref in self._model:
            return self._model[ref]
        chain = []
        cur = ref
        seen = set()
        while cur and cur not in seen:
            seen.add(cur)
            if cur.startswith("builtin/") or cur.startswith("minecraft:builtin/"):
                break
            ns, path = blockcolors.split(cur)
            j = self.assets.json("assets/%s/models/%s.json" % (ns, path))
            if j is None:
                break
            chain.append(j)
            cur = j.get("parent")
        textures = {}
        elements = None
        ao = True
        for j in reversed(chain):
            textures.update(j.get("textures", {}))
            if "ambientocclusion" in j:
                ao = j["ambientocclusion"]
        for j in chain:
            if "elements" in j:
                elements = j["elements"]
                break
        res = (elements, textures, ao, bool(chain))
        self._model[ref] = res
        return res

    @staticmethod
    def resolve_texture(textures, name):
        seen = 0
        while name and name.startswith("#") and seen < 16:
            name = textures.get(name[1:])
            seen += 1
        if not name or name.startswith("#"):
            return None
        return name if ":" in name else "minecraft:" + name

    def bake(self, ref, rx, ry, block_path, props):
        """Quads of one model with the variant rotation; None if the model has no elements."""
        key = (ref, rx, ry, block_path, props.get("power"))
        if key in self._baked:
            return self._baked[key]
        elements, textures, _ao, exists = self.model(ref)
        if not elements:
            self._baked[key] = None
            return None
        quads = []
        for el in elements:
            f, t = el["from"], el["to"]
            rot = el.get("rotation")
            shade = el.get("shade", True)
            for face, fd in el.get("faces", {}).items():
                if face not in DIRS:
                    continue
                corners = face_corners(face, f[0] / 16, f[1] / 16, f[2] / 16, t[0] / 16, t[1] / 16, t[2] / 16)
                normal = DIRS[face]
                if rot:
                    ang = math.radians(rot.get("angle", 0))
                    org = [c / 16 for c in rot.get("origin", [8, 8, 8])]
                    corners = [_rot_axis(c, rot["axis"], ang, org, rot.get("rescale", False)) for c in corners]
                    normal = _rot_vec(normal, rot["axis"], ang)
                if rx or ry:
                    corners = [model_rotate(c, rx, ry) for c in corners]
                    normal = model_rotate_vec(normal, rx, ry)
                corners = [_snap(c) for c in corners]
                normal = _snap(normal)
                cull = fd.get("cullface")
                if cull in DIRS and (rx or ry):
                    cull = VEC2DIR.get(_snap(model_rotate_vec(DIRS[cull], rx, ry)))
                uv = fd.get("uv") or default_uv(face, f, t)
                uvs = corner_uvs(uv, fd.get("rotation", 0))
                tref = self.resolve_texture(textures, fd.get("texture", ""))
                tex = self.assets.texture(tref) if tref else None
                tint = blockcolors.tint(block_path, props) if fd.get("tintindex", -1) >= 0 else None
                if tex is None:
                    color, kind = (255, 0, 255), "opaque"
                else:
                    color, kind = tex.avg, tex.kind
                if tint:
                    color = (color[0] * tint[0] // 255, color[1] * tint[1] // 255, color[2] * tint[2] // 255)
                q = Quad(corners, uvs, tex, color, cull, normal, kind, shade, tint)
                q.plane = plane_of(corners, normal)
                quads.append(q)
        self._baked[key] = quads
        return quads

    def particle(self, ref):
        _el, textures, _ao, _e = self.model(ref)
        tref = self.resolve_texture(textures, "#particle")
        return self.assets.texture(tref) if tref else None


def _variant_matches(key, props):
    if key == "":
        return True
    for kv in key.split(","):
        if "=" not in kv:
            continue
        k, v = kv.split("=", 1)
        if k in props and props[k] != v:
            return False
    return True


def _when(cond, props):
    if "OR" in cond:
        return any(_when(c, props) for c in cond["OR"])
    if "AND" in cond:
        return all(_when(c, props) for c in cond["AND"])
    for k, v in cond.items():
        val = props.get(k)
        if val is None or str(val) not in str(v).split("|"):
            return False
    return True


def _pick(entry, seed):
    if isinstance(entry, list):
        total = sum(e.get("weight", 1) for e in entry)
        r = seed % max(1, total)
        for e in entry:
            r -= e.get("weight", 1)
            if r < 0:
                return e
        return entry[0]
    return entry
