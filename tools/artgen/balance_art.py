"""The power curve's art (v0.15): the five Ascension Shards (one colour and one shape per tier, the same engraved chevron on
each, brighter and more elaborate as the tier climbs) and Hunter's Gear, a hunter's working clothes: four flat icons
(trucker cap, canvas jacket open over a flannel shirt, jeans with a belt, leather work boots) and the two armour layers on
vanilla's 64x32 humanoid layout (layer 1: cap, jacket, boots; layer 2: jeans).

The tier colours are mirrored in Java by client/weapon/AscensionTooltips.TIER_COLORS (the icon marker and tooltip lines).
"""

import math

from common import save
from pixelkit import Ramp, Tex, hexc, item_outline, mix, shade

K = hexc("#1E1410")

# --- the shards ---------------------------------------------------------------------------------------------------------

SHARD_RAMPS = {
    1: Ramp("#4a1406", "#8a300e", "#c4521a", "#e8783a", "#ffb468", "#fff0c8"),   # ember: the rite's own shard
    2: Ramp("#40060e", "#761220", "#ac2232", "#e0464e", "#f88a8a", "#ffe2dc"),   # blood: Azazel and Lilith
    3: Ramp("#0a2450", "#164690", "#2a70c8", "#5aa8ff", "#a8d4ff", "#f2faff"),   # azure: Lucifer, Gabriel, the Horsemen
    4: Ramp("#260a48", "#461a86", "#6e36be", "#b46cff", "#dcb2ff", "#fcf2ff"),   # violet: the Chorus, Metatron, Amara, Death
    5: Ramp("#5a3a0c", "#94661a", "#c99a2c", "#ffe070", "#fff2b0", "#ffffff"),   # gold-white: Uncaged and Michael
}

# Each tier's crystals: (polygon, ridge from, ridge to). Coordinates in pixels of a 16x16 icon, y down.
SHARD_SHAPES = {
    1: [([(9.5, 3), (12, 7.5), (10, 13.5), (6.5, 13.5), (5.5, 8.5)], (9.5, 3), (8.2, 13.5))],
    2: [([(10, 1.5), (12.8, 6.5), (11, 13.8), (7, 14), (6, 7.5)], (10, 1.5), (9, 14)),
        ([(4.2, 6.5), (6.4, 9.5), (6, 14), (3.4, 13.6), (2.8, 10)], (4.2, 6.5), (4.6, 14))],
    3: [([(10.5, 0.8), (13.6, 5.5), (12.2, 13.8), (8.6, 15), (5.2, 11), (6.6, 4.5)], (10.5, 0.8), (10.2, 15)),
        ([(3.4, 7.5), (5.6, 10.5), (5.6, 14.6), (2.8, 14.2), (2.2, 10.5)], (3.4, 7.5), (4.2, 14.6))],
    4: [([(8, 0.5), (11.4, 4.8), (10.6, 12.6), (8, 15.2), (5.4, 12.6), (4.6, 4.8)], (8, 0.5), (8, 15.2)),
        ([(13.4, 5.5), (15, 9.2), (13.6, 14), (11.4, 13.4), (11.6, 8.6)], (13.4, 5.5), (12.6, 14)),
        ([(2.6, 5.5), (4.4, 8.6), (4.6, 13.4), (2.4, 14), (1, 9.2)], (2.6, 5.5), (3.4, 14))],
    5: [([(8, 0.2), (11.6, 4.6), (10.8, 12), (8, 15.6), (5.2, 12), (4.4, 4.6)], (8, 0.2), (8, 15.6))],
}


def inside(px, py, poly):
    hit = False
    n = len(poly)
    for i in range(n):
        x0, y0 = poly[i]
        x1, y1 = poly[(i + 1) % n]
        if (y0 > py) != (y1 > py) and px < (x1 - x0) * (py - y0) / (y1 - y0) + x0:
            hit = not hit
    return hit


def edge_distance(px, py, poly):
    best = 99.0
    n = len(poly)
    for i in range(n):
        ax, ay = poly[i]
        bx, by = poly[(i + 1) % n]
        dx, dy = bx - ax, by - ay
        u = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
        best = min(best, math.hypot(px - ax - u * dx, py - ay - u * dy))
    return best


