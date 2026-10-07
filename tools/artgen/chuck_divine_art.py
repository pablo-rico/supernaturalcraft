"""Chuck as the light: the Author's divine form (chapters 3-5).

Built 4 blocks (64 px) tall and drawn x ChuckGeometry.DIVINE_SCALE (3.5), so some 14 blocks in game:

  * the FIGURE -- a luminous Chuck: his head with its messy hair and short beard in gold light, white
    eyes; an open coat of light falling from his shoulders in panels and folds, tattering at the hem into
    ribbons; sleeves without hands (his hands float apart: author_hand), their cuffs streaming light; and
    a rift in his chest where the heart shows;
  * the CORE (bone `core`) -- that heart: a white star in a gold cage, at ChuckGeometry.CORE_Y;
  * the HALO (bone `halo`, behind his head) -- a typewriter's type-basket: 24 keycaps `key_00..key_23`
    on spokes round a hoop, lettered THE END IS ONLY THE BEGINNING;
  * the 4 RINGS -- sacred geometry: `tilt_r` (pivot at the core, no rest rotation) -> `ring_r` (a circle
    of engraved gold segments, with an inner rail and the triangle inscribed through its nodes) ->
    `ring_r_node_n`, the weak points, at local (R cos(n*120), 0, -R sin(n*120)), R = RING_RADIUS*16/3.5 px;
  * pages and letters drifting round him.

The renderer turns `halo`, `tilt_*`, `ring_*`, nodes, keys and `core` (ChuckBones.PROCEDURAL); no clip keys
them. The rings and core hang from `orbit`, which is not under `root`: clips may only scale it.
"""

import math
import random

from animkit import AnimFile
from chuck_art import ANIM, GEO, Model, composite, cracks_layer, h01, solid
from common import save
from pixelkit import Ramp, fbm, hexc, mix, shade

# --- contract numbers (mirror ChuckGeometry / ChuckBones) ----------------------------------------------
DIVINE_SCALE = 3.5
CORE_Y_BLOCKS = 8.5
RING_RADIUS_BLOCKS = (4.5, 5.75, 7.0, 8.25)
TILT_X = (90, 62, -62, 20)
TILT_Y = (0, 40, -40, 90)
SPIN = (0.030, -0.022, 0.017, -0.012)
HALO_SPIN = 0.008
RINGS, NODES, KEYS = 4, 3, 24

PX = 16 / DIVINE_SCALE
CORE = CORE_Y_BLOCKS * PX                     # 38.857 px
RADII = [r * PX for r in RING_RADIUS_BLOCKS]  # 20.57, 26.29, 32.0, 37.71 px
HALO_C = (0.0, 57.0, 7.5)
HALO_R = 12.5
HALO_TEXT = "THEENDISONLYTHEBEGINNING"


def node_pos(r, n):
    a = math.radians(n * 120)
    return (RADII[r] * math.cos(a), CORE, -RADII[r] * math.sin(a))


# --- palette: white -> gold -> pale blue ----------------------------------------------------------------
SKIN = Ramp("#c69c58", "#e0bd7c", "#f1d9a4", "#fbecc9", "#fff7e6", "#ffffff")
HAIR = Ramp("#5e3a14", "#80531f", "#a8742f", "#cf9c4c", "#ecc77f", "#fff0c4")
ROBE = Ramp("#7f97c0", "#a9bfdf", "#cddcf0", "#e7eff9", "#f7faff", "#ffffff")
GOLD = Ramp("#7d5212", "#a8741f", "#d29e36", "#efc65a", "#fde28e", "#fff6d2")
BLUE = Ramp("#3f6fb8", "#5f92d6", "#86b4ec", "#b0d2fa", "#d6eaff", "#f2f9ff")
WHITE = hexc("#ffffff")
EYE = hexc("#f4fbff")
INK = hexc("#2a3350")

# --- a 5x7 pixel font for keycaps and floating letters ------------------------------------------------
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10000", "10011", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
}


