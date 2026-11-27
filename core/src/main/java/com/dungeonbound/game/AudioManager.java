package com.dungeonbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton AudioManager para sa BGM, Ambience, at SFX.
 * May built-in volume control, fading, at resource management.
 */
public class AudioManager {

    private static AudioManager instance;

    private Music currentMusic;
    private String currentMusicPath;
    private float masterMusicVolume = 1.0f;


    private Music currentAmbience;
    private String currentAmbiencePath;
    private float masterAmbienceVolume = 1.0f;

    private float masterSfxVolume = 1.0f;
    private final Map<String, Sound> soundCache = new HashMap<>();

    // Fading state
    private boolean fadingMusic = false;
    private float musicFadeDuration = 0f;
    private float musicFadeTimer = 0f;
    private float musicFadeStartVolume = 0f;
    private float musicFadeTargetVolume = 0f;
    private boolean stopMusicAfterFade = false;

    // Base volume na hiningi ng caller (bago i-multiply sa master) para live ang slider.
    private float currentMusicBase = 1f;
    private float currentAmbienceBase = 1f;

    private static final String PREFS = "dungeonbound_audio";

    private AudioManager() {
        loadSettings();
    }

    // ---------------------------------------------------------- Settings API

    public float getMusicVolume() { return masterMusicVolume; }
    public float getAmbienceVolume() { return masterAmbienceVolume; }
    public float getSfxVolume() { return masterSfxVolume; }

    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    public void cancelFade() {
        fadingMusic = false;
        stopMusicAfterFade = false;
        if (currentMusic != null) {
            currentMusic.setVolume(currentMusicBase * masterMusicVolume);
        }
    }

    public void setMusicVolume(float v) {
        masterMusicVolume = MathUtils.clamp(v, 0f, 1f);
        fadingMusic = false;
        stopMusicAfterFade = false;
        if (currentMusic != null) {
            currentMusic.setVolume(currentMusicBase * masterMusicVolume);
        }
    }

    public void setAmbienceVolume(float v) {
        masterAmbienceVolume = MathUtils.clamp(v, 0f, 1f);
        if (currentAmbience != null) {
            currentAmbience.setVolume(currentAmbienceBase * masterAmbienceVolume);
        }
    }

    public void setSfxVolume(float v) {
        masterSfxVolume = MathUtils.clamp(v, 0f, 1f);
    }

