package org.azertio.core.messages;

import org.azertio.core.docgen.StepDocEntry;
import org.azertio.core.docgen.StepDocLoader;
import org.azertio.core.util.Lazy;
import org.azertio.core.util.Log;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StepDocMessageAdapter implements MessageProvider {

	private static final Log log = Log.of();


	private final Map<Locale, LocaleMessages> localeCache = new ConcurrentHashMap<>();

	private final Lazy<Map<String,StepDocEntry>> entries = Lazy.of(() -> {
		try {
			var langResources = languageResources();
			if (langResources.isEmpty()) {
				return StepDocLoader.load(getClass().getModule().getResourceAsStream(resource()));
			}
			var langStreams = new LinkedHashMap<String, InputStream>();
			for (var langEntry : langResources.entrySet()) {
				var stream = getClass().getModule().getResourceAsStream(langEntry.getValue());
				if (stream != null) {
					langStreams.put(langEntry.getKey(), stream);
				}
			}
			return StepDocLoader.load(getClass().getModule().getResourceAsStream(resource()), langStreams);
		} catch (Exception e) {
			log.error(e, "Failed to load step documentation from resource {}", resource());
			return Map.of();
		}
	});

	private final String resource;

	protected StepDocMessageAdapter(String resource) {
		this.resource = resource;
	}

	@Override
	public Optional<LocaleMessages> messages(Locale locale) {
		return Optional.of(localeCache.computeIfAbsent(locale, this::createLocaleMessages));
	}


	private LocaleMessages createLocaleMessages(Locale locale) {
		Map<String,String> messages = new HashMap<>();
		entries.get().forEach((step, entry) -> entry.language().forEach((localeTag, langEntry) -> {
			if (locale.getLanguage().equalsIgnoreCase(localeTag)) {
				messages.put(step, langEntry.expression());
			}
		}));
		return messages::get;
	}

	protected String resource() {
		return resource;
	}

	protected abstract Map<String, String> languageResources();

}
