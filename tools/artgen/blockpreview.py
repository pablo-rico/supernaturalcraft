"""Z-buffered previews of vanilla JSON block models (elements, per-face UVs, element rotation), so a
generated block model can be reviewed without launching the game.

Follows the baking code: a face's uv [u1, v1, u2, v2] runs from the viewer's left/top to right/bottom as
seen from outside the face; an element rotation turns about `origin` with a right-handed quaternion
(+45 about Y carries +X toward -Z). Vanilla's directional shade is applied per face normal.

    import blockpreview
    blockpreview.render("spell_bowl", "/tmp/bowl.png")
"""

import json
import math
import os

from common import ASSETS
from pngio import read_png, write_png

MODELS = os.path.join(ASSETS, "models", "block")
TEXTURES = os.path.join(ASSETS, "textures")

# Corner points (left-top, right-top, left-bottom) of each face as seen from outside it.
def _corners(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    if face == "north":
        return (x1, y1, z0), (x0, y1, z0), (x1, y0, z0)
    if face == "south":
        return (x0, y1, z1), (x1, y1, z1), (x0, y0, z1)
    if face == "west":
        return (x0, y1, z0), (x0, y1, z1), (x0, y0, z0)
    if face == "east":
        return (x1, y1, z1), (x1, y1, z0), (x1, y0, z1)
    if face == "up":
        return (x0, y1, z0), (x1, y1, z0), (x0, y1, z1)
    return (x0, y0, z1), (x1, y0, z1), (x0, y0, z0)  # down


def _default_uv(face, f, t):
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


def _rotate(p, rot):
    if not rot:
        return p
    o = rot["origin"]
    a = math.radians(rot["angle"])
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    ax = rot["axis"]
    if ax == "y":
        x, z = x * c + z * s, -x * s + z * c
    elif ax == "x":
        y, z = y * c - z * s, y * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def _load_tex(ref, cache):
    if ref in cache:
        return cache[ref]
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    file = os.path.join(TEXTURES, path + ".png")
    if ns == "supernaturalcraft" and os.path.exists(file):
        w, h, rows = read_png(file)
    else:
        w, h, rows = 2, 2, [[(128, 110, 90, 255)] * 2 for _ in range(2)]
    cache[ref] = (w, h, rows)
    return cache[ref]


def load(name):
    with open(os.path.join(MODELS, name + ".json")) as fh:
        m = json.load(fh)
    return m


def _resolve(textures, key):
    seen = 0
    while key.startswith("#") and seen < 8:
        key = textures.get(key[1:], "missing")
        seen += 1
    return key


def _shade(n):
    x, y, z = n
    if y < -0.99:
        return 0.5
    return min(1.0, x * x * 0.6 + y * y * ((3 + y) / 4) + z * z * 0.8)


def texels(model, extra=()):
    """World-space texel quads: (corners[4], colour)."""
    cache = {}
    out = []
    textures = model.get("textures", {})
    for el in model["elements"]:
        f, t, rot = el["from"], el["to"], el.get("rotation")
        for face, spec in el["faces"].items():
            ref = _resolve(textures, spec["texture"])
            w, h, rows = _load_tex(ref, cache)
            u1, v1, u2, v2 = spec.get("uv") or _default_uv(face, f, t)
            lt, rt, lb = (_rotate(p, rot) for p in _corners(face, f, t))
            ex = [rt[i] - lt[i] for i in range(3)]
            ey = [lb[i] - lt[i] for i in range(3)]
            nx = (ex[1] * ey[2] - ex[2] * ey[1], ex[2] * ey[0] - ex[0] * ey[2], ex[0] * ey[1] - ex[1] * ey[0])
            nl = math.sqrt(sum(c * c for c in nx)) or 1
            # ex x ey points inward for an outside view (left->right, top->bottom); flip it.
            normal = tuple(-c / nl for c in nx)
            k = _shade(normal)
            tx0, tx1 = sorted((u1 * w / 16, u2 * w / 16))
            ty0, ty1 = sorted((v1 * h / 16, v2 * h / 16))
            for ty in range(int(math.floor(ty0 + 1e-6)), int(math.ceil(ty1 - 1e-6))):
                for tx in range(int(math.floor(tx0 + 1e-6)), int(math.ceil(tx1 - 1e-6))):
                    col = rows[ty % h][tx % w]
                    if col[3] < 26:
                        continue
                    # Texel span in face-local 0..1 coordinates (u may run backwards).
                    def fu(x):
                        return (x * 16 / w - u1) / (u2 - u1) if u2 != u1 else 0
                    def fv(y):
                        return (y * 16 / h - v1) / (v2 - v1) if v2 != v1 else 0
                    a0, a1 = max(0, min(1, fu(max(tx, tx0)))), max(0, min(1, fu(min(tx + 1, tx1))))
                    b0, b1 = max(0, min(1, fv(max(ty, ty0)))), max(0, min(1, fv(min(ty + 1, ty1))))
                    quad = []
                    for (a, b) in ((a0, b0), (a1, b0), (a1, b1), (a0, b1)):
                        quad.append(tuple(lt[i] + ex[i] * a + ey[i] * b for i in range(3)))
                    c = (int(col[0] * k), int(col[1] * k), int(col[2] * k), 255)
                    out.append((quad, c, normal))
    out.extend(extra)
    return out


def _camera(yaw, pitch):
    """Camera direction (from the model toward the eye): yaw 0 = from the north, 90 = from the east."""
    y, p = math.radians(yaw), math.radians(pitch)
    c = (math.sin(y) * math.cos(p), math.sin(p), -math.cos(y) * math.cos(p))
    up0 = (0, 1, 0)
    r = (up0[1] * c[2] - up0[2] * c[1], up0[2] * c[0] - up0[0] * c[2], up0[0] * c[1] - up0[1] * c[0])
    rl = math.sqrt(sum(v * v for v in r)) or 1
    r = tuple(v / rl for v in r)
    u = (c[1] * r[2] - c[2] * r[1], c[2] * r[0] - c[0] * r[2], c[0] * r[1] - c[1] * r[0])
    return c, r, u


VIEWS = {"front": (0, 0), "iso": (35, 32), "low": (215, 14), "top": (0, 89), "side": (90, 0), "back": (180, 20)}


def render(name_or_model, path, views=("iso", "low", "front", "top"), scale=24, extra=(), bg=(36, 38, 44),
           center=(8, 6, 8), span=18):
    model = load(name_or_model) if isinstance(name_or_model, str) else name_or_model
    quads = texels(model, extra)
    size = int(span * scale)
    W = size * len(views)
    img = [[bg + (255,)] * W for _ in range(size)]
    zb = [[-1e9] * W for _ in range(size)]
    for vi, view in enumerate(views):
        yaw, pitch = VIEWS[view] if isinstance(view, str) else view
        c, r, u = _camera(yaw, pitch)
        for quad, col, normal in quads:
            if sum(normal[i] * c[i] for i in range(3)) <= 0 and col[3] == 255:
                continue  # back face
            pts = []
            for q in quad:
                q = (q[0] - center[0], q[1] - center[1], q[2] - center[2])
                sx = sum(q[i] * r[i] for i in range(3)) * scale + size / 2
                sy = -sum(q[i] * u[i] for i in range(3)) * scale + size / 2
                d = sum(q[i] * c[i] for i in range(3))
                pts.append((sx, sy, d))
            # Depth as a plane over the screen (a flat per-quad depth breaks decals at grazing angles).
            (x0, y0, d0), (x1, y1, d1), (x3, y3, d3) = pts[0], pts[1], pts[3]
            det = (x1 - x0) * (y3 - y0) - (x3 - x0) * (y1 - y0)
            if abs(det) < 1e-9:
                continue
            ga = ((d1 - d0) * (y3 - y0) - (d3 - d0) * (y1 - y0)) / det
            gb = ((x1 - x0) * (d3 - d0) - (x3 - x0) * (d1 - d0)) / det
            xs, ys = [p[0] for p in pts], [p[1] for p in pts]
            for py in range(max(0, int(min(ys))), min(size, int(max(ys)) + 1)):
                for px in range(max(0, int(min(xs))), min(size, int(max(xs)) + 1)):
                    X, Y = px + 0.5, py + 0.5
                    inside, sign = True, 0
                    for i in range(4):
                        ax, ay = pts[i][0], pts[i][1]
                        bx, by = pts[(i + 1) % 4][0], pts[(i + 1) % 4][1]
                        cr = (bx - ax) * (Y - ay) - (by - ay) * (X - ax)
                        if abs(cr) < 1e-9:
                            continue
                        s = 1 if cr > 0 else -1
                        if sign == 0:
                            sign = s
                        elif s != sign:
                            inside = False
                            break
                    if not inside:
                        continue
                    gx = vi * size + px
                    d = d0 + ga * (X - x0) + gb * (Y - y0)
                    if d > zb[py][gx]:
                        if col[3] == 255:
                            zb[py][gx] = d
                            img[py][gx] = col
                        else:
                            a = col[3] / 255
                            b = img[py][gx]
                            img[py][gx] = tuple(int(col[k] * a + b[k] * (1 - a)) for k in range(3)) + (255,)
    write_png(path, img)


def disc(cx, cy, cz, radius, color, alpha=170, step=0.5):
    """Preview-only overlay: a flat translucent disc (the bowl's liquid surface drawn by code)."""
    out = []
    n = int(radius / step) + 1
    for i in range(-n, n):
        for j in range(-n, n):
            x, z = i * step, j * step
            if math.hypot(x + step / 2, z + step / 2) > radius:
                continue
            quad = [(cx + x, cy, cz + z), (cx + x + step, cy, cz + z), (cx + x + step, cy, cz + z + step), (cx + x, cy, cz + z + step)]
            out.append((quad, color[:3] + (alpha,), (0, 1, 0)))
    return out
