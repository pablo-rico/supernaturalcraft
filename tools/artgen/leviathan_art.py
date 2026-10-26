"""The Leviathans (v0.25): the mob, found out, and its severed head that crawls back on its tongue.

A plain man in a faded chambray work shirt (sleeves rolled to the elbow), jeans and work boots; his skin has gone grey and
black goo threads through it like veins. His head is built in three pieces so it can split down the middle of the face:
the back of the skull (on `head`, its inner face the goo-black throat ringed by needle teeth, `teeth`) and the two front
halves `jaw_left`/`jaw_right`, hinged at their outer back corners, so they swing apart like a pair of doors and show their
inner faces: red gums lined with rows of needle teeth (`teeth_left`/`teeth_right`). The long tongue (`tongue`, its tip
`tongue_tip` folded back inside the closed head) lolls out when it opens. `goo_neck` is the black dripping stump the
renderer shows only while the body is headless.

`build_maw` is shared with Dick Roman (`dick_roman_art`), whose maw is the same thing, bigger.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- jaw_left -- teeth_left;  jaw_right -- teeth_right;  teeth;  tongue -- tongue_tip
               -- goo_neck -- goo_drip
               -- right_arm -- right_forearm -- right_hand   (left the same)
       -- right_leg -- right_shin   (left the same)

The head (`leviathan_head`): root -- jaw_left, jaw_right (and their teeth), teeth, tongue -- tongue_tip, goo_stump.
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, save
from michael_art import write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
A = "animation.leviathan."
AH = "animation.leviathan_head."
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#2a2829", "#45403f", "#625a56", "#7d746d", "#978d84", "#b0a69c")
HAIR = Ramp("#120d0a", "#1f1611", "#2d2018", "#3c2b20", "#4c372a", "#5e4535")
GOO = Ramp("#020203", "#060608", "#0c0c10", "#15151b", "#24242d", "#4a4a58")
GUM = Ramp("#1a0508", "#330a10", "#4f1119", "#6b1a24", "#8a2733", "#a8404a")
TOOTH = Ramp("#5e5848", "#8a8470", "#b3ad98", "#d2ccb8", "#e8e3d2", "#fbf8ee")
TONGUE = Ramp("#22070d", "#3e0e18", "#5c1726", "#7a2434", "#963645", "#b0505c")
SHIRT = Ramp("#273444", "#3a4b5f", "#4f6379", "#657b91", "#7d93a8", "#98abbd")
DENIM = Ramp("#0f1622", "#1b2638", "#283850", "#374b68", "#4a6283", "#62799b")
BOOT = Ramp("#170e08", "#28180d", "#3a2414", "#4e321d", "#634127", "#7a5233")
BELT = Ramp("#0d0907", "#1a120d", "#271b13", "#35251a", "#443023", "#553d2d")
EYE_WHITE, IRIS = hexc("#c9c3b8"), hexc("#3b4448")
GLOSS = hexc("#5c6070")


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


# =====================================================================================================
# Materials
# =====================================================================================================

def veined(base, seed, amount=0.015, scale=8.0, centre=0.62):
    """Black goo threading through a material: thin ridges of a noise field."""
    def m(face, x, y, w, h):
        c = base(face, x, y, w, h)
        if c is None:
            return None
        n = fbm(x + len(face) * 11, y, seed, 48, 48, 3, scale)
        d = abs(n - centre)
        if d < amount:
            return GOO[1]
        if d < amount * 1.9:
            return mix(c, GOO[2], 0.45)
        return c
    return m


def skin(seed, base=3.0):
    return H.skin_mat(SKIN, seed, base, 0.7)


def cloth(ramp, seed, base=2.8, seams=()):
    def m(face, x, y, w, h):
        if face == "up":
            return tone(ramp, base + 0.5)
        if face == "down":
            return tone(ramp, base - 1.3)
        n = fbm(x * 2 + len(face) * 7, y * 2, seed, 64, 64, 2, 7.0)
        v = base + 0.4 - 0.7 * y / max(1, h - 1) + (n - 0.5) * 1.1
        if (x + y) % 2 == 0:
            v += 0.15
        if face in ("east", "west"):
            v -= 0.25
        elif face == "south":
            v -= 0.15
        if face in ("north", "south", "east", "west") and y == h - 1:
            v -= 0.7
        return tone(ramp, v)
    return m


def goo_m(seed, base=1.8):
    """Glossy black goo: near-black with a bluish wet highlight."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face) * 3, y * 2, seed, 48, 48, 2, 5.0)
        c = tone(GOO, base + (n - 0.5) * 2.0)
        if face == "up" or (n > 0.7 and (x + y) % 3 == 0):
            c = mix(c, GLOSS, 0.35 if face != "up" else 0.22)
        return c
    return m


