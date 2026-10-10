"""Asset access for structview: the vanilla client jar (zipfile), the mod's own resources, textures and colours.

Assets   -- reads `assets/<ns>/...` from the client jar, then from the repo's src/main|generated/resources.
Texture  -- a decoded texture (first frame of animated strips): RGBA bytes, average colour, alpha class.
tint()   -- the fixed tint a tinted face gets (grass, foliage, water...).
reports  -- vanilla block properties and defaults from the data generator's blocks.json report (cached).
"""

import glob
import json
import os
import subprocess
import sys
import tempfile
import zipfile

import pngx

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
CACHE = os.path.join(tempfile.gettempdir(), "structview_cache")


def find_jar(name="minecraft_1.21.1_client.jar"):
    home = os.path.expanduser("~")
    p = os.path.join(home, ".gradle", "caches", "neoformruntime", "artifacts", name)
    if os.path.exists(p):
        return p
    hits = glob.glob(os.path.join(home, ".gradle", "caches", "**", name), recursive=True)
    if hits:
        return hits[0]
    raise FileNotFoundError("cannot find %s under ~/.gradle (use --jar)" % name)


class Assets:
    def __init__(self, jar=None):
        self.jar_path = jar or find_jar()
        self.zip = zipfile.ZipFile(self.jar_path)
        self.names = set(self.zip.namelist())
        self.dirs = [os.path.join(REPO, "src", "generated", "resources"), os.path.join(REPO, "src", "main", "resources")]
        self._json = {}
        self._tex = {}

    def read(self, rel):
        """Bytes of `assets/...` (or None)."""
        if rel in self.names:
            return self.zip.read(rel)
        for d in self.dirs:
            p = os.path.join(d, rel)
            if os.path.exists(p):
                with open(p, "rb") as f:
                    return f.read()
        return None

    def json(self, rel):
        if rel not in self._json:
            b = self.read(rel)
            try:
                self._json[rel] = json.loads(b.decode("utf-8")) if b is not None else None
            except ValueError:
                self._json[rel] = None
        return self._json[rel]

    def texture(self, ref):
        """A texture by resource location (`minecraft:block/stone`), or None."""
        if ref in self._tex:
            return self._tex[ref]
        ns, path = split(ref)
        b = self.read("assets/%s/textures/%s.png" % (ns, path))
        t = None
        if b is not None:
            try:
                t = Texture(*pngx.read(b))
            except Exception:
                t = None
        self._tex[ref] = t
        return t


def split(ref):
    if ":" in ref:
        ns, path = ref.split(":", 1)
    else:
        ns, path = "minecraft", ref
    return ns, path


