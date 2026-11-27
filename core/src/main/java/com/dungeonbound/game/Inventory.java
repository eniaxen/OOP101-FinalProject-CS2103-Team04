package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The inventory: a small bag icon right below the character's profile picture (upper left).
 * Tap it (or press I) to open the panel. The panel follows the reference picture:
 * item cards in a row (3 per page) with arrows and page dots, and tabs at the bottom
 * (All / Foods / Potions / Weapons).
 *
 * Controls: tap a card to read about it, tap the arrows (or press LEFT / RIGHT) to change page,
 * tap a tab to filter. Tap X, the bag icon, or press I / ESC to close.
 *
 * Want your own bag icon? Put a picture named inventory_icon.png in assets/dungeon/ui/ .
 * If it is missing, a little bag drawn in code is used instead.
 */
public class Inventory implements Disposable {
    private static final int PER_PAGE = 3;

    private static final Color AREA_EDGE = new Color(0.22f, 0.14f, 0.10f, 1f);
    private static final Color AREA_FILL = new Color(0.09f, 0.06f, 0.05f, 1f);
    private static final Color TAB_EDGE = new Color(0.35f, 0.22f, 0.15f, 1f);
    private static final Color TAB_FILL = new Color(0.18f, 0.11f, 0.08f, 1f);
    private static final Color CARD_EDGE = new Color(0.80f, 0.72f, 0.66f, 1f);
    private static final Color CARD_FILL = new Color(0.60f, 0.53f, 0.52f, 1f);
    private static final Color CARD_PLATE = new Color(0.86f, 0.75f, 0.61f, 1f);
    private static final Color PILL = new Color(0.13f, 0.09f, 0.08f, 1f);
    private static final Color DOT_OFF = new Color(0.30f, 0.20f, 0.15f, 1f);
    private static final Color GLOW = new Color(1f, 0.75f, 0.25f, 0.28f);
    private static final Color CLOSE_EDGE = new Color(0.93f, 0.62f, 0.55f, 1f);
    private static final Color CLOSE_FILL = new Color(0.72f, 0.16f, 0.20f, 1f);

    /** The tabs at the bottom of the panel. */
    private enum Category {
        ALL("All"), FOODS("Foods"), POTIONS("Potions"), WEAPONS("Weapons");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    /** One thing the player carries. */
    private static class Item {
        final String name;
        final String description;
        final Category category;
        final int count;
        final Texture texture;

        Item(String name, String description, Category category, int count, String file) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.count = count;
            this.texture = new Texture(Gdx.files.internal(file));
            this.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // crisp pixel art
        }
    }

    private final Array<Item> items = new Array<>();    // everything the player carries
    private final Array<Item> visible = new Array<>();  // what the current tab shows
    private final BitmapFont font;                      // shared with PlayerHud; GameScreen disposes it
    private final Vector2 tmp = new Vector2();

    private final Rectangle iconRect = new Rectangle();
    private final Rectangle panelRect = new Rectangle();
    private final Rectangle areaRect = new Rectangle();
    private final Rectangle closeRect = new Rectangle();
    private final Rectangle prevRect = new Rectangle();
    private final Rectangle nextRect = new Rectangle();
    private final Rectangle[] cards = new Rectangle[PER_PAGE];
    private final Rectangle[] tabs = new Rectangle[Category.values().length];

    private final Texture bagIcon; // null = use the drawn bag
    private boolean open;
    private Category tab = Category.ALL;
    private int page;
    private int selected = -1; // index into "visible"
    private float s = 1f;      // HUD scale

    public Inventory(BitmapFont font) {
        this.font = font;
        for (int i = 0; i < cards.length; i++) cards[i] = new Rectangle();
        for (int i = 0; i < tabs.length; i++) tabs[i] = new Rectangle();
        if (Gdx.files.internal("dungeon/ui/inventory_icon.png").exists()) {
            bagIcon = new Texture(Gdx.files.internal("dungeon/ui/inventory_icon.png"));
            // Nearest keeps pixel art sharp. Use Linear if your icon is a smooth, high-resolution picture.
            bagIcon.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } else {
            bagIcon = null;
        }
        // The starting items
        items.add(new Item("Yellow Potion", "A bottle of yellow potion.", Category.POTIONS, 1, "dungeon/items/yellow_potion.png"));
        items.add(new Item("Itak", "A Filipino bolo knife.", Category.WEAPONS, 1, "dungeon/items/itak.png"));
        items.add(new Item("Sibat", "A Filipino spear.", Category.WEAPONS, 1, "dungeon/items/sibat.png"));
        rebuildVisible();
    }