def gum_m(seed):
    def m(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed, 48, 48, 2, 6.0)
        c = tone(GUM, 2.4 + (n - 0.5) * 2.2)
        if n < 0.33:
            c = mix(c, GOO[1], 0.6)  # goo seeping from the flesh
        return c
    return m


def tooth_m(face, x, y, w, h):
    return TOOTH[4] if face in ("up", "north", "east") else TOOTH[3]


def tooth_row(c_gum, x, y, w, h, along, period=2, depth=2):
    """Needle teeth painted along an edge: `along` is the texel distance from that edge."""
    if along < depth and (x + y) % period == 0:
        return TOOTH[4] if along == 0 else TOOTH[2]
    return c_gum


# =====================================================================================================
# The split head (shared with Dick Roman)
# =====================================================================================================

def build_maw(m, parent, y0, face_rows, legend, skin_mat, hair_mat, size=8.0, front=4.0, inflate=0.0, tooth=1.2,
              seed=25000, tongue_len=(3.4, 4.0), tongue_bones=("tongue", "tongue_tip"), hinge_parent=None, skull=True,
              density=2, skull_parent=None, skull_inflate=None):
    """The front half of a head as two hinged jaws, the back of the skull with the throat, teeth and a folded tongue.

    `face_rows` (16 x 16 legend characters, x = 0 the viewer's left = the entity's right) paint the face across both jaws.
    Returns the jaw bone names."""
    hs = size / 2
    hp = hinge_parent or parent
    gum = gum_m(seed)
    rows = len(face_rows)

    def face_px(face, col, x, y, w, h):
        ch = face_rows[int(y * rows / h)][col]
        v = legend.get(ch, legend["s"])
        return v(face, x, y, w, h) if callable(v) else v

    for side, s in (("left", 1), ("right", -1)):
        bone = f"jaw_{side}"
        # Hinge at the outer back corner of the half.
        m.bone(bone, (s * hs, y0, 0), hp)
        x0 = 0.0 if s > 0 else -hs

        def jaw(face, x, y, w, h, s=s):
            if face == "north":
                # Columns 0..7 of the face are the entity's right (jaw_right), 8..15 its left.
                cols = len(face_rows[0])
                col = int(x * (cols // 2) / w) + (cols // 2 if s > 0 else 0)
                return face_px(face, col, x, y, w, h)
            if face == "up":
                return hair_mat(face, x, y, w, h)
            if face == "down":
                return skin_mat(face, x, y, w, h)
            # Seen in game, a jaw's side faces come out on the opposite side from their labels here (the inner face is the
            # one labelled east on the left jaw), and the front of each is at the other end.
            inner = "east" if s > 0 else "west"
            outer = "west" if s > 0 else "east"
            fx = w - 1 - x if face == "west" else x      # 0 at the front
            if face == inner:
                c = gum(face, x, y, w, h)
                # Teeth rows along the front edge, the crown and the chin.
                return tooth_row(c, x, y, w, h, min(fx, y, h - 1 - y))
            if face == outer:
                if y < 3 or (fx >= w - 3 and y < 6):
                    return hair_mat(face, x, y, w, h)
                return skin_mat(face, x, y, w, h)
            return gum(face, x, y, w, h)             # south: the cut toward the throat
        m.cube(bone, (x0, y0, -hs), (hs, size, front), jaw, inflate=inflate, density=density, tag=f"jaw_{side}")
        # Needle teeth sticking out of the inner face toward the other half (inside it while the head is shut).
        tb = m.bone(f"teeth_{side}", (s * 0.0, y0, -hs), bone)
        off = 0.0 if s > 0 else 0.5
        tx = (-tooth if s > 0 else 0.0)
        n = int(size)
        for i in range(n):
            yy = y0 + 0.25 + i * (size - 0.75) / max(1, n - 1)
            m.cube(tb, (tx, yy, -hs + 0.2 + off * 0.4), (tooth, 0.5, 0.5), tooth_m, density=2, tag="tooth")
        for i in range(max(2, int(front) - 1)):
            zz = -hs + 1.2 + i * (front - 1.6) / max(1, int(front) - 2) + off * 0.3
            m.cube(tb, (tx, y0 + size - 0.6, zz), (tooth * 0.8, 0.5, 0.5), tooth_m, density=2, tag="tooth")
            m.cube(tb, (tx, y0 + 0.1, zz), (tooth * 0.8, 0.5, 0.5), tooth_m, density=2, tag="tooth")

    if skull:
        def back(face, x, y, w, h):
            if face == "north":
                # The throat: a black hole ringed with red, under the hinge.
                r = math.hypot(x - (w - 1) / 2, (y - (h - 1) / 2) * 1.2) / (w / 2)
                if r < 0.42:
                    return GOO[0]
                if r < 0.75:
                    return mix(gum(face, x, y, w, h), GOO[1], 0.5 - r * 0.5)
                return gum(face, x, y, w, h)
            if face in ("up", "south"):
                return hair_mat(face, x, y, w, h)
            if face == "down":
                return skin_mat(face, x, y, w, h)
            fx = x if face == "west" else w - 1 - x      # 0 at the front of this piece
            if y < 3 or fx > 2 or (y < 6 and fx > 0):
                if not (fx <= 2 and 6 <= y <= 11):
                    return hair_mat(face, x, y, w, h)
            if fx in (1, 2) and 6 <= y <= 10:        # the ear
                c = skin_mat(face, x, y, w, h)
                return shade(c, 0.8) if fx == 1 and 7 <= y <= 9 else shade(c, 1.05)
            return skin_mat(face, x, y, w, h)
        m.cube(skull_parent or parent, (-hs, y0, 0), (size, size, hs), back,
               inflate=inflate if skull_inflate is None else skull_inflate, density=density, tag="skull")

    # The ring of needle teeth round the throat (on the skull's inner face), pointing forward.
    tb = m.bone("teeth", (0, y0 + size / 2, 0), parent)
    cy = y0 + size * 0.48
    for i in range(10):
        a = math.radians(i * 36 + 18)
        r = size * 0.27
        L = tooth * 1.1
        m.cube(tb, (math.cos(a) * r - 0.25, cy + math.sin(a) * r * 0.85 - 0.25, -L - 0.05), (0.5, 0.5, L), tooth_m,
               density=2, tag="tooth")

    # The tongue: a thick root inside the head, its tip folded back (the clips unfold it).
    l1, l2 = tongue_len
    ty = y0 + size * 0.28
    t0 = m.bone(tongue_bones[0], (0, ty + 0.4, -0.2), parent)

    def tongue(face, x, y, w, h):
        n = fbm(x * 2 + len(face), y * 2, seed + 9, 32, 32, 2, 5.0)
        c = tone(TONGUE, 3.0 + (n - 0.5) * 2.0 - (1.2 if face in ("down", "south") else 0.0))
        if face == "up" and abs(x - (w - 1) / 2) < 0.6:
            c = TONGUE[1]                                # the central groove
        if n < 0.3:
            c = mix(c, GOO[2], 0.5)
        return c
    m.cube(t0, (-1.0 * size / 8, ty, -0.2 - l1), (2.0 * size / 8, 0.8 * size / 8, l1), tongue, density=2, tag="tongue")
    prev = tongue_bones[0]
    z = -0.2 - l1
    for i, name in enumerate(tongue_bones[1:]):
        seg = l2 / (len(tongue_bones) - 1)
        last = i == len(tongue_bones) - 2
        # Every later segment folds back on the one before it (Y 180), so the whole tongue stays in the head.
        b = m.bone(name, (0, ty + 0.4, z), prev, rotation=(0, 180, 0) if i == 0 else None)
        wdt = (1.6 - 0.35 * i) * size / 8
        m.cube(b, (-wdt / 2, ty + 0.05, z - seg), (wdt, 0.7 * size / 8, seg), tongue, density=2, tag="tongue")
        if last:
            # A forked, goo-black tip.
            m.cube(b, (-wdt / 2, ty + 0.05, z - seg - 0.6), (wdt, 0.5, 0.6), solid(GOO[2]), density=2, tag="tongue_tip")
        prev = name
        z -= seg
    return ["jaw_left", "jaw_right"]


FACE = [   # x = 0 is the viewer's left (the entity's right)
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hHhhhHhhhhhhHhhh",
    "sshhssssssssshss",
    "ssssssssssssssss",
    "sbbbbssvsssbbbbs",
    "ssssssssvsssssss",
    "swwEEwssvswEEwws",
    "suuuuussvssuuuus",
    "sssssssnnsssssss",
    "ssvssssnnsssssss",
    "sssssNNnnNNssvss",
    "ssssssssssssssss",
    "ssssmmmmmmmmssss",
    "jsssssssssssssuj",
    "jjsususuususssjj",
]
assert all(len(r) == 16 for r in FACE)


def face_legend(seed):
    sk = veined(skin(seed), seed + 1, 0.009)
    hair = H.skin_mat(HAIR, seed + 2, 2.6, 1.2)
    return sk, hair, {
        "s": sk, "h": hair, "H": HAIR[4], "b": HAIR[1], "w": EYE_WHITE, "E": IRIS, "u": SKIN[2], "n": SKIN[4],
        "N": SKIN[1], "m": hexc("#3a2a2a"), "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.85),
        "v": GOO[2],   # a vein down the seam where the face will split
    }


# =====================================================================================================
# The body
# =====================================================================================================

HIP, NECK = 12.0, 24.0
SY, ELBOW, WRIST, PALM = 24.0, 18.0, 13.8, 11.4
AX = 6.0
KNEE = 6.0


def build_body(m):
    sk = veined(skin(25101), 25102, 0.015)
    shirt = cloth(SHIRT, 25110, 3.0)

    def torso(face, x, y, w, h):
        c = shirt(face, x, y, w, h)
        if face != "north":
            if face == "south" and y == 4:
                return shade(c, 0.85)          # the yoke seam
            return c
        mid = (w - 1) / 2
        # Open collar: a V of grey skin, a button placket, two flap pockets.
        if y < 5 and abs(x - mid) <= 2.5 - y * 0.5:
            return sk(face, x, y, w, h)
        if abs(x - mid) < 0.6 and y >= 5:
            return SHIRT[1] if y % 4 else TOOTH[2]
        for px in (3, w - 4):
            if 6 <= y <= 9 and abs(x - px) <= 2:
                return SHIRT[4] if y == 6 else (SHIRT[1] if y == 9 or abs(x - px) == 2 else c)
        if y >= h - 3:
            return BELT[2] if y < h - 1 else DENIM[2]      # the belt, the jeans' waistband
        return c
    m.cube("body", (-4, HIP, -2), (8, NECK - HIP, 4), torso, density=2, tag="torso")
    m.cube("body", (-1.2, HIP + 1.2, -2.2), (2.4, 1.2, 0.3), lambda f, x, y, w, h: TOOTH[1] if f == "north" else BELT[1],
           density=2, tag="buckle")
    # The collar of the shirt, standing up a little, open at the throat.
    for s in (-1, 1):
        m.cube("body", (s * 1.6 - 1.0, NECK - 0.9, -2.35), (2.0, 1.0, 0.4), lambda f, x, y, w, h: SHIRT[4] if y == 0 else SHIRT[3],
               density=2, tag="collar", rotation=(16, 0, -28 * s), pivot=(s * 1.6, NECK, -2.2))
    m.cube("body", (-1.6, NECK - 0.6, -1.6), (3.2, 0.8, 3.2), sk, density=2, tag="neck")

    for side, s in (("right", -1), ("left", 1)):
        cx = AX * s
        m.bone(f"{side}_arm", (cx + s * -1.0, SY - 2, 0), "body")
        m.bone(f"{side}_forearm", (cx, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (cx, WRIST, 0), f"{side}_forearm")
        m.cube(f"{side}_arm", (cx - 2, ELBOW - 0.4, -2), (4, SY - ELBOW + 0.4, 4), cloth(SHIRT, 25120 + s, 2.9), density=2,
               tag="sleeve")
        # The sleeve rolled up above the elbow.
        m.cube(f"{side}_arm", (cx - 2, ELBOW - 0.4, -2), (4, 1.4, 4), none_on(("up",), lambda f, x, y, w, h: SHIRT[4] if y == 0 else SHIRT[2]),
               inflate=0.3, density=2, tag="roll")
        m.cube(f"{side}_forearm", (cx - 1.8, WRIST, -1.8), (3.6, ELBOW - WRIST, 3.6), veined(skin(25130 + s), 25131 + s, 0.022),
               density=2, tag="forearm")
        m.cube(f"{side}_hand", (cx - 1.6, PALM, -1.6), (3.2, WRIST - PALM, 3.2), sk, density=2, tag="hand")
        m.cube(f"{side}_hand", (cx - 1.5, PALM - 1.0, -1.5), (3.0, 1.0, 2.2),
               lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9 if x % 2 else 1.0), density=2, tag="fingers")

    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_leg", (2 * s, HIP, 0), "root")
        m.bone(f"{side}_shin", (2 * s, KNEE, 0), f"{side}_leg")
        jeans = cloth(DENIM, 25140 + s, 2.9)

        def jean(face, x, y, w, h, jeans=jeans):
            c = jeans(face, x, y, w, h)
            if face in ("east", "west") and abs(x - (w - 1) / 2) < 0.6 and y % 2 == 0:
                return mix(c, hexc("#b88a3c"), 0.35)      # orange-ish seam stitching
            return c
        x0 = 2 * s - 2
        m.cube(f"{side}_leg", (x0 + (0.05 if s > 0 else -0.05), KNEE, -2), (4, HIP - KNEE, 4), jean, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0 + (0.05 if s > 0 else -0.05), 2.0, -2), (4, KNEE - 2.0, 4), jean, density=2, tag="shin")

        def boot(face, x, y, w, h):
            n = fbm(x * 2, y * 2 + len(face), 25150, 32, 32, 2, 6.0)
            c = tone(BOOT, 2.8 + (n - 0.5) * 1.6)
            if face != "up" and y >= h - 1:
                return BOOT[0]                        # the sole
            if face == "north" and y == 1 and x % 2:
                return TOOTH[1]                       # laces
            return c
        m.cube(f"{side}_shin", (x0 - 0.1 + (0.05 if s > 0 else -0.05), 0, -2.8), (4.2, 2.6, 4.8), boot, density=2, tag="boot")


def build_goo_neck(m, parent, y, seed=25200, w=3.6):
    """The black, dripping stump (shown only while headless)."""
    m.bone("goo_neck", (0, y, 0), parent)
    goo = goo_m(seed)
    m.cube("goo_neck", (-w / 2, y - 0.6, -w / 2), (w, 1.6, w), goo, density=2, tag="stump")
    m.cube("goo_neck", (-w / 2 + 0.6, y + 0.8, -w / 2 + 0.6), (w - 1.2, 1.0, w - 1.2), goo, density=2, tag="stump_top")
    m.cube("goo_neck", (-0.6, y + 1.6, -0.9), (1.2, 0.8, 1.2), goo, density=2, tag="stump_blob")
    m.bone("goo_drip", (0, y, 0), "goo_neck")
    for i, (dx, dz, L) in enumerate(((-1.4, -1.9, 2.6), (1.1, -1.9, 1.6), (1.9, 0.4, 3.2), (-1.9, 1.0, 2.0), (0.3, 1.9, 2.8))):
        m.cube("goo_drip", (dx - 0.35, y - 0.6 - L, dz - 0.35), (0.7, L, 0.7), goo, density=2, tag="drip")
        m.cube("goo_drip", (dx - 0.5, y - 0.6 - L - 0.5, dz - 0.5), (1.0, 0.6, 1.0), goo, density=2, tag="drop")


def rig():
    m = Model("leviathan", 256, 128)
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("head", (0, NECK, 0), "body")
    sk, hair, legend = face_legend(25001)
    build_maw(m, "head", NECK, FACE, legend, sk, hair, seed=25010)
    build_goo_neck(m, "body", NECK)
    build_body(m)
    return m


def head_rig():
    """The severed head: the same split head on the ground, a goo stump where the neck was."""
    m = Model("leviathan_head", 128, 64)
    m.bone("root", (0, 0, 0))
    sk, hair, legend = face_legend(25001)
    build_maw(m, "root", 1.0, FACE, legend, sk, hair, seed=25010)
    goo = goo_m(25300)
    m.cube("root", (-2.0, 0, -2.0), (4.0, 1.0, 4.0), goo, density=2, tag="stump")
    m.cube("root", (-2.6, 0, 0.4), (1.2, 0.4, 2.2), goo, density=2, tag="smear")
    m.cube("root", (1.0, 0, 2.0), (1.6, 0.4, 1.4), goo, density=2, tag="smear")
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

Clip = H.Clip
OPEN = 100.0       # how far each jaw swings out


def maw(k=1.0, splay=0.0, tongue=1.0, droop=25.0):
    """The jaws open by `k` (0 shut, 1 wide), the tongue unfolded by `tongue` and hanging by `droop`."""
    return {"jaw_left": [0, -OPEN * k, splay * k], "jaw_right": [0, OPEN * k, -splay * k],
            "tongue": [droop * tongue, 0, 0], "@tongue": [0, -0.6 * tongue, -2.6 * tongue],
            "tongue_tip": [10 * tongue, -180 * tongue, 0]}


def merge(*ps):
    out = {}
    for p in ps:
        out.update(p)
    return out


REVEALED = merge(maw(1.0, 6), {"head": [-8, 0, 0], "right_arm": [-8, 0, 10], "left_arm": [-8, 0, -10],
                               "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0]})
LYING = {"root": [90, 0, 0], "@root": [0, 2.2, 0], "right_arm": [-160, 0, 12], "left_arm": [-160, 0, -12],
         "right_leg": [0, 0, 6], "left_leg": [0, 0, -6]}


def anims():
    f = AnimFile()
    idle = Clip(f, A + "idle", 3.0, loop=True)
    for t, b, hd in ((0, 0, [0, 0, 0]), (1.5, -0.3, [3, 8, -2]), (3.0, 0, [0, 0, 0])):
        idle.key(t, {"@body": [0, b, 0], "head": hd, "right_arm": [0, 0, 2 + b * -6], "left_arm": [0, 0, -2 + b * 6],
                     "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0]}, E_IO if t else None)
    idle.done()

    walk = Clip(f, A + "walk", 1.0, loop=True)
    for t, s in ((0, 1), (0.5, -1), (1.0, 1)):
        walk.key(t, {"right_leg": [26 * s, 0, 0], "left_leg": [-26 * s, 0, 0], "right_shin": [8 if s < 0 else 22, 0, 0],
                     "left_shin": [22 if s < 0 else 8, 0, 0], "right_arm": [-22 * s, 0, 3], "left_arm": [22 * s, 0, -3],
                     "right_forearm": [-12, 0, 0], "left_forearm": [-12, 0, 0], "body": [3, 5 * s, 0], "head": [-3, -4 * s, 0]},
                 E_IO if t else None)
    walk.anim.pos("body", (0, [0, 0, 0]), (0.25, [0, 0.6, 0], E_OUT), (0.5, [0, 0, 0], E_IN), (0.75, [0, 0.6, 0], E_OUT),
                  (1.0, [0, 0, 0], E_IN))
    walk.done()

    # Headless: the fallen body twitching, the stump oozing.
    hl = Clip(f, A + "headless", 2.0, loop=True)
    for t, tw in ((0, 0), (0.3, 1), (0.45, -0.5), (0.7, 0), (1.3, 0), (1.45, 0.8), (1.6, -0.3), (2.0, 0)):
        hl.key(t, merge(LYING, {"right_arm": [-160 + 14 * tw, 0, 12 + 8 * tw], "left_arm": [-160 - 6 * tw, 0, -12],
                                "right_forearm": [-20 * abs(tw), 0, 0], "left_hand": [30 * tw, 0, 0],
                                "right_leg": [-8 * tw, 0, 6], "body": [0, 0, 3 * tw], "goo_drip": [0, 0, 0],
                                "@goo_drip": [0, -0.4 * abs(tw), 0]}))
    hl.done()

    # Reveal: a shudder, then the face splits and the halves swing out; held open.
    rv = Clip(f, A + "reveal", 1.2, hold=True)
    rv.key(0, {})
    rv.key(0.2, merge(maw(0.04, 0, 0), {"head": [12, 0, 0], "body": [6, 0, 0], "right_arm": [0, 0, 6], "left_arm": [0, 0, -6]}), E_OUT)
    rv.key(0.35, merge(maw(0.08, 0, 0), {"head": [14, 4, 3], "body": [7, 0, 0], "right_arm": [0, 0, 8], "left_arm": [0, 0, -8]}))
    rv.key(0.6, merge(maw(1.1, 10, 1.0, 5), {"head": [-22, 0, 0], "body": [-6, 0, 0], "right_arm": [-14, 0, 22],
                                               "left_arm": [-14, 0, -22], "right_forearm": [-30, 0, 0],
                                               "left_forearm": [-30, 0, 0]}), E_BACK)
    rv.key(0.85, merge(maw(1.0, 8, 1.0, 30), {"head": [-12, 0, 0], "body": [-2, 0, 0], "right_arm": [-10, 0, 14],
                                                "left_arm": [-10, 0, -14], "right_forearm": [-24, 0, 0],
                                                "left_forearm": [-24, 0, 0]}), E_IO)
    rv.key(1.2, REVEALED, E_IO)
    rv.done()

    # Bite: rears back, lunges maw first and snaps shut on the beat (tick 10), opens again.
    bt = Clip(f, A + "bite", 0.9, hold=True)
    bt.key(0, REVEALED)
    bt.key(0.3, merge(maw(1.25, 14, 1.0, 10), {"head": [-26, 0, 0], "body": [-12, 0, 0], "right_arm": [-30, 0, 18],
                                                 "left_arm": [-30, 0, -18], "right_leg": [-8, 0, 0], "left_leg": [10, 0, 0]}), E_OUT)
    bt.key(0.5, merge(maw(0.02, 0, 0.3, 0), {"head": [22, 0, 0], "body": [26, 0, 0], "@body": [0, -0.5, -2.5],
                                              "right_arm": [-70, 0, 8], "left_arm": [-70, 0, -8], "right_leg": [-22, 0, 0],
                                              "left_leg": [18, 0, 0], "left_shin": [20, 0, 0]}), E_SNAP)
    bt.key(0.62, merge(maw(0.05, 0, 0.3, 0), {"head": [18, -6, 0], "body": [20, 0, 0], "@body": [0, -0.4, -2.0],
                                               "right_arm": [-60, 0, 8], "left_arm": [-60, 0, -8], "right_leg": [-18, 0, 0],
                                               "left_leg": [14, 0, 0], "left_shin": [16, 0, 0]}))
    bt.key(0.9, REVEALED, E_IO)
    bt.done()

    # Roar: throws the head back, the jaws splayed as wide as they go, shaking.
    ro = Clip(f, A + "roar", 1.5, hold=True)
    ro.key(0, REVEALED)
    big = lambda j: merge(maw(1.35, 22, 1.0, -10), {"head": [-42, 5 * j, 4 * j], "body": [-14, 0, 0], "right_arm": [-20, 0, 40],
                                                     "left_arm": [-20, 0, -40], "right_forearm": [-40, 0, 0],
                                                     "left_forearm": [-40, 0, 0], "right_hand": [-30, 0, 0],
                                                     "left_hand": [-30, 0, 0]})
    ro.key(0.4, big(0), E_BACK)
    for i, t in enumerate((0.55, 0.7, 0.85, 1.0, 1.1)):
        ro.key(t, big(1 if i % 2 else -1))
    ro.key(1.5, REVEALED, E_IO)
    ro.done()

    # Decapitated: the head comes off on tick 6 (the renderer hides it), the body sags to its knees and falls on its face.
    dc = Clip(f, A + "decapitated", 1.0, hold=True)
    dc.key(0, {})
    dc.key(0.3, {"body": [-10, 0, 0], "right_arm": [-20, 0, 25], "left_arm": [-20, 0, -25], "@body": [0, 0.4, 0]}, E_OUT)
    dc.key(0.55, {"@root": [0, -5.0, 0], "right_leg": [-8, 0, 0], "left_leg": [-8, 0, 0], "right_shin": [88, 0, 0],
                  "left_shin": [88, 0, 0], "body": [18, 0, 0], "right_arm": [-6, 0, 8], "left_arm": [-6, 0, -8]}, E_IN)
    dc.key(0.75, {"root": [40, 0, 0], "@root": [0, -3.0, 0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0],
                  "right_shin": [80, 0, 0], "left_shin": [80, 0, 0], "body": [20, 0, 0], "right_arm": [-80, 0, 10],
                  "left_arm": [-80, 0, -10]}, E_IN)
    dc.key(1.0, LYING, E_OUT)
    dc.done()

    # Rejoin: the body pushes itself up off the floor and stands, head back on (the code shows it again).
    rj = Clip(f, A + "rejoin", 1.5)
    rj.key(0, LYING)
    rj.key(0.4, merge(LYING, {"root": [80, 0, 0], "@root": [0, 2.0, 0], "right_arm": [-100, 0, 10], "left_arm": [-100, 0, -10],
                              "right_forearm": [-40, 0, 0], "left_forearm": [-40, 0, 0]}), E_IO)
    rj.key(0.8, {"root": [30, 0, 0], "@root": [0, -4.0, 0], "right_leg": [-40, 0, 0], "left_leg": [-20, 0, 0],
                 "right_shin": [80, 0, 0], "left_shin": [70, 0, 0], "body": [15, 0, 0], "right_arm": [-30, 0, 6],
                 "left_arm": [-30, 0, -6], "head": [20, 0, 0]}, E_IO)
    rj.key(1.15, {"@root": [0, -1.0, 0], "right_leg": [-20, 0, 0], "left_leg": [-10, 0, 0], "right_shin": [25, 0, 0],
                  "left_shin": [15, 0, 0], "body": [10, 0, 0], "head": [-10, 0, 8]}, E_OUT)
    rj.key(1.5, {}, E_IO)
    rj.done()
    return f


def head_anims():
    f = AnimFile()
    idle = Clip(f, AH + "idle", 2.0, loop=True)
    for t, k in ((0, 0.0), (0.6, 0.06), (0.75, 0.0), (1.4, 0.1), (1.55, 0.02), (2.0, 0.0)):
        idle.key(t, {"jaw_left": [0, -OPEN * k, 0], "jaw_right": [0, OPEN * k, 0], "root": [0, 3 * k * 10, 0]}, E_IO if t else None)
    idle.done()

    # Crawl: the maw cracks, the tongue shoots out along the floor, hooks and drags the head after it.
    cr = Clip(f, AH + "crawl", 1.0, loop=True)

    def reach(k, hook, lurch, tilt):
        return {"jaw_left": [0, -38, 4], "jaw_right": [0, 38, -4], "tongue": [32 * k + hook, 0, 0],
                "@tongue": [0, -0.8 * k, -2.6 * k], "tongue_tip": [-10 * hook, -180 * k, 0], "@root": [0, 0, lurch],
                "root": [tilt, 0, 0]}
    cr.key(0, reach(0.2, 0, 0, 0))
    cr.key(0.35, reach(1.0, 0, 0, -6), E_OUT)
    cr.key(0.55, reach(1.0, 20, 0, -4))
    cr.key(0.85, reach(0.35, 10, -3.0, 10), E_IN)
    cr.key(1.0, reach(0.2, 0, 0, 0), E_IO)
    cr.done()

    sn = Clip(f, AH + "snap", 0.6)
    sn.key(0, {})
    sn.key(0.2, {"jaw_left": [0, -90, 10], "jaw_right": [0, 90, -10], "root": [-20, 0, 0], "@root": [0, 1.5, 0]}, E_OUT)
    sn.key(0.32, {"jaw_left": [0, -2, 0], "jaw_right": [0, 2, 0], "root": [12, 0, 0], "@root": [0, 0.5, -2.5]}, E_SNAP)
    sn.key(0.6, {}, E_IO)
    sn.done()
    return f


def generate():
    m = rig()
    t, _ = m.build(gutter=1, seed=25000)
    m.rig.write(GEO + "leviathan.geo.json")
    save(t, "entity", "leviathan")
    write_compact(anims(), ANIM + "leviathan.animation.json")

    hm = head_rig()
    t, _ = hm.build(gutter=1, seed=25300)
    hm.rig.write(GEO + "leviathan_head.geo.json")
    save(t, "entity", "leviathan_head")
    write_compact(head_anims(), ANIM + "leviathan_head.animation.json")


if __name__ == "__main__":
    generate()
