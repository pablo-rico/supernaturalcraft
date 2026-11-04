"""Crowley, King of the Crossroads (v0.35): the boss's rig (shared with his companion form), texture + glowmask and clips
(names and timings in CrowleyAssets), and the Contract Book's sprite. His retinue is in crowley_retinue_art, the contract
bar in crowley_gui_art.

A short, stocky man (30 px to the crown, broad in the chest): short dark hair going grey, swept back and receding at the
temples; a trimmed salt-and-pepper beard and moustache; heavy-lidded eyes over a smug half-smile (the corner on his left
raised). A black suit, black shirt and black silk tie under a long black wool overcoat, open, to the knees: notch lapels,
a turned-down collar, deep cuffs. Black trousers, polished black shoes. His eyes flash red (`eyes`: a shell over them,
painted red and lit in the glowmask; the renderer hides it while they are dark).

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- body -- head -- eyes, brows, beard
               -- tie;  coat -- collar, coat_r, coat_l, coat_tail
               -- right_arm -- right_forearm -- right_hand -- right_fingers, right_index, right_middle, right_thumb
               -- left_arm  -- left_forearm  -- left_hand  -- (the same)
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, art, save
from michael_art import fabric, write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
NAME = "crowley"
A = "animation.crowley."
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"

# --- proportions (short and stocky: 30 px to the crown) ----------------------------------------------------------
HIP = 12.0
NECK = 22.4
TW = 4.4          # half the torso's width
TD = 2.4          # half its depth
AW = 3.8
AX = 6.3
SX = 5.8
SY = 21.6
ELBOW = 17.0
WRIST = 12.8
PALM = 10.9
LX = 2.1
LW = 4.0
KNEE = 6.2
SHOE_H = 1.5
HEM = 5.2         # the overcoat's hem, just below the knee

# --- palette ------------------------------------------------------------------------------------------------------
SKIN = Ramp("#4f3328", "#74503f", "#956a55", "#b0836b", "#c49880", "#d8b29a")
HAIR = Ramp("#0d0c0d", "#181618", "#252224", "#353133", "#4a4648", "#66625f")
GREY = Ramp("#4a4846", "#625f5c", "#7c7874", "#97928d", "#b2ada7", "#cdc8c1")
COAT = Ramp("#07070a", "#0e0e12", "#16161b", "#1f1f26", "#2b2b34", "#3d3d48")
SUIT = Ramp("#050506", "#0a0a0c", "#111114", "#19191d", "#232328", "#303037")
SHIRT = Ramp("#040406", "#09090d", "#101016", "#18181f", "#22222c", "#30303d")
TIE = Ramp("#030304", "#08080a", "#0f0f13", "#1a1a20", "#2a2a33", "#45454f")
SHOE = Ramp("#020203", "#07070a", "#0e0e12", "#17171d", "#26262f", "#5a5d6a")
LIPS = Ramp("#3e221c", "#573025", "#6c3c30", "#83493b", "#985748", "#ad6858")
EYE_WHITE, IRIS = hexc("#d3cbbf"), hexc("#2c1a10")
RED, RED_HOT, RED_DARK = hexc("#d8141a"), hexc("#ff4a36"), hexc("#5a0608")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")


def tone(ramp, v):
    return ramp[max(0, min(5, int(round(v))))]


SIDES = (("right", -1), ("left", 1))

FACE = [   # x = 0 is the viewer's left (his right); rows from the crown down
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "ghhhhhhhhhhhhhhg",
    "gsshhhhhhhhhhssg",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "ssUUUUUssUUUUUss",
    "ssuwIIwsswIIwuss",
    "sssbbbsssssbbbss",
    "ssssssssnsssssss",
    "jssssnnnnnnssssj",
    "dssssNNNNNNssssd",
    "dddmmmmmmmmmmLdd",
    "dddmmlllllllLmdd",
    "ddddddBBBBBddddd",
    "dddddddddddddddd",
]
assert all(len(r) == 16 for r in FACE), [len(r) for r in FACE]


def skin_m(seed, base=3.0):
    return H.skin_mat(SKIN, seed, base, 0.5)


def hair_m(seed):
    """Short and swept back, dark going grey: combed strands, grey threads through them."""
    def m(face, x, y, w, h):
        n = fbm(x * 0.6, y * 2.5 + len(face), seed, 64, 64, 2, 6.0)
        v = 1.5 + (n - 0.5) * 1.2
        if (y * 3 + x // 3) % 5 == 0:
            v += 0.8
        c = tone(HAIR, v)
        if h01("grey", seed, face, x, y) < (0.16 if face in ("east", "west") else 0.08):
            c = mix(c, GREY[3], 0.45)               # greying, most at the temples
        return c
    return m


def beard_m(seed, dark=False):
    """Salt and pepper, trimmed close: dark stubble with grey hairs, lighter on the chin."""
    def m(face, x, y, w, h):
        r = h01("beard", seed, face, x, y)
        v = 1.2 + 0.6 * (y / max(1, h - 1)) + (0.0 if dark else 0.4)
        if r < (0.14 if dark else 0.22):
            return mix(tone(GREY, 1.0 + r * 8), tone(HAIR, v), 0.45)
        return tone(HAIR, v + r * 1.2)
    return m


# =====================================================================================================
# Geometry
# =====================================================================================================

def skeleton(m, coat=True):
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, HIP, 0), "root")
    m.bone("head", (0, NECK, 0), "body")
    m.bone("eyes", (0, NECK + 4.2, -4.0), "head")
    m.bone("brows", (0, NECK + 5.0, -4.0), "head")
    m.bone("beard", (0, NECK + 0.6, -3.6), "head")
    m.bone("tie", (0, NECK - 1.0, -TD - 0.3), "body")
    if coat:
        m.bone("coat", (0, NECK, 0), "body")
        m.bone("collar", (0, NECK - 0.2, 0.8), "coat")
        m.bone("coat_r", (-2.6, HIP + 0.6, -TD - 0.4), "coat")
        m.bone("coat_l", (2.6, HIP + 0.6, -TD - 0.4), "coat")
        m.bone("coat_tail", (0, HIP + 0.6, TD + 0.4), "coat")
    for side, s in SIDES:
        m.bone(f"{side}_arm", (SX * s, SY, 0), "body")
        m.bone(f"{side}_forearm", (AX * s, ELBOW, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (AX * s, WRIST, 0), f"{side}_forearm")
        m.bone(f"{side}_fingers", (AX * s, PALM, 0.3), f"{side}_hand")
        m.bone(f"{side}_index", (AX * s, PALM, -1.1), f"{side}_hand")
        m.bone(f"{side}_middle", (AX * s, PALM, -0.4), f"{side}_hand")
        m.bone(f"{side}_thumb", (AX * s - 0.9 * s, PALM + 1.2, -1.3), f"{side}_hand")
    for side, s in SIDES:
        m.bone(f"{side}_leg", (LX * s, HIP, 0), "root")
        m.bone(f"{side}_shin", (LX * s, KNEE, 0), f"{side}_leg")


def build_head(m, face_rows=FACE, sk=None, hr=None, bd=None, seed=35001, eyes="red", hair_back=True, legend_extra=None, beard=True,
               brow=HAIR[1], skin=SKIN):
    """The head: skull with the face painted on, nose, brows, beard volume and the eyes' shell (`eyes`: "red" lit,
    "black" for a demon's black eyes, None for none)."""
    sk = sk or skin_m(seed)
    hr = hr or hair_m(seed + 1)
    bd = bd or beard_m(seed + 2)
    mo = beard_m(seed + 3, dark=True)
    shadow = lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.84)
    lg = {"s": sk, "h": hr, "g": GREY[3], "U": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.72), "u": shadow,
          "w": EYE_WHITE, "I": IRIS, "b": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9), "n": SKIN[4], "N": SKIN[1],
          "d": bd, "m": mo, "j": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), bd(f, x, y, w, h), 0.5), "l": LIPS[0], "L": LIPS[2], "B": LIPS[4]}
    if legend_extra:
        lg.update(legend_extra)

    def skull(f, x, y, w, h):
        if f == "north":
            v = lg.get(face_rows[y][x], sk)
            return v(f, x, y, w, h) if callable(v) else v
        if f == "up":
            return hr(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        if f == "south":
            return hr(f, x, y, w, h) if y < h - 3 or not hair_back else sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x      # 0 at the front
        if y < 4 or (fx >= 6 and y < 13):
            return hr(f, x, y, w, h)
        if fx < 7 and y >= 11 - (fx > 3):
            return bd(f, x, y, w, h)               # the beard along the jaw
        if 7 <= fx <= 9 and 7 <= y <= 10:
            return shade(sk(f, x, y, w, h), 0.86)  # the ear
        return sk(f, x, y, w, h)
    m.cube("head", (-4, NECK, -4), (8, 8, 8), skull, density=2, tag="skull")
    m.cube("head", (-0.75, NECK + 2.4, -4.6), (1.5, 2.0, 0.6),
           lambda f, x, y, w, h: (skin[4] if y < 2 else skin[3]) if f == "north" else (skin[1] if f == "down" else skin[2]),
           density=2, tag="nose")
    # Short hair over the crown, swept back: a thin cap with a forelock ridge.
    m.cube("head", (-4, NECK + 6.5, -4), (8, 1.5, 8), none_on(("down",), hr), inflate=0.3, density=2, tag="hair_cap")
    m.cube("head", (-3.2, NECK + 7.6, -4.2), (6.4, 0.6, 3.0), none_on(("down",), lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.15)),
           density=2, tag="forelock")
    # Brows: heavy, one cocked a touch higher (his left), dark with grey in them.
    for s in (-1, 1):
        m.cube("brows", (s * 2.1 - 1.1, NECK + 4.9 + (0.15 if s > 0 else 0), -4.12), (2.2, 0.45, 0.25),
               lambda f, x, y, w, h: GREY[0] if beard and x == 4 and y == 0 else brow, density=4, tag="brow",
               rotation=(0, 0, (-8 if s > 0 else 6) * s), pivot=(s * 1.0, NECK + 5.0, -4.1))
    # The beard's volume: the chin and the jaw line, the moustache over the lip.
    if beard:
        _beard(m, bd, mo)
    if eyes:
        _eye_shell(m, eyes)
    m.cube("body", (-1.9, NECK - 1.0, -1.9), (3.8, 1.2, 3.8), sk, density=2, tag="neck")


