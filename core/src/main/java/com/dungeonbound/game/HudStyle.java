package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Colors, font and small drawing helpers shared by the in-game HUD (PlayerHud and Inventory).
 * Keeping them in one place makes both parts of the HUD look like one family.
 */
public final class HudStyle {
    private HudStyle() { }

    // ---- palette (wood + dark leather, taken from the inventory reference picture) ----
    public static final Color PANEL_DARK = new Color(0.14f, 0.09f, 0.07f, 0.97f);
    public static final Color PANEL_MID = new Color(0.25f, 0.16f, 0.12f, 1f);
    public static final Color WOOD = new Color(0.50f, 0.32f, 0.19f, 1f);
    public static final Color WOOD_LIGHT = new Color(0.78f, 0.55f, 0.33f, 1f);
    public static final Color GOLD = new Color(0.98f, 0.78f, 0.25f, 1f);
    public static final Color CREAM = new Color(0.95f, 0.89f, 0.78f, 1f);
    public static final Color INK = new Color(0.22f, 0.13f, 0.09f, 1f);
    public static final Color HP = new Color(0.80f, 0.18f, 0.18f, 1f);
    public static final Color MP = new Color(0.25f, 0.50f, 0.95f, 1f);

    /** The font is made once at this size; every text size is a scale of it. */
    private static final int FONT_BASE = 32;
    private static final GlyphLayout LAYOUT = new GlyphLayout();

    /** Makes the HUD font. fonts/font_body.ttf has the peso sign, so the characters list adds it. */
    public static BitmapFont createFont() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = FONT_BASE;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u20B1"; // \u20B1 = the peso sign
        BitmapFont font = gen.generateFont(p);
        gen.dispose();
        return font;
    }

    /** Sets the text height in HUD pixels. */
    public static void setPx(BitmapFont font, float px) {
        font.getData().setScale(px / FONT_BASE);
    }

    /** A rectangle with a border (the ShapeRenderer must be in Filled mode). */
    public static void box(ShapeRenderer shape, float x, float y, float w, float h, float border, Color borderColor, Color fill) {
        shape.setColor(borderColor);
        shape.rect(x, y, w, h);
        shape.setColor(fill);
        shape.rect(x + border, y + border, w - 2f * border, h - 2f * border);
    }

    /** Text centered on (cx, cy). */
    public static void drawCentered(SpriteBatch batch, BitmapFont font, String text, float cx, float cy, Color color) {
        LAYOUT.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, cx - LAYOUT.width / 2f, cy + font.getCapHeight() / 2f);
    }

    /** Text that starts at x and is centered up and down on cy. */
    public static void drawLeft(SpriteBatch batch, BitmapFont font, String text, float x, float cy, Color color) {
        font.setColor(color);
        font.draw(batch, text, x, cy + font.getCapHeight() / 2f);
    }

    /** Centered text with a small dark shadow, so it can be read on top of bars. */
    public static void drawCenteredShadow(SpriteBatch batch, BitmapFont font, String text, float cx, float cy, float shadow) {
        drawCentered(batch, font, text, cx + shadow, cy - shadow, Color.BLACK);
        drawCentered(batch, font, text, cx, cy, Color.WHITE);
    }

    /** Centered text at px height; it shrinks if it is wider than maxW. */
    public static void drawFitted(SpriteBatch batch, BitmapFont font, String text, float cx, float cy, float px, float maxW, Color color) {
        setPx(font, px);
        LAYOUT.setText(font, text);
        if (LAYOUT.width > maxW) setPx(font, px * maxW / LAYOUT.width);
        drawCentered(batch, font, text, cx, cy, color);
    }
}