def glyph_bit(letter, x, y, w, h):
    """Whether texel (x, y) of a w x h face lies on `letter`, drawn centred in a 5x7 cell."""
    rows = FONT.get(letter)
    if not rows:
        return False
    s = max(1, min(w // 7, h // 9))
    gx, gy = (w - 5 * s) // 2, (h - 7 * s) // 2
    cx, cy = (x - gx), (y - gy)
    if not (0 <= cx < 5 * s and 0 <= cy < 7 * s):
        return False
    return rows[cy // s][cx // s] == "1"


# --- materials -------------------------------------------------------------------------------------------

def light(ramp, seed, base=3.6, amp=0.8, scale=6.0):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 9, y, seed, 96, 96, 2, scale)
        return ramp[base + (n - 0.5) * amp * 2]
    return m


def robe(seed, hem_gold=True, folds=7.0):
    """Cloth of light: soft folds running down, a gold border at the hem and the edges."""
    def m(face, x, y, w, h):
        if face in ("up", "down"):
            return ROBE[4]
        n = fbm(x, y + len(face) * 7, seed, 96, 96, 2, 7.0)
        fold = math.sin(x / folds * 2 * math.pi + seed) * 0.7
        c = ROBE[3.3 + (n - 0.5) * 0.8 + fold]
        if hem_gold and y >= h - 2:
            c = GOLD[4] if y == h - 1 else GOLD[3]
        # Script stitched in light down the panels.
        if h > 20 and (x * 7 + y * 3 + seed) % 23 == 0:
            c = BLUE[3]
        return c
    return m


def robe_glow(f=0.82):
    # Additive in game: about two thirds of the albedo keeps the folds readable instead of a white blob.
    def g(face, x, y, w, h, c):
        return shade(c, f * 0.66)
    return g


def full_glow(face, x, y, w, h, c):
    return c


def gold_mat(seed, engraved=False, letters=False):
    rng = random.Random(seed)
    cells = {}

    def m(face, x, y, w, h):
        n = fbm(x * 2, y * 2 + len(face), seed, 96, 96, 2, 8.0)
        c = GOLD[3.2 + (n - 0.5) * 1.6]
        if y == 0 or x == 0:
            c = GOLD[5]
        elif y == h - 1 or x == w - 1:
            c = GOLD[1]
        if engraved and face in ("east", "west", "north", "south") and 1 <= y < h - 1 and w > 3:
            cell = (face, x // 3)
            if cell not in cells:
                cells[cell] = rng.getrandbits(9)
            bits, lx, ly = cells[cell], x % 3, (y - 1) % 3
            if lx < 2 and ly < 2 and (bits >> (ly * 2 + lx)) & 1:
                c = BLUE[4]
        return c
    return m


def gold_glow(face, x, y, w, h, c):
    # The engraving burns pale blue; the metal glows warm, a little under full.
    if c[2] > c[0]:
        return BLUE[5]
    return shade(c, 0.55)


# --- the figure -------------------------------------------------------------------------------------------

class Divine(Model):
    def __init__(self):
        super().__init__("chuck_divine", 1024, 1024)


def rig():
    m = Divine()
    m.bone("root", (0, 0, 0))
    body = m.bone("body", (0, 0, 0), "root")
    m.bone("waist", (0, 28, 0), body.name)
    m.bone("chest", (0, 36, 0), "waist")
    m.bone("head", (0, 51, 0), "chest")
    m.bone("jaw", (0, 55, -2), "head")
    build_torso(m)
    build_head(m)
    build_halo(m)
    build_arms(m)
    build_coat(m)
    build_drift(m)
    build_orbit(m)
    return m


def build_torso(m):
    sk = light(SKIN, 3, 3.9, 0.6)
    rb = robe(5, hem_gold=False)
    g = robe_glow(0.85)

    def rift(face, x, y, w, h):
        """The light-body's skin round the rift: brighter towards the hole, with rays cracking out."""
        c = rb(face, x, y, w, h)
        return c

    # Abdomen and the inner robe that falls from it to the hem (he has no legs: he is the light).
    m.cube("waist", (-6.5, 28, -4), (13, 8, 8), rb, g, density=1, tag="abdomen")
    m.cube("waist", (-6.5, 16, -3.75), (13, 12, 7.5), robe(7, hem_gold=False), g, tag="skirt_upper")
    m.cube("waist", (-7.5, 3, -4.5), (15, 13, 9), robe(8), g, tag="skirt_lower")
    # Chest: built round the rift where the core shows, front and centre at CORE.
    chest = "chest"
    m.cube(chest, (-8, 36, 1.5), (16, 14, 3), rb, g, tag="chest_back")
    m.cube(chest, (-8, 42.5, -4.5), (16, 7.5, 6), rb, g, tag="chest_top")
    for s in (-1, 1):
        x0 = 3.5 if s > 0 else -8
        m.cube(chest, (x0, 36, -4.5), (4.5, 6.5, 6), rb, g, tag="chest_side")
    # Rays of the rift: thin gold-white blades splitting the chest round the heart.
    for k in range(8):
        a = k * 45 + 22.5
        m.cube(chest, (-0.5, CORE - 0.5, -4.8), (6.5, 1, 0.5), solid(GOLD[5]), full_glow, density=2, tag="rift_ray",
               rotation=(0, 0, a), pivot=(0, CORE, -4.6))
    # Neck.
    m.cube(chest, (-2.5, 49.5, -2.5), (5, 2.5, 5), sk, robe_glow(0.8), tag="neck")
    # The inner garment's collar: a band of gold.
    m.cube(chest, (-4, 48.5, -4.75), (8, 1.5, 1), gold_mat(11, True), gold_glow, density=2, tag="collar_band")


def build_head(m):
    sk = light(SKIN, 13, 3.8, 0.5)
    hr = light(HAIR, 14, 2.6, 1.2, 10.0)
    head = "head"
    W = 10  # px; face 20x22 texels

    def face_mat(face, x, y, w, h):
        c = sk(face, x, y, w, h)
        if face == "north":
            # Brows: straight, a little heavy; eyes: white slits of light under tired lids.
            if y in (7, 8) and (2 <= x <= 7 or 12 <= x <= 17):
                return HAIR[1] if y == 7 else mix(HAIR[2], SKIN[2], 0.5)
            if y == 9 and (3 <= x <= 7 or 12 <= x <= 16):
                return SKIN[1]  # the lid's shadow
            if y == 10 and (3 <= x <= 7 or 12 <= x <= 16):
                return EYE
            if y == 11 and (4 <= x <= 6 or 13 <= x <= 15):
                return mix(SKIN[2], BLUE[3], 0.4)  # tired, lit from below
            if 9 <= x <= 10 and 8 <= y <= 14:
                return SKIN[5]  # bridge of the nose
            if y < 4:
                return hr(face, x, y, w, h)
            if 12 <= y and (x < 2 or x > 17):
                return hr(face, x, y, w, h)
        elif face in ("east", "west"):
            fx = x if face == "west" else w - 1 - x
            if y < 6 or fx > 12 or (y < 10 and fx > 9):
                return hr(face, x, y, w, h)
            if y >= 13 and fx < 12:
                return hr(face, x, y, w, h)
        elif face in ("up", "south"):
            return hr(face, x, y, w, h)
        return c

    def face_glow(face, x, y, w, h, c):
        if c == EYE:
            return WHITE
        return shade(c, 0.72)
    m.cube(head, (-5, 52, -5), (W, 11, 10), face_mat, face_glow, density=2, tag="head")
    # Nose, ears.
    m.cube(head, (-1, 54.5, -6), (2, 3.5, 1), lambda f, x, y, w, h: SKIN[4] if f != "down" else SKIN[1], robe_glow(0.75),
           density=2, tag="nose")
    for s in (-1, 1):
        m.cube(head, (5 if s > 0 else -5.5, 55.5, -1), (0.5, 3, 2), solid(SKIN[3]), robe_glow(0.75), density=2, tag="ear")
    # The beard: a short scruff of gold over the jaw, and the moustache. The chin part is `jaw`.
    bd = light(HAIR, 15, 3.0, 1.4, 12.0)

    def beard(face, x, y, w, h):
        if face in ("up", "south"):
            return None
        if face == "north" and y < 3 and 3 <= x <= 16:
            return None
        if face in ("east", "west"):
            fx = x if face == "west" else w - 1 - x
            if fx > 13:
                return None
        n = h01("dbeard", face, x, y)
        c = bd(face, x, y, w, h)
        if n < 0.15:
            c = HAIR[5]
        return c
    m.cube(head, (-5, 54, -5), (10, 2.5, 10), beard, robe_glow(0.8), inflate=0.3, density=2, tag="beard")
    m.cube(head, (-2.5, 56.0, -5.6), (5, 1, 1), bd, robe_glow(0.8), density=2, tag="moustache")
    m.cube("jaw", (-4.5, 51.0, -5.3), (9, 3, 6), lambda f, x, y, w, h: None if f == "up" else bd(f, x, y, w, h),
           robe_glow(0.8), inflate=0.2, density=2, tag="chin")
    # Hair: a messy crown that flickers up like flame, swept back.
    m.cube(head, (-5, 60, -5), (10, 3.5, 10), lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 3 + (x % 3))
           else hr(f, x, y, w, h), robe_glow(0.8), inflate=0.4, density=2, tag="crown")
    m.cube(head, (-5, 54, 1.5), (10, 7, 4), lambda f, x, y, w, h: None if f in ("north", "down") else hr(f, x, y, w, h),
           robe_glow(0.8), inflate=0.35, density=2, tag="hair_back")
    rng = random.Random(21)
    for k in range(9):
        x = -4 + k
        z = rng.uniform(-3.5, 2.5)
        b = m.bone(f"lock_{k}", (x, 63, z), head)

        def lock(face, x_, y, w, h):
            t = y / max(1, h - 1)
            return mix(HAIR[5], HAIR[2], t)
        m.cube(b, (x - 0.75, 63, z - 0.75), (1.5, rng.choice((2.5, 3, 3.5)), 1.5), lock, full_glow, density=2,
               tag="lock", rotation=(rng.uniform(15, 40), rng.uniform(-30, 30), (x * -5) + rng.uniform(-10, 10)),
               pivot=(x, 63, z))


def build_halo(m):
    """A typewriter's type-basket behind his head: hoop, hub, spokes (typebars) and 24 keycaps."""
    cx, cy, cz = HALO_C
    halo = m.bone("halo", HALO_C, "head")
    gm = gold_mat(31)
    for k in range(32):  # the hoop
        m.cube(halo, (cx - 1.5, cy + HALO_R - 0.5, cz + 1.0), (3, 1, 1), gm, gold_glow, density=2, tag="hoop",
               rotation=(0, 0, k * 360 / 32), pivot=(cx, cy, cz + 1.5))
    for k in range(12):  # the hub
        m.cube(halo, (cx - 0.75, cy + 3.5, cz + 1.0), (1.5, 1, 1), gm, gold_glow, density=2, tag="hub",
               rotation=(0, 0, k * 30), pivot=(cx, cy, cz + 1.5))
    for i in range(KEYS):  # the typebars
        m.cube(halo, (cx - 0.25, cy + 4, cz + 1.25), (0.5, HALO_R - 5.5, 0.5), solid(GOLD[4]), full_glow, density=2,
               tag="typebar", rotation=(0, 0, -i * 360 / KEYS), pivot=(cx, cy, cz + 1.5))
    for i in range(KEYS):
        phi = math.radians(i * 360 / KEYS)
        # Clockwise from the top as seen from the front: +X (his left) is the viewer's right.
        kx, ky = cx + HALO_R * math.sin(phi), cy + HALO_R * math.cos(phi)
        key = m.bone(f"key_{i:02d}", (kx, ky, cz), halo.name)
        letter = HALO_TEXT[i]

        def cap(face, x, y, w, h, letter=letter):
            r = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2)
            if face in ("north",):
                if r > w / 2:
                    return None
                if r > w / 2 - 1.3:
                    return hexc("#e9eef2")  # chrome rim
                if glyph_bit(letter, x, y, w, h):
                    return INK
                return mix(hexc("#fff8e6"), hexc("#f3e2bc"), r / (w / 2))
            return hexc("#d6dde3")

        def cap_glow(face, x, y, w, h, c):
            if c == INK:
                return BLUE[2]  # the letter burns blue
            return shade(c, 0.9)
        m.cube(key, (kx - 1.5, ky - 1.5, cz - 0.5), (3, 3, 1), cap, cap_glow, density=4, tag="keycap")
        m.cube(key, (kx - 0.5, ky - 0.5, cz + 0.5), (1, 1, 1), solid(hexc("#c9d1d8")), 0.7, density=2, tag="stem")


def build_arms(m):
    rb = robe(41)
    g = robe_glow(0.85)
    for side, s in (("r", -1), ("l", 1)):
        arm = m.bone(f"arm_{side}", (9 * s, 48.5, 0), "chest", rotation=(-6, 0, -16 * s))
        fore = m.bone(f"forearm_{side}", (10 * s, 37, 0), arm.name, rotation=(-28, 0, 0))
        cuff = m.bone(f"cuff_{side}", (10 * s, 28, 0), fore.name)
        x0 = 9 * s - 2.75
        m.cube(arm, (x0, 37, -2.75), (5.5, 13, 5.5), none_on_down(rb), g, inflate=0.2, tag="sleeve")
        m.cube(arm, (x0 - 0.5, 46, -3.25), (6.5, 4, 6.5), robe(42, hem_gold=False), g, tag="shoulder")
        m.cube(fore, (10 * s - 3, 29, -3), (6, 8.5, 6), none_on_ud(rb), g, inflate=0.3, tag="sleeve")
        # The sleeve flares into a wide gold-hemmed cuff, empty: his hands are elsewhere.
        def cuff_m(face, x, y, w, h):
            if face == "up":
                return None
            if face == "down":
                r = max(abs(x - (w - 1) / 2), abs(y - (h - 1) / 2))
                return WHITE if r < w / 2 - 2 else GOLD[4]  # light pours out of the sleeve
            return GOLD[4] if y >= h - 2 else ROBE[4]
        m.cube(cuff, (10 * s - 4, 26, -4), (8, 4, 8), cuff_m, full_glow, density=2, tag="cuff")
        # Streams of light pouring from the cuff, in two segments each.
        for k, (dx, dz) in enumerate(((-2, -1.5), (1.5, -2), (0, 2))):
            px, pz = 10 * s + dx, dz
            b1 = m.bone(f"stream_{side}{k}", (px, 26, pz), cuff.name, rotation=(8 * (k - 1), 0, 6 * s))
            m.cube(b1, (px - 0.75, 19, pz - 0.75), (1.5, 7, 1.5), stream_mat(0), full_glow, density=2, tag="stream")
            b2 = m.bone(f"stream_{side}{k}_tip", (px, 19, pz), b1.name, rotation=(6, 0, 4 * s))
            m.cube(b2, (px - 0.5, 12, pz - 0.5), (1, 7, 1), stream_mat(1), full_glow, density=2, tag="stream")


def stream_mat(seg):
    def m(face, x, y, w, h):
        t = (y + seg * h) / (2 * h)
        if seg == 1 and y > h - 3 and (x + y) % 2:
            return None
        return mix(WHITE, BLUE[3], t)
    return m


def none_on_down(mat):
    return lambda f, x, y, w, h: None if f == "down" else mat(f, x, y, w, h)


def none_on_ud(mat):
    return lambda f, x, y, w, h: None if f in ("up", "down") else mat(f, x, y, w, h)


def build_coat(m):
    """The open coat of light: front panels, a back, a standing collar, folds, and a hem of ribbons."""
    g = robe_glow(0.84)
    coat_r = m.bone("coat_r", (-5, 50, -4.5), "chest")
    coat_l = m.bone("coat_l", (5, 50, -4.5), "chest")
    back = m.bone("coat_back", (0, 50, 4.5), "chest")
    collar = m.bone("collar", (0, 50, 3.5), "chest")
    for b, s in ((coat_r, -1), (coat_l, 1)):
        # The front panel, splaying open below the chest.
        def panel(face, x, y, w, h, s=s):
            c = robe(51 + s)(face, x, y, w, h)
            inner = (x == (w - 1 if s < 0 else 0)) if face == "north" else False
            if inner:
                c = GOLD[4]  # a gold edge down the opening
            return c
        x0 = 2 if s > 0 else -8.5
        m.cube(b, (x0, 4, -5.5), (6.5, 46, 1), panel, g, tag="panel", rotation=(0, 10 * s, 5 * s), pivot=(5 * s, 50, -4.5))
        # Lapel: folded back, gold-edged.
        m.cube(b, (2 * s - (3 if s < 0 else 0), 38, -6.2), (3, 11, 0.5), lambda f, x, y, w, h: GOLD[3] if x in (0, w - 1)
               else ROBE[5], full_glow, density=2, tag="lapel", rotation=(0, 14 * s, -12 * s), pivot=(2 * s, 49, -5.5))
        # Side panel, flaring out towards the hem.
        m.cube(b, (8 * s - 0.5, 4, -5), (1, 45, 9.5), robe(54 + s), g, tag="side", rotation=(0, 0, 4 * s),
               pivot=(8 * s, 49, 0))
        # Folds: raised strips running down the panel.
        for k in range(3):
            fx = (3 + k * 1.8) * s
            m.cube(b, (fx - 0.5, 5, -6.3), (1, 38 - k * 4, 0.5), solid(ROBE[5] if k % 2 else ROBE[2]), g, density=2,
                   tag="fold", rotation=(0, 10 * s, 5 * s), pivot=(5 * s, 50, -4.5))
    m.cube(back, (-8.5, 3, 4.5), (17, 47, 1.5), robe(57), g, tag="back", rotation=(-3, 0, 0), pivot=(0, 50, 4.5))
    for k in range(5):
        fx = -6 + k * 3
        m.cube(back, (fx - 0.5, 4, 6), (1, 44, 0.5), solid(ROBE[2] if k % 2 else ROBE[5]), g, density=2, tag="fold",
               rotation=(-3, 0, 0), pivot=(0, 50, 4.5))
    # A high collar standing behind the neck, its wings turned forward.
    m.cube(collar, (-6, 48.5, 4), (12, 6, 1.5), lambda f, x, y, w, h: GOLD[4] if y == 0 else ROBE[4], g, tag="collar",
           rotation=(-14, 0, 0), pivot=(0, 49, 4.5))
    for s in (-1, 1):
        m.cube(collar, (6 * s - (4 if s < 0 else 0), 48.5, 1.5), (4, 5, 1), lambda f, x, y, w, h: GOLD[4] if y == 0 else ROBE[4],
               g, tag="collar_wing", rotation=(0, -55 * s, 0), pivot=(6 * s, 49, 3))
    # The hem tatters into ribbons of light, each in two segments, all the way round.
    rng = random.Random(61)
    pts = [(-8, -5.5), (-5.5, -6), (-3.5, -6.2), (3.5, -6.2), (5.5, -6), (8, -5.5), (9, -1), (9, 3), (-9, -1), (-9, 3),
           (-6, 6), (-2, 6.2), (2, 6.2), (6, 6)]
    for i, (x, z) in enumerate(pts):
        par = "coat_r" if x < 0 and z < 0 else "coat_l" if x > 0 and z < 0 else "coat_back"
        if abs(x) > 8.5 and z > -2:
            par = "coat_r" if x < 0 else "coat_l"
        b1 = m.bone(f"hem_{i:02d}", (x, 4, z), par, rotation=(rng.uniform(-8, 8), 0, rng.uniform(-6, 6)))
        L1 = rng.choice((3, 3.5, 4))
        w = rng.choice((1.5, 2))
        m.cube(b1, (x - w / 2, 4 - L1, z - 0.25), (w, L1, 0.5), ribbon(0), full_glow, density=2, tag="ribbon")
        b2 = m.bone(f"hem_{i:02d}_tip", (x, 4 - L1, z), b1.name, rotation=(rng.uniform(-10, 10), 0, rng.uniform(-8, 8)))
        L2 = rng.choice((2.5, 3, 3.5))
        m.cube(b2, (x - w / 2 + 0.25, 4 - L1 - L2, z - 0.25), (w - 0.5, L2, 0.5), ribbon(1), full_glow, density=2,
               tag="ribbon")


def ribbon(seg):
    def m(face, x, y, w, h):
        t = (y + seg * h) / (2 * h)
        if seg == 1 and y >= h - 2 and (x + y) % 2 == 0:
            return None
        c = mix(ROBE[5], BLUE[3], t)
        if seg == 0 and y == 0:
            c = GOLD[4]
        return c
    return m


def build_drift(m):
    """Manuscript pages and loose letters drifting round him."""
    drift = m.bone("drift", (0, 32, 0), "root")
    rng = random.Random(71)
    for i in range(8):
        a = math.radians(i * 45 + rng.uniform(-12, 12))
        rr = rng.uniform(13, 17)
        x, z, y = rr * math.cos(a), -rr * math.sin(a), rng.uniform(14, 52)
        b = m.bone(f"page_{i}", (x, y, z), drift.name)

        def page(face, x_, y_, w, h, seed=i):
            if y_ % 3 == 2 and 2 <= x_ < w - 2 and 2 <= y_ < h - 3 and h01("pg", seed, x_ // 3, y_) > 0.25:
                return INK if face == "north" else mix(INK, ROBE[3], 0.5)  # lines of text
            return hexc("#fbf6e8") if face in ("north", "south") else hexc("#e8dcc0")
        m.cube(b, (x - 2.5, y - 3.5, z), (5, 7, 0), page, lambda f, x_, y_, w, h, c: BLUE[3] if c == INK else shade(c, 0.85),
               faces=("north", "south"), density=4, tag="page",
               rotation=(rng.uniform(-25, 25), math.degrees(a) + 90 + rng.uniform(-30, 30), rng.uniform(-20, 20)),
               pivot=(x, y, z))
    letters = "CHUCKWROTEALL"
    for i, ch in enumerate(letters):
        a = math.radians(i * (360 / len(letters)) + 20)
        rr = rng.uniform(10, 20)
        x, z, y = rr * math.cos(a), -rr * math.sin(a), rng.uniform(8, 60)
        b = m.bone(f"glyph_{i:02d}", (x, y, z), drift.name)

        def letter_m(face, x_, y_, w, h, ch=ch):
            return WHITE if glyph_bit(ch, x_, y_, w, h) else None
        m.cube(b, (x - 1.5, y - 2, z), (3, 4, 0), letter_m, lambda f, x_, y_, w, h, c: BLUE[4], faces=("north", "south"),
               density=4, tag="glyph", rotation=(0, math.degrees(a) + 90, rng.uniform(-15, 15)), pivot=(x, y, z))


# --- the core and the rings -------------------------------------------------------------------------------

def build_orbit(m):
    orbit = m.bone("orbit", (0, CORE, 0), None)
    core = m.bone("core", (0, CORE, 0), orbit.name)
    # The heart: a white star in a gold cage, wrapped in pale blue.
    m.cube(core, (-1.5, CORE - 1.5, -1.5), (3, 3, 3), solid(WHITE), full_glow, density=2, tag="core_white",
           rotation=(45, 45, 0), pivot=(0, CORE, 0))
    m.cube(core, (-2.5, CORE - 2.5, -2.5), (5, 5, 5), lambda f, x, y, w, h: GOLD[5] if (x in (0, w - 1) or y in (0, h - 1))
           else None, full_glow, density=2, tag="core_cage", rotation=(0, 45, 0), pivot=(0, CORE, 0))
    m.cube(core, (-2.5, CORE - 2.5, -2.5), (5, 5, 5), lambda f, x, y, w, h: BLUE[5] if (x + y) % 4 == 0 or
           x in (0, w - 1) or y in (0, h - 1) else None, full_glow, density=2, tag="core_cage",
           rotation=(45, 0, 45), pivot=(0, CORE, 0))
    for k in range(6):  # the star's points, in his chest's plane and through it
        rot = [(0, 0, k * 30), (0, 0, k * 30)][0]
        L = 11 if k % 2 == 0 else 7
        m.cube(core, (-L / 2, CORE - 0.5, -0.5), (L, 1, 1), lambda f, x, y, w, h: mix(WHITE, BLUE[4], abs(x - w / 2) / (w / 2)),
               full_glow, density=2, tag="core_ray", rotation=(0, 0, k * 30), pivot=(0, CORE, 0))
    m.cube(core, (-0.5, CORE - 0.5, -5), (1, 1, 10), solid(BLUE[5]), full_glow, density=2, tag="core_ray")

    for r in range(RINGS):
        tilt = m.bone(f"tilt_{r}", (0, CORE, 0), orbit.name)
        ring = m.bone(f"ring_{r}", (0, CORE, 0), tilt.name)
        build_ring(m, r, ring)


RING_SECTION = [(2.0, 1.5), (2.5, 1.5), (2.5, 2.0), (3.0, 2.0)]  # (radial thickness, height) px


def build_ring(m, r, ring):
    R = RADII[r]
    T, H = RING_SECTION[r]
    n = int(round(2 * math.pi * R / 3.0))
    n -= n % 3  # a whole number of segments between nodes
    L = math.ceil((2 * math.pi * R / n + 0.4) * 2) / 2
    gm = gold_mat(100 + r, engraved=True)
    for k in range(n):
        deg = 360.0 * k / n
        # Built on +X (tangent along Z), then turned about the core: +a about Y takes +X to (cos a, 0, -sin a).
        m.cube(ring, (R - T / 2, CORE - H / 2, -L / 2), (T, H, L), gm, gold_glow, density=2, tag="ring_seg",
               rotation=(0, deg, 0), pivot=(0, CORE, 0))
    # An inner rail tied to the band, and the triangle through the nodes.
    Ri = R - T / 2 - 2.0
    ni = n // 2
    Li = math.ceil((2 * math.pi * Ri / ni + 0.4) * 2) / 2
    for k in range(ni):
        m.cube(ring, (Ri - 0.25, CORE - 0.25, -Li / 2), (0.5, 0.5, Li), solid(BLUE[4]), full_glow, density=2,
               tag="rail", rotation=(0, 360.0 * k / ni, 0), pivot=(0, CORE, 0))
    for k in range(0, n, max(3, n // 12)):
        m.cube(ring, (Ri, CORE - 0.25, -0.25), (2.0 + T / 2 - 0.5, 0.5, 0.5), solid(GOLD[5]), full_glow, density=2,
               tag="tie", rotation=(0, 360.0 * k / n + 180.0 / n, 0), pivot=(0, CORE, 0))
    chord = R * math.sqrt(3)
    for k in range(NODES):
        m.cube(ring, (R / 2 - 0.25, CORE - 0.25, -chord / 2), (0.5, 0.5, round(chord * 2) / 2),
               lambda f, x, y, w, h: mix(BLUE[5], GOLD[4], abs(y - h / 2) / (h / 2)) if f in ("east", "west")
               else BLUE[4], full_glow, density=2, tag="chord", rotation=(0, k * 120 + 60, 0), pivot=(0, CORE, 0))
    for nn in range(NODES):
        build_node(m, r, nn, ring)


def build_node(m, r, n, ring):
    x, y, z = node_pos(r, n)
    node = m.bone(f"ring_{r}_node_{n}", (x, y, z), ring.name)
    deg = n * 120
    gm = gold_mat(200 + r * 3 + n)
    # The setting: a gold collar round the band.
    m.cube(node, (x - 3, y - 2, z - 3), (6, 4, 6), lambda f, xx, yy, w, h: gm(f, xx, yy, w, h) if (xx in (0, 1, w - 2, w - 1)
           or yy in (0, h - 1)) else None, gold_glow, density=2, tag="node_setting", rotation=(0, deg + 45, 0), pivot=(x, y, z))

    def gem(face, xx, yy, w, h):
        r_ = max(abs(xx - (w - 1) / 2), abs(yy - (h - 1) / 2)) / (w / 2)
        return mix(WHITE, BLUE[2], r_ ** 1.5)
    m.cube(node, (x - 2.25, y - 2.25, z - 2.25), (4.5, 4.5, 4.5), gem, full_glow, density=2, tag="node_gem",
           rotation=(45, deg, 45), pivot=(x, y, z))
    # A sigil star round it: spikes up, down and out along the ring's normal and its radius.
    m.cube(node, (x - 0.5, y - 5, z - 0.5), (1, 10, 1), lambda f, xx, yy, w, h: mix(WHITE, GOLD[4], abs(yy - h / 2) / (h / 2)),
           full_glow, density=2, tag="node_spike", rotation=(0, deg, 0), pivot=(x, y, z))
    m.cube(node, (x - 4.5, y - 0.5, z - 0.5), (9, 1, 1), lambda f, xx, yy, w, h: mix(WHITE, BLUE[3], abs(xx - w / 2) / (w / 2)),
           full_glow, density=2, tag="node_spike", rotation=(0, deg, 0), pivot=(x, y, z))
    m.cube(node, (x - 3.5, y - 3.5, z), (7, 7, 0), lambda f, xx, yy, w, h: GOLD[5] if abs(abs(xx - (w - 1) / 2) +
           abs(yy - (h - 1) / 2) - (w / 2 - 1)) < 0.8 else None, full_glow, faces=("north", "south"), density=2,
           tag="node_sigil", rotation=(0, deg + 90, 0), pivot=(x, y, z))


# --- animations -----------------------------------------------------------------------------------------

E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"
A = "animation.chuck_divine."
HEMS = [f"hem_{i:02d}" for i in range(14)]


def sway(a, length, amp=1.0, phase=0.0):
    """Ribbons, streams, locks and pages, alive the whole clip (loops cleanly)."""
    for i, h in enumerate(HEMS):
        p = (i * 0.37 + phase) % 1.0
        k = [(0, [0, 0, 0])]
        for j in range(1, 4):
            t = length * j / 4
            s = math.sin(2 * math.pi * (j / 4 + p))
            k.append((t, [10 * amp * s, 0, 6 * amp * math.cos(2 * math.pi * (j / 4 + p))], E_IO))
        k[-1] = (length, [0, 0, 0], E_IO)
        a.rot(h, *k)
        a.rot(h + "_tip", (0, [0, 0, 0]), (length / 2, [14 * amp * math.sin(p * 6.3), 0, 0], E_IO), (length, [0, 0, 0], E_IO))
    for side in ("r", "l"):
        for k in range(3):
            p = k * 0.3
            a.rot(f"stream_{side}{k}", (0, [0, 0, 0]), (length * (0.4 + p / 3), [8 * amp, 0, 5 * amp], E_IO),
                  (length, [0, 0, 0], E_IO))
            a.rot(f"stream_{side}{k}_tip", (0, [0, 0, 0]), (length * 0.5, [-12 * amp, 0, 0], E_IO), (length, [0, 0, 0], E_IO))
    for k in range(9):
        a.rot(f"lock_{k}", (0, [0, 0, 0]), (length * (0.3 + 0.05 * k), [8 * amp, 0, 4 * amp], E_IO), (length, [0, 0, 0], E_IO))


def anims():
    f = AnimFile()

    idle = f.new(A + "idle", 6.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (3.0, [0, 0.8, 0], E_IO), (6.0, [0, 0, 0], E_IO))
    idle.rot("chest", (0, [0, 0, 0]), (3.0, [-2, 0, 0], E_IO), (6.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [4, 0, 0]), (2.0, [6, -6, 2], E_IO), (4.0, [3, 6, -2], E_IO), (6.0, [4, 0, 0], E_IO))
    for s, side in ((-1, "r"), (1, "l")):
        idle.rot(f"arm_{side}", (0, [0, 0, 0]), (3.0, [-4, 0, -6 * s], E_IO), (6.0, [0, 0, 0], E_IO))
        idle.rot(f"forearm_{side}", (0, [0, 0, 0]), (3.0, [-8, 0, 0], E_IO), (6.0, [0, 0, 0], E_IO))
        idle.rot(f"coat_{side}", (0, [0, 0, 0]), (3.0, [-3, 0, 2 * s], E_IO), (6.0, [0, 0, 0], E_IO))
    idle.rot("drift", (0, [0, 0, 0]), (6.0, [0, 60, 0]))
    for i in range(8):
        idle.pos(f"page_{i}", (0, [0, 0, 0]), (1.5 + i * 0.4, [0, 2, 0], E_IO), (6.0, [0, 0, 0], E_IO))
    sway(idle, 6.0)

    drift = f.new(A + "drift", 4.0, loop=True)
    drift.rot("body", (0, [8, 0, 0]), (2.0, [9, 0, 2], E_IO), (4.0, [8, 0, 0], E_IO))
    drift.pos("body", (0, [0, 0.5, 0]), (2.0, [0, 1.2, 0], E_IO), (4.0, [0, 0.5, 0], E_IO))
    drift.rot("coat_back", (0, [16, 0, 0]), (2.0, [20, 0, 0], E_IO), (4.0, [16, 0, 0], E_IO))
    for s, side in ((-1, "r"), (1, "l")):
        drift.rot(f"coat_{side}", (0, [14, 0, 0]), (2.0, [18, 0, 3 * s], E_IO), (4.0, [14, 0, 0], E_IO))
        drift.rot(f"arm_{side}", (0, [12, 0, -8 * s]), (2.0, [14, 0, -10 * s], E_IO), (4.0, [12, 0, -8 * s], E_IO))
    drift.rot("drift", (0, [0, 0, 0]), (4.0, [0, 60, 0]))
    sway(drift, 4.0, 1.6)

    def clip(name, length, hold=False):
        return f.new(A + name, length, hold=hold)

    def arms(a, keys_r, keys_l=None, fore_r=None, fore_l=None):
        a.rot("arm_r", *keys_r)
        a.rot("arm_l", *(keys_l or [(t, [v[0], -v[1], -v[2]], *e) for (t, v, *e) in keys_r]))
        if fore_r:
            a.rot("forearm_r", *fore_r)
            a.rot("forearm_l", *(fore_l or fore_r))

    rev = clip("reveal", 5.0)
    rev.scale("body", (0, [0.2, 0.02, 0.2]), (1.2, [0.6, 1.2, 0.6], E_OUT), (2.4, [1.05, 1.0, 1.05], E_BACK), (3.0, [1, 1, 1], E_IO),
              (5.0, [1, 1, 1]))
    rev.scale("orbit", (0, [0.01, 0.01, 0.01]), (2.0, [0.01, 0.01, 0.01]), (3.6, [1.08, 1.08, 1.08], E_BACK), (4.2, [1, 1, 1], E_IO),
              (5.0, [1, 1, 1]))
    rev.scale("drift", (0, [0, 0, 0]), (3.0, [0, 0, 0]), (4.5, [1, 1, 1], E_OUT), (5.0, [1, 1, 1]))
    arms(rev, [(0, [0, 0, 0]), (2.4, [-10, 0, 20]), (3.4, [-30, 0, 72], E_BACK), (5.0, [0, 0, 0], E_IO)])
    rev.rot("head", (0, [20, 0, 0]), (2.4, [10, 0, 0]), (3.4, [-24, 0, 0], E_BACK), (5.0, [0, 0, 0], E_IO))
    sway(rev, 5.0, 1.4)

    for name, length in (("transform_4", 4.0), ("transform_5", 5.0)):
        t = clip(name, length)
        peak = length * 0.55
        t.rot("chest", (0, [0, 0, 0]), (peak * 0.6, [18, 0, 0], E_OUT), (peak, [-16, 0, 0], E_BACK), (length, [0, 0, 0], E_IO))
        t.rot("head", (0, [0, 0, 0]), (peak * 0.6, [26, 0, 0], E_OUT), (peak, [-40, 0, 0], E_BACK), (length, [0, 0, 0], E_IO))
        arms(t, [(0, [0, 0, 0]), (peak * 0.6, [-50, 0, -30], E_OUT), (peak, [-20, 0, 85], E_BACK), (length, [0, 0, 0], E_IO)],
             fore_r=[(0, [0, 0, 0]), (peak * 0.6, [-50, 0, 0], E_OUT), (peak, [10, 0, 0], E_BACK), (length, [0, 0, 0], E_IO)])
        t.scale("orbit", (0, [1, 1, 1]), (peak * 0.6, [0.9, 0.9, 0.9], E_IO), (peak, [1.15, 1.15, 1.15], E_BACK),
                (length, [1, 1, 1], E_IO))
        t.scale("drift", (0, [1, 1, 1]), (peak, [1.5, 1.5, 1.5], E_OUT), (length, [1, 1, 1], E_IO))
        sway(t, length, 2.0)

    snap = clip("snap", 2.2)
    # The right sleeve sweeps up and out; the snap itself is his hand's (author_hand) — the body echoes it.
    snap.rot("arm_r", (0, [0, 0, 0]), (0.6, [-20, 0, 64], E_OUT), (1.1, [-24, 0, 70]), (1.2, [-16, 0, 60], E_SNAP),
             (2.2, [0, 0, 0], E_IO))
    snap.rot("forearm_r", (0, [0, 0, 0]), (0.6, [-10, 0, 80], E_OUT), (1.1, [-12, 0, 84]), (1.2, [0, 0, 70], E_SNAP),
             (2.2, [0, 0, 0], E_IO))
    snap.rot("head", (0, [0, 0, 0]), (0.6, [6, -14, 0], E_OUT), (1.2, [12, -16, 0], E_SNAP), (2.2, [0, 0, 0], E_IO))
    snap.rot("chest", (0, [0, 0, 0]), (0.6, [0, -10, 0], E_OUT), (1.2, [4, -12, 0], E_SNAP), (2.2, [0, 0, 0], E_IO))
    snap.scale("drift", (0, [1, 1, 1]), (1.15, [1, 1, 1]), (1.3, [1.4, 1.4, 1.4], E_OUT), (2.2, [1, 1, 1], E_IO))

    rain = clip("type_rain", 3.0)
    keys_r, keys_l = [(0, [0, 0, 0]), (0.4, [-60, 0, 14], E_OUT)], [(0, [0, 0, 0]), (0.4, [-60, 0, -14], E_OUT)]
    for k in range(6):
        t0 = 0.6 + k * 0.32
        keys_r += [(t0, [-52, 0, 14], E_SNAP), (t0 + 0.16, [-62, 0, 14], E_OUT)]
        keys_l += [(t0 + 0.16, [-52, 0, -14], E_SNAP), (min(2.9, t0 + 0.32), [-62, 0, -14], E_OUT)]
    keys_r.append((3.0, [0, 0, 0], E_IO))
    keys_l.append((3.0, [0, 0, 0], E_IO))
    rain.rot("arm_r", *keys_r)
    rain.rot("arm_l", *keys_l)
    rain.rot("head", (0, [0, 0, 0]), (0.4, [-20, 0, 0], E_OUT), (2.6, [-24, 0, 0]), (3.0, [0, 0, 0], E_IO))
    sway(rain, 3.0, 1.3)

    sweep = clip("line_sweep", 2.4)
    arms(sweep, [(0, [0, 0, 0]), (0.7, [-80, -50, 0], E_OUT), (1.4, [-80, 60, 0], E_IO), (2.4, [0, 0, 0], E_IO)],
         keys_l=[(0, [0, 0, 0]), (2.4, [0, 0, 0])])
    sweep.rot("chest", (0, [0, 0, 0]), (0.7, [0, 20, 0], E_OUT), (1.4, [0, -24, 0], E_IO), (2.4, [0, 0, 0], E_IO))
    sweep.rot("head", (0, [0, 0, 0]), (0.7, [0, 16, 0], E_OUT), (1.4, [0, -20, 0], E_IO), (2.4, [0, 0, 0], E_IO))
    sway(sweep, 2.4, 1.5)

    bs = clip("backspace", 1.6)
    bs.rot("body", (0, [0, 0, 0]), (0.3, [-8, 0, 0], E_OUT), (0.5, [6, 0, 0], E_SNAP), (1.6, [0, 0, 0], E_IO))
    arms(bs, [(0, [0, 0, 0]), (0.3, [-40, 0, 30], E_OUT), (0.5, [20, 0, 10], E_SNAP), (1.6, [0, 0, 0], E_IO)])
    bs.scale("drift", (0, [1, 1, 1]), (0.5, [0.6, 0.6, 0.6], E_SNAP), (1.6, [1, 1, 1], E_IO))

    rw = clip("rewrite", 2.6)
    rw.rot("arm_l", (0, [0, 0, 0]), (0.5, [-70, 0, -40], E_OUT), (1.0, [-90, -30, -10], E_IO), (1.5, [-70, 30, -50], E_IO),
           (2.0, [-90, 0, -20], E_IO), (2.6, [0, 0, 0], E_IO))
    rw.rot("forearm_l", (0, [0, 0, 0]), (0.5, [-20, 0, 0]), (2.0, [-20, 0, 0]), (2.6, [0, 0, 0], E_IO))
    rw.rot("head", (0, [0, 0, 0]), (0.5, [-10, 8, 0], E_OUT), (2.0, [-6, -8, 0], E_IO), (2.6, [0, 0, 0], E_IO))
    rw.rot("drift", (0, [0, 0, 0]), (2.6, [0, 180, 0], E_IO))
    sway(rw, 2.6, 1.2)

    echo = clip("echo_call", 3.0)
    arms(echo, [(0, [0, 0, 0]), (1.0, [-150, 0, 30], E_OUT), (2.2, [-155, 0, 32]), (3.0, [0, 0, 0], E_IO)])
    echo.rot("head", (0, [0, 0, 0]), (1.0, [-30, 0, 0], E_OUT), (2.2, [-32, 0, 0]), (3.0, [0, 0, 0], E_IO))
    echo.rot("chest", (0, [0, 0, 0]), (1.0, [-10, 0, 0], E_OUT), (2.2, [-10, 0, 0]), (3.0, [0, 0, 0], E_IO))
    sway(echo, 3.0, 1.8)

    nar = clip("narrate", 3.0)
    nar.rot("head", (0, [0, 0, 0]), (0.4, [14, 0, 0], E_OUT), (1.4, [16, 8, 0], E_IO), (2.4, [12, -8, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    nar.rot("jaw", (0, [0, 0, 0]), (0.4, [0, 0, 0]), (0.55, [10, 0, 0]), (0.75, [2, 0, 0]), (0.95, [12, 0, 0]),
            (1.2, [3, 0, 0]), (1.45, [9, 0, 0]), (1.7, [0, 0, 0]), (2.0, [11, 0, 0]), (2.3, [2, 0, 0]), (3.0, [0, 0, 0], E_IO))
    nar.rot("arm_l", (0, [0, 0, 0]), (0.6, [-50, 0, -30], E_OUT), (2.4, [-54, 10, -26]), (3.0, [0, 0, 0], E_IO))
    nar.rot("forearm_l", (0, [0, 0, 0]), (0.6, [-40, 0, 0], E_OUT), (2.4, [-40, 0, 0]), (3.0, [0, 0, 0], E_IO))

    crack = clip("crack", 1.4)
    crack.rot("chest", (0, [0, 0, 0]), (0.1, [-14, 0, 6], E_SNAP), (0.4, [8, 0, -4], E_OUT), (1.4, [0, 0, 0], E_IO))
    crack.rot("head", (0, [0, 0, 0]), (0.1, [18, 0, -8], E_SNAP), (0.4, [-6, 0, 4], E_OUT), (1.4, [0, 0, 0], E_IO))
    crack.scale("body", (0, [1, 1, 1]), (0.08, [1.04, 0.97, 1.04], E_SNAP), (0.2, [0.98, 1.02, 0.98]), (0.5, [1, 1, 1], E_IO))
    arms(crack, [(0, [0, 0, 0]), (0.1, [-30, 0, 40], E_SNAP), (1.4, [0, 0, 0], E_IO)])

    rec = clip("recoil", 2.0)
    rec.pos("body", (0, [0, 0, 0]), (0.2, [0, 0, 4], E_SNAP), (2.0, [0, 0, 0], E_IO))
    rec.rot("body", (0, [0, 0, 0]), (0.2, [-14, 0, 0], E_SNAP), (0.8, [-10, 0, 0]), (2.0, [0, 0, 0], E_IO))
    rec.rot("head", (0, [0, 0, 0]), (0.2, [-30, 0, 10], E_SNAP), (0.8, [-20, 0, 6]), (2.0, [0, 0, 0], E_IO))
    arms(rec, [(0, [0, 0, 0]), (0.2, [-60, 0, 50], E_SNAP), (0.8, [-50, 0, 40]), (2.0, [0, 0, 0], E_IO)])
    rec.scale("orbit", (0, [1, 1, 1]), (0.2, [1.06, 1.06, 1.06], E_SNAP), (1.0, [1, 1, 1], E_IO), (2.0, [1, 1, 1]))
    sway(rec, 2.0, 2.0)

    held = f.new(A + "held", 3.0, hold=True)
    # Held by two brothers and an angel: arms pinned down and back, head bowed, a last strain against them.
    held.rot("arm_r", (0, [0, 0, 0]), (0.6, [24, 0, 6], E_OUT), (1.5, [28, 0, 12], E_IO), (2.0, [20, 0, 4], E_IO), (3.0, [26, 0, 8], E_IO))
    held.rot("arm_l", (0, [0, 0, 0]), (0.6, [24, 0, -6], E_OUT), (1.5, [28, 0, -12], E_IO), (2.0, [20, 0, -4], E_IO), (3.0, [26, 0, -8], E_IO))
    held.rot("forearm_r", (0, [0, 0, 0]), (0.6, [0, 0, 0]), (3.0, [0, 0, 0]))
    held.rot("forearm_l", (0, [0, 0, 0]), (0.6, [0, 0, 0]), (3.0, [0, 0, 0]))
    held.rot("chest", (0, [0, 0, 0]), (0.6, [10, 0, 0], E_OUT), (1.6, [-6, 6, 0], E_IO), (2.2, [12, -4, 0], E_IO), (3.0, [14, 0, 0], E_IO))
    held.rot("head", (0, [0, 0, 0]), (0.6, [20, 0, 0], E_OUT), (1.6, [-10, 10, 0], E_IO), (2.2, [26, -6, 0], E_IO), (3.0, [30, 0, 0], E_IO))
    held.scale("drift", (0, [1, 1, 1]), (3.0, [0.7, 0.7, 0.7], E_IO))

    appr = f.new(A + "approve", 4.0, hold=True)
    # He smiles: the head tilts, a slow nod, the sleeves lower and open.
    appr.rot("head", (0, [0, 0, 0]), (1.0, [-4, 0, 10], E_IO), (2.0, [12, 0, 8], E_IO), (2.8, [0, 0, 8], E_IO), (4.0, [4, 0, 6], E_IO))
    appr.rot("jaw", (0, [0, 0, 0]), (1.2, [-4, 0, 0], E_IO), (4.0, [-4, 0, 0]))
    arms(appr, [(0, [0, 0, 0]), (2.0, [-10, 0, 30], E_IO), (4.0, [-6, 0, 24], E_IO)],
         fore_r=[(0, [0, 0, 0]), (2.0, [-10, 0, 0], E_IO), (4.0, [-6, 0, 0], E_IO)])
    appr.rot("chest", (0, [0, 0, 0]), (2.0, [-4, 0, 0], E_IO), (4.0, [-2, 0, 0], E_IO))
    sway(appr, 4.0, 0.6)

    death = f.new(A + "death", 6.0, hold=True)
    death.scale("orbit", (0, [1, 1, 1]), (1.0, [1.1, 1.1, 1.1], E_OUT), (3.0, [0.01, 0.01, 0.01], E_IN), (6.0, [0.01, 0.01, 0.01]))
    death.scale("drift", (0, [1, 1, 1]), (1.5, [1.8, 1.8, 1.8], E_OUT), (3.0, [0, 0, 0], E_IN), (6.0, [0, 0, 0]))
    death.rot("head", (0, [0, 0, 0]), (1.0, [-30, 0, 0], E_OUT), (3.0, [-40, 0, 0]), (5.0, [20, 0, 0], E_IO), (6.0, [20, 0, 0]))
    arms(death, [(0, [0, 0, 0]), (1.0, [-30, 0, 90], E_OUT), (3.0, [-30, 0, 100]), (5.0, [0, 0, 10], E_IO), (6.0, [0, 0, 10])])
    death.scale("body", (0, [1, 1, 1]), (3.0, [1, 1, 1]), (5.0, [0.6, 1.3, 0.6], E_IN), (6.0, [0.02, 1.6, 0.02], E_IN))
    sway(death, 6.0, 1.5)
    return f


# --- previews ---------------------------------------------------------------------------------------------

def preview_pose(t=0.0):
    """Bone rotations the renderer sets at game time t (Bedrock degrees for preview3d; see the renderer notes)."""
    pose = {}
    for r in range(RINGS):
        pose[f"tilt_{r}"] = {"rotation": [TILT_X[r], -TILT_Y[r], 0]}
        pose[f"ring_{r}"] = {"rotation": [0, math.degrees(SPIN[r] * t), 0]}
    pose["halo"] = {"rotation": [0, 0, math.degrees(HALO_SPIN * t)]}
    return pose


def preview(path, t=0.0, anim=None, at=0.0, scale=3, views=("front", "three_quarter", "side", "back"), glow=True, hidden=()):
    import json
    import preview3d
    m = rig()
    tex, g = m.build(seed=1101)
    if glow:
        tex = composite(tex, g)
    pose = preview_pose(t)
    if anim:
        data = json.loads(json.dumps({"animations": {x.name: x.to_json() for x in anims().anims}}))
        for b, v in preview3d.pose_from(data, A + anim, at).items():
            pose.setdefault(b, {}).update({k: x for k, x in v.items() if x is not None})
    preview3d.render(m.rig, tex, path, pose=pose, hidden=hidden, scale=scale, views=views)


def generate():
    m = rig()
    t, g = m.build(seed=1101)
    m.rig.write(GEO + "chuck_divine.geo.json")
    save(t, "entity", "chuck_divine")
    save(g, "entity", "chuck_divine_glowmask")
    save(cracks_layer(m, 1103, hexc("#1b2440"), 2), "entity", "chuck_divine_cracks")
    anims().write(ANIM + "chuck_divine.animation.json")


if __name__ == "__main__":
    generate()
