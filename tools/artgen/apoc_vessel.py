"""The Apocalypse's vessels (v0.40): one parametric human rig shared by Alastair, Ruby and Uriel (each module paints its own
clothes and adds its own bones). The build follows crowley_art (fingers, a coat with fronts and a tail, a cuffed sleeve),
with the proportions in a `Body` instead of module constants, so a tall lean man, a slim woman and a broad soldier come out
of the same code.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- eyes, brows, (hair)
               -- coat -- collar, coat_r, coat_l, coat_tail          (only with coat=True)
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_middle, right_thumb
               -- left_arm  -- (the same)
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import horsemen_art as H
from chuck_art import h01, none_on, solid
from michael_art import fabric
from pixelkit import Ramp, fbm, hexc, mix, shade

SIDES = (("right", -1), ("left", 1))
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"


def tone(ramp, v):
    return ramp[max(0, min(len(ramp) - 1, int(round(v))))]


class Body:
    """Proportions in px. Defaults: an average man (32 px to the crown)."""

    def __init__(self, **kw):
        self.HIP = 12.0
        self.NECK = 24.0
        self.HEAD = 8.0        # the head cube's size (its face is always 16x16 texels)
        self.TW = 4.0          # half the torso's width
        self.TD = 2.1          # half its depth
        self.AW = 3.6          # an arm's width
        self.SY = 23.4         # the shoulder pivot's height
        self.ELBOW = 18.4
        self.WRIST = 13.8
        self.PALM = 11.9
        self.LX = 2.0
        self.LW = 3.8
        self.KNEE = 6.4
        self.SHOE_H = 1.5
        self.HEM = 5.0         # a coat's hem
        self.__dict__.update(kw)
        self.AX = self.__dict__.get("AX", self.TW + self.AW / 2 + 0.05)
        self.SX = self.__dict__.get("SX", self.AX - 0.6)

    @property
    def crown(self):
        return self.NECK + self.HEAD


# =====================================================================================================
# Geometry
# =====================================================================================================

def skeleton(m, b, coat=True):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, b.HIP, 0), "root")
    m.bone("head", (0, b.NECK, 0), "body")
    m.bone("eyes", (0, b.NECK + b.HEAD * 0.52, -b.HEAD / 2), "head")
    m.bone("brows", (0, b.NECK + b.HEAD * 0.62, -b.HEAD / 2), "head")
    if coat:
        m.bone("coat", (0, b.NECK, 0), "body")
        m.bone("collar", (0, b.NECK - 0.2, 0.8), "coat")
        m.bone("coat_r", (-2.4, b.HIP + 0.6, -b.TD - 0.4), "coat")
        m.bone("coat_l", (2.4, b.HIP + 0.6, -b.TD - 0.4), "coat")
        m.bone("coat_tail", (0, b.HIP + 0.6, b.TD + 0.4), "coat")
    for side, s in SIDES:
        m.bone(f"{side}_arm", (b.SX * s, b.SY, 0), "body")
        m.bone(f"{side}_forearm", (b.AX * s, b.ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (b.AX * s, b.WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (b.AX * s, b.PALM, 0.3), f"{side}_hand")
        m.bone(f"{side}_index", (b.AX * s, b.PALM, -1.0), f"{side}_hand")
        m.bone(f"{side}_middle", (b.AX * s, b.PALM, -0.35), f"{side}_hand")
        m.bone(f"{side}_thumb", (b.AX * s - 0.8 * s, b.PALM + 1.2, -1.2), f"{side}_hand")
    for side, s in SIDES:
        m.bone(f"{side}_leg", (b.LX * s, b.HIP, 0), "root")
        m.bone(f"{side}_shin", (b.LX * s, b.KNEE, 0), f"{side}_leg")


def head(m, b, face_rows, legend, sides, glow=None, nose=None, hair_shell=None, shell_inflate=0.3, shell_rows=2.5):
    """The skull (its north face from 16 rows of legend characters), the nose, and an optional hair shell over the crown
    (`hair_shell(face, x, y, w, h)` -> colour/None; its north face is the fringe)."""
    hs = b.HEAD / 2

    def skull(f, x, y, w, h):
        if f == "north":
            ch = face_rows[y][x] if y < len(face_rows) and x < len(face_rows[y]) else "s"
            v = legend.get(ch, legend["s"])
            return v(f, x, y, w, h) if callable(v) else v
        return sides(f, x, y, w, h)
    m.cube("head", (-hs, b.NECK, -hs), (b.HEAD, b.HEAD, b.HEAD), skull, glow=glow, density=2, tag="skull")
    if nose:
        m.cube("head", (-0.5, b.NECK + b.HEAD * 0.32, -hs - 0.5), (1, 2, 0.5), nose, density=2, tag="nose")
    if hair_shell:
        m.cube("head", (-hs, b.crown - shell_rows, -hs), (b.HEAD, shell_rows, b.HEAD), none_on(("down",), hair_shell),
               inflate=shell_inflate, density=2, tag="hair_cap")


def eye_shell(m, b, pattern, colours, glow=True):
    """A thin plane over the eyes (bone `eyes`), 12 texels wide at density 2: `pattern` is 12 chars (`.` clear) indexing
    `colours`. Lit in the glowmask; the renderer shows or hides the bone."""
    def shell(f, x, y, w, h):
        ch = pattern[x] if x < len(pattern) else "."
        return None if ch == "." else colours[ch]
    g = (lambda f, x, y, w, h, c: c) if glow else None
    m.cube("eyes", (-3.0, b.NECK + b.HEAD * 0.5, -b.HEAD / 2 - 0.06), (6.0, 0.5, 0), shell, g, faces=("north",), density=2, tag="eyes")


def neck(m, b, skin, w=3.6):
    m.cube("body", (-w / 2, b.NECK - 1.0, -w / 2), (w, 1.2, w), skin, density=2, tag="neck")


def torso(m, b, mat, inflate=0.0, tag="torso", top=None, bottom=None, faces_off=()):
    """A torso layer from the hip (or `bottom`) to the neck (or `top`)."""
    y0 = b.HIP if bottom is None else bottom
    y1 = b.NECK if top is None else top
    return m.cube("body", (-b.TW, y0, -b.TD), (2 * b.TW, y1 - y0, 2 * b.TD), none_on(faces_off, mat) if faces_off else mat,
                  inflate=inflate, density=2, tag=tag)


def coat(m, b, ramp, mat, seed, lapel=True, collar_up=False, pockets=True, hem=None, skirt=True, open_front=True):
    """A long coat over the torso: the shell over the shoulders and back (open in front: only its two fronts show), lapels,
    a collar (turned down, or `collar_up` standing round the neck), and the skirt -- two fronts and the tail -- down to `hem`."""
    hem = b.HEM if hem is None else hem
    TW, TD, NECK, HIP = b.TW, b.TD, b.NECK, b.HIP

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north" and open_front:
            if x < 4 or x > w - 5:
                return ramp[4] if x in (3, w - 4) else mat(f, x, y, w, h)
            return None
        return mat(f, x, y, w, h)
    m.cube("coat", (-TW, HIP + 0.6, -TD), (2 * TW, NECK - HIP - 0.4, 2 * TD), shell, inflate=0.6, density=2, tag="coat_shell")
    for s in (-1, 1):
        if lapel:
            def lap(f, x, y, w, h, s=s):
                if f == "south":
                    return ramp[0]
                c = tone(ramp, 4.0 - 1.6 * y / max(1, h))
                if f == "north" and (x == 0 if s < 0 else x == w - 1):
                    return ramp[5]
                return c
            m.cube("coat", (3.4 * s - 1.1, NECK - 8.0, -TD - 0.95), (2.2, 7.4, 0.4), lap, density=2, tag="coat_lapel",
                   rotation=(3, 0, 8 * s), pivot=(3.0 * s, NECK - 8.0, -TD - 0.8))
        m.cube("coat", (s * (TW + 0.3) - 1.3, NECK - 0.9, -TD - 0.5), (2.6, 0.8, 2 * TD + 1.0),
               lambda f, x, y, w, h: tone(ramp, 4.0 if f == "up" else 3.0), density=2, tag="coat_shoulder")

    def col(f, x, y, w, h):
        if f == "down":
            return ramp[0]
        return tone(ramp, 4.1 - 1.2 * y / max(1, h))
    if collar_up:
        m.cube("collar", (-3.4, NECK - 0.4, 0.6), (6.8, 2.6, 0.5), col, density=2, tag="collar_back", rotation=(-10, 0, 0),
               pivot=(0, NECK - 0.2, 0.8))
        for s in (-1, 1):
            m.cube("collar", (s * 3.3 - 0.25, NECK - 0.4, -2.0), (0.5, 2.4, 2.8), col, density=2, tag="collar_side",
                   rotation=(0, 0, 8 * s), pivot=(s * 3.3, NECK - 0.2, 0))
    else:
        m.cube("collar", (-3.4, NECK - 1.6, 0.4), (6.8, 2.2, 0.5), col, density=2, tag="collar_back", rotation=(14, 0, 0),
               pivot=(0, NECK - 0.2, 0.8))
        for s in (-1, 1):
            m.cube("collar", (s * 3.3 - 0.25, NECK - 1.6, -1.8), (0.5, 2.0, 2.6), col, density=2, tag="collar_side",
                   rotation=(0, 0, -12 * s), pivot=(s * 3.3, NECK - 0.2, 0))
    if not skirt:
        return
    L = HIP + 0.6 - hem
    for bone, s in (("coat_r", -1), ("coat_l", 1)):
        def panel(f, x, y, w, h, s=s):
            if f == "south":
                return ramp[1]
            c = mat(f, x, y + 18, w, h + 18)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return ramp[4]
            if pockets and f == "north" and 2 <= y <= 3 and 2 <= (w - 1 - x if s < 0 else x) <= w - 3:
                return ramp[0] if y == 3 else ramp[4]
            return c
        xa, xb = (-TW - 0.7, -0.6) if s < 0 else (0.6, TW + 0.7)
        m.cube(bone, (xa, hem, -TD - 0.75), (xb - xa, L, 0.5), panel, density=2, tag="coat_front", rotation=(-4, 0, 3 * s),
               pivot=(0, HIP + 0.6, -TD - 0.5))
        xs = -TW - 0.75 if s < 0 else TW + 0.25
        m.cube(bone, (xs, hem, -TD - 0.5), (0.5, L, TD + 0.5), none_on(("up",), mat), density=2, tag="coat_side",
               rotation=(0, 0, 3 * s), pivot=(xs, HIP, 0))

    def back(f, x, y, w, h):
        if f == "north":
            return ramp[0]
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > h * 0.55:
            return ramp[0]
        return mat(f, x, y + 18, w, h + 18)
    m.cube("coat_tail", (-TW - 0.75, hem, TD + 0.25), (2 * TW + 1.5, L, 0.5), back, density=2, tag="coat_back", rotation=(4, 0, 0),
           pivot=(0, HIP + 0.6, TD + 0.4))
    for s in (-1, 1):
        xs = -TW - 0.75 if s < 0 else TW + 0.25
        m.cube("coat_tail", (xs, hem, 0), (0.5, L, TD + 0.5), none_on(("up",), mat), density=2, tag="coat_back_side",
               rotation=(4, 0, 3 * s), pivot=(xs, HIP, TD))


def arms(m, b, sleeve, hand, cuff=None, cuff_depth=1.2, sleeve_inflate=0.3, glove=None):
    """Sleeves, an optional cuff, the hand with its four finger bones (`glove` paints the hand instead of skin)."""
    hand = glove or hand
    for side, s in SIDES:
        cx = b.AX * s
        aw = b.AW
        m.cube(f"{side}_arm", (cx - aw / 2, b.ELBOW, -aw / 2), (aw, b.SY + 0.8 - b.ELBOW, aw), sleeve, inflate=sleeve_inflate,
               density=2, tag="sleeve")
        fw = aw - 0.2
        m.cube(f"{side}_forearm", (cx - fw / 2, b.WRIST + 0.4, -fw / 2), (fw, b.ELBOW - b.WRIST - 0.4, fw), none_on(("up",), sleeve),
               inflate=sleeve_inflate, density=2, tag="forearm_sleeve")
        if cuff:
            m.cube(f"{side}_forearm", (cx - fw / 2, b.WRIST + 0.1, -fw / 2), (fw, cuff_depth, fw), cuff, inflate=sleeve_inflate + 0.15,
                   density=2, tag="cuff")
        pw = min(2.6, aw - 0.8)
        m.cube(f"{side}_hand", (cx - pw / 2, b.PALM, -1.4), (pw, b.WRIST - b.PALM + 0.2, 2.8), hand, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.88 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{side}_fingers", (cx - pw / 2 + 0.05, b.PALM - 1.1, -0.1), (pw - 0.1, 1.1, 1.4), fing, density=2, tag="fingers")
        m.cube(f"{side}_middle", (cx - pw / 2 + 0.05, b.PALM - 1.25, -0.75), (pw - 0.1, 1.25, 0.65), fing, density=4, tag="middle")
        m.cube(f"{side}_index", (cx - pw / 2 + 0.05, b.PALM - 1.15, -1.4), (pw - 0.1, 1.15, 0.65), fing, density=4, tag="index")
        m.cube(f"{side}_thumb", (cx - 0.8 * s - 0.45, b.PALM + 0.2, -1.95), (0.9, 1.5, 0.85), hand, density=2, tag="thumb")


def legs(m, b, trouser, shoe, boot_h=None, toe=1.0):
    """Thighs and shins in `trouser` (a material per side: fn(side) or a single material), shoes (or boots `boot_h` tall)."""
    for side, s in SIDES:
        tr = trouser(side) if callable(trouser) and getattr(trouser, "per_side", False) else trouser
        x0 = b.LX * s - b.LW / 2
        m.cube(f"{side}_leg", (x0, b.KNEE, -b.LW / 2), (b.LW, b.HIP - b.KNEE, b.LW), tr, density=2, tag="thigh")
        sh = b.SHOE_H if boot_h is None else boot_h
        m.cube(f"{side}_shin", (x0, sh, -b.LW / 2), (b.LW, b.KNEE - sh, b.LW), tr, density=2, tag="shin")

        def shoe_m(f, x, y, w, h):
            c = tone(shoe, 2.4 - 0.8 * y / max(1, h))
            if f == "up" or (f in ("north", "east", "west") and y == 0):
                c = shoe[4]
            if f == "up" and (x + y) % 5 == 0:
                c = shoe[5]
            return c
        if boot_h:
            m.cube(f"{side}_shin", (x0 - 0.1, 0, -b.LW / 2 - 0.1), (b.LW + 0.2, boot_h, b.LW + 0.2), shoe_m, density=2, tag="boot")
            m.cube(f"{side}_shin", (x0 - 0.05, 0, -b.LW / 2 - toe), (b.LW + 0.1, 1.2, toe), shoe_m, density=2, tag="toe")
        else:
            m.cube(f"{side}_shin", (x0 - 0.1, 0, -b.LW / 2 - toe), (b.LW + 0.2, b.SHOE_H, b.LW + toe + 0.1), shoe_m, density=2, tag="shoe")


def per_side(fn):
    fn.per_side = True
    return fn


# =====================================================================================================
# Materials
# =====================================================================================================

def skin_m(ramp, seed, base=3.0, amp=0.5):
    return H.skin_mat(ramp, seed, base, amp)


def hair_m(ramp, seed, grey=None, grey_p=0.0, strands=True, base=2.6):
    def m(face, x, y, w, h):
        n = fbm(x * 0.6 + len(face) * 7, y * 2.5, seed, 64, 64, 2, 6.0)
        v = base + (n - 0.5) * 1.3 - 0.5 * (y / max(1, h))
        if strands and (y * 3 + x // 3) % 5 == 0:
            v += 0.7
        c = tone(ramp, v)
        if grey is not None and h01("grey", seed, face, x, y) < grey_p:
            c = mix(c, grey, 0.5)
        return c
    return m


def leather_m(ramp, seed, base=2.8, sheen=0.9, creases=True):
    """Worn leather: smooth, a broad sheen on the upper faces, creases and scuffs."""
    def m(f, x, y, w, h):
        n = fbm(x * 1.5 + len(f) * 5, y * 1.5, seed, 64, 64, 3, 7.0)
        v = base + (n - 0.5) * 1.1 + (sheen if f == "up" else 0) - 0.7 * (y / max(1, h))
        if f in ("north", "east") and abs(((x + y * 0.6) % 9) - 4.5) < 0.6:
            v += sheen * 0.6
        if creases and h01("crease", seed, f, y // 2) < 0.12 and x % 3 == 0:
            v -= 0.8
        c = tone(ramp, v)
        if h01("scuff", seed, f, x, y) < 0.025:
            c = mix(c, ramp[5], 0.4)
        return c
    return m


def wool_m(ramp, seed, base=3.0, nap=0.06):
    cl = fabric(ramp, seed, base, 0.35)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        if h01("nap", seed, f, x, y) < nap:
            c = shade(c, 1.12)
        return c
    return m


def glove_m(ramp, seed):
    def m(f, x, y, w, h):
        c = tone(ramp, 2.4 + 0.8 * math.sin(x * 0.9 + y * 0.4 + seed) - 0.5 * y / max(1, h))
        if f in ("north", "up") and (x + y) % 6 == 0:
            c = ramp[4]
        return c
    return m


def jitter(seed, t, amp):
    return [amp * (2 * h01(seed, t, i) - 1) for i in range(3)]
