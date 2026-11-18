package dev.no.name.bdadvance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumSet;
import java.util.Set;

/** Какие части BloodDesignerAdvance включены. По умолчанию всё включено; храним только выключенные. */
public final class AdvanceFeatures {
    public enum Feature {
        PARTY("party", "Пати", "Кнопка и панель пати в чате"),
        BATTLEPASS("battlepass", "Батл пасс", "Своё меню БП, заданий и покупки вместо серверного"),
        STORE("store", "Магазин", "Своё меню магазина (/donate) с подтверждением покупки"),
        MODES("modes", "Выбрать режим", "Кнопка навигатора режимов в меню паузы");

        private final String id;
        private final String title;
        private final String description;

        Feature(String id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }

        public String title() {
            return title;
        }

        public String description() {
            return description;
        }
    }

    private static final Set<Feature> DISABLED = EnumSet.noneOf(Feature.class);
    private static Runnable onChange = () -> { };

    private AdvanceFeatures() {
    }

    public static void setOnChange(Runnable listener) {
        onChange = listener;
    }

    public static boolean enabled(Feature feature) {
        return !DISABLED.contains(feature);
    }

    public static void toggle(Feature feature) {
        if (!DISABLED.remove(feature)) {
            DISABLED.add(feature);
        }
        onChange.run();
    }

    public static JsonElement toJson() {
        JsonObject json = new JsonObject();
        for (Feature feature : Feature.values()) {
            json.addProperty(feature.id, enabled(feature));
        }
        return json;
    }

    public static void fromJson(JsonElement json) {
        DISABLED.clear();
        if (!json.isJsonObject()) {
            return;
        }
        JsonObject object = json.getAsJsonObject();
        for (Feature feature : Feature.values()) {
            JsonElement value = object.get(feature.id);
            if (value != null && value.isJsonPrimitive() && !value.getAsBoolean()) {
                DISABLED.add(feature);
            }
        }
    }
}
