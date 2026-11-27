package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The dungeon itself: map, player, joystick, attack buttons, fireballs and inventory.
 * StartMenu -> LoadingScreen -> GameScreen.  ESC opens the pause menu (PauseMenuScreen).
 */
public class GameScreen implements Screen {
    private static final float VIEW_W = 400f; // how much of the dungeon is visible (world pixels)
    private static final float VIEW_H = 300f;
    private static final float FIRE_COOLDOWN = 0.7f;

    private final DungeonBound game;
    private final AssetManager assets; // from LoadingScreen (may be null); this screen disposes it

    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shape = new ShapeRenderer();
    private final GameMap map;
    private final Player player;
    private final Joystick joystick = new Joystick();
    private final BitmapFont hudFont;   // one font shared by the profile HUD and the inventory
    private final PlayerHud playerHud;
    private final Inventory inventory;
    private final ActionButtons buttons;
    private final Fireballs fireballs;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);
    private final OrthographicCamera hudCamera = new OrthographicCamera();
    private final Viewport hudViewport = new ScreenViewport(hudCamera);

    private float fireCooldown;
    private boolean disposed;

    public GameScreen(Game game, AssetManager assets) {
        this.game = (DungeonBound) game;
        this.assets = assets;
        map = new GameMap();
        // Start in the big middle room (world y is flipped compared to the picture).
        player = new Player(640f, map.height - 330f);
        hudFont = HudStyle.createFont();
        playerHud = new PlayerHud(hudFont);
        inventory = new Inventory(hudFont);
        buttons = new ActionButtons(hudViewport);
        fireballs = new Fireballs();
    }

    public GameScreen(Game game) {
        this(game, null);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(buttons); // the attack buttons listen to touches
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == buttons) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hudViewport.update(width, height, true);
        float hudW = hudViewport.getWorldWidth();
        float hudH = hudViewport.getWorldHeight();
        joystick.layout(hudH);
        playerHud.layout(hudW, hudH);
        inventory.layout(hudW, hudH, playerHud.getProfileRect()); // the bag icon sits below the profile
        buttons.layout(hudW, hudH);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        // ---- ESC: close the inventory if it is open, otherwise open the pause menu ----
        boolean escPressed = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        boolean inventoryWasOpen = inventory.isOpen();

        // ---- input: inventory, on-screen joystick + keyboard (arrows / WASD) ----
        inventory.update(hudViewport);
        boolean swordTap = buttons.pollSword();
        boolean fireTap = buttons.pollFire();
        fireCooldown = Math.max(0f, fireCooldown - dt);

        if (escPressed && !inventoryWasOpen) {
            openPauseMenu();
            return;
        }

        float dx = 0f;
        float dy = 0f;
        if (!inventory.isOpen()) { // the game pauses while the inventory is open
            Vector2 stick = joystick.update(hudViewport);
            dx = stick.x;
            dy = stick.y;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || swordTap) player.attack();
            if ((Gdx.input.isKeyJustPressed(Input.Keys.F) || fireTap) && fireCooldown <= 0f && !player.isBusy()) {
                player.cast();
                fireballs.shoot(player.x, player.y, player.facingX(), player.facingY());
                fireCooldown = FIRE_COOLDOWN;
            }
        }

        // ---- update ----
        player.update(dt, dx, dy, map);
        fireballs.update(dt, map);

        // camera follows the player but never shows outside the map
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;
        camera.position.x = map.width <= halfW * 2 ? map.width / 2f : MathUtils.clamp(player.x, halfW, map.width - halfW);
        camera.position.y = map.height <= halfH * 2 ? map.height / 2f : MathUtils.clamp(player.y + 10f, halfH, map.height - halfH);
        camera.update();

        // ---- draw ----
        ScreenUtils.clear(0.13f, 0.13f, 0.13f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        map.render(batch);
        player.render(batch);
        fireballs.render(batch);
        batch.end();

        hudViewport.apply();
        shape.setProjectionMatrix(hudCamera.combined);
        joystick.render(shape);
        batch.setProjectionMatrix(hudCamera.combined);
        buttons.render(shape, batch, player.isAttacking(), fireCooldown / FIRE_COOLDOWN);
        playerHud.render(shape, batch, player);
        inventory.render(shape, batch, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
    }

    /** Shows the team's pause menu. This screen stays alive (not disposed) while the menu is open. */
    private void openPauseMenu() {
        final GameScreen self = this;
        Runnable resume = () -> game.setScreen(self);
        Runnable items = () -> {
            inventory.openPanel();
            game.setScreen(self);
        };
        // "Creature Lore": leaves the dungeon and opens the wiki (its back button goes to the Start Menu).
        Runnable lore = () -> {
            game.setScreen(new WikiMenuScreen(game));
            self.dispose();
        };
        Runnable backToStart = () -> {
            game.setScreen(new StartMenuScreen(game));
            self.dispose();
        };
        game.setScreen(PauseMenuScreen.create(game, resume, items, lore, backToStart), false);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        batch.dispose();
        shape.dispose();
        map.dispose();
        player.dispose();
        inventory.dispose();
        hudFont.dispose();
        fireballs.dispose();
        buttons.dispose();
        if (assets != null) assets.dispose();
    }
}
