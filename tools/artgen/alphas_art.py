"""The three Alphas (v0.39): the first vampire, the first werewolf and the first shapeshifter, Eve's eldest -- GeckoLib rigs,
textures + glowmasks and clips. Names (ids, clips, bones, timings) are the contract in `entity/alpha/AlphaAssets.java`;
`AlphaAssetsTest` checks them. Each keeps the clips its kind's code already triggers and adds its own.

  alpha_vampire       tall, gaunt and old (33 px to the crown): grey-pale skin over sunken cheeks, black hair slicked back
                      from a widow's peak with silver at the temples, heavy brows over red eyes (glowmask). An old black frock
                      coat to the knees (fronts `coat_front_r/l`, the split tails `coat_tail`), its collar turned up high
                      round the neck; an oxblood brocade waistcoat with a gold watch chain, an ivory stock and cravat with a
                      garnet pin, lace at the wrists, striped trousers and riding boots. Long, long fingers with dark nails.
                      `fangs` (toggled): the open maw -- a dark red mouth stretched wide -- and on `fang_row` a second row
                      of needle teeth round two long canines, sliding down over the first.
  alpha_werewolf      a hulking grey-silver wolf-man (43 px to the ear tips, much heavier than the legacy werewolf): a silver
                      mane like a mantle over the shoulders (`chest`, `mane`), a dark saddle down the spine, old pale scars
                      across the muzzle and chest, long black claws, digitigrade legs, a heavy tail; a wolf's head with a
                      `jaw` that opens on a red mouth and yellowed fangs; amber-gold eyes (glowmask). No human form.
  alpha_shapeshifter  gaunt and naked but for rags (32 px): his own skin, pale and grey-pink, is coming off him in sheets --
                      flaps hang from his face, collarbones, shoulders, back and forearms (`skin_flaps` and its children,
                      `flap_face`, `flap_arm_*`), the raw wet flesh and yellow slime under them; a few long lank hairs, a
                      lipless mouth whose `jaw` drops on needle teeth; a silver flare in his lidless eyes (glowmask).

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north): as the men of legacy_art (root -- body -- head; arms
with forearm, hand, fingers, thumb; legs with shins), plus each creature's own bones.
"""

import math

import horsemen_art as H
import legacy_art as LG
from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, fabric, tone, write_compact
from pixelkit import Ramp, fbm, hexc, mix, shade

CLEAR = (0, 0, 0, 0)
SIDES = (("right", -1), ("left", 1))
STEEL = LG.STEEL


def build_model(name, rig_fn, sizes=((256, 256), (512, 256), (512, 512))):
    """Builds the rig at the first atlas it packs into."""
    last = None
    for w, h in sizes:
        m = Model(name, w, h)
        rig_fn(m)
        try:
            t, g = m.build(gutter=1, seed=sum(map(ord, name)))
            return m, t, g
        except ValueError as e:
            last = e
    raise last


def jitter(seed, i, amp):
    return [amp * (2 * h01(seed, i, k) - 1) for k in range(3)]


# =====================================================================================================
# The Alpha vampire
# =====================================================================================================

VB = LG.Build(hip=13.4, neck=25.0, tw=3.4, depth=3.6, aw=3.0, ax=5.3, sx=4.9, sy=24.0, elbow=18.8, wrist=14.0, palm=11.8,
              lx=1.8, lw=3.3, knee=6.8, shoe_h=1.4)
V_SKIN = Ramp("#3a3640", "#5b5662", "#7c7783", "#9a96a0", "#b5b1ba", "#ceccd2")
V_HAIR = Ramp("#060608", "#0e0e12", "#17171d", "#222229", "#33333c", "#4c4c57")
V_SILVER = Ramp("#4a4a52", "#64646d", "#7f7f88", "#9a9aa2", "#b6b6bd", "#d4d4d9")
V_RED, V_HOT, V_DEEP = hexc("#c4141c"), hexc("#ff4a3a"), hexc("#4a0608")
COAT = Ramp("#050407", "#0b0a0e", "#131117", "#1c1922", "#28232f", "#383142")
WAIST = Ramp("#17040a", "#27070f", "#3a0c16", "#4e121e", "#651a28", "#7e2533")
IVORY = Ramp("#5f594e", "#7e7768", "#9c9584", "#b6af9c", "#cdc6b2", "#e2dcc9")
TROUSER = Ramp("#0c0c0e", "#141417", "#1d1d21", "#27272c", "#323238", "#414148")
BOOT = Ramp("#030303", "#09090a", "#121213", "#1c1c1e", "#2b2b2e", "#55565c")
GOLD = Ramp("#3e2a08", "#694812", "#99701e", "#c49a32", "#e2c15a", "#f8e6a0")
GARNET = hexc("#8a0c1a")
MOUTH = LG.MOUTH
FANG = LG.FANG
NAIL = hexc("#24191f")

ALPHA_VAMPIRE_FACE = [   # x = 0 is the viewer's left (his right); rows from the crown down
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssshhsssshhh",
    "hhsssssssssssshh",
    "hssssssssssssssh",
    "sBBBBssssssBBBBs",
    "skQRksssssskRQks",
    "skkkksssssskkkks",
    "sssssssnnsssssss",
    "KKssssnNNnssssKK",
    "KKKssssssssssKKK",
    "jKKssmmmmmmssKKj",
    "jjKsssllllsssKjj",
    "jjjssssssssssjjj",
    "jjjjssssssssjjjj",
]
assert all(len(r) == 16 for r in ALPHA_VAMPIRE_FACE)


