package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.ArrayList;
import java.util.List;

/** StartMenu -> LoadingScreen -> GameScreen. Nilo-load ang assets gamit ang AssetManager. */
public class LoadingScreen implements Screen {

    // Pinakamaliit na virtual screen. Kapag mas malapad/mataas ang window o fullscreen,
    // lumalaki ang virtual screen (ExtendViewport), kaya WALANG itim na gilid.
    private static final float MIN_W = 1280f;
    private static final float MIN_H = 720f;

    // Sukat ng na-export na art (1080x720) at posisyon ng bawat layer (top-left, y pababa)
    private static final float ART_W = 1080f;
    private static final float ART_H = 720f;
    private static final float TITLE_X = 236f, TITLE_TOP = 89f,  TITLE_W = 620f, TITLE_H = 441f;
    private static final float TEXT_X  = 479f, TEXT_TOP  = 592f, TEXT_W  = 138f, TEXT_H  = 38f;
    private static final float SPIN_X  = 517f, SPIN_TOP  = 533f, SPIN_S   = 48f;

    // Ang dark panel sa loading_bg ay nasa y 70..650 (art), at ang content (title hanggang text)
    // ay y 89..630. Para hindi lumampas sa panel, scale = taas ng screen / FIT_H.
    // FIT_H = 720 -> 1.0x (kapareho ng mockup). 700 -> 1.03x (pinakamalaki na kasya, may ~11px margin).

    private static final float FIT_H = 700f;
    private static final float MAX_SCALE = 1.6f;

    private static final float MIN_SHOW_TIME = 8.0f;    // 8.0s show time para tumugma sa ~9.2s magical sound
    private static final float FADE_OUT_TIME = 1.2f;    // 1.2s makinis na fade out sa dulo (total = 9.2s)
    private static final float SPIN_STEP     = 1f / 12f; // 12 tuldok, 1 step = 30 degrees

    private final Game game;
    private final Stage stage = new Stage(new ExtendViewport(MIN_W, MIN_H));
    private final List<Texture> textures = new ArrayList<>();
    private final AssetManager assets = new AssetManager();

    private final Image bg;
    private final Image title;
    private final Image spinner;
    private final Image text;

    private float elapsed = 0f;
    private boolean leaving = false;
    private boolean switchNow = false;
    private boolean disposed = false; // para hindi mag-dispose nang dalawang beses

    public LoadingScreen(Game game) {
        this.game = game;

        // ---- UI layers (pinakababa muna): bg -> title -> spinner -> loading text
        bg = new Image(load("ui/loading_bg.png"));
        stage.addActor(bg);

        title = new Image(load("ui/loading_title.png"));
        title.setScaling(Scaling.fit);
        stage.addActor(title);

        spinner = new Image(load("ui/loading_spinner.png"));
        // paikot nang pakanan, 30 degrees kada step para tumugma sa 12 tuldok (pixel-style)
        spinner.addAction(Actions.forever(Actions.sequence(
            Actions.delay(SPIN_STEP),
            new Action() {
                @Override
                public boolean act(float delta) {
                    actor.rotateBy(-30f);
                    return true;
                }
            })));
        stage.addActor(spinner);

        // Tagalog: "Ini ha handa" (loading_text_tl.png), English: "Loading..." (loading_text.png)
        text = new Image(load(Lang.isTL() ? "ui/loading_text_tl.png" : "ui/loading_text.png"));
        stage.addActor(text);

        // I-compute ang layout ayon sa totoong laki ng window
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        layout();

        // ---- I-queue ang assets ng GameScreen dito
        queueAssets();
    }

    /** DITO ILAGAY lahat ng kailangan ng GameScreen. Dapat umiiral ang bawat file sa assets/. */
    private void queueAssets() {
        TextureLoader.TextureParameter nearest = new TextureLoader.TextureParameter();
        nearest.minFilter = Texture.TextureFilter.Nearest;
        nearest.magFilter = Texture.TextureFilter.Nearest;

        assets.load("map/tiles.png", Texture.class, nearest);
        assets.load("map/DoorAnimation.png", Texture.class, nearest);

        String[] playerStrips = {
            "player/walk_down.png", "player/walk_up.png",
            "player/walk_left.png", "player/walk_right.png",
            "player/sword_left.png", "player/sword_right.png"
        };
        for (String p : playerStrips) {
            assets.load(p, Texture.class, nearest);
        }

        assets.load("enemies/enemies.atlas", TextureAtlas.class);
    }

