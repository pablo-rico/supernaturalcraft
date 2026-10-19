"""The witches (v0.21): the Hedge Witch and the Coven Witch -- GeckoLib rigs, textures, glowmasks and clips (names in
entity/witch/WitchAssets).

  hedge_witch   a self-taught small-town witch: messy chestnut hair in a slipping bun (strands loose, a braid over her
                shoulder), freckles, a long oatmeal cardigan over a faded rose print blouse, a patched olive skirt to
                mid-calf, wool socks and laced brown boots; a satchel on a strap across her chest, and on her belt the trade:
                hex bags tied with twine, a bundle of dried herbs, a small copper bowl.
  coven_witch   the Grand Coven's elite: black hair drawn into a low chignon with a silver pin, pale, plum lips; a
                Victorian-cut dress, black with plum velvet panels, a high standing collar with the coven's sigil brooch at
                the throat, silver buttons, a short black capelet; magenta runes inked on the backs of her hands (glowmask).

Both glow at the eyes (glowmask: the renderer shows it while they cast) -- the coven witch's runes too.

Skeleton (witchkit's woman plus): head -- hair -- hair_bun, strand_r, strand_l (hedge: braid -- braid_end; coven: pin);
  body -- satchel / capelet -- capelet_back;  skirt -- skirt_lower, belt charms (hedge: hexbag_1..3, herbs, bowl)
Clips: idle, walk (loops), cast, hex, teleport; coven also channel (loop) and shield.
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from legacy_art import SIDES, done, knit_m, leather_m, worn, reach as _reach
from michael_art import ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, tone, write_compact
from pixelkit import Ramp, hexc, mix, shade
from witchkit import (CLAW_L, CLAW_R, CURL_L, CURL_R, EYE_WHITE, FIST_L, FIST_R, GRACE_L, GRACE_R, OPEN_L, OPEN_R, REST, TWW, WAIST,
                      WB, WClip, bell, boot_shoe, face_head, gem, glow_k, gown_follow, gown_walk, metal, nose_ears, satin, slim_hands,
                      velvet, wavy_hair, womans_legs, wskeleton)

MAGENTA = Ramp("#3d0630", "#6e0c55", "#a3187e", "#d22aa6", "#f05bc8", "#ffb0ea")
SILVER = Ramp("#2e333b", "#4f5661", "#7a828e", "#a6adb8", "#cfd5dc", "#f4f7fa")
LASH = hexc("#1a100c")


def eye_glow(iris):
    def g(f, x, y, w, h, c):
        if c == iris:
            return MAGENTA[5]
        if c == hexc("#121014"):
            return MAGENTA[3]
        return None
    return g


# =====================================================================================================
# The Hedge Witch
# =====================================================================================================

H_SKIN = Ramp("#6e4a3c", "#9e705d", "#c2917a", "#d6a991", "#e4bea8", "#f0d3c1")
H_HAIR = Ramp("#24130a", "#3a2112", "#53311b", "#6b4325", "#845632", "#9e6b42")
H_IRIS = hexc("#5f7a45")
CARDIGAN = Ramp("#5e5444", "#7c705c", "#9a8d75", "#b3a68d", "#c8bca4", "#ddd3bd")
BLOUSE = Ramp("#5e3a3c", "#7d5052", "#9b6a69", "#b58683", "#cca29d", "#e0c0b9")
OLIVE = Ramp("#1f2112", "#30331b", "#434826", "#575d32", "#6d7440", "#858c52")
PATCH = Ramp("#3a2214", "#58361f", "#764b2c", "#93613a", "#ad7a4d", "#c69466")
SOCK = Ramp("#2c2622", "#423a33", "#5a5047", "#71665b", "#887c70", "#a09487")
BOOT = Ramp("#1c120a", "#2c1c10", "#3e2817", "#523620", "#67462b", "#7d5837")
STRAP = Ramp("#22140a", "#36210f", "#4b2f17", "#62401f", "#795129", "#916435")
TWINE = hexc("#b59a68")
COPPER = Ramp("#3a1a0c", "#5e2c14", "#86431f", "#ac5d2c", "#cf7d43", "#eaa46a")
HERB = Ramp("#2a2a12", "#3d3f1b", "#545826", "#6b7232", "#828c40", "#9aa652")
LAV = hexc("#8a72a8")
BAG = [Ramp("#3a1418", "#5a2026", "#7a2e34", "#963e43", "#ae5357", "#c46b6c"),
       Ramp("#2a2a2e", "#3e3e44", "#55555c", "#6c6c74", "#85858c", "#9e9ea5"),
       Ramp("#3a2a12", "#56401c", "#735726", "#8f6e32", "#a7843f", "#bf9b50")]

H_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhshhhhhhshhh",
    "hhssssssshssssss",
    "hssssssssssssssh",
    "hsbbbbssssbbbbsh",
    "hssKKKssssKKKssh",
    "hssWIEssssEIWssh",
    "hssuuussssuuussh",
    "hsfsfssnnssfsfsh",
    "sssfsssnnsssfsss",
    "ssssssNnnNssssss",
    "ssssssssssssssss",
    "sssssslMMlssssss",
    "jssssssLLssssssj",
    "jjssssssssssssjj",
]


def hedge_rig():
    m = Model("hedge_witch", 256, 256)
    b = WB
    wskeleton(m, b)
    m.bone("hair", (0, b.neck + 4, 0), "head")
    m.bone("hair_bun", (0.6, b.neck + 6.4, 3.6), "hair")
    m.bone("strand_r", (-3.4, b.neck + 6.0, -3.6), "hair")
    m.bone("strand_l", (3.4, b.neck + 6.0, -3.6), "hair")
    m.bone("braid", (3.0, b.neck + 1.5, 1.0), "head")
    m.bone("braid_end", (3.6, b.neck - 3.6, -1.2), "braid")
    m.bone("cardigan_r", (-1.8, b.hip + 0.6, -2.0), "skirt")
    m.bone("cardigan_l", (1.8, b.hip + 0.6, -2.0), "skirt")
    m.bone("cardigan_back", (0, b.hip + 0.6, 2.0), "skirt")
    m.bone("satchel", (4.0, b.hip + 1.0, 0.8), "skirt")
    for i, x in enumerate((-3.0, -1.4, 2.4)):
        m.bone(f"hexbag_{i + 1}", (x, b.hip - 0.2, -2.6), "skirt")
    m.bone("herbs", (-3.4, b.hip - 0.2, 0.4), "skirt")
    m.bone("bowl", (-3.0, b.hip - 0.4, 2.2), "skirt")
    sk = H.skin_mat(H_SKIN, 31101, 3.3, 0.45)
    hr = wavy_hair(H_HAIR, 31102, 2.7, wave=1.6, shine=0.4)
    y0 = b.neck
    legend = {"h": hr, "s": sk, "b": H_HAIR[2], "K": LASH, "W": EYE_WHITE, "I": H_IRIS, "E": hexc("#121014"),
              "u": mix(H_SKIN[2], hexc("#6a5060"), 0.3), "n": H_SKIN[4], "N": H_SKIN[1], "f": mix(H_SKIN[3], hexc("#8a5030"), 0.35),
              "l": hexc("#8a4a46"), "M": hexc("#a65d58"), "L": hexc("#b8716a"), "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}

    def sides(f, x, y, w, h):
        if f in ("up", "south"):
            return hr(f, x, y, w, h) if f == "up" or y < 13 else sk(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x
        if y < 4 or (fx > 9 and y < 13) or (fx > 5 and y < 6):
            return hr(f, x, y, w, h)
        if 7 <= fx <= 9 and 6 <= y <= 9:
            return shade(sk(f, x, y, w, h), 0.85 if fx == 8 else 1.05)
        return sk(f, x, y, w, h)
    face_head(m, b, H_FACE, legend, sides, eye_glow(H_IRIS))
    nose_ears(m, b, H_SKIN, nose_h=1.4, ears=False)
    m.cube("body", (-1.2, y0 - 0.8, -1.2), (2.4, 1.2, 2.4), sk, density=2, tag="neck")
    # Messy hair: a shell, flyaway tufts, the bun slipping to one side, loose strands framing her face, a side braid.
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hr(f, x, y, w, h) if y < 2 or (y < 3 and x % 3 == 0) else None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if fx > 3 or y < 3 else None
        return hr(f, x, y, w, h)
    m.cube("hair", (-4, y0 + 4.0, -4), (8, 4.0, 8), shell, inflate=0.35, density=2, tag="hair_shell")
    for i, (x0, z0, rx, rz) in enumerate(((-2.6, -1.0, -20, 25), (1.0, -2.2, -30, -15), (2.8, 1.2, 15, -35), (-1.4, 2.4, 25, 20))):
        m.cube("hair", (x0 - 0.6, y0 + 8.0, z0 - 0.6), (1.2, 1.2, 1.2), none_on(("down",), hr), density=2, tag="flyaway",
               rotation=(rx, 0, rz), pivot=(x0, y0 + 8.0, z0))
    bunm = lambda f, x, y, w, h: tone(H_HAIR, 2.9 + 0.8 * math.sin((x + y) * 1.4) + (0.4 if f == "up" else 0))
    m.cube("hair_bun", (-1.4, y0 + 5.0, 3.4), (3.0, 2.8, 2.2), bunm, density=2, tag="bun", rotation=(0, 0, 18), pivot=(0.6, y0 + 6.4, 3.6))
    m.cube("hair_bun", (-0.4, y0 + 6.6, 4.0), (1.4, 1.0, 1.4), bunm, density=2, tag="bun_tuft", rotation=(30, 0, -20), pivot=(0.6, y0 + 6.4, 4.4))
    m.cube("hair_bun", (-0.5, y0 + 4.4, 4.8), (2.8, 0.3, 0.3), solid(hexc("#5b3a20")), density=4, tag="pencil", rotation=(0, 0, -30),
           pivot=(0.6, y0 + 5.6, 4.9))                    # a pencil stuck through it
    for side, s in SIDES:
        m.cube(f"strand_{side[0]}", (s * 3.6 - 0.5, y0 + 1.6, -3.9), (1.0, 4.6, 0.6), none_on(("up",), hr), density=2, tag="strand",
               rotation=(0, 0, 6 * s), pivot=(s * 3.4, y0 + 6.0, -3.6))
    braid = lambda f, x, y, w, h: tone(H_HAIR, 2.6 + (1.2 if (y + (x if f in ("north", "south") else 0)) % 3 == 0 else 0) - 0.4 * (f in ("east", "west")))
    m.cube("braid", (2.6, y0 - 3.6, -1.9), (1.6, 5.4, 1.6), braid, density=2, tag="braid", rotation=(-20, 0, 8), pivot=(3.0, y0 + 1.5, 1.0))
    m.cube("braid_end", (2.9, y0 - 6.6, -2.4), (1.4, 3.2, 1.4), lambda f, x, y, w, h: TWINE if y in (0, 1) else braid(f, x, y, w, h),
           density=2, tag="braid_end", rotation=(-6, 0, 4), pivot=(3.6, y0 - 3.6, -1.2))
    m.cube("braid_end", (3.0, y0 - 7.8, -2.3), (1.2, 1.2, 1.2), lambda f, x, y, w, h: tone(H_HAIR, 3.4 - y), density=2, tag="braid_tuft",
           rotation=(-6, 0, 4), pivot=(3.6, y0 - 3.6, -1.2))

    # --- clothes ---------------------------------------------------------------------------------------------------
    blouse = lambda f, x, y, w, h: (mix(BLOUSE[3], BLOUSE[5], 0.6) if (x * 3 + y * 5) % 11 == 0 else
                                    (hexc("#6f7d4e") if (x * 3 + y * 5) % 11 == 1 else tone(BLOUSE, 2.8 - 0.5 * y / max(1, h))))
    m.cube("body", (-b.tw, b.hip, -1.9), (2 * b.tw, b.neck - b.hip, 3.8), blouse, density=2, tag="torso")
    m.cube("body", (-1.6, b.neck - 1.2, -2.05), (3.2, 1.2, 0.3), lambda f, x, y, w, h: BLOUSE[4] if y == 0 else BLOUSE[3], density=2,
           tag="blouse_collar", rotation=(18, 0, 0), pivot=(0, b.neck, -2.0))
    knit = knit_m(CARDIGAN, 31201, 3.0)

    def cardi(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f == "north":
            half = 1.6 + 0.6 * y / max(1, h)
            if abs(x - mid) < half:
                return None
            if abs(x - mid) < half + 1:
                return tone(CARDIGAN, 2.0 + (1.2 if y % 3 == 0 else 0))     # the button band
        if f == "up" and abs(x - mid) < 2.4 and y < h * 0.7:
            return None
        return knit(f, x, y, w, h)
    m.cube("body", (-b.tw, b.hip, -1.9), (2 * b.tw, b.neck - b.hip, 3.8), cardi, inflate=0.4, density=2, tag="cardigan")
    # Long cardigan fronts and back to mid-thigh, with patch pockets.
    for bone, s in (("cardigan_r", -1), ("cardigan_l", 1)):
        def front(f, x, y, w, h, s=s):
            if f in ("up",) or f == ("east" if s < 0 else "west") or f == "south":
                return None
            if f == "north" and 1 <= y <= 3 and (1 <= x <= 3 if s > 0 else w - 4 <= x <= w - 2):
                return tone(CARDIGAN, 2.0 if y == 1 else 2.6)               # the pocket
            return knit(f, x, y + 20, w, h)
        m.cube(bone, ((0.4 if s > 0 else -b.tw - 0.6), b.hip - 4.6, -3.0), (b.tw + 0.2, 5.2, 0.5), front, density=2, tag="cardigan_front",
               rotation=(-6, 0, 0), pivot=(0, b.hip + 0.6, -2.0))
    m.cube("cardigan_back", (-b.tw - 0.7, b.hip - 4.6, 2.6), (2 * b.tw + 1.4, 5.2, 0.5), none_on(("up", "north"), lambda f, x, y, w, h: knit(f, x, y + 20, w, h)),
           density=2, tag="cardigan_back")
    # Sleeves: knit to the wrist, pushed up a little, cuffs rolled.
    hand = H.skin_mat(H_SKIN, 31301, 3.3, 0.45)
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.5 - b.elbow, b.aw), knit, inflate=0.3, density=2, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.4, b.wrist + 0.4, -1.4), (2.8, b.elbow - b.wrist - 0.4, 2.8), none_on(("up",), knit), inflate=0.3,
               density=2, tag="sleeve_lower")
        m.cube(f"{side}_forearm", (cx - 1.4, b.wrist + 0.4, -1.4), (2.8, 0.9, 2.8), none_on(("up", "down"), lambda f, x, y, w, h: tone(CARDIGAN, 3.6 - y)),
               inflate=0.45, density=2, tag="cuff")
        m.cube(f"{side}_forearm", (cx - 1.2, b.wrist, -1.2), (2.4, 0.6, 2.4), none_on(("up",), hand), density=2, tag="wrist")
    m.cube("left_hand", (b.ax - 1.15, b.wrist - 0.6, -1.35), (2.3, 0.5, 2.7), lambda f, x, y, w, h: [hexc("#7a2a6a"), TWINE, hexc("#3a6a5a")][x % 3],
           density=4, inflate=0.04, tag="bead_bracelet")
    slim_hands(m, b, hand, nails=H_SKIN[4])
    # The patched skirt, to mid-calf.
    skirt_m = worn(OLIVE, 31401, 2.8, 1.0, scuff=0.1)

    def patched(f, x, y, w, h):
        if f in ("north", "south", "east", "west"):
            px, py = (x + (7 if f == "south" else 0)) % 13, (y + (5 if f in ("east", "west") else 0)) % 11
            cell = ((x + (7 if f == "south" else 0)) // 13, (y + (5 if f in ("east", "west") else 0)) // 11, f)
            if 2 <= px <= 6 and 3 <= py <= 7 and h01("patch", cell) < 0.4:
                if px in (2, 6) or py in (3, 7):
                    return TWINE if (px + py) % 2 else PATCH[2]           # stitched edge
                return mix(tone(PATCH, 2.4 + 0.4 * ((px + py) % 2)), OLIVE[2], 0.35)
        return skirt_m(f, x, y, w, h)
    bell(m, "skirt", b.hip + 0.4, b.knee + 0.4, (3.0, 2.1), (3.7, 2.7), patched, n=3, tag="skirt")
    bell(m, "skirt_lower", b.knee + 0.5, 3.0, (3.75, 2.75), (4.3, 3.2), patched, n=2, tag="skirt_lower", cap=False)
    m.cube("skirt_lower", (-4.3, 3.0, -3.2), (8.6, 0.6, 6.4), none_on(("up",), lambda f, x, y, w, h: tone(OLIVE, 1.6 + (x % 2) * 0.6)),
           inflate=0.1, density=2, tag="hem")
    sock = knit_m(SOCK, 31402, 3.0, rib=True)
    womans_legs(m, b, sock, boot_shoe(BOOT), shoe_len=3.6, shoe_h=2.2)
    for side, s in SIDES:
        m.cube(f"{side}_shin", (b.lx * s - 1.3, 0, -1.3), (2.6, 2.4, 2.6), none_on(("down",), lambda f, x, y, w, h: BOOT[3] if (y % 2 == 0 and f == "north" and x in (2, 3)) else tone(BOOT, 2.4 + 0.4 * (y == 0))),
               density=2, tag="boot_top")
    # The belt, its charms, the satchel strap across her chest.
    strap = leather_m(STRAP, 31501, 2.8)
    m.cube("skirt", (-3.05, b.hip - 0.2, -2.15), (6.1, 0.9, 4.3), none_on(("up", "down"), strap), inflate=0.2, density=2, tag="belt")
    m.cube("skirt", (-0.6, b.hip - 0.3, -2.45), (1.2, 1.1, 0.3), lambda f, x, y, w, h: COPPER[4] if (x in (0, w - 1) or y in (0, h - 1)) else STRAP[1],
           density=4, tag="buckle")
    m.cube("body", (-0.6, b.hip, -2.25), (1.2, b.neck - b.hip + 0.6, 0.35), strap, density=2, tag="strap", rotation=(0, 0, 38),
           pivot=(0, (b.hip + b.neck) / 2, -2.1))
    m.cube("body", (-0.6, b.hip, 1.9), (1.2, b.neck - b.hip + 0.6, 0.35), strap, density=2, tag="strap_back", rotation=(0, 0, -38),
           pivot=(0, (b.hip + b.neck) / 2, 2.1))

    def satchel(f, x, y, w, h):
        c = strap(f, x, y, w, h)
        if f == "east" and y < 3:
            return tone(STRAP, 3.4 - (y == 2))                            # the flap
        if f == "east" and y == 3 and x == w // 2:
            return COPPER[4]
        return c
    m.cube("satchel", (3.4, b.hip - 3.8, -1.6), (1.6, 3.8, 3.4), satchel, density=2, tag="satchel")
    for i in range(3):
        bag = BAG[i]
        bone = f"hexbag_{i + 1}"
        bx = (-3.0, -1.4, 2.4)[i]
        m.cube(bone, (bx - 0.1, b.hip - 1.4, -2.65), (0.2, 1.2, 0.2), solid(TWINE), density=4, tag="twine")
        m.cube(bone, (bx - 0.7, b.hip - 3.1, -3.1), (1.4, 1.8, 1.2), lambda f, x, y, w, h, bag=bag: tone(bag, 2.8 + 0.6 * ((x + y) % 2) - 0.6 * (y == h - 1)),
               density=2, tag="hexbag", rotation=(0, 20 * (i - 1), 0), pivot=(bx, b.hip - 2.0, -2.6))
        m.cube(bone, (bx - 0.5, b.hip - 1.5, -2.95), (1.0, 0.4, 0.8), solid(bag[1]), density=4, tag="hexbag_neck")
    # A bundle of dried herbs (sage, lavender) tied at the stems, heads down.
    m.cube("herbs", (-3.8, b.hip - 1.6, 0.0), (0.8, 1.4, 0.8), solid(TWINE), density=4, tag="herb_tie")
    for k, (dx, dz, col) in enumerate(((0, 0, HERB), (-0.4, 0.4, HERB), (0.3, 0.5, None))):
        m.cube("herbs", (-3.9 + dx, b.hip - 4.8, -0.2 + dz), (1.0, 3.4, 1.0),
               lambda f, x, y, w, h, col=col: (LAV if y > h - 3 and (x + y) % 2 else tone(HERB, 2.4)) if col is None else tone(HERB, 2.0 + 1.2 * ((x + y * 2) % 3 == 0)),
               density=2, tag="herb", rotation=(0, 0, (k - 1) * 8), pivot=(-3.4, b.hip - 1.6, 0.4))
    # The little copper bowl hung from a cord on her hip.
    m.cube("bowl", (-3.1, b.hip - 1.6, 2.1), (0.2, 1.4, 0.2), solid(TWINE), density=4, tag="cord")
    bowlm = lambda f, x, y, w, h: COPPER[5] if f == "up" and (x in (0, w - 1) or y in (0, h - 1)) else (COPPER[1] if f == "up" else tone(COPPER, 3.4 - 1.4 * y / max(1, h) + 0.5 * (x % 3 == 0)))
    m.cube("bowl", (-4.1, b.hip - 2.8, 1.4), (2.2, 1.2, 2.2), bowlm, density=4, tag="bowl")
    m.cube("bowl", (-3.6, b.hip - 3.1, 1.9), (1.2, 0.3, 1.2), solid(COPPER[2]), density=4, tag="bowl_foot")
    return m


# =====================================================================================================
# The Coven Witch
# =====================================================================================================

C_SKIN = Ramp("#6f5550", "#a08479", "#c4a89c", "#d8c0b4", "#e6d3c9", "#f2e4dc")
C_HAIR = Ramp("#060507", "#0d0b0f", "#151219", "#1e1a24", "#2a2532", "#3a3444")
C_IRIS = hexc("#6a6e7a")
PLUM = Ramp("#14060f", "#25091b", "#380e29", "#4d1438", "#641c4a", "#7d275e")
INK = Ramp("#050406", "#0a090c", "#121015", "#1b181f", "#26222b", "#34303a")
C_LIPS = Ramp("#2a0a1c", "#41102a", "#5a183a", "#72214a", "#8a2c5a", "#a33a6c")

C_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhssshhhhhh",
    "hhhhssssssssshhh",
    "hhsssssssssssshh",
    "hssbBBBssBBBbssh",
    "hsKKKKssssKKKKsh",
    "hssWIEssssEIWssh",
    "hssuuussssuuussh",
    "hssssssnnssssssh",
    "hcsssssnnsssssch",
    "hsssssNnnNsssssh",
    "hssssssssssssssh",
    "hssssslMMlsssssh",
    "jssssssLLssssssj",
    "jjssssssssssssjj",
]


def runes(seed):
    """Magenta runes inked on the back of a hand (and its glow): little hooked glyphs in a ring."""
    def albedo_and_glow(f, x, y, w, h, base):
        if f not in ("north", "east", "west", "up"):
            return None
        if (x * 7 + y * 3 + seed) % 5 == 0 and h01("rn", seed, f, x, y) < 0.7:
            return MAGENTA[3]
        return None
    return albedo_and_glow


def coven_rig():
    m = Model("coven_witch", 256, 256)
    b = WB
    wskeleton(m, b)
    m.bone("hair", (0, b.neck + 4, 0), "head")
    m.bone("hair_bun", (0, b.neck + 2.0, 4.2), "hair")
    m.bone("pin", (0, b.neck + 2.4, 5.4), "hair_bun")
    m.bone("capelet", (0, b.neck - 0.2, 0), "body")
    m.bone("capelet_back", (0, b.neck - 3.4, 2.6), "capelet")
    m.bone("brooch", (0, b.neck - 0.6, -2.4), "body")
    sk = H.skin_mat(C_SKIN, 32101, 3.4, 0.3)
    hr = wavy_hair(C_HAIR, 32102, 3.0, wave=0.3, shine=0.9)
    y0 = b.neck
    rune = runes(32103)
    legend = {"h": hr, "s": sk, "b": C_HAIR[2], "B": C_HAIR[1], "K": LASH, "W": EYE_WHITE, "I": C_IRIS, "E": hexc("#121014"),
              "u": mix(C_SKIN[2], hexc("#4a2a4a"), 0.4), "n": C_SKIN[4], "N": C_SKIN[1], "c": mix(C_SKIN[2], C_LIPS[3], 0.15),
              "l": C_LIPS[1], "M": C_LIPS[3], "L": C_LIPS[4], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}

    def sides(f, x, y, w, h):
        if f in ("up", "south"):
            return hr(f, x, y, w, h) if f == "up" or y < 14 else sk(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x
        if y < 4 or (fx > 9 and y < 14) or (fx > 4 and y < 6):
            return hr(f, x, y, w, h)
        if 7 <= fx <= 9 and 6 <= y <= 9:
            return shade(sk(f, x, y, w, h), 0.85 if fx == 8 else 1.05)
        return sk(f, x, y, w, h)
    face_head(m, b, C_FACE, legend, sides, eye_glow(C_IRIS))
    nose_ears(m, b, C_SKIN, nose_h=1.5)
    m.cube("body", (-1.2, y0 - 0.8, -1.2), (2.4, 1.2, 2.4), sk, density=2, tag="neck")

    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            mid = (w - 1) / 2
            return hr(f, x, y, w, h) if y < 2 or (y < 4 and abs(x - mid) > 1.5 + y) else None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if fx > 4 or y < 4 else None
        return hr(f, x, y, w, h)
    m.cube("hair", (-4, y0 + 3.0, -4), (8, 5.0, 8), shell, inflate=0.3, density=2, tag="hair_shell")
    bunm = lambda f, x, y, w, h: tone(C_HAIR, 3.0 + 1.0 * math.sin(x * 1.1 + y * 0.8) + (0.6 if f == "up" else 0))
    m.cube("hair_bun", (-2.0, y0 + 0.6, 3.4), (4.0, 3.0, 2.4), bunm, density=2, tag="chignon")
    m.cube("hair_bun", (-1.4, y0 + 0.9, 5.4), (2.8, 2.4, 0.6), bunm, density=2, tag="chignon_round")
    m.cube("pin", (-2.6, y0 + 2.3, 5.2), (5.2, 0.3, 0.3), lambda f, x, y, w, h: SILVER[4] if x % 4 else MAGENTA[3], glow_k(0.0) if False else None,
           density=4, tag="hair_pin", rotation=(0, 0, 18), pivot=(0, y0 + 2.4, 5.4))
    m.cube("pin", (2.0, y0 + 2.9, 5.1), (0.6, 0.6, 0.6), gem(MAGENTA), glow_k(0.8), density=4, tag="pin_head", rotation=(0, 0, 18),
           pivot=(0, y0 + 2.4, 5.4))

    # --- the dress -------------------------------------------------------------------------------------------------
    vel = velvet(PLUM, 32201, 2.9, 0.8, 0.4)
    ink = velvet(INK, 32202, 3.0, 0.7, 0.3)

    def bodice(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north":
            if abs(x - mid) < 0.6:
                return SILVER[4] if y % 3 == 1 else INK[2]                 # buttons down the front
            if abs(x - mid) < 2.6:
                return ink(f, x, y, w, h)
            return vel(f, x, y, w, h)
        if f == "south":
            return INK[2] if abs(x - mid) < 0.6 and y % 2 else ink(f, x, y, w, h)  # back lacing
        return ink(f, x, y, w, h)
    m.cube("body", (-b.tw, WAIST, -2.0), (2 * b.tw, b.neck - WAIST, 4.0), bodice, density=2, tag="chest")
    m.cube("body", (-TWW, b.hip, -1.75), (2 * TWW, WAIST - b.hip + 0.2, 3.5), bodice, density=2, tag="waist")
    m.cube("body", (-2.7, 18.4, -2.55), (5.4, 2.4, 0.6), lambda f, x, y, w, h: None if f == "south" else bodice(f, x + 1, y + 6, w + 2, h),
           density=2, tag="bust", rotation=(-8, 0, 0), pivot=(0, 20.8, -2.2))
    m.cube("body", (-TWW, b.hip - 0.3, -1.75), (2 * TWW, 1.2, 3.5), none_on(("up", "down"), lambda f, x, y, w, h: SILVER[3] if y == 0 else INK[1]),
           inflate=0.22, density=2, tag="waistband")
    # The high standing collar.
    def collar(f, x, y, w, h):
        if f in ("down",):
            return None
        if f == "up":
            mid = (w - 1) / 2
            return None if abs(x - mid) < 1.6 and abs(y - (h - 1) / 2) < 1.6 else INK[3]
        if f == "north" and abs(x - (w - 1) / 2) < 0.6:
            return None
        return SILVER[3] if y == 0 else tone(INK, 2.4 + 0.5 * (x % 2))
    m.cube("body", (-1.7, y0 - 0.6, -1.7), (3.4, 2.4, 3.4), collar, inflate=0.15, density=2, tag="high_collar")
    m.cube("brooch", (-0.7, y0 - 1.3, -2.55), (1.4, 1.4, 0.3), lambda f, x, y, w, h: MAGENTA[4] if (x, y) in ((2, 2), (3, 2), (2, 3), (3, 3)) else
           (SILVER[4] if (x + y) % 2 else SILVER[2]), lambda f, x, y, w, h, c: MAGENTA[5] if c == MAGENTA[4] else None, density=4, tag="brooch")
    # The capelet over the shoulders, a deeper back that sways.
    def cape(f, x, y, w, h):
        if f == "down":
            return None
        mid = (w - 1) / 2
        if f == "up" and abs(x - mid) < 2.2 and abs(y - (h - 1) / 2) < 2.2:
            return None
        if f == "north" and abs(x - mid) < 1.2 + y * 0.4:
            return None
        if f != "up" and y == h - 1:
            return SILVER[3] if x % 2 else SILVER[1]
        if f != "up" and y == h - 2:
            return PLUM[3]
        return tone(INK, 3.4 + 0.6 * math.sin(x * 1.3) - 0.3 * y)
    m.cube("capelet", (-b.tw - 1.4, y0 - 4.0, -2.2), (2 * b.tw + 2.8, 3.8, 4.4), cape, inflate=0.3, density=2, tag="capelet")
    m.cube("capelet_back", (-b.tw - 1.2, y0 - 7.6, 2.3), (2 * b.tw + 2.4, 4.0, 0.4), none_on(("up", "north"),
           lambda f, x, y, w, h: SILVER[2] if y == h - 1 and x % 2 else ink(f, x, y + 8, w, h + 8)), density=2, tag="capelet_back")
    # Fitted sleeves in black, plum cuffs; bare hands with magenta runes.
    hand = H.skin_mat(C_SKIN, 32301, 3.4, 0.3)

    def runed(f, x, y, w, h):
        c = rune(f, x, y, w, h, None)
        return c if c is not None else hand(f, x, y, w, h)

    def rune_glow(f, x, y, w, h, c):
        return MAGENTA[5] if c == MAGENTA[3] else None
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.5 - b.elbow, b.aw), ink, inflate=0.15, density=2, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.35, b.wrist, -1.35), (2.7, b.elbow - b.wrist, 2.7), none_on(("up",), ink), inflate=0.12, density=2,
               tag="sleeve_lower")
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist - 0.4, -1.5), (3.0, 1.6, 3.0), none_on(("up",), lambda f, x, y, w, h: SILVER[3] if y == h - 1 else vel(f, x, y, w, h)),
               inflate=0.1, density=2, tag="cuff")
        m.cube(f"{side}_hand", (cx - 1.05, b.palm + 0.4, -1.25), (2.1, b.wrist - b.palm - 0.4, 2.5), runed, rune_glow, inflate=0.02, density=2,
               tag="runes")
    slim_hands(m, b, hand, nails=INK[2])
    m.cube("right_index", (-b.ax - 1.0, b.palm - 1.0, -1.3), (2.0, 0.3, 0.7), solid(SILVER[4]), glow_k(0.15), inflate=0.06, density=4, tag="ring")
    # The skirt: black over a plum underskirt that shows at the hem in a flounce.
    def skirtm(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north" and abs(x - mid) < 1.0 + 1.6 * y / max(1, h):
            return vel(f, x, y, w, h)                                     # the plum panel down the front
        return ink(f, x, y, w, h)
    bell(m, "skirt", b.hip + 0.5, b.knee + 0.4, (3.0, 2.1), (3.8, 2.8), skirtm, n=3, tag="skirt")

    def lowerm(f, x, y, w, h):
        if f not in ("up", "down") and y >= h - 3:
            return tone(PLUM, 3.6 - (y - (h - 3))) if (x + y) % 4 else PLUM[1]   # the flounce
        if f not in ("up", "down") and y == h - 4:
            return SILVER[2]
        return skirtm(f, x, y, w, h)
    bell(m, "skirt_lower", b.knee + 0.5, 0.15, (3.85, 2.85), (4.7, 3.5), lowerm, n=3, tag="skirt_lower", cap=False)
    womans_legs(m, b, solid(INK[1]), boot_shoe(INK), shoe_len=3.6, shoe_h=1.0, heel=0.5)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

def hedge_follow(pose):
    def extra(p, out):
        rl = p.get("right_leg", [0, 0, 0])[0]
        ll = p.get("left_leg", [0, 0, 0])[0]
        body = p.get("body", [0, 0, 0])[0]
        out.setdefault("cardigan_r", [min(0.0, rl) * 0.5 - body * 0.3, 0, 0])
        out.setdefault("cardigan_l", [min(0.0, ll) * 0.5 - body * 0.3, 0, 0])
        out.setdefault("cardigan_back", [max(0.0, max(rl, ll)) * 0.3 - body * 0.2, 0, 0])
        k = (ll - rl) * 0.15
        for i in range(3):
            out.setdefault(f"hexbag_{i + 1}", [min(rl, ll) * 0.3, 0, k * (1 + 0.3 * i)])
        out.setdefault("herbs", [0, 0, -k])
        out.setdefault("bowl", [max(rl, ll) * 0.2, 0, -k * 0.7])
        out.setdefault("braid", [-p.get("body", [0, 0, 0])[0] * 0.4, 0, 0])
    return gown_follow(pose, extra=extra)


def coven_follow(pose):
    def extra(p, out):
        body = p.get("body", [0, 0, 0])[0]
        rl = p.get("right_leg", [0, 0, 0])[0]
        ll = p.get("left_leg", [0, 0, 0])[0]
        out.setdefault("capelet_back", [-body * 0.5 + max(rl, ll) * 0.1, 0, 0])
    return gown_follow(pose, extra=extra)


HEDGE_STANCE = P_(REST, {"body": [3, -6, 0], "head": [4, 10, -3], "right_leg": [0, 4, 2], "left_leg": [-5, -8, -2], "left_shin": [10, 0, 0],
                         "right_arm": [-6, 0, 8], "right_forearm": [-20, 0, 0], "left_arm": [-12, 0, -6], "left_forearm": [-50, 0, 0],
                         "left_hand": [0, 0, -30]}, CURL_R, CURL_L)
COVEN_STANCE = P_(REST, {"body": [-2, 0, 0], "head": [-4, 0, 0], "right_arm": [-14, 0, 14], "left_arm": [-14, 0, -14],
                         "right_forearm": [-70, 0, 0], "left_forearm": [-70, 0, 0], "right_hand": [0, 0, -40], "left_hand": [0, 0, 40],
                         "left_leg": [0, 0, -1], "right_leg": [0, 0, 1]}, GRACE_R, GRACE_L)


def witch_anims(name, coven):
    m = coven_rig() if coven else hedge_rig()
    r = m.rig
    A = f"animation.{name}."
    f = AnimFile()
    clips = []
    follow = coven_follow if coven else hedge_follow
    stance = COVEN_STANCE if coven else HEDGE_STANCE

    def clip(n, length, loop=False, hold=False):
        clips.append(WClip(f, A + n, length, loop, hold, follow=follow))
        return clips[-1]

    # --- idle ---
    c = clip("idle", 4.0, loop=True)
    if coven:
        # Hands folded before her, poised and still, the head turning slowly like a bird of prey.
        for t, look in ((0, 0), (1.3, 22), (2.6, -16), (4.0, 0)):
            c.key(t, P_(stance, {"head": [-4, look, 0], "@root": [0, -0.1 * abs(look) / 22, 0]}), E_IO if t else None)
    else:
        # Fidgety: weight shifting, fingers working on a hex bag at her belt, a glance over her shoulder.
        for t, k, look in ((0, 0, 10), (1.0, 1, 24), (2.0, 0, -6), (3.0, -1, -30), (4.0, 0, 10)):
            c.key(t, P_(stance, {"body": [3 + abs(k), -6 + 3 * k, 0], "head": [4 + 2 * abs(k), look, -3 + 2 * k],
                                 "@root": [0, -0.1 * abs(k), 0]}), E_IO if t else None)
        c.layer("left_fingers", lambda t: [-20 * max(0, math.sin(t * 2 * math.pi * 4 / 4.0)), 0, 0])
        c.layer("strand_r", lambda t: [3 * math.sin(t * 2 * math.pi / 2.0), 0, 0])
        c.layer("strand_l", lambda t: [3 * math.sin(t * 2 * math.pi / 2.0 + 1), 0, 0])

    # --- walk ---
    n = 8
    L = 1.1 if coven else 1.0
    c = clip("walk", L, loop=True)
    for i in range(n + 1):
        w = gown_walk(n, i % n, stride=14 if coven else 20, arm=6 if coven else 14, knee=20 if coven else 30)
        extra = {"right_arm": [w["right_arm"][0] - 10, 0, 12], "left_arm": [w["left_arm"][0] - 10, 0, -12], "right_forearm": [-60, 0, 0],
                 "left_forearm": [-60, 0, 0], "head": [-4, w["head"][1], 0]} if coven else {"head": [6, w["head"][1], 0], "body": [4, w["body"][1], 0]}
        c.key(L * i / n, P_(REST, GRACE_R if coven else CURL_R, GRACE_L if coven else CURL_L, w, extra), E_IO if i else None)
    if not coven:
        c.layer("hexbag_1", lambda t: [8 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
        c.layer("hexbag_3", lambda t: [8 * math.sin(t * 2 * math.pi * 2 / L + 1), 0, 0])
        c.layer("braid_end", lambda t: [5 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])

    # --- cast: a hex thrown from the right hand (leaves at tick 10). ---
    c = clip("cast", 1.0)
    c.key(0, stance)
    wind = P_(stance, CLAW_R, {"body": [-4, 22, 0], "head": [0, -16, 0], "right_arm": [-40, 30, 40], "right_forearm": [-100, 0, 0],
                               "right_hand": [30, 0, 0], "left_arm": [-30, 0, -14], "left_forearm": [-40, 0, 0]})
    throw = P_(stance, OPEN_R, {"body": [8, -18, 0], "head": [4, 12, 0], "right_arm": [-88, -16, 0], "right_forearm": [-6, 0, 0],
                                "right_hand": [-50, 0, 0], "left_arm": [10, 0, -18], "left_forearm": [-20, 0, 0], "right_leg": [-12, 0, 0],
                                "left_leg": [10, 0, 0], "@root": [0, -0.3, -0.5]})
    c.key(0.32, wind, E_OUT)
    c.key(0.5, throw, E_SNAP)
    c.key(0.68, P_(throw, {"right_arm": [-80, -14, 0]}), E_OUT)
    c.key(1.0, stance, E_IO)

    # --- hex: both hands cupped over a taglock, muttering, then the curse is sealed with a clench (tick 16). ---
    c = clip("hex", 1.4)
    c.key(0, stance)
    cup = P_(stance, CURL_R, CURL_L, {"body": [16, 0, 0], "head": [26, 0, 0], "right_arm": [-46, -14, -6], "left_arm": [-46, 14, 6],
                                      "right_forearm": [-44, 0, 0], "left_forearm": [-44, 0, 0], "right_hand": [0, 0, 50],
                                      "left_hand": [0, 0, -50], "@root": [0, -0.3, 0]})
    seal = P_(cup, FIST_R, FIST_L, {"body": [22, 0, 0], "head": [30, 0, 0], "right_arm": [-40, -18, -8], "left_arm": [-40, 18, 8],
                                    "@root": [0, -0.7, 0]})
    c.key(0.35, cup, E_OUT)
    c.key(0.6, P_(cup, {"head": [22, -6, 0]}), E_IO)
    c.key(0.8, seal, E_SNAP)
    c.key(1.05, P_(seal, {"head": [-6, 0, 0], "body": [6, 0, 0]}), E_OUT)
    c.key(1.4, stance, E_IO)
    c.layer("right_fingers", lambda t: [-18 * math.sin(t * 40) if 0.35 < t < 0.75 else 0, 0, 0])
    c.layer("left_fingers", lambda t: [18 * math.sin(t * 40) if 0.35 < t < 0.75 else 0, 0, 0])

    # --- teleport: a crouch, arms wrapped, gone in smoke at tick 8; then she straightens where she lands. ---
    c = clip("teleport", 1.0)
    c.key(0, stance)
    crouch = P_(REST, FIST_R, FIST_L, {"@root": [0, -2.2, 0], "body": [24, 0, 0], "head": [20, 0, 0], "right_leg": [-30, 0, 4],
                                       "left_leg": [-30, 0, -4], "right_shin": [50, 0, 0], "left_shin": [50, 0, 0],
                                       "right_arm": [-70, -30, 0], "left_arm": [-70, 30, 0], "right_forearm": [-70, 0, 0],
                                       "left_forearm": [-70, 0, 0], "skirt": [-14, 0, 0], "%skirt": [1.06, 0.84, 1.06]})
    c.key(0.25, crouch, E_OUT)
    c.key(0.4, P_(crouch, {"@root": [0, -2.6, 0]}), E_IO)
    c.key(0.6, P_(crouch, {"@root": [0, -1.6, 0]}), E_OUT)
    c.key(1.0, stance, E_IO)

    if coven:
        # --- channel: both arms raised, palms up and out, feeding the Grand Witch; a slow tremor of power. ---
        L = 2.0
        c = clip("channel", L, loop=True)
        up = P_(REST, OPEN_R, OPEN_L, {"body": [-8, 0, 0], "head": [-24, 0, 0], "right_arm": [-150, -10, -14], "left_arm": [-150, 10, 14],
                                       "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "right_hand": [-30, 0, 0],
                                       "left_hand": [-30, 0, 0], "@root": [0, 0.3, 0], "capelet_back": [14, 0, 0]})
        for i in range(5):
            t = L * i / 4
            k = math.sin(2 * math.pi * i / 4)
            c.key(t, P_(up, {"right_arm": [-150 + 4 * k, -10, -14], "left_arm": [-150 - 4 * k, 10, 14], "head": [-24 - 3 * k, 0, 0],
                             "@root": [0, 0.3 + 0.2 * k, 0]}), E_IO if i else None)
        c.layer("right_hand", lambda t: [0, 0, 3 * math.sin(t * 2 * math.pi * 8 / L)])
        c.layer("left_hand", lambda t: [0, 0, -3 * math.sin(t * 2 * math.pi * 8 / L + 1)])
        c.layer("skirt_lower", lambda t: [2 * math.sin(t * 2 * math.pi / L), 0, 2 * math.sin(t * 2 * math.pi * 2 / L)])

        # --- shield: arms flung up and apart, palms out, raising the bubble round her; settles into a guard. ---
        c = clip("shield", 1.2)
        c.key(0, stance)
        brace = P_(REST, OPEN_R, OPEN_L, {"body": [-6, 0, 0], "head": [-8, 0, 0], "right_arm": [-24, 0, 138], "left_arm": [-24, 0, -138],
                                          "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "right_hand": [0, 70, 0],
                                          "left_hand": [0, -70, 0], "right_leg": [0, 0, 6], "left_leg": [0, 0, -6], "@root": [0, -0.3, 0],
                                          "capelet_back": [20, 0, 0], "skirt_lower": [0, 0, 0]})
        c.key(0.3, brace, E_BACK)
        c.key(0.7, P_(brace, {"right_arm": [-22, 0, 130], "left_arm": [-22, 0, -130]}), E_IO)
        c.key(1.2, stance, E_IO)
    done(clips)
    return f, clips


ANIMS = {"hedge_rig": lambda: witch_anims("hedge_witch", False), "coven_rig": lambda: witch_anims("coven_witch", True)}


def generate():
    for name, rigf, coven in (("hedge_witch", hedge_rig, False), ("coven_witch", coven_rig, True)):
        m = rigf()
        t, g = m.build(gutter=1, seed=sum(map(ord, name)))
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        f, _ = witch_anims(name, coven)
        write_compact(f, ANIM + name + ".animation.json")


if __name__ == "__main__":
    generate()
