"""Bowl-spell seals (MagicAssets.SEALS): the sigil a bowl's smoke draws in the air, 64 x 64, white/grey on alpha (tinted by
the spell's school at runtime).

Every seal is a frame that says what kind of working it is (curses wear thorns, seekers a compass rose, protections a
double ward, brews a ring of droplets, name-workings a devil's-trap band, Rowena's own a ring of moon phases) round an
emblem that says what it does (an eye to see, a chain to bind, a vessel to brew...). Emblems are drawn in pixels about
the centre (y down) and stay inside a radius of ~21 so they never touch the frame.
"""

import math

from vecdraw import Field

S = 64
C = S / 2
W1 = 1.7   # main stroke
W2 = 1.1   # detail stroke
DIM = 0.78  # grey for secondary lines
K = 1.15    # emblem scale: emblems are drawn on a ~18 px radius and blown up to fill the frame


# --- shape helpers (all relative to the centre) ----------------------------------------------------------------------------

def P(x, y):
    return (C + K * x, C + K * y)


def line(f, pts, w=W1, v=1.0, closed=False, a=1.0):
    f.poly([P(*p) for p in pts], w, a, v, closed=closed)


def cutline(f, pts, w=3.0):
    f.poly([P(*p) for p in pts], w, 1.0, cut=True)


def ell(f, cx, cy, rx, ry, rot=0.0, a0=0, a1=360, w=W1, v=1.0, cut=False):
    n = max(10, int(abs(a1 - a0) / 8))
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    pts = []
    for i in range(n + 1):
        t = math.radians(a0 + (a1 - a0) * i / n)
        x, y = rx * math.cos(t), ry * math.sin(t)
        pts.append(P(cx + x * ca - y * sa, cy + x * sa + y * ca))
    f.poly(pts, w, 1.0, v, cut=cut)


def circle(f, cx, cy, r, w=W1, v=1.0):
    f.ring(*P(cx, cy), r * K, w, 1.0, v)


def dot(f, cx, cy, r, v=1.0):
    f.disc(*P(cx, cy), r * K, 1.0, v)


def hole(f, cx, cy, r):
    f.disc(*P(cx, cy), r * K, 1.0, cut=True)


def solid(f, pts, v=1.0):
    f.fill([P(*p) for p in pts], 1.0, v)


def clear(f, pts):
    f.fill([P(*p) for p in pts], 1.0, cut=True)


def arrow(f, x0, y0, x1, y1, head=4.0, w=W1, v=1.0):
    line(f, [(x0, y0), (x1, y1)], w, v)
    a = math.atan2(y1 - y0, x1 - x0)
    for s in (2.5, -2.5):
        line(f, [(x1 - head * math.cos(a + s / 3), y1 - head * math.sin(a + s / 3)), (x1, y1)], w, v)


def eye(f, cx, cy, rx, ry, iris=None, pupil=1.6, slit=False, w=W1):
    ell(f, cx, cy, rx, ry, 0, 180, 360, w)
    ell(f, cx, cy, rx, ry, 0, 0, 180, w)
    iris = ry * 0.85 if iris is None else iris
    circle(f, cx, cy, iris, W2)
    if slit:
        solid(f, [(cx, cy - iris * 0.9), (cx + 1.1, cy), (cx, cy + iris * 0.9), (cx - 1.1, cy)])
    else:
        dot(f, cx, cy, pupil)


def paw(f, cx, cy, k=1.0):
    solid(f, [(cx + k * x, cy + k * y) for x, y in _ellipse_pts(0, 2.2, 4.2, 3.4)])
    for x, y, r in ((-4.6, -2.6, 1.6), (-1.7, -5.6, 1.7), (1.7, -5.6, 1.7), (4.6, -2.6, 1.6)):
        dot(f, cx + k * x, cy + k * y, r * k)


def _ellipse_pts(cx, cy, rx, ry, n=24):
    return [(cx + rx * math.cos(2 * math.pi * i / n), cy + ry * math.sin(2 * math.pi * i / n)) for i in range(n)]


def person(f, cx, cy, k=1.0, w=W1):
    circle(f, cx, cy - 6 * k, 3.4 * k, w)
    ell(f, cx, cy + 6 * k, 7 * k, 7 * k, 0, 180, 360, w)
    line(f, [(cx - 7 * k, cy + 6 * k), (cx + 7 * k, cy + 6 * k)], w)


