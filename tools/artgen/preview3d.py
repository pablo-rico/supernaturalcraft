"""Z-buffered previews of a textured rig, with bone hierarchy, rest rotations and (optionally)
an animation pose sampled at a time. Uses the same transform math as GeckoLib's renderer:

  bedrock (x, y, z) -> java (-x, y, z); bone pivot x negated; bone rotation (-rx, -ry, rz);
  per bone:  p' = pivot + Rz * Ry * Rx * (p - pivot), child transforms applied before parents;
  entity frame (right, up, front) = (x_java, y, -z_java).

Face texels are placed so that each face reads unmirrored from outside the cube.
"""

import json
import math

from pngio import write_png


def _rx(a, p):
    c, s = math.cos(a), math.sin(a)
    return (p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c)


def _ry(a, p):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c)


def _rz(a, p):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2])


def _face_point(cube, face, u, v):
    """Bedrock-space point of texel (u, v) on a face, so the face reads unmirrored from outside."""
    x0, y0, z0 = cube.origin
    w, h, d = cube.size
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    inf = cube.inflate
    k = getattr(cube, "density", 1)
    u, v = (u + 0.5) / k - 0.5, (v + 0.5) / k - 0.5
    if face == "north":
        return (x0 + u + 0.5, y1 - v - 0.5, z0 - inf)
    if face == "south":
        return (x1 - u - 0.5, y1 - v - 0.5, z1 + inf)
    if face == "east":
        return (x1 + inf, y1 - v - 0.5, z1 - u - 0.5)
    if face == "west":
        return (x0 - inf, y1 - v - 0.5, z0 + u + 0.5)
    if face == "up":
        return (x1 - u - 0.5, y1 + inf, z0 + v + 0.5)
    return (x1 - u - 0.5, y0 - inf, z1 - v - 0.5)


def _sample(channel, t):
    """Linear interpolation of a Bedrock keyframe channel at time t (easing ignored)."""
    keys = sorted((float(k), v["vector"] if isinstance(v, dict) else v) for k, v in channel.items())
    keys = [(k, v) for k, v in keys if not isinstance(v, str)]
    if not keys:
        return None
    if t <= keys[0][0]:
        return keys[0][1]
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            f = 0 if t1 == t0 else (t - t0) / (t1 - t0)
            return [a[i] + (b[i] - a[i]) * f for i in range(3)]
    return keys[-1][1]


def pose_from(anim_json, name, t):
    """{bone: {"rotation": [...], "position": [...], "scale": [...]}} at time t of an animation."""
    if name is None:
        return {}
    a = anim_json["animations"][name]
    out = {}
    for bone, chans in a.get("bones", {}).items():
        out[bone] = {k: _sample(v, t) for k, v in chans.items()}
    return out


