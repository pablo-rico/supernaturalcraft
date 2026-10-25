"""The hunter's grimoire as a GeckoLib item: a worn leather journal that opens in the hand.

Built lying flat, pages up, with the spine along Z and the hinge line on the model origin, so the
open spread is centred on (0, ~0.5, 0). Closed it is stacked on +X (fore-edge at x = 10):

  back_half (pivot 0,0,0)   back cover y -3..-2, right page block y -2..0, corner guards, strap
                            (hinged at the fore-edge bottom, its flap + buckle over the front cover),
                            ribbon and a note tucked between the pages
  front_cover (pivot 0,0,0) front cover y 2..3 (embossed devil's trap), left page block y 0..2
  spine (pivot -0.5,-3,0)   closed spine x -1..0; squashed under the gutter when open
  page (pivot 0,0,0)        one loose leaf on the right block that flutters over to the left

Clips: closed (rest), open (strap off, cover swings over 172 deg (negative Z: GeckoLib mirrors X), the leaf flutters), idle (breathing
pages). GrimoireItem plays open -> idle while it is held and closed anywhere else. The GUI keeps the
flat sprite (items.py `grimoire` -> grimoire_icon).
"""

import math
import random

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import paint_cube
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/item/"
ANIM = ASSETS + "/animations/item/"
NAME = "grimoire"

HIDE = Ramp("#1f130c", "#33200f", "#4a2f1b", "#5e3d24", "#76502f", "#8f6a45")
BRASS = Ramp("#2e220e", "#4f3c16", "#745b25", "#977a36", "#b89a50", "#d6bf7e")
PAPER = Ramp("#6e5a39", "#927a50", "#b49c6c", "#cdb684", "#ddc999", "#ebdcb3")
NOTE = Ramp("#7d6c50", "#a08d69", "#bfad86", "#d6c69f", "#e4d7b5", "#f0e7cc")
INK = hexc("#2a1c12")
INK_FADED = hexc("#55402a")
RED_INK = hexc("#7c1d16")
THREAD = hexc("#b99d6c")
RIBBON = Ramp("#3a0709", "#5a0d10", "#7a1418", "#962024", "#ad3330", "#c24a43")

W, H = 10, 14          # cover width (spine to fore-edge) and length
PW, PH = 9, 13         # page block


def _h(*v):
    n = 0
    for k in v:
        n = (n * 1000003 + int(k) * 7919 + 17) & 0xFFFFFFFF
    return (n ^ (n >> 13)) * 0x5BD1E995 & 0xFFFFFFFF


def leather(seed, inner=False, amp=2.2, nicks=37):
    """Mottled, scuffed hide: lighter wear along the edges and at the corners."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 64, 64, 3, 6.0)
        v = 2.2 + (n - 0.5) * amp
        if inner:
            v -= 0.8
        edge = min(x, y, w - 1 - x, h - 1 - y)
        if face in ("up", "down") and edge <= 1 and _h(x, y, seed) % 5 < 2:
            v += 1.3                            # rubbed edges
        if _h(x * 3, y * 5, seed) % nicks == 0:
            v -= 1.0                            # nicks
        return HIDE[max(0, min(5, int(round(v))))]
    return m


def devils_trap_cover(seed):
    """Front cover outer face: tooled border, stitching along the spine and a pressed devil's trap."""
    base = leather(seed, amp=1.3, nicks=97)

    def m(face, x, y, w, h):
        c = base(face, x, y, w, h)
        if face != "up":
            return c
        # Spine side is x = w - 1 on this face (u runs toward -X); k texels per pixel.
        k = w / W
        if x == w - 1 - round(1.5 * k) and y % 3 == 1 and 1 <= y <= h - 2:
            return THREAD
        lo, hi = round(1.2 * k), w - 1 - round(2.6 * k)
        if (x in (lo, hi) and lo <= y <= h - 1 - lo) or (y in (lo, h - 1 - lo) and lo <= x <= hi):
            return mix(c, HIDE[0], 0.6)         # tooled border line
        cx, cy = (lo + hi) / 2, (h - 1) / 2
        dx, dy = x - cx, y - cy
        r = math.hypot(dx, dy)
        R = 3.4 * k
        pressed = abs(r - R) < 0.6 or abs(r - R + 1.6) < 0.5
        ri = R - 1.6
        if r < ri + 0.5:
            pts = [(cx + ri * math.sin(math.radians(a)), cy - ri * math.cos(math.radians(a))) for a in range(0, 360, 72)]
            for i in range(5):
                (ax, ay), (bx, by) = pts[i], pts[(i + 2) % 5]
                vx, vy = bx - ax, by - ay
                t = max(0.0, min(1.0, ((x - ax) * vx + (y - ay) * vy) / (vx * vx + vy * vy)))
                if math.hypot(x - ax - t * vx, y - ay - t * vy) < 0.55:
                    pressed = True
        if pressed:
            return HIDE[0] if _h(x, y) % 5 else HIDE[1]
        if (abs(r - R - 1.0) < 0.5 or abs(r - R + 0.6) < 0.4) and dy > 0:
            return mix(c, HIDE[5], 0.5)         # lit lip of the pressed rings
        return c
    return m


