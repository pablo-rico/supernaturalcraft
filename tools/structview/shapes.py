"""Geometry of a block state for structview.

Real model elements first (models.py); approximations for blocks vanilla draws with block-entity renderers (chests, beds,
signs, banners, skulls, decorated pots, shulker boxes, conduits, bells) and for fluids; a full cube of the average colour as
the last resort. `Shapes.info(state, seed)` returns a BlockInfo (quads in 0..1 block space plus the flags the renderer needs).
"""

import math

import blockcolors
from models import Models, Quad, DIRS, face_corners, default_uv, corner_uvs, plane_of, model_rotate, model_rotate_vec, _snap, \
    VEC2DIR, parse_state

INVISIBLE = {"air", "cave_air", "void_air", "light", "barrier", "structure_void", "moving_piston"}
GLASSY = ("glass", "ice", "slime_block", "honey_block")
WOODS = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped")
COLORS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black")
FACING_Y = {"south": 0, "west": 90, "north": 180, "east": 270}   # rotation (clockwise from above) of a south-facing build


class BlockInfo:
    __slots__ = ("quads", "occludes", "full", "caster", "glassy", "fluid", "bid", "path")

    def __init__(self, bid, quads):
        self.bid = bid
        self.path = blockcolors.split(bid)[1]
        self.quads = quads
        self.occludes = False
        self.full = False
        self.caster = False
        self.glassy = self.path.endswith(GLASSY) or self.path in GLASSY
        self.fluid = None


