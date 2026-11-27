package com.dungeonbound.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Lore ng isang creature: gumagalaw na enemy sa kaliwa, title bar + lore (justified) sa kanan.
 * Kulay ng background / panel / title bar ay galing sa WikiCreature.
 * LEFT/RIGHT = ibang unlocked na creature, BACK (ESC) = balik sa wiki.
 */
public class LoreScreen implements Screen {

    private static final float W = 1920, H = 1080;

    // ====== FONTS (TODO: palitan ng final fonts; pixel font para sa title, bold sans para sa body) ======
    private static final String TITLE_FONT = "fonts/font_body.ttf";
    private static final String BODY_FONT  = "fonts/font_body.ttf";
    private static final Color TITLE_COLOR = new Color(1f, 0.93f, 0.75f, 1f);   // cream
    private static final Color BODY_COLOR  = Color.WHITE;

    // ====== LAYOUT ======
    private static final float PANEL_X = 980, PANEL_Y = 40, PANEL_W = 900, PANEL_H = 1000;
    private static final float BAR_H = 92, BAR_LINE_H = 6;
    private static final float TEXT_PAD_X = 48, TEXT_PAD_TOP = 36, TEXT_PAD_BOTTOM = 40;

    // ====== ANIMATION ======
    private static final float FRAME_TIME = 0.12f;     // segundo kada frame
    private static final float SPRITE_SIZE = 760f;     // 256px frame, naka-scale (mas malaki pa sa card, gaya ng mockup)
    private static final float SPRITE_FEET_Y = 220f;  // taas ng paa ng enemy mula sa ilalim ng screen
    private static final float CARD_H = 720f;          // taas ng blurred card (lapad ayon sa aspect ng PNG)
    private static final float CARD_CX = 470f, CARD_CY = 540f;

    // ====== TRANSITION ======
    private static final float SLIDE = 140f;           // layo ng pasok (px)
    private static final float T_IN = 0.45f;           // tagal ng pasok (segundo)

    private final Game game;
    private final int index;
    private final Runnable onExit;
    private final Stage stage = new Stage(new FitViewport(W, H));
    private TextureAtlas atlas;
    private boolean ownsAtlas;
    private final List<Texture> textures = new ArrayList<>();
    private final List<BitmapFont> fonts = new ArrayList<>();
    private Texture whiteTex;
    private boolean initialized;
    private boolean disposed;

    public LoreScreen(Game game, int index) {
        this(game, index, null);
    }

