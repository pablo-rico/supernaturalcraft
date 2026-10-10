#!/usr/bin/env python3
"""structview: renders a dumped build layout (build/layouts/<name>.json) to PNGs, with the vanilla 1.21.1 block models.

  python tools/structview/structview.py build/layouts/x.json --out build/layouts/png
  python tools/structview/structview.py --extract-ids --out src/test/resources/vanilla_blocks_1_21_1.txt

See README.md for the views, modes and options.
"""

import argparse
import json
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import blockcolors  # noqa: E402
import pngx  # noqa: E402
from shapes import Shapes, box  # noqa: E402
from models import DIRS, Quad, parse_state  # noqa: E402

SUN = (0.45, 1.0, 0.3)
AO_LEVELS = (0.52, 0.68, 0.84, 1.0)
SHADOW = 0.6
HORIZ = ("north", "east", "south", "west")
WATERY = {"seagrass", "tall_seagrass", "kelp", "kelp_plant", "bubble_column"}


def face_brightness(n):
    nx, ny, nz = n
    return min(1.0, nx * nx * 0.6 + ny * ny * (1.0 if ny > 0 else 0.5) + nz * nz * 0.8)


# --- the scene: every visible quad in world space, shaded once ----------------------------------------------------------

class WQuad:
    __slots__ = ("verts", "uvs", "tex", "color", "tint", "kind", "shades", "normal")

    def __init__(self, verts, uvs, tex, color, tint, kind, shades, normal):
        self.verts, self.uvs, self.tex, self.color, self.tint, self.kind, self.shades, self.normal = \
            verts, uvs, tex, color, tint, kind, shades, normal


class Scene:
    def __init__(self, shapes, cells, decor, ao=True, shadow=True):
        self.shapes = shapes
        self.cells = cells
        self.quads = []
        t0 = time.time()
        infos = {}
        fluid = {}
        for pos, st in cells.items():
            seed = 0
            info = shapes.info(st, 0)
            if shapes.variants(st) > 1:
                seed = (pos[0] * 3129871) ^ (pos[2] * 116129781) ^ (pos[1] * 7)
                seed = abs(seed) >> 3
                info = shapes.info(st, seed)
            infos[pos] = info
            if info.fluid:
                fluid[pos] = info.fluid
            elif "waterlogged=true" in st or info.path in WATERY:
                fluid.setdefault(pos, "water_logged")
        self.infos = infos
        occ = {p for p, i in infos.items() if i.occludes}
        full = {p for p, i in infos.items() if i.full}
        casters = {p for p, i in infos.items() if i.caster}
        self.maxy = max((p[1] for p in cells), default=0)
        shadow_cache = {}
        for pos, info in infos.items():
            x, y, z = pos
            quads = info.quads
            if info.fluid:
                kind = info.fluid
                above = fluid.get((x, y + 1, z))
                same_above = above is not None and (above == kind or kind == "water" and above == "water_logged")

                def open_side(d, x=x, y=y, z=z, kind=kind):
                    dx, dy, dz = DIRS[d]
                    n = (x + dx, y + dy, z + dz)
                    f = fluid.get(n)
                    if f == kind or (kind == "water" and f == "water_logged"):
                        return False
                    return n not in occ
                quads = self.shapes.fluid_quads(kind, same_above, open_side)
            for q in quads:
                if q.cull:
                    dx, dy, dz = DIRS[q.cull]
                    n = (x + dx, y + dy, z + dz)
                    if n in occ:
                        continue
                    if info.glassy:
                        ni = infos.get(n)
                        if ni is not None and ni.bid == info.bid:
                            continue
                verts = [(vx + x, vy + y, vz + z) for vx, vy, vz in q.verts]
                base = face_brightness(q.normal) if q.shade else 1.0
                # Sun shadow (one value per quad).
                sh = 1.0
                if shadow:
                    cx = sum(v[0] for v in verts) / 4 + q.normal[0] * 0.05
                    cy = sum(v[1] for v in verts) / 4 + q.normal[1] * 0.05
                    cz = sum(v[2] for v in verts) / 4 + q.normal[2] * 0.05
                    sh = self._shadow(cx, cy, cz, pos, casters, shadow_cache)
                shades = [base * sh] * 4
                if ao and q.plane is not None:
                    axis, side = q.plane
                    f = [x, y, z]
                    f[axis] += side
                    a1, a2 = [a for a in range(3) if a != axis]
                    shades = []
                    for i, v in enumerate(q.verts):
                        s1 = 1 if v[a1] > 0.5 else -1
                        s2 = 1 if v[a2] > 0.5 else -1
                        p1 = list(f)
                        p1[a1] += s1
                        p2 = list(f)
                        p2[a2] += s2
                        pc = list(p1)
                        pc[a2] += s2
                        o1 = tuple(p1) in full
                        o2 = tuple(p2) in full
                        oc = tuple(pc) in full
                        lvl = 0 if (o1 and o2) else 3 - (o1 + o2 + oc)
                        shades.append(base * sh * AO_LEVELS[lvl])
                self.quads.append(WQuad(verts, q.uvs, q.tex, q.color, q.tint, q.kind, shades, q.normal))
        self.build_time = time.time() - t0

    def _shadow(self, px, py, pz, own, casters, cache):
        sx, sy, sz = SUN
        n = math.sqrt(sx * sx + sy * sy + sz * sz)
        sx, sy, sz = sx / n * 0.5, sy / n * 0.5, sz / n * 0.5
        start = (math.floor(px), math.floor(py), math.floor(pz))
        key = None
        if start != own:
            key = start
            r = cache.get(key)
            if r is not None:
                return r
            px, py, pz = start[0] + 0.5, start[1] + 0.5, start[2] + 0.5
        maxy = self.maxy + 1
        x, y, z = px, py, pz
        res = 1.0
        last = start
        while y <= maxy:
            x += sx
            y += sy
            z += sz
            c = (math.floor(x), math.floor(y), math.floor(z))
            if c == last:
                continue
            last = c
            if c != own and c in casters:
                res = SHADOW
                break
        if key is not None:
            cache[key] = res
        return res

    def add(self, quads, shade_all=1.0):
        for q in quads:
            b = face_brightness(q.normal) * shade_all
            self.quads.append(WQuad(q.verts, q.uvs, q.tex, q.color, q.tint, q.kind, [b] * 4, q.normal))


