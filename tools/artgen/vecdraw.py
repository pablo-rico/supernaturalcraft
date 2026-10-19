"""A tiny anti-aliased vector canvas for glow-line art (magic circles, seals, glyphs).

Everything is drawn as a coverage field (alpha 0..1) plus a brightness field (0..1), then baked to a white/grey + alpha
Tex, so the runtime can tint it. Strokes are distance-field segments (coverage = half width + 0.5 - distance), which keeps
1-2 px lines crisp at 16-128 px; `crisp=True` snaps coverage to hard pixels for small sprites.
"""

import math

from pixelkit import Tex


def _clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


class Field:
    def __init__(self, w, h=None):
        self.w = w
        self.h = h if h is not None else w
        self.a = [[0.0] * self.w for _ in range(self.h)]
        self.v = [[1.0] * self.w for _ in range(self.h)]
        self.wrap_x = False

    # -- compositing ---------------------------------------------------------------------------------------------------

    def _put(self, x, y, cov, value):
        if self.wrap_x:
            x %= self.w
        if not (0 <= x < self.w and 0 <= y < self.h) or cov <= 0:
            return
        if cov > self.a[y][x]:
            self.a[y][x] = cov
            self.v[y][x] = value
        elif value > self.v[y][x] and cov > 0.5 * self.a[y][x]:
            self.v[y][x] = max(self.v[y][x], value * cov / max(cov, self.a[y][x]))

    def _cut(self, x, y, cov):
        if self.wrap_x:
            x %= self.w
        if 0 <= x < self.w and 0 <= y < self.h and cov > 0:
            self.a[y][x] = min(self.a[y][x], 1.0 - cov)

    def _bbox(self, x0, y0, x1, y1, pad):
        xa = int(math.floor(min(x0, x1) - pad))
        xb = int(math.ceil(max(x0, x1) + pad))
        ya = max(0, int(math.floor(min(y0, y1) - pad)))
        yb = min(self.h - 1, int(math.ceil(max(y0, y1) + pad)))
        if not self.wrap_x:
            xa, xb = max(0, xa), min(self.w - 1, xb)
        return xa, xb, ya, yb

    # -- primitives (coordinates in pixels, pixel centres at +0.5) --------------------------------------------------------

    def seg(self, x0, y0, x1, y1, width=1.0, alpha=1.0, value=1.0, cut=False):
        hw = width / 2.0
        dx, dy = x1 - x0, y1 - y0
        L2 = dx * dx + dy * dy
        xa, xb, ya, yb = self._bbox(x0, y0, x1, y1, hw + 1.5)
        for y in range(ya, yb + 1):
            py = y + 0.5
            for x in range(xa, xb + 1):
                px = x + 0.5
                if L2 == 0:
                    d = math.hypot(px - x0, py - y0)
                else:
                    t = _clamp(((px - x0) * dx + (py - y0) * dy) / L2)
                    d = math.hypot(px - (x0 + t * dx), py - (y0 + t * dy))
                cov = _clamp(hw + 0.5 - d) * alpha
                if cut:
                    self._cut(x, y, cov)
                else:
                    self._put(x, y, cov, value)

    def poly(self, pts, width=1.0, alpha=1.0, value=1.0, closed=False, cut=False):
        n = len(pts)
        for i in range(n if closed else n - 1):
            a, b = pts[i], pts[(i + 1) % n]
            self.seg(a[0], a[1], b[0], b[1], width, alpha, value, cut)

    def ring(self, cx, cy, r, width=1.0, alpha=1.0, value=1.0, cut=False):
        hw = width / 2.0
        xa, xb, ya, yb = self._bbox(cx - r, cy - r, cx + r, cy + r, hw + 1.5)
        for y in range(ya, yb + 1):
            for x in range(xa, xb + 1):
                d = abs(math.hypot(x + 0.5 - cx, y + 0.5 - cy) - r)
                cov = _clamp(hw + 0.5 - d) * alpha
                if cut:
                    self._cut(x, y, cov)
                else:
                    self._put(x, y, cov, value)

    def arc(self, cx, cy, r, a0, a1, width=1.0, alpha=1.0, value=1.0, cut=False, ry=None):
        """Arc in degrees (0 = +x, 90 = +y i.e. down), drawn as a fine polyline. `ry` makes it elliptical."""
        ry = r if ry is None else ry
        n = max(6, int(abs(a1 - a0) / 360 * max(r, ry) * 2 * math.pi / 1.5))
        pts = [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)),
                cy + ry * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]
        self.poly(pts, width, alpha, value, cut=cut)

    def disc(self, cx, cy, r, alpha=1.0, value=1.0, cut=False):
        xa, xb, ya, yb = self._bbox(cx - r, cy - r, cx + r, cy + r, 1.5)
        for y in range(ya, yb + 1):
            for x in range(xa, xb + 1):
                cov = _clamp(r + 0.5 - math.hypot(x + 0.5 - cx, y + 0.5 - cy)) * alpha
                if cut:
                    self._cut(x, y, cov)
                else:
                    self._put(x, y, cov, value)

    def fill(self, pts, alpha=1.0, value=1.0, cut=False, ss=3):
        """Fills a polygon (even-odd), supersampled ss x ss."""
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        xa, xb, ya, yb = self._bbox(min(xs), min(ys), max(xs), max(ys), 1)
        n = len(pts)
        for y in range(ya, yb + 1):
            for x in range(xa, xb + 1):
                hits = 0
                for sy in range(ss):
                    py = y + (sy + 0.5) / ss
                    for sx in range(ss):
                        px = x + (sx + 0.5) / ss
                        inside = False
                        j = n - 1
                        for i in range(n):
                            xi, yi = pts[i]
                            xj, yj = pts[j]
                            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                                inside = not inside
                            j = i
                        hits += inside
                cov = hits / (ss * ss) * alpha
                if cut:
                    self._cut(x, y, cov)
                else:
                    self._put(x, y, cov, value)

    def radial(self, cx, cy, r, profile, value=1.0):
        """Soft radial glow: alpha = profile(d / r) for d < r."""
        xa, xb, ya, yb = self._bbox(cx - r, cy - r, cx + r, cy + r, 1)
        for y in range(ya, yb + 1):
            for x in range(xa, xb + 1):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
                if d < 1:
                    self._put(x, y, _clamp(profile(d)), value)

    # -- post ----------------------------------------------------------------------------------------------------------

    def glow(self, radius=2, strength=0.45, value=1.0):
        """Adds a soft halo round everything drawn so far (box blur, two passes)."""
        g = [row[:] for row in self.a]
        for _ in range(2):
            g = _blur(g, radius, self.wrap_x)
        for y in range(self.h):
            for x in range(self.w):
                add = g[y][x] * strength
                if add > self.a[y][x]:
                    self.v[y][x] = value if self.a[y][x] < 0.05 else self.v[y][x]
                    self.a[y][x] = self.a[y][x] + (1 - self.a[y][x]) * add

    def bake(self, crisp=False, threshold=0.45, alpha_scale=1.0, min_alpha=0.04):
        t = Tex(self.w, self.h)
        for y in range(self.h):
            for x in range(self.w):
                a = self.a[y][x] * alpha_scale
                if crisp:
                    a = (1.0 if a >= threshold else 0.0) * alpha_scale
                if a < min_alpha:
                    continue
                v = int(round(255 * _clamp(self.v[y][x])))
                t.set(x, y, (v, v, v, int(round(255 * _clamp(a)))))
        return t


