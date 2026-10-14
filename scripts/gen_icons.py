#!/usr/bin/env python3
"""Genera los iconos PNG del launcher (para Android < 8) sin dependencias externas."""
import os
import struct
import sys
import zlib

OUT = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
BG = (0xFF, 0x5A, 0x47)    # bermellón
MASK = (0xF4, 0xF1, 0xEA)  # hueso
EYE = (0x0E, 0x0F, 0x15)   # tinta
SS = 4  # supersampling

# Antifaz en el mismo viewport 108x108 que drawable/ic_launcher_foreground.xml:
# curvas cúbicas (inicio, control 1, control 2, fin) aplanadas a un polígono.
MASK_CURVES = [
    ((24, 46), (24, 40), (30, 38), (38, 39)),
    ((38, 39), (45, 40), (50, 42), (54, 44)),
    ((54, 44), (58, 42), (63, 40), (70, 39)),
    ((70, 39), (78, 38), (84, 40), (84, 46)),
    ((84, 46), (84, 58), (78, 68), (68, 68)),
    ((68, 68), (61, 68), (57, 63), (54, 59)),
    ((54, 59), (51, 63), (47, 68), (40, 68)),
    ((40, 68), (30, 68), (24, 58), (24, 46)),
]


def flatten(curves, steps=24):
    pts = []
    for p0, p1, p2, p3 in curves:
        for i in range(steps):
            t = i / steps
            mt = 1 - t
            pts.append((mt ** 3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t ** 3 * p3[0],
                        mt ** 3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t ** 3 * p3[1]))
    return pts


MASK_POLY = flatten(MASK_CURVES)


def in_poly(x, y, poly):
    inside = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def in_ellipse(x, y, cx, cy, rx, ry):
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1


def in_rrect(x, y, x0, y0, x1, y1, r):
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def color_at(u, v):
    """u, v en coordenadas 0..108 (mismo viewport que el vector)."""
    if not in_rrect(u, v, 0, 0, 108, 108, 24):
        return None
    if in_ellipse(u, v, 40, 52, 7, 5) or in_ellipse(u, v, 68, 53, 7.5, 2.6):
        return EYE
    if in_poly(u, v, MASK_POLY):
        return MASK
    return BG


def render(size):
    rows = []
    for py in range(size):
        row = bytearray()
        for px in range(size):
            acc = [0, 0, 0, 0]
            for sy in range(SS):
                for sx in range(SS):
                    u = (px + (sx + 0.5) / SS) * 108.0 / size
                    v = (py + (sy + 0.5) / SS) * 108.0 / size
                    c = color_at(u, v)
                    if c is not None:
                        acc[0] += c[0]
                        acc[1] += c[1]
                        acc[2] += c[2]
                        acc[3] += 255
            n = SS * SS
            a = acc[3] // n
            if a:
                cov = acc[3] / 255.0
                row += bytes((int(acc[0] / cov), int(acc[1] / cov), int(acc[2] / cov), a))
            else:
                row += b"\0\0\0\0"
        rows.append(bytes(row))
    return rows


def png(size, rows):
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    raw = b"".join(b"\0" + r for r in rows)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


for name, size in SIZES.items():
    d = os.path.join(OUT, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    path = os.path.join(d, "ic_launcher.png")
    with open(path, "wb") as f:
        f.write(png(size, render(size)))
    print("icono", path, size)
