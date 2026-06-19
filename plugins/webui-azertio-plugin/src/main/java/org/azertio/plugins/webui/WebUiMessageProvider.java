package org.azertio.plugins.webui;

import org.myjtools.jexten.Extension;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.StepDocMessageAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

@Extension
public class WebUiMessageProvider extends StepDocMessageAdapter implements MessageProvider {

    public WebUiMessageProvider() {
        super("steps.yaml");
    }

    @Override
    protected Map<String, String> languageResources() {
        var map = new LinkedHashMap<String, String>();
        map.put("dsl", "steps_dsl.yaml");
        map.put("en",  "steps_en.yaml");
        map.put("es",  "steps_es.yaml");
        return map;
    }

    @Override
    public boolean providerFor(String category) {
        return WebUiStepProvider.class.getSimpleName().equals(category);
    }

}