def headstone(f, cx, cy, k=1.0):
    ell(f, cx, cy - 4 * k, 6 * k, 6 * k, 0, 180, 360)
    line(f, [(cx - 6 * k, cy - 4 * k), (cx - 6 * k, cy + 9 * k), (cx + 6 * k, cy + 9 * k), (cx + 6 * k, cy - 4 * k)])
    line(f, [(cx, cy - 6 * k), (cx, cy + 4 * k)], W2)
    line(f, [(cx - 3 * k, cy - 3 * k), (cx + 3 * k, cy - 3 * k)], W2)
    line(f, [(cx - 10 * k, cy + 9 * k), (cx + 10 * k, cy + 9 * k)], W2, DIM)


def drop(f, cx, cy, r, w=W1, filled=False):
    """A teardrop: round bottom at (cx, cy), tip 2.2r above."""
    tip = (cx, cy - 2.3 * r)
    a = math.degrees(math.asin(r / (2.3 * r)))
    if filled:
        pts = [tip] + [(cx + r * math.cos(math.radians(t)), cy + r * math.sin(math.radians(t)))
                       for t in range(int(-a), int(180 + a) + 1, 10)]
        solid(f, pts)
    else:
        ell(f, cx, cy, r, r, 0, -a, 180 + a, w)
        line(f, [(cx + r * math.cos(math.radians(-a)), cy + r * math.sin(math.radians(-a))), tip,
                 (cx + r * math.cos(math.radians(180 + a)), cy + r * math.sin(math.radians(180 + a)))], w)


def link(f, cx, cy, rot, L=6.0, r=3.2, w=W1):
    """A chain link: a stadium of half-length L and radius r, rotated by rot degrees."""
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))

    def R(x, y):
        return (cx + x * ca - y * sa, cy + x * sa + y * ca)
    pts = []
    for i in range(13):
        t = math.radians(-90 + 180 * i / 12)
        pts.append(R(L - r + r * math.cos(t), r * math.sin(t)))
    for i in range(13):
        t = math.radians(90 + 180 * i / 12)
        pts.append(R(-L + r + r * math.cos(t), r * math.sin(t)))
    line(f, pts, w, closed=True)


def witch_knot(f, r=14, w=W1):
    """The witch's knot: two crossed vesicas and the circle that ties them."""
    ell(f, 0, 0, r, r * 0.34, 0, 0, 360, w)
    ell(f, 0, 0, r, r * 0.34, 90, 0, 360, w)
    circle(f, 0, 0, r * 0.52, w)


def name_lines(f, cx, cy, wdt=14, n=3, gap=3.4, w=W2):
    for i in range(n):
        y = cy + (i - (n - 1) / 2) * gap
        ln = wdt * (1.0 if i % 2 == 0 else 0.72)
        # A written line: a stroke with small serifs, like handwriting seen from afar.
        line(f, [(cx - wdt / 2, y), (cx - wdt / 2 + ln, y)], w)
        line(f, [(cx - wdt / 2 + 2, y - 1.2), (cx - wdt / 2 + 3, y)], 0.8)


def shield(f, cx, cy, wd=14, h=17, w=W1):
    x0, x1, y0 = cx - wd / 2, cx + wd / 2, cy - h / 2
    pts = [(x0, y0), (cx, y0 - 1.5), (x1, y0)]
    for i in range(1, 13):
        t = i / 12
        pts.append((x1 - (wd / 2) * t ** 1.6, y0 + h * 0.45 + h * 0.55 * math.sin(t * math.pi / 2)))
    mirror = [(2 * cx - x, y) for x, y in reversed(pts[3:-1])]
    line(f, pts + mirror + [(x0, y0)], w)


def pouch(f, cx, cy):
    """A tied hex bag: a round sack, a cinched neck and two tufts."""
    ell(f, cx, cy + 4, 9, 8, 0, -60, 240)
    line(f, [(cx - 4.5, cy - 3), (cx + 4.5, cy - 3)], W1)
    line(f, [(cx - 3.4, cy - 3), (cx - 6, cy - 9)], W1)
    line(f, [(cx + 3.4, cy - 3), (cx + 6, cy - 9)], W1)
    line(f, [(cx, cy - 3), (cx, cy - 8)], W2)


