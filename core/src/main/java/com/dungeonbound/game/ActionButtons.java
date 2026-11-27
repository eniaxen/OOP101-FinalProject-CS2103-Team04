package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Two round attack buttons in the lower-right corner: sword and fireball.
 * It listens to touches itself, so you can hold the joystick with one finger and tap a button with another.
 *
 * Want your own icons? Put these files in the assets/ui/ folder:
 *   sword_icon.png      fireball_icon.png
 * If a file is missing, the button falls back to the little icon drawn in code.
 */
public class ActionButtons extends InputAdapter implements Disposable {
    private static final String SWORD_ICON = "dungeon/ui/sword_icon.png";
    private static final String FIREBALL_ICON = "dungeon/ui/fireball_icon.png";

    private final Viewport hud;
    private final Vector2 tmp = new Vector2();
    private final Vector2 swordPos = new Vector2();
    private final Vector2 firePos = new Vector2();
    private final Texture swordIcon;   // null = use the drawn icon
    private final Texture fireIcon;    // null = use the drawn icon
    private float radius;
    private boolean swordTapped, fireTapped;

    public ActionButtons(Viewport hud) {
        this.hud = hud;
        swordIcon = loadIcon(SWORD_ICON);
        fireIcon = loadIcon(FIREBALL_ICON);
    }

    private static Texture loadIcon(String file) {
        if (!Gdx.files.internal(file).exists()) return null;
        Texture t = new Texture(Gdx.files.internal(file));
        // Nearest keeps pixel art sharp. Change both to Linear if your icons are smooth, high-resolution pictures.
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    /** Call from resize(). */
    public void layout(float hudWidth, float hudHeight) {
        radius = Math.max(36f, hudHeight * 0.085f);
        float margin = radius * 0.7f;
        swordPos.set(hudWidth - margin - radius, margin + radius);
        firePos.set(hudWidth - margin - radius * 3.1f, margin + radius * 1.9f);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Vector2 p = hud.unproject(tmp.set(screenX, screenY));
        if (p.dst(swordPos) <= radius * 1.15f) {
            swordTapped = true;
            return true;
        }
        if (p.dst(firePos) <= radius * 1.15f) {
            fireTapped = true;
            return true;
        }
        return false;
    }

    /** True once per tap on the sword button. */
    public boolean pollSword() {
        boolean t = swordTapped;
        swordTapped = false;
        return t;
    }

    /** True once per tap on the fireball button. */
    public boolean pollFire() {
        boolean t = fireTapped;
        fireTapped = false;
        return t;
    }

    /** swordBusy: dim the sword button. fireCooldown: 0 (ready) .. 1 (just fired). */
    public void render(ShapeRenderer shape, SpriteBatch batch, boolean swordBusy, float fireCooldown) {
        float r = radius;
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // 1) button backgrounds (+ the drawn icons when there is no picture file)
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.55f);
        shape.circle(swordPos.x, swordPos.y, r, 40);
        shape.circle(firePos.x, firePos.y, r, 40);
        if (swordIcon == null) drawSwordShape(shape, swordPos.x, swordPos.y, r);
        if (fireIcon == null) drawFireShape(shape, firePos.x, firePos.y, r);
        shape.end();

        // 2) your own icon pictures
        if (swordIcon != null || fireIcon != null) {
            batch.begin();
            float size = r * 1.5f;
            if (swordIcon != null) batch.draw(swordIcon, swordPos.x - size / 2f, swordPos.y - size / 2f, size, size);
            if (fireIcon != null) batch.draw(fireIcon, firePos.x - size / 2f, firePos.y - size / 2f, size, size);
            batch.end();
        }

        // 3) dimming / cooldown on top of the icons
        shape.begin(ShapeRenderer.ShapeType.Filled);
        if (swordBusy) {
            shape.setColor(0f, 0f, 0f, 0.5f);
            shape.circle(swordPos.x, swordPos.y, r, 40);
        }
        if (fireCooldown > 0f) {
            shape.setColor(0f, 0f, 0f, 0.6f);
            shape.arc(firePos.x, firePos.y, r, 90f, 360f * Math.min(1f, fireCooldown));
        }
        shape.end();

        // 4) rings
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1f, 1f, 1f, 0.6f);
        shape.circle(swordPos.x, swordPos.y, r, 40);
        shape.circle(firePos.x, firePos.y, r, 40);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSwordShape(ShapeRenderer shape, float x, float y, float r) {
        shape.setColor(0.85f, 0.88f, 0.95f, 1f);                       // blade
        shape.rectLine(x - r * 0.35f, y - r * 0.15f, x + r * 0.5f, y + r * 0.62f, r * 0.17f);
        shape.setColor(0.9f, 0.75f, 0.25f, 1f);                        // cross guard
        shape.rectLine(x - r * 0.52f, y - r * 0.38f, x - r * 0.18f, y + r * 0.02f, r * 0.12f);
        shape.setColor(0.45f, 0.28f, 0.12f, 1f);                       // handle
        shape.rectLine(x - r * 0.5f, y - r * 0.4f, x - r * 0.65f, y - r * 0.58f, r * 0.12f);
    }

    private void drawFireShape(ShapeRenderer shape, float x, float y, float r) {
        shape.setColor(0.85f, 0.2f, 0.05f, 1f);                        // flame tail
        shape.triangle(x - r * 0.62f, y - r * 0.05f, x - r * 0.05f, y + r * 0.38f, x - r * 0.05f, y - r * 0.38f);
        shape.setColor(1f, 0.5f, 0.05f, 1f);
        shape.circle(x + r * 0.12f, y, r * 0.42f, 28);
        shape.setColor(1f, 0.8f, 0.2f, 1f);
        shape.circle(x + r * 0.16f, y, r * 0.28f, 24);
        shape.setColor(1f, 0.97f, 0.7f, 1f);
        shape.circle(x + r * 0.2f, y, r * 0.13f, 16);
    }

    @Override
    public void dispose() {
        if (swordIcon != null) swordIcon.dispose();
        if (fireIcon != null) fireIcon.dispose();
    }
}