# --- decor -------------------------------------------------------------------------------------------------------------

def _dirvec(d):
    return DIRS.get(d, (0, 0, 1))


def _ccw(d):
    return {"north": "west", "west": "south", "south": "east", "east": "north"}.get(d, "east")


def plate(assets, centre, facing, w, h, tex, color, inset):
    """A flat rectangle facing `facing` (a quad), centre given in world coords, w/h in blocks."""
    n = _dirvec(facing)
    if facing in ("up", "down"):
        a = (1, 0, 0)
        up = (0, 0, -1) if facing == "up" else (0, 0, 1)
    else:
        a = _dirvec(_ccw(facing))
        up = (0, 1, 0)
    cx, cy, cz = centre[0] + n[0] * inset, centre[1] + n[1] * inset, centre[2] + n[2] * inset
    hw, hh = w / 2, h / 2

    def p(sa, su):
        return (cx + a[0] * sa + up[0] * su, cy + a[1] * sa + up[1] * su, cz + a[2] * sa + up[2] * su)
    verts = [p(-hw, hh), p(-hw, -hh), p(hw, -hh), p(hw, hh)]
    uvs = [(0, 0), (0, 1), (1, 1), (1, 0)]
    col = tex.avg if tex is not None else color
    return Quad(verts, uvs, tex, col, None, n, tex.kind if tex is not None else "opaque", True)


