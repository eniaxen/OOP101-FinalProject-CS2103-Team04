package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Mga Nilalang — five-stage Filipino folklore dungeon gameplay screen.
 * Ported to libGDX using map.json, tile atlas, player strips, and enemy PNG atlases.
 */
public class GameScreen implements Screen {

    private static final int TILE = 16;
    private static final float VIEW_W = 960f;
    private static final float VIEW_H = 640f;

    private static final String[] TYPES = {"Tiyanak", "Aswang", "Tikbalang", "Manananggal", "Kapre"};
    private static final String[] KEYS = {"tiyanak", "aswang", "tikbalang", "manananggal", "kapre"};
    private static final int[][] AREAS = {
        {10, 7, 20, 15},   // smallest starting chamber
        {25, 4, 27, 17},   // upper hall
        {21, 19, 22, 15},  // west middle chamber
        {34, 19, 35, 12},  // east middle chamber
        {20, 30, 49, 16}   // large lower chamber
    };
    private static final int[][] SPAWNS = {{15, 15}, {39, 10}, {28, 25}, {50, 24}, {34, 38}};
    private static final int[][][] ENEMY_SPAWNS = {
        {{12, 11}, {17, 12}, {17, 18}},
        {{27, 9}, {38, 8},  {40, 10}, {48, 12}},   // (35,12) was in wall → moved to (38,8)
        {{24, 24}, {28, 23}, {30, 24}},
        {{39, 24}, {45, 22}, {48, 25}, {42, 26}},   // (64,23) was in void → moved to (42,26)
        {{25, 36}, {34, 40}, {39, 35}, {54, 40}, {63, 36}}
    };

