package com.demo.mota.engine.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    private static volatile ResourceManager instance;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Image> tileImageCache = new HashMap<>();
    private final Map<String, Image> itemImageCache = new HashMap<>();
    private final Map<String, Image> monsterImageCache = new HashMap<>();
    private final Map<Integer, Image> playerSpriteCache = new HashMap<>();

    private ResourceManager() {
        loadSpriteSheetConfig();
        loadPlayerSprites();
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            synchronized (ResourceManager.class) {
                if (instance == null) {
                    instance = new ResourceManager();
                }
            }
        }
        return instance;
    }

    // --- Centralized I/O ---

    public InputStream getResourceStream(String resourcePath) {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        return is;
    }

    public <T> T loadJsonResource(String resourcePath, TypeReference<T> typeRef) {
        try (InputStream is = getResourceStream(resourcePath)) {
            return objectMapper.readValue(is, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + resourcePath, e);
        }
    }

    // --- Sprite Sheet ---

    @SuppressWarnings("unchecked")
    private void loadSpriteSheetConfig() {
        try (InputStream configStream = getResourceStream("/data/spritesheet-config.json")) {
            Map<String, Object> config = objectMapper.readValue(
                    configStream, new TypeReference<>() {});

            String sheetPath = (String) config.get("spriteSheet");
            int tileWidth = (int) config.get("tileWidth");
            int tileHeight = (int) config.get("tileHeight");

            Image spriteSheet = new Image(getResourceStream(sheetPath));
            PixelReader reader = spriteSheet.getPixelReader();

            Map<String, Map<String, Integer>> tiles =
                    (Map<String, Map<String, Integer>>) config.get("tiles");

            for (Map.Entry<String, Map<String, Integer>> entry : tiles.entrySet()) {
                String resourceId = entry.getKey();
                int col = entry.getValue().get("col");
                int row = entry.getValue().get("row");
                WritableImage subImage = new WritableImage(
                        reader,
                        col * tileWidth, row * tileHeight,
                        tileWidth, tileHeight);
                tileImageCache.put(resourceId, subImage);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load spritesheet config", e);
        }
    }

    public Image getTileImage(String resourceId) {
        return tileImageCache.get(resourceId);
    }

    // --- Player Sprites ---

    private void loadPlayerSprites() {
        try (InputStream is = getClass().getResourceAsStream("/images/011-Braver01.png")) {
            if (is == null) return;
            Image playerSheet = new Image(is);
            PixelReader reader = playerSheet.getPixelReader();

            int frameW = 32;
            int frameH = (int) (playerSheet.getHeight() / 4);
            //int staticCol = 0;

            // Direction ordinals: UP=0, DOWN=1, LEFT=2, RIGHT=3
            // Sprite sheet rows: row0=DOWN, row1=LEFT, row2=RIGHT, row3=UP
            int[] dirToRow = {3, 0, 1, 2};
            for (int dirIdx = 0; dirIdx < 4; dirIdx++) {
                int row = dirToRow[dirIdx];
                WritableImage frame = new WritableImage(
                        reader,
                        0, row * frameH,
                        frameW, frameH);
                playerSpriteCache.put(dirIdx, frame);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load player sprites", e);
        }
    }

    public Image getPlayerSprite(int directionOrdinal) {
        return playerSpriteCache.get(directionOrdinal);
    }

    // --- Item Images ---

    public void registerItemImage(String itemId, String imageFileName) {
        if (imageFileName == null || imageFileName.isEmpty()) return;
        if (itemImageCache.containsKey(itemId)) return;
        InputStream is = getClass().getResourceAsStream("/images/" + imageFileName);
        if (is != null) {
            itemImageCache.put(itemId, new Image(is));
        }
    }

    public Image getItemImage(String itemId) {
        return itemImageCache.get(itemId);
    }

    // --- Monster Images ---

    public void registerMonsterImage(String monsterId, String imageFileName) {
        if (imageFileName == null || imageFileName.isEmpty()) return;
        if (monsterImageCache.containsKey(monsterId)) return;
        InputStream is = getClass().getResourceAsStream("/images/" + imageFileName);
        if (is != null) {
            monsterImageCache.put(monsterId, new Image(is));
        }
    }

    public Image getMonsterImage(String monsterId) {
        return monsterImageCache.get(monsterId);
    }
}
