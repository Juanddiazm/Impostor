#!/usr/bin/env python3
"""Genera los iconos PNG del launcher (para Android < 8) sin dependencias externas."""
import os
import struct
import sys
import zlib

OUT = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
BG = (0x0B, 0x10, 0x20)
HOOD = (0xFF, 0x4D, 0x6D)
FACE = BG
EYE = (0xF5, 0xF5, 0xF7)
SS = 3  # supersampling


def in_circle(x, y, cx, cy, r):
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def in_rrect(x, y, x0, y0, x1, y1, r):
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def in_tri(x, y, a, b, c):
    def s(p, q):
        return (x - q[0]) * (p[1] - q[1]) - (p[0] - q[0]) * (y - q[1])
    d1, d2, d3 = s(a, b), s(b, c), s(c, a)
    neg = d1 < 0 or d2 < 0 or d3 < 0
    pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (neg and pos)


def color_at(u, v):
    """u, v en coordenadas 0..108 (mismo viewport que el vector)."""
    # Fondo: cuadrado redondeado que ocupa todo el icono
    if not in_rrect(u, v, 0, 0, 108, 108, 20):
        return None
    col = BG
    # Capucha
    if in_circle(u, v, 54, 48, 26) or in_rrect(u, v, 28, 48, 80, 80, 6):
        col = HOOD
        # Abertura de la cara
        if in_circle(u, v, 54, 52, 16) or in_rrect(u, v, 38, 52, 70, 70, 3):
            col = FACE
            if in_tri(u, v, (42, 52), (50, 56), (42, 59)) or in_tri(u, v, (66, 52), (58, 56), (66, 59)):
                col = EYE
    if in_rrect(u, v, 52, 84, 56, 88, 0.5):
        col = EYE
    return col


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
