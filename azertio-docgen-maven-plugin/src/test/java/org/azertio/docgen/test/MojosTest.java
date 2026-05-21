package org.azertio.docgen.test;

import org.apache.maven.plugin.MojoExecutionException;
import org.azertio.docgen.GenerateConfigDocsMojo;
import org.azertio.docgen.GenerateStepDocsMojo;
import org.azertio.docgen.GenerateStepIndexMojo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MojosTest {

    private Path testResource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    private static void set(Object obj, String fieldName, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    // ── GenerateStepDocsMojo ──────────────────────────────────────────────────

    @Test
    void stepDocsMojo_skipsWhenInputFileNotFound(@TempDir Path tempDir) throws Exception {
        var mojo = new GenerateStepDocsMojo();
        set(mojo, "inputFile", tempDir.resolve("missing.yaml").toFile());
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "title", "Steps");
        mojo.execute();
    }

    @Test
    void stepDocsMojo_generatesMarkdown(@TempDir Path tempDir) throws Exception {
        var mojo = new GenerateStepDocsMojo();
        set(mojo, "inputFile", testResource("step-doc.yaml").toFile());
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "title", "Step Reference");
        mojo.execute();
        assertThat(Files.exists(tempDir.resolve("step-doc.md"))).isTrue();
    }

    // ── GenerateStepIndexMojo ─────────────────────────────────────────────────

    @Test
    void stepIndexMojo_skipsWhenInputFileNotFound(@TempDir Path tempDir) throws Exception {
        var mojo = new GenerateStepIndexMojo();
        set(mojo, "inputFile", tempDir.resolve("missing.yaml").toFile());
        set(mojo, "outputDirectory", tempDir.toFile());
        mojo.execute();
    }

    @Test
    void stepIndexMojo_generatesJson(@TempDir Path tempDir) throws Exception {
        var mojo = new GenerateStepIndexMojo();
        set(mojo, "inputFile", testResource("step-doc.yaml").toFile());
        set(mojo, "outputDirectory", tempDir.toFile());
        mojo.execute();
        assertThat(Files.exists(tempDir.resolve("step-doc-index.json"))).isTrue();
    }

    // ── GenerateConfigDocsMojo ────────────────────────────────────────────────

    @Test
    void configDocsMojo_skipsWhenInputFileNotFound(@TempDir Path tempDir) throws Exception {
        var mojo = new GenerateConfigDocsMojo();
        set(mojo, "inputFile", tempDir.resolve("missing.yaml").toFile());
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "title", "Config");
        mojo.execute();
    }

    @Test
    void configDocsMojo_generatesMarkdown(@TempDir Path tempDir) throws Exception {
        var mojo = new GenerateConfigDocsMojo();
        set(mojo, "inputFile", testResource("config.yaml").toFile());
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "title", "Config Reference");
        mojo.execute();
        assertThat(Files.exists(tempDir.resolve("config.md"))).isTrue();
    }
}