def decor_quads(assets, shapes, decor):
    out = []
    for d in decor or []:
        kind = str(d.get("kind", "")).upper()
        x, y, z = d.get("x", 0), d.get("y", 0), d.get("z", 0)
        facing = d.get("facing") or "south"
        data = d.get("data") or ""
        n = _dirvec(facing)
        # The point on the wall behind the cell (the face opposite `facing`).
        wall = (x + 0.5 - n[0] * 0.5, y + 0.5 - n[1] * 0.5, z + 0.5 - n[2] * 0.5)
        try:
            if kind == "PAINTING":
                tex = assets.texture("minecraft:painting/" + data.split(":")[-1])
                if tex is None:
                    continue
                w, h = max(1, tex.w // 16), max(1, tex.h // 16)
                a = _dirvec(_ccw(facing))
                imin, imax = -((w - 1) // 2), w // 2
                jmin, jmax = -((h - 1) // 2), h // 2
                mid_i = (imin + imax) / 2
                mid_j = (jmin + jmax) / 2
                centre = (wall[0] + a[0] * mid_i, wall[1] + mid_j, wall[2] + a[2] * mid_i)
                out.append(plate(assets, centre, facing, w, h, tex, None, 1 / 16))
            elif kind == "ITEM_FRAME":
                frame = assets.texture("minecraft:block/" + ("glow_item_frame" if d.get("extra") == "glow" else "item_frame"))
                out.append(plate(assets, wall, facing, 0.75, 0.75, frame, (150, 110, 70), 1 / 32))
                item = data.split("|")[0]
                if item:
                    ns, p = blockcolors.split(item)
                    t = assets.texture("%s:item/%s" % (ns, p)) or assets.texture("%s:block/%s" % (ns, p))
                    if t is not None:
                        out.append(plate(assets, wall, facing, 0.5, 0.5, t, None, 2 / 32))
            elif kind == "BLOCK_DISPLAY" and data:
                info = shapes.info(data if ":" in data or "[" in data else "minecraft:" + data, 0)
                ex = [float(v) for v in (d.get("extra") or "").split(",") if v.strip()]
                sc = ex[0] if ex else 1.0
                tx, ty, tz = (ex[1:4] + [0.0, 0.0, 0.0])[:3] if len(ex) > 1 else (0.0, 0.0, 0.0)
                for qq in info.quads:
                    r = Quad([(x + tx + vx * sc, y + ty + vy * sc, z + tz + vz * sc) for vx, vy, vz in qq.verts], qq.uvs, qq.tex,
                             qq.color, None, qq.normal, qq.kind, qq.shade, qq.tint)
                    out.append(r)
            elif kind == "ITEM_DISPLAY" and data:
                ns, p = blockcolors.split(data.split("|")[0])
                t = assets.texture("%s:item/%s" % (ns, p)) or assets.texture("%s:block/%s" % (ns, p))
                ex = (d.get("extra") or "").split(",")[0].strip()
                sc = float(ex) if ex else 1.0
                if t is not None:
                    out.append(plate(assets, (x + 0.5, y + 0.5, z + 0.5), facing, 0.6 * sc, 0.6 * sc, t, None, 0))
            elif kind == "ARMOR_STAND":
                wood = assets.texture("minecraft:block/oak_planks")
                q = box(7, 1, 7, 9, 22, 9, tex=wood) + box(2, 0, 2, 14, 1, 14, tex=assets.texture("minecraft:block/smooth_stone"))
                q += box(3, 19, 7, 13, 21, 9, tex=wood)
                parts = data.split(",")
                if len(parts) > 1 and parts[1] not in ("", "-"):
                    q += box(4, 13, 6, 12, 22, 10, color=(150, 150, 160))
                if parts and parts[0] not in ("", "-"):
                    q += box(4.5, 22, 4.5, 11.5, 29, 11.5, color=(140, 140, 150))
                for qq in q:
                    out.append(qq.moved(x, y, z))
        except Exception as e:  # decoration never breaks a render
            print("structview: decor %s skipped (%s)" % (kind, e), file=sys.stderr)
    return out


# --- cameras -----------------------------------------------------------------------------------------------------------

def _norm(v):
    n = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) or 1.0
    return (v[0] / n, v[1] / n, v[2] / n)


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


class Ortho:
    persp = False
    hshade = None

    def __init__(self, d, up_hint, scale):
        self.d = _norm(d)
        self.r = _norm(_cross(self.d, up_hint))
        self.u = _cross(self.r, self.d)
        self.scale = scale
        self.ox = self.oy = 0.0

    def fit(self, pts, margin, top, min_w=560):
        xs = [_dot(p, self.r) for p in pts]
        ys = [-_dot(p, self.u) for p in pts]
        s = self.scale
        w = (max(xs) - min(xs)) * s + 2 * margin
        pad = max(0.0, (min_w - w) / 2)
        self.ox = -min(xs) * s + margin + pad
        self.oy = -min(ys) * s + margin + top
        self.W = int(w + 2 * pad)
        self.H = int((max(ys) - min(ys)) * s + 2 * margin + top)

    def project(self, p):
        return (_dot(p, self.r) * self.scale + self.ox, -_dot(p, self.u) * self.scale + self.oy, -_dot(p, self.d), 1.0)

    def facing(self, normal, p):
        return _dot(normal, self.d) < -1e-6


class Persp:
    persp = True

    def __init__(self, eye, look, W, H, vfov=70.0):
        self.eye = eye
        self.d = _norm((look[0] - eye[0], look[1] - eye[1], look[2] - eye[2]))
        self.r = _norm(_cross(self.d, (0, 1, 0)))
        self.u = _cross(self.r, self.d)
        self.W, self.H = W, H
        self.f = (H / 2) / math.tan(math.radians(vfov) / 2)
        self.near = 0.05

    def view(self, p):
        q = (p[0] - self.eye[0], p[1] - self.eye[1], p[2] - self.eye[2])
        return (_dot(q, self.r), _dot(q, self.u), _dot(q, self.d))

    def facing(self, normal, p):
        return _dot(normal, (p[0] - self.eye[0], p[1] - self.eye[1], p[2] - self.eye[2])) < -1e-6


# --- the rasteriser ----------------------------------------------------------------------------------------------------

class Frame:
    def __init__(self, W, H, persp):
        self.W, self.H = W, H
        self.fb = bytearray(W * H * 3)
        self.zb = [-1e30] * (W * H)
        self.eps = 1e-7 if persp else 1e-4
        self.translucent = []

    def background(self):
        W, H, fb = self.W, self.H, self.fb
        top, bot = (150, 186, 230), (226, 236, 246)
        for y in range(H):
            t = y / max(1, H - 1)
            row = bytes((int(top[0] + (bot[0] - top[0]) * t), int(top[1] + (bot[1] - top[1]) * t), int(top[2] + (bot[2] - top[2]) * t))) * W
            fb[y * W * 3:(y + 1) * W * 3] = row

    def tri(self, a, b, c, color, tex, tint, flat, blend):
        """a, b, c: (sx, sy, zkey, iw, u*iw, v*iw, shade). flat: draw `color` (alpha-tested by tex when it has holes)."""
        W, H = self.W, self.H
        x0, y0 = a[0], a[1]
        x1, y1 = b[0], b[1]
        x2, y2 = c[0], c[1]
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if -1e-9 < area < 1e-9:
            return
        if area < 0:
            b, c = c, b
            x1, y1, x2, y2 = x2, y2, x1, y1
            area = -area
        ymin = max(0, math.ceil(min(y0, y1, y2) - 0.5))
        ymax = min(H - 1, math.floor(max(y0, y1, y2) - 0.5))
        if ymin > ymax:
            return
        xlo = max(0, math.ceil(min(x0, x1, x2) - 0.5))
        xhi = min(W - 1, math.floor(max(x0, x1, x2) - 0.5))
        if xlo > xhi:
            return
        inv = 1.0 / area
        e1x, e1y, e2x, e2y = x1 - x0, y1 - y0, x2 - x0, y2 - y0

        def grad(f0, f1, f2):
            return ((f1 - f0) * e2y - (f2 - f0) * e1y) * inv, ((f2 - f0) * e1x - (f1 - f0) * e2x) * inv

        dzx, dzy = grad(a[2], b[2], c[2])
        dsx, dsy = grad(a[6], b[6], c[6])
        textured = tex is not None and (not flat or tex.kind != "opaque")
        if textured:
            dwx, dwy = grad(a[3], b[3], c[3])
            dux, duy = grad(a[4], b[4], c[4])
            dvx, dvy = grad(a[5], b[5], c[5])
            px, tw, th = tex.px, tex.w, tex.h
            twm, thm = tw - 1, th - 1
        edges = []
        for (ax, ay), (bx, by) in (((x0, y0), (x1, y1)), ((x1, y1), (x2, y2)), ((x2, y2), (x0, y0))):
            A = -(by - ay)
            B = bx - ax
            C = -B * ay - A * ax
            edges.append((A, B, C))
        zb, fb, eps = self.zb, self.fb, self.eps
        cr, cg, cb = color
        if tint is not None:
            tr, tg, tb = tint[0] / 255.0, tint[1] / 255.0, tint[2] / 255.0
        else:
            tr = tg = tb = 1.0
        for y in range(ymin, ymax + 1):
            yc = y + 0.5
            lo, hi = xlo, xhi
            for A, B, C in edges:
                t = B * yc + C
                if A > 1e-12:
                    v = math.ceil(-t / A - 0.5 - 1e-7)
                    if v > lo:
                        lo = v
                elif A < -1e-12:
                    v = math.floor(-t / A - 0.5 + 1e-7)
                    if v < hi:
                        hi = v
                elif t < -1e-9:
                    lo = hi + 1
                    break
            if lo > hi:
                continue
            dx0 = lo + 0.5 - x0
            dy0 = yc - y0
            z = a[2] + dzx * dx0 + dzy * dy0
            s = a[6] + dsx * dx0 + dsy * dy0
            i = y * W + lo
            if not textured:
                for _x in range(lo, hi + 1):
                    if z > zb[i] - eps:
                        if blend is None:
                            zb[i] = z
                            j = i * 3
                            fb[j] = min(255, int(cr * s))
                            fb[j + 1] = min(255, int(cg * s))
                            fb[j + 2] = min(255, int(cb * s))
                        else:
                            j = i * 3
                            al = blend
                            fb[j] = int(fb[j] * (1 - al) + min(255, cr * s) * al)
                            fb[j + 1] = int(fb[j + 1] * (1 - al) + min(255, cg * s) * al)
                            fb[j + 2] = int(fb[j + 2] * (1 - al) + min(255, cb * s) * al)
                    z += dzx
                    s += dsx
                    i += 1
                continue
            w = a[3] + dwx * dx0 + dwy * dy0
            uw = a[4] + dux * dx0 + duy * dy0
            vw = a[5] + dvx * dx0 + dvy * dy0
            for _x in range(lo, hi + 1):
                if z > zb[i] - eps:
                    iw = 1.0 / w if w != 0 else 0.0
                    tx = int(uw * iw * tw)
                    ty = int(vw * iw * th)
                    if tx < 0:
                        tx = 0
                    elif tx > twm:
                        tx = twm
                    if ty < 0:
                        ty = 0
                    elif ty > thm:
                        ty = thm
                    r, g, bb, al = px[ty * tw + tx]
                    if blend is None:
                        if al >= 128:
                            zb[i] = z
                            j = i * 3
                            if flat:
                                fb[j] = min(255, int(cr * s))
                                fb[j + 1] = min(255, int(cg * s))
                                fb[j + 2] = min(255, int(cb * s))
                            else:
                                fb[j] = min(255, int(r * tr * s))
                                fb[j + 1] = min(255, int(g * tg * s))
                                fb[j + 2] = min(255, int(bb * tb * s))
                    elif al > 8:
                        j = i * 3
                        f = al / 255.0 if not flat else blend
                        if flat:
                            r, g, bb = cr, cg, cb
                        else:
                            r, g, bb = r * tr, g * tg, bb * tb
                        fb[j] = int(fb[j] * (1 - f) + min(255, r * s) * f)
                        fb[j + 1] = int(fb[j + 1] * (1 - f) + min(255, g * s) * f)
                        fb[j + 2] = int(fb[j + 2] * (1 - f) + min(255, bb * s) * f)
                z += dzx
                s += dsx
                w += dwx
                uw += dux
                vw += dvx
                i += 1


def render(scene, cam, textured):
    fr = Frame(cam.W, cam.H, cam.persp)
    fr.background()
    if not cam.persp and getattr(cam, "grid", None):
        cam.grid(fr)
    trans = []
    for q in scene.quads:
        if not cam.facing(q.normal, q.verts[0]):
            continue
        pts = project_quad(cam, q)
        if pts is None:
            continue
        if q.kind == "translucent":
            depth = sum(p[2] for p in pts) / len(pts)
            trans.append((depth, pts, q))
            continue
        draw_poly(fr, pts, q, textured, None)
    # Back to front: the smallest z key (farthest) first.
    trans.sort(key=lambda t: t[0])
    for _d, pts, q in trans:
        draw_poly(fr, pts, q, textured, 0.62)
    return fr


def draw_poly(fr, pts, q, textured, blend):
    flat = not textured or q.tex is None
    tex = q.tex
    color = q.color
    for k in range(1, len(pts) - 1):
        fr.tri(pts[0], pts[k], pts[k + 1], color, tex, q.tint, flat, blend)


def project_quad(cam, q):
    if not cam.persp:
        out = []
        hs = cam.hshade
        for (p, (u, v), s) in zip(q.verts, q.uvs, q.shades):
            sx, sy, z, _ = cam.project(p)
            if hs is not None:
                # Top view: lower surfaces darker, so terraces and roof slopes read.
                s *= 0.55 + 0.45 * min(1.0, max(0.0, (p[1] - hs[0]) / hs[1]))
            out.append((sx, sy, z, 1.0, u, v, s))
        return out
    # Perspective: view space, clip against the near plane, then project.
    vs = []
    for (p, (u, v), s) in zip(q.verts, q.uvs, q.shades):
        x, y, z = cam.view(p)
        vs.append((x, y, z, u, v, s))
    near = cam.near
    if all(v[2] < near for v in vs):
        return None
    if any(v[2] < near for v in vs):
        clipped = []
        n = len(vs)
        for i in range(n):
            a, b = vs[i], vs[(i + 1) % n]
            ain, bin_ = a[2] >= near, b[2] >= near
            if ain:
                clipped.append(a)
            if ain != bin_:
                t = (near - a[2]) / (b[2] - a[2])
                clipped.append(tuple(a[k] + (b[k] - a[k]) * t for k in range(6)))
        vs = clipped
        if len(vs) < 3:
            return None
    out = []
    W2, H2, f = cam.W / 2, cam.H / 2, cam.f
    for x, y, z, u, v, s in vs:
        iw = 1.0 / z
        out.append((W2 + x * f * iw, H2 - y * f * iw, iw, iw, u * iw, v * iw, s))
    return out


# --- text ---------------------------------------------------------------------------------------------------------------

FONT = {
    "A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110", "E": "111100110100111",
    "F": "111100110100100", "G": "011100101101011", "H": "101101111101101", "I": "111010010010111", "J": "001001001101010",
    "K": "101101110101101", "L": "100100100100111", "M": "101111101101101", "N": "101111111111101", "O": "010101101101010",
    "P": "110101110100100", "Q": "010101101110011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
    "U": "101101101101111", "V": "101101101101010", "W": "101101111111101", "X": "101101010101101", "Y": "101101010010010",
    "Z": "111001010100111", "0": "111101101101111", "1": "010110010010111", "2": "110001010100111", "3": "110001010001110",
    "4": "101101111001001", "5": "111100110001110", "6": "011100111101111", "7": "111001010010010", "8": "111101111101111",
    "9": "111101111001110", " ": "000000000000000", "-": "000000111000000", "_": "000000000000111", ".": "000000000000010",
    ":": "000010000010000", ",": "000000000010100", "=": "000111000111000", "/": "001001010100100", "(": "010100100100010",
    ")": "010001001001010", "[": "110100100100110", "]": "011001001001011", "#": "101111101111101", "+": "000010111010000",
    "<": "001010100010001", ">": "100010001010100", "|": "010010010010010",
}


def text(fr, x, y, s, color=(20, 24, 32), scale=2):
    W, H, fb = fr.W, fr.H, fr.fb
    cx = x
    for ch in s.upper():
        g = FONT.get(ch, FONT["#"])
        for k, bit in enumerate(g):
            if bit != "1":
                continue
            gx, gy = k % 3, k // 3
            for ay in range(scale):
                for ax in range(scale):
                    px, py = cx + gx * scale + ax, y + gy * scale + ay
                    if 0 <= px < W and 0 <= py < H:
                        j = (py * W + px) * 3
                        fb[j:j + 3] = bytes(color)
        cx += 4 * scale
    return cx


def line(fr, x0, y0, x1, y1, color):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for k in range(n + 1):
        t = k / max(1, n)
        x, y = int(x0 + (x1 - x0) * t), int(y0 + (y1 - y0) * t)
        if 0 <= x < fr.W and 0 <= y < fr.H:
            j = (y * fr.W + x) * 3
            fr.fb[j:j + 3] = bytes(color)


def compass(fr, cam):
    if cam.persp:
        north = _norm((cam.d[0], 0, cam.d[2]))
        # In a perspective view only say which way the camera looks.
        names = {(0, -1): "N", (0, 1): "S", (1, 0): "E", (-1, 0): "W"}
        k = (round(north[0]), round(north[2]))
        text(fr, fr.W - 120, fr.H - 18, "LOOKING " + names.get(k, "?"))
        return
    cx, cy = 40, fr.H - 40
    for vec, label, col, ln in (((0, 0, -1), "N", (190, 30, 30), 24), ((1, 0, 0), "E", (40, 40, 40), 18),
                                ((0, 0, 1), "", (90, 90, 90), 12), ((-1, 0, 0), "", (90, 90, 90), 12)):
        vx, vy = _dot(vec, cam.r), -_dot(vec, cam.u)
        n = math.hypot(vx, vy)
        if n < 1e-6:
            continue
        vx, vy = vx / n, vy / n
        for ox, oy in ((0, 0), (1, 0), (0, 1)):
            line(fr, cx + ox, cy + oy, cx + vx * ln + ox, cy + vy * ln + oy, col)
        if label:
            text(fr, int(cx + vx * (ln + 9)) - 3, int(cy + vy * (ln + 9)) - 5, label, col)


# --- driver -------------------------------------------------------------------------------------------------------------

def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def select(layout, cutaway=None, zone=None, detail=None, ymin=None):
    cells = {}
    zb = None
    if zone:
        for z in layout.get("zones", []):
            if z.get("name") == zone:
                zb = (z["min"], z["max"])
        if zb is None:
            raise SystemExit("no zone %r (zones: %s)" % (zone, ", ".join(z.get("name", "?") for z in layout.get("zones", []))))
    for c in layout.get("cells", []):
        x, y, z, st = c[0], c[1], c[2], c[3]
        if cutaway is not None and y > cutaway or ymin is not None and y < ymin:
            continue
        if zb and not (zb[0][0] <= x <= zb[1][0] and zb[0][1] <= y <= zb[1][1] and zb[0][2] <= z <= zb[1][2]):
            continue
        if detail and not (detail[0] <= x <= detail[2] and detail[1] <= z <= detail[3]):
            continue
        p = st.split("[", 1)[0]
        if p in ("minecraft:air", "air", "minecraft:cave_air", "minecraft:void_air"):
            continue
        cells[(x, y, z)] = st
    decor = []
    for d in layout.get("decor", []) or []:
        x, y, z = d.get("x", 0), d.get("y", 0), d.get("z", 0)
        if cutaway is not None and y > cutaway or ymin is not None and y < ymin:
            continue
        if zb and not (zb[0][0] <= x <= zb[1][0] and zb[0][1] <= y <= zb[1][1] and zb[0][2] <= z <= zb[1][2]):
            continue
        if detail and not (detail[0] <= x <= detail[2] and detail[1] <= z <= detail[3]):
            continue
        decor.append(d)
    return cells, decor


def bounds_of(scene):
    xs, ys, zs = [], [], []
    for q in scene.quads:
        for v in q.verts:
            xs.append(v[0])
            ys.append(v[1])
            zs.append(v[2])
    if not xs:
        return (0, 0, 0), (1, 1, 1)
    return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))


