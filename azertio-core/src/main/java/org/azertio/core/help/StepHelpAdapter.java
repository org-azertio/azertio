package org.azertio.core.help;

import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.docgen.StepDocLoader;
import org.azertio.core.docgen.StepDocMarkdownGenerator;
import org.azertio.core.util.Log;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class StepHelpAdapter implements HelpProvider {

    private static final Log log = Log.of();

    @Override
    public String help() {
        return generateHelp();
    }

    protected abstract String resource();

    protected abstract Map<String, String> languageResources();

    protected abstract String title();

    private String generateHelp() {
        try {
            var mod = getClass().getModule();
            var mainStream = mod.getResourceAsStream(resource());
            if (mainStream == null) {
                log.warn("[help] resource not found in module {}: {}", mod.getName(), resource());
                return "";
            }
            var langStreams = new LinkedHashMap<String, InputStream>();
            for (var entry : languageResources().entrySet()) {
                var stream = mod.getResourceAsStream(entry.getValue());
                if (stream != null) langStreams.put(entry.getKey(), stream);
            }
            var docs = langStreams.isEmpty()
                ? StepDocLoader.load(mainStream)
                : StepDocLoader.load(mainStream, langStreams);
            return new StepDocMarkdownGenerator().generate(title(), docs);
        } catch (Exception e) {
            log.error(e, "Failed to generate step help for {}", id());
            return "";
        }
    }
}