def thorns(f, pts, every=3, length=3.0, w=W2):
    """Little thorns sprouting alternately off a polyline."""
    line(f, pts, W1)
    side = 1
    for i in range(1, len(pts) - 1, every):
        (x0, y0), (x1, y1) = pts[i - 1], pts[i + 1]
        a = math.atan2(y1 - y0, x1 - x0) + side * math.pi / 2.6
        x, y = pts[i]
        solid(f, [(x - 1.0 * math.cos(a - side * 1.2), y - 1.0 * math.sin(a - side * 1.2)),
                  (x + length * math.cos(a), y + length * math.sin(a)),
                  (x + 1.0 * math.cos(a + side * 1.2), y + 1.0 * math.sin(a + side * 1.2))])
        side = -side


def rays(f, cx, cy, r0, r1, n, a0=-90, spread=360, w=W2, v=1.0):
    for i in range(n):
        a = math.radians(a0 + (spread * i / n if spread == 360 else spread * (i / max(1, n - 1) - 0.5)))
        line(f, [(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r1 * math.cos(a), cy + r1 * math.sin(a))], w, v)


def wing(f, cx, cy, k=1.0, flip=False):
    sx = -1 if flip else 1
    # The leading edge, then feathers fanning down from it.
    edge = [(cx + sx * k * x, cy + k * y) for x, y in ((-2, 4), (2, -4), (8, -8), (14, -7))]
    line(f, edge, W1)
    for i, (x, y) in enumerate(((1, -2), (5, -6), (9, -7.5), (13, -7))):
        ln = 7 + i * 2.2
        line(f, [(cx + sx * k * x, cy + k * y), (cx + sx * k * (x + ln * 0.25), cy + k * (y + ln))], W2 if i % 2 else W1)


def skull(f, cx, cy, k=1.0):
    pts = []
    for i in range(0, 181, 10):
        t = math.radians(180 + i)
        pts.append((cx + 8 * k * math.cos(t), cy - 1 * k + 8 * k * math.sin(t)))
    pts += [(cx + 8 * k, cy + 2 * k), (cx + 5 * k, cy + 4 * k), (cx + 5 * k, cy + 8 * k), (cx - 5 * k, cy + 8 * k),
            (cx - 5 * k, cy + 4 * k), (cx - 8 * k, cy + 2 * k)]
    solid(f, pts)
    hole(f, cx - 3.3 * k, cy, 2.3 * k)
    hole(f, cx + 3.3 * k, cy, 2.3 * k)
    clear(f, [(cx, cy + 2.4 * k), (cx - 1.3 * k, cy + 4.6 * k), (cx + 1.3 * k, cy + 4.6 * k)])
    for x in (-2.5, 0, 2.5):
        line(f, [(cx + x * k, cy + 5.6 * k), (cx + x * k, cy + 8.4 * k)], 0.9)
        f.seg(*P(cx + x * k, cy + 5.8 * k), *P(cx + x * k, cy + 8.4 * k), 0.9, 1.0, cut=True)


def heart(f, cx, cy, k=1.0, w=W1):
    pts = []
    for i in range(61):
        t = 2 * math.pi * i / 60
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((cx + x * k * 0.55, cy + y * k * 0.55))
    line(f, pts, w)


def vessel_flask(f, cx, cy, k=1.0):
    """A round-bellied flask with a stopper."""
    ell(f, cx, cy + 4 * k, 8 * k, 8 * k, 0, -62, 242)
    line(f, [(cx - 3.6 * k, cy - 3 * k), (cx - 3 * k, cy - 9 * k)])
    line(f, [(cx + 3.6 * k, cy - 3 * k), (cx + 3 * k, cy - 9 * k)])
    line(f, [(cx - 4.5 * k, cy - 9.5 * k), (cx + 4.5 * k, cy - 9.5 * k)])
    line(f, [(cx - 2 * k, cy - 12.5 * k), (cx + 2 * k, cy - 12.5 * k)], W2)
    line(f, [(cx - 6 * k, cy + 5 * k), (cx + 6 * k, cy + 5 * k)], W2, DIM)


def crack(f, pts, w=2.2):
    f.poly([P(*p) for p in pts], w, 1.0, cut=True)


# --- frames -----------------------------------------------------------------------------------------------------------

