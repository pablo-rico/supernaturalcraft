"""Abaddon, Knight of Hell (v0.21): the boss's rig, texture + glowmask and clips (names in AbaddonAssets), the Book of the
Damned's sprite and her bust on the trophies' pedestal. Her retinue and the Knights' Cleaver are in abaddon_retinue_art.

Josie Sands' body worn like a uniform: pale skin, red lips, long deep-red hair in 1940s waves (a victory roll over her
right brow, the waves falling past her shoulders, two locks framing her face), the eyes solid black with a red rim that
burns (glowmask). A long black leather greatcoat, double-breasted with gunmetal buttons, wide peak lapels, a standing
collar flared behind her neck, shoulder boards, turned-back cuffs; belted at the waist with a blood-iron buckle (the
belt's tail hangs loose); its skirt open from the waist in front, to mid-calf, swinging with her stride. A crimson silk
blouse at the throat, a black ribbon choker with a ruby drop, garnet earrings, a ring with a hell-sigil on her right hand,
dark red nails; black trousers, tall black riding boots with a heel.

  brand   the seal on her right palm: a shell tucked inside the hand at rest; `brand` slides it out (glowmask red-orange).
  mouth   her open mouth, tucked inside the head at rest; the snarling clips slide it forward.

Skeleton (witchkit's woman without the gown, plus):
  root -- body -- head -- hair -- roll;  hair_back -- hair_tail;  curl_r, curl_l, brows, mouth, earring_r, earring_l
               -- collar, pendant, belt_tail
               -- right_arm -- right_forearm -- cuff_r;  right_hand -- fingers, index, thumb, ring_r, brand
               -- left_arm  -- left_forearm  -- cuff_l;  left_hand  -- fingers, index, thumb
               -- coat_r -- coat_r_lower;  coat_l -- coat_l_lower;  coat_back -- coat_back_lower
       -- right_leg -- right_shin,  left_leg -- left_shin
"""

import math

import blockmodel as BM
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import art, save
from legacy_art import SIDES, done, leather_m, reach as _reach
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, Clip, aim, to_preview, tone, world_point,
                         write_compact)
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade
from witchkit import (CLAW_L, CLAW_R, CURL_L, CURL_R, FIST_L, FIST_R, GRACE_L, GRACE_R, OPEN_L, OPEN_R, POINT_R, REST, TWW,
                      WAIST, WB, face_head, gem, glow_k, nose_ears, satin, slim_hands, wavy_hair, wskeleton)

A = "animation.abaddon."
NAME = "abaddon"

SKIN = Ramp("#6e4c46", "#a07a70", "#c39d92", "#dbb8ad", "#e9cdc3", "#f5e2da")
HAIR = Ramp("#1f0405", "#38080a", "#550e0e", "#721713", "#902318", "#ae331f")
COAT = Ramp("#08070a", "#110f12", "#1b181c", "#272327", "#363036", "#4b434a")
CRIMSON = Ramp("#22030a", "#3d0610", "#5c0b18", "#7c1222", "#9c1c2d", "#bf2c3c")
GUNMETAL = Ramp("#14161a", "#22252b", "#33373f", "#474c56", "#5e6470", "#7f8693")
IRON = Ramp("#1a0a08", "#2e120d", "#471c13", "#62281a", "#7e3823", "#9c4c30")
RUBY = Ramp("#2a0206", "#55050d", "#850b17", "#b81627", "#e0384a", "#ff8a92")
LIPS = Ramp("#3a0508", "#58090e", "#760f15", "#93161d", "#ad2228", "#c63a3c")
TROUSER = Ramp("#060607", "#0c0c0e", "#141417", "#1d1d21", "#28282d", "#36363c")
BOOT = Ramp("#040404", "#0a0909", "#131111", "#1d1a1a", "#2b2626", "#3e3737")
EYE_BLACK = hexc("#050304")
RIM = hexc("#5a0a0a")
RIM_GLOW = hexc("#e0201a")
BRAND = hexc("#ff5a1e")
BRAND_HOT = hexc("#ffd08a")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhsssshhh",
    "hhhhssssssssssshh",
    "hsbBBBbsssbBBBbh",
    "hsKKKKssssKKKKsh",
    "hsRXQXRssRXQXRsh",
    "hssrrrssssrrrssh",
    "hssssssnnssssssh",
    "hcjssssnnssssjch",
    "hsjssssNNssssjsh",
    "hssjssssssssjssh",
    "hssssslMMlsssssh",
    "hjsssssLLsssssjh",
    "hjjssssssssssjjh",
]


# =====================================================================================================
# The rig
# =====================================================================================================

def rig():
    m = Model(NAME, 256, 256)
    b = WB
    wskeleton(m, b, skirt=False)
    y0 = b.neck
    m.bone("hair", (0, y0 + 4, 0), "head")
    m.bone("roll", (-1.6, y0 + 7.6, -2.6), "hair")
    m.bone("hair_back", (0, y0 + 5.0, 3.6), "head")
    m.bone("hair_tail", (0, y0 - 2.6, 4.2), "hair_back")
    m.bone("curl_r", (-3.7, y0 + 3.0, -2.2), "head")
    m.bone("curl_l", (3.7, y0 + 3.0, -2.2), "head")
    m.bone("brows", (0, y0 + 5.3, -4.0), "head")
    m.bone("mouth", (0, y0 + 2.2, -4.0), "head")
    for side, s in SIDES:
        m.bone(f"earring_{side[0]}", (4.05 * s, y0 + 2.4, -0.2), "head")
        m.bone(f"cuff_{side[0]}", (b.ax * s, b.wrist + 1.4, 0), f"{side}_forearm")
    m.bone("ring_r", (-b.ax, b.palm, 0), "right_hand")
    m.bone("brand", (-b.ax + 1.05, b.palm + 0.8, -0.1), "right_hand")
    m.bone("collar", (0, y0 - 0.2, 0.6), "body")
    m.bone("pendant", (0, y0 - 0.6, -2.0), "body")
    m.bone("belt_tail", (-1.6, WAIST - 0.4, -2.3), "body")
    m.bone("coat_r", (-1.8, b.hip, -1.2), "body")
    m.bone("coat_r_lower", (-2.2, b.knee, -1.6), "coat_r")
    m.bone("coat_l", (1.8, b.hip, -1.2), "body")
    m.bone("coat_l_lower", (2.2, b.knee, -1.6), "coat_l")
    m.bone("coat_back", (0, b.hip, 1.6), "body")
    m.bone("coat_back_lower", (0, b.knee, 2.2), "coat_back")
    build_head(m)
    build_body(m)
    build_arms(m)
    build_coat(m)
    build_legs(m)
    return m


