"""把霞鹜文楷裁成 Leaf 用的常用字子集（M9）。

用法（在仓库根目录）：
    pip install fonttools
    python tools/fonts/subset_kai.py

做了什么：
1. 从 git 历史里取出完整版文楷（M8 时的提交 a2f58ca 里还是完整版），也可以用 --src 指定文件夹。
2. 字表 = 《通用规范汉字表》一级字 3500 ∪ 《现代汉语常用字表》3500 ∪ GB2312 一级字 3755
   ∪ App 源码（app/src/main 下 .kt/.xml）里出现的所有汉字 ∪ 常用标点、全角符号、ASCII。
3. 用 fontTools 裁剪，写到 app/src/main/res/font/leaf_kai_regular.ttf 和 leaf_kai_medium.ttf，
   同时把实际收进去的汉字写到 tools/fonts/leaf-kai-chars.txt（单元测试 FontCoverageTest 用它检查）。
4. 按 SIL OFL 第 3 条，裁剪后的字体属于“修改版”，不能再用保留字体名（霞鹜 / LXGW），
   所以字体内部名字改成 “Leaf Kai”。版权和许可说明保留。

App 里新加了界面文字、FontCoverageTest 报缺字时，重新跑一次这个脚本就行。
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[2]
HERE = ROOT / "tools" / "fonts"
OUT_DIR = ROOT / "app" / "src" / "main" / "res" / "font"
# 这个提交里 res/font 还是完整版文楷（25MB 一个）。
FULL_FONT_COMMIT = "a2f58ca"
SOURCES = {
    "regular": ("lxgw_wenkai_regular.ttf", "leaf_kai_regular.ttf", "Regular", 400),
    "medium": ("lxgw_wenkai_medium.ttf", "leaf_kai_medium.ttf", "Medium", 500),
}
FAMILY = "Leaf Kai"
HAN = re.compile(r"[㐀-䶿一-鿿豈-﫿]")


def read_list(name: str) -> set[str]:
    return set(HAN.findall((HERE / name).read_text(encoding="utf-8")))


def gb2312_level1() -> set[str]:
    chars = set()
    for hi in range(0xB0, 0xD8):
        for lo in range(0xA1, 0xFF):
            try:
                chars.add(bytes([hi, lo]).decode("gb2312"))
            except UnicodeDecodeError:
                pass
    return chars


def app_chars() -> set[str]:
    """app/src/main 下所有 .kt 和 .xml 里出现的汉字（含注释，宁多勿漏）；规则和 FontCoverageTest 一样。"""
    chars = set()
    for path in (ROOT / "app" / "src" / "main").rglob("*"):
        if path.suffix in (".kt", ".xml") and path.is_file():
            chars |= set(HAN.findall(path.read_text(encoding="utf-8")))
    return chars


def extra_codepoints() -> set[int]:
    ranges = [
        (0x0020, 0x007E),  # ASCII
        (0x00A0, 0x00FF),  # Latin-1 符号（· × ÷ ° 等）
        (0x2000, 0x206F),  # 常用标点（— … ‘’ “” · 等）
        (0x2100, 0x214F),  # 字母式符号（℃ № 等）
        (0x2150, 0x218F),  # 数字形式（Ⅰ Ⅱ 等）
        (0x2190, 0x21FF),  # 箭头
        (0x2460, 0x24FF),  # 带圈数字 ① ②
        (0x2500, 0x257F),  # 制表符
        (0x25A0, 0x25FF),  # 几何图形 ○ ● □ ■ △
        (0x2600, 0x26FF),  # 杂项符号 ☆ ★
        (0x2700, 0x27BF),  # ✓ ✗ 等
        (0x3000, 0x303F),  # 中文标点 、。《》「」【】
        (0x3200, 0x32FF),  # 带圈字 ㈠
        (0xFE30, 0xFE4F),  # 竖排标点兼容
        (0xFF00, 0xFFEF),  # 全角字母数字和标点 ，！？（）
    ]
    return {cp for lo, hi in ranges for cp in range(lo, hi + 1)}


def load_source(src_dir: Path | None, file_name: str, scratch: Path) -> Path:
    if src_dir is not None:
        return src_dir / file_name
    target = scratch / file_name
    if not target.exists():
        print(f"从 git 历史 {FULL_FONT_COMMIT} 取出完整版 {file_name} …")
        data = subprocess.run(
            ["git", "show", f"{FULL_FONT_COMMIT}:app/src/main/res/font/{file_name}"],
            cwd=ROOT, check=True, capture_output=True,
        ).stdout
        target.write_bytes(data)
    return target


def rename(font: TTFont, style: str) -> None:
    """OFL 第 3 条：修改版不能用保留字体名，改成 Leaf Kai；保留版权（ID 0）和许可（ID 13/14）。"""
    names = {
        1: FAMILY if style == "Regular" else f"{FAMILY} {style}",
        2: "Regular",
        3: f"{FAMILY} {style}; subset for Leaf app",
        4: f"{FAMILY} {style}",
        6: f"LeafKai-{style}",
        16: FAMILY,
        17: style,
    }
    table = font["name"]
    for record in list(table.names):
        if record.nameID in names or record.nameID in (18, 21, 22, 25):
            table.removeNames(nameID=record.nameID)
    for name_id, value in names.items():
        table.setName(value, name_id, 3, 1, 0x409)
    table.setName(
        "Subset of LXGW WenKai (SIL OFL 1.1) for the Leaf app; renamed as required by OFL clause 3.",
        10, 3, 1, 0x409,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--src", type=Path, help="放完整版 lxgw_wenkai_regular.ttf / lxgw_wenkai_medium.ttf 的文件夹；不填就从 git 历史取")
    args = parser.parse_args()

    level1 = read_list("tygf-level1-3500.txt")
    common = read_list("xdhy-changyong-3500.txt")
    gb = gb2312_level1()
    used = app_chars()
    han = level1 | common | gb | used
    print(f"通用规范一级 {len(level1)}，现代汉语常用 {len(common)}，GB2312 一级 {len(gb)}，App 源码 {len(used)}")
    print(f"合计汉字 {len(han)} 个")

    scratch = HERE / "src"
    scratch.mkdir(exist_ok=True)
    codepoints = {ord(c) for c in han} | extra_codepoints()
    kept_han: set[str] | None = None
    for key, (src_name, out_name, style, _weight) in SOURCES.items():
        source = load_source(args.src, src_name, scratch)
        font = TTFont(source)
        cmap = font.getBestCmap()
        wanted = sorted(cp for cp in codepoints if cp in cmap)
        missing = sorted(chr(cp) for cp in codepoints if cp not in cmap and HAN.match(chr(cp)))
        if missing:
            print(f"{style}：文楷里本来就没有的字 {len(missing)} 个：{''.join(missing)}")
        options = subset.Options()
        options.layout_features = ["*"]
        options.name_IDs = ["*"]
        options.name_languages = ["*"]
        options.notdef_outline = True
        options.glyph_names = False
        options.hinting = False
        subsetter = subset.Subsetter(options)
        subsetter.populate(unicodes=wanted)
        subsetter.subset(font)
        rename(font, style)
        out = OUT_DIR / out_name
        font.save(out)
        kept = {chr(cp) for cp in font.getBestCmap() if HAN.match(chr(cp))}
        kept_han = kept if kept_han is None else kept_han & kept
        print(f"{style}：{len(kept)} 个汉字，{out.stat().st_size / 1024 / 1024:.2f} MB → {out.relative_to(ROOT)}")

    chars = "".join(sorted(kept_han or set()))
    (HERE / "leaf-kai-chars.txt").write_text(
        "# Leaf Kai 里有的汉字（subset_kai.py 生成，别手改）\n" + chars + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
