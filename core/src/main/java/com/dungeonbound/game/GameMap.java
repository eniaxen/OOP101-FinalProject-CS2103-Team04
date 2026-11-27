package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * The dungeon map picture plus an invisible "walkable" mask of the same size.
 * In map_walkable.png a visible (alpha > 0) pixel means the player may stand there;
 * a transparent pixel is a wall / void. This is what keeps the player inside the rooms and hallways.
 */
public class GameMap implements Disposable {
    private final Texture texture;
    private final Pixmap walkable;
    public final int width;
    public final int height;

    public GameMap() {
        texture = new Texture(Gdx.files.internal("dungeon/map.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        walkable = new Pixmap(Gdx.files.internal("dungeon/map_walkable.png"));
        width = texture.getWidth();
        height = texture.getHeight();
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, 0, 0);
    }

    /** x / y are world coordinates (origin bottom-left, y up). */
    public boolean isWalkable(float x, float y) {
        int px = (int) x;
        int py = height - 1 - (int) y; // Pixmap y goes top -> bottom
        if (px < 0 || py < 0 || px >= width || py >= height) return false;
        return (walkable.getPixel(px, py) & 0xff) > 0; // alpha channel
    }

    @Override
    public void dispose() {
        texture.dispose();
        walkable.dispose();
    }
}
