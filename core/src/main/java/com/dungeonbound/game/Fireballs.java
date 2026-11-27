package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/** Fireballs that fly in a straight line and disappear when they hit a wall or travel too far. */
public class Fireballs implements Disposable {
    private static final float SPEED = 230f;      // world pixels per second
    private static final float MAX_DIST = 300f;   // how far a fireball flies
    private static final float DRAW_SIZE = 22f;
    private static final float CHEST_HEIGHT = 14f; // drawn this far above the ground point

    private static class Ball {
        float x, y;      // position on the floor (used for wall collision)
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

    /** Shoots a fireball from (x, y) in direction (dirX, dirY). */
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
        b.angle = MathUtils.atan2(dirY, dirX) * MathUtils.radiansToDegrees; // sprite points right
        balls.add(b);
    }

    public void update(float dt, GameMap map) {
        for (int i = balls.size - 1; i >= 0; i--) {
            Ball b = balls.get(i);
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.dist += SPEED * dt;
            b.time += dt;
            if (b.dist > MAX_DIST || !map.isWalkable(b.x, b.y)) balls.removeIndex(i); // hit a wall
        }
    }

    public void render(SpriteBatch batch) {
        for (Ball b : balls) {
            TextureRegion frame = anim.getKeyFrame(b.time);
            float half = DRAW_SIZE / 2f;
            batch.draw(frame, b.x - half, b.y + CHEST_HEIGHT - half, half, half, DRAW_SIZE, DRAW_SIZE, 1f, 1f, b.angle);
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
