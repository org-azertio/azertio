package org.myjtools.openbbt.docgen;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.myjtools.openbbt.core.docgen.StepDocLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Mojo(name = "generate-step-index", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateStepIndexMojo extends AbstractMojo {

    @Parameter(required = true)
    private File inputFile;

    @Parameter(required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        if (!inputFile.exists()) {
            getLog().warn("Input file not found, skipping: " + inputFile);
            return;
        }
        try {
            var inputPath = inputFile.toPath();
            var langPaths = discoverLanguageFiles(inputPath);
            var steps = langPaths.isEmpty()
                ? StepDocLoader.load(inputPath)
                : StepDocLoader.load(inputPath, langPaths);
            outputDirectory.mkdirs();
            String baseName = baseName(inputFile.getName());
            Files.writeString(
                outputDirectory.toPath().resolve(baseName + "-index.json"),
                new StepIndexJsonGenerator().generate(steps)
            );
            getLog().info("Generated " + baseName + "-index.json");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate step index", e);
        }
    }

    private Map<String, Path> discoverLanguageFiles(Path inputPath) throws IOException {
        String base = baseName(inputPath.getFileName().toString());
        var result = new LinkedHashMap<String, Path>();
        try (var siblings = Files.list(inputPath.getParent())) {
            siblings
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.startsWith(base + "_") && name.endsWith(".yaml");
                })
                .sorted()
                .forEach(p -> {
                    String name = p.getFileName().toString();
                    String langCode = name.substring(base.length() + 1, name.length() - ".yaml".length());
                    result.put(langCode, p);
                });
        }
        return result;
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(0, dot) : fileName;
    }
}