def crystal(t, poly, ridge_a, ridge_b, ramp):
    """A faceted crystal: the left facet catches the light, the right one is in shade, the ridge glows from within."""
    ax, ay = ridge_a
    bx, by = ridge_b
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            if not inside(px, py, poly):
                continue
            side = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
            length = math.hypot(bx - ax, by - ay)
            d_ridge = abs(side) / length
            d_edge = edge_distance(px, py, poly)
            along = max(0.0, min(1.0, ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / (length * length)))
            if d_ridge < 0.6:
                k = 5 if along < 0.55 else 4
            elif side > 0:     # left of the ridge as drawn: the lit facet
                k = 4 if d_edge > 1.2 else 3
            else:
                k = 2 if d_edge > 1.2 else 1
            if d_edge < 0.75 and k > 1:
                k -= 1
            t.set(x, y, ramp[k])


def chevron(t, cx, cy, color):
    """The mark every shard carries: a small rising chevron engraved near its heart."""
    for (dx, dy) in ((-1, 1), (0, 0), (1, 1)):
        t.set(cx + dx, cy + dy, color)


def ascension_shard(tier):
    ramp = SHARD_RAMPS[tier]
    t = Tex(16, 16, 300 + tier)
    if tier == 5:
        # A pale halo behind the crystal.
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - 8, y + 0.5 - 8.2)
                ang = math.degrees(math.atan2(y + 0.5 - 8.2, x + 0.5 - 8)) % 45
                if 6.1 <= d <= 7.0 and 6 < ang < 39:
                    t.set(x, y, ramp[5] if ang < 22 else ramp[4])
    for poly, a, b in SHARD_SHAPES[tier]:
        crystal(t, poly, a, b, ramp)
    item_outline(t, mix(K, ramp[0], 0.35))
    # The engraved chevron, darker than the crystal; then the glints.
    cx, cy = {1: (9, 8), 2: (9, 8), 3: (10, 7), 4: (8, 7), 5: (8, 7)}[tier]
    chevron(t, cx, cy, ramp[1] if tier < 5 else ramp[2])
    glints = {1: [(9, 4)], 2: [(10, 3), (4, 8)], 3: [(10, 2), (12, 5), (3, 9)],
              4: [(8, 2), (13, 7), (2, 7), (6, 4)], 5: [(8, 1), (6, 4), (10, 4), (1, 8), (14, 8)]}[tier]
    for (x, y) in glints:
        t.set(x, y, hexc("#ffffff"))
    if tier >= 4:
        # Sparks off the crystal: a four-point twinkle beside it.
        sx, sy = (14, 2) if tier == 4 else (13, 1)
        for (dx, dy) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            if t.get(sx + dx, sy + dy)[3] == 0:
                t.set(sx + dx, sy + dy, ramp[5] if (dx, dy) == (0, 0) else ramp[3])
    return t


# --- Hunter's Gear: colours --------------------------------------------------------------------------------------------

CANVAS = Ramp("#3a2610", "#5c3f1c", "#80602c", "#a07c3a", "#c09a56", "#dcc084")
CORD = Ramp("#20140a", "#33210f", "#4a3018", "#614024", "#7a5432")
FLANNEL = Ramp("#3e0a0c", "#6a1216", "#962020", "#b8302a", "#d24c3c")
CHECK = hexc("#20161e")
DENIM = Ramp("#121a30", "#1c284c", "#283a68", "#384f88", "#5068a4", "#7e94c4")
STITCH = hexc("#c89a4a")
LEATHER = Ramp("#22150a", "#3c2512", "#5a381c", "#7a4e28", "#9a6838", "#b8864e")
SOLE = Ramp("#141010", "#241c18", "#36302a")
CAP = Ramp("#141c16", "#1f2c22", "#2c3e30", "#3e5442", "#566e58")
PANEL = Ramp("#7a6a48", "#a08e66", "#c2b088", "#ddcfa8", "#efe6c8")
LOGO = hexc("#a8281e")
BRASS = Ramp("#5a4214", "#8a6a22", "#b8923a", "#e0c070")


def flannel(x, y):
    """A red plaid: dark check lines every 4 pixels, lighter where they cross the weft."""
    lx, ly = x % 4 == 1, y % 4 == 2
    if lx and ly:
        return mix(FLANNEL[1], CHECK, 0.6)
    if lx or ly:
        return FLANNEL[1] if (x + y) % 2 else mix(FLANNEL[1], CHECK, 0.3)
    return FLANNEL[3] if (x * 3 + y) % 5 else FLANNEL[2]