def _blur(g, r, wrap):
    h, w = len(g), len(g[0])
    out = [[0.0] * w for _ in range(h)]
    n = 2 * r + 1
    for y in range(h):
        row = g[y]
        for x in range(w):
            s = 0.0
            for k in range(-r, r + 1):
                xx = x + k
                if wrap:
                    s += row[xx % w]
                elif 0 <= xx < w:
                    s += row[xx]
            out[y][x] = s / n
    out2 = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            s = 0.0
            for k in range(-r, r + 1):
                yy = y + k
                if 0 <= yy < h:
                    s += out[yy][x]
            out2[y][x] = s / n
    return out2


# -- glyph strokes ---------------------------------------------------------------------------------------------------------
# A glyph is a list of strokes in a unit box (x right, y down): ("p", [(x, y), ...]) polyline, ("c", cx, cy, r, a0, a1) arc,
# ("d", cx, cy, r) dot. draw_glyph maps the box onto any square, optionally rotated, so the same runes serve a 16 px
# particle, a band of a 128 px circle and a 32 px strip cell.

def draw_glyph(f, glyph, cx, cy, size, width=1.0, rot=0.0, alpha=1.0, value=1.0):
    ca, sa = math.cos(rot), math.sin(rot)

    def m(x, y):
        x, y = (x - 0.5) * size, (y - 0.5) * size
        return cx + x * ca - y * sa, cy + x * sa + y * ca

    for s in glyph:
        if s[0] == "p":
            f.poly([m(*p) for p in s[1]], width, alpha, value)
        elif s[0] == "c":
            _, gx, gy, r, a0, a1 = s
            n = max(6, int(abs(a1 - a0) / 20))
            pts = [(gx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)),
                    gy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]
            f.poly([m(*p) for p in pts], width, alpha, value)
        elif s[0] == "d":
            _, gx, gy, r = s
            x, y = m(gx, gy)
            f.disc(x, y, r * size, alpha, value)