def _beard(m, bd, mo):
    m.cube("beard", (-2.6, NECK - 0.5, -4.3), (5.2, 1.5, 1.0), none_on(("up",), lambda f, x, y, w, h: bd(f, x, y + 3, w, h + 3)),
           density=2, tag="chin")
    m.cube("beard", (-2.1, NECK + 1.9, -4.32), (4.2, 0.55, 0.4), lambda f, x, y, w, h: mo(f, x, y, w, h), density=4, tag="moustache")


def _eye_shell(m, eyes):
    def shell(f, x, y, w, h):
        ch = "uwIIwsswIIwu"[x]
        if ch == "s":
            return None
        if eyes == "black":
            return hexc("#050405") if ch in "wI" else hexc("#140c0c")
        return {"u": RED_DARK, "w": RED, "I": RED_HOT}[ch]
    glow = None if eyes == "black" else (lambda f, x, y, w, h, c: c)
    m.cube("eyes", (-3.0, NECK + 4.0, -4.06), (6.0, 0.5, 0), shell, glow, faces=("north",), density=2, tag="eyes")


def tie_m(ramp):
    def m(f, x, y, w, h):
        v = 2.4 + (1.6 if (x + y) % 5 == 0 else 0) + (0.8 if x == 1 else 0)
        return tone(ramp, v)
    return m