def frame(f, style):
    f.ring(C, C, 29.6, 1.8)
    f.ring(C, C, 24.8, 1.0, 1.0, 0.85)
    r = 27.2
    if style == "rune":
        for i in range(16):
            a = math.radians(i * 22.5)
            if i % 4 == 0:
                x, y = C + r * math.cos(a), C + r * math.sin(a)
                f.poly([(x + 2.1 * math.cos(a), y + 2.1 * math.sin(a)), (x - 1.6 * math.sin(a), y + 1.6 * math.cos(a)),
                        (x - 2.1 * math.cos(a), y - 2.1 * math.sin(a)), (x + 1.6 * math.sin(a), y - 1.6 * math.cos(a))],
                       1.0, 1.0, 0.95, closed=True)
            else:
                f.disc(C + r * math.cos(a), C + r * math.sin(a), 0.75, 1.0, 0.8)
    elif style == "thorn":
        for i in range(18):
            a = math.radians(i * 20 + 10)
            b = math.radians(i * 20 + 10 + (9 if i % 2 else -9))
            f.fill([(C + 25.2 * math.cos(a - 0.09), C + 25.2 * math.sin(a - 0.09)),
                    (C + 25.2 * math.cos(a + 0.09), C + 25.2 * math.sin(a + 0.09)),
                    (C + 29.0 * math.cos(b), C + 29.0 * math.sin(b))], 1.0, 0.9)
        f.ring(C, C, 31.2, 0.8, 0.5, 0.7)
    elif style == "compass":
        for i in range(32):
            a = math.radians(i * 11.25)
            r0 = 25.6 if i % 4 else 25.4
            r1 = 26.8 if i % 2 else 27.8
            if i % 8 == 0:
                continue
            f.seg(C + r0 * math.cos(a), C + r0 * math.sin(a), C + r1 * math.cos(a), C + r1 * math.sin(a), 0.9, 1.0, 0.8)
        for i in range(4):
            a = math.radians(i * 90 - 90)
            tip = (C + 31.6 * math.cos(a), C + 31.6 * math.sin(a))
            f.fill([tip, (C + 25 * math.cos(a) - 2.4 * math.sin(a), C + 25 * math.sin(a) + 2.4 * math.cos(a)),
                    (C + 25 * math.cos(a) + 2.4 * math.sin(a), C + 25 * math.sin(a) - 2.4 * math.cos(a))], 1.0, 1.0)
    elif style == "ward":
        f.ring(C, C, 27.2, 1.0, 1.0, 0.9)
        for i in range(8):
            a = math.radians(i * 45 + 22.5)
            x, y = C + 27.2 * math.cos(a), C + 27.2 * math.sin(a)
            f.disc(x, y, 1.9, 1.0, cut=True)
            f.disc(x, y, 1.1, 1.0, 1.0)
    elif style == "brew":
        for i in range(12):
            a = math.radians(i * 30)
            x, y = C + r * math.cos(a), C + r * math.sin(a)
            if i % 2 == 0:
                f.disc(x, y, 1.15, 1.0, 0.95)
            else:
                f.seg(x - 0.9 * math.cos(a + 1.57), y - 0.9 * math.sin(a + 1.57),
                      x + 0.9 * math.cos(a + 1.57), y + 0.9 * math.sin(a + 1.57), 0.8, 0.8, 0.75)
    elif style == "trap":
        # A devil's-trap band: a pentagram's points pressed into the rim.
        for i in range(5):
            a = math.radians(-90 + 72 * i)
            f.fill([(C + 29.2 * math.cos(a), C + 29.2 * math.sin(a)),
                    (C + 25 * math.cos(a - 0.2), C + 25 * math.sin(a - 0.2)),
                    (C + 25 * math.cos(a + 0.2), C + 25 * math.sin(a + 0.2))], 1.0, 0.9)
            for j in (1, 2, 3):
                b = a + math.radians(72 * j / 4)
                f.seg(C + 25.6 * math.cos(b), C + 25.6 * math.sin(b), C + 28.6 * math.cos(b), C + 28.6 * math.sin(b),
                      0.9, 1.0, 0.75)
    elif style == "moon":
        for i in range(8):
            a = math.radians(i * 45 - 90)
            x, y = C + r * math.cos(a), C + r * math.sin(a)
            f.disc(x, y, 1.9, 1.0, cut=True)
            f.disc(x, y, 1.6, 1.0, 1.0)
            ph = (i % 8) / 8.0
            if i != 4:
                off = 1.0 + 2.4 * abs(math.cos(math.pi * ph))
                f.disc(x + off * (1 if i < 4 else -1), y, 1.6, 1.0, cut=True)


# --- emblems ----------------------------------------------------------------------------------------------------------