def denim(x, y, k=3):
    """Denim twill: a diagonal weave, one shade either side of {@code k}."""
    return DENIM[k + 1] if (x + y) % 4 == 0 else DENIM[k - 1] if (x - y) % 5 == 0 else DENIM[k]


def canvas(x, y, k=3):
    return CANVAS[k + 1] if (x * 7 + y * 3) % 11 == 0 else CANVAS[k - 1] if (x + 2 * y) % 9 == 0 else CANVAS[k]


# --- icons --------------------------------------------------------------------------------------------------------------

def hunters_cap():
    """A trucker cap, three-quarters: olive crown, khaki front panel with a red patch, a curved brim."""
    t = Tex(16, 16, 311)
    for y in range(3, 11):
        for x in range(2, 14):
            if math.hypot((x + 0.5 - 8) / 6.0, (y + 0.5 - 10.5) / 7.2) <= 1.0:
                t.set(x, y, CAP[2] if (x + y) % 3 else CAP[1])
    # The front panel (left half, facing us) and its patch.
    for y in range(4, 10):
        for x in range(3, 9):
            if t.get(x, y)[3] and math.hypot((x + 0.5 - 5.6) / 3.2, (y + 0.5 - 8.6) / 5.2) <= 1.0:
                t.set(x, y, PANEL[3] if x > 3 else PANEL[2])
    t.rect(5, 6, 6, 7, LOGO)
    t.set(5, 6, mix(LOGO, PANEL[4], 0.4))
    # Mesh at the back: lighter dots.
    for (x, y) in ((10, 5), (12, 6), (11, 7), (12, 8), (10, 8), (11, 9), (9, 6)):
        t.set(x, y, CAP[3])
    t.set(8, 3, CAP[4])  # the button on top
    # Sweatband line and the brim jutting out to the left.
    t.hline(2, 13, 10, CAP[0])
    for x in range(0, 9):
        y0 = 11 if x > 1 else 12
        t.set(x, y0, CAP[3] if x < 7 else CAP[2])
        t.set(x, y0 + 1, CAP[1])
    t.set(0, 11, CAP[2])
    item_outline(t, K)
    return t


def hunters_jacket():
    """A tan canvas work jacket hanging open over a red flannel shirt, corduroy collar, brass buttons."""
    t = Tex(16, 16, 312)
    # Body and sleeves.
    for y in range(2, 15):
        for x in range(1, 15):
            body = 4 <= x <= 11 and y >= 2
            sleeve = (x <= 3 or x >= 12) and 3 <= y <= 11
            if not (body or sleeve):
                continue
            if body and 6 <= x <= 9 and y >= 3:
                t.set(x, y, flannel(x, y))
            else:
                t.set(x, y, canvas(x, y, 2 if (x in (3, 12) or y == 14) else 3))
    # Shoulders round off.
    for (x, y) in ((1, 3), (14, 3), (1, 4), (14, 4)):
        t.set(x, y, (0, 0, 0, 0))
    # Corduroy collar folded over the shoulders, the shirt's own collar points inside.
    for (x, y) in ((4, 2), (5, 2), (4, 3), (5, 3), (5, 4), (10, 2), (11, 2), (10, 3), (11, 3), (10, 4)):
        t.set(x, y, CORD[3] if (x + y) % 2 else CORD[2])
    for (x, y) in ((6, 2), (7, 3), (9, 2), (8, 3)):
        t.set(x, y, FLANNEL[4])
    t.set(7, 2, (0, 0, 0, 0))
    t.set(8, 2, (0, 0, 0, 0))
    # Jacket front edges, pockets and cuffs.
    for y in range(5, 15):
        t.set(5, y, CANVAS[1])
        t.set(10, y, CANVAS[1])
    for x in (4, 11):
        t.set(x, 9, CANVAS[1])
        t.set(x, 10, CANVAS[4])
    for x in (1, 2, 3, 12, 13, 14):
        t.set(x, 11, CANVAS[1])
    for y in (6, 9, 12):
        t.set(10, y, BRASS[3])
    item_outline(t, K)
    return t


