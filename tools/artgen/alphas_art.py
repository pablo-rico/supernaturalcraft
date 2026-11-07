"""The three Alphas (v0.39): the first vampire, the first werewolf and the first shapeshifter, Eve's eldest -- GeckoLib rigs,
textures + glowmasks and clips. Names (ids, clips, bones, timings) are the contract in `entity/alpha/AlphaAssets.java`;
`AlphaAssetsTest` checks them. Each keeps the clips its kind's code already triggers and adds its own.

  alpha_vampire       tall, gaunt and old (33 px to the crown): grey-pale skin drawn over a skull -- hollow temples and
                      cheeks under sharp cheekbones, deep sockets, red eyes (glowmask), a hooked nose, a thin mouth turned
                      down; black hair slicked back from a widow's peak, silver at the temples; long pointed ears. An old
                      black velvet frock coat to mid-calf (fronts `coat_front_r/l`, split tails `coat_tail`) with a violet
                      sheen, edged in tarnished gold braid, frogs across the lapels, deep braided cuffs, oxblood lining; a
                      high collar; an oxblood silk damask waistcoat with a gold watch chain, an ivory stock and cravat with
                      a garnet pin, lace at the wrists, striped trousers and riding boots. Long fingers with dark nails.
                      `fangs` (toggled): the jaw unhinged into a taut-lipped oval, the first row (two long canines, the
                      small teeth) on its rim, and on `fang_row` the second row, a ring of needles sliding down behind it.
  alpha_werewolf      a hulking silver wolf-man (much heavier than the legacy werewolf), shaped rather than boxed: a barrel
                      chest over a hard waist, pectorals, lats flaring under the arms, a hump of trapezius, great round
                      shoulders; a ruff of long fur in pointed locks round the neck and over the shoulders (`mane`), dark
                      guard hair down the spine (`mane_back`); pale cream throat and bib, charcoal saddle, near-black
                      points (hands, hocks, paws, the tail's tip); old pale scars. A long tapering muzzle with the stop,
                      black lips and nose leather, a `jaw` on yellowed fangs, swept-back cheek ruffs, tall splayed ears
                      (the left torn), slanted amber eyes in dark sockets (glowmask). Digitigrade legs (thigh, shank, hock,
                      paw), knuckled hands with hooked claws, a bushy tail. No human form.
  alpha_shapeshifter  gaunt and naked but for rags (32 px): waxy, bloodless skin over every rib, the collarbones, the hip
                      bones, the shoulder blades and the spine, and it is coming off him -- sheets hang from the nape, the
                      breast, the belly, the shoulders, the back and the forearms (`skin_flaps` and its children,
                      `flap_face`, `flap_arm_*`), torn into tongues at the edge, wet underneath; the meat under them dark
                      and fibrous with threads of slime; the forearms skinned, the skin rolled down round the wrists
                      (`wrist_skin_*`). Lidless eyes over sagging raw lids with a silver flare (glowmask), the nose all but
                      gone, no lips: bare gums and long uneven teeth clenched in a rictus that the `jaw` drops open; his
                      left cheek flayed to the back teeth, its skin hanging off it (`flap_face`); a few long lank hairs.

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
COAT = Ramp("#050308", "#0c0910", "#151019", "#1f1725", "#2b2033", "#3d2d48")
BRAID = Ramp("#2e2008", "#4e3810", "#73561c", "#97752c", "#b99645", "#d9bb6e")
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
    "HhhhhhhhhhhhhhhH",
    "HHssshhhhhhsssHH",
    "HtssssshhssssstH",
    "tsssssssssssssst",
    "tbbbbbssssbbbbbt",
    "tkKKKksssskKKKkt",
    "tkRQRksnnskRQRkt",
    "tvkkkvsnnsvkkkvt",
    "sccccsnnnnsccccs",
    "VvvssssNNssssvvV",
    "VVvssssssssssvVV",
    "VVvssmmmmmmssvVV",
    "jVvsmssllssmsvVj",
    "jjvvssCCCCssvvjj",
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
    """Oxblood silk brocade: a fine lattice woven in a darker thread, a small flower at each crossing, a sheen down the
    light side."""
    def m(f, x, y, w, h):
        v = 2.9 - 0.6 * y / max(1, h) + (fbm(x * 0.5, y * 0.5, seed, 64, 64, 1, 4.0) - 0.5) * 0.6
        if (x + y) % 6 == 0 or (x - y) % 6 == 0:
            v -= 0.7                                    # the lattice
        elif (x % 6 == 3 and y % 6 == 0) or (x % 6 == 0 and y % 6 == 3):
            v += 0.9                                    # a woven flower in each diamond
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
    dk = lambda col, a: (lambda f, x, y, w, h: mix(sk(f, x, y, w, h), hexc(col), a))
    legend = {"h": hr, "s": sk, "H": lambda f, x, y, w, h: V_SILVER[3 + (x + y) % 2], "b": V_HAIR[1], "Q": V_HOT, "R": V_RED,
              "t": dk("#2a2430", 0.32), "k": dk("#1c1520", 0.5), "K": dk("#100b12", 0.74), "v": dk("#2c2432", 0.34),
              "V": dk("#211a28", 0.52), "c": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.16), "n": V_SKIN[4], "N": V_SKIN[1],
              "m": hexc("#22090f"), "l": mix(WAIST[3], V_SKIN[2], 0.55), "C": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.1),
              "j": lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.82)}
    sides = H.head_sides(sk, hr, top=5, back=15, temple=3, sideburn=11)
    LG.head_cube(m, "head", b.neck, ALPHA_VAMPIRE_FACE, legend, sides, glow=eye_glow)
    # A long, hooked nose and the sharp bridge of it; pointed ears swept back.
    m.cube("head", (-0.6, b.neck + 2.5, -4.65), (1.2, 2.2, 0.7),
           lambda f, x, y, w, h: V_SKIN[4] if f == "north" and y < 2 else (V_SKIN[0] if f == "down" else V_SKIN[2]), density=4, tag="nose")
    for s in (-1, 1):
        def ear(f, x, y, w, h, s=s):
            # A long, narrow leaf of an ear rising to a point at the back; the hollow of it in shadow.
            if f in ("east", "west"):
                fx = x if f == "west" else w - 1 - x  # 0 at the front
                u, v = fx / max(1, w - 1), y / max(1, h - 1)
                if u < 0.65 * (1 - v) - 0.05 or (v > 0.8 and u > 0.8):
                    return CLEAR
                inner = (f == "east") == (s > 0)
                c = sk(f, x, y, w, h)
                if inner and 0.25 < v < 0.85 and 0.35 < u < 0.8:
                    return mix(c, hexc("#2a1d2a"), 0.35)
                return shade(c, 1.05 if not inner else 0.95)
            if f in ("north", "south", "up"):
                return CLEAR if f == "up" else shade(sk(f, x, y, w, h), 0.78)
            return shade(sk(f, x, y, w, h), 0.7)
        m.cube("head", (s * 4.05 - 0.2, b.neck + 2.6, -0.6), (0.4, 3.6, 2.2), ear, density=4, tag="ear",
               rotation=(-14, 18 * s, 6 * s), pivot=(s * 4.0, b.neck + 3.4, 0.2))
    # Brows: heavy, pulled down at the middle.
    for s in (-1, 1):
        m.cube("brows", (s * 2.1 - 1.3, b.neck + 4.75, -4.12), (2.6, 0.35, 0.25),
               lambda f, x, y, w, h: V_SILVER[1] if x in (0, w - 1) else V_HAIR[2], density=4, tag="brow",
               rotation=(0, 0, -14 * s), pivot=(s * 0.9, b.neck + 4.9, -4.1))
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
        # The jaw unhinged: an oval stretched wide past the corners of the mouth, its thin lips pulled taut and pale; the
        # gums along the top; the throat dark at the middle.
        cx, cy = (w - 1) / 2, (h - 1) * 0.42
        d = ((x - cx) / (w / 2)) ** 2 + ((y - cy) / (h * 0.62)) ** 2
        if d > 1.0:
            return CLEAR
        if d > 0.74:
            return mix(WAIST[3], V_SKIN[2], 0.5) if y > cy else mix(WAIST[2], V_SKIN[1], 0.45)
        if y <= 1 or (y == 2 and d > 0.4):
            return MOUTH[4] if (x + y) % 3 else MOUTH[3]
        if y == 2 and x % 2 == 0:
            return FANG[3]                          # the small front teeth of the first row
        return MOUTH[0] if d < 0.3 else MOUTH[1 + (d > 0.55)]
    m.cube("fangs", (-2.5, b.neck + 0.05, -4.08), (5.0, 2.8, 0), maw, lambda f, x, y, w, h, c: shade(c, 0.55) if c[3] else None,
           faces=("north",), density=4, tag="maw")
    # The first row: two long canines at the corners, two lower ones.
    for s in (-1, 1):
        m.cube("fangs", (s * 1.6 - 0.15, b.neck + 1.45, -4.2), (0.3, 1.25, 0.3),
               lambda f, x_, y, w, h: FANG[5] if y < h - 2 else (FANG[3] if y < h - 1 else FANG[1]), density=4, tag="canine")
        m.cube("fangs", (s * 1.25 - 0.12, b.neck + 0.55, -4.18), (0.25, 0.85, 0.25), lambda f, x_, y, w, h: FANG[1 + y], density=4,
               tag="lower_fang")
    # The second row: a ring of needles behind the first, sliding down from the gums.
    for i, x in enumerate((-1.95, -1.6, -1.2, -0.85, -0.45, -0.1, 0.25, 0.6, 1.0, 1.35, 1.7)):
        tall = 0.35 + 0.4 * h01("needle", i) + (0.3 if i in (2, 8) else 0)
        m.cube("fang_row", (x, b.neck + 2.55 - tall, -4.24), (0.2, tall, 0.2),
               lambda f, x_, y, w, h, i=i: FANG[4 - (i % 2)] if y < h - 1 else FANG[1], density=4, tag="needle")
    for i, x in enumerate((-1.6, -1.05, -0.5, 0.05, 0.6, 1.15)):
        m.cube("fangs", (x, b.neck + 0.45, -4.22), (0.2, 0.35 + 0.25 * h01("lneedle", i), 0.2), lambda f, x_, y, w, h: FANG[3 - y],
               density=4, tag="lower_needle")

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
                return BRAID[2 + (y % 3 == 0)] if x in (2, w - 3) else coat(f, x, y, w, h)
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
            if f == "north":
                if x == (0 if s < 0 else w - 1):
                    return BRAID[3 + (y % 2)]
                if y in (3, 6, 9):
                    return BRAID[4] if x == (w - 1 if s < 0 else 0) else BRAID[3 - (x % 2)]
                if y in (4, 7, 10):
                    return COAT[0]                      # the braid's shadow
            return c
        m.cube("coat", (3.0 * s - 1.0, b.neck - 8.0, -b.depth / 2 - 0.85), (2.0, 7.2, 0.4), lapel, density=2, tag="lapel",
               rotation=(3, 0, 9 * s), pivot=(2.6 * s, b.neck - 8.0, -b.depth / 2 - 0.7))
        m.cube("coat", (s * (b.tw + 0.2) - 1.3, b.neck - 0.9, -b.depth / 2 - 0.4), (2.6, 0.9, b.depth + 0.8),
               lambda f, x, y, w, h: tone(COAT, 4.0 if f == "up" else 2.8), density=2, tag="shoulder")
    # The collar, turned up high round the back and the sides of his neck, its lining a dull red.
    def collar(f, x, y, w, h):
        if f == "up":
            return BRAID[3]
        if f in ("south", "east", "west") and y == 0:
            return BRAID[2 + (x % 2)]
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
                return tone(WAIST, 1.6 + 0.6 * ((x + y) % 3 == 0))   # the lining
            c = coat(f, x, y + 20, w, h + 20)
            if f == "north" and (x == w - 1 if s < 0 else x == 0):
                return BRAID[2 + (y % 3 == 0)]
            if f == "north" and y == h - 1:
                return BRAID[2]
            if f == "north" and y == 3 and 1 <= (w - 1 - x if s < 0 else x) <= w - 2:
                return COAT[0]                      # the pocket flap's seam
            if f == "north" and y == 2 and (w - 1 - x if s < 0 else x) == w // 2:
                return BRAID[4]                     # its button
            return c
        xa, xb = (-b.tw - 0.6, -0.2) if s < 0 else (0.2, b.tw + 0.6)
        m.cube(bone, (xa, hem, -b.depth / 2 - 0.7), (xb - xa, L, 0.45), panel, density=2, tag="coat_front",
               rotation=(-5, 0, 4 * s), pivot=(1.9 * s, b.hip + 0.4, -b.depth / 2 - 0.4))
        xs = -b.tw - 0.65 if s < 0 else b.tw + 0.2
        m.cube(bone, (xs, hem, -b.depth / 2 - 0.45), (0.45, L, b.depth / 2 + 0.45), none_on(("up",), coat), density=2, tag="coat_side",
               rotation=(0, 0, 4 * s), pivot=(xs, b.hip, 0))
    def coat_back(f, x, y, w, h):
        if f == "north":
            return COAT[0]
        mid = (w - 1) / 2
        if f == "south" and y == 1 and abs(abs(x - mid) - 1.5) < 0.6:
            return BRAID[4]                         # the two buttons at the small of the back
        return coat(f, x, y + 16, w, h + 16)
    m.cube("coat_tail", (-b.tw - 0.6, b.hip - 3.0, b.depth / 2 + 0.3), (2 * b.tw + 1.2, 3.6, 0.45), coat_back, density=2, tag="coat_back")
    for bone, s in (("coat_tail_r", -1), ("coat_tail_l", 1)):
        def tail(f, x, y, w, h, s=s):
            if f == "north":
                return tone(WAIST, 1.4 + 0.6 * ((x + y) % 3 == 0))   # the lining
            if f == "south" and (x == (w - 1 if s < 0 else 0)) and y > 1:
                return COAT[0]                      # the vent
            if f == "south" and y == h - 1:
                return BRAID[2]
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
        def cuff(f, x, y, w, h, s=s):
            if y == 0:
                return BRAID[3 + (x % 2)]
            if y == 1:
                return BRAID[1]
            outer = f == ("west" if s < 0 else "east")
            if outer and x == w // 2 and y in (2, 4):
                return BRAID[4]
            return tone(COAT, 4.2 - y * 0.35)
        m.cube(f"{side}_forearm", (cx - 1.5, b.wrist + 0.2, -1.5), (3.0, 2.6, 3.0), none_on(("up", "down"), cuff), inflate=0.55, density=2,
               tag="cuff")
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
# Nine-step pelts: the silver body, the pale cream-silver of the throat, chest and ruff, the charcoal saddle down the
# back, the near-black points (hands, hocks, paws, the tail's tip).
SILVER = Ramp("#121317", "#1f2126", "#2f3238", "#41454c", "#575b63", "#6f747c", "#8a8f97", "#a7acb3", "#c6cacf")
SILVER_PALE = Ramp("#38393b", "#4d4f51", "#636567", "#7b7d7e", "#939594", "#aaaba8", "#bebfbb", "#d0d0cb", "#e0dfd9")
SADDLE = Ramp("#07080a", "#0e0f12", "#16181b", "#202226", "#2b2e33", "#383c42", "#474c53", "#595e66", "#6e737b")
POINT = Ramp("#070708", "#0e0e10", "#161618", "#1f1f22", "#29292d", "#343439", "#414147", "#505057", "#616169")
SCAR = hexc("#b9a3a0")
W_CLAW = Ramp("#050505", "#0f0e0d", "#1d1b19", "#302d2a", "#4a4641", "#6e6962")
GUMS = LG.GUMS
LIP = hexc("#0b0908")
TOOTH = Ramp("#5f5644", "#7f7660", "#a09679", "#bdb393", "#d6cdab", "#ebe4c8")
AMBER, AMBER_HOT, AMBER_DARK = hexc("#ffad1f"), hexc("#ffe08a"), hexc("#b0560a")
W_HIP = 14.4


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


def pelt(seed, ramp=SILVER, base=4.6, grad=1.3, lock=(3, 5), vary=1.0, tips=0.12, scars=0.0, blend=None):
    """Shaggy fur in hanging locks: each lock two texels wide and 3-5 long, lit at its root and shadowed at its tip where
    the next lock overlaps it, staggered column to column; lighter towards the top of a part, darker underneath. `blend`
    (f, x, y, w, h) -> (ramp, amount) shifts a region towards another pelt (a pale throat, dark points); `scars` paints
    old pale claw scars in threes."""
    def m(f, x, y, w, h):
        r = ramp
        if f in ("up", "down"):
            n = fbm(x * 0.8 + len(f) * 7, y * 0.8, seed, 64, 64, 2, 5.0)
            v = base + (n - 0.5) * vary + (0.7 if f == "up" else -1.8)
            if h01("sp", seed, f, x, y) < 0.16:
                v += 0.9
            c = tone(r, v)
        else:
            # Locks three texels wide and 4-7 long, staggered column to column: the middle strand lit, the two outer
            # ones cut away into a point at the tip, where the lock below shows through in shadow.
            col = x // 3
            cx = x % 3
            ln = lock[0] + 1 + int(h01("ln", seed, f, col) * (lock[1] - lock[0] + 2))
            off = int(h01("of", seed, f, col) * ln)
            k = (y + off) % ln
            v = (base + (h01("cv", seed, f, col, (y + off) // ln) - 0.5) * 0.7
                 + (fbm(x * 0.5 + len(f) * 9, y * 0.3, seed + 3, 64, 64, 2, 6.0) - 0.5) * vary * 0.9
                 - grad * (y / max(1, h - 1) - 0.4))
            if cx == 1:
                v += 0.3
            if k == 0:
                v += 0.25
            if cx != 1 and k == ln - 1:
                v -= 1.0
            elif cx != 1 and k == ln - 2:
                v -= 0.35
            if f == "south":
                v -= 0.25
            c = tone(r, v)
            if k == 0 and h01("tip", seed, f, x, y) < tips:
                c = mix(c, hexc("#eef0f2"), 0.3)
            if scars and f in ("north", "east", "west"):
                s = (x + y * 0.7 + h01("sc", seed, f) * 9) % 10
                if s < 0.9 and fbm(x * 0.4 + 9, y * 0.4, seed + 31, 64, 64, 1, 3.0) > 1 - scars:
                    c = mix(SCAR, c, 0.3)
        if blend is not None:
            got = blend(f, x, y, w, h)
            if got:
                r2, a = got
                if a > 0:
                    c2 = pelt_cache(seed, r2, base, grad, lock, vary)(f, x, y, w, h)
                    c = mix(c, c2, min(1.0, a))
        return c
    return m


_PELTS = {}


def pelt_cache(seed, ramp, base, grad, lock, vary):
    key = (seed, id(ramp), base, grad, lock, vary)
    if key not in _PELTS:
        _PELTS[key] = pelt(seed, ramp, base, grad, lock, vary, tips=0.0)
    return _PELTS[key]


def fringe(mat, depth=3, period=3, seed=1, faces=("north", "south", "east", "west")):
    """Cuts the lower rows of the side faces into pointed tufts (clear between the points): `depth` and `period` in
    texels at density 2 (doubled, as the tufts would otherwise vanish at a distance)."""
    depth, period = depth * 2, period + 2

    def m(f, x, y, w, h):
        dd = min(depth, max(1, h * 2 // 3))
        if f in faces and y >= h - dd:
            r = h - 1 - y                                   # 0 on the bottom row
            p = period + (1 if h01("fp", seed, f, x // period) < 0.3 else 0)
            off = int(h01("fo", seed, f) * p)
            d = abs(((x + off) % p) - (p - 1) / 2)
            if d > (r + 0.5) * (p / 2) / dd + 0.25 * h01("fj", seed, f, x):
                return CLEAR
        return mat(f, x, y, w, h)
    return m


def fade_down(r2, start=0.5, power=1.0):
    """A blend that darkens a part from `start` of its height down (the points)."""
    def b(f, x, y, w, h):
        if f == "up":
            return None
        if f == "down":
            return (r2, 1.0)
        t = y / max(1, h - 1)
        return (r2, max(0.0, (t - start) / max(0.01, 1 - start)) ** power) if t > start else None
    return b


def front_pale(width=0.5, faces=("north",), r2=SILVER_PALE, amount=1.0):
    def b(f, x, y, w, h):
        if f in faces and abs(x - (w - 1) / 2) < w * width / 2:
            return (r2, amount)
        if f == "down":
            return (r2, amount * 0.8)
        return None
    return b


def build_werewolf(m0):
    m = Scaled(m0, WK)
    m.bone("root", (0, 0, 0))
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

    _wolf_torso(m)
    _wolf_ruff(m)
    _wolf_head(m)
    _wolf_arms(m)
    _wolf_legs(m)


def _saddle_back(f, x, y, w, h):
    """The charcoal saddle: the whole back, and the top of a part."""
    if f == "south":
        return (SADDLE, 0.85)
    if f == "up":
        return (SADDLE, 0.7)
    return None


def _wolf_torso(m):
    # The pelvis and a narrow, hard waist under the barrel of the chest.
    m.cube("body", (-3.6, 12.4, -2.2), (7.2, 3.4, 4.8), pelt(39201, base=4.2, blend=_saddle_back), density=2, tag="pelvis")

    def abs_m(f, x, y, w, h):
        c = pelt(39202, base=4.4, blend=_saddle_back)(f, x, y, w, h)
        if f == "north":
            mid = (w - 1) / 2
            p = pelt(39203, SILVER_PALE, base=4.4, grad=0.8)(f, x, y, w, h)
            if abs(x - mid) < w * 0.36:
                c = p
                if abs(x - mid) < 0.6 or y in (h // 3, 2 * h // 3):
                    c = shade(c, 0.78)                      # the lines of the belly's muscle under the fur
            elif abs(x - mid) < w * 0.45:
                c = mix(c, p, 0.5)
        return c
    m.cube("body", (-3.3, 15.0, -2.4), (6.6, 4.4, 4.6), abs_m, density=2, tag="waist")
    # Fur over the hips, hanging in points over the tops of the thighs.
    m.cube("body", (-4.0, 11.2, -2.6), (8.0, 2.6, 5.4), fringe(none_on(("up",), pelt(39204, base=4.4, blend=_saddle_back)), 3, 3, 39204),
           density=2, tag="hip_fur")

    # The barrel of the ribs, the pectorals, the lats flaring out under the arms, the hump of the trapezius.
    m.cube("chest", (-5.0, 18.4, -3.2), (10.0, 6.4, 6.6),
           pelt(39205, base=4.5, scars=0.55, blend=lambda f, x, y, w, h: _saddle_back(f, x, y, w, h) or
                (front_pale(0.4, amount=0.75)(f, x, y, w, h))), density=2, tag="ribs")
    for s in (-1, 1):
        def pec(f, x, y, w, h, s=s):
            inner = (lambda f_, x_, y_, w_, h_: (SILVER_PALE, max(0.0, 0.8 - 1.2 * (x_ if s > 0 else w_ - 1 - x_) / max(1, w_)))
                     if f_ == "north" else None)
            c = pelt(39206 + (s > 0), SILVER, base=5.0, grad=1.6, scars=0.45 if s > 0 else 0, blend=inner)(f, x, y, w, h)
            if f == "north" and y == h - 1:
                return shade(c, 0.7)                        # the shadow under the muscle
            if f == "north" and (x == 0 if s > 0 else x == w - 1):
                return shade(c, 0.8)                        # the cleft down the sternum
            return c
        m.cube("chest", ((0.15 if s > 0 else -5.0), 20.6, -4.0), (4.85, 3.9, 1.2), pec, density=2, tag="pec",
               rotation=(8, 0, -4 * s), pivot=(2.5 * s, 24.5, -3.4))
        m.cube("chest", ((4.4 if s > 0 else -6.0), 18.6, -2.4), (1.6, 5.0, 4.8),
               pelt(39208 + (s > 0), base=4.0, blend=_saddle_back), density=2, tag="lat",
               rotation=(0, 0, 14 * s), pivot=(5.2 * s, 23.6, 0))
    m.cube("chest", (-4.6, 19.0, 2.6), (9.2, 5.6, 1.4), pelt(39210, SADDLE, base=5.0), density=2, tag="back_muscle")
    m.cube("chest", (-4.4, 23.6, -1.4), (8.8, 2.8, 5.2), pelt(39211, SADDLE, base=5.2), density=2, tag="traps",
           rotation=(-10, 0, 0), pivot=(0, 24.0, 1.0))
    m.cube("chest", (-2.8, 25.6, -0.6), (5.6, 1.8, 4.0), none_on(("down",), pelt(39212, SADDLE, base=5.4)), density=2, tag="hump")


def _wolf_ruff(m):
    """The silver ruff: a mantle of long fur round the neck and over the shoulders, broken into pointed tufts that fan out
    and down (all on `mane`, so it lifts and flies with it); a ridge of dark guard hair down the spine (`mane_back`)."""
    pale = lambda seed, base=5.2: pelt(seed, SILVER_PALE, base=base, grad=1.6, lock=(3, 6))
    silver = lambda seed, base=5.0: pelt(seed, SILVER, base=base, grad=1.4, lock=(3, 6), blend=_saddle_back)
    m.cube("mane", (-4.8, 22.8, -3.8), (9.6, 3.8, 7.8), fringe(none_on(("down",), silver(39220, 5.2)), 2, 3, 39220), density=2,
           tag="ruff_core")
    m.cube("mane", (-3.8, 26.2, -2.6), (7.6, 1.4, 5.8), none_on(("down",), pelt(39221, SADDLE, base=5.6)), density=2, tag="ruff_top")
    # The bib: three long pale locks over the chest, the middle one longest.
    for i, (x, ln, rz) in enumerate(((-2.5, 4.6, 10), (0.0, 5.6, 0), (2.5, 4.6, -10))):
        m.cube("mane", (x - 1.7, 26.0 - ln - 0.6, -4.7), (3.4, ln, 1.4), fringe(pale(39222 + i, 5.4), 3, 3, 39222 + i), density=2,
               tag="bib", rotation=(14, 0, rz), pivot=(x, 26.0, -4.0))
    # Over the shoulders: tufts fanning outwards and down, two a side.
    for s in (-1, 1):
        for j, (z, ln, rz, rx) in enumerate(((-2.2, 4.8, -26, 6), (1.2, 4.4, -18, -8))):
            m.cube("mane", (s * 4.7 - 1.3, 26.4 - ln, z - 1.9), (2.6, ln, 3.8), fringe(silver(39226 + j + 2 * (s > 0), 5.0), 3, 3, 39226 + j),
                   density=2, tag="shoulder_tuft", rotation=(rx, 0, rz * s), pivot=(s * 4.4, 26.4, z))
        # The cheek of the ruff, between the shoulder and the throat.
        m.cube("mane", (s * 3.4 - 1.2, 22.0, -4.4), (2.4, 4.4, 2.6), fringe(pale(39230 + (s > 0), 5.0), 3, 3, 39230 + (s > 0)),
               density=2, tag="ruff_side", rotation=(10, 0, -14 * s), pivot=(s * 3.4, 26.4, -3.2))
    # Behind: the ruff falls down the back in three long points.
    for i, (x, ln) in enumerate(((-2.6, 5.0), (0.0, 6.0), (2.6, 5.0))):
        m.cube("mane", (x - 1.6, 26.2 - ln, 2.6), (3.2, ln, 1.6), fringe(pelt(39232 + i, SADDLE, base=5.4, lock=(3, 6)), 3, 3, 39232 + i),
               density=2, tag="ruff_back", rotation=(16, 0, -8 * (i - 1)), pivot=(x, 26.2, 2.8))
    # The dorsal ridge: dark guard hair standing up along the spine, down to the tail.
    m.cube("mane_back", (-1.2, 15.0, 3.4), (2.4, 9.4, 1.4), fringe(pelt(39235, SADDLE, base=5.2, lock=(2, 4)), 2, 2, 39235), density=2,
           tag="spine_ridge", rotation=(-4, 0, 0), pivot=(0, 25, 3.4))
    for i, (x, y) in enumerate(((-2.2, 22.2), (2.2, 21.6), (0, 19.0), (0, 16.4))):
        m.cube("mane_back", (x - 1.3, y - 4.0, 3.6), (2.6, 4.2, 1.0), fringe(pelt(39236 + i, SADDLE, base=5.6), 2, 3, 39236 + i), density=2,
               tag="back_tuft", rotation=(16, 0, 0), pivot=(x, y, 3.8))


WOLF_EYE = [            # 9 x 5, x = 0 at the eye's inner corner; '.' clear
    "....kkkk.",
    "..kkAHHAk",
    "kkAAPAAAk",
    ".kkDDAAk.",
    "...kkkk..",
]
WOLF_SOCKET = [         # 6 x 3 (density 2), the shadow round it
    "..ssss",
    "ssssss",
    ".ssss.",
]


def _wolf_head(m):
    hy = 26.5
    face = lambda seed, base=4.8: pelt(seed, SILVER, base=base, grad=0.8, lock=(2, 3), vary=0.8)
    pale = lambda seed, base=5.6: pelt(seed, SILVER_PALE, base=base, grad=0.9, lock=(2, 3), vary=0.7)
    dark = lambda seed, base=5.0: pelt(seed, SADDLE, base=base, grad=0.6, lock=(2, 3), vary=0.7)

    # The cranium, broad and flat-topped; a dark stripe from between the eyes back over the crown.
    def cranium(f, x, y, w, h):
        mid = (w - 1) / 2
        if f == "up" and abs(x - mid) < w * 0.22:
            return dark(39240)(f, x, y, w, h)
        if f == "north":
            if abs(x - mid) < w * 0.16:
                return dark(39240)(f, x, y, w, h)
            if y >= h * 0.55:
                return pale(39241, 5.0)(f, x, y, w, h)     # the pale mask under the eyes
        if f in ("east", "west") and y >= h * 0.6:
            return pale(39241, 5.0)(f, x, y, w, h)
        return face(39242)(f, x, y, w, h)
    m.cube("head", (-3.4, hy + 0.8, -6.6), (6.8, 5.2, 6.4), cranium, density=2, tag="cranium")
    # The crown rounded off and the back of the skull falling to the neck.
    m.cube("head", (-2.6, hy + 5.8, -5.6), (5.2, 0.8, 5.0), none_on(("down",), dark(39247, 5.4)), density=2, tag="crown")
    m.cube("head", (-3.0, hy + 1.4, -0.4), (6.0, 4.0, 1.2), dark(39248, 5.0), density=2, tag="occiput")
    # Broad cheeks, the fur on them swept back in points.
    m.cube("head", (-4.0, hy + 0.0, -6.0), (8.0, 3.2, 4.4), fringe(none_on(("up",), pale(39243, 5.2)), 2, 2, 39243,
                                                                    faces=("north", "east", "west")), density=2, tag="cheeks")
    # The brow ridge, heavy and low over the eyes; a pale fleck over each eye, the wolf's mask.
    def brow(f, x, y, w, h):
        if f == "down":
            return SADDLE[1]
        if f == "north":
            u = x / max(1, w - 1)
            if 0.12 < u < 0.32 or 0.68 < u < 0.88:
                return SILVER_PALE[7] if y == 0 else SILVER_PALE[5]
            if 0.42 < u < 0.58:
                return SADDLE[3]
        return dark(39244, 5.4)(f, x, y, w, h)
    m.cube("head", (-3.4, hy + 4.9, -7.2), (6.8, 1.1, 1.6), brow, density=2, tag="brow", rotation=(10, 0, 0), pivot=(0, hy + 6.0, -5.6))
    # The stop: the forehead running down into the long bridge of the snout.
    def bridge(f, x, y, w, h):
        if f == "up":
            mid = (w - 1) / 2
            if abs(x - mid) < w * 0.3:
                return dark(39245, 5.2)(f, x, y, w, h)
        return face(39246, 4.8)(f, x, y, w, h)
    m.cube("head", (-1.8, hy + 2.8, -9.8), (3.6, 2.0, 3.6), bridge, density=2, tag="stop", rotation=(14, 0, 0),
           pivot=(0, hy + 4.8, -6.4))

    # The muzzle: long and tapering, grey on top, pale cream along the jaw line, black lips; an old scar across the bridge.
    def muzzle(seed, front):
        def mat(f, x, y, w, h):
            mid = (w - 1) / 2
            if f == "down":
                return GUMS[2]
            if f == "up":
                c = dark(seed, 5.2)(f, x, y, w, h) if abs(x - mid) < w * 0.32 else face(seed + 1, 5.0)(f, x, y, w, h)
                if not front and abs((x - mid) - (y - h / 2) * 0.8) < 0.6 and 1 <= y <= h - 2:
                    return SCAR
                return c
            if f in ("east", "west"):
                if y == h - 1:
                    return LIP
                if y >= h * 0.5:
                    c = pale(seed + 2, 5.6)(f, x, y, w, h)
                    return shade(c, 0.86) if (x + y) % 4 == 0 and y < h - 2 else c   # whisker pits
                return face(seed + 3, 4.8)(f, x, y, w, h)
            if f == "north":
                if y == h - 1:
                    return LIP
                if abs(x - mid) < 0.6 and y >= h - 3:
                    return SADDLE[1]                        # the cleft of the lip
                return pale(seed + 2, 5.8)(f, x, y, w, h) if y >= h * 0.35 else face(seed + 3, 5.0)(f, x, y, w, h)
            return face(seed + 3, 4.6)(f, x, y, w, h)
        return mat
    m.cube("head", (-2.2, hy + 1.2, -10.6), (4.4, 2.8, 4.4), muzzle(39250, False), density=2, tag="muzzle")
    m.cube("head", (-1.8, hy + 1.3, -14.4), (3.6, 2.4, 4.0), muzzle(39254, True), density=2, tag="snout")
    # The nose leather: black, wet, two nostrils, a highlight.
    def nose(f, x, y, w, h):
        if f == "north":
            if y >= h - 2 and x in (1, w - 2):
                return hexc("#000000")
            if y == 0 and 1 <= x <= 3:
                return hexc("#5a5a62")
            if y == 0:
                return hexc("#2e2d33")
        if f == "up":
            return hexc("#45454c") if y == h - 1 and 1 <= x <= w - 2 else hexc("#2a292e")
        return hexc("#1a191c") if f != "down" else hexc("#050505")
    m.cube("head", (-1.15, hy + 2.85, -14.85), (2.3, 1.1, 1.4), nose, density=4, tag="nose")
    # The upper lip's black edge hanging a little along the sides; long yellowed fangs and a row of teeth.
    for s in (-1, 1):
        m.cube("head", ((1.55 if s > 0 else -2.0), hy + 0.85, -14.2), (0.45, 0.5, 7.4),
               lambda f, x, y, w, h: CLEAR if f in ("east", "west") and y == h - 1 and h01("lip", x) < 0.4 else LIP, density=4, tag="lip")
        m.cube("head", (s * 1.62 - 0.25, hy + 0.4, -13.0), (0.5, 1.2, 0.55),
               lambda f, x, y, w, h: TOOTH[5] if y < h - 2 else (TOOTH[3] if y < h - 1 else TOOTH[1]), density=4, tag="fang")
        for i, z in enumerate((-12.4, -11.4, -10.4, -9.4, -8.4)):
            m.cube("head", (s * 1.45 - 0.2, hy + 1.3 - (0.75 if i == 0 else 0.45), z), (0.4, 0.75 if i == 0 else 0.45, 0.5),
                   lambda f, x, y, w, h: TOOTH[4] if y < h - 1 else TOOTH[2], density=4, tag="tooth")

    # Eyes: slanted almonds of amber set into dark sockets either side of the stop, angled forward and out.
    def eye(s):
        def mat(f, x, y, w, h):
            xx = x if s < 0 else w - 1 - x
            ch = WOLF_EYE[min(4, y * 5 // max(1, h))][min(8, xx * 9 // max(1, w))]
            return {".": CLEAR, "k": hexc("#0a0807"), "A": AMBER, "H": AMBER_HOT, "P": hexc("#140904"), "D": AMBER_DARK}[ch]
        return mat

    def eye_glow(f, x, y, w, h, c):
        return {AMBER_HOT: hexc("#ffe7a0"), AMBER: hexc("#ffa41c"), AMBER_DARK: hexc("#a4500a")}.get(c)
    for s in (-1, 1):
        m.cube("head", (s * 2.8 - 0.9, hy + 3.7, -6.78), (1.8, 1.0, 0), eye(s), eye_glow, faces=("north",), density=4, tag="eye",
               rotation=(0, 0, -10 * s), pivot=(s * 2.8, hy + 4.2, -6.78))
        # The socket's shadow round it, and the dark tear-line running from its inner corner down the side of the muzzle.
        m.cube("head", (s * 2.8 - 1.15, hy + 3.45, -6.7), (2.3, 1.5, 0),
               lambda f, x, y, w, h, s=s: CLEAR if WOLF_SOCKET[min(2, y * 3 // max(1, h))][min(5, (x if s < 0 else w - 1 - x) * 6 // max(1, w))] == "."
               else SADDLE[1], faces=("north",),
               density=2, tag="socket", rotation=(0, 0, -10 * s), pivot=(s * 2.8, hy + 4.2, -6.7))

    # The lower jaw, long and narrow; pale fur beneath; the lower fangs; a beard of pale fur under the chin.
    def jaw(f, x, y, w, h):
        if f == "up":
            return GUMS[3] if 1 <= x <= w - 2 else GUMS[1]
        if f in ("east", "west", "north") and y == 0:
            return LIP
        return pale(39260, 5.4)(f, x, y, w, h)
    m.cube("jaw", (-1.6, hy + 0.1, -13.9), (3.2, 1.2, 7.4), jaw, density=2, tag="jaw")
    for s in (-1, 1):
        m.cube("jaw", (s * 1.05 - 0.22, hy + 1.3, -13.3), (0.45, 1.0, 0.5),
               lambda f, x, y, w, h: TOOTH[5] if y < 1 else TOOTH[3], density=4, tag="fang")
        for z in (-12.0, -10.8, -9.6, -8.4):
            m.cube("jaw", (s * 1.25 - 0.2, hy + 1.3, z), (0.4, 0.45, 0.45), solid(TOOTH[4]), density=4, tag="tooth")
    m.cube("jaw", (-1.4, hy - 1.0, -12.6), (2.8, 1.1, 5.6), fringe(none_on(("up",), pale(39261, 5.8)), 2, 2, 39261), density=2,
           tag="chin_fur")
    m.cube("tongue", (-1.1, hy + 1.25, -12.6), (2.2, 0.15, 5.4), solid(GUMS[4]), density=4, tag="tongue")

    # Ears: tall, pointed, splayed a little; the left one torn at the tip; dark backs, pale fur inside.
    for s in (-1, 1):
        bone = "right_ear" if s < 0 else "left_ear"

        def ear(i, s=s):
            def mat(f, x, y, w, h):
                if f == "north":
                    # Dark rims, pale fur, and the hollow down the middle narrowing to the tip.
                    e = min(x, w - 1 - x)
                    hollow = (w - 1) / 2 - abs(x - (w - 1) / 2) >= (w - 1) / 2 - (0.9 if i == 0 else 0.4)
                    if e == 0:
                        return dark(39268 + i, 5.6)(f, x, y, w, h)
                    if hollow and i < 2:
                        return mix(SADDLE[2], GUMS[1], 0.2) if i == 0 or y > 0 else SADDLE[3]
                    return pale(39265 + i, 5.0 - i * 0.3)(f, x, y, w, h)
                if f == "up":
                    return SADDLE[4]
                return dark(39268 + i, 5.0)(f, x, y, w, h)
            return mat
        steps = ((3.4, 1.8, 1.4, 5.6), (2.6, 1.4, 1.1, 7.2), (1.6, 1.1, 0.8, 8.4), (0.7, 0.8 if s < 0 else 0.35, 0.5, 9.4))
        for i, (wd, ht, dp, y0) in enumerate(steps):
            m.cube(bone, (2.4 * s - wd / 2, hy + y0, -3.6 - dp / 2), (wd, ht, dp), ear(i), density=4 if i else 2, tag="ear",
                   rotation=(-16, 0, 14 * s), pivot=(2.4 * s, hy + 6.0, -3.4))

    # The ruff of the face: long fur swept back from the cheeks; a pale ruff at the throat (on `mane`, so it stays put when he
    # throws the head back to howl).
    for s in (-1, 1):
        for j, (y, rz) in enumerate(((hy - 0.8, 22), (hy + 1.2, 34))):
            m.cube("neck_ruff", (s * 4.0 - 0.7, y, -5.0), (1.4, 3.0, 3.8),
                   fringe(pelt(39270 + j + 2 * (s > 0), SILVER, base=5.0, blend=front_pale(1.0, ("north", "east", "west"), amount=0.55)),
                          2, 2, 39270 + j), density=2, tag="cheek_ruff", rotation=(-6, 28 * s, -rz * s), pivot=(s * 3.8, y + 2.8, -3.6))
    m.cube("mane", (-3.0, hy - 2.8, -5.0), (6.0, 3.2, 4.8), fringe(pelt(39274, SILVER_PALE, base=4.6, grad=1.6), 3, 3, 39274), density=2,
           tag="throat_ruff")


def _wolf_arms(m):
    for side, s in SIDES:
        cx = 7.6 * s
        cf = 7.8 * s
        fur = lambda seed, base=4.4, blend=_saddle_back: pelt(seed, SILVER, base=base, blend=blend)
        # The shoulder: a great round deltoid under a fringe of ruff; the upper arm thick, the biceps bulging forward.
        m.cube(f"{side}_arm", (cx - 3.0, 20.0, -3.0), (6.0, 5.0, 6.0), fringe(fur(39280 + (s > 0), 4.8), 2, 3, 39280 + (s > 0)), density=2,
               tag="deltoid")
        m.cube(f"{side}_arm", (cx - 2.4, 23.8, -2.4), (4.8, 1.6, 4.8), none_on(("down",), pelt(39282, SADDLE, base=5.4)), density=2,
               tag="deltoid_cap")
        m.cube(f"{side}_arm", (cx - 2.3, 16.4, -2.4), (4.6, 4.6, 4.6), fur(39283 + (s > 0), 4.2), density=2, tag="upper_arm")
        m.cube(f"{side}_arm", (cx - 1.7, 17.0, -3.0), (3.4, 3.2, 0.8), pelt(39285, SILVER, base=4.8, grad=1.6), density=2, tag="biceps")
        m.cube(f"{side}_arm", (cx - 2.0, 15.6, 1.6), (4.0, 4.4, 1.4), fringe(fur(39286, 4.6), 3, 3, 39286 + (s > 0)), density=2,
               tag="triceps_tuft", rotation=(18, 0, 0), pivot=(cx, 20.0, 1.8))
        # The forearm: heavy at the elbow, tapering to the wrist and darkening to the near-black hands.
        fore = pelt(39288 + (s > 0), SILVER, base=4.2, blend=fade_down(POINT, 0.45))
        m.cube(f"{side}_forearm", (cf - 2.5, 13.2, -2.5), (5.0, 4.4, 5.0), pelt(39290 + (s > 0), SILVER, base=4.2, blend=_saddle_back),
               density=2, tag="forearm")
        m.cube(f"{side}_forearm", (cf - 2.0, 10.4, -2.0), (4.0, 3.0, 4.0), fore, density=2, tag="wrist")
        m.cube(f"{side}_forearm", (cf + s * 2.0 - 0.7, 11.6, -1.8), (1.4, 5.0, 3.6), fringe(fur(39292, 4.8), 3, 3, 39292 + (s > 0)), density=2,
               tag="forearm_tuft", rotation=(0, 0, -18 * s), pivot=(cf + s * 2.2, 16.6, 0))
        m.cube(f"{side}_forearm", (cf - 2.0, 14.6, 1.8), (4.0, 3.0, 1.4), fringe(fur(39294, 4.8), 2, 3, 39294 + (s > 0)), density=2,
               tag="elbow_tuft", rotation=(26, 0, 0), pivot=(cf, 17.6, 1.9))
        # The hand: a broad dark paw-hand; long knuckled fingers; hooked black claws; a thumb on the inside.
        m.cube(f"{side}_hand", (cf - 2.2, 7.8, -2.4), (4.4, 3.0, 4.4), pelt(39296, POINT, base=5.2, grad=0.8), density=2, tag="hand")
        for i, x in enumerate((-1.6, -0.55, 0.55, 1.6)):
            ln = 2.4 if i in (1, 2) else 2.0
            m.cube(f"{side}_claws", (cf + x - 0.5, 8.0 - ln, -2.3), (1.0, ln, 1.2),
                   lambda f, x_, y, w, h: tone(POINT, 4.6 - 1.4 * y / max(1, h)), density=4, tag="finger")
            m.cube(f"{side}_claws", (cf + x - 0.25, 8.0 - ln - 2.0, -2.25), (0.5, 2.1, 0.6),
                   lambda f, x_, y, w, h: W_CLAW[5 - min(5, y * 5 // max(1, h))] if f != "down" else W_CLAW[0], density=4, tag="claw",
                   rotation=(-22, 0, 0), pivot=(cf + x, 8.0 - ln, -2.0))
        m.cube(f"{side}_claws", (cf - s * 2.3 - 0.5, 6.6, -2.0), (1.0, 2.0, 1.1), lambda f, x_, y, w, h: tone(POINT, 4.2), density=4,
               tag="thumb", rotation=(-10, 0, 14 * s), pivot=(cf - s * 2.0, 8.6, -1.5))
        m.cube(f"{side}_claws", (cf - s * 2.3 - 0.25, 4.9, -2.0), (0.5, 1.8, 0.6),
               lambda f, x_, y, w, h: W_CLAW[5 - min(5, y * 5 // max(1, h))], density=4, tag="claw", rotation=(-30, 0, 14 * s),
               pivot=(cf - s * 2.0, 8.6, -1.5))


def _wolf_legs(m):
    for side, s in SIDES:
        x = 2.9 * s
        # The haunch: a huge muscled thigh, its back rounded out, fur hanging behind.
        m.cube(f"{side}_leg", (x - 2.8, 7.8, -3.0), (5.6, 7.4, 6.2), pelt(39300 + (s > 0), SILVER, base=4.4, blend=_saddle_back), density=2,
               tag="thigh")
        m.cube(f"{side}_leg", (x - 2.4, 9.0, 2.6), (4.8, 5.4, 1.4), pelt(39302 + (s > 0), SADDLE, base=5.0), density=2, tag="haunch")
        m.cube(f"{side}_leg", (x - 2.5, 6.8, 1.0), (5.0, 3.0, 2.6), fringe(pelt(39304, SILVER, base=4.6), 3, 3, 39304 + (s > 0)), density=2,
               tag="thigh_tuft", rotation=(24, 0, 0), pivot=(x, 9.8, 2.0))
        # The shank: lean and sinewy, a calf behind, a tuft at the back.
        m.cube(f"{side}_shin", (x - 1.7, 3.0, -1.8), (3.4, 5.8, 3.4), pelt(39306 + (s > 0), SILVER, base=4.2), density=2, tag="shin")
        m.cube(f"{side}_shin", (x - 1.5, 5.0, 1.4), (3.0, 3.4, 1.2), pelt(39308, SILVER, base=4.0), density=2, tag="calf")
        m.cube(f"{side}_shin", (x - 1.6, 3.0, 1.6), (3.2, 2.6, 1.2), fringe(pelt(39309, SILVER, base=4.4), 2, 3, 39309 + (s > 0)), density=2,
               tag="calf_tuft", rotation=(26, 0, 0), pivot=(x, 5.6, 2.0))
        # The long foot -- the wolf's hock -- darkening to the paw.
        m.cube(f"{side}_foot", (x - 1.3, -0.8, -1.4), (2.6, 4.6, 2.6), pelt(39310 + (s > 0), SILVER, base=4.0, blend=fade_down(POINT, 0.2)),
               density=2, tag="hock")
        m.cube(f"{side}_foot", (x - 1.2, 1.6, 0.9), (2.4, 2.2, 1.2), fringe(pelt(39312, POINT, base=5.0), 2, 2, 39312 + (s > 0)), density=2,
               tag="hock_tuft", rotation=(20, 0, 0), pivot=(x, 3.6, 1.2))
        # The paw: a broad pad, four thick toes, black claws.
        m.cube(f"{side}_paw", (x - 2.0, -1.8, -3.4), (4.0, 1.4, 4.4), pelt(39314, POINT, base=4.8, grad=0.6), density=2, tag="paw")
        for i, dx in enumerate((-1.5, -0.5, 0.5, 1.5)):
            m.cube(f"{side}_paw", (x + dx - 0.48, -1.8, -4.8), (0.95, 1.1 if i in (1, 2) else 0.9, 1.6),
                   lambda f, x_, y, w, h: tone(POINT, 4.4 - y), density=4, tag="toe")
            m.cube(f"{side}_paw", (x + dx - 0.2, -1.8, -5.6), (0.4, 0.55, 0.9), lambda f, x_, y, w, h: W_CLAW[4 - y], density=4, tag="toe_claw")
    # The tail: thick and bushy, silver above, pale beneath, black at the tip.
    m.cube("tail", (-1.6, 8.4, 1.2), (3.2, 6.0, 3.2), pelt(39320, SILVER, base=4.6, blend=lambda f, x, y, w, h: (SADDLE, 0.8) if f in ("south", "up") else None),
           density=2, tag="tail")
    m.cube("tail", (-2.0, 7.4, 0.8), (4.0, 3.6, 4.0), fringe(pelt(39321, SILVER, base=4.8), 2, 3, 39321), density=2, tag="tail_fluff")
    m.cube("tail_tip", (-2.2, 1.4, 0.6), (4.4, 6.8, 4.4), fringe(pelt(39322, SILVER, base=4.8, blend=fade_down(SADDLE, 0.55, 0.8)), 2, 3, 39322),
           density=2, tag="tail_brush")
    m.cube("tail_tip", (-1.4, -0.2, 1.4), (2.8, 2.0, 2.8), fringe(pelt(39323, SADDLE, base=4.2), 2, 2, 39323), density=2, tag="tail_end")


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
# His own skin: waxy, bloodless, a dead yellow-grey going violet in the hollows; under it the raw meat, dark and wet.
S_SKIN = Ramp("#29252a", "#3f3a3c", "#57514f", "#716a64", "#8b847a", "#a39d90", "#b9b4a5", "#cdc9b9")
S_BRUISE = hexc("#4c3a4a")
S_RAW = Ramp("#140306", "#26060b", "#3b0b12", "#511119", "#661a20", "#7c2629", "#953735", "#ad4f47")
S_SLIME = Ramp("#3b3718", "#57511f", "#746c2b", "#918839", "#ada44d", "#c7be68")
PEEL_EDGE = Ramp("#6f685c", "#877f71", "#9d9586", "#b1aa9a", "#c3bdad", "#d2cdbe")
S_HAIR = Ramp("#0e0c0b", "#181513", "#221e1b", "#2e2824", "#3b342f", "#4a423c")
RAG = Ramp("#120f0b", "#1c1711", "#272017", "#332a1f", "#403527", "#4f4231")
GUM = Ramp("#1c0c12", "#2b141a", "#3b1d22", "#4b282b", "#5b3433")
S_TEETH = Ramp("#3d3628", "#585039", "#756c4f", "#908766", "#a9a07d", "#bdb593")
EYE_W = hexc("#b9b29a")
FLARE, FLARE_EDGE = hexc("#f2f8ff"), hexc("#8fa6c0")

# The upper face (14 x 12 texels, x = 0 his right) on the skull; the lower jaw (12 x 5) on `jaw`. Lidless eyes over
# sagging raw lower lids; the nose all but gone; no lips at all -- bare gums and long uneven teeth in a grimace that pulls
# down at the corners; his left cheek peeled away (the flap hangs from it) down to the meat and the back teeth.
SHIFTER_UPPER = [
    "hhshhhshhhhshh",
    "shhsshsshshhsh",
    "ssssssssssssss",
    "dssssssssssssd",
    "dDDDdssssdDDDd",
    "DWWFDdssdDFWWD",
    "dDrrrdnndrrrDe",
    "dddssdnnssseer",
    "dDsssssssserrr",
    "dDdsssssssrrTr",
    "ddsggggggggrTr",
    "ddgTtTTtTtTgrr",
]
SHIFTER_JAW = [
    "dgTtTTtTtTrr",
    "ddgggggggTrr",
    "dDsssssssyrr",
    "ddsssssssyrr",
    "dddssssssdyd",
]


def sick_skin_m(seed, base=4.2):
    """Waxy dead skin: smooth, a faint mottle, violet in the hollows and bruised in blotches, veins showing through."""
    def m(f, x, y, w, h):
        n = fbm(x * 0.6 + len(f) * 3, y * 0.6, seed, 64, 64, 2, 6.0)
        v = base + (n - 0.5) * 1.4 + (0.6 if f == "up" else 0) - (0.8 if f == "down" else 0)
        c = tone(S_SKIN, v)
        vein = fbm(x * 0.8 + len(f) * 3, y * 0.5, seed + 5, 64, 64, 2, 5.0)
        if abs(vein - 0.5) < 0.014 and fbm(x * 0.5 + 3, y * 0.5 + w, seed + 2, 64, 64, 1, 3.0) > 0.5:
            c = mix(c, hexc("#463a57"), 0.4)              # a vein showing through
        if fbm(x * 0.7 + 11 + w * 3, y * 0.7 + len(f) * 7, seed + 9, 64, 64, 3, 5.0) > 0.66:
            c = mix(c, S_BRUISE, 0.3)                      # a bruise gone livid
        return c
    return m


def raw_m(seed, base=3.6):
    """The meat under the skin: dark wet muscle in vertical fibres, grooves between them, a glisten here and there and
    threads of yellow slime."""
    def m(f, x, y, w, h):
        fib = (h01("fib", seed, f, x) - 0.5) * 1.6
        v = base + fib + (fbm(x * 0.4, y * 0.5 + len(f) * 5, seed + 1, 64, 64, 2, 5.0) - 0.5) * 1.6
        if x % 3 == 0:
            v -= 1.1                                       # the groove between two fibres
        c = tone(S_RAW, v)
        if h01("wet", seed, f, x, y) < 0.06:
            c = mix(c, hexc("#e0a090"), 0.55)              # a wet glint
        if abs(fbm(x * 0.9 + 7, y * 0.35, seed + 3, 64, 64, 2, 7.0) - 0.5) < 0.025:
            c = mix(c, S_SLIME[4], 0.5)                    # a thread of slime
        return c
    return m


def skin_sheet(seed, inner="south", drape=True):
    """A sheet of his own skin coming away: the same waxy grey, drier, wrinkled across and paler towards the curled, torn
    lower edge, worn through in holes; the underside wet, pink-yellow, specked with blood."""
    sk = sick_skin_m(seed, 4.6)

    def m(f, x, y, w, h):
        if drape and f not in ("up", "down") and y >= h - 3:
            r = h - 1 - y
            if h01("rag", seed, f, x // 2) < 0.62 - 0.22 * r:
                return CLEAR                              # the torn edge, in tongues
        if f in ("north", "south") and 0 < y < h - 3 and h01("hole", seed, f, x // 2, y // 2) < 0.045:
            return CLEAR
        if drape and f in ("north", "south", "east", "west") and w > 5 and y > h * 0.45:
            # Torn up from the edge into tongues: the splits widen as they run down.
            g = (y / max(1, h - 1) - 0.45) / 0.55
            for k in range(3):
                gx = int(h01("split", seed, f, k) * (w - 4)) + 2
                if abs(x - gx) < 0.3 + g * 1.2 and h01("sp2", seed, f, k) < 0.75:
                    return CLEAR
        if f == inner:
            # The underside: wet, raw pink going dark where it was torn from the meat, threads of slime.
            c = mix(tone(S_RAW, 5.0 + h01("wet", seed, x, y) * 2.0), PEEL_EDGE[2], 0.3)
            if abs(fbm(x * 0.8, y * 0.4 + 3, seed + 4, 64, 64, 2, 6.0) - 0.5) < 0.03:
                c = mix(c, S_SLIME[4], 0.55)
            return mix(c, S_RAW[1], 0.5) if h01("bl", seed, x, y) < 0.14 else c
        t = y / max(1, h - 1)
        c = sk(f, x, y, w, h)
        if h01("wr", seed, f, y) < 0.22:
            c = shade(c, 0.85)                            # a wrinkle across
        if t > 0.6:
            c = mix(c, PEEL_EDGE[4], 0.25 + 0.5 * (t - 0.6))  # dried paper-thin at the edge
        if x in (0, w - 1):
            c = shade(c, 0.78)
        return c
    return m


def peeled_m(seed, skin, share=0.25, raw=None):
    """Skin with raw wet patches where it has already come away, each ringed by a lifted, papery edge and its shadow."""
    raw = raw or raw_m(seed + 1)
    lo = 0.7 - share * 0.25

    def m(f, x, y, w, h):
        n = fbm(x * 0.6 + len(f) * 9 + w * 5.3, y * 0.5 + h * 3.7 + (17 if f in ("east", "south") else 0), seed, 64, 64, 2, 4.0)
        if f == "up":
            n -= 0.08
        if n > lo + 0.012:
            return raw(f, x, y, w, h)
        if n > lo:
            return PEEL_EDGE[3] if (x + y) % 2 else PEEL_EDGE[2]
        if n > lo - 0.02:
            return shade(skin(f, x, y, w, h), 0.7)
        return skin(f, x, y, w, h)
    return m


def build_shapeshifter(m):
    b = SB
    n = b.neck
    m.bone("root", (0, 0, 0))
    LG.skeleton(m, b)
    m.bone("left_index", (b.ax, b.palm, -1.0), "left_hand")
    m.bone("jaw", (0, n + 2.5, -1.0), "head")
    m.bone("flap_face", (1.4, n + 6.6, -4.2), "head")
    m.bone("skin_flaps", (0, n - 0.8, 0), "body")
    m.bone("flap_chest", (-1.2, n - 1.0, -b.depth / 2 - 0.1), "skin_flaps")
    m.bone("flap_back", (0.6, n - 0.6, b.depth / 2 + 0.1), "skin_flaps")
    m.bone("flap_shoulder_r", (-b.sx, b.sy + 0.6, 0), "skin_flaps")
    m.bone("flap_shoulder_l", (b.sx, b.sy + 0.6, 0), "skin_flaps")
    m.bone("flap_belly", (1.4, b.hip + 4.0, -b.depth / 2 - 0.1), "skin_flaps")
    for side, s in SIDES:
        m.bone(f"flap_arm_{side[0]}", (b.ax * s, b.elbow - 0.4, -1.4), f"{side}_forearm")
        m.bone(f"wrist_skin_{side[0]}", (b.ax * s, b.wrist + 0.6, 0), f"{side}_forearm")

    sk = sick_skin_m(39301)
    raw = raw_m(39304)
    patched = peeled_m(39302, sk, 0.12, raw)
    hr = LG.hair_m(S_HAIR, 39303, 2.4)
    _shifter_head(m, b, sk, raw, hr)
    _shifter_body(m, b, sk, raw, patched)
    _shifter_limbs(m, b, sk, raw, patched)
    _shifter_sheets(m, b)


def _shifter_head(m, b, sk, raw, hr):
    n = b.neck
    hollow = lambda f, x, y, w, h: mix(sk(f, x, y, w, h), S_BRUISE, 0.35)
    deep = lambda f, x, y, w, h: mix(sk(f, x, y, w, h), hexc("#140c10"), 0.62)
    scalp = lambda f, x, y, w, h: (hr(f, x, y, w, h) if h01("bald", 39305, f, x // 1, y // 3) < 0.12
                                   else mix(sk(f, x, y, w, h), S_BRUISE, 0.2 + 0.25 * h01("sc", f, x, y)))

    def eye_glow(f, x, y, w, h, c):
        return hexc("#f6faff") if c == FLARE else (hexc("#7d93ad") if c == FLARE_EDGE else None)
    legend = {"s": sk, "h": scalp, "d": hollow, "D": deep, "r": raw, "e": PEEL_EDGE[4], "W": EYE_W, "F": FLARE, "n": hexc("#120709"),
              "g": lambda f, x, y, w, h: tone(GUM, 2.0 + h01("gum", x, y) * 2.2), "T": lambda f, x, y, w, h: S_TEETH[5 - int(h01("th", x, y) * 2.5)], "t": S_TEETH[1],
              "M": hexc("#0b0406"), "y": S_SLIME[3]}

    def face_of(rows, y_off, x_off):
        def mat(f, x, y, w, h):
            if f == "north":
                row = rows[min(len(rows) - 1, y)]
                v = legend.get(row[min(len(row) - 1, x)], sk)
                return v(f, x, y, w, h) if callable(v) else v
            if f == "up":
                return scalp(f, x, y, w, h)
            fx = x if f == "west" else w - 1 - x        # 0 at the front
            if f in ("east", "west"):
                if f == "east" and fx < 6 and y + y_off >= 7:
                    return raw(f, x, y, w, h)             # his left cheek, flayed back to the ear
                if y + y_off < 2 or (fx > 7 and y + y_off < 6):
                    return scalp(f, x, y, w, h)
                if fx < 4 and 3 <= y + y_off <= 9:
                    return hollow(f, x, y, w, h)          # the sunken temple and cheek
            if f == "south" and h01("strand", 39306, x) < 0.3 and y + y_off < 9:
                return hr(f, x, y, w, h)
            return sk(f, x, y + y_off, w, h + y_off)
        return mat
    m.cube("head", (-3.5, n + 2.4, -3.6), (7.0, 6.0, 7.0), face_of(SHIFTER_UPPER, 0, 0), eye_glow, density=2, tag="skull")
    m.cube("jaw", (-3.0, n - 0.1, -3.5), (6.0, 2.5, 6.6), face_of(SHIFTER_JAW, 12, 1), density=2, tag="jaw")
    # Inside the mouth: a dark wet throat behind the teeth.
    m.cube("head", (-2.8, n + 1.4, -3.3), (5.6, 1.2, 5.0), lambda f, x, y, w, h: hexc("#0b0406") if f != "up" else GUM[1],
           density=2, tag="throat")
    # The bone of him pressing out: a ridge of brow over the sockets, the cheekbone on the side that still has skin.
    m.cube("head", (-3.3, n + 6.0, -3.85), (6.6, 0.5, 0.5), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 0.62 if f == "down" else 1.12),
           density=2, tag="brow_ridge")
    m.cube("head", (-3.55, n + 4.6, -3.75), (1.6, 0.5, 2.2), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.1 if f == "up" else 0.9),
           density=2, tag="cheekbone")
    # What is left of the nose: a gristly ridge over two slits.
    m.cube("head", (-0.35, n + 4.6, -3.95), (0.7, 1.4, 0.35), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.15 if f == "north" and y < 2 else 0.85),
           density=4, tag="nose")
    # Ears shrivelled to curled nubs.
    for s in (-1, 1):
        m.cube("head", (s * 3.65 - 0.2, n + 4.2, -0.6), (0.4, 1.6, 1.2), lambda f, x, y, w, h: tone(S_SKIN, 2.4 + y * 0.2), density=4, tag="ear")
    # A few long lank strands from the crown.
    for i, (x, z, ln) in enumerate(((-2.8, -1.0, 7.0), (-1.2, 1.8, 9.0), (2.4, 0.4, 6.0), (3.4, 2.4, 8.0), (0.6, 3.6, 10.0), (-3.4, 2.8, 6.5))):
        m.cube("head", (x - 0.15, n + 8.4 - ln, z - 0.15), (0.3, ln, 0.3),
               lambda f, x_, y, w, h: CLEAR if y > h - 2 and x_ % 2 else tone(S_HAIR, 3.2 - y * 0.08), density=4, tag="strand",
               rotation=(8 * (i % 3 - 1), 0, 6 * (i % 2 * 2 - 1)), pivot=(x, n + 8.4, z))
    # The flap of his face: the skin of the left cheek peeled down from under the eye, hanging out over the meat.
    m.cube("flap_face", (1.2, n + 0.2, -3.95), (2.3, 4.6, 0.25), skin_sheet(39358, inner="south"), density=4, tag="face_flap",
           rotation=(-26, 0, -14), pivot=(2.3, n + 4.8, -3.8))


def _shifter_body(m, b, sk, raw, patched):
    n = b.neck
    m.cube("body", (-1.1, n - 1.2, -1.0), (2.2, 1.8, 2.2), sk, density=2, tag="neck")
    for s in (-1, 1):
        m.cube("body", (s * 0.75 - 0.25, n - 1.3, -1.25), (0.5, 1.6, 0.3), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.12),
               density=4, tag="tendon", rotation=(0, 0, 14 * s), pivot=(s * 0.4, n + 0.2, -1.2))

    # The ribcage: skin drawn over every rib, the sternum, the pit of the stomach; where the chest sheet has come away,
    # the meat beneath.
    def ribs(f, x, y, w, h):
        c = patched(f, x, y, w, h)
        mid = (w - 1) / 2
        # Each rib a pale curve sweeping down and out from the sternum with a hollow under it.
        d = abs(x - mid) if f in ("north", "south") else (w + (x if f == "west" else w - 1 - x)) * 0.7
        k = (y - d * 0.45) % 3.2
        if f == "north":
            if x < mid - 1 and y < h * 0.75:
                return raw(f, x, y, w, h)                 # under the chest flap, his right breast
            if abs(x - mid) < 0.6:
                return shade(c, 0.78)                     # the sternum's groove
        if y > 1 and abs(x - mid) > 0.6 or f in ("east", "west"):
            if k < 1.0:
                return shade(c, 1.12)
            if k < 2.0:
                return shade(c, 0.7)
        return c
    m.cube("body", (-3.2, n - 6.8, -1.6), (6.4, 6.8, 3.2), ribs, density=2, tag="ribcage")
    # The belly sunk in under the ribs; the hip bones jutting over the rags.
    m.cube("body", (-2.6, b.hip + 1.2, -1.2), (5.2, n - 6.8 - b.hip - 1.2, 2.6),
           lambda f, x, y, w, h: shade(patched(f, x, y, w, h), 0.86 if f == "north" else 1.0), density=2, tag="belly")
    m.cube("body", (-3.0, b.hip - 0.4, -1.5), (6.0, 1.8, 3.0), patched, density=2, tag="pelvis")
    for s in (-1, 1):
        m.cube("body", (s * 2.4 - 0.5, b.hip + 0.6, -1.75), (1.0, 0.8, 0.5), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.15),
               density=4, tag="hip_bone")
        m.cube("body", (s * 2.1 - 1.2, n - 1.8, -1.95), (2.4, 0.5, 0.5),
               lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.2 if f in ("north", "up") else 0.7), density=4, tag="collarbone",
               rotation=(0, 0, 12 * s), pivot=(s * 1.0, n - 1.6, -1.8))
        # Shoulder blades standing out of his back.
        m.cube("body", (s * 1.6 - 1.1, n - 5.0, 1.55), (2.2, 3.0, 0.4), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.08 if f != "down" else 0.6),
               density=2, tag="scapula", rotation=(-6, 0, -10 * s), pivot=(s * 1.6, n - 2.0, 1.6))
    for i in range(7):
        m.cube("body", (-0.3, n - 1.4 - i * 1.4, 1.55), (0.6, 0.5, 0.3), lambda f, x, y, w, h: shade(sk(f, x, y, w, h), 1.15),
               density=4, tag="vertebra")
    # Rags: a filthy cloth knotted round the hips, in tatters.
    rag = LG.worn(RAG, 39310, 2.6, 1.2, scuff=0.08)

    def rags(f, x, y, w, h):
        if f in ("up", "down"):
            return None if f == "down" else rag(f, x, y, w, h)
        if y >= h - 4 and h01("tatter", 39311, f, x // 2) * 4 < y - (h - 4) + 0.8:
            return CLEAR
        if h01("hole", 39312, f, x, y) < 0.06:
            return CLEAR
        c = rag(f, x, y, w, h)
        return mix(c, S_RAW[2], 0.4) if h01("stain", 39313, f, x // 2, y // 2) < 0.12 else c
    m.cube("body", (-3.1, b.hip - 3.0, -1.6), (6.2, 3.8, 3.2), rags, inflate=0.3, density=2, tag="rag_hips")
    m.cube("body", (-2.9, b.hip + 0.4, -1.6), (5.8, 0.5, 3.2), none_on(("up", "down"), solid(RAG[1])), inflate=0.35, density=4, tag="rag_cord")
    # Drips of yellow slime down the chest and belly.
    for x in (-1.8, 0.6, 2.2):
        m.cube("body", (x, n - 9.0 - x * 0.5, -1.65), (0.4, 4.0, 0),
               lambda f, x_, y, w, h: CLEAR if h01("dr", x, y) < y / max(1, h) - 0.25 else tone(S_SLIME, 4.0 - y * 0.25), faces=("north",),
               density=4, tag="drip")


def _shifter_limbs(m, b, sk, raw, patched):
    for side, s in SIDES:
        cx = b.ax * s
        # Thin upper arms, the bone of the shoulder showing, knotted elbows; the forearms skinned to the meat, the skin
        # rolled down and bunched round the wrist like a glove half pulled off.
        def upper(f, x, y, w, h, s=s):
            # Raw at the top, where the shoulder's skin has slid down off it, the torn edge ragged.
            edge = h * 0.3 + h01("ue", s, f, x) * 2.0
            if f == "up" or y < edge - 0.6:
                return raw(f, x, y, w, h)
            if y < edge + 0.4:
                return PEEL_EDGE[4]
            return patched(f, x, y, w, h)
        m.cube(f"{side}_arm", (cx - 1.1, b.elbow, -1.1), (2.2, b.sy + 0.8 - b.elbow, 2.2), upper, density=2, tag="upper_arm")
        m.cube(f"{side}_arm", (cx - 1.25, b.sy - 0.4, -1.2), (2.5, 1.2, 2.4), lambda f, x, y, w, h: raw(f, x, y, w, h) if f != "up" or (x + y) % 3 else S_RAW[6],
               density=2, tag="shoulder_knob")
        m.cube(f"{side}_forearm", (cx - 0.95, b.wrist, -0.95), (1.9, b.elbow - b.wrist, 1.9), raw_m(39320 + (s > 0), 3.9), density=2,
               tag="forearm")
        m.cube(f"{side}_forearm", (cx - 1.35, b.elbow - 1.1, -1.2), (2.7, 1.3, 2.4), lambda f, x, y, w, h: tone(S_SKIN, 3.6 + y * 0.4),
               density=2, tag="elbow")

        def roll(f, x, y, w, h):
            if f in ("up", "down"):
                return None if f == "down" else S_RAW[1]
            if y == 0 and h01("rl", s, f, x) < 0.4:
                return CLEAR
            c = tone(S_SKIN, 3.0 + (y % 2) * 1.6 + h01("rl2", s, f, x) * 0.8)
            return mix(c, PEEL_EDGE[4], 0.4) if y == 1 else c
        m.cube(f"wrist_skin_{side[0]}", (cx - 1.25, b.wrist - 0.3, -1.25), (2.5, 1.6, 2.5), roll, density=4, tag="skin_roll")
    _long_hands(m, b, sick_skin_m(39330, 4.0), hexc("#2c2416"))
    for side, s in SIDES:
        x0 = b.lx * s - b.lw / 2
        # Legs like sticks, knees like knots, long bare feet with long toes.
        m.cube(f"{side}_leg", (x0 + 0.25, b.knee, -b.lw / 2 + 0.25), (b.lw - 0.5, b.hip - b.knee, b.lw - 0.5), patched, density=2, tag="thigh")
        m.cube(f"{side}_shin", (x0 + 0.1, b.knee - 1.0, -b.lw / 2 - 0.05), (b.lw - 0.2, 1.8, b.lw - 0.1),
               lambda f, x, y, w, h: tone(S_SKIN, 4.6 - y * 0.5 if f != "down" else 2.0), density=2, tag="knee")
        m.cube(f"{side}_shin", (x0 + 0.35, 0.0, -b.lw / 2 + 0.35), (b.lw - 0.7, b.knee, b.lw - 0.7), peeled_m(39340 + (s > 0), sk, 0.18, raw),
               density=2, tag="shin")
        m.cube(f"{side}_shin", (x0 + 0.1, 0.0, -b.lw / 2 - 2.2), (b.lw - 0.2, 0.8, 2.6), lambda f, x, y, w, h: tone(S_SKIN, 3.2 if f != "up" else 4.4),
               density=2, tag="foot")
        for i, dx in enumerate((0.25, 0.95, 1.65, 2.3)):
            m.cube(f"{side}_shin", (x0 + dx, 0.0, -b.lw / 2 - 2.9), (0.45, 0.45, 0.8), lambda f, x, y, w, h: S_TEETH[1] if f == "north" else tone(S_SKIN, 3.4),
                   density=4, tag="toe")


def _shifter_sheets(m, b):
    """The sheets of his own skin coming off him."""
    n = b.neck
    # Round the neck and over the collarbones: loose skin like a slipping collar.
    m.cube("skin_flaps", (-1.6, n - 3.6, 1.7), (3.4, 3.0, 0.25), skin_sheet(39350, inner="north"), density=4, tag="nape_flap",
           rotation=(12, 0, 0), pivot=(0, n - 0.6, 1.7))
    # Off his right breast: a sheet hanging forward from the collarbone, the wet meat behind it.
    m.cube("flap_chest", (-2.9, n - 7.4, -1.95), (2.9, 6.2, 0.25), skin_sheet(39351), density=4, tag="chest_flap",
           rotation=(-14, 0, 8), pivot=(-1.2, n - 1.0, -1.8))
    m.cube("flap_belly", (0.2, b.hip - 0.6, -1.5), (2.4, 4.6, 0.25), skin_sheet(39352), density=4, tag="belly_flap",
           rotation=(-6, 0, -6), pivot=(1.4, b.hip + 4.0, -1.4))
    # Down his back, a great sheet like a cape, split up the middle.
    m.cube("flap_back", (-3.0, n - 11.0, 1.75), (5.6, 10.2, 0.25), skin_sheet(39353, inner="north"), density=4, tag="back_flap",
           rotation=(8, 0, -3), pivot=(0.6, n - 0.6, 1.8))
    for bone, s in (("flap_shoulder_r", -1), ("flap_shoulder_l", 1)):
        # Sliding off each shoulder: a sheet over the top of it hanging down the outside of the arm, another behind.
        xo = b.sx * s + s * 1.35
        m.cube(bone, (xo - 0.12, b.sy - 5.4, -1.1), (0.25, 5.8, 2.2), skin_sheet(39354 + (s > 0), inner="east" if s < 0 else "west"),
               density=4, tag="shoulder_flap", rotation=(0, 0, -10 * s), pivot=(xo, b.sy + 0.4, 0))
        m.cube(bone, (b.sx * s - 1.2, b.sy - 3.8, 1.3), (2.4, 4.4, 0.25), skin_sheet(39362 + (s > 0), inner="north"), density=4,
               tag="shoulder_back", rotation=(14, 0, -6 * s), pivot=(b.sx * s, b.sy + 0.6, 1.3))
    for side, s in SIDES:
        m.cube(f"flap_arm_{side[0]}", (b.ax * s + s * 1.0 - 0.12, b.elbow - 6.0, -1.1), (0.25, 5.6, 2.2),
               skin_sheet(39356 + (s > 0), inner="east" if s < 0 else "west"), density=4, tag="arm_flap", rotation=(0, 0, 10 * s),
               pivot=(b.ax * s + s * 1.0, b.elbow - 0.4, 0))


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