    public LoreScreen(Game game, int index, Runnable onExit) {
        this.game = game;
        this.index = index;
        this.onExit = onExit;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        if (initialized) return;
        initialized = true;
        WikiCreature c = WikiCreature.ALL.get(index);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (GameControls.is(GameControls.Action.BACK, keycode)
                        || GameControls.is(GameControls.Action.SELECT, keycode)) { back(); return true; }
                if (GameControls.is(GameControls.Action.LEFT, keycode))  { browse(-1); return true; }
                if (GameControls.is(GameControls.Action.RIGHT, keycode)) { browse(1);  return true; }
                return false;
            }
        });

        if (game instanceof DungeonBound) {
            atlas = ((DungeonBound) game).getWikiAtlas();
            ownsAtlas = false;
        } else {
            atlas = new TextureAtlas(Gdx.files.internal("wiki/wiki.atlas"));
            ownsAtlas = true;
        }

        // 1. Background (solid, kulay ng creature)
        Image bg = new Image(solid(c.bg));
        bg.setSize(W, H);
        stage.addActor(bg);

        // 2. Kaliwa: blurred card + gumagalaw na enemy (isang group para sabay ang transition)
        Group left = new Group();
        left.setTouchable(Touchable.disabled);

        TextureRegion blurRegion = atlas.findRegion(c.blurRegion);
        float cardH = CARD_H;
        float cardW = cardH * blurRegion.getRegionWidth() / (float) blurRegion.getRegionHeight();
        float cardX = CARD_CX - cardW / 2f, cardY = CARD_CY - cardH / 2f;
        Image blurCard = new Image(blurRegion);
        blurCard.setSize(cardW, cardH);
        blurCard.setPosition(cardX, cardY);
        left.addActor(blurCard);

        // paa ng enemy ay nasa ibabang bahagi ng frame sa blurred card
        AnimatedImage enemy = new AnimatedImage(loadAnimation(c.animRegion));
        enemy.setSize(SPRITE_SIZE, SPRITE_SIZE);
        enemy.setPosition(CARD_CX - SPRITE_SIZE / 2f, SPRITE_FEET_Y - SPRITE_SIZE * 0.08f);
        left.addActor(enemy);
        stage.addActor(left);

        // 3. Kanan: panel + title + lore (isang group para sa slide-in)
        Group right = new Group();
        stage.addActor(right);

        Image panel = new Image(solid(c.panel));
        panel.setSize(PANEL_W, PANEL_H);
        panel.setPosition(PANEL_X, PANEL_Y);
        right.addActor(panel);

        // 5. Title bar + cream na linya sa ilalim
        float barX = PANEL_X + 40, barW = PANEL_W - 80;
        float barTop = PANEL_Y + PANEL_H - 10;
        float barY = barTop - BAR_H;

        Image bar = new Image(solid(c.bar));
        bar.setSize(barW, BAR_H);
        bar.setPosition(barX, barY);
        right.addActor(bar);

        Image barLine = new Image(solid(TITLE_COLOR));
        barLine.setSize(barW, BAR_LINE_H);
        barLine.setPosition(barX, barY - BAR_LINE_H);
        right.addActor(barLine);

        BitmapFont titleFont = getFont(TITLE_FONT, 64);
        Label title = new Label(c.name.toUpperCase(), new Label.LabelStyle(titleFont, TITLE_COLOR));
        title.setAlignment(Align.center);
        title.setSize(barW, BAR_H);
        title.setPosition(barX, barY);
        right.addActor(title);

        // 6. Lore text (justified). Liliit ang font hanggang kumasya sa panel.
        float textX = PANEL_X + TEXT_PAD_X;
        float textW = PANEL_W - TEXT_PAD_X * 2;
        float textTop = barY - BAR_LINE_H - TEXT_PAD_TOP;
        float avail = textTop - (PANEL_Y + TEXT_PAD_BOTTOM);
        String[] paragraphs = c.getLore().split("\n\n");

        JustifiedText text = null;
        for (int size : new int[] {32, 30, 28, 26, 24, 22}) {
            BitmapFont f = getFont(BODY_FONT, size);
            text = new JustifiedText(f, BODY_COLOR, paragraphs, textW, size * 1.28f, size * 0.75f);
            if (text.getHeight() <= avail) break;
        }
        text.setPosition(textX, textTop - text.getHeight());
        right.addActor(text);

        // 7. Back hint (clickable)
        BitmapFont smallFont = getFont(BODY_FONT, 28);
        Label backLbl = new Label(Strings.btnBack(), new Label.LabelStyle(smallFont, Color.WHITE));
        backLbl.setPosition(60, 40);
        backLbl.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { back(); }
        });
        stage.addActor(backLbl);

        // ====== TRANSITION ======
        // kaliwa: slide mula sa kaliwa + fade
        left.setPosition(-SLIDE, 0);
        left.getColor().a = 0f;
        left.addAction(Actions.parallel(
            Actions.moveTo(0, 0, T_IN, Interpolation.pow3Out),
            Actions.fadeIn(T_IN, Interpolation.pow2Out)));

        // kanan: slide mula sa kanan + fade (may kaunting delay)
        right.setPosition(SLIDE, 0);
        right.getColor().a = 0f;
        right.addAction(Actions.sequence(
            Actions.delay(0.08f),
            Actions.parallel(
                Actions.moveTo(0, 0, T_IN, Interpolation.pow3Out),
                Actions.fadeIn(T_IN, Interpolation.pow2Out))));

        // cream na linya sa ilalim ng title: lumalawak mula kaliwa
        barLine.setOrigin(0, 0);
        barLine.setScale(0f, 1f);
        barLine.addAction(Actions.sequence(
            Actions.delay(0.3f),
            Actions.scaleTo(1f, 1f, 0.4f, Interpolation.pow2Out)));

        // lore text: sumusunod na fade-in pagkatapos pumasok ang panel
        text.getColor().a = 0f;
        text.addAction(Actions.sequence(
            Actions.delay(0.35f),
            Actions.fadeIn(0.45f)));

        // back hint
        backLbl.getColor().a = 0f;
        backLbl.addAction(Actions.sequence(Actions.delay(0.5f), Actions.fadeIn(0.3f)));
    }

    private void back() {
        AudioManager.getInstance().playBtnDeselect();
        goTo(new WikiMenuScreen(game, index, onExit));
    }

    /** Lipat sa kalapit na unlocked na creature (walang wrap). */
    private void browse(int dir) {
        for (int i = index + dir; i >= 0 && i < WikiCreature.ALL.size(); i += dir) {
            if (WikiProgress.isUnlocked(WikiCreature.ALL.get(i).id)) {
                AudioManager.getInstance().playWikiCardHover();
                goTo(new LoreScreen(game, i, onExit));
                return;
            }
        }
    }

    private void goTo(Screen next) {
        game.setScreen(next);
        Gdx.app.postRunnable(this::dispose);
    }

    // ---------- Assets ----------

    /** Nilo-load ang mga naka-pack na frames mula sa TextureAtlas at ginagawang looping animation. */
    private Animation<TextureRegion> loadAnimation(String animRegion) {
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(animRegion);
        TextureRegion[] frames = new TextureRegion[regions.size];
        for (int i = 0; i < regions.size; i++) {
            frames[i] = regions.get(i);
        }
        Animation<TextureRegion> anim = new Animation<TextureRegion>(FRAME_TIME, frames);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    private Drawable solid(Color color) {
        if (whiteTex == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whiteTex = new Texture(pm);
            pm.dispose();
            textures.add(whiteTex);
        }
        return new TextureRegionDrawable(new TextureRegion(whiteTex)).tint(color);
    }

    private BitmapFont getFont(String path, int size) {
        if (game instanceof DungeonBound && BODY_FONT.equals(path)) {
            return ((DungeonBound) game).getWikiFont(size);
        }
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(path));
        BitmapFont f = font(gen, size);
        gen.dispose();
        return f;
    }

    private BitmapFont font(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        BitmapFont f = gen.generateFont(p);
        fonts.add(f);
        return f;
    }

    // ---------- Custom actors ----------

    /** Nagpe-play ng looping Animation, naka-scale sa size ng actor. */
    private static class AnimatedImage extends Actor {
        private final Animation<TextureRegion> anim;
        private float time;

        AnimatedImage(Animation<TextureRegion> anim) {
            this.anim = anim;
            setTouchable(Touchable.disabled);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            time += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color c = getColor();
            batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
            batch.draw(anim.getKeyFrame(time), getX(), getY(), getWidth(), getHeight());
            batch.setColor(Color.WHITE);
        }
    }

    /**
     * Justified na text: parehong gilid (kaliwa at kanan) ay pantay, maliban sa huling linya ng bawat
     * paragraph. Wala itong suporta sa Label kaya manual ang layout.
     */
    private static class JustifiedText extends Actor {
        private static class Line {
            final List<String> words = new ArrayList<>();
            final List<Float> widths = new ArrayList<>();
            boolean justify;
            float top; // layo mula sa taas ng actor
        }

        private final BitmapFontCache cache;
        private final Color color;
        private float lastAlpha = -1f;

        JustifiedText(BitmapFont font, Color color, String[] paragraphs, float width, float lineH, float paraGap) {
            this.cache = new BitmapFontCache(font);
            this.color = color;
            setTouchable(Touchable.disabled);

            GlyphLayout gl = new GlyphLayout();
            gl.setText(font, "a a");
            float aa = gl.width;
            gl.setText(font, "aa");
            float spaceW = Math.max(aa - gl.width, font.getSpaceXadvance());

            List<Line> lines = new ArrayList<>();
            float y = 0;
            for (int p = 0; p < paragraphs.length; p++) {
                Line cur = new Line();
                float curW = 0;
                for (String w : paragraphs[p].trim().split("\\s+")) {
                    gl.setText(font, w);
                    float ww = gl.width;
                    float need = curW + (cur.words.isEmpty() ? 0 : spaceW) + ww;
                    if (need > width && !cur.words.isEmpty()) {
                        cur.justify = true;
                        cur.top = y;
                        lines.add(cur);
                        y += lineH;
                        cur = new Line();
                        curW = 0;
                        need = ww;
                    }
                    cur.words.add(w);
                    cur.widths.add(ww);
                    curW = need;
                }
                cur.justify = false;
                cur.top = y;
                lines.add(cur);
                y += lineH;
                if (p < paragraphs.length - 1) y += paraGap;
            }
            setSize(width, y);

            cache.setColor(color);
            for (Line line : lines) {
                int n = line.words.size();
                float gap = spaceW;
                if (line.justify && n > 1) {
                    float sum = 0;
                    for (float w : line.widths) sum += w;
                    gap = (width - sum) / (n - 1);
                }
                float x = 0;
                for (int i = 0; i < n; i++) {
                    cache.addText(line.words.get(i), x, -line.top);
                    x += line.widths.get(i) + gap;
                }
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float alpha = color.a * getColor().a * parentAlpha;
            if (Math.abs(alpha - lastAlpha) > 0.001f) {
                lastAlpha = alpha;
                cache.tint(new Color(color.r, color.g, color.b, alpha));
            }
            cache.setPosition(getX(), getY() + getHeight());
            cache.draw(batch);
        }
    }

    // ---------- Screen ----------

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        stage.dispose();
        if (ownsAtlas && atlas != null) atlas.dispose();
        for (Texture t : textures) t.dispose();
        for (BitmapFont f : fonts) f.dispose();
    }
}
