package org.azertio.core.messages;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.azertio.core.AzertioException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Abstract adapter for providing localized messages.
 * This class uses a resource file to load messages based on the locale.
 * The resource file should be named in the format "resourceName_language.properties".
 * For example, if the resourceName is "messages" and the locale is "en", the file should be "messages_en.properties".

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public abstract class MessageAdapter implements MessageProvider {



	protected static class ResourceMessages implements LocaleMessages {

		private final Properties properties;

		public ResourceMessages(InputStream inputStream) throws IOException {
			this.properties = new Properties();
			properties.load(inputStream);
		}

		@Override
		public String get(String key) {
			return properties.getProperty(key);
		}
	}



	private final String resourceName;
	private final String category;

	private final LoadingCache<Locale, LocaleMessages> messages = Caffeine.newBuilder()
		.maximumSize(10) // max number of messages to cache
		.build(this::createMessages);


	protected MessageAdapter(String category, String resourceName) {
		this.category = category;
		this.resourceName = resourceName;
	}

	protected MessageAdapter(String category) {
		this(category, category);
	}


	@Override
	public boolean providerFor(String category) {
		return this.category.equalsIgnoreCase(category);
	}


	private LocaleMessages createMessages(Locale locale) {
		var module = getClass().getModule();
		try (var is = module.getResourceAsStream(resourceName + "_" + locale.getLanguage() + ".properties")) {
			if (is == null) {
				return null;
			}
			return new ResourceMessages(is);
		} catch (IOException e) {
			throw new AzertioException(e, "Could not load messages for {} in locale {}", resourceName, locale);
		}
	}


	@Override
	public Optional<LocaleMessages> messages(Locale locale) {
		return Optional.ofNullable(messages.get(locale));
	}



}
