"""The Hand of God: Metatron's quill-hand, out of a door in Heaven.

One GeckoLib model, three parts:
  * the PORTAL -- three ofanim rings of engraved gold (turning on their own axes, the middle one full of
    eyes), a bank of cloud and a disc of light, with rays falling from it;
  * the SLEEVE -- a scribe's robe falling from the portal in three widening tiers, gold-hemmed and
    embroidered, closed at the wrist by a gold bracer of glowing runes;
  * the HAND -- marble shot with veins of grey and of light, a palm with its pads, four fingers of three
    jointed phalanges with nails and knuckles, a jointed thumb, a signet ring; and the quill it holds,
    barbed and gold-nibbed.

The origin is the quill's nib: where the entity stands is where it writes. Everything else rises above
it (about 190 px; drawn at 0.85 scale, some ten blocks).
"""

import math
import random

import palette as P
from animkit import AnimFile
from azazel_art import ANIM, GEO
from common import save
from geomodel import Rig, box
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

MARBLE = Ramp("#b9b0a1", "#cfc6b6", "#ded6c8", "#ebe5da", "#f4f0e8", "#fdfbf6")
VEIN = hexc("#8e98a6")
ROBE = Ramp("#a69f92", "#c3bcae", "#d8d2c6", "#e8e3d9", "#f3efe7", "#fbf9f4")
GOLD = Ramp("#5a3c0c", "#8a5f14", "#bb8a22", "#e0b53c", "#f6d76a", "#fff3b0")
CLOUD = Ramp("#b8ab95", "#d3c8b4", "#e6ddcc", "#f2ebdf", "#f9f5ee", "#ffffff")
LIGHT = [hexc("#ffe08a"), hexc("#fff1c2"), hexc("#ffffff")]
INK = hexc("#120d14")

SHADE = {"up": 1.08, "north": 1.0, "east": 0.9, "west": 0.9, "south": 0.84, "down": 0.72}

# --- geometry -------------------------------------------------------------------------------

# Natural coordinates (before the nib is moved to the origin).
PALM_TOP, PALM_BOTTOM = 78, 46
FINGERS = [  # (name, centre x, width, lengths of the three phalanges, splay)
    ("index", -11.0, 7, (13, 9, 8), 5),
    ("middle", -3.5, 7, (15, 10, 8), 1),
    ("ring", 4.0, 7, (14, 9, 7), -3),
    ("pinky", 11.0, 6, (11, 7, 6), -8),
]
# How each finger curls: the index guides the quill, the others fold in under the palm.
CURLS = {"index": (-6, -10, -8), "middle": (-12, -26, -20), "ring": (-16, -30, -22), "pinky": (-20, -32, -24)}
PORTAL_Y = 138
# The hand is built at a natural size and then enlarged about the wrist, so it dominates its sleeve.
HAND_SCALE = 1.45
NIB = (-11.0, 4.0, -13.0)


class Parts:
    """Every cube, by role, for the painter."""

    def __init__(self):
        self.marble, self.nails, self.gold, self.runes, self.robe, self.hem = [], [], [], [], [], []
        self.clouds, self.eyes, self.discs, self.rays, self.shaft, self.vane, self.nib = [], [], [], [], [], [], []


