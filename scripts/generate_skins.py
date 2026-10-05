"""Original mathematical silk textures. No external or proprietary assets."""
from pathlib import Path
from PIL import Image
import math

output = Path(__file__).resolve().parents[1] / 'app/src/main/res/drawable-nodpi'
output.mkdir(parents=True, exist_ok=True)
palettes = {
    'blue': ((2, 7, 23), (4, 76, 230), (51, 161, 255)),
    'violet': ((18, 8, 47), (88, 47, 213), (192, 172, 255)),
    'gold': ((39, 28, 14), (155, 105, 35), (249, 220, 147)),
    'graphite': ((9, 13, 18), (54, 64, 77), (170, 189, 203)),
    'mint': ((1, 28, 32), (9, 107, 117), (104, 236, 206)),
    'ice': ((14, 39, 76), (67, 120, 177), (195, 223, 248)),
}
w, h = 900, 560
field = []
for py in range(h):
    y = py / h
    for px in range(w):
        x = px / w
        warp = y + 0.23 * math.sin(x * 5.1 - 0.65) + 0.09 * math.sin(x * 10 + y * 2)
        fold = (0.5 + 0.5 * math.sin(warp * 11 - x * 3.3)) ** 3
        edge = math.exp(-((warp - 0.63) / 0.021) ** 2)
        glow = math.exp(-((x - 0.2) ** 2 / 0.22 + (y - 0.08) ** 2 / 0.4))
        shade = max(0, min(1, 0.14 + fold * 0.76 + glow * 0.21))
        shine = min(0.52, edge * 0.33 + glow * 0.12)
        field.append((shade, shine))
for name, (dark, mid, light) in palettes.items():
    pixels = []
    for shade, shine in field:
        pixels.append(tuple(int(min(255, (dark[c] * (1-shade) + mid[c] * shade) * (1-shine) + light[c] * shine)) for c in range(3)))
    img = Image.new('RGB', (w, h))
    img.putdata(pixels)
    img.save(output / f'skin_{name}.png', optimize=True)
    print(f'skin_{name}.png')