def page_edges(seed, ramp=PAPER):
    """Edges of a page block: uneven leaves, darker and thumbed toward the ends."""
    def m(face, x, y, w, h):
        v = 3.4 + ((_h(x // 2, y, seed) % 5) - 2) * 0.35
        if y % 2 == 1 and _h(x, y, seed + 1) % 3 == 0:
            v -= 1.2                            # gaps between leaves
        end = min(x, w - 1 - x)
        if end < 3:
            v -= (3 - end) * 0.45
        return ramp[max(0, min(5, int(round(v))))]
    return m


def page_face(seed, kind, ramp=PAPER):
    """A written page: aged paper, faint water stains and handwritten lines; kind picks the layout."""
    rng = random.Random(seed)

    def scribble_lines(w, h, y0, y1):
        ink = set()
        for ly in range(y0, y1, 3):
            x = 2 + rng.randrange(0, 2)
            end = w - 2 - rng.randrange(0, 5)
            while x < end:
                word = rng.randrange(2, 6)
                for i in range(word):
                    if x + i < end and rng.random() < 0.8:
                        ink.add((x + i, ly if rng.random() < 0.85 else ly - 1))
                x += word + 1
        return ink

    marks = {}

    def build(w, h):
        if marks:
            return
        ink = set()
        red = set()
        if kind == "sigil":
            # A devil's trap drawn in ink over the top of the page, notes below.
            cx, cy, R = w / 2 - 0.5, 8.5, 6.4
            for y in range(h):
                for x in range(w):
                    r = math.hypot(x - cx, y - cy)
                    if abs(r - R) < 0.5 or abs(r - R + 1.6) < 0.45:
                        ink.add((x, y))
            pts = [(cx + (R - 1.6) * math.sin(math.radians(a)), cy - (R - 1.6) * math.cos(math.radians(a)))
                   for a in range(0, 360, 72)]
            for i in range(5):
                (ax, ay), (bx, by) = pts[i], pts[(i + 2) % 5]
                for s in range(24):
                    t = s / 23
                    red.add((int(round(ax + (bx - ax) * t)), int(round(ay + (by - ay) * t))))
            ink |= scribble_lines(w, h, 18, h - 1)
        elif kind == "notes":
            ink |= scribble_lines(w, h, 3, h - 1)
            for x in range(3, w - 6):
                red.add((x, 10))                # underlined in red
            for y in range(18, 23):             # a boxed sketch in the corner
                for x in range(w - 8, w - 2):
                    if y in (18, 22) or x in (w - 8, w - 3):
                        ink.add((x, y))
        elif kind == "note":
            ink |= scribble_lines(w, h, 2, h - 1)
        else:
            ink |= scribble_lines(w, h, 3, h - 1)
        marks.update({p: INK if _h(*p, seed) % 6 else INK_FADED for p in ink})
        marks.update({p: RED_INK for p in red})

    def m(face, x, y, w, h):
        build(w, h)
        n = fbm(x, y, seed, 64, 64, 3, 5.0)
        v = 3.5 + (n - 0.5) * 1.6
        edge = min(x, y, w - 1 - x, h - 1 - y)
        if edge == 0:
            v -= 0.9
        c = ramp[max(0, min(5, int(round(v))))]
        if (x, y) in marks:
            return mix(c, marks[(x, y)], 0.85)
        return c
    return m


def brass(seed):
    def m(face, x, y, w, h):
        v = 3 + ((_h(x, y, seed) % 3) - 1) * 0.6
        if face == "up" and (x, y) == (w - 2 if w > 2 else 0, 1 if h > 2 else 0):
            v = 1                               # rivet
        if min(x, y, w - 1 - x, h - 1 - y) == 0 and face == "up":
            v += 0.8
        return BRASS[max(0, min(5, int(round(v))))]
    return m


def buckle(face, x, y, w, h):
    if face == "up" and 1 <= x <= w - 2 and 1 <= y <= h - 2:
        return HIDE[1] if x != w // 2 else BRASS[3]   # strap showing through, the tongue across it
    return BRASS[3 + (x + y) % 2]


def strap_leather(face, x, y, w, h):
    v = 2.0 + (0.6 if x in (0, w - 1) or y in (0, h - 1) else 0) - (0.5 if _h(x, y, 77) % 4 == 0 else 0)
    return HIDE[int(round(v))]


def ribbon(face, x, y, w, h):
    return RIBBON[3 if (x + y) % 3 else 2]


class Book:
    def __init__(self):
        self.rig = Rig(NAME, 256, 256)
        self.paints = []

    def bone(self, name, pivot, parent=None):
        return self.rig.bone(name, pivot, parent)

    def cube(self, bone, origin, size, material, shading=True, **kw):
        c = box(bone, origin, size, **kw)
        self.paints.append((c, material, shading))
        return c

    def build(self):
        self.rig.pack()
        t = Tex(self.rig.tex_w, self.rig.tex_h, 3)
        for c, material, shading in self.paints:
            paint_cube(t, c, material, shading=shading)
        return t


def grimoire():
    b = Book()
    root = b.bone("root", (0, 0, 0))
    back = b.bone("back_half", (0, 0, 0), "root")
    front = b.bone("front_cover", (0, 0, 0), "root")
    spine = b.bone("spine", (-0.5, -3, 0), "root")
    page = b.bone("page", (0, 0, 0), "root")
    strap = b.bone("strap", (W, -3, 0), "back_half")
    flap = b.bone("strap_flap", (W, 3.5, 0), "strap")
    z0 = -H / 2
    pz0 = -PH / 2

    # Back half: cover, right page block, corner guards, ribbon, tucked note, strap.
    b.cube(back, (0, -3, z0), (W, 1, H), leather(61, inner=False), density=2)
    b.cube(back, (0, -2, pz0), (PW, 2, PH), page_edges(62), density=2)
    for z in (z0, H / 2 - 2):
        b.cube(back, (W - 2, -3, z), (2, 1, 2), brass(63), inflate=0.12, density=2)
    # Planes carry one face each (GeckoLib draws items without culling: two faces on one plane would fight).
    b.cube(back, (4, 0.04, -7.5), (1, 0, 5), ribbon, faces=("up",))
    b.cube(back, (4, 0.04, -11.5), (1, 0, 4), ribbon, faces=("up",), rotation=(55, 0, 0), pivot=(4.5, 0.04, -7.5))
    b.cube(back, (6, 0.06, -4), (5, 0, 4), page_face(64, "note", NOTE), faces=("up",),
           rotation=(0, -9, 0), pivot=(8, 0.05, -2), density=2)
    b.cube(back, (6, -3.75, -1.5), (4, 1, 3), strap_leather, inflate=-0.25)
    b.cube(strap, (W - 0.25, -3.25, -1.5), (1, 7, 3), strap_leather, inflate=-0.25)
    b.cube(flap, (6.5, 2.77, -1.5), (4, 1, 3), strap_leather, inflate=-0.25)
    b.cube(flap, (6.0, 2.9, -1.5), (2, 1, 3), buckle, inflate=-0.1, density=2)

    # Front cover with its block of pages (the left page once open).
    b.cube(front, (0, 2, z0), (W, 1, H), devils_trap_cover(71), density=3)
    b.cube(front, (0, 0, pz0), (PW, 2, PH), page_edges(72), density=2)
    for z in (z0, H / 2 - 2):
        b.cube(front, (W - 2, 2, z), (2, 1, 2), brass(73), inflate=0.12, density=2)

    # Spine: one piece over the closed edge.
    b.cube(spine, (-1, -3, z0), (1, 6, H), leather(81), density=2)

    # The loose leaf on top of the right block.
    b.cube(page, (0, 0.03, pz0), (PW, 0, PH), page_face(91, "notes"), faces=("up",), density=2, shading=False)
    b.cube(page, (0, 0.01, pz0), (PW, 0, PH), page_face(103, "sigil"), faces=("down",), density=2, shading=False)

    t = b.build()
    _paint_page_tops(t, b)
    return b, t


def _paint_page_tops(t, b):
    """The readable faces: the right block's top, the left block's underside (up once open)."""
    by_bone = {bn.name: bn for bn in b.rig.bones}
    right = by_bone["back_half"].cubes[1]
    left = by_bone["front_cover"].cubes[1]
    paint_cube(t, right, page_face(101, "notes"), faces=("up",), shading=False)
    paint_cube(t, left, page_face(102, "plain"), faces=("down",), shading=False)
    spine = by_bone["spine"].cubes[0]
    paint_cube(t, spine, _spine_face, faces=("west",))


def _spine_face(face, x, y, w, h):
    """Outer spine (u along Z, v down the thickness): raised bands and stitching along both joints."""
    if x in (5, 6, 13, 14, 21, 22):
        return HIDE[4] if x % 2 == 1 else HIDE[1]
    if y in (1, h - 2) and x % 2 == 0:
        return THREAD
    n = fbm(x * 2, y * 2, 82, 64, 64, 3, 6.0)
    return HIDE[max(0, min(5, int(round(2.3 + (n - 0.5) * 2))))]


OPEN_FRONT = -172      # the cover stops just short of flat, so the spread reads as a shallow V
OPEN_BACK = -8
LEAF_REST = -170


def animations():
    f = AnimFile()
    bones = ("front_cover", "back_half", "page", "strap", "strap_flap", "spine")
    closed = f.new("animation.grimoire.closed", 0.5, loop=True)
    for bn in bones:
        closed.const_rot(bn, [0, 0, 0])
    closed.pos("spine", (0, [0, 0, 0]), (0.5, [0, 0, 0]))
    closed.scale("spine", (0, [1, 1, 1]), (0.5, [1, 1, 1]))

    o = f.new("animation.grimoire.open", 0.95)
    o.rot("strap", (0, [0, 0, 0]), (0.12, [0, 0, 95], "easeOutQuad"), (0.32, [0, 0, 165], "easeInOutSine"),
          (0.45, [0, 0, 150], "easeInOutSine"), (0.6, [0, 0, 158], "easeInOutSine"))
    o.rot("strap_flap", (0, [0, 0, 0]), (0.12, [0, 0, 20]), (0.32, [0, 0, 70], "easeInOutSine"),
          (0.6, [0, 0, 80], "easeInOutSine"))
    o.rot("front_cover", (0, [0, 0, 0]), (0.08, [0, 0, 0]), (0.4, [0, 0, -110], "easeInSine"),
          (0.6, [0, 0, OPEN_FRONT - 3], "easeOutQuad"), (0.72, [0, 0, OPEN_FRONT], "easeInOutSine"))
    o.rot("back_half", (0, [0, 0, 0]), (0.35, [0, 0, 0]), (0.62, [0, 0, OPEN_BACK - 2], "easeOutQuad"),
          (0.75, [0, 0, OPEN_BACK], "easeInOutSine"))
    o.rot("page", (0, [0, 0, 0]), (0.32, [0, 0, 0]), (0.46, [0, 0, -25], "easeOutQuad"), (0.56, [0, 0, -8], "easeInOutSine"),
          (0.7, [0, 0, -120], "easeInQuad"), (0.82, [0, 0, LEAF_REST - 4], "easeOutQuad"), (0.95, [0, 0, LEAF_REST], "easeInOutSine"))
    o.pos("spine", (0, [0, 0, 0]), (0.3, [0, 0, 0]), (0.6, [0.5, -0.25, 0], "easeInOutSine"))
    o.scale("spine", (0, [1, 1, 1]), (0.3, [1, 1, 1]), (0.6, [1, 0.45, 1], "easeInOutSine"))

    idle = f.new("animation.grimoire.idle", 4.0, loop=True)
    idle.rot("strap", (0, [0, 0, 158]), (2.0, [0, 0, 155], "easeInOutSine"), (4.0, [0, 0, 158], "easeInOutSine"))
    idle.const_rot("strap_flap", [0, 0, 80])
    idle.rot("front_cover", (0, [0, 0, OPEN_FRONT]), (2.0, [0, 0, OPEN_FRONT + 1.5], "easeInOutSine"),
             (4.0, [0, 0, OPEN_FRONT], "easeInOutSine"))
    idle.rot("back_half", (0, [0, 0, OPEN_BACK]), (2.0, [0, 0, OPEN_BACK - 1.5], "easeInOutSine"),
             (4.0, [0, 0, OPEN_BACK], "easeInOutSine"))
    idle.rot("page", (0, [0, 0, LEAF_REST]), (1.4, [0, 0, LEAF_REST + 7], "easeInOutSine"),
             (2.4, [0, 0, LEAF_REST + 2], "easeInOutSine"), (4.0, [0, 0, LEAF_REST], "easeInOutSine"))
    idle.pos("spine", (0, [0.5, -0.25, 0]), (4.0, [0.5, -0.25, 0]))
    idle.scale("spine", (0, [1, 0.45, 1]), (4.0, [1, 0.45, 1]))
    return f


def generate():
    b, t = grimoire()
    b.rig.write(GEO + NAME + ".geo.json")
    animations().write(ANIM + NAME + ".animation.json")
    save(t, "item", NAME)


if __name__ == "__main__":
    generate()