def build_suit(m, suit_r=SUIT, shirt_r=SHIRT, tie_r=TIE, seed=35200, tie_bone="tie", pocket=None):
    """The suit's torso: the jacket over the shirt, notch lapels, the tie in its own bone."""
    shirt = fabric(shirt_r, seed + 1, 3.2, 0.25)
    suit = fabric(suit_r, seed + 2, 3.0, 0.3)
    tie = tie_m(tie_r)
    m.cube("body", (-TW, HIP, -TD), (2 * TW, NECK - HIP, 2 * TD), lambda f, x, y, w, h: shirt(f, x, y, w, h) if f == "north" else suit_r[1],
           density=2, tag="torso")
    front = H.jacket_front(suit, suit_r[4], shirt, tie=None, gap=(5, 12), v_depth=10, buttons=suit_r[0], button_rows=(12, 16))

    def jacket(f, x, y, w, h):
        if pocket and f == "north" and 4 <= y <= 5 and 12 <= x <= 14:
            return pocket[0] if y == 4 else pocket[1]
        return front(f, x, y, w, h)
    m.cube("body", (-TW, HIP - 0.6, -TD), (2 * TW, NECK - HIP + 0.4, 2 * TD), none_on(("down",), jacket), inflate=0.25, density=2,
           tag="jacket")
    m.cube(tie_bone, (-0.7, NECK - 2.2, -TD - 0.55), (1.4, 1.1, 0.5), lambda f, x, y, w, h: tone(tie_r, 3.4 if f != "up" else 4),
           density=4, tag="tie_knot")
    m.cube(tie_bone, (-0.65, NECK - 7.6, -TD - 0.42), (1.3, 5.5, 0.3), tie, density=4, tag="tie_blade")
    for s in (-1, 1):
        m.cube("body", (0.95 * s - 0.8, NECK - 1.3, -TD - 0.45), (1.6, 1.25, 0.4), lambda f, x, y, w, h: tone(shirt_r, 4.4 if y == 0 else 3.6),
               density=2, tag="shirt_collar", rotation=(14, 0, -30 * s), pivot=(0.95 * s, NECK - 0.2, -TD - 0.3))

        def lapel(f, x, y, w, h, s=s):
            c = tone(suit_r, 3.9 - 1.0 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                return suit_r[5]
            return c
        m.cube("body", (2.7 * s - 1.0, NECK - 6.8, -TD - 0.5), (2.0, 6.0, 0.35), lapel, density=2, tag="jacket_lapel",
               rotation=(2, 0, 13 * s), pivot=(1.9 * s, NECK - 6.8, -TD - 0.4))


def coat_m(seed, base=3.0):
    """Black wool: soft vertical folds, a faint nap."""
    cl = fabric(COAT, seed, base, 0.35)

    def m(f, x, y, w, h):
        c = cl(f, x, y, w, h)
        if h01("nap", seed, f, x, y) < 0.06:
            c = shade(c, 1.12)
        return c
    return m


def build_coat(m, seed=35300):
    coat = coat_m(seed)
    # The coat over the shoulders and the back: open in front (only its two fronts, beside the jacket, show there).
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            if x < 4 or x > w - 5:
                c = coat(f, x, y, w, h)
                if x in (3, w - 4):
                    return COAT[4]                       # the fronts' edges catching the light
                return c
            return None
        return coat(f, x, y, w, h)
    m.cube("coat", (-TW, HIP + 0.6, -TD), (2 * TW, NECK - HIP - 0.4, 2 * TD), shell, inflate=0.6, density=2, tag="coat_shell")
    # Notch lapels laid back over the coat's fronts.
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return COAT[0]
            c = tone(COAT, 4.2 - 1.6 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                return COAT[5]
            return c
        m.cube("coat", (3.6 * s - 1.2, NECK - 8.6, -TD - 0.95), (2.4, 8.0, 0.4), lapel, density=2, tag="coat_lapel",
               rotation=(3, 0, 8 * s), pivot=(3.2 * s, NECK - 8.6, -TD - 0.8))
        m.cube("coat", (3.9 * s - 0.9, NECK - 1.4, -TD - 1.05), (1.8, 1.0, 0.4), solid(COAT[4]), density=2, tag="coat_notch",
               rotation=(0, 0, -30 * s), pivot=(3.9 * s, NECK - 1.0, -TD - 0.9))
        m.cube("coat", (s * (TW + 0.3) - 1.3, NECK - 0.9, -TD - 0.5), (2.6, 0.8, 2 * TD + 1.0),
               lambda f, x, y, w, h: tone(COAT, 4.0 if f == "up" else 3.0), density=2, tag="coat_shoulder")
    # The turned-down collar round the back of the neck.
    def collar(f, x, y, w, h):
        if f == "down":
            return COAT[0]
        return tone(COAT, 4.1 - 1.2 * y / max(1, h))
    m.cube("collar", (-3.4, NECK - 1.6, 0.4), (6.8, 2.2, 0.5), collar, density=2, tag="collar_back", rotation=(14, 0, 0),
           pivot=(0, NECK - 0.2, 0.8))
    for s in (-1, 1):
        m.cube("collar", (s * 3.3 - 0.25, NECK - 1.6, -1.8), (0.5, 2.0, 2.6), collar, density=2, tag="collar_side",
               rotation=(0, 0, -12 * s), pivot=(s * 3.3, NECK - 0.2, 0))
    # The skirt: two fronts and the tail, from the waist to just below the knee.
    L = HIP + 0.6 - HEM
    for bone, s in (("coat_r", -1), ("coat_l", 1)):
        def panel(f, x, y, w, h, s=s):
            if f == "south":
                return COAT[1]
            c = coat(f, x, y + 18, w, h + 18)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return COAT[4]
            if f == "north" and 2 <= y <= 3 and 2 <= (w - 1 - x if s < 0 else x) <= w - 3:
                return COAT[0] if y == 3 else COAT[4]       # flap pocket
            return c
        xa, xb = (-TW - 0.7, -0.6) if s < 0 else (0.6, TW + 0.7)
        m.cube(bone, (xa, HEM, -TD - 0.75), (xb - xa, L, 0.5), panel, density=2, tag="coat_front", rotation=(-4, 0, 3 * s),
               pivot=(0, HIP + 0.6, -TD - 0.5))
        xs = -TW - 0.75 if s < 0 else TW + 0.25
        m.cube(bone, (xs, HEM, -TD - 0.5), (0.5, L, TD + 0.5), none_on(("up",), coat), density=2, tag="coat_side",
               rotation=(0, 0, 3 * s), pivot=(xs, HIP, 0))

    def back(f, x, y, w, h):
        if f == "north":
            return COAT[0]
        if f == "south" and abs(x - (w - 1) / 2) < 0.6 and y > h * 0.55:
            return COAT[0]                                 # the vent
        return coat(f, x, y + 18, w, h + 18)
    m.cube("coat_tail", (-TW - 0.75, HEM, TD + 0.25), (2 * TW + 1.5, L, 0.5), back, density=2, tag="coat_back", rotation=(4, 0, 0),
           pivot=(0, HIP + 0.6, TD + 0.4))
    for s in (-1, 1):
        xs = -TW - 0.75 if s < 0 else TW + 0.25
        m.cube("coat_tail", (xs, HEM, 0), (0.5, L, TD + 0.5), none_on(("up",), coat), density=2, tag="coat_back_side",
               rotation=(4, 0, 3 * s), pivot=(xs, HIP, TD))


def build_arms(m, sleeve=None, cuff=None, seed=35400, hand=None, cuff_depth=1.3):
    """Sleeves (the coat's by default), a deep cuff, the hands with their four finger bones."""
    sleeve = sleeve or coat_m(seed, 2.9)
    hand = hand or H.skin_mat(SKIN, seed + 1, 3.1, 0.4)
    cuff = cuff or (lambda f, x, y, w, h: tone(COAT, 3.8 - 1.0 * y / max(1, h)) if f not in ("up", "down") else COAT[0])
    for side, s in SIDES:
        cx = AX * s
        m.cube(f"{side}_arm", (cx - AW / 2, ELBOW, -AW / 2), (AW, SY + 0.8 - ELBOW, AW), sleeve, inflate=0.3, density=2, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.8, WRIST + 0.4, -1.8), (3.6, ELBOW - WRIST - 0.4, 3.6), none_on(("up",), sleeve), inflate=0.3,
               density=2, tag="forearm_sleeve")
        m.cube(f"{side}_forearm", (cx - 1.8, WRIST + 0.1, -1.8), (3.6, cuff_depth, 3.6), cuff, inflate=0.45, density=2, tag="cuff")
        m.cube(f"{side}_hand", (cx - 1.3, PALM, -1.5), (2.6, WRIST - PALM + 0.2, 3.0), hand, density=2, tag="palm")
        fing = lambda f, x, y, w, h: shade(hand(f, x, y, w, h), 0.88 if (f in ("east", "west") and x % 2) else 1.0)
        m.cube(f"{side}_fingers", (cx - 1.25, PALM - 1.1, -0.1), (2.5, 1.1, 1.5), fing, density=2, tag="fingers")
        m.cube(f"{side}_middle", (cx - 1.25, PALM - 1.25, -0.8), (2.5, 1.25, 0.7), fing, density=4, tag="middle")
        m.cube(f"{side}_index", (cx - 1.25, PALM - 1.15, -1.5), (2.5, 1.15, 0.7), fing, density=4, tag="index")
        m.cube(f"{side}_thumb", (cx - 0.9 * s - 0.5, PALM + 0.2, -2.05), (1.0, 1.6, 0.9), hand, density=2, tag="thumb")


def build_legs(m, trouser=SUIT, shoe=SHOE, seed=35500, bare=None):
    for side, s in SIDES:
        tr = fabric(trouser, seed + (s > 0), 2.9, 0.25, crease=True)
        x0 = LX * s - LW / 2
        m.cube(f"{side}_leg", (x0, KNEE, -LW / 2), (LW, HIP - KNEE, LW), tr, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, SHOE_H if not bare else 0, -LW / 2), (LW, KNEE - (SHOE_H if not bare else 0), LW),
               tr if not bare else bare, density=2, tag="shin")
        if bare:
            m.cube(f"{side}_shin", (x0 + 0.1, 0, -LW / 2 - 0.9), (LW - 0.2, 0.9, 1.0), bare, density=2, tag="toes")
            continue

        def shoe_m(f, x, y, w, h):
            c = tone(shoe, 2.4 - 0.8 * y / max(1, h))
            if f == "up" or (f in ("north", "east", "west") and y == 0):
                c = shoe[4]
            if f == "up" and (x + y) % 5 == 0:
                c = shoe[5]
            return c
        m.cube(f"{side}_shin", (x0 - 0.1, 0, -2.9), (LW + 0.2, SHOE_H, 4.9), shoe_m, density=2, tag="shoe")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m)
    build_head(m)
    build_suit(m)
    build_coat(m)
    build_arms(m)
    build_legs(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

Clip = H.Clip


def merge(*ps):
    out = {}
    for p in ps:
        out.update(p)
    return out


def jitter(seed, t, amp):
    return [amp * (2 * h01(seed, t, i) - 1) for i in range(3)]


# Standing at ease: hands loosely clasped in front, weight back, chin up.
IDLE = {"right_arm": [-10, 0, -8], "left_arm": [-10, 0, 8], "right_forearm": [-42, -22, 0], "left_forearm": [-42, 22, 0],
        "right_fingers": [20, 0, 0], "left_fingers": [20, 0, 0], "head": [-4, 0, 0], "body": [-2, 0, 0]}
# Seated, legs crossed (the left over the right knee), right hand on the armrest, left on his knee. His feet are on the seat
# (the entity stands on the slab or stair it sits on): the hips sit at his feet's level, the thighs level, the shins hanging.
THRONE = {"@root": [0, -10.0, 0], "body": [-8, 0, 0], "head": [-4, 6, 0],
          "right_leg": [-88, 8, 3], "right_shin": [84, 0, 0], "left_leg": [-104, -24, -4], "left_shin": [76, 4, 14],
          "right_arm": [-22, 0, 20], "right_forearm": [-42, 0, 0], "right_hand": [0, 0, -10], "right_fingers": [50, 0, 0],
          "left_arm": [-30, 0, -4], "left_forearm": [-34, 22, 0], "left_fingers": [30, 0, 0],
          "coat_r": [-84, 0, 4], "@coat_r": [0, 1.4, 0.4], "coat_l": [-92, -18, -2], "@coat_l": [0, 2.0, 0.4], "tie": [8, 0, 0]}
# On one knee (the right), the left foot planted, a hand on that knee, head up.
KNEEL = {"@root": [0, -5.4, 0], "body": [12, 0, 0], "head": [-16, 0, 0],
         "right_leg": [6, 0, 2], "right_shin": [86, 0, 0], "left_leg": [-84, -6, -2], "left_shin": [82, 0, 0],
         "right_arm": [-6, 0, 10], "right_forearm": [-14, 0, 0], "left_arm": [-44, 0, -10], "left_forearm": [-34, 0, 0],
         "left_fingers": [40, 0, 0], "coat_r": [2, 0, 2], "coat_l": [-70, -6, 0], "@coat_l": [0, 1.2, 0], "coat_tail": [30, 0, 0],
         "tie": [-10, 0, 0]}


def coat_sway(t, amp=1.0):
    ph = t * 2 * math.pi
    return {"coat_tail": [3 * amp * math.sin(ph), 0, 1.5 * amp * math.sin(ph * 0.5)],
            "coat_r": [-2 * amp * math.sin(ph + 0.6), 0, 0], "coat_l": [-2 * amp * math.sin(ph + 1.2), 0, 0]}


def anims():
    f = AnimFile()
    # --- loops ---------------------------------------------------------------------------------------------------
    idle = Clip(f, A + "idle", 4.0, loop=True)
    for t in (0, 1.0, 2.0, 3.0, 4.0):
        b = math.sin(t / 4.0 * 2 * math.pi)
        idle.key(t, merge(IDLE, coat_sway(t / 4.0, 0.6), {"@body": [0, -0.2 * (1 - math.cos(t / 4.0 * 2 * math.pi)), 0],
                                                          "head": [-4 + 2 * b, 8 * b, -2 * b], "brows": [0, 0, 0]}),
                 E_IO if t else None)
    idle.done()

    walk = Clip(f, A + "walk", 1.0, loop=True)
    for t, s in ((0, 1), (0.5, -1), (1.0, 1)):
        walk.key(t, {
            "right_leg": [22 * s, 0, 0], "left_leg": [-22 * s, 0, 0], "right_shin": [6 if s < 0 else 18, 0, 0],
            "left_shin": [18 if s < 0 else 6, 0, 0], "right_arm": [-14 * s, 0, 4], "left_arm": [14 * s, 0, -4],
            "right_forearm": [-16, 0, 0], "left_forearm": [-16, 0, 0], "body": [-3, 7 * s, 0], "head": [-5, -6 * s, 0],
            "coat_r": [-10 * s, 0, 0], "coat_l": [10 * s, 0, 0], "coat_tail": [8, 0, 2 * s], "tie": [-3, 0, 3 * s]},
                 E_IO if t else None)
    walk.anim.pos("body", (0, [0, 0, 0]), (0.25, [0, 0.5, 0], E_OUT), (0.5, [0, 0, 0], E_IN), (0.75, [0, 0.5, 0], E_OUT),
                  (1.0, [0, 0, 0], E_IN))
    walk.done()

    ti = Clip(f, A + "throne_idle", 4.0, loop=True)
    for t in (0, 1.0, 2.0, 3.0, 4.0):
        b = math.sin(t / 4.0 * 2 * math.pi)
        tap = 6 * max(0.0, math.sin(t / 4.0 * 4 * math.pi))
        ti.key(t, merge(THRONE, {"head": [-2 + 2 * b, 6 + 10 * b, 0], "left_shin": [70 + tap, 0, 18],
                                 "right_fingers": [50 - 20 * max(0.0, b), 0, 0]}), E_IO if t else None)
    ti.done()

    ki = Clip(f, A + "kneel_idle", 2.0, loop=True)
    for t in (0, 0.5, 1.0, 1.5, 2.0):
        b = math.sin(t / 2.0 * 2 * math.pi)
        ki.key(t, merge(KNEEL, {"body": [12 + 3 * b, 0, 0], "head": [-16 - 2 * b, 3 * b, 0], "@body": [0, 0.15 * b, 0]}),
               E_IO if t else None)
    ki.done()

    # --- his gestures ---------------------------------------------------------------------------------------------
    # Snap: the right hand comes up beside his face, thumb and middle finger, the snap on tick 8.
    sn = Clip(f, A + "snap", 0.8)
    sn.key(0, IDLE)
    raised = lambda hz, th, mid: {"right_arm": [-36, 0, 48], "right_forearm": [-92, 10, 0], "right_hand": [0, 0, hz],
                                  "right_thumb": [th, 0, 0], "right_middle": [mid, 0, 0], "right_index": [30, 0, 0],
                                  "right_fingers": [70, 0, 0], "head": [-8, -12, 0], "body": [0, -6, 0],
                                  "left_arm": [-8, 0, 6], "left_forearm": [-30, 0, 0], "brows": [0, 0, 0]}
    sn.key(0.25, raised(0, -20, 40), E_OUT)
    sn.key(0.36, raised(-6, -30, 50))
    sn.key(0.4, raised(16, 20, 95), E_SNAP)
    sn.key(0.55, raised(10, 10, 85))
    sn.key(0.8, IDLE, E_IO)
    sn.done()

    # Point: the arm out at you, index extended, on tick 10.
    pt = Clip(f, A + "point", 1.1)
    aimed = lambda k: {"right_arm": [-86 * k, 4 * k, 6 * k], "right_forearm": [-4 * k, 0, 0], "right_hand": [-6 * k, 0, 0],
                       "right_index": [0, 0, 0], "right_middle": [80 * k, 0, 0], "right_fingers": [80 * k, 0, 0],
                       "right_thumb": [20 * k, 0, 0], "body": [0, 10 * k, 0], "head": [-6 * k, -6 * k, 0],
                       "left_arm": [-6, 0, 6], "left_forearm": [-24, 0, 0], "coat_r": [-6 * k, 0, 0]}
    pt.key(0, IDLE)
    pt.key(0.3, merge(aimed(0.55), {"right_forearm": [-60, 0, 0]}), E_OUT)
    pt.key(0.5, aimed(1.0), E_BACK)
    pt.key(0.85, aimed(0.95))
    pt.key(1.1, IDLE, E_IO)
    pt.done()

    # Dismiss: a backhanded wave across and away ("off you go"), the flick on tick 12; his head turns aside.
    dm = Clip(f, A + "dismiss", 1.2)
    dm.key(0, IDLE)
    dm.key(0.4, {"right_arm": [-40, 0, -24], "right_forearm": [-70, 30, 0], "right_hand": [0, 0, 30], "right_fingers": [20, 0, 0],
                 "body": [0, -10, 0], "head": [-4, 6, 0], "left_arm": [-8, 0, 6], "left_forearm": [-30, 0, 0]}, E_IO)
    dm.key(0.6, {"right_arm": [-48, 0, 38], "right_forearm": [-30, -10, 0], "right_hand": [0, 0, -40], "right_fingers": [10, 0, 0],
                 "body": [0, 12, 0], "head": [-12, 24, 0], "left_arm": [-8, 0, 6], "left_forearm": [-30, 0, 0],
                 "coat_r": [-6, 0, 0]}, E_SNAP)
    dm.key(0.85, {"right_arm": [-36, 0, 34], "right_forearm": [-22, 0, 0], "right_hand": [0, 0, -30], "body": [0, 8, 0],
                  "head": [-10, 26, 0], "left_arm": [-8, 0, 6], "left_forearm": [-30, 0, 0]}, E_OUT)
    dm.key(1.2, IDLE, E_IO)
    dm.done()

    # Talk: an open hand turned up as he makes his point, a nod on tick 10.
    tk = Clip(f, A + "talk", 1.5)
    tk.key(0, IDLE)
    palm = lambda k, hz: {"right_arm": [-24 * k, 0, 10 * k], "right_forearm": [-62 * k, -10, 0], "right_hand": [0, -60 * k, hz],
                          "right_fingers": [10, 0, 0], "head": [-6 + 4 * k, -10 * k, 4 * k], "body": [0, 4 * k, 0],
                          "left_arm": [-10, 0, 8], "left_forearm": [-42, 22, 0]}
    tk.key(0.3, palm(1.0, -10), E_IO)
    tk.key(0.5, merge(palm(1.0, 6), {"head": [8, -8, 2]}), E_OUT)
    tk.key(0.8, palm(0.9, -6), E_IO)
    tk.key(1.1, merge(palm(1.1, 4), {"head": [0, 6, -2]}), E_IO)
    tk.key(1.5, IDLE, E_IO)
    tk.done()

    # Sitting down: turns to the seat and lowers himself (tick 16), then crosses his legs.
    seat_flat = merge(THRONE, {"left_leg": [-88, -8, -3], "left_shin": [84, 0, 0], "coat_l": [-84, 0, -4], "@coat_l": [0, 1.4, 0.4]})
    half = {"@root": [0, -5.0, 1.2], "body": [22, 0, 0], "head": [-14, 0, 0], "right_leg": [-48, 6, 2], "right_shin": [62, 0, 0],
            "left_leg": [-48, -6, -2], "left_shin": [62, 0, 0], "right_arm": [-20, 0, 22], "right_forearm": [-30, 0, 0],
            "left_arm": [-20, 0, -22], "left_forearm": [-30, 0, 0], "coat_r": [-40, 0, 4], "coat_l": [-40, 0, -4],
            "@coat_r": [0, 0.8, 0], "@coat_l": [0, 0.8, 0], "coat_tail": [26, 0, 0]}
    ts = Clip(f, A + "throne_sit", 1.2)
    ts.key(0, IDLE)
    ts.key(0.45, half, E_IO)
    ts.key(0.8, seat_flat, E_OUT)
    ts.key(1.2, THRONE, E_IO)
    ts.done()
    ts.anim.scale("coat_tail", (0, [1, 1, 1]), (0.7, [1, 1, 1]), (0.8, [1, 0, 1]), (1.2, [1, 0, 1]))

    tr = Clip(f, A + "throne_rise", 1.1)
    tr.key(0, THRONE)
    tr.key(0.3, seat_flat, E_IO)
    tr.key(0.55, merge(half, {"body": [26, 0, 0]}), E_IO)
    tr.key(0.7, merge(IDLE, {"body": [4, 0, 0]}), E_OUT)
    tr.key(0.9, {"right_arm": [-22, 0, -18], "left_arm": [-22, 0, 18], "right_forearm": [-70, -40, 0], "left_forearm": [-70, 40, 0],
                 "head": [12, 0, 0]}, E_IO)
    tr.key(1.1, IDLE, E_IO)
    tr.done()
    tr.anim.scale("coat_tail", (0, [1, 0, 1]), (0.3, [1, 0, 1]), (0.4, [1, 1, 1]), (1.1, [1, 1, 1]))
    ti.anim.scale("coat_tail", (0, [1, 0, 1]), (4.0, [1, 0, 1]))

    # Vanish: a shrug (shoulders up, palms out, eyebrows), gone on tick 14.
    va = Clip(f, A + "vanish", 1.0, hold=True)
    shrug = lambda k: {"right_arm": [-12 * k, 0, 26 * k], "left_arm": [-12 * k, 0, -26 * k], "right_forearm": [-70 * k, 30 * k, 0],
                       "left_forearm": [-70 * k, -30 * k, 0], "right_hand": [0, 70 * k, 0], "left_hand": [0, -70 * k, 0],
                       "head": [-10 * k, 0, 12 * k], "@body": [0, 0.5 * k, 0], "@brows": [0, 0.4 * k, 0], "body": [-4 * k, 0, 0]}
    va.key(0, IDLE)
    va.key(0.4, shrug(1.0), E_OUT)
    va.key(0.6, shrug(1.1))
    va.key(0.7, shrug(0.9), E_SNAP)
    va.key(1.0, shrug(0.9))
    va.done()

    # Appear: there he is, tugging his tie straight (tick 6) and smoothing a lapel.
    ap = Clip(f, A + "appear", 0.8)
    tug = lambda d: {"right_arm": [-20, 0, -26], "right_forearm": [-104, -38, 0], "right_hand": [0, 0, d], "right_fingers": [40, 0, 0],
                     "right_index": [30, 0, 0], "right_thumb": [-20, 0, 0], "left_arm": [-24, 0, 6], "left_forearm": [-76, 20, 0],
                     "left_hand": [0, 0, 20], "head": [14, 0, 0], "tie": [0, 0, d * 0.3]}
    ap.key(0, merge(tug(0), {"@root": [0, 0.3, 0], "head": [-10, 0, 0]}))
    ap.key(0.3, tug(-10), E_OUT)
    ap.key(0.45, tug(12))
    ap.key(0.6, merge(IDLE, {"head": [-8, 0, 0]}), E_IO)
    ap.key(0.8, IDLE, E_IO)
    ap.done()

    # Kneel: beaten, he goes down onto his right knee (tick 16) and looks up.
    kn = Clip(f, A + "kneel", 1.2)
    kn.key(0, IDLE)
    kn.key(0.35, {"body": [26, 0, 4], "head": [20, 0, 0], "right_arm": [-30, 0, -30], "left_arm": [-30, 0, 30],
                  "right_forearm": [-70, -20, 0], "left_forearm": [-70, 20, 0], "right_leg": [6, 0, 0], "left_leg": [-20, 0, 0],
                  "@root": [0, -0.8, 0]}, E_OUT)
    kn.key(0.8, merge(KNEEL, {"body": [30, 0, 0], "head": [20, 0, 0], "@root": [0, -5.9, 0]}), E_IN)
    kn.key(1.2, KNEEL, E_IO)
    kn.done()

    # Death: the blade goes in -- he doubles, then the light: head back, arms out (tick 50), and he slumps to his knees.
    dt = Clip(f, A + "death", 4.0, hold=True)
    dt.key(0, IDLE)
    stab = {"body": [24, 0, 0], "head": [16, 0, 0], "right_arm": [-40, 0, -30], "left_arm": [-40, 0, 30],
            "right_forearm": [-60, -30, 0], "left_forearm": [-60, 30, 0], "@root": [0, -0.6, 0.8], "right_leg": [-8, 0, 0]}
    dt.key(0.3, stab, E_SNAP)
    for i in range(1, 9):
        t = round(0.3 + 0.2 * i, 2)
        k = (t - 0.3) / 1.8
        j = jitter("crowley-death", i, 3 + 6 * k)
        dt.key(t, merge(stab, {"body": [24 - 40 * k + j[0], j[1], j[2]], "head": [16 - 50 * k + j[2], j[0], j[1]],
                               "right_arm": [-40 + 30 * k, 0, -30 + 100 * k], "left_arm": [-40 + 30 * k, 0, 30 - 100 * k],
                               "right_forearm": [-60 + 50 * k, 0, 0], "left_forearm": [-60 + 50 * k, 0, 0],
                               "@root": [0, -0.6 + 0.8 * k, 0.8]}))
    flare = {"body": [-18, 0, 0], "head": [-46, 0, 0], "right_arm": [-14, 0, 80], "left_arm": [-14, 0, -80],
             "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "right_hand": [0, 0, 20], "left_hand": [0, 0, -20],
             "right_fingers": [-10, 0, 0], "left_fingers": [-10, 0, 0], "@root": [0, 0.4, 0], "coat_r": [-14, 0, -10],
             "coat_l": [-14, 0, 10], "coat_tail": [16, 0, 0], "tie": [-24, 0, 0]}
    dt.key(2.5, flare, E_BACK)
    dt.key(2.9, merge(flare, {"head": [-50, 4, 0]}))
    slump = {"@root": [0, -5.8, 0], "right_leg": [-4, 0, 4], "left_leg": [-4, 0, -4], "right_shin": [88, 0, 0], "left_shin": [88, 0, 0],
             "body": [36, 0, 6], "head": [44, 0, -12], "right_arm": [-8, 0, 12], "left_arm": [-4, 0, -14],
             "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "coat_r": [-10, 0, 0], "coat_l": [-10, 0, 0],
             "coat_tail": [40, 0, 0], "tie": [-30, 0, 0]}
    dt.key(3.5, slump, E_IN)
    dt.key(4.0, slump)
    dt.done()
    return f


# =====================================================================================================
# The Contract Book
# =====================================================================================================

def contract_book():
    """The Contract Book: black leather with a crown stamped in gold, a red ribbon hanging from its pages."""
    pal = {"o": hexc("#050406"), "d": hexc("#0f0d10"), "l": hexc("#1b181c"), "L": hexc("#262228"), "h": hexc("#38323a"),
           "p": hexc("#cfc2a0"), "P": hexc("#a8987a"), "r": hexc("#9a0e12"), "R": hexc("#d8241e"), "k": hexc("#4a0606"),
           "g": GOLD[2], "G": GOLD[4], "W": GOLD[5]}
    return _stamp_crown(art([
        "................",
        "..oooooooooooo..",
        ".ohhhhhhhhhhhhPo",
        ".ohLLLLLLLLLLLpo",
        ".ohLLLLLLLLLLLPo",
        ".ohLLLLLLLLLLLpo",
        ".ohLLLLLLLLLLLPo",
        ".ohLLLLLLLLLLLpo",
        ".ohLLLLLLLLLLLPo",
        ".ohLLLLLLLLLLLpo",
        ".ohLLLLLLLLLLLPo",
        ".ohLlLLLLLLLlLpo",
        ".ohddddddddddoPo",
        "..oooooooRoooo..",
        ".........rR.....",
        ".........kr.....",
    ], pal), pal)


def _stamp_crown(t, pal):
    crown = ["g.G.G.g",
             "gGgGgGg",
             "GWGWGWG",
             "ggggggg"]
    for j, row in enumerate(crown):
        for i, ch in enumerate(row):
            if ch != ".":
                t.set(5 + i, 5 + j, pal[ch])
    # A thin gold rule round the cover.
    for x in range(4, 13):
        t.set(x, 3, pal["g"] if x % 2 else pal["L"])
        t.set(x, 11, pal["g"] if x % 2 else pal["L"])
    return t


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=35000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    write_compact(anims(), ANIM + NAME + ".animation.json")
    save(contract_book(), "item", "contract_book")


if __name__ == "__main__":
    generate()
