package org.azertio.docgen;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.azertio.core.docgen.ConfigDocLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Mojo(name = "generate-config", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateConfigDocsMojo extends AbstractMojo {

    @Parameter(required = true)
    private File inputFile;

    @Parameter(required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "Configuration Reference")
    private String title;

    @Override
    public void execute() throws MojoExecutionException {
        if (!inputFile.exists()) {
            getLog().warn("Input file not found, skipping: " + inputFile);
            return;
        }
        try {
            var entries = ConfigDocLoader.load(inputFile.toPath());
            outputDirectory.mkdirs();
            String baseName = baseName(inputFile.getName());
            Files.writeString(
                outputDirectory.toPath().resolve(baseName + ".md"),
                new ConfigDocMarkdownGenerator().generate(title, inputFile.getName(), entries)
            );
            getLog().info("Generated " + baseName + ".md");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate config docs", e);
        }
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(0, dot) : fileName;
    }
}