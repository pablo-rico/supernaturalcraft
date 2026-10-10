"""Minimal PNG reader/writer (stdlib only).

read(data_or_path) -> (width, height, rgba bytearray)
  Colour types 0 (grey), 2 (RGB), 3 (palette), 4 (grey+alpha), 6 (RGBA); bit depths 1, 2, 4, 8 and 16;
  palette transparency (tRNS) for types 0/2/3; all five scanline filters. Interlaced images are not supported.
write(path, width, height, pixels, alpha=False)
  pixels: bytes/bytearray of RGB (or RGBA when alpha=True), 8 bits per channel.
"""

import struct
import zlib

_SIG = b"\x89PNG\r\n\x1a\n"


def _paeth(a, b, c):
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def _unfilter(raw, height, stride, bpp):
    out = bytearray(height * stride)
    prev = bytearray(stride)
    pos = 0
    for y in range(height):
        ft = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        if ft == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif ft == 3:
            for i in range(stride):
                left = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((left + prev[i]) >> 1)) & 255
        elif ft == 4:
            for i in range(stride):
                left = line[i - bpp] if i >= bpp else 0
                upl = prev[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + _paeth(left, prev[i], upl)) & 255
        elif ft != 0:
            raise ValueError("bad PNG filter %d" % ft)
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return out


def read(src):
    """Decodes a PNG (bytes or a path) into (w, h, RGBA bytearray)."""
    if isinstance(src, (str, bytes)) and not (isinstance(src, bytes) and src[:8] == _SIG):
        with open(src, "rb") as f:
            data = f.read()
    else:
        data = bytes(src)
    if data[:8] != _SIG:
        raise ValueError("not a PNG")
    pos = 8
    idat = []
    palette = None
    trns = None
    w = h = depth = ctype = interlace = None
    while pos < len(data):
        n, typ = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        pos += 12 + n
        if typ == b"IHDR":
            w, h, depth, ctype, _comp, _filt, interlace = struct.unpack(">IIBBBBB", body)
        elif typ == b"PLTE":
            palette = body
        elif typ == b"tRNS":
            trns = body
        elif typ == b"IDAT":
            idat.append(body)
        elif typ == b"IEND":
            break
    if interlace:
        raise ValueError("interlaced PNGs are not supported")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bits = channels * depth
    stride = (w * bits + 7) // 8
    bpp = max(1, bits // 8)
    raw = _unfilter(zlib.decompress(b"".join(idat)), h, stride, bpp)
    out = bytearray(w * h * 4)
    o = 0
    if depth == 16:
        # Keep the high byte of every sample.
        samples = raw[0::2]
        sstride = stride // 2
    else:
        samples = raw
        sstride = stride
    for y in range(h):
        row = y * sstride
        if depth < 8:
            per = 8 // depth
            mask = (1 << depth) - 1
            vals = []
            line = raw[y * stride:(y + 1) * stride]
            for byte in line:
                for k in range(per):
                    vals.append((byte >> (8 - depth * (k + 1))) & mask)
            vals = vals[:w]
            for v in vals:
                if ctype == 3:
                    r, g, b = palette[v * 3:v * 3 + 3]
                    a = trns[v] if trns is not None and v < len(trns) else 255
                else:
                    g8 = v * 255 // mask
                    r = g = b = g8
                    a = 255
                    if trns is not None and len(trns) >= 2 and v == struct.unpack(">H", trns[:2])[0]:
                        a = 0
                out[o:o + 4] = bytes((r, g, b, a))
                o += 4
            continue
        for x in range(w):
            i = row + x * channels
            if ctype == 6:
                out[o:o + 4] = samples[i:i + 4]
            elif ctype == 2:
                r, g, b = samples[i], samples[i + 1], samples[i + 2]
                a = 255
                if trns is not None and len(trns) >= 6:
                    tr = struct.unpack(">HHH", trns[:6])
                    if depth == 8 and (r, g, b) == tr:
                        a = 0
                out[o:o + 4] = bytes((r, g, b, a))
            elif ctype == 0:
                v = samples[i]
                a = 255
                if trns is not None and len(trns) >= 2 and depth == 8 and v == struct.unpack(">H", trns[:2])[0]:
                    a = 0
                out[o:o + 4] = bytes((v, v, v, a))
            elif ctype == 4:
                v, a = samples[i], samples[i + 1]
                out[o:o + 4] = bytes((v, v, v, a))
            else:  # palette, 8 bit
                v = samples[i]
                r, g, b = palette[v * 3:v * 3 + 3]
                a = trns[v] if trns is not None and v < len(trns) else 255
                out[o:o + 4] = bytes((r, g, b, a))
            o += 4
    return w, h, out


def _chunk(typ, body):
    return struct.pack(">I", len(body)) + typ + body + struct.pack(">I", zlib.crc32(typ + body) & 0xFFFFFFFF)


def encode(width, height, pixels, alpha=False):
    ch = 4 if alpha else 3
    stride = width * ch
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        raw += pixels[y * stride:(y + 1) * stride]
    return (_SIG + _chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6 if alpha else 2, 0, 0, 0))
            + _chunk(b"IDAT", zlib.compress(bytes(raw), 6)) + _chunk(b"IEND", b""))


def write(path, width, height, pixels, alpha=False):
    with open(path, "wb") as f:
        f.write(encode(width, height, pixels, alpha))
