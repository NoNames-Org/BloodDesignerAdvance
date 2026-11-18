package dev.no.name.bdadvance.modes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.TreeSet;

public final class ModeFavorites {
    private final Set<String> ids = new TreeSet<>();
    private Runnable onChange = () -> { };

    public void setOnChange(Runnable listener) {
        onChange = listener;
    }

    public boolean contains(String id) {
        return ids.contains(id);
    }

    public boolean toggle(String id) {
        boolean added = ids.add(id);
        if (!added) {
            ids.remove(id);
        }
        onChange.run();
        return added;
    }

    public <T> List<T> order(List<T> items, Function<T, String> id) {
        List<T> result = new ArrayList<>(items.size());
        items.stream().filter(item -> contains(id.apply(item))).forEach(result::add);
        items.stream().filter(item -> !contains(id.apply(item))).forEach(result::add);
        return List.copyOf(result);
    }

    public Set<String> all() {
        return Set.copyOf(ids);
    }

    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        ids.forEach(array::add);
        return array;
    }

    public void fromJson(JsonElement json) {
        ids.clear();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> {
                if (element.isJsonPrimitive()) {
                    ids.add(element.getAsString());
                }
            });
        }
    }
}
