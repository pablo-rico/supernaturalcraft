"""Rowena MacLeod (v0.21): the boss and `rowena_npc` (one rig, texture and clip file; names in RowenaAssets), plus her
grimoire's item sprite and her bust on the trophies' pedestal.

Three hundred years of witchcraft and a taste for the finer things: long wavy red hair half pinned up (a coil of curls at
the crown, ringlets at her temples, the rest falling down her back), pale skin, blue eyes lined dark, red lips. An emerald
velvet gown, Victorian-cut: a fitted bodice with a black lace yoke to a little standing collar, a black satin corset belt
with a gold clasp, velvet sleeves to the elbow ending in black lace bells; a skirt of emerald velvet to the floor under an
overskirt of black lace open at the front, gathered into a bustle behind. Gold everywhere: drop earrings, a ruby pendant,
rings on both hands (garnet, emerald; they glow while she casts), and her grimoire on a gold chain at her left hip.

Props:
  grimoire       the closed book at her left hip (RowenaAssets.PROCEDURAL: no clip keys it; the renderer hides it while
                 `recite` or `read` plays, when the book is in her hand instead);
  grimoire_open  the open book in her left hand, switched by scale: every loop keys it (shown in `recite`/`read`, gone
                 otherwise); `grimoire_page` turns over it in `read`.
  mouth          her open mouth, tucked inside the head at rest; the talking clips slide it forward.

Skeleton (witchkit's woman plus):
  root -- body -- head -- hair -- bun, curl_r, curl_l;  hair_back -- hair_tail;  brows, mouth, earring_r, earring_l
               -- pendant;  collar
               -- right_arm -- right_forearm -- cuff_r;  right_hand -- fingers, index, thumb, ring_r
               -- left_arm  -- left_forearm  -- cuff_l;  left_hand  -- fingers, index, thumb, ring_l, grimoire_open -- grimoire_page
       -- legs;  skirt -- skirt_lower;  skirt -- bustle -- bustle_tail;  skirt -- grimoire -- grimoire_clasp
"""

import math

import blockmodel as BM
import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from legacy_art import SIDES, done, reach as _reach
from michael_art import ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, aim, aim2, to_preview, tone, world_point, write_compact
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade, item_outline
from witchkit import (CLEAR, CLAW_L, CLAW_R, CURL_L, CURL_R, EYE_WHITE, FIST_L, FIST_R, GRACE_L, GRACE_R, OPEN_L, OPEN_R, POINT_R,
                      REST, TWW, WAIST, WB, WClip, bell, boot_shoe, face_head, gem, glow_k, gown_follow, gown_walk, kneel_pose,
                      lace, metal, nose_ears, satin, slim_hands, velvet, wavy_hair, womans_legs, wskeleton)

A = "animation.rowena."
PREVIEW_HIDDEN = ("grimoire_open",)
NAME = "rowena"

SKIN = Ramp("#7a5248", "#ad8173", "#cfa494", "#e2bcab", "#eecfc0", "#f7e2d6")
HAIR = Ramp("#3a0c05", "#5e1608", "#82230c", "#a63514", "#c64c1f", "#df6a32")
EMERALD = Ramp("#041610", "#08261b", "#0d3627", "#134a35", "#1b6045", "#277a58")
LACE = Ramp("#050405", "#0b090b", "#141114", "#1e1a1e", "#2a252a", "#3a343a")
SATIN = Ramp("#040404", "#0b0a0b", "#151315", "#211e21", "#322e32", "#4a454a")
GOLD = Ramp("#4f340c", "#7d5614", "#ad7f1f", "#d6a934", "#efcf5e", "#fff0a6")
RUBY = Ramp("#2a0206", "#55050d", "#850b17", "#b81627", "#e0384a", "#ff8a92")
GREEN_GEM = Ramp("#02200f", "#04401c", "#07662e", "#109145", "#2fc26a", "#9cf2bc")
IRIS = hexc("#4b7fa3")
PUPIL = hexc("#18222b")
LASH = hexc("#1a0c0a")
LIPS = Ramp("#4e0b10", "#6f1219", "#921b24", "#ad2630", "#c73a42", "#dd5a5f")
SHOE = Ramp("#050404", "#0c0a0a", "#151212", "#201b1b", "#2e2828", "#463e3e")
LEATHER = Ramp("#1e0f08", "#33190d", "#4b2513", "#63331c", "#7b4427", "#945936")
PAGE = Ramp("#6b5636", "#93794f", "#b89e70", "#d4bd8f", "#e8d6ad", "#f6ead0")
INK = hexc("#3a1a12")
MOUTH = Ramp("#1a0405", "#33090b", "#4d1013", "#661a1c", "#7f2627", "#993535")
TEETH = hexc("#ece4d6")
SPELL = hexc("#ff9a5a")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhsssshhhhhh",
    "hhhsssssssssshhh",
    "hhsbBBbssbBBbshh",
    "hssKKKKssKKKKssh",
    "hsKWIEWssWEIWKsh",
    "hssuIIussuIIussh",
    "hssssssnnssssssh",
    "hcsssssnnsssssch",
    "hcssssNnnNssssch",
    "hssssssssssssssh",
    "hssssslMMlsssssh",
    "hjsssssLLsssssjh",
    "hjjssssssssssjjh",
]


# =====================================================================================================
# The rig
# =====================================================================================================

def rig():
    m = Model(NAME, 512, 512)
    b = WB
    wskeleton(m, b)
    m.bone("hair", (0, b.neck + 4, 0), "head")
    m.bone("bun", (0, b.neck + 8.2, 2.2), "hair")
    m.bone("curl_r", (-3.6, b.neck + 5.5, -2.4), "hair")
    m.bone("curl_l", (3.6, b.neck + 5.5, -2.4), "hair")
    m.bone("hair_back", (0, b.neck + 5.0, 3.6), "head")
    m.bone("hair_tail", (0, b.neck - 3.0, 4.2), "hair_back")
    m.bone("brows", (0, b.neck + 5.3, -4.0), "head")
    m.bone("mouth", (0, b.neck + 2.2, -4.0), "head")
    for side, s in SIDES:
        m.bone(f"earring_{side[0]}", (4.05 * s, b.neck + 2.4, -0.2), "head")
        m.bone(f"cuff_{side[0]}", (b.ax * s, b.wrist + 1.6, 0), f"{side}_forearm")
        m.bone(f"ring_{side[0]}", (b.ax * s, b.palm, 0), f"{side}_hand")
    m.bone("pendant", (0, b.neck - 1.0, -2.3), "body")
    m.bone("collar", (0, b.neck - 0.3, 0), "body")
    m.bone("bustle", (0, b.hip + 0.4, 2.6), "skirt")
    m.bone("bustle_tail", (0, b.hip - 3.6, 3.6), "bustle")
    m.bone("grimoire", (4.0, b.hip + 0.6, -0.4), "skirt")
    m.bone("grimoire_clasp", (4.9, b.hip - 3.0, -0.4), "grimoire")
    m.bone("grimoire_open", (b.ax, b.palm - 0.6, -1.0), "left_hand")
    m.bone("grimoire_page", (b.ax, b.palm - 0.2, -1.0), "grimoire_open")
    build_head(m)
    build_body(m)
    build_arms(m)
    build_gown(m)
    build_grimoire(m)
    return m


GLOW_EYES = None   # set per cube below


