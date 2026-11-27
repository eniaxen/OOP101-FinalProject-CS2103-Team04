package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Fireballs that fly in a straight line. They disappear when they hit a wall, hit an enemy,
 * or travel too far.
 *
 * Positions use the same coordinates as GameScreen (px, py): x to the right, y going DOWN.
 */
public class Fireballs implements Disposable {
    private static final float SPEED = 230f;      // pixels per second
    private static final float MAX_DIST = 300f;   // how far a fireball flies
    private static final float DRAW_SIZE = 22f;
    private static final float CHEST_HEIGHT = 14f; // drawn this far above the ground point

    /** What the fireballs need to know about the dungeon. GameScreen provides it. */
    public interface World {
        /** True if a fireball at this point is inside a wall or a closed door. */
        boolean isBlocked(float x, float y);

        /** True if a fireball at this point hit an enemy (GameScreen already applied the damage). */
        boolean hitEnemy(float x, float y);
    }

    private static class Ball {
        float x, y;      // position on the floor
        float vx, vy;
        float dist, time, angle;
    }

    private final Texture texture;
    private final Animation<TextureRegion> anim;
    private final Array<Ball> balls = new Array<>();

    public Fireballs() {
        texture = new Texture(Gdx.files.internal("dungeon/fireball.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] split = TextureRegion.split(texture, 16, 16);
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < 4; i++) frames.add(split[0][i]);
        anim = new Animation<>(0.08f, frames, Animation.PlayMode.LOOP);
    }

    /** Shoots a fireball from (x, y) in direction (dirX, dirY). y grows downward. */
    public void shoot(float x, float y, float dirX, float dirY) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len < 0.001f) return;
        dirX /= len;
        dirY /= len;
        Ball b = new Ball();
        b.x = x + dirX * 12f;
        b.y = y + dirY * 6f;
        b.vx = dirX * SPEED;
        b.vy = dirY * SPEED;
        b.angle = MathUtils.atan2(-dirY, dirX) * MathUtils.radiansToDegrees; // sprite points right; screen y is flipped
        balls.add(b);
    }

    /** Removes every fireball (new level, restart). */
    public void clear() {
        balls.clear();
    }

    public void update(float dt, World world) {
        for (int i = balls.size - 1; i >= 0; i--) {
            Ball b = balls.get(i);
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.dist += SPEED * dt;
            b.time += dt;
            if (b.dist > MAX_DIST || world.isBlocked(b.x, b.y) || world.hitEnemy(b.x, b.y)) {
                balls.removeIndex(i);
            }
        }
    }

    /** worldHeight = map height in pixels (GameScreen flips y when drawing). */
    public void render(SpriteBatch batch, float worldHeight) {
        for (Ball b : balls) {
            TextureRegion frame = anim.getKeyFrame(b.time);
            float half = DRAW_SIZE / 2f;
            float drawY = worldHeight - b.y + CHEST_HEIGHT;
            batch.draw(frame, b.x - half, drawY - half, half, half, DRAW_SIZE, DRAW_SIZE, 1f, 1f, b.angle);
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