    private static class Tile {
        final int id, x, y;
        Tile(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static class Door {
        final float x, y, w, h;
        float open = 0f;
        boolean opening = false;

        Door(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private static class Enemy {
        final String type;
        float x, y, hp, maxHp;
        float animTime = 0f;
        float attackCooldown = 0f;
        float hitFlash = 0f;
        boolean dying = false;
        boolean dead = false;
        float deathTime = 0f;
        boolean facingLeft = true;
        TextureRegion[] walkR, walkL, attackR, attackL, die;
        String action = "walk";
        boolean alerted = false;
        float alertTime = 0f;

        Enemy(String type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.maxHp = this.hp = type.equals("kapre") ? 145f : 85f;
        }
    }

    private final Game game;
    private final AssetManager assets;
    private final List<Texture> ownedTextures = new ArrayList<>();
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera worldCam = new OrthographicCamera();
    private final ExtendViewport worldViewport = new ExtendViewport(VIEW_W, VIEW_H, worldCam);
    private final OrthographicCamera hudCam = new OrthographicCamera();
    private final ExtendViewport hudViewport = new ExtendViewport(VIEW_W, VIEW_H, hudCam);

    private Texture pixelTex;
    private BitmapFont hudFont;
    private BitmapFont smallFont;
    private BitmapFont bannerFont;
    private BitmapFont titleFont;
    private final GlyphLayout layout = new GlyphLayout();

    private Texture tileAtlas;
    private TextureAtlas enemyAtlas;
    private TextureAtlas ownedEnemyAtlas;
    private final Map<Integer, TextureRegion> tileRegions = new HashMap<>();
    private int mapWidth, mapHeight;
    private float worldHeight;
    private final List<Tile> drawTiles = new ArrayList<>();
    private boolean[][] wall;
    private boolean[][] floor;
    private final Set<Long> trapTiles = new HashSet<>();
    private float trapDamageCooldown = 0f;

    private TextureRegion[] doorFrames;
    private final List<Door> doors = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<String> pendingEnemyReinforcements = new ArrayList<>();
    private final List<String> defeatedThisLevel = new ArrayList<>();
    private final Map<String, TextureRegion[]> playerFrames = new HashMap<>();
    private final Map<String, TextureRegion[]> enemyAnimCache = new HashMap<>();
    private final Random random = new Random(412);

    private float px, py, playerAnimTime, playerAttackTime;
    private int facing = 3; // 0 up, 1 down, 2 left, 3 right
    private float playerHp = 100f;
    private int level = 0;
    private int reinforcementsInCurrentLevel = 0;
    private float cameraX, cameraY, zoom = 1.0f;
    private float playerHurtTime = 0f;
    private float attackCooldown = 0f;
    private float stageBanner = 2.0f;
    private boolean gameWon = false;
    private boolean gameOver = false;
    private boolean disposed = false;
    // ---- Audio state ----
    private float footstepTimer = 0f;
    private static final float FOOTSTEP_INTERVAL = 0.3f;
    private boolean gameOverSoundPlayed = false;
    private boolean victorySoundPlayed = false;

    // ---- touch controls, profile HUD, inventory, fireballs ----
    private static final float FIRE_COOLDOWN = 0.7f;  // seconds between fireballs
    private static final float FIRE_MP_COST = 10f;
    private static final float FIRE_DAMAGE = 30f;     // the sword does 42
    private static final float MP_REGEN = 2f;         // MP per second

    private final ShapeRenderer shape = new ShapeRenderer();
    private final Joystick joystick = new Joystick();
    private ActionButtons buttons;
    private Fireballs fireballs;
    private Inventory inventory;
    private PlayerHud playerHud;
    private BitmapFont uiFont;          // font of the profile HUD and the inventory (has the peso sign)
    private TextureRegion portrait;     // face shown in the profile picture
    private float playerMp = 50f;
    private final float playerMaxMp = 50f;
    private int pesos = 100;            // money, shown as the peso sign in the HUD
    private float fireCooldown = 0f;
    private float aimX = 1f, aimY = 0f; // direction the next fireball flies (y grows downward)

    /** Tells the fireballs about walls and enemies. */
    private final Fireballs.World fireWorld = new Fireballs.World() {
        @Override
        public boolean isBlocked(float x, float y) {
            return !canStand(x, y);
        }

        @Override
        public boolean hitEnemy(float x, float y) {
            for (Enemy e : enemies) {
                if (e.dead || e.dying) continue;
                if (Math.hypot(e.x - x, e.y - y) < 20.0) {
                    damageEnemy(e, FIRE_DAMAGE);
                    return true;
                }
            }
            return false;
        }
    };

    public GameScreen(Game game, AssetManager assets) {
        this.game = game;
        this.assets = assets;
        init();
    }

    public GameScreen(Game game) {
        this(game, null);
    }

    private void init() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTex = new Texture(pm);
        pm.dispose();

        hudFont = createFont(15);
        smallFont = createFont(12);
        bannerFont = createFont(28);
        titleFont = createFont(40);

        tileAtlas = getTex("map/tiles.png");
        doorFrames = splitStrip(getTex("map/DoorAnimation.png"), 9);
        if (assets != null && assets.isLoaded("enemies/enemies.atlas", TextureAtlas.class)) {
            enemyAtlas = assets.get("enemies/enemies.atlas", TextureAtlas.class);
        } else {
            ownedEnemyAtlas = new TextureAtlas(Gdx.files.internal("enemies/enemies.atlas"));
            enemyAtlas = ownedEnemyAtlas;
        }

        loadMap();
        loadPlayer();
        beginLevel(0);

        uiFont = HudStyle.createFont();
        playerHud = new PlayerHud(uiFont);
        inventory = new Inventory(uiFont);
        buttons = new ActionButtons(hudViewport);
        fireballs = new Fireballs();
        portrait = new TextureRegion(playerFrames.get("down")[0], 22, 28, 56, 56); // head and shoulders
    }

    private BitmapFont createFont(int size) {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/font_body.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        BitmapFont f = gen.generateFont(p);
        gen.dispose();
        return f;
    }

    private Texture getTex(String path) {
        if (assets != null && assets.isLoaded(path, Texture.class)) {
            return assets.get(path, Texture.class);
        }
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(t);
        return t;
    }

    private TextureRegion[] splitStrip(Texture tex, int frames) {
        int cw = tex.getWidth() / frames;
        int ch = tex.getHeight();
        TextureRegion[] out = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            out[i] = new TextureRegion(tex, i * cw, 0, cw, ch);
        }
        return out;
    }

    private void loadMap() {
        JsonValue root = new JsonReader().parse(Gdx.files.internal("map/map.json"));
        mapWidth = root.getInt("mapWidth");
        mapHeight = root.getInt("mapHeight");
        worldHeight = mapHeight * TILE;
        wall = new boolean[mapWidth][mapHeight];
        floor = new boolean[mapWidth][mapHeight];

        List<List<Tile>> parsedLayers = new ArrayList<>();
        JsonValue layers = root.get("layers");
        for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
            String name = layer.getString("name");
            List<Tile> layerTiles = new ArrayList<>();
            boolean solid = isSolidLayer(name);
            boolean isTraps = name.trim().equalsIgnoreCase("traps");
            boolean isBaseFloor = name.equalsIgnoreCase("New layer");
            boolean isDoorLayer = name.equalsIgnoreCase("door wall open");

            JsonValue tiles = layer.get("tiles");
            for (JsonValue tile = tiles.child; tile != null; tile = tile.next) {
                int id = tile.getInt("id");
                int x = tile.getInt("x");
                int y = tile.getInt("y");
                layerTiles.add(new Tile(id, x, y));

                if (x >= 0 && y >= 0 && x < mapWidth && y < mapHeight) {
                    if (solid) wall[x][y] = true;
                    if (isTraps) trapTiles.add((((long) y) << 32) | (x & 0xffffffffL));
                    if (isBaseFloor && (id == 0 || id == 1)) floor[x][y] = true;
                    if (isDoorLayer && id == 78) {
                        doors.add(new Door(x * TILE, y * TILE, 2 * TILE, 2 * TILE));
                    }
                }
            }
            parsedLayers.add(layerTiles);
        }

        // JSON layers are stored front-to-back; reverse order paints the floor first and walls/props above.
        for (int li = parsedLayers.size() - 1; li >= 0; li--) {
            drawTiles.addAll(parsedLayers.get(li));
        }

        // ---- Level 3 bridge runtime collision patch ----
        // The corridor x=29..39, y=17..20 is void in map.json but visually connects the
        // west middle chamber to the Manananggal doors at x=40..41 and x=44..45.
        // Extending to x=29..41 ensures no gap between the patched area and the door cells.
        for (int bx = 29; bx <= 41; bx++) {
            for (int by = 17; by <= 20; by++) {
                if (bx >= 0 && by >= 0 && bx < mapWidth && by < mapHeight) {
                    floor[bx][by] = true;
                    wall[bx][by] = false;
                }
            }
        }

        // Clear static wall collision on door cells; the Door objects handle blocking until opened.
        for (Door d : doors) {
            int x0 = (int) (d.x / TILE);
            int y0 = (int) (d.y / TILE);
            int doorHeightTiles = (int) Math.ceil(d.h / TILE);
            for (int x = x0; x < x0 + 2; x++) {
                for (int y = y0; y < y0 + doorHeightTiles; y++) {
                    if (x >= 0 && y >= 0 && x < mapWidth && y < mapHeight) {
                        wall[x][y] = false;
                    }
                }
            }
        }
    }

