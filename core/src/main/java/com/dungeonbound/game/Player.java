package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/** The hero: walks in 4 directions, collides with walls, and can swing the sword. */
public class Player implements Disposable {
    private enum Dir { DOWN, UP, LEFT, RIGHT }

    /** One animation strip plus how to draw it (scale so every sprite has the same body size). */
    private static class Anim {
        Animation<TextureRegion> anim;
        Texture texture;
        float cellW, cellH; // size of one frame in the strip (pixels)
        float anchorX;      // x in the frame that is the character's centre
        float feetY;        // y in the frame (from top) where the feet are
        float scale;
    }

    private static final float SPEED = 110f;          // world pixels per second
    private static final float FEET_HALF_W = 7f;      // collision box (at the feet) half width
    private static final float FEET_H = 5f;           // collision box height

    public float x, y;

    // stats shown in the HUD (private = encapsulation; read them with the getters below)
    private int maxHp = 100, hp = 100;
    private int maxMp = 50, mp = 50;
    private int pesos = 100; // starting money, in pesos
    private final TextureRegion portrait; // the face shown in the profile picture
    private Dir dir = Dir.DOWN;
    private Dir lastSide = Dir.RIGHT;
    private boolean moving, attacking;
    private float stateTime;
    private float castTime; // > 0 while casting a fireball

    private final Anim walkDown, walkUp, walkLeft, walkRight, swordLeft, swordRight;

    public Player(float startX, float startY) {
        x = startX;
        y = startY;
        // file, frames, cell w, cell h, anchorX, feetY, scale, smooth
        walkDown = load("dungeon/player/walk_down.png", 8, 100, 150, 51, 132, 0.36f, true);
        walkUp = load("dungeon/player/walk_up.png", 7, 48, 62, 24, 48, 1f, false);
        walkLeft = load("dungeon/player/walk_left.png", 8, 48, 62, 24.5f, 48, 1f, false);
        walkRight = load("dungeon/player/walk_right.png", 9, 50, 64, 23.5f, 49, 1f, false);
        swordLeft = load("dungeon/player/sword_left.png", 6, 150, 160, 90, 144, 0.31f, true);
        swordRight = load("dungeon/player/sword_right.png", 6, 160, 150, 56, 128, 0.31f, true);
        swordLeft.anim.setPlayMode(Animation.PlayMode.NORMAL);
        swordRight.anim.setPlayMode(Animation.PlayMode.NORMAL);
        // head and shoulders of the first "walk down" frame (frame is 100x150 pixels)
        portrait = new TextureRegion(walkDown.texture, 22, 28, 56, 56);
    }

    // ---- stats (HP, MP, money) ----
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getMp() { return mp; }
    public int getMaxMp() { return maxMp; }
    public int getPesos() { return pesos; }
    public TextureRegion getPortrait() { return portrait; }

    public void heal(int amount) { hp = Math.max(0, Math.min(maxHp, hp + amount)); }
    public void restoreMp(int amount) { mp = Math.max(0, Math.min(maxMp, mp + amount)); }
    public void addPesos(int amount) { pesos = Math.max(0, pesos + amount); }

    private Anim load(String file, int frames, int w, int h, float ax, float feetY, float scale, boolean smooth) {
        Anim a = new Anim();
        a.texture = new Texture(Gdx.files.internal(file));
        Texture.TextureFilter f = smooth ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest;
        a.texture.setFilter(f, f);
        TextureRegion[][] split = TextureRegion.split(a.texture, w, h);
        Array<TextureRegion> arr = new Array<>();
        for (int i = 0; i < frames; i++) arr.add(split[0][i]);
        a.anim = new Animation<>(0.1f, arr, Animation.PlayMode.LOOP);
        a.cellW = w;
        a.cellH = h;
        a.anchorX = ax;
        a.feetY = feetY;
        a.scale = scale;
        return a;
    }

    public void attack() {
        if (attacking || castTime > 0f) return;
        attacking = true;
        stateTime = 0f;
    }

    /** Short pause while a fireball is thrown. */
    public void cast() {
        if (isBusy()) return;
        castTime = 0.3f;
        moving = false;
        stateTime = 0f;
    }

    /** True while the sword swing or the fireball cast is playing. */
    public boolean isBusy() {
        return attacking || castTime > 0f;
    }

    public boolean isAttacking() {
        return attacking;
    }

    /** Direction the character is looking (unit vector). */
    public float facingX() {
        return dir == Dir.LEFT ? -1f : dir == Dir.RIGHT ? 1f : 0f;
    }

    public float facingY() {
        return dir == Dir.UP ? 1f : dir == Dir.DOWN ? -1f : 0f;
    }

    /** dirX / dirY: -1..1 movement input (y up). */
    public void update(float dt, float dirX, float dirY, GameMap map) {
        if (castTime > 0f) {
            castTime -= dt;
            return; // stand still while casting
        }
        if (attacking) {
            stateTime += dt;
            if (currentSword().anim.isAnimationFinished(stateTime)) {
                attacking = false;
                stateTime = 0f;
            }
            return; // stand still while swinging
        }

        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 1f) {
            dirX /= len;
            dirY /= len;
        }
        moving = len > 0.01f;

        if (moving) {
            if (Math.abs(dirX) > Math.abs(dirY)) {
                dir = dirX < 0 ? Dir.LEFT : Dir.RIGHT;
                lastSide = dir;
            } else {
                dir = dirY > 0 ? Dir.UP : Dir.DOWN;
            }
            // Move one axis at a time so the player slides along walls instead of sticking.
            float nx = x + dirX * SPEED * dt;
            if (canStand(map, nx, y)) x = nx;
            float ny = y + dirY * SPEED * dt;
            if (canStand(map, x, ny)) y = ny;
            stateTime += dt;
        } else {
            stateTime = 0f;
        }
    }

    /** Checks the little box under the character's feet against the walkable mask. */
    private boolean canStand(GameMap map, float px, float py) {
        return map.isWalkable(px - FEET_HALF_W, py)
            && map.isWalkable(px + FEET_HALF_W, py)
            && map.isWalkable(px - FEET_HALF_W, py + FEET_H)
            && map.isWalkable(px + FEET_HALF_W, py + FEET_H)
            && map.isWalkable(px, py + FEET_H / 2f);
    }

    private Anim currentSword() {
        return lastSide == Dir.LEFT ? swordLeft : swordRight;
    }

    private Anim currentAnim() {
        if (attacking) return currentSword();
        switch (dir) {
            case UP: return walkUp;
            case LEFT: return walkLeft;
            case RIGHT: return walkRight;
            default: return walkDown;
        }
    }

    public void render(SpriteBatch batch) {
        Anim a = currentAnim();
        TextureRegion frame = a.anim.getKeyFrame(stateTime);
        float w = a.cellW * a.scale;
        float h = a.cellH * a.scale;
        float drawX = x - a.anchorX * a.scale;
        float drawY = y - (a.cellH - a.feetY) * a.scale;
        batch.draw(frame, drawX, drawY, w, h);
    }

    @Override
    public void dispose() {
        for (Anim a : new Anim[]{walkDown, walkUp, walkLeft, walkRight, swordLeft, swordRight}) {
            a.texture.dispose();
        }
    }
}
