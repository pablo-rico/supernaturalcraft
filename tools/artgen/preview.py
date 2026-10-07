"""Orthographic previews of a textured rig in its rest pose (front, right side, back), so the
geometry/UV/paint pipeline can be checked without launching the game. Bone rotations and
inflation are ignored; faces are drawn back-to-front."""

from pngio import write_png


def _views(rig, tex):
    views = {}
    # (face, depth key, mapping from face pixel -> (screen_x, screen_y) in pixel units)
    def north(c, fx, fy):
        ox, oy, oz = c.origin
        w, h, d = c.size
        return -(ox + w - fx - 0.5), -(oy + h - fy - 0.5)

    def south(c, fx, fy):
        ox, oy, oz = c.origin
        w, h, d = c.size
        return (ox + fx + 0.5), -(oy + h - fy - 0.5)

    def east(c, fx, fy):
        ox, oy, oz = c.origin
        w, h, d = c.size
        # seen from +X: viewer's right is north (-Z), so screen x = -z
        return -(oz + d - fx - 0.5), -(oy + h - fy - 0.5)

    specs = {
        "front": ("north", lambda c: -c.origin[2], north),
        "side": ("east", lambda c: c.origin[0] + c.size[0], east),
        "back": ("south", lambda c: c.origin[2] + c.size[2], south),
    }
    for name, (face, depth, fn) in specs.items():
        pts = []
        cubes = sorted((c for _, c in rig.cubes() if face in c.rects), key=lambda c: (depth(c), c.inflate))
        for c in cubes:
            u, v, w, h = c.rects[face]
            for fy in range(h):
                for fx in range(w):
                    col = tex.rows[v + fy][u + fx]
                    if col[3] < 30:
                        continue
                    sx, sy = fn(c, fx, fy)
                    pts.append((sx, sy, col))
        views[name] = pts
    return views


def render(rig, tex, path, scale=6, pad=4):
    views = _views(rig, tex)
    xs = [p[0] for v in views.values() for p in v]
    ys = [p[1] for v in views.values() for p in v]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    vw = int(maxx - minx + 1) + pad * 2
    vh = int(maxy - miny + 1) + pad * 2
    W, H = vw * 3 * scale, vh * scale
    canvas = [[(40, 42, 48, 255)] * W for _ in range(H)]
    for i, name in enumerate(("front", "side", "back")):
        for sx, sy, col in views[name]:
            px = int(sx - minx) + pad + i * vw
            py = int(sy - miny) + pad
            for dy in range(scale):
                for dx in range(scale):
                    canvas[py * scale + dy][px * scale + dx] = col[:3] + (255,)
    write_png(path, canvas)