    private boolean isSolidLayer(String n) {
        String s = n.toLowerCase(Locale.ROOT);
        return s.equals("wall vertical")
            || s.equals("wall horizontal")
            || s.trim().equals("walls details")
            || s.trim().equals("details wall/ doors");
    }

    private void loadPlayer() {
        playerFrames.put("down", splitStrip(getTex("player/walk_down.png"), 8));
        playerFrames.put("up", splitStrip(getTex("player/walk_up.png"), 7));
        playerFrames.put("left", splitStrip(getTex("player/walk_left.png"), 8));
        playerFrames.put("right", splitStrip(getTex("player/walk_right.png"), 9));
        playerFrames.put("swordLeft", splitStrip(getTex("player/sword_left.png"), 6));
        playerFrames.put("swordRight", splitStrip(getTex("player/sword_right.png"), 6));
    }

    private TextureRegion[] getEnemyStrip(String type, String state) {
        String key = type + "_" + state;
        if (enemyAnimCache.containsKey(key)) {
            return enemyAnimCache.get(key);
        }
        Array<TextureAtlas.AtlasRegion> found = enemyAtlas.findRegions(key);
        if (found == null || found.size == 0) {
            enemyAnimCache.put(key, null);
            return null;
        }
        TextureRegion[] regions = found.toArray(TextureRegion.class);
        enemyAnimCache.put(key, regions);
        return regions;
    }

    private void loadEnemyAnimations(Enemy e) {
        e.walkR = getEnemyStrip(e.type, "walkR");
        e.walkL = getEnemyStrip(e.type, "walkL");
        e.attackR = getEnemyStrip(e.type, "attackR");
        e.attackL = getEnemyStrip(e.type, "attackL");
        e.die = getEnemyStrip(e.type, "die");
        if (e.walkL == null) e.walkL = e.walkR;
        if (e.attackL == null) e.attackL = e.attackR;
    }

    private void beginLevel(int n) {
        level = n;
        if (fireballs != null) fireballs.clear();
        enemies.clear();
        defeatedThisLevel.clear();
        for (Door d : doors) {
            d.open = 0f;
            d.opening = false;
        }
        int[] playerStart = nearestWalkable(SPAWNS[level][0], SPAWNS[level][1]);
        px = playerStart[0] * TILE + 8f;
        py = playerStart[1] * TILE + 8f;

        for (int[] p : ENEMY_SPAWNS[level]) {
            spawnEnemy(KEYS[level], p[0], p[1], false);
        }
        reinforcementsInCurrentLevel = pendingEnemyReinforcements.size();
        for (String type : pendingEnemyReinforcements) {
            spawnEnemy(type, -1, -1, true);
        }
        pendingEnemyReinforcements.clear();
        stageBanner = 2.3f;
        gameOver = false;
        updateCamera(true);
    }

