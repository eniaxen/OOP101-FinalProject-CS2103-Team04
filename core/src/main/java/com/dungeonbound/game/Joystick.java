package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/** On-screen arrow joystick in the bottom-left corner. Works with touch and with the mouse. */
public class Joystick {
    private final Vector2 center = new Vector2();
    private final Vector2 knob = new Vector2();   // offset from center, length 0..1
    private final Vector2 tmp = new Vector2();
    private float radius;
    private int pointer = -1;

    /** Call from resize(); sizes the joystick relative to the screen. */
    public void layout(float hudHeight) {
        radius = Math.max(55f, hudHeight * 0.13f);
        float margin = radius * 0.5f;
        center.set(margin + radius, margin + radius);
    }

    public float getCenterX() { return center.x; }
    public float getCenterY() { return center.y; }
    public float getRadius() { return radius; }

    /** Reads touch input. Returns direction in knob (x, y), each -1..1, y up. */
    public Vector2 update(Viewport hud) {
        if (pointer == -1) {
            for (int i = 0; i < 5; i++) {
                if (Gdx.input.isTouched(i)) {
                    Vector2 p = hud.unproject(tmp.set(Gdx.input.getX(i), Gdx.input.getY(i)));
                    if (p.dst(center) <= radius * 1.4f) {
                        pointer = i;
                        break;
                    }
                }
            }
        }
        if (pointer != -1) {
            if (Gdx.input.isTouched(pointer)) {
                Vector2 p = hud.unproject(tmp.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer)));
                knob.set(p).sub(center).scl(1f / radius);
                if (knob.len() > 1f) knob.nor();
            } else {
                pointer = -1;
                knob.setZero();
            }
        }
        if (knob.len() < 0.2f) return tmp.setZero(); // dead zone
        return tmp.set(knob);
    }

    public void render(ShapeRenderer shape) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        // base
        shape.setColor(1f, 1f, 1f, 0.18f);
        shape.circle(center.x, center.y, radius, 48);
        // four arrows (up, down, left, right)
        shape.setColor(1f, 1f, 1f, 0.55f);
        float a = radius * 0.78f; // distance of arrow tip from center
        float b = radius * 0.50f; // distance of arrow base from center
        float w = radius * 0.20f; // half width of arrow base
        shape.triangle(center.x, center.y + a, center.x - w, center.y + b, center.x + w, center.y + b);
        shape.triangle(center.x, center.y - a, center.x - w, center.y - b, center.x + w, center.y - b);
        shape.triangle(center.x - a, center.y, center.x - b, center.y - w, center.x - b, center.y + w);
        shape.triangle(center.x + a, center.y, center.x + b, center.y - w, center.x + b, center.y + w);
        // knob
        shape.setColor(1f, 1f, 1f, 0.45f);
        shape.circle(center.x + knob.x * radius * 0.6f, center.y + knob.y * radius * 0.6f, radius * 0.28f, 32);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