    public boolean isOpen() {
        return open;
    }

    /** Opens the item panel (used by the pause menu's "Items" button). */
    public void openPanel() {
        open = true;
    }

    // ------------------------------------------------------------------ layout

    /**
     * Call from resize(). w / h are the HUD size in pixels.
     * profile = the profile picture's box (PlayerHud.getProfileRect()); the bag icon sits right below it.
     */
    public void layout(float w, float h, Rectangle profile) {
        s = Math.max(1f, h / 540f);

        // Small bag icon, right below the profile picture.
        float icon = Math.max(30f, h * 0.07f);
        iconRect.set(profile.x, profile.y - 8f * s - icon, icon, icon);

        // Centered panel
        float pw = Math.min(w * 0.92f, 600f * s);
        float ph = Math.min(h * 0.92f, 360f * s);
        panelRect.set((w - pw) / 2f, (h - ph) / 2f, pw, ph);

        float pad = 14f * s;
        float gap = 8f * s;
        float titleH = 50f * s;
        float tabH = 44f * s;

        // Tabs along the bottom
        float tabW = (pw - 2f * pad - gap * (tabs.length - 1)) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].set(panelRect.x + pad + i * (tabW + gap), panelRect.y + pad, tabW, tabH);
        }

        // Dark area that holds the cards
        float areaBottom = panelRect.y + pad + tabH + gap;
        float areaTop = panelRect.y + ph - titleH;
        areaRect.set(panelRect.x + pad, areaBottom, pw - 2f * pad, areaTop - areaBottom);

        // Cards: a row of PER_PAGE, centered between the two arrows
        float arrowW = 30f * s;
        float arrowH = 44f * s;
        float cardGap = 16f * s;
        float cardH = areaRect.height * 0.70f;
        float cardW = Math.min(cardH * 0.80f,
            (areaRect.width - 2f * (arrowW + 12f * s) - (PER_PAGE - 1) * cardGap) / PER_PAGE);
        float rowW = PER_PAGE * cardW + (PER_PAGE - 1) * cardGap;
        float startX = areaRect.x + (areaRect.width - rowW) / 2f;
        float cardY = areaRect.y + areaRect.height - 10f * s - cardH;
        for (int i = 0; i < PER_PAGE; i++) {
            cards[i].set(startX + i * (cardW + cardGap), cardY, cardW, cardH);
        }
        float arrowY = cardY + cardH / 2f - arrowH / 2f;
        prevRect.set(areaRect.x + 6f * s, arrowY, arrowW, arrowH);
        nextRect.set(areaRect.x + areaRect.width - 6f * s - arrowW, arrowY, arrowW, arrowH);

        float close = 30f * s;
        closeRect.set(panelRect.x + pw - close - 10f * s, panelRect.y + ph - close - 10f * s, close, close);
    }

    // ------------------------------------------------------------------ input

    /** Handles taps / clicks and keys. */
    public void update(Viewport hud) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) toggle();
        if (open && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) close();
        if (open && Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) turnPage(-1);
        if (open && Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) turnPage(1);

        if (Gdx.input.justTouched()) {
            Vector2 p = hud.unproject(tmp.set(Gdx.input.getX(), Gdx.input.getY()));
            if (iconRect.contains(p.x, p.y)) {
                toggle();
            } else if (open) {
                if (closeRect.contains(p.x, p.y)) {
                    close();
                } else if (prevRect.contains(p.x, p.y)) {
                    turnPage(-1);
                } else if (nextRect.contains(p.x, p.y)) {
                    turnPage(1);
                } else {
                    for (int i = 0; i < tabs.length; i++) {
                        if (tabs[i].contains(p.x, p.y)) setTab(Category.values()[i]);
                    }
                    for (int slot = 0; slot < PER_PAGE; slot++) {
                        int index = page * PER_PAGE + slot;
                        if (index < visible.size && cards[slot].contains(p.x, p.y)) selected = index;
                    }
                }
            }
        }
    }

    private void toggle() {
        if (open) close();
        else open = true;
    }

    private void close() {
        open = false;
        selected = -1;
    }

    private void setTab(Category category) {
        tab = category;
        page = 0;
        selected = -1;
        rebuildVisible();
    }

    private void rebuildVisible() {
        visible.clear();
        for (Item it : items) {
            if (tab == Category.ALL || it.category == tab) visible.add(it);
        }
    }

    private int pageCount() {
        return Math.max(1, (visible.size + PER_PAGE - 1) / PER_PAGE);
    }

    private void turnPage(int direction) {
        int next = Math.max(0, Math.min(pageCount() - 1, page + direction));
        if (next != page) {
            page = next;
            selected = -1;
        }
    }

    // ------------------------------------------------------------------ drawing

    /** Draws icon, and the panel when open. The batch's projection must already be the HUD camera. */
    public void render(ShapeRenderer shape, SpriteBatch batch, float screenW, float screenH) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        drawIconShapes(shape);
        if (open) drawPanelShapes(shape, screenW, screenH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        if (bagIcon != null) {
            float sz = iconRect.width;
            batch.draw(bagIcon, iconRect.x + sz * 0.08f, iconRect.y + sz * 0.08f, sz * 0.84f, sz * 0.84f);
        }
        if (open) drawPanelContent(batch);
        batch.end();
    }

    private void drawIconShapes(ShapeRenderer shape) {
        float sz = iconRect.width;
        float x = iconRect.x, y = iconRect.y;
        HudStyle.box(shape, x, y, sz, sz, 2f * s, open ? HudStyle.GOLD : HudStyle.WOOD_LIGHT, HudStyle.PANEL_DARK);
        if (bagIcon != null) return;
        shape.setColor(0.45f, 0.30f, 0.15f, 1f);                  // bag body
        shape.rect(x + sz * 0.2f, y + sz * 0.15f, sz * 0.6f, sz * 0.5f);
        shape.setColor(0.60f, 0.42f, 0.22f, 1f);                  // bag flap
        shape.rect(x + sz * 0.2f, y + sz * 0.48f, sz * 0.6f, sz * 0.17f);
        shape.setColor(0.85f, 0.75f, 0.30f, 1f);                  // buckle
        shape.rect(x + sz * 0.44f, y + sz * 0.40f, sz * 0.12f, sz * 0.14f);
        shape.setColor(0.30f, 0.20f, 0.10f, 1f);                  // handle
        shape.rect(x + sz * 0.35f, y + sz * 0.65f, sz * 0.30f, sz * 0.07f);
        shape.rect(x + sz * 0.35f, y + sz * 0.65f, sz * 0.06f, sz * 0.14f);
        shape.rect(x + sz * 0.59f, y + sz * 0.65f, sz * 0.06f, sz * 0.14f);
        shape.rect(x + sz * 0.35f, y + sz * 0.79f, sz * 0.30f, sz * 0.06f);
    }

    private void drawPanelShapes(ShapeRenderer shape, float screenW, float screenH) {
        shape.setColor(0f, 0f, 0f, 0.55f);                        // dim the game
        shape.rect(0, 0, screenW, screenH);

        Rectangle p = panelRect;
        HudStyle.box(shape, p.x, p.y, p.width, p.height, 3f * s, HudStyle.WOOD_LIGHT, HudStyle.WOOD);
        shape.setColor(HudStyle.PANEL_DARK);
        shape.rect(p.x + 8f * s, p.y + 8f * s, p.width - 16f * s, p.height - 16f * s);

        // title plate on the top edge
        float plateW = 240f * s, plateH = 38f * s;
        HudStyle.box(shape, p.x + (p.width - plateW) / 2f, p.y + p.height - plateH - 6f * s,
            plateW, plateH, 3f * s, HudStyle.WOOD_LIGHT, HudStyle.WOOD);

        // dark area behind the cards
        HudStyle.box(shape, areaRect.x, areaRect.y, areaRect.width, areaRect.height, 2f * s, AREA_EDGE, AREA_FILL);

        // arrows (dim when there is no page in that direction)
        Color c = HudStyle.WOOD_LIGHT;
        shape.setColor(c.r, c.g, c.b, page > 0 ? 1f : 0.3f);
        shape.triangle(prevRect.x + prevRect.width, prevRect.y, prevRect.x + prevRect.width, prevRect.y + prevRect.height,
            prevRect.x, prevRect.y + prevRect.height / 2f);
        shape.setColor(c.r, c.g, c.b, page < pageCount() - 1 ? 1f : 0.3f);
        shape.triangle(nextRect.x, nextRect.y, nextRect.x, nextRect.y + nextRect.height,
            nextRect.x + nextRect.width, nextRect.y + nextRect.height / 2f);

        // cards on this page
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = page * PER_PAGE + slot;
            if (index >= visible.size) break;
            drawCardShapes(shape, cards[slot], index == selected);
        }

        // page dots
        int n = pageCount();
        float spacing = 18f * s;
        float dotsX = areaRect.x + areaRect.width / 2f - (n - 1) * spacing / 2f;
        for (int i = 0; i < n; i++) {
            shape.setColor(i == page ? HudStyle.GOLD : DOT_OFF);
            shape.circle(dotsX + i * spacing, areaRect.y + 14f * s, 5f * s, 12);
        }

        // tabs
        for (int i = 0; i < tabs.length; i++) {
            Rectangle t = tabs[i];
            Category cat = Category.values()[i];
            boolean on = cat == tab;
            HudStyle.box(shape, t.x, t.y, t.width, t.height, on ? 3f * s : 2f * s,
                on ? HudStyle.GOLD : TAB_EDGE, on ? HudStyle.PANEL_MID : TAB_FILL);
            drawTabIcon(shape, cat, t.x + t.width / 2f, t.y + t.height * 0.62f, t.height * 0.42f);
        }

        // close button (red square with a white X)
        HudStyle.box(shape, closeRect.x, closeRect.y, closeRect.width, closeRect.height, 3f * s,
            CLOSE_EDGE, CLOSE_FILL);
        shape.setColor(Color.WHITE);
        float m = 8f * s;
        shape.rectLine(closeRect.x + m, closeRect.y + m, closeRect.x + closeRect.width - m, closeRect.y + closeRect.height - m, 3f * s);
        shape.rectLine(closeRect.x + m, closeRect.y + closeRect.height - m, closeRect.x + closeRect.width - m, closeRect.y + m, 3f * s);
    }

    private float plateHeight(Rectangle r) {
        return r.height * 0.34f;
    }

    private void drawCardShapes(ShapeRenderer shape, Rectangle r, boolean isSelected) {
        if (isSelected) {
            shape.setColor(GLOW);                                  // soft glow around the chosen card
            shape.rect(r.x - 7f * s, r.y - 7f * s, r.width + 14f * s, r.height + 14f * s);
        }
        HudStyle.box(shape, r.x, r.y, r.width, r.height, 3f * s, isSelected ? HudStyle.GOLD : CARD_EDGE, CARD_FILL);
        float plateH = plateHeight(r);
        shape.setColor(CARD_PLATE);                                // name plate
        shape.rect(r.x + 4f * s, r.y + 4f * s, r.width - 8f * s, plateH);
        float pillW = r.width * 0.46f, pillH = plateH * 0.36f;     // count pill
        shape.setColor(PILL);
        shape.rect(r.x + (r.width - pillW) / 2f, r.y + 4f * s + plateH * 0.10f, pillW, pillH);
    }

    private void drawTabIcon(ShapeRenderer shape, Category cat, float cx, float cy, float u) {
        switch (cat) {
            case ALL: {                                            // four squares
                float q = u * 0.42f, g = u * 0.08f;
                shape.setColor(HudStyle.GOLD);
                shape.rect(cx - g - q, cy + g, q, q);
                shape.rect(cx + g, cy + g, q, q);
                shape.rect(cx - g - q, cy - g - q, q, q);
                shape.rect(cx + g, cy - g - q, q, q);
                break;
            }
            case FOODS:                                            // apple
                shape.setColor(0.85f, 0.15f, 0.15f, 1f);
                shape.circle(cx, cy - u * 0.05f, u * 0.42f, 16);
                shape.setColor(0.25f, 0.65f, 0.25f, 1f);
                shape.rect(cx + u * 0.02f, cy + u * 0.30f, u * 0.22f, u * 0.14f);
                break;
            case POTIONS:                                          // flask
                shape.setColor(0.25f, 0.40f, 0.95f, 1f);
                shape.circle(cx, cy - u * 0.12f, u * 0.34f, 16);
                shape.setColor(0.80f, 0.85f, 0.95f, 1f);
                shape.rect(cx - u * 0.10f, cy + u * 0.15f, u * 0.20f, u * 0.28f);
                shape.setColor(0.60f, 0.40f, 0.22f, 1f);
                shape.rect(cx - u * 0.12f, cy + u * 0.42f, u * 0.24f, u * 0.10f);
                break;
            default:                                               // crossed swords
                shape.setColor(0.85f, 0.88f, 0.95f, 1f);
                shape.rectLine(cx - u * 0.42f, cy - u * 0.42f, cx + u * 0.42f, cy + u * 0.42f, u * 0.14f);
                shape.rectLine(cx - u * 0.42f, cy + u * 0.42f, cx + u * 0.42f, cy - u * 0.42f, u * 0.14f);
                shape.setColor(HudStyle.GOLD);
                shape.circle(cx, cy, u * 0.12f, 10);
                break;
        }
    }

    private void drawPanelContent(SpriteBatch batch) {
        // title
        HudStyle.setPx(font, 22f * s);
        HudStyle.drawCentered(batch, font, "Inventory", panelRect.x + panelRect.width / 2f,
            panelRect.y + panelRect.height - 6f * s - 19f * s, HudStyle.CREAM);

        // cards
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = page * PER_PAGE + slot;
            if (index >= visible.size) break;
            Item it = visible.get(index);
            Rectangle r = cards[slot];
            float plateH = plateHeight(r);

            // the picture (whole-number zoom keeps pixel art sharp)
            float boxW = r.width - 16f * s;
            float boxH = r.height - plateH - 16f * s;
            float zoom = (float) Math.floor(Math.min(boxW * 0.85f / it.texture.getWidth(), boxH * 0.85f / it.texture.getHeight()));
            zoom = Math.max(1f, zoom);
            float iw = it.texture.getWidth() * zoom;
            float ih = it.texture.getHeight() * zoom;
            batch.draw(it.texture, r.x + (r.width - iw) / 2f, r.y + plateH + 8f * s + (boxH - ih) / 2f, iw, ih);

            // name and count
            HudStyle.drawFitted(batch, font, it.name, r.x + r.width / 2f, r.y + 4f * s + plateH * 0.70f,
                13f * s, r.width - 14f * s, HudStyle.INK);
            HudStyle.setPx(font, 12f * s);
            HudStyle.drawCentered(batch, font, "x" + it.count, r.x + r.width / 2f,
                r.y + 4f * s + plateH * 0.10f + plateH * 0.18f, HudStyle.CREAM);
        }

        // message under the cards
        if (visible.size == 0) {
            HudStyle.setPx(font, 16f * s);
            HudStyle.drawCentered(batch, font, "Nothing here yet", areaRect.x + areaRect.width / 2f,
                areaRect.y + areaRect.height / 2f, HudStyle.CREAM);
        } else {
            String line = "Tap an item to read about it";
            if (selected >= 0 && selected < visible.size) {
                line = visible.get(selected).name + ": " + visible.get(selected).description;
            }
            HudStyle.drawFitted(batch, font, line, areaRect.x + areaRect.width / 2f, areaRect.y + 36f * s,
                12f * s, areaRect.width - 20f * s, HudStyle.CREAM);
        }

        // tab names
        for (int i = 0; i < tabs.length; i++) {
            Rectangle t = tabs[i];
            Category cat = Category.values()[i];
            HudStyle.drawFitted(batch, font, cat.label, t.x + t.width / 2f, t.y + t.height * 0.2f,
                12f * s, t.width - 8f * s, cat == tab ? HudStyle.GOLD : HudStyle.CREAM);
        }
    }

    @Override
    public void dispose() {
        for (Item it : items) it.texture.dispose();
        if (bagIcon != null) bagIcon.dispose();
        // the font is shared and disposed by GameScreen
    }
}