def v_hair_m(seed):
    """Slicked straight back: long combed strands catching a cold sheen, silver threads gathered at the temples."""
    def m(f, x, y, w, h):
        n = fbm(x * 0.7 + len(f) * 3, y * 2.4, seed, 64, 64, 2, 6.0)
        v = 1.6 + (n - 0.5) * 1.1 + (0.9 if (x * 2 + y // 3) % 7 == 0 else 0)
        c = tone(V_HAIR, v)
        if f in ("east", "west"):
            fx = x if f == "west" else w - 1 - x          # 0 at the front
            if fx < w * 0.45 and y % 3 != 2 and h01("gap", seed, f, x // 2, y) < 0.8:
                # The silver swept back from the temples in combed streaks.
                c = mix(c, V_SILVER[3 if (y + fx) % 2 else 4], 0.55 - 0.08 * fx / max(1, w * 0.45))
        elif (x * 3 + y // 4) % 13 == 0:
            c = mix(c, V_SILVER[2], 0.3)
        return c
    return m


def brocade_m(seed):
    """Oxblood brocade: a small damask lozenge woven in a darker thread, a silk sheen down the light side."""
    def m(f, x, y, w, h):
        v = 2.7 - 0.5 * y / max(1, h)
        d = (abs((x % 4) - 1.5) + abs((y % 4) - 1.5))
        if d < 1.2:
            v -= 0.9
        elif d > 2.6:
            v += 0.4
        if f == "up":
            v += 0.5
        if f in ("east", "west"):
            v -= 0.3
        return tone(WAIST, v)
    return m


def build_vampire(m):
    b = VB
    m.bone("root", (0, 0, 0))
    LG.skeleton(m, b)
    m.bone("left_index", (b.ax, b.palm, -1.0), "left_hand")
    m.bone("hair_back", (0, b.neck + 6.0, 3.6), "head")
    m.bone("brows", (0, b.neck + 4.8, -4.0), "head")
    m.bone("fangs", (0, b.neck + 1.9, -4.0), "head")
    m.bone("fang_row", (0, b.neck + 2.6, -4.1), "fangs")
    m.bone("collar", (0, b.neck - 0.4, 0.6), "body")
    m.bone("cravat", (0, b.neck - 1.2, -b.depth / 2 - 0.3), "body")
    m.bone("coat", (0, b.neck, 0), "body")
    m.bone("coat_front_r", (-1.9, b.hip + 0.4, -b.depth / 2 - 0.4), "coat")
    m.bone("coat_front_l", (1.9, b.hip + 0.4, -b.depth / 2 - 0.4), "coat")
    m.bone("coat_tail", (0, b.hip + 0.6, b.depth / 2 + 0.4), "coat")
    m.bone("coat_tail_r", (-1.4, b.hip - 3.0, b.depth / 2 + 0.6), "coat_tail")
    m.bone("coat_tail_l", (1.4, b.hip - 3.0, b.depth / 2 + 0.6), "coat_tail")

    sk = H.skin_mat(V_SKIN, 39101, 3.0, 0.35)
    hr = v_hair_m(39102)

    def eye_glow(f, x, y, w, h, c):
        if c in (V_RED, V_HOT):
            return hexc("#ff5a48") if c == V_HOT else hexc("#a8141a")
        return None
    legend = {"h": hr, "s": sk, "B": V_HAIR[1], "Q": V_HOT, "R": V_RED,
              "k": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), hexc("#251c2a"), 0.5),
              "K": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), V_SKIN[0], 0.42),
              "n": V_SKIN[4], "N": V_SKIN[0], "m": mix(WAIST[1], V_SKIN[1], 0.35), "l": mix(WAIST[2], V_SKIN[2], 0.45),
              "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.86)}
    sides = H.head_sides(sk, hr, top=5, back=15, temple=3, sideburn=11)
    LG.head_cube(m, "head", b.neck, ALPHA_VAMPIRE_FACE, legend, sides, glow=eye_glow)
    # A long, hooked nose and the sharp bridge of it; pointed ears swept back.
    m.cube("head", (-0.6, b.neck + 2.5, -4.65), (1.2, 2.2, 0.7),
           lambda f, x, y, w, h: V_SKIN[4] if f == "north" and y < 2 else (V_SKIN[0] if f == "down" else V_SKIN[2]), density=4, tag="nose")
    for s in (-1, 1):
        def ear(f, x, y, w, h, s=s):
            if f in ("north", "south") and y < 2 and (x == 0 if (f == "north") == (s > 0) else x == w - 1):
                return CLEAR
            return shade(sk(f, x, y, w, h), 0.88 if f in ("east", "west") else 0.8)
        m.cube("head", (s * 4.0 - 0.25, b.neck + 3.0, -0.4), (0.5, 3.2, 1.8), ear, density=4, tag="ear",
               rotation=(-18, 0, 10 * s), pivot=(s * 4.0, b.neck + 3.6, 0.2))
    # Brows: heavy, pulled down at the middle.
    for s in (-1, 1):
        m.cube("brows", (s * 2.1 - 1.2, b.neck + 4.7, -4.14), (2.4, 0.5, 0.3), solid(V_HAIR[1]), density=4, tag="brow",
               rotation=(0, 0, -12 * s), pivot=(s * 1.0, b.neck + 4.9, -4.1))
    # Hair: a slicked cap swept back from the peak, the ridge of it combed high, the back falling to the collar.
    def cap(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            mid = (w - 1) / 2
            return hr(f, x, y, w, h) if y < 2 or (y < 3 and abs(x - mid) < 1.5) else None
        return hr(f, x, y, w, h)
    m.cube("head", (-4, b.neck + 6.0, -4), (8, 2.0, 8), cap, inflate=0.3, density=2, tag="hair_cap")
    m.cube("head", (-3.0, b.neck + 7.9, -3.4), (6.0, 0.8, 7.0), none_on(("down",), lambda f, x, y, w, h: shade(hr(f, x, y, w, h), 1.12)),
           density=2, tag="hair_ridge")
    m.cube("hair_back", (-4.1, b.neck + 1.0, 3.5), (8.2, 6.0, 1.0),
           lambda f, x, y, w, h: None if f in ("north", "up") else hr(f, x, y + 4, w, h), inflate=0.2, density=2, tag="hair_back")
    m.cube("body", (-1.3, b.neck - 0.8, -1.3), (2.6, 1.2, 2.6), sk, density=2, tag="neck")
    # The maw (toggled): the mouth stretched wide and red, the second row of needles round two long canines.
    def maw(f, x, y, w, h):
        if y == 0:
            return mix(WAIST[3], V_SKIN[2], 0.4)
        if y == h - 1:
            return mix(WAIST[3], V_SKIN[2], 0.5)
        cx = (w - 1) / 2
        d = abs(x - cx) / (w / 2)
        if d > 0.92:
            return MOUTH[3]
        return MOUTH[0] if d < 0.55 and 1 < y < h - 2 else MOUTH[1 + (y + x) % 2]
    m.cube("fangs", (-2.25, b.neck + 0.6, -4.08), (4.5, 2.0, 0), maw, lambda f, x, y, w, h, c: shade(c, 0.55), faces=("north",),
           density=4, tag="maw")
    for x in (-1.75, 1.5):
        m.cube("fangs", (x, b.neck + 0.6, -4.18), (0.25, 0.75, 0.25), lambda f, x_, y, w, h: FANG[4 - y], density=4, tag="lower_fang")
    for i, x in enumerate((-2.0, -1.5, -1.0, -0.5, 0.25, 0.75, 1.25, 1.75)):
        tall = 1.25 if i in (1, 6) else (0.5 if i in (0, 7) else 0.75)
        m.cube("fang_row", (x, b.neck + 2.6 - tall, -4.22), (0.25, tall, 0.25),
               lambda f, x_, y, w, h: FANG[5] if y < h - 1 else FANG[2], density=4, tag="fang")

    # --- the clothes ---------------------------------------------------------------------------------------------
    shirt = fabric(IVORY, 39110, 3.2, 0.2)
    waist = brocade_m(39111)
    coat = H.cloth(COAT, 39112, 2.9, 0.35, 0.05) if hasattr(H, "cloth") else fabric(COAT, 39112, 2.9, 0.3)

    def torso(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "north":
            if y < 5 and abs(x - mid) < 2.6 - y * 0.25:
                return shirt(f, x, y, w, h)
            # The waistcoat: two rows of gilt buttons, the watch chain looped across, a pointed hem.
            if abs(x - mid - 0.5) < 0.6 and y in (7, 10, 13, 16):
                return GOLD[4]
            if 12 <= y <= 13 and 1 <= x <= w - 2:
                k = abs(x - mid) / mid
                if abs((y - 12) - (1 - k) * 1.4) < 0.6:
                    return GOLD[3 + (x % 2)]
            if y >= h - 2 and abs(x - mid) > 0.6 + (h - 1 - y) * 1.5:
                return TROUSER[2]
            return waist(f, x, y, w, h)
        return COAT[1] if f in ("east", "west", "south") else waist(f, x, y, w, h)
    m.cube("body", (-b.tw, b.hip - 0.2, -b.depth / 2), (2 * b.tw, b.neck - b.hip + 0.2, b.depth), torso, density=2, tag="torso")
    # The stock and cravat: ivory silk folded high at the throat, a garnet pin in its knot.
    def stock(f, x, y, w, h):
        if f == "down":
            return None
        return tone(IVORY, 3.6 - 0.6 * y / max(1, h) + (0.5 if (x + y) % 3 == 0 else 0))
    m.cube("body", (-1.7, b.neck - 1.4, -1.7), (3.4, 1.6, 3.4), stock, inflate=0.15, density=4, tag="stock")
    def cravat(f, x, y, w, h):
        v = 3.4 + (0.6 if x == 1 else 0) - 0.8 * y / max(1, h) + (0.4 if (x * 2 + y) % 5 == 0 else 0)
        return tone(IVORY, v)
    m.cube("cravat", (-1.1, b.neck - 3.6, -0.15), (2.2, 2.6, 0.5), cravat, density=4, tag="cravat",
           rotation=(-6, 0, 0), pivot=(0, b.neck - 1.2, 0))
    m.cube("cravat", (-0.25, b.neck - 2.0, -0.32), (0.5, 0.5, 0.2), solid(GARNET), lambda f, x, y, w, h, c: shade(c, 0.5), density=4,
           tag="pin")
    # The frock coat over the shoulders and back, open in front over the waistcoat, its fronts' edges catching the light.
    def coat_shell(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            if x < 3 or x > w - 4:
                return COAT[4] if x in (2, w - 3) else coat(f, x, y, w, h)
            return None
        if f == "up":
            mid = (w - 1) / 2
            return None if abs(x - mid) < 2.4 and y < h * 0.75 else coat(f, x, y, w, h)
        return coat(f, x, y, w, h)
    m.cube("coat", (-b.tw, b.hip + 0.4, -b.depth / 2), (2 * b.tw, b.neck - b.hip - 0.6, b.depth), coat_shell, inflate=0.55, density=2,
           tag="coat_shell")
    for s in (-1, 1):
        def lapel(f, x, y, w, h, s=s):
            if f == "south":
                return COAT[0]
            c = tone(COAT, 4.0 - 1.8 * y / max(1, h))
            if f == "north" and (x == 0 if s < 0 else x == w - 1):
                return COAT[5]
            return c
        m.cube("coat", (3.0 * s - 1.0, b.neck - 8.0, -b.depth / 2 - 0.85), (2.0, 7.2, 0.4), lapel, density=2, tag="lapel",
               rotation=(3, 0, 9 * s), pivot=(2.6 * s, b.neck - 8.0, -b.depth / 2 - 0.7))
        m.cube("coat", (s * (b.tw + 0.2) - 1.3, b.neck - 0.9, -b.depth / 2 - 0.4), (2.6, 0.9, b.depth + 0.8),
               lambda f, x, y, w, h: tone(COAT, 4.0 if f == "up" else 2.8), density=2, tag="shoulder")
    # The collar, turned up high round the back and the sides of his neck, its lining a dull red.
    def collar(f, x, y, w, h):
        if f == "north":
            return tone(WAIST, 2.2 - 0.6 * y / max(1, h))
        if f == "down":
            return COAT[0]
        return tone(COAT, 4.2 - 1.4 * y / max(1, h))
    m.cube("collar", (-3.6, b.neck - 0.8, 0.4), (7.2, 4.2, 0.55), collar, density=2, tag="collar_back", rotation=(-10, 0, 0),
           pivot=(0, b.neck - 0.6, 0.6))
    for s in (-1, 1):
        m.cube("collar", (s * 3.5 - 0.27, b.neck - 0.8, -2.4), (0.55, 3.6, 3.0), collar, density=2, tag="collar_side",
               rotation=(0, 10 * s, 10 * s), pivot=(s * 3.4, b.neck - 0.6, 0))
    # The skirts: the two fronts and the split tails, from the waist to below the knee.
    hem = 4.2
    L = b.hip + 0.4 - hem
    for bone, s in (("coat_front_r", -1), ("coat_front_l", 1)):
        def panel(f, x, y, w, h, s=s):
            if f == "south":
                return COAT[1]
            c = coat(f, x, y + 20, w, h + 20)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return COAT[4]
            if f == "north" and y == 3 and 1 <= (w - 1 - x if s < 0 else x) <= w - 2:
                return COAT[0]                      # the pocket flap's seam
            return c
        xa, xb = (-b.tw - 0.6, -0.2) if s < 0 else (0.2, b.tw + 0.6)
        m.cube(bone, (xa, hem, -b.depth / 2 - 0.7), (xb - xa, L, 0.45), panel, density=2, tag="coat_front",
               rotation=(-5, 0, 4 * s), pivot=(1.9 * s, b.hip + 0.4, -b.depth / 2 - 0.4))
        xs = -b.tw - 0.65 if s < 0 else b.tw + 0.2
        m.cube(bone, (xs, hem, -b.depth / 2 - 0.45), (0.45, L, b.depth / 2 + 0.45), none_on(("up",), coat), density=2, tag="coat_side",
               rotation=(0, 0, 4 * s), pivot=(xs, b.hip, 0))
    m.cube("coat_tail", (-b.tw - 0.6, b.hip - 3.0, b.depth / 2 + 0.3), (2 * b.tw + 1.2, 3.6, 0.45),
           lambda f, x, y, w, h: COAT[0] if f == "north" else coat(f, x, y + 16, w, h + 16), density=2, tag="coat_back")
    for bone, s in (("coat_tail_r", -1), ("coat_tail_l", 1)):
        def tail(f, x, y, w, h, s=s):
            if f == "north":
                return COAT[0]
            if f == "south" and (x == (w - 1 if s < 0 else 0)) and y > 1:
                return COAT[0]                      # the vent
            return coat(f, x, y + 24, w, h + 24)
        xa = -b.tw - 0.6 if s < 0 else 0.05
        m.cube(bone, (xa, hem - 0.6, b.depth / 2 + 0.3), (b.tw + 0.55, b.hip - 3.0 - hem + 0.6, 0.45), tail, density=2, tag="coat_tail",
               rotation=(5, 0, -3 * s), pivot=(1.4 * s, b.hip - 3.0, b.depth / 2 + 0.5))
    # Sleeves: the coat's, deep turned-back cuffs, the shirt's lace spilling out over the long hands.
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow, b.aw), coat, inflate=0.3, density=2,
               tag="sleeve")
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist + 0.4, -1.5), (3.0, b.elbow - b.wrist - 0.4, 3.0), none_on(("up",), coat),
               inflate=0.3, density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist + 0.3, -1.5), (3.0, 1.6, 3.0),
               none_on(("up", "down"), lambda f, x, y, w, h: tone(COAT, 4.0 - y * 0.5) if y else COAT[5]), inflate=0.5, density=2, tag="cuff")
        m.cube(f"{side}_forearm", (cx - 1.3, b.wrist - 0.4, -1.3), (2.6, 0.9, 2.6),
               none_on(("up", "down"), lambda f, x, y, w, h: CLEAR if (y == h - 1 and x % 2) else tone(IVORY, 4.2 - y)), inflate=0.2,
               density=4, tag="lace")
    hand = H.skin_mat(V_SKIN, 39120, 2.9, 0.4)
    _long_hands(m, b, hand, NAIL)
    # A signet ring on the right hand.
    m.cube("right_hand", (-b.ax - 1.35, b.palm + 0.2, -1.55), (0.4, 0.6, 0.6), solid(GOLD[3]), lambda f, x, y, w, h, c: None, density=4,
           tag="signet")
    # Striped trousers and tall riding boots, polished.
    stripe = fabric(TROUSER, 39130, 2.9, 0.2, crease=True)
    trous = lambda f, x, y, w, h: shade(stripe(f, x, y, w, h), 1.25) if f in ("north", "south", "east", "west") and x % 3 == 1 else stripe(f, x, y, w, h)
    for side, s in SIDES:
        x0 = b.lx * s - b.lw / 2
        m.cube(f"{side}_leg", (x0, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), trous, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0, b.shoe_h, -b.lw / 2), (b.lw, b.knee - b.shoe_h, b.lw), trous, density=2, tag="shin")

        def boot(f, x, y, w, h):
            if f == "down":
                return BOOT[0]
            v = 2.0 + 0.8 * (1 - y / max(1, h))
            if f == "north" and abs(x - (w - 1) / 2) < 0.8:
                v += 1.6                              # the shine down the front
            if f == "up":
                v = 2.4
            return tone(BOOT, v)
        m.cube(f"{side}_shin", (x0 - 0.15, 0.0, -b.lw / 2 - 0.15), (b.lw + 0.3, b.knee + 0.4, b.lw + 0.3), none_on(("up",), boot),
               density=2, tag="boot_shaft")
        m.cube(f"{side}_shin", (x0 - 0.15, 0.0, -b.lw / 2 - 1.6), (b.lw + 0.3, 1.2, 1.5), boot, density=2, tag="boot_toe")
        m.cube(f"{side}_shin", (x0 - 0.3, b.knee + 0.1, -b.lw / 2 - 0.3), (b.lw + 0.6, 0.6, b.lw + 0.6),
               none_on(("down",), lambda f, x, y, w, h: tone(BOOT, 3.0)), density=2, tag="boot_cuff")


def _long_hands(m, b, skin, nail):
    """Narrow palms and long jointed fingers ending in dark nails; thumbs; the index fingers on their own bones."""
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_hand", (cx - 1.1, b.palm, -1.3), (2.2, b.wrist - b.palm, 2.6), skin, density=2, tag="palm")

        def fing(f, x, y, w, h):
            if f == "down" or (f != "up" and y >= h - 1):
                return nail
            c = skin(f, x, y, w, h)
            if f != "up" and y == h // 2:
                c = shade(c, 0.82)                    # the knuckle crease
            return shade(c, 0.9) if f in ("east", "west") and x % 2 else c
        m.cube(f"{side}_fingers", (cx - 1.05, b.palm - 2.6, -0.5), (2.1, 2.6, 1.7), fing, density=4, tag="fingers")
        m.cube(f"{side}_index", (cx - 1.0, b.palm - 2.8, -1.25), (2.0, 2.8, 0.7), fing, density=4, tag="index")
        m.cube(f"{side}_thumb", (cx - 1.0 * s - 0.45, b.palm - 0.6, -1.9), (0.9, 2.0, 0.8), fing, density=4, tag="thumb")


def coat_follow(pose, k=1.0):
    """The frock coat's skirts follow the legs and the body; the tails trail a little behind a lean."""
    out = dict(pose)
    body = pose.get("body", [0, 0, 0])[0]
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    out.setdefault("coat_front_r", [min(0.0, rl) * 0.6 - body * 0.5, 0, 0])
    out.setdefault("coat_front_l", [min(0.0, ll) * 0.6 - body * 0.5, 0, 0])
    out.setdefault("coat_tail", [max(0.0, max(rl, ll)) * 0.5 + max(0.0, body) * 0.3 * k, 0, 0])
    return out


V_FIST_R = {"right_fingers": [-90, 0, 0], "right_index": [-90, 0, 0], "right_thumb": [-20, 0, 20]}
V_CLAW_R = {"right_fingers": [-38, 0, 0], "right_index": [-30, 0, 0], "right_thumb": [0, 0, -22]}
V_CLAW_L = {"left_fingers": [-38, 0, 0], "left_index": [-30, 0, 0], "left_thumb": [0, 0, 22]}
V_OPEN = {"right_fingers": [-6, 0, 0], "right_index": [-2, 0, 0], "left_fingers": [-6, 0, 0], "left_index": [-2, 0, 0]}


