package org.azertio.it;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.messages.MessageAdapter;
import org.azertio.core.messages.MessageProvider;

@Extension(scope = Scope.SINGLETON)
public class TestValidationStepProviderMessageProvider extends MessageAdapter implements MessageProvider {

	public TestValidationStepProviderMessageProvider() {
		super(TestValidationStepProvider.class.getSimpleName());
	}

}