def e_banish(f):
    circle(f, 0, 0, 4.2)
    dot(f, 0, 0, 1.4)
    for i in range(4):
        a = math.radians(45 + 90 * i)
        arrow(f, 7 * math.cos(a), 7 * math.sin(a), 18 * math.cos(a), 18 * math.sin(a), 4.5)
    for i in range(4):
        a = math.radians(90 * i)
        line(f, [(6 * math.cos(a), 6 * math.sin(a)), (12 * math.cos(a), 12 * math.sin(a))], W2, DIM)


def e_bind(f):
    # Three links: open, edge-on, open.
    link(f, -8.2, -8.2, 45, 7.4, 3.8)
    link(f, 8.2, 8.2, 45, 7.4, 3.8)
    f.poly([P(-4.2, -4.2), P(4.2, 4.2)], 4.6, 1.0, cut=True)
    link(f, 0, 0, 45, 6.2, 1.3)
    line(f, [(-12, 12), (-15, 15)], W2, DIM)
    line(f, [(12, -12), (15, -15)], W2, DIM)


def e_break_deal(f):
    # A contract torn in two, a seal on its lower half.
    line(f, [(-9, -16), (-1, -16), (1, -11), (-2, -6), (2, -1), (-1, 4), (2, 10), (0, 16), (-9, 16), (-9, -16)])
    line(f, [(4, -15), (11, -15), (11, 17), (5, 17), (7, 11), (4, 5), (7, 0), (3, -6), (6, -11), (4, -15)])
    for y in (-11, -7, -3):
        line(f, [(-7, y), (-3, y)], W2, DIM)
        line(f, [(8, y + 1), (9.5, y + 1)], W2, DIM)
    dot(f, -5, 10, 2.8)
    hole(f, -5, 10, 1.2)


def e_concealment(f):
    # A closed eye under a veil.
    ell(f, 0, -2, 13, 7, 0, 0, 180)
    for x in (-8, -3, 3, 8):
        line(f, [(x, 4.4 if abs(x) < 5 else 3.0), (x * 1.25, 9)], W2)
    ell(f, 0, 6, 17, 13, 0, 200, 340, W1, DIM)
    for x in (-12, 0, 12):
        line(f, [(x, -9 + abs(x) * 0.25), (x * 1.1, -4 + abs(x) * 0.3)], W2, DIM)


def e_find_the_author(f):
    # A quill over a written page.
    line(f, [(-12, -13), (6, -13), (12, -7), (12, 15), (-12, 15), (-12, -13)], W1, DIM)
    line(f, [(6, -13), (6, -7), (12, -7)], W2, DIM)
    for y in (-4, 0, 4, 8):
        line(f, [(-8, y), (2 if y % 8 else 5, y)], W2, DIM)
    line(f, [(-6, 12), (14, -16)], W1)
    ell(f, 6, -6, 9, 3.3, -54, 0, 360, W1)
    line(f, [(-6, 12), (-8, 15)], W2)


def e_hex_bag_curse(f):
    pouch(f, 0, 2)
    thorns(f, [(-8 + i, 6 + 1.8 * math.sin(i / 2.5)) for i in range(17)], 3, 2.6)
    dot(f, 0, -12, 1.2)


def e_hex_bag_protection(f):
    pouch(f, 0, 2)
    line(f, [(0, 0), (0, 10)], W2)
    line(f, [(-4, 4), (4, 4)], W2)
    ell(f, 0, -11.5, 5, 1.8, 0, 0, 360, W2, DIM)


def e_locate_grave(f):
    headstone(f, 0, -1, 1.15)


def e_locate_pet(f):
    paw(f, 0, 2, 1.55)


def e_locate_player(f):
    person(f, 0, 0, 1.35)


def e_locate_spire(f):
    line(f, [(-6, 16), (-5, -4), (0, -18), (5, -4), (6, 16)])
    line(f, [(-9, 16), (9, 16)])
    line(f, [(-5, -4), (5, -4)], W2)
    line(f, [(-5.5, 5), (5.5, 5)], W2)
    ell(f, 0, 0, 1.8, 3.2, 0, 180, 360, W2)
    line(f, [(-1.8, 0), (-1.8, 2.5)], W2)
    line(f, [(1.8, 0), (1.8, 2.5)], W2)
    for x in (-12, 12):
        line(f, [(x, 14), (x * 0.92, 4)], W2, DIM)