def build_head(m):
    b = WB
    y0 = b.neck
    sk = H.skin_mat(SKIN, 21101, 3.35, 0.35)
    hr = wavy_hair(HAIR, 21102, 2.8)

    def eye_glow(f, x, y, w, h, c):
        if c in (IRIS, PUPIL):
            return hexc("#ffb070")                     # her eyes kindle when she casts
        return None
    legend = {"h": hr, "s": sk, "b": HAIR[2], "B": HAIR[1], "K": LASH, "W": EYE_WHITE, "I": IRIS, "E": PUPIL,
              "u": mix(SKIN[2], hexc("#5a3a4a"), 0.35), "n": SKIN[4], "N": SKIN[1], "c": mix(SKIN[3], LIPS[4], 0.22),
              "l": LIPS[1], "M": LIPS[3], "L": LIPS[4], "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.9)}

    def sides(f, x, y, w, h):
        if f in ("up", "south"):
            return hr(f, x, y, w, h)
        if f == "down":
            return sk(f, x, y, w, h)
        fx = x if f == "west" else w - 1 - x
        # The hair falls over her ears; a strip of cheek shows at the front below the temple.
        if fx < 3 and y >= 5:
            return sk(f, x, y, w, h)
        return hr(f, x, y, w, h)
    face_head(m, b, FACE, legend, sides, eye_glow)
    nose_ears(m, b, SKIN, ears=False)
    m.cube("body", (-1.2, y0 - 0.8, -1.2), (2.4, 1.2, 2.4), sk, density=2, tag="neck")
    # Brows (their own bone: they arch when she is amused).
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - 1.1, y0 + 5.05, -4.12), (2.2, 0.4, 0.2), lambda f, x, y, w, h: HAIR[1] if x % 3 else HAIR[0],
               density=4, tag="brow", rotation=(0, 0, 7 * s), pivot=(s * 1.2, y0 + 5.2, -4.1))
    # The mouth: tucked behind the lips at rest; talking clips slide it out.
    def mouth(f, x, y, w, h):
        if y == 0:
            return TEETH if 1 <= x <= w - 2 else LIPS[2]
        if y == h - 1:
            return LIPS[4]
        return MOUTH[1] if 1 <= x <= w - 2 else LIPS[2]
    m.cube("mouth", (-1.1, y0 + 1.1, -3.7), (2.2, 1.0, 0), mouth, faces=("north",), density=4, tag="mouth")

    # --- the hair -----------------------------------------------------------------------------------------------
    def shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            # Parted in the middle, swept to the sides in two soft waves.
            mid = (w - 1) / 2
            if y < 2 or (y < 4 and abs(x - mid) > 2.5 + y * 0.5):
                return shade(hr(f, x, y, w, h), 1.08)
            return None
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x
            return hr(f, x, y, w, h) if fx > 2 or y < 4 else None
        c = hr(f, x, y, w, h)
        return HAIR[1] if f == "up" and abs(x - (w - 1) / 2) < 0.6 and y < h * 0.45 else c
    m.cube("hair", (-4, y0 + 3.0, -4), (8, 5.0, 8), shell, inflate=0.35, density=2, tag="hair_shell")
    # Volume over the temples: the waves swept back from the parting.
    for s in (-1, 1):
        m.cube("hair", (s * 2.6 - 1.9, y0 + 6.1, -4.55), (3.8, 1.6, 3.4), none_on(("down", "south"), hr), density=2, tag="hair_wave",
               rotation=(-14, 0, -10 * s), pivot=(s * 2.6, y0 + 7.0, -3.0))
    # The crown coil: curls pinned up at the back of the head.
    bun = "bun"
    cy, cz = y0 + 8.2, 2.2

    def coil(f, x, y, w, h):
        cx_, cy_ = (w - 1) / 2, (h - 1) / 2
        a = math.atan2(y - cy_, x - cx_)
        r = math.hypot((x - cx_) / max(1.0, w / 2), (y - cy_) / max(1.0, h / 2))
        v = 2.8 + 0.9 * math.cos(r * 8.0 + a * 2) + (0.5 if f == "up" else 0) - (0.9 if f == "down" else 0)
        return tone(HAIR, v)
    for i, (dx, dy, dz, sz, rot) in enumerate(((0, 0, 0, (4.4, 2.4, 3.4), (0, 0, 0)), (-1.6, 0.6, 0.6, (2.2, 2.2, 2.2), (20, 30, 15)),
                                                (1.7, 0.5, 0.5, (2.2, 2.2, 2.2), (-15, -25, -20)), (0, 1.3, -0.6, (2.6, 1.8, 2.4), (10, 45, 0)),
                                                (0.2, -0.4, 1.6, (2.4, 2.0, 1.6), (-20, 10, 30)))):
        x0, y0_, z0 = dx, cy + dy, cz + dz
        m.cube(bun, (x0 - sz[0] / 2, y0_ - sz[1] / 2, z0 - sz[2] / 2), sz, coil, density=2, tag="curl", rotation=rot, pivot=(x0, y0_, z0))
    # A gold comb pinned in the coil.
    m.cube(bun, (-1.8, cy + 0.9, cz - 1.9), (3.6, 0.6, 0.4), lambda f, x, y, w, h: GOLD[4] if (x % 3 == 0 or f == "up") else GOLD[2],
           glow_k(0.15), density=4, tag="comb", rotation=(-20, 0, 0), pivot=(0, cy + 1.2, cz - 1.7))
    # Ringlets at her temples, falling to the jaw: each a twist of small turned blocks.
    for side, s in SIDES:
        bone = f"curl_{side[0]}"
        for k in range(5):
            yy = y0 + 5.2 - k * 1.25
            m.cube(bone, (s * 3.75 - 0.75 + 0.15 * s * (k % 2), yy - 1.2, -3.15), (1.5, 1.35, 1.5),
                   lambda f, x, y, w, h, k=k: tone(HAIR, 3.2 + (0.8 if (y == 0 and f != "down") else 0) - 0.15 * k - (0.6 if f in ("south",) else 0)),
                   density=2, tag="ringlet", rotation=(0, 45 if k % 2 else 0, 0), pivot=(s * 3.75, yy, -2.4))
    # The fall down her back: a thick sheet of waves from the crown to below the shoulder blades, then the ends.
    def fall(f, x, y, w, h):
        if f in ("north", "up"):
            return None
        return hr(f, x, y, w, h)
    m.cube("hair_back", (-4.5, y0 - 3.0, 3.0), (9.0, 8.4, 1.8), fall, inflate=0.1, density=2, tag="hair_fall")
    for s in (-1, 1):
        m.cube("hair_back", (s * 4.1 - 0.8, y0 - 2.6, 0.2), (1.6, 7.0, 3.2), none_on(("up",), hr), density=2, tag="hair_side")

    def ends(f, x, y, w, h):
        if f == "up":
            return None
        if f != "down" and y >= h - 3:
            # The ends curl up in points.
            if (x * 5 + y * 3) % 7 < (y - (h - 4)) * 2:
                return None
        return hr(f, x, y + 17, w, h + 17)
    m.cube("hair_tail", (-4.3, y0 - 9.6, 3.2), (8.6, 7.0, 1.7), ends, density=2, tag="hair_ends", rotation=(-6, 0, 0),
           pivot=(0, y0 - 3.0, 4.2))
    # Earrings: a gold hook and a long drop with a garnet.
    for side, s in SIDES:
        e = f"earring_{side[0]}"
        ex = 4.15 * s
        m.cube(e, (ex - 0.15, y0 + 1.6, -0.35), (0.3, 0.8, 0.3), solid(GOLD[4]), glow_k(0.15), density=4, tag="earring_hook")
        m.cube(e, (ex - 0.3, y0 + 0.6, -0.5), (0.6, 1.0, 0.6), gem(RUBY), glow_k(0.6), density=4, tag="earring_drop")


def bodice_m(seed):
    vel = velvet(EMERALD, seed, 2.9, 0.8, 0.35)

    def m(f, x, y, w, h):
        if f == "north":
            # The square neckline (the lace yoke goes over it).
            mid = (w - 1) / 2
            if y < 5 and abs(x - mid) < 4.0:
                return SKIN[3] if y > 0 else SKIN[2]
        return vel(f, x, y, w, h)
    return m


