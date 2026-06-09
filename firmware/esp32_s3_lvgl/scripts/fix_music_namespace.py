from pathlib import Path

p = Path(__file__).resolve().parents[1] / "main" / "modules" / "music_module.cpp"
t = p.read_text(encoding="utf-8")
old = (
    '  create_side_button(layout, LV_SYMBOL_NEXT, "next");\n'
    "}\n\n\nbool is_active()"
)
new = (
    '  create_side_button(layout, LV_SYMBOL_NEXT, "next");\n'
    "}\n\n}  // namespace\n\nnamespace music_module {\n\nbool is_active()"
)
if old not in t:
    raise SystemExit("pattern not found")
p.write_text(t.replace(old, new, 1), encoding="utf-8")
print("ok")
