package ennuo.toolkit.functions;
import ennuo.craftworld.types.LevelData;
import java.io.File;

public class LevelJSONCallbacks {
    public static LevelData loadLevelJSON(File file) {
        LevelData levelData = new LevelData(file);
        return levelData;
    }
}
