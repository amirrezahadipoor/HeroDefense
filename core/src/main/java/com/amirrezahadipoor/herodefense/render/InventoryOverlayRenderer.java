package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.InventoryTouchController;
import com.amirrezahadipoor.herodefense.input.InventoryTouchLayout;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;
import com.amirrezahadipoor.herodefense.render.InventoryItemDetails.Details;
import com.amirrezahadipoor.herodefense.render.InventoryItemDetails.StatComparison;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Vector phone overlay with lazy 96-pixel item icons and touch-matched bounds. */
public final class InventoryOverlayRenderer implements AutoCloseable {
    private static final Color GOLD = Color.valueOf("EAC66D");
    private static final Color IVORY = Color.valueOf("F3E4BC");
    private static final Color SUBTLE = Color.valueOf("AEBCAE");
    private static final Color POSITIVE = Color.valueOf("69C884");
    private static final Color NEGATIVE = Color.valueOf("DF6A65");
    private static final Color MUTED = Color.valueOf("777D76");

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OverlayText text = new OverlayText();
    private final Map<String, Texture> icons = new HashMap<>();

    public InventoryOverlayRenderer() {
    }

    public void drawInventory(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        InventoryTouchController controller,
        UiIconRenderer uiIcons,
        UiFrameRenderer frames
    ) {
        Set<String> visibleIcons = new HashSet<>();
        Item selected = controller.selectedItem(state);
        beginShapes(projection);
        shapes.setColor(0.040f, 0.090f, 0.080f, 0.97f);
        shapes.rect(0f, ScreenEdges.bottom(), 720f, ScreenEdges.height());
        shapes.setColor(0.04f, 0.13f, 0.11f, 0.82f);
        shapes.rect(0f, 1160f, 720f, 120f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        drawInventoryFrames(batch, state, controller, frames);
        if (controller.feedbackMessage() != null) {
            frames.draw(batch, UiFrameRenderer.Kind.PANEL, 110f, 195f, 500f, 60f, true, false);
        }
        batch.end();

        beginShapes(projection);
        for (int index = 0; index < EquipmentSlot.values().length; index++) {
            EquipmentSlot slot = EquipmentSlot.values()[index];
            Item item = state.equippedItems.get(slot.name());
            if (item == null) continue;
            int column = index % 2;
            int row = index / 2;
            float x = column == 0 ? InventoryTouchLayout.SLOT_LEFT_X : InventoryTouchLayout.SLOT_RIGHT_X;
            float y = InventoryTouchLayout.SLOT_TOP_Y
                - InventoryTouchLayout.SLOT_HEIGHT
                - row * InventoryTouchLayout.SLOT_ROW_STRIDE;
            rarityAccent(x + 5f, y + 9f, 5f, InventoryTouchLayout.SLOT_HEIGHT - 18f, item.tier);
        }
        for (int row = 0; row < InventoryTouchLayout.VISIBLE_ROWS; row++) {
            int itemIndex = controller.firstVisibleIndex() + row;
            if (itemIndex >= state.inventory.size()) continue;
            Item item = state.inventory.get(itemIndex);
            float y = InventoryTouchLayout.LIST_TOP_Y
                - InventoryTouchLayout.LIST_ROW_HEIGHT
                - row * InventoryTouchLayout.LIST_ROW_STRIDE;
            rarityAccent(
                InventoryTouchLayout.LIST_X + 5f,
                y + 9f,
                5f,
                InventoryTouchLayout.LIST_ROW_HEIGHT - 18f,
                item.tier
            );
        }
        shapes.end();
        endShapes();

        batch.begin();
        uiIcons.draw(batch, "close", 588f, 1128f, 64f, frames.resolve(
            true, false,
            InventoryTouchLayout.CLOSE_X, InventoryTouchLayout.CLOSE_Y,
            InventoryTouchLayout.CLOSE_SIZE, InventoryTouchLayout.CLOSE_SIZE
        ));
        drawText(batch, "EQUIPMENT & INVENTORY", 40f, 1232f, 1.35f, GOLD);
        drawText(batch, "Tap a loadout slot to unequip", 40f, 1189f, 0.76f, SUBTLE);
        drawText(batch, "EQUIPPED LOADOUT", 40f, 1023f, 0.72f, GOLD);
        drawText(
            batch,
            "BACKPACK  " + state.inventory.size(),
            InventoryTouchLayout.LIST_X,
            668f,
            0.68f,
            GOLD
        );
        drawText(batch, "ITEM DETAILS", InventoryTouchLayout.DETAILS_X, 681f, 0.72f, GOLD);

        for (int index = 0; index < EquipmentSlot.values().length; index++) {
            EquipmentSlot slot = EquipmentSlot.values()[index];
            int column = index % 2;
            int row = index / 2;
            float x = column == 0 ? InventoryTouchLayout.SLOT_LEFT_X : InventoryTouchLayout.SLOT_RIGHT_X;
            float y = InventoryTouchLayout.SLOT_TOP_Y
                - InventoryTouchLayout.SLOT_HEIGHT
                - row * InventoryTouchLayout.SLOT_ROW_STRIDE;
            Item item = state.equippedItems.get(slot.name());
            drawText(batch, pretty(slot.name()).toUpperCase(Locale.ROOT), x + 30f, y + 70f, 0.64f, GOLD);
            if (item == null) {
                drawText(batch, "Empty slot", x + 112f, y + 39f, 0.78f, MUTED);
            } else {
                drawIcon(batch, item, x + 14f, y + 8f, 58f, visibleIcons);
                drawText(batch, item.name, x + 80f, y + 41f, 0.78f, IVORY);
                drawText(batch, prettyOrUnknown(item.tier), x + 220f, y + 66f, 0.58f, rarityColor(item.tier));
            }
        }

        for (int row = 0; row < InventoryTouchLayout.VISIBLE_ROWS; row++) {
            int itemIndex = controller.firstVisibleIndex() + row;
            if (itemIndex >= state.inventory.size()) continue;
            Item item = state.inventory.get(itemIndex);
            float y = InventoryTouchLayout.LIST_TOP_Y
                - InventoryTouchLayout.LIST_ROW_HEIGHT
                - row * InventoryTouchLayout.LIST_ROW_STRIDE;
            drawIcon(batch, item, InventoryTouchLayout.LIST_X + 12f, y + 9f, 70f, visibleIcons);
            drawText(batch, item.name, InventoryTouchLayout.LIST_X + 92f, y + 61f, 0.79f, IVORY);
            drawText(
                batch,
                prettyOrUnknown(item.tier).toUpperCase(Locale.ROOT),
                InventoryTouchLayout.LIST_X + 92f,
                y + 29f,
                0.58f,
                rarityColor(item.tier)
            );
            drawText(batch, "$ " + item.sellPrice, InventoryTouchLayout.LIST_X + 247f, y + 29f, 0.62f, GOLD);
        }

        drawDetails(batch, state, selected);
        boolean hasSelection = selected != null;
        UiFrameRenderer.State equipState = frames.resolve(
            hasSelection, false,
            InventoryTouchLayout.EQUIP_X, InventoryTouchLayout.ACTION_Y,
            InventoryTouchLayout.ACTION_WIDTH, InventoryTouchLayout.ACTION_HEIGHT
        );
        UiFrameRenderer.State sellState = frames.resolve(
            hasSelection, false,
            InventoryTouchLayout.SELL_X, InventoryTouchLayout.ACTION_Y,
            InventoryTouchLayout.ACTION_WIDTH, InventoryTouchLayout.ACTION_HEIGHT
        );
        float equipOffset = MainMenuRenderer.pressedOffset(equipState);
        float sellOffset = MainMenuRenderer.pressedOffset(sellState);
        String equipLabel = selected != null
            && InventoryItemDetails.inspect(state, selected).comparedItemName() != null
            ? "REPLACE"
            : "EQUIP";
        drawCentered(
            batch, equipLabel,
            InventoryTouchLayout.EQUIP_X + InventoryTouchLayout.ACTION_WIDTH * 0.5f,
            145f + equipOffset,
            1.02f,
            hasSelection ? IVORY : MUTED
        );
        drawCentered(
            batch,
            selected == null ? "SELL" : "SELL  $ " + selected.sellPrice,
            InventoryTouchLayout.SELL_X + InventoryTouchLayout.ACTION_WIDTH * 0.5f,
            145f + sellOffset,
            1.02f,
            hasSelection ? GOLD : MUTED
        );
        String feedback = controller.feedbackMessage();
        if (feedback != null) {
            Color feedbackColor = new Color(GOLD);
            feedbackColor.a = controller.feedbackAlpha();
            drawCentered(batch, feedback, 360f, 234f, 0.82f, feedbackColor);
        }
        batch.end();
        disposeHiddenIcons(visibleIcons);
    }

    private void drawInventoryFrames(
        SpriteBatch batch,
        GameState state,
        InventoryTouchController controller,
        UiFrameRenderer frames
    ) {
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            InventoryTouchLayout.CLOSE_X, InventoryTouchLayout.CLOSE_Y,
            InventoryTouchLayout.CLOSE_SIZE, InventoryTouchLayout.CLOSE_SIZE,
            true, false
        );
        for (int index = 0; index < EquipmentSlot.values().length; index++) {
            int column = index % 2;
            int row = index / 2;
            float x = column == 0
                ? InventoryTouchLayout.SLOT_LEFT_X
                : InventoryTouchLayout.SLOT_RIGHT_X;
            float y = InventoryTouchLayout.SLOT_TOP_Y
                - InventoryTouchLayout.SLOT_HEIGHT
                - row * InventoryTouchLayout.SLOT_ROW_STRIDE;
            frames.draw(
                batch, UiFrameRenderer.Kind.SLOT, x, y,
                InventoryTouchLayout.SLOT_WIDTH, InventoryTouchLayout.SLOT_HEIGHT,
                true, false
            );
        }
        for (int row = 0; row < InventoryTouchLayout.VISIBLE_ROWS; row++) {
            int itemIndex = controller.firstVisibleIndex() + row;
            float y = InventoryTouchLayout.LIST_TOP_Y
                - InventoryTouchLayout.LIST_ROW_HEIGHT
                - row * InventoryTouchLayout.LIST_ROW_STRIDE;
            frames.draw(
                batch, UiFrameRenderer.Kind.SLOT,
                InventoryTouchLayout.LIST_X, y,
                InventoryTouchLayout.LIST_WIDTH, InventoryTouchLayout.LIST_ROW_HEIGHT,
                itemIndex < state.inventory.size(), itemIndex == controller.selectedIndex()
            );
        }
        frames.draw(
            batch, UiFrameRenderer.Kind.PANEL,
            InventoryTouchLayout.DETAILS_X, InventoryTouchLayout.DETAILS_Y,
            InventoryTouchLayout.DETAILS_WIDTH, InventoryTouchLayout.DETAILS_HEIGHT,
            true, false
        );
        boolean hasSelection = controller.selectedItem(state) != null;
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            InventoryTouchLayout.EQUIP_X, InventoryTouchLayout.ACTION_Y,
            InventoryTouchLayout.ACTION_WIDTH, InventoryTouchLayout.ACTION_HEIGHT,
            hasSelection, false
        );
        frames.draw(
            batch, UiFrameRenderer.Kind.BUTTON,
            InventoryTouchLayout.SELL_X, InventoryTouchLayout.ACTION_Y,
            InventoryTouchLayout.ACTION_WIDTH, InventoryTouchLayout.ACTION_HEIGHT,
            hasSelection, false
        );
    }

    private void drawDetails(SpriteBatch batch, GameState state, Item selected) {
        float x = InventoryTouchLayout.DETAILS_X + 26f;
        float top = InventoryTouchLayout.DETAILS_Y + InventoryTouchLayout.DETAILS_HEIGHT - 25f;
        if (selected == null) {
            drawText(batch, "SELECT AN ITEM", x, top, 0.82f, SUBTLE);
            drawText(batch, "Review every bonus and compare", x, top - 46f, 0.66f, MUTED);
            drawText(batch, "it with the equipped slot.", x, top - 76f, 0.66f, MUTED);
            drawText(batch, "Green = upgrade", x, top - 150f, 0.66f, POSITIVE);
            drawText(batch, "Red = downgrade", x, top - 182f, 0.66f, NEGATIVE);
            return;
        }

        Details details = InventoryItemDetails.inspect(state, selected);
        drawText(batch, details.name(), x, top, 0.86f, rarityColor(details.rarity()));
        drawText(
            batch,
            prettyOrUnknown(details.rarity()).toUpperCase(Locale.ROOT)
                + "  |  " + (details.slot() == null ? "UNKNOWN" : details.slot().name()),
            x,
            top - 40f,
            0.60f,
            GOLD
        );
        drawText(
            batch,
            details.equipped() ? "CURRENTLY EQUIPPED" : "IN BAG",
            x,
            top - 73f,
            0.58f,
            details.equipped() ? POSITIVE : SUBTLE
        );
        drawText(
            batch,
            "COMPARE: " + (details.comparedItemName() == null ? "EMPTY SLOT" : details.comparedItemName()),
            x,
            top - 108f,
            0.60f,
            SUBTLE
        );
        drawText(batch, "STAT COMPARISON", x, top - 151f, 0.64f, GOLD);
        if (details.stats().isEmpty()) {
            drawText(batch, "No stat bonuses", x, top - 194f, 0.68f, MUTED);
            return;
        }
        boolean compared = details.comparedItemName() != null;
        for (int index = 0; index < details.stats().size(); index++) {
            StatComparison comparison = details.stats().get(index);
            float y = top - 194f - index * 43f;
            drawText(batch, pretty(comparison.stat().name()), x, y, 0.62f, IVORY);
            drawText(batch, signed(comparison.candidateValue()), x + 104f, y, 0.62f, IVORY);
            drawText(
                batch,
                comparisonLabel(comparison.difference(), compared),
                x + 165f,
                y,
                0.57f,
                comparisonColor(comparison.difference(), compared)
            );
        }
    }

    private void drawIcon(
        SpriteBatch batch,
        Item item,
        float x,
        float y,
        float size,
        Set<String> visibleIcons
    ) {
        if (item.iconKey == null || item.iconKey.isEmpty()) return;
        visibleIcons.add(item.id);
        Texture texture = icons.get(item.id);
        if (texture == null) {
            texture = new Texture(Gdx.files.internal(item.iconKey));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            icons.put(item.id, texture);
        }
        batch.draw(texture, x, y, size, size);
    }

    private void disposeHiddenIcons(Set<String> visible) {
        Iterator<Map.Entry<String, Texture>> iterator = icons.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Texture> entry = iterator.next();
            if (!visible.contains(entry.getKey())) {
                entry.getValue().dispose();
                iterator.remove();
            }
        }
    }

    private void beginShapes(Matrix4 projection) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void endShapes() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void rarityAccent(float x, float y, float width, float height, String rarity) {
        shapes.setColor(rarityColor(rarity));
        shapes.rect(x, y, width, height);
    }

    private void drawCentered(
        SpriteBatch batch, String label, float centerX, float y, float scale, Color color
    ) {
        text.drawCentered(batch, label, centerX, y, scale, color);
    }

    private void drawText(
        SpriteBatch batch, String label, float x, float y, float scale, Color color
    ) {
        text.draw(batch, label, x, y, scale, color);
    }

    static String comparisonLabel(float difference, boolean compared) {
        if (!compared) return "NEW " + signed(difference);
        if (difference > 0.001f) return "UP " + signed(difference);
        if (difference < -0.001f) return "DOWN " + signed(difference);
        return "SAME";
    }

    private static Color comparisonColor(float difference, boolean compared) {
        if (!compared || difference > 0.001f) return POSITIVE;
        if (difference < -0.001f) return NEGATIVE;
        return MUTED;
    }

    private static Color rarityColor(String rarity) {
        if (rarity == null) return Color.valueOf("E7D8B1");
        return switch (rarity) {
            case "UNCOMMON" -> Color.valueOf("74C365");
            case "RARE" -> Color.valueOf("6FADEB");
            case "LEGENDARY" -> Color.valueOf("F2B84B");
            default -> Color.valueOf("E7D8B1");
        };
    }

    private static String signed(float value) {
        int rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.001f) {
            return String.format(Locale.ROOT, "%+d", rounded);
        }
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private static String prettyOrUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown" : pretty(value);
    }

    private static String pretty(String value) {
        String text = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void close() {
        for (Texture icon : icons.values()) icon.dispose();
        icons.clear();
        text.close();
        shapes.dispose();
    }
}