def rig():
    r = Rig("scribe_hand", 1024, 512)
    p = Parts()
    r.bone("root", (0, 0, 0))

    # --- the portal ---------------------------------------------------------------------
    r.bone("portal", (0, PORTAL_Y, 0), "root")
    disc = r.bone("disc", (0, PORTAL_Y, 0), "portal")
    p.discs.append(box(disc, (-44, PORTAL_Y - 7, -44), (88, 0, 88), faces=("down",), tag="disc_down"))
    p.discs.append(box(disc, (-30, PORTAL_Y + 4, -30), (60, 0, 60), faces=("up",), tag="disc_up"))
    clouds = r.bone("clouds", (0, PORTAL_Y, 0), "portal")
    rng = random.Random(7)
    for k in range(18):
        a = 2 * math.pi * k / 18 + rng.uniform(-0.12, 0.12)
        d = 26 + rng.uniform(0, 16)
        w, h, dep = rng.randint(14, 22), rng.randint(6, 10), rng.randint(12, 18)
        cx, cz = math.cos(a) * d, math.sin(a) * d
        y = PORTAL_Y - 9 + rng.randint(-2, 3)
        p.clouds.append(box(clouds, (cx - w / 2, y, cz - dep / 2), (w, h, dep), tag="cloud",
                            rotation=(0, -math.degrees(a), 0), pivot=(cx, y, cz)))
        if k % 2 == 0:  # a second, smaller puff on top
            w2, h2 = w - 6, h - 2
            p.clouds.append(box(clouds, (cx - w2 / 2 + 2, y + h - 1, cz - dep / 2 + 3), (w2, h2, dep - 6), tag="cloud",
                                rotation=(0, -math.degrees(a), 0), pivot=(cx, y, cz)))
    rays = r.bone("rays", (0, PORTAL_Y, 0), "portal")
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        cx, cz = math.cos(a) * 24, math.sin(a) * 24
        p.rays.append(box(rays, (cx - 6, PORTAL_Y - 46, cz), (12, 38, 0), faces=("north", "south"), tag="ray",
                          rotation=(0, -math.degrees(a) + 90, 0), pivot=(cx, PORTAL_Y, cz)))
    _ring(r, p, "ring_a", None, 52, 36, (11, 4, 3), plate_every=4, eyes=False)
    tilt_b = r.bone("tilt_b", (0, PORTAL_Y + 4, 0), "portal", rotation=(72, 0, 0))
    _ring(r, p, "ring_b", tilt_b.name, 42, 30, (10, 3, 3), plate_every=3, eyes=True)
    tilt_c = r.bone("tilt_c", (0, PORTAL_Y + 4, 0), "portal", rotation=(0, 35, 62))
    _ring(r, p, "ring_c", tilt_c.name, 34, 26, (9, 3, 2), plate_every=4, eyes=False)

    # --- the sleeve -----------------------------------------------------------------------
    sleeve = r.bone("sleeve", (0, PORTAL_Y, 0), "root")
    # Narrow where the arm comes out of Heaven, flaring towards the wrist.
    tiers = [(PORTAL_Y - 20, 18, 28, 22), (PORTAL_Y - 38, 18, 34, 26), (PORTAL_Y - 56, 18, 40, 30)]
    for y0, h, w, d in tiers:
        p.robe.append(box(sleeve, (-w / 2, y0, -d / 2), (w, h, d), tag="robe"))
    # The hem of the lowest tier, and folds standing out of each.
    y_low, _, w_low, d_low = tiers[-1]
    p.hem.append(box(sleeve, (-w_low / 2, y_low - 3, -d_low / 2), (w_low, 4, d_low), inflate=0.5, tag="hem"))
    # Long folds running the sleeve's length, leaning out with the flare: they soften the tiers into one fall of cloth.
    for k in range(8):
        ang = 2 * math.pi * (k + 0.5) / 8
        cx, cz = math.cos(ang) * 14, math.sin(ang) * 11
        lean = 7
        p.robe.append(box(sleeve, (cx - 2, PORTAL_Y - 56, cz - 2), (4, 52, 4), tag="fold",
                          rotation=(lean * math.sin(ang), 0, -lean * math.cos(ang)), pivot=(cx, PORTAL_Y - 4, cz)))
    # The flared sleeve hangs lower behind the wrist, as a long robe sleeve does.
    p.robe.append(box(sleeve, (-w_low / 2 + 2, y_low - 14, d_low / 2 - 8), (w_low - 4, 14, 8), tag="drape"))
    p.hem.append(box(sleeve, (-w_low / 2 + 2, y_low - 17, d_low / 2 - 8), (w_low - 4, 3, 8), inflate=0.4, tag="hem"))
    for (y0, h, w, d), xs in zip(tiers, ([-9, 0, 9], [-13, -4, 5, 14], [-18, -9, 0, 9, 18])):
        for x in xs:
            p.robe.append(box(sleeve, (x - 1.5, y0 + 1, -d / 2 - 1), (3, h - 1, 1), tag="fold"))
            p.robe.append(box(sleeve, (x - 1.5, y0 + 1, d / 2), (3, h - 1, 1), tag="fold"))
    # The wrist, out of the sleeve, and its bracer.
    p.marble.append(box(sleeve, (-12, PALM_TOP - 4, -6), (24, 14, 12), density=2, tag="wrist"))
    p.gold.append(box(sleeve, (-13, PALM_TOP + 2, -7), (26, 7, 14), inflate=0.4, density=2, tag="bracer"))
    p.runes.append(p.gold[-1])

    # --- the hand ---------------------------------------------------------------------------
    hand = r.bone("hand", (0, PALM_TOP + 2, 0), "sleeve")
    # The palm narrows to the wrist and is no thicker than a hand.
    p.marble.append(box(hand, (-16, PALM_BOTTOM, -4.5), (32, 16, 9), density=2, tag="palm"))
    p.marble.append(box(hand, (-14, PALM_BOTTOM + 16, -4), (28, PALM_TOP - PALM_BOTTOM - 16, 8), density=2, tag="palm"))
    p.marble.append(box(hand, (-16, 52, -6.5), (11, 16, 3), density=2, tag="thenar"))
    p.marble.append(box(hand, (5, 48, -5.5), (10, 14, 2), density=2, tag="hypothenar"))
    for _, cx, w, _, _ in FINGERS:
        p.marble.append(box(hand, (cx - 1, PALM_BOTTOM + 2, 4), (2, 26, 1), density=2, tag="tendon"))
        p.marble.append(box(hand, (cx - w / 2 + 0.5, PALM_BOTTOM - 1, 1.5), (w - 1, 4, 4), inflate=0.3, density=2, tag="knuckle"))
    for name, cx, w, (l1, l2, l3), splay in FINGERS:
        parent, top = "hand", PALM_BOTTOM
        for i, (ln, curl) in enumerate(zip((l1, l2, l3), CURLS[name])):
            wd = w - min(i, 1)
            b = r.bone(f"{name}{i + 1}", (cx, top, 0), parent, rotation=(curl, 0, splay if i == 0 else 0))
            p.marble.append(box(b, (cx - wd / 2 + 0.5, top - ln, -3 + i * 0.5), (wd - 1, ln, 7 - i), density=2, tag="finger"))
            if i < 2:  # the joint below
                p.marble.append(box(b, (cx - wd / 2 + 0.5, top - ln - 1, -3.5 + i * 0.5), (wd - 1, 2, 7 - i), inflate=0.2, density=2, tag="joint"))
            if i == 2:
                p.nails.append(box(b, (cx - wd / 2 + 1.5, top - ln, 3.5 - i * 0.5), (wd - 3, 5, 1), density=2, tag="nail"))
            if name == "middle" and i == 0:
                p.gold.append(box(b, (cx - wd / 2, top - 6, -4), (wd, 3, 8), inflate=0.5, density=2, tag="signet"))
            parent, top = b.name, top - ln
    # The thumb: out from the heel of the palm, curling in to meet the quill.
    # The thumb: from the heel of the palm, down and forward, its tip pressing the quill to the index.
    t1 = r.bone("thumb1", (-14, 66, -5), "hand", rotation=(-16, 0, 22))
    p.marble.append(box(t1, (-18, 52, -9), (8, 14, 8), density=2, tag="finger"))
    t2 = r.bone("thumb2", (-14, 52, -5), "thumb1", rotation=(-14, 0, -16))
    p.marble.append(box(t2, (-17.5, 41, -8.5), (7, 11, 7), density=2, tag="finger"))
    p.marble.append(box(t2, (-17, 51, -8), (6, 2, 6), inflate=0.2, density=2, tag="joint"))
    t3 = r.bone("thumb3", (-14, 41, -5), "thumb2", rotation=(-12, 0, -14))
    p.marble.append(box(t3, (-17, 32, -8), (6, 9, 6), density=2, tag="finger"))
    p.nails.append(box(t3, (-16, 32, -2.5), (4, 5, 1), density=2, tag="nail"))

    # --- the quill ----------------------------------------------------------------------------
    quill = r.bone("quill", NIB, "hand", rotation=(-8, 0, -16))
    nx, ny, nz = NIB
    p.nib.append(box(quill, (nx - 1, ny, nz - 1), (2, 9, 2), density=2, tag="nib"))
    p.shaft.append(box(quill, (nx - 1, ny + 9, nz - 1), (2, 92, 2), density=2, tag="shaft"))
    p.vane.append(box(quill, (nx - 11, ny + 40, nz), (22, 62, 0), faces=("north", "south"), density=2, tag="vane"))

    # Enlarge the hand (and all it holds) about the wrist.
    wx, wy, wz = 0.0, PALM_TOP + 2.0, 0.0
    k = HAND_SCALE
    names = {"hand"}
    for b in r.bones:
        if b.parent in names:
            names.add(b.name)
    for b in r.bones:
        if b.name not in names:
            continue
        b.pivot = (wx + (b.pivot[0] - wx) * k, wy + (b.pivot[1] - wy) * k, wz + (b.pivot[2] - wz) * k)
        for c in b.cubes:
            c.origin = (wx + (c.origin[0] - wx) * k, wy + (c.origin[1] - wy) * k, wz + (c.origin[2] - wz) * k)
            c.size = tuple(int(round(v * k)) for v in c.size)
            c.inflate *= k
            if c.pivot:
                c.pivot = (wx + (c.pivot[0] - wx) * k, wy + (c.pivot[1] - wy) * k, wz + (c.pivot[2] - wz) * k)
    nx, ny, nz = wx + (nx - wx) * k, wy + (ny - wy) * k, wz + (nz - wz) * k
    # The nib to the origin: where the entity stands is where it writes.
    r.translate(-nx, -ny, -nz)
    for _, c in r.cubes():
        if c.pivot:  # Rig.translate moves origins and bone pivots, not per-cube rotation pivots
            c.pivot = (c.pivot[0] - nx, c.pivot[1] - ny, c.pivot[2] - nz)
    r.pack()
    return r, p