def e_purification(f):
    drop(f, 0, 6, 7)
    line(f, [(0, -4), (0, 9)], W2)
    line(f, [(-4, 3), (4, 3)], W2)
    rays(f, 0, -2, 14, 19, 7, -90, 140, W2)


def e_purified_blood(f):
    drop(f, 0, 6, 7.5, W1, filled=True)
    hole(f, -2.6, 6.6, 1.6)
    ell(f, 0, -15, 7, 2.4, 0, 0, 360, W1)


def e_revive_pet(f):
    paw(f, 0, 6, 1.15)
    ell(f, 0, 2, 13, 13, 0, 200, 340, W1, DIM)
    line(f, [(0, -19), (0, -10)], W2)
    line(f, [(-3.5, -15.5), (3.5, -15.5)], W2)


def e_second_sight(f):
    eye(f, 0, 4, 13, 6.5, 4.4, 1.8)
    eye(f, 0, -10, 6, 3.4, 2.2, 1.0, w=W2)
    rays(f, 0, -10, 6.5, 10, 5, -90, 120, W2, DIM)


def e_summon_crossroads(f):
    line(f, [(-18, -12), (18, 12)], W1)
    line(f, [(-18, 12), (18, -12)], W1)
    line(f, [(-15, -12), (18, 10)], W2, DIM)
    line(f, [(-15, 12), (18, -10)], W2, DIM)
    clear(f, [(-5, -5), (5, -5), (5, 5), (-5, 5)])
    line(f, [(-4, -4), (4, -4), (4, 4), (-4, 4)], W1, closed=True)
    line(f, [(-4, -1), (4, -1)], W2)
    dot(f, 0, 1.5, 0.9)


def e_summon_messenger(f):
    wing(f, -1, 2, 1.05)
    wing(f, 1, 2, 1.05, flip=True)
    dot(f, 0, -2, 2.0)
    ell(f, 0, -8, 4.5, 1.6, 0, 0, 360, W2)


def e_curse_of_misfortune(f):
    # An upturned horseshoe, the luck running out of it.
    pts = []
    for i in range(0, 201, 10):
        t = math.radians(170 - i)
        pts.append((10 * math.cos(t), 2 - 11 * math.sin(t)))
    line(f, [(-12, 13)] + pts[::-1][::-1] + [(12, 13)], 3.6)
    for i in range(0, 201, 40):
        t = math.radians(170 - i)
        hole(f, 10 * math.cos(t), 2 - 11 * math.sin(t), 0.8)
    for x, y in ((-11, 15.5), (-11, 18.5), (12, 16)):
        dot(f, x, y, 0.9)
    crack(f, [(4, -10), (2, -7), (4, -5)], 1.4)


def e_curse_of_pain(f):
    heart(f, 0, 2, 0.9)
    for a in (-35, 0, 35):
        r = math.radians(a + 90)
        x0, y0 = 19 * math.cos(r) * -1, -19 * math.sin(r)
        line(f, [(x0 * 0.9, y0), (x0 * 0.1, y0 * 0.05 + 2)], W2)
        dot(f, x0 * 0.9, y0, 1.5)


def e_curse_of_frailty(f):
    # A bone, cracked through.
    line(f, [(-9, 9), (9, -9)], 4.2)
    for x, y in ((-9, 9), (9, -9)):
        for s in (1, -1):
            dot(f, x + s * 2.2 - (2.0 if x < 0 else -2.0) * 0.4, y + s * 2.2 + (2.0 if x < 0 else -2.0) * 0.4, 2.9)
    crack(f, [(-3, -2), (-0.5, 0.5), (-2.5, 1.5), (1, 4)], 1.6)


def e_curse_of_silence(f):
    ell(f, 0, 2, 13, 5, 0, 180, 360, W1)
    ell(f, 0, -1, 13, 9, 0, 20, 160, W1)
    line(f, [(-13, 2), (13, 2)], W2)
    for x in (-8, -4, 0, 4, 8):
        line(f, [(x - 1.2, -2.5), (x + 1.2, 6.5)], W2)
    ell(f, 0, 0, 16, 16, 0, 200, 340, W2, DIM)


def e_hex_of_rot(f):
    skull(f, 0, -2, 1.25)
    for x, ln in ((-4, 3.0), (1, 5.0), (5, 2.5)):
        line(f, [(x, 9.5), (x, 9.5 + ln)], W2, DIM)
        dot(f, x, 10.5 + ln, 0.9, DIM)