def vampire_anims(r):
    f = AnimFile()
    A = "animation.alpha_vampire."
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold, forbid=("fangs",)))
        return clips[-1]

    def K(c, t, pose, ease=None):
        c.key(t, coat_follow(pose), ease)

    # At rest: upright and still, chin lifted, the long hands loose and half-curled at his sides.
    stand = P_({"right_fingers": [-22, 0, 0], "right_index": [-14, 0, 0], "left_fingers": [-22, 0, 0], "left_index": [-14, 0, 0],
                "right_arm": [0, 0, 4], "left_arm": [0, 0, -4], "right_forearm": [-8, 0, 0], "left_forearm": [-8, 0, 0],
                "head": [-6, 0, 0], "body": [-1, 0, 0]})
    # Hunting: shoulders forward, head low, the hands lifted and clawed.
    hunt = P_(V_CLAW_R, V_CLAW_L, {"body": [12, 0, 0], "head": [-16, 0, 0], "right_arm": [-22, 0, 14], "left_arm": [-22, 0, -14],
                                   "right_forearm": [-34, 0, 0], "left_forearm": [-34, 0, 0], "@root": [0, -0.4, 0]})

    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        K(c, t, P_(stand, {"head": [-6 - 3 * abs(k), 14 * k, -3 * k], "body": [-1, 3 * k, 0], "brows": [0, 0, 0]}), E_IO if t else None)
    # The fingers drum, one after another, at his side.
    c.layer("right_fingers", lambda t: [-14 * max(0.0, math.sin(t * 2 * math.pi * 1.5)) if 2.4 < t < 3.6 else 0, 0, 0])
    c.layer("left_index", lambda t: [-18 * max(0.0, math.sin(t * 2 * math.pi * 2)) if 0.5 < t < 1.4 else 0, 0, 0])
    c.layer("coat_tail", lambda t: [1.2 * math.sin(t * 2 * math.pi / 4.0), 0, 0])

    L = 1.2
    c = clip("walk", L, loop=True)
    for i in range(9):
        w = LG.walk_cycle(8, i, stride=24, arm=8, knee=26, lean=-1, sway=3)
        K(c, L * i / 8, P_(stand, w, {"head": [-7, w["head"][1], 0]}), E_IO if i else None)
    c.layer("coat_tail", lambda t: [6 + 3 * math.sin(4 * math.pi * t / L), 0, 0])
    c.layer("coat_tail_r", lambda t: [2 * math.sin(2 * math.pi * t / L), 0, 0])
    c.layer("coat_tail_l", lambda t: [-2 * math.sin(2 * math.pi * t / L), 0, 0])

    # run: low and fast, the arms swept back like wings, the coat streaming behind him.
    L = 0.5
    c = clip("run", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        K(c, L * i / 8, P_(V_CLAW_R, V_CLAW_L, {"right_leg": [-62 * sn, 0, 0], "left_leg": [62 * sn, 0, 0],
                                                 "right_shin": [86 * max(0.0, math.sin(ph + 1.1)), 0, 0],
                                                 "left_shin": [86 * max(0.0, -math.sin(ph + 1.1)), 0, 0],
                                                 "right_arm": [34 + 10 * sn, -10, 36], "left_arm": [34 - 10 * sn, 10, -36],
                                                 "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0],
                                                 "body": [34, 4 * sn, 0], "head": [-34, -4 * sn, 0],
                                                 "@root": [0, 1.2 * abs(cs) - 1.0, 0], "coat_tail": [58 + 8 * sn, 0, 0],
                                                 "coat_tail_r": [10 * sn, 0, -6], "coat_tail_l": [-10 * sn, 0, 6]}),
          E_IO if i else None)

    # bite (0.8): both hands seize a shoulder before him, the head darts down (0.35), holds, pulls back.
    c = clip("bite", 0.8)
    grab = LG.reach(r, hunt, "right", (-3.5, 23.5, -9.5), (-VB.ax, VB.palm - 1.2, -0.5))
    grab = LG.reach(r, grab, "left", (3.5, 22.5, -9.5), (VB.ax, VB.palm - 1.2, -0.5))
    lunge = P_(grab, {"body": [24, 0, 0], "@root": [0, -0.6, -2.2], "right_leg": [-28, 0, 0], "right_shin": [26, 0, 0],
                      "left_leg": [18, 0, 0], "head": [-8, 0, 0]})
    bite = P_(lunge, {"head": [30, -22, 16], "body": [32, -6, 0], "@fang_row": [0, -0.4, 0]})
    K(c, 0, hunt)
    K(c, 0.22, lunge, E_OUT)
    K(c, 0.35, bite, E_SNAP)
    K(c, 0.55, P_(bite, {"head": [34, -24, 18]}), E_IO)
    K(c, 0.8, hunt, E_IO)

    # hiss (1.0): recoils, shoulders up, both arms flung wide with the claws out, the head thrust forward.
    c = clip("hiss", 1.0)
    hiss = P_(V_CLAW_R, V_CLAW_L, {"body": [14, 0, 0], "head": [-30, 0, 0], "@root": [0, -1.4, 1.0], "right_arm": [-34, 34, 62],
                                   "left_arm": [-34, -34, -62], "right_forearm": [-34, 0, 0], "left_forearm": [-34, 0, 0],
                                   "right_leg": [-20, 0, 8], "left_leg": [12, 0, -8], "right_shin": [28, 0, 0], "left_shin": [18, 0, 0],
                                   "brows": [0, 0, 0], "@brows": [0, -0.2, 0], "@fang_row": [0, -0.3, 0]})
    K(c, 0, hunt)
    K(c, 0.16, hiss, E_SNAP)
    K(c, 0.72, P_(hiss, {"head": [-32, 8, 4]}), E_IO)
    K(c, 1.0, hunt, E_IO)
    c.layer("head", lambda t: [1.8 * math.sin(t * 70) if 0.2 < t < 0.7 else 0, 0, 0])

    # fangs_out (0.5): the head jerks back and the needles drop into place.
    c = clip("fangs_out", 0.5)
    K(c, 0, P_(stand, {"%fang_row": [1, 0.2, 1], "@fang_row": [0, 0.8, 0]}))
    K(c, 0.1, P_(stand, {"head": [-26, 0, 0], "%fang_row": [1, 0.3, 1], "@fang_row": [0, 0.6, 0]}), E_OUT)
    K(c, 0.26, P_(hunt, {"head": [-20, 0, 0], "%fang_row": [1, 1, 1], "@fang_row": [0, 0, 0]}), E_SNAP)
    K(c, 0.5, hunt, E_IO)

    # stunned (loop): dead man's blood -- he sags and sways, arms hanging, the head lolling on its long neck.
    c = clip("stunned", 2.0, loop=True)
    for i, t in enumerate((0, 0.5, 1.0, 1.5, 2.0)):
        k = (-1, 0, 1, 0, -1)[i]
        K(c, t, {"body": [16, 8 * k, 6 * k], "head": [28 + 6 * abs(k), -12 * k, 16 * k], "@root": [0, -1.8, 0],
                 "right_leg": [-14, 0, 4], "left_leg": [-14, 0, -4], "right_shin": [32, 0, 0], "left_shin": [32, 0, 0],
                 "right_arm": [6, 0, 4 + 5 * k], "left_arm": [6, 0, -4 + 5 * k], "right_fingers": [-6, 0, 0], "left_fingers": [-6, 0, 0]},
          E_IO if i else None)

    # rise (1.5): from flat on his back he swings up stiff from the heels; the head snaps up last.
    c = clip("rise", 1.5)
    flat = P_(stand, {"root": [-90, 0, 0], "@root": [0, 2.0, 0], "head": [10, 0, 0], "right_arm": [0, 0, 6], "left_arm": [0, 0, -6],
                      "coat_tail": [-10, 0, 0]})
    K(c, 0, flat)
    K(c, 0.3, P_(flat, {"right_fingers": [-80, 0, 0], "left_fingers": [-80, 0, 0], "right_index": [-70, 0, 0], "left_index": [-70, 0, 0]}), E_IO)
    K(c, 1.05, P_(stand, {"root": [-6, 0, 0], "@root": [0, 0.2, 0], "head": [34, 0, 0]}), E_IN)
    K(c, 1.25, P_(hunt, {"head": [-18, 0, 0]}), E_SNAP)
    K(c, 1.5, hunt, E_IO)

    # decapitated (1.5, held): the head goes -- it flies, drops and rolls; the body kneels and falls on its face.
    c = clip("decapitated", 1.5, hold=True)
    knees = {"@root": [0, -6.0, 0], "right_leg": [-6, 0, 0], "left_leg": [-6, 0, 0], "right_shin": [92, 0, 0], "left_shin": [92, 0, 0],
             "body": [18, 0, 0], "right_arm": [8, 0, 12], "left_arm": [8, 0, -12]}
    down = {"@root": [0, -4.4, -4.4], "root": [82, 0, 0], "right_leg": [-70, 0, 0], "left_leg": [-70, 0, 0], "right_shin": [96, 0, 0],
            "left_shin": [96, 0, 0], "body": [6, 0, 0], "right_arm": [-160, 0, 16], "left_arm": [-170, 0, -12], "coat_tail": [-30, 0, 0]}
    K(c, 0, hunt)
    K(c, 0.08, P_(hunt, {"@head": [3.0, 4.0, -1.0], "head": [-40, 30, 60], "body": [-8, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-30, 0, -30]}), E_OUT)
    K(c, 0.25, P_(hunt, {"@head": [7.0, -2.0, -4.0], "head": [-120, 60, 140], "body": [-4, 0, 0]}), E_IN)
    K(c, 0.65, P_(knees, {"@head": [10.0, -25.0, -6.0], "head": [-200, 90, 180]}), E_IN)
    K(c, 1.15, P_(down, {"@head": [10.0, -31.0, 6.0], "head": [-260, 120, 250]}), E_IN)
    K(c, 1.5, P_(down, {"@head": [10.5, -31.0, 6.5], "head": [-270, 120, 260]}), E_OUT)

    # lunge (1.0): he sinks, then is simply there -- a blurring dash forward, both arms out and clawing (the hit at 0.4).
    c = clip("lunge", 1.0)
    coil = P_(hunt, {"body": [30, 0, 0], "head": [-30, 0, 0], "@root": [0, -2.6, 1.4], "right_leg": [-40, 0, 6], "left_leg": [10, 0, -6],
                     "right_shin": [60, 0, 0], "left_shin": [40, 0, 0], "right_arm": [40, 0, 30], "left_arm": [40, 0, -30]})
    dash = P_(V_CLAW_R, V_CLAW_L, {"body": [40, 0, 0], "head": [-40, 0, 0], "@root": [0, -1.0, -9.0], "right_arm": [-96, 8, 16],
                                   "left_arm": [-96, -8, -16], "right_forearm": [-6, 0, 0], "left_forearm": [-6, 0, 0],
                                   "right_leg": [-60, 0, 0], "right_shin": [40, 0, 0], "left_leg": [40, 0, 0], "left_shin": [20, 0, 0],
                                   "coat_tail": [70, 0, 0], "@fang_row": [0, -0.4, 0]})
    K(c, 0, hunt)
    K(c, 0.25, coil, E_OUT)
    K(c, 0.4, dash, E_SNAP)
    K(c, 0.6, P_(dash, {"@root": [0, -1.2, -10.0], "right_arm": [-70, 30, -20], "left_arm": [-70, -30, 20]}), E_OUT)
    K(c, 1.0, P_(hunt, {"@root": [0, -0.4, 0]}), E_IO)

    # drain (2.0): the left hand closes on a shoulder (0.4), the right holds the head aside, he bites deep (0.8) and drinks,
    # shuddering, then lets go and straightens, wiping his mouth with a knuckle.
    c = clip("drain", 2.0)
    hold = LG.reach(r, P_(hunt, V_CLAW_L), "left", (2.5, 23.0, -8.5), (VB.ax, VB.palm - 1.2, -0.5))
    hold = LG.reach(r, P_(hold, V_CLAW_R), "right", (-2.0, 26.5, -8.0), (-VB.ax, VB.palm - 1.2, -0.5))
    deep = P_(hold, {"body": [26, -8, 0], "head": [34, -26, 22], "@root": [0, -0.8, -1.6], "right_leg": [-20, 0, 0], "right_shin": [20, 0, 0],
                     "left_leg": [10, 0, 0], "@fang_row": [0, -0.4, 0]})
    wipe = LG.reach(r, P_(stand, V_FIST_R), "right", (-0.8, 26.2, -5.6), (-VB.ax, VB.palm - 1.0, -0.8))
    K(c, 0, hunt)
    K(c, 0.4, hold, E_OUT)
    K(c, 0.8, deep, E_SNAP)
    K(c, 1.3, P_(deep, {"head": [38, -28, 24], "body": [28, -8, 0]}), E_IO)
    K(c, 1.6, P_(wipe, {"head": [-4, 10, 0]}), E_IO)
    K(c, 2.0, stand, E_IO)
    c.layer("body", lambda t: [1.4 * math.sin(t * 48) if 0.85 < t < 1.35 else 0, 0, 0])
    c.layer("left_fingers", lambda t: [-20 * math.sin(t * 30) if 0.85 < t < 1.35 else 0, 0, 0])

    # command (1.5): he lifts his right hand high, palm out, fingers spread, and hisses to his nest (0.7); then lowers it.
    c = clip("command", 1.5)
    lift = P_(stand, V_OPEN, {"right_arm": [-150, -10, -10], "right_forearm": [-20, 0, 0], "right_hand": [0, 20, 0],
                              "right_fingers": [10, 0, 0], "right_index": [12, 0, 0], "right_thumb": [0, 0, -30],
                              "head": [-10, -10, 0], "body": [-4, -8, 0], "left_arm": [-10, 0, -16], "left_forearm": [-20, 0, 0]})
    call = P_(lift, {"head": [-16, -6, 0], "body": [-6, -8, 0], "@fang_row": [0, -0.3, 0], "@brows": [0, -0.2, 0],
                     "right_fingers": [-30, 0, 0], "right_index": [-24, 0, 0]})
    K(c, 0, stand)
    K(c, 0.45, lift, E_OUT)
    K(c, 0.7, call, E_SNAP)
    K(c, 1.1, P_(call, {"head": [-14, 6, 0]}), E_IO)
    K(c, 1.5, stand, E_IO)
    c.layer("head", lambda t: [1.5 * math.sin(t * 60) if 0.7 < t < 1.1 else 0, 0, 0])

    for cl in clips:
        cl.done(step=0.05)
    return f


# =====================================================================================================
# The Alpha werewolf
# =====================================================================================================

WK = 1.3            # scale over the legacy wolf's proportions
SILVER = Ramp("#17181c", "#2c2e33", "#474a51", "#676a72", "#8b8e95", "#b4b7bd")
SILVER_PALE = Ramp("#3f4246", "#5e6165", "#7f8286", "#a0a3a7", "#c0c3c6", "#dde0e2")
SADDLE = Ramp("#0a0a0c", "#151619", "#22242a", "#33353c", "#474a52", "#5f636b")
SCAR = hexc("#b9a3a0")
W_CLAW = Ramp("#050505", "#0f0e0d", "#1d1b19", "#302d2a", "#4a4641", "#6e6962")
GUMS = LG.GUMS
TOOTH = Ramp("#5f5644", "#7f7660", "#a09679", "#bdb393", "#d6cdab", "#ebe4c8")
AMBER, AMBER_HOT = hexc("#ffad1f"), hexc("#ffe08a")


class Scaled:
    """A Model seen through a uniform scale: pivots, origins, sizes and inflates are multiplied by `k`."""

    def __init__(self, m, k):
        self.m, self.k = m, k

    def bone(self, name, pivot, parent=None, rotation=None):
        return self.m.bone(name, tuple(v * self.k for v in pivot), parent, rotation)

    def cube(self, bone, origin, size, material, glow=None, inflate=0.0, faces=None, rotation=None, pivot=None, density=2, tag=None):
        k = self.k
        return self.m.cube(bone, tuple(v * k for v in origin), tuple(v * k for v in size), material, glow, inflate=inflate * k,
                           faces=faces, rotation=rotation, pivot=tuple(v * k for v in pivot) if pivot else None, density=density, tag=tag)


def wfur(seed, ramp=SILVER, base=3.0, belly=None, belly_face="north", saddle=0.0, scars=0.0):
    """Shaggy silver fur in overlapping tufts, mottled; a paler throat/belly on `belly_face`, a dark saddle on the back, pale
    tips catching the light, and here and there an old scar where it never grew back."""
    def m(f, x, y, w, h):
        n = fbm(x * 0.9 + len(f) * 5, y * 0.6, seed, 64, 64, 3, 6.0)
        col = x // 2
        off = int(h01("tuft", seed, f, col) * 4)
        k = (y + off + (col % 2)) % 4
        v = base + (n - 0.5) * 1.0 + (0.32, 0.12, -0.08, -0.36)[k] - 0.45 * (y / max(1, h))
        if f == "up":
            v += 0.35
        elif f == "down":
            v -= 0.9
        elif f == "south":
            v -= 0.3
        r = ramp
        if belly is not None and f == belly_face and abs(x - (w - 1) / 2) < w * belly:
            r = SILVER_PALE
        if saddle and f in ("south", "up"):
            r = SADDLE if abs(x - (w - 1) / 2) < w * saddle else r
        c = tone(r, v)
        if k == 0 and h01("tip", seed, f, x, y) < 0.12:
            c = mix(c, hexc("#dfe3e7"), 0.22)
        if scars and f in ("north", "east", "west"):
            # Old claw scars: short pale diagonal lines in threes.
            s = (x + y * 0.6 + h01("sc", seed, f) * 8) % 9
            if s < 0.8 and fbm(x * 0.5 + 9, y * 0.5, seed + 31, 64, 64, 1, 3.0) > 1 - scars:
                c = mix(SCAR, c, 0.25)
        return c
    return m


def wfringe(mat, depth=2, seed=1):
    def m(f, x, y, w, h):
        if f not in ("up", "down") and y >= h - depth:
            if h01("fr", seed, f, x) * depth < (y - (h - depth)) + 0.6:
                return CLEAR
        return mat(f, x, y, w, h)
    return m


W_HIP = 14.4


def build_werewolf(m0):
    m = Scaled(m0, WK)
    m.bone("root", (0, 0, 0))
    fur = wfur(39201)
    fur_b = wfur(39202, belly=0.2)
    fur_d = wfur(39203, ramp=SADDLE, base=3.0)
    m.bone("body", (0, W_HIP, 0), "root")
    m.bone("chest", (0, 19.0, 0), "body")
    m.bone("mane", (0, 26.0, 0.5), "chest")
    m.bone("mane_back", (0, 25.0, 3.4), "chest")
    m.bone("head", (0, 26.5, -3.0), "chest")
    m.bone("neck_ruff", (0, 27.0, -2.5), "head")
    m.bone("jaw", (0, 27.6, -6.5), "head")
    m.bone("tongue", (0, 27.7, -7.0), "jaw")
    for side, s in SIDES:
        m.bone(f"{side}_ear", (2.4 * s, 32.8, -3.4), "head")
        m.bone(f"{side}_arm", (7.4 * s, 24.4, 0), "chest")
        m.bone(f"{side}_forearm", (7.8 * s, 17.0, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (7.8 * s, 10.8, 0), f"{side}_forearm")
        m.bone(f"{side}_claws", (7.8 * s, 8.4, -0.8), f"{side}_hand")
        m.bone(f"{side}_leg", (2.9 * s, W_HIP, 0.3), "root", rotation=(-25, 0, 0))
        m.bone(f"{side}_shin", (2.9 * s, W_HIP - 6.0, 0.3), f"{side}_leg", rotation=(60, 0, 0))
        m.bone(f"{side}_foot", (2.9 * s, W_HIP - 11.0, 0.3), f"{side}_shin", rotation=(-50, 0, 0))
        m.bone(f"{side}_paw", (2.9 * s, W_HIP - 15.0, 0.3), f"{side}_foot", rotation=(15, 0, 0))
    m.bone("tail", (0, W_HIP - 0.2, 2.8), "body", rotation=(40, 0, 0))
    m.bone("tail_tip", (0, W_HIP - 6.4, 2.8), "tail", rotation=(-14, 0, 0))

    # Body: a lean waist under a vast barrel chest, a hump of muscle over the shoulders, the silver mane like a mantle.
    m.cube("body", (-4.2, W_HIP - 1.0, -2.8), (8.4, 5.8, 5.6), fur_b, density=2, tag="belly")
    m.cube("body", (-4.0, W_HIP - 2.0, -2.6), (8.0, 1.8, 5.2), wfringe(fur, 1, 39204), density=2, tag="hips")
    m.cube("chest", (-6.4, 18.0, -4.2), (12.8, 8.4, 8.2), wfur(39205, belly=0.16, scars=0.6), density=2, tag="chest")
    m.cube("chest", (-5.6, 24.6, -2.4), (11.2, 2.8, 7.2), none_on(("down",), fur_d), density=2, tag="hump")
    # Pectorals and a ridge down the sternum shaded into the fur.
    for s in (-1, 1):
        m.cube("chest", (s * 3.0 - 2.6, 21.6, -4.5), (5.2, 3.6, 0.5), lambda f, x, y, w, h: tone(SILVER_PALE, 3.0 - y * 0.5 + (0.6 if y == 0 else 0)),
               density=2, tag="pec")
    m.cube("chest", (-0.3, 18.4, -4.35), (0.6, 5.2, 0.2), solid(SILVER_PALE[1]), density=4, tag="sternum")
    m.cube("mane", (-6.0, 21.6, -4.7), (12.0, 5.8, 9.4), wfringe(wfur(39206, base=3.9), 4, 39206), density=2, tag="mantle")
    m.cube("mane", (-5.0, 26.4, -3.6), (10.0, 1.8, 7.4), none_on(("down",), wfur(39207, base=3.8)), density=2, tag="mantle_top")
    m.cube("mane_back", (-1.6, 16.0, 0.0), (3.2, 10.0, 1.8), wfringe(fur_d, 2, 39208), density=2, tag="spine_mane",
           rotation=(-6, 0, 0), pivot=(0, 26, 0.5))
    for i, (x, y) in enumerate(((-2.6, 22.0), (2.6, 21.0), (0, 19.0))):
        m.cube("mane_back", (x - 1.2, y - 4.0, 0.6), (2.4, 4.4, 1.0), wfringe(wfur(39209 + i, base=3.4), 2, 39209 + i), density=2,
               tag="back_tuft", rotation=(-14, 0, 0), pivot=(x, y, 0.6))

    # Head: a heavy wolf's skull, long scarred muzzle, black lips, yellowed teeth; ears up; heavy brows; amber eyes.
    hy = 26.5

    def skull(f, x, y, w, h):
        c = wfur(39210, base=3.2, scars=0.5)(f, x, y, w, h)
        if f == "north":
            d = abs(x - (w - 1) / 2)
            if y in (3, 4) and 6.0 <= d < 9.0:
                # Slanted amber eyes either side of the muzzle's root: a hot point, a dark slit of a pupil.
                if y == 3 and d >= 8.0:
                    return AMBER_HOT
                if y == 4 and 6.5 <= d < 7.5:
                    return hexc("#1a0f05")
                return AMBER
            if y == 5 and 6.0 <= d < 9.0:
                return SADDLE[2]
            if y == 2 and 5.0 <= d < 10.0:
                return SADDLE[1]                     # the heavy brow's shadow
            if y >= 6:
                return tone(SILVER_PALE, 2.6 + 0.3 * math.sin(x))
        return c

    def eye_glow(f, x, y, w, h, c):
        return hexc("#ffd36a") if c == AMBER_HOT else (hexc("#ff9a14") if c == AMBER else None)
    m.cube("head", (-4.2, hy, -7.0), (8.4, 6.6, 7.4), skull, eye_glow, density=2, tag="skull")
    m.cube("head", (-3.6, hy + 6.2, -6.0), (7.2, 1.4, 5.8), none_on(("down",), wfur(39214, base=3.0, saddle=0.25)), density=2, tag="crown")
    snout = wfur(39211, base=3.0, scars=0.9)

    def muzzle(f, x, y, w, h):
        if f == "north":
            if y == h - 1:
                return hexc("#0c0807")               # the black lips meeting
            return tone(SILVER_PALE, 2.4 - 0.6 * y / max(1, h))
        if f == "up":
            c = snout(f, x, y, w, h)
            if abs(x - (w - 1) / 2 - 1) < 0.6 and 2 <= y <= 6:
                return SCAR                          # the old scar along the bridge
            return c
        if f == "down":
            return GUMS[2]
        if f in ("east", "west") and y == h - 1:
            return hexc("#0c0807")
        return tone(SILVER_PALE, 2.4 - 0.4 * y / max(1, h)) if f != "south" else snout(f, x, y, w, h)
    m.cube("head", (-2.4, hy + 1.2, -12.2), (4.8, 3.2, 5.4), muzzle, density=2, tag="muzzle")
    m.cube("head", (-1.2, hy + 3.8, -12.6), (2.4, 1.0, 0.6), solid(hexc("#090707")), density=4, tag="nose")
    for i, z in enumerate((-12.0, -11.2, -10.4, -9.6, -8.8, -8.0)):
        for s in (-1, 1):
            tall = 1.1 if i == 1 else 0.55
            m.cube("head", (s * 2.0 - 0.2, hy + 1.2 - tall, z), (0.4, tall, 0.4), lambda f, x, y, w, h: TOOTH[5] if y < h - 1 else TOOTH[2],
                   density=4, tag="tooth")

    def jaw(f, x, y, w, h):
        if f == "up":
            return GUMS[3] if 1 <= x <= w - 2 else GUMS[1]
        if f == "down":
            return tone(SILVER_PALE, 2.0)
        return tone(SILVER_PALE, 2.6 - 0.4 * y / max(1, h)) if f != "north" else tone(SILVER_PALE, 2.2)
    m.cube("jaw", (-2.1, hy + 0.1, -11.8), (4.2, 1.1, 5.2), jaw, density=2, tag="jaw")
    m.cube("tongue", (-1.3, hy + 1.1, -11.2), (2.6, 0.15, 4.2), solid(GUMS[4]), density=4, tag="tongue")
    for z in (-11.5, -10.2, -8.9):
        for s in (-1, 1):
            m.cube("jaw", (s * 1.7 - 0.2, hy + 1.2, z), (0.4, 0.6 if z != -11.5 else 0.95, 0.4), solid(TOOTH[4]), density=4, tag="tooth")
    for s in (-1, 1):
        m.cube("head", (s * 4.2 - 0.9, hy + 4.9, -7.15), (1.8, 0.7, 1.2), solid(SADDLE[1]), density=2, tag="brow",
               rotation=(0, 0, 16 * s), pivot=(s * 3.5, hy + 5.2, -7.0))
        m.cube("neck_ruff", ((3.6 if s > 0 else -5.4), hy - 1.0, -5.2), (1.8, 4.6, 5.0), wfringe(wfur(39212, base=3.6), 2, 39212 + s),
               density=2, tag="cheek_ruff", rotation=(0, -20 * s, 10 * s), pivot=(3.6 * s, hy + 2, -3.2))

        def ear(inner):
            def mat(f, x, y, w, h):
                if f == "north" and inner and 0 < x < w - 1:
                    return mix(GUMS[1], SILVER[2], 0.5 + 0.1 * y / max(1, h))
                if f == "up":
                    return tone(SILVER, 3.6)
                return tone(SILVER, 3.0 - 0.3 * y / max(1, h)) if f != "down" else tone(SILVER, 2.0)
            return mat
        # Tapered in three steps to a point; the left one torn at the tip (it is shorter).
        bone, tip = f"{'right' if s < 0 else 'left'}_ear", 1.0 if s < 0 else 0.5
        for i, (wd, ht, dp, y0) in enumerate(((2.6, 2.2, 1.3, 6.4), (1.7, 1.5, 1.0, 8.6), (0.9, tip, 0.7, 10.1))):
            m.cube(bone, (2.6 * s - wd / 2, hy + y0, -3.9 - dp / 2), (wd, ht, dp), ear(i < 2), density=4 if i else 2, tag="ear",
                   rotation=(-10, 0, -12 * s), pivot=(2.4 * s, hy + 6.0, -3.4))
    m.cube("neck_ruff", (-4.4, hy - 2.4, -3.4), (8.8, 3.4, 6.0), wfringe(wfur(39213, base=3.7), 2, 39213), inflate=0.15, density=2,
           tag="throat_ruff")

    # Arms: knotted with muscle under the fur, tufts at the elbows, long black claws.
    for side, s in SIDES:
        cx = 7.8 * s
        m.cube(f"{side}_arm", (cx - 2.4, 16.6, -2.5), (4.8, 8.4, 5.0), wfur(39220 + (s > 0), base=2.7, scars=0.3), density=2, tag="upper_arm")
        m.cube(f"{side}_arm", (cx - 2.8, 21.6, -2.9), (5.6, 3.8, 5.8), wfringe(wfur(39222, base=3.6), 2, 39222 + (s > 0)), density=2,
               tag="shoulder")
        m.cube(f"{side}_forearm", (cx - 2.2, 10.6, -2.3), (4.4, 6.8, 4.6), wfur(39224 + (s > 0), base=2.5), density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 2.0, 15.0, 1.9), (4.0, 2.8, 1.4), wfringe(wfur(39226, base=3.7), 2, 39226 + (s > 0)), density=2,
               tag="elbow_tuft", rotation=(24, 0, 0), pivot=(cx, 17.6, 1.9))
        m.cube(f"{side}_hand", (cx - 2.0, 8.4, -2.1), (4.0, 2.8, 4.2), wfur(39228, ramp=SADDLE, base=3.0), density=2, tag="hand")
        for i, x in enumerate((-1.5, -0.5, 0.5, 1.5)):
            m.cube(f"{side}_claws", (cx + x - 0.25, 5.2, -2.3 + 0.2 * abs(i - 1.5)), (0.5, 3.4, 0.55),
                   lambda f, x_, y, w, h: W_CLAW[4 - y * 4 // max(1, h)] if f != "down" else W_CLAW[0], density=4, tag="claw",
                   rotation=(-14, 0, 0), pivot=(cx + x, 8.4, -2.1))
        m.cube(f"{side}_claws", (cx - 1.8, 7.4, -2.3), (3.6, 1.1, 1.6), wfur(39229, ramp=SADDLE, base=2.6), density=2, tag="knuckles")

    # Legs: thick furred thighs, sinewy shins, long feet and paws with claws.
    for side, s in SIDES:
        x = 2.9 * s
        m.cube(f"{side}_leg", (x - 2.5, W_HIP - 6.2, -2.7), (5.0, 6.8, 5.4), wfur(39230 + (s > 0), base=2.6, saddle=0.0), density=2, tag="thigh")
        m.cube(f"{side}_shin", (x - 1.8, W_HIP - 11.4, -1.8), (3.6, 5.8, 3.6), wfur(39232 + (s > 0), base=2.3), density=2, tag="shin")
        m.cube(f"{side}_shin", (x - 2.0, W_HIP - 7.8, 1.3), (4.0, 2.6, 1.4), wfringe(wfur(39234, base=3.5), 2, 39234 + (s > 0)), density=2,
               tag="calf_tuft", rotation=(-20, 0, 0), pivot=(x, W_HIP - 6, 1.3))
        m.cube(f"{side}_foot", (x - 1.5, W_HIP - 15.4, -1.5), (3.0, 4.6, 3.0), wfur(39236, base=2.2), density=2, tag="foot")
        m.cube(f"{side}_paw", (x - 1.9, W_HIP - 16.2, -3.8), (3.8, 1.2, 4.8), wfur(39237, ramp=SADDLE, base=3.0), density=2, tag="paw")
        for dx in (-1.4, -0.5, 0.4, 1.3):
            m.cube(f"{side}_paw", (x + dx - 0.2, W_HIP - 16.2, -5.0), (0.45, 0.7, 1.3), solid(W_CLAW[3]), density=4, tag="toe_claw")
    m.cube("tail", (-1.6, W_HIP - 6.4, 1.4), (3.2, 6.4, 3.2), wfur(39240, base=3.0, saddle=0.3), density=2, tag="tail")
    m.cube("tail_tip", (-1.9, W_HIP - 13.4, 1.1), (3.8, 7.2, 3.8), wfringe(wfur(39241, base=3.4), 2, 39241), density=2, tag="tail_tip")


def werewolf_anims(r):
    f = AnimFile()
    A = "animation.alpha_werewolf."
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    crouch = {"body": [14, 0, 0], "chest": [12, 0, 0], "head": [-22, 0, 0], "right_arm": [-8, 0, 12], "left_arm": [-8, 0, -12],
              "right_forearm": [-26, 0, 0], "left_forearm": [-26, 0, 0], "right_leg": [-10, 0, 4], "left_leg": [-10, 0, -4],
              "right_shin": [12, 0, 0], "left_shin": [12, 0, 0], "right_foot": [-4, 0, 0], "left_foot": [-4, 0, 0],
              "@root": [0, -1.0, 0], "tail": [-4, 0, 0]}

    c = clip("idle", 3.0, loop=True)
    for t, k in ((0, 0), (0.75, 1), (1.5, 0), (2.25, -1), (3.0, 0)):
        c.key(t, P_(crouch, {"chest": [12 - 3 * abs(k), 0, 0], "head": [-22 + 3 * k, 9 * k, 0], "jaw": [8 + 5 * abs(k), 0, 0],
                             "tail": [-4, 12 * k, 0], "right_ear": [0, 0, 6 * max(0, k)], "left_ear": [0, 0, -6 * max(0, -k)],
                             "mane": [-3 * abs(k), 0, 0]}), E_IO if t else None)
    # Heavy breathing: the chest heaves, the mane lifts with it.
    c.layer("chest", lambda t: [2.0 * math.sin(t * 2 * math.pi * 3 / 3.0), 0, 0])
    c.layer("mane", lambda t: [-1.5 * math.sin(t * 2 * math.pi * 3 / 3.0), 0, 0])
    c.layer("tongue", lambda t: [4 * math.sin(t * 2 * math.pi * 3 / 3.0), 0, 0])

    L = 1.1
    c = clip("walk", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(L * i / 8, P_(crouch, {"right_leg": [-10 - 24 * sn, 0, 4], "left_leg": [-10 + 24 * sn, 0, -4],
                                     "right_shin": [12 + 22 * max(0, math.sin(ph + 1.2)), 0, 0],
                                     "left_shin": [12 + 22 * max(0, -math.sin(ph + 1.2)), 0, 0],
                                     "right_arm": [-8 + 20 * sn, 0, 12], "left_arm": [-8 - 20 * sn, 0, -12],
                                     "chest": [14, 8 * sn, -3 * sn], "head": [-24, -7 * sn, 2 * sn], "@root": [0, -1.0 + 0.7 * abs(cs), 0],
                                     "tail": [-2, 16 * sn, 0], "mane": [2 * abs(cs), 0, 0]}), E_IO if i else None)

    L = 0.55
    c = clip("run", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        sn, cs = math.sin(ph), math.cos(ph)
        c.key(L * i / 8, {"body": [52 + 6 * cs, 0, 0], "chest": [24 - 6 * cs, 0, 0], "head": [-62 + 6 * cs, 0, 0], "jaw": [16, 0, 0],
                          "right_arm": [-48 + 46 * sn, 0, 6], "left_arm": [-48 + 46 * math.sin(ph - 0.5), 0, -6],
                          "right_forearm": [-20 + 20 * cs, 0, 0], "left_forearm": [-20 + 20 * math.cos(ph - 0.5), 0, 0],
                          "right_leg": [-14 - 42 * sn, 0, 3], "left_leg": [-14 - 42 * math.sin(ph + 0.4), 0, -3],
                          "right_shin": [20 + 22 * max(0, cs), 0, 0], "left_shin": [20 + 22 * max(0, math.cos(ph + 0.4)), 0, 0],
                          "@root": [0, -3.6 + 1.6 * abs(sn), 0], "tail": [-44, 8 * sn, 0], "right_ear": [-34, 0, 0], "left_ear": [-34, 0, 0],
                          "mane": [-8 + 4 * cs, 0, 0]}, E_IO if i else None)

    # claw (0.7): rears and rakes the right claws down across (the hit at 0.35).
    c = clip("claw", 0.7)
    up = P_(crouch, {"chest": [-8, 22, 0], "body": [6, 10, 0], "head": [-20, -16, 0], "jaw": [26, 0, 0],
                     "right_arm": [-165, 20, 30], "right_forearm": [-30, 0, 0], "left_arm": [-40, 0, -24]})
    rake = P_(crouch, {"chest": [28, -26, 0], "body": [16, -10, 0], "head": [-30, 14, 0], "jaw": [32, 0, 0],
                       "right_arm": [-40, -42, -10], "right_forearm": [-6, 0, 0], "left_arm": [-20, 0, -30], "@root": [0, -1.6, -1.8]})
    c.key(0, crouch)
    c.key(0.22, up, E_OUT)
    c.key(0.35, rake, E_SNAP)
    c.key(0.5, P_(rake, {"right_arm": [-20, -48, -14]}), E_OUT)
    c.key(0.7, crouch, E_IO)

    # pounce (0.9): sinks low, springs and lands claws first (the hit at 0.5).
    c = clip("pounce", 0.9)
    low = P_(crouch, {"body": [30, 0, 0], "chest": [20, 0, 0], "head": [-40, 0, 0], "right_leg": [-40, 0, 3], "left_leg": [-40, 0, -3],
                      "right_shin": [40, 0, 0], "left_shin": [40, 0, 0], "@root": [0, -3.4, 1.2], "right_arm": [10, 0, 14], "left_arm": [10, 0, -14]})
    air = P_(crouch, {"body": [10, 0, 0], "chest": [-4, 0, 0], "head": [-30, 0, 0], "jaw": [40, 0, 0], "right_arm": [-150, 0, 20],
                      "left_arm": [-150, 0, -20], "right_leg": [30, 0, 3], "left_leg": [30, 0, -3], "right_shin": [0, 0, 0],
                      "left_shin": [0, 0, 0], "@root": [0, 6.0, -10.0], "tail": [12, 0, 0]})
    land = P_(crouch, {"body": [40, 0, 0], "chest": [30, 0, 0], "head": [-50, 0, 0], "jaw": [30, 0, 0], "right_arm": [-80, 0, 16],
                       "left_arm": [-80, 0, -16], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "@root": [0, -3.6, -16.0]})
    c.key(0, crouch)
    c.key(0.22, low, E_OUT)
    c.key(0.34, air, E_OUT)
    c.key(0.5, land, E_IN)
    c.key(0.7, P_(land, {"@root": [0, -2.4, -16.0]}), E_OUT)
    c.key(0.9, crouch, E_IO)

    # howl (2.0): straightens to his full height, throws the head back, jaw wide (the call goes out at 0.5) and holds it.
    c = clip("howl", 2.0)
    howl = P_(crouch, {"body": [-6, 0, 0], "chest": [-16, 0, 0], "head": [-62, 0, 0], "jaw": [34, 0, 0], "right_arm": [-10, 0, 26],
                       "left_arm": [-10, 0, -26], "right_forearm": [-16, 0, 0], "left_forearm": [-16, 0, 0], "@root": [0, 0.6, 0],
                       "right_ear": [-34, 0, 0], "left_ear": [-34, 0, 0], "tail": [-14, 0, 0], "mane": [-12, 0, 0]})
    c.key(0, crouch)
    c.key(0.3, P_(crouch, {"head": [-6, 0, 0], "chest": [20, 0, 0]}), E_IO)
    c.key(0.5, howl, E_OUT)
    c.key(1.6, P_(howl, {"head": [-66, 0, 0], "jaw": [38, 0, 0]}), E_IO)
    c.key(2.0, crouch, E_IO)
    c.layer("jaw", lambda t: [3 * math.sin(t * 30) if 0.55 < t < 1.6 else 0, 0, 0])
    c.layer("mane", lambda t: [2.5 * math.sin(t * 40) if 0.55 < t < 1.6 else 0, 0, 0])

    # flee (0.8): silver -- it yelps and cringes away, ears flat, tail down.
    c = clip("flee", 0.8)
    cringe = P_(crouch, {"body": [30, 30, 0], "chest": [24, 20, 0], "head": [-10, 40, 0], "jaw": [16, 0, 0], "right_arm": [-60, 30, 10],
                         "left_arm": [-20, 0, -30], "right_ear": [40, 0, 30], "left_ear": [40, 0, -30], "tail": [32, 0, 0], "@root": [0, -2.4, 1.2]})
    c.key(0, crouch)
    c.key(0.12, P_(crouch, {"chest": [-10, 0, 0], "head": [-40, 0, 0], "jaw": [38, 0, 0]}), E_SNAP)
    c.key(0.4, cringe, E_OUT)
    c.key(0.8, P_(crouch, {"right_ear": [30, 0, 20], "left_ear": [30, 0, -20], "tail": [22, 0, 0]}), E_IO)

    # turn (1.0): he is always the wolf -- a short shake of the great head, the mane flying.
    c = clip("turn", 1.0)
    c.key(0, crouch)
    for i, t in enumerate((0.15, 0.3, 0.45, 0.6, 0.75)):
        s = 1 if i % 2 == 0 else -1
        k = 1 - i * 0.18
        c.key(t, P_(crouch, {"head": [-20, 26 * s * k, 18 * s * k], "chest": [12, -6 * s * k, 0], "mane": [0, 8 * s * k, 10 * s * k],
                             "jaw": [12, 0, 0], "right_ear": [0, 0, 20 * s * k], "left_ear": [0, 0, 20 * s * k]}), E_IO)
    c.key(1.0, crouch, E_IO)

    # leap (1.5): crouches deep, springs high (0.6, the hit as he leaves the ground at the hunter), sails and lands heavily.
    c = clip("leap", 1.5)
    deep = P_(crouch, {"body": [34, 0, 0], "chest": [26, 0, 0], "head": [-46, 0, 0], "right_leg": [-56, 0, 6], "left_leg": [-56, 0, -6],
                       "right_shin": [56, 0, 0], "left_shin": [56, 0, 0], "right_foot": [-20, 0, 0], "left_foot": [-20, 0, 0],
                       "@root": [0, -5.0, 1.6], "right_arm": [20, 0, 20], "left_arm": [20, 0, -20], "tail": [20, 0, 0]})
    spring = P_(crouch, {"body": [-10, 0, 0], "chest": [-20, 0, 0], "head": [-40, 0, 0], "jaw": [44, 0, 0], "right_arm": [-170, 0, 24],
                         "left_arm": [-170, 0, -24], "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0], "right_leg": [24, 0, 2],
                         "left_leg": [24, 0, -2], "right_shin": [-4, 0, 0], "left_shin": [-4, 0, 0], "@root": [0, 14.0, -12.0],
                         "tail": [30, 0, 0], "mane": [-14, 0, 0]})
    landing = P_(crouch, {"body": [44, 0, 0], "chest": [32, 0, 0], "head": [-52, 0, 0], "jaw": [24, 0, 0], "right_arm": [-70, 0, 26],
                          "left_arm": [-70, 0, -26], "right_leg": [-50, 0, 6], "left_leg": [-50, 0, -6], "right_shin": [50, 0, 0],
                          "left_shin": [50, 0, 0], "@root": [0, -4.6, -22.0], "mane": [10, 0, 0]})
    c.key(0, crouch)
    c.key(0.45, deep, E_OUT)
    c.key(0.6, spring, E_OUT)
    c.key(0.85, P_(spring, {"@root": [0, 10.0, -18.0], "body": [20, 0, 0], "right_arm": [-110, 0, 20], "left_arm": [-110, 0, -20]}), E_IO)
    c.key(1.05, landing, E_IN)
    c.key(1.25, P_(landing, {"@root": [0, -3.2, -22.0]}), E_OUT)
    c.key(1.5, crouch, E_IO)

    # slam (1.3): rears up on his hind legs, both fists raised over his head, and brings them down into the ground (0.6).
    c = clip("slam", 1.3)
    rear = P_(crouch, {"body": [-14, 0, 0], "chest": [-22, 0, 0], "head": [-10, 0, 0], "jaw": [30, 0, 0], "right_arm": [-176, -6, 8],
                       "left_arm": [-176, 6, -8], "right_forearm": [-40, 0, 0], "left_forearm": [-40, 0, 0], "@root": [0, 2.0, 1.6],
                       "right_leg": [4, 0, 6], "left_leg": [4, 0, -6], "tail": [10, 0, 0], "mane": [-10, 0, 0]})
    smash = P_(crouch, {"body": [44, 0, 0], "chest": [36, 0, 0], "head": [-50, 0, 0], "jaw": [36, 0, 0], "right_arm": [-60, 10, 10],
                        "left_arm": [-60, -10, -10], "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0], "@root": [0, -4.4, -3.0],
                        "right_leg": [-40, 0, 8], "left_leg": [-40, 0, -8], "right_shin": [44, 0, 0], "left_shin": [44, 0, 0],
                        "mane": [14, 0, 0]})
    c.key(0, crouch)
    c.key(0.42, rear, E_OUT)
    c.key(0.6, smash, E_SNAP)
    c.key(0.9, P_(smash, {"head": [-34, 0, 0]}), E_OUT)
    c.key(1.3, crouch, E_IO)
    c.layer("chest", lambda t: [2.5 * math.sin(t * 60) if 0.6 < t < 0.8 else 0, 0, 0])

    for cl in clips:
        cl.done(step=0.05)
    return f


# =====================================================================================================
# The Alpha shapeshifter
# =====================================================================================================

SB = LG.Build(hip=12.6, neck=23.6, tw=3.2, depth=3.2, aw=2.7, ax=5.0, sx=4.6, sy=22.8, elbow=17.8, wrist=13.2, palm=11.2,
              lx=1.6, lw=2.9, knee=6.2, shoe_h=0.0)
S_SKIN = Ramp("#4f4848", "#726a69", "#968d8b", "#b4aba7", "#ccc4c0", "#e0dad6")
S_RAW = LG.RAW
S_SLIME = LG.SLIME
S_PEEL = LG.PEEL
PEEL_EDGE = Ramp("#7c7268", "#9a9084", "#b8ae9f", "#d2c9b8", "#e6dfd0", "#f3eee4")
S_HAIR = Ramp("#1a1716", "#2a2523", "#3b3431", "#4f4642", "#665b56", "#80746e")
RAG = Ramp("#1f1a14", "#2e271e", "#3e3528", "#504534", "#645743", "#7a6b54")
FLARE, FLARE_EDGE = hexc("#f2f8ff"), hexc("#9fb4cc")
S_TEETH = Ramp("#4e4838", "#6b6450", "#8c8468", "#a9a083", "#c3ba9c", "#dbd4b8")

# The upper face (11 rows) on the skull, the lower jaw (5 rows) on `jaw`.
SHIFTER_UPPER = [
    "hsshsssshssshssh",
    "sssssssssssssss s",
    "ssrrssssssssrrss",
    "ssrrrssssssrrrss",
    "skkkkssssssskkkk",
    "skEFkssssssskFEk",
    "skkkkssssssskkkk",
    "srkksssssssskkrs",
    "rrssssnNNnssssrr",
    "rrrsssssssssssrr",
    "rrssMMMMMMMMssrr",
]
SHIFTER_JAW = [
    "rrssMTMTMTMMssrr",
    "rrssllllllllssrr",
    "rrrsssssssssssrr",
    "jrrsssssssssrrrj",
    "jjjrrsssssrrrjjj",
]


def _fix16(rows):
    out = []
    for r in rows:
        r = r.replace(" ", "")
        out.append((r + "s" * 16)[:16])
    return out


SHIFTER_UPPER = _fix16(SHIFTER_UPPER)
SHIFTER_JAW = _fix16(SHIFTER_JAW)


def sick_skin_m(seed):
    """His own skin: grey-pink and bloodless, blotched and veined, crazed where it has dried and lifted."""
    base = H.skin_mat(S_SKIN, seed, 3.0, 0.5)

    def m(f, x, y, w, h):
        c = base(f, x, y, w, h)
        vein = fbm(x * 0.8 + len(f) * 3, y * 0.5, seed + 5, 64, 64, 2, 5.0)
        if abs(vein - 0.5) < 0.012 and fbm(x * 0.5 + 3, y * 0.5 + w, seed + 2, 64, 64, 1, 3.0) > 0.55:
            c = mix(c, hexc("#5f5470"), 0.35)             # a vein showing through
        if fbm(x * 0.9 + 11 + w * 3, y * 0.9 + len(f) * 7, seed + 9, 64, 64, 3, 5.0) > 0.64:
            c = mix(c, hexc("#a88a86"), 0.14)             # blotched, bloodless pink
        return c
    return m


def skin_sheet(seed, inner="south"):
    """A sheet of his own skin coming away: the same grey-pink, paler and dried at the curled, ragged lower edge, holes
    where it has worn through, wet and yellow on the underside."""
    def m(f, x, y, w, h):
        if f not in ("up", "down") and y >= h - 2 and h01("rag", seed, f, x) < 0.55:
            return CLEAR
        if f in ("north", "south") and h01("hole", seed, f, x, y) < 0.05 and 0 < y < h - 2:
            return CLEAR
        if f == inner:
            return mix(tone(S_SLIME, 2.6 + h01("wet", seed, x, y) * 1.6), S_RAW[2], 0.25)
        t = y / max(1, h - 1)
        c = tone(S_SKIN, 3.6 + 0.9 * t - 0.4 * fbm(x * 2, y * 2, seed, 64, 64, 2, 8.0))
        if t > 0.7:
            c = mix(c, PEEL_EDGE[4], 0.45)                 # dried paper-thin at the edge
        if x in (0, w - 1):
            c = shade(c, 0.82)
        return c
    return m


def raw_patch_m(seed, skin, share=0.35):
    """Skin with raw wet patches where it has already come away."""
    raw = LG.slime_m(seed + 1)

    edge = PEEL_EDGE
    lo = 0.72 - share * 0.25

    def m(f, x, y, w, h):
        # Each face its own patch of the noise (offset by its size and side), tops mostly whole.
        n = fbm(x * 0.7 + len(f) * 9 + w * 5.3, y * 0.6 + h * 3.7 + (17 if f in ("east", "south") else 0), seed, 64, 64, 2, 4.0)
        if f == "up":
            n -= 0.08
        if n > lo + 0.035:
            return mix(raw(f, x, y, w, h), hexc("#6e4246"), 0.3)
        if n > lo:
            return edge[4] if (x + y) % 2 else edge[3]    # the curled, papery edge of the lifted skin
        if n > lo - 0.02:
            return shade(skin(f, x, y, w, h), 0.8)        # its shadow
        return skin(f, x, y, w, h)
    return m


def build_shapeshifter(m):
    b = SB
    m.bone("root", (0, 0, 0))
    LG.skeleton(m, b)
    m.bone("left_index", (b.ax, b.palm, -1.0), "left_hand")
    m.bone("jaw", (0, b.neck + 2.5, -1.0), "head")
    m.bone("flap_face", (1.4, b.neck + 6.6, -4.2), "head")
    m.bone("skin_flaps", (0, b.neck - 0.8, 0), "body")
    m.bone("flap_chest", (-1.2, b.neck - 1.0, -b.depth / 2 - 0.1), "skin_flaps")
    m.bone("flap_back", (0.6, b.neck - 0.6, b.depth / 2 + 0.1), "skin_flaps")
    m.bone("flap_shoulder_r", (-b.sx, b.sy + 0.6, 0), "skin_flaps")
    m.bone("flap_shoulder_l", (b.sx, b.sy + 0.6, 0), "skin_flaps")
    m.bone("flap_belly", (1.4, b.hip + 4.0, -b.depth / 2 - 0.1), "skin_flaps")
    for side, s in SIDES:
        m.bone(f"flap_arm_{side[0]}", (b.ax * s, b.elbow - 0.4, -1.4), f"{side}_forearm")

    sk = sick_skin_m(39301)
    patched = raw_patch_m(39302, sk, 0.12)
    hr = LG.hair_m(S_HAIR, 39303, 2.4)
    raw = LG.slime_m(39304)

    def eye_glow(f, x, y, w, h, c):
        return hexc("#f6faff") if c == FLARE else (hexc("#7d93ad") if c == FLARE_EDGE else None)
    legend = {"s": sk, "h": hr, "r": raw, "k": lambda f, x, y, w, h: mix(sk(f, x, y, w, h), S_RAW[0], 0.45), "F": FLARE, "E": FLARE_EDGE,
              "n": S_SKIN[4], "N": S_RAW[0], "M": S_RAW[0], "T": S_TEETH[5], "l": mix(S_RAW[2], S_SKIN[2], 0.5),
              "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.84)}

    def face_of(rows, y_off):
        def mat(f, x, y, w, h):
            if f == "north":
                v = legend.get(rows[y][x], sk)
                return v(f, x, y, w, h) if callable(v) else v
            if f == "up":
                return hr(f, x, y, w, h) if h01("bald", 39305, x, y) < 0.18 else patched(f, x, y, w, h)
            if f == "south" and h01("strand", 39306, x) < 0.25 and y + y_off < 10:
                return hr(f, x, y, w, h)
            return patched(f, x, y + y_off, w, h + y_off)
        return mat
    upper = 5.5
    m.cube("head", (-4, b.neck + 2.5, -4), (8, upper, 8), face_of(SHIFTER_UPPER, 0), eye_glow, density=2, tag="skull")
    m.cube("jaw", (-3.8, b.neck, -3.8), (7.6, 2.5, 7.4), face_of(SHIFTER_JAW, 11), density=2, tag="jaw")
    # The mouth inside: a dark red throat behind the jaw's lip, needle teeth hanging from the upper gum.
    m.cube("head", (-3.4, b.neck + 1.6, -3.7), (6.8, 1.0, 6.0), lambda f, x, y, w, h: S_RAW[0] if f != "up" else S_RAW[1],
           density=2, tag="throat")
    for i, x in enumerate((-2.6, -1.8, -1.0, -0.2, 0.6, 1.4, 2.2)):
        m.cube("head", (x, b.neck + 1.6, -3.95), (0.3, 0.9 if i % 2 else 0.6, 0.3), lambda f, x_, y, w, h: S_TEETH[5 - y], density=4,
               tag="needle")
    # A long thin nose, almost gone; a few long lank strands of hair hanging from the crown.
    m.cube("head", (-0.5, b.neck + 3.6, -4.5), (1.0, 1.8, 0.5), lambda f, x, y, w, h: S_SKIN[4] if f == "north" and y < 2 else S_SKIN[2],
           density=4, tag="nose")
    for i, (x, z, ln) in enumerate(((-2.8, -1.0, 7.0), (-1.2, 1.8, 9.0), (2.4, 0.4, 6.0), (3.4, 2.4, 8.0), (0.6, 3.6, 10.0))):
        m.cube("head", (x - 0.15, b.neck + 8.0 - ln, z - 0.15), (0.3, ln, 0.3),
               lambda f, x_, y, w, h: CLEAR if y > h - 2 and x_ % 2 else tone(S_HAIR, 2.4 - y * 0.06), density=4, tag="strand",
               rotation=(8 * (i % 3 - 1), 0, 6 * (i % 2 * 2 - 1)), pivot=(x, b.neck + 8.0, z))
    m.cube("body", (-1.2, b.neck - 0.6, -1.2), (2.4, 1.0, 2.4), patched, density=2, tag="neck")
    # The body: ribs under the stretched skin, a hollow belly, raw patches; rags round the hips.
    def torso(f, x, y, w, h):
        c = patched(f, x, y, w, h)
        if f == "north" and 2 <= y <= 9 and y % 2 == 0 and abs(x - (w - 1) / 2) > 1:
            return shade(c, 0.78)                         # the ribs' shadows
        if f == "north" and y > 11 and abs(x - (w - 1) / 2) < 2:
            return shade(c, 0.85)
        if f == "south" and abs(x - (w - 1) / 2) < 0.6:
            return shade(c, 0.75)                         # the spine
        return c
    m.cube("body", (-b.tw, b.hip, -b.depth / 2), (2 * b.tw, b.neck - b.hip, b.depth), torso, density=2, tag="torso")
    for s in (-1, 1):
        m.cube("body", (s * 2.0 - 1.0, b.neck - 2.2, -b.depth / 2 - 0.3), (2.0, 1.0, 0.4),
               lambda f, x, y, w, h: tone(S_SKIN, 3.6 - y), density=2, tag="collarbone", rotation=(0, 0, 10 * s), pivot=(s * 1, b.neck - 1.8, 0))
    rag = LG.worn(RAG, 39310, 2.6, 1.2, scuff=0.08)

    def rags(f, x, y, w, h):
        if f in ("up", "down"):
            return None if f == "down" else rag(f, x, y, w, h)
        if y >= h - 3 and h01("tatter", 39311, f, x) * 3 < y - (h - 3) + 0.8:
            return CLEAR
        if h01("hole", 39312, f, x, y) < 0.06:
            return CLEAR
        return rag(f, x, y, w, h)
    m.cube("body", (-b.tw, b.hip - 2.4, -b.depth / 2), (2 * b.tw, 3.2, b.depth), rags, inflate=0.3, density=2, tag="rag_hips")
    m.cube("body", (-b.tw + 0.4, b.hip + 0.4, -b.depth / 2), (2 * b.tw - 0.8, 0.5, b.depth), none_on(("up", "down"), solid(RAG[1])),
           inflate=0.35, density=4, tag="rag_cord")
    # Arms: thin and long; elbows like knots; raw on the forearms; long cracked nails.
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_arm", (cx - b.aw / 2, b.elbow, -b.aw / 2), (b.aw, b.sy + 0.8 - b.elbow, b.aw), patched, density=2, tag="upper_arm")
        m.cube(f"{side}_forearm", (cx - 1.25, b.wrist, -1.25), (2.5, b.elbow - b.wrist, 2.5), raw_patch_m(39320 + (s > 0), sk, 0.3),
               density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.45, b.elbow - 1.0, -1.1), (2.9, 1.2, 2.4), lambda f, x, y, w, h: tone(S_SKIN, 2.4 + y * 0.5),
               density=2, tag="elbow")
    _long_hands(m, b, H.skin_mat(S_SKIN, 39330, 2.8, 0.5), hexc("#3a3024"))
    # Legs: shins bare, rag strips wound round the thighs, long feet.
    for side, s in SIDES:
        x0 = b.lx * s - b.lw / 2
        m.cube(f"{side}_leg", (x0, b.knee, -b.lw / 2), (b.lw, b.hip - b.knee, b.lw), patched, density=2, tag="thigh")
        m.cube(f"{side}_leg", (x0, b.knee + 1.4, -b.lw / 2), (b.lw, 1.2, b.lw), none_on(("up", "down"), rag), inflate=0.2, density=2,
               tag="wrap")
        m.cube(f"{side}_shin", (x0 + 0.1, 0.0, -b.lw / 2 + 0.1), (b.lw - 0.2, b.knee, b.lw - 0.2), raw_patch_m(39340 + (s > 0), sk, 0.1),
               density=2, tag="shin")
        m.cube(f"{side}_shin", (x0, 0.0, -b.lw / 2 - 1.8), (b.lw, 0.9, 1.9), lambda f, x, y, w, h: tone(S_SKIN, 2.2 if f != "up" else 3.0),
               density=2, tag="foot")

    # --- the sheets of skin coming off him --------------------------------------------------------------------------
    peel = skin_sheet

    def wet_back(mat):
        def m_(f, x, y, w, h):
            c = mat(f, x, y, w, h)
            if c is None or c == CLEAR:
                return c
            if f == "south" or f == "north" and False:
                return mix(c, S_SLIME[3], 0.45)
            return c
        return m_
    m.cube("skin_flaps", (-b.tw, b.neck - 2.6, -b.depth / 2 - 0.35), (2 * b.tw, 2.0, b.depth + 0.7),
           none_on(("up", "down"), lambda f, x, y, w, h: peel(39350, inner=None)(f, x, y, w, h) if h01("sf", f, x) < 0.75 else CLEAR), inflate=0.1,
           density=2, tag="mantle")
    m.cube("flap_chest", (-2.6, b.neck - 6.4, -b.depth / 2 - 0.35), (2.8, 5.4, 0.25), wet_back(peel(39351)), density=4, tag="chest_flap",
           rotation=(-8, 0, 6), pivot=(-1.2, b.neck - 1.0, -b.depth / 2 - 0.1))
    m.cube("flap_belly", (0.2, b.hip - 0.2, -b.depth / 2 - 0.35), (2.4, 4.2, 0.25), wet_back(peel(39352)), density=4, tag="belly_flap",
           rotation=(-4, 0, -4), pivot=(1.4, b.hip + 4.0, -b.depth / 2 - 0.1))
    m.cube("flap_back", (-2.4, b.neck - 8.6, b.depth / 2 + 0.1), (4.4, 7.8, 0.25), skin_sheet(39353, inner="north"), density=4, tag="back_flap",
           rotation=(6, 0, -3), pivot=(0.6, b.neck - 0.6, b.depth / 2 + 0.1))
    for bone, s in (("flap_shoulder_r", -1), ("flap_shoulder_l", 1)):
        m.cube(bone, (b.sx * s - 1.6, b.sy - 4.6, -1.7), (3.2, 5.2, 3.4), none_on(("up", "down", "north"), peel(39354 + (s > 0), inner=None)),
               inflate=0.4, density=2, tag="shoulder_flap", rotation=(0, 0, 8 * s), pivot=(b.sx * s, b.sy + 0.6, 0))
    for side, s in SIDES:
        m.cube(f"flap_arm_{side[0]}", (b.ax * s + s * 1.3 - 0.15, b.elbow - 5.4, -1.3), (0.3, 5.0, 2.6), peel(39356 + (s > 0), inner="east" if s < 0 else "west"), density=4,
               tag="arm_flap", rotation=(0, 0, 8 * s), pivot=(b.ax * s + s * 1.3, b.elbow - 0.4, 0))
    # The face peeling down from the brow over the left cheek, slime under it.
    m.cube("flap_face", (1.0, b.neck + 2.2, -4.45), (2.2, 3.4, 0.25), wet_back(peel(39358)), density=4, tag="face_flap",
           rotation=(-10, 0, 8), pivot=(1.4, b.neck + 6.6, -4.2))
    # Drips of yellow slime down the chest and the forearms.
    for x in (-1.8, 0.6, 2.2):
        m.cube("body", (x, b.neck - 7.0 - x, -b.depth / 2 - 0.05), (0.4, 4.0, 0),
               lambda f, x_, y, w, h: CLEAR if h01("dr", x, y) < y / max(1, h) - 0.2 else tone(S_SLIME, 3.4 - y * 0.25), faces=("north",),
               density=4, tag="drip")


S_LOOSE = {"right_fingers": [-26, 0, 0], "right_index": [-20, 0, 0], "left_fingers": [-26, 0, 0], "left_index": [-20, 0, 0],
           "right_arm": [0, 0, 5], "left_arm": [0, 0, -5]}
S_CLAW_R = {"right_fingers": [-46, 0, 0], "right_index": [-40, 0, 0], "right_thumb": [0, 0, -20]}
S_CLAW_L = {"left_fingers": [-46, 0, 0], "left_index": [-40, 0, 0], "left_thumb": [0, 0, 20]}
S_OPEN_R = {"right_fingers": [0, 0, 0], "right_index": [0, 0, 0], "right_thumb": [0, 0, -16]}


def flaps_sway(t, amp=1.0, period=2.0):
    ph = t * 2 * math.pi / period
    return {"flap_chest": [4 * amp * math.sin(ph), 0, 3 * amp * math.sin(ph * 0.5)],
            "flap_back": [-4 * amp * math.sin(ph + 0.7), 0, 2 * amp * math.sin(ph + 1.1)],
            "flap_shoulder_r": [0, 0, -4 * amp * math.sin(ph + 0.4)], "flap_shoulder_l": [0, 0, 4 * amp * math.sin(ph + 1.4)],
            "flap_belly": [3 * amp * math.sin(ph + 2.0), 0, 0], "flap_face": [5 * amp * math.sin(ph + 0.9), 0, 0],
            "flap_arm_r": [6 * amp * math.sin(ph + 0.2), 0, 0], "flap_arm_l": [6 * amp * math.sin(ph + 1.7), 0, 0]}


def shapeshifter_anims(r):
    f = AnimFile()
    A = "animation.alpha_shapeshifter."
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def sway(c, amp, period):
        for bone in ("flap_chest", "flap_back", "flap_shoulder_r", "flap_shoulder_l", "flap_belly", "flap_face", "flap_arm_r", "flap_arm_l"):
            c.layer(bone, lambda t, bone=bone: flaps_sway(t, amp, period)[bone])

    # Hunched and wrong: the head pushed forward and cocked, the shoulders uneven, the long arms hanging.
    stand = P_(S_LOOSE, {"body": [10, 0, 4], "head": [-14, 0, -10], "right_arm": [-4, 0, 6], "left_arm": [2, 0, -3],
                         "right_forearm": [-12, 0, 0], "left_forearm": [-6, 0, 0], "jaw": [6, 0, 0], "@root": [0, -0.3, 0]})
    c = clip("idle", 4.0, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (4.0, 0)):
        c.key(t, P_(stand, {"head": [-14 + 3 * abs(k), 16 * k, -10 + 6 * k], "body": [10, 4 * k, 4], "jaw": [6 + 6 * abs(k), 0, 0]}),
              E_IO if t else None)
    # The head jerks now and then, birdlike.
    c.layer("head", lambda t: [0, 18 if 1.4 < t < 1.55 else 0, 0])
    sway(c, 1.0, 2.0)

    L = 1.2
    c = clip("walk", L, loop=True)
    for i in range(9):
        w = LG.walk_cycle(8, i, stride=22, arm=10, knee=34, lean=8, sway=6)
        c.key(L * i / 8, P_(stand, w, {"head": [-16, w["head"][1] * 2, -10]}), E_IO if i else None)
    c.layer("right_arm", lambda t: [0, 0, 4 * math.sin(4 * math.pi * t / L)])
    sway(c, 1.6, L)

    # attack (0.7): a backhand rake with the long nails (the hit at 0.3).
    c = clip("attack", 0.7)
    wind = P_(stand, S_CLAW_R, {"right_arm": [-60, 60, 70], "right_forearm": [-50, 0, 0], "body": [6, 30, 4], "head": [-6, -20, 0],
                                "left_arm": [-20, 0, -20], "jaw": [22, 0, 0]})
    rake = P_(stand, S_CLAW_R, {"right_arm": [-84, -40, -10], "right_forearm": [-10, 0, 0], "body": [18, -30, 0], "head": [-10, 22, 0],
                                "left_arm": [-30, 0, -30], "@root": [0, -0.6, -1.8], "right_leg": [-20, 0, 0], "right_shin": [16, 0, 0],
                                "left_leg": [14, 0, 0], "jaw": [26, 0, 0]})
    c.key(0, stand)
    c.key(0.18, wind, E_OUT)
    c.key(0.3, rake, E_SNAP)
    c.key(0.45, P_(rake, {"right_arm": [-62, -50, -14]}), E_OUT)
    c.key(0.7, stand, E_IO)
    sway(c, 2.0, 0.7)

    # revealed (1.2): scalded by silver or the true sight -- he recoils, a hand up to his face, then glares and gapes.
    c = clip("revealed", 1.2)
    flinch = P_(stand, {"body": [-12, 14, 0], "head": [-22, 30, 0], "@root": [0, 0, 1.6], "right_arm": [-20, 0, 30],
                        "right_forearm": [-30, 0, 0], "left_leg": [10, 0, 0], "right_leg": [-6, 0, 0]})
    flinch = LG.reach(r, flinch, "left", (1.5, 27.0, -5.8), (SB.ax, SB.palm - 1.0, -0.6))
    glare = P_(stand, S_CLAW_R, S_CLAW_L, {"body": [18, 0, 0], "head": [-26, 0, 0], "jaw": [34, 0, 0], "@root": [0, -0.9, 0],
                                           "right_arm": [-26, 20, 34], "left_arm": [-26, -20, -34], "right_forearm": [-40, 0, 0],
                                           "left_forearm": [-40, 0, 0], "right_leg": [-12, 0, 4], "left_leg": [8, 0, -4]})
    c.key(0, stand)
    c.key(0.12, flinch, E_SNAP)
    c.key(0.5, P_(flinch, {"head": [-12, 20, 0]}), E_IO)
    c.key(0.75, glare, E_OUT)
    c.key(1.2, P_(glare, {"head": [-20, 0, 0], "jaw": [20, 0, 0]}), E_IO)
    c.layer("head", lambda t: [2 * math.sin(t * 80) if 0.7 < t < 1.0 else 0, 0, 0])
    sway(c, 1.8, 0.8)

    # shed (1.5): he claws at his chest and tears a sheet of skin away; it drops; he shudders, raw.
    c = clip("shed", 1.5)
    tear = LG.reach(r, P_(stand, S_CLAW_R, S_CLAW_L), "right", (-1.4, 21.5, -3.6), (-SB.ax, SB.palm - 1.0, -0.6))
    tear = LG.reach(r, tear, "left", (1.4, 21.0, -3.6), (SB.ax, SB.palm - 1.0, -0.6))
    c.key(0, stand)
    c.key(0.4, P_(tear, {"head": [10, 0, 0]}), E_OUT)
    c.key(0.75, P_(tear, {"body": [22, 0, 0], "head": [20, 0, 0], "flap_chest": [70, 0, 30], "@flap_chest": [0, -4, -2],
                          "flap_back": [-40, 0, 0], "@flap_back": [0, -3, 1]}), E_IN)
    c.key(1.05, P_(stand, {"body": [16, 0, 0], "head": [-24, 0, 0], "jaw": [30, 0, 0], "flap_chest": [100, 0, 40],
                           "@flap_chest": [0, -14, -2], "%flap_chest": [1, 1, 1], "flap_back": [-80, 0, 0], "@flap_back": [0, -12, 2]}), E_IN)
    c.key(1.3, P_(stand, {"flap_chest": [100, 0, 40], "@flap_chest": [0, -15, -2], "%flap_chest": [0.01, 0.01, 0.01],
                          "flap_back": [-90, 0, 0], "@flap_back": [0, -13, 2], "%flap_back": [0.01, 0.01, 0.01]}), E_IO)
    c.key(1.5, P_(stand, {"%flap_chest": [1, 1, 1], "%flap_back": [1, 1, 1]}))
    c.layer("body", lambda t: [2.5 * math.sin(t * 70) if 1.0 < t < 1.3 else 0, 0, 0])

    # steal (1.6): his right hand reaches for the hunter's face (the hit at 0.8, the touch that takes it) and holds there while
    # his own face ripples and runs like wax; then he draws the hand back.
    c = clip("steal", 1.6)
    reach_out = LG.reach(r, P_(stand, S_OPEN_R, {"body": [16, -10, 0], "head": [-20, -10, 0], "@root": [0, -0.4, -1.4],
                                                 "right_leg": [-20, 0, 0], "right_shin": [14, 0, 0], "left_leg": [10, 0, 0]}),
                         "right", (-1.2, 27.0, -12.0), (-SB.ax, SB.palm - 1.4, -0.6))
    c.key(0, stand)
    c.key(0.55, reach_out, E_OUT)
    c.key(0.8, P_(reach_out, {"right_fingers": [-24, 0, 0], "right_index": [-20, 0, 0], "jaw": [20, 0, 0]}), E_SNAP)
    c.key(1.25, P_(reach_out, {"right_fingers": [-30, 0, 0], "right_index": [-24, 0, 0], "jaw": [10, 0, 0]}), E_IO)
    c.key(1.6, stand, E_IO)
    c.layer("head", lambda t: [3 * math.sin(t * 50) if 0.8 < t < 1.3 else 0, 4 * math.sin(t * 37) if 0.8 < t < 1.3 else 0,
                               3 * math.sin(t * 61) if 0.8 < t < 1.3 else 0])
    c.layer("flap_face", lambda t: [20 * math.sin(t * 40) if 0.8 < t < 1.3 else 0, 0, 0])
    c.layer("@head", lambda t: [0.15 * math.sin(t * 44) if 0.8 < t < 1.3 else 0, 0, 0])
    c.layer("%head", lambda t: [0.05 * math.sin(t * 45) if 0.8 < t < 1.3 else 0, -0.04 * math.sin(t * 52) if 0.8 < t < 1.3 else 0, 0])

    # slough (1.2): he shrugs hard -- the sheets of his shoulders and back slide off (0.5) and fall; new skin glistens.
    c = clip("slough", 1.2)
    shrug = P_(stand, {"right_arm": [-10, 0, 26], "left_arm": [-10, 0, -26], "body": [-6, 0, 0], "head": [-22, 0, 0], "jaw": [20, 0, 0],
                       "@root": [0, 0.6, 0]})
    drop = P_(stand, {"body": [20, 0, 0], "head": [-10, 0, 0], "flap_shoulder_r": [0, 0, -50], "@flap_shoulder_r": [-1, -9, 0],
                      "flap_shoulder_l": [0, 0, 50], "@flap_shoulder_l": [1, -9, 0], "flap_back": [-50, 0, 0], "@flap_back": [0, -8, 2],
                      "skin_flaps": [0, 0, 0], "@skin_flaps": [0, -2, 0]})
    c.key(0, stand)
    c.key(0.35, shrug, E_OUT)
    c.key(0.5, P_(shrug, {"@skin_flaps": [0, -1.0, 0], "@flap_shoulder_r": [0, -2, 0], "@flap_shoulder_l": [0, -2, 0]}), E_SNAP)
    c.key(0.8, P_(drop, {"%flap_shoulder_r": [1, 1, 1], "%flap_shoulder_l": [1, 1, 1], "%flap_back": [1, 1, 1]}), E_IN)
    c.key(1.0, P_(drop, {"@flap_shoulder_r": [-1, -14, 0], "@flap_shoulder_l": [1, -14, 0], "@flap_back": [0, -12, 2],
                         "%flap_shoulder_r": [0.01, 0.01, 0.01], "%flap_shoulder_l": [0.01, 0.01, 0.01], "%flap_back": [0.01, 0.01, 0.01]}), E_IO)
    c.key(1.2, P_(stand, {"%flap_shoulder_r": [1, 1, 1], "%flap_shoulder_l": [1, 1, 1], "%flap_back": [1, 1, 1]}))

    # lunge (0.9): a sudden grab with both hands, too far and too fast for those arms (the hit at 0.4).
    c = clip("lunge", 0.9)
    coil = P_(stand, S_CLAW_R, S_CLAW_L, {"body": [24, 0, 0], "head": [-26, 0, 0], "@root": [0, -1.8, 1.0], "right_arm": [30, 0, 20],
                                          "left_arm": [30, 0, -20], "right_leg": [-30, 0, 0], "right_shin": [44, 0, 0], "left_leg": [10, 0, 0],
                                          "left_shin": [30, 0, 0]})
    grab = P_(S_CLAW_R, S_CLAW_L, {"body": [34, 0, 0], "head": [-34, 0, 0], "jaw": [38, 0, 0], "@root": [0, -0.8, -6.0],
                                   "right_arm": [-92, 10, 10], "left_arm": [-92, -10, -10], "right_forearm": [-4, 0, 0], "left_forearm": [-4, 0, 0],
                                   "right_leg": [-50, 0, 0], "right_shin": [30, 0, 0], "left_leg": [30, 0, 0]})
    c.key(0, stand)
    c.key(0.25, coil, E_OUT)
    c.key(0.4, grab, E_SNAP)
    c.key(0.6, P_(grab, {"right_fingers": [-90, 0, 0], "left_fingers": [-90, 0, 0], "right_index": [-90, 0, 0], "left_index": [-90, 0, 0]}), E_OUT)
    c.key(0.9, stand, E_IO)
    sway(c, 2.2, 0.6)

    for cl in clips:
        cl.done(step=0.05)
    return f


# =====================================================================================================
# Output
# =====================================================================================================

RIGS = {"alpha_vampire": build_vampire, "alpha_werewolf": build_werewolf, "alpha_shapeshifter": build_shapeshifter}
ANIMS = {"alpha_vampire": vampire_anims, "alpha_werewolf": werewolf_anims, "alpha_shapeshifter": shapeshifter_anims}


def model(name):
    """The built model, its texture and glowmask."""
    return build_model(name, RIGS[name])


def generate():
    for name in RIGS:
        m, t, g = model(name)
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        write_compact(ANIMS[name](m.rig), ANIM + name + ".animation.json")


if __name__ == "__main__":
    generate()