def build_head(m):
    b = WB
    y0 = b.neck
    sk = H.skin_mat(SKIN, 23101, 3.4, 0.3)
    hr = wavy_hair(HAIR, 23102, 2.7, wave=1.2, shine=0.8)

    def rim_glow(f, x, y, w, h, c):
        if c in (RIM, hexc("#3a0a0a")):
            return RIM_GLOW if c == RIM else hexc("#6a0e0c")
        return None
    legend = {"h": hr, "s": sk, "b": HAIR[2], "B": HAIR[1], "K": hexc("#0e0607"), "X": EYE_BLACK, "Q": hexc("#2a2226"), "R": RIM,
              "r": hexc("#3a0a0a"), "n": SKIN[4], "N": SKIN[1], "c": mix(SKIN[3], LIPS[4], 0.15), "l": LIPS[1], "M": LIPS[3],
              "L": LIPS[4], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}

    def sides(f, x, y, w, h):
        if f in ("up", "south"):
            return hr(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x
        if fx < 3 and y >= 5:
            return sk(f, x, y, w, h)
        return hr(f, x, y, w, h)
    face_head(m, b, FACE, legend, sides, rim_glow)
    nose_ears(m, b, SKIN, ears=False)
    m.cube("body", (-1.2, y0 - 0.8, -1.2), (2.4, 1.2, 2.4), sk, density=2, tag="neck")
    # Brows: arched high and sharp.
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - 1.1, y0 + 5.1, -4.12), (2.2, 0.4, 0.2), lambda f, x, y, w, h: HAIR[1] if x % 3 else HAIR[0],
               density=4, tag="brow", rotation=(0, 0, 11 * s), pivot=(s * 1.0, y0 + 5.2, -4.1))

    def mouth(f, x, y, w, h):
        if y == 0:
            return hexc("#ece4d6") if 1 <= x <= w - 2 else LIPS[2]
        if y == h - 1:
            return LIPS[4]
        return hexc("#1a0405") if 1 <= x <= w - 2 else LIPS[2]
    m.cube("mouth", (-1.1, y0 + 1.1, -3.7), (2.2, 1.0, 0), mouth, faces=("north",), density=4, tag="mouth")

    # --- the hair: side-parted on her left, swept over in a victory roll on her right, waves to the shoulders ---
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            if y < 2 or (y < 4 and x > w * 0.55):
                return shade(hr(f, x, y, w, h), 1.06)
            return None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if fx > 2 or y < 4 else None
        c = hr(f, x, y, w, h)
        if f == "up" and abs(x - w * 0.72) < 0.6 and y < h * 0.5:
            return SKIN[2]                               # the side parting
        return c
    m.cube("hair", (-4, y0 + 3.0, -4), (8, 5.0, 8), shell, inflate=0.35, density=2, tag="hair_shell")
    # The swept wave over the crown (from the parting on her left across to the roll on her right).
    m.cube("hair", (-3.8, y0 + 7.2, -4.4), (6.4, 1.4, 5.0), none_on(("down",), hr), density=2, tag="hair_sweep",
           rotation=(0, 0, 8), pivot=(2.4, y0 + 7.6, -2.0))

    def coil(f, x, y, w, h):
        v = 2.9 + 0.9 * math.sin(y * 1.4 + x * 0.3) + (0.5 if f == "up" else 0) - (0.8 if f == "down" else 0)
        return tone(HAIR, v)
    # The victory roll: an octagonal barrel of hair along X over her right brow, rolled back.
    rx0, ry, rz = -3.9, y0 + 7.6, -2.6
    for rot in (0, 45):
        m.cube("roll", (rx0, ry - 1.25, rz - 1.25), (4.4, 2.5, 2.5), coil, density=2, tag="victory_roll", rotation=(rot, 0, -6),
               pivot=(rx0 + 2.2, ry, rz))
    # Locks framing her face, waves down to the collarbones.
    for side, s in SIDES:
        bone = f"curl_{side[0]}"
        for k in range(4):
            yy = y0 + 3.2 - k * 1.7
            m.cube(bone, (s * 3.9 - 0.9 + 0.35 * s * math.sin(k * 1.6), yy - 1.9, -3.2 + 0.2 * k), (1.8, 1.9, 2.2),
                   lambda f, x, y, w, h, k=k: tone(HAIR, 3.0 + (0.8 if (y == 0 and f != "down") else 0) - 0.2 * k + 0.5 * math.sin(x + k)
                                               - (0.7 if f == "south" else 0)),
                   density=2, tag="lock", rotation=(0, 0, 10 * s * (1 if k % 2 else -1)), pivot=(s * 3.9, yy, -2.2))
    # The fall down her back.
    def fall(f, x, y, w, h):
        if f in ("north", "up"):
            return None
        return hr(f, x, y, w, h)
    m.cube("hair_back", (-4.6, y0 - 2.6, 3.0), (9.2, 8.0, 1.9), fall, inflate=0.1, density=2, tag="hair_fall")
    for s in (-1, 1):
        m.cube("hair_back", (s * 4.2 - 0.85, y0 - 2.4, 0.2), (1.7, 6.8, 3.2), none_on(("up",), hr), density=2, tag="hair_side")

    def ends(f, x, y, w, h):
        if f == "up":
            return None
        if f != "down" and y >= h - 3 and (x * 5 + y * 3) % 7 < (y - (h - 4)) * 2:
            return None
        return hr(f, x, y + 18, w, h + 18)
    m.cube("hair_tail", (-4.4, y0 - 6.4, 3.3), (8.8, 4.0, 1.7), ends, density=2, tag="hair_ends", rotation=(-6, 0, 0),
           pivot=(0, y0 - 2.6, 4.2))
    # Garnet earrings.
    for side, s in SIDES:
        e = f"earring_{side[0]}"
        ex = 4.15 * s
        m.cube(e, (ex - 0.15, y0 + 1.7, -0.35), (0.3, 0.6, 0.3), solid(GUNMETAL[4]), density=4, tag="earring_hook")
        m.cube(e, (ex - 0.3, y0 + 0.8, -0.5), (0.6, 0.9, 0.6), gem(RUBY), glow_k(0.5), density=4, tag="earring_drop")


