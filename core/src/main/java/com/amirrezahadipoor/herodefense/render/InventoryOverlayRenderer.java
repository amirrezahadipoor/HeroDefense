package com.amirrezahadipoor.herodefense.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.amirrezahadipoor.herodefense.input.InventoryTouchController;
import com.amirrezahadipoor.herodefense.input.InventoryTouchLayout;
import com.amirrezahadipoor.herodefense.model.EquipmentSlot;
import com.amirrezahadipoor.herodefense.model.GameState;
import com.amirrezahadipoor.herodefense.model.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Vector phone overlay with lazy 96-pixel item icons and touch-matched bounds. */
public final class InventoryOverlayRenderer implements AutoCloseable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final Map<String, Texture> icons = new HashMap<>();

    public InventoryOverlayRenderer() {
        font.getData().setScale(1.3f);
    }

    public void drawPauseMenu(SpriteBatch batch, Matrix4 projection) {
        beginShapes(projection);
        shapes.setColor(0.03f, 0.06f, 0.075f, 0.90f);
        shapes.rect(0f, 0f, 720f, 1280f);
        panel(180f, 480f, 360f, 240f);
        panel(180f, 760f, 360f, 140f);
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("E7D8B1"));
        font.draw(batch, "Paused", 295f, 1010f);
        font.draw(batch, "Resume", 300f, 615f);
        font.draw(batch, "Inventory", 280f, 845f);
        batch.end();
    }

    public void drawInventory(
        SpriteBatch batch,
        Matrix4 projection,
        GameState state,
        InventoryTouchController controller
    ) {
        Set<String> visibleIcons = new HashSet<>();
        beginShapes(projection);
        shapes.setColor(0.025f, 0.055f, 0.065f, 0.97f);
        shapes.rect(0f, 0f, 720f, 1280f);
        shapes.setColor(0.20f, 0.34f, 0.29f, 1f);
        shapes.rect(
            InventoryTouchLayout.CLOSE_X,
            InventoryTouchLayout.CLOSE_Y,
            InventoryTouchLayout.CLOSE_SIZE,
            InventoryTouchLayout.CLOSE_SIZE
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
            panel(x, y, InventoryTouchLayout.SLOT_WIDTH, InventoryTouchLayout.SLOT_HEIGHT);
        }
        for (int row = 0; row < InventoryTouchLayout.VISIBLE_ROWS; row++) {
            int itemIndex = controller.firstVisibleIndex() + row;
            float y = InventoryTouchLayout.LIST_TOP_Y
                - InventoryTouchLayout.LIST_ROW_HEIGHT
                - row * InventoryTouchLayout.LIST_ROW_STRIDE;
            if (itemIndex == controller.selectedIndex()) {
                shapes.setColor(0.28f, 0.43f, 0.30f, 1f);
            } else {
                shapes.setColor(0.10f, 0.17f, 0.18f, 1f);
            }
            shapes.rect(
                InventoryTouchLayout.LIST_X,
                y,
                InventoryTouchLayout.LIST_WIDTH,
                InventoryTouchLayout.LIST_ROW_HEIGHT
            );
        }
        panel(
            InventoryTouchLayout.EQUIP_X,
            InventoryTouchLayout.ACTION_Y,
            InventoryTouchLayout.ACTION_WIDTH,
            InventoryTouchLayout.ACTION_HEIGHT
        );
        panel(
            InventoryTouchLayout.SELL_X,
            InventoryTouchLayout.ACTION_Y,
            InventoryTouchLayout.ACTION_WIDTH,
            InventoryTouchLayout.ACTION_HEIGHT
        );
        shapes.end();
        endShapes();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.valueOf("E7D8B1"));
        font.draw(batch, "Equipment & Inventory", 205f, 1225f);
        font.draw(batch, "Close", 588f, 1172f);
        for (int index = 0; index < EquipmentSlot.values().length; index++) {
            EquipmentSlot slot = EquipmentSlot.values()[index];
            int column = index % 2;
            int row = index / 2;
            float x = column == 0
                ? InventoryTouchLayout.SLOT_LEFT_X
                : InventoryTouchLayout.SLOT_RIGHT_X;
            float y = InventoryTouchLayout.SLOT_TOP_Y
                - InventoryTouchLayout.SLOT_HEIGHT
                - row * InventoryTouchLayout.SLOT_ROW_STRIDE;
            Item item = state.equippedItems.get(slot.name());
            font.draw(batch, pretty(slot.name()), x + 10f, y + 76f);
            if (item == null) {
                font.draw(batch, "Empty", x + 112f, y + 42f);
            } else {
                drawIcon(batch, item, x + 10f, y + 7f, 58f, visibleIcons);
                font.draw(batch, item.name, x + 76f, y + 40f);
            }
        }
        for (int row = 0; row < InventoryTouchLayout.VISIBLE_ROWS; row++) {
            int itemIndex = controller.firstVisibleIndex() + row;
            if (itemIndex >= state.inventory.size()) continue;
            Item item = state.inventory.get(itemIndex);
            float y = InventoryTouchLayout.LIST_TOP_Y
                - InventoryTouchLayout.LIST_ROW_HEIGHT
                - row * InventoryTouchLayout.LIST_ROW_STRIDE;
            drawIcon(batch, item, InventoryTouchLayout.LIST_X + 8f, y + 8f, 72f, visibleIcons);
            font.draw(batch, item.name, InventoryTouchLayout.LIST_X + 94f, y + 62f);
            font.draw(batch, item.tier + " · " + item.sellPrice + " coins", InventoryTouchLayout.LIST_X + 94f, y + 28f);
        }
        font.draw(batch, "Equip", InventoryTouchLayout.EQUIP_X + 102f, 145f);
        font.draw(batch, "Sell", InventoryTouchLayout.SELL_X + 112f, 145f);
        batch.end();
        disposeHiddenIcons(visibleIcons);
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

    private void panel(float x, float y, float width, float height) {
        shapes.setColor(0.11f, 0.20f, 0.19f, 1f);
        shapes.rect(x, y, width, height);
        shapes.setColor(0.84f, 0.68f, 0.30f, 1f);
        shapes.rect(x, y + height - 5f, width, 5f);
    }

    private static String pretty(String value) {
        String text = value.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    @Override
    public void close() {
        for (Texture icon : icons.values()) icon.dispose();
        icons.clear();
        font.dispose();
        shapes.dispose();
    }
}