def e_locate_being(f):
    eye(f, 0, 0, 15, 8, 6.5, slit=True)


def e_scrying(f):
    # A bowl with an eye looking back out of its water.
    ell(f, 0, 2, 15, 12, 0, 0, 180)
    line(f, [(-15, 2), (15, 2)], W1)
    ell(f, 0, 2, 11, 2.2, 0, 0, 180, W2, DIM)
    line(f, [(-5, 14), (5, 14)], W1)
    eye(f, 0, -8, 9, 4.6, 3.2, 1.3, w=W2)
    for x in (-9, 9):
        line(f, [(x, -2), (x * 1.3, -6)], W2, DIM)


def e_trace_the_familiar(f):
    for (x, y, k) in ((-10, 11, 0.62), (-1, 2, 0.72), (8, -8, 0.82)):
        paw(f, x, y, k)
    ell(f, -2, 2, 19, 19, 0, 60, 120, W2, DIM)


def e_binding_of_the_name(f):
    # A written name with a chain drawn tight across it.
    name_lines(f, 0, 0, 24, 4, 5.0, W1)
    f.poly([P(-15, 11), P(15, -11)], 7.0, 1.0, cut=True)
    for i, (x, y) in enumerate(((-10.5, 7.7), (0, 0), (10.5, -7.7))):
        link(f, x, y, -36.3, 6.8 if i != 1 else 5.8, 3.3 if i != 1 else 1.2)


def e_banish_by_name(f):
    name_lines(f, -3, 0, 16, 3, 5.0, W1)
    arrow(f, 7, 0, 19, 0, 4.5)
    line(f, [(-14, -12), (-14, 12)], W2, DIM)


def e_compel(f):
    # A cross-bar puppeteer's control and its strings.
    line(f, [(-13, -14), (13, -14)], W1)
    line(f, [(0, -18), (0, -10)], W1)
    for x0, x1, y1 in ((-11, -6, 3), (11, 6, 3), (0, 0, -2)):
        line(f, [(x0, -14), (x1, y1)], 0.9, DIM)
    person(f, 0, 9, 0.9)
    dot(f, -6, 3, 0.9)
    dot(f, 6, 3, 0.9)


def e_break_hex(f):
    witch_knot(f, 14)
    line(f, [(-17, 14), (17, -14)], 2.6)
    f.poly([P(-17, 14), P(17, -14)], 5.2, 1.0, cut=True)
    line(f, [(-17, 16), (15, -11)], W1)
    line(f, [(-15, 11), (17, -16)], W1)


def e_ward_of_the_name(f):
    shield(f, 0, 1, 22, 26)
    name_lines(f, 0, -2, 11, 3, 3.6, W2)


def e_mirror_hex(f):
    line(f, [(0, -17), (0, 17)], 2.4)
    for y in range(-14, 16, 4):
        line(f, [(0.5, y), (3.5, y + 3)], 0.9, DIM)
    arrow(f, -17, -12, -2, -1, 0.1)
    arrow(f, -2, 1, -16, 12, 4.5)
    dot(f, -17, -12, 1.6)


def e_brew_chrism(f):
    vessel_flask(f, 0, 3, 1.15)
    # A cross of anointing on its belly and drops of oil.
    line(f, [(0, 3), (0, 11)], W2)
    line(f, [(-3, 6), (3, 6)], W2)
    drop(f, -14, -6, 1.9, W2)
    drop(f, 14, -6, 1.9, W2)


def e_brew_bound_blood(f):
    # A tall vial wrapped in twine, knotted at the front.
    line(f, [(-4, -14), (-4, 14), (4, 14), (4, -14)])
    line(f, [(-6, -15), (6, -15)])
    solid(f, [(-3, 4), (3, 4), (3, 13), (-3, 13)])
    for y in (-9, -3, 9):
        line(f, [(-6, y + 2), (6, y - 2)], W2)
    # A knotted bow on the twine.
    clear(f, [(-9, -1), (9, -1), (9, 5), (-9, 5)])
    line(f, [(-6, 3), (6, 1)], W2)
    ell(f, -4.6, 0.6, 3.6, 2.2, -20, 0, 360, W2)
    ell(f, 4.6, 0.6, 3.6, 2.2, 20, 0, 360, W2)
    dot(f, 0, 1.4, 1.5)
    line(f, [(-0.6, 2.6), (-3, 9)], W2)
    line(f, [(0.6, 2.6), (3, 9)], W2)