    private void spawnEnemy(String type, int preferredX, int preferredY, boolean reinforcement) {
        int sx = -1, sy = -1;
        if (!reinforcement) {
            int[] safe = nearestWalkable(preferredX, preferredY);
            sx = safe[0];
            sy = safe[1];
        } else {
            int[] area = AREAS[level];
            int minX = Math.max(0, area[0]);
            int maxX = Math.min(mapWidth - 1, area[0] + area[2]);
            int minY = Math.max(0, area[1]);
            int maxY = Math.min(mapHeight - 1, area[1] + area[3]);
            for (int attempt = 0; attempt < 700; attempt++) {
                int x = minX + random.nextInt(Math.max(1, maxX - minX + 1));
                int y = minY + random.nextInt(Math.max(1, maxY - minY + 1));
                float wx = x * TILE + 8f;
                float wy = y * TILE + 8f;
                if (!canStand(wx, wy) || Math.hypot(wx - px, wy - py) < 100.0) continue;
                boolean crowded = false;
                for (Enemy other : enemies) {
                    if (Math.hypot(wx - other.x, wy - other.y) < 48.0) {
                        crowded = true;
                        break;
                    }
                }
                if (!crowded) {
                    sx = x;
                    sy = y;
                    break;
                }
            }
            if (sx < 0) {
                int[] safe = nearestWalkable(SPAWNS[level][0] + 3, SPAWNS[level][1] + 3);
                sx = safe[0];
                sy = safe[1];
            }
        }
        Enemy enemy = new Enemy(type, sx * TILE + 8f, sy * TILE + 8f);
        loadEnemyAnimations(enemy);
        enemies.add(enemy);
    }

    private boolean inDoor(float x, float y) {
        for (Door d : doors) {
            if (d.open < 0.96f && x + 6f > d.x && x - 6f < d.x + d.w && y + 3f > d.y && y - 5f < d.y + d.h) {
                return true;
            }
        }
        return false;
    }

    private boolean canStand(float x, float y) {
        int tx = (int) Math.floor(x / TILE);
        int ty = (int) Math.floor(y / TILE);
        if (tx < 0 || ty < 0 || tx >= mapWidth || ty >= mapHeight) return false;
        if (!floor[tx][ty] || wall[tx][ty]) return false;
        return !inDoor(x, y);
    }