    /** Inaayos ang posisyon at laki ng lahat ng layer ayon sa kasalukuyang laki ng virtual screen. */
    private void layout() {
        float ww = stage.getViewport().getWorldWidth();
        float wh = stage.getViewport().getWorldHeight();
        float sc = Math.min(wh / FIT_H, MAX_SCALE);

        bg.setSize(ww, wh); // puno ang buong screen (iniuunat)
        place(title, ww, wh, sc, TITLE_X, TITLE_TOP, TITLE_W, TITLE_H);
        place(spinner, ww, wh, sc, SPIN_X, SPIN_TOP, SPIN_S, SPIN_S);
        spinner.setOrigin(spinner.getWidth() / 2f, spinner.getHeight() / 2f);
        if (Lang.isTL()) {
            place(text, ww, wh, sc, 452.5f, 599f, 191f, 24f);
        } else {
            place(text, ww, wh, sc, TEXT_X, TEXT_TOP, TEXT_W, TEXT_H);
        }
    }

    /** Art coords (top-left, y pababa) -> world coords, naka-scale at naka-center sa screen. */
    private static void place(Actor a, float ww, float wh, float sc,
                              float artX, float artTop, float w, float h) {
        float x = ww / 2f + (artX - ART_W / 2f) * sc;
        float top = wh / 2f + (artTop - ART_H / 2f) * sc;
        a.setSize(w * sc, h * sc);
        a.setPosition(x, wh - top - a.getHeight());
    }

    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(t);
        return t;
    }

    // ---------------------------------------------------------------- Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                // Developer skip: Space / Enter pagkatapos ng 2s kung tapos nang mag-load ang assets
                if (assets.isFinished() && elapsed >= 2.0f &&
                    (keycode == com.badlogic.gdx.Input.Keys.SPACE || keycode == com.badlogic.gdx.Input.Keys.ENTER)) {
                    startLeaving();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (assets.isFinished() && elapsed >= 2.0f) {
                    startLeaving();
                    return true;
                }
                return false;
            }
        });
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.4f));

        // Patigilin agad ang anumang music at ambience para loading sound lamang ang maririnig
        AudioManager.getInstance().stopMusic();
        AudioManager.getInstance().stopAmbience();
        AudioManager.getInstance().playSfx("audio/sfx_loading.ogg", 0.75f);
    }

    @Override
    public void render(float delta) {
        try {
            renderInner(delta);
        } catch (Throwable t) {
            writeCrashLog(t);
            throw new RuntimeException(t);
        }
    }

    /** Isinusulat ang buong error sa crash_log.txt (nasa working directory ng project). */
    private static void writeCrashLog(Throwable t) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            java.io.File f = new java.io.File("crash_log.txt");
            java.io.FileWriter fw = new java.io.FileWriter(f);
            fw.write(sw.toString());
            fw.close();
            System.err.println("CRASH LOG SAVED: " + f.getAbsolutePath());
            System.err.println(sw);
        } catch (Throwable ignored) {
        }
    }

    private void renderInner(float delta) {
        elapsed += delta;

        // Nagloload ng kaunti kada frame (max ~16 ms) kaya hindi nag-hahang ang spinner
        boolean loaded = assets.update(16);

        // #191919, kapareho ng kulay ng bg sa gilid
        Gdx.gl.glClearColor(25f / 255f, 25f / 255f, 25f / 255f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();

        if (loaded && !leaving && elapsed >= MIN_SHOW_TIME) {
            startLeaving();
        }

        if (switchNow) {
            switchNow = false;
            goToGame();
        }
    }

    private void startLeaving() {
        if (!leaving) {
            leaving = true;
            stage.getRoot().addAction(Actions.sequence(
                Actions.fadeOut(FADE_OUT_TIME),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        switchNow = true; // lilipat pagkatapos ng makinis na fade out
                    }
                })));
        }
    }

    private void goToGame() {
        try {
            Gdx.app.log("LoadingScreen", "1. lilipat na sa GameScreen");
            game.setScreen(new GameScreen(game, assets));
            Gdx.app.log("LoadingScreen", "2. nakalipat na sa GameScreen");
            dispose();
            Gdx.app.log("LoadingScreen", "3. tapos dispose");
        } catch (Throwable t) {
            writeCrashLog(t);
            throw new RuntimeException(t);
        }
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        layout(); // i-recompute kapag nag-fullscreen o nag-resize ang window
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (disposed) return; // puwedeng tawagin ulit ng Game/launcher kapag nagsasara ang app
        disposed = true;
        AudioManager.getInstance().stopSfx("audio/sfx_loading.ogg");
        stage.dispose();
        for (Texture t : textures) t.dispose();
        textures.clear();
        // WAG i-dispose ang `assets`; gagamitin pa ng GameScreen at siya ang magdi-dispose nito.
    }
}