def coat_m(seed, base=2.8):
    lea = leather_m(COAT, seed, base)

    def m(f, x, y, w, h):
        c = lea(f, x, y, w, h)
        if f in ("north", "south") and h01("crease", seed, x // 2, y // 5) < 0.18 and y % 5 == 0:
            c = COAT[4]                                    # a crease catching the light
        return c
    return m


def build_body(m):
    b = WB
    coat = coat_m(23201)
    blouse = satin(CRIMSON, 23202, 3.0)
    # The coat's torso: double-breasted, the V open to the blouse, two columns of buttons.
    def front(f, x, y, w, h):
        c = coat(f, x, y, w, h)
        if f != "north":
            return c
        mid = (w - 1) / 2
        half = 2.4 * max(0.0, 1 - y / 9.0)
        if y < 9 and abs(x - mid) <= half + 0.5:
            return blouse(f, x, y, w, h)
        if y < 10 and abs(x - mid) <= half + 1.5:
            return COAT[0]
        if y in (11, 15, 19) and abs(abs(x - mid) - 2.2) < 0.6:
            return GUNMETAL[4] if x % 2 else GUNMETAL[3]
        if abs(x - (mid + 3.5)) < 0.5 and y >= 9:
            return COAT[1]                                 # the overlap seam
        return c
    m.cube("body", (-b.tw, WAIST - 0.2, -2.0), (2 * b.tw, b.neck - WAIST + 0.2, 4.0), front, inflate=0.3, density=2, tag="coat_chest")
    m.cube("body", (-TWW, b.hip, -1.75), (2 * TWW, WAIST - b.hip + 0.4, 3.5), coat, inflate=0.3, density=2, tag="coat_waist")
    # The bust under the coat.
    m.cube("body", (-2.8, 18.4, -2.6), (5.6, 2.6, 0.7), none_on(("south",), lambda f, x, y, w, h: front("north", x + 1, y + 8, 12, 14)
           if f == "north" else coat(f, x, y, w, h)), density=2, tag="bust", rotation=(-8, 0, 0), pivot=(0, 21.0, -2.2))
    # Peak lapels laid back over the chest.
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return COAT[0]
            c = tone(COAT, 4.0 - 1.4 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                c = COAT[5]
            return c
        m.cube("body", (1.9 * s - 1.1, 19.2, -2.62), (2.2, 5.2, 0.35), lapel, density=2, tag="lapel", rotation=(4, 0, 16 * s),
               pivot=(1.4 * s, 19.2, -2.5))
        m.cube("body", (3.0 * s - 0.8, 23.2, -2.66), (1.6, 1.2, 0.35), solid(COAT[4]), density=2, tag="lapel_peak",
               rotation=(0, 0, 40 * s), pivot=(3.0 * s, 23.2, -2.5))
        # Shoulder boards with a gunmetal button.
        m.cube("body", (s * (b.tw + 0.3) - 1.0, b.neck - 0.5, -1.4), (2.0, 0.45, 2.8), lambda f, x, y, w, h: COAT[4] if f == "up" else COAT[2],
               density=4, tag="shoulder_board")
        m.cube("body", (s * (b.tw - 0.2) - 0.25, b.neck - 0.15, -0.3), (0.5, 0.3, 0.6), solid(GUNMETAL[4]), density=4, tag="board_button")
    # The blouse's high neck and the ribbon choker with its ruby drop.
    m.cube("body", (-1.35, b.neck - 1.0, -1.35), (2.7, 1.0, 2.7), blouse, inflate=0.05, density=2, tag="blouse_neck")
    m.cube("pendant", (-1.3, b.neck - 0.75, -1.4), (2.6, 0.35, 2.8), lambda f, x, y, w, h: hexc("#0c0809"), inflate=0.1, density=4,
           tag="choker")
    m.cube("pendant", (-0.35, b.neck - 1.9, -1.65), (0.7, 1.0, 0.35), gem(RUBY), glow_k(0.5), density=4, tag="choker_drop")
    # The standing collar flared up behind her neck.
    def collar(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return CRIMSON[1] if y > 0 else COAT[4]          # the lining shows inside
        return tone(COAT, 3.6 - 1.2 * y / max(1, h))
    m.cube("collar", (-3.4, b.neck - 0.4, 0.4), (6.8, 2.8, 0.45), collar, density=2, tag="collar_back", rotation=(-18, 0, 0),
           pivot=(0, b.neck - 0.4, 0.6))
    for s in (-1, 1):
        m.cube("collar", (s * 3.4 - 0.22, b.neck - 0.4, -1.6), (0.45, 2.4, 2.4), collar, density=2, tag="collar_side",
               rotation=(0, 0, -16 * s), pivot=(s * 3.4, b.neck - 0.4, 0))
    # The belt, the buckle, the tail.
    def belt(f, x, y, w, h):
        if f in ("up", "down"):
            return None
        return COAT[4] if y == 0 else COAT[1]
    m.cube("body", (-TWW, WAIST - 1.2, -1.75), (2 * TWW, 1.1, 3.5), belt, inflate=0.48, density=2, tag="belt")
    m.cube("body", (-0.9, WAIST - 1.45, -2.38), (1.8, 1.5, 0.25), lambda f, x, y, w, h: IRON[4] if (x in (0, w - 1) or y in (0, h - 1))
           else (IRON[1] if f == "north" else IRON[3]), glow_k(0.08), density=4, tag="buckle")
    m.cube("belt_tail", (-2.2, WAIST - 4.4, -2.32), (1.0, 3.6, 0.2), lambda f, x, y, w, h: COAT[3] if x == 0 else COAT[1], density=4,
           tag="belt_tail")
    m.cube("belt_tail", (-2.2, WAIST - 4.6, -2.36), (1.0, 0.25, 0.25), solid(IRON[4]), density=4, tag="belt_tip")


def build_arms(m):
    b = WB
    hand = H.skin_mat(SKIN, 23301, 3.4, 0.3)
    coat = coat_m(23302, 2.9)
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        m.cube(f"{side}_arm", (cx - hw, b.elbow, -hw), (b.aw, b.sy + 0.6 - b.elbow, b.aw), coat, density=2, inflate=0.22, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - hw, b.wrist + 0.3, -hw), (b.aw, b.elbow - b.wrist - 0.1, b.aw), none_on(("up",), coat),
               density=2, inflate=0.2, tag="forearm")
        # The turned-back cuff: a wide band with a strap and button.
        def cuffm(f, x, y, w, h):
            if f in ("up", "down"):
                return COAT[0]
            if f == ("west" if s < 0 else "east") and y == 1 and x in (2, 3):
                return GUNMETAL[4]
            return tone(COAT, 3.7 - 1.0 * y / max(1, h))
        m.cube(f"cuff_{side[0]}", (cx - hw, b.wrist - 0.2, -hw), (b.aw, 1.6, b.aw), cuffm, inflate=0.38, density=2, tag="cuff")
    # Blouse cuffs peeking out.
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_hand", (cx - 1.15, b.wrist - 0.4, -1.3), (2.3, 0.6, 2.6), none_on(("up", "down"), lambda f, x, y, w, h: CRIMSON[3 if y else 4]),
               inflate=0.12, density=4, tag="blouse_cuff")
    slim_hands(m, b, hand, nails=hexc("#4a0a10"))
    # The ring: a gunmetal band, a black stone with a red hell-sigil.
    m.cube("ring_r", (-b.ax - 1.05, b.palm - 0.75, -1.35), (2.1, 0.35, 0.85), solid(GUNMETAL[3]), inflate=0.06, density=4, tag="ring")
    m.cube("ring_r", (-b.ax - 0.4, b.palm - 0.8, -1.6), (0.8, 0.55, 0.45), lambda f, x, y, w, h: hexc("#d0201a") if (f == "north" and
           (x + y) % 2 == 0) else hexc("#0c0809"), lambda f, x, y, w, h, c: c if c == hexc("#d0201a") else None, density=4, tag="ring_stone")
    # The brand: the seal on her palm (inner face of the right hand), tucked inside the hand at rest.
    def sigil(f, x, y, w, h):
        cx_, cy_ = (w - 1) / 2, (h - 1) / 2
        r = math.hypot(x - cx_, y - cy_)
        a = math.atan2(y - cy_, x - cx_)
        if abs(r - min(w, h) * 0.42) < 0.6:
            return BRAND
        if r < min(w, h) * 0.36 and (abs(math.sin(a * 1.5)) < 0.22 or abs(x - cx_) < 0.6):
            return BRAND
        if r < min(w, h) * 0.45 and h01("brd", x, y) < 0.18:
            return hexc("#7a1408")
        return None

    def sigil_glow(f, x, y, w, h, c):
        if c == BRAND:
            return BRAND_HOT if h01("bh", x, y) < 0.3 else BRAND
        return hexc("#5a0c04")
    m.cube("brand", (-b.ax + 0.85, b.palm + 0.1, -1.2), (0, b.wrist - b.palm - 0.2, 2.4), sigil, sigil_glow, faces=("east",),
           density=8, tag="brand")


def build_coat(m):
    """The greatcoat's skirt: two fronts open from the waist and a back, each in two hinged halves (knee), to mid-calf."""
    b = WB
    coat = coat_m(23401)
    top, knee, hem = b.hip + 0.6, b.knee, 3.4
    d = 2.35                                      # the skirt's half-depth (it flares over the hips)

    def panel(kind, s, lower):
        def mm(f, x, y, w, h):
            if f == "up" and not lower:
                return None
            if f == "up" and lower:
                return None
            if kind == "front":
                if f == "south":
                    return None
                inner = (f == "east" and s < 0) or (f == "west" and s > 0)
                if inner:
                    return CRIMSON[1] if (y + x) % 3 else CRIMSON[0]    # the lining at the opening
            else:
                if f == "north":
                    return None
                if f == "south" and abs(x - (w - 1) / 2) < 0.6 and lower:
                    return COAT[0]                                    # the back vent
            if f == "down":
                return COAT[1]
            c = coat(f, x, y + (10 if lower else 0), w, h)
            if lower and f != "down" and y >= h - 1:
                return COAT[4]                                       # the hem's edge
            return c
        return mm
    for bone, lo, s in (("coat_r", "coat_r_lower", -1), ("coat_l", "coat_l_lower", 1)):
        x_out, x_in = (-3.6 if s < 0 else 0.6), (-0.6 if s < 0 else 3.6)
        m.cube(bone, (x_out, knee, -d), (x_in - x_out, top - knee, d), panel("front", s, False), inflate=0.08, density=2, tag="coat_front",
               rotation=(0, 0, -4 * s), pivot=(1.8 * s, top, -1.2))
        x_out2, x_in2 = (-4.2 if s < 0 else 1.0), (-1.0 if s < 0 else 4.2)
        m.cube(lo, (x_out2, hem, -d - 0.2), (x_in2 - x_out2, knee - hem + 0.15, d + 0.2), panel("front", s, True), inflate=0.08,
               density=2, tag="coat_front_lower", rotation=(0, 0, -4 * s), pivot=(2.2 * s, knee, -1.6))
    m.cube("coat_back", (-3.6, knee, 0), (7.2, top - knee, d), panel("back", 0, False), inflate=0.08, density=2, tag="coat_back")
    m.cube("coat_back_lower", (-4.2, hem, 0), (8.4, knee - hem + 0.15, d + 0.2), panel("back", 0, True), inflate=0.08, density=2,
           tag="coat_back_lower")


def build_legs(m):
    b = WB
    tr = leather_m(TROUSER, 23501, 2.8, shine=False)

    def boot(f, x, y, w, h):
        if f == "down":
            return BOOT[0]
        c = tone(BOOT, 2.9 + 0.8 * math.sin(x * 0.9) - 0.6 * y / max(1, h) + (0.8 if f == "up" else 0))
        if f == "north" and abs(x - (w - 1) / 2) < 0.6 and y < h - 2:
            return BOOT[4]                       # the shine down the shin
        if y >= h - 1:
            return BOOT[0]
        return c
    for side, s in SIDES:
        m.cube(f"{side}_leg", (b.lx * s - b.lw / 2, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), tr, density=2, tag="thigh")
        # Tall riding boots to just below the knee, a heel, a pointed toe.
        m.cube(f"{side}_shin", (b.lx * s - 1.3, 0.9, -1.3), (2.6, b.knee + 0.6 - 0.9, 2.6), boot, density=2, tag="boot_shaft")
        m.cube(f"{side}_shin", (b.lx * s - 1.4, b.knee - 0.2, -1.4), (2.8, 0.7, 2.8), none_on(("down",), lambda f, x, y, w, h: BOOT[3 if y else 4]),
               density=2, tag="boot_top")
        m.cube(f"{side}_shin", (b.lx * s - 1.25, 0.6, -2.9), (2.5, 1.1, 2.0), boot, density=2, tag="boot_foot")
        m.cube(f"{side}_shin", (b.lx * s - 0.7, 0.0, -3.7), (1.4, 0.9, 1.0), boot, density=4, tag="boot_toe")
        m.cube(f"{side}_shin", (b.lx * s - 1.3, 0.5, -3.0), (2.6, 0.2, 3.0), solid(BOOT[0]), density=2, tag="sole")
        m.cube(f"{side}_shin", (b.lx * s - 0.5, 0.0, 0.4), (1.0, 0.9, 0.8), solid(BOOT[1]), density=4, tag="heel")


# =====================================================================================================
# Clips
# =====================================================================================================

MOUTH_OPEN = {"@mouth": [0, 0, -0.32]}
BRAND_OUT = {"@brand": [0.32, 0, 0]}
SNEER = {"@brows": [0, -0.12, 0]}


def follow(pose):
    """The coat's skirt swings with the legs and the lean; the hair's fall and the belt tail hang back."""
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])
    head = pose.get("head", [0, 0, 0])[0]
    if "coat_r" not in pose:
        out["coat_r"] = [min(0.0, rl) * 0.8 - body[0] * 0.5, 0, 0]
    if "coat_l" not in pose:
        out["coat_l"] = [min(0.0, ll) * 0.8 - body[0] * 0.5, 0, 0]
    if "coat_r_lower" not in pose:
        out["coat_r_lower"] = [min(0.0, rl) * 0.15 + max(0.0, pose.get("right_shin", [0, 0, 0])[0]) * 0.2, 0, 0]
    if "coat_l_lower" not in pose:
        out["coat_l_lower"] = [min(0.0, ll) * 0.15 + max(0.0, pose.get("left_shin", [0, 0, 0])[0]) * 0.2, 0, 0]
    if "coat_back" not in pose:
        out["coat_back"] = [max(0.0, max(rl, ll)) * 0.45 - body[0] * 0.5, 0, 0]
    if "coat_back_lower" not in pose:
        out["coat_back_lower"] = [max(0.0, max(rl, ll)) * 0.15, 0, 0]
    if "hair_back" not in pose:
        out["hair_back"] = [max(-30.0, min(20.0, -head * 0.85 - body[0] * 0.5)), 0, 0]
    if "belt_tail" not in pose:
        out["belt_tail"] = [-body[0] * 0.8 - min(rl, ll) * 0.2, 0, 0]
    return out