def ortho_camera(view, scene, scale, target=1400, max_h=1600, min_scale=2, max_scale=32):
    if view == "iso_ne":
        d = (-math.cos(math.radians(30)) / math.sqrt(2), -math.sin(math.radians(30)), math.cos(math.radians(30)) / math.sqrt(2))
        up = (0, 1, 0)
    elif view == "iso_sw":
        d = (math.cos(math.radians(30)) / math.sqrt(2), -math.sin(math.radians(30)), -math.cos(math.radians(30)) / math.sqrt(2))
        up = (0, 1, 0)
    elif view == "iso_nw":
        d = (math.cos(math.radians(30)) / math.sqrt(2), -math.sin(math.radians(30)), math.cos(math.radians(30)) / math.sqrt(2))
        up = (0, 1, 0)
    elif view == "iso_se":
        d = (-math.cos(math.radians(30)) / math.sqrt(2), -math.sin(math.radians(30)), -math.cos(math.radians(30)) / math.sqrt(2))
        up = (0, 1, 0)
    else:  # top
        d = (0, -1, 0)
        up = (0, 0, -1)
    lo, hi = bounds_of(scene)
    cam = Ortho(d, up, 1.0)
    # Frame the geometry itself (not its bounding box): the extreme vertices along the screen axes.
    corners = []
    for key in (lambda p: _dot(p, cam.r), lambda p: _dot(p, cam.u)):
        pts = [v for q in scene.quads for v in q.verts]
        if pts:
            corners.append(min(pts, key=key))
            corners.append(max(pts, key=key))
    if not corners:
        corners = [lo, hi]
    xs = [_dot(p, cam.r) for p in corners]
    ys = [_dot(p, cam.u) for p in corners]
    ew, eh = max(xs) - min(xs), max(ys) - min(ys)
    if scale is None:
        s = target / max(ew, 1e-3)
        s = min(s, max_h / max(eh, 1e-3))
        s = max(min_scale, min(max_scale, int(s)))
    else:
        s = scale
    cam.scale = s
    cam.fit(corners, 24, 26)
    if view == "top":
        tops = [v[1] for q in scene.quads if q.normal[1] > 0.5 for v in q.verts[:1]]
        tlo = min(tops) if tops else lo[1]
        cam.hshade = (tlo, max(1.0, hi[1] - tlo))

        def grid(fr, cam=cam, lo=lo, hi=hi):
            for gx in range(int(math.floor(lo[0] / 8)) * 8, int(hi[0]) + 1, 8):
                sx = int(cam.project((gx, 0, 0))[0])
                line(fr, sx, 26, sx, fr.H - 1, (205, 215, 230) if gx else (240, 120, 120))
            for gz in range(int(math.floor(lo[2] / 8)) * 8, int(hi[2]) + 1, 8):
                sy = int(cam.project((0, 0, gz))[1])
                line(fr, 0, sy, fr.W - 1, sy, (205, 215, 230) if gz else (240, 120, 120))
        cam.grid = grid
    return cam


