from pathlib import Path
import math

from PIL import Image, ImageDraw


base = Path(__file__).parent
files = sorted(base.glob("page-*.png"))
width = 300
thumbs = []

for file in files:
    image = Image.open(file).convert("RGB")
    height = round(image.height * width / image.width)
    image.thumbnail((width, height))
    canvas = Image.new("RGB", (width, height + 28), "white")
    canvas.paste(image, ((width - image.width) // 2, 28))
    ImageDraw.Draw(canvas).text((8, 6), file.name, fill="black")
    thumbs.append(canvas)

columns = 4
rows = math.ceil(len(thumbs) / columns)
cell_height = max(image.height for image in thumbs)
sheet = Image.new("RGB", (columns * width, rows * cell_height), (220, 220, 220))

for index, image in enumerate(thumbs):
    sheet.paste(image, ((index % columns) * width, (index // columns) * cell_height))

sheet.save(base / "contact-sheet.png")
