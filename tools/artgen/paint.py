"""Painting onto packed cube faces.

A material is a function (face, x, y, w, h) -> colour, called for every pixel of every face of
a cube in face-local coordinates (x right, y down, as seen from outside the cube). Directional
shading is applied on top so flat-coloured boxes still read as solids.
"""

import random

from pixelkit import fbm, shade

SHADE = {"up": 1.12, "north": 1.0, "east": 0.9, "west": 0.9, "south": 0.82, "down": 0.68}


def paint_cube(t, cube, material, faces=None, shading=True):
    for face, (u, v, w, h) in cube.rects.items():
        if faces and face not in faces:
            continue
        for y in range(h):
            for x in range(w):
                c = material(face, x, y, w, h)
                if c is None:
                    continue
                if shading and c[3]:
                    c = shade(c, SHADE[face])
                t.set(u + x, v + y, c)


def stamp_face(t, cube, face, rows, pal, ox=0, oy=0):
    """String art onto one face (same char conventions as Tex.stamp)."""
    u, v, w, h = cube.rects[face]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ". " or ch not in pal:
                continue
            if 0 <= ox + x < w and 0 <= oy + y < h:
                t.set(u + ox + x, v + oy + y, pal[ch])


def set_face_px(t, cube, face, x, y, c):
    u, v, w, h = cube.rects[face]
    if 0 <= x < w and 0 <= y < h:
        t.set(u + x, v + y, c)


def noise(ramp, seed, base=2.5, amp=1.6, scale=5.0):
    """Mottled fill from a ramp."""
    def m(face, x, y, w, h):
        n = fbm(x * 3 + sum(map(ord, face)) % 7, y * 3, seed, 48, 48, 2, scale)
        return ramp[int(round(base + (n - 0.5) * amp * 2))]
    return m


def cloth(ramp, seed, base=2.4, weave=0.5):
    """Fabric: low noise with a faint weave and darker hems at the face edges."""
    rng = random.Random(seed)

    def m(face, x, y, w, h):
        n = fbm(x * 2, y * 2, seed + len(face), 32, 32, 2, 6.0)
        v = base + (n - 0.5) * 1.4
        if (x + y) % 2 == 0:
            v += weave * 0.3
        if face in ("north", "south", "east", "west") and y == h - 1:
            v -= 0.8
        if rng.random() < 0.02:
            v -= 0.6
        return ramp[int(round(v))]
    return m


def solid(c):
    return lambda face, x, y, w, h: c


def vertical_split(top_mat, bottom_mat, split_from_bottom):
    """Sides use bottom_mat for the lowest rows (hands, boots); up/down pick by role."""
    def m(face, x, y, w, h):
        if face in ("north", "south", "east", "west"):
            return bottom_mat(face, x, y, w, h) if y >= h - split_from_bottom else top_mat(face, x, y, w, h)
        return bottom_mat(face, x, y, w, h) if face == "down" else top_mat(face, x, y, w, h)
    return m
