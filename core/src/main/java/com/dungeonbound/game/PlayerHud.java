package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Upper-left corner of the game screen: the character's profile picture, with the HP bar, the MP bar
 * and the money (in pesos) beside it. The inventory icon is placed right below the profile picture
 * (see Inventory.layout, which asks this class for getProfileRect()).
 */
public class PlayerHud {
    private static final Color BAR_BACK = new Color(0.10f, 0.06f, 0.05f, 0.95f);

    private final BitmapFont font; // shared with Inventory; GameScreen disposes it
    private final Rectangle profileRect = new Rectangle();
    private final Rectangle hpRect = new Rectangle();
    private final Rectangle mpRect = new Rectangle();
    private final Rectangle moneyRect = new Rectangle();
    private float s = 1f; // HUD scale

    public PlayerHud(BitmapFont font) {
        this.font = font;
    }

    /** The profile picture's box. The inventory icon goes right below it. */
    public Rectangle getProfileRect() {
        return profileRect;
    }

    /** Call from resize(). w / h are the HUD size in pixels. */
    public void layout(float w, float h) {
        s = Math.max(1f, h / 540f);
        float margin = 10f * s;
        float size = 64f * s;
        profileRect.set(margin, h - margin - size, size, size);

        float barX = profileRect.x + size + 8f * s;
        float barW = 150f * s;
        float barH = 16f * s;
        float gap = 6f * s;
        float top = profileRect.y + size;
        hpRect.set(barX, top - 2f * s - barH, barW, barH);
        mpRect.set(barX, hpRect.y - gap - barH, barW, barH);
        moneyRect.set(barX, mpRect.y - gap - 18f * s, barW, 18f * s);
    }

    public void render(ShapeRenderer shape, SpriteBatch batch, Player player) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        HudStyle.box(shape, profileRect.x, profileRect.y, profileRect.width, profileRect.height,
            3f * s, HudStyle.WOOD_LIGHT, HudStyle.PANEL_DARK);
        drawBar(shape, hpRect, player.getHp() / (float) player.getMaxHp(), HudStyle.HP);
        drawBar(shape, mpRect, player.getMp() / (float) player.getMaxMp(), HudStyle.MP);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        float pad = 5f * s;
        batch.draw(player.getPortrait(), profileRect.x + pad, profileRect.y + pad,
            profileRect.width - 2f * pad, profileRect.height - 2f * pad);

        HudStyle.setPx(font, 11f * s);
        HudStyle.drawCenteredShadow(batch, font, "HP " + player.getHp() + "/" + player.getMaxHp(),
            hpRect.x + hpRect.width / 2f, hpRect.y + hpRect.height / 2f, Math.max(1f, s));
        HudStyle.drawCenteredShadow(batch, font, "MP " + player.getMp() + "/" + player.getMaxMp(),
            mpRect.x + mpRect.width / 2f, mpRect.y + mpRect.height / 2f, Math.max(1f, s));

        HudStyle.setPx(font, 16f * s);
        HudStyle.drawLeft(batch, font, "\u20B1 " + player.getPesos(),
            moneyRect.x + 2f * s, moneyRect.y + moneyRect.height / 2f, HudStyle.GOLD);
        batch.end();
    }

    /** A bar with a dark back, a wooden edge and a coloured fill (fraction 0..1). Shape must be in Filled mode. */
    private void drawBar(ShapeRenderer shape, Rectangle r, float fraction, Color fill) {
        float edge = 2f * s;
        HudStyle.box(shape, r.x, r.y, r.width, r.height, edge, HudStyle.WOOD, BAR_BACK);
        float f = Math.max(0f, Math.min(1f, fraction));
        float fw = (r.width - 2f * edge) * f;
        if (fw <= 0f) return;
        shape.setColor(fill);
        shape.rect(r.x + edge, r.y + edge, fw, r.height - 2f * edge);
        shape.setColor(1f, 1f, 1f, 0.22f); // light stripe on the top half
        shape.rect(r.x + edge, r.y + r.height / 2f, fw, r.height / 2f - edge);
    }
}
