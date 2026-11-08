"""Amara's particles and item sprites (v0.42). The void motes stay where they always were (amara_art.void_mote).

Particles, textures/particle/<id>.png, 16x16, white with alpha (the code tints them; a few greys inside only shade):
  darkness_ash_0..3  a flake of unmade ground breaking up: a crisp flake, cracked, in pieces, specks
  soul_wisp          a soul being drawn out: a soft teardrop with a bright head (head up, tail trailing down)
  star_mote          a falling star: a four-point twinkle in a soft glow
  ink_drop           a drop of the darkness: round, a bright highlight, a darker rim
  dawn_mote          a mote of returning sunlight: a soft round glow with a tiny bright centre

Items, textures/item/<id>.png, 16x16, full colour, the mod's dark outline:
  amaras_light       her gift at the reconciliation: a mote of dawn inside a black glass bead (violet rim light)
  void_essence       a small stoppered vial of swirling darkness with stars in it
  eclipse_sight      "The Sight": an eye of black glass with a gold iris, a corona of thin gold rays round it
"""

import math
import random

import palette as P
from common import save
from pixelkit import Tex, hexc, item_outline, mix

O = P.OUTLINE


def _white(a, v=255):
    return (v, v, v, int(max(0, min(255, a))))


def _sm(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def _ss(fn, ss=4):
    """Supersamples fn(x, y) -> (value, alpha) into a 16x16 white-alpha texture (value = grey level 0..1)."""
    t = Tex(16, 16, 0)
    for py in range(16):
        for px in range(16):
            va = aa = 0.0
            for sy in range(ss):
                for sx in range(ss):
                    v, a = fn(px + (sx + 0.5) / ss, py + (sy + 0.5) / ss)
                    va += v * a
                    aa += a
            if aa <= 0:
                continue
            a = aa / (ss * ss)
            if a * 255 < 6:
                continue
            t.set(px, py, _white(a * 255, int(255 * va / aa)))
    return t


# --- particles ----------------------------------------------------------------------------------------------------------

def _poly_inside(x, y, pts):
    inside = False
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


FLAKE = [(3.2, 6.0), (7.4, 2.8), (12.6, 4.4), (13.2, 9.0), (9.8, 12.8), (4.4, 11.6)]


def _frag(pts, dx, dy, k, cx=8.0, cy=8.0):
    """A piece of the flake, moved by (dx, dy) and shrunk about its own centre by k."""
    mx = sum(p[0] for p in pts) / len(pts)
    my = sum(p[1] for p in pts) / len(pts)
    return [(mx + (p[0] - mx) * k + dx, my + (p[1] - my) * k + dy) for p in pts]


def darkness_ash(frame):
    """Unmade ground breaking up: frame 0 a crisp flake (a lighter top edge, a crack begun), 1 cracked through and
    crumbling at its edges, 2 three pieces drifting apart, 3 specks."""
    rng = random.Random(9100 + frame)
    if frame == 3:
        t = Tex(16, 16, 0)
        for (x, y, a) in ((4, 5, 230), (9, 3, 200), (12, 7, 255), (6, 10, 220), (10, 12, 190), (3, 12, 150), (13, 11, 170),
                          (7, 7, 140)):
            t.set(x, y, _white(a))
        t.set(12, 8, _white(150))
        t.set(5, 10, _white(120))
        return t
    pieces = [FLAKE]
    if frame == 2:
        a = [(3.2, 6.0), (7.4, 2.8), (9.0, 6.8), (6.0, 9.0)]
        b = [(7.4, 2.8), (12.6, 4.4), (13.2, 9.0), (9.0, 6.8)]
        c = [(6.0, 9.0), (9.0, 6.8), (13.2, 9.0), (9.8, 12.8), (4.4, 11.6)]
        pieces = [_frag(a, -1.6, -1.2, 0.8), _frag(b, 1.8, -1.4, 0.75), _frag(c, 0.4, 1.8, 0.7)]

    def crack(x, y):
        # The break lines of frames 0-1: a fork from the upper middle.
        d1 = abs((x - 9.0) * 0.45 + (y - 6.8) * 0.9) if 6.0 <= x <= 13.2 and 2.5 <= y <= 9.2 else 9
        d2 = abs((x - 9.0) * 0.6 - (y - 6.8) * 0.8) if 5.5 <= x <= 9.4 and 6.5 <= y <= 9.5 else 9
        return min(d1, d2)

    def fn(x, y):
        for p in pieces:
            if _poly_inside(x, y, p):
                # Erode the edges as it crumbles.
                if frame == 1:
                    ex = min(math.hypot(x - q[0], y - q[1]) for q in p)
                    if ex < 1.6 and random.Random(f"{int(x * 2)}:{int(y * 2)}").random() < 0.45:
                        return 0.0, 0.0
                    if crack(x, y) < 0.45:
                        return 0.0, 0.0
                if frame == 0 and crack(x, y) < 0.3 and y < 7.5:
                    return 0.6, 1.0
                v = 0.82 + 0.18 * (1 - y / 16)
                return v, 1.0
        return 0.0, 0.0
    return _ss(fn, 1)


def soul_wisp():
    """A soft teardrop wisp: a bright round head up top, a tail trailing down and curling a little to one side."""
    hx, hy = 7.5, 4.6

    def fn(x, y):
        dh = math.hypot(x - hx, y - hy)
        head = 1 - _sm(1.6, 3.2, dh)
        # The tail: centred on a gentle S below the head, narrowing.
        if y > hy:
            u = min(1.0, (y - hy) / 10.5)
            cx = hx + math.sin(u * 3.0) * 1.6 * u
            w = 2.9 * (1 - u) ** 0.9 + 0.3
            tail = (1 - _sm(w * 0.5, w, abs(x - cx))) * (1 - _sm(0.75, 1.0, u))
        else:
            tail = 0.0
        a = max(head, tail * 0.75)
        if a <= 0:
            return 0.0, 0.0
        core = 1 - _sm(0.0, 1.6, dh)
        return 0.8 + 0.2 * core, a
    return _ss(fn)


def star_mote():
    """A four-point twinkle: thin tapering arms, a bright heart, a soft round glow."""
    cx = cy = 7.5

    def fn(x, y):
        dx, dy = abs(x - cx), abs(y - cy)
        d = math.hypot(dx, dy)
        arm_v = (1 - _sm(0.25, 0.9, dx * (1 + dy * 0.35))) * (1 - _sm(4.5, 7.2, dy))
        arm_h = (1 - _sm(0.25, 0.9, dy * (1 + dx * 0.35))) * (1 - _sm(4.5, 7.2, dx))
        diag = (1 - _sm(0.2, 0.7, abs(dx - dy))) * (1 - _sm(1.5, 3.0, d)) * 0.6
        glow = math.exp(-(d / 2.6) ** 2) * 0.55
        heart = 1 - _sm(0.6, 1.6, d)
        a = min(1.0, max(arm_v, arm_h, diag, glow, heart))
        return 0.75 + 0.25 * max(heart, arm_v * 0.6, arm_h * 0.6), a
    return _ss(fn)


def ink_drop():
    """A round drop, slightly pointed at the top, a bright highlight up and left and a darker rim."""
    cx, cy, r = 7.5, 9.0, 4.4

    def fn(x, y):
        dx, dy = x - cx, y - cy
        # Pull the top into a point.
        if dy < 0:
            dx = dx / max(0.25, 1 + dy / (r * 1.9))
        d = math.hypot(dx, dy)
        a = 1 - _sm(r - 0.5, r + 0.3, d)
        if a <= 0:
            return 0.0, 0.0
        v = 0.62 - _sm(r - 1.6, r, d) * 0.22
        if math.hypot(x - (cx - 1.6), y - (cy - 1.6)) < 1.1:
            v = 1.0
        elif math.hypot(x - (cx + 1.8), y - (cy + 2.0)) < 0.8:
            v = 0.8
        return v, a
    return _ss(fn)


def dawn_mote():
    """A soft round mote of sunlight with a tiny bright centre."""
    cx = cy = 7.5

    def fn(x, y):
        d = math.hypot(x - cx, y - cy)
        a = math.exp(-(d / 3.2) ** 2) * 0.85
        heart = 1 - _sm(0.4, 1.3, d)
        a = max(a, heart)
        if d > 7.4:
            return 0.0, 0.0
        return 0.85 + 0.15 * heart, a
    return _ss(fn)


# --- items --------------------------------------------------------------------------------------------------------------

def _paint(fn):
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def amaras_light():
    """A black glass bead holding a mote of dawn: violet light along its upper-left rim, the warm light inside
    bouncing off its lower-right wall, a bright glint."""
    cx, cy, r = 7.5, 7.8, 5.7
    lx, ly = 7.8, 8.4

    def fn(x, y):
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        d = math.hypot(dx, dy)
        if d > r:
            return None
        c = mix(hexc("#120a22"), hexc("#050309"), min(1, d / r))
        # The light inside: gold-white heart, an amber bloom.
        dl = math.hypot(x + 0.5 - lx, y + 0.5 - ly)
        if dl < 4.2:
            c = mix(c, hexc("#c8741e"), (1 - dl / 4.2) * 0.95)
        if dl < 2.2:
            c = mix(hexc("#ffcf6a"), c, dl / 2.2 * 0.6)
        if dl < 1.1:
            c = hexc("#fff8e0")
        edge = d > r - 1.25
        lit = -(dx + dy) / (d + 1e-6)
        if edge and lit > 0.35:
            c = hexc("#b48cff") if lit > 0.8 else hexc("#7a54c8")
        elif edge and lit < -0.55:
            c = hexc("#4a2a14")                      # the dawn's light caught on the far wall
        return c
    t = _paint(fn)
    t.set(5, 4, hexc("#ffffff"))
    t.set(4, 5, hexc("#e0d0ff"))
    t.set(6, 4, hexc("#d4c0ff"))
    item_outline(t, O)
    return t


def void_essence():
    """A small stoppered vial of swirling darkness with stars caught in the swirl."""
    cork = [hexc("#5c3d22"), hexc("#8f673d"), hexc("#a97e4f")]
    glass = [hexc("#5d6878"), hexc("#8a9aae"), hexc("#cfd8e6"), hexc("#f2f6fc")]
    cx, cy, r = 7.5, 10.2, 4.8

    def fn(x, y):
        if 6 <= x <= 9 and 1 <= y <= 3:
            return cork[2] if x == 6 and y < 3 else cork[1] if y < 3 else cork[0]
        if 5 <= x <= 10 and y == 4:
            return glass[3] if x == 5 else glass[2] if x < 8 else glass[1]
        if 6 <= x <= 9 and 5 <= y <= 6 and math.hypot(x + 0.5 - cx, y + 0.5 - cy) > r - 0.2:
            return glass[2] if x == 6 else glass[0] if x == 9 else hexc("#1a0d30")
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        d = math.hypot(dx, dy)
        if d > r:
            return None
        if d > r - 0.9:
            return glass[2] if dx < -1 and dy < 1.5 else glass[1] if dx < 0 else glass[0]
        ang = math.atan2(dy, dx)
        band = math.sin(ang * 2 + d * 1.25)
        c = hexc("#06040c") if band < -0.2 else hexc("#1a0d30") if band < 0.45 else hexc("#3a1f66")
        if band > 0.85:
            c = hexc("#5a3a9a")
        return c
    t = _paint(fn)
    for (x, y, h) in ((6, 9, "#ffffff"), (9, 11, "#e8deff"), (8, 13, "#c8b4ff"), (10, 9, "#b48cff"), (5, 12, "#e8deff")):
        t.set(x, y, hexc(h))
    t.set(4, 9, glass[3])
    t.set(4, 10, glass[2])
    item_outline(t, O)
    return t


def eclipse_sight():
    """The Sight: an eye of black glass, its iris a ring of gold round a black pupil with one glint, ringed by thin gold
    rays like a corona."""
    cx = cy = 7.5
    gold = [hexc("#5a3a0a"), hexc("#b07d18"), hexc("#d9a92b"), hexc("#f2d257"), hexc("#fff2a8")]

    def fn(x, y):
        dx, dy = x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5
        d = math.hypot(dx, dy)
        if d > 4.9:
            return None
        if d < 1.3:
            return hexc("#000000")
        if d < 3.1:
            # The iris: gold, lit from above, fibres radiating.
            v = 3 - (dy / 3.1) * 1.2
            ang = math.atan2(dy, dx)
            if math.sin(ang * 7) > 0.6:
                v -= 1
            return gold[int(max(1, min(4, round(v))))]
        # The black glass round it, a violet sheen on its upper-left.
        lit = -(dx + dy) / (d + 1e-6)
        if d > 4.0 and lit > 0.5:
            return hexc("#5a3a9a")
        return mix(hexc("#0a0612"), hexc("#1c1030"), max(0.0, lit) * 0.8)
    t = _paint(fn)
    t.set(7, 6, hexc("#ffffff"))
    item_outline(t, O)
    # The corona: eight thin rays, long on the axes, short on the diagonals, gold fading to amber at their tips.
    for (x, y, k) in ((8, 0, 0), (8, 1, 1), (8, 15, 0), (8, 14, 1), (0, 8, 0), (1, 8, 1), (15, 8, 0), (14, 8, 1),
                      (2, 2, 0), (13, 2, 0), (2, 13, 0), (13, 13, 0), (3, 3, 1), (12, 3, 1), (3, 12, 1), (12, 12, 1)):
        if t.get(x, y)[3] == 0:
            t.set(x, y, gold[3] if k else gold[1])
    return t


def generate():
    for f in range(4):
        save(darkness_ash(f), "particle", f"darkness_ash_{f}")
    save(soul_wisp(), "particle", "soul_wisp")
    save(star_mote(), "particle", "star_mote")
    save(ink_drop(), "particle", "ink_drop")
    save(dawn_mote(), "particle", "dawn_mote")
    save(amaras_light(), "item", "amaras_light")
    save(void_essence(), "item", "void_essence")
    save(eclipse_sight(), "item", "eclipse_sight")
