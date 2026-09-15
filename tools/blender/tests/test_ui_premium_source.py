from __future__ import annotations

import ast
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ENVIRONMENT = ROOT / "tools/blender/hd_pipeline/environment.py"
GENERATOR = ROOT / "tools/blender/generate_assets.py"
RUNTIME = ROOT / "core/src/main/java/com/amirrezahadipoor/herodefense/render/UiFrameRenderer.java"
REWARD_RENDERER = ROOT / "core/src/main/java/com/amirrezahadipoor/herodefense/render/RewardCardOverlayRenderer.java"
REWARD_ID = ROOT / "core/src/main/java/com/amirrezahadipoor/herodefense/rewards/RewardCardId.java"
GAME = ROOT / "core/src/main/java/com/amirrezahadipoor/herodefense/HeroDefenseGame.java"
STYLE = ROOT / "docs/VISUAL_STYLE_GUIDE.md"


class UiPremiumSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.environment = ENVIRONMENT.read_text(encoding="utf-8")
        cls.generator = GENERATOR.read_text(encoding="utf-8")
        cls.runtime = RUNTIME.read_text(encoding="utf-8")
        cls.reward_renderer = REWARD_RENDERER.read_text(encoding="utf-8")
        cls.reward_id = REWARD_ID.read_text(encoding="utf-8")
        cls.game = GAME.read_text(encoding="utf-8")
        cls.style = STYLE.read_text(encoding="utf-8")
        cls.tree = ast.parse(cls.environment)

    def test_exact_ui_batch_contains_all_icons_and_twelve_skin_states(self) -> None:
        icon_keys = (
            "ui_health", "ui_wave", "ui_coin", "ui_pause", "ui_speed",
            "ui_inventory", "ui_shop", "ui_settings", "ui_restart",
            "ui_new_game", "ui_continue", "ui_close", "ui_strength",
            "ui_agility", "ui_luck", "ui_dodge",
        )
        for key in icon_keys:
            self.assertIn(f'"{key}"', self.environment)
        self.assertIn('UI_FRAME_KINDS = ("button", "panel", "slot")', self.environment)
        self.assertIn(
            'UI_FRAME_STATES = ("normal", "pressed", "selected", "disabled")',
            self.environment,
        )
        self.assertIn("for key in UI_FRAME_KEYS", self.generator)
        self.assertIn("for key in UI_ICON_KEYS", self.generator)
        self.assertIn('"assetKind": "uiFrame"', self.generator)

    def test_supplement_covers_every_potion_and_reward_card_semantic(self) -> None:
        for key in ("ui_general_power", "ui_lifesteal"):
            self.assertIn(f'"{key}"', self.environment)
        self.assertIn("REWARD_CARD_ICON_KEYS", self.generator)
        self.assertIn('args.batch == "ui-supplement"', self.generator)
        self.assertIn("for tier in range(1, 7)", self.generator)
        potion = self._function_source("build_potion_icon")
        for contract in (
            '"heartwood-elixir"', '"health-potion-premium-v2"',
            '"tierConstruction"', '"visualQuality": "studio-v3"',
            "potion_collar_leaf_", "potion_tier_foot_ring",
            "potion_shoulder_seed_", "potion_cradle_",
        ):
            self.assertIn(contract, potion)
        for icon in (
            'return switch (this)', '"general_power"', '"lifesteal"',
        ):
            self.assertIn(icon, self.reward_id)
        self.assertIn("icons.draw(batch, card.iconKey()", self.reward_renderer)

    def test_icons_share_a_premium_medallion_without_replacing_semantic_glyphs(self) -> None:
        source = self._function_source("build_ui_icon")
        for landmark in (
            "icon_shadow_medallion",
            "icon_inner_medallion",
            "icon_guard_ring",
            "icon_gold_stud_left",
            "icon_gold_stud_right",
        ):
            self.assertIn(landmark, source)
        for semantic in (
            "heart(", "wave_", "coin", "pause_left", "arrow_", "pack_body",
            "shop_body", "gear_ring", "restart_ring", "sword(", "close_a",
            "wing_", "clover_", "shield(",
        ):
            self.assertIn(semantic, source)
        self.assertIn('"ui-control-icon-premium-v2"', source)
        self.assertIn('"visualQuality": "studio-v3"', source)

    def test_frame_states_change_construction_not_only_tint(self) -> None:
        source = self._function_source("build_ui_frame")
        for part in (
            "frame_shadow", "frame_outer", "frame_inner", "frame_edge_top",
            "frame_corner_", "frame_selected_leaf_", "frame_pressed_notch",
            "frame_disabled_bar",
        ):
            self.assertIn(part, source)
        self.assertIn('"ninePatchInsets": {"left": 24', source)
        self.assertIn('"forest-glass-nine-patch-v2"', source)
        self.assertIn("inset = 0.08 if state == \"pressed\"", source)

    def test_runtime_exposes_and_uses_all_touch_visible_states(self) -> None:
        for state in ("NORMAL", "PRESSED", "SELECTED", "DISABLED"):
            self.assertIn(state, self.runtime)
        for method in ("press(", "movePress(", "release(", "resolve(", "NinePatch"):
            self.assertIn(method, self.runtime)
        self.assertIn("uiFrameRenderer.press(worldX, worldY)", self.game)
        self.assertIn("uiFrameRenderer.movePress(worldX, worldY)", self.game)
        self.assertIn("uiFrameRenderer.release()", self.game)

    def test_locked_style_names_each_skin_family_and_state(self) -> None:
        for phrase in (
            "`button`, `panel`, and `slot`",
            "pressed visibly insets",
            "selected completes the gold corners",
            "disabled removes saturation",
            "Heartwood medallion",
        ):
            self.assertIn(phrase, self.style)

    def _function_source(self, name: str) -> str:
        for node in ast.walk(self.tree):
            if isinstance(node, ast.FunctionDef) and node.name == name:
                return ast.get_source_segment(self.environment, node) or ""
        raise AssertionError(f"missing function {name}")


if __name__ == "__main__":
    unittest.main()
