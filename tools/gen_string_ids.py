#!/usr/bin/env python3
"""
UI-র লেখা (strings) পরিচালনার টুল।

কাঠামো
  app/src/main/res/values/strings.xml      ইংরেজি (সোর্স — Weblate এটাকেই বেস ধরে)
  app/src/main/res/values-bn/strings.xml   বাংলা
  app/src/main/res/values-<কোড>/strings.xml  আরও ভাষা (Weblate/হাতে)
  app/src/main/java/com/tubex/app/util/StringIds.kt   (জেনারেটেড) বাংলা লেখা -> R.string আইডি

কোডে লেখা হয় `L.t("বাংলা লেখা")`; বাংলা লেখাটাই "কী"। অন্য ভাষায় L.t() ওই কী দিয়ে StringIds থেকে আইডি
খুঁজে রিসোর্স থেকে অনুবাদ আনে।

নতুন লেখা যোগ করার নিয়ম
  1. কোডে L.t("নতুন বাংলা লেখা") লিখুন।
  2. values/strings.xml-এ ইংরেজি এবং values-bn/strings.xml-এ একই নামে বাংলা (হুবহু কোডের লেখা) যোগ করুন।
  3. `python3 tools/gen_string_ids.py` চালান (StringIds.kt নতুন হবে)।
  4. `python3 tools/gen_string_ids.py --check` দিয়ে যাচাই করুন (CI-ও এটা চালায়)।
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
SRC = ROOT / "app/src/main/java"
OUT = SRC / "com/tubex/app/util/StringIds.kt"
EN_XML = RES / "values/strings.xml"
BN_XML = RES / "values-bn/strings.xml"
PLACEHOLDER = re.compile(r"\{\d+\}")


# ---------- Kotlin লিটারেল ----------
def kt_unescape(s: str) -> str:
    def rep(m):
        t = m.group(1)
        if t[0] == "u":
            return chr(int(t[1:], 16))
        return {"n": "\n", "t": "\t", '"': '"', "\\": "\\", "$": "$", "'": "'"}.get(t, t)
    return re.sub(r"\\(u[0-9a-fA-F]{4}|.)", rep, s)


def kt_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("\n", "\\n")


# ---------- Android XML লিটারেল ----------
def xml_escape(s: str) -> str:
    quote = s != s.strip() or "  " in s          # শুরু/শেষের স্পেস রাখতে কোটেশনে মোড়াতে হয়
    s = s.replace("\\", "\\\\").replace('"', '\\"').replace("'", "\\'").replace("\n", "\\n")
    if s[:1] in ("@", "?"):
        s = "\\" + s
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return f'"{s}"' if quote else s


def xml_unescape(s: str) -> str:
    s = s.strip() if not (s.startswith('"') and s.endswith('"') and len(s) >= 2) else s[1:-1]
    def rep(m):
        t = m.group(1)
        if t[0] == "u":
            return chr(int(t[1:], 16))
        return {"n": "\n", "t": "\t"}.get(t, t)
    return re.sub(r"\\(u[0-9a-fA-F]{4}|.)", rep, s)


def read_xml(path: Path) -> dict:
    out = {}
    for el in ET.parse(path).getroot().iter("string"):
        if el.get("translatable") == "false":
            continue
        out[el.get("name")] = xml_unescape(el.text or "")
    return out


def write_xml(path: Path, entries, translatable_extra: str = ""):
    """entries: [(name, text)] বা [("#", কমেন্ট)]"""
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    if translatable_extra:
        lines.append(translatable_extra)
    for name, text in entries:
        if name == "#":
            lines.append(f"    <!-- {text} -->")
        else:
            lines.append(f'    <string name="{name}" formatted="false">{xml_escape(text)}</string>')
    lines.append("</resources>\n")
    path.write_text("\n".join(lines), encoding="utf-8")


# ---------- জেনারেট ----------
def generate() -> str:
    bn = read_xml(BN_XML)
    seen = {}
    for name, text in bn.items():
        if text in seen:
            sys.exit(f"একই বাংলা লেখা দুই নামে: {seen[text]} ও {name}")
        seen[text] = name
    rows = "\n".join(f'    "{kt_escape(t)}" to R.string.{n},' for n, t in bn.items())
    return (
        "// জেনারেটেড — হাতে বদলাবেন না। উৎস: res/values-bn/strings.xml; চালান: python3 tools/gen_string_ids.py\n"
        "package com.tubex.app.util\n\n"
        "import com.tubex.app.R\n\n"
        "/** বাংলা (কোডে ব্যবহৃত কী) লেখা → string রিসোর্স আইডি */\n"
        "internal val BN_STRING_IDS: Map<String, Int> = mapOf(\n" + rows + "\n)\n"
    )


def call_site_literals():
    pat = re.compile(r'\bL\.[tf]\(\s*"((?:[^"\\]|\\.)*)"')
    found = {}
    for p in SRC.rglob("*.kt"):
        if p == OUT:
            continue
        for m in pat.finditer(p.read_text(encoding="utf-8")):
            found.setdefault(kt_unescape(m.group(1)), p.relative_to(ROOT))
    return found


def check() -> int:
    errors = []
    en, bn = read_xml(EN_XML), read_xml(BN_XML)
    for n in sorted(set(bn) - set(en)):
        errors.append(f"values/ এ নেই (ইংরেজি দরকার): {n}")
    for n in sorted(set(en) - set(bn)):
        errors.append(f"values-bn/ এ নেই (বাংলা দরকার): {n}")
    for n in sorted(set(en) & set(bn)):
        if set(PLACEHOLDER.findall(en[n])) != set(PLACEHOLDER.findall(bn[n])):
            errors.append(f"প্লেসহোল্ডার মেলে না: {n}")
    for res_dir in sorted(RES.glob("values-*")):
        if res_dir.name == "values-bn" or not (res_dir / "strings.xml").exists():
            continue
        other = read_xml(res_dir / "strings.xml")
        for n in sorted(set(other) - set(bn)):
            errors.append(f"{res_dir.name}: অজানা নাম {n}")
        for n in sorted(set(other) & set(bn)):
            if set(PLACEHOLDER.findall(other[n])) != set(PLACEHOLDER.findall(bn[n])):
                errors.append(f"{res_dir.name}: প্লেসহোল্ডার মেলে না: {n}")
    bn_texts = set(bn.values())
    for lit, where in sorted(call_site_literals().items()):
        if lit not in bn_texts:
            errors.append(f"কোডের লেখা values-bn/ এ নেই: {lit!r} ({where})")
    if not OUT.exists() or OUT.read_text(encoding="utf-8") != generate():
        errors.append("StringIds.kt পুরনো — `python3 tools/gen_string_ids.py` চালান")
    for e in errors:
        print("ERROR:", e)
    print(f"{len(bn)} টি লেখা, {len(errors)} টি ত্রুটি")
    return 1 if errors else 0


if __name__ == "__main__":
    if "--check" in sys.argv:
        sys.exit(check())
    OUT.write_text(generate(), encoding="utf-8")
    print("লেখা হয়েছে:", OUT.relative_to(ROOT))
