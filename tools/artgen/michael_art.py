"""The Archangel Michael (v0.12), phases I-IV: his vessel, Adam Milligan as at Stull Cemetery.

A young man (early twenties) in a light-grey suit, white shirt and dark tie, short dark hair, eyes of pale blue
light (glowmask). The angel blade in his right hand (`blade`), the Lance of Michael too (`lance`, leaning out from
the hand with its butt by his right foot; the renderer shows it from phase III and hides it while it is thrown),
a glowing palm on the left hand (`palm_light`, the forehead touch).

In phases III-IV he shows SHADOW WINGS as in the show: two pairs of wings of smoke and light, semi-transparent.
They live in the same atlas with partial alpha (the renderer draws the whole model translucent; the body's
pixels stay opaque). Each wing carries its leading edge (an arm of smoke along its top) and each feather is its own
bone (`<wing>_f00..f17`, direct children of the wing) so a clip can spread, fold and ruffle them:
  f00..f11  flight feathers, inner (near the root) to outer (the tip), fanning from straight down to outward;
  f12..f17  coverts along the leading edge, shorter and broader, a layer behind (dorsal, +Z).

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head
               -- right_arm -- right_forearm -- right_hand -- blade, lance
               -- left_arm  -- left_forearm  -- left_hand  -- palm_light
               -- shadow_wings -- wing_r1, wing_l1 (upper pair), wing_r2, wing_l2 (lower pair) -- feathers
       -- right_leg -- right_shin,  left_leg -- left_shin

Also home to the painting library every Michael module shares: `Plate` (white-and-gold armour with a bevel, gold
filigree, Enochian engraving, rivets and, for phase VI, cracks of light along the seams), `Gilt`, `MetalFeather`,
`SmokeFeather`, the Lance of Michael (`lance_parts`), feather fans and preview helpers.
"""

import math
import random

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
E_IO, E_OUT, E_IN, E_BACK = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack"
A = "animation.michael."

# --- palette ------------------------------------------------------------------------------------------------
SKIN = Ramp("#6e4a3a", "#9a6c57", "#bb8a70", "#d1a185", "#e0b699", "#edcab0")
HAIR = Ramp("#120c08", "#1d140e", "#2a1d14", "#38281b", "#473323", "#57402d")
SUIT = Ramp("#5a6068", "#737a83", "#8c939b", "#a5abb2", "#bcc1c7", "#d3d7db")
SHIRT = Ramp("#9aa1aa", "#b5bbc3", "#cbd0d6", "#dde1e6", "#ebeef1", "#f8f9fb")
TIE = Ramp("#07080c", "#0e1118", "#161b25", "#202733", "#2c3444", "#3c4659")
SHOE = Ramp("#050404", "#0b0909", "#131011", "#1c1819", "#2a2526", "#4a4446")
EYE = hexc("#bfe6ff")          # the pale blue light of his eyes
EYE_CORE = hexc("#f2fbff")
EYE_WHITE = hexc("#e3eaef")
# Metals and light shared by every Michael model.
SILVER = Ramp("#4a5260", "#727b88", "#9aa3ae", "#c1c8d0", "#e0e5ea", "#fbfdff")
WHITE_METAL = Ramp("#7d8698", "#a3abba", "#c4cad5", "#dde1e8", "#eef0f4", "#ffffff")
STEEL = Ramp("#27303e", "#3f4c62", "#62728b", "#8f9fb6", "#c2cddb", "#f1f5fa")
GOLD = Ramp("#5a3a0c", "#8a5c14", "#b8851f", "#ddaf38", "#f3d36a", "#fff1b8")
LIGHT = Ramp("#3d6fc4", "#5d8fe0", "#86b2f2", "#b3d3fb", "#d9ebff", "#f5fbff")
SMOKE = Ramp("#07080d", "#0f1119", "#181b27", "#232838", "#30374b", "#414a62")
WHITE = hexc("#ffffff")


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


# =====================================================================================================
# Painting library
# =====================================================================================================

# Enochian-looking glyphs, 3x5 texels each (rows top to bottom).
GLYPHS = [
    ("111", "100", "110", "100", "111"), ("010", "111", "010", "010", "011"), ("110", "101", "110", "100", "100"),
    ("111", "001", "011", "001", "111"), ("101", "101", "111", "001", "001"), ("011", "100", "110", "100", "011"),
    ("111", "010", "010", "110", "100"), ("100", "110", "101", "110", "100"), ("011", "001", "111", "101", "111"),
    ("110", "010", "011", "010", "110"), ("101", "010", "101", "100", "110"), ("111", "101", "100", "101", "111"),
    ("001", "011", "101", "001", "111"), ("100", "111", "101", "111", "001"),
]


def glyph_bit(seed, gx, gy, cx, cy):
    """Bit (cx, cy) of the glyph in cell (gx, gy) of a script field seeded by `seed`."""
    g = GLYPHS[int(h01("glyph", seed, gx, gy) * len(GLYPHS))]
    return g[cy][cx] == "1"


_CRACKS = {}


