"""
生成游戏打包用的字体子集（src/main/resources/assets/ui/fonts/）。

字体来自 Google Fonts（均为 SIL OFL 1.1 许可）：
  - Noto Sans SC  可变字体  ofl/notosanssc/NotoSansSC[wght].ttf          → 常规 / 粗体两个静态字重
  - Inconsolata   可变字体  ofl/inconsolata/Inconsolata[wdth,wght].ttf   → 特粗（等宽数字，外形接近 Consolas；
                  它的 Bold 比 Consolas Bold 细一档，取 ExtraBold 才与原先的数字粗细相当）

用法（需要 fonttools：pip install fonttools）：
  python tools/fonts/build_fonts.py <NotoSansSC[wght].ttf> <Inconsolata[wdth,wght].ttf>

中文字体只保留：GB2312 全部字符（6763 个汉字 + 符号）+ 源码与 assets/json、assets/levels 里出现过的全部非 ASCII 字符。
新增的文字里出现了生僻字时重跑一次即可；等宽字体只保留 ASCII 与少量符号（只用于数值）。
"""
import pathlib
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "ui" / "fonts"
SCAN = [
    (ROOT / "src" / "main" / "java", "*.java"),
    (ROOT / "src" / "main" / "resources" / "assets", "*.json"),
    (ROOT / "src" / "main" / "resources" / "assets", "*.lang"),
]
EXTRA = "▲▼◀▶●○◆◇■□★☆←→↑↓×÷±…—–·「」『』【】《》〈〉（）：；，。！？、“”‘’～"


def gb2312_chars():
    chars = set()
    for hi in range(0xA1, 0xF8):
        for lo in range(0xA1, 0xFF):
            try:
                chars.add(bytes([hi, lo]).decode("gb2312"))
            except UnicodeDecodeError:
                pass
    return chars


def scanned_chars():
    chars = set()
    for base, pattern in SCAN:
        for path in base.rglob(pattern):
            chars.update(c for c in path.read_text(encoding="utf-8") if ord(c) > 0x7E)
    return chars


def ascii_chars():
    return {chr(c) for c in range(0x20, 0x7F)}


def build(source, axes, chars, out_name):
    font = instancer.instantiateVariableFont(TTFont(source), axes, updateFontNames=True)
    options = subset.Options()
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.notdef_outline = True
    subsetter = subset.Subsetter(options)
    subsetter.populate(text="".join(sorted(chars)))
    subsetter.subset(font)
    target = OUT / out_name
    font.save(target)
    name = font["name"]
    print(f"{out_name}: {target.stat().st_size // 1024} KB, family={name.getDebugName(1)!r}, "
          f"full={name.getDebugName(4)!r}, glyphs={len(font.getGlyphOrder())}")


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    noto, inconsolata = sys.argv[1], sys.argv[2]
    OUT.mkdir(parents=True, exist_ok=True)

    cjk = gb2312_chars() | scanned_chars() | ascii_chars() | set(EXTRA)
    build(noto, {"wght": 400}, cjk, "NotoSansSC-Regular.ttf")
    build(noto, {"wght": 700}, cjk, "NotoSansSC-Bold.ttf")
    build(inconsolata, {"wght": 800, "wdth": 100}, ascii_chars() | set("×÷±…—–·"), "Inconsolata-ExtraBold.ttf")


if __name__ == "__main__":
    main()
