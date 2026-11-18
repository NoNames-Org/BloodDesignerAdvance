package dev.no.name.bdadvance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.no.name.bdadvance.modes.ModeFavorites;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Настройки аддона в config/bdadvance/config.json: избранные режимы и включённые части.
 * При первом запуске берёт их из конфига BloodDesigner, где они жили раньше.
 */
public final class AdvanceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(BdAdvance.NAME);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final String FAVORITE_MODES = "favoriteModes";
    static final String FEATURES = "features";

    private final ModeFavorites favorites;
    private final Path file;
    private final List<Path> legacy;

    /** legacy — старые конфиги по порядку: первый найденный используется, если своего ещё нет. */
    public AdvanceConfig(ModeFavorites favorites, Path file, List<Path> legacy) {
        this.favorites = favorites;
        this.file = file;
        this.legacy = List.copyOf(legacy);
    }

    public void load() {
        Path source = Files.exists(file) ? file : legacy.stream().filter(AdvanceConfig::hasOwnKeys).findFirst().orElse(null);
        if (source == null) {
            return;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(source));
            if (!root.isJsonObject()) {
                LOGGER.warn("Конфиг {} не объект JSON — пропускаю", source);
                return;
            }
            apply(root.getAsJsonObject());
            if (source != file) {
                LOGGER.info("{}: настройки перенесены из {}", BdAdvance.NAME, source);
                save();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Не удалось прочитать конфиг {}", source, e);
        }
    }

    // старый конфиг подходит, только если в нём есть наши ключи: AFriends переписывает свой config.json без них
    static boolean hasOwnKeys(Path path) {
        try {
            JsonElement root = JsonParser.parseString(Files.readString(path));
            return root.isJsonObject() && (root.getAsJsonObject().has(FAVORITE_MODES) || root.getAsJsonObject().has(FEATURES));
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    void apply(JsonObject json) {
        if (json.has(FAVORITE_MODES)) {
            favorites.fromJson(json.get(FAVORITE_MODES));
        }
        if (json.has(FEATURES)) {
            AdvanceFeatures.fromJson(json.get(FEATURES));
        }
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.add(FAVORITE_MODES, favorites.toJson());
        json.add(FEATURES, AdvanceFeatures.toJson());
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(json));
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Не удалось сохранить конфиг в {}", file, e);
        }
    }
}
