package org.azertio.core.messages;

import org.myjtools.jexten.ExtensionPoint;

import java.util.Locale;
import java.util.Optional;

@ExtensionPoint
public interface MessageProvider {


	Optional<LocaleMessages> messages(Locale locale);

	boolean providerFor(String category);

}