    private int[] nearestWalkable(int tx, int ty) {
        int[] area = AREAS[level];
        int bestX = tx, bestY = ty;
        double best = Double.MAX_VALUE;
        int minX = Math.max(0, area[0]);
        int maxX = Math.min(mapWidth - 1, area[0] + area[2]);
        int minY = Math.max(0, area[1]);
        int maxY = Math.min(mapHeight - 1, area[1] + area[3]);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float cx = x * TILE + 8f;
                float cy = y * TILE + 8f;
                if (canStand(cx, cy)) {
                    double d = (x - tx) * (double) (x - tx) + (y - ty) * (double) (y - ty);
                    if (d < best) {
                        best = d;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }
        return new int[]{bestX, bestY};
    }

    private void setZoom(float value) {
        zoom = Math.max(0.65f, Math.min(2.25f, value));
        updateCamera(false);
    }

    private void updateCamera(boolean snap) {
        float visibleW = worldViewport.getWorldWidth() / zoom;
        float visibleH = worldViewport.getWorldHeight() / zoom;
        float targetX = px - visibleW / 2f;
        float targetY = py - visibleH / 2f;
        targetX = Math.max(0f, Math.min(Math.max(0f, mapWidth * TILE - visibleW), targetX));
        targetY = Math.max(0f, Math.min(Math.max(0f, mapHeight * TILE - visibleH), targetY));
        if (snap) {
            cameraX = targetX;
            cameraY = targetY;
        } else {
            cameraX += (targetX - cameraX) * 0.22f;
            cameraY += (targetY - cameraY) * 0.22f;
        }
        worldCam.zoom = 1f / zoom;
        worldCam.position.set(cameraX + visibleW / 2f, worldHeight - (cameraY + visibleH / 2f), 0f);
        worldCam.update();
    }

    private float toWorldY(float topY, float h) {
        return worldHeight - topY - h;
    }

    private void updateGame(float dt) {
        boolean inventoryWasOpen = inventory.isOpen();
        inventory.update(hudViewport);  // I / tap the bag opens it; ESC closes it
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) && !inventoryWasOpen) {
            openPauseMenu();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Keys.EQUALS) || Gdx.input.isKeyJustPressed(Keys.PLUS)
            || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ADD)) {
            setZoom(zoom * 1.12f);
        }
        if (Gdx.input.isKeyJustPressed(Keys.MINUS) || Gdx.input.isKeyJustPressed(Keys.NUMPAD_SUBTRACT)) {
            setZoom(zoom / 1.12f);
        }

        if (inventory.isOpen()) { // the game waits while the inventory is open
            buttons.pollSword();   // throw away taps made on the buttons meanwhile
            buttons.pollFire();
            return;
        }

        if (gameOver || gameWon) {
            if (Gdx.input.isKeyJustPressed(Keys.R)) {
                pendingEnemyReinforcements.clear();
                defeatedThisLevel.clear();
                reinforcementsInCurrentLevel = 0;
                playerHp = 100f;
                playerMp = playerMaxMp;
                fireCooldown = 0f;
                fireballs.clear();
                gameWon = false;
                gameOver = false;
                gameOverSoundPlayed = false;
                victorySoundPlayed = false;
                beginLevel(0);
            }
            return;
        }

        if (stageBanner > 0f) stageBanner = Math.max(0f, stageBanner - dt);
        if (playerHurtTime > 0f) playerHurtTime = Math.max(0f, playerHurtTime - dt);
        if (attackCooldown > 0f) attackCooldown = Math.max(0f, attackCooldown - dt);
        if (playerAttackTime > 0f) playerAttackTime = Math.max(0f, playerAttackTime - dt);
        if (trapDamageCooldown > 0f) trapDamageCooldown = Math.max(0f, trapDamageCooldown - dt);

        boolean up = Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT);

        float dx = (right ? 1f : 0f) - (left ? 1f : 0f);
        float dy = (down ? 1f : 0f) - (up ? 1f : 0f);
        Vector2 stick = joystick.update(hudViewport);
        dx += stick.x;
        dy -= stick.y; // the joystick's y points up, the dungeon's y points down
        float len = (float) Math.hypot(dx, dy);
        if (len > 0f) {
            dx /= len;
            dy /= len;
            aimX = dx; // fireballs fly where the player last walked
            aimY = dy;
            if (Math.abs(dx) > Math.abs(dy)) {
                facing = dx < 0f ? 2 : 3;
            } else {
                facing = dy < 0f ? 0 : 1;
            }
            playerAnimTime += dt;
            // Footstep SFX every FOOTSTEP_INTERVAL seconds while moving
            footstepTimer -= dt;
            if (footstepTimer <= 0f) {
                AudioManager.getInstance().playFootstep();
                footstepTimer = FOOTSTEP_INTERVAL;
            }
        } else {
            footstepTimer = 0f; // reset so next step fires immediately
        }

        float speed = 2.35f * 60f * dt;
        if (canStand(px + dx * speed, py)) px += dx * speed;
        if (canStand(px, py + dy * speed)) py += dy * speed;

        int playerTileX = (int) Math.floor(px / TILE);
        int playerTileY = (int) Math.floor(py / TILE);
        long playerTileKey = (((long) playerTileY) << 32) | (playerTileX & 0xffffffffL);
        if (trapTiles.contains(playerTileKey) && trapDamageCooldown <= 0f) {
            playerHp = Math.max(0f, playerHp - 8f);
            trapDamageCooldown = 48f / 60f;
            playerHurtTime = 12f / 60f;
            AudioManager.getInstance().playPlayerHurt();
            if (playerHp <= 0f) {
                gameOver = true;
                if (!gameOverSoundPlayed) {
                    AudioManager.getInstance().playGameOver();
                    gameOverSoundPlayed = true;
                }
            }
        }

        boolean swordPressed = Gdx.input.isKeyPressed(Keys.SPACE) | buttons.pollSword();
        boolean firePressed = Gdx.input.isKeyJustPressed(Keys.F) | buttons.pollFire();

        if (swordPressed && attackCooldown <= 0f) {
            attackCooldown = 28f / 60f;
            playerAttackTime = 0.42f;
            AudioManager.getInstance().playSwordSwing();
            for (Enemy e : enemies) {
                if (!e.dead && !e.dying && Math.hypot(e.x - px, e.y - py) < 57.0) {
                    damageEnemy(e, 42f);
                }
            }
        }

        // fireball: costs MP, then has a short cooldown
        if (fireCooldown > 0f) fireCooldown = Math.max(0f, fireCooldown - dt);
        playerMp = Math.min(playerMaxMp, playerMp + MP_REGEN * dt);
        if (firePressed && fireCooldown <= 0f && playerMp >= FIRE_MP_COST) {
            playerMp -= FIRE_MP_COST;
            fireCooldown = FIRE_COOLDOWN;
            fireballs.shoot(px, py, aimX, aimY);
        }
        fireballs.update(dt, fireWorld);

        if (Gdx.input.isKeyJustPressed(Keys.E)) {
            Door nearest = null;
            double best = 64.0;
            for (Door d : doors) {
                double dist = Math.hypot(d.x + d.w / 2f - px, d.y + d.h / 2f - py);
                if (dist < best && d.open < 1f) {
                    best = dist;
                    nearest = d;
                }
            }
            if (nearest != null) {
                nearest.opening = true;
                AudioManager.getInstance().playDoorOpen();
            }
        }

        for (Door d : doors) {
            if (d.opening) {
                d.open = Math.min(1f, d.open + dt / 0.78f);
            }
        }

        float enemySpeed = 0.72f * 60f * dt;
        for (Enemy e : enemies) {
            if (e.dead) continue;
            e.animTime += dt;
            if (e.hitFlash > 0f) e.hitFlash = Math.max(0f, e.hitFlash - dt);
            if (e.attackCooldown > 0f) e.attackCooldown = Math.max(0f, e.attackCooldown - dt);

            if (e.dying) {
                e.deathTime += dt;
                int dieLen = e.die == null ? 0 : e.die.length;
                if (dieLen == 0 || e.deathTime > Math.max(0.65f, dieLen * 0.075f)) {
                    e.dead = true;
                }
                continue;
            }

            double dist = Math.hypot(px - e.x, py - e.y);
            if (!e.alerted && dist < 190.0) {
                e.alerted = true;
                e.alertTime = 2.0f;
                AudioManager.getInstance().playEnemyAlert();
            }
            if (e.alerted && e.alertTime > 0f) {
                e.alertTime = Math.max(0f, e.alertTime - dt);
                e.action = "walk";
                e.facingLeft = px < e.x;
            } else if (e.alerted && dist < 230.0) {
                e.facingLeft = px < e.x;
                double attackRange = e.type.equals("tiyanak") ? 22.0 : 34.0;
                if (dist > attackRange) {
                    e.action = "walk";
                    float vx = (px - e.x) / (float) dist;
                    float vy = (py - e.y) / (float) dist;
                    float nx = e.x + vx * enemySpeed;
                    float ny = e.y + vy * enemySpeed;
                    if (canStand(nx, ny)) {
                        // Full diagonal move is clear
                        e.x = nx;
                        e.y = ny;
                    } else if (canStand(nx, e.y)) {
                        // Wall-slide: horizontal only
                        e.x = nx;
                    } else if (canStand(e.x, ny)) {
                        // Wall-slide: vertical only
                        e.y = ny;
                    }
                    // Safety: if enemy somehow drifted into a non-walkable tile, nudge back
                    if (!canStand(e.x, e.y)) {
                        int[] safe = nearestWalkable((int)(e.x / TILE), (int)(e.y / TILE));
                        e.x = safe[0] * TILE + 8f;
                        e.y = safe[1] * TILE + 8f;
                    }
                } else {
                    e.action = "attack";
                    if (e.attackCooldown <= 0f) {
                        playerHp -= e.type.equals("kapre") ? 13f : 9f;
                        e.attackCooldown = 58f / 60f;
                        playerHurtTime = 12f / 60f;
                        AudioManager.getInstance().playPlayerHurt();
                        if (playerHp <= 0f) {
                            playerHp = 0f;
                            gameOver = true;
                            if (!gameOverSoundPlayed) {
                                AudioManager.getInstance().playGameOver();
                                gameOverSoundPlayed = true;
                            }
                        }
                    }
                }
            } else {
                e.action = "walk";
            }
        }

        enemies.removeIf(e -> e.dead);
        if (enemies.isEmpty()) {
            if (level < TYPES.length - 1) {
                pendingEnemyReinforcements.addAll(defeatedThisLevel);
                AudioManager.getInstance().playLevelUp();
                beginLevel(level + 1);
            } else {
                gameWon = true;
                if (!victorySoundPlayed) {
                    AudioManager.getInstance().playVictory();
                    victorySoundPlayed = true;
                }
            }
        }

        updateCamera(false);
    }

    /** Hurts an enemy (sword or fireball). When its HP is gone it starts dying. */
    private void damageEnemy(Enemy e, float damage) {
        e.hp -= damage;
        e.hitFlash = 7f / 60f;
        if (e.hp <= 0f && !e.dying) {
            e.dying = true;
            e.deathTime = 0f;
            e.action = "die";
            defeatedThisLevel.add(e.type);
            WikiProgress.unlock(e.type);
            AudioManager.getInstance().playEnemyDie();
        } else {
            AudioManager.getInstance().playEnemyHit();
        }
    }

    private void openPauseMenu() {
        if (game instanceof DungeonBound) {
            DungeonBound db = (DungeonBound) game;
            db.setScreen(PauseMenuScreen.create(
                db,
                () -> db.setScreen(this, false),
                () -> { // "Items": back to the game with the inventory open
                    inventory.openPanel();
                    db.setScreen(this, false);
                },
                () -> db.setScreen(this, false),
                () -> {
                    dispose();
                    db.setScreen(new StartMenuScreen(db));
                }
            ), false);
        } else {
            game.setScreen(new StartMenuScreen(game));
            dispose();
        }
    }

    private TextureRegion getTileRegion(int id) {
        TextureRegion r = tileRegions.get(id);
        if (r != null) return r;
        int sx = (id % 8) * TILE;
        int sy = (id / 8) * TILE;
        if (sx + TILE <= tileAtlas.getWidth() && sy + TILE <= tileAtlas.getHeight()) {
            r = new TextureRegion(tileAtlas, sx, sy, TILE, TILE);
            tileRegions.put(id, r);
            return r;
        }
        return null;
    }

    private TextureRegion getAnimFrame(TextureRegion[] frames, float time, float fps) {
        if (frames == null || frames.length == 0) return null;
        int idx = ((int) (time * fps)) % frames.length;
        return frames[idx];
    }

    private void drawRect(float x, float y, float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(pixelTex, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void show() {
        InputMultiplexer input = new InputMultiplexer();
        input.addProcessor(buttons); // sword / fireball buttons
        input.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                setZoom(zoom * (amountY < 0f ? 1.12f : 1f / 1.12f));
                return true;
            }
        });
        Gdx.input.setInputProcessor(input);
        // Start BGM + ambience when entering GameScreen
        AudioManager.getInstance().playMusic("audio/bgm_title.mp3", true, 0.7f);
        AudioManager.getInstance().playAmbience("audio/amb_crickets.wav", true, 0.4f);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);
        AudioManager.getInstance().update(dt);
        updateGame(dt);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ---- World pass
        worldViewport.apply();
        batch.setProjectionMatrix(worldCam.combined);
        batch.begin();

        for (Tile t : drawTiles) {
            TextureRegion r = getTileRegion(t.id);
            if (r != null) {
                batch.draw(r, t.x * TILE, toWorldY(t.y * TILE, TILE), TILE, TILE);
            }
        }

        int[] area = AREAS[level];
        drawRect(area[0] * TILE, toWorldY((area[1] + area[3]) * TILE, 0f),
            area[2] * TILE, area[3] * TILE, new Color(230f / 255f, 180f / 255f, 80f / 255f, 18f / 255f));

        for (Door d : doors) {
            int idx = Math.min(doorFrames.length - 1, (int) (d.open * (doorFrames.length - 1) + 0.0001f));
            batch.draw(doorFrames[idx], d.x, toWorldY(d.y, 57f), 48f, 57f);
        }

        for (Enemy e : enemies) {
            if (e.dead) continue;
            boolean flip = e.facingLeft;
            TextureRegion[] anim;
            if (e.dying) anim = e.die;
            else if (e.action.equals("attack")) anim = flip ? e.attackL : e.attackR;
            else anim = flip ? e.walkL : e.walkR;

            TextureRegion im = getAnimFrame(anim, e.dying ? e.deathTime : e.animTime, e.dying ? 13f : 11f);
            float size = e.type.equals("kapre") ? 58f : (e.type.equals("tiyanak") ? 26f : 48f);
            float drawW = size;
            float drawH = size;
            if (im != null) {
                float ratio = (float) im.getRegionWidth() / Math.max(1, im.getRegionHeight());
                drawW = size * Math.max(0.7f, Math.min(1.35f, ratio));
            }
            if (e.hitFlash > 0f && !e.dying) {
                batch.setColor(1f, 0.7f, 0.7f, 1f);
            }
            float dy = toWorldY(e.y - drawH + 7f, drawH);
            if (im != null) {
                boolean mirror = flip && (anim == e.walkR || anim == e.attackR);
                float dx = e.x - drawW / 2f;
                if (mirror) {
                    batch.draw(im, dx + drawW, dy, -drawW, drawH);
                } else {
                    batch.draw(im, dx, dy, drawW, drawH);
                }
            }
            batch.setColor(Color.WHITE);

            if (!e.dying) {
                float barW = e.type.equals("tiyanak") ? 22f : 38f;
                float barX = e.x - barW / 2f;
                float barY = dy + drawH + 2f;
                drawRect(barX, barY, barW, 5f, new Color(25f / 255f, 15f / 255f, 15f / 255f, 1f));
                drawRect(barX, barY, barW * Math.max(0f, e.hp / e.maxHp), 5f,
                    new Color(190f / 255f, 45f / 255f, 38f / 255f, 1f));
                if (e.alerted && e.alertTime > 0f) {
                    smallFont.setColor(new Color(1f, 220f / 255f, 90f / 255f, 1f));
                    smallFont.draw(batch, "!", e.x - 2f, dy + drawH + 17f);
                }
            }
        }

        TextureRegion[] pAnim = playerAttackTime > 0f
            ? playerFrames.get(facing == 2 ? "swordLeft" : "swordRight")
            : playerFrames.get(facing == 0 ? "up" : facing == 1 ? "down" : facing == 2 ? "left" : "right");
        TextureRegion pFrame = getAnimFrame(pAnim, playerAnimTime, 10f);
        if (pFrame != null) {
            float pw = playerAttackTime > 0f ? 50f : (facing == 1 ? 38f : 34f);
            float ph = playerAttackTime > 0f ? 50f : (facing == 1 ? 54f : 46f);
            if (playerHurtTime > 0f) batch.setColor(1f, 0.55f, 0.55f, 1f);
            batch.draw(pFrame, px - pw / 2f, toWorldY(py - ph + 7f, ph), pw, ph);
            batch.setColor(Color.WHITE);
        }

        fireballs.render(batch, worldHeight);

        batch.end();

        // ---- HUD pass
        hudViewport.apply();
        batch.setProjectionMatrix(hudCam.combined);
        shape.setProjectionMatrix(hudCam.combined);
        float hw = hudViewport.getWorldWidth();
        float hh = hudViewport.getWorldHeight();

        batch.begin();
        drawInfoPanel();   // level / enemies / controls: bottom, next to the joystick
        batch.end();

        joystick.render(shape);                                              // bottom left
        buttons.render(shape, batch, attackCooldown > 0f, fireCooldown / FIRE_COOLDOWN); // bottom right
        playerHud.render(shape, batch, portrait, Math.round(playerHp), 100,
            Math.round(playerMp), Math.round(playerMaxMp), pesos);           // upper left
        inventory.render(shape, batch, hw, hh);                              // bag icon below the profile

        batch.begin();
        drawOverlays();    // level banner, game over / win screen
        batch.end();
    }

    /** The text that used to fill the upper-left corner. Now it sits at the bottom, level with the joystick. */
    private void drawInfoPanel() {
        float jr = joystick.getRadius();
        float panelW = 340f;
        float panelH = 108f;
        float panelX = joystick.getCenterX() + jr + 18f;      // right of the joystick
        float panelY = joystick.getCenterY() - panelH / 2f;   // same height as the joystick
        float top = panelY + panelH;

        drawRect(panelX, panelY, panelW, panelH, new Color(12f / 255f, 10f / 255f, 12f / 255f, 200f / 255f));

        hudFont.setColor(new Color(1f, 225f / 255f, 180f / 255f, 1f));
        hudFont.draw(batch, "MGA NILALANG  •  LEVEL " + (level + 1) + " / 5  •  " + TYPES[level], panelX + 12f, top - 10f);

        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "Enemies: " + enemies.size(), panelX + 12f, top - 32f);

        hudFont.setColor(new Color(230f / 255f, 205f / 255f, 150f / 255f, 1f));
        hudFont.draw(batch, "Returning hostile reinforcements: " + reinforcementsInCurrentLevel, panelX + 12f, top - 54f);

        smallFont.setColor(new Color(245f / 255f, 235f / 255f, 215f / 255f, 1f));
        smallFont.draw(batch, "WASD / Joystick: Move   SPACE: Attack   F: Fireball   E: Door", panelX + 12f, top - 78f);
        smallFont.draw(batch, "I: Items   Wheel / +/-: Zoom   ESC: Pause", panelX + 12f, top - 94f);
    }

    private void drawOverlays() {
        float hh = hudViewport.getWorldHeight();
        float hw = hudViewport.getWorldWidth();

        if (stageBanner > 0f) {
            drawRect(hw / 2f - 170f, hh - 110f, 340f, 62f, new Color(0f, 0f, 0f, 175f / 255f));
            String bMsg = "LEVEL " + (level + 1) + " — " + TYPES[level].toUpperCase(Locale.ROOT);
            bannerFont.setColor(new Color(1f, 222f / 255f, 160f / 255f, 1f));
            layout.setText(bannerFont, bMsg);
            bannerFont.draw(batch, bMsg, (hw - layout.width) / 2f, hh - 70f);
        }

        if (gameOver || gameWon) {
            drawRect(0f, 0f, hw, hh, new Color(0f, 0f, 0f, 210f / 255f));
            String msg = gameWon ? "DUNGEON CLEARED" : "YOU HAVE FALLEN";
            titleFont.setColor(Color.WHITE);
            layout.setText(titleFont, msg);
            titleFont.draw(batch, msg, (hw - layout.width) / 2f, hh / 2f + 40f);

            String sub = gameWon
                ? "You survived all five enemy stages. Press R to play again."
                : "Press R to restart from Level 1.";
            hudFont.setColor(Color.WHITE);
            layout.setText(hudFont, sub);
            hudFont.draw(batch, sub, (hw - layout.width) / 2f, hh / 2f - 10f);
        }
    }

    @Override
    public void resize(int w, int h) {
        worldViewport.update(w, h, false);
        hudViewport.update(w, h, true);
        float hw = hudViewport.getWorldWidth();
        float hh = hudViewport.getWorldHeight();
        joystick.layout(hh);                                        // bottom left
        buttons.layout(hw, hh);                                     // bottom right
        playerHud.layout(hw, hh);                                   // upper left
        inventory.layout(hw, hh, playerHud.getProfileRect());       // icon right below the profile
        updateCamera(true);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        batch.dispose();
        shape.dispose();
        inventory.dispose();
        buttons.dispose();
        fireballs.dispose();
        uiFont.dispose();
        pixelTex.dispose();
        hudFont.dispose();
        smallFont.dispose();
        bannerFont.dispose();
        titleFont.dispose();
        for (Texture t : ownedTextures) {
            t.dispose();
        }
        ownedTextures.clear();
        if (ownedEnemyAtlas != null) {
            ownedEnemyAtlas.dispose();
            ownedEnemyAtlas = null;
        }
        if (assets != null) {
            assets.dispose();
        }
    }
}
