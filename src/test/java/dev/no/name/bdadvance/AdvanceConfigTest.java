package dev.no.name.bdadvance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvanceConfigTest {
    @TempDir
    Path temp;

    @Test
    void legacyConfigCountsOnlyWithOwnKeys() throws IOException {
        Path rewritten = temp.resolve("afriends.json");
        Files.writeString(rewritten, "{\"friends\":[]}");
        Path old = temp.resolve("blooddesigner.json");
        Files.writeString(old, "{\"favoriteModes\":[\"duels\"]}");

        assertFalse(AdvanceConfig.hasOwnKeys(rewritten));
        assertTrue(AdvanceConfig.hasOwnKeys(old));
        assertFalse(AdvanceConfig.hasOwnKeys(temp.resolve("missing.json")));
    }
}
