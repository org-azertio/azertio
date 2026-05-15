package org.azertio.core.help;

import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.docgen.ConfigDocLoader;
import org.azertio.core.docgen.ConfigDocMarkdownGenerator;
import org.azertio.core.util.Log;

public abstract class ConfigHelpAdapter implements HelpProvider {

    private static final Log log = Log.of();

    private final String id;
    private final String displayName;
    private final String title;
    private final String resource;

    protected ConfigHelpAdapter(String id, String displayName, String title, String resource) {
        this.id = id;
        this.displayName = displayName;
        this.title = title;
        this.resource = resource;
    }

    @Override public String id()          { return id; }
    @Override public String displayName() { return displayName; }

    @Override
    public String help() {
        var mod = getClass().getModule();
        try (var stream = mod.getResourceAsStream(resource)) {
            if (stream == null) {
                log.warn("[help] resource not found in module {}: {}", mod.getName(), resource);
                return "";
            }
            var docs = ConfigDocLoader.load(stream);
            return new ConfigDocMarkdownGenerator().generate(title, docs);
        } catch (Exception e) {
            log.error(e, "Failed to generate config help for {}", id);
            return "";
        }
    }
}