def hunters_jeans():
    """Worn blue jeans: a leather belt with a brass buckle, faded thighs, orange stitching, rolled cuffs."""
    t = Tex(16, 16, 313)
    for y in range(2, 15):
        for x in range(3, 13):
            leg = y >= 7 and (x <= 6 or x >= 9)
            if y < 7 or leg:
                k = 4 if (y in (8, 9, 10) and x in (4, 5, 10, 11)) else 3
                t.set(x, y, denim(x, y, k))
    t.hline(3, 12, 2, LEATHER[3])
    t.hline(3, 12, 3, LEATHER[2])
    t.rect(7, 2, 8, 3, BRASS[3])
    t.set(8, 3, BRASS[1])
    # Fly, pockets and seams in orange thread.
    for y in range(4, 7):
        t.set(8, y, STITCH)
    for (x, y) in ((4, 4), (5, 5), (11, 4), (10, 5)):
        t.set(x, y, STITCH)
    for y in range(8, 14):
        t.set(3, y, DENIM[2])
        t.set(12, y, DENIM[2])
    # Rolled cuffs.
    for x in list(range(3, 7)) + list(range(9, 13)):
        t.set(x, 14, DENIM[5] if x % 2 else DENIM[4])
    item_outline(t, K)
    return t


def boot(t, ox, oy):
    """One work boot seen from the side, toe to the right, at offset (ox, oy): shaft 5 wide, foot 8 long."""
    for y in range(0, 10):
        for x in range(0, 8):
            shaft = x <= 4
            foot = y >= 6
            if not (shaft or foot):
                continue
            if y == 9:
                c = SOLE[1] if x % 3 else SOLE[0]
            elif y == 0:
                c = LEATHER[2]
            else:
                c = LEATHER[4] if (x == 1 and y < 6) else LEATHER[3] if (x + y) % 4 else LEATHER[2]
            t.set(ox + x, oy + y, c)
    t.set(ox + 7, oy + 6, (0, 0, 0, 0))  # round the toe
    t.set(ox + 6, oy + 6, LEATHER[4])
    # Laces up the front of the shaft and a pull tab.
    for y in (2, 4, 6):
        t.set(ox + 4, oy + y, LEATHER[5])
        t.set(ox + 3, oy + y + 1, LEATHER[1])
    t.hline(ox, ox + 4, oy + 1, LEATHER[1])
    t.hline(ox, ox + 7, oy + 8, SOLE[2])


def hunters_boots():
    t = Tex(16, 16, 314)
    boot(t, 1, 2)
    boot(t, 7, 5)
    item_outline(t, K)
    return t


# --- armour layers (vanilla humanoid 64x32) ----------------------------------------------------------------------------