def build_body(m):
    b = WB
    sk = H.skin_mat(SKIN, 21201, 3.3, 0.3)
    bod = bodice_m(21202)
    lc = lace(LACE, 21203, 0.3)
    m.cube("body", (-b.tw, WAIST, -2.0), (2 * b.tw, b.neck - WAIST, 4.0), bod, density=2, tag="chest")
    m.cube("body", (-TWW, b.hip, -1.75), (2 * TWW, WAIST - b.hip + 0.2, 3.5), velvet(EMERALD, 21204, 2.6, 0.6, 0.3), density=2, tag="waist")

    # The bust, velvet below the lace.
    def bust(f, x, y, w, h):
        if f == "south":
            return None
        if f == "up":
            return EMERALD[4]
        if f == "north" and y == 0:
            return GOLD[3] if x % 2 else GOLD[2]
        return tone(EMERALD, 3.4 - 1.2 * y / max(1, h) + 0.6 * math.sin(x * 0.9))
    m.cube("body", (-2.8, 18.4, -2.6), (5.6, 2.6, 0.7), bust, density=2, tag="bust", rotation=(-8, 0, 0), pivot=(0, 21.0, -2.2))
    # The lace yoke: over the shoulders and the upper chest to the throat (skin through its holes).
    def yoke(f, x, y, w, h):
        if f == "down":
            return None
        if f in ("north", "south", "east", "west") and y >= 6:
            return None
        if f == "up" and abs(x - (w - 1) / 2) < 2.4 and abs(y - (h - 1) / 2) < 2.4:
            return None
        c = lc(f, x, y, w, h)
        if f != "up" and y == 5:
            return tone(LACE, 3.2) if x % 2 else tone(LACE, 1.6)            # the scalloped edge
        return c
    m.cube("body", (-b.tw, b.neck - 3.0, -2.0), (2 * b.tw, 3.0, 4.0), yoke, inflate=0.22, density=2, tag="lace_yoke")
    # The little standing collar of lace, open a touch at the front.
    def collar(f, x, y, w, h):
        if f in ("down", "up"):
            return None
        if f == "north" and abs(x - (w - 1) / 2) < 0.8:
            return None
        if y == 0:
            return tone(LACE, 3.6) if x % 2 == 0 else None
        return tone(LACE, 2.4 + 0.8 * ((x + y) % 2))
    m.cube("collar", (-1.5, b.neck - 0.4, -1.5), (3.0, 1.4, 3.0), collar, inflate=0.2, density=2, tag="collar")
    # The corset belt (black satin, laced in front) and a gold clasp.
    belt = satin(SATIN, 21205, 2.6)

    def corset(f, x, y, w, h):
        if f in ("up", "down"):
            return None
        if f == "north":
            mid = (w - 1) / 2
            if abs(x - mid) < 0.6:
                return SATIN[0]
            if abs(x - mid) < 2.0 and (y + int(abs(x - mid) * 2)) % 3 == 0:
                return GOLD[2]                                    # the lacing
        return belt(f, x, y, w, h)
    m.cube("body", (-TWW, b.hip - 0.2, -1.75), (2 * TWW, WAIST - b.hip + 0.9, 3.5), corset, inflate=0.18, density=2, tag="corset")
    m.cube("body", (-0.9, b.hip - 0.6, -2.15), (1.8, 1.2, 0.3), lambda f, x, y, w, h: GOLD[4] if (x + y) % 3 == 0 else GOLD[2],
           glow_k(0.1), density=4, tag="clasp")
    # Puffed shoulders.
    for s in (-1, 1):
        m.cube("body", (s * (b.tw + 0.1) - 1.0, b.neck - 1.6, -1.6), (2.0, 1.6, 3.2), lambda f, x, y, w, h: tone(EMERALD, 3.8 if f == "up" else 2.8 + 0.6 * (x % 2)),
               density=2, tag="puff")
    # The ruby pendant on a fine chain.
    m.cube("pendant", (-1.4, b.neck - 1.2, -2.32), (2.8, 0.2, 0.1), lambda f, x, y, w, h: GOLD[4] if x % 2 else GOLD[2], glow_k(0.1),
           density=4, tag="chain", rotation=(0, 0, 0))
    m.cube("pendant", (-0.5, b.neck - 2.6, -2.55), (1.0, 1.3, 0.4), gem(RUBY), glow_k(0.6), density=4, tag="ruby")
    m.cube("pendant", (-0.6, b.neck - 1.5, -2.45), (1.2, 0.3, 0.3), solid(GOLD[3]), glow_k(0.1), density=4, tag="setting")


def build_arms(m):
    b = WB
    hand = H.skin_mat(SKIN, 21301, 3.35, 0.3)
    vel = velvet(EMERALD, 21302, 2.8, 0.7, 0.3)
    lc = lace(LACE, 21303, 0.25)
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        m.cube(f"{side}_arm", (cx - hw, b.elbow, -hw), (b.aw, b.sy + 0.4 - b.elbow, b.aw), vel, density=2, inflate=0.15, tag="sleeve")
        # The forearm in sheer lace over skin.
        m.cube(f"{side}_forearm", (cx - 1.25, b.wrist, -1.25), (2.5, b.elbow - b.wrist, 2.5), none_on(("up",), hand), density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.25, b.wrist + 1.4, -1.25), (2.5, b.elbow - b.wrist - 1.4, 2.5), none_on(("up", "down"), lc),
               inflate=0.12, density=2, tag="lace_sleeve")
        m.cube(f"{side}_forearm", (cx - hw, b.elbow - 0.8, -hw), (b.aw, 0.8, b.aw), none_on(("up", "down"),
               lambda f, x, y, w, h: GOLD[3] if y == 0 else EMERALD[1]), inflate=0.2, density=2, tag="sleeve_band")
        # The lace bell flaring over the wrist.
        def bellm(f, x, y, w, h):
            if f in ("up",):
                return None
            if f == "down":
                return None
            if y >= h - 1:
                return tone(LACE, 3.4) if x % 2 else None
            c = lc(f, x, y + 3, w, h)
            return c
        cuff = f"cuff_{side[0]}"
        m.cube(cuff, (cx - 1.6, b.wrist - 0.6, -1.6), (3.2, 2.4, 3.2), bellm, inflate=0.1, density=2, tag="lace_bell")
        m.cube(cuff, (cx - 1.9, b.wrist - 1.2, -1.9), (3.8, 1.0, 3.8), bellm, density=2, tag="lace_bell_hem")
        # Rings: gold bands with stones (they glow while she casts).
        ring = f"ring_{side[0]}"
        stone = RUBY if s < 0 else GREEN_GEM
        m.cube(ring, (cx - 1.05, b.palm - 0.75, -1.35), (2.1, 0.35, 0.85), solid(GOLD[3]), glow_k(0.25), inflate=0.06, density=4, tag="ring")
        m.cube(ring, (cx - 0.35, b.palm - 0.8, -1.55), (0.7, 0.5, 0.4), gem(stone), glow_k(1.0), density=4, tag="ring_stone")
        m.cube(f"{side}_hand", (cx - 1.1, b.wrist - 0.5, -1.3), (2.2, 0.4, 2.6), lambda f, x, y, w, h: GOLD[4] if (x + y) % 2 else GOLD[2],
               glow_k(0.2), inflate=0.05, density=4, tag="bracelet")
    slim_hands(m, b, hand, nails=LIPS[3])
    # A second ring on the right middle and on the left little finger.
    m.cube("right_fingers", (-b.ax - 1.05, b.palm - 0.7, -0.5), (2.1, 0.3, 1.8), solid(GOLD[4]), glow_k(0.25), inflate=0.06, density=4,
           tag="ring")
    m.cube("left_fingers", (b.ax - 1.05, b.palm - 0.7, -0.5), (2.1, 0.3, 1.8), solid(GOLD[3]), glow_k(0.25), inflate=0.06, density=4,
           tag="ring")