def zone_box(layout, name):
    for z in layout.get("zones", []) or []:
        if z.get("name") == name:
            return z["min"], z["max"]
    raise SystemExit("no zone %r (zones: %s)" % (name, ", ".join(z.get("name", "?") for z in layout.get("zones", []) or [])))


def column_top(scene, x, z, near):
    """The standing height (first air) over the highest solid block of column (x, z) within 4 of `near`, or None."""
    best = None
    for (cx, cy, cz), info in scene.infos.items():
        if cx == x and cz == z and info.caster and abs(cy + 1 - near) <= 4:
            if best is None or cy + 1 > best:
                best = cy + 1
    return best


def front_camera(scene, layout, side, eye=None, look=None, W=1600, H=1000, distance=None, target=None):
    if eye and look:
        return Persp(eye, look, W, H)
    htan = math.tan(math.radians(35)) * W / H
    if target:
        zmin, zmax = zone_box(layout, target)
        lo, hi = (zmin[0], zmin[1], zmin[2]), (zmax[0] + 1, zmax[1] + 1, zmax[2] + 1)
        stand = zmin[1] + 2   # the zone's floor block is at min y + 1
    else:
        lo, hi = bounds_of(scene)
        anchors = layout.get("anchors") or {}
        if "entry" in anchors:
            stand = anchors["entry"][1]
        elif lo[1] <= 0 <= hi[1]:
            stand = 0
        else:
            stand = lo[1] + 1
    cx, cz = (lo[0] + hi[0]) / 2, (lo[2] + hi[2]) / 2
    along = side in ("south", "north")
    width = (hi[0] - lo[0]) if along else (hi[2] - lo[2])
    depth = (hi[2] - lo[2]) if along else (hi[0] - lo[0])
    if distance:
        dist = distance
    elif target:
        dist = max(1.2 * width, depth / 2 + 3)          # from the zone's centre
    else:
        dist = depth / 2 + max(4.0, (width / 2) / (htan * 0.78))
    sx, sz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[side]
    ex, ez = cx + sx * dist, cz + sz * dist
    ground = column_top(scene, math.floor(ex), math.floor(ez), stand) if target else None
    ey = (ground if ground is not None else stand) + 1.6
    ty = ey + max(0.0, (hi[1] - ey)) * (0.35 if target else 0.3)
    return Persp((ex, ey, ez), (cx, ty, cz), W, H)


