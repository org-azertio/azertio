package org.azertio.core.test.execution;

import org.azertio.core.execution.Profile;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTest {

    @Test
    void applyProfile_withEmptyProperties_returnsBaseConfigUnchanged() {
        Config config = Config.ofMap(Map.of("url", "http://example.com"));
        Config result = Profile.NONE.applyProfile(config);
        assertThat(result.get("url", String.class)).contains("http://example.com");
    }

    @Test
    void applyProfile_replacesPlaceholders() {
        Config config = Config.ofMap(Map.of("url", "http://${host}:${port}/api"));
        Profile profile = new Profile("test", Map.of("host", "localhost", "port", "8080"));
        Config result = profile.applyProfile(config);
        assertThat(result.get("url", String.class)).contains("http://localhost:8080/api");
    }

    @Test
    void applyProfile_unknownPlaceholder_keepsOriginal() {
        Config config = Config.ofMap(Map.of("url", "http://${unknown}/api"));
        Profile profile = new Profile("test", Map.of("host", "localhost"));
        Config result = profile.applyProfile(config);
        assertThat(result.get("url", String.class)).contains("http://${unknown}/api");
    }

    @Test
    void applyProfile_mixedKnownAndUnknown_replacesOnlyKnown() {
        Config config = Config.ofMap(Map.of("val", "${a}-${b}-${c}"));
        Profile profile = new Profile("test", Map.of("a", "X", "c", "Z"));
        Config result = profile.applyProfile(config);
        assertThat(result.get("val", String.class)).contains("X-${b}-Z");
    }
}