def build_gown(m):
    b = WB
    upper = velvet(EMERALD, 21401, 2.7, 0.8, 0.55)
    lower = velvet(EMERALD, 21402, 2.6, 0.8, 0.6)
    trim = lace(LACE, 21403, 0.35)

    def hemmed(base_m, rows):
        def mm(f, x, y, w, h):
            if f not in ("up", "down") and y >= h - rows:
                if y == h - rows:
                    return GOLD[2]
                c = trim(f, x, y, w, h)
                return c if c is not None else EMERALD[0]
            return base_m(f, x, y, w, h)
        return mm
    bell(m, "skirt", b.hip + 0.6, b.knee + 0.4, (3.05, 2.15), (3.9, 2.85), upper, n=3, tag="skirt")
    bell(m, "skirt_lower", b.knee + 0.5, 0.15, (3.95, 2.9), (4.9, 3.6), hemmed(lower, 3), n=3, tag="skirt_lower", cap=False)
    # The lace overskirt, open in a widening V at the front (the velvet shows through and below it).
    lc = lace(LACE, 21404, 0.7)

    def over(top_gap, bot_gap, total, off):
        def mm(f, x, y, w, h):
            if f in ("up", "down"):
                return None
            yy = y + off
            if f == "north":
                mid = (w - 1) / 2
                gap = top_gap + (bot_gap - top_gap) * yy / max(1, total)
                if abs(x - mid) < gap:
                    return None
                if abs(x - mid) < gap + 1:
                    return GOLD[2] if yy % 2 else tone(LACE, 3.0)
            if yy >= total - 1:
                return tone(LACE, 3.2) if x % 2 else None
            return lc(f, x, yy, w, h)
        return mm
    total = int(round((b.hip + 0.6 - 2.0) * 2))
    up_tx = int(round((b.hip + 0.6 - (b.knee + 0.4)) * 2))
    m.cube("skirt", (-3.2, b.knee + 0.4, -2.3), (6.4, b.hip + 0.6 - (b.knee + 0.4), 4.6), over(1.5, 6.0, total, 0), inflate=0.3,
           density=2, tag="overskirt")
    m.cube("skirt_lower", (-4.2, 2.0, -3.05), (8.4, b.knee + 0.5 - 2.0, 6.1), over(1.5, 6.0, total, up_tx), inflate=0.3, density=2,
           tag="overskirt_lower")
    # The bustle: the overskirt gathered up behind in puffs, a tail falling from it.
    bus = lambda f, x, y, w, h: tone(LACE, 2.0 + 1.0 * math.sin(x * 1.3) + (0.8 if f == "up" else 0))
    for i, (dx, dy, sz, rz) in enumerate(((0, -1.0, (5.6, 2.2, 1.8), 0), (-1.6, -2.4, (2.8, 2.0, 1.6), 12), (1.6, -2.4, (2.8, 2.0, 1.6), -12))):
        x0, y0, z0 = dx, b.hip + 0.4 + dy, 3.1
        m.cube("bustle", (x0 - sz[0] / 2, y0 - sz[1] / 2, z0 - sz[2] / 2), sz, none_on(("north",), bus), density=2, tag="bustle",
               rotation=(0, 0, rz), pivot=(x0, y0, z0))

    def tail(f, x, y, w, h):
        if f in ("up", "north"):
            return None
        if f != "down" and y >= h - 1:
            return tone(LACE, 3.2) if x % 2 else None
        c = lc(f, x, y, w, h)
        return c if c is not None else (EMERALD[1] if f == "south" and (x + y) % 3 == 0 else None)
    m.cube("bustle_tail", (-3.4, 1.4, 3.3), (6.8, b.hip - 3.6 - 1.4, 0.4), tail, density=2, tag="bustle_tail", rotation=(8, 0, 0),
           pivot=(0, b.hip - 3.6, 3.6))
    # Legs (black stockings, pointed black boots with a little heel): hidden but for a toe at the hem and when she kneels.
    st = lambda f, x, y, w, h: tone(SHOE, 2.4 + 0.5 * math.sin(x))
    womans_legs(m, b, st, boot_shoe(SHOE), shoe_len=3.8, shoe_h=1.0, heel=0.6)