def crack_texels(seed, w, h, band):
    """Cracks of light that start on the seam (the gold band round a face) and run inward, branching."""
    key = (seed, w, h, band)
    if key in _CRACKS:
        return _CRACKS[key]
    rng = random.Random(seed)
    pts = set()
    n = 0 if w * h < 50 else min(3, 1 + (w * h) // 500)
    for _ in range(n):
        side = rng.randrange(4)
        if side == 0:
            x, y, dx, dy = rng.randrange(w), band, 0, 1
        elif side == 1:
            x, y, dx, dy = rng.randrange(w), h - 1 - band, 0, -1
        elif side == 2:
            x, y, dx, dy = band, rng.randrange(h), 1, 0
        else:
            x, y, dx, dy = w - 1 - band, rng.randrange(h), -1, 0
        # First a run along the seam, then inward.
        for _ in range(rng.randint(2, 5)):
            pts.add((x, y))
            x, y = x + dy, y + dx
        stack = [(x, y, dx, dy, rng.randint(max(3, min(w, h) // 3), max(4, (w + h) // 3)))]
        while stack:
            x, y, dx, dy, L = stack.pop()
            for _ in range(L):
                if not (0 <= x < w and 0 <= y < h):
                    break
                pts.add((x, y))
                if rng.random() < 0.35:
                    x += rng.choice((-1, 1)) * (1 if dx == 0 else 0)
                    y += rng.choice((-1, 1)) * (1 if dy == 0 else 0)
                x, y = x + dx, y + dy
                if rng.random() < 0.08 and len(stack) < 3:
                    stack.append((x, y, dy or rng.choice((-1, 1)), dx or rng.choice((-1, 1)), L // 2))
    _CRACKS[key] = pts
    return pts


class Plate:
    """White (or steel) armour plate at `d` texels per px: a smooth top-lit gradient with a specular band, gold along
    the top and bottom edges with a bevel under them, and one motif per part:
      plain    -- nothing more;
      engrave  -- a line of Enochian script cut into the metal (glowing pale blue in the glowmask);
      ridge    -- a raised ridge down the middle with a column of script beside it (vambraces, greaves);
      rosette  -- a gold rosette with a gem of light at the centre of the front face (knee cops, couters);
      frame    -- an inset band of gold filigree, rivets and a row of gold script (used on two or three parts);
    `scallop=True` cuts the lower edge into gold-rimmed scallops (pauldrons). `Plate.CRACKED` paints phase VI: cracks
    of white-gold light from the seams. Use `.mat` as the material and `.glow` as the glow."""

    CRACKED = False

    def __init__(self, seed, d=2, ramp=WHITE_METAL, trim=GOLD, engrave=True, rivets=True, base=3.1, band=True,
                 glow=0.14, eng_glow=None, rows=1, motif="plain", scallop=False):
        self.seed, self.d, self.ramp, self.trim = seed, d, ramp, trim
        self.engrave, self.rivets, self.base, self.band, self.g, self.rows = engrave, rivets, base, band, glow, rows
        self.eng_glow = eng_glow if eng_glow is not None else LIGHT[2]
        self.motif, self.scallop = motif, scallop

    def kind(self, face, x, y, w, h):
        if Plate.CRACKED:
            cr = crack_texels(self.seed * 7 + len(face) * 31 + w * 3 + h, w, h, self.d)
            if (x, y) in cr:
                return "crack"
            if (x - 1, y) in cr or (x + 1, y) in cr or (x, y - 1) in cr or (x, y + 1) in cr:
                return "crack_edge"
        side = face in ("north", "south", "east", "west")
        if self.scallop and side and h >= 6:
            sc = max(2, self.d + 1)
            yy = h - 1 - y
            if yy < sc:
                per = max(4, 3 * self.d)
                xm = (x % per) - (per - 1) / 2
                lim = sc * (xm / (per / 2)) ** 2
                if yy < lim - 0.5:
                    return None
                if yy < lim + 0.9:
                    return "gold"
        e = min(x, y, w - 1 - x, h - 1 - y)
        if not side:
            return "gold" if e == 0 and w > 3 and h > 3 and self.band else "metal"
        if w < 4 or h < 4:
            return "gold_hi" if y == 0 and self.band else "metal"
        if e == 0:
            if y == 0:
                return "gold_hi" if self.band else "hi"
            if y == h - 1:
                return "gold" if self.band else "lo"
            return "edge"
        if y == 1:
            return "hi"
        if y == h - 2:
            return "lo"
        mo = self.motif
        front = face in ("north", "south")
        if mo == "frame":
            bw = max(1, self.d)
            if w >= 12 and h >= 10 and e == bw:
                return "gold"
            inner = bw
            if self.rivets and w >= 16 and h >= 14:
                for rx, ry in ((inner + 2, inner + 2), (w - inner - 3, inner + 2), (inner + 2, h - inner - 3),
                               (w - inner - 3, h - inner - 3)):
                    if (x, y) == (rx, ry):
                        return "rivet"
                    if (x, y) == (rx + 1, ry + 1) or (x, y) == (rx, ry + 1):
                        return "rivet_sh"
            if w >= 2 * inner + 14 and h >= 2 * inner + 9:
                cy, cx = y - (inner + 2), x - (inner + 6)
                if 0 <= cy < 5 and 0 <= cx < w - 2 * inner - 12 and cx % 4 < 3:
                    if glyph_bit(self.seed, cx // 4, face == "north", cx % 4, cy):
                        return "glyph"
        elif mo == "engrave" and front and w >= 10 and h >= 10:
            cy, cx = y - 3, x - 3
            if 0 <= cy < 5 and 0 <= cx < w - 6 and cx % 4 < 3 and glyph_bit(self.seed, cx // 4, face == "north", cx % 4, cy):
                return "glyph_faint"
        elif mo == "ridge" and front and w >= 6:
            c = (w - 1) // 2
            if x == c:
                return "ridge_hi"
            if x == c + 1:
                return "ridge_lo"
            cx, cy = x - (c + 3), (y - 3) % 6
            if 0 <= cx < 3 and cy < 5 and 3 <= y < h - 4 and glyph_bit(self.seed, (y - 3) // 6, face == "north", cx, cy):
                return "glyph_faint"
        elif mo == "rosette" and face == "north" and w >= 8 and h >= 8:
            cxr, cyr = (w - 1) / 2, (h - 1) / 2
            r = math.hypot(x - cxr, y - cyr)
            R = min(w, h) / 2 - 1.5
            if r < R * 0.3:
                return "gem"
            if abs(r - R) < 0.6:
                return "gold"
            ang = math.degrees(math.atan2(y - cyr, x - cxr)) % 45
            if r < R - 0.6 and abs(ang - 22.5) < 9 * (1 - r / R) + 3:
                return "gold_hi" if r < R * 0.65 else "gold"
        return "metal"

    def value(self, face, x, y, w, h):
        t = y / max(1, h - 1)
        v = self.base + 1.7 * (1 - t) - 0.95
        if abs(t - 0.24) < 0.06:
            v += 0.9  # the specular band
        elif abs(t - 0.24) < 0.1:
            v += 0.45
        if face == "up":
            v = self.base + 1.4
        elif face == "down":
            v = self.base - 1.4
        elif face in ("east", "west"):
            v -= 0.3
        elif face == "south":
            v -= 0.15
        return v

    def mat(self, face, x, y, w, h):
        k = self.kind(face, x, y, w, h)
        if k is None:
            return None
        if k == "metal":
            return tone(self.ramp, self.value(face, x, y, w, h))
        if k == "hi":
            return self.ramp[5]
        if k == "lo":
            return self.ramp[1]
        if k == "edge":
            return self.ramp[2]
        if k == "gold":
            return tone(self.trim, 3.6 - 1.2 * y / max(1, h))
        if k == "gold_hi":
            return self.trim[5]
        if k == "rivet":
            return self.trim[5]
        if k == "rivet_sh":
            return self.trim[1]
        if k == "glyph":
            return self.trim[2]
        if k == "glyph_faint":
            return self.ramp[1]
        if k == "ridge_hi":
            return self.ramp[5]
        if k == "ridge_lo":
            return self.ramp[1]
        if k == "gem":
            return LIGHT[4]
        if k == "crack":
            return hexc("#fffbea")
        if k == "crack_edge":
            return self.trim[5]
        return self.ramp[3]

    def glow(self, face, x, y, w, h, c):
        k = self.kind(face, x, y, w, h)
        if k in ("glyph", "glyph_faint"):
            return self.eng_glow
        if k == "gem":
            return LIGHT[5]
        if k in ("crack",):
            return hexc("#fffbea")
        if k == "crack_edge":
            return self.trim[4]
        if k in ("gold", "gold_hi", "rivet"):
            return shade(c, 0.28)
        return shade(c, self.g)


class Gilt(Plate):
    """Solid gold work (belts, rims, collars): the same bevel and gradient in gold, no band."""

    def __init__(self, seed, d=2, engrave=False, base=3.0):
        super().__init__(seed, d, ramp=GOLD, trim=GOLD, engrave=engrave, rivets=False, base=base, band=False, glow=0.26,
                         eng_glow=LIGHT[3], motif="engrave" if engrave else "plain")

    def mat(self, face, x, y, w, h):
        k = self.kind(face, x, y, w, h)
        if k in ("glyph", "glyph_faint"):
            return GOLD[1]
        return super().mat(face, x, y, w, h)


class Under:
    """The body of pale light under the plates (glows)."""

    def __init__(self, seed):
        self.seed = seed

    def mat(self, face, x, y, w, h):
        t = y / max(1, h - 1)
        v = 3.9 - 0.9 * t + (0.4 if face == "up" else -0.5 if face == "down" else 0)
        if face in ("north", "south", "east", "west") and x % 6 == 0:
            v -= 0.6  # quilted seams of the gambeson of light
        return tone(LIGHT, v)

    def glow(self, face, x, y, w, h, c):
        return shade(c, 0.85)


class MetalFeather:
    """A metal feather on a hanging cube (y = 0 at the root, y = h-1 at the tip). Kinds: primary (long, pointed,
    gold leading edge), secondary, covert (short, rounded, gold rim), crest (white tipped gold). `s` is the wing's
    side (-1 right, +1 left) so the gold edge lands on the outer vane; the front (north) face is the underside."""

    def __init__(self, kind, s, seed, ramp=STEEL, up=None):
        self.kind, self.s, self.seed, self.ramp = kind, s, seed, ramp
        # Feathers built upward (crests, plumes, the lance's guard) have their tip at the top of the face.
        self.up = kind == "crest" if up is None else up

    def inside(self, x, y, w, h):
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        half = (w - 1) / 2 + 0.35
        if self.kind in ("covert",):
            if t > 0.55:
                q = (t - 0.55) / 0.45
                half *= math.sqrt(max(0.0, 1 - q * q))
        else:
            ts = 0.8 if self.kind == "primary" else 0.84
            if t > ts:
                half *= max(0.25, 1 - (t - ts) / (1 - ts) * 0.8)
        return abs(x - c) <= half

    def edge_x(self, face, w):
        if face == "north":
            return 0 if self.s < 0 else w - 1
        return w - 1 if self.s < 0 else 0

    def classify(self, face, x, y, w, h):
        if face not in ("north", "south"):
            return "side"
        if self.up:
            y = h - 1 - y
        if not self.inside(x, y, w, h):
            return None
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        if self.kind == "crest" and t > 0.62:
            return "gold"
        if self.kind == "covert" and t > 0.5 and not self.inside(x, y + 2, w, h):
            return "gold"  # the covert's rim
        if self.kind == "primary" and x == self.edge_x(face, w) and t < 0.85:
            return "gold"
        if abs(x - c) < 0.6 and t < 0.9:
            return "rachis"
        return "vane"

    def mat(self, face, x, y, w, h):
        k = self.classify(face, x, y, w, h)
        if k is None:
            return None
        if self.up:
            y = h - 1 - y
        t = y / max(1, h - 1)
        under = face == "north" and not self.up
        if k == "side":
            return self.ramp[1]
        if k == "gold":
            return tone(GOLD, 4.2 - 1.6 * t - (0.8 if under else 0))
        if self.kind in ("covert", "crest"):
            v = 4.5 - 1.0 * t
        elif self.kind == "secondary":
            v = 4.5 - 1.2 * t
        else:
            v = 4.8 - 1.7 * t
        if k == "rachis":
            v = max(v + 0.8, 4.4 - 1.0 * t)
        else:
            c = (w - 1) / 2
            if (int(y * 1.0) + int(abs(x - c))) % 4 == 0:
                v -= 0.45  # barbs
            d = t - 0.32 + (x / max(1, w)) * 0.08
            if abs(d) < 0.035 and self.kind != "covert":
                v += 1.3  # the specular band
            if abs(x - (w - 1) * 0.3) < 0.6 and t < 0.7:
                v += 0.5
        if under:
            v -= 0.7
        return tone(self.ramp, v)

    def glow(self, face, x, y, w, h, c):
        k = self.classify(face, x, y, w, h)
        if k == "gold":
            return shade(c, 0.45)
        return shade(c, 0.3)  # polished metal holds the light even in shadow


class SmokeFeather:
    """A shadow feather of smoke and light on a flat plane (y = 0 at the root): a dark smoky core streaked along its
    length, soft alpha falling off towards the tip, a luminous pale-blue rim, wisps dissolving at the point.
    `broad` gives the rounder covert shape."""

    def __init__(self, seed, alpha=215, broad=False):
        self.seed, self.alpha, self.broad = seed, alpha, broad

    def half(self, t, w):
        hw = (w - 1) / 2 + 0.3
        if self.broad:
            return hw * (math.sqrt(max(0.0, 1 - ((t - 0.5) / 0.5) ** 2)) if t > 0.5 else min(1.0, 0.6 + t))
        if t < 0.1:
            return hw * (0.55 + 4.5 * t)
        return hw * (1.0 if t < 0.55 else max(0.08, 1 - (t - 0.55) / 0.45))

    def mat(self, face, x, y, w, h):
        t = y / max(1, h - 1)
        c = (w - 1) / 2
        hw = self.half(t, w)
        dx = abs(x - c)
        if dx > hw:
            return None
        if t > 0.72 and h01("wisp", self.seed, face, x, y) < (t - 0.72) * 1.8:
            return None
        n = fbm(x * 6 + len(face), y * 1.5, self.seed, 64, 64, 3, 5.0)
        q = dx / max(0.5, hw)  # 0 on the shaft .. 1 at the vane's edge
        # A dense, dark core along the shaft; vanes of thin smoke you can see through; softer still at the edge.
        a = self.alpha * (0.95 - 0.6 * q) * (1 - 0.5 * t) * (0.7 + 0.6 * n)
        col = mix(SMOKE[1 + int(n * 2.5)], LIGHT[0], 0.08 + 0.22 * n * n)
        if q < 0.18 and t < 0.8:
            col, a = SMOKE[1], a + 30
        tip = max(0.0, (t - 0.66) / 0.34)
        if tip > 0:
            # Light gathers at the feather's tip.
            col = mix(col, LIGHT[3], min(1.0, tip * 1.1))
            a = a * (1 - 0.25 * tip) + 45 * tip
        return (col[0], col[1], col[2], max(16, min(225, int(a))))

    def glow(self, face, x, y, w, h, col):
        # Only the light in the smoke glows: the rims and the shafts.
        if col[2] > 150:
            return (col[0], col[1], col[2], 255)
        return None


def smoke_mat(seed, alpha=215, broad=False):
    return SmokeFeather(seed, alpha, broad).mat


def smoke_glow(face, x, y, w, h, col):
    return SmokeFeather.glow(None, face, x, y, w, h, col)


def fabric(ramp, seed, base=3.0, folds=0.35, crease=False, hem=True):
    """Smooth suit cloth: top-lit, soft diagonal folds, a pressed crease, a darker hem. No noise."""
    def m(face, x, y, w, h):
        if face == "up":
            return tone(ramp, base + 0.6)
        if face == "down":
            return tone(ramp, base - 1.2)
        t = y / max(1, h - 1)
        v = base + 0.45 - 0.8 * t
        v += folds * math.sin(x * 0.9 + y * 0.35 + seed)
        if face in ("east", "west"):
            v -= 0.3
        elif face == "south":
            v -= 0.15
        if crease and face in ("north", "south") and abs(x - (w - 1) / 2) < 0.6:
            v += 0.9
        if hem and y == h - 1:
            v -= 0.8
        return tone(ramp, v)
    return m


# =====================================================================================================
# Shared rigging helpers
# =====================================================================================================

def pname(b):
    return b if b is None or isinstance(b, str) else b.name


def add_feather(m, parent, name, root, length, width, angle, s, mat, glow=None, dz=0.0, thick=0.5, density=2,
                tag="feather", faces=None, cup=0.0, twist=0.0):
    """One feather as its own bone: pivot at its root on the wing, hanging down (-Y) and fanned by a rest rotation
    about Z. `angle` is degrees from straight down towards the wing's tip; `s` is -1 for a right wing, +1 left.
    `cup` tips it back out of the wing's plane (+X), `twist` turns the vane about its own shaft so its outer edge goes
    back: neighbours then overlap like shingles and the wing keeps its volume seen edge-on."""
    x, y, z = root
    b = m.bone(name, (x, y, z), pname(parent), rotation=(cup, -s * twist, -s * angle))
    m.cube(b, (x - width / 2, y - length, z + dz), (width, length, thick), mat, glow, density=density, tag=tag,
           faces=faces)
    return b


def covert_cube(m, bone, root, length, width, angle, s, mat, glow=None, dz=0.0, thick=0.5, density=2, tag="covert",
                faces=None, tilt=0.0):
    """A feather as a cube in a row bone, fanned by its own rotation about its root (`tilt` lifts it out of the
    wing's plane)."""
    x, y, z = root
    return m.cube(bone, (x - width / 2, y - length, z + dz), (width, length, thick), mat, glow, density=density, tag=tag,
                  rotation=(tilt, 0, -s * angle), pivot=(x, y, z), faces=faces)


def edge_cube(m, bone, a, b, height, s, mat, glow, thick=0.0, density=2, faces=("north", "south"), tag="edge", dz=0.0):
    """A strip along a wing's leading edge from point a to b (b further out), turned in the frontal plane."""
    L = math.hypot(b[0] - a[0], b[1] - a[1])
    ang = math.degrees(math.atan2(b[1] - a[1], abs(b[0] - a[0])))
    x0 = a[0] - L if s < 0 else a[0]
    return m.cube(bone, (x0, a[1] - height / 2, a[2] + dz), (L, height, thick), mat, glow, density=density, faces=faces,
                  rotation=(0, 0, -s * ang), pivot=a, tag=tag)


def mirror_rot(r, s):
    """A right-wing rotation (s=-1) for side s: the left mirrors Y and Z."""
    return (r[0], r[1] * -s, r[2] * -s)


def delta_pose(m, targets):
    """Animation deltas that bring bones to absolute rotations `targets` ({bone: (x, y, z)}) from their rest."""
    out = {}
    for name, tgt in targets.items():
        rest = m.rig.get(name).rotation or (0, 0, 0)
        out[name] = {"rotation": [tgt[i] - rest[i] for i in range(3)]}
    return out


def render_views(m, tex, path, pose=None, hidden=(), scale=4, views=("front", "three_quarter", "side", "back")):
    import preview3d
    preview3d.render(m.rig, tex, path, pose=pose or {}, hidden=hidden, scale=scale, views=views)


def counts(m):
    return len(m.rig.bones), sum(len(b.cubes) for b in m.rig.bones)


# =====================================================================================================
# Animation framework (shared by michael, michael_archangel and host_angel)
# =====================================================================================================

def _ease(name, x):
    if name == "easeInOutSine":
        return -(math.cos(math.pi * x) - 1) / 2
    if name == "easeOutQuad":
        return 1 - (1 - x) ** 2
    if name == "easeInQuad":
        return x * x
    if name == "easeInQuart":
        return x ** 4
    if name == "easeOutBack":
        c1 = 1.70158
        return 1 + (c1 + 1) * (x - 1) ** 3 + c1 * (x - 1) ** 2
    return x


E_SNAP = "easeInQuart"


class Clip:
    """Pose keyframes plus additive layers, written through animkit.

    key(t, pose, ease): a pose maps bone -> [rx, ry, rz] (degrees, relative to rest), "@bone" -> position (px) and
    "%bone" -> scale. Every bone named in any key is keyed at every key time (absent = rest), so poses blend
    predictably. layer(name, fn): fn(t) -> [x, y, z] added on top (ruffles, tremors, wing-beat lag); a layered bone is
    sampled densely. `forbid` lists bones no clip may key (the renderer drives them)."""

    def __init__(self, f, name, length, loop=False, hold=False, forbid=()):
        self.anim = f.new(name, length, loop=loop, hold=hold)
        self.length, self.loop = length, loop
        self.frames, self.layers, self.forbid = [], {}, set(forbid)
        self.mlayers = {}

    def key(self, t, pose, ease=None):
        self.frames.append((min(t, self.length), dict(pose), ease))
        return self

    def layer(self, name, fn):
        self.layers.setdefault(name, []).append(fn)
        return self

    def mlayer(self, name, fn, expr):
        """An additive layer written as Molang (one key per pose key instead of dense samples): `expr` gives a string
        (or None) per component, `fn` the same in Python for previews."""
        self.mlayers.setdefault(name, []).append((fn, expr))
        return self

    def _default(self, name):
        return [1, 1, 1] if name.startswith("%") else [0, 0, 0]

    def value(self, name, t):
        """The keyed (eased) value of channel `name` at time t."""
        fr = sorted(self.frames, key=lambda f: f[0])
        d = self._default(name)
        if not fr:
            return list(d)
        if t <= fr[0][0]:
            return list(fr[0][1].get(name, d))
        for (t0, p0, _), (t1, p1, e1) in zip(fr, fr[1:]):
            if t0 <= t <= t1:
                a, b = p0.get(name, d), p1.get(name, d)
                x = 0 if t1 == t0 else _ease(e1, (t - t0) / (t1 - t0))
                return [a[i] + (b[i] - a[i]) * x for i in range(3)]
        return list(fr[-1][1].get(name, d))

    def pose_at(self, t):
        """The whole pose at t in preview3d's format (layers included)."""
        names = set(k for _, p, _ in self.frames for k in p) | set(self.layers) | set(self.mlayers)
        out = {}
        for k in names:
            v = self.value(k, t)
            for fn in self.layers.get(k, ()):
                v = [v[i] + fn(t)[i] for i in range(3)]
            for fn, _ in self.mlayers.get(k, ()):
                v = [v[i] + fn(t)[i] for i in range(3)]
            bone, ch = (k[1:], "position") if k[0] == "@" else (k[1:], "scale") if k[0] == "%" else (k, "rotation")
            out.setdefault(bone, {})[ch] = v
        return out

    def done(self, step=0.2, tol=0.25):
        names = []
        for _, p, _ in sorted(self.frames, key=lambda f: f[0]):
            for k in p:
                if k not in names:
                    names.append(k)
        for k in list(self.layers) + list(self.mlayers):
            if k not in names:
                names.append(k)
        bad = {k.lstrip("@%") for k in names} & self.forbid
        if bad:
            raise ValueError(f"{self.anim.name} keys procedural bones {sorted(bad)}")
        fr = sorted(self.frames, key=lambda f: f[0])
        n = max(2, int(round(self.length / step)))
        dense = sorted(set([round(self.length * i / n, 4) for i in range(n + 1)] + [round(f[0], 4) for f in fr]))
        for k in names:
            ch = self.anim.pos if k[0] == "@" else self.anim.scale if k[0] == "%" else self.anim.rot
            bone = k.lstrip("@%")
            if k in self.layers:
                keys = []
                for t in dense:
                    v = self.value(k, t)
                    for fn in self.layers[k]:
                        add = fn(t)
                        v = [v[i] + add[i] for i in range(3)]
                    keys.append((t, [round(c, 2) for c in v]))
                keys = _simplify(keys, tol * (0.02 if k[0] == "%" else 1))
            elif k in self.mlayers:
                d = self._default(k)
                src = [(t, p.get(k, d), e) for t, p, e in fr if k in p] or [(0.0, d, None), (self.length, d, None)]
                ded = [src[0]]
                for i in range(1, len(src) - 1):
                    if not (list(src[i][1]) == list(src[i - 1][1]) == list(src[i + 1][1])):
                        ded.append(src[i])
                if len(src) > 1:
                    ded.append(src[-1])
                src = ded
                if len(src) == 1:
                    src.append((self.length, src[0][1], None))
                keys = []
                for t, v, e in src:
                    comp = []
                    for i in range(3):
                        terms = [ex(t)[i] if callable(ex) else ex[i] for _, ex in self.mlayers[k]]
                        terms = [x for x in terms if x]
                        base = round(float(v[i]), 2)
                        if not terms:
                            comp.append(base)
                        else:
                            body = "+".join(terms)
                            comp.append(body if base == 0 else f"{_g(base)}+{body}")
                    keys.append((round(t, 4), comp, e) if e else (round(t, 4), comp))
                ch(bone, *keys)
                continue
            else:
                d = self._default(k)
                keys = []
                for t, p, e in fr:
                    v = [round(float(c), 2) for c in p.get(k, d)]
                    keys.append((round(t, 4), v, e) if e else (round(t, 4), v))
                # Drop keys that only repeat their neighbours.
                keep = [keys[0]]
                for i in range(1, len(keys) - 1):
                    if not (keys[i][1] == keys[i - 1][1] == keys[i + 1][1]):
                        keep.append(keys[i])
                keep.append(keys[-1]) if len(keys) > 1 else None
                keys = keep
                if all(kk[1] == list(d) for kk in keys):
                    continue  # never leaves rest: no channel at all
            ch(bone, *keys)
        return self.anim


def _g(x):
    """A short number for Molang."""
    return f"{x:.3f}".rstrip("0").rstrip(".") if x != int(x) else str(int(x))


def write_compact(f, path):
    """Writes an AnimFile without indentation (the big clip files)."""
    import json as _json
    import os as _os
    _os.makedirs(_os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        _json.dump({"format_version": "1.8.0", "animations": {a.name: a.to_json() for a in f.anims}}, fh, separators=(",", ":"))
        fh.write("\n")


def _simplify(keys, tol):
    """Drops samples that lie within `tol` of the straight line between their neighbours (linear keys)."""
    if len(keys) <= 2:
        return keys
    out = [keys[0]]
    for i in range(1, len(keys) - 1):
        t0, a = out[-1]
        t1, b = keys[i]
        t2, c = keys[i + 1]
        x = (t1 - t0) / (t2 - t0) if t2 != t0 else 0
        if max(abs(a[j] + (c[j] - a[j]) * x - b[j]) for j in range(3)) > tol:
            out.append(keys[i])
    out.append(keys[-1])
    return out


def P_(*poses):
    """Merge poses left to right (later ones win per channel)."""
    out = {}
    for p in poses:
        out.update(p)
    return out


def add_p(*poses):
    """Sum poses channel by channel."""
    out = {}
    for p in poses:
        for k, v in p.items():
            if k in out:
                out[k] = [out[k][i] + v[i] for i in range(3)]
            else:
                out[k] = list(v)
    return out


def scale_p(p, k):
    return {b: [c * k for c in v] for b, v in p.items() if not b.startswith("%")}


# --- world-space helpers (GeckoLib's math, as preview3d: Bedrock in, java inside) ------------------------

def _rx(a, p):
    c, s_ = math.cos(a), math.sin(a)
    return (p[0], p[1] * c - p[2] * s_, p[1] * s_ + p[2] * c)


def _ry(a, p):
    c, s_ = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s_, p[1], -p[0] * s_ + p[2] * c)


def _rz(a, p):
    c, s_ = math.cos(a), math.sin(a)
    return (p[0] * c - p[1] * s_, p[0] * s_ + p[1] * c, p[2])


def _rot(r, v):
    bx, by, bz = (math.radians(c) for c in r)
    return _rz(bz, _ry(-by, _rx(-bx, v)))


def _unrot(r, v):
    bx, by, bz = (math.radians(c) for c in r)
    return _rx(bx, _ry(by, _rz(-bz, v)))


def _chain(rig, name):
    by = {b.name: b for b in rig.bones}
    out, b = [], by[name]
    while b is not None:
        out.append(b)
        b = by.get(b.parent) if b.parent else None
    return out  # child first


def _brot(b, pose):
    rest = b.rotation or (0, 0, 0)
    a = pose.get(b.name, {}).get("rotation") or (0, 0, 0)
    return tuple(rest[i] + a[i] for i in range(3))


def world_point(rig, pose, bone, p):
    """Bedrock point in `bone`'s space -> Bedrock world point, for a preview3d-format pose."""
    q = (-p[0], p[1], p[2])
    for b in _chain(rig, bone):
        piv = (-b.pivot[0], b.pivot[1], b.pivot[2])
        a = pose.get(b.name, {})
        pos = a.get("position") or (0, 0, 0)
        scl = a.get("scale") or (1, 1, 1)
        d = tuple((q[i] - piv[i]) * scl[i] for i in range(3))
        d = _rot(_brot(b, pose), d)
        q = (d[0] + piv[0] - pos[0], d[1] + piv[1] + pos[1], d[2] + piv[2] + pos[2])
    return (-q[0], q[1], q[2])


def world_to_bone(rig, pose, bone, p):
    """Inverse of world_point (scale ignored)."""
    q = (-p[0], p[1], p[2])
    for b in reversed(_chain(rig, bone)):
        piv = (-b.pivot[0], b.pivot[1], b.pivot[2])
        pos = pose.get(b.name, {}).get("position") or (0, 0, 0)
        d = (q[0] - piv[0] + pos[0], q[1] - piv[1] - pos[1], q[2] - piv[2] - pos[2])
        d = _unrot(_brot(b, pose), d)
        q = (d[0] + piv[0], d[1] + piv[1], d[2] + piv[2])
    return (-q[0], q[1], q[2])


def world_dir(rig, pose, bone, v):
    q = (-v[0], v[1], v[2])
    for b in _chain(rig, bone):
        q = _rot(_brot(b, pose), q)
    return (-q[0], q[1], q[2])


def parent_offset(rig, pose, bone, world_delta):
    """A world-space displacement expressed as `bone`'s position channel (in its parent's frame)."""
    q = (-world_delta[0], world_delta[1], world_delta[2])
    ch = _chain(rig, bone)[1:]
    for b in reversed(ch):
        q = _unrot(_brot(b, pose), q)
    return [-q[0], q[1], q[2]]


def to_preview(pose):
    out = {}
    for k, v in pose.items():
        bone, ch = (k[1:], "position") if k[0] == "@" else (k[1:], "scale") if k[0] == "%" else (k, "rotation")
        out.setdefault(bone, {})[ch] = list(v)
    return out


def aim(rig, pose, bone, local, target):
    """The rotation (relative to rest, Y left at 0) that points `bone`'s local vector along world `target`, the rest
    of `pose` (simple format) given. Coarse-to-fine search; plenty for weapons."""
    n = math.sqrt(sum(c * c for c in target))
    tgt = tuple(c / n for c in target)
    base = to_preview(pose)

    def err(ax, az):
        base[bone] = {"rotation": [ax, 0, az]}
        d = world_dir(rig, base, bone, local)
        # Prefer the smallest turn that does the job: Euler keys blend through big turns badly.
        return -sum(d[i] * tgt[i] for i in range(3)) + 0.12 * (abs(ax) + abs(az)) / 360
    best = min(((err(ax, az), ax, az) for ax in range(-180, 181, 15) for az in range(-180, 181, 15)))
    for step in (5, 1):
        _, bx, bz = best
        best = min(((err(bx + i * step, bz + j * step), bx + i * step, bz + j * step) for i in range(-3, 4) for j in range(-3, 4)))
    return [best[1], 0, best[2]]


def aim2(rig, pose, bone, l1, t1, l2, t2):
    """Three-axis aim: local l1 along world t1 and local l2 along world t2 (e.g. a shield's face and its long axis)."""
    def nrm(v):
        n = math.sqrt(sum(c * c for c in v))
        return tuple(c / n for c in v)
    t1, t2 = nrm(t1), nrm(t2)
    base = to_preview(pose)

    def err(r):
        base[bone] = {"rotation": list(r)}
        d1, d2 = world_dir(rig, base, bone, l1), world_dir(rig, base, bone, l2)
        return -sum(d1[i] * t1[i] for i in range(3)) - sum(d2[i] * t2[i] for i in range(3)) + 0.1 * sum(abs(c) for c in r) / 360
    best = min((err((a, b, c)), (a, b, c)) for a in range(-180, 181, 30) for b in range(-180, 181, 30) for c in range(-180, 181, 30))
    for step in (10, 3, 1):
        r0 = best[1]
        best = min((err((r0[0] + i * step, r0[1] + j * step, r0[2] + k * step)), (r0[0] + i * step, r0[1] + j * step, r0[2] + k * step))
                   for i in (-1, 0, 1) for j in (-1, 0, 1) for k in (-1, 0, 1))
    return list(best[1])


def lagged(c, bone, lag, gain=1.0):
    """Overlapping action: `bone` follows its own keyed motion `lag` seconds late (wraps round in a loop)."""
    L = c.length

    def fn(t):
        tl = (t - lag) % L if c.loop else max(0.0, t - lag)
        a, b = c.value(bone, tl), c.value(bone, t)
        return [(a[i] - b[i]) * gain for i in range(3)]
    c.layer(bone, fn)


def ruffle(c, bones, amp, cycles, seed, axis=(1.0, 0.0, 0.35), t0=0.0, t1=None):
    """Additive feather ruffle, written as Molang: each bone its own phase (never all in sync); `cycles` whole cycles
    over the clip so loops close; one-shot clips fade it in and out over [t0, t1]."""
    L = c.length
    t1 = L if t1 is None else t1
    w = 360.0 * cycles / L
    for b in bones:
        ph = h01("ruffle", seed, b) * 360.0
        a = amp * (0.7 + 0.6 * h01("ramp", seed, b))

        def fn(t, ph=ph, a=a):
            env = 1.0 if c.loop else (0.0 if t <= t0 or t >= t1 else math.sin(math.pi * (t - t0) / (t1 - t0)))
            v = a * env * math.sin(math.radians(w * t + ph))
            return [v * axis[0], v * axis[1], v * axis[2]]
        sine = f"math.sin(query.anim_time*{_g(w)}+{_g(round(ph))})"
        if not c.loop:
            # The envelope: a half sine over [t0, t1], clamped at zero after it (clips end before it could rise again).
            assert L <= t1 + (t1 - t0), "ruffle envelope would reopen before the clip ends"
            sine += f"*math.max(0,math.sin((query.anim_time-{_g(t0)})*{_g(180.0 / (t1 - t0))}))"
        expr = [f"{sine}*{_g(round(a * k, 2))}" if k else None for k in axis]
        c.mlayer(b, fn, expr)


def flex(c, bones, amp, lag, step=0.004, group=12):
    """Feathers bending behind the stroke: a sine over the clip, lagging per feather (Molang, one key per pose key)."""
    L = c.length
    for i, b in enumerate(bones):
        d = lag + step * (i % group)

        def fn(t, d=d):
            return [amp * math.sin(2 * math.pi * (t - d) / L), 0, 0]
        c.mlayer(b, fn, [f"math.sin((query.anim_time-{_g(d)})*{_g(360.0 / L)})*{_g(amp)}", None, None])


# =====================================================================================================
# The Lance of Michael (shared by the vessel, the archangel and the item)
# =====================================================================================================

def leaf_blade(glow=True):
    """A leaf-shaped steel blade on a vertical north/south face (y = 0 at the tip): two facets either side of a
    diamond ridge (one lit, one in shadow), gold where it meets the socket, light only along its edges."""
    def inside(x, y, w, h):
        p = 1 - y / max(1, h - 1)  # 0 at the base, 1 at the tip
        half = (w - 1) / 2 * (math.sin(math.pi * (0.16 + 0.84 * p)) ** 0.8) + 0.3
        return abs(x - (w - 1) / 2) <= half, half

    def m(face, x, y, w, h):
        if face not in ("north", "south"):
            return None  # the plane's thickness is the ridge's job
        ok, half = inside(x, y, w, h)
        if not ok:
            return None
        p = 1 - y / max(1, h - 1)
        dx = x - (w - 1) / 2
        if abs(dx) > half - 1.0:
            return LIGHT[4]  # the edge of light
        if p < 0.08:
            return GOLD[4] if p > 0.04 else GOLD[2]
        if abs(dx) < 0.6:
            return SILVER[5]
        lit = (dx < 0) == (face == "north")
        v = (4.2 if lit else 2.6) + 0.7 * (1 - abs(dx) / max(1.0, half)) + 0.5 * p
        return tone(SILVER, v)

    def g(face, x, y, w, h, c):
        if c == LIGHT[4]:
            return LIGHT[5]
        return shade(c, 0.12)
    return m, (g if glow else None)


def spiral_shaft(face, x, y, w, h):
    off = {"north": 0, "east": 3, "south": 6, "west": 9}.get(face, 0)
    if face in ("up", "down"):
        return GOLD[3]
    if (y + x + off) % 12 in (0, 1):
        return GOLD[4] if (y + x + off) % 12 == 0 else GOLD[2]
    return tone(WHITE_METAL, 4.0 - (1 if x == 0 or x == w - 1 else 0))


def lance_parts(m, bone, base, length, k=1.0, grip=0.30, glow=True):
    """The Lance of Michael along +Y from `base` (the butt) to the tip: pommel, a white shaft wound with a gold spiral,
    a grip wrapped in blue leather between gold collars (at `grip` of the length), a socket, a guard with a gem and two
    small wings, and a long leaf blade of light with a ridge. About 30 cubes."""
    x, y0, z = base
    G = (lambda f: H.glow_faint(f)) if glow else (lambda f: None)
    gilt = Gilt(9501, 2)
    blade_l = length * 0.27
    shaft_top = y0 + length - blade_l - 4.0 * k
    th = 1.0 * k
    # Pommel: a ball and a cap.
    m.cube(bone, (x - 0.8 * k, y0, z - 0.8 * k), (1.6 * k, 1.0 * k, 1.6 * k), gilt.mat, gilt.glow, density=2, tag="pommel")
    m.cube(bone, (x - 1.1 * k, y0 + 1.0 * k, z - 1.1 * k), (2.2 * k, 1.5 * k, 2.2 * k), gilt.mat, gilt.glow, density=2,
           tag="pommel", rotation=(0, 45, 0), pivot=(x, y0, z))
    gy = y0 + length * grip
    gl = 6.0 * k
    s0 = y0 + 2.5 * k
    # Shaft below the grip, the grip, and the long shaft above it.
    m.cube(bone, (x - th / 2, s0, z - th / 2), (th, gy - gl / 2 - s0, th), spiral_shaft, G(0.2), density=2, tag="shaft")
    m.cube(bone, (x - th * 0.6, gy - gl / 2, z - th * 0.6), (th * 1.2, gl, th * 1.2),
           lambda f, xx, yy, w, h: TIE[3 + ((yy + xx) % 4 == 0)] if f not in ("up", "down") else TIE[2], G(0.1), density=2,
           tag="grip")
    m.cube(bone, (x - th / 2, gy + gl / 2, z - th / 2), (th, shaft_top - gy - gl / 2, th), spiral_shaft, G(0.2), density=2,
           tag="shaft")
    for cy in (s0, gy - gl / 2 - 0.5 * k, gy + gl / 2, shaft_top - 1.0 * k):
        m.cube(bone, (x - th * 0.8, cy, z - th * 0.8), (th * 1.6, 0.75 * k, th * 1.6), gilt.mat, gilt.glow, density=2,
               tag="collar")
    # Socket and guard.
    m.cube(bone, (x - 1.0 * k, shaft_top, z - 1.0 * k), (2.0 * k, 2.0 * k, 2.0 * k), gilt.mat, gilt.glow, density=2,
           tag="socket", rotation=(0, 45, 0), pivot=(x, shaft_top, z))
    gy2 = shaft_top + 2.0 * k
    m.cube(bone, (x - 3.0 * k, gy2, z - 0.7 * k), (6.0 * k, 1.2 * k, 1.4 * k), gilt.mat, gilt.glow, density=2, tag="guard")
    m.cube(bone, (x - 0.6 * k, gy2 + 0.1 * k, z - 1.0 * k), (1.2 * k, 1.0 * k, 0.5 * k), solid(LIGHT[4]),
           (lambda *a: LIGHT[5]) if glow else None, density=2, tag="gem")
    for s in (-1, 1):
        # Quillon tips curling up, and a small wing of three feathers swept up and out, gold-edged.
        m.cube(bone, (x + s * 3.0 * k - 0.6 * k, gy2, z - 0.6 * k), (1.2 * k, 2.0 * k, 1.2 * k), gilt.mat, gilt.glow,
               density=2, tag="quillon", rotation=(0, 0, -25 * s), pivot=(x + s * 3.0 * k, gy2, z))
        wf = MetalFeather("primary", s, 9511, WHITE_METAL, up=True)
        for i in range(3):
            L = (6.5 - i * 1.3) * k
            px = x + s * (1.6 + i * 1.1) * k
            m.cube(bone, (px - 0.8 * k, gy2 + 1.0 * k, z - 0.25 * k), (1.6 * k, L, 0.5 * k), wf.mat, wf.glow, density=2,
                   tag="guard_wing", rotation=(0, 0, s * (34 + i * 20)), pivot=(px, gy2 + 1.0 * k, z))
    # The blade: a leaf plane, a ridge of light along it (diamond section), and two small lugs below.
    by = gy2 + 1.2 * k
    bw = 3.6 * k
    lm, lg = leaf_blade(glow)
    m.cube(bone, (x - bw / 2, by, z - 0.25 * k), (bw, blade_l, 0.5 * k), lm, lg, density=2, tag="lance_blade")
    m.cube(bone, (x - 0.35 * k, by, z - 0.35 * k), (0.7 * k, blade_l * 0.86, 0.7 * k),
           lambda f, xx, yy, w, h: SILVER[{"north": 5, "west": 4, "east": 2, "south": 3}.get(f, 2)], G(0.2),
           density=2, tag="ridge", rotation=(0, 45, 0), pivot=(x, by, z))
    for s in (-1, 1):
        m.cube(bone, (x + s * 1.2 * k - 0.4 * k, by - 0.6 * k, z - 0.4 * k), (0.8 * k, 1.6 * k, 0.8 * k), gilt.mat, gilt.glow,
               density=2, tag="lug", rotation=(0, 0, -40 * s), pivot=(x + s * 1.2 * k, by - 0.6 * k, z))
    return y0 + length


# =====================================================================================================
# The vessel
# =====================================================================================================

FACE = [  # north face of the head, 16x16 (2 texels per px); x=0 is his right
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhsshhhsshhhshhh",
    "hssssssssssssssh",
    "sbbbbssssssbbbbs",
    "sswEEssnsssEEwss",
    "ssuuussnssssuuss",
    "sssssssnnsssssss",
    "ssssssNNNNssssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "ssssssMMMMssssss",
    "jssssssssssssssj",
    "jjssssssssssssjj",
]

WING_SPECS = {
    # pair: (root y, root x, scale, leading edge (out, rise) px, rest rotation of the right wing)
    1: (21.5, 1.5, 1.0, (30.0, 8.0), (0, 22, 12)),
    2: (17.5, 1.5, 0.78, (24.0, -2.0), (0, 30, -12)),
}
FLIGHT, COVERTS = 12, 6
LANCE_TILT = 12.0


def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 12, 0), "root")
    m.bone("head", (0, 24, 0), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (5 * s, 22, 0), "body")
        m.bone(f"{side}_forearm", (6 * s, 18, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (6 * s, 13.5, 0), f"{side}_forearm")
        m.bone(f"{side}_leg", (2 * s, 12, 0), "root")
        m.bone(f"{side}_shin", (2 * s, 6, 0), f"{side}_leg")


def rig():
    m = Model("michael", 512, 512)
    skeleton(m)
    build_man(m)
    build_blade(m, "right_hand")
    build_lance(m)
    build_shadow_wings(m)
    return m


def build_man(m):
    glow = H.glow_faint(0.10)
    skin = H.skin_mat(SKIN, 4101, 3.2, 0.35)
    hair = lambda f, x, y, w, h: tone(HAIR, 2.6 + 0.9 * math.sin(x * 1.3 + y * 0.5) - 0.4 * (y / max(1, h)))

    def eye_glow(face, x, y, w, h, col):
        if col == EYE_CORE or col == EYE:
            return col
        if col == EYE_WHITE:
            return LIGHT[2]
        return shade(col, 0.10)
    legend = {"h": hair, "s": skin, "b": HAIR[2], "w": EYE_WHITE, "E": EYE, "u": mix(SKIN[2], hexc("#7a6a80"), 0.25),
              "n": SKIN[4], "N": SKIN[1], "m": hexc("#8f5a4f"), "M": hexc("#b97d6d"), "j": SKIN[2]}
    H.head_cube(m, FACE, legend, H.head_sides(skin, hair, top=4, back=12, temple=7, sideburn=7), glow=eye_glow)
    # Short dark hair: a shell over the crown and back with a soft, uneven fringe.
    m.cube("head", (-4, 29.5, -4), (8, 2.5, 8), none_on(("down",), lambda f, x, y, w, h: None if f == "north" and y > 2 + (x % 4 == 1)
           else hair(f, x, y, w, h)), glow=glow, inflate=0.25, density=2, tag="hair")
    m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2], glow=glow,
           density=2, tag="nose")
    for s in (-1, 1):
        m.cube("head", (4 * s - 0.25 + (0 if s > 0 else -0.25), 26, -0.5), (0.5, 2, 1.5), solid(SKIN[2]), glow=glow,
               density=2, tag="ear")
    m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, glow=glow, density=2, tag="neck")

    shirt = fabric(SHIRT, 4104, 3.6, 0.15)
    suit = fabric(SUIT, 4105, 3.1, 0.3)

    def tie(f, x, y, w, h):
        return TIE[4] if x == 7 else TIE[2]  # a sheen down the silk
    m.cube("body", (-4, 12, -2), (8, 12, 4), shirt, glow=glow, density=2, tag="torso")
    front = H.jacket_front(suit, SUIT[4], shirt, tie=tie, gap=(5, 10), v_depth=11, buttons=SUIT[0], button_rows=(13, 17))
    m.cube("body", (-4, 12, -2), (8, 12, 4), none_on(("down",), front), glow=glow, inflate=0.3, density=2, tag="jacket")
    m.cube("body", (-4, 9.5, -2), (8, 2.5, 4), none_on(("up", "down"), lambda f, x, y, w, h: None if f == "north" and 7 <= x <= 8
           else suit(f, x, y, w, h)), glow=glow, inflate=0.35, density=2, tag="jacket_skirt")
    m.cube("body", (-4, 22, -2), (8, 2, 4), none_on(("down",), lambda f, x, y, w, h: None if f in ("north", "up") and 5 <= x <= 10
           else SUIT[3]), glow=glow, inflate=0.45, density=2, tag="collar")
    # Notch lapels, the white shirt collar's points, the tie's knot and a breast pocket.
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            c = tone(SUIT, 4.2 - 0.6 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = SUIT[2]  # the roll of the lapel
            if f == "north" and y < 2 and (x >= w - 1 if s < 0 else x == 0):
                return None  # the notch
            return c
        m.cube("body", (2.4 * s - 1, 17.4, -2.95), (2, 6, 0.5), lapel, glow, density=2, tag="lapel",
               rotation=(3, 0, 18 * s), pivot=(1.0 * s, 17.4, -2.7))
        m.cube("body", (0.9 * s - 0.75, 22.6, -2.45), (1.5, 1.25, 0.4), lambda f, x, y, w, h: SHIRT[5] if y == 0 else SHIRT[4],
               glow, density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.9 * s, 23.7, -2.3))
    m.cube("body", (-0.6, 22.2, -2.6), (1.2, 1.0, 0.4), solid(TIE[3]), glow, density=2, tag="tie_knot")
    m.cube("body", (1.5, 19.2, -2.4), (2.0, 0.5, 0.2), lambda f, x, y, w, h: SUIT[1] if y == 0 else SUIT[4], glow, density=4,
           tag="breast_pocket")
    hand = H.skin_mat(SKIN, 4106, 3.3, 0.35)
    for side in ("right", "left"):
        H.arm_cubes(m, side, suit, hand, glow=glow, cuff=lambda f, x, y, w, h: SHIRT[5], sleeve_inflate=0.25)
        shoe = lambda f, x, y, w, h: SHOE[5] if (f == "up" and y < 3) or (f == "north" and y == 0) else (SHOE[1] if y == h - 1 and f != "up" else SHOE[2])
        H.leg_cubes(m, side, fabric(SUIT, 4107, 2.9, 0.15, crease=True), shoe, glow=glow)
    # The glowing palm of the left hand (the forehead touch): a disc of light on its inner face.
    palm = m.bone("palm_light", (5.0, 12.25, 0), "left_hand")

    def disc(face, x, y, w, h):
        r = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) / (w / 2)
        if r > 1.0:
            return None
        return mix(EYE_CORE, LIGHT[3], r)
    m.cube(palm, (4.45, 11.0, -1.25), (0, 2.5, 2.5), disc, lambda f, x, y, w, h, c: c, faces=("west",), density=4, tag="palm")


def build_blade(m, hand, x=-6.0, y=12.25):
    """The angel blade: a dark wrapped grip and a silver blade of triangular section, held point forward and down."""
    b = m.bone("blade", (x, y, 0), hand, rotation=(32, 0, 0))
    m.cube(b, (x - 0.5, y - 0.5, -0.5), (1, 1, 3.5), lambda f, xx, yy, w, h: TIE[2 + ((xx + yy) % 3 == 0) * 2], density=2,
           tag="grip")
    m.cube(b, (x - 0.6, y - 0.6, 3.0), (1.2, 1.2, 0.5), solid(SILVER[2]), density=2, tag="pommel")
    m.cube(b, (x - 0.75, y - 0.75, -1.0), (1.5, 1.5, 0.5), solid(SILVER[3]), density=2, tag="guard")

    def steel(f, xx, yy, w, h):
        if f in ("north", "south", "up", "down"):
            return SILVER[5]
        return SILVER[4] if f == "west" else SILVER[2]  # two lit facets and a shadowed one
    # Triangular section: a square bar turned 45 degrees about its axis, plus a thin spine on top for the third edge.
    m.cube(b, (x - 0.5, y - 0.5, -9.0), (1, 1, 8), steel, H.glow_faint(0.3), density=2, tag="blade", rotation=(0, 0, 45),
           pivot=(x, y, 0))
    m.cube(b, (x - 0.25, y - 0.25, -10.0), (0.5, 0.5, 1.0), solid(SILVER[5]), H.glow_faint(0.4), density=2, tag="tip",
           rotation=(0, 0, 45), pivot=(x, y, 0))


def build_lance(m):
    # Leaning out from the fist, the butt by his right foot, the blade well clear of his face.
    b = m.bone("lance", (-6, 12.25, 0), "right_hand", rotation=(0, 0, -LANCE_TILT))
    lance_parts(m, b, (-6, 0.5, 0), 48.0, 1.0, grip=11.75 / 48.0)


def build_shadow_wings(m):
    sw = m.bone("shadow_wings", (0, 20, 2.5), "body")
    for pair, (ry, rx, k, (out, rise), rest) in WING_SPECS.items():
        for side, s in (("r", -1), ("l", 1)):
            name = f"wing_{side}{pair}"
            root = (rx * s, ry, 2.6)
            w = m.bone(name, root, sw.name, rotation=mirror_rot(rest, s))
            x0, y0, z0 = root
            tip = (x0 + s * out, y0 + rise, z0)
            seed = 5100 + pair * 100 + (0 if s < 0 else 50)
            # The arm of the wing: a band of dense smoke along the leading edge, lit along its top.
            def arm_m(f, x, y, w_, h, s=s, seed=seed):
                t = y / max(1, h - 1)
                q = x / max(1, w_ - 1)  # 0 at the root .. 1 at the tip
                if (f == "north") == (s < 0):
                    q = 1 - q
                if t > 0.45 + 0.4 * (1 - q) or q > 0.97:
                    return None
                n = fbm(x * 0.8, y * 4, seed, 64, 64, 3, 5.0)
                if t < 0.2:
                    col = mix(LIGHT[3], LIGHT[1], q)
                    return (col[0], col[1], col[2], int(185 - 300 * t))
                col = mix(SMOKE[2 + int(n * 2)], LIGHT[0], 0.2 * n)
                return (col[0], col[1], col[2], int(225 - 60 * q))
            edge_cube(m, w, root, tip, 4.0 * k, s, arm_m, smoke_glow, tag="wing_arm", dz=-0.1)
            for i in range(FLIGHT):
                f = i / (FLIGHT - 1)
                t = 0.1 + 0.9 * f
                px, py = x0 + s * out * t, y0 + rise * t
                length = (16 + 14 * f) * k * (0.94 + 0.12 * h01("vl", seed, i))
                angle = 6 + 80 * f ** 1.25 + (h01("va", seed, i) - 0.5) * 5
                add_feather(m, w, f"{name}_f{i:02d}", (px, py - 0.5, z0 + 0.12 * i), length, 6.5 * k, angle, s,
                            smoke_mat(seed + i), smoke_glow, thick=0, faces=("north", "south"), tag="shadow_feather")
            for j in range(COVERTS):
                f = j / (COVERTS - 1)
                t = 0.05 + 0.8 * f
                px, py = x0 + s * out * t, y0 + rise * t
                add_feather(m, w, f"{name}_f{FLIGHT + j:02d}", (px, py, z0 + 1.6), (9 + 3 * f) * k, 7.0 * k, 20 + 40 * f, s,
                            smoke_mat(seed + 30 + j, 230, broad=True), smoke_glow, thick=0, faces=("north", "south"),
                            tag="shadow_covert")


# --- animations ------------------------------------------------------------------------------------------

VFEATHERS = [f"wing_{sd}{p}_f{i:02d}" for p in (1, 2) for sd in ("r", "l") for i in range(FLIGHT + COVERTS)]
VWINGS = ("wing_r1", "wing_l1", "wing_r2", "wing_l2")


def vwing(m, up, low):
    """Deltas bringing the shadow wings to absolute right-wing rotations `up` (pair 1) and `low` (pair 2), mirrored."""
    out = {}
    for pair, tgt in ((1, up), (2, low)):
        for sd, s in (("r", -1), ("l", 1)):
            n = f"wing_{sd}{pair}"
            rest = m.rig.get(n).rotation
            t = mirror_rot(tgt, s)
            out[n] = [t[i] - rest[i] for i in range(3)]
    return out


def vfan(m, k, lift=0.0):
    """Feather deltas: k < 1 closes each wing's fan, k > 1 opens it wider; `lift` tilts them out of the plane."""
    out = {}
    for n in VFEATHERS:
        rz = m.rig.get(n).rotation[2]
        out[n] = [lift, 0, rz * (k - 1)]
    return out


V_FOLD = ((0, 80, -25), (0, 85, -40))        # tucked along the back
V_REST = ((0, 22, 12), (0, 30, -12))
V_OPEN = ((0, 8, 22), (0, 14, -6))
V_UP = ((0, 18, 52), (0, 24, 22))            # wings raised for a downstroke
V_DOWN = ((0, 6, -20), (0, 12, -38))         # end of a downstroke
V_FORWARD = ((0, -16, 18), (0, -12, -6))    # swept forward (the buffet)
V_WRAP = ((0, -28, 2), (0, -24, -26))      # wrapped round him


def kneel(head=15):
    return {"@root": [0, -5.5, 0], "left_leg": [-80, 0, -4], "left_shin": [80, 0, 0], "right_leg": [5, 0, 6],
            "right_shin": [85, 0, 0], "body": [18, 0, 0], "head": [head, 0, 0], "right_arm": [-15, 0, 8],
            "left_arm": [-35, 0, -10], "left_forearm": [-30, 0, 0]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()

    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def lance_at(pose, d):
        return P_(pose, {"lance": aim(r, pose, "lance", (0, 1, 0), d)})

    W = lambda up, low=None: vwing(m, up, low if low is not None else up)

    # idle: breathing, a look around; the shadow wings breathe, every feather on its own phase.
    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        c.key(t, P_({"body": [0.8 * abs(k), 0, 0], "head": [-1.5 * abs(k), 5 * k, 0], "right_arm": [-1.5 * abs(k), 0, 1.5],
                     "left_arm": [-1.5 * abs(k), 0, -1.5], "@body": [0, -0.15 * abs(k), 0]},
                    W(tuple(V_REST[0][i] + (0, -2, 3)[i] * abs(k) for i in range(3)),
                      tuple(V_REST[1][i] + (0, -2, 2)[i] * abs(k) for i in range(3)))), E_IO if t else None)
    ruffle(c, VFEATHERS, 3.0, 2, 41)

    # walk / run
    def gait(name, L_, stride, arm, lean, knee, bob):
        c = clip(name, L_, loop=True)
        for i in range(5):
            t = L_ * i / 4
            ph = 2 * math.pi * i / 4
            sn = math.sin(ph)
            c.key(t, {"right_leg": [-stride * sn, 0, 0], "left_leg": [stride * sn, 0, 0],
                      "right_shin": [knee * max(0, math.sin(ph + 1.2)), 0, 0], "left_shin": [knee * max(0, -math.sin(ph + 1.2)), 0, 0],
                      "right_arm": [arm * sn, 0, 3], "left_arm": [-arm * sn, 0, -3],
                      "right_forearm": [-arm * 0.8, 0, 0], "left_forearm": [-arm * 0.8, 0, 0],
                      "body": [lean, 4 * sn, 0], "@root": [0, bob * abs(math.cos(ph)), 0]}, E_IO if i else None)
        return c
    gait("walk", 1.2, 26, 16, 2, 30, 0.4)
    gait("run", 0.7, 48, 45, 12, 70, 1.0)

    # intro: falls out of the sky, lands kneeling, stands, looks up.
    c = clip("intro", 4.0)
    fall = {"@root": [0, 48, 0], "body": [-6, 0, 0], "right_arm": [-20, 0, 25], "left_arm": [-20, 0, -25], "head": [-10, 0, 0],
            "right_leg": [-10, 0, 0], "left_leg": [6, 0, 0]}
    impact = P_(kneel(28), {"@root": [0, -6.2, 0], "body": [26, 0, 0], "left_arm": [-60, 0, -14], "left_forearm": [-20, 0, 0],
                            "right_arm": [-8, 0, 20]})
    c.key(0, fall)
    c.key(0.7, impact, E_IN)
    c.key(1.0, kneel(24), E_OUT)
    c.key(1.9, kneel(20), E_IO)
    c.key(2.9, {"body": [4, 0, 0], "head": [10, 0, 0], "right_leg": [-6, 0, 0], "left_leg": [-4, 0, 0]}, E_IO)
    c.key(3.5, {"head": [-6, 0, 0]}, E_OUT)
    c.key(4.0, {}, E_IO)

    # transition: eyes closed, head bowed, a roll of the neck, then he straightens with a flick of the blade.
    c = clip("transition", 2.5)
    c.key(0, {})
    c.key(0.6, {"head": [24, 0, 0], "body": [6, 0, 0], "right_arm": [-6, 0, 6]}, E_IO)
    c.key(1.1, {"head": [10, 0, 22], "body": [4, 0, 0]}, E_IO)
    c.key(1.6, {"head": [-8, 0, -6], "body": [-4, 0, 0], "right_arm": [-40, 25, 30], "right_forearm": [-30, 0, 0]}, E_BACK)
    c.key(2.5, {}, E_IO)

    # blade combos: anticipation -> cut (hit at 0.35) -> follow-through -> recover.
    ready = {"right_arm": [-25, 0, 8], "right_forearm": [-25, 0, 0], "left_arm": [-10, 0, -10], "body": [2, 0, 0]}
    combos = {
        "blade_combo_1": (0.9, {"right_arm": [-125, 30, 45], "right_forearm": [-40, 0, 0], "body": [-4, 28, 0], "head": [0, -14, 0],
                                "left_arm": [-30, 0, -20], "right_leg": [6, 0, 0], "left_leg": [-10, 0, 0]},
                          {"right_arm": [-60, -50, -10], "right_forearm": [-10, 0, 0], "body": [10, -30, 0], "head": [0, 14, 0],
                           "left_arm": [10, 0, -24], "right_leg": [-22, 0, 0], "left_leg": [16, 0, 0], "@root": [0, -0.8, -1.5]},
                          {"right_arm": [-22, -62, -22], "right_forearm": [-8, 0, 0], "body": [8, -38, 0], "head": [0, 18, 0],
                           "left_arm": [14, 0, -20], "right_leg": [-18, 0, 0], "left_leg": [14, 0, 0], "@root": [0, -0.6, -1.5]}),
        "blade_combo_2": (0.9, {"right_arm": [-85, -62, -24], "right_forearm": [-55, 0, 0], "body": [2, -32, 0], "head": [0, 12, 0],
                                "left_arm": [-20, 0, -30]},
                          {"right_arm": [-78, 52, 52], "right_forearm": [-8, 0, 0], "body": [8, 30, 0], "head": [0, -12, 0],
                           "left_arm": [-30, 0, -8], "left_leg": [-20, 0, 0], "right_leg": [14, 0, 0], "@root": [0, -0.8, -1.5]},
                          {"right_arm": [-50, 70, 66], "right_forearm": [-4, 0, 0], "body": [6, 38, 0], "head": [0, -16, 0],
                           "left_arm": [-36, 0, -4], "left_leg": [-16, 0, 0], "right_leg": [12, 0, 0], "@root": [0, -0.6, -1.5]}),
        "blade_combo_3": (1.1, {"right_arm": [-28, 24, 16], "right_forearm": [-95, 0, 0], "body": [-6, 24, 0], "head": [0, -10, 0],
                                "right_leg": [12, 0, 0], "left_leg": [-18, 0, 0], "left_arm": [-40, 0, -24]},
                          {"right_arm": [-92, -4, 0], "right_forearm": [-2, 0, 0], "body": [20, -10, 0], "head": [-10, 6, 0],
                           "left_leg": [-40, 0, 0], "left_shin": [30, 0, 0], "right_leg": [26, 0, 0], "left_arm": [20, 0, -20],
                           "@root": [0, -1.5, -3.5]},
                          {"right_arm": [-86, -6, 0], "right_forearm": [-4, 0, 0], "body": [22, -12, 0], "head": [-8, 6, 0],
                           "left_leg": [-38, 0, 0], "left_shin": [28, 0, 0], "right_leg": [24, 0, 0], "left_arm": [22, 0, -20],
                           "@root": [0, -1.5, -3.5]}),
    }
    for name, (L_, wind, hit, follow) in combos.items():
        c = clip(name, L_)
        c.key(0, ready)
        c.key(0.2, wind, E_OUT)
        c.key(0.35, hit, E_SNAP)
        c.key(0.6 if L_ > 1 else 0.55, follow, E_OUT)
        c.key(L_, ready, E_IO)

    # forehead_touch: two fingers to the brow. A slow reach (windup 1.2 s, the palm brightening), the grab at 1.2,
    # the burn (a tremor), release.
    c = clip("forehead_touch", 2.0)
    reach = {"left_arm": [-92, 6, -4], "left_forearm": [-14, 0, 0], "head": [4, 0, 0], "body": [4, 0, 0], "%palm_light": [1.7, 1.7, 1.7],
             "right_arm": [-6, 0, 8]}
    grab = {"left_arm": [-102, 12, -8], "left_forearm": [-4, 0, 0], "head": [-2, 0, 0], "body": [12, 0, 0], "@root": [0, 0, -2.5],
            "%palm_light": [2.4, 2.4, 2.4], "right_arm": [10, 0, 12], "left_leg": [-18, 0, 0], "right_leg": [10, 0, 0]}
    c.key(0, {})
    c.key(1.0, reach, E_IO)
    c.key(1.2, grab, E_SNAP)
    c.key(1.6, P_(grab, {"%palm_light": [2.2, 2.2, 2.2]}))
    c.key(1.8, {"left_arm": [-55, 0, -20], "left_forearm": [-20, 0, 0], "%palm_light": [1, 1, 1], "body": [4, 0, 0]}, E_OUT)
    c.key(2.0, {}, E_IO)
    c.layer("left_arm", lambda t: [1.6 * math.sin(t * 90) if 1.2 < t < 1.6 else 0, 0, 0])

    # smite: the fist raised high, brought down (hit ~0.8).
    c = clip("smite", 1.6)
    c.key(0, {})
    c.key(0.5, {"right_arm": [-170, 0, 12], "right_forearm": [-20, 0, 0], "body": [-10, 0, 0], "head": [-14, 0, 0],
                "left_arm": [-20, 0, -30]}, E_OUT)
    c.key(0.8, {"right_arm": [-38, 0, 0], "right_forearm": [0, 0, 0], "body": [22, 0, 0], "head": [10, 0, 0], "@root": [0, -2, -1],
                "right_leg": [-10, 0, 0], "left_leg": [-20, 0, 0], "right_shin": [20, 0, 0], "left_shin": [24, 0, 0],
                "left_arm": [10, 0, -24]}, E_SNAP)
    c.key(1.05, {"right_arm": [-30, 0, 0], "body": [18, 0, 0], "head": [8, 0, 0], "@root": [0, -1.6, -1], "right_leg": [-8, 0, 0],
                 "left_leg": [-16, 0, 0], "right_shin": [16, 0, 0], "left_shin": [20, 0, 0]})
    c.key(1.6, {}, E_IO)

    # parried: thrown back, the blade arm flung wide.
    c = clip("parried", 1.0)
    c.key(0, ready)
    c.key(0.15, {"body": [-16, 10, 0], "@root": [0, 0, 3], "right_arm": [-130, 0, 55], "right_forearm": [-10, 0, 0], "head": [-16, 8, 0],
                 "left_arm": [-30, 0, -30], "right_leg": [14, 0, 0], "left_leg": [-8, 0, 0]}, E_SNAP)
    c.key(0.5, {"body": [-6, 4, 0], "@root": [0, 0, 2], "right_arm": [-60, 0, 30], "head": [-4, 0, 0]}, E_OUT)
    c.key(1.0, ready, E_IO)

    # ask_yes: a calm open hand, head tilted.
    c = clip("ask_yes", 3.0)
    ask = {"left_arm": [-52, -12, -6], "left_forearm": [-38, 0, 0], "left_hand": [0, -60, 0], "head": [6, 0, 8], "body": [3, -6, 0],
           "right_arm": [-4, 0, 6]}
    c.key(0, {})
    c.key(0.8, ask, E_IO)
    c.key(2.3, P_(ask, {"head": [8, 0, 10]}), E_IO)
    c.key(3.0, {}, E_IO)

    # command: the blade (or the lance) raised to the sky, then levelled.
    c = clip("command", 2.0)
    up = {"right_arm": [-165, 0, 14], "right_forearm": [-8, 0, 0], "head": [-12, 0, 0], "body": [-4, 0, 0], "left_arm": [-20, 0, -28]}
    c.key(0, {})
    c.key(0.5, lance_at(up, (0.1, 1, -0.2)), E_BACK)
    lvl = {"right_arm": [-100, 0, 10], "right_forearm": [-6, 0, 0], "head": [0, 0, 0], "body": [4, 0, 0], "left_arm": [-10, 0, -20]}
    c.key(1.2, lance_at(lvl, (0, 0.15, -1)), E_IO)
    c.key(1.5, lance_at(lvl, (0, 0.15, -1)))
    c.key(2.0, {}, E_IO)

    # summon_host: arms rising wide, palms up, head back; the wings answer.
    c = clip("summon_host", 2.5)
    wide = P_({"right_arm": [-20, 0, 100], "left_arm": [-20, 0, -100], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
               "head": [-22, 0, 0], "body": [-6, 0, 0]}, W(V_OPEN[0], V_OPEN[1]), vfan(m, 1.12))
    c.key(0, {})
    c.key(1.1, wide, E_IO)
    c.key(1.8, P_(wide, {"head": [-26, 0, 0]}))
    c.key(2.5, {}, E_IO)

    # shadow_wings_unfurl: the shadows grow out of his back, sweep open past their rest, settle; the feathers fan
    # open one after another.
    c = clip("shadow_wings_unfurl", 2.0)
    closed = P_(W(*V_FOLD), vfan(m, 0.15), {"%shadow_wings": [0.05, 0.05, 0.05]})
    c.key(0, closed)
    c.key(0.5, P_(W(*V_FOLD), vfan(m, 0.2), {"%shadow_wings": [0.9, 0.9, 0.9], "body": [6, 0, 0], "head": [10, 0, 0]}), E_OUT)
    c.key(1.1, P_(W(*V_OPEN), vfan(m, 1.15), {"%shadow_wings": [1.12, 1.12, 1.12], "body": [-8, 0, 0], "head": [-14, 0, 0],
                                             "right_arm": [-10, 0, 22], "left_arm": [-10, 0, -22]}), E_BACK)
    c.key(1.5, P_(W(*V_REST), vfan(m, 1.0), {"%shadow_wings": [1, 1, 1]}), E_IO)
    c.key(2.0, {"%shadow_wings": [1, 1, 1]}, E_IO)
    for i, n in enumerate(VFEATHERS):
        k = i % (FLIGHT + COVERTS)
        lag = 0.04 * k
        c.layer(n, lambda t, lag=lag: [10 * math.sin(math.pi * min(1, max(0, (t - 0.7 - lag) / 0.6))), 0, 0])

    # lance_throw: wind up, release at 0.55, follow through.
    c = clip("lance_throw", 1.2)
    wind = {"right_arm": [-150, 25, 40], "right_forearm": [-55, 0, 0], "body": [-10, 32, 0], "head": [0, -26, 0], "left_arm": [-85, 0, -18],
            "left_leg": [-24, 0, 0], "right_leg": [16, 0, 0]}
    rel = {"right_arm": [-105, -18, 4], "right_forearm": [-4, 0, 0], "body": [16, -18, 0], "head": [-4, 14, 0], "left_arm": [10, 0, -20],
           "left_leg": [-30, 0, 0], "right_leg": [24, 0, 0], "@root": [0, -0.8, -2]}
    fol = {"right_arm": [-40, -34, -8], "right_forearm": [-6, 0, 0], "body": [22, -26, 0], "head": [0, 20, 0], "left_arm": [16, 0, -14],
           "left_leg": [-26, 0, 0], "right_leg": [22, 0, 0], "@root": [0, -0.8, -2]}
    c.key(0, {})
    c.key(0.38, lance_at(wind, (0, 0.25, -1)), E_OUT)
    c.key(0.55, lance_at(rel, (0, -0.05, -1)), E_SNAP)
    c.key(0.8, lance_at(fol, (0, -0.4, -1)), E_OUT)
    c.key(1.2, {}, E_IO)

    # lance_recall: the hand reaches out, the lance slaps into it at ~0.75, the arm gives with it.
    c = clip("lance_recall", 1.2)
    reach = {"right_arm": [-115, 0, 32], "right_forearm": [-4, 0, 0], "head": [-8, -10, 0], "body": [-2, 10, 0]}
    c.key(0, {})
    c.key(0.55, lance_at(reach, (0, 1, -0.3)), E_OUT)
    c.key(0.75, lance_at(P_(reach, {"right_arm": [-95, 0, 26], "body": [6, 6, 0], "@root": [0, -0.6, 1]}), (0, 1, -0.2)), E_SNAP)
    c.key(1.2, {}, E_IO)

    # lance_thrust: drawn back at the hip, lunge (hit 0.45), hold, recover.
    c = clip("lance_thrust", 1.0)
    back = {"right_arm": [-26, 14, 12], "right_forearm": [-62, 0, 0], "body": [-6, 26, 0], "head": [0, -18, 0], "left_arm": [-40, 0, -20],
            "right_leg": [12, 0, 0], "left_leg": [-16, 0, 0]}
    lunge = {"right_arm": [-86, 0, 2], "right_forearm": [-4, 0, 0], "body": [20, -10, 0], "head": [-10, 6, 0], "left_arm": [24, 0, -18],
             "left_leg": [-42, 0, 0], "left_shin": [34, 0, 0], "right_leg": [28, 0, 0], "@root": [0, -1.6, -4]}
    c.key(0, {})
    c.key(0.28, lance_at(back, (0, 0.05, -1)), E_OUT)
    c.key(0.45, lance_at(lunge, (0, 0, -1)), E_SNAP)
    c.key(0.65, lance_at(lunge, (0, -0.05, -1)))
    c.key(1.0, {}, E_IO)

    # lance_sweep: held level, swept from his right across to his left (hit 0.6).
    c = clip("lance_sweep", 1.3)
    s0 = {"right_arm": [-80, 58, 55], "right_forearm": [-20, 0, 0], "body": [0, 44, 0], "head": [0, -20, 0], "left_arm": [-30, 0, -30],
          "@root": [0, -1.2, 0], "right_leg": [-10, 0, 0], "left_leg": [10, 0, 0]}
    s1 = {"right_arm": [-82, -36, 0], "right_forearm": [-6, 0, 0], "body": [6, -30, 0], "head": [0, 14, 0], "left_arm": [-10, 0, -40],
          "@root": [0, -1.6, 0], "right_leg": [-14, 0, 0], "left_leg": [14, 0, 0]}
    s2 = {"right_arm": [-70, -80, -12], "right_forearm": [-4, 0, 0], "body": [8, -52, 0], "head": [0, 26, 0], "left_arm": [-6, 0, -44],
          "@root": [0, -1.4, 0], "right_leg": [-12, 0, 0], "left_leg": [12, 0, 0]}
    c.key(0, {})
    c.key(0.38, lance_at(s0, (-1, 0, 0.4)), E_OUT)
    c.key(0.6, lance_at(s1, (0.3, 0, -1)), E_SNAP)
    c.key(0.85, lance_at(s2, (1, 0, -0.1)), E_OUT)
    c.key(1.3, {}, E_IO)

    # flight: takeoff, hover, dive, land.
    dangle = {"right_leg": [12, 0, 4], "left_leg": [6, 0, -4], "right_shin": [24, 0, 0], "left_shin": [18, 0, 0], "body": [8, 0, 0],
              "right_arm": [-10, 0, 14], "left_arm": [-10, 0, -14]}
    c = clip("takeoff", 1.2)
    crouch = P_({"@root": [0, -2.6, 0], "right_leg": [-34, 0, 0], "left_leg": [-34, 0, 0], "right_shin": [56, 0, 0], "left_shin": [56, 0, 0],
                 "body": [22, 0, 0], "right_arm": [10, 0, 16], "left_arm": [10, 0, -16]}, W(*V_UP), vfan(m, 0.9))
    c.key(0, {})
    c.key(0.35, crouch, E_OUT)
    c.key(0.6, P_({"@root": [0, 3, 0], "right_leg": [14, 0, 0], "left_leg": [10, 0, 0], "body": [-4, 0, 0], "right_arm": [-20, 0, 24],
                   "left_arm": [-20, 0, -24]}, W(*V_DOWN), vfan(m, 1.12)), E_SNAP)
    c.key(0.95, P_(dangle, W(*V_UP), vfan(m, 0.95)), E_IO)
    c.key(1.2, P_(dangle, W(*V_REST)), E_IO)

    c = clip("hover", 1.0, loop=True)
    c.key(0, P_(dangle, W(*V_UP), {"@root": [0, -0.8, 0]}))
    c.key(0.5, P_(dangle, W(*V_DOWN), {"@root": [0, 0.8, 0]}), E_IO)
    c.key(1.0, P_(dangle, W(*V_UP), {"@root": [0, -0.8, 0]}), E_IO)
    # The feathers lag the stroke: bent back on the way down, trailing on the way up.
    flex(c, VFEATHERS, 9.0, 0.08, 0.012, FLIGHT + COVERTS)

    c = clip("dive", 1.5)
    stoop = P_({"body": [55, 0, 0], "head": [-40, 0, 0], "right_leg": [30, 0, 0], "left_leg": [26, 0, 0], "right_arm": [-150, 0, 6],
                "left_arm": [20, 0, -10]}, W(*V_FOLD), vfan(m, 0.5))
    c.key(0, P_(dangle, W(*V_REST)))
    c.key(0.35, stoop, E_OUT)
    c.key(0.75, P_(stoop, {"body": [70, 0, 0], "head": [-55, 0, 0]}), E_IO)
    impact = P_({"@root": [0, -3.2, 0], "body": [26, 0, 0], "head": [6, 0, 0], "right_leg": [-40, 0, 0], "left_leg": [-20, 0, 0],
                 "right_shin": [70, 0, 0], "left_shin": [50, 0, 0], "right_arm": [-40, 0, 10], "left_arm": [-30, 0, -40]},
                W(*V_OPEN), vfan(m, 1.2))
    c.key(0.9, impact, E_SNAP)
    c.key(1.1, P_(impact, {"@root": [0, -2.8, 0]}))
    c.key(1.5, {}, E_IO)

    c = clip("land", 1.0)
    c.key(0, P_(dangle, W(*V_UP)))
    c.key(0.15, P_({"@root": [0, -2.4, 0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0], "right_shin": [52, 0, 0], "left_shin": [52, 0, 0],
                    "body": [20, 0, 0], "right_arm": [-20, 0, 20], "left_arm": [-20, 0, -20]}, W(*V_OPEN), vfan(m, 1.1)), E_SNAP)
    c.key(0.5, P_({"@root": [0, -0.6, 0], "body": [6, 0, 0]}, W(*V_REST)), E_OUT)
    c.key(1.0, {}, E_IO)

    # wing_buffet: drawn back and up, then snapped forward in one gale; the fans open on the stroke.
    c = clip("wing_buffet", 1.4)
    c.key(0, {})
    c.key(0.4, P_({"body": [-10, 0, 0], "head": [-8, 0, 0], "right_arm": [-10, 0, 30], "left_arm": [-10, 0, -30]},
                  W((0, 62, 40), (0, 66, 10)), vfan(m, 0.7)), E_OUT)
    c.key(0.62, P_({"body": [16, 0, 0], "head": [6, 0, 0], "right_arm": [-40, 0, 40], "left_arm": [-40, 0, -40], "@root": [0, -0.8, -1]},
                   W(*V_FORWARD), vfan(m, 1.25, 22)), E_SNAP)
    c.key(0.9, P_({"body": [10, 0, 0]}, W((0, -6, 18), (0, -2, -10)), vfan(m, 1.15, 14)), E_OUT)
    c.key(1.4, {}, E_IO)

    # feather_storm: the wings spread wide and every feather fans and shivers as they fly.
    c = clip("feather_storm", 2.0)
    storm = P_({"body": [-8, 0, 0], "head": [-12, 0, 0], "right_arm": [-30, 0, 70], "left_arm": [-30, 0, -70]},
               W((0, 4, 30), (0, 8, -2)), vfan(m, 1.3))
    c.key(0, {})
    c.key(0.5, storm, E_BACK)
    c.key(1.6, P_(storm, {"body": [-10, 0, 0]}), E_IO)
    c.key(2.0, {}, E_IO)
    ruffle(c, VFEATHERS, 14.0, 8, 43, t0=0.4, t1=1.8)

    # transform: (6 s) he staggers, kneels, the shadows wrap round him; he rises arching into the light, which
    # peaks at 4.5 s (the model swaps); held.
    c = clip("transform", 6.0, hold=True)
    c.key(0, {})
    c.key(0.8, {"body": [14, 0, 6], "head": [20, 0, 0], "left_arm": [-60, 0, -10], "left_forearm": [-50, 0, 0], "right_arm": [-10, 0, 8]}, E_OUT)
    c.key(2.2, P_(kneel(30), W(*V_WRAP), vfan(m, 0.8, 30)), E_IO)
    c.key(3.2, P_(kneel(24), {"right_arm": [-40, 0, 40], "left_arm": [-40, 0, -40]}, W(*V_WRAP), vfan(m, 0.8, 30)), E_IO)
    peak = P_({"body": [-20, 0, 0], "head": [-36, 0, 0], "right_arm": [-40, 0, 110], "left_arm": [-40, 0, -110], "@root": [0, 1.5, 0],
               "right_leg": [6, 0, 6], "left_leg": [6, 0, -6], "%shadow_wings": [1.35, 1.35, 1.35]}, W((0, 0, 30), (0, 6, -4)), vfan(m, 1.35))
    c.key(4.5, peak, E_IO)
    c.key(6.0, P_(peak, {"%shadow_wings": [1.45, 1.45, 1.45]}))
    c.layer("body", lambda t: [1.4 * math.sin(t * 70) * (1 if 3.4 < t < 4.6 else 0), 0, 0])
    c.layer("head", lambda t: [0, 1.8 * math.sin(t * 83) * (1 if 3.4 < t < 4.6 else 0), 0])

    # death: (8 s, held) struck, staggers, falls to his knees, sags forward; the shadows fade away.
    c = clip("death", 8.0, hold=True)
    c.key(0, {})
    c.key(0.3, {"body": [-16, 0, 0], "head": [-24, 0, 0], "right_arm": [-20, 0, 40], "left_arm": [-20, 0, -40], "@root": [0, 0, 2]}, E_SNAP)
    c.key(1.4, {"body": [10, 10, 0], "head": [14, 0, 0], "right_arm": [0, 0, 10], "left_arm": [-30, 0, -10], "right_leg": [-12, 0, 0],
                "left_leg": [10, 0, 0], "@root": [0, -0.6, 1]}, E_IO)
    k2 = P_(kneel(34), {"right_arm": [6, 0, 8], "left_arm": [4, 0, -8], "left_forearm": [0, 0, 0], "body": [26, 0, 0]})
    c.key(2.6, k2, E_IN)
    c.key(4.5, P_(k2, {"body": [36, 0, 0], "head": [40, 0, 0], "%shadow_wings": [0.8, 0.8, 0.8]}, W(*V_FOLD)), E_IO)
    c.key(6.5, P_(k2, {"body": [40, 0, 0], "head": [44, 0, 0], "%shadow_wings": [0.02, 0.02, 0.02]}, W(*V_FOLD)), E_IN)
    c.key(8.0, P_(k2, {"body": [40, 0, 0], "head": [44, 0, 0], "%shadow_wings": [0.02, 0.02, 0.02]}, W(*V_FOLD)))

    for cl in clips:
        cl.done()
    return f


HIT_TIMES = {"blade_combo_1": 0.35, "blade_combo_2": 0.35, "blade_combo_3": 0.35, "forehead_touch": 1.2, "lance_throw": 0.55,
             "lance_thrust": 0.45, "lance_sweep": 0.6, "dive": 0.9, "transform": 4.5, "death": 8.0, "smite": 0.8,
             "wing_buffet": 0.62, "feather_storm": 1.0, "intro": 0.7}
WINGLESS = ("walk", "run", "intro", "transition", "blade_combo_1", "blade_combo_2", "blade_combo_3", "forehead_touch", "smite",
            "parried", "ask_yes", "command", "summon_host")


def preview_hidden(clip):
    """What the renderer would hide for a preview of `clip`: the weapon not in use, the wings in phases I-II."""
    out = ["blade"] if clip.startswith("lance") or clip in ("command", "takeoff", "hover", "dive", "land", "feather_storm",
                                                          "wing_buffet", "transform") else ["lance"]
    if clip in WINGLESS:
        out.append("shadow_wings")
    return out


def open_pose(m):
    """Test pose: the shadow wings spread wide (for previews)."""
    t = {}
    for side, s in (("r", -1), ("l", 1)):
        t[f"wing_{side}1"] = mirror_rot((0, 8, 22), s)
        t[f"wing_{side}2"] = mirror_rot((0, 14, -6), s)
    return delta_pose(m, t)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=4100)
    m.rig.write(GEO + "michael.geo.json")
    save(t, "entity", "michael")
    save(g, "entity", "michael_glowmask")
    write_compact(anims(), ANIM + "michael.animation.json")


if __name__ == "__main__":
    generate()