def box(x0, y0, z0, x1, y1, z1, tex=None, color=None, faces=None, tint=None, cull=True):
    """Quads of a box given in pixels; faces textured with `tex` (default UVs) or flat `color`."""
    out = []
    f, t = (x0, y0, z0), (x1, y1, z1)
    for face in faces or DIRS:
        corners = face_corners(face, x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16)
        uvs = corner_uvs([max(0, min(16, c)) for c in default_uv(face, f, t)], 0)
        if tex is not None:
            c = tex.avg
            kind = tex.kind if tex.kind != "translucent" else "translucent"
        else:
            c = color or (200, 0, 200)
            kind = "opaque"
        if tint:
            c = (c[0] * tint[0] // 255, c[1] * tint[1] // 255, c[2] * tint[2] // 255)
        q = Quad(corners, uvs, tex, c, face if cull and _on_edge(face, f, t) else None, DIRS[face], kind, True, tint)
        q.plane = plane_of(corners, DIRS[face])
        out.append(q)
    return out


def _on_edge(face, f, t):
    return {"north": f[2] <= 0, "south": t[2] >= 16, "west": f[0] <= 0, "east": t[0] >= 16, "down": f[1] <= 0, "up": t[1] >= 16}[face]


def rotate(quads, rx=0, ry=0, centre=(0.5, 0.5, 0.5)):
    out = []
    for q in quads:
        verts = [_snap(model_rotate(v, rx, ry, centre)) for v in q.verts]
        n = _snap(model_rotate_vec(q.normal, rx, ry))
        cull = VEC2DIR.get(_snap(model_rotate_vec(DIRS[q.cull], rx, ry))) if q.cull else None
        r = Quad(verts, q.uvs, q.tex, q.color, cull, n, q.kind, q.shade, q.tint)
        r.plane = plane_of(verts, n)
        out.append(r)
    return out


def rotate_free(quads, angle_deg, centre=(0.5, 0.5, 0.5)):
    """Rotation clockwise (from above) by any angle (standing signs and banners)."""
    a = math.radians(-angle_deg)
    c, s = math.cos(a), math.sin(a)
    out = []
    for q in quads:
        verts = []
        for x, y, z in q.verts:
            x -= centre[0]
            z -= centre[2]
            verts.append((x * c + z * s + centre[0], y, -x * s + z * c + centre[2]))
        nx, ny, nz = q.normal
        r = Quad(verts, q.uvs, q.tex, q.color, None, (nx * c + nz * s, ny, -nx * s + nz * c), q.kind, q.shade, q.tint)
        out.append(r)
    return out


def translate(quads, dx, dy, dz):
    out = []
    for q in quads:
        r = q.moved(dx, dy, dz)
        r.plane = None
        r.cull = None
        out.append(r)
    return out


class Shapes:
    def __init__(self, assets):
        self.assets = assets
        self.models = Models(assets)
        self._cache = {}
        self._variants = {}

    def tex(self, ref):
        return self.assets.texture(ref)

    def info(self, state, seed=0):
        key = (state, seed)
        r = self._cache.get(key)
        if r is None:
            r = self._build(state, seed)
            self._cache[key] = r
        return r

    def variants(self, state):
        """How many seeds give different looks (weighted random models): the renderer only varies the seed when > 1."""
        n = self._variants.get(state)
        if n is None:
            n = self._variants[state] = self._count_variants(state)
        return n

    def _count_variants(self, state):
        bid, props = parse_state(state)
        bs = self.models.blockstate(bid)
        if not bs or "variants" not in bs:
            return 1
        props = self.models.full_props(bid, props)
        for key, val in bs["variants"].items():
            from models import _variant_matches
            if _variant_matches(key, props):
                return len(val) if isinstance(val, list) else 1
        return 1

    def _build(self, state, seed):
        bid, props = parse_state(state)
        ns, path = blockcolors.split(bid)
        if path in INVISIBLE:
            return BlockInfo(bid, [])
        if path in ("water", "lava", "bubble_column"):
            info = BlockInfo(bid, [])
            info.fluid = "lava" if path == "lava" else "water"
            info.caster = path == "lava"
            return info
        quads = self._special(path, props)
        if quads is None:
            refs = self.models.models_for(bid, props, seed)
            quads = []
            if refs:
                any_elements = False
                for ref, rx, ry, _lock in refs:
                    q = self.models.bake(ref, rx, ry, path, props)
                    if path == "bell":
                        quads.extend(self.bell_body())
                    if q is not None:
                        any_elements = True
                        quads.extend(q)
                if not any_elements:
                    p = self.models.particle(refs[0][0])
                    quads = box(0, 0, 0, 16, 16, 16, tex=p) if p else box(0, 0, 0, 16, 16, 16, color=(200, 0, 200))
            else:
                quads = self._modded(ns, path)
        info = BlockInfo(bid, quads)
        self._classify(info)
        return info

    @staticmethod
    def _classify(info):
        full_faces = {}
        x0 = y0 = z0 = 9.0
        x1 = y1 = z1 = -9.0
        rotated = False
        for q in info.quads:
            for v in q.verts:
                x0, y0, z0 = min(x0, v[0]), min(y0, v[1]), min(z0, v[2])
                x1, y1, z1 = max(x1, v[0]), max(y1, v[1]), max(z1, v[2])
            if q.plane is None and not any(abs(abs(c) - 1) < 1e-6 for c in q.normal):
                rotated = True
            if q.plane is not None and q.cull is not None:
                xs = [v[0] for v in q.verts]
                ys = [v[1] for v in q.verts]
                zs = [v[2] for v in q.verts]
                area = (max(xs) - min(xs) or 1) * (max(ys) - min(ys) or 1) * (max(zs) - min(zs) or 1)
                if area > 0.999:
                    full_faces.setdefault(q.cull, []).append(q.kind)
        six = len(full_faces) == 6
        info.full = six and not info.glassy
        info.occludes = six and all("opaque" in kinds for kinds in full_faces.values())
        if info.quads and not rotated:
            info.caster = (x1 - x0) >= 0.5 and (z1 - z0) >= 0.5 and (y1 - y0) >= 0.3 and not all(q.kind == "translucent" for q in info.quads)

    # --- block-entity blocks ----------------------------------------------------------------------------------------------

    def _wood(self, path, suffixes):
        for s in suffixes:
            if path.endswith(s):
                w = path[:-len(s)]
                return self.tex("minecraft:block/%s_planks" % w) or self.tex("minecraft:block/oak_planks")
        return self.tex("minecraft:block/oak_planks")

    def _color_of(self, path, suffix):
        c = path[:-len(suffix)] if path.endswith(suffix) else "white"
        return self.tex("minecraft:block/%s_wool" % c) or self.tex("minecraft:block/white_wool")

    def _special(self, path, props):
        T = self.tex
        facing = props.get("facing", "south")
        if path.endswith("_bed"):
            wool = self._color_of(path, "_bed")
            legs = T("minecraft:block/oak_planks")
            white = T("minecraft:block/white_wool")
            head = props.get("part", "foot") == "head"
            # Built for a bed facing south (head toward +z), then turned.
            q = box(0, 3, 0, 16, 9, 16, tex=wool)
            q += box(0, 0, 13 if head else 0, 3, 3, 16 if head else 3, tex=legs)
            q += box(13, 0, 13 if head else 0, 16, 3, 16 if head else 3, tex=legs)
            if head:
                q += box(2, 9, 9, 14, 11, 15, tex=white)
            return rotate(q, ry=FACING_Y.get(facing, 0))
        if path in ("chest", "trapped_chest", "ender_chest"):
            ent = T("minecraft:entity/chest/%s" % {"chest": "normal", "trapped_chest": "trapped", "ender_chest": "ender"}[path])
            col = ent.avg if ent else (150, 105, 50)
            dark = tuple(int(c * 0.7) for c in col)
            typ = props.get("type", "single")
            x0, x1 = 1, 15
            # Built facing south; the partner of a double chest is clockwise (left) or counter-clockwise (right) of facing.
            if typ == "left":
                x0 = 0   # clockwise of south is west (-x)
            elif typ == "right":
                x1 = 16
            q = box(x0, 0, 1, x1, 9, 15, color=col) + box(x0, 9, 1, x1, 10, 15, color=dark) + box(x0, 10, 1, x1, 14, 15, color=col)
            lock = (7, 9) if typ == "single" else (0, 1) if typ == "left" else (15, 16)
            q += box(lock[0], 7, 15, lock[1], 11, 16, color=(190, 190, 190))
            return rotate(q, ry=FACING_Y.get(facing, 0))
        if path.endswith("shulker_box"):
            c = path[:-len("_shulker_box")] if path != "shulker_box" else ""
            t = T("minecraft:entity/shulker/shulker" + ("_" + c if c else ""))
            return box(0, 0, 0, 16, 16, 16, color=t.avg if t else (150, 100, 150))
        if path == "decorated_pot":
            t = T("minecraft:entity/decorated_pot/decorated_pot_side")
            col = t.avg if t else (150, 90, 65)
            return box(1, 0, 1, 15, 13, 15, color=col) + box(4, 13, 4, 12, 16, 12, color=tuple(int(c * 0.85) for c in col))
        if path == "conduit":
            return box(5, 5, 5, 11, 11, 11, tex=T("minecraft:block/prismarine"))
        if path == "bell":
            return None  # model elements (the frame) + the body below
        if path.endswith("_wall_hanging_sign"):
            w = self._wood(path, ["_wall_hanging_sign"])
            q = box(1, 0, 7, 15, 10, 9, tex=w) + box(0, 14, 6, 16, 16, 10, tex=w)
            return rotate(q, ry=FACING_Y.get(facing, 0))
        if path.endswith("_hanging_sign"):
            w = self._wood(path, ["_hanging_sign"])
            q = box(1, 0, 7, 15, 10, 9, tex=w) + box(3, 10, 7.5, 4, 16, 8.5, color=(60, 60, 70)) + box(12, 10, 7.5, 13, 16, 8.5, color=(60, 60, 70))
            return rotate_free(q, int(props.get("rotation", "0")) * 22.5)
        if path.endswith("_wall_sign"):
            w = self._wood(path, ["_wall_sign"])
            # Facing south = text faces south, board against the north side.
            return rotate(box(0, 4.5, 0, 16, 12.5, 2, tex=w), ry=FACING_Y.get(facing, 0))
        if path.endswith("_sign"):
            w = self._wood(path, ["_sign"])
            q = box(-4, 8, 7, 20, 20, 9, tex=w) + box(7, 0, 7, 9, 8, 9, tex=w)
            return rotate_free(q, int(props.get("rotation", "0")) * 22.5)
        if path.endswith("_wall_banner"):
            wool = self._color_of(path, "_wall_banner")
            q = box(-2, -14, 0.5, 18, 15, 1.5, tex=wool) + box(-2, 15, 0, 18, 16, 2, color=(110, 80, 50))
            return rotate(q, ry=FACING_Y.get(facing, 0))
        if path.endswith("_banner"):
            wool = self._color_of(path, "_banner")
            q = box(7, 0, 7, 9, 42, 9, color=(110, 80, 50)) + box(-2, 12, 9, 18, 40, 10, tex=wool) + box(-2, 40, 7.5, 18, 42, 9.5, color=(110, 80, 50))
            return rotate_free(q, int(props.get("rotation", "0")) * 22.5)
        if path.endswith("_skull") or path.endswith("_head"):
            col = {"skeleton": (215, 210, 195), "wither_skeleton": (45, 45, 45), "zombie": (80, 125, 60), "creeper": (95, 170, 75),
                   "dragon": (30, 30, 35), "player": (110, 75, 45), "piglin": (215, 155, 135)}
            kind = path.replace("_wall_skull", "").replace("_wall_head", "").replace("_skull", "").replace("_head", "")
            c = col.get(kind, (150, 150, 150))
            if "_wall_" in path:
                return rotate(box(4, 4, 0, 12, 12, 8, color=c), ry=FACING_Y.get(facing, 0))
            return rotate_free(box(4, 0, 4, 12, 8, 12, color=c), int(props.get("rotation", "0")) * 22.5)
        if path in ("end_portal",):
            return box(0, 12, 0, 16, 12.01, 16, color=(10, 10, 25), faces=["up"])
        if path == "end_gateway":
            return box(0, 0, 0, 16, 16, 16, color=(10, 10, 25))
        return None

    def bell_body(self):
        return box(5, 4, 5, 11, 11, 11, tex=self.tex("minecraft:block/gold_block"))

    def _modded(self, ns, path):
        return box(0, 0, 0, 16, 16, 16, color=(205, 60, 205))

    # --- fluids --------------------------------------------------------------------------------------------------------------

    def fluid_quads(self, kind, same_above, neighbour_open):
        """Fluid surface: `neighbour_open(dir)` says whether that side shows (not fluid, not opaque)."""
        if kind == "lava":
            tex = self.tex("minecraft:block/lava_still")
            tint = None
        else:
            tex = self.tex("minecraft:block/water_still")
            tint = blockcolors.WATER
        top = 16 if same_above else 14
        faces = [f for f in DIRS if neighbour_open(f) and (f != "up" or not same_above)]
        q = box(0, 0, 0, 16, top, 16, tex=tex, faces=faces, tint=tint)
        for x in q:
            x.kind = "translucent" if kind == "water" else "opaque"
            x.shade = True
        return q