def save(fr, path, title):
    text(fr, 8, 7, title)
    pngx.write(path, fr.W, fr.H, fr.fb)


def main(argv=None):
    ap = argparse.ArgumentParser(description="Render a dumped build layout to PNGs (vanilla 1.21.1 models).")
    ap.add_argument("layout", nargs="?", help="build/layouts/<name>.json")
    ap.add_argument("--out", help="output directory (render) or file (--extract-ids)")
    ap.add_argument("--views", default="iso_ne,iso_sw,top,front", help="iso_ne,iso_sw,iso_nw,iso_se,top,front")
    ap.add_argument("--scale", type=float, help="pixels per block for the orthographic views (default: auto)")
    ap.add_argument("--mode", choices=("auto", "flat", "textured"), default="auto",
                    help="flat: average colour per face; textured: block textures (auto: textured from 8 px/block)")
    ap.add_argument("--textures", action="store_true", help="same as --mode textured")
    ap.add_argument("--cutaway", help="y=H: drop cells above H")
    ap.add_argument("--zone", help="render only that zone of the layout (the front view is framed on it)")
    ap.add_argument("--target", help="front view: frame the camera on this zone, keeping every cell (terrain around it)")
    ap.add_argument("--ymin", type=int, help="drop cells below this y (all views and --detail)")
    ap.add_argument("--detail", help="x0,z0,x1,z1: a textured close-up of those columns")
    ap.add_argument("--detail-scale", type=float, help="px/block of --detail (default: auto, ~1400 px wide, 16..48)")
    ap.add_argument("--detail-view", default="iso_ne")
    ap.add_argument("--front-from", default="south", choices=("south", "north", "east", "west"))
    ap.add_argument("--distance", type=float, help="front view: the camera's distance from the build")
    ap.add_argument("--eye", help="x,y,z of a free perspective camera (with --look)")
    ap.add_argument("--look", help="x,y,z the free camera looks at")
    ap.add_argument("--size", default="1600x1000", help="perspective image size")
    ap.add_argument("--no-ao", action="store_true")
    ap.add_argument("--no-shadow", action="store_true")
    ap.add_argument("--jar", help="the Minecraft 1.21.1 client jar")
    ap.add_argument("--extract-ids", action="store_true", help="write the vanilla block id/property list and exit")
    a = ap.parse_args(argv)

    if a.extract_ids:
        txt = blockcolors.extract_ids(a.jar)
        out = a.out or "vanilla_blocks_1_21_1.txt"
        with open(out, "w", encoding="utf-8", newline="\n") as f:
            f.write(txt)
        print("wrote %s (%d blocks)" % (out, txt.count("\n") - 2))
        return 0
    if not a.layout:
        ap.error("a layout JSON is required")
    layout = load(a.layout)
    name = layout.get("name") or os.path.splitext(os.path.basename(a.layout))[0]
    out_dir = a.out or os.path.join(os.path.dirname(os.path.abspath(a.layout)), "png")
    os.makedirs(out_dir, exist_ok=True)
    cut = None
    if a.cutaway:
        cut = int(a.cutaway.split("=", 1)[-1])
    suffix = ("_cut%d" % cut if cut is not None else "") + ("_from%d" % a.ymin if a.ymin is not None else "") + \
        ("_" + a.zone if a.zone else "") + ("_at_" + a.target if a.target else "")
    assets = blockcolors.Assets(a.jar)
    shapes = Shapes(assets)
    W, H = [int(v) for v in a.size.lower().split("x")]
    mode = "textured" if a.textures else a.mode

    def build_scene(detail=None):
        cells, decor = select(layout, cut, a.zone, detail, a.ymin)
        sc = Scene(shapes, cells, decor, ao=not a.no_ao, shadow=not a.no_shadow)
        sc.add(decor_quads(assets, shapes, decor))
        return sc

    t0 = time.time()
    if a.detail:
        x0, z0, x1, z1 = [int(v) for v in a.detail.split(",")]
        sc = build_scene((min(x0, x1), min(z0, z1), max(x0, x1), max(z0, z1)))
        cam = ortho_camera(a.detail_view, sc, a.detail_scale, min_scale=16, max_scale=48)
        fr = render(sc, cam, True)
        compass(fr, cam)
        path = os.path.join(out_dir, "%s_detail%s.png" % (name, suffix))
        save(fr, path, "%s  DETAIL %s  %s" % (name, a.detail, a.detail_view))
        print("%s  (%dx%d, %.1fs)" % (path, fr.W, fr.H, time.time() - t0))
        return 0
    sc = build_scene()
    print("scene: %d cells, %d quads (%.1fs)" % (len(sc.cells), len(sc.quads), sc.build_time))
    views = [v.strip() for v in a.views.split(",") if v.strip()]
    if a.eye and a.look:
        views = ["eye"]
    for v in views:
        t1 = time.time()
        if v in ("front", "eye"):
            eye = tuple(float(c) for c in a.eye.split(",")) if a.eye else None
            look = tuple(float(c) for c in a.look.split(",")) if a.look else None
            cam = front_camera(sc, layout, a.front_from, eye, look, W, H, a.distance, a.target or a.zone)
            textured = mode != "flat"
            label = "%s  %s  FROM %s" % (name, v.upper(), a.front_from.upper()) if v == "front" else "%s  EYE" % name
            if v == "front" and (a.target or a.zone):
                label += "  AT " + (a.target or a.zone)
        else:
            cam = ortho_camera(v, sc, a.scale)
            textured = mode == "textured" or (mode == "auto" and cam.scale >= 8)
            label = "%s  %s  %g PX/BLOCK" % (name, v.upper(), cam.scale)
        if cut is not None:
            label += "  CUT Y=%d" % cut
        if a.zone:
            label += "  ZONE " + a.zone
        fr = render(sc, cam, textured)
        compass(fr, cam)
        path = os.path.join(out_dir, "%s_%s%s.png" % (name, v, suffix))
        save(fr, path, label)
        print("%s  (%dx%d, %s, %.1fs)" % (path, fr.W, fr.H, "textured" if textured else "flat", time.time() - t1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