def e_brew_hexed_ink(f):
    # An inkwell, a quill and a curl of ink.
    line(f, [(-11, 6), (-9, 15), (9, 15), (11, 6), (-11, 6)])
    line(f, [(-5, 6), (-5, 2), (5, 2), (5, 6)])
    ell(f, 0, 9.5, 7, 1.6, 0, 0, 360, W2, DIM)
    line(f, [(2, 3), (14, -16)], W2)
    ell(f, 9.5, -9, 8, 2.6, -58, 0, 360, W1)
    pts = [(-4 + 7 * math.cos(t / 10) * (1 - t / 60), -8 + 7 * math.sin(t / 10) * (1 - t / 60)) for t in range(0, 50, 2)]
    line(f, pts, W2)


def e_familiar_bond(f):
    # A cat's head bound to a ring.
    ell(f, 0, -3, 10, 8.4, 0, -20, 200)
    line(f, [(-9.6, -5), (-9, -15), (-3.5, -10.8)])
    line(f, [(9.6, -5), (9, -15), (3.5, -10.8)])
    line(f, [(-3.5, -10.8), (3.5, -10.8)], W2)
    for s in (-1, 1):
        solid(f, [(s * 4.6, -4.6), (s * 3.2, -2.6), (s * 4.6, -0.6), (s * 6.0, -2.6)])
        line(f, [(s * 3, 1.5), (s * 13, 0)], 0.9, DIM)
        line(f, [(s * 3, 2.8), (s * 12.5, 4)], 0.9, DIM)
    dot(f, 0, 0.4, 1.0)
    circle(f, 0, 12.5, 4.2)
    line(f, [(0, 5.2), (0, 8.3)], W2)


def e_unmark(f):
    # The Mark of Cain, struck through and broken.
    line(f, [(-6, -14), (6, -14), (-3, -2), (8, -2), (-2, 14)], 2.6)
    line(f, [(-9, 6), (3, 6)], 2.2)
    f.poly([P(-17, -13), P(17, 13)], 4.6, 1.0, cut=True)
    line(f, [(-18, -15), (16, 11)], W1)
    line(f, [(-16, -11), (18, 15)], W1)


SEAL_ART = {
    "banish": ("trap", e_banish), "bind": ("trap", e_bind), "break_deal": ("rune", e_break_deal),
    "concealment": ("ward", e_concealment), "find_the_author": ("compass", e_find_the_author),
    "hex_bag_curse": ("thorn", e_hex_bag_curse), "hex_bag_protection": ("ward", e_hex_bag_protection),
    "locate_grave": ("compass", e_locate_grave), "locate_pet": ("compass", e_locate_pet),
    "locate_player": ("compass", e_locate_player), "locate_spire": ("compass", e_locate_spire),
    "purification": ("ward", e_purification), "purified_blood": ("brew", e_purified_blood),
    "revive_pet": ("moon", e_revive_pet), "second_sight": ("rune", e_second_sight),
    "summon_crossroads": ("rune", e_summon_crossroads), "summon_messenger": ("rune", e_summon_messenger),
    "curse_of_misfortune": ("thorn", e_curse_of_misfortune), "curse_of_pain": ("thorn", e_curse_of_pain),
    "curse_of_frailty": ("thorn", e_curse_of_frailty), "curse_of_silence": ("thorn", e_curse_of_silence),
    "hex_of_rot": ("thorn", e_hex_of_rot), "locate_being": ("compass", e_locate_being),
    "scrying": ("compass", e_scrying), "trace_the_familiar": ("compass", e_trace_the_familiar),
    "binding_of_the_name": ("trap", e_binding_of_the_name), "banish_by_name": ("trap", e_banish_by_name),
    "compel": ("moon", e_compel), "break_hex": ("ward", e_break_hex), "ward_of_the_name": ("ward", e_ward_of_the_name),
    "mirror_hex": ("ward", e_mirror_hex), "brew_chrism": ("brew", e_brew_chrism),
    "brew_bound_blood": ("brew", e_brew_bound_blood), "brew_hexed_ink": ("brew", e_brew_hexed_ink),
    "familiar_bond": ("moon", e_familiar_bond), "unmark": ("moon", e_unmark),
}


def seal(spell):
    style, emblem = SEAL_ART[spell]
    f = Field(S)
    frame(f, style)
    emblem(f)
    f.glow(1, 0.3)
    return f.bake()