def build_grimoire(m):
    """The grimoire: worn oxblood leather over thick boards, gold corners, a red clasp, a witch's sigil tooled on the
    cover; closed at her hip on a gold chain; and its open twin for her hand."""
    b = WB
    gx, gy = 4.0, b.hip + 0.6
    # The chain from the belt down to the book's spine.
    m.cube("grimoire", (gx - 0.15, gy - 1.6, -0.55), (0.3, 1.6, 0.3), lambda f, x, y, w, h: GOLD[4] if y % 2 else GOLD[2], glow_k(0.1),
           density=4, tag="chain")
    bx0, by0, bz0 = gx + 0.1, gy - 6.0, -2.0

    def cover(f, x, y, w, h):
        # Facing out (east, her left side): the tooled sigil -- a ring, a pentacle-ish star, gold corners.
        n = h01("lea", f, x, y)
        c = tone(LEATHER, 2.6 + (n - 0.5) * 1.2)
        if f == "east":
            cx_, cy_ = (w - 1) / 2, (h - 1) / 2
            r = math.hypot(x - cx_, y - cy_)
            if (x in (0, w - 1) or y in (0, h - 1)) and (x < 2 or x > w - 3) and (y < 2 or y > h - 3):
                return GOLD[3]
            if 2.4 < r < 3.2:
                return GOLD[2]
            a = math.atan2(y - cy_, x - cx_)
            if r < 2.4 and abs(math.sin(a * 2.5)) < 0.25:
                return GOLD[2]
            return c
        if f in ("north", "south"):
            return PAGE[3] if 0 < y < h - 1 and f == "north" and 0 < x < w - 1 and x % 2 == 0 else c
        if f == "up" or f == "down":
            return PAGE[3] if 0 < x < w - 1 else c
        return shade(c, 0.9)
    turn = dict(rotation=(0, 35, 0), pivot=(gx, gy, -0.4))
    m.cube("grimoire", (bx0, by0, bz0), (1.4, 4.4, 3.2), cover, density=4, tag="grimoire", **turn)
    m.cube("grimoire_clasp", (bx0 + 0.9, gy - 4.3, -0.6), (0.7, 0.9, 0.5), gem(RUBY), glow_k(0.35), density=4, tag="clasp", **turn)
    # The open book: lying flat at rest (pages up, the spine along Z), two covers and two fanned page blocks.
    ox, oy, oz = b.ax, b.palm - 0.6, -1.0

    def page(f, x, y, w, h):
        if f == "up":
            if x in (0, w - 1) or y in (0, h - 1):
                return PAGE[2]
            if 1 < y < h - 2 and (x % 4 != 0) and y % 2 == 0 and h01("ink", x, y) < 0.75:
                return mix(INK, PAGE[3], 0.25)
            if (x // 4 + y // 5) % 5 == 0 and h01("sig", x, y) < 0.4:
                return RUBY[3]
            return tone(PAGE, 4.0 - 0.2 * abs(x - (w - 1) / 2) / max(1, w / 2))
        return PAGE[2] if y % 2 else PAGE[3]
    for s in (-1, 1):
        x0 = ox + (0.1 if s > 0 else -3.3)
        m.cube("grimoire_open", (x0 - 0.1, oy - 0.25, oz - 2.2), (3.4, 0.25, 4.4), lambda f, x, y, w, h: tone(LEATHER, 2.4 + 0.6 * h01("oc", x, y)),
               density=4, tag="open_cover", rotation=(0, 0, -8 * s), pivot=(ox, oy, oz))
        m.cube("grimoire_open", (x0, oy, oz - 2.05), (3.1, 0.6, 4.1), page, density=4, tag="open_pages", rotation=(0, 0, -8 * s),
               pivot=(ox, oy, oz))
    m.cube("grimoire_open", (ox - 0.3, oy - 0.45, oz - 2.2), (0.6, 0.5, 4.4), solid(LEATHER[1]), density=4, tag="open_spine")
    m.cube("grimoire_open", (ox + 3.0, oy + 0.1, oz - 0.3), (0.5, 0.8, 0.6), gem(RUBY), glow_k(0.35), density=4, tag="open_clasp")
    # The page that turns (hinged at the spine, lying over the right-hand pages at rest).
    m.cube("grimoire_page", (ox + 0.05, oy + 0.62, oz - 2.0), (3.0, 0.1, 4.0), lambda f, x, y, w, h: page("up", x, y % 16, w, 16) if f in ("up", "down") else PAGE[3],
           density=4, tag="turning_page", rotation=(0, 0, -8), pivot=(ox, oy + 0.6, oz))


# =====================================================================================================
# Clips
# =====================================================================================================

HIDE_BOOK = {"%grimoire_open": [0.01, 0.01, 0.01]}
SHOW_BOOK = {"%grimoire_open": [1, 1, 1]}
MOUTH_OPEN = {"@mouth": [0, 0, -0.32]}
FORBID = ("grimoire", "grimoire_clasp")


def follow(pose):
    def extra(p, out):
        body = p.get("body", [0, 0, 0])[0]
        head = p.get("head", [0, 0, 0])[0]
        if "hair_back" not in p:
            out["hair_back"] = [max(-30.0, min(18.0, -head * 0.85 - body * 0.5)), 0, 0]
        if "bustle_tail" not in p:
            rl = p.get("right_leg", [0, 0, 0])[0]
            ll = p.get("left_leg", [0, 0, 0])[0]
            out["bustle_tail"] = [max(rl, ll) * 0.25, 0, 0]
    return gown_follow(pose, extra=extra)


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(WClip(f, A + name, length, loop, hold, forbid=FORBID, follow=follow))
        return clips[-1]

    def reach(pose, side, target, grip=None):
        s = -1 if side == "right" else 1
        return _reach(r, pose, side, target, grip or (WB.ax * s, WB.palm - 0.6, -0.3))

    def book(pose, tilt=0.7):
        """The open book in her left hand, its pages turned up towards her face, the top edge away from her."""
        base = P_(pose, {"grimoire_open": [0, 0, 0]})
        rot = aim2(r, base, "grimoire_open", (0, 1, 0), (0, tilt, 1 - tilt * 0.5), (0, 0, -1), (0, 1 - tilt * 0.4, -tilt))
        return P_(pose, {"grimoire_open": rot}, SHOW_BOOK)

    # Her stance: weight on the right leg, the left hip out, chin up; the left hand resting on the book at her hip.
    stance = P_(REST, HIDE_BOOK, {"body": [0, -4, -1.5], "head": [-3, 8, 2], "right_leg": [0, 0, 1], "left_leg": [-4, -6, -3],
                                  "left_shin": [8, 0, 0], "right_arm": [-4, 0, 7], "right_forearm": [-24, 0, 0], "right_hand": [0, -20, 0],
                                  "left_arm": [-6, 0, -16], "left_forearm": [-28, 0, 0], "left_hand": [0, 0, 20]}, GRACE_R, CURL_L)

    # --- idle: the weight shifting, a turn of the head, fingers drumming on the grimoire. ---
    c = clip("idle", 4.0, loop=True)
    for t, k, look in ((0, 0, 8), (1.0, 1, 18), (2.0, 0, 4), (3.0, -1, -10), (4.0, 0, 8)):
        c.key(t, P_(stance, {"body": [0, -4 + 2 * k, -1.5 - 0.5 * k], "@root": [0, -0.08 * abs(k), 0], "head": [-3 + 1.5 * abs(k), look, 2 - k]}),
              E_IO if t else None)
    c.layer("left_fingers", lambda t: [-18 * max(0, math.sin(t * 2 * math.pi * 3 / 4.0)) if 2.2 < t < 3.6 else 0, 0, 0])
    c.layer("hair_tail", lambda t: [1.5 * math.sin(t * 2 * math.pi / 4.0), 0, 1.0 * math.sin(t * 2 * math.pi / 4.0 + 1)])
    c.layer("curl_r", lambda t: [2 * math.sin(t * 2 * math.pi / 2.0), 0, 2 * math.sin(t * 2 * math.pi / 4.0)])
    c.layer("curl_l", lambda t: [2 * math.sin(t * 2 * math.pi / 2.0 + 1), 0, -2 * math.sin(t * 2 * math.pi / 4.0)])

    # --- walk: an unhurried glide, the gown swaying, the curls bouncing. ---
    n = 8
    c = clip("walk", 1.2, loop=True)
    for i in range(n + 1):
        w = gown_walk(n, i % n, stride=15, arm=9)
        c.key(1.2 * i / n, P_(REST, HIDE_BOOK, w, {"head": [-2, w["head"][1], 0], "left_arm": [w["left_arm"][0] - 4, 0, -10]}), E_IO if i else None)
    c.layer("hair_tail", lambda t: [3 * math.sin(t * 2 * math.pi * 2 / 1.2), 0, 0])
    c.layer("curl_r", lambda t: [4 * math.sin(t * 2 * math.pi * 2 / 1.2), 0, 0])
    c.layer("curl_l", lambda t: [4 * math.sin(t * 2 * math.pi * 2 / 1.2 + 0.5), 0, 0])

    # --- levitate: hovering, the toes pointed down, arms floating out, the skirt and hair stirring in a wind of power. ---
    L = 3.0
    c = clip("levitate", L, loop=True)
    hover = P_(HIDE_BOOK, GRACE_R, GRACE_L, {"right_leg": [6, 0, 2], "left_leg": [-4, 0, -2], "right_shin": [18, 0, 0], "left_shin": [26, 0, 0],
                                             "right_arm": [-12, 0, 32], "left_arm": [-12, 0, -32], "right_forearm": [-20, 0, 0],
                                             "left_forearm": [-20, 0, 0], "right_hand": [0, 0, -20], "left_hand": [0, 0, 20],
                                             "head": [-8, 0, 0], "body": [-3, 0, 0], "skirt": [4, 0, 0], "skirt_lower": [6, 0, 0],
                                             "hair_back": [22, 0, 0]})
    for i in range(5):
        t = L * i / 4
        ph = 2 * math.pi * i / 4
        c.key(t, P_(hover, {"@root": [0, 2.2 + 0.8 * math.sin(ph), 0], "right_arm": [-12, 0, 32 + 4 * math.sin(ph)],
                            "left_arm": [-12, 0, -32 - 4 * math.sin(ph)], "right_shin": [18 + 4 * math.sin(ph + 1), 0, 0],
                            "left_shin": [26 - 4 * math.sin(ph + 1), 0, 0]}), E_IO if i else None)
    c.layer("skirt_lower", lambda t: [4 * math.sin(t * 2 * math.pi * 2 / L), 0, 3 * math.sin(t * 2 * math.pi / L)])
    c.layer("bustle_tail", lambda t: [10 + 8 * math.sin(t * 2 * math.pi * 2 / L + 1), 0, 4 * math.sin(t * 2 * math.pi / L)])
    c.layer("hair_tail", lambda t: [16 + 8 * math.sin(t * 2 * math.pi * 2 / L + 0.6), 0, 5 * math.sin(t * 2 * math.pi / L + 2)])
    c.layer("curl_r", lambda t: [20 + 6 * math.sin(t * 2 * math.pi * 2 / L), 0, 10])
    c.layer("curl_l", lambda t: [20 + 6 * math.sin(t * 2 * math.pi * 2 / L + 1), 0, -10])
    c.layer("cuff_r", lambda t: [6 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
    c.layer("cuff_l", lambda t: [6 * math.sin(t * 2 * math.pi * 2 / L + 1), 0, 0])

    # --- recite: the grimoire open on her left palm, her right hand drawing the spell over it, lips moving. ---
    L = 2.4
    c = clip("recite", L, loop=True)
    hold = P_(REST, CURL_L, POINT_R, {"body": [8, 0, 0], "head": [22, 0, 0], "left_arm": [-30, 6, -8], "left_forearm": [-50, 0, 0],
                                      "left_hand": [0, 0, -70], "right_leg": [0, 0, 1], "left_leg": [-3, 0, -1]})
    hold = book(hold)
    for i in range(9):
        t = L * i / 8
        ph = 2 * math.pi * i / 8
        tip = (-1.0 + 1.3 * math.cos(ph), 16.4 + 0.7 * math.sin(ph), -8.0 + 0.6 * math.sin(ph))
        pose = reach(P_(hold, {"right_hand": [-20, 0, 0]}), "right", tip, (-WB.ax, WB.palm - 1.4, -1.0))
        c.key(t, P_(pose, MOUTH_OPEN if i % 2 else {}, {"head": [22 + 2 * math.sin(ph * 2), 0, 2 * math.sin(ph)]}), E_IO if i else None)
    c.layer("hair_tail", lambda t: [1.5 * math.sin(t * 2 * math.pi / L), 0, 0])

    # --- fling: a flick of the wrist that throws a man across the room (lands at tick 10). ---
    c = clip("fling", 1.1)
    c.key(0, stance)
    wind = P_(stance, CURL_R, {"body": [0, -18, 0], "head": [0, 14, 0], "right_arm": [-64, -40, 0], "right_forearm": [-80, 0, 0],
                               "right_hand": [30, 0, 0]})
    flick = P_(stance, OPEN_R, {"body": [2, 16, 0], "head": [-4, -8, 0], "right_arm": [-74, 34, 30], "right_forearm": [-10, 0, 0],
                                "right_hand": [-30, 0, -20], "@brows": [0, 0.25, 0]})
    c.key(0.3, wind, E_OUT)
    c.key(0.5, flick, E_SNAP)
    c.key(0.7, P_(flick, {"right_arm": [-66, 30, 28]}), E_OUT)
    c.key(1.1, stance, E_IO)

    # --- ignis: the hand drawn back to the shoulder, then a palm thrust that sends the fire (tick 16). ---
    c = clip("ignis", 1.5)
    c.key(0, stance)
    draw = P_(stance, CLAW_R, {"body": [-4, 24, 0], "head": [0, -16, 0], "right_arm": [-40, 30, 40], "right_forearm": [-110, 0, 0],
                               "right_hand": [40, 0, 0], "left_arm": [-30, 0, -10], "left_forearm": [-60, 0, 0], "left_hand": [0, 0, -40],
                               "right_leg": [6, 0, 0], "left_leg": [-14, 0, 0]})
    thrust = P_(stance, OPEN_R, {"body": [10, -16, 0], "head": [6, 12, 0], "right_arm": [-92, -14, 0], "right_forearm": [-4, 0, 0],
                                 "right_hand": [-70, 0, 0], "left_arm": [10, 0, -20], "left_forearm": [-30, 0, 0], "right_leg": [-14, 0, 0],
                                 "left_leg": [12, 0, 0], "left_shin": [10, 0, 0], "@root": [0, -0.4, -0.6], "hair_back": [14, 0, 0]})
    c.key(0.45, draw, E_OUT)
    c.key(0.65, P_(draw, {"right_arm": [-46, 34, 44]}), E_IO)
    c.key(0.8, thrust, E_SNAP)
    c.key(1.05, P_(thrust, {"right_arm": [-86, -12, 0]}), E_OUT)
    c.key(1.5, stance, E_IO)

    # --- glacies: both hands raised to her chest, then driven down, palms flat, the frost spreading (tick 18). ---
    c = clip("glacies", 1.7)
    c.key(0, stance)
    rise = P_(stance, OPEN_R, OPEN_L, {"body": [-8, 0, 0], "head": [-14, 0, 0], "right_arm": [-70, 20, 26], "left_arm": [-70, -20, -26],
                                       "right_forearm": [-60, 0, 0], "left_forearm": [-60, 0, 0], "right_hand": [-20, 0, 0],
                                       "left_hand": [-20, 0, 0], "@root": [0, 0.5, 0], "left_leg": [0, 0, -2]})
    press = P_(stance, OPEN_R, OPEN_L, {"body": [22, 0, 0], "head": [10, 0, 0], "right_arm": [-34, 10, 18], "left_arm": [-34, -10, -18],
                                        "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0], "right_hand": [-80, 0, 0],
                                        "left_hand": [-80, 0, 0], "@root": [0, -1.4, 0], "right_leg": [-16, 0, 0], "left_leg": [-16, 0, 0],
                                        "right_shin": [30, 0, 0], "left_shin": [30, 0, 0], "left_arm_": [0, 0, 0]})
    press.pop("left_arm_")
    c.key(0.6, rise, E_OUT)
    c.key(0.75, P_(rise, {"head": [-18, 0, 0]}), E_IO)
    c.key(0.9, press, E_SNAP)
    c.key(1.2, P_(press, {"body": [18, 0, 0]}), E_OUT)
    c.key(1.7, stance, E_IO)

    # --- summon_victims: arms flung wide and up, head thrown back, calling them (tick 20). ---
    c = clip("summon_victims", 2.0)
    c.key(0, stance)
    gather = P_(stance, CURL_R, CURL_L, {"body": [10, 0, 0], "head": [16, 0, 0], "right_arm": [-30, -20, -14], "left_arm": [-30, 20, 14],
                                         "right_forearm": [-70, 0, 0], "left_forearm": [-70, 0, 0], "@root": [0, -0.6, 0]})
    spread = P_(stance, CLAW_R, CLAW_L, {"body": [-12, 0, 0], "head": [-28, 0, 0], "right_arm": [-40, 0, 115], "left_arm": [-40, 0, -115],
                                         "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "right_hand": [0, 0, -30],
                                         "left_hand": [0, 0, 30], "@root": [0, 0.6, 0], "hair_back": [20, 0, 0], "@brows": [0, 0.3, 0],
                                         "left_leg": [0, 0, -4], "right_leg": [0, 0, 4]}, MOUTH_OPEN)
    c.key(0.6, gather, E_IO)
    c.key(1.0, spread, E_BACK)
    c.key(1.4, P_(spread, {"right_arm": [-44, 0, 120], "left_arm": [-44, 0, -120]}), E_IO)
    c.key(2.0, stance, E_IO)
    c.layer("right_hand", lambda t: [0, 0, 6 * math.sin(t * 40)] if 1.0 < t < 1.5 else [0, 0, 0])
    c.layer("left_hand", lambda t: [0, 0, -6 * math.sin(t * 40)] if 1.0 < t < 1.5 else [0, 0, 0])

    # --- teleport: a swirl of the skirts as she turns away into yellow smoke (gone at tick 8), and turns back in. ---
    c = clip("teleport", 1.0)
    c.key(0, stance)
    wrap = P_(REST, HIDE_BOOK, CURL_R, CURL_L, {"body": [6, 0, 0], "head": [10, 0, 0], "right_arm": [-60, -34, 0], "left_arm": [-60, 34, 0],
                                               "right_forearm": [-80, 0, 0], "left_forearm": [-80, 0, 0], "@root": [0, -0.8, 0],
                                               "skirt": [0, 0, 0], "skirt_lower": [-4, 0, 0], "hair_back": [20, 0, 0]})
    c.key(0.2, P_(wrap, {"root": [0, -90, 0], "skirt_lower": [-6, 0, 10]}), E_IN)
    c.key(0.4, P_(wrap, {"root": [0, -200, 0], "skirt_lower": [-6, 0, 10], "bustle_tail": [40, 0, 0]}), E_IN)
    c.key(0.6, P_(wrap, {"root": [0, -300, 0], "skirt_lower": [-8, 0, 10]}), E_OUT)
    c.key(1.0, P_(stance, {"root": [0, -360, 0]}), E_IO)

    # --- death: a hand to her chest, she staggers, sinks to her knees, the head bowed; holds (tick 80). ---
    c = clip("death", 6.0, hold=True)
    c.key(0, stance)
    hit = P_(REST, HIDE_BOOK, CLAW_R, OPEN_L, {"body": [-12, 10, 0], "head": [-20, 6, 0], "right_arm": [-60, -30, -16], "right_forearm": [-80, 0, 0],
                                               "right_hand": [-20, 0, 0], "left_arm": [-24, 0, -30], "left_forearm": [-20, 0, 0],
                                               "left_leg": [10, 0, 0], "right_leg": [-6, 0, 0], "@root": [0, 0, 0.6], "@brows": [0, 0.3, 0]},
               MOUTH_OPEN)
    sway = P_(hit, {"body": [6, -8, 4], "head": [12, -6, 6], "left_arm": [-40, 0, -16], "left_forearm": [-50, 0, 0], "right_leg": [-18, 0, 0],
                    "right_shin": [30, 0, 0], "@root": [0, -1.0, 0]})
    kneel = P_(REST, HIDE_BOOK, kneel_pose(head=30, body=18), CURL_R, OPEN_L,
               {"right_arm": [-40, -20, -10], "right_forearm": [-70, 0, 0], "left_arm": [-20, 0, -14], "left_forearm": [-30, 0, 0],
                "left_hand": [-30, 0, 0], "curl_r": [20, 0, 0], "curl_l": [20, 0, 0]})
    c.key(0.25, hit, E_SNAP)
    c.key(1.2, P_(hit, {"body": [-8, 6, -2]}), E_IO)
    c.key(2.2, sway, E_IO)
    c.key(3.2, P_(kneel, {"@root": [0, -2.8, 0], "%skirt": [1.1, 0.76, 1.1], "right_shin": [50, 0, 0], "left_shin": [50, 0, 0], "body": [4, 0, 0],
                         "head": [6, 0, 0], "left_arm": [-60, 0, -10]}), E_IN)
    c.key(4.0, kneel, E_OUT)
    c.key(4.6, P_(kneel, {"body": [22, 0, 0], "head": [36, 0, 0]}), E_IO)
    c.key(6.0, P_(kneel, {"body": [24, 0, 0], "head": [40, 0, 0]}), E_IO)

    # ---------------------------------------------------------------------- NPC: at home -------------------------
    # --- talk: a hand turning in the air as she explains, a tilt of the head, lips moving. ---
    L = 3.0
    c = clip("talk", L, loop=True)
    g0 = P_(stance, GRACE_R, {"right_arm": [-30, 0, 10], "right_forearm": [-70, 0, 0], "right_hand": [0, -40, 0]})
    g1 = P_(stance, OPEN_R, {"right_arm": [-40, 14, 16], "right_forearm": [-84, 0, 0], "right_hand": [-10, -80, 0], "head": [-6, 14, 6],
                             "@brows": [0, 0.3, 0]})
    g2 = P_(stance, CURL_R, {"right_arm": [-22, -6, 10], "right_forearm": [-60, 0, 0], "right_hand": [14, -20, 0], "head": [2, 4, -2]})
    for t, g in ((0, g0), (0.75, g1), (1.5, g2), (2.25, P_(g1, {"head": [-2, 10, 0]})), (3.0, g0)):
        c.key(t, g, E_IO if t else None)
    c.layer("@mouth", lambda t: [0, 0, -0.32 * max(0, math.sin(t * 2 * math.pi * 6 / L))])

    # --- read: the book open in her hand, her eyes on it, a page turned now and again. ---
    L = 4.0
    c = clip("read", L, loop=True)
    rd = P_(REST, CURL_L, OPEN_R, {"body": [6, 0, 0], "head": [24, -4, 0], "left_arm": [-30, 6, -8], "left_forearm": [-50, 0, 0],
                                   "left_hand": [0, 0, -70], "right_arm": [-6, 0, 6], "right_forearm": [-30, 0, 0]})
    rd = book(rd)
    turn = reach(P_(rd, {"right_hand": [-30, 0, 0]}), "right", (1.6, 16.6, -8.4), (-WB.ax, WB.palm - 1.4, -1.0))
    c.key(0, rd)
    c.key(1.8, P_(rd, {"head": [26, 4, 0]}), E_IO)
    c.key(2.3, P_(turn, {"grimoire_page": [0, 0, 0]}), E_IO)
    c.key(2.8, P_(turn, {"grimoire_page": [0, 0, 175], "head": [24, -6, 0]}), E_IO)
    c.key(3.2, P_(rd, {"grimoire_page": [0, 0, 0]}), E_IO)
    c.key(4.0, rd, E_IO)

    # --- give: she holds out a page (or the cure) on her open palm, then takes the hand back. ---
    c = clip("give", 1.4)
    out = P_(stance, OPEN_R, {"body": [6, -6, 0], "head": [8, 6, 0], "right_arm": [-70, -8, 0], "right_forearm": [-20, 0, 0],
                              "right_hand": [0, 80, 0], "@brows": [0, 0.2, 0]})
    c.key(0, stance)
    c.key(0.45, out, E_OUT)
    c.key(0.9, P_(out, {"right_arm": [-66, -8, 0]}), E_IO)
    c.key(1.4, stance, E_IO)

    done(clips)
    return f, clips


# =====================================================================================================
# The grimoire's sprite
# =====================================================================================================

def grimoire_icon():
    """Rowena's Grimoire: a thick book bound in worn oxblood leather, gold corners and a tooled gold sigil, a red jewelled
    clasp across the fore-edge, the page block showing."""
    from common import art
    O = hexc("#1a0c08")
    pal = {"o": O, "d": LEATHER[1], "l": LEATHER[2], "L": LEATHER[3], "h": LEATHER[4], "H": LEATHER[5],
           "g": GOLD[2], "G": GOLD[4], "y": GOLD[5], "p": PAGE[3], "P": PAGE[4], "q": PAGE[2],
           "r": RUBY[2], "R": RUBY[4], "w": RUBY[5], "s": LEATHER[0]}
    return art([
        "................",
        "..ooooooooooo...",
        ".oGhhhhhhhhhGo..",
        ".ohLLLLLLLLLlPo.",
        ".ohLLLLgLLLLlpo.",
        ".ohLLLgGgLLLlPo.",
        ".ohLLgLyLgLLrro.",
        ".ohLgGyyyGgLRwo.",
        ".ohLLgLyLgLLrro.",
        ".ohLLLgGgLLLlPo.",
        ".ohLLLLgLLLLlpo.",
        ".ohLlLLLLLlLlPo.",
        ".oGdddddddddGqo.",
        "..osssssssssoo..",
        "...ooooooooo....",
        "................",
    ], pal)


def calling_card_icon():
    """Rowena's calling card: a cream card edged in emerald and gold, a line of her looping hand in red ink running off
    one edge, a red wax seal with her mark."""
    from common import art
    O = hexc("#1a0c08")
    pal = {"o": O, "c": PAGE[4], "C": PAGE[5], "q": PAGE[3], "e": EMERALD[4], "E": EMERALD[3], "g": GOLD[3], "r": RUBY[3],
           "R": RUBY[4], "d": RUBY[1], "i": hexc("#9a1a22")}
    return art([
        "................",
        "................",
        ".oooooooooooooo.",
        ".oeggggggggggeo.",
        ".ogCCCCCCCCCCgo.",
        ".ogCiiCiCCiiCgo.",
        ".ogCCiiCiiCCiiro",
        ".ogCCCCCCCCCCgo.",
        ".ogCqCCCCCRRrgo.",
        ".ogCCCCCCRrRRdo.",
        ".ogCCCCCCrRRrdo.",
        ".ogqqqqqqqrddqo.",
        ".oeggggggggggEo.",
        ".oooooooooooooo.",
        "................",
        "................",
    ], pal)


CALLING_CARD_FRAMES = 32


def calling_card_frame(frame):
    """One of the card's 32 compass frames (the vanilla `angle` property, like letters_compass): the same card, its line
    of writing gathered into one stroke of red ink that turns like a needle, a sharp bright nib at its head and a little
    faded tail behind; the wax seal tucked into the bottom-right corner. Frame 16 points straight
    ahead (up), 24 right, 0 down, as vanilla's compass frames do."""
    from common import art
    O = hexc("#1a0c08")
    pal = {"o": O, "c": PAGE[4], "C": PAGE[5], "q": PAGE[3], "e": EMERALD[4], "E": EMERALD[3], "g": GOLD[3], "r": RUBY[3],
           "R": RUBY[4], "d": RUBY[1]}
    t = art([
        "................",
        "................",
        ".oooooooooooooo.",
        ".oeggggggggggeo.",
        ".ogCCCCCCCCCCgo.",
        ".ogCCCCCCCCCCgo.",
        ".ogCCCCCCCCCCgo.",
        ".ogCCCCCCCCCCgo.",
        ".ogCqCCCCCCCCgo.",
        ".ogCCCCCCCCRRro.",
        ".ogCCCCCCCCRrdo.",
        ".ogqqqqqqqqrddo.",
        ".oeggggggggggEo.",
        ".oooooooooooooo.",
        "................",
        "................",
    ], pal, seed=21100 + frame)
    seal = {(11, 9), (12, 9), (13, 9), (11, 10), (12, 10), (13, 10), (11, 11), (12, 11), (13, 11)}
    cx, cy = 7.0, 7.0
    theta = (frame - 16) / CALLING_CARD_FRAMES * 2 * math.pi
    sx, sy = math.sin(theta), -math.cos(theta)
    ink, nib, faded = hexc("#7a0c16"), hexc("#e0283a"), hexc("#d9a08c")

    def put(x, y, c):
        p = (math.floor(x + 0.5), math.floor(y + 0.5))
        if 3 <= p[0] <= 12 and 4 <= p[1] <= 11 and p not in seal:
            t.set(p[0], p[1], c)

    # The tail: a short stroke of faded ink behind the pivot.
    for k in range(1, 5):
        put(cx - sx * k * 0.5, cy - sy * k * 0.5, faded)
    # The head: dark ink running out to a bright nib.
    for k in range(1, 8):
        put(cx + sx * k * 0.5, cy + sy * k * 0.5, nib if k >= 5 else ink)
    put(cx, cy, ink)
    return t


def bust():
    """Her bust on the trophies' pedestal: red waves pinned up into a coil at the crown, ringlets at the temples, the rest
    down her back; the emerald velvet bodice with its black lace yoke and little standing collar, the ruby pendant on a
    gold chain, gold drop earrings, and her grimoire lying closed at her feet."""
    def tex16(fn):
        t = Tex(16, 16, 0)
        for y in range(16):
            for x in range(16):
                c = fn(x, y)
                if c is not None:
                    t.set(x, y, c)
        return t

    def skin(x, y):
        return tone(SKIN, 3.6 + 0.3 * math.sin(x * 0.8 + y * 0.5) - 0.5 * y / 15)

    def hair(x, y):
        # Waves: bands that snake across the lock, a brighter crest on each.
        n = fbm(x * 2, y, 21901, 16, 16, 2, 4.0)
        w = math.sin((y + 1.6 * math.sin(x * 0.75 + n * 2)) * 1.3)
        return tone(HAIR, 2.9 + 1.1 * w + (0.6 if w > 0.85 else 0))
    rows = [   # x = 0 is the viewer's left
        "hhhhhhhhhhhhhhhh",
        "hhhhhhhhhhhhhhhh",
        "hhhhhhssssshhhhh",
        "hhhsssssssssshhh",
        "hhsbBBbssbBBbshh",
        "hsKKKKssssKKKKsh",
        "hsKWIEWssWEIWKsh",
        "hssuIIussuIIussh",
        "hssssssnnssssssh",
        "hcsssssnnsssssch",
        "hcssssNnnNssssch",
        "hssssssssssssssh",
        "hssssslMMlsssssh",
        "hjsssssLLsssssjh",
        "hjjssssssssssjjh",
        "hhjjssssssssjjhh",
    ]
    legend = {"s": skin, "b": HAIR[2], "B": HAIR[1], "K": LASH, "W": EYE_WHITE, "I": IRIS, "E": PUPIL,
              "u": mix(SKIN[2], hexc("#5a3a4a"), 0.35), "n": SKIN[4], "N": SKIN[1], "c": mix(SKIN[3], LIPS[4], 0.22),
              "l": LIPS[1], "M": LIPS[3], "L": LIPS[4], "j": lambda x, y: shade(skin(x, y), 0.9), "h": hair}

    def face(x, y):
        v = legend[rows[y][x]]
        return v(x, y) if callable(v) else v

    def velvet_(x, y):
        return tone(EMERALD, 3.1 - y / 12 + 0.5 * math.sin(x * 1.3 + y * 0.4) + 0.3 * fbm(x, y, 21903, 16, 16, 2, 4.0))

    def chest(x, y):
        # (uv rows 2..14 land on the front) The lace yoke in a V from the shoulders, a gold chain down it to the ruby
        # (a cube of its own), emerald velvet below with a dark seam down the middle of the bodice.
        d = abs(x - 7.5)
        if y < 9 - d * 0.9:
            if abs(d - (4.6 - y * 0.55)) < 0.5 and y < 8:
                return GOLD[3 + (x + y) % 2]
            return LACE[4] if (x + y * 2) % 3 == 0 else (LACE[2] if (x * 3 + y) % 4 else LACE[5])
        if abs(y - (9 - d * 0.9)) < 0.9:
            return LACE[3]                               # the lace scallop along the neckline
        if d < 0.6:
            return EMERALD[1]
        return velvet_(x, y)

    def gown(x, y):
        return tone(EMERALD, 3.2 - y / 14 + 0.4 * math.sin(x * 1.1 + y * 0.6))

    def lace_(x, y):
        return LACE[5] if (x + y) % 3 == 0 else (LACE[3] if (x * 2 + y) % 5 else GOLD[3])

    def gold(x, y):
        return tone(GOLD, 3.4 + 0.9 * math.sin(x * 0.9 + y * 0.35) - (0.8 if y in (0, 15) else 0))

    def gem_(x, y):
        return tone(RUBY, 3.2 + 1.5 * (1 - (x + y) / 15) + (1 if (x, y) in ((4, 4), (5, 4), (4, 5)) else 0))

    def book(x, y):
        # Top half: the cover (oxblood leather, gold corners and a gold sigil ring); bottom half: the page edges.
        if y < 8:
            if x in (0, 15) or y in (0, 7):
                return LEATHER[1]
            if (x in (1, 14)) and (y in (1, 6)) or (x in (1, 2, 13, 14) and y in (1, 6)):
                return GOLD[4]
            r = math.hypot(x - 7.5, (y - 3.5) * 1.6)
            if 2.0 < r < 3.0:
                return GOLD[3 + (x + y) % 2]
            if r < 1.0:
                return GREEN_GEM[4]
            return tone(LEATHER, 2.6 + 0.6 * math.sin(x * 1.7 + y))
        return PAGE[3] if (x + y) % 2 else PAGE[2]
    for name, fn in (("face", face), ("hair", hair), ("skin", skin), ("chest", chest), ("gown", gown), ("lace", lace_),
                     ("gold", gold), ("gem", gem_), ("book", book)):
        save(tex16(fn), "block", f"rowena_trophy_{name}")
    full = [0, 0, 16, 16]
    F, HR, S, CH, G, L, AU, GM, BK = "face", "hair", "skin", "chest", "gown", "lace", "gold", "gem", "book"
    cover, edge = [0, 0, 16, 8], [0, 8, 16, 16]
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": G}),
        # The emerald bodice, puffed velvet shoulders, the standing lace collar round her neck.
        BM.cube("chest", (4.7, 3.4, 6.1), (11.3, 7.4, 9.9), {"north": CH, "*": G}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (4.0, 5.9, 6.3), (12.0, 7.6, 9.7), {"*": G}),
        BM.cube("collar", (6.75, 7.3, 6.75), (9.25, 8.15, 9.25), {"*": L}),
        BM.cube("neck", (7.1, 7.3, 7.1), (8.9, 8.6, 8.9), {"*": S}),
        BM.cube("pendant", (7.6, 5.7, 5.95), (8.4, 6.5, 6.15), {"*": GM}),
        # The head, the hair over it, the coil of curls pinned at the crown, ringlets at the temples, the waves down her back.
        BM.cube("head", (5.7, 8.4, 5.7), (10.3, 13.2, 10.3), {"north": F, "*": HR}, uv={"north": full}),
        BM.cube("hair_cap", (5.45, 11.1, 5.8), (10.55, 13.5, 10.6), {"north": None, "down": None, "*": HR}),
        BM.cube("coil", (6.5, 13.1, 7.2), (9.5, 14.5, 10.1), {"*": HR}),
        BM.cube("coil_top", (7.0, 14.4, 7.7), (9.0, 14.9, 9.6), {"*": HR}),
        BM.cube("hair_fall", (5.2, 5.6, 9.5), (10.8, 11.6, 10.9), {"north": None, "*": HR}),
        BM.cube("wave_r", (5.15, 6.6, 7.4), (5.95, 11.2, 9.7), {"*": HR}),
        BM.cube("wave_l", (10.05, 6.6, 7.4), (10.85, 11.2, 9.7), {"*": HR}),
        BM.cube("ringlet_r", (5.2, 8.7, 5.95), (5.85, 11.6, 6.75), {"*": HR}),
        BM.cube("ringlet_l", (10.15, 8.7, 5.95), (10.8, 11.6, 6.75), {"*": HR}),
        BM.cube("earring_r", (5.35, 8.4, 6.95), (5.7, 9.4, 7.3), {"*": AU}),
        BM.cube("earring_l", (10.3, 8.4, 6.95), (10.65, 9.4, 7.3), {"*": AU}),
        BM.cube("nose", (7.7, 9.9, 5.45), (8.3, 10.8, 5.7), {"*": S}),
        # Her grimoire, closed, lying on the plinth at her feet.
        BM.cube("grimoire", (8.7, 3.0, 4.35), (11.3, 3.75, 6.05), {"up": BK, "down": BK, "*": BK},
                uv={"up": cover, "down": cover, "north": edge, "south": edge, "east": edge, "west": edge},
                rot=("y", -22.5, (10.0, 3.4, 5.2))),
        BM.cube("clasp", (11.25, 3.15, 4.95), (11.5, 3.6, 5.45), {"*": AU}, rot=("y", -22.5, (10.0, 3.4, 5.2))),
    ]
    mdl = BM.model({"base": "trophy_base", F: "rowena_trophy_face", HR: "rowena_trophy_hair", S: "rowena_trophy_skin",
                    CH: "rowena_trophy_chest", G: "rowena_trophy_gown", L: "rowena_trophy_lace", AU: "rowena_trophy_gold",
                    GM: "rowena_trophy_gem", BK: "rowena_trophy_book"}, e, "trophy_base")
    BM.write("rowena_trophy", mdl)


def generate():
    m = rig()
    t, g = m.build(gutter=1, seed=21000)
    m.rig.write(GEO + NAME + ".geo.json")
    save(t, "entity", NAME)
    save(g, "entity", NAME + "_glowmask")
    f, _ = anims()
    write_compact(f, ANIM + NAME + ".animation.json")
    save(grimoire_icon(), "item", "rowenas_grimoire")
    save(calling_card_icon(), "item", "rowenas_calling_card")
    for f in range(CALLING_CARD_FRAMES):
        save(calling_card_frame(f), "item", f"rowenas_calling_card_{f:02d}")
    bust()


if __name__ == "__main__":
    generate()