def box_faces(u, v, w, h, d):
    """The six faces of a vanilla box unwrapped at (u, v): name -> (x0, y0, width, height)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


def fill(t, rect, fn):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                t.set(x0 + x, y0 + y, c)


def layer_1():
    """Cap (head), jacket (body and arms), boots (the feet of the legs)."""
    t = Tex(64, 32, 315)
    head = box_faces(0, 0, 8, 8, 8)
    # The cap covers the top half of the head; the front panel is khaki with the patch, the back is mesh with the strap.
    fill(t, head["top"], lambda x, y, w, h: CAP[2] if (x + y) % 3 else CAP[1])
    fill(t, head["front"], lambda x, y, w, h: (LOGO if (3 <= x <= 4 and 1 <= y <= 2) else PANEL[3] if 1 <= x <= 6 and y < 3
                                               else CAP[2] if y < 3 else CAP[0] if y == 3 else None))
    def mesh(x, y):
        return CAP[3] if (x % 2 == 0 and y % 2 == 1) else CAP[2]

    for side in ("right", "left"):
        fill(t, head[side], lambda x, y, w, h: mesh(x, y) if y < 3 else CAP[0] if y == 3 else None)
    fill(t, head["back"], lambda x, y, w, h: (PANEL[1] if (y == 3 and 2 <= x <= 5) else
                                              mesh(x, y) if y < 3 else CAP[0] if y == 3 else None))

    body = box_faces(16, 16, 8, 12, 4)

    def jacket_front(x, y, w, h):
        if 2 <= x <= 5:
            if y == 0 and x in (2, 5):
                return CORD[3]
            return flannel(x, y)
        if y <= 1:
            return CORD[3] if (x + y) % 2 else CORD[2]
        if x in (1, 6):
            return BRASS[3] if (x == 6 and y in (4, 7, 10)) else CANVAS[1]
        if y in (7, 8) and x in (0, 7):
            return CANVAS[4] if y == 7 else CANVAS[1]
        return canvas(x, y, 2 if y == 11 else 3)

    fill(t, body["front"], jacket_front)
    # The back: a yoke seam across the shoulders, a faint centre seam below it.
    fill(t, body["back"], lambda x, y, w, h: CORD[2] if y == 0 else CANVAS[2] if y == 3 or (x == 4 and y > 3)
         else canvas(x, y, 2 if y == 11 else 3))
    fill(t, body["right"], lambda x, y, w, h: CORD[2] if y == 0 else canvas(x, y, 2 if y == 11 else 3))
    fill(t, body["left"], lambda x, y, w, h: CORD[2] if y == 0 else canvas(x, y, 2 if y == 11 else 3))
    fill(t, body["top"], lambda x, y, w, h: CORD[3] if y in (0, 3) else flannel(x, y) if 2 <= x <= 5 else CORD[2])
    fill(t, body["bottom"], lambda x, y, w, h: CANVAS[1])

    arm = box_faces(40, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        fill(t, arm[face], lambda x, y, w, h: CANVAS[1] if y == 9 else CANVAS[2] if y >= 10 else
             (CANVAS[2] if face == "left" and x == 3 else canvas(x, y, 3)))
    fill(t, arm["top"], lambda x, y, w, h: canvas(x, y, 3))
    fill(t, arm["bottom"], lambda x, y, w, h: flannel(x, y))

    leg = box_faces(0, 16, 4, 12, 4)

    def boot_face(face):
        def f(x, y, w, h):
            if y < 7:
                return None
            if y == 11:
                return SOLE[1] if x % 2 else SOLE[0]
            if y == 7:
                return LEATHER[2]
            if face == "front" and y in (8, 9) and x in (1, 2):
                return LEATHER[5] if (x + y) % 2 else LEATHER[1]
            return LEATHER[3] if (x + y) % 3 else LEATHER[2]
        return f

    for face in ("right", "front", "left", "back"):
        fill(t, leg[face], boot_face(face))
    fill(t, leg["bottom"], lambda x, y, w, h: SOLE[0])
    return t


def layer_2():
    """Jeans: the belt and seat on the body's lower rows, denim down both legs to rolled cuffs."""
    t = Tex(64, 32, 316)
    body = box_faces(16, 16, 8, 12, 4)

    def waist(face):
        def f(x, y, w, h):
            if y < 8:
                return None
            if y == 8:
                if face == "front" and x in (3, 4):
                    return BRASS[3] if y == 8 else BRASS[1]
                return LEATHER[3] if x % 3 else LEATHER[2]
            if face == "front" and x == 4 and y > 8:
                return STITCH
            if face == "back" and y in (9, 10) and x in (1, 2, 5, 6):
                return STITCH if y == 9 else denim(x, y, 4)
            return denim(x, y)
        return f

    for face in ("right", "front", "left", "back"):
        fill(t, body[face], waist(face))
    fill(t, body["bottom"], lambda x, y, w, h: DENIM[2])

    leg = box_faces(0, 16, 4, 12, 4)

    def jeans(face):
        def f(x, y, w, h):
            if y == 11:
                return DENIM[5] if x % 2 else DENIM[4]
            if face == "right" and x == 0:
                return STITCH if y % 3 == 0 else DENIM[2]
            k = 4 if (face == "front" and 2 <= y <= 5) else 3
            return denim(x, y, k)
        return f

    for face in ("right", "front", "left", "back"):
        fill(t, leg[face], jeans(face))
    fill(t, leg["top"], lambda x, y, w, h: DENIM[2])
    fill(t, leg["bottom"], lambda x, y, w, h: DENIM[1])
    return t


def generate():
    for tier in range(1, 6):
        save(ascension_shard(tier), "item", "ascension_shard_%d" % tier)
    save(hunters_cap(), "item", "hunters_cap")
    save(hunters_jacket(), "item", "hunters_jacket")
    save(hunters_jeans(), "item", "hunters_jeans")
    save(hunters_boots(), "item", "hunters_boots")
    save(layer_1(), "models/armor", "hunter_layer_1")
    save(layer_2(), "models/armor", "hunter_layer_2")


if __name__ == "__main__":
    generate()