class AClip(Clip):
    def key(self, t, pose, ease=None):
        return super().key(t, follow(pose), ease)


def stride(n, i, stride_=22, arm=10, knee=26, bob=0.35, sway=4.0, lean=0.5):
    ph = 2 * math.pi * i / n
    sn = math.sin(ph)
    return {"right_leg": [-stride_ * sn, 0, 0], "left_leg": [stride_ * sn, 0, 0],
            "right_shin": [knee * max(0, math.sin(ph + 1.2)), 0, 0], "left_shin": [knee * max(0, -math.sin(ph + 1.2)), 0, 0],
            "right_arm": [arm * sn, 0, 4], "left_arm": [-arm * sn, 0, -4], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
            "body": [lean, sway * sn, 2.0 * sn], "head": [-2, -sway * sn, -1.5 * sn], "@root": [0, bob * abs(math.cos(ph)), 0]}


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(AClip(f, A + name, length, loop, hold))
        return clips[-1]

    def reach(pose, side, target, grip=None):
        s = -1 if side == "right" else 1
        return _reach(r, pose, side, target, grip or (WB.ax * s, WB.palm - 0.6, -0.3))

    # Her stance: chin up, weight on the left leg, the left hand on her hip, the right loose and ready.
    stance = P_(REST, GRACE_R, CURL_L, {"body": [-1, 6, 1.5], "head": [-6, -8, -2], "left_leg": [0, 0, -1], "right_leg": [-4, 8, 4],
                                        "right_shin": [6, 0, 0], "right_arm": [-4, 0, 8], "right_forearm": [-16, 0, 0],
                                        "left_arm": [6, -20, -30], "left_forearm": [-50, 0, 0], "left_hand": [0, 0, 0]})
    # The left hand on her hip, knuckles resting on the belt.
    stance = reach(stance, "left", (TWW + 1.3, WAIST - 1.6, 0.6))
    stance = P_(stance, {"left_hand": [0, -30, 30]})

    # --- idle: a slow look round her domain, the right fingers drumming the air, a tilt of the head. ---
    L = 5.0
    c = clip("idle", L, loop=True)
    for t, look, tilt in ((0, -8, -2), (1.4, 14, 3), (2.6, 6, 0), (3.8, -16, -4), (L, -8, -2)):
        c.key(t, P_(stance, {"head": [-6 + abs(look) * 0.1, look, tilt]}), E_IO if t else None)
    c.layer("right_fingers", lambda t: [-16 * max(0, math.sin(t * 2 * math.pi * 4 / L)) if 2.8 < t < 4.4 else 0, 0, 0])
    c.layer("body", lambda t: [0.6 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
    c.layer("hair_tail", lambda t: [1.5 * math.sin(t * 2 * math.pi / L), 0, 1.0 * math.sin(t * 2 * math.pi / L + 1)])

    # --- walk: a long, sure stride, the coat swinging open. ---
    L, n = 1.2, 8
    c = clip("walk", L, loop=True)
    for i in range(n + 1):
        w = stride(n, i % n)
        c.key(L * i / n, P_(REST, GRACE_R, GRACE_L, w), E_IO if i else None)
    c.layer("hair_tail", lambda t: [4 * math.sin(4 * math.pi * t / L), 0, 0])
    c.layer("curl_r", lambda t: [3 * math.sin(4 * math.pi * t / L), 0, 0])
    c.layer("curl_l", lambda t: [3 * math.sin(4 * math.pi * t / L + 0.6), 0, 0])
    c.layer("belt_tail", lambda t: [8 * math.sin(2 * math.pi * t / L - 1.0), 0, 0])

    # --- trapped: caught in a devil's trap -- bracing against the invisible wall, testing it with her palms, a snarl. ---
    L = 2.4
    c = clip("trapped", L, loop=True)
    brace = P_(REST, CLAW_R, CLAW_L, SNEER, {"body": [12, 0, 0], "head": [-8, 0, 0], "@root": [0, -1.0, 0], "right_leg": [-18, 0, 6],
                                            "right_shin": [26, 0, 0], "left_leg": [12, 0, -6], "left_shin": [14, 0, 0],
                                            "right_arm": [-80, -16, 6], "right_forearm": [-24, 0, 0], "right_hand": [-60, 0, 0],
                                            "left_arm": [-74, 20, -6], "left_forearm": [-30, 0, 0], "left_hand": [-60, 0, 0]})
    shove = P_(brace, MOUTH_OPEN, {"body": [18, 6, 0], "head": [-14, -6, 4], "right_arm": [-86, -10, 4], "left_arm": [-84, 14, -4],
                                   "right_forearm": [-8, 0, 0], "left_forearm": [-12, 0, 0], "@root": [0, -1.3, -0.4]})
    look = P_(brace, {"body": [6, -18, 0], "head": [4, 30, 0], "right_arm": [-40, -10, 20], "left_arm": [-60, 30, -10],
                      "left_forearm": [-50, 0, 0]})
    c.key(0, brace)
    c.key(0.5, shove, E_OUT)
    c.key(0.8, P_(shove, {"right_arm": [-82, -12, 6]}), E_IO)
    c.key(1.3, look, E_IO)
    c.key(1.8, P_(look, {"head": [6, -20, -4], "body": [6, 14, 0]}), E_IO)
    c.key(L, brace, E_IO)
    c.layer("right_arm", lambda t: [1.8 * math.sin(t * 67), 0, 1.2 * math.sin(t * 51)] if 0.4 < t < 0.9 else [0, 0, 0])
    c.layer("left_arm", lambda t: [1.8 * math.sin(t * 59 + 1), 0, 1.2 * math.sin(t * 47)] if 0.4 < t < 0.9 else [0, 0, 0])
    c.layer("hair_tail", lambda t: [3 * math.sin(t * 2 * math.pi * 2 / L), 0, 2 * math.sin(t * 2 * math.pi / L)])

    # --- herald_mark: the arm swung out, a finger levelled at the target (tick 10): "that one". ---
    c = clip("herald_mark", 1.2)
    point = P_(stance, POINT_R, SNEER, {"right_arm": [-88, -16, 0], "right_forearm": [-4, 0, 0], "right_hand": [0, 0, 0],
                                        "body": [2, -14, 0], "head": [-4, 16, 0], "right_leg": [-10, 8, 4]})
    c.key(0, stance)
    c.key(0.3, P_(point, {"right_arm": [-60, 20, 20], "right_forearm": [-50, 0, 0]}), E_OUT)
    c.key(0.5, point, E_SNAP)
    c.key(0.8, P_(point, {"right_arm": [-86, -16, 0], "head": [-6, 14, 4]}), E_IO)
    c.key(1.2, stance, E_IO)

    # --- hellfire_sweep: the right hand drawn across to the left shoulder, then swept out wide, palm open (tick 18). ---
    c = clip("hellfire_sweep", 1.7)
    draw = P_(stance, CLAW_R, SNEER, {"body": [0, 26, 0], "head": [0, -14, 0], "right_arm": [-70, 60, -20], "right_forearm": [-70, 0, 0],
                                      "right_hand": [0, 0, 40], "left_arm": [-10, 0, -30], "left_forearm": [-40, 0, 0], "left_hand": [0, 0, 0],
                                      "right_leg": [6, 0, 4], "left_leg": [-10, 0, -4]})
    sweep = P_(stance, OPEN_R, SNEER, MOUTH_OPEN, {"body": [6, -30, 0], "head": [-2, 20, 0], "right_arm": [-84, -70, 10],
                                                   "right_forearm": [-4, 0, 0], "right_hand": [-30, 0, -20], "left_arm": [-30, 20, -40],
                                                   "left_forearm": [-20, 0, 0], "right_leg": [-18, 0, 6], "left_leg": [12, 0, -6],
                                                   "@root": [0, -0.5, 0], "hair_back": [16, 0, 0]})
    c.key(0, stance)
    c.key(0.55, draw, E_OUT)
    c.key(0.75, P_(draw, {"body": [0, 30, 0], "right_arm": [-72, 64, -22]}), E_IO)
    c.key(0.9, sweep, E_SNAP)
    c.key(1.2, P_(sweep, {"right_arm": [-80, -78, 12]}), E_OUT)
    c.key(1.7, stance, E_IO)

    # --- smoke_out: she throws her head back, arms falling open, as the smoke pours from her mouth (tick 20). ---
    c = clip("smoke_out", 1.8)
    tense = P_(REST, CLAW_R, CLAW_L, {"body": [10, 0, 0], "head": [14, 0, 0], "right_arm": [-20, 0, 18], "left_arm": [-20, 0, -18],
                                      "right_forearm": [-50, 0, 0], "left_forearm": [-50, 0, 0], "@root": [0, -0.6, 0]})
    arch = P_(REST, OPEN_R, OPEN_L, MOUTH_OPEN, {"body": [-22, 0, 0], "head": [-48, 0, 0], "right_arm": [-10, 0, 40], "left_arm": [-10, 0, -40],
                                                 "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0], "@root": [0, 0.6, 0.6],
                                                 "right_hand": [0, 0, -20], "left_hand": [0, 0, 20], "hair_back": [30, 0, 0]})
    limp = P_(REST, OPEN_R, OPEN_L, {"body": [26, 0, 6], "head": [44, 0, 10], "right_arm": [6, 0, 6], "left_arm": [6, 0, -4],
                                     "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0], "@root": [0, -1.6, 0], "right_leg": [-10, 0, 4],
                                     "right_shin": [20, 0, 0], "left_leg": [-6, 0, -4], "left_shin": [16, 0, 0]})
    c.key(0, stance)
    c.key(0.5, tense, E_IO)
    c.key(1.0, arch, E_BACK)
    c.key(1.35, P_(arch, {"head": [-52, 0, 0]}), E_IO)
    c.key(1.8, limp, E_IN)

    # --- possess: the dive -- she pitches forward, arms swept back, into the knight (tick 12). ---
    c = clip("possess", 1.2)
    crouch = P_(REST, CLAW_R, CLAW_L, SNEER, {"body": [24, 0, 0], "head": [-10, 0, 0], "@root": [0, -1.8, 0.6], "right_leg": [-30, 0, 6],
                                             "right_shin": [50, 0, 0], "left_leg": [-30, 0, -6], "left_shin": [50, 0, 0],
                                             "right_arm": [30, 0, 30], "left_arm": [30, 0, -30]})
    dive = P_(REST, OPEN_R, OPEN_L, MOUTH_OPEN, {"body": [60, 0, 0], "head": [-30, 0, 0], "@root": [0, 1.2, -3.0], "right_leg": [20, 0, 4],
                                                 "left_leg": [34, 0, -4], "right_shin": [20, 0, 0], "left_shin": [30, 0, 0],
                                                 "right_arm": [50, 0, 20], "left_arm": [50, 0, -20], "hair_back": [30, 0, 0],
                                                 "coat_back": [40, 0, 0]})
    c.key(0, stance)
    c.key(0.4, crouch, E_OUT)
    c.key(0.6, dive, E_SNAP)
    c.key(0.85, P_(dive, {"@root": [0, 1.0, -3.6]}), E_OUT)
    c.key(1.2, stance, E_IO)

    # --- telekinetic_pin: the right fist held out, clenched, twisting -- the victim pinned (tick 16), the arm trembling. ---
    c = clip("telekinetic_pin", 1.6)
    reach_out = P_(stance, CLAW_R, SNEER, {"right_arm": [-86, -6, 0], "right_forearm": [-6, 0, 0], "right_hand": [0, -40, 0],
                                           "body": [4, -10, 0], "head": [-8, 10, 0], "right_leg": [-12, 6, 4]})
    clench = P_(reach_out, FIST_R, MOUTH_OPEN, {"right_hand": [0, 30, 0], "right_arm": [-82, -4, 0], "head": [-10, 8, 4]})
    c.key(0, stance)
    c.key(0.45, reach_out, E_OUT)
    c.key(0.8, clench, E_SNAP)
    c.key(1.25, P_(clench, {"right_hand": [0, 40, 0]}), E_IO)
    c.key(1.6, stance, E_IO)
    c.layer("right_arm", lambda t: [1.6 * math.sin(t * 71), 0, 1.0 * math.sin(t * 57)] if 0.8 < t < 1.3 else [0, 0, 0])

    # --- brand: down on one knee, the right palm pressed to the ground; the seal burns out at tick 24. ---
    c = clip("brand", 2.2)
    kneel = P_(REST, OPEN_R, GRACE_L, {"@root": [0, -4.9, 0], "left_leg": [6, 0, -4], "left_shin": [86, 0, 0], "right_leg": [-82, 0, 4],
                                       "right_shin": [84, 0, 0], "body": [24, 0, 0], "head": [10, 0, 0], "left_arm": [-30, 0, -10],
                                       "left_forearm": [-40, 0, 0], "left_hand": [0, 0, 0]})
    press = reach(P_(kneel, {"right_hand": [0, 0, 0]}), "right", (-3.2, 0.9, -7.6))
    press = P_(press, {"right_hand": [0, 0, -88], "body": [30, 0, 0], "head": [-6, 0, 0], "@root": [0, -5.1, 0]}, SNEER)
    c.key(0, stance)
    c.key(0.6, P_(kneel, {"@root": [0, -3.0, 0], "right_leg": [-60, 0, 4], "right_shin": [60, 0, 0]}), E_IN)
    c.key(0.95, press, E_OUT)
    c.key(1.2, P_(press, BRAND_OUT, MOUTH_OPEN, {"body": [32, 0, 0]}), E_SNAP)
    c.key(1.7, P_(press, BRAND_OUT), E_IO)
    c.key(2.0, P_(kneel, {"@root": [0, -2.0, 0], "right_leg": [-50, 0, 4], "right_shin": [50, 0, 0]}), E_IO)
    c.key(2.2, stance, E_IO)

    # --- death: struck through, she arches, arms flung wide, burning from inside (tick 80), and holds there on her knees. ---
    c = clip("death", 6.0, hold=True)
    hit = P_(REST, CLAW_R, CLAW_L, MOUTH_OPEN, {"body": [-14, 0, 0], "head": [-20, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-30, 0, -30],
                                               "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "@root": [0, 0, 0.8]})
    stagger = P_(hit, {"body": [10, 8, 4], "head": [6, -6, 4], "right_leg": [-20, 0, 4], "right_shin": [30, 0, 0], "@root": [0, -1.2, 0]})
    kneel2 = {"@root": [0, -5.6, 0.6], "right_leg": [-4, 0, 4], "left_leg": [-4, 0, -4], "right_shin": [94, 0, 0], "left_shin": [94, 0, 0]}
    burn = P_(REST, OPEN_R, OPEN_L, MOUTH_OPEN, kneel2, {"body": [-20, 0, 0], "head": [-46, 0, 0], "right_arm": [-60, 0, 70],
                                                         "left_arm": [-60, 0, -70], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
                                                         "right_hand": [0, 0, -20], "left_hand": [0, 0, 20], "hair_back": [26, 0, 0]})
    c.key(0, stance)
    c.key(0.3, hit, E_SNAP)
    c.key(1.2, P_(hit, {"body": [-10, 4, -2]}), E_IO)
    c.key(2.2, stagger, E_IO)
    c.key(3.1, P_(stagger, kneel2, {"@root": [0, -5.0, 0.4], "right_shin": [80, 0, 0], "left_shin": [80, 0, 0], "body": [24, 0, 0]}), E_IN)
    c.key(4.0, burn, E_BACK)
    c.key(6.0, P_(burn, {"head": [-50, 0, 0], "right_arm": [-64, 0, 74], "left_arm": [-64, 0, -74]}), E_IO)
    c.layer("right_hand", lambda t: [0, 0, 5 * math.sin(t * 43)] if 3.8 < t < 5.6 else [0, 0, 0])
    c.layer("left_hand", lambda t: [0, 0, -5 * math.sin(t * 41)] if 3.8 < t < 5.6 else [0, 0, 0])

    done(clips)
    return f, clips


# =====================================================================================================
# The Book of the Damned and the bust
# =====================================================================================================

def book_icon():
    """The Book of the Damned: a thick tome bound in black, cracked leather, iron corners, a sigil burnt red into the
    cover (the brightest red of any sprite, it seems to glow), a blood-red clasp across the fore-edge, red-edged pages."""
    O = hexc("#0a0506")
    pal = {"o": O, "d": hexc("#140c0e"), "l": hexc("#22161a"), "L": hexc("#2f2025"), "h": hexc("#3e2c32"), "i": GUNMETAL[3],
           "I": GUNMETAL[5], "r": hexc("#a4140f"), "R": hexc("#ff3b22"), "w": hexc("#ffb27a"), "p": hexc("#6a1010"),
           "P": hexc("#8e1b17"), "c": CRIMSON[3], "C": CRIMSON[5], "k": hexc("#3a0606")}
    t = art([
        "................",
        "..oooooooooooo..",
        ".oIhhhhhhhhhhIo.",
        ".ohLLLLLLLLLLLPo",
        ".ohLLLLLLLLLLLpo",
        ".ohLLLLLLLLLLLPo",
        ".ohLLLLLLLLLLccC",
        ".ohLLLLLLLLLLCcC",
        ".ohLLLLLLLLLLLcc",
        ".ohLLLLLLLLLLLPo",
        ".ohLLLLLLLLLLLpo",
        ".ohLlLLLLLLlLLPo",
        ".oIddddddddddIko",
        "..okkkkkkkkkkko.",
        "...ooooooooooo..",
        "................",
    ], pal)
    # The sigil burnt into the cover: a ring, an inverted triangle inside it, a coal at the heart.
    sig = ["...rrr...",
           ".rr...rr.",
           ".rRRRRRr.",
           "r..R.R..r",
           "r..RwR..r",
           "r...R...r",
           ".r..R..r.",
           ".rr...rr.",
           "...rrr..."]
    for j, row in enumerate(sig):
        for i, ch in enumerate(row):
            if ch != ".":
                t.set(3 + i, 3 + j, pal[ch] if ch != "r" or (i + j) % 3 else pal["p"])
    return t


def bust():
    """Her bust on the pedestal: red waves, black eyes, the black greatcoat with its flared collar and crimson blouse."""
    def tex16(fn):
        t = Tex(16, 16, 0)
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                if c is not None:
                    t.set(x, y, c)
        return t

    def skin(x, y):
        return tone(SKIN, 3.5 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.5 * y / 15)

    def hair(x, y):
        n = fbm(x * 2, y, 23901, 16, 16, 2, 4.0)
        return tone(HAIR, 2.8 + 0.6 * math.sin(x * 1.5 + y * 0.6 + n * 2) + (0.7 if (y + x // 3) % 5 == 0 else 0))
    rows = [   # x = 0 is the viewer's left
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhsssshhh",
        "hhhhssssssssssshh",
        "hsbBBBbsssbBBBbh",
        "hsKKKKssssKKKKsh",
        "hsRXXXRssRXXXRsh",
        "hssrrrssssrrrssh",
        "hssssssnnssssssh",
        "hcsssssnnsssssch",
        "hssssssNNssssssh",
        "hssssssssssssssh",
        "hssssslMMlsssssh",
        "hjsssssLLsssssjh",
        "hjjssssssssssjjh",
        "hhjjssssssssjjhh",
    ]
    legend = {"s": skin, "b": HAIR[2], "B": HAIR[1], "K": hexc("#0e0607"), "X": EYE_BLACK, "R": hexc("#a0140e"), "r": hexc("#3a0a0a"),
              "n": SKIN[4], "N": SKIN[1], "c": mix(SKIN[3], LIPS[4], 0.15), "l": LIPS[1], "M": LIPS[3], "L": LIPS[4],
              "j": lambda x, y: shade(skin(x, y), 0.9), "h": hair}

    def face(x, y):
        v = legend.get(rows[y][x][0] if x < len(rows[y]) else "s")
        return v(x, y) if callable(v) else v
    face_t = tex16(face)
    hair_t = tex16(hair)
    skin_t = tex16(skin)

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 2.6 - y * 0.2 and y < 10:
            return tone(CRIMSON, 3.6 - y / 10 + 0.4 * math.sin(x + y))
        if d < 3.4 - y * 0.15 and y < 11:
            return COAT[4]
        if y in (9, 12) and abs(d - 2.0) < 0.6:
            return GUNMETAL[4]
        return tone(COAT, 3.0 - y / 12 + 0.3 * math.sin(x * 1.3 + y * 0.2))
    chest_t = tex16(chest)
    coat_t = tex16(lambda x, y: tone(COAT, 3.2 - y / 14 + 0.3 * math.sin(x * 0.9 + y * 0.4)))
    for name, t in (("face", face_t), ("hair", hair_t), ("skin", skin_t), ("chest", chest_t), ("coat", coat_t)):
        save(t, "block", f"abaddon_trophy_{name}")
    full = [0, 0, 16, 16]
    F, HR, S, CH, C = "face", "hair", "skin", "chest", "coat"
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": C}),
        BM.cube("chest", (4.7, 3.4, 6.1), (11.3, 7.4, 9.9), {"north": CH, "*": C}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (4.0, 5.9, 6.3), (12.0, 7.6, 9.7), {"*": C}),
        # The flared collar standing up behind her neck and the falling hair.
        BM.cube("collar", (5.0, 7.0, 9.2), (11.0, 9.8, 9.9), {"*": C}, rot=("x", -22.5, (8, 7.0, 9.6))),
        BM.cube("neck", (7.1, 7.3, 7.1), (8.9, 8.6, 8.9), {"*": S}),
        BM.cube("head", (5.7, 8.4, 5.7), (10.3, 13.2, 10.3), {"north": F, "up": HR, "south": HR, "east": HR, "west": HR},
                uv={"north": full}),
        BM.cube("hair_cap", (5.45, 11.0, 5.8), (10.55, 13.5, 10.6), {"north": None, "down": None, "*": HR}),
        BM.cube("roll", (5.4, 12.8, 5.3), (8.4, 14.0, 6.6), {"*": HR}),
        BM.cube("hair_fall", (5.3, 6.6, 9.6), (10.7, 11.4, 10.8), {"north": None, "*": HR}),
        BM.cube("lock_r", (5.2, 7.4, 6.0), (5.9, 10.6, 7.6), {"*": HR}),
        BM.cube("lock_l", (10.1, 7.4, 6.0), (10.8, 10.6, 7.6), {"*": HR}),
        BM.cube("nose", (7.7, 9.9, 5.45), (8.3, 10.8, 5.7), {"*": S}),
    ]
    mdl = BM.model({"base": "trophy_base", F: "abaddon_trophy_face", HR: "abaddon_trophy_hair", S: "abaddon_trophy_skin",
                    CH: "abaddon_trophy_chest", C: "abaddon_trophy_coat"}, e, "trophy_base")
    BM.write("abaddon_trophy", mdl)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=23000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    save(book_icon(), "item", "book_of_the_damned")
    bust()


if __name__ == "__main__":
    generate()