    /** I-save sa Preferences (tawagin kapag umalis na sa Settings screen). */
    public void saveSettings() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        p.putFloat("music", masterMusicVolume);
        p.putFloat("ambience", masterAmbienceVolume);
        p.putFloat("sfx", masterSfxVolume);
        p.flush();
    }

    private void loadSettings() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        masterMusicVolume = MathUtils.clamp(p.getFloat("music", 1f), 0f, 1f);
        masterAmbienceVolume = MathUtils.clamp(p.getFloat("ambience", 1f), 0f, 1f);
        masterSfxVolume = MathUtils.clamp(p.getFloat("sfx", 1f), 0f, 1f);
    }

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    // ------------------------------------------------------------------ Music

    public void playTitleMusic() {
        String bgmPath = Gdx.files.internal("audio/bgm_title.mp3").exists()
                ? "audio/bgm_title.mp3"
                : "audio/bgm_title.ogg";
        playMusic(bgmPath, true, 0.7f);
    }

    public void playMusic(String path, boolean loop, float volume) {
        // If an OGG was requested but a 44.1kHz MP3 exists, use the MP3 to avoid 96kHz buffer underruns
        if (path != null && path.endsWith("bgm_title.ogg") && Gdx.files.internal("audio/bgm_title.mp3").exists()) {
            path = "audio/bgm_title.mp3";
        }

        if (currentMusicPath != null && currentMusicPath.equals(path) && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }

        stopMusic();

        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            Gdx.app.error("AudioManager", "Music file not found: " + path);
            return;
        }

        try {
            currentMusic = Gdx.audio.newMusic(fh);
            currentMusicPath = path;
            currentMusicBase = volume;
            currentMusic.setLooping(loop);
            currentMusic.setVolume(volume * masterMusicVolume);
            currentMusic.play();
            fadingMusic = false;
            stopMusicAfterFade = false;
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Failed to play music: " + path, e);
        }
    }

    public void fadeOutMusic(float duration) {
        if (currentMusic == null || !currentMusic.isPlaying()) return;
        fadingMusic = true;
        musicFadeDuration = duration;
        musicFadeTimer = 0f;
        musicFadeStartVolume = currentMusic.getVolume();
        musicFadeTargetVolume = 0f;
        stopMusicAfterFade = true;
    }

    public void stopMusic() {
        fadingMusic = false;
        if (currentMusic != null) {
            try {
                currentMusic.stop();
                currentMusic.dispose();
            } catch (Exception ignored) {}
            currentMusic = null;
            currentMusicPath = null;
        }
    }

    // --------------------------------------------------------------- Ambience

    public void playAmbience(String path, boolean loop, float volume) {
        if (currentAmbiencePath != null && currentAmbiencePath.equals(path) && currentAmbience != null && currentAmbience.isPlaying()) {
            return;
        }

        stopAmbience();

        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            Gdx.app.error("AudioManager", "Ambience file not found: " + path);
            return;
        }

        try {
            currentAmbience = Gdx.audio.newMusic(fh); // using Music streaming for long/looping ambience
            currentAmbiencePath = path;
            currentAmbienceBase = volume;
            currentAmbience.setLooping(loop);
            currentAmbience.setVolume(volume * masterAmbienceVolume);
            currentAmbience.play();
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Failed to play ambience: " + path, e);
        }
    }

    public void stopAmbience() {
        if (currentAmbience != null) {
            try {
                currentAmbience.stop();
                currentAmbience.dispose();
            } catch (Exception ignored) {}
            currentAmbience = null;
            currentAmbiencePath = null;
        }
    }

    // -------------------------------------------------------------------- SFX

    public void preloadSfx(String path) {
        if (soundCache.containsKey(path)) return;
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) return;
        try {
            soundCache.put(path, Gdx.audio.newSound(fh));
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Failed to preload sound: " + path, e);
        }
    }

    public void playSfx(String path, float volume) {
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) {
            Gdx.app.error("AudioManager", "SFX file not found: " + path);
            return;
        }

        Sound sound = soundCache.get(path);
        if (sound == null) {
            try {
                sound = Gdx.audio.newSound(fh);
                soundCache.put(path, sound);
            } catch (Exception e) {
                Gdx.app.error("AudioManager", "Failed to load sound: " + path, e);
                return;
            }
        }

        try {
            sound.play(volume * masterSfxVolume);
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Failed to play sound: " + path, e);
        }
    }

    public long playSfxId(String path, float volume) {
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) return -1;
        Sound sound = soundCache.get(path);
        if (sound == null) {
            try {
                sound = Gdx.audio.newSound(fh);
                soundCache.put(path, sound);
            } catch (Exception e) {
                return -1;
            }
        }
        try {
            return sound.play(volume * masterSfxVolume);
        } catch (Exception e) {
            return -1;
        }
    }

    public void setSfxSoundVolume(String path, long soundId, float volume) {
        Sound sound = soundCache.get(path);
        if (sound != null && soundId != -1) {
            try {
                sound.setVolume(soundId, MathUtils.clamp(volume, 0f, 1f) * masterSfxVolume);
            } catch (Exception ignored) {}
        }
    }

    public void stopSfx(String path, long soundId) {
        Sound sound = soundCache.get(path);
        if (sound != null) {
            try {
                if (soundId != -1) {
                    sound.stop(soundId);
                } else {
                    sound.stop();
                }
            } catch (Exception ignored) {}
        }
    }

    public void stopSfx(String path) {
        Sound sound = soundCache.get(path);
        if (sound != null) {
            try {
                sound.stop();
            } catch (Exception ignored) {}
        }
    }

    public void playBtnSelect() {
        playSfx("audio/sfx_btn_select.ogg", 0.7f);
    }

    public void playBtnDeselect() {
        playSfx("audio/sfx_btn_deselect.ogg", 0.6f);
    }

    public void playBtnHover() {
        playSfx("audio/sfx_btn_hover.ogg", 0.5f);
    }

    public void playSettingsClick() {
        playSfx("audio/sfx_settings_click.ogg", 0.7f);
    }

    public void playSettingsHover() {
        playSfx("audio/sfx_settings_hover.ogg", 0.45f);
    }

    public void playWikiCardHover() {
        playSfx("audio/sfx_wiki_card_hover.ogg", 0.6f);
    }

    public void playWikiCardSelect() {
        playSfx("audio/sfx_wiki_card_select.ogg", 0.75f);
    }

    // --------------------------------------------------- Gameplay SFX helpers

    public void playFootstep() {
        playSfx("audio/sfx_footstep.wav", 0.45f);
    }

    public void playSwordSwing() {
        playSfx("audio/sfx_sword_swing.wav", 0.8f);
    }

    public void playEnemyHit() {
        playSfx("audio/sfx_enemy_hit.wav", 0.75f);
    }

    public void playEnemyDie() {
        playSfx("audio/sfx_enemy_die.wav", 0.8f);
    }

    public void playEnemyAlert() {
        playSfx("audio/sfx_enemy_alert.wav", 0.7f);
    }

    public void playPlayerHurt() {
        playSfx("audio/sfx_player_hurt.wav", 0.85f);
    }

    public void playDoorOpen() {
        playSfx("audio/sfx_door_open.wav", 0.75f);
    }

    public void playLevelUp() {
        playSfx("audio/sfx_level_up.wav", 0.9f);
    }

    public void playGameOver() {
        playSfx("audio/sfx_game_over.wav", 0.9f);
    }

    public void playVictory() {
        playSfx("audio/sfx_victory.wav", 1.0f);
    }

    // ----------------------------------------------------------------- Update

    /**
     * Tawagin sa bawat render frame para sa smooth fades.
     */
    public void update(float delta) {
        if (fadingMusic && currentMusic != null) {
            musicFadeTimer += delta;
            float progress = Math.min(1f, musicFadeTimer / musicFadeDuration);
            float vol = musicFadeStartVolume + (musicFadeTargetVolume - musicFadeStartVolume) * progress;
            currentMusic.setVolume(Math.max(0f, vol));

            if (progress >= 1f) {
                fadingMusic = false;
                if (stopMusicAfterFade) {
                    stopMusic();
                }
            }
        }
    }

    // ---------------------------------------------------------------- Dispose

    public void dispose() {
        stopMusic();
        stopAmbience();
        for (Sound s : soundCache.values()) {
            try {
                s.dispose();
            } catch (Exception ignored) {}
        }
        soundCache.clear();
    }
}