def _ring(r, p, name, parent, radius, n, seg, plate_every, eyes):
    ring = r.bone(name, (0, PORTAL_Y + 4, 0), parent or "portal")
    for k in range(n):
        deg = 360.0 * k / n
        L, H, D = seg
        c = box(ring, (-L / 2, PORTAL_Y + 4 - H / 2, radius - D / 2), (L, H, D), tag="ring",
                rotation=(0, deg, 0), pivot=(0, PORTAL_Y + 4, 0))
        p.gold.append(c)
        if k % plate_every == 0:
            plate = box(ring, (-L / 2 + 1, PORTAL_Y + 4 - H / 2 - 2, radius + D / 2), (L - 2, H + 4, 1), density=2,
                        tag="eye" if eyes else "rune", rotation=(0, deg, 0), pivot=(0, PORTAL_Y + 4, 0))
            (p.eyes if eyes else p.runes).append(plate)
            p.gold.append(plate)


# --- texture ----------------------------------------------------------------------------------

class Painter:
    def __init__(self, r, p):
        self.r, self.p = r, p
        self.t = Tex(r.tex_w, r.tex_h, 300)
        self.g = Tex(r.tex_w, r.tex_h, 301)

    def put(self, cube, face, x, y, c, glow=None, shading=True):
        u, v, w, h = cube.rects[face]
        if not (0 <= x < w and 0 <= y < h) or c is None:
            return
        self.t.set(u + x, v + y, shade(c, SHADE[face]) if shading and c[3] else c)
        if glow is not None:
            self.g.set(u + x, v + y, glow)

    def each(self, cube, fn):
        for face, (u, v, w, h) in cube.rects.items():
            for y in range(h):
                for x in range(w):
                    fn(face, x, y, w, h, u, v)

    # -- materials --

    def marble(self, cube, seed):
        k = cube.density

        def fn(face, x, y, w, h, u, v):
            X, Y = (u + x) / k, (v + y) / k
            n = fbm(int(X * 2), int(Y * 2), seed, 512, 512, 3, 9.0)
            c = MARBLE[2.6 + (n - 0.5) * 2.2]
            warp = fbm(int(X), int(Y), seed + 1, 512, 512, 2, 14.0)
            grey = math.sin((X * 0.9 + Y * 0.45) * 0.42 + warp * 8)
            gold = math.sin((X * 0.55 - Y * 0.9) * 0.3 + fbm(int(X), int(Y), seed + 2, 512, 512, 2, 18.0) * 11)
            glow = None
            if abs(gold) < 0.045:
                c = GOLD[4] if abs(gold) < 0.02 else GOLD[3]
                glow = mix(LIGHT[0], GOLD[4], 0.3)
            elif abs(grey) < 0.05:
                c = mix(c, VEIN, 0.55)
            # Soft creases across the palm and at the joints.
            if cube.tag in ("palm",) and face == "north" and (abs(y - h * 0.35) < 1 or abs(y - h * 0.62 + x * 0.12) < 1):
                c = shade(c, 0.86)
            if cube.tag in ("finger",) and face in ("north", "south") and y in (1, h - 2):
                c = shade(c, 0.9)
            # Edges darken a little, so the forms read even at full brightness.
            edge = min(x, y, w - 1 - x, h - 1 - y) / k
            if edge < 1:
                c = shade(c, 0.82)
            elif edge < 2:
                c = shade(c, 0.92)
            # A faint light of its own: it is not flesh.
            self.put(cube, face, x, y, c, glow if glow else (int(c[0] * 0.38), int(c[1] * 0.36), int(c[2] * 0.3), 255))
        self.each(cube, fn)

    def nail(self, cube):
        def fn(face, x, y, w, h, u, v):
            c = mix(hexc("#efe3d8"), hexc("#d8c2b4"), y / max(1, h - 1))
            if y == h - 1:
                c = GOLD[4]
            self.put(cube, face, x, y, c, (int(c[0] * 0.5), int(c[1] * 0.45), int(c[2] * 0.4), 255))
        self.each(cube, fn)

    def gold(self, cube, seed, runes):
        rng = random.Random(seed)
        glyph = {}

        def fn(face, x, y, w, h, u, v):
            n = fbm(u + x, v + y, seed, 512, 512, 2, 4.0)
            c = GOLD[3 + (n - 0.5) * 1.8]
            if x == 0 or y == 0:
                c = GOLD[5]
            if x == w - 1 or y == h - 1:
                c = GOLD[1]
            glow = None
            if runes and face in ("north", "south", "east", "west") and 1 <= y < h - 1 and 1 <= x < w - 1:
                # Enochian glyphs: short strokes on a 3-wide grid, lit from within.
                cell = (face, x // 3, y // 4)
                if cell not in glyph:
                    glyph[cell] = rng.getrandbits(12)
                bits = glyph[cell]
                lx, ly = x % 3, y % 4
                if ly < 3 and lx < 2 and (bits >> (ly * 2 + lx)) & 1:
                    c = LIGHT[1]
                    glow = LIGHT[1]
            self.put(cube, face, x, y, c, glow)
        self.each(cube, fn)

    def eye(self, cube):
        """One of the ofanim's eyes: on the outward face, open and lit."""
        self.gold(cube, 99, False)
        for face in ("south",):
            if face not in cube.rects:
                continue
            u, v, w, h = cube.rects[face]
            cx, cy = (w - 1) / 2, (h - 1) / 2
            for y in range(h):
                for x in range(w):
                    dx, dy = (x - cx) / (w / 2 - 0.5), (y - cy) / (h / 2 - 0.5)
                    e = dx * dx + (dy * 1.7) ** 2
                    if e > 1:
                        continue
                    r = math.hypot(x - cx, y - cy)
                    if r < 1.2:
                        c, g = INK, None
                    elif r < 3.2:
                        c, g = (GOLD[5] if r < 2.2 else GOLD[3]), LIGHT[0]
                    else:
                        c, g = hexc("#fbf6ea"), (200, 196, 185, 255)
                    self.put(cube, face, x, y, c, g, shading=False)

    def robe(self, cube, seed):
        def fn(face, x, y, w, h, u, v):
            if face == "up":
                return
            n = fbm(u + x, v + y, seed, 512, 512, 2, 6.0)
            fold = math.sin((x / 9.0) * 2 * math.pi + seed) * 0.8
            b = 2.8 + (n - 0.5) * 0.8 + (fold if face != "down" else 0)
            c = ROBE[b]
            glow = None
            if face == "down":
                # Inside the sleeve: dark, save the rim.
                edge = min(x, y, w - 1 - x, h - 1 - y)
                c = ROBE[2] if edge < 2 else shade(ROBE[1], 0.45)
            elif cube.tag == "robe" and y >= h - 4:
                # A gold band with a meander, near the bottom of each tier.
                c = GOLD[3]
                if y in (h - 4, h - 1):
                    c = GOLD[5]
                elif (x // 2 + (y - (h - 3))) % 4 == 0 or (x % 6 == 0):
                    c = GOLD[1]
                    glow = GOLD[3]
            elif cube.tag == "robe" and face in ("north", "south") and abs(x - w / 2) < 1.5:
                # An embroidered vine down the middle.
                if (y // 2) % 3 != 1:
                    c = GOLD[4]
            elif cube.tag == "fold":
                c = ROBE[3.6]
            self.put(cube, face, x, y, c, glow)
        self.each(cube, fn)

    def hem(self, cube):
        def fn(face, x, y, w, h, u, v):
            c = GOLD[4] if y < 2 else GOLD[2]
            glow = None
            if face != "down" and (x + y) % 5 == 0:
                c, glow = LIGHT[1], LIGHT[0]
            self.put(cube, face, x, y, c, glow)
        self.each(cube, fn)

    def cloud(self, cube, seed):
        def fn(face, x, y, w, h, u, v):
            n = fbm(u + x, v + y, seed, 512, 512, 3, 5.0)
            b = 3.2 + (n - 0.5) * 2.4 - (1.2 if face == "down" else 0)
            c = CLOUD[b]
            if face == "down":
                c = mix(c, GOLD[4], 0.25)
            self.put(cube, face, x, y, c, (int(c[0] * 0.55), int(c[1] * 0.52), int(c[2] * 0.45), 255))
        self.each(cube, fn)

    def disc(self, cube):
        def fn(face, x, y, w, h, u, v):
            cx = cy = (w - 1) / 2
            dx, dy = (x - cx) / (w / 2), (y - cy) / (h / 2)
            rr = math.hypot(dx, dy)
            if rr > 1:
                return
            ang = math.atan2(dy, dx)
            swirl = 0.5 + 0.5 * math.sin(ang * 5 + rr * 14)
            core = max(0.0, 1 - rr * 1.6)
            c = mix(mix(GOLD[3], LIGHT[1], swirl), LIGHT[2], core)
            a = int(255 * min(1.0, (1 - rr) * 2.4))
            c = (c[0], c[1], c[2], max(0, a))
            self.put(cube, face, x, y, c, c, shading=False)
        self.each(cube, fn)

    def ray(self, cube, k):
        def fn(face, x, y, w, h, u, v):
            across = 1 - abs(x - (w - 1) / 2) / (w / 2)
            down = 1 - y / h
            a = int(120 * across * down ** 1.4)
            if a < 8:
                return
            c = mix(LIGHT[0], LIGHT[2], across)
            self.put(cube, face, x, y, (c[0], c[1], c[2], a), (c[0], c[1], c[2], a), shading=False)
        self.each(cube, fn)

    def shaft(self, cube):
        def fn(face, x, y, w, h, u, v):
            c = hexc("#f2ead8") if (x + face.__len__()) % 2 else hexc("#ddd2bb")
            self.put(cube, face, x, y, c)
        self.each(cube, fn)

    def nib(self, cube):
        def fn(face, x, y, w, h, u, v):
            c = INK if y >= h - 3 else (GOLD[5] if x == 0 else GOLD[3])
            self.put(cube, face, x, y, c, None if y >= h - 3 else GOLD[4])
        self.each(cube, fn)

    def vane(self, cube):
        """A flight feather: tapered, barbed, gold at the edges; the shaft runs down its middle."""
        def fn(face, x, y, w, h, u, v):
            t = y / (h - 1)  # 0 at the top
            half = (w / 2) * (math.sin(math.pi * min(1.0, t * 1.05)) ** 0.55) * (0.92 if t > 0.5 else 1)
            dx = x - (w - 1) / 2
            if abs(dx) > half or t > 0.97:
                return
            edge = half - abs(dx)
            barb = ((y + int(abs(dx) * 0.8)) % 3 == 0)
            c = hexc("#fbf8f1") if not barb else hexc("#e2dccd")
            glow = None
            if edge < 1.5:
                c, glow = GOLD[4], GOLD[3]
            if abs(dx) < 0.6:
                c = hexc("#efe6d2")
            # A notch or two in the vane, as real feathers have.
            if 0.35 < t < 0.4 and dx > half * 0.4:
                return
            self.put(cube, face, x, y, c, glow, shading=False)
        self.each(cube, fn)

    def paint(self):
        p = self.p
        for i, c in enumerate(p.marble):
            self.marble(c, 400 + i)
        for c in p.nails:
            self.nail(c)
        for i, c in enumerate(p.gold):
            if c in p.eyes:
                continue
            self.gold(c, 500 + i, c in p.runes)
        for c in p.eyes:
            self.eye(c)
        for i, c in enumerate(p.robe):
            self.robe(c, 600 + i)
        for c in p.hem:
            self.hem(c)
        for i, c in enumerate(p.clouds):
            self.cloud(c, 700 + i)
        for c in p.discs:
            self.disc(c)
        for i, c in enumerate(p.rays):
            self.ray(c, i)
        for c in p.shaft:
            self.shaft(c)
        for c in p.nib:
            self.nib(c)
        for c in p.vane:
            self.vane(c)
        return self.t, self.g


# --- animations -------------------------------------------------------------------------------

E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"
H = "animation.scribe_hand."
FINGER_BONES = [f"{n}{i}" for n, *_ in FINGERS for i in (1, 2, 3)]


def _spin(a, bone, axis, turns, length):
    keys = []
    for k in range(9):
        t = length * k / 8
        ang = 360 * turns * k / 8
        keys.append((t, [ang if axis == 0 else 0, ang if axis == 1 else 0, ang if axis == 2 else 0]))
    a.rot(bone, *keys)


def anims():
    f = AnimFile()
    idle = f.new(H + "idle", 8.0, loop=True)
    _spin(idle, "ring_a", 1, 1, 8.0)
    _spin(idle, "ring_b", 1, -1, 8.0)
    _spin(idle, "ring_c", 1, 2, 8.0)
    _spin(idle, "disc", 1, -1, 8.0)
    idle.pos("root", (0, [0, 0, 0]), (2.0, [0, 2, 0], E_IO), (4.0, [0, 0, 0], E_IO), (6.0, [0, 2, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    idle.rot("sleeve", (0, [0, 0, 0]), (2.0, [2, 0, 2], E_IO), (4.0, [0, 0, 0], E_IO), (6.0, [-2, 0, -2], E_IO), (8.0, [0, 0, 0], E_IO))
    idle.rot("hand", (0, [0, 0, 0]), (4.0, [-4, 3, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    for i, bone in enumerate(FINGER_BONES):
        lag = (i % 3) * 0.2
        idle.rot(bone, (0, [0, 0, 0]), (2.0 + lag, [-4, 0, 0], E_IO), (4.0 + lag, [0, 0, 0], E_IO), (8.0, [0, 0, 0], E_IO))

    appear = f.new(H + "appear", 2.4)
    for ring in ("ring_a", "ring_b", "ring_c", "disc", "clouds", "rays"):
        appear.scale(ring, (0, [0.02, 0.02, 0.02]), (0.6, [1.15, 1.15, 1.15], E_BACK), (0.9, [1, 1, 1], E_IO), (2.4, [1, 1, 1]))
    appear.pos("sleeve", (0, [0, 90, 0]), (0.6, [0, 90, 0]), (2.0, [0, -3, 0], E_OUT), (2.4, [0, 0, 0], E_IO))
    appear.scale("sleeve", (0, [0.4, 0.05, 0.4]), (0.6, [0.4, 0.05, 0.4]), (1.6, [1, 1, 1], E_OUT), (2.4, [1, 1, 1]))

    write = f.new(H + "write", 0.5)
    write.rot("hand", (0, [0, 0, 0]), (0.15, [-6, 5, -4], E_OUT), (0.3, [3, -4, 4], E_IO), (0.5, [0, 0, 0], E_IO))
    write.rot("quill", (0, [0, 0, 0]), (0.15, [6, 0, -6], E_OUT), (0.3, [-4, 0, 6], E_IO), (0.5, [0, 0, 0], E_IO))
    write.rot("index3", (0, [0, 0, 0]), (0.25, [-10, 0, 0], E_IO), (0.5, [0, 0, 0], E_IO))

    slam = f.new(H + "slam", 1.5)
    slam.rot("hand", (0, [0, 0, 0]), (0.15, [-82, 0, 0], E_SNAP), (0.95, [-82, 0, 0]), (1.5, [0, 0, 0], E_IO))
    for name, _, _, _, splay in FINGERS:
        for i, curl in enumerate(CURLS[name]):
            slam.rot(f"{name}{i + 1}", (0, [0, 0, 0]), (0.15, [-curl, 0, splay * 0.8 if i == 0 else 0], E_SNAP),
                     (0.95, [-curl, 0, splay * 0.8 if i == 0 else 0]), (1.5, [0, 0, 0], E_IO))
    slam.rot("quill", (0, [0, 0, 0]), (0.15, [30, 0, 50], E_SNAP), (0.95, [30, 0, 50]), (1.5, [0, 0, 0], E_IO))
    slam.rot("sleeve", (0, [0, 0, 0]), (0.15, [6, 0, 0], E_SNAP), (0.95, [6, 0, 0]), (1.5, [0, 0, 0], E_IO))

    sweep = f.new(H + "sweep", 0.9)
    sweep.rot("hand", (0, [0, 0, 0]), (0.12, [-25, 0, 55], E_SNAP), (0.7, [-25, 0, 55]), (0.9, [0, 0, 0], E_IO))
    for name, *_ in FINGERS:
        sweep.rot(f"{name}1", (0, [0, 0, 0]), (0.12, [10, 0, 0], E_SNAP), (0.7, [10, 0, 0]), (0.9, [0, 0, 0], E_IO))
    sweep.rot("quill", (0, [0, 0, 0]), (0.12, [20, 0, 35], E_SNAP), (0.7, [20, 0, 35]), (0.9, [0, 0, 0], E_IO))
    return f


def generate():
    r, p = rig()
    r.write(GEO + "scribe_hand.geo.json")
    t, g = Painter(r, p).paint()
    save(t, "entity", "scribe_hand")
    save(g, "entity", "scribe_hand_glowmask")
    anims().write(ANIM + "scribe_hand.animation.json")


if __name__ == "__main__":
    generate()
