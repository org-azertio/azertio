package org.azertio.plugins.messaging;

import org.myjtools.jexten.Extension;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.StepDocMessageAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

@Extension
public class MessagingMessageProvider extends StepDocMessageAdapter implements MessageProvider {

    public MessagingMessageProvider() {
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
        return MessagingStepProvider.class.getSimpleName().equals(category);
    }

}