def render(rig, tex, path, pose=None, hidden=(), views=("front", "three_quarter", "side", "back"), scale=5, yaw34=35):
    pose = pose or {}
    by_name = {b.name: b for b in rig.bones}

    def chain(bone):
        out = []
        while bone is not None:
            out.append(bone)
            bone = by_name.get(bone.parent) if bone.parent else None
        return out  # child first

    def hidden_bone(bone):
        return any(b.name in hidden for b in chain(bone))

    def transform(bone, p):
        # p is in java space
        for b in chain(bone):
            px, py, pz = -b.pivot[0], b.pivot[1], b.pivot[2]
            rest = b.rotation or (0, 0, 0)
            anim = pose.get(b.name, {})
            rot = anim.get("rotation") or [0, 0, 0]
            pos = anim.get("position") or [0, 0, 0]
            scl = anim.get("scale") or [1, 1, 1]
            bx, by_, bz = [math.radians(rest[i] + rot[i]) for i in range(3)]
            q = (p[0] - px, p[1] - py, p[2] - pz)
            q = (q[0] * scl[0], q[1] * scl[1], q[2] * scl[2])
            q = _rx(-bx, q)
            q = _ry(-by_, q)
            q = _rz(bz, q)
            p = (q[0] + px - pos[0], q[1] + py + pos[1], q[2] + pz + pos[2])
        return p

    texels = []
    for b in rig.bones:
        if hidden_bone(b):
            continue
        for c in b.cubes:
            for face, (u0, v0, w, h) in c.rects.items():
                for v in range(h):
                    for u in range(w):
                        col = tex.rows[v0 + v][u0 + u]
                        if col[3] < 40:
                            continue
                        bx, by_, bz = _face_point(c, face, u, v)
                        q = (-bx, by_, bz)
                        if c.rotation:
                            cp = c.pivot or (0, 0, 0)
                            pj = (-cp[0], cp[1], cp[2])
                            q = (q[0] - pj[0], q[1] - pj[1], q[2] - pj[2])
                            q = _rx(-math.radians(c.rotation[0]), q)
                            q = _ry(-math.radians(c.rotation[1]), q)
                            q = _rz(math.radians(c.rotation[2]), q)
                            q = (q[0] + pj[0], q[1] + pj[1], q[2] + pj[2])
                        j = transform(b, q)
                        texels.append(((j[0], j[1], -j[2]), col, getattr(c, "density", 1)))  # entity frame (R, U, F)

    def project(view, e):
        R, U, F = e
        if view == "front":
            return -R, -U, F
        if view == "back":
            return R, -U, -F
        if view == "side":
            return F, -U, R
        if view == "left":
            return -F, -U, -R
        a = math.radians(yaw34)
        R2 = R * math.cos(a) - F * math.sin(a)
        F2 = R * math.sin(a) + F * math.cos(a)
        return -R2, -U, F2

    panels = []
    for view in views:
        pts = [(project(view, e), c, k) for e, c, k in texels]
        panels.append(pts)
    allx = [p[0][0] for pts in panels for p in pts]
    ally = [p[0][1] for pts in panels for p in pts]
    minx, maxx, miny, maxy = min(allx) - 2, max(allx) + 2, min(ally) - 2, max(ally) + 2
    pw = int((maxx - minx) * scale) + 1
    ph = int((maxy - miny) * scale) + 1
    W, H = pw * len(views), ph
    img = [[(36, 38, 44, 255)] * W for _ in range(H)]
    zb = [[-1e9] * W for _ in range(H)]
    for i, pts in enumerate(panels):
        for (sx, sy, d), col, k in pts:
            # Denser faces (decals) have smaller texels: splat each one only as wide as it is.
            splat = max(1, int(math.ceil(scale * 1.05 / k)))
            X0 = int((sx - minx) * scale - splat / 2) + i * pw
            Y0 = int((sy - miny) * scale - splat / 2)
            for yy in range(Y0, Y0 + splat):
                if not (0 <= yy < H):
                    continue
                for xx in range(X0, X0 + splat):
                    if i * pw <= xx < (i + 1) * pw and d > zb[yy][xx]:
                        zb[yy][xx] = d
                        a = col[3] / 255
                        bg = img[yy][xx]
                        img[yy][xx] = tuple(int(col[k] * a + bg[k] * (1 - a)) for k in range(3)) + (255,)
    write_png(path, img)


def load_anims(path):
    with open(path) as f:
        return json.load(f)


def project_rows(rig, tex, view="front", scale=8, hidden=(), pose=None):
    """One view rasterised on a transparent canvas; returns (rows, width, height)."""
    import tempfile, os
    tmp = tempfile.mktemp(suffix=".png")
    # Reuse the full renderer, then cut the single panel back out and make the background clear.
    render(rig, tex, tmp, pose=pose, hidden=hidden, views=(view,), scale=scale)
    from pngio import read_png
    w, h, rows = read_png(tmp)
    os.remove(tmp)
    bg = rows[0][0]
    clear = [[(0, 0, 0, 0) if px == bg else px for px in row] for row in rows]
    return clear, w, h


def icon(rig, tex, size=16, angle_deg=-45, view="front", hidden=(), pose=None):
    """A GUI icon: the model's front view, turned to lie diagonally like a vanilla sword, fitted
    into `size` pixels with nearest sampling, then outlined."""
    from pixelkit import Tex, item_outline
    rows, w, h = project_rows(rig, tex, view, 8, hidden, pose)
    pts = [(x, y) for y in range(h) for x in range(w) if rows[y][x][3]]
    if not pts:
        return Tex(size, size)
    a = math.radians(angle_deg)
    ca, sa = math.cos(a), math.sin(a)
    cx = sum(p[0] for p in pts) / len(pts)
    cy = sum(p[1] for p in pts) / len(pts)
    rot = [((x - cx) * ca - (y - cy) * sa, (x - cx) * sa + (y - cy) * ca) for x, y in pts]
    minx, maxx = min(r[0] for r in rot), max(r[0] for r in rot)
    miny, maxy = min(r[1] for r in rot), max(r[1] for r in rot)
    span = max(maxx - minx, maxy - miny) + 1
    k = span / (size - 2)
    t = Tex(size, size)
    for v in range(1, size - 1):
        for u in range(1, size - 1):
            # Centre of this icon pixel, mapped back into the rotated model space, then unrotated.
            rx = minx + (u - 1 + 0.5) * k - (span - (maxx - minx + 1)) / 2
            ry = miny + (v - 1 + 0.5) * k - (span - (maxy - miny + 1)) / 2
            sx = rx * ca + ry * sa + cx
            sy = -rx * sa + ry * ca + cy
            ix, iy = int(sx), int(sy)
            if 0 <= ix < w and 0 <= iy < h and rows[iy][ix][3]:
                t.set(u, v, rows[iy][ix])
    item_outline(t, (20, 14, 12, 255))
    return t
