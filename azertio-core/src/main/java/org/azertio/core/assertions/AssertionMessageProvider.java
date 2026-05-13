package org.azertio.core.assertions;

import org.myjtools.jexten.Extension;
import org.azertio.core.messages.MessageAdapter;
import org.azertio.core.messages.MessageProvider;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Extension(name = AssertionMessageProvider.NAME)
public class AssertionMessageProvider extends MessageAdapter implements MessageProvider {

	public static final String NAME = "AssertionFactoryProvider";

	public AssertionMessageProvider() {
		super(NAME);
	}

}
