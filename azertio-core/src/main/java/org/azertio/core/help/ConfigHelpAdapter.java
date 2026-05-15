package org.azertio.core.help;

import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.docgen.ConfigDocLoader;
import org.azertio.core.docgen.ConfigDocMarkdownGenerator;
import org.azertio.core.util.Log;

public abstract class ConfigHelpAdapter implements HelpProvider {

    private static final Log log = Log.of();

    @Override
    public String help() {
        return generateHelp();
    }

    protected abstract String resource();

    protected abstract String title();

    private String generateHelp() {
        var mod = getClass().getModule();
        try (var stream = mod.getResourceAsStream(resource())) {
            if (stream == null) {
                log.warn("[help] resource not found in module {}: {}", mod.getName(), resource());
                return "";
            }
            var docs = ConfigDocLoader.load(stream);
            return new ConfigDocMarkdownGenerator().generate(title(), docs);
        } catch (Exception e) {
            log.error(e, "Failed to generate config help for {}", id());
            return "";
        }
    }
}