class Texture:
    """RGBA texture; animated strips keep their first (square) frame."""

    def __init__(self, w, h, rgba):
        if h > w and h % w == 0:
            rgba = rgba[:w * w * 4]
            h = w
        self.w, self.h, self.rgba = w, h, rgba
        n = w * h
        sr = sg = sb = sa = 0
        minA = 255
        partial = 0
        for i in range(0, n * 4, 4):
            a = rgba[i + 3]
            sr += rgba[i] * a
            sg += rgba[i + 1] * a
            sb += rgba[i + 2] * a
            sa += a
            if a < minA:
                minA = a
            if 0 < a < 255:
                partial += 1
        if sa == 0:
            self.avg = (128, 128, 128)
        else:
            self.avg = (sr // sa, sg // sa, sb // sa)
        self.coverage = sa / (255.0 * n)
        # opaque: no holes; cutout: holes of alpha 0 only; translucent: soft alpha over a good part of it.
        if minA == 255:
            self.kind = "opaque"
        elif partial > n * 0.25:
            self.kind = "translucent"
        else:
            self.kind = "cutout"
        # Packed colours for fast sampling: list of (r, g, b, a).
        self.px = [tuple(rgba[i:i + 4]) for i in range(0, n * 4, 4)]


# --- tints ------------------------------------------------------------------------------------------------------------

GRASS = (0x91, 0xBD, 0x59)
FOLIAGE = (0x77, 0xAB, 0x2F)
BIRCH = (0x80, 0xA7, 0x55)
SPRUCE = (0x61, 0x99, 0x61)
WATER = (0x3F, 0x76, 0xE4)
LILY = (0x20, 0x80, 0x30)

_GRASS_LIKE = ("grass_block", "short_grass", "tall_grass", "fern", "large_fern", "potted_fern", "sugar_cane")


def tint(block_path, state_props=None):
    """The fixed tint for a tinted face of a block (by its id path)."""
    p = block_path
    if p == "birch_leaves":
        return BIRCH
    if p == "spruce_leaves":
        return SPRUCE
    if p.endswith("leaves") or p in ("vine", "melon_stem", "pumpkin_stem", "attached_melon_stem", "attached_pumpkin_stem"):
        if p in ("cherry_leaves", "azalea_leaves", "flowering_azalea_leaves"):
            return None
        return FOLIAGE
    if p in _GRASS_LIKE:
        return GRASS
    if p == "water" or p.endswith("cauldron") or p == "bubble_column":
        return WATER
    if p == "lily_pad":
        return LILY
    if p == "redstone_wire":
        power = int((state_props or {}).get("power", "0"))
        return (int(77 + 178 * power / 15), 0, 0)
    if p == "pink_petals":
        return GRASS
    return GRASS


# --- vanilla block report ---------------------------------------------------------------------------------------------

_REPORT = None


def server_jar(client_jar=None):
    d = os.path.dirname(client_jar or find_jar())
    p = os.path.join(d, "minecraft_1.21.1_server.jar")
    return p if os.path.exists(p) else None


def report(client_jar=None, quiet=True):
    """{id: {"properties": {name: [values]}, "default": {name: value}}} from the data generator (cached), or None."""
    global _REPORT
    if _REPORT is not None:
        return _REPORT or None
    os.makedirs(CACHE, exist_ok=True)
    cached = os.path.join(CACHE, "blocks_1_21_1.json")
    if not os.path.exists(cached):
        jar = server_jar(client_jar)
        if jar is None:
            _REPORT = {}
            return None
        out = tempfile.mkdtemp(prefix="structview_reports_")
        try:
            subprocess.run(["java", "-DbundlerMainClass=net.minecraft.data.Main", "-jar", jar, "--reports", "--output", out],
                           cwd=out, stdout=subprocess.DEVNULL if quiet else None, stderr=subprocess.DEVNULL if quiet else None,
                           check=True, timeout=600)
            with open(os.path.join(out, "reports", "blocks.json"), encoding="utf-8") as f:
                raw = json.load(f)
        except Exception as e:  # no java, no network-free generator...
            print("structview: block report unavailable (%s)" % e, file=sys.stderr)
            _REPORT = {}
            return None
        slim = {}
        for bid, b in raw.items():
            props = b.get("properties", {})
            default = {}
            for s in b.get("states", []):
                if s.get("default"):
                    default = s.get("properties", {})
                    break
            slim[bid] = {"properties": props, "default": default}
        with open(cached, "w", encoding="utf-8") as f:
            json.dump(slim, f)
    with open(cached, encoding="utf-8") as f:
        _REPORT = json.load(f)
    return _REPORT


HEADER = ("# Vanilla 1.21.1 block ids and their state properties (generated by tools/structview/structview.py --extract-ids).\n"
          "# Format: <id> [<property>=<value>|<value>...]...\n")


def extract_ids(jar=None):
    """The text of src/test/resources/vanilla_blocks_1_21_1.txt."""
    rep = report(jar)
    lines = []
    if rep:
        for bid in sorted(rep):
            props = rep[bid]["properties"]
            parts = [bid] + ["%s=%s" % (k, "|".join(sorted(props[k]))) for k in sorted(props)]
            lines.append(" ".join(parts))
    else:
        z = zipfile.ZipFile(jar or find_jar())
        ids = set()
        for n in z.namelist():
            if n.startswith("assets/minecraft/blockstates/") and n.endswith(".json"):
                ids.add("minecraft:" + n[len("assets/minecraft/blockstates/"):-5])
        ids -= {"minecraft:item_frame", "minecraft:glow_item_frame"}
        lines = sorted(ids)
    return HEADER + "\n".join(lines) + "\n"
