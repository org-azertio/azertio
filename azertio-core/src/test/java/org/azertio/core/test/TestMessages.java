package org.azertio.core.test;

import org.azertio.core.messages.LocaleMessages;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.Messages;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TestMessages extends Messages {

	public TestMessages (Map<Locale, Map<String,String>> messages) {
		super(List.of(createMessageProvider(messages)));
	}

	private static MessageProvider createMessageProvider(Map<Locale, Map<String,String>> messages) {
		return new MessageProvider() {
			@Override
			public Optional<LocaleMessages> messages(Locale locale) {
				Map<String, String> localizedMessages = messages.get(locale);
				if (localizedMessages != null) {
					return Optional.of(new LocaleMessages() {
						@Override
						public String get(String key) {
							return localizedMessages.getOrDefault(key, key);
						}
					});
				}
				return Optional.empty();
			}

			@Override
			public boolean providerFor(String category) {
				return true;
			}
		};
	}
}
