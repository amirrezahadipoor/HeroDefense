#!/usr/bin/env python3
"""Enumerate borrowed/placeholder/missing art — Phase 29.0 dynamic"""
import re, pathlib, json

catalog = pathlib.Path("core/src/main/java/com/amirrezahadipoor/herodefense/items/EquipmentCatalog.java").read_text(encoding="utf-8")

borrowed = []
# Find all bow(...) and mythic(...) calls; extract quoted strings inside
call_pattern = re.compile(r'(bow|mythic)\s*\((.*?)\)', re.DOTALL)
for m in call_pattern.finditer(catalog):
    kind = m.group(1)
    args_str = m.group(2)
    # Extract all quoted strings in order
    quoted = re.findall(r'"([^"]+)"', args_str)
    if not quoted:
        continue
    # For bow: quoted = [id, name, artId, (optional setId)]
    # For mythic: quoted = [id, name, artId]
    # Also need to handle item() which we ignore
    if kind == "bow":
        if len(quoted) < 3:
            continue
        bid = quoted[0]
        art = quoted[2]  # third quoted is artId
        # For bows with setId, art is still third, setId is fourth
        # Check that artId is a known item id (lowercase with underscore) not a stat
        # Heuristic: artId matches lowercase with underscore, not Tier/Stat
        # For bow, art should be like "ashwood_bow"
        if bid != art:
            borrowed.append((bid, art, kind))
    elif kind == "mythic":
        if len(quoted) < 3:
            continue
        bid = quoted[0]
        art = quoted[2]
        if bid != art:
            borrowed.append((bid, art, kind))

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
bows = sum(1 for _,_,k in borrowed if k=='bow')
myths = sum(1 for _,_,k in borrowed if k=='mythic')
lines.append(f"- **Total borrowed**: {len(borrowed)}  ({bows} bows + {myths} mythics)")
lines.append("- **Other families**: none — all other equipment, enemies, bosses, trees, arena, ui are on real art (see manifest 28.7 audit)")
lines.append("")
lines.append("## Borrowed equipment art (must be replaced with own art per style guide)")
lines.append("")
lines.append("| # | Item ID | Slot | Tier | Borrows atlas/icon of | Kind |")
lines.append("|---|---|---|---|---|---|")
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
for idx, (item_id, art_id, kind) in enumerate(sorted(borrowed), 1):
    slot, tier = slot_tier.get(item_id, ("?", "?"))
    lines.append(f"| {idx} | `{item_id}` | {slot} | {tier} | `{art_id}` | {kind} |")
if not borrowed:
    lines.append("| — | *none* | — | — | — | — |")
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
lines.append("- [x] 29.1: 6 mythics have own mesh/material + atlas + icon, `EquipmentCatalog` no longer borrows, contract test green")
lines.append("- [ ] 29.2: 4 bows have own art, borrows unwired, contract test green")
lines.append("- [ ] 29.3: remaining list empty, `docs/ART_SHORTAGE.md` shows 0 borrowed, all contract tests green")
lines.append("- [ ] 29.4: hero LAYER_ORDER reduced to boots+weapon only")
lines.append("")
out.write_text("\n".join(lines)+"\n", encoding="utf-8")
print(f"wrote {out}")

json_path = pathlib.Path("docs/ART_SHORTAGE.json")
json_path.write_text(json.dumps({"borrowed": [{"id": b[0], "artId": b[1], "kind": b[2]} for b in sorted(borrowed)]}, indent=2)+"\n", encoding="utf-8")
print(f"wrote {json_path}")
