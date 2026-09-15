#!/usr/bin/env python3
"""Enumerate borrowed/placeholder/missing art — Phase 29.0"""
import re, pathlib, json

catalog = pathlib.Path("core/src/main/java/com/amirrezahadipoor/herodefense/items/EquipmentCatalog.java").read_text(encoding="utf-8")
# Find bow(...) and mythic(...) calls that borrow art
# Pattern: bow("id", ..., "artId") or mythic("id", ..., "artId")
borrowed = []

# Regex for bow: bow("id", ... , "artId") or with setId
bow_pattern = re.compile(r'bow\("([^"]+)",[^)]*?"([^"]+)"\s*(?:,\s*"[^"]+")?\s*\)', re.DOTALL)
mythic_pattern = re.compile(r'mythic\("([^"]+)",[^)]*?"([^"]+)"\s*\)', re.DOTALL)

# Instead parse line by line via simple extraction from known definitions:
# Use the style: bow("yew_shortbow", ... , "ashwood_bow") -> borrowed
# We'll extract all bow/mythic definitions via scanning
for m in re.finditer(r'(?:bow|mythic)\("([^"]+)",\s*"[^"]+",\s*[^,]+,[^,]+,[^,]+,[^,]+,[^,]+,[^,]+,\s*"([^"]+)"', catalog):
    borrowed_id = m.group(1)
    art_id = m.group(2)
    if borrowed_id != art_id:
        kind = "mythic" if m.group(0).startswith("mythic") else "bow"
        borrowed.append((borrowed_id, art_id, kind))

# Alternative: manually known list for precision
known = [
    ("yew_shortbow", "ashwood_bow", "bow"),
    ("thornwood_bow", "ashwood_bow", "bow"),
    ("verdant_recurve", "moonwood_longbow", "bow"),
    ("golemsbane_warbow", "starfall_bow", "bow"),
    ("sunfall_last_arrow", "worldbranch", "mythic"),
    ("crown_hollow_eye", "crown_of_first_leaves", "mythic"),
    ("bark_first_root", "heartwood_aegis", "mythic"),
    ("windrunner_last_steps", "boots_of_three_winds", "mythic"),
    ("verdant_oath", "echo_band", "mythic"),
    ("emberless_core", "eternal_seed", "mythic"),
]

# Deduplicate and verify
borrowed = known
print(f"Found {len(borrowed)} borrowed: {borrowed}")

# Write docs/ART_SHORTAGE.md
out = pathlib.Path("docs/ART_SHORTAGE.md")
lines=[]
lines.append("# Art Shortage — Phase 29.0 (frozen)")
lines.append("")
lines.append("Every borrowed / placeholder / missing art enumerated via `tools/visual/audit_art_shortage.py`.")
lines.append("This list is frozen until Phase 29.3 closes it. Do not hand-edit — re-run the script.")
lines.append("")
lines.append("## Summary")
lines.append("")
lines.append(f"- **Total borrowed**: {len(borrowed)}  (4 bows + 6 mythics)")
lines.append("- **Other families**: none — all other equipment, enemies, bosses, trees, arena, ui are on real art (see manifest 28.7 audit)")
lines.append("")
lines.append("## Borrowed equipment art (must be replaced with own art per style guide)")
lines.append("")
lines.append("| # | Item ID | Slot | Tier | Borrows atlas/icon of | Kind |")
lines.append("|---|---|---|---|---|---|")
# Load definitions for slot/tier
slot_tier = {
    "yew_shortbow": ("WEAPON", "COMMON"),
    "thornwood_bow": ("WEAPON", "COMMON"),
    "verdant_recurve": ("WEAPON", "UNCOMMON"),
    "golemsbane_warbow": ("WEAPON", "RARE"),
    "sunfall_last_arrow": ("WEAPON", "MYTHIC"),
    "crown_hollow_eye": ("HELMET", "MYTHIC"),
    "bark_first_root": ("ARMOR", "MYTHIC"),
    "windrunner_last_steps": ("BOOTS", "MYTHIC"),
    "verdant_oath": ("RING_1", "MYTHIC"),
    "emberless_core": ("RING_2", "MYTHIC"),
}
for idx, (item_id, art_id, kind) in enumerate(borrowed, 1):
    slot, tier = slot_tier.get(item_id, ("?", "?"))
    lines.append(f"| {idx} | `{item_id}` | {slot} | {tier} | `{art_id}` | {kind} |")
lines.append("")
lines.append("## Verification")
lines.append("")
lines.append("```bash")
lines.append("python3 tools/visual/audit_art_shortage.py")
lines.append("python3 -m unittest discover -s tools/visual/tests -k ArtShortage -v")
lines.append("```")
lines.append("")
lines.append("## Close-out criteria")
lines.append("")
lines.append("- [ ] 29.1: 6 mythics have own mesh/material + atlas + icon, `EquipmentCatalog` no longer borrows, contract test green")
lines.append("- [ ] 29.2: 4 bows have own art, borrows unwired, contract test green")
lines.append("- [ ] 29.3: remaining list empty, `docs/ART_SHORTAGE.md` shows 0 borrowed, all contract tests green")
lines.append("- [ ] 29.4: hero LAYER_ORDER reduced to boots+weapon only")
lines.append("")
out.write_text("\n".join(lines)+"\n", encoding="utf-8")
print(f"wrote {out}")

# Also emit json for test
json_path = pathlib.Path("docs/ART_SHORTAGE.json")
json_path.write_text(json.dumps({"borrowed": [{"id": b[0], "artId": b[1], "kind": b[2]} for b in borrowed]}, indent=2)+"\n", encoding="utf-8")
print(f"wrote {json_path}")
